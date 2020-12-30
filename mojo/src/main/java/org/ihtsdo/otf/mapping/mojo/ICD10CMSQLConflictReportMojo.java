/*
 *    Copyright 2019 West Coast Informatics, LLC
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Run the weekly SQL conflict report and email to ICD10CM staff
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal run-icd10cm-sql-conflict-report
 */
public class ICD10CMSQLConflictReportMojo extends AbstractOtfMappingMojo {

  /** The manager. */
  EntityManager manager;

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Start ICD10CM SQLConflictReport Mojo");

    setupBindInfoPackage();

    try (final ContentService service = new ContentServiceJpa() {
      {
        ICD10CMSQLConflictReportMojo.this.manager = manager;
      }
    };) {

      // Obtain an entity manager;

      // Run the SQL report
      
      final javax.persistence.Query query = manager.createNativeQuery(
              "select distinct conceptId, targetId, mapGroup, mapPriority, workflowStatus, "
    		  + "map_users.userName, map_entries_AUD.REVTYPE "
              + "from map_records_AUD, map_entries_AUD, map_users "  
              + "where map_entries_AUD.mapRecord_id = map_records_AUD.id "
              + "and  map_users.id = lastModifiedBy_id " 
              + "and workflowStatus in ('CONFLICT_DETECTED', 'CONFLICT_RESOLVED') "
              + "and lastModified > (select unix_timestamp(editingCycleBeginDate)*1000 from map_projects where id = 17) "
              + "order by conceptId, lastModified, mapGroup, mapPriority;");

      List<Object[]> objects = query.getResultList();

      List<String> results = new ArrayList<>();
      // Add header row
      results.add(
          "conceptId\ttargetId\tmapGroup\tmapPriority\tworkflowStatus\tuserName\tREVTYPE");

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

      File resultFile = new File(System.getProperty("java.io.tmpdir")
          + "/sqlConflictReport_" + dateStamp + ".txt");
      System.out
          .println("Created result file: " + resultFile.getAbsolutePath());

      FileWriter writer = new FileWriter(resultFile);
      for (String str : results) {
        writer.write(str);
        writer.write(System.getProperty("line.separator"));
      }
      writer.close();

      // Zip results file
      File zipFile = new File(System.getProperty("java.io.tmpdir")
          + "/sqlConflictReport_" + dateStamp + ".zip");

      try (FileOutputStream fos = new FileOutputStream(zipFile);
          ZipOutputStream zipOut =
              new ZipOutputStream(new BufferedOutputStream(fos));
          FileInputStream fis = new FileInputStream(resultFile);) {

        ZipEntry ze = new ZipEntry(resultFile.getName());
        System.out.println("Zipping the file: " + resultFile.getName());
        zipOut.putNextEntry(ze);
        byte[] tmp = new byte[4 * 1024];
        int size = 0;
        while ((size = fis.read(tmp)) != -1) {
          zipOut.write(tmp, 0, size);
        }
        zipOut.flush();
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      // Send file to recipients
      sendEmail(zipFile.getAbsolutePath());

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "ICD10CM SQLConflictReport mojo failed to complete", e);
    }
  }

  private void sendEmail(String fileName) throws Exception {

    Properties config;
    try {
      config = ConfigUtility.getConfigProperties();
    } catch (Exception e1) {
      throw new MojoExecutionException("Failed to retrieve config properties");
    }
    String notificationRecipients =
        config.getProperty("sqlreport.send.notification.recipients");
    String notificationMessage = "";
    getLog().info("Request to send notification email to recipients: "
        + notificationRecipients);
    notificationMessage +=
        "Hello,\n\nThe weekly SQL conflict report has been generated.";

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
        "[OTF-Mapping-Tool] Weekly SQL conflict report", from, notificationRecipients,
        notificationMessage, props, fileName,
        "true".equals(config.getProperty("mail.smtp.auth")));
  }
}
