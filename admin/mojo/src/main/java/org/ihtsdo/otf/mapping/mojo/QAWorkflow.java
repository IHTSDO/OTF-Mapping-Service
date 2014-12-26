package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.OtfEmailHandler;

/**
 * Loads unpublished complex maps.
 * 
 * See admin/qa/pom.xml for a sample execution.
 * 
 * @goal qa-workflow
 * @phase package
 */
public class QAWorkflow extends AbstractMojo {

  /**
   * The refSet id
   * @parameter refsetId
   * 
   */
  private String refsetId = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting workflow quality assurance checks");
    getLog().info("  refsetId = " + refsetId);

    try {

      MappingService mappingService = new MappingServiceJpa();
      List<MapProject> mapProjects = new ArrayList<>();

      if (refsetId == null) {
        mapProjects = mappingService.getMapProjects().getMapProjects();
      } else {
        for (MapProject mapProject : mappingService.getMapProjects()
            .getIterable()) {
          for (String id : refsetId.split(",")) {
            if (mapProject.getRefSetId().equals(id)) {
              mapProjects.add(mapProject);
            }
          }
        }
      }

      // Perform the QA checks
      WorkflowService workflowService = new WorkflowServiceJpa();
      for (MapProject mapProject : mapProjects) {
        getLog().info(
            "Checking workflow for " + mapProject.getName() + ", "
                + mapProject.getId());
        ValidationResult result =
            workflowService.computeWorkflowStatusErrors(mapProject);

        // TODO hardcoded while testing in prod environment
        if (!result.isValid()) {
          OtfEmailHandler emailHandler = new OtfEmailHandler();
          StringBuffer message = new StringBuffer();

          message.append(
              "Errors were detected in the workflow for project: "
                  + mapProject.getName()).append("\n\n");

          for (String error : result.getErrors()) {
            message.append(error).append("\n");
          }
          
          message.append("\n");
          emailHandler.sendSimpleEmail("pgranvold@westcoastinformatics.com",
              mapProject.getName() + " Workflow Errors", message.toString());
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