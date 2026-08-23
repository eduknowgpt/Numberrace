//The Number Race: Remediation Software for dyscalculia.
//Copyright (C) Anna Wilson and Stanlislas Dehaene, 2004
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

//This class contains general constants used throughout the game by several classes

package org.unicog.numberrace.util;

import static com.threerings.media.AbstractMedia.HUD_LAYER;

public class Constants {
    public static final byte PLAYER1 = 0;
    public static final byte PLAYER2 = 1;
    public static final int LEFT = 0; //constant for left
    public static final int RIGHT = 1; //constant for right
    public static final byte TRUE = 1;
    public static final byte FALSE = 0;
    public static final byte NO_RESPONSE = 2;
    public static final int FIXED_DENSITY = 0;
    public static final int FIXED_ITEM_SIZE = 1;
    public static final String sides[] = { "left", "right", "null" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    public static final String correct[] = { "false", "true", "no_resp" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    public static int LAST_SQUARE = 10;

    public static final int DOTCARPET_LAYER = -1;
    public static final int DOT_CONTAINER_LAYER = -1;
    public static final int BOARD_LAYER = 0;
    public static final int BOARDANIM_LAYER = 1;
    public static final int MARKERS_LAYER = 2;
    public static final int HAZARD_LAYER = 3;
    public static final int HAZARD_APPEARANCE_ANIMATION_LAYER = 4;
    public static final int OPPONENT_LAYER = 10;
    public static final int PLAYER_LAYER = 11;
    public static final int DOTS_LAYER = 12;
    public static final int BOARD_MOVEMENT_LAYER = 13;

    public static final int BOTTOM_LAYER = HUD_LAYER + 1;
    public static final int MEBUBTN_LAYER = HUD_LAYER + 5;
    public static final int CHOICE_SCRBNT_LAYER = HUD_LAYER + 5;

    public static final int STAMP_MOVEMENT_LAYER = BOTTOM_LAYER + 10;

    public static final int NUMBER_CHARACTERS = 6;
    public static final int NUMBER_POSS_REWARDS = 7;
    public static final int NUM_PLAYERS = 2;
}