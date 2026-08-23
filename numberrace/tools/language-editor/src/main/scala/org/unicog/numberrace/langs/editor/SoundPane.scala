package org.unicog.numberrace.langs.editor

import scala.swing._
import event._
import BorderPanel.Position._

import EditorApp._
import org.unicog.numberrace.sound.{ SoundManager, SoundListener }

object SoundPane extends BorderPanel with SoundListener {
  private[editor] lazy val player = SoundManager.getInstance

  private[this] var key: String = null
  private[this] var path: String = null

  def soundKey = (key, path)
  def soundKey_=(k: (String, String)) = {
    key = k._1
    path = k._2

    playStop.enabled = null != getResourceURL(path)
  }

  private var playing = false

  val playStop = new Button()
  playStop.borderPainted = false
  playStop.contentAreaFilled = false
  playStop.focusPainted = false
  playStop.icon = runIcon

  add(playStop, Center)

  listenTo(playStop)

  reactions += {
    case ActionEvent(`playStop`) =>
      if (playing) {
        player.stopAllSounds
        run
      } else {
        playing = true
        playStop.icon = stopIcon

        // the way sounds are mapped now between properties and soundKeys
        player.play(key.split("\\.").last, this)
      }
  }

  def run = {
    playing = false
    playStop.icon = runIcon
  }

  def stop = {
    if (playing) {
      player.stopAllSounds
      run
    }
  }

}
