/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tejas.eda.spice.parse.semi.jfet;

import static com.tejas.eda.spice.EnumConsts.N_TYPE;
import static com.tejas.eda.spice.EnumConsts.P_TYPE;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.semi.jfet.JFETModelValues;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author owenbad
 */
public abstract class JFETModelCard <I extends Instance, L extends JFETModelValues> extends ModelCard<I, L>{

    public JFETModelCard(String cardString) throws ParserException {
        super(cardString);
    }

    public void setModelType(L model) {
        if (name.equals("NJF")) {
            model.setType(N_TYPE);
        } else if (name.equals("PJF")) {
            model.setType(P_TYPE);
        }
    }
}
