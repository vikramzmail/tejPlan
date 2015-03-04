/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source.isrc;

import com.tejas.eda.spice.device.sources.isrcs.ISrcSine;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.source.SineSrcCard;

/**
 *
 * @author Kristopher T. Beck
 */
public class ISineCard extends SineSrcCard {

    public ISineCard(String cardString) throws ParserException {
        super(cardString);
        prefix = "I";
    }

    @Override
    public ISrcSine createInstance(Deck deck) {
        ISrcSine instance = new ISrcSine();
        initSrcValues(instance, deck);
        initAuxValues(instance);
        return instance;
    }

    protected void initAuxValues(ISrcSine instance) {
        instance.setOffset(parseDouble(offset));
        instance.setAmplitude(parseDouble(amplitude));
        instance.setFrequency(parseDouble(freq));
        instance.setDelayTime(parseDouble(delayTime));
        instance.setTheta(parseDouble(theta));
    }
}
