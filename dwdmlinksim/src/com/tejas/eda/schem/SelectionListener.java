/*
 * SelectionListener.java
 *
 * Created on May 9, 2007, 7:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.eda.schem;

/**
 *
 * @author Kristopher T. Beck
 */
public interface SelectionListener {
    
    public void selected(SelectionEvent e);
    
    public void relessed(SelectionEvent e);
    
    public void moved(SelectionEvent e);
}
