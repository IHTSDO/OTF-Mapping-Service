package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.handlers.ReleaseHandlerJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;

/**
 * Goal which finishes a release and loads the release version of the complex
 * map refset members for the project back in.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal finish-release
 * 
 * @phase package
 */
public class ReleaseFinishMojo extends AbstractMojo {

  /**
   * The refSet id.
   * @parameter
   */
  private String refsetId = null;

  /**
   * The RF2 input file
   * @parameter
   * @required
   */
  private String inputFile;

  /**
   * Flag indicating test mode
   * @parameter
   */
  private boolean testModeFlag = false;

  /**
   * Instantiates a {@link ReleaseFinishMojo} from the specified parameters.
   * 
   */
  public ReleaseFinishMojo() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Finishing RF2 Release");
    getLog().info("  refsetId = " + refsetId);
    getLog().info("  inputFile = " + inputFile);
    getLog().info("  testModeFlag = " + testModeFlag);

    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a ref set id");
    }

    if (refsetId.contains(",")) {
      throw new MojoExecutionException(
          "You must specify only a single ref set id");
    }

    if (inputFile == null) {
      throw new MojoExecutionException("You must specify an input file");
    }

    try {

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = null;
      for (MapProject project : mappingService.getMapProjects().getIterable()) {
        if (project.getRefSetId().equals(refsetId)) {
          mapProject = project;
          break;
        }
      }
      if (mapProject == null) {
        throw new Exception("Unable to find map project for refset " + refsetId);
      }

      //
      // Determine version
      //
      getLog().info("  terminology = " + mapProject.getSourceTerminology());
      getLog().info("  version = " + mapProject.getSourceTerminologyVersion());

      // Begin the release
      ReleaseHandler releaseHandler = new ReleaseHandlerJpa(testModeFlag);
      getLog().info(
          "  Handle project " + mapProject.getName() + ", "
              + mapProject.getId());
      releaseHandler.setMapProject(mapProject);
      releaseHandler.setInputFile(inputFile);
      releaseHandler.finishRelease();

      getLog().info("Done...");

      // Clean-up
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

}
