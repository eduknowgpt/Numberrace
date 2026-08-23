# Evolução do The Number Race

Esta pasta reúne as especificações dos projetos de evolução do **The Number Race** desenvolvidos em contexto educacional e de pesquisa.

A evolução foi organizada em duas frentes complementares:

- **Projeto Evolução TCC** — destinado inicialmente a estudantes do Ensino Médio Integrado, com foco na adaptação do Number Race ao português brasileiro, modernização da experiência de uso e instrumentação do jogo para registro das interações.
- **Projeto Evolução ADS** — destinado a estudantes do curso superior de Análise e Desenvolvimento de Sistemas (ADS), com foco na sustentação técnica, modernização arquitetural, telemetria, persistência, qualidade de software e preparação da plataforma para análise de dados.

Os dois projetos utilizam a mesma base de software, mas possuem escopos e responsabilidades diferentes.

## Visão

A evolução busca transformar gradualmente o Number Race em uma plataforma aberta e sustentável capaz de apoiar:

1. aprendizagem matemática;
2. estudos sobre dificuldades de aprendizagem matemática;
3. projetos de desenvolvimento de software;
4. formação de estudantes do Ensino Médio e do Ensino Superior;
5. coleta estruturada de dados de interação;
6. acompanhamento da evolução do desempenho ao longo de sessões;
7. estudos com indivíduos e grupos;
8. pesquisas futuras envolvendo visualização, análise de dados e inteligência artificial.

O Number Race **não deve ser apresentado como instrumento de diagnóstico de discalculia**. Os dados registrados representam evidências de interação e desempenho no jogo. Sua interpretação clínica, quando pertinente, deve ser realizada por profissionais habilitados e dentro de um protocolo de pesquisa apropriado.

## Relação entre os projetos

    Projeto Evolução ADS
            |
            | infraestrutura, arquitetura,
            | qualidade e telemetria
            v
    +-------------------------+
    |       Number Race       |
    | modernizado e           |
    | instrumentado           |
    +-------------------------+
            ^
            | localização, interface,
            | uso e avaliação educacional
            |
    Projeto Evolução TCC

O Projeto Evolução ADS fornece infraestrutura que pode ser utilizada pelo TCC e por projetos posteriores. O TCC, por sua vez, funciona como um caso de uso concreto para orientar e validar parte dessa evolução.

## Documentos

- [Projeto Evolução TCC](evolucao-tcc.md)
- [Projeto Evolução ADS](evolucao-ads.md)

## Princípios de evolução

Toda contribuição deve procurar:

- preservar uma versão funcional do jogo;
- realizar mudanças incrementais e testáveis;
- preservar os idiomas existentes;
- manter os créditos, copyrights e termos de licença aplicáveis ao projeto original;
- documentar alterações relevantes;
- separar recursos comuns de recursos específicos de idioma;
- evitar acoplamento desnecessário entre interface, jogo, telemetria e análise;
- registrar somente dados necessários aos objetivos definidos;
- proteger identidade e privacidade dos participantes;
- não transformar resultados computacionais em diagnósticos clínicos;
- priorizar formatos abertos e estruturas documentadas;
- manter o projeto compreensível para futuros estudantes e colaboradores.

## Contribuições no GitHub

Sempre que possível, uma contribuição deve estar associada a uma issue ou atividade claramente definida.

Pull requests devem informar:

- problema ou necessidade;
- solução implementada;
- componentes modificados;
- forma de testar;
- impactos conhecidos;
- limitações;
- possíveis trabalhos futuros.

Estes documentos são evolutivos e podem ser refinados conforme o projeto avance. Alterações significativas de objetivos, escopo, requisitos ou responsabilidades devem ser documentadas.
