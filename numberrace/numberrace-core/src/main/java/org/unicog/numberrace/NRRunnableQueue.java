/**
 * 
 */
package org.unicog.numberrace;

import java.awt.Component;
import java.beans.Statement;
import java.util.Comparator;
import java.util.Iterator;
import java.util.logging.Level;

import org.unicog.numberrace.util.Utilities;

import com.samskivert.util.Queue;
import com.samskivert.util.RunQueue;
import com.samskivert.util.SortableArrayList;
import com.threerings.media.FrameParticipant;
import com.threerings.media.Log;

public final class NRRunnableQueue implements Runnable, RunQueue,
        FrameParticipant {

    private class Task implements Runnable {
        private final Statement stmt;
        private long when;
        private Runnable runnable;
        private boolean postToEventDispatchThread;

        Task(Statement stmt, long when, boolean inEventDispatchThread) {
            assert (stmt != null);
            this.runnable = null;
            this.stmt = stmt;
            this.when = when;
            this.postToEventDispatchThread = inEventDispatchThread;
        }

        Task(Statement stmt, long when) {
            this(stmt, when, false);
        }

        Task(Runnable r, long when, boolean inEventDispatchThread) {
            assert (r != null);
            this.stmt = null;
            this.runnable = r;
            this.when = when;
            this.postToEventDispatchThread = inEventDispatchThread;
        }

        Task(Runnable r, long when) {
            this(r, when, false);
        }

        public void run() {
            try {
                if (runnable != null) {
                    runnable.run();
                } else {
                    stmt.execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void fastForward(long delta) {
            when += delta;
        }

        public boolean postToEventDispatchThread() {
            return postToEventDispatchThread;
        }

    }

    Comparator<Task> cmp = new Comparator<Task>() {

        public int compare(Task o1, Task o2) {
            if (o1 == o2) { // catches null == null
                return 0;
            } else if (o1 == null) {
                return 1;
            } else if (o2 == null) {
                return -1;
            }

            return (o1.when < o2.when ? -1 : (o1.when == o2.when ? 0 : 1));
        }
    };

    Queue<Runnable> queue = new Queue<Runnable>(10);

    Queue<Runnable> sequenceQueue = new Queue<Runnable>(10);

    SortableArrayList<Task> delayedTasks = new SortableArrayList<Task>();

    private long lastTickStamp;

    private boolean running;

    private boolean _paused;
    private long _pauseTime;

    public void run() {
        running = true;
        while (running) {
            Runnable toRun = queue.get();
            try {
                toRun.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
    }

    public boolean isDispatchThread() {
        return false;
    }

    public void postRunnable(Runnable r) {
        if (r != null) {
            queue.append(r);
        } else {
            Utilities.log.warning("trying to post null in running queue");
            Thread.dumpStack();
        }
    }

    public void addTask(Statement stmt, long delay) {
        if (delay <= 0) {
            postRunnable(new Task(stmt, 0));
        }
        synchronized (delayedTasks) {
            delayedTasks.insertSorted(new Task(stmt, lastTickStamp + delay),
                                      cmp);
        }
    }

    public void addTask(Runnable r, long delay) {
        addTask(r, delay, false);
    }

    public void addTask(Runnable r, long delay, boolean inEventDispatchThread) {
        if (delay <= 0) {
            if (inEventDispatchThread) {
                AWT.postRunnable(r);
            } else {
                postRunnable(r);
            }
        }
        synchronized (delayedTasks) {
            delayedTasks.insertSorted(new Task(r, lastTickStamp + delay,
                    inEventDispatchThread), cmp);
        }
    }

    public void addTaskInQueue(Runnable r) {
        sequenceQueue.append(r);
    }

    public Runnable nextTaskInQueue() {
        Runnable r = sequenceQueue.getNonBlocking();
        postRunnable(r);
        return r;
    }

    public void tick(long tickStamp) {
        this.lastTickStamp = tickStamp;
        if (_paused) {
            return;
        }
        synchronized (delayedTasks) {
            if (!delayedTasks.isEmpty()) {
                for (Iterator<Task> taskIter = delayedTasks.iterator(); taskIter.hasNext();) {
                    Task task = taskIter.next();
                    if (task.when <= tickStamp && !_paused) {
                        if (task.postToEventDispatchThread()) {
                            AWT.postRunnable(task);
                        } else {
                            postRunnable(task);
                        }
                        taskIter.remove();
                        if (Utilities.log.getLevel().intValue() >= Level.FINE.intValue()) {
                            Utilities.log.fine("when :" + task.when + " rt :"
                                    + tickStamp);
                        }
                    } else {
                        break;
                    }
                }
            }
        }
    }

    public Component getComponent() {
        return null;
    }

    public boolean needsPaint() {
        return false;
    }

    public void reset() {
        synchronized (delayedTasks) {
            delayedTasks.clear();
        }
        sequenceQueue.clear();
    }

    public void setPaused(boolean paused) {
        // sanity check
        if ((paused && (_pauseTime != 0)) || (!paused && (_pauseTime == 0))) {
            Log.warning("Requested to pause when paused or vice-versa [paused="
                    + paused + "].");
            return;
        }

        if (paused) {
            // make a note of our pause time
            _pauseTime = lastTickStamp;

        } else {
            // let the animation and sprite managers know that we just warped into the future
            long delta = lastTickStamp - _pauseTime;

            synchronized (delayedTasks) {
                for (Iterator<Task> taskIter = delayedTasks.iterator(); taskIter.hasNext();) {
                    Task task = taskIter.next();
                    task.fastForward(delta);
                }

            }
            // clear out our pause time
            _pauseTime = 0;
        }
        _paused = paused;

    }
}