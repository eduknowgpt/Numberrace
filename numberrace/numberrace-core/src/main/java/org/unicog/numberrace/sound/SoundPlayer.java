package org.unicog.numberrace.sound;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.SwingUtilities;

public class SoundPlayer extends ThreadPoolExecutor implements PlayerControls {

    public static boolean IS_LINUX = System.getProperty("os.name")
                                           .contains("inux");

    // Sound manager variables
    private static final int SAMPLERATE = 44100;
    private static final int SAMPLESIZE = 16;
    private static final int CHANNELS = 2;
    private static final AudioFormat PLAYBACK_FORMAT = new AudioFormat(
            SAMPLERATE, SAMPLESIZE, CHANNELS, true, false);

    private static final Logger log = Logger.getLogger(SoundPlayer.class.getPackage()
                                                                        .getName());

    private static javax.sound.sampled.Mixer.Info mInfo2use;

    public static boolean soundsON = true;

    private static SoundPlayer INSTANCE;

    private final AudioFormat playbackFormat;
    private ThreadLocal<DataLine> localLine;

    private Set<PlayerControls> playersList = Collections.newSetFromMap(new ConcurrentHashMap<PlayerControls, Boolean>());
    private Set<DataLine> openedLines = Collections.newSetFromMap(new ConcurrentHashMap<DataLine, Boolean>());

    private PlayerControls emptyPlayerControll = new PlayerControls() {

        public void stop() {
        }

        public void pause() {
        }

        public void unpause() {
        }
    };

    private boolean globalPaused;

    public SoundPlayer() {
        this(PLAYBACK_FORMAT);
    }

    public SoundPlayer(AudioFormat playbackFormat) {
        this(playbackFormat, Math.min(5,
                getMaxSimultaneousSounds(playbackFormat)));
    }

    /**
     * Creates a new SoundPlayer with the specified maximum number of
     * simultaneous sounds.
     */
    private SoundPlayer(AudioFormat playbackFormat, int maxSimultaneousSounds) {
        super(Math.min(3, maxSimultaneousSounds), maxSimultaneousSounds, 3L,
                TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(),
                Executors.defaultThreadFactory());

        this.playbackFormat = playbackFormat;
        localLine = new ThreadLocal<DataLine>();
    }

    /**
     * Gets the maximum number of simultaneous sounds with the specified
     * AudioFormat that the default mixer can play.
     */
    public static int getMaxSimultaneousSounds(AudioFormat playbackFormat) {
        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class,
                playbackFormat);

        Mixer.Info[] mInfos = AudioSystem.getMixerInfo();
        int maxLines = 0;
        for (Mixer.Info info : mInfos) {
            Mixer mixer = AudioSystem.getMixer(info);
            int tmpLines = mixer.getMaxLines(lineInfo);

            if (log.isLoggable(Level.FINE)) {
                log.fine("mInfo: " + info.toString() + " maxLines: " + tmpLines);
            }

            if (tmpLines == AudioSystem.NOT_SPECIFIED) {
                maxLines = 32;
                mInfo2use = info;
                break;
            } else if (tmpLines > maxLines) {
                maxLines = tmpLines;
                mInfo2use = info;
            }
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine("MaxLine = " + maxLines);
        }

        return maxLines;
    }

    public void shutdown() {
        stop();
        super.shutdown();
    }

    public void pause() {
        if (!this.globalPaused) {
            for (PlayerControls player : playersList) {
                player.pause();
            }
            this.globalPaused = true;
        }
    }

    public void unpause() {
        if (globalPaused) {
            for (PlayerControls player : playersList) {
                player.unpause();
            }
            globalPaused = false;
        }
    }

    /**
     * Stops all sounds playing
     *
     */
    public void stop() {
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("Requesting %d players to stop... ",
                                   playersList.size()));
        }
        for (PlayerControls player : playersList) {
            player.stop();
            player.unpause();
        }
    }

    /**
     * Plays a sound from an InputStream. This method returns immediately.
     */
    public PlayerControls play(AudioInputStream source, boolean loop, Runnable l) {

        if (source != null) {

            if (source.markSupported()) {
                try {
                    source.reset();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            RunnablePlayerControls player = findPlayer(source, loop, l);
            if (player != null) {
                playersList.add(player);
                execute(player);
                return player;
            }

            log.severe("couldn't convert [" + source.getFormat()
                    + "] to any format we could play.");
            notifyListener(l);
        } else {
            notifyListener(l);
        }
        return emptyPlayerControll;
    }

    private RunnablePlayerControls findPlayer(AudioInputStream source,
            boolean loop, Runnable l) {
        RunnablePlayerControls result = createPlayer(playbackFormat,
                                                     source,
                                                     loop,
                                                     l);

        if (result == null) {
            AudioFormat[] targetFormats = AudioSystem.getTargetFormats(playbackFormat.getEncoding(),
                                                                       source.getFormat());
            for (int i = 0; i < targetFormats.length && result == null; i++) {
                AudioFormat format = targetFormats[i];
                result = createPlayer(format, source, loop, l);
            }

            if (result == null) {
                result = createPlayer(source.getFormat(), source, loop, l);
            }
        }
        return result;

    }

    private RunnablePlayerControls createPlayer(AudioFormat format,
            AudioInputStream source, boolean loop, Runnable l) {

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(format,
                                                                                source);
            if (AudioSystem.isLineSupported(new DataLine.Info(
                    SourceDataLine.class, format))) {
                if (log.isLoggable(Level.FINER)) {
                    log.finer("SourceDataLine: " + source.getFormat() + " -> "
                            + format);
                }
                return new SourceLinePlayer(audioInputStream, loop, l);
            }
            if (AudioSystem.isLineSupported(new DataLine.Info(Clip.class,
                    format))) {
                if (log.isLoggable(Level.FINER)) {
                    log.finer("Clip: " + source.getFormat() + " -> " + format);
                }
                return new ClipPlayer(audioInputStream, loop, l);
            }
        } catch (Exception e) {
        }

        return null;
    }

    protected void notifyListener(Runnable l) {
        if (l != null) {
            SwingUtilities.invokeLater(l);
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {

        if (r instanceof SourceLinePlayer) {
            SourceLinePlayer player = (SourceLinePlayer) r;

            if (log.isLoggable(Level.FINE)) {
                log.fine("SourceLinePlayer for " + player.format);
            }

            SourceDataLine line = null;
            try {
                line = AudioSystem.getSourceDataLine(player.format, mInfo2use);
            } catch (LineUnavailableException ex) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine(ex.getLocalizedMessage());
                }
                // try default system mixer if the chosen one failed to provide us dataline 
                try {
                    line = AudioSystem.getSourceDataLine(player.format);
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }
                return;
            }

            openedLines.add(line);

            localLine.set(line);
        } else if (r instanceof ClipPlayer) {
            ClipPlayer player = (ClipPlayer) r;

            if (log.isLoggable(Level.FINE)) {
                log.fine("SourceLinePlayer for " + player.format);
            }

            Clip line = null;
            try {
                line = (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class,
                        player.format));
            } catch (LineUnavailableException e) {
                e.printStackTrace();
                return;
            }

            openedLines.add(line);

            localLine.set(line);
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {

        if (log.isLoggable(Level.FINE)) {
            log.fine("Player was " + r);
        }

        DataLine line = localLine.get();
        if (line != null) {
            line.stop();
            line.flush();
            line.close();
        }

        openedLines.remove(line);

        localLine.set(null);

    }

    public static SoundPlayer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SoundPlayer(PLAYBACK_FORMAT, 5);
        }
        return INSTANCE;
    }

    interface RunnablePlayerControls extends Runnable, PlayerControls {
    }

    protected class ClipPlayer implements RunnablePlayerControls, LineListener {

        private Object lock = new Object();

        private final AudioInputStream source;
        private final boolean loop;
        private final Runnable listener;
        private final AudioFormat format;
        private Clip clip;
        private volatile boolean paused = false;
        private volatile boolean stopped = false;

        public ClipPlayer(AudioInputStream source, boolean loop, Runnable l) {
            this.source = source;
            this.loop = loop;
            this.format = source.getFormat();
            this.listener = l;
        }

        public void run() {
            clip = (Clip) localLine.get();

            if (clip != null) {
                clip.addLineListener(this);
                try {
                    clip.open(source);

                    if (loop) {
                        clip.setLoopPoints(0, -1);
                        clip.loop(Clip.LOOP_CONTINUOUSLY);
                    }
                    synchronized (lock) {
                        clip.start();
                        lock.wait();
                    }

                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            }

            if (log.isLoggable(Level.FINEST)) {
                log.finest("\tabout to remove player from the active list");
            }
            playersList.remove(this);
        }

        public void stop() {
            stopped = true;
            if (clip != null) {
                clip.stop();
            }
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        public void unpause() {
            if (paused) {
                paused = false;
                clip.start();
            }
        }

        public void pause() {
            paused = true;
            if (clip != null && !stopped) {
                if (paused) {
                    clip.stop();
                }
            }
        }

        public void update(LineEvent event) {
            System.out.println(event + "\n\t" + paused + " : " + stopped);
            if (LineEvent.Type.STOP.equals(event.getType())) {
                if (!paused && !stopped) {
                    stopped = true;
                    if (listener != null) {
                        SwingUtilities.invokeLater(listener);
                    }
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            }
        }
    }

    protected class SourceLinePlayer implements RunnablePlayerControls {

        private volatile boolean toBeStopped = false;
        private volatile boolean pause = false;

        private final Object pauseLock = new Object();
        private final boolean loop;
        private final InputStream source;

        private final Runnable listener;

        private SourceDataLine line;

        private final AudioFormat format;
        private final float sampleRate;
        private final int sampleSize;
        private final int channels;

        public SourceLinePlayer(AudioInputStream source, boolean loop,
                Runnable l) {
            this.source = source;
            this.loop = loop;
            this.listener = l;
            this.format = source.getFormat();
            sampleRate = (AudioSystem.NOT_SPECIFIED == format.getSampleRate()) ? SAMPLERATE
                    : format.getSampleRate();
            sampleSize = (AudioSystem.NOT_SPECIFIED == format.getSampleSizeInBits()) ? SAMPLESIZE
                    : format.getSampleSizeInBits();
            channels = (AudioSystem.NOT_SPECIFIED == format.getChannels()) ? CHANNELS
                    : format.getChannels();
        }

        public void run() {
            // get line and buffer from ThreadLocals
            line = (SourceDataLine) localLine.get();
            if (line != null && !line.isOpen()) {
                try {
                    line.open();
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }
            }
            byte[] buffer = new byte[(int) Math.ceil(sampleRate
                    * (sampleSize / 8) * channels * 2)];

            final boolean finest = log.isLoggable(Level.FINEST);

            if (localLine.get() != null && buffer != null && source != null) {

                try {
                    if (loop) {
                        source.mark(Integer.MAX_VALUE);
                    }

                    line.start();

                    long startTime = System.nanoTime();
                    int totalRead = 0;

                    int numBytesRead = 0;
                    int offset = 0;
                    int bytesWritten = 0;
                    boolean resetTime = true;
                    while (!toBeStopped) {

                        if (numBytesRead == bytesWritten) {
                            if (finest)
                                log.finest("\t reading data");
                            numBytesRead = source.read(buffer, 0, buffer.length);

                            if (finest)
                                log.finest("\tnumBytesRead = " + numBytesRead);
                            if (numBytesRead == -1) {
                                if (loop) {
                                    bytesWritten = numBytesRead;
                                    totalRead = 0;
                                    source.reset();
                                    resetTime = true;
                                    continue;
                                }
                                break;
                            }
                            totalRead += numBytesRead;
                            offset = 0;
                        } else {
                            numBytesRead -= bytesWritten;
                            offset += bytesWritten;
                        }

                        synchronized (pauseLock) {
                            if (pause) {
                                if (finest)
                                    log.finest("\t line STOPPED");
                                long time = System.nanoTime();
                                try {
                                    if (finest)
                                        log.finest("\t--> pausing <--");
                                    pauseLock.wait();
                                    pause = false;
                                    if (finest)
                                        log.finest("\t<-- un-pausing -->");
                                } catch (InterruptedException ex) {
                                    break;
                                }
                                if (resetTime) {
                                    startTime += System.nanoTime() - time;
                                }
                                line.start();
                                if (finest)
                                    log.finest("\t line STARTED");
                            }
                        }
                        if (!toBeStopped) {
                            if (numBytesRead != 0) {
                                if (resetTime) {
                                    resetTime = false;
                                    startTime = System.nanoTime();
                                }
                                bytesWritten = line.write(buffer,
                                                          offset,
                                                          numBytesRead);
                            } else {
                                bytesWritten = 0;
                            }
                        }

                    } // end:while

                    if (!toBeStopped) {

                        /*
                         * idea is from SoundManager nenya library. Do not use
                         * drain, but wait for playing time. Basically manual
                         * drain. Explanation taken from nenya
                         */

                        // sleep the drain time. We never trust line.drain() because
                        // it is buggy and locks up on natively multithreaded systems
                        // (linux, winXP with HT).

                        long totalDrainTime = (long) Math.ceil(((totalRead * 8) / (sampleRate
                                * sampleSize * channels)) * 1000);

                        // subtract out time we've already spent doing things.

                        long spendedTime = (System.nanoTime() - startTime) / 1000000;
                        long drainTime = Math.max(250, totalDrainTime
                                - spendedTime);
                        try {
                            if (!toBeStopped) {
                                Thread.sleep(drainTime);
                            }
                        } catch (InterruptedException ie) {
                        }
                        if (finest) {
                            log.finest(new StringBuilder("\nsr: ").append(sampleRate)
                                                                  .append("\nss: ")
                                                                  .append(sampleSize)
                                                                  .append("\ntotalRead :")
                                                                  .append(totalRead)
                                                                  .append("\nTotalDrainTtime: ")
                                                                  .append(totalDrainTime)
                                                                  .append("\nSpendedTime: ")
                                                                  .append(spendedTime)
                                                                  .append("\nDrainTime :")
                                                                  .append(drainTime)
                                                                  .toString());
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (finest)
                        log.finest("\tstopping the line...");
                    line.close();

                    if (listener != null && !toBeStopped) {
                        if (finest)
                            log.finest("\tWe have listener ! ");
                        synchronized (pauseLock) {
                            if (pause) {
                                try {
                                    if (finest)
                                        log.finest("\t--- pausing before listener ---");
                                    pauseLock.wait();
                                    pause = false;
                                } catch (InterruptedException ex) {
                                }
                            }
                        }

                        if (!toBeStopped) {
                            if (finest)
                                log.finest("\t about to run sound listener...");
                            SwingUtilities.invokeLater(listener);
                        } else if (finest) {
                            log.finest("\tSkipping this listener because this sound was set to be stopped");
                        }

                    }
                    if (finest)
                        log.finest("\tflushing the line...");
                    line.flush();
                }
            } else {
                if (listener != null) { // never happens I think
                    log.warning("\tline, buffer or source is NULL, but there is listener. Firing");
                    listener.run();
                }
            }

            if (finest)
                log.finest("\tabout to remove player from the active list");
            playersList.remove(this);
        }

        public void pause() {
            this.pause = true;
            if (line != null && !IS_LINUX) {
                line.stop();
            }
            if (log.isLoggable(Level.FINE)) {
                log.fine(" requsted to pause --> " + this);
            }

        }

        public void unpause() {
            if (this.pause) {
                this.pause = false;
                synchronized (pauseLock) {
                    pauseLock.notifyAll();
                }
            }
        }

        public void stop() {
            if (!toBeStopped) {
                toBeStopped = true;
                if (line != null && !IS_LINUX) {
                    line.stop();
                }
            }
        }

    }
}