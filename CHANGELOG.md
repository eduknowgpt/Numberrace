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
- Integrity check of the interaction data in the exporter
  (`org.unicog.numberrace.analytics.export.IntegrityChecker`): 21 rules checked in every exported
  session, reported as errors or warnings in `relatorio-integridade.txt`, in the `integrity`
  table and in the manifest (`evolution/pt/validacao-registros.md`).
- Tests of the interaction-data logging and export: deliberately damaged copies of a test
  session, recording failures (forced shutdown, locked database, disk full, clock change) and a
  generator of simulated sessions.
- Conformance tests of the interaction events against the official JSON Schema (test-scope
  dependency `com.networknt:json-schema-validator`), including a differential test with
  thousands of altered real events; regression tests with the sessions recorded by the real game
  (`src/test/resources/.../export/dados-reais/`); concurrency, round-trip (JSON and CSV) and
  robustness tests for unusual input files.

### Changed
- Reorganized the legacy SVN directory structure.
- Removed obsolete `trunk`, `tags`, and `branches` hierarchy.
- Simplified Maven project organization.
- Improved language-pack build process.
- `numberrace-core` now depends on `org.xerial:sqlite-jdbc`.
- Interaction event model 1.1: `PAUSE` and `RESUME` may have `trigger = window_focus` (the game
  pauses when its window loses focus); schema 1.0 kept in `modelo-eventos-1.0.schema.json`.

### Fixed
- Interaction-data logging: on timeout, `ANSWER_TIMEOUT` is recorded before the opponent's
  remark; `REWARD_SAVED.reward_type` ranges from 1 to 7 as in the model; `SESSION_START.display_mode`
  is filled; after a write interrupted mid-line, the next event starts on a new line.
- Interaction-data export: an event with `game_number` or `turn_number` below 1, or with an unknown
  top-level field, is reported as invalid instead of preventing the SQLite database of the whole
  package; a corrupted line with deeply nested JSON is reported as invalid instead of stopping the
  export; a missing input folder is reported with its path.
- Interaction-data logging: `identification/participants.csv` is still recognized after being saved
  by Excel in Portuguese (semicolon separator, Windows-1252 encoding), so a participant keeps the
  same code; a failing destination no longer stops the writing to the other destinations.
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
