/*******************************************************************************
 * Copyright (c) 2013-2014 Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza - initial API and implementation
 ******************************************************************************/

package com.tejas.engine.interfaces.networkDesign;

import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tint.IntFactory1D;

import com.tejas.engine.interfaces.networkDesign.Configuration;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.internal.NetState;
import com.tejas.engine.internal.NetworkElement.Demand;
import com.tejas.engine.internal.NetworkElement.Link;
import com.tejas.engine.internal.NetworkElement.Network;
import com.tejas.engine.internal.NetworkElement.Node;
import com.tejas.engine.internal.NetworkElement.PathElement;
import com.tejas.engine.internal.NetworkElement.Route;
import com.tejas.engine.internal.NetworkElement.SRG;
import com.tejas.engine.internal.NetworkElement.Segment;
import com.tejas.engine.libraries.CandidatePathList;
import com.tejas.engine.libraries.GraphUtils;
import com.tejas.engine.utils.Constants;
import com.tejas.engine.utils.DoubleUtils;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.StringUtils;

import java.awt.geom.Point2D;
import java.io.*;
import java.util.Map.Entry;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * <p>Class defining a complete network structure.</p>
 *
 * <p>An unmodifiable version of NetPlan object can be obtained through the <b>copy</b> constructor. Instances work transparently as NetPlan object
 * unless you try to change it. Calling any method that can potentially change the network (e.g. add/set methods) throws an <code>UnsupportedOperationException</code>.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public final class NetPlan extends NetState
{
	private List<Node> nodes;
	private List<Link> links;
	private List<Demand> demands;
	private List<Route> routes;
	private List<Segment> segments;
	private List<SRG> srgs;
	private Network networkElement;

	/**
	 * Returns the identifier of the ingress node of the route.
	 *
	 * @param routeId Route identifier
	 * @return Ingress node identifier
	 * @since 0.2.0
	 */
	public int getRouteIngressNode(int routeId)
	{
		int demandId = getRouteDemand(routeId);

		return getDemandIngressNode(demandId);
	}

	/**
	 * Returns the identifier of the egress node of the route.
	 *
	 * @param routeId Route identifier
	 * @return Egress node identifier
	 * @since 0.2.0
	 */
	public int getRouteEgressNode(int routeId)
	{
		int demandId = getRouteDemand(routeId);

		return getDemandEgressNode(demandId);
	}

	/**
	 * Returns <code>true</code> if the network is empty (no nodes/links/demands...). It is equivalent to <code>!{@link #hasNodes hasNodes()}</code>.
	 *
	 * @return <code>true</code> if the network is empty, and <code>false</code> otherwise
	 * @since 0.2.0
	 */
	public boolean isEmpty()
	{
		return !hasNodes();
	}

	/**
	 * Removes a set of nodes.
	 *
	 * @param nodeIds Node identifiers
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeNodes(int[] nodeIds)
	{
		checkIsModifiable();

		int[] aux = IntUtils.copy(nodeIds);
		Arrays.sort(aux);

		for(int i = aux.length - 1; i >= 0; i--)
			removeNode(aux[i]);
	}

	/**
	 * Removes a set of links.
	 *
	 * @param linkIds Link identifiers
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeLinks(int[] linkIds)
	{
		checkIsModifiable();

		int[] aux = IntUtils.copy(linkIds);
		Arrays.sort(aux);

		for(int i = aux.length - 1; i >= 0; i--)
			removeLink(aux[i]);
	}

	/**
	 * Removes a set of demands.
	 *
	 * @param demandIds Demand identifiers
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeDemands(int[] demandIds)
	{
		checkIsModifiable();

		int[] aux = IntUtils.copy(demandIds);
		Arrays.sort(aux);

		for(int i = aux.length - 1; i >= 0; i--)
			removeDemand(aux[i]);
	}

	/**
	 * Removes a set of routes.
	 *
	 * @param routeIds Route identifiers
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeRoutes(int[] routeIds)
	{
		checkIsModifiable();

		int[] aux = IntUtils.copy(routeIds);
		Arrays.sort(aux);

		for(int i = aux.length - 1; i >= 0; i--)
			removeRoute(aux[i]);
	}

	/**
	 * Removes a set of protection segments.
	 *
	 * @param segmentIds Protection segment identifiers
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeProtectionSegments(int[] segmentIds)
	{
		checkIsModifiable();

		int[] aux = IntUtils.copy(segmentIds);
		Arrays.sort(aux);

		for(int i = aux.length - 1; i >= 0; i--)
			removeProtectionSegment(aux[i]);
	}

	/**
	 * Returns the routes going from a node to other one.
	 *
	 * @param node1 Ingress node
	 * @param node2 Egress node
	 * @return Route vector
	 * @since 0.2.0
	 */
	public int[] getNodePairRoutes(int node1, int node2)
	{
		Set<Integer> routeSet = new TreeSet<Integer>();
		routeSet.addAll(IntUtils.toList(getNodeOutgoingRoutes(node1)));
		routeSet.retainAll(IntUtils.toList(getNodeIncomingRoutes(node2)));

		return IntUtils.toArray(routeSet);
	}

	/**
	 * Returns links between two nodes (in both senses).
	 *
	 * @param node1 Node 1
	 * @param node2 Node 2
	 * @return Link vector
	 * @since 0.2.0
	 */
	public int[] getNodePairBidirectionalLinks(int node1, int node2)
	{
		int[] downstreamLinks = getNodePairLinks(node1, node2);
		int[] upstreamLinks = getNodePairLinks(node2, node1);

		return IntUtils.union(downstreamLinks, upstreamLinks);
	}

	/**
	 * Returns the links from a node to other one.
	 *
	 * @param node1 Origin node
	 * @param node2 Destination node
	 * @return Link vector
	 * @since 0.2.0
	 */
	public int[] getNodePairLinks(int node1, int node2)
	{
		Set<Integer> linkSet = new TreeSet<Integer>();
		linkSet.addAll(IntUtils.toList(getNodeOutgoingLinks(node1)));
		linkSet.retainAll(IntUtils.toList(getNodeIncomingLinks(node2)));

		return IntUtils.toArray(linkSet);
	}

	/**
	 * Returns the list with the sequence of links for each route.
	 *
	 * @return List of paths
	 * @since 0.2.0
	 */
	public List<int[]> getRouteAllSequenceOfLinks()
	{
		int R = getNumberOfRoutes();
		List<int[]> seqLinks = new LinkedList<int[]>();
		for(int routeId = 0; routeId < R; routeId++)
			seqLinks.add(getRouteSequenceOfLinks(routeId));

		return seqLinks;
	}

	/**
	 * Returns the list with the sequence of links for each protection segment.
	 *
	 * @return List of paths
	 * @since 0.2.0
	 */
	public List<int[]> getProtectionSegmentAllSequenceOfLinks()
	{
		int S = getNumberOfProtectionSegments();
		List<int[]> seqLinks = new LinkedList<int[]>();
		for(int segmentId = 0; segmentId < S; segmentId++)
			seqLinks.add(getProtectionSegmentSequenceOfLinks(segmentId));

		return seqLinks;
	}

	/**
	 * <p>Sets the traffic demands from a given traffic matrix, removing any previous
	 * demand.</p>
	 * 
	 * <p><b>Important</b>: If there are less nodes than those in the traffic matrix
	 * as nodes as needed will be added in position (0,0).</p>
	 * 
	 * @param trafficMatrix Traffic matrix
	 * @since 0.2.2
	 */
	public void setTrafficMatrix(double[][] trafficMatrix)
	{
		checkIsModifiable();

		removeAllDemands();

		int N = getNumberOfNodes();
		int N_aux = trafficMatrix.length;

		if (N < N_aux)
			for(int nodeId = N; nodeId < N_aux; nodeId++)
				addNode(0, 0, null, null);

		N = Math.max(N, N_aux);

		for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
		{
			for(int egressNodeId = 0; egressNodeId < N; egressNodeId++)
			{
				if (ingressNodeId == egressNodeId || ingressNodeId >= N_aux || egressNodeId >= N_aux || trafficMatrix[ingressNodeId][egressNodeId] == 0) continue;

				addDemand(ingressNodeId, egressNodeId, trafficMatrix[ingressNodeId][egressNodeId], null);
			}
		}
	}

	/**
	 * Returns the traffic matrix.
	 *
	 * @return Traffic matrix
	 * @since 0.2.0
	 */
	public double[][] getTrafficMatrix()
	{
		int N = getNumberOfNodes();
		int D = getNumberOfDemands();
		double[][] trafficMatrix = new double[N][N];

		for(int demandId = 0; demandId < D; demandId++)
		{
			int ingressNodeId = getDemandIngressNode(demandId);
			int egressNodeId = getDemandEgressNode(demandId);

			trafficMatrix[ingressNodeId][egressNodeId] += getDemandOfferedTrafficInErlangs(demandId);
		}

		return trafficMatrix;
	}

	/**
	 * Returns a vector indicating whether or not a protection segment is dedicated.
	 *
	 * @return Boolean vector
	 * @since 0.2.0
	 */
	public boolean[] getProtectionSegmentIsDedicatedVector()
	{
		int R = getNumberOfRoutes();
		int S = getNumberOfProtectionSegments();

		int[] counter = new int[S];

		for(int routeId = 0; routeId < R; routeId++)
			for(int segmentId : getRouteBackupSegmentList(routeId))
				counter[segmentId]++;

		boolean[] isDedicated = new boolean[S];
		for(int segmentId = 0; segmentId < S; segmentId++)
		{
			isDedicated[segmentId] = counter[segmentId] == 1 ? true : false;
		}

		return isDedicated;
	}

	/**
	 * Checks whether or not a protection segment is dedicated.
	 *
	 * @param segmentId Protection segment identifier
	 * @return <code>true</code> if the protection segment is dedicated. Otherwise, <code>false</code>
	 * @since 0.2.0
	 */
	public boolean getProtectionSegmentIsDedicated(int segmentId)
	{
		int R = getNumberOfRoutes();

		int counter = 0;
		for(int routeId = 0; routeId < R; routeId++)
		{
			int[] segmentIds_thisRoute = getRouteBackupSegmentList(routeId);
			if (IntUtils.contains(segmentIds_thisRoute, segmentId))
			{
				counter++;
				if (counter > 1) return false;
			}
		}

		return true;
	}

	/**
	 * Returns the utilization of a given link, including the capacity reserved
	 * for protection segments as a part of the carried traffic.
	 *
	 * @param linkId Link identifier
	 * @return Link utilization
	 * @since 0.2.0
	 */
	public double getLinkUtilization(int linkId)
	{
		double u_e = getLinkCapacityInErlangs(linkId);
		double y_e = getLinkCarriedTrafficInErlangs(linkId);
		double rho_e = (y_e + getLinkCapacityReservedForProtectionInErlangs(linkId)) / u_e;
		if (Double.isNaN(rho_e)) rho_e = 0;

		return rho_e;
	}

	/**
	 * Returns the utilization of a given link, not including the capacity reserved
	 * for protection segments as a part of the carried traffic.
	 *
	 * @param linkId Link identifier
	 * @return Link utilization
	 * @since 0.2.0
	 */
	public double getLinkUtilizationWithoutConsiderReservedBandwidthForProtection(int linkId)
	{
		double u_e = getLinkCapacityInErlangs(linkId);
		double y_e = getLinkCarriedTrafficInErlangs(linkId);
		if (y_e == 0) return 0;
		double p_e = y_e / u_e;

		return p_e;
	}

	/**
	 * Returns the maximum link utilization among all links.
	 *
	 * @return Maximum link utilization
	 * @since 0.2.0
	 */
	public double getLinkMaximumUtilization()
	{
		return DoubleUtils.maxValue(getLinkUtilizationVector());
	}

	/**
	 * Returns the maximum link utilization among all links, not including reserved bandwidth
	 * for protection.
	 *
	 * @return Maximum link utilization
	 * @since 0.2.0
	 */
	public double getLinkMaximumUtilizationWithoutConsiderReservedBandwidthForProtection()
	{
		return DoubleUtils.maxValue(getLinkUtilizationWithoutConsiderReservedBandwidthForProtectionVector());
	}

	/**
	 * Returns the link utilization vector.
	 *
	 * @return Link utilization vector
	 * @since 0.2.0
	 */
	public double[] getLinkUtilizationVector()
	{
		int E = getNumberOfLinks();
		double[] p_e = new double[E];

		for(int linkId = 0; linkId < E; linkId++)
		{
			p_e[linkId] = getLinkUtilization(linkId);
		}

		return p_e;
	}

	/**
	 * Returns the link utilization vector, not including reserved bandwidth
	 * for protection.
	 *
	 * @return Link utilization vector
	 * @since 0.2.0
	 */
	public double[] getLinkUtilizationWithoutConsiderReservedBandwidthForProtectionVector()
	{
		int E = getNumberOfLinks();
		double[] p_e = new double[E];

		for(int linkId = 0; linkId < E; linkId++)
		{
			p_e[linkId] = getLinkUtilizationWithoutConsiderReservedBandwidthForProtection(linkId);
		}

		return p_e;
	}

	@Override
	public NetPlan unmodifiableView()
	{
		NetPlan newNetPlan = new NetPlan();
		newNetPlan.isModifiable = false;
		newNetPlan.demands = demands;
		newNetPlan.links = links;
		newNetPlan.networkElement = networkElement;
		newNetPlan.nodes = nodes;
		newNetPlan.routes = routes;
		newNetPlan.segments = segments;
		newNetPlan.srgs = srgs;

		return newNetPlan;
	}

	/**
	 * Gets all the demands from a given ingress node to a given egress node.
	 *
	 * @param ingressNode Ingress node
	 * @param egressNode Egress node
	 * @return Set of demands
	 * @since 0.2.0
	 */
	public int[] getNodePairDemands(int ingressNode, int egressNode)
	{
		Set<Integer> demandSet = new TreeSet<Integer>();
		demandSet.addAll(IntUtils.toList(getNodeOutgoingDemands(ingressNode)));
		demandSet.retainAll(IntUtils.toList(getNodeIncomingDemands(egressNode)));

		return IntUtils.toArray(demandSet);
	}

	/**
	 * Returns the capacity available for each link.
	 *
	 * @return Capacity available vector
	 * @since 0.2.0
	 */
	public double[] getLinkSpareCapacityInErlangsVector()
	{
		int E = getNumberOfLinks();

		double[] u_e = getLinkCapacityNotReservedForProtectionInErlangsVector();
		double[] y_e = getLinkCarriedTrafficInErlangsVector();
		double[] spareCapacity = new double[E];

		for(int linkId = 0; linkId < E; linkId++)
		{
			spareCapacity[linkId] = Math.max(u_e[linkId] - y_e[linkId], 0);
		}

		return spareCapacity;
	}

	/**
	 * Returns the carried traffic by a given link.
	 *
	 * @param linkId Link identifier
	 * @return Carried traffic in Erlangs
	 * @since 0.2.3
	 */
	public double getLinkCarriedTrafficInErlangs(int linkId)
	{
		int[] routeIds = getLinkTraversingRoutes(linkId);
		double carriedTraffic = 0;

		for(int routeId : routeIds)
			carriedTraffic += getRouteCarriedTrafficInErlangs(routeId);

		return carriedTraffic;
	}

	/**
	 * Returns the carried traffic per link.
	 *
	 * @return Carried traffic per link
	 * @since 0.2.0
	 */
	public double[] getLinkCarriedTrafficInErlangsVector()
	{
		int E = getNumberOfLinks();
		int R = getNumberOfRoutes();

		double[] y_e = new double[E];

		for (int routeId = 0; routeId < R; routeId++)
		{
			int[] sequenceOfLinks = getRouteSequenceOfLinks(routeId);
			for (int linkId : sequenceOfLinks)
			{
				y_e[linkId] += getRouteCarriedTrafficInErlangs(routeId);
			}
		}

		return y_e;
	}

	/**
	 * Returns the carried traffic for a given demand.
	 *
	 * @param demandId Demand identifier
	 * @return Carried traffic in Erlangs
	 * @since 0.2.0
	 */
	public double getDemandCarriedTrafficInErlangs(int demandId)
	{
		int[] routeIds = getDemandRoutes(demandId);
		double carriedTraffic = 0;

		for(int routeId : routeIds)
			carriedTraffic += getRouteCarriedTrafficInErlangs(routeId);

		return carriedTraffic;
	}

	/**
	 * Returns the carried traffic for each demand.
	 *
	 * @return Carried traffic in Erlangs vector
	 * @since 0.2.0
	 */
	public double[] getDemandCarriedTrafficInErlangsVector()
	{
		int D = getNumberOfDemands();
		int R = getNumberOfRoutes();

		double[] carriedTraffic = new double[D];

		for (int routeId = 0; routeId < R; routeId++)
		{
			int demandId = getRouteDemand(routeId);
			carriedTraffic[demandId] += getRouteCarriedTrafficInErlangs(routeId);
		}

		return carriedTraffic;
	}

	/**
	 * Returns the egress traffic per node.
	 *
	 * @return Egress traffic per node
	 * @since 0.2.0
	 */
	public double[] getNodeEgressTrafficInErlangsVector()
	{
		int N = getNumberOfNodes();
		int R = getNumberOfRoutes();

		double[] egressTraffic = new double[N];
		for (int routeId = 0; routeId < R; routeId++)
		{
			int[] sequenceOfNodes = getRouteSequenceOfNodes(routeId);

			egressTraffic[sequenceOfNodes[sequenceOfNodes.length - 1]] += getRouteCarriedTrafficInErlangs(routeId);
		}

		return egressTraffic;
	}

	/**
	 * Returns the ingress traffic per node.
	 *
	 * @return Ingress traffic per node
	 * @since 0.2.0
	 */
	public double[] getNodeIngressTrafficInErlangsVector()
	{
		int N = getNumberOfNodes();
		int R = getNumberOfRoutes();

		double[] ingressTraffic = new double[N];
		for (int routeId = 0; routeId < R; routeId++)
		{
			int[] sequenceOfNodes = getRouteSequenceOfNodes(routeId);

			ingressTraffic[sequenceOfNodes[0]] += getRouteCarriedTrafficInErlangs(routeId);
		}

		return ingressTraffic;
	}

	/**
	 * Returns the traversing traffic (no ingress, no egress) per node.
	 *
	 * @return Traversing traffic per node
	 * @since 0.2.0
	 */
	public double[] getNodeTraversingTrafficInErlangsVector()
	{
		int N = getNumberOfNodes();
		int R = getNumberOfRoutes();

		double[] traversingTraffic = new double[N];
		for (int routeId = 0; routeId < R; routeId++)
		{
			int[] sequenceOfNodes = getRouteSequenceOfNodes(routeId);
			double carriedTraffic = getRouteCarriedTrafficInErlangs(routeId);

			for (int seqId = 1; seqId < sequenceOfNodes.length - 1; seqId++)
			{
				traversingTraffic[sequenceOfNodes[seqId]] += carriedTraffic;
			}
		}

		return traversingTraffic;
	}

	/**
	 * Resets the network. Removes nodes, links, demands, routes, protection segments and SRGs.
	 *
	 * @since 0.2.0
	 */
	@Override
	public void reset()
	{
		checkIsModifiable();

		nodes.clear();
		links.clear();
		demands.clear();
		routes.clear();
		segments.clear();
		srgs.clear();

		networkElement.name = "";
		networkElement.description = "";
		networkElement.setAttributes(null);
	}

	/**
	 * <p>Removes all the links defined within the network.</p>
	 *
	 * <p><b>Important</b>: All the traffic routes and protection segments will be also removed.</p>
	 *
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeAllLinks()
	{
		checkIsModifiable();

		links.clear();
		routes.clear();
		segments.clear();

		for(SRG srg : srgs)
			srg.links.clear();
	}

	/**
	 * <p>Removes all the nodes defined within the network.</p>
	 *
	 * <p><b>Important</b>: All the links, traffic demands, traffic routes and protection segments will be also removed.</p>
	 *
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeAllNodes()
	{
		checkIsModifiable();

		nodes.clear();
		links.clear();
		demands.clear();
		routes.clear();
		segments.clear();

		for(SRG srg : srgs)
			srg.nodes.clear();
	}

	/**
	 * <p>Removes all the protection segments defined within the network.</p>
	 *
	 * <p><b>Important</b>: All the list of protection segments from traffic routes will be cleared.</p>
	 *
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeAllProtectionSegments()
	{
		checkIsModifiable();

		int R = getNumberOfRoutes();

		for(int routeId = 0; routeId < R; routeId++)
		{
			int[] backupSegments = getRouteBackupSegmentList(routeId);
			for(int segmentId : backupSegments)
				removeProtectionSegmentFromRouteBackupSegmentList(segmentId, routeId);
		}

		segments.clear();
	}

	/**
	 * Gets the physical distance between a node pair.
	 *
	 * @param nodeId1 First node
	 * @param nodeId2 Second node
	 * @return Physical distance between the node pair
	 * @since 0.2.0
	 */
	public double getNodePairPhysicalDistance(int nodeId1, int nodeId2)
	{
		Point2D source = new Point2D.Double(getNodeXYPosition(nodeId1)[0], getNodeXYPosition(nodeId1)[1]);
		Point2D target = new Point2D.Double(getNodeXYPosition(nodeId2)[0], getNodeXYPosition(nodeId2)[1]);
		return source.distance(target);
	}

	/**
	 * Gets all the links which has a given node as origin or destination.
	 *
	 * @param nodeId Node identifier
	 * @return Set of links
	 * @since 0.2.0
	 */
	public int[] getNodeTraversingLinks(int nodeId)
	{
		Set<Integer> linkSet = new TreeSet<Integer>();
		linkSet.addAll(IntUtils.toList(getNodeIncomingLinks(nodeId)));
		linkSet.addAll(IntUtils.toList(getNodeOutgoingLinks(nodeId)));

		return IntUtils.toArray(linkSet);
	}

	/**
	 * Default constructor.
	 *
	 * @since 0.2.0
	 */
	public NetPlan()
	{
		nodes = new ArrayList<Node>();
		links = new ArrayList<Link>();
		demands = new ArrayList<Demand>();
		routes = new ArrayList<Route>();
		segments = new ArrayList<Segment>();
		srgs = new ArrayList<SRG>();

		networkElement = new Network();

		isModifiable = true;

		reset();
	}

	/**
	 * <p>Adds to the current network plan the traffic demand set from other network plan.</p>
	 *
	 * <p><b>Important</b>: Any previous route will be removed.</p>
	 *
	 * @param demands Network plan containing a set of demands
	 * @since 0.2.0
	 */
	public void addDemandsFrom(NetPlan demands)
	{
		int D = demands.getNumberOfDemands();

		removeAllDemands();
		removeAllRoutes();

		for (int demandId = 0; demandId < D; demandId++)
			addDemand(demands.getDemandIngressNode(demandId), demands.getDemandEgressNode(demandId), demands.getDemandOfferedTrafficInErlangs(demandId), demands.getDemandSpecificAttributes(demandId));
	}

	/**
	 * Generates a new network structure from a given <code>.n2p</code> file.
	 *
	 * @param file .n2p file
	 * @since 0.2.0
	 */
	public NetPlan(File file)
	{
		this();

		try
		{
			InputStream inputStream= new FileInputStream(file);
			Reader reader = new InputStreamReader(inputStream, "UTF-8");

			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(is);
			doc.getDocumentElement().normalize();

			if (!doc.getDocumentElement().getNodeName().equals("network"))
			{
				throw new Exception("Root element must be 'network'");
			}

			// Get network properties
			NamedNodeMap networkAttributesMap = doc.getDocumentElement().getAttributes();
			int numNetworkAttrib = networkAttributesMap.getLength();
			for (int attribId = 0; attribId < numNetworkAttrib; attribId++)
			{
				String attribName = networkAttributesMap.item(attribId).getNodeName();
				String attribValue = networkAttributesMap.item(attribId).getNodeValue();

				switch (attribName)
				{
				case "name":
					networkElement.name = attribValue;
					break;
				case "description":
					networkElement.description = attribValue;
					break;
				default:
					setNetworkAttribute(attribName, attribValue);
					break;
				}
			}

			// Get physical topology
			NodeList phys = doc.getElementsByTagName("physicalTopology");
			switch (phys.getLength())
			{
			case 0:

				break;

			case 1:

				// Get nodes and links
				NodeList nodeList = ((Element) phys.item(0)).getElementsByTagName("node");
				NodeList linkList = ((Element) phys.item(0)).getElementsByTagName("link");

				int N = nodeList.getLength();
				int E = linkList.getLength();

				for (int i = 0; i < N; i++)
				{
					Element node = ((Element) nodeList.item(i));

					Map<String, String> nodeAttributes = new HashMap<String, String>();
					String nodeName = "Node " + i;
					double xCoord = 0;
					double yCoord = 0;

					NamedNodeMap nodeAttributesMap = node.getAttributes();
					int numAttributes = nodeAttributesMap.getLength();
					for (int attribId = 0; attribId < numAttributes; attribId++)
					{
						String attribName = nodeAttributesMap.item(attribId).getNodeName();
						String attribValue = nodeAttributesMap.item(attribId).getNodeValue();

						switch (attribName)
						{
						case "name":
							nodeName = attribValue;
							break;
						case "xCoord":
							xCoord = Double.parseDouble(attribValue);
							break;
						case "yCoord":
							yCoord = Double.parseDouble(attribValue);
							break;
						default:
							nodeAttributes.put(attribName, attribValue);
							break;
						}
					}

					addNode(xCoord, yCoord, nodeName, nodeAttributes);
				}

				for (int i = 0; i < E; i++)
				{
					Element link = ((Element) linkList.item(i));

					Map<String, String> linkAttributes = new HashMap<String, String>();
					double linkCapacityInErlangs = 0;
					double linkLengthInKm = 0;
					int originNodeId = -1;
					int destinationNodeId = -1;
					String subLink = null ;

					NamedNodeMap linkAttributesMap = link.getAttributes();
					int numAttributes = linkAttributesMap.getLength();
					for (int attribId = 0; attribId < numAttributes; attribId++)
					{
						String attribName = linkAttributesMap.item(attribId).getNodeName();
						String attribValue = linkAttributesMap.item(attribId).getNodeValue();

						switch (attribName)
						{
						case "originNodeId":
							originNodeId = Integer.parseInt(attribValue);
							break;
						case "destinationNodeId":
							destinationNodeId = Integer.parseInt(attribValue);
							break;
						case "linkCapacityInErlangs":
							linkCapacityInErlangs = Double.parseDouble(attribValue);
							break;
						case "linkLengthInKm":
							linkLengthInKm = Double.parseDouble(attribValue);
							break;
						case "subLink":
							subLink = attribValue;
						default:
							linkAttributes.put(attribName, attribValue);
							break;
						}
					}

					//modification
					addLink(originNodeId, destinationNodeId, linkCapacityInErlangs, linkLengthInKm, subLink , linkAttributes);
				}

				break;

			default:

				throw new Exception("Only one physical topology per n2p file must be present");
			}

			NodeList demandList = doc.getElementsByTagName("demandSet");
			switch (demandList.getLength())
			{
			case 0:

				break;

			case 1:
				demandList = ((Element) demandList.item(0)).getElementsByTagName("demandEntry");

				int D = demandList.getLength();

				for (int demandId = 0; demandId < D; demandId++)
				{
					Element demand = ((Element) demandList.item(demandId));

					Map<String, String> demandAttributes = new HashMap<String, String>();
					int ingressNodeId = -1;
					int egressNodeId = -1;
					double offeredTrafficInErlangs = 0;

					NamedNodeMap demandAttributesMap = demand.getAttributes();
					int numAttributes = demandAttributesMap.getLength();
					for (int attribId = 0; attribId < numAttributes; attribId++)
					{
						String attribName = demandAttributesMap.item(attribId).getNodeName();
						String attribValue = demandAttributesMap.item(attribId).getNodeValue();

						switch (attribName)
						{
						case "ingressNodeId":
							ingressNodeId = Integer.parseInt(attribValue);
							break;
						case "egressNodeId":
							egressNodeId = Integer.parseInt(attribValue);
							break;
						case "offeredTrafficInErlangs":
							offeredTrafficInErlangs = Double.parseDouble(attribValue);
							break;
						default:
							demandAttributes.put(attribName, attribValue);
							break;
						}
					}

					addDemand(ingressNodeId, egressNodeId, offeredTrafficInErlangs, demandAttributes);
				}

				break;

			default:

				throw new Exception("Only one demand set per n2p file must be present");
			}

			NodeList protectionInfo = doc.getElementsByTagName("protectionInfo");
			switch (protectionInfo.getLength())
			{
			case 0:

				break;

			case 1:

				NodeList segmentList = ((Element) protectionInfo.item(0)).getElementsByTagName("protectionSegment");
				int S = segmentList.getLength();

				for (int segmentId = 0; segmentId < S; segmentId++)
				{
					Element segment = ((Element) segmentList.item(segmentId));

					Map<String, String> segmentAttributes = new HashMap<String, String>();
					List<Integer> seqLinks = new ArrayList<Integer>();
					double reservedBandwidthInErlangs = 0;

					NamedNodeMap segmentAttributesMap = segment.getAttributes();
					int numAttributes = segmentAttributesMap.getLength();
					for (int attribId = 0; attribId < numAttributes; attribId++)
					{
						String attribName = segmentAttributesMap.item(attribId).getNodeName();
						String attribValue = segmentAttributesMap.item(attribId).getNodeValue();

						if (attribName.equals("reservedBandwidthInErlangs")) reservedBandwidthInErlangs = Double.parseDouble(attribValue);
						else segmentAttributes.put(attribName, attribValue);
					}

					NodeList linkList = segment.getElementsByTagName("linkEntry");
					int numLinks = linkList.getLength();
					for (int linkId = 0; linkId < numLinks; linkId++)
					{
						seqLinks.add(Integer.parseInt(((Element) linkList.item(linkId)).getAttribute("id")));
					}

					addProtectionSegment(IntUtils.toArray(seqLinks), reservedBandwidthInErlangs, segmentAttributes);
				}

				break;

			default:

				throw new Exception("Only one proctection segment set per n2p file must be present");
			}

			NodeList routingInfo = doc.getElementsByTagName("routingInfo");
			switch (routingInfo.getLength())
			{
			case 0:

				break;

			case 1:

				NodeList routeList = ((Element) routingInfo.item(0)).getElementsByTagName("route");

				int R = routeList.getLength();

				for (int routeId = 0; routeId < R; routeId++)
				{
					Element route = ((Element) routeList.item(routeId));

					NamedNodeMap routeMap = route.getAttributes();

					Map<String, String> routeAttributes = new HashMap<String, String>();
					int demandId = -1;
					double carriedTrafficInErlangs = 0;
					List<Integer> seqLinks = new ArrayList<Integer>();
					List<Integer> backupSegments = new ArrayList<Integer>();

					int numAttributes = routeMap.getLength();
					for (int attribId = 0; attribId < numAttributes; attribId++)
					{
						String attribName = routeMap.item(attribId).getNodeName();
						String attribValue = routeMap.item(attribId).getNodeValue();
						switch (attribName)
						{
						case "demandId":
							demandId = Integer.parseInt(attribValue);
							break;

						case "carriedTrafficInErlangs":
							carriedTrafficInErlangs = Double.parseDouble(attribValue);
							break;

						default:
							routeAttributes.put(attribName, attribValue);
							break;
						}
					}

					NodeList linkList = route.getElementsByTagName("linkEntry");
					int numLinks = linkList.getLength();
					for (int linkId = 0; linkId < numLinks; linkId++)
					{
						seqLinks.add(Integer.parseInt(((Element) linkList.item(linkId)).getAttribute("id")));
					}

					NodeList backupSegmentList = route.getElementsByTagName("protectionSegmentEntry");
					int numBackupSegments = backupSegmentList.getLength();
					for (int segmentId = 0; segmentId < numBackupSegments; segmentId++)
					{
						backupSegments.add(Integer.parseInt(((Element) backupSegmentList.item(segmentId)).getAttribute("id")));
					}

					addRoute(demandId, carriedTrafficInErlangs, IntUtils.toArray(seqLinks), IntUtils.toArray(backupSegments), routeAttributes);
				}

				break;

			default:

				throw new Exception("Only one route set per n2p file must be present");
			}

			NodeList srgInfo = doc.getElementsByTagName("srgInfo");
			switch (srgInfo.getLength())
			{
			case 0:

				break;

			case 1:

				NodeList srgList = ((Element) srgInfo.item(0)).getElementsByTagName("srg");

				int numSRGs = srgList.getLength();

				for (int srgId = 0; srgId < numSRGs; srgId++)
				{
					Element srg = ((Element) srgList.item(srgId));

					NamedNodeMap srgMap = srg.getAttributes();

					Map<String, String> srgAttributes = new HashMap<String, String>();
					double mttf = 0;
					double mttr = 0;
					List<Integer> srgNodes = new LinkedList<Integer>();
					List<Integer> srgLinks = new LinkedList<Integer>();

					int numAttributes = srgMap.getLength();
					for (int attribId = 0; attribId < numAttributes; attribId++)
					{
						String attribName = srgMap.item(attribId).getNodeName();
						String attribValue = srgMap.item(attribId).getNodeValue();
						switch (attribName)
						{
						case "mttf":
							mttf = Double.parseDouble(attribValue);
							break;

						case "mttr":
							mttr = Double.parseDouble(attribValue);
							break;

						default:
							srgAttributes.put(attribName, attribValue);
							break;
						}
					}

					NodeList nodeList = srg.getElementsByTagName("nodeEntry");
					int numNodes = nodeList.getLength();
					for (int nodeId = 0; nodeId < numNodes; nodeId++)
						srgNodes.add(Integer.parseInt(((Element) nodeList.item(nodeId)).getAttribute("id")));

					NodeList linkList = srg.getElementsByTagName("linkEntry");
					int numLinks = linkList.getLength();
					for (int linkId = 0; linkId < numLinks; linkId++)
						srgLinks.add(Integer.parseInt(((Element) linkList.item(linkId)).getAttribute("id")));

					int[] nodeIds = IntUtils.toArray(srgNodes);
					int[] linkIds = IntUtils.toArray(srgLinks);

					addSRG(nodeIds, linkIds, mttf, mttr, srgAttributes);
				}

				break;

			default:

				throw new Exception("Only one route set per n2p file must be present");
			}
		}
		catch(Throwable e)
		{
			throw new RuntimeException(e);
		}

	}

	/**
	 * Returns a deep copy of the current design.
	 * 
	 * @return Deep copy of the current design
	 * @since 0.2.0
	 */
	@Override
	public NetPlan copy()
	{
		NetPlan netPlan = new NetPlan();
		netPlan.copyFrom(this);

		return netPlan;
	}

	/**
	 * Removes all the current information from the <code>NetPlan</code> object and copy the 
	 * information from the input <code>NetPlan</code>.
	 * 
	 * @param netPlan Network plan to be copied
	 * @since 0.2.3
	 */
	public void copyFrom(NetPlan netPlan)
	{
		if (netPlan == this) return;

		checkIsModifiable();
		reset();

		setNetworkAttributes(netPlan.getNetworkAttributes());
		setNetworkDescription(netPlan.getNetworkDescription());
		setNetworkName(netPlan.getNetworkName());

		int N = netPlan.getNumberOfNodes();
		int E = netPlan.getNumberOfLinks();
		int D = netPlan.getNumberOfDemands();
		int R = netPlan.getNumberOfRoutes();
		int S = netPlan.getNumberOfProtectionSegments();
		int numSRGs = netPlan.getNumberOfSRGs();

		// Copy node information
		for (int nodeId = 0; nodeId < N; nodeId++)
			addNode(netPlan.getNodeXYPosition(nodeId)[0], netPlan.getNodeXYPosition(nodeId)[1], netPlan.getNodeName(nodeId), netPlan.getNodeSpecificAttributes(nodeId));

		// Copy link information
		for (int linkId = 0; linkId < E; linkId++)
			addLink(netPlan.getLinkOriginNode(linkId), netPlan.getLinkDestinationNode(linkId), netPlan.getLinkCapacityInErlangs(linkId), netPlan.getLinkLengthInKm(linkId), netPlan.getSubLink(linkId),netPlan.getLinkSpecificAttributes(linkId));

		// Copy demand information
		for (int demandId = 0; demandId < D; demandId++)
			addDemand(netPlan.getDemandIngressNode(demandId), netPlan.getDemandEgressNode(demandId), netPlan.getDemandOfferedTrafficInErlangs(demandId), netPlan.getDemandSpecificAttributes(demandId));

		// Copy segment information
		for (int segmentId = 0; segmentId < S; segmentId++)
			addProtectionSegment(netPlan.getProtectionSegmentSequenceOfLinks(segmentId), netPlan.getProtectionSegmentReservedBandwidthInErlangs(segmentId), netPlan.getProtectionSegmentSpecificAttributes(segmentId));

		// Copy route information
		for (int routeId = 0; routeId < R; routeId++)
		{
			addRoute(netPlan.getRouteDemand(routeId), netPlan.getRouteCarriedTrafficInErlangs(routeId), IntUtils.copy(netPlan.getRouteSequenceOfLinks(routeId)), null, netPlan.getRouteSpecificAttributes(routeId));

			int[] segmentList = netPlan.getRouteBackupSegmentList(routeId);
			for (int segmentId : segmentList)
				addProtectionSegmentToRouteBackupSegmentList(segmentId, routeId);
		}

		for (int srgId = 0; srgId < numSRGs; srgId++)
			addSRG(netPlan.getSRGNodes(srgId), netPlan.getSRGLinks(srgId), netPlan.getSRGMeanTimeToFailInHours(srgId), netPlan.getSRGMeanTimeToRepairInHours(srgId), netPlan.getSRGSpecificAttributes(srgId));
	}

	/**
	 * <p>Adds a new traffic demand to the network.</p>
	 *
	 * <p><b>Important</b>: Self-demands are not allowed.</p>
	 *
	 * @param ingressNodeId Node identifier of the ingress node. It must be in range [0, <i>N</i>-1], where <i>N</i> is the number of nodes defined in the network
	 * @param egressNodeId Node identifier of the egress node. It must be in range [0, <i>N</i>-1], where <i>N</i> is the number of nodes defined in the network
	 * @param offeredTrafficInErlangs Offered traffic by this demand measured in Erlangs. It must be greater or equal than zero
	 * @param attributes Map for user-defined attributes. The key (String value) in the map will be the attribute name and the value (String value) will be whatever value is stored in that attribute. If <code>null</code>, it will be assumed to be an empty HashMap
	 * @return Demand identifier
	 * @since 0.2.0
	 */
	public int addDemand(int ingressNodeId, int egressNodeId, double offeredTrafficInErlangs, Map<String, String> attributes)
	{
		checkIsModifiable();

		if (ingressNodeId == egressNodeId) throw new Net2PlanException("Self-demands are not allowed");

		Node ingressNode = getNode(ingressNodeId);
		Node egressNode = getNode(egressNodeId);

		if (offeredTrafficInErlangs < 0) throw new Net2PlanException("Offered traffic must be greater or equal than zero");

		Demand demand = new Demand(ingressNode, egressNode);
		demand.offeredTrafficInErlangs = offeredTrafficInErlangs;

		demand.setAttributes(attributes);
		demands.add(demand);

		return getNumberOfDemands() - 1;
	}

	/**
	 * <p>Adds a new link to the network.</p>
	 *
	 * <p><b>Important</b>: Self-links are not allowed.</p>
	 *
	 * @param originNodeId Node identifier of the link origin. It must be in range [0, <i>N</i>-1], where <i>N</i> is the number of nodes defined in the network
	 * @param destinationNodeId Node identifier of the link destination. It must be in range [0, <i>N</i>-1], where <i>N</i> is the number of nodes defined in the network
	 * @param linkCapacityInErlangs Link capacity measured in Erlangs. It must be greater or equal than zero
	 * @param linkLengthInKm Link length measured in Erlangs. It must be greater or equal than zero. Physical distance between node pairs can be obtained through the {@link #getPhysicalDistanceBetweenNodePair getPhysicalDistanceBetweenNodePair()} method
	 * @param attributes Map for user-defined attributes. The key (String value) in the map will be the attribute name and the value (String value) will be whatever value is stored in that attribute. If <code>null</code>, it will be assumed to be an empty HashMap
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @return Link identifier
	 * @since 0.2.0
	 */
	public int addLink(int originNodeId, int destinationNodeId, double linkCapacityInErlangs, double linkLengthInKm, String subLink , Map<String, String> attributes) throws UnsupportedOperationException
	{
		checkIsModifiable();

		if (originNodeId == destinationNodeId) throw new Net2PlanException("Self-links are not allowed");

		Node originNode = getNode(originNodeId);
		Node destinationNode = getNode(destinationNodeId);

		if (linkCapacityInErlangs < 0) throw new Net2PlanException("Link capacity must be greater or equal than zero");
		if (linkLengthInKm < 0) throw new Net2PlanException("Link length must be greater or equal than zero");

		Link link = new Link(originNode, destinationNode);
		link.linkCapacityInErlangs = linkCapacityInErlangs;
		link.linkLengthInKm = linkLengthInKm;
		link.setAttributes(attributes);
		link.sublink = subLink ;
		links.add(link);

		return getNumberOfLinks() - 1;
	}

	/**
	 * Adds a new node to the network.
	 *
	 * @param x Node position in x-axis
	 * @param y Node position in y-axis
	 * @param name Node name. If <code>null</code>, it will be assume to be "Node " + node identifier
	 * @param attributes Map for user-defined attributes. The key (String value) in the map will be the attribute name and the value (String value) will be whatever value is stored in that attribute. If <code>null</code>, it will be assumed to be an empty HashMap
	 * @return Node identifier
	 * @since 0.2.0
	 */
	public int addNode(double x, double y, String name, Map<String, String> attributes)
	{
		checkIsModifiable();

		Node node = new Node();
		node.position = new Point2D.Double(x, y);
		node.name = name == null ? "Node " + getNumberOfNodes() : name;
		node.setAttributes(attributes);
		nodes.add(node);

		return getNumberOfNodes() - 1;
	}

	/**
	 * Adds traffic routes from a candidate path list. Existing routes will be removed, but defined protection segments will remain.
	 *
	 * @param cpl Candidate path list
	 * @param x_p Carried traffic per route in Erlangs. It must be greater or equal than zero
	 * @param includeUnusedRoutes Indicate whether routes carrying no traffic will be included
	 * @since 0.2.0
	 */
	public void addRoutes(CandidatePathList cpl, double[] x_p, boolean includeUnusedRoutes)
	{
		removeAllRoutes();

		double PRECISIONFACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));

		int P = cpl.getNumberOfPaths();

		for(int pathId = 0; pathId < P; pathId++)
		{
			if (x_p[pathId] < PRECISIONFACTOR && !includeUnusedRoutes) continue;

			int demandId = cpl.getDemandId(pathId);
			int[] seqLinks = cpl.getSequenceOfLinks(pathId);

			addRoute(demandId, x_p[pathId], seqLinks, null, null);
		}
	}

	/**
	 * Adds a new traffic route to the network.
	 *
	 * @param demandId Demand identifier
	 * @param carriedTrafficInErlangs Carried traffic by this route measured in Erlangs. It must be greater or equal than zero
	 * @param sequenceOfLinks Sequence of links
	 * @param backupSegmentIds Protection segment identifiers for this route. If <code>null</code>, it will be assumed to be an empty array
	 * @param attributes Map for user-defined attributes. The key (String value) in the map will be the attribute name and the value (String value) will be whatever value is stored in that attribute. If <code>null</code>, it will be assumed to be an empty HashMap
	 * @return Route identifier
	 * @since 0.2.0
	 */
	public int addRoute(int demandId, double carriedTrafficInErlangs, int[] sequenceOfLinks, int[] backupSegmentIds, Map<String, String> attributes)
	{
		checkIsModifiable();

		Demand demand = getDemand(demandId);

		if (carriedTrafficInErlangs < 0) throw new Net2PlanException("Carried traffic must be greater or equal than zero");

		if (IntFactory1D.dense.make(sequenceOfLinks).getMinLocation()[0] < 0)
		{
			throw new Net2PlanException("Link identifiers must be greater or equal than zero");
		}

		checkRouteValidityForDemand(sequenceOfLinks, demandId);

		List<Link> plannedRoute = new LinkedList<Link>();
		for (int linkId : sequenceOfLinks) plannedRoute.add(getLink(linkId));

		Route route = new Route(demand, plannedRoute, carriedTrafficInErlangs);

		if (backupSegmentIds == null) backupSegmentIds = new int[0];

		for (int segmentId : backupSegmentIds)
		{
			Segment segment = getSegment(segmentId);
			assignProtectionSegmentToRoute(segment, route);
		}
		route.setAttributes(attributes);

		routes.add(route);

		return getNumberOfRoutes() - 1;
	}

	/**
	 * Returns the propagation delay in a given protection segment.
	 *
	 * @param segmentId Segment identifier
	 * @return Propagation delay
	 * @since 0.2.0
	 */
	public double getProtectionSegmentPropagationDelayInSeconds(int segmentId)
	{
		double v_prop = Double.parseDouble(Configuration.getOption("propagationSpeedInKmPerSecond"));
		if (v_prop <= 0) return 0;

		return getProtectionSegmentLengthInKm(segmentId) / v_prop;
	}

	/**
	 * Returns the propagation delay for each segment.
	 *
	 * @return Propagation delay vector
	 * @since 0.2.0
	 */
	public double[] getProtectionSegmentPropagationDelayInSecondsVector()
	{
		double v_prop = Double.parseDouble(Configuration.getOption("propagationSpeedInKmPerSecond"));
		if (v_prop <= 0) return DoubleUtils.zeros(getNumberOfProtectionSegments());

		return DoubleUtils.divide(getProtectionSegmentLengthInKmVector(), v_prop);
	}

	/**
	 * Returns the propagation delay in a given route.
	 *
	 * @param routeId Route identifier
	 * @return Propagation delay
	 * @since 0.2.0
	 */
	public double getRoutePropagationDelayInSeconds(int routeId)
	{
		double v_prop = Double.parseDouble(Configuration.getOption("propagationSpeedInKmPerSecond"));
		if (v_prop <= 0) return 0;

		return getRouteLengthInKm(routeId) / v_prop;
	}

	/**
	 * Returns the propagation delay for each route.
	 *
	 * @return Propagation delay vector
	 * @since 0.2.0
	 */
	public double[] getRoutePropagationDelayInSecondsVector()
	{
		double v_prop = Double.parseDouble(Configuration.getOption("propagationSpeedInKmPerSecond"));
		if (v_prop <= 0) return DoubleUtils.zeros(getNumberOfRoutes());

		return DoubleUtils.divide(getRouteLengthInKmVector(), v_prop);
	}

	/**
	 * Returns the propagation delay in a given link.
	 *
	 * @param linkId Link identifier
	 * @return Propagation delay
	 * @since 0.2.0
	 */
	public double getLinkPropagationDelayInSeconds(int linkId)
	{
		double v_prop = Double.parseDouble(Configuration.getOption("propagationSpeedInKmPerSecond"));
		if (v_prop <= 0) return 0;

		return getLinkLengthInKm(linkId) / v_prop;
	}

	/**
	 * Returns the propagation delay for each link.
	 *
	 * @return Propagation delay vector
	 * @since 0.2.0
	 */
	public double[] getLinkPropagationDelayInSecondsVector()
	{
		double v_prop = Double.parseDouble(Configuration.getOption("propagationSpeedInKmPerSecond"));
		if (v_prop <= 0) return DoubleUtils.zeros(getNumberOfLinks());

		return DoubleUtils.divide(getLinkLengthInKmVector(), v_prop);
	}

	/**
	 * Adds a protection segment to the list of backup protection segments of a route.
	 *
	 * @param segmentId Segment identifier
	 * @param routeId Route identifier
	 * @since 0.2.0
	 */
	public void addProtectionSegmentToRouteBackupSegmentList(int segmentId, int routeId)
	{
		checkIsModifiable();

		Route route = getRoute(routeId);
		Segment segment = getSegment(segmentId);

		assignProtectionSegmentToRoute(segment, route);
	}

	/**
	 * Sets the carried traffic by a route.
	 *
	 * @param routeId Route identifier
	 * @param carriedTrafficInErlangs Carried traffic in Erlangs
	 * @since 0.2.0
	 */
	public void setRouteCarriedTrafficInErlangs(int routeId, double carriedTrafficInErlangs)
	{
		checkIsModifiable();

		if (carriedTrafficInErlangs < 0) throw new Net2PlanException("Carried traffic must be greater or equal than zero");

		Route route = getRoute(routeId);
		route.carriedTrafficInErlangs = carriedTrafficInErlangs;
	}

	private void assignProtectionSegmentToRoute(Segment segment, Route route)
	{
		checkIsModifiable();

		if (!isSegmentApplicableToRoute(route, segment))
			throw new Net2PlanException("Segment is not applicable to route");

		route.backupSegments.add(segment);
	}

	/**
	 * Adds a new protection segment to the network. To assign a protection segment
	 * to a route the {@link #addProtectionSegmentToRouteBackupSegmentList(int, int) addProtectionSegmentToRouteBackupSegmentList()} 
	 * method must be used.
	 *
	 * @param sequenceOfLinks Sequence of links
	 * @param reservedBandwidthInErlangs Reserved bandwidth in Erlangs
	 * @param attributes Map for user-defined attributes. The key (String value) in the map will be the attribute name and the value (String value) will be whatever value is stored in that attribute. If <code>null</code>, it will be assumed to be an empty HashMap
	 * @return Protection segment identifier
	 * @since 0.2.0
	 */
	public int addProtectionSegment(int[] sequenceOfLinks, double reservedBandwidthInErlangs, Map<String, String> attributes)
	{
		checkIsModifiable();

		int[][] linkTable = getLinkTable();
		GraphUtils.checkRouteContinuity(linkTable, sequenceOfLinks, GraphUtils.CheckRoutingCycleType.NO_CHECK);

		List<Link> linkList = new LinkedList<Link>();
		for (int linkId : sequenceOfLinks) linkList.add(getLink(linkId));

		if (reservedBandwidthInErlangs < 0) throw new Net2PlanException("Reserved bandwidth must be greater or equal than zero");

		Segment segment = new Segment(linkList, reservedBandwidthInErlangs);
		segment.setAttributes(attributes);
		segments.add(segment);

		return segments.size() - 1;
	}

	@Override
	public void checkValidity(Map<String, String> net2planParameters, boolean allowLinkOversubscription, boolean allowExcessCarriedTraffic)
	{
		checkValidityAndWarnings(net2planParameters, allowLinkOversubscription, allowExcessCarriedTraffic);
	}

	/**
	 * Checks the validity of the current network plan.
	 *
	 * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
	 * @param allowLinkOversubscription <code>true</code> if link capacity constraint may be violated. Otherwise, <code>false</code>
	 * @param allowExcessCarriedTraffic <code>true</code> if carried traffic may be greater than the offered one for some demand. Otherwise, <code>false</code>
	 * @return Array of warnings
	 * @since 0.2.0
	 */
	public String[] checkValidityAndWarnings(Map<String, String> net2planParameters, boolean allowLinkOversubscription, boolean allowExcessCarriedTraffic)
	{
		double PRECISIONFACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
		double[] u_e = getLinkCapacityInErlangsVector();

		List<String> warnings = new LinkedList<String>();

		if (!hasNodes()) warnings.add("Node set is not defined");

		if (!hasLinks()) warnings.add("Link set is not defined");
		else
		{
			if (DoubleUtils.maxValue(u_e) < PRECISIONFACTOR)
				warnings.add("All the links have 0 capacity");
		}
		if (!hasDemands())
		{
			warnings.add("Traffic demand set is not defined");
		}
		if (!hasRoutes())
		{
			warnings.add("Routing is not defined");
		}
		else
		{
			int E = getNumberOfLinks();
			int D = getNumberOfDemands();
			int R = getNumberOfRoutes();

			boolean someLinksOvercongested = false;
			boolean someLinksCongested = false;
			boolean routingCycles = false;
			boolean trafficLosses = false;

			double[] h_d = getDemandOfferedTrafficInErlangsVector();
			double[] r_d = getDemandCarriedTrafficInErlangsVector();
			double[] y_e = getLinkCarriedTrafficInErlangsVector();

			double[] r_e = getLinkCapacityReservedForProtectionInErlangsVector();
			double[] rho_e = getLinkUtilizationVector();

			for (int routeId = 0; routeId < R; routeId++)
			{
				int[] sequenceOfNodes = getRouteSequenceOfNodes(routeId);
				int[] uniqueNodeIds = IntUtils.unique(sequenceOfNodes);

				if (uniqueNodeIds.length != sequenceOfNodes.length)
				{
					routingCycles = true;
					break;
				}
			}

			for (int linkId = 0; linkId < E; linkId++)
			{
				if (DoubleUtils.isEqualWithinRelativeTolerance(rho_e[linkId], 1, PRECISIONFACTOR))
				{
					if (u_e[linkId] > PRECISIONFACTOR) someLinksCongested = true;
				}
				else
				{
					if (rho_e[linkId] > 1)
					{
						if (!allowLinkOversubscription)
							throw new Net2PlanException(String.format("Link capacity constraint violated for link %d (carried = %f E, reserved for protection = %f E, capacity = %f E)", linkId, y_e[linkId], r_e[linkId], u_e[linkId]));

						someLinksOvercongested = true;
					}
				}

				if (allowLinkOversubscription && (someLinksCongested && someLinksOvercongested)) break;
			}

			if (someLinksOvercongested) warnings.add("Some links are overcongested (carried traffic > link capacity)");
			if (someLinksCongested) warnings.add("Some links have 100% utilization");
			if (routingCycles) warnings.add("Some routes have cycles");

			for (int demandId = 0; demandId < D; demandId++)
			{
				if (h_d[demandId] < r_d[demandId] - PRECISIONFACTOR)
				{
					if (!allowExcessCarriedTraffic) throw new Net2PlanException(String.format("Carried traffic for demand %d overcomes the offered traffic (offered = %f E, carried = %f E)", demandId, h_d[demandId], r_d[demandId]));
				}

				if (h_d[demandId] > r_d[demandId] && !DoubleUtils.isEqualWithinRelativeTolerance(h_d[demandId], r_d[demandId], PRECISIONFACTOR))
					trafficLosses = true;

				if (allowExcessCarriedTraffic && trafficLosses) break;
			}

			if (trafficLosses) warnings.add("Traffic losses: Not all the traffic is being carried");
		}

		if (warnings.isEmpty())
		{
			warnings.add("Design is successfully completed!");
		}

		return warnings.toArray(new String[warnings.size()]);
	}

	/**
	 * Checks if a sequence of links is valid for a given demand.
	 *
	 * @param demandId Demand identifier
	 * @param sequenceOfLinks Sequence of links
	 * @since 0.2.0
	 */
	public void checkRouteValidityForDemand(int[] sequenceOfLinks, int demandId)
	{
		int[][] linkTable = getLinkTable();
		GraphUtils.checkRouteContinuity(linkTable, sequenceOfLinks, GraphUtils.CheckRoutingCycleType.NO_CHECK);

		int ingressNodeId = getDemandIngressNode(demandId);
		int egressNodeId = getDemandEgressNode(demandId);

		int originNodeIdFirstLink = getLinkOriginNode(sequenceOfLinks[0]);
		int destinationNodeIdLastLink = getLinkDestinationNode(sequenceOfLinks[sequenceOfLinks.length - 1]);

		if (ingressNodeId != originNodeIdFirstLink)
			throw new Net2PlanException("Ingress node of the demand and origin node of the first link in the route doesn't match");

		if (egressNodeId != destinationNodeIdLastLink)
			throw new Net2PlanException("Egress node of the demand and destination node of the last link in the route doesn't match");
	}

	private Demand getDemand(int demandId)
	{
		try
		{
			return demands.get(demandId);
		}
		catch (Exception ex)
		{
			throw new Net2PlanException(String.format("Demand %d is not defined in the network", demandId));
		}
	}

	/**
	 * Returns the value for an attribute of the given demand. If that attribute doesn't exist a <code>null</code> value will be returned
	 *
	 * @param demandId Demand identifier
	 * @param key Attribute name
	 * @return Returns the value for an attribute of the given demand
	 * @since 0.2.0
	 */
	public String getDemandAttribute(int demandId, String key)
	{
		Demand demand = getDemand(demandId);
		String value = demand.getAttribute(key);
		return value == null ? networkElement.getAttribute(key) : value;
	}

	/**
	 * Returns the value of an attribute for each demand.
	 *
	 * @param key Attribute name
	 * @return Attribute value vector
	 * @since 0.2.0
	 */
	public String[] getDemandAttributeVector(String key)
	{
		int D = getNumberOfDemands();
		String[] demandAttributeVector = new String[D];
		for (int demandId = 0; demandId < D; demandId++)
		{
			demandAttributeVector[demandId] = getDemandAttribute(demandId, key);
		}
		return demandAttributeVector;
	}

	/**
	 * Returns the attributes defined for a given demand.
	 *
	 * @param demandId Demand identifier
	 * @return Demand attributes
	 * @since 0.2.0
	 */
	public Map<String, String> getDemandSpecificAttributes(int demandId)
	{
		Demand demand = getDemand(demandId);
		return demand.getAttributes();
	}

	/**
	 * Returns the identifier of the egress node of the demand.
	 *
	 * @param demandId Demand identifier
	 * @return Egress node identifier
	 * @since 0.2.0
	 */
	public int getDemandEgressNode(int demandId)
	{
		Demand demand = getDemand(demandId);
		return getId(demand.egressNode);
	}

	/**
	 * Returns the identifier of the egress node for every demand.
	 *
	 * @return Egress node identifiers
	 * @since 0.2.0
	 */
	public int[] getDemandEgressNodeVector()
	{
		int D = getNumberOfDemands();
		int[] b_d = new int[D];
		for(int demandId = 0; demandId < D; demandId++)
			b_d[demandId] = getDemandEgressNode(demandId);

		return b_d;
	}

	/**
	 * Returns the identifier of the ingress node of the demand.
	 *
	 * @param demandId Demand identifier
	 * @return Ingress node identifier
	 * @since 0.2.0
	 */
	public int getDemandIngressNode(int demandId)
	{
		Demand demand = getDemand(demandId);
		return getId(demand.ingressNode);
	}

	/**
	 * Returns the identifier of the ingress node for every demand.
	 *
	 * @return Ingress node identifiers
	 * @since 0.2.0
	 */
	public int[] getDemandIngressNodeVector()
	{
		int D = getNumberOfDemands();
		int[] a_d = new int[D];
		for(int demandId = 0; demandId < D; demandId++)
			a_d[demandId] = getDemandIngressNode(demandId);

		return a_d;
	}

	/**
	 * Returns the offered traffic for a given demand.
	 *
	 * @param demandId Demand identifier
	 * @return Offered traffic in Erlangs
	 * @since 0.2.0
	 */
	public double getDemandOfferedTrafficInErlangs(int demandId)
	{
		Demand demand = getDemand(demandId);
		return demand.offeredTrafficInErlangs;
	}

	/**
	 * Returns the offered traffic per demand vector.
	 *
	 * @return Offered traffic in Erlangs vector
	 * @since 0.2.0
	 */
	public double[] getDemandOfferedTrafficInErlangsVector()
	{
		int D = getNumberOfDemands();
		double[] h_d = new double[D];

		for (int demandId = 0; demandId < D; demandId++)
		{
			h_d[demandId] = getDemandOfferedTrafficInErlangs(demandId);
		}

		return h_d;
	}

	/**
	 * Returns the <i>D</i>x<i>2</i> demand table, in which each row represent the ingress and egress node of that demand.
	 *
	 * @return The demand table
	 * @since 0.2.0
	 */
	public int[][] getDemandTable()
	{
		int D = getNumberOfDemands();
		int[][] demandTable = new int[D][2];

		for (int demandId = 0; demandId < D; demandId++)
		{
			demandTable[demandId][0] = getDemandIngressNode(demandId);
			demandTable[demandId][1] = getDemandEgressNode(demandId);
		}

		return demandTable;
	}

	/**
	 * Returns the <i>N</i>x<i>N</i> physical Euclidean-distance matrix, where <i>N</i> is the number of nodes within the network.
	 *
	 * @return The Eucledian distance matrix
	 * @since 0.2.2
	 */
	public double[][] getPhysicalDistanceMatrix()
	{
		int N = getNumberOfNodes();

		double[][] distanceMatrix = new double[N][N];
		for(int originNodeId = 0; originNodeId < N; originNodeId++)
		{
			for(int destinationNodeId = 0; destinationNodeId < N; destinationNodeId++)
			{
				if (originNodeId == destinationNodeId) continue;

				distanceMatrix[originNodeId][destinationNodeId] = getNodePairPhysicalDistance(originNodeId, destinationNodeId);
			}
		}

		return distanceMatrix;
	}

	private int getId(Object elem)
	{
		if (elem instanceof Node) return nodes.indexOf(elem);
		else if (elem instanceof Link) return links.indexOf(elem);
		else if (elem instanceof Demand) return demands.indexOf(elem);
		else if (elem instanceof Route) return routes.indexOf(elem);
		else if (elem instanceof Segment) return segments.indexOf(elem);
		else if (elem instanceof SRG) return srgs.indexOf(elem);

		throw new RuntimeException("Bad");
	}

	private List<Integer> getIds(Collection list)
	{
		List<Integer> ids = new LinkedList<Integer>();
		Iterator it = list.iterator();
		while (it.hasNext()) ids.add(getId(it.next()));

		return ids;
	}

	private Link getLink(int linkId)
	{
		try
		{
			return links.get(linkId);
		}
		catch (Exception ex)
		{
			throw new Net2PlanException(String.format("Link %d is not present in the network", linkId));
		}
	}

	/**
	 * Returns the value of a given attribute for a link. If not defined,
	 * it is search for the network.
	 *
	 * @param linkId Link identifier
	 * @param key Attribute name
	 * @return Attribute value (or null, if not defined)
	 * @since 0.2.0
	 */
	public String getLinkAttribute(int linkId, String key)
	{
		Link link = getLink(linkId);
		String value = link.getAttribute(key);
		return value == null ? networkElement.getAttribute(key) : value;
	}

	/**
	 * Returns the value of an attribute for each link.
	 *
	 * @param key Attribute name
	 * @return Attribute values
	 * @since 0.2.0
	 */
	public String[] getLinkAttributeVector(String key)
	{
		int E = getNumberOfLinks();
		String[] linkAttributeVector = new String[E];
		for (int linkId = 0; linkId < E; linkId++)
		{
			linkAttributeVector[linkId] = getLinkAttribute(linkId, key);
		}
		return linkAttributeVector;
	}

	/**
	 * Returns the attributes of a given link.
	 *
	 * @param linkId Link identifier
	 * @return Attributes
	 * @since 0.2.0
	 */
	public Map<String, String> getLinkSpecificAttributes(int linkId)
	{
		Link link = getLink(linkId);
		return link.getAttributes();
	}

	// Returns the subLink name 
	
	public String getSubLink(int linkId)
	{
		Link link = getLink(linkId);
		return link.sublink;
	}
	
	
	
	/**
	 * Returns the link capacity.
	 *
	 * @param linkId Link identifier
	 * @return Link capacity in Erlangs
	 * @since 0.2.0
	 */
	public double getLinkCapacityInErlangs(int linkId)
	{
		Link link = getLink(linkId);
		return link.linkCapacityInErlangs;
	}

	/**
	 * Returns the link capacity reserved for protection.
	 *
	 * @param linkId Link identifier
	 * @return Link capacity reserved for protection
	 * @since 0.2.0
	 */
	public double getLinkCapacityReservedForProtectionInErlangs(int linkId)
	{
		double u_e = 0;
		int[] segmentList = getLinkTraversingProtectionSegments(linkId);
		for (int segmentId : segmentList)
		{
			u_e += getProtectionSegmentReservedBandwidthInErlangs(segmentId);
		}

		return u_e;
	}

	/**
	 * Returns the link capacity reserved for protection for each link.
	 *
	 * @return Link capacity vector reserved for protection
	 * @since 0.2.0
	 */
	public double[] getLinkCapacityReservedForProtectionInErlangsVector()
	{
		int E = getNumberOfLinks();
		double[] u_e = new double[E];
		for (int linkId = 0; linkId < E; linkId++)
		{
			u_e[linkId] = getLinkCapacityReservedForProtectionInErlangs(linkId);
		}

		return u_e;
	}

	/**
	 * Returns the link capacity not reserved for protection.
	 *
	 * @param linkId Link identifier
	 * @return Link capacity not reserved for protection
	 * @since 0.2.0
	 */
	public double getLinkCapacityNotReservedForProtectionInErlangs(int linkId)
	{
		return getLinkCapacityInErlangs(linkId) - getLinkCapacityReservedForProtectionInErlangs(linkId);
	}

	/**
	 * Returns the link capacity not reserved for protection for each link.
	 *
	 * @return Link capacity vector not reserved for protection
	 * @since 0.2.0
	 */
	public double[] getLinkCapacityNotReservedForProtectionInErlangsVector()
	{
		int E = getNumberOfLinks();
		double[] u_e = new double[E];
		for (int linkId = 0; linkId < E; linkId++)
		{
			u_e[linkId] = getLinkCapacityNotReservedForProtectionInErlangs(linkId);
		}

		return u_e;
	}

	/**
	 * Returns the link capacity for each link.
	 *
	 * @return Link capacities in Erlangs
	 * @since 0.2.0
	 */
	public double[] getLinkCapacityInErlangsVector()
	{
		int E = getNumberOfLinks();
		double[] u_e = new double[E];

		for (int linkId = 0; linkId < E; linkId++)
		{
			u_e[linkId] = getLinkCapacityInErlangs(linkId);
		}

		return u_e;
	}

	/**
	 * Returns the identifier of the destination node of the link.
	 *
	 * @param linkId Link identifier
	 * @return Destination node identifier
	 * @since 0.2.0
	 */
	public int getLinkDestinationNode(int linkId)
	{
		Link link = getLink(linkId);
		return getId(link.destinationNode);
	}

	/**
	 * Returns the length of the specified link.
	 *
	 * @param linkId Link identifier
	 * @return Link length (measured in km)
	 * @since 0.2.0
	 */
	public double getLinkLengthInKm(int linkId)
	{
		Link link = getLink(linkId);
		return link.linkLengthInKm;
	}

	/**
	 * Returns the length for each link.
	 *
	 * @return Link lengths (measured in km)
	 * @since 0.2.0
	 */
	public double[] getLinkLengthInKmVector()
	{
		int E = getNumberOfLinks();
		double[] l_e = new double[E];

		for (int linkId = 0; linkId < E; linkId++)
		{
			l_e[linkId] = getLinkLengthInKm(linkId);
		}

		return l_e;
	}

	/**
	 * Returns the identifier of the origin node of the link.
	 *
	 * @param linkId Link identifier
	 * @return Origin node identifier
	 * @since 0.2.0
	 */
	public int getLinkOriginNode(int linkId)
	{
		Link link = getLink(linkId);
		return getId(link.originNode);
	}

	/**
	 * Returns the <i>E</i>x<i>2</i> link table, in which each row represent the origin and destination node of that link.
	 *
	 * @return The link table
	 * @since 0.2.0
	 */
	public int[][] getLinkTable()
	{
		int E = getNumberOfLinks();
		int[][] linkTable = new int[E][2];

		for (int linkId = 0; linkId < E; linkId++)
		{
			linkTable[linkId][0] = getLinkOriginNode(linkId);
			linkTable[linkId][1] = getLinkDestinationNode(linkId);
		}

		return linkTable;
	}

	/**
	 * Returns the value of a network attribute.
	 *
	 * @param key Attribute name
	 * @return Attribute value
	 * @since 0.2.0
	 */
	public String getNetworkAttribute(String key)
	{
		return networkElement.getAttribute(key);
	}

	/**
	 * Returns the network attributes.
	 *
	 * @return Attributes
	 * @since 0.2.0
	 */
	public Map<String, String> getNetworkAttributes()
	{
		return networkElement.getAttributes();
	}

	/**
	 * Returns the network description.
	 *
	 * @return Network description
	 * @since 0.2.3
	 */
	public String getNetworkDescription() { return networkElement.description; }

	/**
	 * Returns the network name.
	 *
	 * @return Network name
	 * @since 0.2.3
	 */
	public String getNetworkName() { return networkElement.name; }

	private Node getNode(int nodeId)
	{
		try
		{
			return nodes.get(nodeId);
		}
		catch (Exception ex)
		{
			throw new Net2PlanException(String.format("Node %d is not present in the network", nodeId));
		}
	}

	/**
	 * Returns the value of a given attribute for a node. If not defined,
	 * it is search for the network.
	 *
	 * @param nodeId Node identifier
	 * @param key Attribute name
	 * @return Attribute value (or null, if not defined)
	 * @since 0.2.0
	 */
	public String getNodeAttribute(int nodeId, String key)
	{
		Node node = getNode(nodeId);
		String value = node.getAttribute(key);
		return value == null ? networkElement.getAttribute(key) : value;
	}

	/**
	 * Returns the value of an attribute for each node.
	 *
	 * @param key Attribute name
	 * @return Attribute values
	 * @since 0.2.0
	 */
	public String[] getNodeAttributeVector(String key)
	{
		int N = getNumberOfNodes();
		String[] nodeAttributeVector = new String[N];
		for (int nodeId = 0; nodeId < N; nodeId++)
		{
			nodeAttributeVector[nodeId] = getNodeAttribute(nodeId, key);
		}
		return nodeAttributeVector;
	}

	/**
	 * Returns the attributes defined for a given node.
	 *
	 * @param nodeId Node identifier
	 * @return Node attributes
	 * @since 0.2.0
	 */
	public Map<String, String> getNodeSpecificAttributes(int nodeId)
	{
		Node node = getNode(nodeId);
		return node.getAttributes();
	}

	/**
	 * Returns the links starting from a given node.
	 *
	 * @param nodeId Node identifiers
	 * @return Route identifiers
	 * @since 0.2.0
	 */
	public int[] getNodeOutgoingRoutes(int nodeId)
	{
		List<Integer> outgoingRouteIds = new LinkedList<Integer>();
		int[] demandIds = getNodeOutgoingDemands(nodeId);
		for(int demandId : demandIds)
		{
			int[] routeIds = getDemandRoutes(demandId);
			for(int routeId : routeIds)
			{
				outgoingRouteIds.add(routeId);
			}
		}

		return IntUtils.toArray(outgoingRouteIds);
	}

	/**
	 * Returns the routes ending in a given node.
	 *
	 * @param nodeId Node identifiers
	 * @return Route identifiers
	 * @since 0.2.0
	 */
	public int[] getNodeIncomingRoutes(int nodeId)
	{
		List<Integer> incomingRouteIds = new LinkedList<Integer>();
		int[] demandIds = getNodeIncomingDemands(nodeId);
		for(int demandId : demandIds)
		{
			int[] routeIds = getDemandRoutes(demandId);
			for(int routeId : routeIds)
			{
				incomingRouteIds.add(routeId);
			}
		}

		return IntUtils.toArray(incomingRouteIds);
	}

	/**
	 * Returns the demands ending in a given node.
	 *
	 * @param nodeId Node identifiers
	 * @return Demand identifiers
	 * @since 0.2.0
	 */
	public int[] getNodeIncomingDemands(int nodeId)
	{
		List<Integer> egressDemandIds = new LinkedList<Integer>();

		int D = getNumberOfDemands();
		for (int demandId = 0; demandId < D; demandId++)
		{
			if (getDemandEgressNode(demandId) == nodeId)
			{
				egressDemandIds.add(demandId);
			}
		}

		return IntUtils.toArray(egressDemandIds);
	}

	/**
	 * Returns the links ending in a given node.
	 *
	 * @param nodeId Node identifiers
	 * @return Link identifiers
	 * @since 0.2.0
	 */
	public int[] getNodeIncomingLinks(int nodeId)
	{
		List<Integer> incomingLinkIds = new LinkedList<Integer>();

		int E = getNumberOfLinks();
		for (int linkId = 0; linkId < E; linkId++)
		{
			if (getLinkDestinationNode(linkId) == nodeId)
			{
				incomingLinkIds.add(linkId);
			}
		}

		return IntUtils.toArray(incomingLinkIds);
	}

	/**
	 * Returns the demands starting from a given node.
	 *
	 * @param nodeId Node identifiers
	 * @return Demand identifiers
	 * @since 0.2.0
	 */
	public int[] getNodeOutgoingDemands(int nodeId)
	{
		List<Integer> ingressDemandIds = new LinkedList<Integer>();

		int D = getNumberOfDemands();
		for (int demandId = 0; demandId < D; demandId++)
		{
			if (getDemandIngressNode(demandId) == nodeId)
			{
				ingressDemandIds.add(demandId);
			}
		}

		return IntUtils.toArray(ingressDemandIds);
	}

	/**
	 * Returns the node name.
	 *
	 * @param nodeId Node identifier
	 * @return Node name
	 * @since 0.2.0
	 */
	public String getNodeName(int nodeId)
	{
		Node node = getNode(nodeId);
		return node.name;
	}

	/**
	 * Returns the node name for a set of nodes.
	 *
	 * @param nodeIds Node identifiers
	 * @return Node names
	 * @since 0.2.2
	 */
	public String[] getNodeNames(int[] nodeIds)
	{
		String[] nodeNames = new String[nodeIds.length];
		for(int i = 0; i < nodeIds.length; i++)
			nodeNames[i] = getNodeName(nodeIds[i]);

		return nodeNames;
	}

	/**
	 * Returns the name for each node.
	 *
	 * @return Node names
	 * @since 0.2.0
	 */
	public String[] getNodeNameVector()
	{
		int N = getNumberOfNodes();
		String[] nodeNameVector = new String[N];
		for (int nodeId = 0; nodeId < N; nodeId++)
		{
			nodeNameVector[nodeId] = getNodeName(nodeId);
		}
		return nodeNameVector;
	}

	/**
	 * Returns the links starting from a given node.
	 *
	 * @param nodeId Node identifiers
	 * @return Link identifiers
	 * @since 0.2.0
	 */
	public int[] getNodeOutgoingLinks(int nodeId)
	{
		List<Integer> outgoingLinkIds = new LinkedList<Integer>();

		int E = getNumberOfLinks();
		for (int linkId = 0; linkId < E; linkId++)
		{
			if (getLinkOriginNode(linkId) == nodeId)
			{
				outgoingLinkIds.add(linkId);
			}
		}

		return IntUtils.toArray(outgoingLinkIds);
	}

	/**
	 * Returns the node position in a XY-plane.
	 *
	 * @param nodeId Node identifier
	 * @return XY-coordinates
	 * @since 0.2.0
	 */
	public double[] getNodeXYPosition(int nodeId)
	{
		Node node = getNode(nodeId);
		return new double[] { node.position.getX(), node.position.getY() };
	}

	/**
	 * Returns the <i>N</i>x<i>2</i> node position table, in which each row represent the node position in a 2D plane.
	 *
	 * @return The node XY position table
	 * @since 0.2.0
	 */
	public double[][] getNodeXYPositionTable()
	{
		int N = getNumberOfNodes();
		double[][] nodeXYPositionTable = new double[N][2];

		for (int nodeId = 0; nodeId < N; nodeId++)
		{
			nodeXYPositionTable[nodeId][0] = getNodeXYPosition(nodeId)[0];
			nodeXYPositionTable[nodeId][1] = getNodeXYPosition(nodeId)[1];
		}

		return nodeXYPositionTable;
	}

	/**
	 * Returns the number of traffic demands defined within the network.
	 *
	 * @return The number of traffic demands defined within the network
	 * @since 0.2.0
	 */
	public int getNumberOfDemands()
	{
		return demands.size();
	}

	/**
	 * Returns the number of unidirectional links defined within the network.
	 *
	 * @return The number of unidirectional links defined within the network
	 * @since 0.2.0
	 */
	public int getNumberOfLinks()
	{
		return links.size();
	}

	/**
	 * Returns the number of nodes defined within the network.
	 *
	 * @return The number of nodes defined within the network
	 * @since 0.2.0
	 */
	public int getNumberOfNodes()
	{
		return nodes.size();
	}

	/**
	 * Returns the number of routes for traffic demands defined within the network.
	 *
	 * @return The number of routes for traffic demands defined within the network
	 * @since 0.2.0
	 */
	public int getNumberOfRoutes()
	{
		return routes.size();
	}

	/**
	 * Returns the number of protection segments defined within the network.
	 *
	 * @return The number of protection segments defined within the network
	 * @since 0.2.0
	 */
	public int getNumberOfProtectionSegments()
	{
		return segments.size();
	}

	private Route getRoute(int routeId)
	{
		try
		{
			return routes.get(routeId);
		}
		catch (Exception ex)
		{
			throw new Net2PlanException(String.format("Route %d is not defined in the network", routeId));
		}
	}

	/**
	 * Returns the value of a given attribute for a route. If not defined,
	 * it is search for the network.
	 *
	 * @param routeId Route identifier
	 * @param key Attribute name
	 * @return Attribute value (or null, if not defined)
	 * @since 0.2.0
	 */
	public String getRouteAttribute(int routeId, String key)
	{
		Route route = getRoute(routeId);
		String value = route.getAttribute(key);
		return value == null ? networkElement.getAttribute(key) : value;
	}

	/**
	 * Returns the value of an attribute for each route.
	 *
	 * @param key Attribute name
	 * @return Attribute values
	 * @since 0.2.0
	 */
	public String[] getRouteAttributeVector(String key)
	{
		int R = getNumberOfRoutes();
		String[] routeAttributeVector = new String[R];
		for (int routeId = 0; routeId < R; routeId++)
		{
			routeAttributeVector[routeId] = getRouteAttribute(routeId, key);
		}
		return routeAttributeVector;
	}

	/**
	 * Returns the attributes for a given route.
	 *
	 * @param routeId Route identifier
	 * @return Attributes
	 * @since 0.2.0
	 */
	public Map<String, String> getRouteSpecificAttributes(int routeId)
	{
		Route route = getRoute(routeId);
		return route.getAttributes();
	}

	/**
	 * Returns the protection segments defined for a given route.
	 *
	 * @param routeId Route identifier
	 * @return Protection segment identifiers
	 * @since 0.2.0
	 */
	public int[] getRouteBackupSegmentList(int routeId)
	{
		Route route = getRoute(routeId);
		List<Integer> segmentList = getIds(getRouteBackupSegments(route));
		return IntUtils.toArray(segmentList);
	}

	private List<Segment> getRouteBackupSegments(Route route)
	{
		return route.backupSegments;
	}

	/**
	 * Returns the associated demand for a given route.
	 *
	 * @param routeId Route identifier
	 * @return Demand identifier
	 * @since 0.2.0
	 */
	public int getRouteDemand(int routeId)
	{
		Route route = getRoute(routeId);
		return getId(route.demand);
	}

	/**
	 * Returns for each route its associated demand.
	 *
	 * @return Demand identifiers
	 * @since 0.2.0
	 */
	public int[] getRouteDemandVector()
	{
		int R = getNumberOfRoutes();

		int[] d_p = new int[R];

		for(int routeId = 0; routeId < R; routeId++)
			d_p[routeId] = getRouteDemand(routeId);

		return d_p;
	}

	/**
	 * Returns the routes which can carry traffic from a given demand.
	 *
	 * @param demandId Demand identifier
	 * @return Route identifiers
	 * @since 0.2.0
	 */
	public int[] getDemandRoutes(int demandId)
	{
		Demand demand = getDemand(demandId);
		List<Integer> routeList = getIds(getRoutesFromDemand(demand));
		return IntUtils.toArray(routeList);
	}

	private Set<Route> getRoutesFromDemand(Demand demand)
	{
		Set<Route> routesTraversingDemand = new HashSet<Route>();

		Iterator<Route> it = routes.iterator();
		while (it.hasNext())
		{
			Route route = it.next();
			if (route.demand == demand)
			{
				routesTraversingDemand.add(route);
			}
		}

		return routesTraversingDemand;
	}

	/**
	 * Returns the carried traffic for a given route.
	 *
	 * @param routeId Route identifier
	 * @return Carried traffic in Erlangs
	 * @since 0.2.0
	 */
	public double getRouteCarriedTrafficInErlangs(int routeId)
	{
		Route route = getRoute(routeId);
		return route.carriedTrafficInErlangs;
	}

	/**
	 * Returns the length in kilometers for a given protection segment.
	 *
	 * @param segmentId Protection segment identifier
	 * @return Length in km
	 * @since 0.2.0
	 */
	public double getProtectionSegmentLengthInKm(int segmentId)
	{
		double segmentLength = 0;
		int[] seqLinks = getProtectionSegmentSequenceOfLinks(segmentId);
		for(int linkId : seqLinks)
			segmentLength += getLinkLengthInKm(linkId);

		return segmentLength;
	}

	/**
	 * Returns the length in kilometers for each protection segment.
	 *
	 * @return Length in km vector
	 * @since 0.2.0
	 */
	public double[] getProtectionSegmentLengthInKmVector()
	{
		int S = getNumberOfProtectionSegments();
		double[] segmentLength = new double[S];
		for(int segmentId = 0; segmentId < S; segmentId++)
			segmentLength[segmentId] = getProtectionSegmentLengthInKm(segmentId);

		return segmentLength;
	}

	/**
	 * Returns the number of hops for a given protection segment.
	 *
	 * @param segmentId Protection segment identifier
	 * @return Number of hops
	 * @since 0.2.0
	 */
	public int getProtectionSegmentNumberOfHops(int segmentId)
	{
		return getProtectionSegmentSequenceOfLinks(segmentId).length;
	}

	/**
	 * Returns the number of hops for each protection segment.
	 *
	 * @return Number of hops vector
	 * @since 0.2.0
	 */
	public int[] getProtectionSegmentNumberOfHopsVector()
	{
		int S = getNumberOfProtectionSegments();
		int[] numHops = new int[S];
		for(int segmentId = 0; segmentId < S; segmentId++)
		{
			numHops[segmentId] = getProtectionSegmentNumberOfHops(segmentId);
		}

		return numHops;
	}

	/**
	 * Returns the length in kilometers for each route.
	 *
	 * @param routeId Route identifier
	 * @return Length in km
	 * @since 0.2.0
	 */
	public double getRouteLengthInKm(int routeId)
	{
		double routeLength = 0;
		int[] seqLinks = getRouteSequenceOfLinks(routeId);
		for(int linkId : seqLinks)
			routeLength += getLinkLengthInKm(linkId);

		return routeLength;
	}

	/**
	 * Returns the length in kilometers for each route.
	 *
	 * @return Length in km vector
	 * @since 0.2.0
	 */
	public double[] getRouteLengthInKmVector()
	{
		int R = getNumberOfRoutes();
		double[] routeLength = new double[R];
		for(int routeId = 0; routeId < R; routeId++)
			routeLength[routeId] = getRouteLengthInKm(routeId);

		return routeLength;
	}

	/**
	 * Returns the number of hops for a given route.
	 *
	 * @param routeId Route identifier
	 * @return Number of hops
	 * @since 0.2.0
	 */
	public int getRouteNumberOfHops(int routeId)
	{
		return getRouteSequenceOfLinks(routeId).length;
	}

	/**
	 * Returns the number of hops for each route.
	 *
	 * @return Number of hops vector
	 * @since 0.2.0
	 */
	public int[] getRouteNumberOfHopsVector()
	{
		int R = getNumberOfRoutes();
		int[] numHops = new int[R];
		for(int routeId = 0; routeId < R; routeId++)
		{
			numHops[routeId] = getRouteNumberOfHops(routeId);
		}

		return numHops;
	}

	/**
	 * Returns the fraction of traffic (from the offered traffic of the
	 * associated demand) which is carried by each route.
	 *
	 * @return Traffic fraction vector
	 * @since 0.2.0
	 */
	public double[] getRouteCarriedTrafficFractionVector()
	{
		int D = getNumberOfDemands();

		double[] f_p = getRouteCarriedTrafficInErlangsVector();
		double[] h_d = getDemandOfferedTrafficInErlangsVector();

		for(int demandId = 0; demandId < D; demandId++)
		{
			int[] routeIds = getDemandRoutes(demandId);

			if (h_d[demandId] == 0) continue;

			for(int routeId : routeIds)
				f_p[routeId] /= h_d[demandId];
		}

		return f_p;
	}

	/**
	 * Returns the carried traffic for each route.
	 *
	 * @return Carried traffic in Erlangs vector
	 * @since 0.2.0
	 */
	public double[] getRouteCarriedTrafficInErlangsVector()
	{
		int R = getNumberOfRoutes();
		double[] x_p = new double[R];

		for (int routeId = 0; routeId < R; routeId++)
		{
			x_p[routeId] = getRouteCarriedTrafficInErlangs(routeId);
		}

		return x_p;
	}

	// Returns sub Link name 
	
	
	public String[] getSubLinkName(int routeId)
	{
		Route route = getRoute(routeId);
		List<String> sublinks = new ArrayList<String>();
		List<Link> setofLinks = getRoutePlannedSequenceOfLinks(route);
		System.out.println(setofLinks.size());
		
		
		for(int i = 0 ; i < setofLinks.size() ; i++ )  
			
			{
			
			sublinks.add(setofLinks.get(i).sublink);
			
			
			}
		String[] stringArray = sublinks.toArray(new String[sublinks.size()]);
		return  stringArray;
	}
	
	
	/**
	 * Returns the sequence of links traversed by a route.
	 *
	 * @param routeId Route identifier
	 * @return Sequence of links
	 * @since 0.2.0
	 */
	public int[] getRouteSequenceOfLinks(int routeId)
	{
		Route route = getRoute(routeId);
		List<Integer> sequenceOfLinks = getIds(getRoutePlannedSequenceOfLinks(route));
		return IntUtils.toArray(sequenceOfLinks);
	}

	private List<Link> getRoutePlannedSequenceOfLinks(Route route)
	{
		return sequenceOfPathElements2sequenceOfLinks(route.plannedRoute);
	}

	/**
	 * Returns the sequence of nodes traversed by a route.
	 *
	 * @param routeId Route identifier
	 * @return Sequence of nodes
	 * @since 0.2.0
	 */
	public int[] getRouteSequenceOfNodes(int routeId)
	{
		Route route = getRoute(routeId);
		List<Integer> sequenceOfNodes = getIds(getRoutePlannedSequenceOfNodes(route));
		return IntUtils.toArray(sequenceOfNodes);
	}

	private List<Node> getRoutePlannedSequenceOfNodes(Route route)
	{
		return sequenceOfPathElements2sequenceOfNodes(getRoutePlannedSequenceOfLinks(route));
	}

	/**
	 * Returns the routes traversing a given link.
	 *
	 * @param linkId Link identifier
	 * @return Route identifiers
	 * @since 0.2.0
	 */
	public int[] getLinkTraversingRoutes(int linkId)
	{
		IntArrayList routeIds = new IntArrayList();

		int R = getNumberOfRoutes();
		for(int routeId = 0; routeId < R; routeId++)
			if (IntUtils.contains(getRouteSequenceOfLinks(routeId), linkId))
				routeIds.add(routeId);

		routeIds.trimToSize();
		return routeIds.elements();
	}

	private Segment getSegment(int segmentId)
	{
		try
		{
			return segments.get(segmentId);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw new Net2PlanException(String.format("Segment %d is not defined in the network", segmentId));
		}
	}

	/**
	 * Returns the value of a given attribute for a protection segment. If not defined,
	 * it is search for the network.
	 *
	 * @param segmentId Protection segment identifier
	 * @param key Attribute name
	 * @return Attribute value (or null, if not defined)
	 * @since 0.2.0
	 */
	public String getProtectionSegmentAttribute(int segmentId, String key)
	{
		Segment segment = getSegment(segmentId);
		String value = segment.getAttribute(key);
		return value == null ? networkElement.getAttribute(key) : value;
	}

	/**
	 * Returns the value of an attribute for each protection segment.
	 *
	 * @param key Attribute name
	 * @return Attribute values
	 * @since 0.2.0
	 */
	public String[] getProtectionSegmentAttributeVector(String key)
	{
		int S = getNumberOfProtectionSegments();
		String[] segmentAttributeVector = new String[S];
		for (int segmentId = 0; segmentId < S; segmentId++)
		{
			segmentAttributeVector[segmentId] = getProtectionSegmentAttribute(segmentId, key);
		}
		return segmentAttributeVector;
	}

	/**
	 * Returns the attributes for a given protection segment.
	 *
	 * @param segmentId Segment identifier
	 * @return Attributes
	 * @since 0.2.0
	 */
	public Map<String, String> getProtectionSegmentSpecificAttributes(int segmentId)
	{
		Segment segment = getSegment(segmentId);
		return segment.getAttributes();
	}

	/**
	 * Returns the identifier of the destination node of the protection segment.
	 *
	 * @param segmentId Segment identifier
	 * @return Destination node identifier
	 * @since 0.2.0
	 */
	public int getProtectionSegmentDestinationNode(int segmentId)
	{
		Segment segment = getSegment(segmentId);
		return getId(segment.getLastNode());
	}

	/**
	 * Returns the identifier of the origin node of the protection segment.
	 *
	 * @param segmentId Segment identifier
	 * @return Origin node identifier
	 * @since 0.2.0
	 */
	public int getProtectionSegmentOriginNode(int segmentId)
	{
		Segment segment = getSegment(segmentId);
		return getId(segment.getFirstNode());
	}

	/**
	 * Returns the reserved bandwidth for a given protection segment.
	 *
	 * @param segmentId Protection segment identifier
	 * @return Reserved bandwidth in Erlangs
	 * @since 0.2.0
	 */
	public double getProtectionSegmentReservedBandwidthInErlangs(int segmentId)
	{
		Segment segment = getSegment(segmentId);
		return segment.reservedBandwithInErlangs;
	}

	/**
	 * Returns the reserved bandwidth for each protection segment.
	 *
	 * @return Reserved bandwidth in Erlangs vector
	 * @since 0.2.0
	 */
	public double[] getProtectionSegmentReservedBandwithInErlangsVector()
	{
		int S = getNumberOfProtectionSegments();
		double[] reservedBandwithInErlangsVector = new double[S];
		for (int segmentId = 0; segmentId < S; segmentId++)
		{
			reservedBandwithInErlangsVector[segmentId] = getProtectionSegmentReservedBandwidthInErlangs(segmentId);
		}

		return reservedBandwithInErlangsVector;
	}

	/**
	 * Returns the routes which share a given protection segment.
	 *
	 * @param segmentId Protection segment identifier
	 * @return Route identifiers
	 * @since 0.2.2
	 */
	public int[] getProtectionSegmentRoutes(int segmentId)
	{
		int R = getNumberOfRoutes();
		IntArrayList routeIds = new IntArrayList();

		for (int routeId = 0; routeId < R; routeId++)
		{
			int[] backupSegments = getRouteBackupSegmentList(routeId);
			if (IntUtils.find(backupSegments, segmentId, Constants.SearchType.FIRST).length != 0)
				routeIds.add(routeId);
		}

		routeIds.trimToSize();

		return routeIds.elements();
	}

	/**
	 * Returns the sequence of links traversed by a protection segment.
	 *
	 * @param segmentId Protection segment identifier
	 * @return Sequence of links
	 * @since 0.2.0
	 */
	public int[] getProtectionSegmentSequenceOfLinks(int segmentId)
	{
		Segment segment = getSegment(segmentId);
		List<Integer> sequenceOfLinks = getIds(getSegmentSequenceOfLinks(segment));
		return IntUtils.toArray(sequenceOfLinks);
	}

	private List<Link> getSegmentSequenceOfLinks(Segment segment)
	{
		List<Link> sequenceOfLinks = segment.route;
		return sequenceOfLinks;
	}

	/**
	 * Returns the sequence of nodes traversed by a protection segment.
	 *
	 * @param segmentId Protection segment identifier
	 * @return Sequence of nodes
	 * @since 0.2.0
	 */
	public int[] getProtectionSegmentSequenceOfNodes(int segmentId)
	{
		Segment segment = getSegment(segmentId);
		List<Integer> sequenceOfNodes = getIds(getSegmentSequenceOfNodes(segment));
		return IntUtils.toArray(sequenceOfNodes);
	}

	private List<Node> getSegmentSequenceOfNodes(Segment segment)
	{
		return sequenceOfPathElements2sequenceOfNodes(segment.route);
	}

	/**
	 * Returns the protection segments traversing a given link.
	 *
	 * @param linkId Link identifier
	 * @return Protection segment identifiers
	 * @since 0.2.0
	 */
	public int[] getLinkTraversingProtectionSegments(int linkId)
	{
		Link link = getLink(linkId);
		List<Integer> segmentList = getIds(getSegmentsTraversingLink(link));
		return IntUtils.toArray(segmentList);
	}

	/**
	 * Returns the routes traversing a given node.
	 *
	 * @param nodeId Node identifier
	 * @return Route identifiers
	 * @since 0.2.0
	 */
	public int[] getNodeTraversingRoutes(int nodeId)
	{
		int R = getNumberOfRoutes();

		List<Integer> routeList = new LinkedList<Integer>();

		for(int routeId = 0; routeId < R; routeId++)
		{
			int[] seqNodes = getRouteSequenceOfNodes(routeId);
			if (IntUtils.contains(seqNodes, nodeId))
				routeList.add(routeId);
		}

		return IntUtils.toArray(routeList);
	}

	/**
	 * Returns the protection segments traversing a given node.
	 *
	 * @param nodeId Node identifier
	 * @return Protection segment identifiers
	 * @since 0.2.0
	 */
	public int[] getNodeTraversingProtectionSegments(int nodeId)
	{
		int S = getNumberOfProtectionSegments();

		List<Integer> segmentList = new LinkedList<Integer>();

		for(int segmentId = 0; segmentId < S; segmentId++)
		{
			int[] seqNodes = getProtectionSegmentSequenceOfNodes(segmentId);
			if (IntUtils.contains(seqNodes, nodeId))
				segmentList.add(segmentId);
		}

		return IntUtils.toArray(segmentList);
	}

	private Set<Segment> getSegmentsTraversingLink(Link link)
	{
		Set<Segment> segmentsTraversingLink = new HashSet<Segment>();
		for (Segment segment : segments)
		{
			if (segment.route.contains(link))
			{
				segmentsTraversingLink.add(segment);
			}
		}

		return segmentsTraversingLink;
	}

	/**
	 * Returns <code>true</code> if the network has at least one traffic demand. It is equivalent to <code>{@link #getNumberOfDemands getNumberOfDemands()} > 0</code>.
	 *
	 * @return <code>true</code> if there are traffic demands defined for the network, and <code>false</code> otherwise
	 * @since 0.2.0
	 */
	public boolean hasDemands()
	{
		return !demands.isEmpty();
	}

	/**
	 * Returns <code>true</code> if the network has at least one unidirectional link. It is equivalent to <code>{@link #getNumberOfLinks getNumberOfLinks()} > 0</code>.
	 * @return <code>true</code> if there are links within the network, and <code>false</code> otherwise
	 * @since 0.2.0
	 */
	public boolean hasLinks()
	{
		return !links.isEmpty();
	}

	/**
	 * Returns <code>true</code> if the network has at least one node. It is equivalent to <code>{@link #getNumberOfNodes getNumberOfNodes()} > 0</code>.
	 *
	 * @return <code>true</code> if there are nodes within the network, and <code>false</code> otherwise
	 * @since 0.2.0
	 */
	public boolean hasNodes()
	{
		return !nodes.isEmpty();
	}

	/**
	 * Returns <code>true</code> if the network has at least one traffic route. It is equivalent to <code>{@link #getNumberOfRoutes getNumberOfRoutes()} > 0</code>.
	 *
	 * @return <code>true</code> if there are routes defined for any traffic demand in the network, and <code>false</code> otherwise
	 * @since 0.2.0
	 */
	public boolean hasRoutes()
	{
		return !routes.isEmpty();
	}

	/**
	 * Returns <code>true</code> if the network has at least one protection segment. It is equivalent to <code>{@link #getNumberOfProtectionSegments getNumberOfProtectionSegments()} > 0</code>.
	 *
	 * @return <code>true</code> if there are protection segments defined within the network, and <code>false</code> otherwise
	 * @since 0.2.0
	 */
	public boolean hasProtectionSegments()
	{
		return !segments.isEmpty();
	}

	/**
	 * Tests whether a given protection segment is applicable to a route. A protection segment is applicable is contained within the route.
	 *
	 * @param segmentId Segment identifier
	 * @param routeId Route identifier
	 * @return <code>true</code> is segment is applicable to route, <code>false</code> otherwise
	 * @since 0.2.0
	 */
	public boolean checkProtectionSegmentMergeabilityToRoute(int routeId, int segmentId)
	{
		Route route = getRoute(routeId);
		Segment segment = getSegment(segmentId);

		return isSegmentApplicableToRoute(route, segment);
	}

	private static boolean isSegmentApplicableToRoute(Route route, Segment segment)
	{
		Node head = segment.getFirstNode();
		Node tail = segment.getLastNode();

		List<Node> sequenceOfNodes = sequenceOfPathElements2sequenceOfNodes(route.plannedRoute);

		int headPos = sequenceOfNodes.indexOf(head);
		int tailPos = sequenceOfNodes.lastIndexOf(tail);

		if (headPos == -1 || tailPos == -1 || tailPos <= headPos)
		{
			return false;
		}

		return true;
	}

	/**
	 * <p>Removes all the demands defined within the network.</p>
	 *
	 * <p><b>Important</b>: All the traffic routes will be also removed.</p>
	 *
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeAllDemands()
	{
		checkIsModifiable();

		removeAllRoutes();
		demands.clear();
	}

	/**
	 * Removes all the traffic routes defined within the network.
	 *
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeAllRoutes()
	{
		checkIsModifiable();

		routes.clear();
	}

	/**
	 * Removes a protection segment from the list of backup protection segments of a route.
	 *
	 * @param segmentId Segment identifier
	 * @param routeId Route identifier
	 * @since 0.2.0
	 */
	public void removeProtectionSegmentFromRouteBackupSegmentList(int segmentId, int routeId)
	{
		checkIsModifiable();

		Route route = getRoute(routeId);
		Segment segment = getSegment(segmentId);

		route.backupSegments.remove(segment);
	}

	/**
	 * <p>Removes the demand identified by the given parameter.</p>
	 *
	 * <p><b>Important</b>: Any route carrying traffic from that demand will be also removed.</p>
	 * @param demandId Identifier of the demand to be removed
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeDemand(int demandId)
	{
		checkIsModifiable();

		Demand demand = getDemand(demandId);

		int[] routesToRemove = getDemandRoutes(demandId);
		removeRoutes(routesToRemove);

		demands.remove(demand);
	}

	/**
	 * Removes a given attribute from a demand. If the attribute is not defined, no action is made.
	 *
	 * @param demandId Route identifier
	 * @param key Attribute name
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeDemandAttribute(int demandId, String key)
	{
		checkIsModifiable();

		Demand demand = getDemand(demandId);
		demand.removeAttribute(key);
	}

	/**
	 * <p>Removes the link identified by the given parameter.</p>
	 *
	 * <p><b>Important</b>: Any route or protection segment traversing that link will be also removed.</p>
	 * @param linkId Identifier of the link to be removed
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeLink(int linkId)
	{
		checkIsModifiable();

		Link link = getLink(linkId);
		int[] segmentsToRemove = getLinkTraversingProtectionSegments(linkId);
		removeProtectionSegments(segmentsToRemove);

		int[] routesToRemove = getLinkTraversingRoutes(linkId);
		removeRoutes(routesToRemove);

		int[] aux_srgs = getLinkSRGs(linkId);
		for(int srgId : aux_srgs)
			removeLinkFromSRG(linkId, srgId);

		links.remove(link);
	}

	/**
	 * Removes a given attribute from a link. If the attribute is not defined, no action is made.
	 *
	 * @param linkId Link identifier
	 * @param key Attribute name
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeLinkAttribute(int linkId, String key)
	{
		checkIsModifiable();

		Link link = getLink(linkId);
		link.removeAttribute(key);
	}

	/**
	 * Removes a given attribute from the network. If the attribute is not defined, no action is made.
	 *
	 * @param key Attribute name
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeNetworkAttribute(String key)
	{
		checkIsModifiable();

		networkElement.removeAttribute(key);
	}

	/**
	 * <p>Removes the node identified by the given parameter.</p>
	 *
	 * <p><b>Important</b>: Any link or demand referring to that node will be also removed.</p>
	 *
	 * @param nodeId Identifier of the node to be removed
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeNode(int nodeId)
	{
		checkIsModifiable();

		Node node = getNode(nodeId);

		int[] linksToRemove = getNodeTraversingLinks(nodeId);
		removeLinks(linksToRemove);

		int[] demandsToRemove = IntUtils.concatenate(getNodeIncomingDemands(nodeId), getNodeOutgoingDemands(nodeId));
		removeDemands(demandsToRemove);

		int[] aux_srgs = getNodeSRGs(nodeId);
		for(int srgId : aux_srgs)
			removeNodeFromSRG(nodeId, srgId);

		nodes.remove(node);
	}

	/**
	 * Removes a given attribute from a node. If the attribute is not defined, no action is made.
	 *
	 * @param nodeId Node identifier
	 * @param key Attribute name
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeNodeAttribute(int nodeId, String key)
	{
		checkIsModifiable();

		Node node = getNode(nodeId);
		node.removeAttribute(key);
	}

	/**
	 * Removes the traffic route identified by the given parameter.
	 *
	 * @param routeId Identifier of the route to be removed
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeRoute(int routeId)
	{
		checkIsModifiable();

		Route route = getRoute(routeId);
		routes.remove(route);
	}

	/**
	 * Removes a given attribute from a route. If the attribute is not defined, no action is made.
	 *
	 * @param routeId Route identifier
	 * @param key Attribute name
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeRouteAttribute(int routeId, String key)
	{
		checkIsModifiable();

		Route route = getRoute(routeId);
		route.removeAttribute(key);
	}

	/**
	 * <p>Removes the specified protection segment from the network.</p>
	 *
	 * <p><b>Important</b>: That segment will be removed from the list of protection segments of any associated route.</p>
	 *
	 * @param segmentId Identifier of the protection segment to be removed
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeProtectionSegment(int segmentId)
	{
		checkIsModifiable();

		Segment segment = getSegment(segmentId);

		int R = getNumberOfRoutes();
		for(int routeId = 0; routeId < R; routeId++)
			removeProtectionSegmentFromRouteBackupSegmentList(segmentId, routeId);

		segments.remove(segment);
	}

	/**
	 * Removes a given attribute from a segment. If the attribute is not defined, no action is made.
	 *
	 * @param segmentId Segment identifier
	 * @param key Attribute name
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void removeProtectionSegmentAttribute(int segmentId, String key)
	{
		checkIsModifiable();

		Segment segment = getSegment(segmentId);
		segment.removeAttribute(key);
	}

	/**
	 * Saves the current network plan to a given file.
	 *
	 * @param file .n2p file
	 * @since 0.2.0
	 */
	public void saveToFile(File file)
	{
		String filePath = file.getPath();
		if (!filePath.toLowerCase(Locale.getDefault()).endsWith(".n2p")) file = new File(filePath + ".n2p");

		int N = getNumberOfNodes();
		int E = getNumberOfLinks();
		int D = getNumberOfDemands();
		int R = getNumberOfRoutes();
		int S = getNumberOfProtectionSegments();
		int numSRGs = getNumberOfSRGs();

		if (isEmpty()) throw new Net2PlanException("Empty network structure");

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try { builder = factory.newDocumentBuilder(); }
		catch(Throwable ex) { throw new RuntimeException(ex); }

		Document doc = builder.newDocument();
		Element root = doc.createElement("network");

		root.setAttribute("name", getNetworkName());
		root.setAttribute("description", getNetworkDescription());

		for (Entry<String, String> entry : getNetworkAttributes().entrySet())
			root.setAttribute(entry.getKey(), entry.getValue());

		if (N > 0)
		{
			Element phys = doc.createElement("physicalTopology");

			for (int nodeId = 0; nodeId < N; nodeId++)
			{
				Element node = doc.createElement("node");
				//Point2D position = new Point2D.Double(getNodeXYPosition(nodeId)[0], getNodeXYPosition(nodeId)[1]);
				//node.setAttribute("xCoord", Double.toString(position.getX()));
				//node.setAttribute("yCoord", Double.toString(position.getY()));
				node.setAttribute("name", getNodeName(nodeId));
				for (Entry<String, String> entry : getNodeSpecificAttributes(nodeId).entrySet())
					node.setAttribute(entry.getKey(), entry.getValue());

				phys.appendChild(node);
			}

			for (int linkId = 0; linkId < E; linkId++)
			{
				Element link = doc.createElement("link");
				link.setAttribute("originNodeId", Integer.toString(getLinkOriginNode(linkId)));
				link.setAttribute("destinationNodeId", Integer.toString(getLinkDestinationNode(linkId)));
				link.setAttribute("linkCapacityInErlangs", Double.toString(getLinkCapacityInErlangs(linkId)));
				link.setAttribute("linkLengthInKm", Double.toString(getLinkLengthInKm(linkId)));
				link.setAttribute("LinkId", Integer.toString(linkId ));
				for (Entry<String, String> entry : getLinkSpecificAttributes(linkId).entrySet())
				{
					link.setAttribute(entry.getKey(), entry.getValue());
				}

				phys.appendChild(link);
				
			}

			root.appendChild(phys);
		}

		if (D > 0)
		{
			Element demandList = doc.createElement("demandSet");

			for (int demandId = 0; demandId < D; demandId++)
			{
				Element demand = doc.createElement("demandEntry");
				demand.setAttribute("DemandId", Integer.toString(demandId));
				demand.setAttribute("ingressNodeId", Integer.toString(getDemandIngressNode(demandId)));
				demand.setAttribute("egressNodeId", Integer.toString(getDemandEgressNode(demandId)));
				demand.setAttribute("offeredTrafficInErlangs", Double.toString(getDemandOfferedTrafficInErlangs(demandId)));
				for (Entry<String, String> entry : getDemandSpecificAttributes(demandId).entrySet())
				{
					demand.setAttribute(entry.getKey(), entry.getValue());
				}

				demandList.appendChild(demand);
			}

			root.appendChild(demandList);
		}

		if (S > 0)
		{
			Element segmentList = doc.createElement("protectionInfo");

			for (int segmentId = 0; segmentId < S; segmentId++)
			{
				Element segment = doc.createElement("protectionSegment");
				segment.setAttribute("reservedBandwidthInErlangs", Double.toString(getProtectionSegmentReservedBandwidthInErlangs(segmentId)));
				int[] sequenceOfLinks = getProtectionSegmentSequenceOfLinks(segmentId);

				for (int linkId = 0; linkId < sequenceOfLinks.length; linkId++)
				{
					Element link = doc.createElement("linkEntry");
					link.setAttribute("id", Integer.toString(sequenceOfLinks[linkId]));
					segment.appendChild(link);
				}

				for (Entry<String, String> entry : getProtectionSegmentSpecificAttributes(segmentId).entrySet())
				{
					segment.setAttribute(entry.getKey(), entry.getValue());
				}

				segmentList.appendChild(segment);
			}

			root.appendChild(segmentList);
		}

		if (R > 0)
		{
			Element routeList = doc.createElement("routingInfo");

			for (int routeId = 0; routeId < R; routeId++)
			{
				Element route = doc.createElement("route");
				
				
				
                
				int[] sequenceOfLinks = getRouteSequenceOfLinks(routeId);
				int[] backupSegments = getRouteBackupSegmentList(routeId);
				String[] subLinksNames = getSubLinkName(routeId);
				
				 //
				//Element link = doc.createElement("linkEntry");
				List<String> linkEntries = new ArrayList<String>();
				//
				
				for (int linkId = 0; linkId < sequenceOfLinks.length; linkId++)
				{
					
					 linkEntries.add(Integer.toString(sequenceOfLinks[linkId]));
					
				}
				
				String linkEntriesArray[] = new String[linkEntries.size()];
				linkEntriesArray = linkEntries.toArray(linkEntriesArray);
				
				
				
				//link.setAttribute("id", StringUtils.join(linkEntriesArray, " ")) ;
				route.setAttribute("demandId", Integer.toString(getRouteDemand(routeId)));
				route.setAttribute("carriedTrafficInErlangs", Double.toString(getRouteCarriedTrafficInErlangs(routeId)));
				route.setAttribute("LinksTravelled", StringUtils.join(linkEntriesArray, "-"));
				//route.appendChild(link);
				
				 

				
				
				
				
				for (int segmentId = 0; segmentId < backupSegments.length; segmentId++)
				{
					Element segment = doc.createElement("protectionSegmentEntry");
					segment.setAttribute("id", Integer.toString(backupSegments[segmentId]));
					route.appendChild(segment);
				}

				for (Entry<String, String> entry : getRouteSpecificAttributes(routeId).entrySet())
					route.setAttribute(entry.getKey(), entry.getValue());

				routeList.appendChild(route);
			}

			root.appendChild(routeList);
		}

		if (numSRGs > 0)
		{
			Element srgList = doc.createElement("srgInfo");

			for (int srgId = 0; srgId < numSRGs; srgId++)
			{
				Element srg = doc.createElement("srg");
				srg.setAttribute("mttf", Double.toString(getSRGMeanTimeToFailInHours(srgId)));
				srg.setAttribute("mttr", Double.toString(getSRGMeanTimeToRepairInHours(srgId)));

				int[] nodeIds = getSRGNodes(srgId);
				int[] linkIds = getSRGLinks(srgId);

				for(int nodeId : nodeIds)
				{
					Element node = doc.createElement("nodeEntry");
					node.setAttribute("id", Integer.toString(nodeId));
					srg.appendChild(node);
				}

				for(int linkId : linkIds)
				{
					Element link = doc.createElement("linkEntry");
					link.setAttribute("id", Integer.toString(linkId));
					srg.appendChild(link);
				}

				for (Entry<String, String> entry : getRouteSpecificAttributes(srgId).entrySet())
					srg.setAttribute(entry.getKey(), entry.getValue());

				srgList.appendChild(srg);
			}

			root.appendChild(srgList);
		}

		doc.appendChild(root);
		doc.setXmlVersion("1.0");
		doc.setXmlStandalone(true);
		doc.normalizeDocument();

		try
		{
			Source src = new DOMSource(doc);
			Result dst = new StreamResult(file.toURI().getPath());

			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();

			// set some options on the transformer
			transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			transformer.transform(src, dst);
		}
		catch(Throwable ex)
		{
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Converts a sequence of links and segments to a sequence of nodes.
	 *
	 * @param sequenceOfLinksAndSegments Sequence of links and segments
	 * @return Sequence of nodes
	 * @since 0.2.0
	 */
	public int[] convertSequenceOfLinks2SequenceOfNodes(int[] sequenceOfLinksAndSegments)
	{
		List<Integer> sequenceOfNodes = new LinkedList<Integer>();

		for (int itemId = 0; itemId < sequenceOfLinksAndSegments.length; itemId++)
		{
			if (sequenceOfLinksAndSegments[itemId] >= 0)
			{
				int linkId = sequenceOfLinksAndSegments[itemId];

				if (itemId == 0) sequenceOfNodes.add(getLinkOriginNode(linkId));
				sequenceOfNodes.add(getLinkDestinationNode(linkId));
			}
			else
			{
				throw new Net2PlanException("Bad - No protection segments allowed here");
			}
		}

		return IntUtils.toArray(sequenceOfNodes);
	}

	private static List<Link> sequenceOfPathElements2sequenceOfLinks(List<? extends PathElement> sequenceOfPathElements)
	{
		List<Link> sequenceOfLinks = new LinkedList<Link>();

		for (PathElement pathElement : sequenceOfPathElements)
		{
			sequenceOfLinks.addAll(pathElement.getSequenceOfLinks());
		}

		return sequenceOfLinks;
	}

	private static List<Node> sequenceOfPathElements2sequenceOfNodes(List<? extends PathElement> sequenceOfPathElements)
	{
		LinkedList<Node> sequenceOfNodes = new LinkedList<Node>();
		int ct = 0;
		for (Iterator<? extends PathElement> it = sequenceOfPathElements.iterator(); it.hasNext();)
		{
			if (ct > 0) sequenceOfNodes.removeLast();
			sequenceOfNodes.addAll(it.next().getSequenceOfNodes());
			ct++;
		}

		return sequenceOfNodes;
	}

	/**
	 * Sets an attribute for a given demand. If already exists, it will be overriden.
	 *
	 * @param demandId Demand identifier
	 * @param key   Attribute name
	 * @param value Attribute value
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setDemandAttribute(int demandId, String key, String value)
	{
		checkIsModifiable();

		Demand demand = getDemand(demandId);
		demand.setAttribute(key, value);
	}

	/**
	 * Sets the attributes for a given demand. Any previous attribute will be removed.
	 *
	 * @param demandId Demand identifier
	 * @param attributes Map of attributes
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setDemandAttributes(int demandId, Map<String, String> attributes)
	{
		checkIsModifiable();

		Demand demand = getDemand(demandId);
		demand.setAttributes(attributes);
	}

	/**
	 * Sets the offered traffic for a given demand.
	 *
	 * @param demandId Demand identifier
	 * @param offeredTrafficInErlangs Offered traffic in Erlangs
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setDemandOfferedTrafficInErlangs(int demandId, double offeredTrafficInErlangs)
	{
		checkIsModifiable();

		if (offeredTrafficInErlangs < 0) throw new Net2PlanException("Offered traffic must be greater or equal than zero");

		Demand demand = getDemand(demandId);
		demand.offeredTrafficInErlangs = offeredTrafficInErlangs;
	}

	/**
	 * Sets the offered traffic for each demand.
	 *
	 * @param offeredTrafficInErlangs Offered traffic in Erlangs
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setDemandOfferedTrafficInErlangsVector(double[] offeredTrafficInErlangs)
	{
		checkIsModifiable();

		int D = getNumberOfDemands();

		for(int demandId = 0; demandId < D; demandId++)
			setDemandOfferedTrafficInErlangs(demandId, offeredTrafficInErlangs[demandId]);
	}

	/**
	 * Sets an attribute for a given link. If already exists, it will be overriden.
	 *
	 * @param linkId Link identifier
	 * @param key   Attribute name
	 * @param value Attribute value
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setLinkAttribute(int linkId, String key, String value)
	{
		checkIsModifiable();

		Link link = getLink(linkId);
		link.setAttribute(key, value);
	}

	/**
	 * Sets the attributes for a given link. Any previous attribute will be removed.
	 *
	 * @param linkId Link identifier
	 * @param attributes Map of attributes
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setLinkAttributes(int linkId, Map<String, String> attributes)
	{
		checkIsModifiable();

		Link link = getLink(linkId);
		link.setAttributes(attributes);
	}

	/**
	 * Sets the capacity of a link.
	 *
	 * @param linkId Link identifier
	 * @param linkCapacity Link capacity in Erlangs
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setLinkCapacityInErlangs(int linkId, double linkCapacity)
	{
		checkIsModifiable();

		if (Double.isNaN(linkCapacity)) linkCapacity = 0;

		if (linkCapacity < 0) throw new Net2PlanException("Link capacity must be greater or equal than zero");

		Link link = getLink(linkId);
		link.linkCapacityInErlangs = linkCapacity;
	}

	/**
	 * Sets the length of a link.
	 *
	 * @param linkId Link identifier
	 * @param linkLengthInKm Link length measured in Erlangs. It must be greater or equal than zero. Physical distance between node pairs can be obtained through the {@link #getPhysicalDistanceBetweenNodePair getPhysicalDistanceBetweenNodePair()} method
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setLinkLengthInKm(int linkId, double linkLengthInKm)
	{
		checkIsModifiable();

		if (linkLengthInKm < 0) throw new Net2PlanException("Link length must be greater or equal than zero");

		Link link = getLink(linkId);
		link.linkLengthInKm = linkLengthInKm;
	}

	/**
	 * Sets an attribute for the network. If already exists, it will be overriden.
	 *
	 * @param key   Attribute name
	 * @param value Attribute value
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setNetworkAttribute(String key, String value)
	{
		checkIsModifiable();

		networkElement.setAttribute(key, value);
	}

	/**
	 * Sets the network attributes. Any previous attribute will be removed.
	 *
	 * @param attributes Map of attributes
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setNetworkAttributes(Map<String, String> attributes)
	{
		checkIsModifiable();

		networkElement.setAttributes(attributes);
	}

	/**
	 * Sets the network description.
	 *
	 * @param description Network description
	 * @since 0.2.3
	 */
	public void setNetworkDescription(String description)
	{
		checkIsModifiable();

		networkElement.description = description == null ? "" : description;
	}

	/**
	 * Sets the network name.
	 *
	 * @param name Network name
	 * @since 0.2.3
	 */
	public void setNetworkName(String name)
	{
		checkIsModifiable();

		networkElement.name = name == null ? "" : name;
	}

	/**
	 * Sets an attribute for a given node. If already exists, it will be overriden.
	 *
	 * @param nodeId Node identifier
	 * @param key   Attribute name
	 * @param value Attribute value
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setNodeAttribute(int nodeId, String key, String value)
	{
		checkIsModifiable();

		Node node = getNode(nodeId);
		node.setAttribute(key, value);
	}

	/**
	 * Sets the attributes for a given node. Any previous attribute will be removed.
	 *
	 * @param nodeId Node identifier
	 * @param attributes Map of attributes
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setNodeAttributes(int nodeId, Map<String, String> attributes)
	{
		checkIsModifiable();

		Node node = getNode(nodeId);
		node.setAttributes(attributes);
	}

	/**
	 * Sets the node name.
	 *
	 * @param nodeId Node identifier
	 * @param name New name
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setNodeName(int nodeId, String name)
	{
		checkIsModifiable();

		Node node = getNode(nodeId);
		node.name = name;
	}

	/**
	 * Sets the node position.
	 *
	 * @param nodeId Node identifier
	 * @param x Node position in x-axis
	 * @param y Node position in y-axis
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setNodeXYPosition(int nodeId, double x, double y)
	{
		checkIsModifiable();

		Node node = getNode(nodeId);
		node.position = new Point2D.Double(x, y);
	}

	/**
	 * Sets an attribute for a given route. If already exists, it will be overriden.
	 *
	 * @param routeId Route identifier
	 * @param key   Attribute name
	 * @param value Attribute value
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setRouteAttribute(int routeId, String key, String value)
	{
		checkIsModifiable();

		Route route = getRoute(routeId);
		route.setAttribute(key, value);
	}

	/**
	 * Sets the attributes for a given route. Any previous attribute will be removed.
	 *
	 * @param routeId Route identifier
	 * @param attributes Map of attributes
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setRouteAttributes(int routeId, Map<String, String> attributes)
	{
		checkIsModifiable();

		Route route = getRoute(routeId);
		route.setAttributes(attributes);
	}

	/**
	 * Sets an attribute for a given protection segment. If already exists, it will be overriden.
	 *
	 * @param segmentId Protection segment identifier
	 * @param key   Attribute name
	 * @param value Attribute value
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setProtectionSegmentAttribute(int segmentId, String key, String value)
	{
		checkIsModifiable();

		Segment segment = getSegment(segmentId);
		segment.setAttribute(key, value);
	}

	/**
	 * Sets the attributes for a given protection segment. Any previous attribute will be removed.
	 *
	 * @param segmentId Protection segment identifier
	 * @param attributes Map of attributes
	 * @since 0.2.0
	 */
	public void setProtectionSegmentAttributes(int segmentId, Map<String, String> attributes)
	{
		checkIsModifiable();

		Segment segment = getSegment(segmentId);
		segment.setAttributes(attributes);
	}

	/**
	 * Sets the reserved bandwidth for a given protection segment.
	 *
	 * @param segmentId Protection segment id
	 * @param reservedBandwidthInErlangs Reserved bandwidth in Erlangs
	 * @throws UnsupportedOperationException If network plan is not modifiable
	 * @since 0.2.0
	 */
	public void setProtectionSegmentReservedBandwidthInErlangs(int segmentId, double reservedBandwidthInErlangs)
	{
		checkIsModifiable();

		if (reservedBandwidthInErlangs < 0) throw new Net2PlanException("Reserved bandwidth must be greater or equal than zero");

		Segment segment = getSegment(segmentId);
		segment.reservedBandwithInErlangs = reservedBandwidthInErlangs;
	}

	/**
	 * Adds a link to a given SRG.
	 * 
	 * @param linkId Link identifier
	 * @param srgId SRG identifier
	 * @since 0.2.3
	 */
	public void addLinkToSRG(int linkId, int srgId)
	{
		checkIsModifiable();

		getSRG(srgId).links.add(getLink(linkId));
	}

	/**
	 * Adds a node to a given SRG.
	 * 
	 * @param nodeId Node identifier
	 * @param srgId SRG identifier
	 * @since 0.2.3
	 */
	public void addNodeToSRG(int nodeId, int srgId)
	{
		checkIsModifiable();

		getSRG(srgId).nodes.add(getNode(nodeId));
	}

	/**
	 * Adds a new SRG to the network.
	 * 
	 * @param nodeIds Set of node identifiers
	 * @param linkIds Set of link identifiers
	 * @param mttf Mean Time To Fail (in hours). Zero or negative value means <code>Double.MAX_VALUE</code>
	 * @param mttr Mean Time To Repair (in hours). Zero or negative value are not allowed
	 * @param attributes  Map for user-defined attributes. The key (String value) in the map will be the attribute name and the value (String value) will be whatever value is stored in that attribute. If null, it will be assumed to be an empty HashMap
	 * @return SRG identifier
	 * @since 0.2.3
	 */
	public int addSRG(int[] nodeIds, int[] linkIds, double mttf, double mttr, Map<String, String> attributes)
	{
		checkIsModifiable();

		SRG srg = new SRG();

		if (nodeIds == null) nodeIds = new int[0];
		if (linkIds == null) linkIds = new int[0];

		if (mttf <= 0) mttf = Double.MAX_VALUE;
		if (mttr <= 0) throw new Net2PlanException("'mttr' must be greater than zero");

		srg.links = new HashSet<Link>();
		srg.nodes = new HashSet<Node>();

		for(int linkId : linkIds)
			srg.links.add(getLink(linkId));

		for(int nodeId : nodeIds)
			srg.nodes.add(getNode(nodeId));

		srg.mttf = mttf;
		srg.mttr = mttr;
		srg.setAttributes(attributes);

		srgs.add(srg);

		int srgId = getNumberOfSRGs() - 1;
		return srgId;
	}

	/**
	 * Returns all SRGs associated to a given link.
	 * 
	 * @param linkId Link identifier
	 * @return SRGs associated to the link
	 * @since 0.2.3
	 */
	public int[] getLinkSRGs(int linkId)
	{
		List<Integer> out = new LinkedList<Integer>();
		int numSRGs = getNumberOfSRGs();
		for(int srgId = 0; srgId < numSRGs; srgId++)
			if (IntUtils.contains(getSRGLinks(srgId), linkId))
				out.add(srgId);

		return IntUtils.toArray(out);
	}

	/**
	 * Returns all SRGs associated to each node.
	 * 
	 * @return SRGs associated to each node
	 * @since 0.2.3
	 */
	public int[][] getLinkSRGsVector()
	{
		int E = getNumberOfLinks();
		int[][] out = new int[E][];

		for(int linkId = 0; linkId < E; linkId++)
			out[linkId] = getLinkSRGs(linkId);

		return out;
	}

	/**
	 * Returns all SRGs associated to a given node.
	 * 
	 * @param nodeId Node identifier
	 * @return SRGs associated to the node
	 * @since 0.2.3
	 */
	public int[] getNodeSRGs(int nodeId)
	{
		List<Integer> out = new LinkedList<Integer>();
		int numSRGs = getNumberOfSRGs();
		for(int srgId = 0; srgId < numSRGs; srgId++)
			if (IntUtils.contains(getSRGNodes(srgId), nodeId))
				out.add(srgId);

		return IntUtils.toArray(out);
	}

	/**
	 * Returns all SRGs associated to each node.
	 * 
	 * @return SRGs associated to each node
	 * @since 0.2.3
	 */
	public int[][] getNodeSRGsVector()
	{
		int N = getNumberOfNodes();
		int[][] out = new int[N][];

		for(int nodeId = 0; nodeId < N; nodeId++)
			out[nodeId] = getNodeSRGs(nodeId);

		return out;
	}

	/**
	 * Returns all SRGs associated to a given protection segment. It is equal to
	 * the union of the SRGs associated to each traversed node/link.
	 * 
	 * @param segmentId Segment identifier
	 * @return SRGs associated to the protection segment
	 * @since 0.2.3
	 */
	public int[] getProtectionSegmentSRGs(int segmentId)
	{
		int[] nodeIds = getProtectionSegmentSequenceOfNodes(segmentId);
		int[] linkIds = getProtectionSegmentSequenceOfLinks(segmentId);

		List<Integer> out = new LinkedList<Integer>();
		int numSRGs = getNumberOfSRGs();
		for(int srgId = 0; srgId < numSRGs; srgId++)
		{
			if (IntUtils.containsAny(getSRGNodes(srgId), nodeIds))
				out.add(srgId);

			if (IntUtils.containsAny(getSRGLinks(srgId), linkIds))
				out.add(srgId);
		}

		return IntUtils.toArray(out);
	}

	/**
	 * Returns all SRGs associated to a given route. It is equal to the union
	 * of the SRGs associated to each traversed node/link.
	 * 
	 * @param routeId Route identifier
	 * @return SRGs associated to the route
	 * @since 0.2.3
	 */
	public int[] getRouteSRGs(int routeId)
	{
		int[] nodeIds = getRouteSequenceOfNodes(routeId);
		int[] linkIds = getRouteSequenceOfLinks(routeId);

		Set<Integer> out = new HashSet<Integer>();
		int numSRGs = getNumberOfSRGs();
		for(int srgId = 0; srgId < numSRGs; srgId++)
		{
			if (IntUtils.containsAny(getSRGNodes(srgId), nodeIds))
				out.add(srgId);

			if (IntUtils.containsAny(getSRGLinks(srgId), linkIds))
				out.add(srgId);
		}

		return IntUtils.toArray(out);
	}

	/**
	 * Returns the number of SRGs defined within the network.
	 * 
	 * @return Number of SRGs
	 * @since 0.2.3
	 */
	public int getNumberOfSRGs() { return srgs.size(); }

	private SRG getSRG(int srgId) { return srgs.get(srgId); }

	/**
	 * Returns the value of a given attribute for a SRG. If not defined, it is
	 * search for the network.
	 * 
	 * @param srgId SRG identifier
	 * @param key Attribute name
	 * @return Attribute value (or null, if not defined)
	 * @since 0.2.3
	 */
	public String getSRGAttribute(int srgId, String key)
	{
		SRG srg = getSRG(srgId);
		String value = srg.getAttribute(key);
		if (value == null) value = getNetworkAttribute(key);

		return value;
	}

	/**
	 * Returns the value of an attribute for each SRG.
	 * 
	 * @param key Attribute name
	 * @return Attribute values
	 * @since 0.2.3
	 */
	public String[] getSRGAttributeVector(String key)
	{
		List<String> out = new LinkedList<String>();
		int numSRGs = getNumberOfSRGs();
		for(int srgId = 0; srgId < numSRGs; srgId++)
			out.add(getSRGAttribute(srgId, key));

		return StringUtils.toArray(out);
	}

	/**
	 * Returns the availability of the given SRG. The availability is given by 
	 * the quotient of MTTF and the mean time between failures (MTBF), which is 
	 * equal to the sum of MTTF and MTTR.
	 * 
	 * @param srgId SRG identifier
	 * @return Availability
	 * @since 0.2.3
	 */
	public double getSRGAvailability(int srgId)
	{
		double mttf = getSRGMeanTimeToFailInHours(srgId);
		if (mttf == Double.MAX_VALUE) return 1;

		double mttr = getSRGMeanTimeToRepairInHours(srgId);
		return mttf / (mttf + mttr);
	}

	/**
	 * Returns the availability for each SRG.
	 * 
	 * @return Availability per SRG
	 * @since 0.2.3
	 */
	public double[] getSRGAvailabilityVector()
	{
		List<Double> out = new LinkedList<Double>();

		int numSRGs = getNumberOfSRGs();
		for(int srgId = 0; srgId < numSRGs; srgId++)
			out.add(getSRGAvailability(srgId));

		return DoubleUtils.toArray(out);
	}

	/**
	 * Returns the set of links in a given SRG.
	 * 
	 * @param srgId SRG identifier
	 * @return Set of links in the SRG
	 * @since 0.2.3
	 */
	public int[] getSRGLinks(int srgId)
	{
		int[] srgIds = IntUtils.toArray(getIds(getSRG(srgId).links));
		Arrays.sort(srgIds);

		return srgIds;
	}

	/**
	 * Returns the set of links which are associated to each SRG.
	 *
	 * @return Set of nodes per SRG
	 * @since 0.2.3
	 */
	public int[][] getSRGLinksVector()
	{
		int numSRGs = getNumberOfSRGs();

		int[][] out = new int[numSRGs][];

		for(int srgId = 0; srgId < numSRGs; srgId++)
			out[srgId] = getSRGLinks(srgId);

		return out;
	}

	/**
	 * Returns the Mean Time To Fail (in hours) of the given SRG.
	 * 
	 * @param srgId SRG identifier
	 * @return Mean Time To Fail (in hours)
	 * @since 0.2.3
	 */
	public double getSRGMeanTimeToFailInHours(int srgId) { return getSRG(srgId).mttf; }

	/**
	 * Returns the Mean Time To Fail (in hours) for each SRG.
	 * 
	 * @return Mean Time To Fail (in hours) per SRG
	 * @since 0.2.3
	 */
	public double[] getSRGMeanTimeToFailInHoursVector()
	{
		List<Double> out = new LinkedList<Double>();

		int numSRGs = getNumberOfSRGs();
		for(int srgId = 0; srgId < numSRGs; srgId++)
			out.add(getSRGMeanTimeToFailInHours(srgId));

		return DoubleUtils.toArray(out);
	}

	/**
	 * Returns the Mean Time To Repair (in hours) of the given SRG.
	 * 
	 * @param srgId SRG identifier
	 * @return Mean Time To Repair (in hours)
	 * @since 0.2.3
	 */
	public double getSRGMeanTimeToRepairInHours(int srgId) { return getSRG(srgId).mttr; }

	/**
	 * Returns the Mean Time To Repair (in hours) for each SRG.
	 * 
	 * @return Mean Time To Repair (in hours) per SRG
	 * @since 0.2.3
	 */
	public double[] getSRGMeanTimeToRepairInHoursVector()
	{
		List<Double> out = new LinkedList<Double>();

		int numSRGs = getNumberOfSRGs();
		for(int srgId = 0; srgId < numSRGs; srgId++)
			out.add(getSRGMeanTimeToRepairInHours(srgId));

		return DoubleUtils.toArray(out);
	}

	/**
	 * Returns the set of nodes in a given SRG.
	 * 
	 * @param srgId SRG identifier
	 * @return Set of nodes in the SRG
	 * @since 0.2.3
	 */
	public int[] getSRGNodes(int srgId)
	{
		int[] srgIds = IntUtils.toArray(getIds(getSRG(srgId).nodes));
		Arrays.sort(srgIds);

		return srgIds;
	}

	/**
	 * Returns the set of nodes which are associated to each SRG.
	 *
	 * @return Set of nodes per SRG
	 * @since 0.2.3
	 */
	public int[][] getSRGNodesVector()
	{
		int F = getNumberOfSRGs();

		int[][] out = new int[F][];

		for(int srgId = 0; srgId < F; srgId++)
			out[srgId] = getSRGNodes(srgId);

		return out;
	}

	/**
	 * Returns the set of protection segments affected by a given SRG.
	 * 
	 * @param srgId SRG identifier
	 * @return Set of protection segments
	 * @since 0.2.3
	 */
	public int[] getSRGProtectionSegments(int srgId)
	{
		int[] nodeIds = getSRGNodes(srgId);
		int[] linkIds = getSRGLinks(srgId);

		Set<Integer> segmentIds = new TreeSet<Integer>();
		int S = getNumberOfProtectionSegments();
		for(int segmentId = 0; segmentId < S; segmentId++)
		{
			int[] sequenceOfLinks = getProtectionSegmentSequenceOfLinks(segmentId);
			if (IntUtils.containsAny(sequenceOfLinks, linkIds))
				segmentIds.add(segmentId);

			int[] sequenceOfNodes = getProtectionSegmentSequenceOfNodes(segmentId);
			if (IntUtils.containsAny(sequenceOfNodes, nodeIds))
				segmentIds.add(segmentId);
		}

		return IntUtils.toArray(segmentIds);
	}

	/**
	 * Returns the set of routes affected by a given SRG.
	 * 
	 * @param srgId SRG identifier
	 * @return Set of routes
	 * @since 0.2.3
	 */
	public int[] getSRGRoutes(int srgId)
	{
		int[] nodeIds = getSRGNodes(srgId);
		int[] linkIds = getSRGLinks(srgId);

		Set<Integer> routeIds = new HashSet<Integer>();
		int R = getNumberOfRoutes();
		for(int routeId = 0; routeId < R; routeId++)
		{
			int[] sequenceOfLinks = getRouteSequenceOfLinks(routeId);
			if (IntUtils.containsAny(sequenceOfLinks, linkIds))
				routeIds.add(routeId);

			int[] sequenceOfNodes = getRouteSequenceOfNodes(routeId);
			if (IntUtils.containsAny(sequenceOfNodes, nodeIds))
				routeIds.add(routeId);
		}

		return IntUtils.toArray(routeIds);
	}

	/**
	 * Returns the attributes for a given SRG.
	 * 
	 * @param srgId SRG identifier
	 * @return Attributes
	 * @since 0.2.3
	 */
	public Map<String,String> getSRGSpecificAttributes(int srgId)
	{
		SRG srg = getSRG(srgId);
		return srg.getAttributes();
	}

	/**
	 * Returns <code>true</code> if the network has at least one SRG. 
	 * It is equivalent to {@link #getNumberOfSRGs() getNumberOfSRGs()} > 0.
	 * 
	 * @return <code>true</code> if there are SRGs defined for the network, and <code>false</code> otherwise
	 * @since 0.2.3
	 */
	public boolean hasSRGs() { return getNumberOfSRGs() > 0; }

	/**
	 * Removes a link form the set of links of a given SRG.
	 * 
	 * @param linkId Link identifier
	 * @param srgId SRG identifier
	 * @since 0.2.3
	 */
	public void removeLinkFromSRG(int linkId, int srgId)
	{
		checkIsModifiable();

		getSRG(srgId).links.remove(getLink(linkId));
	}

	/**
	 * Removes a node form the set of links of a given SRG.
	 * 
	 * @param nodeId Node identifier
	 * @param srgId SRG identifier
	 * @since 0.2.3
	 */
	public void removeNodeFromSRG(int nodeId, int srgId)
	{
		checkIsModifiable();

		getSRG(srgId).nodes.remove(getNode(nodeId));
	}

	/**
	 * Removes a SRG.
	 * 
	 * @param srgId SRG identifier
	 * @since 0.2.3
	 */
	public void removeSRG(int srgId)
	{
		checkIsModifiable();

		srgs.remove(srgId);
	}

	/**
	 * Removes a set of SRGs.
	 * 
	 * @param srgIds Set of SRG identifiers
	 * @since 0.2.3
	 */
	public void removeSRGs(int[] srgIds)
	{
		checkIsModifiable();

		int[] aux = IntUtils.copy(srgIds);
		IntUtils.sort(aux, Constants.OrderingType.DESCENDING);

		for(int srgId : aux) removeSRG(srgId);
	}

	/**
	 * Removes a given attribute from a SRG. If the attribute is not defined, no action is made.
	 *
	 * @param srgId SRG identifier
	 * @param key Attribute name
	 * @since 0.2.3
	 */
	public void removeSRGAttribute(int srgId, String key)
	{
		checkIsModifiable();

		SRG srg = getSRG(srgId);
		srg.removeAttribute(key);
	}

	/**
	 * Remove all SRGs.
	 * 
	 * @since 0.2.3
	 */
	public void removeAllSRGs()
	{
		checkIsModifiable();

		srgs.clear();
	}

	/**
	 * Sets an attribute for a given srG. If already exists, it will be overriden.
	 *
	 * @param srgId SRG identifier
	 * @param key   Attribute name
	 * @param value Attribute value
	 * @since 0.2.3
	 */
	public void setSRGAttribute(int srgId, String key, String value)
	{
		checkIsModifiable();

		SRG srg = getSRG(srgId);
		srg.setAttribute(key, value);
	}

	/**
	 * Sets the attributes for a given SRG. Any previous attribute will be removed.
	 *
	 * @param srgId SRG identifier
	 * @param attributes Map of attributes
	 * @since 0.2.3
	 */
	public void setSRGAttributes(int srgId, Map<String, String> attributes)
	{
		checkIsModifiable();

		SRG srg = getSRG(srgId);
		srg.setAttributes(attributes);
	}

	/**
	 * Sets the MTTF of a given SRG.
	 * 
	 * @param srgId SRG identifier
	 * @param mttf Mean Time To Fail (in hours). Zero or negative value means <code>Double.MAX_VALUE</code>
	 * @since 0.2.3
	 */
	public void setSRGMeanTimeToFailInHours(int srgId, double mttf)
	{
		checkIsModifiable();

		SRG srg = getSRG(srgId);
		if (mttf <= 0) mttf = Double.MAX_VALUE;

		srg.mttf = mttf;
	}

	/**
	 * Sets the MTTR of a given SRG.
	 * 
	 * @param srgId SRG identifier
	 * @param mttr Mean Time To Repair (in hours). Zero or negative value are not allowed
	 * @since 0.2.3
	 */
	public void setSRGMeanTimeToRepairInHours(int srgId, double mttr)
	{
		checkIsModifiable();

		SRG srg = getSRG(srgId);
		if (mttr <= 0) throw new Net2PlanException("'mttr' must be greater than zero");

		srg.mttr = mttr;
	}

	/**
	 * Returns a String representation of the network plan.
	 *
	 * @return String representation of the network plan
	 * @since 0.2.0
	 */
	@Override
	public String toString()
	{
		StringBuilder netPlanInformation = new StringBuilder();

		Map<String, String> networkAttributes = getNetworkAttributes();
		int N = getNumberOfNodes();
		int E = getNumberOfLinks();
		int D = getNumberOfDemands();
		int S = getNumberOfProtectionSegments();
		int R = getNumberOfRoutes();
		int numSRGs = getNumberOfSRGs();

		if (networkAttributes.isEmpty() && N == 0 && E == 0 && D == 0 && S == 0 && R == 0 && numSRGs == 0)
		{
			netPlanInformation.append("Empty network");
			return netPlanInformation.toString();
		}

		String NEWLINE = String.format("%n");

		netPlanInformation.append("Network information");
		netPlanInformation.append(NEWLINE);
		netPlanInformation.append("--------------------------------");
		netPlanInformation.append(NEWLINE).append(NEWLINE);
		netPlanInformation.append("Name: ").append(getNetworkName()).append(NEWLINE);
		netPlanInformation.append("Description: ").append(getNetworkDescription()).append(NEWLINE);

		if (!networkAttributes.isEmpty())
		{
			netPlanInformation.append("Attributes: ");
			netPlanInformation.append(StringUtils.mapToString(networkAttributes, "=", ", "));
			netPlanInformation.append(NEWLINE);
		}

		netPlanInformation.append("Number of nodes: ").append(N).append(NEWLINE);
		netPlanInformation.append("Number of links: ").append(E).append(NEWLINE);
		netPlanInformation.append("Number of demands: ").append(D).append(NEWLINE);
		netPlanInformation.append("Number of routes: ").append(R).append(NEWLINE);
		netPlanInformation.append("Number of protection segments: ").append(S).append(NEWLINE);

		if (N > 0)
		{
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("Node information");
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("--------------------------------");
			netPlanInformation.append(NEWLINE).append(NEWLINE);

			for (int nodeId = 0; nodeId < N; nodeId++)
			{
				String attributes = StringUtils.mapToString(getNodeSpecificAttributes(nodeId), "=", ", ");

				String nodeInformation = String.format("n%d, position: (%.3g, %.3g), name: %s, attributes: %s", nodeId, getNodeXYPosition(nodeId)[0], getNodeXYPosition(nodeId)[1], getNodeName(nodeId), attributes.isEmpty() ? "-" : attributes);
				netPlanInformation.append(nodeInformation);
				netPlanInformation.append(NEWLINE);
			}
		}

		if (E > 0)
		{
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("Link information");
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("--------------------------------");
			netPlanInformation.append(NEWLINE).append(NEWLINE);

			for (int linkId = 0; linkId < E; linkId++)
			{
				int originNodeId = getLinkOriginNode(linkId);
				int destinationNodeId = getLinkDestinationNode(linkId);

				String attributes = StringUtils.mapToString(getLinkSpecificAttributes(linkId), "=", ", ");

				String linkInformation = String.format("e%d, n%d (%s) -> n%d (%s), capacity: %.3g E, length: %.3g km, attributes: %s", linkId, originNodeId, getNodeName(originNodeId), destinationNodeId, getNodeName(destinationNodeId), getLinkCapacityInErlangs(linkId), getLinkLengthInKm(linkId), attributes.isEmpty() ? "-" : attributes);
				netPlanInformation.append(linkInformation);
				netPlanInformation.append(NEWLINE);
			}
		}

		if (D > 0)
		{
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("Demand information");
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("--------------------------------");
			netPlanInformation.append(NEWLINE).append(NEWLINE);

			for (int demandId = 0; demandId < D; demandId++)
			{
				int ingressNodeId = getDemandIngressNode(demandId);
				int egressNodeId = getDemandEgressNode(demandId);

				String attributes = StringUtils.mapToString(getDemandSpecificAttributes(demandId), "=", ", ");

				String demandInformation = String.format("d%d, n%d (%s) -> n%d (%s), offered traffic: %.3g E, attributes: %s", demandId, ingressNodeId, getNodeName(ingressNodeId), egressNodeId, getNodeName(egressNodeId), getDemandOfferedTrafficInErlangs(demandId), attributes.isEmpty() ? "-" : attributes);
				netPlanInformation.append(demandInformation);
				netPlanInformation.append(NEWLINE);
			}
		}

		if (R > 0)
		{
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("Routing information");
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("--------------------------------");
			netPlanInformation.append(NEWLINE).append(NEWLINE);

			for (int routeId = 0; routeId < R; routeId++)
			{
				int[] sequenceOfLinks = getRouteSequenceOfLinks(routeId);
				StringBuilder path = new StringBuilder();

				for (int linkId : sequenceOfLinks)
				{
					if (path.length() == 0)
					{
						int originNodeId = getLinkOriginNode(linkId);
						path.append(String.format("n%d (%s)", originNodeId, getNodeName(originNodeId)));
					}

					int destinationNodeId = getLinkDestinationNode(linkId);

					path.append(String.format(" => e%d => n%d (%s)", linkId, destinationNodeId, getNodeName(destinationNodeId)));
				}

				String attributes = StringUtils.mapToString(getRouteSpecificAttributes(routeId), "=", ", ");

				String routeInformation = String.format("p%d, demand: d%d, carried traffic: %.3g E, path: %s, backup segments: %s, attributes: %s", routeId, getRouteDemand(routeId), getRouteCarriedTrafficInErlangs(routeId), path.toString(), IntUtils.join(getRouteBackupSegmentList(routeId), ", "), attributes.isEmpty() ? "-" : attributes);
				netPlanInformation.append(routeInformation);
				netPlanInformation.append(NEWLINE);
			}
		}

		if (S > 0)
		{
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("Protection segment information");
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("--------------------------------");
			netPlanInformation.append(NEWLINE).append(NEWLINE);

			for (int segmentId = 0; segmentId < S; segmentId++)
			{
				int[] sequenceOfLinks = getProtectionSegmentSequenceOfLinks(segmentId);
				StringBuilder path = new StringBuilder();

				for (int linkId : sequenceOfLinks)
				{
					if (path.length() == 0)
					{
						int originNodeId = getLinkOriginNode(linkId);
						path.append(String.format("n%d (%s)", originNodeId, getNodeName(originNodeId)));
					}

					int destinationNodeId = getLinkDestinationNode(linkId);

					path.append(String.format(" => e%d => n%d (%s)", linkId, destinationNodeId, getNodeName(destinationNodeId)));
				}

				String attributes = StringUtils.mapToString(getProtectionSegmentSpecificAttributes(segmentId), "=", ", ");

				String segmentInformation = String.format("s%d, reserved bandwidth: %.3g E, path: %s, attributes: %s", segmentId, getProtectionSegmentReservedBandwidthInErlangs(segmentId), path.toString(), attributes.isEmpty() ? "-" : attributes);
				netPlanInformation.append(segmentInformation);
				netPlanInformation.append(NEWLINE);
			}
		}

		if (numSRGs != 0)
		{
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("Shared-risk group information");
			netPlanInformation.append(NEWLINE);
			netPlanInformation.append("--------------------------------");
			netPlanInformation.append(NEWLINE).append(NEWLINE);

			for (int srgId = 0; srgId < numSRGs; srgId++)
			{
				int[] nodeIds = getSRGNodes(srgId);
				int[] linkIds = getSRGLinks(srgId);

				String aux_nodes = nodeIds.length == 0 ? "none" : IntUtils.join(nodeIds, " ");
				String aux_links = linkIds.length == 0 ? "none" : IntUtils.join(linkIds, " ");

				String attributes = StringUtils.mapToString(getSRGSpecificAttributes(srgId), "=", ", ");

				String srgInformation = String.format("srg%d, nodes: %s, links: %s, attributes: %s", srgId, aux_nodes, aux_links, attributes.isEmpty() ? "-" : attributes);
				netPlanInformation.append(srgInformation);
				netPlanInformation.append(NEWLINE);
			}
		}

		return netPlanInformation.toString();
	}
}