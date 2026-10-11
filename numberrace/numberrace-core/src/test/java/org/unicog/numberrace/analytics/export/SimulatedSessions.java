package org.unicog.numberrace.analytics.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;

import org.unicog.numberrace.analytics.Json;

/**
 * Gerador de sessões simuladas (validacao-registros.md): produz eventos do modelo 1.1 como o jogo
 * produziria, seguindo as regras do tabuleiro (avanço pelo número escolhido, esbarrão que faz o
 * outro voltar uma casa, armadilhas que permanecem no tabuleiro, prazo esgotado que dá à criança
 * o menor número, recompensa a cada vitória e novo personagem a cada 7 recompensas). Com a mesma
 * semente, gera sempre os mesmos eventos. Usado nos testes; nenhum dado vem de crianças.
 */
final class SimulatedSessions {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final ZoneOffset BRT = ZoneOffset.ofHours(-3);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .withZone(BRT);
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss").withZone(BRT);
    private static final String[] SIDES = { "left", "right" };
    private static final String[] PIECES = { "player", "opponent" };
    private static final String[] THEMES = { "jungle", "under_the_sea" };

    /** Configuração da simulação. */
    static final class Options {
        long seed = 1;
        int participants = 1;
        int sessionsPerParticipant = 1;
        int gamesPerSession = 2;
        int boardLength = 12;
        double correctRate = 0.7;
        double timeoutRate = 0.2;
        double pauseRate = 0.15;
        double focusPauseRate = 0.1;
        double clickIgnoredRate = 0.1;
        double newHazardRate = 0.5;
        /** Recompensas que cada participante já tem no tema da selva antes da primeira sessão. */
        int initialRewards;
        /** A sessão termina no meio da última partida (a criança sai pelo menu). */
        boolean quitMidGame = true;
        /** A última sessão de cada participante não tem SESSION_END (jogo fechado à força). */
        boolean lastSessionCrashes;
        String schemaVersion = "1.1";
    }

    /** Uma sessão gerada. */
    static final class Session {
        String participantId;
        String sessionId;
        Instant start;
        final List<String> lines = new ArrayList<String>();

        String fileName() {
            return FILE_TIME.format(start) + "_" + sessionId + ".jsonl";
        }
    }

    private final Options o;
    private final Random rnd;

    private SimulatedSessions(Options options) {
        this.o = options;
        this.rnd = new Random(options.seed);
    }

    static List<Session> generate(Options options) {
        return new SimulatedSessions(options).run();
    }

    /** Grava as sessões como a pasta {@code interaction-data} de um computador e devolve a pasta. */
    static File writeComputer(File root, List<Session> sessions) throws IOException {
        File data = new File(root, "interaction-data");
        for (Session s : sessions) {
            File dir = new File(data, "events/" + s.participantId);
            dir.mkdirs();
            OutputStream out = new FileOutputStream(new File(dir, s.fileName()));
            try {
                for (String line : s.lines) {
                    out.write((line + "\n").getBytes(UTF8));
                }
            } finally {
                out.close();
            }
        }
        return data;
    }

    private List<Session> run() {
        List<Session> all = new ArrayList<Session>();
        Instant day = Instant.parse("2026-10-01T11:00:00Z");
        for (int p = 0; p < o.participants; p++) {
            String participant = "PSIM" + (char) ('A' + p) + (char) ('A' + rnd.nextInt(26));
            int[][] rewards = new int[2][7];
            for (int i = 0; i < o.initialRewards && i < 7; i++) {
                rewards[0][i] = 1;
            }
            int[] unlocked = { 1, 1 };
            for (int s = 0; s < o.sessionsPerParticipant; s++) {
                Instant start = day.plusSeconds(86400L * s + 1800L * p + rnd.nextInt(600));
                boolean crash = o.lastSessionCrashes && s == o.sessionsPerParticipant - 1;
                all.add(new SessionWriter(participant, s + 1, start, rewards, unlocked, crash).run());
            }
        }
        return all;
    }

    private String uuid() {
        UUID u = new UUID((rnd.nextLong() & 0xffffffffffff0fffL) | 0x0000000000004000L,
                (rnd.nextLong() & 0x3fffffffffffffffL) | 0x8000000000000000L);
        return u.toString();
    }

    /** Resultado de um lance: os eventos do tabuleiro e o resumo (casas avançadas e recuadas). */
    private static final class Outcome {
        final List<Object[]> events = new ArrayList<Object[]>(); // {tipo, dados}
        final int[] forward = new int[2];
        final int[] back = new int[2];
        int winner = -1;

        int net(int piece) {
            return forward[piece] - back[piece];
        }

        int relative() {
            return net(0) - net(1);
        }

        Map<String, Object> toJson() {
            return Json.obj("relative_net_gain", Integer.valueOf(relative()),
                    "net_gain", pair(net(0), net(1)),
                    "squares_forward", pair(forward[0], forward[1]),
                    "squares_back", pair(back[0], back[1]));
        }
    }

    private static Map<String, Object> pair(int player, int opponent) {
        return Json.obj("player", Integer.valueOf(player), "opponent", Integer.valueOf(opponent));
    }

    private final class SessionWriter {
        private final Session session = new Session();
        private final int sessionNumber;
        private final int[][] rewards;
        private final int[] unlocked;
        private final boolean crash;
        private Instant clock;
        private int seq;
        private Integer game;
        private Integer turn;
        private String screen = "registration";
        private int board;
        private final int[] squares = new int[2];
        private final Map<Integer, Integer> hazards = new TreeMap<Integer, Integer>();
        private int theme;

        SessionWriter(String participant, int sessionNumber, Instant start, int[][] rewards, int[] unlocked,
                boolean crash) {
            session.participantId = participant;
            session.sessionId = uuid();
            session.start = start;
            this.sessionNumber = sessionNumber;
            this.rewards = rewards;
            this.unlocked = unlocked;
            this.crash = crash;
            this.clock = start;
        }

        Session run() {
            int startLevel = new int[] { 1, 8, 14 }[rnd.nextInt(3)];
            emit("SESSION_START", Json.obj("session_number", Integer.valueOf(sessionNumber),
                    "start_level", startLevel == 1 ? "easy" : startLevel == 8 ? "intermediate" : "hard",
                    "start_complexity_level", Integer.valueOf(startLevel), "language", "pt", "app_version", "3.1.0",
                    "display_mode", "in_window"));
            int level = startLevel;
            for (int g = 1; g <= o.gamesPerSession; g++) {
                screen("theme");
                advance(3000);
                theme = rnd.nextInt(4) == 0 ? 1 : 0;
                emit("THEME_SELECTED", Json.obj("theme", THEMES[theme]));
                screen("instructions");
                advance(8000);
                screen("characters");
                if (allRewards() && unlocked[theme] < 6) {
                    int character = unlocked[theme];
                    unlocked[theme]++;
                    java.util.Arrays.fill(rewards[theme], 0);
                    emit("CHARACTER_UNLOCKED", Json.obj("character", Integer.valueOf(character), "theme", THEMES[theme]));
                }
                advance(2000);
                emit("CHARACTER_SELECTED", Json.obj("player_character", Integer.valueOf(rnd.nextInt(unlocked[theme])),
                        "opponent_character", Integer.valueOf(rnd.nextInt(4))));
                screen("choice");
                boolean last = g == o.gamesPerSession;
                int winner = game(g, level, last && o.quitMidGame ? 3 : Integer.MAX_VALUE);
                if (winner < 0) {
                    break; // a criança saiu no meio da partida
                }
                level = Math.max(1, Math.min(22, level + (winner == 0 ? 1 : -1)));
                screen("gameover");
                advance(4000);
                if (winner == 0) {
                    screen("save");
                    advance(3000);
                    int reward = nextReward();
                    rewards[theme][reward]++;
                    emit("REWARD_SAVED", Json.obj("reward_type", Integer.valueOf(reward + 1), "theme", THEMES[theme]));
                    screen("zoo");
                    advance(5000);
                }
            }
            if (crash) {
                return session; // jogo fechado à força: sem SESSION_END
            }
            boolean inGame = game != null;
            if (inGame) {
                advance(1500);
                emit("PAUSE", Json.obj("trigger", "menu", "screen", "choice"));
            }
            advance(2000);
            emit("SESSION_END", Json.obj("end_reason", inGame ? "quit" : rnd.nextBoolean() ? "quit" : "change_student",
                    "screen", inGame ? "choice" : screen, "game_in_progress", Boolean.valueOf(inGame)));
            return session;
        }

        private boolean allRewards() {
            for (int r : rewards[theme]) {
                if (r == 0) {
                    return false;
                }
            }
            return true;
        }

        private int nextReward() {
            List<Integer> missing = new ArrayList<Integer>();
            for (int i = 0; i < 7; i++) {
                if (rewards[theme][i] == 0) {
                    missing.add(Integer.valueOf(i));
                }
            }
            return missing.isEmpty() ? rnd.nextInt(7) : missing.get(rnd.nextInt(missing.size())).intValue();
        }

        /** Uma partida; devolve o vencedor (0 criança, 1 adversário) ou -1 se a sessão terminou antes. */
        private int game(int number, int level, int maxTurns) {
            board = o.boardLength + 2 * rnd.nextInt(4);
            squares[0] = 0;
            squares[1] = 0;
            hazards.clear();
            game = Integer.valueOf(number);
            turn = null;
            emit("GAME_START", Json.obj("board_length", Integer.valueOf(board), "complexity_level", Integer.valueOf(level)));
            difficulty(1);
            for (int t = 1;; t++) {
                if (t > maxTurns) {
                    return -1;
                }
                int winner = turn(t, Math.max(1, Math.min(22, level + rnd.nextInt(3) - 1)));
                if (winner >= 0) {
                    emit("GAME_END", Json.obj("winner", PIECES[winner], "turns_played", Integer.valueOf(t),
                            "final_squares", pair(squares[0], squares[1])));
                    game = null;
                    turn = null;
                    return winner;
                }
                if (rnd.nextDouble() < o.focusPauseRate && o.schemaVersion.equals("1.1")) {
                    advance(500);
                    emit("PAUSE", Json.obj("trigger", "window_focus", "screen", "choice"));
                    advance(5000 + rnd.nextInt(20000));
                    emit("RESUME", Json.obj("trigger", "window_focus", "screen", "choice"));
                }
                difficulty(t + 1);
            }
        }

        private void difficulty(int next) {
            advance(1000 + rnd.nextInt(2000));
            List<Object> coords = new ArrayList<Object>();
            for (int i = 0; i < 3; i++) {
                coords.add(Double.valueOf(rnd.nextInt(10)));
            }
            emit("DIFFICULTY_ADJUSTED", Json.obj("next_turn_number", Integer.valueOf(next),
                    "difficulty", Json.obj("speed", Double.valueOf(rnd.nextDouble()), "distance",
                            Double.valueOf(rnd.nextDouble()), "notation", Double.valueOf(rnd.nextDouble())),
                    "mean_success", Double.valueOf(rnd.nextDouble()), "desired_difficulty", Double.valueOf(0.75),
                    "estimated_difficulty", Double.valueOf(rnd.nextDouble()), "coords_chosen", coords));
        }

        /** Uma rodada; devolve o vencedor da partida, se ela terminou, ou -1. */
        private int turn(int number, int level) {
            turn = Integer.valueOf(number);
            boolean hazardsEnabled = level >= 5;
            boolean deadline = rnd.nextDouble() < 0.6;
            int deadlineMs = 3000 + rnd.nextInt(4000);
            boolean addition = level >= 8 && rnd.nextBoolean();
            boolean subtraction = !addition && level >= 9 && rnd.nextBoolean();
            int maxValue = level <= 7 ? 5 : 9;
            int[] values = { 1 + rnd.nextInt(maxValue), 0 };
            do {
                values[1] = 1 + rnd.nextInt(maxValue);
            } while (values[1] == values[0]);
            List<Object> onBoard = new ArrayList<Object>();
            for (Map.Entry<Integer, Integer> h : hazards.entrySet()) {
                onBoard.add(Json.obj("square", h.getKey(), "penalty", h.getValue()));
            }
            advance(1500);
            emit("TURN_START", Json.obj("complexity_level", Integer.valueOf(level),
                    "representation", Json.obj("dots", Boolean.valueOf(level <= 4), "spoken", Boolean.valueOf(level <= 4),
                            "digits", Boolean.TRUE, "dot_fading", Boolean.FALSE, "dot_fading_ms", Integer.valueOf(0)),
                    "max_value", Integer.valueOf(maxValue), "addition", Boolean.valueOf(addition),
                    "subtraction", Boolean.valueOf(subtraction), "hazards_enabled", Boolean.valueOf(hazardsEnabled),
                    "hazards_on_board", onBoard, "deadline_trial", Boolean.valueOf(deadline),
                    "deadline_ms", deadline ? Integer.valueOf(deadlineMs) : null,
                    "control_for", rnd.nextBoolean() ? "density" : "item_size",
                    "squares", pair(squares[0], squares[1])));
            advance(2000);
            if (rnd.nextDouble() < o.clickIgnoredRate) {
                emit("CLICK_IGNORED", Json.obj("side", SIDES[rnd.nextInt(2)]));
            }
            Instant left = stimulus(0, values[0], addition, subtraction);
            advance(1200 + rnd.nextInt(20));
            stimulus(1, values[1], addition, subtraction);
            emit("RESPONSE_ENABLED", Json.obj());
            Instant enabled = clock;
            int larger = values[0] > values[1] ? 0 : 1;
            boolean timeout = deadline && rnd.nextDouble() < o.timeoutRate;
            int chosen;
            if (timeout) {
                advance(deadlineMs + 20);
                chosen = 1 - larger;
            } else {
                chosen = rnd.nextDouble() < o.correctRate ? larger : 1 - larger;
                int rt = 300 + rnd.nextInt((deadline ? deadlineMs : 6000) - 300);
                if (rnd.nextDouble() < o.pauseRate) {
                    advance(rt / 2);
                    emit("PAUSE", Json.obj("trigger", "pause_key", "screen", "choice"));
                    advance(2000 + rnd.nextInt(8000)); // o prazo fica parado durante a pausa
                    emit("RESUME", Json.obj("trigger", "pause_key", "screen", "choice"));
                    advance(rt - rt / 2);
                } else {
                    advance(rt);
                }
            }
            Outcome actual = simulate(values[chosen], values[1 - chosen], true);
            Outcome hypothetical = simulate(values[1 - chosen], values[chosen], false);
            if (timeout) {
                emit("ANSWER_TIMEOUT", Json.obj("deadline_ms", Integer.valueOf(deadlineMs), "assigned_side", SIDES[chosen]));
            } else {
                emit("ANSWER", Json.obj("side", SIDES[chosen],
                        "response_time_ms", Long.valueOf(clock.toEpochMilli() - enabled.toEpochMilli()),
                        "response_time_from_first_stimulus_ms", Long.valueOf(clock.toEpochMilli() - left.toEpochMilli()),
                        "correct_larger", Boolean.valueOf(chosen == larger),
                        "correct_final", Boolean.valueOf(actual.relative() >= hypothetical.relative())));
            }
            emit("TURN_RESULT", Json.obj("turn_winner", actual.relative() >= 0 ? "player" : "opponent",
                    "outcome", actual.toJson(), "hypothetical_outcome", hypothetical.toJson()));
            feedback(timeout ? "enemy1_iTookTheMost" : actual.relative() >= 0 ? "enemy1_iHaveTheLeast" : "enemy1_iHaveTheMost");
            for (Object[] e : actual.events) {
                advance(2500 + rnd.nextInt(2500));
                emit((String) e[0], (Map<?, ?>) e[1]);
                if (!"MOVE".equals(e[0])) {
                    feedback("COLLISION".equals(e[0]) ? "enemy1_youSentMeBack" : "enemy1_youHaveToGoBack");
                }
            }
            if (actual.winner < 0 && hazardsEnabled && rnd.nextDouble() < o.newHazardRate) {
                int square = 1 + rnd.nextInt(board - 1);
                if (square != squares[0] && square != squares[1] && !hazards.containsKey(Integer.valueOf(square))) {
                    hazards.put(Integer.valueOf(square), Integer.valueOf(1 + rnd.nextInt(3)));
                }
            }
            return actual.winner;
        }

        private Instant stimulus(int side, int value, boolean addition, boolean subtraction) {
            Object operation = null;
            if (addition && rnd.nextBoolean()) {
                int a = rnd.nextInt(value + 1);
                operation = Json.obj("operator", "+", "operands", list(a, value - a));
            } else if (subtraction && rnd.nextBoolean()) {
                int b = rnd.nextInt(5);
                operation = Json.obj("operator", "-", "operands", list(value + b, b));
            }
            emit("STIMULUS_PRESENTED", Json.obj("side", SIDES[side], "value", Integer.valueOf(value), "operation", operation));
            return clock;
        }

        /**
         * Lance da rodada: a criança anda {@code playerSteps} e o adversário {@code opponentSteps}.
         * {@code apply}: altera o tabuleiro (lance real) ou só calcula (lance hipotético).
         */
        private Outcome simulate(int playerSteps, int opponentSteps, boolean apply) {
            int[] pos = { squares[0], squares[1] };
            Outcome out = new Outcome();
            int[] steps = { playerSteps, opponentSteps };
            for (int piece = 0; piece < 2 && out.winner < 0; piece++) {
                int from = pos[piece];
                int to = Math.min(board, from + steps[piece]);
                out.events.add(new Object[] { "MOVE", Json.obj("piece", PIECES[piece], "from_square", Integer.valueOf(from),
                        "to_square", Integer.valueOf(to), "steps", Integer.valueOf(steps[piece]),
                        "too_far_attempts", Integer.valueOf(rnd.nextInt(10) == 0 ? 1 : 0)) });
                out.forward[piece] += steps[piece];
                pos[piece] = to;
                if (to >= board) {
                    out.winner = piece;
                } else if (pos[1 - piece] == to) {
                    int other = 1 - piece;
                    int back = Math.max(0, to - 1);
                    out.events.add(new Object[] { "COLLISION", Json.obj("pushed_piece", PIECES[other],
                            "from_square", Integer.valueOf(to), "to_square", Integer.valueOf(back)) });
                    out.back[other] += to - back;
                    pos[other] = back;
                } else if (hazards.containsKey(Integer.valueOf(to))) {
                    int penalty = hazards.get(Integer.valueOf(to)).intValue();
                    int back = Math.max(0, to - penalty);
                    out.events.add(new Object[] { "HAZARD", Json.obj("piece", PIECES[piece], "square", Integer.valueOf(to),
                            "penalty", Integer.valueOf(penalty), "to_square", Integer.valueOf(back)) });
                    out.back[piece] += to - back;
                    pos[piece] = back;
                }
            }
            if (apply) {
                squares[0] = pos[0];
                squares[1] = pos[1];
            }
            return out;
        }

        private void feedback(String key) {
            advance(1500);
            emit("FEEDBACK_PRESENTED", Json.obj("message_key", key));
        }

        private void screen(String to) {
            advance(500);
            emit("SCREEN_CHANGED", Json.obj("from_screen", screen, "to_screen", to));
            screen = to;
        }

        private void advance(long ms) {
            clock = clock.plusMillis(ms);
        }

        private void emit(String type, Map<?, ?> data) {
            Map<String, Object> e = new LinkedHashMap<String, Object>();
            e.put("schema_version", o.schemaVersion);
            e.put("event_id", uuid());
            e.put("event_type", type);
            e.put("timestamp", TIMESTAMP.format(clock));
            e.put("session_id", session.sessionId);
            e.put("participant_id", session.participantId);
            e.put("sequence_number", Integer.valueOf(++seq));
            if (game != null) {
                e.put("activity", "number_comparison");
                e.put("game_number", game);
                if (turn != null) {
                    e.put("turn_number", turn);
                }
            }
            e.put("data", data);
            session.lines.add(Json.write(e));
        }
    }

    private static List<Object> list(int a, int b) {
        List<Object> l = new ArrayList<Object>();
        l.add(Integer.valueOf(a));
        l.add(Integer.valueOf(b));
        return l;
    }
}
