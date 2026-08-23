/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.unicog.numberrace.listener;

import org.unicog.numberrace.sprites.HazardSprite;

/**
 *
 * @author tero
 */
public interface HazardListener {

    public void play(String string);

    public void addHazard(HazardSprite hazardSprite);

}
