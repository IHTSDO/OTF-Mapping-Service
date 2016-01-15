package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
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

    ValidationResult validationResult = new ValidationResultJpa();
    ContentService contentService = new ContentServiceJpa();

    for (MapEntry mapEntry : mapRecord.getMapEntries()) {

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
                + " map  priority "
                + Integer.toString(mapEntry.getMapPriority()));
          }

          // Validate the code
          if (concept != null && !isTargetCodeValid(concept.getTerminologyId())) {

            validationResult.addError("Target code "
                + mapEntry.getTargetId()
                + " is an invalid code, use a child code instead. "
                + " Entry:"
                + (mapProject.isGroupStructure() ? " group "
                    + Integer.toString(mapEntry.getMapGroup()) + "," : "")
                + " map  priority "
                + Integer.toString(mapEntry.getMapPriority()));

          }
          Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
              "  Concept exists and is valid");
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

  /* see superclass */
  @Override
  public MapAdviceList computeMapAdvice(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {

    List<MapAdvice> advices = new ArrayList<>(mapEntry.getMapAdvices());

    // get hierarchical rel
    MetadataService metadataService = new MetadataServiceJpa();
    Map<String, String> hierarchicalRelationshipTypeMap =
        metadataService.getHierarchicalRelationshipTypes(
            mapProject.getDestinationTerminology(),
            mapProject.getDestinationTerminologyVersion());
    if (hierarchicalRelationshipTypeMap.keySet().size() > 1) {
      metadataService.close();
      throw new IllegalStateException(
          "Map project source terminology has too many hierarchical relationship types - "
              + mapProject.getDestinationTerminology());
    }
    if (hierarchicalRelationshipTypeMap.keySet().size() < 1) {
      metadataService.close();
      throw new IllegalStateException(
          "Map project source terminology has too few hierarchical relationship types - "
              + mapProject.getDestinationTerminology());
    }

    // find number of descendants
    ContentServiceJpa contentService = new ContentServiceJpa();
    SearchResultList results =
        contentService.findDescendantConcepts(mapRecord.getConceptId(),
            mapProject.getDestinationTerminology(),
            mapProject.getDestinationTerminologyVersion(), null);
    contentService.close();
    metadataService.close();
    if (results.getTotalCount() > 10) {
      for (MapAdvice advice : mapProject.getMapAdvices()) {
        if (advice.getName().toLowerCase()
            .equals("DESCENDANTS NOT EXHAUSTIVELY MAPPED".toLowerCase())) {
          advices.add(advice);
          // System.out.println("Found advice: " + advice.toString());
        }
      }
    }

    MapAdviceList mapAdviceList = new MapAdviceListJpa();
    mapAdviceList.setMapAdvices(advices);
    return mapAdviceList;
  }

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {

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

    // open the content service
    ContentService contentService = new ContentServiceJpa();

    // if a three digit code
    if (terminologyId.matches(".[0-9].")) {

      // SPECIFIC CASE for W00-W19, X00-X09, Y10-Y34, fourth digit not required,
      // return true for codes with 3 or more digits
      if (terminologyId.toUpperCase().matches("W..|X..|Y[0-2].|Y3[0-4]")
          && !terminologyId.toUpperCase().equals("Y06")
          && !terminologyId.toUpperCase().equals("Y07")
          && !terminologyId.toUpperCase().equals("Y35")
          && !terminologyId.toUpperCase().equals("Y36")
          && !terminologyId.toUpperCase().equals("X34")
          && !terminologyId.toUpperCase().equals("X59"))
        return true;

      // otherwise, if 3-digit code has children, return false
      TreePositionList tpList =
          contentService.getTreePositions(terminologyId,
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());
      if (tpList.getCount() == 0) {
        contentService.close();
        return false;
      }

      if (tpList.getTreePositions().get(0).getChildrenCount() > 0) {
        contentService.close();
        return false;
      }
    }

    // verify concept exists in database
    Concept concept =
        contentService.getConcept(terminologyId,
            mapProject.getDestinationTerminology(),
            mapProject.getDestinationTerminologyVersion());

    contentService.close();
    if (concept == null) {
      return false;
    }

    // otherwise, return true
    return true;
  }

  /* see superclass */
  @Override
  public void computeTargetTerminologyNotes(TreePositionList treePositionList)
    throws Exception {

    Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
        "Computing target terminology notes.");

    // open the metadata service and get the relationship types
    MetadataService metadataService = new MetadataServiceJpa();
    Map<String, String> simpleRefSets =
        metadataService.getSimpleRefSets(
            mapProject.getDestinationTerminology(),
            mapProject.getDestinationTerminologyVersion());

    // find the dagger-to-asterisk and asterisk-to-dagger types
    String asteriskRefSetId = null;
    String daggerRefSetId = null;

    for (String key : simpleRefSets.keySet()) {
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

    // open one content service to handle all concept retrieval
    ContentService contentService = new ContentServiceJpa();

    // for each tree position initially passed in, call the recursive helper
    for (TreePosition tp : treePositionList.getTreePositions()) {

      computeTargetTerminologyNotesHelper(tp, contentService, asteriskRefSetId,
          daggerRefSetId);
    }
    metadataService.close();
    contentService.close();
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
    for (SimpleRefSetMember simpleRefSetMember : concept
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
    for (TreePosition child : treePosition.getChildren()) {
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
    // for (String rule : member.getMapRule().split("AND IFA")) {
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
      for (MapAdvice advice : mapProject.getMapAdvices()) {
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
    // for (String advice : sortedAdvices) {
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
    for (MapRelation rel : mappingService.getMapRelations().getMapRelations()) {
      if (rel.getTerminologyId().equals("447639009")) {
        mappingService.close();
        return rel;
      }
    }
    mappingService.close();
    return null;
  }

}
