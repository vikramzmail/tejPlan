/*
 * EDAComponent.java
 *
 * Created on April 18, 2007, 2:37 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.eda.schem;

import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.Stroke;
import javolution.util.FastList;
import org.apache.batik.swing.JSVGCanvas;

import com.tejas.eda.Terminal;

/**
 *
 * @author Kristopher T. Beck
 */
public class Symbol extends JSVGCanvas {//implements MouseListener, MouseMotionListener{
    FastList<Shape> shapes;
    FastList<Terminal> terminals;
    boolean selected;
    boolean grabbed;
    BasicStroke stroke;
    Color highlight;
    /** Creates a new instance of EDAComponent */
    public Symbol() {
    }

    public void setTerminals(FastList<Terminal> terminals) {
        this.terminals = terminals;
    }

    public FastList<Terminal> getTerminals() {
        return terminals;
    }

    public void setStroke(BasicStroke stroke) {
        this.stroke = stroke;
    }

    public BasicStroke getStroke() {
        return stroke;
    }

    public void setHighlight(Color highlight) {
        this.highlight = highlight;
    }

    public Color getHighlight() {
        
        return highlight;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

/*    
    public void mouseClicked(MouseEvent e) {
        if(e.getButton() == e.BUTTON1 && e.getClickCount() == 2){
            selected = true;
        }
        if(e.getButton() == e.BUTTON3 && e.getClickCount() == 1){
            if(selected){
                selected = false;
            } else if(grabbed){
                release();
            }
        }
    }
    
    public void mouseEntered(MouseEvent e) {
    }
    
    public void mouseExited(MouseEvent e) {
    }
    
    public void mousePressed(MouseEvent e) {
        if(((Schematic)getParent()).getSelectMode() == select){
            selected = true;
        }
    }
    
    public void mouseReleased(MouseEvent e) {
        release();
    }

    public void mouseDragged(MouseEvent e) {
    }
*/    
    @Override
    public void paint(Graphics g){
   /*     AffineTransform trm = g.getTransform();
        g.setTransform(transform);
     */
        Graphics2D g2D = (Graphics2D)g;
        Color c = g2D.getColor();
        Stroke st = g2D.getStroke();
        if(selected){
            g2D.setColor(highlight);
            g2D.setStroke(stroke);
            super.paint(g2D);
            g2D.setColor(c);
            g2D.setStroke(st);
        }
   /*     for(Shape s: shapes){
            g.draw(s);
        }
        for(Terminal t: terminals){
            t.paint(g);
        }
    * */
    }
}
