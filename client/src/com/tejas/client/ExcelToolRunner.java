package com.tejas.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;

import com.tejas.eda.spice.DWDMRouteToSpiceConverter;
import com.tejas.engine.CLINet2Plan;
import com.tejas.engine.libraries.ConvertNumbersToWavelength;
import com.tejas.engine.libraries.NodePresent;

public class ExcelToolRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String excelFilePath = "excelFiles";
		String inputExcelFileName = excelFilePath + "/Input.xlsx";
		String outputExcelFileName = excelFilePath + "/Output.xlsx";
		String baseFileName = "";
		try {
			baseFileName = createXMLFromExcel(inputExcelFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String path = "xmlFiles";
		String inputfilename = path + "/" + baseFileName + "_input.xml";
		String outputfilename = path + "/" + baseFileName + "_output.xml.n2p";
		String[] cmdArgs = {
				"--mode",
				"net-design",
				"--class-file",
				"lib/algorithms.jar",
				"--class-name",
				"com.tejas.workspace.examples.netDesignAlgorithm.fa.WorkBackUpPathWithRisk",
				"--input-file", inputfilename, "--output-file", outputfilename };

		CLINet2Plan cliObj = new CLINet2Plan(cmdArgs);
		String[] dwdmArgs = { outputfilename };

		DWDMRouteToSpiceConverter dwdmObj = new DWDMRouteToSpiceConverter(
				dwdmArgs);
		try {
			createExcelFromXML(path + "/" + baseFileName, outputExcelFileName);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		}

		System.out.println("Please look at Output.xlsx for details");
	}

	private static String createXMLFromExcel(String inputExcelFileName)
			throws IOException {
		FileInputStream file = new FileInputStream(new File(inputExcelFileName));
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		XSSFSheet siteSheet = workbook.getSheet("Sites");
		XSSFSheet linkSheet = workbook.getSheet("Links");
		XSSFSheet trafficSheet = workbook.getSheet("Traffic");

		Document doc = new Document();
		Element root = new Element("network");
		doc.setRootElement(root);

		String pt = "physicalTopology";
		String n = "node";
		String l = "link";

		Element phyTopology = new Element(pt);
		root.addContent(phyTopology);

		// Map to store the site name to ID mapping
		Map siteNameToIDMap = new HashMap();

		// Writing node information
		Iterator rowIterator = siteSheet.iterator();
		boolean rowTitleFlag = true;
		int nodeIDCount = 0;
		while (rowIterator.hasNext()) {

			Row row = (Row) rowIterator.next();

			if (rowTitleFlag) {
				rowTitleFlag = false;
				continue;
			}

			row.getCell(0).setCellType(1);
			row.getCell(1).setCellType(1);
			row.getCell(2).setCellType(1);
			// row.getCell(3).setCellType(1);

			// Double id = row.getCell(0).;
			// String sid = id.toString();

			siteNameToIDMap.put(row.getCell(1).getStringCellValue(),
					nodeIDCount);

			Element node = new Element(n);
			Attribute name = new Attribute("name", row.getCell(1)
					.getStringCellValue());
			node.setAttribute(name);
			Attribute no = new Attribute("no", "" + nodeIDCount + "");
			node.setAttribute(no);
			Attribute nodeIDfromSheet = new Attribute("nodeIDFromSheet", row
					.getCell(0).getStringCellValue());
			node.setAttribute(nodeIDfromSheet);
			String nodeType = "ROADM";
			if (!row.getCell(3).getStringCellValue().equals("")) {
				nodeType = row.getCell(3).getStringCellValue();
			}
			Attribute type = new Attribute("type", nodeType);
			node.setAttribute(type);
			Attribute abbr = new Attribute("abbr", row.getCell(2)
					.getStringCellValue());
			node.setAttribute(abbr);

			phyTopology.addContent(node);
			nodeIDCount++;
		}

		// Writing link information
		int linkIDCount = 0;
		rowTitleFlag = true;
		rowIterator = linkSheet.iterator();
		while (rowIterator.hasNext()) {
			Row row = (Row) rowIterator.next();

			if (rowTitleFlag) {
				rowTitleFlag = false;
				continue;
			}

			row.getCell(0).setCellType(1);
			row.getCell(1).setCellType(1);
			row.getCell(2).setCellType(1);
			row.getCell(3).setCellType(1);
			row.getCell(4).setCellType(1);

			Element link = new Element(l);
			
			Attribute name = new Attribute("name", row.getCell(1)
					.getStringCellValue()
					+ " <--> "
					+ row.getCell(2).getStringCellValue());
			link.setAttribute(name);
			Attribute no = new Attribute("sublink", "" + linkIDCount + "");
			link.setAttribute(no);
			Attribute linkIDFromSheet = new Attribute("linkIDFromSheet", row
					.getCell(0).getStringCellValue());
			link.setAttribute(linkIDFromSheet);
			Attribute source = new Attribute("originNodeId", siteNameToIDMap
					.get(row.getCell(1).getStringCellValue()).toString());
			link.setAttribute(source);
			Attribute destination = new Attribute("destinationNodeId",
					siteNameToIDMap.get(row.getCell(2).getStringCellValue())
							.toString());
			link.setAttribute(destination);
			Attribute linkCapacityInErlangs = new Attribute(
					"linkCapacityInErlangs", "50.0");
			link.setAttribute(linkCapacityInErlangs);
			Attribute linktype = new Attribute("linktype", row.getCell(3)
					.getStringCellValue());
			link.setAttribute(linktype);
			String lengthInKm = "40.0";
			Attribute length = new Attribute("linkLengthInKm", row.getCell(4)
					.getStringCellValue());
			link.setAttribute(length);

			phyTopology.addContent(link);

			// Adding another link in the reverse direction as N2P considers
			// every link as uni-directional by default
			link = new Element(l);

			name = new Attribute("name", row.getCell(2).getStringCellValue()
					+ " <--> " + row.getCell(1).getStringCellValue());
			link.setAttribute(name);
			no = new Attribute("sublink", "" + linkIDCount + "");
			link.setAttribute(no);
			linkIDFromSheet = new Attribute("linkIDFromSheet", row.getCell(0)
					.getStringCellValue());
			link.setAttribute(linkIDFromSheet);
			source = new Attribute("originNodeId", siteNameToIDMap.get(
					row.getCell(2).getStringCellValue()).toString());
			link.setAttribute(source);
			destination = new Attribute("destinationNodeId", siteNameToIDMap
					.get(row.getCell(1).getStringCellValue()).toString());
			link.setAttribute(destination);
			linkCapacityInErlangs = new Attribute("linkCapacityInErlangs",
					"50.0");
			link.setAttribute(linkCapacityInErlangs);
			linktype = new Attribute("linktype", row.getCell(3)
					.getStringCellValue());
			link.setAttribute(linktype);
			lengthInKm = "40.0";
			length = new Attribute("linkLengthInKm", row.getCell(4)
					.getStringCellValue());
			link.setAttribute(length);

			phyTopology.addContent(link);
			linkIDCount++;
		}

		// Add traffic demand entries into the xml file
		String td = "demandSet";
		String de = "demandEntry";
		Element demandSet = new Element(td);
		root.addContent(demandSet);

		// Adding traffic information
		int trafficIDCount = 0;
		rowTitleFlag = true;
		rowIterator = trafficSheet.iterator();
		while (rowIterator.hasNext()) {
			Row row = (Row) rowIterator.next();

			if (rowTitleFlag) {
				rowTitleFlag = false;
				continue;
			}

			row.getCell(0).setCellType(1);
			row.getCell(1).setCellType(1);
			row.getCell(2).setCellType(1);
			row.getCell(3).setCellType(1);
			row.getCell(4).setCellType(1);
			row.getCell(5).setCellType(1);
			row.getCell(6).setCellType(1);
			row.getCell(7).setCellType(1);
			row.getCell(8).setCellType(1);

			Double trafficSTM16 = 0.0;
			Double trafficSTM64 = 0.0;
			Double trafficGE = 0.0;
			Double traffic10GE = 0.0;
			Double traffic40GE = 0.0;

			String sTrafficSTM16 = row.getCell(3).getStringCellValue();
			if (!sTrafficSTM16.equals("")) {
				trafficSTM16 = Double.parseDouble(sTrafficSTM16);
			}

			String sTrafficSTM64 = row.getCell(4).getStringCellValue();
			if (!sTrafficSTM64.equals("")) {
				trafficSTM64 = Double.parseDouble(sTrafficSTM64);
			}

			String sTrafficGE = row.getCell(5).getStringCellValue();
			if (!sTrafficGE.equals("")) {
				trafficGE = Double.parseDouble(sTrafficGE);
			}

			String sTraffic10GE = row.getCell(6).getStringCellValue();
			if (!sTraffic10GE.equals("")) {
				traffic10GE = Double.parseDouble(sTraffic10GE);
			}

			String sTraffic40GE = row.getCell(7).getStringCellValue();
			if (!sTraffic40GE.equals("")) {
				traffic40GE = Double.parseDouble(sTraffic40GE);
			}

			String protectionRequirement = row.getCell(8).getStringCellValue();

			Double totalTrafficRequirement = trafficSTM16 * 2.5 + trafficSTM64
					* 10 + trafficGE * 1.25 + traffic10GE * 10 + traffic40GE
					* 40;

			Element demandEntry = new Element(de);

			Attribute trafficIDFromSheet = new Attribute("trafficIDFromSheet",
					row.getCell(0).getStringCellValue());
			demandEntry.setAttribute(trafficIDFromSheet);

			Attribute ingressNodeId = new Attribute("ingressNodeId",
					siteNameToIDMap.get(row.getCell(1).getStringCellValue())
							.toString());
			demandEntry.setAttribute(ingressNodeId);

			Attribute egressNodeId = new Attribute("egressNodeId",
					siteNameToIDMap.get(row.getCell(2).getStringCellValue())
							.toString());
			demandEntry.setAttribute(egressNodeId);

			Attribute aTrafficSTM16 = new Attribute("trafficSTM16",
					sTrafficSTM16);
			demandEntry.setAttribute(aTrafficSTM16);

			Attribute aTrafficSTM64 = new Attribute("trafficSTM64",
					sTrafficSTM64);
			demandEntry.setAttribute(aTrafficSTM64);

			Attribute aTrafficGE = new Attribute("trafficGE", sTrafficGE);
			demandEntry.setAttribute(aTrafficGE);

			Attribute aTraffic10GE = new Attribute("traffic10GE", sTraffic10GE);
			demandEntry.setAttribute(aTraffic10GE);

			Attribute aTraffic40GE = new Attribute("traffic40GE", sTraffic40GE);
			demandEntry.setAttribute(aTraffic40GE);

			Attribute offeredTrafficInErlangs = new Attribute(
					"offeredTrafficInErlangs",
					totalTrafficRequirement.toString());
			demandEntry.setAttribute(offeredTrafficInErlangs);

			Attribute aProtectionRequirement = new Attribute(
					"protectionRequirement", protectionRequirement);
			demandEntry.setAttribute(aProtectionRequirement);

			demandSet.addContent(demandEntry);
		}

		file.close();

		String path = "tempfiles";
		// if (savetype.equals("tempsave")){
		// path = "tempfiles";
		// } else if (savetype.equals("saveforsim")){
		path = "xmlFiles";
		// }
		String filename = "filename_" + System.currentTimeMillis();

		// Creates the Parent XML file.
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		try {
			outputter.output(doc, new FileWriter(path + "/" + filename
					+ "_input.xml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return filename;
	}

	private static void createExcelFromXML(String baseFileName,
			String outputExcelFileName) throws IOException, JDOMException {
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet siteSheet = workbook.createSheet("Sites");
		XSSFSheet linkSheet = workbook.createSheet("Links");
		XSSFSheet trafficSheet = workbook.createSheet("Traffic");
		XSSFSheet pathSheet = workbook.createSheet("Paths");
		XSSFSheet linkEngineeringSheet = workbook
				.createSheet("LinkEngineering");
		XSSFSheet billOfMaterials = workbook.createSheet("BillOfMaterials");

		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(baseFileName + "_output.xml.n2p");

		Document document = (Document) builder.build(xmlFile);
		Element network = document.getRootElement();
		Element phyTopology = network.getChild("physicalTopology");
		Element demandSet = network.getChild("demandSet");
		Element routingInfo = network.getChild("routingInfo");

		// Read site information from the output xml file and populate siteSheet
		Row titleRow = siteSheet.createRow(0);
		Cell cell = titleRow.createCell(0);
		cell.setCellValue("ID");
		cell = titleRow.createCell(1);
		cell.setCellValue("Site Name");
		cell = titleRow.createCell(2);
		cell.setCellValue("Site Name Abbreviation");
		cell = titleRow.createCell(3);
		cell.setCellValue("Site Type");
		cell = titleRow.createCell(4);
		cell.setCellValue("Line Amplifiers");

		int nSize = phyTopology.getChildren("node").size();

		for (int i = 1; i <= nSize; i++) {
			String expression = "/network/physicalTopology/node[@nodeIDFromSheet="
					+ i + "]/@name";
			String name = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/node[@nodeIDFromSheet=" + i
					+ "]/@abbr";
			String abbr = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/node[@nodeIDFromSheet=" + i
					+ "]/@type";
			String type = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");

			Row row = siteSheet.createRow(i);
			cell = row.createCell(0);
			cell.setCellValue(i);
			cell = row.createCell(1);
			cell.setCellValue(name);
			cell = row.createCell(2);
			cell.setCellValue(abbr);
			cell = row.createCell(3);
			cell.setCellValue(type);
		}

		// Read link information from the output xml file and populate linkSheet
		titleRow = linkSheet.createRow(0);
		cell = titleRow.createCell(0);
		cell.setCellValue("ID");
		cell = titleRow.createCell(1);
		cell.setCellValue("Source");
		cell = titleRow.createCell(2);
		cell.setCellValue("Destination");
		cell = titleRow.createCell(3);
		cell.setCellValue("Type");
		cell = titleRow.createCell(4);
		cell.setCellValue("Distance");

		int lSize = phyTopology.getChildren("link").size();

		for (int i = 1; i <= lSize / 2; i++) {
			String expression = "/network/physicalTopology/link[@linkIDFromSheet="
					+ i + "]/@originNodeId";
			String sourceID = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/node[@no=" + sourceID
					+ "]/@name";
			String sourceName = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/link[@linkIDFromSheet=" + i
					+ "]/@destinationNodeId";
			String destID = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/node[@no=" + destID
					+ "]/@name";
			String destName = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/link[@linkIDFromSheet=" + i
					+ "]/@linktype";
			String linktype = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/link[@linkIDFromSheet=" + i
					+ "]/@linkLengthInKm";
			String linkLengthInKm = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");

			Row row = linkSheet.createRow(i);
			cell = row.createCell(0);
			cell.setCellValue(i);
			cell = row.createCell(1);
			cell.setCellValue(sourceName);
			cell = row.createCell(2);
			cell.setCellValue(destName);
			cell = row.createCell(3);
			cell.setCellValue(linktype);
			cell = row.createCell(4);
			cell.setCellValue(linkLengthInKm);
		}

		// Read traffic information from the output xml file and populate
		// trafficSheet
		titleRow = trafficSheet.createRow(0);
		cell = titleRow.createCell(0);
		cell.setCellValue("ID");
		cell = titleRow.createCell(1);
		cell.setCellValue("Source");
		cell = titleRow.createCell(2);
		cell.setCellValue("Destination");
		cell = titleRow.createCell(3);
		cell.setCellValue("STM-16");
		cell = titleRow.createCell(4);
		cell.setCellValue("STM-64");
		cell = titleRow.createCell(5);
		cell.setCellValue("GE");
		cell = titleRow.createCell(6);
		cell.setCellValue("10GE");
		cell = titleRow.createCell(7);
		cell.setCellValue("40GE");
		cell = titleRow.createCell(8);
		cell.setCellValue("ProtectionType");

		int dSize = demandSet.getChildren("demandEntry").size();

		for (int i = 1; i <= dSize; i++) {
			String expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ i + "]/@ingressNodeId";
			String sourceID = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/node[@no=" + sourceID
					+ "]/@name";
			String sourceName = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ i + "]/@egressNodeId";
			String destID = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/node[@no=" + destID
					+ "]/@name";
			String destName = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ i + "]/@trafficSTM16";
			String trafficSTM16 = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ i + "]/@trafficSTM64";
			String trafficSTM64 = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ i + "]/@trafficGE";
			String trafficGE = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ i + "]/@traffic10GE";
			String traffic10GE = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ i + "]/@traffic40GE";
			String traffic40GE = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ i + "]/@protectionRequirement";
			String protectionType = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");

			Row row = trafficSheet.createRow(i);
			cell = row.createCell(0);
			cell.setCellValue(i);
			cell = row.createCell(1);
			cell.setCellValue(sourceName);
			cell = row.createCell(2);
			cell.setCellValue(destName);
			cell = row.createCell(3);
			cell.setCellValue(trafficSTM16);
			cell = row.createCell(4);
			cell.setCellValue(trafficSTM64);
			cell = row.createCell(5);
			cell.setCellValue(trafficGE);
			cell = row.createCell(6);
			cell.setCellValue(traffic10GE);
			cell = row.createCell(7);
			cell.setCellValue(traffic40GE);
			cell = row.createCell(8);
			cell.setCellValue(protectionType);
		}

		// Read route information from the output xml and populate pathSheet
		titleRow = pathSheet.createRow(0);
		cell = titleRow.createCell(0);
		cell.setCellValue("ID");
		cell = titleRow.createCell(1);
		cell.setCellValue("Source");
		cell = titleRow.createCell(2);
		cell.setCellValue("Destination");
		cell = titleRow.createCell(3);
		cell.setCellValue("STM-16");
		cell = titleRow.createCell(4);
		cell.setCellValue("STM-64");
		cell = titleRow.createCell(5);
		cell.setCellValue("GE");
		cell = titleRow.createCell(6);
		cell.setCellValue("10GE");
		cell = titleRow.createCell(7);
		cell.setCellValue("40GE");
		cell = titleRow.createCell(8);
		cell.setCellValue("ProtectionType");
		cell = titleRow.createCell(9);
		cell.setCellValue("Work Path");
		cell = titleRow.createCell(10);
		cell.setCellValue("Work Path Frequency");
		cell = titleRow.createCell(11);
		cell.setCellValue("Protect Path");
		cell = titleRow.createCell(12);
		cell.setCellValue("Protect Path Frequency");

		int rSize = routingInfo.getChildren("route").size();

		for (int i = 0; i < rSize; i++) {
			String expression = "/network/routingInfo/route[@routeId=" + i
					+ "]/@demandId";
			String demandID = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/demandSet/demandEntry[@DemandId=" + demandID
					+ "]/@trafficIDFromSheet";
			String trafficIDFromSheet = getValueFromXML(expression,
					baseFileName + "_output.xml.n2p");
			expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ trafficIDFromSheet + "]/@ingressNodeId";
			String sourceID = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/node[@no=" + sourceID
					+ "]/@name";
			String sourceName = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ trafficIDFromSheet + "]/@egressNodeId";
			String destID = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/node[@no=" + destID
					+ "]/@name";
			String destName = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ trafficIDFromSheet + "]/@trafficSTM16";

			List list = routingInfo.getChildren("route");
			Element node = (Element) list.get(i);

			// Attribute value ;
			String traffic40GE = "0";
			String traffic10GE = "0";
			String trafficGE = "0";
			String trafficSTM64 = "0";
			String trafficSTM16 = "0";

			if (node.getAttribute("traffic40GE") != null)
				traffic40GE = "1";
			if (node.getAttribute("traffic10GE") != null)
				traffic10GE = "1";
			if (node.getAttribute("traffic1GE") != null)
				trafficGE = "1";
			if (node.getAttribute("trafficSTM64") != null)
				trafficSTM64 = "1";
			if (node.getAttribute("trafficSTM16") != null)
				trafficSTM16 = "1";

			expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
					+ trafficIDFromSheet + "]/@protectionRequirement";
			String protectionType = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/routingInfo/route[@routeId=" + i
					+ "]/@LinksTravelled";
			String wLinksTravelled = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/routingInfo/route[@routeId=" + i
					+ "]/@seqWavelengths";
			String wSeqWavelengths = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");

			String[] wavelength = wSeqWavelengths.split(" ");
			wSeqWavelengths = ConvertNumbersToWavelength
					.convertToWavelength(wavelength);

			String[] wLinkArray = wLinksTravelled.split("-");
			String[] wNodeString = new String[(wLinkArray.length) + 1];

			for (int j = 0; j < wLinkArray.length; j++) {
				expression = "/network/physicalTopology/link[@LinkId="
						+ wLinkArray[j] + "]/@originNodeId";
				wNodeString[j] = getValueFromXML(expression, baseFileName
						+ "_output.xml.n2p");
			}
			expression = "/network/physicalTopology/link[@LinkId="
					+ wLinkArray[wLinkArray.length - 1]
					+ "]/@destinationNodeId";
			wNodeString[wLinkArray.length] = getValueFromXML(expression,
					baseFileName + "_output.xml.n2p");

			StringBuffer wsb = new StringBuffer();
			for (int j = 0; j < wNodeString.length - 1; j++) {
				expression = "/network/physicalTopology/node[@no="
						+ wNodeString[j] + "]/@name";
				wsb.append(getValueFromXML(expression, baseFileName
						+ "_output.xml.n2p"));
				wsb.append(",");
			}
			expression = "/network/physicalTopology/node[@no="
					+ wNodeString[wNodeString.length - 1] + "]/@name";
			wsb.append(getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p"));

			StringBuffer psb = new StringBuffer();
			psb.append("NotApplicable");
			String pSeqWavelengths = "NotApplicable";

			if (protectionType.equals("PreCalc-Restored")) {
				expression = "/network/routingInfo/route[@routeId=" + i
						+ "]/protectionSegmentEntry/@id";
				String protSegmentId = getValueFromXML(expression, baseFileName
						+ "_output.xml.n2p");
				expression = "/network/protectionInfo/protectionSegment[@id="
						+ protSegmentId + "]/@LinksTravelled";
				String pLinksTravelled = getValueFromXML(expression,
						baseFileName + "_output.xml.n2p");
				expression = "/network/protectionInfo/protectionSegment[@id="
						+ protSegmentId + "]/@seqWavelengths";
				pSeqWavelengths = getValueFromXML(expression, baseFileName
						+ "_output.xml.n2p");

				String wavelength_pro[] = pSeqWavelengths.split(" ");
				pSeqWavelengths = ConvertNumbersToWavelength.convertToWavelength(wavelength_pro);
				
				String[] pLinkArray = pLinksTravelled.split("-");

				String[] pNodeString = new String[(pLinkArray.length) + 1];

				for (int j = 0; j < pLinkArray.length; j++) {
					expression = "/network/physicalTopology/link[@LinkId="
							+ pLinkArray[j] + "]/@originNodeId";
					pNodeString[j] = getValueFromXML(expression, baseFileName
							+ "_output.xml.n2p");
				}
				expression = "/network/physicalTopology/link[@LinkId="
						+ pLinkArray[pLinkArray.length - 1]
						+ "]/@destinationNodeId";
				pNodeString[pLinkArray.length] = getValueFromXML(expression,
						baseFileName + "_output.xml.n2p");

				psb = new StringBuffer();
				for (int j = 0; j < pNodeString.length - 1; j++) {
					expression = "/network/physicalTopology/node[@no="
							+ pNodeString[j] + "]/@name";
					psb.append(getValueFromXML(expression, baseFileName
							+ "_output.xml.n2p"));
					psb.append(",");
				}
				expression = "/network/physicalTopology/node[@no="
						+ pNodeString[pNodeString.length - 1] + "]/@name";
				psb.append(getValueFromXML(expression, baseFileName
						+ "_output.xml.n2p"));
			}

			Row row = pathSheet.createRow(i + 1);
			cell = row.createCell(0);
			cell.setCellValue(i + 1);
			cell = row.createCell(1);
			cell.setCellValue(sourceName);
			cell = row.createCell(2);
			cell.setCellValue(destName);
			cell = row.createCell(3);
			cell.setCellValue(trafficSTM16);
			cell = row.createCell(4);
			cell.setCellValue(trafficSTM64);
			cell = row.createCell(5);
			cell.setCellValue(trafficGE);
			cell = row.createCell(6);
			cell.setCellValue(traffic10GE);
			cell = row.createCell(7);
			cell.setCellValue(traffic40GE);
			cell = row.createCell(8);
			cell.setCellValue(protectionType);
			cell = row.createCell(9);
			cell.setCellValue(wsb.toString());
			cell = row.createCell(10);
			cell.setCellValue(wSeqWavelengths);
			cell = row.createCell(11);
			cell.setCellValue(psb.toString());
			cell = row.createCell(12);
			cell.setCellValue(pSeqWavelengths);
		}

		
		// creating bill of materials sheet.
		titleRow = billOfMaterials.createRow(0);
		cell = titleRow.createCell(0);
		cell.setCellValue("Node");
		cell = titleRow.createCell(1);
		cell.setCellValue("Type");
		cell = titleRow.createCell(2);
		cell.setCellValue("1.25G SFP");
		cell = titleRow.createCell(3);
		cell.setCellValue("2.5G SFP");
		cell = titleRow.createCell(4);
		cell.setCellValue("10G XFP");
		cell = titleRow.createCell(5);
		cell.setCellValue("Tunable XFP");

		Map<String, NodePresent> NodeMap = new HashMap<String, NodePresent>();

		for (int i = 1; i < nSize; i++) {
			String expression = "/network/physicalTopology/node[@nodeIDFromSheet="
					+ i + "]/@name";
			String name = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			expression = "/network/physicalTopology/node[@nodeIDFromSheet=" + i
					+ "]/@type";
			String type = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");
			NodeMap.put("n" + i, new NodePresent(name, type));
		}

		for (NodePresent node : NodeMap.values()) {
			for (int i = 1; i <= dSize; i++) {
				String expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
						+ i + "]/@ingressNodeId";
				String sourceID = getValueFromXML(expression, baseFileName
						+ "_output.xml.n2p");
				expression = "/network/physicalTopology/node[@no=" + sourceID
						+ "]/@name";
				String sourceName = getValueFromXML(expression, baseFileName
						+ "_output.xml.n2p");
				expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
						+ i + "]/@egressNodeId";
				String destID = getValueFromXML(expression, baseFileName
						+ "_output.xml.n2p");
				expression = "/network/physicalTopology/node[@no=" + destID
						+ "]/@name";
				String destName = getValueFromXML(expression, baseFileName
						+ "_output.xml.n2p");

				String nodeName = node.getName();

				if (nodeName.equals(sourceName) || nodeName.equals(destName)) {
					int l = 0;
					expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
							+ i + "]/@trafficSTM16";
					String trafficSTM16 = getValueFromXML(expression,
							baseFileName + "_output.xml.n2p");
					int c = Integer.parseInt(trafficSTM16);
					expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
							+ i + "]/@trafficSTM64";
					String trafficSTM64 = getValueFromXML(expression,
							baseFileName + "_output.xml.n2p");
					int a = Integer.parseInt(trafficSTM64);
					l = a;
					expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
							+ i + "]/@trafficGE";
					String trafficGE = getValueFromXML(expression, baseFileName
							+ "_output.xml.n2p");
					int d = Integer.parseInt(trafficGE);
					expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
							+ i + "]/@traffic10GE";
					String traffic10GE = getValueFromXML(expression,
							baseFileName + "_output.xml.n2p");
					int b = Integer.parseInt(traffic10GE);
					l = l + b;
					expression = "/network/demandSet/demandEntry[@trafficIDFromSheet="
							+ i + "]/@traffic40GE";
					String traffic40GE = getValueFromXML(expression,
							baseFileName + "_output.xml.n2p");

					if (c != 0 && d != 0) {
						int num;
						double y;
						double e = c * 2.5;
						if (e % 10 == 0) {
							l = (int) (l + e / 10);
							y = 0.0;
						} else {
							l = (int) (l + e / 10 + 1);
							y = 10 - (e % 10);
						}
						num = (int) (y / 1.25);
						if (num < d) {
							int diff = d - num;
							double newnum = diff * 1.25;
							if (newnum % 10 == 0)
								l = (int) (newnum / 10) + l;
							else
								l = (int) (l + (newnum / 10) + 1);
						}

					}

					if (c != 0 && d == 0) {
						double f = c * 2.5;
						if (f % 10 == 0)
							l = (int) (l + f / 10);
						else
							l = (int) (l + f / 10) + 1;
					}

					if (c == 0 && d != 0) {
						double g = d * 1.25;
						if (g % 10 == 0)
							l = (int) (l + g / 10);
						else
							l = (int) (l + g / 10) + 1;
					}

					node.modifysfp1(d);
					node.modifysfp2(c);
					node.modifyxfp1(a + b);
					node.modifyxfp2(l);

				}
			}
		}

		int x = 1;
		for (NodePresent node : NodeMap.values()) {
			Row row = billOfMaterials.createRow(x);
			cell = row.createCell(0);
			cell.setCellValue(node.getName());
			cell = row.createCell(1);
			cell.setCellValue(node.getType());
			cell = row.createCell(2);
			cell.setCellValue(node.getsfp1());
			cell = row.createCell(3);
			cell.setCellValue(node.getsfp2());
			cell = row.createCell(4);
			cell.setCellValue(node.getxfp1());
			cell = row.createCell(5);
			cell.setCellValue(node.getxfp2());
			x++;
		}

		// Read Calculations_Output.xml and populate linkEngineeringSheet
		titleRow = linkEngineeringSheet.createRow(0);
		cell = titleRow.createCell(0);
		cell.setCellValue("ID");
		cell = titleRow.createCell(1);
		cell.setCellValue("Source");
		cell = titleRow.createCell(2);
		cell.setCellValue("Destination");
		cell = titleRow.createCell(3);
		cell.setCellValue("Node");
		cell = titleRow.createCell(4);
		cell.setCellValue("Towards");
		cell = titleRow.createCell(5);
		cell.setCellValue("Tx/Rx");
		cell = titleRow.createCell(6);
		cell.setCellValue("Power (in dBm)");
		cell = titleRow.createCell(7);
		cell.setCellValue("OSNR (in dB)");
		cell = titleRow.createCell(8);
		cell.setCellValue("Dispersion (in ps/nm)");
		cell = titleRow.createCell(9);
		cell.setCellValue("CustomerNode_Type");
		cell = titleRow.createCell(10);
		cell.setCellValue("Amplifiers");
		cell = titleRow.createCell(11);
		cell.setCellValue("Link Lengths (km)");
		cell = titleRow.createCell(12);
		cell.setCellValue("DCMs");

		SAXBuilder calcXMLbuilder = new SAXBuilder();
		File calcXmlFile = new File("xmlFiles/Calculations_Output.xml");

		Document calcDocument = (Document) calcXMLbuilder.build(calcXmlFile);
		Element completeCalcInfo = calcDocument.getRootElement();

		List pathList = completeCalcInfo.getChildren("Path");

		int i = 1;
		int j = 1;
		for (Iterator iterator = pathList.iterator(); iterator.hasNext();) {
			Element path = (Element) iterator.next();

			String sourceID = path.getAttributeValue("source");
			String expression = "/network/physicalTopology/node[@no="
					+ sourceID + "]/@name";
			String sourceName = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");

			String destID = path.getAttributeValue("destination");
			expression = "/network/physicalTopology/node[@no=" + destID
					+ "]/@name";
			String destName = getValueFromXML(expression, baseFileName
					+ "_output.xml.n2p");

			// Defining Tx and Rx in linkEngineeringSheet
			Row pathRow = linkEngineeringSheet.createRow(i++);
			cell = pathRow.createCell(0);
			cell.setCellValue(j++);
			cell = pathRow.createCell(1);
			cell.setCellValue(sourceName);
			cell = pathRow.createCell(2);
			cell.setCellValue(destName);

			List calcPointList = path.getChildren("Calc");
			for (Iterator iterator2 = calcPointList.iterator(); iterator2
					.hasNext();) { // SS
				Element calcPoint = (Element) iterator2.next();
				if (calcPoint.getAttribute("measurementType") != null) {
					if (((calcPoint.getAttributeValue("measurementType"))
							.equals("Rx"))
							&& (calcPoint
									.getAttribute("Intermediate_Node_Names") != null)) {
						Row calcPointRow = linkEngineeringSheet.createRow(i++);
						String Intermediate_Node = calcPoint
								.getAttributeValue("Intermediate_Node_Names");
						if (Intermediate_Node.equals("Amplifier")) {
							String AmpType = calcPoint
									.getAttributeValue("Amp_Type");
							cell = calcPointRow.createCell(10);
							cell.setCellValue(AmpType);
						} else if (Intermediate_Node.equals("FiberLink")) {
							String LinkLength = calcPoint
									.getAttributeValue("Link_Length");
							cell = calcPointRow.createCell(11);
							cell.setCellValue(LinkLength);
						} else if (Intermediate_Node.equals("DCM")) {
							String DCMType = calcPoint
									.getAttributeValue("DCM_type");
							cell = calcPointRow.createCell(12);
							cell.setCellValue(DCMType);
						}
					}
					Row calcPointRow = linkEngineeringSheet.createRow(i++);
					String nodeID = calcPoint.getAttributeValue("nodeID");
					expression = "/network/physicalTopology/node[@no=" + nodeID
							+ "]/@name";
					String nodeName = getValueFromXML(expression, baseFileName
							+ "_output.xml.n2p");

					String linkID = calcPoint.getAttributeValue("linkID");
					expression = "/network/physicalTopology/link[@LinkId="
							+ linkID + "]/@originNodeId";
					String candidateNeighbor = getValueFromXML(expression,
							baseFileName + "_output.xml.n2p");
					String neighborNodeId = "";
					if (!candidateNeighbor.equals(nodeID)) {
						neighborNodeId = candidateNeighbor;
					} else {
						expression = "/network/physicalTopology/link[@LinkId="
								+ linkID + "]/@destinationNodeId";
						neighborNodeId = getValueFromXML(expression,
								baseFileName + "_output.xml.n2p");
					}
					expression = "/network/physicalTopology/node[@no="
							+ neighborNodeId + "]/@name";
					String neighborNodeName = getValueFromXML(expression,
							baseFileName + "_output.xml.n2p");

					String measurementType = calcPoint
							.getAttributeValue("measurementType");
					String power = calcPoint.getAttributeValue("Power_in_dBm");
					String osnr = calcPoint.getAttributeValue("OSNR_in_dB");
					String dispersion = calcPoint
							.getAttributeValue("Disp_in_ps_per_nm");
					String model = calcPoint
							.getAttributeValue("Customer_Defined_Node_Type");

					cell = calcPointRow.createCell(3);
					cell.setCellValue(nodeName);
					cell = calcPointRow.createCell(4);
					cell.setCellValue(neighborNodeName);
					cell = calcPointRow.createCell(5);
					cell.setCellValue(measurementType);
					cell = calcPointRow.createCell(6);
					cell.setCellValue(power);
					cell = calcPointRow.createCell(7);
					cell.setCellValue(osnr);
					cell = calcPointRow.createCell(8);
					cell.setCellValue(dispersion);
					cell = calcPointRow.createCell(9);
					cell.setCellValue(model);
				}
				if ((calcPoint.getAttribute("Intermediate_Node_Names") != null)
						&& ((calcPoint.getAttribute("measurementType") != null))) {
					if ((calcPoint.getAttributeValue("measurementType")
							.equals("Tx"))) {
						Row calcPointRow = linkEngineeringSheet.createRow(i++);
						String Intermediate_Node = calcPoint
								.getAttributeValue("Intermediate_Node_Names");
						if (Intermediate_Node.equals("Amplifier")) {
							String AmpType = calcPoint
									.getAttributeValue("Amp_Type");
							cell = calcPointRow.createCell(10);
							cell.setCellValue(AmpType);
						} else if (Intermediate_Node.equals("FiberLink")) {
							String LinkLength = calcPoint
									.getAttributeValue("Link_Length");
							cell = calcPointRow.createCell(11);
							cell.setCellValue(LinkLength);
						} else if (Intermediate_Node.equals("DCM")) {
							String DCMType = calcPoint
									.getAttributeValue("DCM_type");
							cell = calcPointRow.createCell(12);
							cell.setCellValue(DCMType);
						}
					}
				} else if ((calcPoint.getAttribute("Intermediate_Node_Names") != null)) {
					Row calcPointRow = linkEngineeringSheet.createRow(i++);
					String Intermediate_Node = calcPoint
							.getAttributeValue("Intermediate_Node_Names");
					if (Intermediate_Node.equals("Amplifier")) {
						String AmpType = calcPoint
								.getAttributeValue("Amp_Type");
						cell = calcPointRow.createCell(10);
						cell.setCellValue(AmpType);
					} else if (Intermediate_Node.equals("FiberLink")) {
						String LinkLength = calcPoint
								.getAttributeValue("Link_Length");
						cell = calcPointRow.createCell(11);
						cell.setCellValue(LinkLength);
					} else if (Intermediate_Node.equals("DCM")) {
						String DCMType = calcPoint
								.getAttributeValue("DCM_type");
						cell = calcPointRow.createCell(12);
						cell.setCellValue(DCMType);
					}
				}

			} // SS
		}

		try {
			// Write the workbook in file system
			FileOutputStream out = new FileOutputStream(new File(
					outputExcelFileName));
			workbook.write(out);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the object from the session XML file for the corresponding filter
	 * expression.
	 * 
	 * @param expression
	 *            Expression string, like /NECONFIG/NE/Interface/FEIF
	 * @param configFileName
	 *            Config file to parse
	 * @return Value that matches the filter criteria
	 */
	public static String getValueFromXML(String expression,
			String configFileName) {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		InputSource inputSource = null;
		try {
			inputSource = new InputSource(new FileInputStream(configFileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		XPathExpression expr;
		String element = null;
		try {
			expr = xPath.compile(expression);
			element = expr.evaluate(inputSource);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return element;
	}

	/**
	 * Parses the given XML file for the expression and evaluates the result.
	 * 
	 * @param expression
	 *            Expression string, like /NECONFIG/NE/Interface/FEIF
	 * @param configFileName
	 *            XML FileName
	 * @return Object matching the filter criteria
	 * @throws Exception
	 */
	public static Object parseXpath(String expression, String configFileName)
			throws Exception {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		InputSource inputSource = null;
		try {
			inputSource = new InputSource(new FileInputStream(configFileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		XPathExpression expr;
		Object element = null;
		expr = xPath.compile(expression);
		element = expr.evaluate(inputSource, XPathConstants.NODESET);

		return element;
	}

}
