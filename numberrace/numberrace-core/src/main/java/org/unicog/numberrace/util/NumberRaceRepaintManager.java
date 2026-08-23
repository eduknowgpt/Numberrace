package org.unicog.numberrace.util;

import javax.swing.JComponent;
import javax.swing.RepaintManager;

public class NumberRaceRepaintManager extends RepaintManager {

    //    private final static Logger log = Logger.getLogger("NUMBERRACE");

    private boolean dirty;

    public NumberRaceRepaintManager() {
        super();
        setDoubleBufferingEnabled(false);
    }

    public synchronized void addDirtyRegion(JComponent c, int x, int y, int w,
            int h) {
        dirty = true;
    }

    public synchronized void addInvalidComponent(JComponent invalidComponent) {
        dirty = true;
    }

    public void markCompletelyDirty(JComponent component) {
        dirty = true;
    }

    public void paintDirtyRegions() {
    }

    public synchronized boolean resetDirty() {
        boolean ret = dirty;
        dirty = false;
        return ret;
    };

}
