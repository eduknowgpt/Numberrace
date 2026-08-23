package org.unicog.numberrace.langs.editor

import scala.swing.event._
import java.util.Properties
import java.util.ResourceBundle
import java.io.{ FileInputStream, File }
import java.net.URL
import scala.collection.JavaConversions._
import EditorApp._
import Utils._
import scala.swing.FileChooser.SelectionMode._
import scala.swing.FileChooser.Result._
import javax.swing.filechooser.FileFilter
import javax.swing.Icon
import scala.collection.mutable.HashMap
import fi.nmi.scala.swing.CardPanel
import fi.nmi.scala.swing.ToolBar
import scala.swing.FileChooser
import scala.swing.BorderPanel
import scala.swing.CheckBox
import scala.swing.ListView
import scala.swing.ComboBox
import scala.swing.Button
import scala.swing.Label
import scala.swing.ScrollPane
import fi.nmi.scala.swing.EditorPane
import scala.swing.Action
import scala.swing.Dialog
import java.util.MissingResourceException

class ResourcesPane extends BorderPanel {

  lazy val fc = new FileChooser(new File(System.getProperty("user.dir")))
  lazy val img_cache = new HashMap[URL, Icon]
  lazy val explanations = ResourceBundle.getBundle("explanation")

  object View extends Enumeration {
    val IMAGE_VIEW = Value("image")
    val TEXT_VIEW = Value("text")
    val SOUND_VIEW = Value("sound")
    val NOTHING_VIEW = Value("nothing")
    implicit def view2string(v: Value): String = v.toString
  }
  import View._

  var currVeiw = NOTHING_VIEW

  val friend_num_counting_path = prb.getString("SoundManager.friend1_numbers_counting_path")
  val friend_num_path = prb.getString("SoundManager.friend1_numbers_path")
  val enemy_num_counting_path = prb.getString("SoundManager.enemy1_numbers_counting_path")
  val enemy_num_path = prb.getString("SoundManager.enemy1_numbers_path")

  val localResourceBundle = ((prb.keySet.toList.filter(prb.getString(_).contains("{0}"))
    -- List("SoundManager.friend1_numbers_counting_path", "SoundManager.friend1_numbers_path",
      "SoundManager.enemy1_numbers_counting_path", "SoundManager.enemy1_numbers_path", "SoundManager.gamesOutOf",
      "startMarker", "finishMarker", "menuButton"))).map(key => (key, prb.getString(key))).toMap ++ (0 to 40).map {
        n =>
          List(("%s_%d".format("friend1", n), "%s/%d.ogg".format(friend_num_path, n)),
            ("%s_%d".format("enemy1", n), "%s/%d.ogg".format(enemy_num_path, n)))
      }.flatten

  val keyList = new ListView(localResourceBundle.keys.toList.sort(_ < _)) // localResourceBundle.).sort(_ < _)
  keyList.selection.intervalMode = ListView.IntervalMode.Single

  val keyValue = new CheckBox
  keyValue.enabled = false

  keyList.renderer = new ListView.AbstractRenderer[String, CheckBox](new CheckBox) {
    override def configure(list: ListView[_], isSelected: Boolean, hasFocus: Boolean, a: String, index: Int) = {
      val path = localResourceBundle(a)

      component.selected = null != getResourceURL(path)

      if (path.contains("{1}")) {
        component.text = "<html><u>%s</u></html>".format(a)
      } else
        component.text = a
    }
  }

  val contentPane = new CardPanel

  import BorderPanel.Position._

  val resLabel = new Label()
  val textPane = new EditorPane {
    peer.putClientProperty(javax.swing.JEditorPane.HONOR_DISPLAY_PROPERTIES, java.lang.Boolean.TRUE)
  }
  textPane.contentType = "text/plain; charset=UTF-8"
  textPane.font.deriveFont(java.awt.Font.BOLD, 24.f)

  val textEditingPane = new BorderPanel
  val tb = new ToolBar
  tb.floatable = false
  tb.peer.setOrientation(javax.swing.SwingConstants.VERTICAL)
  val saveact = Action("Save")(saveCurrentFile)
  saveact.enabled = false
  tb.peer.add(saveact.peer)
  textEditingPane.layout += (tb -> East, new ScrollPane(textPane) -> Center)

  contentPane.layout += (resLabel -> IMAGE_VIEW, textEditingPane -> TEXT_VIEW, SoundPane -> SOUND_VIEW, new Label("Select Key") -> NOTHING_VIEW)
  contentPane.show(NOTHING_VIEW)

  val panel = new BorderPanel
  val replaceBtn = new Button("Replace")
  replaceBtn.enabled = false

  val explanation = new Label

  val detailsPane = new BorderPanel() {
    add(panel, North)
    add(contentPane, Center)
  }
  add(detailsPane, Center)
  add(new ScrollPane(keyList), West)

  val themes = new ComboBox(List("underWater", "inJungle")) {
    peer.setEditable(false)
  }

  val themeVariables = new org.unicog.numberrace.vars.ThemeVariables(0)

  themes.selection.index = 0

  panel.layout += (keyValue -> Center, replaceBtn -> East, explanation -> South, themes -> North)

  listenTo(keyList.selection, replaceBtn, themes.selection, textPane)

  reactions += {
    case SelectionChanged(`themes`) =>
      themeVariables.setLevel(themes.selection.index.toByte)
      if (!keyList.selection.indices.isEmpty) {
        val selected = keyList.selection.indices.leadIndex
        keyList.selection.indices.clear
        keyList.selection.indices += selected
      }
      repaint
    case ListSelectionChanged(`keyList`, range, false) =>
      replaceBtn.enabled = !keyList.selection.items.isEmpty
      SoundPane.stop
      if (!keyList.selection.items.isEmpty) {
        val key = keyList.selection.items.last
        import org.unicog.numberrace.util.{ Resources, ResourceProvider }
        val rawStr = localResourceBundle(key)
        val value = Resources.getLocalizedThemedPath(rawStr)
        keyValue.text = value
        val resURL = ResourceProvider.getResource(value)

        explanation.text = try {
          explanations.getString(key)
        } catch {
          case _ => ""
        }

        keyValue.selected = resURL != null

        value.split("\\.").last match {
          case "ogg" =>
            SoundPane.soundKey = (key, rawStr)
            show(SOUND_VIEW)
          case "txt" =>
            saveact.enabled = false
            setText(resURL)
            show(TEXT_VIEW)
          case "png" | "jpg" | "gif" =>
            updateImg(resURL)
            show(IMAGE_VIEW)

          case _ =>
            printf("Strange can not identify resources [%s]", value)
            show(NOTHING_VIEW)
        }
      } else contentPane.show(NOTHING_VIEW)
    case ActionEvent(`replaceBtn`) =>
      val key = keyList.selection.items.last
      fc.title = String.format("File for [%s]", key)
      fc.showOpenDialog(contentPane) match {
        case Approve =>
          val new_ext = fc.selectedFile.getName.split("\\.").last
          val value_ext = keyValue.text.split("\\.").last

          if (new_ext == value_ext) {
            printf("Need to replace file with [%s]", fc.selectedFile)
            copyFile(fc.selectedFile, new File(resources_dir + File.separator + keyValue.text))

            val path = localResourceBundle(key)
            currVeiw match {
              case IMAGE_VIEW =>
                val imgURL = getResourceURL(path)
                img_cache - imgURL
                updateImg(imgURL)
              case SOUND_VIEW =>
                SoundPane.soundKey = (key, path)
              case TEXT_VIEW =>
                textPane.clear
                saveact.enabled = false
                setText(getResourceURL(path))
              case _ =>
            }

          } else {
            Dialog.showMessage(contentPane, String.format("Extension of selected file [.%s] does not match expected one [.%s]", new_ext, value_ext))
          }
        case _ =>
      }
    case ValueChanged(`textPane`) =>
      saveact.enabled = true
  }

  private def setText(url: URL) = {
    deafTo(textPane)
    if (null != url) {
      textPane.page = url
    } else
      textPane.clear
    listenTo(textPane)
  }

  private def saveCurrentFile: Unit = {
    saveact.enabled = false
    val file = new File(resources_dir + File.separator + keyValue.text)
    if (!file.exists)
      ensureParentExists(file)

    import java.io.FileOutputStream
    import java.io.OutputStreamWriter
    import java.io.BufferedWriter

    val fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8"))
    fw.write(textPane.text)
    fw.close

  }

  private def show(view: View.Value) = {
    contentPane.show(view)
    currVeiw = view
  }

  private def updateImg(imgURL: URL) = {
    if (null != imgURL) {
      resLabel.icon = img_cache.getOrElseUpdate(imgURL, new javax.swing.ImageIcon(java.awt.Toolkit.getDefaultToolkit.createImage(imgURL)))
      resLabel.text = ""
    } else {
      resLabel.text = "Not Found"
      resLabel.icon = null
    }
  }
}
