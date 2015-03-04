/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.discrete;

import com.tejas.eda.spice.device.discrete.CapInstance;
import com.tejas.eda.spice.device.discrete.CapModelValues;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author owenbad
 */
public class CapModelCard extends ModelCard<CapInstance, CapModelValues> {

    public CapModelCard(String cardString) throws ParserException {
        super(cardString);
    }

    public void setProperty(CapModelValues instance, String name, String value) {
        if (name.equals("CAP")) {
            instance.setCapAC(parseDouble(value));
        } else if (name.equals("CJ")) {
            instance.setCj(parseDouble(value));
        } else if (name.equals("CJSW")) {
            instance.setCjsw(parseDouble(value));
        } else if (name.equals("DEFL")) {
            instance.setDefLength(parseDouble(value));
        } else if (name.equals("DEFW")) {
            instance.setWidth(parseDouble(value));
        } else if (name.equals("TNOM")) {
            instance.setTemp(parseDouble(value));
        } else if (name.equals("NARROW")) {
            instance.setNarrow(parseDouble(value));
        } else if (name.equals("SHORT")) {
            instance.setShorter(parseDouble(value));
        } else if (name.equals("TC1")) {
            instance.setTempCoeff1(parseDouble(value));
        } else if (name.equals("TC2")) {
            instance.setTempCoeff2(parseDouble(value));
        } else if (name.equals("DI")) {
            instance.setDi(parseDouble(value));
        } else if (name.equals("THICK")) {
            instance.setThick(parseDouble(value));
        }
    }

    @Override
    public CapInstance createInstance() {
        CapInstance instance = new CapInstance();
        initModelValues(instance);
        return instance;
    }
}
