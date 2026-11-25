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

## Building / Desenvolvimento no Eclipse

Requisitos: Git, Eclipse IDE for Java Developers com m2e e JDK 21 (recomendado para a equipe; o código mantém compatibilidade de compilação Java 8). Maven 3.9.x para uso fora do Eclipse. A primeira construção exige acesso ao Maven Central.

1. Clone o repositório em qualquer pasta. Não use caminhos absolutos no projeto.
2. No Eclipse, configure o JDK em Window > Preferences > Java > Installed JREs.
3. File > Import > Maven > Existing Maven Projects. Selecione a subpasta `numberrace` do clone (a que contém o POM pai). Importe os 13 projetos padrão; não importe o editor Scala opcional.
4. No projeto `numberrace`, execute Run As > Maven build... com Goals `clean install` (ou use `NumberRace - Build.launch`). Não marque Skip Tests. Aguarde BUILD SUCCESS.
5. Selecione os projetos e execute Maven > Update Project (Alt+F5). Mantenha a resolução de dependências do workspace habilitada para execução Java.
6. Execute `NumberRace - Run.launch` (Run As > Java Application, ou abra `org.unicog.numberrace.Game` no Core e execute). Para depurar, use Debug As > Java Application.

Alternativa de terminal, a partir da pasta clonada:

```bash
mvn -f numberrace/pom.xml clean install
```

Os nove idiomas são dependências de runtime do Core. O Eclipse usa os projetos importados; o Maven usa os artefatos do reactor. Não é necessário copiar JARs para `target/classes/langs` ou para a pasta pessoal. Pacotes externos continuam disponíveis para idiomas não fornecidos pelo build. Os pacotes atuais têm prioridade. O Console informa a origem dos idiomas.

Os dados pessoais e de pesquisa continuam fora do Git, no diretório de aplicação do usuário (por padrão `~/NumberRace/v3`). Não apague esse diretório para atualizar o código.

O perfil opcional `deploy-local` continua disponível para instalar JARs de idioma para aplicações legadas. Não é necessário para desenvolvimento.

As três bibliotecas legadas são versionadas em `numberrace/legacy-maven-repository`; não copie bibliotecas para o repositório Maven pessoal manualmente. Não versione `target`, `.project`, `.classpath` ou `.settings`.

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
2. add it to the `languages` Maven aggregator and the Core runtime dependencies; add a unique marker and register the code in `GamePreferences`;
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

Check the Console for the marker URL, enable workspace dependency resolution, and run the full Maven build. The current classpath packs take priority over external packs.

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
