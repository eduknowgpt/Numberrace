package org.unicog.numberrace;

import java.awt.Color;
import java.io.File;
import java.util.Random;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.unicog.numberrace.algorithms.NumCompAlgManager;
import org.unicog.numberrace.data.Student;
import org.unicog.numberrace.debug.DebugFrame;
import org.unicog.numberrace.setup.GamePreferences;
import org.unicog.numberrace.swing.SafeComboBox;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.vars.ThemeVariables;

import com.threerings.media.MediaPrefs;

public class Game {

    static final Logger log = Logger.getLogger("NUMBERRACE");

    public static void main(String args[]) { // **** program starts here ****;
        if (GamePreferences.setupPreferences()) {

            /*
             * When active a flip-buffer will be used to manage our rendering,
             * otherwise a volatile back buffer is used. By default narya uses
             * flip for Mac OS X, but than screen blinks, so let's use volatile
             * back buffer
             */

            MediaPrefs.config.setValue("narya.media.frame", false);
            if (GamePreferences.setupLanguage()) {

                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                    //                UIManager.setLookAndFeel(UIManager
                    //                        .getCrossPlatformLookAndFeelClassName());
                } catch (Exception e) {
                }
                UIManager.put(SafeComboBox.uiClassID,
                              "org.unicog.numberrace.swing.SafeComboBoxUI");
                UIManager.put("ComboBox.background", Color.WHITE);
                UIManager.put("ComboBox.disabledForeground", Color.BLACK);

                int choice = GamePreferences.CONFIG.getValue("windowed-mode", 0);

                //for (String arg : args) {
                for (int i = 0; i < args.length; ++i) {
                    String arg = args[i];
                    if (arg.endsWith("in-window")) {
                        GamePreferences.CONFIG.setValue("windowed-mode", 1);
                        choice = 1; // doing it after config.getvalue in case config does not work in the system (which is unlikely, but ...)
                    } else if (arg.endsWith("fullscreen")) {
                        GamePreferences.CONFIG.setValue("windowed-mode", 0);
                        choice = 0; // doing it after config.getvalue in case config does not work in the system (which is unlikely, but ...)
                    } else if (arg.startsWith("-ccl=")) {
                        File cclConfigFile = new File(arg.substring(5));
                        if (cclConfigFile.exists()) {
                            NumCompAlgManager.cclPATH = cclConfigFile.getAbsolutePath();
                            log.info(String.format("Using file [%s] to config CCLs",
                                                   NumCompAlgManager.cclPATH));
                        } else {
                            log.info(String.format("Cann't find [%s]. Using resource [%s] to config CCLs.",
                                                   cclConfigFile.getName(),
                                                   NumCompAlgManager.cclPATH));
                        }
                    }
                }

                // Just give option of fullscreen or windowed mode
                //                Object[] options = {
                //                        Messages.getString("Game.fullScreen"), Messages.getString("Game.window") }; //$NON-NLS-1$ //$NON-NLS-2$
                //            if (!DebugFrame.DEBUG) {
                //                choice = JOptionPane
                //                        .showOptionDialog(
                //                                null,
                //                                Messages.getString("Game.mode_q"), //$NON-NLS-1$
                //                                Messages.getString("Game.numberRace"), //$NON-NLS-1$
                //                                JOptionPane.DEFAULT_OPTION,
                //                                JOptionPane.QUESTION_MESSAGE, null, options,
                //                                options[0]);
                //            }

                if (choice == 0 || choice == 1) {
                    final boolean fullScreen = (choice == 0);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            GameObject go = GameObject.getInstance();
                            go.initialize(fullScreen);

                            if (DebugFrame.DEBUG) { //TODO in FS does not work. make it work later.
                                DebugFrame debugFrame = new DebugFrame();
                                debugFrame.pack();
                                debugFrame.setVisible(true);
                                go.setStudent(createDummyStudent());
                            }
                            go.start();

                        }
                    });
                }

            }
        } else {
            log.severe("Game Preferences Initialization failed. Exiting...");
        }
    }

    private static Student createDummyStudent() {
        // put in a fake student, so we have a data file to write to
        boolean[][] characAccess = { { true, true, true, true, true, true },
                { true, true, true, true, true, true } };

        Random rnd = new Random(System.currentTimeMillis());

        byte[][] bs = new byte[ThemeVariables.NUMBER_OF_THEMES][Constants.NUMBER_POSS_REWARDS];
        for (int i = 0; i < ThemeVariables.NUMBER_OF_THEMES; i++) {
            for (int j = 0; j < bs[i].length; j++) {
                bs[i][j] = (byte) rnd.nextInt(2);
            }

        }
        return new Student(1,
                "Java", "Race", 10, "CP", 0, "INTERMEDIATE", 0, 0, 0, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                characAccess, bs);
    }

}
