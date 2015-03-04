package com.tejas.engine;



import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class input {

	private static String[][] NODE_INFO = new String[1000][10] ; //Consists of Node Info.; takes only 1000 Nodes Info
	private static String[][] TOPO_INFO = new String[1000][10] ; //Consists of Topology Info.; takes only 1000 paths
	private static String[][] DEMAND_INFO = new String[1000][10] ; //Consists of Traffic Matrix Info.; takes only 1000 inputs
	
	/* NODE_INFO consists of Node_Number, Name_of_the_Node, Type_of_the_Node, Degree_of_the_Node --- in the order mentioned
	 * TOPO_INFO consists of Link_ID, Source, Destination, Length_in_KM --- in the order mentioned  
	 * DEMAND_INFO consists of Demand_ID, Source, Destination, Demand_In_Gbps --- in the order mentioned  
	 * */

public static void main(String argv[]) {

  try {
	  	
	  	File file = new File("D:\\xmlCode\\outputfile_3.cir");
		file.delete();
		if (!file.exists()) {
			file.createNewFile();
		}
		String title = "Linear DWDM Link";
	  	File fXmlFile = new File("D:\\xmlCode\\inputfile_3.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
	 
		doc.getDocumentElement().normalize();
	 
	//	System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
	 
		NodeList nList = doc.getElementsByTagName("node");
		NodeList lList = doc.getElementsByTagName("link");
		NodeList dList = doc.getElementsByTagName("demandEntry");
		NodeList rList = doc.getElementsByTagName("route");

		FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
		BufferedWriter bw = new BufferedWriter(fw);
		
		bw.write(title);
		bw.newLine();
			
//		System.out.println("----------------------------");
 
		for (int temp = 0; temp < nList.getLength(); temp++) {
	 
			Node nNode = nList.item(temp);
	 
			//System.out.println("\nCurrent Element :" + nNode.getNodeName());
	 
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
	 
				Element eElement = (Element) nNode;
	 
				//System.out.println("Node num : " + eElement.getAttribute("no"));
				String nodeNum = eElement.getAttribute("no");
				NODE_INFO[temp][0]=nodeNum;
				
				//System.out.println("Node name : " + eElement.getAttribute("name"));
				String nodeName = eElement.getAttribute("name");
				NODE_INFO[temp][1]=nodeName;
								
				//System.out.println("Node type : " + eElement.getAttribute("type"));
				String nodeType = eElement.getAttribute("type");
				NODE_INFO[temp][2]=nodeType;
				
				//System.out.println("Node degree : " + eElement.getAttribute("degree"));
				String nodeDegree = eElement.getAttribute("degree");
				NODE_INFO[temp][3]=nodeDegree;
											
				/*bw.newLine();
				bw.write("Number: "+nodeNum+" Name: "+nodeName + " Type: "+nodeType +  " Degree: "+nodeDegree);
				
				System.out.println("\n \nDone writing also");*/
			}
		}
		// bw.newLine();
		for (int temp = 0; temp < lList.getLength(); temp++) {
			 
			Node lLink = lList.item(temp);
	 
			//System.out.println("\nCurrent Element :" + lLink.getNodeName());
	 
			if (lLink.getNodeType() == Node.ELEMENT_NODE) {
	 
				Element eElement = (Element) lLink;
	 
				//System.out.println("Id: " + eElement.getAttribute("id"));
				String Id = eElement.getAttribute("id");
				TOPO_INFO[temp][0]=Id;
				
				//System.out.println("Source : " + eElement.getAttribute("originNodeId"));
				String negNode = eElement.getAttribute("originNodeId");
				TOPO_INFO[temp][1]=negNode;
						
				//System.out.println("Destination: " + eElement.getAttribute("destinationNodeId"));
				String posNode = eElement.getAttribute("destinationNodeId");
				TOPO_INFO[temp][2]=posNode;
				
				//System.out.println("Length in KM : " + eElement.getAttribute("linkLengthInKm"));
				String linkLength = eElement.getAttribute("linkLengthInKm");
				TOPO_INFO[temp][3]=linkLength;
							
				/*bw.newLine();
				bw.write("Source: "+negNode + " Destination: "+posNode + " LengthinKM: "+linkLength+ " Id: "+Id);
				
				
				System.out.println("\n \nDone writing also");*/
			}
		
		}
	//	bw.newLine();
		for (int temp = 0; temp < dList.getLength(); temp++) {
			 
			Node dLink = dList.item(temp);
	 
			//System.out.println("\nCurrent Element :" + dLink.getNodeName());
	 
			if (dLink.getNodeType() == Node.ELEMENT_NODE) {
	 
				Element eElement = (Element) dLink;
	 
				//System.out.println("Id: " + eElement.getAttribute("id"));
				String Id = eElement.getAttribute("id");
				DEMAND_INFO[temp][0]= Id;
						
				//System.out.println("Egress Node: " + eElement.getAttribute("egressNodeId"));
				String negNode = eElement.getAttribute("egressNodeId");
				DEMAND_INFO[temp][1]= negNode;
								
				//System.out.println("Ingress Node : " + eElement.getAttribute("ingressNodeId"));
				String posNode = eElement.getAttribute("ingressNodeId");
				DEMAND_INFO[temp][2]= posNode;
				
				//System.out.println("Traffic In Gbps : " + eElement.getAttribute("offeredTrafficInErlangs"));
				String TrafficInGbps = eElement.getAttribute("offeredTrafficInErlangs");
				DEMAND_INFO[temp][3]=TrafficInGbps ;
				
				/*bw.newLine();
				bw.write("EgressNode: "+negNode + " IngressNode: "+posNode + " Traffic In Gbps: "+TrafficInGbps+ " demandId: "+Id);
		
				
				System.out.println("\n \nDone writing also");*/
			}
		}
//		bw.newLine();
		for (int temp = 0; temp < rList.getLength(); temp++) {
			 
			Node rLink = rList.item(temp);
	 
			System.out.println("\nCurrent Element :" + rLink.getNodeName());
	 
			if (rLink.getNodeType() == Node.ELEMENT_NODE) {
	 
				Element eElement = (Element) rLink;
				
				//System.out.println("Route: " + eElement.getAttribute("routeId"));
				String route = eElement.getAttribute("routeId");
				
				//System.out.println("DemandId: " + eElement.getAttribute("demandId"));
				String demId = eElement.getAttribute("demandId");
		
				bw.newLine();
				bw.write("---------------------------------");
				bw.newLine();
				bw.write("Route = "+route+" demandId = "+demId);
				bw.newLine();
				bw.write("---------------------------------");
				bw.newLine();
				String[] routeId = route.split("-");
				//System.out.println("***********-----------!!!!!@@@@@@@@@-----------"+routeId.length);
				for(int i =0;i<routeId.length;i++) //length of the route in terms of ID
				{
					int INT = Integer.parseInt(routeId[i]);
					int INT_TO_BE_PASSED = Integer.parseInt(TOPO_INFO[INT][2]);
					System.out.println(INT_TO_BE_PASSED);
					
					int count = i;
					

								bw.newLine();
								/*if (i==0)
								{
									bw.write("L"+(i+1)+" "+(i+1)+" "+(i+2)+" "+"OPTLINK LEN="+TOPO_INFO[INT][3]);
									if(NODE_INFO[INT_TO_BE_PASSED][2].equalsIgnoreCase("EDFA"))
									{
										bw.newLine();
										bw.write("A"+(i+1)+" "+(i+2)+" "+(i+3)+" "+"EDFA");
										}
									else if(NODE_INFO[INT_TO_BE_PASSED][2].equalsIgnoreCase("ROADM"))
									{
										bw.newLine();
										bw.write("R"+(i+1)+" "+(i+2)+" "+(i+3)+" "+"ROADM");
										}
									else if(NODE_INFO[INT_TO_BE_PASSED][2].equalsIgnoreCase("MUX"))
									{
										bw.newLine();
										bw.write("M"+(i+1)+" "+(i+2)+" "+(i+3)+" "+"MUX");
										}
								}*/
								//else
								//{
																	
									bw.write("L"+(i+1)+" "+(count+(i+1))+" "+(count+(i+2))+" "+"OPTLINK LEN="+TOPO_INFO[INT][3]);
									if(NODE_INFO[INT_TO_BE_PASSED][2].equalsIgnoreCase("EDFA"))
									{
										bw.newLine();
										bw.write("A "+(count+i+2)+" "+(count+i+3)+" "+"EDFA");
										}
									else if(NODE_INFO[INT_TO_BE_PASSED][2].equalsIgnoreCase("ROADM"))
									{
										bw.newLine();
										bw.write("R "+(count+i+2)+" "+(count+i+3)+" "+"ROADM");
										}
									else if(NODE_INFO[INT_TO_BE_PASSED][2].equalsIgnoreCase("MUX"))
									{
										bw.newLine();
										bw.write("M " +(count+i+2)+" "+(count+i+3)+" "+"MUX");
										}
								//}
									
				}
				bw.newLine();
				bw.newLine();
				bw.newLine();
				bw.write("Pin 1 0 0");
				bw.newLine();
				bw.write(".optqpt");
				bw.newLine();
				bw.write(".end");
				System.out.println("Demand Id: " + eElement.getAttribute("demandId"));
				String demandId = eElement.getAttribute("demandId");
				System.out.println("For demand Id:" + demandId+ ", the path is: "+route);
				System.out.println("\n \nDone writing also");
			}
			
		}
		bw.close();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	  }
	 
	}
  