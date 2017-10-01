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
import org.ihtsdo.otf.mapping.rf2.Relationship;
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
  
  /** The laterality codes. */
  private static Set<String> lateralityCodes = new HashSet<>();

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
    cacheLateralityCodes();
    cacheLateralityCodes2();
    
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
      // PREDICATE: All codes in range S00-T88, except T36-T65, unless there is 
      // a second map group with codes in this chapter External causes of morbidity 
      // (V00-Y99) and does not have the
      // advice "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE"
      // ACTION: add the advice
      //
      boolean found = false;
      for (MapEntry entry : mapRecord.getMapEntries()) {
    	 if (entry.getMapGroup() == 2 &&
    			 entry.getTargetId().matches("(V..|W..|X..|Y..).*")) {
    		 found = true;
    		 break;
    	 }
      }
      final String adviceP01 = "POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE";
      if (!found && mapEntry.getTargetId().matches("(S[0-9].|T[0-8][0-8]).*") &&
    		  !mapEntry.getTargetId().matches("(T[3-9][6-9].|T[6-9][0-5]).*")) {
    	
        if (!TerminologyUtility.hasAdvice(mapEntry, adviceP01)) {
          advices.add(TerminologyUtility.getAdvice(mapProject, adviceP01));
        }
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
      // PREDICATE: Map target code is in the annually updated (by NLM) list of
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
      // PREDICATE: All target codes in the chapter Pregnancy, childbirth and the puerperium (O00-O99), 
      // with ‘unspecified trimester’ in their descriptions and does not have the
      // advice "CONSIDER TRIMESTER SPECIFICATION"
      // ACTION: add the advice
      //
      final String adviceP04 =
          "CONSIDER TRIMESTER SPECIFICATION";
      if (mapEntry.getTargetId().startsWith("O")
    	  && (mapEntry.getTargetName().toLowerCase().indexOf("unspecified trimester") != -1)
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP04)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP04));
      }

      //
      // PREDICATE: All target codes with these prefixes: O31, O32, O33.3-O33.6, O35, 
      // O36, O40, O41, O60.1-O60.2, O64, O69; and ending with the 7th character= 0  (‘fetus unspecified’)
      // and does not have the advice
      // "CONSIDER WHICH FETUS IS AFFECTED BY THE MATERNAL CONDITION"
      // ACTION: add the advice
      //
      final String adviceP05 =
          "CONSIDER WHICH FETUS IS AFFECTED BY THE MATERNAL CONDITION";
      if ((mapEntry.getTargetId().startsWith("O31") ||
    	   mapEntry.getTargetId().startsWith("O32") ||
    	   mapEntry.getTargetId().matches("(O33.[3-6]).*") ||
    	   mapEntry.getTargetId().startsWith("O35") ||
    	   mapEntry.getTargetId().startsWith("O36") ||
    	   mapEntry.getTargetId().startsWith("O40") ||
    	   mapEntry.getTargetId().startsWith("O41") ||
    	   mapEntry.getTargetId().matches("(O60.[1-2]).*") ||
    	   mapEntry.getTargetId().startsWith("O64") ||
    	   mapEntry.getTargetId().startsWith("O69")) 
    	  && mapEntry.getTargetId().matches("\\D\\d{2}.\\w{3}0$")
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP05)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP05));
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
          && mapEntry.getMapGroup() == 1 
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP06)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP06));
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
      if (mapEntry.getTargetId().startsWith("T31")
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP07)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP07));
      }

      //
      // PREDICATE: Primary map target is T32 and does not have the
      // advice
      // "USE AS PRIMARY CODE ONLY IF SITE OF CORROSION UNSPECIFIED, OTHERWISE 
      // USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)"
      // ACTION: add the advice
      //
      final String adviceP08 =
          "USE AS PRIMARY CODE ONLY IF SITE OF CORROSION UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)";
      if (mapEntry.getTargetId().startsWith("T32")
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP08)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP08));
      }
      
      //
      // PREDICATE: All target codes with prefixes H40.10-H40.14, H40.20, H40.22, H40.3-H40.6 
      // and does not have the advice
      // "CONSIDER STAGE OF GLAUCOMA SPECIFICATION"
      // ACTION: add the advice
      //
      final String adviceP09 =
          "CONSIDER STAGE OF GLAUCOMA SPECIFICATION";
      if (mapEntry.getTargetId().startsWith("H40.20") ||
    	   mapEntry.getTargetId().startsWith("H40.22") ||
    	   mapEntry.getTargetId().matches("(^H40.1).*") ||
    	   mapEntry.getTargetId().matches("(^H40.[3-6]).*")
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP09)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP09));
      }      
      
      //
      // PREDICATE: All target codes with prefix M1A and does not have the
      // advice "CONSIDER TOPHUS SPECIFICATION"
      // ACTION: add the advice
      //
      final String adviceP10 = "CONSIDER TOPHUS SPECIFICATION";
      if (mapEntry.getTargetId().startsWith("M1A")
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP10)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP10));
      }

      //
      // PREDICATE: All target codes with prefix R40.2 and does not have the
      // advice "CONSIDER TIME OF COMA SCALE SPECIFICATION"
      // ACTION: add the advice
      //
      final String adviceP11 = "CONSIDER TIME OF COMA SCALE SPECIFICATION";
      if (mapEntry.getTargetId().startsWith("R40.2")
          && !TerminologyUtility.hasAdvice(mapEntry, adviceP11)) {
        advices.add(TerminologyUtility.getAdvice(mapProject, adviceP11));
      }
      
      //
      // PREDICATE: All codes with description notes starting with 'use_additional', 'code_first'
      // 'code_also' or if any of their ancestors* have description notes starting with these
      // phrases should be givene the advice "CONSIDER ADDITIONAL CODE TO IDENTIFY SPECIFIC CONDITION OR DISEASE"
      // *ancestors: are only searched up till one layer above the 3 character level (e.g. T36-T50)
      // ACTION: add the advice
      //
      final String adviceP12 = "CONSIDER ADDITIONAL CODE TO IDENTIFY SPECIFIC CONDITION OR DISEASE";
      final Concept conceptP12 = contentService.getConcept(mapEntry.getTargetId(),
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion()); 
      boolean matchingNote = isMatchingDescriptionNote(conceptP12);
      if(matchingNote
    		  && !TerminologyUtility.hasAdvice(mapEntry, adviceP12)) {
    	advices.add(TerminologyUtility.getAdvice(mapProject, adviceP12));
      } else if (!matchingNote) {
    	advices.remove(TerminologyUtility.getAdvice(mapProject, adviceP12));
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
   * @param concept
   * @return 
   * @throws Exception
   */
  private boolean isMatchingDescriptionNote(Concept concept) throws Exception {
	  for (Description description : concept.getDescriptions()) {
    	  if (description.getTerm().toLowerCase().startsWith("use_additional") ||
    		  description.getTerm().toLowerCase().startsWith("code_first") ||
    		  description.getTerm().toLowerCase().startsWith("code_also")) {
    		return true;
    	  }
      }
	  // if we are already one level above the 3 character level (e.g. T36-T50)
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
      
      TreePositionList list = contentService.getTreePositions(terminologyId, mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());
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
    // -- up propagation checks the threshold already - this is too
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

	/**
	 * Cache first half of the laterality codes.
	 *
	 * TODO: this should be read in from a file
	 * 
	 * @throws Exception
	 *             the exception
	 */

	private void cacheLateralityCodes() throws Exception {
		lateralityCodes.addAll(Arrays.asList(new String[] { "C34.00", "C34.10", "C34.30", "C34.80", "C34.90", "C40.00",
				"C40.10", "C40.20", "C40.30", "C40.80", "C40.90", "C43.10", "C43.20", "C43.60", "C43.70", "C44.101",
				"C44.111", "C44.121", "C44.191", "C44.201", "C44.211", "C44.221", "C44.291", "C44.601", "C44.611",
				"C44.621", "C44.691", "C44.701", "C44.711", "C44.721", "C44.791", "C46.50", "C47.10", "C47.20",
				"C49.10", "C49.20", "C4A.10", "C4A.20", "C4A.60", "C4A.70", "C50.019", "C50.029", "C50.119", "C50.129",
				"C50.219", "C50.229", "C50.319", "C50.329", "C50.419", "C50.429", "C50.519", "C50.529", "C50.619",
				"C50.629", "C50.819", "C50.829", "C50.919", "C50.929", "C56.9", "C57.00", "C57.10", "C57.20", "C62.00",
				"C62.10", "C62.90", "C63.00", "C63.10", "C64.9", "C65.9", "C66.9", "C69.00", "C69.10", "C69.20",
				"C69.30", "C69.40", "C69.50", "C69.60", "C69.80", "C69.90", "C72.20", "C72.30", "C72.40", "C74.00",
				"C74.10", "C74.90", "C76.40", "C76.50", "C78.00", "C79.00", "C79.60", "C79.70", "D02.20", "D03.10",
				"D03.20", "D03.60", "D03.70", "D04.10", "D04.20", "D04.60", "D04.70", "D05.00", "D05.10", "D05.80",
				"D05.90", "D09.20", "D14.30", "D16.00", "D16.10", "D16.20", "D16.30", "D17.20", "D21.10", "D21.20",
				"D22.10", "D22.20", "D22.60", "D22.70", "D23.10", "D23.20", "D23.60", "D23.70", "D24.9", "D27.9",
				"D29.20", "D29.30", "D30.00", "D30.10", "D30.20", "D31.00", "D31.10", "D31.20", "D31.30", "D31.40",
				"D31.50", "D31.60", "D31.90", "D35.00", "D39.10", "D40.10", "D41.00", "D41.10", "D41.20", "D44.10",
				"D48.60", "D49.519", "G56.00", "G56.10", "G56.20", "G56.30", "G56.40", "G56.80", "G56.90", "G57.00",
				"G57.10", "G57.20", "G57.30", "G57.40", "G57.50", "G57.60", "G57.70", "G57.80", "G57.90", "G81.00",
				"G81.10", "G81.90", "G83.10", "G83.20", "G83.30", "G90.519", "G90.529", "H00.019", "H00.029", "H00.039",
				"H00.19", "H01.009", "H01.019", "H01.029", "H01.119", "H01.129", "H01.139", "H01.149", "H02.009",
				"H02.019", "H02.029", "H02.039", "H02.049", "H02.059", "H02.109", "H02.119", "H02.129", "H02.139",
				"H02.149", "H02.209", "H02.219", "H02.229", "H02.239", "H02.30", "H02.409", "H02.419", "H02.429",
				"H02.439", "H02.519", "H02.529", "H02.539", "H02.60", "H02.719", "H02.729", "H02.739", "H02.819",
				"H02.829", "H02.839", "H02.849", "H02.859", "H02.869", "H02.879", "H04.009", "H04.019", "H04.029",
				"H04.039", "H04.119", "H04.129", "H04.139", "H04.149", "H04.159", "H04.169", "H04.209", "H04.219",
				"H04.229", "H04.309", "H04.319", "H04.329", "H04.339", "H04.419", "H04.429", "H04.439", "H04.519",
				"H04.529", "H04.539", "H04.549", "H04.559", "H04.569", "H04.579", "H04.619", "H04.819", "H05.019",
				"H05.029", "H05.039", "H05.049", "H05.119", "H05.129", "H05.219", "H05.229", "H05.239", "H05.249",
				"H05.259", "H05.269", "H05.319", "H05.329", "H05.339", "H05.349", "H05.359", "H05.409", "H05.419",
				"H05.429", "H05.50", "H05.819", "H05.829", "H10.019", "H10.029", "H10.10", "H10.219", "H10.229",
				"H10.239", "H10.30", "H10.409", "H10.419", "H10.429", "H10.439", "H10.509", "H10.519", "H10.529",
				"H10.539", "H10.819", "H11.009", "H11.019", "H11.029", "H11.039", "H11.049", "H11.059", "H11.069",
				"H11.119", "H11.129", "H11.139", "H11.149", "H11.159", "H11.219", "H11.229", "H11.239", "H11.249",
				"H11.30", "H11.419", "H11.429", "H11.439", "H11.449", "H11.819", "H11.829", "H15.009", "H15.019",
				"H15.029", "H15.039", "H15.049", "H15.059", "H15.099", "H15.109", "H15.119", "H15.129", "H15.819",
				"H15.829", "H15.839", "H15.849", "H15.859", "H16.009", "H16.019", "H16.029", "H16.039", "H16.049",
				"H16.059", "H16.069", "H16.079", "H16.109", "H16.119", "H16.129", "H16.139", "H16.149", "H16.209",
				"H16.219", "H16.229", "H16.239", "H16.249", "H16.259", "H16.269", "H16.299", "H16.309", "H16.319",
				"H16.329", "H16.339", "H16.399", "H16.409", "H16.419", "H16.429", "H16.439", "H16.449", "H17.00",
				"H17.10", "H17.819", "H17.829", "H18.009", "H18.019", "H18.029", "H18.039", "H18.049", "H18.059",
				"H18.069", "H18.10", "H18.219", "H18.229", "H18.239", "H18.319", "H18.329", "H18.339", "H18.419",
				"H18.429", "H18.449", "H18.459", "H18.469", "H18.609", "H18.619", "H18.629", "H18.719", "H18.729",
				"H18.739", "H18.799", "H18.819", "H18.829", "H18.839", "H18.899", "H20.019", "H20.029", "H20.039",
				"H20.049", "H20.059", "H20.10", "H20.20", "H20.819", "H20.829", "H21.00", "H21.1X9", "H21.219",
				"H21.229", "H21.239", "H21.249", "H21.259", "H21.269", "H21.279", "H21.309", "H21.319", "H21.329",
				"H21.339", "H21.349", "H21.359", "H21.40", "H21.509", "H21.519", "H21.529", "H21.539", "H21.549",
				"H21.559", "H21.569", "H25.019", "H25.039", "H25.049", "H25.099", "H25.10", "H25.20", "H25.819",
				"H26.009", "H26.019", "H26.039", "H26.049", "H26.059", "H26.069", "H26.109", "H26.119", "H26.129",
				"H26.139", "H26.219", "H26.229", "H26.239", "H26.30", "H26.419", "H26.499", "H27.00", "H27.119",
				"H27.129", "H27.139", "H30.009", "H30.019", "H30.029", "H30.039", "H30.049", "H30.109", "H30.119",
				"H30.129", "H30.139", "H30.149", "H30.20", "H30.819", "H30.899", "H30.90", "H31.009", "H31.019",
				"H31.029", "H31.099", "H31.109", "H31.119", "H31.129", "H31.309", "H31.319", "H31.329", "H31.409",
				"H31.419", "H31.429", "H33.009", "H33.019", "H33.029", "H33.039", "H33.049", "H33.059", "H33.109",
				"H33.119", "H33.129", "H33.199", "H33.20", "H33.309", "H33.319", "H33.329", "H33.339", "H33.40",
				"H34.00", "H34.10", "H34.219", "H34.239", "H34.8190", "H34.8191", "H34.8192", "H34.829", "H34.8390",
				"H34.8391", "H34.8392", "H35.019", "H35.029", "H35.039", "H35.049", "H35.059", "H35.069", "H35.079",
				"H35.109", "H35.119", "H35.129", "H35.139", "H35.149", "H35.159", "H35.169", "H35.179", "H35.20",
				"H35.3190", "H35.3191", "H35.3192", "H35.3193", "H35.3194", "H35.3290", "H35.3291", "H35.3292",
				"H35.3293", "H35.349", "H35.359", "H35.369", "H35.379", "H35.389", "H35.419", "H35.429", "H35.439",
				"H35.449", "H35.459", "H35.469", "H35.60", "H35.719", "H35.729", "H35.739", "H40.009", "H40.019",
				"H40.029", "H40.039", "H40.049", "H40.059", "H40.069", "H40.1190", "H40.1191", "H40.1192", "H40.1193",
				"H40.1194", "H40.1290", "H40.1291", "H40.1292", "H40.1293", "H40.1294", "H40.1390", "H40.1391",
				"H40.1392", "H40.1393", "H40.1394", "H40.1490", "H40.1491", "H40.1492", "H40.1493", "H40.1494",
				"H40.159", "H40.219", "H40.2290", "H40.2291", "H40.2292", "H40.2293", "H40.2294", "H40.239", "H40.249",
				"H40.30X0", "H40.30X1", "H40.30X2", "H40.30X3", "H40.30X4", "H40.40X0", "H40.40X1", "H40.40X2",
				"H40.40X3", "H40.40X4", "H40.50X0", "H40.50X1", "H40.50X2", "H40.50X3", "H40.50X4", "H40.60X0",
				"H40.60X1", "H40.60X2", "H40.60X3", "H40.60X4", "H40.819", "H40.829", "H40.839", "H43.00", "H43.10",
				"H43.20", "H43.319", "H43.399", "H43.819", "H43.829", "H44.009", "H44.019", "H44.029", "H44.119",
				"H44.129", "H44.139", "H44.20", "H44.319", "H44.329", "H44.399", "H44.419", "H44.429", "H44.439",
				"H44.449", "H44.519", "H44.529", "H44.539", "H44.609", "H44.619", "H44.629", "H44.639", "H44.649",
				"H44.659", "H44.699", "H44.709", "H44.719", "H44.729", "H44.739", "H44.749", "H44.759", "H44.799",
				"H44.819", "H44.829", "H46.00", "H46.10", "H47.019", "H47.029", "H47.039", "H47.099", "H47.149",
				"H47.219", "H47.239", "H47.299", "H47.319", "H47.329", "H47.339", "H47.399", "H47.519", "H47.529",
				"H47.539", "H47.619", "H47.629", "H47.639", "H47.649", "H49.00", "H49.10", "H49.20", "H49.30", "H49.40",
				"H49.819", "H49.889", "H51.20", "H52.00", "H52.10", "H52.209", "H52.219", "H52.229", "H52.519",
				"H52.529", "H52.539", "H53.009", "H53.019", "H53.029", "H53.039", "H53.049", "H53.129", "H53.139",
				"H53.149", "H53.419", "H53.429", "H53.439", "H53.459", "H53.489", "H54.10", "H54.40", "H54.50",
				"H54.60", "H57.059", "H57.10", "H59.019", "H59.029", "H59.039", "H59.099", "H59.119", "H59.129",
				"H59.219", "H59.229", "H59.319", "H59.329", "H59.339", "H59.349", "H59.359", "H59.369", "H59.819",
				"H60.00", "H60.10", "H60.20", "H60.319", "H60.329", "H60.339", "H60.399", "H60.40", "H60.509",
				"H60.519", "H60.529", "H60.539", "H60.549", "H60.559", "H60.599", "H60.60", "H60.8X9", "H60.90",
				"H61.009", "H61.019", "H61.029", "H61.039", "H61.109", "H61.119", "H61.129", "H61.199", "H61.20",
				"H61.309", "H61.319", "H61.329", "H61.399", "H61.819", "H61.899", "H61.90", "H62.40", "H62.8X9",
				"H65.00", "H65.07", "H65.117", "H65.119", "H65.197", "H65.199", "H65.20", "H65.30", "H65.419",
				"H65.499", "H65.90", "H66.007", "H66.009", "H66.017", "H66.019", "H66.10", "H66.20", "H66.3X9",
				"H66.40", "H66.90", "H67.9", "H68.009", "H68.019", "H68.029", "H68.109", "H68.119", "H68.129",
				"H68.139", "H69.00", "H69.80", "H69.90", "H70.009", "H70.019", "H70.099", "H70.10", "H70.209",
				"H70.219", "H70.229", "H70.819", "H70.899", "H70.90", "H71.00", "H71.10", "H71.20", "H71.30", "H71.90",
				"H72.00", "H72.10", "H72.2X9", "H72.819", "H72.829", "H72.90", "H73.009", "H73.019", "H73.099",
				"H73.10", "H73.20", "H73.819", "H73.829", "H73.899", "H73.90", "H74.09", "H74.19", "H74.20", "H74.319",
				"H74.329", "H74.399", "H74.40", "H74.8X9", "H74.90", "H75.00", "H75.80", "H80.00", "H80.10", "H80.20",
				"H80.80", "H80.90", "H81.09", "H81.10", "H81.20", "H81.319", "H81.399", "H81.49", "H81.8X9", "H81.90",
				"H82.9", "H83.09", "H83.19", "H83.2X9", "H83.3X9", "H83.8X9", "H83.90", "H91.09", "H91.10", "H91.20",
				"H91.8X9", "H91.90", "H92.09", "H92.10", "H92.20", "H93.019", "H93.099", "H93.19", "H93.219", "H93.229",
				"H93.239", "H93.249", "H93.299", "H93.3X9", "H93.8X9", "H93.90", "H93.A9", "H94.00", "H94.80", "H95.00",
				"H95.119", "H95.129", "H95.139", "H95.199", "H95.819", "I60.00", "I60.10", "I60.30", "I60.50",
				"I63.019", "I63.039", "I63.119", "I63.139", "I63.219", "I63.239", "I63.319", "I63.329", "I63.339",
				"I63.349", "I63.419", "I63.429", "I63.439", "I63.449", "I63.519", "I63.529", "I63.539", "I63.549",
				"I65.09", "I65.29", "I66.09", "I66.19", "I66.29", "I69.039", "I69.049", "I69.059", "I69.069", "I69.139",
				"I69.149", "I69.159", "I69.169", "I69.239", "I69.249", "I69.259", "I69.269", "I69.339", "I69.349",
				"I69.359", "I69.369", "I69.839", "I69.849", "I69.859", "I69.869", "I69.939", "I69.949", "I69.959",
				"I69.969", "I70.208", "I70.209", "I70.219", "I70.229", "I70.269", "I70.299", "I70.308", "I70.309",
				"I70.318", "I70.319", "I70.328", "I70.329", "I70.368", "I70.369", "I70.398", "I70.399", "I70.408",
				"I70.409", "I70.419", "I70.429", "I70.469", "I70.499", "I70.508", "I70.509", "I70.519", "I70.529",
				"I70.569", "I70.599", "I70.608", "I70.609", "I70.619", "I70.629", "I70.669", "I70.699", "I70.708",
				"I70.709", "I70.719", "I70.729", "I70.769", "I70.799", "I75.019", "I75.029", "I80.00", "I80.10",
				"I80.209", "I80.219", "I80.229", "I80.239", "I80.299", "I82.409", "I82.419", "I82.429", "I82.439",
				"I82.449", "I82.499", "I82.4Y9", "I82.4Z9", "I82.509", "I82.519", "I82.529", "I82.539", "I82.549",
				"I82.599", "I82.5Y9", "I82.5Z9", "I82.609", "I82.619", "I82.629", "I82.709", "I82.719", "I82.729",
				"I82.819", "I82.A19", "I82.A29", "I82.B19", "I82.B29", "I82.C19", "I82.C29", "I83.10", "I83.819",
				"I83.899", "I83.90", "I87.009", "I87.019", "I87.029", "I87.039", "I87.099", "I87.309", "I87.319",
				"I87.329", "I87.339", "I87.399", "J38.00", "L02.419", "L02.429", "L02.439", "L02.519", "L02.529",
				"L02.539", "L02.619", "L02.629", "L02.639", "L03.019", "L03.029", "L03.039", "L03.049", "L03.119",
				"L03.129", "M00.019", "M00.029", "M00.039", "M00.049", "M00.059", "M00.069", "M00.079", "M00.119",
				"M00.129", "M00.139", "M00.149", "M00.159", "M00.169", "M00.179", "M00.219", "M00.229", "M00.239",
				"M00.249", "M00.259", "M00.269", "M00.279", "M00.819", "M00.829", "M00.839", "M00.849", "M00.859",
				"M00.869", "M00.879", "M01.X19", "M01.X29", "M01.X39", "M01.X49", "M01.X59", "M01.X69", "M01.X79",
				"M02.019", "M02.029", "M02.039", "M02.049", "M02.059", "M02.069", "M02.079", "M02.119", "M02.129",
				"M02.139", "M02.149", "M02.159", "M02.169", "M02.179", "M02.219", "M02.229", "M02.239", "M02.249",
				"M02.259", "M02.269", "M02.279", "M02.319", "M02.329", "M02.339", "M02.349", "M02.359", "M02.369",
				"M02.379", "M02.819", "M02.829", "M02.839", "M02.849", "M02.859", "M02.869", "M02.879", "M05.019",
				"M05.029", "M05.039", "M05.049", "M05.059", "M05.069", "M05.079", "M05.119", "M05.129", "M05.139",
				"M05.149", "M05.159", "M05.169", "M05.179", "M05.219", "M05.229", "M05.239", "M05.249", "M05.259",
				"M05.269", "M05.279", "M05.319", "M05.329", "M05.339", "M05.349", "M05.359", "M05.369", "M05.379",
				"M05.419", "M05.429", "M05.439", "M05.449", "M05.459", "M05.469", "M05.479", "M05.519", "M05.529",
				"M05.539", "M05.549", "M05.559", "M05.569", "M05.579", "M05.619", "M05.629", "M05.639", "M05.649",
				"M05.659", "M05.669", "M05.679", "M05.719", "M05.729", "M05.739", "M05.749", "M05.759", "M05.769",
				"M05.779", "M05.819", "M05.829", "M05.839", "M05.849", "M05.859", "M05.869", "M05.879", "M06.019",
				"M06.029", "M06.039", "M06.049", "M06.059", "M06.069", "M06.079", "M06.219", "M06.229", "M06.239",
				"M06.249", "M06.259", "M06.269", "M06.279", "M06.319", "M06.329", "M06.339", "M06.349", "M06.359",
				"M06.369", "M06.379", "M06.819", "M06.829", "M06.839", "M06.849", "M06.859", "M06.869", "M06.879",
				"M07.619", "M07.629", "M07.639", "M07.649", "M07.659", "M07.669", "M07.679", "M08.019", "M08.029",
				"M08.039", "M08.049", "M08.059", "M08.069", "M08.079", "M08.219", "M08.229", "M08.239", "M08.249",
				"M08.259", "M08.269", "M08.279", "M08.419", "M08.429", "M08.439", "M08.449", "M08.459", "M08.469",
				"M08.479", "M08.819", "M08.829", "M08.839", "M08.849", "M08.859", "M08.869", "M08.879", "M08.919",
				"M08.929", "M08.939", "M08.949", "M08.959", "M08.969", "M08.979", "M10.019", "M10.029", "M10.039",
				"M10.049", "M10.059", "M10.069", "M10.079", "M10.119", "M10.129", "M10.139", "M10.149", "M10.159",
				"M10.169", "M10.179", "M10.219", "M10.229", "M10.239", "M10.249", "M10.259", "M10.269", "M10.279",
				"M10.319", "M10.329", "M10.339", "M10.349", "M10.359", "M10.369", "M10.379", "M10.419", "M10.429",
				"M10.439", "M10.449", "M10.459", "M10.469", "M10.479", "M11.019", "M11.029", "M11.039", "M11.049",
				"M11.059", "M11.069", "M11.079", "M11.119", "M11.129", "M11.139", "M11.149", "M11.159", "M11.169",
				"M11.179", "M11.219", "M11.229", "M11.239", "M11.249", "M11.259", "M11.269", "M11.279", "M11.819",
				"M11.829", "M11.839", "M11.849", "M11.859", "M11.869", "M11.879", "M12.019", "M12.029", "M12.039",
				"M12.049", "M12.059", "M12.069", "M12.079", "M12.119", "M12.129", "M12.139", "M12.149", "M12.159",
				"M12.169", "M12.179", "M12.219", "M12.229", "M12.239", "M12.249", "M12.259", "M12.269", "M12.279",
				"M12.319", "M12.329", "M12.339", "M12.349", "M12.359", "M12.369", "M12.379", "M12.419", "M12.429",
				"M12.439", "M12.449", "M12.459", "M12.469", "M12.479", "M12.519", "M12.529", "M12.539", "M12.549",
				"M12.559", "M12.569", "M12.579", "M12.819", "M12.829", "M12.839", "M12.849", "M12.859", "M12.869",
				"M12.879", "M13.119", "M13.129", "M13.139", "M13.149", "M13.159", "M13.169", "M13.179", "M13.819",
				"M13.829", "M13.839", "M13.849", "M13.859", "M13.869", "M13.879", "M14.619", "M14.629", "M14.639",
				"M14.649", "M14.659", "M14.669", "M14.679", "M14.819", "M14.829", "M14.839", "M14.849", "M14.859",
				"M14.869", "M14.879", "M19.019", "M19.029", "M19.039", "M19.049", "M19.079", "M19.119", "M19.129",
				"M19.139", "M19.149", "M19.179", "M19.219", "M19.229", "M19.239", "M19.249", "M19.279", "M1A.0190",
				"M1A.0191", "M1A.0290", "M1A.0291", "M1A.0390", "M1A.0391", "M1A.0490", "M1A.0491", "M1A.0590",
				"M1A.0591", "M1A.0690", "M1A.0691", "M1A.0790", "M1A.0791", "M1A.1190", "M1A.1191", "M1A.1290",
				"M1A.1291", "M1A.1390", "M1A.1391", "M1A.1490", "M1A.1491", "M1A.1590", "M1A.1591", "M1A.1690",
				"M1A.1691", "M1A.1790", "M1A.1791", "M1A.2190", "M1A.2191", "M1A.2290", "M1A.2291", "M1A.2390",
				"M1A.2391", "M1A.2490", "M1A.2491", "M1A.2590", "M1A.2591", "M1A.2690", "M1A.2691", "M1A.2790",
				"M1A.2791", "M1A.3190", "M1A.3191", "M1A.3290", "M1A.3291", "M1A.3390", "M1A.3391", "M1A.3490",
				"M1A.3491", "M1A.3590", "M1A.3591", "M1A.3690", "M1A.3691", "M1A.3790", "M1A.3791", "M1A.4190",
				"M1A.4191", "M1A.4290", "M1A.4291", "M1A.4390", "M1A.4391", "M1A.4490", "M1A.4491", "M1A.4590",
				"M1A.4591", "M1A.4690", "M1A.4691", "M1A.4790", "M1A.4791", "M20.009", "M20.019", "M20.029", "M20.039",
				"M20.099", "M20.10", "M20.20", "M20.30", "M20.40", "M20.5X9", "M20.60", "M21.029", "M21.059", "M21.069",
				"M21.079", "M21.129", "M21.159", "M21.169", "M21.179", "M21.219", "M21.229", "M21.239", "M21.249",
				"M21.259", "M21.269", "M21.279", "M21.339", "M21.379", "M21.40", "M21.519", "M21.529", "M21.539",
				"M21.549", "M21.619", "M21.629", "M21.6X9", "M21.729", "M21.739", "M21.759", "M21.769", "M21.829",
				"M21.839", "M21.859", "M21.869", "M21.929", "M21.939", "M21.949", "M21.959", "M21.969", "M22.00",
				"M22.10", "M22.2X9", "M22.3X9", "M22.40", "M22.8X9", "M22.90", "M23.002", "M23.005", "M23.009",
				"M23.019", "M23.029", "M23.039", "M23.049", "M23.059", "M23.069", "M23.202", "M23.205", "M23.209",
				"M23.219", "M23.229", "M23.239", "M23.249", "M23.259", "M23.269", "M23.302", "M23.305", "M23.309",
				"M23.319", "M23.329", "M23.339", "M23.349", "M23.359", "M23.369", "M23.40", "M23.50", "M23.609",
				"M23.619", "M23.629", "M23.639", "M23.649", "M23.679", "M23.8X9", "M23.90", "M24.019", "M24.029",
				"M24.039", "M24.049", "M24.059", "M24.073", "M24.076", "M24.119", "M24.129", "M24.139", "M24.149",
				"M24.159", "M24.173", "M24.176", "M24.219", "M24.229", "M24.239", "M24.249", "M24.259", "M24.273",
				"M24.276", "M24.319", "M24.329", "M24.339", "M24.349", "M24.359", "M24.369", "M24.373", "M24.376",
				"M24.419", "M24.429", "M24.439", "M24.443", "M24.446", "M24.459", "M24.469", "M24.473", "M24.476",
				"M24.479", "M24.519", "M24.529", "M24.539", "M24.549", "M24.559", "M24.569", "M24.573", "M24.576",
				"M24.619", "M24.629", "M24.639", "M24.649", "M24.659", "M24.669", "M24.673", "M24.676", "M24.819",
				"M24.829", "M24.839", "M24.849", "M24.859", "M24.873", "M24.876", "M25.019", "M25.029", "M25.039",
				"M25.049", "M25.059", "M25.069", "M25.073", "M25.076", "M25.119", "M25.129", "M25.139", "M25.149",
				"M25.159", "M25.169", "M25.173", "M25.176", "M25.219", "M25.229", "M25.239", "M25.249", "M25.259",
				"M25.269", "M25.279", "M25.319", "M25.329", "M25.339", "M25.349", "M25.359", "M25.369", "M25.373",
				"M25.376", "M25.419", "M25.429", "M25.439", "M25.449", "M25.459", "M25.469", "M25.473", "M25.476",
				"M25.519", "M25.529", "M25.539", "M25.549", "M25.559", "M25.569", "M25.579", "M25.619", "M25.629",
				"M25.639", "M25.649", "M25.659", "M25.669", "M25.673", "M25.676", "M25.719", "M25.729", "M25.739",
				"M25.749", "M25.759", "M25.769", "M25.773", "M25.776", "M25.819", "M25.829", "M25.839", "M25.849",
				"M25.859", "M25.869", "M25.879", "M26.609", "M26.619", "M26.629", "M26.639", "M54.30", "M54.40",
				"M60.002", "M60.005", "M60.009", "M60.019", "M60.029", "M60.039", "M60.043", "M60.046", "M60.059",
				"M60.069", "M60.072", "M60.075", "M60.078", "M60.119", "M60.129", "M60.139", "M60.149", "M60.159",
				"M60.169", "M60.179", "M60.219", "M60.229", "M60.239", "M60.249", "M60.259", "M60.269", "M60.279",
				"M60.819", "M60.829", "M60.839", "M60.849", "M60.859", "M60.869", "M60.879", "M61.019", "M61.029",
				"M61.039", "M61.049", "M61.059", "M61.069", "M61.079", "M61.119", "M61.129", "M61.139", "M61.143",
				"M61.146", "M61.159", "M61.169", "M61.173", "M61.176", "M61.179", "M61.219", "M61.229", "M61.239",
				"M61.249", "M61.259", "M61.269", "M61.279", "M61.319", "M61.329", "M61.339", "M61.349", "M61.359",
				"M61.369", "M61.379", "M61.419", "M61.429", "M61.439", "M61.449", "M61.459", "M61.469", "M61.479",
				"M61.519", "M61.529", "M61.539", "M61.549", "M61.559", "M61.569", "M61.579", "M62.019", "M62.029",
				"M62.039", "M62.049", "M62.059", "M62.069", "M62.079", "M62.119", "M62.129", "M62.139", "M62.149",
				"M62.159", "M62.169", "M62.179", "M62.219", "M62.229", "M62.239", "M62.249", "M62.259", "M62.269",
				"M62.279", "M62.419", "M62.429", "M62.439", "M62.449", "M62.459", "M62.469", "M62.479", "M62.519",
				"M62.529", "M62.539", "M62.549", "M62.559", "M62.569", "M62.579", "M63.819", "M63.829", "M63.839",
				"M63.849", "M63.859", "M63.869", "M63.879", "M65.019", "M65.029", "M65.039", "M65.049", "M65.059",
				"M65.069", "M65.079", "M65.119", "M65.129", "M65.139", "M65.149", "M65.159", "M65.169", "M65.179",
				"M65.229", "M65.239", "M65.249", "M65.259", "M65.269", "M65.279", "M65.319", "M65.329", "M65.339",
				"M65.349", "M65.359", "M65.819", "M65.829", "M65.839", "M65.849", "M65.859", "M65.869", "M65.879",
				"M66.119", "M66.129", "M66.139", "M66.143", "M66.146", "M66.159", "M66.173", "M66.176", "M66.179",
				"M66.219", "M66.229", "M66.239", "M66.249", "M66.259", "M66.269", "M66.279", "M66.319", "M66.329",
				"M66.339", "M66.349", "M66.359", "M66.369", "M66.379", "M66.819", "M66.829", "M66.839", "M66.849",
				"M66.859", "M66.869", "M66.879", "M67.00", "M67.219", "M67.229", "M67.239", "M67.249", "M67.259",
				"M67.269", "M67.279", "M67.319", "M67.329", "M67.339", "M67.349", "M67.359", "M67.369", "M67.379",
				"M67.419", "M67.429", "M67.439", "M67.449", "M67.459", "M67.469", "M67.479", "M67.50", "M67.819",
				"M67.829", "M67.839", "M67.849", "M67.859", "M67.869", "M67.879", "M67.919", "M67.929", "M67.939",
				"M67.949", "M67.959", "M67.969", "M67.979", "M70.039", "M70.049", "M70.10", "M70.20", "M70.30",
				"M70.40", "M70.50", "M70.60", "M70.70", "M70.819", "M70.829", "M70.839", "M70.849", "M70.859",
				"M70.869", "M70.879", "M70.919", "M70.929", "M70.939", "M70.949", "M70.959", "M70.969", "M70.979",
				"M71.019", "M71.029", "M71.039", "M71.049", "M71.059", "M71.069", "M71.079", "M71.119", "M71.129",
				"M71.139", "M71.149", "M71.159", "M71.169", "M71.179", "M71.20", "M71.319", "M71.329", "M71.339",
				"M71.349", "M71.359", "M71.379", "M71.429", "M71.439", "M71.449", "M71.459", "M71.469", "M71.479",
				"M71.529", "M71.539", "M71.549", "M71.559", "M71.569", "M71.579", "M71.819", "M71.829", "M71.839",
				"M71.849", "M71.859", "M71.869", "M71.879", "M75.00", "M75.100", "M75.110", "M75.120", "M75.20",
				"M75.30", "M75.40", "M75.50", "M75.80", "M75.90", "M76.00", "M76.10", "M76.20", "M76.30", "M76.40",
				"M76.50", "M76.60", "M76.70", "M76.819", "M76.829", "M76.899", "M77.00", "M77.10", "M77.20", "M77.30",
				"M77.40", "M77.50", "M79.603", "M79.606", "M79.609", "M79.629", "M79.639", "M79.643", "M79.646",
				"M79.659", "M79.669", "M79.673", "M79.676", "M79.A19", "M79.A29", "M80.019?", "M80.019A", "M80.019D",
				"M80.019G", "M80.019K", "M80.019P", "M80.019S", "M80.029?", "M80.029A", "M80.029D", "M80.029G",
				"M80.029K", "M80.029P", "M80.029S", "M80.039?", "M80.039A", "M80.039D", "M80.039G", "M80.039K",
				"M80.039P", "M80.039S", "M80.049?", "M80.049A", "M80.049D", "M80.049G", "M80.049K", "M80.049P",
				"M80.049S", "M80.059?", "M80.059A", "M80.059D", "M80.059G", "M80.059K", "M80.059P", "M80.059S",
				"M80.069?", "M80.069A", "M80.069D", "M80.069G", "M80.069K", "M80.069P", "M80.069S", "M80.079?",
				"M80.079A", "M80.079D", "M80.079G", "M80.079K", "M80.079P", "M80.079S", "M80.819?", "M80.819A",
				"M80.819D", "M80.819G", "M80.819K", "M80.819P", "M80.819S", "M80.829?", "M80.829A", "M80.829D",
				"M80.829G", "M80.829K", "M80.829P", "M80.829S", "M80.839?", "M80.839A", "M80.839D", "M80.839G",
				"M80.839K", "M80.839P", "M80.839S", "M80.849?", "M80.849A", "M80.849D", "M80.849G", "M80.849K",
				"M80.849P", "M80.849S", "M80.859?", "M80.859A", "M80.859D", "M80.859G", "M80.859K", "M80.859P",
				"M80.859S", "M80.869?", "M80.869A", "M80.869D", "M80.869G", "M80.869K", "M80.869P", "M80.869S",
				"M80.879?", "M80.879A", "M80.879D", "M80.879G", "M80.879K", "M80.879P", "M80.879S", "M84.319?",
				"M84.319A", "M84.319D", "M84.319G", "M84.319K", "M84.319P", "M84.319S", "M84.329?", "M84.329A",
				"M84.329D", "M84.329G", "M84.329K", "M84.329P", "M84.329S", "M84.339?", "M84.339A", "M84.339D",
				"M84.339G", "M84.339K", "M84.339P", "M84.339S", "M84.343?", "M84.343A", "M84.343D", "M84.343G",
				"M84.343K", "M84.343P", "M84.343S", "M84.346?", "M84.346A", "M84.346D", "M84.346G", "M84.346K",
				"M84.346P", "M84.346S", "M84.353?", "M84.353A", "M84.353D", "M84.353G", "M84.353K", "M84.353P",
				"M84.353S", "M84.359?", "M84.359A", "M84.359D", "M84.359G", "M84.359K", "M84.359P", "M84.359S",
				"M84.369?", "M84.369A", "M84.369D", "M84.369G", "M84.369K", "M84.369P", "M84.369S", "M84.373?",
				"M84.373A", "M84.373D", "M84.373G", "M84.373K", "M84.373P", "M84.373S", "M84.376?", "M84.376A",
				"M84.376D", "M84.376G", "M84.376K", "M84.376P", "M84.376S", "M84.379?", "M84.379A", "M84.379D",
				"M84.379G", "M84.379K", "M84.379P", "M84.379S", "M84.419?", "M84.419A", "M84.419D", "M84.419G",
				"M84.419K", "M84.419P", "M84.419S", "M84.429?", "M84.429A", "M84.429D", "M84.429G", "M84.429K",
				"M84.429P", "M84.429S", "M84.439?", "M84.439A", "M84.439D", "M84.439G", "M84.439K", "M84.439P",
				"M84.439S", "M84.443?", "M84.443A", "M84.443D", "M84.443G", "M84.443K", "M84.443P", "M84.443S",
				"M84.446?", "M84.446A", "M84.446D", "M84.446G", "M84.446K", "M84.446P", "M84.446S", "M84.453?",
				"M84.453A", "M84.453D", "M84.453G", "M84.453K", "M84.453P", "M84.453S", "M84.459?", "M84.459A",
				"M84.459D", "M84.459G", "M84.459K", "M84.459P", "M84.459S", "M84.469?", "M84.469A", "M84.469D",
				"M84.469G", "M84.469K", "M84.469P", "M84.469S", "M84.473?", "M84.473A", "M84.473D", "M84.473G",
				"M84.473K", "M84.473P", "M84.473S", "M84.476?", "M84.476A", "M84.476D", "M84.476G", "M84.476K",
				"M84.476P", "M84.476S", "M84.479?", "M84.479A", "M84.479D", "M84.479G", "M84.479K", "M84.479P",
				"M84.479S", "M84.519?", "M84.519A", "M84.519D", "M84.519G", "M84.519K", "M84.519P", "M84.519S",
				"M84.529?", "M84.529A", "M84.529D", "M84.529G", "M84.529K", "M84.529P", "M84.529S", "M84.539?",
				"M84.539A", "M84.539D", "M84.539G", "M84.539K", "M84.539P", "M84.539S", "M84.549?", "M84.549A",
				"M84.549D", "M84.549G", "M84.549K", "M84.549P", "M84.549S", "M84.553?", "M84.553A", "M84.553D",
				"M84.553G", "M84.553K", "M84.553P", "M84.553S", "M84.559?", "M84.559A", "M84.559D", "M84.559G",
				"M84.559K", "M84.559P", "M84.559S", "M84.569?", "M84.569A", "M84.569D", "M84.569G", "M84.569K",
				"M84.569P", "M84.569S", "M84.573?", "M84.573A", "M84.573D", "M84.573G", "M84.573K", "M84.573P",
				"M84.573S", "M84.576?", "M84.576A", "M84.576D", "M84.576G", "M84.576K", "M84.576P", "M84.576S",
				"M84.619?", "M84.619A", "M84.619D", "M84.619G", "M84.619K", "M84.619P", "M84.619S", "M84.629?",
				"M84.629A", "M84.629D", "M84.629G", "M84.629K", "M84.629P", "M84.629S", "M84.639?", "M84.639A",
				"M84.639D", "M84.639G", "M84.639K", "M84.639P", "M84.639S", "M84.649?", "M84.649A", "M84.649D",
				"M84.649G", "M84.649K", "M84.649P", "M84.649S", "M84.653?", "M84.653A", "M84.653D", "M84.653G",
				"M84.653K", "M84.653P", "M84.653S", "M84.659?", "M84.659A", "M84.659D", "M84.659G", "M84.659K",
				"M84.659P", "M84.659S", "M84.669?", "M84.669A", "M84.669D", "M84.669G", "M84.669K", "M84.669P",
				"M84.669S", "M84.673?", "M84.673A", "M84.673D", "M84.673G", "M84.673K", "M84.673P", "M84.673S",
				"M84.676?", "M84.676A", "M84.676D", "M84.676G", "M84.676K", "M84.676P", "M84.676S", "M84.750?",
				"M84.750A", "M84.750D", "M84.750G", "M84.750K", "M84.750P", "M84.750S", "M84.753?", "M84.753A",
				"M84.753D", "M84.753G", "M84.753K", "M84.753P", "M84.753S", "M84.756?", "M84.756A", "M84.756D",
				"M84.756G", "M84.756K", "M84.756P", "M84.756S", "M84.759?", "M84.759A", "M84.759D", "M84.759G",
				"M84.759K", "M84.759P", "M84.759S", "M84.819", "M84.829", "M84.839", "M84.849", "M84.859", "M84.869",
				"M84.879", "M85.019", "M85.029", "M85.039", "M85.049", "M85.059", "M85.069", "M85.079", "M85.119",
				"M85.129", "M85.139", "M85.149", "M85.159", "M85.169", "M85.179", "M85.319", "M85.329", "M85.339",
				"M85.349", "M85.359", "M85.369", "M85.379", "M85.419", "M85.429", "M85.439", "M85.449", "M85.459",
				"M85.469", "M85.479", "M85.519", "M85.529", "M85.539", "M85.549", "M85.559", "M85.569", "M85.579",
				"M85.619", "M85.629", "M85.639", "M85.649", "M85.659", "M85.669", "M85.679", "M85.819", "M85.829",
				"M85.839", "M85.849", "M85.859", "M85.869", "M85.879", "M86.019", "M86.029", "M86.039", "M86.049",
				"M86.059", "M86.069", "M86.079", "M86.119", "M86.129", "M86.139", "M86.149", "M86.159", "M86.169",
				"M86.179", "M86.219", "M86.229", "M86.239", "M86.249", "M86.259", "M86.269", "M86.279", "M86.319",
				"M86.329", "M86.339", "M86.349", "M86.359", "M86.369", "M86.379", "M86.419", "M86.429", "M86.439",
				"M86.449", "M86.459", "M86.469", "M86.479", "M86.519", "M86.529", "M86.539", "M86.549", "M86.559",
				"M86.569", "M86.579", "M86.619", "M86.629", "M86.639", "M86.649", "M86.659", "M86.669", "M86.679",
				"M87.019", "M87.029", "M87.033", "M87.036", "M87.039", "M87.043", "M87.046", "M87.059", "M87.063",
				"M87.066", "M87.073", "M87.076", "M87.079", "M87.119", "M87.129", "M87.133", "M87.136", "M87.139",
				"M87.143", "M87.146", "M87.159", "M87.163", "M87.166", "M87.173", "M87.176", "M87.179", "M87.219",
				"M87.229", "M87.233", "M87.236", "M87.239", "M87.243", "M87.246", "M87.256", "M87.263", "M87.266",
				"M87.273", "M87.276", "M87.279", "M87.319", "M87.329", "M87.333", "M87.336", "M87.339", "M87.343",
				"M87.346", "M87.353", "M87.363", "M87.366", "M87.373", "M87.376", "M87.379", "M87.819", "M87.829",
				"M87.833", "M87.836", "M87.839", "M87.843", "M87.849", "M87.859", "M87.863", "M87.869", "M87.873",
				"M87.876", "M87.879", "M88.819", "M88.829", "M88.839", "M88.849", "M88.859", "M88.869", "M88.879",
				"M89.019", "M89.029", "M89.039", "M89.049", "M89.059", "M89.069", "M89.079", "M89.129", "M89.139",
				"M89.159", "M89.169", "M89.219", "M89.229", "M89.239", "M89.249", "M89.259", "M89.269", "M89.279",
				"M89.319", "M89.329", "M89.339", "M89.349", "M89.359", "M89.369", "M89.379", "M89.419", "M89.429",
				"M89.439", "M89.449", "M89.459", "M89.469", "M89.479", "M89.519", "M89.529", "M89.539", "M89.549",
				"M89.559", "M89.569", "M89.579", "M89.619", "M89.629", "M89.639", "M89.649", "M89.659", "M89.669",
				"M89.679", "M89.719", "M89.729", "M89.739", "M89.749", "M89.759", "M89.769", "M89.779", "M90.519",
				"M90.529", "M90.539", "M90.549", "M90.559", "M90.569", "M90.579", "M90.619", "M90.629", "M90.639",
				"M90.649", "M90.659", "M90.669", "M90.679", "M90.819", "M90.829", "M90.839", "M90.849", "M90.859",
				"M90.869", "M90.879", "M91.10", "M91.20", "M91.30", "M91.40", "M91.80", "M91.90", "M92.00", "M92.10",
				"M92.209", "M92.219", "M92.229", "M92.299", "M92.30", "M92.40", "M92.50", "M92.60", "M92.70", "M93.003",
				"M93.013", "M93.023", "M93.033", "M93.219", "M93.229", "M93.239", "M93.249", "M93.259", "M93.269",
				"M93.279", "M93.819", "M93.829", "M93.839", "M93.849", "M93.859", "M93.869", "M93.879", "M93.919",
				"M93.929", "M93.939", "M93.949", "M93.959", "M93.969", "M93.979", "M94.219", "M94.229", "M94.239",
				"M94.249", "M94.259", "M94.269", "M94.279", "M94.359", "M95.10", "M96.629", "M96.639", "M96.669",
				"M96.679", "N13.729", "N13.739", "N27.9", "N50.819", "N60.09", "N60.19", "N60.29", "N60.39", "N60.49",
				"N60.89", "N60.99", "N83.00", "N83.10", "N83.209", "N83.299", "N83.319", "N83.329", "N83.339", "N83.40",
				"N83.519", "N83.529", "Q52.129", "Q53.00", "Q60.2", "Q60.5", "Q66.50", "Q66.80", "Q70.00", "Q70.10",
				"Q70.20", "Q70.30", "Q71.00", "Q71.10", "Q71.20", "Q71.30", "Q71.40", "Q71.50", "Q71.60", "Q71.819",
				"Q71.899", "Q71.90", "Q72.00", "Q72.10", "Q72.20", "Q72.30", "Q72.40", "Q72.50", "Q72.60", "Q72.70",
				"Q72.819", "Q72.899", "Q72.90", "R22.30", "R22.40", "R93.429", "S00.10X?", "S00.10XA", "S00.10XD",
				"S00.10XS", "S00.209?", "S00.209A", "S00.209D", "S00.209S", "S00.219?", "S00.219A", "S00.219D",
				"S00.219S", "S00.229?", "S00.229A", "S00.229D", "S00.229S", "S00.249?", "S00.249A", "S00.249D",
				"S00.249S", "S00.259?", "S00.259A", "S00.259D", "S00.259S", "S00.269?", "S00.269A", "S00.269D",
				"S00.269S", "S00.279?", "S00.279A", "S00.279D", "S00.279S", "S00.409?", "S00.409A", "S00.409D",
				"S00.409S", "S00.419?", "S00.419A", "S00.419D", "S00.419S", "S00.429?", "S00.429A", "S00.429D",
				"S00.429S", "S00.439?", "S00.439A", "S00.439D", "S00.439S", "S00.449?", "S00.449A", "S00.449D",
				"S00.449S", "S00.459?", "S00.459A", "S00.459D", "S00.459S", "S00.469?", "S00.469A", "S00.469D",
				"S00.469S", "S00.479?", "S00.479A", "S00.479D", "S00.479S", "S01.109?", "S01.109A", "S01.109D",
				"S01.109S", "S01.119?", "S01.119A", "S01.119D", "S01.119S", "S01.129?", "S01.129A", "S01.129D",
				"S01.129S", "S01.139?", "S01.139A", "S01.139D", "S01.139S", "S01.149?", "S01.149A", "S01.149D",
				"S01.149S", "S01.159?", "S01.159A", "S01.159D", "S01.159S", "S01.309?", "S01.309A", "S01.309D",
				"S01.309S", "S01.319?", "S01.319A", "S01.319D", "S01.319S", "S01.329?", "S01.329A", "S01.329D",
				"S01.329S", "S01.339?", "S01.339A", "S01.339D", "S01.339S", "S01.349?", "S01.349A", "S01.349D",
				"S01.349S", "S01.359?", "S01.359A", "S01.359D", "S01.359S", "S01.409?", "S01.409A", "S01.409D",
				"S01.409S", "S01.419?", "S01.419A", "S01.419D", "S01.419S", "S01.429?", "S01.429A", "S01.429D",
				"S01.429S", "S01.439?", "S01.439A", "S01.439D", "S01.439S", "S01.449?", "S01.449A", "S01.449D",
				"S01.449S", "S01.459?", "S01.459A", "S01.459D", "S01.459S", "S02.109?", "S02.109A", "S02.109B",
				"S02.109D", "S02.109G", "S02.109K", "S02.109S", "S02.110?", "S02.110A", "S02.110B", "S02.110D",
				"S02.110G", "S02.110K", "S02.110S", "S02.111?", "S02.111A", "S02.111B", "S02.111D", "S02.111G",
				"S02.111K", "S02.111S", "S02.112?", "S02.112A", "S02.112B", "S02.112D", "S02.112G", "S02.112K",
				"S02.112S", "S02.113?", "S02.113A", "S02.113B", "S02.113D", "S02.113G", "S02.113K", "S02.113S",
				"S02.118?", "S02.118A", "S02.118B", "S02.118D", "S02.118G", "S02.118K", "S02.118S", "S02.119?",
				"S02.119A", "S02.119B", "S02.119D", "S02.119G", "S02.119K", "S02.119S", "S02.30X?", "S02.30XA",
				"S02.30XB", "S02.30XD", "S02.30XG", "S02.30XK", "S02.30XS", "S02.400?", "S02.400A", "S02.400B",
				"S02.400D", "S02.400G", "S02.400K", "S02.400S", "S02.401?", "S02.401A", "S02.401B", "S02.401D",
				"S02.401G", "S02.401K", "S02.401S", "S02.402?", "S02.402A", "S02.402B", "S02.402D", "S02.402G",
				"S02.402K", "S02.402S", "S02.600?", "S02.600A", "S02.600B", "S02.600D", "S02.600G", "S02.600K",
				"S02.600S", "S02.609?", "S02.609A", "S02.609B", "S02.609D", "S02.609G", "S02.609K", "S02.609S",
				"S02.610?", "S02.610A", "S02.610B", "S02.610D", "S02.610G", "S02.610K", "S02.610S", "S02.620?",
				"S02.620A", "S02.620B", "S02.620D", "S02.620G", "S02.620K", "S02.620S", "S02.630?", "S02.630A",
				"S02.630B", "S02.630D", "S02.630G", "S02.630K", "S02.630S", "S02.640?", "S02.640A", "S02.640B",
				"S02.640D", "S02.640G", "S02.640K", "S02.640S", "S02.650?", "S02.650A", "S02.650B", "S02.650D",
				"S02.650G", "S02.650K", "S02.650S", "S02.670?", "S02.670A", "S02.670B", "S02.670D", "S02.670G",
				"S02.670K", "S02.670S", "S02.80X?", "S02.80XA", "S02.80XB", "S02.80XD", "S02.80XG", "S02.80XK",
				"S02.80XS", "S03.00X?", "S03.00XA", "S03.00XD", "S03.00XS", "S03.40X?", "S03.40XA", "S03.40XD",
				"S03.40XS", "S04.019?", "S04.019A", "S04.019D", "S04.019S", "S04.039?", "S04.039A", "S04.039D",
				"S04.039S", "S04.049?", "S04.049A", "S04.049D", "S04.049S", "S04.10X?", "S04.10XA", "S04.10XD",
				"S04.10XS", "S04.20X?", "S04.20XA", "S04.20XD", "S04.20XS", "S04.30X?", "S04.30XA", "S04.30XD",
				"S04.30XS", "S04.40X?", "S04.40XA", "S04.40XD", "S04.40XS", "S04.50X?", "S04.50XA", "S04.50XD",
				"S04.50XS", "S04.60X?", "S04.60XA", "S04.60XD", "S04.60XS", "S04.70X?", "S04.70XA", "S04.70XD",
				"S04.70XS", "S04.819?", "S04.819A", "S04.819D", "S04.819S", "S04.899?", "S04.899A", "S04.899D",
				"S04.899S", "S05.00X?", "S05.00XA", "S05.00XD", "S05.00XS", "S05.10X?", "S05.10XA", "S05.10XD",
				"S05.10XS", "S05.20X?", "S05.20XA", "S05.20XD", "S05.20XS", "S05.30X?", "S05.30XA", "S05.30XD",
				"S05.30XS", "S05.40X?", "S05.40XA", "S05.40XD", "S05.40XS", "S05.50X?", "S05.50XA", "S05.50XD",
				"S05.50XS", "S05.60X?", "S05.60XA", "S05.60XD", "S05.60XS", "S05.70X?", "S05.70XA", "S05.70XD",
				"S05.70XS", "S05.8X9?", "S05.8X9A", "S05.8X9D", "S05.8X9S", "S05.90X?", "S05.90XA", "S05.90XD",
				"S05.90XS", "S08.119?", "S08.119A", "S08.119D", "S08.119S", "S08.129?", "S08.129A", "S08.129D",
				"S08.129S", "S09.20X?", "S09.20XA", "S09.20XD", "S09.20XS", "S09.309?", "S09.309A", "S09.309D",
				"S09.309S", "S09.319?", "S09.319A", "S09.319D", "S09.319S", "S09.399?", "S09.399A", "S09.399D",
				"S09.399S", "S15.009?", "S15.009A", "S15.009D", "S15.009S", "S15.019?", "S15.019A", "S15.019D",
				"S15.019S", "S15.029?", "S15.029A", "S15.029D", "S15.029S", "S15.099?", "S15.099A", "S15.099D",
				"S15.099S", "S15.109?", "S15.109A", "S15.109D", "S15.109S", "S15.119?", "S15.119A", "S15.119D",
				"S15.119S", "S15.129?", "S15.129A", "S15.129D", "S15.129S", "S15.199?", "S15.199A", "S15.199D",
				"S15.199S", "S15.209?", "S15.209A", "S15.209D", "S15.209S", "S15.219?", "S15.219A", "S15.219D",
				"S15.219S", "S15.229?", "S15.229A", "S15.229D", "S15.229S", "S15.299?", "S15.299A", "S15.299D",
				"S15.299S", "S15.309?", "S15.309A", "S15.309D", "S15.309S", "S15.319?", "S15.319A", "S15.319D",
				"S15.319S", "S15.329?", "S15.329A", "S15.329D", "S15.329S", "S15.399?", "S15.399A", "S15.399D",
				"S15.399S", "S20.00X?", "S20.00XA", "S20.00XD", "S20.00XS", "S20.109?", "S20.109A", "S20.109D",
				"S20.109S", "S20.119?", "S20.119A", "S20.119D", "S20.119S", "S20.129?", "S20.129A", "S20.129D",
				"S20.129S", "S20.149?", "S20.149A", "S20.149D", "S20.149S", "S20.159?", "S20.159A", "S20.159D",
				"S20.159S", "S20.169?", "S20.169A", "S20.169D", "S20.169S", "S20.179?", "S20.179A", "S20.179D",
				"S20.179S", "S20.219?", "S20.219A", "S20.219D", "S20.219S", "S20.229?", "S20.229A", "S20.229D",
				"S20.229S", "S20.309?", "S20.309A", "S20.309D", "S20.309S", "S20.319?", "S20.319A", "S20.319D",
				"S20.319S", "S20.329?", "S20.329A", "S20.329D", "S20.329S", "S20.349?", "S20.349A", "S20.349D",
				"S20.349S", "S20.359?", "S20.359A", "S20.359D", "S20.359S", "S20.369?", "S20.369A", "S20.369D",
				"S20.369S", "S20.379?", "S20.379A", "S20.379D", "S20.379S", "S20.409?", "S20.409A", "S20.409D",
				"S20.409S", "S20.419?", "S20.419A", "S20.419D", "S20.419S", "S20.429?", "S20.429A", "S20.429D",
				"S20.429S", "S20.449?", "S20.449A", "S20.449D", "S20.449S", "S20.459?", "S20.459A", "S20.459D",
				"S20.459S", "S20.469?", "S20.469A", "S20.469D", "S20.469S", "S20.479?", "S20.479A", "S20.479D",
				"S20.479S", "S21.009?", "S21.009A", "S21.009D", "S21.009S", "S21.019?", "S21.019A", "S21.019D",
				"S21.019S", "S21.029?", "S21.029A", "S21.029D", "S21.029S", "S21.039?", "S21.039A", "S21.039D",
				"S21.039S", "S21.049?", "S21.049A", "S21.049D", "S21.049S", "S21.059?", "S21.059A", "S21.059D",
				"S21.059S", "S21.109?", "S21.109A", "S21.109D", "S21.109S", "S21.119?", "S21.119A", "S21.119D",
				"S21.119S", "S21.129?", "S21.129A", "S21.129D", "S21.129S", "S21.139?", "S21.139A", "S21.139D",
				"S21.139S", "S21.149?", "S21.149A", "S21.149D", "S21.149S", "S21.159?", "S21.159A", "S21.159D",
				"S21.159S", "S21.209?", "S21.209A", "S21.209D", "S21.209S", "S21.219?", "S21.219A", "S21.219D",
				"S21.219S", "S21.229?", "S21.229A", "S21.229D", "S21.229S", "S21.239?", "S21.239A", "S21.239D",
				"S21.239S", "S21.249?", "S21.249A", "S21.249D", "S21.249S", "S21.259?", "S21.259A", "S21.259D",
				"S21.259S", "S21.309?", "S21.309A", "S21.309D", "S21.309S", "S21.319?", "S21.319A", "S21.319D",
				"S21.319S", "S21.329?", "S21.329A", "S21.329D", "S21.329S", "S21.339?", "S21.339A", "S21.339D",
				"S21.339S", "S21.349?", "S21.349A", "S21.349D", "S21.349S", "S21.359?", "S21.359A", "S21.359D",
				"S21.359S", "S21.409?", "S21.409A", "S21.409D", "S21.409S", "S21.419?", "S21.419A", "S21.419D",
				"S21.419S", "S21.429?", "S21.429A", "S21.429D", "S21.429S", "S21.439?", "S21.439A", "S21.439D",
				"S21.439S", "S21.449?", "S21.449A", "S21.449D", "S21.449S", "S21.459?", "S21.459A", "S21.459D",
				"S21.459S", "S22.39X?", "S22.39XA", "S22.39XB", "S22.39XD", "S22.39XG", "S22.39XK", "S22.39XS",
				"S22.49X?", "S22.49XA", "S22.49XB", "S22.49XD", "S22.49XG", "S22.49XK", "S22.49XS", "S25.109?",
				"S25.109A", "S25.109D", "S25.109S", "S25.119?", "S25.119A", "S25.119D", "S25.119S", "S25.129?",
				"S25.129A", "S25.129D", "S25.129S", "S25.199?", "S25.199A", "S25.199D", "S25.199S", "S25.309?",
				"S25.309A", "S25.309D", "S25.309S", "S25.319?", "S25.319A", "S25.319D", "S25.319S", "S25.329?",
				"S25.329A", "S25.329D", "S25.329S", "S25.399?", "S25.399A", "S25.399D", "S25.399S", "S25.409?",
				"S25.409A", "S25.409D", "S25.409S", "S25.419?", "S25.419A", "S25.419D", "S25.419S", "S25.429?",
				"S25.429A", "S25.429D", "S25.429S", "S25.499?", "S25.499A", "S25.499D", "S25.499S", "S25.509?",
				"S25.509A", "S25.509D", "S25.509S", "S25.519?", "S25.519A", "S25.519D", "S25.519S", "S25.599?",
				"S25.599A", "S25.599D", "S25.599S", "S25.809?", "S25.809A", "S25.809D", "S25.809S", "S25.819?",
				"S25.819A", "S25.819D", "S25.819S", "S25.899?", "S25.899A", "S25.899D", "S25.899S", "S27.309?",
				"S27.309A", "S27.309D", "S27.309S", "S27.319?", "S27.319A", "S27.319D", "S27.319S", "S27.329?",
				"S27.329A", "S27.329D", "S27.329S", "S27.339?", "S27.339A", "S27.339D", "S27.339S", "S27.399?",
				"S27.399A", "S27.399D", "S27.399S", "S27.409?", "S27.409A", "S27.409D", "S27.409S", "S27.419?",
				"S27.419A", "S27.419D", "S27.419S", "S27.429?", "S27.429A", "S27.429D", "S27.429S", "S27.439?",
				"S27.439A", "S27.439D", "S27.439S", "S27.499?", "S27.499A", "S27.499D", "S27.499S", "S28.219?",
				"S28.219A", "S28.219D", "S28.219S", "S28.229?", "S28.229A", "S28.229D", "S28.229S", "S31.102?",
				"S31.102A", "S31.102D", "S31.102S", "S31.105?", "S31.105A", "S31.105D", "S31.105S", "S31.109?",
				"S31.109A", "S31.109D", "S31.109S", "S31.119?", "S31.119A", "S31.119D", "S31.119S", "S31.129?",
				"S31.129A", "S31.129D", "S31.129S", "S31.139?", "S31.139A", "S31.139D", "S31.139S", "S31.149?",
				"S31.149A", "S31.149D", "S31.149S", "S31.159?", "S31.159A", "S31.159D", "S31.159S", "S31.602?",
				"S31.602A", "S31.602D", "S31.602S", "S31.605?", "S31.605A", "S31.605D", "S31.605S", "S31.609?",
				"S31.609A", "S31.609D", "S31.609S", "S31.619?", "S31.619A", "S31.619D", "S31.619S", "S31.629?",
				"S31.629A", "S31.629D", "S31.629S", "S31.639?", "S31.639A", "S31.639D", "S31.639S", "S31.649?",
				"S31.649A", "S31.649D", "S31.649S", "S31.659?", "S31.659A", "S31.659D", "S31.659S", "S32.309?",
				"S32.309A", "S32.309B", "S32.309D", "S32.309G", "S32.309K", "S32.309S", "S32.313?", "S32.313A",
				"S32.313B", "S32.313D", "S32.313G", "S32.313K", "S32.313S", "S32.316?", "S32.316A", "S32.316B",
				"S32.316D", "S32.316G", "S32.316K", "S32.316S", "S32.399?", "S32.399A", "S32.399B", "S32.399D",
				"S32.399G", "S32.399K", "S32.399S", "S32.409?", "S32.409A", "S32.409B", "S32.409D", "S32.409G",
				"S32.409K", "S32.409S", "S32.413?", "S32.413A", "S32.413B", "S32.413D", "S32.413G", "S32.413K",
				"S32.413S", "S32.416?", "S32.416A", "S32.416B", "S32.416D", "S32.416G", "S32.416K", "S32.416S",
				"S32.423?", "S32.423A", "S32.423B", "S32.423D", "S32.423G", "S32.423K", "S32.423S", "S32.426?",
				"S32.426A", "S32.426B", "S32.426D", "S32.426G", "S32.426K", "S32.426S", "S32.433?", "S32.433A",
				"S32.433B", "S32.433D", "S32.433G", "S32.433K", "S32.433S", "S32.436?", "S32.436A", "S32.436B",
				"S32.436D", "S32.436G", "S32.436K", "S32.436S", "S32.443?", "S32.443A", "S32.443B", "S32.443D",
				"S32.443G", "S32.443K", "S32.443S", "S32.446?", "S32.446A", "S32.446B", "S32.446D", "S32.446G",
				"S32.446K", "S32.446S", "S32.453?", "S32.453A", "S32.453B", "S32.453D", "S32.453G", "S32.453K",
				"S32.453S", "S32.456?", "S32.456A", "S32.456B", "S32.456D", "S32.456G", "S32.456K", "S32.456S",
				"S32.463?", "S32.463A", "S32.463B", "S32.463D", "S32.463G", "S32.463K", "S32.463S", "S32.466?",
				"S32.466A", "S32.466B", "S32.466D", "S32.466G", "S32.466K", "S32.466S", "S32.473?", "S32.473A",
				"S32.473B", "S32.473D", "S32.473G", "S32.473K", "S32.473S", "S32.476?", "S32.476A", "S32.476B",
				"S32.476D", "S32.476G", "S32.476K", "S32.476S", "S32.483?", "S32.483A", "S32.483B", "S32.483D",
				"S32.483G", "S32.483K", "S32.483S", "S32.486?", "S32.486A", "S32.486B", "S32.486D", "S32.486G",
				"S32.486K", "S32.486S", "S32.499?", "S32.499A", "S32.499B", "S32.499D", "S32.499G", "S32.499K",
				"S32.499S", "S32.509?", "S32.509A", "S32.509B", "S32.509D", "S32.509G", "S32.509K", "S32.509S",
				"S32.519?", "S32.519A", "S32.519B", "S32.519D", "S32.519G", "S32.519K", "S32.519S", "S32.599?",
				"S32.599A", "S32.599B", "S32.599D", "S32.599G", "S32.599K", "S32.599S", "S32.609?", "S32.609A",
				"S32.609B", "S32.609D", "S32.609G", "S32.609K", "S32.609S", "S32.613?", "S32.613A", "S32.613B",
				"S32.613D", "S32.613G", "S32.613K", "S32.613S", "S32.616?", "S32.616A", "S32.616B", "S32.616D",
				"S32.616G", "S32.616K", "S32.616S", "S32.699?", "S32.699A", "S32.699B", "S32.699D", "S32.699G",
				"S32.699K", "S32.699S", "S35.403?", "S35.403A", "S35.403D", "S35.403S", "S35.406?", "S35.406A",
				"S35.406D", "S35.406S", "S35.413?", "S35.413A", "S35.413D", "S35.413S", "S35.416?", "S35.416A",
				"S35.416D", "S35.416S", "S35.493?", "S35.493A", "S35.493D", "S35.493S", "S35.496?", "S35.496A",
				"S35.496D", "S35.496S", "S35.513?", "S35.513A", "S35.513D", "S35.513S", "S35.516?", "S35.516A",
				"S35.516D", "S35.516S", "S35.533?", "S35.533A", "S35.533D", "S35.533S", "S35.536?", "S35.536A",
				"S35.536D", "S35.536S", "S37.009?", "S37.009A", "S37.009D", "S37.009S", "S37.019?", "S37.019A",
				"S37.019D", "S37.019S", "S37.029?", "S37.029A", "S37.029D", "S37.029S", "S37.039?", "S37.039A",
				"S37.039D", "S37.039S", "S37.049?", "S37.049A", "S37.049D", "S37.049S", "S37.059?", "S37.059A",
				"S37.059D", "S37.059S", "S37.069?", "S37.069A", "S37.069D", "S37.069S", "S37.099?", "S37.099A",
				"S37.099D", "S37.099S", "S37.409?", "S37.409A", "S37.409D", "S37.409S", "S37.429?", "S37.429A",
				"S37.429D", "S37.429S", "S37.439?", "S37.439A", "S37.439D", "S37.439S", "S37.499?", "S37.499A",
				"S37.499D", "S37.499S", "S37.509?", "S37.509A", "S37.509D", "S37.509S", "S37.519?", "S37.519A",
				"S37.519D", "S37.519S", "S37.529?", "S37.529A", "S37.529D", "S37.529S", "S37.539?", "S37.539A",
				"S37.539D", "S37.539S", "S37.599?", "S37.599A", "S37.599D", "S37.599S", "S40.019?", "S40.019A",
				"S40.019D", "S40.019S", "S40.029?", "S40.029A", "S40.029D", "S40.029S", "S40.219?", "S40.219A",
				"S40.219D", "S40.219S", "S40.229?", "S40.229A", "S40.229D", "S40.229S", "S40.249?", "S40.249A",
				"S40.249D", "S40.249S", "S40.259?", "S40.259A", "S40.259D", "S40.259S", "S40.269?", "S40.269A",
				"S40.269D", "S40.269S", "S40.279?", "S40.279A", "S40.279D", "S40.279S", "S40.819?", "S40.819A",
				"S40.819D", "S40.819S", "S40.829?", "S40.829A", "S40.829D", "S40.829S", "S40.849?", "S40.849A",
				"S40.849D", "S40.849S", "S40.859?", "S40.859A", "S40.859D", "S40.859S", "S40.869?", "S40.869A",
				"S40.869D", "S40.869S", "S40.879?", "S40.879A", "S40.879D", "S40.879S", "S40.919?", "S40.919A",
				"S40.919D", "S40.919S", "S40.929?", "S40.929A", "S40.929D", "S40.929S", "S41.009?", "S41.009A",
				"S41.009D", "S41.009S", "S41.019?", "S41.019A", "S41.019D", "S41.019S", "S41.029?", "S41.029A",
				"S41.029D", "S41.029S", "S41.039?", "S41.039A", "S41.039D", "S41.039S", "S41.049?", "S41.049A",
				"S41.049D", "S41.049S", "S41.059?", "S41.059A", "S41.059D", "S41.059S", "S41.109?", "S41.109A",
				"S41.109D", "S41.109S", "S41.119?", "S41.119A", "S41.119D", "S41.119S", "S41.129?", "S41.129A",
				"S41.129D", "S41.129S", "S41.139?", "S41.139A", "S41.139D", "S41.139S", "S41.149?", "S41.149A",
				"S41.149D", "S41.149S", "S41.159?", "S41.159A", "S41.159D", "S41.159S", "S42.009?", "S42.009A",
				"S42.009B", "S42.009D", "S42.009G", "S42.009K", "S42.009P", "S42.009S", "S42.013?", "S42.013A",
				"S42.013B", "S42.013D", "S42.013G", "S42.013K", "S42.013P", "S42.013S", "S42.016?", "S42.016A",
				"S42.016B", "S42.016D", "S42.016G", "S42.016K", "S42.016P", "S42.016S", "S42.019?", "S42.019A",
				"S42.019B", "S42.019D", "S42.019G", "S42.019K", "S42.019P", "S42.019S", "S42.023?", "S42.023A",
				"S42.023B", "S42.023D", "S42.023G", "S42.023K", "S42.023P", "S42.023S", "S42.026?", "S42.026A",
				"S42.026B", "S42.026D", "S42.026G", "S42.026K", "S42.026P", "S42.026S", "S42.033?", "S42.033A",
				"S42.033B", "S42.033D", "S42.033G", "S42.033K", "S42.033P", "S42.033S", "S42.036?", "S42.036A",
				"S42.036B", "S42.036D", "S42.036G", "S42.036K", "S42.036P", "S42.036S", "S42.109?", "S42.109A",
				"S42.109B", "S42.109D", "S42.109G", "S42.109K", "S42.109P", "S42.109S", "S42.113?", "S42.113A",
				"S42.113B", "S42.113D", "S42.113G", "S42.113K", "S42.113P", "S42.113S", "S42.116?", "S42.116A",
				"S42.116B", "S42.116D", "S42.116G", "S42.116K", "S42.116P", "S42.116S", "S42.123?", "S42.123A",
				"S42.123B", "S42.123D", "S42.123G", "S42.123K", "S42.123P", "S42.123S", "S42.126?", "S42.126A",
				"S42.126B", "S42.126D", "S42.126G", "S42.126K", "S42.126P", "S42.126S", "S42.133?", "S42.133A",
				"S42.133B", "S42.133D", "S42.133G", "S42.133K", "S42.133P", "S42.133S", "S42.136?", "S42.136A",
				"S42.136B", "S42.136D", "S42.136G", "S42.136K", "S42.136P", "S42.136S", "S42.143?", "S42.143A",
				"S42.143B", "S42.143D", "S42.143G", "S42.143K", "S42.143P", "S42.143S", "S42.146?", "S42.146A",
				"S42.146B", "S42.146D", "S42.146G", "S42.146K", "S42.146P", "S42.146S", "S42.153?", "S42.153A",
				"S42.153B", "S42.153D", "S42.153G", "S42.153K", "S42.153P", "S42.153S", "S42.156?", "S42.156A",
				"S42.156B", "S42.156D", "S42.156G", "S42.156K", "S42.156P", "S42.156S", "S42.199?", "S42.199A",
				"S42.199B", "S42.199D", "S42.199G", "S42.199K", "S42.199P", "S42.199S", "S42.209?", "S42.209A",
				"S42.209B", "S42.209D", "S42.209G", "S42.209K", "S42.209P", "S42.209S", "S42.213?", "S42.213A",
				"S42.213B", "S42.213D", "S42.213G", "S42.213K", "S42.213P", "S42.213S", "S42.216?", "S42.216A",
				"S42.216B", "S42.216D", "S42.216G", "S42.216K", "S42.216P", "S42.216S", "S42.223?", "S42.223A",
				"S42.223B", "S42.223D", "S42.223G", "S42.223K", "S42.223P", "S42.223S", "S42.226?", "S42.226A",
				"S42.226B", "S42.226D", "S42.226G", "S42.226K", "S42.226P", "S42.226S", "S42.239?", "S42.239A",
				"S42.239B", "S42.239D", "S42.239G", "S42.239K", "S42.239P", "S42.239S", "S42.249?", "S42.249A",
				"S42.249B", "S42.249D", "S42.249G", "S42.249K", "S42.249P", "S42.249S", "S42.253?", "S42.253A",
				"S42.253B", "S42.253D", "S42.253G", "S42.253K", "S42.253P", "S42.253S", "S42.256?", "S42.256A",
				"S42.256B", "S42.256D", "S42.256G", "S42.256K", "S42.256P", "S42.256S", "S42.263?", "S42.263A",
				"S42.263B", "S42.263D", "S42.263G", "S42.263K", "S42.263P", "S42.263S", "S42.266?", "S42.266A",
				"S42.266B", "S42.266D", "S42.266G", "S42.266K", "S42.266P", "S42.266S", "S42.279?", "S42.279A",
				"S42.279D", "S42.279G", "S42.279K", "S42.279P", "S42.279S", "S42.293?", "S42.293A", "S42.293B",
				"S42.293D", "S42.293G", "S42.293K", "S42.293P", "S42.293S", "S42.296?", "S42.296A", "S42.296B",
				"S42.296D", "S42.296G", "S42.296K", "S42.296P", "S42.296S", "S42.309?", "S42.309A", "S42.309B",
				"S42.309D", "S42.309G", "S42.309K", "S42.309P", "S42.309S", "S42.319?", "S42.319A", "S42.319D",
				"S42.319G", "S42.319K", "S42.319P", "S42.319S", "S42.323?", "S42.323A", "S42.323B", "S42.323D",
				"S42.323G", "S42.323K", "S42.323P", "S42.323S", "S42.326?", "S42.326A", "S42.326B", "S42.326D",
				"S42.326G", "S42.326K", "S42.326P", "S42.326S", "S42.333?", "S42.333A", "S42.333B", "S42.333D",
				"S42.333G", "S42.333K", "S42.333P", "S42.333S", "S42.336?", "S42.336A", "S42.336B", "S42.336D",
				"S42.336G", "S42.336K", "S42.336P", "S42.336S", "S42.343?", "S42.343A", "S42.343B", "S42.343D",
				"S42.343G", "S42.343K", "S42.343P", "S42.343S", "S42.346?", "S42.346A", "S42.346B", "S42.346D",
				"S42.346G", "S42.346K", "S42.346P", "S42.346S", "S42.353?", "S42.353A", "S42.353B", "S42.353D",
				"S42.353G", "S42.353K", "S42.353P", "S42.353S", "S42.356?", "S42.356A", "S42.356B", "S42.356D",
				"S42.356G", "S42.356K", "S42.356P", "S42.356S", "S42.363?", "S42.363A", "S42.363B", "S42.363D",
				"S42.363G", "S42.363K", "S42.363P", "S42.363S", "S42.366?", "S42.366A", "S42.366B", "S42.366D",
				"S42.366G", "S42.366K", "S42.366P", "S42.366S", "S42.399?", "S42.399A", "S42.399B", "S42.399D",
				"S42.399G", "S42.399K", "S42.399P", "S42.399S", "S42.409?", "S42.409A", "S42.409B", "S42.409D",
				"S42.409G", "S42.409K", "S42.409P", "S42.409S", "S42.413?", "S42.413A", "S42.413B", "S42.413D",
				"S42.413G", "S42.413K", "S42.413P", "S42.413S", "S42.416?", "S42.416A", "S42.416B", "S42.416D",
				"S42.416G", "S42.416K", "S42.416P", "S42.416S", "S42.423?", "S42.423A", "S42.423B", "S42.423D",
				"S42.423G", "S42.423K", "S42.423P", "S42.423S", "S42.426?", "S42.426A", "S42.426B", "S42.426D",
				"S42.426G", "S42.426K", "S42.426P", "S42.426S", "S42.433?", "S42.433A", "S42.433B", "S42.433D",
				"S42.433G", "S42.433K", "S42.433P", "S42.433S", "S42.436?", "S42.436A", "S42.436B", "S42.436D",
				"S42.436G", "S42.436K", "S42.436P", "S42.436S", "S42.443?", "S42.443A", "S42.443B", "S42.443D",
				"S42.443G", "S42.443K", "S42.443P", "S42.443S", "S42.446?", "S42.446A", "S42.446B", "S42.446D",
				"S42.446G", "S42.446K", "S42.446P", "S42.446S", "S42.449?", "S42.449A", "S42.449B", "S42.449D",
				"S42.449G", "S42.449K", "S42.449P", "S42.449S", "S42.453?", "S42.453A", "S42.453B", "S42.453D",
				"S42.453G", "S42.453K", "S42.453P", "S42.453S", "S42.456?", "S42.456A", "S42.456B", "S42.456D",
				"S42.456G", "S42.456K", "S42.456P", "S42.456S", "S42.463?", "S42.463A", "S42.463B", "S42.463D",
				"S42.463G", "S42.463K", "S42.463P", "S42.463S", "S42.466?", "S42.466A", "S42.466B", "S42.466D",
				"S42.466G", "S42.466K", "S42.466P", "S42.466S", "S42.473?", "S42.473A", "S42.473B", "S42.473D",
				"S42.473G", "S42.473K", "S42.473P", "S42.473S", "S42.476?", "S42.476A", "S42.476B", "S42.476D",
				"S42.476G", "S42.476K", "S42.476P", "S42.476S", "S42.489?", "S42.489A", "S42.489D", "S42.489G",
				"S42.489K", "S42.489P", "S42.489S", "S42.493?", "S42.493A", "S42.493B", "S42.493D", "S42.493G",
				"S42.493K", "S42.493P", "S42.493S", "S42.496?", "S42.496A", "S42.496B", "S42.496D", "S42.496G",
				"S42.496K", "S42.496P", "S42.496S", "S42.90X?", "S42.90XA", "S42.90XB", "S42.90XD", "S42.90XG",
				"S42.90XK", "S42.90XP", "S42.90XS", "S43.003?", "S43.003A", "S43.003D", "S43.003S", "S43.006?",
				"S43.006A", "S43.006D", "S43.006S", "S43.013?", "S43.013A", "S43.013D", "S43.013S", "S43.016?",
				"S43.016A", "S43.016D", "S43.016S", "S43.023?", "S43.023A", "S43.023D", "S43.023S", "S43.026?",
				"S43.026A", "S43.026D", "S43.026S", "S43.033?", "S43.033A", "S43.033D", "S43.033S", "S43.036?",
				"S43.036A", "S43.036D", "S43.036S", "S43.083?", "S43.083A", "S43.083D", "S43.083S", "S43.086?",
				"S43.086A", "S43.086D", "S43.086S", "S43.109?", "S43.109A", "S43.109D", "S43.109S", "S43.119?",
				"S43.119A", "S43.119D", "S43.119S", "S43.129?", "S43.129A", "S43.129D", "S43.129S", "S43.139?",
				"S43.139A", "S43.139D", "S43.139S", "S43.149?", "S43.149A", "S43.149D", "S43.149S", "S43.159?",
				"S43.159A", "S43.159D", "S43.159S", "S43.203?", "S43.203A", "S43.203D", "S43.203S", "S43.206?",
				"S43.206A", "S43.206D", "S43.206S", "S43.213?", "S43.213A", "S43.213D", "S43.213S", "S43.216?",
				"S43.216A", "S43.216D", "S43.216S", "S43.223?", "S43.223A", "S43.223D", "S43.223S", "S43.226?",
				"S43.226A", "S43.226D", "S43.226S", "S43.303?", "S43.303A", "S43.303D", "S43.303S", "S43.306?",
				"S43.306A", "S43.306D", "S43.306S", "S43.313?", "S43.313A", "S43.313D", "S43.313S", "S43.316?",
				"S43.316A", "S43.316D", "S43.316S", "S43.393?", "S43.393A", "S43.393D", "S43.393S", "S43.396?",
				"S43.396A", "S43.396D", "S43.396S", "S43.409?", "S43.409A", "S43.409D", "S43.409S", "S43.419?",
				"S43.419A", "S43.419D", "S43.419S", "S43.429?", "S43.429A", "S43.429D", "S43.429S", "S43.439?",
				"S43.439A", "S43.439D", "S43.439S", "S43.499?", "S43.499A", "S43.499D", "S43.499S", "S43.50X?",
				"S43.50XA", "S43.50XD", "S43.50XS", "S43.60X?", "S43.60XA", "S43.60XD", "S43.60XS", "S43.80X?",
				"S43.80XA", "S43.80XD", "S43.80XS", "S43.90X?", "S43.90XA", "S43.90XD", "S43.90XS", "S44.00X?",
				"S44.00XA", "S44.00XD", "S44.00XS", "S44.10X?", "S44.10XA", "S44.10XD", "S44.10XS", "S44.20X?",
				"S44.20XA", "S44.20XD", "S44.20XS", "S44.30X?", "S44.30XA", "S44.30XD", "S44.30XS", "S44.40X?",
				"S44.40XA", "S44.40XD", "S44.40XS", "S44.50X?", "S44.50XA", "S44.50XD", "S44.50XS", "S44.8X9?",
				"S44.8X9A", "S44.8X9D", "S44.8X9S", "S44.90X?", "S44.90XA", "S44.90XD", "S44.90XS", "S45.009?",
				"S45.009A", "S45.009D", "S45.009S", "S45.019?", "S45.019A", "S45.019D", "S45.019S", "S45.099?",
				"S45.099A", "S45.099D", "S45.099S", "S45.109?", "S45.109A", "S45.109D", "S45.109S", "S45.119?",
				"S45.119A", "S45.119D", "S45.119S", "S45.199?", "S45.199A", "S45.199D", "S45.199S", "S45.209?",
				"S45.209A", "S45.209D", "S45.209S", "S45.219?", "S45.219A", "S45.219D", "S45.219S", "S45.299?",
				"S45.299A", "S45.299D", "S45.299S", "S45.309?", "S45.309A", "S45.309D", "S45.309S", "S45.319?",
				"S45.319A", "S45.319D", "S45.319S", "S45.399?", "S45.399A", "S45.399D", "S45.399S", "S45.809?",
				"S45.809A", "S45.809D", "S45.809S", "S45.819?", "S45.819A", "S45.819D", "S45.819S", "S45.899?",
				"S45.899A", "S45.899D", "S45.899S", "S45.909?", "S45.909A", "S45.909D", "S45.909S", "S45.919?",
				"S45.919A", "S45.919D", "S45.919S", "S45.999?", "S45.999A", "S45.999D", "S45.999S", "S46.009?",
				"S46.009A", "S46.009D", "S46.009S", "S46.019?", "S46.019A", "S46.019D", "S46.019S", "S46.029?",
				"S46.029A", "S46.029D", "S46.029S", "S46.099?", "S46.099A", "S46.099D", "S46.099S", "S46.109?",
				"S46.109A", "S46.109D", "S46.109S", "S46.119?", "S46.119A", "S46.119D", "S46.119S", "S46.129?",
				"S46.129A", "S46.129D", "S46.129S", "S46.199?", "S46.199A", "S46.199D", "S46.199S", "S46.209?",
				"S46.209A", "S46.209D", "S46.209S", "S46.219?", "S46.219A", "S46.219D", "S46.219S", "S46.229?",
				"S46.229A", "S46.229D", "S46.229S", "S46.299?", "S46.299A", "S46.299D", "S46.299S", "S46.309?",
				"S46.309A", "S46.309D", "S46.309S", "S46.319?", "S46.319A", "S46.319D", "S46.319S", "S46.329?",
				"S46.329A", "S46.329D", "S46.329S", "S46.399?", "S46.399A", "S46.399D", "S46.399S", "S46.809?",
				"S46.809A", "S46.809D", "S46.809S", "S46.819?", "S46.819A", "S46.819D", "S46.819S", "S46.829?",
				"S46.829A", "S46.829D", "S46.829S", "S46.899?", "S46.899A", "S46.899D", "S46.899S", "S46.909?",
				"S46.909A", "S46.909D", "S46.909S", "S46.919?", "S46.919A", "S46.919D", "S46.919S", "S46.929?",
				"S46.929A", "S46.929D", "S46.929S", "S46.999?", "S46.999A", "S46.999D", "S46.999S", "S47.9XX?",
				"S47.9XXA", "S47.9XXD", "S47.9XXS", "S48.019?", "S48.019A", "S48.019D", "S48.019S", "S48.029?",
				"S48.029A", "S48.029D", "S48.029S", "S48.119?", "S48.119A", "S48.119D", "S48.119S", "S48.129?",
				"S48.129A", "S48.129D", "S48.129S", "S48.919?", "S48.919A", "S48.919D", "S48.919S", "S48.929?",
				"S48.929A", "S48.929D", "S48.929S", "S49.009?", "S49.009A", "S49.009D", "S49.009G", "S49.009K",
				"S49.009P", "S49.009S", "S49.019?", "S49.019A", "S49.019D", "S49.019G", "S49.019K", "S49.019P",
				"S49.019S", "S49.029?", "S49.029A", "S49.029D", "S49.029G", "S49.029K", "S49.029P", "S49.029S",
				"S49.039?", "S49.039A", "S49.039D", "S49.039G", "S49.039K", "S49.039P", "S49.039S", "S49.049?",
				"S49.049A", "S49.049D", "S49.049G", "S49.049K", "S49.049P", "S49.049S", "S49.099?", "S49.099A",
				"S49.099D", "S49.099G", "S49.099K", "S49.099P", "S49.099S", "S49.109?", "S49.109A", "S49.109D",
				"S49.109G", "S49.109K", "S49.109P", "S49.109S", "S49.119?", "S49.119A", "S49.119D", "S49.119G",
				"S49.119K", "S49.119P", "S49.119S", "S49.129?", "S49.129A", "S49.129D", "S49.129G", "S49.129K",
				"S49.129P", "S49.129S", "S49.139?", "S49.139A", "S49.139D", "S49.139G", "S49.139K", "S49.139P",
				"S49.139S", "S49.149?", "S49.149A", "S49.149D", "S49.149G", "S49.149K", "S49.149P", "S49.149S",
				"S49.199?", "S49.199A", "S49.199D", "S49.199G", "S49.199K", "S49.199P", "S49.199S", "S49.80X?",
				"S49.80XA", "S49.80XD", "S49.80XS", "S49.90X?", "S49.90XA", "S49.90XD", "S49.90XS", "S50.00X?",
				"S50.00XA", "S50.00XD", "S50.00XS", "S50.10X?", "S50.10XA", "S50.10XD", "S50.10XS", "S50.319?",
				"S50.319A", "S50.319D", "S50.319S", "S50.329?", "S50.329A", "S50.329D", "S50.329S", "S50.349?",
				"S50.349A", "S50.349D", "S50.349S", "S50.359?", "S50.359A", "S50.359D", "S50.359S", "S50.369?",
				"S50.369A", "S50.369D", "S50.369S", "S50.379?", "S50.379A", "S50.379D", "S50.379S", "S50.819?",
				"S50.819A", "S50.819D", "S50.819S", "S50.829?", "S50.829A", "S50.829D", "S50.829S", "S50.849?",
				"S50.849A", "S50.849D", "S50.849S", "S50.859?", "S50.859A", "S50.859D", "S50.859S", "S50.869?",
				"S50.869A", "S50.869D", "S50.869S", "S50.879?", "S50.879A", "S50.879D", "S50.879S", "S50.909?",
				"S50.909A", "S50.909D", "S50.909S", "S50.919?", "S50.919A", "S50.919D", "S50.919S", "S51.009?",
				"S51.009A", "S51.009D", "S51.009S", "S51.019?", "S51.019A", "S51.019D", "S51.019S", "S51.029?",
				"S51.029A", "S51.029D", "S51.029S", "S51.039?", "S51.039A", "S51.039D", "S51.039S", "S51.049?",
				"S51.049A", "S51.049D", "S51.049S", "S51.059?", "S51.059A", "S51.059D", "S51.059S", "S51.809?",
				"S51.809A", "S51.809D", "S51.809S", "S51.819?", "S51.819A", "S51.819D", "S51.819S", "S51.829?",
				"S51.829A", "S51.829D", "S51.829S", "S51.839?", "S51.839A", "S51.839D", "S51.839S", "S51.849?",
				"S51.849A", "S51.849D", "S51.849S", "S51.859?", "S51.859A", "S51.859D", "S51.859S", "S52.009?",
				"S52.009A", "S52.009B", "S52.009C", "S52.009D", "S52.009E", "S52.009F", "S52.009G", "S52.009H",
				"S52.009J", "S52.009K", "S52.009M", "S52.009N", "S52.009P", "S52.009Q", "S52.009R", "S52.009S",
				"S52.019?", "S52.019A", "S52.019D", "S52.019G", "S52.019K", "S52.019P", "S52.019S", "S52.023?",
				"S52.023A", "S52.023B", "S52.023C", "S52.023D", "S52.023E", "S52.023F", "S52.023G", "S52.023H",
				"S52.023J", "S52.023K", "S52.023M", "S52.023N", "S52.023P", "S52.023Q", "S52.023R", "S52.023S",
				"S52.026?", "S52.026A", "S52.026B", "S52.026C", "S52.026D", "S52.026E", "S52.026F", "S52.026G",
				"S52.026H", "S52.026J", "S52.026K", "S52.026M", "S52.026N", "S52.026P", "S52.026Q", "S52.026R",
				"S52.026S", "S52.033?", "S52.033A", "S52.033B", "S52.033C", "S52.033D", "S52.033E", "S52.033F",
				"S52.033G", "S52.033H", "S52.033J", "S52.033K", "S52.033M", "S52.033N", "S52.033P", "S52.033Q",
				"S52.033R", "S52.033S", "S52.036?", "S52.036A", "S52.036B", "S52.036C", "S52.036D", "S52.036E",
				"S52.036F", "S52.036G", "S52.036H", "S52.036J", "S52.036K", "S52.036M", "S52.036N", "S52.036P",
				"S52.036Q", "S52.036R", "S52.036S", "S52.043?", "S52.043A", "S52.043B", "S52.043C", "S52.043D",
				"S52.043E", "S52.043F", "S52.043G", "S52.043H", "S52.043J", "S52.043K", "S52.043M", "S52.043N",
				"S52.043P", "S52.043Q", "S52.043R", "S52.043S", "S52.046?", "S52.046A", "S52.046B", "S52.046C",
				"S52.046D", "S52.046E", "S52.046F", "S52.046G", "S52.046H", "S52.046J", "S52.046K", "S52.046M",
				"S52.046N", "S52.046P", "S52.046Q", "S52.046R", "S52.046S", "S52.099?", "S52.099A", "S52.099B",
				"S52.099C", "S52.099D", "S52.099E", "S52.099F", "S52.099G", "S52.099H", "S52.099J", "S52.099K",
				"S52.099M", "S52.099N", "S52.099P", "S52.099Q", "S52.099R", "S52.099S", "S52.109?", "S52.109A",
				"S52.109B", "S52.109C", "S52.109D", "S52.109E", "S52.109F", "S52.109G", "S52.109H", "S52.109J",
				"S52.109K", "S52.109M", "S52.109N", "S52.109P", "S52.109Q", "S52.109R", "S52.109S", "S52.119?",
				"S52.119A", "S52.119D", "S52.119G", "S52.119K", "S52.119P", "S52.119S", "S52.123?", "S52.123A",
				"S52.123B", "S52.123C", "S52.123D", "S52.123E", "S52.123F", "S52.123G", "S52.123H", "S52.123J",
				"S52.123K", "S52.123M", "S52.123N", "S52.123P", "S52.123Q", "S52.123R", "S52.123S", "S52.126?",
				"S52.126A", "S52.126B", "S52.126C", "S52.126D", "S52.126E", "S52.126F", "S52.126G", "S52.126H",
				"S52.126J", "S52.126K", "S52.126M", "S52.126N", "S52.126P", "S52.126Q", "S52.126R", "S52.126S",
				"S52.133?", "S52.133A", "S52.133B", "S52.133C", "S52.133D", "S52.133E", "S52.133F", "S52.133G",
				"S52.133H", "S52.133J", "S52.133K", "S52.133M", "S52.133N", "S52.133P", "S52.133Q", "S52.133R",
				"S52.133S", "S52.136?", "S52.136A", "S52.136B", "S52.136C", "S52.136D", "S52.136E", "S52.136F",
				"S52.136G", "S52.136H", "S52.136J", "S52.136K", "S52.136M", "S52.136N", "S52.136P", "S52.136Q",
				"S52.136R", "S52.136S", "S52.189?", "S52.189A", "S52.189B", "S52.189C", "S52.189D", "S52.189E",
				"S52.189F", "S52.189G", "S52.189H", "S52.189J", "S52.189K", "S52.189M", "S52.189N", "S52.189P",
				"S52.189Q", "S52.189R", "S52.189S", "S52.209?", "S52.209A", "S52.209B", "S52.209C", "S52.209D",
				"S52.209E", "S52.209F", "S52.209G", "S52.209H", "S52.209J", "S52.209K", "S52.209M", "S52.209N",
				"S52.209P", "S52.209Q", "S52.209R", "S52.209S", "S52.219?", "S52.219A", "S52.219D", "S52.219G",
				"S52.219K", "S52.219P", "S52.219S", "S52.223?", "S52.223A", "S52.223B", "S52.223C", "S52.223D",
				"S52.223E", "S52.223F", "S52.223G", "S52.223H", "S52.223J", "S52.223K", "S52.223M", "S52.223N",
				"S52.223P", "S52.223Q", "S52.223R", "S52.223S", "S52.226?", "S52.226A", "S52.226B", "S52.226C",
				"S52.226D", "S52.226E", "S52.226F", "S52.226G", "S52.226H", "S52.226J", "S52.226K", "S52.226M",
				"S52.226N", "S52.226P", "S52.226Q", "S52.226R", "S52.226S", "S52.233?", "S52.233A", "S52.233B",
				"S52.233C", "S52.233D", "S52.233E", "S52.233F", "S52.233G", "S52.233H", "S52.233J", "S52.233K",
				"S52.233M", "S52.233N", "S52.233P", "S52.233Q", "S52.233R", "S52.233S", "S52.236?", "S52.236A",
				"S52.236B", "S52.236C", "S52.236D", "S52.236E", "S52.236F", "S52.236G", "S52.236H", "S52.236J",
				"S52.236K", "S52.236M", "S52.236N", "S52.236P", "S52.236Q", "S52.236R", "S52.236S", "S52.243?",
				"S52.243A", "S52.243B", "S52.243C", "S52.243D", "S52.243E", "S52.243F", "S52.243G", "S52.243H",
				"S52.243J", "S52.243K", "S52.243M", "S52.243N", "S52.243P", "S52.243Q", "S52.243R", "S52.243S",
				"S52.246?", "S52.246A", "S52.246B", "S52.246C", "S52.246D", "S52.246E", "S52.246F", "S52.246G",
				"S52.246H", "S52.246J", "S52.246K", "S52.246M", "S52.246N", "S52.246P", "S52.246Q", "S52.246R",
				"S52.246S", "S52.253?", "S52.253A", "S52.253B", "S52.253C", "S52.253D", "S52.253E", "S52.253F",
				"S52.253G", "S52.253H", "S52.253J", "S52.253K", "S52.253M", "S52.253N", "S52.253P", "S52.253Q",
				"S52.253R", "S52.253S", "S52.256?", "S52.256A", "S52.256B", "S52.256C", "S52.256D", "S52.256E",
				"S52.256F", "S52.256G", "S52.256H", "S52.256J", "S52.256K", "S52.256M", "S52.256N", "S52.256P",
				"S52.256Q", "S52.256R", "S52.256S", "S52.263?", "S52.263A", "S52.263B", "S52.263C", "S52.263D",
				"S52.263E", "S52.263F", "S52.263G", "S52.263H", "S52.263J", "S52.263K", "S52.263M", "S52.263N",
				"S52.263P", "S52.263Q", "S52.263R", "S52.263S", "S52.266?", "S52.266A", "S52.266B", "S52.266C",
				"S52.266D", "S52.266E", "S52.266F", "S52.266G", "S52.266H", "S52.266J", "S52.266K", "S52.266M",
				"S52.266N", "S52.266P", "S52.266Q", "S52.266R", "S52.266S", "S52.279?", "S52.279A", "S52.279B",
				"S52.279C", "S52.279D", "S52.279E", "S52.279F", "S52.279G", "S52.279H", "S52.279J", "S52.279K",
				"S52.279M", "S52.279N", "S52.279P", "S52.279Q", "S52.279R", "S52.279S", "S52.283?", "S52.283A",
				"S52.283B", "S52.283C", "S52.283D", "S52.283E", "S52.283F", "S52.283G", "S52.283H", "S52.283J",
				"S52.283K", "S52.283M", "S52.283N", "S52.283P", "S52.283Q", "S52.283R", "S52.283S", "S52.299?",
				"S52.299A", "S52.299B", "S52.299C", "S52.299D", "S52.299E", "S52.299F", "S52.299G", "S52.299H",
				"S52.299J", "S52.299K", "S52.299M", "S52.299N", "S52.299P", "S52.299Q", "S52.299R", "S52.299S",
				"S52.309?", "S52.309A", "S52.309B", "S52.309C", "S52.309D", "S52.309E", "S52.309F", "S52.309G",
				"S52.309H", "S52.309J", "S52.309K", "S52.309M", "S52.309N", "S52.309P", "S52.309Q", "S52.309R",
				"S52.309S", "S52.319?", "S52.319A", "S52.319D", "S52.319G", "S52.319K", "S52.319P", "S52.319S",
				"S52.323?", "S52.323A", "S52.323B", "S52.323C", "S52.323D", "S52.323E", "S52.323F", "S52.323G",
				"S52.323H", "S52.323J", "S52.323K", "S52.323M", "S52.323N", "S52.323P", "S52.323Q", "S52.323R",
				"S52.323S", "S52.326?", "S52.326A", "S52.326B", "S52.326C", "S52.326D", "S52.326E", "S52.326F",
				"S52.326G", "S52.326H", "S52.326J", "S52.326K", "S52.326M", "S52.326N", "S52.326P", "S52.326Q",
				"S52.326R", "S52.326S", "S52.333?", "S52.333A", "S52.333B", "S52.333C", "S52.333D", "S52.333E",
				"S52.333F", "S52.333G", "S52.333H", "S52.333J", "S52.333K", "S52.333M", "S52.333N", "S52.333P",
				"S52.333Q", "S52.333R", "S52.333S", "S52.336?", "S52.336A", "S52.336B", "S52.336C", "S52.336D",
				"S52.336E", "S52.336F", "S52.336G", "S52.336H", "S52.336J", "S52.336K", "S52.336M", "S52.336N",
				"S52.336P", "S52.336Q", "S52.336R", "S52.336S", "S52.343?", "S52.343A", "S52.343B", "S52.343C",
				"S52.343D", "S52.343E", "S52.343F", "S52.343G", "S52.343H", "S52.343J", "S52.343K", "S52.343M",
				"S52.343N", "S52.343P", "S52.343Q", "S52.343R", "S52.343S", "S52.346?", "S52.346A", "S52.346B",
				"S52.346C", "S52.346D", "S52.346E", "S52.346F", "S52.346G", "S52.346H", "S52.346J", "S52.346K",
				"S52.346M", "S52.346N", "S52.346P", "S52.346Q", "S52.346R", "S52.346S", "S52.353?", "S52.353A",
				"S52.353B", "S52.353C", "S52.353D", "S52.353E", "S52.353F", "S52.353G", "S52.353H", "S52.353J",
				"S52.353K", "S52.353M", "S52.353N", "S52.353P", "S52.353Q", "S52.353R", "S52.353S", "S52.356?",
				"S52.356A", "S52.356B", "S52.356C", "S52.356D", "S52.356E", "S52.356F", "S52.356G", "S52.356H",
				"S52.356J", "S52.356K", "S52.356M", "S52.356N", "S52.356P", "S52.356Q", "S52.356R", "S52.356S",
				"S52.363?", "S52.363A", "S52.363B", "S52.363C", "S52.363D", "S52.363E", "S52.363F", "S52.363G",
				"S52.363H", "S52.363J", "S52.363K", "S52.363M", "S52.363N", "S52.363P", "S52.363Q", "S52.363R",
				"S52.363S", "S52.366?", "S52.366A", "S52.366B", "S52.366C", "S52.366D", "S52.366E", "S52.366F",
				"S52.366G", "S52.366H", "S52.366J", "S52.366K", "S52.366M", "S52.366N", "S52.366P", "S52.366Q",
				"S52.366R", "S52.366S", "S52.379?", "S52.379A", "S52.379B", "S52.379C", "S52.379D", "S52.379E",
				"S52.379F", "S52.379G", "S52.379H", "S52.379J", "S52.379K", "S52.379M", "S52.379N", "S52.379P",
				"S52.379Q", "S52.379R", "S52.379S", "S52.389?", "S52.389A", "S52.389B", "S52.389C", "S52.389D",
				"S52.389E", "S52.389F", "S52.389G", "S52.389H", "S52.389J", "S52.389K", "S52.389M", "S52.389N",
				"S52.389P", "S52.389Q", "S52.389R", "S52.389S", "S52.399?", "S52.399A", "S52.399B", "S52.399C",
				"S52.399D", "S52.399E", "S52.399F", "S52.399G", "S52.399H", "S52.399J", "S52.399K", "S52.399M",
				"S52.399N", "S52.399P", "S52.399Q", "S52.399R", "S52.399S", "S52.509?", "S52.509A", "S52.509B",
				"S52.509C", "S52.509D", "S52.509E", "S52.509F", "S52.509G", "S52.509H", "S52.509J", "S52.509K",
				"S52.509M", "S52.509N", "S52.509P", "S52.509Q", "S52.509R", "S52.509S", "S52.513?", "S52.513A",
				"S52.513B", "S52.513C", "S52.513D", "S52.513E", "S52.513F", "S52.513G", "S52.513H", "S52.513J",
				"S52.513K", "S52.513M", "S52.513N", "S52.513P", "S52.513Q", "S52.513R", "S52.513S", "S52.516?",
				"S52.516A", "S52.516B", "S52.516C", "S52.516D", "S52.516E", "S52.516F", "S52.516G", "S52.516H",
				"S52.516J", "S52.516K", "S52.516M", "S52.516N", "S52.516P", "S52.516Q", "S52.516R", "S52.516S",
				"S52.529?", "S52.529A", "S52.529D", "S52.529G", "S52.529K", "S52.529P", "S52.529S", "S52.539?",
				"S52.539A", "S52.539B", "S52.539C", "S52.539D", "S52.539E", "S52.539F", "S52.539G", "S52.539H",
				"S52.539J", "S52.539K", "S52.539M", "S52.539N", "S52.539P", "S52.539Q", "S52.539R", "S52.539S",
				"S52.549?", "S52.549A", "S52.549B", "S52.549C", "S52.549D", "S52.549E", "S52.549F", "S52.549G",
				"S52.549H", "S52.549J", "S52.549K", "S52.549M", "S52.549N", "S52.549P", "S52.549Q", "S52.549R",
				"S52.549S", "S52.559?", "S52.559A", "S52.559B", "S52.559C", "S52.559D", "S52.559E", "S52.559F",
				"S52.559G", "S52.559H", "S52.559J", "S52.559K", "S52.559M", "S52.559N", "S52.559P", "S52.559Q",
				"S52.559R", "S52.559S", "S52.569?", "S52.569A", "S52.569B", "S52.569C", "S52.569D", "S52.569E",
				"S52.569F", "S52.569G", "S52.569H", "S52.569J", "S52.569K", "S52.569M", "S52.569N", "S52.569P",
				"S52.569Q", "S52.569R", "S52.569S", "S52.579?", "S52.579A", "S52.579B", "S52.579C", "S52.579D",
				"S52.579E", "S52.579F", "S52.579G", "S52.579H", "S52.579J", "S52.579K", "S52.579M", "S52.579N",
				"S52.579P", "S52.579Q", "S52.579R", "S52.579S", "S52.599?", "S52.599A", "S52.599B", "S52.599C",
				"S52.599D", "S52.599E", "S52.599F", "S52.599G", "S52.599H", "S52.599J", "S52.599K", "S52.599M",
				"S52.599N", "S52.599P", "S52.599Q", "S52.599R", "S52.599S", "S52.609?", "S52.609A", "S52.609B",
				"S52.609C", "S52.609D", "S52.609E", "S52.609F", "S52.609G", "S52.609H", "S52.609J", "S52.609K",
				"S52.609M", "S52.609N", "S52.609P", "S52.609Q", "S52.609R", "S52.609S", "S52.613?", "S52.613A",
				"S52.613B", "S52.613C", "S52.613D", "S52.613E", "S52.613F", "S52.613G", "S52.613H", "S52.613J",
				"S52.613K", "S52.613M", "S52.613N", "S52.613P", "S52.613Q", "S52.613R", "S52.613S", "S52.616?",
				"S52.616A", "S52.616B", "S52.616C", "S52.616D", "S52.616E", "S52.616F", "S52.616G", "S52.616H",
				"S52.616J", "S52.616K", "S52.616M", "S52.616N", "S52.616P", "S52.616Q", "S52.616R", "S52.616S",
				"S52.629?", "S52.629A", "S52.629D", "S52.629G", "S52.629K", "S52.629P", "S52.629S", "S52.699?",
				"S52.699A", "S52.699B", "S52.699C", "S52.699D", "S52.699E", "S52.699F", "S52.699G", "S52.699H",
				"S52.699J", "S52.699K", "S52.699M", "S52.699N", "S52.699P", "S52.699Q", "S52.699R", "S52.699S",
				"S52.90X?", "S52.90XA", "S52.90XB", "S52.90XC", "S52.90XD", "S52.90XE", "S52.90XF", "S52.90XG",
				"S52.90XH", "S52.90XJ", "S52.90XK", "S52.90XM", "S52.90XN", "S52.90XP", "S52.90XQ", "S52.90XR",
				"S52.90XS", "S53.003?", "S53.003A", "S53.003D", "S53.003S", "S53.006?", "S53.006A", "S53.006D",
				"S53.006S", "S53.013?", "S53.013A", "S53.013D", "S53.013S", "S53.016?", "S53.016A", "S53.016D",
				"S53.016S", "S53.023?", "S53.023A", "S53.023D", "S53.023S", "S53.026?", "S53.026A", "S53.026D",
				"S53.026S", "S53.033?", "S53.033A", "S53.033D", "S53.033S", "S53.093?", "S53.093A", "S53.093D",
				"S53.093S", "S53.096?", "S53.096A", "S53.096D", "S53.096S", "S53.103?", "S53.103A", "S53.103D",
				"S53.103S", "S53.106?", "S53.106A", "S53.106D", "S53.106S", "S53.113?", "S53.113A", "S53.113D",
				"S53.113S", "S53.116?", "S53.116A", "S53.116D", "S53.116S", "S53.123?", "S53.123A", "S53.123D",
				"S53.123S", "S53.126?", "S53.126A", "S53.126D", "S53.126S", "S53.133?", "S53.133A", "S53.133D",
				"S53.133S", "S53.136?", "S53.136A", "S53.136D", "S53.136S", "S53.143?", "S53.143A", "S53.143D",
				"S53.143S", "S53.146?", "S53.146A", "S53.146D", "S53.146S", "S53.193?", "S53.193A", "S53.193D",
				"S53.193S", "S53.196?", "S53.196A", "S53.196D", "S53.196S", "S53.20X?", "S53.20XA", "S53.20XD",
				"S53.20XS", "S53.30X?", "S53.30XA", "S53.30XD", "S53.30XS", "S53.409?", "S53.409A", "S53.409D",
				"S53.409S", "S53.419?", "S53.419A", "S53.419D", "S53.419S", "S53.429?", "S53.429A", "S53.429D",
				"S53.429S", "S53.439?", "S53.439A", "S53.439D", "S53.439S", "S53.449?", "S53.449A", "S53.449D",
				"S53.449S", "S53.499?", "S53.499A", "S53.499D", "S53.499S", "S54.00X?", "S54.00XA", "S54.00XD",
				"S54.00XS", "S54.10X?", "S54.10XA", "S54.10XD", "S54.10XS", "S54.20X?", "S54.20XA", "S54.20XD",
				"S54.20XS", "S54.30X?", "S54.30XA", "S54.30XD", "S54.30XS", "S54.8X9?", "S54.8X9A", "S54.8X9D",
				"S54.8X9S", "S54.90X?", "S54.90XA", "S54.90XD", "S54.90XS", "S55.009?", "S55.009A", "S55.009D",
				"S55.009S", "S55.019?", "S55.019A", "S55.019D", "S55.019S", "S55.099?", "S55.099A", "S55.099D",
				"S55.099S", "S55.109?", "S55.109A", "S55.109D", "S55.109S", "S55.119?", "S55.119A", "S55.119D",
				"S55.119S", "S55.199?", "S55.199A", "S55.199D", "S55.199S", "S55.209?", "S55.209A", "S55.209D",
				"S55.209S", "S55.219?", "S55.219A", "S55.219D", "S55.219S", "S55.299?", "S55.299A", "S55.299D",
				"S55.299S", "S55.809?", "S55.809A", "S55.809D", "S55.809S", "S55.819?", "S55.819A", "S55.819D",
				"S55.819S", "S55.899?", "S55.899A", "S55.899D", "S55.899S", "S55.909?", "S55.909A", "S55.909D",
				"S55.909S", "S55.919?", "S55.919A", "S55.919D", "S55.919S", "S55.999?", "S55.999A", "S55.999D",
				"S55.999S", "S56.009?", "S56.009A", "S56.009D", "S56.009S", "S56.019?", "S56.019A", "S56.019D",
				"S56.019S", "S56.029?", "S56.029A", "S56.029D", "S56.029S", "S56.099?", "S56.099A", "S56.099D",
				"S56.099S", "S56.109?", "S56.109A", "S56.109D", "S56.109S", "S56.119?", "S56.119A", "S56.119D",
				"S56.119S", "S56.129?", "S56.129A", "S56.129D", "S56.129S", "S56.199?", "S56.199A", "S56.199D",
				"S56.199S", "S56.209?", "S56.209A", "S56.209D", "S56.209S", "S56.219?", "S56.219A", "S56.219D",
				"S56.219S", "S56.229?", "S56.229A", "S56.229D", "S56.229S", "S56.299?", "S56.299A", "S56.299D",
				"S56.299S", "S56.309?", "S56.309A", "S56.309D", "S56.309S", "S56.319?", "S56.319A", "S56.319D",
				"S56.319S", "S56.329?", "S56.329A", "S56.329D", "S56.329S", "S56.399?", "S56.399A", "S56.399D",
				"S56.399S", "S56.409?", "S56.409A", "S56.409D", "S56.409S", "S56.419?", "S56.419A", "S56.419D",
				"S56.419S", "S56.429?", "S56.429A", "S56.429D", "S56.429S", "S56.499?", "S56.499A", "S56.499D",
				"S56.499S", "S56.509?", "S56.509A", "S56.509D", "S56.509S", "S56.519?", "S56.519A", "S56.519D",
				"S56.519S", "S56.529?", "S56.529A", "S56.529D", "S56.529S", "S56.599?", "S56.599A", "S56.599D",
				"S56.599S", "S56.809?", "S56.809A", "S56.809D", "S56.809S", "S56.819?", "S56.819A", "S56.819D",
				"S56.819S", "S56.829?", "S56.829A", "S56.829D", "S56.829S", "S56.899?", "S56.899A", "S56.899D",
				"S56.899S", "S56.909?", "S56.909A", "S56.909D", "S56.909S", "S56.919?", "S56.919A", "S56.919D",
				"S56.919S", "S56.929?", "S56.929A", "S56.929D", "S56.929S", "S56.999?", "S56.999A", "S56.999D",
				"S56.999S", "S57.00X?", "S57.00XA", "S57.00XD", "S57.00XS", "S57.80X?", "S57.80XA", "S57.80XD",
				"S57.80XS", "S58.019?", "S58.019A", "S58.019D", "S58.019S", "S58.029?", "S58.029A", "S58.029D",
				"S58.029S", "S58.119?", "S58.119A", "S58.119D", "S58.119S", "S58.129?", "S58.129A", "S58.129D",
				"S58.129S", "S58.919?", "S58.919A", "S58.919D", "S58.919S", "S58.929?", "S58.929A", "S58.929D",
				"S58.929S", "S59.009?", "S59.009A", "S59.009D", "S59.009G", "S59.009K", "S59.009P", "S59.009S",
				"S59.019?", "S59.019A", "S59.019D", "S59.019G", "S59.019K", "S59.019P", "S59.019S", "S59.029?",
				"S59.029A", "S59.029D", "S59.029G", "S59.029K", "S59.029P", "S59.029S", "S59.039?", "S59.039A",
				"S59.039D", "S59.039G", "S59.039K", "S59.039P", "S59.039S", "S59.049?", "S59.049A", "S59.049D",
				"S59.049G", "S59.049K", "S59.049P", "S59.049S", "S59.099?", "S59.099A", "S59.099D", "S59.099G",
				"S59.099K", "S59.099P", "S59.099S", "S59.109?", "S59.109A", "S59.109D", "S59.109G", "S59.109K",
				"S59.109P", "S59.109S", "S59.119?", "S59.119A", "S59.119D", "S59.119G", "S59.119K", "S59.119P",
				"S59.119S", "S59.129?", "S59.129A", "S59.129D", "S59.129G", "S59.129K", "S59.129P", "S59.129S",
				"S59.139?", "S59.139A", "S59.139D", "S59.139G", "S59.139K", "S59.139P", "S59.139S", "S59.149?",
				"S59.149A", "S59.149D", "S59.149G", "S59.149K", "S59.149P", "S59.149S", "S59.199?", "S59.199A",
				"S59.199D", "S59.199G", "S59.199K", "S59.199P", "S59.199S", "S59.209?", "S59.209A", "S59.209D",
				"S59.209G", "S59.209K", "S59.209P", "S59.209S", "S59.219?", "S59.219A", "S59.219D", "S59.219G",
				"S59.219K", "S59.219P", "S59.219S", "S59.229?", "S59.229A", "S59.229D", "S59.229G", "S59.229K",
				"S59.229P", "S59.229S", "S59.239?", "S59.239A", "S59.239D", "S59.239G", "S59.239K", "S59.239P",
				"S59.239S", "S59.249?", "S59.249A", "S59.249D", "S59.249G", "S59.249K", "S59.249P", "S59.249S",
				"S59.299?", "S59.299A", "S59.299D", "S59.299G", "S59.299K", "S59.299P", "S59.299S", "S59.809?",
				"S59.809A", "S59.809D", "S59.809S", "S59.819?", "S59.819A", "S59.819D", "S59.819S", "S59.909?",
				"S59.909A", "S59.909D", "S59.909S", "S59.919?", "S59.919A", "S59.919D", "S59.919S", "S60.019?",
				"S60.019A", "S60.019D", "S60.019S", "S60.029?", "S60.029A", "S60.029D", "S60.029S", "S60.039?",
				"S60.039A", "S60.039D", "S60.039S", "S60.049?", "S60.049A", "S60.049D", "S60.049S", "S60.059?",
				"S60.059A", "S60.059D", "S60.059S", "S60.119?", "S60.119A", "S60.119D", "S60.119S", "S60.129?",
				"S60.129A", "S60.129D", "S60.129S", "S60.139?", "S60.139A", "S60.139D", "S60.139S", "S60.149?",
				"S60.149A", "S60.149D", "S60.149S", "S60.159?", "S60.159A", "S60.159D", "S60.159S", "S60.219?",
				"S60.219A", "S60.219D", "S60.219S", "S60.229?", "S60.229A", "S60.229D", "S60.229S", "S60.319?",
				"S60.319A", "S60.319D", "S60.319S", "S60.329?", "S60.329A", "S60.329D", "S60.329S", "S60.349?",
				"S60.349A", "S60.349D", "S60.349S", "S60.359?", "S60.359A", "S60.359D", "S60.359S", "S60.369?",
				"S60.369A", "S60.369D", "S60.369S", "S60.379?", "S60.379A", "S60.379D", "S60.379S", "S60.399?",
				"S60.399A", "S60.399D", "S60.399S", "S60.419?", "S60.419A", "S60.419D", "S60.419S", "S60.429?",
				"S60.429A", "S60.429D", "S60.429S", "S60.449?", "S60.449A", "S60.449D", "S60.449S", "S60.459?",
				"S60.459A", "S60.459D", "S60.459S", "S60.469?", "S60.469A", "S60.469D", "S60.469S", "S60.479?",
				"S60.479A", "S60.479D", "S60.479S", "S60.519?", "S60.519A", "S60.519D", "S60.519S", "S60.529?",
				"S60.529A", "S60.529D", "S60.529S", "S60.549?", "S60.549A", "S60.549D", "S60.549S", "S60.559?",
				"S60.559A", "S60.559D", "S60.559S", "S60.569?", "S60.569A", "S60.569D", "S60.569S", "S60.579?",
				"S60.579A", "S60.579D", "S60.579S", "S60.819?", "S60.819A", "S60.819D", "S60.819S", "S60.829?",
				"S60.829A", "S60.829D", "S60.829S", "S60.849?", "S60.849A", "S60.849D", "S60.849S", "S60.859?",
				"S60.859A", "S60.859D", "S60.859S", "S60.869?", "S60.869A", "S60.869D", "S60.869S", "S60.879?",
				"S60.879A", "S60.879D", "S60.879S", "S60.919?", "S60.919A", "S60.919D", "S60.919S", "S60.929?",
				"S60.929A", "S60.929D", "S60.929S", "S60.939?", "S60.939A", "S60.939D", "S60.939S", "S60.948?",
				"S60.948A", "S60.948D", "S60.948S", "S60.949?", "S60.949A", "S60.949D", "S60.949S", "S61.009?",
				"S61.009A", "S61.009D", "S61.009S", "S61.019?", "S61.019A", "S61.019D", "S61.019S", "S61.029?",
				"S61.029A", "S61.029D", "S61.029S", "S61.039?", "S61.039A", "S61.039D", "S61.039S", "S61.049?",
				"S61.049A", "S61.049D", "S61.049S", "S61.059?", "S61.059A", "S61.059D", "S61.059S", "S61.109?",
				"S61.109A", "S61.109D", "S61.109S", "S61.119?", "S61.119A", "S61.119D", "S61.119S", "S61.129?",
				"S61.129A", "S61.129D", "S61.129S", "S61.139?", "S61.139A", "S61.139D", "S61.139S", "S61.149?",
				"S61.149A", "S61.149D", "S61.149S", "S61.159?", "S61.159A", "S61.159D", "S61.159S", "S61.208?",
				"S61.208A", "S61.208D", "S61.208S", "S61.209?", "S61.209A", "S61.209D", "S61.209S", "S61.219?",
				"S61.219A", "S61.219D", "S61.219S", "S61.229?", "S61.229A", "S61.229D", "S61.229S", "S61.239?",
				"S61.239A", "S61.239D", "S61.239S", "S61.249?", "S61.249A", "S61.249D", "S61.249S", "S61.259?",
				"S61.259A", "S61.259D", "S61.259S", "S61.308?", "S61.308A", "S61.308D", "S61.308S", "S61.309?",
				"S61.309A", "S61.309D", "S61.309S", "S61.319?", "S61.319A", "S61.319D", "S61.319S", "S61.329?",
				"S61.329A", "S61.329D", "S61.329S", "S61.339?", "S61.339A", "S61.339D", "S61.339S", "S61.349?",
				"S61.349A", "S61.349D", "S61.349S", "S61.359?", "S61.359A", "S61.359D", "S61.359S", "S61.409?",
				"S61.409A", "S61.409D", "S61.409S", "S61.419?", "S61.419A", "S61.419D", "S61.419S", "S61.429?",
				"S61.429A", "S61.429D", "S61.429S", "S61.439?", "S61.439A", "S61.439D", "S61.439S", "S61.449?",
				"S61.449A", "S61.449D", "S61.449S", "S61.459?", "S61.459A", "S61.459D", "S61.459S", "S61.509?",
				"S61.509A", "S61.509D", "S61.509S", "S61.519?", "S61.519A", "S61.519D", "S61.519S", "S61.529?",
				"S61.529A", "S61.529D", "S61.529S", "S61.539?", "S61.539A", "S61.539D", "S61.539S", "S61.549?",
				"S61.549A", "S61.549D", "S61.549S", "S61.559?", "S61.559A", "S61.559D", "S61.559S" }));
	}

	/**
	 * Cache second half of the lateratlity codes. TODO: this should be read in
	 * from a file
	 *
	 * @throws Exception
	 *             the exception
	 */
	private void cacheLateralityCodes2() throws Exception {
		lateralityCodes.addAll(Arrays.asList(new String[] { "S62.009?", "S62.009A", "S62.009B", "S62.009D", "S62.009G",
				"S62.009K", "S62.009P", "S62.009S", "S62.013?", "S62.013A", "S62.013B", "S62.013D", "S62.013G",
				"S62.013K", "S62.013P", "S62.013S", "S62.016?", "S62.016A", "S62.016B", "S62.016D", "S62.016G",
				"S62.016K", "S62.016P", "S62.016S", "S62.023?", "S62.023A", "S62.023B", "S62.023D", "S62.023G",
				"S62.023K", "S62.023P", "S62.023S", "S62.026?", "S62.026A", "S62.026B", "S62.026D", "S62.026G",
				"S62.026K", "S62.026P", "S62.026S", "S62.033?", "S62.033A", "S62.033B", "S62.033D", "S62.033G",
				"S62.033K", "S62.033P", "S62.033S", "S62.036?", "S62.036A", "S62.036B", "S62.036D", "S62.036G",
				"S62.036K", "S62.036P", "S62.036S", "S62.109?", "S62.109A", "S62.109B", "S62.109D", "S62.109G",
				"S62.109K", "S62.109P", "S62.109S", "S62.113?", "S62.113A", "S62.113B", "S62.113D", "S62.113G",
				"S62.113K", "S62.113P", "S62.113S", "S62.116?", "S62.116A", "S62.116B", "S62.116D", "S62.116G",
				"S62.116K", "S62.116P", "S62.116S", "S62.123?", "S62.123A", "S62.123B", "S62.123D", "S62.123G",
				"S62.123K", "S62.123P", "S62.123S", "S62.126?", "S62.126A", "S62.126B", "S62.126D", "S62.126G",
				"S62.126K", "S62.126P", "S62.126S", "S62.133?", "S62.133A", "S62.133B", "S62.133D", "S62.133G",
				"S62.133K", "S62.133P", "S62.133S", "S62.136?", "S62.136A", "S62.136B", "S62.136D", "S62.136G",
				"S62.136K", "S62.136P", "S62.136S", "S62.143?", "S62.143A", "S62.143B", "S62.143D", "S62.143G",
				"S62.143K", "S62.143P", "S62.143S", "S62.146?", "S62.146A", "S62.146B", "S62.146D", "S62.146G",
				"S62.146K", "S62.146P", "S62.146S", "S62.153?", "S62.153A", "S62.153B", "S62.153D", "S62.153G",
				"S62.153K", "S62.153P", "S62.153S", "S62.156?", "S62.156A", "S62.156B", "S62.156D", "S62.156G",
				"S62.156K", "S62.156P", "S62.156S", "S62.163?", "S62.163A", "S62.163B", "S62.163D", "S62.163G",
				"S62.163K", "S62.163P", "S62.163S", "S62.166?", "S62.166A", "S62.166B", "S62.166D", "S62.166G",
				"S62.166K", "S62.166P", "S62.166S", "S62.173?", "S62.173A", "S62.173B", "S62.173D", "S62.173G",
				"S62.173K", "S62.173P", "S62.173S", "S62.176?", "S62.176A", "S62.176B", "S62.176D", "S62.176G",
				"S62.176K", "S62.176P", "S62.176S", "S62.183?", "S62.183A", "S62.183B", "S62.183D", "S62.183G",
				"S62.183K", "S62.183P", "S62.183S", "S62.186?", "S62.186A", "S62.186B", "S62.186D", "S62.186G",
				"S62.186K", "S62.186P", "S62.186S", "S62.209?", "S62.209A", "S62.209B", "S62.209D", "S62.209G",
				"S62.209K", "S62.209P", "S62.209S", "S62.213?", "S62.213A", "S62.213B", "S62.213D", "S62.213G",
				"S62.213K", "S62.213P", "S62.213S", "S62.223?", "S62.223A", "S62.223B", "S62.223D", "S62.223G",
				"S62.223K", "S62.223P", "S62.223S", "S62.226?", "S62.226A", "S62.226B", "S62.226D", "S62.226G",
				"S62.226K", "S62.226P", "S62.226S", "S62.233?", "S62.233A", "S62.233B", "S62.233D", "S62.233G",
				"S62.233K", "S62.233P", "S62.233S", "S62.236?", "S62.236A", "S62.236B", "S62.236D", "S62.236G",
				"S62.236K", "S62.236P", "S62.236S", "S62.243?", "S62.243A", "S62.243B", "S62.243D", "S62.243G",
				"S62.243K", "S62.243P", "S62.243S", "S62.246?", "S62.246A", "S62.246B", "S62.246D", "S62.246G",
				"S62.246K", "S62.246P", "S62.246S", "S62.253?", "S62.253A", "S62.253B", "S62.253D", "S62.253G",
				"S62.253K", "S62.253P", "S62.253S", "S62.256?", "S62.256A", "S62.256B", "S62.256D", "S62.256G",
				"S62.256K", "S62.256P", "S62.256S", "S62.299?", "S62.299A", "S62.299B", "S62.299D", "S62.299G",
				"S62.299K", "S62.299P", "S62.299S", "S62.308?", "S62.308A", "S62.308B", "S62.308D", "S62.308G",
				"S62.308K", "S62.308P", "S62.308S", "S62.309?", "S62.309A", "S62.309B", "S62.309D", "S62.309G",
				"S62.309K", "S62.309P", "S62.309S", "S62.319?", "S62.319A", "S62.319B", "S62.319D", "S62.319G",
				"S62.319K", "S62.319P", "S62.319S", "S62.329?", "S62.329A", "S62.329B", "S62.329D", "S62.329G",
				"S62.329K", "S62.329P", "S62.329S", "S62.339?", "S62.339A", "S62.339B", "S62.339D", "S62.339G",
				"S62.339K", "S62.339P", "S62.339S", "S62.349?", "S62.349A", "S62.349B", "S62.349D", "S62.349G",
				"S62.349K", "S62.349P", "S62.349S", "S62.359?", "S62.359A", "S62.359B", "S62.359D", "S62.359G",
				"S62.359K", "S62.359P", "S62.359S", "S62.369?", "S62.369A", "S62.369B", "S62.369D", "S62.369G",
				"S62.369K", "S62.369P", "S62.369S", "S62.399?", "S62.399A", "S62.399B", "S62.399D", "S62.399G",
				"S62.399K", "S62.399P", "S62.399S", "S62.509?", "S62.509A", "S62.509B", "S62.509D", "S62.509G",
				"S62.509K", "S62.509P", "S62.509S", "S62.513?", "S62.513A", "S62.513B", "S62.513D", "S62.513G",
				"S62.513K", "S62.513P", "S62.513S", "S62.516?", "S62.516A", "S62.516B", "S62.516D", "S62.516G",
				"S62.516K", "S62.516P", "S62.516S", "S62.523?", "S62.523A", "S62.523B", "S62.523D", "S62.523G",
				"S62.523K", "S62.523P", "S62.523S", "S62.526?", "S62.526A", "S62.526B", "S62.526D", "S62.526G",
				"S62.526K", "S62.526P", "S62.526S", "S62.608?", "S62.608A", "S62.608B", "S62.608D", "S62.608G",
				"S62.608K", "S62.608P", "S62.608S", "S62.609?", "S62.609A", "S62.609B", "S62.609D", "S62.609G",
				"S62.609K", "S62.609P", "S62.609S", "S62.619?", "S62.619A", "S62.619B", "S62.619D", "S62.619G",
				"S62.619K", "S62.619P", "S62.619S", "S62.629?", "S62.629A", "S62.629B", "S62.629D", "S62.629G",
				"S62.629K", "S62.629P", "S62.629S", "S62.639?", "S62.639A", "S62.639B", "S62.639D", "S62.639G",
				"S62.639K", "S62.639P", "S62.639S", "S62.649?", "S62.649A", "S62.649B", "S62.649D", "S62.649G",
				"S62.649K", "S62.649P", "S62.649S", "S62.659?", "S62.659A", "S62.659B", "S62.659D", "S62.659G",
				"S62.659K", "S62.659P", "S62.659S", "S62.669?", "S62.669A", "S62.669B", "S62.669D", "S62.669G",
				"S62.669K", "S62.669P", "S62.669S", "S62.90X?", "S62.90XA", "S62.90XB", "S62.90XD", "S62.90XG",
				"S62.90XK", "S62.90XP", "S62.90XS", "S63.003?", "S63.003A", "S63.003D", "S63.003S", "S63.006?",
				"S63.006A", "S63.006D", "S63.006S", "S63.013?", "S63.013A", "S63.013D", "S63.013S", "S63.016?",
				"S63.016A", "S63.016D", "S63.016S", "S63.023?", "S63.023A", "S63.023D", "S63.023S", "S63.026?",
				"S63.026A", "S63.026D", "S63.026S", "S63.033?", "S63.033A", "S63.033D", "S63.033S", "S63.036?",
				"S63.036A", "S63.036D", "S63.036S", "S63.043?", "S63.043A", "S63.043D", "S63.043S", "S63.046?",
				"S63.046A", "S63.046D", "S63.046S", "S63.053?", "S63.053A", "S63.053D", "S63.053S", "S63.056?",
				"S63.056A", "S63.056D", "S63.056S", "S63.063?", "S63.063A", "S63.063D", "S63.063S", "S63.066?",
				"S63.066A", "S63.066D", "S63.066S", "S63.073?", "S63.073A", "S63.073D", "S63.073S", "S63.076?",
				"S63.076A", "S63.076D", "S63.076S", "S63.093?", "S63.093A", "S63.093D", "S63.093S", "S63.096?",
				"S63.096A", "S63.096D", "S63.096S", "S63.103?", "S63.103A", "S63.103D", "S63.103S", "S63.106?",
				"S63.106A", "S63.106D", "S63.106S", "S63.113?", "S63.113A", "S63.113D", "S63.113S", "S63.116?",
				"S63.116A", "S63.116D", "S63.116S", "S63.123?", "S63.123A", "S63.123D", "S63.123S", "S63.126?",
				"S63.126A", "S63.126D", "S63.126S", "S63.133?", "S63.133A", "S63.133D", "S63.133S", "S63.136?",
				"S63.136A", "S63.136D", "S63.136S", "S63.143?", "S63.143A", "S63.143D", "S63.143S", "S63.146?",
				"S63.146A", "S63.146D", "S63.146S", "S63.208?", "S63.208A", "S63.208D", "S63.208S", "S63.209?",
				"S63.209A", "S63.209D", "S63.209S", "S63.219?", "S63.219A", "S63.219D", "S63.219S", "S63.228?",
				"S63.228A", "S63.228D", "S63.228S", "S63.229?", "S63.229A", "S63.229D", "S63.229S", "S63.239?",
				"S63.239A", "S63.239D", "S63.239S", "S63.249?", "S63.249A", "S63.249D", "S63.249S", "S63.258?",
				"S63.258A", "S63.258D", "S63.258S", "S63.259?", "S63.259A", "S63.259D", "S63.259S", "S63.269?",
				"S63.269A", "S63.269D", "S63.269S", "S63.278?", "S63.278A", "S63.278D", "S63.278S", "S63.279?",
				"S63.279A", "S63.279D", "S63.279S", "S63.289?", "S63.289A", "S63.289D", "S63.289S", "S63.299?",
				"S63.299A", "S63.299D", "S63.299S", "S63.309?", "S63.309A", "S63.309D", "S63.309S", "S63.319?",
				"S63.319A", "S63.319D", "S63.319S", "S63.329?", "S63.329A", "S63.329D", "S63.329S", "S63.339?",
				"S63.339A", "S63.339D", "S63.339S", "S63.399?", "S63.399A", "S63.399D", "S63.399S", "S63.408?",
				"S63.408A", "S63.408D", "S63.408S", "S63.409?", "S63.409A", "S63.409D", "S63.409S", "S63.419?",
				"S63.419A", "S63.419D", "S63.419S", "S63.429?", "S63.429A", "S63.429D", "S63.429S", "S63.439?",
				"S63.439A", "S63.439D", "S63.439S", "S63.499?", "S63.499A", "S63.499D", "S63.499S", "S63.509?",
				"S63.509A", "S63.509D", "S63.509S", "S63.519?", "S63.519A", "S63.519D", "S63.519S", "S63.529?",
				"S63.529A", "S63.529D", "S63.529S", "S63.599?", "S63.599A", "S63.599D", "S63.599S", "S63.609?",
				"S63.609A", "S63.609D", "S63.609S", "S63.618?", "S63.618A", "S63.618D", "S63.618S", "S63.619?",
				"S63.619A", "S63.619D", "S63.619S", "S63.629?", "S63.629A", "S63.629D", "S63.629S", "S63.639?",
				"S63.639A", "S63.639D", "S63.639S", "S63.649?", "S63.649A", "S63.649D", "S63.649S", "S63.659?",
				"S63.659A", "S63.659D", "S63.659S", "S63.689?", "S63.689A", "S63.689D", "S63.689S", "S63.699?",
				"S63.699A", "S63.699D", "S63.699S", "S63.8X9?", "S63.8X9A", "S63.8X9D", "S63.8X9S", "S63.90X?",
				"S63.90XA", "S63.90XD", "S63.90XS", "S64.00X?", "S64.00XA", "S64.00XD", "S64.00XS", "S64.10X?",
				"S64.10XA", "S64.10XD", "S64.10XS", "S64.20X?", "S64.20XA", "S64.20XD", "S64.20XS", "S64.30X?",
				"S64.30XA", "S64.30XD", "S64.30XS", "S64.8X9?", "S64.8X9A", "S64.8X9D", "S64.8X9S", "S64.90X?",
				"S64.90XA", "S64.90XD", "S64.90XS", "S65.009?", "S65.009A", "S65.009D", "S65.009S", "S65.019?",
				"S65.019A", "S65.019D", "S65.019S", "S65.099?", "S65.099A", "S65.099D", "S65.099S", "S65.109?",
				"S65.109A", "S65.109D", "S65.109S", "S65.119?", "S65.119A", "S65.119D", "S65.119S", "S65.199?",
				"S65.199A", "S65.199D", "S65.199S", "S65.209?", "S65.209A", "S65.209D", "S65.209S", "S65.219?",
				"S65.219A", "S65.219D", "S65.219S", "S65.299?", "S65.299A", "S65.299D", "S65.299S", "S65.309?",
				"S65.309A", "S65.309D", "S65.309S", "S65.319?", "S65.319A", "S65.319D", "S65.319S", "S65.399?",
				"S65.399A", "S65.399D", "S65.399S", "S65.409?", "S65.409A", "S65.409D", "S65.409S", "S65.419?",
				"S65.419A", "S65.419D", "S65.419S", "S65.499?", "S65.499A", "S65.499D", "S65.499S", "S65.508?",
				"S65.508A", "S65.508D", "S65.508S", "S65.509?", "S65.509A", "S65.509D", "S65.509S", "S65.519?",
				"S65.519A", "S65.519D", "S65.519S", "S65.599?", "S65.599A", "S65.599D", "S65.599S", "S65.809?",
				"S65.809A", "S65.809D", "S65.809S", "S65.819?", "S65.819A", "S65.819D", "S65.819S", "S65.899?",
				"S65.899A", "S65.899D", "S65.899S", "S65.909?", "S65.909A", "S65.909D", "S65.909S", "S65.919?",
				"S65.919A", "S65.919D", "S65.919S", "S65.999?", "S65.999A", "S65.999D", "S65.999S", "S66.009?",
				"S66.009A", "S66.009D", "S66.009S", "S66.019?", "S66.019A", "S66.019D", "S66.019S", "S66.029?",
				"S66.029A", "S66.029D", "S66.029S", "S66.099?", "S66.099A", "S66.099D", "S66.099S", "S66.108?",
				"S66.108A", "S66.108D", "S66.108S", "S66.109?", "S66.109A", "S66.109D", "S66.109S", "S66.119?",
				"S66.119A", "S66.119D", "S66.119S", "S66.129?", "S66.129A", "S66.129D", "S66.129S", "S66.199?",
				"S66.199A", "S66.199D", "S66.199S", "S66.209?", "S66.209A", "S66.209D", "S66.209S", "S66.219?",
				"S66.219A", "S66.219D", "S66.219S", "S66.229?", "S66.229A", "S66.229D", "S66.229S", "S66.299?",
				"S66.299A", "S66.299D", "S66.299S", "S66.308?", "S66.308A", "S66.308D", "S66.308S", "S66.309?",
				"S66.309A", "S66.309D", "S66.309S", "S66.319?", "S66.319A", "S66.319D", "S66.319S", "S66.329?",
				"S66.329A", "S66.329D", "S66.329S", "S66.399?", "S66.399A", "S66.399D", "S66.399S", "S66.409?",
				"S66.409A", "S66.409D", "S66.409S", "S66.419?", "S66.419A", "S66.419D", "S66.419S", "S66.429?",
				"S66.429A", "S66.429D", "S66.429S", "S66.499?", "S66.499A", "S66.499D", "S66.499S", "S66.508?",
				"S66.508A", "S66.508D", "S66.508S", "S66.509?", "S66.509A", "S66.509D", "S66.509S", "S66.519?",
				"S66.519A", "S66.519D", "S66.519S", "S66.529?", "S66.529A", "S66.529D", "S66.529S", "S66.599?",
				"S66.599A", "S66.599D", "S66.599S", "S66.809?", "S66.809A", "S66.809D", "S66.809S", "S66.819?",
				"S66.819A", "S66.819D", "S66.819S", "S66.829?", "S66.829A", "S66.829D", "S66.829S", "S66.899?",
				"S66.899A", "S66.899D", "S66.899S", "S66.909?", "S66.909A", "S66.909D", "S66.909S", "S66.919?",
				"S66.919A", "S66.919D", "S66.919S", "S66.929?", "S66.929A", "S66.929D", "S66.929S", "S66.999?",
				"S66.999A", "S66.999D", "S66.999S", "S67.00X?", "S67.00XA", "S67.00XD", "S67.00XS", "S67.20X?",
				"S67.20XA", "S67.20XD", "S67.20XS", "S67.30X?", "S67.30XA", "S67.30XD", "S67.30XS", "S67.40X?",
				"S67.40XA", "S67.40XD", "S67.40XS", "S67.90X?", "S67.90XA", "S67.90XD", "S67.90XS", "S68.019?",
				"S68.019A", "S68.019D", "S68.019S", "S68.029?", "S68.029A", "S68.029D", "S68.029S", "S68.119?",
				"S68.119A", "S68.119D", "S68.119S", "S68.129?", "S68.129A", "S68.129D", "S68.129S", "S68.419?",
				"S68.419A", "S68.419D", "S68.419S", "S68.429?", "S68.429A", "S68.429D", "S68.429S", "S68.519?",
				"S68.519A", "S68.519D", "S68.519S", "S68.529?", "S68.529A", "S68.529D", "S68.529S", "S68.619?",
				"S68.619A", "S68.619D", "S68.619S", "S68.629?", "S68.629A", "S68.629D", "S68.629S", "S68.719?",
				"S68.719A", "S68.719D", "S68.719S", "S68.729?", "S68.729A", "S68.729D", "S68.729S", "S69.80X?",
				"S69.80XA", "S69.80XD", "S69.80XS", "S69.90X?", "S69.90XA", "S69.90XD", "S69.90XS", "S70.00X?",
				"S70.00XA", "S70.00XD", "S70.00XS", "S70.10X?", "S70.10XA", "S70.10XD", "S70.10XS", "S70.219?",
				"S70.219A", "S70.219D", "S70.219S", "S70.229?", "S70.229A", "S70.229D", "S70.229S", "S70.249?",
				"S70.249A", "S70.249D", "S70.249S", "S70.259?", "S70.259A", "S70.259D", "S70.259S", "S70.269?",
				"S70.269A", "S70.269D", "S70.269S", "S70.279?", "S70.279A", "S70.279D", "S70.279S", "S70.319?",
				"S70.319A", "S70.319D", "S70.319S", "S70.329?", "S70.329A", "S70.329D", "S70.329S", "S70.349?",
				"S70.349A", "S70.349D", "S70.349S", "S70.359?", "S70.359A", "S70.359D", "S70.359S", "S70.369?",
				"S70.369A", "S70.369D", "S70.369S", "S70.379?", "S70.379A", "S70.379D", "S70.379S", "S70.919?",
				"S70.919A", "S70.919D", "S70.919S", "S70.929?", "S70.929A", "S70.929D", "S70.929S", "S71.009?",
				"S71.009A", "S71.009D", "S71.009S", "S71.019?", "S71.019A", "S71.019D", "S71.019S", "S71.029?",
				"S71.029A", "S71.029D", "S71.029S", "S71.039?", "S71.039A", "S71.039D", "S71.039S", "S71.049?",
				"S71.049A", "S71.049D", "S71.049S", "S71.059?", "S71.059A", "S71.059D", "S71.059S", "S71.109?",
				"S71.109A", "S71.109D", "S71.109S", "S71.119?", "S71.119A", "S71.119D", "S71.119S", "S71.129?",
				"S71.129A", "S71.129D", "S71.129S", "S71.139?", "S71.139A", "S71.139D", "S71.139S", "S71.149?",
				"S71.149A", "S71.149D", "S71.149S", "S71.159?", "S71.159A", "S71.159D", "S71.159S", "S72.009?",
				"S72.009A", "S72.009B", "S72.009C", "S72.009D", "S72.009E", "S72.009F", "S72.009G", "S72.009H",
				"S72.009J", "S72.009K", "S72.009M", "S72.009N", "S72.009P", "S72.009Q", "S72.009R", "S72.009S",
				"S72.019?", "S72.019A", "S72.019B", "S72.019C", "S72.019D", "S72.019E", "S72.019F", "S72.019G",
				"S72.019H", "S72.019J", "S72.019K", "S72.019M", "S72.019N", "S72.019P", "S72.019Q", "S72.019R",
				"S72.019S", "S72.023?", "S72.023A", "S72.023B", "S72.023C", "S72.023D", "S72.023E", "S72.023F",
				"S72.023G", "S72.023H", "S72.023J", "S72.023K", "S72.023M", "S72.023N", "S72.023P", "S72.023Q",
				"S72.023R", "S72.023S", "S72.026?", "S72.026A", "S72.026B", "S72.026C", "S72.026D", "S72.026E",
				"S72.026F", "S72.026G", "S72.026H", "S72.026J", "S72.026K", "S72.026M", "S72.026N", "S72.026P",
				"S72.026Q", "S72.026R", "S72.026S", "S72.033?", "S72.033A", "S72.033B", "S72.033C", "S72.033D",
				"S72.033E", "S72.033F", "S72.033G", "S72.033H", "S72.033J", "S72.033K", "S72.033M", "S72.033N",
				"S72.033P", "S72.033Q", "S72.033R", "S72.033S", "S72.036?", "S72.036A", "S72.036B", "S72.036C",
				"S72.036D", "S72.036E", "S72.036F", "S72.036G", "S72.036H", "S72.036J", "S72.036K", "S72.036M",
				"S72.036N", "S72.036P", "S72.036Q", "S72.036R", "S72.036S", "S72.043?", "S72.043A", "S72.043B",
				"S72.043C", "S72.043D", "S72.043E", "S72.043F", "S72.043G", "S72.043H", "S72.043J", "S72.043K",
				"S72.043M", "S72.043N", "S72.043P", "S72.043Q", "S72.043R", "S72.043S", "S72.046?", "S72.046A",
				"S72.046B", "S72.046C", "S72.046D", "S72.046E", "S72.046F", "S72.046G", "S72.046H", "S72.046J",
				"S72.046K", "S72.046M", "S72.046N", "S72.046P", "S72.046Q", "S72.046R", "S72.046S", "S72.059?",
				"S72.059A", "S72.059B", "S72.059C", "S72.059D", "S72.059E", "S72.059F", "S72.059G", "S72.059H",
				"S72.059J", "S72.059K", "S72.059M", "S72.059N", "S72.059P", "S72.059Q", "S72.059R", "S72.059S",
				"S72.063?", "S72.063A", "S72.063B", "S72.063C", "S72.063D", "S72.063E", "S72.063F", "S72.063G",
				"S72.063H", "S72.063J", "S72.063K", "S72.063M", "S72.063N", "S72.063P", "S72.063Q", "S72.063R",
				"S72.063S", "S72.066?", "S72.066A", "S72.066B", "S72.066C", "S72.066D", "S72.066E", "S72.066F",
				"S72.066G", "S72.066H", "S72.066J", "S72.066K", "S72.066M", "S72.066N", "S72.066P", "S72.066Q",
				"S72.066R", "S72.066S", "S72.099?", "S72.099A", "S72.099B", "S72.099C", "S72.099D", "S72.099E",
				"S72.099F", "S72.099G", "S72.099H", "S72.099J", "S72.099K", "S72.099M", "S72.099N", "S72.099P",
				"S72.099Q", "S72.099R", "S72.099S", "S72.109?", "S72.109A", "S72.109B", "S72.109C", "S72.109D",
				"S72.109E", "S72.109F", "S72.109G", "S72.109H", "S72.109J", "S72.109K", "S72.109M", "S72.109N",
				"S72.109P", "S72.109Q", "S72.109R", "S72.109S", "S72.113?", "S72.113A", "S72.113B", "S72.113C",
				"S72.113D", "S72.113E", "S72.113F", "S72.113G", "S72.113H", "S72.113J", "S72.113K", "S72.113M",
				"S72.113N", "S72.113P", "S72.113Q", "S72.113R", "S72.113S", "S72.116?", "S72.116A", "S72.116B",
				"S72.116C", "S72.116D", "S72.116E", "S72.116F", "S72.116G", "S72.116H", "S72.116J", "S72.116K",
				"S72.116M", "S72.116N", "S72.116P", "S72.116Q", "S72.116R", "S72.116S", "S72.123?", "S72.123A",
				"S72.123B", "S72.123C", "S72.123D", "S72.123E", "S72.123F", "S72.123G", "S72.123H", "S72.123J",
				"S72.123K", "S72.123M", "S72.123N", "S72.123P", "S72.123Q", "S72.123R", "S72.123S", "S72.126?",
				"S72.126A", "S72.126B", "S72.126C", "S72.126D", "S72.126E", "S72.126F", "S72.126G", "S72.126H",
				"S72.126J", "S72.126K", "S72.126M", "S72.126N", "S72.126P", "S72.126Q", "S72.126R", "S72.126S",
				"S72.133?", "S72.133A", "S72.133B", "S72.133C", "S72.133D", "S72.133E", "S72.133F", "S72.133G",
				"S72.133H", "S72.133J", "S72.133K", "S72.133M", "S72.133N", "S72.133P", "S72.133Q", "S72.133R",
				"S72.133S", "S72.136?", "S72.136A", "S72.136B", "S72.136C", "S72.136D", "S72.136E", "S72.136F",
				"S72.136G", "S72.136H", "S72.136J", "S72.136K", "S72.136M", "S72.136N", "S72.136P", "S72.136Q",
				"S72.136R", "S72.136S", "S72.143?", "S72.143A", "S72.143B", "S72.143C", "S72.143D", "S72.143E",
				"S72.143F", "S72.143G", "S72.143H", "S72.143J", "S72.143K", "S72.143M", "S72.143N", "S72.143P",
				"S72.143Q", "S72.143R", "S72.143S", "S72.146?", "S72.146A", "S72.146B", "S72.146C", "S72.146D",
				"S72.146E", "S72.146F", "S72.146G", "S72.146H", "S72.146J", "S72.146K", "S72.146M", "S72.146N",
				"S72.146P", "S72.146Q", "S72.146R", "S72.146S", "S72.23X?", "S72.23XA", "S72.23XB", "S72.23XC",
				"S72.23XD", "S72.23XE", "S72.23XF", "S72.23XG", "S72.23XH", "S72.23XJ", "S72.23XK", "S72.23XM",
				"S72.23XN", "S72.23XP", "S72.23XQ", "S72.23XR", "S72.23XS", "S72.26X?", "S72.26XA", "S72.26XB",
				"S72.26XC", "S72.26XD", "S72.26XE", "S72.26XF", "S72.26XG", "S72.26XH", "S72.26XJ", "S72.26XK",
				"S72.26XM", "S72.26XN", "S72.26XP", "S72.26XQ", "S72.26XR", "S72.26XS", "S72.309?", "S72.309A",
				"S72.309B", "S72.309C", "S72.309D", "S72.309E", "S72.309F", "S72.309G", "S72.309H", "S72.309J",
				"S72.309K", "S72.309M", "S72.309N", "S72.309P", "S72.309Q", "S72.309R", "S72.309S", "S72.323?",
				"S72.323A", "S72.323B", "S72.323C", "S72.323D", "S72.323E", "S72.323F", "S72.323G", "S72.323H",
				"S72.323J", "S72.323K", "S72.323M", "S72.323N", "S72.323P", "S72.323Q", "S72.323R", "S72.323S",
				"S72.326?", "S72.326A", "S72.326B", "S72.326C", "S72.326D", "S72.326E", "S72.326F", "S72.326G",
				"S72.326H", "S72.326J", "S72.326K", "S72.326M", "S72.326N", "S72.326P", "S72.326Q", "S72.326R",
				"S72.326S", "S72.333?", "S72.333A", "S72.333B", "S72.333C", "S72.333D", "S72.333E", "S72.333F",
				"S72.333G", "S72.333H", "S72.333J", "S72.333K", "S72.333M", "S72.333N", "S72.333P", "S72.333Q",
				"S72.333R", "S72.333S", "S72.336?", "S72.336A", "S72.336B", "S72.336C", "S72.336D", "S72.336E",
				"S72.336F", "S72.336G", "S72.336H", "S72.336J", "S72.336K", "S72.336M", "S72.336N", "S72.336P",
				"S72.336Q", "S72.336R", "S72.336S", "S72.343?", "S72.343A", "S72.343B", "S72.343C", "S72.343D",
				"S72.343E", "S72.343F", "S72.343G", "S72.343H", "S72.343J", "S72.343K", "S72.343M", "S72.343N",
				"S72.343P", "S72.343Q", "S72.343R", "S72.343S", "S72.346?", "S72.346A", "S72.346B", "S72.346C",
				"S72.346D", "S72.346E", "S72.346F", "S72.346G", "S72.346H", "S72.346J", "S72.346K", "S72.346M",
				"S72.346N", "S72.346P", "S72.346Q", "S72.346R", "S72.346S", "S72.353?", "S72.353A", "S72.353B",
				"S72.353C", "S72.353D", "S72.353E", "S72.353F", "S72.353G", "S72.353H", "S72.353J", "S72.353K",
				"S72.353M", "S72.353N", "S72.353P", "S72.353Q", "S72.353R", "S72.353S", "S72.356?", "S72.356A",
				"S72.356B", "S72.356C", "S72.356D", "S72.356E", "S72.356F", "S72.356G", "S72.356H", "S72.356J",
				"S72.356K", "S72.356M", "S72.356N", "S72.356P", "S72.356Q", "S72.356R", "S72.356S", "S72.363?",
				"S72.363A", "S72.363B", "S72.363C", "S72.363D", "S72.363E", "S72.363F", "S72.363G", "S72.363H",
				"S72.363J", "S72.363K", "S72.363M", "S72.363N", "S72.363P", "S72.363Q", "S72.363R", "S72.363S",
				"S72.366?", "S72.366A", "S72.366B", "S72.366C", "S72.366D", "S72.366E", "S72.366F", "S72.366G",
				"S72.366H", "S72.366J", "S72.366K", "S72.366M", "S72.366N", "S72.366P", "S72.366Q", "S72.366R",
				"S72.366S", "S72.399?", "S72.399A", "S72.399B", "S72.399C", "S72.399D", "S72.399E", "S72.399F",
				"S72.399G", "S72.399H", "S72.399J", "S72.399K", "S72.399M", "S72.399N", "S72.399P", "S72.399Q",
				"S72.399R", "S72.399S", "S72.409?", "S72.409A", "S72.409B", "S72.409C", "S72.409D", "S72.409E",
				"S72.409F", "S72.409G", "S72.409H", "S72.409J", "S72.409K", "S72.409M", "S72.409N", "S72.409P",
				"S72.409Q", "S72.409R", "S72.409S", "S72.413?", "S72.413A", "S72.413B", "S72.413C", "S72.413D",
				"S72.413E", "S72.413F", "S72.413G", "S72.413H", "S72.413J", "S72.413K", "S72.413M", "S72.413N",
				"S72.413P", "S72.413Q", "S72.413R", "S72.413S", "S72.416?", "S72.416A", "S72.416B", "S72.416C",
				"S72.416D", "S72.416E", "S72.416F", "S72.416G", "S72.416H", "S72.416J", "S72.416K", "S72.416M",
				"S72.416N", "S72.416P", "S72.416Q", "S72.416R", "S72.416S", "S72.423?", "S72.423A", "S72.423B",
				"S72.423C", "S72.423D", "S72.423E", "S72.423F", "S72.423G", "S72.423H", "S72.423J", "S72.423K",
				"S72.423M", "S72.423N", "S72.423P", "S72.423Q", "S72.423R", "S72.423S", "S72.426?", "S72.426A",
				"S72.426B", "S72.426C", "S72.426D", "S72.426E", "S72.426F", "S72.426G", "S72.426H", "S72.426J",
				"S72.426K", "S72.426M", "S72.426N", "S72.426P", "S72.426Q", "S72.426R", "S72.426S", "S72.433?",
				"S72.433A", "S72.433B", "S72.433C", "S72.433D", "S72.433E", "S72.433F", "S72.433G", "S72.433H",
				"S72.433J", "S72.433K", "S72.433M", "S72.433N", "S72.433P", "S72.433Q", "S72.433R", "S72.433S",
				"S72.436?", "S72.436A", "S72.436B", "S72.436C", "S72.436D", "S72.436E", "S72.436F", "S72.436G",
				"S72.436H", "S72.436J", "S72.436K", "S72.436M", "S72.436N", "S72.436P", "S72.436Q", "S72.436R",
				"S72.436S", "S72.443?", "S72.443A", "S72.443B", "S72.443C", "S72.443D", "S72.443E", "S72.443F",
				"S72.443G", "S72.443H", "S72.443J", "S72.443K", "S72.443M", "S72.443N", "S72.443P", "S72.443Q",
				"S72.443R", "S72.443S", "S72.446?", "S72.446A", "S72.446B", "S72.446C", "S72.446D", "S72.446E",
				"S72.446F", "S72.446G", "S72.446H", "S72.446J", "S72.446K", "S72.446M", "S72.446N", "S72.446P",
				"S72.446Q", "S72.446R", "S72.446S", "S72.453?", "S72.453A", "S72.453B", "S72.453C", "S72.453D",
				"S72.453E", "S72.453F", "S72.453G", "S72.453H", "S72.453J", "S72.453K", "S72.453M", "S72.453N",
				"S72.453P", "S72.453Q", "S72.453R", "S72.453S", "S72.456?", "S72.456A", "S72.456B", "S72.456C",
				"S72.456D", "S72.456E", "S72.456F", "S72.456G", "S72.456H", "S72.456J", "S72.456K", "S72.456M",
				"S72.456N", "S72.456P", "S72.456Q", "S72.456R", "S72.456S", "S72.463?", "S72.463A", "S72.463B",
				"S72.463C", "S72.463D", "S72.463E", "S72.463F", "S72.463G", "S72.463H", "S72.463J", "S72.463K",
				"S72.463M", "S72.463N", "S72.463P", "S72.463Q", "S72.463R", "S72.463S", "S72.466?", "S72.466A",
				"S72.466B", "S72.466C", "S72.466D", "S72.466E", "S72.466F", "S72.466G", "S72.466H", "S72.466J",
				"S72.466K", "S72.466M", "S72.466N", "S72.466P", "S72.466Q", "S72.466R", "S72.466S", "S72.479?",
				"S72.479A", "S72.479D", "S72.479G", "S72.479K", "S72.479P", "S72.479S", "S72.499?", "S72.499A",
				"S72.499B", "S72.499C", "S72.499D", "S72.499E", "S72.499F", "S72.499G", "S72.499H", "S72.499J",
				"S72.499K", "S72.499M", "S72.499N", "S72.499P", "S72.499Q", "S72.499R", "S72.499S", "S72.8X9?",
				"S72.8X9A", "S72.8X9B", "S72.8X9C", "S72.8X9D", "S72.8X9E", "S72.8X9F", "S72.8X9G", "S72.8X9H",
				"S72.8X9J", "S72.8X9K", "S72.8X9M", "S72.8X9N", "S72.8X9P", "S72.8X9Q", "S72.8X9R", "S72.8X9S",
				"S72.90X?", "S72.90XA", "S72.90XB", "S72.90XC", "S72.90XD", "S72.90XE", "S72.90XF", "S72.90XG",
				"S72.90XH", "S72.90XJ", "S72.90XK", "S72.90XM", "S72.90XN", "S72.90XP", "S72.90XQ", "S72.90XR",
				"S72.90XS", "S73.003?", "S73.003A", "S73.003D", "S73.003S", "S73.006?", "S73.006A", "S73.006D",
				"S73.006S", "S73.013?", "S73.013A", "S73.013D", "S73.013S", "S73.016?", "S73.016A", "S73.016D",
				"S73.016S", "S73.023?", "S73.023A", "S73.023D", "S73.023S", "S73.026?", "S73.026A", "S73.026D",
				"S73.026S", "S73.033?", "S73.033A", "S73.033D", "S73.033S", "S73.036?", "S73.036A", "S73.036D",
				"S73.036S", "S73.043?", "S73.043A", "S73.043D", "S73.043S", "S73.046?", "S73.046A", "S73.046D",
				"S73.046S", "S73.109?", "S73.109A", "S73.109D", "S73.109S", "S73.119?", "S73.119A", "S73.119D",
				"S73.119S", "S73.129?", "S73.129A", "S73.129D", "S73.129S", "S73.199?", "S73.199A", "S73.199D",
				"S73.199S", "S74.00X?", "S74.00XA", "S74.00XD", "S74.00XS", "S74.10X?", "S74.10XA", "S74.10XD",
				"S74.10XS", "S74.20X?", "S74.20XA", "S74.20XD", "S74.20XS", "S74.8X9?", "S74.8X9A", "S74.8X9D",
				"S74.8X9S", "S74.90X?", "S74.90XA", "S74.90XD", "S74.90XS", "S75.009?", "S75.009A", "S75.009D",
				"S75.009S", "S75.019?", "S75.019A", "S75.019D", "S75.019S", "S75.029?", "S75.029A", "S75.029D",
				"S75.029S", "S75.099?", "S75.099A", "S75.099D", "S75.099S", "S75.109?", "S75.109A", "S75.109D",
				"S75.109S", "S75.119?", "S75.119A", "S75.119D", "S75.119S", "S75.129?", "S75.129A", "S75.129D",
				"S75.129S", "S75.199?", "S75.199A", "S75.199D", "S75.199S", "S75.209?", "S75.209A", "S75.209D",
				"S75.209S", "S75.219?", "S75.219A", "S75.219D", "S75.219S", "S75.229?", "S75.229A", "S75.229D",
				"S75.229S", "S75.299?", "S75.299A", "S75.299D", "S75.299S", "S75.809?", "S75.809A", "S75.809D",
				"S75.809S", "S75.819?", "S75.819A", "S75.819D", "S75.819S", "S75.899?", "S75.899A", "S75.899D",
				"S75.899S", "S75.909?", "S75.909A", "S75.909D", "S75.909S", "S75.919?", "S75.919A", "S75.919D",
				"S75.919S", "S75.999?", "S75.999A", "S75.999D", "S75.999S", "S76.009?", "S76.009A", "S76.009D",
				"S76.009S", "S76.019?", "S76.019A", "S76.019D", "S76.019S", "S76.029?", "S76.029A", "S76.029D",
				"S76.029S", "S76.099?", "S76.099A", "S76.099D", "S76.099S", "S76.109?", "S76.109A", "S76.109D",
				"S76.109S", "S76.119?", "S76.119A", "S76.119D", "S76.119S", "S76.129?", "S76.129A", "S76.129D",
				"S76.129S", "S76.199?", "S76.199A", "S76.199D", "S76.199S", "S76.209?", "S76.209A", "S76.209D",
				"S76.209S", "S76.219?", "S76.219A", "S76.219D", "S76.219S", "S76.229?", "S76.229A", "S76.229D",
				"S76.229S", "S76.299?", "S76.299A", "S76.299D", "S76.299S", "S76.309?", "S76.309A", "S76.309D",
				"S76.309S", "S76.319?", "S76.319A", "S76.319D", "S76.319S", "S76.329?", "S76.329A", "S76.329D",
				"S76.329S", "S76.399?", "S76.399A", "S76.399D", "S76.399S", "S76.809?", "S76.809A", "S76.809D",
				"S76.809S", "S76.819?", "S76.819A", "S76.819D", "S76.819S", "S76.829?", "S76.829A", "S76.829D",
				"S76.829S", "S76.899?", "S76.899A", "S76.899D", "S76.899S", "S76.909?", "S76.909A", "S76.909D",
				"S76.909S", "S76.919?", "S76.919A", "S76.919D", "S76.919S", "S76.929?", "S76.929A", "S76.929D",
				"S76.929S", "S76.999?", "S76.999A", "S76.999D", "S76.999S", "S77.00X?", "S77.00XA", "S77.00XD",
				"S77.00XS", "S77.10X?", "S77.10XA", "S77.10XD", "S77.10XS", "S77.20X?", "S77.20XA", "S77.20XD",
				"S77.20XS", "S78.019?", "S78.019A", "S78.019D", "S78.019S", "S78.029?", "S78.029A", "S78.029D",
				"S78.029S", "S78.119?", "S78.119A", "S78.119D", "S78.119S", "S78.129?", "S78.129A", "S78.129D",
				"S78.129S", "S78.919?", "S78.919A", "S78.919D", "S78.919S", "S78.929?", "S78.929A", "S78.929D",
				"S78.929S", "S79.009?", "S79.009A", "S79.009D", "S79.009G", "S79.009K", "S79.009P", "S79.009S",
				"S79.019?", "S79.019A", "S79.019D", "S79.019G", "S79.019K", "S79.019P", "S79.019S", "S79.099?",
				"S79.099A", "S79.099D", "S79.099G", "S79.099K", "S79.099P", "S79.099S", "S79.109?", "S79.109A",
				"S79.109D", "S79.109G", "S79.109K", "S79.109P", "S79.109S", "S79.119?", "S79.119A", "S79.119D",
				"S79.119G", "S79.119K", "S79.119P", "S79.119S", "S79.129?", "S79.129A", "S79.129D", "S79.129G",
				"S79.129K", "S79.129P", "S79.129S", "S79.139?", "S79.139A", "S79.139D", "S79.139G", "S79.139K",
				"S79.139P", "S79.139S", "S79.149?", "S79.149A", "S79.149D", "S79.149G", "S79.149K", "S79.149P",
				"S79.149S", "S79.199?", "S79.199A", "S79.199D", "S79.199G", "S79.199K", "S79.199P", "S79.199S",
				"S79.819?", "S79.819A", "S79.819D", "S79.819S", "S79.829?", "S79.829A", "S79.829D", "S79.829S",
				"S79.919?", "S79.919A", "S79.919D", "S79.919S", "S79.929?", "S79.929A", "S79.929D", "S79.929S",
				"S80.00X?", "S80.00XA", "S80.00XD", "S80.00XS", "S80.10X?", "S80.10XA", "S80.10XD", "S80.10XS",
				"S80.219?", "S80.219A", "S80.219D", "S80.219S", "S80.229?", "S80.229A", "S80.229D", "S80.229S",
				"S80.249?", "S80.249A", "S80.249D", "S80.249S", "S80.259?", "S80.259A", "S80.259D", "S80.259S",
				"S80.269?", "S80.269A", "S80.269D", "S80.269S", "S80.279?", "S80.279A", "S80.279D", "S80.279S",
				"S80.819?", "S80.819A", "S80.819D", "S80.819S", "S80.829?", "S80.829A", "S80.829D", "S80.829S",
				"S80.849?", "S80.849A", "S80.849D", "S80.849S", "S80.859?", "S80.859A", "S80.859D", "S80.859S",
				"S80.869?", "S80.869A", "S80.869D", "S80.869S", "S80.879?", "S80.879A", "S80.879D", "S80.879S",
				"S80.919?", "S80.919A", "S80.919D", "S80.919S", "S80.929?", "S80.929A", "S80.929D", "S80.929S",
				"S81.009?", "S81.009A", "S81.009D", "S81.009S", "S81.019?", "S81.019A", "S81.019D", "S81.019S",
				"S81.029?", "S81.029A", "S81.029D", "S81.029S", "S81.039?", "S81.039A", "S81.039D", "S81.039S",
				"S81.049?", "S81.049A", "S81.049D", "S81.049S", "S81.059?", "S81.059A", "S81.059D", "S81.059S",
				"S81.809?", "S81.809A", "S81.809D", "S81.809S", "S81.819?", "S81.819A", "S81.819D", "S81.819S",
				"S81.829?", "S81.829A", "S81.829D", "S81.829S", "S81.839?", "S81.839A", "S81.839D", "S81.839S",
				"S81.849?", "S81.849A", "S81.849D", "S81.849S", "S81.859?", "S81.859A", "S81.859D", "S81.859S",
				"S82.009?", "S82.009A", "S82.009B", "S82.009C", "S82.009D", "S82.009E", "S82.009F", "S82.009G",
				"S82.009H", "S82.009J", "S82.009K", "S82.009M", "S82.009N", "S82.009P", "S82.009Q", "S82.009R",
				"S82.009S", "S82.013?", "S82.013A", "S82.013B", "S82.013C", "S82.013D", "S82.013E", "S82.013F",
				"S82.013G", "S82.013H", "S82.013J", "S82.013K", "S82.013M", "S82.013N", "S82.013P", "S82.013Q",
				"S82.013R", "S82.013S", "S82.016?", "S82.016A", "S82.016B", "S82.016C", "S82.016D", "S82.016E",
				"S82.016F", "S82.016G", "S82.016H", "S82.016J", "S82.016K", "S82.016M", "S82.016N", "S82.016P",
				"S82.016Q", "S82.016R", "S82.016S", "S82.023?", "S82.023A", "S82.023B", "S82.023C", "S82.023D",
				"S82.023E", "S82.023F", "S82.023G", "S82.023H", "S82.023J", "S82.023K", "S82.023M", "S82.023N",
				"S82.023P", "S82.023Q", "S82.023R", "S82.023S", "S82.026?", "S82.026A", "S82.026B", "S82.026C",
				"S82.026D", "S82.026E", "S82.026F", "S82.026G", "S82.026H", "S82.026J", "S82.026K", "S82.026M",
				"S82.026N", "S82.026P", "S82.026Q", "S82.026R", "S82.026S", "S82.033?", "S82.033A", "S82.033B",
				"S82.033C", "S82.033D", "S82.033E", "S82.033F", "S82.033G", "S82.033H", "S82.033J", "S82.033K",
				"S82.033M", "S82.033N", "S82.033P", "S82.033Q", "S82.033R", "S82.033S", "S82.036?", "S82.036A",
				"S82.036B", "S82.036C", "S82.036D", "S82.036E", "S82.036F", "S82.036G", "S82.036H", "S82.036J",
				"S82.036K", "S82.036M", "S82.036N", "S82.036P", "S82.036Q", "S82.036R", "S82.036S", "S82.043?",
				"S82.043A", "S82.043B", "S82.043C", "S82.043D", "S82.043E", "S82.043F", "S82.043G", "S82.043H",
				"S82.043J", "S82.043K", "S82.043M", "S82.043N", "S82.043P", "S82.043Q", "S82.043R", "S82.043S",
				"S82.046?", "S82.046A", "S82.046B", "S82.046C", "S82.046D", "S82.046E", "S82.046F", "S82.046G",
				"S82.046H", "S82.046J", "S82.046K", "S82.046M", "S82.046N", "S82.046P", "S82.046Q", "S82.046R",
				"S82.046S", "S82.099?", "S82.099A", "S82.099B", "S82.099C", "S82.099D", "S82.099E", "S82.099F",
				"S82.099G", "S82.099H", "S82.099J", "S82.099K", "S82.099M", "S82.099N", "S82.099P", "S82.099Q",
				"S82.099R", "S82.099S", "S82.109?", "S82.109A", "S82.109B", "S82.109C", "S82.109D", "S82.109E",
				"S82.109F", "S82.109G", "S82.109H", "S82.109J", "S82.109K", "S82.109M", "S82.109N", "S82.109P",
				"S82.109Q", "S82.109R", "S82.109S", "S82.113?", "S82.113A", "S82.113B", "S82.113C", "S82.113D",
				"S82.113E", "S82.113F", "S82.113G", "S82.113H", "S82.113J", "S82.113K", "S82.113M", "S82.113N",
				"S82.113P", "S82.113Q", "S82.113R", "S82.113S", "S82.116?", "S82.116A", "S82.116B", "S82.116C",
				"S82.116D", "S82.116E", "S82.116F", "S82.116G", "S82.116H", "S82.116J", "S82.116K", "S82.116M",
				"S82.116N", "S82.116P", "S82.116Q", "S82.116R", "S82.116S", "S82.123?", "S82.123A", "S82.123B",
				"S82.123C", "S82.123D", "S82.123E", "S82.123F", "S82.123G", "S82.123H", "S82.123J", "S82.123K",
				"S82.123M", "S82.123N", "S82.123P", "S82.123Q", "S82.123R", "S82.123S", "S82.126?", "S82.126A",
				"S82.126B", "S82.126C", "S82.126D", "S82.126E", "S82.126F", "S82.126G", "S82.126H", "S82.126J",
				"S82.126K", "S82.126M", "S82.126N", "S82.126P", "S82.126Q", "S82.126R", "S82.126S", "S82.133?",
				"S82.133A", "S82.133B", "S82.133C", "S82.133D", "S82.133E", "S82.133F", "S82.133G", "S82.133H",
				"S82.133J", "S82.133K", "S82.133M", "S82.133N", "S82.133P", "S82.133Q", "S82.133R", "S82.133S",
				"S82.136?", "S82.136A", "S82.136B", "S82.136C", "S82.136D", "S82.136E", "S82.136F", "S82.136G",
				"S82.136H", "S82.136J", "S82.136K", "S82.136M", "S82.136N", "S82.136P", "S82.136Q", "S82.136R",
				"S82.136S", "S82.143?", "S82.143A", "S82.143B", "S82.143C", "S82.143D", "S82.143E", "S82.143F",
				"S82.143G", "S82.143H", "S82.143J", "S82.143K", "S82.143M", "S82.143N", "S82.143P", "S82.143Q",
				"S82.143R", "S82.143S", "S82.146?", "S82.146A", "S82.146B", "S82.146C", "S82.146D", "S82.146E",
				"S82.146F", "S82.146G", "S82.146H", "S82.146J", "S82.146K", "S82.146M", "S82.146N", "S82.146P",
				"S82.146Q", "S82.146R", "S82.146S", "S82.153?", "S82.153A", "S82.153B", "S82.153C", "S82.153D",
				"S82.153E", "S82.153F", "S82.153G", "S82.153H", "S82.153J", "S82.153K", "S82.153M", "S82.153N",
				"S82.153P", "S82.153Q", "S82.153R", "S82.153S", "S82.156?", "S82.156A", "S82.156B", "S82.156C",
				"S82.156D", "S82.156E", "S82.156F", "S82.156G", "S82.156H", "S82.156J", "S82.156K", "S82.156M",
				"S82.156N", "S82.156P", "S82.156Q", "S82.156R", "S82.156S", "S82.169?", "S82.169A", "S82.169D",
				"S82.169G", "S82.169K", "S82.169P", "S82.169S", "S82.199?", "S82.199A", "S82.199B", "S82.199C",
				"S82.199D", "S82.199E", "S82.199F", "S82.199G", "S82.199H", "S82.199J", "S82.199K", "S82.199M",
				"S82.199N", "S82.199P", "S82.199Q", "S82.199R", "S82.199S", "S82.209?", "S82.209A", "S82.209B",
				"S82.209C", "S82.209D", "S82.209E", "S82.209F", "S82.209G", "S82.209H", "S82.209J", "S82.209K",
				"S82.209M", "S82.209N", "S82.209P", "S82.209Q", "S82.209R", "S82.209S", "S82.223?", "S82.223A",
				"S82.223B", "S82.223C", "S82.223D", "S82.223E", "S82.223F", "S82.223G", "S82.223H", "S82.223J",
				"S82.223K", "S82.223M", "S82.223N", "S82.223P", "S82.223Q", "S82.223R", "S82.223S", "S82.226?",
				"S82.226A", "S82.226B", "S82.226C", "S82.226D", "S82.226E", "S82.226F", "S82.226G", "S82.226H",
				"S82.226J", "S82.226K", "S82.226M", "S82.226N", "S82.226P", "S82.226Q", "S82.226R", "S82.226S",
				"S82.233?", "S82.233A", "S82.233B", "S82.233C", "S82.233D", "S82.233E", "S82.233F", "S82.233G",
				"S82.233H", "S82.233J", "S82.233K", "S82.233M", "S82.233N", "S82.233P", "S82.233Q", "S82.233R",
				"S82.233S", "S82.236?", "S82.236A", "S82.236B", "S82.236C", "S82.236D", "S82.236E", "S82.236F",
				"S82.236G", "S82.236H", "S82.236J", "S82.236K", "S82.236M", "S82.236N", "S82.236P", "S82.236Q",
				"S82.236R", "S82.236S", "S82.243?", "S82.243A", "S82.243B", "S82.243C", "S82.243D", "S82.243E",
				"S82.243F", "S82.243G", "S82.243H", "S82.243J", "S82.243K", "S82.243M", "S82.243N", "S82.243P",
				"S82.243Q", "S82.243R", "S82.243S", "S82.246?", "S82.246A", "S82.246B", "S82.246C", "S82.246D",
				"S82.246E", "S82.246F", "S82.246G", "S82.246H", "S82.246J", "S82.246K", "S82.246M", "S82.246N",
				"S82.246P", "S82.246Q", "S82.246R", "S82.246S", "S82.253?", "S82.253A", "S82.253B", "S82.253C",
				"S82.253D", "S82.253E", "S82.253F", "S82.253G", "S82.253H", "S82.253J", "S82.253K", "S82.253M",
				"S82.253N", "S82.253P", "S82.253Q", "S82.253R", "S82.253S", "S82.256?", "S82.256A", "S82.256B",
				"S82.256C", "S82.256D", "S82.256E", "S82.256F", "S82.256G", "S82.256H", "S82.256J", "S82.256K",
				"S82.256M", "S82.256N", "S82.256P", "S82.256Q", "S82.256R", "S82.256S", "S82.263?", "S82.263A",
				"S82.263B", "S82.263C", "S82.263D", "S82.263E", "S82.263F", "S82.263G", "S82.263H", "S82.263J",
				"S82.263K", "S82.263M", "S82.263N", "S82.263P", "S82.263Q", "S82.263R", "S82.263S", "S82.266?",
				"S82.266A", "S82.266B", "S82.266C", "S82.266D", "S82.266E", "S82.266F", "S82.266G", "S82.266H",
				"S82.266J", "S82.266K", "S82.266M", "S82.266N", "S82.266P", "S82.266Q", "S82.266R", "S82.266S",
				"S82.299?", "S82.299A", "S82.299B", "S82.299C", "S82.299D", "S82.299E", "S82.299F", "S82.299G",
				"S82.299H", "S82.299J", "S82.299K", "S82.299M", "S82.299N", "S82.299P", "S82.299Q", "S82.299R",
				"S82.299S", "S82.309?", "S82.309A", "S82.309B", "S82.309C", "S82.309D", "S82.309E", "S82.309F",
				"S82.309G", "S82.309H", "S82.309J", "S82.309K", "S82.309M", "S82.309N", "S82.309P", "S82.309Q",
				"S82.309R", "S82.309S", "S82.319?", "S82.319A", "S82.319D", "S82.319G", "S82.319K", "S82.319P",
				"S82.319S", "S82.399?", "S82.399A", "S82.399B", "S82.399C", "S82.399D", "S82.399E", "S82.399F",
				"S82.399G", "S82.399H", "S82.399J", "S82.399K", "S82.399M", "S82.399N", "S82.399P", "S82.399Q",
				"S82.399R", "S82.399S", "S82.409?", "S82.409A", "S82.409B", "S82.409C", "S82.409D", "S82.409E",
				"S82.409F", "S82.409G", "S82.409H", "S82.409J", "S82.409K", "S82.409M", "S82.409N", "S82.409P",
				"S82.409Q", "S82.409R", "S82.409S", "S82.423?", "S82.423A", "S82.423B", "S82.423C", "S82.423D",
				"S82.423E", "S82.423F", "S82.423G", "S82.423H", "S82.423J", "S82.423K", "S82.423M", "S82.423N",
				"S82.423P", "S82.423Q", "S82.423R", "S82.423S", "S82.426?", "S82.426A", "S82.426B", "S82.426C",
				"S82.426D", "S82.426E", "S82.426F", "S82.426G", "S82.426H", "S82.426J", "S82.426K", "S82.426M",
				"S82.426N", "S82.426P", "S82.426Q", "S82.426R", "S82.426S", "S82.433?", "S82.433A", "S82.433B",
				"S82.433C", "S82.433D", "S82.433E", "S82.433F", "S82.433G", "S82.433H", "S82.433J", "S82.433K",
				"S82.433M", "S82.433N", "S82.433P", "S82.433Q", "S82.433R", "S82.433S", "S82.436?", "S82.436A",
				"S82.436B", "S82.436C", "S82.436D", "S82.436E", "S82.436F", "S82.436G", "S82.436H", "S82.436J",
				"S82.436K", "S82.436M", "S82.436N", "S82.436P", "S82.436Q", "S82.436R", "S82.436S", "S82.443?",
				"S82.443A", "S82.443B", "S82.443C", "S82.443D", "S82.443E", "S82.443F", "S82.443G", "S82.443H",
				"S82.443J", "S82.443K", "S82.443M", "S82.443N", "S82.443P", "S82.443Q", "S82.443R", "S82.443S",
				"S82.446?", "S82.446A", "S82.446B", "S82.446C", "S82.446D", "S82.446E", "S82.446F", "S82.446G",
				"S82.446H", "S82.446J", "S82.446K", "S82.446M", "S82.446N", "S82.446P", "S82.446Q", "S82.446R",
				"S82.446S", "S82.453?", "S82.453A", "S82.453B", "S82.453C", "S82.453D", "S82.453E", "S82.453F",
				"S82.453G", "S82.453H", "S82.453J", "S82.453K", "S82.453M", "S82.453N", "S82.453P", "S82.453Q",
				"S82.453R", "S82.453S", "S82.456?", "S82.456A", "S82.456B", "S82.456C", "S82.456D", "S82.456E",
				"S82.456F", "S82.456G", "S82.456H", "S82.456J", "S82.456K", "S82.456M", "S82.456N", "S82.456P",
				"S82.456Q", "S82.456R", "S82.456S", "S82.463?", "S82.463A", "S82.463B", "S82.463C", "S82.463D",
				"S82.463E", "S82.463F", "S82.463G", "S82.463H", "S82.463J", "S82.463K", "S82.463M", "S82.463N",
				"S82.463P", "S82.463Q", "S82.463R", "S82.463S", "S82.466?", "S82.466A", "S82.466B", "S82.466C",
				"S82.466D", "S82.466E", "S82.466F", "S82.466G", "S82.466H", "S82.466J", "S82.466K", "S82.466M",
				"S82.466N", "S82.466P", "S82.466Q", "S82.466R", "S82.466S", "S82.499?", "S82.499A", "S82.499B",
				"S82.499C", "S82.499D", "S82.499E", "S82.499F", "S82.499G", "S82.499H", "S82.499J", "S82.499K",
				"S82.499M", "S82.499N", "S82.499P", "S82.499Q", "S82.499R", "S82.499S", "S82.53X?", "S82.53XA",
				"S82.53XB", "S82.53XC", "S82.53XD", "S82.53XE", "S82.53XF", "S82.53XG", "S82.53XH", "S82.53XJ",
				"S82.53XK", "S82.53XM", "S82.53XN", "S82.53XP", "S82.53XQ", "S82.53XR", "S82.53XS", "S82.56X?",
				"S82.56XA", "S82.56XB", "S82.56XC", "S82.56XD", "S82.56XE", "S82.56XF", "S82.56XG", "S82.56XH",
				"S82.56XJ", "S82.56XK", "S82.56XM", "S82.56XN", "S82.56XP", "S82.56XQ", "S82.56XR", "S82.56XS",
				"S82.63X?", "S82.63XA", "S82.63XB", "S82.63XC", "S82.63XD", "S82.63XE", "S82.63XF", "S82.63XG",
				"S82.63XH", "S82.63XJ", "S82.63XK", "S82.63XM", "S82.63XN", "S82.63XP", "S82.63XQ", "S82.63XR",
				"S82.63XS", "S82.66X?", "S82.66XA", "S82.66XB", "S82.66XC", "S82.66XD", "S82.66XE", "S82.66XF",
				"S82.66XG", "S82.66XH", "S82.66XJ", "S82.66XK", "S82.66XM", "S82.66XN", "S82.66XP", "S82.66XQ",
				"S82.66XR", "S82.66XS", "S82.819?", "S82.819A", "S82.819D", "S82.819G", "S82.819K", "S82.819P",
				"S82.819S", "S82.829?", "S82.829A", "S82.829D", "S82.829G", "S82.829K", "S82.829P", "S82.829S",
				"S82.839?", "S82.839A", "S82.839B", "S82.839C", "S82.839D", "S82.839E", "S82.839F", "S82.839G",
				"S82.839H", "S82.839J", "S82.839K", "S82.839M", "S82.839N", "S82.839P", "S82.839Q", "S82.839R",
				"S82.839S", "S82.843?", "S82.843A", "S82.843B", "S82.843C", "S82.843D", "S82.843E", "S82.843F",
				"S82.843G", "S82.843H", "S82.843J", "S82.843K", "S82.843M", "S82.843N", "S82.843P", "S82.843Q",
				"S82.843R", "S82.843S", "S82.846?", "S82.846A", "S82.846B", "S82.846C", "S82.846D", "S82.846E",
				"S82.846F", "S82.846G", "S82.846H", "S82.846J", "S82.846K", "S82.846M", "S82.846N", "S82.846P",
				"S82.846Q", "S82.846R", "S82.846S", "S82.853?", "S82.853A", "S82.853B", "S82.853C", "S82.853D",
				"S82.853E", "S82.853F", "S82.853G", "S82.853H", "S82.853J", "S82.853K", "S82.853M", "S82.853N",
				"S82.853P", "S82.853Q", "S82.853R", "S82.853S", "S82.856?", "S82.856A", "S82.856B", "S82.856C",
				"S82.856D", "S82.856E", "S82.856F", "S82.856G", "S82.856H", "S82.856J", "S82.856K", "S82.856M",
				"S82.856N", "S82.856P", "S82.856Q", "S82.856R", "S82.856S", "S82.863?", "S82.863A", "S82.863B",
				"S82.863C", "S82.863D", "S82.863E", "S82.863F", "S82.863G", "S82.863H", "S82.863J", "S82.863K",
				"S82.863M", "S82.863N", "S82.863P", "S82.863Q", "S82.863R", "S82.863S", "S82.866?", "S82.866A",
				"S82.866B", "S82.866C", "S82.866D", "S82.866E", "S82.866F", "S82.866G", "S82.866H", "S82.866J",
				"S82.866K", "S82.866M", "S82.866N", "S82.866P", "S82.866Q", "S82.866R", "S82.866S", "S82.873?",
				"S82.873A", "S82.873B", "S82.873C", "S82.873D", "S82.873E", "S82.873F", "S82.873G", "S82.873H",
				"S82.873J", "S82.873K", "S82.873M", "S82.873N", "S82.873P", "S82.873Q", "S82.873R", "S82.873S",
				"S82.876?", "S82.876A", "S82.876B", "S82.876C", "S82.876D", "S82.876E", "S82.876F", "S82.876G",
				"S82.876H", "S82.876J", "S82.876K", "S82.876M", "S82.876N", "S82.876P", "S82.876Q", "S82.876R",
				"S82.876S", "S82.899?", "S82.899A", "S82.899B", "S82.899C", "S82.899D", "S82.899E", "S82.899F",
				"S82.899G", "S82.899H", "S82.899J", "S82.899K", "S82.899M", "S82.899N", "S82.899P", "S82.899Q",
				"S82.899R", "S82.899S", "S82.90X?", "S82.90XA", "S82.90XB", "S82.90XC", "S82.90XD", "S82.90XE",
				"S82.90XF", "S82.90XG", "S82.90XH", "S82.90XJ", "S82.90XK", "S82.90XM", "S82.90XN", "S82.90XP",
				"S82.90XQ", "S82.90XR", "S82.90XS", "S83.003?", "S83.003A", "S83.003D", "S83.003S", "S83.006?",
				"S83.006A", "S83.006D", "S83.006S", "S83.013?", "S83.013A", "S83.013D", "S83.013S", "S83.016?",
				"S83.016A", "S83.016D", "S83.016S", "S83.093?", "S83.093A", "S83.093D", "S83.093S", "S83.096?",
				"S83.096A", "S83.096D", "S83.096S", "S83.103?", "S83.103A", "S83.103D", "S83.103S", "S83.106?",
				"S83.106A", "S83.106D", "S83.106S", "S83.113?", "S83.113A", "S83.113D", "S83.113S", "S83.116?",
				"S83.116A", "S83.116D", "S83.116S", "S83.123?", "S83.123A", "S83.123D", "S83.123S", "S83.126?",
				"S83.126A", "S83.126D", "S83.126S", "S83.133?", "S83.133A", "S83.133D", "S83.133S", "S83.136?",
				"S83.136A", "S83.136D", "S83.136S", "S83.143?", "S83.143A", "S83.143D", "S83.143S", "S83.146?",
				"S83.146A", "S83.146D", "S83.146S", "S83.193?", "S83.193A", "S83.193D", "S83.193S", "S83.196?",
				"S83.196A", "S83.196D", "S83.196S", "S83.202?", "S83.202A", "S83.202D", "S83.202S", "S83.205?",
				"S83.205A", "S83.205D", "S83.205S", "S83.209?", "S83.209A", "S83.209D", "S83.209S", "S83.219?",
				"S83.219A", "S83.219D", "S83.219S", "S83.229?", "S83.229A", "S83.229D", "S83.229S", "S83.239?",
				"S83.239A", "S83.239D", "S83.239S", "S83.249?", "S83.249A", "S83.249D", "S83.249S", "S83.259?",
				"S83.259A", "S83.259D", "S83.259S", "S83.269?", "S83.269A", "S83.269D", "S83.269S", "S83.279?",
				"S83.279A", "S83.279D", "S83.279S", "S83.289?", "S83.289A", "S83.289D", "S83.289S", "S83.30X?",
				"S83.30XA", "S83.30XD", "S83.30XS", "S83.409?", "S83.409A", "S83.409D", "S83.409S", "S83.419?",
				"S83.419A", "S83.419D", "S83.419S", "S83.429?", "S83.429A", "S83.429D", "S83.429S", "S83.509?",
				"S83.509A", "S83.509D", "S83.509S", "S83.519?", "S83.519A", "S83.519D", "S83.519S", "S83.529?",
				"S83.529A", "S83.529D", "S83.529S", "S83.60X?", "S83.60XA", "S83.60XD", "S83.60XS", "S83.8X9?",
				"S83.8X9A", "S83.8X9D", "S83.8X9S", "S83.90X?", "S83.90XA", "S83.90XD", "S83.90XS", "S84.00X?",
				"S84.00XA", "S84.00XD", "S84.00XS", "S84.10X?", "S84.10XA", "S84.10XD", "S84.10XS", "S84.20X?",
				"S84.20XA", "S84.20XD", "S84.20XS", "S84.809?", "S84.809A", "S84.809D", "S84.809S", "S84.90X?",
				"S84.90XA", "S84.90XD", "S84.90XS", "S85.009?", "S85.009A", "S85.009D", "S85.009S", "S85.019?",
				"S85.019A", "S85.019D", "S85.019S", "S85.099?", "S85.099A", "S85.099D", "S85.099S", "S85.109?",
				"S85.109A", "S85.109D", "S85.109S", "S85.119?", "S85.119A", "S85.119D", "S85.119S", "S85.129?",
				"S85.129A", "S85.129D", "S85.129S", "S85.139?", "S85.139A", "S85.139D", "S85.139S", "S85.149?",
				"S85.149A", "S85.149D", "S85.149S", "S85.159?", "S85.159A", "S85.159D", "S85.159S", "S85.169?",
				"S85.169A", "S85.169D", "S85.169S", "S85.179?", "S85.179A", "S85.179D", "S85.179S", "S85.189?",
				"S85.189A", "S85.189D", "S85.189S", "S85.209?", "S85.209A", "S85.209D", "S85.209S", "S85.219?",
				"S85.219A", "S85.219D", "S85.219S", "S85.299?", "S85.299A", "S85.299D", "S85.299S", "S85.309?",
				"S85.309A", "S85.309D", "S85.309S", "S85.319?", "S85.319A", "S85.319D", "S85.319S", "S85.399?",
				"S85.399A", "S85.399D", "S85.399S", "S85.409?", "S85.409A", "S85.409D", "S85.409S", "S85.419?",
				"S85.419A", "S85.419D", "S85.419S", "S85.499?", "S85.499A", "S85.499D", "S85.499S", "S85.509?",
				"S85.509A", "S85.509D", "S85.509S", "S85.519?", "S85.519A", "S85.519D", "S85.519S", "S85.599?",
				"S85.599A", "S85.599D", "S85.599S", "S85.809?", "S85.809A", "S85.809D", "S85.809S", "S85.819?",
				"S85.819A", "S85.819D", "S85.819S", "S85.899?", "S85.899A", "S85.899D", "S85.899S", "S85.909?",
				"S85.909A", "S85.909D", "S85.909S", "S85.919?", "S85.919A", "S85.919D", "S85.919S", "S85.999?",
				"S85.999A", "S85.999D", "S85.999S", "S86.009?", "S86.009A", "S86.009D", "S86.009S", "S86.019?",
				"S86.019A", "S86.019D", "S86.019S", "S86.029?", "S86.029A", "S86.029D", "S86.029S", "S86.099?",
				"S86.099A", "S86.099D", "S86.099S", "S86.109?", "S86.109A", "S86.109D", "S86.109S", "S86.119?",
				"S86.119A", "S86.119D", "S86.119S", "S86.129?", "S86.129A", "S86.129D", "S86.129S", "S86.199?",
				"S86.199A", "S86.199D", "S86.199S", "S86.209?", "S86.209A", "S86.209D", "S86.209S", "S86.219?",
				"S86.219A", "S86.219D", "S86.219S", "S86.229?", "S86.229A", "S86.229D", "S86.229S", "S86.299?",
				"S86.299A", "S86.299D", "S86.299S", "S86.309?", "S86.309A", "S86.309D", "S86.309S", "S86.319?",
				"S86.319A", "S86.319D", "S86.319S", "S86.329?", "S86.329A", "S86.329D", "S86.329S", "S86.399?",
				"S86.399A", "S86.399D", "S86.399S", "S86.809?", "S86.809A", "S86.809D", "S86.809S", "S86.819?",
				"S86.819A", "S86.819D", "S86.819S", "S86.829?", "S86.829A", "S86.829D", "S86.829S", "S86.899?",
				"S86.899A", "S86.899D", "S86.899S", "S86.909?", "S86.909A", "S86.909D", "S86.909S", "S86.919?",
				"S86.919A", "S86.919D", "S86.919S", "S86.929?", "S86.929A", "S86.929D", "S86.929S", "S86.999?",
				"S86.999A", "S86.999D", "S86.999S", "S87.00X?", "S87.00XA", "S87.00XD", "S87.00XS", "S87.80X?",
				"S87.80XA", "S87.80XD", "S87.80XS", "S88.019?", "S88.019A", "S88.019D", "S88.019S", "S88.029?",
				"S88.029A", "S88.029D", "S88.029S", "S88.119?", "S88.119A", "S88.119D", "S88.119S", "S88.129?",
				"S88.129A", "S88.129D", "S88.129S", "S88.919?", "S88.919A", "S88.919D", "S88.919S", "S88.929?",
				"S88.929A", "S88.929D", "S88.929S", "S89.009?", "S89.009A", "S89.009D", "S89.009G", "S89.009K",
				"S89.009P", "S89.009S", "S89.019?", "S89.019A", "S89.019D", "S89.019G", "S89.019K", "S89.019P",
				"S89.019S", "S89.029?", "S89.029A", "S89.029D", "S89.029G", "S89.029K", "S89.029P", "S89.029S",
				"S89.039?", "S89.039A", "S89.039D", "S89.039G", "S89.039K", "S89.039P", "S89.039S", "S89.049?",
				"S89.049A", "S89.049D", "S89.049G", "S89.049K", "S89.049P", "S89.049S", "S89.099?", "S89.099A",
				"S89.099D", "S89.099G", "S89.099K", "S89.099P", "S89.099S", "S89.109?", "S89.109A", "S89.109D",
				"S89.109G", "S89.109K", "S89.109P", "S89.109S", "S89.119?", "S89.119A", "S89.119D", "S89.119G",
				"S89.119K", "S89.119P", "S89.119S", "S89.129?", "S89.129A", "S89.129D", "S89.129G", "S89.129K",
				"S89.129P", "S89.129S", "S89.139?", "S89.139A", "S89.139D", "S89.139G", "S89.139K", "S89.139P",
				"S89.139S", "S89.149?", "S89.149A", "S89.149D", "S89.149G", "S89.149K", "S89.149P", "S89.149S",
				"S89.199?", "S89.199A", "S89.199D", "S89.199G", "S89.199K", "S89.199P", "S89.199S", "S89.209?",
				"S89.209A", "S89.209D", "S89.209G", "S89.209K", "S89.209P", "S89.209S", "S89.219?", "S89.219A",
				"S89.219D", "S89.219G", "S89.219K", "S89.219P", "S89.219S", "S89.229?", "S89.229A", "S89.229D",
				"S89.229G", "S89.229K", "S89.229P", "S89.229S", "S89.299?", "S89.299A", "S89.299D", "S89.299G",
				"S89.299K", "S89.299P", "S89.299S", "S89.309?", "S89.309A", "S89.309D", "S89.309G", "S89.309K",
				"S89.309P", "S89.309S", "S89.319?", "S89.319A", "S89.319D", "S89.319G", "S89.319K", "S89.319P",
				"S89.319S", "S89.329?", "S89.329A", "S89.329D", "S89.329G", "S89.329K", "S89.329P", "S89.329S",
				"S89.399?", "S89.399A", "S89.399D", "S89.399G", "S89.399K", "S89.399P", "S89.399S", "S89.80X?",
				"S89.80XA", "S89.80XD", "S89.80XS", "S89.90X?", "S89.90XA", "S89.90XD", "S89.90XS", "S90.00X?",
				"S90.00XA", "S90.00XD", "S90.00XS", "S90.119?", "S90.119A", "S90.119D", "S90.119S", "S90.129?",
				"S90.129A", "S90.129D", "S90.129S", "S90.219?", "S90.219A", "S90.219D", "S90.219S", "S90.229?",
				"S90.229A", "S90.229D", "S90.229S", "S90.30X?", "S90.30XA", "S90.30XD", "S90.30XS", "S90.413?",
				"S90.413A", "S90.413D", "S90.413S", "S90.416?", "S90.416A", "S90.416D", "S90.416S", "S90.423?",
				"S90.423A", "S90.423D", "S90.423S", "S90.426?", "S90.426A", "S90.426D", "S90.426S", "S90.443?",
				"S90.443A", "S90.443D", "S90.443S", "S90.446?", "S90.446A", "S90.446D", "S90.446S", "S90.453?",
				"S90.453A", "S90.453D", "S90.453S", "S90.456?", "S90.456A", "S90.456D", "S90.456S", "S90.463?",
				"S90.463A", "S90.463D", "S90.463S", "S90.466?", "S90.466A", "S90.466D", "S90.466S", "S90.473?",
				"S90.473A", "S90.473D", "S90.473S", "S90.476?", "S90.476A", "S90.476D", "S90.476S", "S90.519?",
				"S90.519A", "S90.519D", "S90.519S", "S90.529?", "S90.529A", "S90.529D", "S90.529S", "S90.549?",
				"S90.549A", "S90.549D", "S90.549S", "S90.559?", "S90.559A", "S90.559D", "S90.559S", "S90.569?",
				"S90.569A", "S90.569D", "S90.569S", "S90.579?", "S90.579A", "S90.579D", "S90.579S", "S90.819?",
				"S90.819A", "S90.819D", "S90.819S", "S90.829?", "S90.829A", "S90.829D", "S90.829S", "S90.849?",
				"S90.849A", "S90.849D", "S90.849S", "S90.859?", "S90.859A", "S90.859D", "S90.859S", "S90.869?",
				"S90.869A", "S90.869D", "S90.869S", "S90.879?", "S90.879A", "S90.879D", "S90.879S", "S90.919?",
				"S90.919A", "S90.919D", "S90.919S", "S90.929?", "S90.929A", "S90.929D", "S90.929S", "S90.933?",
				"S90.933A", "S90.933D", "S90.933S", "S90.936?", "S90.936A", "S90.936D", "S90.936S", "S91.009?",
				"S91.009A", "S91.009D", "S91.009S", "S91.019?", "S91.019A", "S91.019D", "S91.019S", "S91.029?",
				"S91.029A", "S91.029D", "S91.029S", "S91.039?", "S91.039A", "S91.039D", "S91.039S", "S91.049?",
				"S91.049A", "S91.049D", "S91.049S", "S91.059?", "S91.059A", "S91.059D", "S91.059S", "S91.103?",
				"S91.103A", "S91.103D", "S91.103S", "S91.106?", "S91.106A", "S91.106D", "S91.106S", "S91.109?",
				"S91.109A", "S91.109D", "S91.109S", "S91.113?", "S91.113A", "S91.113D", "S91.113S", "S91.116?",
				"S91.116A", "S91.116D", "S91.116S", "S91.119?", "S91.119A", "S91.119D", "S91.119S", "S91.123?",
				"S91.123A", "S91.123D", "S91.123S", "S91.126?", "S91.126A", "S91.126D", "S91.126S", "S91.129?",
				"S91.129A", "S91.129D", "S91.129S", "S91.133?", "S91.133A", "S91.133D", "S91.133S", "S91.136?",
				"S91.136A", "S91.136D", "S91.136S", "S91.139?", "S91.139A", "S91.139D", "S91.139S", "S91.143?",
				"S91.143A", "S91.143D", "S91.143S", "S91.146?", "S91.146A", "S91.146D", "S91.146S", "S91.149?",
				"S91.149A", "S91.149D", "S91.149S", "S91.153?", "S91.153A", "S91.153D", "S91.153S", "S91.156?",
				"S91.156A", "S91.156D", "S91.156S", "S91.159?", "S91.159A", "S91.159D", "S91.159S", "S91.203?",
				"S91.203A", "S91.203D", "S91.203S", "S91.206?", "S91.206A", "S91.206D", "S91.206S", "S91.209?",
				"S91.209A", "S91.209D", "S91.209S", "S91.213?", "S91.213A", "S91.213D", "S91.213S", "S91.216?",
				"S91.216A", "S91.216D", "S91.216S", "S91.219?", "S91.219A", "S91.219D", "S91.219S", "S91.223?",
				"S91.223A", "S91.223D", "S91.223S", "S91.226?", "S91.226A", "S91.226D", "S91.226S", "S91.229?",
				"S91.229A", "S91.229D", "S91.229S", "S91.233?", "S91.233A", "S91.233D", "S91.233S", "S91.236?",
				"S91.236A", "S91.236D", "S91.236S", "S91.239?", "S91.239A", "S91.239D", "S91.239S", "S91.243?",
				"S91.243A", "S91.243D", "S91.243S", "S91.246?", "S91.246A", "S91.246D", "S91.246S", "S91.249?",
				"S91.249A", "S91.249D", "S91.249S", "S91.253?", "S91.253A", "S91.253D", "S91.253S", "S91.256?",
				"S91.256A", "S91.256D", "S91.256S", "S91.259?", "S91.259A", "S91.259D", "S91.259S", "S91.309?",
				"S91.309A", "S91.309D", "S91.309S", "S91.319?", "S91.319A", "S91.319D", "S91.319S", "S91.329?",
				"S91.329A", "S91.329D", "S91.329S", "S91.339?", "S91.339A", "S91.339D", "S91.339S", "S91.349?",
				"S91.349A", "S91.349D", "S91.349S", "S91.359?", "S91.359A", "S91.359D", "S91.359S", "S92.009?",
				"S92.009A", "S92.009B", "S92.009D", "S92.009G", "S92.009K", "S92.009P", "S92.009S", "S92.013?",
				"S92.013A", "S92.013B", "S92.013D", "S92.013G", "S92.013K", "S92.013P", "S92.013S", "S92.016?",
				"S92.016A", "S92.016B", "S92.016D", "S92.016G", "S92.016K", "S92.016P", "S92.016S", "S92.023?",
				"S92.023A", "S92.023B", "S92.023D", "S92.023G", "S92.023K", "S92.023P", "S92.023S", "S92.026?",
				"S92.026A", "S92.026B", "S92.026D", "S92.026G", "S92.026K", "S92.026P", "S92.026S", "S92.033?",
				"S92.033A", "S92.033B", "S92.033D", "S92.033G", "S92.033K", "S92.033P", "S92.033S", "S92.036?",
				"S92.036A", "S92.036B", "S92.036D", "S92.036G", "S92.036K", "S92.036P", "S92.036S", "S92.043?",
				"S92.043A", "S92.043B", "S92.043D", "S92.043G", "S92.043K", "S92.043P", "S92.043S", "S92.046?",
				"S92.046A", "S92.046B", "S92.046D", "S92.046G", "S92.046K", "S92.046P", "S92.046S", "S92.053?",
				"S92.053A", "S92.053B", "S92.053D", "S92.053G", "S92.053K", "S92.053P", "S92.053S", "S92.056?",
				"S92.056A", "S92.056B", "S92.056D", "S92.056G", "S92.056K", "S92.056P", "S92.056S", "S92.063?",
				"S92.063A", "S92.063B", "S92.063D", "S92.063G", "S92.063K", "S92.063P", "S92.063S", "S92.066?",
				"S92.066A", "S92.066B", "S92.066D", "S92.066G", "S92.066K", "S92.066P", "S92.066S", "S92.109?",
				"S92.109A", "S92.109B", "S92.109D", "S92.109G", "S92.109K", "S92.109P", "S92.109S", "S92.113?",
				"S92.113A", "S92.113B", "S92.113D", "S92.113G", "S92.113K", "S92.113P", "S92.113S", "S92.116?",
				"S92.116A", "S92.116B", "S92.116D", "S92.116G", "S92.116K", "S92.116P", "S92.116S", "S92.123?",
				"S92.123A", "S92.123B", "S92.123D", "S92.123G", "S92.123K", "S92.123P", "S92.123S", "S92.126?",
				"S92.126A", "S92.126B", "S92.126D", "S92.126G", "S92.126K", "S92.126P", "S92.126S", "S92.133?",
				"S92.133A", "S92.133B", "S92.133D", "S92.133G", "S92.133K", "S92.133P", "S92.133S", "S92.136?",
				"S92.136A", "S92.136B", "S92.136D", "S92.136G", "S92.136K", "S92.136P", "S92.136S", "S92.143?",
				"S92.143A", "S92.143B", "S92.143D", "S92.143G", "S92.143K", "S92.143P", "S92.143S", "S92.146?",
				"S92.146A", "S92.146B", "S92.146D", "S92.146G", "S92.146K", "S92.146P", "S92.146S", "S92.153?",
				"S92.153A", "S92.153B", "S92.153D", "S92.153G", "S92.153K", "S92.153P", "S92.153S", "S92.156?",
				"S92.156A", "S92.156B", "S92.156D", "S92.156G", "S92.156K", "S92.156P", "S92.156S", "S92.199?",
				"S92.199A", "S92.199B", "S92.199D", "S92.199G", "S92.199K", "S92.199P", "S92.199S", "S92.209?",
				"S92.209A", "S92.209B", "S92.209D", "S92.209G", "S92.209K", "S92.209P", "S92.209S", "S92.213?",
				"S92.213A", "S92.213B", "S92.213D", "S92.213G", "S92.213K", "S92.213P", "S92.213S", "S92.216?",
				"S92.216A", "S92.216B", "S92.216D", "S92.216G", "S92.216K", "S92.216P", "S92.216S", "S92.223?",
				"S92.223A", "S92.223B", "S92.223D", "S92.223G", "S92.223K", "S92.223P", "S92.223S", "S92.226?",
				"S92.226A", "S92.226B", "S92.226D", "S92.226G", "S92.226K", "S92.226P", "S92.226S", "S92.233?",
				"S92.233A", "S92.233B", "S92.233D", "S92.233G", "S92.233K", "S92.233P", "S92.233S", "S92.236?",
				"S92.236A", "S92.236B", "S92.236D", "S92.236G", "S92.236K", "S92.236P", "S92.236S", "S92.243?",
				"S92.243A", "S92.243B", "S92.243D", "S92.243G", "S92.243K", "S92.243P", "S92.243S", "S92.246?",
				"S92.246A", "S92.246B", "S92.246D", "S92.246G", "S92.246K", "S92.246P", "S92.246S", "S92.253?",
				"S92.253A", "S92.253B", "S92.253D", "S92.253G", "S92.253K", "S92.253P", "S92.253S", "S92.256?",
				"S92.256A", "S92.256B", "S92.256D", "S92.256G", "S92.256K", "S92.256P", "S92.256S", "S92.309?",
				"S92.309A", "S92.309B", "S92.309D", "S92.309G", "S92.309K", "S92.309P", "S92.309S", "S92.313?",
				"S92.313A", "S92.313B", "S92.313D", "S92.313G", "S92.313K", "S92.313P", "S92.313S", "S92.316?",
				"S92.316A", "S92.316B", "S92.316D", "S92.316G", "S92.316K", "S92.316P", "S92.316S", "S92.323?",
				"S92.323A", "S92.323B", "S92.323D", "S92.323G", "S92.323K", "S92.323P", "S92.323S", "S92.326?",
				"S92.326A", "S92.326B", "S92.326D", "S92.326G", "S92.326K", "S92.326P", "S92.326S", "S92.333?",
				"S92.333A", "S92.333B", "S92.333D", "S92.333G", "S92.333K", "S92.333P", "S92.333S", "S92.336?",
				"S92.336A", "S92.336B", "S92.336D", "S92.336G", "S92.336K", "S92.336P", "S92.336S", "S92.343?",
				"S92.343A", "S92.343B", "S92.343D", "S92.343G", "S92.343K", "S92.343P", "S92.343S", "S92.346?",
				"S92.346A", "S92.346B", "S92.346D", "S92.346G", "S92.346K", "S92.346P", "S92.346S", "S92.353?",
				"S92.353A", "S92.353B", "S92.353D", "S92.353G", "S92.353K", "S92.353P", "S92.353S", "S92.356?",
				"S92.356A", "S92.356B", "S92.356D", "S92.356G", "S92.356K", "S92.356P", "S92.356S", "S92.403?",
				"S92.403A", "S92.403B", "S92.403D", "S92.403G", "S92.403K", "S92.403P", "S92.403S", "S92.406?",
				"S92.406A", "S92.406B", "S92.406D", "S92.406G", "S92.406K", "S92.406P", "S92.406S", "S92.413?",
				"S92.413A", "S92.413B", "S92.413D", "S92.413G", "S92.413K", "S92.413P", "S92.413S", "S92.416?",
				"S92.416A", "S92.416B", "S92.416D", "S92.416G", "S92.416K", "S92.416P", "S92.416S", "S92.423?",
				"S92.423A", "S92.423B", "S92.423D", "S92.423G", "S92.423K", "S92.423P", "S92.423S", "S92.426?",
				"S92.426A", "S92.426B", "S92.426D", "S92.426G", "S92.426K", "S92.426P", "S92.426S", "S92.499?",
				"S92.499A", "S92.499B", "S92.499D", "S92.499G", "S92.499K", "S92.499P", "S92.499S", "S92.503?",
				"S92.503A", "S92.503B", "S92.503D", "S92.503G", "S92.503K", "S92.503P", "S92.503S", "S92.506?",
				"S92.506A", "S92.506B", "S92.506D", "S92.506G", "S92.506K", "S92.506P", "S92.506S", "S92.513?",
				"S92.513A", "S92.513B", "S92.513D", "S92.513G", "S92.513K", "S92.513P", "S92.513S", "S92.516?",
				"S92.516A", "S92.516B", "S92.516D", "S92.516G", "S92.516K", "S92.516P", "S92.516S", "S92.523?",
				"S92.523A", "S92.523B", "S92.523D", "S92.523G", "S92.523K", "S92.523P", "S92.523S", "S92.526?",
				"S92.526A", "S92.526B", "S92.526D", "S92.526G", "S92.526K", "S92.526P", "S92.526S", "S92.533?",
				"S92.533A", "S92.533B", "S92.533D", "S92.533G", "S92.533K", "S92.533P", "S92.533S", "S92.536?",
				"S92.536A", "S92.536B", "S92.536D", "S92.536G", "S92.536K", "S92.536P", "S92.536S", "S92.599?",
				"S92.599A", "S92.599B", "S92.599D", "S92.599G", "S92.599K", "S92.599P", "S92.599S", "S92.819?",
				"S92.819A", "S92.819B", "S92.819D", "S92.819G", "S92.819K", "S92.819P", "S92.819S", "S92.909?",
				"S92.909A", "S92.909B", "S92.909D", "S92.909G", "S92.909K", "S92.909P", "S92.909S", "S92.919?",
				"S92.919A", "S92.919B", "S92.919D", "S92.919G", "S92.919K", "S92.919P", "S92.919S", "S93.03X?",
				"S93.03XA", "S93.03XD", "S93.03XS", "S93.06X?", "S93.06XA", "S93.06XD", "S93.06XS", "S93.103?",
				"S93.103A", "S93.103D", "S93.103S", "S93.106?", "S93.106A", "S93.106D", "S93.106S", "S93.113?",
				"S93.113A", "S93.113D", "S93.113S", "S93.116?", "S93.116A", "S93.116D", "S93.116S", "S93.119?",
				"S93.119A", "S93.119D", "S93.119S", "S93.123?", "S93.123A", "S93.123D", "S93.123S", "S93.126?",
				"S93.126A", "S93.126D", "S93.126S", "S93.129?", "S93.129A", "S93.129D", "S93.129S", "S93.133?",
				"S93.133A", "S93.133D", "S93.133S", "S93.136?", "S93.136A", "S93.136D", "S93.136S", "S93.139?",
				"S93.139A", "S93.139D", "S93.139S", "S93.143?", "S93.143A", "S93.143D", "S93.143S", "S93.146?",
				"S93.146A", "S93.146D", "S93.146S", "S93.149?", "S93.149A", "S93.149D", "S93.149S", "S93.303?",
				"S93.303A", "S93.303D", "S93.303S", "S93.306?", "S93.306A", "S93.306D", "S93.306S", "S93.313?",
				"S93.313A", "S93.313D", "S93.313S", "S93.316?", "S93.316A", "S93.316D", "S93.316S", "S93.323?",
				"S93.323A", "S93.323D", "S93.323S", "S93.326?", "S93.326A", "S93.326D", "S93.326S", "S93.333?",
				"S93.333A", "S93.333D", "S93.333S", "S93.336?", "S93.336A", "S93.336D", "S93.336S", "S93.409?",
				"S93.409A", "S93.409D", "S93.409S", "S93.419?", "S93.419A", "S93.419D", "S93.419S", "S93.429?",
				"S93.429A", "S93.429D", "S93.429S", "S93.439?", "S93.439A", "S93.439D", "S93.439S", "S93.499?",
				"S93.499A", "S93.499D", "S93.499S", "S93.503?", "S93.503A", "S93.503D", "S93.503S", "S93.506?",
				"S93.506A", "S93.506D", "S93.506S", "S93.509?", "S93.509A", "S93.509D", "S93.509S", "S93.513?",
				"S93.513A", "S93.513D", "S93.513S", "S93.516?", "S93.516A", "S93.516D", "S93.516S", "S93.519?",
				"S93.519A", "S93.519D", "S93.519S", "S93.523?", "S93.523A", "S93.523D", "S93.523S", "S93.526?",
				"S93.526A", "S93.526D", "S93.526S", "S93.529?", "S93.529A", "S93.529D", "S93.529S", "S93.609?",
				"S93.609A", "S93.609D", "S93.609S", "S93.619?", "S93.619A", "S93.619D", "S93.619S", "S93.629?",
				"S93.629A", "S93.629D", "S93.629S", "S93.699?", "S93.699A", "S93.699D", "S93.699S", "S94.00X?",
				"S94.00XA", "S94.00XD", "S94.00XS", "S94.10X?", "S94.10XA", "S94.10XD", "S94.10XS", "S94.20X?",
				"S94.20XA", "S94.20XD", "S94.20XS", "S94.30X?", "S94.30XA", "S94.30XD", "S94.30XS", "S94.8X9?",
				"S94.8X9A", "S94.8X9D", "S94.8X9S", "S94.90X?", "S94.90XA", "S94.90XD", "S94.90XS", "S95.009?",
				"S95.009A", "S95.009D", "S95.009S", "S95.019?", "S95.019A", "S95.019D", "S95.019S", "S95.099?",
				"S95.099A", "S95.099D", "S95.099S", "S95.109?", "S95.109A", "S95.109D", "S95.109S", "S95.119?",
				"S95.119A", "S95.119D", "S95.119S", "S95.199?", "S95.199A", "S95.199D", "S95.199S", "S95.209?",
				"S95.209A", "S95.209D", "S95.209S", "S95.219?", "S95.219A", "S95.219D", "S95.219S", "S95.299?",
				"S95.299A", "S95.299D", "S95.299S", "S95.809?", "S95.809A", "S95.809D", "S95.809S", "S95.819?",
				"S95.819A", "S95.819D", "S95.819S", "S95.899?", "S95.899A", "S95.899D", "S95.899S", "S95.909?",
				"S95.909A", "S95.909D", "S95.909S", "S95.919?", "S95.919A", "S95.919D", "S95.919S", "S95.999?",
				"S95.999A", "S95.999D", "S95.999S", "S96.009?", "S96.009A", "S96.009D", "S96.009S", "S96.019?",
				"S96.019A", "S96.019D", "S96.019S", "S96.029?", "S96.029A", "S96.029D", "S96.029S", "S96.099?",
				"S96.099A", "S96.099D", "S96.099S", "S96.109?", "S96.109A", "S96.109D", "S96.109S", "S96.119?",
				"S96.119A", "S96.119D", "S96.119S", "S96.129?", "S96.129A", "S96.129D", "S96.129S", "S96.199?",
				"S96.199A", "S96.199D", "S96.199S", "S96.209?", "S96.209A", "S96.209D", "S96.209S", "S96.219?",
				"S96.219A", "S96.219D", "S96.219S", "S96.229?", "S96.229A", "S96.229D", "S96.229S", "S96.299?",
				"S96.299A", "S96.299D", "S96.299S", "S96.809?", "S96.809A", "S96.809D", "S96.809S", "S96.819?",
				"S96.819A", "S96.819D", "S96.819S", "S96.829?", "S96.829A", "S96.829D", "S96.829S", "S96.899?",
				"S96.899A", "S96.899D", "S96.899S", "S96.909?", "S96.909A", "S96.909D", "S96.909S", "S96.919?",
				"S96.919A", "S96.919D", "S96.919S", "S96.929?", "S96.929A", "S96.929D", "S96.929S", "S96.999?",
				"S96.999A", "S96.999D", "S96.999S", "S97.00X?", "S97.00XA", "S97.00XD", "S97.00XS", "S97.109?",
				"S97.109A", "S97.109D", "S97.109S", "S97.119?", "S97.119A", "S97.119D", "S97.119S", "S97.129?",
				"S97.129A", "S97.129D", "S97.129S", "S97.80X?", "S97.80XA", "S97.80XD", "S97.80XS", "S98.019?",
				"S98.019A", "S98.019D", "S98.019S", "S98.029?", "S98.029A", "S98.029D", "S98.029S", "S98.119?",
				"S98.119A", "S98.119D", "S98.119S", "S98.129?", "S98.129A", "S98.129D", "S98.129S", "S98.139?",
				"S98.139A", "S98.139D", "S98.139S", "S98.149?", "S98.149A", "S98.149D", "S98.149S", "S98.219?",
				"S98.219A", "S98.219D", "S98.219S", "S98.229?", "S98.229A", "S98.229D", "S98.229S", "S98.319?",
				"S98.319A", "S98.319D", "S98.319S", "S98.329?", "S98.329A", "S98.329D", "S98.329S", "S98.919?",
				"S98.919A", "S98.919D", "S98.919S", "S98.929?", "S98.929A", "S98.929D", "S98.929S", "S99.009?",
				"S99.009A", "S99.009B", "S99.009D", "S99.009G", "S99.009K", "S99.009P", "S99.009S", "S99.019?",
				"S99.019A", "S99.019B", "S99.019D", "S99.019G", "S99.019K", "S99.019P", "S99.019S", "S99.029?",
				"S99.029A", "S99.029B", "S99.029D", "S99.029G", "S99.029K", "S99.029P", "S99.029S", "S99.039?",
				"S99.039A", "S99.039B", "S99.039D", "S99.039G", "S99.039K", "S99.039P", "S99.039S", "S99.049?",
				"S99.049A", "S99.049B", "S99.049D", "S99.049G", "S99.049K", "S99.049P", "S99.049S", "S99.099?",
				"S99.099A", "S99.099B", "S99.099D", "S99.099G", "S99.099K", "S99.099P", "S99.099S", "S99.109?",
				"S99.109A", "S99.109B", "S99.109D", "S99.109G", "S99.109K", "S99.109P", "S99.109S", "S99.119?",
				"S99.119A", "S99.119B", "S99.119D", "S99.119G", "S99.119K", "S99.119P", "S99.119S", "S99.129?",
				"S99.129A", "S99.129B", "S99.129D", "S99.129G", "S99.129K", "S99.129P", "S99.129S", "S99.139?",
				"S99.139A", "S99.139B", "S99.139D", "S99.139G", "S99.139K", "S99.139P", "S99.139S", "S99.149?",
				"S99.149A", "S99.149B", "S99.149D", "S99.149G", "S99.149K", "S99.149P", "S99.149S", "S99.199?",
				"S99.199A", "S99.199B", "S99.199D", "S99.199G", "S99.199K", "S99.199P", "S99.199S", "S99.209?",
				"S99.209A", "S99.209B", "S99.209D", "S99.209G", "S99.209K", "S99.209P", "S99.209S", "S99.219?",
				"S99.219A", "S99.219B", "S99.219D", "S99.219G", "S99.219K", "S99.219P", "S99.219S", "S99.229?",
				"S99.229A", "S99.229B", "S99.229D", "S99.229G", "S99.229K", "S99.229P", "S99.229S", "S99.239?",
				"S99.239A", "S99.239B", "S99.239D", "S99.239G", "S99.239K", "S99.239P", "S99.239S", "S99.249?",
				"S99.249A", "S99.249B", "S99.249D", "S99.249G", "S99.249K", "S99.249P", "S99.249S", "S99.299?",
				"S99.299A", "S99.299B", "S99.299D", "S99.299G", "S99.299K", "S99.299P", "S99.299S", "S99.819?",
				"S99.819A", "S99.819D", "S99.819S", "S99.829?", "S99.829A", "S99.829D", "S99.829S", "S99.919?",
				"S99.919A", "S99.919D", "S99.919S", "S99.929?", "S99.929A", "S99.929D", "S99.929S", "T15.00X?",
				"T15.00XA", "T15.00XD", "T15.00XS", "T15.10X?", "T15.10XA", "T15.10XD", "T15.10XS", "T15.80X?",
				"T15.80XA", "T15.80XD", "T15.80XS", "T15.90X?", "T15.90XA", "T15.90XD", "T15.90XS", "T16.9XX?",
				"T16.9XXA", "T16.9XXD", "T16.9XXS", "T20.019?", "T20.019A", "T20.019D", "T20.019S", "T20.119?",
				"T20.119A", "T20.119D", "T20.119S", "T20.219?", "T20.219A", "T20.219D", "T20.219S", "T20.319?",
				"T20.319A", "T20.319D", "T20.319S", "T20.419?", "T20.419A", "T20.419D", "T20.419S", "T20.519?",
				"T20.519A", "T20.519D", "T20.519S", "T20.619?", "T20.619A", "T20.619D", "T20.619S", "T20.719?",
				"T20.719A", "T20.719D", "T20.719S", "T22.019?", "T22.019A", "T22.019D", "T22.019S", "T22.029?",
				"T22.029A", "T22.029D", "T22.029S", "T22.039?", "T22.039A", "T22.039D", "T22.039S", "T22.049?",
				"T22.049A", "T22.049D", "T22.049S", "T22.059?", "T22.059A", "T22.059D", "T22.059S", "T22.069?",
				"T22.069A", "T22.069D", "T22.069S", "T22.099?", "T22.099A", "T22.099D", "T22.099S", "T22.119?",
				"T22.119A", "T22.119D", "T22.119S", "T22.129?", "T22.129A", "T22.129D", "T22.129S", "T22.139?",
				"T22.139A", "T22.139D", "T22.139S", "T22.149?", "T22.149A", "T22.149D", "T22.149S", "T22.159?",
				"T22.159A", "T22.159D", "T22.159S", "T22.169?", "T22.169A", "T22.169D", "T22.169S", "T22.199?",
				"T22.199A", "T22.199D", "T22.199S", "T22.219?", "T22.219A", "T22.219D", "T22.219S", "T22.229?",
				"T22.229A", "T22.229D", "T22.229S", "T22.239?", "T22.239A", "T22.239D", "T22.239S", "T22.249?",
				"T22.249A", "T22.249D", "T22.249S", "T22.259?", "T22.259A", "T22.259D", "T22.259S", "T22.269?",
				"T22.269A", "T22.269D", "T22.269S", "T22.299?", "T22.299A", "T22.299D", "T22.299S", "T22.319?",
				"T22.319A", "T22.319D", "T22.319S", "T22.329?", "T22.329A", "T22.329D", "T22.329S", "T22.339?",
				"T22.339A", "T22.339D", "T22.339S", "T22.349?", "T22.349A", "T22.349D", "T22.349S", "T22.359?",
				"T22.359A", "T22.359D", "T22.359S", "T22.369?", "T22.369A", "T22.369D", "T22.369S", "T22.399?",
				"T22.399A", "T22.399D", "T22.399S", "T22.419?", "T22.419A", "T22.419D", "T22.419S", "T22.429?",
				"T22.429A", "T22.429D", "T22.429S", "T22.439?", "T22.439A", "T22.439D", "T22.439S", "T22.449?",
				"T22.449A", "T22.449D", "T22.449S", "T22.459?", "T22.459A", "T22.459D", "T22.459S", "T22.469?",
				"T22.469A", "T22.469D", "T22.469S", "T22.499?", "T22.499A", "T22.499D", "T22.499S", "T22.519?",
				"T22.519A", "T22.519D", "T22.519S", "T22.529?", "T22.529A", "T22.529D", "T22.529S", "T22.539?",
				"T22.539A", "T22.539D", "T22.539S", "T22.549?", "T22.549A", "T22.549D", "T22.549S", "T22.559?",
				"T22.559A", "T22.559D", "T22.559S", "T22.569?", "T22.569A", "T22.569D", "T22.569S", "T22.599?",
				"T22.599A", "T22.599D", "T22.599S", "T22.619?", "T22.619A", "T22.619D", "T22.619S", "T22.629?",
				"T22.629A", "T22.629D", "T22.629S", "T22.639?", "T22.639A", "T22.639D", "T22.639S", "T22.649?",
				"T22.649A", "T22.649D", "T22.649S", "T22.659?", "T22.659A", "T22.659D", "T22.659S", "T22.669?",
				"T22.669A", "T22.669D", "T22.669S", "T22.699?", "T22.699A", "T22.699D", "T22.699S", "T22.719?",
				"T22.719A", "T22.719D", "T22.719S", "T22.729?", "T22.729A", "T22.729D", "T22.729S", "T22.739?",
				"T22.739A", "T22.739D", "T22.739S", "T22.749?", "T22.749A", "T22.749D", "T22.749S", "T22.759?",
				"T22.759A", "T22.759D", "T22.759S", "T22.769?", "T22.769A", "T22.769D", "T22.769S", "T22.799?",
				"T22.799A", "T22.799D", "T22.799S", "T23.009?", "T23.009A", "T23.009D", "T23.009S", "T23.019?",
				"T23.019A", "T23.019D", "T23.019S", "T23.029?", "T23.029A", "T23.029D", "T23.029S", "T23.039?",
				"T23.039A", "T23.039D", "T23.039S", "T23.049?", "T23.049A", "T23.049D", "T23.049S", "T23.059?",
				"T23.059A", "T23.059D", "T23.059S", "T23.069?", "T23.069A", "T23.069D", "T23.069S", "T23.079?",
				"T23.079A", "T23.079D", "T23.079S", "T23.099?", "T23.099A", "T23.099D", "T23.099S", "T23.109?",
				"T23.109A", "T23.109D", "T23.109S", "T23.119?", "T23.119A", "T23.119D", "T23.119S", "T23.129?",
				"T23.129A", "T23.129D", "T23.129S", "T23.139?", "T23.139A", "T23.139D", "T23.139S", "T23.149?",
				"T23.149A", "T23.149D", "T23.149S", "T23.159?", "T23.159A", "T23.159D", "T23.159S", "T23.169?",
				"T23.169A", "T23.169D", "T23.169S", "T23.179?", "T23.179A", "T23.179D", "T23.179S", "T23.199?",
				"T23.199A", "T23.199D", "T23.199S", "T23.209?", "T23.209A", "T23.209D", "T23.209S", "T23.219?",
				"T23.219A", "T23.219D", "T23.219S", "T23.229?", "T23.229A", "T23.229D", "T23.229S", "T23.239?",
				"T23.239A", "T23.239D", "T23.239S", "T23.249?", "T23.249A", "T23.249D", "T23.249S", "T23.259?",
				"T23.259A", "T23.259D", "T23.259S", "T23.269?", "T23.269A", "T23.269D", "T23.269S", "T23.279?",
				"T23.279A", "T23.279D", "T23.279S", "T23.299?", "T23.299A", "T23.299D", "T23.299S", "T23.309?",
				"T23.309A", "T23.309D", "T23.309S", "T23.319?", "T23.319A", "T23.319D", "T23.319S", "T23.329?",
				"T23.329A", "T23.329D", "T23.329S", "T23.339?", "T23.339A", "T23.339D", "T23.339S", "T23.349?",
				"T23.349A", "T23.349D", "T23.349S", "T23.359?", "T23.359A", "T23.359D", "T23.359S", "T23.369?",
				"T23.369A", "T23.369D", "T23.369S", "T23.379?", "T23.379A", "T23.379D", "T23.379S", "T23.399?",
				"T23.399A", "T23.399D", "T23.399S", "T23.409?", "T23.409A", "T23.409D", "T23.409S", "T23.419?",
				"T23.419A", "T23.419D", "T23.419S", "T23.429?", "T23.429A", "T23.429D", "T23.429S", "T23.439?",
				"T23.439A", "T23.439D", "T23.439S", "T23.449?", "T23.449A", "T23.449D", "T23.449S", "T23.459?",
				"T23.459A", "T23.459D", "T23.459S", "T23.469?", "T23.469A", "T23.469D", "T23.469S", "T23.479?",
				"T23.479A", "T23.479D", "T23.479S", "T23.499?", "T23.499A", "T23.499D", "T23.499S", "T23.509?",
				"T23.509A", "T23.509D", "T23.509S", "T23.519?", "T23.519A", "T23.519D", "T23.519S", "T23.529?",
				"T23.529A", "T23.529D", "T23.529S", "T23.539?", "T23.539A", "T23.539D", "T23.539S", "T23.549?",
				"T23.549A", "T23.549D", "T23.549S", "T23.559?", "T23.559A", "T23.559D", "T23.559S", "T23.569?",
				"T23.569A", "T23.569D", "T23.569S", "T23.579?", "T23.579A", "T23.579D", "T23.579S", "T23.599?",
				"T23.599A", "T23.599D", "T23.599S", "T23.609?", "T23.609A", "T23.609D", "T23.609S", "T23.619?",
				"T23.619A", "T23.619D", "T23.619S", "T23.629?", "T23.629A", "T23.629D", "T23.629S", "T23.639?",
				"T23.639A", "T23.639D", "T23.639S", "T23.649?", "T23.649A", "T23.649D", "T23.649S", "T23.659?",
				"T23.659A", "T23.659D", "T23.659S", "T23.669?", "T23.669A", "T23.669D", "T23.669S", "T23.679?",
				"T23.679A", "T23.679D", "T23.679S", "T23.699?", "T23.699A", "T23.699D", "T23.699S", "T23.709?",
				"T23.709A", "T23.709D", "T23.709S", "T23.719?", "T23.719A", "T23.719D", "T23.719S", "T23.729?",
				"T23.729A", "T23.729D", "T23.729S", "T23.739?", "T23.739A", "T23.739D", "T23.739S", "T23.749?",
				"T23.749A", "T23.749D", "T23.749S", "T23.759?", "T23.759A", "T23.759D", "T23.759S", "T23.769?",
				"T23.769A", "T23.769D", "T23.769S", "T23.779?", "T23.779A", "T23.779D", "T23.779S", "T23.799?",
				"T23.799A", "T23.799D", "T23.799S", "T24.009?", "T24.009A", "T24.009D", "T24.009S", "T24.019?",
				"T24.019A", "T24.019D", "T24.019S", "T24.029?", "T24.029A", "T24.029D", "T24.029S", "T24.039?",
				"T24.039A", "T24.039D", "T24.039S", "T24.099?", "T24.099A", "T24.099D", "T24.099S", "T24.109?",
				"T24.109A", "T24.109D", "T24.109S", "T24.119?", "T24.119A", "T24.119D", "T24.119S", "T24.129?",
				"T24.129A", "T24.129D", "T24.129S", "T24.139?", "T24.139A", "T24.139D", "T24.139S", "T24.199?",
				"T24.199A", "T24.199D", "T24.199S", "T24.209?", "T24.209A", "T24.209D", "T24.209S", "T24.219?",
				"T24.219A", "T24.219D", "T24.219S", "T24.229?", "T24.229A", "T24.229D", "T24.229S", "T24.239?",
				"T24.239A", "T24.239D", "T24.239S", "T24.299?", "T24.299A", "T24.299D", "T24.299S", "T24.309?",
				"T24.309A", "T24.309D", "T24.309S", "T24.319?", "T24.319A", "T24.319D", "T24.319S", "T24.329?",
				"T24.329A", "T24.329D", "T24.329S", "T24.339?", "T24.339A", "T24.339D", "T24.339S", "T24.399?",
				"T24.399A", "T24.399D", "T24.399S", "T24.409?", "T24.409A", "T24.409D", "T24.409S", "T24.419?",
				"T24.419A", "T24.419D", "T24.419S", "T24.429?", "T24.429A", "T24.429D", "T24.429S", "T24.439?",
				"T24.439A", "T24.439D", "T24.439S", "T24.499?", "T24.499A", "T24.499D", "T24.499S", "T24.509?",
				"T24.509A", "T24.509D", "T24.509S", "T24.519?", "T24.519A", "T24.519D", "T24.519S", "T24.529?",
				"T24.529A", "T24.529D", "T24.529S", "T24.539?", "T24.539A", "T24.539D", "T24.539S", "T24.599?",
				"T24.599A", "T24.599D", "T24.599S", "T24.609?", "T24.609A", "T24.609D", "T24.609S", "T24.619?",
				"T24.619A", "T24.619D", "T24.619S", "T24.629?", "T24.629A", "T24.629D", "T24.629S", "T24.639?",
				"T24.639A", "T24.639D", "T24.639S", "T24.699?", "T24.699A", "T24.699D", "T24.699S", "T24.709?",
				"T24.709A", "T24.709D", "T24.709S", "T24.719?", "T24.719A", "T24.719D", "T24.719S", "T24.729?",
				"T24.729A", "T24.729D", "T24.729S", "T24.739?", "T24.739A", "T24.739D", "T24.739S", "T24.799?",
				"T24.799A", "T24.799D", "T24.799S", "T25.019?", "T25.019A", "T25.019D", "T25.019S", "T25.029?",
				"T25.029A", "T25.029D", "T25.029S", "T25.039?", "T25.039A", "T25.039D", "T25.039S", "T25.099?",
				"T25.099A", "T25.099D", "T25.099S", "T25.119?", "T25.119A", "T25.119D", "T25.119S", "T25.129?",
				"T25.129A", "T25.129D", "T25.129S", "T25.139?", "T25.139A", "T25.139D", "T25.139S", "T25.199?",
				"T25.199A", "T25.199D", "T25.199S", "T25.219?", "T25.219A", "T25.219D", "T25.219S", "T25.229?",
				"T25.229A", "T25.229D", "T25.229S", "T25.239?", "T25.239A", "T25.239D", "T25.239S", "T25.299?",
				"T25.299A", "T25.299D", "T25.299S", "T25.319?", "T25.319A", "T25.319D", "T25.319S", "T25.329?",
				"T25.329A", "T25.329D", "T25.329S", "T25.339?", "T25.339A", "T25.339D", "T25.339S", "T25.399?",
				"T25.399A", "T25.399D", "T25.399S", "T25.419?", "T25.419A", "T25.419D", "T25.419S", "T25.429?",
				"T25.429A", "T25.429D", "T25.429S", "T25.439?", "T25.439A", "T25.439D", "T25.439S", "T25.499?",
				"T25.499A", "T25.499D", "T25.499S", "T25.519?", "T25.519A", "T25.519D", "T25.519S", "T25.529?",
				"T25.529A", "T25.529D", "T25.529S", "T25.539?", "T25.539A", "T25.539D", "T25.539S", "T25.599?",
				"T25.599A", "T25.599D", "T25.599S", "T25.619?", "T25.619A", "T25.619D", "T25.619S", "T25.629?",
				"T25.629A", "T25.629D", "T25.629S", "T25.639?", "T25.639A", "T25.639D", "T25.639S", "T25.699?",
				"T25.699A", "T25.699D", "T25.699S", "T25.719?", "T25.719A", "T25.719D", "T25.719S", "T25.729?",
				"T25.729A", "T25.729D", "T25.729S", "T25.739?", "T25.739A", "T25.739D", "T25.739S", "T25.799?",
				"T25.799A", "T25.799D", "T25.799S", "T26.00X?", "T26.00XA", "T26.00XD", "T26.00XS", "T26.10X?",
				"T26.10XA", "T26.10XD", "T26.10XS", "T26.20X?", "T26.20XA", "T26.20XD", "T26.20XS", "T26.30X?",
				"T26.30XA", "T26.30XD", "T26.30XS", "T26.40X?", "T26.40XA", "T26.40XD", "T26.40XS", "T26.50X?",
				"T26.50XA", "T26.50XD", "T26.50XS", "T26.60X?", "T26.60XA", "T26.60XD", "T26.60XS", "T26.70X?",
				"T26.70XA", "T26.70XD", "T26.70XS", "T26.80X?", "T26.80XA", "T26.80XD", "T26.80XS", "T26.90X?",
				"T26.90XA", "T26.90XD", "T26.90XS", "T28.419?", "T28.419A", "T28.419D", "T28.419S", "T28.919?",
				"T28.919A", "T28.919D", "T28.919S", "T33.019?", "T33.019A", "T33.019D", "T33.019S", "T33.40X?",
				"T33.40XA", "T33.40XD", "T33.40XS", "T33.519?", "T33.519A", "T33.519D", "T33.519S", "T33.529?",
				"T33.529A", "T33.529D", "T33.529S", "T33.539?", "T33.539A", "T33.539D", "T33.539S", "T33.60X?",
				"T33.60XA", "T33.60XD", "T33.60XS", "T33.70X?", "T33.70XA", "T33.70XD", "T33.70XS", "T33.819?",
				"T33.819A", "T33.819D", "T33.819S", "T33.829?", "T33.829A", "T33.829D", "T33.829S", "T33.839?",
				"T33.839A", "T33.839D", "T33.839S", "T34.019?", "T34.019A", "T34.019D", "T34.019S", "T34.40X?",
				"T34.40XA", "T34.40XD", "T34.40XS", "T34.519?", "T34.519A", "T34.519D", "T34.519S", "T34.529?",
				"T34.529A", "T34.529D", "T34.529S", "T34.539?", "T34.539A", "T34.539D", "T34.539S", "T34.60X?",
				"T34.60XA", "T34.60XD", "T34.60XS", "T34.70X?", "T34.70XA", "T34.70XD", "T34.70XS", "T34.819?",
				"T34.819A", "T34.819D", "T34.819S", "T34.829?", "T34.829A", "T34.829D", "T34.829S", "T34.839?",
				"T34.839A", "T34.839D", "T34.839S", "T69.019?", "T69.019A", "T69.019D", "T69.019S", "T69.029?",
				"T69.029A", "T69.029D", "T69.029S", "T79.A19?", "T79.A19A", "T79.A19D", "T79.A19S", "T79.A29?",
				"T79.A29A", "T79.A29D", "T79.A29S", "T84.019?", "T84.019A", "T84.019D", "T84.019S", "T84.029?",
				"T84.029A", "T84.029D", "T84.029S", "T84.039?", "T84.039A", "T84.039D", "T84.039S", "T84.059?",
				"T84.059A", "T84.059D", "T84.059S", "T84.069?", "T84.069A", "T84.069D", "T84.069S", "T84.099?",
				"T84.099A", "T84.099D", "T84.099S", "T84.119?", "T84.119A", "T84.119D", "T84.119S", "T84.129?",
				"T84.129A", "T84.129D", "T84.129S", "T84.199?", "T84.199A", "T84.199D", "T84.199S", "T84.50X?",
				"T84.50XA", "T84.50XD", "T84.50XS", "T84.619?", "T84.619A", "T84.619D", "T84.619S", "T84.629?",
				"T84.629A", "T84.629D", "T84.629S", "T87.0X9", "T87.1X9", "T87.30", "T87.40", "T87.50", "Z44.009",
				"Z44.019", "Z44.029", "Z44.109", "Z44.119", "Z44.129", "Z44.20", "Z44.30", "Z45.819", "Z89.019",
				"Z89.029", "Z89.119", "Z89.129", "Z89.209", "Z89.219", "Z89.229", "Z89.239", "Z89.419", "Z89.429",
				"Z89.439", "Z89.449", "Z89.519", "Z89.529", "Z89.619", "Z89.629", "Z90.10", "Z96.619", "Z96.629",
				"Z96.639", "Z96.649", "Z96.659", "Z96.669", "Z97.10", "Z98.49" }));
	}
  
}
