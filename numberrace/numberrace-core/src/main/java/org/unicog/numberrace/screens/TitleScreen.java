package org.unicog.numberrace.screens;

import java.util.logging.Level;
import java.util.logging.Logger;
import static org.unicog.numberrace.screens.ScaleUtils.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.GameObject.GameStates;
import org.unicog.numberrace.animations.ScrollImageAnimation;
import org.unicog.numberrace.setup.GamePreferences;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.Messages;
import org.unicog.numberrace.util.MyEditorKit;
import org.unicog.numberrace.util.ResourceProvider;
import org.unicog.numberrace.util.Resources;
import org.unicog.numberrace.util.Utilities;

import com.samskivert.swing.util.SwingUtil;
import com.threerings.media.FrameManager;
import com.threerings.media.SafeScrollPane;
import com.threerings.media.VirtualMediaPanel;
import com.threerings.media.animation.Animation;
import com.threerings.media.animation.AnimationObserver;
import com.threerings.media.image.BufferedMirage;
import java.awt.BasicStroke;
import java.io.BufferedReader;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class TitleScreen extends VirtualMediaPanel implements Screen {

    private static final int CREDIT_WINDOW_HEIGHT = 140;

    private JTextPane tpCredits;
    private JTextPane tpClick;
    private JLabel namelabel;
    private Runnable crRunnable;
    private URL copyr;

    private long startTimeStamp = -1;
    protected Animation creditsAnimation;
    private String versionStr;

    private final Rectangle startRect;
    private final Rectangle closeRect;
    private final Rectangle infoRect;
    private final Rectangle creditsRect;
    private final Rectangle popupRect;

    private SafeScrollPane infoPane;
    private SafeScrollPane creditPane;

    // PopUps
    private JPanel creditPanel;

    public TitleScreen(FrameManager framemgr) {
        super(framemgr);

        setLayout(null);

        creditPanel = new JPanel() {

            public void paint(Graphics g) {

                Graphics2D g2d = (Graphics2D) g;
                super.paint(g2d);

                g2d.setFont(new Font("Arial", Font.BOLD, 14));

                URL credits = ResourceProvider.getResource(Resources.getString("CREDITS"));
                try {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(credits.openStream()));

                    String inputLine;
                    int y = 20;
                    int x = 60;

                    while ((inputLine = in.readLine()) != null) {
                        y = y + 20;

                        if (y > 540) {
                            y = 40;
                            x = 340;
                        }

                        g2d.drawString(inputLine, x, y);
                    }

                    in.close();

                } catch (IOException ex) {
                    Logger.getLogger(TitleScreen.class.getName())
                          .log(Level.SEVERE, null, ex);
                }

                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(Color.BLACK);
                g2d.draw(popupRect);
                g2d.drawLine(popupRect.x, popupRect.y, popupRect.x
                        + popupRect.width, popupRect.y + popupRect.height);
                g2d.drawLine(popupRect.x,
                             popupRect.y + popupRect.height,
                             popupRect.x + popupRect.width,
                             popupRect.y);

            }
        };

        creditPanel.setBounds(new Rectangle(0, 100, 600, 600));
        creditPanel.setBackground(new Color(194, 156, 241));
        creditPanel.setVisible(false);

        popupRect = translateRect(new Rectangle(560, 20, 20, 20));

        startRect = translateRect(new Rectangle(125 - 15, 375 + 97, 145 + 15,
                90 + 60));

        closeRect = translateRect(new Rectangle(16, 698, 62, 56));

        creditsRect = translateRect(new Rectangle(850 + 78, 698 - 46, 62, 56));

        infoRect = translateRect(new Rectangle(850, 698 - 46, 62, 56));

        tpCredits = new JTextPane();
        tpCredits.setEditable(false);
        tpCredits.setOpaque(false);
        tpCredits.setFocusable(false);
        tpCredits.setForeground(Color.BLACK);
        tpCredits.setFont(tpCredits.getFont().deriveFont(Font.BOLD, f(22.f)));
        //tpCredits.setText(ResourceProvider.getResource(Resources.getString("CREDITS")).toString());

        //creditPanel.add(tpCredits);

        try {
            tpCredits.setEditorKit(new MyEditorKit());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        tpClick = new JTextPane();
        tpClick.setEditable(false);
        tpClick.setOpaque(false);
        tpClick.setFocusable(false);
        tpClick.setForeground(Color.BLACK);
        tpClick.setFont(tpClick.getFont().deriveFont(Font.BOLD, f(24.f)));
        tpClick.setBounds(translateRect(new Rectangle(35, 555, 290, 85)));

        try {
            tpClick.setEditorKit(new MyEditorKit());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        namelabel = new JLabel(Messages.getString("Game.numberRace")); //$NON-NLS-1$
        namelabel.setFont(new Font("Arial Black", Font.PLAIN, i(54))); //$NON-NLS-1$
        namelabel.setSize(namelabel.getPreferredSize());
        namelabel.setLocation(translatePoint(new Point(15, 15)));

        namelabel.setVisible(false);
        tpClick.setVisible(false);

        this.add(namelabel);
        this.add(tpClick);

        //        JButton pauseButton = new JButton("PAUSE");
        //        pauseButton.setBounds(50, 50, 50, 10);
        //        pauseButton.addActionListener(new ActionListener() {
        //            public void actionPerformed(ActionEvent e) {
        //                //             _metamgr.setPaused(!_metamgr.isPaused());
        //                GameObject.getInstance().play();
        //            }
        //        });
        //        this.add(pauseButton);

        crRunnable = new Runnable() {

            public void run() {
                try {
                    Reader r = new InputStreamReader(copyr.openStream(),
                            "UTF-8");

                    tpCredits.read(r, copyr);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                SimpleAttributeSet as = new SimpleAttributeSet();
                StyleConstants.setAlignment(as, StyleConstants.ALIGN_CENTER);
                StyledDocument sd = (StyledDocument) tpCredits.getDocument();
                sd.setParagraphAttributes(0, sd.getLength() - 1, as, false);
                tpCredits.setSize(tpCredits.getPreferredSize());

                sd = (StyledDocument) tpClick.getDocument();
                try {
                    sd.remove(0, sd.getLength());
                    sd.insertString(0,
                                    Messages.getString("CLICK_ONE2START"), as); //$NON-NLS-1$
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
                sd.setParagraphAttributes(0, sd.getLength() - 1, as, false);
            }
        };

        addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (startRect.contains(e.getX(), e.getY())) {
                    e.consume();
                    GameObject.getInstance()
                              .changeState(GameStates.REGISTRATION);
                } else if (closeRect.contains(e.getX(), e.getY())) {
                    System.exit(0);
                } else if (creditsRect.contains(e.getX(), e.getY())) {
                    JOptionPane.showInternalMessageDialog(TitleScreen.this.getParent(),
                                                          getCreditPane(),
                                                          "",
                                                          JOptionPane.INFORMATION_MESSAGE);
                    //creditPanel.setVisible(true);
                    //repaint();
                } else if (infoRect.contains(e.getX(), e.getY())) {
                    JOptionPane.showInternalMessageDialog(TitleScreen.this.getParent(),
                                                          getInfoPane(),
                                                          "",
                                                          JOptionPane.INFORMATION_MESSAGE);
                }
                /*
                else if (popupRect.contains(e.getX() - (1024-600)/2, e.getY() - 100)) {
                    creditPanel.setVisible(false);
                    repaint();
                }
                 *
                 */
            }
        });

    }

    protected Object getInfoPane() {
        if (infoPane == null) {
            JTextPane tp = new JTextPane();
            tp.setEditable(false);
            tp.setFocusable(false);
            infoPane = new SafeScrollPane(tp);
            infoPane.setPreferredSize(new Dimension(500, 300));
            URL regInfo = ResourceProvider.getResource(Resources.getString("REG_SCREEN_INFO")); //$NON-NLS-1$

            if (regInfo != null) {
                try {
                    Reader r = new InputStreamReader(regInfo.openStream(),
                            "UTF-8");
                    tp.read(r, regInfo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                tp.setText(null);
            }
        }
        return infoPane;
    }

    protected Object getCreditPane() {
        if (creditPane == null) {
            JTextPane tp = new JTextPane();
            tp.setEditable(false);
            tp.setFocusable(false);
            creditPane = new SafeScrollPane(tp);
            creditPane.setPreferredSize(new Dimension(500, 300));
            URL regInfo = ResourceProvider.getResource(Resources.getString("CREDITS")); //$NON-NLS-1$

            if (regInfo != null) {
                try {
                    Reader r = new InputStreamReader(regInfo.openStream(),
                            "UTF-8");
                    tp.read(r, regInfo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                tp.setText(null);
            }
        }
        return creditPane;
    }

    private void loadCredits() {
        copyr = ResourceProvider.getResource(Resources.getString("CREDITS")); //$NON-NLS-1$
        if (copyr != null) {
            Utilities.log.info(copyr.toString());

            if (SwingUtilities.isEventDispatchThread()) {
                crRunnable.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(crRunnable);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            if (tpCredits.getWidth() > 0 && tpCredits.getHeight() > 0) {
                int width = tpCredits.getWidth();

                BufferedImage creditsImage = ImageFactory.createCompatibleTranslucentImage(width,
                                                                                           tpCredits.getHeight());

                //TODO: if creditsImage.height >= view.height we do not need animation - it's just image (sprite maybe);
                Graphics2D g2D = creditsImage.createGraphics();
                g2D.addRenderingHints(Utilities.antialiasRH);
                tpCredits.paint(g2D);
                g2D.dispose();

                Rectangle scrollCreditsView = new Rectangle(
                        _background.getWidth() - 4 - creditsImage.getWidth(),
                        i(560), creditsImage.getWidth(),
                        i(CREDIT_WINDOW_HEIGHT));
                creditsAnimation = new ScrollImageAnimation(new BufferedMirage(
                        creditsImage), scrollCreditsView);
                creditsAnimation.addAnimationObserver(new AnimationObserver() {

                    public void animationCompleted(Animation anim, long when) {
                        if (creditsAnimation != null) {
                            creditsAnimation.reset();
                            addAnimation(creditsAnimation);
                        }
                    }

                    public void animationStarted(Animation anim, long when) {

                    }

                });
            } else {
                creditsAnimation = null;
            }
        }
    }

    public void tick(long tickStamp) {
        if (!_metamgr.isPaused()) {
            super.tick(tickStamp);

            if (startTimeStamp < 0) {
                startTimeStamp = tickStamp;
            }

            if (!namelabel.isVisible() && tickStamp - startTimeStamp >= 2400) {
                tpClick.setVisible(true);
                namelabel.setVisible(true);
                //                dirtyScreenRect(tpClick.getBounds());
                //                dirtyScreenRect(namelabel.getBounds());
            }
        }
    }

    @Override
    protected void paintBehind(Graphics2D gfx, Rectangle dirtyRect) {
        super.paintBehind(gfx, dirtyRect);
        gfx.drawString(versionStr, 3, getHeight() - 3);
    }

    protected void paintBetween(Graphics2D gfx, Rectangle dirtyRect) {
        super.paintBetween(gfx, dirtyRect);
        super.paintComponents(gfx);
    }

    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Object antiAliasing = SwingUtil.activateAntiAliasing(g2d);
        super.paint(g2d);
        SwingUtil.restoreAntiAliasing(g2d, antiAliasing);
        //g2d.draw(startRect);
        //g2d.draw(closeRect);
        //g2d.draw(infoRect);
        //g2d.draw(creditsRect);

        if (creditPanel.isVisible())
            creditPanel.paint(g2d.create((1024 - 600) / 2, 100, 600, 600));

    }

    //    public Dimension getPreferredSize() {at org.unicog.numberrace.screens.TitleScreen.load(TitleScreen.java:265)
    //        return wSize;
    //        //        return new Dimension(GraphicsVariables.DISPLAY_WIDTH,
    ////                GraphicsVariables.DISPLAY_HEIGHT);
    //    }

    public void load() {
        if (_background == null) {
            setBackground(new BufferedMirage(
                    ImageFactory.getImage("resources/images/mainPage.png")));
        }
        //loadCredits();
        versionStr = GamePreferences.getVersionStr();
    }

    public void start() {
        if (creditsAnimation != null) {
            addAnimation(creditsAnimation);
        }

        GameObject.getInstance().getSoundManager().play("introMusic", true);
    }

    public void stop() {
        clearAnimations();
    }

    public void unload() {
        _background = null;
        creditsAnimation = null;
    }

    public void pause() {
        _metamgr.setPaused(true);
    }

    public void unpause() {
        _metamgr.setPaused(false);
    }
}
