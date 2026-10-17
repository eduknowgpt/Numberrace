package org.unicog.numberrace.analytics.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Regressão com os dados do jogo real (Etapas 5 e 7): exportar essas sessões tem que dar sempre os
 * mesmos números e a mesma conclusão da verificação de integridade. Se uma mudança no código
 * alterar algum desses resultados, o teste mostra onde.
 */
public class RealDataRegressionTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final ByteArrayOutputStream console = new ByteArrayOutputStream();

    @Test
    public void sessoesDoWindowsSaoExportadasSemErroNemAviso() throws Exception {
        File pkg = export(RealData.computer(tmp.newFolder(), RealData.WINDOWS));
        Connection c = db(pkg);
        try {
            assertEquals(197, number(c, "SELECT COUNT(*) FROM event"));
            assertEquals(2, number(c, "SELECT COUNT(*) FROM session"));
            assertEquals(12, number(c, "SELECT COUNT(*) FROM turn"));
            assertEquals(0, number(c, "SELECT COUNT(*) FROM integrity"));
            assertEquals(2, number(c, "SELECT COUNT(*) FROM event WHERE event_type = 'HAZARD'"));
            assertEquals(1, number(c, "SELECT COUNT(*) FROM turn WHERE response = 'timeout'"));
            assertEquals(1, number(c, "SELECT COUNT(*) FROM event WHERE event_type = 'CLICK_IGNORED'"));
            assertEquals(7, number(c, "SELECT COUNT(*) FROM event WHERE event_type = 'PAUSE' "
                    + "AND json_extract(event_json, '$.data.trigger') = 'window_focus'"));
            // F8 e depois troca de janela: a retomada vem da recuperação do foco (problema P4)
            assertEquals(1, number(c, "SELECT COUNT(*) FROM event WHERE event_type = 'PAUSE' "
                    + "AND json_extract(event_json, '$.data.trigger') = 'pause_key'"));
            assertEquals("[quit, window_closed]", texts(c, "SELECT end_reason FROM session ORDER BY end_reason"));
            assertEquals("[1, 2]", texts(c, "SELECT session_number FROM session ORDER BY session_number"));
        } finally {
            c.close();
        }
    }

    @Test
    public void sessoesDoRoboTemSoOAvisoDaSessaoEncerradaAForca() throws Exception {
        File pkg = export(RealData.computer(tmp.newFolder(), RealData.ROBOT));
        Connection c = db(pkg);
        try {
            assertEquals(1345, number(c, "SELECT COUNT(*) FROM event"));
            assertEquals(4, number(c, "SELECT COUNT(*) FROM session"));
            assertEquals(87, number(c, "SELECT COUNT(*) FROM turn"));
            assertEquals("[R04 warning " + RealData.KILLED_SESSION + "]",
                    texts(c, "SELECT rule || ' ' || severity || ' ' || session_id FROM integrity"));
            assertEquals(14, number(c, "SELECT COUNT(*) FROM event WHERE event_type = 'HAZARD'"));
            assertEquals(6, number(c, "SELECT COUNT(*) FROM turn WHERE response = 'timeout'"));
            assertEquals(1, number(c, "SELECT COUNT(*) FROM event WHERE event_type = 'CHARACTER_UNLOCKED'"));
            // recompensas de 1 a 7, como no modelo (problema P2)
            assertEquals("[1, 2, 3, 4, 5, 6, 7]", texts(c, "SELECT DISTINCT json_extract(event_json, "
                    + "'$.data.reward_type') AS r FROM event WHERE event_type = 'REWARD_SAVED' ORDER BY r"));
            assertEquals("[change_student, quit, window_closed]",
                    texts(c, "SELECT end_reason FROM session WHERE end_reason IS NOT NULL ORDER BY end_reason"));
        } finally {
            c.close();
        }
    }

    @Test
    public void tresComputadoresComAsDuasVersoesDoModelo() throws Exception {
        File etapa5 = Fixture.computer(tmp.newFolder(), Fixture.lines(), true);
        File windows = RealData.computer(tmp.newFolder(), RealData.WINDOWS);
        File robot = RealData.computer(tmp.newFolder(), RealData.ROBOT);
        File pkg = export(etapa5, windows, robot);
        Connection c = db(pkg);
        try {
            assertEquals(180 + 197 + 1345, number(c, "SELECT COUNT(*) FROM event"));
            assertEquals(7, number(c, "SELECT COUNT(*) FROM session"));
            assertEquals(11 + 12 + 87, number(c, "SELECT COUNT(*) FROM turn"));
            assertEquals("[1.0, 1.1]", texts(c, "SELECT DISTINCT schema_version FROM event ORDER BY 1"));
            assertEquals(1, number(c, "SELECT COUNT(*) FROM integrity"));
            assertEquals(5, number(c, "SELECT COUNT(DISTINCT participant_id) FROM session"));
        } finally {
            c.close();
        }
    }

    @Test
    public void aMesmaEntradaGeraSempreOsMesmosDados() throws Exception {
        File windows = RealData.computer(tmp.newFolder(), RealData.WINDOWS);
        File robot = RealData.computer(tmp.newFolder(), RealData.ROBOT);
        File first = export(windows, robot);
        File second = export(robot, windows); // ordem das entradas trocada
        for (String name : Arrays.asList("events.jsonl", "csv/events.csv", "csv/sessions.csv", "csv/turns.csv",
                "csv/integrity.csv", "csv/dicionario-dados.csv", "excel/events.csv", "excel/sessions.csv",
                "excel/turns.csv", "excel/integrity.csv", "excel/dicionario-dados.csv")) {
            assertTrue(name, Arrays.equals(EventImporter.readAll(new File(first, name)),
                    EventImporter.readAll(new File(second, name))));
        }
    }

    // ------------------------------------------------------------------ auxiliares

    private File export(File... inputs) throws Exception {
        File out = tmp.newFolder();
        List<String> args = new ArrayList<String>();
        for (File in : inputs) {
            args.add("--entrada");
            args.add(in.getPath());
        }
        args.add("--saida");
        args.add(out.getPath());
        int code = ExportTool.run(args.toArray(new String[args.size()]), new PrintStream(console, true, "UTF-8"));
        assertEquals(console.toString("UTF-8"), ExportTool.OK, code);
        File[] dirs = out.listFiles();
        assertEquals(1, dirs.length);
        assertTrue(PackageVerifier.verify(dirs[0]).isEmpty());
        return dirs[0];
    }

    private static Connection db(File pkg) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + new File(pkg, "interaction.db").getPath());
    }

    private static int number(Connection c, String sql) throws SQLException {
        ResultSet rs = c.createStatement().executeQuery(sql);
        try {
            return rs.getInt(1);
        } finally {
            rs.close();
        }
    }

    private static String texts(Connection c, String sql) throws SQLException {
        ResultSet rs = c.createStatement().executeQuery(sql);
        try {
            List<String> out = new ArrayList<String>();
            while (rs.next()) {
                out.add(rs.getString(1));
            }
            return out.toString();
        } finally {
            rs.close();
        }
    }
}
