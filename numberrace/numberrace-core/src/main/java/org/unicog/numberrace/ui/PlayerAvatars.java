package org.unicog.numberrace.ui;
import javax.swing.*;import java.awt.*;import java.util.prefs.Preferences;import java.nio.charset.StandardCharsets;import java.util.UUID;import org.unicog.numberrace.data.Student;
public final class PlayerAvatars {
 private static final ImageIcon[] ART={GameAssets.load("avatar0.png"),GameAssets.load("avatar1.png"),GameAssets.load("avatar2.png"),GameAssets.load("avatar3.png")};
 private static String key(Student s){return UUID.nameUUIDFromBytes(s.getFileHeader().getBytes(StandardCharsets.UTF_8)).toString();}
 public static int get(Student s){try{return Preferences.userNodeForPackage(PlayerAvatars.class).getInt(key(s),-1);}catch(RuntimeException e){return -1;}}
 public static void save(Student s,int n){try{Preferences p=Preferences.userNodeForPackage(PlayerAvatars.class);if(n<0)p.remove(key(s));else p.putInt(key(s),n);}catch(RuntimeException e){java.util.logging.Logger.getLogger(PlayerAvatars.class.getName()).warning("Avatar preference unavailable");}}
 public static ImageIcon icon(int n,int size){if(n<0||n>=ART.length||ART[n]==null)return null;return new ImageIcon(ART[n].getImage().getScaledInstance(size,size,Image.SCALE_SMOOTH));}
 private PlayerAvatars(){}
}