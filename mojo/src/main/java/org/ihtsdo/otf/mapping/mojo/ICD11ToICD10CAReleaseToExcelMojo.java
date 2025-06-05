/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Create the CIHI ICD11 to ICD10CA release file from *
 * tls_Icd11HumanReadableMap_INT_YYYYMMDD.tsv
 * 
 * mvn package -P CihiICD11ToICD10CAReleaseToExcel "-DreleaseFile=<full path to
 * tls_Icd11HumanReadableMap_INT_YYYYMMDD.tsv file> "
 * 
 * @goal cihi-icd11-to-icd10ca-release-to-excel
 */
public class ICD11ToICD10CAReleaseToExcelMojo extends AbstractMojo {

  /**
   * Comma delimited list of project ids.
   *
   * @parameter
   * @required
   */
  private String releaseFile;

  /**
   * Excel file.
   *
   * @parameter
   * @required
   */
  private String outputFile;

  /**
   * The specified refsetId
   * @parameter
   * @required
   */
  private String refsetId = null;

  /**
   * Map project Id.
   */
  private Long mapProjectId;
  
  private Map<String, ExternalData> sourceConceptToExternalData = new HashMap<>();  
  
  private Set<String> icd10caAsteriskCodes = new HashSet<>();
  
  private Set<String> icd10caCanadianCodes = new HashSet<>();
  
  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    getLog().info("Start ICD11 to ICD10CA Release To Excel Mojo");

    if (StringUtils.isEmpty(releaseFile)) {
      getLog().error("Parameter releaseFile is empty.");
      throw new MojoExecutionException("Parameter releaseFile is empty.");
    }

    try (final MappingService mappingService = new MappingServiceJpa();) {

      MapProject mapProject = mappingService.getMapProjectForRefSetId(refsetId);

      if(mapProject == null) {
        throw new MojoExecutionException("Map Project does not exist for refsetId=" + refsetId);
      }
      
      mapProjectId=mapProject.getId();
      
      final File rf = new File(releaseFile);
      if (!rf.exists()) {
        throw new MojoExecutionException(releaseFile + " file does not exist.");
      }
      getExternalData();
      runConvert(rf);

    } catch (Exception e) {
      getLog().error("Error running ICD11 to ICD10CA Release To Excel Mojo.", e);
    }
  }

  //
  // Look up additional data not present in the release file
  // Most are pulled from database objects, but a few are pulled from external files
  //
  private void getExternalData()  throws Exception {

    getLog().info("Gathering mapping data stored outside of the release file");

    
    
    MappingService mappingService = new MappingServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    MapProject mapProject = mappingService.getMapProject(mapProjectId);
    final String icd10caTerminology = mapProject.getDestinationTerminology();
    final String icd10caTerminologyVersion = mapProject.getDestinationTerminologyVersion();

    // Get all map records for the project
    MapRecordList allMapRecords = mappingService.getMapRecordsForMapProject(mapProjectId);

    for(MapRecord mapRecord : allMapRecords.getMapRecords()) {
      // Only process READY_FOR_PUBLICATION and PUBLISHED records
      if(!(mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION) || mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED))) {
        continue;
      }

      //Create an External Data object, based on the source concept
      ExternalData externalData = new ExternalData(mapRecord.getConceptId());
            
      
      //Look up Additional Entry Info
      for(MapEntry mapEntry : mapRecord.getMapEntries()) {
        for(AdditionalMapEntryInfo additionalMapEntryInfo : mapEntry.getAdditionalMapEntryInfos()) {
          if(additionalMapEntryInfo.getField().equals("Relation - Target")) {
            externalData.setRelationTarget(additionalMapEntryInfo.getValue());
          }
          else if(additionalMapEntryInfo.getField().equals("Unmappable Reason")) {
            externalData.setUnmappableReason(additionalMapEntryInfo.getValue());
          }
          else if(additionalMapEntryInfo.getField().equals("Target Mismatch Reason")) {
            externalData.setTargetMismatchReason(additionalMapEntryInfo.getValue());
          } 
        }
      }
      
      //Add the External Data object to the map
      sourceConceptToExternalData.put(mapRecord.getConceptId(), externalData);
      
    }
    
    //
    // Look up additional data stored in external files
    //

    // Look up which codes are canadian-specific from file
    String icd10caVersion = mapProject.getSourceTerminologyVersion();

    final String dataDir = ConfigUtility.getConfigProperties().getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Check preconditions
    final String inputFile = dataDir + "/doc/" + mapProjectId + "/projectFiles/"
        + icd10caTerminology.toLowerCase() + "_" + icd10caTerminologyVersion + "_CanadianCodes.txt";

    if (!new File(inputFile).exists()) {
      throw new Exception("Specified input file missing: " + inputFile);
    }

    // Open reader and service
    final BufferedReader canadianCodeReader = new BufferedReader(new FileReader(inputFile));

    String line = null;

    while ((line = canadianCodeReader.readLine()) != null) {
      String canadianCode = line;
      icd10caCanadianCodes.add(canadianCode);
    }
    
    
    //Identify which ICD10CA concepts have an asterisk
    ConceptList icd10caConcepts = contentService.getAllConcepts(icd10caTerminology, icd10caTerminologyVersion);
    for(Concept concept : icd10caConcepts.getConcepts()) {
      if(TerminologyUtility.isAsteriskCode(concept, contentService)) {
        icd10caAsteriskCodes.add(concept.getTerminologyId());
      }
    }
    
    canadianCodeReader.close();
    mappingService.close();
    contentService.close();
    
  }
  
  
/**
   * Run convert.
   *
   * @param releaseFile the release file
   * @throws Exception the exception
   */
  private void runConvert(final File releaseFile) throws Exception {

    final List<GroupRecord> groupRecordList = new ArrayList<GroupRecord>();

    try {

      // read file
      final List<String> lines = FileUtils.readLines(releaseFile, "UTF-8");
      for (final String line : lines) {
        final String[] fields = line.split("\\t");
        if ("id".equals(fields[0]))
          continue;
        groupRecordList.add(
            new GroupRecord(fields[5], fields[6], Integer.valueOf(fields[7]),
                Integer.valueOf(fields[8]), fields[11], fields[12]));
      }

      getLog().info("Source file rows " + groupRecordList.size());

      Collections.sort(groupRecordList,
          Comparator.comparing(GroupRecord::getCode)
              .thenComparing(GroupRecord::getMapPriority)
              .thenComparing(GroupRecord::getMapGroup));

      // MapGroup is ICD-11

      String previousId = null;
      String previousMapPriority = null;
      Record record = null;
      final List<Record> recordList = new ArrayList<>();
      for (final GroupRecord groupRecord : groupRecordList) {
        if (!groupRecord.getCode().equals(previousId)) {
          if (record != null) {
        	record.setCardinality("1:" + previousMapPriority);
            recordList.add(record);
          }
          record = new Record(groupRecord.getCode(), groupRecord.getTermName());
        }
        
        // ICD-11
        if (groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 1) {
          record.setIcd10CACode(groupRecord.getTargetTermCode());
          record.setIcd10CATerm(groupRecord.getTargetTermName());
        }
        if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 2) {
        	record.setIcd10CACodeCluster2(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
        }
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 3) {
			record.setIcd10CACodeCluster3(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName())); 	
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 4) {
			record.setIcd10CACodeCluster4(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 5) {
			record.setIcd10CACodeCluster5(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 6) {
			record.setIcd10CACodeCluster6(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 7) {
			record.setIcd10CACodeCluster7(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 8) {
			record.setIcd10CACodeCluster8(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 9) {
			record.setIcd10CACodeCluster9(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 10) {
			record.setIcd10CACodeCluster10(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 2 && groupRecord.getMapPriority() == 1) {
			record.setWHOMapCode(groupRecord.getTargetTermCode());
	        record.setWHOMapName(groupRecord.getTargetTermName());
		}
        
		previousId = groupRecord.getCode();
        previousMapPriority = Integer.toString(groupRecord.getMapPriority());

      }
      // get last record
      if (record != null) {
      	record.setCardinality("1:" + previousMapPriority);
          recordList.add(record);
      }

      // Use the records to create the C&T format output file
      createCTOutput(releaseFile, recordList);

      // Now create the CaseMix format output file
      createCaseMixOutput(releaseFile, recordList);      
      
    } catch (Exception e) {
      e.printStackTrace();
    }

  }


  private void createCTOutput(File releaseFile, List<Record> recordList) throws Exception {

    final Workbook wb = new XSSFWorkbook();

    final LocalDate localDate = LocalDate.now();
    final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyyMMdd");
    final Sheet sheet = wb.createSheet(
        outputFile.replace("{FORMAT}", "C&T").replace("{DATE}", localDate.format(formatter)));

    // Create header row and add cells
    int rownum = 0;
    int cellnum = 0;

    Row row = sheet.createRow(rownum++);
    Cell cell = null;
    
    cell = row.createCell(0);
    cell.setCellValue("Source Concept");
    
    cell = row.createCell(2);
    cell.setCellValue("CIHI Map - 1st Entry (Map Group 1)");
    
    cell = row.createCell(6);
    cell.setCellValue("CIHI Map - Full Expression");

    cell = row.createCell(7);
    cell.setCellValue("CIHI map - Mapping Parameters");
    
    cell = row.createCell(9);
    cell.setCellValue("CIHI Map - Additional Entries");
    
    cell = row.createCell(45);
    cell.setCellValue("WHO map (Map Group 2)");
    
    cell = row.createCell(47);
    cell.setCellValue("WHO - Mapping Parameters");
    
    sheet.addMergedRegion(new CellRangeAddress(0,0,0,1));
    sheet.addMergedRegion(new CellRangeAddress(0,0,2,5));
    sheet.addMergedRegion(new CellRangeAddress(0,0,7,8));
    sheet.addMergedRegion(new CellRangeAddress(0,0,9,44));
    sheet.addMergedRegion(new CellRangeAddress(0,0,45,46));
    
    row = sheet.createRow(rownum++);
    

    // ICD 11
    cell = row.createCell(cellnum++);
    cell.setCellValue("ICD-11 Code");
    cell = row.createCell(cellnum++);
    cell.setCellValue("ICD-11 Code Title");
    // ICD 10CA
    cell = row.createCell(cellnum++);
    cell.setCellValue("ICD-10-CA Code");
    cell = row.createCell(cellnum++);
    cell.setCellValue("ICD-10-CA Code Title");
    cell = row.createCell(cellnum++);
    cell.setCellValue("ICD-10-CA Code Asterisk");
    cell = row.createCell(cellnum++);
    cell.setCellValue("ICD-10-CA Canadian Specific Code");
    // CIHI Cardinality
    cell = row.createCell(cellnum++);
    cell.setCellValue("Cardinality");

    // CIHI Mapping Parameters
    cell = row.createCell(cellnum++);
    cell.setCellValue("Relation -  Target");
    cell = row.createCell(cellnum++);
    cell.setCellValue("Unmappable Reason");
    
    for(int start = 2; start <= 10; start++) {
        // other clusters
        cell = row.createCell(cellnum++);
        cell.setCellValue("ICD-10-CA Code " + start);
        cell = row.createCell(cellnum++);
        cell.setCellValue("ICD-10-CA Code Title " + start);
        cell = row.createCell(cellnum++);
        cell.setCellValue("ICD-10-CA Asterisk " + start);
        cell = row.createCell(cellnum++);
        cell.setCellValue("ICD-10-CA Canadian Specific Code " + start);
    }
    
    // WHO map (Map Group 2)  
    cell = row.createCell(cellnum++);
    cell.setCellValue("ICD-10 Code");
    cell = row.createCell(cellnum++);
    cell.setCellValue("ICD-10 Title");
    
    // WHO map - Mapping Parameters   
    cell = row.createCell(cellnum++);
    cell.setCellValue("Target Mismatch Reason");
    
    Set<String> codeSet = new HashSet<>();
    for (final Record outRecord : recordList) {
      // Add data row
      getLog().info("Add: " + outRecord.toString());
      codeSet.add(outRecord.getIcd11Code());
      cellnum = 0;
      row = sheet.createRow(rownum++);


      // Lookup external data
      ExternalData externalData = sourceConceptToExternalData.get(outRecord.getIcd11Code());
      //If none found, create a blank one, but also log a warning
      if(externalData == null) {
        externalData = new ExternalData(outRecord.getIcd11Code());
        getLog().warn("No external data found for source concept " + outRecord.getIcd11Code());
      }        
      
      // ICD 11
      cell = row.createCell(cellnum++);
      cell.setCellValue(outRecord.getIcd11Code());
      cell = row.createCell(cellnum++);
      cell.setCellValue(outRecord.getIcd11Term());

      // ICD 10 CA
      cell = row.createCell(cellnum++);
      cell.setCellValue(!(outRecord.getIcd10CACode().isEmpty() || outRecord.getIcd10CACode().isBlank()) ? outRecord.getIcd10CACode() : "No target");
      cell = row.createCell(cellnum++);
      cell.setCellValue(outRecord.getIcd10CATerm());
      cell = row.createCell(cellnum++);
      cell.setCellValue(Boolean.valueOf(icd10caAsteriskCodes.contains(outRecord.getIcd10CACode())).toString().toUpperCase());
      cell = row.createCell(cellnum++);
      cell.setCellValue(Boolean.valueOf(icd10caCanadianCodes.contains(outRecord.getIcd10CACode())).toString().toUpperCase());
      
      cell = row.createCell(cellnum++);
      cell.setCellValue(!(outRecord.getIcd10CACode().isEmpty() || outRecord.getIcd10CACode().isBlank()) ? outRecord.getCardinality() : "n/a - no target");
      

      // Mapping Parameters
      cell = row.createCell(cellnum++);
      cell.setCellValue(externalData.getRelationTarget());
      cell = row.createCell(cellnum++);
      cell.setCellValue(externalData.getUnmappableReason());

      
      List<Map<String, String>> clusters = Arrays.asList(
              outRecord.Icd10CACodeCluster2, outRecord.Icd10CACodeCluster3, outRecord.Icd10CACodeCluster4,
              outRecord.Icd10CACodeCluster5, outRecord.Icd10CACodeCluster6, outRecord.Icd10CACodeCluster7,
              outRecord.Icd10CACodeCluster8, outRecord.Icd10CACodeCluster9, outRecord.Icd10CACodeCluster10
      );

      // Iterate over clusters
      for (Map<String, String> currentCluster : clusters) {

          // Check if the cluster is empty (null or empty map)
          if (currentCluster == null || currentCluster.isEmpty()) {
              break; // Stop if current cluster is empty
          }
          else {
              String key = currentCluster.entrySet().iterator().next().getKey();
              String value = currentCluster.entrySet().iterator().next().getValue();
              cell = row.createCell(cellnum++);
              cell.setCellValue(key);
              cell = row.createCell(cellnum++);
              cell.setCellValue(value);
              cell = row.createCell(cellnum++);
              cell.setCellValue(Boolean.valueOf(icd10caAsteriskCodes.contains(key)).toString().toUpperCase());
              cell = row.createCell(cellnum++);
              cell.setCellValue(Boolean.valueOf(icd10caCanadianCodes.contains(key)).toString().toUpperCase());
          }

      }
      cellnum = 45;
      
      // WHO mapping
      cell = row.createCell(cellnum++);
      cell.setCellValue(outRecord.getWHOMapCode());
      cell = row.createCell(cellnum++);
      cell.setCellValue(outRecord.getWHOMapName());

      // WHO mapping parameters
      cell = row.createCell(cellnum++);
      cell.setCellValue(externalData.getTargetMismatchReason());
      

    }

    // write file
    final Path fileLocation = Paths.get(releaseFile.getParent(),
        outputFile.replace("{FORMAT}", "C&T").replace("{DATE}", localDate.format(formatter)));

    try (final FileOutputStream outputStream =
        new FileOutputStream(fileLocation.toString());) {
      wb.write(outputStream);
      wb.close();
    }

    getLog().info("Conversion completed " + fileLocation.toString());
  
}
  
  
  private void createCaseMixOutput(File releaseFile, List<Record> recordList) throws Exception  {

    final Workbook wb = new XSSFWorkbook();

    final LocalDate localDate = LocalDate.now();
    final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyyMMdd");
    final Sheet sheet = wb.createSheet(
        outputFile.replace("{FORMAT}", "CaseMix").replace("{DATE}", localDate.format(formatter)));

    // Create header row and add cells
    int rownum = 0;
    int cellnum = 0;

    Row row = sheet.createRow(rownum++);
    Cell cell = null;
    
    cell = row.createCell(0);
    cell.setCellValue("ICD-11 code");

    cell = row.createCell(1);
    cell.setCellValue("ICD-11 code title");

    cell = row.createCell(2);
    cell.setCellValue("ICD-10-CA code");

    cell = row.createCell(3);
    cell.setCellValue("ICD-10-CA title");

    cell = row.createCell(4);
    cell.setCellValue("ICD-10-CA asterisk");

    cell = row.createCell(5);
    cell.setCellValue("ICD-10-CA canadian specific code");

    cell = row.createCell(6);
    cell.setCellValue("Cardinality");

    cell = row.createCell(7);
    cell.setCellValue("Relation -  Target");

    
    Set<String> codeSet = new HashSet<>();
    for (final Record outRecord : recordList) {
      // Add data row
      getLog().info("Add: " + outRecord.toString());
      codeSet.add(outRecord.getIcd11Code());
      cellnum = 0;
      row = sheet.createRow(rownum++);


      // Lookup external data
      ExternalData externalData = sourceConceptToExternalData.get(outRecord.getIcd11Code());
      //If none found, create a blank one, but also log a warning
      if(externalData == null) {
        externalData = new ExternalData(outRecord.getIcd11Code());
        getLog().warn("No external data found for source concept " + outRecord.getIcd11Code());
      }        
      
      // ICD 11 (Case Mix format wants decimal removed)
      cell = row.createCell(cellnum++);
      cell.setCellValue(outRecord.getIcd11Code().replace(".", ""));
      cell = row.createCell(cellnum++);
      cell.setCellValue(outRecord.getIcd11Term());

      // ICD 10 CA (Case Mix format wants decimal removed)
      cell = row.createCell(cellnum++);
      cell.setCellValue(!(outRecord.getIcd10CACode().isEmpty() || outRecord.getIcd10CACode().isBlank()) ? outRecord.getIcd10CACode().replace(".", "") : "No target");
      cell = row.createCell(cellnum++);
      cell.setCellValue(outRecord.getIcd10CATerm());
      
      // Asterisk, Canadian Specific Code, and Cardinality
      cell = row.createCell(cellnum++);
      cell.setCellValue(Boolean.valueOf(icd10caAsteriskCodes.contains(outRecord.getIcd10CACode())).toString().toUpperCase());
      cell = row.createCell(cellnum++);
      cell.setCellValue(Boolean.valueOf(icd10caCanadianCodes.contains(outRecord.getIcd10CACode())).toString().toUpperCase());
      cell = row.createCell(cellnum++);
      cell.setCellValue(!(outRecord.getIcd10CACode().isEmpty() || outRecord.getIcd10CACode().isBlank()) ? outRecord.getCardinality() : "n/a - no target");
      

      // Mapping Parameters
      cell = row.createCell(cellnum++);
      cell.setCellValue(externalData.getRelationTarget());

      
      List<Map<String, String>> clusters = Arrays.asList(
              outRecord.Icd10CACodeCluster2, outRecord.Icd10CACodeCluster3, outRecord.Icd10CACodeCluster4,
              outRecord.Icd10CACodeCluster5, outRecord.Icd10CACodeCluster6, outRecord.Icd10CACodeCluster7,
              outRecord.Icd10CACodeCluster8, outRecord.Icd10CACodeCluster9, outRecord.Icd10CACodeCluster10
      );

      // Iterate over clusters
      for (Map<String, String> currentCluster : clusters) {

          // Check if the cluster is empty (null or empty map)
          if (currentCluster == null || currentCluster.isEmpty()) {
              break; // Stop if current cluster is empty
          }
          else {
            
              // For each additional target, create a new row and populate only ICD11 and ICD10CA code and titles, and ICD10CA Asterisk
              cellnum = 0;
              row = sheet.createRow(rownum++);
              
              // ICD 11 (Case Mix format wants decimal removed)
              cell = row.createCell(cellnum++);
              cell.setCellValue(outRecord.getIcd11Code().replace(".", ""));
              cell = row.createCell(cellnum++);
              cell.setCellValue(outRecord.getIcd11Term());
              
              // ICD 10 CA (Case Mix format wants decimal removed)
              String key = currentCluster.entrySet().iterator().next().getKey();
              String value = currentCluster.entrySet().iterator().next().getValue();
              cell = row.createCell(cellnum++);
              cell.setCellValue(key.replace(".", ""));
              cell = row.createCell(cellnum++);
              cell.setCellValue(value);
              cell = row.createCell(cellnum++);
              cell.setCellValue(Boolean.valueOf(icd10caAsteriskCodes.contains(key)).toString().toUpperCase());
              cell = row.createCell(cellnum++);
              cell.setCellValue(Boolean.valueOf(icd10caCanadianCodes.contains(key)).toString().toUpperCase());
          }

      }
    }

    // write file
    final Path fileLocation = Paths.get(releaseFile.getParent(),
        outputFile.replace("{FORMAT}", "CaseMix").replace("{DATE}", localDate.format(formatter)));

    try (final FileOutputStream outputStream =
        new FileOutputStream(fileLocation.toString());) {
      wb.write(outputStream);
      wb.close();
    }

    getLog().info("Conversion completed " + fileLocation.toString());
  
  
}

  /**
   * The Class GroupRecord.
   */
  @SuppressWarnings("unused")
  private class GroupRecord {

    /** The code. */
    private String code;

    /** The term name. */
    private String termName;

    /** The map group. */
    private int mapGroup;

    /** The map priority. */
    private int mapPriority;

    /** The target term code. */
    private String targetTermCode;

    /** The target term name. */
    private String targetTermName;

    /**
     * Instantiates a {@link GroupRecord} from the specified parameters.
     *
     * @param code the code
     * @param termName the term name
     * @param mapGroup the map group
     * @param mapPriority the map priority
     * @param targetTermCode the target term code
     * @param targetTermName the target term name
     */
    public GroupRecord(final String code, final String termName,
        final int mapGroup, final int mapPriority, final String targetTermCode,
        final String targetTermName) {
      super();
      this.code = code;
      this.termName = termName;
      this.mapGroup = mapGroup;
      this.mapPriority = mapPriority;
      this.targetTermCode = targetTermCode;
      this.targetTermName = targetTermName;
    }

    /**
     * Returns the code.
     *
     * @return the code
     */
    public String getCode() {
      return code;
    }

    /**
     * Sets the code.
     *
     * @param code the code
     */
    public void setCode(final String code) {
      this.code = code;
    }

    /**
     * Returns the term name.
     *
     * @return the term name
     */
    public String getTermName() {
      return termName;
    }

    /**
     * Sets the term name.
     *
     * @param termName the term name
     */
    public void setTermName(final String termName) {
      this.termName = termName;
    }

    /**
     * Returns the map group.
     *
     * @return the map group
     */
    public int getMapGroup() {
      return mapGroup;
    }

    /**
     * Sets the map group.
     *
     * @param mapGroup the map group
     */
    public void setMapGroup(final int mapGroup) {
      this.mapGroup = mapGroup;
    }

    /**
     * Returns the map priority.
     *
     * @return the map priority
     */
    public int getMapPriority() {
      return mapPriority;
    }

    /**
     * Sets the map priority.
     *
     * @param mapPriority the map priority
     */
    public void setMapPriority(final int mapPriority) {
      this.mapPriority = mapPriority;
    }

    /**
     * Returns the target term code.
     *
     * @return the target term code
     */
    public String getTargetTermCode() {
      return targetTermCode;
    }

    /**
     * Sets the target term code.
     *
     * @param targetTermCode the target term code
     */
    public void setTargetTermCode(final String targetTermCode) {
      this.targetTermCode = targetTermCode;
    }

    /**
     * Returns the target term name.
     *
     * @return the target term name
     */
    public String getTargetTermName() {
      return targetTermName;
    }

    /**
     * Sets the target term name.
     *
     * @param targetTermName the target term name
     */
    public void setTargetTermName(final String targetTermName) {
      this.targetTermName = targetTermName;
    }

    /* see superclass */
    @Override
    public String toString() {
      return "GroupRecord [code=" + code + ", termName=" + termName
          + ", mapGroup=" + mapGroup + ", mapPriority=" + mapPriority
          + ", targetTermCode=" + targetTermCode + ", targetTermName="
          + targetTermName + "]";
    }

  }

  /**
   * The Class Record.
   */
  @SuppressWarnings("unused")
  private class Record {

    /** The icd 10 CA code. */
    private String icd10CACode;

    /** The icd 10 CA term. */
    private String icd10CATerm;

    /** The icd 11 code. */
    private String icd11Code;

    /** The icd 11 term. */
    private String icd11Term;
    
    /** The cardinality. */
    private String cardinality;
    
    /** The cluster */
    private String cluster;
    
    /** The other clusters. */
    private Map<String, String> Icd10CACodeCluster2;
    private Map<String, String> Icd10CACodeCluster3;
    private Map<String, String> Icd10CACodeCluster4;
    private Map<String, String> Icd10CACodeCluster5;
    private Map<String, String> Icd10CACodeCluster6;
    private Map<String, String> Icd10CACodeCluster7;
    private Map<String, String> Icd10CACodeCluster8;
    private Map<String, String> Icd10CACodeCluster9;
    private Map<String, String> Icd10CACodeCluster10;
    
    /** The WHO map code. */
    private String WHOMapCode;
    
    /** The WHO map name. */
    private String WHOMapName;

    /**
     * Instantiates a {@link Record} from the specified parameters.
     *
     * @param snomedCode the snomed code
     * @param snomedTerm the snomed term
     */
    public Record(final String icd11Code, final String icd11Term) {
      super();
      this.icd11Code = icd11Code;
      this.icd11Term = icd11Term;
    }

    /**
     * Returns the icd 10 code.
     *
     * @return the icd 10 code
     */
    public String getIcd10CACode() {
      return icd10CACode;
    }

    /**
     * Sets the icd 10 code.
     *
     * @param icd10Code the icd 10 code
     */
    public void setIcd10CACode(final String icd10CACode) {
      this.icd10CACode = icd10CACode;
    }

    /**
     * Returns the icd 10 term.
     *
     * @return the icd 10 term
     */
    public String getIcd10CATerm() {
      return icd10CATerm;
    }

    /**
     * Sets the icd 10 term.
     *
     * @param icd10Term the icd 10 term
     */
    public void setIcd10CATerm(final String icd10CATerm) {
      this.icd10CATerm = icd10CATerm;
    }

    /**
     * Returns the icd 11 code.
     *
     * @return the icd 11 code
     */
    public String getIcd11Code() {
      return icd11Code;
    }

    /**
     * Sets the icd 11 code.
     *
     * @param icd11Code the icd 11 code
     */
    public void setIcd11Code(final String icd11Code) {
      this.icd11Code = icd11Code;
    }

    /**
     * Returns the icd 11 term.
     *
     * @return the icd 11 term
     */
    public String getIcd11Term() {
      return icd11Term;
    }

    /**
     * Sets the icd 11 term.
     *
     * @param icd11Term the icd 11 term
     */
    public void setIcd11Term(final String icd11Term) {
      this.icd11Term = icd11Term;
    }


    public String getCardinality() {
		return cardinality;
	}

	public void setCardinality(String cardinality) {
		this.cardinality = cardinality;
	}

	public String getCluster() {
		return cluster;
	}

	public void setCluster(String cluster) {
		this.cluster = cluster;
	}
	
	public Map<String, String> getIcd10CACodeCluster2() {
        return Icd10CACodeCluster2;
    }

    public void setIcd10CACodeCluster2(Map<String, String> Icd10CACodeCluster2) {
        this.Icd10CACodeCluster2 = Icd10CACodeCluster2;
    }

    public Map<String, String> getIcd10CACodeCluster3() {
        return Icd10CACodeCluster3;
    }

    public void setIcd10CACodeCluster3(Map<String, String> Icd10CACodeCluster3) {
        this.Icd10CACodeCluster3 = Icd10CACodeCluster3;
    }

    public Map<String, String> getIcd10CACodeCluster4() {
        return Icd10CACodeCluster4;
    }

    public void setIcd10CACodeCluster4(Map<String, String> Icd10CACodeCluster4) {
        this.Icd10CACodeCluster4 = Icd10CACodeCluster4;
    }

    public Map<String, String> getIcd10CACodeCluster5() {
        return Icd10CACodeCluster5;
    }

    public void setIcd10CACodeCluster5(Map<String, String> Icd10CACodeCluster5) {
        this.Icd10CACodeCluster5 = Icd10CACodeCluster5;
    }

    public Map<String, String> getIcd10CACodeCluster6() {
        return Icd10CACodeCluster6;
    }

    public void setIcd10CACodeCluster6(Map<String, String> Icd10CACodeCluster6) {
        this.Icd10CACodeCluster6 = Icd10CACodeCluster6;
    }

    public Map<String, String> getIcd10CACodeCluster7() {
        return Icd10CACodeCluster7;
    }

    public void setIcd10CACodeCluster7(Map<String, String> Icd10CACodeCluster7) {
        this.Icd10CACodeCluster7 = Icd10CACodeCluster7;
    }

    public Map<String, String> getIcd10CACodeCluster8() {
        return Icd10CACodeCluster8;
    }

    public void setIcd10CACodeCluster8(Map<String, String> Icd10CACodeCluster8) {
        this.Icd10CACodeCluster8 = Icd10CACodeCluster8;
    }

    public Map<String, String> getIcd10CACodeCluster9() {
        return Icd10CACodeCluster9;
    }

    public void setIcd10CACodeCluster9(Map<String, String> Icd10CACodeCluster9) {
        this.Icd10CACodeCluster9 = Icd10CACodeCluster9;
    }

    public Map<String, String> getIcd10CACodeCluster10() {
        return Icd10CACodeCluster10;
    }

    public void setIcd10CACodeCluster10(Map<String, String> Icd10CACodeCluster10) {
        this.Icd10CACodeCluster10 = Icd10CACodeCluster10;
    }

	public String getWHOMapCode() {
		return WHOMapCode;
	}

	public void setWHOMapCode(String wHOMapCode) {
		WHOMapCode = wHOMapCode;
	}

	public String getWHOMapName() {
		return WHOMapName;
	}

	public void setWHOMapName(String wHOMapName) {
		WHOMapName = wHOMapName;
	}

	/* see superclass */
    @Override
    public String toString() {
    	return "Record [icd11Code=" + icd11Code + ", icd11Term=" + icd11Term
        + ", icd10CACode=" + icd10CACode + ", icd10CATerm=" + icd10CATerm
        + ", cardinality=" + cardinality + ", cluster=" + cluster
        + ", Icd10CACodeCluster2=" + Icd10CACodeCluster2
        + ", Icd10CACodeCluster3=" + Icd10CACodeCluster3
        + ", Icd10CACodeCluster4=" + Icd10CACodeCluster4
        + ", Icd10CACodeCluster5=" + Icd10CACodeCluster5
        + ", Icd10CACodeCluster6=" + Icd10CACodeCluster6
        + ", Icd10CACodeCluster7=" + Icd10CACodeCluster7
        + ", Icd10CACodeCluster8=" + Icd10CACodeCluster8
        + ", Icd10CACodeCluster9=" + Icd10CACodeCluster9
        + ", Icd10CACodeCluster10=" + Icd10CACodeCluster10
        + ", WHOMapCode=" + WHOMapCode
        + ", WHOMapName=" + WHOMapName +"]";
    }
    
  }
  
  /**
   * The External Data.
   */
  @SuppressWarnings("unused")
  private class ExternalData {

    /** The source code. */
    private String sourceCode;
    
    private String relationTarget;
        
    private String unmappableReason;

    private String targetMismatchReason;

    /**
     * Instantiates a {@link ExternalData} from the specified parameters.
     *
     * @param sourceCode the source ode
     */
    public ExternalData(final String sourceCode) {
      super();
      this.sourceCode = sourceCode;
    }

    public String getSourceCode() {
      return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
      this.sourceCode = sourceCode;
    }

    public String getRelationTarget() {
      return relationTarget;
    }

    public void setRelationTarget(String relationTarget) {
      this.relationTarget = relationTarget;
    }

    public String getUnmappableReason() {
      return unmappableReason;
    }

    public void setUnmappableReason(String unmappableReason) {
      this.unmappableReason = unmappableReason;
    }

    public String getTargetMismatchReason() {
      return targetMismatchReason;
    }

    public void setTargetMismatchReason(String targetMismatchReason) {
      this.targetMismatchReason = targetMismatchReason;
    }
  }

}
