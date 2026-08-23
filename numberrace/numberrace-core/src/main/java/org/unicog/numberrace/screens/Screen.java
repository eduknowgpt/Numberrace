package org.unicog.numberrace.screens;

import com.threerings.media.FrameParticipant;

public interface Screen extends FrameParticipant {

    public void load();

    public void start();

    public void stop();

    public void unload();

    public void pause();

    public void unpause();
}
