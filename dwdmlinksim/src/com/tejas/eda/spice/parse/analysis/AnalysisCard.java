/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.analysis;

import com.tejas.eda.spice.analysis.Analysis;
import com.tejas.eda.spice.parse.Card;
import com.tejas.eda.spice.parse.ParserException;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public abstract class AnalysisCard extends Card {

    public AnalysisCard(String cardString) throws ParserException {
        super(cardString);
    }

    public abstract void parse(String cardString) throws ParserException;

    public abstract Analysis createAnalysis();

    public abstract Text toText();

    @Override
    public String toString() {
        return toText().toString();
    }
}
