package com.tejas.client;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.sql.Time;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.xml.*;

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.*;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class XMLWriter extends MyCanvas {

	public XMLWriter() {
		
	}

	public String writeXML(String savetype) {
		mxCodec codec = new mxCodec();
		String xml = mxXmlUtils.getXml(codec.encode(graph.getModel()));
		
		Document doc = new Document();
		Element root = new Element("network");
		doc.setRootElement(root);
		
		Map nodeIDMap = new HashMap();
		Map nodeIDNameMap = new HashMap();
		
		String pt = "physicalTopology";
		String n = "node";
		String l = "link";
		
		int nodeid = 0;
		int linkid = 0;
		
		Element phyTopology = new Element(pt);
		root.addContent(phyTopology);
		
		Map m = new HashMap();
		m = ((mxGraphModel) graph.getModel()).getCells();
		Set keys = m.keySet();
		for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			String value = m.get(key).toString();
			mxCell cell = (mxCell) m.get(key);
			if (cell.getId().equals("0") || cell.getId().equals("1")) {
				continue;
			} else {
				if(cell.isVertex()){
					Element node = new Element(n);					
					
					Attribute name = new Attribute("name", cell.getValue().toString());
					node.setAttribute(name);
					Attribute no = new Attribute("no", String.valueOf(nodeid));
					nodeIDMap.put(cell.getId(), String.valueOf(nodeid));
					nodeIDNameMap.put(cell.getValue().toString(), String.valueOf(nodeid++));
					node.setAttribute(no);
					String nodeType = "ROADM";
					if (cell.getAttribute("type") != null){
						nodeType = cell.getAttribute("type");
					}
					Attribute type = new Attribute("type", nodeType);
					node.setAttribute(type);
					
					
					phyTopology.addContent(node);
					
				} 
			}
		}
		
		for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			String value = m.get(key).toString();
			mxCell cell = (mxCell) m.get(key);
			if (cell.getId().equals("0") || cell.getId().equals("1")) {
				continue;
			} else {
				if (cell.isEdge()){
					Element link = new Element(l);
					
					
					Attribute name = new Attribute("name", cell.getValue().toString());
					link.setAttribute(name);
					Attribute no = new Attribute("sublink", String.valueOf(linkid++));
					link.setAttribute(no);
					Attribute source = new Attribute("originNodeId", nodeIDMap.get(cell.getSource().getId()).toString());
					link.setAttribute(source);
					Attribute destination = new Attribute("destinationNodeId", nodeIDMap.get(cell.getTarget().getId()).toString());
					link.setAttribute(destination);
					Attribute linkCapacityInErlangs = new Attribute("linkCapacityInErlangs", "50.0");
					link.setAttribute(linkCapacityInErlangs);
					String lengthInKm = "40.0";
					if (cell.getAttribute("length") != null){
						lengthInKm = cell.getAttribute("length");
					}
					Attribute length = new Attribute("linkLengthInKm", lengthInKm);
					link.setAttribute(length);
					
					phyTopology.addContent(link);
				}
			}
		}
		
		//Add traffic demand entries into the xml file
		String td = "demandSet";
		String de = "demandEntry";
		Element demandSet = new Element(td);
		root.addContent(demandSet);
		
//		Set trafficEntryKeys = trafficMap.keySet();
//		for (Iterator iterator = trafficEntryKeys.iterator(); iterator.hasNext();) {
//			String key = (String) iterator.next();
//			String value = m.get(key).toString();
//			mxCell cell = (mxCell) m.get(key);
//			if (cell.getId().equals("0") || cell.getId().equals("1")) {
//				continue;
//			} else {
//				if(cell.isVertex()){
		for(int i = 0; i < numTrafficEntries; i++){
			String demand = trafficMap.get(String.valueOf(i)).toString();
			
			String[] tempStr = demand.split("\\^\\&\\*");
			String trfSrcNodeName = tempStr[0];
			String trfDstNodeName = tempStr[1];
			String trfDemand = tempStr[2];
			
			Element demandEntry = new Element(de);					

			Attribute ingressNodeId = new Attribute("ingressNodeId", nodeIDNameMap.get(trfSrcNodeName).toString());
			demandEntry.setAttribute(ingressNodeId);
			
			Attribute egressNodeId = new Attribute("egressNodeId", nodeIDNameMap.get(trfDstNodeName).toString());
			demandEntry.setAttribute(egressNodeId);
			
			Attribute offeredTrafficInErlangs = new Attribute("offeredTrafficInErlangs", trfDemand);
			demandEntry.setAttribute(offeredTrafficInErlangs);

			demandSet.addContent(demandEntry);
		}
					
//				} 
//			}
//		}
		
		String path = "tempfiles";
		if (savetype.equals("tempsave")){
			path = "tempfiles";
		} else if (savetype.equals("saveforsim")){
			path = "xmlFiles";
		}
		String filename = "filename_" + System.currentTimeMillis();
		
//		 Creates the Parent XML file.
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		try {
			outputter.output(doc, new FileWriter(path + "/" + filename + "_input.xml"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		Date date = new Date();
		
		return filename;
	}
		


}		


