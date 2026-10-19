package org.unicog.numberrace.analytics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registro oficial: um arquivo JSON Lines por sessão, em
 * {@code events/<participant_id>/<AAAA-MM-DD>_<HHMMSS>_<session_id>.jsonl}. Cada evento é
 * acrescentado numa linha e forçado ao disco antes do próximo.
 */
class JsonLinesSink implements EventSink {

    private static final Logger LOG = Logger.getLogger("org.unicog.numberrace.analytics");
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final File eventsDir;
    private String currentSession;
    private File currentFile;
    private OutputStream out;

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
                endPartialLine(currentFile);
                out = open(currentFile);
            }
            out.write((e.toJson() + "\n").getBytes(UTF8));
            out.flush();
            if (out instanceof FileOutputStream) {
                ((FileOutputStream) out).getChannel().force(false);
            }
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Falha ao gravar o evento " + e.getSequenceNumber() + " em " + currentFile
                    + "; nova tentativa no próximo evento", t);
            closeQuietly();
        }
    }

    /**
     * Se uma gravação anterior parou no meio de uma linha (disco cheio, por exemplo), termina essa
     * linha antes de acrescentar a próxima. Assim o pedaço incompleto fica numa linha sozinho e o
     * evento seguinte continua legível.
     */
    private static void endPartialLine(File f) throws IOException {
        if (!f.isFile() || f.length() == 0) {
            return;
        }
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        try {
            raf.seek(raf.length() - 1);
            if (raf.read() != '\n') {
                raf.seek(raf.length());
                raf.write('\n');
                LOG.warning("Linha incompleta no fim de " + f + " (gravação interrompida); o próximo evento começa "
                        + "numa linha nova");
            }
        } finally {
            raf.close();
        }
    }

    /** Abre o arquivo para acrescentar linhas (substituído nos testes de falha de gravação). */
    OutputStream open(File f) throws IOException {
        return new FileOutputStream(f, true);
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
