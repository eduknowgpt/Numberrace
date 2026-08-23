package org.unicog.numberrace.sprites;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.util.Utilities;
import org.unicog.numberrace.vars.ThemeVariables;

import com.threerings.media.sprite.Sprite;

public class BubbleSprite extends Sprite {

    private String label = null;
    private final Font font;
    private final Color color;

    public BubbleSprite(int w, int h) {
        super(w, h);
        ThemeVariables theme = GameObject.getInstance().getTheme();
        font = theme.bubbleFont;
        color = theme.bubbleTextColor;
    }

    @Override
    public void paint(Graphics2D gfx) {
        if (label != null) {
            gfx.setFont(font);
            gfx.setColor(color);
            Utilities.drawFromPoint(label,
                                    _bounds.x + _bounds.width / 2,
                                    _bounds.y + _bounds.height / 2,
                                    0,
                                    0,
                                    gfx);
        }
    }

    public void setLabel(String label) {
        this.label = label;
        invalidate();
    }

}
