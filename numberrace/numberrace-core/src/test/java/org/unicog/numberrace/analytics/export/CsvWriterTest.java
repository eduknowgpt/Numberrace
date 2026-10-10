package org.unicog.numberrace.analytics.export;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CsvWriterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static Table table() {
        Table t = new Table("t", "teste");
        t.column("texto", Table.Type.TEXT, "", "", "").column("real", Table.Type.REAL, "", "", "")
                .column("inteiro", Table.Type.INTEGER, "", "", "").column("logico", Table.Type.BOOLEAN, "", "", "");
        Map<String, Object> r = t.newRow();
        r.put("texto", "a;b,\"c\"");
        r.put("real", Double.valueOf(0.4311));
        r.put("inteiro", Long.valueOf(1584));
        r.put("logico", Boolean.TRUE);
        t.newRow().put("texto", "nível");
        return t;
    }

    @Test
    public void versaoPadraoComVirgulaEPontoSemBom() throws Exception {
        File f = tmp.newFile("padrao.csv");
        CsvWriter.write(table(), f, CsvWriter.Flavor.STANDARD);
        String text = new String(EventImporter.readAll(f), Fixture.UTF8);
        assertEquals("texto,real,inteiro,logico\r\n\"a;b,\"\"c\"\"\",0.4311,1584,true\r\nnível,,,\r\n", text);
    }

    @Test
    public void versaoExcelComPontoEVirgulaEVirgulaDecimalComBom() throws Exception {
        File f = tmp.newFile("excel.csv");
        CsvWriter.write(table(), f, CsvWriter.Flavor.EXCEL);
        String text = new String(EventImporter.readAll(f), Fixture.UTF8);
        assertEquals("\uFEFFtexto;real;inteiro;logico\r\n\"a;b,\"\"c\"\"\";0,4311;1584;true\r\nnível;;;\r\n", text);
    }

    @Test
    public void leituraDevolveOsMesmosValores() throws Exception {
        File f = tmp.newFile("ida-e-volta.csv");
        CsvWriter.write(table(), f, CsvWriter.Flavor.EXCEL);
        List<List<String>> records = CsvReader.read(f, ';');
        assertEquals(3, records.size());
        assertEquals(Arrays.asList("a;b,\"c\"", "0,4311", "1584", "true"), records.get(1));
        assertEquals(Arrays.asList("nível", "", "", ""), records.get(2));
    }
}
