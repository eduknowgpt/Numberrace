package org.unicog.numberrace.screens;

import static org.unicog.numberrace.screens.ScaleUtils.i;

import java.awt.Point;
import java.util.Random;

import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.GameObject.GameStates;
import org.unicog.numberrace.sound.SoundListener;
import org.unicog.numberrace.sprites.RewardSprite;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.RewardPath;
import org.unicog.numberrace.util.Utilities;
import org.unicog.numberrace.vars.GraphicsVariables;
import org.unicog.numberrace.vars.ThemeVariables;

import com.samskivert.swing.Controller;
import com.samskivert.swing.ControllerProvider;
import com.threerings.media.FrameManager;
import com.threerings.media.VirtualMediaPanel;
import com.threerings.media.image.BufferedMirage;

public class SaveScreen extends VirtualMediaPanel implements Screen,
        ControllerProvider {

    public boolean saved;

    public final class Commander extends Controller {
        public void saved(Object src, final int rewardType) {
            if (!saved) {
                saved = true;
                go.getTaskQueue().postRunnable(new Runnable() {
                    public void run() {
                        go.getStudent().addReward(rewardType,
                                                  ThemeVariables.getLevel());
                        go.getSoundManager().play("iconClick");
                        go.getDataFileHandler().saveGame();
                        go.changeState(GameStates.ZOO);
                    }
                });
            }
        }
    }

    private GameObject go;
    private RewardSprite rewardSprite;
    private Random randomNumber;
    private int xCorrection;
    private int yCorrection;
    private int minx;
    private int maxx;
    private int miny;
    private int maxy;
    private Controller controller;

    public SaveScreen(FrameManager framemgr) {
        super(framemgr);
        go = GameObject.getInstance();
    }

    public void load() {
        saved = false;

        if (_background == null) {
            setBackground(new BufferedMirage(
                    ImageFactory.getImage(GameObject.getInstance().getTheme().rewardScreenBkgrd)));

        }
        ThemeVariables theme = go.getTheme();
        int rewardChosen = go.getStudent()
                             .getNextRewardEasy(ThemeVariables.getLevel());
        String[] rewardFiles = new String[2];
        if (rewardChosen == 0)
            rewardFiles = theme.reward1;
        else if (rewardChosen == 1)
            rewardFiles = theme.reward2;
        else if (rewardChosen == 2)
            rewardFiles = theme.reward3;
        else if (rewardChosen == 3)
            rewardFiles = theme.reward4;
        else if (rewardChosen == 4)
            rewardFiles = theme.reward5;
        else if (rewardChosen == 5)
            rewardFiles = theme.reward6;
        else if (rewardChosen == 6)
            rewardFiles = theme.reward7;

        randomNumber = new Random(System.currentTimeMillis());

        //        rewardSprite = new RewardSprite(game,this,rewardLocation,rewardFiles,rewardChosen,
        //            GraphicsVariables.REWARD_CHOICE_SPRITE_MINX, GraphicsVariables.REWARD_CHOICE_SPRITE_MAXX,
        //            GraphicsVariables.REWARD_CHOICE_SPRITE_MINY, GraphicsVariables.REWARD_CHOICE_SPRITE_MAXY);

        //        mirages = new Mirage[] {
        //                new BufferedMirage(ImageFactory.getImage(rewardFiles[0])),
        //                new BufferedMirage(ImageFactory.getImage(rewardFiles[1])) };
        //        MultiFrameImageImpl multiFrameImageImpl = new MultiFrameImageImpl(
        //                mirages);
        //        rewardSprite.setFrames(multiFrameImageImpl);
        rewardSprite = new RewardSprite(rewardFiles, "saved", rewardChosen);

        //        side = randomNumber.nextInt(2);

        //        rewardSprite.setMirage(mirages[side]);
        xCorrection = rewardSprite.getWidth();
        yCorrection = rewardSprite.getHeight();
        Utilities.log.info("xc :" + xCorrection + " yc :" + yCorrection);

        minx = i(GraphicsVariables.REWARD_CHOICE_SPRITE_MINX);
        maxx = i(GraphicsVariables.REWARD_CHOICE_SPRITE_MAXX) - xCorrection;
        miny = i(GraphicsVariables.REWARD_CHOICE_SPRITE_MINY);
        maxy = i(GraphicsVariables.REWARD_CHOICE_SPRITE_MAXY) - yCorrection;

        Point rewardLocation = new Point(randomNumber.nextInt(maxx - minx)
                + minx, randomNumber.nextInt(maxy - miny) + miny);

        rewardSprite.setLocation(rewardLocation.x, rewardLocation.y);

        //        SmoothBobblePath bobblePath = new SmoothBobblePath(
        ////                GraphicsVariables.REWARD_CHOICE_SPRITE_MINX,
        ////                GraphicsVariables.REWARD_CHOICE_SPRITE_MINY,
        //                GraphicsVariables.REWARD_CHOICE_SPRITE_MAXX - GraphicsVariables.REWARD_CHOICE_SPRITE_MINX + 1,
        //                GraphicsVariables.REWARD_CHOICE_SPRITE_MAXY - GraphicsVariables.REWARD_CHOICE_SPRITE_MINY + 1, 10000, 2);

        //        animation = new SpriteAnimation(_spritemgr, rewardSprite, bobblePath);
        //        rewardSprite.setLocation(500,500);

    }

    public void start() {
        go.getSoundManager().play("takeReward", new SoundListener() {

            public void run() {
                go.getSoundManager().play("rewardMusic", true);
            }
        });

        addSprite(rewardSprite);
        rewardSprite.move(new RewardPath(minx, miny, maxx, maxy));
    }

    public void stop() {
        removeSprite(rewardSprite);
        setViewLocation(0, 0);
        invalidate();
    }

    public void unload() {
        _background = null;
        rewardSprite = null;
        randomNumber = null;
    }

    public Controller getController() {
        if (controller == null) {
            controller = new Commander();
        }
        return controller;
    }

    public void pause() {
        _metamgr.setPaused(true);
    }

    public void unpause() {
        _metamgr.setPaused(false);
    }

}
