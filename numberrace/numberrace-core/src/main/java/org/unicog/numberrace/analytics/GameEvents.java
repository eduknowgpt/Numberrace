package org.unicog.numberrace.analytics;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.jmat.data.Matrix;
import org.unicog.numberrace.GameObject;
import org.unicog.numberrace.GameObject.GameStates;
import org.unicog.numberrace.algorithms.AdapDimensions;
import org.unicog.numberrace.algorithms.GameTurn;
import org.unicog.numberrace.algorithms.NotnDimLevel;
import org.unicog.numberrace.data.Student;
import org.unicog.numberrace.managers.HazardManager;
import org.unicog.numberrace.setup.GamePreferences;
import org.unicog.numberrace.util.Constants;
import org.unicog.numberrace.util.Messages;
import org.unicog.numberrace.vars.ThemeVariables;

/**
 * Ponte entre o jogo e o componente de registro: traduz o que acontece no jogo em eventos do
 * modelo 1.1. É a única classe do pacote que conhece as classes do jogo; cada ponto do jogo
 * chama um método daqui com uma linha. Nenhum método lança exceção para o jogo.
 */
public final class GameEvents {

    static final String ACTIVITY = "number_comparison";

    private static final InteractionLogger LOGGER = InteractionLogger.get();

    private static int gamesInSession;
    private static boolean gameInProgress;
    private static String pendingEndReason;
    private static long leftStimulusNanos;
    private static long responseEnabledNanos;
    private static int tooFarAttempts;
    private static boolean pauseOpen;
    private static String displayMode;

    private GameEvents() {
    }

    // ------------------------------------------------------------------ aplicação e sessão

    /** Jogo aberto: liga o registro no diretório da aplicação (ao lado da pasta Data). */
    public static void applicationStarted() {
        try {
            File appDir;
            String dataDir = GamePreferences.getDataDir();
            if (dataDir != null) {
                appDir = new File(dataDir).getParentFile();
            } else {
                appDir = new File(System.getProperty("user.home"), "NumberRace" + File.separator + "v3");
            }
            LOGGER.start(appDir);
        } catch (Throwable t) {
            fail("applicationStarted", t);
        }
    }

    /** Modo de exibição escolhido ao abrir o jogo (tela cheia ou janela). */
    public static void displayMode(boolean fullscreen) {
        displayMode = fullscreen ? "fullscreen" : "in_window";
    }

    /** Aluno selecionado ou cadastrado: abre a sessão. */
    public static void sessionStarted(Student student, int startComplexityLevel) {
        try {
            if (LOGGER.inSession()) {
                sessionEnded("change_student", null);
            }
            String participant = LOGGER.participantCodeFor(student.getLastName(), student.getFirstName());
            gamesInSession = 0;
            gameInProgress = false;
            pendingEndReason = null;
            pauseOpen = false;
            Map<String, Object> data = Json.obj(
                    "session_number", Integer.valueOf(student.getSessionNumber()),
                    "start_level", startLevel(student.getStartLevel(), startComplexityLevel),
                    "start_complexity_level", Integer.valueOf(startComplexityLevel),
                    "language", language(),
                    "app_version", appVersion());
            if (displayMode != null) {
                data.put("display_mode", displayMode);
            }
            LOGGER.startSession(participant, data);
        } catch (Throwable t) {
            fail("sessionStarted", t);
        }
    }

    /** Fim da sessão (troca de aluno pelo menu ou fechamento do jogo). */
    public static void sessionEnded(String reason, GameStates screen) {
        try {
            if (!LOGGER.inSession()) {
                return;
            }
            LOGGER.endSession(Json.obj(
                    "end_reason", reason,
                    "screen", screenName(screen),
                    "game_in_progress", Boolean.valueOf(gameInProgress)));
            gameInProgress = false;
            pauseOpen = false;
        } catch (Throwable t) {
            fail("sessionEnded", t);
        }
    }

    /** Guarda o motivo do encerramento (saída pelo menu ou Shift+Esc, ou janela fechada). */
    public static void endRequested(String reason) {
        pendingEndReason = reason;
    }

    /** O jogo está fechando: encerra a sessão e grava o que estiver na fila (até 2 s). */
    public static void applicationStopping(GameStates screen) {
        try {
            sessionEnded(pendingEndReason != null ? pendingEndReason : "quit", screen);
            LOGGER.shutdown(2000);
        } catch (Throwable t) {
            fail("applicationStopping", t);
        }
    }

    // ------------------------------------------------------------------ navegação

    public static void screenChanged(GameStates from, GameStates to) {
        try {
            if (from == null || to == null || from == to) {
                return;
            }
            LOGGER.log(EventType.SCREEN_CHANGED, Json.obj(
                    "from_screen", screenName(from),
                    "to_screen", screenName(to)));
        } catch (Throwable t) {
            fail("screenChanged", t);
        }
    }

    public static void themeSelected(byte theme) {
        try {
            LOGGER.log(EventType.THEME_SELECTED, Json.obj("theme", themeName(theme)));
        } catch (Throwable t) {
            fail("themeSelected", t);
        }
    }

    public static void characterSelected(int playerCharacter, int opponentCharacter) {
        try {
            LOGGER.log(EventType.CHARACTER_SELECTED, Json.obj(
                    "player_character", Integer.valueOf(playerCharacter),
                    "opponent_character", Integer.valueOf(opponentCharacter)));
        } catch (Throwable t) {
            fail("characterSelected", t);
        }
    }

    /**
     * Pausa pela tecla F8 ({@code pause_key}), pela abertura do menu ({@code menu}) ou porque a
     * janela do jogo perdeu o foco ({@code window_focus}, modelo 1.1).
     */
    public static void paused(String trigger, GameStates screen) {
        try {
            if (pauseOpen || !LOGGER.inSession()) {
                return;
            }
            pauseOpen = true;
            LOGGER.log(EventType.PAUSE, Json.obj("trigger", trigger, "screen", screenName(screen)));
        } catch (Throwable t) {
            fail("paused", t);
        }
    }

    public static void resumed(String trigger, GameStates screen) {
        try {
            if (!pauseOpen) {
                return;
            }
            pauseOpen = false;
            LOGGER.log(EventType.RESUME, Json.obj("trigger", trigger, "screen", screenName(screen)));
        } catch (Throwable t) {
            fail("resumed", t);
        }
    }

    /** {@code rewardType}: índice do jogo, de 0 a 6; o modelo numera os animais de 1 a 7. */
    public static void rewardSaved(int rewardType, byte theme) {
        try {
            LOGGER.log(EventType.REWARD_SAVED, Json.obj(
                    "reward_type", Integer.valueOf(rewardType + 1),
                    "theme", themeName(theme)));
        } catch (Throwable t) {
            fail("rewardSaved", t);
        }
    }

    public static void characterUnlocked(int character, byte theme) {
        try {
            if (character < 0) {
                return;
            }
            LOGGER.log(EventType.CHARACTER_UNLOCKED, Json.obj(
                    "character", Integer.valueOf(character),
                    "theme", themeName(theme)));
        } catch (Throwable t) {
            fail("characterUnlocked", t);
        }
    }

    // ------------------------------------------------------------------ partida

    public static void gameStarted(int boardLength, int notationLevelIndex) {
        try {
            gamesInSession++;
            gameInProgress = true;
            LOGGER.beginGame(gamesInSession, ACTIVITY);
            LOGGER.log(EventType.GAME_START, Json.obj(
                    "board_length", Integer.valueOf(boardLength),
                    "complexity_level", Integer.valueOf(notationLevelIndex + 1)));
        } catch (Throwable t) {
            fail("gameStarted", t);
        }
    }

    public static void gameEnded(int winnerId, int turnsPlayed) {
        try {
            GameObject go = GameObject.getInstance();
            LOGGER.log(EventType.GAME_END, Json.obj(
                    "winner", piece(winnerId),
                    "turns_played", Integer.valueOf(turnsPlayed),
                    "final_squares", squares(go.getPlayerPosition(), go.getOpponentPosition())));
            LOGGER.endGame();
            gameInProgress = false;
        } catch (Throwable t) {
            fail("gameEnded", t);
        }
    }

    /** O algoritmo adaptativo escolheu a dificuldade da rodada {@code next}. */
    public static void difficultyAdjusted(GameTurn next) {
        try {
            Matrix d = next.getCurrentDifficulty();
            Matrix c = next.getCoordsChosen();
            List<Object> coords = new ArrayList<Object>();
            for (int i = 0; i < AdapDimensions.NUM_ADAP_DIMS; i++) {
                coords.add(Double.valueOf(c.get(0, i)));
            }
            LOGGER.log(EventType.DIFFICULTY_ADJUSTED, Json.obj(
                    "next_turn_number", Integer.valueOf(next.getTurnNumber()),
                    "difficulty", Json.obj(
                            "speed", Double.valueOf(d.get(0, AdapDimensions.SPEED_DIM)),
                            "distance", Double.valueOf(d.get(0, AdapDimensions.DIST_DIM)),
                            "notation", Double.valueOf(d.get(0, AdapDimensions.NOTN_DIM))),
                    "mean_success", Double.valueOf(next.getMeanSuccess()),
                    "desired_difficulty", Double.valueOf(next.getCurrDesiredDiff()),
                    "estimated_difficulty", Double.valueOf(next.getEstimatedDiff()),
                    "coords_chosen", coords));
        } catch (Throwable t) {
            fail("difficultyAdjusted", t);
        }
    }

    // ------------------------------------------------------------------ rodada

    public static void turnStarted(GameTurn turn, NotnDimLevel level) {
        try {
            GameObject go = GameObject.getInstance();
            LOGGER.beginTurn(turn.getTurnNumber());
            tooFarAttempts = 0;
            LOGGER.log(EventType.TURN_START, Json.obj(
                    "complexity_level", Integer.valueOf(turn.getCurrentNotnLevel() + 1),
                    "representation", Json.obj(
                            "dots", Boolean.valueOf(level.analogMagStims),
                            "spoken", Boolean.valueOf(level.verbalStims),
                            "digits", Boolean.valueOf(level.arabicStims),
                            "dot_fading", Boolean.valueOf(level.dotsFade),
                            "dot_fading_ms", Integer.valueOf(level.dotsFade ? level.fadeTime : 0)),
                    "max_value", Integer.valueOf(level.rangeCeilling),
                    "addition", Boolean.valueOf(level.addition),
                    "subtraction", Boolean.valueOf(level.subtraction),
                    "hazards_enabled", Boolean.valueOf(level.hazards),
                    "hazards_on_board", hazardsOnBoard(go.getHazardManager()),
                    "deadline_trial", Boolean.valueOf(turn.isDeadlineTrial()),
                    "deadline_ms", turn.isDeadlineTrial()
                            ? Integer.valueOf((int) Math.round(turn.getCurrentDeadline())) : null,
                    // controlDensity verdadeiro = densidade fixa (NumCompManager.setStimsForTurn)
                    "control_for", turn.getControlDensity() ? "density" : "item_size",
                    "squares", squares(go.getPlayerPosition(), go.getOpponentPosition())));
        } catch (Throwable t) {
            fail("turnStarted", t);
        }
    }

    public static void stimulusPresented(GameTurn turn, int side) {
        try {
            if (side == Constants.LEFT) {
                leftStimulusNanos = System.nanoTime();
                responseEnabledNanos = 0;
            }
            String operator = turn.getOperators()[side];
            Object operation = null;
            if (operator != null) {
                List<Object> operands = new ArrayList<Object>();
                operands.add(Integer.valueOf(turn.getSubNumber(side, Constants.LEFT)));
                operands.add(Integer.valueOf(turn.getSubNumber(side, Constants.RIGHT)));
                operation = Json.obj("operator", operator, "operands", operands);
            }
            LOGGER.log(EventType.STIMULUS_PRESENTED, Json.obj(
                    "side", side(side),
                    "value", Integer.valueOf(turn.getNumber(side)),
                    "operation", operation));
        } catch (Throwable t) {
            fail("stimulusPresented", t);
        }
    }

    public static void responseEnabled() {
        try {
            responseEnabledNanos = System.nanoTime();
            LOGGER.log(EventType.RESPONSE_ENABLED, Json.obj());
        } catch (Throwable t) {
            fail("responseEnabled", t);
        }
    }

    /**
     * Clique num lado antes da liberação da resposta (ignorado pelo jogo). Registro opcional
     * (decisão D1): desligado com {@code -Dnumberrace.analytics.clickIgnored=false}.
     */
    public static void clickIgnored(int side) {
        try {
            if (Boolean.parseBoolean(System.getProperty("numberrace.analytics.clickIgnored", "true"))
                    && LOGGER.inTurn()) {
                LOGGER.log(EventType.CLICK_IGNORED, Json.obj("side", side(side)));
            }
        } catch (Throwable t) {
            fail("clickIgnored", t);
        }
    }

    /** Resposta da criança, já com o resultado calculado pelo jogo. Registra também TURN_RESULT. */
    public static void answered(GameTurn turn, int side) {
        try {
            long now = System.nanoTime();
            long fromFirst = leftStimulusNanos > 0 ? (now - leftStimulusNanos) / 1000000L : 0;
            long fromEnabled = responseEnabledNanos > 0 ? (now - responseEnabledNanos) / 1000000L : fromFirst;
            LOGGER.log(EventType.ANSWER, Json.obj(
                    "side", side(side),
                    "response_time_ms", Long.valueOf(Math.max(0, fromEnabled)),
                    "response_time_from_first_stimulus_ms", Long.valueOf(Math.max(0, fromFirst)),
                    "correct_larger", Boolean.valueOf(turn.getResponseCorrect() == Constants.TRUE),
                    "correct_final", Boolean.valueOf(turn.getFinalCorrect() == 1)));
            turnResult(turn);
        } catch (Throwable t) {
            fail("answered", t);
        }
    }

    /** Prazo esgotado sem resposta. Registra também TURN_RESULT. */
    public static void timedOut(GameTurn turn) {
        try {
            LOGGER.log(EventType.ANSWER_TIMEOUT, Json.obj(
                    "deadline_ms", Integer.valueOf((int) Math.round(turn.getCurrentDeadline())),
                    "assigned_side", side(turn.getResponseSide())));
            turnResult(turn);
        } catch (Throwable t) {
            fail("timedOut", t);
        }
    }

    private static void turnResult(GameTurn turn) {
        LOGGER.log(EventType.TURN_RESULT, Json.obj(
                "turn_winner", piece(turn.getActualWinner()),
                "outcome", outcome(turn.getRelNetGain(), turn.getNetGain(), turn.getSquaresMovedForward(),
                        turn.getSquaresMovedBack()),
                "hypothetical_outcome", outcome(turn.getHypRelNetGain(), turn.getHypNetGain(),
                        turn.getHypSquaresMovedForward(), turn.getHypSquaresMovedBack())));
    }

    // ------------------------------------------------------------------ tabuleiro

    /** A criança clicou numa casa além da permitida durante um movimento. */
    public static void moveTooFar() {
        tooFarAttempts++;
    }

    /** Um personagem chegou à casa de destino do movimento. */
    public static void moved(int pieceId, int fromSquare, int toSquare, int steps) {
        try {
            LOGGER.log(EventType.MOVE, Json.obj(
                    "piece", piece(pieceId),
                    "from_square", Integer.valueOf(fromSquare),
                    "to_square", Integer.valueOf(toSquare),
                    "steps", Integer.valueOf(steps),
                    "too_far_attempts", Integer.valueOf(tooFarAttempts)));
        } catch (Throwable t) {
            fail("moved", t);
        } finally {
            tooFarAttempts = 0;
        }
    }

    /** Esbarrão: o personagem {@code pushedId} volta uma casa. */
    public static void collision(int pushedId, int square) {
        try {
            LOGGER.log(EventType.COLLISION, Json.obj(
                    "pushed_piece", piece(pushedId),
                    "from_square", Integer.valueOf(square),
                    "to_square", Integer.valueOf(Math.max(0, square - 1))));
        } catch (Throwable t) {
            fail("collision", t);
        }
    }

    /** Armadilha: {@code penalty} é negativo no jogo (casas de recuo). */
    public static void hazard(int pieceId, int square, int penalty) {
        try {
            LOGGER.log(EventType.HAZARD, Json.obj(
                    "piece", piece(pieceId),
                    "square", Integer.valueOf(square),
                    "penalty", Integer.valueOf(Math.abs(penalty)),
                    "to_square", Integer.valueOf(Math.max(0, square - Math.abs(penalty)))));
        } catch (Throwable t) {
            fail("hazard", t);
        }
    }

    /** Fala do adversário apresentada à criança (nome do áudio). */
    public static void feedback(String messageKey) {
        try {
            if (LOGGER.inTurn()) {
                LOGGER.log(EventType.FEEDBACK_PRESENTED, Json.obj("message_key", messageKey));
            }
        } catch (Throwable t) {
            fail("feedback", t);
        }
    }

    // ------------------------------------------------------------------ auxiliares

    private static Map<String, Object> outcome(int relative, int[] net, int[] forward, int[] back) {
        return Json.obj(
                "relative_net_gain", Integer.valueOf(relative),
                "net_gain", squares(net[Constants.PLAYER1], net[Constants.PLAYER2]),
                "squares_forward", squares(forward[Constants.PLAYER1], forward[Constants.PLAYER2]),
                "squares_back", squares(back[Constants.PLAYER1], back[Constants.PLAYER2]));
    }

    private static Map<String, Object> squares(int player, int opponent) {
        return Json.obj("player", Integer.valueOf(player), "opponent", Integer.valueOf(opponent));
    }

    private static List<Object> hazardsOnBoard(HazardManager hm) {
        List<Object> list = new ArrayList<Object>();
        if (hm == null) {
            return list;
        }
        for (int square = 1; square <= Constants.LAST_SQUARE; square++) {
            int penalty = hm.movementDueToHazard(square); // negativo: casas de recuo
            if (penalty != 0) {
                list.add(Json.obj("square", Integer.valueOf(square), "penalty", Integer.valueOf(Math.abs(penalty))));
            }
        }
        return list;
    }

    static String side(int side) {
        return side == Constants.LEFT ? "left" : "right";
    }

    static String piece(int playerId) {
        return playerId == Constants.PLAYER1 ? "player" : "opponent";
    }

    static String screenName(GameStates state) {
        if (state == null || state == GameStates.MENU || state == GameStates.PAUSE || state == GameStates.END) {
            return "choice";
        }
        return state.name().toLowerCase(Locale.ROOT);
    }

    static String themeName(byte theme) {
        return theme == ThemeVariables.UNDER_THE_SEA ? "under_the_sea" : "jungle";
    }

    static String startLevel(String level, int complexityLevel) {
        if (level != null) {
            String l = level.trim().toUpperCase(Locale.ROOT);
            if (l.equals("EASY")) {
                return "easy";
            } else if (l.equals("INTERMEDIATE")) {
                return "intermediate";
            } else if (l.equals("HARD")) {
                return "hard";
            }
        }
        // nível antigo ou desconhecido: deduz pela faixa do nível de complexidade
        return complexityLevel >= 14 ? "hard" : complexityLevel >= 8 ? "intermediate" : "easy";
    }

    private static String language() {
        Locale locale = Messages.getLocale();
        String lang = locale != null ? locale.getLanguage() : Locale.getDefault().getLanguage();
        return lang == null || lang.length() == 0 ? "und" : lang;
    }

    private static String appVersion() {
        try {
            InputStream in = GameEvents.class.getResourceAsStream("analytics.properties");
            if (in != null) {
                try {
                    Properties p = new Properties();
                    p.load(in);
                    String v = p.getProperty("app.version");
                    if (v != null && !v.startsWith("$")) {
                        return v;
                    }
                } finally {
                    in.close();
                }
            }
        } catch (Throwable ignored) {
            // segue para o valor padrão
        }
        return "unknown";
    }

    private static void fail(String where, Throwable t) {
        InteractionLogger.LOG.log(Level.WARNING, "Falha no registro de interação (" + where + "); o jogo continua", t);
    }
}
