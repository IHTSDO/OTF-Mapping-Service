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
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Create the CIHI ICD10CA to ICD11 release file from *
 * tls_Icd11HumanReadableMap_INT_YYYYMMDD.tsv
 * 
 * mvn package -P CihiICD10CAToICD11ReleaseToExcel "-DreleaseFile=<full path to
 * tls_Icd11HumanReadableMap_INT_YYYYMMDD.tsv file> "
 * 
 * @goal cihi-icd10ca-to-icd11-release-to-excel
 */
public class ICD10CAToICD11ReleaseToExcelMojo extends AbstractMojo {

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
   * Map project Id (hard-coded, and may need updating if project changes).
   */
  private Long mapProjectId = 4L;
    
  private Map<String, ExternalData> sourceConceptToExternalData = new HashMap<>();

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    getLog().info("Start ICD10CA to ICD11 Release To Excel Mojo");

    if (StringUtils.isEmpty(releaseFile)) {
      getLog().error("Parameter releaseFile is empty.");
      throw new MojoExecutionException("Parameter releaseFile is empty.");
    }

    try {

      final File rf = new File(releaseFile);
      if (!rf.exists()) {
        throw new MojoExecutionException(releaseFile + " file does not exist.");
      }
      getExternalData();
      runConvert(rf);

    } catch (Exception e) {
      getLog().error("Error running ICD10CA to ICD11 Release To Excel Mojo.", e);
    }
  }

  //
  // Look up additional data not present in the release file
  // Most are pulled from database objects, but a few are pulled from external files
  //
  private void getExternalData()  throws Exception {

    getLog().info("Gathering mapping data stored outside of the release file");

    
    // Get all map records for the project
    
    MappingService mappingService = new MappingServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    MapProject mapProject = mappingService.getMapProject(mapProjectId);
    final String icd10caTerminology = mapProject.getSourceTerminology();
    final String icd10caTerminologyVersion = mapProject.getSourceTerminologyVersion();
    
    MapRecordList allMapRecords = mappingService.getMapRecordsForMapProject(mapProjectId);

    for(MapRecord mapRecord : allMapRecords.getMapRecords()) {
      // Only process READY_FOR_PUBLICATION and PUBLISHED records
      if(!(mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION) || mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED))) {
        continue;
      }

      //Create an External Data object, based on the source concept
      ExternalData externalData = new ExternalData(mapRecord.getConceptId());
      
      // Look up if ICD10CA code is an Asterisk code
      final Concept icd10caConcept = contentService.getConcept(mapRecord.getConceptId(), icd10caTerminology, icd10caTerminologyVersion);
      if(icd10caConcept != null) {
        externalData.setAsterisk(TerminologyUtility.isAsteriskCode(icd10caConcept, contentService));
      }
      else {
        getLog().warn("Could not find concept for:" + icd10caTerminology + " " + icd10caTerminologyVersion + ": " + mapRecord.getConceptId());
      }
      
      // Look up map notes
      // Examples:
      // ICD-11 Foundation Entity Name and URI and Relation:<br>Cholera due to Vibrio cholerae O1, biovar eltor<br>http://id.who.int/icd/entity/581614179<br>Equivalent entity without xcodes/stem codes
      // ICD-11 Foundation Entity Name and URI and Relation:<br>N/A
      Set<MapNote> mapNotes = mapRecord.getMapNotes();
      for(MapNote mapNote : mapNotes) {
        if(mapNote.getNote().startsWith("ICD-11 Foundation Entity Name and URI and Relation")) {

          if(mapNote.getNote().endsWith("N/A")) {
            externalData.setNoteFoundationEntityName("");
            externalData.setNoteRelation("");
            externalData.setNoteURI("");
          }
          
          else {
            // Split the string by <br>, <br/> or <br />
            String[] noteParts = mapNote.getNote().split("(?i)<br\\s*/?>");

            externalData.setNoteFoundationEntityName(noteParts.length >= 2 ? noteParts[1] : "");
            externalData.setNoteURI(noteParts.length >= 3 ? noteParts[2] : "");
            externalData.setNoteRelation(noteParts.length >= 4 ? noteParts[3] : "");
          }
        }
      }
      
      //Look up Additional Entry Info
      for(MapEntry mapEntry : mapRecord.getMapEntries()) {
        for(AdditionalMapEntryInfo additionalMapEntryInfo : mapEntry.getAdditionalMapEntryInfos()) {
          if(additionalMapEntryInfo.getField().equals("Relation - Cluster")) {
            externalData.setRelationCluster(additionalMapEntryInfo.getValue());
          }
          else if(additionalMapEntryInfo.getField().equals("Relation - Target")) {
            externalData.setRelationTarget(additionalMapEntryInfo.getValue());
          }
          else if(additionalMapEntryInfo.getField().equals("Unmappable Reason")) {
            externalData.setUnmappableReason(additionalMapEntryInfo.getValue());
          }
          else if(additionalMapEntryInfo.getField().equals("Relation - WHO")) {
            externalData.setRelationWHO(additionalMapEntryInfo.getValue());
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
    final String inputFile =
        dataDir + "/doc/" + mapProjectId + "/projectFiles/" + icd10caTerminology.toLowerCase() + "_" + icd10caTerminologyVersion + "_CanadianCodes.txt";

    if (!new File(inputFile).exists()) {
      throw new Exception("Specified input file missing: " + inputFile);
    }
    
    // Open reader and service
    final BufferedReader canadianCodeReader = new BufferedReader(new FileReader(inputFile));

    String line = null;
    
    while ((line = canadianCodeReader.readLine()) != null) {
      String canadianCode = line;
      final ExternalData externalData = sourceConceptToExternalData.get(canadianCode);
      if(externalData != null) {
        externalData.setCanadianSpecificCode(true);
      }
    }
    
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

    final Workbook wb = new XSSFWorkbook();

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

      // MapGroup is ICD-10-CA

      String previousId = null;
      String previousMapPriority = null;
      Record record = null;
      final List<Record> recordList = new ArrayList<>();
      for (final GroupRecord groupRecord : groupRecordList) {
        if (!groupRecord.getCode().equals(previousId)) {
          if (record != null) {
        	record.setCluster(record.getClusterValue());
        	record.setCardinality("1:" + previousMapPriority);
            recordList.add(record);
          }
          record = new Record(groupRecord.getCode(), groupRecord.getTermName());
        }
        
        // ICD-10-CA
        if (groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 1) {
          record.setIcd11Code(groupRecord.getTargetTermCode());
          record.setIcd11Term(groupRecord.getTargetTermName());
        }
        if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 2) {
        	record.setIcd11CodeCluster2(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
        }
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 3) {
			record.setIcd11CodeCluster3(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName())); 	
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 4) {
			record.setIcd11CodeCluster4(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 5) {
			record.setIcd11CodeCluster5(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 6) {
			record.setIcd11CodeCluster6(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 7) {
			record.setIcd11CodeCluster7(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 8) {
			record.setIcd11CodeCluster8(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 9) {
			record.setIcd11CodeCluster9(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 1 && groupRecord.getMapPriority() == 10) {
			record.setIcd11CodeCluster10(Map.of(groupRecord.getTargetTermCode(), groupRecord.getTargetTermName()));
		}
		if(groupRecord.getMapGroup() == 2 && groupRecord.getMapPriority() == 1) {
			record.setWHOMapCode(groupRecord.getTargetTermCode());
	        record.setWHOMapName(groupRecord.getTargetTermName());
		}
        
		previousId = groupRecord.getCode();
        previousMapPriority = Integer.toString(groupRecord.getMapPriority());

      }
      
      final LocalDate localDate = LocalDate.now();
      final DateTimeFormatter formatter =
          DateTimeFormatter.ofPattern("yyyyMMdd");
      final Sheet sheet = wb.createSheet(
          outputFile.replace("{DATE}", localDate.format(formatter)));

      // Create header row and add cells
      int rownum = 0;
      int cellnum = 0;

      Row row = sheet.createRow(rownum++);
      Cell cell = null;
      
      cell = row.createCell(0);
      cell.setCellValue("Source Concept");
      
      cell = row.createCell(2);
      cell.setCellValue("CIHI Map - 1st Entry (Map Group 1)	");
      
      cell = row.createCell(4);
      cell.setCellValue("CIHI Map - Full Expression");
      
      cell = row.createCell(6);
      cell.setCellValue("CIHI map - Mapping Parameters");
      
      cell = row.createCell(9);
      cell.setCellValue("CIHI Map - Additional Entries");

      cell = row.createCell(27);
      cell.setCellValue("Source Concept");
      
      cell = row.createCell(29);
      cell.setCellValue("WHO map (Map Group 2)");
      
      cell = row.createCell(31);
      cell.setCellValue("WHO map - Mapping Parameters");
      
      cell = row.createCell(33);
      cell.setCellValue("Notes");
      
      sheet.addMergedRegion(new CellRangeAddress(0,0,0,1));
      sheet.addMergedRegion(new CellRangeAddress(0,0,2,3));
      sheet.addMergedRegion(new CellRangeAddress(0,0,4,5));
      sheet.addMergedRegion(new CellRangeAddress(0,0,6,8));
      sheet.addMergedRegion(new CellRangeAddress(0,0,9,26));
      sheet.addMergedRegion(new CellRangeAddress(0,0,27,28));
      sheet.addMergedRegion(new CellRangeAddress(0,0,29,30));
      sheet.addMergedRegion(new CellRangeAddress(0,0,31,32));
      sheet.addMergedRegion(new CellRangeAddress(0,0,33,35));
      
      row = sheet.createRow(rownum++);
      

      // ICD 10 CA
      cell = row.createCell(cellnum++);
      cell.setCellValue("ICD-10-CA Code");
      cell = row.createCell(cellnum++);
      cell.setCellValue("ICD-10-CA Code Title");
      // ICD 11
      cell = row.createCell(cellnum++);
      cell.setCellValue("ICD-11 Target Stem Code");
      cell = row.createCell(cellnum++);
      cell.setCellValue("ICD-11 Code Title");
      // CIHI Cluster/Cardinality
      cell = row.createCell(cellnum++);
      cell.setCellValue("ICD-11 Cluster");
      cell = row.createCell(cellnum++);
      cell.setCellValue("Cardinality");
      

      // CIHI Mapping Parameters
      cell = row.createCell(cellnum++);
      cell.setCellValue("Relation -  Target");
      cell = row.createCell(cellnum++);
      cell.setCellValue("Relation - Cluster");
      cell = row.createCell(cellnum++);
      cell.setCellValue("Unmappable Reason");
      
      for(int start = 2; start <= 10; start++) {
    	  // other clusters
          cell = row.createCell(cellnum++);
          cell.setCellValue("ICD-11 code in cluster " + start);
          cell = row.createCell(cellnum++);
          cell.setCellValue("ICD-11 code title in cluster " + start);
      }
      
      // Source Concept
      cell = row.createCell(cellnum++);
      cell.setCellValue("Asterisk");
      cell = row.createCell(cellnum++);
      cell.setCellValue("Canadian Specific Code");
      
      
      // WHO map (Map Group 2)	
      cell = row.createCell(cellnum++);
      cell.setCellValue("ICD-11 code");
      cell = row.createCell(cellnum++);
      cell.setCellValue("ICD-11 title");
      
      // WHO map - Mapping Parameters	
      cell = row.createCell(cellnum++);
      cell.setCellValue("Relation - WHO");
      cell = row.createCell(cellnum++);
      cell.setCellValue("Target Mismatch Reason");
      
      // Notes		
      cell = row.createCell(cellnum++);
      cell.setCellValue("Foundation entity name/title");
      cell = row.createCell(cellnum++);
      cell.setCellValue("Uniform Resource Identified (URI)");
      cell = row.createCell(cellnum++);
      cell.setCellValue("Relation (Foundation entity)");
      
      
      
      Set<String> codeSet = new HashSet<>();
      for (final Record outRecord : recordList) {
        // Add data row
        getLog().info("Add: " + outRecord.toString());
        codeSet.add(outRecord.getIcd10CACode());
        cellnum = 0;
        row = sheet.createRow(rownum++);

        // Lookup external data
        ExternalData externalData = sourceConceptToExternalData.get(outRecord.getIcd10CACode());
        //If none found, create a blank one, but also log a warning
        if(externalData == null) {
          externalData = new ExternalData(outRecord.getIcd10CACode());
          getLog().warn("No external data found for source concept " + outRecord.getIcd10CACode());
        }
        
        // ICD 10 CA
        cell = row.createCell(cellnum++);
        cell.setCellValue(outRecord.getIcd10CACode());
        cell = row.createCell(cellnum++);
        cell.setCellValue(outRecord.getIcd10CATerm());

        // ICD 11
        cell = row.createCell(cellnum++);
        cell.setCellValue(outRecord.getIcd11Code());
        cell = row.createCell(cellnum++);
        cell.setCellValue(outRecord.getIcd11Term());
        
        // Cluster/Cardinality
        cell = row.createCell(cellnum++);
        cell.setCellValue(outRecord.getCluster());
        cell = row.createCell(cellnum++);
        cell.setCellValue(outRecord.getCardinality());

        // Mapping Parameters
        cell = row.createCell(cellnum++);
        cell.setCellValue(externalData.getRelationTarget());
        cell = row.createCell(cellnum++);
        cell.setCellValue(externalData.getRelationCluster());
        cell = row.createCell(cellnum++);
        cell.setCellValue(externalData.getUnmappableReason());
        
        
        List<Map<String, String>> clusters = Arrays.asList(
        		outRecord.icd11CodeCluster2, outRecord.icd11CodeCluster3, outRecord.icd11CodeCluster4,
        		outRecord.icd11CodeCluster5, outRecord.icd11CodeCluster6, outRecord.icd11CodeCluster7,
        		outRecord.icd11CodeCluster8, outRecord.icd11CodeCluster9, outRecord.icd11CodeCluster10
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
            }

        }
     
        cellnum = 27;
        
        //Source Concept
        cell = row.createCell(cellnum++);
        cell.setCellValue(externalData.getAsterisk().toString().toUpperCase());
        cell = row.createCell(cellnum++);
        cell.setCellValue(externalData.getCanadianSpecificCode().toString().toUpperCase());
        

        // WHO mapping
        cell = row.createCell(cellnum++);
        cell.setCellValue(outRecord.getWHOMapCode());
        cell = row.createCell(cellnum++);
        cell.setCellValue(outRecord.getWHOMapName());
       

        // WHO mapping parameters
        cell = row.createCell(cellnum++);
        cell.setCellValue(externalData.getRelationWHO());
        cell = row.createCell(cellnum++);
        cell.setCellValue(externalData.getTargetMismatchReason());
        
        
        // Notes
        cell = row.createCell(cellnum++);
        cell.setCellValue(externalData.getNoteFoundationEntityName());
        cell = row.createCell(cellnum++);
        cell.setCellValue(externalData.getNoteURI());
        cell = row.createCell(cellnum++);
        cell.setCellValue(externalData.getNoteRelation());
        
        
      }

      // write file
      final Path fileLocation = Paths.get(releaseFile.getParent(),
          outputFile.replace("{DATE}", localDate.format(formatter)));

      try (final FileOutputStream outputStream =
          new FileOutputStream(fileLocation.toString());) {
        wb.write(outputStream);
        wb.close();
      }

      getLog().info("Conversion completed " + fileLocation.toString());

    } catch (Exception e) {
      e.printStackTrace();
    }

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
    private Map<String, String> icd11CodeCluster2;
    private Map<String, String> icd11CodeCluster3;
    private Map<String, String> icd11CodeCluster4;
    private Map<String, String> icd11CodeCluster5;
    private Map<String, String> icd11CodeCluster6;
    private Map<String, String> icd11CodeCluster7;
    private Map<String, String> icd11CodeCluster8;
    private Map<String, String> icd11CodeCluster9;
    private Map<String, String> icd11CodeCluster10;
    
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
    public Record(final String icd10CACode, final String icd10CATerm) {
      super();
      this.icd10CACode = icd10CACode;
      this.icd10CATerm = icd10CATerm;
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
	
	public Map<String, String> getIcd11CodeCluster2() {
        return icd11CodeCluster2;
    }

    public void setIcd11CodeCluster2(Map<String, String> icd11CodeCluster2) {
        this.icd11CodeCluster2 = icd11CodeCluster2;
    }

    public Map<String, String> getIcd11CodeCluster3() {
        return icd11CodeCluster3;
    }

    public void setIcd11CodeCluster3(Map<String, String> icd11CodeCluster3) {
        this.icd11CodeCluster3 = icd11CodeCluster3;
    }

    public Map<String, String> getIcd11CodeCluster4() {
        return icd11CodeCluster4;
    }

    public void setIcd11CodeCluster4(Map<String, String> icd11CodeCluster4) {
        this.icd11CodeCluster4 = icd11CodeCluster4;
    }

    public Map<String, String> getIcd11CodeCluster5() {
        return icd11CodeCluster5;
    }

    public void setIcd11CodeCluster5(Map<String, String> icd11CodeCluster5) {
        this.icd11CodeCluster5 = icd11CodeCluster5;
    }

    public Map<String, String> getIcd11CodeCluster6() {
        return icd11CodeCluster6;
    }

    public void setIcd11CodeCluster6(Map<String, String> icd11CodeCluster6) {
        this.icd11CodeCluster6 = icd11CodeCluster6;
    }

    public Map<String, String> getIcd11CodeCluster7() {
        return icd11CodeCluster7;
    }

    public void setIcd11CodeCluster7(Map<String, String> icd11CodeCluster7) {
        this.icd11CodeCluster7 = icd11CodeCluster7;
    }

    public Map<String, String> getIcd11CodeCluster8() {
        return icd11CodeCluster8;
    }

    public void setIcd11CodeCluster8(Map<String, String> icd11CodeCluster8) {
        this.icd11CodeCluster8 = icd11CodeCluster8;
    }

    public Map<String, String> getIcd11CodeCluster9() {
        return icd11CodeCluster9;
    }

    public void setIcd11CodeCluster9(Map<String, String> icd11CodeCluster9) {
        this.icd11CodeCluster9 = icd11CodeCluster9;
    }

    public Map<String, String> getIcd11CodeCluster10() {
        return icd11CodeCluster10;
    }

    public void setIcd11CodeCluster10(Map<String, String> icd11CodeCluster10) {
        this.icd11CodeCluster10 = icd11CodeCluster10;
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
    	return "Record [icd10Code=" + icd10CACode + ", icd10Term=" + icd10CATerm
        + ", icd11Code=" + icd11Code + ", icd11Term=" + icd11Term
        + ", cardinality=" + cardinality + ", cluster=" + cluster
        + ", icd11CodeCluster2=" + icd11CodeCluster2
        + ", icd11CodeCluster3=" + icd11CodeCluster3
        + ", icd11CodeCluster4=" + icd11CodeCluster4
        + ", icd11CodeCluster5=" + icd11CodeCluster5
        + ", icd11CodeCluster6=" + icd11CodeCluster6
        + ", icd11CodeCluster7=" + icd11CodeCluster7
        + ", icd11CodeCluster8=" + icd11CodeCluster8
        + ", icd11CodeCluster9=" + icd11CodeCluster9
        + ", icd11CodeCluster10=" + icd11CodeCluster10
        + ", WHOMapCode=" + WHOMapCode
        + ", WHOMapName=" + WHOMapName +"]";
    }
    
    public String getClusterValue() {
        StringBuilder clusterBuilder = new StringBuilder();

        List<Map<String, String>> clusters = Arrays.asList(
                icd11CodeCluster2, icd11CodeCluster3, icd11CodeCluster4,
                icd11CodeCluster5, icd11CodeCluster6, icd11CodeCluster7,
                icd11CodeCluster8, icd11CodeCluster9, icd11CodeCluster10
        );

        // Iterate over clusters
        for (Map<String, String> currentCluster : clusters) {

            // Check if the cluster is empty (null or empty map)
            if (currentCluster == null || currentCluster.isEmpty()) {
                break; // Stop if current cluster is empty
            }

            // Get the first entry in the map
            Map.Entry<String, String> value = currentCluster.entrySet().iterator().next();

            if (clusterBuilder.length() == 0) {
                // First non-empty cluster starts with icd11Code
                clusterBuilder.append(icd11Code);
            }

            // Append based on the value's first character
            if (value.getKey().startsWith("X")) {
                clusterBuilder.append("&").append(value.getKey());
            } else {
            	clusterBuilder.append("/").append(value.getKey());
            }
        }

        return clusterBuilder.toString();
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
    
    private String relationCluster;
    
    private String unmappableReason;
    
    private String noteFoundationEntityName;

    private String noteURI;

    private String noteRelation;
    
    private String relationWHO;

    private String targetMismatchReason;

    private Boolean asterisk = false;
    
    private Boolean canadianSpecificCode = false;

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

    public String getRelationCluster() {
      return relationCluster;
    }

    public void setRelationCluster(String relationCluster) {
      this.relationCluster = relationCluster;
    }

    public String getUnmappableReason() {
      return unmappableReason;
    }

    public void setUnmappableReason(String unmappableReason) {
      this.unmappableReason = unmappableReason;
    }

    public String getNoteFoundationEntityName() {
      return noteFoundationEntityName;
    }

    public void setNoteFoundationEntityName(String noteFoundationEntityName) {
      this.noteFoundationEntityName = noteFoundationEntityName;
    }

    public String getNoteURI() {
      return noteURI;
    }

    public void setNoteURI(String noteURI) {
      this.noteURI = noteURI;
    }

    public String getNoteRelation() {
      return noteRelation;
    }

    public void setNoteRelation(String noteRelation) {
      this.noteRelation = noteRelation;
    }

    public String getRelationWHO() {
      return relationWHO;
    }

    public void setRelationWHO(String relationWHO) {
      this.relationWHO = relationWHO;
    }

    public String getTargetMismatchReason() {
      return targetMismatchReason;
    }

    public void setTargetMismatchReason(String targetMismatchReason) {
      this.targetMismatchReason = targetMismatchReason;
    }

    public Boolean getAsterisk() {
      return asterisk;
    }

    public void setAsterisk(Boolean asterisk) {
      this.asterisk = asterisk;
    }

    public Boolean getCanadianSpecificCode() {
      return canadianSpecificCode;
    }

    public void setCanadianSpecificCode(Boolean canadianSpecificCode) {
      this.canadianSpecificCode = canadianSpecificCode;
    }
  }
}
