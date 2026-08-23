# Refatoração aplicada

- preservados `numberrace-core` e `numberrace-res` como módulos separados;
- preservados todos os idiomas existentes, incluindo português e sueco;
- removidos níveis SVN `trunk`, `branches` e `tags`;
- removidos `target`, `.settings`, `.classpath` e `.project` da distribuição;
- consolidada a configuração Maven dos idiomas em `languages/pom.xml`;
- corrigido `${version}` para `${project.version}`;
- incluído staging automático dos JARs de idioma para `numberrace-core/target/classes/langs`;
- mantido `deploy-local` para copiar language packs para `~/NumberRace/v3/langs`;
- mantido o repositório Maven legado necessário ao núcleo;
- `language-editor` preservado como ferramenta opcional;
- retirados do projeto ativo JNLP, Web e Installer, que não participavam do reactor Maven funcional atual;
- removidas mensagens temporárias de diagnóstico adicionadas durante a investigação do carregamento de idiomas.
