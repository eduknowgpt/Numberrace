package org.unicog.numberrace.analytics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Correspondência entre participante (sobrenome e nome do cadastro do jogo) e código
 * pseudonimizado. Fica em {@code identification/participants.csv}, fora da pasta de dados de
 * interação, e nunca é exportada. A equipe de pesquisa pode editar os códigos no arquivo.
 */
public final class ParticipantRegistry {

    static final String FILE_NAME = "participants.csv";
    private static final String HEADER = "participant_id,last_name,first_name,created_at";
    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Logger LOG = Logger.getLogger("org.unicog.numberrace.analytics");

    private final File file;
    private final SecureRandom random = new SecureRandom();

    public ParticipantRegistry(File identificationDir) {
        this.file = new File(identificationDir, FILE_NAME);
    }

    /** Devolve o código do participante, criando um novo se ele ainda não estiver no arquivo. */
    public synchronized String codeFor(String lastName, String firstName) {
        String last = lastName == null ? "" : lastName.trim();
        String first = firstName == null ? "" : firstName.trim();
        List<String[]> rows;
        try {
            rows = read();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Não foi possível ler " + file + "; um código novo será usado nesta sessão", e);
            return newCode(new ArrayList<String[]>());
        }
        for (String[] row : rows) {
            if (row.length >= 3 && row[1].equals(last) && row[2].equals(first)) {
                return row[0];
            }
        }
        String code = newCode(rows);
        try {
            append(code, last, first);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Não foi possível gravar o código em " + file, e);
        }
        return code;
    }

    private String newCode(List<String[]> rows) {
        while (true) {
            StringBuilder sb = new StringBuilder("P");
            for (int i = 0; i < 6; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            String code = sb.toString();
            boolean used = false;
            for (String[] row : rows) {
                used |= row.length > 0 && row[0].equals(code);
            }
            if (!used) {
                return code;
            }
        }
    }

    private List<String[]> read() throws IOException {
        List<String[]> rows = new ArrayList<String[]>();
        if (!file.exists()) {
            return rows;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), UTF8));
        try {
            String line;
            boolean header = true;
            while ((line = in.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.trim().length() > 0) {
                    rows.add(parse(line));
                }
            }
        } finally {
            in.close();
        }
        return rows;
    }

    private void append(String code, String last, String first) throws IOException {
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Não foi possível criar " + dir);
        }
        boolean isNew = !file.exists();
        Writer out = new OutputStreamWriter(new FileOutputStream(file, true), UTF8);
        try {
            if (isNew) {
                out.write(HEADER + "\n");
            }
            out.write(quote(code) + "," + quote(last) + "," + quote(first) + ","
                    + quote(InteractionEvent.TIMESTAMP.format(OffsetDateTime.now())) + "\n");
        } finally {
            out.close();
        }
    }

    static String quote(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    static String[] parse(String line) {
        List<String> fields = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[fields.size()]);
    }
}
