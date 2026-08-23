package org.unicog.numberrace.managers;

import java.awt.Point;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jmat.data.Matrix;
import org.unicog.numberrace.GameObject.GameStates;
import org.unicog.numberrace.NRRunnableQueue;
import org.unicog.numberrace.algorithms.AdapDimensions;
import org.unicog.numberrace.algorithms.GameTurn;
import org.unicog.numberrace.algorithms.Matrix3D;
import org.unicog.numberrace.algorithms.NotnDimLevel;
import org.unicog.numberrace.algorithms.NumCompAlgManager;
import org.unicog.numberrace.data.Student;
import org.unicog.numberrace.listener.GameListener;
import org.unicog.numberrace.listener.NumCompListener;
import org.unicog.numberrace.screens.ActionState;
import org.unicog.numberrace.sound.SoundListener;
import org.unicog.numberrace.sound.SoundManager;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.Utilities;


/**
 * This class handles all the game logic related to the Number Comparison task,
 * apart from controlling the algorithm classes (which is done by
 * NumCompAlgManager) It is instantiated by and interacts with ncl, as
 * well as managing its relevant subcomponents
 * 
 */
public class NumCompManager implements GameListener {

    private final Logger log = Logger.getLogger(NumCompManager.class.getPackage()
                                                                    .getName());

    //state attributes
    private boolean stimsDisplayed = false;
    private boolean turnBeginning = false; //true if beginning of turn (ie. have not displayed stims yet)
    private boolean gameBeginning = true; //true if beginning of a game
    private int numGamesWon = 0; //number of games won this session
    private int numGamesFinished = 0; //number of games played this session
    private int currentTurnNumber = 0;
    private int currentGameNumber = 0;
    private GameTurn nextTurn;
    private GameTurn currentTurn;

    //constants
    // following indicate the type of response the game is expecting
    public static final byte NO_RESPONSE_ACCEPTED = 0;
    public static final byte NUM_COMP_RESPONSE = 1;
    public static final byte PLAYER1_CLICK_RESPONSE = 2;
    public static final byte PLAYER2_CLICK_RESPONSE = 3;
    public static final byte RETURN_TO_CHOICE_RESPONSE = 4;

    //utility variables
    private Random randomNumber;

    //objects owned
    private NumCompAlgManager numCompAlgManager;

    //response variables
    public boolean responseMade = false;
    private Point mousePoint = new Point(0, 0);
    public boolean responseHandled = false;
    private long systemTimeStamp;

    //board response variables
    private int dotsLeftToClick = 0; //remaining number of dots to click to board
    private int dotsAlreadyClicked = 0; //number of dots already clicked to board
    private int offScreenDots = 0; //this is so dotClick responses work when near end of board
    //private GameObject go;
    //private final ncl ncl;
    private int lastWinner;
    private final NumCompListener ncl;
    final NRRunnableQueue taskQueue;
    private final SoundManager soundManager;

    public NumCompManager(NumCompListener ncl, NRRunnableQueue taskQueue,
            SoundManager soundManager) {
        //this.ncl = cs;
        //go = GameObject.getInstance();
        this.ncl = ncl;
        this.taskQueue = taskQueue;
        this.soundManager = soundManager;
    }

    public void load() {
        randomNumber = new Random();
        randomNumber.setSeed(System.currentTimeMillis()); //seed with system time in msec		
    }

    public void start() {
        turnBeginning = true;
    }

    public void gameBegins() {
        gameBeginning = false;
        resetGame();
    }

    public GameTurn turnBegins() {
        ncl.setActionState(ActionState.TURN_START);

        //Run events for beginning of turn
        currentTurnNumber++;
        currentTurn = nextTurn;
        nextTurn = null;
        ncl.setCurrentTurn(currentTurn);
        currentTurn.startTurn();
        setStimsForTurn();

        //        ncl.dotManagers[Constants.PLAYER1]
        //                .switchDrawingScreen("NOWHERE"); //$NON-NLS-1$
        //        ncl.dotManagers[Constants.PLAYER2]
        //                .switchDrawingScreen("NOWHERE"); //$NON-NLS-1$

        //        if (Debugger.IN_USE) {
        //            String debugLbl = "CURR_DESIR_DIFF: " + currentTurn.getCurrDesiredDiffForPrompt() + //$NON-NLS-1$
        //                    "   DIFF_LVLS: " + currentTurn.getDiffLvlsForPrompt() + //$NON-NLS-1$
        //                    "CONV_DIFF_LVLS: " + currentTurn.getConvDiffLvlsForPrompt(); //$NON-NLS-1$
        //            ncl.setLblDebugAlg(debugLbl);
        //            boardScreen.setLblDebugAlg(debugLbl);
        //        }
        showContainers(); //shows closed containers

        taskQueue.addTask(new Runnable() {

            public void run() {
                Utilities.log.info("runTurn");
                NumCompManager.this.runTurn();
            }

        }, 2000);

        if (log.isLoggable(Level.FINE)) {
            log.fine(currentTurn.toString());
        }

        return currentTurn;
    }

    private void setStimsForTurn() {
        //Sets the stimulus parameters for this turn consistent with its attributes		
        if (currentTurn.getControlDensity()) {
            ncl.getDots()[Constants.LEFT].setFixedDensity(true); //set fixedDensity and fixedItemSize parameters for the dot stims
            ncl.getDots()[Constants.LEFT].setFixedItemSize(false);
            ncl.getDots()[Constants.RIGHT].setFixedDensity(true); //set fixedDensity and fixedItemSize parameters for the dot stims
            ncl.getDots()[Constants.RIGHT].setFixedItemSize(false);
        } else {
            ncl.getDots()[Constants.LEFT].setFixedDensity(false); //set fixedDensity and fixedItemSize parameters for the dot stims
            ncl.getDots()[Constants.LEFT].setFixedItemSize(true);
            ncl.getDots()[Constants.RIGHT].setFixedDensity(false); //set fixedDensity and fixedItemSize parameters for the dot stims
            ncl.getDots()[Constants.RIGHT].setFixedItemSize(true);
        }

        //Set the stimuli according to mag and distance dimensions
        ncl.getDots()[Constants.LEFT].setNumber(currentTurn.getNumber(Constants.LEFT));
        ncl.getDots()[Constants.RIGHT].setNumber(currentTurn.getNumber(Constants.RIGHT));

        //Set the notation dimension
        NotnDimLevel notnLevel = numCompAlgManager.getNotnLevel(currentTurn.getCurrentNotnLevel());

        //analog magnitude stims (note: if addition or subtraction, this is more complicated, and set in getStims
        if (!((notnLevel.addition) || (notnLevel.subtraction))) {
            ncl.getDots()[Constants.LEFT].setDotsVisible(notnLevel.analogMagStims);
            ncl.getDots()[Constants.RIGHT].setDotsVisible(notnLevel.analogMagStims);
        }

        //        //verbal stims
        //        ncl.dotContainers[Constants.LEFT]
        //                .setVerbalStims(notnLevel.verbalStims);
        //        ncl.dotContainers[Constants.RIGHT]
        //                .setVerbalStims(notnLevel.verbalStims);

        //note: arabic stims get set in showStims(), so that they are not displayed immediately
        //range restriction and deadline already been taken into account

        //dot fading
        ncl.getDots()[Constants.LEFT].setDotsFade(notnLevel.dotsFade);
        ncl.getDots()[Constants.RIGHT].setDotsFade(notnLevel.dotsFade);
        ncl.getDots()[Constants.LEFT].setFadeTime(notnLevel.fadeTime);
        ncl.getDots()[Constants.RIGHT].setFadeTime(notnLevel.fadeTime);

        //pick a side at random for addition/subtraction
        if ((notnLevel.addition) || (notnLevel.subtraction)) {
            int additionSide = currentTurn.getAdditionSide();
            if (notnLevel.addition == true) {
                //set all the relevant parameters to display stims
                ncl.getDotContainer(additionSide).setAddition(true);
                //Set the stimuli according to mag and distance dimensions

                ncl.getDots()[additionSide].setSubNumber(currentTurn.getSubNumber(additionSide,
                                                                                  Constants.LEFT),
                                                         currentTurn.getSubNumber(additionSide,
                                                                                  Constants.RIGHT));

                ncl.getDots()[additionSide].setDotsVisible(notnLevel.analogMagStims);

            }
            if (notnLevel.subtraction == true) {
                int subtractionSide = Utilities.oppositeSide(additionSide);
                //set all the relevant graphical parameters to display stims
                ncl.getDotContainer(subtractionSide).setSubtraction(true);
                //re-set the number of the main dotArray (subtracted dots will disappear later)
                ncl.getDots()[subtractionSide].setNumber(currentTurn.getSubNumber(subtractionSide,
                                                                                  Constants.LEFT));
                ncl.getDots()[subtractionSide].setDotsVisible(notnLevel.analogMagStims);
            }
        }

        //finally set DotContainer parameters
        ncl.getDotContainer(Constants.LEFT).setArabicDigit(currentTurn);
        ncl.getDotContainer(Constants.RIGHT).setArabicDigit(currentTurn);

    }

    private void setHazardsForTurn(GameTurn turn) {
        //Sets hazards in place on the board for a turn (called separately so that they
        //can be seen by player before leaving boardScreen)
        NotnDimLevel notnLevel = numCompAlgManager.getNotnLevel(turn.getCurrentNotnLevel());
        //Place hazards if they are present, and if turn is greater than one
        if (turn.getTurnNumber() > 1) {
            ncl.setHazardLevel(notnLevel.hazards);

            ncl.setHazards(notnLevel.rangeCeilling, this.getGameBeginning());
        } else
            ncl.setHazardLevel(false);
    }

    public void runTurn() {
        //stamp the current system time
        //        go.mouseMove(i(GraphicsVariables.BETWEEN_DOT_CONTAINERS.x),
        //                     i(GraphicsVariables.BETWEEN_DOT_CONTAINERS.y));
        systemTimeStamp = System.currentTimeMillis();
        stimsDisplayed = true;

        //schedule right container to open in 1200 msec
        taskQueue.addTask(new Runnable() {
            public void run() {
                Utilities.log.info("showRightStims");
                NumCompManager.this.showRightStims();
            }
        }, 1200);

        taskQueue.addTask(new Runnable() {
            public void run() {
                NumCompManager.this.setSneakInAction(Constants.PLAYER2);
            }
        }, 1200);

        showLeftStims();
        //        if (Debugger.IN_USE) {
        //            if (game.debugger.simulationMode) {
        //                game.delay(2000, "game.simulator.doChoiceResponse()"); //$NON-NLS-1$
        //            }
        //        }
    }

    public void initialiseTurn(GameTurn turnToSet) {
        //Sets up a turn in advance
        //Called at beginning of game for current turn, and then at end of each turn
        //for nextTurn (this is all so that hazards are placed on the board for the next
        //turn in advance so player can see where they are
        numCompAlgManager.setStimAttributes(turnToSet);
    }

    public void showContainers() {

        ncl.getDots()[Constants.LEFT].setDotsVisible(true);
        ncl.getDots()[Constants.RIGHT].setDotsVisible(true);
        if (!currentTurn.isDeadlineTrial()) {
            soundManager.play("chooseSide"); //$NON-NLS-1$
        } else {
            soundManager.play("chooseQuickly"); //$NON-NLS-1$
        }

    }

    public void showLeftStims() {
        if (numCompAlgManager.getNotnLevel(currentTurn.getCurrentNotnLevel()).analogMagStims == true) {
            ncl.openContainer(Constants.LEFT);
        }
        if (numCompAlgManager.getNotnLevel(currentTurn.getCurrentNotnLevel()).arabicStims == true) {
            ncl.getDotContainer(Constants.LEFT).setArabicStims(true);
        }
        //        ncl.getDotContainer(Constants.LEFT).setActive(true);
        //        ncl.getDotContainer(Constants.LEFT).mainDotArray.setActive(true);
        if (numCompAlgManager.getNotnLevel(currentTurn.getCurrentNotnLevel()).verbalStims == true) {
            playVerbalStims(Constants.LEFT, Constants.PLAYER1, getCurrentTurn());
        }
    }

    public void showRightStims() {
        if (numCompAlgManager.getNotnLevel(currentTurn.getCurrentNotnLevel()).analogMagStims == true) {
            ncl.openContainer(Constants.RIGHT);
        }
        if (numCompAlgManager.getNotnLevel(currentTurn.getCurrentNotnLevel()).arabicStims == true) {
            ncl.getDotContainer(Constants.RIGHT).setArabicStims(true);
        }
        //        ncl.getDotContainer(Constants.RIGHT].setActive(true);
        //        ncl.getDotContainer(Constants.RIGHT].mainDotArray
        //                .setActive(true);
        if (numCompAlgManager.getNotnLevel(currentTurn.getCurrentNotnLevel()).verbalStims == true) {
            playVerbalStims(Constants.RIGHT,
                            Constants.PLAYER1,
                            getCurrentTurn());
        }

        //don't accept responses 'till now!
        //        ncl.responseMode = NUM_COMP_RESPONSE;
        if (!currentTurn.isDeadlineTrial()) {
            ncl.setActionState(ActionState.CHOOSE);
        }
    }

    public void setSneakInAction(int player) {
        //If there is a deadline, this method sets in action the computers player icon
        //who will move to steal the dots
        //System.out.println("deadlineTrial: " + deadlineTrial);
        //System.out.println("currentDeadline: " + currentDeadline);
        if (currentTurn.isDeadlineTrial()) {
            ncl.startSneaking((long) currentTurn.getCurrentDeadline());
        }
    }

    public boolean resetTurn() {
        //        ncl.resetTurn();

        responseMade = false;
        responseHandled = false;
        stimsDisplayed = false;
        offScreenDots = 0;
        dotsLeftToClick = 0;
        dotsAlreadyClicked = 0;
        //        ncl.setRendered(false);
        //		ncl.repaint();
        return true;
    }

    public boolean resetGame() {
        //resets for a new game
        resetTurn();
        currentTurnNumber = 0;
        currentGameNumber++;
        currentTurn = nextTurn = new GameTurn(currentTurnNumber + 1);
        initialiseTurn(nextTurn);

        ncl.setBoardLength(numCompAlgManager.getNotnLevel(currentTurn.getCurrentNotnLevel()).boardLength);

        return true;
    }

    public void responseScript(final boolean player1response,
            int playerMostDots, final int player1sideSelected, int sideMostDots) {
        //this adds a series of events to the event queue, to be executed one after the other...
        int playerLeastDots = Utilities.oppositeSide(playerMostDots);
        int sideLeastDots = Utilities.oppositeSide(sideMostDots);
        final int player2sideSelected = Utilities.oppositeSide(player1sideSelected);

        final int responderID;
        final int responderSideSelected;
        final int waiterID;
        final int waiterSideSelected;

        if (player1response) {
            responderID = Constants.PLAYER1;
            responderSideSelected = player1sideSelected;
            waiterID = Constants.PLAYER2;
            waiterSideSelected = player2sideSelected;
        } else {
            responderID = Constants.PLAYER2;
            responderSideSelected = player2sideSelected;
            waiterID = Constants.PLAYER1;
            waiterSideSelected = player1sideSelected;
        }
        final NumCompManager ncm = NumCompManager.this;

        //        taskQueue.addTaskInQueue(new Runnable() {
        //            public void run() {
        //                ncm.lineUpDotsOnCarpet(responderID, responderSideSelected);
        //            }
        //        });

        taskQueue.addTaskInQueue(new Runnable() {
            public void run() {
                //                ncl.showNumberOnCarpet(responderID);
                ncm.playerGetsArray(waiterID,
                                    waiterSideSelected,
                                    player1response);

            }
        });

        //        taskQueue.addTaskInQueue(new Runnable() {
        //            public void run() {
        //                ncm.lineUpDotsOnCarpet(waiterID, waiterSideSelected);
        //            }
        //        });

        taskQueue.addTaskInQueue(new Runnable() {
            public void run() {
                //                ncl.showNumberOnCarpet(waiterID);
                //react to selection
                ncm.reactToPlayerGetsArray(Constants.PLAYER1,
                                           player1sideSelected);

            }
        });

        taskQueue.addTaskInQueue(new Runnable() {
            public void run() {
                //react to result
                ncm.reactToPlayerGetsArray(Constants.PLAYER2,
                                           player2sideSelected);

            }
        });

        taskQueue.addTaskInQueue(new Runnable() {

            public void run() {
                //switch to board screen
                ncl.opponentTalks("enemy1_wellSee");
                ncl.setActionState(ActionState.WAIT4_PLAYER_MOVE);

            }
        });
        taskQueue.addTaskInQueue(new Runnable() {

            public void run() {
                ncl.setActionState(ActionState.WAIT4_OPPONENT_MOVE);
            }
        });

        //        taskQueue.addTaskInQueue(new Runnable() {
        //            public void run() {
        //                //collect responses of player with most dots
        //                ncm.collectClickResponse(Constants.PLAYER1,
        //                                         player1sideSelected,
        //                                         player2sideSelected);
        //            }
        //        });

        //        taskQueue.addTaskInQueue(new Runnable() {
        //            public void run() {
        //
        //                //collect responses of player with least dots
        //                ncm.collectClickResponse(Constants.PLAYER2,
        //                                         player2sideSelected,
        //                                         player1sideSelected);
        //            }
        //        });

        taskQueue.addTaskInQueue(new Runnable() {
            public void run() {
                //react to move
                ncm.reactToBoardMoves();
            }
        });

        taskQueue.addTaskInQueue(new Runnable() {
            public void run() {
                //reset turn
                ncm.runTurnEndEvents();
            }
        });

        ncm.playerGetsArray(responderID, responderSideSelected, player1response);

    }

    protected SoundListener nextTaskAfterSound = new SoundListener() {

        public void run() {
            taskQueue.nextTaskInQueue();
        }

    };

    public void playerGetsArray(final int player, final int sideSelected,
            boolean player1response) {
        //assign a dot manager
        //        ncl.setPlayerDost(sideSelected);

        //        ncl.dotManagers[player].assignDotManager(
        //                ncl.getDotContainer(sideSelected], currentTurn);

        //        ncl.getDotContainer(sideSelected].select();
        ncl.openContainer(sideSelected, true);

        //SoundManager soundManager = soundManager;

        if (player == Constants.PLAYER1) {
            if (player1response)
                soundManager.play("friend1_youChose"); //$NON-NLS-1$
            else
                soundManager.play("friend1_andYouGet"); //$NON-NLS-1$
        } else if (player == Constants.PLAYER2) {
            if (player1response)
                ncl.opponentTalks("enemy1_andITake"); //$NON-NLS-1$
            else {
                ncl.opponentTalks("enemy1_iTake"); //$NON-NLS-1$
            }
        }

        final NRRunnableQueue tQueue = taskQueue;

        tQueue.addTask(new Runnable() {

            public void run() {
                playVerbalStims(sideSelected,
                                player,
                                getCurrentTurn(),
                                nextTaskAfterSound,
                                true);
                ncl.getDotContainer(sideSelected).setArabicStims(true);
            }

        },
                       1500);

        //        if (ncl.getDotContainer(sideSelected).isAddition()) {
        //            tQueue.addTask(new Runnable() {
        //
        //                public void run() {
        //                    ncl.getDotContainer(sideSelected).showEqual();
        //                }
        //
        //            }, 3000);
        //
        //            tQueue.addTask(new Runnable() {
        //
        //                public void run() {
        //                    ncl.getDotContainer(sideSelected).showSum(
        //                            getCurrentTurn());
        //                    tQueue.nextTaskInQueue();
        //                    //                    game.delay(5000, "game.nextTaskInQueue(\"ncm.playerGetsArray\")"); //$NON-NLS-1$
        //                }
        //
        //            }, 5000);
        //        } else 
        //        if (ncl.getDotContainer(sideSelected).isSubtraction()
        //                || ncl.getDotContainer(sideSelected).isAddition()) {
        //
        //            tQueue.addTask(new Runnable() {
        //
        //                public void run() {
        //                    ncl.getDotContainer(sideSelected).showEqual();
        //                }
        //
        //            }, 3000);
        //
        //            tQueue.addTask(new Runnable() {
        //
        //                public void run() {
        //                    //                    game
        //                    //                    .delay(4000,
        //                    //                            "ncl.dotManagers[" + player + "].start()"); //$NON-NLS-1$ //$NON-NLS-2$
        //
        ////                    ncl.getDotContainer(sideSelected).showSum(
        ////                            getCurrentTurn());
        //                    tQueue.nextTaskInQueue();
        //                }
        //
        //            }, 5000);
        //
        //        } else {
        //            tQueue.addTask(new Runnable() {
        //
        //                public void run() {
        //                    tQueue.nextTaskInQueue();
        //                }
        //
        //            }, 1700); // (1700, "game.nextTaskInQueue(\"ncm.playerGetsArray\")"); //$NON-NLS-1$
        //        }
    }

    public void playVerbalStims(final int side, final int characterID,
            final GameTurn currentTurn) {
        playVerbalStims(side, characterID, currentTurn, null, false);
    }

    public void playVerbalStims(final int side, final int characterID,
            final GameTurn currentTurn, final SoundListener l,
            final boolean lineUp) {
        final int[] numbers = currentTurn.getNumbers();
        final int[][] subNumbers = currentTurn.getSubNumbers();
        final SoundManager sndM = soundManager;
        final NRRunnableQueue taskQ = taskQueue;

        final boolean addition = ncl.getDotContainer(side).isAddition();
        if (addition || ncl.getDotContainer(side).isSubtraction()) {
            //Play "trois plus quatre �gale sept"

            final int num1 = subNumbers[side][Constants.LEFT];
            final int num2 = subNumbers[side][Constants.RIGHT];

            final DelayedSoundListener resultSL = new DelayedSoundListener(
                    taskQ, 150, new Runnable() {
                        public void run() {
                            // use numeric sound files 1.wav ... 40.wav

                            //sndM.play(Utilities.charac4id(characterID)
                            //                  + Utilities.getVerbalForArabic(numbers[side]),
                            //          l);
                            sndM.play(Utilities.charac4id(characterID)
                                    + numbers[side], l);
                            if (lineUp) {
                                lineUpDotsOnCarpet(characterID, side);
                            }
                            ncl.getDotContainer(side).showSum(currentTurn);
                        }
                    });

            final DelayedSoundListener equalSL = new DelayedSoundListener(
                    taskQ, 150, new Runnable() {
                        public void run() {
                            sndM.play(Utilities.charac4id(characterID)
                                    + "equals", resultSL);
                            ncl.getDotContainer(side).showEqual();
                        }
                    });

            final SoundListener num2SL = new DelayedSoundListener(taskQ, 150,
                    new Runnable() {
                        public void run() {
                            // use numeric sound files

                            //sndM.play(Utilities.charac4id(characterID)
                            //                  + Utilities.getVerbalForArabic(num2),
                            //          equalSL);
                            sndM.play(Utilities.charac4id(characterID) + num2,
                                      equalSL);
                            if (!addition) { // substraction
                                ncl.grabAndSubtractDots(characterID,
                                                        side,
                                                        currentTurn);
                            }
                        }
                    });

            DelayedSoundListener signSL = new DelayedSoundListener(taskQ, 150,
                    new Runnable() {
                        public void run() {
                            if (addition) {
                                sndM.play(Utilities.charac4id(characterID)
                                        + "plus", num2SL);
                            } else {
                                sndM.play(Utilities.charac4id(characterID)
                                        + "minus", num2SL);
                            }

                        }
                    });

            // start sound sequence
            // use numeric sound files
            //sndM.play(Utilities.charac4id(characterID)
            //                  + Utilities.getVerbalForArabic(num1),
            //          signSL);
            sndM.play(Utilities.charac4id(characterID) + num1, signSL);

        } else {
            // use numeric sound files
            //sndM.play(Utilities.charac4id(characterID)
            //                  + Utilities.getVerbalForArabic(numbers[side]),
            //          l);
            sndM.play(Utilities.charac4id(characterID) + numbers[side], l);

            if (lineUp) {
                lineUpDotsOnCarpet(characterID, side);
            }
            //            if(l != null) {
            //                l.run();
            //            }
        }
    }

    public void lineUpDotsOnCarpet(int player, int sideSelected) {
        ncl.grabAndLineUpDotsOnCarpet(player, sideSelected, getCurrentTurn());
    }

    public void reactToPlayerGetsArray(int player, int sideSelected) {
        final SoundManager sm = soundManager;
        final NRRunnableQueue tQueue = taskQueue;
        int[] numbers = currentTurn.getNumbers();
        if (player == Constants.PLAYER1) {
            if (numbers[sideSelected] < numbers[Utilities.oppositeSide((byte) sideSelected)]) {
                sm.play("friend1_youHaveLess"); //637 msec //$NON-NLS-1$ [NUMBERRACE-12]
                //ncl.playerIcons[player].speak(637);
                tQueue.addTask(new Runnable() {

                    public void run() {
                        sm.play("wrong1");
                    }

                }, 700);

            } else if (numbers[sideSelected] > numbers[Utilities.oppositeSide((byte) sideSelected)]) {

                sm.play("friend1_youHaveMore"); //950 msec //$NON-NLS-1$ [NUMBERRACE-12]

                tQueue.addTask(new Runnable() {
                    public void run() {
                        sm.play("cheer");
                    }
                }, 1000);
            }

        } else if (player == Constants.PLAYER2) {
            if (numbers[sideSelected] < numbers[Utilities.oppositeSide((byte) sideSelected)]) {
                ncl.opponentTalks("enemy1_iHaveTheLeast"); //1644 msec				 //$NON-NLS-1$
                //TODO:                ncl.playerIcons[player].speak(1644);
            } else if (numbers[sideSelected] > numbers[Utilities.oppositeSide((byte) sideSelected)]) {
                ncl.opponentTalks("enemy1_iHaveTheMost"); //2068 msec	 //$NON-NLS-1$
                //ncl.playerIcons[player].speak(2068);
            }
        }

        tQueue.addTask(new Runnable() {

            public void run() {
                tQueue.nextTaskInQueue();
            }

        }, 3000);

    }

    public void collectClickResponse(int player, int sideSelected,
            int oppositeSide) {
        //SoundManager soundManager = soundManager;
        if (player == Constants.PLAYER1) {
            ncl.setActionState(ActionState.WAIT4_PLAYER_MOVE);
        }
        if (player == Constants.PLAYER2) {
            ncl.setActionState(ActionState.WAIT4_OPPONENT_MOVE);
        }
    }

    public void reactToBoardMoves() {
        //play the appropriate sound file, according to who moved the furtherest
        //Calculate the resulting inter player distance
        currentTurn.setPostTurnInterPlayerDistance(ncl.getPlayer(Constants.PLAYER1)
                                                      .getBoardPosition()
                - ncl.getPlayer(Constants.PLAYER2).getBoardPosition());
        int changeInDistance = currentTurn.getPostTurnInterPlayerDistance()
                - currentTurn.getPreTurnInterPlayerDistance();
        //final SoundManager soundManager = soundManager;
        final NRRunnableQueue tQueue = taskQueue;
        if (currentTurn.getPreTurnInterPlayerDistance() < 0) {
            //if player two was ahead
            if (currentTurn.getPostTurnInterPlayerDistance() > 0) {
                //player one has overtaken
                ncl.opponentTalks("enemy1_youveOvertakenMe"); //2407 msec		 //$NON-NLS-1$
                //TODO:                boardScreen.players[Constants.PLAYER2].speak(2400);
                tQueue.addTask(new Runnable() {

                    public void run() {
                        soundManager.play("right"); //$NON-NLS-1$
                    }

                }, 3000);
            } else {
                if (changeInDistance < 0) {
                    //player two gets further ahead 
                    ncl.opponentTalks("enemy1_tryToCatchMe"); //2139 msec	 //$NON-NLS-1$
                    //boardScreen.players[ncl.PLAYER2].speak(2140);
                    tQueue.addTask(new Runnable() {

                        public void run() {
                            soundManager.play("wrong2"); //$NON-NLS-1$
                        }

                    }, 3000);
                } else if (changeInDistance > 0) {
                    //player one catches up
                    ncl.opponentTalks("enemy1_youreCatchingUp"); //2986 msec //$NON-NLS-1$
                    //TODO:                    boardScreen.players[Constants.PLAYER2].speak(2980);
                    tQueue.addTask(new Runnable() {

                        public void run() {
                            soundManager.play("right"); //$NON-NLS-1$
                        }

                    }, 3000);
                } else {
                    //the distance stays the same 	
                    ncl.opponentTalks("enemy1_imStillAhead"); //2253 msec //$NON-NLS-1$
                    //boardScreen.players[ncl.PLAYER2].speak(2250);
                    tQueue.addTask(new Runnable() {

                        public void run() {
                            soundManager.play("wrong2"); //$NON-NLS-1$
                        }

                    }, 3000);
                }
            }
        } else if (currentTurn.getPreTurnInterPlayerDistance() > 0) {
            //if player one was ahead
            if (currentTurn.getPostTurnInterPlayerDistance() < 0) {
                //player two has overtaken
                ncl.opponentTalks("enemy1_iveOvertakenYou"); //3365 msec //$NON-NLS-1$
                //boardScreen.players[ncl.PLAYER2].speak(3360);
                tQueue.addTask(new Runnable() {

                    public void run() {
                        soundManager.play("wrong2"); //$NON-NLS-1$
                    }

                }, 3000);

            } else { //player two is still behind
                if (changeInDistance < 0) {
                    //player two catches up
                    ncl.opponentTalks("enemy1_imCatchingUp"); //2883 msec //$NON-NLS-1$
                    //boardScreen.players[ncl.PLAYER2].speak(2880);
                    tQueue.addTask(new Runnable() {

                        public void run() {
                            soundManager.play("wrong2"); //$NON-NLS-1$
                        }

                    }, 3000);
                } else if (changeInDistance > 0) {
                    //player one gets further ahead
                    if (currentTurn.getPostTurnInterPlayerDistance() >= 10) {
                        ncl.opponentTalks("enemy1_imMilesBehind"); //3306 msec //$NON-NLS-1$
                        //TODO:                        boardScreen.players[Constants.PLAYER2].speak(3300);
                        tQueue.addTask(new Runnable() {

                            public void run() {
                                soundManager.play("right"); //$NON-NLS-1$
                            }

                        }, 3000);
                    } else if (currentTurn.getPostTurnInterPlayerDistance() >= 5) {
                        ncl.opponentTalks("enemy1_gettingBehind"); //2893 msec //$NON-NLS-1$
                        //TODO:                        boardScreen.players[Constants.PLAYER2].speak(2900);
                        tQueue.addTask(new Runnable() {

                            public void run() {
                                soundManager.play("right"); //$NON-NLS-1$
                            }

                        }, 3000);
                    } else {
                        ncl.opponentTalks("enemy1_justBehind"); //1593 msec //$NON-NLS-1$
                        //TODO:                        boardScreen.players[Constants.PLAYER2].speak(1600);
                        tQueue.addTask(new Runnable() {

                            public void run() {
                                soundManager.play("right"); //$NON-NLS-1$
                            }

                        }, 3000);
                    }

                } else {
                    //the distance stays the same 	
                    ncl.opponentTalks("enemy1_youreStillAhead"); //3327 msec	 //$NON-NLS-1$
                    //boardScreen.players[ncl.PLAYER2].speak(3320);
                    tQueue.addTask(new Runnable() {

                        public void run() {
                            soundManager.play("right"); //$NON-NLS-1$
                        }

                    }, 3000);
                }
            }
        } else {
            //the first turn
            if (currentTurn.getPostTurnInterPlayerDistance() < 0) {
                //player two is ahead
                ncl.opponentTalks("enemy1_imAhead"); //3365 msec //$NON-NLS-1$
                //boardScreen.players[ncl.PLAYER2].speak(3360);
                tQueue.addTask(new Runnable() {

                    public void run() {
                        soundManager.play("wrong2"); //$NON-NLS-1$
                    }

                }, 3000);
            } else {
                //player one is ahead
                ncl.opponentTalks("enemy1_youWentTheFurtherest"); //$NON-NLS-1$
                //TODO:                boardScreen.players[Constants.PLAYER2].speak(3360);
                tQueue.addTask(new Runnable() {

                    public void run() {
                        soundManager.play("right"); //$NON-NLS-1$
                    }

                }, 3000);
            }
        }

        tQueue.addTask(new Runnable() {

            public void run() {
                tQueue.nextTaskInQueue();
            }

        }, 6000);

    }

    public void runTurnEndEvents() {
        //initialise the next turn, because then we can place hazards
        nextTurn = new GameTurn(currentTurnNumber + 1);
        ncl.clearCarpet();
        initialiseTurn(nextTurn);
        setHazardsForTurn(nextTurn);

        //        taskQueue.addTask(new Runnable() {
        //
        //            public void run() {
        //                soundManager.play("toContinue"); //$NON-NLS-1$
        //            }
        //
        //        }, 5000);

        //        if (Debugger.IN_USE) {
        //            if ((game.currentScreen == boardScreen)
        //                    && (game.debugger.simulationMode))
        //                game.delay(500, "game.simulator.selectTopRightButton()"); //$NON-NLS-1$
        //        }
        resetTurn();
        ncl.setActionState(ActionState.WAIT4NEXTTURN);
    }

    public void playerWins(int playerID) {
        this.lastWinner = playerID;
        //Method called by any playing piece that reaches the end of the track,
        //Sets in action end of game sequence 
        taskQueue.reset();
        //clear all scheduled tasks 
        ncl.getStudent().augmentGamesPlayed();

        final SoundListener l = new SoundListener() {

            public void run() {
                ncl.changeState(GameStates.GAMEOVER);
            }

        };

        if (playerID == Constants.PLAYER1) {
            soundManager.play("cheer", new SoundListener() {

                public void run() {
                    soundManager.play("right", l);
                }

            }); //$NON-NLS-1$ 
            //            game.delay(1800, "game.soundManager.play(\"right\")"); //$NON-NLS-1$
            numGamesWon++;
            ncl.getStudent().augmentGamesWon();
        } else {
            assert playerID == Constants.PLAYER2;
            soundManager.play("wrong2", l); //$NON-NLS-1$
        }

        numGamesFinished++;
        //        setActive(false);
        gameBeginning = true;

        //write the data about who won to a file... (separate from data file?)

        //        game.delay(1000,
        //                "game.changeCurrentScreenNoUnload(game.gameOverScreen)"); //$NON-NLS-1$ 
        //        go.changeState(GameStates.GAMEOVER);
    }

    public int getlastWinner() {
        return lastWinner;
    }

    public void endGame() { //Called if player selects exitSession on the game menu //Sets in action end of game sequence
        taskQueue.reset();
        //        game.taskManager.clearTaskList(); //clear all scheduled tasks
        //        game.taskManager.clearTaskQueue();
        //        setActive(false);
        gameBeginning = true;
        //        game.delay(1000, "game.changeCurrentScreen(game.regScreen)");
        ncl.changeState(GameStates.REGISTRATION);
        //$NON-NLS-1$ 
    }

    public int getNumGamesWon() {
        return numGamesWon;
    }

    public int getNumGamesPlayed() {
        return numGamesFinished;
    }

    public boolean getGameBeginning() {
        return gameBeginning;
    }

    public GameTurn getCurrentTurn() {
        return currentTurn;
    }

    //wrapper functions for alg manager
    public void newStudent(Student aStudent) {
        //instantiate a new NumCompAlgManager
        numCompAlgManager = new NumCompAlgManager(aStudent.getStartLevel());
    }

    public void exitStudent() {
        nextTurn = null;
        numCompAlgManager = null;
    }

    public String getNotnAttrCommaDelim(int currentNotnLevel) {
        return numCompAlgManager.getNotnAttrCommaDelim(currentNotnLevel);
    }

    public void setModelData(Matrix modelData, Matrix3D edMatrix) {
        numCompAlgManager.setModelData(modelData, edMatrix);
    }

    /**
     * Invoked when player was not fast to beat opponent in choice
     */
    public void successfullSneak() {
        //Note: if the child misses the deadline, their response is always coded as false
        responseHandled = true;
        ncl.opponentTalks("enemy1_iTookTheMost"); //$NON-NLS-1$

        Matrix responsePoint = new Matrix(1, AdapDimensions.NUM_ADAP_DIMS + 1);
        int[] numbers = currentTurn.getNumbers();

        //call the noResponse script
        if (numbers[Constants.LEFT] < numbers[Constants.RIGHT]) {
            //computer takes right array
            currentTurn.setResponseSide(Constants.LEFT);
            currentTurn.calculateActualWinner(); //do this anyway to store the variables
            currentTurn.setResponseCorrect(Constants.FALSE);
            currentTurn.setActualWinner(Constants.PLAYER2);
            ncl.getDataFileHandler().writeStudentFileDataLine(currentTurn);
            responsePoint.setSubMatrix(0, 0, currentTurn.getCurrentDifficulty());
            responsePoint.set(0, AdapDimensions.NUM_ADAP_DIMS, Constants.FALSE);
            boolean successfullyAddedTrial = numCompAlgManager.addTrial(responsePoint);

            taskQueue.addTask(new Runnable() {

                public void run() {
                    responseScript(false,
                                   Constants.PLAYER2,
                                   Constants.LEFT,
                                   Constants.RIGHT);
                }

            },
                              3200);

        } else if (numbers[Constants.LEFT] > numbers[Constants.RIGHT]) {
            currentTurn.setResponseSide(Constants.RIGHT);
            currentTurn.calculateActualWinner(); //do this anyway to store the variables
            currentTurn.setResponseCorrect(Constants.FALSE);
            currentTurn.setActualWinner(Constants.PLAYER2);
            ncl.getDataFileHandler().writeStudentFileDataLine(currentTurn);
            responsePoint.setSubMatrix(0, 0, currentTurn.getCurrentDifficulty());
            responsePoint.set(0, AdapDimensions.NUM_ADAP_DIMS, Constants.FALSE);
            boolean successfullyAddedTrial = numCompAlgManager.addTrial(responsePoint);

            taskQueue.addTask(new Runnable() {

                public void run() {
                    responseScript(false,
                                   Constants.PLAYER2,
                                   Constants.RIGHT,
                                   Constants.LEFT);
                }

            },
                              3200);
        }
    }

    /**
     * Invoked when player made his choice
     * 
     * @param leftrigth
     *            id of chosen side
     */
    public void imFast(int leftrigth) {
        long RT = (System.currentTimeMillis() - systemTimeStamp); //((InputEvent)e).getWhen();
        responseHandled = true;
        currentTurn.setRT(RT);
        int numbers[] = currentTurn.getNumbers();
        currentTurn.setResponseSide(leftrigth);

        if (leftrigth == Constants.LEFT) {
            currentTurn.setResponseSide(Constants.LEFT);
            if (numbers[Constants.LEFT] < numbers[Constants.RIGHT]) {
                currentTurn.setResponseCorrect(Constants.FALSE);
                //Call the response script
                responseScript(true,
                               Constants.PLAYER2,
                               Constants.LEFT,
                               Constants.RIGHT);
            } else if (numbers[Constants.LEFT] > numbers[Constants.RIGHT]) {
                currentTurn.setResponseCorrect(Constants.TRUE);
                //Call the response script
                responseScript(true,
                               Constants.PLAYER1,
                               Constants.LEFT,
                               Constants.LEFT);
            }
        } else if (leftrigth == Constants.RIGHT) {
            currentTurn.setResponseSide(Constants.RIGHT);
            if (numbers[Constants.LEFT] > numbers[Constants.RIGHT]) {
                currentTurn.setResponseCorrect(Constants.FALSE);
                //Call the response script
                responseScript(true,
                               Constants.PLAYER2,
                               Constants.RIGHT,
                               Constants.LEFT);
            } else if (numbers[Constants.LEFT] < numbers[Constants.RIGHT]) {
                currentTurn.setResponseCorrect(Constants.TRUE);
                //Call the response script
                responseScript(true,
                               Constants.PLAYER1,
                               Constants.RIGHT,
                               Constants.RIGHT);
            }
        }

        //Calculate the net relative gain, and the hypothetical net relative gain
        currentTurn.calculateActualWinner();
        ncl.getDataFileHandler().writeStudentFileDataLine(currentTurn);
        Matrix responsePoint = new Matrix(1, AdapDimensions.NUM_ADAP_DIMS + 1);
        responsePoint.setSubMatrix(0, 0, currentTurn.getCurrentDifficulty());
        responsePoint.set(0,
                          AdapDimensions.NUM_ADAP_DIMS,
                          currentTurn.getFinalCorrect());
        Utilities.log.fine("NC: Adding trial to algorithm...");
        boolean successfullyAddedTrial = numCompAlgManager.addTrial(responsePoint);
        Utilities.log.fine("NC: addTrial returned: " + successfullyAddedTrial);
    }

}