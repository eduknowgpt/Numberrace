package org.unicog.numberrace.analytics.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/** Leitura de CSV (RFC 4180), usada para conferir os arquivos exportados. */
final class CsvReader {

    /** Recebe cada registro lido (a primeira chamada é o cabeçalho). */
    interface RecordHandler {
        void record(List<String> fields) throws IOException;
    }

    private CsvReader() {
    }

    /** Lê o arquivo inteiro na memória (para arquivos pequenos e testes). */
    static List<List<String>> read(File file, char separator) throws IOException {
        final List<List<String>> records = new ArrayList<List<String>>();
        read(file, separator, new RecordHandler() {
            public void record(List<String> fields) {
                records.add(fields);
            }
        });
        return records;
    }

    /** Lê o arquivo registro por registro, sem guardá-lo na memória. Remove o BOM, se houver. */
    static void read(File file, char separator, RecordHandler handler) throws IOException {
        Reader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")), 65536);
        try {
            List<String> record = new ArrayList<String>();
            StringBuilder field = new StringBuilder();
            boolean quoted = false;
            boolean any = false;
            boolean first = true;
            int c;
            while ((c = in.read()) >= 0) {
                if (first) {
                    first = false;
                    if (c == '\uFEFF') {
                        continue;
                    }
                }
                if (quoted) {
                    if (c == '"') {
                        in.mark(1);
                        int next = in.read();
                        if (next == '"') {
                            field.append('"');
                        } else {
                            quoted = false;
                            if (next >= 0) {
                                in.reset();
                            }
                        }
                    } else {
                        field.append((char) c);
                    }
                } else if (c == '"') {
                    quoted = true;
                    any = true;
                } else if (c == separator) {
                    record.add(field.toString());
                    field.setLength(0);
                    any = true;
                } else if (c == '\r') {
                    continue;
                } else if (c == '\n') {
                    record.add(field.toString());
                    handler.record(record);
                    record = new ArrayList<String>();
                    field.setLength(0);
                    any = false;
                } else {
                    field.append((char) c);
                    any = true;
                }
            }
            if (quoted) {
                throw new IOException(file.getName() + ": aspas não fechadas");
            }
            if (any || field.length() > 0) {
                record.add(field.toString());
                handler.record(record);
            }
        } finally {
            in.close();
        }
    }
}
