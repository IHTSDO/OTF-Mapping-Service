package org.ihtsdo.otf.mapping.mojo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
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
public class ReportGenerateDailyMojo extends AbstractMojo {

  /**
   * Name of terminology to be loaded.
   * 
   * @parameter
   * @required
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
   * 
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


    ReportService reportService = null;
    MapUser mapUser = null;
    MapProject mapProject = null;
    try {

      reportService = new ReportServiceJpa();

      if (refsetId == null)
        throw new MojoFailureException(
            "You must specify at least one ref set id");
      else
        getLog().info("refsetId(s): " + refsetId);

      if (startDate == null)
        throw new MojoFailureException("You must specify a start date");
      else
        getLog().info("Start date: " + startDate);

      if (endDate == null)
        getLog().warn(
            "No end date specified.  Reports will be computed through today");
      else
        getLog().info("End date:   " + endDate);

      // parse the dates
      DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
      Date start = dateFormat.parse(startDate);
      Date end = endDate == null ? new Date() : dateFormat.parse(endDate);

      getLog().info("Parsed start date: " + start.toString());
      getLog().info("Parsed end date:   " + end.toString());

      MappingService mappingService = new MappingServiceJpa();

      String refsetIds[] = refsetId.split(",");
      List<MapProject> mapProjects = new ArrayList<>();

      // retrieve the map project objects
      for (String id : refsetIds) {
        mapProject = mappingService.getMapProjectForRefSetId(id);
        mapProjects.add(mapProject);
      }

      // get the loader map user
      mapUser = mappingService.getMapUser("loader");

      mappingService.close();

      for (MapProject mp : mapProjects) {
        reportService.generateReportsForDateRange(mp, mapUser, start, end);
      }
      reportService.close();

      getLog().info("Done ...");

    } catch (Exception e) {

      OtfErrorHandler errorHandler = new OtfErrorHandler();

      errorHandler.handleException(
          e,
          "Error generating reports",
          "admin mojo",
          mapProject == null ? "Project could not be retrieved" : mapProject
              .getName(), "");

      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }
}
