# Changelog

All notable changes to this maintained version of The Number Race
are documented here.

## Unreleased

### Added
- Brazilian Portuguese language pack.
- Maven reactor for language packs.
- Eclipse-oriented development workflow.

### Changed
- Reorganized the legacy SVN directory structure.
- Removed obsolete `trunk`, `tags`, and `branches` hierarchy.
- Simplified Maven project organization.
- Improved language-pack build process.

### Fixed
- Corrected localized audio references from `.ogg` to `.wav`.
- Fixed WAV loading from compressed language-pack JARs using buffered
  streams.
- Fixed Portuguese localized resource loading.
- Improved diagnostics for missing audio resources.
