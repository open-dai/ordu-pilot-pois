/*
 *    Geotools2 - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002-2006, Geotools Project Managment Committee (PMC)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 */
package com.sampas.socbs.core.data.arcsde.impl;

import java.io.IOException;

/**
 * Exception thrown when a free SDE connection can't be obtained after the
 * calling thread was waiting an available connection for
 * <code>SdeConnectionPool instance's getMaxWaitTime()</code> milliseconds
 * 
 * @author Gabriel Roldan
 * @source $URL:
 *         http://svn.geotools.org/geotools/trunk/gt/modules/plugin/arcsde/datastore/src/main/java/org/geotools/arcsde/pool/UnavailableArcSDEConnectionException.java $
 * @version $Id: UnavailableArcSDEConnectionException.java 28039 2007-11-25
 *          13:03:08Z groldan $
 */
public class UnavailableArcSDEConnectionException extends IOException {
    /**
     * serial version id
     */
    private static final long serialVersionUID = -7964603239735118491L;

    /**
     * Creates a new UnavailableArcSDEConnectionException object.
     * 
     * @param usedConnections
     *            DOCUMENT ME!
     * @param config
     *            DOCUMENT ME!
     */
    public UnavailableArcSDEConnectionException(int usedConnections, ArcSDEConnectionConfig config) {
        super("The maximun of " + usedConnections + " to " + config.toString()
                + " has been reached");
    }
}
