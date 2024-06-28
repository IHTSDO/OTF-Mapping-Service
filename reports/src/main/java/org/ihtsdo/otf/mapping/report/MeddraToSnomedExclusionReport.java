/*
 *    Copyright 2024 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.report;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
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
            + "		when me.targetId in (select c.terminologyId from map_projects mp, concepts c \n"
            + "                             where mp.id = mr.mapProjectId \n"
            + "                              and mr.id = me.mapRecord_id \n"
            + "                              and me.targetId = c.terminologyId\n"
            + "							  and c.terminology=mp.destinationTerminology\n"
            + "							  and c.terminologyVersion=mp.destinationTerminologyVersion\n"
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
            + "         or me.targetId in (select c.terminologyId from map_projects mp, concepts c \n"
            + "                            where mp.id = mr.mapProjectId \n"
            + "                              and mr.id = me.mapRecord_id \n"
            + "                              and me.targetId = c.terminologyId\n"
            + "							  and c.terminology=mp.destinationTerminology\n"
            + "							  and c.terminologyVersion=mp.destinationTerminologyVersion\n"
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

        final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        final String dateStamp = dateFormat.format(new Date());
        final String filename = "/meddraToSnomed_ExclusionReport_" + dateStamp;
        final File resultFile = ConfigUtility.createFile(filename, results);

        // Zip results file
        final File zipFile = ConfigUtility.zipFile(filename, resultFile);

        // Send file to recipients
        emailReportFile("[OTF-Mapping-Tool] MedDRA to SNOMED Exclusion Report",
            zipFile.getAbsolutePath(),
            "report.send.notification.recipients.meddra."
                + MeddraToSnomedExclusionReport.class.getSimpleName(),
            "Hello,\n\nThe MedDRA to SNOMED Exclusion report has been generated.");

        LOGGER.info("MedDRA to SNOMED Exclusion Report completed.");

      } catch (Exception e) {

        LOGGER.error("ERROR", e);

        emailReportError("Error generating MedDRA to SNOMED Exclusion Report",
            "report.send.notification.recipients.meddra."
                + MeddraToSnomedExclusionReport.class.getSimpleName(),
            "There was an error generating the MedDRA to SNOMED Exclusion Report.  Please contact support for assistance.");

        throw new Exception("MedDRA to SNOMED Exclusion Report failed to complete",
            e);
      }
  } 
}
