/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.semi.hfet;

import static com.tejas.eda.spice.EnumConsts.N_TYPE;
import static com.tejas.eda.spice.EnumConsts.P_TYPE;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.semi.hfet.HFETModelValues;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author owenbad
 */
public abstract class HFETModelCard<I extends Instance, L extends HFETModelValues> extends ModelCard<I, L> {

    public HFETModelCard(String cardString) throws ParserException {
        super(cardString);
    }

    public void setModelType(L model, String type) {
        if (type.equals("NMOS")) {
            model.setType(N_TYPE);
        } else if (type.equals("PMOS")) {
            model.setType(P_TYPE);
        }
    }

    @Override
    public void initModelValues(L model) {
        setModelType(model, type);
        super.initModelValues(model);
    }
}
