package org.unicog.numberrace.sprites;

import static org.unicog.numberrace.screens.ScaleUtils.i;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.unicog.numberrace.algorithms.GameTurn;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.Utilities;
import org.unicog.numberrace.vars.GraphicsVariables;

import com.samskivert.swing.util.SwingUtil;
import com.threerings.media.sprite.ImageSprite;
import com.threerings.media.sprite.action.DisableableSprite;
import com.threerings.media.sprite.action.HoverSprite;

public class DotContainerSprite extends ImageSprite implements HoverSprite,
        DisableableSprite, CommandOnPressedSprite {

    private Color background = Color.BLACK;
    private int myHeight;
    private int myWidth;
    private int imgOffsetY;
    private int imgOffsetX;
    private boolean addition;
    private boolean subtraction;
    private String lblDigitName;
    private final int id;
    private boolean arabicStims;
    private boolean toBeClosed;
    private boolean toBeOpened;
    private Color dotContDigitColor;
    private Font dotContDigitFont;
    private boolean toBeValidated;
    private boolean opened;
    private boolean enabled = false;
    private Object cmdArg;
    private String cmd;
    private boolean hovered;

    public DotContainerSprite(int id, int width, int height, int imgOffsetX,
            int imgOffsetY, String command, Object argument) {
        this.id = id;
        myWidth = width;
        myHeight = height;
        this.imgOffsetX = imgOffsetX;
        this.imgOffsetY = imgOffsetY;
        _bounds = new Rectangle(width, height);
        this.cmd = command;
        this.cmdArg = argument;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    @Override
    public void paint(Graphics2D gfx) {

        gfx.setColor(background);
        if (hovered && enabled) {
            gfx.fill3DRect(_bounds.x,
                           _bounds.y,
                           _bounds.width,
                           _bounds.height,
                           false);
        } else {
            gfx.fill3DRect(_bounds.x,
                           _bounds.y,
                           _bounds.width,
                           _bounds.height,
                           true);
        }
        // documentation inherited
        if (_frames != null) {
            //                 // DEBUG: fill our background with an alpha'd rectangle
            //                 Composite ocomp = gfx.getComposite();
            //                 gfx.setComposite(ALPHA_BOUNDS);
            //                 gfx.setColor(Color.blue);
            //                 gfx.fill(_bounds);
            //                 gfx.setComposite(ocomp);

            // render our frame
            _frames.paintFrame(gfx,
                               _frameIdx,
                               _bounds.x + imgOffsetX,
                               _bounds.y + imgOffsetY);
        }

        if (arabicStims) {
            Object antiAliasing = SwingUtil.activateAntiAliasing(gfx);
            gfx.setColor(dotContDigitColor);
            gfx.setFont(dotContDigitFont);
            if (!(addition || subtraction)) {
                if (id == Constants.LEFT)
                    Utilities.drawFromPoint(lblDigitName,
                                            i(GraphicsVariables.LEFT_DOT_CONT_DIGIT_X)
                                                    + getX(),
                                            i(GraphicsVariables.LEFT_DOT_CONT_DIGIT_Y)
                                                    + getY(),
                                            0,
                                            0,
                                            gfx);
                else if (id == Constants.RIGHT)
                    Utilities.drawFromPoint(lblDigitName,
                                            i(GraphicsVariables.RIGHT_DOT_CONT_DIGIT_X)
                                                    + getX(),
                                            i(GraphicsVariables.RIGHT_DOT_CONT_DIGIT_Y)
                                                    + getY(),
                                            0,
                                            0,
                                            gfx);
            } else {
                if (id == Constants.LEFT)
                    Utilities.drawFromPoint(lblDigitName,
                                            i(GraphicsVariables.LEFT_DOT_CONT_MULTIDIGIT_X)
                                                    + getX(),
                                            i(GraphicsVariables.LEFT_DOT_CONT_MULTIDIGIT_Y)
                                                    + getY(),
                                            0,
                                            0,
                                            gfx);
                else if (id == Constants.RIGHT)
                    Utilities.drawFromPoint(lblDigitName,
                                            i(GraphicsVariables.RIGHT_DOT_CONT_MULTIDIGIT_X)
                                                    + getX(),
                                            i(GraphicsVariables.RIGHT_DOT_CONT_MULTIDIGIT_Y)
                                                    + getY(),
                                            0,
                                            0,
                                            gfx);
            }
            SwingUtil.restoreAntiAliasing(gfx, antiAliasing);
        }

    }

    @Override
    public boolean hitTest(int x, int y) {
        return _bounds.contains(x, y);
    }

    @Override
    protected void accomodateFrame(int frameIdx, int width, int height) {
        super.accomodateFrame(frameIdx, myWidth, myHeight);
    }

    public void setAddition(boolean addition) {
        this.addition = addition;
        toBeValidated = true;
    }

    public void setSubtraction(boolean subtraction) {
        this.subtraction = subtraction;
        toBeValidated = true;
    }

    public void setArabicDigit(GameTurn currentTurn) {
        lblDigitName = ""; //$NON-NLS-1$
        int[][] subNumbers = currentTurn.getSubNumbers();
        //replace with new one
        if (addition) {
            lblDigitName = new String(
                    String.valueOf(subNumbers[id][Constants.LEFT]
                            + "+" + subNumbers[id][Constants.RIGHT])); //$NON-NLS-1$
        } else if (subtraction) {
            lblDigitName = new String(
                    String.valueOf(subNumbers[id][Constants.LEFT]
                            + "-" + subNumbers[id][Constants.RIGHT])); //$NON-NLS-1$
        } else {
            lblDigitName = new String(String.valueOf(currentTurn.getNumber(id)));
        }
        toBeValidated = true;
    }

    public void setArabicStims(boolean arabicStim) {
        this.arabicStims = arabicStim;
        toBeValidated = true;
    }

    public void showEqual() {
        lblDigitName += "="; //$NON-NLS-1$
        toBeValidated = true;
    }

    public void showSum(GameTurn turn) {
        lblDigitName += String.valueOf(turn.getNumber(id)); //$NON-NLS-1$
        toBeValidated = true;
    }

    public void open() {
        toBeOpened = true;
    }

    public void close() {
        toBeClosed = true;
    }

    @Override
    public void tick(long timestamp) {
        if (toBeOpened) {
            setFrameIndex(1, false);
            toBeOpened = false;
            opened = true;
        }
        if (toBeClosed) {
            setFrameIndex(0, false);
            toBeClosed = false;
            opened = false;
        }
        if (toBeValidated) {
            invalidate();
            toBeValidated = false;
        }
        super.tick(timestamp);
    }

    public void setDotContDigitColor(Color dotContDigitColor) {
        this.dotContDigitColor = dotContDigitColor;
    }

    public void setDotContDigitFont(Font dotContDigitFont) {
        this.dotContDigitFont = dotContDigitFont;
    }

    public boolean isAddition() {
        return addition;
    }

    public boolean isSubtraction() {
        return subtraction;
    }

    public boolean isOpened() {
        return opened;
    }

    public void reset() {
        close();
        arabicStims = false;
        addition = false;
        subtraction = false;
        lblDigitName = null; //clear the digit label
    }

    public void setHovered(boolean hovered) {
        if (this.hovered != hovered) {
            this.hovered = hovered;
            toBeValidated = true;
        }
    }

    @Override
    public void viewLocationDidChange(int dx, int dy) {
        // no matter what renderOrder is movement when view scrolls prohibited
        setLocation(_ox + dx, _oy + dy);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            invalidate();
        }
    }

    public Object getCommandArgument() {
        return cmdArg;
    }

    public String getActionCommand() {
        return cmd;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
