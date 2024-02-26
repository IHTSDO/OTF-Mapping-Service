/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Implementation for ICD10CA to ICD11 mapping project.
 */
public class ICD10CAtoICD11ProjectSpecificAlgorithmHandler
    extends DefaultProjectSpecificAlgorithmHandler {


  /* see superclass */
  @Override
  public void initialize() throws Exception {
    Logger.getLogger(getClass()).info("Running initialize for " + getClass().getSimpleName());
    // Populate any project-specific caches.
  }

  /* see superclass */
  @Override
  public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception {

    final ValidationResult validationResult = new ValidationResultJpa();
    final ContentService contentService = new ContentServiceJpa();

    for (final MapEntry mapEntry : mapRecord.getMapEntries()) {

      // "No target" targets are valid
      if(mapEntry.getTargetId() != null && mapEntry.getTargetId().isBlank()) {
        continue;
      }
      
      // Target code must be an existing concept
      final Concept concept = contentService.getConcept(mapEntry.getTargetId(),
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      // Concept must exist
      if (concept == null) {
        validationResult.addError("Concept for target id " + mapEntry.getTargetId() + " does not exist.");
      }
      
      // Concept must be active
      if (concept != null && !concept.isActive()) {
        validationResult.addError("Concept for target id " + mapEntry.getTargetId() + " is not active.");
      }
    }

    contentService.close();
    return validationResult;
  }

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {

    final ContentService contentService = new ContentServiceJpa();

    try {
      // check that code has at least three characters, that the second
      // character
      // is a number, and does not contain a dash
      if (!terminologyId.matches(".[0-9].*") || terminologyId.contains("-")) {
        return false;
      }

      // verify concept exists in database
      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      // Only leaf nodes (concepts with no descendants) are valid
      TreePositionList list = contentService.getTreePositions(terminologyId,
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());
      for (TreePosition tp : list.getTreePositions()) {
        if (tp.getDescendantCount() > 0) {
          return false;
        }
      }

      if (concept == null) {
        return false;
      }

      // otherwise, return true
      return true;
    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
  }

  /* see superclass */
  @Override
  public ValidationResult validateSemanticChecks(MapRecord mapRecord) throws Exception {
    final ValidationResult result = new ValidationResultJpa();

    //TODO

    return result;
  }

  /**
   * Overriding defaultChecks, because there are some project-specific settings
   * that don't conform to the standard map requirements.
   * 
   * @param mapRecord the map record
   * @return the validation result
   */
  @Override
  public ValidationResult performDefaultChecks(MapRecord mapRecord) {
    Map<Integer, List<MapEntry>> entryGroups = getEntryGroups(mapRecord);

    final ValidationResult validationResult = new ValidationResultJpa();

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

    // For the this project project, we are allowing multiple entries without rules.
    // This is acceptable because their desired final release format is not
    // intended to follow strict RF2 guidelines.
    // // FATAL ERROR: multiple entries in groups for non-rule based
    // if (!mapProject.isRuleBased()) {
    // for (Integer key : entryGroups.keySet()) {
    // if (entryGroups.get(key).size() > 1) {
    // validationResult.addError(
    // "Project has no rule structure but multiple map entries found in group "
    // + key);
    // }
    // }
    // if (!validationResult.isValid()) {
    // return validationResult;
    // }
    // }

    // Verify that groups begin at index 1 and are sequential (i.e. no empty
    // groups)
    validationResult.merge(checkMapRecordGroupStructure(mapRecord, entryGroups));

    // Validation Check: verify correct positioning of TRUE rules
    validationResult.merge(checkMapRecordRules(mapRecord, entryGroups));

    // Validation Check: very higher map groups do not have only NC nodes
    validationResult.merge(checkMapRecordNcNodes(mapRecord, entryGroups));

    // Validation Check: verify entries are not duplicated
    validationResult.merge(checkMapRecordForDuplicateEntries(mapRecord));

    // Validation Check: verify advice values are valid for the project (this
    // can happen if "allowable map advice" changes without updating map
    // entries)
    validationResult.merge(checkMapRecordAdvices(mapRecord, entryGroups));

    // Validation Check: all entries are non-null (empty entries are empty
    // strings)
    validationResult.merge(checkMapRecordForNullTargetIds(mapRecord));

    return validationResult;
  }

  /**
   * Verify no duplicate entries in the map.
   * 
   * @param mapRecord the map record
   * @return a list of errors detected
   */
  @SuppressWarnings("static-method")
  public ValidationResult checkMapRecordForDuplicateEntries(MapRecord mapRecord) {
    ValidationResult validationResult = new ValidationResultJpa();
    final List<MapEntry> entries = mapRecord.getMapEntries();

    // cycle over all entries but last
    for (int i = 0; i < entries.size() - 1; i++) {

      // cycle over all entries after this one
      // NOTE: separated boolean checks for easier handling of possible null
      // relations
      for (int j = i + 1; j < entries.size(); j++) {

        // if first entry target null
        if (entries.get(i).getTargetId() == null || entries.get(i).getTargetId().equals("")) {

          // if both null, check relations
          if (entries.get(j).getTargetId() == null || entries.get(j).getTargetId().equals("")) {

            if (entries.get(i).getMapRelation() != null && entries.get(j).getMapRelation() != null
                && entries.get(i).getMapRelation().equals(entries.get(j).getMapRelation())
                && !entries.get(i).getMapRelation().getName()
                    .equals("MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA")) {
              validationResult
                  .addWarning("Duplicate entries (null target code, same map relation) found: "
                      + "Group " + Integer.toString(entries.get(i).getMapGroup()) + ", priority "
                      + Integer.toString(entries.get(i).getMapPriority()) + " and " + "Group "
                      + Integer.toString(entries.get(j).getMapGroup()) + ", priority "
                      + Integer.toString(entries.get(j).getMapPriority()));
            }
          }

        } else if (entries.get(i).getRule() != null && entries.get(j).getRule() != null) {

          // check if second entry's target identical to this one
          if (entries.get(i).getTargetId().equals(entries.get(j).getTargetId())
              && entries.get(i).getRule().equals(entries.get(j).getRule())) {
            validationResult.addWarning("Duplicate entries (same target code and rule) found: "
                + "Group " + Integer.toString(entries.get(i).getMapGroup()) + ", priority "
                + Integer.toString(entries.get(i).getMapPriority()) + " and " + "Group "
                + Integer.toString(entries.get(j).getMapGroup()) + ", priority "
                + Integer.toString(entries.get(j).getMapPriority()));
          }

        } else {

          // check if second entry's target identical to this one
          if (entries.get(i).getTargetId().equals(entries.get(j).getTargetId())) {
            validationResult.addError("Duplicate entries (same target code) found: " + "Group "
                + Integer.toString(entries.get(i).getMapGroup()) + ", priority "
                + Integer.toString(entries.get(i).getMapPriority()) + " and " + "Group "
                + Integer.toString(entries.get(j).getMapGroup()) + ", priority "
                + Integer.toString(entries.get(j).getMapPriority()));
          }
        }

      }
    }
    return validationResult;
  }
}