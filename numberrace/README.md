# NumberRace — base refatorada

Esta árvore preserva a versão funcional atual do NumberRace e seus pacotes de idioma, removendo a organização histórica do SVN (`trunk`, `branches`, `tags`) e artefatos Maven gerados (`target`).

## Estrutura
- `numberrace-core`: código do jogo.
- `numberrace-res`: recursos comuns.
- `languages`: pacotes `de`, `el`, `en`, `es`, `fi`, `fr`, `nl`, `pt` e `sv`.
- `tools/language-editor`: editor de pacotes de idioma (opcional; profile Maven `tools`).
- `legacy-maven-repository`: dependências antigas necessárias ao `numberrace-core`.

## Importar no Eclipse
1. `File > Import > Maven > Existing Maven Projects`.
2. Selecione a pasta raiz deste projeto.
3. Importe o reactor principal (`pom.xml`) e seus módulos.
4. Execute `Maven > Update Project` se necessário.
5. Faça `Run As > Maven build...` na raiz com goal `package`.

Ao empacotar, cada language pack é copiado automaticamente para `numberrace-core/target/classes/langs`. Assim, ao executar `org.unicog.numberrace.Game` pelo Eclipse, o jogo encontra os idiomas sem cópia manual para a instalação do usuário.

Para também copiar os pacotes para `${user.home}/NumberRace/v3/langs`, use:

    mvn package -Pdeploy-local

O editor de idiomas é opcional e não participa do build padrão. Para incluí-lo:

    mvn package -Ptools

## Observação
Os módulos JNLP, Web e Installer antigos não participavam do build Maven funcional atual e foram retirados desta base ativa. O histórico SVN (`branches`, `tags`) também não foi incluído.
