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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
public class CihiPhcvsReleaseToExcelMojo extends AbstractMojo {

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

    getLog().info("Start CIHI PHCVS Release To Excel Mojo");

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
      getLog().error("Error running CIHI PHCVS Release To Excel Mojo.", e);
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

      // MapGroup 1 is ICD-10-CA,
      // MapGroup 2 is ICD-9,
      // MapGroup 3 is CED-DxS.

      String previousId = null;
      Integer previousMapPriority = null;
      Record record = null;
      final List<Record> recordList = new ArrayList<>();
      for (final GroupRecord groupRecord : groupRecordList) {
        if (!groupRecord.getCode().equals(previousId)
            || groupRecord.getMapPriority() != previousMapPriority) {
          if (record != null) {
            recordList.add(record);
          }
          record = new Record(groupRecord.getCode(), groupRecord.getTermName());
        }
        // ICD-10-CA
        if (groupRecord.getMapGroup() == 1) {
          record.setIcd10Code(groupRecord.getTargetTermCode());
          record.setIcd10Term(groupRecord.getTargetTermName());
        }

        // ICD 9
        if (groupRecord.getMapGroup() == 2) {
          record
              .setIcd9Code(groupRecord.getTargetTermCode().replaceAll("-", ""));
          record.setIcd9Term(groupRecord.getTargetTermName());
        }
        // CED-DxS
        if (groupRecord.getMapGroup() == 3) {
          record.setCedDxsCode(groupRecord.getTargetTermCode());
          record.setCedDxsTerm(groupRecord.getTargetTermName());
        }

        previousId = groupRecord.getCode();
        previousMapPriority = groupRecord.getMapPriority();
      }
      recordList.add(record);

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
      styleHeader.setFillForegroundColor(IndexedColors.ORANGE.index);
      styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      styleHeader.setBorderBottom(BorderStyle.THIN);
      styleHeader.setBottomBorderColor(IndexedColors.ORANGE.getIndex());
      styleHeader.setBorderTop(BorderStyle.THIN);
      styleHeader.setTopBorderColor(IndexedColors.ORANGE.getIndex());
      Font font = wb.createFont();
      font.setColor(IndexedColors.WHITE.index);
      font.setBold(true);
      styleHeader.setFont(font);

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

      // Snomed
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("SNOMED_ID");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("SNOMED_Term");
      // ICD 10 CA
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("ICD10CA_Code");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("ICD10CA_Term");
      // ICD 9
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("ICD9_Code");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("ICD9CA_Term");
      // CedDxs
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("CedDxs_code");
      cell = row.createCell(cellnum++);
      cell.setCellStyle(styleHeader);
      cell.setCellValue("CedDxs_Common Term");

      Set<String> codeSet = new HashSet<>();
      for (final Record outRecord : recordList) {
        // Add data row
        getLog().info("Add: " + outRecord.toString());
        codeSet.add(outRecord.getSnomedCode());
        cellnum = 0;
        row = sheet.createRow(rownum++);

        // Snomed
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getSnomedCode());
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getSnomedTerm());

        // ICD 10
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getIcd10Code());
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getIcd10Term());

        // ICD 9
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getIcd9Code());
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getIcd9Term());

        // CedDxs
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getCedDxsCode());
        cell = row.createCell(cellnum++);
        cell.setCellStyle((codeSet.size() % 2 == 0 ? styleA : styleB));
        cell.setCellValue(outRecord.getCedDxsTerm());

      }

      mergeMapTargetCells(sheet, 2);
      mergeMapTargetCells(sheet, 4);
      mergeMapTargetCells(sheet, 6);
      mergeMapSourceCells(sheet, 0);

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
   * Merge map source cells.
   *
   * @param sheet the sheet
   * @param startColumn the start column
   */
  private void mergeMapSourceCells(final Sheet sheet, final int startColumn) {
    // merge cells in column A and B if the values are the same as the
    // previous row.
    int first = 1;
    int last = 1;
    int i = 1;

    while (i < sheet.getPhysicalNumberOfRows()) {
      first = i;
      last = i;
      for (int j = i + 1; j < sheet.getPhysicalNumberOfRows(); j++) {
        if (sheet.getRow(i).getCell(0).toString()
            .equals(sheet.getRow(j).getCell(0).toString())) {
          last = j;
        }
      }
      final int NON_REPTNG_COLS = 2;
      for (int k = startColumn; k < NON_REPTNG_COLS; k++) {
        if (last > first) {
          CellRangeAddress cellRangeAddress =
              new CellRangeAddress(first, last, k, k);
          sheet.addMergedRegion(cellRangeAddress);
        }
      }
      i = last + 1;
    }

  }

  /**
   * Merge map target cells.
   *
   * @param sheet the sheet
   * @param startColumn the start column
   */
  private void mergeMapTargetCells(final Sheet sheet, final int startColumn) {
    // merge cells in startColumn and next column if the value is empty and
    // the previous values is not for a map source
    int first = 1;
    int last = 1;
    int i = 1;

    while (i < sheet.getPhysicalNumberOfRows()) {
      first = i;
      last = i;
      for (int j = i + 1; j < sheet.getPhysicalNumberOfRows(); j++) {

        if (sheet.getRow(i).getCell(0).toString()
            .equals(sheet.getRow(j).getCell(0).toString()) &&

            StringUtils
                .isNotBlank(sheet.getRow(i).getCell(startColumn).toString())
            && StringUtils
                .isBlank(sheet.getRow(j).getCell(startColumn).toString())) {

          last = j;
        }
      }
      final int NON_REPTNG_COLS = startColumn + 2;
      for (int k = startColumn; k < NON_REPTNG_COLS; k++) {
        if (last > first) {
          CellRangeAddress cellRangeAddress =
              new CellRangeAddress(first, last, k, k);
          sheet.addMergedRegion(cellRangeAddress);
        }
      }
      i = last + 1;
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

    /** The snomed code. */
    private String snomedCode;

    /** The snomed term. */
    private String snomedTerm;

    /** The icd 10 code. */
    private String icd10Code;

    /** The icd 10 term. */
    private String icd10Term;

    /** The icd 9 code. */
    private String icd9Code;

    /** The icd 9 term. */
    private String icd9Term;

    /** The ced dxs code. */
    private String cedDxsCode;

    /** The ced dxs term. */
    private String cedDxsTerm;

    /**
     * Instantiates a {@link Record} from the specified parameters.
     *
     * @param snomedCode the snomed code
     * @param snomedTerm the snomed term
     */
    public Record(final String snomedCode, final String snomedTerm) {
      super();
      this.snomedCode = snomedCode;
      this.snomedTerm = snomedTerm;
    }

    /**
     * Returns the snomed code.
     *
     * @return the snomed code
     */
    public String getSnomedCode() {
      return snomedCode;
    }

    /**
     * Sets the snomed code.
     *
     * @param snomedCode the snomed code
     */
    public void setSnomedCode(final String snomedCode) {
      this.snomedCode = snomedCode;
    }

    /**
     * Returns the snomed term.
     *
     * @return the snomed term
     */
    public String getSnomedTerm() {
      return snomedTerm;
    }

    /**
     * Sets the snomed term.
     *
     * @param snomedTerm the snomed term
     */
    public void setSnomedTerm(final String snomedTerm) {
      this.snomedTerm = snomedTerm;
    }

    /**
     * Returns the icd 10 code.
     *
     * @return the icd 10 code
     */
    public String getIcd10Code() {
      return icd10Code;
    }

    /**
     * Sets the icd 10 code.
     *
     * @param icd10Code the icd 10 code
     */
    public void setIcd10Code(final String icd10Code) {
      this.icd10Code = icd10Code;
    }

    /**
     * Returns the icd 10 term.
     *
     * @return the icd 10 term
     */
    public String getIcd10Term() {
      return icd10Term;
    }

    /**
     * Sets the icd 10 term.
     *
     * @param icd10Term the icd 10 term
     */
    public void setIcd10Term(final String icd10Term) {
      this.icd10Term = icd10Term;
    }

    /**
     * Returns the icd 9 code.
     *
     * @return the icd 9 code
     */
    public String getIcd9Code() {
      return icd9Code;
    }

    /**
     * Sets the icd 9 code.
     *
     * @param icd9Code the icd 9 code
     */
    public void setIcd9Code(final String icd9Code) {
      this.icd9Code = icd9Code;
    }

    /**
     * Returns the icd 9 term.
     *
     * @return the icd 9 term
     */
    public String getIcd9Term() {
      return icd9Term;
    }

    /**
     * Sets the icd 9 term.
     *
     * @param icd9Term the icd 9 term
     */
    public void setIcd9Term(final String icd9Term) {
      this.icd9Term = icd9Term;
    }

    /**
     * Returns the ced dxs code.
     *
     * @return the ced dxs code
     */
    public String getCedDxsCode() {
      return cedDxsCode;
    }

    /**
     * Sets the ced dxs code.
     *
     * @param cedDxsCode the ced dxs code
     */
    public void setCedDxsCode(final String cedDxsCode) {
      this.cedDxsCode = cedDxsCode;
    }

    /**
     * Returns the ced dxs term.
     *
     * @return the ced dxs term
     */
    public String getCedDxsTerm() {
      return cedDxsTerm;
    }

    /**
     * Sets the ced dxs term.
     *
     * @param cedDxsTerm the ced dxs term
     */
    public void setCedDxsTerm(final String cedDxsTerm) {
      this.cedDxsTerm = cedDxsTerm;
    }

    /* see superclass */
    @Override
    public String toString() {
      return "Record [snomedCode=" + snomedCode + ", snomedTerm=" + snomedTerm
          + ", icd10Code=" + icd10Code + ", icd10Term=" + icd10Term
          + ", icd9Code=" + icd9Code + ", icd9Term=" + icd9Term
          + ", cedDxsCode=" + cedDxsCode + ", cedDxsTerm=" + cedDxsTerm + "]";
    }

  }
}
