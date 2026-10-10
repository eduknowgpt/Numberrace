package org.unicog.numberrace.analytics;

import static org.junit.Assert.assertEquals;
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

    private static final String LINHA = "{\"schema_version\":\"1.0\",\"event_id\":\"0a1b2c3d-1111-4222-8333-444455556666\","
            + "\"event_type\":\"CLICK_IGNORED\",\"timestamp\":\"2026-10-09T18:29:27.452-03:00\","
            + "\"session_id\":\"5fb5269a-ab53-4ee9-bf7a-f8489e351cf4\",\"participant_id\":\"P4WVSUY\","
            + "\"sequence_number\":11,\"activity\":\"number_comparison\",\"game_number\":1,\"turn_number\":1,"
            + "\"data\":{\"side\":\"left\"}}";

    @SuppressWarnings("unchecked")
    private static Map<String, Object> lido(String json) {
        return (Map<String, Object>) Json.parse(json);
    }

    @Test
    public void validaEventoLidoDeArquivo() {
        assertEquals("[]", EventValidator.validateParsed(lido(LINHA)).toString());
        assertTrue(EventValidator.validateParsed(lido(LINHA.replace("0a1b2c3d-", "0a1b2c3d")))
                .toString().contains("event_id não é um UUID"));
        assertTrue(EventValidator.validateParsed(lido(LINHA.replace("18:29:27.452", "18h29")))
                .toString().contains("timestamp inválido"));
        assertTrue(EventValidator.validateParsed(lido(LINHA.replace("\"sequence_number\":11", "\"sequence_number\":\"11\"")))
                .toString().contains("sequence_number não é um número inteiro"));
        assertTrue(EventValidator.validateParsed(lido(LINHA.replace("\"1.0\"", "\"2.0\"")))
                .toString().contains("schema_version"));
    }

    @Test
    public void versaoMaisNovaToleraCamposETiposDesconhecidos() {
        String nova = LINHA.replace("\"1.0\"", "\"1.1\"");
        assertEquals("[]", EventValidator.validateParsed(lido(nova.replace("{\"side\":\"left\"}",
                "{\"side\":\"left\",\"x\":1}"))).toString());
        assertEquals("[]", EventValidator.validateParsed(lido(nova.replace("CLICK_IGNORED", "NOVO_TIPO"))).toString());
        assertTrue(EventValidator.validateParsed(lido(LINHA.replace("CLICK_IGNORED", "NOVO_TIPO")))
                .toString().contains("event_type desconhecido"));
    }
}
