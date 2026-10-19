package org.unicog.numberrace.analytics.export;

import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.unicog.numberrace.analytics.Json;

/**
 * Um evento lido de um arquivo JSON Lines. Para caber muitos eventos na memória (por exemplo,
 * dados de uma escola inteira), guarda a linha original em UTF-8 e só os campos da parte comum
 * usados para juntar e ordenar; o resto é lido da linha quando necessário ({@link #json()}).
 */
final class SourceEvent {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final byte[] line;
    private final String sourceFile;
    private final int lineNumber;
    private final String schemaVersion;
    private final String eventId;
    private final String eventType;
    private final String sessionId;
    private final String participantId;
    private final String activity;
    private final int sequenceNumber;
    private final Integer gameNumber;
    private final Integer turnNumber;

    SourceEvent(String line, Map<String, Object> json, String sourceFile, int lineNumber) {
        this.line = line.getBytes(UTF8);
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.schemaVersion = ((String) json.get("schema_version")).intern();
        this.eventId = (String) json.get("event_id");
        // textos que se repetem em muitos eventos são compartilhados
        this.eventType = ((String) json.get("event_type")).intern();
        this.sessionId = ((String) json.get("session_id")).intern();
        this.participantId = ((String) json.get("participant_id")).intern();
        this.activity = json.get("activity") instanceof String ? ((String) json.get("activity")).intern() : null;
        this.sequenceNumber = ((Number) json.get("sequence_number")).intValue();
        this.gameNumber = toInteger(json.get("game_number"));
        this.turnNumber = toInteger(json.get("turn_number"));
    }

    /** A linha original, exatamente como estava no arquivo. */
    String getLine() {
        return new String(line, UTF8);
    }

    boolean sameLine(SourceEvent other) {
        return Arrays.equals(line, other.line);
    }

    /** O evento completo, lido de novo da linha original. */
    @SuppressWarnings("unchecked")
    Map<String, Object> json() {
        return (Map<String, Object>) Json.parse(getLine());
    }

    String getSourceFile() {
        return sourceFile;
    }

    int getLineNumber() {
        return lineNumber;
    }

    String getSchemaVersion() {
        return schemaVersion;
    }

    String getActivity() {
        return activity;
    }

    String getEventId() {
        return eventId;
    }

    String getEventType() {
        return eventType;
    }

    String getSessionId() {
        return sessionId;
    }

    String getParticipantId() {
        return participantId;
    }

    int getSequenceNumber() {
        return sequenceNumber;
    }

    Integer getGameNumber() {
        return gameNumber;
    }

    Integer getTurnNumber() {
        return turnNumber;
    }

    boolean is(String type) {
        return type.equals(eventType);
    }

    /** Leitura do evento já convertido, com acesso aos campos mais usados. */
    static final class Parsed {
        final Map<String, Object> json;

        Parsed(Map<String, Object> json) {
            this.json = json;
        }

        String timestamp() {
            return (String) json.get("timestamp");
        }

        OffsetDateTime time() {
            return OffsetDateTime.parse(timestamp());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data() {
            Object d = json.get("data");
            return d instanceof Map ? (Map<String, Object>) d : Collections.<String, Object> emptyMap();
        }
    }

    Parsed parse() {
        return new Parsed(json());
    }

    private static Integer toInteger(Object v) {
        return v instanceof Number ? Integer.valueOf(((Number) v).intValue()) : null;
    }
}
