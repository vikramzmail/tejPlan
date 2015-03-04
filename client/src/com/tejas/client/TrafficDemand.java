package com.tejas.client;

import java.awt.List;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TrafficDemand extends MyCanvas {
	
	ArrayList list = new ArrayList();
	
	
	
	public TrafficDemand(){

		String inStr = JOptionPane.showInputDialog(null, "Number of nodes to be configured",
	            "Traffic Pattern", JOptionPane.PLAIN_MESSAGE);
		int cnt_node = Integer.parseInt(inStr);
		int cnt_link = (cnt_node*(cnt_node-1))/2;
		
		JOptionPane.showMessageDialog(null, "For "+cnt_node+" Nodes, "+cnt_link+" Links can be configured for Mesh Network.",
	            "Message Dialog", JOptionPane.PLAIN_MESSAGE);
	
		System.out.println("For "+cnt_node+" Nodes,"+cnt_link+" Links can be configured.");

		while(cnt_link != 0){

			String str1 = JOptionPane.showInputDialog("From node: ");
			String str2 = JOptionPane.showInputDialog("To node: ");
			String str3 = JOptionPane.showInputDialog("Traffic between two nodes(in Mbps)");
					
			list.add(str1);            
        	list.add(str2);
        	list.add(str3);     	
			
        	cnt_link--;
		
        	System.out.println(list);	      
        	writeXMLfile(list);
	    
	    int No = JOptionPane.showConfirmDialog(null, "Do you want to add more nodes ?");        
        if (No == JOptionPane.NO_OPTION)
        {                 	
      		System.exit(0);		
        }
        
		}
	}

	public void writeXMLfile(ArrayList<Object> list) {

		try {

	        DocumentBuilderFactory dFact = DocumentBuilderFactory.newInstance();
	        DocumentBuilder build = dFact.newDocumentBuilder();
	        Document doc = build.newDocument();

	        Element root = doc.createElement("demandSet");
	        doc.appendChild(root);

	        Element Details = doc.createElement("demandEntry");
	        root.appendChild(Details);


	        for(int i=0; i<list.size()-2; i += 3 ) {
	        
	            Element name = doc.createElement("egressNodeId");
	            name.appendChild(doc.createTextNode(String.valueOf(list.get(i))));
	            Details.appendChild(name);

	            Element id = doc.createElement("ingressNodeId");
	            id.appendChild(doc.createTextNode(String.valueOf(list.get(i+1))));
	            Details.appendChild(id);


	            Element mmi = doc.createElement("offeredTraffic");
	            mmi.appendChild(doc.createTextNode(String.valueOf(list.get(i+2))));
	            Details.appendChild(mmi);

	       }

	         // Save the document to the disk file
	        TransformerFactory tranFactory = TransformerFactory.newInstance();
	        Transformer aTransformer = tranFactory.newTransformer();

	        // format the XML nicely
	        aTransformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
	        aTransformer.setOutputProperty(
	                "{http://xml.apache.org/xslt}indent-amount", "4");
	        aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");

	        DOMSource source = new DOMSource(doc);
	        try {
	            FileWriter fos = new FileWriter("E:/temp/demandSet.xml");
	            StreamResult result = new StreamResult(fos);
	            aTransformer.transform(source, result);

	        } catch (IOException e) {

	            e.printStackTrace();
	        }

	    } catch (TransformerException ex) {
	        System.out.println("Error outputting document");

	    } catch (ParserConfigurationException ex) {
	        System.out.println("Error building document");
	    }
	}
}
