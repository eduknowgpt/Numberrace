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
 * This class is an interface with the adaptive algorithm class
 * It is instantiated by and interacts with NumCompManager
 * It passes data in and out of the algorithm, and it chooses the
 * stimulus attributes for the next GameTurn.
 *
 */

package org.unicog.numberrace.algorithms;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jmat.data.Matrix;
import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.ResourceProvider;
import org.unicog.numberrace.util.Utilities;


public class NumCompAlgManager {

    public static String cclPATH = "org/unicog/numberrace/algorithms/ccl.properties";

    private final Logger log = Logger.getLogger(NumCompAlgManager.class.getPackage()
                                                                       .getName());

    //algorithm variables
    private NotnDimLevel[] notnDimLevels;
    private NewMultiDimAlg mda;

    //utility variables
    private Random randomNumber = new Random();
    public double[] defaultInitialProbs;

    public NumCompAlgManager(String aStartLevel) {
        randomNumber.setSeed(System.currentTimeMillis()); //seed with system time in msec
        //instantiate adaptive algorithm
        mda = new NewMultiDimAlg(AdapDimensions.NUM_ADAP_DIMS,
                initializeNotnLevels(aStartLevel));
    }

    public void setStimAttributes(GameTurn currentTurn) {
        //get a generated data point with difficulties from zero to one
        Matrix currentDifficulty = mda.getGeneratedDataPoint();
        currentTurn.setCurrentDifficulty(currentDifficulty);
        currentTurn.setCoordsChosen(mda.getCoordsChosen());
        currentTurn.setCurrDesiredDiff(mda.getCurrDesir());
        currentTurn.setMeanSuccess(mda.getMeanYmat());
        currentTurn.setEstimatedDiff(mda.getEdChosen());

        //!!!!!this for debugging only!!!!!
        //currentDifficulty.set(0,AdapDimensions.SPEED_DIM,0.3);

        //convert the difficulty levels using the AdapDimensions algorithms
        Matrix convCurrDifficulty = new Matrix(1, AdapDimensions.NUM_ADAP_DIMS);
        convCurrDifficulty.set(0,
                               AdapDimensions.SPEED_DIM,
                               AdapDimensions.speedDimension(currentDifficulty.get(0,
                                                                                   AdapDimensions.SPEED_DIM)));
        // distance dimension set below...
        convCurrDifficulty.set(0,
                               AdapDimensions.NOTN_DIM,
                               notnDimension(currentDifficulty.get(0,
                                                                   AdapDimensions.NOTN_DIM)));

        //Set these parameters in the game

        //Speed dimension
        if (currentDifficulty.get(0, AdapDimensions.SPEED_DIM) < AdapDimensions.DEADLINE_ALPHA)
            currentTurn.setDeadlineTrial(false);
        else {
            currentTurn.setDeadlineTrial(true);
            //set deadline, convert to milliseconds
            currentTurn.setCurrentDeadline(convCurrDifficulty.get(0,
                                                                  AdapDimensions.SPEED_DIM) * 1000);
        }

        //Notation dimension
        int currentNotnLevel;
        //		if(Debugger.IN_USE && game.debugger.debugNotnLevel)	//works b/c stops as soon as finds a false
        //			currentNotnLevel = game.debugger.notnLevelToDebug;	
        //		else
        currentNotnLevel = (int) convCurrDifficulty.get(0,
                                                        AdapDimensions.NOTN_DIM);

        currentTurn.setCurrentNotnLevel(currentNotnLevel);

        //Distance dimension
        //randomly select the side of number x
        int xSide = (int) Math.floor(randomNumber.nextInt(2));
        int ySide = Math.abs(xSide - 1);
        Double weberFraction = new Double(0);
        int[] xyValues = AdapDimensions.distDimension(currentDifficulty.get(0,
                                                                            AdapDimensions.DIST_DIM),
                                                      randomNumber.nextDouble(),
                                                      notnDimLevels[currentNotnLevel].rangeCeilling,
                                                      weberFraction);
        int[] numbers = new int[2];
        numbers[xSide] = xyValues[0];
        numbers[ySide] = xyValues[1];
        currentTurn.setNumbers(numbers);
        //ncm.numbers[ySide] = 2;	//debug end of game	
        //*** below doesn't seem to work *** not catastrophe, can do later in excel	
        convCurrDifficulty.set(0,
                               AdapDimensions.DIST_DIM,
                               weberFraction.doubleValue());
        currentTurn.setConvCurrDifficulty(convCurrDifficulty);

        //Set fixed density or fixed area parameters - which is set selected at random trial by trial
        boolean controlDensity = randomNumber.nextBoolean();
        currentTurn.setControlDensity(controlDensity);

        int additionSide = (int) Math.floor(randomNumber.nextInt(2));
        int subtractionSide = Utilities.oppositeSide(additionSide);
        currentTurn.setAdditionSide((byte) additionSide);
        int[][] subNumbers = new int[2][2];
        String[] operators = new String[2];

        //if there is addition, put an addition sum on that side
        if (notnDimLevels[currentNotnLevel].addition == true) {
            //for the moment, pick the sum at random
            //let mainDotArray.numberDots = leftSubArray.numberDots + rightSubArray.numberDots
            //pick leftSubArray.numberDots randomly from set {1,...mainDotArray.numberDots}
            subNumbers[additionSide][Constants.LEFT] = randomNumber.nextInt(numbers[additionSide]) + 1;
            subNumbers[additionSide][Constants.RIGHT] = numbers[additionSide]
                    - subNumbers[additionSide][Constants.LEFT];
            operators[additionSide] = "+"; //$NON-NLS-1$

            //use for screenshots
            /*
             * ncm.subNumbers[additionSide][Constants.LEFT] = 4;
             * ncm.subNumbers[additionSide][Constants.RIGHT] = 3;
             * ncm.numbers[additionSide] = 7;
             * choiceScreen.dotContainers[additionSide].mainDotArray.setNumber(7);
             */
        }

        //if there is subtraction, put a subtraction on the other side
        if (notnDimLevels[currentNotnLevel].subtraction == true) {
            //for the moment, pick the sum at random
            //given n:0<n<10, want x,y: n = x - y.  Using constraints y!= 0 and x < 11, then
            //x should be chosen from sample space {(n+1),...,10}
            subNumbers[subtractionSide][Constants.LEFT] = randomNumber.nextInt(10 - numbers[subtractionSide])
                    + numbers[subtractionSide] + 1;
            subNumbers[subtractionSide][Constants.RIGHT] = subNumbers[subtractionSide][Constants.LEFT]
                    - numbers[subtractionSide];
            operators[subtractionSide] = "-"; //$NON-NLS-1$

            //use for screenshots
            /*
             * ncm.subNumbers[subtractionSide][Constants.LEFT] = 8;
             * ncm.subNumbers[subtractionSide][Constants.RIGHT] = 6;
             * ncm.numbers[subtractionSide] = 2;
             * choiceScreen.dotContainers[subtractionSide].mainDotArray.setNumber(2);
             */
        }

        //set the relevant attributes of currentTurn
        currentTurn.setSubNumbers(subNumbers);
        currentTurn.setOperators(operators);

    }

    public boolean addTrial(Matrix responsePoint) {
        boolean sucessfullyAddedTrial = mda.addTrial(responsePoint);
        //save the new data
        GameObject.getInstance()
                  .getDataFileHandler()
                  .writeStudentAlgDataFile(mda.getModelData(),
                                           mda.getEdMatrix());
        //		game.dataFileHandler.saveEdMatrix(mda.getEdMatrix());	
        return sucessfullyAddedTrial;
    }

    private int initializeNotnLevels(String level2resolve) {

        if (!loadNotnLevelsFromResources()) {
            notnDimLevels = new NotnDimLevel[14];
            //key: dots, verb,  arbic, fade,  rstRng, hazrd,addtn, subtr,fdspd, board length
            notnDimLevels[0] = new NotnDimLevel(true, false, false, false, 5,
                    false, false, false, 0, 40);
            notnDimLevels[1] = new NotnDimLevel(true, false, false, false, 9,
                    false, false, false, 0, 40);
            notnDimLevels[2] = new NotnDimLevel(true, true, true, false, 5,
                    false, false, false, 0, 40);
            notnDimLevels[3] = new NotnDimLevel(true, true, true, false, 9,
                    false, false, false, 0, 40);
            notnDimLevels[4] = new NotnDimLevel(true, true, true, true, 9,
                    false, false, false, 4000, 40);
            notnDimLevels[5] = new NotnDimLevel(true, true, true, true, 9,
                    false, false, false, 1000, 40);
            notnDimLevels[6] = new NotnDimLevel(false, true, true, false, 9,
                    false, false, false, 0, 40);
            notnDimLevels[7] = new NotnDimLevel(false, false, true, false, 9,
                    false, false, false, 0, 40);
            notnDimLevels[8] = new NotnDimLevel(false, false, true, false, 9,
                    true, false, false, 0, 40);
            notnDimLevels[9] = new NotnDimLevel(false, false, true, false, 9,
                    true, true, false, 0, 40);
            notnDimLevels[10] = new NotnDimLevel(false, false, true, false, 9,
                    true, true, false, 0, 40);
            notnDimLevels[11] = new NotnDimLevel(false, false, true, false, 9,
                    true, false, true, 0, 40);
            notnDimLevels[12] = new NotnDimLevel(false, false, true, false, 9,
                    true, false, true, 0, 40);
            notnDimLevels[13] = new NotnDimLevel(false, false, true, false, 9,
                    true, true, true, 0, 40);
        }

        if (log.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder(
                    "\nNotnLevels (CCL) has been initialized.\n");
            sb.append(String.format("Level boardLength analogMagStims verbalStims arabicStims rangeCeilling dotsFade fadeTime hazards addition subtraction\n"));
            for (int i = 0; i < notnDimLevels.length; i++) {
                sb.append(String.format("%5d ", i + 1))
                  .append(notnDimLevels[i])
                  .append("\n");
            }
            log.fine(sb.toString());
        }

        boolean resolved = false;
        int startFrom = 0;
        for (int i = 0; i < notnDimLevels.length; i++) {
            if (notnDimLevels[i] == null) {
                log.severe("Level [" + i + "] has not been initialized !!!");
            } else if (!resolved && notnDimLevels[i].isStartOf(level2resolve)) {
                startFrom = i;
                if (log.isLoggable(Level.FINE)) {
                    log.fine(String.format("Level %d has been choosen as starting one for %s.",
                                           startFrom + 1,
                                           level2resolve));
                }
                resolved = true;
            }
        }

        return startFrom;
    }

    private boolean loadNotnLevelsFromResources() {
        InputStream cclAsStream = ResourceProvider.getResourceAsStream(cclPATH);
        if (cclAsStream == null) {
            if (log.isLoggable(Level.FINE)) {
                log.fine(String.format("Can not find resource [%s]. Trying to laad it as file.",
                                       cclPATH));
            }
            try {
                cclAsStream = new FileInputStream(cclPATH);
            } catch (FileNotFoundException e) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine(String.format("Can not find file [%s]. Using default value.",
                                           cclPATH));
                }
                return false;
            }
        }
        try {
            Properties ccls = new Properties();
            ccls.load(cclAsStream);
            if (log.isLoggable(Level.FINE)) {
                log.fine(String.format("Loaded %d CCLs. %s",
                                       ccls.size(),
                                       ccls.keySet().toString()));
            }
            notnDimLevels = new NotnDimLevel[ccls.size()];
            Set<Entry<Object, Object>> entries = ccls.entrySet();

            for (Entry<Object, Object> entry : entries) {
                int level = 0;
                try {
                    level = Integer.valueOf((String) entry.getKey()) - 1;
                } catch (NumberFormatException e) {
                    log.severe(String.format("\n key [%s] is need to be an integer (Level number). Setting values for LEVEL 1",
                                             entry.getKey()));
                }

                if (level < 0) {
                    log.severe("Level can not be less than 0, but you have ["
                            + entry.getKey() + "] skipping it.");
                    continue;
                }

                if (level >= notnDimLevels.length) {
                    log.severe("Level number is bigger than tatol amount of level in the file ["
                            + entry.getKey() + "]. Can't use it. skipping...");
                    continue;
                }

                String[] strings = ((String) entry.getValue()).split(",");
                if (strings.length < 10) {
                    log.severe(String.format("\n CCLevel needs at least 10 parameters\n[Board Length, Non-symbolic, Symbolic verbal, Symbolic arabic, Range ceilling, Dot fading, Dot fading duration, Hazards present, Addition required, Subtraction required, [start of level]]\nbut there are %d for key %s",
                                             strings.length,
                                             entry.getKey()));
                    continue;
                }

                int boardLength = 40;
                try {
                    boardLength = Integer.parseInt(strings[0]);
                } catch (NumberFormatException e) {
                    log.severe(String.format("1st parameter for [%s] is not integer [%s] Can't use it for board's length. Using %d",
                                             entry.getKey(),
                                             strings[0],
                                             boardLength));
                }
                boolean nonSymbolic = Utilities.str2bool(strings[1]);
                boolean verbal = Utilities.str2bool(strings[2]);
                boolean arabic = Utilities.str2bool(strings[3]);
                int rangeCeilling = 9;
                try {
                    rangeCeilling = Integer.parseInt(strings[4]);
                } catch (NumberFormatException e) {
                    log.severe(String.format("5th parameter for [%s] is not integer [%s] Can't use as range restriction. Using %d",
                                             entry.getKey(),
                                             strings[4],
                                             rangeCeilling));
                }
                boolean dotFading = Utilities.str2bool(strings[5]);
                int fadingDuration = 0;
                try {
                    fadingDuration = Integer.parseInt(strings[6]);
                } catch (NumberFormatException e) {
                    log.severe(String.format("7th parameter for [%s] is not integer [%s] Can't use as fading duration. Using %d",
                                             entry.getKey(),
                                             strings[6],
                                             fadingDuration));
                }
                boolean hazards = Utilities.str2bool(strings[7]);
                boolean addition = Utilities.str2bool(strings[8]);
                boolean subtraction = Utilities.str2bool(strings[9]);

                notnDimLevels[level] = new NotnDimLevel(nonSymbolic, verbal,
                        arabic, dotFading, rangeCeilling, hazards, addition,
                        subtraction, fadingDuration, boardLength);

                if (strings.length > 10 && !"".equals(strings[10])) {
                    notnDimLevels[level].setStartOf(strings[10]);
                }

            }

            return true;
        } catch (IOException e) {
            log.severe(e.getMessage());
            return false;
        } finally {
            if (cclAsStream != null) {
                try {
                    cclAsStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public NotnDimLevel getNotnLevel(int level) {
        return notnDimLevels[level];
    }

    private int notnDimension(double dn) {
        final int levels_amout = notnDimLevels.length;
        int zValue = (int) Math.floor(dn * levels_amout);
        if (zValue == levels_amout) //in case dn==1
            zValue--;
        return zValue;
    }

    public void setModelData(Matrix modelData, Matrix3D edMatrix) {
        mda.setModelData(modelData);
        mda.setEdMatrix(edMatrix);
    }

    public Matrix3D getEdMatrix() {
        return mda.getEdMatrix();
    }

    public String getNotnAttrCommaDelim(int currentNotnLevel) {
        //Returns converted difficulty levels in the form of a comma delimited string	
        return notnDimLevels[currentNotnLevel].getAttributesCommaDelim();
    }

}