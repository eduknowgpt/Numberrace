package org.unicog.numberrace.screens;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

public class ScaleUtils {

    static public double resolutionCoef = 1.;

    static final public boolean doScale() {
        return resolutionCoef != 1. && resolutionCoef > 0;
    }

    static public Rectangle translateRect(Rectangle rect) {
        if (doScale()) {
            rect.x *= resolutionCoef;
            rect.y *= resolutionCoef;
            rect.height *= resolutionCoef;
            rect.width *= resolutionCoef;
        }
        return rect;
    }

    static public Point translatePoint(Point point) {
        if (doScale()) {
            point.x *= resolutionCoef;
            point.y *= resolutionCoef;
        }
        return point;
    }

    static public Dimension translateDimension(Dimension dim) {
        if (doScale()) {
            dim.width *= resolutionCoef;
            dim.height *= resolutionCoef;
        }
        return dim;
    }

    static public Insets translateInsets(Insets insets) {
        if (doScale()) {
            insets.top *= resolutionCoef;
            insets.left *= resolutionCoef;
            insets.right *= resolutionCoef;
            insets.left *= resolutionCoef;
        }
        return insets;
    }

    static public float f(float val) {
        if (doScale()) {
            return (float) resolutionCoef * val;
        }
        return val;
    }

    static public int i(int val) {
        if (doScale()) {
            return (int) Math.round(resolutionCoef * val);
        }
        return val;
    }

}
