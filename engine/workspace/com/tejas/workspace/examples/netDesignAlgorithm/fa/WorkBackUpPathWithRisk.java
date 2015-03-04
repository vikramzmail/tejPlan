package com.tejas.workspace.examples.netDesignAlgorithm.fa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import com.google.common.primitives.Ints;
import com.tejas.engine.interfaces.networkDesign.IAlgorithm;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.libraries.WDMUtils;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.Triple;

public class WorkBackUpPathWithRisk implements IAlgorithm {
	@Override
	public String executeAlgorithm(NetPlan netPlan,
			Map<String, String> algorithmParameters,
			Map<String, String> net2planParameters) {
		/* Initialize some variables */
		int N = netPlan.getNumberOfNodes();
		int E = netPlan.getNumberOfLinks();
		int D = netPlan.getNumberOfDemands();

		if (N == 0 || E == 0 || D == 0)
			throw new Net2PlanException(
					"This algorithm requires a topology and a demand set");

		String shortestPathType = algorithmParameters.get("shortestPathType");
		final int W = Integer.parseInt(algorithmParameters
				.get("numWavelengthsPerFiber"));
		final double binaryRatePerChannel_Gbps = Double
				.parseDouble(algorithmParameters
						.get("binaryRatePerChannel_Gbps"));

		if (!shortestPathType.equalsIgnoreCase("hops")
				&& !shortestPathType.equalsIgnoreCase("km"))
			throw new Net2PlanException(
					"'shortestPathType' must be 'hops' or 'km'");
		double[] linkCosts = (shortestPathType.equalsIgnoreCase("hops")) ? DoubleUtils
				.ones(E) : netPlan.getLinkLengthInKmVector();
		double[] h_d = netPlan.getDemandOfferedTrafficInErlangsVector();
		int[][] linkTable = netPlan.getLinkTable();

		List<Set<Integer>> wavelengthOccupancy = new ArrayList<Set<Integer>>();
		for (int fiberId = 0; fiberId < E; fiberId++)
			wavelengthOccupancy.add(new HashSet<Integer>());

		int[] w_f = new int[E];
		Arrays.fill(w_f, W);

		/* First compute all the primary and backup paths */
		/* If for two nodes, we could not find primary & backup paths => return */
		ArrayList<int[]> primaryPaths = new ArrayList<int[]>();
		ArrayList<int[]> backupPaths = new ArrayList<int[]>();
		for (int d = 0; d < D; d++) {
			int a_d = netPlan.getDemandIngressNode(d);
			int b_d = netPlan.getDemandEgressNode(d);
			int[] primaryPath = GraphUtils.getShortestPath(linkTable, a_d, b_d,
					linkCosts);
			primaryPaths.add(primaryPath);
			if (primaryPath.length == 0)
				throw new Net2PlanException("The network is not connected");
			double[] onesInNonUsedLinks = DoubleUtils.ones(E);
			for (int e : primaryPath)
				onesInNonUsedLinks[e] = 0;
			int[] backupPath = GraphUtils.getCapacitatedShortestPath(linkTable,
					a_d, b_d, linkCosts, onesInNonUsedLinks, 1);
			// if (backupPath.length == 0) throw new Net2PlanException
			// ("Could not find a link disjoint backup path");
			if (backupPath.length == 0) {
				Set<Integer> iSet = new LinkedHashSet<Integer>();

				int[] sequenceOfNodes = netPlan
						.convertSequenceOfLinks2SequenceOfNodes(primaryPath);

				for (int i = 0; i < sequenceOfNodes.length - 1; i++) {
					int a = sequenceOfNodes[i];
					int b = sequenceOfNodes[i + 1];

					int[] secondaryPath = GraphUtils.getShortestPath(linkTable,
							a, b, linkCosts);
					double[] onesNonUsed = DoubleUtils.ones(E);
					for (int e : secondaryPath)
						onesNonUsed[e] = 0;
					int[] secondaryBackupPath = GraphUtils
							.getCapacitatedShortestPath(linkTable, a, b,
									linkCosts, onesNonUsed, 1);
					if (secondaryBackupPath.length == 0)
						secondaryBackupPath = secondaryPath;

					Integer[] a_n = ArrayUtils.toObject(secondaryBackupPath);
					iSet.addAll(Arrays.asList(a_n));

				}

				backupPath = Ints.toArray(iSet);
				iSet.clear();

			}

			backupPaths.add(backupPath);
		}

		/* Remove all the routes and links */
		netPlan.removeAllRoutes();
		netPlan.removeAllProtectionSegments();

		int[][] fiberTable = netPlan.getLinkTable();

		WDMUtils.setFiberNumWavelengthsAttributes(netPlan, W);

		int routeNum = 0;
		int protNum = 0;

		/* Update the netPlan object with the routes and the protection segments */
		for (int d = 0; d < D; d++) {
			double traffic = netPlan.getDemandOfferedTrafficInErlangs(d);
			// int n= 0 ;
			//
			// if (traffic % binaryRatePerChannel_Gbps == 0) n = (int) (traffic
			// / binaryRatePerChannel_Gbps) ;
			// else n = (int) (traffic / binaryRatePerChannel_Gbps) + 1 ;

			Map<String, String> trafficType = netPlan
					.getDemandSpecificAttributes(d);

			// ///////////////////

			if (trafficType.containsValue("PreCalc-Restored")) {
				if (trafficType.get("traffic40GE") != "") {
					int t40 = Integer.parseInt(trafficType.get("traffic40GE"));
					int total = t40 * 40;
					int w = 0;
					if (total % binaryRatePerChannel_Gbps == 0)
						w = (int) (total / binaryRatePerChannel_Gbps);
					else
						w = (int) (total / binaryRatePerChannel_Gbps + 1);
					for (int i = 0; i < w; i++) {
						Map<String, String> protectionSegmentMap = new HashMap<String, String>();
						Map<String, String> routeMap = new HashMap<String, String>();

						StringBuffer sb = new StringBuffer();
						for (int j = 0; j < backupPaths.get(d).length - 1; j++) {
							sb.append(backupPaths.get(d)[j]).append("-");
						}
						sb.append(backupPaths.get(d)[backupPaths.get(d).length - 1]);

						protectionSegmentMap.put("LinksTravelled",
								sb.toString());
						protectionSegmentMap.put("id", "" + protNum++ + "");
						routeMap.put("routeId", "" + routeNum++ + "");
						protectionSegmentMap.put("traffic40GE", "1");
						routeMap.put("traffic40GE", "1");

						int backupSegment = netPlan.addProtectionSegment(
								backupPaths.get(d), h_d[d],
								protectionSegmentMap);
						int routeId = netPlan.addRoute(d, h_d[d],
								primaryPaths.get(d),
								new int[] { backupSegment }, routeMap);

						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						int[] wavelengthsAssignedProtect = WDMUtils
								.WA_firstFit(fiberTable, backupPaths.get(d),
										w_f, wavelengthOccupancy);

						WDMUtils.setLightpathSeqWavelengthsAttribute(netPlan,
								routeId, wavelengthsAssignedWork);
						WDMUtils.setProtectionLightpathSeqWavelengthsAttribute(
								netPlan, backupSegment,
								wavelengthsAssignedProtect);
					}
				}

				if (trafficType.get("traffic10GE") != "") {
					int t10 = Integer.parseInt(trafficType.get("traffic10GE"));
					int total = t10 * 10;
					int w = 0;
					if (total % binaryRatePerChannel_Gbps == 0)
						w = (int) (total / binaryRatePerChannel_Gbps);
					else
						w = (int) (total / binaryRatePerChannel_Gbps + 1);
					for (int i = 0; i < w; i++) {
						Map<String, String> protectionSegmentMap = new HashMap<String, String>();
						Map<String, String> routeMap = new HashMap<String, String>();

						StringBuffer sb = new StringBuffer();
						for (int j = 0; j < backupPaths.get(d).length - 1; j++) {
							sb.append(backupPaths.get(d)[j]).append("-");
						}
						sb.append(backupPaths.get(d)[backupPaths.get(d).length - 1]);

						protectionSegmentMap.put("LinksTravelled",
								sb.toString());
						protectionSegmentMap.put("id", "" + protNum++ + "");
						routeMap.put("routeId", "" + routeNum++ + "");
						protectionSegmentMap.put("traffic10GE", "1");
						routeMap.put("traffic10GE", "1");

						int backupSegment = netPlan.addProtectionSegment(
								backupPaths.get(d), h_d[d],
								protectionSegmentMap);
						int routeId = netPlan.addRoute(d, h_d[d],
								primaryPaths.get(d),
								new int[] { backupSegment }, routeMap);

						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						int[] wavelengthsAssignedProtect = WDMUtils
								.WA_firstFit(fiberTable, backupPaths.get(d),
										w_f, wavelengthOccupancy);

						WDMUtils.setLightpathSeqWavelengthsAttribute(netPlan,
								routeId, wavelengthsAssignedWork);
						WDMUtils.setProtectionLightpathSeqWavelengthsAttribute(
								netPlan, backupSegment,
								wavelengthsAssignedProtect);
					}

				}

				if (trafficType.get("trafficSTM64") != "") {
					int stm64 = Integer.parseInt(trafficType
							.get("trafficSTM64"));
					int total = stm64 * 10;
					int w = 0;
					if (total % binaryRatePerChannel_Gbps == 0)
						w = (int) (total / binaryRatePerChannel_Gbps);
					else
						w = (int) (total / binaryRatePerChannel_Gbps + 1);
					for (int i = 0; i < w; i++) {
						Map<String, String> protectionSegmentMap = new HashMap<String, String>();
						Map<String, String> routeMap = new HashMap<String, String>();

						StringBuffer sb = new StringBuffer();
						for (int j = 0; j < backupPaths.get(d).length - 1; j++) {
							sb.append(backupPaths.get(d)[j]).append("-");
						}
						sb.append(backupPaths.get(d)[backupPaths.get(d).length - 1]);

						protectionSegmentMap.put("LinksTravelled",
								sb.toString());
						protectionSegmentMap.put("id", "" + protNum++ + "");
						routeMap.put("routeId", "" + routeNum++ + "");
						protectionSegmentMap.put("trafficSTM64", "1");
						routeMap.put("trafficSTM64", "1");

						int backupSegment = netPlan.addProtectionSegment(
								backupPaths.get(d), h_d[d],
								protectionSegmentMap);
						int routeId = netPlan.addRoute(d, h_d[d],
								primaryPaths.get(d),
								new int[] { backupSegment }, routeMap);

						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						int[] wavelengthsAssignedProtect = WDMUtils
								.WA_firstFit(fiberTable, backupPaths.get(d),
										w_f, wavelengthOccupancy);

						WDMUtils.setLightpathSeqWavelengthsAttribute(netPlan,
								routeId, wavelengthsAssignedWork);
						WDMUtils.setProtectionLightpathSeqWavelengthsAttribute(
								netPlan, backupSegment,
								wavelengthsAssignedProtect);
					}
				}

				if (trafficType.get("trafficSTM16") != ""
						&& trafficType.get("trafficGE") != "") {
					int stm16 = Integer.parseInt(trafficType
							.get("trafficSTM16"));
					int tge = Integer.parseInt(trafficType.get("trafficGE"));
					double z;
					int w, num;
					int[] extraLambdaWork = new int[100]; // considering the
															// number of links
															// traveled is
															// always less than
															// 100
					int[] extraLambdaProtect = new int[100];
					double y = stm16 * 2.5;
					double p = tge * 1.25;

					if (y % 10 == 0) {
						z = 0;
						w = (int) (y / 10);
					} else {
						z = 10 - (y % 10);
						w = (int) ((y / 10) + 1);
					}

					int counter = 1;
					for (int i = 0; i < w; i++) {
						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						int[] wavelengthsAssignedProtect = WDMUtils
								.WA_firstFit(fiberTable, backupPaths.get(d),
										w_f, wavelengthOccupancy);

						for (int j = 0; j < 4 && counter++ <= stm16; j++) // four
																			// STM-16
																			// requests
																			// can
																			// be
																			// accommodated
																			// in
																			// one
																			// 10G
																			// wavelength
						{
							Map<String, String> protectionSegmentMap = new HashMap<String, String>();
							Map<String, String> routeMap = new HashMap<String, String>();

							StringBuffer sb = new StringBuffer();
							for (int k = 0; k < backupPaths.get(d).length - 1; k++) {
								sb.append(backupPaths.get(d)[k]).append("-");
							}
							sb.append(backupPaths.get(d)[backupPaths.get(d).length - 1]);

							protectionSegmentMap.put("LinksTravelled",
									sb.toString());
							protectionSegmentMap.put("id", "" + protNum++ + "");
							routeMap.put("routeId", "" + routeNum++ + "");
							protectionSegmentMap.put("trafficSTM16", "1");
							routeMap.put("trafficSTM16", "1");

							int backupSegment = netPlan.addProtectionSegment(
									backupPaths.get(d), h_d[d],
									protectionSegmentMap);
							int routeId = netPlan.addRoute(d, h_d[d],
									primaryPaths.get(d),
									new int[] { backupSegment }, routeMap);

							WDMUtils.setLightpathSeqWavelengthsAttribute(
									netPlan, routeId, wavelengthsAssignedWork);
							WDMUtils.setProtectionLightpathSeqWavelengthsAttribute(
									netPlan, backupSegment,
									wavelengthsAssignedProtect);

						}
						// storing the last wavelength assigned so as to use in
						// the Traffic1GE requirement if any space in the
						// wavlength could be used.

						if (i == w - 1) {
							extraLambdaWork = wavelengthsAssignedWork;
							extraLambdaProtect = wavelengthsAssignedProtect;
						}
					}
					num = (int) (z / 1.25);
					// In the remaining space available in the last wavelength
					// that was assigned to the last STM-16 request, we try to
					// accommodate the requests of 1 GE traffic.
					for (int i = 0; i < num && i < tge; i++) {
						// assign wavelengths only for tge number of demands and
						// within the fixed space of the available wavelength
						Map<String, String> protectionSegmentMap = new HashMap<String, String>();
						Map<String, String> routeMap = new HashMap<String, String>();

						StringBuffer sb = new StringBuffer();
						for (int k = 0; k < backupPaths.get(d).length - 1; k++) {
							sb.append(backupPaths.get(d)[k]).append("-");
						}
						sb.append(backupPaths.get(d)[backupPaths.get(d).length - 1]);

						protectionSegmentMap.put("LinksTravelled",
								sb.toString());
						protectionSegmentMap.put("id", "" + protNum++ + "");
						routeMap.put("routeId", "" + routeNum++ + "");
						protectionSegmentMap.put("traffic1GE", "1");
						routeMap.put("traffic1GE", "1");

						int backupSegment = netPlan.addProtectionSegment(
								backupPaths.get(d), h_d[d],
								protectionSegmentMap);
						int routeId = netPlan.addRoute(d, h_d[d],
								primaryPaths.get(d),
								new int[] { backupSegment }, routeMap);

						WDMUtils.setLightpathSeqWavelengthsAttribute(netPlan,
								routeId, extraLambdaWork);
						WDMUtils.setProtectionLightpathSeqWavelengthsAttribute(
								netPlan, backupSegment, extraLambdaProtect);

					}

					int newnum;
					if (tge >= num)
						newnum = tge - num;
					else
						newnum = 0;

					if (newnum != 0) {
						double request = newnum * 1.25;
						if (request % 10 == 0)
							w = (int) request / 10;
						else
							w = (int) request / 10 + 1;
						int counternew = 1;
						for (int i = 0; i < w; i++) {
							int[] wavelengthsAssignedWork = WDMUtils
									.WA_firstFit(fiberTable,
											primaryPaths.get(d), w_f,
											wavelengthOccupancy);
							int[] wavelengthsAssignedProtect = WDMUtils
									.WA_firstFit(fiberTable,
											backupPaths.get(d), w_f,
											wavelengthOccupancy);

							for (int j = 0; j < 8 && counternew++ <= newnum; j++) {
								Map<String, String> protectionSegmentMap = new HashMap<String, String>();
								Map<String, String> routeMap = new HashMap<String, String>();

								StringBuffer sb = new StringBuffer();
								for (int k = 0; k < backupPaths.get(d).length - 1; k++) {
									sb.append(backupPaths.get(d)[k])
											.append("-");
								}
								sb.append(backupPaths.get(d)[backupPaths.get(d).length - 1]);

								protectionSegmentMap.put("LinksTravelled",
										sb.toString());
								protectionSegmentMap.put("id", "" + protNum++
										+ "");
								routeMap.put("routeId", "" + routeNum++ + "");
								protectionSegmentMap.put("traffic1GE", "1");
								routeMap.put("traffic1GE", "1");

								int backupSegment = netPlan
										.addProtectionSegment(
												backupPaths.get(d), h_d[d],
												protectionSegmentMap);
								int routeId = netPlan.addRoute(d, h_d[d],
										primaryPaths.get(d),
										new int[] { backupSegment }, routeMap);

								WDMUtils.setLightpathSeqWavelengthsAttribute(
										netPlan, routeId,
										wavelengthsAssignedWork);
								WDMUtils.setProtectionLightpathSeqWavelengthsAttribute(
										netPlan, backupSegment,
										wavelengthsAssignedProtect);

							}

						}

					}

				}

				if (trafficType.get("trafficSTM16") == ""
						&& trafficType.get("trafficGE") != "") {
					int tge = Integer.parseInt(trafficType.get("trafficGE"));
					double p = tge * 1.25;
					int w;
					int counter = 1;
					if (p % 10 == 0)
						w = (int) p / 10;
					else
						w = (int) p / 10 + 1;

					for (int i = 0; i < w; i++) {
						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						int[] wavelengthsAssignedProtect = WDMUtils
								.WA_firstFit(fiberTable, backupPaths.get(d),
										w_f, wavelengthOccupancy);

						for (int k = 0; k < 8 && counter++ <= tge; k++) {
							Map<String, String> protectionSegmentMap = new HashMap<String, String>();
							Map<String, String> routeMap = new HashMap<String, String>();

							StringBuffer sb = new StringBuffer();
							for (int j = 0; j < backupPaths.get(d).length - 1; j++) {
								sb.append(backupPaths.get(d)[j]).append("-");
							}
							sb.append(backupPaths.get(d)[backupPaths.get(d).length - 1]);

							protectionSegmentMap.put("LinksTravelled",
									sb.toString());
							protectionSegmentMap.put("id", "" + protNum++ + "");
							routeMap.put("routeId", "" + routeNum++ + "");
							protectionSegmentMap.put("traffic1GE", "1");
							routeMap.put("traffic1GE", "1");

							int backupSegment = netPlan.addProtectionSegment(
									backupPaths.get(d), h_d[d],
									protectionSegmentMap);
							int routeId = netPlan.addRoute(d, h_d[d],
									primaryPaths.get(d),
									new int[] { backupSegment }, routeMap);

							WDMUtils.setLightpathSeqWavelengthsAttribute(
									netPlan, routeId, wavelengthsAssignedWork);
							WDMUtils.setProtectionLightpathSeqWavelengthsAttribute(
									netPlan, backupSegment,
									wavelengthsAssignedProtect);
						}
					}
				}

				if (trafficType.get("trafficSTM16") != ""
						&& trafficType.get("trafficGE") == "") {
					int stm16 = Integer.parseInt(trafficType
							.get("trafficSTM16"));
					double y = stm16 * 2.5;
					int w;
					int counter = 1;
					if (y % 10 == 0)
						w = (int) (y / 10);
					else
						w = (int) (y / 10) + 1;

					for (int i = 0; i < w; i++) {
						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						int[] wavelengthsAssignedProtect = WDMUtils
								.WA_firstFit(fiberTable, backupPaths.get(d),
										w_f, wavelengthOccupancy);

						for (int k = 0; k < 4 && counter++ <= stm16; k++) {

							Map<String, String> protectionSegmentMap = new HashMap<String, String>();
							Map<String, String> routeMap = new HashMap<String, String>();

							StringBuffer sb = new StringBuffer();
							for (int j = 0; j < backupPaths.get(d).length - 1; j++) {
								sb.append(backupPaths.get(d)[j]).append("-");
							}
							sb.append(backupPaths.get(d)[backupPaths.get(d).length - 1]);

							protectionSegmentMap.put("LinksTravelled",
									sb.toString());
							protectionSegmentMap.put("id", "" + protNum++ + "");
							routeMap.put("routeId", "" + routeNum++ + "");
							protectionSegmentMap.put("trafficSTM16", "1");
							routeMap.put("trafficSTM16", "1");

							int backupSegment = netPlan.addProtectionSegment(
									backupPaths.get(d), h_d[d],
									protectionSegmentMap);
							int routeId = netPlan.addRoute(d, h_d[d],
									primaryPaths.get(d),
									new int[] { backupSegment }, routeMap);

							WDMUtils.setLightpathSeqWavelengthsAttribute(
									netPlan, routeId, wavelengthsAssignedWork);
							WDMUtils.setProtectionLightpathSeqWavelengthsAttribute(
									netPlan, backupSegment,
									wavelengthsAssignedProtect);
						}
					}
				}
			} else // unprotected traffic.. only working paths are alloted.
			{
				if (trafficType.get("traffic40GE") != "") {
					int t40 = Integer.parseInt(trafficType.get("traffic40GE"));
					int total = t40 * 40;
					int w = 0;
					if (total % binaryRatePerChannel_Gbps == 0)
						w = (int) (total / binaryRatePerChannel_Gbps);
					else
						w = (int) (total / binaryRatePerChannel_Gbps + 1);
					for (int i = 0; i < w; i++) {
						Map<String, String> routeMap = new HashMap<String, String>();
						routeMap.put("routeId", "" + routeNum++ + "");
						routeMap.put("traffic40GE", "1");

						final int routeId = netPlan.addRoute(d, h_d[d],
								primaryPaths.get(d), null, routeMap);
						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						WDMUtils.setLightpathSeqWavelengthsAttribute(netPlan,
								routeId, wavelengthsAssignedWork);
					}
				}

				if (trafficType.get("traffic10GE") != "") {
					int t10 = Integer.parseInt(trafficType.get("traffic10GE"));
					int total = t10 * 10;
					int w = 0;
					if (total % binaryRatePerChannel_Gbps == 0)
						w = (int) (total / binaryRatePerChannel_Gbps);
					else
						w = (int) (total / binaryRatePerChannel_Gbps + 1);
					for (int i = 0; i < w; i++) {
						Map<String, String> routeMap = new HashMap<String, String>();
						routeMap.put("routeId", "" + routeNum++ + "");
						routeMap.put("traffic10GE", "1");

						final int routeId = netPlan.addRoute(d, h_d[d],
								primaryPaths.get(d), null, routeMap);
						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						WDMUtils.setLightpathSeqWavelengthsAttribute(netPlan,
								routeId, wavelengthsAssignedWork);
					}
				}

				if (trafficType.get("trafficSTM64") != "") {
					int stm64 = Integer.parseInt(trafficType
							.get("trafficSTM64"));
					int total = stm64 * 10;
					int w = 0;
					if (total % binaryRatePerChannel_Gbps == 0)
						w = (int) (total / binaryRatePerChannel_Gbps);
					else
						w = (int) (total / binaryRatePerChannel_Gbps + 1);
					for (int i = 0; i < w; i++) {
						Map<String, String> routeMap = new HashMap<String, String>();
						routeMap.put("routeId", "" + routeNum++ + "");
						routeMap.put("trafficSTM64", "1");

						final int routeId = netPlan.addRoute(d, h_d[d],
								primaryPaths.get(d), null, routeMap);
						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						WDMUtils.setLightpathSeqWavelengthsAttribute(netPlan,
								routeId, wavelengthsAssignedWork);
					}
				}

				// ***********
				if (trafficType.get("trafficSTM16") != ""
						&& trafficType.get("trafficGE") != "") {
					int stm16 = Integer.parseInt(trafficType
							.get("trafficSTM16"));
					int tge = Integer.parseInt(trafficType.get("trafficGE"));
					double z;
					int w, num;
					int[] extraLambdaWork = new int[100]; // considering the
															// number of links
															// traveled is
															// always less than
															// 100
					double y = stm16 * 2.5;
					double p = tge * 1.25;

					if (y % 10 == 0) {
						z = 0;
						w = (int) (y / 10);
					} else {
						z = 10 - (y % 10);
						w = (int) ((y / 10) + 1);
					}

					int counter = 1;
					for (int i = 0; i < w; i++) {
						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						for (int j = 0; j < 4 && counter++ <= stm16; j++) // four
																			// STM-16
																			// requests
																			// can
																			// be
																			// accommodated
																			// in
																			// one
																			// 10G
																			// wavelength
						{
							Map<String, String> routeMap = new HashMap<String, String>();
							routeMap.put("routeId", "" + routeNum++ + "");
							routeMap.put("trafficSTM16", "1");

							final int routeId = netPlan.addRoute(d, h_d[d],
									primaryPaths.get(d), null, routeMap);
							WDMUtils.setLightpathSeqWavelengthsAttribute(
									netPlan, routeId, wavelengthsAssignedWork);

						}
						if (i == w - 1) {
							extraLambdaWork = wavelengthsAssignedWork;
						}

					}

					num = (int) (z / 1.25);
					// In the remaining space available in the last wavelength
					// that was assigned to the last STM-16 request, we try to
					// accommodate the requests of 1 GE traffic.
					for (int i = 0; i < num && i < tge; i++) {
						Map<String, String> routeMap = new HashMap<String, String>();
						routeMap.put("routeId", "" + routeNum++ + "");
						routeMap.put("traffic1GE", "1");

						final int routeId = netPlan.addRoute(d, h_d[d],
								primaryPaths.get(d), null, routeMap);
						WDMUtils.setLightpathSeqWavelengthsAttribute(netPlan,
								routeId, extraLambdaWork);
					}

					int newnum;
					if (tge >= num)
						newnum = tge - num;
					else
						newnum = 0;

					if (newnum != 0) {
						double request = newnum * 1.25;
						if (request % 10 == 0)
							w = (int) request / 10;
						else
							w = (int) request / 10 + 1;
						int counternew = 1;
						for (int i = 0; i < w; i++) {
							int[] wavelengthsAssignedWork = WDMUtils
									.WA_firstFit(fiberTable,
											primaryPaths.get(d), w_f,
											wavelengthOccupancy);
							for (int j = 0; j < 8 && counternew++ <= newnum; j++) {
								Map<String, String> routeMap = new HashMap<String, String>();
								routeMap.put("routeId", "" + routeNum++ + "");
								routeMap.put("traffic1GE", "1");

								final int routeId = netPlan.addRoute(d, h_d[d],
										primaryPaths.get(d), null, routeMap);
								WDMUtils.setLightpathSeqWavelengthsAttribute(
										netPlan, routeId,
										wavelengthsAssignedWork);
							}
						}
					}
				}

				if (trafficType.get("trafficSTM16") == ""
						&& trafficType.get("trafficGE") != "") {
					int tge = Integer.parseInt(trafficType.get("trafficGE"));
					double p = tge * 1.25;
					int w, counter = 1;
					if (p % 10 == 0)
						w = (int) p / 10;
					else
						w = (int) p / 10 + 1;

					for (int i = 0; i < w; i++) {
						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						for (int j = 0; j < 8 && counter++ <= tge; j++) //
						{
							Map<String, String> routeMap = new HashMap<String, String>();
							routeMap.put("routeId", "" + routeNum++ + "");
							routeMap.put("traffic1GE", "1");

							final int routeId = netPlan.addRoute(d, h_d[d],
									primaryPaths.get(d), null, routeMap);
							WDMUtils.setLightpathSeqWavelengthsAttribute(
									netPlan, routeId, wavelengthsAssignedWork);

						}
					}
				}

				if (trafficType.get("trafficSTM16") != ""
						&& trafficType.get("trafficGE") == "") {
					int stm16 = Integer.parseInt(trafficType
							.get("trafficSTM16"));
					double y = stm16 * 2.5;
					int w;
					int counter = 1;
					if (y % 10 == 0)
						w = (int) (y / 10);
					else
						w = (int) (y / 10) + 1;

					for (int i = 0; i < w; i++) {
						int[] wavelengthsAssignedWork = WDMUtils.WA_firstFit(
								fiberTable, primaryPaths.get(d), w_f,
								wavelengthOccupancy);
						for (int j = 0; j < 4 && counter++ <= stm16; j++) //
						{
							Map<String, String> routeMap = new HashMap<String, String>();
							routeMap.put("routeId", "" + routeNum++ + "");
							routeMap.put("trafficSTM16", "1");

							final int routeId = netPlan.addRoute(d, h_d[d],
									primaryPaths.get(d), null, routeMap);
							WDMUtils.setLightpathSeqWavelengthsAttribute(
									netPlan, routeId, wavelengthsAssignedWork);

						}
					}
				}
				// ***********

			}

			
		}

		return "Ok!";

	}

	@Override
	public List<Triple<String, String, String>> getParameters() {
		List<Triple<String, String, String>> parameters = new ArrayList<Triple<String, String, String>>();
		parameters
				.add(Triple
						.of("shortestPathType",
								"hops",
								"Each demand is routed according to the shortest path according to this type. Can be 'km' or 'hops'"));
		parameters.add(Triple.of("numWavelengthsPerFiber", "80",
				"Number of wavelengths per link"));
		parameters.add(Triple.of("binaryRatePerChannel_Gbps", "10",
				"Binary rate of all the lightpaths"));
		return parameters;
	}

	@Override
	public String getDescription() {
		return "Algorithm for flow assignment problems which routes all traffic of each demand, through the shortest path, and"
				+ "then creates a backup path , measured in number of hops or in km, being this a user-defined parameter.";
	}

}
