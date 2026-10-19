# Componente de registro dos eventos de interação do Number Race (versão 1)

Produto da **Etapa 5 (desenvolvimento do componente de registro)** do projeto "Modelagem,
registro e persistência de dados de interação no Number Race". Faz parte do
[registro de trabalho](registro-dados-interacao.md) e implementa o
[modelo de eventos de interação](modelo-eventos.md) (Etapa 3) segundo a
[estratégia de persistência](persistencia-eventos.md) (Etapa 4).

Este documento descreve **como o jogo passou a registrar os eventos**: a organização do
componente, os pontos de integração no jogo, a validação, o tratamento de erros, a configuração,
os testes realizados e as decisões tomadas. A exportação dos dados para pesquisadores é tema da
Etapa 6; os testes de integridade, da Etapa 7.

| Local | Conteúdo |
|---|---|
| `numberrace-core/src/main/java/org/unicog/numberrace/analytics/` | Código do componente (10 classes) |
| `numberrace-core/src/main/resources/org/unicog/numberrace/analytics/` | Estrutura do banco (`schema.sql`, cópia de `persistencia-eventos.sql`) e versão do jogo (`analytics.properties`) |
| `numberrace-core/src/test/java/org/unicog/numberrace/analytics/` | Testes automatizados (JUnit 4) |

---

## 1. Requisitos considerados

| Origem | Requisito | Como foi atendido |
|---|---|---|
| Plano, Etapa 5 | Componente desacoplado da lógica principal do jogo | Pacote próprio; apenas uma classe conhece as classes do jogo (seção 2) |
| Plano, Etapa 5 | Geração dos identificadores e associação dos eventos às sessões | Seção 4 |
| Plano, Etapa 5 | Validação antes da persistência | Seção 6 |
| Plano, Etapa 5 | Tratamento de erros e recuperação de falhas | Seção 7 |
| Plano, Etapa 5 | Desenvolvimento incremental, com teste de cada grupo de eventos | Quatro incrementos: estrutura e sessão; partida e rodada; tabuleiro; navegação e recompensas (seção 9) |
| `evolucao-ads.md`, seção 11 | Registro não deve interromper o jogo; funcionamento sem rede; formatos abertos | Gravação em segundo plano; nenhuma exceção chega ao jogo; JSON Lines e SQLite em disco local |
| `persistencia-eventos.md`, seção 7.5 | JSON Lines como registro oficial, SQLite como cópia para consulta, fila e uma linha de execução | Seção 2 |
| `modelo-eventos.md`, seção 14 | Código do participante no cadastro, relógio monotônico, cliques ignorados, falha sem interromper o jogo | Seções 4, 5 e 7 |
| Decisão D22 | Registro atual mantido sem alteração | Os arquivos da pasta `Data` continuam sendo gravados como antes |

## 2. Arquitetura

```
  Jogo (telas e lógica)
        |  uma linha em cada ponto do jogo
        v
  GameEvents ............ traduz o que acontece no jogo em eventos do modelo 1.0
        |
        v
  InteractionLogger ..... parte comum (sessão, participante, sequência, partida, rodada)
        |                 + validação; o jogo continua sem esperar
        v
  fila em memória  --->  linha de execução "interaction-logger" (segundo plano)
                              |
                              +--> JsonLinesSink: events/<participante>/<data>_<sessão>.jsonl
                              |      (registro oficial; gravação forçada no disco a cada evento)
                              |
                              +--> SqliteSink: interaction.db
                                     (cópia para consulta; melhor esforço)

  ParticipantRegistry ... identification/participants.csv (código do participante)
```

O jogo chama `GameEvents` e segue imediatamente. A gravação ocorre numa única linha de execução
em segundo plano, que preserva a ordem dos eventos. A medição da Etapa 4 no Windows (Java 8 de
32 bits) indicou cerca de 25 ms por evento no SQLite; em segundo plano, esse tempo não é
percebido no jogo.

## 3. Classes

| Classe | Responsabilidade |
|---|---|
| `GameEvents` | Adaptador entre o jogo e o componente. Um método por situação do jogo (por exemplo, `answered`, `moved`, `paused`). É a única classe do pacote que importa classes do jogo. Todos os métodos capturam qualquer erro e apenas o registram no log |
| `InteractionLogger` | Núcleo do componente: liga e desliga o registro, abre e fecha sessões, mantém o contexto (partida e rodada atuais), numera os eventos, valida e coloca na fila |
| `InteractionEvent` | Um evento: parte comum e parte específica (`data`); gera a linha JSON na ordem do modelo |
| `EventType` | Os 23 tipos de evento do modelo, com o escopo de cada um (sessão, partida ou rodada) |
| `EventValidator` | Regras de validação aplicadas antes da gravação (seção 6) |
| `Json` | Geração de JSON sem bibliotecas externas |
| `EventSink` | Interface comum aos destinos de gravação |
| `JsonLinesSink` | Grava o arquivo JSON Lines de cada sessão |
| `SqliteSink` | Insere os eventos no banco SQLite |
| `ParticipantRegistry` | Atribui e guarda o código pseudonimizado de cada participante |

Alterações em classes do jogo, além das chamadas a `GameEvents`: métodos de leitura (*getters*)
em `GameTurn` para valores que já eram calculados (coordenadas escolhidas pelo algoritmo e
resultado da rodada), o índice do nível inicial em `NumCompAlgManager`, o nível inicial em
`NumCompManager.getStartComplexityLevel()` e a tela atual em `GameObject.getScreenState()`.
Ao todo, cerca de 120 linhas adicionadas em 7 classes do jogo, sem mudança de comportamento.

## 4. Identificadores, sessão e contexto

| Item | Implementação |
|---|---|
| `event_id`, `session_id` | UUID versão 4 (`java.util.UUID`) |
| `participant_id` | `P` seguido de 6 caracteres sorteados (algarismos de 2 a 9 e letras maiúsculas sem `I`, `L` e `O`), por exemplo `PZRGW99`. Gerado na primeira vez em que o participante é cadastrado ou selecionado e guardado em `identification/participants.csv` (código, sobrenome, nome e data de criação). Nas sessões seguintes, o mesmo sobrenome e nome recebem o mesmo código (decisão D25) |
| `sequence_number` | Contador por sessão, iniciado em 1, incrementado apenas quando o evento é aceito pela validação |
| `game_number`, `turn_number`, `activity` | Contexto mantido pelo `InteractionLogger`: aberto em `GAME_START` e `TURN_START`, fechado em `GAME_END` e no fim da sessão. `activity` vale `number_comparison` |
| `timestamp` | Data e hora local com milissegundos e fuso (ISO 8601) |
| Tempos de resposta | Medidos pelo componente com relógio monotônico (`System.nanoTime`), desde a liberação da resposta e desde o primeiro estímulo (decisão D28) |

**Sessão.** Começa quando o aluno é cadastrado ou selecionado (`SESSION_START`) e termina na troca
de aluno pelo menu (`end_reason = change_student`), na saída pelo menu ou por Shift+Esc (`quit`)
ou no fechamento da janela (`window_closed`). Eventos antes da seleção do aluno não são
registrados (decisão D10 do modelo).

## 5. Pontos de integração no jogo

| Evento | Ponto no código |
|---|---|
| `SESSION_START` | `GameObject.setStudent` |
| `SESSION_END` | `GameObject.stop` (fechamento) e ação "trocar aluno" do menu; o motivo vem de `EndGameAction` e de `windowClosing` |
| `SCREEN_CHANGED` | `GameObject.proceedChanging` |
| `THEME_SELECTED` | `GameObject.setTheme` |
| `CHARACTER_SELECTED` | `GameObject.chooseCharacter` |
| `PAUSE`, `RESUME` | `GameObject.changeState` (menu) e `PauseUnPauseGameAction` (F8) |
| `GAME_START` | `NumCompManager.resetGame` |
| `DIFFICULTY_ADJUSTED` | `NumCompManager.resetGame` (primeira rodada) e `runTurnEndEvents` (rodadas seguintes) |
| `GAME_END` | `NumCompManager.playerWins` |
| `TURN_START` | `NumCompManager.turnBegins`, depois de `setStimsForTurn` |
| `STIMULUS_PRESENTED` | Início de `NumCompManager.showLeftStims` e `showRightStims` |
| `RESPONSE_ENABLED` | Fim de `NumCompManager.showRightStims` |
| `CLICK_IGNORED` | `ChoiceScreen.mousePressed` (ponto novo: clique sobre um dos lados antes da liberação) |
| `ANSWER` e `TURN_RESULT` | `NumCompManager.imFast`, depois da gravação do registro atual |
| `ANSWER_TIMEOUT` e `TURN_RESULT` | `NumCompManager.successfullSneak` |
| `MOVE` | `ChoiceScreen.sqClicked` e movimento do adversário; cliques além da casa correta são contados em `too_far_attempts` |
| `COLLISION` | `ChoiceScreen.resolveColisions` e movimento do adversário |
| `HAZARD` | `ChoiceScreen`, tratamento da armadilha depois de cada movimento |
| `FEEDBACK_PRESENTED` | `ChoiceScreen.opponentTalks` (nome do áudio) |
| `REWARD_SAVED` | `SaveScreen`, depois de guardar o animal |
| `CHARACTER_UNLOCKED` | `CharacterScreen.start` |
| (ligar e desligar) | `GameObject.start` e `GameObject.stop` |

## 6. Validação antes da gravação

Cada evento é conferido pelo `EventValidator` antes de entrar na fila:

- tipo, identificadores e data e hora presentes;
- `participant_id` no formato do modelo (1 a 32 caracteres `A-Z`, `a-z`, `0-9`, `_`, `-`);
- `sequence_number`, `game_number` e `turn_number` maiores ou iguais a 1;
- contexto exigido pelo escopo: eventos de partida exigem `game_number` e `activity`; eventos de
  rodada exigem também `turn_number`;
- campos obrigatórios de `data` presentes e nenhum campo não previsto para o tipo;
- tempos (`_ms`) não negativos.

São as regras de **estrutura** do JSON Schema da Etapa 3, sem depender de uma biblioteca de
validação no jogo (decisão D27). O tipo e o valor de cada campo (por exemplo, `side` ser `left` ou
`right`) não são conferidos durante o jogo: essa conformidade é verificada pelos testes
automatizados, que conferem os eventos gravados contra o JSON Schema oficial (`SchemaConformanceTest`,
acrescentado na validação complementar da Etapa 7; até então este texto dizia que as regras
"correspondem" às do schema). Um evento inválido é **descartado** e registrado no log do jogo, e **não
consome número de sequência**: assim, uma lacuna em `sequence_number` indica perda na gravação,
e não um evento recusado.

## 7. Tratamento de erros e recuperação

| Situação | Comportamento |
|---|---|
| Erro em qualquer método de `GameEvents` | Registrado no log (`WARNING`); o jogo continua |
| O componente não consegue iniciar | Registro desligado, com aviso no log; o jogo funciona normalmente |
| Evento inválido | Descartado, com aviso no log (seção 6) |
| Falha ao gravar o JSON Lines (disco cheio, permissão) | Registrado no log (`SEVERE`); o arquivo é reaberto no evento seguinte |
| O SQLite não abre ou uma inserção falha (driver, biblioteca nativa, arquivo) | Registrado no log; SQLite desativado até o fim da sessão, que segue apenas com JSON Lines; nova tentativa na sessão seguinte. A importação (Etapa 6) completa o banco a partir dos arquivos |
| Falha ao ler ou gravar `participants.csv` | Registrado no log; se a leitura falhar, a sessão usa um código novo; se a gravação falhar, o código não fica guardado para as sessões seguintes |
| Fechamento normal do jogo | `SESSION_END` e espera de até 2 s para esvaziar a fila |
| Interrupção do processo (travamento, falta de energia) | Os eventos já gravados permanecem (gravação forçada no disco a cada evento) |

## 8. Configuração

| Item | Valor |
|---|---|
| Local dos dados | `<diretório da aplicação>/interaction-data/` e `<diretório da aplicação>/identification/`, ao lado da pasta `Data` do registro atual (por padrão, `%USERPROFILE%\NumberRace\v3` no Windows) |
| Biblioteca nativa do SQLite | Extraída para `interaction-data/native/` (`org.sqlite.tmpdir`, decisão D19), salvo se a propriedade já tiver sido definida |
| Registro de cliques ignorados | Ligado por padrão; desligado com `-Dnumberrace.analytics.clickIgnored=false` (decisão D29) |
| Dependências novas (`numberrace-core/pom.xml`) | `org.xerial:sqlite-jdbc:3.53.4.0` (execução) e `junit:junit:4.13.2` (somente testes) |
| Compatibilidade | Código compatível com Java 8 (verificado com `javac --release 8` e com os testes executados em Java 8) |

## 9. Testes e resultados

### 9.1 Testes automatizados

Onze testes JUnit, executados em cada `mvn package`, em Java 8 (Temurin 8u504) e Java 21:

| Classe de teste | O que verifica |
|---|---|
| `JsonTest` (3) | Ordem das chaves, caracteres especiais, números e listas |
| `EventValidatorTest` (2) | Evento completo aceito; contexto ausente, campos obrigatórios ausentes e campos não previstos detectados |
| `ParticipantRegistryTest` (2) | Mesmo participante recebe o mesmo código, inclusive após reabrir o arquivo, e participantes diferentes recebem códigos diferentes; nomes com vírgula e aspas preservados no CSV |
| `InteractionLoggerTest` (4) | Eventos gravados na ordem no JSON Lines e no SQLite; evento inválido descartado sem lacuna na sequência; falha do SQLite sem perda no JSON Lines; nada gravado sem sessão aberta |

Durante o build, os testes de falha produzem avisos no log (SQLite que não abre, evento
descartado). São esperados.

### 9.2 Execução do jogo em ambiente de teste

O jogo modificado foi executado em Linux com Java 21, em tela virtual (Xvfb), com cliques
automatizados (`java.awt.Robot`) e dados simulados, em dois incrementos:

- **Sessão e rodada:** cadastro, escolha do mundo e do personagem, uma rodada respondida e saída
  pelo menu. 11 eventos, todos válidos; `SESSION_END` com `game_in_progress = true`.
- **Tabuleiro e navegação:** duas sessões do mesmo participante (troca de aluno pelo menu e saída
  do jogo). **35 eventos**, de **17 dos 23 tipos**: inclui cliques ignorados antes da liberação
  da resposta, movimento com uma tentativa além da casa correta (`too_far_attempts = 1`), cinco
  falas de retorno, pausa e retomada pela tecla F8 e pelo menu, ajuste de dificuldade e os dois
  motivos de encerramento.

Resultados da segunda execução:

- os 35 eventos são válidos pelo JSON Schema da Etapa 3 e pelas regras de consistência
  (`modelo-eventos.md`, seção 10);
- `sequence_number` de 1 a N em cada sessão, sem lacunas;
- o banco SQLite contém as mesmas 35 linhas;
- o mesmo código de participante nas duas sessões; nenhum nome nos arquivos de eventos;
- nenhum aviso do componente no log; o jogo abriu, funcionou e fechou normalmente;
- o registro atual (`Data/`) continuou sendo gravado.

Os 6 tipos não alcançados pelos cliques automatizados (`GAME_END`, `ANSWER_TIMEOUT`, `COLLISION`,
`HAZARD`, `REWARD_SAVED` e `CHARACTER_UNLOCKED`) exigem partidas completas e foram buscados no
teste no Windows (seção 9.3).

### 9.3 Windows

O jogo modificado foi compilado e executado pelo Eclipse no Windows em 09/10/2026: compilação com o
Java 25 do Eclipse (`clean package`, com os 11 testes; `BUILD SUCCESS`) e execução com o Java 8 de
32 bits (JRE 1.8.0_441). Foi usado um participante de teste, no nível inicial intermediário, em uma
sessão com duas partidas: a primeira jogada até a vitória, com a recompensa guardada; a segunda
encerrada pela saída do jogo pelo menu, depois da segunda rodada. Durante a sessão foram usadas a
pausa pela tecla F8 e pelo menu, cliques antes da liberação da resposta e uma rodada com prazo
esgotado.

| Verificação | Resultado |
|---|---|
| Eventos gravados | 180 eventos de 21 tipos, em 11 rodadas |
| Validação pelo JSON Schema e pelas regras de consistência | 180 eventos válidos, 0 erros |
| `sequence_number` | 1 a 180, sem lacunas nem repetições |
| SQLite | 180 linhas, com a mesma contagem por tipo do arquivo JSON Lines |
| Tipos verificados pela primeira vez | `GAME_END`, `ANSWER_TIMEOUT` (prazo de 4.469 ms), `COLLISION` (4 ocorrências) e `REWARD_SAVED` |
| Posições no tabuleiro | A posição de cada personagem em `TURN_START` corresponde aos movimentos (`MOVE`) e esbarrões (`COLLISION`) da rodada anterior |
| Rodadas | 11 `TURN_START` e 11 `TURN_RESULT` (10 `ANSWER` e 1 `ANSWER_TIMEOUT`); 2 `STIMULUS_PRESENTED` por rodada |
| Tempos de resposta | Diferença entre `response_time_from_first_stimulus_ms` e `response_time_ms` de 1.201 a 1.218 ms, o que corresponde ao intervalo de 1,2 s entre os dois estímulos; `RESPONSE_ENABLED` registrado até 17 ms após o estímulo direito |
| Pausas | 3 `PAUSE` e 2 `RESUME`: F8, menu e, por último, o menu usado para sair do jogo, seguido de `SESSION_END` (`quit`, `game_in_progress = true`) |
| Tentativa além da casa correta | `too_far_attempts = 1` num movimento do adversário, coincidente com a fala `enemy1_opponentTooFar` no log do jogo |
| Log do jogo | Nenhum aviso do componente de registro. Os avisos presentes (sons, `mark not supported`, biblioteca `unsafe`) pertencem ao jogo original |

Dois tipos não ocorreram nesse teste:

- `HAZARD`: uma armadilha esteve no tabuleiro a partir da quarta rodada da primeira partida (casa
  7, penalidade 1), conforme `hazards_on_board`, mas nenhum personagem parou nela (os movimentos
  passaram por ela, por exemplo da casa 6 para a 8). O ponto de integração foi conferido no código
  (`ChoiceScreen.resolveColisions`), e o evento será verificado nos testes da Etapa 7.
- `CHARACTER_UNLOCKED`: exige guardar as sete recompensas de um mundo.

Também foi registrado como `CLICK_IGNORED` um clique ocorrido antes do primeiro estímulo da rodada,
o que está de acordo com a definição do evento (clique antes da liberação da resposta).

## 10. Observações encontradas

| # | Observação | Encaminhamento |
|---|---|---|
| 15 | No registro atual, o rótulo da coluna `controlFor` está **invertido**: quando o jogo mantém a densidade dos pontos constante (`controlDensity = true` em `NumCompManager.setStimsForTurn`), o arquivo grava `itemSize`, e vice-versa (`GameTurn.getControlDensityStr`) | O novo registro grava o valor conforme o comportamento do jogo (`control_for = density` quando a densidade é mantida constante). O registro atual não foi alterado (D22) |
| 16 | O jogo também pausa quando a janela perde o foco (`windowDeactivated`, por exemplo ao clicar em outro programa) e retoma quando ela volta a ter o foco (`windowActivated`), tanto em janela quanto em tela cheia (a exceção é a tela cheia no macOS com Java 7). Essa pausa não é registrada, porque o modelo 1.0 prevê apenas `pause_key` e `menu` | Proposta para a versão 1.1 do modelo: `trigger = window_focus` |
| 17 | Os campos opcionais `display_mode` (`SESSION_START`) e `sound_key` (`FEEDBACK_PRESENTED`) não são preenchidos nesta versão | Podem ser incluídos sem mudança no modelo |

## 11. Decisões tomadas

| # | Decisão | Escolha | Justificativa |
|---|---|---|---|
| D24 | Integração com o jogo | Chamadas de uma linha a `GameEvents` nos pontos do jogo; apenas `GameEvents` conhece as classes do jogo | Desacoplamento com o mínimo de alterações no código legado; a refatoração do jogo (por exemplo, com observadores) fica fora do escopo |
| D25 | Código do participante | `P` + 6 caracteres sorteados, guardado em `identification/participants.csv` e reutilizado pelo mesmo sobrenome e nome | Não revela nome, ordem de cadastro nem quantidade de participantes; não é calculado a partir do nome (modelo, seção 4); baixa chance de coincidência entre computadores. A equipe de pesquisa pode cadastrar outros códigos (por exemplo, `P017`) editando o arquivo |
| D26 | Limites da sessão | Do cadastro ou seleção do aluno até a troca de aluno ou o fechamento do jogo | Corresponde ao uso do jogo por uma criança; o motivo do encerramento fica em `SESSION_END` |
| D27 | Validação | Regras de estrutura em código (campos, contexto, numeração e tempos); o tipo e o valor de cada campo são conferidos contra o JSON Schema nos testes; evento inválido descartado sem consumir número de sequência | Sem nova dependência no jogo; lacunas na sequência passam a indicar apenas perdas na gravação |
| D28 | Medição do tempo | Relógio monotônico do próprio componente | Não é afetado por ajustes do relógio do sistema; independente do `RT` do registro atual |
| D29 | Cliques ignorados | Registrados por padrão, com opção de desligar | Atende à decisão D1 do catálogo (registro opcional) |
| D30 | Geração de JSON | Código próprio (`Json`), sem biblioteca externa | A única dependência nova do jogo é o driver do SQLite |

## 12. Limitações e próximos passos

- A pausa por perda de foco da janela não é registrada (observação 16).
- O código do participante é associado ao sobrenome e ao nome exatamente como digitados: grafias
  diferentes geram códigos diferentes, e homônimos recebem o mesmo código. Códigos editados
  manualmente fora do formato do modelo fazem os eventos da sessão serem descartados (com aviso
  no log).
- Se a gravação do JSON Lines falhar, o evento correspondente fica apenas no SQLite (quando
  ativo); a lacuna aparece em `sequence_number`.
- O componente ainda não informa ao usuário do jogo que o registro está desligado ou com falha;
  isso aparece apenas no log.
- `HAZARD` e `CHARACTER_UNLOCKED` ainda não foram observados em execução (seção 9.3).
- **Etapa 6:** importação do JSON Lines para o SQLite (completar o banco após falhas) e exportação
  dos dados.
- **Etapa 7:** testes de integridade com dados simulados, incluindo interrupção forçada e disco
  cheio, e verificação de `HAZARD` e `CHARACTER_UNLOCKED`.
