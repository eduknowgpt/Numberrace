package org.unicog.numberrace.ui;
import javax.swing.*;import java.net.URL;
public final class GameAssets {private GameAssets(){} public static ImageIcon load(String name){URL u=GameAssets.class.getResource("/ui/"+name);return u==null?null:new ImageIcon(u);}}