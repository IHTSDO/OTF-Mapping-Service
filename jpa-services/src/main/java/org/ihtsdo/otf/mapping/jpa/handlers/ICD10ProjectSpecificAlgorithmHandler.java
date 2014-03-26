package org.ihtsdo.otf.mapping.jpa.handlers;

import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;

// TODO: Auto-generated Javadoc
/**
 * The Class ICD10ProjectSpecificAlgorithmHandler.
 */
public class ICD10ProjectSpecificAlgorithmHandler extends DefaultProjectSpecificAlgorithmHandler {

	
	/**
	 * For ICD10, a target code is valid if:
	 * - Concept exists
	 * - Concept has at least 3 digits in terminology id
	 * - Concept does not contain a dash (-) character
	 * @param mapRecord
	 * @return
	 * @throws Exception 
	 */
	@Override
	public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception {
		
		ValidationResult validationResult = new ValidationResultJpa();
		ContentService contentService = new ContentServiceJpa();
		
		for (MapEntry mapEntry : mapRecord.getMapEntries()) {
			// first, check terminology id based on above rules
			if (!mapEntry.getTargetId().matches("[*d*d*d*]")|| mapEntry.getTargetId().contains("-")) {
				validationResult.addError(
							 "Invalid target code " + mapEntry.getTargetId() + "!  For ICD10, valid target codes must contain 3 digits and must not contain a dash." 
						+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
						+    " map priority " + Integer.toString(mapEntry.getMapPriority()));
			
			}
			
			// second, verify concept exists
			Concept concept = contentService.getConcept(mapRecord.getConceptId(), mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

			if (concept == null) {
				validationResult.addError(
						 "Target code " + mapEntry.getTargetId() + " not found in database!"
					+    " Entry:" +    (mapProject.isGroupStructure() ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
					+    " map priority " + Integer.toString(mapEntry.getMapPriority()));

			}
		}
		
		contentService.close();
		return validationResult;
		

	}

	@Override
	public ValidationResult computeMapAdviceAndMapRelations(MapRecord mapRecord) {
		
		ValidationResult validationResult = new ValidationResultJpa();
		
		// find the relevant map relations
		MapRelation relationTrueRule = null;
		MapRelation relationNotTrueRule = null;
		for (MapRelation relation : mapProject.getMapRelations()) {
			if (relation.getTerminologyId().equals("447639009")) relationNotTrueRule = relation;
			if (relation.getTerminologyId().equals("447637006")) relationTrueRule = relation;
		}
		
		for (MapEntry mapEntry : mapRecord.getMapEntries()) {
			
			if (mapEntry.getTargetId() != null && !mapEntry.getTargetId().isEmpty()) {
				if (mapEntry.getRule().equals("TRUE")) mapEntry.setMapRelation(relationTrueRule);
				else mapEntry.setMapRelation(relationNotTrueRule);
			}
		}
		
		return validationResult;
		
	}


}
