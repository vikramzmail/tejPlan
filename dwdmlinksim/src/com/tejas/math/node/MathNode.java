/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.node;

import com.tejas.math.Mathlet;
import com.tejas.math.Term;
import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;

/**
 *
 * @author Kristopher T. Beck
 */
public class MathNode {

    MathNode left;
    MathNode right;
    Mathlet here;
    
    public boolean isLeaf(){
        if(here instanceof Real || here instanceof Complex || here instanceof Term){
            return true;
        }
        return false;
    }
}
