package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;

/**
 * Loads unpublished complex maps.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal remove-reports-id
 * @phase package
 */
public class ReportRemoverByIdMojo extends AbstractMojo {


  /**
   * Starting id
   * @parameter
   */
  private String startId = null;

  /**
   * Ending id
   * @parameter
   */
  private String endId = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting to remove reports");
    getLog().info("  startId = " + startId);
    getLog().info("  endId = " + endId);

    try {

      // check params
      if (startId == null && endId == null) {
        throw new Exception(
            "This call will delete all reports.");
      }

      

      MappingService mappingService = new MappingServiceJpa();

      // Remove reports
      ReportService reportService = new ReportServiceJpa();
      
      reportService.removeReportsForIdRange(Long.parseLong(startId), Long.parseLong(endId));

      mappingService.close();
      reportService.close();
      getLog().info("Done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Report remover failed.", e);
    }

  }
}
