package org.unicog.numberrace.observer;

import com.threerings.media.sprite.PathObserver;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.util.Path;
import org.unicog.numberrace.listener.SneakListener;


public class SneakObserver implements PathObserver {

    private SneakListener listener;

        public void setSneakListener(SneakListener listener) {
            this.listener = listener;
        }

        public void pathCancelled(Sprite sprite, Path path) {
            this.listener.sneakCancelled(sprite, path);
        }

        public void pathCompleted(Sprite sprite, Path path, long when) {
            this.listener.sneakCompleted(sprite, path, when);
        }

    }