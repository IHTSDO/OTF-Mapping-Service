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
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.FileSorter;

/**
 * Implementation for sample allergy mapping project. Require valid codes to be
 * allergies.
 */
public class PHCVSProjectSpecificAlgorithmHandler extends DefaultProjectSpecificAlgorithmHandler {

  private static Set<String> ICD10CACodeSet = new HashSet<>();
  
  /** The CED-DxS codes. */
  private static Set<String> CEDDxSCodeSet = new HashSet<>();
  
  private static Set<String> ICD9CodeSet = new HashSet<>();

  /** The BC HCVS maps for preloading. */
  private static Map<String, MapRecord> existingPHCVSMaps = new HashMap<>();
  
  /* see superclass */
  @Override
  public void initialize() throws Exception {
    Logger.getLogger(getClass()).info("Running initialize for " + getClass().getSimpleName());
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
      if(entry.getMapGroup() > maxMapGroup) {
        maxMapGroup = entry.getMapGroup();
      }
      
      if (entry.getMapGroup() == 1 && !ICD10CACodeSet.contains(entry.getTargetId())) {
        validationResult.addError("1st group must be a ICD10CA code.");
      } else if (entry.getMapGroup() == 2 && !ICD9CodeSet.contains(entry.getTargetId())) {
        validationResult.addError("2nd group must be a ICD9 code.");
      } else if (entry.getMapGroup() == 3 && !CEDDxSCodeSet.contains(entry.getTargetId())) {
        validationResult.addError("3rd group must be a CedDXS code.");
      } else if (entry.getMapGroup() > 3) {
        validationResult.addError("Maps must contain only 3 group - " + entry.getMapGroup()
            + " group identified.");
      } else {
        // Shouldn't be possible to get here.
      }
    }
    
    if(maxMapGroup < 3) {
      validationResult.addError("Maps must contain 3 groups - only " + maxMapGroup
      + " groups identified.");
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


    Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
    .info("Caching the terminology codes");
    
    
    if(ICD10CACodeSet.isEmpty() || ICD9CodeSet.isEmpty() || CEDDxSCodeSet.isEmpty()) {
      
      ICD10CACodeSet.clear();
      ICD9CodeSet.clear();
      CEDDxSCodeSet.clear();
      
      final ContentServiceJpa contentService = new ContentServiceJpa();
      ConceptList destinationConcepts = contentService.getAllConcepts(mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

      for (final Concept concept : destinationConcepts.getConcepts()) {
        if(concept.getTerminologyId().endsWith("-")) {
          ICD9CodeSet.add(concept.getTerminologyId());
        }
        else {
          ICD10CACodeSet.add(concept.getTerminologyId());
        }
      }    
      
      CEDDxSCodeSet.addAll(Arrays.asList(new String[] {
          "A04.7", "A05.1", "A05.9", "A09.9", "A16.91", "A35", "A37.9", "A38", "A39.0", "A39.2",
          "A41.9", "A46", "A48.0", "A48.1", "A48.3", "A49.9", "A54.9", "A63.0", "A64", "A69.2",
          "A86", "A87.9", "B00.9", "B01.9", "B02.9", "B05.9", "B06.9", "B08.3", "B08.4", "B09",
          "B19.9", "B24", "B26.9", "B27.9", "B34.9", "B37.9", "B49", "B54", "B58.9", "B83.9",
          "B85.2", "B86", "B89", "C18.9", "C25.9", "C34.99", "C44.9", "C50.99", "C57.9", "C61",
          "C62.99", "C71.9", "C76.0", "C76.2", "C76.3", "C76.7", "C90.0", "C95.9", "C96.9", "D36.9",
          "D48.9", "D57.0", "D64.9", "D65", "D68.9", "D69.38", "D69.6", "D70.0", "D75.9", "D89.9",
          "E03.9", "E05.9", "E06.9", "E10.0", "E10.10", "E10.63", "E10.9", "E11.0", "E11.63",
          "E11.9", "E14.9", "E16.2", "E21.5", "E22.2", "E23.2", "E23.7", "E27.2", "E28.9", "E34.9",
          "E63.9", "E83.5", "E84.9", "E86.0", "E87.0", "E87.1", "E87.5", "E87.6", "E87.7", "E87.8",
          "E88.9", "F03", "F05.9", "F07.2", "F10.0", "F10.3", "F11.9", "F12.90", "F13.9", "F14.9",
          "F15.99", "F16.9", "F18.9", "F19.998", "F20.9", "F23.9", "F31.9", "F32.9", "F41.9",
          "F48.9", "F50.9", "F60.9", "F99", "G00.9", "G03.9", "G04.9", "G06.0", "G06.1", "G06.2",
          "G20", "G21.0", "G24.9", "G25.9", "G35", "G37.9", "G40.90", "G41.9", "G43.9", "G44.8",
          "G45.4", "G45.9", "G50.0", "G51.0", "G52.9", "G53.0", "G56.0", "G61.0", "G62.9", "G70.0",
          "G72.9", "G83.4", "G83.9", "G91.9", "G93.4", "G95.9", "G96.09", "G96.9", "H02.9", "H10.9",
          "H11.3", "H16.0", "H16.9", "H18.9", "H20.9", "H21.0", "H33.5", "H35.9", "H40.9", "H43.1",
          "H43.9", "H44.9", "H46", "H53.2", "H53.9", "H57.1", "H57.9", "H60.9", "H61.2", "H66.9",
          "H70.9", "H72.9", "H81.0", "H81.1", "H81.3", "H81.4", "H91.9", "H92.0", "H93.1", "H93.9",
          "I09.9", "I10.0", "I10.1", "I20.0", "I20.9", "I21.9", "I24.1", "I26.9", "I27.0", "I30.9",
          "I31.3", "I33.9", "I40.9", "I42.9", "I44.1", "I44.2", "I46.9", "I47.1", "I47.2", "I48.90",
          "I48.91", "I49.5", "I49.9", "I50.0", "I60.9", "I61.9", "I62.0", "I64", "I67.4", "I67.9",
          "I71.0", "I71.9", "I72.8", "I73.9", "I74.9", "I77.9", "I80.0", "I80.9", "I83.9", "I85.0",
          "I88.9", "I89.1", "I95.9", "I99", "J01.9", "J02.9", "J03.9", "J04.0", "J04.1", "J04.2",
          "J05.0", "J05.1", "J06.9", "J11.8", "J18.9", "J20.9", "J21.9", "J32.9", "J36", "J38.7",
          "J39.0", "J43.9", "J44.0", "J44.1", "J45.90", "J47", "J68.9", "J69.0", "J80", "J90",
          "J93.9", "J94.9", "J95.08", "J96.09", "J96.19", "J98.5", "J98.9", "K02.9", "K04.7",
          "K07.69", "K08.87", "K08.9", "K11.9", "K13.7", "K14.9", "K20.9", "K21.9", "K22.2",
          "K22.3", "K22.9", "K27.9", "K29.9", "K30", "K31.9", "K35.8", "K40.9", "K42.9", "K46.9",
          "K50.9", "K51.9", "K52.9", "K55.9", "K56.1", "K56.2", "K56.6", "K56.7", "K57.8", "K57.9",
          "K59.0", "K59.9", "K60.0", "K60.3", "K61.2", "K61.3", "K62.3", "K62.9", "K63.1", "K63.9",
          "K64.9", "K65.0", "K70.9", "K72.9", "K73.9", "K74.6", "K76.6", "K76.9", "K80.50",
          "K80.80", "K81.0", "K83.08", "K83.9", "K85.9", "K86.9", "K90.9", "K91.9", "K92.2",
          "K92.81", "K92.9", "L00", "L01.0", "L02.9", "L03.00", "L03.01", "L03.9", "L05.0", "L13.9",
          "L21.1", "L22", "L25.9", "L29.9", "L30.9", "L40.9", "L42", "L50.9", "L51.9", "L52",
          "L60.0", "L60.9", "L73.9", "L84", "L89.9", "L98.9", "M00.99", "M06.9", "M08.9", "M10.99",
          "M11.99", "M13.99", "M22.9", "M23.9", "M25.09", "M25.49", "M25.59", "M25.99", "M30.0",
          "M30.3", "M32.9", "M34.9", "M35.3", "M35.9", "M43.6", "M45", "M46.49", "M47.99", "M48.09",
          "M48.99", "M50.9", "M54.2", "M54.3", "M54.5", "M62.69", "M62.99", "M65.99", "M66.59",
          "M70.2", "M70.4", "M70.6", "M71.2", "M71.9", "M72.2", "M72.69", "M75.1", "M75.2", "M75.4",
          "M75.5", "M75.9", "M76.5", "M76.6", "M77.9", "M79.19", "M79.29", "M79.69", "M81.9",
          "M84.29", "M86.99", "M88.9", "M89.99", "M94.0", "M94.99", "N00.9", "N04.9", "N10", "N12",
          "N13.9", "N17.9", "N18.9", "N20.9", "N23", "N28.9", "N34.1", "N39.0", "N39.9", "N41.0",
          "N43.3", "N44.08", "N45.92", "N47.8", "N48.3", "N48.9", "N50.9", "N63", "N64.9", "N73.9",
          "N75.1", "N76.0", "N80.9", "N81.9", "N83.2", "N83.50", "N89.9", "N92.6", "N93.9", "N94.6",
          "O00.9", "O02.1", "O03.4", "O03.9", "O08.99", "O15.003", "O20.009", "O21.909", "O36.999",
          "O47.903", "O60.101", "O75.909", "O99.809", "P22.9", "P36.9", "P38", "P59.9", "P61.9",
          "P76.9", "P77", "P78.9", "P90", "P92.9", "P95", "P96.9", "Q40.0", "Q43.0", "Q43.1",
          "Q89.9", "Q99.9", "R00.0", "R00.1", "R00.2", "R04.0", "R04.2", "R05", "R06.0", "R06.1",
          "R06.4", "R06.6", "R07.4", "R09.2", "R10.0", "R10.2", "R10.4", "R11.1", "R11.3", "R13.8",
          "R17.0", "R18", "R20.8", "R21", "R22.9", "R25.8", "R26.88", "R30.0", "R31.8", "R33",
          "R36", "R39.8", "R40.0", "R40.29", "R41.0", "R42", "R44.3", "R45.8", "R50.9", "R51",
          "R53", "R55", "R56.09", "R56.88", "R57.0", "R57.1", "R57.9", "R58", "R59.9", "R60.0",
          "R60.1", "R63.0", "R64", "R69", "R79.9", "R93.8", "R94.8", "R95", "R99", "S00.2", "S00.9",
          "S01.00", "S01.01", "S01.10", "S01.11", "S01.30", "S01.31", "S01.50", "S01.51", "S01.90",
          "S01.91", "S02.100", "S02.101", "S02.200", "S02.201", "S02.300", "S02.301", "S02.5",
          "S02.600", "S02.601", "S02.900", "S02.901", "S03.0", "S04.98", "S05.0", "S05.9", "S06.0",
          "S06.25", "S06.26", "S06.35", "S06.36", "S06.4", "S06.5", "S06.6", "S08.1", "S09.2",
          "S10.9", "S11.90", "S11.91", "S12.900", "S12.901", "S13.1", "S13.6", "S15.9", "S20.8",
          "S21.90", "S21.91", "S22.090", "S22.091", "S22.200", "S22.201", "S22.300", "S22.301",
          "S22.490", "S22.491", "S22.500", "S22.501", "S23.1", "S25.9", "S26.800", "S26.801",
          "S27.200", "S27.201", "S27.300", "S27.301", "S27.390", "S27.391", "S27.900", "S27.901",
          "S28.0", "S30.80", "S30.81", "S30.85", "S31.000", "S31.001", "S31.190", "S31.191",
          "S31.500", "S31.501", "S32.090", "S32.091", "S32.100", "S32.101", "S32.200", "S32.201",
          "S32.400", "S32.401", "S32.800", "S32.801", "S33.1", "S34.19", "S34.38", "S35.9",
          "S36.990", "S36.991", "S37.090", "S37.091", "S37.990", "S37.991", "S38.2", "S39.08",
          "S41.80", "S41.81", "S42.090", "S42.091", "S42.190", "S42.191", "S42.290", "S42.291",
          "S42.390", "S42.391", "S42.490", "S42.491", "S43.090", "S43.100", "S43.200", "S43.5",
          "S43.79", "S48.9", "S51.90", "S52.000", "S52.001", "S52.100", "S52.101", "S52.500",
          "S52.501", "S52.900", "S52.901", "S53.0", "S53.190", "S53.48", "S53.49", "S57.9", "S58.9",
          "S60.0", "S60.1", "S60.9", "S61.00", "S61.01", "S61.10", "S61.11", "S61.90", "S61.91",
          "S62.000", "S62.001", "S62.190", "S62.191", "S62.290", "S62.291", "S62.490", "S62.491",
          "S62.590", "S62.591", "S62.690", "S62.691", "S63.090", "S63.190", "S63.59", "S63.69",
          "S63.79", "S64.98", "S65.9", "S66.98", "S67.8", "S68.1", "S68.9", "S70.9", "S71.80",
          "S71.81", "S72.190", "S72.191", "S72.900", "S72.901", "S73.090", "S73.19", "S78.9",
          "S81.90", "S81.91", "S82.000", "S82.001", "S82.400", "S82.401", "S82.890", "S82.891",
          "S82.900", "S82.901", "S83.000", "S83.190", "S83.6", "S85.9", "S86.08", "S86.98", "S87.8",
          "S88.9", "S90.1", "S90.2", "S90.9", "S91.30", "S91.31", "S92.000", "S92.001", "S92.100",
          "S92.101", "S92.290", "S92.291", "S92.300", "S92.301", "S92.400", "S92.401", "S92.500",
          "S92.501", "S92.900", "S92.901", "S93.000", "S93.110", "S93.310", "S93.49", "S93.6",
          "S97.8", "S98.1", "S98.4", "T00.9", "T01.90", "T01.91", "T02.90", "T02.91", "T11.0",
          "T11.1", "T13.0", "T13.1", "T14.7", "T15.0", "T15.9", "T16", "T17.1", "T17.9", "T18.1",
          "T18.5", "T18.9", "T19.0", "T19.2", "T29.0", "T30.1", "T30.2", "T30.3", "T35.7", "T39.0",
          "T39.9", "T40.1", "T40.5", "T40.6", "T40.79", "T40.9", "T42.4", "T43.9", "T46.9", "T50.9",
          "T51.0", "T51.1", "T51.8", "T52.9", "T54.9", "T56.9", "T58", "T59.9", "T60.9", "T62.9",
          "T63.9", "T65.9", "T66", "T67.0", "T67.9", "T68", "T70.3", "T70.9", "T71", "T74.9",
          "T75.0", "T75.1", "T75.4", "T78.2", "T78.3", "T78.4", "T79.0", "T79.1", "T79.3", "T79.4",
          "T81.4", "T85.0", "T85.9", "T86.9", "T88.7", "T88.9", "U04.90", "U07.0", "U07.1", "U07.2",
          "U07.3", "U07.4", "U07.7", "Z01.6", "Z01.7", "Z02.7", "Z04.0", "Z04.4", "Z04.6", "Z09.4",
          "Z09.9", "Z20.9", "Z29.9", "Z30.9", "Z34.9", "Z37.900", "Z37.910", "Z38.200", "Z38.800",
          "Z43.9", "Z45.9", "Z48.8", "Z51.88", "Z65.9", "Z71.9", "Z76.0", "Z76.9"
      }));
      
      contentService.close();
    }
  }

  /**
   * Cache existing maps.
   *
   * @throws Exception the exception
   */
  private void cacheExistingMaps() throws Exception {
    // Cache existing BC HCVS map records to pre-populate the maps
    // Up to date file, saved as tab-delimited txt must be saved here:
    // {data.dir}/doc/{projectNumber}/preloadMaps/PHCVS_maps.txt
    //
    //Format is:
    // SNOMED ID    SNOMED Term ICD-10CA Code   ICD-10CA Term   ICD-9 CA    ICD-9 Term  CedDxs (ICD-10CA) Code  CedDxs (ICD-10CA) Term  CedDxs CIHI Common Term
    // 63650001    Cholera (disorder)  A009    Cholera, unspecified    0019    Cholera unspecified A059    Bacterial foodborne intoxication, unspecified   Bacterial foodborne intox
    // 4834000 Typhoid fever (disorder)    A010    Typhoid fever   0020    Typhoid Fever   A059    Bacterial foodborne intoxication, unspecified   Bacterial foodborne intox


    final ContentService contentService = new ContentServiceJpa();

    Logger.getLogger(ICD10CAProjectSpecificAlgorithmHandler.class)
        .info("Caching the existing BC HCVS maps");

    final String dataDir = ConfigUtility.getConfigProperties().getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Check preconditions
    String inputFile =
        dataDir + "/doc/" + mapProject.getId() + "/preloadMaps/PHCVS_maps.txt";

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

      // For each line, create three map entries (one for ICD10CA, one for ICD9, one for CedDxs), 
      // and attach it to the record
      MapEntry icd10CAmapEntry = createMapEntry(fields[2], 1, 1, destinationIdToName);
      
      //Some of the provided ICD9 codes were actually from ICD9CM - need to truncate
      // 00845 -> 0084
      String icd9Target = "";
      try {
       icd9Target = fields[4];
      }
      catch (Exception e) {
        System.out.println("No index[4] for line " + line);
      }
      if(icd9Target.length()>4) {
        icd9Target = icd9Target.substring(0,4);
      }
      MapEntry icd9mapEntry = createMapEntry(icd9Target, 2, 1, destinationIdToName);

      String cedDexCode = "";
      try {
        cedDexCode = fields[6];
      }
      catch (Exception e) {
        System.out.println("No index[6] for line " + line);
      }
      
      MapEntry cedDxsmapEntry = createMapEntry(cedDexCode.equals("") ? null : cedDexCode, 3, 1, destinationIdToName);

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
  
  private MapEntry createMapEntry(String targetId, int mapGroup, int mapPriority, Map<String, String> destinationIdToName) throws Exception {
    MapEntry mapEntry = new MapEntryJpa();
    
    //For all three sources, need to add a decimal after the third character
    String formattedTargetId;
    if(targetId != null && targetId.length()>3) {
      formattedTargetId = targetId.substring(0, 3).concat(".").concat(targetId.substring(3));
    } else {
      formattedTargetId = targetId;
    }
    
    //For ICD-9 codes, add a hyphen to the end, so it will match how the code is represented in the terminology
    if(mapGroup == 2) {
      formattedTargetId = formattedTargetId + "-";
    }
    
    mapEntry.setTargetId(formattedTargetId);
    mapEntry.setMapGroup(mapGroup);
    mapEntry.setMapPriority(mapPriority);
    String targetConceptName = (formattedTargetId == null) ? "No target" : destinationIdToName.get(formattedTargetId);
    
    if (targetConceptName != null) {
      mapEntry.setTargetName(targetConceptName);
    } else {
      mapEntry
          .setTargetName("CONCEPT DOES NOT EXIST IN " + mapProject.getDestinationTerminology());
    }
    
    return mapEntry;
  }
  
  
}