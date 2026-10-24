/**
 * Registro dos eventos de interação do Number Race (modelo de eventos 1.1).
 *
 * <p>Visão geral para quem for continuar o componente (guia completo em
 * {@code evolution/pt/guia-tecnico-dados-interacao.md}):
 *
 * <ul>
 * <li>{@link org.unicog.numberrace.analytics.GameEvents} é a única classe que conhece o jogo: o jogo
 * chama um método por situação (resposta, movimento, pausa...) e segue sem esperar. Nenhum erro do
 * registro chega ao jogo.</li>
 * <li>{@link org.unicog.numberrace.analytics.InteractionLogger} completa a parte comum do evento
 * (sessão, participante, sequência, partida e rodada), valida com
 * {@link org.unicog.numberrace.analytics.EventValidator} e põe o evento numa fila.</li>
 * <li>Uma linha de execução em segundo plano grava cada evento primeiro no arquivo JSON Lines da
 * sessão ({@code JsonLinesSink}, registro oficial) e depois no
 * banco SQLite ({@code SqliteSink}, cópia para consulta).</li>
 * <li>{@link org.unicog.numberrace.analytics.ParticipantRegistry} guarda, fora da pasta de dados,
 * a correspondência entre o aluno e o código pseudonimizado usado nos eventos.</li>
 * </ul>
 *
 * <p>Para registrar um tipo de evento novo: modelo e JSON Schema em {@code evolution/pt/},
 * {@link org.unicog.numberrace.analytics.EventType}, regras em
 * {@link org.unicog.numberrace.analytics.EventValidator}, um método em
 * {@link org.unicog.numberrace.analytics.GameEvents} chamado do ponto certo do jogo, e testes.
 *
 * @see org.unicog.numberrace.analytics.export
 */
package org.unicog.numberrace.analytics;
