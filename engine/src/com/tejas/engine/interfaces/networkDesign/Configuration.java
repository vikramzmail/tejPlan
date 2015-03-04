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

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.tejas.engine.internal.SystemUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * <p>Class containing current Net2Plan-wide options, and methods to work with them.</p>
 *
 * <p>In the current version the available options are:</p>
 *
 * <ul>
 * <li><tt>averagePacketLengthInBytes</tt> (double): Average packet length measured in bytes. Default: 340.46</li>
 * <li><tt>binaryRateInBitsPerSecondPerErlang</tt> (double): Binary rate in bps per Erlang. Default: 1e9</li>
 * <li><tt>classpath</tt> (String): Set of external libraries loaded at runtime (separated by semi-colon). Default: None</li>
 * <li><tt>defaultRunnableCodePath</tt> (String): Default path (either .jar file or folder) for external code. Default: <code>BuiltInExamples.jar</code> file</li>
 * <li><tt>precisionFactor</tt> (double): Precision factor for checks to overcome numeric errors. Default: 1e-3</li>
 * <li><tt>propagationSpeedInKmPerSecond</tt> (double): Propagation speed in the transmission media measured in km/s. On internal computations, a zero or negative value implies the propagation delay is equal to zero. Default: 2e5</li>
 * </ul>
 *
 * <p>In addition, due to the close relation to JOM library, some JOM-specific options can be configured:</p>
 *
 * <ul>
 * <li><tt>defaultILPSolver</tt> (String): Default solver for LP/ILP models. Default: glpk</li>
 * <li><tt>defaultNLPSolver</tt> (String): Default solver for NLP models. Default: ipopt</li>
 * <li><tt>cplexSolverLibraryName</tt> (String): Default path for cplex library (.dll/.so file). Default: None</li>
 * <li><tt>glpkSolverLibraryName</tt> (String): Default path for glpk library (.dll/.so file). Default: None</li>
 * <li><tt>ipoptSolverLibraryName</tt> (String): Default path for ipopt library (.dll/.so file). Default: None</li>
 * </ul>

* <p><b>Important</b>: Values are stored in <code>String</code> format. Users are
 * responsible to make conversions to the appropiate type (i.e. <code>double</code>).</p>
 *
 * <p><b>Important</b>: Users should not access this class directly. All interfaces
 * for implementing user-made code (i.e. algorithms) include a map so-called
 * <code>net2planParameters</code> as input parameter, where users can find the
 * current configuration of the tool.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class Configuration
{
    private final static File optionsFile;
    private static File currentOptionsFile;

    private final static Map<String, String> options;

    static
    {
	options = new HashMap<String, String>();
	options.put("averagePacketLengthInBytes", "340.46");
	options.put("binaryRateInBitsPerSecondPerErlang", "1e9");
	options.put("classpath", "");
	options.put("precisionFactor", "1e-3");
	options.put("propagationSpeedInKmPerSecond", "2e5");
	options.put("cplexSolverLibraryName", "");
	options.put("glpkSolverLibraryName", "c:\\windows\\system32\\glpk_4_54.dll");
	options.put("ipoptSolverLibraryName", "");
        options.put("defaultILPSolver", "glpk");
        options.put("defaultNLPSolver", "ipopt");
        options.put("defaultRunnableCodePath", SystemUtils.getCurrentDir() + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "BuiltInExamples.jar");

	optionsFile = new File(SystemUtils.getCurrentDir() + SystemUtils.getDirectorySeparator() + "options.ini");
        currentOptionsFile = optionsFile;
    };

    /**
     * Checks the given options for validity.
     *
     * @param net2planParameters A key-value map with <code>Net2Plan</code>-wide configuration options
     * @since 0.2.2
     */
    public static void check(Map<String, String> net2planParameters)
    {
	try
	{
	    Double averagePacketLengthInBytes = Double.parseDouble(net2planParameters.get("averagePacketLengthInBytes"));
	    if (averagePacketLengthInBytes <= 0) throw new Exception("");
	}
	catch(Exception ex)
	{
	    throw new Net2PlanException("'averagePacketLengthInBytes' option must be greater than zero");
	}

	try
	{
	    Double binaryRateInBitsPerSecondPerErlang = Double.parseDouble(net2planParameters.get("binaryRateInBitsPerSecondPerErlang"));
	    if (binaryRateInBitsPerSecondPerErlang <= 0) throw new Exception("");
	}
	catch(Exception ex)
	{
	    throw new Net2PlanException("'binaryRateInBitsPerSecondPerErlang' option must be greater than zero");
	}

	try
	{
	    Double propagationSpeedInKmPerSecond = Double.parseDouble(net2planParameters.get("propagationSpeedInKmPerSecond"));
	}
	catch(Exception ex)
	{
	    throw new Net2PlanException("'propagationSpeedInKmPerSecond' option must be a valid number");
	}

	try
	{
	    Double precisionFactor = Double.parseDouble(net2planParameters.get("precisionFactor"));
	    if (precisionFactor <= 0) throw new Exception("");
	}
	catch(Exception ex)
	{
	    throw new Net2PlanException("'precisionFactor' option must be greater than zero");
	}
    }

    /**
     * Checks value of current options.
     *
     * @since 0.2.0
     */
    private static void check()
    {
	check(options);
    }

    /**
     * Reads options from a given file.
     *
     * @param f Options file
     * @throws IOException If the specified file cannot be loaded
     * @since 0.2.0
     */
    public static void readFromOptionsFile(File f) throws IOException
    {
	Map<String, String> old_options = new HashMap<String, String>(options);
	Properties p = new Properties();

	try
	{
	    try (InputStream in = new FileInputStream(f))
	    {
		p.load(in);
	    }

	    for (Entry<Object, Object> entry : p.entrySet())
	    {
		options.put(entry.getKey().toString(), entry.getValue().toString());
	    }

	    check();

	    currentOptionsFile = f.getAbsoluteFile();
	}
	catch (FileNotFoundException ex)
	{
	    throw new IOException("Options file not found (" + f + "), default options were loaded");
	}
	catch (Throwable ex1)
	{
	    options.putAll(old_options);
	    throw new IOException(String.format("%s%n%s", ex1.getMessage(), "Default options were loaded"));
	}
    }

    /**
     * Reads options from the default file.
     *
     * @throws IOException If the specified file cannot be loaded
     * @since 0.2.0
     */
    public static void readFromOptionsDefaultFile() throws IOException
    {
        readFromOptionsFile(optionsFile);
    }

    /**
     * Returns the current map of options.
     *
     * @return Map of current options
     * @since 0.2.0
     */
    public static Map<String, String> getOptions()
    {
	return new HashMap<String, String>(options);
    }

    /**
     * Returns the value of an option.
     *
     * @param option Option name
     * @return Option value
     * @since 0.2.0
     */
    public static String getOption(String option)
    {
	if (!options.containsKey(option)) throw new RuntimeException("Unknown option '" + option + "'");

	String value = options.get(option);
	return value == null ? "" : value;
    }

    /**
     * Puts the value for an option. If an option already exists, its value will be overriden.
     *
     * @param option Option name
     * @param value  Option value
     * @since 0.2.0
     */
    public static void setOption(String option, String value)
    {
        boolean isPresent = options.containsKey(option);
        String oldValue = null;
        
        try
        {
            if (isPresent) oldValue = options.get(option);
            options.put(option, value);
            check(options);
        }
        catch(Throwable e)
        {
            if (isPresent)
                options.put(option, oldValue);
        }
    }

    /**
     * Saves current options to the file system.
     *
     * @since 0.2.0
     */
    public static void saveOptions()
    {
	check();

	Properties p = new Properties();

	for (Entry<String, String> entry : options.entrySet())
	    p.setProperty(entry.getKey(), entry.getValue());

	try (OutputStream out = new FileOutputStream(currentOptionsFile))
	{
	    p.store(out, "Options file from Net2Plan");
	}
	catch (Throwable ex)
	{
	    throw new RuntimeException(ex);
	}
    }
}
