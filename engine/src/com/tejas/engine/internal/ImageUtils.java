/*******************************************************************************
 * Copyright (c) 2013-2014 Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza - initial API and implementation
 ******************************************************************************/

package com.tejas.engine.internal;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Auxiliary functions to work with images.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ImageUtils
{
    /**
     * Image type
     * 
     * @since 0.2.3
     */
    public enum ImageType
    {
        /**
         * Bitmap file.
         * 
         * @since 0.2.3
         */
        BMP,
        
        /**
         * JPG file.
         * 
         * @since 0.2.3
         */
        JPG,

        /**
         * PNG file.
         * 
         * @since 0.2.3
         */
        PNG };
    
    /**
     * Converts an <code>Image</code> to a <code>BufferedImage</code>.
     *
     * @param im <code>Image</code>
     * @return A <code>BufferedImage</code>
     * @since 0.2.0
     */
    public static BufferedImage imageToBufferedImage(Image im)
    {
	BufferedImage bi = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_ARGB);
	Graphics bg = bi.getGraphics();
	bg.drawImage(im, 0, 0, null);
	bg.dispose();
	return bi;
    }

    /**
     * Reads an image from a file.
     *
     * @param file File
     * @return A <code>BufferedImage</code>
     * @since 0.2.0
     */
    public static BufferedImage readImageFromFile(File file)
    {
	try { return ImageIO.read(file); }
	catch(Throwable e) { throw new RuntimeException(e); }
    }

    /**
     * Writes an image to a file.
     *
     * @param file Output file
     * @param bufferedImage Image to save
     * @param imageType Image type (bmp, jpg, png)
     * @since 0.2.3
     */
    public static void writeImageToFile(File file, BufferedImage bufferedImage, ImageType imageType)
    {
	try
	{
            switch(imageType)
            {
                case BMP:
                    ImageIO.write(bufferedImage, "bmp", file);
                    break;
                    
                case JPG:
                    ImageIO.write(bufferedImage, "jpg", file);
                    break;
                    
                case PNG:
                    ImageIO.write(bufferedImage, "png", file);
                    break;
                    
                default:
                    throw new UnsupportedOperationException("Not implemented yet");
            }
	}
	catch(Throwable e)
	{
	    throw new RuntimeException(e);
	}
    }
}
