package fi.nmi.scala.swing

import javax.swing.JEditorPane
import javax.swing.text.Document
import scala.swing.TextComponent

class EditorPane extends TextComponent {
  override lazy val peer : JEditorPane = new JEditorPane

  def clear = {
    document.putProperty(Document.StreamDescriptionProperty, null);
    peer.setText(null)
  }

  def document = peer.getDocument 
  
  def page = peer.getPage
  @throws(classOf[java.io.IOException])
  def page_=(p : java.net.URL) { peer.setPage(p) }
  
  def contentType = peer.getContentType
  def contentType_=(t : String ) { peer.setContentType(t) }
  
  import javax.swing.event.{DocumentListener, DocumentEvent}
  import scala.swing.TextComponent;
  import scala.swing.event.ValueChanged
  
  private val docListener = new DocumentListener {
    def changedUpdate(e:DocumentEvent) { publish(new ValueChanged(EditorPane.this)) }
    def insertUpdate(e:DocumentEvent) { publish(new ValueChanged(EditorPane.this)) }
    def removeUpdate(e:DocumentEvent) { publish(new ValueChanged(EditorPane.this)) }
  }
 
  peer.addPropertyChangeListener("document", new java.beans.PropertyChangeListener() {
                                   override def propertyChange(evt : java.beans.PropertyChangeEvent) = {
                                     evt.getOldValue.asInstanceOf[Document].removeDocumentListener(docListener)
                                     evt.getNewValue.asInstanceOf[Document].addDocumentListener(docListener)
                                   }
                                 })
}
