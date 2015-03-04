/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.semi.bjt;

import static com.tejas.eda.spice.EnumConsts.N_TYPE;
import static com.tejas.eda.spice.EnumConsts.P_TYPE;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.semi.bjt.BJTModelValues;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author owenbad
 */
public abstract class BJTModelCard<I extends Instance, L extends BJTModelValues> extends ModelCard<I, L> {

    public BJTModelCard(String cardString) throws ParserException {
        super(cardString);
    }

    public void setModelType(L model) {
        if (type.equals("NPN")) {
            model.setType(N_TYPE);
        } else if (type.equals("PNP")) {
            model.setType(P_TYPE);
        }
    }

    @Override
    public void initModelValues(L model) {
        setModelType(model);
        super.initModelValues(model);
    }
}
