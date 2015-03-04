/*
 * ComplexVector.java
 *
 * Created on August 11, 2006, 8:34 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.math.vector;

import com.tejas.math.numbers.Complex;
import com.tejas.math.numbers.Real;

import javolution.context.ObjectFactory;
import javolution.lang.Realtime;
/**
 *
 * @author Kristopher T. Beck
 */
public class ComplexVector extends Vector<Complex> implements Realtime{
    public static final ObjectFactory<ComplexVector> FACTORY = new ObjectFactory<ComplexVector>(){
        protected ComplexVector create(){
            return new ComplexVector();
        }
    };
    /** Creates a new instance of ComplexVector */
    public ComplexVector() {
    }
    
    public static ComplexVector newInstance(int size, Real fill){
        ComplexVector v = FACTORY.object();
        return v;
    }
    
    public double getRealDouble(int index){
        return get(index).getReal();
    }
    
    public double getImagDouble(int index){
        return get(index).getReal();
    }
    
    public void setRealDouble(int index, double real){
        get(index).setReal(real);
    }
    
    public void setImagDouble(int index, double imag){
        get(index).setImag(imag);
    }
    
    public RealVector getRealVector(){
        RealVector v = RealVector.FACTORY.object();
        for(Complex c: this){
            v.add(Real.valueOf(c.getReal()));
        }
        return v;
    }
    
    public RealVector getImagVector(){
        RealVector v = RealVector.FACTORY.object();
        for(Complex c: this){
            v.add(Real.valueOf(c.getImag()));
        }
        return v;
    }
    
    public static ComplexVector valueOf(int size, Complex fill){
        ComplexVector v = FACTORY.object();
        for(int i = 0; i < size; i++){
            v.add(fill.copy());
        }
        return v;
    }
    
    public static ComplexVector valueOf(int size){
        ComplexVector v = FACTORY.object();
        for(int i = 0; i < size; i++){
            v.add(Complex.zero());
        }
        return v;
    }
}
