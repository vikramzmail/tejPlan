/*
 * AbstractNumber.java
 *
 * Created on April 9, 2006, 1:09 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.math.numbers;

import com.tejas.math.Mathlet;

import javolution.lang.Realtime;
import javolution.lang.Reusable;
import javolution.lang.ValueType;
/**
 *
 * @param N 
 * @author Kristopher T. Beck
 */
public abstract class Numeric<N extends Numeric<N>> implements Realtime,
        ValueType, Reusable, Mathlet, Field<N> {
/*
    public static Factory FACTORY;
    protected static <I extends Numeric> I newInstance(){
        return (I)FACTORY.object();
    }
 */
    public abstract N copy();
    
    public abstract void reset();

    public N plus(double y){
        N z = this.copy();
        z.plusEq(y);
        return z;
    }
    
    public N minus(double y){
        N z = this.copy();
        z.minusEq(y);
        return z;
    }
    
    public N times(double y){
        N z = this.copy();
        z.timesEq(y);
        return z;
    }
    
    public N divide(double y){
        N z = this.copy();
        z.divideEq(y);
        return z;
    }
        
    public N plus(N y){
        N z = this.copy();
        z.plusEq(y);
        return z;
    }
    
    public N minus(N y){
        N z = this.copy();
        z.minusEq(y);
        return z;
    }
    
    public N times(N y){
        N z = this.copy();
        z.timesEq(y);
        return z;
    }
    
    public N divide(N y){
        N z = this.copy();
        z.divideEq(y);
        return z;
    }
    
    public N inverse(){
        N z = this.copy();
        z.inverseEq();
        return z;
    }
    
    public abstract void inverseEq();
    
    public abstract void plusEq(double value);
    
    public abstract void minusEq(double value);
    
    public abstract void timesEq(double value);
    
    public abstract void divideEq(double value);
    
    /**
     * Adds value of y to this value.
     * Equivalant to: x += y;
     *    where x = this;
     * @param value
     */
    public abstract void plusEq(N value);
    
    public abstract void minusEq(N value);
    
    public abstract void timesEq(N value);
    
    public abstract void divideEq(N value);
     
    public abstract N pow(double exp);

    public abstract N pow(N exp);

    public N pow2(){
        return pow(2);
    }

    public abstract N sqrt();
   
    public abstract double abs();
    
    public abstract N exp();
    
    public abstract N log();
    
    public abstract N log10();
    
    public abstract boolean isZero();
    
    public abstract void setZero();
    
    public abstract boolean isOne();
    
    public abstract void setOne();
    
    public abstract void set(N n);
    
    public abstract void negateEq();
    
    public N negate(){
        N n = this.copy();
        n.negateEq();
        return n;
    }
}
