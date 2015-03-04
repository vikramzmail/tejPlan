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

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Extends JFileChooser to avoid the problem of users overwritting an existing file without any warning.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class FileChooserConfirmOverwrite extends JFileChooser
{
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     * 
     * @since 0.2.0
     */
    public FileChooserConfirmOverwrite()
    {
        super();
    }

    /**
     * Constructor that allows to set the current directory.
     * 
     * @param currentDirectory Current directory
     * @since 0.2.0
     */
    public FileChooserConfirmOverwrite(File currentDirectory)
    {
        super(currentDirectory);
    }

    @Override
    public void approveSelection()
    {
        File f = getSelectedFile();

        if(f.exists() && getDialogType() == SAVE_DIALOG)
	{
            int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file", JOptionPane.YES_NO_CANCEL_OPTION);

	    switch(result)
	    {
                case JOptionPane.YES_OPTION:
                    super.approveSelection();
                    return;

                case JOptionPane.NO_OPTION:
                    return;

                case JOptionPane.CLOSED_OPTION:
                    return;

                case JOptionPane.CANCEL_OPTION:
                    cancelSelection();
                    return;
            }
        }

	super.approveSelection();
    }
}
