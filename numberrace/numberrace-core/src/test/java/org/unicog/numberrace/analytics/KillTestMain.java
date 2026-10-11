package org.unicog.numberrace.analytics;

import java.io.File;

/**
 * Programa usado pelo teste de fechamento à força ({@link PersistenceFailureTest}): abre uma sessão
 * e registra eventos sem parar, até ser encerrado de fora (como o Gerenciador de Tarefas faz).
 */
public final class KillTestMain {

    private KillTestMain() {
    }

    public static void main(String[] args) throws Exception {
        InteractionLogger logger = new InteractionLogger();
        logger.start(new File(args[0]), true);
        logger.startSession("P017", Json.obj("session_number", 1, "start_level", "easy",
                "start_complexity_level", 1, "language", "pt", "app_version", "teste"));
        for (int i = 0;; i++) {
            logger.log(EventType.THEME_SELECTED, Json.obj("theme", i % 2 == 0 ? "jungle" : "under_the_sea"));
            Thread.sleep(1);
        }
    }
}
