package org.unicog.numberrace.screens;

import static org.unicog.numberrace.screens.ScaleUtils.*;

import java.awt.AlphaComposite;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.GameObject.GameStates;
import org.unicog.numberrace.sprites.ImageButtonSprite;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.MyEditorKit;
import org.unicog.numberrace.util.ResourceProvider;
import org.unicog.numberrace.util.Resources;
import org.unicog.numberrace.vars.GraphicsVariables;

import com.samskivert.swing.util.SwingUtil;
import com.threerings.media.FrameManager;
import com.threerings.media.SafeScrollPane;
import com.threerings.media.VirtualMediaPanel;
import com.threerings.media.image.BufferedMirage;
import com.threerings.media.sprite.ImageSprite;

public class InstructionScreen extends VirtualMediaPanel implements Screen {

    private static final long INST_DELAY = 1500L;

    private final AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                                 0.45f);
    private final Rectangle square;

    private Runnable liRunnable;
    private JTextPane tp;
    private JScrollPane sp;
    private URL copyr;
    private long startStamp = -1L;

    private ImageSprite nextScreen;
    private final String resource;

    public InstructionScreen(FrameManager framemgr, String resource) {
        super(framemgr);

        this.resource = resource;

        square = translateRect(new Rectangle(54, 90, 909, 540));

        tp = new JTextPane();
        tp.setEditable(false);
        tp.setOpaque(false);
        tp.setFocusable(false);
        tp.setForeground(Color.BLACK);
        tp.setFont(tp.getFont().deriveFont(Font.BOLD, f(24.f)));

        sp = new SafeScrollPane(tp);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);

        sp.setBounds(square);
        sp.setVisible(false);

        this.add(sp);

        try {
            tp.setEditorKit(new MyEditorKit());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        liRunnable = new Runnable() {

            public void run() {
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
    }

    private void loadInstructions() {
        copyr = ResourceProvider.getResource(Resources.getString(resource)); //$NON-NLS-1$
        if (SwingUtilities.isEventDispatchThread()) {
            liRunnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(liRunnable);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public void load() {
        if (_background == null) {
            setBackground(new BufferedMirage(
                    ImageFactory.getImage(GameObject.getInstance().getTheme().instrucScreenBackground)));
        }

        BufferedImage image = ImageFactory.getImage(Resources.getString("scrSwitchButton"));
        nextScreen = new ImageButtonSprite(new BufferedMirage(image),
                "changeState", GameStates.CHARACTERS);
        nextScreen.setLocation(i(GraphicsVariables.SCREENSWITCH_BUTTON_X)
                                       - (image.getWidth() >> 1),
                               i(GraphicsVariables.SCREENSWITCH_BUTTON_Y)
                                       - (image.getHeight() >> 1));

        loadInstructions();
    }

    @Override
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

    public void start() {
        GameObject.getInstance().getTaskQueue().addTask(new Runnable() {

            public void run() {
                GameObject.getInstance()
                          .getSoundManager()
                          .play(GameObject.getInstance()
                                          .getTheme()
                                          .getInstructionsSoundKey());
            }

        },
                                                        INST_DELAY);
        addSprite(nextScreen);
    }

    public void stop() {
        removeSprite(nextScreen);
        sp.setVisible(false);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Object antiAliasing = SwingUtil.activateAntiAliasing(g2d);
        super.paint(g2d);
        SwingUtil.restoreAntiAliasing(g2d, antiAliasing);
    }

    @Override
    protected void paintBetween(Graphics2D gfx, Rectangle dirtyRect) {
        paintComponents(gfx);
        super.paintBetween(gfx, dirtyRect);
    }

    @Override
    protected void paintBehind(Graphics2D gfx, Rectangle dirtyRect) {
        super.paintBehind(gfx, dirtyRect);
        if (sp.isVisible() && dirtyRect.intersects(square)) {
            Graphics2D g2D = (Graphics2D) gfx.create();
            g2D.setComposite(ac);
            g2D.setPaint(Color.WHITE);
            g2D.fill(dirtyRect.intersection(square));
            g2D.dispose();
        }
    }

    public void unload() {
        _background = null;
        nextScreen = null;
        startStamp = -1;
    }

    public void pause() {
        _metamgr.setPaused(true);
    }

    public void unpause() {
        _metamgr.setPaused(false);
    }

}
