/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.semi.mesfet;

import static com.tejas.eda.spice.EnumConsts.N_TYPE;
import static com.tejas.eda.spice.EnumConsts.P_TYPE;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.semi.mesfet.MESModelValues;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author owenbad
 */
public abstract class MESModelCard<I extends Instance, L extends MESModelValues> extends ModelCard<I, L> {

    public MESModelCard(String cardString) throws ParserException {
        super(cardString);
    }

    @Override
    public void initModelValues(L model) {
        setModelType(model, type);
        super.initModelValues(model);
    }

    public void setModelType(MESModelValues model, String type) {
        if (type.equals("NMF")) {
            model.setType(N_TYPE);
        } else if (type.equals("PMF")) {
            model.setType(P_TYPE);
        }
    }
}
