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
public class VoltageNode extends SpiceNode {

    static final ObjectFactory<VoltageNode> FACTORY = new ObjectFactory<VoltageNode>() {

        protected VoltageNode create() {
            return new VoltageNode();
        }
    };

    public static VoltageNode valueOf(String id, int index) {
        VoltageNode n = FACTORY.object();
        n.id = id;
        n.index = index;
        n.value = Complex.zero();
        return n;
    }
}
