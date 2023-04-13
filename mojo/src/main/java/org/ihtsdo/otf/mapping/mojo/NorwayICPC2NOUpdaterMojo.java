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
import org.ihtsdo.otf.mapping.jpa.algo.ICPC2NODownloadAlgorithm;
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
 * @goal update-icpc2no
 */
public class NorwayICPC2NOUpdaterMojo extends AbstractOtfMappingMojo {

  /** The manager. */
  EntityManager manager;

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    getLog().info("Start ICPC2NO Updater Mojo");

    try {
    final MappingService mappingService = new MappingServiceJpa();

    
    // Calculate today's date to set as version
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd");  
    LocalDateTime now = LocalDateTime.now();  
    String version =  dtf.format(now);
    
    //Download the newest version of ICPC2NO from the terminology server
    try (final ICPC2NODownloadAlgorithm algo = new ICPC2NODownloadAlgorithm();) {

      algo.compute();

    } catch (Exception e) {
      throw new MojoExecutionException("trying to download most recent terminology ICPC2NO from API", e
          );
    }
    
    //Load the newly downloaded version of ICPC2NO into the mapping tool
    String inputDir = "";
    try {
    inputDir = ConfigUtility.getConfigProperties()
        .getProperty("icpc2noAPI.dir");
    // Strip off final /, if it exists
    if (inputDir.endsWith("/")) {
      inputDir = inputDir.substring(0, inputDir.length() - 1);
    }
    inputDir = inputDir + "/" + version;
    

    Logger.getLogger(getClass())
        .info("Input directory generated from config.properties, and set to "
            + inputDir);
    }
    catch (Exception e) {
      throw new MojoExecutionException("trying to set the input directory from config.properties");
    }
    
    
    try (final SimpleLoaderAlgorithm algo =
            new SimpleLoaderAlgorithm("ICPC2_NO", version, inputDir, "0");) {

      algo.compute();

    } catch (Exception e) {
      throw new MojoExecutionException("trying to load terminology ICPC2NO from directory", e);
    }
    
    //Set the ICPC2NO forvaltning project to point to the newly downloaded ICPC2NO version
    MapProject ICPC2NOProject = mappingService.getMapProject(2L);
    ICPC2NOProject.setDestinationTerminologyVersion(version); 
    mappingService.updateMapProject(ICPC2NOProject);
    
    mappingService.close();
    
    } catch (Exception e) {
      throw new MojoExecutionException("ICPC2NO Updater failed.", e);      
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
    String notificationRecipients =
        config.getProperty("report.send.notification.recipients.norway."
            + getClass().getSimpleName());
    String notificationMessage = "";
    getLog().info("Request to send notification email to recipients: "
        + notificationRecipients);
    notificationMessage +=
        "Hello,\n\nICPC2NO has been updated.";

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
    props.put("mail.smtp.starttls.enable",
        config.getProperty("mail.smtp.starttls.enable"));
    props.put("mail.smtp.auth", config.getProperty("mail.smtp.auth"));

    ConfigUtility.sendEmailWithAttachment(
        "[OTF-Mapping-Tool] ICPC2NO Updater", from, notificationRecipients,
        notificationMessage, props, fileName,
        "true".equals(config.getProperty("mail.smtp.auth")));
  }
}
