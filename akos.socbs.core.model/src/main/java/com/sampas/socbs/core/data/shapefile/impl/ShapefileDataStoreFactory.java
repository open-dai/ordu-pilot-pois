/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002-2006, Geotools Project Managment Committee (PMC)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package com.sampas.socbs.core.data.shapefile.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.data.FileDataStoreFactorySpi;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Implementation of the DataStore service provider interface for Shapefiles.
 * <p>
 * The specific implementation of ShapefileDataStore created by this class is not specified. For
 * more information on the connection parameters please review the following public Param constants.
 * <ul>
 * <li>{@link URLP}
 * <li>{@link NAMESPACEP}
 * <li>{@link CREATE_SPATIAL_INDEX}
 * <li>{@link MEMORY_MAPPED}
 * <li>{@link DBFCHARSET}
 * </ul>
 * 
 * @author Chris Holmes, TOPP
 * @source $URL:
 *         http://svn.geotools.org/geotools/trunk/gt/modules/plugin/shapefile/src/main/java/org/geotools/data/shapefile/ShapefileDataStoreFactory.java $
 * @version $Id: ShapefileDataStoreFactory.java 29603 2008-03-11 22:49:05Z jgarnett $
 */
@SuppressWarnings("unchecked")
public class ShapefileDataStoreFactory implements FileDataStoreFactorySpi {
    
    protected static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("com.sampas.socbs.core.data.shapefile.impl");
    /**
     * url to the .shp file.
     */
    public static final Param URLP = new Param("url", URL.class,
        "url to a .shp file");

    /**
     * Optional - uri of the FeatureType's namespace
     */
    public static final Param NAMESPACEP = new Param("namespace", URI.class,
    "uri to a the namespace",false); //not required
    
    /**
     * Optional - enable/disable the use of memory-mapped io
     */
    public static final Param MEMORY_MAPPED =
        new Param("memory mapped buffer", Boolean.class,
                  "enable/disable the use of memory-mapped io", false);
        
    /**
     * Optional - Enable/disable the automatic creation of spatial index
     */
    public static final Param CREATE_SPATIAL_INDEX = new Param("create spatial index",
            Boolean.class,
            "enable/disable the automatic creation of spatial index", false);
    
    /**
     * Optional - character used to decode strings from the DBF file
     */
    public static final Param DBFCHARSET = new Param("charset", Charset.class,
            "character used to decode strings from the DBF file", false, Charset.forName("ISO-8859-1")) {
            /* This is an example of a non simple Param type where a custom parse
             * method is required.
             * @see org.geotools.data.DataStoreFactorySpi.Param#parse(java.lang.String)
             */
            public Object parse(String text) throws IOException {
                return Charset.forName(text);
            }

            public String text(Object value) {
                return ((Charset) value).name();
            }
        };
        
    /**
     * It is the users responsibility to prevent the creation of duplicate DataStore instances
     * (they should hold the results in a Map, Repository or Catalog as described in the documenation).
     * <p>
     * However in the real world we are patching the problem, this Map is used to store
     * previously created DataStores so we can hand them out again. There are two problems with
     * this approach:
     * <ul>
     * <li>The ShapefileDataStoreFactory may be created more than once with different Hints, for example when
     * used to read a shapefile into alternate Feature or Geometry representations.
     * <li>It flys in the face of a DataStore lifecycle, the JDBC DataStore implementations
     * have taken to support a dispose operation.
     * </ul>
     * As such I expect this "feature" to disappear, I am keeping it now only so we can
     * produce good warnings.
     */
	private static Map liveStores=Collections.synchronizedMap(new HashMap());
    

    /**
     * Takes a map of parameters which describes how to access a DataStore
     * and determines if it can be read by the ShapefileDataStore or IndexedShapefileDataStore
     * implementations.
     * 
     * @param params A map of parameters describing the location of a datastore.
     *        Files should be pointed to by a 'url' param.
     *
     * @return true iff params contains a url param which points to a file
     *         ending in shp
     */
    public boolean canProcess(Map params) {
        boolean accept = false;
        if (params.containsKey(URLP.key)) {
            try {
                URL url = (URL) URLP.lookUp(params);
                accept = canProcess(url);
            } catch (IOException ioe) {
                // yes, I am eating this - since it is my job to return a true/false
            }
        }
        return accept;
    }

    /**
     * Returns an instance of DataStore iff the resource pointed to the
     * Map of paramers can be handled as a shapefile.
     * <p>
     * The specific implementation of ShapefileDataStore returned is not specified,
     * and depends on the parameters given. For more information please review the
     * public static Param instances available for this class.
     * </p>
     * <ul>
     * <li>{@link URLP}
     * <li>{@link NAMESPACEP}
     * <li>{@link CREATE_SPATIAL_INDEX}
     * <li>{@link MEMORY_MAPPED}
     * <li>{@link DBFCHARSET}
     * </ul> 
     * @param params A param list with information on the location of a
     *        restore.  For shapefiles this should contain a 'url' param which
     *        points to a file which ends in shp.
     *
     * @return DataStore A ShapefileDatastore
     * @throws IOException If a connection error (such as the file not existing occurs)
     * @throws DataSourceException Thrown if the datastore which is created
     *         cannot be attached to the restore specified in params.
     */
    public DataStore createDataStore(Map params) throws IOException {
    	DataStore ds = null;
    	synchronized (liveStores) {
            if ( !liveStores.containsKey(params) ){	
    	        URL url = null;
    	        try {
    	        	ds=createDataStoreInstance(params);
    	            liveStores.put(params,ds);
    	        } catch (MalformedURLException mue) {
    	            throw new DataSourceException("Unable to attatch datastore to "
    	                + url, mue);
    	        } 
        	} else {
        		ds=(DataStore) liveStores.get(params);
        	}
    	}
        return ds;
    }
    /**
     * Creates a new DataStore - for a file that does not exist yet.
     * <p>
     * This method has different logic than createDataStore. It is willing
     * to be memory mapped, and generate an index for a local file that
     * does not exist yet.
     * 
     */
    public DataStore createNewDataStore( Map params ) throws IOException {
        DataStore ds = null;
        synchronized (liveStores) {
            if ( !liveStores.containsKey(params) ){ 
                URL url = null;
                try {
                    ds=createNewShapefile(params);
                    liveStores.put(params,ds);
                } catch (MalformedURLException mue) {
                    throw new DataSourceException("Unable to attatch datastore to "
                        + url, mue);
                } 
            } else {
                ds=(DataStore) liveStores.get(params);
            }
        }
        return ds;
    }

    /**
     * Will create a new shapefile baed on the provided parameters.
     *
     * @param params Map of parameters
     * @throws IOException If the filename is not valid.
     * @throws UnsupportedOperationException
     */
    DataStore createNewShapefile( Map params ) throws IOException{
        URL url = (URL) URLP.lookUp(params);
        Boolean isMemoryMapped = (Boolean) MEMORY_MAPPED.lookUp(params);
        URI namespace = (URI) NAMESPACEP.lookUp(params);  
        Charset dbfCharset = (Charset) DBFCHARSET.lookUp(params);
        Boolean isCreateSpatialIndex = (Boolean) CREATE_SPATIAL_INDEX.lookUp(params);
        
        if (isCreateSpatialIndex == null) {
            // should not be needed as default is TRUE
            assert( true );
            isCreateSpatialIndex = Boolean.TRUE;
        }
        if (dbfCharset == null) {
            assert( true );            
            // this should not happen as Charset.forName("ISO-8859-1") was used as the param default?
            dbfCharset = Charset.forName("ISO-8859-1");
        }
        if (isMemoryMapped == null){
            assert( true );            
            // this should not happen as false was the default
            isMemoryMapped = Boolean.FALSE;
        }
        URL shpUrl = toShpURL(url);
        
        boolean isLocal = shpUrl.getProtocol().equalsIgnoreCase("file");        
        if( !isLocal || new File(shpUrl.getFile()).exists() ){
            LOGGER.warning("File already exists: "+shpUrl );
        }
        boolean useMemoryMappedBuffer = isLocal && isMemoryMapped.booleanValue();
        boolean createIndex = isCreateSpatialIndex.booleanValue() && isLocal;
        
        try {                        
            if (createIndex){
                return new IndexedShapefileDataStore(url, namespace, useMemoryMappedBuffer, true, IndexedShapefileDataStore.TREE_QIX, dbfCharset );
            }
            else {
                return new ShapefileDataStore(url, namespace, useMemoryMappedBuffer, dbfCharset);
            }            
        } catch (MalformedURLException mue) {
            throw new DataSourceException("Url for shapefile malformed: "+ url, mue);
        }  
    }

    /**
     * Will create the correct implementation of ShapefileDataStore for the provided
     * parameters.
     *
     * @param params Map of parameters
     * @throws IOException If the specified shapefle could not be accessed
     * @throws UnsupportedOperationException
     */
    DataStore createDataStoreInstance(Map params) throws IOException{
        URL url = (URL) URLP.lookUp(params);
        Boolean isMemoryMapped = (Boolean) MEMORY_MAPPED.lookUp(params);
        URI namespace = (URI) NAMESPACEP.lookUp(params);
        Charset dbfCharset = (Charset) DBFCHARSET.lookUp(params);
        Boolean isCreateSpatialIndex = (Boolean) CREATE_SPATIAL_INDEX.lookUp(params);
        
        if (isCreateSpatialIndex == null) {
            // should not be needed as default is TRUE
            isCreateSpatialIndex = Boolean.TRUE;
        }
        if (dbfCharset == null) {
            // this should not happen as Charset.forName("ISO-8859-1") was used as the param default?
            dbfCharset = Charset.forName("ISO-8859-1");
        }
        if (isMemoryMapped == null){
            isMemoryMapped = Boolean.FALSE;
        }
        
        URL shpUrl = toShpURL(url);        
        boolean isLocal = shpUrl.getProtocol().equalsIgnoreCase("file");
        if( isLocal &&  !new File(shpUrl.getFile()).exists() ){
            throw new FileNotFoundException( "Shapefile not found:"+shpUrl.getFile() );
        }
        boolean useMemoryMappedBuffer = isLocal && new File(shpUrl.getFile()).exists() && isMemoryMapped.booleanValue();
        boolean createIndex = isCreateSpatialIndex.booleanValue() && isLocal;
        byte treeIndex = IndexedShapefileDataStore.TREE_NONE;
        if( isLocal ){
            if( createIndex ){
                treeIndex = IndexedShapefileDataStore.TREE_QIX; // default
            }
            else {
                // lets check and see if any index file is avaialble
                if( new File( toQixURL(url).getFile() ).exists() ){
                    treeIndex = IndexedShapefileDataStore.TREE_QIX;
                }
                else if( new File( toFixURL(url).getFile() ).exists() ){
                    treeIndex = IndexedShapefileDataStore.TREE_GRX;
                } 
            }
        }
        
        try {                        
            if (createIndex ){
                return new IndexedShapefileDataStore(url, namespace, useMemoryMappedBuffer, createIndex, IndexedShapefileDataStore.TREE_QIX, dbfCharset );
            }
            else if( treeIndex != IndexedShapefileDataStore.TREE_NONE ){
                return new IndexedShapefileDataStore(url, namespace, useMemoryMappedBuffer, false, treeIndex, dbfCharset );
            }
            else {
                return new ShapefileDataStore(url, namespace, useMemoryMappedBuffer, dbfCharset);
            }            
        } catch (MalformedURLException mue) {
            throw new DataSourceException("Url for shapefile malformed: "+ url, mue);
        }  
    }

    public String getDisplayName() {
        return "Shapefile";
    }
    /**
     * Describes the type of data the datastore returned by this factory works
     * with.
     *
     * @return String a human readable description of the type of restore
     *         supported by this datastore.
     */
    public String getDescription() {
        return "ESRI(tm) Shapefiles (*.shp)";
    }

//    public DataSourceMetadataEnity createMetadata( Map params )
//            throws IOException {
//        
//        URL url = (URL) URLP.lookUp(params);
//        Boolean mm = (Boolean) MEMORY_MAPPED.lookUp(params);
//        
//        String server;
//        String name;
//        if( url.getProtocol().equals("file")){
//            server = "localhost";
//            name = url.getPath();
//        }
//        else {
//            server = url.getHost()+":"+url.getPort();
//            name = url.getFile();
//        }
//        return new DataSourceMetadataEnity( server, name, "Shapefile access for "+url );
//    }
    /**
     * Test to see if this datastore is available, if it has all the
     * appropriate libraries to construct a datastore.
     * 
     * This datastore just checks for the ShapefileDataStore, IndexedShapefileDataStore
     * and Geometry implementations.
     *
     * @return <tt>true</tt> if and only if this factory is available to create
     *         DataStores.
     */
    public boolean isAvailable() {
        try {
            ShapefileDataStore.class.getName();
            IndexedShapefileDataStore.class.getName();
            Geometry.class.getName();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Describe parameters.
     *
     *
     * @see org.geotools.data.DataStoreFactorySpi#getParametersInfo()
     */
    public Param[] getParametersInfo() {
        return new Param[] {
            URLP, NAMESPACEP, CREATE_SPATIAL_INDEX, DBFCHARSET, MEMORY_MAPPED
        };        
    }

    /**
     * @see org.geotools.data.dir.FileDataStoreFactorySpi#getFileExtensions()
     */
    public String[] getFileExtensions() {
        return new String[] {".shp",};
    }

    /**
     * @see org.geotools.data.dir.FileDataStoreFactorySpi#canProcess(java.net.URL)
     */
    public boolean canProcess(URL f) {
        return f.getFile().toUpperCase().endsWith("SHP");
    }


    /**
     * We may need to create a new datastore if the provided file does not exist.
     * 
     * @see org.geotools.data.dir.FileDataStoreFactorySpi#createDataStore(java.net.URL)
     */
    public DataStore createDataStore(URL url)  throws IOException{
        Map params=new HashMap();
        params.put( URLP.key, url);
        
        boolean isLocal = url.getProtocol().equalsIgnoreCase("file");
        if( isLocal && !(new File(url.getFile()).exists())){
            return createNewDataStore(params);
        }
        else {
            return createDataStore(params);
        }
    }

    /**
     * @see org.geotools.data.dir.FileDataStoreFactorySpi#createDataStore(java.net.URL)
     */
    public DataStore createDataStore(URL url, boolean memorymapped)  throws IOException{
    	Map params=new HashMap();
    	params.put( URLP.key, url);
    	params.put( MEMORY_MAPPED.key, new Boolean(memorymapped) );
        return createDataStore(params);
    }


    /**
     * @see org.geotools.data.dir.FileDataStoreFactorySpi#getTypeName(java.net.URL)
     */
    public String getTypeName(URL url) throws IOException {
        DataStore ds = createDataStore(url);
        String[] names = ds.getTypeNames(); // should be exactly one
        return ((names == null || names.length==0)?null:names[0]);
    }

    /**
     * Returns the implementation hints. The default implementation returns an
     * empty map.
     * <p>
     * When we have FeatureFactory, GeometryFactory and so on hooked up this
     * map will return Hints we paid attention too when we were constructed.
     *
     * @return An empty map.
     */
    public Map getImplementationHints() {
        return Collections.EMPTY_MAP;
    }
    
    /**
     * Convert a URL to a String that is suitable for manipulation of its
     * extension (generally the last three characters of the file).
     * 
     * This uses URL.toExternalForm() in order to preserve valuable
     * information about the URL's Authority.
     * 
     * @param url the url to convert to a String. Must not be null.
     * @return a String representation of the URL
     * @throws MalformedURLException if the url is invalid
     */
    public static String toFilename( URL url ) throws MalformedURLException {
        if (url == null) {
            throw new NullPointerException("A shapefile URL is required");
        }
        try {
        	/*
        	 * The use of the four parameter URL constructor is discouraged
        	 * as it throws away valuable information about the authority that
        	 * the URL is hosted by. It is better if we use the full URL
        	 * String to reconstruct the other URLs.
        	 */
            return java.net.URLDecoder.decode(url.toExternalForm(),"US-ASCII");
        } catch (java.io.UnsupportedEncodingException use) {
            throw new java.net.MalformedURLException("Unable to decode " + url
                + " cause " + use.getMessage());
        }
    }
    /**
     * Convert the URL to a string that includes the provided
     * extension (this method respects the case of the original
     * file extension.
     * 
     * This implementation calls toFileName( URL ) and then knocks off
     * the known extensions (ie "shp", "dbf", "shx") if present and makes
     * use of the provided extension.
     * 
     * 
     * @param url
     * @param ext
     * @return
     */
    public static String toFilename( URL url, String ext ) throws java.net.MalformedURLException {
    	String filename = toFilename(url);
    	if( ext != null && !ext.startsWith(".")){
    		ext += ".";
    	}
    	
        if (filename.endsWith(".shp") || filename.endsWith(".dbf")
                || filename.endsWith(".shx")) {
            filename = filename.substring(0, filename.length() - 4);
            if( ext != null ){
            	filename += ext.toLowerCase();
            }
        } else if (filename.endsWith(".SHP") || filename.endsWith(".DBF")
                || filename.endsWith(".SHX")) {
            filename = filename.substring(0, filename.length() - 4);
            if( ext != null ){
                filename += ext.toUpperCase();            
            }
        }
        else if( ext != null ){
            filename += ext.toLowerCase();
        }
        return filename;
    }
    
    /** Figure out the URL for the "fix" file */
    public static URL toFixURL( URL url ) throws java.net.MalformedURLException {
        String filename = toFilename(url, ".fix");
        return new URL(filename);
    }
    /** Figure out the URL for the "qix" file */
    public static URL toQixURL( URL url ) throws java.net.MalformedURLException {
        String filename = toFilename(url,".qix");
        return new URL(filename);
    }
    
    /** Figure out the URL for the "grx" file */
    public static URL toGrxURL( URL url ) throws java.net.MalformedURLException {
        String filename = toFilename(url, ".grx");
        return new URL(filename);
    }
    
    /** Figure out the URL for the "prj" file */
    public static URL toXmlURL( URL url ) throws java.net.MalformedURLException {
        String filename = toFilename(url, ".shp.xml");
        return new URL(filename);
    }

    /** Figure out the URL for the "prj" file */
    public static URL toPrjURL( URL url ) throws java.net.MalformedURLException {
        String filename = toFilename(url,".prj");
        return new URL(filename);
    }

    /** Figure out the URL for the "shx" file */
    public static URL toShxURL( URL url ) throws java.net.MalformedURLException {
        String filename = toFilename(url,".shx");
        return new URL(filename);
    }

    /** Figure out the URL for the "dbf" file */
    public static URL toDbfURL( URL url ) throws java.net.MalformedURLException {
        String filename = toFilename(url,".dbf");
        return new URL(filename);
    }

    /** Figure out the URL for the "shp" file */
    public static URL toShpURL( URL url ) throws java.net.MalformedURLException {
        String filename = toFilename(url,".shp");
        return new URL(filename);
    }
}
