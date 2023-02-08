/*
 *    Copyright 2019 West Coast Informatics, LLC
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
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.FileSorter;

/**
 * Implementation for sample allergy mapping project. Require valid codes to be
 * allergies.
 */
public class MIMSAllergyToSnomedProjectSpecificAlgorithmHandler
    extends DefaultProjectSpecificAlgorithmHandler {


  /** The auto-generated map suggestions for preloading. */
  private static Map<String, MapRecord> automaps = new HashMap<>();
  
  
  
  /* see superclass */
  @Override
  public void initialize() throws Exception {
    Logger.getLogger(getClass()).info("Running initialize for " + getClass().getSimpleName());
    // Populate any project-specific caches.
    cacheAutomaps();
  }  
  
  /* see superclass */
  @Override
  public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception {

    final ValidationResult validationResult = new ValidationResultJpa();
    final ContentService contentService = new ContentServiceJpa();

    for (final MapEntry mapEntry : mapRecord.getMapEntries()) {

      // "No target" targets are valid
      if( mapEntry.getTargetId().isBlank()) {
        continue;
      }
      
      // Target code must be an existing concept
      final Concept concept = contentService.getConcept(mapEntry.getTargetId(),
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      // Concept must exist
      if (concept == null) {
        validationResult.addError("Concept for target id " + mapEntry.getTargetId() + " does not exist.");
      }
      
      // Concept must be active
      if (concept != null && !concept.isActive()) {
        validationResult.addError("Concept for target id " + mapEntry.getTargetId() + " is not active.");
      }
    }

    contentService.close();
    return validationResult;
  }

  /* see superclass */
  @Override
  public MapRecord computeInitialMapRecord(MapRecord mapRecord) throws Exception {

    try {

      if (automaps.isEmpty()) {
        cacheAutomaps();
      }

      MapRecord existingMapRecord = automaps.get(mapRecord.getConceptId());

//      // Run existing map record through standard map advice and relation
//      // calculation
//      if (existingMapRecord != null) {
//        List<MapEntry> updatedMapEntries = new ArrayList<>();
//
//        for (MapEntry mapEntry : existingMapRecord.getMapEntries()) {
//          MapRelation mapRelation = computeMapRelation(existingMapRecord, mapEntry);
//          MapAdviceList mapAdvices = computeMapAdvice(existingMapRecord, mapEntry);
//          mapEntry.setMapRelation(mapRelation);
//          mapEntry.getMapAdvices().addAll(mapAdvices.getMapAdvices());
//          updatedMapEntries.add(mapEntry);
//        }
//
//        existingMapRecord.setMapEntries(updatedMapEntries);
//      }

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
    //Only concepts from three hierarchies are valid targets:
    // (substance)
    // (organism)
    // (physical object)
    
    final List<String> validSemanticTagsList = new ArrayList<>();
    validSemanticTagsList.add("(substance)");
    validSemanticTagsList.add("(organism)");
    validSemanticTagsList.add("(physical object)");
    
    // "No target" targets are valid
    if(terminologyId.isBlank()) {
      return true;
    }
    
    final ContentService contentService = new ContentServiceJpa();

    try {
      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getDestinationTerminology(),
          mapProject.getDestinationTerminologyVersion());

      // Concept must exist
      if (concept == null) {
        return false;
      }
      
      // Concept must be active
      if (!concept.isActive()) {
        return false;
      }      
      
      // Only concepts that end with one of the valid semantic tags are valid
      for (String validSemanticTag : validSemanticTagsList) {
        if (concept.getDefaultPreferredName().endsWith(validSemanticTag)) {
          return true;
        }
      }
      
      return false;

    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
  }

  /* see superclass */
  @Override
  public ValidationResult validateSemanticChecks(MapRecord mapRecord)
    throws Exception {
    final ValidationResult result = new ValidationResultJpa();
    
    // Map record name must share at least one word with target name
    final Set<String> recordWords = new HashSet<>(
        Arrays.asList(mapRecord.getConceptName().toLowerCase().split(" ")));
    final Set<String> entryWords = new HashSet<>();
    for (final MapEntry entry : mapRecord.getMapEntries()) {
      if (entry.getTargetName() != null) {
        entryWords.addAll(
            Arrays.asList(entry.getTargetName().toLowerCase().split(" ")));
      }
    }
    final Set<String> recordMinusEntry = new HashSet<>(recordWords);
    recordMinusEntry.removeAll(entryWords);

    // All populated entries must have a map relation
    for (final MapEntry entry : mapRecord.getMapEntries()) {
      if(entry.getTargetName() != null && !entry.getTargetName().equals("No target") && entry.getMapRelation() == null) {
        result.addError("Required map relation missing for target=" + entry.getTargetId());
      }
    }   
    
    // If there are entry words and none match, warning
    // if (entryWords.size() > 0 && recordWords.size() ==
    // recordMinusEntry.size()) {
    // result
    // .addWarning("From concept and target code names must share at least one
    // word.");
    // }

    return result;
  }
  
  /**
   * Cache existing maps.
   *
   * @throws Exception the exception
   */
  private void cacheAutomaps() throws Exception {
    // Lookup if this concept has an existing, auto-generated map record to pre-load
    // Generated automap file must be saved here:
    // {data.dir}/MIMS-Allergy/automap/results.xls

    final ContentService contentService = new ContentServiceJpa();

    Logger.getLogger(MIMSAllergyToSnomedProjectSpecificAlgorithmHandler.class)
        .info("Caching the existing automaps");

    final String dataDir = ConfigUtility.getConfigProperties().getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Check preconditions
    String inputFile =
        dataDir + "/MIMS-Allergy/automap/results.xls";

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

    // Open reader and service
    BufferedReader preloadMapReader = new BufferedReader(new FileReader(inputFile));

    String line = null;

    while ((line = preloadMapReader.readLine()) != null) {
      String fields[] = line.split("\t");

      final String conceptId = fields[0];

      // The first time a conceptId is encountered, set up the map (only need
      // very limited information for the purpose of this function
      if (automaps.get(conceptId) == null) {
        MapRecord mimsAllergyAutomapRecord = new MapRecordJpa();
        mimsAllergyAutomapRecord.setConceptId(conceptId);
        String sourceConceptName = sourceIdToName.get(conceptId);
        if (sourceConceptName != null) {
          mimsAllergyAutomapRecord.setConceptName(sourceConceptName);
        } else {
          mimsAllergyAutomapRecord
              .setConceptName("CONCEPT DOES NOT EXIST IN " + mapProject.getSourceTerminology());
        }

        automaps.put(conceptId, mimsAllergyAutomapRecord);
      }
            
      for (int i = 2; i < fields.length; i += 2) {
        
        if(fields[i] == null || fields[i].isBlank()) {
          continue;
        }
        
        MapRecord mimsAllergyAutomapRecord = automaps.get(conceptId);      
        final String mapTarget = fields[i];
        
        // For each suggested map target, check if it has already been added to map.
        Boolean targetAlreadyAdded = false;
        for(MapEntry existingMapEntry : mimsAllergyAutomapRecord.getMapEntries()) {
          if(existingMapEntry.getTargetId().equals(mapTarget)) {
            targetAlreadyAdded = true;
          }
        }
        
        // Don't add a target twice
        if(targetAlreadyAdded) {
          continue;
        }
        
        // If this is a new suggested target, create a map entry, and attach it to the record
        MapEntry mapEntry = new MapEntryJpa();
        mapEntry.setMapGroup(1);
        mapEntry.setMapPriority(mimsAllergyAutomapRecord.getMapEntries().size() + 1);
        mapEntry.setTargetId(mapTarget);
        String targetConceptName = destinationIdToName.get(mapTarget);

        if (targetConceptName != null) {
          mapEntry.setTargetName(targetConceptName);
        } else {
          mapEntry
              .setTargetName("CONCEPT DOES NOT EXIST IN " + mapProject.getDestinationTerminology());
        }


        // Add the entry to the record, and put the updated record in the map
        mimsAllergyAutomapRecord.addMapEntry(mapEntry);
        automaps.put(conceptId, mimsAllergyAutomapRecord);
        
      }
    }


    Logger.getLogger(getClass()).info("Done caching maps");

    preloadMapReader.close();
    contentService.close();

  }

  
  /**
   * Overriding defaultChecks, because there are some MIMS-specific settings that 
   * don't conform to the standard map requirements.
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

    
    // For the MIMS project, we are allowing multiple entries without rules.
    // This is acceptable because their desired final release format is not
    // intended to follow strict RF2 guidelines.
//    // FATAL ERROR: multiple entries in groups for non-rule based
//    if (!mapProject.isRuleBased()) {
//      for (Integer key : entryGroups.keySet()) {
//        if (entryGroups.get(key).size() > 1) {
//          validationResult.addError(
//              "Project has no rule structure but multiple map entries found in group " + key);
//        }
//      }
//      if (!validationResult.isValid()) {
//        return validationResult;
//      }
//    }
    
    // Verify that groups begin at index 1 and are sequential (i.e. no empty
    // groups)
    validationResult.merge(checkMapRecordGroupStructure(mapRecord, entryGroups));

    // Validation Check: verify correct positioning of TRUE rules
    validationResult.merge(checkMapRecordRules(mapRecord, entryGroups));

    // Validation Check: very higher map groups do not have only NC nodes
    validationResult.merge(checkMapRecordNcNodes(mapRecord, entryGroups));

    // Validation Check: verify entries are not duplicated
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

  
  

}