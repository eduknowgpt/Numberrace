package org.unicog.numberrace.setup;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class LanguageClasspathTest {
    @Test public void allLanguageModulesAreAvailable() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (String code : new String[] {"de", "el", "en", "es", "fi", "fr", "nl", "pt", "sv"}) {
            assertNotNull(code, loader.getResource("META-INF/numberrace/languages/" + code + ".marker"));
            assertNotNull(code, loader.getResource("resources/messages_" + code + ".properties"));
        }
        assertEquals(1, Collections.list(loader.getResources("resources/messages_pt.properties")).size());
    }
}
