# Estratégia de persistência dos eventos de interação do Number Race (versão 1)

Produto da **Etapa 4 (investigação e definição da estratégia de persistência)** do projeto
"Modelagem, registro e persistência de dados de interação no Number Race". Faz parte do
[registro de trabalho](registro-dados-interacao.md) e toma como base o
[modelo de eventos de interação](modelo-eventos.md) (Etapa 3).

Este documento define **onde e como os eventos são guardados**: as alternativas investigadas, a
prova de conceito, a comparação, a decisão e a estrutura de persistência. A implementação no
jogo é tema da Etapa 5; a exportação para pesquisadores, da Etapa 6.

| Arquivo | Conteúdo |
|---|---|
| `persistencia-eventos.md` | Este documento |
| [`persistencia-eventos.sql`](persistencia-eventos.sql) | Estrutura do banco SQLite (tabelas, restrições e índices) |

---

## 1. Necessidades do Number Race

A decisão parte das características do jogo e do seu uso, e não da familiaridade com uma
tecnologia:

1. **Uso sem internet**, em computadores de escola possivelmente antigos. O jogo tem como alvo
   o Java 8 e já foi executado, neste projeto, com um Java 8 de 32 bits no Windows.
2. **Uma falha no registro não pode interromper o jogo** (`evolucao-ads.md`, seção 11).
3. **Gravação contínua durante o jogo; leitura e análise depois**, por pesquisadores e outros
   sistemas.
4. **Reunião de dados de vários computadores**, por exemplo por cópia em mídia removível.
5. **Proteção dos dados** (`evolucao-ads.md`, seção 16): pseudonimização, separação entre
   identificação e registro, possibilidade de exclusão e proibição de dados identificáveis em
   arquivos publicados.
6. **Formatos abertos e versionamento** do esquema.
7. **Convivência com o registro atual.** O arquivo `<aluno>_Alg.txt` guarda o estado do
   algoritmo adaptativo e é lido pelo jogo; por isso o registro atual (`Data/`) é mantido sem
   alteração, e a nova persistência funciona em paralelo. A remoção do registro antigo não faz
   parte deste projeto.

## 2. Alternativas investigadas

| # | Alternativa | Situação |
|---|---|---|
| 1 | **JSON Lines** (`.jsonl`): um evento JSON por linha, acrescentado ao fim do arquivo | Finalista |
| 2 | **Arquivo JSON único** (lista com todos os eventos) | Descartada: exige reescrever o arquivo a cada evento, e uma interrupção durante a escrita pode inutilizar o arquivo inteiro |
| 3 | **CSV** como armazenamento principal | Descartada como armazenamento: os tipos de evento têm campos diferentes, o que gera tabelas esparsas. Mantida como **formato de exportação** (Etapa 6) |
| 4 | **SQLite** (banco de dados relacional em um único arquivo, sem servidor) | Finalista |
| 5 | **Banco em servidor** (por exemplo, MySQL ou PostgreSQL) ou **serviço em nuvem** | Descartada para o funcionamento básico: depende de rede e de infraestrutura externa, o que contraria o funcionamento offline, e expõe dados de crianças em trânsito. Pode ser considerada no futuro para consolidação, fora do jogo |
| 6 | **Serialização Java** (como o `_Alg.txt` atual) | Descartada: legível apenas por Java e dependente da versão das classes |

## 3. Prova de conceito

Para comparar os finalistas com dados, e não apenas com argumentos, foi escrito um programa de
teste em Java, compilado para Java 8, que grava eventos do modelo 1.0 (os 44 eventos de
exemplo da Etapa 3, repetidos com novos identificadores) nos dois formatos. Para o SQLite, foi
usado o driver `org.xerial:sqlite-jdbc` 3.53.4.0 (licença Apache 2.0; 12 MB; classes compatíveis
com Java 8; bibliotecas nativas para Windows x86, x86_64, aarch64 e armv7, Linux e macOS).

### 3.1 Linux, Java 8 (Temurin 8u504, x86_64)

| Medida | JSON Lines | SQLite |
|---|---|---|
| Tempo por evento | 0,05 ms | 1,4 a 1,9 ms (uma transação por evento) |
| Tempo por evento com gravação forçada no disco a cada evento | 0,25 a 0,30 ms | (já incluída na transação) |
| Tamanho de 1.000 eventos | 412 KB | 741 KB |
| Abertura (primeiro uso no processo) | cerca de 13 ms | cerca de 230 ms (extração da biblioteca nativa) |
| Processo interrompido à força durante a escrita (`kill -9`, 3 vezes) | nenhuma linha danificada | `integrity_check = ok`, sem lacunas no `sequence_number` |
| Pasta temporária inexistente | sem efeito | o banco não abre (`SQLException: Error opening connection`, biblioteca nativa não encontrada) |
| Arquivos deixados após interrupção forçada | nenhum | 2 por interrupção (biblioteca nativa e arquivo de trava) |

Também foi verificado que o driver aceita a propriedade `org.sqlite.tmpdir`, que define onde a
biblioteca nativa é extraída, e que essa cópia é apagada quando o processo termina normalmente.

### 3.2 Windows

O mesmo programa foi executado num computador pessoal com Windows, com os dois Java instalados:
o Java 8 usado para rodar o jogo no Eclipse (Oracle 1.8.0_441, 32 bits, "Client VM") e o Java 21
(Temurin 21.0.12.1, 64 bits).

| Medida (1.000 eventos) | Java 8, 32 bits | Java 21, 64 bits |
|---|---|---|
| JSON Lines | 0,66 ms por evento | 0,18 ms por evento |
| JSON Lines com gravação forçada no disco | 1,24 ms por evento | 1,63 ms por evento |
| SQLite (uma transação por evento) | 24,9 ms por evento | 6,2 ms por evento |
| Tamanhos | 412 KB e 741 KB (iguais aos do Linux) | iguais |

O driver do SQLite carregou a biblioteca nativa de 32 bits sem problemas. A gravação no SQLite
foi muito mais lenta que no Linux: cerca de 25 ms por evento no Java 8. Como o jogo gera bem
menos de um evento por segundo, esse tempo é aceitável **desde que a gravação ocorra em segundo
plano** (seção 7.5), sem o jogo esperar. Se necessário, a Etapa 5 pode agrupar eventos próximos
numa mesma transação ou usar o modo WAL.

### 3.3 Limites da prova de conceito

A interrupção forçada do processo não reproduz uma queda de energia, e o disco do servidor de
testes não representa um disco mecânico antigo. Esses casos devem ser considerados nos testes
da Etapa 7.

## 4. Volume esperado

- No arquivo de dados de um teste do jogo, o intervalo entre o início de rodadas consecutivas
  foi de 41 a 69 s (média de 50 s).
- Uma sessão de 25 minutos tem, assim, cerca de 30 rodadas. Com cerca de 12 eventos por rodada,
  mais os de sessão e partida, são **cerca de 400 eventos por sessão**.
- Uma sessão de 400 eventos ocupa **164 KB em JSON Lines e 307 KB em SQLite** (medido).
- Cenário de referência: 30 participantes, 2 sessões por semana, 16 semanas = 960 sessões, cerca
  de 384 mil eventos, **cerca de 160 MB em JSON Lines e 290 MB em SQLite**.

O volume não é um critério restritivo para nenhuma das duas alternativas.

## 5. Comparação

| Critério | JSON Lines | SQLite |
|---|---|---|
| Portabilidade | Texto UTF-8, legível em qualquer sistema | Formato de arquivo estável, documentado e multiplataforma |
| Interoperabilidade | Lido por qualquer linguagem; sem consultas diretas | Lido por qualquer linguagem; consultas em SQL, inclusive sobre o JSON (`json_extract`) |
| Volume | Adequado | Adequado |
| Recuperação após interrupção | Perde no máximo a linha em escrita; demais linhas intactas | Transações; banco íntegro após interrupção |
| Exportação | Exige leitura e conversão | Consultas prontas para exportar |
| Integridade | Depende de validação posterior (schema, `sequence_number`) | Restrições no próprio banco (chave, unicidade, verificações) |
| Funcionamento offline | Sim | Sim |
| Dependências no jogo | Nenhuma | Driver de 12 MB com biblioteca nativa |
| Falha sem interromper o jogo | Riscos mínimos (disco cheio, permissão) | Além desses, falha ao carregar a biblioteca nativa |
| Reunião de vários computadores | Cópia de arquivos | Exige junção dos bancos |
| Arquitetura de referência (`evolucao-ads.md`, seção 8) | Saída JSON | Saída SQLite |

As duas alternativas atendem às necessidades. Foram então avaliadas três arquiteturas:

| Arquitetura | Descrição | Avaliação |
|---|---|---|
| **A** | JSON Lines no jogo; SQLite criado na exportação | Mais simples no jogo, mas sem consultas antes da exportação |
| **B** | SQLite no jogo; JSON Lines apenas quando o SQLite falhar | Consultas imediatas, mas o caminho de reserva só é exercitado em falhas e os dois armazenamentos podem divergir |
| **C** | JSON Lines sempre, como registro oficial; cópia de cada evento no SQLite, para consulta | Reúne a robustez de A e as consultas de B, sem divergência: o JSON Lines é a única fonte |

## 6. Decisão

**Adota-se a arquitetura C: JSON Lines como registro oficial e SQLite como cópia para consulta.**

1. Todo evento é gravado primeiro no arquivo JSON Lines da sessão, que não depende de
   biblioteca externa nem de código nativo.
2. Em seguida, o mesmo evento é inserido no banco SQLite, que oferece consultas em SQL e
   restrições de integridade e segue a arquitetura de referência de `evolucao-ads.md`.
3. Se o SQLite falhar, nenhum evento se perde: a importação (seção 7.6) completa o banco a partir
   dos arquivos JSON Lines.
4. O custo aceito é a dependência de 12 MB no jogo e o teste das duas gravações na Etapa 5.

A arquitetura A foi preterida porque deixaria as consultas para depois da exportação, embora o
SQLite tenha funcionado bem na prova de conceito. A arquitetura B foi preterida porque o
caminho de reserva seria executado apenas em situações de falha, justamente as mais difíceis de
testar, e porque dois armazenamentos alternativos poderiam divergir.

## 7. Estrutura de persistência

### 7.1 Pastas

Os dados ficam no diretório da aplicação já usado pelo jogo (`GamePreferences`; por padrão,
`<pasta do usuário>/NumberRace/v3`), ao lado da pasta `Data` do registro atual:

```
NumberRace/v3/
├── Data/                     registro atual (sem alteração)
├── identification/           correspondência entre código e participante (fora dos dados)
└── interaction-data/
    ├── events/
    │   └── P017/
    │       └── 2026-10-01_140000_f38b2ffc-80a4-4f5a-91c9-bc701e7ea419.jsonl
    ├── interaction.db        banco SQLite (cópia para consulta)
    └── native/               biblioteca nativa do SQLite, extraída durante o uso
```

- A pasta deve estar em **disco local**. A documentação do SQLite desaconselha o uso do banco em
  sistemas de arquivos de rede, comuns em computadores de laboratório com pastas de usuário
  redirecionadas.
- A pasta `identification/` fica **fora** de `interaction-data/`, para que a cópia dos dados de
  interação nunca leve a identificação dos participantes. O formato desse arquivo e a atribuição
  dos códigos serão definidos na Etapa 5.

### 7.2 Nomes dos arquivos de eventos

- Uma pasta por participante (`events/<participant_id>/`).
- Um arquivo por sessão: `<AAAA-MM-DD>_<HHMMSS>_<session_id>.jsonl`, com a data e a hora locais de
  início da sessão (sem dois-pontos, que não são aceitos em nomes de arquivo no Windows) e o
  identificador da sessão, que garante nomes únicos.
- Nenhum nome de arquivo ou de pasta contém nome de pessoa.

Um arquivo por sessão limita o efeito de um arquivo danificado a uma sessão, facilita a coleta
parcial e permite excluir os dados de um participante removendo a sua pasta.

### 7.3 Formato do arquivo JSON Lines

- Texto em UTF-8, sem marca de ordem de bytes (BOM).
- Uma linha por evento, terminada por `\n` (LF) em qualquer sistema operacional.
- Cada linha é um evento completo do modelo 1.x, sem quebras de linha internas.
- O arquivo só recebe acréscimos; nunca é reescrito.
- Na leitura, uma última linha incompleta (interrupção durante a escrita) é ignorada e relatada.

### 7.4 Banco SQLite

- Arquivo `interaction.db`, com a estrutura de [`persistencia-eventos.sql`](persistencia-eventos.sql):
  - tabela `meta`, com a versão de armazenamento (`storage_version = 1`);
  - tabela `event`, com os campos da parte comum do evento em colunas e o evento completo em
    `event_json`;
  - restrições: chave primária `event_id`; unicidade de (`session_id`, `sequence_number`);
    `sequence_number`, `game_number` e `turn_number` maiores ou iguais a 1; `event_json`
    obrigatoriamente um JSON válido (`json_valid`);
  - índices por participante e por tipo de evento.
- Modo de diário padrão (*rollback journal*) com gravação síncrona completa: com o jogo fechado,
  o banco é um único arquivo, que pode ser copiado com segurança. O modo WAL foi mais rápido na
  prova de conceito (0,6 a 0,7 ms por evento), mas cria arquivos auxiliares e não funciona em
  pastas de rede; o ganho não é necessário com o volume esperado.
- A biblioteca nativa é extraída para `interaction-data/native/` (propriedade `org.sqlite.tmpdir`),
  e não para a pasta temporária do sistema. Arquivos restantes de interrupções anteriores podem
  ser removidos ao iniciar.

A estrutura foi verificada com o SQLite do driver (3.53.4, em Java 8) e com o SQLite da
biblioteca padrão do Python (3.45.1): eventos válidos são aceitos; evento repetido e JSON
inválido são recusados.

### 7.5 Gravação durante o jogo

Orientações para a implementação (Etapa 5):

- O jogo entrega cada evento a um componente de registro (por exemplo, `InteractionLogger`,
  como em `evolucao-ads.md`, seção 8) e continua sem esperar. Uma fila em memória e uma única
  linha de execução em segundo plano fazem a gravação, preservando a ordem.
- Para cada evento: (1) acrescenta a linha ao arquivo JSON Lines e força a gravação no disco;
  (2) insere o evento no SQLite, em uma transação.
- Nenhuma exceção do componente de registro chega ao jogo.

| Situação | Comportamento |
|---|---|
| O SQLite não abre (driver, biblioteca nativa, arquivo) | Registra no log do jogo e segue apenas com JSON Lines na sessão |
| Uma inserção no SQLite falha | Registra no log, desativa o SQLite até o fim da sessão e segue com JSON Lines |
| A gravação do JSON Lines falha (disco cheio, permissão) | Registra no log e tenta novamente no evento seguinte; o jogo continua |
| O jogo é fechado normalmente | Esvazia a fila (tempo máximo de 2 s) e fecha os arquivos |
| O jogo é interrompido (travamento, falta de energia) | Perde-se no máximo o evento em gravação; os demais permanecem |

### 7.6 Importação e reunião de dados

Orientações para a exportação (Etapa 6):

- Um importador lê os arquivos JSON Lines de um ou de vários computadores, valida cada evento
  com o JSON Schema do modelo e insere no SQLite os eventos ausentes (`INSERT OR IGNORE`, pela
  chave `event_id`). Pode completar o banco local ou montar um banco consolidado.
- Relata linhas incompletas, lacunas no `sequence_number` e sessões sem `SESSION_END`.
- Como `event_id` e `session_id` são UUID, dados de computadores diferentes não colidem. O
  `participant_id` precisa ser único no estudo; a atribuição dos códigos será definida na
  Etapa 5.

Verificação da ideia com um importador de teste: com 44 eventos no arquivo JSON Lines (mais uma
linha final cortada) e apenas 30 no SQLite, a importação inseriu os 14 que faltavam e relatou a
linha cortada; repetida, não inseriu nada.

### 7.7 Coleta e cópia de segurança

Com o jogo fechado, basta copiar `interaction-data/events/`. Os arquivos JSON Lines são
suficientes para reconstruir o banco. O arquivo `interaction.db` pode ser copiado junto, por
conveniência.

## 8. Portabilidade e interoperabilidade

- **JSON Lines:** texto aberto, legível em qualquer editor e por qualquer linguagem (por exemplo,
  Python com `json` ou `pandas.read_json(lines=True)`; R com `jsonlite::stream_in`).
- **SQLite:** formato de arquivo multiplataforma, lido por Python (`sqlite3`, biblioteca
  padrão), R (`RSQLite`), ferramentas gráficas como o DB Browser for SQLite e outros sistemas. As
  funções JSON do SQLite permitem consultar campos da parte específica. Exemplo verificado:

  ```sql
  SELECT participant_id, COUNT(*), AVG(json_extract(event_json, '$.data.response_time_ms'))
  FROM event WHERE event_type = 'ANSWER' GROUP BY participant_id;
  ```

- Nenhum dos formatos depende de Java para leitura. O driver nativo só é necessário dentro do
  jogo.
- A conversão para CSV, para uso em planilhas, é tema da Etapa 6.

## 9. Integridade

- **Conteúdo:** cada evento é validado pelo JSON Schema do modelo na importação.
- **Completude e ordem:** o `sequence_number` permite detectar eventos ausentes.
- **No banco:** chave primária, unicidade por sessão e sequência, verificações de faixa e de JSON
  válido.
- **Na gravação:** transação no SQLite; acréscimo e gravação forçada no JSON Lines.
- **Fonte oficial:** em caso de diferença entre os dois armazenamentos, prevalece o JSON Lines.

## 10. Proteção dos dados

- Nenhum nome de pessoa no conteúdo, no nome dos arquivos ou no nome das pastas.
- Correspondência entre código e participante guardada em `identification/`, fora dos dados de
  interação, e nunca exportada.
- Exclusão dos dados de um participante: remover a pasta `events/<participant_id>/` e executar
  `DELETE FROM event WHERE participant_id = ?`.
- Acesso protegido pela conta do usuário do sistema operacional. Criptografia dos arquivos não é
  adotada nesta versão (limitação; ver seção 13).
- O prazo de retenção cabe à equipe de pesquisa.
- O repositório contém apenas dados simulados.

## 11. Versionamento e migração

- Cada evento traz a sua `schema_version` (Etapa 3). Eventos de versões diferentes podem
  conviver no mesmo arquivo e na mesma tabela, pois o evento completo fica em `event_json`.
- A `storage_version` (tabela `meta`) só muda se a estrutura das tabelas mudar, por exemplo com
  uma nova coluna da parte comum numa versão maior do modelo.
- **Migração:** como o JSON Lines é a fonte oficial, migrar o banco consiste em recriá-lo com a
  nova estrutura e reimportar os arquivos.

## 12. Decisões tomadas

Continuação das decisões D1–D14 (catálogo e modelo). Podem ser revistas pelo orientador.

| # | Questão | Decisão | Justificativa |
|---|---|---|---|
| D15 | Arquitetura de persistência | C: JSON Lines como registro oficial e SQLite como cópia para consulta | Robustez sem dependências para o registro oficial; consultas e restrições no SQLite; sem divergência entre armazenamentos |
| D16 | Organização dos arquivos | Um arquivo por sessão, numa pasta por participante | Limita danos a uma sessão; facilita coleta e exclusão |
| D17 | Local dos dados | `interaction-data/` no diretório da aplicação, em disco local | Mesmo local configurável já usado pelo jogo; SQLite desaconselhado em pastas de rede |
| D18 | Estrutura do banco | Tabela única com a parte comum em colunas e o evento completo em JSON | Consultas simples sem perder campos; novas versões do modelo sem alterar tabelas |
| D19 | Biblioteca nativa do SQLite | Extraída para `interaction-data/native/` | Independência da pasta temporária do sistema |
| D20 | Gravação | Em segundo plano, com gravação forçada no disco a cada evento; falhas apenas registradas no log | O jogo nunca espera nem é interrompido pelo registro |
| D21 | Identificação dos participantes | Em `identification/`, fora de `interaction-data/` | Separação entre identificação e registro (`evolucao-ads.md`, seção 16) |
| D22 | Registro atual | Mantido sem alteração, em paralelo | O jogo depende do `_Alg.txt`; refatoração fora do escopo |
| D23 | Migração do banco | Recriar o banco e reimportar o JSON Lines | O JSON Lines é a fonte oficial |

## 13. Limitações e próximos passos

- Falta de energia e discos mecânicos antigos não foram testados (Etapa 7).
- Criptografia dos arquivos não foi adotada; pode ser avaliada se a equipe de pesquisa exigir.
- **Etapa 5:** componente de registro, atribuição dos códigos de participante e arquivo de
  identificação.
- **Etapa 6:** importador definitivo, banco consolidado e exportação em CSV.
