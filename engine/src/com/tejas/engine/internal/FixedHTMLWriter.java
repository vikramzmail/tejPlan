package com.tejas.engine.internal;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLWriter;

public class FixedHTMLWriter extends HTMLWriter
{
    private Vector                      tags                = new Vector(10);
    private Vector                      tagValues       = new Vector(10);
    private Vector                      tagsToRemove    = new Vector(10);
    private MutableAttributeSet convAttr            = new SimpleAttributeSet();
    private MutableAttributeSet oConvAttr       = new SimpleAttributeSet();

    public FixedHTMLWriter(Writer w, HTMLDocument doc, int pos, int len)
    {
        super(w, doc, pos, len);
    }

    AttributeSet convertToHTML(AttributeSet from, MutableAttributeSet to)
    {
        if (to == null)
        {
            to = convAttr;
        }
        to.removeAttributes(to);
        if (from != null)
        {
            Enumeration keys = from.getAttributeNames();
            String value = "";
            while (keys.hasMoreElements())
            {
                       Object key = keys.nextElement();
                if (key instanceof CSS.Attribute)
                {
                    value +=  key + ": " + from.getAttribute(key) + ";";
                    if (keys.hasMoreElements())
                        value += " ";
                }

                else
                {
                    to.addAttribute(key, from.getAttribute(key));
                }
            }
            if (value.length() > 0)
            {
                to.addAttribute(HTML.Attribute.STYLE, value);
            }
        }
        return to;
    }

    @Override
    protected void closeOutUnwantedEmbeddedTags(AttributeSet attr) throws IOException
    {
        tagsToRemove.removeAllElements();
        // translate css attributes to html
        attr = convertToHTML(attr, null);
        HTML.Tag t;
        Object tValue;
        int firstIndex = -1;
        int size = tags.size();
        // First, find all the tags that need to be removed.
        for (int i = size - 1; i >= 0; i--)
        {
            t = (HTML.Tag) tags.elementAt(i);
            tValue = tagValues.elementAt(i);
            if ((attr == null) || noMatchForTagInAttributes(attr, t, tValue))
            {
                firstIndex = i;
                tagsToRemove.addElement(t);
            }
        }
        if (firstIndex != -1)
        {
            // Then close them out.
            boolean removeAll = ((size - firstIndex) == tagsToRemove.size());
            for (int i = size - 1; i >= firstIndex; i--)
            {
                t = (HTML.Tag) tags.elementAt(i);
                if (removeAll || tagsToRemove.contains(t))
                {
                    tags.removeElementAt(i);
                    tagValues.removeElementAt(i);
                }
                write('<');
                write('/');
                write(t.toString());
                write('>');
            }
            // Have to output any tags after firstIndex that still remaing,
            // as we closed them out, but they should remain open.
            size = tags.size();
            for (int i = firstIndex; i < size; i++)
            {
                t = (HTML.Tag) tags.elementAt(i);
                write('<');
                write(t.toString());
                Object o = tagValues.elementAt(i);
                if (o != null && o instanceof AttributeSet)
                {
                    writeAttributes((AttributeSet) o);
                }
                write('>');
            }
        }
    }

    private boolean noMatchForTagInAttributes(AttributeSet attr, HTML.Tag t, Object tagValue)
    {
        if (attr != null && attr.isDefined(t))
        {
            Object newValue = attr.getAttribute(t);
            if ((tagValue == null) ? (newValue == null) : (newValue != null && tagValue.equals(newValue)))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void writeEmbeddedTags(AttributeSet attr) throws IOException
    {
        // translate css attributes to html
        attr = convertToHTML(attr, oConvAttr);
        Enumeration names = attr.getAttributeNames();
        while (names.hasMoreElements())
        {
            Object name = names.nextElement();
            if (name instanceof HTML.Tag)
            {
                HTML.Tag tag = (HTML.Tag) name;
                if (tag == HTML.Tag.FORM || tags.contains(tag))
                {
                    continue;
                }
                write('<');
                write(tag.toString());
                Object o = attr.getAttribute(tag);
                if (o != null && o instanceof AttributeSet)
                {
                    writeAttributes((AttributeSet) o);
                }
                write('>');
                tags.addElement(tag);
                tagValues.addElement(o);
            }
        }
    }

    @Override
    protected void writeAttributes(AttributeSet attr) throws IOException
    {
        convAttr.removeAttributes(convAttr);
        convertToHTML(attr, convAttr);
        Enumeration names = convAttr.getAttributeNames();
        while (names.hasMoreElements())
        {
            Object name = names.nextElement();
            if (name instanceof HTML.Tag || name instanceof StyleConstants || name == HTML.Attribute.ENDTAG)
            {
                continue;
            }
            write(" " + name + "=\"" + convAttr.getAttribute(name) + "\"");
        }
    }
}