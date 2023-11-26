/*
 * Copyright 2020 Wci Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Wci Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * Wci Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * The {@link ProjectSpecificAlgorithmHandler} for CCI project.
 */
public class CCIProjectSpecificAlgorithmHandler extends DefaultProjectSpecificAlgorithmHandler {

  // These state variables are maintained by sequential calls to
  // validate complex map refset records during a release
  // Ideally this would be encapsulated into a kind of parameter
  // object and passed locally rather than relying on class state.
  // these should NOT be used in the interactive application

  /** The qa prev group. */
  private int qaPrevGroup = 0;

  /** The qa prev priority. */
  private int qaPrevPriority = 0;

  /** The qa prev concept. */
  private String qaPrevConcept = null;

  /** The qa only nc. */
  private boolean qaOnlyNc = true;

  /** The qa true rule in group. */
  private boolean qaTrueRuleInGroup = false;

  /** The icd10 external cause codes. */
  private static Map<String, Set<String>> externalCauseCodesMap = new HashMap<>();

  /** The code to advices map. */
  private static Map<String, Set<String>> codeToAdvicesMap = new HashMap<>();

  /**
   * The parser.
   *
   * @param mapRecord the map record
   * @return the validation result
   * @throws Exception the exception
   */
  // private MapRuleParser parser = new MapRuleParser();

  /* see superclass */
  @Override
  public void initialize() throws Exception {
    Logger.getLogger(getClass()).info("Running initialize for " + getClass().getSimpleName());
    // Populate any project-specific caches.
  }

  /**
   * For ICD10, a target code is valid if: - Concept exists - Concept has at
   * least 3 characters - The second character is a number (e.g. XVII is
   * invalid, but B10 is) - Concept does not contain a dash (-) character
   *
   * @param mapRecord the map record
   * @return the validation result
   * @throws Exception the exception
   */
  @Override
  public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception {

    final ValidationResult validationResult = new ValidationResultJpa();
    final ContentService contentService = new ContentServiceJpa();

    for (final MapEntry mapEntry : mapRecord.getMapEntries()) {

      // add an error if neither relation nor target are set
      if (mapEntry.getMapRelation() == null
          && (mapEntry.getTargetId() == null || mapEntry.getTargetId().equals(""))) {

        validationResult.addError(
            "A relation indicating the reason must be selected when no target is assigned.");

        // if a target is specified check it
      } else if (mapEntry.getTargetId() != null && !mapEntry.getTargetId().equals("")) {

        // All selectable codes are valid

        // otherwise, check that relation is assignable to null target
      } else {
        if (!mapEntry.getMapRelation().isAllowableForNullTarget()) {
          validationResult.addError("The map relation " + mapEntry.getMapRelation().getName()
              + " is not allowable for null targets");
        }
      }
    }

    contentService.close();
    return validationResult;

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

      if (concepts.size() == 0 || concepts.get(1) == null) {
        result.addError("Null concept in entry");
        return result;
      }

      // get the primary code (if not NC)
      final String primaryCode =
          concepts.size() == 0 || concepts.get(1) == null || concepts.get(1).get(0) == null ? null
              : concepts.get(1).get(0).getTerminologyId();

      // Only process these rules if these is a single entry per group
      if (concepts.keySet().size() == mapRecord.getMapEntries().size()) {

        //
        // PREDICATE: map entries must all have a relation
        //
        for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
          final MapEntry entry = mapRecord.getMapEntries().get(i);
          if (entry.getMapRelation() == null) {
            result.addError("Entry " + entry.getMapGroup() + "/" + entry.getMapPriority()
                + " must be assigned a Relation.");
          }
        }

        //
        // PREDICATE: for map entries that have the relation "Partially
        // classified",
        // they must also be assigned a Grade.
        //
        for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
          final Concept concept = concepts.get(i + 1).get(0);
          if (concept != null) {
            final MapEntry entry = mapRecord.getMapEntries().get(i);
            if (entry.getMapRelation() != null && entry.getMapRelation().getName().toLowerCase()
                .contains("partially classified")) {
              Boolean gradePresent = false;
              for (AdditionalMapEntryInfo additionalMapEntryInfo : entry
                  .getAdditionalMapEntryInfos()) {
                if (additionalMapEntryInfo.getField().equals("Grade")) {
                  gradePresent = true;
                  break;
                }
              }
              if (!gradePresent) {
                result.addError("Partially classified entry " + entry.getMapGroup() + "/"
                    + entry.getMapPriority() + " must be assigned a Grade.");
              }
            }
          }
        }

        //
        // PREDICATE: for map entries that don't have the relation "Partially
        // classified",
        // they must Not be assigned a Grade.
        //
        for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
          final Concept concept = concepts.get(i + 1).get(0);
          if (concept != null) {
            final MapEntry entry = mapRecord.getMapEntries().get(i);
            if (entry.getMapRelation() != null && !entry.getMapRelation().getName().toLowerCase()
                .contains("partially classified")) {
              Boolean gradePresent = false;
              for (AdditionalMapEntryInfo additionalMapEntryInfo : entry
                  .getAdditionalMapEntryInfos()) {
                if (additionalMapEntryInfo.getField().equals("Grade")) {
                  gradePresent = true;
                  break;
                }
              }
              if (gradePresent) {
                result.addError("Non-partially classified entry " + entry.getMapGroup() + "/"
                    + entry.getMapPriority() + " must not be assigned a Grade.");
              }
            }
          }
        }

        //
        // PREDICATE: for map entries that have a target that ends in a wildcard
        // "^" character,
        // they should have the relation "Partially classified"
        //
        for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
          final Concept concept = concepts.get(i + 1).get(0);
          if (concept != null) {
            final String terminologyId = concept.getTerminologyId();
            final MapEntry entry = mapRecord.getMapEntries().get(i);
            if (terminologyId.endsWith("^") && (entry.getMapRelation() == null || !entry
                .getMapRelation().getName().toLowerCase().contains("partially classified"))) {
              result.addWarning("Target id for entry " + entry.getMapGroup() + "/"
                  + entry.getMapPriority()
                  + " ends with '^', and should be assigned the 'Partially classified' Relation.");
            }
          }
        }

        //
        // PREDICATE: for map entries that have a target that don't end in a
        // wildcard "^" character,
        // they should NOT have the relation "Partially classified"
        //
        for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
          final Concept concept = concepts.get(i + 1).get(0);
          if (concept != null) {
            final String terminologyId = concept.getTerminologyId();
            final MapEntry entry = mapRecord.getMapEntries().get(i);
            if (!terminologyId.endsWith("^") && (entry.getMapRelation() == null
                || !entry.getMapRelation().getName().toLowerCase().contains("fully classified"))) {
              result.addWarning("Target id for entry " + entry.getMapGroup() + "/"
                  + entry.getMapPriority()
                  + " does not end with '^', and should be assigned one of the 'Fully classified' Relations.");
            }
          }
        }

        //
        // PREDICATE: an entry cannot have multiple Grades selected at the same
        // time
        //
        for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
          final Concept concept = concepts.get(i + 1).get(0);
          if (concept != null) {
            final MapEntry entry = mapRecord.getMapEntries().get(i);
            if (entry.getMapRelation() != null && entry.getMapRelation().getName().toLowerCase()
                .contains("partially classified")) {
              int gradeCount = 0;
              for (AdditionalMapEntryInfo additionalMapEntryInfo : entry
                  .getAdditionalMapEntryInfos()) {
                if (additionalMapEntryInfo.getField().equals("Grade")) {
                  gradeCount++;
                }
              }
              if (gradeCount > 1) {
                result.addError("Multiple grades selected for entry " + entry.getMapGroup() + "/"
                    + entry.getMapPriority() + ".");
              }
            }
          }
        }

      }

      // Handle multi-entry group rules here
      else {
        // n/a
      }

    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
    return result;
  }

  /* see superclass */
  @Override
  public MapAdviceList computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry) throws Exception {
    final List<MapAdvice> advices = new ArrayList<>(mapEntry.getMapAdvices());
    final ContentService contentService = new ContentServiceJpa();

    try {

      final Concept concept = contentService.getConcept(mapEntry.getTargetId(),
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      // lazy initialize
      if (concept != null) {
        concept.getDescriptions().size();
        concept.getRelationships().size();
        concept.getInverseRelationships().size();
        concept.getSimpleRefSetMembers().size();
      } else {
        return new MapAdviceListJpa();
      }

      // Remove any advices that are purely computed and keep only manually
      // assigned ones
      final List<MapAdvice> notComputed = new ArrayList<>();
      for (final MapAdvice advice : advices) {
        if (!advice.isComputed()) {
          notComputed.add(advice);
        }
      }
      advices.clear();
      advices.addAll(notComputed);

      //
      // PREDICATE: If map grade = B or C, then map advice is required.
      // ACTION: add advice: Requires additional qualifier 2 and/or qualifier 3
      //
      final String requiresAdditionalQualifier = "Requires additional qualifier 2 and/or qualifier 3";

      Set<AdditionalMapEntryInfo> additionalMapInfos = mapEntry.getAdditionalMapEntryInfos();
      Boolean hasGradeBorC = false;
      for (AdditionalMapEntryInfo additionalMapEntryInfo : additionalMapInfos) {
        if (additionalMapEntryInfo.getName().contains("Grade|B")
            || additionalMapEntryInfo.getName().contains("Grade|C")) {
          hasGradeBorC = true;
        }
      }

      if (hasGradeBorC) {
        if (!TerminologyUtility.hasAdvice(mapEntry, requiresAdditionalQualifier)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, requiresAdditionalQualifier));
        }
      }

      // Deduplicate advices

      MapAdviceList mapAdviceList = new MapAdviceListJpa();
      for (MapAdvice mapAdvice : advices) {
        if (!mapAdviceList.contains(mapAdvice)) {
          mapAdviceList.addMapAdvice(mapAdvice);
        }
      }

      return mapAdviceList;
    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
  }

  /* see superclass */
  @Override
  public MapRecord computeInitialMapRecord(MapRecord mapRecord) throws Exception {

    final ContentService contentService = new ContentServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();
    try {
      final String terminology = mapProject.getSourceTerminology();
      final String version = mapProject.getSourceTerminologyVersion();

      MapRecord existingMapRecord = null;

      // Get all finished map records for the project
      MapRecordList finishedMapRecords =
          mappingService.getPublishedAndReadyForPublicationMapRecordsForMapProject(
              mapProject.getId(), new PfsParameterJpa());
      Map<String, MapRecord> conceptIdsToMap = new HashMap<>();
      if (finishedMapRecords != null) {
        for (MapRecord finishedMapRecord : finishedMapRecords.getMapRecords()) {
          conceptIdsToMap.put(finishedMapRecord.getConceptId(), finishedMapRecord);
        }
      }

      // Check if any parent has already been mapped
      Concept mapRecordConcept =
          contentService.getConcept(mapRecord.getConceptId(), terminology, version);
      Set<MapRecord> parentMaps = new HashSet<>();

      List<Concept> activeParents = TerminologyUtility.getActiveParents(mapRecordConcept);
      for (final Concept activeParent : activeParents) {
        if (conceptIdsToMap.get(activeParent.getTerminologyId()) != null) {
          parentMaps.add(conceptIdsToMap.get(activeParent.getTerminologyId()));
        }
      }

      // If exactly 1 mapped parent identified, pre-assign CCI rubric level
      // codes (5 digit) to child map record
      if (parentMaps.size() == 1) {
        existingMapRecord = new MapRecordJpa(parentMaps.iterator().next(), false);
        for (MapEntry mapEntry : existingMapRecord.getMapEntries()) {
          String mapTargetId = mapEntry.getTargetId();
          // Modify targetId to the 5 digit rubric level, and lookup the updated
          // concept name
          final String modifiedTargetId = mapTargetId.substring(0, 7).concat(".^^");
          final Concept modifiedTargetConcept =
              contentService.getConcept(modifiedTargetId, mapProject.getDestinationTerminology(),
                  mapProject.getDestinationTerminologyVersion());
          String modifiedTargetName = modifiedTargetConcept == null ? "CONCEPT NOT FOUND"
              : modifiedTargetConcept.getDefaultPreferredName();
          mapEntry.setTargetId(modifiedTargetId);
          mapEntry.setTargetName(modifiedTargetName);
        }
      }

      // If more than 1 mapped parent identified, create a note with map details
      else if (parentMaps.size() > 1) {
        existingMapRecord = new MapRecordJpa();
        MapNote parentMapsAsNote = new MapNoteJpa();
        parentMapsAsNote.setUser(mappingService.getMapUser("loader"));

        StringBuilder noteStringBuilder = new StringBuilder();
        for (MapRecord parentMapRecord : parentMaps) {

          noteStringBuilder.append("<br>Parent Map for " + parentMapRecord.getConceptId() + " - "
              + parentMapRecord.getConceptName() + " :<br>");
          noteStringBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Map Entries<br>");
          for (MapEntry mapEntry : parentMapRecord.getMapEntries()) {
            noteStringBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;" + mapEntry.getMapGroup() + "/"
                + mapEntry.getMapPriority() + "   " + mapEntry.getTargetId() + " - "
                + mapEntry.getTargetName() + "<br>");
          }
          parentMapsAsNote.setNote(noteStringBuilder.toString());

          existingMapRecord.addMapNote(parentMapsAsNote);

        }
      }

      return existingMapRecord;
    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
      mappingService.close();
    }
  }

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {

    final ContentService contentService = new ContentServiceJpa();

    try {
      // verify concept exists in database
      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      if (concept == null) {
        return false;
      }

      // Valid concepts must contain a "." character. Others are Section or
      // Grouper concepts.
      if (!concept.getTerminologyId().contains(".")) {
        return false;
      }

      // Additionally, concepts must be at the Rubric level or below to be
      // selectable.
      // Rubric level is defined as having codes for the first 5 characters,
      // followed by ^^ wildcards.
      // e.g. 1.VP.87.^^ is a valid Rubric code, whereas 1.VP.^^.^^ is invalid.
      if (concept.getTerminologyId().length() >= 7
          && concept.getTerminologyId().substring(5, 7).equals("^^")) {
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
}
