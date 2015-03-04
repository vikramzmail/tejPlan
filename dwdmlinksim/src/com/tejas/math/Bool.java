/*
 * Bool.java
 *
 * Created on Aug 16, 2007, 10:55:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.math;

import javolution.context.ObjectFactory;
import javolution.lang.Realtime;
import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class Bool implements Realtime{
    public static final ObjectFactory<Bool> FACTORY = new ObjectFactory<Bool>(){
        protected Bool create(){
            return new Bool();
        }
    };
    
    protected boolean value;
    
    public Bool() {
    }
    
    public static Bool valueOf(boolean value){
        Bool b = FACTORY.object();
        b.set(value);
        return b;
    }
    
    public static Bool TRUE(){
        return valueOf(true);
    }
    
    public static Bool FALSE(){
        return valueOf(false);
    }
    
    public void setTrue(){
        value = true;
    }
    
    public void setFalse(){
        value = false;
    }
    
    public void set(boolean value){
        this.value = value;
    }
    
    public boolean isTrue(){
        return value;
    }
    
    public boolean isFalse(){
        return !value;
    }
    
    public Bool and(boolean value){
        return valueOf(this.value & value); 
    }
    
    public Bool and(Bool value){
        return valueOf(this.value & value.value); 
    }

    public void andEq(boolean value){
        this.value &= value; 
    }

    public void andEq(Bool value){
        this.value &= value.value; 
    }

    public Bool nand(boolean value){
        return valueOf(!(this.value & value)); 
    }
    
    public Bool nand(Bool value){
        return valueOf(!(this.value & value.value)); 
    }

    public void nandEq(boolean value){
        this.value = !(this.value & value); 
    }

    public void nandEq(Bool value){
        this.value = !(this.value & value.value); 
    }

    public Bool or(boolean value){
        return valueOf(this.value | value); 
    }

    public Bool or(Bool value){
        return valueOf(this.value | value.value); 
    }
    
    public void orEq(boolean value){
        this.value |= value;
    }
    
    public void orEq(Bool value){
        this.value |= value.value; 
    }
    
    public Bool xor(boolean value){
        return valueOf(this.value ^ value); 
    }

    public Bool xor(Bool value){
        return valueOf(this.value ^ value.value); 
    }
    
    public void xorEq(boolean value){
        this.value ^= value;
    }
    
    public void xorEq(Bool value){
        this.value ^= value.value; 
    }

    public Bool not(boolean value){
        return valueOf(!value); 
    }
    
    public Bool not(Bool value){
        return valueOf(!value.value); 
    }

    public void notEq(boolean value){
        this.value = !value; 
    }

    public void notEq(Bool value){
        this.value = !value.value; 
    }
    
    public Text toText() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
