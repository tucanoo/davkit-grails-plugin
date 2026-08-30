package com.tucanoo.davkit.demo

import com.tucanoo.davkit.grails.GormDavResourceProvider
import com.tucanoo.davkit.spi.DavContent
import com.tucanoo.davkit.spi.DavContext
import com.tucanoo.davkit.spi.DavPath
import com.tucanoo.davkit.spi.DavPermissions
import com.tucanoo.davkit.spi.DavPreconditionFailedException
import com.tucanoo.davkit.spi.DavResource
import com.tucanoo.davkit.spi.DavWriteRequest
import org.springframework.dao.OptimisticLockingFailureException

/**
 * The reference GORM-backed provider — demo-spring-boot's {@code JpaDocumentProvider} translated
 * onto {@link GormDavResourceProvider}, which supplies the withNewSession / withNewTransaction
 * plumbing. One row per document under
 * {@code /webdav/documents/<name>}.
 */
class GormDocumentProvider extends GormDavResourceProvider {

    static final String MOUNT = 'documents'

    @Override
    String mountPoint() { MOUNT }

    @Override
    protected Class<?> domainClass() { Document }

    @Override
    protected Optional<DavResource> resolveInSession(DavPath path, DavContext ctx) {
        List<String> rest = path.remainderFor(MOUNT)
        if (rest.size() != 1) {
            return Optional.empty()
        }
        Document doc = Document.findByName(rest[0])
        doc == null ? Optional.<DavResource> empty() : Optional.of(describe(doc, path))
    }

    @Override
    protected DavContent readInSession(DavResource resource, DavContext ctx) {
        // The stashed entity is detached but its byte[] is already loaded (no lazy blob), so the
        // content survives the session — the base class's contract.
        Document doc = resource.attribute('document', Document)
                .orElseGet { Document.get(idOf(resource)) }
        DavContent.of(doc.bytes)
    }

    @Override
    protected DavResource writeInTransaction(DavResource resource, DavWriteRequest request, DavContext ctx) {
        Document doc = Document.get(idOf(resource))
        if (doc == null) {
            throw new DavPreconditionFailedException('document deleted')
        }
        doc.bytes = request.content().open().withCloseable { it.readAllBytes() }
        try {
            doc.save(flush: true, failOnError: true)
        } catch (OptimisticLockingFailureException ignored) {
            // 412 rather than 500: Word shows "changed by another user" instead of retrying.
            throw new DavPreconditionFailedException('document changed concurrently')
        }
        describe(doc, resource.path())
    }

    private static DavResource describe(Document doc, DavPath path) {
        new DavResource(
                String.valueOf(doc.id), path, doc.name, false, null,
                doc.bytes.length, "${doc.id}-${doc.version}".toString(), doc.lastUpdated.toInstant(),
                Optional.of(doc.dateCreated.toInstant()), DavPermissions.READ_WRITE,
                [document: doc])
    }

    private static long idOf(DavResource resource) {
        Long.parseLong(resource.key())
    }
}
