/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Run the weekly SQLdump report and email to MedDRA staff
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal run-meddra3-sql-report
 */
public class Meddra3SqlReportMojo extends AbstractOtfMappingMojo {

  /**
   * Comma delimited list of project ids.
   *
   * @parameter
   * @required
   */
  private String projectIds;

  /** The manager. */
  EntityManager manager;

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    getLog().info("Start MedDRA SQLReport Mojo");

    setupBindInfoPackage();

    if (projectIds.isEmpty()) {
      getLog().error("Parameter projectIds is empty.");
      throw new MojoExecutionException("Parameter projectIds is empty.");
    }

    try {
      // pass in report id list
      final StringTokenizer st = new StringTokenizer(this.projectIds, ",");

      while (st.hasMoreTokens()) {
        long project = Long.parseLong(st.nextToken());
        runReport(project);
      }

    } catch (Exception e) {
      getLog().error("Error running MedDRA SQLReport Mojo.", e);
    }
  }

  /**
   * Run report.
   *
   * @param mapProjectId the map project id
   * @throws Exception the exception
   */
  private void runReport(long mapProjectId) throws Exception {
    try (ContentService service = new ContentServiceJpa() {
      {
        Meddra3SqlReportMojo.this.manager = manager;
      }
    }; MappingService mappingService = new MappingServiceJpa();) {

      // Run the SQL report
    	
    final MapProject mapProject = mappingService.getMapProject(mapProjectId);
    final long lastModified = mapProject.getEditingCycleBeginDate().getTime();
	  final javax.persistence.Query query = manager.createNativeQuery(
	      "SELECT DISTINCT " + 
	      "		map_records.conceptId, " + 
	      "		map_notes.note, " + 
	      "		map_records_labels_AUD.labels, " + 
	      "		map_records.conceptName, " + 
	      "		map_entries.targetId as newTargetId, " + 
	      "		map_entries.targetName newTargetName, " + 
	      "		map_entries_AUD.targetId as oldTargetId, " + 
	      "		map_entries_AUD.targetName as oldTargetName, " + 
	      "	FROM_UNIXTIME(map_records.lastModified / 1000, '%d/%m/%y %H:%i:%s') as lastModifiedDate	from map_records " + 
	      "		left join map_notes on map_records.id = map_notes.note " + 
	      "		left join map_records_labels_AUD on map_records_labels_AUD.id = map_records.id " + 
	      "		inner join map_entries on map_entries.mapRecord_id = map_records.id " + 
	      "		inner join map_entries_AUD on map_entries_AUD.mapRecord_id = (SELECT id FROM map_records_AUD " +
	      "		where conceptId = map_records.conceptId and workflowStatus in ('READY_FOR_PUBLICATION', 'PUBLISHED')" +
	      "		and lastModified < :LAST_MODIFIED ORDER BY lastModified DESC LIMIT 1) " + 
	      "	where workflowStatus in ('READY_FOR_PUBLICATION', 'PUBLISHED') " + 
	      "     and lastModified > :LAST_MODIFIED " + 
	      "		and mapProjectId = :MAP_PROJECT_ID" +
	      "		and (map_entries.targetId != map_entries_AUD.targetId);");

      query.setParameter("MAP_PROJECT_ID", mapProjectId);
      query.setParameter("LAST_MODIFIED", lastModified);

      @SuppressWarnings("unchecked")
      List<Object[]> objects = query.getResultList();

      List<String> results = new ArrayList<>();
      // Add header row
      results.add(
          "Source Concept\tSource Id\tTarget Concept\tTarget Id\tSNOMED Hierarchy");

      // Add result rows
      for (Object[] array : objects) {
        StringBuilder sb = new StringBuilder();
        for (Object o : array) {
          sb.append((o != null ? o.toString() : "null")).append("\t");
        }
        results.add(sb.toString());
      }

      // Add results to file
      final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
      final String dateStamp = dateFormat.format(new Date());

      final String filename = "/sqlReport_"
          + mapProject.getName().replace(" ", "") + "_" + dateStamp;

      File resultFile =
          new File(System.getProperty("java.io.tmpdir") + filename + ".txt");
      getLog().info("Created result file: " + resultFile.getAbsolutePath());

      try (FileWriter writer = new FileWriter(resultFile);) {
        for (String str : results) {
          writer.write(str);
          writer.write(System.getProperty("line.separator"));
        }
      }

      // Zip results file
      File zipFile =
          new File(System.getProperty("java.io.tmpdir") + filename + ".zip");

      try (FileOutputStream fos = new FileOutputStream(zipFile);
          ZipOutputStream zipOut =
              new ZipOutputStream(new BufferedOutputStream(fos));
          FileInputStream fis = new FileInputStream(resultFile);) {

        ZipEntry ze = new ZipEntry(resultFile.getName());
        getLog().info("Zipping the file: " + resultFile.getName());
        zipOut.putNextEntry(ze);
        byte[] tmp = new byte[4 * 1024];
        int size = 0;
        while ((size = fis.read(tmp)) != -1) {
          zipOut.write(tmp, 0, size);
        }

      } catch (Exception e) {
        getLog().error(e);
      }

      // Send file to recipients
      sendEmail(zipFile.getAbsolutePath());

    } catch (Exception e) {
      getLog().error(e);
      throw new MojoExecutionException(
          "MedDRA SQLReport mojo failed to complete", e);
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
        config.getProperty("sqlreport.send.notification.recipients.meddra");
    String notificationMessage = "";
    getLog().info("Request to send notification email to recipients: "
        + notificationRecipients);
    notificationMessage +=
        "Hello,\n\nThe weekly SQL report has been generated.";

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
        "[OTF-Mapping-Tool] Weekly SQL report", from, notificationRecipients,
        notificationMessage, props, fileName,
        "true".equals(config.getProperty("mail.smtp.auth")));
  }
}
