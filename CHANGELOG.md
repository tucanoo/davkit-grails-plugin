# Changelog

DavKit components use the same exact version and release together.

## 1.0.12 — 2026-10-27

- Updated the starter and transitive core dependencies to 1.0.12, fixing Office GET/POST
  requests to application content outside `davkit.path`, including Outlook email images.
  Root discovery, non-root HEAD handling and SharePoint/FrontPage path handling are unchanged.
- Documented the application-wide discovery filter scope and its disable setting.

## 1.0.11 — 2026-10-10

- Added support for `class`, `id`, `title`, `aria-*` and `data-*` attributes on
  `davkit:editLink`. Attribute values are HTML-encoded; the tag continues to generate
  the signed Office `href`.
- Updated the starter dependency to 1.0.11, including its startup WARN for a non-root
  servlet context and the core's per-link `SignedUrls.path` validity overload.
- Documented edit-link styling, startup logging configuration and the requirement to
  configure `davkit.enabled` before auto-configuration runs.

## 1.0.10 — 2026-09-12

- Exited beta.
