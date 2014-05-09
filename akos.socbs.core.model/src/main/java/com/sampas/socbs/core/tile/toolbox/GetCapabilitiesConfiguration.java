package com.sampas.socbs.core.tile.toolbox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.ows.ServiceException;
import com.sampas.socbs.core.tile.impl.Grid;
import com.sampas.socbs.core.tile.impl.GridCalculator;
import com.sampas.socbs.core.tile.impl.TileLayer;


@SuppressWarnings({"unchecked", "unused"})
public class GetCapabilitiesConfiguration implements Configuration {
    
	private static Log log = LogFactory.getLog(com.sampas.socbs.core.tile.toolbox.GetCapabilitiesConfiguration.class);

    private CacheFactory cacheFactory = null;

    private String url = null;

    private String mimeTypes = null;

    private String metaTiling = null;
    
    private String vendorParameters = null;
    
    private boolean allowCacheBypass = false;

    public GetCapabilitiesConfiguration(CacheFactory cacheFactory, String url,
            String mimeTypes, String metaTiling, String allowCacheBypass) {
        this.cacheFactory = cacheFactory;
        this.url = url;
        this.mimeTypes = mimeTypes;
        this.metaTiling = metaTiling;
        
        if(Boolean.parseBoolean(allowCacheBypass)) {
            this.allowCacheBypass = true;
        }
        
        //log.info("Constructing from url " + url);
    }
    
    public GetCapabilitiesConfiguration(CacheFactory cacheFactory, String url,
            String mimeTypes, String metaTiling, String vendorParameters, 
            String allowCacheBypass) {
        this.cacheFactory = cacheFactory;
        this.url = url;
        this.mimeTypes = mimeTypes;
        this.metaTiling = metaTiling;
        this.vendorParameters = vendorParameters;
        
        if(Boolean.parseBoolean(allowCacheBypass)) {
            this.allowCacheBypass = true;
        }
        
        //log.info("Constructing from url " + url);
    }

    /**
     * Identifier for this Configuration instance
     * 
     * @return the URL given to the constructor
     */
    public String getIdentifier() {
        return url;
    }

    /**
     * Gets the XML document and parses it, creates WMSLayers for the relevant
     * 
     * @return the layers described at the given URL
     */
    public List<TileLayer> getTileLayers(boolean reload) throws GeoWebCacheException {
        
    	List<TileLayer> layers = null;

        WebMapServer wms = getWMS();
        if (wms == null) {
            throw new ConfigurationException("Unable to connect to " + this.url);
        }

        String wmsUrl = getWMSUrl(wms);
        
        //log.info("Using " + wmsUrl + " to generate URLs for WMS requests");

        layers = getLayers(wms, wmsUrl);
        if (layers == null || layers.size() < 1) {
            
        	//log.error("Unable to find any layers based on " + url);
        } else {
            
        	//log.info("Loaded " + layers.size() + " layers from " + url);
        }

        return layers;
    }

    /**
     * Finds URL to WMS service and attempts to slice away the service
     * parameter, since we will add that anyway.
     * 
     * @param wms
     * @return
     */
    private String getWMSUrl(WebMapServer wms) {
        // // http://sigma.openplans.org:8080/geoserver/wms?SERVICE=WMS&
        String wmsUrl = wms.getCapabilities().getRequest().getGetCapabilities().getGet().toString();
        int queryStart = wmsUrl.lastIndexOf("?");
        String preQuery = wmsUrl.substring(queryStart);
        if (preQuery.equalsIgnoreCase("?service=wms&")) {
            wmsUrl = wmsUrl.substring(0, wmsUrl.lastIndexOf("?"));
        }
        return wmsUrl;
    }

    private List<TileLayer> getLayers(WebMapServer wms, String wmsUrl)
            throws GeoWebCacheException {
        List<TileLayer> layers = new LinkedList<TileLayer>();
        
        WMSCapabilities capabilities = wms.getCapabilities();
        if (capabilities == null) {
            throw new ConfigurationException("Unable to get capabitilies from " + wmsUrl);
        }

        List<Layer> layerList = capabilities.getLayerList();
        Iterator<Layer> layerIter = layerList.iterator();
        
        while (layerIter.hasNext()) {
            Layer layer = layerIter.next();
            String name = layer.getName();
            String stylesStr = "";
            
            if (name != null) {
                List<StyleImpl> styles = layer.getStyles();
                
                StringBuffer buf = new StringBuffer();
                if(styles != null) {
                    Iterator<StyleImpl> iter = styles.iterator();
                    boolean hasOne = false;
                    while(iter.hasNext()) {
                        if(hasOne) {
                            buf.append(",");
                        }
                        buf.append(iter.next().getName());
                        hasOne = true;
                    }
                    stylesStr = buf.toString();
                }
                
                double minX = layer.getLatLonBoundingBox().getMinX();
                double minY = layer.getLatLonBoundingBox().getMinY();
                double maxX = layer.getLatLonBoundingBox().getMaxX();
                double maxY = layer.getLatLonBoundingBox().getMaxY();

                BBOX bounds4326 = new BBOX(minX,minY,maxX,maxY);
                
                //log.info("Found layer: " + layer.getName()+ " with LatLon bbox " + bounds4326.toString());
                
                BBOX bounds900913 = new BBOX(
                        longToSphericalMercatorX(minX),
                        latToSphericalMercatorY(minY),
                        longToSphericalMercatorX(maxX),
                        latToSphericalMercatorY(maxY));
               
                String[] wmsUrls = {wmsUrl};
                
                WMSLayer wmsLayer = null;
                try {
                    wmsLayer = getLayer(name, wmsUrls, bounds4326, 
                            bounds900913, stylesStr);
                } catch (GeoWebCacheException gwc) {
                    //log.error("Error creating " + layer.getName() + ": " + gwc.getMessage());
                }

                if (wmsLayer != null) {
                    // Finalize with some defaults
                    wmsLayer.isCacheBypassAllowed(allowCacheBypass);
                    wmsLayer.setBackendTimeout(120);
                    layers.add(wmsLayer);
                }
            }
        }

        return layers;
    }

    private WMSLayer getLayer(String name, String[] wmsurl, 
            BBOX bounds4326, BBOX bounds900913, String stylesStr)
            throws GeoWebCacheException {
        
        Hashtable<SRS,Grid> grids = new Hashtable<SRS,Grid>(2);
        grids.put(SRS.getEPSG4326(), new Grid(SRS.getEPSG4326(), bounds4326, 
                BBOX.WORLD4326, GridCalculator.get4326Resolutions()));
        grids.put(SRS.getEPSG900913(), new Grid(SRS.getEPSG900913(), bounds900913,
                BBOX.WORLD900913, GridCalculator.get900913Resolutions()));
        
        List<String> mimeFormats = null;
        if(this.mimeTypes != null) {
            String[] mimeFormatArray = this.mimeTypes.split(",");
            mimeFormats = new ArrayList<String>(mimeFormatArray.length);
            
            // This is stupid... but oh well, we're only doing it once
            for(int i=0;i<mimeFormatArray.length;i++) {
                mimeFormats.add(mimeFormatArray[i]);
            }
        } else {
            mimeFormats = new ArrayList<String>(3);
            mimeFormats.add("image/png");
            mimeFormats.add("image/png8");
            mimeFormats.add("image/jpeg");  
        }
        
        String[] metaStrings = this.metaTiling.split("x");
        
        int[] metaWidthHeight = { Integer.parseInt(metaStrings[0]), Integer.parseInt(metaStrings[1])};
        
        // TODO We're dropping the styles now...
        return new WMSLayer(name, this.cacheFactory,
                wmsurl, stylesStr, name, mimeFormats, grids, 
                metaWidthHeight, this.vendorParameters);
    }

    private WebMapServer getWMS() {
        try {
            return new WebMapServer(new URL(url));
        } catch (IOException ioe) {
            
        	//log.error(url + " -> " + ioe.getMessage());
        } catch (ServiceException se) {
            
        	//log.error(se.getMessage());
        }
        return null;
    }
    
    private double longToSphericalMercatorX(double x) {
        return (x/180.0)*20037508.34;
    }
    
    private double latToSphericalMercatorY(double y) {        
        if(y > 85.05112) {
            y = 85.05112;
        }
        
        if(y < -85.05112) {
            y = -85.05112;
        }
        
        y = (Math.PI/180.0)*y;
        double tmp = Math.PI/4.0 + y/2.0; 
        return 20037508.34 * Math.log(Math.tan(tmp)) / Math.PI;
    }
    
}
