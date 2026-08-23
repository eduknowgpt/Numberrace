package org.unicog.numberrace.observer;

import org.unicog.numberrace.screens.CountAnimationVariableContainer;
import org.unicog.numberrace.util.Utilities;

import com.threerings.media.sprite.PathObserver;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.util.Path;

public class LineUpObserver implements PathObserver {

    private int i;
    private int j;
    private int x = 0;
    private CountAnimationVariableContainer container;

    public void pathCancelled(Sprite sprite, Path path) {
        Utilities.log.info("path cancelled: " + sprite.toString() + " : "
                + path.toString());
        sprite.removeSpriteObserver(this);
        this.container.getGameArea().setDnum(-1, -1);
    }

    public void pathCompleted(Sprite sprite, Path path, long l) {
        sprite.removeSpriteObserver(this);
        if (x++ <= j) {
            Utilities.log.info("x: " + x + " i: " + i + " j: " + j);
            this.container.getGameArea().setDnum(i, x);
        }
    }

    public void setBounds(int start, int finnish, int from,
            CountAnimationVariableContainer container) {
        this.container = container;
        this.i = start;
        this.x = from;
        this.j = finnish;
    }

}
