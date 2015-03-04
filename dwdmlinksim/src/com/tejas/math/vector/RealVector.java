/*
 * RealVector.java
 *
 * Created on April 23, 2006, 11:30 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.math.vector;

import com.tejas.math.numbers.Real;

import javolution.context.ObjectFactory;

/**
 *
 * @author Kristopher T. Beck
 */
public class RealVector extends Vector<Real>{
    public static ObjectFactory<RealVector> FACTORY = new ObjectFactory<RealVector>(){
        protected RealVector create(){
            return new RealVector();
        }
    };
    /** Creates a new instance of RealVector */
    public  RealVector() {
    }
    
    public RealVector copy() {
        RealVector v = FACTORY.object();
        v.addAll(this);
        return v;
    }
    
    public static RealVector newInstance(int size, Real fill){
        RealVector v = FACTORY.object();
        v.fill = fill;
        v.setSize(size);
        return v;
    }
    
    public static RealVector newInstance(){
        RealVector v = (RealVector)FACTORY.object();
        return v;
    }
    
    public double getDouble(int index){
        return get(index).get();
    }
    public void setDouble(int index, double real){
        get(index).set(real);
    }
}
