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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.FileSorter;

/**
 * The {@link ProjectSpecificAlgorithmHandler} for ICDCA10 project.
 */
public class ICD10CAProjectSpecificAlgorithmHandler extends DefaultProjectSpecificAlgorithmHandler {

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

  /** The icd10 maps for preloading. */
  private static Map<String, MapRecord> existingIcd10Maps = new HashMap<>();

  /** The dagger codes. */
  private static Set<String> daggerCodes = new HashSet<>();

  /** The asterisk codes. */
  private static Set<String> asteriskCodes = new HashSet<>();

  /** The asterisk ref set id. */
  private static String asteriskRefSetId;

  /** The dagger ref set id. */
  private static String daggerRefSetId;

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
    cacheExistingMaps();
    cacheCodes();
    cacheCodeToAdvices();
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

        // first, check terminology id based on above rules
        if (!mapEntry.getTargetId().equals("") && (!mapEntry.getTargetId().matches(".[0-9].*")
            || mapEntry.getTargetId().contains("-"))) {
          validationResult.addError("Invalid target code " + mapEntry.getTargetId()
              + "!  For ICD10CA, valid target codes must contain at least 3 digits and must not contain a dash."
              + " Entry:"
              + (mapProject.isGroupStructure()
                  ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
              + " map priority " + Integer.toString(mapEntry.getMapPriority()));
        } else {

          // Validate the code
          if (!isTargetCodeValid(mapEntry.getTargetId())) {

            validationResult.addError("Target code " + mapEntry.getTargetId()
                + " is an invalid code, use a child code instead. " + " Entry:"
                + (mapProject.isGroupStructure()
                    ? " group " + Integer.toString(mapEntry.getMapGroup()) + "," : "")
                + " map  priority " + Integer.toString(mapEntry.getMapPriority()));

          }

        }

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
    cacheCodes();
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
      }

      // get the primary code (if not NC)
      final String primaryCode =
          concepts.size() == 0 || concepts.get(1) == null || concepts.get(1).get(0) == null ? null
              : concepts.get(1).get(0).getTerminologyId();

      // Only process these rules if these is a single entry per group
      if (concepts.keySet().size() == mapRecord.getMapEntries().size()) {

        //
        // PREDICATE: primary map target is an asterisk code with secondary map
        // code as a dagger code.
        // GUIDANCE: Switch order of codes (this was eventually automated)
        //
        if (mapRecord.getMapEntries().size() == 2 && TerminologyUtility
            .isDaggerForAsterisk(concepts.get(1).get(0), concepts.get(2).get(0), contentService)) {
          result.addWarning(
              "Primary asterisk with secondary dagger" + " code, consider switching order.");
        }

        //
        // PREDICATE: primary map target is a dagger code with an asterisk
        // reference in the preferred rubric AND there is no secondary code
        // matching that asterisk code
        // GUIDANCE: Add the secondary code
        //
        if (concepts.get(1).get(0) != null && daggerCodes.contains(primaryCode)) {

          // iterate through descriptions/relationships and see if there is an
          // asterisk code
          String asteriskCode = null;
          for (final Description desc : concepts.get(1).get(0).getDescriptions()) {
            // "preferred" type - TODO: this could be improved upon by accessing
            // metadata
            if (desc.getTypeId().equals("4")) {
              for (final Relationship rel : concepts.get(1).get(0).getRelationships()) {
                // the relationship terminologyId will match the description id
                if (rel.getTerminologyId().startsWith(desc.getTerminologyId() + "~")) {
                  asteriskCode = rel.getDestinationConcept().getTerminologyId();
                }
              }
            }
          }
          if (asteriskCode != null) {
            // if there is no secondary code matching asterisk
            if (concepts.keySet().size() == 1
                || !concepts.get(2).get(0).getTerminologyId().equals(asteriskCode)) {
              result.addWarning("Remap, primary dagger code should have a secondary asterisk "
                  + "code mapping indicated by the preferred rubric (" + asteriskCode + ")");
            }
          }
        }

        //
        // PREDICATE: primary map target is a 4th digit ICD code having a fifth
        // digit option of 0 (open) or 1 (closed).
        // GUIDANCE: Remap to 5 digits and consider “MAPPED FOLLOWING CIHI
        // GUIDANCE" if SNOMED does not indicate open or "closed"
        //
        final List<Concept> children = TerminologyUtility.getActiveChildren(concepts.get(1).get(0));
        if (concepts.get(1).get(0) != null && primaryCode.length() == 5 && children.size() > 1
            && (children.get(0).getDefaultPreferredName().endsWith("open")
                || children.get(0).getDefaultPreferredName().endsWith("closed"))) {
          result.addError("Remap to 5 or 6 digits and add \"MAPPED FOLLOWING CIHI GUIDANCE\" "
              + "advice if SNOMED does not indicate open or closed");

        }

        //
        // PREDICATE: primary map target is a 5th digit ICD code for "open"
        // where SNOMED doesn't indicate "open" or "closed".
        // GUIDANCE: Remap to "open" and add MAPPED FOLLOWING CIHI GUIDANCE
        //
        if (concepts.get(1).get(0) != null
            && (primaryCode.length() == 6 || primaryCode.length() == 7)
            && concepts.get(1).get(0).getDefaultPreferredName().endsWith("open")
            && !mapRecord.getConceptName().toLowerCase().contains("open")
            && !mapRecord.getConceptName().toLowerCase().contains("closed")) {
          result.addWarning("Remap fracture to \"closed\" and "
              + "add \"MAPPED FOLLOWING CIHI GUIDANCE\" advice");
        }
        if (concepts.get(1).get(0) != null
            && (primaryCode.length() == 6 || primaryCode.length() == 7)
            && concepts.get(1).get(0).getDefaultPreferredName().endsWith("open")
            && mapRecord.getConceptName().toLowerCase().contains("closed")) {
          result.addWarning("Possible closed fracture mapped to 'open'");
        }
        if (concepts.get(1).get(0) != null
            && (primaryCode.length() == 6 || primaryCode.length() == 7)
            && concepts.get(1).get(0).getDefaultPreferredName().endsWith("closed")
            && mapRecord.getConceptName().toLowerCase().contains("open")) {
          result.addWarning("Possible open fracture mapped to 'closed'");
        }

        //
        // PREDICATE: primary map target is a Chapter XX code and there is a non
        // Chapter XX secondary code (e.g. V, W, X, or Y code)
        // GUIDANCE: Remap, Chapter XX codes should either be on their own (when
        // mapping events), or used as secondary codes.
        //
        if (concepts.get(1).get(0) != null && mapRecord.getMapEntries().size() > 1
            && primaryCode.matches("^[VWXY].*")) {
          result.addError("Remap, Chapter XX codes should either be on their "
              + "own, or used as secondary codes.");
        }

        //
        // PREDICATE: Code range T90.0 through T98.3 must have either an
        // external cause code from range Y85.0 - Y89.9 or advice POSSIBLE
        // REQUIREMENT FOR EXTERNAL CAUSE CODE.
        //
        if (primaryCode != null && (primaryCode.matches("^T9[0-7].*")
            || primaryCode.startsWith("T98.0") || primaryCode.startsWith("T98.1")
            || primaryCode.startsWith("T98.2") || primaryCode.startsWith("T98.3"))) {

          boolean hasAdvice = TerminologyUtility.hasAdvice(mapRecord.getMapEntries().get(0),
              "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE");
          boolean hasExternalCauseCode = false;
          boolean hasOtherExternalCauseCode = false;
          for (final MapEntry entry : mapRecord.getMapEntries()) {
            if (entry.getTargetId().matches("^Y8[5-9].*")) {
              hasExternalCauseCode = true;
              break;
            } else if (entry.getTargetId().matches("^[VWXY].*")) {
              hasOtherExternalCauseCode = true;
            }

          }
          if (hasOtherExternalCauseCode || (!hasAdvice && !hasExternalCauseCode)) {
            result.addError("Code range T90.0 through T98.3 must have either an "
                + "external cause code from range Y85.0 - Y89.9 or "
                + "advice \"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE\"");
          }
        }

        // PREDICATE: Code K52.1 must have either an
        // external cause code or advice POSSIBLE
        // REQUIREMENT FOR EXTERNAL CAUSE CODE.
        //
        if (primaryCode != null && primaryCode.equals("K52.1")) {

          boolean hasAdvice = TerminologyUtility.hasAdvice(mapRecord.getMapEntries().get(0),
              "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE");
          boolean hasExternalCauseCode = false;
          for (final MapEntry entry : mapRecord.getMapEntries()) {
            if (entry.getTargetId().matches("^[VWXY].*")) {
              hasExternalCauseCode = true;
              break;
            }
          }
          if (!hasExternalCauseCode && !hasAdvice) {
            result.addError("Code K52.1 must have either an " + "external cause code or "
                + "advice \"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE\"");
          }
        }

        //
        // PREDICATE: SNOMED CT concept to map is a poisoning concept
        // (descendant of “Poisoning” 75478009 or "Adverse reaction to drug"
        // 62014003) and there is not a secondary (or higher) map
        // target from the list of external cause codes applicable to poisonings
        // (as derived from columns 2,3, and 5 from TEIL3.ASC index file).
        // GUIDANCE: Remap to include the (required) external cause code.
        //
        boolean isPoisoning = mapRecord.getConceptId().equals("75478009")
            || mapRecord.getConceptId().equals("62014003")
            || contentService.isDescendantOf(mapRecord.getConceptId(),
                mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion(),
                Arrays.asList(new String[] {
                    "75478009", "62014003"
                }));
        if (concepts.get(1).get(0) != null && mapRecord.getMapEntries().size() == 1
            && isPoisoning) {
          result.addWarning(
              "Remap, poisoning requires an external cause code from the TEIL3.ASC index");
        }
        // Validate external cause code presence and not primary position
        else if (concepts.get(1).get(0) != null && mapRecord.getMapEntries().size() > 1
            && isPoisoning) {

          // cause code in primary position
          if (getIcd10ExternalCauseCodes()
              .contains(mapRecord.getMapEntries().get(0).getTargetId())) {
            result.addWarning(
                "Remap, poisoning requires an external cause code in a secondary position");
          }

          // Validate the external cause code
          else {

            Set<String> cmpCodes = getIcd10ExternalCauseCodes();
            String type = "unspecified";
            String column = "accidental";
            // accidental
            if (mapRecord.getConceptName().toLowerCase().contains("accidental")
                && mapRecord.getConceptName().toLowerCase().contains("poisoning")) {
              type = "accidental";
              column = type;
              cmpCodes = getIcd10AccidentalPoisoningCodes();
            }

            // intensional
            else if (mapRecord.getConceptName().toLowerCase().contains("intensional")
                && mapRecord.getConceptName().toLowerCase().contains("poisoning")) {
              type = "intensional";
              column = type;
              cmpCodes = getIcd10IntentionalPoisoningCodes();
            }

            // undetermined
            else if (mapRecord.getConceptName().toLowerCase().contains("undetermined")
                && mapRecord.getConceptName().toLowerCase().contains("undetermined")) {
              type = "undetermined";
              column = type;
              cmpCodes = getIcd10UndeterminedPoisoningCodes();

            }

            // adverse reaction
            else if (mapRecord.getConceptName().toLowerCase().contains("adverse")
                && mapRecord.getConceptName().toLowerCase().contains("reaction")) {
              type = "adverse reaction";
              column = "adverse reaction";
              cmpCodes = getIcd10AdverseEffectPoisoningCodes();

            }

            boolean found = false;
            for (int i = 1; i < mapRecord.getMapEntries().size(); i++) {
              final String targetId = mapRecord.getMapEntries().get(i).getTargetId();
              if (cmpCodes.contains(targetId)) {
                found = true;
                break;
              }
            }
            if (!found) {
              // check each of higher map entries looking for code in the valid
              // index list
              // Unfortunately index data is not loaded, so we need a static
              // list.
              result.addWarning("Remap poisoning, " + type
                  + " poisoning requires an external cause code from the '" + column
                  + "' column of the TEIL3.ASC index");
            }
          }
        }

        //
        // PREDICATE: J40, J20.0, J20.1, J20.2, J20.3, J20.4, J20.5, J20.6,
        // J20.7,
        // J20.8, J20.9, A50.2
        // and no "current patient age" rule
        // GUIDANCE: Recommend using a "current patient age" map rule
        //
        for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
          final Concept concept = concepts.get(i + 1).get(0);
          if (concept != null) {
            final MapEntry entry = mapRecord.getMapEntries().get(i);

            if (Arrays.asList(new String[] {
                "J40", "J20.0", "J20.1", "J20.2", "J20.3", "J20.4", "J20.5", "J20.6", "J20.7",
                "J20.8", "J20.9", "A50.2"
            }).contains(concept.getTerminologyId())
                && !entry.getRule().contains("Current chronological age")) {
              result
                  .addWarning("Consider adding a \"Current chronological age\" rule to entry " + i);
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

  /**
   * Computes the map relation for the SNOMEDCT to ICD10 map project. Based
   * solely on whether an entry has a TRUE rule or not. No advices are computed
   * for this project.
   *
   * @param mapRecord the map record
   * @param mapEntry the map entry
   * @return the map relation
   * @throws Exception the exception
   */
  @Override
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry) throws Exception {

    if (mapEntry == null) {
      return null;
    }
    // if entry has no target
    if (mapEntry.getTargetId() == null || mapEntry.getTargetId().isEmpty()) {

      // if a relation is already set, and is allowable for null target,
      // keep it
      if (mapEntry.getMapRelation() != null && mapEntry.getMapRelation().isAllowableForNullTarget())
        return mapEntry.getMapRelation();
      else {
        // retrieve the not classifiable relation
        // 447638001 - Map source concept cannot be classified with available
        // data
        for (final MapRelation relation : mapProject.getMapRelations()) {
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

    // if entry has a gender rule
    if (mapEntry.getRule().contains("MALE")) {

      // retrieve the relations by terminology id
      // 447639009 - Map of source concept is context dependent
      for (final MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // if entry has an age rule
    } else if (mapEntry.getRule().contains("AGE")) {

      // retrieve the relations by terminology id
      // 447639009 - Map of source concept is context dependent
      for (final MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // if the entry has a non-gender, non-age IFA
    } else if (mapEntry.getRule().startsWith("IFA")) {

      // retrieve the relations by terminology id
      // 447639009 - Map of source concept is context dependent
      for (final MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals("447639009")) {
          return relation;
        }
      }

      // using contains here to capture TRUE and OTHERWISE TRUE
    } else if (mapEntry.getRule().contains("TRUE")) {

      // retrieve the relations by terminology id
      for (final MapRelation relation : mapProject.getMapRelations()) {
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

  /* see superclass */
  @Override
  public MapAdviceList computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry) throws Exception {
    cacheCodes();
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
      // Define advices and boolean flags
      //
      final String externalCauseCodeAdvice = "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE";
      boolean hasExternalCauseCodeAdvice =
          TerminologyUtility.hasAdvice(mapEntry, externalCauseCodeAdvice);

      //
      // PREDICATE: asterisk code is used and it does not have
      // THIS CODE MAY BE USED IN THE PRIMARY POSITION WHEN THE MANIFESTATION IS
      // THE PRIMARY FOCUS OF CARE
      // advice.
      // ACTION: add the advice if not present, remove the advice if not
      // asterisk code
      // primary or secondary - any position
      final String asteriskAdvice = "THIS CODE MAY BE USED IN THE PRIMARY POSITION "
          + "WHEN THE MANIFESTATION IS THE PRIMARY FOCUS OF CARE";
      // If asterisk code
      if (asteriskCodes.contains(concept.getTerminologyId())) {
        if (!TerminologyUtility.hasAdvice(mapEntry, asteriskAdvice)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, asteriskAdvice));
        }
      }
      // otherwise if advice present
      else if (TerminologyUtility.hasAdvice(mapEntry, asteriskAdvice)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, asteriskAdvice));
      }

      //
      // PREDICATE: single asterisk code without dagger code
      // ACTION: add advice: THIS MAP REQUIRES A DAGGER CODE AS WELL AS AN
      // ASTERISK CODE
      //
      final String daggerAlsoAdvice = "THIS MAP REQUIRES A DAGGER CODE AS WELL AS AN ASTERISK CODE";

      if (asteriskCodes.contains(concept.getTerminologyId())
          && mapRecord.getMapEntries().size() == 1) {
        if (!TerminologyUtility.hasAdvice(mapEntry, daggerAlsoAdvice)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, daggerAlsoAdvice));
        }
      }
      // otherwise if advice present
      else if (TerminologyUtility.hasAdvice(mapEntry, daggerAlsoAdvice)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, daggerAlsoAdvice));
      }

      //
      // PREDICATE: W00-Y34 except X34,X59,Y06,Y07,Y35,Y36 without
      // "MANDATORY REQUIREMENT FOR PLACE OF OCCURRENCE" advice
      // W26 added to this list also.
      // ACTION: add the advice
      //
      final String adviceP03 = "MANDATORY REQUIREMENT FOR PLACE OF OCCURRENCE";
      if (mapEntry.getTargetId().matches("(W..|X..|Y[0-2].|Y3[0-4]).*")
          && !mapEntry.getTargetId().startsWith("W26") && !mapEntry.getTargetId().startsWith("Y06")
          && !mapEntry.getTargetId().startsWith("Y07") && !mapEntry.getTargetId().startsWith("Y35")
          && !mapEntry.getTargetId().startsWith("Y36") && !mapEntry.getTargetId().startsWith("X34")
          && !mapEntry.getTargetId().startsWith("X59")) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP03)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP03));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP03)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP03));
      }

      //
      // PREDICATE: Primary map target is T31 or T32 and does not have the
      // advice
      // "USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS
      // A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29 (Burns)"
      // ACTION: add the advice
      //
      final String adviceP06 =
          "USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29 (Burns)";
      if (mapEntry.getTargetId().matches("(T31|T32).*") && mapEntry.getMapGroup() == 1
          && mapEntry.getMapPriority() == 1 && !TerminologyUtility.hasAdvice(mapEntry, adviceP06)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP06));
      }

      //
      // PREDICATE: Primary target is a poisoning code and there is a secondary
      // code indicating accidental intent and the SNOMED concept does not
      // indicate intent and the entry does not have the advice
      // "MAPPED FOLLOWING CIHI GUIDANCE"
      // ACTION: add the advice
      //
      final String adviceP21a = "MAPPED FOLLOWING CIHI GUIDANCE";
      final String advice = "CONSIDER AVAILABILITY OF FURTHER CODE SPECIFICITY";
      boolean isPoisoning = mapRecord.getConceptId().equals("75478009") || contentService
          .isDescendantOf(mapRecord.getConceptId(), mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion(), "75478009");
      if (isPoisoning && !mapRecord.getConceptName().toLowerCase().matches("adverse")
          && !mapRecord.getConceptName().toLowerCase().matches("unintentional")
          && !mapRecord.getConceptName().toLowerCase().matches("accidental")
          && !mapRecord.getConceptName().toLowerCase().matches("intentional")
          && !mapRecord.getConceptName().toLowerCase().matches("undetermined")
          && mapEntry.getMapGroup() > 1 && mapEntry.getMapPriority() == 1
          && getIcd10AccidentalPoisoningCodes().contains(mapEntry.getTargetId())
          && (!TerminologyUtility.hasAdvice(mapEntry, adviceP21a)
              || !TerminologyUtility.hasAdvice(mapEntry, advice))) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP21a));
        advices.add(TerminologyUtility.getAdvice(mapProject, advice));
      }

      //
      // PREDICATE: Fracture mapped to "closed" and SNOMED does not
      // indicate open or closed.
      // ACTION: "add MAPPED FOLLOWING CIHI GUIDANCE" advice if mapped to closed
      //
      if (mapEntry.getMapGroup() == 1 && mapEntry.getMapPriority() == 1
          && !mapRecord.getConceptName().toLowerCase().contains("open")
          && !mapRecord.getConceptName().toLowerCase().contains("closed")
          && mapEntry.getTargetName().endsWith("closed")
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP21a)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP21a));
      }

      else if (mapEntry.getMapGroup() == 1 && mapEntry.getMapPriority() == 1
          && !mapRecord.getConceptName().toLowerCase().contains("open")
          && !mapRecord.getConceptName().toLowerCase().contains("closed")
          && mapEntry.getTargetName().endsWith("open")
          && TerminologyUtility.hasAdvice(mapEntry, adviceP21a)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP21a));
      }

      //
      // PREDICATE: S or T code without advice
      // "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE"
      // and without secondary external cause code
      // ACTION: add the advice
      //
      if ((mapEntry.getTargetId().startsWith("S") || mapEntry.getTargetId().startsWith("T"))) {
        if (mapRecord.getMapEntries().size() == 1 && !hasExternalCauseCodeAdvice) {
          advices.add(TerminologyUtility.getAdvice(mapProject, externalCauseCodeAdvice));
          hasExternalCauseCodeAdvice = true;
        } else {
          boolean found = false;
          for (int i = 1; i < mapRecord.getMapEntries().size(); i++) {
            // If external cause code found, set flag
            if (mapRecord.getMapEntries().get(i).getTargetId() != null
                && mapRecord.getMapEntries().get(i).getTargetId().matches("^[VWXY].*")) {
              found = true;
              break;
            }
          }
          if (!found && !hasExternalCauseCodeAdvice) {
            advices.add(TerminologyUtility.getAdvice(mapProject, externalCauseCodeAdvice));
            hasExternalCauseCodeAdvice = true;
          } else if (found && hasExternalCauseCodeAdvice) {
            advices.remove(TerminologyUtility.getAdvice(mapProject, externalCauseCodeAdvice));
            hasExternalCauseCodeAdvice = false;
          }
        }
      }

      //
      // PREDICATE: K52.1 code without advice
      // "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE"
      // and without secondary external cause code
      // ACTION: add the advice
      //
      if (mapEntry.getTargetId().equals("K52.1")) {
        if (mapRecord.getMapEntries().size() == 1 && !hasExternalCauseCodeAdvice) {
          advices.add(TerminologyUtility.getAdvice(mapProject, externalCauseCodeAdvice));
          hasExternalCauseCodeAdvice = true;
        } else {
          boolean found = false;
          for (int i = 1; i < mapRecord.getMapEntries().size(); i++) {
            // If external cause code found, set flag
            if (mapRecord.getMapEntries().get(i).getTargetId() != null
                && mapRecord.getMapEntries().get(i).getTargetId().matches("^[VWXY].*")) {
              found = true;
              break;
            }
          }
          if (!found && !hasExternalCauseCodeAdvice) {
            advices.add(TerminologyUtility.getAdvice(mapProject, externalCauseCodeAdvice));
            hasExternalCauseCodeAdvice = true;
          } else if (found && hasExternalCauseCodeAdvice) {
            advices.remove(TerminologyUtility.getAdvice(mapProject, externalCauseCodeAdvice));
            hasExternalCauseCodeAdvice = false;
          }
        }
      }

      //
      // PREDICATE: Map target is between I20-I25 and I60-64 and does not have
      // the advice
      // "USE ADDITIONAL CODE TO IDENTIFY THE PRESENCE OF HYPERTENSION"
      // ACTION: add the advice
      //
      final String hypertensionAdvice =
          "USE ADDITIONAL CODE TO IDENTIFY THE PRESENCE OF HYPERTENSION";

      if ((mapEntry.getTargetId().matches("(^I2[0-5]).*")
          || mapEntry.getTargetId().matches("(^I6[0-4]).*"))
          && !TerminologyUtility.hasAdvice(mapEntry, hypertensionAdvice)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, hypertensionAdvice));
      } else if (TerminologyUtility.hasAdvice(mapEntry, hypertensionAdvice)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, hypertensionAdvice));
      }

      //
      // PREDICATE: If a concept and/or ICD-10 code description contains the
      // word "hereditary" then it shouldn't have advice for external cause
      // code.
      //
      if (hasExternalCauseCodeAdvice
          && (mapRecord.getConceptName().toLowerCase().contains("hereditary")
              || mapEntry.getTargetName().toLowerCase().contains("hereditary"))) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, externalCauseCodeAdvice));
        hasExternalCauseCodeAdvice = false;
      }

      //
      // PREDICATE: Code range I20.0 through I25.9 and I60.0 through I69.8
      // should not have advice POSSIBLE REQUIREMENT FOR ADDITIONAL CODE TO
      // FULLY DESCRIBE THE DISEASE OR CONDITION.
      //
      if (TerminologyUtility.hasAdvice(mapEntry,
          "POSSIBLE REQUIREMENT FOR ADDITIONAL CODE "
              + "TO FULLY DESCRIBE THE DISEASE OR CONDITION")
          && mapEntry.getTargetId().matches("^I2[0-5].*")) {
        advices.remove(
            TerminologyUtility.getAdvice(mapProject, "POSSIBLE REQUIREMENT FOR ADDITIONAL CODE "
                + "TO FULLY DESCRIBE THE DISEASE OR CONDITION"));
      }

      //
      // PREDICATE: Any code from category E10, Insulin-dependent diabetes
      // mellitus, should not have advice POSSIBLE REQUIREMENT FOR EXTERNAL
      // CAUSE CODE.
      //
      if (hasExternalCauseCodeAdvice && mapEntry.getTargetId().startsWith("E10")) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, externalCauseCodeAdvice));
        hasExternalCauseCodeAdvice = false;
      }

      //
      // PREDICATE: code in range S00-T98 without a subsequent target code from
      // V01-Y98
      // ACTION: replace "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE" with
      // "MANDATORY REQUIREMENT FOR AN EXTERNAL CAUSE CODE".
      //
      final String mandatoryExternalCauseCodeAdvice =
          "MANDATORY REQUIREMENT FOR AN EXTERNAL CAUSE CODE";

      if (mapEntry.getTargetId().startsWith("S")
          || mapEntry.getTargetId().startsWith("T") && hasExternalCauseCodeAdvice) {
        boolean found = false;
        for (int i = 1; i < mapRecord.getMapEntries().size(); i++) {
          // If V01-Y98 code found, set flag
          if (mapRecord.getMapEntries().get(i).getTargetId() != null
              && mapRecord.getMapEntries().get(i).getTargetId().matches("^[VWXY].*")) {
            found = true;
            break;
          }
        }
        if (!found && hasExternalCauseCodeAdvice) {
          advices.remove(TerminologyUtility.getAdvice(mapProject, externalCauseCodeAdvice));
          if (!TerminologyUtility.hasAdvice(mapEntry, mandatoryExternalCauseCodeAdvice)) {
            advices.add(TerminologyUtility.getAdvice(mapProject, mandatoryExternalCauseCodeAdvice));
          }
          hasExternalCauseCodeAdvice = false;
        }
      }

      //
      // PREDICATE: primary code has advice
      // "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE"
      // and there is a secondary code from chapter XX
      // ACTION: remove the advice
      //
      if (hasExternalCauseCodeAdvice && mapRecord.getMapEntries().size() > 1) {
        for (int i = 1; i < mapRecord.getMapEntries().size(); i++) {
          // If external cause code found, move on
          if (mapRecord.getMapEntries().get(i).getTargetId() != null
              && mapRecord.getMapEntries().get(i).getTargetId().matches("^[VWXY].*")) {
            advices.remove(TerminologyUtility.getAdvice(mapProject, externalCauseCodeAdvice));
            hasExternalCauseCodeAdvice = false;
            break;
          }
        }
      }

      //
      // PREDICATE: code is listed in CODE_TO_ADVICES.txt
      // ACTION: add the listed advice(s)
      //
      if (codeToAdvicesMap.containsKey(mapEntry.getTargetId())) {
        for (final String adviceStr : codeToAdvicesMap.get(mapEntry.getTargetId())) {
          if (!TerminologyUtility.hasAdvice(mapEntry, adviceStr)) {
            advices.add(TerminologyUtility.getAdvice(mapProject, adviceStr));
          }
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

    try {

      if (existingIcd10Maps.isEmpty()) {
        cacheExistingMaps();
      }

      MapRecord existingMapRecord = existingIcd10Maps.get(mapRecord.getConceptId());

      // Run existing map record through standard map advice and relation
      // calculation
      if (existingMapRecord != null) {
        List<MapEntry> updatedMapEntries = new ArrayList<>();

        for (MapEntry mapEntry : existingMapRecord.getMapEntries()) {
          MapRelation mapRelation = computeMapRelation(mapRecord, mapEntry);
          MapAdviceList mapAdvices = computeMapAdvice(mapRecord, mapEntry);
          mapEntry.setMapRelation(mapRelation);
          mapEntry.getMapAdvices().addAll(mapAdvices.getMapAdvices());
          updatedMapEntries.add(mapEntry);
        }

        existingMapRecord.setMapEntries(updatedMapEntries);
      }

      return existingMapRecord;
    } catch (Exception e) {
      throw e;
    } finally {
      // n/a
    }
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
  public void computeTargetTerminologyNotes(List<TreePosition> treePositionList) throws Exception {

    Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
        .info("Computing target terminology notes.");
    cacheCodes();

    // for each tree position initially passed in, call the recursive helper
    for (final TreePosition tp : treePositionList) {
      computeTargetTerminologyNotesHelper(tp, asteriskRefSetId, daggerRefSetId);
    }

  }

  /**
   * Compute target terminology notes helper.
   * 
   * @param treePosition the tree position
   * @param asteriskRefSetId the asterisk ref set id
   * @param daggerRefSetId the dagger ref set id
   * @throws Exception the exception
   */
  private void computeTargetTerminologyNotesHelper(TreePosition treePosition,
    String asteriskRefSetId, String daggerRefSetId) throws Exception {

    Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
        .info("Computing target terminology note for " + treePosition.getTerminologyId());

    // initially set the note to an empty string
    treePosition.setTerminologyNote("");

    // Simple lookup here
    if (asteriskCodes.contains(treePosition.getTerminologyId())) {
      treePosition.setTerminologyNote("*");
    } else if (asteriskCodes.contains(treePosition.getTerminologyId())) {
      treePosition.setTerminologyNote("\u2020");
    }

    // if this tree position has children, set their terminology notes
    // recursively
    for (final TreePosition child : treePosition.getChildren()) {
      computeTargetTerminologyNotesHelper(child, asteriskRefSetId, daggerRefSetId);
    }

  }

  /* see superclass */
  @Override
  public Set<String> getDependentModules() {

    Set<String> moduleDependencies = new HashSet<>();
    moduleDependencies.add("900000000000012004");
    moduleDependencies.add("900000000000207008");
    return moduleDependencies;

  }

  /* see superclass */
  @Override
  public String getModuleDependencyRefSetId() {
    return "900000000000534007";
  }

  /* see superclass */
  @Override
  public ValidationResult validateForRelease(ComplexMapRefSetMember member) throws Exception {
    ValidationResult result = super.validateForRelease(member);

    // Verify mapTarget is not null when mapCategory is 447637006 or 447639009
    // 447637006|Map source concept is properly classified
    // 447639009|Map of source concept is context dependent (also applies to
    // gender)
    if (member.getMapTarget().isEmpty()
        && member.getMapRelationId().equals(Long.valueOf("447637006"))) {
      result.addError("Map has empty target with map category 447637006 - " + member);
    }
    if (member.getMapTarget().isEmpty()
        && member.getMapRelationId().equals(Long.valueOf("447639009"))) {
      result.addError("Map has empty target with map category 447639009 - " + member);
    }

    // Verify mapTarget is null when mapCategory is not 447637006 or 447639009
    if (!member.getMapTarget().isEmpty()
        && !member.getMapRelationId().equals(Long.valueOf("447637006"))
        && !member.getMapRelationId().equals(Long.valueOf("447639009"))) {
      result.addError("Map has non-empty target without map category 447639009 or 447637006  - "
          + member.getMapRelationId());
    }

    // Verify IFA rules with mapTargets have 447639009 mapCategory
    if (member.getMapRule().startsWith("IFA") && !member.getMapTarget().isEmpty()
        && !member.getMapRelationId().equals(Long.valueOf("447639009"))) {
      result.addError("IFA map has category other than 447639009 - " + member);
    }

    // Verify higher map groups do not have only NC nodes
    // check when group goes back to 1
    if (member.getMapGroup() != qaPrevGroup && qaPrevGroup > 1) {
      if (qaOnlyNc) {
        result
            .addError("Higher map group has only NC nodes - " + qaPrevConcept + ", " + qaPrevGroup);
      }
      // this starts true
      qaOnlyNc = true;
    }
    if (member.getMapGroup() > 1 && !member.getMapTarget().isEmpty()) {
      qaOnlyNc = false;
    }

    // Verify TRUE rules do not appear before IFA rules
    if (member.getMapGroup() != qaPrevGroup || member.getMapGroup() == 1) {
      // reset flag when group changes
      qaTrueRuleInGroup = false;
    }
    if (member.getMapRule().equals("TRUE") || member.getMapRule().equals("OTHERWISE TRUE")) {
      // mark finding a true rule
      qaTrueRuleInGroup = true;
    } else if (member.getMapRule().startsWith("IFA") && qaTrueRuleInGroup) {
      // error if an "ifa" rule is found in the group while a true rule exists
      result.addError("TRUE rule before end of group " + member);
    }

    // Verify IFA rules refer to valid conceptId
    // -- all concepts are looked up and fail if not found

    // Verify AGE rules do not end with <= 0
    // -- not possible given new age ranges - this had to do with cartographer

    // Verify each mapRule has valid syntax.
    // It was difficult to create an LR(1) compliant grammar for the map rule
    // so we settled for validating map rule clauses. Though, because " AND "
    // appears
    // in SNOMED preferred names, we had to ignore those cases
    // see maprule.abnf for grammar
    // TODO: ideally this should use a better parser with a full implemenation

    // OK disabled because needs to accommodate UTF8 characters also
    // for (final String rule : member.getMapRule().split("AND IFA")) {
    // // replace IFA part of the rule
    // if (!rule.startsWith("IFA") && !rule.equals("TRUE") &&
    // !rule.equals("OTHERWISE TRUE")) {
    // rule = "IFA" + rule;
    // }
    // // skip where there are embedded parens, the parser can't handle this
    // if (rule.indexOf('(') != rule.lastIndexOf('(')) {
    // continue;
    // }
    // boolean isMatch = parser.parse(new
    // ByteArrayInputStream(rule.getBytes()));
    // if (!isMatch) {
    // result.addError("Rule clause has incorrect grammar: " + rule);
    // }
    // }

    // Verify mapAdvice is restricted to the defined list
    // -- all map advices are controlled at project level now

    // Verify mapAdvice is not duplicated ...Wed Dec 17 00:41:49 PST 2014
    // -- advice is a set so it can't be duplicated

    // Verify that for empty target codes the advice contains
    // the reason for the null code (e.g. an advice that is
    // allowable for a null target).
    if (member.getMapTarget().isEmpty()) {
      boolean found = false;
      for (final MapAdvice advice : mapProject.getMapAdvices()) {
        if (member.getMapAdvice().contains(advice.getName())
            && !advice.isAllowableForNullTarget()) {
          result.addError("Empty target with advice not allowed - " + member);
        } else if (member.getMapAdvice().contains(advice.getName())
            && advice.isAllowableForNullTarget() && found) {
          result.addError("Empty target with too many advice values - " + member);
        } else if (member.getMapAdvice().contains(advice.getName())
            && advice.isAllowableForNullTarget() && !found) {
          found = true;
        }
      }
    }

    // Verify HLC concepts must not have explicit concept exclusion rules ...Wed
    // -- up propagation checks the threshold already - this is to
    // expensive to double-check again here

    // Verify advice MAP IS CONTEXT DEPENDENT FOR GENDER should only apply to
    // gender rules
    if (member.getMapAdvice().contains("MAP IS CONTEXT DEPENDENT FOR GENDER")
        && !member.getMapRule().contains("| Male (finding) |")
        && !member.getMapRule().contains("| Female (finding) |")) {
      result.addError("GENDER advice without gender rule - " + member);
    }
    if (!member.getMapAdvice().contains("MAP IS CONTEXT DEPENDENT FOR GENDER")
        && (member.getMapRule().contains("| Male (finding) |")
            || member.getMapRule().contains("| Female (finding) |"))) {
      result.addError("Gender rulel without GENDER advice - " + member);
    }

    // Verify advice MAP IS CONTEXT DEPENDENT FOR GENDER is not used in
    // conjunction with CD advice ...Wed Dec 17 00:41:58 PST 2014
    if (member.getMapAdvice().contains("MAP IS CONTEXT DEPENDENT FOR GENDER")
        && member.getMapAdvice().contains(" MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT")) {
      result.addError("Gender rule contains invalid CONTEXT DEPENDENT advice - " + member);
    }

    // Verify map advice is sorted ...Wed Dec 17 00:41:58 PST 2014
    // -- advice is sort uniqued when created:
    // sortedAdvices = new ArrayList<>(new HashSet<>(sortedAdvices));
    // Collections.sort(sortedAdvices);
    // for (final String advice : sortedAdvices) {
    // mapAdviceStr += (mapAdviceStr.length() != 0 ? " | " : "") + advice;
    // }

    // Verify referencedComponentId in valid
    // -- concepts are looked up when build occurs and reported then

    // Verify refSetId and module id are valid
    // -- Verified by release mojo

    // Verify moduleId ss RefSet file is moduleId of map file ...Wed Dec 17
    if (!member.getModuleId().equals(Long.valueOf("449080006"))) {
      result.addError("Module id is wrong - " + member);
    }

    // Verify all referencedComponentId are Clinical Finding, Event, or
    // Situation
    // -- scope is defined at project level and "begin release" verifies
    // that all in scope concepts are mapped.

    // Group QA
    // Groups are consecutive starting with 1
    if (member.getMapGroup() != qaPrevGroup && member.getMapGroup() != 1
        && member.getMapGroup() != qaPrevGroup + 1) {
      result.addError("Groups are not consecutive starting with 1 - " + member);
    }

    // Priorities within a group are consecutive and starting with 1
    if (member.getMapGroup() == qaPrevGroup && member.getMapPriority() != 1
        && member.getMapPriority() != qaPrevPriority + 1) {
      result.addError(
          "Priorities are not consecutive starting with 1 - " + qaPrevGroup + ", " + member);

    }

    qaPrevGroup = member.getMapGroup();
    qaPrevPriority = member.getMapPriority();
    qaPrevConcept = member.getConcept().getTerminologyId();
    return result;

  }

  /* see superclass */
  @Override
  public MapRelation getDefaultUpPropagatedMapRelation() throws Exception {
    MappingService mappingService = new MappingServiceJpa();
    for (final MapRelation rel : mappingService.getMapRelations().getMapRelations()) {
      if (rel.getTerminologyId().equals("447639009")) {
        mappingService.close();
        return rel;
      }
    }
    mappingService.close();
    return null;
  }

  /* see superclass */
  @Override
  public void computeIdentifyAlgorithms(MapRecord mapRecord) throws Exception {
    // do nothing
  }

  /* see superclass */
  @Override
  public Map<String, String> getAllTerminologyNotes() throws Exception {
    final Map<String, String> map = new HashMap<>();
    cacheCodes();
    for (final String code : asteriskCodes) {
      if (isTargetCodeValid(code)) {
        map.put(code, "*");
      }
    }
    for (final String code : daggerCodes) {
      if (isTargetCodeValid(code)) {
        map.put(code, "\u2020");
      }
    }
    return map;
  }

  /**
   * Returns the icd10 accidental poisoning codes. For descendants of 72431002
   * (accidental poisoning)
   * @return the icd10 accidental poisoning codes
   */
  private static Set<String> getIcd10AccidentalPoisoningCodes() {
    final String key = "accidental";
    if (!externalCauseCodesMap.containsKey(key)) {
      final Set<String> accidentalCodes = new HashSet<>();
      // These codes come from columns 2/3/5 of the TEIL3.ASC index table
      accidentalCodes.addAll(Arrays.asList(new String[] {
          "X40", "X41", "X42", "X43", "X44", "X45", "X46", "X47", "X48", "X49"
      }));
      externalCauseCodesMap.put(key, accidentalCodes);
    }
    return externalCauseCodesMap.get(key);
  }

  /**
   * Returns the icd10 intentional poisoning codes. For descendants of 410061008
   * (intensional poisoining)
   * @return the icd10 intentional poisoning codes
   */
  private static Set<String> getIcd10IntentionalPoisoningCodes() {
    final String key = "intentional";
    if (!externalCauseCodesMap.containsKey(key)) {
      final Set<String> intentionalCodes = new HashSet<>();
      // These codes come from columns 2/3/5 of the TEIL3.ASC index table
      intentionalCodes.addAll(Arrays.asList(new String[] {
          "X60", "X61", "X62", "X63", "X66", "X65", "X66", "X67", "X68", "X69"
      }));
      externalCauseCodesMap.put(key, intentionalCodes);
    }
    return externalCauseCodesMap.get(key);
  }

  /**
   * Returns the icd10 undetermined poisoning codes. For descendants of
   * 269736006 (poisoning of undetermined intent)
   * @return the icd10 undetermined poisoning codes
   */
  private static Set<String> getIcd10UndeterminedPoisoningCodes() {
    final String key = "undetermined";
    if (!externalCauseCodesMap.containsKey(key)) {
      final Set<String> undeterminedCodes = new HashSet<>();
      // These codes come from columns 2/3/5 of the TEIL3.ASC index table
      undeterminedCodes.addAll(Arrays.asList(new String[] {
          "Y10", "Y11", "Y12", "Y13", "Y16", "Y15", "Y16", "Y17", "Y18", "Y19"
      }));
      externalCauseCodesMap.put(key, undeterminedCodes);
    }
    return externalCauseCodesMap.get(key);
  }

  /**
   * Returns the icd10 adverse effect poisoning codes. For descendants of
   * 281647001 (adverse reaction)
   * @return the icd10 adverse effect poisoning codes
   */
  private static Set<String> getIcd10AdverseEffectPoisoningCodes() {
    final String key = "adverseEffect";
    if (!externalCauseCodesMap.containsKey(key)) {
      final Set<String> adverseEffectCodes = new HashSet<>();
      // These codes come from columns 2/3/5 of the TEIL3.ASC index table
      adverseEffectCodes.addAll(Arrays.asList(new String[] {
          "Y40.0", "Y40.1", "Y40.3", "Y40.5", "Y40.6", "Y40.7", "Y40.8", "Y40.9", "Y41.0", "Y41.1",
          "Y41.2", "Y41.3", "Y41.4", "Y41.5", "Y41.8", "Y41.9", "Y42.2", "Y42.3", "Y42.4", "Y42.5",
          "Y42.6", "Y42.7", "Y42.8", "Y43.0", "Y43.1", "Y43.2", "Y43.3", "Y43.4", "Y43.5", "Y43.6",
          "Y43.8", "Y44.2", "Y44.3", "Y44.5", "Y44.6", "Y44.9", "Y45.0", "Y45.1", "Y45.2", "Y45.3",
          "Y45.4", "Y45.5", "Y45.8", "Y45.9", "Y46.0", "Y46.1", "Y46.2", "Y46.3", "Y46.6", "Y46.7",
          "Y46.8", "Y47.0", "Y47.1", "Y47.4", "Y47.8", "Y47.9", "Y48.0", "Y48.1", "Y48.2", "Y48.3",
          "Y48.4", "Y49.0", "Y49.1", "Y49.2", "Y49.3", "Y49.5", "Y49.7", "Y49.8", "Y50.0", "Y50.1",
          "Y51.0", "Y51.1", "Y51.3", "Y51.4", "Y51.5", "Y51.6", "Y51.7", "Y51.8", "Y51.9", "Y52.0",
          "Y52.2", "Y52.3", "Y52.4", "Y52.5", "Y52.6", "Y52.7", "Y52.8", "Y52.9", "Y53.1", "Y53.2",
          "Y53.4", "Y53.5", "Y53.6", "Y53.7", "Y53.8", "Y54.0", "Y54.1", "Y54.2", "Y54.3", "Y54.5",
          "Y54.6", "Y54.7", "Y54.8", "Y55.1", "Y55.3", "Y55.4", "Y55.5", "Y55.6", "Y55.7", "Y56.0",
          "Y56.1", "Y56.2", "Y56.3", "Y56.4", "Y56.5", "Y56.6", "Y57.0", "Y57.1", "Y57.2", "Y57.3",
          "Y57.5", "Y57.6", "Y57.7", "Y57.8", "Y59.3"
      }));
      externalCauseCodesMap.put(key, adverseEffectCodes);
    }
    return externalCauseCodesMap.get(key);
  }

  /**
   * Returns the icd10 external cause codes.
   *
   * @return the icd10 external cause codes
   */
  private static Set<String> getIcd10ExternalCauseCodes() {
    final Set<String> externalCauseCodes = new HashSet<>();
    externalCauseCodes.addAll(getIcd10AdverseEffectPoisoningCodes());
    externalCauseCodes.addAll(getIcd10IntentionalPoisoningCodes());
    externalCauseCodes.addAll(getIcd10UndeterminedPoisoningCodes());
    externalCauseCodes.addAll(getIcd10AccidentalPoisoningCodes());
    return externalCauseCodes;
  }

  /**
   * Checks for use additional.
   *
   * @param concept the concept
   * @return true, if successful
   * @throws Exception the exception
   */
  @SuppressWarnings("unused")
  private boolean hasUseAdditional(Concept concept) throws Exception {
    for (final Description desc : concept.getDescriptions()) {
      if (desc.getTerm().matches("Use additional code.*infectious agent.*")) {
        return true;
      } else if (desc.getTerm().matches("Use additional code.*bacterial agent.*")) {
        return true;
      }
    }

    final List<Concept> parents = TerminologyUtility.getActiveParents(concept);
    for (final Concept parent : parents) {
      return hasUseAdditional(parent);
    }

    return false;
  }

  /**
   * Cache dagger, asterisk, and valid 3-digit codes.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings({
      "unchecked"
  })
  private void cacheCodes() throws Exception {

    // lazy initialize
    if (!asteriskCodes.isEmpty()) {
      return;
    }

    Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
        .info("Caching the asterisk and dagger codes");

    final ContentServiceJpa contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();
    final EntityManager manager = contentService.getEntityManager();
    try {
      // open the metadata service and get the relationship types
      Map<String, String> simpleRefSets = metadataService.getSimpleRefSets(
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      // find the dagger/asterisk types
      for (final String key : simpleRefSets.keySet()) {
        if (simpleRefSets.get(key).equals("Asterisk refset"))
          asteriskRefSetId = key;
        if (simpleRefSets.get(key).equals("Dagger refset"))
          daggerRefSetId = key;
      }

      if (asteriskRefSetId == null)
        Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
            .warn("Could not find Asterisk refset");

      if (daggerRefSetId == null)
        Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
            .warn("Could not find Dagger refset");

      // Look up asterisk codes
      final javax.persistence.Query asteriskQuery = manager.createQuery(
          "select m.concept from SimpleRefSetMemberJpa m " + "where m.terminology = :terminology "
              + "and m.terminologyVersion = :terminologyVersion " + "and m.refSetId = :refSetId ");
      asteriskQuery.setParameter("terminology", mapProject.getDestinationTerminology());
      asteriskQuery.setParameter("terminologyVersion",
          mapProject.getDestinationTerminologyVersion());
      asteriskQuery.setParameter("refSetId", asteriskRefSetId);
      List<Concept> concepts = asteriskQuery.getResultList();
      for (final Concept concept : concepts) {
        asteriskCodes.add(concept.getTerminologyId());
      }

      // Look up dagger codes
      final javax.persistence.Query daggerQuery = manager.createQuery(
          "select m.concept from SimpleRefSetMemberJpa m " + "where m.terminology = :terminology "
              + "and m.terminologyVersion = :terminologyVersion " + "and m.refSetId = :refSetId ");
      daggerQuery.setParameter("terminology", mapProject.getDestinationTerminology());
      daggerQuery.setParameter("terminologyVersion", mapProject.getDestinationTerminologyVersion());
      daggerQuery.setParameter("refSetId", daggerRefSetId);
      concepts = daggerQuery.getResultList();
      for (final Concept concept : concepts) {
        daggerCodes.add(concept.getTerminologyId());
      }

      // Report to log
      Logger.getLogger(getClass()).info(" asterisk codes = " + asteriskCodes);
      Logger.getLogger(getClass()).info(" dagger codes = " + daggerCodes);
    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
      metadataService.close();
    }
  }

  /**
   * Cache the explicit code to advices map.
   *
   * @throws Exception the exception
   */
  private void cacheCodeToAdvices() throws Exception {

    // Check if map is already cached
    if (!codeToAdvicesMap.isEmpty()) {
      return;
    }

    Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
        .info("Caching the code to advices map");

    codeToAdvicesMap = new HashMap<>();

    final Properties config = ConfigUtility.getConfigProperties();
    final String dataDir = config.getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Check preconditions
    if (!new File(dataDir + "/ICD10CA/CODES_TO_ADVICES.txt").exists()) {
      throw new Exception(
          "Specified input file missing: " + dataDir + "/ICD10CA/CODES_TO_ADVICES.txt");
    }

    // Open reader and service
    BufferedReader codeListReader =
        new BufferedReader(new FileReader(new File(dataDir + "/ICD10CA/CODES_TO_ADVICES.txt")));

    String line = null;

    while ((line = codeListReader.readLine()) != null) {
      String tokens[] = line.split("\t");
      final String code = tokens[0].trim();
      final String advice = tokens[1].trim();
      if (codeToAdvicesMap.get(code) == null) {
        codeToAdvicesMap.put(code, new HashSet<>());
      }
      final Set<String> advices = codeToAdvicesMap.get(code);
      advices.add(advice);
      codeToAdvicesMap.put(code, advices);
    }

    codeListReader.close();
  }

  /**
   * Cache existing maps.
   *
   * @throws Exception the exception
   */
  private void cacheExistingMaps() throws Exception {
    // Lookup if this concept has an existing ICD10 map record to pre-load
    // Up to date map release file must be saved here:
    // {data.dir}/doc/{projectNumber}/preloadMaps/ExtendedMapSnapshot.txt

    final ContentService contentService = new ContentServiceJpa();

    Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
        .info("Caching the existing ICD10 maps");

    final String dataDir = ConfigUtility.getConfigProperties().getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Check preconditions
    String inputFile =
        dataDir + "/doc/" + mapProject.getId() + "/preloadMaps/ExtendedMapSnapshot.txt";

    if (!new File(inputFile).exists()) {
      throw new Exception("Specified input file missing: " + inputFile);
    }

    // Preload all concepts and create terminologyId->name maps, to avoid having
    // to do individual lookups later
    ConceptList sourceConcepts = contentService.getAllConcepts(mapProject.getSourceTerminology(),
        mapProject.getSourceTerminologyVersion());
    ConceptList destinationConcepts = contentService.getAllConcepts(
        mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

    Map<String, String> sourceIdToName = new HashMap<>();
    Map<String, String> destinationIdToName = new HashMap<>();

    for (final Concept concept : sourceConcepts.getConcepts()) {
      sourceIdToName.put(concept.getTerminologyId(), concept.getDefaultPreferredName());
    }
    for (final Concept concept : destinationConcepts.getConcepts()) {
      destinationIdToName.put(concept.getTerminologyId(), concept.getDefaultPreferredName());
    }

    // There is a special-case that requires checking after all loading is
    // complete.
    final Set<String> conceptIdsForChecking = new HashSet<>();

    // sort input file
    Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
        .info("  Sorting the file into " + System.getProperty("java.io.tmpdir"));
    File sortedFile =
        File.createTempFile("ttt", ".sort", new File(System.getProperty("java.io.tmpdir")));
    sortedFile.delete();
    // Sort file according to unix sort
    // -k 5,5 -k 6,6n -k 7,7n -k 8,8n -k 1,4 -k 9,9 -k 10,10 -k 11,11
    // -k 12,12 -k 13,13
    FileSorter.sortFile(inputFile, sortedFile.getPath(), new Comparator<String>() {

      @Override
      public int compare(String o1, String o2) {
        String[] fields1 = o1.split("\t");
        String[] fields2 = o2.split("\t");

        // keep headers at top
        if (o1.startsWith("id")) {
          return 1;
        }

        long i = fields1[4].compareTo(fields2[4]);
        if (i != 0) {
          return (int) i;
        } else {
          i = fields1[5].compareTo(fields2[5]);
          // i = (Long.parseLong(fields1[5]) -
          // Long.parseLong(fields2[5]));
          if (i != 0) {
            return (int) i;
          } else {
            i = Long.parseLong(fields1[6]) - Long.parseLong(fields2[6]);
            if (i != 0) {
              return (int) i;
            } else {
              i = Long.parseLong(fields1[7]) - Long.parseLong(fields2[7]);
              if (i != 0) {
                return (int) i;
              } else {
                i = (fields1[0] + fields1[1] + fields1[2] + fields1[3])
                    .compareTo(fields1[0] + fields1[1] + fields1[2] + fields1[3]);
                if (i != 0) {
                  return (int) i;
                } else {
                  i = fields1[8].compareTo(fields2[8]);
                  if (i != 0) {
                    return (int) i;
                  } else {
                    i = fields1[9].compareTo(fields2[9]);
                    if (i != 0) {
                      return (int) i;
                    } else {
                      i = fields1[10].compareTo(fields2[10]);
                      if (i != 0) {
                        return (int) i;
                      } else {
                        i = fields1[11].compareTo(fields2[11]);
                        if (i != 0) {
                          return (int) i;
                        } else {

                          // complex maps do not have mapCategory field
                          if (fields1.length == 12) {
                            return 0;
                          }

                          // extended maps have extra mapCategory field
                          i = fields1[12].compareTo(fields2[12]);
                          if (i != 0) {
                            return (int) i;
                          } else {
                            return 0;
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    });
    Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class).info("  Done sorting the file ");

    // Open reader and service
    BufferedReader preloadMapReader = new BufferedReader(new FileReader(sortedFile));

    // These advices are not being used by ICD10CA, and need to be removed
    final Set<String> deletedAdvices = new HashSet<>();
    deletedAdvices.add("MAPPED FOLLOWING IHTSDO GUIDANCE");
    deletedAdvices.add("MAPPED FOLLOWING SNOMED GUIDANCE");
    deletedAdvices.add("POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE");
    deletedAdvices.add("THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION");
    deletedAdvices.add("FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE");
    deletedAdvices.add("THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION");
    deletedAdvices.add("DESCENDANTS NOT EXHAUSTIVELY MAPPED");

    // These advices have different wording in ICD10CA, and need to be
    // replaced
    final Map<String, String> changedAdvices = new HashMap<>();
    // changedAdvices.put("POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
    // "MANDATORY REQUIREMENT FOR AN EXTERNAL CAUSE CODE");
    changedAdvices.put("MAPPED FOLLOWING WHO GUIDANCE", "MAPPED FOLLOWING CIHI GUIDANCE");
    changedAdvices.put("POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE",
        "MANDATORY REQUIREMENT FOR PLACE OF OCCURRENCE");
    changedAdvices.put("MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT",
        "MAP IS CONTEXT DEPENDENT FOR AGE");
    changedAdvices.put("POSSIBLE REQUIREMENT FOR CAUSATIVE AGENT CODE",
        "POSSIBLE REQUIREMENT FOR INFECTIOUS AGENT WHEN DOCUMENTED");
    changedAdvices.put("MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
        "NOT CLASSIFIABLE IN ICD-10-CA");
    changedAdvices.put("SOURCE SNOMED CONCEPT IS AMBIGUOUS", "NOT CLASSIFIABLE IN ICD-10-CA");
    changedAdvices.put("MAPPING GUIDANCE FROM WHO IS AMBIGUOUS", "NOT CLASSIFIABLE IN ICD-10-CA");
    changedAdvices.put("SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED",
        "NOT CLASSIFIABLE IN ICD-10-CA");

    String line = null;

    while ((line = preloadMapReader.readLine()) != null) {
      String fields[] = line.split("\t");

      // Only keep ICD10 maps (refset=447562003)
      if (!fields[4].equals("447562003")) {
        continue;
      }

      // Only keep active maps
      if (!fields[2].equals("1")) {
        continue;
      }

      final String conceptId = fields[5];
      final String mapGroup = fields[6];
      final String mapPriority = fields[7];
      final String mapRule = fields[8];
      final String mapAdviceStr = fields[9];
      final String mapTarget = fields[10];
      final String correlationId = fields[11];
      final String mapCategoryId = fields[12];

      // The first time a conceptId is encountered, set up the map (only need
      // very limited information for the purpose of this function
      if (existingIcd10Maps.get(conceptId) == null) {
        MapRecord icd10MapRecord = new MapRecordJpa();
        icd10MapRecord.setConceptId(conceptId);
        String sourceConceptName = sourceIdToName.get(conceptId);
        if (sourceConceptName != null) {
          icd10MapRecord.setConceptName(sourceConceptName);
        } else {
          icd10MapRecord
              .setConceptName("CONCEPT DOES NOT EXIST IN " + mapProject.getSourceTerminology());
        }

        existingIcd10Maps.put(conceptId, icd10MapRecord);
      }
      MapRecord icd10MapRecord = existingIcd10Maps.get(conceptId);

      // For each line, create a map entry, and attach it to the record
      MapEntry mapEntry = new MapEntryJpa();
      mapEntry.setMapGroup(Integer.parseInt(mapGroup));
      mapEntry.setMapPriority(Integer.parseInt(mapPriority));
      mapEntry.setTargetId(mapTarget);
      String targetConceptName = destinationIdToName.get(mapTarget);

      if (targetConceptName != null) {
        mapEntry.setTargetName(targetConceptName);
      } else {
        mapEntry
            .setTargetName("CONCEPT DOES NOT EXIST IN " + mapProject.getDestinationTerminology());
      }

      // Load map rule
      if (mapRule.equals("OTHERWISE TRUE")) {
        mapEntry.setRule("TRUE");
      } else {
        mapEntry.setRule(mapRule);
      }

      // load map relation
      for (final MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals(mapCategoryId)) {
          mapEntry.setMapRelation(relation);
          continue;
        }
      }

      // Load map advices
      // Advices aren't identical between ICD10 and ICD10CA, so some
      // manipulation is required

      final Set<MapAdvice> advices = new HashSet<>();

      String splitAdvices[] = mapAdviceStr.split("\\|");
      for (String advice : splitAdvices) {
        // skip IF/ALWAYS
        if (advice.trim().startsWith("ALWAYS ") || advice.trim().startsWith("IF ")) {
          continue;
        }
        // don't add advices that are duplicates of the map relation
        else if (mapEntry.getMapRelation().getName().equals(advice.trim())) {
          continue;
        }
        // don't add advices that have been deleted by ICD10CA
        else if (deletedAdvices.contains(advice.trim())) {
          continue;
        }
        // modify and add advices that have been changed by ICD10CA
        else if (changedAdvices.keySet().contains(advice.trim())) {
          advices.add(TerminologyUtility.getAdvice(mapProject, changedAdvices.get(advice.trim())));
        }
        // Otherwise, add the advice as-is
        else {
          MapAdvice adviceToAdd = null;
          try {
            adviceToAdd = TerminologyUtility.getAdvice(mapProject, advice.trim());
          } catch (Exception e) {
            Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
                .warn("Advice not found: " + advice.trim());
          }
          advices.add(adviceToAdd);
        }

        // "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE" is a special case,
        // and needs to be revisited once all entries are added
        if (advice.trim().equals("POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE")) {
          conceptIdsForChecking.add(conceptId);
        }
      }

      // Add "USE ADDITIONAL CODE TO IDENTIFY THE PRESENCE OF HYPERTENSION" to
      // any target code from I20-I25 and I60-64
      if ((mapTarget.trim().matches("(^I2[0-5]).*") || mapTarget.trim().matches("(^I6[0-4]).*"))) {
        advices.add(TerminologyUtility.getAdvice(mapProject,
            "USE ADDITIONAL CODE TO IDENTIFY THE PRESENCE OF HYPERTENSION"));
      }

      mapEntry.setMapAdvices(advices);

      // Add the entry to the record, and put the updated record in the map
      icd10MapRecord.addMapEntry(mapEntry);
      existingIcd10Maps.put(conceptId, icd10MapRecord);

    }

    // Check all maps with "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE".
    // If the target code is in range S00-T98 without a subsequent target code
    // from
    // V01-Y98, the advice needs to be changed to
    // "MANDATORY REQUIREMENT FOR AN EXTERNAL CAUSE CODE"
    for (final String conceptId : conceptIdsForChecking) {
      MapRecord mapRecord = existingIcd10Maps.get(conceptId);
      for (MapEntry mapEntry : mapRecord.getMapEntries()) {
        if (mapEntry.getTargetId().startsWith("S") || mapEntry.getTargetId().startsWith("T")) {
          boolean found = false;
          for (int i = 1; i < mapRecord.getMapEntries().size(); i++) {
            // If V01-Y98 code found, set flag
            if (mapRecord.getMapEntries().get(i).getTargetId() != null
                && mapRecord.getMapEntries().get(i).getTargetId().matches("^[VWXY].*")) {
              found = true;
              break;
            }
          }
          if (!found) {
            mapEntry.getMapAdvices().remove(TerminologyUtility.getAdvice(mapProject,
                "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE"));
            mapEntry.getMapAdvices().add(TerminologyUtility.getAdvice(mapProject,
                "MANDATORY REQUIREMENT FOR AN EXTERNAL CAUSE CODE"));
          }
        }
      }
    }

    Logger.getLogger(getClass()).info("Done caching maps");

    preloadMapReader.close();
    contentService.close();

  }

}
