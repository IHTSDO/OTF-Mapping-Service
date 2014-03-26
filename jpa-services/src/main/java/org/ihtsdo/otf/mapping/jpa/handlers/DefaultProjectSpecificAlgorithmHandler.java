package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
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
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#computeMapAdviceAndMapRelations(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	/**
	 * This must be overwritten for each project specific handler
	 * @param mapRecord
	 * @return
	 */
	public ValidationResult computeMapAdviceAndMapRelations(MapRecord mapRecord) {
		return new ValidationResultJpa();
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
		validationResult.merge(checkMapRecordTargets(mapRecord, entryGroups));
		
		
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

		// if only one group, ignore
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
	 * Check map record's entries for EITHER:
	 * 1) Valid map targets
	 *    - check target exists (i.e. in database)
	 *    - check valid position (i.e. must be leaf node)
	 * 	2) Entry with no target has valid relationId
	 * 
	 * @param mapRecord the map record
	 * @param mapProject the map project
	 * @param entryGroups the binned entry groups
	 * @return the list of error messages
	 */
	public ValidationResult checkMapRecordTargets(MapRecord mapRecord, Map<Integer, List<MapEntry>> entryGroups) {

		Logger.getLogger(MappingServiceJpa.class).info("  Checking map record for valid targets...");

		ValidationResult validationResult = new ValidationResultJpa();

		ContentService contentService = new ContentServiceJpa();

		String terminology = mapProject.getDestinationTerminology();
		String terminologyVersion = mapProject.getDestinationTerminologyVersion();

		for (MapEntry mapEntry : mapRecord.getMapEntries()) {

			// if this entry has a target assigned
			if (mapEntry.getTargetId() != null && !mapEntry.getTargetId().equals("")) {

				// first, check the terminology id
				if (!mapEntry.getTargetId().matches("[*d*d*d*]")) {
					validationResult.addError("Target code must contain at least three digits!"
							+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
							+    " map priority " + Integer.toString(mapEntry.getMapPriority()));
				}

				if (mapEntry.getTargetId().contains("-")) {
					validationResult.addError("Target code cannot contain a dash (-) character!"
							+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
							+    " map priority " + Integer.toString(mapEntry.getMapPriority()));

				}

				// if terminology id meets correct format, retrieve the target concept
				try {
					Concept concept = contentService.getConcept(mapEntry.getTargetId(), terminology, terminologyVersion);

					if (concept == null) {
						validationResult.addError("Target concept (" + terminology + ", " + terminologyVersion + ", " + mapEntry.getTargetId() + ")"
								+    " not in database!"
								+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
								+    " map priority " + Integer.toString(mapEntry.getMapPriority()));
					} else {

						MetadataService metadataService = new MetadataServiceJpa();
						Map<Long, String> hierarchicalRelationshipTypeMap = metadataService.getHierarchicalRelationshipTypes(terminology, terminologyVersion);
						Long hierarchicalRelationshipType =
								hierarchicalRelationshipTypeMap.entrySet().iterator().next().getKey();

						// check this concept is a leaf node
						if(contentService.getDescendants(
								mapEntry.getTargetId(), 
								mapProject.getDestinationTerminology(), 
								mapProject.getDestinationTerminologyVersion(),
								hierarchicalRelationshipType).size() > 0) {

							validationResult.addError("Target concept (" + terminology + ", " + terminologyVersion + ", " + mapEntry.getTargetId() + ")"
									+    " is not a leaf node!"
									+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
									+    " map priority " + Integer.toString(mapEntry.getMapPriority()));
						}
					}


				} catch (Exception e) {
					Logger.getLogger(MappingServiceJpa.class).info(
							"    Unexpected error while validating map target for "
									+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
									+    " map priority " + Integer.toString(mapEntry.getMapPriority()));
				}

				// if no assigned target, check relation id
			} else if (mapEntry.getMapRelation() != null) {

			} else {
				validationResult.addError("Entry is blank (no target code or relation category)"
						+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
						+    " map priority " + Integer.toString(mapEntry.getMapPriority()));

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
