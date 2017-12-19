/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.algo;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.services.helpers.ProgressEvent;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

/**
 * Abstract support for loader algorithms.
 */
public abstract class AbstractAlgorithm extends WorkflowServiceJpa implements Algorithm
{

	/** Listeners. */
	private List<ProgressListener> listeners = new ArrayList<>();

	/**
	 * Instantiates an empty {@link AbstractAlgorithm}.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public AbstractAlgorithm() throws Exception {
		// n/a
	}


	/**
	 * Log info to console and the database.
	 *
	 * @param message the message
	 * @throws Exception the exception
	 */
	public void logInfo(String message) throws Exception {
				
		addLogEntry(getLastModifiedBy(), 0l, 0l, null, null,
				"INFO: " + message);

		Logger.getLogger(getClass()).info(message);
	}

	/**
	 * Log warning to console and the database.
	 *
	 * @param message the message
	 * @throws Exception the exception
	 */
	public void logWarn(String message) throws Exception {

		addLogEntry(getLastModifiedBy(), 0l, 0l,
				null, null, "WARNING: " + message);

		//fireWarningEvent(message);
		Logger.getLogger(getClass()).warn(message);
	}

	/**
	 * Log error to console and the database.
	 *
	 * @param message the message
	 * @throws Exception the exception
	 */
	public void logError(String message) throws Exception {

		addLogEntry(getLastModifiedBy(), 0l, 0l,
				null, null, "ERROR: " + message);
		
		Logger.getLogger(getClass()).error(message);
	}

	/**
	 * Returns the total elapsed time str.
	 *
	 * @param time the time
	 * @return the total elapsed time str
	 */
	@SuppressWarnings({ "boxing" })
	protected static String getTotalElapsedTimeStr(long time) {
		Long resultnum = (System.nanoTime() - time) / 1000000000;
		String result = resultnum.toString() + "s";
		resultnum = resultnum / 60;
		result = result + " / " + resultnum.toString() + "m";
		resultnum = resultnum / 60;
		result = result + " / " + resultnum.toString() + "h";
		return result;
	}

	/**
	 * Fires a {@link ProgressEvent}.
	 *
	 * @param pct percent done
	 * @param note progress note
	 * @throws Exception the exception
	 */
	public void fireProgressEvent(int pct, String note) throws Exception {
		final ProgressEvent pe = new ProgressEvent(this, pct, pct, note);
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).updateProgress(pe);
		}
		// don't write this to a log entry
		Logger.getLogger(getClass()).info("    " + pct + "% " + note);
	}

	/**
	 * Fire adusted progress event.
	 *
	 * @param pct the pct
	 * @param step the step
	 * @param steps the steps
	 * @param note the note
	 * @throws Exception the exception
	 */
	public void fireAdjustedProgressEvent(int pct, int step, int steps,
			String note) throws Exception {
		final ProgressEvent pe = new ProgressEvent(this, pct, pct, note);
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).updateProgress(pe);
		}
		logInfo("    "
				+ ((int) (((pct * 1.0) / steps) + ((step - 1) * 100.0 / steps)))
				+ "% " + note);
	}


	/* see superclass */
	@Override
	public void addProgressListener(ProgressListener l) {
		listeners.add(l);
	}

	/* see superclass */
	@Override
	public void removeProgressListener(ProgressListener l) {
		listeners.remove(l);
	}

}
