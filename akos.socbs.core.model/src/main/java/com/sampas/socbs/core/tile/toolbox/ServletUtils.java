package com.sampas.socbs.core.tile.toolbox;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServletUtils {
    
	private static Log log = LogFactory.getLog(com.sampas.socbs.core.tile.toolbox.ServletUtils.class);
    
    // Calendar objects are unfortunately expensive and not thread safe :(
    static private Calendar calendar = new GregorianCalendar();
    static private TimeZone timeZone = TimeZone.getTimeZone("GMT");
    static private SimpleDateFormat format = null;
    static private long localOffset = TimeZone.getDefault().getRawOffset();
    
    /**
     * Case insensitive lookup
     * 
     * @param map
     * @param key
     * @return all matchings string
     */
    public static String[] stringsFromMap(Map<String, String[]> map, String key) {
        String[] strArray = (String[]) map.get(key);
        
        if (strArray != null) {
            return strArray;
        } else {            
            // In case there is a case mismatch
            Iterator<Entry<String,String[]>> iter = map.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String,String[]> entry = iter.next();
                if(entry.getKey().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Case insensitive lookup
     * 
     * @param map
     * @param key
     * @return
     */
    public static String stringFromMap(Map<String,String[]> map, String key) {
        String[] strArray = stringsFromMap(map, key);
        if(strArray != null) {
            return strArray[0];
        }
        return null;
      
    }
    
    /**
     * Case insensitive lookup for a couple of strings,
     * drops everything else
     * 
     * @param map
     * @param keys
     * @return
     */
    public static String[][] selectedStringArraysFromMap(Map<String,String[]> map, String[] keys) {
        String[][] retAr = new String[keys.length][];
        
        Iterator<Entry<String,String[]>> iter = map.entrySet().iterator();
        while(iter.hasNext()) {
            Entry<String,String[]> entry = iter.next();
            String key = entry.getKey();
            
            for(int i=0;i<keys.length;i++) {
                if(key.equalsIgnoreCase(keys[i])) {
                    retAr[i] = entry.getValue();
                    continue;
                }
            }
        }
        
        return retAr;
    }
    
    
    /**
     * Case insensitive lookup for a couple of strings,
     * drops everything else
     * 
     * @param map
     * @param keys
     * @return
     */
    public static String[] selectedStringsFromMap(Map<String,String[]> map, String[] keys) {
        String[] retAr = new String[keys.length];
        
        Iterator<Entry<String,String[]>> iter = map.entrySet().iterator();
        while(iter.hasNext()) {
            Entry<String,String[]> entry = iter.next();
            String key = entry.getKey();
            
            for(int i=0;i<keys.length;i++) {
                if(key.equalsIgnoreCase(keys[i])) {
                    retAr[i] = entry.getValue()[0];
                    break;
                }
            }
        }
        
        return retAr;
    }
    
    /**
     * Extracts the cache control header
     * 
     * @param cacheControlHeader
     * @return Long representing expiration time in seconds
     */
//    public static Long extractHeaderMaxAge(URLConnection backendCon) {
//        
//        String cacheControlHeader = backendCon.getHeaderField("Cache-Control");
//        
//        if (cacheControlHeader == null) {
//            return null;
//        }
//
//        String expression = "max-age=([0-9]*)[ ,]";
//        Pattern p = Pattern.compile(expression);
//        Matcher m = p.matcher(cacheControlHeader.toLowerCase());
//
//        if (m.find()) {
//            return Long.valueOf(m.group(1));
//        } else {
//            return null;
//        }
//    }
    
    /**
     * Reads an inputstream and stores all the information in a buffer.
     * 
     * @param is the inputstream
     * @param bufferHint hint for the total buffer, -1 = 10240
     * @param tmpBufferSize how many bytes to read at a time, -1 = 1024
     * 
     * @return a compacted buffer with all the data
     * @throws IOException
     */
    public static byte[] readStream(InputStream is, int bufferHint, int tmpBufferSize) throws IOException {
        
        byte[] buffer = null;
        if(bufferHint > 0) {
            buffer = new byte[bufferHint];
        } else {
            buffer = new byte[10240];
        }
        
        byte[] tmpBuffer = null;
        if(tmpBufferSize > 0) {
            tmpBuffer = new byte[tmpBufferSize];
        } else {
            tmpBuffer = new byte[1024];
        }
        
        
        int totalCount = 0;
        for(int c = 0; c != -1; c = is.read(tmpBuffer)) {
                // Expand buffer if needed
                if(totalCount + c >= buffer.length) {
                        int newLength = buffer.length * 2;
                        if(newLength < totalCount)
                                newLength = totalCount;
                        
                        byte[] newBuffer = new byte[newLength];
                        System.arraycopy(buffer, 0, newBuffer, 0, totalCount);
                        buffer = newBuffer;
                }
                System.arraycopy(tmpBuffer, 0, buffer, totalCount, c);
                totalCount += c;                
        }
        is.close();
        
        // Compact buffer
        byte[] newBuffer = new byte[totalCount];
        System.arraycopy(buffer, 0, newBuffer, 0, totalCount);
        
        return newBuffer;
    }

    /**
     * Makes HTTP Expire header value
     * 
     * Has to be synchronized due to the shared Calendar objects
     * 
     * @param seconds
     * @return
     */
    public static String makeExpiresHeader(int seconds) {
        String ret;
        synchronized (calendar) {
            if (ServletUtils.format == null) {
                ServletUtils.format = new SimpleDateFormat(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                ServletUtils.format.setTimeZone(ServletUtils.timeZone);
            }

            calendar.setTimeInMillis(System.currentTimeMillis() + seconds * 1000);
            ret = format.format(calendar.getTime());
        }
        return ret;
    }
    
    /**
     * Returns the expiration time in milliseconds from now
     * 
     * @param expiresHeader
     * @return
     */
    public static long parseExpiresHeader(String expiresHeader) {
        if(expiresHeader == null) {
            return -1;
        }
        
        long ret;
        
        synchronized (calendar) {
            if (ServletUtils.format == null) {
                ServletUtils.format = new SimpleDateFormat(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                ServletUtils.format.setTimeZone(ServletUtils.timeZone);
                
            }
            
            try { 
                format.parse(expiresHeader);
            } catch (ParseException pe) {
                log.debug("Cannot parse " + expiresHeader + ", " + pe.getMessage());
                return -1;
            }
            
            ret = calendar.getTimeInMillis() - System.currentTimeMillis() - localOffset;
        }
        return ret;
    }
}
