/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Create the CIHI CCI RF2 import files from *
 * der2_iissscRefset_ComplexMapActiveSnapshot_INT_YYYYMMDD.txt
 * 
 * mvn package -PCihiCCIReleaseToRF2Import "-DreleaseFile=<full path to
 * der2_iissscRefset_ComplexMapActiveSnapshot_INT_YYYYMMDD.txt file>
 * -Dcurrent.refset.id=<current, published project refset id>"
 * -Dnew.refset.id=<new project refset id>"
 * 
 * @goal cihi-cci-release-to-rf2-import
 */
public class CCIReleaseToRF2ImportMojo extends AbstractMojo {

  /**
   * Release file.
   *
   * @parameter
   * @required
   */
  private String releaseFile;

  /**
   * Current refset id.
   *
   * @parameter
   * @required
   */
  private String currentRefsetId;

  /**
   * New refset id.
   *
   * @parameter
   * @required
   */
  private String newRefsetId;

  /**
   * Map project Id (looked up from currentRefsetId).
   */
  private Long mapProjectId;

  private Map<String, ExternalData> sourceConceptToExternalData = new HashMap<>();

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    getLog().info("Start CCI Release To RF2 Import Mojo");

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
      getLog().error("Error running CCI Release To RF2 Import Mojo.", e);
    }
  }

  //
  // Look up additional data not present in the release file
  // Most are pulled from database objects, but a few are pulled from external
  // files
  //
  private void getExternalData() throws Exception {

    getLog().info("Gathering mapping data stored outside of the release file");

    // Get all map records for the project

    MappingService mappingService = new MappingServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    MapProject mapProject = mappingService.getMapProjectForRefSetId(currentRefsetId);
    mapProjectId = mapProject.getId();

    MapRecordList allMapRecords = mappingService.getMapRecordsForMapProject(mapProjectId);

    for (MapRecord mapRecord : allMapRecords.getMapRecords()) {
      // Only process READY_FOR_PUBLICATION and PUBLISHED records
      if (!(mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)
          || mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED))) {
        continue;
      }

      // Create an External Data object, based on the source concept
      ExternalData externalData = new ExternalData(mapRecord.getConceptId());

      // Look up map notes
      Set<MapNote> mapNotes = mapRecord.getMapNotes();
      if (mapNotes != null) {
        externalData.setMapNotes(mapNotes);
      }

      // Look up Additional Entry Info
      for (MapEntry mapEntry : mapRecord.getMapEntries()) {
        for (AdditionalMapEntryInfo additionalMapEntryInfo : mapEntry
            .getAdditionalMapEntryInfos()) {
          // Set the grade and unmappable reason values for specific map location (grade + "|" + priority).
          if (additionalMapEntryInfo.getField().equals("Grade")) {
            externalData.setGradeAtLocation(mapEntry.getMapGroup() + "|" + mapEntry.getMapPriority(), additionalMapEntryInfo.getValue());
          } else if (additionalMapEntryInfo.getField().equals("Unmappable Reason")) {
            externalData.setUnmappableReasonAtLocation(mapEntry.getMapGroup() + "|" + mapEntry.getMapPriority(), additionalMapEntryInfo.getValue());
          }
        }
      }

      // Add the External Data object to the map
      sourceConceptToExternalData.put(mapRecord.getConceptId(), externalData);

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

    try {

      // Use the records to create an RF2 file including additional map entry
      // info
      createRF2WithAdditionalInfo(releaseFile);

      // Now create a Map Notes RF2 file
      createMapNotesRF2(releaseFile);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private void createRF2WithAdditionalInfo(final File releaseFile) throws Exception {

    String outputFilePath = releaseFile.getParent() + "/" + releaseFile.getName().replace(".txt",
        "_WithAdditionalInfo_For" + newRefsetId + "Import.txt");

    try (BufferedReader reader = new BufferedReader(new FileReader(releaseFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

      String line;
      while ((line = reader.readLine()) != null) {
        // Split the line into columns using tab as a delimiter
        String[] columns = line.split("\t");

        // If this is an empty line, skip
        if (columns.length < 1) {
          continue;
        }

        StringBuilder newLine = new StringBuilder();

        // Handle header line, adding additional info to the existing
        if (columns[0].equals("id")) {
          newLine.append(String.join("\t", columns));
          newLine.append("\t").append("Grade").append("\t").append("Unmappable Reason").append("\t")
              .append("END");
        }
        // Handle data rows
        else {
          // Update refset id for new project
          columns[4] = newRefsetId;

          // If correlationId is null, change to 0
          if (columns[11].equals("null")) {
            columns[11] = "0";
          }

          newLine.append(String.join("\t", columns));

          // Look up external data, based on source concept id
          final String sourceConcept = columns[5];

          ExternalData externalData = sourceConceptToExternalData.get(sourceConcept);

          if (externalData == null) {
            newLine.append("\tNo external data found");
            // Write the modified line to the output file
            writer.write(newLine.toString());
            writer.newLine();
            continue;
          }

          // Add additional map entry info based on the group and priority
          final String group = columns[6];
          final String priority = columns[7];

          // Check for grade
          if (externalData.getGradeMap().get(group + "|" + priority) != null) {
            newLine.append("\t").append(externalData.getGradeMap().get(group + "|" + priority));
          } else {
            newLine.append("\t").append("");
          }

          //  Check for unmappable reason 
          if (externalData.getUnmappableReasonMap().get(group + "|" + priority) != null) {
            newLine.append("\t").append(externalData.getUnmappableReasonMap().get(group + "|" + priority));
          } else {
            newLine.append("\t").append("");
          }

          // Finally, add "END" (this is used by the Additional Map Entry Info
          // loader)
          newLine.append("\t").append("END");

        }

        // Write the modified line to the output file
        writer.write(newLine.toString());
        writer.newLine();
      }

      System.out.println("Conversion completed. Updated data written to " + outputFilePath);

    } catch (IOException e) {
      System.err.println("Error processing the file: " + e.getMessage());
    }
  }

  private void createMapNotesRF2(final File releaseFile) throws Exception {

    String outputFilePath = releaseFile.getParent() + "/"
        + releaseFile.getName().replace(".txt", "_MapNotes_For" + newRefsetId + "Import.txt");

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

      StringBuilder newLine = new StringBuilder();

      // Write the header line
      newLine.append("id").append("\t").append("effectiveTime").append("\t").append("active")
          .append("\t").append("moduleId").append("\t").append("refsetId").append("\t")
          .append("referencedComponentId").append("\t").append("referencedComponentName")
          .append("\t").append("note");

      writer.write(newLine.toString());
      writer.newLine();

      // Sort the source concept ids
      List<String> sortedSourceConceptIds = new ArrayList<>(sourceConceptToExternalData.keySet());
      Collections.sort(sortedSourceConceptIds);

      SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

      // For every source concept id, add a line for every map note
      for (String sourceConceptId : sortedSourceConceptIds) {
        ExternalData externalData = sourceConceptToExternalData.get(sourceConceptId);
        for (MapNote mapNote : externalData.getMapNotes()) {
          newLine = new StringBuilder();
          newLine.append("").append("\t").append(formatter.format(mapNote.getTimestamp()))
              .append("\t").append("1").append("\t").append("2").append("\t").append(newRefsetId)
              .append("\t").append(sourceConceptId).append("\t").append("").append("\t")
              .append(mapNote.getNote());

          // Write the modified line to the output file
          writer.write(newLine.toString());
          writer.newLine();
        }
      }

      System.out.println("Conversion completed. Updated data written to " + outputFilePath);

    } catch (IOException e) {
      System.err.println("Error processing the file: " + e.getMessage());
    }

  }

  /**
   * The External Data.
   */
  @SuppressWarnings("unused")
  private class ExternalData {

    /** The source code. */
    private String sourceCode;

    /** Map of entry location (group + "|" + priority) to grade */
    private Map<String, String> gradeMap = new HashMap<>();

    /** Map of entry location (group + "|" + priority) to unmappable reason */
    private Map<String, String> unmappableReasonMap = new HashMap<>();

    private Set<MapNote> mapNotes = new HashSet<>();

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

    public Map<String, String> getGradeMap() {
      return gradeMap;
    }

    public void setGradeMap(Map<String, String> GradeMap) {
      this.gradeMap = GradeMap;
    }
    
    public void setGradeAtLocation(String location, String grade) {
      this.gradeMap.put(location, grade);
    }

    public Map<String, String> getUnmappableReasonMap() {
      return unmappableReasonMap;
    }

    public void setUnmappableReasonMap(Map<String, String> UnmappableReasonMap) {
      this.unmappableReasonMap = UnmappableReasonMap;
    }
    
    public void setUnmappableReasonAtLocation(String location, String UnmappableReason) {
      this.unmappableReasonMap.put(location, UnmappableReason);
    }    

    public Set<MapNote> getMapNotes() {
      return mapNotes;
    }

    public void setMapNotes(Set<MapNote> mapNotes) {
      this.mapNotes = mapNotes;
    }
  }
}
