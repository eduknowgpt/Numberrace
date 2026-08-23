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
 * This class is for the "carpet" that the dots are lined up on after selection.
 * For reasons unknown to everyone including himself, Stan says it is perfectly logical
 * that we find a carpet hanging out under the sea...
 * 
 */

package org.unicog.numberrace.animations;

import static org.unicog.numberrace.screens.ScaleUtils.i;
import static org.unicog.numberrace.util.Constants.DOTCARPET_LAYER;
import static org.unicog.numberrace.vars.GraphicsVariables.DOT_CARPET_LINEWIDTH;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.algorithms.AdapDimensions;
import org.unicog.numberrace.util.Constants;

import com.threerings.media.sprite.Sprite;

public class DotCarpet extends Sprite {

    public Point[][] dotStacks = { new Point[AdapDimensions.XMAX_FULL_RANGE],
            new Point[AdapDimensions.XMAX_FULL_RANGE] };

    private Point[] leftDotStackPoints = dotStacks[Constants.LEFT];
    private Point[] rightDotStackPoints = dotStacks[Constants.RIGHT];
    private GameObject go;

    private Color color;
    private BasicStroke stroke;

    private final int interDotInterval;
    private boolean visible = false;

    public DotCarpet(int width, int height) {
        super(width, height);
        go = GameObject.getInstance();
        setRenderOrder(DOTCARPET_LAYER);
        interDotInterval = _bounds.height
                / (AdapDimensions.XMAX_FULL_RANGE + 1);
    }

    public void load() {
        calculatePointLocations();
        color = go.getTheme().dotCarpetForeColor;
        stroke = new BasicStroke(i(DOT_CARPET_LINEWIDTH));
    }

    private void calculatePointLocations() {
        //*** this needs convertion for multiple screen sizes

        final int centerX = (int) _bounds.getCenterX();
        final int leftStackX = centerX - (_bounds.width >> 2);
        final int rightStackX = centerX + (_bounds.width >> 2);

        final int firstY = _bounds.height + _bounds.y - interDotInterval;
        for (int i = 0; i < AdapDimensions.XMAX_FULL_RANGE; i++) {
            int y = firstY - i * interDotInterval;
            leftDotStackPoints[i] = new Point(leftStackX, y);
            rightDotStackPoints[i] = new Point(rightStackX, y);
        }

    }

    public void paint(Graphics2D g) {
        if (visible) {
            g.setColor(color);
            g.setStroke(stroke);

            final int x = (int) _bounds.getCenterX() + i(DOT_CARPET_LINEWIDTH)
                    / 2;
            final int offset = interDotInterval >> 1;
            g.drawLine(x, _bounds.y + offset, x, _bounds.y + _bounds.height
                    - offset);
        }
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            invalidate();
        }
    }

    public void setTopDigit(int newDigit) {
    }

    public void setBottomDigit(int newDigit) {
    }

    public void setTopDigitVisible(boolean state) {
    }

    public void setBottomDigitVisible(boolean state) {
    }

    @Override
    public void viewLocationDidChange(int dx, int dy) {
        // no matter what renderOrder is movement when view scrolls prohibited
        for (int i = 0; i < leftDotStackPoints.length; i++) {
            leftDotStackPoints[i].translate(dx, dy);
            rightDotStackPoints[i].translate(dx, dy);
        }
        setLocation(_ox + dx, _oy + dy);
    }

} //DotContainer
