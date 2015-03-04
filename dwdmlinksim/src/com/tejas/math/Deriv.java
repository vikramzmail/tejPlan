/*
 * DerivFuncs.java
 *
 * Created on August 23, 2006, 3:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.tejas.math;

import com.tejas.math.numbers.*;

import javolution.context.ObjectFactory;
import javolution.lang.Realtime;
import javolution.lang.ValueType;
import javolution.text.Text;
/**
 *
 * @author Kristopher T. Beck
 */
public class Deriv implements Realtime, ValueType{
    public static final ObjectFactory FACTORY = new ObjectFactory(){
        protected Object create(){
            return new Deriv();
        }
    };
    public double value;
    public double d1_p;
    public double d1_q;
    public double d1_r;
    public double d2_p2;
    public double d2_q2;
    public double d2_r2;
    public double d2_pq;
    public double d2_qr;
    public double d2_pr;
    public double d3_p3;
    public double d3_q3;
    public double d3_r3;
    public double d3_p2q;
    public double d3_p2r;
    public double d3_pq2;
    public double d3_q2r;
    public double d3_pr2;
    public double d3_qr2;
    public double d3_pqr;
    
    public double getValue() {
        return value;
    }
    
    public void setValue(double value) {
        this.value = value;
    }
    
    public double getD1_p() {
        return d1_p;
    }
    
    public void setD1_p(double d1_p) {
        this.d1_p = d1_p;
    }
    
    public double getD1_q() {
        return d1_q;
    }
    
    public void setD1_q(double d1_q) {
        this.d1_q = d1_q;
    }
    
    public double getD1_r() {
        return d1_r;
    }
    
    public void setD1_r(double d1_r) {
        this.d1_r = d1_r;
    }
    
    public double getD2_p2() {
        return d2_p2;
    }
    
    public void setD2_p2(double d2_p2) {
        this.d2_p2 = d2_p2;
    }
    
    public double getD2_q2() {
        return d2_q2;
    }
    
    public void setD2_q2(double d2_q2) {
        this.d2_q2 = d2_q2;
    }
    
    public double getD2_r2() {
        return d2_r2;
    }
    
    public void setD2_r2(double d2_r2) {
        this.d2_r2 = d2_r2;
    }
    
    public double getD2_pq() {
        return d2_pq;
    }
    
    public void setD2_pq(double d2_pq) {
        this.d2_pq = d2_pq;
    }
    
    public double getD2_qr() {
        return d2_qr;
    }
    
    public void setD2_qr(double d2_qr) {
        this.d2_qr = d2_qr;
    }
    
    public double getD2_pr() {
        return d2_pr;
    }
    
    public void setD2_pr(double d2_pr) {
        this.d2_pr = d2_pr;
    }
    
    public double getD3_p3() {
        return d3_p3;
    }
    
    public void setD3_p3(double d3_p3) {
        this.d3_p3 = d3_p3;
    }
    
    public double getD3_q3() {
        return d3_q3;
    }
    
    public void setD3_q3(double d3_q3) {
        this.d3_q3 = d3_q3;
    }
    
    public double getD3_r3() {
        return d3_r3;
    }
    
    public void setD3_r3(double d3_r3) {
        this.d3_r3 = d3_r3;
    }
    
    public double getD3_p2q() {
        return d3_p2q;
    }
    
    public void setD3_p2q(double d3_p2q) {
        this.d3_p2q = d3_p2q;
    }
    
    public double getD3_p2r() {
        return d3_p2r;
    }
    
    public void setD3_p2r(double d3_p2r) {
        this.d3_p2r = d3_p2r;
    }
    
    public double getD3_pq2() {
        return d3_pq2;
    }
    
    public void setD3_pq2(double d3_pq2) {
        this.d3_pq2 = d3_pq2;
    }
    
    public double getD3_q2r() {
        return d3_q2r;
    }
    
    public void setD3_q2r(double d3_q2r) {
        this.d3_q2r = d3_q2r;
    }
    
    public double getD3_pr2() {
        return d3_pr2;
    }
    
    public void setD3_pr2(double d3_pr2) {
        this.d3_pr2 = d3_pr2;
    }
    
    public double getD3_qr2() {
        return d3_qr2;
    }
    
    public void setD3_qr2(double d3_qr2) {
        this.d3_qr2 = d3_qr2;
    }
    
    public double getD3_pqr() {
        return d3_pqr;
    }
    
    public void setD3_pqr(double d3_pqr) {
        this.d3_pqr = d3_pqr;
    }
    
    /** Creates a new instance of DerivFuncs */
    public Deriv() {
    }
    
    public static Deriv valueOf(Deriv deriv){
        Deriv d = (Deriv)FACTORY.object();
        d.set(deriv);
        return d;
    }

    public static Deriv newInstance(){
        Deriv d = (Deriv)FACTORY.object();
        return d;
    }

    public void set(Deriv deriv){
        value = deriv.value;
        d1_p = deriv.d1_p;
        d1_q = deriv.d1_q;
        d1_r = deriv.d1_r;
        d2_p2 = deriv.d2_p2 ;
        d2_q2 = deriv.d2_q2 ;
        d2_r2 = deriv.d2_r2 ;
        d2_pq = deriv.d2_pq ;
        d2_qr = deriv.d2_qr ;
        d2_pr = deriv.d2_pr ;
        d3_p3 = deriv.d3_p3  ;
        d3_q3 = deriv.d3_q3  ;
        d3_r3 = deriv.d3_r3  ;
        d3_p2r = deriv.d3_p2r  ;
        d3_p2q = deriv.d3_p2q  ;
        d3_q2r = deriv.d3_q2r  ;
        d3_pq2 = deriv.d3_pq2  ;
        d3_pr2 = deriv.d3_pr2  ;
        d3_qr2 = deriv.d3_qr2  ;
        d3_pqr = deriv.d3_pqr  ;
    }
    
/*
 * AtanDeriv computes the partial derivatives of the arctangent
 * function where the argument to the atan function is itself a
 * function of three variables p, q, and r.
 */
    
    public void atanEq() {
        Deriv temp = valueOf(this);
        value = Math.atan(temp.value);
        d1_p = temp.d1_p / (1 + temp.value * temp.value);
        d1_q = temp.d1_q / (1 + temp.value * temp.value);
        d1_r = temp.d1_r / (1 + temp.value * temp.value);
        d2_p2 = temp.d2_p2 / (1 + temp.value * temp.value) - 2 *
                temp.value * d1_p * d1_p;
        d2_q2 = temp.d2_q2 / (1 + temp.value * temp.value) - 2 *
                temp.value * d1_q * d1_q;
        d2_r2 = temp.d2_r2 / (1 + temp.value * temp.value) - 2 *
                temp.value * d1_r * d1_r;
        d2_pq = temp.d2_pq / (1 + temp.value * temp.value) - 2 *
                temp.value * d1_p * d1_q;
        d2_qr = temp.d2_qr / (1 + temp.value * temp.value) - 2 *
                temp.value * d1_q * d1_r;
        d2_pr = temp.d2_pr / (1 + temp.value * temp.value) - 2 *
                temp.value * d1_p * d1_r;
        d3_p3 = (temp.d3_p3 - temp.d2_p2 * d1_p * 2 *
                temp.value) / (1 + temp.value * temp.value) - 2 *
                (d1_p * d1_p * temp.d1_p + temp.value *
                ( d2_p2 * d1_p + d2_p2 * d1_p));
        
        d3_q3 = (temp.d3_q3 - temp.d2_q2 * d1_q * 2 *
                temp.value) / (1 + temp.value * temp.value) - 2 *
                (d1_q * d1_q * temp.d1_q + temp.value *
                ( d2_q2 * d1_q + d2_q2 * d1_q));
        d3_r3 = (temp.d3_r3 - temp.d2_r2 * d1_r * 2 *
                temp.value) / (1 + temp.value * temp.value) - 2 *
                (d1_r * d1_r * temp.d1_r + temp.value *
                ( d2_r2 * d1_r + d2_r2 * d1_r));
        d3_p2r = (temp.d3_p2r - temp.d2_p2 * d1_r * 2 *
                temp.value) / (1 + temp.value * temp.value) - 2 *
                (d1_p * d1_p * temp.d1_r + temp.value *
                ( d2_pr * d1_p + d2_pr * d1_p));
        d3_p2q = (temp.d3_p2q - temp.d2_p2 * d1_q * 2 *
                temp.value) / (1 + temp.value * temp.value) - 2 *
                (d1_p * d1_p * temp.d1_q + temp.value *
                ( d2_pq * d1_p + d2_pq * d1_p));
        d3_q2r = (temp.d3_q2r - temp.d2_q2 * d1_r * 2 *
                temp.value) / (1 + temp.value * temp.value) - 2 *
                (d1_q * d1_q * temp.d1_r + temp.value *
                ( d2_qr * d1_q + d2_qr * d1_q));
        d3_pq2 = (temp.d3_pq2 - temp.d2_q2 * d1_p * 2 *
                temp.value) / (1 + temp.value * temp.value) - 2 *
                (d1_q * d1_q * temp.d1_p + temp.value *
                ( d2_pq * d1_q + d2_pq * d1_q));
        d3_pr2 = (temp.d3_pr2 - temp.d2_r2 * d1_p * 2 *
                temp.value) / (1 + temp.value * temp.value) - 2 *
                (d1_r * d1_r * temp.d1_p + temp.value *
                ( d2_pr * d1_r + d2_pr * d1_r));
        d3_qr2 = (temp.d3_qr2 - temp.d2_r2 * d1_q * 2 *
                temp.value) / (1 + temp.value * temp.value) - 2 *
                (d1_r * d1_r * temp.d1_q + temp.value *
                ( d2_qr * d1_r + d2_qr * d1_r));
        d3_pqr = (temp.d3_pqr - temp.d2_pq * d1_r * 2 *
                temp.value) / (1 + temp.value * temp.value) - 2 *
                (d1_p * d1_q * temp.d1_r + temp.value *
                ( d2_pr * d1_q + d2_qr * d1_p));
    }
    
/*
 * CosDeriv computes the partial derivatives of the Math.coMath.sine
 * function where the argument to the function is itself a
 * function of three variables p, q, and r.
 */
    
    public void cosEq() {
        Deriv temp = valueOf(this);
        value = Math.cos(temp.value);
        d1_p = - Math.sin(temp.value)*temp.d1_p;
        d1_q = - Math.sin(temp.value)*temp.d1_q;
        d1_r = - Math.sin(temp.value)*temp.d1_r;
        d2_p2 = -(Math.cos(temp.value)*temp.d1_p*temp.d1_p +
                Math.sin(temp.value)*temp.d2_p2);
        d2_q2 = -(Math.cos(temp.value)*temp.d1_q*temp.d1_q +
                Math.sin(temp.value)*temp.d2_q2);
        d2_r2 = -(Math.cos(temp.value)*temp.d1_r*temp.d1_r +
                Math.sin(temp.value)*temp.d2_r2);
        d2_pq = -(Math.cos(temp.value)*temp.d1_p*temp.d1_q +
                Math.sin(temp.value)*temp.d2_pq);
        d2_qr = -(Math.cos(temp.value)*temp.d1_q*temp.d1_r +
                Math.sin(temp.value)*temp.d2_qr);
        d2_pr = -(Math.cos(temp.value)*temp.d1_p*temp.d1_r +
                Math.sin(temp.value)*temp.d2_pr);
        d3_p3 = -(Math.sin(temp.value)*(temp.d3_p3 - temp.d1_p*temp.d1_p*temp.d1_p)
        + Math.cos(temp.value)*(temp.d1_p*temp.d2_p2 +
                temp.d1_p*temp.d2_p2
                + temp.d1_p*temp.d2_p2));
        d3_q3 = -(Math.sin(temp.value)*(temp.d3_q3 - temp.d1_q*temp.d1_q*temp.d1_q)
        + Math.cos(temp.value)*(temp.d1_q*temp.d2_q2 +
                temp.d1_q*temp.d2_q2
                + temp.d1_q*temp.d2_q2));
        d3_r3 = -(Math.sin(temp.value)*(temp.d3_r3 - temp.d1_r*temp.d1_r*temp.d1_r)
        + Math.cos(temp.value)*(temp.d1_r*temp.d2_r2 +
                temp.d1_r*temp.d2_r2
                + temp.d1_r*temp.d2_r2));
        d3_p2r = -(Math.sin(temp.value)*(temp.d3_p2r -
                temp.d1_r*temp.d1_p*temp.d1_p)
                + Math.cos(temp.value)*(temp.d1_p*temp.d2_pr +
                temp.d1_p*temp.d2_pr
                + temp.d1_r*temp.d2_p2));
        d3_p2q = -(Math.sin(temp.value)*(temp.d3_p2q -
                temp.d1_q*temp.d1_p*temp.d1_p)
                + Math.cos(temp.value)*(temp.d1_p*temp.d2_pq +
                temp.d1_p*temp.d2_pq
                + temp.d1_q*temp.d2_p2));
        d3_q2r = -(Math.sin(temp.value)*(temp.d3_q2r -
                temp.d1_r*temp.d1_q*temp.d1_q)
                + Math.cos(temp.value)*(temp.d1_q*temp.d2_qr +
                temp.d1_q*temp.d2_qr
                + temp.d1_r*temp.d2_q2));
        d3_pq2 = -(Math.sin(temp.value)*(temp.d3_pq2 -
                temp.d1_p*temp.d1_q*temp.d1_q)
                + Math.cos(temp.value)*(temp.d1_q*temp.d2_pq +
                temp.d1_q*temp.d2_pq
                + temp.d1_p*temp.d2_q2));
        d3_pr2 = -(Math.sin(temp.value)*(temp.d3_pr2 -
                temp.d1_p*temp.d1_r*temp.d1_r)
                + Math.cos(temp.value)*(temp.d1_r*temp.d2_pr +
                temp.d1_r*temp.d2_pr
                + temp.d1_p*temp.d2_r2));
        d3_qr2 = -(Math.sin(temp.value)*(temp.d3_qr2 -
                temp.d1_q*temp.d1_r*temp.d1_r)
                + Math.cos(temp.value)*(temp.d1_r*temp.d2_qr +
                temp.d1_r*temp.d2_qr
                + temp.d1_q*temp.d2_r2));
        d3_pqr = -(Math.sin(temp.value)*(temp.d3_pqr -
                temp.d1_r*temp.d1_p*temp.d1_q)
                + Math.cos(temp.value)*(temp.d1_q*temp.d2_pr +
                temp.d1_p*temp.d2_qr
                + temp.d1_r*temp.d2_pq));
    }
    
/*
 * CubeDeriv computes the partial derivatives of the cube
 * function where the argument to the function is itself a
 * function of three variables p, q, and r.
 */
    
    public void cubeEq() {
        Deriv temp = valueOf(this);
        value = temp.value * temp.value * temp.value;
        d1_p = 3*temp.value*temp.value*temp.d1_p;
        d1_q = 3*temp.value*temp.value*temp.d1_q;
        d1_r = 3*temp.value*temp.value*temp.d1_r;
        d2_p2 = 3*(2*temp.value*temp.d1_p*temp.d1_p
                + temp.value*temp.value*temp.d2_p2);
        d2_q2 = 3*(2*temp.value*temp.d1_q*temp.d1_q
                + temp.value*temp.value*temp.d2_q2);
        d2_r2 = 3*(2*temp.value*temp.d1_r*temp.d1_r
                + temp.value*temp.value*temp.d2_r2);
        d2_pq = 3*(2*temp.value*temp.d1_p*temp.d1_q
                + temp.value*temp.value*temp.d2_pq);
        d2_qr = 3*(2*temp.value*temp.d1_q*temp.d1_r
                + temp.value*temp.value*temp.d2_qr);
        d2_pr = 3*(2*temp.value*temp.d1_p*temp.d1_r
                + temp.value*temp.value*temp.d2_pr);
        d3_p3 = 3*(2*(temp.d1_p*temp.d1_p*temp.d1_p
                + temp.value*(temp.d2_p2*temp.d1_p
                + temp.d2_p2*temp.d1_p
                + temp.d2_p2*temp.d1_p))
                + temp.value*temp.value*temp.d3_p3);
        d3_q3 = 3*(2*(temp.d1_q*temp.d1_q*temp.d1_q
                + temp.value*(temp.d2_q2*temp.d1_q
                + temp.d2_q2*temp.d1_q
                + temp.d2_q2*temp.d1_q))
                + temp.value*temp.value*temp.d3_q3);
        d3_r3 = 3*(2*(temp.d1_r*temp.d1_r*temp.d1_r
                + temp.value*(temp.d2_r2*temp.d1_r
                + temp.d2_r2*temp.d1_r
                + temp.d2_r2*temp.d1_r))
                + temp.value*temp.value*temp.d3_r3);
        d3_p2r = 3*(2*(temp.d1_p*temp.d1_p*temp.d1_r
                + temp.value*(temp.d2_p2*temp.d1_r
                + temp.d2_pr*temp.d1_p
                + temp.d2_pr*temp.d1_p))
                + temp.value*temp.value*temp.d3_p2r);
        d3_p2q = 3*(2*(temp.d1_p*temp.d1_p*temp.d1_q
                + temp.value*(temp.d2_p2*temp.d1_q
                + temp.d2_pq*temp.d1_p
                + temp.d2_pq*temp.d1_p))
                + temp.value*temp.value*temp.d3_p2q);
        d3_q2r = 3*(2*(temp.d1_q*temp.d1_q*temp.d1_r
                + temp.value*(temp.d2_q2*temp.d1_r
                + temp.d2_qr*temp.d1_q
                + temp.d2_qr*temp.d1_q))
                + temp.value*temp.value*temp.d3_q2r);
        d3_pq2 = 3*(2*(temp.d1_q*temp.d1_q*temp.d1_p
                + temp.value*(temp.d2_q2*temp.d1_p
                + temp.d2_pq*temp.d1_q
                + temp.d2_pq*temp.d1_q))
                + temp.value*temp.value*temp.d3_pq2);
        d3_pr2 = 3*(2*(temp.d1_r*temp.d1_r*temp.d1_p
                + temp.value*(temp.d2_r2*temp.d1_p
                + temp.d2_pr*temp.d1_r
                + temp.d2_pr*temp.d1_r))
                + temp.value*temp.value*temp.d3_pr2);
        d3_qr2 = 3*(2*(temp.d1_r*temp.d1_r*temp.d1_q
                + temp.value*(temp.d2_r2*temp.d1_q
                + temp.d2_qr*temp.d1_r
                + temp.d2_qr*temp.d1_r))
                + temp.value*temp.value*temp.d3_qr2);
        d3_pqr = 3*(2*(temp.d1_p*temp.d1_q*temp.d1_r
                + temp.value*(temp.d2_pq*temp.d1_r
                + temp.d2_qr*temp.d1_p
                + temp.d2_pr*temp.d1_q))
                + temp.value*temp.value*temp.d3_pqr);
    }
    
/*
 * DivDeriv computes the partial derivatives of the division
 * function where the arguments to the function are
 * functions of three variables p, q, and r.
 */
    
    public void divideEq(Deriv deriv) {
        
        Deriv num = valueOf(this);
        value = num.value / deriv.value;
        d1_p = (num.d1_p - num.value * deriv.d1_p / deriv.value) / deriv.value;
        d1_q = (num.d1_q - num.value * deriv.d1_q / deriv.value) / deriv.value;
        d1_r = (num.d1_r - num.value * deriv.d1_r / deriv.value) / deriv.value;
        d2_p2 = (num.d2_p2 - deriv.d1_p * d1_p
                - value * deriv.d2_p2
                + (deriv.d1_p * (value * deriv.d1_p - num.d1_p)
                / deriv.value)) / deriv.value;
        d2_q2 = (num.d2_q2 - deriv.d1_q * d1_q
                - value * deriv.d2_q2
                + (deriv.d1_q * (value * deriv.d1_q - num.d1_q)
                / deriv.value)) / deriv.value;
        d2_r2 = (num.d2_r2 - deriv.d1_r * d1_r - value *
                deriv.d2_r2 + deriv.d1_r * (value * deriv.d1_r -
                num.d1_r) / deriv.value) /
                deriv.value;
        d2_pq = (num.d2_pq - deriv.d1_q * d1_p - value *
                deriv.d2_pq + deriv.d1_p * (value * deriv.d1_q -
                num.d1_q) / deriv.value) /
                deriv.value;
        d2_qr = (num.d2_qr - deriv.d1_r * d1_q - value *
                deriv.d2_qr + deriv.d1_q * (value * deriv.d1_r -
                num.d1_r) / deriv.value) /
                deriv.value;
        d2_pr = (num.d2_pr - deriv.d1_r * d1_p - value *
                deriv.d2_pr + deriv.d1_p * (value * deriv.d1_r -
                num.d1_r) / deriv.value) /
                deriv.value;
        d3_p3 = (-deriv.d1_p * d2_p2 + num.d3_p3 -deriv.d2_p2 *
                d1_p - deriv.d1_p * d2_p2 - d1_p *
                deriv.d2_p2 - value * deriv.d3_p3
                + (deriv.d1_p * (d1_p * deriv.d1_p +
                value * deriv.d2_p2 -
                num.d2_p2) +
                (value * deriv.d1_p - num.d1_p) *
                (deriv.d2_p2 - deriv.d1_p * deriv.d1_p / deriv.value))
                / deriv.value) / deriv.value;
        d3_q3 = (-deriv.d1_q * d2_q2 + num.d3_q3 -deriv.d2_q2 *
                d1_q - deriv.d1_q * d2_q2 - d1_q *
                deriv.d2_q2 - value * deriv.d3_q3 +
                (deriv.d1_q * (d1_q * deriv.d1_q + value *
                deriv.d2_q2 - num.d2_q2) +
                (value * deriv.d1_q - num.d1_q) *
                (deriv.d2_q2 - deriv.d1_q * deriv.d1_q / deriv.value)) /
                deriv.value) / deriv.value;
        d3_r3 = (-deriv.d1_r * d2_r2 + num.d3_r3 -deriv.d2_r2 *
                d1_r - deriv.d1_r * d2_r2 - d1_r *
                deriv.d2_r2 - value * deriv.d3_r3 +
                (deriv.d1_r * (d1_r * deriv.d1_r + value *
                deriv.d2_r2 - num.d2_r2) +
                (value * deriv.d1_r - num.d1_r) *
                (deriv.d2_r2 - deriv.d1_r * deriv.d1_r / deriv.value)) /
                deriv.value) / deriv.value;
        d3_p2r = (-deriv.d1_r * d2_p2 + num.d3_p2r -deriv.d2_pr *
                d1_p - deriv.d1_p * d2_pr - d1_r *
                deriv.d2_p2 - value * deriv.d3_p2r +
                (deriv.d1_p * (d1_r * deriv.d1_p + value *
                deriv.d2_pr - num.d2_pr) +
                (value * deriv.d1_p - num.d1_p) *
                (deriv.d2_pr - deriv.d1_p * deriv.d1_r / deriv.value))
                / deriv.value) / deriv.value;
        d3_p2q = (-deriv.d1_q * d2_p2 + num.d3_p2q -deriv.d2_pq *
                d1_p - deriv.d1_p * d2_pq - d1_q *
                deriv.d2_p2 - value * deriv.d3_p2q +
                (deriv.d1_p * (d1_q * deriv.d1_p + value *
                deriv.d2_pq - num.d2_pq) +
                (value * deriv.d1_p - num.d1_p) *
                (deriv.d2_pq - deriv.d1_p * deriv.d1_q / deriv.value)) /
                deriv.value) / deriv.value;
        d3_q2r = (-deriv.d1_r * d2_q2 + num.d3_q2r -deriv.d2_qr *
                d1_q - deriv.d1_q * d2_qr - d1_r *
                deriv.d2_q2 - value * deriv.d3_q2r +
                (deriv.d1_q * (d1_r * deriv.d1_q + value *
                deriv.d2_qr - num.d2_qr) +
                (value * deriv.d1_q - num.d1_q) *
                (deriv.d2_qr - deriv.d1_q * deriv.d1_r / deriv.value)) /
                deriv.value) / deriv.value;
        d3_pq2 = (-deriv.d1_p * d2_q2 + num.d3_pq2 -deriv.d2_pq *
                d1_q - deriv.d1_q * d2_pq - d1_p *
                deriv.d2_q2 - value * deriv.d3_pq2 +
                (deriv.d1_q * (d1_p * deriv.d1_q + value *
                deriv.d2_pq - num.d2_pq) +
                (value * deriv.d1_q - num.d1_q) *
                (deriv.d2_pq - deriv.d1_q * deriv.d1_p / deriv.value)) /
                deriv.value) / deriv.value;
        d3_pr2 = (-deriv.d1_p * d2_r2 + num.d3_pr2 -deriv.d2_pr *
                d1_r - deriv.d1_r * d2_pr - d1_p *
                deriv.d2_r2 - value * deriv.d3_pr2 +
                (deriv.d1_r * (d1_p * deriv.d1_r + value *
                deriv.d2_pr - num.d2_pr) +
                (value * deriv.d1_r - num.d1_r) *
                (deriv.d2_pr - deriv.d1_r * deriv.d1_p / deriv.value)) /
                deriv.value) / deriv.value;
        d3_qr2 = (-deriv.d1_q * d2_r2 + num.d3_qr2 -deriv.d2_qr *
                d1_r - deriv.d1_r * d2_qr - d1_q *
                deriv.d2_r2 - value * deriv.d3_qr2 +
                (deriv.d1_r * (d1_q * deriv.d1_r + value *
                deriv.d2_qr - num.d2_qr) +
                (value * deriv.d1_r - num.d1_r) *
                (deriv.d2_qr - deriv.d1_r * deriv.d1_q / deriv.value)) /
                deriv.value) / deriv.value;
        d3_pqr = (-deriv.d1_r * d2_pq + num.d3_pqr -deriv.d2_qr *
                d1_p - deriv.d1_q * d2_pr - d1_r *
                deriv.d2_pq - value * deriv.d3_pqr +
                (deriv.d1_p * (d1_r * deriv.d1_q + value *
                deriv.d2_qr - num.d2_qr) +
                (value * deriv.d1_q - num.d1_q) *
                (deriv.d2_pr - deriv.d1_p * deriv.d1_r / deriv.value)) /
                deriv.value) / deriv.value;
    }
    
/*
 * ExpDeriv computes the partial derivatives of the exponential
 * function where the argument to the function is itself a
 * function of three variables p, q, and r.
 */
    
    public void expEq(Deriv deriv) {
        value = Math.exp(deriv.value);
        d1_p = value*deriv.d1_p;
        d1_q = value*deriv.d1_q;
        d1_r = value*deriv.d1_r;
        d2_p2 = value*deriv.d2_p2 + deriv.d1_p*d1_p;
        d2_q2 = value*deriv.d2_q2 + deriv.d1_q*d1_q;
        d2_r2 = value*deriv.d2_r2 + deriv.d1_r*d1_r;
        d2_pq = value*deriv.d2_pq + deriv.d1_p*d1_q;
        d2_qr = value*deriv.d2_qr + deriv.d1_q*d1_r;
        d2_pr = value*deriv.d2_pr + deriv.d1_p*d1_r;
        d3_p3 = value*deriv.d3_p3 + deriv.d2_p2*d1_p
                + deriv.d2_p2*d1_p
                + d2_p2*deriv.d1_p;
        d3_q3 = value*deriv.d3_q3 + deriv.d2_q2*d1_q
                + deriv.d2_q2*d1_q
                + d2_q2*deriv.d1_q;
        d3_r3 = value*deriv.d3_r3 + deriv.d2_r2*d1_r
                + deriv.d2_r2*d1_r
                + d2_r2*deriv.d1_r;
        d3_p2r = value*deriv.d3_p2r + deriv.d2_p2*d1_r
                + deriv.d2_pr*d1_p
                + d2_pr*deriv.d1_p;
        d3_p2q = value*deriv.d3_p2q + deriv.d2_p2*d1_q
                + deriv.d2_pq*d1_p
                + d2_pq*deriv.d1_p;
        d3_q2r = value*deriv.d3_q2r + deriv.d2_q2*d1_r
                + deriv.d2_qr*d1_q
                + d2_qr*deriv.d1_q;
        d3_pq2 = value*deriv.d3_pq2 + deriv.d2_q2*d1_p
                + deriv.d2_pq*d1_q
                + d2_pq*deriv.d1_q;
        d3_pr2 = value*deriv.d3_pr2 + deriv.d2_r2*d1_p
                + deriv.d2_pr*d1_r
                + d2_pr*deriv.d1_r;
        d3_qr2 = value*deriv.d3_qr2 + deriv.d2_r2*d1_q
                + deriv.d2_qr*d1_r
                + d2_qr*deriv.d1_r;
        d3_pqr = value*deriv.d3_pqr + deriv.d2_pq*d1_r
                + deriv.d2_pr*d1_q
                + d2_qr*deriv.d1_p;
        
    }
    
/*
 * InvDeriv computes the partial derivatives of the 1/x
 * function where the argument to the function is itself a
 * function of three variables p, q, and r.
 */
    
    public void invEq() {
        Deriv temp = valueOf(this);
        value = 1/temp.value;
        d1_p = -value*value*temp.d1_p;
        d1_q = -value*value*temp.d1_q;
        d1_r = -value*value*temp.d1_r;
        d2_p2 = -value*(2*d1_p*temp.d1_p + value*temp.d2_p2);
        d2_q2 = -value*(2*d1_q*temp.d1_q + value*temp.d2_q2);
        d2_r2 = -value*(2*d1_r*temp.d1_r + value*temp.d2_r2);
        d2_pq = -value*(2*d1_q*temp.d1_p + value*temp.d2_pq);
        d2_qr = -value*(2*d1_r*temp.d1_q + value*temp.d2_qr);
        d2_pr = -value*(2*d1_r*temp.d1_p + value*temp.d2_pr);
        d3_p3 = -(2*(temp.d1_p*d1_p*d1_p + value*(
                d2_p2*temp.d1_p + d1_p*temp.d2_p2 +
                d1_p*temp.d2_p2)) + value*value*temp.d3_p3);
        d3_q3 = -(2*(temp.d1_q*d1_q*d1_q + value*(
                d2_q2*temp.d1_q + d1_q*temp.d2_q2 +
                d1_q*temp.d2_q2)) + value*value*temp.d3_q3);
        d3_r3 = -(2*(temp.d1_r*d1_r*d1_r + value*(
                d2_r2*temp.d1_r + d1_r*temp.d2_r2 +
                d1_r*temp.d2_r2)) + value*value*temp.d3_r3);
        d3_p2r = -(2*(temp.d1_p*d1_p*d1_r + value*(
                d2_pr*temp.d1_p + d1_p*temp.d2_pr +
                d1_r*temp.d2_p2)) + value*value*temp.d3_p2r);
        d3_p2q = -(2*(temp.d1_p*d1_p*d1_q + value*(
                d2_pq*temp.d1_p + d1_p*temp.d2_pq +
                d1_q*temp.d2_p2)) + value*value*temp.d3_p2q);
        d3_q2r = -(2*(temp.d1_q*d1_q*d1_r + value*(
                d2_qr*temp.d1_q + d1_q*temp.d2_qr +
                d1_r*temp.d2_q2)) + value*value*temp.d3_q2r);
        d3_pq2 = -(2*(temp.d1_q*d1_q*d1_p + value*(
                d2_pq*temp.d1_q + d1_q*temp.d2_pq +
                d1_p*temp.d2_q2)) + value*value*temp.d3_pq2);
        d3_pr2 = -(2*(temp.d1_r*d1_r*d1_p + value*(
                d2_pr*temp.d1_r + d1_r*temp.d2_pr +
                d1_p*temp.d2_r2)) + value*value*temp.d3_pr2);
        d3_qr2 = -(2*(temp.d1_r*d1_r*d1_q + value*(
                d2_qr*temp.d1_r + d1_r*temp.d2_qr +
                d1_q*temp.d2_r2)) + value*value*temp.d3_qr2);
        d3_pqr = -(2*(temp.d1_p*d1_q*d1_r + value*(
                d2_qr*temp.d1_p + d1_q*temp.d2_pr +
                d1_r*temp.d2_pq)) + value*value*temp.d3_pqr);
        
    }
    
/*
 * MultDeriv computes the partial derivatives of the multiplication
 * function where the arguments to the function are
 * functions of three variables p, q, and r.
 */
    
    public void timesEq(Deriv deriv) {
        Deriv temp = valueOf(this);
        value = temp.value * deriv.value;
        d1_p = temp.d1_p*deriv.value + temp.value*deriv.d1_p;
        d1_q = temp.d1_q*deriv.value + temp.value*deriv.d1_q;
        d1_r = temp.d1_r*deriv.value + temp.value*deriv.d1_r;
        d2_p2 = temp.d2_p2*deriv.value + temp.d1_p*deriv.d1_p
                + temp.d1_p*deriv.d1_p + temp.value*deriv.d2_p2;
        d2_q2 = temp.d2_q2*deriv.value + temp.d1_q*deriv.d1_q
                + temp.d1_q*deriv.d1_q + temp.value*deriv.d2_q2;
        d2_r2 = temp.d2_r2*
                deriv.value + temp.d1_r*deriv.d1_r
                + temp.d1_r*deriv.d1_r + temp.value*deriv.d2_r2;
        d2_pq = temp.d2_pq*deriv.value + temp.d1_p*deriv.d1_q
                + temp.d1_q*deriv.d1_p + temp.value*deriv.d2_pq;
        d2_qr = temp.d2_qr*deriv.value + temp.d1_q*deriv.d1_r
                + temp.d1_r*deriv.d1_q + temp.value*deriv.d2_qr;
        d2_pr = temp.d2_pr*deriv.value + temp.d1_p*deriv.d1_r
                + temp.d1_r*deriv.d1_p + temp.value*deriv.d2_pr;
        d3_p3 = temp.d3_p3*deriv.value + temp.d2_p2*deriv.d1_p
                + temp.d2_p2*deriv.d1_p + deriv.d2_p2*temp.d1_p
                + deriv.d2_p2*temp.d1_p + temp.d2_p2*deriv.d1_p
                + deriv.d2_p2*temp.d1_p + temp.value*deriv.d3_p3;
        
        d3_q3 = temp.d3_q3*deriv.value + temp.d2_q2*deriv.d1_q
                + temp.d2_q2*deriv.d1_q + deriv.d2_q2*temp.d1_q
                + deriv.d2_q2*temp.d1_q + temp.d2_q2*deriv.d1_q
                + deriv.d2_q2*temp.d1_q + temp.value*deriv.d3_q3;
        d3_r3 = temp.d3_r3*deriv.value + temp.d2_r2*deriv.d1_r
                + temp.d2_r2*deriv.d1_r + deriv.d2_r2*temp.d1_r
                + deriv.d2_r2*temp.d1_r + temp.d2_r2*deriv.d1_r
                + deriv.d2_r2*temp.d1_r + temp.value*deriv.d3_r3;
        d3_p2r = temp.d3_p2r*deriv.value + temp.d2_p2*deriv.d1_r
                + temp.d2_pr*deriv.d1_p + deriv.d2_p2*temp.d1_r
                + deriv.d2_pr*temp.d1_p + temp.d2_pr*deriv.d1_p
                + deriv.d2_pr*temp.d1_p + temp.value*deriv.d3_p2r;
        d3_p2q = temp.d3_p2q*deriv.value + temp.d2_p2*deriv.d1_q
                + temp.d2_pq*deriv.d1_p + deriv.d2_p2*temp.d1_q
                + deriv.d2_pq*temp.d1_p + temp.d2_pq*deriv.d1_p
                + deriv.d2_pq*temp.d1_p + temp.value*deriv.d3_p2q;
        d3_q2r = temp.d3_q2r*deriv.value + temp.d2_q2*deriv.d1_r
                + temp.d2_qr*deriv.d1_q + deriv.d2_q2*temp.d1_r
                + deriv.d2_qr*temp.d1_q + temp.d2_qr*deriv.d1_q
                + deriv.d2_qr*temp.d1_q + temp.value*deriv.d3_q2r;
        d3_pq2 = temp.d3_pq2*deriv.value + temp.d2_q2*deriv.d1_p
                + temp.d2_pq*deriv.d1_q + deriv.d2_q2*temp.d1_p
                + deriv.d2_pq*temp.d1_q + temp.d2_pq*deriv.d1_q
                + deriv.d2_pq*temp.d1_q + temp.value*deriv.d3_pq2;
        d3_pr2 = temp.d3_pr2*deriv.value + temp.d2_r2*deriv.d1_p
                + temp.d2_pr*deriv.d1_r + deriv.d2_r2*temp.d1_p
                + deriv.d2_pr*temp.d1_r + temp.d2_pr*deriv.d1_r
                + deriv.d2_pr*temp.d1_r + temp.value*deriv.d3_pr2;
        d3_qr2 = temp.d3_qr2*deriv.value + temp.d2_r2*deriv.d1_q
                + temp.d2_qr*deriv.d1_r + deriv.d2_r2*temp.d1_q
                + deriv.d2_qr*temp.d1_r + temp.d2_qr*deriv.d1_r
                + deriv.d2_qr*temp.d1_r + temp.value*deriv.d3_qr2;
        d3_pqr = temp.d3_pqr*deriv.value + temp.d2_pq*deriv.d1_r
                + temp.d2_pr*deriv.d1_q + deriv.d2_pq*temp.d1_r
                + deriv.d2_qr*temp.d1_p + temp.d2_qr*deriv.d1_p
                + deriv.d2_pr*temp.d1_q + temp.value*deriv.d3_pqr;
    }
    
/*
 * PlusDeriv computes the partial derivatives of the addition
 * function where the arguments to the function are
 * functions of three variables p, q, and r.
 */
    
    public void plusEq(Deriv deriv) {
        value += deriv.value;
        d1_p = d1_p  + deriv.d1_p;
        d1_q = d1_q  + deriv.d1_q;
        d1_r = d1_r  + deriv.d1_r;
        d2_p2 = d2_p2  + deriv.d2_p2;
        d2_q2 = d2_q2  + deriv.d2_q2;
        d2_r2 = d2_r2  + deriv.d2_r2;
        d2_pq = d2_pq  + deriv.d2_pq;
        d2_qr = d2_qr  + deriv.d2_qr;
        d2_pr = d2_pr  + deriv.d2_pr;
        d3_p3 = d3_p3 + deriv.d3_p3;
        d3_q3 = d3_q3 + deriv.d3_q3;
        d3_r3 = d3_r3 + deriv.d3_r3;
        d3_p2r = d3_p2r + deriv.d3_p2r;
        d3_p2q = d3_p2q + deriv.d3_p2q;
        d3_q2r = d3_q2r + deriv.d3_q2r;
        d3_pq2 = d3_pq2 + deriv.d3_pq2;
        d3_pr2 = d3_pr2 + deriv.d3_pr2;
        d3_qr2 = d3_qr2 + deriv.d3_qr2;
        d3_pqr = d3_pqr + deriv.d3_pqr;
    }
    
/*
 * PowDeriv computes the partial derivatives of the x^^m
 * function where the argument to the function is itself a
 * function of three variables p, q, and r. m is a constant.
 */
    
    public void powEq(double emm) {
        Deriv temp = valueOf(this);
        
        value = Math.pow(temp.value, emm);
        d1_p = emm * value / temp.value * temp.d1_p;
        d1_q = emm * value / temp.value * temp.d1_q;
        d1_r = emm * value / temp.value * temp.d1_r;
        d2_p2 = emm * value / temp.value *
                ((emm-1) / temp.value * temp.d1_p * temp.d1_p + temp.d2_p2);
        d2_q2 = emm * value / temp.value *
                ((emm-1) / temp.value * temp.d1_q * temp.d1_q + temp.d2_q2);
        d2_r2 = emm * value / temp.value *
                ((emm-1) / temp.value * temp.d1_r * temp.d1_r + temp.d2_r2);
        d2_pq = emm * value / temp.value *
                ((emm-1) / temp.value * temp.d1_p * temp.d1_q + temp.d2_pq);
        d2_qr = emm * value / temp.value *
                ((emm-1) / temp.value * temp.d1_q * temp.d1_r + temp.d2_qr);
        d2_pr = emm * value / temp.value *
                ((emm-1) / temp.value * temp.d1_p * temp.d1_r + temp.d2_pr);
        d3_p3 = emm * (emm-1) * value / (temp.value * temp.value) *
                ((emm-2) / temp.value * temp.d1_p *
                temp.d1_p * temp.d1_p + temp.d1_p * temp.d2_p2 + temp.d1_p *
                temp.d2_p2 + temp.d1_p * temp.d2_p2) + emm * value /
                temp.value * temp.d3_p3;
        d3_q3 = emm * (emm-1) * value / (temp.value * temp.value) *
                ((emm-2) / temp.value * temp.d1_q *
                temp.d1_q * temp.d1_q + temp.d1_q * temp.d2_q2 + temp.d1_q *
                temp.d2_q2 + temp.d1_q * temp.d2_q2) + emm * value /
                temp.value * temp.d3_q3;
        d3_r3 = emm * (emm-1) * value / (temp.value * temp.value) *
                ((emm-2) / temp.value * temp.d1_r *temp.d1_r * temp.d1_r +
                temp.d1_r * temp.d2_r2 + temp.d1_r * temp.d2_r2 + temp.d1_r *
                temp.d2_r2) + emm * value / temp.value * temp.d3_r3;
        d3_p2r = emm * (emm-1) * value / (temp.value * temp.value) *
                ((emm-2) / temp.value * temp.d1_p * temp.d1_p * temp.d1_r +
                temp.d1_p * temp.d2_pr + temp.d1_p * temp.d2_pr + temp.d1_r *
                temp.d2_p2) + emm * value / temp.value * temp.d3_p2r;
        d3_p2q = emm * (emm-1) * value / (temp.value * temp.value) *
                ((emm-2) / temp.value * temp.d1_p * temp.d1_p * temp.d1_q +
                temp.d1_p * temp.d2_pq + temp.d1_p * temp.d2_pq + temp.d1_q *
                temp.d2_p2) + emm * value / temp.value * temp.d3_p2q;
        d3_q2r = emm * (emm-1) * value / (temp.value * temp.value) *
                ((emm-2) / temp.value * temp.d1_q * temp.d1_q * temp.d1_r +
                temp.d1_q * temp.d2_qr + temp.d1_q * temp.d2_qr + temp.d1_r *
                temp.d2_q2) + emm * value / temp.value * temp.d3_q2r;
        d3_pq2 = emm * (emm-1) * value / (temp.value * temp.value) *
                ((emm-2) / temp.value * temp.d1_q * temp.d1_q * temp.d1_p +
                temp.d1_q * temp.d2_pq + temp.d1_q * temp.d2_pq + temp.d1_p *
                temp.d2_q2) + emm * value / temp.value * temp.d3_pq2;
        d3_pr2 = emm * (emm-1) * value / (temp.value * temp.value) *
                ((emm-2) / temp.value * temp.d1_r * temp.d1_r * temp.d1_p +
                temp.d1_r * temp.d2_pr + temp.d1_r * temp.d2_pr + temp.d1_p *
                temp.d2_r2) + emm * value / temp.value * temp.d3_pr2;
        d3_qr2 = emm * (emm-1) * value / (temp.value * temp.value) *
                ((emm-2) / temp.value * temp.d1_r * temp.d1_r * temp.d1_q +
                temp.d1_r * temp.d2_qr + temp.d1_r * temp.d2_qr + temp.d1_q *
                temp.d2_r2) + emm * value / temp.value * temp.d3_qr2;
        d3_pqr = emm * (emm-1) * value / (temp.value * temp.value) *
                ((emm-2) / temp.value * temp.d1_p * temp.d1_q * temp.d1_r +
                temp.d1_p * temp.d2_qr + temp.d1_q * temp.d2_pr + temp.d1_r *
                temp.d2_pq) + emm * value / temp.value * temp.d3_pqr;
    }
    
/*
 * SqrtDeriv computes the partial derivatives of the sqrt
 * function where the argument to the function is itself a
 * function of three variables p, q, and r.
 */
    
    public void sqrtEq() {
        Deriv temp = valueOf(this);
        value = Math.sqrt(temp.value);
        if (temp.value == 0.0) {
            d1_p = 0.0;
            d1_q = 0.0;
            d1_r = 0.0;
            d2_p2 = 0.0;
            d2_q2 = 0.0;
            d2_r2 = 0.0;
            d2_pq = 0.0;
            d2_qr = 0.0;
            d2_pr = 0.0;
            d3_p3 = 0.0;
            d3_q3 = 0.0;
            d3_r3 = 0.0;
            d3_p2r = 0.0;
            d3_p2q = 0.0;
            d3_q2r = 0.0;
            d3_pq2 = 0.0;
            d3_pr2 = 0.0;
            d3_qr2 = 0.0;
            d3_pqr = 0.0;
        } else {
            d1_p = 0.5*temp.d1_p/value;
            d1_q = 0.5*temp.d1_q/value;
            d1_r = 0.5*temp.d1_r/value;
            d2_p2 = 0.5/value * (temp.d2_p2 - 0.5 * temp.d1_p *
                    temp.d1_p/ temp.value);
            d2_q2 = 0.5/value*(temp.d2_q2 -0.5 * temp.d1_q *
                    temp.d1_q/ temp.value);
            d2_r2 = 0.5/value*(temp.d2_r2 -0.5 * temp.d1_r *
                    temp.d1_r/ temp.value);
            d2_pq = 0.5/value*(temp.d2_pq -0.5 * temp.d1_p *
                    temp.d1_q/ temp.value);
            d2_qr = 0.5/value*(temp.d2_qr -0.5 * temp.d1_q *
                    temp.d1_r/ temp.value);
            d2_pr = 0.5/value*(temp.d2_pr -0.5 * temp.d1_p *
                    temp.d1_r/ temp.value);
            d3_p3 = 0.5 *
                    (temp.d3_p3 / value - 0.5 / (temp.value*value) *
                    (-1.5 / temp.value * temp.d1_p * temp.d1_p * temp.d1_p
                    + temp.d1_p*temp.d2_p2
                    + temp.d1_p*temp.d2_p2
                    + temp.d1_p*temp.d2_p2));
            d3_q3 = 0.5 *
                    (temp.d3_q3 / value - 0.5 / (temp.value*value) *
                    (-1.5 / temp.value * temp.d1_q * temp.d1_q * temp.d1_q
                    + temp.d1_q*temp.d2_q2
                    + temp.d1_q*temp.d2_q2
                    + temp.d1_q* temp.d2_q2));
            d3_r3 = 0.5 *
                    (temp.d3_r3 / value - 0.5 / (temp.value*value) *
                    (-1.5 / temp.value * temp.d1_r * temp.d1_r * temp.d1_r
                    + temp.d1_r*temp.d2_r2
                    + temp.d1_r*temp.d2_r2
                    + temp.d1_r* temp.d2_r2));
            d3_p2r = 0.5 *
                    (temp.d3_p2r / value - 0.5 / (temp.value*value) *
                    (-1.5 / temp.value * temp.d1_p * temp.d1_p * temp.d1_r
                    + temp.d1_p * temp.d2_pr
                    + temp.d1_p * temp.d2_pr
                    + temp.d1_r * temp.d2_p2));
            d3_p2q = 0.5 *
                    (temp.d3_p2q / value - 0.5 / (temp.value * value) *
                    (-1.5/temp.value*temp.d1_p*temp.d1_p*temp.d1_q
                    + temp.d1_p * temp.d2_pq
                    + temp.d1_p * temp.d2_pq
                    + temp.d1_q * temp.d2_p2));
            d3_q2r = 0.5 *
                    (temp.d3_q2r / value - 0.5 / (temp.value * value) *
                    (-1.5 / temp.value * temp.d1_q * temp.d1_q * temp.d1_r
                    + temp.d1_q*temp.d2_qr
                    + temp.d1_q*temp.d2_qr
                    + temp.d1_r* temp.d2_q2));
            d3_pq2 = 0.5 *
                    (temp.d3_pq2 / value - 0.5 / (temp.value * value) *
                    (-1.5 / temp.value * temp.d1_q * temp.d1_q * temp.d1_p
                    + temp.d1_q*temp.d2_pq
                    + temp.d1_q*temp.d2_pq
                    + temp.d1_p* temp.d2_q2));
            d3_pr2 = 0.5 *
                    (temp.d3_pr2 / value - 0.5 / (temp.value * value) *
                    (-1.5/temp.value * temp.d1_r * temp.d1_r * temp.d1_p
                    + temp.d1_r*temp.d2_pr
                    + temp.d1_r*temp.d2_pr
                    + temp.d1_p* temp.d2_r2));
            d3_qr2 = 0.5 *
                    (temp.d3_qr2 / value - 0.5 / (temp.value * value) *
                    (-1.5/temp.value * temp.d1_r * temp.d1_r * temp.d1_q
                    + temp.d1_r*temp.d2_qr
                    + temp.d1_r*temp.d2_qr
                    + temp.d1_q* temp.d2_r2));
            d3_pqr = 0.5 *
                    (temp.d3_pqr / value - 0.5 / (temp.value * value) *
                    (-1.5/temp.value * temp.d1_p * temp.d1_q * temp.d1_r
                    + temp.d1_p*temp.d2_qr
                    + temp.d1_q*temp.d2_pr
                    + temp.d1_r* temp.d2_pq));
        }
    }
    
/*
 * TanDeriv computes the partial derivatives of the tangent
 * function where the argument to the function is itself a
 * function of three variables p, q, and r.
 */
    
   public void tanEq() {
        Deriv temp = valueOf(this);
        value = Math.tan(temp.value);
        
        d1_p = (1 + value*value)*temp.d1_p;
        d1_q = (1 + value*value)*temp.d1_q;
        d1_r = (1 + value*value)*temp.d1_r;
        d2_p2 = (1 + value*value)*temp.d2_p2 + 2*value*temp.d1_p*d1_p;
        d2_q2 = (1 + value*value)*temp.d2_q2 + 2*value*temp.d1_q*d1_q;
        d2_r2 = (1 + value*value)*temp.d2_r2 + 2*value*temp.d1_r*d1_r;
        d2_pq = (1 + value*value)*temp.d2_pq + 2*value*temp.d1_p*d1_q;
        d2_qr = (1 + value*value)*temp.d2_qr + 2*value*temp.d1_q*d1_r;
        d2_pr = (1 + value*value)*temp.d2_pr + 2*value*temp.d1_p*d1_r;
        d3_p3 = (1 + value*value)*temp.d3_p3 +2*( value*(
                temp.d2_p2*d1_p + temp.d2_p2*d1_p + d2_p2*
                temp.d1_p) + temp.d1_p*d1_p*d1_p);
        d3_q3 = (1 + value*value)*temp.d3_q3 +2*( value*(
                temp.d2_q2*d1_q + temp.d2_q2*d1_q + d2_q2*
                temp.d1_q) + temp.d1_q*d1_q*d1_q);
        d3_r3 = (1 + value*value)*temp.d3_r3 +2*( value*(
                temp.d2_r2*d1_r + temp.d2_r2*d1_r + d2_r2*
                temp.d1_r) + temp.d1_r*d1_r*d1_r);
        d3_p2r = (1 + value*value)*temp.d3_p2r +2*( value*(
                temp.d2_p2*d1_r + temp.d2_pr*d1_p + d2_pr*
                temp.d1_p) + temp.d1_p*d1_p*d1_r);
        d3_p2q = (1 + value*value)*temp.d3_p2q +2*( value*(
                temp.d2_p2*d1_q + temp.d2_pq*d1_p + d2_pq*
                temp.d1_p) + temp.d1_p*d1_p*d1_q);
        d3_q2r = (1 + value*value)*temp.d3_q2r +2*( value*(
                temp.d2_q2*d1_r + temp.d2_qr*d1_q + d2_qr*
                temp.d1_q) + temp.d1_q*d1_q*d1_r);
        d3_pq2 = (1 + value*value)*temp.d3_pq2 +2*( value*(
                temp.d2_q2*d1_p + temp.d2_pq*d1_q + d2_pq*
                temp.d1_q) + temp.d1_q*d1_q*d1_p);
        d3_pr2 = (1 + value*value)*temp.d3_pr2 +2*( value*(
                temp.d2_r2*d1_p + temp.d2_pr*d1_r + d2_pr*
                temp.d1_r) + temp.d1_r*d1_r*d1_p);
        d3_qr2 = (1 + value*value)*temp.d3_qr2 +2*( value*(
                temp.d2_r2*d1_q + temp.d2_qr*d1_r + d2_qr*
                temp.d1_r) + temp.d1_r*d1_r*d1_q);
        d3_pqr = (1 + value*value)*temp.d3_pqr +2*( value*(
                temp.d2_pq*d1_r + temp.d2_pr*d1_q + d2_qr*
                temp.d1_p) + temp.d1_p*d1_q*d1_r);
    }
    
/*
 * TimesDeriv computes the partial derivatives of the x*k
 * function where the argument to the function is itself a
 * function of three variables p, q, and r. k is a constant.
 */
    
    public void timesEq(double k) {
        value = k* value;
        d1_p = k * d1_p;
        d1_q = k * d1_q;
        d1_r = k * d1_r;
        d2_p2 = k * d2_p2;
        d2_q2 = k * d2_q2;
        d2_r2 = k * d2_r2;
        d2_pq = k * d2_pq;
        d2_qr = k * d2_qr;
        d2_pr = k * d2_pr;
        d3_p3 = k * d3_p3;
        d3_q3 = k * d3_q3;
        d3_r3 = k * d3_r3;
        d3_p2r = k * d3_p2r;
        d3_p2q = k * d3_p2q;
        d3_q2r = k * d3_q2r;
        d3_pq2 = k * d3_pq2;
        d3_pr2 = k * d3_pr2;
        d3_qr2 = k * d3_qr2;
        d3_pqr = k * d3_pqr;
    }

    public Deriv copy() {
        Deriv d = (Deriv)FACTORY.object();
        d.set(this);
        return d;
    }

    public Text toText() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
