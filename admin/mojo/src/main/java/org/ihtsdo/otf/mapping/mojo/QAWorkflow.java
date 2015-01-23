package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
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
   * The refSet id.
   * 
   * @parameter refsetId
   */
  private String refsetId = null;

  /**
   * Whether to send notifications via email, default is true
   * 
   * @parameter sendNotification
   */
  private boolean sendNotification = true;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting workflow quality assurance checks");
    getLog().info("  refsetId = " + refsetId);
    getLog().info("  sendNotification = " + sendNotification);

    try {

      MappingService mappingService = new MappingServiceJpa();
      WorkflowService workflowService = new WorkflowServiceJpa();

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

      List<String> errors = new ArrayList<>();

      // Perform the QA checks

      for (MapProject mapProject : mapProjects) {
        getLog().info(
            "Checking workflow for " + mapProject.getName() + ", "
                + mapProject.getId());
        List<String> results =
            workflowService.computeWorkflowStatusErrors(mapProject);

        if (results.size() != 0) {

          // add some header material
          errors.add("------------------------------------------");
          errors.add(mapProject.getName());
          errors.add("------------------------------------------");
          errors.add("");

          errors.addAll(results);
        }
      }

      // convert the list of error strings into a single message
      StringBuffer message = new StringBuffer();
      for (String error : errors) {
        message.append(error).append("\n");
      }

      // log the message sent
      getLog().info(message);

      // try to send the email
      if (sendNotification == true && errors.size() > 0) {
        
        getLog().info("Errors detected and email notification requested, sending email");
        
        Properties config;
        try {
          config = ConfigUtility.getConfigProperties();
        } catch (Exception e1) {
          throw new MojoExecutionException(
              "Could not send email:  Failed to retrieve config properties");
        }
        String notificationRecipients =
            config.getProperty("send.notification.recipients");

        // instantiate the email handler
        OtfEmailHandler emailHandler = new OtfEmailHandler();
        emailHandler.sendSimpleEmail(notificationRecipients,
            "OTF Mapping Tool:  Workflow Errors Detected", message.toString());


       
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
