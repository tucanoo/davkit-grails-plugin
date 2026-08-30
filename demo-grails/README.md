# demo-grails

The Grails mirror of `demo-spring-boot`, kept zero-setup: embedded in-memory H2, no Postgres, no
Docker, no login. Three GORM `Document` rows seeded with a minimal `.docx`, `.xlsx` and `.pptx`
(generated in `BootStrap`, no binary fixtures), one page whose edit links are rendered by the
plugin's `davkit:editLink` taglib as signed URLs — the taglib picks `ms-word:` / `ms-excel:` /
`ms-powerpoint:` and the label per extension.

```bash
DEMO_LICENSE_KEY=<your key> ./gradlew :demo-grails:bootRun
```

DavKit needs a licence key for all operation and this repository ships none, deliberately — a key
committed to a demo is a key anyone can use, and verification is offline, so there would be no way
to withdraw it. Free evaluation keys: <https://tucanoo.com/products/davkit>. Without a key the demo still
boots and the page renders; only `/webdav/**` answers 503 with the reason. The integration test
needs no key: helpers in `src/integration-test` supply the starter's primary internal test state and are absent from
normal builds and every published artifact.

Development mode serves **https://localhost:8443**. Office trusts the OS certificate store
rather than the browser's, so the mkcert root CA must be installed on the machine running Office.
The `mkcert` command is in the repository README; it writes the keystore to
`src/main/resources/certs/localhost.p12`, which this demo loads as `classpath:certs/localhost.p12`.
`certs/` is gitignored, so a fresh clone has none until you generate one.

The keystore is a classpath location rather than a file path on purpose: `gradlew bootRun` starts
in the module directory and an IDE run configuration usually starts in the repository root, and a
relative file path cannot satisfy both. `DEMO_KEYSTORE` takes a full Spring resource location, so
`file:/absolute/path.p12` works for a certificate kept outside the project. Click a link, let Word/Excel/PowerPoint open it, Enable
Editing, type, Ctrl+S; the row updates (and vanishes on restart — it's an in-memory database).

`src/integration-test/.../WordSequenceSpec.groovy` boots the real server and replays the Word
verb sequence over HTTP with the JDK client. Run with
`./gradlew :demo-grails:integrationTest`.

Skeleton generated from Grails Forge (`latest.grails.org`, type `web`; parameters in
`grails-forge-cli.yml`).
