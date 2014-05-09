/*
 *    Geotools2 - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002, Geotools Project Managment Committee (PMC)
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

import net.sf.jsqlparser.schema.Table;
import com.esri.sde.sdk.client.SeConnection;
import com.esri.sde.sdk.client.SeException;

/**
 * Utility used to qualify table names
 * 
 * @author Gabriel Roldan, Axios Engineering
 * @version $Id: TableQualifier.java 29625 2008-03-14 13:55:02Z groldan $
 * 
 * @source $URL:
 *         http://svn.geotools.org/geotools/trunk/gt/modules/plugin/arcsde/datastore/src/main/java/org/geotools/arcsde/data/view/TableQualifier.java $
 * @since 2.3.x
 */
class TableQualifier {
    /**
     * Returns a Table with the same name as the argument one but fully
     * qualified in the ArcSDE sense.
     * 
     * @param conn
     *            connection to obtain database and user name from
     * @param table
     *            table whose schema name is to be qualified
     * 
     * @return a qualified Table.
     * 
     * @throws IllegalStateException
     *             if an SDE error is catched up while asking <code>conn</code>
     *             for the database and user name.
     */
    public static Table qualify(SeConnection conn, Table table) throws IllegalStateException {
        if (table == null) {
            return null;
        }

        final Table qualifiedTable = new Table();
        final String databaseName;
        final String userName;

        qualifiedTable.setName(table.getName());
        qualifiedTable.setAlias(table.getAlias());

        //String schema = table.getSchemaName(); // user name in sde land

        // if (schema != null) {
        try {
            databaseName = conn.getDatabaseName();
            userName = conn.getUser();
        } catch (SeException e) {
            throw new IllegalStateException("getting database name: " + e.getMessage());
        }

        // we'll replace the table schema name by
        // databaseName.userName
        String qualifiedSchema = databaseName;
        if ("".equals(qualifiedSchema)) {
            qualifiedSchema = userName;
        } else {
            qualifiedSchema += ("." + userName);
        }

        qualifiedTable.setSchemaName(qualifiedSchema);
        // }

        return qualifiedTable;
    }
}
