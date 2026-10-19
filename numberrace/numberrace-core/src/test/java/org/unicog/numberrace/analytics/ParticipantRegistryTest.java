package org.unicog.numberrace.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ParticipantRegistryTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void mesmoAlunoRecebeOMesmoCodigo() throws Exception {
        File dir = tmp.newFolder("identification");
        String a = new ParticipantRegistry(dir).codeFor("Silva", "Ana");
        String b = new ParticipantRegistry(dir).codeFor("Silva", "Ana");
        String c = new ParticipantRegistry(dir).codeFor("Souza", "Ana");
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertTrue(a.matches("P[2-9A-Z]{6}"));
    }

    @Test
    public void nomesComVirgulaEAspasSaoPreservados() {
        String[] row = ParticipantRegistry.parse(ParticipantRegistry.quote("P1") + ","
                + ParticipantRegistry.quote("D'Ávila, \"Zé\"") + "," + ParticipantRegistry.quote("Ana"));
        assertEquals("D'Ávila, \"Zé\"", row[1]);
        assertEquals("Ana", row[2]);
    }

    @Test
    public void codigosSaoUnicosEUsamSoOAlfabetoSemAmbiguidade() throws Exception {
        ParticipantRegistry r = new ParticipantRegistry(tmp.newFolder("identification"));
        Set<String> codes = new HashSet<String>();
        for (int i = 0; i < 300; i++) {
            String code = r.codeFor("Aluno", "Número " + i);
            assertTrue(code, code.matches("P[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{6}"));
            codes.add(code);
        }
        assertEquals(300, codes.size());
    }

    @Test
    public void espacosNasPontasDoNomeNaoMudamOCodigo() throws Exception {
        File dir = tmp.newFolder("identification");
        assertEquals(new ParticipantRegistry(dir).codeFor("Silva", "Ana"),
                new ParticipantRegistry(dir).codeFor("  Silva ", "Ana  "));
    }

    @Test
    public void codigoCadastradoAMaoPelaEquipeEUsado() throws Exception {
        File dir = tmp.newFolder("identification");
        grava(new File(dir, ParticipantRegistry.FILE_NAME), "participant_id,last_name,first_name,created_at\n"
                + "\"P017\",\"Silva\",\"Ana\",\"2026-10-01T10:00:00.000-03:00\"\n", "UTF-8", false);
        assertEquals("P017", new ParticipantRegistry(dir).codeFor("Silva", "Ana"));
    }

    @Test
    public void arquivoSalvoPeloExcelEmPortuguesContinuaValendo() throws Exception {
        // P11: o Excel em português salva com ponto e vírgula e na codificação Windows-1252
        File dir = tmp.newFolder("identification");
        File f = new File(dir, ParticipantRegistry.FILE_NAME);
        grava(f, "participant_id;last_name;first_name;created_at\r\nP017;Conceição;João;01/10/2026 10:00\r\n",
                "windows-1252", false);
        ParticipantRegistry r = new ParticipantRegistry(dir);
        assertEquals("P017", r.codeFor("Conceição", "João"));
        // depois de um cadastro novo (gravado em UTF-8, com vírgula), as duas formas convivem
        String nova = r.codeFor("Araújo", "Lúcia");
        assertNotEquals("P017", nova);
        assertEquals("P017", new ParticipantRegistry(dir).codeFor("Conceição", "João"));
        assertEquals(nova, new ParticipantRegistry(dir).codeFor("Araújo", "Lúcia"));
    }

    @Test
    public void arquivoSalvoComoCsvUtf8DoExcelContinuaValendo() throws Exception {
        File dir = tmp.newFolder("identification");
        grava(new File(dir, ParticipantRegistry.FILE_NAME),
                "participant_id;last_name;first_name;created_at\r\nP020;Conceição;João;01/10/2026 10:00\r\n",
                "UTF-8", true);
        assertEquals("P020", new ParticipantRegistry(dir).codeFor("Conceição", "João"));
    }

    @Test
    public void pastaImpossivelNaoImpedeOCodigo() throws Exception {
        File naoEPasta = tmp.newFile("identification");
        String code = new ParticipantRegistry(naoEPasta).codeFor("Silva", "Ana");
        assertTrue(code.matches("P[2-9A-Z]{6}"));
    }

    private static void grava(File f, String text, String charset, boolean bom) throws Exception {
        OutputStream out = new FileOutputStream(f);
        try {
            if (bom) {
                out.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
            }
            out.write(text.getBytes(Charset.forName(charset)));
        } finally {
            out.close();
        }
    }
}
