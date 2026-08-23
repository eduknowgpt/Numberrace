package org.unicog.numberrace.langs.editor

import javax.swing.SwingUtilities
import scala.swing.{Frame, GUIApplication}

abstract class MySimpleGuiApplication extends GUIApplication {
  lazy val mainFrame: Frame = top

  protected def top : Frame

  def main(args: Array[String]) = {
    SwingUtilities.invokeLater {
      new Runnable { def run() { init(); mainFrame.pack(); mainFrame.visible = true } }
    }
  }
  
  def resourceFromClassloader(path: String): java.net.URL =
    this.getClass.getResource(path)
  
  def resourceFromUserDirectory(path: String): java.io.File =
    new java.io.File(System.getProperty("user.dir"), path)
}
