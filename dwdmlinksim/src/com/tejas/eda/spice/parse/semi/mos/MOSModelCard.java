/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.semi.mos;

import static com.tejas.eda.spice.EnumConsts.N_TYPE;
import static com.tejas.eda.spice.EnumConsts.P_TYPE;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.semi.mos.MOSModelValues;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author owenbad
 */
public abstract class MOSModelCard<I extends Instance, L extends MOSModelValues> extends ModelCard<I, L> {

    public MOSModelCard(String cardString) throws ParserException {
        super(cardString);
    }

    @Override
    public void initModelValues(L model) {
        setModelType(model);
        super.initModelValues(model);
    }

    public void setModelType(L model) {
        if (type.equals("NMOS")) {
            model.setType(N_TYPE);
        } else if (type.equals("PMOS")) {
            model.setType(P_TYPE);
        }
    }
}
