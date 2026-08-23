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
 * Stores relevant parameters of a DotArray (or group of associated DotSprites)
 * Contains a java version of matlab algorithm to produce density and area independent dot displays.
 * It also instantiates the DotSprite objects.
 * Note: The DotArray does not paint itself, instead renders to Graphics2D object which is
 * passed to it by DotContainer
 * 
 */

package org.unicog.numberrace.others;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Random;

import org.jmat.data.Matrix;
import org.jmat.data.matrixTools.Shuffle;
import org.jmat.data.matrixTools.Sorting;
import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.algorithms.AdapDimensions;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.ScrollSafeLinePath;
import org.unicog.numberrace.util.Utilities;

import com.samskivert.swing.util.SwingUtil;
import com.threerings.media.AbstractMedia;
import com.threerings.media.FrameManager;
import com.threerings.media.FrameParticipant;
import com.threerings.media.MediaPanel;
import com.threerings.media.ViewTracker;
import com.threerings.media.image.BufferedMirage;
import com.threerings.media.image.Mirage;
import com.threerings.media.sprite.FadableImageSprite;
import com.threerings.media.util.LinePath;

//Note: Matrix algebra functions from JMAT package, see jmat.sourceforge.net,
//based on Jama matrix package, developed by NIST, see http://math.nist.gov/javanumerics/jama/

public class DotArray implements FrameParticipant, ViewTracker {

    private static int DOT_BORDER_SIZE = 2;

    private final class DotSprite extends FadableImageSprite {
        private boolean visible;
        private int d;
        private int r;

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setDiameter(int diameter) {
            diameter += DOT_BORDER_SIZE;
            if (diameter != d) {
                d = diameter;
                r = diameter >> 1;
                setMirage(getMirage(diameter));
            }
        }

        public void comleteFadeOut() {
            _fadeOutDuration = -1;
        }

        public int getRadius() {
            return r;
        }

        @Override
        public void viewLocationDidChange(int dx, int dy) {
            super.viewLocationDidChange(dx, dy);
            if (_path != null && _path instanceof ViewTracker) {
                ((ViewTracker) _path).viewLocationDidChange(dx, dy);
            }
            //            setLocation(_ox + dx, _oy + dy);
        }
    }

    private static HashMap<Integer, Mirage> cachedDots = new HashMap<Integer, Mirage>();
    private static Stroke dotStroke = new BasicStroke(DOT_BORDER_SIZE);
    private static Color dot_color;
    private static Color dot_stroke_color;

    private static Mirage getMirage(int diameter) {
        Mirage mirage = cachedDots.get(diameter);
        if (mirage == null) {
            BufferedImage img = ImageFactory.createCompatibleTranslucentImage(diameter + 2,
                                                                              diameter + 2);
            Graphics2D g2d = img.createGraphics();
            SwingUtil.activateAntiAliasing(g2d);
            g2d.setColor(dot_color);
            g2d.setStroke(dotStroke);
            g2d.fillOval(1, 1, diameter - 1, diameter - 1);
            g2d.setColor(dot_stroke_color);
            g2d.drawOval(1, 1, diameter - 1, diameter - 1);
            g2d.dispose();
            mirage = new BufferedMirage(img);
            cachedDots.put(diameter, mirage);
        }
        return mirage;
    }

    private int id; //side of dot array (left,right)

    //state attributes
    //    private boolean drawDotstoDotArray = true; //true if dots are drawn to this object

    //object attributes
    private int numberDots; //number of dots to display at a given time

    //subobjects
    public DotSprite[] dotList; //declares array of DotSprites for the dots

    //utility variables
    private Random randomNumber;
    private float fadeAlpha = 1f; //this is used to fade the dots

    //notation dimension attributes:
    private boolean dotsVisible = false;//whether the dots owned by this object are visible or not
    //used for analogMagStims parameter
    private boolean dotsFade = false; //whether dots fade after initial presentation or not
    private int fadeTime; //time taken for dots to fade

    //dot algorithm attributes:
    private boolean fixedItemSize; //If this is true, radius is identical for all dots
    // total dark area therefore covaries with number
    private boolean fixedDensity; //If this is true, spacing of dots is constant on average
    //occupied area is therefore bigger for larger numbers
    private int maxNumDots; //max number of dots allowed
    private double rmax; //max radius of dots allowed
    private double rmin; //min radius of dots allowed
    private Matrix gridCoords; //Coordinates of grid, changes when maxNumDots changes
    //	note: assumes dotArray is square
    private double radiusMultiplier;
    private double convertedMaxR;
    private int n_dots; //this is the number of potential points in the grid finally

    private boolean changeVisibility;

    private boolean visible;

    private final MediaPanel mp;
    private GameObject go;

    private Point[] lineUpPoints;

    private boolean lineUp;
    private int first2lineUp;
    private int last2lineUp;
    private Object lineUpObserver;
    private boolean toUnfade;

    private Rectangle[] circles = new Rectangle[3];
    private final FrameManager frameManager;
    private int lineUpDelay;
    private int borderY;
    private Object perDotObserver;

    public DotArray(int id, FrameManager frameManager, MediaPanel mediaPanel) {
        this.id = id;
        this.frameManager = frameManager;
        this.mp = mediaPanel;
        setMaxNumDots(AdapDimensions.XMAX_FULL_RANGE + 1); //note, add one because we need some of 10
        go = GameObject.getInstance();
    }

    public void load() {
        dot_color = go.getTheme().dotColor;
        dot_stroke_color = Color.BLACK;

        randomNumber = new Random();
        randomNumber.setSeed(System.currentTimeMillis()); //seed with system time in msec
        //offScreenDotArray = Utilities.createImage(choiceScreen,width,height);
        //Create a shape array for the dots
        dotList = new DotSprite[maxNumDots]; //creates Dotsprite array object
        for (int i = 0; i < maxNumDots; i++) {
            dotList[i] = new DotSprite();
        }
        frameManager.registerFrameParticipant(this);
    }

    public void unload() {
        frameManager.removeFrameParticipant(this);
        randomNumber = null;
        dotList = null;
    }

    public boolean start() {
        return true;
    }

    private void setDotPositions(Rectangle bounds, int startNum, int nDots) {
        //This is based on the second half of Stan's matlab algorithm
        //Note: unlike matlab, the arrays in class JMAT are zero indexed

        double r; //radius of dots for this trial
        double convertedR; //will store radius on same scale as screen
        int maxCoords = gridCoords.getRowDimension(); //the number of possible coords in the grid
        int usedCoords;

        // manipulation of the radius of dots
        if (fixedItemSize) //if dots will stay the same size
            rmax = Math.min((((1d / n_dots)) / 1.7d), 0.1d); // this is the maximum radius of a given dot
        //rmax goes from 0.6(1) to 0.1 (5+)
        else {
            if (fixedDensity)
                rmax = 0.1; //note: this used to be 0.1 / sqrt(minnum)
            else
                rmax = 0.2; //note: this used to be 0.2 / sqrt(minnum)
        }

        // given the spacing n of the location matrix
        rmin = rmax * Math.sqrt(1 / (double) maxNumDots); //used to be = rmax*sqrt(minnum/maxnum);
        //will be rmax * 1/3 for min = 1 and max = 9

        //System.out.println("With fixedItemSize = " + fixedItemSize);	//debug
        //System.out.println("With fixedDensity = " + fixedDensity);
        //System.out.println("rmax: " + rmax);
        //System.out.println("rmin: " + rmin);
        //Calculate the Radius and spacing parameters, accoring to nDots
        if (fixedItemSize)
            r = rmax;
        // r=rmin + (rmax-rmin)*rand; 
        // radius is identical for all numerosities
        // total dark area therefore covaries with number
        else
            //assume (fixedDensity)
            r = rmax * Math.sqrt(1 / (double) nDots); //used to be: =rmax*sqrt(minnum/number)
        // radius covaries with numerosity
        // total dark area is constant

        radiusMultiplier = bounds.width >> 1; //note: assumes dotArray is square
        //System.out.println("DA: id: " + this.id);
        //System.out.println("DA: radMultiplier: " + radiusMultiplier);
        convertedMaxR = rmax * radiusMultiplier;
        //System.out.println("DA: convertedMaxR: " + convertedMaxR);
        convertedR = r * radiusMultiplier; //convert r to screen scale

        //System.out.println("nDots: " + nDots);
        //System.out.println("r: " + r);		//debug
        //System.out.println("radiusMultiplier: " + radiusMultiplier);
        //System.out.println("convertedR: " + convertedR);

        usedCoords = Math.round(nDots * 1.3f);
        // the 1.3 factor ensures some variability in which locations get used
        if (usedCoords > maxCoords)
            usedCoords = maxCoords;
        if (usedCoords == 0) //make sure there is at least one usedCoord
            usedCoords++;

        //System.out.println("usedCoords: " + usedCoords);	//debug

        //Calculate positions for the dots
        double roffset;
        double scaleFactor;
        double tmpRandom; //done like this for debugging

        if (fixedDensity) {
            // spacing of dots is constant on average
            // occupied area is therefore bigger for larger numbers
            // This is achieved by using only a subset of locations 
            tmpRandom = randomNumber.nextDouble();
            roffset = 0.9d * tmpRandom
                    * (1 - Math.sqrt(usedCoords / (double) maxCoords));
            scaleFactor = 1;

            //System.out.println("tmpRandom for roffset: " + tmpRandom);	//debug
        } else // test stimuli
        {
            roffset = 0;
            scaleFactor = 0.9d * Math.sqrt(maxCoords / usedCoords);
            //Note: Above line changed from (maxCoords/usedCoords)
            // average spacing of dots covaries inversely with number 
            // total occupied area is thus roughly constant (for large numbers)
            // this area must be compressed a bit so that the corresponding
            // average dot spacing has been presented in the habituation set
        }

        //System.out.println("roffset: " + roffset);	//debug
        //System.out.println("scaleFactor" + scaleFactor);

        tmpRandom = randomNumber.nextDouble();
        //System.out.println("tmpRandom for omega: " + tmpRandom);	//debug

        double omega = 2 * Math.PI * tmpRandom;
        //System.out.println("omega: " + omega);

        Matrix offset = new Matrix(1, 2);
        offset.set(0, 0, (roffset * Math.sin(omega)));
        offset.set(0, 1, (roffset * Math.cos(omega)));
        //so there will be no offset for non fixed density - why??

        //offset.toCommandLine("offset: ");

        //shuffle to select a random subset of the used coords
        Matrix tmp1 = new Matrix(usedCoords, 1);
        for (int i = 0; i < usedCoords; i++)
            tmp1.set(i, 0, (i + 1));

        Matrix order = (Matrix) Shuffle.MatrixRows(tmp1);

        //order.toCommandLine("order: ");

        // add random rotation
        tmpRandom = randomNumber.nextDouble();
        //System.out.println("tmpRandom for omega: " + tmpRandom);	//debug		
        omega = 2 * Math.PI * tmpRandom;
        //System.out.println("omega: " + omega);
        Matrix rotmat = new Matrix(2, 2);
        rotmat.set(0, 0, Math.sin(omega));
        rotmat.set(0, 1, Math.cos(omega));
        rotmat.set(1, 0, (-1 * Math.cos(omega)));
        rotmat.set(1, 1, Math.sin(omega));
        rotmat = (Matrix) rotmat.times(scaleFactor);

        //rotmat.toCommandLine("rotmat: ");

        // i just for order.get(i) because in prev version were used 3 dot arrays and always there counting done starting from 0, let's leave it as much old way as possible for now
        for (int i = 0, j = startNum; i < nDots; i++, j++) {
            //System.out.println("loop for dots (i): " + i);

            //first find the coordinates for this point
            Matrix startCoords = new Matrix(1, 2);
            startCoords.set(0,
                            0,
                            gridCoords.get((int) (order.get((i), 0) - 1), 0));
            startCoords.set(0,
                            1,
                            gridCoords.get((int) (order.get((i), 0) - 1), 1));

            //startCoords.toCommandLine("startCoords: ");

            //then calculate an individual random offset for this point
            Matrix indivRandOffset = new Matrix(1, 2);
            tmpRandom = randomNumber.nextDouble();
            //System.out.println("tmpRand1 for indivRandOffset: " + tmpRandom);
            indivRandOffset.set(0, 0, tmpRandom);
            tmpRandom = randomNumber.nextDouble();
            //System.out.println("tmpRand2 for indivRandOffset: " + tmpRandom);
            indivRandOffset.set(0, 1, tmpRandom);
            indivRandOffset = (Matrix) indivRandOffset.minus(0.5d);
            indivRandOffset = (Matrix) indivRandOffset.times(r);

            //indivRandOffset.toCommandLine("indivRandOffset: ");

            //finally, multiply by the rotation matrix, then add the individual
            //and group offsets
            Matrix finalCoords = new Matrix(1, 2);
            finalCoords = (Matrix) startCoords.times(rotmat);
            finalCoords.plusEquals(indivRandOffset);
            finalCoords.plusEquals(offset);

            //finalCoords.toCommandLine("finalCoords: ");

            Matrix dotPos = new Matrix(1, 2);
            dotPos.set(0, 0, finalCoords.get(0, 0));
            dotPos.set(0, 1, finalCoords.get(0, 1));

            //dotPos.toCommandLine("DotPos prior to size conversion: ");

            //now convert the location to be relative to the DotArray object
            //add 1 to shift the coordinate system, multiply by dimension/2, then minus radius
            //(Ellipse2D.Double draws from the top left corner)
            dotPos.set(0,
                       0,
                       (((dotPos.get(0, 0) + 1) * (bounds.width >> 1)) - convertedR));
            dotPos.set(0,
                       1,
                       (((dotPos.get(0, 1) + 1) * (bounds.width >> 1)) - convertedR));

            //dotPos.toCommandLine("DotPos after size conversion: ");

            //all this to implement one freaking line of matlab code!!
            //namely:	c = coords(order(i),:) * rotmat + ((rand(1,2)-0.5)/1)*r + offset;

            double diameter = convertedR * 2d;

            //reset the parameters of the dot sprite
            dotList[j].setLocation((int) dotPos.get(0, 0) + bounds.x,
                                   (int) dotPos.get(0, 1) + bounds.y);

            dotList[j].setDiameter((int) diameter);

            dotList[j].setVisible(true);

        } //next i,j

        //make the non-relevant dotSprites invisible
        for (int i = (nDots); i < maxNumDots; i++) {
            dotList[i].setVisible(false);
        }

    }

    private void setNewDotGrid()
    //This sets up a new grid of potential dot positions every time the maxNumDots changes
    //Translation of first half of Stan's matlab controlleddots.m function using JMAT package
    //Note: unlike Matlab, all matrices in JMAT package are zero indexed
    {
        int minNumDots = 1;
        int ndesired = Math.round(maxNumDots * 1.3f);
        int n = Math.round((float) Math.sqrt(ndesired));
        int numCoords;

        //debug:
        //System.out.println("minNumDots: " + minNumDots);
        //System.out.println("maxNumDots: " + maxNumDots);
        //System.out.println("ndesired: " + ndesired);

        int loopCounter = 1; //keeps track of the number of while iterations

        whileLoop: //label for while loop, so can break out later
        while (true) {
            /*
             * Note: for a maxnum of 9 the while loop runs through 3 iterations,
             * maybe more with smaller maxnums? Maybe multiply by more than 1.3?
             */

            numCoords = ((n + 1) * (n + 1));
            //System.out.println("n: " + n);
            //System.out.println("numCoords: " + numCoords);
            //System.out.println("while iteration number: " + loopCounter);

            Matrix initialCoords = new Matrix(numCoords, 2);
            /*
             * produces zeros matrix to initialize initialCoords is an n x 2
             * matrix of x,y coords
             */

            int a = 0; //counter for number of coordinates
            for (int i = 0; i <= n; i++) //one side of square
            {
                for (int j = 0; j <= n; j++) //other side of square
                {
                    double iDouble = i; //cast the integers as double so the maths
                    double jDouble = j; //comes out right - really annoying!!
                    double nDouble = n; //Is there a better way?

                    int iHalf = i >> 1;
                    int nHalf = n >> 1;
                    int jHalf = j >> 1;

                    initialCoords.set(a,
                                      0,
                                      ((i - nHalf + (((jDouble % 2) - 0.5) / 4)) / nHalf)); //the x coordinate
                    initialCoords.set(a,
                                      1,
                                      ((j - nHalf + (((iDouble % 2) - 0.5) / 4)) / nHalf)); //the y coordinate

                    //					initialCoords.set(a,0,((iDouble-(nDouble/2)+(((jDouble%2)-0.5)/4))/(nDouble/2)));	//the x coordinate
                    //					initialCoords.set(a,1,((jDouble-(nDouble/2)+(((iDouble%2)-0.5)/4))/(nDouble/2)));	//the y coordinate

                    a++;
                    //System.out.println("i " + i + "\tj: " + j + "\ta: " + a);
                    /*
                     * this generates a roughly square matrix, with offsets that
                     * prevent subjects from noticing the regular matrix
                     */
                }
            }

            //initialCoords.toCommandLine("initialCoords: ");		//debug

            //distance of each point from (0,0) by pythagoras 
            Matrix dist = new Matrix(numCoords, 1); //will be stored in vector dist
            //pythagoras loop
            for (int i = 0; i < numCoords; i++) {
                dist.set(i,
                         0,
                         Math.sqrt(Math.pow(initialCoords.get(i, 0), 2)
                                 + Math.pow(initialCoords.get(i, 1), 2)));
            }

            //dist.toCommandLine("dist: ");		//debug

            //temporary matrix for storing the coordinates along with a column at the left
            //of the matrix for T/F distance indicators
            Matrix tmpCoords = new Matrix(numCoords, 3);
            tmpCoords.setSubMatrix(0, 1, initialCoords); //copy in 2nd and 3rd rows from coords

            /*
             * Element by element check that dist < (1-1/sqrt(ndesired)) i.e.
             * points are within a circle, circle smaller if not many points
             * fill column 1 with elements set to 1 where relation true, and 0
             * if not.
             */
            int numGoodCoords = 0; //for counting how many coords are at correct distance			
            for (int i = 0; i < numCoords; i++) {
                if (dist.get(i, 0) < (1 - (1 / Math.sqrt(ndesired)))) {
                    numGoodCoords++;
                    tmpCoords.set(i, 0, 1);
                } else
                    tmpCoords.set(i, 0, 0);
            }

            //tmpCoords.toCommandLine("tmpCoords: ");		//debug

            if (numGoodCoords > ndesired) //check there's enough points
            {
                //if there are, store the points which are ok in the gridCoords matrix
                gridCoords = new Matrix(numGoodCoords, 2); //instantiate matrix of correct size
                //Put in the final coords (but then they have to be sorted - see below)
                int gridCoordsRow = 0; //counts which row we are up to in gridCoords
                for (int i = 0; i < numCoords; i++) {
                    if (tmpCoords.get(i, 0) == 1) {
                        gridCoords.set(gridCoordsRow, 0, tmpCoords.get(i, 1));
                        gridCoords.set(gridCoordsRow, 1, tmpCoords.get(i, 2));
                        gridCoordsRow++;
                    }
                }

                //gridCoords.toCommandLine("gridCoords prior to sorting: ");	//debug

                //Now sort the gridCoords Matrix by distance
                //First construct a temporary index and distance matrix (index is first column, dist second)
                Matrix distList = new Matrix(numGoodCoords, 2);

                for (int i = 0; i < numGoodCoords; i++) {
                    distList.set(i, 0, i); //index
                    distList.set(i,
                                 1,
                                 Math.sqrt(Math.pow(gridCoords.get(i, 0), 2)
                                         + Math.pow(gridCoords.get(i, 1), 2))); //dist
                }

                //distList.toCommandLine("distList prior to sorting: ");	//debug

                //Sort the distList matrix, store output as indexDist
                Matrix indexDist;
                indexDist = (Matrix) Sorting.MatrixRows(distList, 1); //sort by dist column

                //indexDist.toCommandLine("indexList: ");		//debug

                //Re-order the gridCoords Matrix according to the index list
                Matrix tmpGridCoords = (Matrix) gridCoords.copy(); //make a copy of the original

                //tmpGridCoords.toCommandLine("tmpGridCoords (should be a copy of gridCoords): ");	//debug

                for (int i = 0; i < numGoodCoords; i++) {
                    gridCoords.set(i,
                                   0,
                                   tmpGridCoords.get((int) indexDist.get(i, 0),
                                                     0));
                    gridCoords.set(i,
                                   1,
                                   tmpGridCoords.get((int) indexDist.get(i, 0),
                                                     1));
                }

                //gridCoords.toCommandLine("final gridCoords matrix: ");

                //Finally, exit the while loop			
                break whileLoop;
            } else
                //if not enough points, run through the while loop again
                loopCounter++;
            n = n + 1;
        } //while

        //Note: radius is manipulated in setDotPositions()
        n_dots = n; //this info needs to be stored for the calculations
    }

    public void setMaxNumDots(int num) {
        //WARN: called by NumCompAlgManager    !!!! NEVER CALLED OUTSIDE OF DOTARRAY CONSTRUCTUR !!!
        maxNumDots = num;
        setNewDotGrid();
    }

    public void setNumber(int numDots) {
        Utilities.log.info("Was :" + numDots + " will : " + numDots);
        numberDots = numDots;
        setDotPositions(circles[2], 0, numDots);
    }

    public void setSubNumber(int... subNumber) {
        assert subNumber.length > 0 && subNumber.length < 3;
        for (int i = 0; i < subNumber.length; i++) {
            setDotPositions(circles[i],
                            (i > 0) ? subNumber[i - 1] : 0,
                            subNumber[i]);
        }
    }

    public void reset() {
        dotsFade = false;
        fadeAlpha = 1f;
        setVisible(false);
    }

    public int getId() {
        return id;
    }

    public double getConvertedMaxR() {
        //returns the maximum dot radius in screen units
        return convertedMaxR;
    }

    public int getNumber() {
        return numberDots;
    }

    public void setDotsVisible(boolean showDots) {
        dotsVisible = showDots;
    }

    public boolean getDotsVisible() {
        return dotsVisible;
    }

    public void setDotsFade(boolean state) {
        dotsFade = state;
    }

    public boolean getDotsFade() {
        return dotsFade;
    }

    public void setFadeTime(int fadeTime) {
        this.fadeTime = fadeTime;
    }

    public void setFixedDensity(boolean state) {
        fixedDensity = state;
    }

    public void setFixedItemSize(boolean state) {
        fixedItemSize = state;
    }

    public void setBounds(Rectangle mainCircle, Rectangle leftCircle,
            Rectangle rightCircle) {
        circles[0] = leftCircle;
        circles[1] = rightCircle;
        circles[2] = mainCircle;
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible && dotsVisible == visible) {
            Utilities.log.warning("SAME VISIBILITY STATUS");
            return;
        }
        setDotsVisible(visible);
        this.visible = visible;

        changeVisibility = true;
    }

    public Component getComponent() {
        return null;
    }

    public boolean needsPaint() {
        return false;
    }

    public void tick(long tickStamp) {
        if (changeVisibility) {
            changeVisibility = false;
            if (visible) {
                Utilities.log.info("Show Some Dots");
                for (int i = 0; i < numberDots; i++) {
                    DotSprite dot = dotList[i];
                    dot.setVisible(true);
                    addSprite(dot);
                    if (dotsFade) {
                        Utilities.log.info("will fade");
                        dot.fadeOut(0, fadeTime);
                    }
                }
            } else {
                Utilities.log.info("Hide Dots");
                for (int i = 0; i < dotList.length; i++) {
                    DotSprite dot = dotList[i];
                    if (dot.isVisible()) {
                        removeSprite(dot);
                        dot.setVisible(false);
                    }
                }
            }
        }
        if (toUnfade) {
            toUnfade = false;
            for (int i = 0; i < dotList.length; i++) {
                DotSprite dot = dotList[i];
                dot.setAlpha(1.0f);
            }
        }

        if (lineUp) {
            Utilities.log.info("Start lining up");
            lineUp = false;

            //loop through the dots, assign a destination for each
            for (int i = first2lineUp - 1, p = 0; i < last2lineUp; i++, p++) {

                if (perDotObserver != null) {
                    dotList[i].addSpriteObserver(perDotObserver);
                }

                int radius = dotList[i].getRadius();
                Point dest = new Point(lineUpPoints[p]);
                dest.translate(-radius, -radius);
                if (dest.y > borderY) {
                    dotList[i].setRenderOrder(Constants.DOTS_LAYER);
                    dotList[i].move(new LinePath(dest, 500 + lineUpDelay * p));
                } else {
                    dotList[i].move(new ScrollSafeLinePath(dest, 500
                            + lineUpDelay * p));
                }

            }

            if (lineUpObserver != null) {
                dotList[first2lineUp - 1].addSpriteObserver(lineUpObserver);
            }

            this.lineUpPoints = null;
            this.lineUpObserver = null;
            this.perDotObserver = null;
        }
    }

    private void removeSprite(DotSprite dot) {
        mp.removeSprite(dot);
    }

    private void addSprite(DotSprite dot) {
        dot.setRenderOrder(AbstractMedia.HUD_LAYER);
        mp.addSprite(dot);
    }

    public void completeFadeOut() {
        for (int i = 0; i < dotList.length; i++) {
            dotList[i].comleteFadeOut();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * 
     * @param points    will not be changed, but stored and used during next tick
     * @param first2lineUp
     * @param last2lineUp
     * @param observer
     */
    public void lineUp(Point[] points, int first2lineUp, int last2lineUp,
            Object observer) {
        lineUp(points, first2lineUp, last2lineUp, observer, 0, null);
    }

    public void lineUp(Point[] points, int first2lineUp, int last2lineUp,
            Object observer, int delay, Object perDotObserver) {
        Utilities.log.info("1st : " + first2lineUp + " 2nd : " + last2lineUp
                + " p.len : " + points.length);
        this.lineUpPoints = points;
        this.first2lineUp = first2lineUp;
        this.last2lineUp = last2lineUp;
        this.lineUpObserver = observer;
        this.perDotObserver = perDotObserver;
        this.lineUpDelay = delay;
        lineUp = true;
    }

    public void unFade() {
        toUnfade = true;
    }

    public static void clearDotsCache() {
        cachedDots.clear();
    }

    //TODO: invent something better.
    public void cleanAfterSubstraction() {
        int x = dotList[0].getX();
        for (int i = 1; i < dotList.length; i++) {
            DotSprite ds = dotList[i];
            if (ds.isVisible()) {
                if (ds.getX() != x) {
                    ds.setVisible(false);
                    removeSprite(ds);
                }
            } else {
                break;
            }
        }

    }

    public void setBorderY(int borderY) {
        this.borderY = borderY;
    }

    public void viewLocationDidChange(int dx, int dy) {
        for (int i = 0; i < circles.length; i++) {
            circles[i].translate(dx, dy);
        }

        for (int i = 0; i < dotList.length; i++) {
            DotSprite dot = dotList[i];
            //  adjust dot position if it is not yet visible
            if (!mp.isManaged(dot)) {
                Rectangle bounds = dot.getBounds();
                dot.setLocation(bounds.x + dx, bounds.y + dy);
            }
        }
    }

    public Rectangle[] getCircles() {
        return circles;
    }
} //DotContainer
