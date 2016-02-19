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
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowFixErrorPathHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowQaPathHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
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
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.WorkflowException;
import org.ihtsdo.otf.mapping.workflow.WorkflowExceptionJpa;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

// TODO Reminder to Patrick to clean up logging after finish (e.g. .info -> .debug)

/**
 * REST implementation for workflow service.
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
   * 
   * @throws Exception
   */
  public WorkflowServiceRest() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /**
   * Compute workflow.
   *
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/compute")
  @ApiOperation(value = "Compute workflow for a map project.", notes = "Recomputes workflow for the specified map project.")
  public void computeWorkflow(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/compute");

    String user = null;
    String project = "";

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken,
          MapUserRole.ADMINISTRATOR, "compute workflow", securityService);

      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      workflowService.computeWorkflow(mapProject);
      return;
    } catch (Exception e) {
      handleException(e, "trying to compute workflow", user, project, "");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /**
   * Finds available concepts for the specified map project and user.
   *
   * @param mapProjectId the map project id
   * @param userName the username
   * @param query the query
   * @param pfsParameter the paging parameter
   * @param authToken the auth token
   * @return the search result list
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id/" + userName + "/availableConcepts");

    String project = "";
    String user = null;

    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "find available concepts", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();

      // get the workflow tracking records
      SearchResultList results = workflowService.findAvailableWork(mapProject,
          mapUser, MapUserRole.SPECIALIST, query, pfsParameter);

      // Check that all concepts referenced here are active (and thus have
      // a tree position)
      // This is a temporary fix, which will be removed once a more robust
      // solution to scope concepts is found
      // SEE MAP-862
      final SearchResultList revisedResults = new SearchResultListJpa();
      for (final SearchResult result : results.getIterable()) {

        final Concept c = contentService.getConcept(result.getTerminologyId(),
            mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion());
        if (c == null) {
          Logger.getLogger(WorkflowServiceJpa.class)
              .warn("Could not get concept " + result.getTerminologyId() + ", "
                  + mapProject.getSourceTerminology() + ", "
                  + mapProject.getSourceTerminologyVersion());
        } else if (c.isActive()) {
          revisedResults.addSearchResult(result);
        } else {
          Logger.getLogger(WorkflowServiceJpa.class)
              .warn("Skipping inactive concept " + result.getTerminologyId());
        }
      }

      revisedResults.setTotalCount(results.getTotalCount());
      results = revisedResults;

      return results;
    } catch (Exception e) {
      handleException(e, "trying to find available work", user, project, "");
      return null;
    } finally {
      contentService.close();
      workflowService.close();
      securityService.close();
    }
  }

  /**
   * Finds assigned concepts for the specified map project and user.
   *
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query the query
   * @param pfsParameter the paging parameter
   * @param authToken the auth token
   * @return the search result list
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id/" + userName + "/assignedConcepts");

    String project = "";
    String user = null;

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "find assigned concepts", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();

      // create PfsParameter from query restriction passed
      PfsParameter localPfs = new PfsParameterJpa();
      localPfs.setQueryRestriction(pfsParameter.getQueryRestriction());
      localPfs.setSortField(pfsParameter.getSortField());

      // get ALL FixErrorPath work at specialist level for the project
      WorkflowPathHandler fixErrorHandler = new WorkflowFixErrorPathHandler();
      SearchResultList fixErrorWork =
          fixErrorHandler.findAssignedWork(mapProject, mapUser,
              MapUserRole.SPECIALIST, query, localPfs, workflowService);

      // get ALL assigned work at specialist level for project
      SearchResultList assignedWork = workflowService.findAssignedWork(
          mapProject, mapUser, MapUserRole.SPECIALIST, query, localPfs);

      // concatenate the results
      assignedWork.addSearchResults(fixErrorWork);

      // apply paging
      int[] totalCt = new int[1];
      localPfs = new PfsParameterJpa(pfsParameter);
      localPfs.setQueryRestriction("");
      localPfs.setSortField("");

      // create list of SearchResultJpas
      // NOTE: This could be cleaned up with better typing
      // currently cannot convert List<SearchResultJpa> to
      // List<SearchResult>
      List<SearchResultJpa> results = new ArrayList<>();
      for (SearchResult sr : assignedWork.getSearchResults()) {
        results.add((SearchResultJpa) sr);
      }

      // apply paging to the list
      results = workflowService.applyPfsToList(results, SearchResultJpa.class,
          totalCt, localPfs);

      // reconstruct the assignedWork search result list
      assignedWork.setSearchResults(new ArrayList<SearchResult>());
      for (SearchResult sr : results) {
        assignedWork.addSearchResult(sr);
      }

      return assignedWork;

    } catch (Exception e) {
      handleException(e, "trying to find assigned concepts", user, project, "");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /**
   * Finds available conflicts for the specified map project and user.
   *
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query the query
   * @param pfsParameter the paging parameter
   * @param authToken the auth token
   * @return the search result list
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id" + userName + "/availableConflicts");

    String project = "";
    String user = null;

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "find available conflicts", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();

      // get the workflow tracking records
      return workflowService.findAvailableWork(mapProject, mapUser,
          MapUserRole.LEAD, query, pfsParameter);
    } catch (Exception e) {
      handleException(e, "trying to find available conflicts", user, project,
          "");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /**
   * Finds assigned conflicts for the specified map project and user.
   *
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query the query
   * @param pfsParameter the paging parameter
   * @param authToken the auth token
   * @return the search result list
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id/" + userName + "/assignedConflicts");
    String project = "";
    String user = null;

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "find assigned conflicts", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();

      // get the map records
      return workflowService.findAssignedWork(mapProject, mapUser,
          MapUserRole.LEAD, query, pfsParameter);

    } catch (Exception e) {
      handleException(e, "trying to find assigned conflicts", user, project,
          "");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /**
   * Finds available review work for the specified map project and user.
   *
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query the query
   * @param pfsParameter the paging parameter
   * @param authToken the auth token
   * @return the search result list
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id" + userName + "/availableReviewWork");

    String project = "";
    String user = null;

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "find available review work ", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();

      PfsParameter localPfs = new PfsParameterJpa();
      localPfs.setQueryRestriction(pfsParameter.getQueryRestriction());
      localPfs.setSortField(pfsParameter.getSortField());

      SearchResultList availableWork = new SearchResultListJpa();

      // get ALL FixErrorPath work at specialist level for the project
      WorkflowPathHandler fixErrorHandler = new WorkflowFixErrorPathHandler();
      SearchResultList fixErrorWork =
          fixErrorHandler.findAvailableWork(mapProject, mapUser,
              MapUserRole.LEAD, query, localPfs, workflowService);

      // if review project path, get normal work
      if (mapProject.getWorkflowType().equals(WorkflowType.REVIEW_PROJECT)) {

        // get ALL normal workflow work at lead level
        availableWork = workflowService.findAvailableWork(mapProject, mapUser,
            MapUserRole.LEAD, query, pfsParameter);
      }

      // combine the results
      availableWork.addSearchResults(fixErrorWork);

      // apply paging
      int[] totalCt = new int[1];
      localPfs = new PfsParameterJpa(pfsParameter);
      localPfs.setQueryRestriction(null);
      localPfs.setSortField(null);

      // create list of SearchResultJpas
      // NOTE: This could be cleaned up with better typing
      // currently cannot convert List<SearchResultJpa> to
      // List<SearchResult>
      List<SearchResultJpa> results = new ArrayList<>();
      for (SearchResult sr : availableWork.getSearchResults()) {
        results.add((SearchResultJpa) sr);
      }

      // apply paging to the list
      results = workflowService.applyPfsToList(results, SearchResultJpa.class,
          totalCt, localPfs);

      // reconstruct the assignedWork search result list
      availableWork.setSearchResults(new ArrayList<SearchResult>());
      for (SearchResult sr : results) {
        availableWork.addSearchResult(sr);
      }

      return availableWork;
    } catch (Exception e) {
      handleException(e, "trying to find available review work", user, project,
          "");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
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
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/availableQAWork");

    String project = "";
    String user = "qa"; // always the qa user for specialist level work

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
     authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "find available qa work ", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(user);
      user = mapUser.getUserName();

      WorkflowPathHandler handler = new WorkflowQaPathHandler();
      SearchResultList results = handler.findAvailableWork(mapProject, mapUser,
          MapUserRole.SPECIALIST, query, pfsParameter, workflowService);

      return results;

    } catch (Exception e) {
      e.printStackTrace();
      handleException(e, "trying to find available qa work", user, project, "");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /**
   * Finds assigned review work for the specified map project and user.
   *
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param query the query
   * @param pfsParameter the paging parameter
   * @param authToken the auth token
   * @return the search result list
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id/" + userName + "/assignedReviewWork");

    String project = "";
    String user = null;

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "find assigned review work ", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();

      PfsParameter localPfs = new PfsParameterJpa();
      localPfs.setSortField(pfsParameter.getSortField());
      localPfs.setQueryRestriction(pfsParameter.getQueryRestriction());

      // get ALL FixErrorPath work at specialist level for the project
      WorkflowPathHandler fixErrorHandler = new WorkflowFixErrorPathHandler();
      SearchResultList assignedWork =
          fixErrorHandler.findAssignedWork(mapProject, mapUser,
              MapUserRole.LEAD, query, localPfs, workflowService);

      // if a review project, get all normal workflow work and combine
      if (mapProject.getWorkflowType().equals(WorkflowType.REVIEW_PROJECT)) {
        SearchResultList reviewProjectWork = workflowService.findAssignedWork(
            mapProject, mapUser, MapUserRole.LEAD, query, pfsParameter);
        assignedWork.addSearchResults(reviewProjectWork);
      }
      
      // apply paging
      int[] totalCt = new int[1];
      localPfs = new PfsParameterJpa(pfsParameter);
      localPfs.setQueryRestriction("");
      localPfs.setSortField("");

      // create list of SearchResultJpas
      // NOTE: This could be cleaned up with better typing
      // currently cannot convert List<SearchResultJpa> to
      // List<SearchResult>
      List<SearchResultJpa> results = new ArrayList<>();
      for (SearchResult sr : assignedWork.getSearchResults()) {
        results.add((SearchResultJpa) sr);
      }

      // apply paging to the list
      results = workflowService.applyPfsToList(results, SearchResultJpa.class,
          totalCt, localPfs);

      // reconstruct the assignedWork search result list
      assignedWork.setSearchResults(new ArrayList<SearchResult>());
      for (SearchResult sr : results) {
        assignedWork.addSearchResult(sr);
      }

      return assignedWork;

    } catch (Exception e) {
      handleException(e, "trying to find assigned review work", user, project,
          "");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
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
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/user/id/" + userName + "/query/" + query + "/assignedQAWork");

    String project = "";
    String user = null;

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "find assigned qa work ", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();
      
      // SPECIAL CASE: Access QA Workflow Path directly
      WorkflowPathHandler handler = new WorkflowQaPathHandler();
      SearchResultList results = handler.findAssignedWork(mapProject, mapUser,
          MapUserRole.SPECIALIST, query, pfsParameter, workflowService);
      return results;



    } catch (Exception e) {
      handleException(e, "trying to find assigned qa work", user, project, "");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /**
   * Assign user to to work based on an existing map record.
   *
   * @param userName the user name
   * @param mapRecord the map record (can be null)
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/assignFromRecord/user/id/{userName}")
  @ApiOperation(value = "Assign user to concept", notes = "Assigns a user (specialist or lead) to a previously mapped concept.")
  public void assignConceptFromMapRecord(
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Initial map record to copy, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /assignFromRecord/user/id/" + userName
            + " with map record id " + mapRecord.getId() + " for project "
            + mapRecord.getMapProjectId() + " and concept id "
            + mapRecord.getConceptId());

    String project = "";

    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "assign concept from record",
          securityService);

      final MapProject mapProject =
          workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      final Concept concept = contentService.getConcept(
          mapRecord.getConceptId(), mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      workflowService.processWorkflowAction(mapProject, concept, mapUser,
          mapRecord, WorkflowAction.ASSIGN_FROM_INITIAL_RECORD);

    } catch (Exception e) {
      handleException(e, "trying to assign concept from a map record", userName,
          project, mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }
  }

  /**
   * Assigns user to unmapped concept.
   *
   * @param mapProjectId the map project id
   * @param terminologyId the terminology id
   * @param userName the user name
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/assign/project/id/{id}/concept/id/{terminologyId}/user/id/{userName}")
  @ApiOperation(value = "Assign user to concept", notes = "Assigns specified user to map the specified concept for the specified project.")
  public void assignConcept(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Concept id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /assign/project/id/"
            + mapProjectId.toString() + "/concept/id/" + terminologyId
            + "/user/id/" + userName);

    String project = "";

    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "assign concept", securityService);

      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      MapUser mapUser = workflowService.getMapUser(userName);
      Concept concept = contentService.getConcept(terminologyId,
          mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      workflowService.processWorkflowAction(mapProject, concept, mapUser, null,
          WorkflowAction.ASSIGN_FROM_SCRATCH);

    } catch (Exception e) {
      handleException(e, "trying to assign work", userName, project,
          terminologyId);
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }
  }

  /**
   * Assign batch to user.
   *
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param terminologyIds the terminology ids
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/assignBatch/project/id/{id}/user/id/{userName}")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Assign user to batch of concepts.", notes = "Assigns specified user to map the specified list of concept ids for the specified project.")
  public void assignBatch(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "List of terminology ids to be assigned, in JSON or XML POST data", required = true) List<String> terminologyIds,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /assignBatch/project/id/" + mapProjectId
            + "/user/id/" + userName);

    String project = "";

    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "assign Batch", securityService);

      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);

      for (final String terminologyId : terminologyIds) {
        Logger.getLogger(WorkflowServiceRest.class)
            .info("   Assigning " + terminologyId);
        final Concept concept = contentService.getConcept(terminologyId,
            mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion());

        // Only process batch assignment request if the concept is
        // active
        // and thus has a tree position
        // This is a temporary fix for the Unmapped ICD-10 project
        // SEE MAP-862
        if (concept.isActive()) {

          workflowService.processWorkflowAction(mapProject, concept, mapUser,
              null, WorkflowAction.ASSIGN_FROM_SCRATCH);
        } else {
          Logger.getLogger(WorkflowServiceJpa.class)
              .warn("Skipping inactive concept " + concept.getTerminologyId());
        }
      }

    } catch (Exception e) {
      handleException(e, "trying to assign a batch", userName, project,
          terminologyIds.toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }
  }

  /**
   * Unassign user from a concept.
   *
   * @param mapProjectId the map project id
   * @param terminologyId the terminology id
   * @param userName the user name
   * @param authToken the auth token
   * @return the map record
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /unassign/project/id/"
            + mapProjectId.toString() + "/concept/id/" + terminologyId
            + "/user/id/" + userName);

    String project = "";
    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();

    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "unassign concept", securityService);

      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      workflowService.processWorkflowAction(mapProject, concept, mapUser, null,
          WorkflowAction.UNASSIGN);

      return null;
    } catch (Exception e) {
      handleException(e, "trying to unassign work", userName, project,
          terminologyId);
      return null;
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }
  }

  /**
   * Unassign user from a specified batch of currently assigned work.
   *
   * @param mapProjectId the map project id
   * @param userName the user name
   * @param terminologyIds the terminology ids
   * @param authToken the auth token
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /unassign/project/id/"
            + mapProjectId.toString() + "/user/id/" + userName + "/all");

    String project = "";

    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {

      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "unassign Batch", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);

      for (final String terminologyId : terminologyIds) {

        final Concept concept = contentService.getConcept(terminologyId,
            mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion());
        workflowService.processWorkflowAction(mapProject, concept, mapUser,
            null, WorkflowAction.UNASSIGN);
      }

    } catch (Exception e) {
      handleException(e, "trying to unassign work batch", userName, project,
          terminologyIds.toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }
  }

  /**
   * Attempt to validate and finish work on a record.
   * 
   * @param mapRecord the completed map record
   * @param authToken
   * @throws Exception
   */
  @POST
  @Path("/finish")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Finish work on a map record.", notes = "Finished work on the specified map record if it passes validation, then moves it forward in the worfklow.")
  public void finishWork(
    @ApiParam(value = "Completed map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /finish" + " for map record with id = "
            + mapRecord.getId().toString());

    String user = null;
    String project = "";
    final ContentService contentService = new ContentServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "finish work", securityService);

      // get the map project and map user
      final MapProject mapProject =
          workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final MapUser mapUser = mapRecord.getOwner();
      user = mapUser.getUserName();

      // get the concept
      final Concept concept = contentService.getConcept(
          mapRecord.getConceptId(), mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      // execute the workflow call
      workflowService.processWorkflowAction(mapProject, concept, mapUser,
          mapRecord, WorkflowAction.FINISH_EDITING);

    } catch (Exception e) {
      handleException(e, "trying to finish work", user, project,
          mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }

  }

  /**
   * Attempt to publish a previously resolved record This action is only
   * available to map leads, and only for resolved conflict or review work
   * 
   * @param mapRecord the completed map record
   * @param authToken
   * @throws Exception
   */
  @POST
  @Path("/publish")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Mark a map record for publication.", notes = "Moves a previously resolved conflict or review record owned by a lead out of the workflow and into publication-ready status")
  public void publishWork(
    @ApiParam(value = "Completed map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /publish" + " for map record with id = "
            + mapRecord.getId().toString());

    String user = null;
    String project = "";
    final ContentService contentService = new ContentServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "publish work", securityService);

      // get the map project and map user
      final MapProject mapProject =
          workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final MapUser mapUser = mapRecord.getOwner();
      user = mapUser.getUserName();

      // get the concept
      final Concept concept = contentService.getConcept(
          mapRecord.getConceptId(), mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      // execute the workflow call
      workflowService.processWorkflowAction(mapProject, concept, mapUser,
          mapRecord, WorkflowAction.PUBLISH);

    } catch (Exception e) {
      handleException(e, "trying to publish work", user, project,
          mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }

  }

  /**
   * Save map record without validation checks or workflow action.
   *
   * @param mapRecord the map record
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/save")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Save work on a map record.", notes = "Updates the map record and sets workflow accordingly.")
  public void saveWork(
    @ApiParam(value = "Map record to save, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /save" + " for map record with id = "
            + mapRecord.getId().toString());

    String user = null;
    String project = "";
    final ContentService contentService = new ContentServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "save work", securityService);
      final MapUserRole role = securityService
          .getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to save a map record.")
            .build());

      // get the map project and map user
      final MapProject mapProject =
          workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final MapUser mapUser = mapRecord.getOwner();
      user = mapUser.getUserName();

      // get the concept
      final Concept concept = contentService.getConcept(
          mapRecord.getConceptId(), mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      // process the workflow action
      workflowService.processWorkflowAction(mapProject, concept, mapUser,
          mapRecord, WorkflowAction.SAVE_FOR_LATER);

    } catch (Exception e) {
      handleException(e, "trying to save work", user, project,
          mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
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

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /cancel for map record with id = "
            + mapRecord.getId());

    String userName = null;
    String project = "";
    // open the services
    final ContentService contentService = new ContentServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "cancel work", securityService);

      // get the map project and concept
      final MapProject mapProject =
          workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final Concept concept = contentService.getConcept(
          mapRecord.getConceptId(), mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      // process the workflow action
      workflowService.processWorkflowAction(mapProject, concept,
          mapRecord.getOwner(), mapRecord, WorkflowAction.CANCEL);
    } catch (Exception e) {
      handleException(e, "trying to cancel editing a map record", userName,
          project, mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
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

    String userName = null;
    String project = "";
    // open the services
    final ContentService contentService = new ContentServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "create QA record", securityService);

      // get the map project and concept
      final MapProject mapProject =
          workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final Concept concept = contentService.getConcept(
          mapRecord.getConceptId(), mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      // find the qa user
      MapUser mapUser = null;
      for (final MapUser user : workflowService.getMapUsers().getMapUsers()) {
        if (user.getUserName().equals("qa"))
          mapUser = user;
      }

      // process the workflow action
      workflowService.processWorkflowAction(mapProject, concept, mapUser,
          mapRecord, WorkflowAction.CREATE_QA_RECORD);

    } catch (Exception e) {
      handleException(e, "trying to create a qa map record", userName, project,
          mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
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

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /createQAWork for report with id = "
            + reportId);

    String userName = null;
    String project = "";
    Report report = null;

    final ReportService reportService = new ReportServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {

      // get report and projectId
      report = reportService.getReport(reportId);

      // authorize call
      authorizeProject(report.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "create QA work", securityService);

      workflowService.createQAWork(report);
    } catch (Exception e) {
      handleException(e, "trying to create qa work", userName, project,
          report == null ? "" : report.getId().toString());
    } finally {
      workflowService.close();
      reportService.close();
      securityService.clear();
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

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /assignedRecord/projectId/"
            + mapProjectId + "/concept/" + terminologyId + "/user/" + userName);

    String user = null;
    String project = "";
    final ContentService contentService = new ContentServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "get assigned record for concept and userk", securityService);

      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();

      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      final TrackingRecord trackingRecord =
          workflowService.getTrackingRecord(mapProject, concept);

      for (final MapRecord mr : workflowService
          .getMapRecordsForTrackingRecord(trackingRecord)) {
        if (mr.getOwner().equals(mapUser)) {
          return mr;
        }
      }

      return null;
    } catch (Exception e) {
      handleException(e, "trying to get an assigned map record", user, project,
          terminologyId);
      return null;
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
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

    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      final MapRecord mapRecord = workflowService.getMapRecord(recordId);
      final MapProject mapProject =
          workflowService.getMapProject(mapRecord.getMapProjectId());

      // authorize call
      authorizeProject(mapProject.getId(), authToken, MapUserRole.SPECIALIST,
          "is map record false conflict", securityService);

      // if not a conflict resolution record, return null
      if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
          && !mapRecord.getWorkflowStatus()
              .equals(WorkflowStatus.CONFLICT_DETECTED))
        return false;

      final WorkflowException workflowException = workflowService
          .getWorkflowException(mapProject, mapRecord.getConceptId());

      if (workflowException != null) {
        if (workflowException.getFalseConflictMapRecordIds().contains(recordId))
          return true;
        else
          return false;
      }
    } catch (Exception e) {
      handleException(e, "trying to get flag for false conflict");
    } finally {
      workflowService.close();
      securityService.close();
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
   * @throws Exception the exception
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/conversation/add")
  @ApiOperation(value = "Add a feedback conversation.", notes = "Adds the specified feedback conversation.", response = FeedbackConversationJpa.class)
  public FeedbackConversation addFeedbackConversation(
    @ApiParam(value = "Feedback conversation, in JSON or XML POST data", required = true) FeedbackConversationJpa conversation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    // log call
    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /conversation/add");

    String userName = null;
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      userName = authorizeProject(conversation.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "add feedback conversation", securityService);

      Logger.getLogger(WorkflowServiceRest.class)
          .info("RESTful call (Workflow): /conversation/update feedback msg: "
              + conversation.getFeedbacks().get(0));
      workflowService.addFeedbackConversation(conversation);

      return conversation;

    } catch (Exception e) {
      handleException(e, "add a feedback conversation", userName, "",
          conversation.getMapRecordId().toString());
      return null;
    } finally {
      workflowService.close();
      securityService.close();
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

    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /record/id/" + recordId
            + "/setFalseConflict/" + isFalseConflict);

    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      final MapRecord mapRecord = workflowService.getMapRecord(recordId);
      final MapProject mapProject =
          workflowService.getMapProject(mapRecord.getMapProjectId());

      // authorize call
      authorizeProject(mapProject.getId(), authToken, MapUserRole.LEAD,
          "set map record false conflict", securityService);

      // if not a conflict resolution record, throw an error
      if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
          && !mapRecord.getWorkflowStatus()
              .equals(WorkflowStatus.CONFLICT_IN_PROGRESS)
          && !mapRecord.getWorkflowStatus()
              .equals(WorkflowStatus.CONFLICT_RESOLVED))
        throw new WebApplicationException(Response.status(401)
            .entity("Cannot set false conflict flag on a non-conflict record")
            .build());

      WorkflowException workflowException = workflowService
          .getWorkflowException(mapProject, mapRecord.getConceptId());

      // if no workflow exception for this concept, add it
      if (workflowException == null) {
        workflowException = new WorkflowExceptionJpa();
        workflowException.setMapProjectId(mapProject.getId());
        workflowException.setTerminology(mapProject.getSourceTerminology());
        workflowException
            .setTerminologyVersion(mapProject.getSourceTerminologyVersion());
        workflowException.setTerminologyId(mapRecord.getConceptId());
      }

      Set<Long> recordIds = new HashSet<>();

      // if setting to true, add the record ids
      if (isFalseConflict) {
        // find the tracking record for this map record
        TrackingRecord trackingRecord =
            workflowService.getTrackingRecordForMapProjectAndConcept(mapProject,
                mapRecord.getConceptId());

        // instantiate workflow handler for this tracking record
        WorkflowPathHandler handler = workflowService.getWorkflowPathHandler(
            trackingRecord.getWorkflowPath().toString());

        // add the specialist records for this conflict
        for (final MapRecord mr : handler
            .getOriginMapRecordsForMapRecord(mapRecord, workflowService)
            .getIterable()) {
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
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /**
   * Updates a feedback conversation.
   *
   * @param conversation the conversation
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @POST
  @Path("/conversation/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a feedback conversation.", notes = "Updates specified feedback conversation.", response = Response.class)
  public void updateFeedbackConversation(
    @ApiParam(value = "Feedback conversation, in JSON or XML POST data", required = true) FeedbackConversationJpa conversation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    // log call
    Logger.getLogger(WorkflowServiceRest.class)
        .info("RESTful call (Workflow): /conversation/update");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(conversation.getMapProjectId(), authToken,
          MapUserRole.VIEWER, "udpate feedback conversation", securityService);

      workflowService.updateFeedbackConversation(conversation);

      // add debug
      final int ct = conversation.getFeedbacks().size();
      Logger.getLogger(WorkflowServiceRest.class)
          .info("RESTful call (Workflow): /conversation/update feedback msg: "
              + conversation.getFeedbacks().get(ct - 1));

    } catch (Exception e) {
      handleException(e, "update the feedback conversation");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /**
   * Returns the feedback conversation for a given id (auto-generated) in JSON
   * format.
   *
   * @param mapRecordId the mapRecordId
   * @param authToken the auth token
   * @return the feedbackConversation
   * @throws Exception the exception
   */
  @GET
  @Path("/conversation/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get feedback conversation by map record id", notes = "Gets a feedback conversation for the specified map record id.", response = FeedbackConversation.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public FeedbackConversation getFeedbackConversation(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(WorkflowServiceRest.class).info(
        "RESTful call (Workflow): /conversation/id/" + mapRecordId.toString());

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final FeedbackConversation conversation =
          workflowService.getFeedbackConversation(mapRecordId);

      if (conversation != null) {
        // authorize call
        authorizeProject(conversation.getMapProjectId(), authToken,
            MapUserRole.VIEWER, "get feedback conversation", securityService);
      }
      return conversation;
    } catch (Exception e) {
      handleException(e, "trying to get the feedback conversation");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
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
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    // log call
    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /conversation/project/id/"
            + mapProjectId.toString() + " userName: " + userName + " query: "
            + query);

    String user = null;
    final WorkflowService workflowService = new WorkflowServiceJpa();
    // execute the service call
    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "find feedback conversations for project and user", securityService);

      final FeedbackConversationList feedbackConversationList =
          workflowService.findFeedbackConversationsForProject(mapProjectId,
              userName, query, pfsParameter);
      return feedbackConversationList;
    } catch (Exception e) {
      handleException(e,
          "trying to get the feedback conversations for a map project", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /**
   * Returns the feedback conversations for terminology id.
   *
   * @param mapProjectId the map project id
   * @param conceptId the concept id
   * @param authToken the auth token
   * @return the feedback conversations for terminology id
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /record/concept/id/" + conceptId);

    String user = null;
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get feedback conversations for terminology id", securityService);

      return (FeedbackConversationListJpa) workflowService
          .getFeedbackConversationsForConcept(mapProjectId, conceptId);

    } catch (Exception e) {
      handleException(e,
          "trying to find feedback conversations by the given concept id", user,
          "", conceptId);
      return null;
    } finally {
      workflowService.close();
      securityService.close();
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
   * @throws Exception the exception
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
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
      throws Exception {

    // log call
    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /assign/project/id/" + mapProjectId
            + "/fixErrorPath with ids: " + terminologyIds.toString());

    String user = null;
    // open workflow and content services
    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    // execute the service call
    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "assign Batch To Fix Error Path", securityService);

      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      final MapUser mapUser = workflowService.getMapUser(userName);

      if (mapUser == null)
        throw new LocalException("The user could not be found");

      final ValidationResult result = new ValidationResultJpa();

      final List<MapRecord> mapRecords = new ArrayList<>();

      // cycle over these ids
      for (final String terminologyId : terminologyIds) {

        final MapRecordList mrList = workflowService
            .getMapRecordsForProjectAndConcept(mapProjectId, terminologyId);

        // first check: records getd
        if (mrList.getCount() == 0) {
          result.addError("No records found for concept " + terminologyId);
          continue;
        }

        // first check: only one record
        if (mrList.getCount() != 1) {
          result.addError(
              "Multiple records present for concept " + terminologyId);
          continue;
        }

        // get the first record
        final MapRecord mapRecord = mrList.getIterable().iterator().next();

        // second check: PUBLISHED or READY_FOR_PUBLICATION
        if (!mapRecord.getWorkflowStatus()
            .equals(WorkflowStatus.READY_FOR_PUBLICATION)
            && !mapRecord.getWorkflowStatus()
                .equals(WorkflowStatus.PUBLISHED)) {
          result.addError("Record is not publication-ready for concept "
              + terminologyId + ", " + mapRecord.getConceptName());
          continue;
        }

        mapRecords.add(mapRecord);
      }

      // cycle over all eligible map records
      for (final MapRecord mapRecord : mapRecords) {

        // get concept for this terminology id
        final Concept concept = contentService.getConcept(
            mapRecord.getConceptId(), mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion());

        if (concept == null) {
          result.addError(
              "Could not get concept for id " + mapRecord.getConceptId());
          continue;
        }

        try {

          // assign the user to this concept and record
          workflowService.processWorkflowAction(mapProject, concept, mapUser,
              mapRecord, WorkflowAction.ASSIGN_FROM_INITIAL_RECORD);

          // add success message if no errors thrown
          result.addMessage(
              "Successfully assigned concept " + mapRecord.getConceptId() + ", "
                  + concept.getDefaultPreferredName());
        } catch (LocalException e) {
          result
              .addError("Concept already in workflow, could not assign concept "
                  + mapRecord.getConceptId() + ", "
                  + concept.getDefaultPreferredName());
        }
      }

      return result;

    } catch (Exception e) {
      handleException(e, "trying to assign concepts along the fix error path.",
          user, mapProjectId.toString(), "");
      return null;
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }

  }

}
