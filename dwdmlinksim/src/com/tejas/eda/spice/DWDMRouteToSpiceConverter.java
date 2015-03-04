package com.tejas.eda.spice;

import java.awt.List;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Multiset.Entry;
import com.tejas.eda.spice.parse.optical.LinkCard;
import com.tejas.engine.utils.Pair;
import com.tejas.engine.utils.Triple;

public class DWDMRouteToSpiceConverter {
	private static int INT_TO_BE_PASSED = 0;

	private static String[][] NODE_INFO = new String[1000][10]; // Consists of
																// Node Info.;
																// takes only
																// 1000 Nodes
																// Info
	private static String[][] TOPO_INFO = new String[1000][10]; // Consists of
																// Topology
																// Info.; takes
																// only 1000
																// paths
	private static String[][] DEMAND_INFO = new String[1000][10]; // Consists of
																	// Traffic
																	// Matrix
																	// Info.;
																	// takes
																	// only
																	// 1000
																	// inputs
	static double nLoss = 0;
	static double n2Loss = 0;
	static double n3Loss = 0;

	static double LineAmp_min_ip = -28.0;
	static double LineAmp_max_ip = -3.0;
	static double LineAmp_sat_op = 17;
	static double LineAmp_gain = 20;
	static double Booster_gain = 23;
	static double PreAmp_gain = 20;

	static double ROADM_max_ip = 24.0;

	static double Tx_op = 0.0;
	static double Rx_min_sens = -25;
	static double Rx_max_sens = -15;

	static double Alpha_SM = -0.27;
	static double Alpha_NZ = -0.22;
	double Disp_SM = 17;
	double Disp_NZ = 10;

	static int Disp_30Km = 510;
	static double Disp_30_Loss = -4;
	static int Disp_60Km = 1020;
	static double Disp_60_Loss = -4;
	static int Disp_80Km = 1360;
	static double Disp_80_Loss = -4;
	static int Disp_120Km = 2040;
	static double Disp_120_Loss = -4;
	static int Disp_30KmNegLimit = -510;
	static int Disp_LengthLimit = 79;
	static double Connector_loss = 0.5;

	static int LineAmp_Num;
	static double key_previous;
	static int x;
	static int i;
	static int m;
	static int l;
	static int j;
	static int n;
	static int b;
	static int d;
	static double Length_Loss;
	static double Length;
	static double Length1;
	static double Dispersion;
	static double copy;
	static String key;
	static int count;
	static int Disp_count;

	static LinkedHashMap<String, Double> New_PowerMap = new LinkedHashMap();
	static LinkedHashMap<String, Double> New_DispersionMap = new LinkedHashMap();
	static LinkedHashMap<String, Double> New_NoiseMap = new LinkedHashMap();
	Map<String, Pair<String, String>> ModelNames_New = new LinkedHashMap<String, Pair<String, String>>();
	static LinkedHashMap<String, Double> ModelNames_Old = new LinkedHashMap();
	static ArrayList<String> ModelArray = new ArrayList<String>();
	static Iterator<String> keySetIterator;

	/*
	 * NODE_INFO consists of Node_Number, Name_of_the_Node, Type_of_the_Node,
	 * Degree_of_the_Node --- in the order mentioned TOPO_INFO consists of
	 * 
	 * 
	 * Link_ID, Source, Destination, Length_in_KM --- in the order mentioned
	 * DEMAND_INFO consists of Demand_ID, Source, Destination, Demand_In_Gbps
	 * --- in the order mentioned
	 */
	private static double nNoise = 0;
	private static String[] linkId; // ss
	private static String[] destList; // ss
	private static String[] fiberType; // ss
	private static int INT; // ss
	private static double character;

	static int k;
	static ArrayList<String> list = new ArrayList<String>();
	ArrayList<String> keySet = new ArrayList<String>();
	ArrayList<Integer> keySetToInt = new ArrayList<Integer>();
	ArrayList<Double> LengthArray = new ArrayList<Double>();
	static ArrayList<String> DCMArray = new ArrayList<String>();

	String cirFilePath = "cirFiles";

	FileWriter fw;
	static BufferedWriter bw;

	public static void main(String args[]) {
		new DWDMRouteToSpiceConverter(args);
	} // comment for debugging

	public DWDMRouteToSpiceConverter(String[] args) { // comment for debugging
		try {

			String routesFileName = args[0]; // comment for debugging
			// comment for debugging
			String xmlFilePath = "xmlFiles";
			String title = "Linear DWDM Link";
			File fXmlFile = new File(routesFileName); // comment for debugg
			// File fXmlFile = new File("D:/tejplan/inputfile_linear_dwdm.xml");
			// // remove comment for debug
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			Document doc_to_write = dBuilder.newDocument();
			Element rootElement = doc_to_write
					.createElement("Complete_calc_Info");
			doc_to_write.appendChild(rootElement);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("node");
			NodeList lList = doc.getElementsByTagName("link");
			NodeList dList = doc.getElementsByTagName("demandEntry");
			NodeList rList = doc.getElementsByTagName("route");
			String nodeNum = null;
			String nodeDegree = null;// SS
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					nodeNum = eElement.getAttribute("no");
					NODE_INFO[Integer.parseInt(nodeNum)][0] = nodeNum;
					String nodeName = eElement.getAttribute("name");
					NODE_INFO[Integer.parseInt(nodeNum)][1] = nodeName;
					String nodeType = eElement.getAttribute("type");
					NODE_INFO[Integer.parseInt(nodeNum)][2] = nodeType;
					nodeDegree = eElement.getAttribute("degree");
					// if (nodeDegree == ""){
					// nodeDegree = "2";
				}
			}
			destList = new String[lList.getLength()]; // ss
			fiberType = new String[lList.getLength()]; // ss
			String PosNode = null;// *SS
			String NegNode = null;// ss
			int temp1 = 0;
			for (temp1 = 0; temp1 < lList.getLength(); temp1++) {
				Node lLink = lList.item(temp1);
				if (lLink.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) lLink;
					String Id = eElement.getAttribute("LinkId");
					TOPO_INFO[Integer.parseInt(Id)][0] = Id;
					NegNode = eElement.getAttribute("originNodeId");
					TOPO_INFO[Integer.parseInt(Id)][1] = NegNode;
					PosNode = eElement.getAttribute("destinationNodeId");
					TOPO_INFO[Integer.parseInt(Id)][2] = PosNode;
					String linkLength = eElement.getAttribute("linkLengthInKm");
					TOPO_INFO[Integer.parseInt(Id)][3] = linkLength;
					String fiber = eElement.getAttribute("linktype"); // SS
					TOPO_INFO[Integer.parseInt(Id)][4] = fiber; // SS
					// SS ROADM degree identification (Set the degree of each
					// ROADM unit in the topology based on the
					// number of fiber links passing through each ROADM node)
					fiberType[temp1] = fiber; // ss
				}
				destList[temp1] = PosNode; // *SS
			}
			int[] a = new int[destList.length];
			for (int i = 0; i < a.length; i++) {
				a[i] = Integer.parseInt(destList[i]);
			}
			Arrays.sort(a);
			for (int i = 0; i < a.length; i++) {
				destList[i] = String.valueOf(a[i]);
			}
			HashMap<String, Integer> repeatNames = new HashMap<String, Integer>();
			int repeatCount = 0;
			for (int j = 0; j < destList.length; j++) {
				int count = 0;
				for (int k = 0; k < destList.length; k++) {
					if (destList[j].equals(destList[k])) {
						count++;
					}
				}
				if (!repeatNames.containsKey(destList[j])) {
					System.out.println("Dest node " + destList[j]
							+ ": Degree= " + count);
					repeatNames.put(destList[j], count);
					repeatCount += count;
					if (count > 0 && count < 3) {
						nodeDegree = "2";
						NODE_INFO[Integer.parseInt(destList[j])][3] = nodeDegree;
					} else if (count > 2 && count < 5) {
						nodeDegree = "4";
						NODE_INFO[Integer.parseInt(destList[j])][3] = nodeDegree;
					} else if (count > 4 && count < 9) {
						nodeDegree = "8";
						NODE_INFO[Integer.parseInt(destList[j])][3] = nodeDegree;
					}
				}
			} // *SS
			for (int temp = 0; temp < dList.getLength(); temp++) {
				Node dLink = dList.item(temp);
				if (dLink.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) dLink;
					String Id = eElement.getAttribute("DemandId");
					DEMAND_INFO[Integer.parseInt(Id)][0] = Id;
					String posNode = eElement.getAttribute("ingressNodeId");
					DEMAND_INFO[Integer.parseInt(Id)][1] = posNode;
					String negNode = eElement.getAttribute("egressNodeId");
					DEMAND_INFO[Integer.parseInt(Id)][2] = negNode;
					String TrafficInGbps = eElement
							.getAttribute("offeredTrafficInErlangs");
					DEMAND_INFO[Integer.parseInt(Id)][3] = TrafficInGbps;
				}
			}
			for (int temp = 0; temp < rList.getLength(); temp++) {
				// comment for
				File file = new File(cirFilePath
						+ "/outputfile_linear_dwdm.cir"); // debugging
				// File file = new
				// File("D:/tejplan/outputfile_linear_dwdm.cir"); // remove
				// comment for debug
				file.delete();
				if (!file.exists()) {
					file.createNewFile();
				}
				fw = new FileWriter(file.getAbsoluteFile(), true);
				bw = new BufferedWriter(fw);
				bw.write(title);
				Node rLink = rList.item(temp);
				Map<String, Triple<String, String, String>> calcPointMap = new HashMap<String, Triple<String, String, String>>();

				j = 0;
				x = 0;
				l = 0;
				m = 0;
				b = 0;
				d = 0;
				count = 0;
				Length1 = 0;
				Length = 0;
				Length_Loss = 0;
				key_previous = 0;
				copy = 0;
				Dispersion = 0;
				Disp_count = 0;
				if (rLink.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) rLink;
					String route = eElement.getAttribute("LinksTravelled");
					String demId = eElement.getAttribute("demandId");
					linkId = route.split("-");
					// SS
					// SS for DCF implementation (place the 80 Km DCF module DCF
					// at the end of a link if, for the next link dispersion is
					// going beyond 1360 ps/nm-km) calculate dispersion for
					// individual span, then estimate the disp at the end of
					// each link

					String[] linktype = new String[linkId.length];
					String[] linkDisp = new String[linkId.length];
					double[] mult = new double[linkId.length];
					double count1 = 0.0;
					double count2 = 0.0;
					double count3 = 0.0;
					for (i = 0; i < linkId.length; i++) {
						INT = Integer.parseInt(linkId[i]);
						linktype[i] = fiberType[INT];
						if (linktype[i].equals("SM")) {
							linkDisp[i] = "17";
						} else if (linktype[i].equals("NZ")) {
							linkDisp[i] = "10";
						}
						mult[i] += Integer.parseInt(linkDisp[i])
								* Double.parseDouble(TOPO_INFO[INT][3]);
					}
					count1 = mult[0];
					// SS for DCF implementation (End)
					// SS ROADM Insertion loss at Add port
					i = 0;
					INT = Integer.parseInt(linkId[i]);
					INT_TO_BE_PASSED = Integer.parseInt(TOPO_INFO[INT][1]);

					n3Loss = getNode3Loss(
							NODE_INFO[INT_TO_BE_PASSED][2].toUpperCase(),
							Integer.parseInt(NODE_INFO[INT_TO_BE_PASSED][3]));
					bw.newLine();
					bw.write("Ra" + " " + (count + (i + 1 + j)) + " "
							+ (count + (i + 2 + j)) + " " + "NODE_ADD NLOSS="
							+ n3Loss);
					bw.newLine();
					/*
					 * if (mult[0] >= Disp_80Km) { bw.write("D" + (x + 1) + " "
					 * + (count + i + 2 + j) + " " + (count + i + 3 + j) + " " +
					 * "DISP_30Km"); bw.newLine(); count1 = mult[0] -
					 * Disp_30KmNegLimit; x = x + 1; j = j + 1; }
					 */
					bw.write("Ab" + " " + (count + i + 2 + j) + " "
							+ (count + i + 3 + j) + " " + "EDFA_BOOSTER");
					System.out.println("\nRoute info");
					calcPointMap.put("" + (count + i + 1 + j) + "",
							new Triple<>((NODE_INFO[INT_TO_BE_PASSED][0]), ""
									+ INT + "", "Tx_ADD"));
					calcPointMap.put("" + (count + i + 2 + j) + "",
							new Triple<>((NODE_INFO[INT_TO_BE_PASSED][0]), ""
									+ INT + "", "Tx"));
					key_previous = -n3Loss + Booster_gain;
					for (i = 0; i < linkId.length; i++) // length of the route
														// in terms of ID
					{
						INT = Integer.parseInt(linkId[i]);
						INT_TO_BE_PASSED = Integer.parseInt(TOPO_INFO[INT][2]);
						Length = Double.parseDouble(TOPO_INFO[INT][3]); // SS

						System.out.println(INT_TO_BE_PASSED);
						// if (mult[0] >= Disp_80Km)

						// else

						if (fiberType[INT].equals("SM")) {
							Length_Loss = Length * Alpha_SM;
							copy = key_previous + Length_Loss;
							// if(copy<LineAmp_min_ip){}
							while (copy < LineAmp_min_ip) {

								Length_Loss = LineAmp_min_ip - key_previous;
								Length1 = Length_Loss / Alpha_SM;
								LengthArray.add(Length1);
								bw.newLine();
								bw.write("L" + (m + 1) + " "
										+ (count + (i + 3 + j)) + " "
										+ (count + (i + 4 + j)) + " "
										+ "OPTLINK_SM LEN=" + Length1);
								bw.newLine();
								key_previous += Length1 * Alpha_SM;
								bw.write("A" + (l + 1) + " "
										+ (count + i + 4 + j) + " "
										+ (count + i + 5 + j) + " "
										+ "EDFA_LINE");

								Dispersion += Length1 * Disp_SM;
								copy += LineAmp_gain;
								key_previous = LineAmp_min_ip + LineAmp_gain;
								j++;
								m++;
								l++;
								/*if (i != (linkId.length - 1))
									Dispersion_comp();
								else if (Dispersion > Disp_60Km)
									Dispersion_comp();*/
								Length -= Length1;
								count++; 

							}

							bw.newLine();
							bw.write("L" + (m + 1) + " "
									+ (count + (i + 3 + j)) + " "
									+ (count + (i + 4 + j)) + " "
									+ "OPTLINK_SM LEN=" + Length);
							LengthArray.add(Length);
							key_previous += Length * Alpha_SM;
							Dispersion += Length * Disp_SM;
							m++;
							if (Dispersion > Disp_60Km)
								Dispersion_comp();

						} // ss
						else if (fiberType[INT].equals("NZ")) {
							Length_Loss = Length * Alpha_NZ;
							copy = key_previous + Length_Loss;
							// if(copy<LineAmp_min_ip){}
							while (copy + LineAmp_gain < LineAmp_min_ip) {
								Length_Loss = LineAmp_min_ip - key_previous;
								Length1 = Length_Loss / Alpha_NZ;
								LengthArray.add(Length1);
								bw.newLine();
								bw.write("L" + (m + 1) + " "
										+ (count + (i + 3 + j)) + " "
										+ (count + (i + 4 + j)) + " "
										+ "OPTLINK_NZ LEN=" + Length1);
								key_previous += Length1 * Alpha_NZ;
								bw.newLine();
								bw.write("A" + (l + 1) + " "
										+ (count + i + 4 + j) + " "
										+ (count + i + 5 + j) + " "
										+ "EDFA_LINE");

								Dispersion += Length1 * Disp_NZ;
								key_previous = LineAmp_min_ip + LineAmp_gain;
								j++;
								l++;
								m++;
								Dispersion_comp();
								Length -= Length1;
								count++;
							}
							bw.newLine();
							bw.write("L" + (m + 1) + " "
									+ (count + (i + 3 + j)) + " "
									+ (count + (i + 4 + j)) + " "
									+ "OPTLINK_NZ LEN=" + Length);
							LengthArray.add(Length);
							key_previous += Length * Alpha_NZ;
							Dispersion += Length * Disp_NZ;
							x++;
							m++;

						}

						if (i != (linkId.length - 1)) {
							if (NODE_INFO[INT_TO_BE_PASSED][2]
									.equalsIgnoreCase("EDFA")) {
								Dispersion_comp();
								bw.newLine();
								bw.write("A" + (l + 1) + " "
										+ (count + i + 4 + j) + " "
										+ (count + i + 5 + j) + " "
										+ "EDFA_LINE");
								j++;
								l++;
								key_previous += LineAmp_gain;
							} else if (NODE_INFO[INT_TO_BE_PASSED][2]
									.equalsIgnoreCase("ROADM")) {
								Dispersion_comp();
								nLoss = getNode1Loss(
										NODE_INFO[INT_TO_BE_PASSED][2]
												.toUpperCase(),
										Integer.parseInt(NODE_INFO[INT_TO_BE_PASSED][3]));
								nNoise = getNodeNoise(
										NODE_INFO[INT_TO_BE_PASSED][2]
												.toUpperCase(),
										Integer.parseInt(NODE_INFO[INT_TO_BE_PASSED][3]));
								if (key_previous - nLoss < LineAmp_min_ip) {
									bw.newLine();
									bw.write("A" + (l + 1) + " "
											+ (count + i + 4 + j) + " "
											+ (count + i + 5 + j) + " "
											+ "EDFA_LINE");
									j++;
									l++;
									key_previous += LineAmp_gain;
								}
								if (NODE_INFO[INT_TO_BE_PASSED][3] == "2")// ss
								{
									bw.newLine();
									bw.write("R" + (i + 1) + " "
											+ (count + i + 4 + j) + " "
											+ (count + i + 5 + j) + " "
											+ "NODE1 NLOSS=" + nLoss);

								} else if (NODE_INFO[INT_TO_BE_PASSED][3] == "4")// ss
								{
									bw.newLine();
									bw.write("R" + (i + 1) + " "
											+ (count + i + 4 + j) + " "
											+ (count + i + 5 + j) + " "
											+ "NODE2 NLOSS=" + nLoss);
								} else if (NODE_INFO[INT_TO_BE_PASSED][3] == "8")// ss
								{
									bw.newLine();
									bw.write("R" + (i + 1) + " "
											+ (count + i + 4 + j) + " "
											+ (count + i + 5 + j) + " "
											+ "NODE3 NLOSS=" + nLoss);
								}
								key_previous -= nLoss;
								j++;
								// SS
								/*
								 * bw.newLine(); // ss
								 * 
								 * bw.write("A" + (i + 2) + " " + (count + i + 5
								 * + j) + " " + (count + i + 6 + j) + " " +
								 * "EDFA"); j = j + 1;// ss
								 */
								// for now it is considered that NODE type is
								// only ROADM
							}

							calcPointMap.put("" + (count + i + 3 + j) + "",
									new Triple<>(
											(NODE_INFO[INT_TO_BE_PASSED][0]),
											"" + INT + "", "Rx"));
							calcPointMap.put("" + (count + i + 4 + j) + "",
									new Triple<>(
											(NODE_INFO[INT_TO_BE_PASSED][0]),
											"" + INT + "", "Tx"));
						}
					}

					n2Loss = getNode2Loss(
							NODE_INFO[INT_TO_BE_PASSED][2].toUpperCase(),
							Integer.parseInt(NODE_INFO[INT_TO_BE_PASSED][3]));
					bw.newLine();
					bw.write("Ap" + " " + (count + i + 3 + j) + " "
							+ (count + i + 4 + j) + " " + "EDFA_PRE");
					bw.newLine();

					bw.write("Rd" + " " + (count + (i + 4 + j)) + " "
							+ (count + (i + 5 + j)) + " " + "NODE_DROP NLOSS="
							+ n2Loss);
					calcPointMap.put("" + (count + i + 4 + j) + "",
							new Triple<>((NODE_INFO[INT_TO_BE_PASSED][0]), ""
									+ INT + "", "Rx"));
					calcPointMap.put("" + (count + i + 5 + j) + "",
							new Triple<>((NODE_INFO[INT_TO_BE_PASSED][0]), ""
									+ INT + "", "Rx_DROP"));
					bw.newLine();
					bw.newLine();
					bw.write("Pin 1 0 0");

					bw.newLine();
					bw.write(".optqpt");
					bw.newLine();
					bw.newLine();
					bw.write(".MODEL OPTLINK_SM L(ALPHA=0.27 D=-17.0)");
					bw.newLine();
					bw.write(".MODEL OPTLINK_NZ L(ALPHA=0.22 D=-10.0)"); // ss
					bw.newLine();
					bw.write(".MODEL EDFA_BOOSTER A(G=23 NF=5.5 LOSS =1)");
					bw.newLine();
					bw.write(".MODEL EDFA_LINE A(G=20 NF=5 LOSS =1)");
					bw.newLine();
					bw.write(".MODEL EDFA_PRE A(G=20 NF=6 LOSS =1)");
					bw.newLine();
					bw.write(".MODEL NODE_ADD R(NLOSS=" + n3Loss + ")");
					bw.newLine();
					bw.write(".MODEL NODE1 R(NLOSS=14)");
					bw.newLine();
					bw.write(".MODEL NODE2 R(NLOSS=16)");
					bw.newLine();
					bw.write(".MODEL NODE3 R(NLOSS=18)");
					bw.newLine();
					bw.write(".MODEL NODE_DROP R(NLOSS=" + n2Loss + ")");
					bw.newLine();
					bw.write(".MODEL DISP_30Km D(PL=4 DC=510)"); // ss
					bw.newLine();
					bw.write(".MODEL DISP_60Km D(PL=4 DC=1020)"); // ss
					bw.newLine();
					bw.write(".MODEL DISP_80Km D(PL=4 DC=1360)");
					bw.newLine();
					bw.write(".MODEL DISP_120Km D(PL=4 DC=2040)"); // ss// ss
					bw.newLine();
					bw.newLine();
					bw.newLine();
					bw.write(".end");
					bw.close();
					String arg = cirFilePath + "/outputfile_linear_dwdm.cir"; // comment

					// for debugging
					OpticalSimulator.resetSimulator();
					OpticalSimulator optsim = OpticalSimulator.getSimulator()
							.simulate(arg);

					list = new ArrayList<String>(optsim.ModelNames.keySet());// ss
					list.add(0, "i/p");
					optsim.ModelNames.clear();

					for (n = 0; n < list.size(); n++)

						optsim.ModelNames.put(list.get(n), (10 * Math
								.log10((double) optsim.nwk.powerMap.get(n))));
					character = (double) (optsim.nwk.powerMap
							.get(optsim.nwk.powerMap.size() - 1));
					System.out.println("\n" + optsim.ModelNames);

					Element optpath = doc_to_write.createElement("Path");
					optpath.setAttribute("demandId", demId);
					optpath.setAttribute("routeId", route);
					optpath.setAttribute("source",
							DEMAND_INFO[Integer.parseInt(demId)][1]);
					optpath.setAttribute("destination",
							DEMAND_INFO[Integer.parseInt(demId)][2]);

					keySet = new ArrayList<String>(calcPointMap.keySet());
					keySetToInt = new ArrayList<Integer>(keySet.size());
					for (String myInt : keySet) {
						keySetToInt.add(Integer.parseInt(myInt));
					}
					Collections.sort(keySetToInt);
					ArrayList<Pair<String, String>> ListValue = new ArrayList<Pair<String, String>>();
					int m = 0;
					for (k = 0; k < keySetToInt.size(); k++) {
						ListValue.add(new Pair<>((String.valueOf(keySetToInt
								.get(k))), (String.valueOf(keySetToInt
								.get(k + 1)))));
						k += 1;
					}

					for (int k = 0; k < ListValue.size(); k++) {
						Pair dummi = ListValue.get(k);
						optsim.Model_Node.values().remove(dummi);
					}
					System.out.println(optsim.Model_Node);
					ArrayList<String> ListKey = new ArrayList<String>(
							optsim.Model_Node.keySet());

					ArrayList<Pair<String, String>> List = new ArrayList(
							optsim.Model_Node.values());

					for (k = 0; k < optsim.nwk.wrk.getSize(); k++) {
						Element calcPoint = (Element) getInfo(doc_to_write,
								optsim.nwk.nodeNames.get(k),
								optsim.nwk.getPowerMap(k),
								optsim.nwk.getNoiseMap(k),
								optsim.nwk.getDispersionMap(k));
						// Set keySet = calcPointMap.keySet();

						if (keySet.contains(optsim.nwk.nodeNames.get(k))) {
							Triple<String, String, String> tempTriple = calcPointMap
									.get(optsim.nwk.nodeNames.get(k));
							String nodeIdForXML = tempTriple.getFirst();
							String linkIdForXML = tempTriple.getSecond();
							String measurementTypeForXML = tempTriple
									.getThird();
							calcPoint.setAttribute("nodeID", nodeIdForXML);
							calcPoint.setAttribute("linkID", linkIdForXML);
							calcPoint.setAttribute("measurementType",
									measurementTypeForXML);
							calcPoint
									.setAttribute("Customer_Defined_Node_Type",
											NODE_INFO[Integer
													.parseInt(nodeIdForXML)][2]);

						}
						if (!(k == optsim.nwk.wrk.getSize() - 1)) {
							Pair<String, String> List2 = List.get(m);
							if (k == Integer.parseInt(List2.getSecond()) - 1) {

								if (ListKey.get(m).charAt(0) == 'A') {
									calcPoint.setAttribute(
											"Intermediate_Node_Names",
											"Amplifier");
									if (ListKey.get(m).charAt(1) == 'b')
										calcPoint.setAttribute("Amp_Type",
												"Booster");
									else if (ListKey.get(m).charAt(1) == 'p')
										calcPoint.setAttribute("Amp_Type",
												"PreAmp");
									else
										calcPoint.setAttribute("Amp_Type",
												"LineAmp");
								} else if (ListKey.get(m).charAt(0) == 'L') {
									calcPoint.setAttribute(
											"Intermediate_Node_Names",
											"FiberLink");
									calcPoint.setAttribute("Link_Length",
											String.valueOf(LengthArray.get(b)));
									b++;
								} else if (ListKey.get(m).charAt(0) == 'D') {
									calcPoint.setAttribute(
											"Intermediate_Node_Names", "DCM");
									calcPoint.setAttribute("DCM_type",
											DCMArray.get(d));
									d++;
								}
								m++;// SS

							}
						}

						optpath.appendChild(calcPoint);
						rootElement.appendChild(optpath);

					}

				}

			}
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			javax.xml.transform.Transformer transformer = transformerFactory
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc_to_write);
			StreamResult result = new StreamResult(new File(xmlFilePath
					+ "/Calculations_Output.xml"));
			transformer.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// SS ROADM modeling (Set the Loss through ROADM nodes based on the ROADM
	// Degree. The loss from A/D to line and line to A/D vary.
	// losses at both the points are summed here, representing the total loss
	// through the ROADM unit)
	private static double getNodeNoise(String typ, int deg) {
		nNoise = 0;
		return nNoise;
	}

	// SS Local Add loss (From Add port to the line)
	private static double getNode3Loss(String typ3, int deg3) {
		switch (typ3) {
		case "ROADM": // C/L ROADM loss + Degree ROADM loss
			if (deg3 == 2) // SS
				n3Loss = 14; // 7+7
			else if (deg3 == 4)
				n3Loss = 14; // 7+7
			else if (deg3 == 8)
				n3Loss = 14; // 7+7
		}
		return n3Loss;
	}

	// SS ExpressLoss between two units
	private static double getNode1Loss(String typ, int deg) {
		switch (typ) {
		case "ROADM":
			if (deg == 2)
				nLoss = 15; // 7+8
			else if (deg == 4)
				nLoss = 16; // 7+9
			else if (deg == 8)
				nLoss = 18; // 7+11
		}
		return nLoss;
	}

	// SS Local Drop loss (From line to drop port)
	private static double getNode2Loss(String typ2, int deg2) {
		switch (typ2) {
		case "ROADM":
			if (deg2 == 2)
				n2Loss = 15;
			else if (deg2 == 4)
				n2Loss = 16;
			else if (deg2 == 8)
				n2Loss = 18;
		}
		return n2Loss;
	}

	// SS ROADM modeling (End)

	/*
	 * private static double ROADMLoss(int deg) { nLoss = Math.pow(10, deg);
	 * return nLoss; }
	 */
	private static Node getInfo(Document doc_to_write, String calPoint,
			double power, double noise, double dispersion) {
		Element calc = doc_to_write.createElement("Calc");
		double power_value = (10 * Math.log10(power));
		double osnr_value = (10 * Math.log10(power / noise));
		calc.setAttribute("CalcPoint", calPoint);
		calc.setAttribute("Power_in_dBm", String.valueOf(power_value));
		calc.setAttribute("OSNR_in_dB", String.valueOf(osnr_value));
		calc.setAttribute("Disp_in_ps_per_nm", String.valueOf(dispersion));
		return calc;
	}

	private static void Dispersion_comp() {
		try {
			if ((Dispersion < (Disp_60Km + Disp_80Km) / 2 && Dispersion > (Disp_60Km + Disp_30Km) / 2)) {
				Dispersion -= Disp_60Km;

				if (key_previous + Disp_60_Loss < LineAmp_min_ip) {
					bw.newLine();
					bw.write("A" + (l + 1) + " " + (count + i + 4 + j) + " "
							+ (count + i + 5 + j) + " " + "EDFA_LINE");
					key_previous += LineAmp_gain;
					j++;
					l++;
				}
				bw.newLine();
				bw.write("D" + (x + 1) + " " + (count + i + 4 + j) + " "
						+ (count + i + 5 + j) + " " + "DISP_60Km");
				j++;
				x++;
				DCMArray.add("DCM60km");
				key_previous += Disp_60_Loss;
			}

			else if (Dispersion < (Disp_80Km + Disp_120Km) / 2
					&& Dispersion > (Disp_80Km + Disp_60Km) / 2) {
				Dispersion -= Disp_80Km;
				if (key_previous + Disp_80_Loss < LineAmp_min_ip) {
					bw.newLine();
					bw.write("A" + (l + 1) + " " + (count + i + 4 + j) + " "
							+ (count + i + 5 + j) + " " + "EDFA_LINE");
					key_previous += LineAmp_gain;
					j++;
					l++;
				}
				bw.newLine();
				bw.write("D" + (x + 1) + " " + (count + i + 4 + j) + " "
						+ (count + i + 5 + j) + " " + "DISP_80Km");
				j++;
				x++;
				DCMArray.add("DCM80km");
				key_previous += Disp_80_Loss;
			} else if (Dispersion >= (Disp_120Km + Disp_80Km) / 2) {
				Dispersion -= Disp_120Km;
				if (key_previous + Disp_120_Loss < LineAmp_min_ip) {
					bw.newLine();
					bw.write("A" + (l + 1) + " " + (count + i + 4 + j) + " "
							+ (count + i + 5 + j) + " " + "EDFA_LINE");
					key_previous += LineAmp_gain;
					j++;
					l++;
				}
				bw.newLine();
				bw.write("D" + (x + 1) + " " + (count + i + 4 + j) + " "
						+ (count + i + 5 + j) + " " + "DISP_120Km");
				key_previous += Disp_120_Loss;
				j++;
				x++;
				DCMArray.add("DCM120km");
				while(!(Dispersion<= Disp_60Km)){
					bw.newLine();
					bw.write("D" + (x + 1) + " " + (count + i + 4 + j) + " "
							+ (count + i + 5 + j) + " " + "DISP_120Km");
					key_previous += Disp_120_Loss;
					Dispersion-= Disp_120Km;
					DCMArray.add("DCM120km");
					j++;
					x++;
					
				}
			}

			Disp_count++;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void Model() {
		String model = list.get(n);
		if (model.charAt(0) == 'A') {
			list.set(n, "Amplifier");
		}
	}	
}
