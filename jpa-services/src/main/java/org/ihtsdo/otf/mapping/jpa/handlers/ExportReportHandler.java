package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportResult;
import org.ihtsdo.otf.mapping.reports.ReportResultItem;

/**
 * A handler for exporting a {@link Report}.
 */
public class ExportReportHandler {

    /**
     * Instantiates an empty {@link ExportReportHandler}.
     */
    public ExportReportHandler() {

    }

    /**
     * Export report.
     *
     * @param report
     *            the report
     * @return the input stream
     * @throws Exception
     *             the exception
     */
    public InputStream exportReport(Report report) throws Exception {

        // Create workbook
        Workbook wb = new HSSFWorkbook();

        // Export report
        handleExportReport(report, wb);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        InputStream in = new ByteArrayInputStream(out.toByteArray());
        return in;

    }

    /**
     * Handle export report.
     *
     * @param report
     *            the report
     * @param wb
     *            the wb
     * @throws Exception
     *             the exception
     */
    @SuppressWarnings("static-method")
    private void handleExportReport(Report report, Workbook wb) throws Exception {
        Logger.getLogger(ReportServiceJpa.class).info("Exporting report " + report.getName() + "...");

        try {

            CreationHelper createHelper = wb.getCreationHelper();
            // Set font
            Font font = wb.createFont();
            font.setFontName("Cambria");
            font.setFontHeightInPoints((short) 11);

            // Fonts are set into a style
            CellStyle style = wb.createCellStyle();
            style.setFont(font);

            Sheet sheet = wb.createSheet("Report");

            // Create header row and add cells
            int rownum = 0;
            int cellnum = 0;
            Row row = sheet.createRow(rownum++);
            Cell cell = null;
            for (String header : new String[] { "Count", "Value" }) {
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(header));
            }

            for (ReportResult result : report.getResults()) {
                // Add data row
                cellnum = 0;
                row = sheet.createRow(rownum++);

                // Count
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(new Long(result.getCt()).toString()));

                // Value
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(result.getValue().toString()));
            }

            Sheet sheet2 = wb.createSheet("Report Results");

            // Create header row and add cells
            rownum = 0;
            cellnum = 0;
            row = sheet2.createRow(rownum++);
            for (String header : new String[] { "Count", "Value" }) {
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(header));
            }

            for (ReportResult result : report.getResults()) {
                // Add data row
                cellnum = 0;
                row = sheet2.createRow(rownum++);

                // Count
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(new Long(result.getCt()).toString()));

                // Value
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(result.getValue().toString()));

                row = sheet2.createRow(rownum++);

                for (String header : new String[] { "Id", "Name" }) {
                    cell = row.createCell(cellnum++);
                    cell.setCellStyle(style);
                    cell.setCellValue(createHelper.createRichTextString(header));
                }

                // limit results so as not to exceeed (hopefully) the 65535 row
                // limit for HSSF
                if (result.getCt() < 2000) {

                    for (ReportResultItem resultItem : result.getReportResultItems()) {

                      // Add data row
                      cellnum = 2;
                      row = sheet2.createRow(rownum++);

                      // Id
                      cell = row.createCell(cellnum++);
                      cell.setCellStyle(style);
                      cell.setCellValue(createHelper.createRichTextString(resultItem.getItemId().toString()));

                      // Name
                      cell = row.createCell(cellnum++);
                      cell.setCellStyle(style);
                      cell.setCellValue(createHelper.createRichTextString(resultItem.getItemName()));
                    }
                } else {
                  // Add data row
                  cellnum = 2;
                  row = sheet2.createRow(rownum++);

                  // Id
                  cell = row.createCell(cellnum++);
                  cell.setCellStyle(style);
                  cell.setCellValue(createHelper.createRichTextString("There are too many rows to export (2000 max)"));

                  // Name
                  cell = row.createCell(cellnum++);
                  cell.setCellStyle(style);
                  cell.setCellValue(createHelper.createRichTextString(String.valueOf(result.getCt())));
                  
                }
                   
            }

            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
                sheet2.autoSizeColumn(i);
            }
        } catch (Exception e) {
            throw new LocalException(e.getMessage(), e);
        }

    }
    
    /**
     * Export file comparison report.
     *
     * @param report
     *            the report
     * @return the input stream
     * @throws Exception
     *             the exception
     */
    public InputStream exportExtendedFileComparisonReport(TreeMap<String, String> updatedList, Map<String, String> newList, Map<String, 
      String> inactivatedList, Map<String, String> removedList ) throws Exception {

        // Create workbook
        Workbook wb = new HSSFWorkbook();

        // Export report
        handleExportExtendedFileComparisonReport(updatedList, newList, inactivatedList, removedList, wb);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        InputStream in = new ByteArrayInputStream(out.toByteArray());
        return in;

    }
    
    private void makeExtendedRowHelper(String[] tokens, Workbook wb, Sheet sheet, int rownum, CellStyle style) {
      CreationHelper createHelper = wb.getCreationHelper();
      
      Cell cell = null;
      int cellnum = 0;

      Row row = sheet.createRow(rownum);
      
      // UUID
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[0]));
      
      // Effective Time
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[1]));
      
      // Acitve
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[2]));
      
      // Module Id
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[3]));
      
      // Refset Id
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[4]));
      
      // Referenced Component Id
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[5]));
      
      // Map Group
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[6]));
      
      // Map Priority
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[7]));
      
      // Map Rule
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[8]));
      
      // Map Advice
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[9]));
      
      // Map Target
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[10]));
      
      // Correlation Id
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[11]));
      
      // Map Category Id
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[12]));
    }
    
    /**
     * Handle export report.
     *
     * @param report
     *            the report
     * @param wb
     *            the wb
     * @throws Exception
     *             the exception
     */
    @SuppressWarnings("static-method")
    private void handleExportExtendedFileComparisonReport(TreeMap<String, String> updatedList, 
      Map<String, String> newList, Map<String, String> inactivatedList, Map<String, String> removedList, Workbook wb) throws Exception {
        Logger.getLogger(ReportServiceJpa.class).info("Exporting file comparison report " );

        try {

            CreationHelper createHelper = wb.getCreationHelper();
            // Set font
            Font font = wb.createFont();
            font.setFontName("Cambria");
            font.setFontHeightInPoints((short) 11);

            // Fonts are set into a style
            CellStyle style = wb.createCellStyle();
            style.setFont(font);

            Sheet sheet = wb.createSheet("New Records");
            Sheet sheet2 = wb.createSheet("Updated Records");
            Sheet sheet3 = wb.createSheet("Inactivated Records");
            Sheet sheet4 = wb.createSheet("Removed Records");

            // Create sheet1 for new records
            int rownum = 0;
            int cellnum = 0;
            Row row = sheet.createRow(rownum++);
            Cell cell = null;
            for (String header : new String[] {"UUID", "Effective Time", "Active", "ModuleId", "RefsetId", 
                "ReferencedComponentId", "MapGroup", "MapPriority", "MapRule", "MapAdvice", "MapTarget", "CorrelationId", "MapCategoryId" }) {
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(header));
            }

            // Add new records to sheet1
            for (Entry<String, String> entry : newList.entrySet()) {
                // Add data row
                String[] tokens = entry.getValue().split("\t");
                makeExtendedRowHelper(tokens, wb, sheet, rownum++, style);
            }
            
            // Create sheet2 for updated records
            rownum = 0;
            cellnum = 0;
            row = sheet2.createRow(rownum++);
            cell = null;
            for (String header : new String[] {"", "Effective Time", "Active", "ModuleId", "RefsetId", 
                "ReferencedComponentId", "MapGroup", "MapPriority", "MapRule", "MapAdvice", "MapTarget", "CorrelationId", "MapCategoryId" }) {
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(header));
            }
            // Add updated records to sheet2
            for (Entry<String, String> entry : updatedList.entrySet()) {
              
              String tokens[] = entry.getValue().split("\t");
              makeExtendedRowHelper(tokens, wb, sheet2, rownum, style);
              rownum++;
              
              // updated rows show both the original record and the updated record
              // so make second row for updated record
              String[] tokensRow2 = new String[13];
              for (int d = 13, i = 0; d<tokens.length; d++, i++) {
                tokensRow2[i] = tokens[d];
              }              
              makeExtendedRowHelper(tokensRow2, wb, sheet2, rownum++, style);
              
              // make blank row to separate records
              row = sheet2.createRow(rownum++);
            }

            // Create sheet3 for inactivated records
            rownum = 0;
            cellnum = 0;
            row = sheet3.createRow(rownum++);
            for (String header : new String[] {"UUID", "Effective Time", "Active", "ModuleId", "RefsetId", 
                "ReferencedComponentId", "MapGroup", "MapPriority", "MapRule", "MapAdvice", "MapTarget", "CorrelationId", "MapCategoryId" }) {
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(header));
            }

            // Add inactivated records to sheet3
            for (Entry<String, String> entry : inactivatedList.entrySet()) {
                // Add data row
                String[] tokens = entry.getValue().split("\t");
                makeExtendedRowHelper(tokens, wb, sheet3, rownum++, style);
            }

            // Create sheet4 for removed records
            rownum = 0;
            cellnum = 0;
            row = sheet4.createRow(rownum++);
            for (String header : new String[] {"UUID", "Effective Time", "Active", "ModuleId", "RefsetId", 
                "ReferencedComponentId", "MapGroup", "MapPriority", "MapRule", "MapAdvice", "MapTarget", "CorrelationId", "MapCategoryId" }) {
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(header));
            }

            // Add removed records to sheet4
            for (Entry<String, String> entry : removedList.entrySet()) {
                // Add data row
                String[] tokens = entry.getValue().split("\t");
                makeExtendedRowHelper(tokens, wb, sheet4, rownum++, style);
            }
            for (int i = 0; i < 26; i++) {
                sheet.autoSizeColumn(i);
                sheet2.autoSizeColumn(i);
                sheet3.autoSizeColumn(i);
                sheet4.autoSizeColumn(i);
            }
            sheet.setColumnWidth(8, 50);
            sheet.setColumnWidth(9, 50);
            sheet2.setColumnWidth(8, 50);
            sheet2.setColumnWidth(9, 50);
            sheet3.setColumnWidth(8, 50);
            sheet3.setColumnWidth(9, 50);
            sheet4.setColumnWidth(8, 50);
            sheet4.setColumnWidth(9, 50);
            
        } catch (Exception e) {
            throw new LocalException(e.getMessage(), e);
        }

    }
    
    public InputStream exportSimpleFileComparisonReport(TreeMap<String, String> updatedList, Map<String, String> newList, 
      Map<String, String> inactivatedList, Map<String, String> removedList ) throws Exception {

      // Create workbook
      Workbook wb = new HSSFWorkbook();

      // Export report
      handleExportSimpleFileComparisonReport(updatedList, newList, inactivatedList, removedList, wb);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      wb.write(out);
      InputStream in = new ByteArrayInputStream(out.toByteArray());
      return in;

  }
    
    /**
     * Handle export report.
     *
     * @param report
     *            the report
     * @param wb
     *            the wb
     * @throws Exception
     *             the exception
     */
    @SuppressWarnings("static-method")
    private void handleExportSimpleFileComparisonReport(TreeMap<String, String> updatedList, 
      Map<String, String> newList, Map<String, String> inactivatedList, 
      Map<String, String> removedList, Workbook wb) throws Exception {
        Logger.getLogger(ReportServiceJpa.class).info("Exporting file comparison report " );

        try {

            CreationHelper createHelper = wb.getCreationHelper();
            // Set font
            Font font = wb.createFont();
            font.setFontName("Cambria");
            font.setFontHeightInPoints((short) 11);

            // Fonts are set into a style
            CellStyle style = wb.createCellStyle();
            style.setFont(font);

            Sheet sheet = wb.createSheet("New Records");
            Sheet sheet2 = wb.createSheet("Updated Records");
            Sheet sheet3 = wb.createSheet("Inactivated Records");
            Sheet sheet4 = wb.createSheet("Removed Records");

            // Create sheet1 for new records
            int rownum = 0;
            int cellnum = 0;
            Row row = sheet.createRow(rownum++);
            Cell cell = null;
            for (String header : new String[] {"UUID", "Effective Time", "Active", "ModuleId", "RefsetId", 
                "ReferencedComponentId", "MapTarget"}) {
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(header));
            }

            // Add new records to sheet1
            for (Entry<String, String> entry : newList.entrySet()) {
                // Add data row
                String[] tokens = entry.getValue().split("\t");
                makeSimpleRowHelper(tokens, wb, sheet, rownum++, style);
            }
            
            // Create sheet2 for updated records
            rownum = 0;
            cellnum = 0;
            row = sheet2.createRow(rownum++);
            cell = null;
            for (String header : new String[] {"", "Effective Time", "Active", "ModuleId", "RefsetId", 
                "ReferencedComponentId", "MapTarget"}) {
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(header));
            }
            // Add updated records to sheet2
            for (Entry<String, String> entry : updatedList.entrySet()) {
              
              String tokens[] = entry.getValue().split("\t");
              makeSimpleRowHelper(tokens, wb, sheet2, rownum, style);
              rownum++;
              
              // updated rows show both the original record and the updated record
              // so make second row for updated record
              String[] tokensRow2 = new String[7];
              for (int d = 7, i = 0; d<tokens.length; d++, i++) {
                tokensRow2[i] = tokens[d];
              }              
              makeSimpleRowHelper(tokensRow2, wb, sheet2, rownum++, style);
              
              // make blank row to separate records
              row = sheet2.createRow(rownum++);
              
            }

            // Create sheet3 for inactivated records
            rownum = 0;
            cellnum = 0;
            row = sheet3.createRow(rownum++);
            for (String header : new String[] {"UUID", "Effective Time", "Active", "ModuleId", "RefsetId", 
                "ReferencedComponentId", "MapTarget"}) {
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(header));
            }

            // Add inactivated records to sheet3
            for (Entry<String, String> entry : inactivatedList.entrySet()) {
                // Add data row
                String[] tokens = entry.getValue().split("\t");
                makeSimpleRowHelper(tokens, wb, sheet3, rownum++, style);
            }
            
            // Create sheet4 for removed records
            rownum = 0;
            cellnum = 0;
            row = sheet4.createRow(rownum++);
            for (String header : new String[] {"UUID", "Effective Time", "Active", "ModuleId", "RefsetId", 
                "ReferencedComponentId", "MapTarget"}) {
                cell = row.createCell(cellnum++);
                cell.setCellStyle(style);
                cell.setCellValue(createHelper.createRichTextString(header));
            }

            // Add removed records to sheet4
            for (Entry<String, String> entry : removedList.entrySet()) {
                // Add data row
                String[] tokens = entry.getValue().split("\t");
                makeSimpleRowHelper(tokens, wb, sheet4, rownum++, style);
            }


            for (int i = 0; i < 26; i++) {
                sheet.autoSizeColumn(i);
                sheet2.autoSizeColumn(i);
                sheet3.autoSizeColumn(i);
                sheet4.autoSizeColumn(i);
            }
            sheet.setColumnWidth(8, 50);
            sheet.setColumnWidth(9, 50);
            sheet2.setColumnWidth(8, 50);
            sheet2.setColumnWidth(9, 50);
            sheet3.setColumnWidth(8, 50);
            sheet3.setColumnWidth(9, 50);
            sheet4.setColumnWidth(8, 50);
            sheet4.setColumnWidth(9, 50);
            
        } catch (Exception e) {
            throw new LocalException(e.getMessage(), e);
        }

    }
    
    private void makeSimpleRowHelper(String[] tokens, Workbook wb, Sheet sheet, int rownum, CellStyle style) {
      CreationHelper createHelper = wb.getCreationHelper();
      
      Cell cell = null;
      int cellnum = 0;

      Row row = sheet.createRow(rownum);
      
      // UUID
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[0]));
      
      // Effective Time
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[1]));
      
      // Acitve
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[2]));
      
      // Module Id
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[3]));
      
      // Refset Id
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[4]));
      
      // Referenced Component Id
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[5]));
      
      // Map Target
      cell = row.createCell(cellnum++);
      cell.setCellStyle(style);
      cell.setCellValue(createHelper.createRichTextString(tokens[6]));
      
 
    }
}