package org.unicog.numberrace.util;

import java.awt.Point;

import com.threerings.media.ViewTracker;
import com.threerings.media.util.LinePath;

public class ScrollSafeLinePath extends LinePath implements ViewTracker {

    public void viewLocationDidChange(int dx, int dy) {
        _source.translate(dx, dy);
        _dest.translate(dx, dy);
    }

    public ScrollSafeLinePath(int x1, int y1, int x2, int y2, long duration) {
        super(x1, y1, x2, y2, duration);
    }

    public ScrollSafeLinePath(Point dest, long duration) {
        super(dest, duration);
    }

    public ScrollSafeLinePath(Point source, Point dest, long duration) {
        super(source, dest, duration);
    }
}
