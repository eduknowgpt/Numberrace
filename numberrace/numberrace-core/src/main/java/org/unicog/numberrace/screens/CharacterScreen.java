package org.unicog.numberrace.screens;

import static org.unicog.numberrace.screens.ScaleUtils.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.data.Student;
import org.unicog.numberrace.sound.SoundListener;
import org.unicog.numberrace.sprites.ImageButtonSprite;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.Messages;
import org.unicog.numberrace.util.Utilities;
import org.unicog.numberrace.vars.GraphicsVariables;
import org.unicog.numberrace.vars.ThemeVariables;

import com.samskivert.swing.util.SwingUtil;
import com.threerings.media.FrameManager;
import com.threerings.media.VirtualMediaPanel;
import com.threerings.media.image.BufferedMirage;
import com.threerings.media.image.Mirage;
import com.threerings.media.util.MultiFrameImageImpl;

public class CharacterScreen extends VirtualMediaPanel implements Screen {

    private ImageButtonSprite charac1Button;
    private final GameObject go;
    private ImageButtonSprite charac2Button;
    private ImageButtonSprite charac3Button;
    private ImageButtonSprite charac4Button;
    private ImageButtonSprite charac5Button;
    private ImageButtonSprite charac6Button;

    private String[] status = new String[6];
    private Color textColor;
    private Font characChoiceTitleFont;
    private Font characChoiceLabelFont;

    public CharacterScreen(FrameManager frameManager) {
        super(frameManager);
        go = GameObject.getInstance();
        translatePoint(GraphicsVariables.CHARAC_CHOICE_TITLE_LOC);
        translatePoint(GraphicsVariables.CHARAC_1_TITLE_LOC);
        translatePoint(GraphicsVariables.CHARAC_2_TITLE_LOC);
        translatePoint(GraphicsVariables.CHARAC_3_TITLE_LOC);
        translatePoint(GraphicsVariables.CHARAC_4_TITLE_LOC);
        translatePoint(GraphicsVariables.CHARAC_5_TITLE_LOC);
        translatePoint(GraphicsVariables.CHARAC_6_TITLE_LOC);
    }

    public void load() {
        if (_background == null) {
            setBackground(new BufferedMirage(
                    ImageFactory.getImage(go.getTheme().characChoiceBackground)));
        }
        charac1Button = new ImageButtonSprite(
                new MultiFrameImageImpl(
                        new Mirage[] {
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_1_files[0])),
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_1_files[1])) }),
                "chooseCharacter", 0);

        charac2Button = new ImageButtonSprite(
                new MultiFrameImageImpl(
                        new Mirage[] {
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_2_files[0])),
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_2_files[1])) }),
                "chooseCharacter", 1);
        charac3Button = new ImageButtonSprite(
                new MultiFrameImageImpl(
                        new Mirage[] {
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_3_files[0])),
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_3_files[1])) }),
                "chooseCharacter", 2);
        charac4Button = new ImageButtonSprite(
                new MultiFrameImageImpl(
                        new Mirage[] {
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_4_files[0])),
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_4_files[1])) }),
                "chooseCharacter", 3);
        charac5Button = new ImageButtonSprite(
                new MultiFrameImageImpl(
                        new Mirage[] {
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_5_files[0])),
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_5_files[1])) }),
                "chooseCharacter", 4);
        charac6Button = new ImageButtonSprite(
                new MultiFrameImageImpl(
                        new Mirage[] {
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_6_files[0])),
                                new BufferedMirage(
                                        ImageFactory.getImage(go.getTheme().charac_6_files[1])) }),
                "chooseCharacter", 5);

        int col1 = i(150);
        int col2 = i(550);
        int row1 = i(180);
        int row2 = i(330);
        int row3 = i(480);

        charac1Button.setLocation(col1, row1);
        charac2Button.setLocation(col1, row2);
        charac3Button.setLocation(col1, row3);

        charac4Button.setLocation(col2, row1);
        charac5Button.setLocation(col2, row2);
        charac6Button.setLocation(col2, row3);

        Student student = go.getStudent();
        ThemeVariables themeVars = go.getTheme();
        for (int i = 0; i < status.length; i++) {
            if (student.getCharacAccess(i, ThemeVariables.getLevel())) {
                status[i] = themeVars.charactLabel[i];
            } else {
                status[i] = Messages.getString("LangVars.LOCKED");
            }
        }

        textColor = themeVars.textColor;
        characChoiceTitleFont = themeVars.characChoiceTitleFont;
        characChoiceLabelFont = themeVars.characChoiceLabelFont;

    }

    public void start() {
        addSprite(charac1Button);
        addSprite(charac2Button);
        addSprite(charac3Button);
        addSprite(charac4Button);
        addSprite(charac5Button);
        addSprite(charac6Button);

        boolean characterUnlocked = go.getStudent()
                                      .checkForCharacterUnlock(ThemeVariables.getLevel());
        if (characterUnlocked) {
            go.getStudent().unlockNextCharacter(ThemeVariables.getLevel());
            Student student = go.getStudent();
            ThemeVariables themeVars = go.getTheme();
            for (int i = 0; i < status.length; i++) {
                if (student.getCharacAccess(i, ThemeVariables.getLevel())) {
                    status[i] = themeVars.charactLabel[i];
                }
            }
            go.getSoundManager().play("unlockedCharacter", new SoundListener() {

                public void run() {
                    go.getSoundManager().play("chooseCharacter"); //$NON-NLS-1$
                }

            }); //$NON-NLS-1$
        } else {
            go.getSoundManager().play("chooseCharacter");
        }
    }

    public void stop() {
        removeSprite(charac6Button);
        removeSprite(charac5Button);
        removeSprite(charac4Button);
        removeSprite(charac3Button);
        removeSprite(charac2Button);
        removeSprite(charac1Button);
    }

    @Override
    protected void paintBetween(Graphics2D gfx, Rectangle dirtyRect) {
        super.paintBetween(gfx, dirtyRect);
        Graphics2D g2D = (Graphics2D) gfx.create();
        //Draw the text
        SwingUtil.activateAntiAliasing(g2D);
        g2D.setColor(textColor);
        g2D.setFont(characChoiceTitleFont);
        Utilities.drawFromPoint(Messages.getString("LangVars.CHARAC_CHOICE_TITLE"),
                                GraphicsVariables.CHARAC_CHOICE_TITLE_LOC,
                                0,
                                -1,
                                g2D);
        g2D.setFont(characChoiceLabelFont);
        Utilities.drawFromPoint(status[0],
                                GraphicsVariables.CHARAC_1_TITLE_LOC,
                                -1,
                                -1,
                                g2D);
        Utilities.drawFromPoint(status[1],
                                GraphicsVariables.CHARAC_2_TITLE_LOC,
                                -1,
                                -1,
                                g2D);
        Utilities.drawFromPoint(status[2],
                                GraphicsVariables.CHARAC_3_TITLE_LOC,
                                -1,
                                -1,
                                g2D);
        Utilities.drawFromPoint(status[3],
                                GraphicsVariables.CHARAC_4_TITLE_LOC,
                                -1,
                                -1,
                                g2D);
        Utilities.drawFromPoint(status[4],
                                GraphicsVariables.CHARAC_5_TITLE_LOC,
                                -1,
                                -1,
                                g2D);
        Utilities.drawFromPoint(status[5],
                                GraphicsVariables.CHARAC_6_TITLE_LOC,
                                -1,
                                -1,
                                g2D);
        g2D.dispose();
    }

    public void unload() {
        _background = null;
        charac1Button = null;
        charac2Button = null;
        charac3Button = null;
        charac4Button = null;
        charac5Button = null;
        charac6Button = null;
    }

    public void pause() {
        _metamgr.setPaused(true);
    }

    public void unpause() {
        _metamgr.setPaused(false);
    }

}
