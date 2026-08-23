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

//Theme-related variables

package org.unicog.numberrace.vars;

import static org.unicog.numberrace.screens.ScaleUtils.*;

import java.awt.Color;
import java.awt.Font;

import org.unicog.numberrace.util.Messages;
import org.unicog.numberrace.util.Resources;

public class ThemeVariables {
    //constants
    public static final byte DEFAULT_THEME = 0;
    public static final byte UNDER_THE_SEA = 0;
    public static final byte IN_THE_JUNGLE = 1;
    public static final byte NUMBER_OF_THEMES = 2;

    //Current level
    private static byte currentLevel;

    //Colors
    public static Color screenColor; //color of choiceScreen and boardScreen background
    public Color dotContColor; //color of dot Container background (ie. square that chest, tree is in)
    public static Color boardColor; //color of numberBoard
    public Color dotColor; //color of dots
    public Color dotContDigitColor; //color of digits on dot container
    public Color bubbleTextColor; // color of the text in bubble
    public Color boardLineColor; //color of lines on number board
    public Color lblDebugAlgColor; //debugging label color
    public Color lblTransColor; //color of translation text
    public Color dotCarpetBackColor; //color of dot carpet background
    public Color dotCarpetForeColor; //color of dot carpet foreground
    public Color dotCarpetTextColor;
    public Color textColor; //color of text

    //Fonts
    public Font lblDebugAlgFont;
    public Font lblTransFont;
    public Font dotContDigitFont;
    public Font boardNumberFont;
    public Font bubbleFont;

    public Font buttonTextFont;
    public Font regScreenTitleFont;
    public Font regScreenTextFont;
    public Font characChoiceTitleFont;
    public Font characChoiceLabelFont;

    //Strings character names
    public String charac1Label;
    public String charac2Label;
    public String charac3Label;
    public String charac4Label;
    public String charac5Label;
    public String charac6Label;

    public String[] charactLabel = new String[6];

    //sounds
    public final String[] player1CharacNoises = { "dolphinSqueak",
            "dolphinClick", "orcaSplash", "whale1Bubble", "whale2Bubble",
            "whale3Bubble" };
    public final String[] player2CharacNoises = { "crab2Click", "crab3Click",
            "puffFish", "squidSquirt" };

    //graphics: gameScreens
    public final String[] leftDotContainerGifs = {
            Resources.getString("leftContainerClosed"),
            Resources.getString("leftContainerOpened") };
    public final String[] rightDotContainerGifs = {
            Resources.getString("rightContainerClosed"),
            Resources.getString("rightContainerOpened") };

    public static final String[][] hazardGifs = {
            { Resources.getString("hazard1a"), Resources.getString("hazard1b") },
            { Resources.getString("hazard2a"), Resources.getString("hazard2b") },
            { Resources.getString("hazard3a"), Resources.getString("hazard3b") }, };

    public static final String[][] player1CharacterFiles = {
            { Resources.getString("player_char_1a"),
                    Resources.getString("player_char_1b") },
            { Resources.getString("player_char_2a"),
                    Resources.getString("player_char_2b") },
            { Resources.getString("player_char_3a"),
                    Resources.getString("player_char_3b") },
            { Resources.getString("player_char_4a"),
                    Resources.getString("player_char_4b") },
            { Resources.getString("player_char_5a"),
                    Resources.getString("player_char_5b") },
            { Resources.getString("player_char_6a"),
                    Resources.getString("player_char_6b") } };
    public static final String[][] player2CharacterFiles = {
            { Resources.getString("enemy_char_1a"),
                    Resources.getString("enemy_char_1b") },
            { Resources.getString("enemy_char_2a"),
                    Resources.getString("enemy_char_2b") },
            { Resources.getString("enemy_char_3a"),
                    Resources.getString("enemy_char_3b") },
            { Resources.getString("enemy_char_4a"),
                    Resources.getString("enemy_char_4b") } };

    public final String choiceButton = Resources.getString("choiceButton");
    public final String helpButton = Resources.getString("helpButton");
    public final String boardButton = Resources.getString("boardButton");
    public static final String menuButton = Resources.getString("menuButton");
    public final String cheatKey = Resources.getString("cheatKey");
    public final String startMarker = Resources.getString("startMarker");
    public final String finishMarker = Resources.getString("finishMarker");
    public final String bottomImage = Resources.getString("bottomImage");

    //graphics: instruction screen
    public final String screenSwitchButton = Resources.getString("scrSwitchButton");
    public final String instrucScreenBackground = Resources.getString("THEME_BACKGROUND_IMAGE");
    //    public String instrucScreenInstrucs;

    //graphics: game over screen
    public final String gameOverScreenBkgrd = Resources.getString("GAMEOVER_THEME_BACKGROUND_IMAGE");

    //graphics: reward screen
    public final String rewardScreenBkgrd = Resources.getString("REWARD_BACKGROUND");
    public final String rewardViewScreenBkgrd = Resources.getString("REWARDVIEW_BACKGROUND");
    public final String[] reward1 = { Resources.getString("reward1_left"),
            Resources.getString("reward1_right") };
    public final String[] reward2 = { Resources.getString("reward2_left"),
            Resources.getString("reward2_right") };
    public final String[] reward3 = { Resources.getString("reward3_left"),
            Resources.getString("reward3_right") };
    public final String[] reward4 = { Resources.getString("reward4_left"),
            Resources.getString("reward4_right") };
    public final String[] reward5 = { Resources.getString("reward5_left"),
            Resources.getString("reward5_right") };
    public final String[] reward6 = { Resources.getString("reward6_left"),
            Resources.getString("reward6_right") };
    public final String[] reward7 = { Resources.getString("reward7_left"),
            Resources.getString("reward7_right") };

    //graphics: characChoice Screen
    public final String characChoiceBackground = Resources.getString("CHARACTER_CHOICE_BACKGROUND");
    public final String[] charac_1_files = { Resources.getString("charac_1a"),
            Resources.getString("charac_1b") };
    public final String[] charac_2_files = { Resources.getString("charac_2a"),
            Resources.getString("charac_2b") };
    public final String[] charac_3_files = { Resources.getString("charac_3a"),
            Resources.getString("charac_3b") };
    public final String[] charac_4_files = { Resources.getString("charac_4a"),
            Resources.getString("charac_4b") };
    public final String[] charac_5_files = { Resources.getString("charac_5a"),
            Resources.getString("charac_5b") };
    public final String[] charac_6_files = { Resources.getString("charac_6a"),
            Resources.getString("charac_6b") };

    public ThemeVariables(byte level) {

        //instantiate array objects
        //        player1CharacNoises = new String[6];
        //        player2CharacNoises = new String[4];
        //        leftDotContainerGifs = new String[2];
        //        rightDotContainerGifs = new String[2];
        //		hazardGifs = new String[3][2];
        //        player1CharacterFiles = new String[6][2];
        //        player2CharacterFiles = new String[4][2];
        //		reward1 = new String[2];
        //		reward2 = new String[2];
        //		reward3 = new String[2];
        //		reward4 = new String[2];
        //		reward5 = new String[2];
        //		reward6 = new String[2];
        //		reward7 = new String[2];

        //gif files: characChoice Screen
        //        charac_1_files = new String[3];
        //        charac_2_files = new String[3];
        //        charac_3_files = new String[3];
        //        charac_4_files = new String[3];
        //        charac_5_files = new String[3];
        //        charac_6_files = new String[3];

        setLevel(level);
    }

    public boolean setLevel(byte levelName) {
        if (levelName == UNDER_THE_SEA) {
            currentLevel = levelName;

            //Set colors
            screenColor = new Color(0, 46, 152);
            dotContColor = new Color(0, 176, 229);
            ThemeVariables.boardColor = dotContColor;
            dotColor = new Color(255, 254, 1);
            dotContDigitColor = Color.BLACK;
            bubbleTextColor = Color.RED;
            boardLineColor = new Color(136, 240, 253);
            lblDebugAlgColor = new Color(255, 255, 255);
            lblTransColor = new Color(255, 255, 255);
            dotCarpetBackColor = new Color(102, 29, 156);
            dotCarpetForeColor = new Color(255, 254, 1);
            dotCarpetTextColor = Color.WHITE;
            textColor = Color.BLACK;

            //Fonts
            lblDebugAlgFont = new Font("Courier", Font.PLAIN, 12);
            lblTransFont = new Font("Arial Black", Font.PLAIN, i(18));
            dotContDigitFont = new Font("Arial Black", Font.PLAIN,
                    i(GraphicsVariables.DOT_CONT_FONT_SIZE));
            boardNumberFont = new Font("Arial Black", Font.PLAIN, i(40));
            bubbleFont = new Font("Arial Black", Font.PLAIN, i(36));
            buttonTextFont = new Font("Arial Black", Font.PLAIN, i(14));
            regScreenTitleFont = new Font("Arial Black", Font.PLAIN, i(40));
            regScreenTextFont = new Font("Arial Black", Font.PLAIN, i(14));
            characChoiceTitleFont = new Font("Arial Black", Font.PLAIN, i(40));
            characChoiceLabelFont = new Font("Arial Black", Font.PLAIN, i(24));

            //Strings character names
            charactLabel[0] = charac1Label = Messages.getString("LangVars.THEME0CHARAC1");
            charactLabel[1] = charac2Label = Messages.getString("LangVars.THEME0CHARAC2");
            charactLabel[2] = charac3Label = Messages.getString("LangVars.THEME0CHARAC3");
            charactLabel[3] = charac4Label = Messages.getString("LangVars.THEME0CHARAC4");
            charactLabel[4] = charac5Label = Messages.getString("LangVars.THEME0CHARAC5");
            charactLabel[5] = charac6Label = Messages.getString("LangVars.THEME0CHARAC6");

            //FileNames - sounds
            //            player1CharacNoises[0] = "dolphinSqueak";
            //            player1CharacNoises[1] = "dolphinClick";
            //            player1CharacNoises[2] = "orcaSplash";
            //            player1CharacNoises[3] = "whale1Bubble";
            //            player1CharacNoises[4] = "whale2Bubble";
            //            player1CharacNoises[5] = "whale3Bubble";
            //            player2CharacNoises[0] = "crab2Click";
            //            player2CharacNoises[1] = "crab3Click";
            //            player2CharacNoises[2] = "puffFish";
            //            player2CharacNoises[3] = "squidSquirt";

            //Graphics gamescreens
            //            leftDotContainerGifs[0] = "resources/images/underWater/gameScreens/leftChestClosed192.gif";
            //            leftDotContainerGifs[1] = "resources/images/underWater/gameScreens/leftChestOpen192.gif";
            //            rightDotContainerGifs[0] = "resources/images/underWater/gameScreens/rightChestClosed192.gif";
            //            rightDotContainerGifs[1] = "resources/images/underWater/gameScreens/rightChestOpen192.gif";

            //			hazardGifs[0][0] = "resources/images/underWater/gameScreens/anemone1A.gif";
            //			hazardGifs[0][1] = "resources/images/underWater/gameScreens/anemone1B.gif";
            //			hazardGifs[1][0] = "resources/images/underWater/gameScreens/anemone2A.gif";
            //			hazardGifs[1][1] = "resources/images/underWater/gameScreens/anemone2B.gif";
            //			hazardGifs[2][0] = "resources/images/underWater/gameScreens/anemone3A.gif";
            //			hazardGifs[2][1] = "resources/images/underWater/gameScreens/anemone3B.gif";
            //            player1CharacterFiles[0][0] = "resources/images/underWater/gameScreens/dolphin8A.gif";
            //            player1CharacterFiles[0][1] = "resources/images/underWater/gameScreens/dolphin8B.gif";
            //            player1CharacterFiles[1][0] = "resources/images/underWater/gameScreens/dolphin6A.gif";
            //            player1CharacterFiles[1][1] = "resources/images/underWater/gameScreens/dolphin6B.gif";
            //            player1CharacterFiles[2][0] = "resources/images/underWater/gameScreens/orcaA.gif";
            //            player1CharacterFiles[2][1] = "resources/images/underWater/gameScreens/orcaB.gif";
            //            player1CharacterFiles[3][0] = "resources/images/underWater/gameScreens/whale1A.gif";
            //            player1CharacterFiles[3][1] = "resources/images/underWater/gameScreens/whale1B.gif";
            //            player1CharacterFiles[4][0] = "resources/images/underWater/gameScreens/whale2A.gif";
            //            player1CharacterFiles[4][1] = "resources/images/underWater/gameScreens/whale2B.gif";
            //            player1CharacterFiles[5][0] = "resources/images/underWater/gameScreens/whale3A.gif";
            //            player1CharacterFiles[5][1] = "resources/images/underWater/gameScreens/whale3B.gif";

            //            player2CharacterFiles[0][0] = "resources/images/underWater/gameScreens/crab2A.gif";
            //            player2CharacterFiles[0][1] = "resources/images/underWater/gameScreens/crab2B.gif";
            //            player2CharacterFiles[1][0] = "resources/images/underWater/gameScreens/squidA.gif";
            //            player2CharacterFiles[1][1] = "resources/images/underWater/gameScreens/squidB.gif";
            //            player2CharacterFiles[2][0] = "resources/images/underWater/gameScreens/crab3A.gif";
            //            player2CharacterFiles[2][1] = "resources/images/underWater/gameScreens/crab3B.gif";
            //            player2CharacterFiles[3][0] = "resources/images/underWater/gameScreens/puff_fishA.gif";
            //            player2CharacterFiles[3][1] = "resources/images/underWater/gameScreens/puff_fishB.gif";
            //            choiceButton = "resources/images/underWater/gameScreens/choiceButton.gif";
            //            helpButton = "resources/images/underWater/gameScreens/helpButton.gif";
            //            boardButton = "resources/images/underWater/gameScreens/boardButton.gif";
            //            menuButton = "resources/images/underWater/gameScreens/menuButton.gif";
            //			cheatKey = "resources/images/underWater/gameScreens/key.gif";
            //            startMarker = "resources/images/underWater/gameScreens/startMarker.gif";
            //            finishMarker = "resources/images/underWater/gameScreens/finishMarker.gif";
            //            screenSwitchButton = "resources/images/underWater/gameScreens/coquille.gif";

            //graphics instruction screen
            //			instrucScreenBackground = "resources/images/common/titlePages/backgroundUnderwater.gif";
            //            instrucScreenInstrucs = "resources/images/common/titlePages/instrucsUnderwater.gif";

            //			//graphics game over screen
            //			gameOverScreenBkgrd = "resources/images/common/titlePages/gameOverUnderwater.gif";

            //graphics: reward Screen
            //            rewardScreenBkgrd = "resources/images/underWater/rewardScreen/rewardChoiceScreen.png";
            //            rewardViewScreenBkgrd = "resources/images/underWater/rewardScreen/rewardViewScreen.png";
            //			reward1[0] = "resources/images/underWater/rewardScreen/rewardFish1_left.gif";
            //			reward1[1] = "resources/images/underWater/rewardScreen/rewardFish1_right.gif";
            //			reward2[0] = "resources/images/underWater/rewardScreen/rewardFish2_left.gif";
            //			reward2[1] = "resources/images/underWater/rewardScreen/rewardFish2_right.gif";
            //			reward3[0] = "resources/images/underWater/rewardScreen/rewardFish3_left.gif";
            //			reward3[1] = "resources/images/underWater/rewardScreen/rewardFish3_right.gif";
            //			reward4[0] = "resources/images/underWater/rewardScreen/rewardFish4_left.gif";
            //			reward4[1] = "resources/images/underWater/rewardScreen/rewardFish4_right.gif";
            //			reward5[0] = "resources/images/underWater/rewardScreen/rewardFish6_left.gif";
            //			reward5[1] = "resources/images/underWater/rewardScreen/rewardFish6_right.gif";
            //			reward6[0] = "resources/images/underWater/rewardScreen/rewardFish7_left.gif";
            //			reward6[1] = "resources/images/underWater/rewardScreen/rewardFish7_right.gif";
            //			reward7[0] = "resources/images/underWater/rewardScreen/rewardFish8_left.gif";
            //			reward7[1] = "resources/images/underWater/rewardScreen/rewardFish8_right.gif";

            //graphics: characChoice Screen
            //            characChoiceBackground = "resources/images/underWater/characChoicePage/characChoicePage.gif";
            //            charac_1_files[0] = "resources/images/underWater/characChoicePage/dolphin8A_100sq.gif";
            //            charac_1_files[1] = "resources/images/underWater/characChoicePage/dolphin8B_100sq.gif";
            //            charac_1_files[2] = "resources/images/underWater/characChoicePage/dolphin8A_100sq_selec.gif";
            //            charac_2_files[0] = "resources/images/underWater/characChoicePage/dolphin6A_100sq.gif";
            //            charac_2_files[1] = "resources/images/underWater/characChoicePage/dolphin6B_100sq.gif";
            //            charac_2_files[2] = "resources/images/underWater/characChoicePage/dolphin6A_100sq_selec.gif";
            //            charac_3_files[0] = "resources/images/underWater/characChoicePage/orcaA_100sq.gif";
            //            charac_3_files[1] = "resources/images/underWater/characChoicePage/orcaB_100sq.gif";
            //            charac_3_files[2] = "resources/images/underWater/characChoicePage/orcaA_100sq_selec.gif";
            //            charac_4_files[0] = "resources/images/underWater/characChoicePage/whale1A_100sq.gif";
            //            charac_4_files[1] = "resources/images/underWater/characChoicePage/whale1B_100sq.gif";
            //            charac_4_files[2] = "resources/images/underWater/characChoicePage/whale1A_100sq_selec.gif";
            //            charac_5_files[0] = "resources/images/underWater/characChoicePage/whale2A_100sq.gif";
            //            charac_5_files[1] = "resources/images/underWater/characChoicePage/whale2B_100sq.gif";
            //            charac_5_files[2] = "resources/images/underWater/characChoicePage/whale2A_100sq_selec.gif";
            //            charac_6_files[0] = "resources/images/underWater/characChoicePage/whale3A_100sq.gif";
            //            charac_6_files[1] = "resources/images/underWater/characChoicePage/whale3B_100sq.gif";
            //            charac_6_files[2] = "resources/images/underWater/characChoicePage/whale3A_100sq_selec.gif";

            //level was successfully set
            return true;
        }

        else if (levelName == IN_THE_JUNGLE) {
            currentLevel = levelName;

            //Set colors
            screenColor = new Color(0, 179, 86); //dark green (foliage)
            dotContColor = new Color(30, 233, 44); //fluro green (vine)
            ThemeVariables.boardColor = dotContColor;
            dotColor = new Color(55, 53, 53); //dark brown (coconut shell)
            dotContDigitColor = Color.BLACK;
            bubbleTextColor = Color.RED;
            boardLineColor = new Color(255, 233, 55); //yellow
            lblDebugAlgColor = new Color(255, 255, 255); //white
            lblTransColor = new Color(255, 255, 255); //white
            dotCarpetBackColor = new Color(228, 207, 179); //tan (matches coconut tree trunk)
            dotCarpetForeColor = new Color(166, 66, 151); //dark purple
            dotCarpetTextColor = Color.BLACK;
            textColor = Color.BLACK;

            //Fonts
            lblDebugAlgFont = new Font("Courier", Font.PLAIN, 12);
            lblTransFont = new Font("Arial Black", Font.PLAIN, i(18));
            dotContDigitFont = new Font("Arial Black", Font.PLAIN,
                    i(GraphicsVariables.DOT_CONT_FONT_SIZE));
            boardNumberFont = new Font("Arial Black", Font.PLAIN, i(40));
            bubbleFont = new Font("Arial Black", Font.PLAIN, i(36));
            buttonTextFont = new Font("Arial Black", Font.PLAIN, i(14));
            regScreenTitleFont = new Font("Arial Black", Font.PLAIN, i(40));
            regScreenTextFont = new Font("Arial Black", Font.PLAIN, i(14));
            characChoiceTitleFont = new Font("Arial Black", Font.PLAIN, i(40));
            characChoiceLabelFont = new Font("Arial Black", Font.PLAIN, i(24));

            //Strings character names
            charactLabel[0] = charac1Label = Messages.getString("LangVars.THEME1CHARAC1");
            charactLabel[1] = charac2Label = Messages.getString("LangVars.THEME1CHARAC2");
            charactLabel[2] = charac3Label = Messages.getString("LangVars.THEME1CHARAC3");
            charactLabel[3] = charac4Label = Messages.getString("LangVars.THEME1CHARAC4");
            charactLabel[4] = charac5Label = Messages.getString("LangVars.THEME1CHARAC5");
            charactLabel[5] = charac6Label = Messages.getString("LangVars.THEME1CHARAC6");

            //FileNames - sounds
            //            player1CharacNoises[0] = "dolphinSqueak";
            //            player1CharacNoises[1] = "dolphinClick";
            //            player1CharacNoises[2] = "orcaSplash";
            //            player1CharacNoises[3] = "whale1Bubble";
            //            player1CharacNoises[4] = "whale2Bubble";
            //            player1CharacNoises[5] = "whale3Bubble";
            //            player2CharacNoises[0] = "crab2Click";
            //            player2CharacNoises[1] = "crab3Click";
            //            player2CharacNoises[2] = "puffFish";
            //            player2CharacNoises[3] = "squidSquirt";

            //Graphics gamescreens
            //            leftDotContainerGifs[0] = "resources/images/inJungle/gameScreens/lefttreeclosed.gif";
            //            leftDotContainerGifs[1] = "resources/images/inJungle/gameScreens/lefttreeopen.gif";
            //            rightDotContainerGifs[0] = "resources/images/inJungle/gameScreens/righttreeclosed.gif";
            //            rightDotContainerGifs[1] = "resources/images/inJungle/gameScreens/righttreeopen.gif";

            //			hazardGifs[0][0] = "resources/images/inJungle/gameScreens/carnivore1a.gif";
            //			hazardGifs[0][1] = "resources/images/inJungle/gameScreens/carnivore1b.gif";
            //			hazardGifs[1][0] = "resources/images/inJungle/gameScreens/carnivore2a.gif";
            //			hazardGifs[1][1] = "resources/images/inJungle/gameScreens/carnivore2b.gif";
            //			hazardGifs[2][0] = "resources/images/inJungle/gameScreens/carnivore3a.gif";
            //			hazardGifs[2][1] = "resources/images/inJungle/gameScreens/carnivore3b.gif";

            //            player1CharacterFiles[0][0] = "resources/images/inJungle/gameScreens/singe1a.gif";
            //            player1CharacterFiles[0][1] = "resources/images/inJungle/gameScreens/singe1b.gif";
            //            player1CharacterFiles[1][0] = "resources/images/inJungle/gameScreens/perroquet1a.gif";
            //            player1CharacterFiles[1][1] = "resources/images/inJungle/gameScreens/perroquet1b.gif";
            //            player1CharacterFiles[2][0] = "resources/images/inJungle/gameScreens/singe2a.gif";
            //            player1CharacterFiles[2][1] = "resources/images/inJungle/gameScreens/singe2b.gif";
            //            player1CharacterFiles[3][0] = "resources/images/inJungle/gameScreens/elephant1a.gif";
            //            player1CharacterFiles[3][1] = "resources/images/inJungle/gameScreens/elephant1b.gif";
            //            player1CharacterFiles[4][0] = "resources/images/inJungle/gameScreens/perroquet2a.gif";
            //            player1CharacterFiles[4][1] = "resources/images/inJungle/gameScreens/perroquet2b.gif";
            //            player1CharacterFiles[5][0] = "resources/images/inJungle/gameScreens/singe3a.gif";
            //            player1CharacterFiles[5][1] = "resources/images/inJungle/gameScreens/singe3b.gif";

            //            player2CharacterFiles[0][0] = "resources/images/inJungle/gameScreens/tigre1a.gif";
            //            player2CharacterFiles[0][1] = "resources/images/inJungle/gameScreens/tigre1b.gif";
            //            player2CharacterFiles[1][0] = "resources/images/inJungle/gameScreens/tigre2a.gif";
            //            player2CharacterFiles[1][1] = "resources/images/inJungle/gameScreens/tigre2b.gif";
            //            player2CharacterFiles[2][0] = "resources/images/inJungle/gameScreens/serpent1a.gif";
            //            player2CharacterFiles[2][1] = "resources/images/inJungle/gameScreens/serpent1b.gif";
            //            player2CharacterFiles[3][0] = "resources/images/inJungle/gameScreens/cobra1a.gif";
            //            player2CharacterFiles[3][1] = "resources/images/inJungle/gameScreens/cobra1b.gif";

            //            choiceButton = "resources/images/inJungle/gameScreens/choiceButton.gif";
            //            helpButton = "resources/images/inJungle/gameScreens/helpButton.gif";
            //            boardButton = "resources/images/inJungle/gameScreens/boardButton.gif";
            //            menuButton = "resources/images/inJungle/gameScreens/menuButton.gif";
            //			cheatKey = "resources/images/inJungle/gameScreens/vine.gif";
            //            startMarker = "resources/images/inJungle/gameScreens/startMarker.gif";
            //            finishMarker = "resources/images/inJungle/gameScreens/finishMarker.gif";
            //            screenSwitchButton = "resources/images/inJungle/gameScreens/fleursexotiques.gif";

            //graphics instruction screen
            //			instrucScreenBackground = "resources/images/common/titlePages/backgroundJungle.gif";
            //            instrucScreenInstrucs = "resources/images/common/titlePages/instrucsJungle.gif";

            //			//graphics game over screen
            //			gameOverScreenBkgrd = "resources/images/common/titlePages/gameOverJungle.gif";

            //graphics: reward Screen
            //            rewardScreenBkgrd = "resources/images/inJungle/rewardScreen/rewardChoiceScreen.png";
            //            rewardViewScreenBkgrd = "resources/images/inJungle/rewardScreen/rewardViewScreen.png";
            //			reward1[0] = "resources/images/inJungle/rewardScreen/butterfly1_left.gif";
            //			reward1[1] = "resources/images/inJungle/rewardScreen/butterfly1_right.gif";
            //			reward2[0] = "resources/images/inJungle/rewardScreen/butterfly2_left.gif";
            //			reward2[1] = "resources/images/inJungle/rewardScreen/butterfly2_right.gif";
            //			reward3[0] = "resources/images/inJungle/rewardScreen/butterfly3_left.gif";
            //			reward3[1] = "resources/images/inJungle/rewardScreen/butterfly3_right.gif";
            //			reward4[0] = "resources/images/inJungle/rewardScreen/butterfly4_left.gif";
            //			reward4[1] = "resources/images/inJungle/rewardScreen/butterfly4_right.gif";
            //			reward5[0] = "resources/images/inJungle/rewardScreen/butterfly5_left.gif";
            //			reward5[1] = "resources/images/inJungle/rewardScreen/butterfly5_right.gif";
            //			reward6[0] = "resources/images/inJungle/rewardScreen/butterfly6_left.gif";
            //			reward6[1] = "resources/images/inJungle/rewardScreen/butterfly6_right.gif";
            //			reward7[0] = "resources/images/inJungle/rewardScreen/butterfly7_left.gif";
            //			reward7[1] = "resources/images/inJungle/rewardScreen/butterfly7_right.gif";

            //graphics: characChoice Screen
            //            characChoiceBackground = "resources/images/inJungle/characChoicePage/characChoicePage.gif";
            //            charac_1_files[0] = "resources/images/inJungle/characChoicePage/singe1violet.gif";
            //            charac_1_files[1] = "resources/images/inJungle/characChoicePage/singe1vert.gif";
            //            charac_1_files[2] = "resources/images/inJungle/characChoicePage/singe1vert.gif";
            //            charac_2_files[0] = "resources/images/inJungle/characChoicePage/perroquet1violet.gif";
            //            charac_2_files[1] = "resources/images/inJungle/characChoicePage/perroquet1vert.gif";
            //            charac_2_files[2] = "resources/images/inJungle/characChoicePage/perroquet1vert.gif";
            //            charac_3_files[0] = "resources/images/inJungle/characChoicePage/singe2violet.gif";
            //            charac_3_files[1] = "resources/images/inJungle/characChoicePage/singe2vert.gif";
            //            charac_3_files[2] = "resources/images/inJungle/characChoicePage/singe2vert.gif";
            //            charac_4_files[0] = "resources/images/inJungle/characChoicePage/elephantviolet.gif";
            //            charac_4_files[1] = "resources/images/inJungle/characChoicePage/elephantvert.gif";
            //            charac_4_files[2] = "resources/images/inJungle/characChoicePage/elephantvert.gif";
            //            charac_5_files[0] = "resources/images/inJungle/characChoicePage/perroquet2violet.gif";
            //            charac_5_files[1] = "resources/images/inJungle/characChoicePage/perroquet2vert.gif";
            //            charac_5_files[2] = "resources/images/inJungle/characChoicePage/perroquet2vert.gif";
            //            charac_6_files[0] = "resources/images/inJungle/characChoicePage/singe3violet.gif";
            //            charac_6_files[1] = "resources/images/inJungle/characChoicePage/singe3vert.gif";
            //            charac_6_files[2] = "resources/images/inJungle/characChoicePage/singe3vert.gif";

            //level was successfully set
            return true;

        }
        //do this if the level doesn't exist
        return false;
    }

    public static byte getLevel() {
        return currentLevel;
    }

    public static String getThemeName() {
        switch (currentLevel) {
            case IN_THE_JUNGLE:
                return "inJungle";

            default:
                return "underWater";
        }
    }

    public String getInstructionsSoundKey() {
        switch (currentLevel) {
            case IN_THE_JUNGLE:
                return "instrucsJungle";

            default:
                return "instrucsUnderwater";
        }
    }
}
