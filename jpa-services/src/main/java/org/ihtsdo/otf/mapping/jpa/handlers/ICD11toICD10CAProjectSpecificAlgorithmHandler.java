/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;

/**
 * Implementation for ICD11 to ICD10CA mapping project.
 */
public class ICD11toICD10CAProjectSpecificAlgorithmHandler
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
      if (mapEntry.getTargetId() != null && mapEntry.getTargetId().isBlank()) {
        continue;
      }

      // Target code must be an existing concept
      final Concept concept = contentService.getConcept(mapEntry.getTargetId(),
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      // Concept must exist
      if (concept == null) {
        validationResult
            .addError("Concept for target id " + mapEntry.getTargetId() + " does not exist.");
      }

      // Concept must be active
      if (concept != null && !concept.isActive()) {
        validationResult
            .addError("Concept for target id " + mapEntry.getTargetId() + " is not active.");
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

    // Bail immediately if map has no entries (other QA will catch this)
    if (mapRecord.getMapEntries().size() == 0) {
      return result;
    }

    final ContentService contentService = new ContentServiceJpa();
    try {
      final String terminology = mapProject.getDestinationTerminology();
      final String version = mapProject.getDestinationTerminologyVersion();

      // Collect concepts in entry order, null if it doesn't exist
      // group by mapGroup
      final Map<Integer, List<Concept>> concepts = new HashMap<>();
      for (final MapEntry entry : mapRecord.getMapEntries()) {
        if (!concepts.containsKey(entry.getMapGroup())) {
          concepts.put(entry.getMapGroup(), new ArrayList<Concept>());
        }
        final Concept concept =
            contentService.getConcept(entry.getTargetId(), terminology, version);
        // Lazy initialize
        if (concept != null) {
          concept.getDescriptions().size();
          concept.getRelationships().size();
          concept.getInverseRelationships().size();
          concept.getSimpleRefSetMembers().size();
        }
        concepts.get(entry.getMapGroup()).add(concept);
      }

      // Gather the target and cluster relations - these are used for multiple
      // validation checks.
      final List<String> relationTargets = new ArrayList<>();
      final List<String> relationClusters = new ArrayList<>();
      final List<String> unmappableReasons = new ArrayList<>();
      final List<String> targetMismatchReasons = new ArrayList<>();

      for (final MapEntry entry : mapRecord.getMapEntries()) {
        Set<AdditionalMapEntryInfo> additionalMapEntryInfos = entry.getAdditionalMapEntryInfos();
        for (AdditionalMapEntryInfo additionalMapEntryInfo : additionalMapEntryInfos) {
          if (additionalMapEntryInfo.getField().equals("Relation - Target")) {
            relationTargets.add(additionalMapEntryInfo.getValue());
          } else if (additionalMapEntryInfo.getField().equals("Relation - Cluster")) {
            relationClusters.add(additionalMapEntryInfo.getValue());
          } else if (additionalMapEntryInfo.getField().equals("Unmappable Reason")) {
            unmappableReasons.add(additionalMapEntryInfo.getValue());
          } else if (additionalMapEntryInfo.getField().equals("Target Mismatch Reason")) {
            targetMismatchReasons.add(additionalMapEntryInfo.getValue());
          }
        }
      }

      if (concepts.size() == 0 || concepts.get(1) == null) {
        result.addError("Null concept in entry");
        return result;
      }

      //
      // PREDICATE: Each map cannot have more than one Target Relation,
      // Cluster Relation, Unmappable Reason, or Mismatch Reason
      //
      if (relationTargets.size() > 1) {
        result.addError("Map cannot have more than one Relation - Target");
      }
      if (relationClusters.size() > 1) {
        result.addError("Map cannot have more than one Relation - Cluster");
      }
      if (unmappableReasons.size() > 1) {
        result.addError("Map cannot have more than one Unmappable Reason");
      }
      if (targetMismatchReasons.size() > 1) {
        result.addError("Map cannot have more than one Target Mismatch Reason");
      }

      // Now that we've checked for multiples, save the first one (should be
      // only one) and use it going forward.
      String relationTarget =
          relationTargets.size() != 0 && relationTargets.get(0) != "" ? relationTargets.get(0) : "";
      String relationCluster = relationClusters.size() != 0 && relationClusters.get(0) != ""
          ? relationClusters.get(0) : "";
      String unmappableReason = unmappableReasons.size() != 0 && unmappableReasons.get(0) != ""
          ? unmappableReasons.get(0) : "";

      //
      // PREDICATE: When Target code is blank “Unmappable reason” must have a
      // value entered.
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getTargetId().isBlank() && unmappableReason.isBlank()) {
          result
              .addError("When Target code is blank “Unmappable reason” must have a value entered.");
        }
      }

      //
      // PREDICATE: When Target code is not blank, “Unmappable reason” cannot
      // have a value filled in.
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (!entry.getTargetId().isBlank() && !unmappableReason.isBlank()) {
          result.addError(
              "When Target code is not blank, “Unmappable reason” cannot have a value filled in.");
        }
      }
      
      //
      // PREDICATE: 2nd Group (WHO target) must have a target mismatch reason
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if(entry.getMapGroup() == 2) {
          Boolean targetMismatchReasonPresent = false;
          for(AdditionalMapEntryInfo additionalMapEntryInfo : entry.getAdditionalMapEntryInfos()) {
            if(additionalMapEntryInfo.getField().equals("Target Mismatch Reason")) {
              targetMismatchReasonPresent = true;
            }
          }
          if(!targetMismatchReasonPresent) {
            result.addError("2nd Group (WHO targe)t must have a target mismatch reason");
          }
        }
      }

      //
      // PREDICATE: 1st Group (CIHI target) cannot have a target mismatch reason
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if(entry.getMapGroup() == 1) {
          Boolean targetMismatchReasonPresent = false;
          for(AdditionalMapEntryInfo additionalMapEntryInfo : entry.getAdditionalMapEntryInfos()) {
            if(additionalMapEntryInfo.getField().equals("Target Mismatch Reason")) {
              targetMismatchReasonPresent = true;
            }
          }
          if(targetMismatchReasonPresent) {
            result.addError("1st Group (CIHI target) cannot have a target mismatch reason");
          }
        }
      }         

    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }

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

    // For this project project, we are allowing multiple entries without rules.
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

    // Validation Check: verify entries are not duplicated - only within Map
    // Group 1
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
   * Verify no duplicate entries in the map. For this project, we are only
   * looking within Group 1 (there will be duplicates with Group 2)
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

        // Only look within Map Group 1 (map group 2 is WHO target, which can be
        // duplicated)
        if (entries.get(i).getMapGroup() == 1 && entries.get(j).getMapGroup() == 1) {

          // if first entry target null
          if (entries.get(i).getTargetId() == null || entries.get(i).getTargetId().equals("")) {

            // if second entry target null
            if (entries.get(j).getTargetId() == null || entries.get(j).getTargetId().equals("")) {

              validationResult.addError("Duplicate entries (null target code) found: " + "Group "
                  + Integer.toString(entries.get(i).getMapGroup()) + ", priority "
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
    }
    return validationResult;
  }
}