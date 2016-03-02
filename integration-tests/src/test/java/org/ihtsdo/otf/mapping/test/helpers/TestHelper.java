package org.ihtsdo.otf.mapping.test.helpers;

import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.rest.SecurityServiceRest;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.junit.Ignore;

/**
 * The Class TestHelper.
 */
@Ignore("Utility class for other tests")
public class TestHelper {

  /** The content service. */
  ContentService contentService;

  /** The workflow service. */
  WorkflowService workflowService;

  /** The security service. */
  SecurityServiceRest securityService;

  /**
   * Instantiates an empty {@link TestHelper}.
   *
   * @throws Exception the exception
   */
  public TestHelper() throws Exception {
    contentService = new ContentServiceJpa();
    securityService = new SecurityServiceRest();
    workflowService = new WorkflowServiceJpa();
  }

  /**
   * Creates the test data.
   *
   * @throws Exception the exception
   */
  public void createTestData() throws Exception {
    // create the test data
    createAndAuthenticateMapUsers();
    createMapProject("project", WorkflowType.SIMPLE_PATH);
  }

  /**
   * Creates the and authenticate map users.
   *
   * @throws Exception the exception
   */
  private void createAndAuthenticateMapUsers() throws Exception {
    MapUser user;

    user = new MapUserJpa();
    user.setName("viewer");
    user.setUserName("viewer");
    user.setEmail("viewer");
    user.setApplicationRole(MapUserRole.VIEWER);
    workflowService.addMapUser(user);
    securityService.authenticate(user.getUserName(), user.getUserName());

    user = new MapUserJpa();
    user.setName("specialist1");
    user.setUserName("specialist1");
    user.setEmail("specialist1");
    user.setApplicationRole(MapUserRole.SPECIALIST);
    workflowService.addMapUser(user);
    securityService.authenticate(user.getUserName(), user.getUserName());

    user = new MapUserJpa();
    user.setName("specialist2");
    user.setUserName("specialist2");
    user.setEmail("specialist2");
    user.setApplicationRole(MapUserRole.SPECIALIST);
    workflowService.addMapUser(user);
    securityService.authenticate(user.getUserName(), user.getUserName());

    user = new MapUserJpa();
    user.setName("lead");
    user.setUserName("lead");
    user.setEmail("lead");
    user.setApplicationRole(MapUserRole.LEAD);
    workflowService.addMapUser(user);
    securityService.authenticate(user.getUserName(), user.getUserName());

    user = new MapUserJpa();
    user.setName("admin");
    user.setUserName("admin");
    user.setEmail("admin");
    user.setApplicationRole(MapUserRole.ADMINISTRATOR);
    workflowService.addMapUser(user);
    securityService.authenticate(user.getUserName(), user.getUserName());
  }

  /**
   * Creates the map project.
   *
   * @param name the name
   * @param workflowType the workflow type
   * @throws Exception the exception
   */
  private void createMapProject(String name, WorkflowType workflowType)
    throws Exception {
    // instantiate the project
    MapProject mapProject = new MapProjectJpa();
    mapProject.setSourceTerminology("sourceTerminology");
    mapProject.setSourceTerminologyVersion("sourceTerminologyVersion");
    mapProject.setDestinationTerminology("destinationTerminology");
    mapProject
        .setDestinationTerminologyVersion("destinationTerminologyVersion");
    mapProject.setGroupStructure(false);
    mapProject.setMapRefsetPattern(MapRefsetPattern.ExtendedMap);
    mapProject.setMapRelationStyle(RelationStyle.MAP_CATEGORY_STYLE);
    mapProject.setName("Test Project");
    mapProject.setPropagatedFlag(false);
    mapProject
        .setProjectSpecificAlgorithmHandlerClass("org.ihtsdo.otf.mapping.jpa.handlers.ICD10ProjectSpecificAlgorithmHandler");
    mapProject.setPublic(true);
    mapProject.setRefSetId("refsetId");
    mapProject.setRuleBased(true);
    mapProject.setWorkflowType(WorkflowType.CONFLICT_PROJECT);
    mapProject.addMapSpecialist(workflowService.getMapUser("specialist1"));
    mapProject.addMapSpecialist(workflowService.getMapUser("specialist2"));
    mapProject.addMapLead(workflowService.getMapUser("lead"));

    mapProject.addScopeConcept("1");
    workflowService.addMapProject(mapProject);
  }

  /**
   * Clear test data.
   *
   * @throws Exception the exception
   */
  public void clearTestData() throws Exception {
    for (MapProject obj : workflowService.getMapProjects().getMapProjects()) {
      workflowService.removeMapProject(obj.getId());
    }
    for (MapRecord obj : workflowService.getMapRecords().getMapRecords()) {
      workflowService.removeMapRecord(obj.getId());
    }
    for (MapPrinciple obj : workflowService.getMapPrinciples()
        .getMapPrinciples()) {
      workflowService.removeMapPrinciple(obj.getId());
    }
    for (MapAdvice obj : workflowService.getMapAdvices().getMapAdvices()) {
      workflowService.removeMapAdvice(obj.getId());
    }
    for (MapAgeRange obj : workflowService.getMapAgeRanges().getMapAgeRanges()) {
      workflowService.removeMapAdvice(obj.getId());
    }
    for (MapRelation obj : workflowService.getMapRelations().getMapRelations()) {
      workflowService.removeMapAdvice(obj.getId());
    }
    for (MapUserPreferences obj : workflowService.getMapUserPreferences()
        .getMapUserPreferences()) {
      workflowService.removeMapAdvice(obj.getId());
    }
    for (MapUser obj : workflowService.getMapUsers().getMapUsers()) {
      workflowService.removeMapUser(obj.getId());
    }

  }

  /**
   * Close.
   *
   * @throws Exception the exception
   */
  public void close() throws Exception {
    contentService.close();
    workflowService.close();
  }

}