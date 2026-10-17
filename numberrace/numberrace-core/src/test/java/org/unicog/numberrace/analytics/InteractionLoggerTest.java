package org.unicog.numberrace.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class InteractionLoggerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static void sessaoDeExemplo(InteractionLogger logger) {
        logger.startSession("P017", Json.obj("session_number", 1, "start_level", "easy",
                "start_complexity_level", 1, "language", "pt", "app_version", "3.1.0"));
        logger.beginGame(1, "number_comparison");
        logger.log(EventType.GAME_START, Json.obj("board_length", 12, "complexity_level", 1));
        logger.beginTurn(1);
        logger.log(EventType.RESPONSE_ENABLED, Json.obj());
        logger.log(EventType.CLICK_IGNORED, Json.obj("side", "left"));
        logger.endSession(Json.obj("end_reason", "quit", "screen", "choice", "game_in_progress", true));
    }

    private static List<String> linhas(File appDir) throws Exception {
        File participantDir = new File(appDir, "interaction-data/events/P017");
        File[] files = participantDir.listFiles();
        assertEquals(1, files.length);
        assertTrue(files[0].getName().matches("\\d{4}-\\d{2}-\\d{2}_\\d{6}_[0-9a-f-]{36}\\.jsonl"));
        return Files.readAllLines(files[0].toPath(), Charset.forName("UTF-8"));
    }

    @Test
    public void gravaJsonLinesESqliteNaOrdem() throws Exception {
        File appDir = tmp.newFolder("app");
        InteractionLogger logger = new InteractionLogger();
        logger.start(appDir, true);
        sessaoDeExemplo(logger);
        logger.shutdown(5000);

        List<String> lines = linhas(appDir);
        assertEquals(5, lines.size());
        for (int i = 0; i < lines.size(); i++) {
            assertTrue(lines.get(i).contains("\"sequence_number\":" + (i + 1) + ","));
        }
        assertTrue(lines.get(0).contains("\"event_type\":\"SESSION_START\""));
        assertFalse(lines.get(0).contains("game_number"));
        assertTrue(lines.get(3).contains("\"game_number\":1,\"turn_number\":1"));

        Connection c = DriverManager.getConnection("jdbc:sqlite:" + new File(appDir, "interaction-data/interaction.db"));
        try {
            ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*), MAX(sequence_number) FROM event");
            assertEquals(5, rs.getInt(1));
            assertEquals(5, rs.getInt(2));
        } finally {
            c.close();
        }
    }

    @Test
    public void eventoInvalidoEDescartadoSemBuracoNaSequencia() throws Exception {
        File appDir = tmp.newFolder("app");
        InteractionLogger logger = new InteractionLogger();
        logger.start(appDir, false);
        logger.startSession("P017", Json.obj("session_number", 1, "start_level", "easy",
                "start_complexity_level", 1, "language", "pt", "app_version", "3.1.0"));
        logger.log(EventType.ANSWER, Json.obj("side", "left")); // sem partida/rodada e campos faltando
        logger.log(EventType.THEME_SELECTED, Json.obj("theme", "jungle"));
        logger.shutdown(5000);

        List<String> lines = linhas(appDir);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("\"sequence_number\":2,"));
    }

    @Test
    public void falhaDoSqliteNaoPerdeEventosDoJsonLines() throws Exception {
        File appDir = tmp.newFolder("app");
        // um diretório no lugar do arquivo do banco impede o SQLite de abrir
        assertTrue(new File(appDir, "interaction-data/interaction.db").mkdirs());
        InteractionLogger logger = new InteractionLogger();
        logger.start(appDir, true);
        sessaoDeExemplo(logger);
        logger.shutdown(5000);
        assertEquals(5, linhas(appDir).size());
    }

    @Test
    public void semSessaoNadaEGravado() throws Exception {
        File appDir = tmp.newFolder("app");
        InteractionLogger logger = new InteractionLogger();
        logger.start(appDir, false);
        logger.log(EventType.THEME_SELECTED, Json.obj("theme", "jungle"));
        logger.shutdown(5000);
        assertFalse(new File(appDir, "interaction-data/events").exists());
    }
}
