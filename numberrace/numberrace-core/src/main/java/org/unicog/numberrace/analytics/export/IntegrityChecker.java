package org.unicog.numberrace.analytics.export;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verificação de integridade (evolution/pt/validacao-registros.md): confere, sessão por sessão,
 * regras que todo registro correto cumpre, por exemplo a ordem dos eventos de uma rodada, a
 * continuidade das casas no tabuleiro e a coerência entre o tempo de resposta e os horários.
 *
 * <p>Cada problema é um {@link Finding} com a regra violada, a gravidade e o evento. Erros indicam
 * dados incoerentes; avisos indicam situações que merecem atenção, mas podem ter explicação
 * legítima (por exemplo, o relógio do computador acertado durante a sessão). A verificação não
 * altera nem descarta eventos.
 */
final class IntegrityChecker {

    enum Severity {
        ERROR("erro"), WARNING("aviso");

        final String label;

        Severity(String label) {
            this.label = label;
        }
    }

    /** Uma regra do catálogo. */
    static final class Rule {
        final String id;
        final Severity severity;
        final String description;

        Rule(String id, Severity severity, String description) {
            this.id = id;
            this.severity = severity;
            this.description = description;
        }
    }

    /** Um problema encontrado numa sessão. */
    static final class Finding {
        final Rule rule;
        final String sessionId;
        final String participantId;
        final Integer sequenceNumber;
        final Integer gameNumber;
        final Integer turnNumber;
        final String message;

        /** {@code game} e {@code turn}: partida e rodada do problema (podem ser nulos). */
        Finding(Rule rule, SessionData s, SourceEvent e, Integer game, Integer turn, String message) {
            this.rule = rule;
            this.sessionId = s.getSessionId();
            this.participantId = s.getParticipantId();
            this.sequenceNumber = e == null ? null : Integer.valueOf(e.getSequenceNumber());
            this.gameNumber = game;
            this.turnNumber = turn;
            this.message = message;
        }
    }

    /** Diferença tolerada entre o tempo de resposta (cronômetro) e os horários dos eventos. */
    static final long TIME_TOLERANCE_MS = 50;

    static final Rule SEQUENCE = rule("R01", Severity.ERROR, "Números de sequência 1, 2, 3... sem lacunas");
    static final Rule CLOCK = rule("R02", Severity.WARNING,
            "Horários em ordem (um horário que volta indica relógio do computador ajustado durante a sessão)");
    static final Rule SESSION_START = rule("R03", Severity.ERROR, "A sessão começa com um único SESSION_START");
    static final Rule SESSION_END = rule("R04", Severity.WARNING,
            "A sessão termina com um único SESSION_END (sem ele, o jogo foi fechado de forma inesperada)");
    static final Rule ONE_FILE = rule("R05", Severity.WARNING, "Cada arquivo contém eventos de uma única sessão");
    static final Rule NUMBERING = rule("R06", Severity.ERROR,
            "Partidas e rodadas numeradas em ordem (game_number e turn_number coerentes com GAME_START e TURN_START)");
    static final Rule TURN_ORDER = rule("R07", Severity.ERROR,
            "Ordem da rodada: início, estímulo da esquerda, estímulo da direita, liberação, resposta ou prazo "
                    + "esgotado e resultado, cada um uma única vez (a última rodada pode ficar incompleta quando a "
                    + "sessão ou a partida termina no meio dela)");
    static final Rule RESPONSE_TIME = rule("R08", Severity.WARNING,
            "O tempo de resposta gravado é igual à diferença entre os horários dos eventos (tolerância de "
                    + TIME_TOLERANCE_MS + " ms)");
    static final Rule RESPONSE_TIMES = rule("R09", Severity.ERROR,
            "O tempo desde o primeiro estímulo não é menor que o tempo desde a liberação");
    static final Rule DEADLINE = rule("R10", Severity.WARNING,
            "Resposta dentro do prazo, nas rodadas com prazo e sem pausa registrada durante a espera pela resposta");
    static final Rule TIMEOUT = rule("R11", Severity.ERROR,
            "Prazo esgotado só em rodada com prazo, com o mesmo prazo informado no início da rodada");
    static final Rule PAUSES = rule("R12", Severity.ERROR,
            "Pausa e retomada se alternam (a sessão pode terminar pausada)");
    static final Rule SQUARES = rule("R13", Severity.ERROR,
            "Casas contínuas: cada movimento, esbarrão ou armadilha parte da casa onde a peça estava, e o "
                    + "início da rodada e o fim da partida informam essas casas");
    static final Rule MOVES = rule("R14", Severity.ERROR,
            "Movimento coerente: casas avançadas iguais a destino menos origem, sem passar do fim do tabuleiro, "
                    + "e recuo da armadilha igual à penalidade");
    static final Rule OUTCOME = rule("R15", Severity.ERROR,
            "Os movimentos da rodada conferem com o resultado calculado pelo jogo (casas avançadas e recuadas)");
    static final Rule DIFFICULTY = rule("R16", Severity.ERROR,
            "Cada rodada é anunciada por um ajuste de dificuldade (DIFFICULTY_ADJUSTED.next_turn_number)");
    static final Rule CORRECTNESS = rule("R17", Severity.ERROR,
            "Acerto coerente: correct_larger indica a escolha do maior valor; no prazo esgotado, a criança recebe "
                    + "o menor");
    static final Rule OPERATIONS = rule("R18", Severity.ERROR,
            "Contas coerentes: o valor é o resultado da conta, e só há contas em níveis com adição ou subtração");
    static final Rule GAME_END = rule("R19", Severity.ERROR,
            "Fim de partida coerente: turns_played igual às rodadas registradas e vencedor no fim do tabuleiro");
    static final Rule SESSION_END_STATE = rule("R20", Severity.ERROR,
            "Fim de sessão coerente: game_in_progress verdadeiro só se havia partida sem GAME_END");
    static final Rule GAME_ABANDONED = rule("R21", Severity.WARNING,
            "Nova partida iniciada sem o GAME_END da partida anterior");

    static final List<Rule> RULES = Collections.unmodifiableList(java.util.Arrays.asList(SEQUENCE, CLOCK,
            SESSION_START, SESSION_END, ONE_FILE, NUMBERING, TURN_ORDER, RESPONSE_TIME, RESPONSE_TIMES, DEADLINE,
            TIMEOUT, PAUSES, SQUARES, MOVES, OUTCOME, DIFFICULTY, CORRECTNESS, OPERATIONS, GAME_END,
            SESSION_END_STATE, GAME_ABANDONED));

    private static Rule rule(String id, Severity severity, String description) {
        return new Rule(id, severity, description);
    }

    private static final String[] PIECES = { "player", "opponent" };

    private IntegrityChecker() {
    }

    /**
     * Confere uma sessão. {@code sessionsPerFile} diz, para cada arquivo de origem, quantas
     * sessões diferentes ele contém (regra R05); pode ser vazio.
     */
    static List<Finding> check(SessionData s, Map<String, Integer> sessionsPerFile) {
        return new SessionCheck(s, sessionsPerFile).run();
    }

    /** Confere todas as sessões, na ordem recebida. */
    static List<Finding> check(List<SessionData> sessions, Map<String, Integer> sessionsPerFile) {
        List<Finding> all = new ArrayList<Finding>();
        for (SessionData s : sessions) {
            all.addAll(check(s, sessionsPerFile));
        }
        return all;
    }

    static int count(List<Finding> findings, Severity severity) {
        int n = 0;
        for (Finding f : findings) {
            if (f.rule.severity == severity) {
                n++;
            }
        }
        return n;
    }

    /** Resumo de uma linha, para o console. */
    static String summary(int sessions, List<Finding> findings) {
        return "Verificação de integridade: " + sessions + " sessão(ões), " + RULES.size() + " regras; "
                + count(findings, Severity.ERROR) + " erro(s) e " + count(findings, Severity.WARNING) + " aviso(s)";
    }

    /** Relatório em texto, para o arquivo {@code relatorio-integridade.txt}. */
    static String report(int sessions, List<Finding> findings) {
        StringBuilder sb = new StringBuilder("RELATÓRIO DA VERIFICAÇÃO DE INTEGRIDADE\n\n");
        sb.append(summary(sessions, findings)).append(".\n\n");
        sb.append("Erro: dados incoerentes entre si. Aviso: situação que merece atenção, mas pode ter explicação\n")
                .append("legítima. A verificação não altera nem descarta eventos.\n\n");
        sb.append("REGRAS\n\n");
        Map<Rule, Integer> perRule = new LinkedHashMap<Rule, Integer>();
        Map<Rule, Set<String>> sessionsPerRule = new LinkedHashMap<Rule, Set<String>>();
        for (Finding f : findings) {
            Integer n = perRule.get(f.rule);
            perRule.put(f.rule, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
            Set<String> ids = sessionsPerRule.get(f.rule);
            if (ids == null) {
                ids = new HashSet<String>();
                sessionsPerRule.put(f.rule, ids);
            }
            ids.add(f.sessionId);
        }
        for (Rule r : RULES) {
            Integer n = perRule.get(r);
            sb.append(r.id).append(" (").append(r.severity.label).append(") ").append(r.description).append('\n');
            sb.append("    ").append(n == null ? "OK"
                    : n + " ocorrência(s) em " + sessionsPerRule.get(r).size() + " sessão(ões)").append('\n');
        }
        if (!findings.isEmpty()) {
            sb.append("\nPROBLEMAS ENCONTRADOS (também em integrity.csv)\n");
            String current = null;
            for (Finding f : findings) {
                if (!f.sessionId.equals(current)) {
                    current = f.sessionId;
                    sb.append("\nSessão ").append(f.sessionId).append(" (").append(f.participantId).append("):\n");
                }
                sb.append("- ").append(f.rule.id).append(' ').append(f.rule.severity.label);
                if (f.sequenceNumber != null) {
                    sb.append(", evento ").append(f.sequenceNumber);
                }
                if (f.gameNumber != null) {
                    sb.append(", partida ").append(f.gameNumber);
                }
                if (f.turnNumber != null) {
                    sb.append(", rodada ").append(f.turnNumber);
                }
                sb.append(": ").append(f.message).append('\n');
            }
        } else {
            sb.append("\nNenhum problema encontrado.\n");
        }
        return sb.toString();
    }

    /** Estado de uma rodada enquanto os seus eventos são lidos. */
    private static final class Turn {
        final int game;
        final int number;
        final List<String> core = new ArrayList<String>();
        Map<String, Object> start;
        final Map<String, Long> values = new LinkedHashMap<String, Long>();
        Instant leftStimulus;
        Instant responseEnabled;
        SourceEvent answer;
        Map<String, Object> answerData;
        Map<String, Object> result;
        SourceEvent resultEvent;
        boolean pausedWhileWaiting;
        boolean gameEnded;
        final int[] forward = new int[2];
        final int[] back = new int[2];
        final boolean[] moved = new boolean[2];

        Turn(int game, int number) {
            this.game = game;
            this.number = number;
        }

        boolean finished() {
            return result != null && (core.contains("ANSWER") || core.contains("ANSWER_TIMEOUT"));
        }
    }

    private static final class SessionCheck {
        private final SessionData s;
        private final Map<String, Integer> sessionsPerFile;
        private final List<Finding> findings = new ArrayList<Finding>();

        private Instant previousTime;
        private boolean pauseOpen;
        private Integer game;
        private boolean gameOpen;
        private int lastGame;
        private int boardLength;
        private int lastTurn;
        private int turnsInGame;
        private final int[] squares = new int[2];
        private final Set<Integer> announced = new HashSet<Integer>();
        private Turn turn;

        SessionCheck(SessionData s, Map<String, Integer> sessionsPerFile) {
            this.s = s;
            this.sessionsPerFile = sessionsPerFile;
        }

        List<Finding> run() {
            if (!s.getMissingSequenceNumbers().isEmpty()) {
                add(SEQUENCE, null, "faltam os eventos de sequência " + s.getMissingSequenceNumbers());
            }
            for (String file : s.getSourceFiles()) {
                Integer n = sessionsPerFile.get(file);
                if (n != null && n.intValue() > 1) {
                    add(ONE_FILE, null, "o arquivo " + file + " contém eventos de " + n + " sessões");
                }
            }
            List<SourceEvent> events = s.getEvents();
            for (int i = 0; i < events.size(); i++) {
                SourceEvent e = events.get(i);
                SourceEvent.Parsed p;
                try {
                    p = e.parse();
                } catch (RuntimeException ex) {
                    continue; // já validado na importação
                }
                checkEnvelope(e, p, i, events.size());
                handle(e, p);
            }
            closeTurn(null);
            return findings;
        }

        private void checkEnvelope(SourceEvent e, SourceEvent.Parsed p, int index, int count) {
            Instant time = p.time().toInstant();
            if (previousTime != null && time.isBefore(previousTime)) {
                add(CLOCK, e, "horário " + p.timestamp() + " anterior ao do evento anterior (diferença de "
                        + (previousTime.toEpochMilli() - time.toEpochMilli()) + " ms)");
            }
            previousTime = time;
            if (e.is("SESSION_START") && index > 0) {
                add(SESSION_START, e, "SESSION_START fora do início da sessão");
            }
            if (index == 0 && !e.is("SESSION_START")) {
                add(SESSION_START, e, "a sessão começa com " + e.getEventType() + " em vez de SESSION_START");
            }
            if (e.is("SESSION_END") && index < count - 1) {
                add(SESSION_END, e, "SESSION_END antes do último evento da sessão");
            }
            if (index == count - 1 && !e.is("SESSION_END")) {
                add(SESSION_END, e, "a sessão não termina com SESSION_END (jogo fechado de forma inesperada ou "
                        + "sessão ainda aberta)");
            }
        }

        private void handle(SourceEvent e, SourceEvent.Parsed p) {
            Map<String, Object> d = p.data();
            String type = e.getEventType();
            checkContext(e);
            if ("GAME_START".equals(type)) {
                closeTurn(e);
                if (gameOpen) {
                    add(GAME_ABANDONED, e, "a partida " + game + " não tem GAME_END");
                }
                int number = e.getGameNumber() == null ? lastGame + 1 : e.getGameNumber().intValue();
                if (number != lastGame + 1) {
                    add(NUMBERING, e, "partida " + number + " depois da partida " + lastGame);
                }
                lastGame = number;
                game = Integer.valueOf(number);
                gameOpen = true;
                boardLength = intValue(d.get("board_length"));
                lastTurn = 0;
                turnsInGame = 0;
                squares[0] = 0;
                squares[1] = 0;
                announced.clear();
            } else if ("DIFFICULTY_ADJUSTED".equals(type)) {
                announced.add(Integer.valueOf(intValue(d.get("next_turn_number"))));
            } else if ("TURN_START".equals(type)) {
                closeTurn(e);
                int number = e.getTurnNumber() == null ? lastTurn + 1 : e.getTurnNumber().intValue();
                if (number != lastTurn + 1) {
                    add(NUMBERING, e, "rodada " + number + " depois da rodada " + lastTurn);
                }
                lastTurn = number;
                turnsInGame++;
                if (!announced.contains(Integer.valueOf(number))) {
                    add(DIFFICULTY, e, "nenhum DIFFICULTY_ADJUSTED anunciou a rodada " + number);
                }
                turn = new Turn(game == null ? 0 : game.intValue(), number);
                turn.core.add(type);
                turn.start = d;
                Map<String, Object> sq = map(d.get("squares"));
                checkSquare(e, 0, sq.get("player"), "TURN_START.squares.player");
                checkSquare(e, 1, sq.get("opponent"), "TURN_START.squares.opponent");
            } else if (turn != null && sameTurn(e)) {
                handleTurnEvent(e, p, d, type);
            }
            if ("PAUSE".equals(type)) {
                if (pauseOpen) {
                    add(PAUSES, e, "PAUSE sem RESUME depois da pausa anterior");
                }
                pauseOpen = true;
                if (turn != null && turn.responseEnabled != null && turn.answerData == null
                        && !turn.core.contains("ANSWER_TIMEOUT")) {
                    turn.pausedWhileWaiting = true;
                }
            } else if ("RESUME".equals(type)) {
                if (!pauseOpen) {
                    add(PAUSES, e, "RESUME sem PAUSE antes");
                }
                pauseOpen = false;
            } else if ("GAME_END".equals(type)) {
                if (turn != null) {
                    turn.gameEnded = true;
                }
                closeTurn(e);
                if (intValue(d.get("turns_played")) != turnsInGame) {
                    add(GAME_END, e, "turns_played = " + d.get("turns_played") + ", mas há " + turnsInGame
                            + " rodadas registradas");
                }
                Map<String, Object> fin = map(d.get("final_squares"));
                checkSquare(e, 0, fin.get("player"), "GAME_END.final_squares.player");
                checkSquare(e, 1, fin.get("opponent"), "GAME_END.final_squares.opponent");
                Object winner = d.get("winner");
                int w = "opponent".equals(winner) ? 1 : 0;
                if (boardLength > 0 && intValue(fin.get(PIECES[w])) < boardLength) {
                    add(GAME_END, e, "o vencedor (" + winner + ") terminou na casa " + fin.get(PIECES[w])
                            + ", antes do fim do tabuleiro (" + boardLength + ")");
                }
                gameOpen = false;
            } else if ("SESSION_END".equals(type)) {
                closeTurn(e);
                Object inProgress = d.get("game_in_progress");
                if (inProgress instanceof Boolean && ((Boolean) inProgress).booleanValue() != gameOpen) {
                    add(SESSION_END_STATE, e, "game_in_progress = " + inProgress + ", mas "
                            + (gameOpen ? "a partida " + game + " não tem GAME_END" : "não havia partida em curso"));
                }
                gameOpen = false;
            }
        }

        /** game_number e turn_number do evento conferem com a partida e a rodada em curso. */
        private void checkContext(SourceEvent e) {
            if (e.is("GAME_START") || e.getGameNumber() == null) {
                return;
            }
            if (e.is("TURN_START")) {
                if (!gameOpen || !e.getGameNumber().equals(game)) {
                    add(NUMBERING, e, "TURN_START com game_number " + e.getGameNumber() + " fora da partida em curso");
                }
                return;
            }
            if (!gameOpen || !e.getGameNumber().equals(game)) {
                add(NUMBERING, e, e.getEventType() + " com game_number " + e.getGameNumber() + " fora da partida "
                        + (gameOpen ? "em curso (" + game + ")" : "(nenhuma partida em curso)"));
            } else if (e.getTurnNumber() != null && (turn == null || e.getTurnNumber().intValue() != turn.number)) {
                add(NUMBERING, e, e.getEventType() + " com turn_number " + e.getTurnNumber() + " fora da rodada em curso");
            }
        }

        private boolean sameTurn(SourceEvent e) {
            return e.getTurnNumber() != null && e.getTurnNumber().intValue() == turn.number
                    && e.getGameNumber() != null && e.getGameNumber().intValue() == turn.game;
        }

        private void handleTurnEvent(SourceEvent e, SourceEvent.Parsed p, Map<String, Object> d, String type) {
            if ("STIMULUS_PRESENTED".equals(type)) {
                String side = String.valueOf(d.get("side"));
                turn.core.add(type + ":" + side);
                if ("left".equals(side)) {
                    turn.leftStimulus = p.time().toInstant();
                }
                Object v = d.get("value");
                if (v instanceof Number) {
                    turn.values.put(side, Long.valueOf(((Number) v).longValue()));
                }
                checkOperation(e, d);
            } else if ("RESPONSE_ENABLED".equals(type)) {
                turn.core.add(type);
                turn.responseEnabled = p.time().toInstant();
            } else if ("ANSWER".equals(type)) {
                turn.core.add(type);
                turn.answer = e;
                turn.answerData = d;
                checkAnswer(e, p, d);
            } else if ("ANSWER_TIMEOUT".equals(type)) {
                turn.core.add(type);
                checkTimeout(e, d);
            } else if ("TURN_RESULT".equals(type)) {
                turn.core.add(type);
                turn.result = d;
                turn.resultEvent = e;
            } else if ("MOVE".equals(type)) {
                int piece = piece(d.get("piece"));
                int from = intValue(d.get("from_square"));
                int to = intValue(d.get("to_square"));
                int steps = intValue(d.get("steps"));
                checkSquare(e, piece, d.get("from_square"), "MOVE.from_square");
                if (to > boardLength && boardLength > 0) {
                    add(MOVES, e, "destino " + to + " além do fim do tabuleiro (" + boardLength + ")");
                }
                if (to - from != steps && !(to == boardLength && to - from < steps)) {
                    add(MOVES, e, "steps = " + steps + ", mas a peça foi de " + from + " para " + to);
                }
                turn.forward[piece] += steps;
                turn.moved[piece] = true;
                squares[piece] = to;
            } else if ("COLLISION".equals(type) || "HAZARD".equals(type)) {
                int piece = piece("COLLISION".equals(type) ? d.get("pushed_piece") : d.get("piece"));
                Object fromValue = "COLLISION".equals(type) ? d.get("from_square") : d.get("square");
                int from = intValue(fromValue);
                int to = intValue(d.get("to_square"));
                checkSquare(e, piece, fromValue, type + ("COLLISION".equals(type) ? ".from_square" : ".square"));
                if ("HAZARD".equals(type) && to != Math.max(0, from - intValue(d.get("penalty")))) {
                    add(MOVES, e, "armadilha de " + d.get("penalty") + " casa(s) na casa " + from
                            + ", mas a peça foi para " + to);
                }
                turn.back[piece] += from - to;
                squares[piece] = to;
            }
        }

        private void checkOperation(SourceEvent e, Map<String, Object> d) {
            Map<String, Object> op = map(d.get("operation"));
            if (op.isEmpty()) {
                return;
            }
            Map<String, Object> start = turn.start == null ? Collections.<String, Object> emptyMap() : turn.start;
            Object operator = op.get("operator");
            Object operands = op.get("operands");
            if ("+".equals(operator) && !Boolean.TRUE.equals(start.get("addition"))
                    || "-".equals(operator) && !Boolean.TRUE.equals(start.get("subtraction"))) {
                add(OPERATIONS, e, "conta com " + operator + " numa rodada sem essa operação no nível");
            }
            if (operands instanceof List && ((List<?>) operands).size() == 2 && d.get("value") instanceof Number) {
                long a = longValue(((List<?>) operands).get(0));
                long b = longValue(((List<?>) operands).get(1));
                long value = ((Number) d.get("value")).longValue();
                long expected = "-".equals(operator) ? a - b : a + b;
                if (expected != value) {
                    add(OPERATIONS, e, "a conta " + a + " " + operator + " " + b + " dá " + expected + ", mas o valor é "
                            + value);
                }
            }
        }

        private void checkAnswer(SourceEvent e, SourceEvent.Parsed p, Map<String, Object> d) {
            long rt = longValue(d.get("response_time_ms"));
            long fromFirst = longValue(d.get("response_time_from_first_stimulus_ms"));
            Instant at = p.time().toInstant();
            if (fromFirst < rt) {
                add(RESPONSE_TIMES, e, "tempo desde o primeiro estímulo (" + fromFirst
                        + " ms) menor que o tempo desde a liberação (" + rt + " ms)");
            }
            if (turn.responseEnabled != null) {
                long byClock = at.toEpochMilli() - turn.responseEnabled.toEpochMilli();
                if (Math.abs(byClock - rt) > TIME_TOLERANCE_MS) {
                    add(RESPONSE_TIME, e, "response_time_ms = " + rt + ", mas os horários indicam " + byClock + " ms");
                }
            }
            if (turn.leftStimulus != null) {
                long byClock = at.toEpochMilli() - turn.leftStimulus.toEpochMilli();
                if (Math.abs(byClock - fromFirst) > TIME_TOLERANCE_MS) {
                    add(RESPONSE_TIME, e, "response_time_from_first_stimulus_ms = " + fromFirst
                            + ", mas os horários indicam " + byClock + " ms");
                }
            }
            Map<String, Object> start = turn.start;
            if (start != null && Boolean.TRUE.equals(start.get("deadline_trial")) && !turn.pausedWhileWaiting
                    && start.get("deadline_ms") instanceof Number
                    && rt > ((Number) start.get("deadline_ms")).longValue() + TIME_TOLERANCE_MS) {
                add(DEADLINE, e, "resposta em " + rt + " ms, depois do prazo de " + start.get("deadline_ms") + " ms");
            }
            Long left = turn.values.get("left");
            Long right = turn.values.get("right");
            Object side = d.get("side");
            if (left != null && right != null && d.get("correct_larger") instanceof Boolean
                    && left.longValue() != right.longValue()) {
                boolean choseLarger = "left".equals(side) ? left.longValue() > right.longValue()
                        : right.longValue() > left.longValue();
                if (choseLarger != ((Boolean) d.get("correct_larger")).booleanValue()) {
                    add(CORRECTNESS, e, "correct_larger = " + d.get("correct_larger") + ", mas a criança escolheu "
                            + side + " (esquerda " + left + ", direita " + right + ")");
                }
            }
        }

        private void checkTimeout(SourceEvent e, Map<String, Object> d) {
            Map<String, Object> start = turn.start;
            if (start != null) {
                if (!Boolean.TRUE.equals(start.get("deadline_trial"))) {
                    add(TIMEOUT, e, "prazo esgotado numa rodada sem prazo");
                } else if (longValue(d.get("deadline_ms")) != longValue(start.get("deadline_ms"))) {
                    add(TIMEOUT, e, "prazo de " + d.get("deadline_ms") + " ms, mas a rodada começou com "
                            + start.get("deadline_ms") + " ms");
                }
            }
            Long left = turn.values.get("left");
            Long right = turn.values.get("right");
            if (left != null && right != null && left.longValue() != right.longValue()) {
                String smaller = left.longValue() < right.longValue() ? "left" : "right";
                if (!smaller.equals(d.get("assigned_side"))) {
                    add(CORRECTNESS, e, "assigned_side = " + d.get("assigned_side") + ", mas o menor valor está em "
                            + smaller);
                }
            }
        }

        /**
         * Confere a rodada que terminou: ordem dos eventos, movimentos e resultado. {@code next} é o
         * evento que encerrou a rodada (nulo no fim dos eventos da sessão).
         */
        private void closeTurn(SourceEvent next) {
            Turn t = turn;
            turn = null;
            if (t == null) {
                return;
            }
            List<String> core = t.core;
            List<String> withAnswer = java.util.Arrays.asList("TURN_START", "STIMULUS_PRESENTED:left",
                    "STIMULUS_PRESENTED:right", "RESPONSE_ENABLED", "ANSWER", "TURN_RESULT");
            List<String> withTimeout = java.util.Arrays.asList("TURN_START", "STIMULUS_PRESENTED:left",
                    "STIMULUS_PRESENTED:right", "RESPONSE_ENABLED", "ANSWER_TIMEOUT", "TURN_RESULT");
            boolean complete = core.equals(withAnswer) || core.equals(withTimeout);
            if (!complete) {
                boolean prefix = isPrefix(core, withAnswer) || isPrefix(core, withTimeout);
                // sessão encerrada (ou jogo fechado) no meio da rodada: comportamento normal da criança
                boolean interrupted = next == null || next.is("SESSION_END") || next.is("GAME_START");
                if (!(prefix && interrupted)) {
                    add(TURN_ORDER, t.resultEvent != null ? t.resultEvent : next, "rodada " + t.number
                            + " da partida " + t.game + " com os eventos " + describe(core), t);
                }
            }
            if (t.result != null && !t.gameEnded && t.moved[0] && t.moved[1]) {
                Map<String, Object> outcome = map(t.result.get("outcome"));
                Map<String, Object> forward = map(outcome.get("squares_forward"));
                Map<String, Object> back = map(outcome.get("squares_back"));
                for (int piece = 0; piece < 2; piece++) {
                    if (intValue(forward.get(PIECES[piece])) != t.forward[piece]
                            || intValue(back.get(PIECES[piece])) != t.back[piece]) {
                        add(OUTCOME, t.resultEvent, "resultado da rodada " + t.number + " indica " + PIECES[piece]
                                + " avançando " + forward.get(PIECES[piece]) + " e recuando " + back.get(PIECES[piece])
                                + ", mas os movimentos registrados somam " + t.forward[piece] + " e " + t.back[piece], t);
                    }
                }
            }
        }

        private static boolean isPrefix(List<String> core, List<String> full) {
            return core.size() <= full.size() && full.subList(0, core.size()).equals(core);
        }

        private static String describe(List<String> core) {
            return core.toString().replace(":", " ");
        }

        private void checkSquare(SourceEvent e, int piece, Object reported, String field) {
            if (!(reported instanceof Number)) {
                return;
            }
            int value = ((Number) reported).intValue();
            if (value != squares[piece]) {
                add(SQUARES, e, field + " = " + value + ", mas a peça " + PIECES[piece] + " estava na casa "
                        + squares[piece]);
                squares[piece] = value; // segue a partir do valor informado, para não repetir o mesmo problema
            }
        }

        private void add(Rule r, SourceEvent e, String message) {
            findings.add(new Finding(r, s, e, e == null ? null : e.getGameNumber(),
                    e == null ? null : e.getTurnNumber(), message));
        }

        private void add(Rule r, SourceEvent e, String message, Turn t) {
            findings.add(new Finding(r, s, e, Integer.valueOf(t.game), Integer.valueOf(t.number), message));
        }
    }

    private static int piece(Object name) {
        return "opponent".equals(name) ? 1 : 0;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object v) {
        return v instanceof Map ? (Map<String, Object>) v : Collections.<String, Object> emptyMap();
    }

    private static int intValue(Object v) {
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    private static long longValue(Object v) {
        return v instanceof Number ? ((Number) v).longValue() : 0;
    }
}
