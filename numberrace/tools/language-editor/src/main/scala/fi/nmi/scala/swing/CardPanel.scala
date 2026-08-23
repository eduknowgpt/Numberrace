package fi.nmi.scala.swing
import scala.swing.Panel
import scala.swing.LayoutContainer

class CardPanel extends Panel with LayoutContainer {
    import scala.swing.Component;
import scala.swing.LayoutContainer;
import scala.swing.Panel;
import java.awt.CardLayout

    override lazy val peer = new javax.swing.JPanel(new CardLayout)

    type Constraints = String
    
    def layoutManager = peer.getLayout.asInstanceOf[CardLayout]
    def show(con : String) = layoutManager.show(peer, con)
    
    protected def constraintsFor(comp: Component) = null
    protected def areValid(c: Constraints): (Boolean, String) = (true, "")
    protected def add(c: Component, l: Constraints) { peer.add(c.peer, l) }
  }
