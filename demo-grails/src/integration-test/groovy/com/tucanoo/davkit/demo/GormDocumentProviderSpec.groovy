package com.tucanoo.davkit.demo

import com.tucanoo.davkit.spi.DavContent
import com.tucanoo.davkit.spi.DavContext
import com.tucanoo.davkit.spi.DavPath
import com.tucanoo.davkit.spi.DavPreconditionFailedException
import com.tucanoo.davkit.spi.DavPrincipal
import com.tucanoo.davkit.spi.DavResource
import com.tucanoo.davkit.spi.DavWriteRequest
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets

/** Real GORM transactions: another application save must survive a stale DAV write. */
@Integration
class GormDocumentProviderSpec extends Specification {

    private static final DavContext CONTEXT = new DavContext(
            DavPrincipal.ANONYMOUS, 'PUT', null, 'http://localhost/webdav', [:])

    @Autowired
    GormDocumentProvider provider

    long id
    DavResource resolved

    void setup() {
        String name = "concurrency-${UUID.randomUUID()}.docx"
        id = Document.withNewSession {
            Document.withNewTransaction {
                new Document(name: name, bytes: bytes('original')).save(flush: true, failOnError: true).id
            }
        }
        resolved = provider.resolve(DavPath.of('documents', name), CONTEXT).orElseThrow()
    }

    void cleanup() {
        Document.withNewSession {
            Document.withNewTransaction { Document.get(id)?.delete(flush: true) }
        }
    }

    @Unroll
    void 'rejects an edit committed between resolve and write with If-Match present: #withIfMatch'() {
        given:
        long updatedVersion = commitApplicationEdit()
        Optional<String> ifMatch = withIfMatch ? Optional.of(resolved.etag()) : Optional.empty()

        when:
        provider.write(resolved, request(DavContent.of(bytes('DAV edit')), ifMatch), CONTEXT)

        then:
        thrown(DavPreconditionFailedException)
        assertApplicationEditPreserved(updatedVersion)

        where:
        withIfMatch << [false, true]
    }

    void 'rejects an edit committed after the write transaction reloads the row'() {
        given:
        long updatedVersion
        DavContent content = new DavContent() {
            @Override
            InputStream open() {
                // The row has been loaded; commit another transaction before the provider flushes.
                updatedVersion = commitApplicationEdit()
                new ByteArrayInputStream(bytes('DAV edit'))
            }

            @Override
            OptionalLong length() { OptionalLong.empty() }
        }

        when:
        provider.write(resolved, request(content, Optional.empty()), CONTEXT)

        then:
        thrown(DavPreconditionFailedException)
        assertApplicationEditPreserved(updatedVersion)
    }

    void 'accepts an unchanged resource and returns the new version'() {
        given:
        long versionBefore = Document.withNewSession { Document.get(id).version }

        when:
        DavResource saved = provider.write(resolved,
                request(DavContent.of(bytes('DAV edit')), Optional.empty()), CONTEXT)

        then:
        Map row = Document.withNewSession {
            Document doc = Document.get(id)
            [bytes: doc.bytes, version: doc.version]
        }
        row.bytes == bytes('DAV edit')
        row.version == versionBefore + 1
        saved.etag() == "${id}-${versionBefore + 1}".toString()
    }

    void 'rejects a document deleted since resolve'() {
        given:
        Document.withNewSession {
            Document.withNewTransaction { Document.get(id).delete(flush: true) }
        }

        when:
        provider.write(resolved, request(DavContent.of(bytes('DAV edit')), Optional.empty()), CONTEXT)

        then:
        thrown(DavPreconditionFailedException)
        Document.withNewSession { Document.get(id) } == null
    }

    private long commitApplicationEdit() {
        Document.withNewSession {
            Document.withNewTransaction {
                Document row = Document.get(id)
                row.bytes = bytes('application edit')
                row.save(flush: true, failOnError: true).version
            }
        }
    }

    private void assertApplicationEditPreserved(long expectedVersion) {
        Map row = Document.withNewSession {
            Document doc = Document.get(id)
            [bytes: doc.bytes, version: doc.version]
        }
        assert row.bytes == bytes('application edit')
        assert row.version == expectedVersion
    }

    private static DavWriteRequest request(DavContent content, Optional<String> ifMatch) {
        new DavWriteRequest(content, ifMatch, Optional.empty(), Optional.empty())
    }

    private static byte[] bytes(String text) { text.getBytes(StandardCharsets.UTF_8) }
}
