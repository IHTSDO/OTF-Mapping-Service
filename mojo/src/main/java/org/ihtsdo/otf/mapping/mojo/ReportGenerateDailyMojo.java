/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.helpers.OtfErrorHandler;

/**
 * Admin tool to create historical reporting data on a daily basis
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal generate-daily-reports
 * 
 * @phase package
 */
public class ReportGenerateDailyMojo extends AbstractOtfMappingMojo {

  /**
   * Name of terminology to be loaded.
   * 
   * @parameter
   */
  private String refsetId = null;

  /**
   * Start date
   * 
   * @parameter
   * @required
   */
  private String startDate = null;

  /**
   * End date
   * 
   * @parameter
   */
  private String endDate = null;

  /**
   * Instantiates a {@link ReportGenerateDailyMojo} from the specified
   * parameters.
   * 
   */
  public ReportGenerateDailyMojo() {
    // do nothing
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting generation of daily reports");
    getLog().info("  refsetId = " + refsetId);
    getLog().info("  startDate = " + startDate);
    getLog().info("  endDate = " + endDate);

    MapUser mapUser = null;
    try (final ReportService reportService = new ReportServiceJpa();
        final MappingService mappingService = new MappingServiceJpa();) {

      setupBindInfoPackage();

      if (startDate == null) {
        throw new MojoFailureException("You must specify a start date");
      }

      // parse the dates
      DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
      Date start = dateFormat.parse(startDate);
      Date end = endDate == null ? new Date() : dateFormat.parse(endDate);

      getLog().info("Parsed start date: " + start.toString());
      getLog().info("Parsed end date:   " + end.toString());

      // Determine map projects to generate reports for
      List<MapProject> mapProjects = new ArrayList<>();
      if (refsetId != null) {
        String refsetIds[] = refsetId.split(",");

        // retrieve the map project objects
        for (String id : refsetIds) {
          MapProject mapProject = mappingService.getMapProjectForRefSetId(id);
          getLog().info("  Found project " + mapProject.getId() + " "
              + mapProject.getName());
          mapProjects.add(mapProject);
        }
      } else {
        for (MapProject project : mappingService.getMapProjects()
            .getMapProjects()) {
          getLog().info(
              "  Found project " + project.getId() + " " + project.getName());
          mapProjects.add(project);
        }
      }

      // get the loader map user
      mapUser = mappingService.getMapUser("loader");

      // Generate reports for all projects
      for (MapProject mp : mapProjects) {
        getLog().info("  Generate reports for project " + mp.getName());
        reportService.generateReportsForDateRange(mp, mapUser, start, end);
      }

      // Cleanup and end
      getLog().info("Done ...");

    } catch (Exception e) {
      e.printStackTrace();
      // Send email if something went wrong
      OtfErrorHandler errorHandler = new OtfErrorHandler();
      errorHandler.handleException(e, "Error generating reports", "admin mojo",
          refsetId == null ? "Project could not be retrieved" : refsetId, "");
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }
}
