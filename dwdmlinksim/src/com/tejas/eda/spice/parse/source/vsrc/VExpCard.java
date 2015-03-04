/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source.vsrc;

import com.tejas.eda.spice.device.sources.vsrcs.VSrcExp;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.source.ExpSrcCard;

/**
 *
 * @author Kristopher T. Beck
 */
public class VExpCard extends ExpSrcCard {

    public VExpCard(String cardString) throws ParserException {
        super(cardString);
        prefix = "V";
    }

    @Override
    public VSrcExp createInstance(Deck deck) {
        VSrcExp instance = new VSrcExp();
        initSrcValues(instance, deck);
        initAuxValues(instance);
        return instance;
    }

    protected void initAuxValues(VSrcExp instance) {
        instance.setInitValue(parseDouble(initValue));
        instance.setPulsedValue(parseDouble(pulsedValue));
        instance.setRiseDelay(parseDouble(riseDelay));
        instance.setRiseConst(parseDouble(riseConst));
        instance.setFallDelay(parseDouble(fallDelay));
        instance.setFallConst(parseDouble(fallConst));
    }
}
