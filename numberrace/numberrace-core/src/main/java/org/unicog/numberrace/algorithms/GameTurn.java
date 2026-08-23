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

/*
*This class is a data structure to store info for each turn in the game;
*both stimulus and response parameters. It also contains methods to output data
*about a turn, and to calculate the winner of a turn, by taking into account the relative
*change in position of the players.
*/

package org.unicog.numberrace.algorithms;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import org.jmat.data.Matrix;
import org.unicog.numberrace.Game;
import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.Utilities;

public class GameTurn {
    
    private static Calendar cal = Calendar.getInstance();
    private int turnNumber = 0; //turn number for this game

    //stimulus parameters
    private int numbers[]; //numbers for this turn
    private int subNumbers[][]; //numbers for sub dot arrays (if addition/subtractions present)
                                //[dotContainerSide][subDotArraySide]
    private String operators[]; //stores operands (eg. addition, subtraction) for each side
    private int additionSide; //side which has addition
    private boolean deadlineTrial = false; //true if there is a time deadline to respond this turn
    private double currentDeadline = 0.0; //value of deadline in msec
    private int notationLevel; //the notation level of the current trial
    private boolean controlDensity;
    private Matrix difficulty; //difficulty levels from 0 to 1 for current trial
    private Matrix convDifficulty; //difficulty levels in correct range converted by AdapDimension algorithms
    private Matrix coordsChosen; //coordinates chosen in problem space
    private double desiredDifficulty;
    private double meanSuccess;
    private double estimatedDifficulty;
    private Date turnStartTime;

    //response parameters
    private int responseSide = Constants.NO_RESPONSE; //default value for no response
    private byte responseCorrect = Constants.FALSE; //whether the child picked the bigger number
    private long RT = 0;
    private int relNetGain = 0; //player1 net move - player 2 net move
    private int[] netGain = new int[2]; //net squares moved for each player after hazards and collisions
    private int[] squaresMovedForward = new int[2]; //number of squares moved back
    private int[] squaresMovedBack = new int[2]; //number of squares moved forward
    private int h_relNetGain = 0; //the same variables but for the
    private int[] h_netGain = new int[2]; //hypothetical case of a choice
    private int[] h_squaresMovedForward = new int[2];//of the other number
    private int[] h_squaresMovedBack = new int[2];
    private int preTurnInterPlayerDistance = 0; //these are needed independently
    private int postTurnInterPlayerDistance = 0; //for detailed board feedback

    private byte actualWinner = 2; //the player who actually wins this turn
    //Note: the child wins the turn if a) he picks the biggest number or b) he picks the smallest
    //number, but the relative net gain was greater than if he had made the other choice

    public static final String controlFor[] = { "density", "itemSize" }; //$NON-NLS-1$ //$NON-NLS-2$
    private static String[] titles = { "Speed: ", "Dist: ", "Notn: " }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    private GameObject go;
    private org.unicog.numberrace.managers.HazardManager hzm;

    public GameTurn(int turnNumber) {
        go = GameObject.getInstance();
        hzm = go.getHazardManager();
        this.turnNumber = turnNumber;
        preTurnInterPlayerDistance = go.getPlayerPosition()
                - go.getOpponentPosition();
        subNumbers = new int[2][2]; //sometimes this is not set so initialise to zero
        operators = new String[2];
    }

    public void startTurn() {
        cal.setTimeInMillis(System.currentTimeMillis());
        turnStartTime = cal.getTime();
        Utilities.log.info("GAME TURN STARTS AT: " + turnStartTime);
    }

    public void calculateActualWinner() {
        //This calculates the actual winner of a given trial, taking into account
        //hazards and players landing on each other
        //Based on the relative net gain
        //take the numbers that were chosen
        int simBoardLocations[] = new int[Constants.NUM_PLAYERS];
        simBoardLocations[Constants.PLAYER1] = go.getPlayerPosition();
        simBoardLocations[Constants.PLAYER2] = go.getOpponentPosition();
        squaresMovedForward[Constants.PLAYER1] = numbers[responseSide];
        squaresMovedForward[Constants.PLAYER2] = numbers[Utilities.oppositeSide(responseSide)];

        //for each player figure out how many squares their choice will advance them,
        //	taking into account any hazards, and being landed on by other players
        for (int i = 0; i < Constants.NUM_PLAYERS; i++) {
            //simulate the players' move
            simBoardLocations[i] += squaresMovedForward[i];

            //make adjustments according to hazards and landing on the other player
            do {
                //check if landed on character
                if (simBoardLocations[i] == simBoardLocations[Utilities.otherPlayer((byte) i)]) {
                    //if so send that character back
                    simBoardLocations[Utilities.otherPlayer((byte) i)]--;
                    //move the other player according to hazards
                    do {
                        if (hzm.movementDueToHazard(simBoardLocations[Utilities.otherPlayer((byte) i)]) < 0) {
                            simBoardLocations[Utilities.otherPlayer((byte) i)] += hzm.movementDueToHazard(simBoardLocations[Utilities.otherPlayer((byte) i)]);
                        } else {
                            break;
                        }
                    } while (true);
                }

                //move the current player according to hazards
                if (hzm.movementDueToHazard(simBoardLocations[i]) < 0) {
                    simBoardLocations[i] += hzm.movementDueToHazard(simBoardLocations[i]);
                } else {
                    break;
                }
            } while (true);
        } //for num players

        //record values for later
        netGain[Constants.PLAYER1] = simBoardLocations[Constants.PLAYER1]
                - go.getPlayerPosition();
        netGain[Constants.PLAYER2] = simBoardLocations[Constants.PLAYER2]
                - go.getOpponentPosition();
        squaresMovedBack[Constants.PLAYER1] = squaresMovedForward[Constants.PLAYER1]
                - netGain[Constants.PLAYER1];
        squaresMovedBack[Constants.PLAYER2] = squaresMovedForward[Constants.PLAYER2]
                - netGain[Constants.PLAYER2];

        relNetGain = netGain[Constants.PLAYER1] - netGain[Constants.PLAYER2];

        //figure out what would have happened if the other choice had been made
        calculateHypotheticalWinner((byte) Utilities.oppositeSide(responseSide));

        //finally code the response of the child as correct if they
        //a) choose the bigger number, OR
        //b) choose the smallest number but got the biggest relative gain
        //(we give them the benefit of the doubt)
        if ((relNetGain >= h_relNetGain) || responseCorrect == (Constants.TRUE))
            actualWinner = Constants.PLAYER1;
        else
            actualWinner = Constants.PLAYER2;

    }

    public void calculateHypotheticalWinner(byte responseSide) {
        //This calculates the hypothetical winner of a given trial, taking into account
        //hazards and players landing on each other
        //take the numbers that were chosen
        int simBoardLocations[] = new int[Constants.NUM_PLAYERS];

        simBoardLocations[Constants.PLAYER1] = go.getPlayerPosition();
        simBoardLocations[Constants.PLAYER2] = go.getOpponentPosition();
        h_squaresMovedForward[Constants.PLAYER1] = numbers[responseSide];
        h_squaresMovedForward[Constants.PLAYER2] = numbers[Utilities.oppositeSide(responseSide)];

        //for each player figure out how many squares their choice will advance them,
        //	taking into account any hazards, and being landed on by other players
        for (int i = 0; i < Constants.NUM_PLAYERS; i++) {
            //simulate the players' move
            simBoardLocations[i] += h_squaresMovedForward[i];

            //make adjustments according to hazards and landing on the other player
            do {
                //check if landed on character
                if (simBoardLocations[i] == simBoardLocations[Utilities.otherPlayer((byte) i)]) {
                    //if so send that character back
                    simBoardLocations[Utilities.otherPlayer((byte) i)]--;
                    //move the other player according to hazards
                    do {
                        if (hzm.movementDueToHazard(simBoardLocations[Utilities.otherPlayer((byte) i)]) < 0) {
                            simBoardLocations[Utilities.otherPlayer((byte) i)] += hzm.movementDueToHazard(simBoardLocations[Utilities.otherPlayer((byte) i)]);
                        } else {
                            break;
                        }
                    } while (true);
                }

                //move the current player according to hazards
                if (hzm.movementDueToHazard(simBoardLocations[i]) < 0) {
                    simBoardLocations[i] += hzm.movementDueToHazard(simBoardLocations[i]);
                } else {
                    break;
                }
            } while (true);
        } //for num players

        //record the actual winner for later
        h_netGain[Constants.PLAYER1] = simBoardLocations[Constants.PLAYER1]
                - go.getPlayerPosition();
        h_netGain[Constants.PLAYER2] = simBoardLocations[Constants.PLAYER2]
                - go.getOpponentPosition();
        h_squaresMovedBack[Constants.PLAYER1] = h_squaresMovedForward[Constants.PLAYER1]
                - h_netGain[Constants.PLAYER1];
        h_squaresMovedBack[Constants.PLAYER2] = h_squaresMovedForward[Constants.PLAYER2]
                - h_netGain[Constants.PLAYER2];

        h_relNetGain = h_netGain[Constants.PLAYER1]
                - h_netGain[Constants.PLAYER2];
    }

    public Date getTurnStartTime() {
        return turnStartTime;
    }

    public double getMeanSuccess() {
        return meanSuccess;
    }

    public void setMeanSuccess(double meanSucc) {
        meanSuccess = meanSucc;
    }

    public double getEstimatedDiff() {
        return estimatedDifficulty;
    }

    public void setEstimatedDiff(double diff) {
        estimatedDifficulty = diff;
    }

    public String getCoordsChosenCommaDelim() {
        //Returns coords chosen in the form of a comma delimited string
        String commaDelimStr = ""; //$NON-NLS-1$
        for (int i = 0; i < AdapDimensions.NUM_ADAP_DIMS; i++) {
            commaDelimStr = commaDelimStr.concat(String.valueOf(coordsChosen.get(0,
                                                                                 i))
                    + ","); //$NON-NLS-1$
        }
        return commaDelimStr;
    }

    public void setCoordsChosen(Matrix coordsChosen) {
        this.coordsChosen = coordsChosen;
    }

    public double getCurrDesiredDiff() {
        return desiredDifficulty;
    }

    public void setCurrDesiredDiff(double diff) {
        desiredDifficulty = diff;
    }

    public String getCurrDesiredDiffForPrompt() {
        String str = ""; //$NON-NLS-1$
        str = str.concat(String.valueOf(Utilities.round(desiredDifficulty, 2)));
        return str;
    }

    public String getDiffLvlsCommaDelim() {
        //Returns difficulty levels in the form of a comma delimited string
        String commaDelimStr = ""; //$NON-NLS-1$
        for (int i = 0; i < AdapDimensions.NUM_ADAP_DIMS; i++) {
            commaDelimStr = commaDelimStr.concat(String.valueOf(difficulty.get(0,
                                                                               i))
                    + ","); //$NON-NLS-1$
        }
        return commaDelimStr;
    }

    public String getDiffLvlsForPrompt() {
        //Returns current difficulty in the form of a tab delimited string with titles for each
        String promptStr = ""; //$NON-NLS-1$
        for (int i = 0; i < AdapDimensions.NUM_ADAP_DIMS; i++) {
            promptStr = promptStr.concat(titles[i]
                    + String.valueOf(Utilities.round((difficulty.get(0, i)), 2))
                    + "  "); //$NON-NLS-1$
        }
        return promptStr;
    }

    public String getConvDiffLvlsCommaDelim() {
        //Returns converted difficulty levels in the form of a comma delimited string
        String commaDelimStr = ""; //$NON-NLS-1$
        for (int i = 0; i < AdapDimensions.NUM_ADAP_DIMS; i++) {
            commaDelimStr = commaDelimStr.concat(String.valueOf(convDifficulty.get(0,
                                                                                   i))
                    + ","); //$NON-NLS-1$
        }
        return commaDelimStr;
    }

    public String getConvDiffLvlsForPrompt() {
        //Returns converted difficulty levels in the form of a comma delimited string
        String promptStr = ""; //$NON-NLS-1$
        for (int i = 0; i < AdapDimensions.NUM_ADAP_DIMS; i++) {
            promptStr = promptStr.concat(titles[i]
                    + String.valueOf(Utilities.round((convDifficulty.get(0, i)),
                                                     2)) + "  "); //$NON-NLS-1$
        }
        return promptStr;
    }

    public byte getActualWinner() {
        return actualWinner;
    }

    public byte getFinalCorrect() {
        if (actualWinner == Constants.PLAYER1)
            return 1;
        else
            return 0;
    }

    public String getFinalCorrectStr() {
        if (actualWinner == Constants.PLAYER1)
            return "1"; //$NON-NLS-1$
        else
            return "0"; //$NON-NLS-1$
    }

    public void setActualWinner(byte player) {
        //this is used when the child misses the deadline
        actualWinner = player;
    }

    public String getRelativeGainInfo() {
        return relNetGain + "," //$NON-NLS-1$
                + netGain[Constants.PLAYER1] + "," //$NON-NLS-1$
                + netGain[Constants.PLAYER2] + "," //$NON-NLS-1$
                + squaresMovedForward[Constants.PLAYER1] + "," //$NON-NLS-1$
                + squaresMovedForward[Constants.PLAYER2] + "," //$NON-NLS-1$
                + squaresMovedBack[Constants.PLAYER1] + "," //$NON-NLS-1$
                + squaresMovedBack[Constants.PLAYER2] + "," //$NON-NLS-1$
                + go.getPlayerPosition() + "," //$NON-NLS-1$
                + go.getOpponentPosition() + ","; //$NON-NLS-1$
    }

    public String getHypRelativeGainInfo() {
        return h_relNetGain + "," //$NON-NLS-1$
                + h_netGain[Constants.PLAYER1] + "," //$NON-NLS-1$
                + h_netGain[Constants.PLAYER2] + "," //$NON-NLS-1$
                + h_squaresMovedForward[Constants.PLAYER1] + "," //$NON-NLS-1$
                + h_squaresMovedForward[Constants.PLAYER2] + "," //$NON-NLS-1$
                + h_squaresMovedBack[Constants.PLAYER1] + "," //$NON-NLS-1$
                + h_squaresMovedBack[Constants.PLAYER2] + ","; //$NON-NLS-1$
    }

    public int getResponseSide() {
        return responseSide;
    }

    public String getResponseSideString() {
        return Constants.sides[responseSide];
    }

    public long getRT() {
        return RT;
    }

    public String getRTStr() {
        return String.valueOf(RT);
    }

    public void setRT(long rt) {
        this.RT = rt;
    }

    public byte getResponseCorrect() {
        return responseCorrect;
    }

    public String getResponseCorrectStr() {
        return Constants.correct[responseCorrect];
    }

    public void setResponseCorrect(byte status) {
        responseCorrect = status;
    }

    public void setResponseSide(int respSide) {
        responseSide = respSide;
    }

    public int getPostTurnInterPlayerDistance() {
        return postTurnInterPlayerDistance;
    }

    public void setPostTurnInterPlayerDistance(int value) {
        postTurnInterPlayerDistance = value;
    }

    public int getPreTurnInterPlayerDistance() {
        return preTurnInterPlayerDistance;
    }

    public void setPreTurnInterPlayerDistance(int value) {
        preTurnInterPlayerDistance = value;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public String[] getOperators() {
        return operators;
    }

    public void setOperators(String[] operators) {
        this.operators = operators;
    }

    public int getAdditionSide() {
        return additionSide;
    }

    public void setAdditionSide(int additionSide) {
        this.additionSide = additionSide;
    }

    public int[][] getSubNumbers() {
        return subNumbers;
    }

    public int getSubNumber(int i, int j) {
        return subNumbers[i][j];
    }

    public void setSubNumbers(int[][] subNumbers) {
        this.subNumbers = subNumbers;
    }

    public String getSubNumbersAndOpsCommaDelim() {
        return String.valueOf(subNumbers[Constants.LEFT][Constants.LEFT])
                + "," //$NON-NLS-1$
                + operators[Constants.LEFT]
                + "," //$NON-NLS-1$
                + String.valueOf(subNumbers[Constants.LEFT][Constants.RIGHT])
                + "," //$NON-NLS-1$
                + String.valueOf(subNumbers[Constants.RIGHT][Constants.LEFT])
                + "," //$NON-NLS-1$
                + operators[Constants.RIGHT]
                + "," //$NON-NLS-1$
                + String.valueOf(subNumbers[Constants.RIGHT][Constants.RIGHT])
                + ","; //$NON-NLS-1$
    }

    public int[] getNumbers() {
        return numbers;
    }

    public int getNumber(int i) {
        return numbers[i];
    }

    public String getNumbersCommaDelim() {
        return String.valueOf(numbers[Constants.LEFT]) + "," //$NON-NLS-1$
                + String.valueOf(numbers[Constants.RIGHT]) + ","; //$NON-NLS-1$
    }

    public void setNumbers(int[] numbers) {
        this.numbers = numbers;
    }

    public boolean getControlDensity() {
        return controlDensity;
    }

    public int getControlDensityInt() {
        if (controlDensity)
            return 1;
        else
            return 0;
    }

    public String getControlDensityStr() {
        return controlFor[getControlDensityInt()];
    }

    public void setControlDensity(boolean state) {
        controlDensity = state;
    }

    public int getCurrentNotnLevel() {
        return notationLevel;
    }

    public void setCurrentNotnLevel(int level) {
        notationLevel = level;
    }

    public Matrix getConvCurrDifficulty() {
        return convDifficulty;
    }

    public void setConvCurrDifficulty(Matrix convDifficulty) {
        this.convDifficulty = convDifficulty;
    }

    public Matrix getCurrentDifficulty() {
        return difficulty;
    }

    public void setCurrentDifficulty(Matrix difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isDeadlineTrial() {
        return deadlineTrial;
    }

    public void setDeadlineTrial(boolean state) {
        deadlineTrial = state;
    }

    public double getCurrentDeadline() {
        return currentDeadline;
    }

    public void setCurrentDeadline(double deadline) {
        currentDeadline = deadline;
    }

    @Override
    public String toString() {
        return String.format("GameTurn\n\tturnNumber=%d ccLevel=%d\n\tnumbers=%s subNumbers=%s operators=%s\n\tadditionSide=%s deadlineTrial=%s currentDeadline=%s controlDensity=%s\n\tdifficulty=%s\tconvDifficulty=%s\tcoordsChosen=%s\tdesiredDifficulty=%s meanSuccess=%s estimatedDifficulty=%s\n\tturnStartTime=%s\n\tresponseSide=%s",
                             turnNumber,
                             notationLevel + 1,
                             Arrays.toString(numbers),
                             Arrays.deepToString(subNumbers),
                             Arrays.toString(operators),
                             additionSide,
                             deadlineTrial,
                             currentDeadline,
                             controlDensity,
                             difficulty,
                             convDifficulty,
                             coordsChosen,
                             desiredDifficulty,
                             meanSuccess,
                             estimatedDifficulty,
                             turnStartTime,
                             responseSide);
    }

    
    
}