package org.unicog.numberrace.analytics.export;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Escolha do que exportar: sessões inteiras, por participante, por sessão ({@code session_id}
 * completo ou o seu começo) e por período (data de início da sessão). Sem filtros, exporta
 * tudo. As sessões nunca são cortadas ao meio, para preservar a relação entre sessão e eventos.
 */
final class ExportFilter {

    private final List<String> participants = new ArrayList<String>();
    private final List<String> sessionPrefixes = new ArrayList<String>();
    private LocalDate from;
    private LocalDate to;

    void addParticipant(String code) {
        participants.add(code.trim());
    }

    void addSession(String idOrPrefix) {
        sessionPrefixes.add(idOrPrefix.trim().toLowerCase());
    }

    void setFrom(LocalDate date) {
        from = date;
    }

    void setTo(LocalDate date) {
        to = date;
    }

    boolean isEmpty() {
        return participants.isEmpty() && sessionPrefixes.isEmpty() && from == null && to == null;
    }

    boolean accepts(SessionData s) {
        if (!participants.isEmpty() && !participants.contains(s.getParticipantId())) {
            return false;
        }
        if (!sessionPrefixes.isEmpty()) {
            boolean match = false;
            for (String p : sessionPrefixes) {
                match |= s.getSessionId().toLowerCase().startsWith(p);
            }
            if (!match) {
                return false;
            }
        }
        LocalDate day = s.getStartTime().toLocalDate();
        return (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to));
    }

    List<SessionData> apply(List<SessionData> sessions) {
        List<SessionData> out = new ArrayList<SessionData>();
        for (SessionData s : sessions) {
            if (accepts(s)) {
                out.add(s);
            }
        }
        return out;
    }

    /** Descrição para o manifesto ({@code null} nos filtros não usados). */
    Map<String, Object> describe() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("participants", participants.isEmpty() ? null : new ArrayList<Object>(participants));
        m.put("sessions", sessionPrefixes.isEmpty() ? null : new ArrayList<Object>(sessionPrefixes));
        m.put("from", from == null ? null : from.toString());
        m.put("to", to == null ? null : to.toString());
        return m;
    }

    /** Descrição em português, para o LEIA-ME e o console. */
    String describeText() {
        if (isEmpty()) {
            return "nenhum (todas as sessões encontradas)";
        }
        StringBuilder sb = new StringBuilder();
        if (!participants.isEmpty()) {
            sb.append("participantes ").append(participants).append("; ");
        }
        if (!sessionPrefixes.isEmpty()) {
            sb.append("sessões ").append(sessionPrefixes).append("; ");
        }
        if (from != null) {
            sb.append("a partir de ").append(from).append("; ");
        }
        if (to != null) {
            sb.append("até ").append(to).append("; ");
        }
        return sb.substring(0, sb.length() - 2);
    }
}
