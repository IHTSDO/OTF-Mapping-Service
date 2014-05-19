package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * The Class ICD10ProjectSpecificAlgorithmHandler.
 */
public class ICD10ProjectSpecificAlgorithmHandler extends
    DefaultProjectSpecificAlgorithmHandler {

  /**
   * For ICD10, a target code is valid if: - Concept exists - Concept has at
   * least 3 characters - The second character is a number (e.g. XVII is
   * invalid, but B10 is) - Concept does not contain a dash (-) character
   * @param mapRecord
   * @return the validation result
   * @throws Exception
   */
	@Override
	public ValidationResult validateTargetCodes(MapRecord mapRecord)
			throws Exception {

		Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
				"Validating target codes");

		ValidationResult validationResult = new ValidationResultJpa();
		ContentService contentService = new ContentServiceJpa();

		for (MapEntry mapEntry : mapRecord.getMapEntries()) {

			// check the target codee only if this entry does not has a map relation specified
			if (mapEntry.getMapRelation() == null ) {
				
				// first, check terminology id based on above rules
				if ( !mapEntry.getTargetId().equals("") && (!mapEntry.getTargetId().matches(".[0-9].*")
						|| mapEntry.getTargetId().contains("-"))) {
					validationResult
					.addError("Invalid target code "
							+ mapEntry.getTargetId()
							+ "!  For ICD10, valid target codes must contain 3 digits and must not contain a dash."
							+ " Entry:"
							+ (mapProject.isGroupStructure() ? " group "
									+ Integer.toString(mapEntry.getMapGroup()) + "," : "")
									+ " map priority "
									+ Integer.toString(mapEntry.getMapPriority()));
					
					// second, verify concept exists
				  Concept concept =
						contentService.getConcept(mapEntry.getTargetId(),
								mapProject.getDestinationTerminology(),
								mapProject.getDestinationTerminologyVersion());

				  if (concept == null) {
					  validationResult.addError("Target code "
							+ mapEntry.getTargetId()
							+ " not found in database!"
							+ " Entry:"
							+ (mapProject.isGroupStructure() ? " group "
									+ Integer.toString(mapEntry.getMapGroup()) + "," : "")
									+ " map  priority " + Integer.toString(mapEntry.getMapPriority()));

				  }
				} else if (mapEntry.getTargetId() == null || mapEntry.getTargetId().equals("")) {
					validationResult.addError("A reason must be selected from the picklist when no target is assigned.");
				}


			}
		}

    contentService.close();
    return validationResult;

  }

  /**
   * Computes the map relation for the SNOMEDCT->ICD10 map project. Based solely
   * on whether an entry has a TRUE rule or not. No advices are computed for
   * this project.
   */
  @Override
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry) {

    // if entry has no target
    if (mapEntry.getTargetId() == null || mapEntry.getTargetId().isEmpty()) {
      return null;
    }

    // if rule is not set, return null
    if (mapEntry.getRule() == null || mapEntry.getRule().isEmpty()) {
      return null;
    }

    // if entry has a target and TRUE rule
    if (mapEntry.getRule().equals("TRUE")) {

      // retrieve the relations by terminology id
      for (MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447637006")) {
          return relation;
        }
      }

      // if entry has a target and not TRUE rule
    } else {
      // retrieve the relations by terminology id
      for (MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }
    }

    // if relation not found, return null
    return null;

  }

  @Override
  public List<MapAdvice> computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry) throws Exception {
    Set<MapAdvice> advices = mapEntry.getMapAdvices();
  	/*For any mapRelation value other than 447637006, 
  	 * Find the allowed project advice that matches (on string, case-insensitive) 
  	 * and return that value. Throw an exception if no corresponding advice is found.
  	 */
    if (mapEntry.getMapRelation() != null && !mapEntry.getMapRelation().getTerminologyId().equals("447637006")) {
    	boolean adviceFound = false;
    	for (MapAdvice advice : mapProject.getMapAdvices()) {
        if (advice.getName().toLowerCase().equals(mapEntry.getMapRelation().getName())) {
          advices.add(advice);
          adviceFound = true;
        }
      }
    	if (!adviceFound)
    		throw new Exception ("Advice was not found in mapProject " + mapProject.getName() + 
    				" that matches mapRelation " + mapEntry.getMapRelation().getName() + ":" +
    				mapEntry.getMapRelation().getTerminologyId());
    }
    
    //ALSO, if the descendant count for the concept of the map record > 10,
    //also add 'DESCENDANTS NOT EXHAUSTIVELY MAPPED' advice.    
    
		// get hierarchical rel
		MetadataService metadataService = new MetadataServiceJpa();
		Map<String, String> hierarchicalRelationshipTypeMap = metadataService
				.getHierarchicalRelationshipTypes(mapProject.getDestinationTerminology(),
						mapProject.getDestinationTerminologyVersion());
		if (hierarchicalRelationshipTypeMap.keySet().size() > 1) {
			throw new IllegalStateException(
					"Map project source terminology has too many hierarchical relationship types - "
							+ mapProject.getDestinationTerminology());
		}
		if (hierarchicalRelationshipTypeMap.keySet().size() < 1) {
			throw new IllegalStateException(
					"Map project source terminology has too few hierarchical relationship types - "
							+ mapProject.getDestinationTerminology());
		}
		// ASSUMPTION: only a single "isa" type
		String hierarchicalRelationshipType = hierarchicalRelationshipTypeMap
				.entrySet().iterator().next().getKey();
    
		// find number of descendants
    ContentServiceJpa contentService = new ContentServiceJpa();
    SearchResultList results = contentService.findDescendants(mapRecord.getConceptId(), mapProject.getDestinationTerminology(),
    		mapProject.getDestinationTerminologyVersion(), hierarchicalRelationshipType);
    contentService.close();
    if (results.getTotalCount() > 10) {
    	for (MapAdvice advice : mapProject.getMapAdvices()) {
        if (advice.getName().toLowerCase().equals("DESCENDANTS NOT EXHAUSTIVELY MAPPED".toLowerCase())) {
          advices.add(advice);
        }
      }    	
    }
    	
    return new ArrayList<MapAdvice>(advices);
  }

  
  // TODO Implement this in the validation routines for checking target codes
  // i.e. that routine should call this
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {

    // check that code has at least three characters, that the second character
    // is a number, and does not contain a dash
    if (!terminologyId.matches(".[0-9].*") || terminologyId.contains("-")) { // "(.*?[0-9]){3,}")
                                                                             // ||
                                                                             // terminologyId.contains("-"))
                                                                             // {
      return false;
    }

    // second, verify concept exists in database
    ContentService contentService = new ContentServiceJpa();
    Concept concept =
        contentService.getConcept(terminologyId,
            mapProject.getDestinationTerminology(),
            mapProject.getDestinationTerminologyVersion());

    if (concept == null) {
      contentService.close();
      return false;
    }

    // otherwise, return true
    contentService.close();
    return true;
  }
  
  @Override
public void computeTargetTerminologyNotes(TreePositionList treePositionList)
			throws Exception {
	
	  Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
			  "Computing target terminology notes.");
	  
	  // open the metadata service and get the relationship types
	  MetadataService metadataService = new MetadataServiceJpa();
	  Map<String, String> simpleRefSets = metadataService.getSimpleRefSets(mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

	  
	  // find the dagger-to-asterisk and asterisk-to-dagger types
	  String asteriskRefSetId = null;
	  String daggerRefSetId = null;
	  
	  for (String key : simpleRefSets.keySet()) {
		  if (simpleRefSets.get(key).equals("Asterisk refset")) asteriskRefSetId = key;
		  if (simpleRefSets.get(key).equals("Dagger refset")) daggerRefSetId = key;
	  }
	  
	  
	  if (asteriskRefSetId == null) 
		  Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).warn(
				  "Could not find Asterisk refset");
	  
	  if (daggerRefSetId == null) 
		  Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).warn(
				  "Could not find Dagger refset");
	  
	  Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
			  "  Asterisk to dagger relationship type found: " + asteriskRefSetId);
	  
	  Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
			  "  Dagger to asterisk relationship type found: " + daggerRefSetId);

	  // open one content service to handle all concept retrieval
	  ContentService contentService = new ContentServiceJpa();

	  // for each tree position initially passed in, call the recursive helper
	  for (TreePosition tp : treePositionList.getTreePositions()) {

		  computeTargetTerminologyNotesHelper(tp, contentService, asteriskRefSetId, daggerRefSetId);
	  }
  }

public void computeTargetTerminologyNotesHelper(TreePosition treePosition, ContentService contentService, String asteriskRefSetId, String daggerRefSetId) throws Exception {
	
	  Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
			  "Computing target terminology note for " + treePosition.getTerminologyId());

	// initially set the note to an empty string
	treePosition.setTerminologyNote("");
	
	// get the concept
	Concept concept = contentService.getConcept(
			treePosition.getTerminologyId(), 
			mapProject.getDestinationTerminology(), 
			mapProject.getDestinationTerminologyVersion());
	
	// cycle over the simple ref set members
	for (SimpleRefSetMember simpleRefSetMember : concept.getSimpleRefSetMembers()) {
		  Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
				  "   " + simpleRefSetMember.getRefSetId());
		if (simpleRefSetMember.getRefSetId().equals(asteriskRefSetId)) treePosition.setTerminologyNote("*");
		else if (simpleRefSetMember.getRefSetId().equals(daggerRefSetId)) treePosition.setTerminologyNote("+");
	}

	// if this tree position has children, set their terminology notes recursively
	for (TreePosition child : treePosition.getChildren()) {
		computeTargetTerminologyNotesHelper(child, contentService, asteriskRefSetId, daggerRefSetId);
	}
	
}
}
