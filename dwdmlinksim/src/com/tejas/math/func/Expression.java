/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math.func;

import com.tejas.math.Mathlet;
import com.tejas.math.oper.NaryOp;

import javolution.text.Text;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 *
 * @author Kristopher T. Beck
 */
public class Expression implements NaryOp {

    boolean enclosed;
    FastMap<String, Mathlet> varables = FastMap.newInstance();
    FastList<Mathlet> elements = FastList.newInstance();

    public FastList<Mathlet> getElements() {
        return elements;
    }

    public boolean isEnclosed() {
        return enclosed;
    }

    public void setEnclosed(boolean enclosed) {
        this.enclosed = enclosed;
    }

    public FastMap<String, Mathlet> getVarables() {
        return varables;
    }

    public void addElement(Mathlet mlet) {
        elements.add(mlet);
    }

    public void addVarable(String name, Mathlet mlet) {
        varables.put(name, mlet);
    }

    public Mathlet eval() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Text toText() {
        Text text = Text.EMPTY;
        if (enclosed) {
            text.concat(Text.valueOf('('));
        }
        for (Mathlet m : elements) {
            text.concat(m.toText());
        }
        if (enclosed) {
            text.concat(Text.valueOf(')'));
        }
        return text;
    }
}
