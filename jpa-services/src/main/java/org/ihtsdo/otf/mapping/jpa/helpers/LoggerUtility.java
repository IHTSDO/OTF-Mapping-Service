package org.ihtsdo.otf.mapping.jpa.helpers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class LoggerUtility {

	private static Map<String, Boolean> initializationFlags = new HashMap<String, Boolean>();
	private static String fileName;
	private static String loggerName;

	private static void intializeLogger(String loggerName) {

		// remove the previous file of the same name
		removeFile();

		final PatternLayout layOut = new PatternLayout();
		layOut.setConversionPattern("%d{yyyy-MM-dd_HH:mm:ss.SSS} %p - %m%n");

		final FileAppender appender = new FileAppender();
		appender.setName(loggerName);
		appender.setAppend(true);
		// appender.setMaxFileSize("1MB");
		// appender.setMaxBackupIndex(1);
		appender.setFile(fileName);
		appender.setLayout(layOut);
		appender.activateOptions();

		Logger.getLogger(loggerName).addAppender(appender);
		Logger.getLogger(loggerName).setLevel(Level.INFO);

	}

	/**
	 * Return the logger for the given name.
	 * 
	 * @param loggerName
	 * @return Loggger {@see org.apache.log4j.Logger}
	 */
	public static Logger getLogger(String loggerName) {

		LoggerUtility.loggerName = loggerName;

		if (initializationFlags != null) {
			if (initializationFlags.get(loggerName + "|" + fileName) == null
					|| initializationFlags.get(loggerName + "|" + fileName) == false) {
				intializeLogger(loggerName + "|" + fileName);
				initializationFlags.put(loggerName + "|" + fileName, true);
				return Logger.getLogger(loggerName + "|" + fileName);
			} else {
				return Logger.getLogger(loggerName + "|" + fileName);
			}
		} else {
			return Logger.getLogger(loggerName + "|" + fileName);
		}
	}

	/**
	 * Sets the configuration for the logger
	 * 
	 * @param loggerName Name of logger to be used for logging
	 * @param fileName Full path including file name
	 */
	public static void setConfiguration(String loggerName, String fileName) {
		LoggerUtility.fileName = fileName;
		LoggerUtility.loggerName = loggerName;
	}

	/**
	 * Remove a logger when no longer needed.
	 * 
	 * @param loggerName Name of the logger to be removed.
	 */
	public static void removeLogger(String loggerName) {
		if (initializationFlags.containsKey(loggerName)) {
			initializationFlags.remove(loggerName);
		}
		Logger.getLogger(loggerName).removeAppender(loggerName);

	}

	private static void removeFile() {

		try {
			File file = new File(LoggerUtility.fileName);
			file.delete();
		} catch (Exception e) {
			// noop
		}
	}

}
