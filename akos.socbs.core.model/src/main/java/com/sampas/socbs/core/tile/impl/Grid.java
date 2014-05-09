package com.sampas.socbs.core.tile.impl;

import com.sampas.socbs.core.tile.toolbox.BBOX;
import com.sampas.socbs.core.tile.toolbox.GeoWebCacheException;
import com.sampas.socbs.core.tile.toolbox.SRS;

/**
 * Grid Class - Each TileLayer keeps a list of Grid Objects
 */

public class Grid {
    
	//private static Log log = LogFactory.getLog(org.geowebcache.layer.Grid.class);

    private SRS srs = null;
    
    protected BBOX dataBounds = null;
    
    protected BBOX gridBounds = null;
    
    protected int zoomStart;
    
    protected int zoomStop;
    
    protected double[] resolutions = null;
    
    private volatile transient GridCalculator gridCalculator;
    
    public Grid(SRS srs, BBOX bounds, BBOX gridBounds, double[] resolutions) {
        this.srs = srs;
        this.dataBounds = bounds;
        this.gridBounds = gridBounds;
        this.resolutions = resolutions;
    }
    
    /**
     * method will set the bounds of the layer for this grid from a BBOX 
     * @param bounds - BBOX with bounds
     */
    public void setBounds(BBOX bounds) {
        this.dataBounds = bounds;
    }
    /**
     * method will set the bounds of the layer for this grid from a String
     * @param bounds - String containing bounds
     */
    public void setBounds(String bounds) {
        this.dataBounds = new BBOX(bounds);
    }
    /**
     * method will set the grid bounds (world) of the layer for this grid from a BBOX 
     * @param dataBounds - BBOX with bounds
     */
    public void setGridBounds(BBOX gridbounds) {
        this.gridBounds = gridbounds;
    }
    /**
     * method will set the grid bounds (world) of the layer for this grid from a String 
     * @param dataBounds - String containing bounds
     */
    public void setGridBounds(String gridbounds) {

        this.gridBounds = new BBOX(gridbounds);
    }
    /**
     * method set the projection supported by the layer for this grid
     * @param projection - SRS
     */
    public void setSRS(SRS srs) {
        this.srs = srs;
    }
    /**
     * method returns the projection supported by the layer for this grid
     * @return
     */
    public SRS getSRS() {
        return this.srs;
    }
    /**
     * method returns the bounds of the layer for this grid
     * @return
     */
    public BBOX getBounds() {
        return this.dataBounds;
    }
    /**
     * method returns the grid bounds of the layer for this grid
     * @return
     */
    public BBOX getGridBounds() {
        return this.gridBounds;
    }
    
    public int getZoomStart() {
        return this.zoomStart;
    }
    
    public int getZoomStop() {
        return this.zoomStop;
    }
    
    public void setResolutions(double[] resolutions) {
        this.resolutions = resolutions;
    }
    
    public double[] getResolutions() throws GeoWebCacheException {
        return getGridCalculator().getResolutions();
    }
    
    /** 
     * Use double locking to get the calculator to avoid performance hit.
     * 
     * @return
     * @throws GeoWebCacheException
     */
    public GridCalculator getGridCalculator() throws GeoWebCacheException {
        GridCalculator ret = gridCalculator;
        if (ret == null) {
            synchronized (this) {
                ret = gridCalculator;
                if (gridCalculator == null) {
                    gridCalculator = ret = initGridCalculator();
                }
            }
        }
        return ret;
    }

    private GridCalculator initGridCalculator() throws GeoWebCacheException {
        if(zoomStart < 0 || zoomStop < zoomStart || zoomStop == 0) {
            
        	//log.debug("Missing values, setting zoomStart,zoomStop to 0,30");
            zoomStart = 0;
            zoomStop = 30;
        }
        
        GridCalculator gridCalc = new GridCalculator(this);
        return gridCalc;
    }
}
