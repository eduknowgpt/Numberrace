package org.unicog.numberrace.screens;

import static org.unicog.numberrace.screens.ScaleUtils.i;
import static org.unicog.numberrace.screens.ScaleUtils.f;
import static org.unicog.numberrace.screens.ScaleUtils.translateRect;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Random;

import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.GameObject.GameStates;
import org.unicog.numberrace.sound.SoundListener;
import org.unicog.numberrace.sprites.ImageButtonSprite;
import org.unicog.numberrace.sprites.RewardSprite;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.MyEditorKit;
import org.unicog.numberrace.util.ResourceProvider;
import org.unicog.numberrace.util.Resources;
import org.unicog.numberrace.util.RewardPath;
import org.unicog.numberrace.vars.GraphicsVariables;
import org.unicog.numberrace.vars.ThemeVariables;

import com.samskivert.swing.util.SwingUtil;
import com.threerings.media.FrameManager;
import com.threerings.media.SafeScrollPane;
import com.threerings.media.VirtualMediaPanel;
import com.threerings.media.image.BufferedMirage;
import com.threerings.media.sprite.ImageSprite;
import com.threerings.media.sprite.Sprite;

public class ZooScreen extends VirtualMediaPanel implements Screen {

    protected static final Color ALPHA_COLOR = new Color(255, 255, 255, 150);

    private static final long INST_DELAY = 500;

    private GameObject go;
    private ImageSprite[] rewardSprites;
    private final int minx;
    private final int maxx;
    private final int miny;
    private final int maxy;
    private JTextPane tp;

    private Runnable ltRunnable;

    private final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                                 0.45f);
    private final Rectangle square;

    private ImageButtonSprite nextScreen;

    private long startStamp = -1L;
    private SafeScrollPane sp;

    public ZooScreen(FrameManager framemgr) {
        super(framemgr);

        minx = i(GraphicsVariables.REWARD_VIEW_SPRITE_MINX);
        maxx = i(GraphicsVariables.REWARD_VIEW_SPRITE_MAXX);
        miny = i(GraphicsVariables.REWARD_VIEW_SPRITE_MINY);
        maxy = i(GraphicsVariables.REWARD_VIEW_SPRITE_MAXY);
        square = translateRect(new Rectangle(35, 560, 570, 150));

        setLayout(null);
        go = GameObject.getInstance();

        tp = new JTextPane();

        tp.setMargin(new Insets(2, 2, 2, 2));
        tp.setEditable(false);
        tp.setOpaque(false);
        tp.setFocusable(false);
        tp.setForeground(Color.BLACK);
        tp.setFont(tp.getFont().deriveFont(Font.BOLD, f(20.f)));

        //        Utilities.ignoreRepaint(this);

        try {
            tp.setEditorKit(new MyEditorKit());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        ltRunnable = new Runnable() {

            public void run() {
                URL copyr = ResourceProvider.getResource(Resources.getString("CHAR_UNLOCK_CONDITION"));
                if (copyr != null) {
                    try {
                        Reader r = new InputStreamReader(copyr.openStream(),
                                "UTF-8");
                        tp.read(r, copyr);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    SimpleAttributeSet as = new SimpleAttributeSet();
                    StyleConstants.setAlignment(as, StyleConstants.ALIGN_CENTER);
                    StyledDocument sd = (StyledDocument) tp.getDocument();
                    sd.setParagraphAttributes(0, sd.getLength() - 1, as, false);

                } else {
                    tp.setText(null);
                }
            }
        };

        sp = new SafeScrollPane(tp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        sp.setBounds(square);
        sp.setVisible(false);
        this.add(sp);

    }

    public void load() {
        if (_background == null) {
            setBackground(new BufferedMirage(
                    ImageFactory.getImage(go.getTheme().rewardViewScreenBkgrd)));
        }
        loadRewards();

        BufferedImage image = ImageFactory.getImage(Resources.getString("scrSwitchButton"));
        nextScreen = new ImageButtonSprite(new BufferedMirage(image),
                "changeState", GameStates.THEME);
        nextScreen.setLocation(i(GraphicsVariables.REWARD_VIEW_BUTTON_X)
                                       - (image.getWidth() >> 1),
                               i(GraphicsVariables.REWARD_VIEW_BUTTON_Y)
                                       - (image.getHeight() >> 1));
        ltRunnable.run();
    }

    public void start() {

        go.getTaskQueue().addTask(new Runnable() {

            public void run() {
                go.getSoundManager().play("hereAreRewards",
                                          new SoundListener() {

                                              public void run() {
                                                  go.getSoundManager()
                                                    .play("rewardChoice", true);
                                              }
                                          });
            }

        },
                                  INST_DELAY);

        for (int i = 0; i < rewardSprites.length; i++) {
            Sprite rs = rewardSprites[i];
            addSprite(rs);
            rs.move(new RewardPath(minx, miny, maxx, maxy));
        }
        addSprite(nextScreen);

    }

    public void stop() {
        clearSprites();
        //        for (int i = 0; i < rewardSprites.length; i++) {
        //            removeSprite(rewardSprites[i]);
        //        }
        //        removeSprite(nextScreen);
    }

    public void loadRewards() {
        // find out the total number of rewards, and use this to dimension the
        // array
        ThemeVariables theme = go.getTheme();
        int numRewardSprites = go.getStudent()
                                 .getNumRewardsCollected(ThemeVariables.getLevel());
        rewardSprites = new ImageSprite[numRewardSprites];

        Random randomNumber = new Random(System.currentTimeMillis());
        // loop through the array, assigning all the reward
        String[] rewardFiles = new String[2];
        int count = 0;
        byte[] rewardIDArray = go.getStudent()
                                 .getRewardCount(ThemeVariables.getLevel());
        for (int j = 0; j < Constants.NUMBER_POSS_REWARDS; j++) {
            for (int i = 0; i < rewardIDArray[j]; i++) {
                if (j == 0)
                    rewardFiles = theme.reward1;
                else if (j == 1)
                    rewardFiles = theme.reward2;
                else if (j == 2)
                    rewardFiles = theme.reward3;
                else if (j == 3)
                    rewardFiles = theme.reward4;
                else if (j == 4)
                    rewardFiles = theme.reward5;
                else if (j == 5)
                    rewardFiles = theme.reward6;
                else if (j == 6)
                    rewardFiles = theme.reward7;

                RewardSprite sprite = new RewardSprite(rewardFiles, null, null);

                //                int xCorrection = sprite.getWidth() / 2;
                //                int yCorrection = sprite.getHeight() / 2;
                //                minx = GraphicsVariables.REWARD_VIEW_SPRITE_MINX + xCorrection;
                //                maxx = GraphicsVariables.REWARD_VIEW_SPRITE_MAXX - xCorrection;
                //                miny = GraphicsVariables.REWARD_VIEW_SPRITE_MINY - yCorrection;
                //                maxy = GraphicsVariables.REWARD_VIEW_SPRITE_MAXY - yCorrection;

                sprite.setLocation(randomNumber.nextInt(maxx),
                                   randomNumber.nextInt(maxy - miny) + miny);
                rewardSprites[count++] = sprite;
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Object antiAliasing = SwingUtil.activateAntiAliasing(g2d);
        super.paint(g2d);
        SwingUtil.restoreAntiAliasing(g2d, antiAliasing);
    }

    @Override
    protected void paintInFront(Graphics2D gfx, Rectangle dirtyRect) {
        super.paintInFront(gfx, dirtyRect);
        if (dirtyRect.intersects(square) && sp.isVisible()) {
            Graphics2D g2D = (Graphics2D) gfx.create();
            g2D.setComposite(ac);
            g2D.setPaint(Color.WHITE);
            g2D.fill(dirtyRect.intersection(square));
            g2D.dispose();
        }
        paintComponents(gfx);
    }

    public void unload() {
        _background = null;
        nextScreen = null;
        rewardSprites = null;
        startStamp = -1L;
    }

    public void tick(long tickStamp) {
        if (!_metamgr.isPaused()) {
            super.tick(tickStamp);
            if (startStamp < 0) {
                startStamp = tickStamp;
            }

            if (!sp.isVisible() && tickStamp - startStamp >= INST_DELAY) {
                sp.setVisible(true);
            }
        }
    }

    public void pause() {
        _metamgr.setPaused(true);
    }

    public void unpause() {
        _metamgr.setPaused(false);
    }

}
