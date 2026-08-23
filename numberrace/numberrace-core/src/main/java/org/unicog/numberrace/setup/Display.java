package org.unicog.numberrace.setup;

import java.awt.Dimension;
import java.awt.Graphics2D;

import javax.swing.JFrame;

public interface Display {

    /**
     * 
     * @param fsWindow
     * @return whether full-screen EXCLUSIVE mode were initialized
     */
    public boolean init(JFrame fsWindow);

    public void restoreScreen();

    /**
     * Do not forget to dispose() graphics object
     * 
     * @return
     */
    public Graphics2D getGraphics();

    public void show();

    public Dimension getActualResolution();

}