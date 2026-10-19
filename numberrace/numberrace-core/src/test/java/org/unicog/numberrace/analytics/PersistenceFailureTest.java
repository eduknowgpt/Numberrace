package org.unicog.numberrace.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Testes de falha da gravação (validacao-registros.md, cenários F1 a F5): o jogo nunca recebe
 * uma exceção, e o que já foi gravado no JSON Lines continua legível.
 */
public class PersistenceFailureTest {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static void session(InteractionLogger logger, int themes) {
        logger.startSession("P017", Json.obj("session_number", 1, "start_level", "easy",
                "start_complexity_level", 1, "language", "pt", "app_version", "teste"));
        for (int i = 0; i < themes; i++) {
            logger.log(EventType.THEME_SELECTED, Json.obj("theme", "jungle"));
        }
        logger.endSession(Json.obj("end_reason", "quit", "screen", "theme", "game_in_progress", false));
    }

    private static File onlyFile(File appDir) {
        File[] files = new File(appDir, "interaction-data/events/P017").listFiles();
        assertEquals(1, files.length);
        return files[0];
    }

    private static List<String> lines(File f) throws IOException {
        return Files.readAllLines(f.toPath(), UTF8);
    }

    private static int sequence(String line) {
        Object parsed = Json.parse(line);
        return ((Number) ((Map<?, ?>) parsed).get("sequence_number")).intValue();
    }

    private static int count(File db) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + db.getPath());
        try {
            ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM event");
            return rs.getInt(1);
        } finally {
            c.close();
        }
    }

    /** F1: o jogo é fechado à força no meio da gravação (como pelo Gerenciador de Tarefas). */
    @Test
    public void f1FechamentoAForcaNaoEstragaOQueFoiGravado() throws Exception {
        File appDir = tmp.newFolder("app");
        File javaBin = new File(System.getProperty("java.home"), "bin");
        File java = new File(javaBin, "java.exe").isFile() ? new File(javaBin, "java.exe") : new File(javaBin, "java");
        Process p = new ProcessBuilder(java.getPath(), "-cp", System.getProperty("java.class.path"),
                KillTestMain.class.getName(), appDir.getPath()).redirectErrorStream(true)
                .redirectOutput(new File(tmp.getRoot(), "saida-do-processo.txt")).start();
        File eventsDir = new File(appDir, "interaction-data/events/P017");
        long limit = System.currentTimeMillis() + 60000;
        try {
            while (true) {
                File[] files = eventsDir.listFiles();
                if (files != null && files.length == 1 && files[0].length() > 30000) { // uns 120 eventos
                    break;
                }
                if (!p.isAlive() || System.currentTimeMillis() > limit) {
                    fail("o processo de teste não gravou eventos: "
                            + new String(Files.readAllBytes(new File(tmp.getRoot(), "saida-do-processo.txt").toPath()), UTF8));
                }
                Thread.sleep(50);
            }
        } finally {
            p.destroyForcibly();
            p.waitFor();
        }
        List<String> lines = lines(onlyFile(appDir));
        assertTrue(lines.size() > 50);
        int complete = 0;
        for (int i = 0; i < lines.size(); i++) {
            try {
                assertEquals(i + 1, sequence(lines.get(i)));
                complete++;
            } catch (IllegalArgumentException e) {
                assertEquals("só a última linha pode estar incompleta", lines.size() - 1, i);
            }
        }
        assertTrue(complete >= lines.size() - 1);
        // o banco também continua legível e não tem mais eventos que o arquivo oficial
        int inDb = count(new File(appDir, "interaction-data/interaction.db"));
        assertTrue("banco: " + inDb + ", arquivo: " + complete, inDb > 0 && inDb <= complete);
    }

    /** F2: o banco SQLite está travado por outro programa (por exemplo, o DB Browser). */
    @Test
    public void f2BancoTravadoNaoPerdeEventosDoJsonLines() throws Exception {
        File appDir = tmp.newFolder("app");
        File db = new File(appDir, "interaction-data/interaction.db");
        db.getParentFile().mkdirs();
        Connection other = DriverManager.getConnection("jdbc:sqlite:" + db.getPath());
        Statement st = other.createStatement();
        st.execute("CREATE TABLE travado (x INTEGER)");
        st.execute("BEGIN EXCLUSIVE"); // trava o banco até o fim do teste
        try {
            InteractionLogger logger = new InteractionLogger();
            logger.start(appDir, true);
            long t0 = System.nanoTime();
            session(logger, 20);
            long callerMs = (System.nanoTime() - t0) / 1000000L;
            logger.shutdown(20000);
            assertTrue("o jogo esperou " + callerMs + " ms", callerMs < 1000);
            assertEquals(22, lines(onlyFile(appDir)).size());
        } finally {
            st.execute("ROLLBACK");
            other.close();
        }
    }

    /** F3: a pasta dos arquivos não pode ser criada (sem permissão, por exemplo). */
    @Test
    public void f3PastaImpossivelDeCriarNaoTravaOJogo() throws Exception {
        File appDir = tmp.newFolder("app");
        File events = new File(appDir, "interaction-data/events");
        events.getParentFile().mkdirs();
        assertTrue(events.createNewFile()); // um arquivo no lugar da pasta impede criar a pasta
        InteractionLogger logger = new InteractionLogger();
        logger.start(appDir, true);
        session(logger, 3);
        logger.shutdown(10000);
        assertTrue(events.isFile());
        // o JSON Lines falhou, mas a cópia no SQLite guardou os eventos
        assertEquals(5, count(new File(appDir, "interaction-data/interaction.db")));
    }

    /** Arquivo que aceita alguns bytes e depois falha, como um disco que enche no meio da gravação. */
    private static final class DiskThatFills extends OutputStream {
        private final OutputStream out;
        private int room;

        DiskThatFills(File f, int room) throws IOException {
            this.out = new FileOutputStream(f, true);
            this.room = room;
        }

        public void write(int b) throws IOException {
            write(new byte[] { (byte) b }, 0, 1);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            int n = Math.min(len, room);
            out.write(b, off, n);
            room -= n;
            if (n < len) {
                throw new IOException("disco cheio (simulado)");
            }
        }

        public void close() throws IOException {
            out.close();
        }
    }

    /** F4: o disco enche no meio de um evento e depois volta a ter espaço. */
    @Test
    public void f4DiscoCheioNoMeioDeUmEvento() throws Exception {
        File appDir = tmp.newFolder("app");
        // gravação normal, só para saber o tamanho de cada linha
        InteractionLogger normal = new InteractionLogger();
        normal.start(appDir, false);
        session(normal, 4);
        normal.shutdown(5000);
        File f = onlyFile(appDir);
        List<String> reference = lines(f);
        assertTrue(f.delete());
        // cabem os 2 primeiros eventos e metade do 3º; depois o espaço é liberado
        final int room = reference.get(0).length() + 1 + reference.get(1).length() + 1 + reference.get(2).length() / 2;
        final int[] opens = { 0 };
        JsonLinesSink sink = new JsonLinesSink(new File(appDir, "interaction-data/events")) {
            OutputStream open(File file) throws IOException {
                return opens[0]++ == 0 ? new DiskThatFills(file, room) : new FileOutputStream(file, true);
            }
        };
        InteractionLogger logger = new InteractionLogger();
        logger.start(appDir, new ArrayList<EventSink>(Arrays.<EventSink> asList(sink)));
        session(logger, 4); // 6 eventos: o 3º é cortado no meio
        logger.shutdown(5000);
        List<String> lines = lines(onlyFile(appDir));
        assertEquals(6, lines.size());
        assertEquals(1, sequence(lines.get(0)));
        assertEquals(2, sequence(lines.get(1)));
        try {
            sequence(lines.get(2));
            fail("a 3ª linha deveria estar incompleta");
        } catch (IllegalArgumentException expected) {
            // o pedaço do evento cortado fica numa linha sozinho
        }
        for (int i = 3; i < 6; i++) {
            assertEquals("o evento depois do corte continua legível", i + 1, sequence(lines.get(i)));
        }
    }

    /** F5: o relógio do computador é acertado (volta 1 hora) no meio da sessão. */
    @Test
    public void f5RelogioQueVoltaNaoAtrapalhaAGravacao() throws Exception {
        File appDir = tmp.newFolder("app");
        final Instant start = Instant.parse("2026-10-11T13:00:00Z");
        final int[] calls = { 0 };
        Clock clock = new Clock() {
            public ZoneId getZone() {
                return ZoneOffset.ofHours(-3);
            }

            public Clock withZone(ZoneId zone) {
                return this;
            }

            public Instant instant() {
                int n = calls[0]++;
                // a partir do 3º evento, o relógio volta 1 hora
                return start.plusMillis(n * 1000L).minusSeconds(n >= 2 ? 3600 : 0);
            }
        };
        InteractionLogger logger = new InteractionLogger();
        logger.start(appDir, false);
        logger.setClock(clock);
        session(logger, 3);
        logger.shutdown(5000);
        List<String> lines = lines(onlyFile(appDir));
        assertEquals(5, lines.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 1, sequence(lines.get(i)));
        }
        assertTrue(lines.get(1).contains("\"timestamp\":\"2026-10-11T10:00:01.000-03:00\""));
        assertTrue(lines.get(2).contains("\"timestamp\":\"2026-10-11T09:00:02.000-03:00\""));
    }
}
