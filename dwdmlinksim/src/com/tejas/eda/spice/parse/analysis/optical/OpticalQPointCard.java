package com.tejas.eda.spice.parse.analysis.optical;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javolution.text.Text;

import com.tejas.eda.spice.analysis.optical.OpticalAnalysis;
import com.tejas.eda.spice.analysis.optical.OpticalQAnalysis;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.analysis.OpticalAnalysisCard;

public class OpticalQPointCard extends OpticalAnalysisCard {

	public static final String PATTERN_STRING = "^\\.OPTQPT$";
	public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING); 
	
	public OpticalQPointCard(String cardString) throws ParserException {
		super(cardString);
	}

	@Override
	public void parse(String cardString) throws ParserException {
		Matcher m = PATTERN.matcher(cardString);
        if(!m.find()){
            throw new ParserException();
        }
	}

	@Override
	public OpticalAnalysis createAnalysis() {
		return new OpticalQAnalysis();
	}

	@Override
	public Text toText() {
		return Text.intern(".OPTQPT");
	}

}
