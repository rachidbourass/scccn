package org.ascn.utils;

//Script will pull the file Symphony_OIM_Outbound_07222021120102_HRI247_Full.txt (the full file).

/*Java class HRI247FullFeed must be converted to .jar format
* Windows schedule will run HRFeed.bat which will run "java -jar -Xms1024m -Xmx1024m -Djava.awt.headless=true D:\Ministry\HRI247FullFeed.jar"
*When run it will pull a file like "Symphony_OIM_Outbound_12042023120056_HRI247Full.txt" from the MoveIT server.
*The file will be converted to HRI247FullFeed.csv and placed in ANDC1WAPP0147\d$\Ministry\AMITA\INBOUND 
*/

import java.io.File;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class HR247FullFeed {

	  public static void main(String[] args) {
		Logger log = Logger.getLogger("MyLog");  
		FileHandler fh;
	    FTPClient client = new FTPClient();
	    String destinationPath = "\\\\andc1wapp0144/d$/Ministry/AMITA/INBOUND/ARCHIVED/";
	    //String destinationPath = "D:/Ministry/AMITA/INBOUND/ARCHIVED/";
	    //String workingPath = "D:/Ministry/AMITA/INBOUND/Full Files";
	    //String workingPath = "D:/Ministry/AMITA/INBOUND";
	    String workingPath = "\\\\andc1wapp0144/d$/Ministry/AMITA/INBOUND";
	    InputStream iStream = null;
	    String pattern = "MMddyyyy";
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
	    try {
	    	fh = new FileHandler("\\\\andc1wapp0144/d$/Ministry/AMITA/INBOUND/ARCHIVED/HR247FullPull"+simpleDateFormat.format(new Date())+".log");  //My own insertion 10-12-23

	    	 log.addHandler(fh);
	         SimpleFormatter formatter = new SimpleFormatter();  
	         fh.setFormatter(formatter);
	         client.connect("10.240.101.24"); //New Test
	         client.login("ta_ahnat_idm", "55$LjRC5"); //New Test
	         boolean success = client.login("ta_ahnat_idm", "55$LjRC5"); //New Test
	     
	     if(!success)
	     {
	 	    log.info("Connection Failed to FTP Server.");
		    File file = new File(String.valueOf(workingPath) + "/HRI247FullFeed.csv");
		    file.delete();
	     }
	     else {
	      if (client.isConnected()) {
	        client.enterLocalPassiveMode();
	        log.info("Connection Success to FTP Server.");
		 	  client.changeWorkingDirectory("/Ascension/AHNAT/IDM/fr_eng");         //New Test
   
	        String lookUpFileName = "Symphony_OIM_Outbound_" + simpleDateFormat.format(new Date());
     
	        
	        FTPFile[] files = client.listFiles();
	        byte b;
	        int i;
	        FTPFile[] arrayOfFTPFile1;
	        for (i = (arrayOfFTPFile1 = files).length, b = 0; b < i; ) {
	          FTPFile file = arrayOfFTPFile1[b];
	          //System.out.println(destinationPath);
	          if (StringUtils.containsIgnoreCase(file.getName(), lookUpFileName) && (StringUtils.containsIgnoreCase(file.getName(), "HRI247_Full.txt"))) {
	           System.out.println(file.getName());
//	            System.out.println(file.getName());
		            System.out.println(lookUpFileName);
	            //FileUtils.copyToFile(iStream, new File(String.valueOf(destinationPath) + file.getName()));
	            FileUtils.copyFile(new File(String.valueOf(destinationPath) + file.getName()), new File(String.valueOf(workingPath) + "/HRI247FullFeed.csv"));
	          
	          } 
	          b++;
	        } 
	       
	      } 
	      
	      
	      client.logout();
	     }
	    } catch (Throwable e) {
	      e.printStackTrace();
	    } finally {
	      try {
	        client.disconnect();
	      } catch (IOException e) {
	        e.printStackTrace();
	      } 
	    } 
	  }
}