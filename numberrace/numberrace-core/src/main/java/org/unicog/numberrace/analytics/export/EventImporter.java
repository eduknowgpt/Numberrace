package org.unicog.numberrace.analytics.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.unicog.numberrace.analytics.EventValidator;
import org.unicog.numberrace.analytics.InteractionLogger;
import org.unicog.numberrace.analytics.Json;

/**
 * Importação (persistencia-eventos.md, seção 7.6): lê os arquivos JSON Lines de uma ou mais
 * pastas (por exemplo, copiadas de vários computadores), valida cada evento, junta tudo sem
 * repetir ({@code event_id}) e relata linhas incompletas, eventos inválidos, conflitos, lacunas
 * na sequência e sessões sem início ou fim. A pasta de identificação nunca é lida.
 */
final class EventImporter {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final ImportReport report = new ImportReport();
    private final Map<String, SourceEvent> byEventId = new HashMap<String, SourceEvent>();
    private final Map<String, TreeMap<Integer, SourceEvent>> bySession = new LinkedHashMap<String, TreeMap<Integer, SourceEvent>>();
    private final Map<String, Set<String>> sessionsByFile = new HashMap<String, Set<String>>();

    ImportReport getReport() {
        return report;
    }

    /** Para cada arquivo lido, quantas sessões diferentes ele contém (o esperado é uma). */
    Map<String, Integer> getSessionsPerFile() {
        Map<String, Integer> m = new HashMap<String, Integer>();
        for (Map.Entry<String, Set<String>> entry : sessionsByFile.entrySet()) {
            m.put(entry.getKey(), Integer.valueOf(entry.getValue().size()));
        }
        return m;
    }

    /** Importa os arquivos {@code .jsonl} das entradas (pastas ou arquivos) e devolve as sessões. */
    List<SessionData> importFrom(List<File> inputs) {
        for (int i = 0; i < inputs.size(); i++) {
            File input = inputs.get(i);
            String prefix = inputs.size() > 1 ? "entrada " + (i + 1) + "/" : "";
            if (!input.exists()) {
                report.fileErrors.add(prefix + input.getPath() + ": pasta ou arquivo não encontrado");
                continue;
            }
            List<File> files = new ArrayList<File>();
            collect(input, files);
            Collections.sort(files);
            for (File f : files) {
                readFile(f, prefix + relativeName(input, f));
            }
        }
        return buildSessions();
    }

    private static void collect(File f, List<File> out) {
        if (f.isDirectory()) {
            if (f.getName().equalsIgnoreCase(InteractionLogger.IDENTIFICATION_DIR)) {
                return; // nunca ler a correspondência entre código e participante
            }
            File[] children = f.listFiles();
            for (int i = 0; children != null && i < children.length; i++) {
                collect(children[i], out);
            }
        } else if (f.getName().toLowerCase().endsWith(".jsonl")) {
            out.add(f);
        }
    }

    private static String relativeName(File root, File f) {
        if (root.isFile()) {
            return f.getName();
        }
        String r = root.getAbsoluteFile().toURI().relativize(f.getAbsoluteFile().toURI()).getPath();
        return r.length() > 0 ? r : f.getName();
    }

    private void readFile(File f, String name) {
        Reader in;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(f), UTF8), 65536);
        } catch (IOException e) {
            report.fileErrors.add(name + ": " + e.getMessage());
            return;
        }
        try {
            StringBuilder line = new StringBuilder();
            int lineNumber = 1;
            int c;
            while ((c = in.read()) >= 0) {
                if (c == '\n') {
                    handleLine(line, name, lineNumber++, false);
                    line.setLength(0);
                } else if (!(c == '\uFEFF' && lineNumber == 1 && line.length() == 0)) {
                    line.append((char) c);
                }
            }
            handleLine(line, name, lineNumber, true); // última linha sem fim de linha
            report.filesRead++;
        } catch (IOException e) {
            report.fileErrors.add(name + ": " + e.getMessage());
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // nada a fazer
            }
        }
    }

    private void handleLine(StringBuilder sb, String name, int lineNumber, boolean lastWithoutNewline) {
        int end = sb.length();
        if (end > 0 && sb.charAt(end - 1) == '\r') {
            end--;
        }
        String line = sb.substring(0, end);
        if (line.trim().length() == 0) {
            return;
        }
        report.linesRead++;
        readLine(line, name, lineNumber, lastWithoutNewline);
    }

    private void readLine(String line, String file, int lineNumber, boolean lastWithoutNewline) {
        String where = file + ", linha " + lineNumber;
        Object parsed;
        try {
            parsed = Json.parse(line);
        } catch (IllegalArgumentException e) {
            parsed = null;
        }
        if (!(parsed instanceof Map)) {
            report.incompleteLines.add(where + (lastWithoutNewline
                    ? ": linha incompleta no fim do arquivo (gravação interrompida)"
                    : ": não é um JSON válido"));
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> json = (Map<String, Object>) parsed;
        List<String> problems = EventValidator.validateParsed(json);
        if (!problems.isEmpty()) {
            report.invalidEvents.add(where + " (" + json.get("event_type") + "): " + join(problems));
            return;
        }
        SourceEvent e = new SourceEvent(line, json, file, lineNumber);
        Set<String> fileSessions = sessionsByFile.get(file);
        if (fileSessions == null) {
            fileSessions = new HashSet<String>();
            sessionsByFile.put(file, fileSessions);
        }
        fileSessions.add(e.getSessionId());
        SourceEvent same = byEventId.get(e.getEventId());
        if (same != null) {
            if (same.sameLine(e)) {
                report.duplicates++;
            } else {
                report.conflicts.add("event_id " + e.getEventId() + " com conteúdos diferentes em "
                        + location(same) + " e " + location(e));
            }
            return;
        }
        TreeMap<Integer, SourceEvent> session = bySession.get(e.getSessionId());
        if (session == null) {
            session = new TreeMap<Integer, SourceEvent>();
            bySession.put(e.getSessionId(), session);
        } else {
            SourceEvent first = session.firstEntry().getValue();
            if (!first.getParticipantId().equals(e.getParticipantId())) {
                report.conflicts.add("sessão " + e.getSessionId() + " com mais de um participant_id ("
                        + first.getParticipantId() + " e " + e.getParticipantId() + " em " + location(e) + ")");
                return;
            }
        }
        SourceEvent other = session.get(Integer.valueOf(e.getSequenceNumber()));
        if (other != null) {
            report.conflicts.add("sessão " + e.getSessionId() + ", sequence_number " + e.getSequenceNumber()
                    + ": eventos diferentes em " + location(other) + " e " + location(e));
            return;
        }
        byEventId.put(e.getEventId(), e);
        session.put(Integer.valueOf(e.getSequenceNumber()), e);
        report.eventsAccepted++;
    }

    private List<SessionData> buildSessions() {
        List<SessionData> sessions = new ArrayList<SessionData>();
        for (Map.Entry<String, TreeMap<Integer, SourceEvent>> entry : bySession.entrySet()) {
            SessionData s = new SessionData(entry.getKey(), new ArrayList<SourceEvent>(entry.getValue().values()));
            sessions.add(s);
            String id = "sessão " + s.getSessionId() + " (" + s.getParticipantId() + ")";
            if (!s.hasSessionStart()) {
                report.sessionWarnings.add(id + ": não começa com SESSION_START");
            }
            if (!s.getMissingSequenceNumbers().isEmpty()) {
                report.sessionWarnings.add(id + ": faltam os eventos de sequência " + s.getMissingSequenceNumbers());
            }
            if (!s.hasSessionEnd()) {
                report.sessionWarnings.add(id + ": sem SESSION_END (jogo fechado de forma inesperada ou sessão "
                        + "ainda aberta)");
            }
        }
        Collections.sort(sessions, new Comparator<SessionData>() {
            public int compare(SessionData a, SessionData b) {
                int c = a.getStartTime().toInstant().compareTo(b.getStartTime().toInstant());
                return c != 0 ? c : a.getSessionId().compareTo(b.getSessionId());
            }
        });
        return sessions;
    }

    private static String location(SourceEvent e) {
        return e.getSourceFile() + ":" + e.getLineNumber();
    }

    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String s : items) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    static byte[] readAll(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            byte[] buf = new byte[(int) f.length()];
            int off = 0;
            int n;
            while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) {
                off += n;
            }
            if (off < buf.length) {
                byte[] shorter = new byte[off];
                System.arraycopy(buf, 0, shorter, 0, off);
                return shorter;
            }
            return buf;
        } finally {
            in.close();
        }
    }
}
