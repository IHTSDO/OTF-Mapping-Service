package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * The Class ICD10ProjectSpecificAlgorithmHandler.
 */
public class ICD10ProjectSpecificAlgorithmHandler extends
    DefaultProjectSpecificAlgorithmHandler {

  /**
   * For ICD10, a target code is valid if: - Concept exists - Concept has at
   * least 3 characters - The second character is a number (e.g. XVII is
   * invalid, but B10 is) - Concept does not contain a dash (-) character
   * 
   * @param mapRecord
   * @return the validation result
   * @throws Exception
   */
  @Override
  public ValidationResult validateTargetCodes(MapRecord mapRecord)
    throws Exception {

    Logger.getLogger(ICD10ProjectSpecificAlgorithmHandler.class).info(
        "Validating target codes");

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
      } else if (mapEntry.getTargetId() != null && !mapEntry.getTargetId().equals("")) {

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
   * Computes the map relation for the SNOMEDCT to ICD10 map project. Based solely
   * on whether an entry has a TRUE rule or not. No advices are computed for
   * this project.
   * @throws Exception
   */
  @Override
  public MapRelation computeMapRelation(MapRecord mapRecord, MapEntry mapEntry)
    throws Exception {

    
    // if entry has no target
    if (mapEntry.getTargetId() == null || mapEntry.getTargetId().isEmpty()) {

      // if a relation is already set, and is allowable for null target,
      // keep it
      if (mapEntry.getMapRelation() != null
          && mapEntry.getMapRelation().isAllowableForNullTarget() == true)
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
      throw new IllegalStateException(
          "Map project source terminology has too many hierarchical relationship types - "
              + mapProject.getDestinationTerminology());
    }
    if (hierarchicalRelationshipTypeMap.keySet().size() < 1) {
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

    // System.out.println("computed advices: ");
    // for (MapAdvice advice : advices) {
    // System.out.println("  " + advice.getName());
    // }

    MapAdviceList mapAdviceList = new MapAdviceListJpa();
    mapAdviceList.setMapAdvices(advices);
    return mapAdviceList;
  }

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

    // open the content service
    ContentService contentService = new ContentServiceJpa();

    if (terminologyId.matches(".[0-9].")) {

      if (terminologyId.toUpperCase().matches("W..|X..|Y[0-2].|Y3[0-4]"))
        return true;

      /*
       * // Fourth digit not necessary: check W00-W19 if
       * (terminologyId.startsWith("W")) return true;
       * 
       * // Fourth digit not necessary: check X00-X09 if
       * (terminologyId.startsWith("X")) return true;
       * 
       * // Fourth digit not necessary: check Y10-Y34 if
       * (terminologyId.startsWith("Y")) { if (terminologyId.charAt(1) == '1' ||
       * terminologyId.charAt(1) == '2') return true; if
       * (terminologyId.charAt(1) == '3' && (terminology.)) return true;
       */
      // otherwise, if 3-digit code has children, return false
      TreePositionList tpList =
          contentService.getTreePositions(terminologyId,
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());
      if (tpList.getCount() == 0)
        return false;

      if (tpList.getTreePositions().get(0).getChildrenCount() > 0) {
        contentService.close();
        return false;
      }
    }

    // third, verify concept exists in database
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

  @Override
  public Set<String> getDependentModules() {
    
    Set<String> moduleDependencies = new HashSet<>();
    
    moduleDependencies.add("900000000000012004");
    moduleDependencies.add("900000000000207008");
    
    System.out.println("NUMBER OF DEPENDENCIES: " + moduleDependencies.size());
    
    return moduleDependencies;
    
  }
  
  @Override
  public String getModuleDependencyRefSetId() {
    return "900000000000534007";
  }
}
