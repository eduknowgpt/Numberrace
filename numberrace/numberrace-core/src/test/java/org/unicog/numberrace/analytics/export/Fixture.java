package org.unicog.numberrace.analytics.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dados de teste: {@code sessao-teste.jsonl} é a sessão gravada pelo jogo modificado no teste no
 * Windows (participante de teste {@code P4WVSUY}, 2 partidas, 11 rodadas, 180 eventos).
 */
final class Fixture {

    static final String SESSION_ID = "5fb5269a-ab53-4ee9-bf7a-f8489e351cf4";
    static final String PARTICIPANT = "P4WVSUY";
    static final Charset UTF8 = Charset.forName("UTF-8");

    private Fixture() {
    }

    /** As 180 linhas da sessão de teste. */
    static List<String> lines() throws IOException {
        InputStream in = Fixture.class.getResourceAsStream("sessao-teste.jsonl");
        try {
            byte[] buf = new byte[200000];
            int off = 0;
            int n;
            while ((n = in.read(buf, off, buf.length - off)) > 0) {
                off += n;
            }
            String text = new String(buf, 0, off, UTF8);
            return new ArrayList<String>(Arrays.asList(text.split("\n")));
        } finally {
            in.close();
        }
    }

    /** Grava as linhas como a pasta {@code interaction-data} de um computador. */
    static File computer(File root, List<String> lines, boolean finalNewline) throws IOException {
        File dir = new File(root, "interaction-data/events/" + PARTICIPANT);
        dir.mkdirs();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1 || finalNewline) {
                sb.append('\n');
            }
        }
        OutputStream out = new FileOutputStream(new File(dir, "2026-10-09_182904_" + SESSION_ID + ".jsonl"));
        try {
            out.write(sb.toString().getBytes(UTF8));
        } finally {
            out.close();
        }
        return new File(root, "interaction-data");
    }
}
