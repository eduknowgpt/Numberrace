# Reestruturação da base do Number Race

## 1. Objetivo deste documento

Este documento registra, de forma resumida, a principal reestruturação realizada na base de código do **Number Race** antes do início dos novos projetos de evolução.

A reestruturação teve como objetivo transformar a árvore histórica do projeto em uma base de desenvolvimento mais simples e compreensível, preservando a versão funcional do jogo e os recursos necessários para sua continuidade.

Este documento não pretende registrar cada alteração de código. Seu propósito é explicar **o que mudou na organização do projeto, por que mudou e quais decisões devem ser preservadas nas próximas evoluções**.



## 2. Situação encontrada

A distribuição original analisada preservava a organização histórica do antigo repositório SVN. Por isso, além do código efetivamente utilizado, continha:

- `trunk`;
- diversos `branches`;
- diversas `tags`;
- versões repetidas dos mesmos módulos;
- módulos antigos de distribuição;
- projetos de idiomas organizados separadamente;
- artefatos gerados por builds anteriores;
- configurações específicas de IDE;
- dependências legadas distribuídas em diferentes partes da árvore.

Essa estrutura era útil como histórico do desenvolvimento original, mas dificultava identificar qual versão deveria ser utilizada como ponto de partida para novas contribuições.

A árvore original continha mais de oito mil entradas no pacote analisado e múltiplas cópias históricas dos módulos Maven.



## 3. Princípio adotado

A reestruturação não foi uma reescrita do Number Race.

A decisão foi partir da **versão funcional atual**, conservar o código e os recursos necessários à execução e retirar da árvore ativa elementos históricos ou de distribuição que não eram necessários ao desenvolvimento pretendido.

Em resumo:

> preservar o software funcional, simplificar sua estrutura e criar uma base única para as próximas evoluções.



## 4. Principais mudanças

### 4.1 Remoção da estrutura histórica do SVN

Os níveis:

- `trunk`;
- `branches`;
- `tags`;

deixaram de fazer parte da estrutura ativa.

O histórico original continua sendo uma referência externa, mas não precisa ser reproduzido dentro da nova árvore de desenvolvimento, pois o Git passa a fornecer o controle de versões do projeto atual.

### 4.2 Definição de uma única base ativa

Os componentes necessários ao desenvolvimento foram reorganizados em uma única raiz.

A estrutura principal passou a ser:

    numberraceIFBA/
    ├── pom.xml
    ├── numberrace-core/
    ├── numberrace-res/
    ├── languages/
    ├── tools/
    │   └── language-editor/
    ├── legacy-maven-repository/
    ├── docs/
    └── README.md

Essa passa a ser a referência para novas alterações.

### 4.3 Preservação do núcleo e dos recursos

Foram mantidos como módulos distintos:

- `numberrace-core` — código principal do jogo;
- `numberrace-res` — recursos comuns utilizados pela aplicação.

Essa separação já fazia sentido na arquitetura existente e foi preservada.

### 4.4 Simplificação dos pacotes de idiomas

Os projetos de idiomas, anteriormente distribuídos em estruturas próprias com níveis como `trunk` e, em alguns casos, `tags`, foram reunidos sob:

    languages/
    ├── de/
    ├── el/
    ├── en/
    ├── es/
    ├── fi/
    ├── fr/
    ├── nl/
    ├── pt/
    └── sv/

Foi criado um `languages/pom.xml` para concentrar a organização Maven desses módulos.

Assim, os idiomas passam a fazer parte de uma estrutura única e previsível, sem eliminar o suporte multilíngue original.

### 4.5 Inclusão do português

O português foi incorporado como um pacote de idioma próprio:

    languages/pt/

O objetivo não é substituir os demais idiomas, mas acrescentar e manter o suporte a português brasileiro dentro da mesma arquitetura multilíngue.

### 4.6 Automatização dos pacotes de idioma

O processo Maven foi ajustado para que, durante o empacotamento, os JARs dos idiomas sejam disponibilizados automaticamente em:

    numberrace-core/target/classes/langs/

Isso permite executar o jogo pelo Eclipse utilizando os pacotes recém-compilados, reduzindo a necessidade de cópias manuais.

Também foi mantida a possibilidade de copiar os pacotes para a instalação local do usuário por meio do profile:

    mvn package -Pdeploy-local

### 4.7 Language Editor como ferramenta opcional

O editor de idiomas foi preservado, mas deixou de fazer parte obrigatória do build principal.

Sua localização passou a ser:

    tools/language-editor/

Ele pode ser incluído quando necessário por meio do profile Maven `tools`.

### 4.8 Retirada de módulos antigos da base ativa

Os antigos módulos relacionados a:

- JNLP;
- Web;
- Installer;

não foram incorporados ao reactor Maven principal da nova base.

Esses componentes pertenciam a formas antigas de distribuição e não eram necessários para a versão funcional escolhida como ponto de partida.

Isso não significa que o código histórico original deixou de existir em sua fonte de referência; significa apenas que esses módulos não fazem parte da base ativa de evolução.

### 4.9 Preservação das dependências legadas necessárias

Algumas dependências antigas do Number Race não estão disponíveis de forma conveniente em repositórios Maven atuais.

Por isso, foi mantido:

    legacy-maven-repository/

Esse diretório existe por necessidade de compatibilidade com a aplicação legada e **não deve ser removido apenas por parecer antigo**.

A substituição dessas dependências deve ocorrer somente como parte de uma modernização planejada e testada.

### 4.10 Limpeza da configuração Maven

A configuração Maven foi reorganizada para permitir um reactor principal mais simples:

    numberrace-res
          ↓
    numberrace-core
          ↓
       languages

Também foram corrigidas referências Maven antigas, como o uso de `${version}`, substituído por `${project.version}` quando aplicável.


## 5. O que foi preservado

A simplificação não teve como objetivo eliminar funcionalidades do jogo.

Foram preservados, entre outros:

- código principal do Number Race;
- recursos comuns;
- arquitetura de pacotes de idiomas;
- idiomas existentes;
- novo pacote em português;
- editor de idiomas;
- dependências legadas necessárias;
- compatibilidade necessária para execução da versão funcional utilizada como baseline.

A regra para futuras refatorações deve continuar sendo:

> remover complexidade acidental sem remover funcionalidades necessárias.



## 6. Situação atual dos recursos dos idiomas

A reorganização estrutural dos idiomas já foi realizada, mas a migração de todos os recursos ainda não está concluída.

No estado atual analisado, o pacote `pt` já contém os recursos de áudio em português que vêm sendo integrados e testados.

Os demais diretórios de idioma já existem na nova estrutura, porém **os arquivos de imagem e áudio da distribuição original ainda não foram integralmente copiados para todos eles**.

Portanto, neste momento:

- a estrutura multilíngue está preservada;
- os módulos dos idiomas estão organizados;
- o português é o idioma em que a integração de áudio está mais avançada;
- a migração dos assets dos demais idiomas permanece como atividade pendente.

Essa pendência deve ser resolvida antes de considerar concluída a migração integral dos recursos multilíngues.



## 7. Estrutura lógica resultante

A organização pretendida pode ser entendida da seguinte forma:

    Number Race
    │
    ├── numberrace-core
    │   └── lógica e execução do jogo
    │
    ├── numberrace-res
    │   └── recursos comuns
    │
    ├── languages
    │   ├── de
    │   ├── el
    │   ├── en
    │   ├── es
    │   ├── fi
    │   ├── fr
    │   ├── nl
    │   ├── pt
    │   └── sv
    │
    ├── tools
    │   └── language-editor
    │
    ├── legacy-maven-repository
    │   └── dependências antigas ainda necessárias
    │
    └── docs
        └── documentação técnica e de evolução

Essa estrutura deve ser preferida nas próximas contribuições.



## 8. Artefatos que não pertencem ao código-fonte

Diretórios e arquivos gerados automaticamente não devem ser considerados parte da arquitetura do projeto.

Exemplos:

- `target/`;
- `.metadata/`;
- históricos internos do workspace Eclipse;
- arquivos temporários;
- logs;
- caches;
- artefatos locais de build.

Eles podem existir em uma cópia de trabalho local, mas não devem orientar a organização do código nem, em regra, ser versionados no Git.

Da mesma forma, configurações locais do Eclipse devem ser mantidas apenas quando forem deliberadamente necessárias ao projeto.


## 9. Resultado da reestruturação

A mudança reduziu uma árvore histórica complexa para uma organização centrada nos componentes efetivamente utilizados.

Antes, para compreender o projeto, era necessário distinguir entre várias versões, branches, tags, trunks, módulos de distribuição e projetos de idioma separados.

Depois da reestruturação, existe uma única base ativa, na qual:

- o núcleo está claramente identificado;
- os recursos comuns estão separados;
- os idiomas ficam reunidos;
- o português é tratado como um idioma da arquitetura existente;
- ferramentas auxiliares ficam separadas;
- dependências legadas necessárias ficam explicitamente identificadas;
- o Maven coordena o build a partir da raiz;
- novas evoluções podem ser realizadas sobre uma baseline única.



## 10. Diretriz para as próximas evoluções

Esta reestruturação deve ser considerada a **baseline arquitetural inicial** dos novos projetos de evolução do Number Race.

Mudanças futuras devem evitar reintroduzir a complexidade eliminada.

Em particular:

1. não recriar estruturas `trunk`, `branches` ou `tags` dentro do projeto;
2. utilizar Git para versionamento e histórico;
3. manter os idiomas sob `languages/`;
4. manter recursos comuns separados dos recursos específicos de idioma;
5. evitar cópias manuais como requisito permanente do processo de build;
6. não versionar artefatos gerados;
7. não remover dependências legadas sem verificar seu uso;
8. realizar modernizações de forma incremental;
9. preservar uma versão executável durante a evolução;
10. documentar mudanças estruturais relevantes.



## 11. Próximos passos imediatos

A partir da estrutura atual, as atividades mais imediatas são:

1. concluir a migração dos arquivos de imagem e áudio dos idiomas ainda incompletos;
2. verificar se cada pacote de idioma produz um JAR autocontido com os recursos esperados;
3. validar a execução do jogo em cada idioma;
4. eliminar do repositório artefatos locais eventualmente gerados por Eclipse ou Maven;
5. continuar as modernizações sobre essa estrutura, sem alterar desnecessariamente a baseline funcional.

Após essa consolidação, a evolução pode avançar para as frentes descritas nos documentos de TCC e ADS, incluindo modernização da interface e registro estruturado dos dados de interação.
