package org.unicog.numberrace.sprites;

public interface CommandOnPressedSprite {

    /**
     * @return the action command to submit if this sprite is clicked.
     */
    public String getActionCommand();

    /**
    * @return the argument to the action command.
    */
    public Object getCommandArgument();

}
