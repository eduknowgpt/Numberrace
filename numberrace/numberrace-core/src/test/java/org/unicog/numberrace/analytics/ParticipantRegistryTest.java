package org.unicog.numberrace.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ParticipantRegistryTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void mesmoAlunoRecebeOMesmoCodigo() throws Exception {
        File dir = tmp.newFolder("identification");
        String a = new ParticipantRegistry(dir).codeFor("Silva", "Ana");
        String b = new ParticipantRegistry(dir).codeFor("Silva", "Ana");
        String c = new ParticipantRegistry(dir).codeFor("Souza", "Ana");
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertTrue(a.matches("P[2-9A-Z]{6}"));
    }

    @Test
    public void nomesComVirgulaEAspasSaoPreservados() {
        String[] row = ParticipantRegistry.parse(ParticipantRegistry.quote("P1") + ","
                + ParticipantRegistry.quote("D'Ávila, \"Zé\"") + "," + ParticipantRegistry.quote("Ana"));
        assertEquals("D'Ávila, \"Zé\"", row[1]);
        assertEquals("Ana", row[2]);
    }
}
