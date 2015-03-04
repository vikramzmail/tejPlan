/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class Term<T extends Mathlet> implements Mathlet {

    String name;
    T value;

    public Term(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Text toText() {
        return Text.intern(name + " = ").concat(value.toText());
    }
}
