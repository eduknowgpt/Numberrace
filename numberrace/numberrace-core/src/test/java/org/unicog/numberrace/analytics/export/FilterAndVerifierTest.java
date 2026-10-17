package org.unicog.numberrace.analytics.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Filtros da exportação (sempre sessões inteiras) e a conferência de um pacote recebido, com os
 * dados do jogo real.
 */
public class FilterAndVerifierTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private List<SessionData> allSessions() throws Exception {
        File etapa5 = Fixture.computer(tmp.newFolder(), Fixture.lines(), true); // 09/10
        File windows = RealData.computer(tmp.newFolder(), RealData.WINDOWS); // 11/10
        File robot = RealData.computer(tmp.newFolder(), RealData.ROBOT); // 11/10
        return new EventImporter().importFrom(Arrays.asList(etapa5, windows, robot));
    }

    @Test
    public void filtroPorDataIncluiOsDoisExtremos() throws Exception {
        List<SessionData> all = allSessions();
        assertEquals(7, all.size());
        ExportFilter f = new ExportFilter();
        f.setFrom(LocalDate.of(2026, 10, 9));
        f.setTo(LocalDate.of(2026, 10, 9));
        assertEquals(1, f.apply(all).size());
        f = new ExportFilter();
        f.setFrom(LocalDate.of(2026, 10, 10));
        assertEquals(6, f.apply(all).size());
        f = new ExportFilter();
        f.setTo(LocalDate.of(2026, 10, 8));
        assertEquals(0, f.apply(all).size());
    }

    @Test
    public void filtrosPorParticipanteESessaoSeCombinam() throws Exception {
        List<SessionData> all = allSessions();
        ExportFilter f = new ExportFilter();
        f.addParticipant("P54G8G6");
        f.addParticipant(" PX43KJ8 ");
        assertEquals(4, f.apply(all).size());
        f.addSession("3A5F7974"); // começo do identificador, sem diferenciar maiúsculas
        List<SessionData> one = f.apply(all);
        assertEquals(1, one.size());
        assertEquals(1100, one.get(0).getEvents().size()); // a sessão inteira, nunca parte dela
    }

    @Test
    public void conferenciaAcusaArquivoAlteradoApagadoOuManifestoEstragado() throws Exception {
        File in = RealData.computer(tmp.newFolder(), RealData.WINDOWS);
        File out = tmp.newFolder();
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        assertEquals(ExportTool.OK, ExportTool.run(new String[] { "--entrada", in.getPath(), "--saida", out.getPath() },
                new PrintStream(console, true, "UTF-8")));
        File pkg = out.listFiles()[0];
        assertEquals("[]", PackageVerifier.verify(pkg).toString());

        // uma linha a mais no fim de uma tabela
        append(new File(pkg, "csv/turns.csv"), "x,y\r\n");
        // um arquivo apagado
        assertTrue(new File(pkg, "excel/events.csv").delete());
        List<String> problems = PackageVerifier.verify(pkg);
        assertTrue(problems.toString(), problems.toString().contains("csv/turns.csv"));
        assertTrue(problems.toString(), problems.toString().contains("excel/events.csv: arquivo ausente"));

        // manifesto estragado
        append(new File(pkg, "manifest.json"), "}{");
        assertTrue(PackageVerifier.verify(pkg).toString().contains("manifest.json ausente ou ilegível"));

        console.reset();
        assertEquals(ExportTool.PROBLEMS, ExportTool.run(new String[] { "--verificar", pkg.getPath() },
                new PrintStream(console, true, "UTF-8")));
    }

    private static void append(File f, String text) throws Exception {
        FileOutputStream out = new FileOutputStream(f, true);
        try {
            out.write(text.getBytes(Fixture.UTF8));
        } finally {
            out.close();
        }
    }
}
