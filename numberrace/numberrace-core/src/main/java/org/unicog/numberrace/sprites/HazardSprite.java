package org.unicog.numberrace.sprites;

import com.threerings.media.sprite.ImageSprite;

public class HazardSprite extends ImageSprite {

    private int boardPosition;
    private int penaltyValue;

    public int getBoardPosition() {
        return boardPosition;
    }

    public void setBoardPosition(int boardPosition) {
        this.boardPosition = boardPosition;
    }

    public int getPenaltyValue() {
        return penaltyValue;
    }

    public void setPenaltyValue(int penaltyValue) {
        this.penaltyValue = penaltyValue;
    }

}
