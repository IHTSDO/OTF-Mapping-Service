package org.ihtsdo.otf.mapping.services.helpers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.TreePosition;

/**
 * Generically represents a handler for performing a release.
 */
public interface ReleaseHandler {
	
	/**
	 * Process release.
	 *
	 * @param mapProject the map project
	 * @param machineReadableOutputFileName the machine readable output file name
	 * @param humanReadableOutputFileName the human readable output file name
	 * @param mapRecordsToPublish the map records to publish
	 * @param effectiveTime the effective time
	 * @param moduleId the module id
	 * @throws Exception the exception
	 */
	public void processRelease(MapProject mapProject,
			String machineReadableOutputFileName,
			String humanReadableOutputFileName,
			Set<MapRecord> mapRecordsToPublish, String effectiveTime,
			String moduleId) throws Exception;

	/**
	 * Function to construct propagated rule for an entry.
	 * 
	 * @param mapEntry
	 *            the map entry
	 * @return the map entry
	 */
	public MapEntry setPropagatedRuleForMapEntry(MapEntry mapEntry);

	/**
	 * Gets the human readable map advice.
	 * 
	 * @param mapEntry
	 *            the map entry
	 * @return the human readable map advice
	 */
	public String getHumanReadableMapAdvice(MapEntry mapEntry);

	/**
	 * Takes a tree position graph and converts it to a sorted list of tree
	 * positions where order is based on depth in tree.
	 *
	 * @param tp            the tp
	 * @return the sorted tree position descendant list
	 * @throws Exception             the exception
	 */
	public List<TreePosition> getSortedTreePositionDescendantList(
			TreePosition tp) throws Exception;

	/**
	 * Given a map record and map entry, return the next assignable map priority
	 * for this map entry.
	 * 
	 * @param mapRecord
	 *            the map record
	 * @param mapEntry
	 *            the map entry
	 * @return the next map priority
	 */
	public int getNextMapPriority(MapRecord mapRecord, MapEntry mapEntry);

	/*
	 * public String getReleaseUuid(MapEntry mapEntry, MapRecord mapRecord,
	 * MapProject mapProject) {
	 * 
	 * long hashCode = 17;
	 * 
	 * hashCode = hashCode * 31 + mapProject.getRefSetId().hashCode(); hashCode
	 * = hashCode * 31 + mapRecord.getConceptId().hashCode(); hashCode =
	 * hashCode * 31 + mapEntry.getMapGroup(); hashCode = hashCode * 31 +
	 * mapEntry.getRule().hashCode(); hashCode = hashCode * 31 +
	 * mapEntry.getTargetId().hashCode();
	 * 
	 * return ""; }
	 */

	/**
	 * Returns the raw bytes.
	 *
	 * @param uid the uid
	 * @return the raw bytes
	 */
	public byte[] getRawBytes(UUID uid);

	/**
	 * Gets the release uuid.
	 * 
	 * @param name
	 *            the name
	 * @return the release uuid
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws UnsupportedEncodingException
	 *             the unsupported encoding exception
	 */
	public UUID getReleaseUuid(String name)
			throws NoSuchAlgorithmException, UnsupportedEncodingException;
	
	/**
	 * Write release entry.
	 *
	 * @param machineReadableWriter the machine readable writer
	 * @param humanReadableWriter the human readable writer
	 * @param mapEntry            the map entry
	 * @param mapRecord            the map record
	 * @param mapProject            the map project
	 * @param effectiveTime            the effective time
	 * @param moduleId            the module id
	 * @throws IOException             Signals that an I/O exception has occurred.
	 * @throws NoSuchAlgorithmException             the no such algorithm exception
	 */
	public void writeReleaseEntry(BufferedWriter machineReadableWriter, BufferedWriter humanReadableWriter, MapEntry mapEntry,
			MapRecord mapRecord, MapProject mapProject, String effectiveTime,
			String moduleId) throws IOException, NoSuchAlgorithmException;

	
}
