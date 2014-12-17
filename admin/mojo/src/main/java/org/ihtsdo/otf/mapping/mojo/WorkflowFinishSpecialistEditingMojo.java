package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Admin tool to force comparison and validation of Conflict Project tracking
 * records in situations where two specialists have finished work but the
 * workflow did not successfully compare and generate CONFLICT_DETECTED or
 * READY_FOR_PUBLICATION map records.
 * 
 * Created to address an issue discovered in early October 2014 where workflow
 * advancement was not properly being executed. Only valid for projects of
 * workflow type CONFLICT_PROJECT
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal finish-specialist-editing
 * @phase package
 */
public class WorkflowFinishSpecialistEditingMojo extends AbstractMojo {

  /**
   * The refset id
   * @parameter
   * @required
   */
  private String refsetId = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info(
        "Forcing workflow finish actions on errant tracking records");
    getLog().info("  refsetId = " + refsetId);

    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a refsetId.");
    }

    try {

      MappingService mappingService = new MappingServiceJpa();
      Set<MapProject> mapProjects = new HashSet<>();

      for (MapProject mapProject : mappingService.getMapProjects()
          .getIterable()) {
        for (String id : refsetId.split(",")) {
          if (mapProject.getRefSetId().equals(id)) {
            mapProjects.add(mapProject);
          }
        }
      }

      // Perform the QA checks
      WorkflowService workflowService = new WorkflowServiceJpa();
      for (MapProject mapProject : mapProjects) {

        if (mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_PROJECT)) {
          getLog().info(
              "Checking workflow for " + mapProject.getName() + ", "
                  + mapProject.getId());
          workflowService.finishEditingDoneTrackingRecords(mapProject);
        } else {
          getLog().error(
              "Project " + mapProject.getName()
                  + " is not a Conflict Project -- cannot process");
        }
      }

      mappingService.close();
      workflowService.close();
      getLog().info("Done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing workflow QA failed.", e);
    }

  }
}