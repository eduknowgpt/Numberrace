package org.unicog.numberrace.analytics.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.unicog.numberrace.analytics.Json;

/**
 * Verificação de integridade na sessão real do teste no Windows e em cópias dela estragadas de
 * propósito (validacao-registros.md, seção dos estragos): cada estrago tem de ser percebido pela
 * regra certa, e a sessão original não pode gerar nenhum problema (nenhum falso alarme).
 */
public class IntegrityCheckerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** A sessão de teste como lista de eventos que podem ser alterados. */
    private static List<Map<String, Object>> events() throws Exception {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (String line : Fixture.lines()) {
            list.add(parse(line));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String line) {
        return (Map<String, Object>) Json.parse(line);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(Map<String, Object> event) {
        return (Map<String, Object>) event.get("data");
    }

    private static Map<String, Object> bySequence(List<Map<String, Object>> events, int seq) {
        for (Map<String, Object> e : events) {
            if (((Number) e.get("sequence_number")).intValue() == seq) {
                return e;
            }
        }
        throw new AssertionError("evento " + seq + " não encontrado");
    }

    /** Remove o evento e renumera os seguintes, como se o jogo nunca o tivesse registrado. */
    private static void removeAndRenumber(List<Map<String, Object>> events, int seq) {
        events.remove(bySequence(events, seq));
        for (Map<String, Object> e : events) {
            int n = ((Number) e.get("sequence_number")).intValue();
            if (n > seq) {
                e.put("sequence_number", Long.valueOf(n - 1));
            }
        }
    }

    private static List<String> lines(List<Map<String, Object>> events) {
        List<String> lines = new ArrayList<String>();
        for (Map<String, Object> e : events) {
            lines.add(Json.write(e));
        }
        return lines;
    }

    private static final class Result {
        EventImporter importer;
        List<SessionData> sessions;
        List<IntegrityChecker.Finding> findings;

        Set<String> rules() {
            Set<String> ids = new HashSet<String>();
            for (IntegrityChecker.Finding f : findings) {
                ids.add(f.rule.id);
            }
            return ids;
        }
    }

    private Result check(List<String> lines, boolean finalNewline) throws Exception {
        Result r = new Result();
        r.importer = new EventImporter();
        File dir = Fixture.computer(tmp.newFolder(), lines, finalNewline);
        r.sessions = r.importer.importFrom(Collections.singletonList(dir));
        r.findings = IntegrityChecker.check(r.sessions, r.importer.getSessionsPerFile());
        return r;
    }

    private Result check(List<Map<String, Object>> events) throws Exception {
        return check(lines(events), true);
    }

    private static void assertOnly(Result r, String... rules) {
        assertEquals(describe(r), new HashSet<String>(Arrays.asList(rules)), r.rules());
    }

    private static String describe(Result r) {
        StringBuilder sb = new StringBuilder();
        for (IntegrityChecker.Finding f : r.findings) {
            sb.append(f.rule.id).append(' ').append(f.message).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void sessaoRealPassaEmTodasAsRegras() throws Exception {
        Result r = check(Fixture.lines(), true);
        assertEquals(describe(r), 0, r.findings.size());
        assertEquals(21, IntegrityChecker.RULES.size());
    }

    @Test
    public void e01EventoApagadoDoMeio() throws Exception {
        List<Map<String, Object>> ev = events();
        ev.remove(bySequence(ev, 26)); // MOVE do adversário na rodada 1
        Result r = check(ev);
        assertTrue(describe(r), r.rules().contains("R01"));
        assertTrue(describe(r), r.rules().contains("R13")); // a peça "pula" da casa 0 para a 3
    }

    @Test
    public void e02e03CopiaRepetidaEConflito() throws Exception {
        List<String> lines = Fixture.lines();
        lines.add(lines.get(40)); // cópia idêntica: ignorada sem alarme
        Map<String, Object> other = parse(lines.get(41));
        other.put("event_id", "00000000-0000-4000-8000-000000000001"); // outro evento com a mesma sequência
        lines.add(Json.write(other));
        Result r = check(lines, true);
        assertEquals(1, r.importer.getReport().duplicates);
        assertEquals(1, r.importer.getReport().conflicts.size());
        assertOnly(r);
    }

    @Test
    public void e04UltimaLinhaCortada() throws Exception {
        List<String> lines = Fixture.lines();
        String last = lines.remove(lines.size() - 1);
        lines.add(last.substring(0, last.length() / 2));
        Result r = check(lines, false);
        assertEquals(1, r.importer.getReport().incompleteLines.size());
        assertOnly(r, "R04");
    }

    @Test
    public void e05SemSessionEnd() throws Exception {
        List<Map<String, Object>> ev = events();
        ev.remove(ev.size() - 1);
        assertOnly(check(ev), "R04");
    }

    @Test
    public void e06EventoDeOutraSessaoNoArquivo() throws Exception {
        List<Map<String, Object>> ev = events();
        Map<String, Object> intruder = parse(Json.write(bySequence(ev, 30)));
        intruder.put("session_id", "11111111-1111-4111-8111-111111111111");
        intruder.put("event_id", "22222222-2222-4222-8222-222222222222");
        ev.add(intruder);
        Result r = check(ev);
        assertEquals(2, r.sessions.size());
        assertTrue(describe(r), r.rules().containsAll(Arrays.asList("R05", "R03", "R04")));
    }

    @Test
    public void e07HorarioQueVolta() throws Exception {
        List<Map<String, Object>> ev = events();
        bySequence(ev, 100).put("timestamp", "2026-10-09T18:20:00.000-03:00");
        assertOnly(check(ev), "R02");
    }

    @Test
    public void e08RodadaSemResposta() throws Exception {
        List<Map<String, Object>> ev = events();
        removeAndRenumber(ev, 33); // ANSWER da rodada 2
        assertOnly(check(ev), "R07");
    }

    @Test
    public void e09TempoDeRespostaAlterado() throws Exception {
        List<Map<String, Object>> ev = events();
        data(bySequence(ev, 16)).put("response_time_ms", Long.valueOf(1484)); // era 1584
        assertOnly(check(ev), "R08");
    }

    @Test
    public void e10PecaTeletransportada() throws Exception {
        List<Map<String, Object>> ev = events();
        data(bySequence(ev, 56)).put("from_square", Long.valueOf(1)); // estava na casa 2
        data(bySequence(ev, 56)).put("steps", Long.valueOf(3));
        // percebido duas vezes: a peça não estava lá, e o avanço não confere com o resultado da rodada
        assertOnly(check(ev), "R13", "R15");
    }

    @Test
    public void e11FimDePartidaComRodadasErradas() throws Exception {
        List<Map<String, Object>> ev = events();
        for (Map<String, Object> e : ev) {
            if ("GAME_END".equals(e.get("event_type"))) {
                data(e).put("turns_played", Long.valueOf(8));
            }
        }
        assertOnly(check(ev), "R19");
    }

    @Test
    public void e12TempoNegativoEInvalido() throws Exception {
        List<Map<String, Object>> ev = events();
        data(bySequence(ev, 16)).put("response_time_ms", Long.valueOf(-5));
        Result r = check(ev);
        assertEquals(1, r.importer.getReport().invalidEvents.size());
        assertTrue(describe(r), r.rules().contains("R01")); // o evento inválido é descartado: vira lacuna
    }

    @Test
    public void e13AcertoInvertido() throws Exception {
        List<Map<String, Object>> ev = events();
        data(bySequence(ev, 16)).put("correct_larger", Boolean.TRUE); // escolheu 1 contra 3
        assertOnly(check(ev), "R17");
    }

    @Test
    public void e14ContaErrada() throws Exception {
        List<Map<String, Object>> ev = events();
        data(bySequence(ev, 30)).put("value", Long.valueOf(2)); // 1 + 0
        Result r = check(ev);
        assertTrue(describe(r), r.rules().contains("R18"));
    }

    @Test
    public void e15FimDeSessaoIncoerente() throws Exception {
        List<Map<String, Object>> ev = events();
        data(ev.get(ev.size() - 1)).put("game_in_progress", Boolean.FALSE); // a partida 2 não terminou
        assertOnly(check(ev), "R20");
    }

    @Test
    public void e16AjusteDeDificuldadeErrado() throws Exception {
        List<Map<String, Object>> ev = events();
        data(bySequence(ev, 46)).put("next_turn_number", Long.valueOf(4)); // anuncia a rodada 3
        assertOnly(check(ev), "R16");
    }

    @Test
    public void e17RetomadaSemPausa() throws Exception {
        List<Map<String, Object>> ev = events();
        removeAndRenumber(ev, 21); // PAUSE pela tecla F8
        assertOnly(check(ev), "R12");
    }

    @Test
    public void e18RodadaNumeradaErrada() throws Exception {
        List<Map<String, Object>> ev = events();
        for (Map<String, Object> e : ev) {
            if (Long.valueOf(1).equals(e.get("game_number")) && Long.valueOf(5).equals(e.get("turn_number"))) {
                e.put("turn_number", Long.valueOf(6));
            }
        }
        Result r = check(ev);
        assertTrue(describe(r), r.rules().contains("R06"));
    }
}
