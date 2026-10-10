package org.unicog.numberrace.analytics.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Grava uma {@link Table} em CSV (RFC 4180: aspas quando necessário, fim de linha CRLF), em
 * duas versões:
 * <ul>
 * <li>{@link Flavor#STANDARD}: vírgula como separador e ponto como decimal, UTF-8 sem BOM, para
 * R, Python e outros programas;</li>
 * <li>{@link Flavor#EXCEL}: ponto e vírgula como separador e vírgula como decimal, UTF-8 com
 * BOM, para abrir com dois cliques no Excel em português.</li>
 * </ul>
 * Valores verdadeiro/falso são gravados como {@code true}/{@code false}; valores ausentes, como
 * célula vazia.
 */
final class CsvWriter {

    enum Flavor {
        STANDARD(',', '.', false), EXCEL(';', ',', true);

        final char separator;
        final char decimal;
        final boolean bom;

        Flavor(char separator, char decimal, boolean bom) {
            this.separator = separator;
            this.decimal = decimal;
            this.bom = bom;
        }
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private CsvWriter() {
    }

    /** Grava a tabela e devolve o número de linhas de dados. */
    static int write(Table table, File file, Flavor flavor) throws IOException {
        Writer w = new OutputStreamWriter(new FileOutputStream(file), UTF8);
        try {
            if (flavor.bom) {
                w.write('\uFEFF');
            }
            List<Table.Column> columns = table.getColumns();
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    line.append(flavor.separator);
                }
                line.append(quote(columns.get(i).name, flavor));
            }
            w.write(line.append("\r\n").toString());
            for (Map<String, Object> row : table.getRows()) {
                line.setLength(0);
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) {
                        line.append(flavor.separator);
                    }
                    line.append(format(row.get(columns.get(i).name), flavor));
                }
                w.write(line.append("\r\n").toString());
            }
        } finally {
            w.close();
        }
        return table.getRowCount();
    }

    static String format(Object v, Flavor flavor) {
        if (v == null) {
            return "";
        }
        if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return "";
            }
            String s = BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
            return flavor.decimal == '.' ? s : s.replace('.', flavor.decimal);
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        return quote(v.toString(), flavor);
    }

    private static String quote(String s, Flavor flavor) {
        boolean needs = s.indexOf(flavor.separator) >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0
                || s.indexOf('\r') >= 0 || (s.length() > 0 && (s.charAt(0) == ' ' || s.charAt(s.length() - 1) == ' '));
        if (!needs) {
            return s;
        }
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
