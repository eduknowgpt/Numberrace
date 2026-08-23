/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.unicog.numberrace.listener;

import org.unicog.numberrace.algorithms.GameTurn;

/**
 *
 * @author tero
 */
public interface GameListener {

    public void successfullSneak();

    public GameTurn turnBegins();

    public void imFast(int leftrigth);

    public void playerWins(int PLAYER1);

    public void gameBegins();

}
