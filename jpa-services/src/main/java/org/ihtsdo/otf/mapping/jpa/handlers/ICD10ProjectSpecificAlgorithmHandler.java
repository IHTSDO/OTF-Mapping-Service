package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * The {@link ProjectSpecificAlgorithmHandler} for ICD10 projects.
 */
public class ICD10ProjectSpecificAlgorithmHandler extends
    DefaultProjectSpecificAlgorithmHandler {

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
  private static Map<String, Set<String>> externalCauseCodesMap =
      new HashMap<>();

  /**
   * The parser.
   *
   * @param mapRecord the map record
   * @return the validation result
   * @throws Exception the exception
   */
  // private MapRuleParser parser = new MapRuleParser();

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

    Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
        "Validating target codes for ICD10");

    final ValidationResult validationResult = new ValidationResultJpa();
    final ContentService contentService = new ContentServiceJpa();

    for (final MapEntry mapEntry : mapRecord.getMapEntries()) {

      // add an error if neither relation nor target are set
      if (mapEntry.getMapRelation() == null
          && (mapEntry.getTargetId() == null || mapEntry.getTargetId().equals(
              ""))) {

        validationResult
            .addError("A relation indicating the reason must be selected when no target is assigned.");

        // if a target is specified check it
      } else if (mapEntry.getTargetId() != null
          && !mapEntry.getTargetId().equals("")) {

        Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
            "  Checking id: " + mapEntry.getTargetId());

        // first, check terminology id based on above rules
        if (!mapEntry.getTargetId().equals("")
            && (!mapEntry.getTargetId().matches(".[0-9].*") || mapEntry
                .getTargetId().contains("-"))) {
          validationResult
              .addError("Invalid target code "
                  + mapEntry.getTargetId()
                  + "!  For ICD10, valid target codes must contain 3 digits and must not contain a dash."
                  + " Entry:"
                  + (mapProject.isGroupStructure() ? " group "
                      + Integer.toString(mapEntry.getMapGroup()) + "," : "")
                  + " map priority "
                  + Integer.toString(mapEntry.getMapPriority()));
        } else {

          // Validate the code
          if (!isTargetCodeValid(mapEntry.getTargetId())) {

            validationResult.addError("Target code "
                + mapEntry.getTargetId()
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
          validationResult.addError("The map relation "
              + mapEntry.getMapRelation().getName()
              + " is not allowable for null targets");
        }
      }
    }

    contentService.close();
    return validationResult;

  }

  /* see superclass */
  @Override
  public ValidationResult validateSemanticChecks(MapRecord mapRecord)
    throws Exception {
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
            contentService
                .getConcept(entry.getTargetId(), terminology, version);
        // Lazy initialize
        if (concept != null) {
          concept.getDescriptions().size();
          concept.getRelationships().size();
          concept.getInverseRelationships().size();
          concept.getSimpleRefSetMembers().size();
        }
        concepts.get(entry.getMapGroup()).add(concept);

      }

      // Only process these rules if these is a single entry per group
      if (concepts.keySet().size() == mapRecord.getMapEntries().size()) {

        //
        // PREDICATE: primary map target is an asterisk code with secondary map
        // code as a dagger code.
        // GUIDANCE: Switch order of codes (this was eventually automated)
        //
        if (mapRecord.getMapEntries().size() == 2
            && TerminologyUtility.isDaggerForAsterisk(concepts.get(1).get(0),
                concepts.get(2).get(0), contentService)) {
          result.addWarning("Primary asterisk with secondary dagger"
              + " code, consider switching order.");
        }

        //
        // PREDICATE: primary map target is a dagger code with an asterisk
        // reference in the preferred rubric AND there is no secondary code
        // matching that asterisk code
        // GUIDANCE: Add the secondary code
        //
        if (concepts.get(1).get(0) != null
            && TerminologyUtility.isDaggerCode(concepts.get(1).get(0),
                contentService)) {
          // iterate through descriptions/relationships and see if there is an
          // asterisk code
          String asteriskCode = null;
          for (final Description desc : concepts.get(1).get(0)
              .getDescriptions()) {
            for (final Relationship rel : concepts.get(1).get(0)
                .getRelationships()) {
              // the relationship terminologyId will match the description id
              if (rel.getTerminologyId().startsWith(
                  desc.getTerminologyId() + "~")) {
                asteriskCode = rel.getDestinationConcept().getTerminologyId();
              }
            }
          }
          if (asteriskCode != null) {
            // if there is no secondary code matching asterisk
            if (concepts.keySet().size() == 1
                || !concepts.get(2).get(0).getTerminologyId()
                    .equals(asteriskCode)) {
              result
                  .addError("Remap, primary dagger code should have a secondary asterisk code mapping indicated by the preferred rubric ("
                      + asteriskCode + ")");
            }
          }
        }

        //
        // PREDICATE: primary map target is a 4th digit ICD code having a fifth
        // digit
        // option of 0 (open) or 1 (closed).
        // GUIDANCE: Remap to 5 digits and consider “MAPPED FOLLOWING WHO
        // GUIDANCE" if SNOMED does not indicate open or closed"
        //
        final List<Concept> children =
            TerminologyUtility.getActiveChildren(concepts.get(1).get(0));
        if (concepts.get(1).get(0) != null
            && concepts.get(1).get(0).getTerminologyId().length() == 5
            && children.size() > 1
            && (children.get(0).getDefaultPreferredName().endsWith("open") || children
                .get(0).getDefaultPreferredName().endsWith("open"))) {
          result
              .addError("Remap to 5 digits and add \"MAPPED FOLLOWING WHO GUIDANCE\" "
                  + "advice if SNOMED does not indicate open or closed");

        }

        //
        // PREDICATE: primary map target is a Chapter XX code and there is a non
        // Chapter XX secondary code (e.g. V, W, X, or Y code)
        // GUIDANCE: Remap, Chapter XX codes should either be on their own (when
        // mapping events), or used as secondary codes.
        //
        if (concepts.get(1).get(0) != null
            && mapRecord.getMapEntries().size() > 1
            && concepts.get(1).get(0).getTerminologyId().matches("^[VWXY].*")) {
          result.addError("Remap, Chapter XX codes should either be on their "
              + "own, or used as secondary codes.");
        }

        //
        // PREDICATE: primary map target has a coding hint matching one of these
        // patterns: “Use additional code, if desired, to identify infectious
        // agent or disease”, “Use additional code (B95-B97), if desired, to
        // identify infectious agent”, “Use additional code (B95-B96), if
        // desired,
        // to identify bacterial agent” AND does not have “POSSIBLE REQUIREMENT
        // FOR CAUSATIVE AGENT CODE” and contains the words “infection”,
        // “infectious”, or “bacterial”.
        // GUIDANCE: Review to consider a second code or the advice.
        //
        if (concepts.get(1).get(0) != null
            && hasUseAdditional(concepts.get(1).get(0))
            && !TerminologyUtility.hasAdvice(mapRecord.getMapEntries().get(0),
                "POSSIBLE REQUIREMENT FOR CAUSATIVE AGENT CODE")) {
          result.addWarning("Primary map entry may requre \"POSSIBLE "
              + "REQUIREMENT FOR CAUSATIVE AGENT CODE\" advice");
        }

        //
        // PREDICATE: map target is a 4 digit in Chapter XIII, Diseases of the
        // Musculskeletal System and Connective Tissue (for which there is a
        // codable 5th level) and there is no “FIFTH CHARACTER REQUIRED TO
        // FURTHER
        // SPECIFY THE SITE” advice.
        // GUIDANCE: Consider adding a 5th digit, or adding the advice
        //
        // check each code
        for (int i = 0; i < mapRecord.getMapEntries().size(); i++) {
          if (concepts.get(i + 1).get(0) != null) {
            final Concept concept = concepts.get(i + 1).get(0);
            if (concept.getTerminologyId().startsWith("M")
                && concept.getTerminologyId().length() == 5
                && TerminologyUtility.hasActiveChildren(concept)
                && !TerminologyUtility.hasAdvice(
                    mapRecord.getMapEntries().get(i),
                    "FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE")) {
              result
                  .addWarning("4 digit M code entry may require 5th digit or \"FIFTH "
                      + "CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE\" advice");
              break;
            }
          }
        }

        //
        // PREDICATE: SNOMED CT concept to map is a poisoning concept
        // (descendant
        // of “Poisoning” 75478009) and there is not a secondary (or higher) map
        // target from the list of external cause codes applicable to poisonings
        // (as derived from columns 2,3, and 5 from TEIL3.ASC index file).
        // GUIDANCE: Remap to include the (required) external cause code.
        //
        boolean isPoisoning =
            contentService.isDescendantOf(mapRecord.getConceptId(),
                mapProject.getSourceTerminology(),
                mapProject.getSourceTerminologyVersion(), "75478009");
        if (concepts.get(1).get(0) != null
            && mapRecord.getMapEntries().size() == 1 && isPoisoning) {
          result
              .addWarning("Remap, poisoning requires an external cause code from the TEIL3.ASC index");
        }
        // Validate external cause code presence and not primary position
        else if (concepts.get(1).get(0) != null
            && mapRecord.getMapEntries().size() > 1 && isPoisoning) {

          // cause code in primary position
          if (getIcd10ExternalCauseCodes().contains(
              mapRecord.getMapEntries().get(0).getTargetId())) {
            result
                .addWarning("Remap, poisoning requires an external cause code in a secondary position");
          }

          // Validate the external cause code
          else {
            // TODO: also need to add map advice when undetermined poisoning and
            // ismapped to accidental code

            Set<String> cmpCodes = getIcd10ExternalCauseCodes();
            String type = "unspecified";
            String column = "accidental";
            // accidental
            if (mapRecord.getConceptName().toLowerCase().contains("accidental")
                && mapRecord.getConceptName().toLowerCase()
                    .contains("poisoning")) {
              type = "accidental";
              column = type;
              cmpCodes = getIcd10AccidentalPoisoningCodes();
            }

            // intensional
            else if (mapRecord.getConceptName().toLowerCase()
                .contains("intensional")
                && mapRecord.getConceptName().toLowerCase()
                    .contains("poisoning")) {
              type = "intensional";
              column = type;
              cmpCodes = getIcd10IntentionalPoisoningCodes();
            }

            // undetermined
            else if (mapRecord.getConceptName().toLowerCase()
                .contains("undetermined")
                && mapRecord.getConceptName().toLowerCase()
                    .contains("undetermined")) {
              type = "undetermined";
              column = type;
              cmpCodes = getIcd10UndeterminedPoisoningCodes();

            }

            // adverse effect
            // TODO: need to know whether this is hierarchical or text

            boolean found = false;
            for (int i = 1; i < mapRecord.getMapEntries().size(); i++) {
              final String targetId =
                  mapRecord.getMapEntries().get(i).getTargetId();
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
                  + " poisoning requires an external cause code from the '"
                  + column + "' column of the TEIL3.ASC index");
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

            if (Arrays.asList(
                new String[] {
                    "J40", "J20.0", "J20.1", "J20.2", "J20.3", "J20.4",
                    "J20.5", "J20.6", "J20.7", "J20.8", "J20.9", "A50.2"
                }).contains(concept.getTerminologyId())
                && !entry.getRule().contains("Current chronological age")) {
              result
                  .addWarning("Consider adding a \"Current chronological age\" rule to entry "
                      + i);
            }
          }
        }

        //
        // PREDICATE: All descendants of Tumor Stage finding: SCTID: 385356007
        // should be mapped to NC
        // GUIDANCE: require map to NC
        //
        boolean isTumorStageFinding =
            contentService.isDescendantOf(mapRecord.getConceptId(),
                mapProject.getSourceTerminology(),
                mapProject.getSourceTerminologyVersion(), "385356007");
        if (isTumorStageFinding && concepts.get(1).get(0) != null) {
          result
              .addWarning("Generally, descendants of tumor stage finding are mapped to NC");
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
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {

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

    final List<MapAdvice> advices = new ArrayList<>(mapEntry.getMapAdvices());
    final ContentService contentService = new ContentServiceJpa();

    try {

      final Concept concept =
          contentService.getConcept(mapEntry.getTargetId(),
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());

      // lazy initialize
      if (concept != null) {
        concept.getDescriptions().size();
        concept.getRelationships().size();
        concept.getInverseRelationships().size();
        concept.getSimpleRefSetMembers().size();
      }

      // Remove any advices that are purlely computed and keep only manually
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
      // PREDICATE: asterisk code is used and it does not have
      // THIS CODE MAY BE USED IN THE PRIMARY POSITION WHEN THE MANIFESTATION IS
      // THE PRIMARY FOCUS OF CARE
      // advice.
      // ACTION: add the advice if not present, remove the advice if not
      // asterisk code
      // primary or secondary - any position
      final String asteriskAdvice =
          "THIS CODE MAY BE USED IN THE PRIMARY POSITION WHEN THE MANIFESTATION IS THE PRIMARY FOCUS OF CARE";
      // If asterisk code
      if (TerminologyUtility.isAsteriskCode(concept, contentService)) {
        if (!TerminologyUtility.hasAdvice(mapEntry, asteriskAdvice)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, asteriskAdvice));
        }
      }
      // otherwise if advice present
      else if (TerminologyUtility.hasAdvice(mapEntry, asteriskAdvice)) {
        advices
            .remove(TerminologyUtility.getAdvice(mapProject, asteriskAdvice));
      }

      //
      // PREDICATE: Non-primary W00-Y34 except X34,X59,Y06,Y07,Y35,Y36 without
      // "POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE" advice
      // ACTION: add the advice
      //
      final String adviceP03 = "POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE";
      if (mapEntry.getTargetId().matches("(W..|X..|Y[0-2].|Y3[0-4]).*")
          && !mapEntry.getTargetId().startsWith("Y06")
          && !mapEntry.getTargetId().startsWith("Y07")
          && !mapEntry.getTargetId().startsWith("Y35")
          && !mapEntry.getTargetId().startsWith("Y36")
          && !mapEntry.getTargetId().startsWith("X34")
          && !mapEntry.getTargetId().startsWith("X59")
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP03)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP03));
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP03)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP03));
      }

      //
      // PREDICATE: Map target is in the range C00-D48 and does not have the
      // advice "POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE"
      // ACTION: add the advice
      //
      final String adviceP05 = "POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE";
      if (mapEntry.getTargetId().matches("(C..|D[0-3].|D4[0-8]).*")
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP05)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP05));
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP05)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP05));
      }

      //
      // PREDICATE: Primary map target is T31 or T32 and does not have the
      // advice
      // "USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29 (Burns)"
      // ACTION: add the advice
      //
      final String adviceP06 =
          "USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29 (Burns)";
      if (mapEntry.getTargetId().matches("(T31|T32).*")
          && mapEntry.getMapGroup() == 1 && mapEntry.getMapPriority() == 1
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP06)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP06));
      }

      //
      // PREDICATE: Primary chapter XX code without advice
      // "THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION"
      // ACTION: add the advice
      //
      final String adviceP07 =
          "THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION";
      if (mapEntry.getTargetId().matches("^[VWXY].*")
          && mapEntry.getMapGroup() == 1 && mapEntry.getMapPriority() == 1
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP07)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP07));
      }

      //
      // PREDICATE: Primary target is a poisoning code and there is a secondary
      // code indicating accidental intent and the SNOMED concept does not
      // indicate intent and the entry does not have the advice
      // "MAPPED FOLLOWING WHO GUIDANCE"
      // ACTION: add the advice
      //
      final String adviceP21a = "MAPPED FOLLOWING WHO GUIDANCE";
      boolean isPoisoning =
          contentService.isDescendantOf(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion(), "75478009");
      if (isPoisoning
          && !mapRecord.getConceptName().toLowerCase().matches("adverse")
          && !mapRecord.getConceptName().toLowerCase().matches("unintentional")
          && !mapRecord.getConceptName().toLowerCase().matches("accidental")
          && !mapRecord.getConceptName().toLowerCase().matches("intentional")
          && !mapRecord.getConceptName().toLowerCase().matches("undetermined")
          && mapEntry.getMapGroup() > 1
          && mapEntry.getMapPriority() == 1
          && getIcd10AccidentalPoisoningCodes()
              .contains(mapEntry.getTargetId())
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP21a)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP21a));
      }

      //
      // PREDICATE: primary code has advice
      // "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE"
      // and there is a secondary code from chapter XX
      // ACTION: remove the advice
      //
      final String adviceP23 =
          "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE";
      if (TerminologyUtility.hasAdvice(mapEntry, adviceP23)
          && mapRecord.getMapEntries().size() > 1) {
        for (int i = 1; i < mapRecord.getMapEntries().size(); i++) {
          // If external cause code found, move on
          if (mapRecord.getMapEntries().get(i).getTargetId()
              .matches("^[VWXY].*")) {
            advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP23));
            break;
          }
        }
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

      // SPECIFIC CASE: for M14.__, codes with fifth digit are not assignable
      if (terminologyId.toUpperCase().startsWith("M14")
          && terminologyId.length() == 6)
        return false;

      // if a three digit code
      if (terminologyId.matches(".[0-9].")) {

        // SPECIFIC CASE for W00-W19, X00-X09, Y10-Y34, fourth digit not
        // required,
        // return true for codes with 3 or more digits
        if (terminologyId.toUpperCase().matches("W..|X..|Y[0-2].|Y3[0-4]")
            && !terminologyId.toUpperCase().startsWith("Y06")
            && !terminologyId.toUpperCase().startsWith("Y07")
            && !terminologyId.toUpperCase().startsWith("Y35")
            && !terminologyId.toUpperCase().startsWith("Y36")
            && !terminologyId.toUpperCase().startsWith("X34")
            && !terminologyId.toUpperCase().startsWith("X59")) {
          // n/a
        }

        // Check other 3 digit codes
        else {

          // otherwise, if 3-digit code has children, return false
          TreePositionList tpList =
              contentService.getTreePositions(terminologyId,
                  mapProject.getDestinationTerminology(),
                  mapProject.getDestinationTerminologyVersion());
          if (tpList.getCount() == 0) {
            return false;
          }

          if (tpList.getTreePositions().get(0).getChildrenCount() > 0) {
            return false;
          }
        }
      }

      // if a four digit code disall
      else if (terminologyId.matches(".[0-9].\\..")) {

        // SPECIFIC CASE for W00-W19, X00-X09, Y10-Y34, fourth digit not
        // required,
        // return true for codes with 3 or more digits
        if (terminologyId.toUpperCase().matches("(W..|X..|Y[0-2].|Y3[0-4])..")
            && !terminologyId.toUpperCase().startsWith("Y06")
            && !terminologyId.toUpperCase().startsWith("Y07")
            && !terminologyId.toUpperCase().startsWith("Y35")
            && !terminologyId.toUpperCase().startsWith("Y36")
            && !terminologyId.toUpperCase().startsWith("X34")
            && !terminologyId.toUpperCase().startsWith("X59")) {
          return false;
        }

      }
      // verify concept exists in database
      final Concept concept =
          contentService.getConcept(terminologyId,
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());

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

    Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
        "Computing target terminology notes.");
    final MetadataService metadataService = new MetadataServiceJpa();
    final// open one content service to handle all concept retrieval
    ContentService contentService = new ContentServiceJpa();
    try {
      // open the metadata service and get the relationship types
      Map<String, String> simpleRefSets =
          metadataService.getSimpleRefSets(
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());

      // find the dagger-to-asterisk and asterisk-to-dagger types
      String asteriskRefSetId = null;
      String daggerRefSetId = null;

      for (final String key : simpleRefSets.keySet()) {
        if (simpleRefSets.get(key).equals("Asterisk refset"))
          asteriskRefSetId = key;
        if (simpleRefSets.get(key).equals("Dagger refset"))
          daggerRefSetId = key;
      }

      if (asteriskRefSetId == null)
        Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).warn(
            "Could not find Asterisk refset");

      if (daggerRefSetId == null)
        Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).warn(
            "Could not find Dagger refset");

      Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
          "  Asterisk to dagger relationship type found: " + asteriskRefSetId);

      Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
          "  Dagger to asterisk relationship type found: " + daggerRefSetId);

      // for each tree position initially passed in, call the recursive helper
      for (final TreePosition tp : treePositionList) {

        computeTargetTerminologyNotesHelper(tp, contentService,
            asteriskRefSetId, daggerRefSetId);
      }
    } catch (Exception e) {
      throw e;
    } finally {
      metadataService.close();
      contentService.close();
    }
  }

  /**
   * Compute target terminology notes helper.
   * 
   * @param treePosition the tree position
   * @param contentService the content service
   * @param asteriskRefSetId the asterisk ref set id
   * @param daggerRefSetId the dagger ref set id
   * @throws Exception the exception
   */
  private void computeTargetTerminologyNotesHelper(TreePosition treePosition,
    ContentService contentService, String asteriskRefSetId,
    String daggerRefSetId) throws Exception {

    Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
        "Computing target terminology note for "
            + treePosition.getTerminologyId());

    // initially set the note to an empty string
    treePosition.setTerminologyNote("");

    // get the concept
    Concept concept =
        contentService.getConcept(treePosition.getTerminologyId(),
            mapProject.getDestinationTerminology(),
            mapProject.getDestinationTerminologyVersion());

    // cycle over the simple ref set members
    // Add dagger/asterisk
    for (final SimpleRefSetMember simpleRefSetMember : concept
        .getSimpleRefSetMembers()) {
      Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
          "   " + simpleRefSetMember.getRefSetId());
      if (simpleRefSetMember.getRefSetId().equals(asteriskRefSetId))
        treePosition.setTerminologyNote("*");
      else if (simpleRefSetMember.getRefSetId().equals(daggerRefSetId))
        treePosition.setTerminologyNote("\u2020");
    }

    // if this tree position has children, set their terminology notes
    // recursively
    for (final TreePosition child : treePosition.getChildren()) {
      computeTargetTerminologyNotesHelper(child, contentService,
          asteriskRefSetId, daggerRefSetId);
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
  public ValidationResult validateForRelease(ComplexMapRefSetMember member)
    throws Exception {
    ValidationResult result = super.validateForRelease(member);

    // Verify mapTarget is not null when mapCategory is 447637006 or 447639009
    // 447637006|Map source concept is properly classified
    // 447639009|Map of source concept is context dependent (also applies to
    // gender)
    if (member.getMapTarget().isEmpty()
        && member.getMapRelationId().equals(Long.valueOf("447637006"))) {
      result.addError("Map has empty target with map category 447637006 - "
          + member);
    }
    if (member.getMapTarget().isEmpty()
        && member.getMapRelationId().equals(Long.valueOf("447639009"))) {
      result.addError("Map has empty target with map category 447639009 - "
          + member);
    }

    // Verify mapTarget is null when mapCategory is not 447637006 or 447639009
    if (!member.getMapTarget().isEmpty()
        && !member.getMapRelationId().equals(Long.valueOf("447637006"))
        && !member.getMapRelationId().equals(Long.valueOf("447639009"))) {
      result
          .addError("Map has non-empty target without map category 447639009 or 447637006  - "
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
          result.addError("Empty target with too many advice values - "
              + member);
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
        && (member.getMapRule().contains("| Male (finding) |") || member
            .getMapRule().contains("| Female (finding) |"))) {
      result.addError("Gender rulel without GENDER advice - " + member);
    }

    // Verify advice MAP IS CONTEXT DEPENDENT FOR GENDER is not used in
    // conjunction with CD advice ...Wed Dec 17 00:41:58 PST 2014
    if (member.getMapAdvice().contains("MAP IS CONTEXT DEPENDENT FOR GENDER")
        && member.getMapAdvice().contains(
            " MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT")) {
      result
          .addError("Gender rule contains invalid CONTEXT DEPENDENT advice - "
              + member);
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
  public void computeIdentifyAlgorithms(MapRecord mapRecord) throws Exception {
    // Attach a NOTE if the criteria for the algorithm is met
    final List<MapPrinciple> principles = new ArrayList<>();
    final ContentService contentService = new ContentServiceJpa();
    try {

      final Map<String, MapPrinciple> principleMap = new HashMap<>();
      for (final MapPrinciple principle : mapProject.getMapPrinciples()) {
        String id = principle.getPrincipleId();
        // Strip leading zero
        if (id.startsWith("0")) {
          id = id.substring(1);
        }
        principleMap.put(id, principle);
      }

      //
      // PREDICATE: SNOMED CT concept is a descendant of "Allergic Disposition"
      // 609328004
      // TODO: consider other possible ancestors
      // RESULT: principle 19
      //
      boolean isAllergy =
          contentService.isDescendantOf(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion(), "609328004");
      if (isAllergy) {
        principles.add(principleMap.get("19"));
      }

      //
      // PREDICATE: SNOMED CT concept is a descendant of "Poisoning" 75478009
      // RESULT: principle 21
      //
      boolean isPoisoning =
          contentService.isDescendantOf(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion(), "75478009");
      if (isPoisoning) {
        principles.add(principleMap.get("21"));
        principles.add(principleMap.get("27"));
      }

      //
      // PREDICATE: SNOMED CT concept contains "and/or"
      // RESULT: principle 31
      //
      if (mapRecord.getConceptName().toLowerCase().contains("and/or")) {
        principles.add(principleMap.get("31"));
      }

      //
      // PREDICATE: SNOMED CT concept is descendant of "Animal Bite Wound"
      // 399907009
      // RESULT: principle 32
      //
      boolean isAnimalBite =
          contentService.isDescendantOf(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion(), "399907009");
      if (isAnimalBite) {
        principles.add(principleMap.get("32"));
      }

      //
      // PREDICATE: SNOMED CT concept contains "postoperative" or "postsurgical"
      // and also contains "complication.*procedure"
      // RESULT: principle 37
      //
      if (mapRecord.getConceptName().toLowerCase().contains("postoperative")
          || mapRecord.getConceptName().toLowerCase().contains("postsurgical")
          || mapRecord.getConceptName().toLowerCase()
              .matches(".*complication.*procedure.*")) {
        principles.add(principleMap.get("37"));
      }

      // Add the note if it exists
      if (principles.size() > 0) {
        mapRecord.getMapPrinciples().addAll(principles);
      }
    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
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
          "Y40.0", "Y40.1", "Y40.3", "Y40.5", "Y40.6", "Y40.7", "Y40.8",
          "Y40.9", "Y41.0", "Y41.1", "Y41.2", "Y41.3", "Y41.4", "Y41.5",
          "Y41.8", "Y41.9", "Y42.2", "Y42.3", "Y42.4", "Y42.5", "Y42.6",
          "Y42.7", "Y42.8", "Y43.0", "Y43.1", "Y43.2", "Y43.3", "Y43.4",
          "Y43.5", "Y43.6", "Y43.8", "Y44.2", "Y44.3", "Y44.5", "Y44.6",
          "Y44.9", "Y45.0", "Y45.1", "Y45.2", "Y45.3", "Y45.4", "Y45.5",
          "Y45.8", "Y45.9", "Y46.0", "Y46.1", "Y46.2", "Y46.3", "Y46.6",
          "Y46.7", "Y46.8", "Y47.0", "Y47.1", "Y47.4", "Y47.8", "Y47.9",
          "Y48.0", "Y48.1", "Y48.2", "Y48.3", "Y48.4", "Y49.0", "Y49.1",
          "Y49.2", "Y49.3", "Y49.5", "Y49.7", "Y49.8", "Y50.0", "Y50.1",
          "Y51.0", "Y51.1", "Y51.3", "Y51.4", "Y51.5", "Y51.6", "Y51.7",
          "Y51.8", "Y51.9", "Y52.0", "Y52.2", "Y52.3", "Y52.4", "Y52.5",
          "Y52.6", "Y52.7", "Y52.8", "Y52.9", "Y53.1", "Y53.2", "Y53.4",
          "Y53.5", "Y53.6", "Y53.7", "Y53.8", "Y54.0", "Y54.1", "Y54.2",
          "Y54.3", "Y54.5", "Y54.6", "Y54.7", "Y54.8", "Y55.1", "Y55.3",
          "Y55.4", "Y55.5", "Y55.6", "Y55.7", "Y56.0", "Y56.1", "Y56.2",
          "Y56.3", "Y56.4", "Y56.5", "Y56.6", "Y57.0", "Y57.1", "Y57.2",
          "Y57.3", "Y57.5", "Y57.6", "Y57.7", "Y57.8", "Y59.3"
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
    final List<String> externalCauseCodes = new ArrayList<>();
    externalCauseCodes.addAll(getIcd10AdverseEffectPoisoningCodes());
    externalCauseCodes.addAll(getIcd10IntentionalPoisoningCodes());
    externalCauseCodes.addAll(getIcd10UndeterminedPoisoningCodes());
    externalCauseCodes.addAll(getIcd10AccidentalPoisoningCodes());
    return null;
  }

  /**
   * Checks for use additional.
   *
   * @param concept the concept
   * @return true, if successful
   */
  @SuppressWarnings("static-method")
  private boolean hasUseAdditional(Concept concept) {
    for (final Description desc : concept.getDescriptions()) {
      if (desc.getTerm().matches("Use additional code.*infectious agent")) {
        return true;
      } else if (desc.getTerm().matches("Use additional code.*bacterial agent")) {
        return true;
      }

    }
    return false;
  }
}
