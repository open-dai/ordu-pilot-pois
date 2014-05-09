/*
 *    Geotools2 - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002-2008, Geotools Project Managment Committee (PMC)
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
 */
package com.sampas.socbs.core.data.arcsde.impl;

import java.io.IOException;
import java.util.NoSuchElementException;
import org.geotools.data.FeatureListenerManager;
import org.geotools.data.FeatureReader;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureType;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * A FeatureWriter for auto commit mode.
 * 
 * @author Gabriel Roldan (TOPP)
 * @version $Id: AutoCommitFeatureWriter.java 29625 2008-03-14 13:55:02Z groldan $
 * @since 2.5
 * @source $URL:
 *         http://svn.geotools.org/geotools/trunk/gt/modules/plugin/arcsde/datastore/src/main/java/org/geotools/arcsde/data/AutoCommitFeatureWriter.java $
 */
class AutoCommitFeatureWriter extends ArcSdeFeatureWriter {

    public AutoCommitFeatureWriter(final FIDReader fidReader,
                                   final FeatureType featureType,
                                   final FeatureReader/*<SimpleFeatureType, SimpleFeature>*/ filteredContent,
                                   final ArcSDEPooledConnection connection,
                                   final FeatureListenerManager listenerManager,
                                   final ArcSdeVersionHandler versionHandler) throws NoSuchElementException,
                                                                             IOException {

        super(fidReader, featureType, filteredContent, connection, listenerManager, versionHandler);
    }

   
    //@Override
    protected void doFireFeaturesAdded(String typeName, ReferencedEnvelope bounds) {
        listenerManager.fireFeaturesAdded(typeName, Transaction.AUTO_COMMIT, bounds, false);
    }

    //@Override
    protected void doFireFeaturesChanged(String typeName, ReferencedEnvelope bounds) {
        listenerManager.fireFeaturesChanged(typeName, Transaction.AUTO_COMMIT, bounds, false);
    }

    //@Override
    protected void doFireFeaturesRemoved(String typeName, ReferencedEnvelope bounds) {
        listenerManager.fireFeaturesRemoved(typeName, Transaction.AUTO_COMMIT, bounds, false);
    }

}
