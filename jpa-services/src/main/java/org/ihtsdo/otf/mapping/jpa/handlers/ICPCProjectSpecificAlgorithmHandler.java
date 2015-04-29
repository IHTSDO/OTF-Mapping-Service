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
public class ICPCProjectSpecificAlgorithmHandler extends
    DefaultProjectSpecificAlgorithmHandler {

  /**
   * For ICPC, a target code is valid if: - Concept exists - Concept has at
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

      // get concept
      Concept concept =
          contentService.getConcept(mapEntry.getTargetId(),
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());

      // verify that concept exists
      if (concept == null) {
        validationResult.addError("Target code "
            + mapEntry.getTargetId()
            + " not found in database!"
            + " Entry:"
            + (mapProject.isGroupStructure() ? " group "
                + Integer.toString(mapEntry.getMapGroup()) + "," : "")
            + " map priority " + Integer.toString(mapEntry.getMapPriority()));

        // if concept exists, verify that it is a leaf node (no children)
      } else {
        if (!isTargetCodeValid(mapEntry.getTargetId())) {

          validationResult.addError("Target "
              + mapEntry.getTargetId()
              + " is not a leaf node!"
              + " Entry:"
              + (mapProject.isGroupStructure() ? " group "
                  + Integer.toString(mapEntry.getMapGroup()) + "," : "")
              + " map priority " + Integer.toString(mapEntry.getMapPriority()));

        }
      }
    }

    contentService.close();
    return validationResult;

  }

  /**
   * Computes the map relation for the SNOMEDCT to ICD10 map project. Based
   * solely on whether an entry has a TRUE rule or not. No advices are computed
   * for this project.
   * @throws Exception
   */
  @Override
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {

    // System.out.println("Computing map relation");
    // if entry has no target
    if (mapEntry.getTargetId() == null || mapEntry.getTargetId().isEmpty()) {

      // if a relation is already set, and is allowable for null target,
      // keep it
      if (mapEntry.getMapRelation() != null
          && mapEntry.getMapRelation().isAllowableForNullTarget())
        return mapEntry.getMapRelation();
      else {
        // retrieve the not classifiable relation
        // 447638001 - Map source concept cannot be classified with available
        // data
        for (MapRelation relation : mapProject.getMapRelations()) {
          if (relation.getTerminologyId().equals("447638001"))
            return relation;
        }

        // if cannot find, return null
        return null;
      }
    }

    // if rule is not set, return null
    if (mapEntry.getRule() == null || mapEntry.getRule().isEmpty()) {
      return null;
    }

    // System.out.println("Entry has target");

    // if entry has a gender rule
    if (mapEntry.getRule().contains("MALE")) {

      // retrieve the relations by terminology id
      // 447639009 - Map of source concept is context dependent
      for (MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // if entry has an age rule
    } else if (mapEntry.getRule().contains("AGE")) {

      // retrieve the relations by terminology id
      // 447639009 - Map of source concept is context dependent
      for (MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // if the entry has a non-gender, non-age IFA
    } else if (mapEntry.getRule().startsWith("IFA")) {

      // retrieve the relations by terminology id
      // 447639009 - Map of source concept is context dependent
      for (MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // using contains here to capture TRUE and OTHERWISE TRUE
    } else if (mapEntry.getRule().contains("TRUE")) {

      // System.out.println("Entry has TRUE rule");

      // retrieve the relations by terminology id
      for (MapRelation relation : mapProject.getMapRelations()) {
        // 447637006 - Map source concept is properly classified
        if (relation.getTerminologyId().equals("447637006")) {
          return relation;
        }
      }

      // if entry has a target and not TRUE rule
    } else {

      throw new Exception("Unexpected map relation condition.");
    }

    // if relation not found, return null
    return null;

  }

}
