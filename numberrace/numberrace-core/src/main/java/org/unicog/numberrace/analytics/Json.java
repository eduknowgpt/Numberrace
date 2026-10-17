package org.unicog.numberrace.analytics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Escrita e leitura mínimas de JSON (sem bibliotecas externas), suficientes para os eventos do
 * modelo. Objetos são {@link Map} (a ordem das chaves é mantida com {@link LinkedHashMap}).
 */
public final class Json {

    private Json() {
    }

    /** Cria um objeto JSON a partir de pares chave, valor. */
    public static Map<String, Object> obj(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("pares chave/valor incompletos");
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb.toString();
    }

    /**
     * Lê um texto JSON. Objetos viram {@link LinkedHashMap}, listas viram {@link ArrayList},
     * números inteiros viram {@link Long} e os demais números, {@link Double}.
     *
     * @throws IllegalArgumentException se o texto não for JSON válido
     */
    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipSpaces();
        Object value = p.value();
        p.skipSpaces();
        if (p.pos != text.length()) {
            throw p.error("conteúdo depois do fim do JSON");
        }
        return value;
    }

    private static void append(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            appendString(sb, (String) v);
        } else if (v instanceof Boolean) {
            sb.append(v.toString());
        } else if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) {
            sb.append(v.toString());
        } else if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append("null");
            } else if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                sb.append((long) d).append(".0");
            } else {
                sb.append(Double.toString(d));
            }
        } else if (v instanceof Map) {
            sb.append('{');
            Iterator<?> it = ((Map<?, ?>) v).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) it.next();
                appendString(sb, String.valueOf(e.getKey()));
                sb.append(':');
                append(sb, e.getValue());
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append('}');
        } else if (v instanceof Collection) {
            sb.append('[');
            Iterator<?> it = ((Collection<?>) v).iterator();
            while (it.hasNext()) {
                append(sb, it.next());
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append(']');
        } else if (v instanceof int[]) {
            int[] a = (int[]) v;
            sb.append('[');
            for (int i = 0; i < a.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(a[i]);
            }
            sb.append(']');
        } else {
            appendString(sb, v.toString());
        }
    }

    private static void appendString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '"':
                sb.append("\\\"");
                break;
            case '\\':
                sb.append("\\\\");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            default:
                if (c < 0x20) {
                    sb.append(String.format("\\u%04x", (int) c));
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    private static final class Parser {

        /**
         * Profundidade máxima de objetos e listas. Os eventos do modelo têm no máximo quatro
         * níveis; o limite evita que uma linha corrompida com milhares de níveis estoure a pilha.
         */
        private static final int MAX_DEPTH = 64;

        private final String s;
        private int pos;
        private int depth;

        Parser(String s) {
            this.s = s;
        }

        Object value() {
            if (pos >= s.length()) {
                throw error("fim inesperado");
            }
            char c = s.charAt(pos);
            if (c == '{' || c == '[') {
                if (++depth > MAX_DEPTH) {
                    throw error("mais de " + MAX_DEPTH + " níveis de objetos e listas");
                }
                Object nested = c == '{' ? object() : array();
                depth--;
                return nested;
            } else if (c == '"') {
                return string();
            } else if (c == '-' || (c >= '0' && c <= '9')) {
                return number();
            } else if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            } else if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            } else if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw error("valor inesperado");
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            pos++; // {
            skipSpaces();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipSpaces();
                if (peek() != '"') {
                    throw error("nome de campo esperado");
                }
                String key = string();
                skipSpaces();
                expect(':');
                skipSpaces();
                map.put(key, value());
                skipSpaces();
                char c = next();
                if (c == '}') {
                    return map;
                } else if (c != ',') {
                    throw error("',' ou '}' esperado");
                }
            }
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<Object>();
            pos++; // [
            skipSpaces();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipSpaces();
                list.add(value());
                skipSpaces();
                char c = next();
                if (c == ']') {
                    return list;
                } else if (c != ',') {
                    throw error("',' ou ']' esperado");
                }
            }
        }

        private String string() {
            pos++; // "
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    return sb.toString();
                } else if (c == '\\') {
                    char e = next();
                    switch (e) {
                    case '"':
                    case '\\':
                    case '/':
                        sb.append(e);
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (pos + 4 > s.length()) {
                            throw error("escape \\u incompleto");
                        }
                        try {
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("escape \\u inválido");
                        }
                        pos += 4;
                        break;
                    default:
                        throw error("escape inválido");
                    }
                } else if (c < 0x20) {
                    throw error("caractere de controle dentro de texto");
                } else {
                    sb.append(c);
                }
            }
        }

        private Object number() {
            int start = pos;
            boolean decimal = false;
            if (peek() == '-') {
                pos++;
            }
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c >= '0' && c <= '9') {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    decimal = true;
                    pos++;
                } else {
                    break;
                }
            }
            String n = s.substring(start, pos);
            try {
                if (!decimal) {
                    try {
                        return Long.valueOf(n);
                    } catch (NumberFormatException tooBig) {
                        return Double.valueOf(n);
                    }
                }
                return Double.valueOf(n);
            } catch (NumberFormatException ex) {
                throw error("número inválido: " + n);
            }
        }

        void skipSpaces() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private char peek() {
            if (pos >= s.length()) {
                throw error("fim inesperado");
            }
            return s.charAt(pos);
        }

        private char next() {
            char c = peek();
            pos++;
            return c;
        }

        private void expect(char c) {
            if (next() != c) {
                throw error("'" + c + "' esperado");
            }
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException("JSON inválido na posição " + pos + ": " + message);
        }
    }
}
