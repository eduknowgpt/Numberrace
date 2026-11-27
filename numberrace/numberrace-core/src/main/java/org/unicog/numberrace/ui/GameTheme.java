package org.unicog.numberrace.ui;
import java.awt.*;
public final class GameTheme {
 public static final Color INK=new Color(23,62,105), BLUE=new Color(12,101,183);
 public static Graphics2D graphics(Graphics g){Graphics2D p=(Graphics2D)g.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);return p;}
 public static void surface(Graphics2D p,int w,int h,Color top,Color bottom,int r){p.setColor(new Color(0,45,80,70));p.fillRoundRect(3,5,w-6,h-6,r,r);p.setPaint(new GradientPaint(0,0,top,0,Math.max(1,h-5),bottom));p.fillRoundRect(2,1,w-5,h-7,r,r);p.setColor(bottom.darker());p.drawRoundRect(2,1,w-5,h-7,r,r);p.setColor(new Color(255,255,255,155));p.drawRoundRect(5,4,w-11,h-13,r-4,r-4);}
 private GameTheme(){}
}