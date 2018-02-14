package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
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
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * The {@link ProjectSpecificAlgorithmHandler} for ICD10 projects.
 */
public class ICD10CMProjectSpecificAlgorithmHandler
    extends DefaultProjectSpecificAlgorithmHandler {

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

  /** The dagger codes. */
  private static Set<String> daggerCodes = null;

  /** The asterisk codes. */
  private static Set<String> asteriskCodes = null;
//
//  /** The valid3 digit codes. */
//  private static Set<String> valid3DigitCodes = new HashSet<>();

  /** The laterality codes. */
  private static Set<String> lateralityCodes = null;

  /** The additional advice codes. */
  private static Set<String> additionalAdviceCodes = null;;

  /** The asterisk ref set id. */
  private static String asteriskRefSetId;

  /** The dagger ref set id. */
  private static String daggerRefSetId;

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
  public ValidationResult validateTargetCodes(MapRecord mapRecord)
    throws Exception {

    final ValidationResult validationResult = new ValidationResultJpa();
    final ContentService contentService = new ContentServiceJpa();

    for (final MapEntry mapEntry : mapRecord.getMapEntries()) {

      // add an error if neither relation nor target are set
      if (mapEntry.getMapRelation() == null && (mapEntry.getTargetId() == null
          || mapEntry.getTargetId().equals(""))) {

        validationResult.addError(
            "A relation indicating the reason must be selected when no target is assigned.");

        // if a target is specified check it
      } else if (mapEntry.getTargetId() != null
          && !mapEntry.getTargetId().equals("")) {

        // first, check terminology id based on above rules
        if (!mapEntry.getTargetId().equals("")
            && (!mapEntry.getTargetId().matches(".[0-9].*")
                || mapEntry.getTargetId().contains("-"))) {
          validationResult
              .addError("Invalid target code " + mapEntry.getTargetId()
                  + "!  For ICD10, valid target codes must contain 3 digits and must not contain a dash."
                  + " Entry:"
                  + (mapProject.isGroupStructure() ? " group "
                      + Integer.toString(mapEntry.getMapGroup()) + "," : "")
                  + " map priority "
                  + Integer.toString(mapEntry.getMapPriority()));
        } else {

          // Validate the code
          if (!isTargetCodeValid(mapEntry.getTargetId())) {

            validationResult
                .addError("Target code " + mapEntry.getTargetId()
                    + " is an invalid code, use a child code instead. "
                    + " Entry:"
                    + (mapProject.isGroupStructure() ? " group "
                        + Integer.toString(mapEntry.getMapGroup()) + "," : "")
                    + " map  priority "
                    + Integer.toString(mapEntry.getMapPriority()));

          }

        }

        // otherwise, check that relation is assignable to null target
      } else {
        if (!mapEntry.getMapRelation().isAllowableForNullTarget()) {
          validationResult.addError(
              "The map relation " + mapEntry.getMapRelation().getName()
                  + " is not allowable for null targets");
        }
      }
    }

    contentService.close();
    return validationResult;

  }

  /**
   * Computes the map relation for the SNOMEDCT to ICD10CM map project. Based
   * solely on whether an entry has a TRUE rule or not. No advices are computed
   * for this project.
   *
   * @param mapRecord the map record
   * @param mapEntry the map entry
   * @return the map relation
   * @throws Exception the exception
   */
  @Override
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {

    if (mapEntry == null) {
      return null;
    }
    // if entry has no target
    if (mapEntry.getTargetId() == null || mapEntry.getTargetId().isEmpty()) {

      // if a relation is already set, and is allowable for null target,
      // keep it
      if (mapEntry.getMapRelation() != null
          && mapEntry.getMapRelation().isAllowableForNullTarget())
        return mapEntry.getMapRelation();
      else {
        // retrieve the not classifiable relation
        // 447638001 - Map source concept cannot be classified with
        // available
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
  public MapAdviceList computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {
    cacheCodes();
    cacheLateralityCodes();
    cacheAdditionalAdviceCodes();

    final List<MapAdvice> advices = new ArrayList<>(mapEntry.getMapAdvices());
    final ContentService contentService = new ContentServiceJpa();

    try {

      final Concept concept = contentService.getConcept(mapEntry.getTargetId(),
          mapProject.getDestinationTerminology(),
          mapProject.getDestinationTerminologyVersion());
      // lazy initialize
      if (concept != null) {
        concept.getDescriptions().size();
        concept.getRelationships().size();
        concept.getInverseRelationships().size();
        concept.getSimpleRefSetMembers().size();
      } else {
        return new MapAdviceListJpa();
      }
      final Concept sourceConcept = contentService.getConcept(
          mapRecord.getConceptId(), mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());
      final List<Concept> descendants =
          TerminologyUtility.getActiveDescendants(sourceConcept);

      // lazy initialize
      if (sourceConcept != null) {
        sourceConcept.getDescriptions().size();
        sourceConcept.getRelationships().size();
        sourceConcept.getInverseRelationships().size();
        sourceConcept.getSimpleRefSetMembers().size();
      } else {
        return new MapAdviceListJpa();
      }

      // Remove any advices that are purely computed and keep only
      // manually
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
      // PREDICATE: All codes in range S00-T88, except T36-T65, unless
      // there is
      // a second map group with codes in this chapter External causes of
      // morbidity
      // (V00-Y99) and does not have the
      // advice "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE"
      // ACTION: add the advice
      //
      boolean found = false;
      for (MapEntry entry : mapRecord.getMapEntries()) {
        if (entry.getMapGroup() == 2 && entry.getTargetId() != null
            && entry.getTargetId().matches("(V..|W..|X..|Y..).*")) {
          found = true;
          break;
        }
      }
      final String adviceP01 =
          "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE";
      if (!found && mapEntry.getTargetId().matches("(S[0-9].|T[0-8][0-8]).*")
          && !mapEntry.getTargetId().matches("(T[3-9][6-9].|T[6-9][0-5]).*")) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP01)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP01));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP01)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP01));
      }

      //
      // PREDICATE: Map target code ends in '?' and does not have the
      // advice "EPISODE OF CARE INFORMATION NEEDED"
      // ACTION: add the advice
      //
      final String adviceP02 = "EPISODE OF CARE INFORMATION NEEDED";
      if (mapEntry.getTargetId().endsWith("?")) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP02)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP02));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP02)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP02));
      }

      //
      // PREDICATE: Map target code is in the annually updated (by NLM)
      // list of
      // laterality codes and does not have the
      // advice "CONSIDER LATERALITY SPECIFICATION"
      // ACTION: add the advice
      //
      final String adviceP03 = "CONSIDER LATERALITY SPECIFICATION";
      if (lateralityCodes.contains(mapEntry.getTargetId())) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP03)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP03));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP03)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP03));
      }

      //
      // PREDICATE: All target codes in the chapter Pregnancy, childbirth
      // and the puerperium (O00-O99),
      // with ‘unspecified trimester’ in their descriptions and does not
      // have the
      // advice "CONSIDER TRIMESTER SPECIFICATION"
      // ACTION: add the advice
      //
      final String adviceP04 = "CONSIDER TRIMESTER SPECIFICATION";
      if (mapEntry.getTargetId().startsWith("O") && (mapEntry.getTargetName()
          .toLowerCase().indexOf("unspecified trimester") != -1)) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP04)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP04));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP04)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP04));
      }

      //
      // PREDICATE: All target codes with these prefixes: O31, O32,
      // O33.3-O33.6, O35,
      // O36, O40, O41, O60.1-O60.2, O64, O69; and ending with the 7th
      // character= 0 (‘fetus unspecified’)
      // and does not have the advice
      // "CONSIDER WHICH FETUS IS AFFECTED BY THE MATERNAL CONDITION"
      // ACTION: add the advice
      //
      final String adviceP05 =
          "CONSIDER WHICH FETUS IS AFFECTED BY THE MATERNAL CONDITION";
      if ((mapEntry.getTargetId().startsWith("O31")
          || mapEntry.getTargetId().startsWith("O32")
          || mapEntry.getTargetId().matches("(O33.[3-6]).*")
          || mapEntry.getTargetId().startsWith("O35")
          || mapEntry.getTargetId().startsWith("O36")
          || mapEntry.getTargetId().startsWith("O40")
          || mapEntry.getTargetId().startsWith("O41")
          || mapEntry.getTargetId().matches("(O60.[1-2]).*")
          || mapEntry.getTargetId().startsWith("O64")
          || mapEntry.getTargetId().startsWith("O69"))
          && mapEntry.getTargetId().matches("\\D\\d{2}.\\w{3}0$")) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP05)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP05));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP05)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP05));
      }

      //
      // PREDICATE: All target codes in this chapter External causes of
      // morbidity (V00-Y99) that occur in Map Group 1 without advice
      // "THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION"
      // ACTION: add the advice
      //
      final String adviceP06 =
          "THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION";
      if (mapEntry.getTargetId().matches("^[VWXY].*")
          && mapEntry.getMapGroup() == 1) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP06)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP06));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP06)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP06));
      }

      //
      // PREDICATE: Primary map target is T31 and does not have the
      // advice
      // "USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE
      // USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)"
      // ACTION: add the advice
      //
      final String adviceP07 =
          "USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)";
      if (mapEntry.getTargetId().startsWith("T31")) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP07)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP07));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP07)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP07));
      }

      //
      // PREDICATE: Primary map target is T32 and does not have the
      // advice
      // "USE AS PRIMARY CODE ONLY IF SITE OF CORROSION UNSPECIFIED,
      // OTHERWISE
      // USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)"
      // ACTION: add the advice
      //
      final String adviceP08 =
          "USE AS PRIMARY CODE ONLY IF SITE OF CORROSION UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)";
      if (mapEntry.getTargetId().startsWith("T32")) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP08)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP08));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP08)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP08));
      }

      //
      // PREDICATE: All target codes with prefixes H40.10-H40.14, H40.20,
      // H40.22, H40.3-H40.6
      // and does not have the advice
      // "CONSIDER STAGE OF GLAUCOMA SPECIFICATION"
      // ACTION: add the advice
      //
      final String adviceP09 = "CONSIDER STAGE OF GLAUCOMA SPECIFICATION";
      if (mapEntry.getTargetId().startsWith("H40.20")
          || mapEntry.getTargetId().startsWith("H40.22")
          || mapEntry.getTargetId().matches("(^H40.1).*")
          || mapEntry.getTargetId().matches("(^H40.[3-6]).*")) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP09)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP09));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP09)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP09));
      }

      // RAW: Removed 2/2/2018 based on client request
      //
      // PREDICATE: All target codes with prefix M1A and does not have the
      // advice "CONSIDER TOPHUS SPECIFICATION"
      // ACTION: add the advice
      //
      final String adviceP10 = "CONSIDER TOPHUS SPECIFICATION";
//      if (mapEntry.getTargetId().startsWith("M1A")) {
//        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP10)) {
//          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP10));
//        }
//      } else 
        if (TerminologyUtility.hasAdvice(mapEntry, adviceP10)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP10));
      }

      //
      // PREDICATE: All target codes with prefix R40.2 and does not have
      // the
      // advice "CONSIDER TIME OF COMA SCALE SPECIFICATION"
      // ACTION: add the advice
      //
      final String adviceP11 = "CONSIDER TIME OF COMA SCALE SPECIFICATION";
      if (mapEntry.getTargetId().startsWith("R40.2")) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP11)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP11));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP11)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP11));
      }

      //
      //
      // PREDICATE: Map target code is in the annually updated (by NLM)
      // list of
      // additional advice codes and does not have the
      // advice "CONSIDER ADDITIONAL CODE TO
      // IDENTIFY SPECIFIC CONDITION OR DISEASE"
      // ACTION: add the advice
      //
      final String adviceP12 =
          "CONSIDER ADDITIONAL CODE TO IDENTIFY SPECIFIC CONDITION OR DISEASE";
      if (additionalAdviceCodes.contains(mapEntry.getTargetId())) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP12)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP12));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP12)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP12));
      }

      //
      // PREDICATE: All SNOMEDCT_US concepts that have more than 10 distinct
      // descendants should be give then advice
      // "DESCENDANTS NOT EXHAUSTIVELY MAPPED
      // ACTION: add the advice
      //

      final String descendantAdvice = "DESCENDANTS NOT EXHAUSTIVELY MAPPED";
      if (descendants.size() > 10) {
        if (!TerminologyUtility.hasAdvice(mapEntry, descendantAdvice)) {
          advices
              .add(TerminologyUtility.getAdvice(mapProject, descendantAdvice));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, descendantAdvice)) {
        advices
            .remove(TerminologyUtility.getAdvice(mapProject, descendantAdvice));
      }

      MapAdviceList mapAdviceList = new MapAdviceListJpa();
      mapAdviceList.setMapAdvices(advices);
      return mapAdviceList;
    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
  }

  /**
   * Helper method to calculate if concept or its ancestors match specified
   * descriptions indicating if advice should be added.
   * 
   * @param concept
   * @return
   * @throws Exception
   */
  private boolean isMatchingDescriptionNote(Concept concept) throws Exception {
    for (Description description : concept.getDescriptions()) {
      if (description.getTerm().toLowerCase().startsWith("use_additional")
          || description.getTerm().toLowerCase().startsWith("code_first")
          || description.getTerm().toLowerCase().startsWith("code_also")) {
        return true;
      }
    }
    // if we are already one level above the 3 character level (e.g.
    // T36-T50)
    // and we haven't found a match, we stop searching
    if (concept.getTerminologyId().contains("-")) {
      return false;
    }
    final List<Concept> parents = TerminologyUtility.getActiveParents(concept);
    for (Concept parent : parents) {
      return isMatchingDescriptionNote(parent);
    }
    return false;
  }

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {

    final ContentService contentService = new ContentServiceJpa();

    try {
      // check that code has at least three characters, that the second
      // character
      // is a number, and does not contain a dash
      if (!terminologyId.matches(".[0-9].*") || terminologyId.contains("-")) { // "(.*?[0-9]){3,}")
        // ||
        // terminologyId.contains("-"))
        // {
        return false;
      }

      // verify concept exists in database
      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getDestinationTerminology(),
          mapProject.getDestinationTerminologyVersion());

      TreePositionList list = contentService.getTreePositions(terminologyId,
          mapProject.getDestinationTerminology(),
          mapProject.getDestinationTerminologyVersion());
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
  public void computeTargetTerminologyNotes(List<TreePosition> treePositionList)
    throws Exception {

    Logger.getLogger(ICD10CMProjectSpecificAlgorithmHandler.class)
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

    Logger.getLogger(ICD10CMProjectSpecificAlgorithmHandler.class)
        .info("Computing target terminology note for "
            + treePosition.getTerminologyId());

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
      computeTargetTerminologyNotesHelper(child, asteriskRefSetId,
          daggerRefSetId);
    }

  }

  /* see superclass */
  @Override
  public Set<String> getDependentModules() {

    Set<String> moduleDependencies = new HashSet<>();
    return moduleDependencies;

  }

  /* see superclass */
  @Override
  public String getModuleDependencyRefSetId() {
    return "";
  }

  /* see superclass */
  @Override
  public ValidationResult validateForRelease(ComplexMapRefSetMember member)
    throws Exception {
    ValidationResult result = super.validateForRelease(member);

    // Verify mapTarget is not null when mapCategory is 447637006 or
    // 447639009
    // 447637006|Map source concept is properly classified
    // 447639009|Map of source concept is context dependent (also applies to
    // gender)
    if (member.getMapTarget().isEmpty()
        && member.getMapRelationId().equals(Long.valueOf("447637006"))) {
      result.addError(
          "Map has empty target with map category 447637006 - " + member);
    }
    if (member.getMapTarget().isEmpty()
        && member.getMapRelationId().equals(Long.valueOf("447639009"))) {
      result.addError(
          "Map has empty target with map category 447639009 - " + member);
    }

    // Verify mapTarget is null when mapCategory is not 447637006 or
    // 447639009
    if (!member.getMapTarget().isEmpty()
        && !member.getMapRelationId().equals(Long.valueOf("447637006"))
        && !member.getMapRelationId().equals(Long.valueOf("447639009"))) {
      result.addError(
          "Map has non-empty target without map category 447639009 or 447637006  - "
              + member.getMapRelationId());
    }

    // Verify IFA rules with mapTargets have 447639009 mapCategory
    if (member.getMapRule().startsWith("IFA")
        && !member.getMapTarget().isEmpty()
        && !member.getMapRelationId().equals(Long.valueOf("447639009"))) {
      result.addError("IFA map has category other than 447639009 - " + member);
    }

    // Verify higher map groups do not have only NC nodes
    // check when group goes back to 1
    if (member.getMapGroup() != qaPrevGroup && qaPrevGroup > 1) {
      if (qaOnlyNc) {
        result.addError("Higher map group has only NC nodes - " + qaPrevConcept
            + ", " + qaPrevGroup);
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
    if (member.getMapRule().equals("TRUE")
        || member.getMapRule().equals("OTHERWISE TRUE")) {
      // mark finding a true rule
      qaTrueRuleInGroup = true;
    } else if (member.getMapRule().startsWith("IFA") && qaTrueRuleInGroup) {
      // error if an "ifa" rule is found in the group while a true rule
      // exists
      result.addError("TRUE rule before end of group " + member);
    }

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
          result
              .addError("Empty target with too many advice values - " + member);
        } else if (member.getMapAdvice().contains(advice.getName())
            && advice.isAllowableForNullTarget() && !found) {
          found = true;
        }
      }
    }

    // Verify HLC concepts must not have explicit concept exclusion rules
    // ...Wed
    // -- up propagation checks the threshold already - this is too
    // expensive to double-check again here

    // Verify advice MAP IS CONTEXT DEPENDENT FOR GENDER should only apply
    // to
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
        && member.getMapAdvice()
            .contains(" MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT")) {
      result.addError(
          "Gender rule contains invalid CONTEXT DEPENDENT advice - " + member);
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

    // Verify moduleId ss RefSet file is moduleId of map file ...
    if (!member.getModuleId().equals(Long.valueOf("5991000124107"))) {
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
      result.addError("Priorities are not consecutive starting with 1 - "
          + qaPrevGroup + ", " + member);

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
    for (final MapRelation rel : mappingService.getMapRelations()
        .getMapRelations()) {
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
      } else if (desc.getTerm()
          .matches("Use additional code.*bacterial agent.*")) {
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
    if (asteriskCodes != null) {
      return;
    }

    asteriskCodes = new HashSet<>();
    daggerCodes = new HashSet<>();

    final ContentServiceJpa contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();
    final EntityManager manager = contentService.getEntityManager();
    try {
      // open the metadata service and get the relationship types
      Map<String, String> simpleRefSets = metadataService.getSimpleRefSets(
          mapProject.getDestinationTerminology(),
          mapProject.getDestinationTerminologyVersion());

      // find the dagger/asterisk types
      for (final String key : simpleRefSets.keySet()) {
        if (simpleRefSets.get(key).equals("Asterisk refset"))
          asteriskRefSetId = key;
        if (simpleRefSets.get(key).equals("Dagger refset"))
          daggerRefSetId = key;
      }

      if (asteriskRefSetId == null)
        Logger.getLogger(ICD10CMProjectSpecificAlgorithmHandler.class)
            .warn("Could not find Asterisk refset");

      if (daggerRefSetId == null)
        Logger.getLogger(ICD10CMProjectSpecificAlgorithmHandler.class)
            .warn("Could not find Dagger refset");

      // Look up asterisk codes
      final javax.persistence.Query asteriskQuery =
          manager.createQuery("select m.concept from SimpleRefSetMemberJpa m "
              + "where m.terminology = :terminology "
              + "and m.terminologyVersion = :terminologyVersion "
              + "and m.refSetId = :refSetId ");
      asteriskQuery.setParameter("terminology",
          mapProject.getDestinationTerminology());
      asteriskQuery.setParameter("terminologyVersion",
          mapProject.getDestinationTerminologyVersion());
      asteriskQuery.setParameter("refSetId", asteriskRefSetId);
      List<Concept> concepts = asteriskQuery.getResultList();
      for (final Concept concept : concepts) {
        asteriskCodes.add(concept.getTerminologyId());
      }

      // Look up dagger codes
      final javax.persistence.Query daggerQuery =
          manager.createQuery("select m.concept from SimpleRefSetMemberJpa m "
              + "where m.terminology = :terminology "
              + "and m.terminologyVersion = :terminologyVersion "
              + "and m.refSetId = :refSetId ");
      daggerQuery.setParameter("terminology",
          mapProject.getDestinationTerminology());
      daggerQuery.setParameter("terminologyVersion",
          mapProject.getDestinationTerminologyVersion());
      daggerQuery.setParameter("refSetId", daggerRefSetId);
      concepts = daggerQuery.getResultList();
      for (final Concept concept : concepts) {
        daggerCodes.add(concept.getTerminologyId());
      }

      // Report to log
      Logger.getLogger(getClass()).info("  asterisk codes = " + asteriskCodes);
      Logger.getLogger(getClass()).info("  dagger codes = " + daggerCodes);
//      Logger.getLogger(getClass())
//          .info("  valid 3 digit codes* = " + );
    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
      metadataService.close();
    }

  }

  /**
   * Cache laterality codes.
   *
   * @throws Exception the exception
   */

  private void cacheLateralityCodes() throws Exception {

    // Check if codes are already cached
    if(lateralityCodes != null){
      return;
    }
    lateralityCodes = new HashSet<>();
    
    final Properties config = ConfigUtility.getConfigProperties();
    final String dataDir = config.getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Check preconditions
    if (!new File(dataDir + "/ICD10CM/LATERALITY ADVICE LIST ICD10CM.txt")
        .exists()) {
      throw new Exception("Specified input file missing");
    }

    // Open reader and service
    BufferedReader codeListReader = new BufferedReader(new FileReader(
        new File(dataDir + "/ICD10CM/LATERALITY ADVICE LIST ICD10CM.txt")));

    String line = null;

    while ((line = codeListReader.readLine()) != null) {
      lateralityCodes.add(line.trim());
    }

    codeListReader.close();
  }

  /**
   * Cache additional advice codes.
   * 
   * @throws Exception the exception
   */

  private void cacheAdditionalAdviceCodes() throws Exception {

    // Check if codes are already cached
    if(additionalAdviceCodes != null){
      return;
    }
    additionalAdviceCodes = new HashSet<>();    
    
    final Properties config = ConfigUtility.getConfigProperties();
    final String dataDir = config.getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Check preconditions
    if (!new File(
        dataDir + "/ICD10CM/CONSIDER ADDITIONAL CODE ADVICE ICD10CM.txt")
            .exists()) {
      throw new Exception("Specified input file missing");
    }

    // Open reader and service
    BufferedReader codeListReader = new BufferedReader(new FileReader(new File(
        dataDir + "/ICD10CM/CONSIDER ADDITIONAL CODE ADVICE ICD10CM.txt")));

    String line = null;

    while ((line = codeListReader.readLine()) != null) {
      additionalAdviceCodes.add(line.trim());
    }

    codeListReader.close();
  }

  @Override
  public String getReleaseFile3rdElement() throws Exception {
    // For the US release files, 3rd element is "US1000124".
    return "US1000124";
  }  
  
}
