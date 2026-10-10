package org.unicog.numberrace.analytics.export;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Os eventos de uma sessão, na ordem de {@code sequence_number}, depois da importação. */
final class SessionData {

    private final String sessionId;
    private final String participantId;
    private final List<SourceEvent> events;
    private final List<Integer> missingSequenceNumbers;
    private final Set<String> sourceFiles = new LinkedHashSet<String>();
    private final OffsetDateTime startTime;

    SessionData(String sessionId, List<SourceEvent> sortedEvents) {
        this.sessionId = sessionId;
        this.events = Collections.unmodifiableList(new ArrayList<SourceEvent>(sortedEvents));
        this.participantId = sortedEvents.get(0).getParticipantId();
        this.startTime = sortedEvents.get(0).parse().time();
        List<Integer> missing = new ArrayList<Integer>();
        int expected = 1;
        for (SourceEvent e : sortedEvents) {
            for (int n = expected; n < e.getSequenceNumber(); n++) {
                missing.add(Integer.valueOf(n));
            }
            expected = e.getSequenceNumber() + 1;
            sourceFiles.add(e.getSourceFile());
        }
        this.missingSequenceNumbers = Collections.unmodifiableList(missing);
    }

    String getSessionId() {
        return sessionId;
    }

    String getParticipantId() {
        return participantId;
    }

    List<SourceEvent> getEvents() {
        return events;
    }

    /** Números de sequência que faltam antes do último evento (eventos perdidos). */
    List<Integer> getMissingSequenceNumbers() {
        return missingSequenceNumbers;
    }

    Set<String> getSourceFiles() {
        return sourceFiles;
    }

    OffsetDateTime getStartTime() {
        return startTime;
    }

    boolean hasSessionStart() {
        return events.get(0).is("SESSION_START");
    }

    boolean hasSessionEnd() {
        return events.get(events.size() - 1).is("SESSION_END");
    }
}
