# Exportação dos dados de interação do Number Race (versão 1)

Produto da **Etapa 6 (exportação e disponibilização dos dados)** do projeto "Modelagem, registro
e persistência de dados de interação no Number Race". Faz parte do
[registro de trabalho](registro-dados-interacao.md) e completa o
[componente de registro](componente-registro.md) (Etapa 5), segundo a
[estratégia de persistência](persistencia-eventos.md) (Etapa 4).

Este documento descreve **como os dados saem do computador onde o jogo foi usado**: os formatos
investigados, o formato adotado (um pacote de exportação), as tabelas produzidas, a reunião de
dados de vários computadores, o uso do exportador, a validação dos arquivos, os testes e as
decisões tomadas.

| Local | Conteúdo |
|---|---|
| `numberrace-core/src/main/java/org/unicog/numberrace/analytics/export/` | Exportador (12 classes) |
| `numberrace-core/src/test/java/org/unicog/numberrace/analytics/export/` | Testes automatizados (JUnit 4) |
| `numberrace-core/src/test/resources/org/unicog/numberrace/analytics/export/sessao-teste.jsonl` | Sessão de teste gravada pelo jogo modificado (participante de teste, 180 eventos) |
| `numberrace/tools/exportar.bat` | Execução com dois cliques no Windows |

---

## 1. Requisitos considerados

| Origem | Requisito | Como foi atendido |
|---|---|---|
| Plano, Etapa 6 | Exportar uma sessão, várias sessões ou todos os registros disponíveis | Filtros por sessão, participante e período; sem filtros, exporta tudo (seção 7) |
| Plano, Etapa 6 | Preservar a relação entre sessão e eventos | Sessões exportadas sempre inteiras; `session_id` em todas as tabelas; tabela de sessões |
| Plano, Etapa 6 | Estrutura dos dados, preservação dos campos, identificação de sessões e eventos | Dado bruto exportado sem alteração, ao lado das tabelas; identificadores do modelo mantidos |
| Plano, Etapa 6 | Portabilidade, facilidade de análise, interoperabilidade | Formatos abertos (JSON Lines, CSV, SQLite); tabela por rodada; versões do CSV para o Excel em português e para R e Python |
| `evolucao-ads.md`, seção 13 | Exportador JSON e exportador CSV | Seção 3 |
| `evolucao-ads.md`, seções 14 e 16 | Diferenciar dado bruto, indicador e interpretação; proteção de arquivos exportados; proibição de dados identificáveis | Tabelas derivadas sem indicadores nem interpretação; identificação nunca lida; orientações no LEIA-ME |
| `persistencia-eventos.md`, seção 7.6 | Importador que reúne arquivos de vários computadores, valida, insere os ausentes e relata problemas | Seção 6 |

## 2. Formatos investigados

| Formato | Pontos a favor | Pontos contra | Uso no pacote |
|---|---|---|---|
| JSON Lines | Preserva todos os campos do modelo; leitura direta em R e Python | Pouco prático em planilhas | Dado bruto (`events.jsonl`) |
| CSV | Abre em planilhas, R, Python e outros programas | Tipos de evento com campos diferentes; diferenças de separador e decimal entre países; conversões automáticas do Excel | Tabelas de eventos, sessões e rodadas |
| SQLite | Consultas em SQL; restrições de integridade; um único arquivo | Requer programa próprio (por exemplo, DB Browser for SQLite) | Banco consolidado (`interaction.db`) |
| Planilha do Excel (`.xlsx`) | Abre sem conversão no Excel | Formato de um fabricante; biblioteca adicional no Java | Não adotado (o CSV atende) |
| JSON único | Comum em sistemas web | Arquivo precisa ser lido por inteiro | Não adotado |
| Parquet | Eficiente para grandes volumes | Desproporcional ao volume; difícil de abrir | Não adotado |

**Teste com o Excel em português.** Dois arquivos CSV com as 11 rodadas da sessão de teste foram
abertos no Excel (configuração brasileira): a versão com vírgula como separador e ponto decimal e
a versão com ponto e vírgula e vírgula decimal. Somente a segunda abriu em colunas com dois
cliques. Além disso, as contas gravadas como texto (`6-2` e `8-6`) foram convertidas
automaticamente em datas (6 de fevereiro e 8 de junho). Esses resultados orientaram as decisões
D33 e D34.

## 3. Formato adotado: o pacote de exportação

O exportador grava uma pasta nova a cada execução (e, opcionalmente, um `.zip` com a mesma
pasta):

```
exportacao_AAAA-MM-DD_HHMMSS/
├── LEIA-ME.txt                 explicação do pacote, em português
├── manifest.json               data, versões, filtros, quantidades e SHA-256 de cada arquivo
├── relatorio-importacao.txt    problemas encontrados nos arquivos de origem
├── events.jsonl                dado bruto: os eventos exportados, sem alteração
├── interaction.db              SQLite: tabela event (como no jogo) e tabelas session e turn
├── csv/                        separador vírgula, ponto decimal, UTF-8 sem BOM
│   ├── events.csv
│   ├── sessions.csv
│   ├── turns.csv
│   └── dicionario-dados.csv
└── excel/                      separador ponto e vírgula, vírgula decimal, UTF-8 com BOM
    └── (os mesmos quatro arquivos)
```

- O **dado bruto** (`events.jsonl`) é a fonte oficial; as tabelas são visões derivadas dele e
  podem ser refeitas a qualquer momento.
- O **manifesto** permite conferir que nenhum arquivo foi alterado ou perdido na cópia (seção 9).
- O **dicionário de dados** descreve cada coluna: tipo, unidade, significado e evento de origem.
- A pasta `identification/`, com a correspondência entre código e participante, **nunca é lida**
  pelo exportador.

## 4. Tabelas exportadas

| Tabela | Uma linha por | Colunas | Conteúdo |
|---|---|---|---|
| `events` | evento | 11 | Parte comum do evento em colunas e a parte específica (`data`) em JSON (`data_json`) |
| `sessions` | sessão | 26 | Dados de `SESSION_START` e `SESSION_END`, duração e contagens (partidas, rodadas, respostas, prazos esgotados, pausas, recompensas, eventos ausentes) |
| `turns` | rodada | 52 | Condições da rodada (`TURN_START` e `DIFFICULTY_ADJUSTED`), estímulos, resposta, tempos, resultado e o que aconteceu no tabuleiro (casas no início e no fim, esbarrões, armadilhas, tentativas além da casa correta) |

Regras das tabelas derivadas (`sessions` e `turns`):

- reorganizam e contam o que está nos eventos; não calculam taxas, médias nem outros
  indicadores, que ficam para as análises posteriores (`evolucao-ads.md`, seção 10);
- as contas apresentadas como estímulo ficam em colunas separadas (`*_operator`,
  `*_operand_1`, `*_operand_2`), além do valor (`*_value`);
- em rodadas com prazo esgotado, `response = timeout`, o lado atribuído fica em
  `assigned_side`, e os campos de acerto e de tempo de resposta ficam vazios, para não confundir
  ausência de resposta com resposta errada ou instantânea (decisão D9 do modelo);
- as casas no fim de cada rodada são calculadas a partir dos movimentos, esbarrões e armadilhas
  registrados, e devem coincidir com as casas no início da rodada seguinte (verificação usada
  nos testes).

## 5. Regras dos arquivos CSV

| Item | `csv/` (padrão) | `excel/` (Excel em português) |
|---|---|---|
| Separador | vírgula | ponto e vírgula |
| Decimal | ponto | vírgula |
| Codificação | UTF-8 sem BOM | UTF-8 com BOM |
| Fim de linha | CRLF (RFC 4180) | CRLF |
| Verdadeiro/falso | `true` / `false` | `true` / `false` |
| Valor ausente | célula vazia | célula vazia |

Os nomes das colunas seguem o modelo de eventos (*snake_case*, decisão D6). Os tempos são
números inteiros em milissegundos, sem casas decimais. Textos que contêm separador, aspas ou
quebra de linha são gravados entre aspas.

## 6. Reunião de dados de vários computadores (importação)

Antes de gravar o pacote, o exportador:

1. lê todos os arquivos `.jsonl` das pastas indicadas (por exemplo, pastas `interaction-data`
   copiadas de vários computadores), ignorando a pasta `identification/`;
2. valida cada evento com as regras da Etapa 5 e com verificações adicionais do formato
   (identificadores no formato UUID, data e hora ISO 8601, números inteiros); aceita qualquer
   versão 1.x do modelo, tolerando tipos e campos novos de versões posteriores (modelo, seção 11);
3. junta os eventos pelo `event_id`: um evento repetido com o mesmo conteúdo é contado e ignorado;
   eventos diferentes com o mesmo `event_id`, ou com a mesma sessão e o mesmo `sequence_number`,
   são relatados como conflito, mantendo-se o primeiro;
4. relata linhas incompletas (gravação interrompida no fim do arquivo), linhas que não são JSON,
   eventos inválidos, eventos ausentes (lacunas em `sequence_number`) e sessões sem
   `SESSION_START` ou sem `SESSION_END`.

O relatório aparece na tela e é gravado no pacote (`relatorio-importacao.txt`). O banco
`interaction.db` de cada computador não é usado: o pacote é montado a partir dos arquivos
JSON Lines, que são o registro oficial (decisão D15).

## 7. Uso

O exportador é um programa separado do jogo, no mesmo `numberrace-core`
(`org.unicog.numberrace.analytics.export.ExportTool`), executado:

- pelo Eclipse: `ExportTool.java` → Run As → Java Application (opções em Run Configurations →
  Arguments);
- pela linha de comando: `java -cp "lib/*" org.unicog.numberrace.analytics.export.ExportTool [opções]`;
- no Windows, com dois cliques em `exportar.bat`, colocado na pasta do jogo, ao lado da pasta
  `lib` (exporta tudo, gera também o `.zip` e abre a pasta de exportações).

Sem opções, lê `<pasta do usuário>/NumberRace/v3/interaction-data` e grava em
`<pasta do usuário>/NumberRace/v3/exportacoes`.

| Opção | Efeito |
|---|---|
| `--entrada <pasta>` | Pasta com arquivos `.jsonl`; pode ser repetida para reunir vários computadores |
| `--saida <pasta>` | Onde criar a pasta do pacote |
| `--participante <código>` | Somente as sessões do participante (pode ser repetida) |
| `--sessao <id>` | Somente a sessão (identificador completo ou o seu início; pode ser repetida) |
| `--de AAAA-MM-DD`, `--ate AAAA-MM-DD` | Somente as sessões iniciadas no período |
| `--zip` | Também gera um `.zip` com o pacote |
| `--verificar <pasta>` | Confere um pacote já gerado com o seu manifesto |
| `--ajuda` | Mostra as opções |

Códigos de saída: 0 (concluído), 1 (erro, por exemplo nenhuma sessão encontrada ou opção
inválida) e 2 (conferência com problemas).

## 8. Proteção dos dados

- O pacote contém apenas códigos pseudonimizados; a pasta `identification/` nunca é lida.
- O manifesto e o relatório não contêm caminhos completos do computador (que poderiam conter o
  nome do usuário), apenas nomes relativos dos arquivos.
- O LEIA-ME orienta a guarda em local com acesso restrito, a não publicação e o descarte conforme
  o protocolo da pesquisa, e lembra que os dados não constituem diagnóstico.
- A criptografia do pacote não foi adotada nesta versão (limitação; seção 12).

## 9. Validação dos arquivos produzidos

Ao final de cada exportação, o pacote é conferido automaticamente com o manifesto, e a mesma
conferência pode ser repetida depois, por quem recebe o pacote, com `--verificar`:

- cada arquivo listado existe e tem o tamanho e o SHA-256 registrados;
- `events.jsonl` tem uma linha JSON por evento, na quantidade registrada;
- cada CSV tem a quantidade de linhas registrada e o mesmo número de colunas em todas as linhas;
- o banco tem um registro na tabela `event` por evento exportado.

## 10. Testes e resultados

### 10.1 Testes automatizados

Vinte e dois testes novos (JUnit 4), somados aos onze da Etapa 5, executados em cada
`mvn package`. Os 33 testes passaram em Java 8 (Temurin 8u504) e Java 21. O código também foi
compilado com o compilador do Java 8 (`javac` 1.8.0_504), sem avisos.

| Classe de teste | O que verifica |
|---|---|
| `JsonTest` (+2) | Leitura de JSON (textos com escapes, números, listas, objetos) e recusa de JSON inválido |
| `EventValidatorTest` (+2) | Validação de eventos lidos de arquivo; tolerância a tipos e campos novos em versões 1.x posteriores |
| `EventImporterTest` (6) | Sessão completa sem problemas; dois computadores com os mesmos dados sem repetição; linha cortada no fim do arquivo; evento ausente e evento inválido; conflito de sequência; pasta de identificação ignorada |
| `TablesTest` (5) | Uma linha por rodada, com casas contínuas entre rodadas; contas em colunas separadas; prazo esgotado sem resposta nem tempo; resumo da sessão; tabela de eventos |
| `CsvWriterTest` (3) | Versões padrão e Excel (separador, decimal, BOM, aspas); leitura de volta com os mesmos valores |
| `ExportToolTest` (4) | Exportação completa, conferida, com banco e `.zip`; conferência que acusa arquivo alterado; filtros; opções inválidas |

### 10.2 Execução com dados de teste

| Cenário | Resultado |
|---|---|
| Sessão de teste da Etapa 5 (1 participante, 2 partidas, 180 eventos) | 180 eventos, 11 rodadas e 1 sessão exportados; pacote conferido |
| Três "computadores": a sessão de teste, 264 eventos simulados de 3 participantes (exercício da Etapa 4, com a última linha de um arquivo cortada e uma pasta `identification/`) e uma cópia da primeira pasta | 443 eventos de 7 sessões; 180 repetidos ignorados; a linha cortada e a sessão sem `SESSION_END` relatadas; pasta de identificação não lida |
| Volume: 600 sessões e 30 participantes (108.000 eventos, 45 MB) em Java 8 com 128 MB de memória | Exportação e conferência concluídas em cerca de 20 s |

### 10.3 Leitura independente dos arquivos

Os pacotes dos três cenários foram conferidos com um programa em Python, independente do
exportador (bibliotecas `jsonschema`, `pandas` e `sqlite3`), em 31 verificações, sem erros:

- SHA-256 e tamanho de todos os arquivos conferem com o manifesto;
- todos os eventos de `events.jsonl` são válidos pelo JSON Schema da Etapa 3, sem `event_id`
  repetido;
- as tabelas de `csv/` e de `excel/` têm os mesmos valores (lidas com as configurações de cada
  versão);
- as contagens são coerentes entre os arquivos (uma linha de `turns` por `TURN_START`, uma linha
  de `sessions` por sessão, soma dos eventos por sessão igual ao total);
- o dicionário descreve todas as colunas das três tabelas;
- o banco tem as tabelas completas e todo `event_json` é JSON válido;
- o `.zip` está íntegro e completo.

### 10.4 Windows

O exportador foi compilado e executado pelo Eclipse no Windows em 11/10/2026, com o Java 8 de 32
bits (JRE 1.8.0_441), sobre os dados gravados no teste da Etapa 5 (uma sessão, 180 eventos).

| Verificação | Resultado |
|---|---|
| Compilação e testes (`clean package`) | Na primeira execução, 1 dos 33 testes falhou: ao baixar o código, o Git converteu o fim de linha do arquivo de dados de teste para o padrão do Windows (CRLF), e a comparação do arquivo exportado com o original acusou a diferença. O teste passou a desconsiderar o fim de linha, e um arquivo `.gitattributes` mantém os dados de teste no formato do jogo (LF). Depois da correção, `BUILD SUCCESS` |
| Exportação | 1 arquivo, 180 eventos aceitos, nenhum problema encontrado; pacote conferido automaticamente com o manifesto |
| `excel/turns.csv` aberto com dois cliques no Excel | Uma informação por coluna; as contas da direita da rodada 8 da partida 1 (`-`, `6`, `2`) e da rodada 1 da partida 2 (`-`, `8`, `6`) em colunas separadas, sem conversão em data; colunas `difficulty_*` com vírgula decimal; na rodada com prazo esgotado, `response = timeout`, `assigned_side = left` e acerto e tempos vazios |
| Conferência após alteração de um valor em `csv/turns.csv` (um número trocado por outro com a mesma quantidade de dígitos) | `--verificar` acusou "impressão digital (SHA-256) diferente do manifesto"; o tamanho e o número de linhas não mudaram, e a alteração foi detectada pela impressão digital |
| Console | Mensagens em português com acentos corretos no Eclipse |

## 11. Decisões tomadas

| # | Decisão | Escolha | Justificativa |
|---|---|---|---|
| D31 | Onde a exportação é executada | Programa separado do jogo (`ExportTool`), no mesmo `numberrace-core`, por linha de comando, pelo Eclipse ou por `exportar.bat` | Funciona tanto no computador da escola quanto no do pesquisador; mesmo Java 8 do jogo; sem internet; não fica ao alcance das crianças; reaproveita a validação da Etapa 5 |
| D32 | Formato | Pacote com dado bruto (JSON Lines), tabelas em CSV, banco SQLite, dicionário de dados, LEIA-ME, relatório e manifesto | Atende pesquisadores que usam planilhas, R, Python ou SQL, sem perder campos |
| D33 | Separador e decimal do CSV | Duas versões: `csv/` (padrão internacional) e `excel/` (Excel em português) | O teste com o Excel mostrou que só a versão com ponto e vírgula abre em colunas com dois cliques; R e Python usam o padrão internacional |
| D34 | Contas nos estímulos | Operador e números em colunas separadas | O Excel converteu `6-2` e `8-6` em datas |
| D35 | Tabelas derivadas | Uma linha por sessão e por rodada, só com reorganização e contagens | Facilita a análise sem misturar dado bruto, indicador e interpretação |
| D36 | Reunião de computadores | Junção pelo `event_id`; repetições idênticas ignoradas; conflitos e eventos inválidos relatados, mantendo-se o primeiro evento | Permite juntar cópias parciais ou repetidas sem duplicar dados |
| D37 | Seleção | Filtros por sessão, participante e período, sempre com sessões inteiras | Atende "uma sessão, várias sessões ou tudo" e preserva a relação entre sessão e eventos |
| D38 | Fonte dos dados | Arquivos JSON Lines; o `interaction.db` de cada computador não é lido | O JSON Lines é o registro oficial (D15); o banco pode estar incompleto |
| D39 | Integridade do pacote | Manifesto com SHA-256, conferido ao final e com `--verificar` | Quem recebe o pacote consegue confirmar que nada mudou nem se perdeu |
| D40 | Uso de memória | Linhas guardadas em UTF-8 e tabela de eventos gravada sem ficar inteira na memória | Exportar dados de uma escola inteira com o Java 8 de 32 bits, que tem pouca memória disponível |

## 12. Limitações e próximos passos

- O pacote não é criptografado; a proteção depende de onde ele é guardado.
- O exportador não lê pastas em outro local configurado no jogo; nesse caso, a pasta deve ser
  indicada com `--entrada`.
- Não há janela gráfica: o uso é por dois cliques (exportar tudo), pelo Eclipse ou por opções na
  linha de comando. Uma janela pode ser avaliada se a equipe de pesquisa pedir.
- O repositório não tem um pacote de distribuição do jogo; o `exportar.bat` pressupõe a pasta
  `lib` usada para executar o jogo, com o `sqlite-jdbc` incluído.
- A leitura dos arquivos foi testada em Python; a leitura em R está descrita no LEIA-ME, mas não
  foi executada.
- **Etapa 7:** testes de integridade com dados simulados, incluindo `HAZARD` e
  `CHARACTER_UNLOCKED`, interrupção forçada e disco cheio, e conferência da exportação com esses
  dados.
