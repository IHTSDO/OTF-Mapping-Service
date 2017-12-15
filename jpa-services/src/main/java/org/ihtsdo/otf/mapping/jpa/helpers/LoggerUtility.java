package org.ihtsdo.otf.mapping.jpa.helpers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

public class LoggerUtility {
	
	private static Logger log =  Logger.getLogger(LoggerUtility.class);
    private static Map<String, Boolean> initializationFlags = new HashMap<String, Boolean>();
    private static String fileName;
    private static String loggerName;

    private static void intializeLogger(String name){
    	
    	//may want to set these in the configuration file    	
    	
        log.setLevel(Level.INFO);

//        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
//        final Date date = new Date();

        final PatternLayout layOut = new PatternLayout();
        layOut.setConversionPattern("%d{yyyy-MM-dd_HH:mm:ss.SSS} %p - %m%n");
        
        final RollingFileAppender appender = new RollingFileAppender();
        appender.setName(name);
        appender.setAppend(true);
        appender.setMaxFileSize("1MB");
        appender.setMaxBackupIndex(1);
        appender.setFile(fileName);
        appender.setLayout(layOut);
        appender.activateOptions();

        log.addAppender(appender);
    }

    public static Logger getLogger(String loggerName){
    	
    	LoggerUtility.loggerName = loggerName;
    	
    	if(initializationFlags != null) 
    	{
    		if (initializationFlags.get(loggerName) == null || initializationFlags.get(loggerName) == false)
    		{
    			intializeLogger(loggerName);
    			initializationFlags.put(loggerName, true);
    			return LoggerUtility.log;
    		}
    		else {
    			return LoggerUtility.log;
    		}
        }
    	else
		{
			return LoggerUtility.log;
		}
    }
    
    public static void setConfiguration(String loggerName, String fileName) {
    	LoggerUtility.fileName = fileName;
    	LoggerUtility.loggerName = loggerName;
    }

}
