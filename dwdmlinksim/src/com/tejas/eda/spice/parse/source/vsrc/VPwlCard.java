/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source.vsrc;

import com.tejas.eda.spice.device.sources.vsrcs.VSrcPWL;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.source.PwlSrcCard;

/**
 *
 * @author Kristopher T. Beck
 */
public class VPwlCard extends PwlSrcCard {

    public VPwlCard(String cardString) throws ParserException {
        super(cardString);
    }

    @Override
    public VSrcPWL createInstance(Deck deck) {
        VSrcPWL instance = new VSrcPWL();
        initSrcValues(instance, deck);
        initAuxValues(instance);
        return instance;
    }

    protected void initAuxValues(VSrcPWL instance) {
        if (array != null && array.size() != 0) {
            int size = array.size() / 2;
            double[] times = new double[size];
            double[] values = new double[size];
            for (int i = 0; i < size; i++) {
                times[i] = parseDouble(array.get(i * 2));
                values[i] = parseDouble(array.get(i * 2 + 1));
            }
            instance.setTimes(times);
            instance.setValues(values);
        }
    }
}
