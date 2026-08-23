# Projeto Evolução ADS — The Number Race

## 1. Identificação

**Modalidade:** projeto de pesquisa e desenvolvimento para estudantes de Análise e Desenvolvimento de Sistemas (ADS)  
**Software-base:** The Number Race  
**Natureza:** engenharia, modernização, instrumentação e análise de software educacional  
**Integração:** apoio ao Projeto Evolução TCC e a pesquisas futuras

## 2. Contexto

The Number Race é um software educacional legado voltado à aprendizagem matemática. Sua manutenção constitui um cenário real de evolução de sistemas: código existente, dependências antigas, recursos multilíngues, decisões arquiteturais históricas e necessidade de preservar o comportamento enquanto novas funcionalidades são incorporadas.

A versão mantida neste repositório busca preservar uma aplicação funcional e, simultaneamente, transformá-la de forma gradual em uma base sustentável para desenvolvimento, ensino e pesquisa.

Uma das evoluções centrais é a instrumentação do jogo para registrar eventos de interação de forma estruturada. Esses registros poderão sustentar análises individuais, longitudinais e agregadas e, futuramente, ferramentas de visualização, análise estatística e inteligência artificial.

## 3. Problema

O software legado não foi concebido originalmente como uma plataforma moderna de coleta e análise de dados de interação. Além disso, componentes e práticas técnicas herdadas dificultam manutenção, testes, extensibilidade, desempenho e integração com novas ferramentas.

A questão orientadora de engenharia é:

> Como modernizar e instrumentar o Number Race de forma incremental, preservando sua funcionalidade e arquitetura multilíngue, mas tornando-o sustentável, observável e preparado para coleta e análise estruturada de dados?

## 4. Justificativa

O projeto oferece aos estudantes de ADS um ambiente autêntico para aplicar conhecimentos de:

- análise de sistemas;
- engenharia de requisitos;
- arquitetura de software;
- manutenção de sistemas legados;
- programação Java;
- Maven;
- modelagem e persistência de dados;
- testes;
- controle de versão;
- integração;
- visualização;
- análise de dados;
- segurança e privacidade.

As contribuições passam a integrar uma aplicação utilizada por outros estudantes e potencialmente por projetos de pesquisa, em vez de constituírem apenas exercícios isolados.

A instrumentação também permite que o Number Race evolua de uma aplicação que executa atividades para uma plataforma capaz de produzir evidências estruturadas das interações realizadas.

## 5. Objetivo geral

Modernizar e instrumentar o The Number Race para permitir a coleta estruturada, segura e extensível de dados de interação, melhorar sua manutenção e desempenho e preparar a aplicação para análises individuais e agregadas em projetos educacionais e de pesquisa.

## 6. Objetivos específicos

1. compreender e documentar a arquitetura atual;
2. identificar e priorizar dívida técnica;
3. estabelecer uma estratégia incremental de modernização;
4. melhorar o processo de build e desenvolvimento;
5. revisar dependências e componentes legados;
6. melhorar desempenho e responsividade;
7. projetar uma arquitetura desacoplada de telemetria;
8. definir um modelo versionado de eventos;
9. implementar mecanismos de sessão e identificação pseudonimizada;
10. implementar persistência e exportação;
11. desenvolver testes automatizados;
12. criar mecanismos de validação de recursos;
13. produzir indicadores derivados dos eventos;
14. preparar interfaces para visualização e análise;
15. documentar APIs, modelos, decisões e procedimentos;
16. preparar a base para projetos posteriores de analytics e inteligência artificial.

## 7. Frentes de trabalho

O projeto pode ser dividido entre diferentes equipes, turmas ou planos de trabalho.

### Frente A — Modernização técnica

- revisão dos POMs e do reactor Maven;
- estabelecimento de uma baseline Java;
- revisão de dependências;
- remoção gradual de APIs obsoletas;
- tratamento de warnings;
- melhoria dos logs;
- refatoração incremental;
- documentação arquitetural.

### Frente B — Desempenho

- medição da situação inicial;
- cache de imagens e recursos;
- redução de I/O repetitivo;
- revisão de operações na Event Dispatch Thread;
- carregamento assíncrono quando apropriado;
- análise da camada de áudio;
- comparação objetiva antes/depois.

### Frente C — Registro de Eventos

- modelo de eventos;
- sessões;
- esquema de dados;
- logger;
- timestamps;
- contexto das atividades;
- pseudonimização;
- tratamento de falhas;
- versionamento.

### Frente D — Persistência

- JSON;
- CSV;
- SQLite ou alternativa apropriada;
- versionamento do esquema;
- exportação;
- consultas;
- integridade;
- mecanismos de migração quando necessários.

### Frente E — Testes e qualidade

- testes unitários;
- testes de integração;
- testes de regressão;
- validação de recursos;
- verificação dos language packs;
- validação de caminhos de áudio e imagens;
- integração contínua, quando viável.

### Frente F — Visualização e analytics

- indicadores;
- relatórios;
- dashboard;
- análise por sessão;
- evolução temporal;
- análise por atividade;
- agregação por grupos.

### Frente G — Inteligência artificial

Esta frente é posterior e depende de dados adequados, documentação, validação e questões de pesquisa claramente definidas.

Possibilidades incluem:

- sumarização de sessões;
- identificação exploratória de padrões;
- agrupamento;
- análise longitudinal;
- apoio à interpretação de indicadores;
- geração assistida de relatórios.

A IA não deve ser utilizada para produzir diagnóstico clínico automático.

## 8. Arquitetura de referência

A instrumentação deve permanecer desacoplada da lógica principal do jogo.

Uma organização inicial poderá seguir:

```
org.unicog.numberrace
├── analytics/
│   ├── InteractionEvent.java
│   ├── EventType.java
│   ├── InteractionLogger.java
│   ├── SessionManager.java
│   ├── context/
│   ├── persistence/
│   └── exporters/
```

Fluxo conceitual:

```
Number Race
    |
    v
InteractionLogger
    |
    +--> JSON
    |
    +--> CSV
    |
    +--> SQLite
             |
             +--> consultas
             +--> dashboard
             +--> analytics
             +--> IA futura
```

Essa estrutura é uma referência, não uma imposição. Soluções alternativas podem ser adotadas quando justificadas e documentadas.

## 9. Modelo de eventos

Os eventos devem registrar fatos observáveis, evitando incorporar interpretações clínicas ao dado bruto.

Exemplo:

```json
{
  "schemaVersion": "1.0",
  "sessionId": "S001",
  "participantId": "P017",
  "timestamp": "2026-08-23T10:32:18",
  "event": "ANSWER",
  "activity": "number_comparison",
  "level": 3,
  "stimulus": {
    "leftValue": 7,
    "rightValue": 9
  },
  "response": 9,
  "correct": true,
  "responseTimeMs": 1834,
  "attempt": 1,
  "helpUsed": false
}
```

Eventos candidatos:

- `SESSION_START`;
- `SESSION_END`;
- `ACTIVITY_START`;
- `ACTIVITY_END`;
- `STIMULUS_PRESENTED`;
- `ANSWER`;
- `HELP_REQUESTED`;
- `FEEDBACK_PRESENTED`;
- `LEVEL_START`;
- `LEVEL_END`;
- `LEVEL_CHANGED`;
- `PAUSE`;
- `RESUME`;
- `ABANDON`.

O vocabulário definitivo deve possuir documentação própria.

## 10. Indicadores derivados

A infraestrutura deve permitir derivar indicadores sem destruir ou substituir o dado bruto, como:

- quantidade de atividades;
- acertos;
- erros;
- taxa de acerto;
- tempo médio e mediano de resposta;
- distribuição de tempos;
- número de tentativas;
- frequência de ajuda;
- tipos recorrentes de erro;
- progressão entre níveis;
- duração das sessões;
- evolução entre sessões;
- desempenho por atividade;
- desempenho por período;
- medidas agregadas por grupo.

Indicadores devem ser claramente diferenciados de interpretações clínicas.

## 11. Requisitos arquiteturais

A solução deve buscar:

- baixo acoplamento;
- separação de responsabilidades;
- eventos controlados;
- tolerância a falhas de telemetria;
- versionamento do esquema;
- formatos abertos;
- documentação;
- extensibilidade;
- funcionamento offline quando aplicável;
- proteção dos dados;
- ausência de dependência obrigatória de serviços externos para o funcionamento básico do jogo.

Uma falha de registro de evento não deve, sempre que possível, impedir a execução do jogo.

## 12. Estratégia de modernização

### Fase 1 — Baseline

- preservar uma versão funcional;
- documentar ambiente;
- estabilizar build;
- registrar problemas conhecidos;
- criar testes básicos.

### Fase 2 — Melhorias de baixo risco

- logs;
- tratamento de erros;
- cache;
- validação de assets;
- limpeza de código;
- redução de warnings;
- pequenas refatorações.

### Fase 3 — Instrumentação

- eventos;
- sessões;
- persistência;
- exportação;
- testes.

### Fase 4 — Dados e visualização

- consultas;
- indicadores;
- dashboard;
- análises agregadas.

### Fase 5 — Evoluções avançadas

- atualização mais ampla de dependências;
- revisão da camada gráfica;
- integração com outros sistemas;
- analytics avançado;
- IA.

Uma reescrita completa não constitui o objetivo inicial.

## 13. Entregas possíveis

Uma equipe não precisa executar todas as frentes. Cada plano de trabalho deve definir um subconjunto coerente de entregas, por exemplo:

- documentação arquitetural;
- inventário de dívida técnica;
- benchmark de desempenho;
- mecanismo de cache;
- infraestrutura de telemetria;
- esquema de eventos;
- exportador JSON;
- exportador CSV;
- persistência SQLite;
- testes automatizados;
- validador de recursos;
- dashboard;
- biblioteca de indicadores;
- documentação de API;
- pipeline de integração contínua.

## 14. Critérios de qualidade

Cada contribuição deve:

- compilar no ambiente documentado;
- preservar funcionalidades não relacionadas à alteração;
- possuir procedimento de teste;
- evitar coleta de dados pessoais desnecessários;
- manter compatibilidade com a arquitetura multilíngue;
- possuir documentação suficiente;
- reduzir ou não aumentar injustificadamente a dívida técnica;
- diferenciar dado bruto, indicador e interpretação;
- registrar decisões arquiteturais relevantes.

## 15. Apoio ao Projeto Evolução TCC

O projeto de ADS deve fornecer ao TCC uma interface simples para registro das interações.

Idealmente, as telas não devem conhecer detalhes de arquivos, banco de dados ou ferramentas analíticas. Uma operação conceitual como:

```java
interactionLogger.log(event);
```

deve ser suficiente.

O projeto de ADS poderá fornecer:

- biblioteca de eventos;
- gerenciamento de sessões;
- identificadores pseudonimizados;
- persistência;
- exportadores;
- testes;
- documentação;
- exemplos de integração.

Isso permite que os estudantes do Ensino Médio concentrem-se no problema educacional, na interface, na localização e na avaliação.

## 16. Aspectos éticos, segurança e privacidade

Dados de interação de crianças exigem tratamento cuidadoso.

A arquitetura deve prever:

- minimização;
- pseudonimização;
- controle de acesso;
- separação entre identificação e registro de evento;
- retenção definida;
- possibilidade de exclusão quando aplicável;
- proteção de arquivos exportados;
- documentação da finalidade;
- ausência de credenciais e dados sensíveis no repositório;
- proibição de publicação de dados identificáveis.

A disponibilidade de uma infraestrutura de coleta não constitui autorização para pesquisa com participantes. Estudos deverão observar as exigências éticas e institucionais aplicáveis.

## 17. Organização no GitHub

Recomenda-se utilizar:

- **Issues** para bugs, melhorias e atividades;
- **labels** como `audio`, `pt-BR`, `telemetry`, `performance`, `ui`, `documentation`, `research` e `testing`;
- **branches** para desenvolvimento isolado;
- **pull requests** para revisão;
- `CHANGELOG.md` para mudanças relevantes;
- `evolution/` para especificações e decisões de evolução.

Uma issue deve preferencialmente registrar:

- problema;
- comportamento esperado;
- critérios de aceitação;
- componentes envolvidos;
- evidências ou passos para reprodução.

## 18. Resultados esperados

No médio prazo, espera-se obter:

1. base de código mais sustentável;
2. build reproduzível;
3. melhor desempenho e responsividade;
4. arquitetura de registro de eventos documentada;
5. registros estruturados de interação;
6. exportação em formatos abertos;
7. testes automatizados;
8. mecanismos para detectar inconsistências de recursos;
9. suporte técnico ao TCC do Ensino Médio;
10. base para dashboards e análises;
11. capacidade de análises longitudinais e agregadas;
12. infraestrutura preparada para pesquisas futuras.

## 19. Possibilidades de projetos e TCCs em ADS

O projeto poderá originar trabalhos independentes, por exemplo:

- modernização arquitetural de software educacional legado;
- instrumentação e registro de eventos em jogos educacionais;
- arquitetura offline-first para coleta de dados educacionais;
- visualização da progressão em jogos de matemática;
- testes automatizados de recursos multilíngues;
- análise de desempenho e otimização do Number Race;
- dashboards para acompanhamento de interação;
- análise longitudinal de eventos;
- técnicas de agrupamento aplicadas a padrões de interação;
- geração assistida de relatórios a partir da telemetria.

Cada trabalho deverá possuir problema, escopo, método e avaliação próprios.

## 20. Visão de longo prazo

A visão é que o Number Race possa funcionar simultaneamente como:

- jogo educacional;
- objeto de estudo de Engenharia de Software;
- plataforma para TCCs;
- laboratório de manutenção e evolução de sistemas;
- fonte estruturada de dados de interação;
- plataforma de pesquisa sobre aprendizagem matemática.

Essa evolução deve preservar a simplicidade de uso do jogo e evitar que análises computacionais sejam apresentadas como diagnósticos clínicos automáticos.
