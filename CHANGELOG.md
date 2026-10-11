# Changelog

All notable changes to this maintained version of The Number Race
are documented here.

## Unreleased

### Added
- Brazilian Portuguese language pack.
- Validated development environment guide (`numberrace/docs/AMBIENTE.pt.md`).
- Maven reactor for language packs.
- Eclipse-oriented development workflow.
- Interaction-data logging component (`org.unicog.numberrace.analytics`): records the
  23 event types of the interaction event model 1.0 to one JSON Lines file per session
  (official record) and to a SQLite database (query copy), in a background thread,
  with validation before writing and pseudonymous participant codes
  (`evolution/pt/componente-registro.md`).
- Unit tests (JUnit 4) for the logging component.
- Interaction-data exporter (`org.unicog.numberrace.analytics.export.ExportTool`,
  `numberrace/tools/exportar.bat`): merges the JSON Lines files of one or more
  computers without duplicates, reports problems, and writes an export package with
  the raw events, per-event, per-session and per-turn tables in CSV (standard and
  Brazilian Excel versions), a consolidated SQLite database, a data dictionary and a
  SHA-256 manifest (`evolution/pt/exportacao-dados.md`).

### Changed
- Reorganized the legacy SVN directory structure.
- Removed obsolete `trunk`, `tags`, and `branches` hierarchy.
- Simplified Maven project organization.
- Improved language-pack build process.
- `numberrace-core` now depends on `org.xerial:sqlite-jdbc`.

### Fixed
- Build from a fresh clone: added the missing legacy JARs (jmat, nenya-media,
  samskivert) and fixed the unresolved `${project.parent.basedir}` paths in the
  core and language-pack POMs.
- Game no longer crashes on startup on Java 16+ (`SimpleFormatter` no longer uses
  the internal `sun.security.action` API).
- Corrected localized audio references from `.ogg` to `.wav`.
- Fixed WAV loading from compressed language-pack JARs using buffered
  streams.
- Fixed Portuguese localized resource loading.
- Improved diagnostics for missing audio resources.
