//The Number Race: Remediation Software for dyscalculia.
//Copyright (C) Anna Wilson and Stanlislas Dehaene, 2004
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

//Source: AV & OM

package org.unicog.numberrace.util;

import java.awt.Graphics;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.ShortLookupTable;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import static org.unicog.numberrace.screens.ScaleUtils.*;

public class ImageFactory {
    static final private Logger log = Logger.getLogger("NUMBERRACE");
    //  static private Map<String, BufferedImage> images = new WeakHashMap<String, BufferedImage>();
    //    static private WeakHashMap compatibleImages = new WeakHashMap();

    private static BufferedImage fakeImage;

    static public BufferedImage getImage(String fileName) {
        fileName = Resources.getLocalizedThemedPath(fileName);

        Utilities.log.info("geting image: " + fileName);

        //        BufferedImage resultImage = images.get(fileName);

        //System.out.println("IF: Loading file: " + fileName);
        if (log.getLevel().intValue() >= Level.FINE.intValue()) {
            log.fine(fileName);
        }
        //        if (resultImage == null) {

        Utilities.log.info("image was null");

        try {
            URL url = ResourceProvider.getResource(fileName);

            Utilities.log.info("URL : " + url);

            if (url == null) {
                Utilities.log.info("can not find resource : " + fileName);
                return fakeImage();
            } else {
                if (log.getLevel().intValue() >= Level.FINE.intValue()) {
                    log.fine(url.toString());
                }
            }

            BufferedImage resultImage = toCompatibleImage(ImageIO.read(url));

            if (doScale()) {
                return getFasterScaledInstance(resultImage,
                                               (int) (resultImage.getWidth() * resolutionCoef),
                                               (int) (resultImage.getHeight() * resolutionCoef),
                                               RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                                               resolutionCoef < 1 ? true
                                                       : false);
            }
            Utilities.log.info("put to map : " + fileName);
            //                images.put(fileName, resultImage);
            return resultImage;
        } catch (IOException e) {
            Utilities.log.fine(e.getMessage());
        }
        return fakeImage();
        //        }
    }

    private static BufferedImage fakeImage() {
        if (fakeImage == null) {
            fakeImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_BGR);
        }
        return fakeImage;
    }

    static public BufferedImage getCompatibleImage(String fileName) {
        return getImage(fileName);
    }

    // src: Filthy Rick Clients p.126
    public static BufferedImage toCompatibleImage(BufferedImage image) {
        GraphicsConfiguration gc = getConfiguration();

        BufferedImage compatibleImage = image;

        if (!image.getColorModel().equals(gc.getColorModel())) {
            compatibleImage = gc.createCompatibleImage(image.getWidth(),
                                                       image.getHeight(),
                                                       image.getTransparency());
            Graphics g = compatibleImage.getGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }

        return compatibleImage;
    }

    public static BufferedImage createCompatibleTranslucentImage(int width,
            int height) {
        return getConfiguration().createCompatibleImage(width,
                                                        height,
                                                        Transparency.TRANSLUCENT);
    }

    /**
    * 
    * @see Filthy Rich Clients: Developing Animated and Graphical Effects for
    *      Desktop Java Applications. ISBN-10: 0132413930 ISBN-13:
    *      978-0132413930 Chapter 8, p.211
    */
    public static BufferedImage createNegative(BufferedImage img) {
        short[] data = new short[256];
        for (int i = 0; i < data.length; i++) {
            data[i] = (short) (255 - i);
        }

        LookupTable lookupTable = new ShortLookupTable(0, data);
        LookupOp op = new LookupOp(lookupTable, null);

        /* 
         * Not using createCompatibleTranslucentImage because under Windows we ale loosing ALPHA.
         * So 1st create ARGB image and then use toCompatibleImage 
         */
        BufferedImage dest = new BufferedImage(img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        /*
         * Book does not say I have to draw original to the destination before using LookupOp.
         * But under OSX without this step result is empty image.  
         */
        Graphics g = dest.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();

        op.filter(img, dest);
        return toCompatibleImage(dest);
    }

    private static GraphicsConfiguration getConfiguration() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
                                  .getDefaultScreenDevice()
                                  .getDefaultConfiguration();
    }

    /**
     * Convenience method that returns a scaled instance of the provided
     * BufferedImage.
     * 
     * 
     * @param img
     *            the original image to be scaled
     * @param targetWidth
     *            the desired width of the scaled instance, in pixels
     * @param targetHeight
     *            the desired height of the scaled instance, in pixels
     * @param hint
     *            one of the rendering hints that corresponds to
     *            RenderingHints.KEY_INTERPOLATION (e.g.
     *            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
     *            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
     *            RenderingHints.VALUE_INTERPOLATION_BICUBIC)
     * @param progressiveBilinear
     *            if true, this method will use a multi-step scaling technique
     *            that provides higher quality than the usual one-step technique
     *            (only useful in down-scaling cases, where targetWidth or
     *            targetHeight is smaller than the original dimensions)
     * @return a scaled version of the original BufferedImage
     * 
     * @see Filthy Rich Clients: Developing Animated and Graphical Effects for
     *      Desktop Java Applications. ISBN-10: 0132413930 ISBN-13:
     *      978-0132413930 Chapter 4, p.111
     */
    public static BufferedImage getFasterScaledInstance(BufferedImage img,
            int targetWidth, int targetHeight, Object hint,
            boolean progressiveBilinear) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        BufferedImage scratchImage = null;
        Graphics2D g2 = null;
        int prevW = ret.getWidth();
        int prevH = ret.getHeight();
        boolean isTranslucent = img.getTransparency() != Transparency.OPAQUE;

        // Use multi-step technique: start with original size, then
        // scale down in multiple passes with drawImage()
        // until the target size is reached
        // or
        // Use one-step technique: scale directly from original
        // size to target size with a single drawImage() call
        int w = progressiveBilinear && targetWidth < prevW ? prevW
                : targetWidth;
        int h = progressiveBilinear && targetHeight < prevH ? prevH
                : targetHeight;
        do {
            if (progressiveBilinear && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (progressiveBilinear && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            if (scratchImage == null || isTranslucent) {
                // Use a single scratch buffer for all iterations
                // and then copy to the final, correctly-sized image
                // before returning
                scratchImage = new BufferedImage(w, h, type);
                g2 = scratchImage.createGraphics();
            }
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, 0, 0, prevW, prevH, null);
            prevW = w;
            prevH = h;

            ret = scratchImage;
        } while (w != targetWidth || h != targetHeight);

        g2.dispose();

        // If we used a scratch buffer that is larger than our target size,
        // create an image of the right size and copy the results into it
        if (targetWidth != ret.getWidth() || targetHeight != ret.getHeight()) {
            scratchImage = new BufferedImage(targetWidth, targetHeight, type);
            g2 = scratchImage.createGraphics();
            g2.drawImage(ret, 0, 0, null);
            g2.dispose();
            ret = scratchImage;
        }

        return ret;
    }

}
