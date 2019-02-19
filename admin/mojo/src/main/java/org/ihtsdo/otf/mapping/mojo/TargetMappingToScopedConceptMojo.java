package org.ihtsdo.otf.mapping.mojo;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Mojo to transform targets of a source mapping project into scope concepts for
 * another mapping project. i.e. from MedDRA to SNOMED -> SNOMED to MedDRA
 * 
 * See admin/loader/pom.xml for a sample execution
 *
 * @goal target-to-scope-concept
 */
public class TargetMappingToScopedConceptMojo extends AbstractMojo {

  /**
   * Name of source mapping project to retrieve targets
   * 
   * @parameter
   * @required
   */
  private String sourceMapProjectName;

  /**
   * Name of target mapping project to create scoped concepts
   * 
   * @parameter
   * @required
   */
  private String targetMapProjectName;

  public void execute() throws MojoExecutionException, MojoFailureException {

    try {
      doWork();
    } catch (MojoExecutionException me) {
      throw me;
    } catch (Exception e) {
      throw new MojoExecutionException("Mojo failed to execute.");
    }
  }

  private void doWork() throws Exception {

    MapProject sourceMapProject = new MapProjectJpa();
    MapProject targetMapProject = new MapProjectJpa();

    // get target ids from MED-to-SCT that are ready for publication.
    try (MappingService mappingService = new MappingServiceJpa();
        WorkflowService workflowService = new WorkflowServiceJpa()) {

      final MapProjectList mapProjectList = mappingService.getMapProjects();
      for (MapProject mapProject : mapProjectList.getMapProjects()) {
        if (mapProject.getName().equals(sourceMapProjectName)) {
          sourceMapProject = mapProject;
          break;
        }
      }

      for (MapProject mapProject : mapProjectList.getMapProjects()) {
        if (mapProject.getName().equals(targetMapProjectName)) {
          targetMapProject = mapProject;
          break;
        }
      }

      if (sourceMapProject.getId() == null) {
        throw new MojoExecutionException(
            "Source project name not found.  Please verify parameters.");
      }

      if (targetMapProject.getId() == null) {
        throw new MojoExecutionException(
            "Target project name not found.  Please verify parameters.");
      }

      final List<String> newScopeConceptList = mappingService
          .getTargetCodeForReadyForPublication(sourceMapProject.getId());

      if (newScopeConceptList != null && !newScopeConceptList.isEmpty()) {
        // upload code as scope concepts to other project
        for (String oldScopedConcept : targetMapProject.getScopeConcepts()) {
          targetMapProject.removeScopeConcept(oldScopedConcept);
        }
        for (String newScopedConcept : newScopeConceptList) {
          targetMapProject.addScopeConcept(newScopedConcept);
        }

        // recompute workflow
        Logger.getLogger(getClass()).info("Computing workflow for "
            + targetMapProject.getName() + ", " + targetMapProject.getId());
        workflowService.computeWorkflow(targetMapProject);
      }

      Logger.getLogger(getClass())
          .info("Complete loading scoped concepts to target map project");

    } catch (Exception e) {
      Logger.getLogger(getClass()).error(
          "ERROR occured during processing of scoped concepts between map projects.",
          e);
      throw e;
    }

  }

}
