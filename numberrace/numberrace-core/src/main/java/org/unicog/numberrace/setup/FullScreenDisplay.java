package org.unicog.numberrace.setup;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferStrategy;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.unicog.numberrace.screens.ScaleUtils;
import org.unicog.numberrace.vars.GraphicsVariables;

public class FullScreenDisplay implements Display {

    private static final Logger log = Logger.getLogger("NUMBERRACE");
    private GraphicsEnvironment genv;
    private GraphicsDevice gd;
    //    private DisplayMode gameDisplayMode;
    private boolean fsem;
    private Dimension actualSize;

    /*
     * (non-Javadoc)
     * 
     * @see org.unicog.numberrace.setup.Display#init(java.awt.Window)
     */
    public boolean init(final JFrame fsWindow) {

        genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        gd = genv.getDefaultScreenDevice();

        gd.setFullScreenWindow(fsWindow);

        boolean workAroundFSBUG = ((System.getProperty("os.name")
                                          .contains("OS X") && System.getProperty("java.version")
                                                                     .startsWith("1.7")));

        if (workAroundFSBUG) {
            fsWindow.setVisible(false);
            fsWindow.setVisible(true);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fsWindow.setVisible(true);
            }
        });

        GraphicsConfiguration gc = fsWindow.getGraphicsConfiguration();
        Rectangle screenBounds = gc.getBounds();

        double resolutionCoef = Math.min(screenBounds.getWidth()
                / GraphicsVariables.DISPLAY_WIDTH, screenBounds.getHeight()
                / GraphicsVariables.DISPLAY_HEIGHT);

        if (resolutionCoef > 0) {
            ScaleUtils.resolutionCoef = resolutionCoef;
            actualSize = new Dimension(
                    (int) (GraphicsVariables.DISPLAY_WIDTH * resolutionCoef),
                    (int) (GraphicsVariables.DISPLAY_HEIGHT * resolutionCoef));
        } else {
            actualSize = new Dimension(GraphicsVariables.DISPLAY_WIDTH,
                    GraphicsVariables.DISPLAY_HEIGHT);
        }

        return fsem;
    }

    //    private void createBufferStrategy() {
    //        final Window window = gd.getFullScreenWindow();
    //        if (window != null) {
    //            Runnable cbsr = new Runnable() {
    //
    //                public void run() {
    //                    BufferCapabilities bc = new BufferCapabilities(
    //                            new ImageCapabilities(true), new ImageCapabilities(
    //                                    true), null);
    //                    try {
    //                        window.createBufferStrategy(2, bc);
    //                    } catch (AWTException e) {
    //                        e.printStackTrace();
    //                        window.createBufferStrategy(2);
    //                    }
    //                }
    //            };
    //
    //            cbsr.run();
    //        }
    //
    //    }

    //    private void doResize() {
    //        final Window fsWindow = gd.getFullScreenWindow();
    //        if (fsWindow != null) {
    //            Runnable resize = new Runnable() {
    //
    //                public void run() {
    //                    fsWindow.setSize(gameDisplayMode.getWidth(),
    //                            gameDisplayMode.getHeight());
    //                    DisplayMode currentDM = gd.getDisplayMode();
    //                    if (currentDM.getHeight() != gameDisplayMode.getHeight()) {
    //                        fsWindow
    //                                .setLocation(fsWindow.getBounds().x, (currentDM
    //                                        .getHeight() - gameDisplayMode
    //                                        .getHeight()) / 2);
    //                    }
    //                    if (currentDM.getWidth() != gameDisplayMode.getWidth()) {
    //                        fsWindow.setLocation(
    //                                (currentDM.getWidth() - gameDisplayMode
    //                                        .getWidth()) / 2,
    //                                fsWindow.getBounds().y);
    //                    }
    //
    //                }
    //
    //            };
    //            if (SwingUtilities.isEventDispatchThread()) {
    //                resize.run();
    //            } else {
    //                try {
    //                    SwingUtilities.invokeAndWait(resize);
    //                } catch (InterruptedException e) {
    //                    // TODO Auto-generated catch block
    //                    e.printStackTrace();
    //                } catch (InvocationTargetException e) {
    //                    // TODO Auto-generated catch block
    //                    e.printStackTrace();
    //                }
    //            }
    //            fsWindow.invalidate();
    //        }
    //    }

    /*
     * (non-Javadoc)
     * 
     * @see org.unicog.numberrace.setup.Display#restoreScreen()
     */
    public void restoreScreen() {
        final Window fsWindow = gd.getFullScreenWindow();
        try {
            gd.setFullScreenWindow(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (fsWindow != null) {
            fsWindow.dispose();
        }
    }

    //    private boolean checkDevice(GraphicsDevice gdev, DisplayMode ndm) {
    //        if (gdev.getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
    //            if (!ndm.equals(gdev.getDisplayMode())) {
    //                log
    //                        .config("need to check if display supports mode change and are capable of giving us needed 1024x768x16\n"
    //                                + gdev.getIDstring());
    //                if (gdev.isDisplayChangeSupported()) {
    //                    DisplayMode[] dms = gdev.getDisplayModes();
    //                    for (int i = 0; i < dms.length; i++) {
    //                        DisplayMode sdm = dms[i];
    //                        if (sdm.getHeight() == ndm.getHeight()
    //                                && sdm.getWidth() == ndm.getWidth()
    //                                && (sdm.getBitDepth() == ndm.getBitDepth() || sdm
    //                                        .getBitDepth() == DisplayMode.BIT_DEPTH_MULTI)) {
    //                            return true;
    //                        }
    //                    }
    //                } else {
    //                    log.config("display does not support mode change");
    //                }
    //            } else {
    //                log.config("Display already in needed Mode");
    //                return true;
    //            }
    //        }
    //        return false;
    //    }

    /*
     * (non-Javadoc)
     * 
     * @see org.unicog.numberrace.setup.Display#getGraphics()
     */
    public Graphics2D getGraphics() {
        Window window = gd.getFullScreenWindow();
        if (window != null) {
            return (Graphics2D) window.getBufferStrategy().getDrawGraphics();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.unicog.numberrace.setup.Display#show()
     */
    public void show() {
        Window fsWindow = gd.getFullScreenWindow();
        if (fsWindow != null) {
            BufferStrategy bs = fsWindow.getBufferStrategy();
            if (!bs.contentsLost()) {
                bs.show();
            }
        }
        Toolkit.getDefaultToolkit().sync();
    }

    public Dimension getActualResolution() {
        return actualSize;
    }

}
