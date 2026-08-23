/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.unicog.numberrace.screens;

import com.threerings.media.animation.Animation;
import org.unicog.numberrace.sound.SoundManager;

/**
 *
 * @author tero
 */
public interface CountAnimationVariableContainer {

    public ActionState getCurrentState();

    public Player getActiveCharacter();

    public SoundManager getSoundManager();

    public void addAnimation(Animation a);

    public GameArea getGameArea();

    public void repaint();

}
