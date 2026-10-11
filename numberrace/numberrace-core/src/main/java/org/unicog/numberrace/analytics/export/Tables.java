package org.unicog.numberrace.analytics.export;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.unicog.numberrace.analytics.Json;
import org.unicog.numberrace.analytics.export.Table.Type;

/**
 * Monta as tabelas exportadas a partir dos eventos: {@code events} (um evento por linha),
 * {@code sessions} (uma sessão por linha) e {@code turns} (uma rodada por linha). As duas
 * últimas são visões derivadas: só reorganizam e contam o que está nos eventos, sem
 * interpretação.
 */
final class Tables {

    private static final String ENVELOPE = "parte comum do evento";

    private Tables() {
    }

    static Table events(final List<SessionData> sessions) {
        Table t = new Table("events", "Um evento por linha, na ordem das sessões e de sequence_number. "
                + "A parte específica de cada evento fica em data_json.");
        t.column("schema_version", Type.TEXT, "", "Versão do modelo de eventos", ENVELOPE)
                .column("event_id", Type.TEXT, "", "Identificador único do evento (UUID)", ENVELOPE)
                .column("event_type", Type.TEXT, "", "Tipo do evento (modelo-eventos.md, seção 7)", ENVELOPE)
                .column("timestamp", Type.TEXT, "ISO 8601", "Data e hora do evento, com milissegundos e fuso", ENVELOPE)
                .column("session_id", Type.TEXT, "", "Identificador da sessão (UUID)", ENVELOPE)
                .column("participant_id", Type.TEXT, "", "Código pseudonimizado do participante", ENVELOPE)
                .column("sequence_number", Type.INTEGER, "", "Ordem do evento na sessão, a partir de 1", ENVELOPE)
                .column("activity", Type.TEXT, "", "Atividade (number_comparison), quando há partida em curso", ENVELOPE)
                .column("game_number", Type.INTEGER, "", "Número da partida na sessão, quando há partida em curso", ENVELOPE)
                .column("turn_number", Type.INTEGER, "", "Número da rodada na partida, quando há rodada em curso", ENVELOPE)
                .column("data_json", Type.TEXT, "JSON", "Parte específica do evento (campo data), em JSON", "data");
        int count = 0;
        for (SessionData s : sessions) {
            count += s.getEvents().size();
        }
        t.setRowSource(new Iterable<Map<String, Object>>() {
            public Iterator<Map<String, Object>> iterator() {
                return new Iterator<Map<String, Object>>() {
                    private final Iterator<SessionData> sessionIt = sessions.iterator();
                    private Iterator<SourceEvent> eventIt = Collections.<SourceEvent> emptyList().iterator();

                    public boolean hasNext() {
                        while (!eventIt.hasNext() && sessionIt.hasNext()) {
                            eventIt = sessionIt.next().getEvents().iterator();
                        }
                        return eventIt.hasNext();
                    }

                    public Map<String, Object> next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        return eventRow(eventIt.next());
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }, count);
        return t;
    }

    private static Map<String, Object> eventRow(SourceEvent e) {
        SourceEvent.Parsed p = e.parse();
        Map<String, Object> r = new LinkedHashMap<String, Object>();
        r.put("schema_version", e.getSchemaVersion());
        r.put("event_id", e.getEventId());
        r.put("event_type", e.getEventType());
        r.put("timestamp", p.timestamp());
        r.put("session_id", e.getSessionId());
        r.put("participant_id", e.getParticipantId());
        r.put("sequence_number", Integer.valueOf(e.getSequenceNumber()));
        r.put("activity", e.getActivity());
        r.put("game_number", e.getGameNumber());
        r.put("turn_number", e.getTurnNumber());
        r.put("data_json", Json.write(p.data()));
        return r;
    }

    static Table sessions(List<SessionData> sessions) {
        return sessions(sessions, null);
    }

    /**
     * Tabela de sessões. {@code findings}: resultado da verificação de integridade, para as colunas
     * {@code integrity_errors} e {@code integrity_warnings} (nulo: colunas vazias).
     */
    static Table sessions(List<SessionData> sessions, List<IntegrityChecker.Finding> findings) {
        Map<String, int[]> integrity = new HashMap<String, int[]>();
        if (findings != null) {
            for (IntegrityChecker.Finding f : findings) {
                int[] c = integrity.get(f.sessionId);
                if (c == null) {
                    c = new int[2];
                    integrity.put(f.sessionId, c);
                }
                c[f.rule.severity == IntegrityChecker.Severity.ERROR ? 0 : 1]++;
            }
        }
        Table t = new Table("sessions", "Uma sessão por linha: dados de SESSION_START e SESSION_END e contagens "
                + "de eventos da sessão.");
        t.column("participant_id", Type.TEXT, "", "Código pseudonimizado do participante", ENVELOPE)
                .column("session_id", Type.TEXT, "", "Identificador da sessão (UUID)", ENVELOPE)
                .column("session_number", Type.INTEGER, "", "Número da sessão do participante no jogo", "SESSION_START.session_number")
                .column("started_at", Type.TEXT, "ISO 8601", "Data e hora do primeiro evento da sessão", "timestamp")
                .column("ended_at", Type.TEXT, "ISO 8601", "Data e hora do fim da sessão (vazio se não houver SESSION_END)", "SESSION_END.timestamp")
                .column("duration_ms", Type.INTEGER, "ms", "Duração da sessão (ended_at menos started_at)", "derivado")
                .column("end_reason", Type.TEXT, "", "Motivo do encerramento: change_student, quit ou window_closed", "SESSION_END.end_reason")
                .column("game_in_progress_at_end", Type.BOOLEAN, "", "Havia partida em curso no encerramento", "SESSION_END.game_in_progress")
                .column("language", Type.TEXT, "", "Idioma do jogo", "SESSION_START.language")
                .column("app_version", Type.TEXT, "", "Versão do jogo", "SESSION_START.app_version")
                .column("start_level", Type.TEXT, "", "Nível inicial escolhido no cadastro: easy, intermediate ou hard", "SESSION_START.start_level")
                .column("start_complexity_level", Type.INTEGER, "", "Nível de complexidade inicial (1 a 22)", "SESSION_START.start_complexity_level")
                .column("games_started", Type.INTEGER, "", "Partidas iniciadas", "contagem de GAME_START")
                .column("games_finished", Type.INTEGER, "", "Partidas terminadas", "contagem de GAME_END")
                .column("games_won", Type.INTEGER, "", "Partidas vencidas pela criança", "GAME_END.winner = player")
                .column("turns", Type.INTEGER, "", "Rodadas iniciadas", "contagem de TURN_START")
                .column("answers", Type.INTEGER, "", "Rodadas respondidas", "contagem de ANSWER")
                .column("timeouts", Type.INTEGER, "", "Rodadas com prazo esgotado", "contagem de ANSWER_TIMEOUT")
                .column("correct_larger_answers", Type.INTEGER, "", "Respostas em que a criança escolheu o maior número", "ANSWER.correct_larger = true")
                .column("clicks_ignored", Type.INTEGER, "", "Cliques antes da liberação da resposta", "contagem de CLICK_IGNORED")
                .column("pauses", Type.INTEGER, "", "Pausas (tecla F8, menu ou, a partir do modelo 1.1, perda de foco da janela)", "contagem de PAUSE")
                .column("rewards_saved", Type.INTEGER, "", "Recompensas guardadas", "contagem de REWARD_SAVED")
                .column("characters_unlocked", Type.INTEGER, "", "Personagens desbloqueados", "contagem de CHARACTER_UNLOCKED")
                .column("events", Type.INTEGER, "", "Eventos exportados da sessão", "contagem")
                .column("missing_events", Type.INTEGER, "", "Números de sequência ausentes (eventos perdidos)", "lacunas em sequence_number")
                .column("complete", Type.BOOLEAN, "", "Sessão com SESSION_START, SESSION_END e sem lacunas", "derivado")
                .column("integrity_errors", Type.INTEGER, "", "Erros encontrados na verificação de integridade "
                        + "(detalhes em integrity.csv e relatorio-integridade.txt)", "verificação de integridade")
                .column("integrity_warnings", Type.INTEGER, "", "Avisos da verificação de integridade",
                        "verificação de integridade");
        for (SessionData s : sessions) {
            Map<String, Object> r = t.newRow();
            r.put("participant_id", s.getParticipantId());
            r.put("session_id", s.getSessionId());
            r.put("started_at", s.getEvents().get(0).parse().timestamp());
            Map<String, Integer> counts = new HashMap<String, Integer>();
            int gamesWon = 0;
            int correctLarger = 0;
            for (SourceEvent e : s.getEvents()) {
                increment(counts, e.getEventType());
                if (!(e.is("SESSION_START") || e.is("SESSION_END") || e.is("GAME_END") || e.is("ANSWER"))) {
                    continue; // os demais tipos só são contados
                }
                SourceEvent.Parsed p = e.parse();
                Map<String, Object> d = p.data();
                if (e.is("SESSION_START")) {
                    r.put("session_number", d.get("session_number"));
                    r.put("language", d.get("language"));
                    r.put("app_version", d.get("app_version"));
                    r.put("start_level", d.get("start_level"));
                    r.put("start_complexity_level", d.get("start_complexity_level"));
                } else if (e.is("SESSION_END")) {
                    r.put("ended_at", p.timestamp());
                    r.put("duration_ms", Long.valueOf(p.time().toInstant().toEpochMilli()
                            - s.getStartTime().toInstant().toEpochMilli()));
                    r.put("end_reason", d.get("end_reason"));
                    r.put("game_in_progress_at_end", d.get("game_in_progress"));
                } else if (e.is("GAME_END") && "player".equals(d.get("winner"))) {
                    gamesWon++;
                } else if (e.is("ANSWER") && Boolean.TRUE.equals(d.get("correct_larger"))) {
                    correctLarger++;
                }
            }
            r.put("games_started", count(counts, "GAME_START"));
            r.put("games_finished", count(counts, "GAME_END"));
            r.put("games_won", Integer.valueOf(gamesWon));
            r.put("turns", count(counts, "TURN_START"));
            r.put("answers", count(counts, "ANSWER"));
            r.put("timeouts", count(counts, "ANSWER_TIMEOUT"));
            r.put("correct_larger_answers", Integer.valueOf(correctLarger));
            r.put("clicks_ignored", count(counts, "CLICK_IGNORED"));
            r.put("pauses", count(counts, "PAUSE"));
            r.put("rewards_saved", count(counts, "REWARD_SAVED"));
            r.put("characters_unlocked", count(counts, "CHARACTER_UNLOCKED"));
            r.put("events", Integer.valueOf(s.getEvents().size()));
            r.put("missing_events", Integer.valueOf(s.getMissingSequenceNumbers().size()));
            r.put("complete", Boolean.valueOf(s.hasSessionStart() && s.hasSessionEnd()
                    && s.getMissingSequenceNumbers().isEmpty()));
            if (findings != null) {
                int[] c = integrity.get(s.getSessionId());
                r.put("integrity_errors", Integer.valueOf(c == null ? 0 : c[0]));
                r.put("integrity_warnings", Integer.valueOf(c == null ? 0 : c[1]));
            }
        }
        return t;
    }

    /** Uma linha por problema encontrado na verificação de integridade. */
    static Table integrity(List<IntegrityChecker.Finding> findings) {
        Table t = new Table("integrity", "Verificação de integridade: um problema por linha (regras em "
                + "relatorio-integridade.txt). Sem linhas, nenhum problema foi encontrado.");
        t.column("participant_id", Type.TEXT, "", "Código pseudonimizado do participante", ENVELOPE)
                .column("session_id", Type.TEXT, "", "Identificador da sessão (UUID)", ENVELOPE)
                .column("rule", Type.TEXT, "", "Regra verificada (R01 a R21)", "verificação de integridade")
                .column("severity", Type.TEXT, "", "error (dados incoerentes) ou warning (merece atenção, pode ter "
                        + "explicação legítima)", "verificação de integridade")
                .column("sequence_number", Type.INTEGER, "", "Evento em que o problema foi encontrado (vazio se for "
                        + "da sessão inteira)", ENVELOPE)
                .column("game_number", Type.INTEGER, "", "Partida do problema, quando houver", ENVELOPE)
                .column("turn_number", Type.INTEGER, "", "Rodada do problema, quando houver", ENVELOPE)
                .column("rule_description", Type.TEXT, "", "O que a regra exige", "verificação de integridade")
                .column("message", Type.TEXT, "", "O que foi encontrado", "verificação de integridade");
        for (IntegrityChecker.Finding f : findings) {
            Map<String, Object> r = t.newRow();
            r.put("participant_id", f.participantId);
            r.put("session_id", f.sessionId);
            r.put("rule", f.rule.id);
            r.put("severity", f.rule.severity == IntegrityChecker.Severity.ERROR ? "error" : "warning");
            r.put("sequence_number", f.sequenceNumber);
            r.put("game_number", f.gameNumber);
            r.put("turn_number", f.turnNumber);
            r.put("rule_description", f.rule.description);
            r.put("message", f.message);
        }
        return t;
    }

    static Table turns(List<SessionData> sessions) {
        Table t = new Table("turns", "Uma rodada por linha: condições da rodada (TURN_START e DIFFICULTY_ADJUSTED), "
                + "estímulos, resposta, resultado e o que aconteceu no tabuleiro.");
        t.column("participant_id", Type.TEXT, "", "Código pseudonimizado do participante", ENVELOPE)
                .column("session_id", Type.TEXT, "", "Identificador da sessão (UUID)", ENVELOPE)
                .column("game_number", Type.INTEGER, "", "Número da partida na sessão", ENVELOPE)
                .column("turn_number", Type.INTEGER, "", "Número da rodada na partida", ENVELOPE)
                .column("started_at", Type.TEXT, "ISO 8601", "Data e hora do início da rodada", "TURN_START.timestamp")
                .column("complexity_level", Type.INTEGER, "", "Nível de complexidade da rodada (1 a 22)", "TURN_START.complexity_level")
                .column("representation_dots", Type.BOOLEAN, "", "Números apresentados como pontos", "TURN_START.representation.dots")
                .column("representation_spoken", Type.BOOLEAN, "", "Números falados", "TURN_START.representation.spoken")
                .column("representation_digits", Type.BOOLEAN, "", "Números em algarismos", "TURN_START.representation.digits")
                .column("dot_fading", Type.BOOLEAN, "", "Pontos que se apagam", "TURN_START.representation.dot_fading")
                .column("dot_fading_ms", Type.INTEGER, "ms", "Tempo até os pontos se apagarem", "TURN_START.representation.dot_fading_ms")
                .column("max_value", Type.INTEGER, "", "Maior número possível no nível", "TURN_START.max_value")
                .column("addition", Type.BOOLEAN, "", "Nível com adição", "TURN_START.addition")
                .column("subtraction", Type.BOOLEAN, "", "Nível com subtração", "TURN_START.subtraction")
                .column("hazards_enabled", Type.BOOLEAN, "", "Nível que cria armadilhas", "TURN_START.hazards_enabled")
                .column("hazards_on_board", Type.INTEGER, "", "Armadilhas presentes no tabuleiro", "tamanho de TURN_START.hazards_on_board")
                .column("control_for", Type.TEXT, "", "Controle das nuvens de pontos: density ou item_size", "TURN_START.control_for")
                .column("deadline_trial", Type.BOOLEAN, "", "Rodada com prazo para responder", "TURN_START.deadline_trial")
                .column("deadline_ms", Type.INTEGER, "ms", "Prazo para responder (vazio sem prazo)", "TURN_START.deadline_ms")
                .column("difficulty_speed", Type.REAL, "0 a 1", "Dificuldade escolhida pelo algoritmo: velocidade", "DIFFICULTY_ADJUSTED.difficulty.speed")
                .column("difficulty_distance", Type.REAL, "0 a 1", "Dificuldade escolhida pelo algoritmo: distância numérica", "DIFFICULTY_ADJUSTED.difficulty.distance")
                .column("difficulty_notation", Type.REAL, "0 a 1", "Dificuldade escolhida pelo algoritmo: notação", "DIFFICULTY_ADJUSTED.difficulty.notation")
                .column("left_value", Type.INTEGER, "", "Valor do estímulo da esquerda (resultado, se for uma conta)", "STIMULUS_PRESENTED.value")
                .column("left_operator", Type.TEXT, "", "Operação da esquerda: + ou - (vazio se não houver conta)", "STIMULUS_PRESENTED.operation.operator")
                .column("left_operand_1", Type.INTEGER, "", "Primeiro número da conta da esquerda", "STIMULUS_PRESENTED.operation.operands")
                .column("left_operand_2", Type.INTEGER, "", "Segundo número da conta da esquerda", "STIMULUS_PRESENTED.operation.operands")
                .column("right_value", Type.INTEGER, "", "Valor do estímulo da direita (resultado, se for uma conta)", "STIMULUS_PRESENTED.value")
                .column("right_operator", Type.TEXT, "", "Operação da direita: + ou - (vazio se não houver conta)", "STIMULUS_PRESENTED.operation.operator")
                .column("right_operand_1", Type.INTEGER, "", "Primeiro número da conta da direita", "STIMULUS_PRESENTED.operation.operands")
                .column("right_operand_2", Type.INTEGER, "", "Segundo número da conta da direita", "STIMULUS_PRESENTED.operation.operands")
                .column("larger_side", Type.TEXT, "", "Lado com o maior valor: left, right ou equal", "derivado de left_value e right_value")
                .column("start_square_player", Type.INTEGER, "", "Casa da criança no início da rodada", "TURN_START.squares.player")
                .column("start_square_opponent", Type.INTEGER, "", "Casa do adversário no início da rodada", "TURN_START.squares.opponent")
                .column("clicks_ignored", Type.INTEGER, "", "Cliques antes da liberação da resposta", "contagem de CLICK_IGNORED")
                .column("response", Type.TEXT, "", "answer (respondeu), timeout (prazo esgotado) ou vazio (rodada interrompida)", "ANSWER ou ANSWER_TIMEOUT")
                .column("chosen_side", Type.TEXT, "", "Lado escolhido pela criança: left ou right", "ANSWER.side")
                .column("assigned_side", Type.TEXT, "", "Lado atribuído à criança quando o prazo esgota", "ANSWER_TIMEOUT.assigned_side")
                .column("correct_larger", Type.BOOLEAN, "", "Escolheu o maior número (vazio se não respondeu)", "ANSWER.correct_larger")
                .column("correct_final", Type.BOOLEAN, "", "Escolha vantajosa, considerando armadilhas e esbarrões (vazio se não respondeu)", "ANSWER.correct_final")
                .column("response_time_ms", Type.INTEGER, "ms", "Tempo de resposta desde a liberação da resposta", "ANSWER.response_time_ms")
                .column("response_time_from_first_stimulus_ms", Type.INTEGER, "ms", "Tempo de resposta desde o primeiro estímulo", "ANSWER.response_time_from_first_stimulus_ms")
                .column("turn_winner", Type.TEXT, "", "Quem ganhou a rodada: player ou opponent", "TURN_RESULT.turn_winner")
                .column("relative_net_gain", Type.INTEGER, "casas", "Avanço da criança menos o do adversário na rodada", "TURN_RESULT.outcome.relative_net_gain")
                .column("too_far_attempts", Type.INTEGER, "", "Cliques em casas além da correta durante os movimentos", "soma de MOVE.too_far_attempts")
                .column("collisions", Type.INTEGER, "", "Esbarrões", "contagem de COLLISION")
                .column("hazards", Type.INTEGER, "", "Quedas em armadilha", "contagem de HAZARD")
                .column("feedback_messages", Type.INTEGER, "", "Falas do adversário", "contagem de FEEDBACK_PRESENTED")
                .column("pauses", Type.INTEGER, "", "Pausas durante a rodada", "contagem de PAUSE")
                .column("end_square_player", Type.INTEGER, "", "Casa da criança no fim da rodada", "TURN_START, MOVE, COLLISION e HAZARD")
                .column("end_square_opponent", Type.INTEGER, "", "Casa do adversário no fim da rodada", "TURN_START, MOVE, COLLISION e HAZARD")
                .column("game_ended", Type.BOOLEAN, "", "A partida terminou nesta rodada", "GAME_END")
                .column("complete", Type.BOOLEAN, "", "Rodada com início e resultado registrados", "TURN_START e TURN_RESULT");
        for (SessionData s : sessions) {
            Map<String, Map<String, Object>> difficulty = new HashMap<String, Map<String, Object>>();
            Map<String, Map<String, Object>> rows = new LinkedHashMap<String, Map<String, Object>>();
            Map<String, int[]> squares = new HashMap<String, int[]>();
            for (SourceEvent e : s.getEvents()) {
                Integer game = e.getGameNumber();
                if (game == null) {
                    continue; // eventos fora de partida não entram na tabela de rodadas
                }
                SourceEvent.Parsed p = e.parse();
                Map<String, Object> d = p.data();
                Integer turn = e.getTurnNumber();
                if (e.is("DIFFICULTY_ADJUSTED")) {
                    difficulty.put(game + "/" + d.get("next_turn_number"), map(d.get("difficulty")));
                }
                if (turn == null) {
                    continue;
                }
                String key = game + "/" + turn;
                Map<String, Object> r = rows.get(key);
                if (r == null) {
                    r = t.newRow();
                    rows.put(key, r);
                    r.put("participant_id", e.getParticipantId());
                    r.put("session_id", e.getSessionId());
                    r.put("game_number", game);
                    r.put("turn_number", turn);
                    for (String c : new String[] { "clicks_ignored", "too_far_attempts", "collisions", "hazards",
                            "feedback_messages", "pauses" }) {
                        r.put(c, Integer.valueOf(0));
                    }
                    r.put("game_ended", Boolean.FALSE);
                    r.put("complete", Boolean.FALSE);
                    Map<String, Object> dif = difficulty.get(key);
                    if (dif != null) {
                        r.put("difficulty_speed", dif.get("speed"));
                        r.put("difficulty_distance", dif.get("distance"));
                        r.put("difficulty_notation", dif.get("notation"));
                    }
                }
                int[] sq = squares.get(key);
                String type = e.getEventType();
                if ("TURN_START".equals(type)) {
                    Map<String, Object> rep = map(d.get("representation"));
                    Map<String, Object> start = map(d.get("squares"));
                    r.put("started_at", p.timestamp());
                    r.put("complexity_level", d.get("complexity_level"));
                    r.put("representation_dots", rep.get("dots"));
                    r.put("representation_spoken", rep.get("spoken"));
                    r.put("representation_digits", rep.get("digits"));
                    r.put("dot_fading", rep.get("dot_fading"));
                    r.put("dot_fading_ms", rep.get("dot_fading_ms"));
                    r.put("max_value", d.get("max_value"));
                    r.put("addition", d.get("addition"));
                    r.put("subtraction", d.get("subtraction"));
                    r.put("hazards_enabled", d.get("hazards_enabled"));
                    Object hazards = d.get("hazards_on_board");
                    r.put("hazards_on_board", hazards instanceof List ? Integer.valueOf(((List<?>) hazards).size()) : null);
                    r.put("control_for", d.get("control_for"));
                    r.put("deadline_trial", d.get("deadline_trial"));
                    r.put("deadline_ms", d.get("deadline_ms"));
                    r.put("start_square_player", start.get("player"));
                    r.put("start_square_opponent", start.get("opponent"));
                    sq = new int[] { intValue(start.get("player")), intValue(start.get("opponent")) };
                    squares.put(key, sq);
                } else if ("STIMULUS_PRESENTED".equals(type)) {
                    String side = "right".equals(d.get("side")) ? "right" : "left";
                    r.put(side + "_value", d.get("value"));
                    Map<String, Object> op = map(d.get("operation"));
                    if (!op.isEmpty()) {
                        r.put(side + "_operator", op.get("operator"));
                        Object operands = op.get("operands");
                        if (operands instanceof List && ((List<?>) operands).size() == 2) {
                            r.put(side + "_operand_1", ((List<?>) operands).get(0));
                            r.put(side + "_operand_2", ((List<?>) operands).get(1));
                        }
                    }
                    if (r.get("left_value") instanceof Number && r.get("right_value") instanceof Number) {
                        long left = ((Number) r.get("left_value")).longValue();
                        long right = ((Number) r.get("right_value")).longValue();
                        r.put("larger_side", left > right ? "left" : left < right ? "right" : "equal");
                    }
                } else if ("CLICK_IGNORED".equals(type)) {
                    add(r, "clicks_ignored", 1);
                } else if ("ANSWER".equals(type)) {
                    r.put("response", "answer");
                    r.put("chosen_side", d.get("side"));
                    r.put("correct_larger", d.get("correct_larger"));
                    r.put("correct_final", d.get("correct_final"));
                    r.put("response_time_ms", d.get("response_time_ms"));
                    r.put("response_time_from_first_stimulus_ms", d.get("response_time_from_first_stimulus_ms"));
                } else if ("ANSWER_TIMEOUT".equals(type)) {
                    r.put("response", "timeout");
                    r.put("assigned_side", d.get("assigned_side"));
                } else if ("TURN_RESULT".equals(type)) {
                    r.put("turn_winner", d.get("turn_winner"));
                    r.put("relative_net_gain", map(d.get("outcome")).get("relative_net_gain"));
                    r.put("complete", Boolean.valueOf(r.get("started_at") != null));
                } else if ("MOVE".equals(type)) {
                    add(r, "too_far_attempts", intValue(d.get("too_far_attempts")));
                    move(sq, d.get("piece"), d.get("to_square"));
                } else if ("COLLISION".equals(type)) {
                    add(r, "collisions", 1);
                    move(sq, d.get("pushed_piece"), d.get("to_square"));
                } else if ("HAZARD".equals(type)) {
                    add(r, "hazards", 1);
                    move(sq, d.get("piece"), d.get("to_square"));
                } else if ("FEEDBACK_PRESENTED".equals(type)) {
                    add(r, "feedback_messages", 1);
                } else if ("PAUSE".equals(type)) {
                    add(r, "pauses", 1);
                } else if ("GAME_END".equals(type)) {
                    r.put("game_ended", Boolean.TRUE);
                }
                if (sq != null) {
                    r.put("end_square_player", Integer.valueOf(sq[0]));
                    r.put("end_square_opponent", Integer.valueOf(sq[1]));
                }
            }
        }
        return t;
    }

    private static void move(int[] sq, Object piece, Object toSquare) {
        if (sq == null || !(toSquare instanceof Number)) {
            return;
        }
        sq["opponent".equals(piece) ? 1 : 0] = ((Number) toSquare).intValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object v) {
        return v instanceof Map ? (Map<String, Object>) v : new HashMap<String, Object>();
    }

    private static int intValue(Object v) {
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    private static void add(Map<String, Object> row, String column, int n) {
        row.put(column, Integer.valueOf(intValue(row.get(column)) + n));
    }

    private static void increment(Map<String, Integer> counts, String key) {
        Integer n = counts.get(key);
        counts.put(key, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
    }

    private static Integer count(Map<String, Integer> counts, String key) {
        Integer n = counts.get(key);
        return n == null ? Integer.valueOf(0) : n;
    }
}
