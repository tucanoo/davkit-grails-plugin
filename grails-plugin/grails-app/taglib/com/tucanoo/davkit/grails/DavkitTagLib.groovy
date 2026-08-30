package com.tucanoo.davkit.grails

import com.tucanoo.davkit.auth.SignedUrls
import com.tucanoo.davkit.protocol.DavServlet
import com.tucanoo.davkit.protocol.DavServletConfig
import com.tucanoo.davkit.protocol.PercentCodec
import org.springframework.beans.factory.annotation.Autowired

/**
 * Server-rendered Office links (the documented path is an {@code ms-word:ofe|u|<url>}
 * anchor, no JavaScript required). Mirrors demo-spring-boot's {@code IndexController}: picking
 * the URI scheme by extension is a client-side concern, so it lives here, not in the core.
 */
class DavkitTagLib {

    static namespace = 'davkit'
    // The anchor is assembled here from individually-encoded pieces; letting GSP re-encode the
    // whole output would escape the markup itself.
    static defaultEncodeAs = [taglib: 'none']

    /** Auto-configured from the licence; links are signed per user. */
    @Autowired(required = false)
    SignedUrls davSignedUrls

    @Autowired(required = false)
    DavServletConfig davServletConfig

    /**
     * <code>&lt;davkit:editLink path="documents/Report.docx"&gt;Edit&lt;/davkit:editLink&gt;</code>
     *
     * Attributes:
     * <ul>
     *   <li>{@code path} (required) — the document's path under the DavKit endpoint:
     *       {@code <mountPoint>/<name>}, unencoded.</li>
     *   <li>{@code user} — host-supplied subject for the signed URL; defaults to the authenticated
     *       principal, then {@code anonymous}. An explicit value should come only from a trusted
     *       authenticated identity.</li>
     * </ul>
     * Body: link text; defaults to "Edit in Word/Excel/PowerPoint" by extension.
     *
     * With DavKit active the href carries a token minted for {@code user}, using signing material
     * derived from the licence by default. It is a bearer credential: possession of the URL, not
     * a fresh login by its holder, authenticates the request. The plain endpoint fallback exists
     * only for hosts that instantiate the taglib without the starter's {@code SignedUrls} bean.
     */
    def editLink = { attrs, body ->
        String docPath = attrs.path
        if (!docPath) {
            throwTagError("Tag [editLink] requires attribute [path]")
        }
        String user = attrs.user ?: request.userPrincipal?.name ?: 'anonymous'
        // Office resolves the URL itself, so it must be absolute — scheme, host, port, context
        // path — and it must be the PUBLIC origin: grails.serverURL when the host declares one,
        // otherwise the same X-Forwarded-aware derivation the servlet uses for its multistatus
        // hrefs, so page links and WebDAV hrefs never disagree behind a proxy.
        String origin = serverBaseUrl()
        // Both branches encode with the server's own codec, so the link matches what PathParser
        // decodes — names with '#', '%' or non-ASCII survive, not just spaces.
        String url = davSignedUrls != null
                ? origin + davSignedUrls.path(user, docPath)
                : origin + (davServletConfig != null ? davServletConfig.path() : '/webdav') + '/' + PercentCodec.encodePath(docPath)
        String scheme = officeScheme(docPath)
        out << '<a href="' << "${scheme}:ofe|u|${url}".encodeAsHTML() << '">'
        String text = body()
        // Default text comes from the plugin's message bundle (davkit.editLink.<scheme>), so
        // hosts translate or rebrand it in their own messages*.properties instead of overriding
        // the tag body everywhere.
        out << (text ? text : String.valueOf(message(code: "davkit.editLink.${scheme}")).encodeAsHTML())
        out << '</a>'
    }

    /** {@code grails.serverURL} (authoritative, trailing slash tolerated) or the request-derived origin. */
    private String serverBaseUrl() {
        String configured = grailsApplication?.config?.getProperty('grails.serverURL')
        if (configured) {
            return configured.endsWith('/') ? configured.substring(0, configured.length() - 1) : configured
        }
        DavServlet.resolveBaseUrl(request) // includes the context path
    }

    /** {@code ofe|u|} is "open for edit" (Microsoft Office URI Schemes). */
    private static String officeScheme(String name) {
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
        switch (ext) {
            case ['xlsx', 'xlsm', 'xls', 'csv']: return 'ms-excel'
            case ['pptx', 'pptm', 'ppt']: return 'ms-powerpoint'
            default: return 'ms-word'
        }
    }

}
