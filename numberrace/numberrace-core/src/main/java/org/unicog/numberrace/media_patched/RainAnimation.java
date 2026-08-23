package org.unicog.numberrace.media_patched;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class RainAnimation extends com.threerings.media.animation.RainAnimation {

    public RainAnimation(Rectangle bounds, long duration) {
        super(bounds, duration);
    }

    public RainAnimation(Rectangle bounds, long duration, int count, int wid,
            int hei) {
        super(bounds, duration, count, wid, hei);
    }

    @Override
    public void paint(Graphics2D gfx) {
        gfx.translate(_bounds.x, _bounds.y);
        gfx.setColor(Color.white);
        for (int ii = 0; ii < _count; ii++) {
            int x = _drops[ii] >> 16;
            int y = _drops[ii] & 0xFFFF;
            gfx.drawLine(x, y, x + _wid, y + _hei);
        }
        gfx.translate(-_bounds.x, -_bounds.y);
    }
}
