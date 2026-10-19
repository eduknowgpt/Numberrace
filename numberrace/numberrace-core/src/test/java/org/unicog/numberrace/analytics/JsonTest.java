package org.unicog.numberrace.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class JsonTest {

    @Test
    public void escreveObjetoNaOrdemDasChaves() {
        assertEquals("{\"b\":1,\"a\":true,\"c\":null}", Json.write(Json.obj("b", 1, "a", true, "c", null)));
    }

    @Test
    public void escapaCaracteresEspeciais() {
        assertEquals("\"a\\\"b\\\\c\\nd\\u0001\"", Json.write("a\"b\\c\nd\u0001"));
    }

    @Test
    public void escreveNumerosEListas() {
        assertEquals("[0.75,8.0,3,null]", Json.write(Arrays.asList(0.75, 8.0, 3, Double.NaN)));
    }

    @Test
    public void leDeVoltaOQueEscreve() {
        String json = "{\"a\":\"x\\\"y\\u00e9\",\"n\":-12,\"d\":0.25,\"l\":[true,false,null],\"o\":{}}";
        Object parsed = Json.parse(json);
        Map<?, ?> m = (Map<?, ?>) parsed;
        assertEquals("x\"y\u00e9", m.get("a"));
        assertEquals(Long.valueOf(-12), m.get("n"));
        assertEquals(Double.valueOf(0.25), m.get("d"));
        assertEquals(Arrays.asList(true, false, null), (List<?>) m.get("l"));
        assertEquals("{\"a\":\"x\\\"y\u00e9\",\"n\":-12,\"d\":0.25,\"l\":[true,false,null],\"o\":{}}",
                Json.write(parsed));
    }

    @Test
    public void recusaJsonInvalido() {
        for (String bad : new String[] { "{\"a\":1", "{\"a\" 1}", "[1,]", "{\"a\":1}x", "\"sem fim" }) {
            try {
                Json.parse(bad);
                fail("deveria recusar: " + bad);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }
}
