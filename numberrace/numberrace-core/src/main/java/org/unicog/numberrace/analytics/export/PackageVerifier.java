package org.unicog.numberrace.analytics.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.unicog.numberrace.analytics.Json;

/**
 * Confere um pacote de exportação com o seu manifesto: cada arquivo existe, tem o tamanho e a
 * impressão digital (SHA-256) registrados e o número de linhas de dados esperado; cada linha de
 * {@code events.jsonl} é JSON; o banco tem um evento por linha do arquivo.
 */
final class PackageVerifier {

    private PackageVerifier() {
    }

    /** Lista os problemas encontrados; lista vazia significa pacote íntegro. */
    static List<String> verify(File folder) {
        List<String> problems = new ArrayList<String>();
        File manifestFile = new File(folder, PackageWriter.MANIFEST);
        Map<String, Object> manifest;
        try {
            Object parsed = Json.parse(new String(EventImporter.readAll(manifestFile), Charset.forName("UTF-8")).trim());
            manifest = asMap(parsed);
        } catch (Exception e) {
            problems.add(PackageWriter.MANIFEST + " ausente ou ilegível: " + e.getMessage());
            return problems;
        }
        Object files = manifest.get("files");
        if (!(files instanceof List)) {
            problems.add(PackageWriter.MANIFEST + " sem a lista de arquivos");
            return problems;
        }
        for (Object o : (List<?>) files) {
            Map<String, Object> entry = asMap(o);
            String path = String.valueOf(entry.get("path"));
            File f = new File(folder, path.replace('/', File.separatorChar));
            if (!f.isFile()) {
                problems.add(path + ": arquivo ausente");
                continue;
            }
            try {
                Object bytes = entry.get("bytes");
                if (bytes instanceof Number && ((Number) bytes).longValue() != f.length()) {
                    problems.add(path + ": tamanho diferente do manifesto (" + f.length() + " em vez de " + bytes + ")");
                }
                if (!PackageWriter.sha256(f).equals(entry.get("sha256"))) {
                    problems.add(path + ": impressão digital (SHA-256) diferente do manifesto");
                }
                Object rows = entry.get("rows");
                if (rows instanceof Number) {
                    int expected = ((Number) rows).intValue();
                    int actual = countRows(f, path);
                    if (actual != expected) {
                        problems.add(path + ": " + actual + " linhas de dados em vez de " + expected);
                    }
                }
            } catch (Exception e) {
                problems.add(path + ": não foi possível conferir (" + e.getMessage() + ")");
            }
        }
        return problems;
    }

    private static int countRows(File f, String path) throws Exception {
        if (path.endsWith(".jsonl")) {
            int n = 0;
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), Charset.forName("UTF-8")));
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.trim().length() > 0) {
                        if (!(Json.parse(line) instanceof Map)) {
                            throw new IOException("linha " + (n + 1) + " não é um objeto JSON");
                        }
                        n++;
                    }
                }
            } finally {
                in.close();
            }
            return n;
        }
        if (path.endsWith(".csv")) {
            char separator = path.startsWith("excel/") ? CsvWriter.Flavor.EXCEL.separator
                    : CsvWriter.Flavor.STANDARD.separator;
            final int[] counter = { 0, -1 }; // registros lidos, colunas do cabeçalho
            CsvReader.read(f, separator, new CsvReader.RecordHandler() {
                public void record(List<String> fields) throws IOException {
                    if (counter[1] < 0) {
                        counter[1] = fields.size();
                    } else if (fields.size() != counter[1]) {
                        throw new IOException("linha " + (counter[0] + 1) + " com " + fields.size()
                                + " colunas em vez de " + counter[1]);
                    }
                    counter[0]++;
                }
            });
            return Math.max(0, counter[0] - 1);
        }
        if (path.endsWith(".db")) {
            Class.forName("org.sqlite.JDBC");
            Connection c = DriverManager.getConnection("jdbc:sqlite:" + f.getAbsolutePath());
            try {
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM event");
                rs.next();
                int n = rs.getInt(1);
                s.close();
                return n;
            } finally {
                c.close();
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("objeto JSON esperado");
        }
        return (Map<String, Object>) o;
    }
}
