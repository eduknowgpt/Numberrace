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
 *This class implements a quick and dirty 3D Matrix, providing
 *only the methods needed for the second learning algorithm.
 * 
 */

package org.unicog.numberrace.algorithms;

// Because this one saved in data file using serialization mechanism can not move it to another package easily :(

import org.jmat.data.Matrix; //Classes imported from matrix package
import org.jmat.data.AbstractDoubleArray;
import java.util.Vector;
import java.util.ArrayList;
import java.io.Serializable;

public class Matrix3D implements Serializable {

    //The 3D Matrix is composed of z-sized array of (x by y) Matrix objects
    private Matrix[] matrixArray;
    private int numRows;
    private int numColumns;
    private int numLayers;

    /**
    Constructs a double Matrix3D will all elements == 0
    Matlab equiv. is zeros(x,y,z)
    **/
    public Matrix3D(int x, int y, int z) {
        matrixArray = new Matrix[z];
        for (int i = 0; i < z; i++) {
            matrixArray[i] = new Matrix(x, y);
        }
        numRows = x;
        numColumns = y;
        numLayers = z;
    }

    /**
    Constructs a double Matrix3D will all elements == k
    **/
    public Matrix3D(int x, int y, int z, double k) {
        matrixArray = new Matrix[z];
        for (int i = 0; i < z; i++) {
            matrixArray[i] = new Matrix(x, y, k);
        }
        numRows = x;
        numColumns = y;
        numLayers = z;
    }

    /**
    Constructs a double Matrix3D from a Matrix array
    **/
    public Matrix3D(Matrix[] inputMatrixArray) {
        matrixArray = inputMatrixArray;
        numRows = matrixArray[0].getRowDimension();
        numColumns = matrixArray[0].getColumnDimension();
        numLayers = inputMatrixArray.length;
    }

    /**
    Sets the element (x,y,z) equal to k
    **/
    public boolean set(int x, int y, int z, double k) {
        matrixArray[z].set(x, y, k);
        return true;
    }

    /**
     * Sets a whole layer of a 3D Matrix
     * 
     * @param layerToSet A Matrix of correct dimensions
     * @param zIndex The z index of the layer
     */
    public boolean setLayer(Matrix layerToSet, int zIndex) {
        if ((layerToSet.getRowDimension() == numRows)
                && (layerToSet.getColumnDimension() == numColumns)) {
            matrixArray[zIndex] = layerToSet;
            return true;
        } else {
            System.out.println("Error: Matrix3D.setLayer: dimensions do not agree"); //$NON-NLS-1$
            return false;
        }
    }

    /**
    Gets the element (x,y,z)
    **/
    public double get(int x, int y, int z) {
        return matrixArray[z].get(x, y);
    }

    /**
    Subtracts a constant from each value in the matrix
    **/
    public Matrix3D minus(double k) {
        Matrix[] result = new Matrix[numLayers];
        for (int i = 0; i < numLayers; i++) {
            result[i] = (Matrix) matrixArray[i].minus(k);
        }
        return new Matrix3D(result);
    }

    /**
    Returns a Matrix which is the absolute value of all the elements
    **/
    public Matrix3D ebeAbs() {
        Matrix[] result = new Matrix[numLayers];
        for (int i = 0; i < numLayers; i++) {
            result[i] = (Matrix) matrixArray[i].ebeAbs();
        }
        return new Matrix3D(result);
    }

    /**
     * Returns a specified sub matrix
     * 
     * @param xStart initial row index
     * @param xEnd final row index
     * @param yStart initial column index
     * @param yEnd final column index
     * @param zStart initial layer index
     * @param zEnd final layer index
     * @return a Matrix3D 
     */
    public Matrix3D getSubMatrix(int xStart, int xEnd, int yStart, int yEnd,
            int zStart, int zEnd) {
        Matrix3D subMatrix = new Matrix3D(xEnd - xStart + 1, yEnd - yStart + 1,
                zEnd - zStart + 1);
        for (int z = zStart; z < (zEnd + 1); z++) {
            Matrix layerSubMatrix = (Matrix) matrixArray[z].getSubMatrix(xStart,
                                                                         xEnd,
                                                                         yStart,
                                                                         yEnd);
            subMatrix.setLayer(layerSubMatrix, z - zStart);
        }
        return subMatrix;
    }

    /**
    Finds values in the matrix which meet test
    and returns their indices
    e.g. if test is "<" and val is 0.3
    Will return an index matrix of all coordinates < 0.3
    **/
    public Matrix findIndicesLessThan(double val) {
        Matrix indexMatrix = null; //holds all the valid indices found
        Matrix xyzCoords = new Matrix(1, 3); //holds the current index
        double currentValue;
        for (int z = 0; z < numLayers; z++) { //ascending z values
            for (int y = 0; y < numColumns; y++) { //ascending y values
                for (int x = 0; x < numRows; x++) { //ascending x values
                    currentValue = this.get(x, y, z);
                    if (currentValue < val) {
                        xyzCoords.set(0, 0, x);
                        xyzCoords.set(0, 1, y);
                        xyzCoords.set(0, 2, z);
                        if (indexMatrix != null) {
                            indexMatrix.insertRowsEquals(indexMatrix.getRowDimension(),
                                                         (AbstractDoubleArray) xyzCoords);
                        } else {
                            indexMatrix = (Matrix) xyzCoords.copy();
                        }
                    }
                }
            }
        }
        return indexMatrix;
    }

    /**
    Finds values in the matrix which meet test
    and returns their indices
    e.g. if test is "<" and val is 0.3
    Will return an index matrix of all coordinates < 0.3
    **/
    public ArrayList findIndicesLessThan_v2ArrayList(double val) {
        ArrayList foundIndices = new ArrayList(); //holds all the valid indices found
        double currentValue;
        for (int z = 0; z < numLayers; z++) { //ascending z values
            for (int y = 0; y < numColumns; y++) { //ascending y values
                for (int x = 0; x < numRows; x++) { //ascending x values
                    currentValue = this.get(x, y, z);
                    if (currentValue < val) {
                        int[] xyzCoords = new int[3]; //int array for the current index
                        xyzCoords[0] = x;
                        xyzCoords[1] = y;
                        xyzCoords[2] = z;
                        foundIndices.add(xyzCoords);
                    }
                }
            }
        }
        return foundIndices;
    }

    /**
    Finds minimum value in the matrix and returns its indices.
    If there are more than one minima, the function finds out how
    many there are, and then picks one at random.
    **/
    public Matrix getRandMinimum(double randNumber) {
        //pass once through the whole matrix and find the min
        double currentMin = this.get(0, 0, 0); //stores min value found so far
        double currentValue;
        for (int z = 0; z < numLayers; z++) {
            for (int y = 0; y < numColumns; y++) {
                for (int x = 0; x < numRows; x++) {
                    currentValue = this.get(x, y, z);
                    if (currentValue < currentMin) {
                        currentMin = currentValue;
                    }
                }
            }
        }

        //now pass through the whole matrix again, and collect up all the minima
        Vector minima = new Vector();
        for (int z = 0; z < numLayers; z++) {
            for (int y = 0; y < numColumns; y++) {
                for (int x = 0; x < numRows; x++) {
                    currentValue = this.get(x, y, z);
                    if (currentValue == currentMin) {
                        Matrix indexMatrix = new Matrix(1, 3);
                        indexMatrix.set(0, 0, x);
                        indexMatrix.set(0, 1, y);
                        indexMatrix.set(0, 2, z);
                        minima.add(indexMatrix);
                    }
                }
            }
        }

        //pick one of the collected minima at random
        int chosenIndex = (int) Math.floor(randNumber * minima.size());
        Matrix indexMatrix = (Matrix) minima.get(chosenIndex);
        return indexMatrix;
    }

    /**
     * Returns the same of the determinants of each layer
     * Use to check for changes in matrix
     * 
     **/
    public double sum() {
        double sum = 0;
        for (int i = 0; i < numLayers; i++) {
            sum = sum + matrixArray[i].sum().transpose().sum().toDouble();
        }
        return sum;
    }

    public void toCommandLine(String label) {
        System.out.println(label);
        for (int i = 0; i < numLayers; i++) {
            matrixArray[i].toCommandLine("layer " + i + " ="); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
    Finds values in the matrix which meet test
    and returns their indices
    e.g. if test is "<" and val is 0.3
    Will return an index matrix of all coordinates < 0.3
    **/
    //This never worked, because when you pass the string "<", it comes through as ""
    /*	public Matrix findIndicesOld(String test, double val){
    		Matrix indexMatrix = null;
    		for(int i=(numLayers-1);i>=0;i--){	//loop provides z values
    			//System.out.println("Matrix3D.find i = " + i);
    			Matrix tmpIndexMatrix;	//used to hold values from find function
    			Matrix zIndexMatrix;	//used to insert z values
    			tmpIndexMatrix = (Matrix)matrixArray[i].find(test,val).getIndexMatrix();	//2D find
    			Tester.sortFoundMatrixRowsbyYX(tmpIndexMatrix);	//sort by y(desc) then x(desc)
    			//tmpIndexMatrix.toCommandLine("tmpIndexMatrix before merge");
    			int tmpZLength = tmpIndexMatrix.getRowDimension();	
    			if(tmpZLength>0){	//make column for z values
    				zIndexMatrix = new Matrix(tmpZLength,1,i);	//column to insert
    				//debug:
    				//tmpIndexMatrix.toCommandLine("tmpIndexMatrix");
    				//zIndexMatrix.toCommandLine("zIndexMatrix");
    				tmpIndexMatrix.mergeColumnsEquals(zIndexMatrix);	//insert the column	
    				//tmpIndexMatrix.toCommandLine("tmpIndexMatrix after merge");
    				if(indexMatrix==null)
    					indexMatrix = tmpIndexMatrix;
    				else
    					indexMatrix.mergeRowsEquals((AbstractDoubleArray)tmpIndexMatrix);				
    				//indexMatrix.toCommandLine("updated Index matrix");
    			}
    		}
    		return indexMatrix;
    	}*/

}