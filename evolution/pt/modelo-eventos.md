# Modelo de eventos de interação do Number Race (versão 1.0)

Produto da **Etapa 3 (modelagem dos eventos de interação)** do projeto "Modelagem, registro e
persistência de dados de interação no Number Race". Faz parte do
[registro de trabalho](registro-dados-interacao.md) e toma como base o
[catálogo preliminar de eventos](catalogo-eventos.md) (Etapa 2).

Este documento define **como cada evento é escrito**: estrutura, campos, tipos de dados,
campos obrigatórios e opcionais, identificadores, relação entre sessão e eventos, regras de
consistência e forma de evolução do modelo. Onde e como os eventos serão armazenados é tema da
Etapa 4; a implementação no jogo, da Etapa 5.

Arquivos que acompanham este documento:

| Arquivo | Conteúdo |
|---|---|
| [`modelo-eventos.schema.json`](modelo-eventos.schema.json) | O modelo em formato verificável por programa (JSON Schema, versão 2020-12) |
| [`modelo-eventos-exemplos.json`](modelo-eventos-exemplos.json) | Uma sessão simulada com 44 eventos (três rodadas), validada contra o schema |

As referências de código (arquivo e linha) correspondem ao ramo `main` deste repositório após a
incorporação do Pull Request #1 (commit `c04df11`). Os exemplos usam **dados simulados**.

---

## 1. Princípios

1. **Registrar fatos, não interpretações.** O evento guarda o que aconteceu e o contexto em que
   aconteceu, sem incorporar interpretações clínicas (`evolucao-ads.md`, seção 9).
2. **Separar observado, contexto e derivado** (catálogo, seção 2). Campos derivados só são
   gravados quando úteis e ficam identificados como tais (decisão D4).
3. **Nenhum dado pessoal direto.** O participante é identificado apenas por um código
   pseudonimizado.
4. **Cada evento se explica sozinho.** Um evento traz a identificação da sessão, da partida e da
   rodada, sem depender da posição em um arquivo (ao contrário das colunas do registro atual).
5. **Formato aberto e portável.** JSON em UTF-8, data e hora em ISO 8601 e regras descritas em
   JSON Schema, legíveis por qualquer linguagem.

## 2. Estrutura do evento

Todo evento tem duas partes:

- a **parte comum** (campos do primeiro nível), igual para todos os tipos de evento: o que
  aconteceu, quando, em qual sessão, com qual participante e em qual versão do modelo;
- a **parte específica** (campo `data`), cujos campos dependem do tipo de evento.

Exemplo (resposta da criança):

```json
{
  "schema_version": "1.0",
  "event_id": "e01c683e-99a4-4df0-9de3-a361c0099eba",
  "event_type": "ANSWER",
  "timestamp": "2026-10-01T14:00:35.944-03:00",
  "session_id": "f38b2ffc-80a4-4f5a-91c9-bc701e7ea419",
  "participant_id": "P017",
  "sequence_number": 23,
  "activity": "number_comparison",
  "game_number": 1,
  "turn_number": 2,
  "data": {
    "side": "left",
    "response_time_ms": 1834,
    "response_time_from_first_stimulus_ms": 3034,
    "correct_larger": true,
    "correct_final": true
  }
}
```

A separação permite que qualquer programa ordene, filtre e agrupe os eventos olhando apenas a
parte comum, sem conhecer todos os tipos, e que novos tipos de evento sejam acrescentados sem
alterar essa parte (seção 11). No exemplo de `evolucao-ads.md` (seção 9), os campos comuns e
os específicos aparecem no mesmo nível; o presente modelo mantém os mesmos tipos de informação
e apenas os organiza em duas partes (decisão D5).

## 3. Parte comum

| Campo | Tipo | Obrigatório | Descrição | Exemplo |
|---|---|---|---|---|
| `schema_version` | texto | Sim | Versão do modelo usada para escrever o evento | `"1.0"` |
| `event_id` | identificador (UUID) | Sim | Identificador único do evento | `"e01c683e-…"` |
| `event_type` | categoria | Sim | Tipo do evento (seção 7) | `"ANSWER"` |
| `timestamp` | data e hora | Sim | Instante do evento, em ISO 8601 com milissegundos e fuso horário | `"2026-10-01T14:00:35.944-03:00"` |
| `session_id` | identificador (UUID) | Sim | Sessão a que o evento pertence | `"f38b2ffc-…"` |
| `participant_id` | identificador (pseudônimo) | Sim | Código do participante; nunca o nome | `"P017"` |
| `sequence_number` | inteiro, a partir de 1 | Sim | Ordem do evento dentro da sessão | `23` |
| `activity` | categoria | Quando aplicável | Atividade em curso; presente nos eventos de partida e de rodada | `"number_comparison"` |
| `game_number` | inteiro, a partir de 1 | Quando aplicável | Número da partida dentro da sessão | `1` |
| `turn_number` | inteiro, a partir de 1 | Quando aplicável | Número da rodada dentro da partida | `2` |
| `data` | objeto | Sim | Parte específica do tipo de evento; pode ser vazio (`{}`) | ver seção 7 |

Regras para os campos "quando aplicável":

| Eventos | `activity` e `game_number` | `turn_number` |
|---|---|---|
| De rodada: `TURN_START`, `STIMULUS_PRESENTED`, `RESPONSE_ENABLED`, `CLICK_IGNORED`, `ANSWER`, `ANSWER_TIMEOUT`, `TURN_RESULT`, `MOVE`, `COLLISION`, `HAZARD`, `FEEDBACK_PRESENTED` | Obrigatórios | Obrigatório |
| De partida: `GAME_START`, `GAME_END`, `DIFFICULTY_ADJUSTED` | Obrigatórios | Opcional (presente quando há uma rodada em curso) |
| De sessão: os demais | Opcionais (presentes quando o evento ocorre durante uma partida, por exemplo uma pausa) | Opcional |

`turn_number` exige `game_number`, e `game_number` exige `activity`. O jogo tem uma única
atividade (comparação numérica; catálogo, seção 3.5), mas o campo permite acrescentar outras
no futuro.

## 4. Identificadores

| Identificador | Formato | Gerado quando | Observações |
|---|---|---|---|
| `event_id` | UUID versão 4 | A cada evento | Único mesmo entre computadores diferentes, sem necessidade de coordenação. Permite referenciar um evento e detectar duplicados |
| `session_id` | UUID versão 4 | No início da sessão (`SESSION_START`) | Liga todos os eventos da sessão. O número ordinal da sessão do participante (coluna `session` do registro atual) fica em `SESSION_START.data.session_number` |
| `participant_id` | Texto de 1 a 32 caracteres (`A-Z`, `a-z`, `0-9`, `_`, `-`) | No cadastro do participante | Ver regras abaixo |
| `game_number` | Inteiro a partir de 1 | No início de cada partida | Corresponde à coluna `game` do registro atual |
| `turn_number` | Inteiro a partir de 1 | No início de cada rodada | Corresponde à coluna `turn`; reinicia a cada partida |
| `sequence_number` | Inteiro a partir de 1 | A cada evento | Reinicia a cada sessão. Ordena eventos com o mesmo instante e permite detectar eventos perdidos (lacunas) |

Regras para o `participant_id`:

- não pode conter nome, sobrenome ou outro dado que identifique diretamente a criança;
- não deve ser calculado a partir do nome (por exemplo, por *hash*), pois esse cálculo pode ser
  revertido testando nomes conhecidos;
- a correspondência entre código e pessoa, quando existir, fica fora dos dados de interação,
  sob responsabilidade da equipe de pesquisa;
- formato recomendado: `P` seguido de número (`P017`), como no exemplo de `evolucao-ads.md`.

Hoje o cadastro (`RegistrationScreen`) pede nome e sobrenome, e esses dados aparecem no
conteúdo e no nome dos arquivos do registro atual. A forma de atribuir o código no jogo será
definida na implementação (Etapa 5).

## 5. Tipos de dados e convenções

| Tipo no modelo | Tipo em JSON Schema | Exemplo | Uso |
|---|---|---|---|
| Texto | `string` | `"3.1.0"` | versões, códigos de mensagem |
| Categoria | `string` com lista fechada (`enum`) | `"left"` | lado, personagem, tela, motivo |
| Identificador | `string` com padrão | `"P017"`, UUID | seção 4 |
| Data e hora | `string`, ISO 8601 | `"2026-10-01T14:00:35.944-03:00"` | `timestamp` |
| Inteiro | `integer` | `1834` | contagens, casas, durações em ms |
| Real | `number` | `0.35` | dificuldade de 0 a 1, estado do algoritmo |
| Lógico | `boolean` | `true` | acerto, presença de soma ou de prazo |
| Nulo | `null` | `null` | "não se aplica" (ex.: prazo de rodada sem prazo) |
| Objeto | `object` | `{"player": 2, "opponent": 4}` | grupos de campos |
| Lista | `array` | `[{"square": 6, "penalty": 3}]` | armadilhas no tabuleiro |

Convenções:

- **Nomes de campos** em inglês, em letras minúsculas separadas por sublinhado (*snake_case*),
  como na tabela "Modelo inicial" do plano de trabalho (`event_id`, `session_id`,
  `participant_id`). Ver decisão D6.
- **Tipos de evento** em maiúsculas (`ANSWER`), como no catálogo e em `evolucao-ads.md`.
- **Valores de categoria** em minúsculas (`left`, `number_comparison`).
- **Durações** em milissegundos, como inteiros, com o sufixo `_ms` no nome do campo.
- **Data e hora** em ISO 8601 com milissegundos e deslocamento do fuso horário (`-03:00`). O
  exemplo de `evolucao-ads.md` (`2026-08-23T10:32:18`) não tem milissegundos nem fuso; ambos
  foram acrescentados porque uma rodada pode durar menos de um segundo e porque dados de
  computadores ou períodos diferentes precisam ser comparáveis.
- **Lado** (`left`, `right`) e **personagem** (`player` para o da criança, `opponent` para o
  adversário) usam sempre os mesmos valores.

## 6. Sessão, partida, rodada e eventos

```
Participante (participant_id)
└── Sessão (session_id)                    SESSION_START ... SESSION_END
    ├── eventos de navegação               SCREEN_CHANGED, THEME_SELECTED, CHARACTER_SELECTED, PAUSE, RESUME
    ├── recompensas                        REWARD_SAVED, CHARACTER_UNLOCKED
    └── Partida (game_number)              GAME_START ... GAME_END
        ├── adaptação                      DIFFICULTY_ADJUSTED
        └── Rodada (turn_number)           TURN_START ... (estímulos, resposta, movimentos)
```

- Um participante tem várias sessões; uma sessão tem várias partidas; uma partida tem várias
  rodadas; todos os níveis têm eventos. A relação é feita pelos identificadores da parte comum,
  sem necessidade de tabelas separadas.
- A **sessão** começa quando um aluno é selecionado ou cadastrado e termina com a troca de aluno
  pelo menu, a saída do programa (menu ou Shift+Esc) ou o fechamento da janela. Uma mesma
  execução do programa pode ter várias sessões, pois o menu permite trocar de aluno
  (`GameObject`, l. 927–931).
- Eventos anteriores à seleção do aluno (escolha do idioma e telas de cadastro) não são
  registrados como eventos; o idioma passa a ser um campo de `SESSION_START` (decisão D10).
- **Sequência típica de uma rodada**, na ordem em que ocorre:

| Ordem | Evento | Origem no código |
|---|---|---|
| 1 | `TURN_START` | `NumCompManager.turnBegins` (l. 103) |
| 2 | `STIMULUS_PRESENTED` (esquerda) | `runTurn` (l. 232) |
| (3) | `CLICK_IGNORED`, se a criança clicar antes da hora | `ChoiceScreen` (estado `TURN_START`) |
| 4 | `STIMULUS_PRESENTED` (direita), cerca de 1,2 s depois | `showRightStims` (l. 295) |
| 5 | `RESPONSE_ENABLED` | `showRightStims` (l. 314) |
| 6 | `ANSWER` ou `ANSWER_TIMEOUT` | `imFast` (l. 1094) ou `successfullSneak` (l. 1032) |
| 7 | `TURN_RESULT` | `GameTurn.calculateActualWinner` |
| 8 | `FEEDBACK_PRESENTED` (pode ocorrer em vários momentos) | `opponentTalks`, `reactToBoardMoves` (l. 743) |
| 9 | `MOVE` (criança), com `HAZARD` ou `COLLISION` se houver | `ChoiceScreen.sqClicked` (l. 338), `resolveColisions` (l. 641) |
| 10 | `MOVE` (adversário), com `HAZARD` ou `COLLISION` se houver | idem |
| 11 | `DIFFICULTY_ADJUSTED` (para a rodada seguinte) | `runTurnEndEvents` (l. 912) → `setStimAttributes` (l. 68) |

O arquivo de exemplos mostra essa sequência em três rodadas, incluindo um clique antes da hora,
um esbarrão, uma armadilha, um prazo esgotado, uma pausa e a saída no meio da partida.

## 7. Tipos de evento

Coluna "Cl.": **O** (observado), **C** (contexto) ou **D** (derivado), conforme o catálogo.
Todos os campos listados são obrigatórios, salvo indicação. O catálogo tinha 25 eventos
preliminares; o modelo tem 23 tipos (ver a correspondência na seção 7.5).

### 7.1 Sessão e navegação

**`SESSION_START`**: aluno selecionado ou cadastrado (catálogo E02, com E01).

| Campo | Tipo | Cl. | Descrição | Registro atual |
|---|---|---|---|---|
| `session_number` | inteiro | C | Ordem da sessão para este participante | `session` |
| `start_level` | `easy`, `intermediate`, `hard` | C | Nível inicial escolhido no cadastro | não gravado |
| `start_complexity_level` | inteiro 1–22 | C | Nível correspondente em `ccl.properties` (1, 8 ou 14) | não gravado |
| `language` | texto (ISO 639-1) | C | Idioma do jogo | não gravado |
| `app_version` | texto | C | Versão do jogo (`3.1.0`) | não gravado |
| `display_mode` (opcional) | `fullscreen`, `in_window` | C | Modo de exibição | não gravado |

**`SESSION_END`**: fim da sessão (E08).

| Campo | Tipo | Cl. | Descrição |
|---|---|---|---|
| `end_reason` | `change_student`, `quit`, `window_closed` | O | Troca de aluno pelo menu, saída do programa (menu ou Shift+Esc) ou fechamento da janela |
| `screen` | tela | C | Tela em que estava |
| `game_in_progress` | lógico | C | Se havia partida em andamento. O indicador de abandono (candidato `ABANDON` de `evolucao-ads.md`) é derivado deste campo |

**`SCREEN_CHANGED`** (E03): `from_screen` e `to_screen` (tela; C). Telas: `start`, `title`,
`registration`, `theme`, `instructions`, `characters`, `choice`, `gameover`, `save`, `zoo`
(estados de `GameObject.GameStates`, em minúsculas).

**`THEME_SELECTED`** (E04): `theme` (`jungle` ou `under_the_sea`; O).

**`CHARACTER_SELECTED`** (E05): `player_character` (índice 0–5, escolhido pela criança; O) e
`opponent_character` (índice 0–3, sorteado pelo jogo; C).

**`PAUSE`** e **`RESUME`** (E06 e E07): `trigger` (`pause_key` para a tecla F8, `menu` para a
tecla Esc; O) e `screen` (C). Abrir o menu também pausa o jogo (`GameObject.changeState`,
l. 334); por isso a abertura do menu é registrada como `PAUSE` com `trigger = menu`
(decisão D11). Depois dela vem `RESUME` (volta ao jogo) ou `SESSION_END` (troca de aluno ou
saída).

### 7.2 Partida

**`GAME_START`** (E09): `board_length` (casas do tabuleiro; C) e `complexity_level` (nível da
primeira rodada, que define o tabuleiro; C). Origem: `resetGame` (l. 342).

**`GAME_END`** (E10): `winner` (`player` ou `opponent`; O), `turns_played` (inteiro; D) e
`final_squares` (posição final de cada personagem; O). Origem: `playerWins` (l. 936).

**`DIFFICULTY_ADJUSTED`** (E23): escolha da dificuldade pelo algoritmo adaptativo para a
rodada seguinte (no início da partida, para a primeira rodada).

| Campo | Tipo | Cl. | Descrição | Registro atual |
|---|---|---|---|---|
| `next_turn_number` | inteiro | C | Rodada para a qual a dificuldade foi escolhida | — |
| `difficulty.speed`, `.distance`, `.notation` | real 0–1 | C | Dificuldade escolhida em cada dimensão | `DiffSpeed`, `DiffDist`, `DiffNotn` |
| `mean_success` | real | C | Sucesso médio estimado | `meanSucc` |
| `desired_difficulty` | real | C | Dificuldade desejada | `currDesir` |
| `estimated_difficulty` | real | C | Dificuldade estimada do ponto escolhido | `edChosen` |
| `coords_chosen` | lista de 3 reais | C | Ponto escolhido no espaço de problemas | `ptChosn1–3` |

O resultado enviado ao algoritmo não é repetido aqui: é o `correct_final` do `ANSWER` ou, no
prazo esgotado, sempre erro (seção 8). A conversão da distância em fração de Weber não é
registrada, pois o próprio código indica que ela não funciona (`NumCompAlgManager`, l. 129).

### 7.3 Rodada

**`TURN_START`** (E11): configuração aplicada na rodada.

| Campo | Tipo | Cl. | Descrição | Registro atual |
|---|---|---|---|---|
| `complexity_level` | inteiro 1–22 | C | Nível de complexidade da rodada (numeração de `ccl.properties`) | `cDiffNotn` (índice a partir de 0: o nível 1 aparece como `0.0`) |
| `representation.dots` | lógico | C | Quantidade em pontos | `anlgMag` |
| `representation.spoken` | lógico | C | Número falado | `verbal` |
| `representation.digits` | lógico | C | Número em algarismos | `arabic` |
| `representation.dot_fading` | lógico | C | Os pontos se apagam | `dotFade` |
| `representation.dot_fading_ms` | inteiro | C | Duração do apagamento (0 quando não há) | `fadeSpd` |
| `max_value` | inteiro | C | Maior valor possível | `rstrRge` |
| `addition`, `subtraction` | lógico | C | Há soma ou subtração | `add`, `subtrc` |
| `hazards_enabled` | lógico | C | O nível da rodada prevê armadilhas (o jogo pode acrescentar novas) | `hazard` |
| `hazards_on_board` | lista de `{square, penalty}` | C | Armadilhas presentes no tabuleiro no início da rodada, inclusive as de rodadas anteriores; vazia quando não há | `hazSq1…N` (penalidade em cada casa; 0 quando não há) |
| `deadline_trial` | lógico | C | A rodada tem prazo | `DiffSpeed` (indireto: há prazo acima de um limite) |
| `deadline_ms` | inteiro ou nulo | C | Prazo; `null` quando não há | `cDiffSpeed` (em segundos) |
| `control_for` | `density`, `item_size` | C | Controle das nuvens de pontos | `controlFor` |
| `squares.player`, `squares.opponent` | inteiro | O | Posições no início da rodada | `p1square`, `p2square` |

**`STIMULUS_PRESENTED`** (E12): um evento para cada lado, no instante em que é mostrado.

| Campo | Tipo | Cl. | Descrição | Registro atual |
|---|---|---|---|---|
| `side` | lado | C | Lado apresentado | — |
| `value` | inteiro | C | Quantidade a comparar | `leftStim`, `rightStim` |
| `operation` | objeto ou nulo | C | `{"operator": "+", "operands": [1, 3]}`; `null` quando não há operação | `leftSub…`, `rightSub…` |

**`RESPONSE_ENABLED`** (E13): instante a partir do qual o clique é aceito. Parte específica
vazia (`{}`); a informação está no `timestamp`.

**`CLICK_IGNORED`** (E14): clique antes da liberação da resposta. `side` (O). Registro
**opcional e configurável** (decisão D1).

**`ANSWER`** (E15): clique válido num lado.

| Campo | Tipo | Cl. | Descrição | Registro atual |
|---|---|---|---|---|
| `side` | lado | O | Lado escolhido | `respSide` |
| `response_time_ms` | inteiro | O | Tempo desde a liberação da resposta até o clique (seção 8) | não gravado |
| `response_time_from_first_stimulus_ms` | inteiro | O | Tempo desde o estímulo esquerdo até o clique | `RT` |
| `correct_larger` | lógico | D | Escolheu o número maior | `respCorr` |
| `correct_final` | lógico | D | Escolha mais vantajosa considerando armadilhas e esbarrões; é o resultado enviado ao algoritmo | `finalCorr` |

**`ANSWER_TIMEOUT`** (E16): prazo esgotado sem resposta.

| Campo | Tipo | Cl. | Descrição | Registro atual |
|---|---|---|---|---|
| `deadline_ms` | inteiro | C | Prazo da rodada | — |
| `assigned_side` | lado | C | Lado que sobrou para a criança (o menor), atribuído pelo jogo | `respSide` (sem distinção de uma escolha real) |

**`TURN_RESULT`** (E17): consequências da escolha no tabuleiro, calculadas pelo jogo.

| Campo | Tipo | Cl. | Descrição | Registro atual |
|---|---|---|---|---|
| `turn_winner` | personagem | D | Quem levou vantagem na rodada | — (`actualWinner`, não gravado) |
| `outcome.relative_net_gain` | inteiro | D | Ganho líquido da criança menos o do adversário | `relNetGain` |
| `outcome.net_gain`, `.squares_forward`, `.squares_back` | um inteiro por personagem | D | Ganhos, avanços e recuos | `p1NetGain`, `p2NetGain`, `p1movfor`, `p2movfor`, `p1movback`, `p2movback` |
| `hypothetical_outcome` | mesma estrutura de `outcome` | D | Resultado se a escolha tivesse sido o outro lado | `h_relNetGain` … `h_p2movback` |

**`MOVE`** (E18 e E19): a criança move um dos personagens no tabuleiro.

| Campo | Tipo | Cl. | Descrição |
|---|---|---|---|
| `piece` | personagem | O | Personagem movido (a criança move o seu e o do adversário) |
| `from_square`, `to_square` | inteiro | O | Casa de origem e de destino |
| `steps` | inteiro | C | Casas que deviam ser percorridas |
| `too_far_attempts` | inteiro | O | Cliques em casas além do permitido (decisão D2) |

**`COLLISION`** (E20): `pushed_piece` (personagem que volta uma casa), `from_square`,
`to_square` (C: consequência aplicada pelo jogo).

**`HAZARD`** (E21): `piece`, `square` (casa da armadilha), `penalty` (1 a 3 casas) e
`to_square` (C).

**`FEEDBACK_PRESENTED`** (E22): `message_key` (identificador da fala, que é o nome do áudio,
por exemplo `enemy1_youreCatchingUp`; C) e `sound_key` (som de acerto ou erro, opcional; C).

### 7.4 Após a partida

**`REWARD_SAVED`** (E24): `reward_type` (animal salvo, 1 a 7; O) e `theme` (C).

**`CHARACTER_UNLOCKED`** (E25): `character` (índice 0–5; C) e `theme` (C).

### 7.5 Correspondência com o catálogo

| Catálogo (Etapa 2) | Modelo | Mudança |
|---|---|---|
| E01 `LANGUAGE_SELECTED` | `SESSION_START.data.language` | Incorporado (D10) |
| E06 `PAUSE` / `RESUME` e E07 `MENU_OPENED` | `PAUSE` e `RESUME` com `trigger` | Unificados (D11) |
| E18 `PLAYER_MOVE` e E19 `OPPONENT_MOVE` | `MOVE` com `piece` | Unificados (D12) |
| Demais (E02–E05, E08–E17, E20–E25) | Mesmo nome | — |

## 8. Tempo de resposta

- `response_time_ms` é medido **desde a liberação da resposta** (`RESPONSE_ENABLED`, quando o
  estímulo direito aparece) até o clique. É o tempo em que a criança pôde de fato responder.
- `response_time_from_first_stimulus_ms` é medido desde o estímulo esquerdo, como a coluna `RT`
  do registro atual (`imFast`, l. 1095, a partir de `runTurn`, l. 236). Mantê-lo permite
  comparar dados novos e antigos. A diferença entre os dois é de cerca de 1,2 s.
- Os dois tempos devem ser medidos com um relógio monotônico (por exemplo, `System.nanoTime`),
  e não pela diferença entre os `timestamp`, que dependem do relógio do sistema e podem ser
  ajustados durante o uso.
- **Prazo esgotado** não tem tempo de resposta: é registrado como `ANSWER_TIMEOUT`, e não como
  um `ANSWER` com tempo zero. Nesse caso, o jogo envia "erro" ao algoritmo
  (`successfullSneak`, l. 1032) e atribui à criança o lado com o menor número; no modelo isso
  aparece como `assigned_side`, para não ser confundido com uma escolha.
- Os instantes de `STIMULUS_PRESENTED` correspondem ao momento em que o jogo manda mostrar o
  estímulo; o atraso de desenho na tela não é medido.

## 9. Correspondência com o plano de trabalho

Elementos que o plano pede para considerar na Etapa 3:

| Elemento do plano | Onde está no modelo |
|---|---|
| Identificador do evento | `event_id` |
| Identificador pseudonimizado do participante | `participant_id` |
| Identificador da sessão | `session_id` |
| Instante do evento | `timestamp` (e `sequence_number` para a ordem) |
| Atividade | `activity` |
| Estímulo | `STIMULUS_PRESENTED` (`value`, `operation`) |
| Resposta | `ANSWER.side`; `ANSWER_TIMEOUT` |
| Resultado | `ANSWER.correct_larger` e `correct_final`; `TURN_RESULT` |
| Tempo de resposta | `ANSWER.response_time_ms` (unidade: milissegundos) |
| Nível de dificuldade | `TURN_START.complexity_level` e `deadline_ms`; `DIFFICULTY_ADJUSTED.difficulty` |
| Representação utilizada | `TURN_START.representation` |
| Estado relevante do jogo | `TURN_START.squares` e `hazards_on_board`; `GAME_START`; `MOVE`, `COLLISION`, `HAZARD`; `GAME_END` |
| Informações relacionadas à adaptação | `DIFFICULTY_ADJUSTED` |
| Tipos de dados | Seção 5 |
| Campos obrigatórios e opcionais | Seções 3 e 7 |
| Identificadores | Seção 4 |
| Relacionamento entre sessão e eventos | Seção 6 |
| Possibilidade de evolução futura | Seção 11 |
| Consistência | Seção 10 |
| Portabilidade | Seção 12 |

Na tabela "Modelo inicial" do plano, estímulo, resposta e resultado aparecem como campos de um
mesmo registro. No modelo, eles são **eventos separados**, porque acontecem em instantes
diferentes (estímulo esquerdo, estímulo direito cerca de 1,2 s depois e, então, a resposta) e o
intervalo entre eles é justamente o que se quer medir (decisão D7). Uma visão com uma linha
por rodada, reunindo estímulo, resposta e resultado, como no exemplo de `evolucao-ads.md`, será
produzida na exportação (Etapa 6) a partir dos eventos.

O modelo também corrige três problemas do registro atual: as colunas deslocadas (cada valor
passa a ter nome; problema verificado na Etapa 2), o nome da criança no conteúdo e nos nomes
dos arquivos (substituído pelo `participant_id`) e o prazo esgotado registrado como `RT = 0`
(substituído por `ANSWER_TIMEOUT`).

## 10. Consistência e validação

O JSON Schema confere, em cada evento:

- a presença dos campos obrigatórios, inclusive os condicionais (por exemplo, `turn_number` em
  eventos de rodada);
- o tipo de cada campo e os valores permitidos nas categorias;
- os limites numéricos (por exemplo, tempo de resposta não negativo, penalidade de 1 a 3);
- os formatos de `timestamp`, UUID e `participant_id`;
- regras entre campos: `deadline_ms` é inteiro quando `deadline_trial` é verdadeiro e `null`
  quando é falso;
- a ausência de campos não previstos, o que revela nomes digitados errado.

Regras que envolvem vários eventos e por isso são conferidas por teste, e não pelo schema:

1. `sequence_number` vai de 1 até o número de eventos da sessão, sem repetição nem lacuna;
2. os `timestamp` de uma sessão não diminuem;
3. o primeiro evento da sessão é `SESSION_START` e, se houver `SESSION_END`, ele é o último;
4. uma sessão tem um único `participant_id`;
5. cada rodada tem no máximo um `ANSWER` ou `ANSWER_TIMEOUT`.

Validação realizada nesta etapa: os 44 eventos de `modelo-eventos-exemplos.json` são válidos
pelo schema e pelas cinco regras acima. Oito eventos alterados
de propósito foram rejeitados: sem `timestamp`; tipo escrito errado (`ANSER`); tempo de
resposta negativo; campo com nome errado (`responce_time`); resposta sem `turn_number`;
`participant_id` com nome e espaço; `timestamp` sem milissegundos e sem fuso; prazo informado
em rodada sem prazo.

**Conferência com dados reais.** Uma rodada de um arquivo gerado pelo jogo atual num teste
(rodada 4, prazo esgotado, `RT = 0`) foi convertida para o modelo, e os eventos resultantes
são válidos. A conferência revelou um fato que a primeira versão do modelo não previa: as
armadilhas **permanecem no tabuleiro** de uma rodada para outra (`HazardManager.setHazards`,
l. 153, só acrescenta armadilhas). Naquela rodada, o nível não previa armadilhas, mas havia uma
na casa 6, colocada antes. Por isso, a lista de armadilhas da rodada (`hazards_on_board`) não
depende de `hazards_enabled`, e cada armadilha é registrada com a sua penalidade, como no
registro atual. A mesma rodada mostra por que `ANSWER_TIMEOUT` não traz acerto: por causa da
armadilha, o número menor, atribuído à criança, era a opção mais vantajosa (ganho relativo de
−1 contra −2), mas o jogo conta o prazo esgotado sempre como erro (`finalCorr = 0`).

## 11. Extensibilidade e versionamento

- `schema_version` tem a forma `MAIOR.MENOR`.
  - **Versão menor** (por exemplo, `1.1`): acrescenta um tipo de evento ou um campo opcional.
    Dados antigos continuam válidos, e programas antigos continuam funcionando se ignorarem o
    que não conhecem.
  - **Versão maior** (por exemplo, `2.0`): renomeia ou remove um campo, ou muda o significado
    ou o tipo de um campo.
- Cada evento é validado com o schema da **sua** versão. O schema de cada versão deve ser
  preservado.
- Quem lê os eventos deve **ignorar campos e tipos de evento desconhecidos**, em vez de
  falhar. O schema, por sua vez, é rigoroso dentro da própria versão (não aceita campos não
  previstos) para revelar erros de gravação.
- A parte comum é estável; a evolução ocorre principalmente na parte específica. Extensões já
  previstas: novos valores de `activity` (outras atividades do jogo); registro clique a clique
  no tabuleiro (decisão D2); novos tipos de evento, se o jogo ganhar funcionalidades (por
  exemplo, ajuda à criança, hoje inexistente).

## 12. Portabilidade

- Os eventos são texto JSON em UTF-8, com nomes de campos em ASCII, data e hora em ISO 8601 e
  unidades explícitas. Podem ser lidos por qualquer linguagem ou ferramenta de análise.
- As regras estão em JSON Schema, um padrão aberto com validadores em várias linguagens.
- O modelo não depende de Java. No registro atual, o estado do algoritmo (`<aluno>_Alg.txt`) é
  gravado por serialização Java (`DataFileHandler`, l. 381), que só pode ser lida por Java e
  depende da versão das classes.
- A conversão para tabela (CSV), mais prática para planilhas, é tema da exportação (Etapa 6).

## 13. Decisões tomadas

Continuação das decisões D1–D4 do catálogo. Podem ser revistas pelo orientador.

| # | Questão | Decisão | Justificativa |
|---|---|---|---|
| D5 | Estrutura do evento | Parte comum + parte específica em `data` | Leitura uniforme de todos os eventos; novos tipos sem alterar a parte comum |
| D6 | Estilo dos nomes de campos | *snake_case* (`response_time_ms`) | Segue a tabela "Modelo inicial" do plano; comum em ferramentas de análise e em CSV. O exemplo de `evolucao-ads.md` usa *camelCase* (`responseTimeMs`); adotou-se um único estilo |
| D7 | Estímulo, resposta e resultado | Eventos separados; visão por rodada na exportação | Ocorrem em instantes diferentes, e o intervalo entre eles é o que se mede |
| D8 | Tempo de resposta | Medido desde a liberação da resposta; tempo desde o primeiro estímulo também gravado | Definição sem o intervalo de 1,2 s em que a resposta não é aceita, mantendo compatibilidade com o `RT` atual |
| D9 | Prazo esgotado | Evento próprio (`ANSWER_TIMEOUT`), sem tempo de resposta | Evita confundir prazo esgotado com resposta instantânea (`RT = 0`) |
| D10 | Eventos fora da sessão | Não registrados; idioma em `SESSION_START` | Antes da seleção do aluno não há participante; todos os eventos ficam ligados a uma sessão |
| D11 | Menu | Registrado como `PAUSE`/`RESUME` com `trigger = menu` | O jogo pausa ao abrir o menu; evita dois eventos para o mesmo fato |
| D12 | Movimentos | Um tipo (`MOVE`) com o campo `piece` | Os dois movimentos têm a mesma estrutura |
| D13 | Identificadores | UUID para evento e sessão; `sequence_number` por sessão | Unicidade sem coordenação entre computadores; ordem e detecção de perdas |
| D14 | Formato de data e hora | ISO 8601 com milissegundos e fuso | Rodadas com menos de um segundo; comparabilidade entre computadores |

## 14. Limitações e pontos para as próximas etapas

- **Etapa 4:** onde e como gravar os eventos (arquivo, banco de dados, um evento por linha
  etc.).
- **Etapa 5:** atribuição do `participant_id` no cadastro; medição do tempo com relógio
  monotônico; captura dos cliques ignorados (novo ponto no código); falha do registro sem
  interromper o jogo.
- **Etapa 7:** conferir o modelo com eventos gerados pelo jogo modificado, em dados simulados.
- Personagens e animais são registrados por índice, como no código; a correspondência com nomes
  e imagens será documentada na implementação.
- `message_key` usa o nome do áudio; se os áudios forem renomeados (por exemplo, com as
  gravações em português previstas no TCC), a correspondência precisa ser mantida.
