package org.unicog.numberrace.setup;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.unicog.numberrace.Game;
import org.unicog.numberrace.debug.DebugFrame;
import org.unicog.numberrace.logging.SimpleFormatter;
import org.unicog.numberrace.util.Messages;
import org.unicog.numberrace.util.ResourceProvider;
import org.unicog.numberrace.util.Resources;

import com.samskivert.util.PrefsConfig;

public class GamePreferences {

    public static PrefsConfig CONFIG;
    private static String langPackStr = "unspecified";
    private static Handler changedHandler;
    private static Formatter changedFormatter;
    private static final Logger log = Logger.getLogger("NUMBERRACE");

    static {
        log.info(Thread.currentThread().getContextClassLoader().toString());

        InputStream loggingCfg = Thread.currentThread()
                                       .getContextClassLoader()
                                       .getResourceAsStream("org/unicog/numberrace/logging/logging.properties");
        if (loggingCfg != null) {
            try {
                LogManager.getLogManager().readConfiguration(loggingCfg);
            } catch (SecurityException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else {
            log.info("Can't load logger properties");
        }

        Logger logger = log;
        boolean logConfigDone = false;
        while (logger != null) {
            Handler targets[] = logger.getHandlers();

            if (targets != null) {
                for (Handler handler : targets) {
                    if (handler instanceof ConsoleHandler) {
                        changedHandler = handler;
                        changedFormatter = handler.getFormatter();
                        handler.setFormatter(new SimpleFormatter());
                        logConfigDone = true;
                        break;
                    }
                }
            }

            if (logConfigDone || !logger.getUseParentHandlers()) {
                break;
            }

            logger = logger.getParent();
        }

        CONFIG = new PrefsConfig("org/unicog/numberrace/app_v3");
    }

    private static final String APP_DIR = "application_directory";
    private static String application_directory;
    private static String languages_directory;
    private static String data_directory;
    private static String globalLangDir;

    public static String getDataDir() {
        return data_directory;
    }

    public static boolean setupPreferences() {

        application_directory = CONFIG.getValue(APP_DIR,
                                                System.getProperty("user.home")
                                                        + File.separator
                                                        + "NumberRace"
                                                        + File.separator + "v3");
        data_directory = confirmDataDir(application_directory);
        if (data_directory == null) {
            log.info("Data directory has not been confirmed.");
            return false;
        }

        languages_directory = application_directory + File.separator + "langs";

        //        try {
        //            System.setOut(new PrintStream(new FileOutputStream(application_directory + File.separator + String.format("nr_%tF-%1$tH%1$tM%1$tS.out.log", new Date()))));
        //            System.setErr(new PrintStream(new FileOutputStream(application_directory + File.separator + String.format("nr_%tF-%1$tH%1$tM%1$tS.err.log", new Date()))));
        //        } catch (FileNotFoundException e) {
        //            e.printStackTrace();
        //            log.warning("Can not create log files");
        //        }

        if (log.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer("\napp_dir  : ").append(application_directory);
            sb.append("\ndata_dir : ").append(data_directory);
            sb.append("\nlang_dir : ").append(languages_directory);
            log.fine(sb.toString());
        }

        try {
            URI uri = Game.class.getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .toURI();

            if ("file".equals(uri.getScheme())) {
                File corePath = new File(uri.getSchemeSpecificPart());
                if (!corePath.isDirectory()) {
                    corePath = corePath.getParentFile();
                }
                globalLangDir = corePath.getAbsolutePath() + File.separator
                        + "langs";
            }
        } catch (URISyntaxException e) {
            log.warning(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warning(e.getMessage());
        }

        log.fine(globalLangDir);

        //        SoundManager.soundsON = (JOptionPane.showConfirmDialog(null, "Play with sounds ?", "Sounds ?", JOptionPane.YES_NO_OPTION) != JOptionPane.NO_OPTION);

        return true;
    }

    private static String confirmDataDir(String appName) {

        File dataDir = new File(appName + File.separator + "Data");

        boolean done = false;
        while (!done) {

            try {
                done = dataDir.exists() || dataDir.mkdirs();

                if (done) {
                    File.createTempFile("nb_", null, dataDir).delete();
                    done = true;
                }
            } catch (Exception e) {
                done = false;
            }

            if (!done) {
                Object[] options = new Object[] { "Quit", "Choose Folder" };
                if (JOptionPane.showOptionDialog(null,
                                                 new Object[] {
                                                         dataDir.getAbsolutePath(),
                                                         "Can not be used for data storage" },
                                                 "Write error !",
                                                 JOptionPane.YES_NO_OPTION,
                                                 JOptionPane.WARNING_MESSAGE,
                                                 null,
                                                 options,
                                                 options[1]) == 1) {
                    JFileChooser fc = new JFileChooser(dataDir);
                    fc.setMultiSelectionEnabled(false);
                    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    fc.addChoosableFileFilter(fc.getAcceptAllFileFilter());

                    if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        String newPath = fc.getSelectedFile().getAbsolutePath();
                        if (!newPath.endsWith("NumberRace")) {
                            dataDir = new File(newPath + File.separator
                                    + "NumberRace" + File.separator + "Data");
                        } else {
                            dataDir = new File(newPath + File.separator
                                    + "Data");
                        }

                        continue;
                    }
                }
                return null; // quit
            } else {
                if (appName.compareTo(dataDir.getParentFile().getAbsolutePath()) != 0) {
                    application_directory = dataDir.getParentFile()
                                                   .getAbsolutePath();
                    CONFIG.setValue(APP_DIR, application_directory);
                }
            }
        }

        return dataDir.getAbsolutePath();
    }

    /**
     * Find jar files and set up needed language
     *
     */
    public static boolean setupLanguage() {

        boolean cont = true;

        Locale locale2use = Locale.getDefault();

        boolean ignoreEmptyLangPacks = Boolean.parseBoolean(System.getProperty("jnlp.numberrace.ignoreEmptyLangPacks"));

        Map<Locale, URL> localesTable = new HashMap<Locale, URL>();

        String requestedLocale = System.getProperty("jnlp.numberrace.locale");
        if (requestedLocale != null && !"".equals(requestedLocale.trim())) {
            locale2use = new Locale(requestedLocale);
            log.config("Using jnlp.numberrace.locale property as preffered locale :"
                    + locale2use.toString());
            localesTable.put(locale2use, null);
        }

        addLanguages(localesTable, GamePreferences.getGlobalLangDir());
        addLanguages(localesTable, GamePreferences.getLangDir());

        // screen for choosing language
        if (!localesTable.isEmpty()) {

            Map<String, Locale> name_locale = new HashMap<String, Locale>();

            for (Iterator<Entry<Locale, URL>> iterator = localesTable.entrySet()
                                                                     .iterator(); iterator.hasNext();) {
                Entry<Locale, URL> locale_file = iterator.next();
                Locale loc = locale_file.getKey();
                name_locale.put(loc.getDisplayName(loc), loc);
            }

            Object selectedValue = null;
            if (!DebugFrame.DEBUG && localesTable.size() > 1) {
                selectedValue = JOptionPane.showInputDialog(null,
                                                            "Choose your language",
                                                            "Number Race",
                                                            JOptionPane.QUESTION_MESSAGE,
                                                            null,
                                                            name_locale.keySet()
                                                                       .toArray(),
                                                            locale2use.getDisplayName(locale2use));

                // if Cancel or Exit
                if (selectedValue == null) {
                    return false;
                }
            } else {
                selectedValue = name_locale.keySet().toArray()[0];
            }
            locale2use = name_locale.get(selectedValue);
            langPackStr = selectedValue.toString();
            
            final URL langPackURL = localesTable.get(locale2use);
            
            if (langPackURL != null) {
                URL[] urls = new URL[] { langPackURL };
                for (int i = 0; i < urls.length; i++) {
                    log.info(urls[i].toExternalForm());
                }
                ResourceProvider.setResourceClassLoader(new URLClassLoader(
                        urls, Thread.currentThread().getContextClassLoader()));
                Matcher m = Pattern.compile("(\\d+(?:.\\d+)?(?:.\\d+)?(?:-\\w+)?)?.jar")
                                   .matcher(langPackURL.getPath());
                if (m.find() && m.group(1) != null) {
                    langPackStr += " - " + m.group(1);
                }
            } else {
                ResourceProvider.setResourceClassLoader(Thread.currentThread()
                                                              .getContextClassLoader());
            }

        } else {
            if (!ignoreEmptyLangPacks) {
                String contStr = "Continue";
                String quitStr = "Quit";
                Object[] message = {
                        "Download language pack(s) you need, put them into the:",
                        "\n" + globalLangDir, "or", languages_directory,
                        "\nand restart Game." };
                cont = (JOptionPane.showOptionDialog(null,
                                                     message,
                                                     "No Language Pack Installed !",
                                                     JOptionPane.OK_CANCEL_OPTION,
                                                     JOptionPane.WARNING_MESSAGE,
                                                     null,
                                                     new Object[] { contStr,
                                                             quitStr },
                                                     quitStr) == new Integer(0));
            }
            ResourceProvider.setResourceClassLoader(Thread.currentThread()
                                                          .getContextClassLoader());
        }

        Messages.setLocaleInResourseBundle(locale2use);
        Resources.setLocaleInResourseBundle(locale2use);

        return cont;
    }

    private static void addLanguages(Map<Locale, URL> localeTable,
            String directory) {
        if (directory == null) {
            return;
        }

        File langDir;
        try {
            langDir = new File(directory);
        } catch (Exception e) {
            log.warning(e.getMessage());
            return;
        }

        log.info(langDir.getAbsolutePath());

        File[] jarFiles = langDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });

        if (jarFiles != null && jarFiles.length > 0) {

            for (int i = 0; i < jarFiles.length; i++) {
                try {
                    // array of URL's with jar files
                    URL jarURL = jarFiles[i].toURI().toURL();
                    // regular expression to separate language letters
                    // allowed file name format: 'name_xx.jar' or
                    // 'name_xx_XX.jar',
                    // where xx: 2-letter language code defined in ISO 639,
                    // and XX: 2-letter country code defined in ISO 3166
                    // (optional)
                    log.info(jarFiles[i].getName());

                    //                    Matcher m = Pattern.compile("[_-]?([a-z]{2})(_[A-Z]{2})?(?:-\\d+(?:.\\d+)?(?:.\\d+)?(?:-\\w+)?)?.jar")
                    //                 .matcher(jarFiles[i].getName());

                    Matcher m = Pattern.compile("[_-]?([a-z]{2})(?:_([A-Z]{2}))?-?(\\d+(?:.\\d+)?(?:.\\d+)?(?:-\\w+)?)?.jar")
                                       .matcher(jarFiles[i].getName());
                    if (m.find()) {
                        // show language name in that language
                        Locale tmpLoc = null;
                        if (m.group(1) != null) { // should never happen I think.
                            tmpLoc = new Locale(m.group(1), m.group(2) == null
                                    ? ""
                                    : m.group(2));
                            localeTable.put(tmpLoc, jarURL);

                        } else {
                            log.warning("File : ["
                                    + jarFiles[i].getName()
                                    + "] is not language pack or does not follow naming convention for ResourceBundles.");
                        }
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getLangDir() {
        return languages_directory;
    }

    public static String getGlobalLangDir() {
        return globalLangDir;
    }

    public static String getVersionStr() {
        return "v." + GamePreferences.CONFIG.getValue("version", "unspecified")
                + " - " + GamePreferences.langPackStr;
    }

    public static void clean() {
        if (changedHandler != null && changedFormatter != null) {
            changedHandler.setFormatter(changedFormatter);
        }
    }
}
