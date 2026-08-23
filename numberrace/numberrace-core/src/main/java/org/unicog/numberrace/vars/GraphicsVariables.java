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

//Graphical variables (will only be altered by screenSize)
//Eventually make into a data structure so that variables can be loaded up for different screen sizes
//Need to include filenames of custom sized gif images for each possible resolution also

package org.unicog.numberrace.vars;

import java.awt.*;

public class GraphicsVariables {
    //game variables
    public static final int DISPLAY_WIDTH = 1024;
    public static final int DISPLAY_HEIGHT = 768;

    //player icons
    public static final Point TOP_CENTER_SCREEN = new Point(512, 320);
    public static final Point PLAYER1_ICON_START_POINT = new Point(192, 80);
    public static final Point PLAYER2_ICON_START_POINT = new Point(192, 176);
    public static final Point BETWEEN_DOT_CONTAINERS = new Point(512, 512);

    public static final int OFFSET_Y = 10;
    public static final int OFFSET_X = 10;

    public static final int DOT_CONT_WIDTH = 450;
    public static final int STAMP_WIDTH = 75;

    //dot carpet
    public static final int DOT_CARPET_LINEWIDTH = 4;

    //dot container
    public static final int DOT_CONT_HEIGHT = 427;
    public static final int DOT_CONT_FONT_SIZE = 96;
    public static final int RIGHT_DOT_CONT_X = 558;
    public static final int LEFT_DOT_CONT_IMG_X = 0;
    public static final int LEFT_DOT_CONT_IMG_Y = 234;
    public static final int RIGHT_DOT_CONT_IMG_X = 256;
    public static final int RIGHT_DOT_CONT_IMG_Y = 234;
    public static final int DOT_CONT_IMG_DIAM = 192;
    public static final int LEFT_DOT_CONT_DIGIT_X = 48; //we are drawing from the center point
    public static final int LEFT_DOT_CONT_DIGIT_Y = 48;
    public static final int LEFT_DOT_CONT_MULTIDIGIT_X = 268; //352
    public static final int LEFT_DOT_CONT_MULTIDIGIT_Y = 48;
    public static final int RIGHT_DOT_CONT_DIGIT_X = 400;
    public static final int RIGHT_DOT_CONT_DIGIT_Y = 48;
    public static final int RIGHT_DOT_CONT_MULTIDIGIT_X = 180; //96
    public static final int RIGHT_DOT_CONT_MULTIDIGIT_Y = 48;
    public static final int LEFT_DOT_CONT_STACK_START_POINT_X = 190;
    public static final int LEFT_DOT_CONT_STACK_START_POINT_Y = 402;
    public static final int RIGHT_DOT_CONT_STACK_START_POINT_X = DOT_CONT_WIDTH
            - LEFT_DOT_CONT_STACK_START_POINT_X;
    public static final int RIGHT_DOT_CONT_STACK_START_POINT_Y = 402;

    //dot array
    public static final int DOT_ARRAY_DIAMETER = 320;
    public static final int LEFT_DOT_ARRAY_X = 96;
    public static final int LEFT_DOT_ARRAY_Y = 64;
    public static final int RIGHT_DOT_ARRAY_X = 32;
    public static final int RIGHT_DOT_ARRAY_Y = 64;
    public static final int SUB_DOT_ARRAY_DIAM = 256;
    public static final int LEFTDC_LEFT_SUB_DOT_ARRAY_X = 192;
    public static final int LEFTDC_LEFT_SUB_DOT_ARRAY_Y = 170;
    public static final int LEFTDC_RIGHT_SUB_DOT_ARRAY_X = 0;
    public static final int LEFTDC_RIGHT_SUB_DOT_ARRAY_Y = 0;
    public static final int RIGHTDC_LEFT_SUB_DOT_ARRAY_X = 0;
    public static final int RIGHTDC_LEFT_SUB_DOT_ARRAY_Y = 170;
    public static final int RIGHTDC_RIGHT_SUB_DOT_ARRAY_X = 192;
    public static final int RIGHTDC_RIGHT_SUB_DOT_ARRAY_Y = 0;

    //buttons
    public static final int CHOICE_SCREEN_BUTTON_X = 960;
    public static final int CHOICE_SCREEN_BUTTON_Y = 64;
    public static final int HELP_BUTTON_X = 960;
    public static final int HELP_BUTTON_Y = 160;
    public static final int MENU_BUTTON_X = 64;
    public static final int MENU_BUTTON_Y = 64;
    public static final int SCREENSWITCH_BUTTON_X = 314;
    public static final int SCREENSWITCH_BUTTON_Y = 674;
    public static final int SCREENSWITCH_BUTTON_WIDTH = 120;
    public static final int SCREENSWITCH_BUTTON_HEIGHT = 69;

    //debugging labels
    public static final int LBL_DEBUG_ALG_X = 512;
    public static final int LBL_DEBUG_ALG_Y = 256;
    public static final int LBL_TRANS_X = 512;
    public static final int LBL_TRANS_Y = 750;

    //boardScreen:
    public static final int SQUARES_Y = 570;

    //playing pieces
    public static final int PLAYER1_START_X = 192;
    public static final int PLAYER1_START_Y = 80;
    public static final int PLAYER2_START_X = 192;
    public static final int PLAYER2_START_Y = 176;

    //number board
    public static final int NUMBER_BOARD_X = 32;
    public static final int NUMBER_BOARD_Y = 256;
    public static final int NUMBER_BOARD_WIDTH = 960;
    public static final int NUMBER_BOARD_HEIGHT = 512;
    public static final int NUMBER_BOARD_SQUARE_WIDTH = 80;
    public static final int NUMBER_BOARD_LINE_WIDTH = 3;
    public static final int NUMBER_BOARD_ARROW_SIZE = 15;
    // public static final int NUMBER_BOARD_INTER_LINE_DIST = 32;

    //start/end images		note: these are in board coords
    public static final int START_IMAGE_X = 50;
    public static final int START_IMAGE_Y = 55;
    public static final int FINISH_IMAGE_X = 910;
    public static final int FINISH_IMAGE_Y = 460;

    //regScreen:
    public static final int TITLE_LABEL_X = 512;
    public static final int TITLE_LABEL_Y = 64;
    public static final int TITLE_LABEL_WIDTH = 832;
    public static final int TITLE_LABEL_HEIGHT = 64;
    public static final int BUTTONS_X = 96;
    public static final int BUTTONS_Y = 544;
    public static final int BUTTON_X_INTERSPACE = 192;
    public static final int BUTTON_Y_INTERSPACE = 96;
    public static final int ENTER_BUTTON_X = 512;
    public static final int ENTER_BUTTON_Y = 512;
    public static final int CANCEL_BUTTON_X = 512;
    public static final int CANCEL_BUTTON_Y = 608;
    public static final int DELETE_BUTTON_X = 768;
    public static final int DELETE_BUTTON_Y = 512;
    public static final int BUTTON_WIDTH = 160;
    public static final int BUTTON_HEIGHT = 64;
    public static final int TEXT_FIELDS_X = 608;
    public static final int TEXT_FIELDS_Y = 128;
    public static final int TEXT_FIELDS_INTERSPACE = 64;
    public static final int TEXT_FIELD_WIDTH = 320;
    public static final int TEXT_FIELD_HEIGHT = 32;
    public static final int FIELD_LABELS_X = 512;
    public static final int FIELD_LABELS_Y = 128;
    public static final int FIELD_LABELS_WIDTH = 128;
    public static final int FIELD_LABELS_HEIGHT = 32;
    public static final int LAST_SESSION_LABEL_WIDTH = 448;
    public static final int STUDENT_LIST_X = 96;
    public static final int STUDENT_LIST_Y = 128;
    public static final int STUDENT_LIST_WIDTH = 352;
    public static final int STUDENT_LIST_HEIGHT = 384;

    //themeChoice Screen:
    public static final Point THEME_TITLE_LOC = new Point(512, 100);
    public static final Point THEME_1_LOC = new Point(512, 300);
    public static final Point THEME_2_LOC = new Point(512, 600);

    //characChoice Screen:
    public static final Point CHARAC_1_BUTTON_LOC = new Point(200, 230);
    public static final Point CHARAC_2_BUTTON_LOC = new Point(200, 380);
    public static final Point CHARAC_3_BUTTON_LOC = new Point(200, 530);
    public static final Point CHARAC_4_BUTTON_LOC = new Point(600, 230);
    public static final Point CHARAC_5_BUTTON_LOC = new Point(600, 380);
    public static final Point CHARAC_6_BUTTON_LOC = new Point(600, 530);
    public static final Point CHARAC_CHOICE_TITLE_LOC = new Point(512, 110);
    public static final Point CHARAC_1_TITLE_LOC = new Point(270, 200);
    public static final Point CHARAC_2_TITLE_LOC = new Point(270, 350);
    public static final Point CHARAC_3_TITLE_LOC = new Point(270, 500);
    public static final Point CHARAC_4_TITLE_LOC = new Point(670, 200);
    public static final Point CHARAC_5_TITLE_LOC = new Point(670, 350);
    public static final Point CHARAC_6_TITLE_LOC = new Point(670, 500);

    //reward Screen:
    public static final int REWARD_VIEW_BUTTON_X = 675;
    public static final int REWARD_VIEW_BUTTON_Y = 675;
    public static final int REWARD_VIEW_SPRITE_MAXY = 550;
    public static final int REWARD_VIEW_SPRITE_MINY = 73;
    public static final int REWARD_VIEW_SPRITE_MAXX = 1024;
    public static final int REWARD_VIEW_SPRITE_MINX = 0;
    public static final int REWARD_CHOICE_SPRITE_MAXY = 600;
    public static final int REWARD_CHOICE_SPRITE_MINY = 350;
    public static final int REWARD_CHOICE_SPRITE_MAXX = 650;
    public static final int REWARD_CHOICE_SPRITE_MINX = 250;
}