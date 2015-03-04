
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.discrete;

import com.tejas.eda.spice.device.txline.URCInstance;
import com.tejas.eda.spice.device.txline.URCModelValues;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author owenbad
 */
public class URCModelCard extends ModelCard<URCInstance, URCModelValues> {

    public URCModelCard(String cardString) throws ParserException {
        super(cardString);
    }

    @Override
    public URCInstance createInstance() {
        URCInstance instance = new URCInstance();
        initModelValues(instance);
        return instance;
    }

    @Override
    public void setProperty(URCModelValues model, String name, String value) {
        if (name.equals("K")) {
            model.setK(parseDouble(value));
        } else if (name.equals("FMAX")) {
            model.setFmax(parseDouble(value));
        } else if (name.equals("RPERL")) {
            model.setrPerL(parseDouble(value));
        } else if (name.equals("CPERL")) {
            model.setcPerL(parseDouble(value));
        } else if (name.equals("ISPERL")) {
            model.setiSatPerL(parseDouble(value));
        } else if (name.equals("RSPERL")) {
            model.setRsPerL(parseDouble(value));
        }
    }
}
