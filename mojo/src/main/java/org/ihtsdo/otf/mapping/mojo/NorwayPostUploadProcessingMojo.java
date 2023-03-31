/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Performs special post-upload processing for project 
 * Export all notes for READY_FOR_PUBLICATION maps into previously-created-notes document, in case these concepts get looked at again.
 * Make sure to remove previous notes for these specific concepts before appending the new notes, to avoid duplication.
 * Remove READY_FOR_PUBLICATION map records, and remove the concepts from scope.
 * Recompute workflow

 * 
 * See admin/release/pom.xml for a sample execution.
 * 
 * @goal postupload-processing
 * @phase package
 */
public class NorwayPostUploadProcessingMojo extends AbstractOtfMappingMojo {

  /**
   * The refSet id.
   *
   * @parameter refsetId
   */
  private String refsetId = null;
  
  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Running the post-upload processes for ");
    getLog().info("  refsetId = " + refsetId);

    setupBindInfoPackage();

    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a refset Id.");
    }

    try {

      final MappingService mappingService = new MappingServiceJpa();
      final WorkflowService workflowService = new WorkflowServiceJpa();

      MapProject mapProject = null;

      for (final MapProject mp : mappingService.getMapProjects()
          .getIterable()) {
        if (mp.getRefSetId().equals(refsetId)) {
          mapProject = mp;
          break;
        }
      }

      if (mapProject == null) {
        getLog().info("NO PROJECTS FOUND FOR refsetId: " + refsetId);
        mappingService.close();
        return;
      }

      // Identify ready for publication maps
      final Set<MapRecord> readyForPublicationMapRecords = new HashSet<>();
      final Set<Long> readyForPublicationMapRecordIds = new HashSet<>();
      final Set<String> readyForPublicationConceptIds = new HashSet<>();

      Logger.getLogger(NorwayPostUploadProcessingMojo.class).info("Identifying ready for publication maps");

      final MapRecordList mapRecords =
          mappingService.getMapRecordsForMapProject(mapProject.getId());
      
      for(MapRecord mapRecord : mapRecords.getMapRecords()) {
        if(mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)) {
          readyForPublicationMapRecords.add(mapRecord);
          readyForPublicationMapRecordIds.add(mapRecord.getId());
          readyForPublicationConceptIds.add(mapRecord.getConceptId());
        }
      }
      
      // Export all notes for READY_FOR_PUBLICATION maps into previously-created-notes document, in case these concepts get looked at again.
      // Make sure to remove previous notes for these specific concepts before appending the new notes, to avoid duplication.
      Logger.getLogger(NorwayPostUploadProcessingMojo.class).info("Exporting map notes from ready for publication maps");

      final String dataDir = ConfigUtility.getConfigProperties().getProperty("data.dir");
      if (dataDir == null) {
        throw new Exception("Config file must specify a data.dir property");
      }
      
      String noteFile =
          dataDir + "/doc/" + mapProject.getId() + "/preloadMaps/previouslyCreatedMapNotes.txt";
      
      if (!new File(noteFile).exists()) {
        throw new Exception("Specified note file missing: " + noteFile);
      }
      
      // Read through the existing note file, removing any notes associated with this batch of ready for publication concepts
      String noteFileContent = "";

      BufferedReader noteReader = new BufferedReader(new FileReader(noteFile));

      String line = null;

      while ((line = noteReader.readLine()) != null) {
        String fields[] = line.split("\t");
        
        String conceptId = fields[0];
        
        if(readyForPublicationConceptIds.contains(conceptId)) {
          continue;
        }
        else {
          noteFileContent = noteFileContent + line + System.lineSeparator();
        }
      }
      
      noteReader.close();
      
      //Now go through the ready for publication maps, adding all notes
      //File structure is:
      //conceptId   userName    timestamp   note
      for(MapRecord mapRecord : readyForPublicationMapRecords) {
        final String conceptId = mapRecord.getConceptId();
        for(MapNote mapNote : mapRecord.getMapNotes()) {
          final String userName = mapNote.getUser().getUserName();
          final String timestamp = mapNote.getTimestamp().toString();
          final String noteText = mapNote.getNote().replaceAll("\n", "<br>");
          String noteLine = conceptId + "\t" + userName + "\t" + timestamp + "\t" + noteText;
          noteFileContent = noteFileContent + noteLine + System.lineSeparator();
        }
      }
      
      //Overwrite the note file with the new content
      FileWriter writer = new FileWriter(noteFile);
      writer.write(noteFileContent);
      writer.close();      
      
      
      // With the notes saved, now remove READY_FOR_PUBLICATION map records
      for(Long mapRecordId : readyForPublicationMapRecordIds) {
        mappingService.removeMapRecord(mapRecordId);
      }
      
      // Remove the uploaded concepts from scope.
      Set<String> scopeConcepts = mapProject.getScopeConcepts();
      for(String conceptId : readyForPublicationConceptIds) {
        scopeConcepts.remove(conceptId);
      }
      mapProject.setScopeConcepts(scopeConcepts);
      mappingService.updateMapProject(mapProject);
      
      // Finally, recompute workflow
      workflowService.computeWorkflow(mapProject);
      
      
      mappingService.close();
      workflowService.close();

      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Post-release processing for project failed.", e);
    }
  }
}