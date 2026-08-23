package org.unicog.numberrace.managers;

import org.unicog.numberrace.NRRunnableQueue;
import org.unicog.numberrace.sound.SoundListener;

public class DelayedSoundListener implements SoundListener {

    private final NRRunnableQueue taskQ;
    private final Runnable runnable;
    private final int delay;
    private final boolean inEventDispatchThread;

    public DelayedSoundListener(NRRunnableQueue taskQ, int delay,
            Runnable runnable, boolean inEventDispatchThread) {
        this.inEventDispatchThread = inEventDispatchThread;
        this.taskQ = taskQ;
        this.delay = delay;
        this.runnable = runnable;
    }

    public DelayedSoundListener(NRRunnableQueue taskQ, int delay,
            Runnable runnable) {
        this(taskQ, delay, runnable, false);
    }

    public void run() {
        taskQ.addTask(runnable, delay, inEventDispatchThread);
    }
}