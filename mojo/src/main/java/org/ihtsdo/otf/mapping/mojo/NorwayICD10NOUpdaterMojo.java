/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.algo.ICD10NODownloadAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.RemoverAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.SimpleLoaderAlgorithm;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Run the weekly SQLdump report and email
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal update-icd10no
 */
public class NorwayICD10NOUpdaterMojo extends AbstractOtfMappingMojo {

  /** The manager. */
  EntityManager manager;

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    getLog().info("Start ICD10NO Updater Mojo");

    try {
      final MappingService mappingService = new MappingServiceJpa();

      // Calculate today's date to set as version
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd");
      LocalDateTime now = LocalDateTime.now();
      String version = dtf.format(now);

      // Download the newest version of ICD10NO from the terminology server
      try (final ICD10NODownloadAlgorithm algo = new ICD10NODownloadAlgorithm();) {

        algo.compute();

      } catch (Exception e) {
        throw new MojoExecutionException(
            "trying to download most recent terminology ICD10NO from API", e);
      }

      // Load the newly downloaded version of ICD10NO into the mapping tool
      String inputDir = "";
      try {
        inputDir = ConfigUtility.getConfigProperties().getProperty("icd10noAPI.dir");
        // Strip off final /, if it exists
        if (inputDir.endsWith("/")) {
          inputDir = inputDir.substring(0, inputDir.length() - 1);
        }
        inputDir = inputDir + "/" + version;

        Logger.getLogger(getClass())
            .info("Input directory generated from config.properties, and set to " + inputDir);
      } catch (Exception e) {
        throw new MojoExecutionException(
            "trying to set the input directory from config.properties");
      }

      try (final SimpleLoaderAlgorithm algo =
          new SimpleLoaderAlgorithm("ICD10NO", version, inputDir, "0");) {

        algo.compute();

      } catch (Exception e) {
        throw new MojoExecutionException("trying to load terminology ICD10NO from directory", e);
      }

      // Set the ICD10NO forvaltning project to point to the newly downloaded
      // ICD10NO version
      MapProject ICD10NOProject = mappingService.getMapProject(3L);
      String previousICD10NOTerminology = ICD10NOProject.getDestinationTerminology();
      String previousICD10NOVersion = ICD10NOProject.getDestinationTerminologyVersion();
      ICD10NOProject.setDestinationTerminologyVersion(version);
      mappingService.updateMapProject(ICD10NOProject);

      mappingService.close();

      // Remove the version of ICD10NO that was previously assigned to the
      // project
      try (final RemoverAlgorithm algo =
          new RemoverAlgorithm(previousICD10NOTerminology, previousICD10NOVersion);) {

        algo.compute();

      } catch (Exception e) {
        throw new MojoExecutionException("trying to remove previous version of terminology ICD10NO",
            e);
      }

    } catch (Exception e) {
      throw new MojoExecutionException("ICD10NO Updater failed.", e);
    }
  }

  /**
   * Send email.
   *
   * @param fileName the file name
   * @throws Exception the exception
   */
  private void sendEmail(String fileName) throws Exception {

    Properties config;
    try {
      config = ConfigUtility.getConfigProperties();
    } catch (Exception e1) {
      throw new MojoExecutionException("Failed to retrieve config properties");
    }
    String notificationRecipients = config
        .getProperty("report.send.notification.recipients.norway." + getClass().getSimpleName());
    String notificationMessage = "";
    getLog().info("Request to send notification email to recipients: " + notificationRecipients);
    notificationMessage += "Hello,\n\nICD10NO has been updated.";

    String from;
    if (config.containsKey("mail.smtp.from")) {
      from = config.getProperty("mail.smtp.from");
    } else {
      from = config.getProperty("mail.smtp.user");
    }

    Properties props = new Properties();
    props.put("mail.smtp.user", config.getProperty("mail.smtp.user"));
    props.put("mail.smtp.password", config.getProperty("mail.smtp.password"));
    props.put("mail.smtp.host", config.getProperty("mail.smtp.host"));
    props.put("mail.smtp.port", config.getProperty("mail.smtp.port"));
    props.put("mail.smtp.starttls.enable", config.getProperty("mail.smtp.starttls.enable"));
    props.put("mail.smtp.auth", config.getProperty("mail.smtp.auth"));

    ConfigUtility.sendEmailWithAttachment("[OTF-Mapping-Tool] ICD10NO Updater", from,
        notificationRecipients, notificationMessage, props, fileName,
        "true".equals(config.getProperty("mail.smtp.auth")));
  }
}
