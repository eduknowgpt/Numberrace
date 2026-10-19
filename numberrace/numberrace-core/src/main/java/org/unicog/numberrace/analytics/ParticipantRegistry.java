package org.unicog.numberrace.analytics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Correspondência entre participante (sobrenome e nome do cadastro do jogo) e código
 * pseudonimizado. Fica em {@code identification/participants.csv}, fora da pasta de dados de
 * interação, e nunca é exportada. A equipe de pesquisa pode editar os códigos no arquivo, inclusive
 * no Excel em português: cada linha é lida com vírgula ou ponto e vírgula, em UTF-8 ou em
 * Windows-1252.
 */
public final class ParticipantRegistry {

    static final String FILE_NAME = "participants.csv";
    private static final String HEADER = "participant_id,last_name,first_name,created_at";
    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Charset EXCEL = excelCharset();
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
            if (row.length >= 3 && row[1].trim().equals(last) && row[2].trim().equals(first)
                    && row[0].trim().length() > 0) {
                return row[0].trim();
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
                used |= row.length > 0 && row[0].trim().equals(code);
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
        byte[] bytes = readAll(file);
        boolean header = true;
        int start = 0;
        for (int i = 0; i <= bytes.length; i++) {
            if (i == bytes.length || bytes[i] == '\n') {
                String line = decode(bytes, start, i - start);
                start = i + 1;
                if (header) {
                    header = false;
                } else if (line.trim().length() > 0) {
                    rows.add(parse(line));
                }
            }
        }
        return rows;
    }

    /**
     * Uma linha do arquivo: em UTF-8 ou, se não for UTF-8 válido, em Windows-1252 (codificação do
     * Excel em português ao salvar como CSV). A decisão é por linha porque, depois de uma edição no
     * Excel, as linhas novas continuam sendo gravadas em UTF-8.
     */
    static String decode(byte[] bytes, int offset, int length) {
        String line;
        try {
            line = UTF8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, length)).toString();
        } catch (CharacterCodingException e) {
            line = new String(bytes, offset, length, EXCEL);
        }
        if (line.startsWith("\uFEFF")) {
            line = line.substring(1);
        }
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    private static byte[] readAll(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static Charset excelCharset() {
        try {
            return Charset.forName("windows-1252");
        } catch (RuntimeException e) {
            return Charset.forName("ISO-8859-1");
        }
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

    /** Separa os campos de uma linha, com vírgula ou, se não houver vírgula fora de aspas, ponto e vírgula. */
    static String[] parse(String line) {
        char separator = separatorOf(line);
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
            } else if (c == separator) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[fields.size()]);
    }

    private static char separatorOf(String line) {
        boolean inQuotes = false;
        boolean semicolon = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && c == ',') {
                return ',';
            } else if (!inQuotes && c == ';') {
                semicolon = true;
            }
        }
        return semicolon ? ';' : ',';
    }
}
