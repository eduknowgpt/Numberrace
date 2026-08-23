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
*This class implements the all the adaptive dimensions
*Its variables and methods are all static, so it does not need to be instantiated
*Each of the "dimension" methods will return the correct value for that dimension given a difficulty
*input from 0 to 1
*
*Note: eventually these methods should throw an exception if they are sent a d that is out
*of range!
*
*/

package org.unicog.numberrace.algorithms;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AdapDimensions {
    
    private static Logger log = Logger.getLogger(AdapDimensions.class.getPackage().getName());
    
    public static final int NUM_ADAP_DIMS = 3; //number of adaptive dimensions
    public static final int XMIN = 2;
    public static final int YMIN = 1;
    // public static final int XMAX_RESTR_RANGE = 5;
    public static final int XMAX_FULL_RANGE = 9;
    public static final double DEADLINE_DECR_RATE = 0.001;
    public static final double DEADLINE_MIN = 0.25; //asymptote for deadline
    public static final int DEADLINE_MAX = 10;
    public static final double DEADLINE_ALPHA = 0.3;
    public static final double DEADLINE_RIGHTSHIFT = Math.log(1d / (DEADLINE_MAX - DEADLINE_MIN))
            / Math.log(DEADLINE_DECR_RATE) + DEADLINE_ALPHA;
    public static final double MAX_DIST_RATIO = 2;
    public static final byte SPEED_DIM = 0;
    public static final byte DIST_DIM = 1;
    public static final byte NOTN_DIM = 2;

    public static double speedDimension(double ds) {
        //institutes deadline (in seconds)
        return Math.pow(DEADLINE_DECR_RATE, (ds - DEADLINE_RIGHTSHIFT))
                + DEADLINE_MIN;
    }

    public static int[] distDimension(double dd, double randx,
            int rangeCeilling, Double weberFraction) {
        if(log.isLoggable(Level.FINE)) {
            log.fine(String.format("\ndd=%f\trandx=%f\tceilling=%d\twebberFr=%f", dd,randx, rangeCeilling, weberFraction));
        }
        
        //Note: randx should be a randomly selected number from the distribution X ~ Uniform[0,1]
        int xyValues[] = new int[2];
        xyValues[0] = (int) Math.floor(((rangeCeilling - XMIN + 1) * randx) + XMIN);
        xyValues[1] = (int) Math.min((Math.floor(xyValues[0]
                                             * Math.pow(MAX_DIST_RATIO,
                                                        (dd - 1)))),
                                     (xyValues[0] - 1));
        //old code: = (int)Math.max((Math.floor(xyValues[0]*Math.pow(2,(dd-1)))),1);
        weberFraction = new Double((xyValues[1] - xyValues[0]) / xyValues[0]);
        return xyValues;
    }

}