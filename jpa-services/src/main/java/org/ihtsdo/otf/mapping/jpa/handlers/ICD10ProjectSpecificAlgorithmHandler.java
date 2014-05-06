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

    ValidationResult validationResult = new ValidationResultJpa();
    ContentService contentService = new ContentServiceJpa();

    for (MapEntry mapEntry : mapRecord.getMapEntries()) {
      // first, check terminology id based on above rules
      if (!mapEntry.getTargetId().matches(".[0-9].*")
          || mapEntry.getTargetId().contains("-")) {
        validationResult
            .addError("Invalid target code "
                + mapEntry.getTargetId()
                + "!  For ICD10, valid target codes must contain 3 digits and must not contain a dash."
                + " Entry:"
                + (mapProject.isGroupStructure() ? " group "
                    + Integer.toString(mapEntry.getMapGroup()) + "," : "")
                + " map priority "
                + Integer.toString(mapEntry.getMapPriority()));

      }

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

}
