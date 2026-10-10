package org.unicog.numberrace.analytics.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.zip.ZipFile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ExportToolTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final ByteArrayOutputStream console = new ByteArrayOutputStream();

    private int run(String... args) throws Exception {
        return ExportTool.run(args, new PrintStream(console, true, "UTF-8"));
    }

    private File onlyPackage(File out) {
        File[] dirs = out.listFiles();
        for (File d : dirs) {
            if (d.isDirectory()) {
                return d;
            }
        }
        throw new AssertionError("pacote não encontrado");
    }

    @Test
    public void exportaTudoEConfereOPacote() throws Exception {
        File in = Fixture.computer(tmp.newFolder(), Fixture.lines(), true);
        File out = tmp.newFolder();
        assertEquals(ExportTool.OK, run("--entrada", in.getPath(), "--saida", out.getPath(), "--zip"));
        File pkg = onlyPackage(out);
        for (String name : new String[] { "LEIA-ME.txt", "manifest.json", "relatorio-importacao.txt", "events.jsonl",
                "interaction.db", "csv/events.csv", "csv/sessions.csv", "csv/turns.csv", "csv/dicionario-dados.csv",
                "excel/events.csv", "excel/sessions.csv", "excel/turns.csv", "excel/dicionario-dados.csv" }) {
            assertTrue(name, new File(pkg, name).isFile());
        }
        assertTrue(PackageVerifier.verify(pkg).isEmpty());
        assertTrue(console.toString("UTF-8").contains("Pacote conferido"));

        // o bruto exportado é idêntico ao arquivo do jogo
        assertEquals(new String(EventImporter.readAll(new File(in, "events/" + Fixture.PARTICIPANT + "/"
                + "2026-10-09_182904_" + Fixture.SESSION_ID + ".jsonl")), Fixture.UTF8),
                new String(EventImporter.readAll(new File(pkg, "events.jsonl")), Fixture.UTF8));

        Connection c = DriverManager.getConnection("jdbc:sqlite:" + new File(pkg, "interaction.db").getPath());
        ResultSet rs = c.createStatement().executeQuery(
                "SELECT (SELECT COUNT(*) FROM event), (SELECT COUNT(*) FROM session), (SELECT COUNT(*) FROM turn), "
                        + "(SELECT COUNT(*) FROM turn WHERE response = 'timeout')");
        assertEquals(180, rs.getInt(1));
        assertEquals(1, rs.getInt(2));
        assertEquals(11, rs.getInt(3));
        assertEquals(1, rs.getInt(4));
        c.close();

        ZipFile zip = new ZipFile(new File(out, pkg.getName() + ".zip"));
        assertTrue(zip.getEntry(pkg.getName() + "/excel/turns.csv") != null);
        assertEquals(13, zip.size());
        zip.close();
    }

    @Test
    public void verificacaoAcusaArquivoAlterado() throws Exception {
        File out = tmp.newFolder();
        run("--entrada", Fixture.computer(tmp.newFolder(), Fixture.lines(), true).getPath(), "--saida", out.getPath());
        File pkg = onlyPackage(out);
        FileOutputStream f = new FileOutputStream(new File(pkg, "csv/turns.csv"), true);
        f.write("linha a mais\r\n".getBytes(Fixture.UTF8));
        f.close();
        assertEquals(ExportTool.PROBLEMS, run("--verificar", pkg.getPath()));
        String text = console.toString("UTF-8");
        assertTrue(text.contains("csv/turns.csv: impressão digital (SHA-256) diferente"));
        assertTrue(text.contains("csv/turns.csv: tamanho diferente do manifesto"));
        assertTrue(text.contains("linha 13 com 1 colunas em vez de 52"));
        assertFalse(text.contains("excel/turns.csv"));
    }

    @Test
    public void filtrosEscolhemSessoesInteiras() throws Exception {
        File in = Fixture.computer(tmp.newFolder(), Fixture.lines(), true);
        assertEquals(ExportTool.ERROR, run("--entrada", in.getPath(), "--saida", tmp.newFolder().getPath(),
                "--participante", "P999999"));
        assertEquals(ExportTool.ERROR, run("--entrada", in.getPath(), "--saida", tmp.newFolder().getPath(),
                "--de", "2026-10-10"));
        File out = tmp.newFolder();
        assertEquals(ExportTool.OK, run("--entrada", in.getPath(), "--saida", out.getPath(), "--sessao", "5FB5269A",
                "--de", "2026-10-09", "--ate", "2026-10-09", "--participante", Fixture.PARTICIPANT));
        String manifest = new String(EventImporter.readAll(new File(onlyPackage(out), "manifest.json")), Fixture.UTF8);
        assertTrue(manifest.contains("\"events\":180"));
        assertTrue(manifest.contains("\"sessions\":[\"5fb5269a\"]"));
    }

    @Test
    public void opcoesInvalidasNaoExportam() throws Exception {
        assertEquals(ExportTool.ERROR, run("--de", "09/10/2026"));
        assertEquals(ExportTool.ERROR, run("--qualquer"));
        assertEquals(ExportTool.ERROR, run("--entrada"));
        assertEquals(ExportTool.OK, run("--ajuda"));
        assertFalse(console.toString("UTF-8").contains("Pacote gravado"));
    }
}
