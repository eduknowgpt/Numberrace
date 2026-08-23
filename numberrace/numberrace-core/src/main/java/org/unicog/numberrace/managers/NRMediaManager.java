package org.unicog.numberrace.managers;

import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.samskivert.util.ComparableArrayList;
import com.samskivert.util.SortableArrayList;
import com.threerings.media.AbstractMedia;
import com.threerings.media.FrameManager;
import com.threerings.media.Log;
import com.threerings.media.MediaHost;
import com.threerings.media.MetaMediaManager;
import com.threerings.media.animation.AnimationManager;
import com.threerings.media.sprite.SpriteManager;

//TODO: re-write in more understandable way :)
/**
 * This class renders media according to the RenderOrder value. Media with
 * smallest value painted first. I decided to have it because I want to combine
 * Sprites and Animation on different layers. In standard implementation paining
 * goes in like this: if order is negative it renders ANIMATIONS and than
 * SPRITES if order not NEGATIVE it renders SPRITES and than ANIMATIONS. But I
 * can not use it in a situation when I want sprite, some animation inside and
 * another sprite on top.
 * 
 * @author Alexander Maslov (at) gmail.com
 * 
 */
public class NRMediaManager extends MetaMediaManager {

    /** Our render-order sorted list of media. */
    protected SortableArrayList<AbstractMedia> allMedia = new SortableArrayList<AbstractMedia>();

    public NRMediaManager(FrameManager framemgr, MediaHost host) {
        super(framemgr, host);

        _animmgr = new AnimationManager() {
            @Override
            protected boolean insertMedia(AbstractMedia media) {
                if (super.insertMedia(media)) {
                    allMedia.insertSorted(media, RENDER_ORDER);
                    return true;
                }
                return false;
            }

            @Override
            protected boolean removeMedia(AbstractMedia media) {
                if (super.removeMedia(media)) {
                    return allMedia.remove(media);
                }
                return false;
            }

            @Override
            protected void clearMedia() {
                if (_tickStamp > 0) {
                    Log.warning("Egads! Requested to clearMedia() during a tick.");
                    Thread.dumpStack();
                }

                for (int ii = _media.size() - 1; ii >= 0; ii--) {
                    allMedia.remove(_media.get(ii));
                }

                super.clearMedia();
            }

            @Override
            public void renderOrderDidChange(AbstractMedia media) {
                super.renderOrderDidChange(media);

                if (_tickStamp > 0) {
                    Log.warning("Egads! Render order changed during a tick.");
                    Thread.dumpStack();
                }

                allMedia.remove(media);
                allMedia.insertSorted(media, RENDER_ORDER);
            }

        };
        _spritemgr = new SpriteManager() {
            @Override
            protected boolean insertMedia(AbstractMedia media) {
                if (super.insertMedia(media)) {
                    allMedia.insertSorted(media, RENDER_ORDER);
                    return true;
                }
                return false;
            }

            @Override
            protected boolean removeMedia(AbstractMedia media) {
                if (super.removeMedia(media)) {
                    return allMedia.remove(media);
                }
                return false;
            }

            @Override
            protected void clearMedia() {
                if (_tickStamp > 0) {
                    Log.warning("Egads! Requested to clearMedia() during a tick.");
                    Thread.dumpStack();
                }

                for (int ii = _media.size() - 1; ii >= 0; ii--) {
                    allMedia.remove(_media.get(ii));
                }

                super.clearMedia();
            }

            @Override
            public void renderOrderDidChange(AbstractMedia media) {
                super.renderOrderDidChange(media);

                if (_tickStamp > 0) {
                    Log.warning("Egads! Render order changed during a tick.");
                    Thread.dumpStack();
                }

                allMedia.remove(media);
                allMedia.insertSorted(media, RENDER_ORDER);
            }

        };

        // initialize our managers
        _animmgr.init(host, _remgr);
        _spritemgr.init(host, _remgr);
    }

    @Override
    public void paintMedia(Graphics2D gfx, int layer, Rectangle dirty) {
        for (int ii = 0, nn = allMedia.size(); ii < nn; ii++) {
            AbstractMedia media = allMedia.get(ii);
            int order = media.getRenderOrder();
            try {
                if (((layer == ALL) || (layer == FRONT && order >= 0) || (layer == BACK && order < 0))
                        && dirty.intersects(media.getBounds())) {
                    media.paint(gfx);
                }

            } catch (Exception e) {
                Log.warning("Failed to render media [media=" + media + ", e="
                        + e + "].");
                Log.logStackTrace(e);
            }
        }
    }

}
