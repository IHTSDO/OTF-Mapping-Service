/*
 *    Copyright 2021 West Coast Informatics, LLC
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
 * Run the nightly SQLdump report and email to MIMS staff
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal run-mims2-sql-report
 */
public class Mims2SqlReportMojo extends AbstractOtfMappingMojo {

  /**
   * Comma delimited list of project ids.
   *
   * @parameter
   * @required
   */
  private String projectIds;
  
  /** The map name. */
  private String mapName;

  /** The manager. */
  EntityManager manager;

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    

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
      getLog().error("Error running " + mapName + " Report Mojo.", e);
    }
  }

  /**
   * Run report.
   *
   * @param mapProjectId the map project id
   * @throws Exception the exception
   */
  private void runReport(long mapProjectId) throws Exception {
    
    MappingService mappingService = new MappingServiceJpa();
    final MapProject mapProject = mappingService.getMapProject(mapProjectId);
    mapName = mapProject.getName();
    
    getLog().info("Start " + mapProject.getName() + " Mojo");
    
    try (ContentService service = new ContentServiceJpa() {
      {
        Mims2SqlReportMojo.this.manager = manager;
      }
    }; ) {

      // Run the SQL report
      final javax.persistence.Query query = manager.createNativeQuery(
          "select FROM_UNIXTIME(MapRecordAndSpecialistInfo.lastModified/1000, '%m/%d/%Y') as 'Date Published', " +
          "MapRecordAndSpecialistInfo.conceptId as 'MIMS Concept ID', " +
          "MapRecordAndSpecialistInfo.conceptName as 'MIMS Concept Name', " +
          "MapRecordAndSpecialistInfo.mapPriority as 'Map Entry', " +
          "MapRecordAndSpecialistInfo.targetId as 'SNOMED Concept Id', MapRecordAndSpecialistInfo.targetName as 'SNOMED Concept Name', " +
          "Relation.relationName as 'Map Relation', " +
          "MapRecordAndSpecialistInfo.SpecialistName as 'Specialist', ReviewerInfo.ReviewerName as 'Reviewer', " +
          "if(MapRecordAndSpecialistInfo.flagForConsensusReview,'True','False') as 'Consensus Review', " +
          "if(MapRecordAndSpecialistInfo.flagForEditorialReview,'True','False') as 'Editorial Review', " +
          "if(MapRecordAndSpecialistInfo.flagForMapLeadReview,'True','False') as 'Map Lead Review', " +
          "if(AdviceInfo.adviceName='INCLUDE CHILDREN','True','False') as 'Include Children' " +
          "from " +
          "(select mr.conceptId, mr.conceptName, me.mapPriority, me.targetId, me.targetName, me.mapRelation_id, mu.userName as SpecialistName, mr.lastModified, mr.flagForConsensusReview, mr.flagForEditorialReview, mr.flagForMapLeadReview, me.id as map_entries_id " +
          "from map_records mr, map_records_AUD mra, map_users mu, map_entries me " +
          "where mr.conceptId = mra.conceptId and " +
          "mr.mapProjectId = :MAP_PROJECT_ID and " +
          "mr.workflowStatus in ('PUBLISHED','READY_FOR_PUBLICATION') and " +
          "mra.owner_id = mu.id and " +
          "me.mapRecord_id=mr.id and " +
          "mra.workflowStatus in ('REVIEW_NEEDED') group by mr.conceptId, mr.conceptName,mra.owner_id, me.targetId " +
          // Special case for maps that were loaded in directly as READY_FOR_PUBLICATION, which have no REVIEW_NEEDED audit entry.
          "UNION " + 
          "select mr.conceptId, mr.conceptName, me.mapPriority, me.targetId, me.targetName, me.mapRelation_id, mu.userName as SpecialistName, mr.lastModified, mr.flagForConsensusReview, mr.flagForEditorialReview, mr.flagForMapLeadReview, me.id as map_entries_id " + 
          "from map_records mr, map_users mu, map_entries me " + 
          "where mr.mapProjectId = :MAP_PROJECT_ID and " + 
          "mr.workflowStatus in ('READY_FOR_PUBLICATION') and " + 
          // The loader user is id=1
          "mr.owner_id =1 and " + 
          "mr.owner_id = mu.id and " + 
          "me.mapRecord_id=mr.id " + 
          "group by mr.conceptId, mr.conceptName,mr.owner_id, me.targetId " +
          ") as MapRecordAndSpecialistInfo " +
          "left join " + 
          "(select mema.map_entries_id, ma.name as adviceName " + 
          "from map_entries_map_advices mema, map_advices ma where " + 
          "mema.mapAdvices_id=ma.id) as AdviceInfo " + 
          "on AdviceInfo.map_entries_id=MapRecordAndSpecialistInfo.map_entries_id " +
          "left join " +
          "(select name as relationName, id from map_relations rel) as Relation " +
          "on MapRecordAndSpecialistInfo.mapRelation_id=Relation.id " +
          "left join " +
          "(select mr.conceptId, mu.userName as ReviewerName from map_records mr, map_records_AUD mra, map_users mu where mr.conceptId = mra.conceptId and " +
          "mr.workflowStatus in ('PUBLISHED','READY_FOR_PUBLICATION') and " +
          "mra.owner_id = mu.id and " +
          "mra.workflowStatus in ('REVIEW_RESOLVED') group by mr.conceptId,mra.owner_id) as ReviewerInfo " +
          "on MapRecordAndSpecialistInfo.conceptId=ReviewerInfo.conceptId order by MapRecordAndSpecialistInfo.conceptId, MapRecordAndSpecialistInfo.mapPriority; "
          );

      query.setParameter("MAP_PROJECT_ID", mapProjectId);

      @SuppressWarnings("unchecked")
      List<Object[]> objects = query.getResultList();

      List<String> results = new ArrayList<>();
      // Add header row
      results.add(
          "Date Published\tMIMS Concept ID\tMIMS Concept Name\tMap Entry\tSNOMED Concept Id\tSNOMED Concept Name\tMap Relation\tSpecialist\tReviewer\tConsensus Review\tEditorial Review\tMap Lead Review\tInclude Children");

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
          mapProject.getName() + " mojo failed to complete", e);
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
        config.getProperty("sqlreport.send.notification.recipients.mims."
            + getClass().getSimpleName());
    String notificationMessage = "";
    getLog().info("Request to send notification email to recipients: "
        + notificationRecipients);
    notificationMessage +=
        "Hello,\n\nThe nightly SQL report has been generated.";

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
        "[OTF-Mapping-Tool] Nightly " + mapName + " SQL report", from, notificationRecipients,
        notificationMessage, props, fileName,
        "true".equals(config.getProperty("mail.smtp.auth")));
  }
}
