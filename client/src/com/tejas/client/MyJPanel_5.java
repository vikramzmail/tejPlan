package com.tejas.client;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

public class MyJPanel_5 extends JPanel {
	public MyJPanel_5(){
		init();
	}

	private void init() {
		this.setLayout(null);
		this.setBorder(BorderFactory.createTitledBorder(
			            BorderFactory.createEtchedBorder(
			                    EtchedBorder.RAISED, Color.GRAY
			                    , Color.DARK_GRAY), "Main Panel 5"));
		this.setBounds(303, 535, 679, 193);		
	}
}
