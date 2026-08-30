package com.tucanoo.davkit.grails

import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

/**
 * Separate spec because the configuration is fixed at context setup: when the host declares
 * {@code grails.serverURL} (the idiomatic Grails "my public base URL" setting), it wins over
 * anything derived from the request — including forwarded headers.
 */
class DavkitTagLibServerUrlSpec extends Specification implements TagLibUnitTest<DavkitTagLib> {

    Closure doWithConfig() {
        { config ->
            config['grails.serverURL'] = 'https://public.example.com/app/' // trailing slash on purpose
        }
    }

    void 'grails.serverURL is authoritative for the link origin'() {
        given: 'forwarded headers that would otherwise win'
        request.addHeader('X-Forwarded-Host', 'internal.example.com')

        when:
        String html = applyTemplate('<davkit:editLink path="documents/Report.docx"/>')

        then:
        html.contains('ms-word:ofe|u|https://public.example.com/app/webdav/documents/Report.docx')
    }
}
