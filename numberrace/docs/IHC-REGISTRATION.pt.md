# Reformulação da seleção de jogadores

A classe RegistrationScreen utiliza CardLayout para separar seleção e edição. A seleção apresenta nomes, linhas maiores, indicação textual de seleção e botão principal Jogar. O formulário preserva os campos originais e o nível inicial permanece bloqueado na edição. A exclusão continua exigindo confirmação. Não há autenticação administrativa: a separação é visual, não uma barreira de segurança. Não foram adicionados avatares nem modificada a persistência.

Corrigidos: idade inválida, comparação de nomes por conteúdo (ignorando maiúsculas), exclusão do próprio registro na verificação de duplicidade, atualização da lista após exclusão e seleção inicial explícita. A regra de duplicidade continua baseada em nome e sobrenome; homônimos ainda precisam de política própria.

## Eclipse
Importar o pom.xml raiz via Existing Maven Projects, Maven > Update Project, executar clean install no projeto raiz e usar a configuração de execução habitual. Não copiar target. Fazer backup dos dados dos jogadores antes de testar.

## Testes manuais pendentes
- Lista vazia: Jogar e Alterar desabilitados; cadastro disponível.
- Selecionar jogador com mouse/teclado; Jogar abre seleção de tema.
- Cadastro válido, idade não numérica, zero, duplicidade, cancelamento.
- Alterar sem mudar nome; nível inicial preservado; cancelar edição.
- Excluir: cancelar e confirmar; lista atualizada; reiniciar e verificar persistência.
- Conferir português e outros idiomas, resoluções e foco do teclado.

Validação realizada: compilação isolada da classe Java com source/target 8 contra classes e dependências do ZIP original. Maven e teste gráfico não executados neste ambiente. A mensagem de idade em grego usa inglês provisoriamente. A interface não foi validada com crianças.
