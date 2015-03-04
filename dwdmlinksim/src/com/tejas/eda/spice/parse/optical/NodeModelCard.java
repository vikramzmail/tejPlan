package com.tejas.eda.spice.parse.optical;

import com.tejas.eda.spice.device.Instance;
import com.tejas.eda.spice.device.Model;
import com.tejas.eda.spice.device.optical.NodeInstance;
import com.tejas.eda.spice.device.optical.NodeModelValues;
import com.tejas.eda.spice.device.optical.EDFAInstance;
import com.tejas.eda.spice.device.optical.EDFAModelValues;
import com.tejas.eda.spice.device.optical.LinkInstance;
import com.tejas.eda.spice.device.optical.LinkModelValues;
import com.tejas.eda.spice.parse.ModelCard;
import com.tejas.eda.spice.parse.OpticalModelCard;
import com.tejas.eda.spice.parse.ParserException;

public class NodeModelCard extends OpticalModelCard<NodeInstance, NodeModelValues> {

	public NodeModelCard(String cardString) throws ParserException {
		super(cardString);
	}

	@Override
	public NodeInstance createInstance() {
		NodeInstance instance = new NodeInstance();
		initModelValues(instance);
		return instance;
	}

	@Override
	public void setProperty(NodeModelValues model, String name, String value) {
		if (name.equalsIgnoreCase("NLOSS")) {
            model.setnLoss(parseDouble(value));
        }
	}
}
