package org.unicog.numberrace.animations;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.unicog.numberrace.util.Utilities;

import com.threerings.media.animation.Animation;
import com.threerings.media.image.Mirage;

public class ScrollImageAnimation extends Animation {

    private int currOffset;
    private final Mirage image;
    private long prevTick;

    public ScrollImageAnimation(Mirage image, Rectangle scrollView) {
        super(scrollView);
        this.image = image;
        currOffset = -scrollView.height;
    }

    public void paint(Graphics2D gfx) {
        if (gfx.getClip().intersects(_bounds)) {
            Graphics2D g2d = (Graphics2D) gfx.create(_bounds.x,
                                                     _bounds.y,
                                                     _bounds.width,
                                                     _bounds.height);
            image.paint(g2d, 0, -currOffset);
            g2d.dispose();
        }
    }

    public void tick(long tickStamp) {
        if (tickStamp - prevTick > 32) {

            currOffset += 1;
            prevTick = tickStamp;
            _finished = currOffset > image.getHeight();
            invalidate();
        }
    }

    @Override
    public void reset() {
        super.reset();
        currOffset = -_bounds.height;
    }

    @Override
    protected void willFinish(long tickStamp) {
        super.willFinish(tickStamp);
        Utilities.log.info("");
    }
}
