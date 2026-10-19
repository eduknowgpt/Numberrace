package org.unicog.numberrace.analytics.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Os arquivos CSV do pacote, nas duas versões (padrão e Excel em português): qualquer texto volta
 * igual na leitura, inclusive com separadores, aspas e quebras de linha dentro da célula.
 */
public class CsvRoundTripTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String POOL = "abc XYZ 019 ,;\"'\n\r\t=+-@ áçãõ€ {}[]:";

    @Test
    public void textosQuaisquerVoltamIguaisNasDuasVersoes() throws Exception {
        Random r = new Random(11);
        List<List<String>> expected = new ArrayList<List<String>>();
        Table t = new Table("teste", "teste");
        t.column("a", Table.Type.TEXT, "", "", "").column("b", Table.Type.TEXT, "", "", "")
                .column("c", Table.Type.TEXT, "", "", "");
        for (int i = 0; i < 500; i++) {
            List<String> row = Arrays.asList(text(r), text(r), text(r));
            Map<String, Object> m = t.newRow();
            m.put("a", row.get(0));
            m.put("b", row.get(1));
            m.put("c", row.get(2));
            expected.add(row);
        }
        for (CsvWriter.Flavor flavor : CsvWriter.Flavor.values()) {
            File f = tmp.newFile(flavor + ".csv");
            assertEquals(500, CsvWriter.write(t, f, flavor));
            List<List<String>> read = CsvReader.read(f, flavor.separator);
            assertEquals(Arrays.asList("a", "b", "c"), read.get(0));
            assertEquals(flavor.toString(), expected, read.subList(1, read.size()));
        }
    }

    @Test
    public void numerosEValoresAusentesNasDuasVersoes() {
        assertEquals("0.431", CsvWriter.format(Double.valueOf(0.431), CsvWriter.Flavor.STANDARD));
        assertEquals("0,431", CsvWriter.format(Double.valueOf(0.431), CsvWriter.Flavor.EXCEL));
        // sem notação científica, que o Excel e o R leriam de jeitos diferentes
        assertEquals("100000000000000000000", CsvWriter.format(Double.valueOf(1e20), CsvWriter.Flavor.STANDARD));
        assertEquals("0,0000001", CsvWriter.format(Double.valueOf(1e-7), CsvWriter.Flavor.EXCEL));
        assertEquals("3", CsvWriter.format(Double.valueOf(3.0), CsvWriter.Flavor.STANDARD));
        assertEquals("", CsvWriter.format(Double.valueOf(Double.NaN), CsvWriter.Flavor.EXCEL));
        assertEquals("", CsvWriter.format(null, CsvWriter.Flavor.STANDARD));
        assertEquals("1584", CsvWriter.format(Long.valueOf(1584), CsvWriter.Flavor.EXCEL));
        assertEquals("true", CsvWriter.format(Boolean.TRUE, CsvWriter.Flavor.EXCEL));
    }

    @Test
    public void soAVersaoDoExcelTemBomEPontoEVirgula() throws Exception {
        Table t = new Table("teste", "teste");
        t.column("valor", Table.Type.REAL, "", "", "").column("nome", Table.Type.TEXT, "", "", "");
        Map<String, Object> m = t.newRow();
        m.put("valor", Double.valueOf(1.5));
        m.put("nome", "a;b,c");
        File std = tmp.newFile("std.csv");
        File xls = tmp.newFile("xls.csv");
        CsvWriter.write(t, std, CsvWriter.Flavor.STANDARD);
        CsvWriter.write(t, xls, CsvWriter.Flavor.EXCEL);
        byte[] a = EventImporter.readAll(std);
        byte[] b = EventImporter.readAll(xls);
        assertFalse(a[0] == (byte) 0xEF);
        assertTrue(b[0] == (byte) 0xEF && b[1] == (byte) 0xBB && b[2] == (byte) 0xBF);
        assertEquals("valor,nome\r\n1.5,\"a;b,c\"\r\n", new String(a, "UTF-8"));
        assertEquals("﻿valor;nome\r\n1,5;\"a;b,c\"\r\n", new String(b, "UTF-8"));
    }

    private static String text(Random r) {
        int n = r.nextInt(12);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(POOL.charAt(r.nextInt(POOL.length())));
        }
        return sb.toString();
    }
}
