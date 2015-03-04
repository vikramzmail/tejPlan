/*
 * SingularException.java
 *
 * Created on May 4, 2006, 11:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.math.matrix;

/**
 *
 * @author Kristopher T. Beck
 */
public class SingularException extends MatrixException {
    int row, col;
    /** Creates a new instance of SingularException */
    public SingularException(int row, int col) {
        super("Singular Exception has occered at " + 
                row + ", " + col);
        this.row = row;
        this.col = col;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }
    
    public SingularException(int index) {
        super("Singular Exception has occered at " + 
                index + ", " + index);
        row = index;
        col = index;
    } 
}
