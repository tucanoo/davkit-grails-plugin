package com.tucanoo.davkit.grails

import com.tucanoo.davkit.spi.DavContent
import com.tucanoo.davkit.spi.DavContext
import com.tucanoo.davkit.spi.DavPath
import com.tucanoo.davkit.spi.DavResource
import com.tucanoo.davkit.spi.DavResourceProvider
import com.tucanoo.davkit.spi.DavWriteRequest

/**
 * Base class for GORM-backed providers, implementing the session/transaction pattern DavKit's
 * storage SPI expects: DavKit calls arrive on WebDAV worker threads with
 * no Hibernate session bound, so every read runs inside {@code withNewSession} and every write
 * inside {@code withNewTransaction}. Subclasses implement the {@code *InSession} /
 * {@code *InTransaction} methods and never worry about session plumbing.
 *
 * <p>Two contract points the wrapping imposes:</p>
 * <ul>
 *   <li>{@link #readInSession} must return session-independent content — {@code byte[]} or a
 *       stream over something outside the session (a file), never a lazy blob proxy: the session
 *       is closed before DavKit streams the content to the client.</li>
 *   <li>{@link #resolveInSession} should stash the loaded entity in the resource's
 *       {@code attributes} map so the read in the same request can reuse it, but the
 *       entity is detached by then — reattach or reload before touching lazy state.</li>
 * </ul>
 */
abstract class GormDavResourceProvider implements DavResourceProvider {

    /** The domain class whose GORM statics provide the session/transaction context. */
    protected abstract Class<?> domainClass()

    /** {@link DavResourceProvider#resolve} body, already inside {@code withNewSession}. */
    protected abstract Optional<DavResource> resolveInSession(DavPath path, DavContext ctx)

    /** {@link DavResourceProvider#read} body, already inside {@code withNewSession}. */
    protected abstract DavContent readInSession(DavResource resource, DavContext ctx)

    /** {@link DavResourceProvider#write} body, already inside {@code withNewTransaction}. */
    protected abstract DavResource writeInTransaction(DavResource resource, DavWriteRequest request, DavContext ctx)

    @Override
    Optional<DavResource> resolve(DavPath path, DavContext ctx) throws IOException {
        domainClass().withNewSession { resolveInSession(path, ctx) }
    }

    @Override
    DavContent read(DavResource resource, DavContext ctx) throws IOException {
        domainClass().withNewSession { readInSession(resource, ctx) }
    }

    @Override
    DavResource write(DavResource resource, DavWriteRequest request, DavContext ctx) throws IOException {
        domainClass().withNewTransaction { writeInTransaction(resource, request, ctx) }
    }
}
