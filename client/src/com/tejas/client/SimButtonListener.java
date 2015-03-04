package com.tejas.client;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;





import java.io.File;
import java.io.StringReader;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.mxgraph.canvas.mxGraphicsCanvas2D;
import com.mxgraph.reader.mxSaxOutputHandler;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import com.tejas.eda.spice.DWDMRouteToSpiceConverter;
import com.tejas.engine.CLINet2Plan;


public class SimButtonListener implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
		XMLWriter xmlpg = new XMLWriter();
		String baseFileName = xmlpg.writeXML("saveforsim");
		System.out.println(baseFileName);

		String path = "xmlFiles";
		String inputfilename = path + "/" + baseFileName + "_input.xml";
		String outputfilename = path + "/" + baseFileName + "_output.xml.n2p";
		String[] cmdArgs = { 
				"--mode", "net-design",
				"--class-file", "lib/algorithms.jar",
				"--class-name", "com.tejas.workspace.examples.netDesignAlgorithm.fa.workingAndProtectionPath",
				"--input-file", inputfilename,
				"--output-file", outputfilename};

		CLINet2Plan cliObj = new CLINet2Plan(cmdArgs);
		String[] dwdmArgs = {outputfilename};
		DWDMRouteToSpiceConverter dwdmObj = new DWDMRouteToSpiceConverter(dwdmArgs);
//		createImg();
		JOptionPane.showMessageDialog(null, "Simulation Completed. Please check xml files for details");
	}
		
//	public void createImg() {
//			
//		String path = null;
//		File f = new File(path + "/temp_file.png");
//		f.createNewFile();
//		
//		mxGraph graph = new mxGraph();
//		BufferedImage image = mxCellRenderer.createBufferedImage(graph,null, 1, Color.white, true, null);
//		Graphics2D g2 = image.createGraphics();
//		
//		XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
//		reader.setContentHandler(new mxSaxOutputHandler(new mxGraphicsCanvas2D(g2)));
//		reader.parse(new InputSource(new StringReader("C:\\Users\\vivekanandas\\Desktop\\sample.xml")));		
//	
//		ImageIO.write(image, "png", new File("imageexport.png"));
//	}
}

