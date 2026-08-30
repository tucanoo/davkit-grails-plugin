package com.tucanoo.davkit.grails

import com.tucanoo.davkit.auth.SignedUrlKeys
import com.tucanoo.davkit.auth.SignedUrls
import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

import java.time.Clock
import java.time.Duration

/**
 * The taglib in isolation (the demo's integration test covers it wired into a real host).
 * With no SignedUrls bean (as when the starter is explicitly disabled) the tag falls back to the
 * plain endpoint URL; the signed branch is checked by decoding the minted token.
 */
class DavkitTagLibSpec extends Specification implements TagLibUnitTest<DavkitTagLib> {

    private static String href(String html) {
        def m = html =~ /href="([^"]+)"/
        assert m.find()
        m.group(1)
    }

    void 'renders a plain ms-word link when the starter supplies no SignedUrls bean'() {
        when:
        String html = applyTemplate('<davkit:editLink path="documents/Report.docx"/>')

        then: 'absolute URL: Office resolves it itself, so origin and endpoint path are included'
        href(html) == "ms-word:ofe|u|http://localhost/webdav/documents/Report.docx"
    }

    void 'picks the scheme by extension, unknown extensions open in Word'() {
        expect:
        href(applyTemplate("<davkit:editLink path=\"$path\"/>")).startsWith(scheme + ':ofe|u|')

        where:
        path                     | scheme
        'documents/Report.docx'  | 'ms-word'
        'documents/Numbers.XLSX' | 'ms-excel'
        'documents/data.csv'     | 'ms-excel'
        'documents/Deck.pptx'    | 'ms-powerpoint'
        'documents/notes.txt'    | 'ms-word'
        'documents/extensionless'| 'ms-word'
    }

    void 'percent-encodes the plain URL with the server codec, not just spaces'() {
        expect:
        href(applyTemplate('<davkit:editLink path="documents/Welcome letter.docx"/>'))
                .endsWith('/webdav/documents/Welcome%20letter.docx')
        href(applyTemplate('<davkit:editLink path="documents/Q1 #2 100%.docx"/>'))
                .endsWith('/webdav/documents/Q1%20%232%20100%25.docx')
    }

    void 'honours X-Forwarded headers like the servlet does for its hrefs'() {
        given: 'a request that came through a TLS-terminating proxy'
        request.addHeader('X-Forwarded-Proto', 'https')
        request.addHeader('X-Forwarded-Host', 'app.example.com')

        expect: 'the link carries the public origin, not the container host'
        href(applyTemplate('<davkit:editLink path="documents/Report.docx"/>'))
                .startsWith('ms-word:ofe|u|https://app.example.com/webdav/')
    }

    void 'the tag body overrides the default link text'() {
        expect:
        applyTemplate('<davkit:editLink path="d/x.docx">Open it</davkit:editLink>').contains('>Open it</a>')
    }

    void 'default link text resolves through the message source by scheme key'() {
        given: 'the host (here: the test) defines the key the plugin bundle ships'
        messageSource.addMessage('davkit.editLink.ms-excel', request.locale, 'In Excel bearbeiten')

        expect:
        applyTemplate('<davkit:editLink path="d/x.xlsx"/>').contains('>In Excel bearbeiten</a>')
    }

    void 'uses the signed URL for the given user when SignedUrls is present'() {
        given:
        tagLib.davSignedUrls = new SignedUrls('/webdav',
                new SignedUrlKeys([test: 'unit-test-signing-key-0123456789abcdef'], 'test'),
                Duration.ofHours(8), Clock.systemUTC())

        when:
        String url = href(applyTemplate('<davkit:editLink path="documents/Report.docx" user="dave"/>'))

        then: 'the href carries a token minted for that user over that document'
        url.contains('/webdav/t/')
        String payload = url.split('/t/')[1].split('/')[0].split('\\.')[0]
        new String(Base64.urlDecoder.decode(payload)).contains('dave')
        url.endsWith('/documents/Report.docx')
    }

    void 'a missing path attribute is a tag error, not a broken link'() {
        when:
        applyTemplate('<davkit:editLink/>')

        then:
        Exception e = thrown()
        List<String> messages = []
        for (Throwable t = e; t != null; t = t.cause) {
            messages << String.valueOf(t.message)
        }
        messages.any { it.contains('[path]') }
    }
}
