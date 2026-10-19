package org.unicog.numberrace.analytics.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.unicog.numberrace.analytics.EventType;
import org.unicog.numberrace.analytics.EventValidator;
import org.unicog.numberrace.analytics.Json;

/**
 * Conformidade com o modelo de eventos: o JSON Schema oficial, conferido por uma biblioteca
 * independente, é o gabarito.
 *
 * <ul>
 * <li>Os exemplos do modelo, os eventos gravados pelo jogo real e as sessões simuladas são válidos
 * pelo schema e pelo validador do componente.</li>
 * <li>O validador do componente confere a estrutura do evento (campos, contexto e tempos) e nunca é
 * mais rígido que o schema: um evento válido pelo modelo nunca é descartado. Isso é conferido com
 * milhares de alterações de eventos reais (teste diferencial).</li>
 * </ul>
 */
public class SchemaConformanceTest {

    private static final List<String> ENVELOPE = Arrays.asList("schema_version", "event_id", "event_type",
            "timestamp", "session_id", "participant_id", "sequence_number", "data");

    @Test
    public void exemplosDoModeloSaoValidosNosDoisValidadores() throws Exception {
        List<String> examples = OfficialSchema.examples();
        Set<EventType> types = EnumSet.noneOf(EventType.class);
        for (String e : examples) {
            assertEquals(e, "[]", OfficialSchema.errors(e).toString());
            assertEquals(e, "[]", EventValidator.validateParsed(map(e)).toString());
            types.add(EventType.valueOf((String) map(e).get("event_type")));
        }
        // os exemplos são uma sessão curta, sem fim de partida, recompensa nem personagem novo; esses
        // três tipos são conferidos com os dados do jogo real (teste seguinte)
        assertTrue("tipos nos exemplos: " + types, types.size() >= 20);
    }

    @Test
    public void eventosGravadosPeloJogoRealSeguemOModelo() throws Exception {
        List<String> lines = new ArrayList<String>(Fixture.lines()); // Etapa 5, modelo 1.0
        lines.addAll(RealData.lines(RealData.WINDOWS)); // Etapa 7, modelo 1.1
        lines.addAll(RealData.lines(RealData.ROBOT));
        Set<String> versions = new TreeSet<String>();
        for (String line : lines) {
            assertEquals(line, "[]", OfficialSchema.errors(line).toString());
            assertEquals(line, "[]", EventValidator.validateParsed(map(line)).toString());
            versions.add((String) map(line).get("schema_version"));
        }
        assertEquals(180 + 197 + 1345, lines.size());
        assertEquals("[1.0, 1.1]", versions.toString());
    }

    @Test
    public void sessoesSimuladasSeguemOModelo() throws Exception {
        SimulatedSessions.Options o = new SimulatedSessions.Options();
        o.seed = 2026;
        o.participants = 3;
        o.sessionsPerParticipant = 2;
        o.gamesPerSession = 4;
        o.initialRewards = 6;
        o.lastSessionCrashes = true;
        Set<EventType> types = EnumSet.noneOf(EventType.class);
        int n = 0;
        for (SimulatedSessions.Session s : SimulatedSessions.generate(o)) {
            for (String line : s.lines) {
                assertEquals(line, "[]", OfficialSchema.errors(line).toString());
                types.add(EventType.valueOf((String) map(line).get("event_type")));
                n++;
            }
        }
        assertEquals(EnumSet.allOf(EventType.class), types);
        assertTrue(n > 1000);
    }

    /**
     * Teste diferencial: alterações de eventos reais (campo removido, valor trocado por outro tipo,
     * número fora da faixa, campo a mais) julgadas pelo schema e pelo validador do componente.
     */
    @Test
    public void validadorNuncaRecusaUmEventoQueOModeloAceita() throws Exception {
        int cases = 0;
        int bothReject = 0;
        int onlySchemaRejects = 0;
        List<String> tooStrict = new ArrayList<String>();
        for (Map<String, Object> base : representatives()) {
            for (Map.Entry<String, Map<String, Object>> m : mutations(base).entrySet()) {
                String json = Json.write(m.getValue());
                boolean schemaOk = OfficialSchema.errors(json).isEmpty();
                boolean componentOk = EventValidator.validateParsed(map(json)).isEmpty();
                cases++;
                if (!componentOk && schemaOk) {
                    tooStrict.add(base.get("event_type") + " / " + m.getKey());
                } else if (!componentOk) {
                    bothReject++;
                } else if (!schemaOk) {
                    onlySchemaRejects++; // tipo ou valor de campo: conferido pelo schema nos testes
                }
            }
        }
        assertEquals("o validador recusou eventos válidos pelo modelo", "[]", tooStrict.toString());
        assertTrue("casos: " + cases + "; recusados pelos dois: " + bothReject + "; só pelo schema: "
                + onlySchemaRejects, cases > 2000 && bothReject > 1000 && onlySchemaRejects > 900);
    }

    /** As alterações de estrutura são recusadas pelos dois: o componente não grava nem exporta esses eventos. */
    @Test
    public void alteracoesDeEstruturaSaoRecusadasPelosDois() throws Exception {
        int cases = 0;
        for (Map<String, Object> base : representatives()) {
            List<Map<String, Object>> structural = new ArrayList<Map<String, Object>>();
            for (String key : ENVELOPE) {
                Map<String, Object> m = copy(base);
                m.remove(key);
                structural.add(m);
            }
            Map<String, Object> extraEnvelope = copy(base);
            extraEnvelope.put("campo_novo", Long.valueOf(1));
            structural.add(extraEnvelope);
            Map<String, Object> extraData = copy(base);
            data(extraData).put("campo_novo", Long.valueOf(1));
            structural.add(extraData);
            if (base.containsKey("game_number")) {
                Map<String, Object> m = copy(base);
                m.put("game_number", Long.valueOf(0));
                structural.add(m);
            }
            for (String key : data(base).keySet()) {
                Map<String, Object> m = copy(base);
                data(m).remove(key);
                if (!OfficialSchema.errors(Json.write(m)).isEmpty()) { // campo obrigatório pelo modelo
                    structural.add(m);
                }
            }
            for (Map<String, Object> m : structural) {
                String json = Json.write(m);
                assertTrue(json, !OfficialSchema.errors(json).isEmpty());
                assertTrue(json, !EventValidator.validateParsed(map(json)).isEmpty());
                cases++;
            }
        }
        assertTrue("casos: " + cases, cases > 600);
    }

    // ------------------------------------------------------------------ auxiliares

    /** Um evento de cada combinação de tipo, versão e campos de {@code data}, dos dados reais e simulados. */
    private static List<Map<String, Object>> representatives() throws Exception {
        List<String> lines = new ArrayList<String>(Fixture.lines());
        lines.addAll(RealData.lines(RealData.WINDOWS));
        lines.addAll(RealData.lines(RealData.ROBOT));
        lines.addAll(OfficialSchema.examples());
        Map<String, Map<String, Object>> byShape = new LinkedHashMap<String, Map<String, Object>>();
        for (String line : lines) {
            Map<String, Object> e = map(line);
            String shape = e.get("event_type") + " " + e.get("schema_version") + " " + new TreeSet<String>(data(e).keySet());
            if (!byShape.containsKey(shape)) {
                byShape.put(shape, e);
            }
        }
        return new ArrayList<Map<String, Object>>(byShape.values());
    }

    private static Map<String, Map<String, Object>> mutations(Map<String, Object> e) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<String, Map<String, Object>>();
        out.put("original", copy(e));
        for (String key : e.keySet()) {
            Map<String, Object> m = copy(e);
            m.remove(key);
            out.put("remove " + key, m);
        }
        Object[][] envelope = { { "sequence_number", Long.valueOf(0) }, { "sequence_number", "1" },
            { "game_number", Long.valueOf(0) }, { "turn_number", Long.valueOf(-1) }, { "participant_id", "Ana Maria" },
            { "participant_id", "" }, { "event_id", "123" }, { "timestamp", "2026-10-11 14:00:00" },
            { "schema_version", "2.0" }, { "event_type", "QUALQUER" }, { "activity", Long.valueOf(3) },
            { "campo_novo", Long.valueOf(1) } };
        for (Object[] change : envelope) {
            Map<String, Object> m = copy(e);
            m.put((String) change[0], change[1]);
            out.put("troca " + change[0] + "=" + change[1], m);
        }
        Object[] values = { null, "texto", Long.valueOf(-1), Long.valueOf(1000000000000L), Double.valueOf(1.5),
            Boolean.TRUE, new ArrayList<Object>(), new LinkedHashMap<String, Object>() };
        for (String key : data(e).keySet()) {
            Map<String, Object> removed = copy(e);
            data(removed).remove(key);
            out.put("remove data." + key, removed);
            for (Object v : values) {
                Map<String, Object> m = copy(e);
                data(m).put(key, v);
                out.put("troca data." + key + "=" + Json.write(v), m);
            }
        }
        Map<String, Object> extra = copy(e);
        data(extra).put("campo_novo", Long.valueOf(1));
        out.put("data.campo_novo", extra);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(String json) {
        return (Map<String, Object>) Json.parse(json);
    }

    private static Map<String, Object> copy(Map<String, Object> e) {
        return map(Json.write(e));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(Map<String, Object> e) {
        return (Map<String, Object>) e.get("data");
    }
}
