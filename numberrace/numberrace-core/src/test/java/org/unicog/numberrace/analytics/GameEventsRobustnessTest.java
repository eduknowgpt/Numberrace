package org.unicog.numberrace.analytics;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * "Nenhum erro do registro chega ao jogo": todo método público de {@link GameEvents} é chamado com
 * valores ausentes ou estranhos (nulos, zeros e números negativos), sem sessão aberta, e nenhuma
 * exceção pode escapar.
 *
 * <p>Ficam de fora os métodos que buscam o próprio jogo ({@code GameObject.getInstance()}), que
 * abriria a janela do jogo durante os testes, e o que liga o registro na pasta do usuário.
 */
public class GameEventsRobustnessTest {

    private static final List<String> TOUCH_THE_GAME = Arrays.asList("applicationStarted", "turnStarted",
            "gameEnded");

    @Test
    public void nenhumaExcecaoEscapaParaOJogo() throws Exception {
        int called = 0;
        for (Method m : GameEvents.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())
                    || TOUCH_THE_GAME.contains(m.getName())) {
                continue;
            }
            for (int variant = 0; variant < 3; variant++) {
                Class<?>[] types = m.getParameterTypes();
                Object[] args = new Object[types.length];
                for (int i = 0; i < types.length; i++) {
                    args[i] = value(types[i], variant);
                }
                try {
                    m.invoke(null, args);
                } catch (InvocationTargetException e) {
                    fail(m.getName() + Arrays.toString(args) + " deixou escapar " + e.getCause());
                }
            }
            called++;
        }
        assertTrue("métodos chamados: " + called, called >= 20);
        assertTrue(!InteractionLogger.get().inSession());
    }

    private static Object value(Class<?> type, int variant) {
        long n = variant == 0 ? 0 : variant == 1 ? -1 : 99999;
        if (type == int.class) {
            return Integer.valueOf((int) n);
        } else if (type == long.class) {
            return Long.valueOf(n);
        } else if (type == byte.class) {
            return Byte.valueOf((byte) n);
        } else if (type == boolean.class) {
            return Boolean.valueOf(variant == 1);
        } else if (type == double.class) {
            return Double.valueOf(n);
        } else if (type == String.class) {
            return variant == 0 ? null : variant == 1 ? "" : "valor_inesperado";
        }
        return null;
    }
}
