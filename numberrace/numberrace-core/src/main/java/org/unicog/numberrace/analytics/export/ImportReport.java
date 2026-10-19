package org.unicog.numberrace.analytics.export;

import java.util.ArrayList;
import java.util.List;

/** O que a importação encontrou: quantidades e a lista de problemas, em português. */
final class ImportReport {

    int filesRead;
    int linesRead;
    int eventsAccepted;
    int duplicates;
    final List<String> invalidEvents = new ArrayList<String>();
    final List<String> incompleteLines = new ArrayList<String>();
    final List<String> conflicts = new ArrayList<String>();
    final List<String> sessionWarnings = new ArrayList<String>();
    final List<String> fileErrors = new ArrayList<String>();

    /** Quantidade de problemas que merecem atenção (repetições idênticas não contam). */
    int problemCount() {
        return invalidEvents.size() + incompleteLines.size() + conflicts.size() + sessionWarnings.size()
                + fileErrors.size();
    }

    /** Relatório em texto, para o console e para o arquivo {@code relatorio-importacao.txt}. */
    String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Arquivos lidos: ").append(filesRead).append('\n');
        sb.append("Linhas lidas: ").append(linesRead).append('\n');
        sb.append("Eventos aceitos: ").append(eventsAccepted).append('\n');
        sb.append("Eventos repetidos ignorados (mesmo event_id e mesmo conteúdo): ").append(duplicates).append('\n');
        section(sb, "Arquivos que não puderam ser lidos", fileErrors);
        section(sb, "Linhas incompletas ou que não são JSON (descartadas)", incompleteLines);
        section(sb, "Eventos inválidos (descartados)", invalidEvents);
        section(sb, "Conflitos (mantido o primeiro evento)", conflicts);
        section(sb, "Avisos sobre sessões", sessionWarnings);
        if (problemCount() == 0) {
            sb.append("\nNenhum problema encontrado.\n");
        }
        return sb.toString();
    }

    private static void section(StringBuilder sb, String title, List<String> items) {
        if (items.isEmpty()) {
            return;
        }
        sb.append('\n').append(title).append(" (").append(items.size()).append("):\n");
        for (String item : items) {
            sb.append("- ").append(item).append('\n');
        }
    }
}
