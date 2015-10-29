package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.handlers.ReleaseHandlerJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;

/**
 * Loads unpublished complex maps.
 * 
 * See admin/release/pom.xml for a sample execution.
 * 
 * <pre>
 * % mvn -PRelease -Drun.config=/home/ihtsdo/config/config.properties \
 *       -Drefset.id=450993002 -Doutput.dir=/tmp -Dtime=20150131 \
 *       -Dmodule.id=900000000000207008 install
 * </pre>
 * 
 * @goal release
 * @phase package
 */
public class ReleaseProcessingMojo extends AbstractMojo {

  /**
   * The refSet id
   * 
   * @parameter
   */
  private String refsetId = null;

  /**
   * The refSet id
   * 
   * @parameter
   */
  private String outputDir = null;

  /**
   * The effective time of release
   * 
   * @parameter
   */
  private String effectiveTime = null;

  /**
   * The module id.
   * 
   * @parameter
   */
  private String moduleId = null;

  /**
   * Flag indicating test mode
   * @parameter
   */
  private boolean testModeFlag = false;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Processing RF2 release");
    getLog().info("  refsetId = " + refsetId);
    getLog().info("  outputDir = " + outputDir);
    getLog().info("  effectiveTime = " + effectiveTime);
    getLog().info("  moduleId = " + moduleId);
    getLog().info("  testModeFlag = " + testModeFlag);

    // Check preconditions
    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a refsetId.");
    }

    if (refsetId.contains(",")) {
      throw new MojoExecutionException(
          "You must specify only a single ref set id");
    }

    if (outputDir == null) {
      throw new MojoExecutionException(
          "You must specify an output file directory.");
    }

    File outputDirFile = new File(outputDir);
    if (!outputDirFile.isDirectory())
      throw new MojoExecutionException("Output file directory (" + outputDir
          + ") could not be found.");

    if (effectiveTime == null)
      throw new MojoExecutionException("You must specify a release time");

    if (moduleId == null)
      throw new MojoExecutionException("You must specify a module id");

    try {

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = null;

      // ///////////////////
      // Test Parameters //
      // ///////////////////

      String testConcepts[] = {};

      /*
       * {"771000119108", "741000119101", "140131000119102", "711000119100",
       * "140101000119109", "751000119104", "71421000119105", "140111000119107",
       * "140121000119100", "731000119105", "721000119107"};
       */

      // Get Projects
      for (MapProject project : mappingService.getMapProjects().getIterable()) {
        if (project.getRefSetId().equals(refsetId)) {
          mapProject = project;
          break;
        }
      }

      // Create and configure release handler
      ReleaseHandler releaseHandler = new ReleaseHandlerJpa(testModeFlag);
      releaseHandler.setMapProject(mapProject);
      releaseHandler.setEffectiveTime(effectiveTime);
      releaseHandler.setModuleId(moduleId);
      releaseHandler.setMapProject(mapProject);
      releaseHandler.setWriteDelta(true);
      releaseHandler.setWriteSnapshot(true);
      releaseHandler.setOutputDir(outputDir);
      if (testConcepts.length > 0) {
        List<MapRecord> mapRecords = new ArrayList<>();
        for (String terminologyId : testConcepts) {
          mapRecords.addAll(mappingService.getMapRecordsForProjectAndConcept(
              mapProject.getId(), terminologyId).getMapRecords());
        }
        releaseHandler.setMapRecords(mapRecords);
      }
      // call release handler with specific records
      getLog().info(
          "  Handle project " + mapProject.getName() + ", "
              + mapProject.getId());
      releaseHandler.processRelease();

      getLog().info("done ...");
      mappingService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing release processing failed.",
          e);
    }

  }

}
