# Dados de interação do Number Race: registro de trabalho

## 1. Identificação

**Projeto:** Modelagem, registro e persistência de dados de interação no Number Race
(Projeto Evolução ADS, frentes C — Registro de eventos e D — Persistência)
**Estudante:** Sávio (Análise e Desenvolvimento de Sistemas)
**Início:** 30/09/2026
**Plano:** 8 etapas, carga prevista de 97 h
**Produto principal:** componente de registro e exportação dos dados de interação

Este documento registra, etapa por etapa, **o que foi feito, o que foi encontrado e quais
decisões foram tomadas**, para apoiar a pesquisa e a continuidade do projeto. Ele é
atualizado ao final de cada etapa.

---

## 2. Etapa 1 — Ambientação e preparação do ambiente (concluída em 01/10/2026)

**Produto previsto no plano:** ambiente configurado, software executado e documentação
inicial do funcionamento do Number Race.

### 2.1 Atividades realizadas

| Atividade do plano | O que foi feito |
|---|---|
| Conhecer os objetivos e o escopo do projeto | Leitura do plano de atividades e definição do escopo: registro, persistência, exportação e testes com dados simulados. Fora do escopo: estudos com crianças, diagnóstico, estatística avançada e IA |
| Leitura da documentação disponível | `README.md`, `CHANGELOG.md`, `NOTICE.md`, `numberrace/docs/REFACTORING.pt.md` e `evolution/pt/` (README, evolução ADS e TCC, reestruturação, referências científicas) |
| Conhecer a arquitetura e a organização do código | Estudo dos pacotes de `numberrace-core` e do caminho de uma jogada no código (seção 2.4) |
| Executar a versão atual do software | Jogo executado no Windows: primeiro com um pacote montado a partir do código, depois compilado e executado pelo Eclipse |
| Identificar os principais fluxos de interação | Fluxo de telas e de uma rodada descritos a partir do uso do jogo e conferidos no código (seção 2.5) |
| Configurar o ambiente de desenvolvimento | Windows + JDK 21 (Temurin) + Eclipse IDE 2026-09 + GitHub Desktop. Detalhes em `numberrace/docs/AMBIENTE.pt.md` |
| Configurar o repositório e o versionamento | Fork `saviosant0s/Numberrace`, ramo `correcao-build` e Pull Request #1 para este repositório |
| Registrar o ambiente e os procedimentos | `numberrace/docs/AMBIENTE.pt.md` e este documento |

### 2.2 Problemas que impediam compilar ou executar (corrigidos neste Pull Request)

| # | Problema | Como foi identificado | Correção |
|---|---|---|---|
| 1 | Os JARs `jmat-5.0-nmi`, `nenya-media-rev407` e `samskivert-2272.1` não estavam no repositório (só os `.sha1`/`.md5`) | `mvn clean package` num clone novo: `Could not find artifact fi.nmi:jmat:jar:5.0-nmi` | JARs obtidos do SVN original (`svn.code.sf.net/p/numberrace/svn/NumberRace/trunk/mvn_repo`); SHA-1 conferido com os `.sha1` do repositório |
| 2 | `${project.parent.basedir}` não é resolvida pelo Maven na URL do repositório local (`numberrace-core/pom.xml`) nem na cópia dos idiomas (`languages/*/pom.xml`) | Mensagem do Maven com o texto literal da variável; idiomas copiados para `languages/<idioma>/numberrace-core/...` | `${project.baseUri}../legacy-maven-repository` e `${project.basedir}/../../numberrace-core/...` |
| 3 | `SimpleFormatter` usava a API interna `sun.security.action.GetPropertyAction` | `IllegalAccessError` ao abrir o jogo no Java 21; `BUILD FAILURE` em `SimpleFormatter.java:[24,101]` no JDK do Eclipse 2026-09 | `System.getProperty("line.separator")`. O jogo deixou de precisar de `--add-exports` |

Validação: compilação a partir de um cache vazio do Maven (`BUILD SUCCESS`), compilação
com `-Dmaven.compiler.release=8` e execução pelo Eclipse no Windows.

### 2.3 Observações registradas (não alteradas)

| # | Observação | Evidência | Relação com este projeto |
|---|---|---|---|
| 4 | Em máquina sem dispositivo de áudio, `SoundPlayer` lança `IllegalArgumentException` após a escolha do idioma (`getMaxSimultaneousSounds` retorna 0 e o `ThreadPoolExecutor` é criado com 0 threads) | Execução num servidor sem placa de som | Impede testes automatizados da interface em servidores |
| 5 | O registro atual grava dados pessoais: `AllStudentsList.txt` (sobrenome, nome, idade, sexo, turma) e arquivos de rodada nomeados `<sobrenome>_<nome>_<sessão>_Data.txt` | `data/Student.java` e `data/DataFileHandler.java` | O novo componente deve usar identificador **pseudonimizado** e não repetir esse padrão |
| 6 | O cabeçalho de `AllStudentsList.txt` tem vírgulas faltando entre os blocos (`gamesPlayedlev1char1`, `lev1char6lev2char1`…), então o número de colunas do cabeçalho difere do dos dados | `Student.getHeaders()` | Mostra a necessidade de um modelo de dados definido e validado (Etapas 3 e 7) |
| 7 | Os áudios do pacote `pt` estão gravados em inglês (textos traduzidos, voz não) | Transcrição automática dos 92 `.wav` com modelos de inglês e português (Vosk): 80 claramente em inglês, 11 números curtos provavelmente em inglês, 1 indefinido, nenhum claramente em português | Nenhuma. Confirmado pelo professor: os áudios serão produzidos pelos alunos do TCC |
| 8 | `p2far.wav` e `o2far.wav` (aviso de "casas demais") são referenciados em `resources.properties` mas não existem no pacote `pt`; nessa situação o jogo fica em silêncio | Comparação das 59 referências de voz com os arquivos do pacote | Exemplo de verificação de integridade de recursos (Frente E) |
| 9 | Cliques antes do segundo estímulo aparecer são ignorados sem aviso. É intencional (comentário `//don't accept responses 'till now!` em `NumCompManager.showRightStims`) | Percebido jogando; confirmado no código | O tempo de resposta e os cliques ignorados precisam ser considerados no modelo de eventos (seção 2.6) |
| 10 | A pasta `numberrace/.metadata/` (workspace do Eclipse) está versionada. O `.gitignore` da raiz ignora apenas `/.metadata/` na raiz | O GitHub Desktop passou a mostrar alterações nesses arquivos depois do uso no Eclipse | Coincide com o item 4 dos "próximos passos" de `reestruturacao-projeto.md` |
| 11 | A lista de idiomas mostra um item "gs", de origem ainda não identificada | Janela "Choose your language" | A investigar |

### 2.4 Organização do código relevante para este projeto

| Elemento | Papel |
|---|---|
| `Game.main` | Ponto de entrada: preferências, escolha do idioma e criação do `GameObject` |
| `GameObject` (`GameStates`, `changeState`) | Máquina de estados das telas: `TITLE`, `REGISTRATION`, `THEME`, `INSTRUCTIONS`, `CHARACTERS`, `CHOICE`, `GAMEOVER`, `SAVE`, `ZOO`… |
| `screens/ChoiceScreen` | Tabuleiro: escolha do lado (`choiceIS`), clique no personagem (`stampClicked`) e nas casas (`sqClicked`) |
| `listener/GameListener` | Interface entre tela e lógica: `gameBegins`, `turnBegins`, `imFast`, `successfullSneak`, `playerWins` |
| `managers/NumCompManager` | Implementa `GameListener` e conduz a rodada: estímulos, tempo de resposta, acerto, envio ao algoritmo (`addTrial`) e gravação do registro atual |
| `algorithms/NumCompAlgManager`, `NewMultiDimAlg`, `AdapDimensions` | Algoritmo adaptativo em 3 dimensões: velocidade (prazo de 10 s até 0,25 s), distância numérica e notação |
| `algorithms/GameTurn` | Estrutura de uma rodada (números, lado escolhido, acerto, tempo, dificuldades, prazo) |
| `resources/.../algorithms/ccl.properties` | 22 níveis de complexidade conceitual: tamanho do tabuleiro, representações (pontos, fala, algarismos), faixa numérica, armadilhas, adição e subtração |
| `data/DataFileHandler`, `data/Student` | Registro atual (cerca de 60 colunas por rodada) |

**Pontos de integração identificados para o futuro componente:** os cinco métodos de
`GameListener` e `GameObject.changeState`. Usá-los permite registrar eventos alterando pouco
a lógica do jogo, conforme a seção 11 de `evolucao-ads.md`.

### 2.5 Fluxos de interação observados

**Telas:** título → cadastro → escolha do mundo (selva ou fundo do mar) → instruções →
escolha do personagem → partida → fim de partida → se venceu: salvar o prisioneiro →
coleção de animais → escolha do mundo.

**Rodada:** dois estímulos (o direito aparece 1,2 s após o esquerdo) → escolha do maior →
o jogador move o próprio personagem pelo número escolhido e o do adversário pelo outro →
armadilhas (recuo sorteado de 1 a 3 casas) → próxima rodada. Em rodadas com prazo
("Escolha rápido!"), se a criança não responder a tempo, o adversário fica com o maior e a
resposta é registrada como errada (`successfullSneak`).

**Recompensas:** cada vitória salva um animal. Um de cada um dos 7 tipos libera o próximo
personagem (6 por mundo).

### 2.6 Implicações para as próximas etapas

- **Tempo de resposta:** o jogo mede desde a apresentação do estímulo **esquerdo**
  (`NumCompManager.runTurn`), mas a resposta só é aceita depois do **direito**, cerca de
  1,2 s depois. O modelo de eventos deve registrar os instantes separadamente para que o
  tempo de resposta tenha definição explícita.
- **Cliques ignorados:** hoje não deixam registro; avaliar um evento próprio.
- **Pseudonimização:** o identificador do participante não deve conter nome nem dados
  pessoais (observação 5).
- **Eventos candidatos já identificados:** início e fim de sessão e de partida, início de
  rodada, estímulo apresentado (valores e notação), resposta (lado, acerto, tempo), resposta
  fora do prazo, movimentos no tabuleiro, armadilha, fim de partida, recompensa, personagem
  liberado e ajuste de dificuldade.

---

## 3. Próximas etapas

| Etapa | Descrição | Situação |
|---|---|---|
| 2 | Levantamento dos eventos produzidos pelo jogo (catálogo preliminar) | próxima |
| 3 | Modelagem dos eventos de interação | — |
| 4 | Estratégia de persistência | — |
| 5 | Desenvolvimento do componente de registro | — |
| 6 | Exportação e disponibilização dos dados | — |
| 7 | Testes, validação e integridade dos registros | — |
| 8 | Documentação e consolidação | — |

---

## Nota sobre o desenvolvimento

Este trabalho é feito por Sávio, com apoio de uma ferramenta de inteligência artificial
(Claude, da Anthropic) na análise do código, na preparação das correções e na redação da
documentação. As alterações são acompanhadas e testadas por ele.
