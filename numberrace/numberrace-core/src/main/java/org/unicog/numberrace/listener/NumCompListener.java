/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.unicog.numberrace.listener;

import org.unicog.numberrace.GameObject.GameStates;
import org.unicog.numberrace.algorithms.GameTurn;
import org.unicog.numberrace.data.DataFileHandler;
import org.unicog.numberrace.data.Student;
import org.unicog.numberrace.others.DotArray;
import org.unicog.numberrace.screens.ActionState;
import org.unicog.numberrace.screens.Player;
import org.unicog.numberrace.sprites.DotContainerSprite;

/**
 *
 * @author tero
 */
public abstract class NumCompListener {
    
    public abstract DotArray[] getDots();

    public abstract void setActionState(ActionState actionState);

    public abstract void setCurrentTurn(GameTurn currentTurn);

    public abstract DotContainerSprite getDotContainer(int additionSide);

    public abstract void setHazardLevel(boolean hazards);

    public abstract void setHazards(int rangeCeilling, boolean gameBeginning);

    public abstract void openContainer(int LEFT);

    public abstract void startSneaking(long l);

    public abstract void opponentTalks(String string);

    public abstract void openContainer(int sideSelected, boolean b);

    public abstract void grabAndLineUpDotsOnCarpet(int player, int sideSelected, GameTurn currentTurn);

    public abstract void grabAndSubtractDots(int characterID, int side, GameTurn currentTurn);

    public abstract Player getPlayer(int PLAYER);

    public abstract void clearCarpet();

    public abstract Student getStudent();

    public abstract void changeState(GameStates gameStates);

    public abstract DataFileHandler getDataFileHandler();
    
    public abstract void setBoardLength(int length);

}
