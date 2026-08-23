package fi.nmi.scala.swing

import javax.swing.JTextPane
import javax.swing.text.StyledDocument

class TextPane extends EditorPane {
  override lazy val peer : JTextPane = new JTextPane
  
  def this(doc : StyledDocument) = { this(); peer.setStyledDocument(doc)}
  
  def styledDocument  = peer.getStyledDocument
  def styledDocument_=(doc : StyledDocument) { peer.setStyledDocument(doc) }
  
}
