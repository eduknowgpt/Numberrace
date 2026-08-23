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
 *This class is a data structure to store info for each student
 */

package org.unicog.numberrace.data;

import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.StringTokenizer;

import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.vars.ThemeVariables;

public class Student {
    private int dataLine; //records the line in the data file, so record can be accessed quickly
    private String lastName;
    private String firstName;
    private int age;
    private String classLevel;
    private int sex;
    private String startLevel;
    private int sessionNumber;
    private String fileHeader;
    private boolean[][] characAccess; //2-D array, first dim is graphical level, second is data for that level
    private byte[][] rewardCount; //ditto
    private int gamesPlayedSession;
    private int gamesWonSession;
    private int gamesPlayedTotal;
    private int gamesWonTotal;
    private Random randomNumber;

    public final static int NUM_GAMES_PER_CHARAC = 10;

    //    // might be deleted later
    //    public Student(int dataLine, String lastName, String firstName, int age,
    //            String classLevel, int sex, int lastSessionNumber, int gamesWon,
    //            int gamesPlayed, boolean[][] characAccess, byte[][] rewardCount) {
    //
    //        this(dataLine, lastName, firstName, age, classLevel, sex, 0,
    //                lastSessionNumber, gamesWon, gamesPlayed, characAccess,
    //                rewardCount);
    //    }

    /**
     * Constructor (new, includes startLevel)
     * 
     * @param dataLine
     * @param lastName
     * @param firstName
     * @param age
     * @param classLevel
     * @param sex
     *            0 - male 1 - female
     * @param startLevel
     *            Level where student starts the game
     * @param lastSessionNumber
     * @param gamesWon
     * @param gamesPlayed
     * @param characAccess
     * @param rewardCount
     */
    public Student(int dataLine, String lastName, String firstName, int age,
            String classLevel, int sex, String startLevel,
            int lastSessionNumber, int gamesWon, int gamesPlayed,
            boolean[][] characAccess, byte[][] rewardCount) {

        this.dataLine = dataLine;
        this.lastName = lastName;
        this.firstName = firstName;
        this.age = age;
        this.classLevel = classLevel;
        this.sex = sex;
        this.startLevel = startLevel;
        this.sessionNumber = lastSessionNumber;
        this.gamesWonTotal = gamesWon;
        this.gamesPlayedTotal = gamesPlayed;
        this.characAccess = characAccess;
        this.rewardCount = rewardCount;
        fileHeader = lastName + "_" + firstName; //$NON-NLS-1$
        randomNumber = new Random(System.currentTimeMillis());
    }

    public String toString() {
        //This is what JList uses to display the object
        return lastName + ", " + firstName; //$NON-NLS-1$
    }

    public void augmentSession() {
        sessionNumber++;
    }

    public void augmentGamesPlayed() {
        gamesPlayedSession++;
        gamesPlayedTotal++;
    }

    public void augmentGamesWon() {
        gamesWonSession++;
        gamesWonTotal++;
    }

    public static String getHeaders() {
        //outputs headers for data file
        return "DataLine, LastName,FirstName,Age,ClassLevel,Sex,LastSession,gamesWon,gamesPlayed" + //$NON-NLS-1$
                "lev1char1,lev1char2,lev1char3,lev1char4,lev1char5,lev1char6"
                + //$NON-NLS-1$
                "lev2char1,lev2char2,lev2char3,lev2char4,lev2char5,lev2char6"
                + //$NON-NLS-1$
                "lev1rew1,lev1rew2,lev1rew3,lev1rew4,lev1rew5,lev1rew6,lev1rew7"
                + //$NON-NLS-1$
                "lev2rew1,lev2rew2,lev2rew3,lev2rew4,lev2rew5,lev2rew6,lev2rew7"; //$NON-NLS-1$
    }

    public String getAttributesCommaDelim() {
        //Returns converted difficulty levels in the form of a comma delimited string
        String commaDelimStr = dataLine
                + "," + lastName + "," + firstName + "," + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                age + "," + classLevel + "," + //$NON-NLS-1$ //$NON-NLS-2$
                sex + "," + sessionNumber + "," + //$NON-NLS-1$ //$NON-NLS-2$
                gamesWonTotal + "," + gamesPlayedTotal + "," + //$NON-NLS-1$ //$NON-NLS-2$
                characAccess[0][0] + "," + //$NON-NLS-1$
                characAccess[0][1] + "," + //$NON-NLS-1$
                characAccess[0][2] + "," + //$NON-NLS-1$
                characAccess[0][3] + "," + //$NON-NLS-1$
                characAccess[0][4] + "," + //$NON-NLS-1$
                characAccess[0][5] + "," + //$NON-NLS-1$
                characAccess[1][0] + "," + //$NON-NLS-1$
                characAccess[1][1] + "," + //$NON-NLS-1$
                characAccess[1][2] + "," + //$NON-NLS-1$
                characAccess[1][3] + "," + //$NON-NLS-1$
                characAccess[1][4] + "," + //$NON-NLS-1$
                characAccess[1][5] + "," + //$NON-NLS-1$
                rewardCount[0][0] + "," + //$NON-NLS-1$
                rewardCount[0][1] + "," + //$NON-NLS-1$
                rewardCount[0][2] + "," + //$NON-NLS-1$
                rewardCount[0][3] + "," + //$NON-NLS-1$
                rewardCount[0][4] + "," + //$NON-NLS-1$
                rewardCount[0][5] + "," + //$NON-NLS-1$
                rewardCount[0][6] + "," + //$NON-NLS-1$
                rewardCount[1][0] + "," + //$NON-NLS-1$
                rewardCount[1][1] + "," + //$NON-NLS-1$
                rewardCount[1][2] + "," + //$NON-NLS-1$
                rewardCount[1][3] + "," + //$NON-NLS-1$
                rewardCount[1][4] + "," + //$NON-NLS-1$
                rewardCount[1][5] + "," + //$NON-NLS-1$
                rewardCount[1][6] + "," + startLevel;
        return commaDelimStr;
    }

    public int getNumRewardsCollected(byte graphicalLevel) {
        int rewardTotal = 0;
        for (int i = 0; i < Constants.NUMBER_POSS_REWARDS; i++) {
            rewardTotal = rewardTotal + rewardCount[graphicalLevel][i];
        }
        return rewardTotal;
    }

    public byte[] getRewardCount(byte graphicalLevel) {
        return rewardCount[graphicalLevel]; //will this return the sub-array???
    }

    public void addReward(int rewardType, byte graphicalLevel) {
        rewardCount[graphicalLevel][rewardType]++;
    }

    public int getNextRewardEasy(byte graphicalLevel) {
        int reward = 99;

        for (int numLoops = 0; numLoops < (Constants.NUMBER_POSS_REWARDS * 2); numLoops++) {

            //pick a reward at random
            reward = randomNumber.nextInt(Constants.NUMBER_POSS_REWARDS);

            //check that this isn't a reward that the child already has
            if (rewardCount[graphicalLevel][reward] == 0) {
                return reward;
            }
        }

        for (int i = 0; i < Constants.NUMBER_POSS_REWARDS; i++) {
            if (rewardCount[graphicalLevel][i] == 0) {
                return i;
            }
        }

        return reward;

    }

    //
    //    public int getNextReward(byte graphicalLevel) {
    //        //this method no longer used
    //        int reward = 99;
    //
    //        //check to see how many rewards child has none of
    //        int numZeros = 0;
    //        for (int i = 0; i < Constants.NUMBER_POSS_REWARDS; i++) {
    //            if (rewardCount[graphicalLevel][i] == 0)
    //                numZeros++;
    //        }
    //
    //        //check to see how many games have been played since last character unlocked
    //        //and how many more games are left before the next unlock should happen
    //        int numGamesSinceLastUnlock = gamesPlayedTotal
    //                - (getNumCharacsUnlocked(graphicalLevel) * NUM_GAMES_PER_CHARAC);
    //        int numGamesBeforeNextUnlock = NUM_GAMES_PER_CHARAC
    //                - numGamesSinceLastUnlock;
    //
    //        if (numZeros == numGamesBeforeNextUnlock) {
    //            //if the number of rewards not collected is the same as games left before next unlock
    //            //make sure to give the child one of the rewards they need
    //            for (int i = 0; i < Constants.NUMBER_POSS_REWARDS; i++) {
    //                if (rewardCount[graphicalLevel][i] == 0) {
    //                    reward = i;
    //                    break;
    //                }
    //            }
    //        } else if (numZeros == 1) {
    //            //if there is only one reward to collect, make sure not to give that reward			
    //            reward = randomNumber.nextInt(Constants.NUMBER_POSS_REWARDS);
    //            for (int i = 0; i < Constants.NUMBER_POSS_REWARDS; i++) {
    //                if (rewardCount[graphicalLevel][i] == 0) {
    //                    //if this is the reward not to give, pick another
    //                    while (reward == i) {
    //                        reward = randomNumber
    //                                .nextInt(Constants.NUMBER_POSS_REWARDS);
    //                    }
    //                }
    //            }
    //        } else {
    //            //assign any reward at random
    //            reward = randomNumber.nextInt(Constants.NUMBER_POSS_REWARDS);
    //        }
    //        //System.out.println("Reward number returned: " + reward);
    //        return reward;
    //    }

    public boolean checkForCharacterUnlock(byte graphicalLevel) {
        for (int i = 0; i < Constants.NUMBER_POSS_REWARDS; i++) {
            if (rewardCount[graphicalLevel][i] == 0)
                return false;
        }
        return true;
    }

    public void unlockNextCharacter(byte graphicalLevel) {
        for (int i = 0; i < Constants.NUMBER_CHARACTERS; i++) {
            if (characAccess[graphicalLevel][i] == false) {
                characAccess[graphicalLevel][i] = true;
                //reset all the collected rewards to zero
                for (int j = 0; j < Constants.NUMBER_POSS_REWARDS; j++) {
                    rewardCount[graphicalLevel][j] = 0;
                }
                return;
            }
        }
    }

    public static Student readAllStudentListLine(
            LineNumberReader studentTextFileLineReader) {
        String lineReadFromFile = null;
        Student student = null;
        if (studentTextFileLineReader != null) {
            byte passes = 0;
            do {
                try {
                    lineReadFromFile = studentTextFileLineReader.readLine();
                } catch (IOException e) {
                    System.out.println(e);
                }
                passes++;
            } while ((studentTextFileLineReader.getLineNumber() == 1)
                    && (passes <= 1)); //skip header

            if (lineReadFromFile != null) {
                //!! need some way to check if the line is in the right format
                StringTokenizer stringConvertor = new StringTokenizer(
                        lineReadFromFile, ","); //$NON-NLS-1$
                int dataLine = Integer.parseInt(stringConvertor.nextToken());
                String lastName = stringConvertor.nextToken();
                String firstName = stringConvertor.nextToken();
                String ageStr = stringConvertor.nextToken();
                int age = Integer.parseInt(ageStr);
                String classLevel = stringConvertor.nextToken();
                int sex = -1; // not def
                try {
                    sex = Integer.parseInt(stringConvertor.nextToken());
                } catch (NumberFormatException e) {
                    // silently skipping. Because old user data contain work for gender definition, let's make it UNDEFINED in such a case and user can change it
                }

                int sessionNumber = Integer.parseInt(stringConvertor.nextToken());
                int gamesWon = Integer.parseInt(stringConvertor.nextToken());
                int gamesTotal = Integer.parseInt(stringConvertor.nextToken());
                boolean[][] characAccess = new boolean[ThemeVariables.NUMBER_OF_THEMES][Constants.NUMBER_CHARACTERS];
                for (int j = 0; j < ThemeVariables.NUMBER_OF_THEMES; j++) {
                    for (int i = 0; i < Constants.NUMBER_CHARACTERS; i++) {
                        String tmpStr = stringConvertor.nextToken();
                        characAccess[j][i] = Boolean.valueOf(tmpStr)
                                                    .booleanValue();
                    }
                }
                byte[][] rewardCount = new byte[ThemeVariables.NUMBER_OF_THEMES][Constants.NUMBER_POSS_REWARDS];
                for (int j = 0; j < ThemeVariables.NUMBER_OF_THEMES; j++) {
                    for (int i = 0; i < Constants.NUMBER_POSS_REWARDS; i++) {
                        String tmpStr = stringConvertor.nextToken();
                        rewardCount[j][i] = Byte.valueOf(tmpStr).byteValue();
                    }
                }
                //Now read in the level at which the student started
                String startLvlStr = stringConvertor.nextToken();

                //	            if(startLvlStr!=null){
                //					startLvl = Integer.parseInt(startLvlStr);           	
                //	            }
                //	            else {
                //	            	startLvl = 0;
                //	            }

                student = new Student(dataLine, lastName, firstName, age,
                        classLevel, sex, startLvlStr, sessionNumber, gamesWon,
                        gamesTotal, characAccess, rewardCount);
            }
        }
        if (student != null) {
            return student;
        } else
            return null;
    }

    public void writeAllStudentListLine(FileWriter fileWriter) {
        if (fileWriter != null) {
            try {
                fileWriter.write(getAttributesCommaDelim() + "\r\n"); //$NON-NLS-1$
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getDataLine() {
        return dataLine;
    }

    public void setDataLine(int dataLine) {
        this.dataLine = dataLine;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setfirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFileHeader() {
        return fileHeader;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    /**
     * 
     * @return 0 - male 1 - female;
     */
    public int getSex() {
        return sex;
    }

    /**
     * 
     * @param sex
     *            0 - male, 1 - female;
     */
    public void setSex(int sex) {
        this.sex = sex;
    }

    public String getClassLevel() {
        return classLevel;
    }

    public void setClassLevel(String classLevel) {
        this.classLevel = classLevel;
    }

    public String getStartLevel() {
        return startLevel;
    }

    public void setStartLevel(String level) {
        this.startLevel = level;
    }

    public int getSessionNumber() {
        return sessionNumber;
    }

    public boolean getCharacAccess(int characID, byte graphicalLevel) {
        //		if (characAccess[graphicalLevel][characID])
        //			return true;
        //		else
        //			return false;
        return characAccess[graphicalLevel][characID];
    }

    public int getNumCharacsUnlocked(byte graphicalLevel) {
        int numCharacsUnlocked = 0;
        for (int i = 0; i < Constants.NUMBER_CHARACTERS; i++) {
            //			if(characAccess[graphicalLevel][i]==true)
            if (characAccess[graphicalLevel][i])
                numCharacsUnlocked++;
        }
        return numCharacsUnlocked;
    }

}