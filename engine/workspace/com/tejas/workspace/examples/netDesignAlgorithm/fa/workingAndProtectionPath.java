package com.tejas.workspace.examples.netDesignAlgorithm.fa;

import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.jom.OptimizationProblem;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.libraries.WDMUtils;
import com.tejas.engine.libraries.GraphUtils.JUNGUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.StringUtils;
import com.tejas.engine.utils.Triple;
import com.tejas.engine.utils.Constants.OrderingType;
import com.tejas.engine.utils.Constants.SearchType;

import edu.uci.ics.jung.graph.Graph;

public class workingAndProtectionPath  implements IAlgorithm{

	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize some variables */
		final int N = netPlan.getNumberOfNodes();
		final int D = netPlan.getNumberOfDemands();
		final int E = netPlan.getNumberOfLinks();

		//*****
		final int W = Integer.parseInt(algorithmParameters.get("numWavelengthsPerFiber"));
		final double maxLightpathLengthInKm = Double.parseDouble(algorithmParameters.get("maxLightpathLengthInKm"));
		final double binaryRatePerChannel_Gbps = Double.parseDouble(algorithmParameters.get("binaryRatePerChannel_Gbps"));

		//*****
		Graph<Integer, Integer> g = JUNGUtils.getPhysicalLayerGraph(netPlan);

		/* Basic checks */
		if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology with links and a demand set");
		if (Double.parseDouble(net2planParameters.get("binaryRateInBitsPerSecondPerErlang")) != 1E9) throw new Net2PlanException("To avoid confusions, the traffic should be measured in Gbps in binaryRateInBitsPerSecondPerErlang net2plan option");

		final double [] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
		//final double [] u_e = netPlan.getLinkCapacityInErlangsVector();

		final int k = Integer.parseInt(algorithmParameters.get("k"));

		/* Round up the demand offered traffic to upper multiple of lightpath binary rate */
		for (int d = 0; d < D; d++) { final double hd = netPlan.getDemandOfferedTrafficInErlangs(d); netPlan.setDemandOfferedTrafficInErlangs(d, Math.ceil(hd / binaryRatePerChannel_Gbps) * binaryRatePerChannel_Gbps); }

		/* Set the link capacity as the binary rate of a lightpath multiplied by the number of wavelengths */
		for (int e = 0; e < E; e++)
			netPlan.setLinkCapacityInErlangs(e, W * binaryRatePerChannel_Gbps);

		// *****
		List<Set<Integer>> wavelengthOccupancy = new ArrayList<Set<Integer>>();
		for(int fiberId = 0; fiberId < E; fiberId++)
			wavelengthOccupancy.add(new HashSet<Integer>());

		int[] w_f = new int[E];
		Arrays.fill(w_f, W);
		// *****

		/* Construct the candidate path list */
		CandidatePathList cpl = new CandidatePathList(netPlan , netPlan.getLinkLengthInKmVector() , "K" , Integer.toString(k),"maxLengthInKm", Double.toString(maxLightpathLengthInKm));


		/* For each path, compute those of the same demand which are link disjoint */
		List<List<int[]>> candidate11PairsList_d = candidatePairs(netPlan, cpl);

		netPlan.removeAllRoutes();
		netPlan.removeAllProtectionSegments();

		int[][] fiberTable = netPlan.getLinkTable();

		WDMUtils.setFiberNumWavelengthsAttributes(netPlan, W);

		int routeNum = 0;
		int protNum = 0;
		for (int d = 0 ; d < D ; d ++)
		{


			int[] lowestlinks = null ;
			int[] a = null ;
			int n = 0;


			double traffic = netPlan.getDemandOfferedTrafficInErlangs(d) ;


			if (traffic % binaryRatePerChannel_Gbps == 0)  n = (int) (traffic / binaryRatePerChannel_Gbps) ;
			else n = (int) (traffic / binaryRatePerChannel_Gbps) + 1 ;


			for(int count = 0 ;count < candidate11PairsList_d.get(d).size()  ; count ++ )
			{
				a = candidate11PairsList_d.get(d).get(count) ;



				int b = a[1] ;

				int[] seqlinks =  cpl.getSequenceOfLinks(b);
				//System.out.println("the b sequence of links is " + Arrays.toString(seqlinks));


				List<Integer> intList = new ArrayList<Integer>();
				for (int index = 0; index < seqlinks.length; index++)
				{
					intList.add(seqlinks[index]);
				}
				//		    	double cost = CandidatePathList.getCostOfPath(intList) ;
				
				double cost = JUNGUtils.getPathWeight(intList, JUNGUtils.getEdgeWeightTransformer(netPlan.getLinkLengthInKmVector()));
				
				double temp,lowest = 0;

				if(count == 0) 
				{
					temp = cost;  lowest = temp ; lowestlinks = seqlinks ;
				}

				else
					if(cost < lowest)  { lowest = cost ; lowestlinks = seqlinks ;}



			}

			final int[] primLink = cpl.getSequenceOfLinks(a[0]);
			//System.out.println("The primary link is " + Arrays.toString(primLink));

			Map<String , String> trafficType = netPlan.getDemandSpecificAttributes(d);

			//   if(trafficType.containsValue("PreCalc-Restored"))

			//    {


			//    int temp = 0;

			for(int i = 0 ; i < n ; i++)

			{

				if (trafficType.containsValue("PreCalc-Restored")) 
				{
					Map<String, String> protectionSegmentMap = new HashMap<String, String>();
					Map<String, String> routeMap = new HashMap<String, String>();
					
					StringBuffer sb = new StringBuffer();
					for(int j = 0; j < lowestlinks.length-1; j++){
						sb.append(lowestlinks[j]).append("-");
					}
					sb.append(lowestlinks[lowestlinks.length - 1]);
					
					protectionSegmentMap.put("LinksTravelled", sb.toString());
					protectionSegmentMap.put("id", "" + protNum++ + "");
					routeMap.put("routeId", "" + routeNum++ + "");
					
					final int routeIdProtect = netPlan.addProtectionSegment(lowestlinks, 0 , protectionSegmentMap);
					final int routeId = netPlan.addRoute(d, h_d [d], primLink , new int [] { routeIdProtect } , routeMap);
					int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(fiberTable, primLink , w_f, wavelengthOccupancy) ;
					int[] wavelengthsAssignedProtect = WDMUtils.WA_firstFit(fiberTable, lowestlinks , w_f, wavelengthOccupancy) ;
					WDMUtils.setLightpathSeqWavelengthsAttribute(netPlan, routeId, wavelengthsAssignedWork);
					WDMUtils.setProtectionLightpathSeqWavelengthsAttribute(netPlan, routeIdProtect, wavelengthsAssignedProtect);

				}

				else

				{
					Map<String, String> routeMap = new HashMap<String, String>();
					routeMap.put("routeId", "" + routeNum++ + "");
					final int routeId = netPlan.addRoute(d, h_d [d], primLink , null , routeMap);
					int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(fiberTable, primLink , w_f, wavelengthOccupancy) ;
					WDMUtils.setLightpathSeqWavelengthsAttribute(netPlan, routeId, wavelengthsAssignedWork);

				}

			}

		}

		return " Ok! Cost " ;
	}

	private List<List<int[]>> candidatePairs(NetPlan netPlan , CandidatePathList cpl)
	{
		final int D = netPlan.getNumberOfDemands();
		List<List<int[]>> cpl11 = new ArrayList<List<int[]>>();
		for (int d = 0 ; d < D ; d ++)
		{
			cpl11.add(new ArrayList<int []> ());
			int[] PIds = cpl.getPathsPerDemand(d);
			final int p1 = PIds[0] ;
			int[] seqlink1 = cpl.getSequenceOfLinks(PIds[0]) ;
			//System.out.println("The seqlink1 is " + Arrays.toString(seqlink1));



			for (int count = 1 ; count < PIds.length; count ++)
			{
				int[] seqlink2 = cpl.getSequenceOfLinks(PIds[count]) ;
				final int p2 = PIds[count] ;
				//System.out.println("The seqlink2 is " + Arrays.toString(seqlink2));
				if (IntUtils.intersect(seqlink1 , seqlink2).length == 0)  
				{
					cpl11.get(d).add (new int [] { p1 , p2 } );
					//System.out.println("The seqlink2 after finding out intersectino is " + Arrays.toString(seqlink2));
				}

			}
			if (cpl11.get(d).isEmpty()) throw new Net2PlanException("Demand " + d + " has no two link-disjoint paths");
		}

		return cpl11 ;
	}
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> algorithmParameters = new ArrayList<Triple<String, String, String>>();
		algorithmParameters.add(Triple.of("k", "10", "Number of candidate paths per demand, used as a source to compute the 1:1 pairs"));
		algorithmParameters.add(Triple.of("numWavelengthsPerFiber", "80", "Number of wavelengths per link"));
		algorithmParameters.add(Triple.of("binaryRatePerChannel_Gbps", "10", "Binary rate of all the lightpaths"));
		algorithmParameters.add(Triple.of("maxLightpathLengthInKm", "5000", "Maximum allowed lightpath length in km"));
		algorithmParameters.add(Triple.of("solverName", "glpk", "The solver name to be used by JOM"));
		algorithmParameters.add(Triple.of("solverLibraryName", "c:\\windows\\system32\\glpk_4_54.dll", "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default."));
		return algorithmParameters;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

}
