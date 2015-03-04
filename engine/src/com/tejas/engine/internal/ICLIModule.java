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

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Interface for any module to be executed from the command-line user interface.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public interface ICLIModule
{
    /**
     * Executes the module according to the specified command-line call.
     * 
     * @param args Command line parameters
     * @throws ParseException
     * @since 0.2.2
     */
    public void executeFromCommandLine(String[] args) throws ParseException;
    
    /**
     * Returns a human-readable help for this module.
     * 
     * @return Help string
     * @since 0.2.2
     */
    public String getCommandLineHelp();

    /**
     * Returns the set of options for this module.
     * 
     * @return Options for this module
     * @since 0.2.2
     */
    public Options getCommandLineOptions();
}
