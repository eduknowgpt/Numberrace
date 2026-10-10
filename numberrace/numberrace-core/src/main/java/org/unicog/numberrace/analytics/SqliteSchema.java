package org.unicog.numberrace.analytics;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Estrutura do banco SQLite de eventos ({@code schema.sql}, cópia de
 * evolution/pt/persistencia-eventos.sql), usada pelo jogo e pelo exportador.
 */
public final class SqliteSchema {

    /** Inserção de um evento, com as colunas na ordem da tabela {@code event}. */
    public static final String INSERT_EVENT = "INSERT INTO event (event_id, schema_version, event_type, timestamp, "
            + "session_id, participant_id, sequence_number, activity, game_number, turn_number, event_json) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

    private SqliteSchema() {
    }

    /** Comandos SQL de {@code schema.sql}, sem os comentários. */
    public static String[] statements() throws IOException {
        InputStream in = SqliteSchema.class.getResourceAsStream("schema.sql");
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
        List<String> statements = new ArrayList<String>();
        for (String p : noComments.split(";")) {
            if (p.trim().length() > 0) {
                statements.add(p.trim());
            }
        }
        return statements.toArray(new String[statements.size()]);
    }
}
