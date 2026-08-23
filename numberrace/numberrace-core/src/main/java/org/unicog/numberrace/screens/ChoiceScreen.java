package org.unicog.numberrace.screens;

import static org.unicog.numberrace.screens.ActionState.SNEAK;
import static org.unicog.numberrace.screens.ScaleUtils.i;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import javax.swing.SwingUtilities;

import org.unicog.numberrace.NRRunnableQueue;
import org.unicog.numberrace.algorithms.AdapDimensions;
import org.unicog.numberrace.algorithms.GameTurn;
import org.unicog.numberrace.animations.DotCarpet;
import org.unicog.numberrace.debug.DebugFrame;
import org.unicog.numberrace.listener.GameListener;
import org.unicog.numberrace.listener.HazardListener;
import org.unicog.numberrace.listener.SneakListener;
import org.unicog.numberrace.managers.DelayedSoundListener;
import org.unicog.numberrace.managers.HazardManager;
import org.unicog.numberrace.managers.NRMediaManager;
import org.unicog.numberrace.observer.LineUpObserver;
import org.unicog.numberrace.observer.SneakObserver;
import org.unicog.numberrace.others.DotArray;
import org.unicog.numberrace.sound.SoundListener;
import org.unicog.numberrace.sound.SoundManager;
import org.unicog.numberrace.sprites.CommandOnPressedSprite;
import org.unicog.numberrace.sprites.DotContainerSprite;
import org.unicog.numberrace.sprites.HazardSprite;
import org.unicog.numberrace.sprites.ImageButtonSprite;
import org.unicog.numberrace.sprites.TimerSprite;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.ScrollSafeLinePath;
import org.unicog.numberrace.util.Utilities;
import org.unicog.numberrace.vars.GraphicsVariables;
import org.unicog.numberrace.vars.ThemeVariables;

import com.samskivert.swing.Controller;
import com.samskivert.swing.ControllerProvider;
import com.samskivert.swing.event.CommandEvent;
import com.threerings.media.FrameManager;
import com.threerings.media.MediaPanel;
import com.threerings.media.VirtualMediaPanel;
import com.threerings.media.animation.RainAnimation;
import com.threerings.media.image.BufferedMirage;
import com.threerings.media.image.Mirage;
import com.threerings.media.sprite.ImageSprite;
import com.threerings.media.sprite.PathObserver;
import com.threerings.media.sprite.Sprite;
import com.threerings.media.sprite.action.ActionSprite;
import com.threerings.media.sprite.action.ArmingSprite;
import com.threerings.media.util.DelayPath;
import com.threerings.media.util.MultiFrameImageImpl;
import com.threerings.media.util.Path;

public class ChoiceScreen extends VirtualMediaPanel implements Screen,
        ControllerProvider, SneakListener, HazardListener,
        CountAnimationVariableContainer {

    static final long BMOVE_MS = 600;
    private final NRRunnableQueue taskQueue;
    private final SoundManager soundManager;
    private int opponentCharacter;
    private int playerCharacter;
    private final GameListener gameListener;

    public void sneakCancelled(Sprite sprite, Path path) {

        sprite.removeSpriteObserver(this.sneakObserver);
        removeSprite(sprite);
        Utilities.log.info("Player WAS FASTER !");
    }

    public void sneakCompleted(Sprite sprite, Path path, long when) {

        if (currentState != ActionState.CHOICE_MADE && nextState == null) {
            int[] numbers = currentTurn.getNumbers();
            choice = (numbers[Constants.LEFT] < numbers[Constants.RIGHT]) ? Constants.RIGHT
                    : Constants.LEFT;
            setActionState(ActionState.CHOICE_MADE);
            sprite.removeSpriteObserver(this.sneakObserver);
            Utilities.log.info("Opponent has stollen the treasure !");
            this.taskQueue.postRunnable(new Runnable() {

                public void run() {
                    gameListener.successfullSneak();
                }

            });
            setOpponentChoice(choice);
            removeSprite(sprite);
        }
    }

    public void play(String string) {

        this.soundManager.play(string);
    }

    public void setPlayerCharacter(int character) {
        this.playerCharacter = character;
    }

    public void setOpponentCharacter(int opponentCharacter) {
        this.opponentCharacter = opponentCharacter;
    }

    public void setHazardLevel(boolean hazards) {
        this.hzm.setHazardLevel(hazards);
    }

    public void setHazards(int rangeCeilling, boolean gameBeginning) {
        hzm.setHazards(rangeCeilling,
                       getPlayer(Constants.PLAYER1),
                       getPlayer(Constants.PLAYER2),
                       gameBeginning);
    }

    public ActionState getCurrentState() {
        return currentState;
    }

    public Player getActiveCharacter() {

        return activeCharacter;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public GameArea getGameArea() {
        return gameArea;
    }

    public final class SpriteAnimationSwitcher implements PathObserver {
        public void pathCancelled(Sprite sprite, Path path) {
            ((ImageSprite) sprite).setAnimationMode(ImageSprite.NO_ANIMATION);
        }

        public void pathCompleted(Sprite sprite, Path path, long when) {
            ((ImageSprite) sprite).setAnimationMode(ImageSprite.NO_ANIMATION);
            Utilities.log.info("SP Location : "
                    + sprite.getBounds().getLocation());
        }
    }

    public final class GameController extends Controller {

        private CountAnimationVariableContainer container;

        public GameController(CountAnimationVariableContainer container) {
            super();
            this.container = container;
        }

        public void setActionState(ActionState state) {
            if (nextState != null) {
                ActionState tmp1 = currentState;
                ActionState tmp2 = nextState;
                Thread.dumpStack();
                Utilities.log.severe("We are still in " + tmp1 + "-to-" + tmp2
                        + " transition, so skipping changing state to " + state);
                new Exception().printStackTrace();
            }

            if (!currentState.canChangeTo(state)) {
                Utilities.log.warning("Transition [" + currentState + "]-to-["
                        + state + "] not allowed");
                new Exception().printStackTrace();
            }

            switch (state) {
                case NEXTTURN:
                case TURN_START:
                case PLAYER_MOVE:
                case OPPONENT_MOVE:
                case BOARD_MOVEMENTS:
                    boardScrollHandler.setEnabled(false);
                    break;

                default:
                    boardScrollHandler.setEnabled(true);
                    break;
            }

            switch (state) {
                case NEXTTURN:
                    taskQueue.reset();
                    soundManager.stopAllSounds();
                    // add task to be executed after screen scrolls to dot containers
                    taskQueue.addTaskInQueue(new Runnable() {
                        public void run() {
                            gameListener.turnBegins();
                        }
                    });
                    break;

                case PLAYER_MOVE:
                    soundManager.stopAllSounds();
                    soundManager.play("moveYourCharacter"); //$NON-NLS-1$
                    updateSteps();
                    break;

                case OPPONENT_MOVE:
                    Utilities.log.severe("MOVE ENEMY");
                    //soundManager.play("enemy1_moveMeForward"); //1645 msec   //$NON-NLS-1$
                    updateSteps();
                    break;

                case BOARD_MOVEMENTS:
                    if (activeCharacter.isMovingBackwards()) {
                        gameArea.switchArea(activeCharacter.boardPosition,
                                            new PathObserver() {
                                                public void pathCompleted(
                                                        Sprite sprite,
                                                        Path path, long when) {
                                                    setFollowsPathable(activeCharacter.sprite,
                                                                       ENCLOSE_PATHABLE);
                                                }

                                                public void pathCancelled(
                                                        Sprite sprite, Path path) {
                                                    setFollowsPathable(activeCharacter.sprite,
                                                                       ENCLOSE_PATHABLE);
                                                }
                                            });
                    } else {
                        setFollowsPathable(activeCharacter.sprite,
                                           ENCLOSE_PATHABLE);
                    }
                    final Sprite remove = activeCharacter.getHalfSizeStamp();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            removeSprite(remove);
                            setCursor(null);
                        }
                    });

                default:
                    break;
            }

            nextState = state;
            Utilities.log.info(currentState + "-to-" + state + " transition");

            if (state.equals(ActionState.WAIT4_OPPONENT_MOVE)) {
                // Play opponent sound before blinking
                soundManager.play("enemy1_moveMeForward"); //1645 msec   //$NON-NLS-1$
            }
            if (!requestFocusInWindow()) {
                requestFocus();
            }
        }

        public void stampClicked(Object src, int playerID) {

            if (playerID == Constants.PLAYER1
                    && (currentState == ActionState.WAIT4_PLAYER_MOVE || currentState == ActionState.WAIT4NEXTTURN)) {
                soundManager.play("iconClick");
                player.getStamp().setEnabled(false);
                player.getStamp().setAnimationMode(ImageSprite.NO_ANIMATION);
                player.getStamp().resetToFirstFrame();
                if (currentState == ActionState.WAIT4_PLAYER_MOVE) {
                    //TODO: setCursor(player.getHalfSizeCursor());
                    setCursor(dotCursor);
                    setActionState(ActionState.PLAYER_MOVE);
                    removeSprite(player.getStamp());
                    Sprite sprite = player.getHalfSizeStamp();
                    sprite.setLocation(lastMouseX - (sprite.getWidth() >> 1),
                                       lastMouseY - (sprite.getHeight() >> 1));
                    addSprite(sprite);
                } else {
                    //TODO: setCursor(player.getCursor());
                    player.getStamp()
                          .setRenderOrder(Constants.STAMP_MOVEMENT_LAYER);
                    setCursor(dotCursor);
                    setActionState(ActionState.NEXTTURN);
                }
            } else if (playerID == Constants.PLAYER2
                    && currentState == ActionState.WAIT4_OPPONENT_MOVE) {
                soundManager.play("iconClick");
                opponent.getStamp().setEnabled(false);
                opponent.getStamp().setAnimationMode(ImageSprite.NO_ANIMATION);
                opponent.getStamp().resetToFirstFrame();
                removeSprite(opponent.getStamp());
                Sprite sprite = opponent.getHalfSizeStamp();
                sprite.setLocation(lastMouseX - (sprite.getWidth() >> 1),
                                   lastMouseY - (sprite.getHeight() >> 1));
                addSprite(sprite);
                //TODO: setCursor(opponent.getHalfSizeCursor());
                setCursor(dotCursor);
                setActionState(ActionState.OPPONENT_MOVE);
            }
        }

        public void choiceIS(Object src, final int leftrigth) {
            if (currentState != ActionState.CHOICE_MADE && nextState == null) {
                soundManager.play("iconClick");
                choice = leftrigth;
                setActionState(ActionState.CHOICE_MADE);
                taskQueue.postRunnable(new Runnable() {

                    public void run() {
                        gameListener.imFast(leftrigth);
                    }

                });
                setPlayerChoice(choice);
            }
        }

        public void playerClicked(Object src, int playerID) {
            Utilities.log.info(src.toString());
            if (currentState == ActionState.PLAYER_MOVE
                    || currentState == ActionState.OPPONENT_MOVE) {
                if (playerID == Constants.PLAYER1 && player.boardPosition > 0) {
                    sqClicked(null, player.boardPosition);
                } else if (playerID == Constants.PLAYER2
                        && opponent.boardPosition > 0) {
                    sqClicked(null, opponent.boardPosition);
                }
            }
        }

        public void bottomSpriteClicked(Object src) {
            System.out.println("Clicked : " + src);
        }

        public void sqClicked(Object src, int number) {
            Utilities.log.info("Square clicked : " + number);

            switch (currentState) {
                case PLAYER_MOVE:
                    Utilities.log.info("CASE PLAYER_MOVE");
                    Utilities.log.info("tmpBoardPosition = "
                            + player.boardPosition);
                    Utilities.log.info("steps = " + player.steps);

                    if (number > player.tmpBoardPosition && player.toCount > 0) {
                        int finalTarged = player.boardPosition + player.steps;

                        if (finalTarged > Constants.LAST_SQUARE)
                            finalTarged = Constants.LAST_SQUARE;

                        soundManager.stopAllSounds();
                        if (number > finalTarged) {
                            Utilities.log.info("too far");
                            opponentTalks("enemy1_playerTooFar");
                        } else {
                            int tmp = number - player.tmpBoardPosition;

                            Point[] points = new Point[tmp];
                            for (int i = points.length - 1, j = 1; i >= 0; i--, j++) {
                                points[j - 1] = gameArea.getSquareCenter(j
                                        + player.tmpBoardPosition);
                            }

                            LineUpObserver lineUpObserver = new LineUpObserver();
                            lineUpObserver.setBounds(player.boardPosition,
                                                     finalTarged,
                                                     player.tmpBoardPosition,
                                                     this.container);

                            //Utilities.log.info("new line up obeserver for player");

                            dots[playerChoice].lineUp(points,
                                                      player.toCount - tmp + 1,
                                                      player.toCount,
                                                      null,
                                                      50,
                                                      lineUpObserver);
                            player.toCount -= tmp;

                            player.tmpBoardPosition = number;

                            if (number == finalTarged) {
                                Utilities.log.info("bull's eye!");
                                // use numeric sound files

                                soundManager.play(Utilities.charac4id(Constants.PLAYER1)
                                                          + (player.tmpBoardPosition),
                                                  new SoundListener() {

                                                      public void run() {
                                                          // TODO Auto-generated method stub
                                                          player.setSteps(player.steps);
                                                          setActionState(player.go());
                                                      }

                                                  });
                                //                            player.boardPosition = number;
                            } else {
                                // use numeric sound files

                                soundManager.play(Utilities.charac4id(Constants.PLAYER1)
                                        + "counting_"
                                        + (player.tmpBoardPosition));
                            }
                        }

                    }
                    break;

                case OPPONENT_MOVE:
                    Utilities.log.info("CASE OPPONENT_MOVE");
                    Utilities.log.info("tmpBoardPosition = "
                            + opponent.boardPosition);
                    Utilities.log.info("steps = " + opponent.steps);

                    if (number > opponent.tmpBoardPosition
                            && opponent.toCount > 0) {
                        int finalTarged = opponent.boardPosition
                                + opponent.steps;

                        soundManager.stopAllSounds();
                        if (number > finalTarged) {
                            Utilities.log.info("too far");
                            opponentTalks("enemy1_opponentTooFar");
                        } else {
                            int tmp = number - opponent.tmpBoardPosition;

                            Point[] points = new Point[tmp];
                            for (int i = points.length - 1, j = 1; i >= 0; i--, j++) {
                                points[j - 1] = gameArea.getSquareCenter(j
                                //                        for (int i = 1; i <= points.length; i++) {
                                //                            points[i - 1] = gameArea.getSquareCenter(i
                                        + opponent.tmpBoardPosition);
                            }

                            LineUpObserver lineUpObserver = new LineUpObserver();
                            lineUpObserver.setBounds(opponent.boardPosition,
                                                     finalTarged,
                                                     opponent.tmpBoardPosition,
                                                     this.container);

                            dots[opponentChoice].lineUp(points,
                                                        opponent.toCount - tmp
                                                                + 1,
                                                        opponent.toCount,
                                                        null,
                                                        50,
                                                        lineUpObserver);
                            opponent.toCount -= tmp;

                            opponent.tmpBoardPosition = number;

                            if (number == finalTarged) {
                                Utilities.log.info("bull's eye!");
                                // use numeric soun files
                                soundManager.play(Utilities.charac4id(Constants.PLAYER2)
                                                          + (opponent.tmpBoardPosition),
                                                  new SoundListener() {

                                                      public void run() {
                                                          // TODO Auto-generated method stub
                                                          setActionState(opponent.go());
                                                      }

                                                  });
                                //                            player.boardPosition = number;
                            } else {
                                // use numeric sound files

                                soundManager.play(Utilities.charac4id(Constants.PLAYER2)
                                        + "counting_"
                                        + (opponent.tmpBoardPosition));
                            }
                        }
                    }

                    break;

                default:
                    break;
            }
        }

        public GameArea getGameArea() {
            return gameArea;
        }

        /* This method is never called
        public void menu(Object src) {
            //stateListener.changeState(GameStates.MENU);
            //            if (go.getTheme().getLevel() == ThemeVariables.IN_THE_JUNGLE) {
            //                go.setTheme(ThemeVariables.UNDER_THE_SEA);
            //            } else {
            //                go.setTheme(ThemeVariables.IN_THE_JUNGLE);
            //            }

            //            go.changeState(GameStates.GAMEOVER);
            //            switch (currentState) {
            //            case PLAYER_MOVE:
            //                player.go();
            //                break;
            //
            //            case OPPONENT_MOVE:
            //                opponent.go();
            //                break;
            //
            //            default:
            //                break;
            //            }
        }
         *
         */
    }

    //GameObject go;
    private ImageButtonSprite menuBtn;
    private DotCarpet dotCarpet;
    private ImageSprite playerSprite;
    private ImageSprite opponentSprite;
    private GameController controller;

    GameArea gameArea;
    //private NumCompManager ncm;

    public DotArray[] dots = new DotArray[2];
    private long sneakDeadline;
    //    private boolean startSneaking;
    private ActionState nextState;

    private SpriteAnimationSwitcher switchOffAnimation = new SpriteAnimationSwitcher();
    private SneakObserver sneakObserver = new SneakObserver();
    //    private CharacterMoverObserver characterMover = new CharacterMoverObserver();
    private ActionState currentState = ActionState.BEGINNING;
    private int playerChoice;
    private int opponentChoice;

    private int choice;
    private Point[] dotStackPoints = new Point[AdapDimensions.XMAX_FULL_RANGE]; //this stores the positions in which to stack dots when subtracting

    private GameTurn currentTurn;

    private Player player;
    private Player opponent;

    private HazardManager hzm;
    private LinkedList<Sprite> sprites2add = new LinkedList<Sprite>();
    private Player activeCharacter;

    private final FrameManager frameManager;

    protected ImageSprite scaringCreature;

    private TimerSprite timerSprite;

    private BoardScrollHandler boardScrollHandler;
    private Cursor dotCursor;
    protected int lastMouseX;
    protected int lastMouseY;

    public ChoiceScreen(FrameManager frameManager, NRRunnableQueue taskQueue,
            SoundManager soundManager, int playerCharacter,
            int opponentCharacter, GameListener gameListener) {

        super(frameManager);
        this.frameManager = frameManager;
        this.taskQueue = taskQueue;
        this.soundManager = soundManager;
        this.opponentCharacter = opponentCharacter;
        this.playerCharacter = playerCharacter;
        this.gameListener = gameListener;

        // create our meta manager
        _metamgr = new NRMediaManager(frameManager, this);
        _remgr = _metamgr.getRegionManager();
        _animmgr = _metamgr.getAnimationManager();
        _spritemgr = _metamgr.getSpriteManager();

        //go = GameObject.getInstance();

        hzm = new HazardManager(this);
        //ncm = new NumCompManager(this);

        controller = new GameController(this);

        this.sneakObserver = new SneakObserver();
        this.sneakObserver.setSneakListener(this);
    }

    @Override
    protected ActionSpriteHandler createActionSpriteHandler() {
        return new MediaPanel.ActionSpriteHandler() {
            public void mousePressed(MouseEvent me) {

                if (_activeSprite == null) {
                    // see if we can find one
                    Sprite s = getHit(me);
                    if (s instanceof ActionSprite
                            || s instanceof CommandOnPressedSprite) {
                        _activeSprite = s;
                    }
                }

                if (_activeSprite instanceof ArmingSprite) {
                    ((ArmingSprite) _activeSprite).setArmed(true);
                }

                if (_activeSprite instanceof CommandOnPressedSprite) {
                    CommandOnPressedSprite cs = (CommandOnPressedSprite) _activeSprite;
                    Controller.postAction(new CommandEvent(ChoiceScreen.this,
                            cs.getActionCommand(), cs.getCommandArgument(),
                            me.getWhen(), me.getModifiers()));
                }
            }

            @Override
            public void mouseMoved(MouseEvent me) {
                super.mouseMoved(me);
                lastMouseX = me.getX();
                lastMouseY = me.getY();
                final ActionState state = currentState;
                Sprite sprite = null;
                if (state == ActionState.TURN_START
                        || state == ActionState.CHOOSE
                        || state == ActionState.SNEAK) {
                    sprite = player.getStamp();
                } else if (state == ActionState.PLAYER_MOVE) {
                    sprite = player.getHalfSizeStamp();
                } else if (state == ActionState.OPPONENT_MOVE) {
                    sprite = opponent.getHalfSizeStamp();
                }
                if (sprite != null) {
                    final Rectangle bounds = sprite.getBounds();
                    sprite.setLocation(me.getX() - (bounds.width >> 1),
                                       me.getY() - (bounds.height >> 1));
                }
            }
        };
    }

    public boolean resolveColisions() {
        Utilities.log.info("Trying to resolve colisions pp :"
                + player.boardPosition + " op: " + opponent.boardPosition);

        if (activeCharacter == player) {
            if (opponent.boardPosition == player.boardPosition) {
                setActiveCharacter(opponent);
                opponentTalks("enemy1_youSentMeBack", new SoundListener() {

                    public void run() {
                        setActionState(opponent.go(-1));
                    }

                });
                return true;
            } else {

                // Player hazard
                final HazardSprite hazardSprite = hzm.checkForCollisions(player.boardPosition);
                if (hazardSprite != null) {

                    final int penalty = hazardSprite.getPenaltyValue();

                    final SoundListener l = new SoundListener() {

                        public void run() {
                            setActionState(player.go(penalty));
                        }

                    };

                    SoundListener hazard_sound = null;

                    //make a nasty noise
                    if (penalty == -1) {
                        hazard_sound = new SoundListener() {
                            public void run() {
                                talk(hazardSprite, "anemone1", l);
                            }
                        };
                    } else if (penalty == -2) {
                        hazard_sound = new SoundListener() {
                            public void run() {
                                talk(hazardSprite, "anemone2", l);
                            }
                        };
                    } else if (penalty == -3) {
                        hazard_sound = new SoundListener() {
                            public void run() {
                                talk(hazardSprite, "anemone3", l);
                            }
                        };
                    }

                    //play explanation
                    iTalk(Utilities.charac4id(Constants.PLAYER1)
                            + "landedOnTrap", hazard_sound); //$NON-NLS-1$

                    scaringCreature = hazardSprite;
                    return true;
                }
            }
            if (player.boardPosition == Constants.LAST_SQUARE) {
                taskQueue.reset();
                taskQueue.addTaskInQueue(new Runnable() {

                    public void run() {
                        gameListener.playerWins(Constants.PLAYER1);
                    }

                });
            }
        } else if (activeCharacter == opponent) {
            if (opponent.boardPosition == player.boardPosition) {
                setActiveCharacter(player);
                opponentTalks("enemy1_youHaveToGoBack", new SoundListener() {

                    public void run() {
                        setActionState(player.go(-1));
                    }

                });
                return true;
            } else {
                // Opponent hazard
                final HazardSprite hazardSprite = hzm.checkForCollisions(opponent.boardPosition);
                if (hazardSprite != null) {
                    final int penalty = hazardSprite.getPenaltyValue();

                    final SoundListener l = new SoundListener() {

                        public void run() {
                            setActionState(opponent.go(penalty));
                        }

                    };

                    SoundListener hazard_sound = null;

                    //make a nasty noise
                    if (penalty == -1) {
                        hazard_sound = new SoundListener() {
                            public void run() {
                                talk(hazardSprite, "anemone1", l);
                            }
                        };
                    } else if (penalty == -2) {
                        hazard_sound = new SoundListener() {
                            public void run() {
                                talk(hazardSprite, "anemone2", l);
                            }
                        };
                    } else if (penalty == -3) {
                        hazard_sound = new SoundListener() {
                            public void run() {
                                talk(hazardSprite, "anemone3", l);
                            }
                        };
                    }
                    //play explanation
                    opponentTalks(Utilities.charac4id(Constants.PLAYER2)
                            + "landedOnTrap", hazard_sound); //$NON-NLS-1$
                    scaringCreature = hazardSprite;
                    return true;
                }
            }
            if (opponent.boardPosition == Constants.LAST_SQUARE) {
                taskQueue.reset();
                taskQueue.addTaskInQueue(new Runnable() {

                    public void run() {
                        gameListener.playerWins(Constants.PLAYER2);
                    }

                });
            }
        }
        return false;
    }

    private void setActiveCharacter(Player character) {
        activeCharacter = character;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.addRenderingHints(Utilities.antialiasRH);
        super.paint(g2d);
        g2d.dispose();
    }

    @Override
    protected void paintBehind(Graphics2D gfx, Rectangle dirtyRect) {
        super.paintBehind(gfx, dirtyRect);
        gfx.setBackground(getBackground());
        gfx.clearRect(dirtyRect.x,
                      dirtyRect.y,
                      dirtyRect.width,
                      dirtyRect.height);
    }

    @Override
    protected void paintInFront(Graphics2D gfx, Rectangle dirtyRect) {
        super.paintInFront(gfx, dirtyRect);
        if (DebugFrame.DEBUG) {
            for (int i = 0; i < dots.length; i++) {
                Rectangle[] circles = dots[i].getCircles();
                for (int j = 0; j < circles.length; j++) {
                    if (dirtyRect.intersects(circles[j])) {
                        Rectangle rect = circles[j];
                        gfx.drawArc(rect.x,
                                    rect.y,
                                    rect.width,
                                    rect.height,
                                    0,
                                    360);
                    }

                }
            }
        }
    }

    public void load() {
        player = new Player(Constants.PLAYER1);
        opponent = new Player(Constants.PLAYER2);

        gameListener.gameBegins();

        gameArea = new GameArea(frameManager, this);

        dots[Constants.LEFT] = new DotArray(Constants.LEFT, frameManager, this);
        dots[Constants.RIGHT] = new DotArray(Constants.RIGHT, frameManager,
                this);

        for (int i = 0; i < dots.length; i++) {
            dots[i].load();
        }

        player.setGameArea(gameArea);
        opponent.setGameArea(gameArea);

        playerSprite = new ImageButtonSprite("playerClicked", Constants.PLAYER1);
        playerSprite.setFrames(new MultiFrameImageImpl(
                new Mirage[] {
                        new BufferedMirage(
                                ImageFactory.getImage(ThemeVariables.player1CharacterFiles[playerCharacter][0])),
                        new BufferedMirage(
                                ImageFactory.getImage(ThemeVariables.player1CharacterFiles[playerCharacter][1])) }));

        opponentSprite = new ImageButtonSprite("playerClicked",
                Constants.PLAYER2);
        opponentSprite.setFrames(new MultiFrameImageImpl(
                new Mirage[] {
                        new BufferedMirage(
                                ImageFactory.getImage(ThemeVariables.player2CharacterFiles[opponentCharacter][0])),
                        new BufferedMirage(
                                ImageFactory.getImage(ThemeVariables.player2CharacterFiles[opponentCharacter][1])) }));

        playerSprite.addSpriteObserver(switchOffAnimation);
        opponentSprite.addSpriteObserver(switchOffAnimation);
        player.setSprite(playerSprite);
        opponent.setSprite(opponentSprite);

        menuBtn = new ImageButtonSprite(new BufferedMirage(
                ImageFactory.getImage(ThemeVariables.menuButton)), "menu", null);

        menuBtn.setLocation(i(32), i(900));

        menuBtn.setRenderOrder(Constants.MEBUBTN_LAYER);
        playerSprite.setRenderOrder(Constants.PLAYER_LAYER);
        opponentSprite.setRenderOrder(Constants.OPPONENT_LAYER);

        gameArea.load();

        Rectangle ldcb = getDotContainer(Constants.LEFT).getBounds();
        Rectangle rdcb = getDotContainer(Constants.RIGHT).getBounds();

        dotCarpet = new DotCarpet(rdcb.x - (ldcb.x + ldcb.width), ldcb.height);
        dotCarpet.setLocation(ldcb.x + ldcb.width, ldcb.y);

        timerSprite = new TimerSprite(opponent.getStampImage());

        dotCarpet.load();

        Rectangle r1 = new Rectangle(i(GraphicsVariables.LEFT_DOT_ARRAY_X)
                + ldcb.x, i(GraphicsVariables.LEFT_DOT_ARRAY_Y) + ldcb.y,
                i(GraphicsVariables.DOT_ARRAY_DIAMETER),
                i(GraphicsVariables.DOT_ARRAY_DIAMETER));
        Rectangle r1_L = new Rectangle(
                i(GraphicsVariables.LEFTDC_LEFT_SUB_DOT_ARRAY_X) + ldcb.x,
                i(GraphicsVariables.LEFTDC_LEFT_SUB_DOT_ARRAY_Y) + ldcb.y,
                i(GraphicsVariables.SUB_DOT_ARRAY_DIAM),
                i(GraphicsVariables.SUB_DOT_ARRAY_DIAM));
        Rectangle r1_R = new Rectangle(
                i(GraphicsVariables.LEFTDC_RIGHT_SUB_DOT_ARRAY_X) + ldcb.x,
                i(GraphicsVariables.LEFTDC_RIGHT_SUB_DOT_ARRAY_Y) + ldcb.y,
                i(GraphicsVariables.SUB_DOT_ARRAY_DIAM),
                i(GraphicsVariables.SUB_DOT_ARRAY_DIAM));

        Rectangle r2 = new Rectangle(i(GraphicsVariables.RIGHT_DOT_ARRAY_X)
                + rdcb.x, i(GraphicsVariables.RIGHT_DOT_ARRAY_Y) + rdcb.y,
                i(GraphicsVariables.DOT_ARRAY_DIAMETER),
                i(GraphicsVariables.DOT_ARRAY_DIAMETER));
        Rectangle r2_L = new Rectangle(
                i(GraphicsVariables.RIGHTDC_LEFT_SUB_DOT_ARRAY_X) + rdcb.x,
                i(GraphicsVariables.RIGHTDC_LEFT_SUB_DOT_ARRAY_Y) + rdcb.y,
                i(GraphicsVariables.SUB_DOT_ARRAY_DIAM),
                i(GraphicsVariables.SUB_DOT_ARRAY_DIAM));
        Rectangle r2_R = new Rectangle(
                i(GraphicsVariables.RIGHTDC_RIGHT_SUB_DOT_ARRAY_X) + rdcb.x,
                i(GraphicsVariables.RIGHTDC_RIGHT_SUB_DOT_ARRAY_Y) + rdcb.y,
                i(GraphicsVariables.SUB_DOT_ARRAY_DIAM),
                i(GraphicsVariables.SUB_DOT_ARRAY_DIAM));

        dots[Constants.LEFT].setBounds(r1, r1_L, r1_R);
        dots[Constants.RIGHT].setBounds(r2, r2_L, r2_R);

        BufferedImage dotCursorImage = new BufferedImage(32, 32,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dotCursorImage.createGraphics();
        g.setColor(ThemeVariables.boardColor);
        g.fillRect(15, 15, 1, 1);
        g.dispose();

        dotCursor = Toolkit.getDefaultToolkit()
                           .createCustomCursor(dotCursorImage,
                                               new Point(15, 15),
                                               "Dot Cursor");

        //                new BubbleSprite(
        //                new MultiFrameImageImpl(
        //                        new Mirage[] {
        //                                new BufferedMirage(
        //                                        ImageFactory.getImage("resources/images/bubble.png")),
        //                                new BufferedMirage(
        //                                        ImageFactory.getImage("resources/images/bubble_wo_anchor.png")) }));

        Utilities.log.info("LOAD HZM!");
        hzm.load();

    }

    public void start() {
        setBackground(ThemeVariables.screenColor);
        timerSprite.setBackground(ThemeVariables.screenColor);

        gameArea.start();

        Rectangle boardBounds = gameArea.getBoardBounds();
        final Point playerStartPos = new Point(boardBounds.x, boardBounds.y);
        playerStartPos.translate((-player.sprite.getWidth()),
                                 -player.sprite.getHeight());
        final Point opponentStartPos = new Point(boardBounds.x, boardBounds.y);
        opponentStartPos.translate((-opponent.sprite.getWidth()), 0);

        for (int i = 0; i < dots.length; i++) {
            dots[i].setBorderY(boardBounds.y);
            addViewTracker(dots[i]);
        }

        player.setLocation(playerStartPos);
        opponent.setLocation(opponentStartPos);

        player.setMiniSprite(gameArea.createSpriteForBottom(playerSprite, 1.5));
        opponent.setMiniSprite(gameArea.createSpriteForBottom(opponentSprite,
                                                              1.5));

        final Point pos1 = gameArea.calculatePositionATBottom(player.getMiniSprite(),
                                                              0);
        player.getMiniSprite().setLocation(pos1.x, pos1.y);
        final Point pos2 = gameArea.calculatePositionATBottom(opponent.getMiniSprite(),
                                                              0);
        opponent.getMiniSprite().setLocation(pos2.x, pos2.y);

        addSprite(player.getMiniSprite());
        addSprite(opponent.getMiniSprite());
        addSprite(playerSprite);
        addSprite(opponentSprite);
        addSprite(dotCarpet);
        //        addSprite(choiceSrcBtn);
        addSprite(menuBtn);

        boardScrollHandler = new BoardScrollHandler(this, boardBounds);
        addMouseListener(boardScrollHandler);
        addMouseMotionListener(boardScrollHandler);

        timerSprite.layout();
        //bubbleSprite.layout();

        setActionState(ActionState.WAIT4NEXTTURN);
    }

    public void setCurrentTurn(GameTurn currentTurn) {
        this.currentTurn = currentTurn;
    }

    public void stop() {
        removeMouseListener(boardScrollHandler);
        removeMouseMotionListener(boardScrollHandler);
        boardScrollHandler = null;

        gameArea.stop();
        for (int i = 0; i < dots.length; i++) {
            removeViewTracker(dots[i]);
        }

        clearSprites();
        clearAnimations();

        _nx = _vbounds.x = 0;
        _ny = _vbounds.y = 0;
        currentState = ActionState.BEGINNING;
    }

    public Controller getController() {
        return controller;
    }

    public DotContainerSprite getDotContainer(int side) {
        return (side == Constants.LEFT) ? gameArea.getLeftDotContainer()
                : gameArea.getRightDotContainer();
    }

    public void openContainer(int side) {
        openContainer(side, false);
    }

    public void openContainer(int side, boolean andUnfadeDots) {

        if (!getDotContainer(side).isOpened()) {
            getDotContainer(side).open();
            soundManager.play("chestOpen");
        }
        dots[side].setVisible(true);

        if (andUnfadeDots) {
            dots[side].unFade();
        }

    }

    public void setActionState(ActionState state) {
        this.controller.setActionState(state);
    }

    /* Moved to Game controller
    public void setActionState(ActionState state) {
        if (nextState != null) {
            ActionState tmp1 = currentState;
            ActionState tmp2 = nextState;
            Thread.dumpStack();
            Utilities.log.severe("We are still in " + tmp1 + "-to-" + tmp2
                    + " transition, so skipping changing state to " + state);
            new Exception().printStackTrace();
        }

        if (!currentState.canChangeTo(state)) {
            Utilities.log.warning("Transition [" + currentState + "]-to-["
                    + state + "] not allowed");
            new Exception().printStackTrace();
        }

        switch (state) {
            case NEXTTURN:
            case TURN_START:
            case PLAYER_MOVE:
            case OPPONENT_MOVE:
            case BOARD_MOVEMENTS:
                boardScrollHandler.setEnabled(false);
                break;
            default:
                boardScrollHandler.setEnabled(true);
                break;
        }

        switch (state) {
            case NEXTTURN:
                go.getTaskQueue().reset();
                go.getSoundManager().stopAllSounds();
                // add task to be executed after screen scrolls to dot containers
                go.getTaskQueue().addTaskInQueue(new Runnable() {
                    public void run() {
                        ncm.turnBegins();
                    }
                });
                break;

            case PLAYER_MOVE:
                go.getSoundManager().stopAllSounds();
                go.getSoundManager().play("moveYourCharacter"); //$NON-NLS-1$
                updateSteps();
                break;

            case OPPONENT_MOVE:
                go.getSoundManager().play("enemy1_moveMeForward"); //1645 msec   //$NON-NLS-1$
                updateSteps();
                break;

            case BOARD_MOVEMENTS:
                setFollowsPathable(activeCharacter.sprite, ENCLOSE_PATHABLE);
                final Sprite remove = activeCharacter.getHalfSizeStamp();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        removeSprite(remove);
                        setCursor(null);
                    }
                });

            default:
                break;
        }

        nextState = state;
        Utilities.log.info(currentState + "-to-" + state + " transition");
    }
     *
     */

    public void startSneaking(long currentDeadline) {
        sneakDeadline = currentDeadline;
        setActionState(SNEAK);
        Utilities.log.info("Sneaking time : " + currentDeadline);
    }

    @Override
    protected void willTick(long tickStamp) {
        super.willTick(tickStamp);
        if (nextState != null) {
            ActionState tmpState = nextState;
            nextState = null;

            //            if (tmpState != currentState) {
            final Rectangle dcBounds = dotCarpet.getBounds();
            final ImageButtonSprite playerStamp = player.getStamp();
            final ImageButtonSprite opponentStamp = opponent.getStamp();

            switch (tmpState) {
                case TURN_START:
                    gameArea.getLeftDotContainer().setEnabled(false);
                    gameArea.getRightDotContainer().setEnabled(false);
                    break;

                case SNEAK:
                    timerSprite.setLocation(dcBounds.x
                                                    + ((dcBounds.width - timerSprite.getWidth()) >> 1),
                                            dcBounds.y);
                    addSprite(timerSprite);
                    timerSprite.setDuration(sneakDeadline);
                    Path sneakPath = new DelayPath(sneakDeadline);
                    timerSprite.addSpriteObserver(this.sneakObserver);
                    timerSprite.start(sneakPath);

                case CHOOSE:
                    gameArea.getLeftDotContainer().setEnabled(true);
                    gameArea.getRightDotContainer().setEnabled(true);
                    break;

                case CHOICE_MADE:
                    gameArea.getLeftDotContainer().setEnabled(false);
                    gameArea.getRightDotContainer().setEnabled(false);

                    Utilities.log.info("Choice is " + choice);
                    for (int i = 0; i < dots.length; i++) {
                        dots[i].completeFadeOut();
                    }

                    timerSprite.stop();

                    gameArea.align(playerStamp, playerChoice);
                    gameArea.align(opponentStamp, opponentChoice);

                    //                    addSprite(playerStamp);
                    addSprite(opponentStamp);
                    //                openContainer(choice, true);

                    setCursor(null);
                    dotCarpet.setVisible(true);

                    break;

                case WAIT4_PLAYER_MOVE:
                    if (gameArea.getLeftDotContainer().isSubtraction()) {
                        dots[Constants.LEFT].cleanAfterSubstraction();
                    }
                    if (gameArea.getRightDotContainer().isSubtraction()) {
                        dots[Constants.RIGHT].cleanAfterSubstraction();
                    }
                    setActiveCharacter(player);

                    gameArea.switchArea(player.getBoardPosition(),
                                        boardScrollHandler);

                    playerStamp.setEnabled(true);
                    playerStamp.setAnimationMode(ImageSprite.TIME_BASED);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            controller.stampClicked(playerStamp, player.id);
                        }
                    });

                    break;
                case PLAYER_MOVE:
                    break;

                case WAIT4_OPPONENT_MOVE:
                    setActiveCharacter(opponent);
                    gameArea.switchArea(opponent.getBoardPosition(),
                                        boardScrollHandler);
                    opponentStamp.setEnabled(true);
                    opponentStamp.setAnimationMode(ImageSprite.TIME_BASED);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            controller.stampClicked(opponentStamp, opponent.id);
                        }
                    });

                    break;
                case OPPONENT_MOVE:
                    break;

                case WAIT4NEXTTURN:
                    playerStamp.setAnimationMode(ImageSprite.TIME_BASED);
                    playerStamp.setEnabled(true);
                    playerStamp.setLocation(dcBounds.x
                                                    + ((dcBounds.width - playerStamp.getWidth()) >> 1),
                                            dcBounds.y
                                                    + ((dcBounds.height - playerStamp.getHeight()) >> 1));
                    removeSprite(playerStamp);
                    removeSprite(opponentStamp);
                    addSprite(playerStamp);
                    dotCarpet.setVisible(false);
                    gameArea.resetContainers();

                    break;

                case NEXTTURN:
                    //                choiceSrcBtn.setEnabled(false);
                    //                choiceSrcBtn.setOrientation(DirectionCodes.NORTH);
                    for (int i = 0; i < dots.length; i++) {
                        dots[i].reset();
                    }
                    taskQueue.nextTaskInQueue();

                    break;

                case BOARD_MOVEMENTS:
                    playerSprite.setRenderOrder(Constants.PLAYER_LAYER);
                    opponentSprite.setRenderOrder(Constants.OPPONENT_LAYER);
                    if (activeCharacter.boardMovementPath != null) {
                        Utilities.log.info("We have PAHT TO MOVE !");
                        activeCharacter.sprite.setRenderOrder(Constants.BOARD_MOVEMENT_LAYER);
                        final int difference = activeCharacter.boardPosition
                                - activeCharacter.prevPosition;
                        taskQueue.addTask(new Runnable() {
                            public void run() {
                                playAdditionOrSubtruction(difference >= 0,
                                                          activeCharacter.id,
                                                          activeCharacter.prevPosition,
                                                          Math.abs(difference),
                                                          resultAnounced);
                            }
                        },
                                          750,
                                          true);
                    }
                    break;
                default:
                    break;
            }
            //            }

            currentState = tmpState;
        }

        synchronized (sprites2add) {
            if (!sprites2add.isEmpty()) {

                for (Sprite spr : sprites2add) {
                    addSprite(spr);
                    RainAnimation rainAnimation = new org.unicog.numberrace.media_patched.RainAnimation(
                            new Rectangle(spr.getBounds()), 600);
                    rainAnimation.setRenderOrder(Constants.HAZARD_APPEARANCE_ANIMATION_LAYER);
                    addAnimation(rainAnimation);
                }
                sprites2add.clear();
            }
        }
    }

    public final class ResultAnnonced implements SoundListener, PathObserver {

        volatile private boolean need2finish = false;

        public void run() {
            need2finish = !need2finish;

            if (!need2finish) {
                finish();
            }
        }

        private void finish() {
            taskQueue.addTask(new Runnable() {
                public void run() {
                    activeCharacter.getMiniSprite()
                                   .move(new ScrollSafeLinePath(
                                           gameArea.calculatePositionATBottom(activeCharacter.getMiniSprite(),
                                                                              activeCharacter.getBoardPosition()),
                                           300));

                    getGameArea().setHnum(-1);
                    getGameArea().setDnum(-1, -1);
                    getGameArea().setHint(-1, null);

                    //            dots[opponentChoice].removeLineUpObserver();
                    //            dots[playerChoice].removeLineUpObserver();

                    clearPathable();
                    if (scaringCreature != null) {
                        scaringCreature.setAnimationMode(ImageSprite.NONE);
                        scaringCreature = null;
                    }
                    if (!activeCharacter.isMovingBackwards()) { // hide dot's only after character moved forward, not when character sent back. bug: [http://www.nmi.jyu.fi/jira/browse/NUMBERRACE-1]
                        hideDots(activeCharacter.id);
                    }

                    if (!resolveColisions()) {
                        taskQueue.nextTaskInQueue();
                    }
                }
            },
                              750,
                              true);
        }

        public void pathCancelled(Sprite sprite, Path path) {
            observe(sprite);
        }

        public void pathCompleted(Sprite sprite, Path path, long when) {
            observe(sprite);
        }

        private void observe(Sprite sprite) {
            sprite.removeSpriteObserver(this);
            need2finish = !need2finish;

            if (!need2finish) {
                finish();
            }
        }

    }

    SoundListener resultAnounced = new ResultAnnonced();

    public void setPlayerChoice(int sideSelected) {
        playerChoice = sideSelected;
        opponentChoice = (playerChoice == Constants.LEFT) ? Constants.RIGHT
                : Constants.LEFT;

        updateSteps();
    }

    public void setOpponentChoice(int sideSelected) {
        opponentChoice = sideSelected;
        playerChoice = (opponentChoice == Constants.LEFT) ? Constants.RIGHT
                : Constants.LEFT;
        updateSteps();
    }

    private void updateSteps() {
        player.setSteps(currentTurn.getNumber(playerChoice));
        opponent.setSteps(currentTurn.getNumber(opponentChoice));
    }

    public void grabAndSubtractDots(int playerId, int sideSelected,
            GameTurn currentTurn) {

        //this function sets dots in motion, and shifts their drawing responsibility to ChoiceScreen or BoardScreen
        //        nextState = ActionState.LINEUP_SUBTRACTION;

        int numberDots = dots[sideSelected].getNumber();
        int numberToSubtract = currentTurn.getSubNumber(sideSelected,
                                                        Constants.RIGHT);
        int numberRemaining = numberDots - numberToSubtract;

        Utilities.log.info("nd: " + numberDots + " nts:" + numberToSubtract
                + " nr: " + numberRemaining);

        dots[sideSelected].lineUp(calculatePointLocations(sideSelected),
                                  numberRemaining + 1,
                                  numberDots,
                                  null);

    }

    public void grabAndLineUpDotsOnCarpet(final int player2, int sideSelected,
            GameTurn currentTurn) {
        //        nextState = LINING_UP_ON_CARPET;

        int numberDots = dots[sideSelected].getNumber();
        int numberToSubtract = (getDotContainer(sideSelected).isSubtraction()) ? currentTurn.getSubNumber(sideSelected,
                                                                                                          Constants.RIGHT)
                : 0;
        int numberRemaining = numberDots - numberToSubtract;

        PathObserver pathObserver = new PathObserver() {

            public void pathCancelled(Sprite arg0, Path arg1) {
                arg0.removeSpriteObserver(this);
            }

            public void pathCompleted(Sprite arg0, Path arg1, long arg2) {
                arg0.removeSpriteObserver(this);
                showNumberOnCarpet(player2);
            }
        };

        dots[sideSelected].lineUp(dotCarpet.dotStacks[sideSelected],
                                  1,
                                  numberRemaining,
                                  pathObserver);
    }

    private Point[] calculatePointLocations(int id) {
        int interDotInterval = (int) (dots[id].getConvertedMaxR() * 2); //note: need to call when this been assigned
        int x = getDotContainer(id).getX();
        int y = getDotContainer(id).getY();
        if (id == Constants.LEFT) {
            for (int i = 0; i < AdapDimensions.XMAX_FULL_RANGE; i++) {
                if ((i % 2) == 0) {
                    //                  dotStackPoints[i] = new Point(x + GraphicsVariables.LEFT_DOT_CONT_STACK_START_POINT_X - (int)((i >> 1)*interDotInterval),
                    dotStackPoints[i] = new Point(
                            x
                                    + i(GraphicsVariables.LEFT_DOT_CONT_STACK_START_POINT_X)
                                    + (int) ((Math.floor(i / 2)) * interDotInterval),
                            y
                                    + i(GraphicsVariables.LEFT_DOT_CONT_STACK_START_POINT_Y));
                } else {
                    //                  dotStackPoints[i] = new Point(x + GraphicsVariables.LEFT_DOT_CONT_STACK_START_POINT_X - (int)((i >> 1)*interDotInterval),
                    dotStackPoints[i] = new Point(
                            x
                                    + i(GraphicsVariables.LEFT_DOT_CONT_STACK_START_POINT_X)
                                    + (int) ((Math.floor(i / 2)) * interDotInterval),
                            y
                                    + i(GraphicsVariables.LEFT_DOT_CONT_STACK_START_POINT_Y)
                                    - interDotInterval);
                }
            }
        } else if (id == Constants.RIGHT) {
            for (int i = 0; i < AdapDimensions.XMAX_FULL_RANGE; i++) {
                if ((i % 2) == 0) {
                    //                  dotStackPoints[i] = new Point(x + GraphicsVariables.RIGHT_DOT_CONT_STACK_START_POINT_X + (int)((i >> 1)*interDotInterval),
                    dotStackPoints[i] = new Point(
                            x
                                    + i(GraphicsVariables.RIGHT_DOT_CONT_STACK_START_POINT_X)
                                    - (int) ((Math.floor(i / 2)) * interDotInterval),
                            y
                                    + i(GraphicsVariables.RIGHT_DOT_CONT_STACK_START_POINT_Y));
                } else {
                    //                  dotStackPoints[i] = new Point(x + GraphicsVariables.RIGHT_DOT_CONT_STACK_START_POINT_X + (int)((i >> 1)*interDotInterval),
                    dotStackPoints[i] = new Point(
                            x
                                    + i(GraphicsVariables.RIGHT_DOT_CONT_STACK_START_POINT_X)
                                    - (int) ((Math.floor(i / 2)) * interDotInterval),
                            y
                                    + i(GraphicsVariables.RIGHT_DOT_CONT_STACK_START_POINT_Y)
                                    - interDotInterval);
                }
            }
        }
        return dotStackPoints;
    }

    public void showNumberOnCarpet(int playerID) {
        if (playerID == Constants.PLAYER1) {
            dotCarpet.setTopDigit(currentTurn.getNumber(playerChoice));
            dotCarpet.setTopDigitVisible(true);
        } else { // OPPONENT
            assert playerID == Constants.PLAYER2;
            dotCarpet.setBottomDigit(currentTurn.getNumber(opponentChoice));
            dotCarpet.setBottomDigitVisible(true);
        }
    }

    public void hideDots(int playerID) {
        if (playerID == Constants.PLAYER1) {
            dots[playerChoice].setVisible(false);
        } else {
            dots[opponentChoice].setVisible(false);
        }
    }

    public Player getPlayer(int playerID) {
        if (playerID == Constants.PLAYER1) {
            return player;
        }
        assert playerID == Constants.PLAYER2;
        return opponent;
    }

    public HazardManager getHazardManager() {
        return hzm;
    }

    @Override
    protected void viewLocationDidChange(int dx, int dy) {
        super.viewLocationDidChange(dx, dy);
        for (int i = 0; i < dotStackPoints.length; i++) {
            Point p = dotStackPoints[i];
            if (p != null) {
                p.translate(dx, dy);
            }
        }
    }

    public void addHazard(HazardSprite hazardSprite) {

        final Sprite miniSprite = gameArea.createSpriteForBottom(hazardSprite);
        final Point pos = gameArea.calculatePositionATBottom(miniSprite,
                                                             hazardSprite.getBoardPosition());
        miniSprite.setLocation(pos.x, pos.y);

        Point point = gameArea.getSquareCenter(hazardSprite.getBoardPosition());
        hazardSprite.setLocation(point.x - (hazardSprite.getWidth() >> 1),
                                 point.y - (hazardSprite.getHeight() >> 1));
        synchronized (sprites2add) {
            sprites2add.add(hazardSprite);
            sprites2add.add(miniSprite);
        }
    }

    public void clearCarpet() {
        dotCarpet.setBottomDigitVisible(false);
        dotCarpet.setTopDigitVisible(false);
    }

    public void unload() {
        gameArea.unload();
        player = opponent = null;
        playerSprite = null;
        opponentSprite = null;
        dotCarpet = null;
        menuBtn = null;
        gameArea = null; // maybe also unload
        dotCursor = null;

        for (int i = 0; i < dots.length; i++) {
            dots[i].unload();
            dots[i] = null;
        }

        DotArray.clearDotsCache();
    }

    private SoundListener stopTalkingMe = new SoundListener() {

        public void run() {
            playerSprite.setAnimationMode(ImageSprite.NONE);
        }
    };

    private SoundListener stopTalkingOpponent = new SoundListener() {

        public void run() {
            opponentSprite.setAnimationMode(ImageSprite.NONE);
        }
    };

    private void talk(ImageSprite sprite, String remark, SoundListener l) {
        sprite.setFrameRate(5);
        sprite.setAnimationMode(ImageSprite.TIME_BASED);
        soundManager.play(remark, l);
    }

    public void iTalk(String remark) {
        talk(playerSprite, remark, stopTalkingMe);
    }

    public void iTalk(String remark, final SoundListener l) {
        talk(playerSprite, remark, new SoundListener() {

            public void run() {
                stopTalkingMe.run();
                if (l != null) {
                    l.run();
                }
            }
        });
    }

    public void opponentTalks(String remark) {
        talk(opponentSprite, remark, stopTalkingOpponent);
    }

    public void opponentTalks(String remark, final SoundListener l) {
        talk(opponentSprite, remark, new SoundListener() {

            public void run() {
                stopTalkingOpponent.run();
                if (l != null) {
                    l.run();
                }
            }
        });
    }

    public void playAdditionOrSubtruction(final boolean addition,
            final int characterID, final int num1, final int num2,
            final SoundListener talk_finnished) {

        final int result = addition ? num1 + num2 : num1 - num2;

        final int delay = 250;
        final DelayedSoundListener resultSL = new DelayedSoundListener(
                taskQueue, delay, new Runnable() {
                    public void run() {
                        getGameArea().setHnum(result);
                        activeCharacter.sprite.addSpriteObserver(talk_finnished);
                        activeCharacter.sprite.move(activeCharacter.boardMovementPath);
                        talk(activeCharacter.sprite,
                             Utilities.charac4id(characterID) + result,
                             talk_finnished);
                    }
                }, true);

        final DelayedSoundListener equalSL = new DelayedSoundListener(
                taskQueue, delay, new Runnable() {
                    public void run() {
                        talk(activeCharacter.sprite,
                             Utilities.charac4id(characterID) + "equals",
                             resultSL);
                    }
                }, true);

        final SoundListener num2SL = new DelayedSoundListener(taskQueue, delay,
                new Runnable() {
                    public void run() {
                        if (!addition) {
                            getGameArea().setHnum(-1);
                        }
                        getGameArea().setDnum(num1, result);
                        getGameArea().setHint((int) Math.ceil((double) (result + num1) / 2),
                                              addition ? "+" + num2 : "-"
                                                      + num2);
                        talk(activeCharacter.sprite,
                             Utilities.charac4id(characterID) + num2,
                             equalSL);
                    }
                }, true);

        DelayedSoundListener signSL = new DelayedSoundListener(taskQueue,
                delay, new Runnable() {
                    public void run() {
                        talk(activeCharacter.sprite,
                             Utilities.charac4id(characterID)
                                     + (addition ? "plus" : "minus"),
                             num2SL);
                    }
                }, true);

        getGameArea().setDnum(-1, -1);
        getGameArea().setHnum(num1);
        talk(activeCharacter.sprite,
             Utilities.charac4id(characterID) + num1,
             signSL);
    }

    public void pause() {
        _metamgr.setPaused(true);
    }

    public void unpause() {
        _metamgr.setPaused(false);
    }

}
