package org.unicog.numberrace.setup;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferStrategy;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.unicog.numberrace.screens.ScaleUtils;
import org.unicog.numberrace.vars.GraphicsVariables;

public class WindowedDisplay implements Display {

    private static final Logger log = Logger.getLogger("NUMBERRACE");
    private GraphicsDevice gd;
    private DisplayMode gameDisplayMode;
    private Window window;
    protected Dimension actualSize;

    /*
     * (non-Javadoc)
     * 
     * @see org.unicog.numberrace.setup.Display#init(java.awt.Window)
     */
    public boolean init(JFrame fsWindow) {
        this.window = fsWindow;

        gameDisplayMode = new DisplayMode(GraphicsVariables.DISPLAY_WIDTH,
                GraphicsVariables.DISPLAY_HEIGHT, 16,
                DisplayMode.REFRESH_RATE_UNKNOWN);
        GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        gd = genv.getDefaultScreenDevice();

        fsWindow.setVisible(true);
        doResize();
        return false;
    }

    private void doResize() {
        Runnable resize = new Runnable() {

            public void run() {

                GraphicsConfiguration gc = window.getGraphicsConfiguration();
                Rectangle screenBounds = gc.getBounds();
                Insets sInsets = Toolkit.getDefaultToolkit()
                                        .getScreenInsets(gc);
                Insets wInsets = window.getInsets();
                //                System.out.println("Bounds: " + screenBounds + "\nsInsets: " + sInsets + "\nwInsets: " + wInsets);

                screenBounds.width -= (sInsets.left + sInsets.right
                        + wInsets.left + wInsets.right);
                screenBounds.height -= (sInsets.top + sInsets.bottom
                        + wInsets.top + wInsets.bottom);

                //                System.out.println("NewBounds: " + screenBounds);

                double resolutionCoef = Math.min(screenBounds.getWidth()
                                                         / GraphicsVariables.DISPLAY_WIDTH,
                                                 screenBounds.getHeight()
                                                         / GraphicsVariables.DISPLAY_HEIGHT);
                //                double resolutionCoef = Math.min(1.,
                //                                                 Math.min(screenBounds.getWidth()
                //                                                          / GraphicsVariables.DISPLAY_WIDTH,
                //                                                          screenBounds.getHeight()
                //                                                          / GraphicsVariables.DISPLAY_HEIGHT));

                //                if (resolutionCoef < 1 && resolutionCoef > 0) {
                ScaleUtils.resolutionCoef = resolutionCoef;
                actualSize = new Dimension(
                        (int) (gameDisplayMode.getWidth() * resolutionCoef),
                        (int) (gameDisplayMode.getHeight() * resolutionCoef));
                //                } else {
                //                    actualSize = new Dimension(gameDisplayMode.getWidth(),
                //                            gameDisplayMode.getHeight());
                //                }

                //                System.out.println("NewBounds: " + screenBounds + "\nActualSize: " + actualSize);

                Dimension wDim = new Dimension(actualSize.width + wInsets.left
                        + wInsets.right, actualSize.height + wInsets.top
                        + wInsets.bottom);
                window.setBounds((screenBounds.width - wDim.width) / 2
                        + screenBounds.x, (screenBounds.height - wDim.height)
                        / 2 + screenBounds.y, wDim.width, wDim.height);
            }

        };
        if (SwingUtilities.isEventDispatchThread()) {
            resize.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(resize);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        window.validate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.unicog.numberrace.setup.Display#restoreScreen()
     */
    public void restoreScreen() {
        window.dispose();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.unicog.numberrace.setup.Display#getGraphics()
     */
    public Graphics2D getGraphics() {
        return (Graphics2D) window.getBufferStrategy().getDrawGraphics();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.unicog.numberrace.setup.Display#show()
     */
    public void show() {
        BufferStrategy bs = window.getBufferStrategy();
        if (!bs.contentsLost()) {
            bs.show();
        }
        Toolkit.getDefaultToolkit().sync();
    }

    public Dimension getActualResolution() {
        return actualSize;
    }

}
