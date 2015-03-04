/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source.isrc;

import java.util.regex.Matcher;

import com.tejas.eda.spice.device.sources.isrcs.ISrcExp;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.source.ExpSrcCard;

/**
 *
 * @author Kristopher T. Beck
 */
public class IExpCard extends ExpSrcCard {

    public IExpCard(String cardString) throws ParserException {
        super(cardString);
        prefix = "I";
    }

    @Override
    public ISrcExp createInstance(Deck deck) {
        ISrcExp instance = new ISrcExp();
        initSrcValues(instance, deck);
        initAuxValues(instance);
        return instance;
    }

    protected void initAuxValues(ISrcExp instance) {
        instance.setInitValue(parseDouble(initValue));
        instance.setPulsedValue(parseDouble(pulsedValue));
        instance.setRiseDelay(parseDouble(riseDelay));
        instance.setRiseConst(parseDouble(riseConst));
        instance.setFallDelay(parseDouble(fallDelay));
        instance.setFallConst(parseDouble(fallConst));
    }
}
