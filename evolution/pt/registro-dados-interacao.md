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

### 2.2 Problemas que impediam compilar ou executar (corrigidos no Pull Request #1)

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

## 3. Etapa 2 — Levantamento dos eventos (concluída em 01/10/2026)

**Produto previsto no plano:** catálogo preliminar de eventos de interação.
**Produto:** [`catalogo-eventos.md`](catalogo-eventos.md).

### 3.1 Atividades realizadas

| Atividade do plano | O que foi feito |
|---|---|
| Identificação dos fluxos de interação | Fluxos da Etapa 1 revisados e conferidos jogando |
| Levantamento dos eventos existentes no código | 25 eventos localizados (arquivo e linha), em 4 grupos: sessão e navegação, partida, rodada e pós-partida |
| Identificação das informações de cada evento | Informações disponíveis em cada ponto do código, comparadas com o registro atual |
| Classificação entre dados observados e derivados | Três tipos: observado, contexto e derivado. Todas as colunas do registro atual foram classificadas |
| Identificação de eventos ausentes ou insuficientes | 10 lacunas do registro atual (catálogo, seção 5) |
| Organização do catálogo de eventos | `catalogo-eventos.md`, com nomes preliminares e correspondência com os eventos candidatos de `evolucao-ads.md` |

### 3.2 Conferência jogando

- O jogo **facilita** após erros propositais: num arquivo de teste, a rodada 1 estava no nível 9
  (algarismos e subtração) e a 4 já no nível 1 (pontos, fala e algarismos até 3).
- Rodada com prazo sem resposta: o adversário fica com o maior e o registro grava erro com
  `RT = 0`. Rodadas sem prazo aguardam a resposta indefinidamente.
- Armadilha observada no nível Intermediário, com recuo de 3 casas (penalidade sorteada de 1 a 3).
- O nível inicial no cadastro é **Fácil, Intermediário ou Difícil** (níveis 1, 8 e 14 de
  `ccl.properties`), e não "de 1 a 14", como diz o texto de ajuda da tela de cadastro.
- Teclas: F8 (pausa), Esc (menu), Shift+Esc (encerrar).

### 3.3 Novas observações

| # | Observação | Relação com este projeto |
|---|---|---|
| 12 | No arquivo de dados por rodada, os **valores têm uma coluna a mais que o cabeçalho**: a configuração da representação grava 10 valores (inclui `boardLength`), mas o cabeçalho tem 9 nomes. A partir de `controlFor`, as colunas ficam deslocadas | O registro atual não pode ser lido corretamente numa planilha. Reforça a necessidade de um modelo de dados definido e validado |
| 13 | Há **dois critérios de acerto**: `respCorr` (escolheu o maior) e `finalCorr` (escolha vantajosa considerando armadilhas e colisões). O algoritmo adaptativo usa `finalCorr` | Ambos serão registrados e identificados como derivados (decisão D4) |
| 14 | Não existe ajuda à criança durante a partida | O evento candidato `HELP_REQUESTED` não se aplica |

### 3.4 Decisões

As quatro decisões de escopo (cliques antes da liberação, granularidade dos movimentos, estado
do algoritmo e registro do acerto) estão na seção 6 do catálogo, com a justificativa. Podem ser
revistas pelo orientador.

---

## 4. Etapa 3 — Modelagem dos eventos de interação (concluída em 02/10/2026)

**Produto previsto no plano:** especificação do modelo de eventos de interação.
**Produto:** [`modelo-eventos.md`](modelo-eventos.md), acompanhado do schema
[`modelo-eventos.schema.json`](modelo-eventos.schema.json) e de uma sessão simulada de exemplo
([`modelo-eventos-exemplos.json`](modelo-eventos-exemplos.json)).

### 4.1 Atividades realizadas

| Atividade do plano | O que foi feito |
|---|---|
| Definição dos campos dos eventos | Parte comum a todos os eventos (11 campos) e parte específica de 23 tipos de evento, com a classificação em observado, contexto ou derivado e a correspondência com as colunas do registro atual |
| Definição dos tipos de dados | Dez tipos (texto, categoria, identificador, data e hora, inteiro, real, lógico, nulo, objeto e lista) e convenções para nomes, unidades e data e hora |
| Definição de identificadores | UUID para evento e sessão; código pseudonimizado para o participante; números de partida, de rodada e de sequência |
| Modelagem de sessão e eventos | Hierarquia participante, sessão, partida, rodada e evento; sequência típica de uma rodada, com a origem de cada evento no código |
| Análise de extensibilidade do modelo | Versionamento no formato MAIOR.MENOR, leitura tolerante a campos desconhecidos e pontos de extensão previstos |
| Documentação do modelo | `modelo-eventos.md` e o modelo em JSON Schema, validado com a sessão de exemplo |

### 4.2 Principais resultados

- O tempo de resposta passa a ser medido **desde a liberação da resposta**. O tempo contado
  desde o primeiro estímulo, equivalente à coluna `RT` atual, também é registrado, para
  permitir a comparação com dados antigos.
- O prazo esgotado passa a ter **evento próprio** (`ANSWER_TIMEOUT`), em vez de `RT = 0`.
- O participante é identificado apenas por **código pseudonimizado**; o nome não aparece nos
  dados.
- Cada valor é gravado **com o seu nome**, o que elimina o problema das colunas deslocadas
  (observação 12).
- Estímulo, resposta e resultado são **eventos separados**, porque ocorrem em instantes
  diferentes; a visão com uma linha por rodada será produzida na exportação (Etapa 6).
- **Validação:** os 44 eventos da sessão de exemplo são válidos; oito eventos com erros
  propositais (campo ausente, tipo inexistente, valor negativo, nome de campo errado, entre
  outros) foram rejeitados.
- **Conferência com dados reais:** uma rodada de um arquivo de teste do jogo atual (prazo
  esgotado) foi convertida para o modelo e validada. A conferência mostrou que as armadilhas
  permanecem no tabuleiro entre rodadas, mesmo quando o nível da rodada não prevê armadilhas, e
  o modelo foi ajustado para registrar todas as armadilhas presentes, com a respectiva
  penalidade (modelo, seção 10).

### 4.3 Decisões

As dez decisões de modelagem (D5 a D14) estão na seção 13 do modelo, com a justificativa. Entre
elas: o estilo dos nomes de campos segue a tabela "Modelo inicial" do plano (*snake_case*, como
`event_id`), e não o exemplo de `evolucao-ads.md` (*camelCase*, como `responseTimeMs`) (D6).
Podem ser revistas pelo orientador.

---

## 5. Etapa 4 — Estratégia de persistência (concluída em 03/10/2026)

**Produto previsto no plano:** especificação da estratégia de persistência dos eventos.
**Produto:** [`persistencia-eventos.md`](persistencia-eventos.md), acompanhado da estrutura do
banco SQLite ([`persistencia-eventos.sql`](persistencia-eventos.sql)).

### 5.1 Atividades realizadas

| Atividade do plano | O que foi feito |
|---|---|
| Levantamento de alternativas de persistência | Seis alternativas: JSON Lines, arquivo JSON único, CSV, SQLite, banco em servidor ou nuvem e serialização Java |
| Comparação das alternativas | Prova de conceito em Java 8 com JSON Lines e SQLite (tempo, tamanho, interrupção forçada, falha do driver), estimativa de volume e comparação por critério |
| Análise de portabilidade e interoperabilidade | Formatos abertos, leitura por outras linguagens e ferramentas, consultas em SQL sobre o JSON |
| Definição da estratégia de armazenamento | Arquitetura com JSON Lines como registro oficial e SQLite como cópia para consulta |
| Especificação da estrutura de persistência | Pastas, nomes de arquivos, formato do JSON Lines, tabelas do banco, comportamento em falhas, importação e coleta |
| Documentação da decisão técnica | `persistencia-eventos.md`, com as decisões D15 a D23 |

### 5.2 Principais resultados

- As duas alternativas finalistas funcionaram em Java 8 e resistiram a interrupções forçadas do
  processo durante a escrita. O SQLite não abre quando a biblioteca nativa do driver não pode
  ser extraída.
- Volume estimado de cerca de 400 eventos por sessão (rodadas de cerca de 50 s num teste do
  jogo), o que não restringe a escolha.
- **Decisão:** cada evento é gravado primeiro num arquivo JSON Lines por sessão, que é o registro
  oficial, e depois copiado para um banco SQLite, usado para consultas. Se o SQLite falhar, a
  importação completa o banco a partir dos arquivos, sem perda de dados.
- A identificação dos participantes fica fora da pasta de dados de interação, e o registro atual
  do jogo é mantido sem alteração.

### 5.3 Decisões

As nove decisões desta etapa (D15 a D23) estão na seção 12 de `persistencia-eventos.md`, com a
justificativa. Podem ser revistas pelo orientador.

---

## 6. Etapa 5 — Desenvolvimento do componente de registro (concluída em 09/10/2026)

**Produto previsto no plano:** componente funcional de registro estruturado das interações.
**Produto:** pacote `org.unicog.numberrace.analytics` no `numberrace-core`, descrito em
[`componente-registro.md`](componente-registro.md).

### 6.1 Atividades realizadas

| Atividade do plano | O que foi feito |
|---|---|
| Implementação da estrutura básica do componente | Pacote `analytics` com 10 classes: adaptador do jogo, núcleo do registro, evento, tipos, validação, JSON, gravação em JSON Lines e em SQLite e registro de participantes |
| Implementação do registro de sessões | Sessão aberta no cadastro ou seleção do aluno e encerrada na troca de aluno ou no fechamento do jogo, com o motivo; código pseudonimizado do participante |
| Implementação do registro de eventos | Os 23 tipos do modelo 1.0, com numeração por sessão e contexto de partida e rodada |
| Integração com os fluxos do jogo | Chamadas de uma linha em 7 classes do jogo (seção 5 do documento), incluindo um ponto novo para os cliques antes da liberação da resposta |
| Validação dos dados antes da persistência | Regras de estrutura do JSON Schema da Etapa 3, aplicadas a cada evento antes da gravação; o tipo e o valor de cada campo são conferidos contra o schema nos testes automatizados |
| Tratamento de situações de erro | Nenhuma exceção chega ao jogo; falhas do SQLite e do JSON Lines tratadas conforme a Etapa 4 |
| Refatoração e organização do código | Código do registro concentrado num pacote próprio; alterações mínimas nas classes do jogo; 11 testes automatizados |

### 6.2 Principais resultados

- O jogo passou a gravar os eventos no formato do modelo 1.0, em JSON Lines (registro oficial) e
  em SQLite (cópia para consulta), sem alterar o registro atual.
- A gravação ocorre em segundo plano e não interrompe o jogo; falhas do registro aparecem apenas
  no log.
- **Teste em ambiente automatizado:** 35 eventos de 17 tipos, em duas sessões, todos válidos pelo
  JSON Schema e pelas regras de consistência, sem lacunas na sequência e com as mesmas 35 linhas
  no SQLite.
- **Teste no Windows** (Eclipse, Java 8 de 32 bits): uma sessão com duas partidas, 180 eventos de
  21 tipos, todos válidos, sem lacunas na sequência e com as mesmas 180 linhas no SQLite. Os tipos
  de fim de partida, prazo esgotado, esbarrão e recompensa foram verificados nesse teste. Não
  ocorreram queda em armadilha nem desbloqueio de personagem (componente, seção 9.3).
- Três novas observações (seção 6.3).

### 6.3 Novas observações

| # | Observação | Relação com este projeto |
|---|---|---|
| 15 | O rótulo da coluna `controlFor` do registro atual está invertido em relação ao comportamento do jogo | O novo registro grava o valor correto; o registro atual não foi alterado |
| 16 | A pausa automática quando a janela do jogo perde o foco não tem `trigger` previsto no modelo 1.0 | Proposta para a versão 1.1 do modelo (`trigger = window_focus`) |
| 17 | Os campos opcionais `display_mode` e `sound_key` não são preenchidos nesta versão | Inclusão possível sem mudança no modelo |

### 6.4 Decisões

As sete decisões desta etapa (D24 a D30) estão na seção 11 de `componente-registro.md`, com a
justificativa. Podem ser revistas pelo orientador.

---

## 7. Etapa 6 — Exportação e disponibilização dos dados (concluída em 11/10/2026)

**Produto previsto no plano:** mecanismo de exportação dos dados de interação.
**Produto:** exportador (`org.unicog.numberrace.analytics.export`) e pacote de exportação,
descritos em [`exportacao-dados.md`](exportacao-dados.md).

### 7.1 Atividades realizadas

| Atividade do plano | O que foi feito |
|---|---|
| Investigação dos formatos de exportação | Seis formatos comparados (JSON Lines, CSV, SQLite, planilha do Excel, JSON único e Parquet) e teste de abertura de CSVs no Excel em português |
| Definição do formato adotado | Pacote de exportação com dado bruto, tabelas em CSV (versão padrão e versão para o Excel em português), banco SQLite, dicionário de dados, LEIA-ME, relatório e manifesto |
| Implementação da exportação | Programa separado do jogo, com reunião de dados de vários computadores, filtros por sessão, participante e período, tabelas por evento, sessão e rodada e `exportar.bat` |
| Validação dos arquivos produzidos | Conferência automática de cada pacote com o manifesto (SHA-256, tamanho, linhas e colunas) e opção `--verificar` para quem recebe o pacote |
| Testes de recuperação e leitura dos dados | 22 testes automatizados novos; cenários com vários computadores, linhas cortadas, repetições e grande volume; leitura independente dos pacotes em Python |
| Documentação do mecanismo | `exportacao-dados.md`, LEIA-ME gerado em cada pacote e dicionário de dados |

### 7.2 Principais resultados

- Os dados de um ou de vários computadores são reunidos sem repetição, com relatório dos
  problemas encontrados (linhas cortadas, eventos inválidos, conflitos, eventos ausentes e
  sessões sem fim).
- O pacote atende diferentes usos: planilha (tabela com uma linha por rodada), R e Python, SQL e
  o dado bruto completo.
- No teste com o Excel em português, só o CSV com ponto e vírgula abriu em colunas com dois
  cliques, e contas como `6-2` foram convertidas em datas; por isso há duas versões do CSV e as
  contas ficam em colunas separadas.
- **Testes:** 33 testes automatizados aprovados em Java 8 e Java 21; reunião de três pastas (443
  eventos de 7 sessões, 180 repetições ignoradas, linha cortada relatada); 108.000 eventos
  exportados com 128 MB de memória em cerca de 20 s; 31 verificações independentes em Python sem
  erros.
- **Teste no Windows** (Eclipse, Java 8 de 32 bits): exportação da sessão de teste da Etapa 5 (180
  eventos) sem problemas; tabela por rodada aberta corretamente no Excel em português, com as
  contas em colunas separadas; uma alteração proposital em um arquivo do pacote foi detectada pela
  conferência. O teste também revelou uma falha de um teste automatizado causada pela conversão do
  fim de linha pelo Git no Windows, corrigida (`exportacao-dados.md`, seção 10.4).

### 7.3 Decisões

As dez decisões desta etapa (D31 a D40) estão na seção 11 de `exportacao-dados.md`, com a
justificativa. Podem ser revistas pelo orientador.

---

## 8. Etapa 7 — Testes, validação e integridade dos registros (concluída em 11/10/2026)

**Produto previsto no plano:** conjunto de testes e relatório de validação técnica dos registros.
**Produto:** verificação de integridade incorporada ao exportador, testes automatizados, gerador
de sessões simuladas e relatório [`validacao-registros.md`](validacao-registros.md).

### 8.1 Atividades realizadas

| Atividade do plano | O que foi feito |
|---|---|
| Elaboração dos cenários de teste | 22 cenários, cobrindo todos os itens do plano, com o resultado esperado definido antes da execução |
| Criação de dados simulados | Gerador de sessões (semente fixa, todos os 23 tipos de evento, vários participantes e sessões) e execução do jogo real por um programa de teste |
| Testes do registro | Execução do jogo por programa (armadilhas, recompensas, novo personagem, pausas por tecla, menu e perda de foco, sessão fechada à força) e roteiro no Windows |
| Testes de persistência | Cinco falhas simuladas: fechamento à força, banco bloqueado, pasta impossível de criar, disco cheio e relógio atrasado |
| Testes de exportação | Exportação dos dados gerados e dos dados do jogo, conferida também por um programa independente em Python |
| Análise de integridade e completude | Verificação com 21 regras incorporada ao exportador; 18 alterações propositais da sessão de teste, todas detectadas pela regra esperada |
| Correção dos problemas encontrados | Nove problemas registrados; seis corrigidos e três documentados (seção 10 de `validacao-registros.md`) |

### 8.2 Principais resultados

- O exportador confere 21 regras em toda exportação e entrega o resultado no próprio pacote
  (relatório em texto, tabela `integrity` e colunas na tabela de sessões), separando erros (dados
  incoerentes) de avisos (situações com possível explicação legítima, como o relógio do
  computador ajustado ou o jogo fechado à força).
- A sessão de teste da Etapa 5 não apresentou nenhum problema; as 18 alterações propositais foram
  todas detectadas.
- **Problemas encontrados e corrigidos:** ordem dos eventos no prazo esgotado; numeração das
  recompensas fora do intervalo do modelo (geraria eventos inválidos); pausa por perda de foco da
  janela não registrada, inclusive quando ela retomava um jogo pausado pela tecla F8; evento
  perdido após uma gravação interrompida por falta de espaço; modo de exibição não preenchido.
  **Documentados:** o tempo de resposta inclui o tempo em pausa; o tempo de resposta do registro
  atual usa o relógio do computador; nos níveis com números falados, o jogo não reproduz os
  números do personagem da criança (defeito do jogo, fora do componente de registro), de modo que
  `representation.spoken` indica a condição prevista e não a reprodução do som.
- **Versão 1.1 do modelo de eventos:** novo valor `window_focus` em `PAUSE` e `RESUME`. A versão
  1.0 continua válida e o seu schema foi preservado.
- **Testes:** 62 testes automatizados aprovados em Java 8 e Java 21. Sessões geradas: 1.104
  eventos com os 23 tipos, válidos pelo JSON Schema e sem ocorrências nas 21 regras. Jogo real
  executado por programa (4 sessões, 3 participantes, 1.345 eventos): armadilhas, prazos
  esgotados, recompensas de 1 a 7, liberação de personagem, pausas por tecla, menu e perda de foco
  e os quatro tipos de encerramento (inclusive o processo encerrado à força), sem nenhum erro nas
  21 regras; o único aviso foi o esperado para a sessão encerrada à força.
- **Teste no Windows** (Eclipse, Java 8 de 32 bits): 62 testes aprovados, inclusive o encerramento
  forçado de um processo; duas sessões de um participante novo com respostas erradas, prazo
  esgotado, pausas por perda de foco, pausa pela tecla F8 retomada pela recuperação do foco,
  armadilhas, recompensa e saída pelo menu, sem nenhum erro nem aviso nas 21 regras. O
  encerramento pelo Gerenciador de Tarefas foi tratado pelo jogo como fechamento da janela
  (`window_closed`), porque o Windows primeiro pede que a janela se feche
  (`validacao-registros.md`, seção 9).

### 8.3 Decisões

As nove decisões desta etapa (D41 a D49) estão na seção 12 de `validacao-registros.md`, com a
justificativa. Podem ser revistas pelo orientador.

### 8.4 Validação complementar (17/10/2026)

Depois da conclusão da etapa, todo o trabalho entregue foi conferido de novo
(`validacao-registros.md`, seção 15):

- **Teste diferencial com o JSON Schema:** 10.312 alterações de eventos reais e simulados julgadas
  pelo schema oficial (com duas bibliotecas independentes, que concordaram em todos os casos) e pelo
  validador do componente. O validador nunca recusou um evento válido pelo modelo.
- **Testes de estresse e de robustez:** exportação de 5.473 eventos fora do modelo e de 600 sessões
  com um evento alterado (95.810 eventos); 36.635 linhas de JSON cortadas ou corrompidas; opções
  inválidas na linha de comando. A exportação não falhou em nenhum caso depois das correções.
- **Jogo real executado por programa,** de novo, com o código corrigido e com o arquivo de
  participantes no formato do Excel em português: 3 sessões, 2 participantes e 984 eventos de 21
  tipos. O participante cadastrado pela equipe manteve o seu código nas duas execuções, e o arquivo
  continuou legível depois de um cadastro novo. Nenhum erro nas 21 regras; o único aviso foi o
  esperado para a sessão interrompida à força.
- **Problemas encontrados e corrigidos:** um evento com número de partida ou rodada menor que 1
  impedia o banco do pacote inteiro (P10); o arquivo de participantes salvo pelo Excel em português
  deixava de ser reconhecido, e o participante recebia um código novo (P11); uma linha corrompida
  com aninhamento exagerado interrompia a exportação (P12). Reforço: um destino de gravação com
  defeito não para mais a gravação nos outros. Correção de documentação: a descrição da decisão D27
  afirmava que as regras do validador eram equivalentes ao JSON Schema; elas conferem a estrutura, e
  o tipo e o valor dos campos são conferidos nos testes.
- **Testes automatizados:** de 62 para 100, entre eles a conformidade com o JSON Schema oficial, a
  regressão com as sessões do jogo real e testes de concorrência, de ida e volta do JSON e do CSV e
  de entradas estranhas. Todos aprovados em Java 8 e Java 21. Ao todo, a etapa registrou 12
  problemas: 9 corrigidos e 3 documentados.

---

## 9. Etapa 8 — Documentação e consolidação (concluída em 24/10/2026)

**Produto previsto no plano:** documentação técnica consolidada.
**Produto:** [guia técnico do componente](guia-tecnico-dados-interacao.md), descrição dos pacotes no
código (`package-info.java`) e este registro de trabalho atualizado.

### 9.1 Atividades realizadas

| Atividade do plano | O que foi feito |
|---|---|
| Documentação técnica do componente | Guia técnico com visão geral, arquitetura (diagrama), mapa do código, uso pelo pesquisador e pelo desenvolvedor, com receitas de extensão; descrição dos dois pacotes no próprio código |
| Documentação do modelo de dados | Estrutura dos dados reunida no guia (pastas no computador, formato do evento, pacote de exportação e tabelas), com referência ao modelo, ao JSON Schema e ao dicionário de dados |
| Documentação dos testes | Procedimentos de teste reunidos no guia: comandos, tipos de teste e os 100 testes automatizados por classe |
| Registro das decisões e limitações | Índice único das decisões; lista única das limitações, com a situação de cada uma; possibilidades de evolução organizadas conforme a seção 13 do plano |
| Apresentação dos resultados à equipe | A agendar com o orientador |

### 9.2 Principais resultados

- Os dez assuntos pedidos pelo plano para a documentação final estão reunidos no guia técnico:

| Assunto pedido no plano | Seção do guia técnico | Detalhe |
|---|---|---|
| Arquitetura do componente | 2 e 3 | `componente-registro.md`, `exportacao-dados.md` |
| Modelo de eventos | 1 e 4 | `modelo-eventos.md` e JSON Schema |
| Estrutura dos dados | 4 | `persistencia-eventos.md`, `exportacao-dados.md` |
| Estratégia de persistência | 2 e 4 | `persistencia-eventos.md` |
| Mecanismo de exportação | 2, 4 e 5 | `exportacao-dados.md` |
| Instruções de utilização | 5 e 6 | — |
| Procedimentos de teste | 7 | `validacao-registros.md` |
| Decisões técnicas | 8 | Documento de cada etapa |
| Limitações | 9 | Documento de cada etapa |
| Possibilidades de evolução | 10 | — |

- A consolidação mostrou que nenhuma decisão foi substituída. Cinco limitações e observações
  registradas nas Etapas 4 e 5 foram resolvidas nas etapas seguintes; as que continuam em aberto estão listadas,
  com a consequência de cada uma, na seção 9 do guia.
- Foram acrescentadas receitas para as extensões mais prováveis: novo tipo de evento, novo campo,
  nova coluna ou formato de exportação, nova regra de integridade e mudança na estrutura do banco.
- O código do componente não teve mudança de comportamento: apenas a descrição dos dois pacotes.
  Os 100 testes continuam aprovados.
- **Teste da documentação no Windows:** As seções 1 e 5 do guia foram seguidas como faria um usuário novo: jogar, encontrar os dados, exportar, abrir no Excel, ler os relatórios e exportar um único participante. O teste mostrou três pontos em que o texto supunha conhecimento prévio (como abrir uma pasta indicada por `%USERPROFILE%`, onde ficam os relatórios do pacote e como informar opções ao exportador no Eclipse), e a seção 5 foi reescrita com passo a passo. O teste levou de 20 a 30 minutos, e os 100 testes automatizados também foram aprovados no Windows, pelo Eclipse.

### 9.3 Decisões

| # | Decisão | Escolha | Justificativa |
|---|---|---|---|
| D50 | Forma da documentação consolidada | Um guia técnico de entrada, que reúne o essencial e aponta para os documentos de cada etapa, mantidos sem alteração | Os documentos das etapas registram o raciocínio de cada momento e já foram apresentados nos Pull Requests; um guia de entrada evita duplicar o conteúdo e indica a quem chega por onde começar |

---

## 10. Andamento das etapas

| Etapa | Descrição | Situação |
|---|---|---|
| 1 | Ambientação no Number Race e preparação do ambiente | Concluída |
| 2 | Levantamento dos eventos produzidos pelo jogo (catálogo preliminar) | Concluída |
| 3 | Modelagem dos eventos de interação | Concluída |
| 4 | Estratégia de persistência | Concluída |
| 5 | Desenvolvimento do componente de registro | Concluída |
| 6 | Exportação e disponibilização dos dados | Concluída |
| 7 | Testes, validação e integridade dos registros | Concluída |
| 8 | Documentação e consolidação | Concluída (apresentação à equipe a agendar) |

As Etapas 1 e 2 foram incorporadas a este repositório pelo Pull Request #1 (01/10/2026), as Etapas
3 a 5 pelos Pull Requests #2 a #4 (17/10/2026) e as Etapas 6 e 7 pelos Pull Requests #5 e #6
(19/10/2026). A Etapa 8 é apresentada no Pull Request #8. Uma correção encontrada durante os testes,
fora do componente de registro (estalo no início de cada som), foi apresentada à parte, no Pull
Request #7, incorporado em 17/10/2026.

---

## 11. Considerações finais

O projeto partiu da questão: como estruturar e implementar o registro das interações realizadas
durante as sessões do Number Race, de modo a preservar os dados produzidos pelo jogo, permitir a
sua exportação e possibilitar análises posteriores? A resposta construída nas oito etapas foi:

- **estruturar** as interações como eventos com uma parte comum estável e uma parte específica por
  tipo, descritos num modelo versionado e num JSON Schema (Etapas 2 e 3);
- **preservar** os eventos num arquivo de texto por sessão, gravado a cada evento e acompanhado de
  uma cópia em banco de dados, sem depender de internet e sem interromper o jogo (Etapas 4 e 5);
- **exportar** os dados de um ou mais computadores num pacote com o dado bruto, tabelas prontas
  para planilhas e ferramentas de análise, dicionário de dados e conferência de integridade
  (Etapas 6 e 7);
- **documentar** o componente para que outros estudantes e pesquisadores possam usá-lo e
  continuá-lo (Etapa 8).

Produtos finais previstos no plano e onde estão:

| Produto | Local |
|---|---|
| Catálogo de eventos de interação | [`catalogo-eventos.md`](catalogo-eventos.md) |
| Modelo estruturado dos eventos | [`modelo-eventos.md`](modelo-eventos.md) e [`modelo-eventos.schema.json`](modelo-eventos.schema.json) |
| Especificação dos dados registrados | [`modelo-eventos.md`](modelo-eventos.md) e dicionário de dados de cada pacote de exportação |
| Especificação da estratégia de persistência | [`persistencia-eventos.md`](persistencia-eventos.md) |
| Componente de registro de eventos integrado ao software | Pacote `org.unicog.numberrace.analytics` ([`componente-registro.md`](componente-registro.md)) |
| Mecanismo de exportação dos dados | Pacote `org.unicog.numberrace.analytics.export` ([`exportacao-dados.md`](exportacao-dados.md)) |
| Conjunto de testes técnicos | 100 testes automatizados em `numberrace-core/src/test/java/org/unicog/numberrace/analytics/` |
| Relatório de validação dos registros | [`validacao-registros.md`](validacao-registros.md) |
| Documentação técnica do componente | [`guia-tecnico-dados-interacao.md`](guia-tecnico-dados-interacao.md) |
| Relatório final | Formato de entrega a definir com o orientador; o conteúdo está reunido neste registro e no guia técnico |

Todos os testes foram feitos com dados simulados. O uso do componente em pesquisas com crianças
depende dos procedimentos institucionais e éticos previstos no projeto principal. As
possibilidades de continuidade estão na seção 10 do guia técnico.

---

## Nota sobre o desenvolvimento

Este trabalho é feito por Sávio, com apoio de uma ferramenta de inteligência artificial
(Claude, da Anthropic) na análise do código, na preparação das correções, na implementação do
componente de registro, do exportador e da verificação de integridade, nos testes automatizados e
na redação da documentação.
As alterações são acompanhadas, revisadas e testadas por ele.
