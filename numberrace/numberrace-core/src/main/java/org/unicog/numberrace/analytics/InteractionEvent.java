package org.unicog.numberrace.analytics;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Um evento de interação no modelo 1.0: parte comum (envelope) e parte específica ({@code data}).
 */
public final class InteractionEvent {

    public static final String SCHEMA_VERSION = "1.0";

    static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final String eventId;
    private final EventType eventType;
    private final OffsetDateTime time;
    private final String sessionId;
    private final String participantId;
    private final int sequenceNumber;
    private final String activity;
    private final Integer gameNumber;
    private final Integer turnNumber;
    private final Map<String, Object> data;

    InteractionEvent(String eventId, EventType eventType, OffsetDateTime time, String sessionId,
            String participantId, int sequenceNumber, String activity, Integer gameNumber,
            Integer turnNumber, Map<String, Object> data) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.time = time;
        this.sessionId = sessionId;
        this.participantId = participantId;
        this.sequenceNumber = sequenceNumber;
        this.activity = activity;
        this.gameNumber = gameNumber;
        this.turnNumber = turnNumber;
        this.data = data == null ? Collections.<String, Object> emptyMap() : data;
    }

    public String getEventId() {
        return eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public OffsetDateTime getTime() {
        return time;
    }

    public String getTimestamp() {
        return TIMESTAMP.format(time);
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getActivity() {
        return activity;
    }

    public Integer getGameNumber() {
        return gameNumber;
    }

    public Integer getTurnNumber() {
        return turnNumber;
    }

    public Map<String, Object> getData() {
        return data;
    }

    /** O evento completo em JSON, numa linha, na ordem de campos do modelo. */
    public String toJson() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("schema_version", SCHEMA_VERSION);
        m.put("event_id", eventId);
        m.put("event_type", eventType.name());
        m.put("timestamp", getTimestamp());
        m.put("session_id", sessionId);
        m.put("participant_id", participantId);
        m.put("sequence_number", sequenceNumber);
        if (activity != null) {
            m.put("activity", activity);
        }
        if (gameNumber != null) {
            m.put("game_number", gameNumber);
        }
        if (turnNumber != null) {
            m.put("turn_number", turnNumber);
        }
        m.put("data", data);
        return Json.write(m);
    }
}
