# Ambiente de desenvolvimento validado

Este documento registra um ambiente em que o Number Race foi **compilado e executado com
sucesso** a partir de um clone novo do repositório, e o procedimento usado. Complementa as
seções *Building*, *Importing into Eclipse* e *Running from Eclipse* do `README.md`.

## Ambientes validados

| Data | Sistema | Java | Ferramentas | Resultado |
|---|---|---|---|---|
| 01/10/2026 | Windows | Eclipse Temurin JDK 21 | Eclipse IDE for Java Developers 2026-09 (M2E), GitHub Desktop | `BUILD SUCCESS`; jogo executado pelo Eclipse |
| 01/10/2026 | Linux | OpenJDK 21 | Apache Maven 3.9.11 (linha de comando) | `BUILD SUCCESS`; jogo executado a partir de `numberrace-core/target/classes` |

O código continua compilado para **Java 8** (`maven.compiler.source/target = 8`). Um JDK mais
novo pode ser usado para compilar.

## Procedimento no Windows

### 1. Instalar

- **JDK:** Eclipse Temurin 21 (https://adoptium.net), instalador `.msi`, com a opção
  *Add to PATH*.
- **Eclipse:** *Eclipse IDE for Java Developers* (https://www.eclipse.org/downloads/), que já
  inclui o Maven (M2E) e o Git (EGit).
- **Git:** GitHub Desktop (https://desktop.github.com) ou Git for Windows.

### 2. Clonar

Clonar o repositório (ou um fork) e usar a pasta interna `numberrace/`, onde está o
`pom.xml` principal.

### 3. Importar no Eclipse

1. **File → Import… → Maven → Existing Maven Projects**.
2. *Root Directory*: a pasta `numberrace/` do clone.
3. Desmarcar `tools/language-editor`, que é opcional e fica no perfil `tools`.
4. **Finish** e aguardar o download das dependências.

### 4. Compilar

Botão direito no projeto `numberrace` → **Run As → Maven build…** → *Goals*: `clean package` →
**Run**. Resultado esperado: `BUILD SUCCESS`.

Durante o `package`, cada pacote de idioma é copiado para
`numberrace-core/target/classes/langs/`. É ali que o jogo procura os idiomas quando é
executado pelo Eclipse.

Depois do build, atualizar o projeto no Eclipse (**F5**).

### 5. Executar

`numberrace-core` → `src/main/java` → `org.unicog.numberrace.Game` → botão direito →
**Run As → Java Application**. Na primeira execução o jogo pede o idioma.

Os dados das partidas ficam em `%USERPROFILE%\NumberRace\v3\Data`.

## Problemas encontrados e soluções

| Sintoma | Causa | Solução |
|---|---|---|
| `Could not find artifact fi.nmi:jmat:jar:5.0-nmi` (e `nenya-media`, `samskivert`) | Os `.jar` não estavam no repositório, só os `.sha1`/`.md5` | `.jar` adicionados a `legacy-maven-repository/`, obtidos do SVN original (`svn.code.sf.net/p/numberrace/svn/NumberRace/trunk/mvn_repo`), com SHA-1 idêntico ao registrado |
| Mesmo com os `.jar`, o Maven não os encontra; a URL aparece como `file://${project.parent.basedir}/...` | A variável `${project.parent.basedir}` não é resolvida na seção `<repositories>` | `numberrace-core/pom.xml` usa `${project.baseUri}../legacy-maven-repository` |
| Pacotes de idioma copiados para `languages/<idioma>/numberrace-core/...` | Mesma variável no `maven-antrun-plugin` | `languages/*/pom.xml` usam `${project.basedir}/../../numberrace-core/...` |
| `IllegalAccessError … sun.security.action.GetPropertyAction` ao abrir (Java 16+), ou falha de compilação em `SimpleFormatter.java:[24,101]` em JDKs mais novos | Uso de API interna do JDK | `SimpleFormatter` usa `System.getProperty("line.separator")` |
| `Unrecognized option: --add-exports` | O Eclipse executou o jogo com um Java 8 instalado no computador | Não usar `--add-exports` (desnecessário após a correção acima) |
| Em máquina **sem dispositivo de áudio**, `IllegalArgumentException` em `SoundPlayer` após escolher o idioma | `getMaxSimultaneousSounds` retorna 0 | Não corrigido. Não ocorre em computadores comuns |

## Nota sobre o desenvolvimento

Este trabalho foi feito por Sávio, como parte da ACC "Modelagem, registro e persistência de
dados de interação no Number Race", com apoio de uma ferramenta de inteligência artificial
(Claude, da Anthropic) na análise do código, na preparação das correções e na redação desta
documentação. As alterações foram acompanhadas por ele e testadas no ambiente Windows
descrito acima.
