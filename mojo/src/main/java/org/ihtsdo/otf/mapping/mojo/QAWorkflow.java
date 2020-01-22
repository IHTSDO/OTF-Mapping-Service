/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Performs QA of the workflow
 * 
 * See admin/qa/pom.xml for a sample execution.
 * 
 * @goal qa-workflow
 * @phase package
 */
public class QAWorkflow extends AbstractOtfMappingMojo {

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

    setupBindInfoPackage();

    try (final WorkflowService workflowService = new WorkflowServiceJpa();) {
      List<MapProject> mapProjects = new ArrayList<>();

      if (refsetId == null) {
        mapProjects = workflowService.getMapProjects().getMapProjects();
      } else {
        for (MapProject mapProject : workflowService.getMapProjects()
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
        getLog().info("Checking workflow for " + mapProject.getName() + ", "
            + mapProject.getId());
        List<String> results = null;

        try {
          results = workflowService.computeWorkflowStatusErrors(mapProject);
        } catch (Exception e) {
          results.add("Unexpected error trying to check workflow -- see logs");
        }
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
      if (sendNotification && errors.size() > 0) {

        getLog().info(
            "Errors detected and email notification requested, sending email");

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
        String from;
        if (config.containsKey("mail.smtp.from")) {
          from = config.getProperty("mail.smtp.from");
        } else {
          from = config.getProperty("mail.smtp.user");
        }
        Properties props = new Properties();
        props.put("mail.smtp.user", config.getProperty("mail.smtp.user"));
        props.put("mail.smtp.password",
            config.getProperty("mail.smtp.password"));
        props.put("mail.smtp.host", config.getProperty("mail.smtp.host"));
        props.put("mail.smtp.port", config.getProperty("mail.smtp.port"));
        props.put("mail.smtp.starttls.enable",
            config.getProperty("mail.smtp.starttls.enable"));
        props.put("mail.smtp.auth", config.getProperty("mail.smtp.auth"));
        ConfigUtility.sendEmail("OTF Mapping Tool:  Workflow Errors Detected",
            from, notificationRecipients, message.toString(), props,
            "true".equals(config.getProperty("mail.smtp.auth")));
      }

      workflowService.close();

      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing workflow QA failed.", e);
    }
  }

}
