# Validação técnica dos registros de interação do Number Race (versão 1)

Produto da **Etapa 7 (testes, validação e integridade dos registros)** do projeto "Modelagem,
registro e persistência de dados de interação no Number Race". Faz parte do
[registro de trabalho](registro-dados-interacao.md) e avalia o
[componente de registro](componente-registro.md) (Etapa 5) e o
[mecanismo de exportação](exportacao-dados.md) (Etapa 6), conforme o
[modelo de eventos](modelo-eventos.md) (Etapa 3) e a
[estratégia de persistência](persistencia-eventos.md) (Etapa 4).

Este documento é o **relatório de validação técnica dos registros**. Ele descreve os cenários de
teste, os dados simulados utilizados, a verificação de integridade incorporada ao exportador, os
testes de falha da gravação, a execução do jogo por um programa de teste, os problemas
encontrados e as correções realizadas. Todos os testes usaram **dados simulados**; nenhum dado de
criança foi coletado.

A seção 15 descreve a **validação complementar** feita em 17/10/2026, depois da conclusão da
etapa: teste diferencial com o JSON Schema, testes de estresse e de robustez, nova execução do jogo
por programa e 38 testes automatizados novos (de 62 para 100). Ela encontrou e corrigiu três
problemas (P10 a P12).

| Local | Conteúdo |
|---|---|
| `numberrace-core/src/main/java/org/unicog/numberrace/analytics/export/IntegrityChecker.java` | Verificação de integridade (21 regras) |
| `numberrace-core/src/test/java/org/unicog/numberrace/analytics/` | Testes automatizados (JUnit 4), entre eles `PersistenceFailureTest` (falhas da gravação) |
| `numberrace-core/src/test/java/org/unicog/numberrace/analytics/export/` | `IntegrityCheckerTest` (estragos propositais), `SimulatedSessions` (gerador), `SimulatedSessionsTest`, `SchemaConformanceTest` (conformidade com o JSON Schema) e `RealDataRegressionTest` (dados do jogo real) |
| `numberrace-core/src/test/resources/org/unicog/numberrace/analytics/export/dados-reais/` | Sessões gravadas pelo jogo real na Etapa 7 (teste no Windows e jogo executado por programa), compactadas |
| [`modelo-eventos.schema.json`](modelo-eventos.schema.json) | JSON Schema do modelo, versão 1.1 (a versão 1.0 está preservada em [`modelo-eventos-1.0.schema.json`](modelo-eventos-1.0.schema.json)) |

---

## 1. Objetivo e escopo

O plano de trabalho pede que sejam testados, quando aplicável: criação de sessão, encerramento de
sessão, apresentação de tarefa, resposta correta, resposta incorreta, diferentes tempos de
resposta, diferentes níveis de dificuldade, mudanças no estado adaptativo, interrupção, retomada,
exportação e múltiplas sessões. Pede também que sejam investigados: eventos duplicados, eventos
ausentes, dados inválidos, inconsistências temporais, perda de registros e inconsistência entre
sessão e eventos. Os termos usados neste documento são:

- **integridade**: os dados não estão errados nem foram alterados;
- **completude**: nenhum evento esperado está faltando;
- **consistência**: as partes do registro concordam entre si (por exemplo, o número de rodadas
  informado no fim da partida e as rodadas registradas).

## 2. Método

### 2.1 Níveis de teste

| Nível | O que verifica | Exemplos |
|---|---|---|
| Unidade | Uma classe isolada | Leitura e escrita de JSON; validação de um evento; escrita de CSV |
| Integração | Classes em conjunto | Registro, gravação e leitura de volta; importação, verificação e exportação |
| Ponta a ponta | O caminho completo, como o usuário | Jogo, gravação, exportação, leitura do pacote em outra ferramenta |

### 2.2 Dados simulados

| Fonte | Descrição | Uso |
|---|---|---|
| Sessão de teste | Sessão jogada por um adulto no Windows na Etapa 5 (participante de teste, 2 partidas, 11 rodadas, 180 eventos, modelo 1.0) | Referência real; base dos estragos propositais (seção 5) |
| Gerador | Programa (`SimulatedSessions`) que produz sessões do modelo 1.1 segundo as regras do tabuleiro, com semente fixa: os mesmos parâmetros produzem sempre os mesmos eventos | Situações raras e múltiplas sessões e participantes (seção 7) |
| Jogo executado por programa | O jogo real, executado num servidor com tela virtual e operado por um programa de teste que aciona os mesmos comandos dos botões (seção 8) | Eventos produzidos pelo jogo modificado, inclusive os que não haviam ocorrido nos testes manuais |
| Teste manual | Roteiro executado no Windows (seção 9) | Ambiente real de uso (Windows, Java 8 de 32 bits) |

O **resultado esperado** de cada caso foi definido antes da execução. Para os estragos
propositais, foi definida previamente a regra que deveria acusar cada um.

### 2.3 Ambientes

| Ambiente | Uso |
|---|---|
| Linux (servidor), Java 8 (Temurin 8u504) e Java 21 | Testes automatizados, gerador, jogo executado por programa (Java 8, tela virtual Xvfb, sem placa de som) |
| Windows, Java 8 de 32 bits (JRE 1.8.0_441), Eclipse | Teste manual (seção 9) |

## 3. Cenários de teste

| # | Item do plano | Cenário | Como foi testado | Resultado |
|---|---|---|---|---|
| C01 | Criação de sessão | Seleção ou cadastro do aluno abre a sessão, com código pseudonimizado | Testes automatizados; jogo executado por programa; Windows (Etapa 5) | Sim |
| C02 | Encerramento de sessão | Saída pelo menu, troca de aluno e fechamento da janela | Jogo executado por programa; Windows | Sim: `quit`, `change_student` e `window_closed`, com `game_in_progress` correto |
| C03 | Encerramento de sessão | Jogo fechado à força | F1 (seção 6), em Linux e Windows; jogo encerrado com `kill -9` (seção 8); Gerenciador de Tarefas no Windows (seção 9) | Sim: eventos anteriores íntegros; sessão sem `SESSION_END` (aviso R04). No Windows, "Finalizar tarefa" encerrou a sessão normalmente (`window_closed`) |
| C04 | Apresentação de tarefa | Início da rodada, dois estímulos e liberação da resposta, nessa ordem | Regra R07 em todos os dados | Sim |
| C05 | Resposta correta | `correct_larger` verdadeiro quando o lado escolhido tem o maior valor | Regra R17; estrago E13 | Sim |
| C06 | Resposta incorreta | Idem, falso | Regra R17 | Sim |
| C07 | Prazo esgotado | Um `ANSWER_TIMEOUT`, sem `ANSWER`, com o prazo da rodada e o menor valor atribuído à criança | Regras R07, R11 e R17 | Sim (com correção da ordem, P1) |
| C08 | Diferentes tempos de resposta | Respostas rápidas, próximas do prazo, sem prazo e com pausa durante a espera | Regras R08 a R10; gerador; jogo executado por programa | Sim; o tempo de resposta inclui o tempo em pausa (P6) |
| C09 | Diferentes níveis de dificuldade | Níveis com algarismos, pontos e fala, somas e subtrações | Regra R18; sessão de teste (níveis 1, 7, 8, 9, 11 e 12) | Sim |
| C10 | Mudanças no estado adaptativo | Cada rodada anunciada por `DIFFICULTY_ADJUSTED` | Regra R16; estrago E16 | Sim |
| C11 | Interrupção | Pausa pela tecla F8, pelo menu e por perda de foco da janela | Regra R12; jogo executado por programa | Sim, após a versão 1.1 do modelo (P3) |
| C12 | Retomada | Retomada após cada tipo de pausa, inclusive F8 seguida de perda de foco | Regra R12; jogo executado por programa | Sim (P4) |
| C13 | Exportação | Pacote completo, conferido e lido por ferramenta independente | Testes automatizados; Python (seção 7.2) | Sim |
| C14 | Múltiplas sessões | Duas sessões do mesmo participante; vários participantes | Gerador; jogo executado por programa | Sim |
| C15 | (tabuleiro) | Movimentos, esbarrões e armadilhas, com casas contínuas | Regras R13 a R15; jogo executado por programa | Sim |
| C16 | (recompensas) | Recompensa a cada vitória; novo personagem após 7 recompensas | Gerador; jogo executado por programa | Sim: recompensas de 1 a 7; `CHARACTER_UNLOCKED` depois da sétima |
| C17 | Eventos duplicados | Cópia repetida e conflito de sequência | Importação; estragos E02 e E03 | Sim |
| C18 | Eventos ausentes | Evento apagado; rodada sem resposta | Regras R01 e R07; estragos E01 e E08 | Sim |
| C19 | Dados inválidos | Campo com valor proibido; tipo errado | Validação na importação; estrago E12; JSON Schema | Sim (P2) |
| C20 | Inconsistências temporais | Relógio do computador ajustado; tempo de resposta incoerente; intervalo sem explicação | Regras R02 e R08; estragos E07 e E09; F5 | Sim (P3) |
| C21 | Perda de registros | Fechamento à força; disco cheio | F1 e F4 | Sim (P5) |
| C22 | Inconsistência entre sessão e eventos | Número de rodadas; partida em andamento no fim; evento de outra sessão | Regras R05, R19 e R20; estragos E06, E11 e E15 | Sim |

## 4. Verificação de integridade

O exportador passou a conferir, em toda exportação, 21 regras que todo registro correto cumpre.
A verificação não altera nem descarta eventos: o resultado é informado no console, no arquivo
`relatorio-integridade.txt`, na tabela `integrity` (arquivos `csv/integrity.csv` e
`excel/integrity.csv` e tabela do banco), nas colunas `integrity_errors` e `integrity_warnings`
de `sessions` e no manifesto. Cada regra tem uma gravidade:

- **erro**: dados incoerentes entre si;
- **aviso**: situação que merece atenção, mas pode ter explicação legítima (por exemplo, o relógio
  do computador ajustado durante a sessão).

| Regra | Gravidade | O que exige |
|---|---|---|
| R01 | Erro | Números de sequência 1, 2, 3... sem lacunas |
| R02 | Aviso | Horários em ordem (um horário que volta indica relógio do computador ajustado) |
| R03 | Erro | A sessão começa com um único `SESSION_START` |
| R04 | Aviso | A sessão termina com um único `SESSION_END` (sem ele, o jogo foi fechado de forma inesperada) |
| R05 | Aviso | Cada arquivo contém eventos de uma única sessão |
| R06 | Erro | Partidas e rodadas numeradas em ordem, coerentes com `GAME_START` e `TURN_START` |
| R07 | Erro | Ordem da rodada: início, estímulo da esquerda, estímulo da direita, liberação, resposta ou prazo esgotado e resultado, cada um uma única vez (a última rodada pode ficar incompleta quando a sessão ou a partida termina no meio dela) |
| R08 | Aviso | Tempo de resposta igual à diferença entre os horários dos eventos (tolerância de 50 ms) |
| R09 | Erro | O tempo desde o primeiro estímulo não é menor que o tempo desde a liberação |
| R10 | Aviso | Resposta dentro do prazo, nas rodadas com prazo e sem pausa durante a espera |
| R11 | Erro | Prazo esgotado só em rodada com prazo, com o prazo informado no início da rodada |
| R12 | Erro | Pausa e retomada se alternam (a sessão pode terminar pausada) |
| R13 | Erro | Casas contínuas: cada movimento, esbarrão ou armadilha parte da casa onde a peça estava; o início da rodada e o fim da partida informam essas casas |
| R14 | Erro | Movimento coerente: avanço igual a destino menos origem, sem passar do fim do tabuleiro; recuo da armadilha igual à penalidade |
| R15 | Erro | Os movimentos da rodada conferem com o resultado calculado pelo jogo (casas avançadas e recuadas) |
| R16 | Erro | Cada rodada é anunciada por um ajuste de dificuldade |
| R17 | Erro | `correct_larger` indica a escolha do maior valor; no prazo esgotado, a criança recebe o menor |
| R18 | Erro | O valor do estímulo é o resultado da conta, e só há contas em níveis com adição ou subtração |
| R19 | Erro | `turns_played` igual às rodadas registradas; o vencedor termina no fim do tabuleiro |
| R20 | Erro | `game_in_progress` verdadeiro somente se havia partida sem `GAME_END` |
| R21 | Aviso | Nova partida iniciada sem o `GAME_END` da anterior |

A saída da criança no meio de uma partida ou de uma rodada é comportamento esperado e não é
tratada como problema: ela fica registrada em `SESSION_END.game_in_progress` e na coluna
`complete` de `turns`.

Antes de ser implementada em Java, a primeira versão das regras foi aplicada à sessão de teste.
Ela revelou que a regra das pausas precisava aceitar uma sessão terminada em pausa (na sessão de
teste, a última pausa foi a abertura do menu, seguida da saída do jogo pelo próprio menu). Sem
essa observação, a regra produziria um falso alarme em sessões normais.

## 5. Estragos propositais

Cópias da sessão de teste foram alteradas de propósito, cada uma com um defeito. Para cada
estrago, a regra que deveria acusá-lo foi definida antes do teste. A sessão original não gerou
nenhum problema (nenhum falso alarme).

| # | Estrago | Esperado | Resultado |
|---|---|---|---|
| E01 | Evento apagado do meio do arquivo (movimento do adversário) | R01 | R01 e R13 (a peça "salta" de uma casa para outra) |
| E02 | Evento copiado duas vezes | Importação ignora a cópia | Cópia ignorada, sem problema relatado |
| E03 | Dois eventos diferentes com o mesmo número de sequência | Importação relata conflito | Conflito relatado |
| E04 | Última linha cortada ao meio | Importação relata linha incompleta; R04 | Como esperado |
| E05 | `SESSION_END` apagado | R04 | R04 |
| E06 | Evento de outra sessão no arquivo | R05; a outra sessão sem início nem fim | R05, R03 e R04 |
| E07 | Horário de um evento uma hora antes do anterior | R02 | R02 |
| E08 | Resposta de uma rodada apagada, com a sequência renumerada | R07 | R07 |
| E09 | Tempo de resposta alterado em 100 ms | R08 | R08 |
| E10 | Peça "teletransportada" (casa de origem e avanço alterados) | R13 | R13 e R15 |
| E11 | Fim de partida com uma rodada a menos | R19 | R19 |
| E12 | Tempo de resposta negativo | Evento rejeitado na importação | Rejeitado; a lacuna resultante é acusada por R01 |
| E13 | Acerto invertido | R17 | R17 |
| E14 | Resultado da conta alterado | R18 | R18 |
| E15 | `game_in_progress` falso com partida em andamento | R20 | R20 |
| E16 | Ajuste de dificuldade anunciando a rodada errada | R16 | R16 |
| E17 | Pausa apagada (retomada sem pausa) | R12 | R12 |
| E18 | Rodada com número trocado | R06 | R06 |

## 6. Falhas na gravação

Requisito da Etapa 4: uma falha no registro não interrompe o jogo e não compromete o que já foi
gravado no arquivo JSON Lines.

| # | Situação simulada | Esperado | Resultado |
|---|---|---|---|
| F1 | Processo encerrado à força durante a gravação (outro processo Java gravando eventos sem pausa, encerrado com `destroyForcibly`) | Linhas anteriores íntegras; no máximo a última incompleta; o banco não tem mais eventos que o arquivo | Como esperado |
| F2 | Banco SQLite bloqueado por outro programa (transação exclusiva aberta) | Jogo não espera; JSON Lines completo | Como esperado: o registro desativa o banco até o fim da sessão |
| F3 | Pasta dos arquivos impossível de criar | Jogo não interrompido; os eventos ficam na cópia do banco | Como esperado |
| F4 | Disco cheio no meio de um evento, com espaço liberado em seguida | O evento cortado fica isolado numa linha; os seguintes continuam legíveis | Falhou na primeira execução: o evento seguinte era gravado na mesma linha do pedaço cortado e também se perdia. Corrigido (P5) |
| F5 | Relógio do computador atrasado em uma hora no meio da sessão | Sequência preservada; horários gravados como informados pelo relógio; R02 acusa | Como esperado |

## 7. Sessões geradas por programa

### 7.1 Gerador

O gerador produz sessões do modelo 1.1 seguindo as regras do jogo: avanço igual ao número
escolhido; esbarrão que faz a outra peça voltar uma casa; armadilhas que permanecem no tabuleiro;
prazo esgotado que atribui à criança o menor número; recompensa a cada vitória e novo personagem
a cada sete recompensas no mesmo tema; pausas pela tecla F8, pelo menu e por perda de foco;
sessão encerrada no meio da partida. O participante simulado pode começar com recompensas já
obtidas, para que o novo personagem seja liberado em poucas partidas.

### 7.2 Resultados

| Cenário | Resultado |
|---|---|
| 3 participantes, 2 sessões cada, até 4 partidas por sessão | 6 sessões e 1.104 eventos; os 23 tipos de evento presentes; importação sem problemas; nenhuma ocorrência nas 21 regras |
| O mesmo, com a última sessão de cada participante sem `SESSION_END` | Somente três avisos R04, um por participante |
| Mesma semente, duas execuções | Eventos idênticos |
| Validação independente pelo JSON Schema 1.1 (Python, `jsonschema`) | 2.173 eventos válidos, nenhum inválido |
| Exportação dos dados gerados | 6 sessões de 3 participantes; uma linha de `turns` por `TURN_START`; tabela `integrity` vazia |
| Volume: os 108.000 eventos de 600 sessões usados na Etapa 6, em Java 8 com 128 MB de memória | Exportados e verificados em 23 s (cerca de 20 s antes da verificação de integridade); nenhuma ocorrência |

O gerador e a verificação de integridade foram escritos pela mesma equipe e poderiam compartilhar
um mesmo engano. Por isso, os resultados foram confirmados de duas formas independentes: pelo
JSON Schema, executado em Python, e pela execução do jogo real (seção 8). Além disso, o programa
de conferência em Python (`conferir_pacote.py`, usado na Etapa 6) passou a refazer, de forma
independente, sete das regras (R01, R02, R04, R07, R08, R12 e R13) e a comparar o resultado com a
tabela `integrity`.

## 8. Execução do jogo por programa

O jogo modificado foi executado num servidor Linux com Java 8, com tela virtual (Xvfb) e sem
placa de som (o reprodutor de sons foi ajustado somente na cópia de teste para não falhar sem
dispositivo de áudio). Um programa de teste, executado no mesmo processo do jogo, lê o estado das
telas e aciona os mesmos comandos dos botões: cadastro e seleção do aluno, tema, personagem,
início da rodada, escolha de um lado, contagem das casas da criança e do adversário, recompensa e
tela das recompensas. Em rodadas sorteadas, o programa responde errado, deixa o prazo esgotar,
pausa pela tecla F8, abre e fecha o menu, abre outra janela por cima do jogo (perda de foco) e
clica numa casa além da correta. O programa de teste não faz parte do componente entregue, porque
depende de detalhes internos das telas do jogo.

### 8.1 Execuções

| Execução | Participante simulado | O que fez | Fim da sessão |
|---|---|---|---|
| 1 | A | Cadastro, início da primeira partida | Processo encerrado com `kill -9` (equivalente ao Gerenciador de Tarefas) |
| 2 | A (segunda sessão) | Oito partidas no mesmo tema, até liberar um personagem; pausas pela tecla F8, pelo menu e por perda de foco; respostas erradas e prazos esgotados de propósito | Saída pelo menu, no meio da nona partida |
| 3 | B (nível inicial intermediário) | Uma partida completa com prazos frequentes; perda de foco | Troca de aluno pelo menu, no meio da segunda partida |
| 3 (continuação) | C | Cadastro e três rodadas; pausa pela tecla F8 | Janela fechada (botão fechar) |

### 8.2 Resultados

| Verificação | Resultado |
|---|---|
| Eventos | 1.345 eventos de 4 sessões e 3 participantes, todos na versão 1.1 do modelo e válidos pelo JSON Schema |
| Tipos de evento | 22 dos 23. `CLICK_IGNORED` não ocorre porque o programa não clica antes da liberação da resposta; ele foi registrado no teste manual da Etapa 5 |
| Partidas e rodadas | 13 partidas iniciadas e 9 terminadas, 87 rodadas, níveis de complexidade de 1 a 20, 38 rodadas com prazo; algarismos, pontos e fala |
| Respostas | 81 respostas (77 com o maior número) e 6 prazos esgotados |
| Tabuleiro | 165 movimentos, 14 armadilhas, 3 esbarrões e 8 cliques além da casa correta |
| Recompensas | 9 recompensas, com os sete tipos (`reward_type` de 1 a 7, inclusive o 1, que antes da correção P2 seria gravado como 0); `CHARACTER_UNLOCKED` (personagem 1, tema selva) após a sétima |
| Pausas | Pela tecla F8 (8), pelo menu (3) e por perda de foco (10, com `trigger = window_focus`), todas com a retomada correspondente, exceto as duas aberturas do menu que encerraram a sessão |
| Prazo esgotado | Nos 6 casos, a ordem `ANSWER_TIMEOUT`, `TURN_RESULT` e fala do adversário (correção P1) |
| Encerramentos | `quit`, `change_student` e `window_closed`, com `game_in_progress` verdadeiro nos três; a sessão encerrada à força ficou sem `SESSION_END`, com os 9 eventos anteriores íntegros |
| Verificação de integridade | Nenhum erro nas 21 regras; um aviso R04 (a sessão encerrada à força), como esperado |
| Conferência independente (Python) | Pacote conferido sem erros; as sete regras refeitas em Python deram o mesmo resultado |

A sessão longa (execução 2, 1.100 eventos, com 13 armadilhas e esbarrões) confirmou no jogo real
as regras do tabuleiro usadas na verificação (R13 a R15), que até então tinham sido conferidas
com a sessão de teste e com o gerador.

## 9. Execução no Windows

O roteiro foi executado em 11/10/2026 num computador com Windows, pelo Eclipse. A compilação e os
testes automatizados usaram o Java do próprio Eclipse (Java 25); o jogo e o exportador, o Java 8
de 32 bits (JRE 1.8.0_441). Foi cadastrado um participante de teste novo, com nível inicial
intermediário.

| Verificação | Resultado |
|---|---|
| Compilação e testes (`clean package`) | 62 testes: 61 aprovados e 1 ignorado (gravação dos dados simulados); `BUILD SUCCESS`. Inclui o teste F1, que encerrou à força um segundo processo Java no Windows |
| Primeira sessão (158 eventos) | Duas respostas erradas; um prazo esgotado, na ordem `ANSWER_TIMEOUT`, `TURN_RESULT` e fala do adversário (P1); sete pausas por perda de foco, todas com retomada (P3); uma pausa pela tecla F8 seguida de perda e recuperação do foco, registrada como `PAUSE` (`pause_key`) e `RESUME` (`window_focus`) (P4); duas armadilhas (a criança e o adversário na casa 15); vitória e recompensa com `reward_type` 2; `display_mode = in_window` (P8) |
| Encerramento pelo Gerenciador de Tarefas | Esperado: sessão sem `SESSION_END` (aviso R04). Obtido: `SESSION_END` com `end_reason = window_closed` e `game_in_progress` verdadeiro. No Windows, a opção "Finalizar tarefa" da aba de processos primeiro pede que a janela se feche, como o botão fechar, e o jogo encerrou a sessão normalmente. Ao abrir o Gerenciador de Tarefas, a janela do jogo perdeu o foco e a sessão terminou em pausa (`PAUSE` com `window_focus`), o que a regra R12 admite. O encerramento forçado propriamente dito, sem aviso ao programa, foi verificado no próprio Windows pelo teste F1 e no servidor pelo jogo encerrado com `kill -9` |
| Segunda sessão (39 eventos) | `session_number` 2 do mesmo participante; um clique antes da liberação da resposta (`CLICK_IGNORED`); saída pelo menu no meio de uma rodada (`PAUSE` com `menu` e `SESSION_END` com `quit`, `game_in_progress` verdadeiro) |
| Exportação (as duas sessões e a sessão de teste da Etapa 5) | 377 eventos aceitos, das versões 1.0 e 1.1 do modelo; nenhum problema na importação; nenhum erro nem aviso nas 21 regras; pacote conferido |
| Conferência independente (feita depois, no servidor) | 377 eventos válidos pelo JSON Schema 1.1; as sete regras refeitas em Python sem ocorrências |
| Console da compilação | Avisos do Java 25 sobre "métodos restritos" chamados pela biblioteca do SQLite (acesso a código nativo). Não afetam o jogo, executado em Java 8 (seção 13) |

O resultado diferente do esperado no encerramento pelo Gerenciador de Tarefas mostra a utilidade
de definir o resultado esperado antes do teste: a diferença revelou um comportamento do Windows,
e não um defeito do registro.

## 10. Problemas encontrados e correções

| # | Problema | Como foi encontrado | Correção |
|---|---|---|---|
| P1 | No prazo esgotado, a fala do adversário era registrada antes do próprio `ANSWER_TIMEOUT` (a fala era acionada no início de `NumCompManager.successfullSneak`, e o registro do prazo esgotado, no fim, depois da gravação do registro atual) | Comparação da ordem esperada dos eventos de uma rodada com a sessão de teste | A fala passou para o fim do método, depois do registro do prazo esgotado e do resultado da rodada. A ordem passou a ser a mesma da rodada com resposta |
| P2 | `REWARD_SAVED.reward_type` era gravado de 0 a 6 (índice do jogo), mas o modelo define de 1 a 7. O primeiro animal (índice 0) produziria um evento inválido pelo JSON Schema | Comparação sistemática dos limites do JSON Schema com os valores gravados pelo componente | Gravação do índice mais um. Eventos 1.0 gravados antes da correção (apenas dados de teste) podem conter os valores de 0 a 6 |
| P3 | A pausa automática do jogo quando a sua janela perde o foco não era registrada (observação 16 da Etapa 5). O tempo parado aparecia como um intervalo sem explicação | Análise de inconsistências temporais | Versão 1.1 do modelo: `trigger = window_focus` em `PAUSE` e `RESUME`; registro no `GameObject` |
| P4 | Com o jogo pausado pela tecla F8, a perda e a recuperação do foco retomam o jogo, mas o registro continuava indicando pausa | Leitura do código durante a correção de P3 | Com P3, essa retomada passa a ser registrada (`RESUME` com `trigger = window_focus`) |
| P5 | Depois de uma gravação interrompida no meio de uma linha (disco cheio), o evento seguinte era gravado na mesma linha e também se perdia | Teste F4 | Antes de reabrir o arquivo, o registro termina a linha incompleta |
| P6 | O tempo de resposta inclui o tempo em que o jogo ficou pausado durante a espera pela resposta (o mesmo ocorre no registro atual) | Teste com pausa durante a espera | Mantido, por ser o tempo efetivamente decorrido; documentado. A regra R10 desconsidera essas rodadas, e a tabela `turns` informa as pausas da rodada |
| P7 | O tempo de resposta do registro atual (`NumCompManager`) é medido com o relógio do computador, que pode ser ajustado durante a sessão; o do componente novo usa um cronômetro (`System.nanoTime`) | Análise de inconsistências temporais | Sem alteração no registro atual (decisão D22); documentado |
| P8 | `SESSION_START.display_mode` não era preenchido (observação 17 da Etapa 5) | Revisão das observações da Etapa 5 | Preenchido com `fullscreen` ou `in_window` |
| P9 | Nos níveis com números falados, os números do personagem da criança não são reproduzidos: o jogo procura o som pelo algarismo (por exemplo, `friend1_2`), mas `SoundManager` registra esses sons pelo nome do número (`friend1_two`); ouvem-se apenas "mais", "menos" e "igual". O mesmo ocorre na contagem das casas da criança (`friend1_counting_1` procurado, `friend1_counting_counting_one` registrado). Faltam ainda dois arquivos no pacote em português (`Friend1/Numbers/ten.wav` e `Enemy1/Numbers/deux_ennemi.wav`), e não há áudio para números acima de 10 | Registro de execução do jogo por programa (sons solicitados sem correspondência: `friend1_2` 69 vezes, `friend1_3` 65 vezes, entre outros) e teste manual com os áudios em português | Fora do escopo do componente de registro; correção proposta em contribuição separada. Consequência para os dados: `TURN_START.representation.spoken` informa a condição prevista pelo nível, não a reprodução efetiva do som, enquanto o defeito não for corrigido |
| P10 | A importação aceitava `game_number` ou `turn_number` menores que 1 e campos desconhecidos na parte comum do evento, que o JSON Schema recusa. O banco SQLite do pacote recusa números menores que 1, e um único evento assim fazia o pacote inteiro sair **sem o banco** | Validação complementar: teste diferencial e exportação de eventos alterados (seção 15) | O validador passou a recusar esses eventos, que aparecem no relatório de importação como inválidos; o resto é exportado normalmente, com o banco |
| P11 | O arquivo `identification/participants.csv` deixava de ser reconhecido depois de aberto e salvo no Excel em português (que grava com ponto e vírgula e na codificação Windows-1252). O participante recebia um código novo, e as sessões seguintes deixavam de ficar ligadas às anteriores | Validação complementar: revisão do código e teste com um arquivo no formato do Excel | Leitura tolerante: cada linha é lida com vírgula ou ponto e vírgula, em UTF-8 ou em Windows-1252; espaços nas pontas dos campos são ignorados |
| P12 | Uma linha corrompida com milhares de níveis de listas ou objetos (`[[[[...`) esgotava a pilha de execução e interrompia a exportação inteira, sem pacote | Validação complementar: teste de robustez do leitor de JSON | Limite de 64 níveis na leitura (os eventos do modelo têm no máximo quatro); a linha passa a ser relatada como inválida |

## 11. Testes automatizados

Os testes automatizados passaram de 33 (Etapas 5 e 6) para 62 na conclusão da etapa e para 100 na
validação complementar (seção 15), executados em cada `mvn package` (cerca de 22 s). Todos passaram
em Java 8 (Temurin 8u504) e Java 21; um deles grava os dados simulados numa pasta e só é executado
quando essa pasta é informada. O código foi compilado também com o compilador do Java 8, sem avisos
nas classes do registro e da exportação.

| Classe de teste | Testes | O que verifica |
|---|---|---|
| `IntegrityCheckerTest` | 18 | Sessão de teste sem nenhum problema; estragos E01 a E18 |
| `SimulatedSessionsTest` | 5 | Sessões geradas válidas e coerentes, com os 23 tipos de evento; exportação de várias sessões e participantes; sessões sem `SESSION_END`; repetição com a mesma semente |
| `PersistenceFailureTest` | 5 | Falhas F1 a F5 |
| `EventValidatorTest` | 5 (+1) | Versão 1.1 rigorosa; `window_focus` aceito na 1.1 e recusado na 1.0 |
| `ExportToolTest` | 4 | Pacote com os arquivos da verificação de integridade |
| Demais (Etapas 5 e 6) | 25 | Sem alteração |

Testes acrescentados na validação complementar (17/10/2026):

| Classe de teste | Testes | O que verifica |
|---|---|---|
| `SchemaConformanceTest` | 5 | Exemplos do modelo, eventos do jogo real (1.722) e sessões simuladas válidos pelo JSON Schema oficial, conferido por uma biblioteca independente; teste diferencial com cerca de 2.500 alterações de eventos reais: o validador do componente nunca recusa um evento válido pelo modelo, e as alterações de estrutura são recusadas pelos dois |
| `RealDataRegressionTest` | 4 | Exportação das sessões do jogo real (teste no Windows e jogo executado por programa): contagens, ocorrências de integridade, as duas versões do modelo juntas e o mesmo resultado em exportações repetidas, com as entradas em outra ordem |
| `JsonRoundTripTest` | 5 | O que é gravado volta igual (textos com aspas, acentos, emoji e caracteres de controle; números); linhas cortadas ou corrompidas são recusadas sem outra exceção; aninhamento exagerado (P12) |
| `CsvRoundTripTest` | 3 | Textos com separadores, aspas e quebras de linha voltam iguais nas duas versões do CSV; números sem notação científica; BOM só na versão do Excel |
| `InteractionLoggerConcurrencyTest` | 4 | Oito linhas de execução registrando ao mesmo tempo (sequência e ordem certas); o jogo não espera um destino lento; um destino com defeito não impede os outros; eventos depois de desligar são ignorados |
| `GameEventsRobustnessTest` | 1 | Todos os métodos do adaptador chamados com valores ausentes ou estranhos, sem nenhuma exceção chegar ao jogo |
| `ImportEdgeCasesTest` | 5 | BOM, fim de linha do Windows e linhas em branco; lixo binário, linha de 1 MB e aninhamento exagerado; evento com partida 0 não impede o banco (P10); pasta inexistente; pasta de identificação nunca lida |
| `FilterAndVerifierTest` | 3 | Filtros por data (extremos incluídos), participante e sessão, sempre com sessões inteiras; conferência acusa arquivo alterado, apagado e manifesto estragado |
| `ParticipantRegistryTest` | +6 | Códigos únicos no alfabeto sem ambiguidade; espaços; código cadastrado pela equipe; arquivo salvo pelo Excel em português (P11) e como CSV UTF-8; pasta impossível |
| `EventValidatorTest` | +2 | Partida e rodada a partir de 1 (P10); campo desconhecido na parte comum |

Cada teste de correção foi conferido também ao contrário: com o código anterior à correção, ele
falha.

## 12. Decisões tomadas

| # | Decisão | Escolha | Justificativa |
|---|---|---|---|
| D41 | Onde fica a verificação de integridade | Dentro do exportador, executada em toda exportação, sem impedir a exportação | Quem recebe os dados recebe junto o resultado da verificação; um problema num dado não impede a entrega dos demais |
| D42 | Gravidade | Erro (dados incoerentes) e aviso (pode ter explicação legítima) | Distingue defeito do registro de situações como relógio ajustado ou jogo fechado à força |
| D43 | Saída no meio da partida | Não é problema; registrada em `game_in_progress` e na coluna `complete` | É comportamento esperado da criança; tratá-la como problema geraria falsos alarmes |
| D44 | Pausa por perda de foco | Modelo 1.1 com `trigger = window_focus`; schema 1.0 preservado; validação rigorosa nas versões conhecidas | Elimina intervalos sem explicação; versão menor compatível (seção 11 do modelo) |
| D45 | Ordem no prazo esgotado | `ANSWER_TIMEOUT` e `TURN_RESULT` antes da fala do adversário | A ordem dos eventos deve seguir a ordem dos fatos |
| D46 | Numeração das recompensas | `reward_type` de 1 a 7, como no modelo | O componente deve seguir o modelo; o índice interno do jogo começa em 0 |
| D47 | Gravação interrompida | A linha incompleta fica isolada | Preserva os eventos seguintes |
| D48 | Tempo de resposta com pausa | Mantido como medido e documentado | É o tempo decorrido; alterar a medida mudaria o significado em relação ao registro atual |
| D49 | Dados para os testes | Sessão de teste, gerador com semente fixa, jogo executado por programa e teste manual no Windows | Testes repetíveis, situações raras e verificação no jogo real, sem dados de crianças |

## 13. Limitações

- A verificação de integridade confere a coerência interna do registro, não a correção das regras
  do jogo nem do algoritmo adaptativo.
- As regras R13 a R15 (tabuleiro) foram confirmadas com 15 partidas do jogo real (2 da sessão de
  teste e 13 do jogo executado por programa) e com as partidas geradas; situações raras do tabuleiro (por exemplo, esbarrão seguido de
  armadilha) podem exigir ajuste.
- O programa de teste que opera o jogo não usa o mouse: aciona diretamente os comandos dos
  botões. A conversão do clique em comando (incluindo os cliques antes da liberação da resposta)
  foi verificada nos testes manuais.
- Não foram testados o sistema operacional macOS nem o modo tela cheia.
- Em Java 24 ou mais novo, a biblioteca do SQLite produz avisos de acesso a código nativo; versões
  futuras do Java poderão exigir a opção `--enable-native-access=ALL-UNNAMED`. O jogo é executado em
  Java 8, sem esse efeito.
- Enquanto o problema P9 não for corrigido, `representation.spoken` indica a condição prevista pelo
  nível e não garante que os números foram ouvidos.
- O teste de disco cheio foi simulado por programa; não foi executado com um disco realmente
  cheio.
- O validador usado durante o jogo confere a estrutura do evento (campos, contexto, numeração e
  tempos), e não o tipo e o valor de cada campo. Um valor fora do modelo produzido por um defeito
  futuro do jogo seria gravado; ele é detectado pelos testes de conformidade com o JSON Schema
  (`SchemaConformanceTest`), que precisam ser executados a cada mudança.

## 14. Como reproduzir

Na pasta `numberrace/`:

```
mvn clean package
```

executa os 100 testes. Os testes de conformidade leem o JSON Schema oficial em
`evolution/pt/modelo-eventos.schema.json`, procurado a partir da pasta em que os testes rodam. Para gravar os dados simulados numa pasta, para conferência por outras
ferramentas:

```
mvn -pl numberrace-core -am test -Dtest=SimulatedSessionsTest -Dsurefire.failIfNoSpecifiedTests=false -Dsimulados.saida=<pasta>
```

Para exportar esses dados e verificar a integridade (com a pasta `lib` usada para executar o
jogo):

```
java -cp "lib/*" org.unicog.numberrace.analytics.export.ExportTool --entrada <pasta>/escola/interaction-data
```

Para conferir o pacote de forma independente, em Python (`jsonschema` e `pandas`), com o
programa `conferir_pacote.py` (disponível com o autor):

```
python conferir_pacote.py <pasta do pacote> evolution/pt/modelo-eventos.schema.json
```

## 15. Validação complementar (17/10/2026)

Depois da conclusão da etapa, todo o trabalho entregue foi conferido de novo, com o objetivo de
encontrar o que os testes anteriores não tinham coberto.

| Verificação | Como foi feita | Resultado |
|---|---|---|
| Ramos enviados | Compilação e testes de cada ramo com Pull Request aberto, em Java 21; o último também em Java 8 | Todos aprovados; nenhum arquivo fora do escopo nos ramos |
| Validador × modelo (teste diferencial) | 10.312 alterações de 169 eventos representativos, tirados de 3.451 eventos reais e simulados: campo removido, valor de outro tipo, número fora da faixa, campo a mais. Cada alteração foi julgada pelo JSON Schema, com duas bibliotecas independentes (Python e Java, que concordaram em todos os casos), e pelo validador do componente | O validador nunca recusou um evento válido pelo modelo. Ele deixava passar números de partida e rodada menores que 1 e campos a mais na parte comum (P10). Depois da correção, as diferenças restantes são só o tipo e o valor dos campos de `data`, conferidos pelos testes de conformidade |
| Estresse da exportação | Os 5.473 eventos inválidos pelo modelo e aceitos pelo validador, cada um numa sessão; e 600 sessões completas com um evento alterado (95.810 eventos) | Nenhuma falha da exportação; 18.508 erros e 10.763 avisos de integridade relatados no primeiro caso. Antes da correção P10, o banco do pacote não era gerado |
| Leitor de JSON | 36.635 linhas cortadas em cada posição ou com caracteres trocados, e linhas com 10.000 e 100.000 níveis de aninhamento | Nenhuma exceção inesperada nas linhas cortadas; o aninhamento profundo interrompia a exportação (P12), corrigido |
| Linha de comando | Opção sem valor, opção desconhecida, data inválida, período invertido, pasta inexistente, conferência sem manifesto, filtros sem resultado | Mensagens e códigos de saída corretos; a mensagem de pasta inexistente passou a mostrar o caminho informado |
| Código | Leitura do registro, da gravação, dos códigos de participante, dos filtros e da escrita de CSV | Problema P11 (arquivo de participantes editado no Excel). Reforço: se um destino de gravação deixar escapar um erro, a gravação nos outros continua (antes, a linha de execução de gravação pararia) |
| Documentação | Afirmações conferidas contra o código | A decisão D27 dizia que as regras do validador eram equivalentes ao JSON Schema; o texto foi corrigido em `componente-registro.md`: as regras conferem a estrutura, e o tipo e o valor de cada campo são conferidos nos testes |
| Jogo real executado por programa | Duas execuções do jogo com o código corrigido, pelo mesmo programa de teste da seção 8 (Java 8, sem placa de som), num computador cujo arquivo `identification/participants.csv` foi preparado no formato do Excel em português (ponto e vírgula e Windows-1252), com o participante `P017` cadastrado pela equipe. A primeira execução foi interrompida pelo limite de tempo do programa de teste no meio da sétima partida, o que equivale a um encerramento forçado; a segunda terminou com a troca de aluno e o fechamento da janela | 3 sessões, 2 participantes e 984 eventos da versão 1.1, de 21 tipos: 13 armadilhas, 10 prazos esgotados, recompensas de seis tipos, pausas por tecla, menu e perda de foco e encerramentos por troca de aluno, fechamento da janela e interrupção. O participante `P017` foi reconhecido nas duas execuções (sessões 1 e 2), e o participante novo foi acrescentado ao mesmo arquivo; relido depois, o arquivo com as duas formas continuou reconhecendo os três participantes. Com o código anterior, o mesmo arquivo dava um código novo ao `P017` (P11). Nenhum erro nas 21 regras; um aviso R04, o esperado para a sessão interrompida |

Os testes automatizados que registram essas verificações estão na seção 11.
