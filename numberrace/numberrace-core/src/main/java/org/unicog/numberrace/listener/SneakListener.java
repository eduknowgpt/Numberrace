package org.unicog.numberrace.listener;

import com.threerings.media.sprite.Sprite;
import com.threerings.media.util.Path;

public interface SneakListener {

    public void sneakCancelled(Sprite sprite, Path path);

    public void sneakCompleted(Sprite sprite, Path path, long when);
}
