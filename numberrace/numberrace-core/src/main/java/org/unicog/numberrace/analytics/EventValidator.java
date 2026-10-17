package org.unicog.numberrace.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validação de um evento antes da gravação: campos obrigatórios da parte comum, contexto de
 * partida e rodada conforme o tipo, campos obrigatórios e permitidos da parte específica e
 * durações não negativas. A conformidade completa com o JSON Schema é verificada nos testes.
 */
public final class EventValidator {

    private static final Pattern PARTICIPANT = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private static final Map<EventType, Set<String>> REQUIRED = new EnumMap<EventType, Set<String>>(EventType.class);
    private static final Map<EventType, Set<String>> OPTIONAL = new EnumMap<EventType, Set<String>>(EventType.class);

    static {
        req(EventType.SESSION_START, "session_number", "start_level", "start_complexity_level", "language", "app_version");
        opt(EventType.SESSION_START, "display_mode");
        req(EventType.SESSION_END, "end_reason", "screen", "game_in_progress");
        req(EventType.SCREEN_CHANGED, "from_screen", "to_screen");
        req(EventType.THEME_SELECTED, "theme");
        req(EventType.CHARACTER_SELECTED, "player_character", "opponent_character");
        req(EventType.PAUSE, "trigger", "screen");
        req(EventType.RESUME, "trigger", "screen");
        req(EventType.GAME_START, "board_length", "complexity_level");
        req(EventType.GAME_END, "winner", "turns_played", "final_squares");
        req(EventType.DIFFICULTY_ADJUSTED, "next_turn_number", "difficulty", "mean_success", "desired_difficulty",
                "estimated_difficulty", "coords_chosen");
        req(EventType.TURN_START, "complexity_level", "representation", "max_value", "addition", "subtraction",
                "hazards_enabled", "hazards_on_board", "deadline_trial", "deadline_ms", "control_for", "squares");
        req(EventType.STIMULUS_PRESENTED, "side", "value", "operation");
        req(EventType.RESPONSE_ENABLED);
        req(EventType.CLICK_IGNORED, "side");
        req(EventType.ANSWER, "side", "response_time_ms", "response_time_from_first_stimulus_ms", "correct_larger",
                "correct_final");
        req(EventType.ANSWER_TIMEOUT, "deadline_ms", "assigned_side");
        req(EventType.TURN_RESULT, "turn_winner", "outcome", "hypothetical_outcome");
        req(EventType.MOVE, "piece", "from_square", "to_square", "steps", "too_far_attempts");
        req(EventType.COLLISION, "pushed_piece", "from_square", "to_square");
        req(EventType.HAZARD, "piece", "square", "penalty", "to_square");
        req(EventType.FEEDBACK_PRESENTED, "message_key");
        opt(EventType.FEEDBACK_PRESENTED, "sound_key");
        req(EventType.REWARD_SAVED, "reward_type", "theme");
        req(EventType.CHARACTER_UNLOCKED, "character", "theme");
    }

    private static void req(EventType t, String... keys) {
        REQUIRED.put(t, new HashSet<String>(Arrays.asList(keys)));
    }

    private static void opt(EventType t, String... keys) {
        OPTIONAL.put(t, new HashSet<String>(Arrays.asList(keys)));
    }

    private EventValidator() {
    }

    /** Lista os problemas encontrados; lista vazia significa evento válido. */
    public static List<String> validate(InteractionEvent e) {
        List<String> problems = new ArrayList<String>();
        if (e.getEventType() == null) {
            problems.add("event_type ausente");
            return problems;
        }
        if (e.getEventId() == null || e.getSessionId() == null || e.getTime() == null) {
            problems.add("event_id, session_id ou timestamp ausente");
        }
        if (e.getParticipantId() == null || !PARTICIPANT.matcher(e.getParticipantId()).matches()) {
            problems.add("participant_id inválido: " + e.getParticipantId());
        }
        if (e.getSequenceNumber() < 1) {
            problems.add("sequence_number menor que 1");
        }
        EventType.Scope scope = e.getEventType().getScope();
        if (scope != EventType.Scope.SESSION && (e.getGameNumber() == null || e.getActivity() == null)) {
            problems.add(e.getEventType() + " exige game_number e activity");
        }
        if (scope == EventType.Scope.TURN && e.getTurnNumber() == null) {
            problems.add(e.getEventType() + " exige turn_number");
        }
        if (e.getTurnNumber() != null && e.getGameNumber() == null) {
            problems.add("turn_number sem game_number");
        }
        Set<String> required = REQUIRED.get(e.getEventType());
        Set<String> optional = OPTIONAL.containsKey(e.getEventType()) ? OPTIONAL.get(e.getEventType())
                : Collections.<String> emptySet();
        Map<String, Object> data = e.getData();
        for (String key : required) {
            if (!data.containsKey(key)) {
                problems.add("campo obrigatório ausente em data: " + key);
            }
        }
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (!required.contains(key) && !optional.contains(key)) {
                problems.add("campo não previsto em data: " + key);
            }
            Object v = entry.getValue();
            if (key.endsWith("_ms") && v instanceof Number && ((Number) v).longValue() < 0) {
                problems.add(key + " negativo");
            }
        }
        return problems;
    }
}
