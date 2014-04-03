package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

// TODO: Auto-generated Javadoc
/**
 * The Class DefaultProjectSpecificAlgorithmHandler.
 */
public class DefaultProjectSpecificAlgorithmHandler implements ProjectSpecificAlgorithmHandler {

	/** The map project. */
	MapProject mapProject = null;
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#getMapProject()
	 */
	@Override
	public MapProject getMapProject() {
		return this.mapProject;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#setMapProject(org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public void setMapProject(MapProject mapProject) {
		this.mapProject = mapProject;		
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#isMapAdviceComputable(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public boolean isMapAdviceComputable(MapRecord mapRecord) {
		if (mapProject != null) {
			for (MapAdvice mapAdvice : mapProject.getMapAdvices()) {
				if (mapAdvice.isComputed() == true) return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#isMapRelationComputable(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public boolean isMapRelationComputable(MapRecord mapRecord) {
		if (mapProject != null) {
			for (MapRelation mapRelation : mapProject.getMapRelations()) {
				if (mapRelation.isComputed() == true) return true;
			}
		}
		return false;
	}
	
	@Override
	/**
	 * Given a map record and a map entry, returns any computed advice.
	 * This must be overwritten for each project specific handler.
	 * @param mapRecord
	 * @return
	 */
	public List<MapAdvice> computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry) {
		return null;
	}
	
	/**
	 * Given a map record and a map entry, returns the computed map relation (if applicable)
	 * This must be overwritten for each project specific handler.
	 * @param mapRecord
	 * @return
	 */
	public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#validateRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public ValidationResult validateRecord(MapRecord mapRecord)
			throws Exception {

		ValidationResult validationResult = new ValidationResultJpa();
		
		validationResult.merge(performUniversalValidationChecks(mapRecord));
		validationResult.merge(validateTargetCodes(mapRecord));
		
		return validationResult;
	}
	

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#validateTargetCodes(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	/**
	 * This must be overwritten for each project specific handler
	 */
	public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception {
		return new ValidationResultJpa();
	}

	
	
	public ValidationResult performUniversalValidationChecks(MapRecord mapRecord) {
		Map<Integer, List<MapEntry>> entryGroups = getEntryGroups(mapRecord);
		
		ValidationResult validationResult = new ValidationResultJpa();
		
		// FATAL ERROR: map record has no entries
		if (mapRecord.getMapEntries().size() == 0) {
			validationResult.addError("Map record has no entries");
			return validationResult;
		}
		
		// FATAL ERROR: multiple map groups present for a project without group structure
		if (!mapProject.isGroupStructure() && entryGroups.keySet().size() > 1) {
			validationResult.addError("Project has no group structure but multiple map groups were found.");
			return validationResult;
		}
		
		
	   /*	Group validation checks
			•	Verify the last entry in a group is a TRUE rule
			•	Verify higher map groups do not have only NC nodes
		*/
			
		// Validation Check:  verify correct positioning of TRUE rules
		validationResult.merge(checkMapRecordTrueRules(mapRecord, entryGroups));
		
		// Validation Check:  very higher map groups do not have only NC nodes
		validationResult.merge(checkMapRecordNcNodes(mapRecord, entryGroups));
		
		
		/*  Entry Validation Checks
			•	Verify no duplicate entries in record
			•	Verify advice values are valid for the project (this can happen if “allowable map advice” changes without updating map entries)
			•	Entry must have target code that is both in the target terminology and valid (e.g. leaf nodes) OR have a relationId corresponding to a valid map category
		*/
		
		// Validation Check: verify entries are not duplicated
		validationResult.merge(checkMapRecordForDuplicateEntries(mapRecord));	
		
		// Validation Check: verify advice values are valid for the project (this can happen if “allowable map advice” changes without updating map entries)
		validationResult.merge(checkMapRecordAdvices(mapRecord, entryGroups));
		
		// Validation Check: very that map entry targets OR relationIds are valid
	/*	validationResult.merge(checkMapRecordTargets(mapRecord, entryGroups));
		*/
		
		return validationResult;
	}

	
	//////////////////////
	// HELPER FUNCTIONS //
	//////////////////////
	
	/////////////////////////////////////////////////////////
	// Map Record Validation Checks and Helper Functions
	/////////////////////////////////////////////////////////

	/**
	 * Function to check a map record for duplicate entries within map groups.
	 *
	 * @param mapRecord the map record
	 * @param mapProject the map project for this record
	 * @param entryGroups the binned entry lists by group
	 * @return a list of errors detected
	 */
	public ValidationResult checkMapRecordForDuplicateEntries(MapRecord mapRecord) {

		Logger.getLogger(MappingServiceJpa.class).info("  Checking map record for duplicate entries within map groups...");

		ValidationResult validationResult = new ValidationResultJpa();
		List<MapEntry> entries = mapRecord.getMapEntries();

		// cycle over all entries but last
		for (int i = 0 ; i < entries.size() - 1; i++) {

			// cycle over all entries after this one
			for (int j = i+1; j < entries.size(); j++) {

				// compare the two entries
				if (entries.get(i).getTargetId().equals(entries.get(j).getTargetId())
						&& entries.get(i).getMapRelation().equals(entries.get(j).getMapRelation()) 
						&& entries.get(i).getRule().equals(entries.get(j).getRule()) ) {

					validationResult.addError("Duplicate entries found: " + 
							"Group " + Integer.toString(entries.get(i).getMapGroup()) + ", priority " + Integer.toString(i) + 
							" and " + 
							"Group " + Integer.toString(entries.get(j).getMapGroup()) + ", priority " + Integer.toString(j));
				}
			}
		}

		for (String error : validationResult.getErrors()) {
			Logger.getLogger(MappingServiceJpa.class).info("    " + error);
		}

		return validationResult;
	}

	/**
	 * Function to check proper use of TRUE rules.
	 *
	 * @param mapRecord the map record
	 * @param mapProject the map project for this record
	 * @param entryGroups the binned entry lists by group
	 * @return a list of errors detected
	 */
	public ValidationResult checkMapRecordTrueRules(MapRecord mapRecord, Map<Integer, List<MapEntry>> entryGroups) {

		Logger.getLogger(MappingServiceJpa.class).info("  Checking map record for proper use of TRUE rules...");

		ValidationResult validationResult = new ValidationResultJpa();

		// if not rule based, return empty validation result
		if (mapProject.isRuleBased() == false) return validationResult;
		
		// cycle over the groups
		for (Integer key : entryGroups.keySet()) {

			for (MapEntry mapEntry : entryGroups.get(key)) {

				Logger.getLogger(MappingServiceJpa.class).info("    Checking entry " + Integer.toString(mapEntry.getMapPriority()));

				// add message if TRUE rule found at non-terminating entry
				if (mapEntry.getMapPriority() != entryGroups.get(key).size() && mapEntry.getRule().equals("TRUE")) {
					validationResult.addError("Found non-terminating entry with TRUE rule."
							+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
							+    " map priority " + Integer.toString(mapEntry.getMapPriority()));

					// add message if terminating entry rule is not TRUE
				} else if (mapEntry.getMapPriority() == entryGroups.get(key).size() && !mapEntry.getRule().equals("TRUE")) {
					validationResult.addError("Terminating entry has non-TRUE rule."
							+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
							+    " map priority " + Integer.toString(mapEntry.getMapPriority()));
				}
			}
		}
		for (String error : validationResult.getErrors()) {
			Logger.getLogger(MappingServiceJpa.class).info("    " + error);
		}

		return validationResult;
	}

	/**
	 * Function to check higher level groups do not have only NC target codes.
	 *
	 * @param mapRecord the map record
	 * @param mapProject the map project for this record
	 * @param entryGroups the binned entry lists by group
	 * @return a list of errors detected
	 */
	public ValidationResult checkMapRecordNcNodes(MapRecord mapRecord, Map<Integer, List<MapEntry>> entryGroups) {

		Logger.getLogger(MappingServiceJpa.class).info("  Checking map record for high-level groups with only NC target codes...");

		ValidationResult validationResult = new ValidationResultJpa();

		// if only one group, return empty validation result (also covers non-group-structure projects)
		if (entryGroups.keySet().size() == 1) return validationResult;

		// otherwise cycle over the high-level groups (i.e. all but first group)
		for (int group : entryGroups.keySet()) {

			if (group != 1) {
				List<MapEntry> entries = entryGroups.get(group);

				boolean isValidGroup = false;
				for (MapEntry entry : entries) {
					if (entry.getTargetId() != null && entry.getTargetId() != "") isValidGroup = true;
				}

				if (!isValidGroup) {
					validationResult.addError("High-level group " + Integer.toString(group) + " has no entries with targets");
				}
			}	
		}


		for (String error : validationResult.getErrors()) {
			Logger.getLogger(MappingServiceJpa.class).info("    " + error);
		}

		return validationResult;
	}

	/**
	 * Function to check that all advices attached are allowable by the project.
	 *
	 * @param mapRecord the map record
	 * @param mapProject the map project for this record
	 * @param entryGroups the binned entry lists by group
	 * @return a list of errors detected
	 */
	public ValidationResult checkMapRecordAdvices(MapRecord mapRecord, Map<Integer, List<MapEntry>> entryGroups) {

		Logger.getLogger(MappingServiceJpa.class).info("  Checking map record for valid map advices...");

		ValidationResult validationResult = new ValidationResultJpa();

		for (MapEntry mapEntry : mapRecord.getMapEntries()) {

			for (MapAdvice mapAdvice : mapEntry.getMapAdvices()) {

				if (!mapProject.getMapAdvices().contains(mapAdvice)) {
					validationResult.addError("Invalid advice " + mapAdvice.getName() + "."
							+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
							+    " map priority " + Integer.toString(mapEntry.getMapPriority()));
				}
			}

		}

		for (String error : validationResult.getErrors()) {
			Logger.getLogger(MappingServiceJpa.class).info("    " + error);
		}

		return validationResult;
	}


	/**
	 * Helper function to sort a records entries into entry lists binned by group.
	 *
	 * @param mapRecord the map record
	 * @return a map of group->entry list
	 */
	public Map<Integer, List<MapEntry>> getEntryGroups(MapRecord mapRecord) {

		Map<Integer, List<MapEntry>> entryGroups = new HashMap<Integer, List<MapEntry>>();

		for (MapEntry entry : mapRecord.getMapEntries()) {

			// if no existing set for this group, create a blank set
			List<MapEntry> entryGroup = entryGroups.get(entry.getMapGroup());
			if (entryGroup == null) {
				entryGroup = new ArrayList<MapEntry>();
			} 

			// add this entry to group and put it in group map
			entryGroup.add(entry);
			entryGroups.put(entry.getMapGroup(), entryGroup);
		}

		return entryGroups;
	}
}
