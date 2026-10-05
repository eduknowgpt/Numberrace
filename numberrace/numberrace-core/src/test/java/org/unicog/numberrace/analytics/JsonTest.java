package org.unicog.numberrace.analytics;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

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
}
