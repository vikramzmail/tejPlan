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

import java.awt.Color;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.UIManager;

/**
 * Class with system utilities depending on the operating system and locale configuration.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class SystemUtils
{
    private static Class mainClass;
    
    /**
     * Types of user inteface.
     * 
     * @since 0.2.2
     */
    public enum UserInterface
    {
        /**
         * Command-line interface.
         * 
         * @since 0.2.2
         */
        CLI,

        /**
         * Graphical user interface.
         * 
         * @since 0.2.2
         */
        GUI
    };
    
    private static UserInterface ui = null;
    
    public static UserInterface getUserInterface() { return ui; }
    
    /**
     * Configures the environment for Net2Plan (locale, number format, look and feel...).
     * 
     * @param mainClass Main class of the running program
     * @param ui User interface (GUI or CLI)
     * @since 0.2.3
     */
    public static void configureEnvironment(Class mainClass, UserInterface ui)
    {
        SystemUtils.mainClass = mainClass;
        
        try { Locale.setDefault(Locale.US); }
        catch (Throwable e) { }

        try { NumberFormat.getInstance().setGroupingUsed(false); }
        catch (Throwable e) { }
        
        if (SystemUtils.ui != null) throw new RuntimeException("Environment was already configure");
            
        SystemUtils.ui = ui;

        if (ui == UserInterface.GUI)
        {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Throwable e) { }

            Color bgColor = new Color(200, 200, 200);
            Color fgColor = new Color(0, 0, 0);

            // More information on http://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.htm
            // "Customizing a XXX Look and Feel"

            UIManager.put("Button.background", bgColor);
            UIManager.put("CheckBox.background", bgColor);
            UIManager.put("CheckBox.interiorBackground", Color.WHITE);
            UIManager.put("ComboBox.background", Color.WHITE);
            UIManager.put("Label.background", bgColor);
            UIManager.put("Menu.background", bgColor);
            UIManager.put("MenuBar.background", bgColor);
            UIManager.put("MenuItem.background", bgColor);
            UIManager.put("OptionPane.background", bgColor);
            UIManager.put("Panel.background", bgColor);
            UIManager.put("ScrollPane.background", bgColor);
            UIManager.put("Separator.background", bgColor);
            UIManager.put("SplitPane.background", bgColor);
            UIManager.put("TabbedPane.background", bgColor);
            UIManager.put("Table.background", Color.WHITE);
            UIManager.put("TableHeader.background", bgColor);
            UIManager.put("TextArea.background", Color.WHITE);
            UIManager.put("TextField.background", Color.WHITE);
            UIManager.put("TextField.disabledBackground", bgColor);
            UIManager.put("TextField.inactiveBackground", bgColor);
            UIManager.put("ToolBar.background", bgColor);
            UIManager.put("Viewport.background", bgColor);
            UIManager.put("Button.foreground", fgColor);
            UIManager.put("CheckBox.foreground", fgColor);
            UIManager.put("ComboBox.foreground", fgColor);
            UIManager.put("Label.foreground", fgColor);
            UIManager.put("Label.disabledForeground", fgColor);
            UIManager.put("Menu.foreground", fgColor);
            UIManager.put("MenuBar.foreground", fgColor);
            UIManager.put("MenuItem.foreground", fgColor);
            UIManager.put("MenuItem.acceleratorForeground", fgColor);
            UIManager.put("OptionPane.foreground", fgColor);
            UIManager.put("Separator.foreground", fgColor);
            UIManager.put("TabbedPane.foreground", fgColor);
            UIManager.put("Table.foreground", fgColor);
            UIManager.put("TableHeader.foreground", fgColor);
            UIManager.put("TextArea.foreground", fgColor);
            UIManager.put("TextField.foreground", fgColor);
            UIManager.put("TextField.inactiveForeground", fgColor);
            UIManager.put("Separator.shadow", bgColor);
            UIManager.put("TitledBorder.titleColor", fgColor);
        }                
    }
    
    /**
     * Returns the current directory where the application is executing in.
     *
     * @return Current directory where the application is executing in
     * @since 0.2.0
     */
    public static File getCurrentDir()
    {
	try
	{
            if (mainClass == null) throw new RuntimeException("Bad");
            
	    File curDir = new File(mainClass.getProtectionDomain().getCodeSource().getLocation().toURI());
	    if (!curDir.isDirectory())
		curDir = curDir.getAbsoluteFile().getParentFile();
	    return curDir;

//	    return new File("test").getCanonicalFile().getParentFile();
	}
	catch(Throwable e)
	{
	    throw new RuntimeException(e);
	}
    }
    
    /**
     * Returns the system-dependent default name-separator character.
     *
     * @return System-dependent default name-separator character
     * @since 0.2.2
     */
    public static String getDirectorySeparator()
    {
        return File.separator;
    }

    /**
     * Returns the decimal separator
     *
     * @return The decimal separator
     * @since 0.2.0
     */
    public static char getDecimalSeparator()
    {
	return ((DecimalFormat) DecimalFormat.getInstance()).getDecimalFormatSymbols().getDecimalSeparator();
    }

    /**
     * Returns the name (without extension) of the given file.
     *
     * @param file File to analyze
     * @return File name
     * @since 0.2.0
     */
    public static String getFilename(File file)
    {
	if (!file.isFile()) throw new RuntimeException("'file' is not a valid file (i.e. it is a directory)");

	String name = file.getName();
	int pos = name.lastIndexOf(".");
	return pos == -1 ? name : name.substring(0, pos);
    }

    /**
     * Returns the extension of the given file.
     *
     * @param file File to analyze
     * @return File extension, an empty <code>String</code> ("") if it doesn't have extension
     * @throws IllegalArgumentException If <code>file</code> is not an existing file or it is a directory
     * @since 0.2.0
     */
    public static String getExtension(File file)
    {
	if (!file.isFile())
	    throw new IllegalArgumentException("'file' is not a valid file (i.e. it is a directory)");

	String name = file.getName();
	int pos = name.lastIndexOf(".");
	return pos == -1 ? "" : name.substring(pos + 1);
    }
}
