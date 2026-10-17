package org.unicog.numberrace.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * O registro sob carga: várias partes do jogo registrando ao mesmo tempo, um destino lento e um
 * destino com defeito. O jogo nunca espera a gravação, e a ordem e a numeração continuam certas.
 */
public class InteractionLoggerConcurrencyTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final Map<String, Object> START = Json.obj("session_number", 1, "start_level", "easy",
            "start_complexity_level", 1, "language", "pt", "app_version", "3.1.0");

    private static Map<String, Object> screen(int i) {
        return Json.obj("from_screen", "choice", "to_screen", "theme" + i);
    }

    @Test
    public void variasLinhasDeExecucaoAoMesmoTempoMantemASequencia() throws Exception {
        File appDir = tmp.newFolder("app");
        final InteractionLogger logger = new InteractionLogger();
        logger.start(appDir, false);
        logger.startSession("P017", START);
        final int threads = 8;
        final int each = 250;
        final CountDownLatch go = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<Thread>();
        for (int t = 0; t < threads; t++) {
            Thread w = new Thread(new Runnable() {
                public void run() {
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    for (int i = 0; i < each; i++) {
                        logger.log(EventType.SCREEN_CHANGED, screen(i));
                    }
                }
            });
            w.start();
            workers.add(w);
        }
        go.countDown();
        for (Thread w : workers) {
            w.join();
        }
        logger.endSession(Json.obj("end_reason", "quit", "screen", "choice", "game_in_progress", false));
        logger.shutdown(10000);

        File[] files = new File(appDir, "interaction-data/events/P017").listFiles();
        List<String> lines = Files.readAllLines(files[0].toPath(), Charset.forName("UTF-8"));
        assertEquals(threads * each + 2, lines.size());
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < lines.size(); i++) {
            @SuppressWarnings("unchecked")
            Map<String, Object> e = (Map<String, Object>) Json.parse(lines.get(i));
            assertEquals("a ordem no arquivo é a da sequência", Long.valueOf(i + 1), e.get("sequence_number"));
            ids.add((String) e.get("event_id"));
        }
        assertEquals(lines.size(), ids.size());
    }

    @Test
    public void oJogoNaoEsperaUmDestinoLento() throws Exception {
        Recorder slow = new Recorder(20, false); // 20 ms por evento
        InteractionLogger logger = new InteractionLogger();
        logger.start(tmp.newFolder("app"), Arrays.<EventSink> asList(slow));
        long t0 = System.nanoTime();
        logger.startSession("P017", START);
        for (int i = 0; i < 100; i++) {
            logger.log(EventType.SCREEN_CHANGED, screen(i));
        }
        long elapsedMs = (System.nanoTime() - t0) / 1000000;
        // gravando de forma síncrona seriam pelo menos 2 s; o jogo só põe na fila
        assertTrue("o jogo esperou " + elapsedMs + " ms", elapsedMs < 1000);
        logger.shutdown(30000);
        assertEquals(101, slow.sequences.size());
        for (int i = 0; i < slow.sequences.size(); i++) {
            assertEquals(Integer.valueOf(i + 1), slow.sequences.get(i));
        }
    }

    @Test
    public void umDestinoComDefeitoNaoImpedeOsOutros() throws Exception {
        Recorder broken = new Recorder(0, true);
        Recorder good = new Recorder(0, false);
        InteractionLogger logger = new InteractionLogger();
        logger.start(tmp.newFolder("app"), Arrays.<EventSink> asList(broken, good));
        logger.startSession("P017", START);
        for (int i = 0; i < 10; i++) {
            logger.log(EventType.SCREEN_CHANGED, screen(i));
        }
        logger.endSession(Json.obj("end_reason", "quit", "screen", "choice", "game_in_progress", false));
        logger.shutdown(10000);
        assertEquals(12, good.sequences.size());
        assertEquals(1, good.endedSessions.size());
    }

    @Test
    public void eventosDepoisDeDesligarSaoIgnoradosSemErro() throws Exception {
        Recorder r = new Recorder(0, false);
        InteractionLogger logger = new InteractionLogger();
        logger.start(tmp.newFolder("app"), Arrays.<EventSink> asList(r));
        logger.startSession("P017", START);
        logger.shutdown(10000);
        logger.log(EventType.SCREEN_CHANGED, screen(1));
        logger.endSession(Json.obj("end_reason", "quit", "screen", "choice", "game_in_progress", false));
        logger.shutdown(10000);
        assertEquals(1, r.sequences.size());
        assertTrue(r.closed);
    }

    /** Destino de teste: guarda a sequência recebida; pode ser lento ou falhar sempre. */
    private static final class Recorder implements EventSink {
        final List<Integer> sequences = Collections.synchronizedList(new ArrayList<Integer>());
        final List<String> endedSessions = Collections.synchronizedList(new ArrayList<String>());
        final long delayMs;
        final boolean fail;
        volatile boolean closed;

        Recorder(long delayMs, boolean fail) {
            this.delayMs = delayMs;
            this.fail = fail;
        }

        public void write(InteractionEvent e) {
            if (fail) {
                throw new IllegalStateException("destino com defeito");
            }
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            sequences.add(Integer.valueOf(e.getSequenceNumber()));
        }

        public void sessionEnded(String sessionId) {
            if (fail) {
                throw new IllegalStateException("destino com defeito");
            }
            endedSessions.add(sessionId);
        }

        public void close() {
            closed = true;
        }
    }
}
