# Changelog

DavKit components use the same exact version and release together.

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
