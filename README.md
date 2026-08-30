# davkit-grails-plugin

The Grails 7 and 8 plugin for [DavKit](https://tucanoo.com/products/davkit): the
`davkit:editLink` taglib and a `GormDavResourceProvider` base class, over the Spring Boot starter.

Apache License 2.0. This wrapper is open source; the DavKit core it ultimately depends on
(`com.tucanoo.davkit:davkit-server`) is proprietary and requires a licence key. Publishing this wrapper
openly does not make the core open source.

Copyright 2026 Tucanoo Solutions Ltd.

## Modules

| Module | Purpose | Published |
|---|---|---|
| `grails-plugin` | The plugin itself | yes, `com.tucanoo.davkit:davkit-grails-plugin` |
| `demo-grails` | Reference host on embedded H2, zero setup | no |

The directory is `grails-plugin` but the published artifactId is `davkit-grails-plugin`; the
rename lives in `settings.gradle`.

The plugin enables DavKit by default. A valid licence is the only signing material needed:

```yaml
davkit:
  license-key: ${DAVKIT_LICENSE_KEY}
```

Set `davkit.enabled=false` to keep the plugin dependency while disabling DavKit's servlet,
discovery filters, Spring Security firewall and supporting beans.

Installations using the same OEM licence derive the same signing key. When those installations
are separate trust boundaries, configure a distinct `davkit.signed-url.keys` map in each one.

The plugin and starter pass raw `davkit.license-key` text to the proprietary core. Only the core
verifies the product signature and creates approved runtime state; a host `LicenseGate` bean does
not replace that decision.

## Build

```
./gradlew build
```

Java 17, Gradle 8.14 (wrapper included). The plugin is built and tested against Grails 7.2.2.
The same Java 17 artifact is intended for Grails 8 and is validated in a real Grails 8 host
before release.

The build needs `com.tucanoo.davkit:davkit-spring-boot` at the same version, which in turn needs
the core. Both are resolved automatically in one of two ways:

- **Sibling checkouts.** Whichever of `../davkit-core` and `../davkit-spring-boot` exist are
  included as composite builds, so an upstream change is picked up with no publish step. This is
  the internal development layout:

  ```
  davkit/
    ├─ davkit-core/
    ├─ davkit-spring-boot/
    └─ davkit-grails-plugin/    <- you are here
  ```

- **Maven.** Otherwise the coordinates resolve normally. Until the core is published to Maven
  Central, that means `mavenLocal`, fed by `publishToMavenLocal` in the two upstream repositories.

## Deployment

Deploy the application at the container's root context. Office sends its discovery probes to the
origin's `/`, which an application under a context path never receives. `grails run-app` and
executable jars already own the root; the case to watch is a WAR in a shared container. The
deployment guide supplied with your licence covers this and the rest of a production install.

## Running the demo

```
./gradlew :demo-grails:bootRun
```

Embedded H2, one seeded document, edits vanish on restart. Development mode serves HTTPS because
Office trusts the OS certificate store, not the browser's. Generate a local certificate with
[mkcert](https://github.com/FiloSottile/mkcert) and install its root CA on the machine that will
run Word:

```
mkcert -pkcs12 -p12-file demo-grails/src/main/resources/certs/localhost.p12 localhost 127.0.0.1 ::1
```

The demo loads it as `classpath:certs/localhost.p12`, so it resolves identically under
`bootRun`, an IDE run configuration and a built war. Point `DEMO_KEYSTORE` at any Spring resource
location (`file:/path/to/your.p12`) to use your own. The demo also needs a DavKit licence key in
`DEMO_LICENSE_KEY`; without one the application starts normally and the DavKit endpoints answer
503 explaining why.

## Versioning

All three DavKit repositories ship in lockstep and bump together. The plugin depends on the
starter at that exact version, never a range.
