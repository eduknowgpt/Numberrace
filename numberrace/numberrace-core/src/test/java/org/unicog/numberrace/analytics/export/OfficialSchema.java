package org.unicog.numberrace.analytics.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

/**
 * O JSON Schema oficial do modelo de eventos ({@code evolution/pt/modelo-eventos.schema.json}),
 * conferido por uma biblioteca independente do componente. É o "gabarito" dos testes de
 * conformidade: o componente segue o modelo se os eventos dele passam aqui.
 */
final class OfficialSchema {

    static final String SCHEMA = "evolution/pt/modelo-eventos.schema.json";
    static final String EXAMPLES = "evolution/pt/modelo-eventos-exemplos.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static JsonSchema schema;

    private OfficialSchema() {
    }

    /** Um arquivo do repositório, procurado a partir da pasta em que os testes rodam. */
    static File repositoryFile(String path) {
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (dir != null) {
            File f = new File(dir, path);
            if (f.isFile()) {
                return f;
            }
            dir = dir.getParentFile();
        }
        throw new IllegalStateException(path + " não encontrado a partir de " + System.getProperty("user.dir"));
    }

    private static synchronized JsonSchema get() throws IOException {
        if (schema == null) {
            InputStream in = new FileInputStream(repositoryFile(SCHEMA));
            try {
                schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(in,
                        SchemaValidatorsConfig.builder().formatAssertionsEnabled(Boolean.TRUE).build());
            } finally {
                in.close();
            }
        }
        return schema;
    }

    /** Os erros de um evento (uma linha JSON) segundo o schema; lista vazia significa evento válido. */
    static List<String> errors(String json) throws IOException {
        List<String> out = new ArrayList<String>();
        for (ValidationMessage m : get().validate(MAPPER.readTree(json))) {
            out.add(m.getMessage());
        }
        return out;
    }

    /** Os exemplos do modelo de eventos, um por linha JSON. */
    static List<String> examples() throws IOException {
        List<String> out = new ArrayList<String>();
        for (Object node : MAPPER.readTree(repositoryFile(EXAMPLES))) {
            out.add(node.toString());
        }
        return out;
    }
}
