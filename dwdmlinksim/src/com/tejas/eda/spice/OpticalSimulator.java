/*
 * Simulator.java
 *
 * Created on March 25, 2006, 2:29 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
/**
 *Origional copyrights compiled from multiple source codes.
 *Copyright 1990 Regents of the University of California.  All rights reserved.
 **/
package com.tejas.eda.spice;

import java.awt.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.tejas.eda.spice.analysis.ACAnalysis;
import com.tejas.eda.spice.analysis.Analysis;
import com.tejas.eda.spice.analysis.DcTrCurv;
import com.tejas.eda.spice.analysis.TransientAnalysis;
import com.tejas.eda.spice.analysis.optical.OpticalAnalysis;
import com.tejas.eda.spice.device.DWDMNetwork;
import com.tejas.eda.spice.device.SpiceCircuit;
import com.tejas.eda.spice.device.source.optical.OpticalSourceElement;
import com.tejas.eda.spice.device.sources.SourceElement;
import com.tejas.eda.spice.output.DefaultOutput;
import com.tejas.eda.spice.output.OpticalDefaultOutput;
import com.tejas.eda.spice.output.SpiceOutput;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.InstanceCard;
import com.tejas.eda.spice.parse.OpticalDeck;
import com.tejas.eda.spice.parse.OpticalInstanceCard;
import com.tejas.eda.spice.parse.ParserException;
import com.tejas.eda.spice.parse.analysis.AnalysisCard;
import com.tejas.eda.spice.parse.analysis.OpticalAnalysisCard;
import com.tejas.eda.spice.parse.discrete.MutualCard;
import com.tejas.eda.spice.parse.output.OpticalOutputCard;
import com.tejas.eda.spice.parse.output.OutputCard;
import com.tejas.eda.spice.parse.source.SourceCard;
import com.tejas.eda.spice.parse.source.optical.OpticalSourceCard;
import com.tejas.engine.utils.Pair;
import com.tejas.engine.utils.Triple;

import javolution.context.Context;
import javolution.util.FastList;
import javolution.util.FastList.Node;
import javolution.util.FastTable;
import javolution.util.StandardLog;

/**
 *
 * @author AdMathLib.ministrator
 */
public class OpticalSimulator {

   
	static String[] Elements=null;
	public LinkedHashMap<String, Double> ModelNames = new LinkedHashMap<String, Double>(); //ss
	Map<String, Pair<String, String>> Model_Node = new LinkedHashMap<String,Pair<String, String>>();
    public OpticalAnalysis analysis;
    FastList<OpticalAnalysis> analyses = new FastList<OpticalAnalysis>();
    FastList<OpticalSourceElement> srcElements = new FastList<OpticalSourceElement>();
    public DWDMNetwork nwk = new DWDMNetwork();
    double time;
    OpticalDefaultOutput defaultOutput;

    /*
    //TODO XSpice
    FastTable<XNode> nodes;
    FastMap<String, XInstance> instTable = new FastMap<String, XInstance>();
    FastMap<String, XNode> nodeTable = new FastMap<String, XNode>();
    FastTable<Statistics> statistics = new FastTable<Statistics>();
    FastTable<XInstance> hybrids = new FastTable<XInstance>();
    FastTable<XNode> analogNodes = new FastTable<XNode>();
    FastTable<XNode> eventNodes = new FastTable<XNode>();
    FastMap<DEVICE, XDevice> deviceTable = new FastMap<DEVICE, XDevice>();
    InstanceQueue instQueue = new InstanceQueue();
    NodeQueue nodeQueue = new NodeQueue();
    OutputQueue outputQueue = new OutputQueue();
    //    FastTable<Real> hybridIndex = new FastTable<Real>);
    //    FastTable<Port> portTable = new FastTable<Port>();
    //    FastTable<Output> outputTable;

    int numHybridOutputs;
    int maxEventPasses;
    int maxOpAlternations;
    int opPasses;
     *
     */
//    FastTable<State> states;
//    boolean opAlternate;
//    public EnumMap<DEVICE, Class> devMap = new EnumMap(DEVICE.class);
    private static OpticalSimulator sim ;

    /**
     * Creates a new instance of Simulator
     * @return 
     * @return 
     */
    
    public OpticalSimulator() {
    }

    public static OpticalSimulator getSimulator() {
        if (sim == null) {
            sim = new OpticalSimulator(); 
        }
        return sim;
    }
    
    
    public static void resetSimulator() {
    	sim = null;
	}

	public static void main(String args[]) {
    	getSimulator().simulate(args[0]);
    }

  	public  OpticalSimulator simulate(String arg) {
        OpticalDeck deck = null; 
            try {
            	deck = sim.parseFile(new File(arg)); 	
            } catch (ParserException ex) {
                Logger.getLogger(OpticalSimulator.class.getName()).log(Level.SEVERE, null, ex);
            }
        if (deck != null) { 
            sim.assembleDeck(deck); 
            sim.run(); 
        }
        return sim;
    }
  	 //ArrayList<String> ModelNames = new ArrayList<String>();
  	
    public OpticalDeck parseFile(File file) throws ParserException {
        OpticalDeck deck = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            deck = new OpticalDeck(); // an object of opticalDeck in same file name
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {    
                	Elements = line.replaceAll(",|=", " ").split(" ");//SS
                	if(!((Elements[0].charAt(0)=='.')||(Elements[0].charAt(1)=='i')))//ss
                	{
                	ModelNames.put(Elements[0],null); //ss 
                	Model_Node.put(Elements[0], new Pair<>((Elements[1]),(Elements[2])));
                 	}
                    if (!deck.readLine(line)) {
                    	
                        break;
                    }
                 
                }
            }
            
            System.out.println(Model_Node.values());
        } catch (java.lang.ExceptionInInitializerError ex) {
            Logger.getLogger(OpticalSimulator.class.getName()).log(Level.SEVERE, null, ex.getCause());
        } catch (FileNotFoundException ex) {
            throw new ParserException(ex);
        } catch (IOException ex) {
            throw new ParserException(ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                throw new ParserException(ex);
            }
        }
        return deck;
    }

    public boolean assembleDeck(OpticalDeck deck) {
    	
        defaultOutput = new OpticalDefaultOutput(deck.getTitle());	
        FastList<OpticalInstanceCard> saveForLast = new FastList<OpticalInstanceCard>();
        
        FastList<OpticalInstanceCard> instCards = deck.getInstanceCards(); 
        //TODO Need to check about the order of placing components - Anant
        for (FastList.Node<OpticalInstanceCard> n = instCards.head(),
                end = instCards.tail(); (n = n.getNext()) != end;) { 
            OpticalInstanceCard icard = n.getValue();
       


            if (icard instanceof OpticalSourceCard) {
                saveForLast.add(icard);  
            } else { 
            	nwk.addInstance(icard.createInstance(deck));
                  
            }
        }
        for (FastList.Node<OpticalInstanceCard> n = saveForLast.head(),
                end = saveForLast.tail(); (n = n.getNext()) != end;) {
            OpticalInstanceCard icard = n.getValue();
            nwk.addInstance(icard.createInstance(deck)); //add to the DWDM network
        }
        FastTable<String> nodes = deck.getNodes();
        nwk.initNodes(nodes);
        FastList<OpticalAnalysisCard> analCards = deck.getAnalysisCards();
        
        for (FastList.Node<OpticalAnalysisCard> n = analCards.head(),
                end = analCards.tail(); (n = n.getNext()) != end;) {
            OpticalAnalysisCard acard = n.getValue();
            OpticalAnalysis a = acard.createAnalysis();
            a.addNonConverganceEventListener(defaultOutput);
            
            analyses.add(a);
        }
        FastList<OpticalOutputCard> outCards = deck.getOutputCards();
        if (outCards.size() > 0) {
            for (FastList.Node<OpticalOutputCard> n = outCards.head(),
                    end = outCards.tail(); (n = n.getNext()) != end;) {
                OpticalOutputCard ocard = n.getValue();
                SpiceOutput output = ocard.createOutput(deck);
                String analType = ocard.getAnalysisString();
                for (FastList.Node<OpticalAnalysis> m = analyses.head(),
                        e = analyses.tail(); (m = m.getNext()) != e;) {
                    OpticalAnalysis anal = m.getValue();
//TODO Needs to be checked for different analysis types
//                    if (analType.equals("DC") && anal instanceof DcTrCurv) {
//                        anal.addAnalysisEventListener(output);
//                        continue;
//                    } else if (analType.equals("AC") && anal instanceof ACAnalysis) {
//                        anal.addAnalysisEventListener(output);
//                        continue;
//                    } else if (analType.equals("TRAN") && anal instanceof TransientAnalysis) {
//                        anal.addAnalysisEventListener(output);
//                        continue;
//                    }
                }
            }
        } else {
            for (Node<OpticalAnalysis> n = analyses.head(),
                    end = analyses.tail(); (n = n.getNext()) != end;) {
                OpticalAnalysis anal = n.getValue();
                anal.addAnalysisEventListener(defaultOutput);
            }
        }
        srcElements = deck.getSrcElements();
        return true;
    }

    public void run() {
        StandardLog debugLog = new StandardLog();//Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
        StandardLog.enter(debugLog);
        try {
            nwk.init();
            for (FastList.Node<OpticalSourceElement> n = srcElements.head(),
                    end = srcElements.tail(); (n = n.getNext()) != end;) {
                n.getValue().init(nwk);
            }
            //TODO - Temperature specific analysis is not required for networks.  - Anant
//            nwk.temperature();
            for (FastList.Node<OpticalAnalysis> n = analyses.head(),
                    end = analyses.tail(); (n = n.getNext()) != end;) {
                OpticalAnalysis anal = n.getValue();
                analysis = anal;
                analysis.init(nwk);
                analysis.analyze(true);
                
            }
        } finally {
            StandardLog.exit(debugLog);
        }
    }
}
