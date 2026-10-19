package org.unicog.numberrace.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

/**
 * O JSON próprio do componente (sem biblioteca externa, decisão D30): o que é gravado volta igual
 * na leitura, e qualquer texto que não seja JSON válido é recusado com
 * {@link IllegalArgumentException}, a única exceção que a importação espera.
 */
public class JsonRoundTripTest {

    /** Letras comuns e os casos difíceis: aspas, barras, controle, acentos, emoji e separadores Unicode. */
    private static final String POOL = "abcXYZ019 _-,;:{}[]\"\\/\b\f\n\r\t\u0000\u0001\u001f\u007f"
            + "áéíóúãõçÁÉÍÓÚÃÕÇ€ñü\u2028\u2029\uD83D\uDE00\uD83C\uDFB2";

    @Test
    public void textosQuaisquerVoltamIguais() {
        Random r = new Random(42);
        for (int i = 0; i < 2000; i++) {
            String key = text(r, 1 + r.nextInt(8));
            String value = text(r, r.nextInt(40));
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put(key, value);
            m.put("lista", new ArrayList<Object>(Arrays.asList(value, key)));
            String json = Json.write(m);
            assertTrue(json, json.indexOf('\n') < 0 && json.indexOf('\r') < 0); // uma linha por evento
            assertEquals(json, m, Json.parse(json));
        }
    }

    @Test
    public void numerosVoltamComOMesmoValor() {
        long[] longs = { 0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE,
            (long) Integer.MAX_VALUE + 1 };
        for (long v : longs) {
            assertEquals(Long.valueOf(v), Json.parse(Json.write(Long.valueOf(v))));
        }
        assertEquals(Long.valueOf(7), Json.parse(Json.write(Integer.valueOf(7))));
        double[] doubles = { 0.5, -1.25, 1e-7, 123456.789, 0.431, 1e20, -0.0, 3.0 };
        for (double v : doubles) {
            assertEquals(v, ((Number) Json.parse(Json.write(Double.valueOf(v)))).doubleValue(), 0.0);
        }
        // sem valor numérico possível: gravado como null, e a linha continua sendo JSON válido
        assertEquals("[null,null]", Json.write(Arrays.asList(Double.valueOf(Double.NaN),
                Double.valueOf(Double.POSITIVE_INFINITY))));
    }

    @Test
    public void estruturasAninhadasVoltamIguais() {
        Map<String, Object> e = Json.obj("data", Json.obj("hazards_on_board",
                Arrays.asList(Json.obj("square", Long.valueOf(7), "penalty", Long.valueOf(1))),
                "representation", Json.obj("dots", Boolean.TRUE, "spoken", Boolean.FALSE), "deadline_ms", null));
        assertEquals(e, Json.parse(Json.write(e)));
        assertEquals("{\"a\":[1,2,3]}", Json.write(Json.obj("a", new int[] { 1, 2, 3 })));
    }

    /** Teste de robustez: pedaços e versões estragadas de eventos reais. */
    @Test
    public void textoInvalidoERecusadoSemOutraExcecao() throws Exception {
        List<String> lines = realLines();
        Random r = new Random(7);
        int rejected = 0;
        for (String line : lines.subList(0, 40)) {
            List<String> variants = new ArrayList<String>();
            for (int i = 0; i < line.length(); i++) {
                variants.add(line.substring(0, i)); // gravação interrompida em qualquer ponto
            }
            for (int k = 0; k < 100; k++) {
                char[] c = line.toCharArray();
                for (int j = 0; j < 3; j++) {
                    c[r.nextInt(c.length)] = (char) r.nextInt(128);
                }
                variants.add(new String(c));
            }
            variants.add(line + "}");
            variants.add(line + line);
            for (String v : variants) {
                try {
                    Json.parse(v);
                } catch (IllegalArgumentException expected) {
                    rejected++;
                } catch (Throwable t) {
                    fail(t + " ao ler: " + v);
                }
            }
        }
        assertTrue(rejected > 10000);
        for (String v : new String[] { "", " ", "{", "{\"a\"", "{\"a\":}", "{,}", "[1,]", "\"\\u12\"", "\"\\x\"",
            "{\"a\":-}", "{\"a\":tru}", "nul", "\"sem fim" }) {
            try {
                Json.parse(v);
                fail("aceitou: " + v);
            } catch (IllegalArgumentException expected) {
                // recusado como deve
            }
        }
    }

    @Test
    public void aninhamentoExageradoERecusadoSemEstourarAPilha() {
        // P12: uma linha corrompida com milhares de níveis derrubava a exportação inteira
        for (int depth : new int[] { 65, 10000, 100000 }) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                sb.append('[');
            }
            try {
                Json.parse(sb.toString());
                fail("aceitou " + depth + " níveis");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("níveis"));
            }
        }
        StringBuilder ok = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            ok.append('[');
        }
        for (int i = 0; i < 64; i++) {
            ok.append(']');
        }
        Json.parse(ok.toString()); // 64 níveis ainda são aceitos
    }

    private static String text(Random r, int length) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            int i = r.nextInt(POOL.length());
            char c = POOL.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < POOL.length()) {
                sb.append(c).append(POOL.charAt(i + 1)); // emoji inteiro
            } else if (!Character.isLowSurrogate(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static List<String> realLines() throws Exception {
        InputStream in = JsonRoundTripTest.class.getResourceAsStream("export/sessao-teste.jsonl");
        try {
            byte[] buf = new byte[200000];
            int off = 0;
            int n;
            while ((n = in.read(buf, off, buf.length - off)) > 0) {
                off += n;
            }
            List<String> lines = new ArrayList<String>();
            for (String l : new String(buf, 0, off, "UTF-8").replace("\r\n", "\n").split("\n")) {
                if (l.trim().length() > 0) {
                    lines.add(l);
                }
            }
            return lines;
        } finally {
            in.close();
        }
    }
}
