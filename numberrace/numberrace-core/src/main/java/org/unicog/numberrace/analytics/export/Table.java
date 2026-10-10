package org.unicog.numberrace.analytics.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Uma tabela exportada (eventos, sessões ou rodadas): colunas documentadas, usadas também no
 * dicionário de dados, e linhas com os valores.
 */
final class Table {

    enum Type {
        INTEGER, REAL, BOOLEAN, TEXT
    }

    static final class Column {
        final String name;
        final Type type;
        final String unit;
        final String description;
        final String source;

        Column(String name, Type type, String unit, String description, String source) {
            this.name = name;
            this.type = type;
            this.unit = unit;
            this.description = description;
            this.source = source;
        }
    }

    private final String name;
    private final String description;
    private final List<Column> columns = new ArrayList<Column>();
    private final List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
    private Iterable<Map<String, Object>> rowSource;
    private int rowSourceCount;

    Table(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /** Nome da tabela, que também é o nome do arquivo CSV ({@code <nome>.csv}). */
    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    Table column(String columnName, Type type, String unit, String columnDescription, String source) {
        columns.add(new Column(columnName, type, unit, columnDescription, source));
        return this;
    }

    List<Column> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    /** Nova linha, vazia; os valores ausentes são exportados como célula vazia. */
    Map<String, Object> newRow() {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        rows.add(row);
        return row;
    }

    /**
     * Linhas montadas sob demanda, uma de cada vez, em vez de guardadas na memória (usado na
     * tabela de eventos, que pode ter centenas de milhares de linhas).
     */
    void setRowSource(Iterable<Map<String, Object>> source, int count) {
        rowSource = source;
        rowSourceCount = count;
    }

    Iterable<Map<String, Object>> getRows() {
        return rowSource != null ? rowSource : Collections.unmodifiableList(rows);
    }

    /** Linhas guardadas na memória (tabelas pequenas, como sessões e rodadas). */
    List<Map<String, Object>> getRowList() {
        if (rowSource != null) {
            throw new IllegalStateException("tabela " + name + " é montada sob demanda");
        }
        return Collections.unmodifiableList(rows);
    }

    int getRowCount() {
        return rowSource != null ? rowSourceCount : rows.size();
    }
}
