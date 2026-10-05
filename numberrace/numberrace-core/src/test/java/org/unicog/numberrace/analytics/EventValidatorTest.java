package org.unicog.numberrace.analytics;

import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.Test;

public class EventValidatorTest {

    private static InteractionEvent evento(EventType t, Integer game, Integer turn, Map<String, Object> data) {
        return new InteractionEvent("id", t, OffsetDateTime.now(), "s", "P017", 1,
                game == null ? null : "number_comparison", game, turn, data);
    }

    @Test
    public void eventoCompletoEValido() {
        assertTrue(EventValidator.validate(evento(EventType.CLICK_IGNORED, 1, 2, Json.obj("side", "left"))).isEmpty());
    }

    @Test
    public void detectaContextoECamposErrados() {
        assertTrue(EventValidator.validate(evento(EventType.CLICK_IGNORED, null, null, Json.obj("side", "left")))
                .toString().contains("exige game_number"));
        assertTrue(EventValidator.validate(evento(EventType.CLICK_IGNORED, 1, 1, Json.obj("lado", "left")))
                .toString().contains("campo não previsto em data: lado"));
        assertTrue(EventValidator.validate(evento(EventType.ANSWER_TIMEOUT, 1, 1,
                Json.obj("deadline_ms", -1, "assigned_side", "left"))).toString().contains("negativo"));
    }
}
