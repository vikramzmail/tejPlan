/*
 * AbstractVector.java
 *
 * Created on April 14, 2006, 9:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.math.vector;

import com.tejas.math.numbers.Numeric;

import javolution.context.ObjectFactory;
import javolution.text.Text;
import javolution.util.FastTable;
/**
 *
 * @param N 
 * @author Kristopher T. Beck
 */
public class Vector<N extends Numeric> extends FastTable<N>{
    public static final ObjectFactory<Vector> FACTORY = new ObjectFactory<Vector>(){
        protected Vector create(){
            return new Vector();
        }
    };
    
//    FastTable<N> array = FastTable.newInstance();
    N fill;
    /**
     * Creates a new instance of AbstractVector
     */
    protected Vector() {
    }
    
    @Override
    public void setSize(int size){
        int s = size();
        while(s < size){
            add((N)fill.copy());
            s++;
        }
        while(s > size){
            removeLast();
        }
    }
    
    public Vector<N> plus(double num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).plusEq(num);
        }
        return v;
    }
    
    public Vector<N> plus(N num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).plusEq(num);
        }
        return v;
    }
    
    public Vector<N> plus(Vector<N> num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).plusEq(num.get(i));
        }
        return v;
    }
    
    public Vector<N> minus(double num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).minusEq(num);
        }
        return v;
    }
    
    public Vector<N> minus(N num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).minusEq(num);
        }
        return v;
    }
    
    public Vector<N> minus(Vector<N> num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).minusEq(num.get(i));
        }
        return v;
    }
    
    public Vector<N> times(double num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).timesEq(num);
        }
        return v;
    }
    
    public Vector<N> times(N num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).timesEq(num);
        }
        return v;
    }
    
    public Vector<N> times(Vector<N> num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).timesEq(num.get(i));
        }
        return v;
    }
    
    public Vector<N> divide(double num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).divideEq(num);
        }
        return v;
    }
    
    public Vector<N> divide(N num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).divideEq(num);
        }
        return v;
    }
    
    public Vector<N> divide(Vector<N> num){
        Vector<N> v = copy();
        for(int i = 0; i < size(); i++){
            v.get(i).plusEq(num.get(i));
        }
        return v;
    }
    
    public void plusEq(double num) {
        for(int i = 0; i < size(); i++)
            get(i).plusEq(num);
    }
    
    public void plusEq(N num) {
        for(int i = 0; i < size(); i++)
            get(i).plusEq(num);
    }
    
    public void plusEq(Vector<N> num) {
        for(int i = 0; i < size(); i++)
            get(i).plusEq(num.get(i));
        
    }
    
    public void minusEq(double num) {
        for(int i = 0; i < size(); i++)
            get(i).minusEq(num);
    }
    
    public void minusEq(N num) {
        for(int i = 0; i < size(); i++)
            get(i).minusEq(num);
    }
    
    public void minusEq(Vector<N> num) {
        for(int i = 0; i < size(); i++)
            get(i).minusEq(num.get(i));
    }
    
    public void timesEq(double num) {
        for(int i = 0; i < size(); i++)
            get(i).timesEq(num);
    }
    
    public void timesEq(N num) {
        for(int i = 0; i < size(); i++)
            get(i).timesEq(num);
    }
    
    public void timesEq(Vector<N> num) {
        for(int i = 0; i < size(); i++)
            get(i).timesEq(num.get(i));
    }
    
    public void divideEq(double num) {
        for(int i = 0; i < size(); i++)
            get(i).divideEq(num);
    }
    
    public void divideEq(N num) {
        for(int i = 0; i < size(); i++)
            get(i).divideEq(num);
    }
    
    public void divideEq(Vector<N> num) {
        for(int i = 0; i < size(); i++)
            get(i).divideEq(num.get(i));
    }
    
    public Vector<N> copy(){
        Vector<N> v = (Vector<N>)FACTORY.object();
        for(int i = 0; i < size(); i++)
            v.add(get(i));
        return v;
    }
    
    public void assign(N n) {
        for(int i = 0; i < size(); i++)
            get(i).set(n);
    }
    
    public void assign(Vector<N> v) {
        for(int i = 0; i < size(); i++)
            get(i).set(v.get(i));
    }
    
    public void inverseEq() {
        for(int i = 0; i < size(); i++)
            get(i).inverseEq();
    }
    
    public boolean isOne() {
        for(int i = 0; i < size(); i++){
            if(!get(i).isOne())
                return false;
        }
        return true;
    }
    
    public boolean isZero() {
        for(int i = 0; i < size(); i++){
            if(!get(i).isZero())
                return false;
        }
        return true;
    }
    public void negateEq() {
        for(int i = 0; i < size(); i++)
            get(i).negateEq();
    }
/*
    public void powEq(double exp) {
        for(int i = 0; i < size(); i++)
            get(i).powEq(exp);
    }

    public void powEq(N exp) {
        for(int i = 0; i < size(); i++)
            get(i).powEq(exp);
    }
    
    public void powEq(Vector<N> exp) {
        for(int i = 0; i < size(); i++)
            get(i).powEq(exp.get(i));
    }
    
    public void sqrtEq() {
        for(int i = 0; i < size(); i++)
            get(i).sqrtEq();
    }
   */
    public void setZero() {
        for(int i = 0; i < size(); i++)
            get(i).setZero();
    }
    
    public void setOne() {
        for(int i = 0; i < size(); i++)
            get(i).setOne();
    }
    
    public static <N extends Numeric> Vector<N> newInstance(int size, N fill){
        Vector<N> v = (Vector<N>) FACTORY.object();
        v.fill = (N)fill.copy();
        v.setSize(size);
        return v;
    }

    @Override
    public Text toText() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
