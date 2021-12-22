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

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Implementation for MIMS condition mapping project.
 */
public class MIMSConditionToSnomedProjectSpecificAlgorithmHandler
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

    // No current validation restrictions

    final ValidationResult validationResult = new ValidationResultJpa();

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

      // Maps with too many suggestions are unuseful.
      // Per MIMS' request, if >10 suggestions, don't return anything.

      if (existingMapRecord != null && existingMapRecord.getMapEntries() != null
          && existingMapRecord.getMapEntries().size() > 10) {
        existingMapRecord = null;
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
    // Only concepts from three hierarchies are valid targets:
    // (substance)
    // (organism)
    // (physical object)

    final List<String> validSemanticTagsList = new ArrayList<>();
    validSemanticTagsList.add("(finding)");
    validSemanticTagsList.add("(disorder)");
    validSemanticTagsList.add("(event)");
    validSemanticTagsList.add("(situation)");

    final ContentService contentService = new ContentServiceJpa();

    try {
      // Concept must exist
      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      // Only concepts that end with one of the
      if (concept != null) {
        for (String validSemanticTag : validSemanticTagsList) {
          if (concept.getDefaultPreferredName().endsWith(validSemanticTag)) {
            return true;
          }
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
  public ValidationResult validateSemanticChecks(MapRecord mapRecord) throws Exception {
    final ValidationResult result = new ValidationResultJpa();

    // Map record name must share at least one word with target name
    final Set<String> recordWords =
        new HashSet<>(Arrays.asList(mapRecord.getConceptName().toLowerCase().split(" ")));
    final Set<String> entryWords = new HashSet<>();
    for (final MapEntry entry : mapRecord.getMapEntries()) {
      if (entry.getTargetName() != null) {
        entryWords.addAll(Arrays.asList(entry.getTargetName().toLowerCase().split(" ")));
      }
    }
    final Set<String> recordMinusEntry = new HashSet<>(recordWords);
    recordMinusEntry.removeAll(entryWords);

    // All populated entries must have a map relation
    for (final MapEntry entry : mapRecord.getMapEntries()) {
      if (entry.getTargetName() != null && !entry.getTargetName().equals("No target")
          && entry.getMapRelation() == null) {
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
    // Use the reverse maps from international SNOMED to ICD10 to auto-populate
    // suggestions.
    // Map is mims code -> map containing all SNOMED concepts that map to the
    // icd10 suffix of the mims code.
    // {data.dir}/MIMS-Condition/automap/mims-snomed-map.txt

    final ContentService contentService = new ContentServiceJpa();

    Logger.getLogger(MIMSConditionToSnomedProjectSpecificAlgorithmHandler.class)
        .info("Caching the existing automaps");

    final String dataDir = ConfigUtility.getConfigProperties().getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Check preconditions
    String inputFile = dataDir + "/MIMS-Condition/automap/mims-snomed-map.txt";

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

      final String mimsCodeId = fields[0];

      // The first time a conceptId is encountered, set up the map (only need
      // very limited information for the purpose of this function
      if (automaps.get(mimsCodeId) == null) {
        MapRecord mimsConditionAutomapRecord = new MapRecordJpa();
        mimsConditionAutomapRecord.setConceptId(mimsCodeId);
        String sourceConceptName = sourceIdToName.get(mimsCodeId);
        if (sourceConceptName != null) {
          mimsConditionAutomapRecord.setConceptName(sourceConceptName);
        } else {
          mimsConditionAutomapRecord
              .setConceptName("CONCEPT DOES NOT EXIST IN " + mapProject.getSourceTerminology());
        }

        automaps.put(mimsCodeId, mimsConditionAutomapRecord);
      }

      final String snomedId = fields[1];

      if (snomedId == null || snomedId.isBlank()) {
        continue;
      }

      MapRecord mimsConditionAutomapRecord = automaps.get(mimsCodeId);
      final String mapTarget = snomedId;

      // For each suggested map target, check if it has already been added to
      // map.
      Boolean targetAlreadyAdded = false;
      for (MapEntry existingMapEntry : mimsConditionAutomapRecord.getMapEntries()) {
        if (existingMapEntry.getTargetId().equals(mapTarget)) {
          targetAlreadyAdded = true;
        }
      }

      // Don't add a target twice
      if (targetAlreadyAdded) {
        continue;
      }

      // If this is a new suggested target, create a map entry, and attach it to
      // the record
      MapEntry mapEntry = new MapEntryJpa();
      mapEntry.setMapGroup(1);
      mapEntry.setMapPriority(mimsConditionAutomapRecord.getMapEntries().size() + 1);
      mapEntry.setTargetId(mapTarget);
      String targetConceptName = destinationIdToName.get(mapTarget);

      if (targetConceptName != null) {
        mapEntry.setTargetName(targetConceptName);
      } else {
        mapEntry
            .setTargetName("CONCEPT DOES NOT EXIST IN " + mapProject.getDestinationTerminology());
      }

      // Add the entry to the record, and put the updated record in the map
      mimsConditionAutomapRecord.addMapEntry(mapEntry);
      automaps.put(mimsCodeId, mimsConditionAutomapRecord);

    }

    Logger.getLogger(getClass()).info("Done caching maps");

    preloadMapReader.close();
    contentService.close();

  }

  /**
   * Overriding defaultChecks, because there are some MIMS-specific settings
   * that don't conform to the standard map requirements.
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