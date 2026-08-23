package org.unicog.numberrace.sprites;

import org.unicog.numberrace.util.ImageFactory;

import com.threerings.media.image.BufferedMirage;
import com.threerings.media.image.Mirage;

public class RewardSprite extends ImageButtonSprite {

    private Mirage[] mirages;

    public RewardSprite(String[] images, String cmd, Object cmdArg) {
        super(cmd, cmdArg);
        mirages = new Mirage[] {
                new BufferedMirage(ImageFactory.getImage(images[0])),
                new BufferedMirage(ImageFactory.getImage(images[1])) };
        setMirage(mirages[0]);
    }

    @Override
    public void setOrientation(int orient) {
        super.setOrientation(orient);
        setMirage(mirages[orient]);
    }

}
