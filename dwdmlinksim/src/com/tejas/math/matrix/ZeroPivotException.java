/*
 * ZeroPivotException.java
 *
 * Created on May 6, 2006, 1:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.math.matrix;

/**
 *
 * @author Kristopher T. Beck
 */
public class ZeroPivotException extends SingularException{
    /** Creates a new instance of ZeroPivotException */
    public ZeroPivotException(Element elem) {
        super(elem.row, elem.col);
    }
    
    public ZeroPivotException(int index){
        super(index);
    }
    
}
