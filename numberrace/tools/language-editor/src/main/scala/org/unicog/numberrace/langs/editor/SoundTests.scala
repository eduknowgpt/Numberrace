package org.unicog.numberrace.langs.editor

import java.awt.event.ActionListener

import scala.swing.GridBagPanel.Fill
import scala.swing.event.ActionEvent
import scala.swing.Alignment
import scala.swing.Button
import scala.swing.ComboBox
import scala.swing.GridBagPanel
import scala.swing.Label

import org.unicog.numberrace.sound.SoundListener
import org.unicog.numberrace.util.Utilities

import SoundPane.player
import javax.swing.Timer

class SoundTests extends GridBagPanel {
  import SoundPane._

  private def playVerbalStims(characterID: Int, num1: Int, num2: Int, addition: Boolean, l: org.unicog.numberrace.sound.SoundListener): Unit = {

    player.play(Utilities.charac4id(characterID) + Utilities.getVerbalForArabic(num1));
    List(new Timer(1000, new ActionListener() {
      override def actionPerformed(e: java.awt.event.ActionEvent) = {
        if (addition) {
          player.play(Utilities.charac4id(characterID) + "plus")
        } else {
          player.play(Utilities.charac4id(characterID) + "minus")
        }
      }
    }),
      new Timer(1600, new ActionListener() {
        override def actionPerformed(e: java.awt.event.ActionEvent) = {
          player.play(Utilities.charac4id(characterID) + num2)
        }
      }),
      new Timer(2600, new ActionListener() {
        override def actionPerformed(e: java.awt.event.ActionEvent) = {
          player.play(Utilities.charac4id(characterID) + "equals")
        }
      }),
      new Timer(3200, new ActionListener() {
        override def actionPerformed(e: java.awt.event.ActionEvent) = {
          player.play(Utilities.charac4id(characterID) + (if (addition) num1 + num2 else num1 - num2), l)
        }
      })).foreach(t => { t.setRepeats(false); t.start })
  }

  import org.unicog.numberrace.sound.SoundListener
  private def play(characterID: Int, num1: Int, num2: Int, addition: Boolean, l: SoundListener): Unit = {
    import SoundPane._

    val num2S = getDelayedSoundListener(150,
      player.play(Utilities.charac4id(characterID) + num2,
        getDelayedSoundListener(150, player.play(Utilities.charac4id(characterID) + "equals",
          getDelayedSoundListener(150,
            player.play(Utilities.charac4id(characterID) + (if (addition) num1 + num2 else num1 - num2), l))))))

    player.play(Utilities.charac4id(characterID) + num1, getDelayedSoundListener(150,
      if (addition) {
        player.play(Utilities.charac4id(characterID) + "plus", num2S)
      } else {
        player.play(Utilities.charac4id(characterID) + "minus", num2S)
      }))

  }

  private def playSequence(charId: Int, t: (Int, Int), l: SoundListener) = {
    val BMOVE_MS = 600;
    val nums = if (t._1 > t._2) Array.range(t._2, t._1 + 1) else Array.range(t._1, t._2 + 1)

//    val timers = new Timer(BMOVE_MS * nums.size - 100, new ActionListener() {
//      override def actionPerformed(e: java.awt.event.ActionEvent) = {
//        player.play(Utilities.charac4id(charId)
//          + (nums.last), l)
//      }
//    }) :: List.range(0, nums.size - 1).map(i => new Timer(BMOVE_MS * (i + 1), new ActionListener() {
//      override def actionPerformed(e: java.awt.event.ActionEvent) = {
//        player.play(Utilities.charac4id(charId)
//          + "counting_"
//          + nums(i))
//      }
//    }))
//

    def play(iter : Iterator[Int]) {
       if(iter.hasNext) {
         val i = iter.next()
         val last_? = !iter.hasNext
         val prefix = if(last_?) Utilities.charac4id(charId) else Utilities.charac4id(charId) + "counting_"
         player.play(prefix + i, if(last_?) l else new SoundListener() {
           override def run() {
             val movement_delay = new Timer(BMOVE_MS, new ActionListener() {
                 override def actionPerformed(e: java.awt.event.ActionEvent) = {
                     play(iter)
                 }
             })
             movement_delay.setRepeats(false)
             movement_delay.start
           }
         });
       }
    }

    play(nums.iterator)
  }

  private def getDelayedSoundListener(delay: Int, act: => Unit): SoundListener = {
    new SoundListener() {
      override def run = {
        if (delay > 0) {
          val t = new Timer(delay, new ActionListener() {
            override def actionPerformed(e: java.awt.event.ActionEvent) = {
              act
            }
          })
          t.setRepeats(false)
          t.start
        } else {
          act
        }
      }
    }
  }

  import scala.swing.{ ComboBox, Button }
  val addCB = new ComboBox(for (num1 <- List.range(0, 41); num2 <- List.range(0, 41) if num1 + num2 < 41) yield (num1, num2))
  val subtCB = new ComboBox(for (num1 <- List.range(1, 41); num2 <- List.range(0, 41) if num1 - num2 > -1) yield (num1, num2))
  addCB.peer.setFocusable(false)
  subtCB.peer.setFocusable(false)

  val newPlayAdd = new Button(" + ")
  val newPlaySubt = new Button(" - ")
  val play1_10 = new Button(" ... ")

  val l = new org.unicog.numberrace.sound.SoundListener {
    override def run = {
      newPlayAdd.enabled = true
      newPlaySubt.enabled = true
      play1_10.enabled = true
    }
  }

  val addLbl = new Label("Addition :")
  addLbl.horizontalAlignment = Alignment.Left
  val subtLbl = new Label("Subtraction :")
  subtLbl.horizontalAlignment = Alignment.Left
  val voiceOfLbl = new Label("Voice of ")
  voiceOfLbl.horizontalAlignment = Alignment.Left

  val charCB = new ComboBox(List("player", "rival"))
  charCB.peer.setFocusable(false)

  val con = new Constraints
  con.gridy = 0
  con.insets = new java.awt.Insets(3, 3, 3, 3)
  con.fill = Fill.Horizontal
  layout(voiceOfLbl) = con
  layout(charCB) = con

  con.gridy = 1
  layout(addLbl) = con
  layout(addCB) = con
  layout(newPlayAdd) = con
  layout(play1_10) = con
  con.gridy = 2
  layout(subtLbl) = con
  layout(subtCB) = con
  layout(newPlaySubt) = con

  listenTo(newPlayAdd, newPlaySubt, play1_10)

  import scala.swing.event.ActionEvent

  reactions += {
    case ActionEvent(src) =>
      src match {
        case `newPlayAdd` =>
          play(charCB.selection.index, addCB.selection.item._1, addCB.selection.item._2, true, l)
        case `newPlaySubt` =>
          play(charCB.selection.index, subtCB.selection.item._1, subtCB.selection.item._2, false, l)
        case `play1_10` =>
          playSequence(charCB.selection.index, addCB.selection.item, l)
        case _ =>
      }
      newPlayAdd.enabled = false
      newPlaySubt.enabled = false
      play1_10.enabled = false
  }

}
