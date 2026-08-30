# davkit-grails-plugin

The Grails plugin for [DavKit](https://tucanoo.com/products/davkit/). It adds the
`davkit:editLink` taglib and `GormDavResourceProvider` base class to the Spring Boot starter.

The dependency coordinates for this checkout are:

```groovy
dependencies {
    implementation "com.tucanoo.davkit:davkit-grails-plugin:0.3.0-SNAPSHOT"
}
```

This is prerelease source. The plugin, `com.tucanoo.davkit:davkit-spring-boot` and the
proprietary `com.tucanoo.davkit:davkit-server` dependency all use `0.3.0-SNAPSHOT`.
The matching DavKit artifacts are not yet available from Maven Central. Before using the
dependency or building from source, ask [dave@tucanoo.com](mailto:dave@tucanoo.com) about
binary access and repository setup. A licence key alone does not supply the dependencies.

Request a key through the [evaluation form](https://tucanoo.com/products/davkit/#evaluation-form).
The plugin and demo source in this repository are licensed under [Apache 2.0](LICENSE).
The core is proprietary and requires a valid licence key at runtime; this repository's
licence does not grant rights to the core. No key is included.

## Compatibility and builds

| Component | Baseline in this checkout |
|---|---|
| Java | Java 17 bytecode and build toolchain |
| Gradle | 8.14.5, wrapper included |
| Grails | Plugin and demo build against 7.2.2 |
| Grails 8 | A manual host check was recorded for 8.0.0-M5 on Java 25 on 2026-08-29 |

The Grails 8 milestone check is not part of this repository's Gradle test suite and does
not establish compatibility with later Grails 8 versions. The plugin leaves Grails and
Spring dependency versions to the host. Validate the plugin in your application's exact
framework combination before deployment.

DavKit repositories version together. The plugin depends on the starter at exactly the
same version, and the starter does the same for the core. Do not mix DavKit versions.

Once matching starter and core binaries are available, run from this repository root:

```sh
./gradlew build
```

The build resolves DavKit binaries from Maven Local or Maven Central. Contributors do not
need proprietary core source. Maintainers with authorised sibling checkouts can use
`../davkit-core` and `../davkit-spring-boot` as optional composite builds; Gradle detects
each directory and substitutes its projects for Maven dependencies. See
[CONTRIBUTING.md](CONTRIBUTING.md) for checks and the remaining public-release requirements.

The `grails-plugin` directory is Gradle project `:davkit-grails-plugin` and produces
the plugin. `demo-grails` is a host application and is not published.

## Using the plugin

Register a provider bean for your document storage. The demo's
[GormDocumentProvider](demo-grails/src/main/groovy/com/tucanoo/davkit/demo/GormDocumentProvider.groovy)
and [bean configuration](demo-grails/grails-app/conf/spring/resources.groovy) show the setup.
The provider's unrestricted document permissions are for local demonstration; a host must
enforce its own access rules.

The plugin enables DavKit by default. Supply the licence through configuration:

```yaml
davkit:
  license-key: ${DAVKIT_LICENSE_KEY}
```

Render an edit link for a document exposed by the provider:

```gsp
<davkit:editLink path="documents/${document.name}" />
```

The tag uses the authenticated request principal as its signing subject, falling back to
`anonymous` when there is no principal. An explicit `user` attribute must come from a
trusted identity, never a request parameter. Signed links are bearer credentials, so only
render them for users who may access the document and keep them out of logs and public pages.
The tag chooses Word, Excel or PowerPoint from the filename extension.

Set `davkit.enabled=false` to disable DavKit's servlet, filters, firewall and supporting
beans. If the application uses Spring Security, configure a separate WebDAV chain without
CSRF or form-login redirects; the [starter documentation](https://github.com/tucanoo/davkit-spring-boot#wiring-the-starter-into-a-host)
explains the host configuration.

Installations sharing an OEM licence derive the same signing key. Configure distinct
`davkit.signed-url.keys` maps when installations must not trust one another's URLs.
A missing, invalid or expired licence key causes DavKit endpoints to return 503 with the reason.

Deploy at the container's root context. Office sends discovery requests to the origin's
`/`, which an application mounted under a context path cannot receive.

## Demo and reporting

The [demo instructions](demo-grails/README.md) cover the local HTTPS certificate and embedded
H2 database. The demo has no login or per-document access restrictions; keep it on a
development machine.

For bugs and changes, see [CONTRIBUTING.md](CONTRIBUTING.md). Report vulnerabilities privately
using [SECURITY.md](SECURITY.md).

Copyright 2026 Tucanoo Solutions Ltd.
