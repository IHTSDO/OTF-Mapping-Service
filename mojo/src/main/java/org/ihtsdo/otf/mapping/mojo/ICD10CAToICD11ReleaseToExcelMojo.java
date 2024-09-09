/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.io.FileOutputStream;
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
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Create the CIHI PHVCS release file from *
 * tls_PhcvsHumanReadableMap_INT_YYYYMMDD.tsv
 * 
 * mvn package -P CihiPhcvsReleaseToExcel "-DreleaseFile=<full path to
 * tls_PhcvsHumanReadableMap_INT_YYYYMMDD.tsv file> "
 * 
 * @goal run-cihi-phcvs-release-to-excel
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

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    getLog().info("Start ICD11 to ICD10CA Release To Excel Mojo");

    if (StringUtils.isEmpty(releaseFile)) {
      getLog().error("Parameter releaseFile is empty.");
      throw new MojoExecutionException("Parameter releaseFile is empty.");
    }

    try {

      final File rf = new File(releaseFile);
      if (!rf.exists()) {
        throw new MojoExecutionException(releaseFile + " file does not exist.");
      }
      runConvert(rf);

    } catch (Exception e) {
      getLog().error("Error running ICD11 to ICD10CA Release To Excel Mojo.", e);
    }
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
          previousId = groupRecord.getCode();
          previousMapPriority = Integer.toString(groupRecord.getMapPriority());
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

      // Create header CellStyle
      final CellStyle styleHeader = sheet.getWorkbook().createCellStyle();
      styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      styleHeader.setBorderBottom(BorderStyle.THIN);
      styleHeader.setBorderTop(BorderStyle.THIN);
      Font font = wb.createFont();
      font.setColor(IndexedColors.BLACK.index);
      font.setBold(true);
      styleHeader.setFont(font);
      
      cell = row.createCell(0);
      styleHeader.setFillBackgroundColor(IndexedColors.PALE_BLUE.index);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("Source Concept");
      
      cell = row.createCell(2);
      styleHeader.setFillBackgroundColor(IndexedColors.BLUE.index);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("CIHI Map - 1st Entry (Map Group 1)	");
      
      cell = row.createCell(4);
      styleHeader.setFillBackgroundColor(IndexedColors.LIGHT_BLUE.index);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("CIHI Map - Full Expression");
      
      cell = row.createCell(6);
      styleHeader.setFillBackgroundColor(IndexedColors.LIGHT_ORANGE.index);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("CIHI Map - Additional Entries");
      
      cell = row.createCell(24);
      styleHeader.setFillBackgroundColor(IndexedColors.ORANGE.index);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("CIHI map - Mapping Parameters");
      
      cell = row.createCell(27);
      styleHeader.setFillBackgroundColor(IndexedColors.GREEN.index);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("WHO map (Map Group 2)");
      
      cell = row.createCell(29);
      styleHeader.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.index);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("WHO map - Mapping Parameters");
      
      cell = row.createCell(31);
      styleHeader.setFillBackgroundColor(IndexedColors.PLUM.index);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("Notes");
      
      sheet.addMergedRegion(new CellRangeAddress(0,1,0,0));
      sheet.addMergedRegion(new CellRangeAddress(2,3,0,0));
      sheet.addMergedRegion(new CellRangeAddress(4,5,0,0));
      sheet.addMergedRegion(new CellRangeAddress(6,23,0,0));
      sheet.addMergedRegion(new CellRangeAddress(24,26,0,0));
      sheet.addMergedRegion(new CellRangeAddress(27,28,0,0));
      sheet.addMergedRegion(new CellRangeAddress(29,30,0,0));
      sheet.addMergedRegion(new CellRangeAddress(31,33,0,0));
      

      // Create styles for alternating rows
      final CellStyle styleA = sheet.getWorkbook().createCellStyle();
      styleA.setFillForegroundColor(IndexedColors.TAN.index);
      styleA.setBorderBottom(BorderStyle.THIN);
      styleA.setBottomBorderColor(IndexedColors.TAN.getIndex());
      styleA.setBorderTop(BorderStyle.THIN);
      styleA.setTopBorderColor(IndexedColors.TAN.getIndex());

      styleA.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      final CellStyle styleB = sheet.getWorkbook().createCellStyle();
      styleB.setFillPattern(FillPatternType.NO_FILL);

      // ICD 10 CA
      cell = row.createCell(cellnum++);
      styleHeader.setFillBackgroundColor(IndexedColors.PALE_BLUE.index);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("ICD-10-CA Code");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("ICD-10-CA Code Title");
      // ICD 11
      styleHeader.setFillBackgroundColor(IndexedColors.DARK_BLUE.index);
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("ICD-11 Code");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("ICD-11 Code Title");
      // CIHI Cluster/Cardinality
      styleHeader.setFillBackgroundColor(IndexedColors.LIGHT_BLUE.index);
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("ICD-11 Cluster");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("Cardinality");
      
      for(int start = 2; start <= 10; start++) {
    	  // other clusters
    	  styleHeader.setFillBackgroundColor(IndexedColors.LIGHT_ORANGE.index);
          cell = row.createCell(cellnum++);
          cell.setCellStyle(styleHeader);
          cell.setCellValue("ICD-11 code in cluster " + start);
          cell = row.createCell(cellnum++);
          cell.setCellStyle(styleHeader);
          cell.setCellValue("ICD-11 code title in cluster " + start);
      }
      
      // CIHI Mapping Parameters
      styleHeader.setFillBackgroundColor(IndexedColors.ORANGE.index);
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("Relation -  Target");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("Relation - Cluster");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("Unmappable Reason");
      
      // WHO map (Map Group 2)	
	  styleHeader.setFillBackgroundColor(IndexedColors.GREEN.index);
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("ICD-11 code");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("ICD-11 title");
      
      // WHO map - Mapping Parameters	
	  styleHeader.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.index);
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("Relation - WHO");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("Target Mismatch Reason");
      
      // Notes		
	  styleHeader.setFillBackgroundColor(IndexedColors.PLUM.index);
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("Foundation entity name/title");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("Uniform Resource Identified (URI)");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("Relation (Foundation entity)");
      
      Set<String> codeSet = new HashSet<>();
      for (final Record outRecord : recordList) {
        // Add data row
        getLog().info("Add: " + outRecord.toString());
        codeSet.add(outRecord.getIcd10CACode());
        cellnum = 0;
        row = sheet.createRow(rownum++);

        // ICD 10 CA
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getIcd10CACode());
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getIcd10CATerm());

        // ICD 11
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getIcd11Code());
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getIcd11Term());
        
        // Cluster/Cardinality
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getCluster());
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getCardinality());
        
        // Cluster/Cardinality
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getCluster());
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getCardinality());
        
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
                cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
                cell.setCellValue(key);
                cell = row.createCell(cellnum++);
                cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
                cell.setCellValue(value);
            }

        }
        cellnum = 27;
        // WHO mapping
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getWHOMapCode());
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getWHOMapName());

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
            String value = currentCluster.values().iterator().next();

            if (clusterBuilder.length() == 0) {
                // First non-empty cluster starts with icd11Code
                clusterBuilder.append(icd11Code);
            }

            // Append based on the value's first character
            if (value.startsWith("X")) {
                clusterBuilder.append("&").append(value);
            } else {
                clusterBuilder.append("/").append(value);
            }
        }

        return clusterBuilder.toString();
    }

  }
}
