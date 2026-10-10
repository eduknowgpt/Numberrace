package org.unicog.numberrace.analytics;

import java.io.File;
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
            for (String sql : SqliteSchema.statements()) {
                s.execute(sql);
            }
        } finally {
            s.close();
        }
        insert = connection.prepareStatement(SqliteSchema.INSERT_EVENT);
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
