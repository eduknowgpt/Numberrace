package org.unicog.numberrace.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
    private static final String BUNDLE_NAME = "resources.messages"; //$NON-NLS-1$

    private static ResourceBundle RESOURCE_BUNDLE;

    //    private static Logger log = Logger.getLogger("NUMBERRACE");

    private static Object[] args = { null, null };
    private static MessageFormat mf = new MessageFormat("", Locale.getDefault());

    private static Locale locale;

    private Messages() {
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public static String getString(String key, Object arg0) {
        args[0] = arg0;
        mf.applyPattern(getString(key));
        return mf.format(args);
    };

    public static String getString(String key, Object arg0, Object arg1) {
        args[0] = arg0;
        args[1] = arg1;
        mf.applyPattern(getString(key));
        return mf.format(args);
    };

    public static void setLocaleInResourseBundle(Locale aLocale) {
        RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME,
                                                   aLocale,
                                                   ResourceProvider.getResourceClassLoader());
        mf.setLocale(aLocale);
        locale = aLocale;
    }

    public static Locale getLocale() {
        if (locale == null) {
            return Locale.getDefault();
        }
        return locale;
    }
}
