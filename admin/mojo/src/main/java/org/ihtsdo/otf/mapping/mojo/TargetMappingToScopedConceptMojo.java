package org.ihtsdo.otf.mapping.mojo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

/**
 * Mojo to transform targets of a source mapping project into scope concepts for
 * another mapping project. i.e. from MedDRA to SNOMED -> SNOMED to MedDRA
 * 
 * See admin/loader/pom.xml for a sample execution
 *
 * @goal target-to-scope-concept
 */
public class TargetMappingToScopedConceptMojo extends AbstractOtfMappingMojo {

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
      transfer();
    } catch (MojoExecutionException me) {
      throw me;
    } catch (Exception e) {
      throw new MojoExecutionException("Mojo failed to execute.");
    }
  }

  private void transfer() throws Exception {

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

      final Map<String, String> newScopeConceptList = mappingService
          .getTargetCodeForReadyForPublication(sourceMapProject.getId());

      if (newScopeConceptList != null && !newScopeConceptList.isEmpty()) {
        // upload code as scope concepts to other project
        final Set<String> oldScopedConceptList = new HashSet<>(
            targetMapProject.getScopeConcepts());

        for (String oldScopedConcept : oldScopedConceptList) {
          Logger.getLogger(getClass()).info("Removing scoped concept "
              + oldScopedConcept + " from target map project.");
          targetMapProject.removeScopeConcept(oldScopedConcept);
          mappingService.updateMapProject(targetMapProject);
        }
        
        for (Map.Entry<String, String> newScopedConcept : newScopeConceptList.entrySet()) {
          Logger.getLogger(getClass()).info("Adding scoped concept "
              + newScopedConcept.getKey() + " to target map project id: " + targetMapProject.getId());
          targetMapProject.addScopeConcept(newScopedConcept.getKey());
          mappingService.updateMapProject(targetMapProject);
        }

        // recompute workflow
        Logger.getLogger(getClass()).info("Computing workflow for "
            + targetMapProject.getName() + ", " + targetMapProject.getId());
        workflowService.computeWorkflow(targetMapProject);
        
        workflowService.beginTransaction();
        Logger.getLogger(getClass()).info("Updating tracking records for mapProjectId: " + targetMapProject.getId());
        for (Map.Entry<String, String> newScopedConcept : newScopeConceptList
            .entrySet()) {
          
            Logger.getLogger(getClass())
                .info("Updating tracking record - terminologyId: " + newScopedConcept.getKey()
                    + " with team name: " + newScopedConcept.getValue() + " in target map project.");
            
            TrackingRecord tr = workflowService
                .getTrackingRecord(targetMapProject, newScopedConcept.getKey());
  
            if (tr != null) {             
              tr.setAssignedTeamName(newScopedConcept.getValue());
              workflowService.updateTrackingRecord(tr);
            }

        }
        workflowService.commit();
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
