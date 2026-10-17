package org.unicog.numberrace.analytics.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Arquivos de eventos como podem chegar de verdade: editados ou copiados por programas diferentes,
 * corrompidos, com linhas estranhas. A importação nunca quebra: aproveita o que é válido e relata o
 * resto.
 */
public class ImportEdgeCasesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void bomFimDeLinhaDoWindowsELinhasEmBrancoNaoAtrapalham() throws Exception {
        StringBuilder sb = new StringBuilder("﻿");
        for (String line : Fixture.lines()) {
            sb.append(line).append("\r\n").append("   \r\n");
        }
        EventImporter importer = new EventImporter();
        List<SessionData> sessions = importer.importFrom(Arrays.asList(write("sessao.jsonl", sb.toString())));
        assertEquals(180, sessions.get(0).getEvents().size());
        assertEquals(0, importer.getReport().problemCount());
    }

    @Test
    public void linhasEstranhasSaoRelatadasEORestoEAproveitado() throws Exception {
        List<String> lines = Fixture.lines();
        StringBuilder huge = new StringBuilder("{\"x\":\"");
        for (int i = 0; i < 1000000; i++) {
            huge.append('a');
        }
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            deep.append('[');
        }
        List<String> mixed = new ArrayList<String>(lines.subList(0, 90));
        mixed.add("\u0000\u0001\u0002 lixo binário ��");
        mixed.add(huge.append("\"}").toString()); // 1 MB, JSON válido mas não é evento
        mixed.add(deep.toString()); // P12: antes, estourava a pilha e derrubava a exportação
        mixed.add("[1,2,3]");
        mixed.addAll(lines.subList(90, lines.size()));
        EventImporter importer = new EventImporter();
        List<SessionData> sessions = importer.importFrom(Arrays.asList(Fixture.computer(tmp.newFolder(), mixed, true)));
        ImportReport r = importer.getReport();
        assertEquals(180, sessions.get(0).getEvents().size());
        assertEquals(3, r.incompleteLines.size()); // lixo, aninhamento exagerado e lista solta
        assertEquals(1, r.invalidEvents.size()); // o objeto de 1 MB
    }

    @Test
    public void umEventoComPartidaZeroNaoImpedeOBancoDoPacote() throws Exception {
        // P10: o banco do pacote recusa partida 0, e antes o pacote inteiro saía sem banco
        List<String> lines = Fixture.lines();
        String bad = lines.get(20).replace("\"game_number\":1,", "\"game_number\":0,")
                .replace(Fixture.SESSION_ID, "00000000-0000-4000-8000-000000000000")
                .replaceFirst("\"event_id\":\"[0-9a-f-]{36}\"", "\"event_id\":\"11111111-2222-4333-8444-555555555555\"");
        assertTrue(bad.contains("\"game_number\":0,"));
        File in = Fixture.computer(tmp.newFolder(), lines, true);
        FileOutputStream out = new FileOutputStream(new File(in, "events/" + Fixture.PARTICIPANT + "/outra.jsonl"));
        out.write((bad + "\n").getBytes(Fixture.UTF8));
        out.close();
        File pkgRoot = tmp.newFolder();
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        assertEquals(ExportTool.OK, ExportTool.run(new String[] { "--entrada", in.getPath(), "--saida", pkgRoot.getPath() },
                new PrintStream(console, true, "UTF-8")));
        File pkg = pkgRoot.listFiles()[0];
        assertTrue(console.toString("UTF-8"), new File(pkg, "interaction.db").isFile());
        String report = new String(EventImporter.readAll(new File(pkg, "relatorio-importacao.txt")), "UTF-8");
        assertTrue(report, report.contains("game_number menor que 1"));
    }

    @Test
    public void pastaDeEntradaInexistenteEInformadaPeloCaminho() throws Exception {
        EventImporter importer = new EventImporter();
        File missing = new File(tmp.getRoot(), "pendrive/interaction-data");
        importer.importFrom(Arrays.asList(missing));
        assertEquals("[" + missing.getPath() + ": pasta ou arquivo não encontrado]",
                importer.getReport().fileErrors.toString());
    }

    @Test
    public void aPastaDeIdentificacaoNuncaELidaMesmoDentroDeOutraPasta() throws Exception {
        File root = tmp.newFolder();
        File in = Fixture.computer(root, Fixture.lines(), true);
        File identification = new File(in, "copia/Identification");
        identification.mkdirs();
        OutputStream out = new FileOutputStream(new File(identification, "nomes.jsonl"));
        out.write("{\"participant_id\":\"P4WVSUY\",\"nome\":\"Fulano\"}\n".getBytes(Fixture.UTF8));
        out.close();
        EventImporter importer = new EventImporter();
        importer.importFrom(Arrays.asList(in));
        assertEquals(1, importer.getReport().filesRead);
        assertEquals(0, importer.getReport().problemCount());
    }

    private File write(String name, String text) throws Exception {
        File dir = tmp.newFolder();
        OutputStream out = new FileOutputStream(new File(dir, name));
        try {
            out.write(text.getBytes(Fixture.UTF8));
        } finally {
            out.close();
        }
        return dir;
    }
}
