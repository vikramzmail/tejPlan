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

package com.tejas.engine.utils;

import java.awt.Graphics;
import java.awt.Image;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public final class TransparentPanel extends JPanel
{
    private Image bgImage;
 
    /**
     * Default constructor.
     * 
     * @since 0.2.3
     */
    public TransparentPanel()
    {
        super();
        setOpaque(false);
    }
    
    /**
     * Sets the background image.
     * 
     * @param bgImage Background image (use <code>null</code> to remove it)
     * @since 0.2.3
     */
    public void setBackgroundImage(Image bgImage) { this.bgImage = bgImage; }
    
    /**
     * Reads the background image.
     * 
     * @param path Relative path to the class
     * @return Background image
     * @since 0.2.3
     */
    public ImageIcon createImage(String path)
    {
        URL imgURL = getClass().getResource(path);
        if (imgURL != null) return new ImageIcon(imgURL);

        System.err.println("Couldn't find file: " + path);
        return null;
    }
 
    @Override
    public void paint(Graphics g)
    {
        // First, draw the background image
        if (bgImage != null) g.drawImage(bgImage, 0, 0, null);

        // Then, the rest of the elements
        super.paint(g);
    }
}