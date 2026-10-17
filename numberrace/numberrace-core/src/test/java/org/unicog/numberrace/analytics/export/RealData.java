package org.unicog.numberrace.analytics.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.unicog.numberrace.analytics.Json;

/**
 * Sessões gravadas pelo jogo real na Etapa 7, compactadas em {@code dados-reais/}: as duas do teste
 * no Windows (participante de teste {@code PX43KJ8}, 197 eventos) e as quatro do jogo executado por
 * programa (três participantes de teste, 1.345 eventos, uma delas encerrada à força). Todas são
 * dados simulados: nenhuma criança participou.
 */
final class RealData {

    static final String WINDOWS = "windows";
    static final String ROBOT = "robo";

    static final String[] WINDOWS_SESSIONS = {
        "2026-10-11_142340_5f454cb9-cae8-4529-961f-77716e593343",
        "2026-10-11_143244_51747c7e-69b4-4b56-86f8-b24b4e88ffcf",
    };

    static final String[] ROBOT_SESSIONS = {
        "2026-10-11_161047_51ccdb24-4c61-4c92-8013-7f3dad83d657",
        "2026-10-11_161742_3a5f7974-9678-4566-8a43-b044336def0f",
        "2026-10-11_165319_2eb63d90-d26f-4f00-b315-c5d4532b50ed",
        "2026-10-11_165849_50c1faf3-663b-43f3-89bc-dce85c7d9cc0",
    };

    /** A sessão do robô encerrada à força (sem {@code SESSION_END}). */
    static final String KILLED_SESSION = "51ccdb24-4c61-4c92-8013-7f3dad83d657";

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private RealData() {
    }

    static String[] sessions(String group) {
        return WINDOWS.equals(group) ? WINDOWS_SESSIONS : ROBOT_SESSIONS;
    }

    /** As linhas de uma sessão. */
    static List<String> lines(String group, String session) throws IOException {
        InputStream in = new GZIPInputStream(
                RealData.class.getResourceAsStream("dados-reais/" + group + "/" + session + ".jsonl.gz"));
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, UTF8));
            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().length() > 0) {
                    lines.add(line);
                }
            }
            return lines;
        } finally {
            in.close();
        }
    }

    /** Todas as linhas de um grupo. */
    static List<String> lines(String group) throws IOException {
        List<String> all = new ArrayList<String>();
        for (String s : sessions(group)) {
            all.addAll(lines(group, s));
        }
        return all;
    }

    /** Grava as sessões do grupo como a pasta {@code interaction-data} de um computador e devolve essa pasta. */
    @SuppressWarnings("unchecked")
    static File computer(File root, String group) throws IOException {
        File data = new File(root, "interaction-data");
        for (String s : sessions(group)) {
            List<String> lines = lines(group, s);
            String participant = (String) ((Map<String, Object>) Json.parse(lines.get(0))).get("participant_id");
            File dir = new File(data, "events/" + participant);
            dir.mkdirs();
            OutputStream out = new FileOutputStream(new File(dir, s + ".jsonl"));
            try {
                for (String line : lines) {
                    out.write((line + "\n").getBytes(UTF8));
                }
            } finally {
                out.close();
            }
        }
        return data;
    }
}
