package org.ihtsdo.otf.mapping.mojo;

import javax.persistence.NoResultException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Creates a new project with the specified user as administrator
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal create-new-project
 * @phase package
 */
public class CreateMapAdministratorMojo extends AbstractMojo {

  /**
   * The user name to set as administrator
   * @parameter
   * @required
   */
  private String mapUser = null;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Start creating an admin user");
    getLog().info("  mapUser = " + mapUser);

    if (mapUser == null)
      throw new MojoExecutionException(
          "You must specify a user name to serve as project administrator");

    try {

      MappingService mappingService = new MappingServiceJpa();

      MapUser newAdmin = null;
      try {
        newAdmin = mappingService.getMapUser(mapUser);

        // set the application role to ADMINISTRATOR and update user
        newAdmin.setApplicationRole(MapUserRole.ADMINISTRATOR);
        mappingService.updateMapUser(newAdmin);
      } catch (NoResultException e) {

        newAdmin = new MapUserJpa();
        newAdmin.setName(mapUser);
        newAdmin.setUserName(mapUser);
        newAdmin.setEmail("(Email not yet set)");
        newAdmin.setApplicationRole(MapUserRole.ADMINISTRATOR);
        mappingService.addMapUser(newAdmin);
      }

      MapProjectList mapProjects = mappingService.getMapProjects();

      if (mapProjects.getCount() == 0) {

        // create a blank project
        MapProject project = new MapProjectJpa();
        project.setDestinationTerminology("");
        project.setDestinationTerminologyVersion("");
        project.setGroupStructure(false);
        project.setMapRefsetPattern(MapRefsetPattern.SimpleMap);
        project.setMapRelationStyle(RelationStyle.MAP_CATEGORY_STYLE);
        project.setName("Blank Project");
        project.setPropagatedFlag(false);
        project.setPropagationDescendantThreshold(0);
        project.setPublic(false);
        project.setPublished(false);
        project.setRefSetId("");
        project.setRefSetName("");
        project.setRuleBased(false);
        project.setSourceTerminology("");
        project.setSourceTerminologyVersion("");
        project.setWorkflowType(WorkflowType.CONFLICT_PROJECT);

        // add project
        mappingService.addMapProject(project);

        mappingService.close();
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Creation of blank map project failed",
          e);
    }

  }
}