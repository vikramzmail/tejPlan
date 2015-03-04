/*
 * Wire.java
 *
 * Created on May 9, 2007, 6:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.eda.schem;

import java.awt.Shape;

import com.tejas.eda.Device;
import com.tejas.eda.node.Node;

import javolution.util.FastList;

/**
 *
 * @author Kristopher T. Beck
 */
public class Wire extends Device{
    Node node;
    FastList<Shape> lines;
    FastList<InterConnect> connects;
    /** Creates a new instance of Wire */
    public Wire() {
    }
    
}
