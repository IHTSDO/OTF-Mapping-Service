/*
 * 
 */
package org.ihtsdo.otf.mapping.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.FeedbackConversationList;
import org.ihtsdo.otf.mapping.helpers.FeedbackConversationListJpa;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.FeedbackConversation;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.WorkflowException;
import org.ihtsdo.otf.mapping.workflow.WorkflowExceptionJpa;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * The Workflow Services REST package.
 */
@Path("/workflow")
@Api(value = "/workflow", description = "Operations supporting workflow.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class WorkflowServiceRest extends RootServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link WorkflowServiceRest}.
   * @throws Exception
   */
  public WorkflowServiceRest() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /**
   * Compute workflow.
   * 
   * @param mapProjectId the map project id
   * @param authToken
   */
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/compute")
  @ApiOperation(value = "Compute workflow for a map project.", notes = "Recomputes workflow for the specified map project.")
  public void computeWorkflow(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/compute");

    String userName = "";
    String project = "";

    try {
      // authorize call
      userName = securityService.getUsernameForToken(authToken);
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to compute workflow.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      project = mapProject.getName();
      mappingService.close();
      WorkflowService workflowService = new WorkflowServiceJpa();
      workflowService.computeWorkflow(mapProject);
      workflowService.close();
      return;
    } catch (Exception e) {
      handleException(e, "trying to compute workflow", userName, project, "");
    }
  }

  /**
   * Finds available concepts for the specified map project and user.
   * 
   * @param mapProjectId the map project id
   * @param userName the username
   * @param query
   * @param pfsParameter the paging parameter
   * @param authToken
   * @return the search result list
   */
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/query/{query}/availableConcepts")
  @ApiOperation(value = "Find available concepts.", notes = "Gets a list of search results for concepts available to be worked on for the specified parameters.", response = SearchResultList.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAvailableConcepts(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'", required = true) @PathParam("query") String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id/" + userName + "/availableConcepts"
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String project = "";
    String user = "";

    try {
      // System.out.println("Authorizing at: " + System.currentTimeMillis()
      // / 1000);

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to find available work.")
            .build());

      // System.out.println("Retrieving objects at "
      // + System.currentTimeMillis() / 1000);

      // retrieve the project and user
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);
      user = mapUser.getUserName();
      mappingService.close();

      // System.out.println("Getting available work at "
      // + System.currentTimeMillis() / 1000);

      // retrieve the workflow tracking records
      WorkflowService workflowService = new WorkflowServiceJpa();
      SearchResultList results =
          workflowService.findAvailableWork(mapProject, mapUser, query,
              pfsParameter);
      workflowService.close();

      // Check that all concepts referenced here are active (and thus have
      // a tree position)
      // This is a temporary fix, which will be removed once a more robust
      // solution to scope concepts is found
      // SEE MAP-862
      SearchResultList revisedResults = new SearchResultListJpa();
      ContentService contentService = new ContentServiceJpa();
      for (SearchResult result : results.getIterable()) {

        Concept c =
            contentService.getConcept(result.getTerminologyId(),
                mapProject.getSourceTerminology(),
                mapProject.getSourceTerminologyVersion());
        if (c == null) {
          Logger.getLogger(WorkflowServiceJpa.class).warn(
              "Could not get concept " + result.getTerminologyId() + ", "
                  + mapProject.getSourceTerminology() + ", "
                  + mapProject.getSourceTerminologyVersion());
        } else if (c.isActive() == true) {
          revisedResults.addSearchResult(result);
        } else {
          Logger.getLogger(WorkflowServiceJpa.class).warn(
              "Skipping inactive concept " + result.getTerminologyId());
        }
      }

      revisedResults.setTotalCount(results.getTotalCount());

      // System.out.println(revisedResults.getCount());
      contentService.close();

      results = revisedResults;

      // System.out.println("Done at " + System.currentTimeMillis() / 1000);

      return results;
    } catch (Exception e) {
      handleException(e, "trying to find available work", user, project, "");
      return null;
    }
  }

  /**
   * Finds assigned concepts for the specified map project and user.
   * 
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query
   * @param pfsParameter the paging parameter
   * @param authToken
   * @return the search result list
   */
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/query/{query}/assignedConcepts")
  @ApiOperation(value = "Find assigned concepts for a map project.", notes = "Gets a list of search results of assigned concepts for the specified parameters.", response = SearchResultList.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAssignedConcepts(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'", required = true) @PathParam("query") String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id/" + userName + "/assignedConcepts"
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String project = "";
    String user = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to find assigned work.")
            .build());

      // retrieve the project and user
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);
      user = mapUser.getUserName();
      mappingService.close();

      // retrieve the workflow tracking records
      WorkflowService workflowService = new WorkflowServiceJpa();
      SearchResultList results =
          workflowService.findAssignedWork(mapProject, mapUser, query,
              pfsParameter);
      workflowService.close();

      return results;

    } catch (Exception e) {
      handleException(e, "trying to find assigned concepts", user, project, "");
      return null;
    }
  }

  /**
   * Finds available conflicts for the specified map project and user.
   * 
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query
   * @param pfsParameter the paging parameter
   * @param authToken
   * @return the search result list
   */
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/query/{query}/availableConflicts")
  @ApiOperation(value = "Find available conflicts for a map project.", notes = "Gets a list of search results of available conflicts for the specified parameters.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAvailableConflicts(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'", required = true) @PathParam("query") String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id" + userName + "/availableConflicts"
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String project = "";
    String user = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to find available conflicts.")
            .build());

      // retrieve the project and user
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);
      user = mapUser.getUserName();
      mappingService.close();

      // retrieve the workflow tracking records
      WorkflowService workflowService = new WorkflowServiceJpa();
      SearchResultList results =
          workflowService.findAvailableConflicts(mapProject, mapUser, query,
              pfsParameter);
      workflowService.close();

      return results;
    } catch (Exception e) {
      handleException(e, "trying to find available conflicts", user, project,
          "");
      return null;
    }
  }

  /**
   * Finds assigned conflicts for the specified map project and user.
   * 
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query
   * @param pfsParameter the paging parameter
   * @param authToken
   * @return the search result list
   */
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/query/{query}/assignedConflicts")
  @ApiOperation(value = "Find assigned conflicts for a map project.", notes = "Gets a list of search results of assigned conflicts for the specified parameters.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAssignedConflicts(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'", required = true) @PathParam("query") String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id/" + userName + "/assignedConflicts"
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String project = "";
    String user = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to find assigned conflicts.")
            .build());

      // retrieve the project and user
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);
      user = mapUser.getUserName();
      mappingService.close();

      // retrieve the map records
      WorkflowService workflowService = new WorkflowServiceJpa();
      SearchResultList results =
          workflowService.findAssignedConflicts(mapProject, mapUser, query,
              pfsParameter);
      workflowService.close();

      return results;
    } catch (Exception e) {
      handleException(e, "trying to find assigned conflicts", user, project, "");
      return null;
    }
  }

  /**
   * Finds available review work for the specified map project and user.
   * 
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query
   * @param pfsParameter the paging parameter
   * @param authToken
   * @return the search result list
   */
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/query/{query}/availableReviewWork")
  @ApiOperation(value = "Find available review work for a map project.", notes = "Gets a list of search results of available review work for the specified parameters.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAvailableReviewWork(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'", required = true) @PathParam("query") String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id" + userName + "/availableReviewWork"
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String project = "";
    String user = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to find available review work.")
                .build());

      // retrieve the project and user
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);
      user = mapUser.getUserName();
      mappingService.close();

      // retrieve the workflow tracking records
      WorkflowService workflowService = new WorkflowServiceJpa();
      SearchResultList results =
          workflowService.findAvailableReviewWork(mapProject, mapUser, query,
              pfsParameter);
      workflowService.close();

      return results;
    } catch (Exception e) {
      handleException(e, "trying to find available review work", user, project,
          "");
      return null;
    }
  }

  /**
   * Find available qa work.
   * 
   * @param mapProjectId the map project id
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the search result list
   */
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/query/{query}/availableQAWork")
  @ApiOperation(value = "Find available qa work for a map project.", notes = "Gets a list of search results of available qa work for the specified parameters.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAvailableQAWork(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Query, e.g. 'heart attack'", required = true) @PathParam("query") String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/availableQAWork" + " with PfsParameter: " + "\n"
            + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String project = "";
    // all qa work will have user "qa"
    String user = "qa";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to find available qa work.")
                .build());

      // retrieve the project and user
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(user);
      user = mapUser.getUserName();
      mappingService.close();

      // retrieve the workflow tracking records
      WorkflowService workflowService = new WorkflowServiceJpa();
      SearchResultList results =
          workflowService.findAvailableQAWork(mapProject, mapUser, query,
              pfsParameter);
      workflowService.close();

      return results;
    } catch (Exception e) {
      handleException(e, "trying to find available qa work", user, project, "");
      return null;
    }
  }

  /**
   * Finds assigned review work for the specified map project and user.
   * 
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query
   * @param pfsParameter the paging parameter
   * @param authToken
   * @return the search result list
   */
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/query/{query}/assignedReviewWork")
  @ApiOperation(value = "Find assigned review work for a map project.", notes = "Gets a list of search results of assigned review work for the specified parameters.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAssignedReviewWork(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'", required = true) @PathParam("query") String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id/" + userName + "/assignedReviewWork"
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String project = "";
    String user = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to find assigned review work.")
            .build());

      // retrieve the project and user
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);
      user = mapUser.getUserName();
      mappingService.close();

      // retrieve the map records
      WorkflowService workflowService = new WorkflowServiceJpa();
      SearchResultList results =
          workflowService.findAssignedReviewWork(mapProject, mapUser, query,
              pfsParameter);
      workflowService.close();

      return results;
    } catch (Exception e) {
      handleException(e, "trying to find assigned review work", user, project,
          "");
      return null;
    }
  }

  /**
   * Find assigned qa work.
   * 
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the search result list
   */
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/query/{query}/assignedQAWork")
  @ApiOperation(value = "Find assigned qa work for a map project.", notes = "Gets a list of search results of assigned qa work for the specified parameters.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAssignedQAWork(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'", required = true) @PathParam("query") String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id/" + userName + "/assignedQAWork"
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String project = "";
    String user = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to find assigned qa work.")
            .build());

      // retrieve the project and user
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);
      user = mapUser.getUserName();
      mappingService.close();

      // retrieve the map records
      WorkflowService workflowService = new WorkflowServiceJpa();
      SearchResultList results =
          workflowService.findAssignedQAWork(mapProject, mapUser, query,
              pfsParameter);
      workflowService.close();

      return results;
    } catch (Exception e) {
      handleException(e, "trying to find assigned qa work", user, project, "");
      return null;
    }
  }

  /**
   * Assign user to to work based on an existing map record.
   * 
   * @param userName the user name
   * @param mapRecord the map record (can be null)
   * @param authToken
   */
  @POST
  @Path("/assignFromRecord/user/id/{userName}")
  @ApiOperation(value = "Assign user to concept", notes = "Assigns a user (specialist or lead) to a previously mapped concept.")
  public void assignConceptFromMapRecord(
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Initial map record to copy, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /assignFromRecord/user/id/" + userName
            + " with map record id " + mapRecord.getId() + " for project "
            + mapRecord.getMapProjectId() + " and concept id "
            + mapRecord.getConceptId());

    String project = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, new Long(
              mapRecord.getMapProjectId()));
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to assign work from a map record.")
                .build());

      WorkflowService workflowService = new WorkflowServiceJpa();
      MappingService mappingService = new MappingServiceJpa();
      ContentService contentService = new ContentServiceJpa();

      MapProject mapProject =
          mappingService.getMapProject(new Long(mapRecord.getMapProjectId()));
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);
      Concept concept =
          contentService.getConcept(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      workflowService.processWorkflowAction(mapProject, concept, mapUser,
          mapRecord, WorkflowAction.ASSIGN_FROM_INITIAL_RECORD);

      mappingService.close();
      workflowService.close();
      contentService.close();

    } catch (Exception e) {
      handleException(e, "trying to assign concept from a map record",
          userName, project, mapRecord.getId().toString());
    }
  }

  /**
   * Assigns user to unmapped concept.
   * 
   * @param mapProjectId the map project id
   * @param terminologyId the terminology id
   * @param userName the user name
   * @param authToken
   */
  @POST
  @Path("/assign/project/id/{id}/concept/id/{terminologyId}/user/id/{userName}")
  @ApiOperation(value = "Assign user to concept", notes = "Assigns specified user to map the specified concept for the specified project.")
  public void assignConcept(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") String mapProjectId,
    @ApiParam(value = "Concept id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /assign/project/id/"
            + mapProjectId.toString() + "/concept/id/" + terminologyId
            + "/user/id/" + userName);

    String project = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, new Long(
              mapProjectId));
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to assign work.").build());

      WorkflowService workflowService = new WorkflowServiceJpa();
      MappingService mappingService = new MappingServiceJpa();
      ContentService contentService = new ContentServiceJpa();

      MapProject mapProject =
          mappingService.getMapProject(new Long(mapProjectId));
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);
      Concept concept =
          contentService.getConcept(terminologyId,
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      workflowService.processWorkflowAction(mapProject, concept, mapUser, null,
          WorkflowAction.ASSIGN_FROM_SCRATCH);

      mappingService.close();
      workflowService.close();
      contentService.close();

    } catch (Exception e) {
      handleException(e, "trying to assign work", userName, project,
          terminologyId);
    }
  }

  /**
   * Assign batch to user.
   * 
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param terminologyIds the terminology ids
   * @param authToken
   */
  @POST
  @Path("/assignBatch/project/id/{id}/user/id/{userName}")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Assign user to batch of concepts.", notes = "Assigns specified user to map the specified list of concept ids for the specified project.")
  public void assignBatch(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") String mapProjectId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "List of terminology ids to be assigned, in JSON or XML POST data", required = true) List<String> terminologyIds,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /assignBatch/project/id/" + mapProjectId
            + "/user/id/" + userName);

    WorkflowService workflowService = null;
    MappingService mappingService = null;
    ContentService contentService = null;

    String project = "";

    try {

      // authorize call
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, new Long(
              mapProjectId));
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to assign batches.")
            .build());

      workflowService = new WorkflowServiceJpa();
      mappingService = new MappingServiceJpa();
      contentService = new ContentServiceJpa();

      MapProject mapProject =
          mappingService.getMapProject(new Long(mapProjectId));
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);

      for (String terminologyId : terminologyIds) {
        Logger.getLogger(WorkflowServiceRest.class).info(
            "   Assigning " + terminologyId);
        Concept concept =
            contentService.getConcept(terminologyId,
                mapProject.getSourceTerminology(),
                mapProject.getSourceTerminologyVersion());

        // Only process batch assignment request if the concept is
        // active
        // and thus has a tree position
        // This is a temporary fix for the Unmapped ICD-10 project
        // SEE MAP-862
        if (concept.isActive() == true) {

          workflowService.processWorkflowAction(mapProject, concept, mapUser,
              null, WorkflowAction.ASSIGN_FROM_SCRATCH);
        } else {
          Logger.getLogger(WorkflowServiceJpa.class).warn(
              "Skipping inactive concept " + concept.getTerminologyId());
        }

      }

      mappingService.close();
      workflowService.close();
      contentService.close();

    } catch (Exception e) {
      handleException(e, "trying to assign a batch", userName, project,
          terminologyIds.toString());
    }
  }

  /**
   * Unassign user from a concept.
   * 
   * @param mapProjectId the map project id
   * @param terminologyId the terminology id
   * @param userName the user name
   * @param authToken
   * @return the map record
   */
  @POST
  @Path("/unassign/project/id/{id}/concept/id/{terminologyId}/user/id/{userName}")
  @ApiOperation(value = "Unassign user from a concept.", notes = "Unassigns specified user from the specified concept for the specified project.")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Response unassignConcept(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Concept id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /unassign/project/id/"
            + mapProjectId.toString() + "/concept/id/" + terminologyId
            + "/user/id/" + userName);

    String project = "";

    try {

      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to unassign work.").build());

      WorkflowService workflowService = new WorkflowServiceJpa();
      MappingService mappingService = new MappingServiceJpa();
      ContentService contentService = new ContentServiceJpa();

      MapProject mapProject =
          mappingService.getMapProject(new Long(mapProjectId));
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);
      Concept concept =
          contentService.getConcept(terminologyId,
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      workflowService.processWorkflowAction(mapProject, concept, mapUser, null,
          WorkflowAction.UNASSIGN);

      mappingService.close();
      workflowService.close();
      contentService.close();

      return null;
    } catch (Exception e) {
      handleException(e, "trying to unassign work", userName, project,
          terminologyId);
      return null;
    }
  }

  /**
   * Unassign user from a specified batch of currently assigned work.
   * 
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param terminologyIds the terminology ids
   * @param authToken the auth token
   */
  @POST
  @Path("/unassign/project/id/{id}/user/id/{userName}/batch")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Unassign user from a batch of concepts.", notes = "Unassigns specified user from the specified list of concept ids for the specified project.")
  public void unassignWorkBatch(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "List of terminology ids to be assigned, in JSON or XML POST data", required = true) List<String> terminologyIds,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /unassign/project/id/"
            + mapProjectId.toString() + "/user/id/" + userName + "/all");

    String project = "";

    try {

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to unassign work batch.")
            .build());

      WorkflowService workflowService = new WorkflowServiceJpa();
      MappingService mappingService = new MappingServiceJpa();
      ContentService contentService = new ContentServiceJpa();

      // get the project and user
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      project = mapProject.getName();
      MapUser mapUser = mappingService.getMapUser(userName);

      for (String terminologyId : terminologyIds) {

        Concept concept =
            contentService.getConcept(terminologyId,
                mapProject.getSourceTerminology(),
                mapProject.getSourceTerminologyVersion());

        Logger.getLogger(WorkflowServiceRest.class).info(
            "  Unassigning " + mapUser.getUserName() + " from concept "
                + terminologyId);
        workflowService.processWorkflowAction(mapProject, concept, mapUser,
            null, WorkflowAction.UNASSIGN);
      }

      mappingService.close();
      workflowService.close();
      contentService.close();
    } catch (Exception e) {
      handleException(e, "trying to unassign work batch", userName, project,
          terminologyIds.toString());
    }
  }

  /**
   * Attempt to validate and finish work on a record.
   * 
   * @param mapRecord the completed map record
   * @param authToken
   */
  @POST
  @Path("/finish")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Finish work on a map record.", notes = "Finished work on the specified map record if it passes validation, then moves it forward in the worfklow.")
  public void finishWork(
    @ApiParam(value = "Completed map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /finish" + " for map record with id = "
            + mapRecord.getId().toString());

    String user = "";
    String project = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to set a record to finished.")
            .build());

      MappingService mappingService = new MappingServiceJpa();

      // get the map project and map user
      MapProject mapProject =
          mappingService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      MapUser mapUser = mapRecord.getOwner();
      user = mapUser.getUserName();
      mappingService.close();

      // get the concept
      ContentService contentService = new ContentServiceJpa();
      Concept concept =
          contentService.getConcept(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());
      contentService.close();

      // execute the workflow call
      WorkflowService workflowService = new WorkflowServiceJpa();
      workflowService.processWorkflowAction(mapProject, concept, mapUser,
          mapRecord, WorkflowAction.FINISH_EDITING);
      workflowService.close();

    } catch (Exception e) {
      handleException(e, "trying to finish work", user, project, mapRecord
          .getId().toString());
    }

  }

  /**
   * Attempt to publish a previously resolved record This action is only
   * available to map leads, and only for resolved conflict or review work
   * 
   * @param mapRecord the completed map record
   * @param authToken
   */
  @POST
  @Path("/publish")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Mark a map record for publication.", notes = "Moves a previously resolved conflict or review record owned by a lead out of the workflow and into publication-ready status")
  public void publishWork(
    @ApiParam(value = "Completed map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /publish" + " for map record with id = "
            + mapRecord.getId().toString());

    String user = "";
    String project = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to set a record to published.")
            .build());

      MappingService mappingService = new MappingServiceJpa();

      // get the map project and map user
      MapProject mapProject =
          mappingService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      MapUser mapUser = mapRecord.getOwner();
      user = mapUser.getUserName();
      mappingService.close();

      // get the concept
      ContentService contentService = new ContentServiceJpa();
      Concept concept =
          contentService.getConcept(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());
      contentService.close();

      // execute the workflow call
      WorkflowService workflowService = new WorkflowServiceJpa();
      workflowService.processWorkflowAction(mapProject, concept, mapUser,
          mapRecord, WorkflowAction.PUBLISH);
      workflowService.close();

    } catch (Exception e) {
      handleException(e, "trying to publish work", user, project, mapRecord
          .getId().toString());
    }

  }

  /**
   * Save map record without validation checks or workflow action.
   * 
   * @param mapRecord the map record
   * @param authToken
   */
  @POST
  @Path("/save")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Save work on a map record.", notes = "Updates the map record and sets workflow accordingly.")
  public void saveWork(
    @ApiParam(value = "Map record to save, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /save" + " for map record with id = "
            + mapRecord.getId().toString());

    String user = "";
    String project = "";

    try {

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to save a map record.")
            .build());

      securityService.getMapProjectRoleForToken(authToken,
          mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to save a map record.")
            .build());

      // get the map project and map user
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject =
          mappingService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      MapUser mapUser = mapRecord.getOwner();
      user = mapUser.getUserName();
      mappingService.close();

      // get the concept
      ContentService contentService = new ContentServiceJpa();
      Concept concept =
          contentService.getConcept(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());
      contentService.close();

      // process the workflow action
      WorkflowService workflowService = new WorkflowServiceJpa();
      workflowService.processWorkflowAction(mapProject, concept, mapUser,
          mapRecord, WorkflowAction.SAVE_FOR_LATER);
      workflowService.close();

    } catch (Exception e) {
      handleException(e, "trying to save work", user, project, mapRecord
          .getId().toString());
    }

  }

  /**
   * Cancel work for map record.
   * 
   * @param mapRecord the map record
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/cancel")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Cancel editing of a map record.", notes = "Cancels editing of a map record.  Performs necessary workflow actions for current workflow path and status.")
  public void cancelWorkForMapRecord(
    @ApiParam(value = "Map record to cancel work for , in JSON or XML POST data") MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /cancel for map record with id = "
            + mapRecord.getId());

    String userName = "";
    String project = "";

    try {
      // authorize call
      userName = securityService.getUsernameForToken(authToken);
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to cancel editing a map record.")
                .build());

      // open the services
      ContentService contentService = new ContentServiceJpa();
      MappingService mappingService = new MappingServiceJpa();
      WorkflowService workflowService = new WorkflowServiceJpa();

      // get the map project and concept
      MapProject mapProject =
          mappingService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      Concept concept =
          contentService.getConcept(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      // process the workflow action
      workflowService.processWorkflowAction(mapProject, concept,
          mapRecord.getOwner(), mapRecord, WorkflowAction.CANCEL);
    } catch (Exception e) {
      handleException(e, "trying to cancel editing a map record", userName,
          project, mapRecord.getId().toString());
    }

  }

  /**
   * Creates the qa record.
   * 
   * @param mapRecord the map record
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/createQARecord")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Creates a qa record.", notes = "Creates a qa record given a map record.")
  public void createQARecord(
    @ApiParam(value = "Map record to create qa record for , in JSON or XML POST data") MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /createQARecord for map record with id = "
            + mapRecord.getId());

    String userName = "";
    String project = "";

    try {
      // authorize call
      userName = securityService.getUsernameForToken(authToken);
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to create a qa record from a map record.")
                .build());

      // open the services
      ContentService contentService = new ContentServiceJpa();
      MappingService mappingService = new MappingServiceJpa();
      WorkflowService workflowService = new WorkflowServiceJpa();

      // get the map project and concept
      MapProject mapProject =
          mappingService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      Concept concept =
          contentService.getConcept(mapRecord.getConceptId(),
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());

      // find the qa user
      MapUser mapUser = null;
      for (MapUser user : mappingService.getMapUsers().getMapUsers()) {
        if (user.getUserName().equals("qa"))
          mapUser = user;
      }

      // process the workflow action
      workflowService.processWorkflowAction(mapProject, concept, mapUser,
          mapRecord, WorkflowAction.CREATE_QA_RECORD);

      workflowService.close();
    } catch (Exception e) {
      handleException(e, "trying to create a qa map record", userName, project,
          mapRecord.getId().toString());
    }

  }

  /**
   * Creates the qa work given a report of concepts.
   * 
   * @param reportId the report id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/createQAWork")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Creates qa work.", notes = "Creates qa work given a report of concepts.")
  public void createQAWork(
    @ApiParam(value = "Report of concepts to create qa records for , in JSON or XML POST data") Long reportId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /createQAWork for report with id = "
            + reportId);

    String userName = "";
    String project = "";
    Report report = null;

    try {
      // authorize call
      userName = securityService.getUsernameForToken(authToken);

      // get report and projectId
      ReportService reportService = new ReportServiceJpa();
      report = reportService.getReport(reportId);

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              report.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to create qa work from a report of concepts.")
                .build());

      WorkflowService workflowService = new WorkflowServiceJpa();
      workflowService.createQAWork(report);
      workflowService.close();

      reportService.close();

    } catch (Exception e) {
      handleException(e, "trying to create qa work", userName, project,
          report == null ? "" : report.getId().toString());
    }

  }

  /**
   * Gets the assigned map record from the existing workflow for concept and map
   * user, if it exists
   * 
   * @param mapProjectId the map project id
   * @param terminologyId the terminology id
   * @param userName the user name
   * @param authToken
   * @return the assigned map record for concept and map user
   * @throws Exception the exception
   */
  @GET
  @Path("/record/project/id/{id}/concept/id/{terminologyId}/user/id/{userName}")
  @ApiOperation(value = "Get a map record for concept and user", notes = "Gets a map record for the specified project, concept id, and user info.")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecord getAssignedMapRecordForConceptAndMapUser(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Concept id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /assignedRecord/projectId/" + mapProjectId
            + "/concept/" + terminologyId + "/user/" + userName);

    String user = "";
    String project = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve an assigned map record given concept and user.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapUser mapUser = mappingService.getMapUser(userName);
      user = mapUser.getUserName();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      project = mapProject.getName();
      mappingService.close();

      ContentService contentService = new ContentServiceJpa();
      Concept concept =
          contentService.getConcept(terminologyId,
              mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());
      contentService.close();

      WorkflowService workflowService = new WorkflowServiceJpa();
      TrackingRecord trackingRecord =
          workflowService.getTrackingRecord(mapProject, concept);

      for (MapRecord mr : workflowService
          .getMapRecordsForTrackingRecord(trackingRecord)) {
        if (mr.getOwner().equals(mapUser)) {
          workflowService.close();
          return mr;
        }
      }

      workflowService.close();

      return null;
    } catch (Exception e) {
      handleException(e, "trying to retrieve an assigned map record", user,
          project, terminologyId);
      return null;
    }

  }

  /**
   * Is map record false conflict.
   * 
   * @param recordId the record id
   * @param authToken the auth token
   * @return the boolean
   * @throws Exception the exception
   */
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/isFalseConflict")
  @ApiOperation(value = "Indicate whether a map record is a false conflict", notes = "Indicates whether the specified map record id is a false conflict.", response = Boolean.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Boolean isMapRecordFalseConflict(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long recordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /record/id/" + recordId + "/sFalseConflict");

    MappingService mappingService = new MappingServiceJpa();
    WorkflowService workflowService = new WorkflowServiceJpa();

    MapRecord mapRecord = mappingService.getMapRecord(recordId);
    MapProject mapProject =
        mappingService.getMapProject(mapRecord.getMapProjectId());

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, new Long(
              mapProject.getId()));
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to check record for false conflict")
                .build());

      // if not a conflict resolution record, return null
      if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
          && !mapRecord.getWorkflowStatus().equals(
              WorkflowStatus.CONFLICT_DETECTED))
        return false;

      WorkflowException workflowException =
          workflowService.getWorkflowException(mapProject,
              mapRecord.getConceptId());

      if (workflowException != null) {
        if (workflowException.getFalseConflictMapRecordIds().contains(recordId))
          return true;
        else
          return false;
      }
    } catch (Exception e) {
      handleException(e, "trying to retrieve flag for false conflict");
    }

    // return default false
    return false;
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Feedback
  // ///////////////////////////////////////////////////

  /**
   * Adds the feedback conversation.
   * 
   * @param conversation the conversation
   * @param authToken the auth token
   * @return the map user
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/conversation/add")
  @ApiOperation(value = "Add a feedback conversation.", notes = "Adds the specified feedback conversation.", response = FeedbackConversationJpa.class)
  public FeedbackConversation addFeedbackConversation(
    @ApiParam(value = "Feedback conversation, in JSON or XML POST data", required = true) FeedbackConversationJpa conversation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /conversation/add");

    String userName = "";

    try {
      // authorize call
      userName = securityService.getUsernameForToken(authToken);
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to add a feedback conversation.")
                .build());

      WorkflowService workflowService = new WorkflowServiceJpa();

      Logger.getLogger(WorkflowServiceRest.class).info(
          "RESTful call (Workflow): /conversation/update feedback msg: "
              + conversation.getFeedbacks().get(0));

      workflowService.addFeedbackConversation(conversation);
      workflowService.close();

      return conversation;

    } catch (Exception e) {
      handleException(e, "add a feedback conversation", userName, "",
          conversation.getMapRecordId().toString());
      return null;
    }
  }

  /**
   * Sets the map record false conflict.
   * 
   * @param recordId the record id
   * @param isFalseConflict the is false conflict
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/record/id/{id:[0-9][0-9]*}/falseConflict/{isFalseConflict}")
  @ApiOperation(value = "Sets whether record is false conflict.", notes = "Sets a flag indicating a false conflict for the specified parameters.", response = Response.class)
  public void setMapRecordFalseConflict(
    @ApiParam(value = "Map record id, e.g. 7", required = true) @PathParam("id") Long recordId,
    @ApiParam(value = "Whether is false conflict, e.g. true", required = true) @PathParam("isFalseConflict") boolean isFalseConflict,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /record/id/" + recordId
            + "/setFalseConflict/" + isFalseConflict);

    MappingService mappingService = new MappingServiceJpa();
    WorkflowService workflowService = new WorkflowServiceJpa();

    MapRecord mapRecord = mappingService.getMapRecord(recordId);
    MapProject mapProject =
        mappingService.getMapProject(mapRecord.getMapProjectId());

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, new Long(
              mapProject.getId()));
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to check record for false conflict")
                .build());

      // if not a conflict resolution record, throw an error
      if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
          && !mapRecord.getWorkflowStatus().equals(
              WorkflowStatus.CONFLICT_IN_PROGRESS)
          && !mapRecord.getWorkflowStatus().equals(
              WorkflowStatus.CONFLICT_RESOLVED))
        throw new WebApplicationException(Response.status(401)
            .entity("Cannot set false conflict flag on a non-conflict record")
            .build());

      WorkflowException workflowException =
          workflowService.getWorkflowException(mapProject,
              mapRecord.getConceptId());

      // if no workflow exception for this concept, add it
      if (workflowException == null) {
        workflowException = new WorkflowExceptionJpa();
        workflowException.setMapProjectId(mapProject.getId());
        workflowException.setTerminology(mapProject.getSourceTerminology());
        workflowException.setTerminologyVersion(mapProject
            .getSourceTerminologyVersion());
        workflowException.setTerminologyId(mapRecord.getConceptId());
      }

      Set<Long> recordIds = new HashSet<>();

      // if setting to true, add the record ids
      if (isFalseConflict == true) {
        // add this record
        recordIds.add(recordId);

        // add the specialist records for this conflict
        for (MapRecord mr : mappingService.getOriginMapRecordsForConflict(
            recordId).getIterable()) {
          recordIds.add(mr.getId());
        }

      }

      workflowException.setFalseConflictMapRecordIds(recordIds);

      // if empty, remove, if new, add, if not, update
      if (workflowException.isEmpty()) {

        // if id is set, remove the record
        if (workflowException.getId() != null)
          workflowService.removeWorkflowException(workflowException.getId());
      } else if (workflowException.getId() != null) {
        workflowService.updateWorkflowException(workflowException);
      } else {
        workflowService.addWorkflowException(workflowException);
      }

    } catch (Exception e) {
      handleException(e, "trying to set flag for false conflict");
    }
  }

  /**
   * Updates a feedback conversation
   * 
   * @param feedbackConversation the feedback conversation to be updated
   * @param authToken
   */
  @POST
  @Path("/conversation/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a feedback conversation.", notes = "Updates specified feedback conversation.", response = Response.class)
  public void updateFeedbackConversation(
    @ApiParam(value = "Feedback conversation, in JSON or XML POST data", required = true) FeedbackConversationJpa feedbackConversation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /conversation/update");

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to update the feedback conversation.")
                .build());

      WorkflowService workflowService = new WorkflowServiceJpa();
      workflowService.updateFeedbackConversation(feedbackConversation);

      // add debug
      int ct = feedbackConversation.getFeedbacks().size();
      Logger.getLogger(WorkflowServiceRest.class).info(
          "RESTful call (Workflow): /conversation/update feedback msg: "
              + feedbackConversation.getFeedbacks().get(ct - 1));

      workflowService.close();
    } catch (Exception e) {
      handleException(e, "update the feedback conversation");
    }
  }

  /**
   * Returns the feedback conversation for a given id (auto-generated) in JSON
   * format
   * 
   * @param mapRecordId the mapRecordId
   * @param authToken
   * @return the feedbackConversation
   */
  @GET
  @Path("/conversation/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get feedback conversation by map record id", notes = "Gets a feedback conversation for the specified map record id.", response = FeedbackConversation.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public FeedbackConversation getFeedbackConversation(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /conversation/id/" + mapRecordId.toString());

    try {

      WorkflowService workflowService = new WorkflowServiceJpa();
      FeedbackConversation feedbackConversation =
          workflowService.getFeedbackConversation(new Long(mapRecordId));
      workflowService.close();

      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the feedback conversation.")
                .build());

      return feedbackConversation;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the feedback conversation");
      return null;
    }
  }

  /**
   * Returns the feedback conversations for map project.
   * 
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the feedback conversations for map project
   */
  @POST
  @Path("/conversation/project/id/{id:[0-9][0-9]*}/{userName}/query/{query}")
  @ApiOperation(value = "Get feedback conversations by map project", notes = "Gets a list of feedback conversations for the specified map project and user.", response = FeedbackConversationListJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @CookieParam(value = "userInfo")
  public FeedbackConversationList findFeedbackConversationsForMapProjectAndUser(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'", required = true) @PathParam("query") String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /conversation/project/id/"
            + mapProjectId.toString() + " userName: " + userName + " query: "
            + query + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String user = "";
    // execute the service call
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the feedback conversations for a map project.")
                .build());

      WorkflowService workflowService = new WorkflowServiceJpa();
      FeedbackConversationList feedbackConversationList =
          workflowService.findFeedbackConversationsForProject(mapProjectId,
              userName, query, pfsParameter);
      workflowService.close();
      return feedbackConversationList;
    } catch (Exception e) {
      handleException(e,
          "trying to retrieve the feedback conversations for a map project",
          user, mapProjectId.toString(), "");
      return null;
    }

  }

  /**
   * Returns the feedback conversations for terminology id.
   * 
   * @param mapProjectId the map project id
   * @param conceptId the concept id
   * @param authToken the auth token
   * @return the feedback conversations for terminology id
   */
  @GET
  @Path("/conversation/project/id/{id:[0-9][0-9]*}/concept/id/{terminologyId}")
  @ApiOperation(value = "Get feedback conversations by concept id.", notes = "Gets a list of feedback conversations for the specified concept and project.", response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public FeedbackConversationListJpa getFeedbackConversationsForTerminologyId(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Concept id, e.g. 22298006", required = true) @PathParam("terminologyId") String conceptId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/concept/id/" + conceptId);

    String user = "";
    try {
      // authorize call
      MapUserRole applicationRole =
          securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);

      if (!applicationRole.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to find feedback conversations by the given concept id.")
                .build());

      WorkflowService workflowService = new WorkflowServiceJpa();
      FeedbackConversationListJpa feedbackConversationList =
          (FeedbackConversationListJpa) workflowService
              .getFeedbackConversationsForConcept(mapProjectId, conceptId);

      workflowService.close();
      return feedbackConversationList;
    } catch (Exception e) {
      handleException(e,
          "trying to find feedback conversations by the given concept id",
          user, "", conceptId);
      return null;
    }
  }

  /**
   * Assign batch to fix error path.
   *
   * @param mapProjectId the map project id
   * @param terminologyIds the terminology ids
   * @param userName the user name
   * @param authToken the auth token
   * @return the validation result
   */
  @POST
  @Path("/assign/fixErrorPath/project/id/{projectId}/user/id/{userName}")
  @ApiOperation(value = "Assign concepts to fix error path", notes = "Assigns publication-ready map records to the Fix Error Workflow Path given a list of concept ids", response = ValidationResult.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @CookieParam(value = "userInfo")
  public ValidationResult assignBatchToFixErrorPath(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "List of terminology ids to assign", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map user to assign to", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /assign/project/id/" + mapProjectId
            + "/fixErrorPath with ids: " + terminologyIds.toString());

    String user = "";
    // execute the service call
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to assign map records to the fix error path.")
                .build());

      MappingService mappingService = new MappingServiceJpa();

      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      MapUser mapUser = mappingService.getMapUser(userName);

      if (mapUser == null)
        throw new LocalException("The user could not be retrieved");

      ValidationResult result = new ValidationResultJpa();

      List<MapRecord> mapRecords = new ArrayList<>();

      // cycle over these ids
      for (String terminologyId : terminologyIds) {

        MapRecordList mrList =
            mappingService.getMapRecordsForProjectAndConcept(mapProjectId,
                terminologyId);

        // first check: records retrieved
        if (mrList.getCount() == 0) {
          result.addError("No records found for concept " + terminologyId);
          continue;
        }

        // first check: only one record
        if (mrList.getCount() != 1) {
          result.addError("Multiple records present for concept "
              + terminologyId);
          continue;
        }

        // get the first record
        MapRecord mapRecord = mrList.getIterable().iterator().next();

        // second check: PUBLISHED or READY_FOR_PUBLICATION
        if (!mapRecord.getWorkflowStatus().equals(
            WorkflowStatus.READY_FOR_PUBLICATION)
            && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {
          result.addError("Record is not publication-ready for concept "
              + terminologyId + ", " + mapRecord.getConceptName());
          continue;
        }

        mapRecords.add(mapRecord);
      }

      // close the mapping service
      mappingService.close();

      // open workflow and content services
      WorkflowService workflowService = new WorkflowServiceJpa();
      ContentService contentService = new ContentServiceJpa();

      // cycle over all eligible map records
      for (MapRecord mapRecord : mapRecords) {

        // get concept for this terminology id
        Concept concept =
            contentService.getConcept(mapRecord.getConceptId(),
                mapProject.getSourceTerminology(),
                mapProject.getSourceTerminologyVersion());

        if (concept == null) {
          result.addError("Could not retrieve concept for id "
              + mapRecord.getConceptId());
          continue;
        }

        try {

          // assign the user to this concept and record
          workflowService.processWorkflowAction(mapProject, concept, mapUser,
              mapRecord, WorkflowAction.ASSIGN_FROM_INITIAL_RECORD);

          // add success message if no errors thrown
          result.addMessage("Successfully assigned concept "
              + mapRecord.getConceptId() + ", "
              + concept.getDefaultPreferredName());
        } catch (LocalException e) {
          result
              .addError("Concept already in workflow, could not assign concept "
                  + mapRecord.getConceptId()
                  + ", "
                  + concept.getDefaultPreferredName());
        }
      }

      return result;

    } catch (Exception e) {
      handleException(e, "trying to assign concepts along the fix error path.",
          user, mapProjectId.toString(), "");
      return null;
    }

  }

}
