package org.unicog.numberrace.screens;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public enum ActionState {
    BEGINNING, TURN_START, SNEAK, CHOOSE, CHOICE_MADE, WAIT4_PLAYER_MOVE, PLAYER_MOVE, WAIT4_OPPONENT_MOVE, OPPONENT_MOVE, NEXTTURN, WAIT4NEXTTURN, BOARD_MOVEMENTS;
    Set<ActionState> next;

    static {
        BEGINNING.nexts(WAIT4NEXTTURN);
        TURN_START.nexts(SNEAK, CHOOSE);
        SNEAK.nexts(CHOICE_MADE);
        CHOOSE.nexts(CHOICE_MADE);
        CHOICE_MADE.nexts(WAIT4_PLAYER_MOVE);
        WAIT4_PLAYER_MOVE.nexts(PLAYER_MOVE);
        PLAYER_MOVE.nexts(BOARD_MOVEMENTS);
        WAIT4_OPPONENT_MOVE.nexts(OPPONENT_MOVE);
        OPPONENT_MOVE.nexts(BOARD_MOVEMENTS);
        WAIT4NEXTTURN.nexts(NEXTTURN);
        NEXTTURN.nexts(TURN_START);
        BOARD_MOVEMENTS.nexts(WAIT4_OPPONENT_MOVE,
                              BOARD_MOVEMENTS,
                              WAIT4NEXTTURN);
    }

    private void nexts(ActionState... nexts) {
        this.next = EnumSet.copyOf(Arrays.asList(nexts));
    }

    public boolean canChangeTo(ActionState state) {
        return next.contains(state);
    }
}
