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
    getLog().info("Start creating a blank project");
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

      // create a new project
      String projectName = "Blank Project";

      MapProjectList mapProjects = mappingService.getMapProjects();

      boolean projectExists = false;
      do {

        for (MapProject mapProject : mapProjects.getMapProjects()) {
          if (mapProject.getName().equals(projectName))
            projectExists = true;
        }
        
        if (projectExists == true) {

          // if project name ends in character, increment it
          if (Character.isDigit(projectName.charAt(projectName.length() - 1))) {
  
            projectName =
                projectName.substring(0, projectName.length() - 2)
                    + (Integer
                        .valueOf(projectName.charAt(projectName.length() - 1)) + 1);
  
            // else add a differentiating number
          } else {
            projectName += " 2";
          }
        }

      } while (projectExists == true);

      // create the project
      MapProject project = new MapProjectJpa();
      project.setDestinationTerminology("");
      project.setDestinationTerminologyVersion("");
      project.setGroupStructure(false);
      project.setMapRefsetPattern(MapRefsetPattern.SimpleMap);
      project.setMapRelationStyle(RelationStyle.MAP_CATEGORY_STYLE);
      project.setName(projectName);
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

      // add user as administrator and add project
      project.addMapAdministrator(newAdmin);
      mappingService.addMapProject(project);

      mappingService.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Creation of blank map project failed",
          e);
    }
  }
}