package org.unicog.numberrace.debug;

import java.awt.GridLayout;

import javax.swing.JFrame;

import com.samskivert.swing.RuntimeAdjust;
import com.samskivert.swing.RuntimeAdjust.BooleanAdjust;
import com.threerings.media.MediaPrefs;

public class DebugFrame extends JFrame {
    public final static boolean DEBUG = false;

    private BooleanAdjust displayFPS;

    private BooleanAdjust verboseSound;

    private BooleanAdjust useFlip;

    public DebugFrame() {
        super("NR DebugFrame");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    @Override
    protected void frameInit() {
        super.frameInit();

        displayFPS = new RuntimeAdjust.BooleanAdjust("Display FPS",
                "narya.media.fps_display", MediaPrefs.config, false);
        verboseSound = new RuntimeAdjust.BooleanAdjust("Verbose sound",
                "narya.media.sound.verbose", MediaPrefs.config, false);

        useFlip = new RuntimeAdjust.BooleanAdjust(
                "When active a flip-buffer will be used to manage our rendering, otherwise a "
                        + "volatile back buffer is used [requires restart]",
                "narya.media.frame",
                // back buffer rendering doesn't work on the Mac, so we default to flip buffer on that
                // platform; we still allow it to be toggled so that we can easily test things when
                // they release new JVMs
                //                MediaPrefs.config, RunAnywhere.isMacOS());
                MediaPrefs.config, false);

        getContentPane().setLayout(new GridLayout(3, 1));
        getContentPane().add(useFlip.getEditor());
        getContentPane().add(displayFPS.getEditor());
        getContentPane().add(verboseSound.getEditor());
    }
}
