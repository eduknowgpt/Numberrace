# Projeto Evolução TCC — The Number Race

## 1. Identificação

**Modalidade:** Trabalho de Conclusão de Curso — Ensino Médio Integrado  
**Software-base:** The Number Race  
**Tema:** dificuldades de aprendizagem matemática e discalculia  
**Natureza:** evolução e avaliação de software educacional

## 2. Contexto

The Number Race é um jogo educacional voltado ao desenvolvimento de habilidades relacionadas ao senso numérico e à aprendizagem matemática. O projeto possui uma base de software legada, arquitetura multilíngue e recursos visuais e sonoros que podem ser preservados e evoluídos.

A versão mantida neste repositório incorpora esforços de reorganização, correções técnicas e suporte ao português brasileiro. Essa evolução permite considerar o jogo não apenas como recurso educacional, mas também como fonte estruturada de evidências sobre as interações realizadas durante as atividades.

Este projeto será desenvolvido inicialmente por estudantes do Ensino Médio Integrado e terá como eixo a utilização e evolução do Number Race no contexto de dificuldades de aprendizagem matemática, com atenção à discalculia.

## 3. Problema

A versão legada apresenta limitações relacionadas à interface, localização e registro sistemático das interações.

Sem instrumentação adequada, informações como acertos, erros, tempos de resposta, tentativas, progressão e utilização de ajuda podem não permanecer disponíveis após uma sessão. Isso limita o acompanhamento longitudinal e a possibilidade de utilizar o jogo como fonte estruturada de dados.

A questão orientadora inicial é:

> Como adaptar e evoluir o Number Race para oferecer uma experiência adequada em português brasileiro e produzir registros estruturados das interações que possam subsidiar o acompanhamento da aprendizagem matemática?

A formulação definitiva da questão de pesquisa deverá ser estabelecida no TCC em conjunto com o orientador.

## 4. Justificativa

Dificuldades persistentes na aprendizagem matemática podem afetar significativamente o percurso escolar. Recursos digitais podem apoiar atividades de aprendizagem e, quando adequadamente instrumentados, produzir evidências complementares sobre como o estudante interage com tarefas numéricas.

Evoluir o Number Race permite partir de uma aplicação educacional existente e funcional, evitando que o TCC seja consumido pela construção de um novo jogo.

O trabalho também oferece aos estudantes uma experiência real de evolução de software, envolvendo compreensão de código legado, localização, interface, testes, documentação e integração de mecanismos de registro.

O projeto **não tem como finalidade diagnosticar discalculia**. Eventuais interpretações clínicas não pertencem ao escopo dos estudantes e devem ser realizadas por profissionais habilitados em condições metodológica e eticamente adequadas.

## 5. Objetivo geral

Adaptar e evoluir o The Number Race, com ênfase na experiência de uso em português brasileiro e no registro estruturado das principais interações realizadas durante as atividades, produzindo dados que possam subsidiar estudos sobre aprendizagem matemática e acompanhamento de progressão.

## 6. Objetivos específicos

1. estudar fundamentos sobre discalculia e dificuldades de aprendizagem matemática relevantes ao projeto;
2. estudar o propósito, funcionamento e principais atividades do Number Race;
3. revisar e completar a localização para português brasileiro;
4. integrar, revisar e testar recursos de áudio em português;
5. identificar problemas de usabilidade nas telas selecionadas;
6. propor e implementar melhorias incrementais de interface;
7. identificar interações relevantes para acompanhamento;
8. integrar ao jogo o mecanismo de registro de eventos;
9. verificar a correção e consistência dos registros;
10. realizar testes funcionais e de usabilidade compatíveis com o TCC;
11. produzir análises descritivas básicas a partir dos registros;
12. documentar desenvolvimento, avaliação, resultados, limitações e possibilidades de continuidade.

## 7. Escopo

### 7.1 Incluído

- revisão da experiência de uso em telas selecionadas;
- localização PT-BR;
- textos e instruções em português;
- integração e teste de áudios em português;
- correções diretamente relacionadas à localização;
- modernização visual incremental;
- integração com a infraestrutura de registro de interação;
- registro de eventos essenciais;
- exportação ou inspeção dos registros;
- testes funcionais;
- testes de usabilidade definidos no projeto;
- análise descritiva básica;
- documentação.

### 7.2 Fora do escopo

Não constitui responsabilidade principal dos estudantes deste TCC:

- reescrever completamente o Number Race;
- substituir toda a tecnologia gráfica;
- modernizar todas as dependências legadas;
- construir infraestrutura complexa de banco de dados;
- desenvolver sistema clínico;
- diagnosticar discalculia;
- desenvolver IA para diagnóstico;
- construir dashboard profissional completo;
- realizar aprendizado de máquina ou análise estatística avançada.

Essas atividades podem integrar o Projeto Evolução ADS ou trabalhos posteriores.

## 8. Questões orientadoras

Os estudantes devem investigar questões como:

1. quais características do Number Race são relevantes para o contexto estudado?
2. quais dificuldades de uso existem na versão atual?
3. quais adaptações são necessárias para uma experiência consistente em português brasileiro?
4. quais eventos representam evidências úteis da interação do usuário?
5. como registrar essas evidências sem prejudicar a execução do jogo?
6. quais informações podem ser extraídas de uma sessão?
7. quais informações permitem observar mudanças entre diferentes sessões?

## 9. Entregas

### 9.1 Versão PT-BR funcional

Deve contemplar, dentro do escopo definido:

- interface em português;
- mensagens revisadas;
- instruções localizadas;
- áudios em português;
- correções de referências inconsistentes de recursos.

### 9.2 Interface revisada

As telas prioritárias deverão ser selecionadas com o orientador. Exemplos:

- tela inicial;
- identificação ou seleção do jogador;
- seleção de cenário;
- seleção de personagem;
- instruções;
- telas principais das atividades;
- feedback de acerto e erro;
- encerramento da sessão.

As mudanças devem priorizar clareza, legibilidade, consistência, acessibilidade e facilidade de uso.

### 9.3 Registro das interações

O jogo deverá registrar eventos essenciais definidos no modelo de telemetria, por exemplo:

- início e término da sessão;
- início e término de atividade;
- estímulo apresentado;
- resposta;
- acerto ou erro;
- tempo de resposta;
- número de tentativas;
- utilização de ajuda;
- nível;
- progressão;
- pausa;
- abandono.

### 9.4 Dados estruturados

Os registros deverão poder ser inspecionados ou exportados em formato estruturado, preferencialmente JSON e/ou CSV.

### 9.5 Testes

Devem ser documentados:

- cenários funcionais;
- testes de localização;
- testes de áudio;
- testes da interface modificada;
- testes dos registros de interação;
- problemas encontrados;
- correções realizadas;
- regressões identificadas.

### 9.6 Trabalho escrito

O TCC deverá documentar:

- fundamentação;
- trabalhos relacionados;
- Number Race e sua versão inicial;
- problema;
- objetivos;
- método;
- evolução realizada;
- modelo de registro utilizado;
- avaliação;
- resultados;
- limitações;
- trabalhos futuros.

## 10. Exemplo de evento

O esquema definitivo será definido pelo projeto de telemetria. Um evento poderá assumir forma semelhante a:

```json
{
  "sessionId": "S001",
  "participantId": "P017",
  "timestamp": "2026-08-23T10:32:18",
  "event": "ANSWER",
  "activity": "number_comparison",
  "level": 3,
  "leftValue": 7,
  "rightValue": 9,
  "answer": 9,
  "correct": true,
  "responseTimeMs": 1834,
  "attempt": 1,
  "helpUsed": false
}
```

O registro deve representar fatos observáveis da interação. Interpretações educacionais ou clínicas não devem ser incorporadas como se fossem dados brutos.

## 11. Etapas sugeridas

### Etapa 1 — Fundamentação e compreensão

- estudar discalculia e dificuldades de aprendizagem matemática;
- estudar artigos relacionados ao Number Race;
- executar a versão funcional;
- compreender os fluxos e atividades principais.

### Etapa 2 — Diagnóstico

- mapear telas;
- revisar localização;
- verificar áudios;
- identificar problemas de usabilidade;
- identificar eventos relevantes;
- definir prioridades com o orientador.

### Etapa 3 — Evolução

- revisar PT-BR;
- integrar áudios;
- modernizar telas selecionadas;
- integrar a telemetria;
- corrigir problemas encontrados.

### Etapa 4 — Testes

- definir cenários;
- executar testes;
- verificar registros;
- avaliar regressões;
- documentar resultados.

### Etapa 5 — Avaliação e escrita

- organizar resultados;
- analisar dados descritivamente;
- discutir achados;
- registrar limitações;
- propor continuidade;
- concluir o TCC.

## 12. Critérios de conclusão

O projeto estará tecnicamente apto à conclusão quando, dentro do escopo aprovado:

- a versão puder ser compilada e executada;
- o fluxo principal funcionar em português;
- os áudios selecionados forem reproduzidos corretamente;
- as telas previstas tiverem sido revisadas;
- os eventos essenciais forem registrados;
- os registros puderem ser inspecionados ou exportados;
- os testes estiverem documentados;
- as alterações relevantes estiverem registradas no repositório;
- o TCC documentar claramente o processo e os resultados.

## 13. Aspectos éticos e privacidade

Como o projeto poderá envolver crianças e dados educacionais, devem ser observados princípios de:

- minimização de dados;
- pseudonimização;
- controle de acesso;
- separação entre identificação e registro de dados, quando necessária;
- finalidade explícita;
- retenção definida;
- proteção dos arquivos;
- não publicação de dados identificáveis;
- não utilização dos resultados como diagnóstico clínico automático.

A existência da funcionalidade de coleta **não autoriza automaticamente a realização de pesquisa com participantes**. Antes de qualquer coleta real, deverão ser observados os procedimentos éticos e institucionais aplicáveis ao estudo.

## 14. Relação com o Projeto Evolução ADS

O Projeto Evolução ADS poderá fornecer:

- arquitetura de telemetria;
- componentes de registro;
- gerenciamento de sessões;
- persistência;
- exportação;
- testes automatizados;
- suporte à modernização;
- ferramentas de análise.

Os estudantes do TCC devem compreender os componentes utilizados e ser capazes de explicar sua integração, mas não precisam ser responsáveis por toda a infraestrutura avançada.

## 15. Resultados esperados

Espera-se obter:

1. versão funcional com suporte aprimorado ao português brasileiro;
2. áudios PT-BR integrados e testados;
3. melhorias de interface em um conjunto delimitado de telas;
4. registro estruturado das interações essenciais;
5. representação de sessões e progressão;
6. documentação técnica das alterações;
7. resultados de testes;
8. análise descritiva dos dados produzidos;
9. TCC discutindo criticamente potencialidades e limitações da ferramenta;
10. base funcional preparada para continuidade.

## 16. Trabalhos futuros

Possibilidades incluem:

- ampliação da modernização visual;
- acessibilidade;
- novos recursos educacionais;
- estudos longitudinais;
- comparação entre grupos;
- dashboards;
- novos minijogos;
- integração com outros sistemas;
- análise estatística;
- análise de padrões;
- ferramentas para profissionais;
- inteligência artificial aplicada a dados adequadamente validados.

A inteligência artificial é considerada uma camada posterior de análise e **não é requisito para conclusão deste TCC**.
