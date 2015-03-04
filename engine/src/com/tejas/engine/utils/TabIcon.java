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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.Icon;

/**
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class TabIcon implements Icon
{
    /**
     *
     */
    final public static int PLUS_SIGN = 1;
    /**
     *
     */
    final public static int TIMES_SIGN = 2;

    private int iconType = -1;

    private int x_pos;
    private int y_pos;
    private int width;
    private int height;
    private Icon fileIcon;

    /**
     *
     * @param fileIcon
     * @param iconType
     */
    public TabIcon(Icon fileIcon, int iconType)
    {
        this.fileIcon = fileIcon;
        width = 16;
        height = 16;
        this.iconType = iconType;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        this.x_pos = x;
        this.y_pos = y;

        Color col = g.getColor();

        g.setColor(Color.black);
        int y_p = y + 2;

        // Border
        g.drawLine(x + 1, y_p, x + 12, y_p);
        g.drawLine(x + 1, y_p + 13, x + 12, y_p + 13);
        g.drawLine(x, y_p + 1, x, y_p + 12);
        g.drawLine(x + 13, y_p + 1, x + 13, y_p + 12);

        switch(iconType)
        {
            case PLUS_SIGN:
                g.drawLine(x + 3, y_p + 7, x + 10, y_p + 7);
                g.drawLine(x + 3, y_p + 6, x + 10, y_p + 6);
                g.drawLine(x + 7, y_p + 3, x + 7, y_p + 10);
                g.drawLine(x + 6, y_p + 3, x + 6, y_p + 10);
                break;

            case TIMES_SIGN:
                g.drawLine(x + 3, y_p + 3, x + 10, y_p + 10);
                g.drawLine(x + 3, y_p + 4, x + 9, y_p + 10);
                g.drawLine(x + 4, y_p + 3, x + 10, y_p + 9);
                g.drawLine(x + 10, y_p + 3, x + 3, y_p + 10);
                g.drawLine(x + 10, y_p + 4, x + 4, y_p + 10);
                g.drawLine(x + 9, y_p + 3, x + 3, y_p + 9);
                break;
                
            default:
                break;
        }

        g.setColor(col);
        if (fileIcon != null) { fileIcon.paintIcon(c, g, x + width, y_p); }
    }

    @Override
    public int getIconHeight() { return height; }
    
    @Override
    public int getIconWidth() { return width + (fileIcon != null ? fileIcon.getIconWidth() : 0); }
    
    /**
     *
     * @return
     */
    public Rectangle getBounds() { return new Rectangle(x_pos, y_pos, width, height); }
}

