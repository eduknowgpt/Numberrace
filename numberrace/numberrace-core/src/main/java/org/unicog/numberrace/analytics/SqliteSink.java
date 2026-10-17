package org.unicog.numberrace.analytics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cópia para consulta: insere cada evento no banco SQLite {@code interaction.db}. Funciona como
 * "melhor esforço": se o banco falhar, fica desativado até o fim da sessão, e a importação
 * completa o banco a partir do JSON Lines depois.
 */
final class SqliteSink implements EventSink {

    private static final Logger LOG = Logger.getLogger("org.unicog.numberrace.analytics");
    private static final String INSERT = "INSERT INTO event (event_id, schema_version, event_type, timestamp, "
            + "session_id, participant_id, sequence_number, activity, game_number, turn_number, event_json) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

    private final File dbFile;
    private final File nativeDir;
    private Connection connection;
    private PreparedStatement insert;
    private String disabledSession;

    SqliteSink(File dbFile, File nativeDir) {
        this.dbFile = dbFile;
        this.nativeDir = nativeDir;
    }

    public void write(InteractionEvent e) {
        if (e.getSessionId().equals(disabledSession)) {
            return;
        }
        try {
            open();
            insert.setString(1, e.getEventId());
            insert.setString(2, InteractionEvent.SCHEMA_VERSION);
            insert.setString(3, e.getEventType().name());
            insert.setString(4, e.getTimestamp());
            insert.setString(5, e.getSessionId());
            insert.setString(6, e.getParticipantId());
            insert.setInt(7, e.getSequenceNumber());
            insert.setString(8, e.getActivity());
            setInteger(9, e.getGameNumber());
            setInteger(10, e.getTurnNumber());
            insert.setString(11, e.toJson());
            insert.executeUpdate();
        } catch (Throwable t) {
            // inclui UnsatisfiedLinkError e NoClassDefFoundError (driver ou biblioteca nativa ausente)
            LOG.log(Level.WARNING, "SQLite desativado até o fim da sessão " + e.getSessionId()
                    + "; o registro continua no JSON Lines", t);
            disabledSession = e.getSessionId();
            close();
        }
    }

    private void setInteger(int index, Integer value) throws SQLException {
        if (value == null) {
            insert.setNull(index, Types.INTEGER);
        } else {
            insert.setInt(index, value.intValue());
        }
    }

    private void open() throws Exception {
        if (connection != null) {
            return;
        }
        if (System.getProperty("org.sqlite.tmpdir") == null) {
            if (nativeDir.isDirectory()) {
                File[] old = nativeDir.listFiles();
                for (int i = 0; old != null && i < old.length; i++) {
                    old[i].delete(); // restos de interrupções anteriores; falha se ainda estiver em uso
                }
            } else {
                nativeDir.mkdirs();
            }
            System.setProperty("org.sqlite.tmpdir", nativeDir.getAbsolutePath());
        }
        Class.forName("org.sqlite.JDBC");
        File dir = dbFile.getParentFile();
        if (dir != null && !dir.isDirectory()) {
            dir.mkdirs();
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        Statement s = connection.createStatement();
        try {
            for (String sql : schemaStatements()) {
                s.execute(sql);
            }
        } finally {
            s.close();
        }
        insert = connection.prepareStatement(INSERT);
    }

    static String[] schemaStatements() throws IOException {
        InputStream in = SqliteSink.class.getResourceAsStream("schema.sql");
        if (in == null) {
            throw new IOException("schema.sql não encontrado");
        }
        StringBuilder sb = new StringBuilder();
        Reader r = new InputStreamReader(in, "UTF-8");
        try {
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
        } finally {
            r.close();
        }
        String noComments = sb.toString().replaceAll("(?m)^\\s*--.*$", "");
        String[] parts = noComments.split(";");
        java.util.List<String> statements = new java.util.ArrayList<String>();
        for (String p : parts) {
            if (p.trim().length() > 0) {
                statements.add(p.trim());
            }
        }
        return statements.toArray(new String[statements.size()]);
    }

    public void sessionEnded(String sessionId) {
        if (sessionId.equals(disabledSession)) {
            disabledSession = null; // a próxima sessão tenta de novo
        }
    }

    public void close() {
        try {
            if (insert != null) {
                insert.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // nada a fazer
        }
        insert = null;
        connection = null;
    }
}
