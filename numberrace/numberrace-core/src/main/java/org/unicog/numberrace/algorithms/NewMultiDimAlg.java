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
 * This class implements the multidimensional learning algorithm...
 * 
 * Here brand new algorithm which explores the entire problem space and 
 * finds difficult spots wherever they are
 * 
 * All methods are synchronized
 *
 */

//Note: Important that all numbers are expressed with DOUBLE precision, otherwise
//we see deviations from matlab... - remember to do this with all numbers even
//if declared on the fly eg. multip = 1d + (0.5d * OMEGA).... etc.

package org.unicog.numberrace.algorithms;

import java.util.Random; //needed for random class
import org.jmat.data.Matrix; //Classes imported from matrix package
import org.jmat.data.AbstractDoubleArray;


import java.util.ArrayList; //for candidate coords (we use this b/c a lot faster than Matrix)

public class NewMultiDimAlg {
    //pointers

    //constants
    private final double ALPHA = 0.7d; //algorithm learning rate adjustment factor
    private final double DESIRED_SUCCESS = 0.75d; //desired success rate
    private final double NORM_CUTOFF = 2.5d; //number of standard deviations to use for cutoff in
                                             //generation of random normally distributed numbers
    private final double[] ST_DEVS = { 0.05d, 0.05d, 0.05d };
    //	private final double[] START_KNOWLEDGE = {0.4d, 0.4d, 0.4d};	//0.4 assumed starting knowledge of child
    public static final int MODEL_SIZE = 20;
    public static final int[] NUM_AXIS_DIVISIONS = { 20, 20, 20 }; //number of discrete categories each
    //axis is divided into when calculating estimated difficulty
    private final int[] GENERALIZING_DISTANCE = { 4, 4, 4 }; //distance to generalize when learning
    private final int MODEL_THREASHOLD = 5; //threashold for model to be sufficiently full
    private final double OMEGA = 0.5d; //child learning rate adjustment factor
    private final double TOLERANCE = 0.05d; //tolerance for difficulty of point chosen
    private final double TOLERANCE_INCREMENT = 0.02d;
    private final double SIG_SLOPE_SLOW = 10d;
    private final double SIG_SLOPE_FAST = 40d;

    //variables
    private boolean ready;
    private int numDimensions;
    private double[] coordsChosen; //discrete coordinates chosen for next problem
    private Matrix meanDifficulty; //the above converted into a continuous value
    private double edChosen; //the estimated difficulty of the chosen problem
    private double currDesir; //the current desired difficulty
    private double meanYmat; //the mean success in the last n trials
    private Matrix axisDivisions = new Matrix(1, AdapDimensions.NUM_ADAP_DIMS);
    private boolean modelFull; //true if we have the max num of trials included in a regression
    private boolean modelSufficientlyFull; //true if we have enough trials to run a regression, but not the max
    private Matrix modelData; //stores the data, including outcomes (i.e. is Djt;yt)
    private int dataLine; //keeps track of line of data we are up to in entry process, capped at numTrials
    private Matrix currentDifficulty; //a row vector, the new trial probs that the algorithm returns
    private Matrix3D estimatedDifficulty;
    double[] indexCurrDifficulty;
    Random randomNumber = new Random(); //random number generator

    //constructor
    public NewMultiDimAlg(int numDimensions, int aStartLevel) {
        this.numDimensions = numDimensions;
        for (int i = 0; i < numDimensions; i++) { //convert the int[] to a Matrix
            axisDivisions.set(0, i, NUM_AXIS_DIVISIONS[i]);
        }
        modelData = new Matrix(MODEL_SIZE, (numDimensions + 1)); //initializes to zero matrix
        estimatedDifficulty = new Matrix3D(NUM_AXIS_DIVISIONS[0],
                NUM_AXIS_DIVISIONS[1], NUM_AXIS_DIVISIONS[2], 0.5);
        estimatedDifficulty.set(0, 0, 0, DESIRED_SUCCESS);
        setStartKnowledge(aStartLevel);
        currentDifficulty = new Matrix(1, numDimensions);
        indexCurrDifficulty = new double[AdapDimensions.NUM_ADAP_DIMS];
        dataLine = 0;
        modelFull = false;
        modelSufficientlyFull = false;
        randomNumber.setSeed(System.currentTimeMillis());
        setReady(true);
    }

    public boolean start() {
        if (isReady()) {
            return true;
        }
        return false;
    }

    public boolean addTrial(Matrix dataPoint) {
        synchronized (this) {
            //check the number of dimensions is correct
            if ((dataPoint.getColumnDimension() == (numDimensions + 1))
                    && (dataPoint.getRowDimension() == 1)) {
                //now add the data line to the array
                if (modelFull == false) {
                    modelData.setRow(dataLine, dataPoint); //set the current row
                    dataLine++; //change marker to next row
                } else {
                    modelData = (Matrix) modelData.deleteRow(0); //delete the first row (first in, first out)
                    modelData = (Matrix) modelData.insertRows((MODEL_SIZE - 1),
                                                              dataPoint); //add the new row (last in, last out)		
                }
                //check if the model is at threashold /full
                checkIfModelAtThreashold();
                checkIfModelFull();

                //run the learning algorithm
                boolean success;
                if (dataPoint.get(0, AdapDimensions.NUM_ADAP_DIMS) == 0)
                    success = false;
                else
                    success = true;

                runLearningAlg(success);

                return true;
            } else
                //if dimensions are wrong
                return false;
        }
    }

    public Matrix getGeneratedDataPoint() {
        synchronized (this) {
            // select a problem of the desired difficulty 

            // first find which level of difficulty is desired, given the child's
            // recent history of success and failures
            int numTrials;
            if (modelFull)
                numTrials = MODEL_SIZE;
            else
                numTrials = dataLine;

            if (modelSufficientlyFull) {
                Matrix yMat = (Matrix) modelData.getSubMatrix(0,
                                                              (numTrials - 1),
                                                              numDimensions,
                                                              numDimensions);
                meanYmat = yMat.mean().toDouble();
                currDesir = DESIRED_SUCCESS - ALPHA
                        * (meanYmat - DESIRED_SUCCESS);
            } else {
                currDesir = DESIRED_SUCCESS;
            }

            // then scan the matrix and find some points close to this value
            Matrix3D proximity = estimatedDifficulty.minus(currDesir).ebeAbs();
            //proximity.getSubMatrix(0,4,0,4,0,4).toCommandLine("Proximity: ");
            //old code used in Study 1  //coordsChosen = proximity.getRandMinimum(randomNumber.nextDouble());
            double currentTolerance = TOLERANCE;
            //Matrix candidateCoords = null;
            ArrayList candidateCoords;
            int its = 0;
            do {
                its++;
                //System.out.println("Tolerance iteration: " + its);
                //System.out.println("Current tolerance: " + currentTolerance);
                candidateCoords = proximity.findIndicesLessThan_v2ArrayList(currentTolerance);
                if (candidateCoords.isEmpty()) {
                    currentTolerance += TOLERANCE_INCREMENT;
                }
            } while (candidateCoords.isEmpty());
            //candidateCoords.toCommandLine("Candidate Coords:");
            //System.out.println("Final Tolerance = " + currentTolerance);

            // choose one of these points at random
            //int rowPicked = (int)(Math.ceil(tester.getRandomNumber(0)*candidateCoords.getRowDimension())-1);
            candidateCoords.trimToSize();
            int rowPicked = (int) (Math.ceil(randomNumber.nextDouble()
                    * candidateCoords.size()) - 1);
            //coordsChosen = (Matrix)candidateCoords.getRow(rowPicked);
            int[] coordsChosenInt = (int[]) candidateCoords.get(rowPicked); //the candidateCoords are ints to save memory
            coordsChosen = new double[3];
            coordsChosen[0] = (double) coordsChosenInt[0];
            coordsChosen[1] = (double) coordsChosenInt[1];
            coordsChosen[2] = (double) coordsChosenInt[2];
            edChosen = estimatedDifficulty.get(coordsChosenInt[0],
                                               coordsChosenInt[1],
                                               coordsChosenInt[2]);
            Matrix coordsChosenMatrix = new Matrix(coordsChosen, 1);
            meanDifficulty = (Matrix) coordsChosenMatrix.plus(0.5d)
                                                        .ebeDivide(axisDivisions);
            //we add 0.5 because we are actually adding 1 to convert from 0 to 1 indexed, and
            //then subtracting 0.5 to get to the middle of the category

            //Randomly sample the point with the parameters calculated
            double tmpValue;
            for (int i = 0; i < numDimensions; i++) {
                do {
                    //generate point				
                    //double chosenNumber = randomNumber.nextGaussian();	
                    double chosenNumber = randomNumber.nextGaussian();
                    tmpValue = (chosenNumber * ST_DEVS[i] + meanDifficulty.get(0,
                                                                               i));
                    //institute bounding of distribution	
                    //*** check - are we sure we don't want to still do this???			
                    //��if((tmpValue>0.0d)&&(tmpValue<1.0d)&&
                    //	(tmpValue<(double)((NORM_CUTOFF*ST_DEVS)+mean_Dj))&&
                    //	(tmpValue>(double)((NORM_CUTOFF*ST_DEVS*-1)+mean_Dj))){
                    if (tmpValue < 0)
                        tmpValue = 0d;
                    if (tmpValue > 1)
                        tmpValue = 1d;
                    currentDifficulty.set(0, i, tmpValue);
                    break;
                    //}

                } while (true);//Re-sample if constraints not met
            }
            //return the resulting data point
            return currentDifficulty;
        }
    }

    private void runLearningAlg(boolean success) {
        // update our estimate of the difficulty
        synchronized (this) { //this prevents a dataPoint from being returned until algorithm is run	
            //Index the current difficulty location in the coordinates of the axis division system
            for (int i = 0; i < AdapDimensions.NUM_ADAP_DIMS; i++) {
                indexCurrDifficulty[i] = currentDifficulty.get(0, i)
                        * NUM_AXIS_DIVISIONS[i];
                indexCurrDifficulty[i] = Math.floor(indexCurrDifficulty[i]);
                if (indexCurrDifficulty[i] > (NUM_AXIS_DIVISIONS[i] - 1))
                    indexCurrDifficulty[i] = (NUM_AXIS_DIVISIONS[i] - 1);
            }
            //declare a few variables needed in all loops below
            double multip;
            double booleanSuccess;
            if (success)
                booleanSuccess = 1d;
            else
                booleanSuccess = 0d;

            //generalize weakly to neighboring problems
            for (int i = (int) Math.max(0,
                                        (indexCurrDifficulty[0] - GENERALIZING_DISTANCE[0])); i <= Math.min((NUM_AXIS_DIVISIONS[0] - 1),
                                                                                                            (indexCurrDifficulty[0] + GENERALIZING_DISTANCE[0])); i++) {
                for (int j = (int) Math.max(0,
                                            (indexCurrDifficulty[1] - GENERALIZING_DISTANCE[1])); j <= Math.min((NUM_AXIS_DIVISIONS[1] - 1),
                                                                                                                (indexCurrDifficulty[1] + GENERALIZING_DISTANCE[1])); j++) {
                    for (int k = (int) Math.max(0,
                                                (indexCurrDifficulty[2] - GENERALIZING_DISTANCE[2])); k <= Math.min((NUM_AXIS_DIVISIONS[2] - 1),
                                                                                                                    (indexCurrDifficulty[2] + GENERALIZING_DISTANCE[2])); k++) {
                        double dist = Math.abs(i - indexCurrDifficulty[0])
                                + Math.abs(j - indexCurrDifficulty[1])
                                + Math.abs(k - indexCurrDifficulty[2]);
                        if (dist < 5) {
                            multip = 1d - (dist / 5d);
                            estimatedDifficulty.set(i,
                                                    j,
                                                    k,
                                                    ((1d - (OMEGA * multip))
                                                            * estimatedDifficulty.get(i,
                                                                                      j,
                                                                                      k) + OMEGA
                                                            * multip
                                                            * booleanSuccess));
                        }
                    }
                }
            }

            // in case of success, generalize to all simpler problems
            if (success) {
                for (int i = 0; i <= (indexCurrDifficulty[0]); i++) { //note: consistent with matlab, removed the -1 here so that
                    for (int j = 0; j <= (indexCurrDifficulty[1]); j++) { //problems of equal difficulty are updated
                        for (int k = 0; k <= (indexCurrDifficulty[2]); k++) {
                            multip = 1d;
                            estimatedDifficulty.set(i,
                                                    j,
                                                    k,
                                                    ((1d - (0.5d * OMEGA * multip))
                                                            * estimatedDifficulty.get(i,
                                                                                      j,
                                                                                      k) + 0.5d
                                                            * OMEGA
                                                            * multip
                                                            * booleanSuccess));
                        }
                    }
                }
            }

            // in case of failure, generalize to all harder problems
            else {
                //System.out.println("i,j,k,tmpValue");
                for (int i = (int) indexCurrDifficulty[0]; i < NUM_AXIS_DIVISIONS[0]; i++) {
                    for (int j = (int) indexCurrDifficulty[1]; j < NUM_AXIS_DIVISIONS[1]; j++) {
                        for (int k = (int) indexCurrDifficulty[2]; k < NUM_AXIS_DIVISIONS[2]; k++) {
                            multip = 1d;
                            double tmpValue = estimatedDifficulty.get(i, j, k);
                            tmpValue = ((1d - (0.5d * OMEGA * multip))
                                    * tmpValue + 0.5d * OMEGA * multip * 0.5d);
                            estimatedDifficulty.set(i, j, k, tmpValue);
                            //System.out.println(i + "," + j + "," + k + "," + tmpValue);	
                        }
                    }
                }
            }
        }//synchronized
    }//runLearningAlg

    public boolean isModelSufficientlyFull() {
        return modelSufficientlyFull;
    }

    public Matrix getModelData() {
        synchronized (this) {
            return modelData;
        }
    }

    public boolean setModelData(Matrix modelDataToSet) {
        synchronized (this) {
            //check matrix is the right size
            if ((modelDataToSet.getColumnDimension() == (numDimensions + 1))
                    && (modelDataToSet.getRowDimension() >= 1)
                    && (modelDataToSet.getRowDimension() <= MODEL_SIZE)) {
                addDataLoop: for (int i = 0; i < MODEL_SIZE; i++) {
                    //set the data line
                    dataLine = i;
                    //check there is some data in this line
                    if ((modelDataToSet.get(i, 0) != 0)
                            && (modelDataToSet.get(i, 1) != 0)
                            && (modelDataToSet.get(i, 2) != 0)) {
                        modelData.setRow(i,
                                         (AbstractDoubleArray) modelDataToSet.getRow(i));
                    } else { //we are done adding data
                        break addDataLoop;
                    }
                }
                checkIfModelAtThreashold();
                checkIfModelFull();
                return true;
            }
            //if not the right size return false
            else
                return false;
        }
    }

    private void setStartKnowledge(int aStartLevel) {
        //convert startLevel to 0-1 scale
        double convStartLvl = (aStartLevel - 1d) / 14d;
        double[] startKnowledge = new double[] { convStartLvl, convStartLvl,
                convStartLvl };

        for (int i = 0; i < (Math.min(NUM_AXIS_DIVISIONS[0],
                                      Math.floor(NUM_AXIS_DIVISIONS[0]
                                              * startKnowledge[0] * 2))); i++) { //note: consistent with matlab, removed the -1 here so that
            for (int j = 0; j < (Math.min(NUM_AXIS_DIVISIONS[1],
                                          Math.floor(NUM_AXIS_DIVISIONS[1]
                                                  * startKnowledge[1] * 2))); j++) { //problems of equal difficulty are updated
                for (int k = 0; k < (Math.min(NUM_AXIS_DIVISIONS[2],
                                              Math.floor(NUM_AXIS_DIVISIONS[2]
                                                      * startKnowledge[2] * 2))); k++) {
                    double tmpVal = 1d;
                    double[] location = new double[numDimensions];
                    location[0] = (double) (i + 1)
                            / (double) NUM_AXIS_DIVISIONS[0];
                    location[1] = (double) (j + 1)
                            / (double) NUM_AXIS_DIVISIONS[1];
                    location[2] = (double) (k + 1)
                            / (double) NUM_AXIS_DIVISIONS[2];
                    for (int d = 0; d < numDimensions; d++) {
                        if (location[d] <= startKnowledge[d]) {
                            tmpVal = tmpVal
                                    / (1d + Math.exp(SIG_SLOPE_SLOW
                                            * (location[d] - startKnowledge[d])));
                        } else {
                            tmpVal = tmpVal
                                    / (1d + Math.exp(SIG_SLOPE_FAST
                                            * (location[d] - startKnowledge[d])));
                        }
                    }
                    tmpVal = 0.5d + 0.5d * Math.pow(tmpVal, 0.3333d);
                    estimatedDifficulty.set(i, j, k, tmpVal);
                }
            }
        }
        //tester.saveEdMatrix(estimatedDifficulty);
    }

    private void checkIfModelAtThreashold() {
        //check if the model is at threadhold
        if (dataLine > (MODEL_THREASHOLD - 1)) {
            modelSufficientlyFull = true;
        }
    }

    private void checkIfModelFull() {
        //check if the model is full
        if (dataLine > (MODEL_SIZE - 1)) {
            modelFull = true;
        }
    }

    public Matrix3D getEdMatrix() {
        return estimatedDifficulty;
    }

    public void setEdMatrix(Matrix3D edMatrix) {
        estimatedDifficulty = edMatrix;
    }

    public Matrix getMeanDiff() {
        return meanDifficulty;
    }

    public Matrix getCoordsChosen() {
        return new Matrix(coordsChosen, 1);
    }

    public double getEdChosen() {
        return edChosen;
    }

    public double getMeanYmat() {
        return meanYmat;
    }

    public double getCurrDesir() {
        return currDesir;
    }

    public void setReady(boolean state) {
        ready = state;
    }

    public boolean isReady() {
        return ready;
    }
}