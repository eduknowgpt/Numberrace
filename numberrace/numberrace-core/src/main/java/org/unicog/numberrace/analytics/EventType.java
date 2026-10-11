package org.unicog.numberrace.analytics;

/**
 * Tipos de evento do modelo de eventos de interação 1.1 (evolution/pt/modelo-eventos.md).
 * O escopo indica quais campos de contexto a parte comum do evento deve ter.
 */
public enum EventType {
    SESSION_START(Scope.SESSION),
    SESSION_END(Scope.SESSION),
    SCREEN_CHANGED(Scope.SESSION),
    THEME_SELECTED(Scope.SESSION),
    CHARACTER_SELECTED(Scope.SESSION),
    PAUSE(Scope.SESSION),
    RESUME(Scope.SESSION),
    GAME_START(Scope.GAME),
    GAME_END(Scope.GAME),
    DIFFICULTY_ADJUSTED(Scope.GAME),
    TURN_START(Scope.TURN),
    STIMULUS_PRESENTED(Scope.TURN),
    RESPONSE_ENABLED(Scope.TURN),
    CLICK_IGNORED(Scope.TURN),
    ANSWER(Scope.TURN),
    ANSWER_TIMEOUT(Scope.TURN),
    TURN_RESULT(Scope.TURN),
    MOVE(Scope.TURN),
    COLLISION(Scope.TURN),
    HAZARD(Scope.TURN),
    FEEDBACK_PRESENTED(Scope.TURN),
    REWARD_SAVED(Scope.SESSION),
    CHARACTER_UNLOCKED(Scope.SESSION);

    /** SESSION: partida e rodada opcionais; GAME: partida obrigatória; TURN: partida e rodada obrigatórias. */
    public enum Scope {
        SESSION, GAME, TURN
    }

    private final Scope scope;

    EventType(Scope scope) {
        this.scope = scope;
    }

    public Scope getScope() {
        return scope;
    }
}
