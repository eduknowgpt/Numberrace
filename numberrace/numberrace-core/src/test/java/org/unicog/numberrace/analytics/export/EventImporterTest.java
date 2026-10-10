package org.unicog.numberrace.analytics.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class EventImporterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void importaSessaoCompletaSemProblemas() throws Exception {
        EventImporter importer = new EventImporter();
        List<SessionData> sessions = importer.importFrom(Arrays.asList(Fixture.computer(tmp.newFolder(), Fixture.lines(), true)));
        assertEquals(1, sessions.size());
        assertEquals(180, sessions.get(0).getEvents().size());
        assertEquals(Fixture.PARTICIPANT, sessions.get(0).getParticipantId());
        assertEquals(0, importer.getReport().problemCount());
        assertTrue(sessions.get(0).hasSessionStart() && sessions.get(0).hasSessionEnd());
    }

    @Test
    public void juntaComputadoresSemRepetirEventos() throws Exception {
        File pc1 = Fixture.computer(tmp.newFolder(), Fixture.lines(), true);
        File pc2 = Fixture.computer(tmp.newFolder(), Fixture.lines(), true);
        EventImporter importer = new EventImporter();
        List<SessionData> sessions = importer.importFrom(Arrays.asList(pc1, pc2));
        assertEquals(180, sessions.get(0).getEvents().size());
        assertEquals(180, importer.getReport().duplicates);
        assertEquals(0, importer.getReport().problemCount());
    }

    @Test
    public void relataLinhaCortadaNoFimESessaoSemFim() throws Exception {
        List<String> lines = Fixture.lines();
        String last = lines.remove(lines.size() - 1);
        lines.add(last.substring(0, last.length() / 2)); // gravação interrompida
        EventImporter importer = new EventImporter();
        List<SessionData> sessions = importer.importFrom(Arrays.asList(Fixture.computer(tmp.newFolder(), lines, false)));
        ImportReport r = importer.getReport();
        assertEquals(179, sessions.get(0).getEvents().size());
        assertEquals(1, r.incompleteLines.size());
        assertTrue(r.incompleteLines.get(0).contains("linha 180") && r.incompleteLines.get(0).contains("interrompida"));
        assertTrue(r.sessionWarnings.toString().contains("sem SESSION_END"));
    }

    @Test
    public void relataEventoPerdidoEEventoInvalido() throws Exception {
        List<String> lines = Fixture.lines();
        lines.remove(49); // sequence_number 50 perdido
        lines.set(9, lines.get(9).replace("\"complexity_level\"", "\"nivel\"")); // TURN_START inválido (seq 10)
        EventImporter importer = new EventImporter();
        List<SessionData> sessions = importer.importFrom(Arrays.asList(Fixture.computer(tmp.newFolder(), lines, true)));
        ImportReport r = importer.getReport();
        assertEquals(178, sessions.get(0).getEvents().size());
        assertEquals(Arrays.asList(10, 50), sessions.get(0).getMissingSequenceNumbers());
        assertEquals(1, r.invalidEvents.size());
        assertTrue(r.invalidEvents.get(0).contains("linha 10 (TURN_START)"));
        assertTrue(r.invalidEvents.get(0).contains("campo obrigatório ausente em data: complexity_level"));
        assertTrue(r.sessionWarnings.toString().contains("faltam os eventos de sequência [10, 50]"));
    }

    @Test
    public void relataConflitoDeSequencia() throws Exception {
        List<String> lines = Fixture.lines();
        String copy = lines.get(20).replaceFirst("\"event_id\":\"[0-9a-f-]{36}\"",
                "\"event_id\":\"ffffffff-ffff-4fff-8fff-ffffffffffff\"");
        lines.add(copy); // outro event_id com o mesmo sequence_number
        EventImporter importer = new EventImporter();
        List<SessionData> sessions = importer.importFrom(Arrays.asList(Fixture.computer(tmp.newFolder(), lines, true)));
        assertEquals(180, sessions.get(0).getEvents().size());
        assertEquals(1, importer.getReport().conflicts.size());
        assertTrue(importer.getReport().conflicts.get(0).contains("sequence_number 21"));
    }

    @Test
    public void nuncaLeAPastaDeIdentificacao() throws Exception {
        File root = tmp.newFolder();
        Fixture.computer(root, Fixture.lines(), true);
        File id = new File(root, "identification");
        id.mkdirs();
        FileOutputStream out = new FileOutputStream(new File(id, "nomes.jsonl"));
        out.write("{\"nome\":\"Maria\"}\n".getBytes(Fixture.UTF8));
        out.close();
        EventImporter importer = new EventImporter();
        importer.importFrom(Arrays.asList(root));
        assertEquals(1, importer.getReport().filesRead);
        assertFalse(importer.getReport().toText().contains("nomes"));
    }
}
