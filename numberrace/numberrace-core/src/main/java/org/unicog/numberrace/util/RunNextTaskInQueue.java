package org.unicog.numberrace.util;

import org.unicog.numberrace.GameObject;

public class RunNextTaskInQueue implements Runnable {

    private static RunNextTaskInQueue single;

    public void run() {
        GameObject.getInstance().getTaskQueue().nextTaskInQueue();
    }

    public static Runnable getInstance() {
        if (single == null) {
            single = new RunNextTaskInQueue();
        }
        return single;
    }
}
