package com.sampas.socbs.core.network.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class SmpZipUtil {
 
  public static void zip(String zipFilename, String[] filenames) 
    throws IOException {
    zip(zipFilename, filenames, filenames);  
  }
  
  public static void zip(
    String zipFilename, String[] filenames, String[] archFilenames
  ) throws IOException {
    
    ZipOutputStream zout = new ZipOutputStream(
      new BufferedOutputStream(
        new FileOutputStream(zipFilename)    
      )
    );
    
    byte[] data = new byte[512];
    int bc;
    for (int i = 0; i < filenames.length; i++) {
      InputStream fin = new BufferedInputStream(
        new FileInputStream(filenames[i])    
      );
      
      ZipEntry entry = new ZipEntry(SmpStringUtil.stripPath(archFilenames[i]));
      zout.putNextEntry(entry);

      while ((bc = fin.read(data, 0, 512)) != -1) {
        zout.write(data, 0, bc);
      }
      zout.flush();
    }
    
    zout.close();  
  }
  
  
  public static void unzip(
    String zipFilename, Collection filenames, String outdir
  ) throws IOException {
    unzip(
      zipFilename,
      (String[])filenames.toArray(new String[filenames.size()]), 
      outdir
    );
  }
  
  public static void unzip(
    String zipFilename, String[] filenames, String outdir
  ) throws IOException  {
    
    ZipFile zipFile = new ZipFile(zipFilename);
    Enumeration entries = zipFile.entries();
    
L1: while(entries.hasMoreElements()) {
      ZipEntry entry = (ZipEntry)entries.nextElement();
      
      for (int i = 0; i < filenames.length; i++) {
        if (entry.getName().equals(filenames[i])) {
	        byte[] buffer = new byte[1024];
	        int len;
	        
	        InputStream zipin = zipFile.getInputStream(entry);
	        BufferedOutputStream fileout = new BufferedOutputStream(
	          new FileOutputStream(outdir + "\\"+filenames[i])
	        );
	        
	        while((len = zipin.read(buffer)) >= 0)
	          fileout.write(buffer, 0, len); 
	      
	        zipin.close();
	        fileout.flush();
	        fileout.close();
	        
	        continue L1;
	      }
      }
	  }   
  }
  
  public static void unzip(String zipFilename, String outdir) throws IOException {
    ZipFile zipFile = new ZipFile(zipFilename);
    Enumeration entries = zipFile.entries();
    
    while(entries.hasMoreElements()) {
      ZipEntry entry = (ZipEntry)entries.nextElement();
      byte[] buffer = new byte[1024];
	    int len;
	        
      InputStream zipin = zipFile.getInputStream(entry);
      BufferedOutputStream fileout = new BufferedOutputStream(
        new FileOutputStream(outdir + "\\" +entry.getName())
      );
      
      while((len = zipin.read(buffer)) >= 0)
        fileout.write(buffer, 0, len); 
    
      zipin.close();
      fileout.flush();
      fileout.close();
    }
  }
  
}