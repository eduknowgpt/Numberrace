package org.unicog.numberrace.analytics.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TablesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private List<SessionData> sessions() throws Exception {
        return new EventImporter().importFrom(Arrays.asList(Fixture.computer(tmp.newFolder(), Fixture.lines(), true)));
    }

    @Test
    public void umaLinhaPorRodadaComCasasContinuas() throws Exception {
        List<Map<String, Object>> rows = Tables.turns(sessions()).getRowList();
        assertEquals(11, rows.size());
        for (int i = 0; i + 1 < rows.size(); i++) {
            Map<String, Object> a = rows.get(i);
            Map<String, Object> b = rows.get(i + 1);
            if (a.get("game_number").equals(b.get("game_number"))) {
                // a casa no fim de uma rodada é a casa no início da seguinte
                assertEquals(((Number) a.get("end_square_player")).intValue(),
                        ((Number) b.get("start_square_player")).intValue());
                assertEquals(((Number) a.get("end_square_opponent")).intValue(),
                        ((Number) b.get("start_square_opponent")).intValue());
            }
        }
        Map<String, Object> win = rows.get(8);
        assertEquals(Boolean.TRUE, win.get("game_ended"));
        assertEquals(20, ((Number) win.get("end_square_player")).intValue());
    }

    @Test
    public void contasSeparadasEmColunas() throws Exception {
        Map<String, Object> r = Tables.turns(sessions()).getRowList().get(7); // partida 1, rodada 8: 2 x (6-2)
        assertEquals(8, ((Number) r.get("turn_number")).intValue());
        assertEquals("-", r.get("right_operator"));
        assertEquals(6L, ((Number) r.get("right_operand_1")).longValue());
        assertEquals(2L, ((Number) r.get("right_operand_2")).longValue());
        assertEquals(4L, ((Number) r.get("right_value")).longValue());
        assertEquals("right", r.get("larger_side"));
        assertNull(r.get("left_operator"));
    }

    @Test
    public void prazoEsgotadoSemRespostaNemTempo() throws Exception {
        Map<String, Object> r = Tables.turns(sessions()).getRowList().get(10); // partida 2, rodada 2
        assertEquals("timeout", r.get("response"));
        assertEquals("left", r.get("assigned_side"));
        assertNull(r.get("chosen_side"));
        assertNull(r.get("correct_larger"));
        assertNull(r.get("response_time_ms"));
        assertEquals(4469L, ((Number) r.get("deadline_ms")).longValue());
    }

    @Test
    public void resumoDaSessao() throws Exception {
        Map<String, Object> s = Tables.sessions(sessions()).getRowList().get(0);
        assertEquals(Integer.valueOf(2), s.get("games_started"));
        assertEquals(Integer.valueOf(1), s.get("games_won"));
        assertEquals(Integer.valueOf(11), s.get("turns"));
        assertEquals(Integer.valueOf(10), s.get("answers"));
        assertEquals(Integer.valueOf(1), s.get("timeouts"));
        assertEquals("quit", s.get("end_reason"));
        assertEquals(Long.valueOf(549105), s.get("duration_ms"));
        assertEquals(Boolean.TRUE, s.get("complete"));
    }

    @Test
    public void eventosComAParteComumEmColunas() throws Exception {
        Table t = Tables.events(sessions());
        assertEquals(180, t.getRowCount());
        int n = 0;
        for (Map<String, Object> row : t.getRows()) {
            assertEquals(Integer.valueOf(++n), row.get("sequence_number"));
        }
        assertEquals(180, n);
        Map<String, Object> first = t.getRows().iterator().next();
        assertEquals("SESSION_START", first.get("event_type"));
        assertEquals(Integer.valueOf(1), first.get("sequence_number"));
        assertEquals("{\"session_number\":1,\"start_level\":\"intermediate\",\"start_complexity_level\":8,"
                + "\"language\":\"pt\",\"app_version\":\"3.1.0\"}", first.get("data_json"));
    }
}
