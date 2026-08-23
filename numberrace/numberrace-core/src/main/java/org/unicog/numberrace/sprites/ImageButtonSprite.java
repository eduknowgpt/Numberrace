package org.unicog.numberrace.sprites;

import org.unicog.numberrace.util.ScrollSafeLinePath;

import com.threerings.media.image.Mirage;
import com.threerings.media.sprite.ImageSprite;
import com.threerings.media.sprite.action.ArmingSprite;
import com.threerings.media.sprite.action.CommandSprite;
import com.threerings.media.sprite.action.DisableableSprite;
import com.threerings.media.sprite.action.HoverSprite;
import com.threerings.media.util.MultiFrameImage;
import com.threerings.media.util.Path;
import com.threerings.media.util.SingleFrameImageImpl;

public class ImageButtonSprite extends ImageSprite implements CommandSprite,
        ArmingSprite, DisableableSprite, HoverSprite {

    private final String cmd;
    private final Object cmdArg;
    protected boolean enabled = true;
    private boolean armed;
    protected boolean hovered;
    private boolean animate_on_hover = true;

    public ImageButtonSprite(String cmd, Object cmdArg) {
        this((MultiFrameImage) null, cmd, cmdArg);
    }

    public ImageButtonSprite(Mirage image, String cmd, Object cmdArg) {
        this(new SingleFrameImageImpl(image), cmd, cmdArg);
    }

    public ImageButtonSprite(MultiFrameImage frames, String cmd, Object cmdArg) {
        super(frames);
        this.cmd = cmd;
        this.cmdArg = cmdArg;
        setFrameRate(4);
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

    public void setArmed(boolean armed) {
        if (this.armed != armed) {
            this.armed = armed;
            invalidate();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setHovered(boolean hovered) {
        if (this.hovered != hovered) { // not sure it is necessary 
            this.hovered = hovered;
            if (animate_on_hover) {
                if (hovered) {
                    setAnimationMode(TIME_BASED);
                } else {
                    setAnimationMode(NO_ANIMATION);
                    setFrameIndex(0, false);
                }
            }
            invalidate();
        }
    }

    @Override
    public void viewLocationDidChange(int dx, int dy) {
        super.viewLocationDidChange(dx, dy);
        if (_renderOrder >= HUD_LAYER && _path instanceof ScrollSafeLinePath) {
            ((ScrollSafeLinePath) (_path)).viewLocationDidChange(dx, dy);
        }

    }

    @Override
    public void move(Path path) {
        setFrameRate(4);
        setAnimationMode(TIME_BASED);
        super.move(path);
    }

    @Override
    public void pathCompleted(long timestamp) {
        super.pathCompleted(timestamp);
        setFrameIndex(0, false);
    }

    @Override
    public void cancelMove() {
        super.cancelMove();
        setFrameIndex(0, false);
    }

    public void setAnimateOnHover(boolean animate_on_hover) {
        this.animate_on_hover = animate_on_hover;
    }

    public boolean isAnimateOnHover() {
        return animate_on_hover;
    }

    public void resetToFirstFrame() {
        setFrameIndex(0, false);
    }
}
