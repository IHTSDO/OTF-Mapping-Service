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
 * Run the weekly SQLdump report and email to ICD10CM staff
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal run-icd10cm-sql-report
 */
public class ICD10CMSQLReportMojo extends AbstractOtfMappingMojo {

  /** The manager. */
  EntityManager manager;

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Start ICD10CM SQLReport Mojo");

    getLog().info("  preloading bind info package");
    setupBindInfoPackage();        
    
    try (final ContentService service = new ContentServiceJpa() {
      {
        ICD10CMSQLReportMojo.this.manager = manager;
      }
    };) {

      // Obtain an entity manager;

      // Run the SQL report
      final javax.persistence.Query query = manager.createNativeQuery(
          "SELECT '' as id, '' as effectiveTime, c.active as active, '' as moduleId, '' as refSetId, "
              + "mr.conceptId as referencedComponentId, mr.conceptName as sctNname, "
              + "me.mapGroup as mapGroup, me.mapPriority as mapPriority, me.rule as mapRule, "
              + "ma.name as mapAdvice, me.targetId as mapTarget, me.targetName as icdName, "
              + "mrel.terminologyId as mapCategoryId, mrel.name as mapCategoryValue "
              + "FROM map_records mr  "
              + "LEFT JOIN map_entries me ON mr.id = me.mapRecord_id        "
              + "LEFT JOIN map_entries_map_advices mema ON me.id = mema.map_entries_id "
              + "LEFT JOIN map_advices ma ON mema.mapAdvices_id = ma.id "
              + "LEFT JOIN map_relations mrel ON mrel.id = me.mapRelation_id "
              + "LEFT JOIN concepts c ON c.terminologyId = mr.conceptId "
              + "WHERE mr.workflowStatus in('READY_FOR_PUBLICATION','PUBLISHED') "
              + "AND mr.mapProjectId=17 " + "AND c.terminology='SNOMEDCT_US' "
              + "ORDER BY referencedComponentId, mapGroup, mapPriority;");

      List<Object[]> objects = query.getResultList();

      List<String> results = new ArrayList<>();
      // Add header row
      results.add(
          "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tsctNname\tmapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\ticdName\tmapCategoryId\tmapCategoryValue");

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
          + "/sqlReport_" + dateStamp + ".txt");
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
          + "/sqlReport_" + dateStamp + ".zip");

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
          "ICD10CM SQLReport mojo failed to complete", e);
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
