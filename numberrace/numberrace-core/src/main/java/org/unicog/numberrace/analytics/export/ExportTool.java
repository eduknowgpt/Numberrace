package org.unicog.numberrace.analytics.export;

import java.io.File;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.unicog.numberrace.analytics.InteractionLogger;

/**
 * Exportador dos dados de interação (evolution/pt/exportacao-dados.md), executado fora do jogo:
 *
 * <pre>
 * java -cp "lib/*" org.unicog.numberrace.analytics.export.ExportTool [opções]
 * </pre>
 *
 * Sem opções, lê {@code <pasta do usuário>/NumberRace/v3/interaction-data} e grava o pacote em
 * {@code <pasta do usuário>/NumberRace/v3/exportacoes}. Use {@code --ajuda} para ver as opções.
 */
public final class ExportTool {

    static final int OK = 0;
    static final int ERROR = 1;
    static final int PROBLEMS = 2;

    private ExportTool() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out));
    }

    /** Executa o exportador e devolve o código de saída (0 = ok, 1 = erro, 2 = verificação com problemas). */
    static int run(String[] args, PrintStream out) {
        File appDir = new File(System.getProperty("user.home"), "NumberRace" + File.separator + "v3");
        List<File> inputs = new ArrayList<File>();
        File output = new File(appDir, "exportacoes");
        File verify = null;
        boolean zip = false;
        ExportFilter filter = new ExportFilter();
        try {
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                if (a.equals("--ajuda") || a.equals("--help") || a.equals("-h")) {
                    out.println(help());
                    return OK;
                } else if (a.equals("--entrada")) {
                    inputs.add(new File(value(args, ++i, a)));
                } else if (a.equals("--saida")) {
                    output = new File(value(args, ++i, a));
                } else if (a.equals("--participante")) {
                    filter.addParticipant(value(args, ++i, a));
                } else if (a.equals("--sessao")) {
                    filter.addSession(value(args, ++i, a));
                } else if (a.equals("--de")) {
                    filter.setFrom(date(value(args, ++i, a)));
                } else if (a.equals("--ate")) {
                    filter.setTo(date(value(args, ++i, a)));
                } else if (a.equals("--zip")) {
                    zip = true;
                } else if (a.equals("--verificar")) {
                    verify = new File(value(args, ++i, a));
                } else {
                    throw new IllegalArgumentException("opção desconhecida: " + a);
                }
            }
        } catch (IllegalArgumentException e) {
            out.println("Erro: " + e.getMessage());
            out.println("Use --ajuda para ver as opções.");
            return ERROR;
        }

        if (verify != null) {
            return verify(verify, out);
        }
        if (inputs.isEmpty()) {
            inputs.add(new File(appDir, InteractionLogger.DATA_DIR));
        }

        out.println("Number Race - exportação dos dados de interação");
        for (File in : inputs) {
            out.println("Lendo: " + in.getAbsolutePath());
        }
        try {
            return export(inputs, output, filter, zip, out);
        } catch (OutOfMemoryError e) {
            out.println();
            out.println("Erro: memória insuficiente para exportar tantos dados de uma vez. Exporte por partes "
                    + "(--participante, --de, --ate) ou execute o Java com mais memória (por exemplo, java -Xmx1g ...).");
            return ERROR;
        }
    }

    private static int export(List<File> inputs, File output, ExportFilter filter, boolean zip, PrintStream out) {
        EventImporter importer = new EventImporter();
        List<SessionData> all = importer.importFrom(inputs);
        ImportReport report = importer.getReport();
        List<SessionData> selected = filter.apply(all);
        out.println();
        out.println(report.toText());
        out.println("Filtros: " + filter.describeText());
        out.println("Sessões encontradas: " + all.size() + "; selecionadas: " + selected.size());
        if (selected.isEmpty()) {
            out.println();
            out.println("Nenhuma sessão para exportar. Confira a pasta de entrada e os filtros.");
            return ERROR;
        }
        List<IntegrityChecker.Finding> findings = IntegrityChecker.check(selected, importer.getSessionsPerFile());
        out.println(IntegrityChecker.summary(selected.size(), findings)
                + (findings.isEmpty() ? "." : " (detalhes em relatorio-integridade.txt)."));
        try {
            PackageWriter.Result result = new PackageWriter().write(output, selected, report, findings, filter,
                    all.size() - selected.size(), zip);
            out.println();
            for (String w : result.warnings) {
                out.println("Aviso: " + w);
            }
            List<String> problems = PackageVerifier.verify(result.folder);
            if (problems.isEmpty()) {
                out.println("Pacote conferido: todos os arquivos batem com o manifest.json.");
            } else {
                out.println("Problemas na conferência do pacote:");
                for (String p : problems) {
                    out.println("- " + p);
                }
            }
            out.println("Pacote gravado em: " + result.folder.getAbsolutePath());
            if (result.zip != null) {
                out.println("Arquivo .zip: " + result.zip.getAbsolutePath());
            }
            return problems.isEmpty() ? OK : PROBLEMS;
        } catch (Exception e) {
            out.println("Erro ao gravar o pacote: " + e);
            return ERROR;
        }
    }

    private static int verify(File folder, PrintStream out) {
        out.println("Conferindo o pacote: " + folder.getAbsolutePath());
        List<String> problems = PackageVerifier.verify(folder);
        if (problems.isEmpty()) {
            out.println("Pacote íntegro: todos os arquivos batem com o manifest.json.");
            return OK;
        }
        out.println("Problemas encontrados:");
        for (String p : problems) {
            out.println("- " + p);
        }
        return PROBLEMS;
    }

    private static String value(String[] args, int i, String option) {
        if (i >= args.length || args[i].startsWith("--")) {
            throw new IllegalArgumentException(option + " precisa de um valor");
        }
        return args[i];
    }

    private static LocalDate date(String s) {
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("data inválida (use AAAA-MM-DD): " + s);
        }
    }

    static String help() {
        return "Number Race - exportação dos dados de interação\n"
                + "\n"
                + "Sem opções: lê <pasta do usuário>\\NumberRace\\v3\\interaction-data e grava o pacote em\n"
                + "<pasta do usuário>\\NumberRace\\v3\\exportacoes.\n"
                + "\n"
                + "Opções:\n"
                + "  --entrada <pasta>        pasta com os arquivos .jsonl (pode repetir, para juntar\n"
                + "                           pastas copiadas de vários computadores)\n"
                + "  --saida <pasta>          onde criar a pasta do pacote\n"
                + "  --participante <código>  exporta só as sessões deste participante (pode repetir)\n"
                + "  --sessao <id>            exporta só esta sessão (id completo ou o começo; pode repetir)\n"
                + "  --de AAAA-MM-DD          só sessões iniciadas a partir desta data\n"
                + "  --ate AAAA-MM-DD         só sessões iniciadas até esta data\n"
                + "  --zip                    também gera um arquivo .zip com o pacote\n"
                + "  --verificar <pasta>      confere um pacote já gerado com o seu manifest.json\n"
                + "  --ajuda                  mostra esta ajuda";
    }
}
