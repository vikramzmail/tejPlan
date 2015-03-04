package com.tejas.client;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EtchedBorder;

import com.mxgraph.model.mxCell;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.BoxLayout;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.DefaultComboBoxModel;
import javax.swing.border.TitledBorder;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class MyJPanel_2 extends JPanel {
	private JTextField textField = new JTextField();
	private JTextField textField2 = new JTextField();
	private JComboBox comboBox = new JComboBox();
	private JPanel panel = new JPanel();

	public MyJPanel_2() {
		init(); 
	}
	
	private void init() {
		this.setBounds(16, 533, 283, 229);
		setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(128, 128, 128), new Color(64, 64, 64)), "Main Panel 2", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		setLayout(new GridLayout(2, 1, 0, 0));
		
		JButton btnRefresh = new JButton("Refresh");
		add(btnRefresh);
		add(panel);
		btnRefresh.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshProperties();
			}
		});
		
		textField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				String enteredText = textField.getText();
				MyCanvas.cell.setValue(enteredText);
				MyCanvas.graph.refresh();
			}
		});
		
		comboBox.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				String enteredType = comboBox.getSelectedItem().toString();
				MyCanvas.cell.setAttribute("type", enteredType);
				MyCanvas.graph.refresh();
			}
		});
		
		textField2.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				String enteredText = textField2.getText();
				MyCanvas.cell.setAttribute("length", enteredText);
				MyCanvas.graph.refresh();
			}
		});
	}

	protected void refreshProperties() {
		panel.removeAll();
		panel.setLayout(new GridLayout(2, 2, 0, 0));
		if(MyCanvas.currentobj instanceof mxCell){
			MyCanvas.cell = (mxCell) MyCanvas.currentobj;
			if(MyCanvas.cell.isVertex()){
				JLabel lblValue = new JLabel("Value");
				lblValue.setHorizontalAlignment(SwingConstants.RIGHT);
				panel.add(lblValue);
				
				textField.setText(MyCanvas.cell.getValue().toString());
				panel.add(textField);
				textField.setColumns(10);
				
				JLabel lblType = new JLabel("Type");
				lblType.setHorizontalAlignment(SwingConstants.RIGHT);
				panel.add(lblType);
				
				comboBox.setModel(new DefaultComboBoxModel(new String[] {"EDFA", "ROADM"}));
				String selectedItem = MyCanvas.cell.getAttribute("type");
				if (selectedItem != null){
					comboBox.setSelectedItem(selectedItem);
				} else {
					comboBox.setSelectedItem("EDFA");
				}
				panel.add(comboBox);
			}
			if (MyCanvas.cell.isEdge()){
				JLabel lblValue = new JLabel("Value");
				lblValue.setHorizontalAlignment(SwingConstants.RIGHT);
				panel.add(lblValue);
				
				textField.setText(MyCanvas.cell.getValue().toString());
				panel.add(textField);
				textField.setColumns(10);
				
				JLabel lblType = new JLabel("Length (km)");
				lblType.setHorizontalAlignment(SwingConstants.RIGHT);
				panel.add(lblType);
				
				textField2.setText("40");
				panel.add(textField2);
				textField2.setColumns(10);
			}
		}
	}
}