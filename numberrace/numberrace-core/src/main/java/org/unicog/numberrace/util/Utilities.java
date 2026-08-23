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

//source: various, see labels on individual functions

package org.unicog.numberrace.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class contains static utility functions used by several classes
 */
public class Utilities {

    public static Logger log = Logger.getLogger("NUMBERRACE");

    // source: Anna
    public static int oppositeSide(int side) {
        return (side + 1) % 2;
    }

    // source: Anna
    public static byte otherPlayer(byte player) {
        byte otherPlayer = 9;
        if (player == Constants.PLAYER1)
            otherPlayer = Constants.PLAYER2;
        else if (player == Constants.PLAYER2)
            otherPlayer = Constants.PLAYER1;
        return otherPlayer;
    }

    static String numberWords[] = {
            "zero", "one", "two", "three", "four", "five", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            "six",
            "seven",
            "eight",
            "nine",
            "ten",
            "eleven",
            "twelve",
            "thirteen",
            "fourteen",
            "fifteen",
            "sixteen",
            "seventeen",
            "eighteen",
            "nineteen",
            "twenty",
            "twenty-one",
            "twenty-two",
            "twenty-three",
            "twenty-four",
            "twenty-five",
            "twenty-six",
            "twenty-seven",
            "twenty-eight",
            "twenty-nine",
            "thirty",
            "thirty-one",
            "thirty-two",
            "thirty-three",
            "thirty-four",
            "thirty-five",
            "thirty-six", "thirty-seven", "thirty-eight", "thirty-nine", "forty" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

    // source: Anna
    public static String getVerbalForArabic(int arabicNumber) {
        return numberWords[arabicNumber];
    } // getVerbalforArabic

    // source: Anna
    public static int int4Bool(boolean bool) {
        if (bool)
            return 1;
        else
            return 0;
    }

    // source: M&M
    public static void drawFromPoint(String text, Point xy, int relativeToX,
            int relativeToY, Graphics g) {
        int x = xy.x;
        int y = xy.y;
        int widthOffset;
        int heightOffset;

        ((Graphics2D) g).addRenderingHints(antialiasRH);

        FontMetrics fm = g.getFontMetrics();

        if (relativeToX < 0)
            widthOffset = 0; // left of point x
        else if (relativeToX == 0)
            widthOffset = -(fm.stringWidth(text) / 2); // middle of point x
        else
            widthOffset = -fm.stringWidth(text); // right of point x

        if (relativeToY < 0)
            heightOffset = fm.getAscent(); // top of y
        else if (relativeToY == 0)
            heightOffset = (fm.getHeight() / 2) - fm.getDescent(); // middle of
        // point y
        else
            heightOffset = -fm.getDescent(); // bottom of point y

        x += widthOffset;
        y += heightOffset;

        //        System.out.printf("\"%s\"\n\t%d.%d %d %d\n\t%d.%d\n", text, xy.x, xy.y, relativeToX, relativeToY, x, y);
        g.drawString(text, x, y);

    } // draw from point

    public static void drawFromPoint(String text, int x, int y,
            int relativeToX, int relativeToY, Graphics g) {
        int widthOffset;
        int heightOffset;

        ((Graphics2D) g).addRenderingHints(antialiasRH);

        FontMetrics fm = g.getFontMetrics();

        if (relativeToX < 0)
            widthOffset = 0; // left of point x
        else if (relativeToX == 0)
            widthOffset = -(fm.stringWidth(text) / 2); // middle of point x
        else
            widthOffset = -fm.stringWidth(text); // right of point x

        if (relativeToY < 0)
            heightOffset = fm.getAscent(); // top of y
        else if (relativeToY == 0)
            heightOffset = (fm.getHeight() / 2) - fm.getDescent(); // middle of
        // point y
        else
            heightOffset = -fm.getDescent(); // bottom of point y

        x += widthOffset;
        y += heightOffset;

        g.drawString(text, x, y);

    } // draw from point

    // Source: M&M (i think...)
    public static int writeParagraph(Graphics2D g2D, int width,
            AttributedString textLine, int xPos, int yPos) {

        g2D.addRenderingHints(antialiasRH);

        // Set formatting width
        float formatWidth = width;
        float drawPosY = 0;
        float descent = 0;

        // set text stuff
        // Create a new LineBreakMeasurer from the paragraph.
        LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(
                textLine.getIterator(), new FontRenderContext(null, false,
                        false));

        AttributedCharacterIterator paragraph = textLine.getIterator();
        int paragraphStart = paragraph.getBeginIndex();
        int paragraphEnd = paragraph.getEndIndex();

        lineMeasurer.setPosition(paragraphStart);

        // Get lines from lineMeasurer until the entire
        // paragraph has been displayed.
        drawPosY += yPos;
        while (lineMeasurer.getPosition() < paragraphEnd) {

            // Retrieve next layout.
            TextLayout layout = lineMeasurer.nextLayout(formatWidth);
            // Move y-coordinate by the ascent of the layout.
            drawPosY += layout.getAscent();

            // Compute pen x position. If the paragraph is
            // right-to-left, we want to align the TextLayouts
            // to the right edge of the panel.
            float drawPosX = xPos;
            if (layout.isLeftToRight()) {
                drawPosX += 0;
            } else {
                drawPosX += (formatWidth - layout.getAdvance());
            }

            // Draw the TextLayout at (drawPosX, drawPosY).
            layout.draw(g2D, drawPosX, drawPosY);

            // Move y-coordinate in preparation for next layout.
            drawPosY += layout.getDescent() + layout.getLeading();

            descent = (2 * (layout.getDescent() + layout.getLeading()));
        }

        return (int) (drawPosY + descent);

    }

    //    // Source: doesn't matter
    //    public static Image createImage(ChoiceScreen choiceScreen, int width,
    //            int height) {
    //        return Game.gc.createCompatibleImage(width, height,
    //                Transparency.BITMASK);
    //    }

    // Source: M&M
    // public static Image convertToCompatibleImage(BufferedImage bImage,
    // Component c, GraphicsConfiguration gc, int transparencyType)
    // {
    // int width = bImage.getWidth();
    // int height = bImage.getHeight();
    //		
    // Image newImage = gc.createCompatibleImage(width, height,
    // transparencyType);
    //		
    // Graphics g = newImage.getGraphics();
    // g.drawImage(bImage, 0, 0, null);
    // g.dispose();
    //		
    // return newImage;
    // }

    // Source: M&M
    // public static Image convertToCompatibleImage(Image image, Component c,
    // GraphicsConfiguration gc, int transparencyType)
    // {
    // int width = image.getWidth(c);
    // int height = image.getHeight(c);
    // //int transparencyType = bImage.getColorModel().getTransparency();
    //		
    // Image newImage = gc.createCompatibleImage(width, height,
    // transparencyType);
    //		
    // Graphics g = newImage.getGraphics();
    // g.drawImage(image, 0, 0, null);
    // g.dispose();
    //		
    // return newImage;
    // }

    // Source: Anna
    public static float calculateDistance(Point x, Point y) {
        // Calculates the distance between two points on the screen
        // using pythagoras
        float distance = (float) Math.sqrt(Math.pow(Math.abs(x.getX()
                                                            - y.getX()),
                                                    2)
                + Math.pow(Math.abs(x.getY() - y.getY()), 2));
        return distance;
    }

    // Source: Anna
    public static Point2D.Float calculateMovementVector(Point2D.Float origin,
            Point2D.Float destination) {
        // Calculates the movement vector to move from origin to destination
        Point2D.Float movementVector = new Point2D.Float(
                (float) (destination.getX() - origin.getX()),
                (float) (destination.getY() - origin.getY()));
        return movementVector;
    }

    // Source: Deitel, but not word for word anyway
    public static double round(double x, int decimalPlaces) {
        double y = Math.floor(x * Math.pow(10, decimalPlaces) + 0.5)
                / Math.pow(10, decimalPlaces);
        return y;
    }

    // source: Anna
    public static int searchStrArrStr(String strArr[], String key) {
        // note: linear search, only use for small arrays
        for (int i = 0; i < strArr.length; i++) {
            if (strArr[i] == key)
                return i;
        }
        return -1;
    }

    // source: Anna
    public static String charac4id(int player) {
        if (player == Constants.PLAYER1)
            return "friend1_"; //$NON-NLS-1$
        else if (player == Constants.PLAYER2)
            return "enemy1_"; //$NON-NLS-1$
        else
            return null;
    }

    public static void ignoreRepaint(Container container) {
        ignoreRepaint(container, true);
    }

    public static void ignoreRepaint(Container container, boolean flag) {
        container.setIgnoreRepaint(true);
        Component[] components = container.getComponents();
        for (int i = 0; i < components.length; i++) {
            //			System.out.println(components[i]);
            if (components[i] instanceof Container) {
                Container c = (Container) components[i];
                if (c.getComponentCount() > 0) {
                    ignoreRepaint(c, flag);
                    continue;
                }
            }
            components[i].setIgnoreRepaint(flag);
        }

    }

    public static boolean str2bool(String str) {
        if ("YES".compareToIgnoreCase(str) == 0
                || "Y".compareToIgnoreCase(str) == 0) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    public static Map antialiasRH;

    static {

        antialiasRH = (Map) Toolkit.getDefaultToolkit()
                                   .getDesktopProperty("awt.font.desktophints");
        if (antialiasRH == null) {
            antialiasRH = new HashMap(3);
        }
        antialiasRH.put(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
        antialiasRH.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        antialiasRH.put(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static String OS = System.getProperty("os.name");
    public static boolean IS_LINUX = (OS != null) ? OS.toUpperCase()
                                                      .startsWith("LINUX")
            : false;

}