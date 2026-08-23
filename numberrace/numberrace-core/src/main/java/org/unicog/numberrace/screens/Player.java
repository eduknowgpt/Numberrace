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

package org.unicog.numberrace.screens;

import static org.unicog.numberrace.screens.ScaleUtils.i;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import org.unicog.numberrace.sprites.ImageButtonSprite;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.Utilities;
import org.unicog.numberrace.vars.GraphicsVariables;

import com.threerings.media.image.BufferedMirage;
import com.threerings.media.image.Mirage;
import com.threerings.media.sprite.ImageSprite;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.util.ArcPath;
import com.threerings.media.util.LinePath;
import com.threerings.media.util.MultiFrameImageImpl;
import com.threerings.media.util.Path;
import com.threerings.media.util.PathSequence;
import org.unicog.numberrace.vars.ThemeVariables;

public class Player {

    private GameArea gameArea;
    public int id;
    public int tmpBoardPosition;
    public int boardPosition = 0;
    public int prevPosition = boardPosition;
    private Point startLocation;
    public ImageSprite sprite;
    private ImageButtonSprite stamp;
    public int steps;
    int toCount;
    PathSequence boardMovementPath;
    private boolean backwardMovement;
    private BufferedImage stampImage;
    private Sprite miniSprite;
    private Cursor cursor;
    private Cursor halfSizeCursor;
    private ImageSprite halfSizeStamp;
    private List<Path> path;

    public Player(int id) {
        this.id = id;
    }

    public void setLocation(Point loc) {
        if (startLocation == null) {
            startLocation = loc.getLocation();
        }
        if (sprite != null) {
            sprite.setLocation(loc.x, loc.y);
        }
    }

    public void setSteps(int number) {
        if (number + boardPosition > Constants.LAST_SQUARE) {
            number = Constants.LAST_SQUARE - boardPosition;
        }
        this.steps = number;
        this.toCount = number;
    }

    public void setSprite(ImageSprite sprite) {
        this.sprite = sprite;
    }

    public int getSteps() {
        return steps;
    }

    public ActionState go() {
        return go(steps);
    }

    public ActionState go(int stp) {
        Utilities.log.info("go stp = " + stp);
        //steps = Math.abs(stp);
        this.setSteps(stp);

        Utilities.log.info("go steps = " + steps);

        int xCor = sprite.getWidth() >> 1;
        int yCor = sprite.getHeight() >> 1;
        path = new LinkedList<Path>();

        double startAng = Math.PI;
        double ang = Math.PI;

        if ((backwardMovement = (stp < 0))) {
            startAng = 0.;
            ang = -Math.PI;
        }

        final int nextbp = boardPosition + stp;

        if (nextbp > 0) {
            Point squareCenter = this.gameArea.getSquareCenter(nextbp)
                                              .getLocation();
            squareCenter.translate(-xCor, -yCor);

            Point start = sprite.getBounds().getLocation();

            if (squareCenter.y != start.y) {
                start = new Point(start.x, squareCenter.y);
                getPath().add(new LinePath(start, ChoiceScreen.BMOVE_MS / 4));
            }
            ArcPath ap = new ArcPath(start,
                    Math.abs((squareCenter.x - start.x)) / 2,
                    i(GraphicsVariables.NUMBER_BOARD_SQUARE_WIDTH) / 2,
                    startAng, ang,
                    (long) (ChoiceScreen.BMOVE_MS * (1 + stp * 0.1)),
                    ArcPath.NONE);
            Utilities.log.info(ap.getEndPos().toString());
            getPath().add(ap);
        } else {
            getPath().add(new LinePath(startLocation.getLocation(),
                    ChoiceScreen.BMOVE_MS));
            Utilities.log.info(startLocation.toString());
        }

        boardMovementPath = new PathSequence(getPath());

        prevPosition = boardPosition;
        boardPosition += stp;
        tmpBoardPosition = boardPosition;
        if (tmpBoardPosition < 0) {
            tmpBoardPosition = 0;
            boardPosition = 0;
        }

        return ActionState.BOARD_MOVEMENTS;
    }

    public int getBoardPosition() {
        return boardPosition;
    }

    public boolean isMovingBackwards() {
        return backwardMovement;
    }

    public ImageButtonSprite getStamp() {
        if (stamp == null) {

            BufferedImage img = getStampImage();
            //                ColorConvertOp op = new ColorConvertOp(
            //                        scaledImage.getColorModel().getColorSpace(),
            //                        ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
            //                BufferedImage gray = op.filter(scaledImage, null);
            //                stamp = new ImageSprite(new BufferedMirage(
            //                        ImageFactory.toCompatibleImage(gray)));
            stamp = new ImageButtonSprite("stampClicked", id) {
                public void viewLocationDidChange(int dx, int dy) {
                    this.setLocation(_ox + dx, _oy + dy);
                };
            };

            stamp.setFrames(new MultiFrameImageImpl(new Mirage[] {
                    new BufferedMirage(img),
                    new BufferedMirage(ImageFactory.createNegative(img)) }));
            stamp.setAnimateOnHover(false);

            stamp.layout();
        }
        return stamp;
    }

    public ImageSprite getHalfSizeStamp() {
        if (halfSizeStamp == null) {
            final BufferedImage si = getStampImage();
            final BufferedImage img = ImageFactory.getFasterScaledInstance(si,
                                                                           si.getWidth() >> 1,
                                                                           si.getHeight() >> 1,
                                                                           RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                                                                           true);
            halfSizeStamp = new ImageSprite(new BufferedMirage(img));
            halfSizeStamp.setRenderOrder(Constants.STAMP_MOVEMENT_LAYER);
            halfSizeStamp.layout();
        }

        return halfSizeStamp;
    }

    public BufferedImage getStampImage() {
        if (stampImage == null) {
            sprite.layout();
            Rectangle sb = new Rectangle(sprite.getBounds());

            final int strokeWidth = i(6);
            final int w = (int) (2 * Math.max(sb.width, sb.height) / Math.sqrt(2));

            BufferedImage img = ImageFactory.createCompatibleTranslucentImage(w,
                                                                              w);

            Graphics2D g2d = img.createGraphics();
            g2d.addRenderingHints(Utilities.antialiasRH);
            //                g2d.setColor(Color.BLACK);
            //                g2d.drawRect(0, 0, w - 1, w - 1);
            //                g2d.setColor(Color.WHITE);
            g2d.setColor(ThemeVariables.boardColor);
            g2d.fillOval(0, 0, w, w);
            int outside = strokeWidth + 4;
            g2d.setStroke(new BasicStroke(outside));
            g2d.setColor(Color.GRAY);
            g2d.drawOval(outside >> 1, outside >> 1, w - 1 - outside, w - 1
                    - outside);
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawOval((outside >> 1), (outside >> 1), w - 1 - outside, w - 1
                    - outside);
            g2d.translate(-sb.x + ((w - sb.width) >> 1), -sb.y
                    + ((w - sb.height) >> 1));
            sprite.paint(g2d);
            g2d.dispose();
            stampImage = ImageFactory.getFasterScaledInstance(img,
                                                              i(GraphicsVariables.STAMP_WIDTH),
                                                              i(GraphicsVariables.STAMP_WIDTH),
                                                              RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                                                              true);
        }
        return stampImage;
    }

    public void setMiniSprite(Sprite miniSprite) {
        this.miniSprite = miniSprite;
    }

    public Sprite getMiniSprite() {
        return miniSprite;
    }

    public Cursor getCursor() {
        if (cursor == null) {
            BufferedImage img = getStampImage();
            Dimension bestCursorSize = Toolkit.getDefaultToolkit()
                                              .getBestCursorSize(img.getWidth(),
                                                                 img.getHeight());
            if (bestCursorSize.width < img.getWidth()
                    || bestCursorSize.height < img.getHeight()) {
                img = ImageFactory.getFasterScaledInstance(img,
                                                           bestCursorSize.width,
                                                           bestCursorSize.height,
                                                           RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                                                           true);
            }
            cursor = Toolkit.getDefaultToolkit()
                            .createCustomCursor(img,
                                                new Point(img.getWidth() >> 1,
                                                        img.getHeight() >> 1),
                                                "Player Stamp Cursor" + id);
        }

        return cursor;
    }

    public Cursor getHalfSizeCursor() {
        if (halfSizeCursor == null) {
            BufferedImage orig = getStampImage();
            int halfWidth = orig.getWidth() >> 1;
            int halfHeight = orig.getHeight() >> 1;
            Dimension bestCursorSize = Toolkit.getDefaultToolkit()
                                              .getBestCursorSize(halfWidth,
                                                                 halfHeight);
            BufferedImage img = null;
            if (bestCursorSize.width < halfWidth
                    || bestCursorSize.height < halfHeight) {
                img = ImageFactory.getFasterScaledInstance(orig,
                                                           bestCursorSize.width,
                                                           bestCursorSize.height,
                                                           RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                                                           true);
            } else {
                img = ImageFactory.getFasterScaledInstance(orig,
                                                           orig.getWidth() >> 1,
                                                           orig.getHeight() >> 1,
                                                           RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                                                           true);
            }

            halfSizeCursor = Toolkit.getDefaultToolkit()
                                    .createCustomCursor(img,
                                                        new Point(
                                                                img.getWidth() >> 1,
                                                                img.getHeight() >> 1),
                                                        "Player Small Stamp Cursor"
                                                                + id);
        }

        return halfSizeCursor;
    }

    /**
     * @return the path
     */
    public List<Path> getPath() {
        return path;
    }

    public void setGameArea(GameArea gameArea) {
        this.gameArea = gameArea;
    }

}