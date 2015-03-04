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

/**
 * {@code Net2PlanException} is the superclass of those exceptions that can be
 * thrown during the normal operation of algorithms or reports.
 *
 * It is devoted to situations like wrong input parameters. Contrary to other
 * exceptions, it has a special treatment by Net2Plan kernel: a popup with the
 * message will be thrown, instead of redirecting the message and stack trace
 * to the error console.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class Net2PlanException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new Net2PlanException exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param   message   the detail message. The detail message is saved for
     *          later retrieval by the {@link #getMessage()} method.
     * @since 0.2.0
     */
    public Net2PlanException(String message)
    {
        super(message);
    }
}
