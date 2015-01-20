package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.handlers.BeginEditingCycleHandlerJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.BeginEditingCycleHandler;

/**
 * Goal which initiates a new editing cycle.  Mostly this is
 * for tracking purposes.
 * 
 * See admin/release/pom.xml for a sample execution.
 * 
 * @goal begin-editing-cycle
 * 
 * @phase package
 */
public class EditingCycleBeginMojo extends AbstractMojo {

  /**
   * The refSet id.
   * @parameter
   */
  private String refsetId = null;

  /**
   * Instantiates a {@link EditingCycleBeginMojo} from the specified parameters.
   * 
   */
  public EditingCycleBeginMojo() {
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

    if (refsetId == null) {
      throw new MojoExecutionException("You must specify a ref set id");
    }

    if (refsetId.contains(",")) {
      throw new MojoExecutionException(
          "You must specify only a single ref set id");
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
      BeginEditingCycleHandler handler = new BeginEditingCycleHandlerJpa();
      getLog().info(
          "  Handle project " + mapProject.getName() + ", "
              + mapProject.getId());
      handler.setMapProject(mapProject);
      handler.beginEditingCycle();

      getLog().info("Done...");

      // Clean-up
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

}
