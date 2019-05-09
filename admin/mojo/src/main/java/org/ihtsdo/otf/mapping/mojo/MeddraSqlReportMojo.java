/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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

import org.apache.maven.plugin.AbstractMojo;
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
 * @goal run-meddra-sql-report
 */
public class MeddraSqlReportMojo extends AbstractMojo {

  /**
   * 
   * Comma delimited list of project ids
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

    if (projectIds.isEmpty()) {
      getLog().error("Parameter projectIds is empty.");
      throw new MojoExecutionException("Parameter projectIds is empty.");
    }
    
    try {
      // pass in report id list
      final StringTokenizer st = new StringTokenizer(this.projectIds, ",");
      
      while(st.hasMoreTokens())
      {
        long project = Long.parseLong(st.nextToken());
        runReport(project);  
      }
      
    } catch (Exception e) {
      getLog().error("Error running MedDRA SQLReport Mojo.", e);
    }
  }

  private void runReport(long mapProjectId) throws Exception {
    try (ContentService service = new ContentServiceJpa()
        {{
          MeddraSqlReportMojo.this.manager = manager;
        }};
        MappingService mappingService = new MappingServiceJpa();
        ) {

      // Run the SQL report
      final javax.persistence.Query query = manager.createNativeQuery(
                " SELECT DISTINCT "
              + " mr.conceptId AS referencedComponentId "
              + " , mr.conceptName AS referencedComponentName "
              + " , mapGroup AS mapGroup "
              + " , mapPriority AS mapPriority "
              + " , me.targetId AS mapTarget "
              + " , me.targetName AS mapTargetName "
              + " , rel.name AS mapRelation "
              + " FROM map_records mr "
              + " LEFT OUTER JOIN map_entries me ON mr.id = me.mapRecord_id "
              + " LEFT OUTER JOIN map_relations rel ON me.mapRelation_id = rel.id "
              + " WHERE mr.mapProjectId = :MAP_PROJECT_ID "
              + " AND EXISTS (SELECT 1 FROM map_records_AUD mra, map_records_labels_AUD mrla "
                  + " WHERE mra.id = mrla.id AND mra.mapProjectId = :MAP_PROJECT_ID "
                  + " AND mrla.labels = 'Final QA Check MSSO'   AND mr.conceptId = mra.conceptId) "
              + " AND EXISTS (SELECT 1 FROM map_records_AUD mra, map_records_labels_AUD mrla "
                  + " WHERE mra.id = mrla.id AND mra.mapProjectId = :MAP_PROJECT_ID "
                  + " AND mrla.labels = 'Final QA Check SNOMED' AND mr.conceptId = mra.conceptId) "
              + " AND mr.workflowStatus = 'READY_FOR_PUBLICATION' "
              + " ORDER BY 2; "
      );

      query.setParameter("MAP_PROJECT_ID", mapProjectId);
      
      @SuppressWarnings("unchecked")
      List<Object[]> objects = query.getResultList();

      List<String> results = new ArrayList<>();
      // Add header row
      results.add(
          "Referenced Component Id\tReferenced Component Name\tMap Group\tMap Priority\tMap Target\tMap Target Name\tMap Relation");

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

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);
      final String filename = "/sqlReport_"
          + mapProject.getName().replace(" ", "") + "_" + dateStamp;

      File resultFile = new File(
          System.getProperty("java.io.tmpdir") + filename + ".txt");
      getLog().info("Created result file: " + resultFile.getAbsolutePath());

      try (FileWriter writer = new FileWriter(resultFile);) {
        for (String str : results) {
          writer.write(str);
          writer.write(System.getProperty("line.separator"));
        }
      }

      // Zip results file
      File zipFile = new File(System.getProperty("java.io.tmpdir")
          + filename + ".zip");

      try (FileOutputStream fos = new FileOutputStream(zipFile);
          ZipOutputStream zipOut = new ZipOutputStream(
              new BufferedOutputStream(fos));
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

  private void sendEmail(String fileName) throws Exception {

    Properties config;
    try {
      config = ConfigUtility.getConfigProperties();
    } catch (Exception e1) {
      throw new MojoExecutionException("Failed to retrieve config properties");
    }
    String notificationRecipients = config
        .getProperty("sqlreport.send.notification.recipients.meddra." + getClass().getSimpleName());
    String notificationMessage = "";
    getLog().info("Request to send notification email to recipients: "
        + notificationRecipients);
    notificationMessage += "Hello,\n\nThe weekly SQL report has been generated.";

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
