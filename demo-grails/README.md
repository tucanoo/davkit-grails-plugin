# demo-grails

A Grails host with an in-memory H2 database and three generated documents: a `.docx`, an `.xlsx`
and a `.pptx`. The plugin's `davkit:editLink` tag renders links for desktop Word, Excel and
PowerPoint. Saves update the GORM rows; all data disappears when the application stops.

This is a local development demo, bound to `127.0.0.1`. It has no login and grants read/write access to every
seeded document. Anyone who can load the page can obtain its signed edit links. The embedded
database uses user `sa` with an empty password. Do not expose the application to the internet
or use it for sensitive documents. HTTP does not encrypt document contents or signed URLs.

## Run

Start in the `davkit-grails-plugin` repository root, not this module directory. You need
Java 17, desktop Office for a manual edit test, and the matching DavKit binary dependencies
described in the [repository README](../README.md). The current `0.3.0-SNAPSHOT` DavKit binaries are
not yet available from Maven Central. No Docker, separate database, mkcert or `.p12` file is needed.

Request a licence key through the [evaluation form](https://tucanoo.com/products/davkit/#evaluation-form).
No key is included. Set `DEMO_LICENSE_KEY` in your local environment before starting the demo.
Without a valid key, DavKit endpoints return 503 with the reason.

With `DEMO_LICENSE_KEY` set, run:

```sh
./gradlew :demo-grails:bootRun
```

Development mode serves [http://localhost:8080/](http://localhost:8080/). Click an edit link,
enable editing if Office asks, make a change and save. Click **Refresh** to reload the page
and check the **Version** and **Updated** columns, just like the Spring Boot demo.

Ctrl+C stops the demo and discards edits. Restarting recreates the three sample documents.
Both demos default to port 8080; run them one at a time, or use `--args='--server.port=8081'`
for one of them. If Office refuses HTTP, try the optional HTTPS profile below.

## Optional HTTPS

For a comparison using trusted HTTPS, install [mkcert](https://github.com/FiloSottile/mkcert)
and create a local certificate from the repository root:

```sh
mkdir -p demo-grails/src/main/resources/certs
mkcert -install
mkcert -pkcs12 -p12-file demo-grails/src/main/resources/certs/localhost.p12 localhost 127.0.0.1 ::1
./gradlew :demo-grails:bootRun --args='--spring.profiles.active=https'
```

With `DEMO_LICENSE_KEY` still set, open [https://localhost:8443/](https://localhost:8443/).
The profile changes only the port and TLS settings; development storage remains in-memory H2.

`mkcert -install` adds its local CA to your trust store. Office must trust that CA in the OS
store too; accepting a browser certificate warning is not enough. Never share the CA's
private key. Keep generated certificates and keys out of commits and distributed packages.

The HTTPS profile uses `classpath:certs/localhost.p12`, with password `changeit`.
`DEMO_KEYSTORE` accepts another location, such as `file:/absolute/path/localhost.p12`, and
`DEMO_KEYSTORE_PASSWORD` overrides the password. Neither setting is used by default HTTP.

## Code and checks

[GormDocumentProvider](src/main/groovy/com/tucanoo/davkit/demo/GormDocumentProvider.groovy) implements
the storage operations, and [resources.groovy](grails-app/conf/spring/resources.groovy) registers
it. The [GSP](grails-app/views/home/index.gsp) renders the edit links.

The page uses Bootstrap CSS only. Edit links and the inline **Refresh** button need no
jQuery, Bootstrap JavaScript or JavaScript bundle, so loading the page does not invoke
Asset Pipeline's Babel/GraalVM JavaScript processor.

Run the integration tests from the repository root:

```sh
./gradlew :demo-grails:integrationTest
```

Tests use the embedded database and exercise the real server's HTTP lock/read/write sequence.
They need no licence key or TLS certificate. A manual Office session is still needed to check
that a document opens without a repair prompt and saves correctly on your client.
