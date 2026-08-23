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
 *
 *This class is essentially a data structure to store the values of variable notation
 *parameters for a notation level
 *
 */

package org.unicog.numberrace.algorithms;

import org.unicog.numberrace.util.Utilities;

public class NotnDimLevel {
    public boolean analogMagStims; //dots visible before choice
    public boolean verbalStims; //auditory stims presented before choice
    public boolean arabicStims; //arabic stims presented before choice
    public boolean dotsFade; //fading of dots
    public int rangeCeilling; // range restriction ceiling
    public boolean hazards; //general hazard present
    public boolean addition; //addition stims present
    public boolean subtraction; //subtraction stims present
    public int fadeTime; //time for dots to fade
    public int boardLength;
    private String startLevel;

    public NotnDimLevel(boolean analogMagStims, boolean verbalStims,
            boolean arabicStims, boolean dotsFade, int rangeRestriction,
            boolean hazards, boolean addition, boolean subtraction,
            int fadeTime, int boardLength) {
        this.analogMagStims = analogMagStims;
        this.verbalStims = verbalStims;
        this.arabicStims = arabicStims;
        this.dotsFade = dotsFade;
        this.rangeCeilling = rangeRestriction;
        this.hazards = hazards;
        this.addition = addition;
        this.subtraction = subtraction;
        this.fadeTime = fadeTime;
        this.boardLength = boardLength;
    }

    public String getAttributesCommaDelim() {
        //Returns converted difficulty levels in the form of a comma delimited string
        String commaDelimStr = Utilities.int4Bool(analogMagStims)
                + "," + Utilities.int4Bool(verbalStims) + "," + //$NON-NLS-1$ //$NON-NLS-2$
                Utilities.int4Bool(arabicStims)
                + "," + Utilities.int4Bool(dotsFade) + "," + //$NON-NLS-1$ //$NON-NLS-2$
                rangeCeilling
                + "," + Utilities.int4Bool(hazards) + "," + //$NON-NLS-1$ //$NON-NLS-2$
                Utilities.int4Bool(addition)
                + "," + Utilities.int4Bool(subtraction) + "," + fadeTime + "," + boardLength + ","; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        return commaDelimStr;
    }

    public void setStartOf(String level) {
        startLevel = level;
    }

    public boolean isStartOf(String level) {
        return startLevel != null && level.compareToIgnoreCase(startLevel) == 0;
    }

    @Override
    public String toString() {
        return String.format("%11d %14s %11s %11s %13d %8s %8d %7s %8s %11s %s",
                             boardLength,
                             analogMagStims,
                             verbalStims,
                             arabicStims,
                             rangeCeilling,
                             dotsFade,
                             fadeTime,
                             hazards,
                             addition,
                             subtraction,
                             startLevel);
    }

}