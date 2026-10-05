package org.unicog.numberrace.analytics;

/** Destino de gravação dos eventos. Chamado somente pela linha de execução do gravador. */
interface EventSink {

    /** Grava um evento. Erros são tratados pelo próprio destino, sem propagar exceções. */
    void write(InteractionEvent event);

    /** Chamado depois que o {@code SESSION_END} de uma sessão foi gravado. */
    void sessionEnded(String sessionId);

    void close();
}
