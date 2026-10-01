# Catálogo preliminar de eventos de interação do Number Race

Produto da **Etapa 2 (levantamento dos eventos)** do projeto "Modelagem, registro e
persistência de dados de interação no Number Race". Faz parte do
[registro de trabalho](registro-dados-interacao.md).

Este catálogo lista **o que acontece durante o uso do jogo e pode ser registrado**: quando
cada evento ocorre, onde está no código, quais informações estão disponíveis naquele momento
e o que o registro atual já grava. Os nomes dos eventos são **preliminares**; o modelo de dados
definitivo é tarefa da Etapa 3.

As referências de código (arquivo e linha) correspondem ao ramo `correcao-build`
(Pull Request #1).

---

## 1. Método

1. Partimos dos fluxos de interação observados na Etapa 1 (telas e passos de uma rodada).
2. No código, localizamos cada ponto em que algo acontece (método, arquivo e linha).
3. Para cada ponto, levantamos as informações disponíveis naquele instante.
4. Comparamos com o registro atual (`DataFileHandler`/`Student`).
5. Conferimos jogando: cadastro, erros propositais, rodadas com prazo, armadilhas, teclas de
   pausa, menu e saída, e um arquivo de dados gerado em teste.

## 2. Classificação das informações

Seguindo a separação entre dado observado e indicador derivado (`evolucao-ads.md`, seção 10),
cada informação foi classificada em um de três tipos:

| Tipo | Definição | Exemplo |
|---|---|---|
| **Observado** | Fato produzido pela interação | lado escolhido; instante do clique |
| **Contexto** | O que o jogo apresentou ou decidiu naquele momento | valores mostrados; prazo; dificuldade escolhida pelo algoritmo |
| **Derivado** | Pode ser calculado depois a partir dos anteriores | acerto; ganho relativo; taxa de acerto; tempo médio |

O registro deve preservar **observados** e **contexto** (dado bruto). Os **derivados** não
devem substituir o dado bruto. Quando forem gravados por conveniência, devem estar
identificados como tais (decisão D4).

---

## 3. Eventos

Coluna "Registro atual": **Sim** (já gravado), **Parcial** (gravado em parte) ou **Não** (não gravado).

### 3.1 Sessão e navegação

| ID | Nome preliminar | Quando ocorre | Onde no código | Informações | Registro atual |
|---|---|---|---|---|---|
| E01 | `LANGUAGE_SELECTED` | Abertura do jogo | `setup/GamePreferences.setupLanguage` | idioma | Não |
| E02 | `SESSION_START` | Aluno selecionado ou cadastrado | `RegistrationScreen` (l. 445) → `GameObject.setStudent` (l. 1053); nº da sessão em `Student` (l. 109) | participante (**pseudônimo**), nº da sessão, nível inicial (Fácil, Intermediário ou Difícil: níveis 1, 8 e 14 de `ccl.properties`), instante | Parcial: cria `<sobrenome>_<nome>_<sessão>_Data.txt` |
| E03 | `SCREEN_CHANGED` | Toda troca de tela | `GameObject.changeState` (l. 310) | tela anterior, tela nova, instante | Não |
| E04 | `THEME_SELECTED` | Escolha de selva ou fundo do mar | `ThemeScreen` (l. 57–59) | mundo | Não |
| E05 | `CHARACTER_SELECTED` | Escolha do personagem | `GameObject.chooseCharacter` (l. 194) | personagem do jogador, adversário (sorteado) | Não |
| E06 | `PAUSE` / `RESUME` | Tecla F8 | `GameObject.pause` / `unpause` (l. 355, 371) | instante, tela | Não |
| E07 | `MENU_OPENED` | Tecla Esc | `GameObject.showMenu` (l. 498) | instante, tela | Não |
| E08 | `SESSION_END` | Shift+Esc, saída pelo menu ou fechamento da janela | estado `END` (`windowClosing`, l. 861) → `exitStudent` (l. 930) | motivo (encerramento normal ou abandono), tela em que estava, instante | Não |

### 3.2 Partida

| ID | Nome preliminar | Quando ocorre | Onde no código | Informações | Registro atual |
|---|---|---|---|---|---|
| E09 | `GAME_START` | Início de cada corrida | `NumCompManager.gameBegins` (l. 98) / `resetGame` (l. 342) | nº da partida, nível de complexidade (1 a 22), tamanho do tabuleiro, armadilhas ativas | Parcial: coluna `game` |
| E10 | `GAME_END` | Um personagem chega à última casa | `ChoiceScreen.resolveColisions` → `NumCompManager.playerWins` (l. 936) | vencedor, nº de rodadas, posições finais | Não (só contadores acumulados em `AllStudentsList.txt`) |

### 3.3 Rodada

| ID | Nome preliminar | Quando ocorre | Onde no código | Informações | Registro atual |
|---|---|---|---|---|---|
| E11 | `TURN_START` | Início da rodada | `NumCompManager.turnBegins` (l. 103) | nº da rodada; dificuldade escolhida nas 3 dimensões (velocidade, distância, notação); se há prazo e qual; posições das armadilhas | Sim |
| E12 | `STIMULUS_PRESENTED` | Estímulo esquerdo em `runTurn` (l. 232); direito cerca de 1,2 s depois em `showRightStims` (l. 295) | `NumCompManager` | lado; valor; operação (ex.: `3-1=2`); representação (pontos, fala, algarismos; pontos que se apagam); **instante de cada lado** | Parcial: valores sim; instantes não |
| E13 | `RESPONSE_ENABLED` | Depois do estímulo direito (estado `CHOOSE`, l. 314) ou início do prazo (`SNEAK`) | `NumCompManager.showRightStims` | instante | Não |
| E14 | `CLICK_IGNORED` | Clique num lado antes de a resposta ser liberada | `ChoiceScreen` (estado `TURN_START`, l. ~1143: recipientes desabilitados) | instante; lado | Não (o clique não chega à lógica do jogo; exige novo ponto de captura) |
| E15 | `ANSWER` | Clique válido num lado | `NumCompManager.imFast` (l. 1094) | lado escolhido; tempo de resposta; acerto (derivado: `respCorr`) | Sim: `respSide`, `RT`, `respCorr` |
| E16 | `ANSWER_TIMEOUT` | Prazo esgotado sem resposta | `NumCompManager.successfullSneak` (l. 1032) | prazo; o adversário fica com o maior; contado como erro | Parcial: aparece como erro com `RT = 0`, sem identificação própria |
| E17 | `TURN_RESULT` | Após a resposta | `GameTurn.calculateActualWinner` | resultado considerando armadilhas e colisões (derivado: `finalCorr`, usado pelo algoritmo); ganhos e recuos | Sim |
| E18 | `PLAYER_MOVE` | Jogador move o próprio personagem | `ChoiceScreen.stampClicked` (l. 264), `sqClicked` (l. 338); tentativa além do permitido (l. 357) | casa de origem e de destino; tentativas "longe demais" | Não (só posição) |
| E19 | `OPPONENT_MOVE` | Jogador move o personagem adversário | `ChoiceScreen.sqClicked`; tentativa além do permitido (l. 427) | idem | Não (só posição) |
| E20 | `COLLISION` | Um personagem para na casa do outro | `ChoiceScreen.resolveColisions` (l. 641) | personagem empurrado (recua 1 casa) | Parcial: só nos totais de recuo |
| E21 | `HAZARD` | Personagem para numa casa com armadilha | `ChoiceScreen.resolveColisions` (l. 661) | quem caiu; casa; penalidade (1 a 3 casas, sorteada em `HazardManager`, l. 248) | Parcial: posições das armadilhas e totais de recuo |
| E22 | `FEEDBACK_PRESENTED` | Depois dos movimentos | `NumCompManager.reactToBoardMoves` (l. 743) | mensagem do adversário (ultrapassagem, aproximação…); som de acerto ou erro | Não |
| E23 | `DIFFICULTY_ADJUSTED` | Depois de cada resposta | `NumCompAlgManager.addTrial` (l. 190) e `setStimAttributes` (l. 68) | resultado enviado ao algoritmo; sucesso médio; dificuldade desejada e estimada; nova notação | Sim: colunas `meanSucc`, `currDesir`, `edChosen`… e `<aluno>_Alg.txt` (serialização Java) |

### 3.4 Após a partida

| ID | Nome preliminar | Quando ocorre | Onde no código | Informações | Registro atual |
|---|---|---|---|---|---|
| E24 | `REWARD_SAVED` | Clique no animal a ser salvo | `SaveScreen` (l. 30) → `Student.addReward` | tipo do animal (1 a 7); mundo | Parcial: contadores em `AllStudentsList.txt` |
| E25 | `CHARACTER_UNLOCKED` | Ao reunir os 7 tipos de animal | `CharacterScreen` (l. 149–151) → `Student.unlockNextCharacter` | personagem liberado; mundo | Parcial: acesso aos personagens em `AllStudentsList.txt` |

### 3.5 Relação com os eventos candidatos de `evolucao-ads.md`

| Evento candidato | Situação no catálogo |
|---|---|
| `SESSION_START`, `SESSION_END` | E02, E08 |
| `ACTIVITY_START`, `ACTIVITY_END` | O jogo tem uma única atividade (comparação numérica); corresponde a E09/E10 (partida) |
| `STIMULUS_PRESENTED` | E12 |
| `ANSWER` | E15 (e E16 para prazo esgotado) |
| `HELP_REQUESTED` | **Não se aplica:** não há ajuda à criança durante a partida (o "?" da tela inicial mostra os créditos; a ajuda do cadastro é para o professor) |
| `FEEDBACK_PRESENTED` | E22 |
| `LEVEL_START`, `LEVEL_END`, `LEVEL_CHANGED` | Não há fases fixas: a dificuldade muda a cada rodada. Corresponde a E23 |
| `PAUSE`, `RESUME` | E06 |
| `ABANDON` | E08 com motivo "abandono" |

---

## 4. O registro atual, coluna por coluna

Arquivo `<sobrenome>_<nome>_<sessão>_Data.txt`, gravado em `DataFileHandler.writeStudentFileDataLine`
(l. 296), uma linha por resposta. O cabeçalho (l. 285) tem 57 nomes fixos e mais um por casa do
tabuleiro (`hazSq1…hazSqN`, com N de 12 a 40), e é reescrito na primeira rodada de cada partida.

| Colunas | Significado | Tipo |
|---|---|---|
| `lastName`, `firstName` | nome da criança | **dado pessoal** (deve ser substituído por pseudônimo) |
| `session`, `game`, `turn` | nº da sessão, partida e rodada | contexto |
| `date`, `time` | início da rodada (precisão de segundos) | observado |
| `meanSucc`, `currDesir`, `edChosen` | estado do algoritmo | contexto |
| `DiffSpeed`, `DiffDist`, `DiffNotn`, `cDiff…`, `ptChosn1–3` | dificuldade escolhida nas 3 dimensões | contexto |
| `anlgMag`, `verbal`, `arabic`, `dotFade`, `rstrRge`, `hazard`, `add`, `subtrc`, `fadeSpd`, *(sem nome: `boardLength`)*, `controlFor` | configuração da representação na rodada | contexto |
| `leftStim`, `rightStim`, `leftSub…`, `rightSub…` | valores e operações apresentados | contexto (estímulo) |
| `respSide` | lado escolhido | observado |
| `RT` | tempo de resposta em ms, contado desde o estímulo **esquerdo**; `0` quando o prazo se esgota | observado |
| `respCorr` | escolheu o maior | derivado |
| `finalCorr` | escolha vantajosa considerando armadilhas e colisões | derivado (usado pelo algoritmo) |
| `relNetGain`, `p1NetGain`, `p2NetGain`, `p1movfor`, `p2movfor`, `p1movback`, `p2movback` | ganhos e recuos da rodada | derivado |
| `p1square`, `p2square` | posições **antes** do movimento da rodada | observado |
| `h_relNetGain` … `h_p2movback` | resultado hipotético da escolha oposta | derivado |
| `hazSq1…hazSqN` | armadilha em cada casa | contexto |

**Problemas verificados num arquivo gerado em teste:**

- **Colunas deslocadas:** a configuração da representação grava 10 valores
  (`NotnDimLevel.getAttributesCommaDelim`, incluindo `boardLength`), mas o cabeçalho tem 9 nomes.
  A partir de `controlFor`, cada valor fica sob o nome da coluna seguinte.
- O `boardLength` gravado é o do nível de representação da rodada, que pode diferir do
  tabuleiro da partida em curso.
- O nome real do aluno aparece no **nome dos arquivos** e no conteúdo.
- O prazo esgotado só é reconhecível por `RT = 0`.

## 5. Eventos ausentes ou insuficientes no registro atual

1. Início e fim de sessão, com instante e motivo do encerramento (normal ou abandono).
2. Idioma, mundo, personagem, troca de telas, pausa e menu.
3. Instantes de cada estímulo e da liberação da resposta (só há o início da rodada, em segundos,
   e o RT).
4. Cliques antes da liberação da resposta.
5. Prazo esgotado como evento próprio.
6. Movimentos, tentativas "longe demais", armadilhas e colisões como eventos.
7. Fim de partida com vencedor e número de rodadas.
8. Recompensas e personagens liberados como eventos (só há contadores acumulados).
9. Mensagens e sons de retorno apresentados à criança.
10. Identificação pseudonimizada (hoje há nome e sobrenome no conteúdo e no nome dos arquivos).

## 6. Decisões tomadas

Decisões de escopo tomadas nesta etapa. Podem ser revistas pelo orientador.

| # | Questão | Decisão | Justificativa |
|---|---|---|---|
| D1 | Registrar cliques antes da liberação da resposta (E14)? | **Sim, como opção configurável** (ligada ou desligada) | Pode indicar pressa ou impulsividade, mas exige um novo ponto de captura no código. Ser opcional evita impor o custo a todo uso |
| D2 | Registrar cada clique de casa no tabuleiro (E18/E19)? | **Registrar o resultado de cada movimento e as tentativas "longe demais"**. Clique a clique fica como extensão possível | Preserva a informação sobre erros de contagem sem multiplicar o volume de dados |
| D3 | Registrar o estado do algoritmo adaptativo (E23)? | **Sim** | O plano pede o registro das "adaptações realizadas pelo algoritmo". Sem isso não se explica por que uma rodada foi fácil ou difícil |
| D4 | Gravar o acerto no evento de resposta, sendo derivável? | **Sim, `respCorr` e `finalCorr`, identificados como derivados** | Segue o exemplo de `evolucao-ads.md` (`"correct": true`). Os dois "acertos" têm significados diferentes, e `finalCorr` é o que alimenta o algoritmo |

## 7. Implicações para a Etapa 3 (modelagem)

- Definir explicitamente o **tempo de resposta**. Recomendação: registrar os instantes de E12
  (cada lado), E13 e E15, e calcular o RT a partir deles.
- **Identificação:** `participant_id` pseudonimizado, sem nome no conteúdo nem nos nomes de arquivo.
- **Estrutura comum** a todos os eventos (identificador, sessão, participante, instante, tipo)
  e **campos específicos** por tipo (estímulo, resposta, movimento…).
- **Versão do esquema** em cada evento (`schemaVersion`), conforme `evolucao-ads.md`.
