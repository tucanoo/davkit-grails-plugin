package com.tucanoo.davkit.grails

import grails.plugins.Plugin

/**
 * Thin Grails wrapper over the Spring Boot starter. Nothing
 * protocol-level lives here: Grails 7/8 run Spring Boot auto-configuration, so
 * {@code server-spring-boot}'s {@code DavKitAutoConfiguration} — pulled in transitively by this
 * plugin — registers the servlet, filters, lock service and auth chain exactly as it does in a
 * plain Boot host. The plugin adds only the Grails-facing conveniences: the {@code davkit:editLink}
 * taglib and the {@link GormDavResourceProvider} base class.
 */
class DavkitGrailsPlugin extends Plugin {

    def grailsVersion = "7.2.2  > *"

    def title = "DavKit"
    def author = "Tucanoo Solutions Ltd"
    def authorEmail = "dave@tucanoo.com"
    def description = '''\
Office-aware WebDAV for Grails: expose application documents (GORM blobs, files, object storage)
so Microsoft Word, Excel and PowerPoint open, lock, edit and save them back from one click,
without the host becoming a file server.
'''
    def documentation = "https://github.com/tucanoo/davkit-grails-plugin"

    // Bean registration is deliberately empty: DavKitAutoConfiguration does it all (verified by
    // demo-grails' integration test). Hand-register here only what auto-configuration provably
    // cannot do under Grails.
    Closure doWithSpring() { { ->
        }
    }
}
