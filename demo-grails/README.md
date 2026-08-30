# demo-grails

A Grails host with an in-memory H2 database and three generated documents: a `.docx`, an `.xlsx`
and a `.pptx`. The plugin's `davkit:editLink` tag renders links for desktop Word, Excel and
PowerPoint. Saves update the GORM rows; all data disappears when the application stops.

This is a local development demo. It has no login and grants read/write access to every
seeded document. Anyone who can load the page can obtain its signed edit links. The embedded
database uses user `sa` with an empty password. Do not expose the application to the internet
or use it for sensitive documents.

## Run

Start in the `davkit-grails-plugin` repository root, not this module directory. You need
Java 17, desktop Office for a manual edit test, and the matching DavKit binary dependencies
described in the [repository README](../README.md). The current `0.3.0-SNAPSHOT` DavKit binaries are
not yet available from Maven Central. No separate database or Docker setup is needed.

Request a licence key through the [evaluation form](https://tucanoo.com/products/davkit/#evaluation-form).
No key is included. Set `DEMO_LICENSE_KEY` in your local environment before starting the demo.
Without a valid key, DavKit endpoints return 503 with the reason.

Install [mkcert](https://github.com/FiloSottile/mkcert), then create the ignored certificate
directory and a certificate for this machine:

```sh
mkdir -p demo-grails/src/main/resources/certs
mkcert -install
mkcert -pkcs12 -p12-file demo-grails/src/main/resources/certs/localhost.p12 localhost 127.0.0.1 ::1
```

`mkcert -install` adds its local CA to your trust store. The machine running Office must trust
that CA too; accepting a browser certificate warning is not enough. Never share the CA's
private key. Use a certificate with the correct hostname if Office runs on another machine.

With `DEMO_LICENSE_KEY` set, run:

```sh
./gradlew :demo-grails:bootRun
```

Development mode serves [https://localhost:8443/](https://localhost:8443/). Click an edit link,
enable editing if Office asks, make a change and save. Reload the page to check the row's
version and timestamp.

The default keystore is `classpath:certs/localhost.p12`, with password `changeit`. Set
`DEMO_KEYSTORE` to a Spring resource location such as `file:/absolute/path/localhost.p12` to
use a different file. Change `server.ssl.key-store-password` in the development configuration
if that file has a different password. Keep generated certificates and keys out of commits
and distributed application packages.

## Code and checks

[GormDocumentProvider](src/main/groovy/com/tucanoo/davkit/demo/GormDocumentProvider.groovy) implements
the storage operations, and [resources.groovy](grails-app/conf/spring/resources.groovy) registers
it. The [GSP](grails-app/views/home/index.gsp) renders the edit links.

Run the integration tests from the repository root:

```sh
./gradlew :demo-grails:integrationTest
```

Tests use the embedded database and exercise the real server's HTTP lock/read/write sequence.
They need no licence key or TLS certificate. A manual Office session is still needed to check
that a document opens without a repair prompt and saves correctly on your client.
