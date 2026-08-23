package org.unicog.numberrace.sprites;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.unicog.numberrace.util.Utilities;

import com.threerings.media.image.BufferedMirage;
import com.threerings.media.sprite.ImageSprite;
import com.threerings.media.util.Path;

public class TimerSprite extends ImageSprite {

    private long duration = 0;
    private int curAngle;

    private long _startStamp;
    private Color bgColor;

    public TimerSprite(BufferedImage img) {
        super(new BufferedMirage(img));
    }

    @Override
    public void tick(long tickStamp) {
        super.tick(tickStamp);

        if (duration > 0) {
            if (_startStamp == 0) {
                _startStamp = tickStamp;
            } else {
                final long elapsed = (tickStamp - _startStamp) % duration;
                final int angle = 360 - (int) ((elapsed * 360) / duration);
                if (angle != curAngle) {
                    curAngle = angle;
                    invalidate();
                }
            }
        }
    }

    @Override
    public void paint(Graphics2D gfx) {
        Graphics2D g2d = (Graphics2D) gfx.create();
        g2d.addRenderingHints(Utilities.antialiasRH);
        super.paint(g2d);
        g2d.setColor(bgColor);
        g2d.fillArc(_bounds.x,
                    _bounds.y,
                    _bounds.width,
                    _bounds.height,
                    90,
                    curAngle);

        g2d.dispose();
    }

    public void setDuration(long duration) {
        this.duration = duration;
        this._startStamp = 0L;
    }

    public void start(Path sneakPath) {
        move(sneakPath);
    }

    public void stop() {
        cancelMove();
    }

    public void setBackground(Color bgColor) {
        this.bgColor = bgColor;
    }
}
