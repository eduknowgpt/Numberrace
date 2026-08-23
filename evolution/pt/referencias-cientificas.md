# Fundamentação científica e referências do projeto Number Race

## 1. Finalidade deste documento

Este documento registra a literatura científica utilizada como referência para o projeto de evolução do **Number Race**.

Seu objetivo é:

- explicitar a fundamentação científica que antecede e sustenta o projeto;
- distinguir a evolução tecnológica proposta de uma criação inteiramente nova;
- apoiar a elaboração de projetos de TCC, projetos de pesquisa, planos de trabalho e documentos institucionais no IFBA;
- fornecer aos estudantes e colaboradores um conjunto inicial de referências para compreender os fundamentos educacionais e cognitivos do Number Race;
- preservar a rastreabilidade entre o software que está sendo evoluído e os estudos científicos que investigaram seu projeto e sua utilização.

A presença dessas referências é especialmente importante porque o Number Race não deve ser tratado apenas como um jogo educacional. O software foi concebido a partir de princípios da cognição numérica e da aprendizagem adaptativa e foi posteriormente investigado em diferentes populações e contextos educacionais.

 

## 2. Fundamentação original do Number Race

Os dois trabalhos de Wilson et al. (2006) constituem as referências centrais para compreender a origem científica do Number Race.

### 2.1 Princípios de projeto

**Wilson et al. (2006a)** descrevem os princípios cognitivos, educacionais e algorítmicos utilizados no desenvolvimento do Number Race. O jogo foi concebido para trabalhar comparação numérica por meio de atividades adaptadas ao desempenho individual da criança.

O algoritmo considera um espaço de aprendizagem multidimensional e adapta a dificuldade em função de três dimensões principais:

1. distância numérica;
2. tempo disponível para resposta;
3. complexidade conceitual, progredindo de representações não simbólicas para operações simbólicas.

O trabalho apresenta, portanto, uma referência particularmente importante para qualquer alteração futura no mecanismo adaptativo do jogo. Mudanças nesse mecanismo devem considerar que sua implementação original está associada a hipóteses e princípios explícitos de cognição numérica, e não apenas a decisões de programação.

**Uso no projeto:** fundamentação do funcionamento do jogo, do mecanismo adaptativo, das tarefas de comparação numérica e das decisões relacionadas à modernização do software.

### 2.2 Avaliação inicial

**Wilson et al. (2006b)** apresentam uma avaliação inicial do Number Race com nove crianças de 7 a 9 anos com dificuldades em matemática.

Após cinco semanas de treinamento adaptativo, foram observadas melhorias específicas em tarefas relacionadas ao senso numérico, incluindo velocidade de subitização e comparação numérica, além de aumento na acurácia em subtração. Os próprios autores, entretanto, destacam que os resultados eram preliminares e necessitavam de estudos posteriores, maiores e controlados.

**Uso no projeto:** evidência histórica da aplicação do Number Race e referência para a definição dos tipos de interação e desempenho que podem ser registrados nas futuras versões do sistema.

 

## 3. Estudos posteriores com o Number Race

### 3.1 Crianças em idade pré-escolar

**Wilson et al. (2009)** avaliaram o Number Race com 53 crianças de educação infantil de baixo nível socioeconômico na França. O estudo encontrou melhorias em tarefas simbólicas tradicionalmente utilizadas para avaliar senso numérico, mas não em medidas não simbólicas. Os autores interpretaram os resultados como possível melhoria no **acesso ao senso numérico**, particularmente nas ligações entre representações simbólicas e não simbólicas.

**Uso no projeto:** fundamentação para considerar separadamente diferentes formas de representação numérica e para evitar tratar o desempenho no jogo como uma medida única e indiferenciada.

### 3.2 Ensaio controlado randomizado

**Sella et al. (2016)** conduziram um ensaio controlado randomizado com crianças em idade pré-escolar. O treinamento com o Number Race produziu melhorias importantes em cálculo mental e mapeamento espacial de números e melhorias menores na representação semântica de números.

O artigo também descreve quatro princípios associados ao jogo:

1. aprimorar o senso numérico;
2. fortalecer as ligações entre diferentes representações do número;
3. desenvolver e automatizar a aritmética;
4. maximizar a motivação.

O estudo descreve ainda a adaptação da dificuldade a partir da distância numérica, do limite de tempo e do formato/complexidade das quantidades apresentadas.

**Uso no projeto:** referência para a compreensão das variáveis de dificuldade, progressão, desempenho e adaptação que podem orientar o futuro modelo de registro de dados de interação.

### 3.3 Crianças com baixo desempenho em matemática

**Hellstrand et al. (2019)** investigaram o Number Race como intervenção para alunos do primeiro ano com baixo desempenho matemático. O grupo de intervenção utilizou o jogo em sessões de aproximadamente 15 minutos, três a quatro dias por semana, durante quatro semanas.

Nesse estudo, **não foi encontrado efeito estatisticamente significativo da intervenção com o Number Race**.

Esse resultado é relevante para o projeto e deve ser preservado na fundamentação. A literatura disponível não autoriza apresentar o Number Race como uma intervenção de eficácia universal ou garantida. Os resultados variam de acordo com população, desenho do estudo, duração, medidas utilizadas e contexto de aplicação.

**Uso no projeto:** fundamentação para uma abordagem cientificamente cautelosa e para a necessidade de registrar dados suficientemente detalhados para permitir análises posteriores, em vez de assumir antecipadamente que a utilização do jogo necessariamente produz melhoria.

### 3.4 Crianças com síndrome de Down

**Sella et al. (2021)** avaliaram o Number Race com crianças com síndrome de Down. O treinamento ocorreu durante dez semanas, com duas sessões semanais de 20 a 30 minutos.

Os autores encontraram evidência fraca de diferenças entre grupos no escore global de numeracia após o treinamento, mas observaram melhorias substanciais em habilidades numéricas específicas e em cálculo mental no grupo que utilizou o Number Race, mantidas no acompanhamento posterior. O estudo conclui que o jogo parece adequado para melhorar alguns aspectos da numeracia nessa população.

**Uso no projeto:** demonstra que o Number Race já foi investigado em diferentes populações e reforça a importância de registrar resultados por habilidade, tarefa, sessão e participante, evitando reduzir a evolução da criança a um único indicador global.



## 4. Referência complementar sobre numeracia inicial

### 4.1 The Great Race

**McKevett, Codding e Running (2024)** investigaram um pacote instrucional baseado na sequência concreto–representacional–abstrato (CRA) associado ao **The Great Race**, com três crianças em idade pré-escolar. O estudo encontrou aumento da fluência de correspondência de quantidades em dois dos três participantes, sem generalização para sequenciamento numérico.

Este artigo deve ser classificado como **referência complementar**, e não como uma avaliação do software Number Race. O *The Great Race* estudado nesse artigo não deve ser confundido, apenas pela semelhança do nome, com o *The Number Race* que constitui o objeto deste projeto.

Sua relevância está na discussão sobre:

- numeracia inicial;
- diferentes representações de quantidade;
- correspondência entre quantidade e número;
- intervenção em crianças pré-escolares;
- avaliação e acompanhamento de habilidades numéricas específicas.

 

## 5. Relação da literatura com o projeto de evolução

A literatura analisada fornece uma base para três dimensões complementares do projeto.

### Dimensão educacional e cognitiva

Os trabalhos fundamentam conceitos como senso numérico, comparação de magnitude, representações simbólicas e não simbólicas, cálculo, linha numérica e adaptação da dificuldade.

### Dimensão tecnológica

O artigo original de Wilson et al. (2006a) mostra que determinadas características do software possuem uma justificativa científica. Por isso, a modernização da implementação deve procurar separar:

- elementos tecnológicos que podem ser substituídos ou modernizados;
- elementos funcionais associados ao modelo educacional/cognitivo que precisam ser compreendidos antes de serem alterados.

### Dimensão de dados e análise

Os estudos utilizam diferentes medidas, populações, períodos de intervenção e resultados. Isso reforça a proposta de evoluir o Number Race para produzir **registros estruturados das interações**, permitindo posteriormente analisar aspectos como:

- acertos e erros;
- tempo de resposta;
- tipo de representação numérica;
- nível de dificuldade;
- progressão entre atividades;
- adaptações realizadas pelo jogo;
- duração e frequência das sessões;
- evolução ao longo do tempo.

O registro desses dados não deve, por si só, produzir diagnóstico ou interpretação clínica. A evolução tecnológica deve priorizar a produção de dados rastreáveis e adequadamente contextualizados para que análises posteriores possam ser realizadas por pesquisadores e, quando pertinente, por profissionais qualificados ou por métodos computacionais de apoio à análise.

 

## 6. Posicionamento científico do projeto

Para documentos institucionais, recomenda-se caracterizar a iniciativa da seguinte forma:

> O projeto parte do software educacional aberto *The Number Race*, originalmente desenvolvido a partir de princípios da cognição numérica e de aprendizagem adaptativa e investigado em estudos científicos com diferentes populações. A proposta não consiste em desenvolver um jogo inteiramente novo, mas em evoluir sua base tecnológica, ampliar sua localização para o português brasileiro e acrescentar mecanismos estruturados de registro das interações realizadas durante as sessões. Esses registros poderão subsidiar estudos posteriores sobre padrões de interação, desempenho e progressão dos participantes. A evolução será conduzida preservando a rastreabilidade com os princípios do software original e considerando criticamente as evidências disponíveis na literatura, que apresentam resultados distintos conforme o contexto e a população investigada.

Esse posicionamento evita duas afirmações que a literatura disponível não sustenta:

- que o Number Race seja uma ferramenta diagnóstica para discalculia;
- que sua utilização garanta melhoria das habilidades matemáticas.

O projeto deve ser apresentado como **evolução tecnológica e instrumento de apoio a atividades educacionais e de pesquisa**, com qualquer estudo envolvendo participantes humanos submetido aos procedimentos éticos e institucionais aplicáveis.

 

## 7. Referências

As referências abaixo correspondem aos trabalhos utilizados como base inicial deste projeto.

WILSON, Anna J.; DEHAENE, Stanislas; PINEL, Philippe; REVKIN, Susannah K.; COHEN, Laurent; COHEN, David. Principles underlying the design of “The Number Race”, an adaptive computer game for remediation of dyscalculia. **Behavioral and Brain Functions**, v. 2, art. 19, 2006. DOI: 10.1186/1744-9081-2-19.

WILSON, Anna J.; REVKIN, Susannah K.; COHEN, David; COHEN, Laurent; DEHAENE, Stanislas. An open trial assessment of “The Number Race”, an adaptive computer game for remediation of dyscalculia. **Behavioral and Brain Functions**, v. 2, art. 20, 2006. DOI: 10.1186/1744-9081-2-20.

WILSON, Anna J.; DEHAENE, Stanislas; DUBOIS, Ophélie; FAYOL, Michel. Effects of an adaptive game intervention on accessing number sense in low-socioeconomic-status kindergarten children. **Mind, Brain, and Education**, v. 3, n. 4, p. 224–234, 2009. DOI: 10.1111/j.1751-228X.2009.01075.x.

SELLA, Francesco; TRESSOLDI, Patrizio; LUCANGELI, Daniela; ZORZI, Marco. Training numerical skills with the adaptive videogame “The Number Race”: a randomized controlled trial on preschoolers. **Trends in Neuroscience and Education**, v. 5, p. 20–29, 2016. DOI: 10.1016/j.tine.2016.02.002.

HELLSTRAND, Heidi; KORHONEN, Johan; LINNANMÄKI, Karin; AUNIO, Pirjo. The Number Race – computer-assisted intervention for mathematically low-performing first graders. **European Journal of Special Needs Education**, 2019. DOI: 10.1080/13488678.2019.1615792.

SELLA, Francesco; ONNIVELLO, Sara; LUNARDON, Maristella; LANFRANCHI, Silvia; ZORZI, Marco. Training basic numerical skills in children with Down syndrome using the computerized game “The Number Race”. **Scientific Reports**, v. 11, art. 2087, 2021. DOI: 10.1038/s41598-020-78801-5.

MCKEVETT, Nicole M.; CODDING, Robin S.; RUNNING, Kristin R. The Effects of Representational Instruction and The Great Race on Preschoolers’ Early Numeracy Skills. **Remedial and Special Education**, v. 45, n. 1, p. 18–29, 2024. DOI: 10.1177/07419325231160292.

 

## 8. Manutenção deste documento

Este arquivo deve ser atualizado quando novos estudos forem incorporados à fundamentação do projeto.

Recomenda-se que novas referências sejam classificadas conforme sua relação com o projeto:

- **fundamentação do Number Race** — trabalhos sobre concepção, algoritmo e princípios do software;
- **avaliação do Number Race** — estudos empíricos que utilizam diretamente o jogo;
- **fundamentação complementar** — trabalhos sobre numeracia, cognição numérica, discalculia, jogos educacionais, avaliação ou tecnologias relacionadas;
- **evolução tecnológica** — trabalhos sobre interfaces, acessibilidade, engenharia de software, registro de interação, análise de dados e outros aspectos introduzidos pela nova versão.

Essa classificação ajuda a manter clara a diferença entre evidências diretamente produzidas com o Number Race e literatura utilizada para fundamentar novas extensões do projeto.
