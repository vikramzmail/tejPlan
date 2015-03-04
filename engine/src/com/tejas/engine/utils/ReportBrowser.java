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

import com.tejas.engine.utils.HTMLUtils;
import com.tejas.engine.internal.ErrorHandling;
import com.tejas.engine.utils.HTMLUtils.CustomHTMLEditorKit;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultCaret;

/**
 * Class to show HTML files with images in a panel.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ReportBrowser extends JPanel
{
    private static final long serialVersionUID = 1L;

    private JEditorPane editor;

    /**
     * Default constructor.
     *
     * @param html HTML to be shown (version 3.2 compatible, no Javascript)
     */
    public ReportBrowser(String html)
    {
	editor = new JEditorPane();
	editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        // Configure HTML viewer
	final CustomHTMLEditorKit htmlEditorKit = new CustomHTMLEditorKit();
	editor.setEditorKit(htmlEditorKit);
	editor.setContentType("text/html");
	editor.setEditable(false);
	editor.setText(html);
	((DefaultCaret) editor.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
	editor.addHyperlinkListener(new HyperlinkListener()
	{
	    @Override
	    public void hyperlinkUpdate(HyperlinkEvent e)
	    {
		try
		{
		    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
		    {
			editor.scrollToReference(e.getDescription().substring(1));
		    }
		}
		catch (Throwable ex)
		{
                    ErrorHandling.addErrorOrException(ex, ReportBrowser.class);
		    ErrorHandling.showErrorDialog("Please, check console for more information", "Error exporting report");
		}
	    }
	});

	// Generate toolbar
	JToolBar buttonBar = new JToolBar();
	JButton viewInNavigator = new JButton("View in navigator");
	viewInNavigator.addActionListener(new ActionListener()
	{

	    @Override
	    public void actionPerformed(ActionEvent ae)
	    {
		try
		{
		    File htmlName = File.createTempFile("tmp_", ".html");
                    
                    String html = HTMLUtils.includeNet2PlanHeader(editor.getText());
                    
                    JEditorPane editor1 = new JEditorPane();
                    editor1.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
                    CustomHTMLEditorKit htmlEditorKit1 = new CustomHTMLEditorKit();
                    editor1.setEditorKit(htmlEditorKit1);
                    editor1.setContentType("text/html");
                    editor1.setEditable(false);
                    editor1.setText(html);

                    Map<String, Image> path2Image = htmlEditorKit.getImages();
                    Map<String, Image> aux = htmlEditorKit1.getImages();
                    for(Entry<String, Image> entry : aux.entrySet())
                    {
                        String path = entry.getKey();
                        if (!path.endsWith("/resources/reportHeader.png")) continue;
                        
                        path2Image.put(path, entry.getValue());
                    }
                    
		    HTMLUtils.exportToHTML(htmlName, html, path2Image);
		    Desktop.getDesktop().browse(htmlName.toURI());
		}
		catch (Throwable ex)
		{
                    ErrorHandling.addErrorOrException(ex, ReportBrowser.class);
		    ErrorHandling.showErrorDialog("Please, check console for more information", "Error exporting report");
		}
	    }
	});

        buttonBar.setFloatable(false);
	buttonBar.add(viewInNavigator);

	setLayout(new BorderLayout());
	add(buttonBar, BorderLayout.NORTH);
	add(new JScrollPane(editor), BorderLayout.CENTER);
    }
}
