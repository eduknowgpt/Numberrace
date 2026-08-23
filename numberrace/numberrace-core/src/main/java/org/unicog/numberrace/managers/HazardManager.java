//The Number Race: Remediation Software for dyscalculia.
//Copyright (C) Anna Wilson and Stanlislas Dehaene, 2004
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

/*
 *	This class manages the appearance of hazards on the board.
 *	It maintains an array of hazard objects whose length is 1/2 length of the board.
 *   Once hazards are placed they cannot be removed until the end of the game.
 * 	Hazards are only placed for the next 9 squares ahead of the player who is on the front on the board.
 *   Placement of new hazards each turn is determined by the relevant notation dimension parameters.
 */

package org.unicog.numberrace.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.unicog.numberrace.listener.HazardListener;
import org.unicog.numberrace.screens.Player;
import org.unicog.numberrace.sprites.HazardSprite;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.ImageFactory;
import org.unicog.numberrace.util.Utilities;
import org.unicog.numberrace.vars.ThemeVariables;

import com.threerings.media.image.BufferedMirage;
import com.threerings.media.image.Mirage;
import com.threerings.media.util.MultiFrameImage;
import com.threerings.media.util.MultiFrameImageImpl;

public class HazardManager {

    //properties
    private static final int MAX_NUM_HAZARDS = (int) (Constants.LAST_SQUARE / 2f); //max num hazards is 2/3 of board
    private int numHazards; //Total number of hazards currently on board
    private int numHazardsToAdd; //number of hazards to be added this turn
    //private int currentHazardLevel;
    private boolean hazardsPresent;
    public String[][] hazardFiles;

    //objects
    public HazardSprite[] hazards;

    //utility variables
    private Random randomNumberGenerator;
    //private ChoiceScreen choiceScreen;
    //private GameObject go;
    private Map<Integer, MultiFrameImage> cachedHazards;
    private HazardListener hazardListener;

    //private final SoundManager soundManager;

    public HazardManager(HazardListener hazardListener) {
        this.hazardListener = hazardListener;
    }

    public void load() {
        numHazards = 0;
        hazardFiles = ThemeVariables.hazardGifs;
        randomNumberGenerator = new Random();
        randomNumberGenerator.setSeed(System.currentTimeMillis()); //seed with system time in msec
        cachedHazards = new HashMap<Integer, MultiFrameImage>();
        createHazards();
        //		for(int i=0;i<MAX_NUM_HAZARDS;i++){
        //			hazards[i].load();
        //		}
    }

    public void unload() {
        //null out memory intensive objects
        randomNumberGenerator = null;
        hazards = null;
        cachedHazards = null;
    }

    private void createHazards() {
        Utilities.log.info("CREATE HAZARDS");
        //instantiate hazards
        hazards = new HazardSprite[MAX_NUM_HAZARDS];
        for (int i = 0; i < MAX_NUM_HAZARDS; i++) {

            HazardSprite hs = new HazardSprite();
            hs.setRenderOrder(Constants.HAZARD_LAYER);

            //			hazards[i] = new HazardSprite(choiceScreen, boardScreen, this, 0, new Point(-100,-100),
            //							GraphicsVariables.NUMBER_BOARD_SQUARE_WIDTH, 0, (byte)i);
            hazards[i] = hs;
        }
    }

    //	public boolean start(){
    //		if(isReady()){
    //			//System.out.println("HazardManager is active");
    //			setActive(true);
    //			setImageActive(true);
    //			
    //			//start hazards
    //			for(int i=0;i<MAX_NUM_HAZARDS;i++){
    //				boolean suceeded = false;
    //				do{
    //					suceeded = hazards[i].start();	//start the hazard
    //				}while(!suceeded);
    //			}
    //			return true;
    //		}
    //		return false;
    //	}

    public HazardSprite checkForCollisions(int playerBoardPosition) {
        for (int i = 0; i < numHazards; i++) {
            if (playerBoardPosition == (hazards[i].getBoardPosition())) {
                return hazards[i];
            }
        }
        return null; // no hazard on playerPosition
    }

    public int movementDueToHazard(int boardSquare) {
        //Checks to see if a player has landed on a hazard (called by numCompManager, in queue)
        //Note: returns a negative number
        int penalty = 0;
        for (int i = 0; i < numHazards; i++) {
            if ((boardSquare) == (hazards[i].getBoardPosition())) {
                penalty = hazards[i].getPenaltyValue();
                break;
            }
        }
        return penalty;
    }

    public void setHazardLevel(boolean state) {
        this.hazardsPresent = state;
        //		if(state==true)		
        //			setActive(true);
        //		else
        //			setActive(false);
    }

    public void setHazards(int rangeCeilling, Player player1, Player player2,
            boolean gameBeginning) {

        //this is called every turn by NumCompAlgManager, and adds more hazards if necessary
        //if(generalHazards||specificHazards){
        Utilities.log.info("HAZARDS PRESENT = " + hazardsPresent);
        //hazardsPresent = true; //changet to true for testing
        if (hazardsPresent) {
            //according to hazard level, decide on number and type of hazards to add
            //max num of hazards is 3, decide based on a normal curve
            numHazardsToAdd = (byte) Math.ceil(Math.abs(randomNumberGenerator.nextGaussian()));
            //numHazardsToAdd = 3;  //for debugging!!!
            //check we aren't adding too many
            if (numHazardsToAdd > 3) //don't add more than three each turn
                numHazardsToAdd = 3;
            if ((numHazards + numHazardsToAdd) > MAX_NUM_HAZARDS) //don't go over array limit
                numHazardsToAdd = MAX_NUM_HAZARDS - numHazards;
            //System.out.println("HM: Number hazards: " + numHazards);
            int[] hazardSquares = new int[numHazardsToAdd];
            int[] penaltyValues = new int[numHazardsToAdd];
            byte[] hazardTypes = new byte[numHazardsToAdd];

            //PICK RANDOM SQUARES FOR HAZARDS

            //sample space: square occupied by player1 + 1, to last square possible for player1 to arrive at
            int lowSquare = player1.getBoardPosition(); //exclusive
            int highSquare = Math.min((player1.getBoardPosition() + rangeCeilling),
                                      Constants.LAST_SQUARE); //inclusive				
            //System.out.println("HM: lowSquare: " + lowSquare);
            //System.out.println("HM: highSquare: " + highSquare);

            int squarePicked;
            boolean squareOkay;
            int lastSquare = Constants.LAST_SQUARE;
            hazardPickingLoop: for (int i = 0; i < numHazardsToAdd; i++) {
                byte passes = 0;
                do {
                    //break out if both players on last square
                    if (((player1.getBoardPosition()) == (lastSquare - 1))
                            && ((player2.getBoardPosition()) == (lastSquare - 1))) {
                        numHazardsToAdd = (byte) 0;
                        break hazardPickingLoop;
                    }

                    //pick a square at random from the sample space
                    squarePicked = randomNumberGenerator.nextInt(highSquare
                            - lowSquare)
                            + lowSquare + 1;

                    /*
                     * check that square isn't occupied by player and 
                     * don't pick the last square on the board, 
                     * or the first three squares (so can't go backwards off the board)
                     */
                    squareOkay = player1.getBoardPosition() != squarePicked
                            && player2.getBoardPosition() != squarePicked
                            && squarePicked < lastSquare && squarePicked > 3;

                    //check that neither square nor adjacent square is occupied by other hazard to add
                    for (int j = 0; squareOkay && j < i; j++) {
                        if (((hazardSquares[j] - 1) <= squarePicked)
                                && ((hazardSquares[j] + 1) >= squarePicked))
                            squareOkay = false;
                    }
                    //check that neither square nor adjacent saure is occupied by hazard already on board
                    for (int j = 0; squareOkay && j < numHazards; j++) {
                        if (((hazards[j].getBoardPosition() - 1) <= squarePicked)
                                && ((hazards[j].getBoardPosition() + 1) >= squarePicked))
                            squareOkay = false;
                    }

                    //System.out.println("HM: squarePicked: " + squarePicked);
                    //System.out.println("HM: player1square: " + numberTrack.players[Constants.PLAYER1].trackSquare);
                    //System.out.println("HM: player2square: " + numberTrack.players[Constants.PLAYER2].trackSquare);
                    //System.out.println("HM: squareOkay = " + squareOkay);

                    passes++;
                } while ((squareOkay == false) && (passes < 3));
                //gets going until finds ok square or has tried 3 times

                if (squareOkay == true) {
                    hazardSquares[i] = squarePicked;
                } else { //if couldn't find a square in three tries, reduce num of hazards to current
                    numHazardsToAdd = (byte) i;
                    break hazardPickingLoop;
                }
            }

            if (numHazardsToAdd > 0) {
                //play a warning
                if (!gameBeginning)
                    hazardListener.play("watchOutNewTraps"); //$NON-NLS-1$

                //decide on penalty values
                for (int i = 0; i < numHazardsToAdd; i++) {
                    penaltyValues[i] = -1
                            * (randomNumberGenerator.nextInt(3) + 1);
                }

                //assign general or specific to each hazard
                /*
                 * for(int i=0;i<numHazardsToAdd;i++){
                 * if((generalHazards==true)&&(specificHazards==true)){ boolean
                 * coinFlip = randomNumberGenerator.nextBoolean(); if(coinFlip){
                 * hazardTypes[i] = HazardSprite.GENERAL; penaltyValues[i] = -1;
                 * //this can be taken out, so that general hazards have a
                 * random penalty value } else hazardTypes[i] =
                 * HazardSprite.SPECIFIC; }
                 * if((generalHazards==true)&&(specificHazards==false)){
                 * hazardTypes[i] = HazardSprite.GENERAL; penaltyValues[i] = -1;
                 * //this can be taken out, so that general hazards have a
                 * random penalty value }
                 * if((generalHazards==false)&&(specificHazards==true)){
                 * hazardTypes[i] = HazardSprite.SPECIFIC; } }
                 */

                //set locations and visibility for new hazards
                for (int i = numHazards; i < (numHazards + numHazardsToAdd); i++) {
                    //if(i<numHazards){
                    hazards[i].setBoardPosition(hazardSquares[i - numHazards]);
                    int penaltyValue = penaltyValues[i - numHazards];
                    hazards[i].setPenaltyValue(penaltyValue);
                    //hazards[i].setHazardType(hazardTypes[i-numHazards]);

                    //                    hazards[i].setVisible(true);
                    hazards[i].setFrames(getHazard(penaltyValue));
                    // TO RETURN VALUE
                    hazardListener.addHazard(hazards[i]);

                    //}
                    /*
                     * else{ //set all the other hazards to zero and not visible
                     * hazards[i].setSquare(0); hazards[i].setPenaltyValue(0);
                     * hazards[i].setVisible(false); }
                     */

                }
                //augment the number of hazards
                numHazards = numHazards + numHazardsToAdd;

            }
        }
    }

    private MultiFrameImage getHazard(int penaltyValue) {
        assert penaltyValue < 0;
        MultiFrameImage hzi = cachedHazards.get(penaltyValue);
        if (hzi == null) {
            int ind = (-penaltyValue) - 1;
            hzi = new MultiFrameImageImpl(new Mirage[] {
                    new BufferedMirage(
                            ImageFactory.getImage(hazardFiles[ind][0])),
                    new BufferedMirage(
                            ImageFactory.getImage(hazardFiles[ind][1])) });
            cachedHazards.put(penaltyValue, hzi);
        }
        return hzi;
    }

    protected void render() {
        //does nothing
    }

    //    public boolean animate(Screen screen) {
    //        //System.out.println("HazardManager: animate called");
    //        if (isActive()) {
    //            //System.out.println("H: called animate and active");
    //            //Call the animate method of hazards
    //            for (int i = 0; i < numHazards; i++) {
    //                hazards[i].animate(screen);
    //            }
    //            return true;
    //        }
    //        return false;
    //    }

    //    public void animateImage(Screen screen) {
    //        //System.out.println("HM: Called animate Image");
    //        //Call the animate method of hazards
    //        for (int i = 0; i < numHazards; i++) {
    //            if (hazards[i].isImageActive())
    //                hazards[i].animateImage(screen);
    //        }
    //    }

    //	public void themeChanged(){
    //		hazardFiles = game.themeVars.hazardGifs;
    //		for(int i=0;i<numHazards;i++){
    //			hazards[i].themeChanged();
    //		}
    //		update();
    //	}

    //	public void resetGame(){
    //		//resets for another game
    //		//reset all hazards that were used
    //		for(int i=0;i<MAX_NUM_HAZARDS;i++){
    //			if(i<numHazards){
    //				hazards[i].reset();
    //			}
    //		}
    //		numHazards = 0;
    //	}

    public String getHazardPositionsCommaDelim() {
        //Returns a comma delimited string with value of hazards (if any, else zero)
        //for each board square
        String hazPos = new String(""); //$NON-NLS-1$
        for (int i = 0; i < Constants.LAST_SQUARE; i++) {
            boolean hazardPresent = false;
            int hazardValue = 0;
            for (int j = 0; j < numHazards; j++) {
                if (hazards[j].getBoardPosition() == (i + 1))
                    hazardValue = hazards[j].getPenaltyValue();
            }
            hazPos = hazPos + hazardValue + ","; //$NON-NLS-1$
        }
        return hazPos;
    }

    public static String getHazardHeadersCommaDelim() { // strange way, but don't have time to move it or REmove yet. made it static
        String hazHeadr = new String(""); //$NON-NLS-1$
        for (int i = 0; i < Constants.LAST_SQUARE; i++) {
            hazHeadr = hazHeadr + "hazSq" + (i + 1) + ","; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return hazHeadr;
    }

}