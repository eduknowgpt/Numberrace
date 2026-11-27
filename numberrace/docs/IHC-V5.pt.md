# NumberRace — IHC V5

Aplicar sobre V4 (ou V3 com mensagens da primeira refatoração). Extrair na raiz do repositório, preservar caminhos, atualizar Eclipse (F5), Maven Update Project, clean install. Nenhuma dependência nova. Fazer commit antes da substituição.

## Decisões
- Cenário ilustrado original gerado por IA, sem controles estáticos; carregamento por classpath e escala proporcional com recorte central. Carrinho e bandeira integram o cenário.
- Painéis, cabeçalhos, botões e cartões desenhados por Graphics2D; controles Swing reais e textos existentes via Messages. Ícones vetoriais. Quatro retratos recortados circularmente do conceito anterior; escolha explícita no cadastro/edição, sem inferir sexo. ABC usa inicial do nome.
- Preferências de avatar locais por usuário do sistema operacional, chave derivada do fileHeader existente; não modifica formato de dados Student. Não sincroniza entre máquinas; alteração de identificador pode perder associação. A escolha não é exclusiva. Não é autenticação.
- Rótulo Avatar e ABC comuns aos idiomas; demais textos mantidos. Não há migração automática de nomes cadastrados incorretamente.
- Campos e seletores preservam Swing nativo; não foi criada uma biblioteca de campos customizados nesta versão.

## Verificação
Compilação javac source/target 8 contra classes originais e jars locais concluída com avisos. V5Smoke.java verifica carregamento dos recursos e pintura offscreen de componentes. Não substitui teste da aplicação completa. Maven e WindowBuilder indisponíveis neste ambiente.

## Testes no Eclipse
1. Abrir seleção em 800x600, 1024x768, 1440x1080 e 1920x1080; verificar recortes, fontes e navegação.
2. Selecionar jogador, jogar, voltar, alterar e cancelar. Criar jogador com avatar; reiniciar e conferir persistência. Alterar avatar, cancelar, excluir jogador.
3. Verificar foco por Tab, seleção por setas, estados de botões e idiomas.
4. Verificar aba Design do WindowBuilder e execução real. Se divergirem, registrar captura e stack trace.
5. Avaliar com crianças após aprovação ética aplicável; medir erros, autonomia e tempo de seleção.
