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

import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for ICD10CA to ICD11 mapping project.
 */
public class ICD10CAtoICD11ProjectSpecificAlgorithmHandler
    extends DefaultICD10ProjectSpecificAlgorithmHandler {

  final static Logger LOGGER =
      LoggerFactory.getLogger(ICD11toICD10CAProjectSpecificAlgorithmHandler.class);

  final static List<String> allowableWHOTargets = new ArrayList(Arrays.asList("n/a - not applicable","No 1:1 WHO map"));

  /** The canadian ICD10 codes. */
  private static Set<String> icd10caCanadianCodes = new HashSet<>();  
  
  
  /* see superclass */
  @Override
  public void initialize() throws Exception {
    LOGGER.info("Running initialize for " + getClass().getSimpleName());
    super.initialize();
    
    // Populate any project-specific caches.
    if(!mapProject.isPublished()) {
      cacheICD10CACodes();
    }
  }

  /* see superclass */
  @Override
  public ValidationResult validateTargetCodes(final MapRecord mapRecord) throws Exception {

    final ValidationResult validationResult = new ValidationResultJpa();
    final ContentService contentService = new ContentServiceJpa();

    for (final MapEntry mapEntry : mapRecord.getMapEntries()) {

      // "No target" targets are valid
      if (mapEntry.getTargetId() != null && mapEntry.getTargetId().isBlank()) {
        continue;
      }
      
      // For WHO targets, specific non-terminologyId entries are allowed
      if(mapEntry.getMapGroup() == 2) {
        if(allowableWHOTargets.contains(mapEntry.getTargetId())) {
          continue;
        }
      }

      // Target code must be an existing concept
      final Concept concept = contentService.getConcept(mapEntry.getTargetId(),
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      // For CIHI Map entries, Concept must exist
      if (mapEntry.getMapGroup() == 1 && concept == null) {
        validationResult
            .addError("Concept for CIHI target: " + mapEntry.getTargetId() + " does not exist.");
      }

      // For CIHI Map entries, Concept must be active
      if (mapEntry.getMapGroup() == 1 && concept != null && !concept.isActive()) {
        validationResult
            .addError("Concept for CIHI target: " + mapEntry.getTargetId() + " is not active.");
      }

      // For WHO Map entries, non-existent concepts are allowable
      if (mapEntry.getMapGroup() == 2 && concept == null) {
        validationResult
            .addWarning("Concept for WHO target: " + mapEntry.getTargetId() + " does not exist.");
      }
    }

    contentService.close();
    return validationResult;
  }

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(final String terminologyId) throws Exception {

    //Check if one of the allowable WHO targets
    if(allowableWHOTargets.contains(terminologyId)) {
      return true;
    }
    
    try (final ContentService contentService = new ContentServiceJpa();) {
      // verify concept exists in database
      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      if (concept == null) {
        return false;
      }

      // Only leaf nodes (concepts with no descendants) are valid
      // UNLESS they are Extension codes (start with an X) - all are valid
      // regardless of location in the hierarchy
      final TreePositionList list = contentService.getTreePositions(terminologyId,
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());
      for (final TreePosition tp : list.getTreePositions()) {
        if (tp.getDescendantCount() > 0 && !concept.getTerminologyId().startsWith("X")) {
          return false;
        }
      }

      // otherwise, return true
      return true;
    } catch (final Exception e) {
      throw e;
    }
  }

  /* see superclass */
  @Override
  public Boolean getClearAdditionalMapEntryInfoOnChange() {
    return Boolean.TRUE;
  }  
  
  /* see superclass */
  @Override
  public boolean isSourceCodeValid(final String terminologyId) throws Exception {

    try (final ContentService contentService = new ContentServiceJpa();) {
      // verify concept exists in database
      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

      if (concept == null) {
        return false;
      }

      // Only leaf nodes (concepts with no descendants) are valid
      final TreePositionList list = contentService.getTreePositions(terminologyId,
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());
      for (final TreePosition tp : list.getTreePositions()) {
        if (tp.getDescendantCount() > 0) {
          return false;
        }
      }

      // otherwise, return true
      return true;
    } catch (final Exception e) {
      throw e;
    }
  }

  /* see superclass */
  @Override
  public ValidationResult validateSemanticChecks(final MapRecord mapRecord) throws Exception {
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
      final List<String> relationWHOs = new ArrayList<>();
      final List<String> unmappableReasons = new ArrayList<>();
      final List<String> targetMismatchReasons = new ArrayList<>();

      for (final MapEntry entry : mapRecord.getMapEntries()) {
        final Set<AdditionalMapEntryInfo> additionalMapEntryInfos =
            entry.getAdditionalMapEntryInfos();
        for (final AdditionalMapEntryInfo additionalMapEntryInfo : additionalMapEntryInfos) {
          if (additionalMapEntryInfo.getField().equals("Relation - Target")) {
            relationTargets.add(additionalMapEntryInfo.getValue());
          } else if (additionalMapEntryInfo.getField().equals("Relation - Cluster")) {
            relationClusters.add(additionalMapEntryInfo.getValue());
          } else if (additionalMapEntryInfo.getField().equals("Relation - WHO")) {
            relationWHOs.add(additionalMapEntryInfo.getValue());
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
      // Cluster Relation, or Unmappable Reason
      //
      if (relationTargets.size() > 1) {
        result.addError("Map cannot have more than one Relation - Target");
      }
      if (relationClusters.size() > 1) {
        result.addError("Map cannot have more than one Relation - Cluster");
      }
      if (relationWHOs.size() > 1) {
        result.addError("Map cannot have more than one Relation - WHO");
      }
      if (unmappableReasons.size() > 1) {
        result.addError("Map cannot have more than one Unmappable Reason");
      }
      if (targetMismatchReasons.size() > 1) {
        result.addError("Map cannot have more than one Target Mismatch Reason");
      }

      // Now that we've checked for multiples, save the first one (should be
      // only one) and use it going forward.
      final String relationTarget =
          relationTargets.size() != 0 && relationTargets.get(0) != "" ? relationTargets.get(0) : "";
      final String relationCluster = relationClusters.size() != 0 && relationClusters.get(0) != ""
          ? relationClusters.get(0) : "";
      final String relationWHO =
          relationWHOs.size() != 0 && relationWHOs.get(0) != "" ? relationWHOs.get(0) : "";

      //
      // PREDICATE: When Relation - target is E-Equivalent then Relation –
      // Cluster cannot be E, B or N
      //
      if (relationTarget.startsWith("E") && (relationCluster.startsWith("E")
          || relationCluster.startsWith("B") || relationCluster.startsWith("N"))) {
        result.addError(
            "When Relation - target is E-Equivalent then Relation – Cluster cannot be E, B or N");
      }

      //
      // PREDICATE: When Relation – target is B-Broader then Relation –
      // cluster cannot be blank
      //
      if (relationTarget.startsWith("B") && relationCluster.isBlank()) {
        result.addError(
            "When Relation – target is B-Broader then Relation – cluster cannot be blank");
      }

      //
      // PREDICATE: When Relation – target is N-Narrower then Relation –
      // cluster cannot be blank
      //
      if (relationTarget.startsWith("N") && relationCluster.isBlank()) {
        result.addError(
            "When Relation – target is N-Narrower then Relation – cluster cannot be blank");
      }

      //
      // PREDICATE: When Target code is blank then Relation-Target and
      // Relation-Cluster cannot be E, B, or N (must be n/a).
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getTargetId().isBlank()
            && (!relationTarget.equals("n/a - not applicable") || !relationCluster.equals("n/a - not applicable"))) {
          result.addError(
              "When Target code is blank then Relation-Target and Relation-Cluster cannot be E, B, or N (must be n/a).");
        }
      }

      //
      // PREDICATE: 1st Group (CIHI target), when Target code is blank
      // “Unmappable reason” must have a value entered.
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getMapGroup() == 1) {
          Boolean unmappableReasonPresent = false;
          for (final AdditionalMapEntryInfo additionalMapEntryInfo : entry
              .getAdditionalMapEntryInfos()) {
            if (additionalMapEntryInfo.getField().equals("Unmappable Reason")) {
              unmappableReasonPresent = true;
            }
          }
          if (entry.getTargetId().isBlank() && !unmappableReasonPresent) {
            result.addError(
                "1st Group (CIHI target), when Target code is blank “Unmappable reason” must have a value entered.");
          }
        }
      }

      //
      // PREDICATE: 1st Group (CIHI target), when Target code is not blank,
      // “Unmappable reason” cannot have a value filled in.
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getMapGroup() == 1) {
          Boolean unmappableReasonPresent = false;
          for (final AdditionalMapEntryInfo additionalMapEntryInfo : entry
              .getAdditionalMapEntryInfos()) {
            if (additionalMapEntryInfo.getField().equals("Unmappable Reason")) {
              unmappableReasonPresent = true;
            }
          }
          if (!entry.getTargetId().isBlank() && unmappableReasonPresent) {
            result.addError(
                "1st Group (CIHI target), when Target code is not blank, “Unmappable reason” cannot have a value filled in.");
          }
        }
      }
      
      //
      // PREDICATE: When CIHI Target Code (1/1) is NOT BLANK,
      // “Relation - Target” cannot be blank.
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getMapGroup() == 1 && entry.getMapPriority() == 1) {
          Boolean relationTargetPresent = false;
          for (final AdditionalMapEntryInfo additionalMapEntryInfo : entry
              .getAdditionalMapEntryInfos()) {
            if (additionalMapEntryInfo.getField().equals("Relation - Target")) {
              relationTargetPresent = true;
            }
          }
          if (!entry.getTargetId().isBlank() && !relationTargetPresent) {
            result.addError(
                "When CIHI Target Code (1/1) is NOT BLANK, “Relation - Target” cannot be blank.");
          }
        }
      }      
      
      //
      // PREDICATE: When CIHI Target Code (1/1) is NOT BLANK,
      // “Relation - Cluster” cannot be blank.
      //
      Boolean cihiTargetCodeBlank = false;
      Boolean relationClusterPresent = false;
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getMapGroup() == 1 && entry.getMapPriority() == 1 && entry.getTargetId().isBlank()) {
          cihiTargetCodeBlank = true;
        }
        for (final AdditionalMapEntryInfo additionalMapEntryInfo : entry
            .getAdditionalMapEntryInfos()) {
          if (additionalMapEntryInfo.getField().equals("Relation - Cluster")) {
            relationClusterPresent = true;
          }
        }
      }
      if (cihiTargetCodeBlank && !relationClusterPresent) {
        result.addError(
            "When CIHI Target Code (1/1) is NOT BLANK, “Relation - Cluster” cannot be blank.");
      }
      
      //
      // PREDICATE: 2nd Group (WHO target) must have a target mismatch reason
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getMapGroup() == 2) {
          Boolean targetMismatchReasonPresent = false;
          for (final AdditionalMapEntryInfo additionalMapEntryInfo : entry
              .getAdditionalMapEntryInfos()) {
            if (additionalMapEntryInfo.getField().equals("Target Mismatch Reason")) {
              targetMismatchReasonPresent = true;
            }
          }
          if (!targetMismatchReasonPresent) {
            result.addError("2nd Group (WHO target) must have a target mismatch reason");
          }
        }
      }

      //
      // PREDICATE: 2nd Group (WHO target) target mismatch reason must be "n/a - match" if it matches 1st Group (CIHI target)
      //
      String WHOTarget = "";
      String CIHITarget = "";
      String targetMismatchReason = "";
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getMapGroup() == 1 && entry.getMapPriority() == 1) {
          CIHITarget = entry.getTargetId();
        }
        if (entry.getMapGroup() == 2 && entry.getMapPriority() == 1) {
          WHOTarget = entry.getTargetId();
          for (final AdditionalMapEntryInfo additionalMapEntryInfo : entry
              .getAdditionalMapEntryInfos()) {
            if (additionalMapEntryInfo.getField().equals("Target Mismatch Reason")) {
              targetMismatchReason = additionalMapEntryInfo.getValue();
            }
          }
        }
      }      
      if(WHOTarget.equals(CIHITarget) && !targetMismatchReason.equals("n/a - Match"))
      {
        result.addError("1st Group (CIHI Target) and 2nd Group (WHO target) match, but Target Mismatch Reason is not set to 'n/a - Match'");
      }
      
      //
      // PREDICATE: 2nd Group (WHO target) target mismatch reason cannot be "n/a - match" if it doesn't match 1st Group (CIHI target)
      //
      WHOTarget = "";
      CIHITarget = "";
      targetMismatchReason = "";
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getMapGroup() == 1 && entry.getMapPriority() == 1) {
          CIHITarget = entry.getTargetId();
        }
        if (entry.getMapGroup() == 2 && entry.getMapPriority() == 1) {
          WHOTarget = entry.getTargetId();
          for (final AdditionalMapEntryInfo additionalMapEntryInfo : entry
              .getAdditionalMapEntryInfos()) {
            if (additionalMapEntryInfo.getField().equals("Target Mismatch Reason")) {
              targetMismatchReason = additionalMapEntryInfo.getValue();
            }
          }
        }
      }   
      if(!WHOTarget.equals(CIHITarget) && targetMismatchReason.equals("n/a - Match"))
      {
        result.addError("1st Group (CIHI Target) and 2nd Group (WHO target) do not match, but Target Mismatch Reason is set to 'n/a - Match'");
      }
      
      //
      // PREDICATE: 1st Group (CIHI target) cannot have a target mismatch reason
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getMapGroup() == 1) {
          Boolean targetMismatchReasonPresent = false;
          for (final AdditionalMapEntryInfo additionalMapEntryInfo : entry
              .getAdditionalMapEntryInfos()) {
            if (additionalMapEntryInfo.getField().equals("Target Mismatch Reason")) {
              targetMismatchReasonPresent = true;
            }
          }
          if (targetMismatchReasonPresent) {
            result.addError("1st Group (CIHI target) cannot have a target mismatch reason");
          }
        }
      }

      //
      // PREDICATE: 2nd Group (WHO target) must have a relation - WHO
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getMapGroup() == 2) {
          Boolean relationWHOPresent = false;
          for (final AdditionalMapEntryInfo additionalMapEntryInfo : entry
              .getAdditionalMapEntryInfos()) {
            if (additionalMapEntryInfo.getField().equals("Relation - WHO")) {
              relationWHOPresent = true;
            }
          }
          if (!relationWHOPresent) {
            result.addError("2nd Group (WHO target) must have a Relation - WHO");
          }
        }
      }

      //
      // PREDICATE: 1st Group (CIHI target) cannot have a Relation - WHO
      //
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getMapGroup() == 1) {
          Boolean relationWHOPresent = false;
          for (final AdditionalMapEntryInfo additionalMapEntryInfo : entry
              .getAdditionalMapEntryInfos()) {
            if (additionalMapEntryInfo.getField().equals("Relation - WHO")) {
              relationWHOPresent = true;
            }
          }
          if (relationWHOPresent) {
            result.addError("1st Group (CIHI target) cannot have a Relation - WHO");
          }
        }
      }
      
      //
      // PREDICATE: When Source Code is a Canadian Code, 
      // then WHO Map (2/1) must be N/A – NOT APPLICABLE 
      // and Target Mismatch Reason must be N/A – CANADIAN CODE
      //
      
      Boolean isCanadianCode = false;
      WHOTarget = "";
      targetMismatchReason = "";
      if(icd10caCanadianCodes.contains(mapRecord.getConceptId())) {
        isCanadianCode = true;
      }
      for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
        final MapEntry entry = mapRecord.getMapEntries().get(i);
        if (entry.getMapGroup() == 2 && entry.getMapPriority() == 1) {
          WHOTarget = entry.getTargetId();
          for (final AdditionalMapEntryInfo additionalMapEntryInfo : entry
              .getAdditionalMapEntryInfos()) {
            if (additionalMapEntryInfo.getField().equals("Target Mismatch Reason")) {
              targetMismatchReason = additionalMapEntryInfo.getValue();
            }
          }
        }
      }   
      if(isCanadianCode && !targetMismatchReason.equals("n/a - Canadian code"))
      {
        result.addError("When Source Code is a canadian code, Target Mismatch Reason must be set to 'n/a - Canadian code'");
      }      
      if(isCanadianCode && !WHOTarget.equals("n/a - not applicable"))
      {
        result.addError("When Source Code is a canadian code, WHO Map (2/1) must be 'n/a - not applicable'");
      }        

    } catch (final Exception e) {
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
  public ValidationResult performDefaultChecks(final MapRecord mapRecord) {
    final Map<Integer, List<MapEntry>> entryGroups = getEntryGroups(mapRecord);

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
  @Override
  @SuppressWarnings("static-method")
  public ValidationResult checkMapRecordForDuplicateEntries(final MapRecord mapRecord) {
    final ValidationResult validationResult = new ValidationResultJpa();
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
              validationResult.addWarning("Duplicate entries (same target code) found: " + "Group "
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

  //
  // Look up the canadian codes from the saved file in the project folder
  //
  private void cacheICD10CACodes()  throws Exception {

    // Check if codes are already cached
    if (!icd10caCanadianCodes.isEmpty()) {
      return;
    }
    
    LOGGER.info("Caching ICD10 canadian codes");
   
    MappingService mappingService = new MappingServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    final String icd10caTerminology = mapProject.getSourceTerminology();
    final String icd10caTerminologyVersion = mapProject.getSourceTerminologyVersion();


    // Look up which codes are canadian-specific from file
    String icd10caVersion = mapProject.getSourceTerminologyVersion();

    final String dataDir = ConfigUtility.getConfigProperties().getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Check preconditions
    final String inputFile = dataDir + "/doc/" + mapProject.getId() + "/projectFiles/"
        + icd10caTerminology.toLowerCase() + "_" + icd10caTerminologyVersion + "_CanadianCodes.txt";

    if (!new File(inputFile).exists()) {
      throw new Exception("Specified input file missing: " + inputFile);
    }

    // Open reader and service
    final BufferedReader canadianCodeReader = new BufferedReader(new FileReader(inputFile));

    String line = null;

    while ((line = canadianCodeReader.readLine()) != null) {
      String canadianCode = line;
      icd10caCanadianCodes.add(canadianCode);
    }
    
    canadianCodeReader.close();
    mappingService.close();
    contentService.close();
    
  }  
  
}