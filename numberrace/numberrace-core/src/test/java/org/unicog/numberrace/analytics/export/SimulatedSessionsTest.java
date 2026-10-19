package org.unicog.numberrace.analytics.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.unicog.numberrace.analytics.EventType;

/**
 * Testes com sessões simuladas (validacao-registros.md): várias sessões e participantes, todos os
 * tipos de evento, e a cadeia completa gerar → importar → verificar → exportar.
 */
public class SimulatedSessionsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static SimulatedSessions.Options escola() {
        SimulatedSessions.Options o = new SimulatedSessions.Options();
        o.seed = 2026;
        o.participants = 3;
        o.sessionsPerParticipant = 2;
        o.gamesPerSession = 4;
        o.initialRewards = 6; // aluno "preparado": falta uma recompensa para liberar um personagem
        return o;
    }

    private static String describe(List<IntegrityChecker.Finding> findings) {
        StringBuilder sb = new StringBuilder();
        for (IntegrityChecker.Finding f : findings) {
            sb.append(f.rule.id).append(' ').append(f.sessionId).append(' ').append(f.sequenceNumber).append(' ')
                    .append(f.message).append('\n');
        }
        return sb.toString();
    }

    /**
     * Grava os dados simulados numa pasta, para conferência por outras ferramentas (por exemplo, o
     * JSON Schema em Python). Só roda com {@code -Dsimulados.saida=<pasta>}.
     */
    @Test
    public void gravaDadosSimuladosParaConferenciaExterna() throws Exception {
        String dir = System.getProperty("simulados.saida");
        Assume.assumeTrue(dir != null);
        SimulatedSessions.Options o = escola();
        SimulatedSessions.writeComputer(new File(dir, "escola"), SimulatedSessions.generate(o));
        o.lastSessionCrashes = true;
        SimulatedSessions.writeComputer(new File(dir, "escola-com-sessoes-quebradas"), SimulatedSessions.generate(o));
    }

    @Test
    public void mesmaSementeGeraOsMesmosEventos() {
        assertEquals(SimulatedSessions.generate(escola()).get(4).lines,
                SimulatedSessions.generate(escola()).get(4).lines);
    }

    @Test
    public void sessoesSimuladasSaoValidasECoerentes() throws Exception {
        List<SimulatedSessions.Session> generated = SimulatedSessions.generate(escola());
        File dir = SimulatedSessions.writeComputer(tmp.newFolder(), generated);
        EventImporter importer = new EventImporter();
        List<SessionData> sessions = importer.importFrom(Collections.singletonList(dir));
        assertEquals(importer.getReport().toText(), 0, importer.getReport().problemCount());
        assertEquals(6, sessions.size());
        List<IntegrityChecker.Finding> findings = IntegrityChecker.check(sessions, importer.getSessionsPerFile());
        assertEquals(describe(findings), 0, findings.size());

        // todos os tipos de evento do modelo aparecem nos dados simulados
        Set<EventType> seen = EnumSet.noneOf(EventType.class);
        for (SessionData s : sessions) {
            for (SourceEvent e : s.getEvents()) {
                seen.add(EventType.valueOf(e.getEventType()));
            }
        }
        assertEquals(EnumSet.allOf(EventType.class), seen);
    }

    @Test
    public void exportaVariasSessoesEParticipantes() throws Exception {
        File dir = SimulatedSessions.writeComputer(tmp.newFolder(), SimulatedSessions.generate(escola()));
        File out = tmp.newFolder();
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        int code = ExportTool.run(new String[] { "--entrada", dir.getPath(), "--saida", out.getPath() },
                new PrintStream(console, true, "UTF-8"));
        assertEquals(console.toString("UTF-8"), ExportTool.OK, code);
        assertTrue(console.toString("UTF-8").contains("0 erro(s) e 0 aviso(s)"));
        File pkg = out.listFiles()[0];
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + new File(pkg, "interaction.db").getPath());
        try {
            ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(DISTINCT participant_id), COUNT(*), "
                    + "SUM(session_number = 2), SUM(characters_unlocked) FROM session");
            assertEquals(3, rs.getInt(1));
            assertEquals(6, rs.getInt(2));
            assertEquals(3, rs.getInt(3));
            assertTrue(rs.getInt(4) >= 1);
            rs = c.createStatement().executeQuery("SELECT (SELECT COUNT(*) FROM turn), "
                    + "(SELECT COUNT(*) FROM event WHERE event_type = 'TURN_START'), (SELECT COUNT(*) FROM integrity)");
            assertEquals(rs.getInt(2), rs.getInt(1));
            assertEquals(0, rs.getInt(3));
        } finally {
            c.close();
        }
    }

    @Test
    public void jogoFechadoAForcaGeraSoOAvisoDeSessaoSemFim() throws Exception {
        SimulatedSessions.Options o = escola();
        o.lastSessionCrashes = true;
        File dir = SimulatedSessions.writeComputer(tmp.newFolder(), SimulatedSessions.generate(o));
        EventImporter importer = new EventImporter();
        List<SessionData> sessions = importer.importFrom(Collections.singletonList(dir));
        List<IntegrityChecker.Finding> findings = IntegrityChecker.check(sessions, importer.getSessionsPerFile());
        assertEquals(describe(findings), 3, findings.size()); // uma por participante
        for (IntegrityChecker.Finding f : findings) {
            assertEquals("R04", f.rule.id);
        }
    }
}
