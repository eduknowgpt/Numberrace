package org.unicog.numberrace.analytics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registro oficial: um arquivo JSON Lines por sessão, em
 * {@code events/<participant_id>/<AAAA-MM-DD>_<HHMMSS>_<session_id>.jsonl}. Cada evento é
 * acrescentado numa linha e forçado ao disco antes do próximo.
 */
final class JsonLinesSink implements EventSink {

    private static final Logger LOG = Logger.getLogger("org.unicog.numberrace.analytics");
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final File eventsDir;
    private String currentSession;
    private File currentFile;
    private FileOutputStream out;

    JsonLinesSink(File eventsDir) {
        this.eventsDir = eventsDir;
    }

    File getCurrentFile() {
        return currentFile;
    }

    public void write(InteractionEvent e) {
        try {
            if (!e.getSessionId().equals(currentSession)) {
                closeQuietly();
                currentSession = e.getSessionId();
                currentFile = new File(new File(eventsDir, e.getParticipantId()),
                        FILE_TIME.format(e.getTime()) + "_" + e.getSessionId() + ".jsonl");
            }
            if (out == null) {
                File dir = currentFile.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new IOException("Não foi possível criar " + dir);
                }
                out = new FileOutputStream(currentFile, true);
            }
            out.write((e.toJson() + "\n").getBytes(UTF8));
            out.flush();
            out.getChannel().force(false);
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Falha ao gravar o evento " + e.getSequenceNumber() + " em " + currentFile
                    + "; nova tentativa no próximo evento", t);
            closeQuietly();
        }
    }

    public void sessionEnded(String sessionId) {
        if (sessionId.equals(currentSession)) {
            closeQuietly();
        }
    }

    public void close() {
        closeQuietly();
    }

    private void closeQuietly() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {
                // nada a fazer
            }
            out = null;
        }
    }
}
