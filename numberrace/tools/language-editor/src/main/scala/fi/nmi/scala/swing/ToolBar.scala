package fi.nmi.scala.swing
 
import javax.swing.JToolBar

import scala.swing.Component;
import scala.swing.Container;

class ToolBar extends Component with Container.Wrapper {
  
  override lazy val peer : JToolBar = new JToolBar with SuperMixin
  
  def floatable = peer.isFloatable 
  def floatable_=(f : Boolean) { peer.setFloatable(f) }
  
  def addSeparator = peer.addSeparator
}     
