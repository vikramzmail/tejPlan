/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source.vsrc;

import com.tejas.eda.spice.device.sources.vsrcs.VSrcSine;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.source.SineSrcCard;

/**
 *
 * @author Kristopher T. Beck
 */
public class VSineCard extends SineSrcCard {

    public VSineCard(String cardString) throws ParserException {
        super(cardString);
        prefix = "V";
    }

    @Override
    public VSrcSine createInstance(Deck deck) {
        VSrcSine instance = new VSrcSine();
        initSrcValues(instance, deck);
        initAuxValues(instance);
        return instance;
    }

    protected void initAuxValues(VSrcSine instance) {
        instance.setOffset(parseDouble(offset));
        instance.setAmplitude(parseDouble(amplitude));
        instance.setFrequency(parseDouble(freq));
        instance.setDelayTime(parseDouble(delayTime));
        instance.setTheta(parseDouble(theta));
    }
}
