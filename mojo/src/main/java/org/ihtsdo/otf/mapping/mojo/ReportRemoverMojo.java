/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;

/**
 * 
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal remove-reports
 * @phase package
 */
public class ReportRemoverMojo extends AbstractOtfMappingMojo {
  /**
   * The refSet id
   * @parameter refsetId
   */
  private String refsetId = null;

  /**
   * An optional start date
   * @parameter
   */
  private String startDate = null;

  /**
   * An optional end date
   * @parameter
   */
  private String endDate = null;
  
  /**
   * A keep manual runs
   * @parameter
   */
  private boolean keepManualRuns = false;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting to remove reports");
    getLog().info("  refsetId = " + refsetId);
    getLog().info("  startDate = " + startDate);
    getLog().info("  keepManualRuns = " + keepManualRuns);
    getLog().info("  endDate = " + endDate);

    setupBindInfoPackage();

    try {
      // check params
      if (refsetId == null && startDate == null && endDate == null) {
        throw new Exception(
            "This call will delete all reports, if you really want to do this, use a start date of 19700101");
      }

      // parse the dates
      Date start = new Date(0);
      Date end = new Date();
      DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
      if (startDate != null) {
        start = dateFormat.parse(startDate);
      }
      if (endDate != null) {
        end = dateFormat.parse(endDate);
      }
      getLog().info("  start = " + start);
      getLog().info("  end = " + end);

      MappingService mappingService = new MappingServiceJpa();
      Set<MapProject> mapProjects = new HashSet<>();

      if (refsetId == null) {
        mapProjects =
            new HashSet<>(mappingService.getMapProjects().getMapProjects());
      } else {
        for (MapProject mapProject : mappingService.getMapProjects()
            .getIterable()) {
          for (String id : refsetId.split(",")) {
            if (mapProject.getRefSetId().equals(id)) {
              mapProjects.add(mapProject);
            }
          }
        }
      }

      // Remove reports
      ReportService reportService = new ReportServiceJpa();
      for (MapProject mapProject : mapProjects) {
        reportService.removeReportsForMapProject(mapProject, start, end, keepManualRuns);
      }

      mappingService.close();
      reportService.close();
      getLog().info("Done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Report remover failed.", e);
    }

  }
}