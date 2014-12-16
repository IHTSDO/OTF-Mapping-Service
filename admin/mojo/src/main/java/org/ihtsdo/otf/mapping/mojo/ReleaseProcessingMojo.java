package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
   * @parameter refsetId
   */
  private String refsetId = null;

  /**
   * The refSet id
   * 
   * @parameter outputDirName
   */
  private String outputDirName = null;

  /**
   * The effective time of release
   * 
   * @parameter effectiveTime
   */
  private String effectiveTime = null;

  /**
   * The module id.
   * 
   * @parameter moduleId
   */
  private String moduleId = null;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Processing release");
    getLog().info("  refset.id = " + refsetId);

    // Check preconditions
    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a refsetId.");
    }

    if (refsetId == null) {
      throw new MojoExecutionException(
          "You must specify an output file directory.");
    }

    File outputDir = new File(outputDirName);
    if (!outputDir.isDirectory())
      throw new MojoExecutionException("Output file directory ("
          + outputDirName + ") could not be found.");

    if (effectiveTime == null)
      throw new MojoExecutionException("You must specify a release time");

    if (moduleId == null)
      throw new MojoExecutionException("You must specify a module id");

    try {

      MappingService mappingService = new MappingServiceJpa();
      Set<MapProject> mapProjects = new HashSet<>();

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
      for (MapProject mapProject : mappingService.getMapProjects()
          .getIterable()) {
        for (String id : refsetId.split(",")) {
          if (mapProject.getRefSetId().equals(id)) {
            mapProjects.add(mapProject);
          }
        }
      }

      // Iterate through map projects
      for (MapProject mapProject : mapProjects) {

        // Create and configure release handler
        ReleaseHandler releaseHandler = new ReleaseHandlerJpa();
        releaseHandler.setMapProject(mapProject);
        releaseHandler.setEffectiveTime(effectiveTime);
        releaseHandler.setModuleId(moduleId);
        releaseHandler.setMapProject(mapProject);
        releaseHandler.setWriteDelta(true);
        releaseHandler.setWriteSnapshot(false);
        releaseHandler.setOutputDir(outputDirName);
        if (testConcepts.length > 0) {
          List<MapRecord> mapRecords = new ArrayList<>();
          for (String terminologyId : testConcepts) {
            mapRecords.addAll(mappingService.getMapRecordsForProjectAndConcept(
                mapProject.getId(), terminologyId).getMapRecords());
          }
          releaseHandler.setMapRecords(mapRecords);
        }
        // call release handler with specific records
        releaseHandler.processRelease();
      }

      getLog().info("done ...");
      mappingService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing release processing failed.",
          e);
    }

  }

}
