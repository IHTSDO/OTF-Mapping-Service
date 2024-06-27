/*
 *    Copyright 2024 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.report;

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

import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class NorwayReplacementMapReport.
 */
public class MeddraToSnomedExclusionReport extends AbstractOtfMappingReport {

  /** The Constant LOGGER. */
  private static final Logger LOGGER =
      LoggerFactory.getLogger(MeddraToSnomedExclusionReport.class);
  
  /** The manager. */
  static EntityManager manager;
  


  /**
   * Instantiates an empty {@link MeddraToSnomedExclusionReport}.
   */
  private MeddraToSnomedExclusionReport() {

  }

  /**
   * Run report.
   *
   * @throws Exception the exception
   */
  public static void runReport() throws Exception {

    LOGGER.info("Start Meddra To Snomed Exclusion Report");
    
    
    try (final ContentService service = new ContentServiceJpa() {
        {
        	MeddraToSnomedExclusionReport.manager = manager;
        }
      };) {
    	
    	// Run the SQL report
        final javax.persistence.Query query = manager.createNativeQuery(
            "select \n"
            + "	mr.conceptId as \"MedDRA source code id\",\n"
            + "    mr.conceptName as \"MedDRA source code name\",\n"
            + "    me.targetId as \"SNOMED target code id\",\n"
            + "    me.targetName as \"SNOMED target code name\",\n"
            + "    case \n"
            + "        when me.targetId not in (select scopeConcepts from map_projects_scope_concepts where id = 1) \n"
            + "            then 'Not in scope concepts' \n"
            + "        when me.targetId in (select scopeExcludedConcepts from map_projects_scope_excluded_concepts where id = 1) \n"
            + "            then 'In excluded scope concepts' \n"
            + "		when me.targetId in (select c.id from map_projects mp, concepts c \n"
            + "                             where mp.id = mr.mapProjectId \n"
            + "                              and mr.id = me.mapRecord_id \n"
            + "                              and me.targetId = c.id\n"
            + "							  and c.terminology=mp.sourceTerminology\n"
            + "							  and c.terminologyVersion=mp.sourceTerminologyVersion\n"
            + "                              and c.active != 1)\n"
            + "            then 'Inactive Destination Concept' \n"
            + "		else 'Unknown' \n"
            + "    end as \"Reason\"\n"
            + "from map_records mr \n"
            + "join map_entries me\n"
            + "	on mr.id=me.mapRecord_id\n"
            + "where \n"
            + "	workflowStatus in (\"READY_FOR_PUBLICATION\",\"PUBLISHED\") \n"
            + "    and mapProjectId = 2\n"
            + "    and me.targetId != \"\"\n"
            + "    and (me.targetId not in (SELECT scopeConcepts FROM mappingservicedb.map_projects_scope_concepts where id = 1)\n"
            + "		or me.targetId in (select scopeExcludedConcepts from map_projects_scope_excluded_concepts where id = 1)\n"
            + "         or me.targetId in (select c.id from map_projects mp, concepts c \n"
            + "                            where mp.id = mr.mapProjectId \n"
            + "                              and mr.id = me.mapRecord_id \n"
            + "                              and me.targetId = c.id\n"
            + "							  and c.terminology=mp.sourceTerminology\n"
            + "							  and c.terminologyVersion=mp.sourceTerminologyVersion\n"
            + "                              and c.active != 1));");

        List<Object[]> objects = query.getResultList();

        List<String> results = new ArrayList<>();
        // Add header row
        results.add(
            "MedDRA source code id\tMedDRA source code name\tSNOMED target code id\tSNOMED target code name\tReason");

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
    	
    	}
    	catch (Exception e) {
	      e.printStackTrace();
	      throw new Exception(
	          "ICD10CM SQLReport mojo failed to complete", e);
	    }
    }

  
  private static void sendEmail(String fileName) throws Exception {

	    Properties config;
	    try {
	      config = ConfigUtility.getConfigProperties();
	    } catch (Exception e1) {
	      throw new Exception("Failed to retrieve config properties");
	    }
	    String notificationRecipients =
	            config.getProperty("sqlreport.send.notification.recipients");
	        String notificationMessage = "";
	        LOGGER.info("Request to send notification email to recipients: "
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
