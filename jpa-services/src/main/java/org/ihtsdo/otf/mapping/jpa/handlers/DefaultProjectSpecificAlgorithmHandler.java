package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

// TODO: Auto-generated Javadoc
/**
 * Reference implementation of {@link ProjectSpecificAlgorithmHandler}.
 *
 * @author ${author}
 */
public class DefaultProjectSpecificAlgorithmHandler implements
		ProjectSpecificAlgorithmHandler {

	/** The map project. */
	MapProject mapProject = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#getMapProject
	 * ()
	 */
	@Override
	public MapProject getMapProject() {
		return this.mapProject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#setMapProject
	 * (org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public void setMapProject(MapProject mapProject) {
		this.mapProject = mapProject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
	 * isMapAdviceComputable(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public boolean isMapAdviceComputable(MapRecord mapRecord) {
		if (mapProject != null) {
			for (MapAdvice mapAdvice : mapProject.getMapAdvices()) {
				if (mapAdvice.isComputed() == true)
					return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
	 * isMapRelationComputable(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public boolean isMapRelationComputable(MapRecord mapRecord) {
		if (mapProject != null) {
			for (MapRelation mapRelation : mapProject.getMapRelations()) {
				if (mapRelation.isComputed() == true)
					return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
	 * computeMapAdvice (org.ihtsdo.otf.mapping.model.MapRecord,
	 * org.ihtsdo.otf.mapping.model.MapEntry)
	 */
	@Override
	/**
	 * Given a map record and a map entry, returns any computed advice.
	 * This must be overwritten for each project specific handler.
	 * @param mapRecord
	 * @return
	 */
	public MapAdviceList computeMapAdvice(MapRecord mapRecord,
			MapEntry mapEntry) throws Exception {
		return null;
	}

	/**
	 * Given a map record and a map entry, returns the computed map relation (if
	 * applicable) This must be overwritten for each project specific handler.
	 * 
	 * @param mapRecord
	 *            the map record
	 * @param mapEntry
	 *            the map entry
	 * @return computed map relation
	 */
	@Override
	public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#validateRecord
	 * (org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public ValidationResult validateRecord(MapRecord mapRecord)
			throws Exception {

		ValidationResult validationResult = new ValidationResultJpa();

		validationResult.merge(performUniversalValidationChecks(mapRecord));
		validationResult.merge(validateTargetCodes(mapRecord));

		return validationResult;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
	 * validateTargetCodes(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	/**
	 * This must be overwritten for each project specific handler
	 */
	public ValidationResult validateTargetCodes(MapRecord mapRecord)
			throws Exception {
		return new ValidationResultJpa();
	}

	/**
	 * Perform universal validation checks.
	 * 
	 * @param mapRecord
	 *            the map record
	 * @return the validation result
	 */
	public ValidationResult performUniversalValidationChecks(MapRecord mapRecord) {
		Map<Integer, List<MapEntry>> entryGroups = getEntryGroups(mapRecord);

		ValidationResult validationResult = new ValidationResultJpa();

		// FATAL ERROR: map record has no entries
		if (mapRecord.getMapEntries().size() == 0) {
			validationResult.addError("Map record has no entries");
			return validationResult;
		}

		// FATAL ERROR: multiple map groups present for a project without group
		// structure
		if (!mapProject.isGroupStructure() && entryGroups.keySet().size() > 1) {
			validationResult
					.addError("Project has no group structure but multiple map groups were found.");
			return validationResult;
		}

		/*
		 * Group validation checks â€¢ Verify the last entry in a group is a TRUE
		 * rule â€¢ Verify higher map groups do not have only NC nodes
		 */

		// Validation Check: verify correct positioning of TRUE rules
		validationResult.merge(checkMapRecordTrueRules(mapRecord, entryGroups));

		// Validation Check: very higher map groups do not have only NC nodes
		validationResult.merge(checkMapRecordNcNodes(mapRecord, entryGroups));

		/*
		 * Entry Validation Checks â€¢ Verify no duplicate entries in record â€¢
		 * Verify advice values are valid for the project (this can happen if
		 * â€œallowable map adviceâ€� changes without updating map entries) â€¢ Entry
		 * must have target code that is both in the target terminology and
		 * valid (e.g. leaf nodes) OR have a relationId corresponding to a valid
		 * map category
		 */

		// Validation Check: verify entries are not duplicated
		validationResult.merge(checkMapRecordForDuplicateEntries(mapRecord));

		// Validation Check: verify advice values are valid for the project
		// (this
		// can happen if â€œallowable map adviceâ€� changes without updating map
		// entries)
		validationResult.merge(checkMapRecordAdvices(mapRecord, entryGroups));

		// Validation Check: very that map entry targets OR relationIds are
		// valid
		/*
		 * validationResult.merge(checkMapRecordTargets(mapRecord,
		 * entryGroups));
		 */

		return validationResult;
	}

	// ////////////////////
	// HELPER FUNCTIONS //
	// ////////////////////

	// ///////////////////////////////////////////////////////
	// Map Record Validation Checks and Helper Functions
	// ///////////////////////////////////////////////////////

	/**
	 * Function to check a map record for duplicate entries within map groups.
	 * 
	 * @param mapRecord
	 *            the map record
	 * @return a list of errors detected
	 */
	@SuppressWarnings("static-method")
	public ValidationResult checkMapRecordForDuplicateEntries(
			MapRecord mapRecord) {

		// Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
		// .info("  Checking map record for duplicate entries within map groups...");

		ValidationResult validationResult = new ValidationResultJpa();
		List<MapEntry> entries = mapRecord.getMapEntries();

		// cycle over all entries but last
		for (int i = 0; i < entries.size() - 1; i++) {

			// cycle over all entries after this one
			// NOTE:  separated boolean checks for easier handling of possible null relations
			for (int j = i + 1; j < entries.size(); j++) {
				
				// check if both targets are null OR if targets equal
				boolean targetIdsNull = entries.get(i).getTargetId().isEmpty() && entries.get(j).getTargetId().isEmpty();
				boolean targetIdsEqual = false;
				if (!targetIdsNull) {
					targetIdsEqual = entries.get(i).getTargetId().equals(entries.get(j).getTargetId());
				}
				
				// default:  relations are not equal
				boolean mapRelationsNull = entries.get(i).getMapRelation() == null && entries.get(j).getMapRelation() == null;
				boolean mapRelationsEqual = false;
				if (!mapRelationsNull) {
					if (entries.get(i).getMapRelation().equals(entries.get(j).getMapRelation())) mapRelationsEqual = true;
				}		

				// if target ids are the same, add error
				if (!targetIdsNull && targetIdsEqual) {

					validationResult.addError("Duplicate entries (same target code) found: "
							+ "Group "
							+ Integer.toString(entries.get(i).getMapGroup())
							+ ", priority " + Integer.toString(i) + " and "
							+ "Group "
							+ Integer.toString(entries.get(j).getMapGroup())
							+ ", priority " + Integer.toString(j));
				}
				
				// if target ids are null and map relations are equal, add error
				if (targetIdsNull && mapRelationsEqual) {
					validationResult.addError("Duplicate entries (null target code, same map relation) found: "
							+ "Group "
							+ Integer.toString(entries.get(i).getMapGroup())
							+ ", priority " + entries.get(i).getMapPriority() + " and "
							+ "Group "
							+ Integer.toString(entries.get(j).getMapGroup())
							+ ", priority " + entries.get(j).getMapPriority());
				}
			}
		}

		for (String error : validationResult.getErrors()) {
			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
					.info("    " + error);
		}

		return validationResult;
	}

	/**
	 * Function to check proper use of TRUE rules.
	 * 
	 * @param mapRecord
	 *            the map record
	 * @param entryGroups
	 *            the binned entry lists by group
	 * @return a list of errors detected
	 */
	public ValidationResult checkMapRecordTrueRules(MapRecord mapRecord,
			Map<Integer, List<MapEntry>> entryGroups) {

		// Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
		// "  Checking map record for proper use of TRUE rules...");

		ValidationResult validationResult = new ValidationResultJpa();

		// if not rule based, return empty validation result
		if (mapProject.isRuleBased() == false)
			return validationResult;

		// cycle over the groups
		for (Integer key : entryGroups.keySet()) {

			for (MapEntry mapEntry : entryGroups.get(key)) {

				Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
						.info("    Checking entry "
								+ Integer.toString(mapEntry.getMapPriority()));

				// add message if TRUE rule found at non-terminating entry
				if (mapEntry.getMapPriority() != entryGroups.get(key).size()
						&& mapEntry.getRule().equals("TRUE")) {
					validationResult
							.addError("Found non-terminating entry with TRUE rule."
									+ " Entry:"
									+ (mapProject.isGroupStructure() ? " group "
											+ Integer.toString(mapEntry
													.getMapGroup()) + ","
											: "")
									+ " map priority "
									+ Integer.toString(mapEntry
											.getMapPriority()));

					// add message if terminating entry rule is not TRUE
				} else if (mapEntry.getMapPriority() == entryGroups.get(key)
						.size() && !mapEntry.getRule().equals("TRUE")) {
					validationResult
							.addError("Terminating entry has non-TRUE rule."
									+ " Entry:"
									+ (mapProject.isGroupStructure() ? " group "
											+ Integer.toString(mapEntry
													.getMapGroup()) + ","
											: "")
									+ " map priority "
									+ Integer.toString(mapEntry
											.getMapPriority()));
				}
			}
		}
		for (String error : validationResult.getErrors()) {
			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
					.info("    " + error);
		}

		return validationResult;
	}

	/**
	 * Function to check higher level groups do not have only NC target codes.
	 * 
	 * @param mapRecord
	 *            the map record
	 * @param entryGroups
	 *            the binned entry lists by group
	 * @return a list of errors detected
	 */
	@SuppressWarnings("static-method")
	public ValidationResult checkMapRecordNcNodes(MapRecord mapRecord,
			Map<Integer, List<MapEntry>> entryGroups) {

		// Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
		// .info("  Checking map record for high-level groups with only NC target codes...");

		ValidationResult validationResult = new ValidationResultJpa();

		// if only one group, return empty validation result (also covers
		// non-group-structure projects)
		if (entryGroups.keySet().size() == 1)
			return validationResult;

		// otherwise cycle over the high-level groups (i.e. all but first group)
		for (int group : entryGroups.keySet()) {

			if (group != 1) {
				List<MapEntry> entries = entryGroups.get(group);

				boolean isValidGroup = false;
				for (MapEntry entry : entries) {
					if (entry.getTargetId() != null
							&& entry.getTargetId() != "")
						isValidGroup = true;
				}

				if (!isValidGroup) {
					validationResult.addError("High-level group "
							+ Integer.toString(group)
							+ " has no entries with targets");
				}
			}
		}

		for (String error : validationResult.getErrors()) {
			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
					.info("    " + error);
		}

		return validationResult;
	}

	/**
	 * Function to check that all advices attached are allowable by the project.
	 * 
	 * @param mapRecord
	 *            the map record
	 * @param entryGroups
	 *            the binned entry lists by group
	 * @return a list of errors detected
	 */
	public ValidationResult checkMapRecordAdvices(MapRecord mapRecord,
			Map<Integer, List<MapEntry>> entryGroups) {

		// Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
		// "  Checking map record for valid map advices...");

		ValidationResult validationResult = new ValidationResultJpa();

		for (MapEntry mapEntry : mapRecord.getMapEntries()) {

			for (MapAdvice mapAdvice : mapEntry.getMapAdvices()) {

				if (!mapProject.getMapAdvices().contains(mapAdvice)) {
					validationResult.addError("Invalid advice "
							+ mapAdvice.getName()
							+ "."
							+ " Entry:"
							+ (mapProject.isGroupStructure() ? " group "
									+ Integer.toString(mapEntry.getMapGroup())
									+ "," : "") + " map priority "
							+ Integer.toString(mapEntry.getMapPriority()));
				}
			}

		}

		for (String error : validationResult.getErrors()) {
			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
					.info("    " + error);
		}

		return validationResult;
	}

	/**
	 * Helper function to sort a records entries into entry lists binned by
	 * group.
	 * 
	 * @param mapRecord
	 *            the map record
	 * @return a map of group->entry list
	 */
	@SuppressWarnings("static-method")
	public Map<Integer, List<MapEntry>> getEntryGroups(MapRecord mapRecord) {

		Map<Integer, List<MapEntry>> entryGroups = new HashMap<>();

		for (MapEntry entry : mapRecord.getMapEntries()) {

			// if no existing set for this group, create a blank set
			List<MapEntry> entryGroup = entryGroups.get(entry.getMapGroup());
			if (entryGroup == null) {
				entryGroup = new ArrayList<>();
			}

			// add this entry to group and put it in group map
			entryGroup.add(entry);
			entryGroups.put(entry.getMapGroup(), entryGroup);
		}

		return entryGroups;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
	 * compareMapRecords(org.ihtsdo.otf.mapping.model.MapRecord,
	 * org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public ValidationResult compareMapRecords(MapRecord record1,
			MapRecord record2) {
		ValidationResult validationResult = new ValidationResultJpa();

		// compare mapProjectId
		if (record1.getMapProjectId() != record2.getMapProjectId())
			validationResult.addError("Map Project Ids don't match! "
					+ record1.getMapProjectId() + " "
					+ record2.getMapProjectId());

		// compare conceptId
		if (!record1.getConceptId().equals(record2.getConceptId()))
			validationResult.addError("Concept Ids don't match! "
					+ record1.getConceptId() + " " + record2.getConceptId());

		// compare mapPrinciples
		Comparator<Object> principlesComparator = new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {

				String x1 = ((MapPrinciple) o1).getPrincipleId();
				String x2 = ((MapPrinciple) o2).getPrincipleId();

				if (!x1.equals(x2)) {
					return x1.compareTo(x2);
				}
				return 0;
			}
		};
		List<MapPrinciple> principles1 = new ArrayList<>(
				record1.getMapPrinciples());
		Collections.sort(principles1, principlesComparator);
		List<MapPrinciple> principles2 = new ArrayList<>(
				record2.getMapPrinciples());
		Collections.sort(principles2, principlesComparator);

		if (principles1.size() != principles2.size())
			validationResult.addWarning("Map Principles count doesn't match! "
					+ principles1.toString() + " " + principles2.toString());
		else {
			for (int i = 0; i < principles1.size(); i++) {
				if (!principles1.get(i).getPrincipleId()
						.equals(principles2.get(i).getPrincipleId()))
					validationResult
							.addWarning("Map Principles content doesn't match! "
									+ principles1.toString()
									+ " "
									+ principles2.toString());
			}
		}

		// check force map lead review flag
		if (record1.isFlagForMapLeadReview()) {
			validationResult
					.addError("Specialist 1 indicated the need for map lead review.");
		}
		if (record2.isFlagForMapLeadReview()) {
			validationResult
					.addError("Specialist 2 indicated the need for map lead review.");
		}

		// check consensus review flag
		if (record1.isFlagForConsensusReview()) {
			validationResult
					.addError("Specialist 1 indicated consensus review is required.");
		}
		if (record2.isFlagForConsensusReview()) {
			validationResult
					.addError("Specialist 2 indicated consensus review is required.");
		}

		// compare mapEntries
		// organize map entries by group
		Map<Integer, List<MapEntry>> groupToMapEntryList1 = new HashMap<>();
		for (MapEntry entry : record1.getMapEntries()) {
			if (groupToMapEntryList1.containsKey(entry.getMapGroup())) {
				List<MapEntry> entryList = groupToMapEntryList1.get(entry
						.getMapGroup());
				entryList.add(entry);
			} else {
				List<MapEntry> entryList = new ArrayList<>();
				entryList.add(entry);
				groupToMapEntryList1.put(entry.getMapGroup(), entryList);
			}
		}
		Map<Integer, List<MapEntry>> groupToMapEntryList2 = new HashMap<>();
		for (MapEntry entry : record2.getMapEntries()) {
			if (groupToMapEntryList2.containsKey(entry.getMapGroup())) {
				List<MapEntry> entryList = groupToMapEntryList2.get(entry
						.getMapGroup());
				entryList.add(entry);
			} else {
				List<MapEntry> entryList = new ArrayList<>();
				entryList.add(entry);
				groupToMapEntryList2.put(entry.getMapGroup(), entryList);
			}
		}

		// for each group
		for (int i = 1; i < Math.max(groupToMapEntryList1.size(),
				groupToMapEntryList2.size()) + 1; i++) {
			List<MapEntry> entries1 = groupToMapEntryList1.get(new Integer(i));
			List<MapEntry> entries2 = groupToMapEntryList2.get(new Integer(i));

			// error if different numbers of entries
			if (entries1 == null) {
				validationResult.addError("Record 1 Group " + i
						+ " has no entries!");
				continue;
			} else if (entries2 == null) {
				validationResult.addError("Record 2 Group " + i
						+ " has no entries!");
				continue;
			} else if (entries1.size() != entries2.size()) {
				validationResult
						.addError("Groups have different entry counts!  Record 1, Group "
								+ i
								+ ":"
								+ entries1.size()
								+ " Record 2, Group "
								+ i
								+ ":"
								+ entries2.size());
			}

			// create string lists for entry comparison
			List<String> stringEntries1 = new ArrayList<>();
			List<String> stringEntries2 = new ArrayList<>();
			for (MapEntry entry1 : entries1) {
				stringEntries1.add(convertToString(entry1));
			}
			for (MapEntry entry2 : entries2) {
				stringEntries2.add(convertToString(entry2));
			}

			// check for matching entries in different order
			boolean outOfOrderFlag = false;
			boolean missingEntry = false;

			for (int d = 0; d < Math.min(stringEntries1.size(),
					stringEntries2.size()); d++) {

				if (stringEntries1.get(d).equals(stringEntries2.get(d)))
					continue;
				else if (stringEntries2.contains(stringEntries1.get(d)))
					outOfOrderFlag = true;
				else
					missingEntry = true;
			}
			if (!outOfOrderFlag && !missingEntry) {
				continue; // to next group for comparison
			}
			if (outOfOrderFlag && !missingEntry) {
				validationResult.addWarning("Group " + i
						+ " has all the same entries but in different orders.");
				continue; // to next group for comparison
			}

			// check for details of missing entries
			boolean matchFound = false;
			for (int d = 0; d < entries1.size(); d++) {
				for (int f = 0; f < entries2.size(); f++) {					
					if (isRulesEqual(entries1.get(d), entries2.get(f))
							&& isTargetIdsEqual(entries1.get(d), entries2.get(f))
							&& !isMapRelationsEqual(entries1.get(d), entries2.get(f)))
						matchFound = true;
				}
				if (matchFound) {
					validationResult
							.addError("Record "
									+ convertToString(entries1.get(d))
									+ " matches an entry from record 2 on rule and target code but not on relation id.");
				}
				matchFound = false;
			}
			for (int d = 0; d < entries1.size(); d++) {
				for (int f = 0; f < entries2.size(); f++) {
					if (isRulesEqual(entries1.get(d), entries2.get(f))
							&& isTargetIdsEqual(entries1.get(d), entries2.get(f))
							&& !entries1.get(d).getMapAdvices()
									.equals(entries2.get(f).getMapAdvices()))
						matchFound = true;
				}
				if (matchFound) {
					validationResult
							.addError("Record "
									+ convertToString(entries1.get(d))
									+ " matches an entry from record 2 on rule and target code but not on advice.");
				}
				matchFound = false;
			}
			for (int d = 0; d < entries1.size(); d++) {
				for (int f = 0; f < entries2.size(); f++) {
					if (isRulesEqual(entries1.get(d), entries2.get(f))
							&& !isTargetIdsEqual(entries1.get(d), entries2.get(f)))
						matchFound = true;
				}
				if (matchFound) {
					validationResult
							.addError("Record "
									+ convertToString(entries1.get(d))
									+ " matches an entry from record 2 on rule but not on target code.");
				}
				matchFound = false;
			}
			for (int d = 0; d < entries1.size(); d++) {
				for (int f = 0; f < entries2.size(); f++) {
					if (isRulesEqual(entries1.get(d), entries2.get(f)))
						matchFound = true;
				}
				if (!matchFound) {
					validationResult
							.addError("Record "
									+ convertToString(entries1.get(d))
									+ " does not match any entry from record 2 on rule.");
				}
				matchFound = false;
			}

		}

		return validationResult;
	}
	
	/**
	 * Indicates whether or not rules are equal.
	 *
	 * @param entry1 the entry1
	 * @param entry2 the entry2
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean isRulesEqual(MapEntry entry1, MapEntry entry2) {
		// check null comparisons first
		if (entry1.getRule() == null && entry2.getRule() != null)
			return false;
		if (entry1.getRule() != null && entry2.getRule() == null)
			return false;
		if (entry1.getRule() == null && entry2.getRule() == null)
			return true;
		return entry1.getRule().equals(entry2.getRule());
	}
	
	/**
	 * Indicates whether or not target ids are equal.
	 *
	 * @param entry1 the entry1
	 * @param entry2 the entry2
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean isTargetIdsEqual(MapEntry entry1, MapEntry entry2) {
		// check null comparisons first
		if (entry1.getTargetId() == null && entry2.getTargetId() != null)
			return false;
		if (entry1.getTargetId() != null && entry2.getTargetId() == null)
			return false;
		if (entry1.getTargetId() == null && entry2.getTargetId() == null)
			return true;
		return entry1.getTargetId().equals(entry2.getTargetId());
	}
	
	/**
	 * Indicates whether or not map relations are equal.
	 *
	 * @param entry1 the entry1
	 * @param entry2 the entry2
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 */
	public boolean isMapRelationsEqual(MapEntry entry1, MapEntry entry2) {
		// check null comparisons first
		if (entry1.getMapRelation() == null && entry2.getMapRelation() != null)
			return false;
		if (entry1.getMapRelation() != null && entry2.getMapRelation() == null)
			return false;
		if (entry1.getMapRelation() == null && entry2.getMapRelation() == null)
			return true;
		return entry1.getMapRelation().getId().equals(entry2.getMapRelation().getId());
	}

	/**
	 * Convert to string.
	 * 
	 * @param mapEntry
	 *            the map entry
	 * @return the string
	 */
	private String convertToString(MapEntry mapEntry) {

		// check map advices
		Comparator<Object> advicesComparator = new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {

				String x1 = ((MapAdvice) o1).getName();
				String x2 = ((MapAdvice) o2).getName();

				if (!x1.equals(x2)) {
					return x1.compareTo(x2);
				}
				return 0;
			}
		};

		List<MapAdvice> advices = new ArrayList<>(mapEntry.getMapAdvices());
		Collections.sort(advices, advicesComparator);

		StringBuffer sb = new StringBuffer();
		sb.append(mapEntry.getTargetId()
				+ " "
				+ mapEntry.getRule()
				+ " "
				+ (mapEntry.getMapRelation() != null ? mapEntry
						.getMapRelation().getId() : ""));
		for (MapAdvice mapAdvice : advices) {
			sb.append(mapAdvice.getObjectId() + " ");
		}

		return sb.toString();
	}

	/**
	 * For default project, all target codes are considered valid.
	 * 
	 * @param terminologyId
	 *            the terminology id
	 * @return true, if is target code valid
	 * @throws Exception
	 *             the exception
	 */
	@Override
	public boolean isTargetCodeValid(String terminologyId) throws Exception {
		return true;
	}

	/**
	 * Assign a new map record from existing record, performing any necessary
	 * workflow actions based on workflow status
	 * 
	 * - READY_FOR_PUBLICATION, PUBLICATION -> FIX_ERROR_PATH: Create a new
	 * record with origin ids set to the existing record (and its antecedents) -
	 * Add the record to the tracking record - Return the tracking record.
	 * 
	 * - NEW, EDITING_IN_PROGRESS, EDITING_DONE, CONFLICT_NEW,
	 * CONFLICT_IN_PROGRESS Invalid workflow statuses, should never be called
	 * with a record of this nature
	 * 
	 * Expects the tracking record to be a detached Jpa entity. Does not modify
	 * objects via services.
	 *
	 * @param trackingRecord            the tracking record
	 * @param mapRecords the map records
	 * @param mapRecord            the map record
	 * @param mapUser            the map user
	 * @return the workflow tracking record
	 * @throws Exception             the exception
	 */
	@Override
	public Set<MapRecord> assignFromInitialRecord(
			TrackingRecord trackingRecord, Set<MapRecord> mapRecords,
			MapRecord mapRecord, MapUser mapUser) throws Exception {

		Set<MapRecord> newRecords = new HashSet<>();

		switch (trackingRecord.getWorkflowPath()) {

		case FIX_ERROR_PATH:
			
			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info("assignFromInitialRecord:  FIX_ERROR_PATH");

			// case 1 : User claims a PUBLISHED or READY_FOR_PUBLICATION record to fix error on.
			if (mapRecord.getWorkflowStatus()
					.equals(WorkflowStatus.PUBLISHED) || mapRecord
					.getWorkflowStatus().equals(
							WorkflowStatus.READY_FOR_PUBLICATION)) {

				// check that only one record exists for this tracking record
				if (!(trackingRecord.getMapRecordIds().size() == 1)) {
					System.out.println(trackingRecord.toString());
					throw new Exception(
							"DefaultProjectSpecificHandlerException - assignFromInitialRecord: More than one record exists for FIX_ERROR_PATH assignment.");
				}
	
				// deep copy the map record
				MapRecord newRecord = new MapRecordJpa(mapRecord, true);
	
				// set origin ids
				newRecord.addOrigin(mapRecord.getId());
				newRecord.addOrigins(mapRecord.getOriginIds());
	
				// set other relevant fields
				newRecord.setOwner(mapUser);
				newRecord.setLastModifiedBy(mapUser);
				newRecord.setWorkflowStatus(WorkflowStatus.NEW);
	
				// add the record to the list
				newRecords.add(newRecord);
	
				// set the workflow status of the old record to review and add it to
				// new records
				mapRecord.setWorkflowStatus(WorkflowStatus.REVISION);
				newRecords.add(mapRecord);
			}
			
			break;	

		case CONSENSUS_PATH:
			break;
		case LEGACY_PATH:
			break;
		case NON_LEGACY_PATH:
			throw new Exception(
					"Invalid assignFromInitialRecord call for NON_LEGACY_PATH workflow");
		case QA_PATH:
			break;
		default:
			throw new Exception(
					"assignFromInitialRecord called with invalid Workflow Path.");

		}

		return newRecords;
	}

	/**
	 * Assign a map user to a concept for this project
	 * 
	 * Conditions: - Only valid workflow paths: NON_LEGACY_PATH, LEGACY_PATH,
	 * and CONSENSUS_PATH Note that QA_PATH and FIX_ERROR_PATH should never call
	 * this method - Only valid workflow statuses: Any status preceding
	 * CONFLICT_DETECTED and CONFLICT_DETECTED
	 * 
	 * Default Behavior: - Create a record with workflow status based on current
	 * workflow status - Add the record to the tracking record - Return the
	 * tracking record.
	 *
	 * @param trackingRecord            the tracking record
	 * @param mapRecords the map records
	 * @param concept            the concept
	 * @param mapUser            the map user
	 * @return the workflow tracking record
	 * @throws Exception             the exception
	 */
	@Override
	public Set<MapRecord> assignFromScratch(
			TrackingRecord trackingRecord, Set<MapRecord> mapRecords,
			Concept concept, MapUser mapUser) throws Exception {

		// the list of map records to return
		Set<MapRecord> newRecords = new HashSet<>(mapRecords);
		
		for (MapRecord mr : mapRecords) {
			System.out.println(mr.toString());
		}

		// create new record
		MapRecord mapRecord = new MapRecordJpa();
		mapRecord.setMapProjectId(mapProject.getId());
		mapRecord.setConceptId(concept.getTerminologyId());
		mapRecord.setConceptName(concept.getDefaultPreferredName());
		mapRecord.setOwner(mapUser);
		mapRecord.setLastModifiedBy(mapUser);
		mapRecord.setWorkflowStatus(WorkflowStatus.NEW);

		// set additional record parameters based on workflow path and status
		switch (trackingRecord.getWorkflowPath()) {
		case NON_LEGACY_PATH:

			// if a "new" tracking record (i.e. prior to conflict detection),
			// add a NEW record
			if (getWorkflowStatus(mapRecords).compareTo(
					WorkflowStatus.CONFLICT_DETECTED) < 0) {

				// check that this record is valid to be assigned (i.e. no more
				// than
				// one other specialist assigned)
				if (getMapUsers(mapRecords).size() >= 2) {
					throw new Exception(
							"DefaultProjectSpecificHandlerException - assignFromScratch:  Two users already assigned");
				}

				mapRecord.setWorkflowStatus(WorkflowStatus.NEW);
				Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
						.info("NON_LEGACY_PATH: NEW");

				// otherwise, if this is a tracking record with conflict
				// detected, add a CONFLICT_NEW record
			} else if (getWorkflowStatus(mapRecords).equals(
					WorkflowStatus.CONFLICT_DETECTED)) {

				mapRecord.setWorkflowStatus(WorkflowStatus.CONFLICT_NEW);

				// get the origin ids from the tracking record
				for (MapRecord mr : newRecords) {
					mapRecord.addOrigin(mr.getId());
				}
				Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
						.info("NON_LEGACY_PATH: CONFLICT_NEW");

			}
			break;
			
		case FIX_ERROR_PATH:
			
			// Case 1:  A lead claims an error-fixed record for review
			if (getWorkflowStatus(mapRecords).equals(WorkflowStatus.REVIEW_NEEDED)) {
				
				Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
				.info("FIX_ERROR_PATH: Lead claiming an error-fixed record");
							
				// check that only one record exists for this tracking record
				if (!(trackingRecord.getMapRecordIds().size() == 1)) {
					System.out.println(trackingRecord.toString());
					throw new Exception(
							"assignFromScratch: More than one record exists for FIX_ERROR_PATH assignment.");
				}
				
				// deep copy the new record
				mapRecord = new MapRecordJpa(mapRecord, true);
				
				// set origin id
				for (MapRecord mr : mapRecords) {
					mapRecord.addOrigin(mr.getId());
					mapRecord.addOrigins(mr.getOriginIds());
				}
				
				// set other relevant fields
				mapRecord.setOwner(mapUser);
				mapRecord.setLastModifiedBy(mapUser);
				mapRecord.setWorkflowStatus(WorkflowStatus.REVIEW_NEW);
				
				Logger.getLogger("FIX_ERROR_PATH final record: " + mapRecord.toString());

			} else {
				throw new Exception("assignFromScratch called on FIX_ERROR_PATH but tracking record is not of status REVIEW_NEEDED");
			}
			
			break;
			
		case CONSENSUS_PATH:
			break;
		case LEGACY_PATH:
			break;
		case QA_PATH:
			break;

			

		default:
			throw new Exception(
					"assignFromScratch called with erroneous Workflow Path.");
		}

		ContentService contentService = new ContentServiceJpa();
		mapRecord.setCountDescendantConcepts(new Long(
		// get the tree positions for this concept
				contentService
						.getTreePositionsWithDescendants(
								trackingRecord.getTerminologyId(),
								trackingRecord.getTerminology(),
								trackingRecord.getTerminologyVersion())

						.getTreePositions() // get the list of tree positions
						.get(0) // get the first tree position
						.getDescendantCount())); // get the descendant count
		contentService.close();

		// add this record to the tracking record
		newRecords.add(mapRecord);

		// return the modified record set
		return newRecords;
	}

	/**
	 * Unassigns a user from a concept, and performs any other necessary
	 * workflow actions
	 * 
	 * Conditions: - Valid workflow paths: All - Valid workflow status: All
	 * except READY_FOR_PUBLICATION, PUBLISHED.
	 *
	 * @param trackingRecord            the tracking record
	 * @param mapRecords the map records
	 * @param mapUser            the map user
	 * @return the workflow tracking record
	 * @throws Exception             the exception
	 */
	@Override
	public Set<MapRecord> unassign(TrackingRecord trackingRecord,
			Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

		Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class).info(
				"Unassign called along " + trackingRecord.getWorkflowPath() + " path");

		Set<MapRecord> newRecords = new HashSet<>(mapRecords);
		

		// find the map record this user is assigned to
		MapRecord mapRecord = null;
		for (MapRecord mr : newRecords) {
			if (mr.getOwner().equals(mapUser)) {
				mapRecord = mr;
			}
		}

		// switch on workflow path
		switch (trackingRecord.getWorkflowPath()) {
		case NON_LEGACY_PATH:

			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
					.info("Unassign: NON_LEGACY_PATH");

			

			if (mapRecord == null)
				throw new Exception(
						"unassign called for concept that does not have specified user assigned");

			// remove this record from the tracking record
			newRecords.remove(mapRecord);

			// determine action based on record's workflow status after removal
			// of
			// the unassigned record
			switch (mapRecord.getWorkflowStatus()) {

			// standard removal cases, no special action required
			case NEW:
			case EDITING_IN_PROGRESS:
			case EDITING_DONE:
			case CONFLICT_NEW:
			case CONFLICT_IN_PROGRESS:
			case REVIEW_NEEDED:
			case REVIEW_NEW:
			case REVIEW_IN_PROGRESS:

				Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
						.info("Unassign: NON_LEGACY_PATH -- "
								+ mapRecord.getWorkflowStatus() + " -- No special action required");

				break;

			// where a conflict is detected, two cases to handle
			// (1) any lead-claimed resolution record is now invalid, and should
			// be deleted (no conflict remaining)
			// (2) a specialist's record is now not in conflict, and should be
			// reverted to EDITING_DONE
			case CONFLICT_DETECTED:

				MapRecord recordToRemove = null;
				for (MapRecord mr : newRecords) {

					// if another specialist's record, revert to EDITING_DONE
					if (mr.getWorkflowStatus().equals(
							WorkflowStatus.CONFLICT_DETECTED)) {
						mr.setWorkflowStatus(WorkflowStatus.EDITING_DONE);
					}

					// if the lead's record, delete
					if (mr.getWorkflowStatus().equals(
							WorkflowStatus.CONFLICT_NEW)) {
						recordToRemove = mr;
					}
				}

				if (recordToRemove != null)
					newRecords.remove(recordToRemove);

				break;	
	
			// consensus path not implemented
			case CONSENSUS_NEEDED:
				break;
			case CONSENSUS_IN_PROGRESS:
				break;

			// If REVISION is detected, something has gone horribly wrong for the
			// NON_LEGACY_PATH
			case REVISION:

				throw new Exception(
						"Unassign:  A user has been improperly assigned to a review record");

				// throw an exception if somehow an invalid workflow status is
				// found
			default:
				throw new Exception(
						"unassign found a map record with invalid workflow status");
			}

			break;

		case LEGACY_PATH:
			break;

		// If unassignment for error fixing, need to reset the existing record
		// from REVIEW
		case FIX_ERROR_PATH:

			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
					.info("Unassign:  FIX_ERROR_PATH");
			
			// Case 1: A user decides not to attempt to fix an error
			if (getWorkflowStatus(mapRecords).compareTo(WorkflowStatus.REVISION) == 0) {
	
				// cycle over records
				for (MapRecord mr : mapRecords) {
	
					// set the REVISION record to its previous state
					if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
						mr = getPreviousVersionOfMapRecord(mr);
					} else if (mr.getOwner().equals(mapUser)) {
						newRecords.remove(mr);
					} else {
						throw new Exception(
								"Unassign called on FIX_ERROR_PATH tracking record for "
										+ mapUser.getName() + ", but record (id="
										+ mr.getId() + " is neither REVISION status nor owned by user.");
					}
				}
				
			// Case 2:  A lead unassigns themselves from reviewing a fixed error
			// delete the record, no special action needed
			} else if ((getWorkflowStatus(mapRecords).compareTo(WorkflowStatus.REVIEW_NEW) == 0
					|| getWorkflowStatus(mapRecords).compareTo(WorkflowStatus.REVIEW_IN_PROGRESS) == 0)) {
						
				newRecords.remove(mapRecord);
			}

			break;
		case QA_PATH:
			break;
		case CONSENSUS_PATH:
			break;

		default:
			break;
		}

		// return the modified record set
		return newRecords;

	}

	/**
	 * Updates workflow information when a specialist or lead clicks "Finished"
	 * Expects the tracking record to be detached from persistence environment.
	 *
	 * @param trackingRecord            the tracking record
	 * @param mapRecords the map records
	 * @param mapUser            the map user
	 * @return the workflow tracking record
	 * @throws Exception             the exception
	 */
	@Override
	public Set<MapRecord> finishEditing(TrackingRecord trackingRecord,
			Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

		Set<MapRecord> newRecords = new HashSet<>(mapRecords);

		// find the record assigned to this user
		MapRecord mapRecord = null;
		for (MapRecord mr : newRecords) {
			if (mr.getOwner().equals(mapUser)) {
				mapRecord = mr;
			}
		}
		if (mapRecord == null)
			throw new Exception(
					"finishEditing:  Record for user could not be found");

		// switch on workflow path
		switch (trackingRecord.getWorkflowPath()) {
		case NON_LEGACY_PATH:

			// case 1: A specialist is finished with a record
			if (getWorkflowStatus(mapRecords).compareTo(
					WorkflowStatus.CONFLICT_DETECTED) < 0) {

				Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
						.info("NON_LEGACY_PATH - New finished record, checking for other records");

				// set this record to EDITING_DONE
				mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_DONE);

				// check if two specialists have completed work
				if (getLowestWorkflowStatus(mapRecords).equals(
						WorkflowStatus.EDITING_DONE)
						&& mapRecords.size() == 2) {

					Logger.getLogger(
							DefaultProjectSpecificAlgorithmHandler.class).info(
							"NON_LEGACY_PATH - Two records found");

					java.util.Iterator<MapRecord> record_iter = mapRecords
							.iterator();
					MapRecord mapRecord1 = record_iter.next();
					MapRecord mapRecord2 = record_iter.next();
					ValidationResult validationResult = compareMapRecords(
							mapRecord1, mapRecord2);

					// if map records validation is successful
					if (validationResult.isValid() == true) {

						Logger.getLogger(
								DefaultProjectSpecificAlgorithmHandler.class)
								.info("NON_LEGACY_PATH - No conflicts detected.");

						// deep copy the record and mark the new record
						// READY_FOR_PUBLICATION
						MapRecord newRecord = new MapRecordJpa(mapRecord, true);
						newRecord.setOwner(mapUser);
						newRecord.setLastModifiedBy(mapUser);
						mapRecord.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

						// construct and set the new origin ids
						Set<Long> originIds = new HashSet<>();
						originIds.add(mapRecord1.getId());
						originIds.add(mapRecord2.getId());
						originIds.addAll(mapRecord1.getOriginIds());
						originIds.addAll(mapRecord2.getOriginIds());
						newRecord.setOriginIds(originIds);

						// clear the records and add a single record owned by this user
						newRecords.clear();
						newRecords.add(newRecord);

					} else {

						Logger.getLogger(
								DefaultProjectSpecificAlgorithmHandler.class)
								.info("NON_LEGACY_PATH - Conflicts detected");

						// conflict detected, change workflow status of all
						// records and
						// update records
						for (MapRecord mr : newRecords) {
							mr.setWorkflowStatus(WorkflowStatus.CONFLICT_DETECTED);
						}
					}
					// otherwise, only one specialist has finished work, do
					// nothing else
				} else {
					Logger.getLogger(
							DefaultProjectSpecificAlgorithmHandler.class)
							.info("NON_LEGACY_PATH - Only this specialist has completed work");
				}

				// case 2: A lead is finished with a conflict resolution
				// Determined by workflow status of:
				// CONFLICT_NEW (i.e. conflict was resolved immediately)
				// CONFLICT_IN_PROGRESS (i.e. conflict had been previously saved
				// for later)
			} else if (getWorkflowStatus(mapRecords).equals(
					WorkflowStatus.CONFLICT_NEW)
					|| getWorkflowStatus(mapRecords).equals(
							WorkflowStatus.CONFLICT_IN_PROGRESS)) {

				Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
						.info("NON_LEGACY_PATH - Conflict resolution detected");

				// cycle over the previously existing records
				for (MapRecord mr : mapRecords) {

					// remove the CONFLICT_DETECTED records from the revised set
					if (mr.getWorkflowStatus().equals(
							WorkflowStatus.CONFLICT_DETECTED)) {
						newRecords.remove(mr);

						// set the CONFLICT_NEW or CONFLICT_IN_PROGRESS record
						// to
						// READY_FOR_PUBLICATION
						// and update
					} else {
						mr.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
					}
				}
				
				// case 3: A lead is finished reviewing a record with no conflicts
			} else if (getWorkflowStatus(mapRecords).equals(
					WorkflowStatus.REVIEW_NEW) || getWorkflowStatus(mapRecords).equals(WorkflowStatus.REVIEW_IN_PROGRESS)) {
				

			} else {
				throw new Exception("finishEditing failed! Invalid workflow status combination on record(s)");
			}

			break;

		case FIX_ERROR_PATH:

			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
					.info("FIX_ERROR_PATH");
			
			// case 1:  A user has finished correcting an error on a previously published record
			if (this.getWorkflowStatus(mapRecords).compareTo(WorkflowStatus.REVISION) == 0) {
				

				Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
						.info("  User has finished correcting an error");
	
				// assumption check: should only be 2 records
				// 1) The original record (now marked REVIEW)
				// 2) The modified record
				if (mapRecords.size() != 2) {
					throw new Exception(
							"finishEditing on FIX_ERROR_PATH:  Attempt to fix an error requires exactly two map records.");
				}
	
				// cycle over the records
				for (MapRecord mr : mapRecords) {
	
					// if the original PUBLISHED/READY_FOR_PUBLICATION record (i.e.
					// now has REVISION), remove
					if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
						newRecords.remove(mr);
					} else {
						mr.setWorkflowStatus(WorkflowStatus.REVIEW_NEEDED);
					}
				}
	
				// Case 2:  A lead has finished reviewing a corrected error and is ready to send it to publication status
			} else if (getWorkflowStatus(mapRecords).compareTo(WorkflowStatus.REVIEW_NEW) == 0 || getWorkflowStatus(mapRecords).compareTo(WorkflowStatus.REVIEW_IN_PROGRESS) == 0 ) {
				
				// assumption check
				// 1) should only be one record
				if (mapRecords.size() != 2) {
					throw new Exception(
							"finishEditing on FIX_ERROR_PATH:  Attempted to finish a lead review requires exactly two record.");	
				}
				
				// set the record to ready for publication
				for (MapRecord mr : mapRecords) {
					
					// remove the REVIEW_NEEDED record
					if (mr.getWorkflowStatus().equals(WorkflowStatus.REVIEW_NEEDED)) {
						newRecords.remove(mr);
						
					// set the finished record to READY_FOR_PUBLICATION
					} else {
						mr.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
					}
				}
				
				
			}
			
			
			break;

		case LEGACY_PATH:
			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
					.info("LEGACY_PATH");
			break;
		case QA_PATH:
			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
					.info("QA_PATH");
			break;
		case CONSENSUS_PATH:
			Logger.getLogger(DefaultProjectSpecificAlgorithmHandler.class)
					.info("CONSENSUS_PATH");
			break;
		default:
			throw new Exception("finishEditing: Unexpected workflow path");
		}

		return newRecords;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#saveForLater
	 * (org.ihtsdo.otf.mapping.workflow.TrackingRecord,
	 * org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public Set<MapRecord> saveForLater(TrackingRecord trackingRecord,
			Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

		Set<MapRecord> newRecords = new HashSet<>(mapRecords);

		// find the record assigned to this user
		MapRecord mapRecord = null;
		for (MapRecord mr : newRecords) {
			// find using mapping service instead of workflow service?
			if (mr.getOwner().equals(mapUser)) {
				mapRecord = mr;
			}
		}
		if (mapRecord == null)
			throw new Exception(
					"finishEditing:  Record for user could not be found");

		switch (trackingRecord.getWorkflowPath()) {
		case CONSENSUS_PATH:
			break;
		case FIX_ERROR_PATH:
			if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.NEW))
				mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
			break;
		case LEGACY_PATH:
			break;
		case NON_LEGACY_PATH:
			if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.NEW))
				mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
			if (mapRecord.getWorkflowStatus().equals(
					WorkflowStatus.CONFLICT_NEW))
				mapRecord
						.setWorkflowStatus(WorkflowStatus.CONFLICT_IN_PROGRESS);
			break;
		case QA_PATH:
			break;
		default:
			break;

		}
		return newRecords;
	}

	/**
	 * Cancel work on a map record.
	 * 
	 * Only use-case is for the FIX_ERROR_PATH where a new map record has been
	 * assigned due to editing a published record.
	 *
	 * @param trackingRecord the tracking record
	 * @param mapRecords the map records
	 * @param mapUser the map user
	 * @return the sets the
	 * @throws Exception the exception
	 */
	@Override
	public Set<MapRecord> cancelWork(
			TrackingRecord trackingRecord,
			Set<MapRecord> mapRecords, MapUser mapUser) throws Exception {

		Set<MapRecord> newRecords = new HashSet<>(mapRecords);

		switch (trackingRecord.getWorkflowPath()) {

		case FIX_ERROR_PATH:

			MapRecord reviewRecord = null;
			MapRecord newRecord = null;

			// check for the appropriate map records
			for (MapRecord mr : mapRecords) {
				if (mr.getOwner().equals(mapUser)) {
					newRecord = mr;
				} else if (mr.getWorkflowStatus().equals(WorkflowStatus.REVISION)) {
					reviewRecord = mr;
				}
			}

			// check assumption: user has an assigned record
			if (newRecord == null)
				throw new Exception("Cancel work called for user "
						+ mapUser.getName() + " and "
						+ trackingRecord.getTerminology() + " concept "
						+ trackingRecord.getTerminologyId()
						+ ", but could not retrieve assigned record.");

			// check assumption: a REVIEW record exists
			if (reviewRecord == null)
				throw new Exception(
						"Cancel work called for user "
								+ mapUser.getName()
								+ " and "
								+ trackingRecord.getTerminology()
								+ " concept "
								+ trackingRecord.getTerminologyId()
								+ ", but could not retrieve the previously published or ready-for-publication record.");
			
			// perform action only if the user's record is NEW
			// if editing has occured (EDITING_IN_PROGRESS or above), null-op
			if (newRecord.getWorkflowStatus().equals(WorkflowStatus.NEW)) {

				// remove the user's map record
				newRecords.remove(newRecord);

				// set the workflow status of the REVIEW record back to its
				// original state
				reviewRecord = getPreviousVersionOfMapRecord(reviewRecord);
				
				// add the revised REVIEW record to new records
				newRecords.add(reviewRecord);
			}

			break;
		default:
			// do nothing
			break;
		}

		return newRecords;
	}

	/**
	 * Returns the previous version of map record.
	 *
	 * @param mapRecord the map record
	 * @return the previous version of map record
	 * @throws Exception the exception
	 */
	public MapRecord getPreviousVersionOfMapRecord(MapRecord mapRecord)
			throws Exception {

		MappingService mappingService = new MappingServiceJpa();
		List<MapRecord> revisions = mappingService.getMapRecordRevisions(
				mapRecord.getId()).getMapRecords();

		// check assumption: last revision exists, at least two records must be present
		if (revisions.size() < 2)
			throw new Exception(
					"Attempted to get the previous version of map record with id "
							+ mapRecord.getId() + ", "
							+ mapRecord.getOwner().getName()
							+ ", and concept id " + mapRecord.getConceptId()
							+ ", but no previous revisions exist.");
		
		for (MapRecord revision : revisions) {
			System.out.println(revision.toString());
		}

		// get the most recent revision (0 is the current record, 1 is the previous)
		return revisions.get(1);

	}

	/**
	 * Returns the workflow status.
	 *
	 * @param mapRecords the map records
	 * @return the workflow status
	 */
	public WorkflowStatus getWorkflowStatus(Set<MapRecord> mapRecords) {
		WorkflowStatus workflowStatus = WorkflowStatus.NEW;
		for (MapRecord mr : mapRecords) {
			System.out.println(mr.getWorkflowStatus());
			if (mr.getWorkflowStatus().compareTo(workflowStatus) > 0)
				workflowStatus = mr.getWorkflowStatus();
		}
		return workflowStatus;
	}

	/**
	 * Returns the lowest workflow status.
	 *
	 * @param mapRecords the map records
	 * @return the lowest workflow status
	 */
	public WorkflowStatus getLowestWorkflowStatus(Set<MapRecord> mapRecords) {
		WorkflowStatus workflowStatus = WorkflowStatus.REVISION;
		for (MapRecord mr : mapRecords) {
			if (mr.getWorkflowStatus().compareTo(workflowStatus) < 0)
				workflowStatus = mr.getWorkflowStatus();
		}
		return workflowStatus;
	}

	/**
	 * Returns the map users.
	 *
	 * @param mapRecords the map records
	 * @return the map users
	 */
	public Set<MapUser> getMapUsers(Set<MapRecord> mapRecords) {
		Set<MapUser> mapUsers = new HashSet<>();
		for (MapRecord mr : mapRecords) {
			mapUsers.add(mr.getOwner());
		}
		return mapUsers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
	 * computeTargetTerminologyNotes
	 * (org.ihtsdo.otf.mapping.helpers.TreePositionList)
	 */
	@Override
	public void computeTargetTerminologyNotes(TreePositionList treePositions)
			throws Exception {

		// DO NOTHING -- Override in project specific handlers if necessary
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#
	 * isRecordEditableByUser(org.ihtsdo.otf.mapping.model.MapRecord,
	 * org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public boolean isRecordEditableByUser(MapRecord mapRecord, MapUser mapUser)
			throws Exception {

		// check that this user is on this project
		if (!mapProject.getMapSpecialists().contains(mapUser)
				&& !mapProject.getMapLeads().contains(mapUser)) {
			return false;
		}

		switch (mapRecord.getWorkflowStatus()) {

		// neither lead nor specialist can modify a CONFLICT_DETECTED record
		case CONFLICT_DETECTED:
			return false;

			// the following cases can only be edited by an owner who is a lead
			// for this project
		case CONFLICT_IN_PROGRESS:
		case CONFLICT_NEW:
			if (mapRecord.getOwner().equals(mapUser)
					&& mapProject.getMapLeads().contains(mapUser))
				return true;
			else
				return false;
			
		case REVIEW_NEEDED:
			return false;
			
		// review editing can only be done by the lead who owns this record
		case REVIEW_NEW:
		case REVIEW_IN_PROGRESS:
			if (mapRecord.getOwner().equals(mapUser)
					&& mapProject.getMapLeads().contains(mapUser))
				return true;
			else
				return false;

			// consensus record handling - Phase 2
		case CONSENSUS_NEEDED:
		case CONSENSUS_NEW:
		case CONSENSUS_IN_PROGRESS:
			return false;

			// initial editing stages can be edited only by owner
		case EDITING_DONE:
		case EDITING_IN_PROGRESS:
		case NEW:
			if (mapRecord.getOwner().equals(mapUser))
				return true;
			else
				return false;

			// published and ready_for_publication records are available to
			// either specialists or leads
		case PUBLISHED:
		case READY_FOR_PUBLICATION:
			return true;

			// review records are not editable
		case REVISION:
			return false;

			// if a non-specified case, throw error
		default:
			throw new Exception("Invalid Workflow Status "
					+ mapRecord.getWorkflowStatus().toString()
					+ " when checking editable for map record");

		}

	}

}
