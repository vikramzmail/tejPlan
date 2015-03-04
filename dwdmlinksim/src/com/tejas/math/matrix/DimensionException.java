/*
 * DimentionException.java
 *
 * Created on January 18, 2007, 2:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.math.matrix;

/**
 *
 * @author Kristopher T. Beck
 */
public class DimensionException extends MatrixException{
    
    /** Creates a new instance of DimentionException */
    public DimensionException() {
    }
    
    public DimensionException(String msg){
        super(msg);
    }
    
}
