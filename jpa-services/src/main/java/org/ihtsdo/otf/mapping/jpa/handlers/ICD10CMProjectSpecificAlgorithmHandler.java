package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
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
  private static Set<String> daggerCodes = new HashSet<>();

  /** The asterisk codes. */
  private static Set<String> asteriskCodes = new HashSet<>();

  /** The valid3 digit codes. */
  private static Set<String> valid3DigitCodes = new HashSet<>();

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
    cacheCodes();
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
      // PREDICATE: Map target is in the range C00-D48 and does not have the
      // advice "POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE"
      // ACTION: add the advice
      //
      final String adviceP05 = "POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE";
      if (mapEntry.getTargetId().matches("(C..|D[0-3].|D4[0-8]).*")) {
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP05)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP05));
        }
      } else if (TerminologyUtility.hasAdvice(mapEntry, adviceP05)) {
        advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP05));
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
      if (mapEntry.getTargetId().matches("(T31|T32).*")
          && mapEntry.getMapGroup() == 1 && mapEntry.getMapPriority() == 1
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP06)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP06));
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
            && !terminologyId.toUpperCase().startsWith("W26")
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

          cacheCodes();

          // Is it a valid 3 digit code?
          return valid3DigitCodes.contains(terminologyId.toUpperCase());

        }
      }

      // if a four digit code is all
      else if (terminologyId.matches(".[0-9].\\..")) {

        // SPECIFIC CASE for W00-W19, X00-X09, Y10-Y34, fourth digit not
        // required,
        // return true for codes with 3 or more digits
        if (terminologyId.toUpperCase().matches("(W..|X..|Y[0-2].|Y3[0-4])..")
            && !terminologyId.toUpperCase().startsWith("W26")
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
      final Concept concept = contentService.getConcept(terminologyId,
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

    // Verify mapTarget is not null when mapCategory is 447637006 or 447639009
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

    // Verify mapTarget is null when mapCategory is not 447637006 or 447639009
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
      // error if an "ifa" rule is found in the group while a true rule exists
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
    if (!asteriskCodes.isEmpty()) {
      return;
    }
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

      // Look up valid 3 digit codes. This is actually just a manual list
      // derived from this query:
      // select terminologyId from concepts
      // where terminology = 'ICD10'
      // and terminologyVersion = '2010'
      // and length(terminologyId) = 3
      // and terminologyId NOT IN
      // (select substring_index(ancestorPath, '~',-1)
      // from tree_positions
      // where terminology='ICD10'
      // and terminologyVersion = '2010');
      //
      valid3DigitCodes.addAll(Arrays.asList(new String[] {
          "A33", "A34", "A35", "A38", "A46", "A55", "A57", "A58", "A64", "A65",
          "A70", "A78", "A86", "A89", "A90", "A91", "A94", "A99", "B03", "B04",
          "B07", "B09", "B24", "B49", "B54", "B59", "B64", "B72", "B73", "B75",
          "B79", "B80", "B86", "B89", "B91", "B92", "B99", "C01", "C07", "C12",
          "C19", "C20", "C23", "C33", "C37", "C52", "C55", "C56", "C58", "C61",
          "C64", "C65", "C66", "C73", "C97", "D24", "D27", "D34", "D45", "D62",
          "D65", "D66", "D67", "D70", "D71", "D77", "E02", "E15", "E40", "E41",
          "E42", "E43", "E45", "E46", "E52", "E54", "E58", "E59", "E60", "E65",
          "E68", "E86", "E90", "F03", "F04", "F09", "F21", "F24", "F28", "F29",
          "F39", "F54", "F55", "F59", "F61", "F69", "F82", "F83", "F88", "F89",
          "F99", "G01", "G07", "G08", "G09", "G10", "G14", "G20", "G22", "G26",
          "G35", "G64", "G92", "G98", "H46", "H55", "H71", "H82", "I00", "I10",
          "I38", "I64", "I81", "I99", "J00", "J09", "J13", "J14", "J22", "J36",
          "J40", "J42", "J46", "J47", "J60", "J61", "J64", "J65", "J80", "J81",
          "J82", "J90", "J91", "K20", "K30", "K36", "K37", "L00", "L14", "L22",
          "L26", "L42", "L45", "L52", "L80", "L82", "L83", "L84", "L86", "L88",
          "L97", "N10", "N12", "N19", "N23", "N26", "N40", "N44", "N46", "N47",
          "N61", "N62", "N63", "N72", "N86", "N96", "O11", "O13", "O16", "O25",
          "O40", "O48", "O85", "O94", "O95", "P38", "P53", "P60", "P75", "P77",
          "P90", "P93", "P95", "Q02", "R02", "R05", "R11", "R12", "R13", "R14",
          "R15", "R17", "R18", "R21", "R31", "R32", "R33", "R34", "R35", "R36",
          "R42", "R51", "R53", "R54", "R55", "R58", "R64", "R69", "R71", "R72",
          "R75", "R80", "R81", "R91", "R92", "R98", "R99", "S16", "S18", "S47",
          "T07", "T16", "T55", "T58", "T64", "T66", "T68", "T71", "T96", "T97",
          "U85", "U88", "V98", "V99", "Y66", "Y69", "Y86", "Y95", "Y96", "Y97",
          "Y98", "Z21", "Z33"
      }));
      // for 2016 removed: I48 R95

      // Report to log
      Logger.getLogger(getClass()).info("  asterisk codes = " + asteriskCodes);
      Logger.getLogger(getClass()).info("  dagger codes = " + daggerCodes);
      Logger.getLogger(getClass())
          .info("  valid 3 digit codes = " + valid3DigitCodes);
    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
      metadataService.close();
    }
  }

}
