package org.unicog.numberrace.util;

import java.io.InputStream;
import java.net.URL;

public class ResourceProvider {

    //    private static Logger log = Logger.getLogger("NUMBERRACE");
    private static ClassLoader resourceClassLoader = ClassLoader.getSystemClassLoader();

    public static ClassLoader getResourceClassLoader() {
        return resourceClassLoader;
    }

    public static void setResourceClassLoader(ClassLoader resourceClassLoader) {
        ResourceProvider.resourceClassLoader = resourceClassLoader;
    }

    public static InputStream getResourceAsStream(String name) {
        return resourceClassLoader.getResourceAsStream(processName(name));
    }

    public static URL getResource(String name) {
        return resourceClassLoader.getResource(processName(name));
    }

    private static String processName(String name) {
        return Resources.getLocalizedThemedPath(name);
    }

}
