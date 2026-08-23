package org.unicog.numberrace.util;

import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Random;

import com.threerings.media.util.Path;
import com.threerings.media.util.Pathable;

public class RewardPath implements Path {
    //constants
    private final static int MIN_VELOCITY = 8;
    private final static int MAX_VELOCITY = 15;

    private final int minx;
    private final int miny;
    private final int maxx;
    private final int maxy;
    private int destinationX;
    private int destinationY;
    private float vX;
    private float vY;
    private int side;
    private Random randomNumber;

    public RewardPath(int minx, int miny, int maxx, int maxy) {
        this.minx = minx;
        this.miny = miny;
        this.maxx = maxx;
        this.maxy = maxy;

        randomNumber = new Random(System.currentTimeMillis());

        side = randomNumber.nextInt(2);

        if (side == Constants.LEFT) {
            setDestination(new Point(minx, randomNumber.nextInt(maxy - miny)
                    + miny));
        } else {
            setDestination(new Point(maxx, randomNumber.nextInt(maxy - miny)
                    + miny));
        }

        //pick an initial velocity at random
        int newVelocity = randomNumber.nextInt(MAX_VELOCITY - MIN_VELOCITY);
        setVelocity(newVelocity, newVelocity);
    }

    public void fastForward(long timeDelta) {

    }

    public void init(Pathable pable, long tickStamp) {
        pable.setOrientation(side);
        pable.pathBeginning();
    }

    public void paint(Graphics2D gfx) {
        //TODO:
    }

    public boolean tick(Pathable pable, long tickStamp) {
        int xLocation = pable.getX();
        int yLocation = pable.getY();

        //if at side of screen, turn around and move back
        if ((destinationX == minx) && (xLocation <= minx)) {
            pable.setOrientation(side ^= 1);
            setDestination(new Point(maxx, (int) destinationY));
            //change the velocity with a random fluctuation
            int newVelocity = randomNumber.nextInt(MAX_VELOCITY - MIN_VELOCITY);
            setVelocity(newVelocity, newVelocity);

        } else if ((destinationX == maxx) && (xLocation >= maxx)) {
            pable.setOrientation(side ^= 1);
            //            switchImage();
            //            side = Utilities.oppositeSide(side); //switch side
            //            if (side == Constants.LEFT) {
            //            } else if (side == Constants.RIGHT) {
            setDestination(new Point(minx, (int) destinationY));
            //            }
            //change the velocity with a random fluctuation
            int newVelocity = randomNumber.nextInt(MAX_VELOCITY - MIN_VELOCITY);
            setVelocity(newVelocity, newVelocity);

        } else if (vX == 0) {
            int newVelocity = randomNumber.nextInt(MAX_VELOCITY - MIN_VELOCITY);
            setVelocity(newVelocity, newVelocity);
        }

        //If we are not at destination, move in the x and y directions
        //        else {
        //decide where to move based on direction of destination
        if (xLocation < destinationX) //moving to right
            xLocation = (int) (xLocation + Math.min(vX,
                                                    (destinationX - xLocation))); //starts  pos
        else if (xLocation > destinationX) //moving to left
            xLocation = (int) (xLocation + Math.max((-1 * vX),
                                                    (destinationX - xLocation))); //starts neg
        if (yLocation < destinationY) //moving down
            yLocation = (int) (yLocation + Math.min(vY,
                                                    (destinationY - yLocation))); //starts pos
        else if (yLocation > destinationY) //moving up
            yLocation = (int) (yLocation + Math.max((-1 * vY),
                                                    (destinationY - yLocation))); //starts neg
        else { // yLocation == yLocatiom
               //change the y destination with a random fluctuation
            setDestination(new Point((int) destinationX,
                    randomNumber.nextInt(maxy - miny) + miny));

        }

        if (xLocation == pable.getX() && yLocation == pable.getY()) {
            Utilities.log.info("SAME COORDINATES"); //TODO: remove - does not happen often 
            return false;
        } else {
            //set the new location  
            pable.setLocation(xLocation, yLocation);
            //            return true;
            //        }

            //change destination and velocity for next time

            return true;
        }
    }

    public void wasRemoved(Pathable pable) {

    }

    public void setDestination(Point destinationToSet) {
        destinationX = destinationToSet.x;
        destinationY = destinationToSet.y;
    }

    public void setVelocity(float x, float y) { //Animated interface
        vX = x;
        vY = y;
    }

}
