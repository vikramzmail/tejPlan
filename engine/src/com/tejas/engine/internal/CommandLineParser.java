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

package com.tejas.engine.internal;

import com.tejas.engine.utils.Triple;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * Extends the <code>PosixParser</code> to modify the <code>processOption</code>
 * method.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class CommandLineParser extends PosixParser
{
    /**
     * Modifies the original method to bypass (for convenience) options not
     * defined in the <code>Options</code> object.
     *
     * @param arg  The <code>String</code> value representing an <code>Option</code>
     * @param iter The iterator over the flattened command line arguments
     * @since 0.2.2
     */
    @Override
    protected void processOption(String arg, ListIterator iter) throws ParseException
    {
	boolean hasOption = getOptions().hasOption(arg);
	if (!hasOption) return;

	super.processOption(arg, iter);
    }
    
    /**
     * Gets the current parameters from the user-specified ones, taking default
     * values for unspecified parameters.
     * 
     * @param defaultParameters Default parameters (key, value, and description)
     * @param inputParameters   User parameters (key, value)
     * @return Current parameters (key, value)
     * @since 0.2.2
     */
    public static Map<String, String> getParameters(List<Triple<String, String, String>> defaultParameters, Properties inputParameters)
    {
        Map<String, String> parameters = new HashMap<String, String>();

        for(Triple<String, String, String> param : defaultParameters)
	{
	    if (param.getFirst().startsWith("alg_"))
	    {
		parameters.put(param.getFirst() + "_File", "");
		parameters.put(param.getFirst() + "_Algorithm", "");
		parameters.put(param.getFirst() + "_Parameters", "");
	    }
	    else
	    {
                parameters.put(param.getFirst(), param.getSecond());
	    }
	}

        for(Map.Entry<Object, Object> param : inputParameters.entrySet())
            parameters.put(param.getKey().toString(), param.getValue().toString());

        return parameters;
    }
}
