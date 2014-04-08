package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;

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
	 * @return computed map relation
	 */
	@Override
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

	
	
	/**
	 * Perform universal validation checks.
	 *
	 * @param mapRecord the map record
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
	 * @return a list of errors detected
	 */
	@SuppressWarnings("static-method")
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
	 * @param entryGroups the binned entry lists by group
	 * @return a list of errors detected
	 */
	@SuppressWarnings("static-method")
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

	@Override
	public ValidationResult compareMapRecords(MapRecord record1,
		MapRecord record2) {
		ValidationResult validationResult = new ValidationResultJpa();
		
		// compare mapProjectId
		if (record1.getMapProjectId() != record2.getMapProjectId())
			validationResult.addError("Map Project Ids don't match! " + 
					record1.getMapProjectId() + " " + record2.getMapProjectId());
		
		// compare conceptId
		if (record1.getConceptId() != record2.getConceptId())
			validationResult.addError("Concept Ids don't match! " +
					record1.getConceptId() + " " + record2.getConceptId());
		
	   // compare mapPrinciples
	   Comparator principlesComparator = new Comparator() {
	      public int compare(Object o1, Object o2) {

	          String x1 = ((MapPrinciple) o1).getPrincipleId();
	          String x2 = ((MapPrinciple) o2).getPrincipleId();

	          if (!x1.equals(x2)) {
	              return x1.compareTo(x2);
	          } 
	          return 0;
	      }
	   };
	   List<MapPrinciple> principles1 = new ArrayList<>(record1.getMapPrinciples());	   
	   Collections.sort(principles1, principlesComparator);
	   List<MapPrinciple> principles2 = new ArrayList<>(record2.getMapPrinciples());	   
	   Collections.sort(principles2, principlesComparator);
	   
	   if (principles1.size() != principles2.size())
				validationResult.addWarning("Map Principles count doesn't match! " +
						principles1.toString() + " " + principles2.toString());
	   else {
	  	 for (int i=0; i<principles1.size(); i++) {
	  		 if (!principles1.get(i).getPrincipleId().equals(principles2.get(i).getPrincipleId()))
	  			 validationResult.addWarning("Map Principles content doesn't match! " +
	  					 principles1.toString() + " " + principles2.toString());
	  	 }
	   }	   
	   
	   // check force map lead review flag
	   if (record1.isFlagForMapLeadReview()) {
	  	 validationResult.addError("Specialist 1 indicated the need for map lead review.");
	   }
	   if (record2.isFlagForMapLeadReview()) {
	  	 validationResult.addError("Specialist 2 indicated the need for map lead review.");
	   } 
	  	 
	   // check consensus review flag
	   if (record1.isFlagForConsensusReview()) {
	  	 validationResult.addError("Specialist 1 indicated consensus review is required.");
	   }
	   if (record2.isFlagForConsensusReview()) {
	  	 validationResult.addError("Specialist 2 indicated consensus review is required.");
	   }
	   
	   // compare mapEntries
	   // organize map entries by group
	   Map<Integer, List<MapEntry>> groupToMapEntryList1 = new HashMap<Integer, List<MapEntry>>();
	   for (MapEntry entry : record1.getMapEntries()) {
	  	 if (groupToMapEntryList1.containsKey(entry.getMapGroup())) {
	  	   List<MapEntry> entryList = groupToMapEntryList1.get(entry.getMapGroup());
	  	   entryList.add(entry);
	  	 } else {
	  		 List<MapEntry> entryList = new ArrayList<>();
	  		 entryList.add(entry);
	  		 groupToMapEntryList1.put(entry.getMapGroup(), entryList);
	  	 }
	   }
	   Map<Integer, List<MapEntry>> groupToMapEntryList2 = new HashMap<Integer, List<MapEntry>>();
	   for (MapEntry entry : record2.getMapEntries()) {
	  	 if (groupToMapEntryList2.containsKey(entry.getMapGroup())) {
	  	   List<MapEntry> entryList = groupToMapEntryList2.get(entry.getMapGroup());
	  	   entryList.add(entry);
	  	 } else {
	  		 List<MapEntry> entryList = new ArrayList<>();
	  		 entryList.add(entry);
	  		 groupToMapEntryList2.put(entry.getMapGroup(), entryList);
	  	 }
	   }
	   
	   // for each group
	   for (int i=1; i< Math.max(groupToMapEntryList1.size(), groupToMapEntryList2.size()) + 1; i++) {
	  	 List<MapEntry> entries1 = groupToMapEntryList1.get(new Integer(i));
	  	 List<MapEntry> entries2 = groupToMapEntryList2.get(new Integer(i));
	  	 
	  	 // error if different numbers of entries
	  	 if (entries1 == null) {
	  		 validationResult.addError("Record 1 Group " + i + " has no entries!");
	  		 continue;
	  	 } else if (entries2 == null) {
	  		 validationResult.addError("Record 2 Group " + i + " has no entries!");
	  		 continue;
		   } else if (entries1.size() != entries2.size()) {
	  		 validationResult.addError("Groups have different entry counts!  Record 1, Group " + i + ":" + entries1.size() +
	  				 " Record 2, Group " + i + ":" + entries2.size());
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
	  	 for (int d=0; d<Math.max(stringEntries1.size(), stringEntries2.size()); d++) {
	  		 if (stringEntries1.get(d) == null || stringEntries2.get(d) == null) {
	  			 // already reported differing number of entries
	  			 break;
	  		 }
	  		 if (stringEntries1.get(d).equals(stringEntries2.get(d)))
	  			 continue;
	  		 else if (stringEntries2.contains(stringEntries1.get(d))) 
	  			 outOfOrderFlag = true;
	  		 else
	  			 missingEntry = true;
	  	 }
	  	 if (!outOfOrderFlag && !missingEntry) {
	  		 continue; //to next group for comparison
	  	 }
	  	 if (outOfOrderFlag && !missingEntry) {
	  		 validationResult.addWarning("Group " + i + " has all the same entries but in different orders.");
	  	   continue; // to next group for comparison
	  	 }
	  	 
	  	 // check for details of missing entries
	  	 boolean matchFound = false;
	  	 for (int d=0; d<entries1.size(); d++) {
	  	   for (int f=0; f<entries2.size(); f++) {
	  	  	 if(entries1.get(d).getRule().equals(entries2.get(f).getRule()) &&
	  	  			 entries1.get(d).getTargetId().equals(entries2.get(f).getTargetId()) &&
	  	  			 !entries1.get(d).getMapRelation().getId().equals(entries2.get(f).getMapRelation().getId()))
	  	  		 matchFound = true;
	  	   }	  
		  	 if (matchFound) {
		  		 validationResult.addError("Record " + convertToString(entries1.get(d)) + " matches an entry from record 2 on rule and target code but not on relation id.");	 
		  	 }
		  	 matchFound = false;
	     }
	  	 for (int d=0; d<entries1.size(); d++) {
	  	   for (int f=0; f<entries2.size(); f++) {
	  	  	 if(entries1.get(d).getRule().equals(entries2.get(f).getRule()) &&
	  	  			 entries1.get(d).getTargetId().equals(entries2.get(f).getTargetId()) &&
	  	  			 !entries1.get(d).getMapAdvices().equals(entries2.get(f).getMapAdvices()))
	  	  		 matchFound = true;
	  	   }	  
		  	 if (matchFound) {
		  		 validationResult.addError("Record " + convertToString(entries1.get(d)) + " matches an entry from record 2 on rule and target code but not on advice.");	 
		  	 }
		  	 matchFound = false;
	     }
	  	 for (int d=0; d<entries1.size(); d++) {
	  	   for (int f=0; f<entries2.size(); f++) {
	  	  	 if(entries1.get(d).getRule().equals(entries2.get(f).getRule()) &&
	  	  			 !entries1.get(d).getTargetId().equals(entries2.get(f).getTargetId()))
	  	  		 matchFound = true;
	  	   }	  
		  	 if (matchFound) {
		  		 validationResult.addError("Record " + convertToString(entries1.get(d)) + " matches an entry from record 2 on rule but not on target code.");	 
		  	 }
		  	 matchFound = false;
	     }	   
	  	 for (int d=0; d<entries1.size(); d++) {
	  	   for (int f=0; f<entries2.size(); f++) {
	  	  	 if(entries1.get(d).getRule().equals(entries2.get(f).getRule()))
	  	  		 matchFound = true;
	  	   }	  
		  	 if (!matchFound) {
		  		 validationResult.addError("Record " + convertToString(entries1.get(d)) + " does not match any entry from record 2 on rule.");	 
		  	 }
		  	 matchFound = false;
	     }	   
	   
	   }	   
	   
	   // TODO: Do we do this here?
	   if (validationResult.getErrors().size() > 0) {
	     record1.setWorkflowStatus(WorkflowStatus.CONFLICT_DETECTED);
	     record2.setWorkflowStatus(WorkflowStatus.CONFLICT_DETECTED);
	   }

		return validationResult;
	}
	
	private String convertToString(MapEntry mapEntry) {
		
		// check map advices
	  Comparator advicesComparator = new Comparator() {
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
    sb.append(mapEntry.getTargetId() + " " + mapEntry.getRule() + " " + mapEntry.getMapRelation().getId());
    for (MapAdvice mapAdvice : advices) {
   	  sb.append(mapAdvice.getObjectId() + " ");
    }
  
		return sb.toString();
	}

	/**
	 * For default project, all target codes are considered valid.
	 * @throws Exception 
	 */
	@Override
	public boolean isTargetCodeValid(String terminologyId) throws Exception {
		return true;
	}
}
