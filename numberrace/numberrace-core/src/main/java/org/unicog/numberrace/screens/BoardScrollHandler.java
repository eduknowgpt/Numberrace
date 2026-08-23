package org.unicog.numberrace.screens;

import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.unicog.numberrace.util.Constants;

import com.threerings.media.sprite.PathObserver;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.util.Path;

public class BoardScrollHandler extends MouseAdapter implements PathObserver {

    /**
     * 
     */
    private final ChoiceScreen virtualMediaPanel;

    private final Rectangle brdBounds;

    private final int leftBorder;
    private final int rightBorder;

    /**
     * @param choiceScreen
     */
    BoardScrollHandler(ChoiceScreen choiceScreen, Rectangle boardBounds) {
        virtualMediaPanel = choiceScreen;
        this.brdBounds = new Rectangle(boardBounds);
        leftBorder = boardBounds.x - boardBounds.width / Constants.LAST_SQUARE;
        rightBorder = boardBounds.x + boardBounds.width;
    }

    private boolean enabled = false;

    private int x;
    //        private long when;
    //        private double speed;
    private boolean doIt;

    public void mouseDragged(MouseEvent e) {
        if (doIt) {
            final Rectangle viewBounds = virtualMediaPanel.getViewBounds();
            final int curX = e.getX() - viewBounds.x;
            if (enabled) {
                int dx = x - curX;
                final int newX = viewBounds.x + dx;
                if (newX > leftBorder
                        && (newX + viewBounds.width < rightBorder)) {
                    virtualMediaPanel.setViewLocation(newX, viewBounds.y);
                }
                //                  when = time;
            }
            x = curX;
        }
    }

    public void mousePressed(MouseEvent e) {
        if (enabled) {
            final Rectangle viewBounds = virtualMediaPanel.getViewBounds();
            final int y = e.getY() - viewBounds.y;
            if (brdBounds.contains(brdBounds.x, y)) {
                doIt = true;
                x = e.getX() - viewBounds.x;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        doIt = false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void pathCancelled(Sprite sprite, Path path) {
        handle(sprite);
    }

    public void pathCompleted(Sprite sprite, Path path, long when) {
        handle(sprite);
    }

    private void handle(Sprite sprite) {
        if (sprite != null) {
            sprite.removeSpriteObserver(this);
        }
        enabled = true;
    }

}