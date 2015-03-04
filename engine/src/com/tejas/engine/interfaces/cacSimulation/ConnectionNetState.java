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

package com.tejas.engine.interfaces.cacSimulation;

import cern.colt.list.tlong.LongArrayList;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.jet.math.tdouble.DoubleFunctions;
import static com.tejas.engine.interfaces.cacSimulation.CACAction.ActionType.ACCEPT_REQUEST;
import static com.tejas.engine.interfaces.cacSimulation.CACAction.ActionType.BLOCK_REQUEST;
import static com.tejas.engine.interfaces.cacSimulation.CACAction.ActionType.RELEASE_CONNECTION;
import static com.tejas.engine.interfaces.cacSimulation.CACEvent.EventType.CONNECTION_RELEASE;
import static com.tejas.engine.interfaces.cacSimulation.CACEvent.EventType.CONNECTION_REQUEST;

import com.tejas.engine.interfaces.cacSimulation.CACAction;
import com.tejas.engine.interfaces.cacSimulation.CACEvent;
import com.tejas.engine.interfaces.cacSimulation.ConnectionNetState;
import com.tejas.engine.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.interfaces.networkDesign.NetPlan;
import com.tejas.engine.internal.NetworkElement;
import com.tejas.engine.internal.sim.SimEvent;
import com.tejas.engine.internal.sim.SimState;
import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.LongUtils;
import com.tejas.engine.utils.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents the current state of a network within the connection-admission-control
 * simulation: active connections, current traffic per link...
 *
 * <p><b>Important</b>: Users only should use this class only for queries via
 * <code>getX()</code> methods, since kernel is in charge of modifying the network
 * state using the events ({@link com.tejas.engine.interfaces.cacSimulation.CACEvent CACEvent})
 * scheduled by the event generator (see {@link com.tejas.engine.interfaces.cacSimulation.IConnectionEventGenerator IConnectionEventGenerator})
 * and the actions (see {@link com.tejas.engine.interfaces.cacSimulation.CACAction CACAction})
 * provided by the event processor (see {@link com.tejas.engine.interfaces.cacSimulation.ICACAlgorithm ICACAlgorithm}).</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public final class ConnectionNetState extends SimState
{
    private int N, E, D;
    
    private Map<Long, Connection> connections;
    private Map<Long, ConnectionRoute> connectionRoutes;
    
    private long nextConnectionId;
    private long nextConnectionRouteId;
    private double[] u_e;
    private double[] r_e;
    private double[] spareCapacity_e;
    
    private ConnectionNetState() { super(null); }
    
    /**
     * Default constructor.
     *
     * @param netPlan Complete network design (including traffic demands)
     * @since 0.2.0
     */
    public ConnectionNetState(NetPlan netPlan)
    {
        super(netPlan);
        
        N = netPlan.getNumberOfNodes();
        E = netPlan.getNumberOfLinks();
        D = netPlan.getNumberOfDemands();
        
        u_e = netPlan.getLinkCapacityInErlangsVector();
        r_e = netPlan.getLinkCapacityReservedForProtectionInErlangsVector();
        spareCapacity_e = new double[E];
        for(int linkId = 0; linkId < E; linkId++)
        {
            if (r_e[linkId] > u_e[linkId]) throw new Net2PlanException("Bad - Capacity reserved for protection in link " + linkId + " is greater than the link capacity");
            
            spareCapacity_e[linkId] = u_e[linkId] - r_e[linkId];
        }

        isModifiable = true;
        
        reset();
    }
    
    /**
     * Returns a network design from the current network state.
     * 
     * @return Network design
     * @since 0.2.1
     */
    @Override
    public NetPlan convertToNetPlan()
    {
	NetPlan aux = new NetPlan();

	aux.setNetworkAttributes(netPlan.getNetworkAttributes());
        aux.setNetworkDescription(netPlan.getNetworkDescription());
        aux.setNetworkName(netPlan.getNetworkName());

	for (int nodeId = 0; nodeId < N; nodeId++)
	    aux.addNode(netPlan.getNodeXYPosition(nodeId)[0], netPlan.getNodeXYPosition(nodeId)[1], netPlan.getNodeName(nodeId), netPlan.getNodeSpecificAttributes(nodeId));

	for (int linkId = 0; linkId < E; linkId++)
	    aux.addLink(netPlan.getLinkOriginNode(linkId), netPlan.getLinkDestinationNode(linkId), u_e[linkId], netPlan.getLinkLengthInKm(linkId), netPlan.getLinkSpecificAttributes(linkId));
        
        long[] connIds = getConnectionIds();
        for(long connId : connIds)
        {
            int demandId = getConnectionDemand(connId);
            int ingressNodeId = netPlan.getDemandIngressNode(demandId);
            int egressNodeId = netPlan.getDemandEgressNode(demandId);
            double h_d = getConnectionRequestedTrafficInErlangs(connId);
            Map<String, String> attributes = getConnectionAttributes(connId);
            
            int newDemandId = aux.addDemand(ingressNodeId, egressNodeId, h_d, attributes);
            
            long[] connRouteIds = getConnectionRoutes(connId);
            for(long connRouteId : connRouteIds)
            {
                int[] seqLinks = getConnectionRouteSequenceOfLinks(connRouteId);
                double r_d = getConnectionRouteCarriedTrafficInErlangs(connRouteId);
                Map<String, String> routeAttributes = getConnectionRouteAttributes(connRouteId);
                
                aux.addRoute(newDemandId, r_d, seqLinks, null, routeAttributes);
            }
        }

	return aux;
    }
    
    @Override
    public void checkValidity(Map<String, String> net2planParameters, boolean allowLinkOversubscription, boolean allowExcessCarriedTraffic)
    {
	double PRECISIONFACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));

	double[] y_e = getLinkCurrentCarriedTrafficInErlangsVector();

        if (!allowLinkOversubscription)
        {
            for(int linkId = 0; linkId < E; linkId++)
            {
                if (y_e[linkId] > spareCapacity_e[linkId] * (1 + PRECISIONFACTOR))
                    throw new RuntimeException(String.format("Carried traffic (%f E) and reserved bandwidth for protection (%f E) for link %d overcomes link capacity (%f E)", y_e[linkId], r_e[linkId], linkId, u_e[linkId]));
            }
        }
        
        if (!allowExcessCarriedTraffic)
        {
            long[] connIds = getConnectionIds();

            for(long connId : connIds)
            {
                double h_c = getConnectionRequestedTrafficInErlangs(connId);
                double r_c = getConnectionCurrentCarriedTrafficInErlangs(connId);
                
		if (r_c > h_c * (1 + PRECISIONFACTOR))
                    throw new Net2PlanException(String.format("Carried traffic for connection %d overcomes the offered traffic (offered = %f E, carried = %f E)", connId, h_c, r_c));
            }
        }
    }
    
    /**
     * Returns a deep copy of the current state.
     * 
     * @return Deep copy of the current state
     * @since 0.2.0
     */
    @Override
    public ConnectionNetState copy()
    {
        ConnectionNetState copy = new ConnectionNetState(netPlan);
        
        copy.nextConnectionId = nextConnectionId;
        copy.nextConnectionRouteId = nextConnectionRouteId;
        copy.isModifiable = true;
        
        Iterator<Entry<Long, Connection>> connIt = connections.entrySet().iterator();
        while(connIt.hasNext())
        {
            Entry<Long, Connection> entry = connIt.next();
            copy.connections.put(entry.getKey(), entry.getValue().clone());
        }

        Iterator<Entry<Long, ConnectionRoute>> connRouteId = connectionRoutes.entrySet().iterator();
        while(connRouteId.hasNext())
        {
            Entry<Long, ConnectionRoute> entry = connRouteId.next();
            copy.connectionRoutes.put(entry.getKey(), entry.getValue().clone());
        }
        
        return copy;
    }
    
    /**
     * Returns an unmodifiable view of the network.
     *
     * @return An unmodifiable view of the network
     * @since 0.2.0
     */
    @Override
    public ConnectionNetState unmodifiableView()
    {
        ConnectionNetState view = new ConnectionNetState();
        
        view.netPlan = netPlan;
        view.N = N;
        view.E = E;
        view.D = D;
        view.connections = connections;
        view.connectionRoutes = connectionRoutes;
        view.nextConnectionId = nextConnectionId;
        view.nextConnectionRouteId = nextConnectionRouteId;
        view.u_e = u_e;
        view.r_e = r_e;
        view.spareCapacity_e = spareCapacity_e;
        view.isModifiable = false;
        
        return view;
    }
    
    private Connection getConnection(long connId)
    {
        Connection conn = connections.get(connId);
        if (conn == null) throw new Net2PlanException("Connection " + connId + " is not active now");
        
        return conn;
    }

    /**
     * Returns the arrival time of the specified connection.
     *
     * @param connId Connection identifier
     * @return Arrival time
     * @since 0.2.0
     */
    public double getConnectionArrivalTime(long connId) { return getConnection(connId).arrivalTime; }

    /**
     * Returns the value of a given attribute for a connection route. If not defined,
     * it is search for the network.
     *
     * @param connId Connection identifier
     * @param key Attribute name
     * @return Attribute value (or null, if not defined)
     * @since 0.2.3
     */
    public String getConnectionAttribute(long connId, String key)
    {
	Connection connection = getConnection(connId);
	String value = connection.getAttribute(key);
	return value == null ? netPlan.getNetworkAttribute(key) : value;
    }

    /**
     * Returns the set of attributes of a given connection.
     *
     * @param connId Connection identifier
     * @return Connection attributes
     * @since 0.2.3
     */
    public Map<String, String> getConnectionAttributes(long connId) { return Collections.unmodifiableMap(getConnection(connId).getAttributes()); }
    
    /**
     * Returns the current traffic volume for a given connection.
     *
     * @param connId Connection identifier
     * @return Current traffic volume
     * @since 0.2.0
     */
    public double getConnectionCurrentCarriedTrafficInErlangs(long connId)
    {
        Connection conn = getConnection(connId);
        
        double carriedTraffic = 0;
        
        for(long connRouteId : conn.connRouteIds)
        {
            ConnectionRoute connRoute = getConnectionRoute(connRouteId);
            carriedTraffic += connRoute.currentTrafficVolume;
        }
        
        return carriedTraffic;
    }
    
    /**
     * Returns the demand identifier for a given connection.
     *
     * @param connId Connection identifier
     * @return Demand identifier
     * @since 0.2.0
     */
    public int getConnectionDemand(long connId) { return getConnection(connId).demandId; }

    /**
     * Returns the duration of the specified connection.
     *
     * @param connId Connection identifier
     * @return Duration
     * @since 0.2.0
     */
    public double getConnectionDuration(long connId) { return getConnection(connId).duration; }
    
    /**
     * Returns the set of active connection identifiers (in ascending order).
     *
     * @return Active connection identifiers
     * @since 0.2.0
     */
    public long[] getConnectionIds()
    {
        Set<Long> aux_connIds = connections.keySet();
        if (aux_connIds.isEmpty()) return new long[0];
        
        LongArrayList connIds = new LongArrayList();
        connIds.addAllOf(new TreeSet<Long>(connections.keySet()));
        
        connIds.trimToSize();
        return connIds.elements();
    }

    /**
     * Returns the connection identifier for the next created connection.
     *
     * @return Connection identifier for the next created connection
     * @since 0.2.3
     */
    public long getConnectionNextId() { return nextConnectionId; }
    
    private ConnectionRoute getConnectionRoute(long connRouteId)
    {
        ConnectionRoute connRoute = connectionRoutes.get(connRouteId);
        if (connRoute == null) throw new Net2PlanException("Connection route " + connRouteId + " is not active now");
        
        return connRoute;
    }

    /**
     * Returns the value of a given attribute for a connection route. If not defined,
     * it is search for the network.
     *
     * @param connRouteId Connection route identifier
     * @param key Attribute name
     * @return Attribute value (or null, if not defined)
     * @since 0.2.3
     */
    public String getConnectionRouteAttribute(long connRouteId, String key)
    {
	ConnectionRoute connectionRoute = getConnectionRoute(connRouteId);
	String value = connectionRoute.getAttribute(key);
	return value == null ? netPlan.getNetworkAttribute(key) : value;
    }

    /**
     * Returns the set of attributes of a given connection route.
     *
     * @param connRouteId Connection route identifier
     * @return Connection route attributes
     * @since 0.2.3
     */
    public Map<String, String> getConnectionRouteAttributes(long connRouteId) { return Collections.unmodifiableMap(getConnectionRoute(connRouteId).getAttributes()); }
    
    /**
     * Returns the current traffic volume for a given connection route.
     *
     * @param connRouteId Connection route identifier
     * @return Current traffic volume
     * @since 0.2.3
     */
    public double getConnectionRouteCarriedTrafficInErlangs(long connRouteId) { return getConnectionRoute(connRouteId).currentTrafficVolume; }
    
    /**
     * Returns the set of active connection route identifiers (in ascending order).
     *
     * @return Active connection route identifiers
     * @since 0.2.0
     */
    public long[] getConnectionRouteIds()
    {
        if (connectionRoutes.isEmpty()) return new long[0];
        
        LongArrayList connRouteIds = new LongArrayList();
        connRouteIds.addAllOf(new TreeSet<Long>(connectionRoutes.keySet()));
        
        connRouteIds.trimToSize();
        return connRouteIds.elements();
    }

    /**
     * Returns the requested traffic volume for a given connection.
     *
     * @param connId Connection identifier
     * @return Requested traffic volume
     * @since 0.2.0
     */
    public double getConnectionRequestedTrafficInErlangs(long connId) { return getConnection(connId).requestedTrafficVolume; }
    
    /**
     * Returns the connection identifier for the given connection route.
     *
     * @param connRouteId Connection route identifier
     * @return Connection identifier
     * @since 0.2.3
     */
    public long getConnectionRouteConnection(long connRouteId) { return getConnectionRoute(connRouteId).connId; }
    
    /**
     * Returns the connection identifier for the next created connection route.
     *
     * @return Connection identifier for the next created connection route
     * @since 0.2.3
     */
    public long getConnectionRouteNextId() { return nextConnectionRouteId; }
    
    /**
     * Returns the connection route identifiers associated to the connection.
     *
     * @param connId Connection identifier
     * @return Connection route identifiers
     * @since 0.2.3
     */
    public long[] getConnectionRoutes(long connId) { return LongUtils.toArray(connections.get(connId).connRouteIds); }
    
    /**
     * Returns the sequence of links traversed by a given connection route.
     *
     * @param connRouteId Connection route identifier
     * @return List of links and traversed by the connection route
     * @since 0.2.3
     */
    public int[] getConnectionRouteSequenceOfLinks(long connRouteId)
    {
        ConnectionRoute connRoute = getConnectionRoute(connRouteId);
        return IntUtils.copy(connRoute.seqLinks);
    }

    /**
     * Returns the set of connection ids associated to a given demand.
     *
     * @param demandId Demand identifier
     * @return Set of connection ids associated to that demand
     * @since 0.2.0
     */
    public long[] getDemandCurrentConnections(int demandId)
    {
        LongArrayList connectionList = new LongArrayList();

	long[] connIds = getConnectionIds();

	for(long connId : connIds)
	    if (getConnectionDemand(connId) == demandId)
                connectionList.add(connId);

	connectionList.trimToSize();

        return connectionList.elements();
    }
    
    /**
     * Returns a vector with the carried traffic per demand.
     *
     * @return Vector with the carried traffic per demand
     * @since 0.2.0
     */
    public double[] getDemandCurrentCarriedTrafficInErlangsVector()
    {
        DoubleMatrix1D carriedTraffic = DoubleFactory1D.dense.make(D);
        
        for(Connection conn : connections.values())
        {
            int demandId = conn.demandId;
            
            Set<Long> connRouteIds = conn.connRouteIds;
            for(long connRouteId: connRouteIds)
            {
                ConnectionRoute connRoute = getConnectionRoute(connRouteId);
                carriedTraffic.setQuick(demandId, carriedTraffic.getQuick(demandId) + connRoute.currentTrafficVolume);
            }
        }

        return carriedTraffic.toArray();
    }

    /**
     * Returns a vector with the offered traffic per demand.
     *
     * @return Vector with the offered traffic per demand
     * @since 0.2.0
     */
    public double[] getDemandCurrentOfferedTrafficInErlangsVector()
    {
        DoubleMatrix1D offeredTraffic = DoubleFactory1D.dense.make(D);
        
        for(Connection conn : connections.values())
        {
            int demandId = conn.demandId;
            offeredTraffic.setQuick(demandId, offeredTraffic.getQuick(demandId) + conn.requestedTrafficVolume);
        }

        return offeredTraffic.toArray();
    }

    /**
     * Returns the number of current active connections per each demand.
     *
     * @return Number of current active connections per each demand
     * @since 0.2.2
     */
    public int[] getDemandCurrentNumberOfConnectionsVector()
    {
        int[] numTraversingConnections = new int[D];
        
        for(int demandId = 0; demandId < D; demandId++)
            numTraversingConnections[demandId] = getDemandCurrentConnections(demandId).length;
        
        return numTraversingConnections;
    }
    
    /**
     * Returns a vector with the carried traffic per link.
     *
     * @return Vector with the carried traffic per link
     * @since 0.2.0
     */
    public double[] getLinkCurrentCarriedTrafficInErlangsVector()
    {
        DoubleMatrix1D carriedTraffic = DoubleFactory1D.dense.make(E);
        
        for(ConnectionRoute connRoute : connectionRoutes.values())
            carriedTraffic.viewSelection(connRoute.seqLinks).assign(DoubleFunctions.plus(connRoute.currentTrafficVolume));

        return carriedTraffic.toArray();
    }
    
    /**
     * Returns the number of current traversing connection routes per each link.
     *
     * @return Number of current traversing connection routes per each link
     * @since 0.2.3
     */
    public int[] getLinkCurrentNumberOfTraversingConnectionRoutesVector()
    {
        int[] numTraversingConnections = new int[E];
        
        for(long connRouteId : connectionRoutes.keySet())
        {
            ConnectionRoute connRoute = getConnectionRoute(connRouteId);
            
            for(int linkId : connRoute.seqLinks)
                numTraversingConnections[linkId]++;
        }
        
        return numTraversingConnections;
    }

    /**
     * Returns a vector with the spare capacity per link.
     *
     * @return Vector with the spare capacity per link
     * @since 0.2.2
     */
    public double[] getLinkCurrentSpareCapacityInErlangsVector()
    {
        double[] y_e = getLinkCurrentCarriedTrafficInErlangsVector();
        double[] currentSpareCapacity_e = new double[E];
        for(int linkId = 0; linkId < E; linkId++)
            currentSpareCapacity_e[linkId] = Math.max(0, spareCapacity_e[linkId] - y_e[linkId]);
        
        return currentSpareCapacity_e;
    }
    
    /**
     * Returns the list of connection routes traversing the given link.
     *
     * @param linkId Link identifier
     * @return List of connection routes traversing the given link
     * @since 0.2.3
     */
    public long[] getLinkCurrentTraversingConnectionRoutes(int linkId)
    {
        LongArrayList connectionRouteList = new LongArrayList();
        
        for(long connRouteId : connectionRoutes.keySet())
        {
            ConnectionRoute connRoute = getConnectionRoute(connRouteId);
            
	    if (IntUtils.contains(connRoute.seqLinks, linkId))
		connectionRouteList.add(connRouteId);
        }

	connectionRouteList.trimToSize();

        return connectionRouteList.elements();
    }
    
    /**
     * Returns the current egress traffic from each node.
     *
     * @return Current egress traffic from each node
     * @since 0.2.0
     */
    public double[] getNodeCurrentEgressTrafficInErlangsVector()
    {
	double[] egressTraffic = new double[N];
        
        for(ConnectionRoute connRoute : connectionRoutes.values())
        {
            int lastLinkId = connRoute.seqLinks[connRoute.seqLinks.length - 1];
            int egressNodeId = netPlan.getLinkDestinationNode(lastLinkId);
            egressTraffic[egressNodeId] += connRoute.currentTrafficVolume;
        }
        
	return egressTraffic;
    }

    /**
     * Returns the current ingress traffic to each node.
     *
     * @return Current ingress traffic to each node
     * @since 0.2.0
     */
    public double[] getNodeCurrentIngressTrafficInErlangsVector()
    {
	double[] ingressTraffic = new double[N];

        for(ConnectionRoute connRoute : connectionRoutes.values())
        {
            int firstLinkId = connRoute.seqLinks[0];
            int ingressNodeId = netPlan.getLinkOriginNode(firstLinkId);
            ingressTraffic[ingressNodeId] += connRoute.currentTrafficVolume;
        }

	return ingressTraffic;
    }

    /**
     * Returns the current traffic traversing each node.
     *
     * @return Current traffic traversing each node
     * @since 0.2.0
     */
    public double[] getNodeCurrentTraversingTrafficInErlangsVector()
    {
	double[] traversingTraffic = new double[N];

        for(ConnectionRoute connRoute : connectionRoutes.values())
        {
            int[] seqLinks = connRoute.seqLinks;
	    int[] seqNodes = netPlan.convertSequenceOfLinks2SequenceOfNodes(seqLinks);
	    for (int seqId = 1; seqId < seqNodes.length - 1; seqId++)
		traversingTraffic[seqNodes[seqId]] += connRoute.currentTrafficVolume;
	}

	return traversingTraffic;
    }
    
    /**
     * Returns the number of active connections in the network.
     *
     * @return Number of active connections
     * @since 0.2.0
     */
    public int getNumberOfConnections() { return connections.size(); }
    
    /**
     * Returns the number of active connection routes in the network.
     *
     * @return Number of active connection routes
     * @since 0.2.3
     */
    public int getNumberOfConnectionRoutes() { return connectionRoutes.size(); }

    /**
     * Returns <code>true</code> if the network has at least one active connection. It is equivalent to <code>{@link #getNumberOfConnections getNumberOfConnections()} > 0</code>.
     *
     * @return <code>true</code> if there are active connections in the network, and <code>false</code> otherwise
     * @since 0.2.0
     */
    public boolean hasConnections() { return getNumberOfConnections() > 0; }

    /**
     * Returns <code>true</code> if the network has at least one active connection. It is equivalent to <code>{@link #getNumberOfConnectionRoutes getNumberOfConnectionRoutes()} > 0</code>.
     *
     * @return <code>true</code> if there are active connections in the network, and <code>false</code> otherwise
     * @since 0.2.3
     */
    public boolean hasConnectionRoutes() { return getNumberOfConnectionRoutes() > 0; }

    /**
     * Returns <code>true</code> if the given connection is active.
     *
     * @param connId Connection identifier
     * @return <code>true</code> if the given connection is active, <code>false</code> otherwise
     * @since 0.2.0
     */
    public boolean isActiveConnection(long connId) { return connections.containsKey(connId); }
    
    /**
     * Returns <code>true</code> if the given connection route is active.
     *
     * @param connRoute Connection route identifier
     * @return <code>true</code> if the given connection route is active, <code>false</code> otherwise
     * @since 0.2.3
     */
    public boolean isActiveConnectionRoute(long connRoute) { return connectionRoutes.containsKey(connRoute); }

    /**
     * Removes the connection given by the identifier.
     *
     * @param connId Connection identifier
     * @since 0.2.0
     */
    private void removeConnection(long connId)
    {
        checkIsModifiable();
        
        Connection conn = getConnection(connId);
        
        for(long connRouteId : conn.connRouteIds)
            removeConnectionRoute(connRouteId);
        
        connections.remove(connId);
    }
    
    /**
     * Removes the connection route given by the identifier.
     *
     * @param connRouteId Connection route identifier
     * @since 0.2.3
     */
    private void removeConnectionRoute(long connRouteId)
    {
        checkIsModifiable();
        
        ConnectionRoute connRoute = getConnectionRoute(connRouteId);
        connectionRoutes.remove(connRouteId);
        
        long connId = connRoute.connId;
        Connection conn = getConnection(connId);
        conn.connRouteIds.remove(connRouteId);
    }

    /**
     * Update the network state.
     *
     * @param event Current simulation event
     * @param actions List of actions to perform
     * @return Identifier of the new connection, in case event is a connection request, and this connection is accepted; otherwise, -1
     * @since 0.2.0
     */
    @Override
    public Object update(SimEvent event, List actions)
    {
        checkIsModifiable();
        
        if (!(event instanceof CACEvent)) throw new RuntimeException("Bad");

        CACEvent cacEvent = (CACEvent) event;
        double simTime = cacEvent.getEventTime();
	long newConnId = -1;

	// Check event and actions
	boolean isEventRequest = cacEvent.getEventType() == CACEvent.EventType.CONNECTION_REQUEST ? true : false;
	boolean isRequestDecisionMade = false;
	Set<Long> modifiedConnections = new HashSet<Long>();
	Set<Long> releasedConnections = new HashSet<Long>();
        
	for(Object action : actions)
	{
            if (!(action instanceof CACAction)) throw new RuntimeException("Bad");
            
            CACAction cacAction = (CACAction) action;
	    switch(cacAction.getActionType())
	    {
		case ACCEPT_REQUEST:
		    if (!isEventRequest) throw new RuntimeException("Wrong action - Connection cannot be accepted since the event is not a connection request");
		    if (isRequestDecisionMade) throw new RuntimeException("Wrong action - Previously a decision was made for the request");
		    isRequestDecisionMade = true;
		    break;

		case BLOCK_REQUEST:
		    if (!isEventRequest) throw new RuntimeException("Wrong action - Connection cannot be blocked since the event is not a connection request");
		    if (isRequestDecisionMade) throw new RuntimeException("Wrong action - Previously a decision was made for the request");
		    isRequestDecisionMade = true;
		    break;

		case MODIFY_CONNECTION_ROUTE:
                    long connRouteIdToModify = cacAction.modifyConnectionRoute_connRouteId;
		    if (modifiedConnections.contains(connRouteIdToModify)) throw new RuntimeException("Wrong action - Connection " + connRouteIdToModify + " was already modified in this state");
		    if (releasedConnections.contains(connRouteIdToModify)) throw new RuntimeException("Wrong action - Connection " + connRouteIdToModify + " was already released in this state");
		    modifiedConnections.add(connRouteIdToModify);
		    break;

		case RELEASE_CONNECTION:
		    long connIdToRelease = cacAction.releaseConnection_connId;
		    if (modifiedConnections.contains(connIdToRelease)) throw new RuntimeException("Wrong action - Connection " + connIdToRelease + " was already modified in this state");
		    if (releasedConnections.contains(connIdToRelease)) throw new RuntimeException("Wrong action - Connection " + connIdToRelease + " was already released in this state");
		    releasedConnections.add(connIdToRelease);
		    break;

		default:
		    throw new RuntimeException("Bad - Unknown CAC action type");
	    }
	}
        
	switch(cacEvent.getEventType())
	{
	    case CONNECTION_REQUEST:
                if (!isRequestDecisionMade) throw new Net2PlanException("No decision made upon connection request");
		break;

	    case CONNECTION_RELEASE:
                if (isRequestDecisionMade) throw new Net2PlanException("An accept/block decision has been made upon connection release");
		long connId = cacEvent.getReleaseConnectionId();
		removeConnection(connId);
		break;

	    default:
		throw new RuntimeException("Bad - Unknown CAC event type");
	}

	for(Object action : actions)
	{
            if (!(action instanceof CACAction)) throw new RuntimeException("Bad");
            
            CACAction cacAction = (CACAction) action;
	    switch(cacAction.getActionType())
	    {
		case ACCEPT_REQUEST:
		    int demandId = cacEvent.getRequestDemandId();
		    double requestedTrafficVolumeInErlangs = cacEvent.getRequestTrafficVolumeInErlangs();
		    double durationInSeconds = cacEvent.getRequestDurationInSeconds();
                    Map<String, String> attributes = cacEvent.getRequestAttributes();
                    
                    List<int[]> seqLinks = cacAction.acceptConnection_seqLinks;
                    double[] currentTrafficVolumeInErlangs = cacAction.acceptConnection_trafficVolume;
                    List<Map<String, String>> connRouteAttributes = cacAction.acceptConnection_attributes;
                    
                    newConnId = addConnection(demandId, simTime, durationInSeconds, requestedTrafficVolumeInErlangs, seqLinks, currentTrafficVolumeInErlangs, attributes, connRouteAttributes);
		    break;
                    
                case ADD_CONNECTION_ROUTE:
                    long addConnectionRoute_connId = cacAction.addConnectionRoute_connId;
                    int[] addConnectionRoute_seqLinks = cacAction.addConnectionRoute_seqLinks;
                    double addConnectionRoute_trafficVolume = cacAction.addConnectionRoute_trafficVolume;
                    Map<String, String> addConnectionRoute_attributes = cacAction.addConnectionRoute_attributes;
                    addConnectionRoute(addConnectionRoute_connId, addConnectionRoute_seqLinks, addConnectionRoute_trafficVolume, addConnectionRoute_attributes);

		case BLOCK_REQUEST:
		    break;

		case MODIFY_CONNECTION_ROUTE:
		    long connRouteIdToModify = cacAction.modifyConnectionRoute_connRouteId;
                    ConnectionRoute modifyConnectionRoute_route = getConnectionRoute(connRouteIdToModify);
		    double trafficVolumeToModify = cacAction.modifyConnectionRoute_trafficVolume;
                    if (trafficVolumeToModify != -1)
                    {
                        if (trafficVolumeToModify < 0) throw new Net2PlanException("Traffic volume for connection route " + connRouteIdToModify + " must be greater or equal than zero, or -1 for 'no change'");
                        modifyConnectionRoute_route.currentTrafficVolume = trafficVolumeToModify;
                    }
                    
                    Map<String, String> modifyConnectionRoute_attributes = cacAction.modifyConnectionRoute_attributes;
                    if (modifyConnectionRoute_attributes != null) modifyConnectionRoute_route.setAttributes(modifyConnectionRoute_attributes);

		    break;

		case RELEASE_CONNECTION:
		    long connIdToRelease = cacAction.releaseConnection_connId;

		    removeConnection(connIdToRelease);
		    break;

		default:
		    throw new RuntimeException("Bad - Unknown CAC action type");
	    }
	}

	return newConnId;
    }

    /**
     * Resets the state of the network.
     * 
     * @since 0.2.0
     */
    @Override
    public void reset()
    {
        checkIsModifiable();
        
        connections = new HashMap<Long, Connection>();
        connectionRoutes = new HashMap<Long, ConnectionRoute>();

        nextConnectionId = 0;
        nextConnectionRouteId = 0;
    }
    
    private class Connection extends NetworkElement implements Cloneable
    {
        int demandId;
        double arrivalTime;
        double duration;
        double requestedTrafficVolume;
        Set<Long> connRouteIds;
        
        public Connection(int demandId, double arrivalTime, double duration, double requestedTrafficVolume, Map<String, String> attributes)
        {
            super(attributes);
            
            this.demandId = demandId;
            this.arrivalTime = arrivalTime;
            this.duration = duration;
            this.requestedTrafficVolume = requestedTrafficVolume;
            
            connRouteIds = new HashSet<Long>();
        }

        @Override
        public Connection clone()
        {
            Connection aux = new Connection(demandId, arrivalTime, duration, requestedTrafficVolume, getAttributes());
            aux.connRouteIds.addAll(connRouteIds);
            
            return aux;
        }

	@Override
	public String toString()
	{
            double currentCarriedTraffic = 0;
            
            StringBuilder out = new StringBuilder();
            for(long connRouteId : connRouteIds)
            {
                ConnectionRoute aux = connectionRoutes.get(connRouteId);
                currentCarriedTraffic += aux.currentTrafficVolume;
                out.append(String.format(" - connection route %d: %s%n", connRouteId, aux.toString()));
            }
            
            Map<String, String> attributes = getAttributes();
            String connInfo = String.format("demand id = %d, arrival time = %s, duration = %s, req. volume = %f E, cur. volume = %f E, attributes = %s%n", demandId, SimEvent.secondsToYearsDaysHoursMinutesSeconds(arrivalTime), SimEvent.secondsToYearsDaysHoursMinutesSeconds(duration), requestedTrafficVolume, currentCarriedTraffic, attributes.isEmpty() ? "none" : StringUtils.mapToString(attributes, "=", ", "));
            
            return connInfo + out.toString();
	}
    }

    private class ConnectionRoute extends NetworkElement implements Cloneable
    {
        long connId;
        double currentTrafficVolume;
        int[] seqLinks;
        
        public ConnectionRoute(long connId, double currentTrafficVolume, int[] seqLinks, Map<String, String> attributes)
        {
            super(attributes);
            
            this.connId = connId;
            this.currentTrafficVolume = currentTrafficVolume;
            this.seqLinks = IntUtils.copy(seqLinks);
        }

        @Override
        public ConnectionRoute clone()
        {
            return new ConnectionRoute(connId, currentTrafficVolume, seqLinks, getAttributes());
        }

	@Override
	public String toString()
	{
            Map<String, String> attributes = getAttributes();
	    return "connection id = " + connId + ", seqLinks = " + Arrays.toString(seqLinks) + ", cur. volume = " + currentTrafficVolume + ", attributes = " + (attributes.isEmpty() ? "none" : StringUtils.mapToString(attributes, "=", ", "));
	}
    }
    
    private long addConnection(int demandId, double arrivalTime, double duration, double requestedTrafficVolume, List<int[]> sequenceOfLinksList, double[] currentTrafficVolumeVector, Map<String, String> attributes, List<Map<String, String>> routeAttributesList)
    {
        checkIsModifiable();
        
        if (demandId < 0 || demandId >= D) throw new Net2PlanException("New connection accepted: Demand identifier not valid (" + demandId + ")");
        if (arrivalTime < 0) throw new Net2PlanException("New connection accepted: Arrival time must be greater or equal than zero (" + arrivalTime + ")");
        if (duration <= 0) throw new Net2PlanException("New connection accepted: Duration must be greater than zero (" + duration + ")");
        if (requestedTrafficVolume <= 0) throw new Net2PlanException("New connection accepted: Requested traffic volume must be greater than zero (" + requestedTrafficVolume + ")");
        
        int numSeqLinks = sequenceOfLinksList.size();
        int numTrafficVolumes = currentTrafficVolumeVector.length;
        int numAttributes = routeAttributesList == null ? 0 : routeAttributesList.size();
        
        if (numSeqLinks == 0) throw new Net2PlanException("New connection accepted: No routes specified for new connection");
        if (numSeqLinks != numTrafficVolumes) throw new Net2PlanException("New connection accepted: Number of routes specified (" + numSeqLinks + ") doesn't match the number of traffic volume values (" + numTrafficVolumes + ")");
        if (numAttributes > 0 && numSeqLinks != numAttributes) throw new Net2PlanException("New connection accepted: Number of routes specified (" + numSeqLinks + ") doesn't match the number of route-specific attribute maps (" + numAttributes + ")");
        
        long connId = nextConnectionId++;

        if (attributes == null) attributes = new HashMap<String, String>();
        Connection conn = new Connection(demandId, arrivalTime, duration, requestedTrafficVolume, attributes);

        connections.put(connId, conn);
        
        ListIterator<int[]> itRoutes = sequenceOfLinksList.listIterator();
        Iterator<Map<String, String>> itAttributes = routeAttributesList.iterator();
        while(itRoutes.hasNext())
        {
            int connRouteId = itRoutes.nextIndex();
            
            double thisRouteTrafficValue = currentTrafficVolumeVector[connRouteId];
            int[] thisRouteSeqLinks = itRoutes.next();
            Map<String, String> thisRouteAttributes = itAttributes.next();
            
            addConnectionRoute(connId, thisRouteSeqLinks, thisRouteTrafficValue, thisRouteAttributes);
        }
        
        return connId;
    }
    
    private long addConnectionRoute(long connId, int[] seqLinks, double trafficVolume, Map<String, String> attributes)
    {
        Connection conn = getConnection(connId);
        
        int demandId = conn.demandId;
        netPlan.checkRouteValidityForDemand(seqLinks, demandId);
        
        if (trafficVolume < 0) throw new Net2PlanException("Traffic volume for new connection routes must be greater or equal than zero");
        
        ConnectionRoute connRoute = new ConnectionRoute(connId, trafficVolume, seqLinks, attributes);
        long connRouteId = nextConnectionRouteId++;
        
        conn.connRouteIds.add(connId);
        connectionRoutes.put(connRouteId, connRoute);
        
        return connRouteId;
    }
}