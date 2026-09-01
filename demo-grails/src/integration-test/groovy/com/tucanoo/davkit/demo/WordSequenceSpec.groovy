package com.tucanoo.davkit.demo

import com.tucanoo.davkit.auth.SignedUrls
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets

/**
 * The whole host wired together on H2 and the real embedded Tomcat, over real HTTP with the JDK
 * client (the same approach as demo-spring-boot's DemoApplicationTest — MockMvc cannot reach a
 * registered servlet). Proves the starter's auto-configuration engages under Grails with an
 * empty doWithSpring, then walks the sequence Word actually sends (OPTIONS → HEAD → LOCK →
 * GET → PUT with the token → UNLOCK) and checks the row changed and the ETag moved on.
 */
@Integration
class WordSequenceSpec extends Specification {

    static final String LOCK_BODY = '<?xml version="1.0" encoding="utf-8" ?>' +
            '<D:lockinfo xmlns:D="DAV:"><D:lockscope><D:exclusive/></D:lockscope>' +
            '<D:locktype><D:write/></D:locktype><D:owner><D:href>DAVE-PC\\dave</D:href></D:owner></D:lockinfo>'

    @Autowired
    SignedUrls signedUrls

    HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

    private HttpResponse<byte[]> send(String method, String path, byte[] body, String... headers) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:${serverPort}${path}"))
                .method(method, body == null ? BodyPublishers.noBody() : BodyPublishers.ofByteArray(body))
        if (headers.length > 0) {
            b.headers(headers)
        }
        http.send(b.build(), BodyHandlers.ofByteArray())
    }

    void 'index page renders a signed server-side Office link per document type via the taglib'() {
        when:
        HttpResponse<byte[]> page = send('GET', '/', null)
        String html = new String(page.body(), StandardCharsets.UTF_8)

        then:
        page.statusCode() == 200
        html.contains("ms-word:ofe|u|http://localhost:${serverPort}/webdav/t/".toString())
        html.contains('/documents/Welcome%20letter.docx"')

        and: 'the Excel and PowerPoint rows get their own scheme from the same taglib'
        html.contains("ms-excel:ofe|u|http://localhost:${serverPort}/webdav/t/".toString())
        html.contains('/documents/Quarterly%20numbers.xlsx"')
        html.contains("ms-powerpoint:ofe|u|http://localhost:${serverPort}/webdav/t/".toString())
        html.contains('/documents/Kickoff%20deck.pptx"')

        and: 'the default link text resolves from the plugin\'s i18n bundle, not a hardcoded label'
        html.contains('>Edit in Word</a>')
        html.contains('>Edit in Excel</a>')
        html.contains('>Edit in PowerPoint</a>')
    }

    void 'the Excel and PowerPoint rows serve the same verb sequence with their own content type'() {
        expect: 'no type-specific code path: LOCK → PUT → UNLOCK works unchanged for both'
        [['Quarterly numbers.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'],
         ['Kickoff deck.pptx', 'application/vnd.openxmlformats-officedocument.presentationml.presentation']]
                .every { name, contentType ->
            String doc = signedUrls.path('dave', "documents/${name}")
            HttpResponse<byte[]> get = send('GET', doc, null)
            assert get.statusCode() == 200
            assert get.headers().firstValue('Content-Type').orElse('').contains(contentType)

            HttpResponse<byte[]> lock = send('LOCK', doc, LOCK_BODY.getBytes(StandardCharsets.UTF_8),
                    'Content-Type', 'text/xml', 'Timeout', 'Second-600')
            assert lock.statusCode() == 200
            String token = lock.headers().firstValue('Lock-Token').orElseThrow()

            byte[] edited = "edited ${name}".getBytes(StandardCharsets.UTF_8)
            assert send('PUT', doc, edited, 'Content-Type', 'text/xml', 'If', "(${token})".toString())
                    .statusCode() == 204
            assert send('UNLOCK', doc, null, 'Lock-Token', token).statusCode() == 204
            assert Document.withNewSession { Document.findByName(name).bytes } as List == edited as List
            true
        }
    }

    void 'plain URLs without a token are refused'() {
        expect: 'no login redirect, no challenge — a plain 403'
        send('GET', '/webdav/documents/Welcome%20letter.docx', null).statusCode() == 403
    }

    void 'the Word verb sequence updates the row through the GORM provider'() {
        given: 'the same tokenised prefix for the document and its parent collection, as Word uses them'
        String doc = signedUrls.path('dave', 'documents/Welcome letter.docx')
        String parent = doc.substring(0, doc.lastIndexOf('/') + 1)
        long versionBefore = Document.withNewSession { Document.findByName('Welcome letter.docx').version }

        when: 'the parent collection is probed'
        HttpResponse<byte[]> options = send('OPTIONS', parent, null)

        then: 'class 2 is advertised'
        options.statusCode() == 200
        options.headers().firstValue('DAV').orElse('') == '1, 2'

        when:
        HttpResponse<byte[]> head = send('HEAD', doc, null)
        String etagBefore = head.headers().firstValue('ETag').orElseThrow()

        then:
        head.statusCode() == 200

        when: 'Word locks with its observed timeout'
        HttpResponse<byte[]> lock = send('LOCK', doc, LOCK_BODY.getBytes(StandardCharsets.UTF_8),
                'Content-Type', 'text/xml', 'Timeout', 'Second-600')
        String token = lock.headers().firstValue('Lock-Token').orElseThrow()

        then:
        lock.statusCode() == 200
        token.startsWith('<opaquelocktoken:')

        and: 'GET streams the document with the Word content type'
        with(send('GET', doc, null)) {
            statusCode() == 200
            headers().firstValue('Content-Type').orElse('')
                    .contains('application/vnd.openxmlformats-officedocument.wordprocessingml.document')
        }

        and: 'PUT without the token on a locked document is refused'
        send('PUT', doc, 'intruder'.bytes, 'Content-Type', 'text/xml').statusCode() == 423

        when: 'the save carries the token'
        byte[] edited = 'edited in Word'.getBytes(StandardCharsets.UTF_8)
        HttpResponse<byte[]> put = send('PUT', doc, edited, 'Content-Type', 'text/xml', 'If', "(${token})".toString())
        String etagAfter = put.headers().firstValue('ETag').orElseThrow()

        then: '204, with the refreshed ETag echoed'
        put.statusCode() == 204
        etagAfter != etagBefore

        and:
        send('UNLOCK', doc, null, 'Lock-Token', token).statusCode() == 204

        when: 'the row is re-read outside any request'
        Map row = Document.withNewSession {
            Document d = Document.findByName('Welcome letter.docx')
            [bytes: d.bytes, version: d.version, id: d.id]
        }

        then: 'the GORM provider committed the new bytes and the version moved once'
        row.bytes == edited
        row.version == versionBefore + 1
        etagAfter == "\"${row.id}-${row.version}\"".toString()
    }
}
