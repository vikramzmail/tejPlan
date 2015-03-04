/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.discrete;

import com.tejas.eda.spice.device.discrete.ISwitchInstance;
import com.tejas.eda.spice.device.discrete.ISwitchModelValues;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author owenbad
 */
public class CSWModelCard extends ModelCard<ISwitchInstance, ISwitchModelValues> {

    public CSWModelCard(String cardString) throws ParserException {
        super(cardString);
    }

    @Override
    public void setProperty(ISwitchModelValues instance, String name, String value) {
        if (name.equals("IT")) {
            instance.setIThreshold(parseDouble(value));
        } else if (name.equals("IH")) {
            instance.setIHysteresis(parseDouble(value));
        } else if (name.equals("RON")) {
            instance.setOnResist(parseDouble(value));
        } else if (name.equals("ROFF")) {
            instance.setOffResist(parseDouble(value));
        }
    }

    @Override
    public ISwitchInstance createInstance() {
        ISwitchInstance instance = new ISwitchInstance();
        initModelValues(instance);
        return instance;
    }
}
