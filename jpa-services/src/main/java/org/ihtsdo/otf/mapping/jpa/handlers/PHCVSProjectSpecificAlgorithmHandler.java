/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
 * Implementation for sample allergy mapping project. Require valid codes to be
 * allergies.
 */
public class PHCVSProjectSpecificAlgorithmHandler extends DefaultProjectSpecificAlgorithmHandler {

  private static Set<String> ICD10CACodeSet = new HashSet<>();

  /** The CED-DxS codes. */
  private static Set<String> CEDDxSCodeSet = new HashSet<>();

  private static Set<String> ICD9CodeSet = new HashSet<>();

  /** The PHCVS maps for preloading. */
  private static Map<String, MapRecord> existingPHCVSMaps = new HashMap<>();

  /* see superclass */
  @Override
  public void initialize() throws Exception {
    Logger.getLogger(getClass()).info("Running initialize for " + getClass().getSimpleName());
    super.initialize();
    cacheCodes();
    cacheExistingMaps();
  }

  /* see superclass */
  @Override
  public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception {

    final ValidationResult validationResult = new ValidationResultJpa();

    // Every map must follow this convention:
    // 1st group must be from ICD10CA (terminology Id doesn't end with a hyphen)
    // 2nd group must be from ICD9 (terminology Id ends with a space)
    // 3rd group must be from CedDXS (part of the CEDDxSCodesSet)

    int maxMapGroup = 0;

    for (final MapEntry entry : mapRecord.getMapEntries()) {
      if (entry.getMapGroup() > maxMapGroup) {
        maxMapGroup = entry.getMapGroup();
      }

      if (entry.getMapGroup() == 1 && !ICD10CACodeSet.contains(entry.getTargetId())) {
        validationResult.addError("1st group must be a ICD10CA code.");
      } else if (entry.getMapGroup() == 2 && !ICD9CodeSet.contains(entry.getTargetId())) {
        validationResult.addError("2nd group must be a ICD9 code.");
      } else if (entry.getMapGroup() == 3 && !CEDDxSCodeSet.contains(entry.getTargetId())) {
        validationResult.addError("3rd group must be a CedDXS code.");
      } else if (entry.getMapGroup() > 3) {
        validationResult.addError(
            "Maps must contain only 3 group - " + entry.getMapGroup() + " group identified.");
      } else {
        // Shouldn't be possible to get here.
      }
    }

    if (maxMapGroup < 3) {
      validationResult
          .addError("Maps must contain 3 groups - only " + maxMapGroup + " groups identified.");
    }

    return validationResult;
  }

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {

    final ContentService contentService = new ContentServiceJpa();

    try {
      // Concept must exist
      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      if (concept != null) {
        return true;
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
   * Overriding defaultChecks, because there are some project-specific settings
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

    // For this project, we are allowing multiple entries without rules.
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

  /* see superclass */
  @Override
  public MapRecord computeInitialMapRecord(MapRecord mapRecord) throws Exception {

    try {

      if (existingPHCVSMaps.isEmpty()) {
        cacheExistingMaps();
      }

      MapRecord existingMapRecord = existingPHCVSMaps.get(mapRecord.getConceptId());

      return existingMapRecord;
    } catch (Exception e) {
      throw e;
    } finally {
      // n/a
    }
  }

  /**
   * Cache the CED-DxS codes.
   *
   * @throws Exception the exception
   */

  private void cacheCodes() throws Exception {

    Logger.getLogger(PHCVSProjectSpecificAlgorithmHandler.class)
        .info("Caching the terminology codes");

    if (ICD10CACodeSet.isEmpty() || ICD9CodeSet.isEmpty() || CEDDxSCodeSet.isEmpty()) {

      ICD10CACodeSet.clear();
      ICD9CodeSet.clear();
      CEDDxSCodeSet.clear();

      final ContentServiceJpa contentService = new ContentServiceJpa();
      ConceptList destinationConcepts = contentService.getAllConcepts(
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      for (final Concept concept : destinationConcepts.getConcepts()) {
        if (concept.getTerminologyId().endsWith("-")) {
          ICD9CodeSet.add(concept.getTerminologyId());
        } else {
          ICD10CACodeSet.add(concept.getTerminologyId());
        }
      }
      
      Set<String> codes = contentService.findDescendantConceptIds("CEDDxS", mapProject.getDestinationTerminology(),
				mapProject.getDestinationTerminologyVersion(), null);
      // Add the CEDDxS codes   
      CEDDxSCodeSet.addAll(codes);
      
      
      Logger.getLogger(PHCVSProjectSpecificAlgorithmHandler.class).info("Done caching codes for " + mapProject.getId());
      
      contentService.close();
    }
  }

  /**
   * Cache existing maps.
   *
   * @throws Exception the exception
   */
  private void cacheExistingMaps() throws Exception {
    // Cache existing PHCVS map records to pre-populate the maps
    // Up to date file, saved as tab-delimited txt must be saved here:
    // {data.dir}/doc/{projectNumber}/preloadMaps/PHCVS_maps.txt
    //
    // Format is:
    // SNOMED ID SNOMED Term ICD-10CA Code ICD-10CA Term ICD-9 CA ICD-9 Term
    // CedDxs (ICD-10CA) Code CedDxs (ICD-10CA) Term CedDxs CIHI Common Term
    // 63650001 Cholera (disorder) A009 Cholera, unspecified 0019 Cholera
    // unspecified A059 Bacterial foodborne intoxication, unspecified Bacterial
    // foodborne intox
    // 4834000 Typhoid fever (disorder) A010 Typhoid fever 0020 Typhoid Fever
    // A059 Bacterial foodborne intoxication, unspecified Bacterial foodborne
    // intox

    if (!existingPHCVSMaps.isEmpty()) {
      return;
    }
    
    final ContentService contentService = new ContentServiceJpa();

    Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
        .info("Caching the existing PHCVS maps");

    final String dataDir = ConfigUtility.getConfigProperties().getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Check preconditions
    String inputFile = dataDir + "/doc/" + mapProject.getId() + "/preloadMaps/PHCVS_maps.txt";

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
      if (existingPHCVSMaps.get(conceptId) == null) {
        MapRecord PHCVSMapRecord = new MapRecordJpa();
        PHCVSMapRecord.setConceptId(conceptId);
        String sourceConceptName = sourceIdToName.get(conceptId);
        if (sourceConceptName != null) {
          PHCVSMapRecord.setConceptName(sourceConceptName);
        } else {
          PHCVSMapRecord
              .setConceptName("CONCEPT DOES NOT EXIST IN " + mapProject.getSourceTerminology());
        }

        existingPHCVSMaps.put(conceptId, PHCVSMapRecord);
      }
      MapRecord PHCVSMapRecord = existingPHCVSMaps.get(conceptId);

      // For each line, create three map entries (one for ICD10CA, one for ICD9,
      // one for CedDxs),
      // and attach it to the record
      MapEntry icd10CAmapEntry = createMapEntry(fields[2], 1, 1, destinationIdToName);

      // Some of the provided ICD9 codes were actually from ICD9CM - need to
      // truncate
      // 00845 -> 0084
      String icd9Target = "";
      try {
        icd9Target = fields[4];
      } catch (Exception e) {
        System.out.println("No index[4] for line " + line);
      }
      if (icd9Target.length() > 4) {
        icd9Target = icd9Target.substring(0, 4);
      }
      MapEntry icd9mapEntry = createMapEntry(icd9Target, 2, 1, destinationIdToName);

      String cedDexCode = "";
      try {
        cedDexCode = fields[6];
      } catch (Exception e) {
        System.out.println("No index[6] for line " + line);
      }

      MapEntry cedDxsmapEntry =
          createMapEntry(cedDexCode.equals("") ? null : cedDexCode, 3, 1, destinationIdToName);

      // Add the entries to the record, and put the updated record in the map
      PHCVSMapRecord.addMapEntry(icd10CAmapEntry);
      PHCVSMapRecord.addMapEntry(icd9mapEntry);
      PHCVSMapRecord.addMapEntry(cedDxsmapEntry);
      existingPHCVSMaps.put(conceptId, PHCVSMapRecord);

    }

    Logger.getLogger(getClass()).info("Done caching maps");

    preloadMapReader.close();
    contentService.close();

  }

  private MapEntry createMapEntry(String targetId, int mapGroup, int mapPriority,
    Map<String, String> destinationIdToName) throws Exception {
    MapEntry mapEntry = new MapEntryJpa();

    // For all three sources, need to add a decimal after the third character
    String formattedTargetId;
    if (targetId != null && targetId.length() > 3) {
      formattedTargetId = targetId.substring(0, 3).concat(".").concat(targetId.substring(3));
    } else {
      formattedTargetId = targetId;
    }

    // For ICD-9 codes, add a hyphen to the end, so it will match how the code
    // is represented in the terminology
    if (mapGroup == 2) {
      formattedTargetId = formattedTargetId + "-";
    }

    mapEntry.setTargetId(formattedTargetId);
    mapEntry.setMapGroup(mapGroup);
    mapEntry.setMapPriority(mapPriority);
    String targetConceptName =
        (formattedTargetId == null) ? "No target" : destinationIdToName.get(formattedTargetId);

    if (targetConceptName != null) {
      mapEntry.setTargetName(targetConceptName);
    } else {
      mapEntry.setTargetName("CONCEPT DOES NOT EXIST IN " + mapProject.getDestinationTerminology());
    }

    return mapEntry;
  }

}