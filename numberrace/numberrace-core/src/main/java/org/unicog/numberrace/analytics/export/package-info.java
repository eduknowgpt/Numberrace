/**
 * Exportação dos dados de interação, executada fora do jogo
 * ({@link org.unicog.numberrace.analytics.export.ExportTool}, ou {@code numberrace/tools/exportar.bat}
 * no Windows).
 *
 * <p>Etapas, na ordem em que acontecem:
 *
 * <ol>
 * <li>{@code EventImporter}: lê os arquivos JSON Lines de uma
 * ou mais pastas (vários computadores), valida cada linha, descarta repetições pelo
 * {@code event_id} e relata o que encontrou
 * ({@code ImportReport}).</li>
 * <li>{@code ExportFilter}: aplica os filtros por
 * participante, sessão e data.</li>
 * <li>{@code IntegrityChecker}: confere 21 regras de
 * integridade em cada sessão (erros e avisos), sem alterar nem descartar eventos.</li>
 * <li>{@code Tables}: monta as tabelas de eventos, sessões,
 * rodadas, integridade e o dicionário de dados.</li>
 * <li>{@code PackageWriter}: grava o pacote (eventos brutos,
 * CSV nas versões padrão e Excel brasileiro, banco SQLite, relatórios e manifesto com SHA-256);
 * {@code PackageVerifier} confere um pacote depois.</li>
 * </ol>
 *
 * <p>Para um formato de exportação novo, o ponto de partida é
 * {@code PackageWriter}, que recebe as tabelas já prontas.
 */
package org.unicog.numberrace.analytics.export;
