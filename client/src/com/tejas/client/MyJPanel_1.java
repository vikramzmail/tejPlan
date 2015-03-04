package com.tejas.client;

import java.awt.Color;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.mxgraph.layout.mxFastOrganicLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MyJPanel_1 extends JPanel {
private File file;
	public MyJPanel_1(){
		init();
	}

	private void init() {
		this.setBounds(12, 55, 291, 235);
		this.setBorder(
	            BorderFactory.createTitledBorder(
	            BorderFactory.createEtchedBorder(
	                    EtchedBorder.RAISED, Color.GRAY
	                    , Color.DARK_GRAY), "Main Panel 1"));
		this.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Main Label");
		lblNewLabel.setBounds(12, 13, 123, 38);
		this.add(lblNewLabel);
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblNewLabel.setForeground(new Color(255, 99, 71));
		
		JRadioButton rdbtn1 = new JRadioButton("Load an existing topology");
		rdbtn1.setBounds(12, 45, 220, 25);
		this.add(rdbtn1);
		rdbtn1.setFont(new Font("Tahoma", Font.PLAIN, 14));		
		
		JRadioButton rdbtn2 = new JRadioButton("Design a new topology");
		rdbtn2.setBounds(12, 114, 220, 25);
		this.add(rdbtn2);
		rdbtn2.setFont(new Font("Tahoma", Font.PLAIN, 14));
		
		ButtonGroup group = new ButtonGroup();
        group.add(rdbtn1);
        group.add(rdbtn2);
		
		JButton btnload = new JButton("Load");
		btnload.setBounds(41, 80, 84, 25);
		this.add(btnload);
				
		btnload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		        showLoadDialog();
			}			
	});
		JButton btnsave = new JButton("Save");
		btnsave.setBounds(137, 80, 84, 25);
		this.add(btnsave);		
		
		btnsave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent m) {
				showSaveDialog();
			}			
	});
					
	}
		
		public void showLoadDialog() {
			JFileChooser fc = new JFileChooser();
			fc.setDialogTitle("Open");   
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.addChoosableFileFilter(new FileNameExtensionFilter("XML Documents", "xml"));
		    fc.setAcceptAllFileFilterUsed(true);
		    
		    int result = fc.showOpenDialog(this);
		    if (result == JFileChooser.APPROVE_OPTION) {
		    	File selectedFile = fc.getSelectedFile();
		    	loadGraphAndTraffic(selectedFile);
		    }
		}
		
		private void loadGraphAndTraffic(File selectedFile) {
			MyCanvas.getGraph().getModel().beginUpdate();
			MyCanvas.graph.repaint();
			MyCanvas.getM().clear();
			MyCanvas.trafficMap.clear();
			MyCanvas.numTrafficEntries = 0;
			
			Object parent = MyCanvas.graph.getDefaultParent();
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = null;
			try {
				dBuilder = dbFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
			Document doc = null;
			try {
				doc = dBuilder.parse(selectedFile);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("node");
			NodeList lList = doc.getElementsByTagName("link");
			NodeList dList = doc.getElementsByTagName("demandEntry");
			NodeList rList = doc.getElementsByTagName("route");
			
			Map nodeIDNameMap = new HashMap();
			Map nodeNameIDMap = new HashMap();
			
			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					Object v1 = MyCanvas.graph.insertVertex(parent, null, eElement.getAttribute("name"), 300, 165, 35, 30);
					MyCanvas.getM().put(eElement.getAttribute("name"), v1);
					
					nodeIDNameMap.put(eElement.getAttribute("no"), eElement.getAttribute("name"));
					nodeNameIDMap.put(eElement.getAttribute("name"), eElement.getAttribute("no"));
				}
			}
			
			for (int temp = 0; temp < lList.getLength(); temp++) {

				Node lLink = lList.item(temp);
	
				if (lLink.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) lLink;
					String srcNodeName = nodeIDNameMap.get(eElement.getAttribute("originNodeId")).toString();
					String dstNodeName = nodeIDNameMap.get(eElement.getAttribute("destinationNodeId")).toString();
					Object v1 = MyCanvas.getM().get(srcNodeName);
					Object v2 = MyCanvas.getM().get(dstNodeName);
					MyCanvas.getGraph().insertEdge(parent, null, "L", v1, v2);
					String linkLength = eElement.getAttribute("linkLengthInKm");

				}
			
			}
			
			for (int temp = 0; temp < dList.getLength(); temp++) {

				Node dLink = dList.item(temp);

				if (dLink.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) dLink;
					MyCanvas.trafficMap.put(String.valueOf(MyCanvas.numTrafficEntries++), 
							nodeIDNameMap.get(eElement.getAttribute("ingressNodeId")) + "^&*" + 
							nodeIDNameMap.get(eElement.getAttribute("egressNodeId"))  + "^&*" + 
							eElement.getAttribute("offeredTrafficInErlangs"));
				}
			}
			
			MyCanvas.getGraph().getModel().endUpdate();
			MyCanvas.getGraph().refresh();
			mxFastOrganicLayout mxf = new mxFastOrganicLayout(MyCanvas.getGraph());
			mxf.execute(MyCanvas.getGraph().getDefaultParent());
		}

		public void showSaveDialog(){
			String samp = "SAMPLE";
			JFileChooser fc1 = new JFileChooser();
			fc1.setDialogTitle("Save as");		
		    fc1.setCurrentDirectory(new File("E:\\Java UI\\tejplan\\tempfiles"));

			int userSelection = fc1.showSaveDialog(null);
			if (userSelection == JFileChooser.APPROVE_OPTION) {
			    
				try(FileWriter fw = new FileWriter(fc1.getSelectedFile()+".txt")) {
				    fw.write(samp.toString());
					fw.close();
//					System.out.println("Save as file: " + fw.getAbsolutePath());
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		}
}	        
		
		

