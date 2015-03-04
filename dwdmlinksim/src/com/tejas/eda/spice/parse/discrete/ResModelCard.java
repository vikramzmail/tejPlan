/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.discrete;

import com.tejas.eda.spice.device.discrete.ResInstance;
import com.tejas.eda.spice.device.discrete.ResModelValues;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author owenbad
 */
public class ResModelCard extends ModelCard<ResInstance, ResModelValues> {

    public ResModelCard(String cardString) throws ParserException {
        super(cardString);
    }

    public void setProperty(ResModelValues instance, String key, String value) {
        if (key.equals("RSH")) {
            instance.setSheetRes(parseDouble(value));
        } else if (key.equals("NARROW")) {
            instance.setNarrow(parseDouble(value));
        } else if (key.equals("SHORT")) {
            instance.setShorter(parseDouble(value));
        } else if (key.equals("TC1")) {
            instance.setTc1(parseDouble(value));
        } else if (key.equals("TC2")) {
            instance.setTc2(parseDouble(value));
        } else if (key.equals("DEFW")) {
            instance.setWidth(parseDouble(value));
        } else if (key.equals("TNOM")) {
            instance.setTemp(parseDouble(value));
        }
    }

    @Override
    public ResInstance createInstance() {
        ResInstance instance = new ResInstance();
        initModelValues(instance);
        return instance;
    }
}
