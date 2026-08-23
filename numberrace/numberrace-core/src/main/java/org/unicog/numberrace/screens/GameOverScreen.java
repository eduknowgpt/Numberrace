package org.unicog.numberrace.screens;

import static org.unicog.numberrace.screens.ScaleUtils.i;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashMap;
import java.util.Map;

import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.NRRunnableQueue;
import org.unicog.numberrace.GameObject.GameStates;
import org.unicog.numberrace.managers.NumCompManager;
import org.unicog.numberrace.sound.SoundListener;
import org.unicog.numberrace.sound.SoundManager;
import org.unicog.numberrace.sprites.ImageButtonSprite;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.Messages;
import org.unicog.numberrace.util.Resources;
import org.unicog.numberrace.util.Utilities;
import org.unicog.numberrace.vars.GraphicsVariables;

import com.threerings.media.FrameManager;
import com.threerings.media.VirtualMediaPanel;
import com.threerings.media.image.BufferedMirage;
import com.threerings.media.sprite.ImageSprite;

public class GameOverScreen extends VirtualMediaPanel implements Screen {

    private StringBuffer lblInfoPrompt;
    private Color lblInfoPromptColor;
    private final Color TEXT_COLOR = new Color(0, 0, 0); //black
    Font promptFont;

    private final Map<Attribute, Object> map = new HashMap<Attribute, Object>();

    private AttributedString text1;
    // private AttributedString text2;
    private AttributedString text3;

    private ImageSprite nextScreen;
    private GameObject go;

    public GameOverScreen(FrameManager framemgr) {
        super(framemgr);
        map.put(TextAttribute.FONT, new Font("Arial Black", Font.PLAIN, i(26))); //$NON-NLS-1$
        promptFont = new Font("Arial Black", Font.PLAIN, i(32)); //$NON-NLS-1$

        go = GameObject.getInstance();
    }

    public void load() {
        if (_background == null) {
            setBackground(new BufferedMirage(
                    ImageFactory.getImage(GameObject.getInstance().getTheme().gameOverScreenBkgrd)));
        }
        NumCompManager ncm = go.getNumCompManager();

        BufferedImage image = ImageFactory.getImage(Resources.getString("scrSwitchButton"));
        nextScreen = new ImageButtonSprite(new BufferedMirage(image),
                "changeState", (ncm.getlastWinner() == Constants.PLAYER1)
                        ? GameStates.SAVE
                        : GameStates.THEME);
        nextScreen.setLocation(i(GraphicsVariables.SCREENSWITCH_BUTTON_X)
                                       - (image.getWidth() >> 1),
                               i(GraphicsVariables.SCREENSWITCH_BUTTON_Y)
                                       - (image.getHeight() >> 1));

        //add labels
        lblInfoPrompt = new StringBuffer(100);
        lblInfoPromptColor = new Color(0, 0, 0); //black
        lblInfoPrompt.delete(0, lblInfoPrompt.capacity());
        lblInfoPrompt.append(Messages.getString("LangVars.GAMEOVER_GAMEOVER"));

        if (ncm.getlastWinner() == Constants.PLAYER1) {
            text1 = new AttributedString(
                    Messages.getString("LangVars.GAMEOVER_WONGAME"), map);
        } else {
            text1 = new AttributedString(
                    Messages.getString("LangVars.GAMEOVER_LOSTGAME"), map);
        }

        //        if (ncm.getNumGamesWon() == 1) {
        //            text2 = new AttributedString(
        //                    Messages.getString("LangVars.GAMEOVER_YOUVEWON")
        //                            + ncm.getNumGamesWon()
        //                            + Messages.getString("LangVars.GAMEOVER_GAMESOFSING")
        //                            + ncm.getNumGamesPlayed(), map);
        //        } else {
        //            text2 = new AttributedString(
        //                    Messages.getString("LangVars.GAMEOVER_YOUVEWON")
        //                            + ncm.getNumGamesWon()
        //                            + Messages.getString("LangVars.GAMEOVER_GAMESOFMULTI")
        //                            + ncm.getNumGamesPlayed(), map);
        //        }

        if (ncm.getlastWinner() == Constants.PLAYER1) {
            text3 = new AttributedString(
                    Messages.getString("LangVars.GAMEOVER_YOUCANRESCUE"), map);
        } else {
            text3 = new AttributedString(
                    Messages.getString("LangVars.GAMEOVER_TOPLAYAGAIN"), map);
        }
    }

    public void start() {
        //Play the sound script
        final SoundManager soundManager = go.getSoundManager();
        NRRunnableQueue tq = go.getTaskQueue();

        tq.addTask(new Runnable() {

            public void run() {
                soundManager.play("gameOver");
            }

        }, 1000);

        final NumCompManager ncm = go.getNumCompManager();
        if (ncm.getlastWinner() == Constants.PLAYER1) {
            tq.addTask(new Runnable() {

                public void run() {
                    soundManager.play("wellDone"); //$NON-NLS-1$
                }

            }, 2500);
        } else {
            tq.addTask(new Runnable() {

                public void run() {
                    soundManager.play("badLuck"); //$NON-NLS-1$
                }

            }, 2500);
        }

        //        if ((ncm.getNumGamesWon() <= 10) && (ncm.getNumGamesPlayed() <= 10)) {
        //            Utilities.log.info("Let's say it");
        //
        //            //make sure not to play numbers higher than we have sound files
        //            tq.addTask(new Runnable() {
        //
        //                public void run() {
        //                    soundManager.play("youHaveWon", new SoundListener() {
        //
        //                        public void run() {
        //                            // use numeric sound files
        //                            /*
        //                            soundManager.play("friend1_"
        //                                                      + Utilities.getVerbalForArabic(ncm.getNumGamesWon()),
        //                                              new SoundListener() {
        //                                                  public void run() {
        //                                                      soundManager.play("gamesOutOf",
        //                                                                        new SoundListener() {
        //                                                                            public void run() {
        //                                                                                soundManager.play("friend1_" + Utilities.getVerbalForArabic(ncm.getNumGamesPlayed())); //$NON-NLS-1$
        //                                                                            }
        //                                                                        }); //$NON-NLS-1$
        //                                                  }
        //                                              }); *///$NON-NLS-1$
        //                            soundManager.play("friend1_" + ncm.getNumGamesWon(),
        //                                              new SoundListener() {
        //
        //                                                  public void run() {
        //                                                      soundManager.play("gamesOutOf",
        //                                                                        new SoundListener() {
        //
        //                                                                            public void run() {
        //                                                                                soundManager.play("friend1_" + ncm.getNumGamesPlayed()); //$NON-NLS-1$
        //                                                                            }
        //                                                                        }); //$NON-NLS-1$
        //                                                  }
        //                                              }); //$NON-NLS-1$
        //                        }
        //                    }); //$NON-NLS-1$
        //                }
        //
        //            },
        //                       4700);
        //
        //            if (ncm.getlastWinner() == Constants.PLAYER1) {
        //                tq.addTask(new Runnable() {
        //
        //                    public void run() {
        //                        soundManager.play("youGetReward"); //$NON-NLS-1$
        //                    }
        //
        //                }, 8600);
        //            }
        //        } else {
        if (ncm.getlastWinner() == Constants.PLAYER1) {
            tq.addTask(new Runnable() {

                public void run() {
                    soundManager.play("youGetReward"); //$NON-NLS-1$
                }

            }, 4700);

        }
        //        }

        //        //game.soundManager.play(backgroundSoundtrack,true);
        //        if(Debugger.IN_USE){
        //            if(game.debugger.simulationMode){
        //                game.delay(2000,"game.simulator.clickScreenSwitchButton()");                     //$NON-NLS-1$
        //            }
        //        }
        addSprite(nextScreen);
    }

    @Override
    protected void paintBetween(Graphics2D gfx, Rectangle dirtyRect) {
        Graphics2D g2D = (Graphics2D) gfx.create();
        g2D.setFont(promptFont);
        g2D.setColor(lblInfoPromptColor);
        Utilities.drawFromPoint(lblInfoPrompt.toString(),
                                i(GraphicsVariables.DISPLAY_WIDTH / 2),
                                i(GraphicsVariables.DISPLAY_HEIGHT / 5),
                                0,
                                0,
                                g2D);

        g2D.setColor(TEXT_COLOR);

        //Write the instructions
        int yPos;
        yPos = Utilities.writeParagraph(g2D,
                                        i((2 * GraphicsVariables.DISPLAY_WIDTH) / 3),
                                        text1,
                                        i(GraphicsVariables.DISPLAY_WIDTH / 6),
                                        i(GraphicsVariables.DISPLAY_HEIGHT / 3));
        //        yPos = Utilities.writeParagraph(g2D,
        //                                        i((2 * GraphicsVariables.DISPLAY_WIDTH) / 3),
        //                                        text2,
        //                                        i(GraphicsVariables.DISPLAY_WIDTH / 6),
        //                                        yPos);
        yPos = Utilities.writeParagraph(g2D,
                                        i((2 * GraphicsVariables.DISPLAY_WIDTH) / 3),
                                        text3,
                                        i(GraphicsVariables.DISPLAY_WIDTH / 6),
                                        yPos);

        //        if(Debugger.IN_USE){
        //            if(game.debugger.translation==true){
        //            //in translation mode, display the translation label
        //            g2D.setFont(game.themeVars.lblTransFont);
        //            g2D.setColor(game.themeVars.lblTransColor);
        //                Utilities.drawFromPoint(lblTranslation.toString(),GraphicsVariables.LBL_TRANS_X,GraphicsVariables.LBL_TRANS_Y,0,0, g);                  
        //            }                   
        //        }

        g2D.dispose();
    }

    public void stop() {
        removeSprite(nextScreen);
    }

    public void unload() {
        // TODO Auto-generated method stub
        _background = null;
        nextScreen = null;
        lblInfoPrompt = null;
        lblInfoPromptColor = null;
        text1 = null;
        //        text2 = null;
        text3 = null;
    }

    public void pause() {
        _metamgr.setPaused(true);
    }

    public void unpause() {
        _metamgr.setPaused(false);
    }

}
