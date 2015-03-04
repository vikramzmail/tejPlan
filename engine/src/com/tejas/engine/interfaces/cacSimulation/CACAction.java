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

import com.tejas.engine.utils.IntUtils;
import com.tejas.engine.utils.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * <p>Provides a set of actions to be returned by CAC algorithms. These actions are:</p>
 *
 * <ul>
 * <li>Accept connection request: The connection request is accepted</li>
 * <li>Block connection request: The connection request is not accepted for some
 * reason (i.e. no capacity available). The algorithm has the opportunity to use
 * a <code>String</code> to specify that reason</li>
 * <li>Modify connection: Modify an existing connection (sequence of links and/or
 * traffic volume</li>
 * <li>Release connection: Release an existing connection</li>
 * </ul>
 *
 * <p>Although the <code>CACAction</code> class is common for all actions, its meaning (i.e.
 * action type) depends on the constructor used to get an instance. Take a look
 * on the description of the constructors to obtain more information.</p>
 *
 * <p><b>Important</b>: 'Accept' and 'block' actions only can be returned when
 * the processed event is a connection request. Upon connection release, these
 * events are forbidden. This fact is checked by the kernel after the execution
 * of the algorithmm.</p>
 *
 * <p><b>Important</b>: Actions don't take effect within the algorithm. This means,
 * for example, that if you release a connection in order to accommodate another one,
 * methods like <code>netState.getLinkAvailableCapacityInErlangs()</code> will return
 * the previous values to the execution of the algorithm, instead of the current according
 * to the actions. Network state is actually modified by the kernel after
 * the execution of the algorithm, so users should deal with current network state
 * by their own.</p>
 *
 * <p><b>Important</b>: When connections are forced to be released using the
 * action, the previously scheduled 'release connection' event is then removed.</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 * @see com.tejas.engine.interfaces.cacSimulation.ICACAlgorithm
 * @see com.tejas.engine.interfaces.cacSimulation.CACEvent
 */
public class CACAction
{
    /**
     * Type of action.
     *
     * @since 0.2.0
     */
    public enum ActionType
    {
	/**
	 * Accept a connection request.
	 *
	 * @since 0.2.0
	 */
	ACCEPT_REQUEST,

	/**
	 * Block a connection request.
	 *
	 * @since 0.2.0
	 */
	BLOCK_REQUEST,

	/**
	 * Add a connection route to an existing connection.
	 *
	 * @since 0.2.3
	 */
	ADD_CONNECTION_ROUTE,

        /**
	 * Modify an existing connection route.
	 *
	 * @since 0.2.3
	 */
	MODIFY_CONNECTION_ROUTE,

	/**
	 * Remove an existing connection route.
	 *
	 * @since 0.2.3
	 */
	REMOVE_CONNECTION_ROUTE,
        
	/**
	 * <p>Release an existing connection.</p>
	 *
	 * <p>Important: This action doesn't trigger a 'connection release' event</p>
	 *
	 * @since 0.2.0
	 */
	RELEASE_CONNECTION
    };

    private ActionType type;

    protected double[] acceptConnection_trafficVolume;
    protected List<int[]> acceptConnection_seqLinks;
    protected List<Map<String, String>> acceptConnection_attributes;
    protected String blockConnection_reason;
    protected long addConnectionRoute_connId;
    protected int[] addConnectionRoute_seqLinks;
    protected double addConnectionRoute_trafficVolume;
    protected Map<String, String> addConnectionRoute_attributes;
    protected long modifyConnectionRoute_connRouteId;
    protected double modifyConnectionRoute_trafficVolume;
    protected Map<String, String> modifyConnectionRoute_attributes;
    protected long releaseConnection_connId;
    protected long removeConnectionRoute_connRouteId;
    
    /**
     * Default constructor. Users must generate new actions via factory methods.
     *
     * @since 0.2.3
     */
    private CACAction(ActionType type) { reset(); this.type = type; }

    /**
     * Blocks connection request.
     *
     * @param reasonToBlockConnection String defining the reason to block the request (can be <code>null</code>)
     * @return CAC action
     * @since 0.2.3
     */
    public static CACAction blockRequest(String reasonToBlockConnection)
    {
	CACAction action = new CACAction(ActionType.BLOCK_REQUEST);
	action.blockConnection_reason = reasonToBlockConnection == null ? "No reason specified" : reasonToBlockConnection;
        
        return action;
    }

    /**
     * Accepts a connection request. 
     *
     * @param seqLinks Sequence of links
     * @param trafficVolumeInErlangs Traffic volume accepted (can be lower to the one requested)
     * @param attributes Map for user-defined attributes. The key (String value) in the map will be the attribute name and the value (String value) will be whatever value is stored in that attribute. If <code>null</code>, it will be assumed to be an empty HashMap
     * @return CAC action
     * @since 0.2.3
     */
    public static CACAction acceptRequest(final int[] seqLinks, double trafficVolumeInErlangs, final Map<String, String> attributes)
    {
        List<int[]> seqLinks_complete = new LinkedList<int[]>(); seqLinks_complete.add(seqLinks);
        double[] trafficVolumeInErlangs_complete = new double[1]; trafficVolumeInErlangs_complete[0] = trafficVolumeInErlangs;
        List<Map<String, String>> attributes_complete = new LinkedList<Map<String, String>>(); attributes_complete.add(attributes);
        
        CACAction action = acceptRequest(seqLinks_complete, trafficVolumeInErlangs_complete, attributes_complete);
        return action;
    }
    
    /**
     * Accepts a connection request. 
     *
     * @param seqLinks List of sequence of links
     * @param trafficVolumeInErlangs Traffic volume accepted vector
     * @param attributes List of maps for user-defined attributes. The key (String value) in the map will be the attribute name and the value (String value) will be whatever value is stored in that attribute. If <code>null</code>, it will be assumed to be an empty HashMap
     * @return CAC action
     * @since 0.2.3
     */
    public static CACAction acceptRequest(List<int[]> seqLinks, double[] trafficVolumeInErlangs, List<Map<String, String>> attributes)
    {
        CACAction action = new CACAction(ActionType.ACCEPT_REQUEST);

        action.acceptConnection_seqLinks = seqLinks;
	action.acceptConnection_trafficVolume = trafficVolumeInErlangs;
        
        int numRoutes = seqLinks.size();
        
        if (attributes == null)
        {
            for(int connRouteId = 0; connRouteId < numRoutes; connRouteId++)
                action.acceptConnection_attributes.add(new HashMap<String, String>());
        }
        else
        {
            Iterator<Map<String, String>> connRouteIt = attributes.iterator();
            while(connRouteIt.hasNext())
            {
                Map<String, String> aux_attributes = connRouteIt.next();
                if (aux_attributes == null) aux_attributes = new HashMap<String, String>();
                action.acceptConnection_attributes.add(new HashMap<String, String>(aux_attributes));
            }
        }
        
        return action;
    }

    /**
     * Constructor to define a 'add connection route' action.
     *
     * @param connId Connection identifier
     * @param seqLinks Sequence of links
     * @param trafficVolumeInErlangs New traffic volume in Erlangs
     * @param attributes Connection route attributes (null means empty)
     * @return CAC action
     * @since 0.2.3
     */
    public static CACAction addConnectionRoute(int connId, int[] seqLinks, double trafficVolumeInErlangs, Map<String, String> attributes)
    {
        CACAction action = new CACAction(ActionType.ADD_CONNECTION_ROUTE);
        action.addConnectionRoute_connId = connId;
        action.addConnectionRoute_seqLinks = IntUtils.copy(seqLinks);
        action.addConnectionRoute_trafficVolume = trafficVolumeInErlangs;
        if (attributes != null) action.addConnectionRoute_attributes.putAll(attributes);
        
        return action;
    }

    /**
     * Constructor to define a 'modify connection route' action.
     *
     * @param connRouteId Connection route identifier
     * @param trafficVolumeInErlangs New traffic volume in Erlangs (-1 means 'no change')
     * @param attributes Connection route attributes (null means 'no change')
     * @return CAC action
     * @since 0.2.3
     */
    public static CACAction modifyConnectionRoute(int connRouteId, double trafficVolumeInErlangs, Map<String, String> attributes)
    {
        CACAction action = new CACAction(ActionType.MODIFY_CONNECTION_ROUTE);
        
	action.modifyConnectionRoute_connRouteId = connRouteId;
	if (trafficVolumeInErlangs != -1) action.modifyConnectionRoute_trafficVolume = trafficVolumeInErlangs;
        if (attributes != null) action.modifyConnectionRoute_attributes = new HashMap<String, String>(attributes);
        
        return action;
    }

    /**
     * <p>Releases an existing connection.</p>
     *
     * <p>Important: A 'connection release' event will be triggered inmediately.</p>
     *
     * @param connId Connection identifier (if it doesn't exist, nothing it will happen)
     * @return CAC action
     * @since 0.2.3
     */
    public static CACAction releaseConnection(int connId)
    {
	CACAction action = new CACAction(ActionType.RELEASE_CONNECTION);
	action.releaseConnection_connId = connId;
        
        return action;
    }

    /**
     * Returns the action type.
     *
     * @return Action type
     * @since 0.2.0
     */
    public ActionType getActionType() { return type; }

    private void reset()
    {
	acceptConnection_seqLinks = new LinkedList<int[]>();
	acceptConnection_trafficVolume = new double[0];
        acceptConnection_attributes = new LinkedList<Map<String, String>>();
	blockConnection_reason = "";
        addConnectionRoute_connId = -1;
	addConnectionRoute_trafficVolume = -1;
	addConnectionRoute_seqLinks = new int[0];
        addConnectionRoute_attributes = new HashMap<String, String>();
	modifyConnectionRoute_connRouteId = -1;
	modifyConnectionRoute_trafficVolume = -1;
	modifyConnectionRoute_attributes = null;
	releaseConnection_connId = -1;
        removeConnectionRoute_connRouteId = -1;
    }

    /**
     * Returns a <code>String</code> representation of the object.
     *
     * @return <code>String</code> representation of the object
     * @since 0.2.0
     */
    @Override
    public String toString()
    {
	switch(getActionType())
	{
	    case ACCEPT_REQUEST:
                StringBuilder connInfo = new StringBuilder();
                connInfo.append("Connection request accepted");
                connInfo.append(StringUtils.getLineSeparator());
                ListIterator<int[]> itRoute = acceptConnection_seqLinks.listIterator();
                ListIterator<Map<String, String>> itAttributes = acceptConnection_attributes.listIterator();
                boolean isFirstRoute = true;
                while(itRoute.hasNext())
                {
                    int routeId = itRoute.nextIndex();
                    double thisRouteCarriedTraffic = acceptConnection_trafficVolume[routeId];
                    int[] thisRouteSeqLinks = itRoute.next();
                    Map<String, String> thisRouteAttributes = itAttributes.next();
                    
                    connInfo.append(String.format(" - Route %d: seq. links = %s, traffic volume = %.3f E, attributes = %s", routeId, IntUtils.join(thisRouteSeqLinks, " "), thisRouteCarriedTraffic, thisRouteAttributes.isEmpty() ? "none" : StringUtils.mapToString(thisRouteAttributes, "=", ", ")));
                    if (isFirstRoute)
                    {
                        connInfo.append(StringUtils.getLineSeparator());
                        isFirstRoute = false;
                    }
                }
                
                return connInfo.toString();

            case ADD_CONNECTION_ROUTE:
                return String.format("Add connection route (connection = %d, seq. links = %s, traffic volume = %.3f E, attributes = %s)", addConnectionRoute_connId, IntUtils.join(addConnectionRoute_seqLinks, " => "), addConnectionRoute_trafficVolume, addConnectionRoute_attributes.isEmpty() ? "none" : StringUtils.mapToString(addConnectionRoute_attributes, "=", ", "));

            case BLOCK_REQUEST:
		return String.format("Connection request blocked (reason = %s)", blockConnection_reason);

	    case MODIFY_CONNECTION_ROUTE:
                String modifyConnRoute_trafficVolume = modifyConnectionRoute_trafficVolume == -1 ? "no change" : Double.toString(modifyConnectionRoute_trafficVolume);
                String modifyConnRoute_attributes = modifyConnectionRoute_attributes == null ? "no change" : StringUtils.mapToString(modifyConnectionRoute_attributes, "=", ", ");
                
		return String.format("Modify connection route %d (traffic volume = %s, attributes = %s)", modifyConnectionRoute_connRouteId, modifyConnRoute_trafficVolume, modifyConnRoute_attributes);

	    case RELEASE_CONNECTION:
		return String.format("Release connection %d", releaseConnection_connId);
                
            case REMOVE_CONNECTION_ROUTE:
                return String.format("Remove connection route %d", removeConnectionRoute_connRouteId);

	    default:
		throw new RuntimeException("Bad - Unknown action type");
	}
    }
}
