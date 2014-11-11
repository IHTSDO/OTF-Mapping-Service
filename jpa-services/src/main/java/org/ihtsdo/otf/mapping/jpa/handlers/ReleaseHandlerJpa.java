package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;

public class ReleaseHandlerJpa implements ReleaseHandler {

	// class-global services
	private MappingService mappingService;
	private ContentService contentService;

	public ReleaseHandlerJpa() {
	}

	@Override
	public void processRelease(MapProject mapProject,
			String machineReadableOutputFileName,
			String humanReadableOutputFileName,
			Set<MapRecord> mapRecordsToPublish, String effectiveTime,
			String moduleId) throws Exception {

		Logger.getLogger(MappingServiceJpa.class).info(
				"Processing publication release for project "
						+ mapProject.getName());

		Logger.getLogger(MappingServiceJpa.class).info(
				" " + mapRecordsToPublish.size()
						+ " records selected for publication");

		// file writers
		BufferedWriter humanReadableWriter = null;
		BufferedWriter machineReadableWriter = null;

		// check file directory exists and open writers
		if (machineReadableOutputFileName != null) {
			Logger.getLogger(MappingServiceJpa.class).info(
					"  Creating machine-readable output file: "
							+ machineReadableOutputFileName);
			machineReadableWriter = new BufferedWriter(new FileWriter(
					machineReadableOutputFileName));
		}

		if (humanReadableOutputFileName != null) {
			Logger.getLogger(MappingServiceJpa.class).info(
					"  Creating human-readable output file: "
							+ humanReadableOutputFileName);
			humanReadableWriter = new BufferedWriter(new FileWriter(
					humanReadableOutputFileName));
		}

		// Write header based on relation style
		if (mapProject.getMapRefsetPattern().equals("ExtendedMap")) {
			if (machineReadableWriter != null) {
				machineReadableWriter
						.write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId\r\n");
				machineReadableWriter.flush();
			}
			if (humanReadableWriter != null) {
				humanReadableWriter
						.write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId\treferencedComponentName\tmapTargetName\tmapCategoryName\r\n");
				humanReadableWriter.flush();
			}

		} else if (mapProject.getMapRefsetPattern().equals("ComplexMap")) {
			if (machineReadableWriter != null) {
				machineReadableWriter
						.write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\r\n");
				machineReadableWriter.flush();
			}
			if (humanReadableWriter != null) {
				humanReadableWriter
						.write("id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\treferencedComponentName\tmapTargetName\tmapCategoryName\r\n");
				humanReadableWriter.flush();
			}
		} else {
			machineReadableWriter.close();
			humanReadableWriter.close();
			throw new Exception("Unsupported map refset pattern - "
					+ mapProject.getMapRefsetPattern());
		}

		// instantiate the required services
		contentService = new ContentServiceJpa();
		mappingService = new MappingServiceJpa();

		// Create a map by concept id for quick retrieval of descendants
		Logger.getLogger(MappingServiceJpa.class).info(
				"  Creating terminology id map");
		Map<String, MapRecord> mapRecordMap = new HashMap<>();

		for (MapRecord mr : mapRecordsToPublish) {
			mapRecordMap.put(mr.getConceptId(), mr);
		}

		// Create a concept set to represent processed concepts
		// Multiple tree positions may point to the same concept, don't want to
		// process twice
		Set<String> conceptsProcessed = new HashSet<>();

		// set of concept ids for records that could not be retrieved
		Map<String, String> conceptErrors = new HashMap<>();

		Logger.getLogger(MappingServiceJpa.class).info(
				"  Instantiating algorithm handler "
						+ mapProject.getProjectSpecificAlgorithmHandlerClass());

		// instantiate the project specific handler
		ProjectSpecificAlgorithmHandler algorithmHandler = mappingService
				.getProjectSpecificAlgorithmHandler(mapProject);

		int nRecords = 0;
		int nRecordsPropagated = 0;

		// create a list from the set
		Logger.getLogger(MappingServiceJpa.class).info("  Sorting records");

		List<MapRecord> mapRecordsToPublishList = new ArrayList<>(
				mapRecordsToPublish);
		Collections.sort(mapRecordsToPublishList, new Comparator<MapRecord>() {

			@Override
			public int compare(MapRecord o1, MapRecord o2) {
				Long conceptId1 = Long.parseLong(o1.getConceptId());
				Long conceptId2 = Long.parseLong(o2.getConceptId());

				return conceptId1.compareTo(conceptId2);

			}
		});

		// perform the release
		Logger.getLogger(MappingServiceJpa.class).info(
				"  Processing release...");

		// cycle over the map records marked for publishing
		for (MapRecord mapRecord : mapRecordsToPublishList) {

			Logger.getLogger(MappingServiceJpa.class).info(
					"   Processing map record " + mapRecord.getId() + ", "
							+ mapRecord.getConceptId() + ", "
							+ mapRecord.getConceptName());

			// this concept has already been analyzed, skip
			// This accounts for possibility of multiple tree-position routes to
			// the same concept
			if (conceptsProcessed.contains(mapRecord.getConceptId())) {
				Logger.getLogger(MappingServiceJpa.class).info(
						"    Concept has already been processed.");
				continue;
			} else {
				conceptsProcessed.add(mapRecord.getConceptId());
			}

			// instantiate map of entries by group
			// this is the object containing entries to write
			Map<Integer, List<MapEntry>> entriesByGroup = new HashMap<>();

			// second, check whether this record should be up-propagated
			if (algorithmHandler
					.isUpPropagatedRecordForReleaseProcessing(mapRecord) == true) {

				Logger.getLogger(MappingServiceJpa.class).info(
						"    Record is up-propagated.");

				TreePosition treePosition;
				try {
					// get the first tree position (may be several for this
					// concept)
					treePosition = contentService
							.getTreePositions(mapRecord.getConceptId(),
									mapProject.getSourceTerminology(),
									mapProject.getSourceTerminologyVersion())
							.getIterable().iterator().next();

					// use the first tree position to retrieve a tree position
					// graph with populated descendants
					treePosition = contentService
							.getTreePositionWithDescendants(treePosition);
				} catch (NoSuchElementException e) {
					conceptErrors.put(mapRecord.getConceptId(),
							"Could not retrieve tree positions");
					continue;
				}

				// increment the propagated counter
				nRecordsPropagated++;

				// get a list of tree positions sorted by position in hiearchy
				// (deepest-first)
				// NOTE: This list will contain the top-level/root map record
				List<TreePosition> treePositionDescendantList = getSortedTreePositionDescendantList(treePosition);

				System.out.println("*** Descendant list has "
						+ treePositionDescendantList.size() + " elements");

				// construct a map of ancestor path + terminologyId to map
				// records, used to easily retrieve parent records for
				// descendants of
				// up-propagated records
				// key: A~B~C~D, value: map record for concept D
				Map<String, MapRecord> treePositionToMapRecordMap = new HashMap<>();

				// cycle over all descendants of this position
				// and add all required records to the map
				// for use later
				for (TreePosition tp : treePositionDescendantList) {

					System.out.println("Retrieving record for concept "
							+ tp.getTerminologyId());

					// retrieve map record from cache, or retrieve from database
					// and add to cache
					MapRecord mr = new MapRecordJpa();
					if (mapRecordMap.containsKey(tp.getTerminologyId())) {
						mr = mapRecordMap.get(tp.getTerminologyId());
					} else {

						// if not in cache yet, try to retrieve it
						try {
							MapRecordList mapRecordList = mappingService
									.getMapRecordsForProjectAndConcept(
											mapProject.getId(),
											tp.getTerminologyId());

							// check number of records retrieved for erroneous
							// states
							if (mapRecordList.getCount() == 0) {
								// if on excluded list, add to errors to output
								if (mapProject.getScopeExcludedConcepts()
										.contains(tp.getTerminologyId()))
									System.out
											.println("  Concept on excluded list for project");
								// if not found, add to errors to output
								else
									conceptErrors.put(tp.getTerminologyId(),
											"No record for concept.");
							} else if (mapRecordList.getCount() > 1) {
								conceptErrors.put(
										tp.getTerminologyId(),
										"Multiple records ("
												+ mapRecordList.getCount()
												+ ") found for concept");
							} else {
								MapRecord recordToAdd = mapRecordList
										.getMapRecords().iterator().next();

								// if ready for publication, add to map
								if (recordToAdd.getWorkflowStatus().equals(
										WorkflowStatus.READY_FOR_PUBLICATION)
										|| recordToAdd
												.getWorkflowStatus()
												.equals(WorkflowStatus.PUBLISHED))
									mapRecordMap.put(tp.getTerminologyId(),
											recordToAdd);
								else {
									conceptErrors
											.put(tp.getTerminologyId(),
													"Invalid workflow status "
															+ recordToAdd
																	.getWorkflowStatus()
															+ " on record");
								}
							}
							// catch no result for error outputting
							// does not interrupt the routine
						} catch (NoResultException e) {

						}
					}

					// add record to TreePosition->MapRecord map
					treePositionToMapRecordMap.put(tp.getAncestorPath() + "~"
							+ tp.getTerminologyId(), mr);
				}

				// cycle over the tree positions again and add entries
				// note that the tree positions are in reverse order of
				// hierarchy depth
				for (TreePosition tp : treePositionDescendantList) {

					// skip the root level record, these entries are added
					// below, after the up-propagated entries
					if (!tp.getTerminologyId().equals(mapRecord.getConceptId())) {

						// get the map record corresponding to this specific
						// ancestor path + concept Id
						MapRecord mr = treePositionToMapRecordMap.get(tp
								.getAncestorPath()
								+ "~"
								+ tp.getTerminologyId());

						Logger.getLogger(MappingServiceJpa.class).info(
								"     Adding entries from map record "
										+ mr.getId() + ", " + mr.getConceptId()
										+ ", " + mr.getConceptName());

						// get the parent map record for this tree position
						// used to check if entries are duplicated on parent
						MapRecord mrParent = treePositionToMapRecordMap.get(tp
								.getAncestorPath());

						// if no parent, continue, but log error
						if (mrParent == null) {
							Logger.getLogger(MappingServiceJpa.class).warn(
									"Could not retrieve parent map record!");
							mrParent = new MapRecordJpa(); // only here during
															// testing
							conceptErrors.put(tp.getTerminologyId(),
									"Could not retrieve parent record along ancestor path "
											+ tp.getAncestorPath());
						}

						// cycle over the entries
						for (MapEntry me : mr.getMapEntries()) {

							// get the current list of entries for this group
							List<MapEntry> existingEntries = entriesByGroup
									.get(me.getMapGroup());

							if (existingEntries == null)
								existingEntries = new ArrayList<>();

							// flag for whether this entry is a duplicate of an
							// existing or parent entry
							boolean isDuplicateEntry = false;

							// compare to the entries on the parent record (this
							// produces short-form)
							// NOTE: This uses unmodified rules,
							for (MapEntry parentEntry : mrParent
									.getMapEntries()) {

								if (parentEntry.getMapGroup() == me
										.getMapGroup()
										&& parentEntry.isEquivalent(me))
									isDuplicateEntry = true;
							}

							// if not a duplicate entry, add it to the map
							if (!isDuplicateEntry) {

								// create new map entry to prevent
								// hibernate-managed entity modification
								// TODO This probably could be handled by the
								// entry copy routines
								// for testing purposes, doing this explicitly
								MapEntry newEntry = new MapEntryJpa();
								newEntry.setMapAdvices(me.getMapAdvices());
								newEntry.setMapGroup(me.getMapGroup());
								newEntry.setMapBlock(me.getMapBlock());
								newEntry.setMapRecord(mr); // used for rule
															// propagation (i.e.
															// concept Id and
															// concept Name)
								newEntry.setRule(me.getRule());
								newEntry.setTargetId(me.getTargetId());
								newEntry.setTargetName(me.getTargetName());

								// set map priority based on size of current
								// list
								newEntry.setMapPriority(existingEntries.size() + 1);

								// set the propagated rule for this entry
								setPropagatedRuleForMapEntry(newEntry);

								// recalculate the map relation
								newEntry.setMapRelation(algorithmHandler
										.computeMapRelation(mapRecord, me));

								// add to the list
								existingEntries.add(newEntry);

								// replace existing list with modified list
								entriesByGroup.put(newEntry.getMapGroup(),
										existingEntries);

							}
						}
					}
				}
			}

			// increment the total record count
			nRecords++;

			// add the original entries
			System.out.println("Adding original entries: ");
			for (MapEntry me : mapRecord.getMapEntries()) {

				List<MapEntry> existingEntries = entriesByGroup.get(me
						.getMapGroup());

				if (existingEntries == null)
					existingEntries = new ArrayList<>();

				// create a new managed instance for this entry
				MapEntry newEntry = new MapEntryJpa();
				newEntry.setMapAdvices(me.getMapAdvices());
				newEntry.setMapGroup(me.getMapGroup());
				newEntry.setMapBlock(me.getMapBlock());
				newEntry.setMapRecord(mapRecord); // used for rule propagation
													// (i.e. concept Id and
													// concept Name)
				newEntry.setRule(me.getRule());
				newEntry.setTargetId(me.getTargetId());
				newEntry.setTargetName(me.getTargetName());

				// add map entry to map
				newEntry.setMapPriority(existingEntries.size() + 1);

				// if not the first entry and contains TRUE rule, set to
				// OTHERWISE TRUE
				if (newEntry.getMapPriority() > 1
						&& newEntry.getRule().equals("TRUE"))
					newEntry.setRule("OTHERWISE TRUE");

				// recalculate the map relation
				newEntry.setMapRelation(algorithmHandler.computeMapRelation(
						mapRecord, me));

				// add to the existing entries list
				existingEntries.add(newEntry);

				// replace the previous list with the new list
				entriesByGroup.put(newEntry.getMapGroup(), existingEntries);
			}

			// check that each group is "capped" with a TRUE or OTHERWISE
			// TRUE rule
			for (int mapGroup : entriesByGroup.keySet()) {

				List<MapEntry> existingEntries = entriesByGroup.get(mapGroup);

				// if no entries or last entry is not true
				if (existingEntries.size() == 0
						|| !existingEntries.get(existingEntries.size() - 1)
								.getRule().contains("TRUE")) {

					// create a new map entry
					MapEntry newEntry = new MapEntryJpa();

					// set the record and group
					newEntry.setMapRecord(mapRecord);
					newEntry.setMapGroup(mapGroup);
					newEntry.setMapPriority(existingEntries.size() + 1);

					// set the rule to TRUE if no entries, OTHERWISE true if
					// entries exist
					if (existingEntries.size() == 0)
						newEntry.setRule("TRUE");
					else
						newEntry.setRule("OTHERWISE TRUE");

					// compute the map relation for no target for this
					// project
					newEntry.setMapRelation(algorithmHandler
							.computeMapRelation(mapRecord, newEntry));

					existingEntries.add(newEntry);
					entriesByGroup.put(mapGroup, existingEntries);

				}
			}

			// write each group in sequence
			for (int mapGroup : entriesByGroup.keySet()) {
				for (MapEntry mapEntry : entriesByGroup.get(mapGroup)) {

					// write this entry
					writeReleaseEntry(machineReadableWriter,
							humanReadableWriter, mapEntry, mapRecord,
							mapProject, effectiveTime, moduleId);

				}
			}

		}

		// write the concepts with no id
		System.out.println("Concept errors (" + conceptErrors.keySet().size()
				+ ")");
		for (String terminologyId : conceptErrors.keySet()) {
			System.out.println("  " + terminologyId + ": "
					+ conceptErrors.get(terminologyId));
		}

		System.out.println("Total records released      : " + nRecords);
		System.out.println("Total records up-propagated : "
				+ nRecordsPropagated);
		System.out.println("Total records with errors   : "
				+ conceptErrors.keySet().size());

		// close the content service
		contentService.close();

		// close the writer
		machineReadableWriter.close();
		humanReadableWriter.close();
	}

	/**
	 * Function to construct propagated rule for an entry.
	 * 
	 * @param mapEntry
	 *            the map entry
	 * @return the map entry
	 */
	@SuppressWarnings("static-method")
	public MapEntry setPropagatedRuleForMapEntry(MapEntry mapEntry) {

		MapRecord mapRecord = mapEntry.getMapRecord();

		// construct propagated rule based on concept id and name
		// e.g. for TRUE rule
		// IFA 104831000119109 | Drug induced central sleep apnea
		//
		// for age rule
		// IFA 104831000119109 | Drug induced central sleep apnea
		// (disorder) | AND IFA 445518008 | Age at onset of clinical finding
		// (observable entity) | <= 28.0 days
		// (disorder)
		String rule = "IFA " + mapRecord.getConceptId() + " | "
				+ mapRecord.getConceptName() + " |";

		// if an age or gender rule, append the existing rule
		if (!mapEntry.getRule().contains("TRUE")) {
			rule += " AND " + mapEntry.getRule();
		}

		// set the rule
		mapEntry.setRule(rule);

		Logger.getLogger(MappingServiceJpa.class).info(
				"       Set rule to " + rule);
		/**
		 * e.g. for age IFA 104831000119109 | Drug induced central sleep apnea
		 * (disorder) | AND IFA 445518008 | Age at onset of clinical finding
		 * (observable entity) | <= 28.0 days IF DRUG INDUCED CENTRAL SLEEP
		 * APNEA AND IF AGE AT ONSET OF CLINICAL FINDING BEFORE 28.0 DAYS CHOOSE
		 * P28.3 | MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT P28.3 447561005
		 * 447639009 67fecd6d-f583-53de-9fbb-df292a764d08 20130731 1 449080006
		 * 447562003 27405005 1 3 IFA
		 */

		return mapEntry;
	}

	/**
	 * Gets the human readable map advice.
	 * 
	 * @param mapEntry
	 *            the map entry
	 * @return the human readable map advice
	 */
	@SuppressWarnings("static-method")
	public String getHumanReadableMapAdvice(MapEntry mapEntry) {

		String advice = "";

		System.out.println("Constructing human-readable advice for:  "
				+ mapEntry.getRule());

		String[] comparatorComponents; // used for parsing age rules

		// if map target is blank
		if (mapEntry.getTargetId() == null || mapEntry.getTargetId() == "") {
			System.out.println("  Use map relation");
			advice = mapEntry.getMapRelation().getName();
		}

		// if map rule is IFA (age)
		else if (mapEntry.getRule().toUpperCase().contains("AGE")) {
			// IF AGE AT ONSET OF
			// CLINICAL FINDING BETWEEN 1.0 YEAR AND 18.0 YEARS CHOOSE
			// M08.939

			// Rule examples
			// IFA 104831000119109 | Drug induced central sleep apnea
			// (disorder) | AND IFA 445518008 | Age at onset of clinical finding
			// (observable
			// entity) | < 65 years
			// IFA 104831000119109 | Drug induced central sleep apnea
			// (disorder) | AND IFA 445518008 | Age at onset of clinical finding
			// (observable entity) | <= 28.0 days
			// (disorder)

			// split by pipe (|) character. Expected fields
			// 0: IFA conceptId
			// 1: conceptName
			// 2: AND IFA ageConceptId
			// 3: Age rule type (Age at onset, Current chronological age)
			// 4: Comparator, Value, Units (e.g. < 65 years)
			// ---- The following only exist for two-value age rules
			// 5: AND IFA ageConceptId
			// 6: Age rule type (Age at onset, Current chronological age
			// 7: Comparator, Value, Units
			String[] ruleComponents = mapEntry.getRule().split("|");

			// add the type of age rule
			advice = "IF " + ruleComponents[3];

			// if a single component age rule, construct per example:
			// IF CURRENT CHRONOLOGICAL AGE ON OR AFTER 15.0 YEARS CHOOSE J20.9
			if (ruleComponents.length == 5) {

				comparatorComponents = ruleComponents[4].split(" ");

				// add appropriate text based on comparator
				switch (comparatorComponents[0]) {
				case ">":
					advice += " AFTER";
					break;
				case "<":
					advice += " BEFORE";
					break;
				case ">=":
					advice += " ON OR AFTER";
					break;
				case "<=":
					advice += " ON OR BEFORE";
					break;
				default:
					break;
				}

				// add the value and units
				advice += " " + comparatorComponents[1] + " "
						+ comparatorComponents[2];

				// otherwise, if a double-component age rule, construct per
				// example
				// IF AGE AT ONSET OF CLINICAL FINDING BETWEEN 1.0 YEAR AND 18.0
				// YEARS CHOOSE M08.939
			} else if (ruleComponents.length == 8) {

				advice += " BETWEEN ";

				// get the first comparator/value/units triple
				comparatorComponents = ruleComponents[4].split(" ");

				advice += comparatorComponents[1] + " "
						+ comparatorComponents[2];
			}

			// finally, add the CHOOSE {targetId}
			advice += " CHOOSE " + mapEntry.getTargetId();

			// if a gender rule (i.e. contains (FE)MALE)
		} else if (mapEntry.getRule().toUpperCase().contains("MALE")) {

			// add the advice based on gender
			if (mapEntry.getRule().toUpperCase().contains("FEMALE")) {
				advice += "IF FEMALE CHOOSE " + mapEntry.getTargetId();
			} else {
				advice += "IF MALE CHOOSE " + mapEntry.getTargetId();
			}
		} // if not an IFA rule (i.e. TRUE, OTHERWISE TRUE), simply return
			// ALWAYS
		else if (!mapEntry.getRule().toUpperCase().contains("IFA")) {

			advice = "ALWAYS " + mapEntry.getTargetId();

			// otherwise an IFA rule
		} else {
			String[] ifaComponents = mapEntry.getRule().toUpperCase()
					.split("\\|");

			// remove any (disorder), etc.
			String targetName = ifaComponents[1].trim(); // .replace("[(.*)]",
															// "");

			advice = "IF " + targetName + " CHOOSE " + mapEntry.getTargetId();
		}

		System.out.println("   Human-readable advice: " + advice);

		return advice;

	}

	/**
	 * Takes a tree position graph and converts it to a sorted list of tree
	 * positions where order is based on depth in tree
	 * 
	 * @param tp
	 *            the tp
	 * @return the sorted tree position descendant list
	 * @throws Exception
	 *             the exception
	 */
	@SuppressWarnings("static-method")
	public List<TreePosition> getSortedTreePositionDescendantList(
			TreePosition tp) throws Exception {

		// construct list of unprocessed tree positions and initialize with root
		// position
		List<TreePosition> positionsToAdd = new ArrayList<>();
		positionsToAdd.add(tp);

		List<TreePosition> sortedTreePositionDescendantList = new ArrayList<>();

		while (!positionsToAdd.isEmpty()) {

			// add the first element
			sortedTreePositionDescendantList.add(positionsToAdd.get(0));

			// add the children of first element
			for (TreePosition childTp : positionsToAdd.get(0).getChildren()) {
				positionsToAdd.add(childTp);
			}

			// remove the first element
			positionsToAdd.remove(0);
		}

		// sort the tree positions by position in the hierarchy (e.g. # of ~
		// characters)
		Collections.sort(sortedTreePositionDescendantList,
				new Comparator<TreePosition>() {
					@Override
					public int compare(TreePosition tp1, TreePosition tp2) {
						int levels1 = tp1.getAncestorPath().length()
								- tp1.getAncestorPath().replace("~", "")
										.length();
						int levels2 = tp1.getAncestorPath().length()
								- tp1.getAncestorPath().replace("~", "")
										.length();

						// if first has more ~'s than second, it is considered
						// LESS than the second
						// i.e. this is a reverse sort
						return levels2 - levels1;
					}
				});

		return sortedTreePositionDescendantList;
	}

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
	@SuppressWarnings("static-method")
	public int getNextMapPriority(MapRecord mapRecord, MapEntry mapEntry) {

		int maxPriority = 0;
		for (MapEntry me : mapRecord.getMapEntries()) {
			if (me.getMapGroup() == mapEntry.getMapGroup()
					&& mapEntry.getMapPriority() > maxPriority)
				maxPriority = mapEntry.getMapPriority();

		}

		return maxPriority + 1;
	}

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
	 * @param uid
	 *            the uid
	 * @return the raw bytes
	 */
	public byte[] getRawBytes(UUID uid) {
		String id = uid.toString();
		byte[] rawBytes = new byte[16];

		for (int i = 0, j = 0; i < 36; ++j) {
			// Need to bypass hyphens:
			switch (i) {
			case 8:
			case 13:
			case 18:
			case 23:
				++i;
				break;
			default:
				break;
			}
			char c = id.charAt(i);

			if (c >= '0' && c <= '9') {
				rawBytes[j] = (byte) ((c - '0') << 4);
			} else if (c >= 'a' && c <= 'f') {
				rawBytes[j] = (byte) ((c - 'a' + 10) << 4);
			}

			c = id.charAt(++i);

			if (c >= '0' && c <= '9') {
				rawBytes[j] |= (byte) (c - '0');
			} else if (c >= 'a' && c <= 'f') {
				rawBytes[j] |= (byte) (c - 'a' + 10);
			}
			++i;
		}
		return rawBytes;
	}

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
	public UUID getReleaseUuid(String name) throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		MessageDigest sha1Algorithm = MessageDigest.getInstance("SHA-1");

		String namespace = "00000000-0000-0000-0000-000000000000";
		String encoding = "UTF-8";

		UUID namespaceUUID = UUID.fromString(namespace);

		// Generate the digest.
		sha1Algorithm.reset();

		// Generate the digest.
		sha1Algorithm.reset();
		if (namespace != null) {
			sha1Algorithm.update(getRawBytes(namespaceUUID));
		}

		sha1Algorithm.update(name.getBytes(encoding));
		byte[] sha1digest = sha1Algorithm.digest();

		sha1digest[6] &= 0x0f; /* clear version */
		sha1digest[6] |= 0x50; /* set to version 5 */
		sha1digest[8] &= 0x3f; /* clear variant */
		sha1digest[8] |= 0x80; /* set to IETF variant */

		long msb = 0;
		long lsb = 0;
		for (int i = 0; i < 8; i++) {
			msb = (msb << 8) | (sha1digest[i] & 0xff);
		}
		for (int i = 8; i < 16; i++) {
			lsb = (lsb << 8) | (sha1digest[i] & 0xff);
		}

		return new UUID(msb, lsb);

	}

	/**
	 * Write release entry.
	 * 
	 * @param writer
	 *            the writer
	 * @param mapEntry
	 *            the map entry
	 * @param mapRecord
	 *            the map record
	 * @param mapProject
	 *            the map project
	 * @param effectiveTime
	 *            the effective time
	 * @param moduleId
	 *            the module id
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 */
	public void writeReleaseEntry(BufferedWriter machineReadableWriter,
			BufferedWriter humanReadableWriter, MapEntry mapEntry,
			MapRecord mapRecord, MapProject mapProject, String effectiveTime,
			String moduleId) throws IOException, NoSuchAlgorithmException {

		Logger.getLogger(MappingServiceJpa.class).info(
				"     Writing entry for concept " + mapRecord.getConceptId()
						+ ", map group " + mapEntry.getMapGroup()
						+ ", priority " + mapEntry.getMapPriority());

		// create UUID from refset id, concept id, map group, map rule,
		// map target
		UUID uuid = getReleaseUuid(mapProject.getRefSetId()
				+ mapRecord.getConceptId() + mapEntry.getMapGroup()
				+ mapEntry.getRule() + mapEntry.getTargetId());

		// construct human-readable map advice based on rule
		String mapAdviceStr = getHumanReadableMapAdvice(mapEntry);

		// add the entry's map advices
		for (MapAdvice mapAdvice : mapEntry.getMapAdvices()) {
			mapAdviceStr += " | " + mapAdvice.getDetail();
		}

		/**
		 * Add to advice based on target/relation and rule - If the map target
		 * is blank, advice contains the map relation name - If it's an IFA rule
		 * (gender), add MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT FOR GENDER -
		 * If it's an IFA rule (age/upproagated), add MAP OF SOURCE CONCEPT IS
		 * CONTEXT DEPENDENT
		 */

		// THIS is already done in getHumanReadableMapAdvice
		// if (mapEntry.getTargetId() == null ||
		// mapEntry.getTargetId().equals(""))
		// mapAdviceStr += " | " + mapEntry.getMapRelation().getName();
		// else

		if (mapEntry.getRule().startsWith("IFA")
				&& mapEntry.getRule().toUpperCase().contains("MALE")) {
			// do nothing
		}

		else if (mapEntry.getRule().startsWith("IFA"))
			mapAdviceStr += " | "
					+ "MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT";

		String entryLine = "";

		// switch line on map relation style
		if (mapProject.getMapRefsetPattern().equals("ExtendedMap")) {
			entryLine = uuid.toString()
					+ "\t"
					+ effectiveTime
					+ "\t"
					+ "1"
					+ "\t"
					+ moduleId
					+ "\t"
					+ mapProject.getRefSetId()
					+ "\t"
					+ mapRecord.getConceptId()
					+ "\t"
					+ mapEntry.getMapGroup()
					+ "\t"
					+ mapEntry.getMapPriority()
					+ "\t"
					+ mapEntry.getRule()
					+ "\t"
					+ mapAdviceStr
					+ "\t"
					+ (mapEntry.getTargetId() == null ? "" : mapEntry
							.getTargetId())
					+ "\t"
					+ "447561005"
					+ "\t"
					+ (mapEntry.getMapRelation() == null ? "THIS SHOULD NOT HAVE HAPPENED!!!"
							: mapEntry.getMapRelation().getTerminologyId());

			// ComplexMap style is identical to ExtendedMap
			// with the exception of the terminating map relation terminology id
		} else if (mapProject.getMapRefsetPattern().equals("ComplexMap")) {
			entryLine = uuid.toString()
					+ "\t"
					+ effectiveTime
					+ "\t"
					+ "1"
					+ "\t"
					+ moduleId
					+ "\t"
					+ mapProject.getRefSetId()
					+ "\t"
					+ mapRecord.getConceptId()
					+ "\t"
					+ mapEntry.getMapGroup()
					+ "\t"
					+ mapEntry.getMapPriority()
					+ "\t"
					+ mapEntry.getRule()
					+ "\t"
					+ mapAdviceStr
					+ "\t"
					+ (mapEntry.getTargetId() == null ? "" : mapEntry
							.getTargetId()) + "\t" + "447561005";
		}

		// write the line to the machine readable file with line nedings
		if (machineReadableWriter != null) {
			machineReadableWriter.write(entryLine + "\r\n");
		}

		// construct the additional fields for the human readable file
		if (humanReadableWriter != null) {
			entryLine += "\t" + mapRecord.getConceptName() + "\t"
					+ mapEntry.getTargetName() + "\t"
					+ mapEntry.getMapRelation().getName();

			// write the line to the human readable file
			humanReadableWriter.write(entryLine + "\r\n");
		}
	}
}
