# The Number Race — Maintained and Extended Version

This repository contains a maintained and extended version of **The Number Race**, an open-source educational game designed to support the development of number sense and basic numerical cognition.

This codebase is derived from the original **The Number Race** project. It preserves the multilingual architecture and core gameplay while introducing maintenance fixes, Brazilian Portuguese support, build improvements, and incremental modernization.

> **Project status:** active maintenance and development. The goal is to preserve the behavior and educational value of the original application while improving maintainability, compatibility, performance, and extensibility.

## About the Original Project

**The Number Race** is an educational game originally developed to support numerical cognition and mathematics learning.

This repository is **not the original upstream repository**. It is a derived and maintained version based on the GPL-licensed source code of The Number Race.

The original authors and contributors retain copyright over their respective contributions. Subsequent modifications and additions belong to their respective contributors.

For provenance, attribution, and licensing information, see `NOTICE.md` and `LICENSE`.

## Goals of This Repository

The main goals of this maintained version are to:

- preserve the original functionality and multilingual nature of The Number Race;
- maintain a reproducible and understandable source-code distribution;
- simplify the legacy project structure without unnecessarily changing its architecture;
- improve Maven and Eclipse development workflows;
- add and maintain Brazilian Portuguese support;
- correct legacy resource and audio-loading issues;
- improve performance and responsiveness;
- modernize the Java codebase incrementally;
- make the project easier to extend with new educational functionality;
- preserve compatibility with existing language packs whenever practical.

Modernization is intentionally incremental. Functional behavior is kept as the baseline while legacy components are reviewed, tested, and replaced where appropriate.

## Project Structure

```
numberrace/
├── pom.xml
├── README.md
├── LICENSE
├── NOTICE.md
├── CHANGELOG.md
├── numberrace-core/
├── numberrace-res/
├── languages/
│   ├── de/
│   ├── el/
│   ├── en/
│   ├── es/
│   ├── fi/
│   ├── fr/
│   ├── nl/
│   ├── pt/
│   └── sv/
├── tools/
│   └── language-editor/
└── legacy-maven-repository/
```

The former SVN-oriented `trunk`, `branches`, and `tags` hierarchy is not part of the active source tree. Maven-generated `target/` directories are excluded from version control.

### Main Modules

- **`numberrace-core`** — application code and game logic.
- **`numberrace-res`** — shared, language-independent resources such as images, effects, and music.
- **`languages`** — Maven aggregator for the language packs.
- **`languages/<locale>`** — localized text, messages, instructions, and audio resources.
- **`tools/language-editor`** — optional language-related tooling.
- **`legacy-maven-repository`** — legacy dependencies required by the current codebase.

## Brazilian Portuguese Support

This maintained version adds **Brazilian Portuguese (`pt-BR`) support** while preserving the language packs inherited from the original project.

The Portuguese localization is maintained under:

```
languages/pt/
```

It includes, as applicable:

- translated interface messages;
- localized instructions and informational texts;
- Brazilian Portuguese audio resources;
- integration with the existing Number Race language-pack mechanism.

The Portuguese pack follows the same architecture as the other language packs and is packaged as a JAR during the Maven build.

### Localized Audio

Maintenance work identified a legacy inconsistency in the resource configuration: several localized audio resources were referenced as `.ogg` although the corresponding language-pack files are `.wav`.

The maintained version therefore distinguishes between:

- **localized speech and instructions**, which may be stored as `.wav`; and
- **shared effects and music**, which may remain `.ogg`.

Another compatibility issue occurs when WAV files are loaded from compressed language-pack JARs. Streams obtained from JAR entries do not necessarily support `mark/reset`, while Java Sound may require this capability during audio-format detection. The maintained audio-loading code buffers such streams before passing them to Java Sound.

These fixes allow localized WAV audio and the original shared OGG resources to coexist without changing the screen-level sound API.

## Supported Languages

| Code | Language |
| --- | --- |
| `de` | German |
| `el` | Greek |
| `en` | English |
| `es` | Spanish |
| `fi` | Finnish |
| `fr` | French |
| `nl` | Dutch |
| `pt` | Brazilian Portuguese |
| `sv` | Swedish |

Availability and completeness of individual text and audio resources may vary among language packs inherited from the original project.

## Building

### Requirements

To build the project, you need:

- a Java Development Kit (JDK);
- Apache Maven;
- Git, when cloning the repository;
- Eclipse IDE with Maven/M2E support, optionally.

The Number Race is a legacy Java application and still depends on older libraries. Until modernization establishes a formally supported Java baseline, use a JDK version known to work with the current project and document newly validated environments.

### Clone the Repository

```bash
git clone <repository-url>
cd <repository-directory>
```

Replace the placeholders with the actual repository URL and directory name.

### Build with Maven

From the repository root:

```bash
mvn clean package
```

The root POM acts as a Maven reactor and builds the application modules and language packs in the required order.

A successful build should finish with:

```
BUILD SUCCESS
```

Generated artifacts are written to the corresponding `target/` directories. These directories are build output and should **not** be committed to Git.

### Language-Pack Deployment

Language packs are generated as JAR files during the Maven build.

During development, ensure that the application is actually loading the newly generated language pack. An older installed JAR may otherwise take precedence over recently modified resources.

The application may use an external language directory such as:

```
${user.home}/NumberRace/v3/langs/
```

When diagnosing localization issues, verify both the newly generated language-pack JAR and the JAR actually loaded by the running application.

If the current POM provides the `deploy-local` profile, local deployment can be performed with:

```bash
mvn package -Pdeploy-local
```

## Importing into Eclipse

The recommended Eclipse workflow uses Maven/M2E:

1. Open **File → Import**.
2. Select **Maven → Existing Maven Projects**.
3. Select the repository root directory.
4. Import the root project and detected Maven modules.
5. Run **Maven → Update Project**.
6. If necessary, enable **Force Update of Snapshots/Releases**.

To perform a complete build inside Eclipse:

1. Right-click the root Maven project.
2. Select **Run As → Maven build...**
3. Enter `clean package`.
4. Run the build and confirm `BUILD SUCCESS`.

## Running from Eclipse

After a successful build, locate the main application class in `numberrace-core`:

```
org.unicog.numberrace.Game
```

Right-click `Game.java` and select **Run As → Java Application**.

If the game starts but a localized resource is missing or outdated, first verify which language-pack JAR is being loaded at runtime.

## Working with Language Packs

Each language is maintained independently under `languages/`.

For example:

```
languages/pt/src/main/resources/resources/pt/
```

contains Portuguese-specific resources, while shared resources belong in `numberrace-res`.

When modifying a language pack:

1. edit the source under `languages/<locale>/src/main/resources`;
2. rebuild the project;
3. verify the generated JAR;
4. ensure that the runtime resolves the updated language pack;
5. restart the application when necessary.

Do not edit generated files under `target/`, because Maven replaces them on subsequent builds.

### Adding a New Language

A new language should follow the existing language-pack architecture:

1. create a module under `languages/<locale>`;
2. add it to the `languages` Maven aggregator;
3. provide the corresponding message bundle and localized resources;
4. package the language as a JAR;
5. test textual localization, resource lookup, and audio independently.

## Resource Organization

As a general rule:

```
numberrace-res
    shared images, music, effects, and common assets

languages/<locale>
    translated messages, localized texts, speech, and instructions
```

Resource paths are part of the runtime contract. Renaming or moving an asset therefore requires updating its resource mapping and testing the packaged application, not only the source-tree version.

## Development and Modernization

The project contains legacy code and dependencies. Refactoring should favor small, testable changes over large rewrites.

Current and planned modernization areas include:

- resource and image caching;
- reduced logging on performance-critical paths;
- asynchronous loading where appropriate;
- improved audio-resource handling;
- better diagnostics for missing resources;
- removal of obsolete APIs;
- Maven configuration cleanup;
- dependency review;
- establishment of a supported Java baseline;
- automated resource-integrity tests;
- incremental performance improvements;
- new educational functionality.

A modernization change should preserve observable game behavior unless the change intentionally modifies a feature.

## Troubleshooting

### A localized change does not appear

The application may be loading an older language-pack JAR from its external language directory. Check the runtime language-pack location, rebuild the language module, and redeploy the updated JAR.

### A localized sound is not played

Check:

1. whether the resource mapping uses the actual extension (`.wav` or `.ogg`);
2. whether the file exists inside the generated language-pack JAR;
3. whether the application is loading the expected JAR;
4. whether Java Sound can decode the resource.

### `mark/reset not supported`

This usually means Java Sound received a stream directly from a compressed JAR entry. The audio-loading layer should wrap the stream in a `BufferedInputStream` before calling `AudioSystem.getAudioInputStream(...)`.

### A shared OGG resource is missing

Shared music and sound effects belong to `numberrace-res`. Verify both:

```
numberrace-res/src/main/resources/
```

and:

```
numberrace-res/target/classes/
```

before treating the problem as a language-pack issue.

## Contributing

Contributions are welcome, particularly in:

- localization and translation review;
- accessibility;
- compatibility fixes;
- automated tests;
- performance improvements;
- documentation;
- educational features;
- careful modernization of legacy components.

When contributing:

- preserve existing copyright and license notices;
- do not commit generated `target/` directories;
- document behavior-changing modifications;
- keep language-specific assets in the appropriate language pack;
- test existing functionality after refactoring;
- add attribution for newly created assets where appropriate.

A dedicated `CONTRIBUTING.md` can be added as the contribution workflow evolves.

## Changelog

Significant changes to this maintained version should be recorded in `CHANGELOG.md`.

Git history provides implementation-level detail; the changelog should provide a concise, human-readable summary of notable additions, fixes, localization work, compatibility changes, and performance improvements.

## License and Attribution

This repository is derived from **The Number Race** and preserves the licensing and copyright notices applicable to the original project.

The original source was distributed under the **GNU General Public License version 2 (GPLv2)**. See `LICENSE` for the license text and `NOTICE.md` for project provenance and attribution.

Copyright in original source code and assets remains with the respective original copyright holders. Copyright in subsequent modifications and newly created material remains with the respective contributors, subject to the project's applicable license terms.

Do not remove existing copyright, authorship, or licensing notices from inherited source files or assets.

## Acknowledgments

This maintained version builds upon the work of the original Number Race authors and contributors, including contributors to its translations, educational resources, artwork, audio, and supporting libraries.

Their work provides the foundation for the continued maintenance and extension of the project.
