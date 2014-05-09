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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.feature.FeatureType;
import org.geotools.filter.FilterCapabilities;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.PropertyName;

/**
 * Encodes an attribute filter into a SQL WHERE statement for arcsde.
 * 
 * <p>
 * Although not all filters support is coded yet, the strategy to filtering
 * queries for ArcSDE datasources is separated in two parts, the SQL where
 * clause construction, provided here and the spatial filters (or spatial
 * constraints, in SDE vocabulary) provided by <code>GeometryEncoderSDE</code>;
 * mirroring the java SDE api approach for easy programing
 * </p>
 * 
 * @author Saul Farber
 * @author Gabriel Roldan
 * 
 * @see org.geotools.data.sde.GeometryEncoderSDE
 * @source $URL:
 *         http://gtsvn.refractions.net/geotools/branches/2.4.x/modules/unsupported/arcsde/datastore/src/main/java/org/geotools/arcsde/filter/FilterToSQLSDE.java $
 */
@SuppressWarnings("unchecked")
public class FilterToSQLSDE extends FilterToSQL implements FilterVisitor {
    /** Standard java logger */
    private static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger("org.geotools.filter");

    private final String layerQualifiedName;

    private final String layerFidFieldName;

    /** DOCUMENT ME! */
    private final PlainSelect definitionQuery;

    /**
     * If definitionQuery != null, holds alias/unaliased attribute names from
     * the sql query
     */
    private Map attributeNames = Collections.EMPTY_MAP;

    /**
     * 
     * @param layerQName
     *            full qualified name of the ArcSDE layer
     * @param layerFidColName
     *            name of the column that holds fids
     * @param ft
     * @param definitionQuery
     */
    public FilterToSQLSDE(String layerQName, String layerFidColName, FeatureType ft,
            PlainSelect definitionQuery) {
        this.layerQualifiedName = layerQName;
        this.layerFidFieldName = layerFidColName;
        this.featureType = ft;
        this.definitionQuery = definitionQuery;

        if (definitionQuery != null) {
            attributeNames = new HashMap();

            List selectItems = definitionQuery.getSelectItems();
            SelectItem item;

            for (Iterator it = selectItems.iterator(); it.hasNext();) {
                item = (SelectItem) it.next();

                if (!(item instanceof SelectExpressionItem)) {
                    String msg = "for item '"
                            + item
                            + "': only SelectExpressionItems should be in query at this stage."
                            + " AllColumns and AllTableColumns instances should be resolved to their list "
                            + " of column names at view registration time.";
                    LOGGER.severe(msg);
                    throw new IllegalStateException(msg);
                }
                SelectExpressionItem colDef = (SelectExpressionItem) item;
                String alias = colDef.getAlias();
                if (alias == null) {
                    if (!(colDef.getExpression() instanceof Column)) {
                        throw new RuntimeException(
                                "if select item is not a plain column an alias should be provided: "
                                        + colDef);
                    }
                    Column column = (Column) colDef.getExpression();
                    alias = column.getColumnName();
                }

                attributeNames.put(alias, colDef);
            }
        }
    }

    /**
     * Returns the full qualifed name of sql expression that is registered as
     * the source of the attribute named <code>alias</code>.
     * 
     * @param alias
     * @return
     */
    public String getColumnDefinition(final String alias) {
        final String encodedColumnDefinition;
        if (this.definitionQuery != null) {
            // its an inprocess view
            SelectExpressionItem colDef = (SelectExpressionItem) attributeNames.get(alias);
            // String alias = colDef.getAlias();
            String sqlExpression = String.valueOf(colDef);

            // encodedColumnDefinition = sqlExpression + " AS " + alias;
            encodedColumnDefinition = sqlExpression;
        } else {
            String nonNsPrefixedAlias = alias;
            if (alias.indexOf(":") != -1) {
                // we've got to 'de-namespaceify' this attribute, if neccesary
                nonNsPrefixedAlias = alias.substring(alias.indexOf(":") + 1);
            }
            encodedColumnDefinition = layerQualifiedName + "." + nonNsPrefixedAlias;
        }
        return encodedColumnDefinition;
    }

    /**
     * Overrides the superclass implementation to indicate that we support
     * pushing FeatureId filters down into the data store.
     * 
     * @return DOCUMENT ME!
     */
    // @Override
    protected FilterCapabilities createFilterCapabilities() {
        FilterCapabilities capabilities = new FilterCapabilities();

        capabilities.addAll(FilterCapabilities.LOGICAL_OPENGIS);
        capabilities.addAll(FilterCapabilities.SIMPLE_COMPARISONS_OPENGIS);
        capabilities.addType(PropertyIsNull.class);
        capabilities.addType(PropertyIsBetween.class);
        capabilities.addType(Id.class);
        capabilities.addType(IncludeFilter.class);
        capabilities.addType(ExcludeFilter.class);
        capabilities.addType(PropertyIsLike.class);

        return capabilities;
    }

    /**
     * overriden just to avoid the "WHERE" keyword
     * 
     * @param out
     *            DOCUMENT ME!
     * @param filter
     *            DOCUMENT ME!
     * 
     * @throws GeoAPIFilterToSQLEncoderException
     *             DOCUMENT ME!
     */
    // @Override
    public void encode(Filter filter) throws FilterToSQLException {
        if (getCapabilities().fullySupports(filter)) {
            filter.accept(this, null);
        } else {
            throw new FilterToSQLException("Filter type not supported");
        }
    }

    /**
     * This only exists the fulfill the interface - unless There is a way of
     * determining the FID column in the database...
     * 
     * @param filter
     *            the Fid Filter.
     * 
     * @throws RuntimeException
     *             DOCUMENT ME!
     */
    // @Override
    public Object visit(final Id filter, final Object unused) {
        final Set/*<Identifier>*/ identifiers = filter.getIdentifiers();
        if (identifiers.size() == 0) {
            Filter.EXCLUDE.accept(this, unused);
            return unused;
        }
        long[] fids = ArcSDEAdapter.getNumericFids(identifiers);

        int nFids = fids.length;

        if (nFids == 0) {
            return unused;
        }

        String fidField = layerFidFieldName;

        try {
            StringBuffer sb = new StringBuffer();
            sb.append("(" + fidField + " IN(");

            for (int i = 0; i < nFids; i++) {
                sb.append(fids[i]);

                if (i < (nFids - 1)) {
                    sb.append(", ");
                }
                if (i == 999) {
                    sb.deleteCharAt(sb.length() - 1); // delete the trailing
                    // space
                    sb.deleteCharAt(sb.length() - 1); // delete the trailing
                    // comma
                    sb.append(")) OR (" + fidField + " IN(");
                }
            }

            sb.append("))");

            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("added fid filter: " + sb.toString());
            }

            this.out.write(sb.toString());
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        return unused;
    }

    /**
     * Writes the SQL for the attribute Expression.
     * 
     * NOTE: If the feature type is the product of an in process sql query, the
     * attribute name encoded will be the actual one, not the alias (if any)
     * used in the sql query.
     * 
     * @param expression
     *            the attribute to turn to SQL.
     * 
     * @throws RuntimeException
     *             for io exception with writer
     */
    // @Override
    public Object visit(PropertyName expression, Object extraData) throws RuntimeException {
        LOGGER.finer("exporting PropertyName");
        final String attName = expression.getPropertyName();

        final String encodedColumnDefinition = getColumnDefinition(attName);

        try {
            out.write(encodedColumnDefinition);
        } catch (java.io.IOException ioe) {
            throw new RuntimeException("IO problems writing attribute exp", ioe);
        }

        return extraData;
    }

    /**
     * Overrides to avoid encoding an empty operator if <code>filter</code>
     * has no children.
     */
    // @Override
    protected Object visit(BinaryLogicOperator filter, Object extraData) {
        final List/*<Filter>*/ children = filter.getChildren();
        if (children.isEmpty()) {
            return extraData;
        }
        return super.visit(filter, extraData);
    }

    /**
     * @see {@link FilterVisitor#visit(ExcludeFilter, Object)}
     * 
     * Writes the SQL for the IncludeFilter by writing "FALSE".
     * 
     * @param the
     *            filter to be visited
     */
    // @Override
    public Object visit(ExcludeFilter filter, Object extraData) {
        try {
            out.write("1 = 2");
        } catch (IOException ioe) {
            throw new RuntimeException(IO_ERROR, ioe);
        }
        return extraData;
    }

    /**
     * @see {@link FilterVisitor#visit(IncludeFilter, Object)}
     * 
     * Writes the SQL for the IncludeFilter by writing "TRUE".
     * 
     * @param the
     *            filter to be visited
     * 
     */
    // @Override
    public Object visit(IncludeFilter filter, Object extraData) {
        try {
            out.write("1 = 1");
        } catch (IOException ioe) {
            throw new RuntimeException(IO_ERROR, ioe);
        }
        return extraData;
    }
}