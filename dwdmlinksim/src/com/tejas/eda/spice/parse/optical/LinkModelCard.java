package com.tejas.eda.spice.parse.optical;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.Model;
import com.tejas.eda.spice.device.optical.LinkInstance;
import com.tejas.eda.spice.device.optical.LinkModelValues;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.OpticalModelCard;
import com.tejas.eda.spice.parse.ParserException;

public class LinkModelCard extends OpticalModelCard<LinkInstance, LinkModelValues> {

	public LinkModelCard(String cardString) throws ParserException {
		super(cardString);
	}

	@Override
	public LinkInstance createInstance() {
		LinkInstance instance = new LinkInstance();
		initModelValues(instance);
		return instance;
	}

	@Override
	public void setProperty(LinkModelValues model, String name, String value) {
		if (name.equals("ALPHA")) {
            model.setAlpha(parseDouble(value));
        } else if (name.equals("D")){
        	model.setDee(parseDouble(value));
        }
	}
}
