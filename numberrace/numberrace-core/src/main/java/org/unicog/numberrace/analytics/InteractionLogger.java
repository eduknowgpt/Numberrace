package org.unicog.numberrace.analytics;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Componente de registro dos eventos de interação (evolution/pt/persistencia-eventos.md).
 *
 * <p>O jogo chama {@link #log} e continua sem esperar: o evento recebe a parte comum (sessão,
 * participante, sequência, partida e rodada), é validado e entra numa fila. Uma única linha de
 * execução em segundo plano grava cada evento primeiro no JSON Lines (registro oficial) e depois
 * no SQLite (cópia para consulta). Nenhuma exceção do registro chega ao jogo.
 */
public final class InteractionLogger {

    public static final String DATA_DIR = "interaction-data";
    public static final String IDENTIFICATION_DIR = "identification";

    static final Logger LOG = Logger.getLogger("org.unicog.numberrace.analytics");
    private static final InteractionLogger INSTANCE = new InteractionLogger();
    private static final Object STOP = new Object();

    private final Object lock = new Object();
    private BlockingQueue<Object> queue;
    private Thread writer;
    private List<EventSink> sinks;
    private ParticipantRegistry registry;
    private volatile boolean enabled;

    private String sessionId;
    private String participantId;
    private int sequence;
    private String activity;
    private Integer gameNumber;
    private Integer turnNumber;

    /** Uso normal: {@link #get()}. O construtor fica visível no pacote para os testes. */
    InteractionLogger() {
    }

    public static InteractionLogger get() {
        return INSTANCE;
    }

    /**
     * Liga o registro, com os dados em {@code <appDir>/interaction-data} e a identificação em
     * {@code <appDir>/identification}.
     */
    public void start(File appDir) {
        start(appDir, true);
    }

    void start(File appDir, boolean useSqlite) {
        synchronized (lock) {
            if (enabled) {
                return;
            }
            try {
                File dataDir = new File(appDir, DATA_DIR);
                sinks = new ArrayList<EventSink>();
                sinks.add(new JsonLinesSink(new File(dataDir, "events")));
                if (useSqlite) {
                    sinks.add(new SqliteSink(new File(dataDir, "interaction.db"), new File(dataDir, "native")));
                }
                registry = new ParticipantRegistry(new File(appDir, IDENTIFICATION_DIR));
                queue = new LinkedBlockingQueue<Object>();
                final BlockingQueue<Object> q = queue;
                final List<EventSink> s = sinks;
                writer = new Thread(new Runnable() {
                    public void run() {
                        writeLoop(q, s);
                    }
                }, "interaction-logger");
                writer.setDaemon(true);
                writer.start();
                enabled = true;
                LOG.info("Registro de interação ligado em " + dataDir.getAbsolutePath());
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Registro de interação desligado: falha ao iniciar", t);
                enabled = false;
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean inSession() {
        synchronized (lock) {
            return enabled && sessionId != null;
        }
    }

    public boolean inGame() {
        synchronized (lock) {
            return gameNumber != null;
        }
    }

    public boolean inTurn() {
        synchronized (lock) {
            return gameNumber != null && turnNumber != null;
        }
    }

    /** Código pseudonimizado do participante (criado na primeira vez). */
    public String participantCodeFor(String lastName, String firstName) {
        ParticipantRegistry r = registry;
        return r == null ? null : r.codeFor(lastName, firstName);
    }

    /** Abre uma sessão e registra {@code SESSION_START}. */
    public void startSession(String participant, Map<String, Object> data) {
        synchronized (lock) {
            if (!enabled || participant == null) {
                return;
            }
            sessionId = UUID.randomUUID().toString();
            participantId = participant;
            sequence = 0;
            activity = null;
            gameNumber = null;
            turnNumber = null;
            enqueue(EventType.SESSION_START, data);
        }
    }

    /** Registra {@code SESSION_END} e fecha a sessão. */
    public void endSession(Map<String, Object> data) {
        synchronized (lock) {
            if (!enabled || sessionId == null) {
                return;
            }
            enqueue(EventType.SESSION_END, data);
            sessionId = null;
            participantId = null;
            gameNumber = null;
            turnNumber = null;
            activity = null;
        }
    }

    /** Início de uma partida: os eventos seguintes levam {@code activity} e {@code game_number}. */
    public void beginGame(int number, String activityName) {
        synchronized (lock) {
            gameNumber = Integer.valueOf(number);
            activity = activityName;
            turnNumber = null;
        }
    }

    public void endGame() {
        synchronized (lock) {
            gameNumber = null;
            turnNumber = null;
            activity = null;
        }
    }

    /** Início de uma rodada: os eventos seguintes levam {@code turn_number}. */
    public void beginTurn(int number) {
        synchronized (lock) {
            if (gameNumber != null) {
                turnNumber = Integer.valueOf(number);
            }
        }
    }

    /** Registra um evento na sessão atual. Sem sessão aberta, o evento é ignorado. */
    public void log(EventType type, Map<String, Object> data) {
        synchronized (lock) {
            if (!enabled || sessionId == null) {
                return;
            }
            enqueue(type, data);
        }
    }

    private void enqueue(EventType type, Map<String, Object> data) {
        try {
            boolean withGame = type.getScope() != EventType.Scope.SESSION || gameNumber != null;
            InteractionEvent e = new InteractionEvent(UUID.randomUUID().toString(), type, OffsetDateTime.now(),
                    sessionId, participantId, sequence + 1, withGame ? activity : null,
                    withGame ? gameNumber : null, withGame ? turnNumber : null, data);
            List<String> problems = EventValidator.validate(e);
            if (!problems.isEmpty()) {
                LOG.warning("Evento descartado por ser inválido " + problems + ": " + e.toJson());
                return;
            }
            sequence++;
            queue.offer(e);
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Falha ao montar o evento " + type, t);
        }
    }

    /**
     * Desliga o registro: grava o que está na fila (no máximo {@code timeoutMs}) e fecha os
     * arquivos.
     */
    public void shutdown(long timeoutMs) {
        Thread w;
        synchronized (lock) {
            if (!enabled) {
                return;
            }
            enabled = false;
            queue.offer(STOP);
            w = writer;
        }
        try {
            w.join(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void writeLoop(BlockingQueue<Object> q, List<EventSink> sinks) {
        try {
            while (true) {
                Object item = q.take();
                if (item == STOP) {
                    break;
                }
                InteractionEvent e = (InteractionEvent) item;
                for (EventSink sink : sinks) {
                    sink.write(e);
                }
                if (e.getEventType() == EventType.SESSION_END) {
                    for (EventSink sink : sinks) {
                        sink.sessionEnded(e.getSessionId());
                    }
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Erro inesperado no gravador de eventos", t);
        } finally {
            for (EventSink sink : sinks) {
                sink.close();
            }
        }
    }
}
