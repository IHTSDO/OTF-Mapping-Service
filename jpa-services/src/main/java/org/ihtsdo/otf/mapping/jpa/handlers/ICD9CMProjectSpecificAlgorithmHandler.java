package org.ihtsdo.otf.mapping.jpa.handlers;

import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * The Class ICD10ProjectSpecificAlgorithmHandler.
 */
public class ICD9CMProjectSpecificAlgorithmHandler extends DefaultProjectSpecificAlgorithmHandler {


	/**
	 * For ICD9, a target code is valid if:
	 * - Concept exists
	 * - Concept is a leaf node (i.e. no children)
	 * @param mapRecord
	 * @return the validation result
	 * @throws Exception 
	 */
	@Override
	public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception {

		ValidationResult validationResult = new ValidationResultJpa();
		ContentService contentService = new ContentServiceJpa();

		for (MapEntry mapEntry : mapRecord.getMapEntries()) {

			// get concept
			Concept concept = contentService.getConcept(mapEntry.getTargetId(), mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

			// verify that concept exists
			if (concept == null) {
				validationResult.addError(
						"Target code " + mapEntry.getTargetId() + " not found in database!"
								+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
								+    " map priority " + Integer.toString(mapEntry.getMapPriority()));

				// if concept exists, verify that it is a leaf node (no children)
			} else {
				if (contentService
						.findDescendantsFromTreePostions(	concept.getTerminologyId(), 
								concept.getTerminology(), 
								concept.getTerminologyVersion() )
								.getCount() != 0) {

					validationResult.addError(
							"Target " + mapEntry.getTargetId() + " is not a leaf node!"
									+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
									+    " map priority " + Integer.toString(mapEntry.getMapPriority()));

				}
			}
		}

		contentService.close();
		return validationResult;


	}


	@Override
	public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry) {

		// if null code
		if (mapEntry.getTargetId() == null || mapEntry.getTargetId().equals("")) {

			// retrieve the cannot match relation
			for (MapRelation relation : mapProject.getMapRelations()) {
				if (relation.getTerminologyId().equals("447556008")) return relation;
			}

			// if cannot match relation not found, return null
			return null;

			// otherwise, assign match relation if no relation specified
		} else {

			// if a relation is specified and is not the cannot match relation, return unchanged
			if (mapEntry.getMapRelation() != null && !mapEntry.getMapRelation().getTerminologyId().equals("447556008")) {
				
				return mapEntry.getMapRelation();

			// otherwise return exact match relation
			} else {

				for (MapRelation relation : mapProject.getMapRelations()) {
					if (relation.getTerminologyId().equals("447557004")) { 
						return relation;
					}
				}
				
				// if relation not found, return null
				return null;
			}



		}
	}
	
	@Override
	public boolean isTargetCodeValid(String terminologyId) throws Exception {
		
		ContentService contentService = new ContentServiceJpa();
		
		// get concept
		Concept concept = contentService.getConcept(terminologyId, mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

		// verify that concept exists
		if (concept == null) {
			contentService.close();
			return false;

		// if concept exists, verify that it is a leaf node (no children)
		} else if (contentService
				.findDescendantsFromTreePostions(	concept.getTerminologyId(), 
						concept.getTerminology(), 
						concept.getTerminologyVersion() )
						.getCount() != 0) {
			contentService.close();
			return false;

		}

		// otherwise, return true
		contentService.close();
		return true;
	}
}
