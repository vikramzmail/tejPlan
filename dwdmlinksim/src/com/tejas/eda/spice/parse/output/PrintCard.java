/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.output;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tejas.eda.spice.output.ACPrintOutput;
import com.tejas.eda.spice.output.DCCurvePrintOutput;
import com.tejas.eda.spice.output.PrintOutput;
import com.tejas.eda.spice.output.TranPrintOutput;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;

/**
 *
 * @author owenbad
 */
public class PrintCard extends OutputCard {

    public static final String PATTERN_STRING = "\\.PRINT\\s+(\\w+)(.+)";
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    public PrintCard(String cardString) throws ParserException {
        this.cardString = cardString;
        parseOutputs(cardString);
    }

    @Override
    public PrintOutput createOutput(Deck deck) {
        PrintOutput output = null;
        if (type.startsWith("TRAN")) {
            output = new TranPrintOutput(deck.getTitle());
        } else if (type.startsWith("AC")) {
            output = new ACPrintOutput(deck.getTitle());
        } else if (type.startsWith("DC")) {
            output = new DCCurvePrintOutput(deck.getTitle());
        }
        initParameters(output, deck);
        return output;
    }

    @Override
    public void parseOutputs(String cardString) throws ParserException {
        Matcher m = PATTERN.matcher(cardString);
        if (m.find()) {
            if (m.group(1).equals("AC")) {
                type = "AC_PRINT";
            } else if (m.group(1).equals("TRAN")) {
                type = "TRAN_PRINT";
            } else if (m.group(1).equals("DC")) {
                type = "DC_PRINT";
            }
            super.parseOutputs(cardString);
        }
    }
}
