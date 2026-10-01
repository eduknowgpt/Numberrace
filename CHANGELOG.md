# Changelog

All notable changes to this maintained version of The Number Race
are documented here.

## Unreleased

### Added
- Brazilian Portuguese language pack.
- Validated development environment guide (`numberrace/docs/AMBIENTE.pt.md`).
- Maven reactor for language packs.
- Eclipse-oriented development workflow.

### Changed
- Reorganized the legacy SVN directory structure.
- Removed obsolete `trunk`, `tags`, and `branches` hierarchy.
- Simplified Maven project organization.
- Improved language-pack build process.

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
