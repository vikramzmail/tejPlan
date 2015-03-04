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

import com.tejas.engine.internal.FixedHTMLWriter;
import com.tejas.engine.internal.ImageUtils;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import javax.swing.JEditorPane;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.MinimalHTMLWriter;
import javax.swing.text.html.parser.ParserDelegator;
import org.jsoup.Jsoup;

/**
 * <p>Auxiliary functions to work with HTML. The intended use is for {@link com.net2plan.interfaces.IReport reports}.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class HTMLUtils
{
    /**
     * Includes a Net2Plan header to a HTML string. It is an internal function, 
     * and users do not need to use it.
     * 
     * @param html HTML content
     * @return A new HTML content including the Net2Plan header before the contents
     * @since 0.2.3
     */
    public static String includeNet2PlanHeader(String html)
    {
        org.jsoup.nodes.Document doc = Jsoup.parse(html);
        org.jsoup.nodes.Element title = doc.head().getElementsByTag("title").first();
        String reportTitle = title != null && title.hasText() ? title.text() : "Net2Plan Report";
        
        String header;
        try { header = HTMLUtils.getHTMLFromURL(HTMLUtils.class.getResource("/resources/reportHeader.html").toURI().toURL()); }
        catch(URISyntaxException | MalformedURLException ex) { throw new RuntimeException(ex); }
        header = header.replaceFirst("#ReportTitle#", reportTitle);
        doc.body().prepend(header);
        
        return doc.toString();
    }
    
    /**
     * <p>Save a HTML content to a a given file.</p>
     *
     * @param file A valid file
     * @param html HTML content
     * @since 0.2.0
     */
    public static void exportToHTML(File file, String html)
    {
        html = HTMLUtils.includeNet2PlanHeader(html);

        HTMLUtils.CustomHTMLEditorKit kit = new HTMLUtils.CustomHTMLEditorKit();
        JEditorPane editor = new JEditorPane();
        editor.setEditorKit(kit);
        editor.setContentType("text/html");
        editor.setText(html);
        HTMLUtils.exportToHTML(file, editor.getText(), kit.getImages());
    }

    /**
     * <p>Save a HTML content to a a given file.</p>
     * 
     * <p><b>Important</b>: This method is for internal use, users should use the other {@link com.tejas.engine.utils.HTMLUtils#exportToHTML(java.io.File, java.lang.String) exportToHTML} method.</p>
     *
     * @param file A valid file
     * @param html HTML content
     * @param images Map containing the image cache (URL -> Image)
     * @since 0.2.0
     */
    public static void exportToHTML(File file, String html, Map<String, Image> images)
    {
	try
	{
	    File dir = file.getAbsoluteFile().getParentFile();

	    for (Map.Entry<String, Image> entry : images.entrySet())
	    {
		File imgName = File.createTempFile("tmp_", ".png", dir);
		BufferedImage bi = ImageUtils.imageToBufferedImage(entry.getValue());
		ImageUtils.writeImageToFile(imgName, bi, ImageUtils.ImageType.PNG);
		html = html.replace(entry.getKey(), imgName.toURI().toURL().toString());
	    }
            
	    try (BufferedWriter out = new BufferedWriter(new FileWriter(file)))
	    {
		out.write(html);
	    }
	}
	catch(Throwable e)
	{
	    throw new RuntimeException(e);
	}
    }

    /**
     * <p>Returns the HTML text from a given file. It is a wrapper method for {@link #getHTMLFromURL getHTMLFromURL()}.</p>
     *
     * @param file A valid file
     * @return A HTML String
     * @since 0.2.0
     */
    public static String getHTMLFromFile(File file)
    {
        try
        {
	    URL url = file.toURI().toURL();
	    return getHTMLFromURL(url);
        }
        catch(Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>Returns the HTML text from a given URL.</p>
     *
     * <p>This method is intended for HTML files enclosed into the same JAR file as that enclosing the calling class. Therefore, the URL must point the location of the HTML file within that JAR file.</p>
     *
     * <p>For example, assuming that a file named "example.html" is in the path "/aux-files/html" within the JAR file, then the calling would be as follows:</p>
     *
     * <code>String html = HTMLUtils.getHTMLFromURL(getClass().getResource("/aux-files/html/examples.html").toURI().toURL());</code>
     *
     * <p><b>Important</b>: Image paths are converted to absolute paths.</p>
     *
     * @param url A valid URL
     * @return A HTML String
     * @since 0.2.0
     */
    public static String getHTMLFromURL(URL url)
    {
	StringBuilder content = new StringBuilder();
	try
	{
	    URLConnection urlConnection = url.openConnection();
	    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream())))
	    {
		String line;
		while ((line = bufferedReader.readLine()) != null)
		    content.append(line).append(String.format("%n"));
	    }
        }
        catch(Throwable e)
        {
            throw new RuntimeException(e);
        }

	final Set<String> list = new TreeSet<String>();

	ParserDelegator parserDelegator = new ParserDelegator();
	HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback()
	{
	    @Override
	    public void handleText(final char[] data, final int pos) { }

	    @Override
	    public void handleStartTag(HTML.Tag tag, MutableAttributeSet attribute, int pos)
	    {
		if (tag == HTML.Tag.IMG)
		{
		    String address = (String) attribute.getAttribute(HTML.Attribute.SRC);
		    list.add(address);
		}
	    }

	    @Override
	    public void handleEndTag(HTML.Tag t, final int pos) { }

	    @Override
	    public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, final int pos)
	    {
		if (t == HTML.Tag.IMG)
		{
		    String address = (String) a.getAttribute(HTML.Attribute.SRC);
		    list.add(address);
		}
	    }

	    @Override
	    public void handleComment(final char[] data, final int pos) { }

	    @Override
	    public void handleError(final String errMsg, final int pos) { }
	};

	String html = content.toString();
	Reader reader = new StringReader(html);

	try { parserDelegator.parse(reader, parserCallback, false); }
        catch(Throwable e) { throw new RuntimeException(e); }

	for(String item : list)
	{
	    try
	    {
		URL newURL = new URL(url, item);
		html = html.replace(item, newURL.toExternalForm());
	    }
	    catch(Throwable e)
	    {
		throw new RuntimeException(e);
	    }
	}

	return html;
    }

    /**
     * Custom version of <code>HTMLEditorKit</code> with an image cache.
     *
     * @since 0.2.0
     */
    public static class CustomHTMLEditorKit extends HTMLEditorKit
    {
        private static final long serialVersionUID = 1L;
        
	private Map<String, Image> imageCache;

	/**
	 * Default constructor
	 *
	 * @since 0.2.0
	 */
	public CustomHTMLEditorKit()
	{
	    super();
	    imageCache = new HashMap<String, Image>();
	}
        
	/**
	 * Returns the image cache
	 *
	 * @return Map containing the image cache
	 * @since 0.2.0
	 */
	public Map<String, Image> getImages() { return imageCache; }

	@Override
	public ViewFactory getViewFactory() { return new HTMLFactoryX(); }

        @Override
        public void write(Writer out, Document doc, int pos, int len) throws IOException, BadLocationException
        {
            if (doc instanceof HTMLDocument)
            {
                FixedHTMLWriter w = new FixedHTMLWriter(out, (HTMLDocument) doc, pos, len);
                w.write();
            }
            else if (doc instanceof StyledDocument)
            {
                MinimalHTMLWriter w = new MinimalHTMLWriter(out, (StyledDocument) doc, pos, len);
                w.write();
            }
            else
            {
                super.write(out, doc, pos, len);
            }
        }
        
        private class HTMLFactoryX extends HTMLEditorKit.HTMLFactory
	{
	    @Override
	    public View create(Element elem)
	    {
		Object obj = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
		if (obj instanceof HTML.Tag && (HTML.Tag) obj == HTML.Tag.IMG)
		{
		    String src = (String) elem.getAttributes().getAttribute(HTML.Attribute.SRC);

		    ImageView imageView = new ImageView(elem);
		    imageCache.put(src, imageView.getImage());

		    return imageView;
		}

		return super.create(elem);
	    }
        }
    }
}
