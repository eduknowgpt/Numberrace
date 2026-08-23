package org.unicog.numberrace.langs.editor

import scala.swing._
import javax.swing.{ JTextPane, UIManager }
import java.awt.Dimension
import java.io.File
import java.util.Locale
import java.util.ResourceBundle
import java.util.prefs.Preferences
import javax.swing.filechooser.FileFilter
import FileChooser.SelectionMode._
import FileChooser.Result._
import org.unicog.numberrace.util.ResourceProvider
import org.unicog.numberrace.util.Resources
import org.unicog.numberrace.util.Messages
import fi.nmi.scala.swing.CardPanel
import fi.nmi.scala.swing.ToolBar
import java.util.logging.Logger
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Filter
import java.util.logging.Level
import scala.reflect.BeanProperty
import org.unicog.numberrace.logging.SimpleFormatter
import javax.swing.SwingUtilities

/**
 * Hello world!
 *
 */
object EditorApp extends SimpleGUIApplication {

  lazy val runIcon = new javax.swing.ImageIcon(resourceFromClassloader("/run.png"))
  lazy val stopIcon = new javax.swing.ImageIcon(resourceFromClassloader("/stop.png"))

  var amount = 0
  var working_dir: File = null
  var cur_locale: Locale = null
  val work_dir_name = "working_resource_dir"

  lazy val prb = ResourceBundle.getBundle("resources.resources", cur_locale, ResourceProvider.getResourceClassLoader)
  lazy val messages = ResourceBundle.getBundle("resources.messages", cur_locale, ResourceProvider.getResourceClassLoader)
  lazy val resources_dir = new File(working_dir + "/src/main/resources/")
  lazy val messages_file = new File(resources_dir + File.separator + "resources" + File.separator + "messages_" + cur_locale + ".properties")
  lazy val prefs = Preferences.userRoot.node("org.unicog.numberrace.langs.editor")
  lazy val mainPane = new CardPanel

  override lazy val top = new MainFrame {
    title = "Number Race Language Pack Editor"

    val fc = prefs.get(work_dir_name, null) match {
      case null => new FileChooser(new File(System.getProperty("user.home")))
      case wd => new FileChooser(new File(wd))
    }

    fc.title = "Select your Working [Language Pack] folder"
    fc.fileSelectionMode = DirectoriesOnly
    fc.fileFilter = new FileFilter {
      override def accept(f: File): Boolean = f.isDirectory
      override def getDescription: String = "Folders only"
    }

    fc.showDialog(mainPane, "Use It") match {
      case Cancel => System.exit(1)
      case Approve =>
        working_dir = fc.selectedFile

        if (!new File(working_dir + "/src/main/resources/").exists) {
          import Dialog.{ Options, Result }

          Dialog.showConfirmation(mainPane, String.format("Seems like [%s] is not NumberRace language pack maven project directory\nWould you like to start new language pack in selected folder?", working_dir.getPath), "Would you like to start new LanguagePack ?", Options.YesNo) match {
            case Result.Yes =>
              val lang = new ComboBox(Locale.getISOLanguages.map(new Locale(_)))
              val country = new ComboBox(Locale.getISOCountries.map(new Locale("", _)))
              country.enabled = false

              lang.renderer = new ListView.AbstractRenderer[Locale, Label](new Label) {
                override def configure(list: ListView[_], isSelected: Boolean, hasFocus: Boolean, a: Locale, index: Int) = {
                  component.xAlignment = Alignment.Left
                  component.text = a.getDisplayLanguage
                }
              }

              country.renderer = new ListView.AbstractRenderer[Locale, Label](new Label) {
                override def configure(list: ListView[_], isSelected: Boolean, hasFocus: Boolean, a: Locale, index: Int) = {
                  component.xAlignment = Alignment.Left
                  component.text = a.getDisplayCountry
                }
              }

              val gp = new GridPanel(2, 2)

              val use_country = new CheckBox("Use Country :")

              listenTo(use_country)

              reactions += {
                case event.ButtonClicked(`use_country`) =>
                  country.enabled = use_country.selected
              }

              val langLbl = new Label("Language :")
              langLbl.xAlignment = Alignment.Left

              gp.contents ++= List(langLbl, lang, use_country, country)

              import javax.swing.JOptionPane
              JOptionPane.showConfirmDialog(mainPane.peer, gp.peer, "Choose", JOptionPane.YES_NO_OPTION) match {
                case JOptionPane.YES_OPTION =>
                  if (use_country.selected) {
                    cur_locale = new Locale(lang.selection.item.getLanguage, country.selection.item.getCountry);
                  } else {
                    cur_locale = lang.selection.item;
                  }
                  working_dir = new File(working_dir.getPath + File.separator + "numberrace-langs-" + cur_locale.toString);
                  if (!new File(resources_dir + File.separator + "resources").mkdirs) {
                    Dialog.showMessage(mainPane, "Can not create resource folder [" + resources_dir.getPath + "] or it exists already")
                    System.exit(1)
                  }

                case _ =>
                  System.exit(1)
              }

            case _ =>
              System.exit(1)
          }

          //              printf("Directory\n\t[%s}\ndoes not exist.\nMost probably directory you have choosen\n\t[%s]\nis not NumberRace language pack maven project directory\n", resources_dir.getPath, working_dir.getName)
        }

        import java.net.URLClassLoader
        import java.util.regex.{ Matcher, Pattern }

        ResourceProvider.setResourceClassLoader(new URLClassLoader(Array(resources_dir.toURI.toURL), Thread.currentThread.getContextClassLoader))

        val m = Pattern.compile("[_-]?([a-z]{2})(?:_([A-Z]{2}))?$").matcher(working_dir.getName)
        if (m.find()) {
          cur_locale = new Locale(m.group(1), if (m.group(2) == null) "" else m.group(2))
          title = String.format("Editing LanguagePack for locale [%s] - [%s]", cur_locale, working_dir)

          Messages.setLocaleInResourseBundle(cur_locale)
          Resources.setLocaleInResourseBundle(cur_locale)

          prefs.put(work_dir_name, working_dir.toString)
        } else {
          printf("Directory : [$s] is not language pack or does not follow naming convention for ResourceBundles.");
          System.exit(1)
        }
    }
    messages_file.createNewFile

    import TabbedPane._
    import BorderPanel.Position._
    import javax.swing.JToolBar

    val editorPane = new MessagesPane
    editorPane.peer.setPreferredSize(new Dimension(800, 400))

    val tabPane = new TabbedPane
    tabPane.tabPlacement(Alignment.Left)

    val contentPane = new BorderPanel
    contents = contentPane

    val toolBar = new ToolBar
    toolBar.floatable = false

    val logArea = new TextArea(10, 80)
    logArea.wordWrap = true
    logArea.editable = false

    contentPane.layout += (toolBar -> North, mainPane -> Center, new ScrollPane(logArea) -> South)

    var log = Logger.getLogger("org.unicog")

    log.addHandler(new Handler() {
      override def publish(record: LogRecord) = {
        if (isLoggable(record)) {
          val msg = new SimpleFormatter().format(record)

          SwingUtilities.invokeLater(new Runnable {
            override def run = {
              logArea.append(msg)
            }
          })
        }
      }

      override def flush = {}
      override def close = {}
    })

    val res = new ToggleButton("")
    res.selected = true
    res.focusPainted = false

    val msg = new ToggleButton("")
    res.focusPainted = false

    val numTest = new ToggleButton("")
    res.focusPainted = false

    new ButtonGroup(res, msg, numTest)

    res.action = Action("R")(mainPane.show("R"))
    msg.action = Action("M")(mainPane.show("M"))
    numTest.action = Action("Test Sound Sequences")(mainPane.show("TSS"))

    val tryAct = Action("Try")(runGame)
    tryAct.icon = runIcon

    toolBar.peer.add(res.peer)
    toolBar.peer.add(msg.peer)
    toolBar.peer.add(numTest.peer)
    toolBar.peer.add(javax.swing.Box.createHorizontalGlue)
    toolBar.peer.add(tryAct.peer)

    mainPane.layout += (new ResourcesPane -> "R", editorPane -> "M", new SoundTests -> "TSS")

    editorPane.load

    amount = amount + 1
    printf("top invocation #%d\n", amount)
  }

  def getResourceURL(path: String) = ResourceProvider.getResource(Resources.getLocalizedThemedPath(path))

  def runGame = {

    import org.unicog.numberrace.setup.GamePreferences
    GamePreferences.setupPreferences
    val lang_dir = GamePreferences.getLangDir

    new File(lang_dir).mkdirs

    Utils.createJar(new File(lang_dir + File.separator + "EDITOR-NB-langs_" + cur_locale.toString + ".jar"), resources_dir)

    //      val pb = new ProcessBuilder("jar", "cvf", lang_dir + File.separator + "EDITOR-NB-langs_" + cur_locale.toString + ".jar", "-C", resources_dir.getPath, ".")
    //      println("jar cvf "+lang_dir + File.separator + "EDITOR-NB-langs_" + cur_locale.toString + ".jar -C " + resources_dir.getPath + " .")
    //      
    if (core_path != null) {
      new Thread(new Runnable() {
        override def run() {
          val pb = new ProcessBuilder("java", "-Xmx128m", "-Djnlp.numberrace.locale=" + cur_locale.toString, "-jar", core_path)
          pb.redirectErrorStream(true)

          val process = pb.start

          val output = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream))

          try {
            var line = output.readLine
            while (line != null) {
              System.out.println(line)
              line = output.readLine
            }
          } catch { case e: Exception => e.printStackTrace }
          println("END")
        }
      }).start

    } else {
      println("I do not know where numberrace-core jar file. Not running game")
    }

  }

  var core_path: String = null

  def start(path2core_jar: String) = {
    main(null)
    core_path = path2core_jar
  }
}
