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

package com.tejas.engine.tools;

import com.tejas.engine.internal.SystemUtils;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Generic template for plugins (tools) within Net2Plan.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public abstract class IGUIModule extends JPanel
{
    private static final long serialVersionUID = 1L;

    /**
     * Reference to the current execution directory.
     * 
     * @since 0.2.0
     */
    protected final static File CURRENT_DIR;
    
    static { CURRENT_DIR = SystemUtils.getCurrentDir(); }
    
    /**
     * Reference to the main panel of the window.
     * 
     * @since 0.2.0
     */
    protected JPanel contentPane;

    /**
     * Default constructor.
     * 
     * @since 0.2.0
     */
    public IGUIModule()
    {
	this(null);
    }

    /**
     * Default constructor.
     * 
     * @param title Title of the plugin (or tool). It may be null
     * @since 0.2.0
     */
    public IGUIModule(String title)
    {
	if (title == null)
	{
	    setLayout(new MigLayout("insets 0 0 0 0, nocache", "[grow]", "[grow]"));
	}
	else
	{
	    setLayout(new MigLayout("insets 0 0 0 0, nocache", "[grow]", "[50px][grow]"));
	    JPanel pnl_title = new JPanel();

	    JLabel lbl_title = new JLabel(title);
	    pnl_title.setBackground(Color.YELLOW);
	    pnl_title.add(lbl_title);
	    lbl_title.setFont(new Font(lbl_title.getFont().getName(), Font.BOLD, 20));
	    lbl_title.setForeground(Color.BLACK);
            add(pnl_title, "grow, wrap");
	}

	contentPane = new JPanel(new MigLayout("fill, insets 0 0 0 0, nocache", "", ""));
        add(contentPane, "grow");
        contentPane.revalidate();
    }
    
    /**
     * Asks user to confirm plugin reset.
     * 
     * @return <code>true</code> if user confirms to reset the plugin, or <code>false</code> otherwise
     * @since 0.2.3
     */
    protected boolean askForReset()
    {
	int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to reset? This will remove all unsaved data", "Reset", JOptionPane.YES_NO_OPTION);
        
        return result == JOptionPane.YES_OPTION;
    }
}