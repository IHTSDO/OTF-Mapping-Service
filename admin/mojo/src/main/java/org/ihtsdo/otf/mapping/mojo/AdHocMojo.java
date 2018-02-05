/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Customizable mojo to run ad hoc code
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal run-ad-hoc
 */
public class AdHocMojo extends AbstractMojo {

  /**
   * The specified refsetId
   * @parameter
   * @required
   */
  private String refsetId = null;

  /**
   * The specified mode
   * @parameter
   * @required
   */
  private String mode = null;

  /**
   * The specified input file (for driving action)
   * @parameter
   */
  private String inputFile = null;

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Start Ad hoc mojo");
    getLog().info("  refsetId = " + refsetId);
    getLog().info("  mode = " + mode);

    try (final WorkflowService workflowService = new WorkflowServiceJpa();
        final ContentService contentService = new ContentServiceJpa();
        final MappingService mappingService = new MappingServiceJpa()) {

      if (mode != null && mode.equals("icd11")) {
        handleIcd11(refsetId, inputFile, workflowService, contentService,
            mappingService);
      }

      if (mode != null && mode.equals("icd11-editing-done")) {
        handleIcd11EditingDone(refsetId, inputFile, workflowService,
            contentService, mappingService);
      }

      if (mode != null && mode.equals("remove-bogus-map-records")) {
        removeBogusMapRecords(workflowService,
            contentService, mappingService);
      }      
      
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Ad-hoc mojo failed to complete", e);
    }
  }

  private void removeBogusMapRecords(WorkflowService workflowService,
    ContentService contentService, MappingService mappingService) throws Exception {
    ArrayList<Long> mapRecordIds = new ArrayList<>(Arrays.asList(1885395L,1885758L,1884977L,1885416L));
    
    mappingService.setTransactionPerOperation(true);
    
    
    
    for(final Long mapRecordId : mapRecordIds){
      MapRecord mapRecord = null;
      try{
      mapRecord = mappingService.getMapRecord(mapRecordId);
      }
      catch (Exception e){
        //do nothing
      }
      if(mapRecord != null){
      mappingService.removeMapRecord(mapRecordId);
      }
    }
    
  }

  /**
   * Handle icd 11.
   *
   * @param refsetId the refset id
   * @param workflowService the workflow service
   * @param contentService the content service
   * @param mappingService the mapping service
   * @throws Exception the exception
   */
  private void handleIcd11(String refsetId, String inputFile,
    WorkflowService workflowService, ContentService contentService,
    MappingService mappingService) throws Exception {
    if (inputFile == null) {
      throw new Exception("Unexpectedly null input file.");
    }
    if (!new File(inputFile).exists()) {
      throw new Exception("Input file does not exist = " + inputFile);
    }

    // Concept ids
    final Set<String> conceptIds = new HashSet<>();
    try (final BufferedReader in =
        new BufferedReader(new FileReader(new File(inputFile)))) {
      String line = null;
      while ((line = in.readLine()) != null) {
        String tokens[] = line.split("\t");
        conceptIds.add(tokens[5]);
      }
    }

    // Load the map project
    final Map<String, MapProject> mapProjectMap = new HashMap<>();
    for (MapProject project : mappingService.getMapProjects().getIterable()) {
      mapProjectMap.put(project.getRefSetId(), project);
    }
    final MapProject project = mapProjectMap.get(refsetId);
    if (project == null) {
      throw new Exception(
          "Unable to find map project for refsetId = " + refsetId);
    }
    getLog().info("  map project = " + project.getId() + ","
        + project.getRefSetId() + ", " + project.getName());

    // Iterate through, load concept, load records, make QA record
    // find the qa user
    MapUser mapUser = null;
    for (final MapUser user : workflowService.getMapUsers().getMapUsers()) {
      if (user.getUserName().equals("qa"))
        mapUser = new MapUserJpa(user);
    }
    int ct = 0;
    for (final String conceptId : conceptIds) {
      final MapRecordList list = mappingService
          .getMapRecordsForProjectAndConcept(project.getId(), conceptId);
      if (list.getCount() > 1) {
        throw new Exception("Unexpected number of records (" + list.getCount()
            + ") for project " + project.getId() + ", conceptId = "
            + conceptId);
      }
      if (list.getCount() == 0) {
        getLog().warn("No mappings for conceptId = " + conceptId);
        continue;
      }

      createQARecord(workflowService, contentService, project, mapUser,
          list.getMapRecords().get(0));

      if (++ct % 500 == 0) {
        getLog().info("    count = " + ct);
      }
    }

  }

  /**
   * Handle icd 11 editing done.
   *
   * @param refsetId the refset id
   * @param inputFile the input file
   * @param workflowService the workflow service
   * @param contentService the content service
   * @param mappingService the mapping service
   * @throws Exception the exception
   */
  private void handleIcd11EditingDone(String refsetId, String inputFile,
    WorkflowService workflowService, ContentService contentService,
    MappingService mappingService) throws Exception {

    // Load the map project
    final Map<String, MapProject> mapProjectMap = new HashMap<>();
    for (MapProject project : mappingService.getMapProjects().getIterable()) {
      mapProjectMap.put(project.getRefSetId(), project);
    }
    final MapProject project = mapProjectMap.get(refsetId);

    MapUser mapUser = null;
    for (final MapUser user : workflowService.getMapUsers().getMapUsers()) {
      if (user.getId().equals(59L))
        mapUser = new MapUserJpa(user);
    }

    // Get map records for the project
    final MapRecordList list =
        mappingService.getMapRecordsForMapProject(project.getId());
    for (final MapRecord record : list.getMapRecords()) {
      if (record.getWorkflowStatus().toString().equals("EDITING_IN_PROGRESS")
          && record.getOwner().getUserName().equals("loader")) {
        // Referesh the map record
        final MapRecord mapRecord =
            workflowService.getMapRecord(record.getId());
        // Get the concept
        final Concept concept2 = contentService.getConcept(
            mapRecord.getConceptId(), project.getSourceTerminology(),
            project.getSourceTerminologyVersion());
        final Concept concept = new ConceptJpa();
        concept.setTerminologyId(mapRecord.getConceptId());
        concept.setTerminology(project.getSourceTerminology());
        concept.setTerminologyVersion(project.getSourceTerminologyVersion());
        concept.setDefaultPreferredName(concept2.getDefaultPreferredName());
        // Perform the workflow action
        workflowService.processWorkflowAction(project, concept, mapUser,
            mapRecord, WorkflowAction.FINISH_EDITING);
      }
    }
  }

  /**
   * Creates the QA record.
   *
   * @param workflowService the workflow service
   * @param contentService the content service
   * @param mapRecord the map record
   * @throws Exception the exception
   */
  private void createQARecord(WorkflowService workflowService,
    ContentService contentService, MapProject mapProject, MapUser mapUser,
    MapRecord mapRecord) throws Exception {

    mapRecord.addLabel("HIGH");
    final Concept concept2 = contentService.getConcept(mapRecord.getConceptId(),
        mapProject.getSourceTerminology(),
        mapProject.getSourceTerminologyVersion());
    final Concept concept = new ConceptJpa();
    concept.setTerminologyId(mapRecord.getConceptId());
    concept.setTerminology(mapProject.getSourceTerminology());
    concept.setTerminologyVersion(mapProject.getSourceTerminologyVersion());
    concept.setDefaultPreferredName(concept2.getDefaultPreferredName());

    // process the workflow action
    workflowService.processWorkflowAction(mapProject, concept, mapUser,
        new MapRecordJpa(mapRecord, true), WorkflowAction.CREATE_QA_RECORD);
  }
}
