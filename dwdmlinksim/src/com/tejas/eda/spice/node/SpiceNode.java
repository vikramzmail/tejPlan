/*
 * DefaultNode.java
 *
 * Created on June 9, 2006, 5:11 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.tejas.eda.spice.node;

import com.tejas.eda.node.Node;
import com.tejas.math.numbers.Complex;
/**
 *
 * @author Kristopher T. Beck
 */

public abstract class SpiceNode extends Node<Complex> {

    protected Complex ic;
    protected double ns;
    protected boolean icGiven = false;
    protected boolean nsGiven = false;

    /** Creates a new instance of DefaultNode */
    public SpiceNode() {
    }

    public Complex getIC() {
        return ic;
    }

    public double getNS() {
        return ns;
    }

    public void setNSGiven(boolean nsGiven) {
        this.nsGiven = nsGiven;
    }

    public void setICGiven(boolean icGiven) {
        this.icGiven = icGiven;
    }

    public boolean isICGiven() {
        return icGiven;
    }

    public boolean isNSGiven() {
        return nsGiven;
    }

    public void setIC(Complex ic) {
        this.ic = ic;
    }

    public void setNS(double ns) {
        this.ns = ns;
    }
}
