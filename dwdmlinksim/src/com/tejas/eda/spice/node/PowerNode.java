/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.node;

import com.tejas.math.numbers.Complex;

import javolution.context.ObjectFactory;

/**
 *
 * @author Kristopher T. Beck
 */
public class PowerNode extends SpiceNode {

    static final ObjectFactory<PowerNode> FACTORY = new ObjectFactory<PowerNode>() {

        protected PowerNode create() {
            return new PowerNode();
        }
    };

    public static PowerNode valueOf(String id, int index) {
        PowerNode n = FACTORY.object();
        n.id = id;
        n.index = index;
        n.value = Complex.zero();
        return n;
    }
}
