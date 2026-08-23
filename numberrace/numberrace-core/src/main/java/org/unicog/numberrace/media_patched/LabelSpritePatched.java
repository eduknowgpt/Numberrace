package org.unicog.numberrace.media_patched;

import java.awt.Graphics2D;

import com.samskivert.swing.Label;
import com.samskivert.swing.util.SwingUtil;
import com.threerings.media.sprite.LabelSprite;

public class LabelSpritePatched extends LabelSprite {

    public LabelSpritePatched(Label label) {
        super(label);
    }

    @Override
    public void paint(Graphics2D gfx) {
        Object ohints = null;
        if (_antiAliased) {
            ohints = SwingUtil.activateAntiAliasing(gfx);
        }
        super.paint(gfx);
        if (_antiAliased) {
            SwingUtil.restoreAntiAliasing(gfx, ohints);
        }
    }

}
