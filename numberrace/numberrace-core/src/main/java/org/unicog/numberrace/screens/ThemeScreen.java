package org.unicog.numberrace.screens;

import static org.unicog.numberrace.screens.ScaleUtils.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingConstants;

import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.GameObject.GameStates;
import org.unicog.numberrace.media_patched.LabelSpritePatched;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.Messages;
import org.unicog.numberrace.vars.GraphicsVariables;
import org.unicog.numberrace.vars.ThemeVariables;

import com.samskivert.swing.Label;
import com.threerings.media.FrameManager;
import com.threerings.media.MediaPanel;
import com.threerings.media.image.BufferedMirage;
import com.threerings.media.sprite.ImageSprite;
import com.threerings.media.sprite.LabelSprite;

public class ThemeScreen extends MediaPanel implements Screen {

    private ImageSprite uwSprite;
    private ImageSprite ijSprite;
    private LabelSprite labelSprite;
    private boolean loaded;
    private boolean started;
    private boolean respond;
    private final GameObject go;
    private final int VGAP;

    public ThemeScreen(FrameManager framemgr) {
        super(framemgr);
        go = GameObject.getInstance();
        setBackground(new Color(0, 255, 255)); //light blue
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (respond
                        && started
                        && (uwSprite.hitTest(e.getX(), e.getY()) || (ijSprite.hitTest(e.getX(),
                                                                                      e.getY())))) {
                    go.getSoundManager().play("iconClick");
                    respond = false;
                    e.consume();

                    if (uwSprite.contains(e.getX(), e.getY())) {
                        go.setTheme(ThemeVariables.UNDER_THE_SEA);
                    } else if (ijSprite.contains(e.getX(), e.getY())) {
                        go.setTheme(ThemeVariables.IN_THE_JUNGLE);
                    }
                    go.changeState(GameStates.INSTRUCTIONS);
                }
            }
        });
        VGAP = i(10);
    }

    public void load() {
        if (!loaded) {
            uwSprite = new ImageSprite(
                    new BufferedMirage(
                            ImageFactory.getImage("resources/images/underWater/theme_thumbnail.gif")));
            ijSprite = new ImageSprite(
                    new BufferedMirage(
                            ImageFactory.getImage("resources/images/inJungle/theme_thumbnail.gif")));
            Label label = new Label(
                    Messages.getString("LangVars.THEME_CHOICE_TITLE"));
            label.setTargetWidth(i(GraphicsVariables.DISPLAY_WIDTH));
            label.setAlignment(SwingConstants.CENTER);
            label.setFont(go.getTheme().characChoiceTitleFont);

            labelSprite = new LabelSpritePatched(label);
            labelSprite.setAntiAliased(true);
            loaded = true;
        }
    }

    public void start() {
        go.getSoundManager().play("chooseTheme");

        addSprite(uwSprite);
        addSprite(ijSprite);
        addSprite(labelSprite);

        labelSprite.setLocation((this.getWidth() - labelSprite.getWidth()) / 2,
                                i(50));

        int contentY = (labelSprite.getY() + labelSprite.getHeight());

        int contentHeight = (this.getHeight() - contentY);
        int totalHeights = uwSprite.getHeight() + ijSprite.getHeight() + VGAP;

        contentY += (contentHeight - totalHeights) / 2;

        uwSprite.setLocation((this.getWidth() - uwSprite.getWidth()) / 2,
                             contentY);
        ijSprite.setLocation((this.getWidth() - ijSprite.getWidth()) / 2,
                             contentY + uwSprite.getHeight() + VGAP);
        respond = started = true;
    }

    public void stop() {
        started = false;
        removeSprite(uwSprite);
        removeSprite(ijSprite);
        removeSprite(labelSprite);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setBackground(getBackground());
        super.paint(g2d);
    }

    @Override
    protected void paintBehind(Graphics2D gfx, Rectangle dirtyRect) {
        super.paintBehind(gfx, dirtyRect);
        gfx.clearRect(dirtyRect.x,
                      dirtyRect.y,
                      dirtyRect.width,
                      dirtyRect.height);
    }

    //    @Override
    //    public Dimension getPreferredSize() {
    //        return new Dimension(GraphicsVariables.DISPLAY_WIDTH,
    //                GraphicsVariables.DISPLAY_HEIGHT);
    //    }

    public void unload() {

    }

    public void pause() {
        _metamgr.setPaused(true);
    }

    public void unpause() {
        _metamgr.setPaused(false);
    }

}
