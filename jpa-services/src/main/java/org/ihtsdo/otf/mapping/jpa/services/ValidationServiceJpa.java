package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.ValidationService;

/**
 * Reference implementation of the validation service
 */
@SuppressWarnings("static-method")
public class ValidationServiceJpa implements ValidationService {

	/**
	 * Instantiates an empty {@link ValidationServiceJpa}.
	 */
	public ValidationServiceJpa() { }
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.ValidationService#validateMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public ValidationResult validateMapRecord(MapRecord mapRecord) throws Exception {
		
		ValidationResult validationResult = new ValidationResultJpa();
		
		// retrieve the map project
		MappingService mappingService = new MappingServiceJpa();
		MapProject mapProject = mappingService.getMapProject(mapRecord.getMapProjectId());
		mappingService.close();
		
		
		Map<Integer, List<MapEntry>> entryGroups = getEntryGroups(mapRecord);
		
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
		
		/*
		Group validation checks
		
		•	Verify the last entry in a group is a TRUE rule
		•	Verify higher map groups do not have only NC nodes
		
		*/	
		// Validation Check:  verify correct positioning of TRUE rules
		validationResult.addErrors(checkMapRecordTrueRules(mapRecord, mapProject, entryGroups));
		
		// Validation Check:  very higher map groups do not have only NC nodes
		validationResult.addErrors(checkMapRecordNcNodes(mapRecord, mapProject, entryGroups));
		
		/*
		Entry Validation Checks
		•	Verify no duplicate entries in record
		•	Verify advice values are valid for the project (this can happen if “allowable map advice” changes without updating map entries)
		•	Entry must have target code that is both in the target terminology and valid (e.g. leaf nodes) OR have a relationId corresponding to a valid map category
		*/	
		
		// Validation Check: verify entries are not duplicated
		validationResult.addErrors(checkMapRecordForDuplicateEntries(mapRecord, mapProject, entryGroups));	
		
		// Validation Check: verify advice values are valid for the project (this can happen if “allowable map advice” changes without updating map entries)
		validationResult.addErrors(checkMapRecordAdvices(mapRecord, mapProject, entryGroups));
		
		// Validation Check: very that map entry targets OR relationIds are valid
		validationResult.addErrors(checkMapRecordTargets(mapRecord, mapProject, entryGroups));
		
		return validationResult;
	}
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
  public Set<String> checkMapRecordForDuplicateEntries(MapRecord mapRecord, MapProject mapProject, Map<Integer, List<MapEntry>> entryGroups) {
	
		Logger.getLogger(MappingServiceJpa.class).info("  Checking map record for duplicate entries within map groups...");
		
		Set<String> messages = new HashSet<>();
		List<MapEntry> entries = mapRecord.getMapEntries();
		
		// cycle over all entries but last
		for (int i = 0 ; i < entries.size() - 1; i++) {
		
			// cycle over all entries after this one
			for (int j = i+1; j < entries.size(); j++) {
			
			// compare the two entries
			if (entries.get(i).getTargetId().equals(entries.get(j).getTargetId())
					&& entries.get(i).getRelationId().equals(entries.get(j).getRelationId()) 
					&& entries.get(i).getRule().equals(entries.get(j).getRule()) ) {
			
					messages.add("Duplicate entries found: " + 
					"Group " + Integer.toString(entries.get(i).getMapGroup()) + ", priority " + Integer.toString(i) + 
					" and " + 
					"Group " + Integer.toString(entries.get(j).getMapGroup()) + ", priority " + Integer.toString(j));
				}
			}
		}
		
		for (String message : messages) {
			Logger.getLogger(MappingServiceJpa.class).info("    " + message);
		}
		
		return messages;
	}
	
	/**
	 * Function to check proper use of TRUE rules.
	 *
	 * @param mapRecord the map record
	 * @param mapProject the map project for this record
	 * @param entryGroups the binned entry lists by group
	 * @return a list of errors detected
	 */
	public Set<String> checkMapRecordTrueRules(MapRecord mapRecord, MapProject mapProject, Map<Integer, List<MapEntry>> entryGroups) {
	
	Logger.getLogger(MappingServiceJpa.class).info("  Checking map record for proper use of TRUE rules...");
	
	Set<String> messages = new HashSet<>();
	
	for (Integer key : entryGroups.keySet()) {
	
		for (MapEntry mapEntry : entryGroups.get(key)) {
		
			Logger.getLogger(MappingServiceJpa.class).info("    Checking entry " + Integer.toString(mapEntry.getMapPriority()));

			// add message if TRUE rule found at non-terminating entry
			if (mapEntry.getMapPriority() != entryGroups.get(key).size() && mapEntry.getRule().equals("TRUE")) {
				messages.add("Found non-terminating entry with TRUE rule."
				+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
				+    " map priority " + Integer.toString(mapEntry.getMapPriority()));
			
			// add message if terminating entry rule is not TRUE
			} else if (mapEntry.getMapPriority() == entryGroups.get(key).size() && !mapEntry.getRule().equals("TRUE")) {
				messages.add("Terminating entry has non-TRUE rule."
				+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
				+    " map priority " + Integer.toString(mapEntry.getMapPriority()));
			}
		}
	}
	for (String message : messages) {
	Logger.getLogger(MappingServiceJpa.class).info("    " + message);
	}
	
	return messages;
	}
	
	/**
	 * Function to check higher level groups do not have only NC target codes.
	 *
	 * @param mapRecord the map record
	 * @param mapProject the map project for this record
	 * @param entryGroups the binned entry lists by group
	 * @return a list of errors detected
	 */
	public Set<String> checkMapRecordNcNodes(MapRecord mapRecord, MapProject mapProject, Map<Integer, List<MapEntry>> entryGroups) {
	
		Logger.getLogger(MappingServiceJpa.class).info("  Checking map record for high-level groups with only NC target codes...");
		
		Set<String> messages = new HashSet<>();
		
		// if only one group, ignore
		if (entryGroups.keySet().size() == 1) return messages;
		
			// otherwise cycle over the high-level groups (i.e. all but first group)
			for (int group : entryGroups.keySet()) {
			
				if (group != 1) {
				List<MapEntry> entries = entryGroups.get(group);
				
				boolean isValidGroup = false;
				for (MapEntry entry : entries) {
				if (entry.getTargetId() != null && entry.getTargetId() != "") isValidGroup = true;
				}
			
				if (!isValidGroup) {
				messages.add("High-level group " + Integer.toString(group) + " has no entries with targets");
				}
			}	
		}
		
		
		for (String message : messages) {
			Logger.getLogger(MappingServiceJpa.class).info("    " + message);
		}
		
		return messages;
	}
	
	/**
	 * Function to check that all advices attached are allowable by the project.
	 *
	 * @param mapRecord the map record
	 * @param mapProject the map project for this record
	 * @param entryGroups the binned entry lists by group
	 * @return a list of errors detected
	 */
	public Set<String> checkMapRecordAdvices(MapRecord mapRecord, MapProject mapProject, Map<Integer, List<MapEntry>> entryGroups) {
	
		Logger.getLogger(MappingServiceJpa.class).info("  Checking map record for valid map advices...");
		
		Set<String> messages = new HashSet<>();
		
		for (MapEntry mapEntry : mapRecord.getMapEntries()) {
		
			for (MapAdvice mapAdvice : mapEntry.getMapAdvices()) {
			
				if (!mapProject.getMapAdvices().contains(mapAdvice)) {
					messages.add("Invalid advice " + mapAdvice.getName() + "."
					+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
					+    " map priority " + Integer.toString(mapEntry.getMapPriority()));
				}
			}
			
		}
		
		for (String message : messages) {
			Logger.getLogger(MappingServiceJpa.class).info("    " + message);
		}
		
		return messages;
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
	public Set<String> checkMapRecordTargets(MapRecord mapRecord, MapProject mapProject, Map<Integer, List<MapEntry>> entryGroups) {
	
		Logger.getLogger(MappingServiceJpa.class).info("  Checking map record for valid targets...");
		
		Set<String> messages = new HashSet<>();
		
		ContentService contentService = new ContentServiceJpa();
		
		String terminology = mapProject.getDestinationTerminology();
		String terminologyVersion = mapProject.getDestinationTerminologyVersion();
		
		for (MapEntry mapEntry : mapRecord.getMapEntries()) {
			
			// if this entry has a target assigned
			if (mapEntry.getTargetId() != null && !mapEntry.getTargetId().equals("")) {
			
			// retrieve the target concept
			try {
				Concept concept = contentService.getConcept(mapEntry.getTargetId(), terminology, terminologyVersion);
				
				if (concept == null) {
					messages.add("Target concept (" + terminology + ", " + terminologyVersion + ", " + mapEntry.getTargetId() + ")"
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
						
						messages.add("Target concept (" + terminology + ", " + terminologyVersion + ", " + mapEntry.getTargetId() + ")"
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
			} else if (mapEntry.getRelationId() != null && !mapEntry.getRelationId().equals("")) {
			
			// TODO: Insert check here
			} else {
			messages.add("Entry has no target or relation id."
			+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
			+    " map priority " + Integer.toString(mapEntry.getMapPriority()));
			
			}
			
		}
		
		for (String message : messages) {
			Logger.getLogger(MappingServiceJpa.class).info("    " + message);
		}
		
		return messages;
	}
	
	/**
	 * Helper function to sort a records entries into entry lists binned by group.
	 *
	 * @param mapRecord the map record
	 * @return a map of group->entry list
	 */
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
	
	
	
	
	
	
}
	