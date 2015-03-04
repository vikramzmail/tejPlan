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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class enclosing the current version of the kernel.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class Version
{
    private final static String version;
    private final static boolean unstable = false;
    
    static
    {
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
        version = "Net2Plan 0.2.3" + (unstable ? String.format(" [Build %s]", f.format(new Date())) : "");
    }
    
    /**
     * Returns a <code>String</code> representation of the object.
     *
     * @return <code>String</code> representation of the object
     * @since 0.2.3
     */
    public static String getVersion()
    {
        return version;
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
        return getVersion();
    }
}
