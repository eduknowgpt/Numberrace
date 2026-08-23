package org.unicog.numberrace.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.unicog.numberrace.vars.ThemeVariables;

public class Resources {
    private static final String BUNDLE_NAME = "resources.resources"; //$NON-NLS-1$

    //    private static Logger log = Logger.getLogger("NUMBERRACE");

    private static ResourceBundle RESOURCE_BUNDLE;

    private static Object[] args = { null, null };
    private static MessageFormat mf = new MessageFormat("", Locale.getDefault());

    private Resources() {
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public static String getString(String key, Object arg0) {
        if (arg0 != null) {
            args[0] = arg0;
            mf.applyPattern(getString(key));
            return mf.format(args);
        }
        return getString(key);
    };

    public static void setLocaleInResourseBundle(Locale aLocale) {
        Utilities.log.info("Load Resource Bundle, name =  " + BUNDLE_NAME
                + " loacale = " + aLocale);
        RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME,
                                                   aLocale,
                                                   ResourceProvider.getResourceClassLoader());
        mf.setLocale(aLocale);
    }

    /**
     * {0} in path will be changed to localized_path from messages.properties
     * {1} will be changed to current theme's name
     * 
     * @param path
     * @return
     */
    public static String getLocalizedThemedPath(String path) {
        //        log.config(path);
        args[0] = Messages.getString("localized_path");
        if ("!localized_path!".equals(args[0])) {
            args[0] = mf.getLocale().toString();
        }
        args[1] = ThemeVariables.getThemeName();
        mf.applyPattern(path);
        String str = mf.format(args);
        //		log.config(str);
        return str;
    }

}
