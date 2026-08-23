package org.unicog.numberrace;

import org.unicog.numberrace.algorithms.GameTurn;
import org.unicog.numberrace.others.DotArray;
import org.unicog.numberrace.screens.ActionState;
import org.unicog.numberrace.screens.Player;

import static org.unicog.numberrace.screens.ScaleUtils.i;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.unicog.numberrace.data.DataFileHandler;
import org.unicog.numberrace.data.Student;
import org.unicog.numberrace.managers.HazardManager;
import org.unicog.numberrace.managers.NumCompManager;
import org.unicog.numberrace.screens.CharacterScreen;
import org.unicog.numberrace.screens.ChoiceScreen;
import org.unicog.numberrace.screens.GameOverScreen;
import org.unicog.numberrace.screens.InstructionScreen;
import org.unicog.numberrace.screens.RegistrationScreen;
import org.unicog.numberrace.screens.SaveScreen;
import org.unicog.numberrace.screens.Screen;
import org.unicog.numberrace.screens.ThemeScreen;
import org.unicog.numberrace.screens.TitleScreen;
import org.unicog.numberrace.screens.ZooScreen;
import org.unicog.numberrace.setup.Display;
import org.unicog.numberrace.setup.FullScreenDisplay;
import org.unicog.numberrace.setup.GamePreferences;
import org.unicog.numberrace.setup.WindowedDisplay;
import org.unicog.numberrace.sound.SoundListener;
import org.unicog.numberrace.sound.SoundManager;
import org.unicog.numberrace.sprites.DotContainerSprite;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.Messages;
import org.unicog.numberrace.util.Utilities;
import org.unicog.numberrace.vars.GraphicsVariables;
import org.unicog.numberrace.vars.ThemeVariables;

import com.samskivert.swing.Controller;
import com.threerings.media.FrameManager;
import com.threerings.media.FrameManager.ManagedRoot;

import org.unicog.numberrace.listener.GameListener;
import org.unicog.numberrace.listener.NumCompListener;

public class GameObject extends NumCompListener {

    private static final String LOADING = "loading";
    private static final String MAINMENU = "mainmenu";
    private static final String PAUSE = "pause";

    //    protected static final Dimension size = new Dimension(
    //            GraphicsVariables.DISPLAY_WIDTH, GraphicsVariables.DISPLAY_HEIGHT);
    //    public Dimension wsize = new Dimension(
    //            GraphicsVariables.DISPLAY_WIDTH, GraphicsVariables.DISPLAY_HEIGHT);
    final private Component firstBox = Box.createGlue();
    final private Component secondBox = Box.createGlue();

    private Random randomNumber = new Random();

    private int playerCharacter = randomNumber.nextInt(6);
    private int opponentCharacter = randomNumber.nextInt(4);

    @Override
    public void setActionState(ActionState actionState) {
        choiceScreen.setActionState(actionState);
    }

    @Override
    public void setCurrentTurn(GameTurn currentTurn) {
        choiceScreen.setCurrentTurn(currentTurn);
    }

    @Override
    public DotContainerSprite getDotContainer(int additionSide) {
        return choiceScreen.getDotContainer(additionSide);
    }

    @Override
    public void setHazardLevel(boolean hazards) {
        choiceScreen.setHazardLevel(hazards);
    }

    @Override
    public void setHazards(int rangeCeilling, boolean gameBeginning) {
        choiceScreen.setHazards(rangeCeilling, gameBeginning);
    }

    @Override
    public void setBoardLength(int length) {
        Constants.LAST_SQUARE = length;
    }

    @Override
    public void openContainer(int LEFT) {
        choiceScreen.openContainer(LEFT);
    }

    @Override
    public void startSneaking(long l) {
        choiceScreen.startSneaking(l);
    }

    @Override
    public void opponentTalks(String string) {
        choiceScreen.opponentTalks(string);
    }

    @Override
    public void openContainer(int sideSelected, boolean b) {
        choiceScreen.openContainer(sideSelected, initialized);
    }

    @Override
    public void grabAndLineUpDotsOnCarpet(int player, int sideSelected,
            GameTurn currentTurn) {
        choiceScreen.grabAndLineUpDotsOnCarpet(player,
                                               sideSelected,
                                               currentTurn);
    }

    @Override
    public void grabAndSubtractDots(int characterID, int side,
            GameTurn currentTurn) {
        choiceScreen.grabAndSubtractDots(side, side, currentTurn);
    }

    @Override
    public Player getPlayer(int PLAYER) {
        return choiceScreen.getPlayer(PLAYER);
    }

    @Override
    public void clearCarpet() {
        choiceScreen.clearCarpet();
    }

    @Override
    public DotArray[] getDots() {
        return choiceScreen.dots;
    }

    //    private int playerPosition;
    //    private int opponentPosition;

    public final class MainController extends Controller {
        public void changeState(Object source, final GameStates newState) {
            soundManager.play("iconClick", new SoundListener() {
                public void run() {
                    GameObject.this.changeState(newState);
                }
            });
        }

        public void chooseCharacter(Object source, int character) {
            if (player.getCharacAccess(character, ThemeVariables.getLevel())) {
                GameObject.this.playerCharacter = character;
                GameObject.this.getChoiceScreen().setPlayerCharacter(character);

                randomNumber.setSeed(System.currentTimeMillis());
                GameObject.this.opponentCharacter = randomNumber.nextInt(4);
                GameObject.this.getChoiceScreen()
                               .setOpponentCharacter(GameObject.this.opponentCharacter);

                GameObject.this.changeState(GameStates.CHOICE);
            } else {
                getSoundManager().stopAllSounds();
                getSoundManager().play("characterLocked");
            }
        }
    }

    public enum GameStates {
        START, TITLE, REGISTRATION, THEME, INSTRUCTIONS, CHARACTERS, CHOICE, GAMEOVER, SAVE, ZOO, END, MENU, PAUSE;

        Set<GameStates> next;

        static {
            START.nexts(TITLE,
                        REGISTRATION,
                        THEME,
                        INSTRUCTIONS,
                        CHARACTERS,
                        CHOICE,
                        GAMEOVER,
                        SAVE,
                        ZOO,
                        MENU,
                        END);
            TITLE.nexts(REGISTRATION, PAUSE, END);
            REGISTRATION.nexts(THEME, PAUSE, END); //, SAVE, ZOO, INSTRUCTIONS);
            THEME.nexts(INSTRUCTIONS, PAUSE, MENU, END);
            INSTRUCTIONS.nexts(CHARACTERS, PAUSE, MENU, END);
            CHARACTERS.nexts(CHOICE, PAUSE, MENU, END);
            CHOICE.nexts(MENU, PAUSE, GAMEOVER, END);
            GAMEOVER.nexts(SAVE, THEME, PAUSE, END);
            SAVE.nexts(ZOO, PAUSE, END);
            ZOO.nexts(THEME, PAUSE, MENU, END);
            MENU.nexts(REGISTRATION, CHOICE, PAUSE, END);
        }

        private void nexts(GameStates... nexts) {
            this.next = EnumSet.copyOf(Arrays.asList(nexts));
        }

        public boolean canChangeTo(GameStates state) {
            return next.contains(state);
        }
    }

    static GameObject go;

    protected GameStates currentState;
    protected FrameManager frameManager;
    protected JFrame window;

    private TitleScreen titleScreen;
    private RegistrationScreen regScreen;
    private ThemeScreen themeScreen;
    private InstructionScreen instructionScreen;
    private InstructionScreen creditScreen;
    private CharacterScreen characterScreen;

    private boolean initialized;
    protected boolean running;
    private boolean transition;

    protected SoundManager soundManager;
    protected NRRunnableQueue taskQueue;

    protected Display display;
    private ThemeVariables theme;
    private DataFileHandler dataFileHandler;

    private Student player;

    protected ChoiceScreen choiceScreen;
    private Robot robot;
    //    private ResourceManager resourceManager;
    //    private ImageManager imageManager;

    protected GameOverScreen gameOverScreen;

    protected SaveScreen saveScreen;

    protected ZooScreen zooScreen;

    //    private JLabel loadingLabel;

    private boolean paused;

    private GameStates tmpState;

    private JPanel contentPane;
    private JPanel loadingScreen;
    private JPanel pausePane;
    private NumCompManager ncm;
    private WindowListener windowListener;

    GameObject() {
        currentState = GameStates.START;
        initialized = false;

        //ncm = new NumCompManager(this,this.taskQueue,this.soundManager);
        //        loadingLabel = new JLabel("Loading...");

        //        ((JComponent)firstBox).setAlignmentX(JComponent.CENTER_ALIGNMENT);
        //        ((JComponent)secondBox).setAlignmentX(JComponent.CENTER_ALIGNMENT);
    }

    public void changeState(final GameStates newState) {
        assert (initialized);
        if (!running || transition) {
            Utilities.log.warning("Do not do anything - Game not running or in transition state :)");
            return;
        }

        if (currentState == GameStates.MENU
                || currentState.canChangeTo(newState)) {

            if (currentState == GameStates.MENU) {
                hideMenu();
                currentState = tmpState;
                unpause();
                if (newState == GameStates.MENU) {
                    return;
                }
            }

            if (newState != GameStates.MENU) {
                transition = true;
                taskQueue.reset();
                soundManager.stopAllSounds();
            } else {
                pause();
            }

            if (!SwingUtilities.isEventDispatchThread()) {
                proceedChanging(newState);
            } else {
                taskQueue.postRunnable(new Runnable() {

                    public void run() {
                        proceedChanging(newState);
                    }
                });
            }

        } else {
            Utilities.log.warning("Can not go from state ["
                    + currentState.name() + "] to state [" + newState.name()
                    + "]");
        }
    }

    private void pause() {
        paused = true;
        taskQueue.setPaused(true);
        soundManager.setPaused(true);
        Screen scree4State = getScree4State(currentState);
        if (scree4State != null) {
            scree4State.pause();
        }

        JComponent glassPane = (JComponent) window.getGlassPane();
        ((CardLayout) glassPane.getLayout()).show(glassPane, PAUSE);
        glassPane.setVisible(true);

        Utilities.log.info("Paused");
    }

    private void unpause() {
        paused = false;
        taskQueue.setPaused(false);
        soundManager.setPaused(false);
        Screen scree4State = getScree4State(currentState);
        if (scree4State != null) {
            scree4State.unpause();
        }

        window.getGlassPane().setVisible(false);
        Utilities.log.info("UnPaused");
    }

    private void proceedChanging(GameStates newState) {

        if (newState == GameStates.END) {
            stop();
            return;
        }

        if (newState == GameStates.MENU) {
            showMenu();
            tmpState = currentState;
            currentState = newState;
            return;
        }

        Utilities.log.info("Changin states : [" + currentState.name()
                + "] -> [" + newState.name() + "]");
        //        switch (currentState) {
        //        case START:
        //            //            switch (newState) {
        //            //            case REGISTRATION:
        //            //                changeScreen(getRegistrationScreen());
        //            //                break;
        //            //            case THEME:
        //            //                changeScreen(getThemeScreen());
        //            //                break;
        //            //            case INSTRUCTIONS:
        //            //                changeScreen(getInstructionsScreen());
        //            //                break;
        //            //            case CHARACTERS:
        //            //                changeScreen(getCharactersScreen());
        //            //                break;
        //            //            case CHOICE:
        //            //                changeScreen(getChoiceScreen());
        //            //                break;
        //            //
        //            //            case GAMEOVER:
        //            //                changeScreen(getGameOverScreen());
        //            //                break;
        //            //            case SAVE:
        //            //                changeScreen(getSaveScreen());
        //            //                break;
        //            //
        //            //            case ZOO:
        //            //                changeScreen(getZooScreen());
        //            //                break;
        //            //            default:
        //            //                changeScreen(getTitleScreen());
        //            //                break;
        //            //            }
        //            //
        //            break;
        //
        //        case TITLE:
        //            stopScreen(getTitleScreen());
        //            //            changeScreen(getRegistrationScreen());
        //            break;
        //        case REGISTRATION:
        //            stopScreen(getRegistrationScreen());
        //            //            changeScreen(getThemeScreen());
        //
        //            break;
        //        case THEME:
        //            stopScreen(getThemeScreen());
        //            //            changeScreen(getInstructionsScreen());
        //            break;
        //
        //        case INSTRUCTIONS:
        //            stopScreen(instructionScreen);
        //            //            changeScreen(getCharactersScreen());
        //
        //            break;
        //        case CHARACTERS:
        //            stopScreen(characterScreen);
        //            //            changeScreen(getChoiceScreen());
        //            break;
        //
        //        case CHOICE:
        //            stopScreen(choiceScreen);
        //            //            changeScreen(getGameOverScreen());
        //            break;
        //
        //        //        case BOARD:
        //        //            throw new NotImplementedException();
        //
        //        case GAMEOVER:
        //            stopScreen(gameOverScreen);
        //            //            changeScreen(getSaveScreen());
        //            break;
        //        case SAVE:
        //            stopScreen(saveScreen);
        //            break;
        //        case ZOO:
        //            stopScreen(zooScreen);
        //            //            changeScreen(getThemeScreen());
        //            break;
        //
        //        case MENU:
        //            throw new NotImplementedException();
        //
        //        default:
        //            break;
        //        }
        //
        stopScreen(getScree4State(currentState));
        changeScreen(getScree4State(newState));

        if (currentState == GameStates.TITLE) {
            titleScreen = null;
        }

        currentState = newState;
        transition = false;
    }

    private void showMenu() {
        JComponent glassPane = (JComponent) window.getGlassPane();
        ((CardLayout) glassPane.getLayout()).show(glassPane, MAINMENU);
        glassPane.setVisible(true);
    }

    private void hideMenu() {
        window.getGlassPane().setVisible(false);
        window.repaint();
    }

    private void showLoading() {
        JComponent glassPane = (JComponent) window.getGlassPane();
        ((CardLayout) glassPane.getLayout()).show(glassPane, LOADING);
        glassPane.setVisible(true);
    }

    private void hideLoading() {
        if (paused) {
            JComponent glassPane = (JComponent) window.getGlassPane();
            ((CardLayout) glassPane.getLayout()).show(glassPane, PAUSE);
        } else {
            window.getGlassPane().setVisible(false);
        }
    }

    protected Screen getScree4State(GameStates newState) {
        //        System.out.printf("Getting Screen for State %s\n", newState);
        switch (newState) {
            case TITLE:
                return getTitleScreen();
            case REGISTRATION:
                return getRegistrationScreen();
            case THEME:
                return getThemeScreen();
            case INSTRUCTIONS:
                return getInstructionsScreen();
            case CHARACTERS:
                return getCharactersScreen();
            case CHOICE:
                return getChoiceScreen();
            case GAMEOVER:
                return getGameOverScreen();
            case SAVE:
                return getSaveScreen();
            case ZOO:
                return getZooScreen();
            case MENU:
            default:
                return null;
        }
    }

    private JPanel getPausePane() {
        if (pausePane == null) {
            pausePane = new JPanel(new BorderLayout());
            pausePane.setBackground(new Color(0, 0, 0, 220));
            JLabel label = new JLabel("[Paused]");
            label.setFont(label.getFont().deriveFont(22.f));
            label.setForeground(Color.LIGHT_GRAY);
            label.setVerticalAlignment(JLabel.TOP);
            label.setHorizontalAlignment(JLabel.LEFT);
            pausePane.add(label, BorderLayout.CENTER);
        }
        return pausePane;
    }

    private JPanel getLoadingPane() {
        if (loadingScreen == null) {
            loadingScreen = new JPanel(new BorderLayout());
            loadingScreen.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            loadingScreen.setBackground(new Color(0, 0, 0, 240));
            JLabel label = new JLabel(Messages.getString("Screen.loading"));
            label.setFont(label.getFont().deriveFont(22.f));
            label.setForeground(Color.LIGHT_GRAY);
            label.setVerticalAlignment(JLabel.TOP);
            label.setHorizontalAlignment(JLabel.LEFT);
            loadingScreen.add(label, BorderLayout.CENTER);
        }
        return loadingScreen;
    }

    protected void stopScreen(final Screen scr) {
        //        if (scr != null) {
        stopScreen(scr, true);
        //        }
    }

    private void stopScreen(final Screen scr, final boolean unload) {

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                showLoading();

                //                contentPane.remove(scr.getComponent());
                //                contentPane.setVisible(false);
                contentPane.removeAll();
                if (scr == null) {
                    return;
                }
                scr.stop();
                if (unload) {
                    taskQueue.postRunnable(new Runnable() {
                        public void run() {
                            scr.unload();
                        }
                    });
                }
            }

        });
    }

    private Screen getZooScreen() {
        if (zooScreen == null) {
            invokeAndWait(new Runnable() {
                public void run() {
                    zooScreen = new ZooScreen(frameManager);
                }
            });
        }
        return zooScreen;
    }

    private Screen getSaveScreen() {
        if (saveScreen == null) {
            invokeAndWait(new Runnable() {
                public void run() {
                    saveScreen = new SaveScreen(frameManager);
                }
            });
        }
        return saveScreen;
    }

    private Screen getGameOverScreen() {
        if (gameOverScreen == null) {
            invokeAndWait(new Runnable() {

                public void run() {
                    gameOverScreen = new GameOverScreen(frameManager);
                }

            });
        }
        return gameOverScreen;
    }

    private ChoiceScreen getChoiceScreen() {
        if (choiceScreen == null) {
            invokeAndWait(new Runnable() {

                public void run() {
                    choiceScreen = new ChoiceScreen(frameManager, taskQueue,
                            soundManager, playerCharacter, opponentCharacter,
                            ncm);
                }
            });
        }
        return choiceScreen;
    }

    private Screen getCharactersScreen() {
        if (characterScreen == null) {
            invokeAndWait(new Runnable() {

                public void run() {
                    characterScreen = new CharacterScreen(frameManager);

                }
            });
        }
        return characterScreen;
    }

    private Screen getInstructionsScreen() {
        if (instructionScreen == null) {
            invokeAndWait(new Runnable() {

                public void run() {
                    instructionScreen = new InstructionScreen(frameManager,
                            "INSTRUCTIONS");

                }
            });
        }
        return instructionScreen;
    }

    private Screen getCreditScreen() {
        if (creditScreen == null) {
            invokeAndWait(new Runnable() {

                public void run() {
                    creditScreen = new InstructionScreen(frameManager,
                            "CREDITS");

                }
            });
        }
        return creditScreen;
    }

    private Screen getThemeScreen() {
        if (themeScreen == null) {
            invokeAndWait(new Runnable() {
                public void run() {
                    themeScreen = new ThemeScreen(frameManager);
                }
            });
        }
        return themeScreen;
    }

    private Screen getRegistrationScreen() {
        if (regScreen == null) {
            invokeAndWait(new Runnable() {
                public void run() {
                    regScreen = new RegistrationScreen();
                }
            });
        }
        return regScreen;
    }

    private void invokeAndWait(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private Screen getTitleScreen() {
        if (titleScreen == null) {
            invokeAndWait(new Runnable() {
                public void run() {
                    titleScreen = new TitleScreen(frameManager);
                }
            });
        }
        return titleScreen;
    }

    protected void changeScreen(final Screen scr) {
        if (scr == null) {
            throw new NullPointerException("can not change to NULL screen");
        }
        assert !SwingUtilities.isEventDispatchThread();

        scr.load();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                Dimension size = display.getActualResolution();
                JComponent cmp = (JComponent) scr.getComponent();
                cmp.setAlignmentX(JComponent.CENTER_ALIGNMENT);
                cmp.setPreferredSize(size);
                cmp.setSize(size);
                cmp.setMinimumSize(size);
                cmp.setMaximumSize(size);
                contentPane.add(firstBox);
                contentPane.add(cmp);
                contentPane.add(secondBox);
                if (paused) {
                    scr.pause();
                }
                //                RepaintManager.currentManager(contentPane).addInvalidComponent(contentPane);
                hideLoading();
                //                contentPane.setVisible(true);
                if (!cmp.requestFocusInWindow()) {
                    cmp.requestFocus();
                }
                scr.start();

            }

        });

    }

    public GameStates getCurrentState() {
        return currentState;
    }

    public static GameObject getInstance() {
        if (go == null) {
            go = new GameObject();
        }
        return go;
    }

    public void initialize(final boolean fullscreen) {
        //        System.out.printf("init %s", fullscreen);
        if (initialized && !running) {
            Utilities.log.warning("Already initilized or running");
            return;
        }

        Controller mainController = new MainController();

        window = new MainFrame(Messages.getString("Game.numberRace"),
                mainController);

        //        window.getRootPane().setBackground(Color.BLACK);
        //        p.add(Box.createGlue());
        //        p.add(getContentPane());
        //        p.add(Box.createGlue());

        window.setContentPane(getContentPane());

        soundManager = org.unicog.numberrace.sound.SoundManager.getInstance();
        taskQueue = new NRRunnableQueue();

        frameManager = FrameManager.newInstance((ManagedRoot) window);
        frameManager.registerFrameParticipant(taskQueue);

        //        resourceManager = new com.threerings.resource.ResourceManager(
        //                "resources/", ResourceProvider.getResourceClassLoader());
        //        imageManager = new ImageManager(resourceManager, window);

        initRobot();
        initialized = true;

        initializeGlobalActions();
        window.setFocusTraversalKeysEnabled(false);

        window.setUndecorated(fullscreen);
        window.setResizable(false); // prevent any user resizing

        if (fullscreen) {
            display = new FullScreenDisplay();
        } else {
            display = new WindowedDisplay();
        }

        display.init(window);
        window.setGlassPane(getGlassPane());

        ncm = new NumCompManager(this, this.taskQueue, this.soundManager);

        windowListener = new WindowAdapter() {
            private boolean reactOnActivation = !fullscreen
                    || !(isOSX() && System.getProperty("java.version")
                                          .startsWith("1.7"));

            private boolean isOSX() {
                return System.getProperty("os.name").contains("OS X");
            }

            public void windowActivated(WindowEvent e) {
                if (reactOnActivation && currentState != GameStates.MENU) {
                    unpause();
                }
            }

            public void windowClosing(WindowEvent e) {
                changeState(GameStates.END);
            }

            public void windowDeactivated(WindowEvent e) {
                if (reactOnActivation && currentState != GameStates.MENU) {
                    pause();
                }
            }
        };
    }

    private JComponent getContentPane() {
        if (contentPane == null) {
            contentPane = new JPanel();
            contentPane.setBackground(Color.BLACK);
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
            contentPane.setFocusable(true);
            contentPane.setRequestFocusEnabled(true);
        }
        return contentPane;
    }

    private Component getGlassPane() {
        JPanel glassPane = new JPanel();
        glassPane.setOpaque(true);
        glassPane.addMouseListener(new MouseAdapter() {
        });

        CardLayout cardLayout = new CardLayout();
        glassPane.setLayout(cardLayout);

        glassPane.add(getMainMenu(), MAINMENU);
        glassPane.add(getLoadingPane(), LOADING);
        glassPane.add(getPausePane(), PAUSE);

        return glassPane;
    }

    private Component getMainMenu() {
        JPanel mnu = new JPanel() {
            private final String versionStr = GamePreferences.getVersionStr();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawString(versionStr,
                             3,
                             i(GraphicsVariables.DISPLAY_HEIGHT - 3));
            }
        };

        //        panel.setOpaque(false);

        mnu.setBackground(new Color(0, 255, 255)); //light blue)
        mnu.setLayout(new BoxLayout(mnu, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(
                Messages.getString("LangVars.MENU_TITLE"));
        titleLabel.setFont(new Font("Arial Black", Font.BOLD, i(24))); //$NON-NLS-1$
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel explnLabel = new JLabel(
                Messages.getString("LangVars.MENU_SELECT_OPTION"));
        explnLabel.setFont(new Font("Arial", Font.ITALIC, i(20))); //$NON-NLS-1$
        explnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton exitSessionButton = new JButton(new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                getDataFileHandler().exitStudent();
                getNumCompManager().endGame();
            }

        });
        exitSessionButton.setFont(new Font("Arial", Font.PLAIN, i(20))); //$NON-NLS-1$
        exitSessionButton.setBackground(Color.YELLOW);
        exitSessionButton.setForeground(Color.BLACK);
        exitSessionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitSessionButton.setText(Messages.getString("LangVars.MENU_CHANGE_STUDENT"));

        JButton exitProgramButton = new JButton(getEndAction());
        exitProgramButton.setFont(new Font("Arial", Font.PLAIN, i(20))); //$NON-NLS-1$
        exitProgramButton.setBackground(Color.YELLOW);
        exitProgramButton.setForeground(Color.BLACK);
        exitProgramButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitProgramButton.setText(Messages.getString("LangVars.MENU_QUIT"));

        JButton returnToGameButton = new JButton(getMenuAction());
        returnToGameButton.setFont(new Font("Arial", Font.PLAIN, i(20))); //$NON-NLS-1$
        returnToGameButton.setBackground(Color.YELLOW);
        returnToGameButton.setForeground(Color.BLACK);
        returnToGameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        returnToGameButton.setText(Messages.getString("LangVars.MENU_RETURN"));

        Dimension ps1 = exitSessionButton.getPreferredSize();
        Dimension ps2 = exitSessionButton.getPreferredSize();
        Dimension ps3 = returnToGameButton.getPreferredSize();

        ps1.setSize(Math.max(ps1.width, ps2.width),
                    Math.max(ps1.height, ps2.height));
        ps1.setSize(Math.max(ps1.width, ps3.width),
                    Math.max(ps1.height, ps3.height));

        mnu.add(Box.createGlue());
        mnu.add(titleLabel);
        mnu.add(explnLabel);
        mnu.add(Box.createGlue());
        mnu.add(exitSessionButton);
        mnu.add(Box.createGlue());
        mnu.add(exitProgramButton);
        mnu.add(Box.createGlue());
        mnu.add(returnToGameButton);
        mnu.add(Box.createGlue());

        exitSessionButton.setSize(ps1);
        exitProgramButton.setSize(ps1);
        returnToGameButton.setSize(ps1);

        mnu.setMaximumSize(display.getActualResolution());
        mnu.setMinimumSize(display.getActualResolution());
        mnu.setPreferredSize(display.getActualResolution());
        mnu.setSize(display.getActualResolution());

        JPanel p = new JPanel();
        p.setBackground(Color.BLACK);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(Box.createGlue());
        p.add(mnu);
        p.add(Box.createGlue());
        return p;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public ThemeVariables getTheme() {
        if (theme == null) {
            theme = new ThemeVariables((byte) new Random(
                    System.currentTimeMillis()).nextInt(2));
        }
        return theme;
    }

    public DataFileHandler getDataFileHandler() {
        if (dataFileHandler == null) {
            dataFileHandler = new DataFileHandler();
        }
        return dataFileHandler;
    }

    void stop() {
        running = false;
        Utilities.log.info("Ending game");
        frameManager.stop();
        taskQueue.stop();
        getSoundManager().stopAllSounds();
        display.restoreScreen();
        GamePreferences.clean();
        lazilyExit();
    }

    /**
     * Exits the VM from a daemon thread. The daemon thread waits 2 seconds then
     * calls System.exit(0). Since the VM should exit when only daemon threads
     * are running, this makes sure System.exit(0) is only called if necessary.
     * It's necessary if the Java Sound system is running.
     */
    private void lazilyExit() {
        Thread thread = new Thread() {
            public void run() {
                // first, wait for the VM exit on its own.
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                }
                // system is still running, so force an exit
                System.exit(0);
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    public void setTheme(byte theme) {
        getTheme().setLevel(theme);
    }

    public NRRunnableQueue getTaskQueue() {
        return taskQueue;
    }

    public void setStudent(Student tmpStudent) {
        this.player = tmpStudent;
        getDataFileHandler().setStudent(tmpStudent);
    }

    public Student getStudent() {
        return this.player;
    }

    public HazardManager getHazardManager() {
        return getChoiceScreen().getHazardManager();
    }

    public void start() {
        //        System.out.println("Start...");
        assert (initialized && !running);

        //        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
        //
        //            public void eventDispatched(AWTEvent event) {
        //                Utilities.log.info(event.paramString());
        //            }
        //
        //        }, AWTEvent.ACTION_EVENT_MASK);

        running = true;

        frameManager.start();
        new Thread(taskQueue, "NumberRace-RunningQueue").start();
        changeState(GameStates.TITLE);

        window.addWindowListener(windowListener);
        //        window.requestFocusInWindow();

    }

    public void mouseMove(int x, int y) {
        if (robot != null) {
            Point where2move = new Point(x, y);
            SwingUtilities.convertPointToScreen(where2move,
                                                getScree4State(getCurrentState()).getComponent());
            robot.mouseMove(where2move.x, where2move.y);
        }
    }

    private Robot initRobot() {
        if (robot == null) {
            try {
                robot = new Robot();
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
        return robot;
    }

    /*
    public int getPlayerCharacter() {
        return playerCharacter;
    }

    public int getOpponentCharacter() {
        return opponentCharacter;
    }
     *
     */

    //    public ImageManager getImageManager() {
    //        return imageManager;
    //    }

    public NumCompManager getNumCompManager() {
        return ncm;
    }

    private void initializeGlobalActions() {
        window.getRootPane()
              .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,
                                          InputEvent.SHIFT_DOWN_MASK,
                                          true),
                   "endGame");
        window.getRootPane()
              .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0, true), "pauseGame");
        window.getRootPane()
              .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true),
                   "showMenu");

        window.getRootPane().getActionMap().put("endGame", getEndAction());
        window.getRootPane()
              .getActionMap()
              .put("pauseGame", new PauseUnPauseGameAction());
        window.getRootPane().getActionMap().put("showMenu", getMenuAction());

    }

    private Action endAction;
    private Action menuAction;

    private Action getEndAction() {
        if (endAction == null) {
            endAction = new EndGameAction();
        }
        return endAction;
    }

    private Action getMenuAction() {
        if (menuAction == null) {
            menuAction = new ShowMenuAction();
        }
        return menuAction;
    }

    private final class EndGameAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            changeState(GameStates.END);
        }
    }

    private final class ShowMenuAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            changeState(GameStates.MENU);
        }
    }

    private final class PauseUnPauseGameAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            if (!transition) {
                if (currentState != GameStates.MENU && paused) {
                    unpause();
                } else {
                    pause();
                }
            }
        }

    }

    public int getPlayerPosition() {
        return getChoiceScreen().getPlayer(Constants.PLAYER1)
                                .getBoardPosition();
    }

    public int getOpponentPosition() {
        return getChoiceScreen().getPlayer(Constants.PLAYER2)
                                .getBoardPosition();
    }
}
