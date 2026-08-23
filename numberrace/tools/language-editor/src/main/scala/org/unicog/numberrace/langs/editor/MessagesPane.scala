package org.unicog.numberrace.langs.editor

import scala.swing._
import event._
import EditorApp._
import Utils._
import BorderPanel.Position._
import java.util.Properties
import fi.nmi.scala.swing.ToolBar

class MessagesPane extends BorderPanel {
  val doc = new javax.swing.text.DefaultStyledDocument
  val props = new Properties
  
  val keysList = new ListView[String]()
  keysList.selection.intervalMode = ListView.IntervalMode.Single
  
  keysList.renderer = new ListView.AbstractRenderer[String, CheckBox](new CheckBox) {
    override def configure(list: ListView[_], isSelected: Boolean, hasFocus: Boolean, a: String, index: Int) = {
      component.selected = props.containsKey(a)
      component.text = a
    }
  }

  val textValue = new TextArea
  textValue.font = textValue.font.deriveFont(14.f)
  
  listenTo(keysList.selection, textValue)
  
  val contentPane = new BorderPanel
  val valueLabel = new Label
  valueLabel.horizontalAlignment = Alignment.Left
  valueLabel.border = javax.swing.BorderFactory.createEtchedBorder
  
  contentPane.layout += (valueLabel -> North, new ScrollPane(textValue) -> Center)
  
  val saveAct = Action("save")(save)
  saveAct.enabled = false
  val tb = new ToolBar
  tb.floatable = false
  tb.peer.add(saveAct.peer)
  
  add(new ScrollPane(keysList), West)
  add(contentPane, Center)
  add(tb, North)

  var changeUponListSelection = false
  
  reactions += {
    case ListSelectionChanged(`keysList`, _, false) =>
      changeUponListSelection = true
      if(keysList.selection.items.isEmpty) {
        valueLabel.text = ""
        textValue.text = ""
      } else {
        valueLabel.text = String.format("<html>%s</html>", messages.getString(keysList.selection.items.last).replace("\n","<br>"))
        textValue.text = props.getProperty(keysList.selection.items.last)
      }
      changeUponListSelection = false
      print("!\n")
    case ValueChanged(`textValue`) =>
      print("*")
      if(!changeUponListSelection && !keysList.selection.indices.isEmpty) {
        saveAct.enabled = true
        if(textValue.text.isEmpty) {
          props.remove(keysList.selection.items.last)
        } else {
          props.setProperty(keysList.selection.items.last, textValue.text)
        }
      }
  }
  
  def load = {
    import java.io.BufferedInputStream
    import java.io.FileInputStream
    import scala.collection.JavaConversions._
    
    keysList.listData = (messages.keySet.toList -- List("DIFFICULTY_LEVELS","localized_path","Game.fullScreen","Game.mode_q","Game.window")).sort(_ < _)
    props.load(new BufferedInputStream(new FileInputStream(messages_file)))
    props.setProperty("localized_path", cur_locale.toString)
  }
  
  def save : Unit = {
    import java.io.BufferedOutputStream
    import java.io.FileOutputStream
    
    saveAct.enabled = false
    props.store(new BufferedOutputStream(new FileOutputStream(messages_file)), null)
  }
  
}
