/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.source.vsrc;

import com.tejas.eda.spice.device.sources.vsrcs.VSrcSFFM;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.source.SffmSrcCard;

/**
 *
 * @author Kristopher T. Beck
 */
public class VSFFMCard extends SffmSrcCard {

    public VSFFMCard(String cardString) throws ParserException {
        super(cardString);
        prefix = "V";
    }

    @Override
    public VSrcSFFM createInstance(Deck deck) {
        VSrcSFFM instance = new VSrcSFFM();
        initSrcValues(instance, deck);
        initAuxValues(instance);
        return instance;
    }

    protected void initAuxValues(VSrcSFFM instance) {
        instance.setOffset(parseDouble(offset));
        instance.setAmplitude(parseDouble(amplitude));
        instance.setCarrierFreq(parseDouble(carrierFreq));
        instance.setModulationIndex(parseDouble(modulationIndex));
        instance.setSignalFreq(parseDouble(signalFreq));
    }
}
