/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math;

import com.tejas.math.numbers.Numeric;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class Constant<N extends Numeric<N>> implements Mathlet {

    N value;

    public Constant(N value) {
        this.value = value;
    }

    public N getValue() {
        return value;
    }

    public void setValue(N value) {
        this.value = value;
    }

    public Text toText() {
        return value.toText();
    }

    @Override
    public String toString() {
        return toText().toString();
    }

}
