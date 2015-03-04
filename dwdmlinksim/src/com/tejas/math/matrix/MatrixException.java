/*
 * MatrixException.java
 *
 * Created on January 11, 2007, 4:05 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.math.matrix;

/**
 *
 * @author Kristopher T. Beck
 */
public class MatrixException extends RuntimeException{
    String message;
    /** Creates a new instance of MatrixException */
    public MatrixException() {
    }
    
    public MatrixException(String msg){
        message = msg;
    }

    @Override
    public String toString() {
        return message;
    }
    
}
