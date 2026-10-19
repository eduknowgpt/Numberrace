package org.unicog.numberrace.analytics.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.unicog.numberrace.analytics.Json;
import org.unicog.numberrace.analytics.SqliteSchema;

/**
 * Grava o pacote de exportação numa pasta nova:
 *
 * <pre>
 * exportacao_AAAA-MM-DD_HHMMSS/
 *   LEIA-ME.txt, manifest.json, relatorio-importacao.txt
 *   events.jsonl      eventos exportados, sem alteração (dado bruto)
 *   interaction.db    SQLite: tabela event (como no jogo) e tabelas session e turn
 *   csv/              events, sessions, turns e dicionario-dados (vírgula, ponto decimal)
 *   excel/            os mesmos arquivos para o Excel em português (ponto e vírgula, vírgula decimal)
 * </pre>
 *
 * O manifesto lista cada arquivo com tamanho, número de linhas de dados e SHA-256.
 */
final class PackageWriter {

    static final String PACKAGE_FORMAT = "1";
    static final String MANIFEST = "manifest.json";
    static final String EVENTS_JSONL = "events.jsonl";
    static final String DATABASE = "interaction.db";
    static final String DICTIONARY = "dicionario-dados";

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int BATCH = 1000;
    private static final DateTimeFormatter FOLDER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    /** Resultado da gravação. */
    static final class Result {
        File folder;
        File zip;
        Map<String, Object> manifest;
        final List<String> warnings = new ArrayList<String>();
    }

    private final List<Map<String, Object>> fileEntries = new ArrayList<Map<String, Object>>();
    private File folder;

    Result write(File outputRoot, List<SessionData> sessions, ImportReport report, ExportFilter filter,
            int sessionsExcluded, boolean zip) throws IOException {
        Result result = new Result();
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        folder = newFolder(outputRoot, "exportacao_" + FOLDER_TIME.format(now));
        result.folder = folder;

        Table events = Tables.events(sessions);
        Table sessionTable = Tables.sessions(sessions);
        Table turns = Tables.turns(sessions);
        Table dictionary = dictionary(events, sessionTable, turns);

        writeJsonLines(sessions);
        for (CsvWriter.Flavor flavor : CsvWriter.Flavor.values()) {
            String dir = flavor == CsvWriter.Flavor.EXCEL ? "excel" : "csv";
            String how = flavor == CsvWriter.Flavor.EXCEL
                    ? " (Excel em português: ponto e vírgula, vírgula decimal)"
                    : " (vírgula, ponto decimal)";
            for (Table t : new Table[] { events, sessionTable, turns, dictionary }) {
                String path = dir + "/" + t.getName() + ".csv";
                File f = file(path);
                f.getParentFile().mkdirs();
                int rows = CsvWriter.write(t, f, flavor);
                entry(path, rows, t.getDescription() + how);
            }
        }
        try {
            writeDatabase(sessions, sessionTable, turns, now);
            entry(DATABASE, events.getRowCount(), "Banco SQLite: tabela event (como no jogo) e tabelas "
                    + "session e turn (as mesmas de sessions.csv e turns.csv)");
        } catch (Throwable t) {
            file(DATABASE).delete();
            result.warnings.add("O banco SQLite não foi gerado (" + t + "). Os demais arquivos estão completos.");
        }

        Set<String> versions = new LinkedHashSet<String>();
        int games = 0;
        Set<String> participants = new LinkedHashSet<String>();
        for (SessionData s : sessions) {
            participants.add(s.getParticipantId());
            for (SourceEvent e : s.getEvents()) {
                versions.add(e.getSchemaVersion());
                if (e.is("GAME_START")) {
                    games++;
                }
            }
        }
        Map<String, Object> counts = Json.obj("participants", Integer.valueOf(participants.size()),
                "sessions", Integer.valueOf(sessions.size()), "games", Integer.valueOf(games),
                "turns", Integer.valueOf(turns.getRowCount()), "events", Integer.valueOf(events.getRowCount()));

        writeText("relatorio-importacao.txt", reportText(report, filter, sessionsExcluded, counts, result.warnings));
        entry("relatorio-importacao.txt", -1, "O que a importação encontrou: arquivos lidos, eventos descartados, "
                + "conflitos e avisos sobre sessões");
        writeText("LEIA-ME.txt", readMe(now, counts, filter));
        entry("LEIA-ME.txt", -1, "Explicação do pacote, em português");

        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        manifest.put("package_format_version", PACKAGE_FORMAT);
        manifest.put("created_at", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        manifest.put("generator", "Number Race - exportador de dados de interação");
        manifest.put("generator_version", appVersion());
        manifest.put("event_schema_versions", new ArrayList<Object>(versions));
        manifest.put("filters", filter.describe());
        manifest.put("counts", counts);
        manifest.put("excluded_by_filter", Json.obj("sessions", Integer.valueOf(sessionsExcluded)));
        manifest.put("import", Json.obj("files_read", Integer.valueOf(report.filesRead),
                "lines_read", Integer.valueOf(report.linesRead),
                "events_accepted", Integer.valueOf(report.eventsAccepted),
                "duplicates_ignored", Integer.valueOf(report.duplicates),
                "incomplete_lines", Integer.valueOf(report.incompleteLines.size()),
                "invalid_events", Integer.valueOf(report.invalidEvents.size()),
                "conflicts", Integer.valueOf(report.conflicts.size()),
                "session_warnings", Integer.valueOf(report.sessionWarnings.size())));
        manifest.put("files", fileEntries);
        writeText(MANIFEST, Json.write(manifest) + "\n", false);
        result.manifest = manifest;

        if (zip) {
            result.zip = zipFolder(folder);
        }
        return result;
    }

    private static File newFolder(File root, String name) throws IOException {
        File f = new File(root, name);
        for (int i = 2; f.exists(); i++) {
            f = new File(root, name + "_" + i);
        }
        if (!f.mkdirs()) {
            throw new IOException("Não foi possível criar a pasta " + f.getAbsolutePath());
        }
        return f;
    }

    private File file(String path) {
        return new File(folder, path.replace('/', File.separatorChar));
    }

    private void entry(String path, int rows, String description) throws IOException {
        File f = file(path);
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("path", path);
        m.put("bytes", Long.valueOf(f.length()));
        m.put("rows", rows < 0 ? null : Integer.valueOf(rows));
        m.put("sha256", sha256(f));
        m.put("description", description);
        fileEntries.add(m);
    }

    private void writeJsonLines(List<SessionData> sessions) throws IOException {
        OutputStream out = new FileOutputStream(file(EVENTS_JSONL));
        int rows = 0;
        try {
            for (SessionData s : sessions) {
                for (SourceEvent e : s.getEvents()) {
                    out.write((e.getLine() + "\n").getBytes(UTF8));
                    rows++;
                }
            }
        } finally {
            out.close();
        }
        entry(EVENTS_JSONL, rows, "Dado bruto: os eventos exportados exatamente como foram gravados pelo jogo, "
                + "um por linha (JSON Lines)");
    }

    private void writeDatabase(List<SessionData> sessions, Table sessionTable, Table turns, OffsetDateTime now)
            throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + file(DATABASE).getAbsolutePath());
        try {
            c.setAutoCommit(false);
            Statement st = c.createStatement();
            for (String sql : SqliteSchema.statements()) {
                st.execute(sql);
            }
            st.execute(createTable("session", sessionTable));
            st.execute(createTable("turn", turns));
            st.close();
            PreparedStatement meta = c.prepareStatement("INSERT OR REPLACE INTO meta (key, value) VALUES (?, ?)");
            meta.setString(1, "export_created_at");
            meta.setString(2, now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            meta.executeUpdate();
            meta.setString(1, "export_package_format");
            meta.setString(2, PACKAGE_FORMAT);
            meta.executeUpdate();
            meta.close();
            PreparedStatement ins = c.prepareStatement(SqliteSchema.INSERT_EVENT);
            int pending = 0;
            for (SessionData s : sessions) {
                for (SourceEvent e : s.getEvents()) {
                    ins.setString(1, e.getEventId());
                    ins.setString(2, e.getSchemaVersion());
                    ins.setString(3, e.getEventType());
                    ins.setString(4, e.parse().timestamp());
                    ins.setString(5, e.getSessionId());
                    ins.setString(6, e.getParticipantId());
                    ins.setInt(7, e.getSequenceNumber());
                    ins.setObject(8, e.getActivity());
                    ins.setObject(9, e.getGameNumber());
                    ins.setObject(10, e.getTurnNumber());
                    ins.setString(11, e.getLine());
                    ins.addBatch();
                    if (++pending == BATCH) {
                        ins.executeBatch();
                        pending = 0;
                    }
                }
            }
            ins.executeBatch();
            ins.close();
            insertRows(c, "session", sessionTable);
            insertRows(c, "turn", turns);
            c.commit();
        } finally {
            c.close();
        }
    }

    private static String createTable(String name, Table t) {
        StringBuilder sb = new StringBuilder("CREATE TABLE ").append(name).append(" (");
        List<Table.Column> cols = t.getColumns();
        for (int i = 0; i < cols.size(); i++) {
            Table.Column col = cols.get(i);
            sb.append(i > 0 ? ", " : "").append(col.name).append(' ')
                    .append(col.type == Table.Type.TEXT ? "TEXT" : col.type == Table.Type.REAL ? "REAL" : "INTEGER");
        }
        return sb.append(')').toString();
    }

    private static void insertRows(Connection c, String name, Table t) throws Exception {
        List<Table.Column> cols = t.getColumns();
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(name).append(" VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            sql.append(i > 0 ? ",?" : "?");
        }
        PreparedStatement ps = c.prepareStatement(sql.append(')').toString());
        int pending = 0;
        for (Map<String, Object> row : t.getRows()) {
            for (int i = 0; i < cols.size(); i++) {
                Object v = row.get(cols.get(i).name);
                if (v instanceof Boolean) {
                    v = Integer.valueOf(((Boolean) v).booleanValue() ? 1 : 0);
                }
                ps.setObject(i + 1, v);
            }
            ps.addBatch();
            if (++pending == BATCH) {
                ps.executeBatch();
                pending = 0;
            }
        }
        ps.executeBatch();
        ps.close();
    }

    private static Table dictionary(Table... tables) {
        Table d = new Table(DICTIONARY, "Dicionário de dados: o significado de cada coluna das tabelas exportadas");
        d.column("table", Table.Type.TEXT, "", "Tabela (arquivo CSV ou tabela do banco)", "")
                .column("column", Table.Type.TEXT, "", "Nome da coluna", "")
                .column("type", Table.Type.TEXT, "", "Tipo: INTEGER, REAL, BOOLEAN (true/false) ou TEXT", "")
                .column("unit", Table.Type.TEXT, "", "Unidade ou formato", "")
                .column("description", Table.Type.TEXT, "", "Significado", "")
                .column("source", Table.Type.TEXT, "", "Evento e campo de origem (modelo-eventos.md)", "");
        for (Table t : tables) {
            for (Table.Column c : t.getColumns()) {
                Map<String, Object> r = d.newRow();
                r.put("table", t.getName());
                r.put("column", c.name);
                r.put("type", c.type.name());
                r.put("unit", c.unit);
                r.put("description", c.description);
                r.put("source", c.source);
            }
        }
        return d;
    }

    private void writeText(String name, String text) throws IOException {
        writeText(name, text, true);
    }

    /** Textos para pessoas: UTF-8 com BOM e fim de linha CRLF, para o Bloco de Notas. */
    private void writeText(String name, String text, boolean forPeople) throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(file(name)), UTF8);
        try {
            if (forPeople) {
                w.write('\uFEFF');
                w.write(text.replace("\r\n", "\n").replace("\n", "\r\n"));
            } else {
                w.write(text);
            }
        } finally {
            w.close();
        }
    }

    private static String reportText(ImportReport report, ExportFilter filter, int sessionsExcluded,
            Map<String, Object> counts, List<String> warnings) {
        StringBuilder sb = new StringBuilder("RELATÓRIO DA IMPORTAÇÃO E DA EXPORTAÇÃO\n\n");
        sb.append(report.toText());
        sb.append("\nFiltros: ").append(filter.describeText()).append('\n');
        sb.append("Sessões deixadas de fora pelos filtros: ").append(sessionsExcluded).append('\n');
        sb.append("Exportado: ").append(counts.get("participants")).append(" participante(s), ")
                .append(counts.get("sessions")).append(" sessão(ões), ").append(counts.get("games"))
                .append(" partida(s), ").append(counts.get("turns")).append(" rodada(s), ")
                .append(counts.get("events")).append(" evento(s)\n");
        for (String w : warnings) {
            sb.append("Aviso: ").append(w).append('\n');
        }
        return sb.toString();
    }

    private static String readMe(OffsetDateTime now, Map<String, Object> counts, ExportFilter filter) {
        return "NUMBER RACE - PACOTE DE EXPORTAÇÃO DOS DADOS DE INTERAÇÃO\n"
                + "\n"
                + "Gerado em: " + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "\n"
                + "Participantes: " + counts.get("participants") + " | Sessões: " + counts.get("sessions")
                + " | Partidas: " + counts.get("games") + " | Rodadas: " + counts.get("turns")
                + " | Eventos: " + counts.get("events") + "\n"
                + "Filtros: " + filter.describeText() + "\n"
                + "\n"
                + "O QUE TEM NESTE PACOTE\n"
                + "\n"
                + "events.jsonl            Dado bruto: os eventos exatamente como o jogo gravou, um por linha.\n"
                + "                        É a fonte oficial; as tabelas abaixo são montadas a partir dele.\n"
                + "excel\\                  Tabelas para abrir com dois cliques no Excel em português\n"
                + "                        (ponto e vírgula como separador, vírgula como decimal).\n"
                + "csv\\                    As mesmas tabelas no padrão internacional, para R, Python e outros\n"
                + "                        programas (vírgula como separador, ponto como decimal).\n"
                + "  turns.csv             Uma linha por rodada: números apresentados, resposta, tempo, resultado.\n"
                + "  sessions.csv          Uma linha por sessão: início, fim, partidas, rodadas, respostas.\n"
                + "  events.csv            Um evento por linha: parte comum em colunas e o resto em data_json.\n"
                + "  dicionario-dados.csv  O significado, a unidade e a origem de cada coluna.\n"
                + "interaction.db          Banco SQLite (abre no DB Browser for SQLite) com as tabelas event,\n"
                + "                        session e turn.\n"
                + "relatorio-importacao.txt  Problemas encontrados nos arquivos de origem.\n"
                + "manifest.json           Etiqueta do pacote: data, versões, filtros, quantidades e a\n"
                + "                        impressão digital (SHA-256) de cada arquivo.\n"
                + "\n"
                + "COMO ABRIR\n"
                + "\n"
                + "Excel: dois cliques em excel\\turns.csv (ou sessions.csv, events.csv).\n"
                + "R: read.csv(\"csv/turns.csv\", encoding = \"UTF-8\")\n"
                + "Python: pandas.read_csv(\"csv/turns.csv\")\n"
                + "SQLite: DB Browser for SQLite > Abrir banco de dados > interaction.db\n"
                + "\n"
                + "COMO CONFERIR SE O PACOTE CHEGOU INTEIRO\n"
                + "\n"
                + "Execute o exportador com a opção --verificar e o caminho desta pasta. Ele confere o tamanho,\n"
                + "a impressão digital e o número de linhas de cada arquivo com o manifest.json.\n"
                + "\n"
                + "PROTEÇÃO DOS DADOS\n"
                + "\n"
                + "Os participantes aparecem apenas por código pseudonimizado. A correspondência entre código e\n"
                + "nome fica no computador onde o jogo foi usado (pasta identification) e não faz parte deste\n"
                + "pacote. Guarde o pacote em local com acesso restrito, não o publique e siga o protocolo da\n"
                + "pesquisa quanto ao prazo de guarda e ao descarte.\n"
                + "\n"
                + "Os dados descrevem a interação com o jogo. Não constituem diagnóstico.\n";
    }

    static String appVersion() {
        try {
            InputStream in = PackageWriter.class.getResourceAsStream("/org/unicog/numberrace/analytics/analytics.properties");
            if (in != null) {
                try {
                    Properties p = new Properties();
                    p.load(in);
                    String v = p.getProperty("app.version");
                    if (v != null && !v.startsWith("$")) {
                        return v;
                    }
                } finally {
                    in.close();
                }
            }
        } catch (IOException ignored) {
            // segue para o valor padrão
        }
        return "unknown";
    }

    static String sha256(File f) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        InputStream in = new FileInputStream(f);
        try {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
        } finally {
            in.close();
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", Integer.valueOf(b & 0xff)));
        }
        return sb.toString();
    }

    private static File zipFolder(File dir) throws IOException {
        File zip = new File(dir.getParentFile(), dir.getName() + ".zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        try {
            addToZip(out, dir, dir.getName() + "/");
        } finally {
            out.close();
        }
        return zip;
    }

    private static void addToZip(ZipOutputStream out, File dir, String prefix) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        java.util.Arrays.sort(children);
        for (File f : children) {
            if (f.isDirectory()) {
                addToZip(out, f, prefix + f.getName() + "/");
            } else {
                out.putNextEntry(new ZipEntry(prefix + f.getName()));
                InputStream in = new FileInputStream(f);
                try {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }
                } finally {
                    in.close();
                }
                out.closeEntry();
            }
        }
    }
}
