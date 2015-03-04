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
public class CurrentNode extends SpiceNode {

    static final ObjectFactory<CurrentNode> FACTORY = new ObjectFactory<CurrentNode>() {

        protected CurrentNode create() {
            return new CurrentNode();
        }
    };

    public static CurrentNode valueOf(String id, int index) {
        CurrentNode n = FACTORY.object();
        n.id = id;
        n.index = index;
        n.value = Complex.zero();
        return n;
    }
}
