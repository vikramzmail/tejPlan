/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.analysis;

import com.tejas.eda.spice.analysis.Analysis;
import com.tejas.eda.spice.analysis.optical.OpticalAnalysis;
import com.tejas.eda.spice.parse.Card;
import com.tejas.eda.spice.parse.ParserException;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public abstract class OpticalAnalysisCard extends Card {

    public OpticalAnalysisCard(String cardString) throws ParserException {
        super(cardString);
    }

    public abstract void parse(String cardString) throws ParserException;

    public abstract OpticalAnalysis createAnalysis();

    public abstract Text toText();

    @Override
    public String toString() {
        return toText().toString();
    }
}
