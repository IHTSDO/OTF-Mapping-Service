/*
 *    Copyright 2017 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.TrackingRecordList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa;
import org.ihtsdo.otf.mapping.jpa.FeedbackJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowFixErrorPathHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.WorkflowQaPathHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.rest.WorkflowServiceRest;
import org.ihtsdo.otf.mapping.model.Feedback;
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
import org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;
import org.ihtsdo.otf.mapping.workflow.WorkflowException;
import org.ihtsdo.otf.mapping.workflow.WorkflowExceptionJpa;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

// TODO: Auto-generated Javadoc
/**
 * REST implementation for workflow service.
 *
 * @author ${author}
 */
@Path("/workflow")
@Api(value = "/workflow", description = "Operations supporting workflow.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class WorkflowServiceRestImpl extends RootServiceRestImpl implements WorkflowServiceRest {

  /** The security service. */
  private final SecurityService securityService;

  /** The Constant lock. */
  private final static String lock = "LOCK";

  /**
   * Instantiates an empty {@link WorkflowServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public WorkflowServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /* see superclass */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/compute")
  @ApiOperation(value = "Compute workflow for a map project.",
      notes = "Recomputes workflow for the specified map project.")
  public void computeWorkflow(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString() + "/compute");

    String user = null;
    String project = "";

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD, "compute workflow",
          securityService);

      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      workflowService.computeWorkflow(mapProject);
      return;
    } catch (final Exception e) {
      handleException(e, "trying to compute workflow", user, project, "");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/availableConcepts")
  @ApiOperation(value = "Find available concepts.",
      notes = "Gets a list of search results for concepts available to be worked on for the specified parameters.",
      response = SearchResultList.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAvailableConcepts(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'",
        required = true) @QueryParam("query") final String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) final PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /project/id/"
        + mapProjectId.toString() + "/user/id/" + userName + "/availableConcepts " + query);

    String project = "";
    String user = null;

    try (final WorkflowService workflowService = new WorkflowServiceJpa();
        final ContentService contentService = new ContentServiceJpa();
        final MappingService mappingService = new MappingServiceJpa();) {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "find available concepts", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();

      // get the workflow tracking records
      SearchResultList results = workflowService.findAvailableWork(mapProject, mapUser,
          MapUserRole.SPECIALIST, query, pfsParameter);

      // Check that all concepts referenced here are active (and thus have
      // a tree position)
      // This is a temporary fix, which will be removed once a more robust
      // solution to scope concepts is found
      // SEE MAP-862
      final SearchResultList revisedResults = new SearchResultListJpa();

      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      final Map<String, String> termNotes = handler.getAllTerminologyNotes();

      for (final SearchResult result : results.getIterable()) {

        final Concept c = contentService.getConcept(result.getTerminologyId(),
            mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());
        if (c == null) {
          Logger.getLogger(WorkflowServiceJpa.class)
              .warn("Could not get concept " + result.getTerminologyId() + ", "
                  + mapProject.getSourceTerminology() + ", "
                  + mapProject.getSourceTerminologyVersion());
        } else if (c.isActive()) {
          if (termNotes != null && termNotes.containsKey(result.getTerminologyId())) {
            result.setTerminologyNote(termNotes.get(result.getTerminologyId()));
          }
          revisedResults.addSearchResult(result);
        } else {
          Logger.getLogger(WorkflowServiceJpa.class)
              .warn("Skipping inactive concept " + result.getTerminologyId());
        }
      }

      revisedResults.setTotalCount(results.getTotalCount());
      results = revisedResults;

      return results;
    } catch (final Exception e) {
      handleException(e, "trying to find available work", user, project, "");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/assignedConcepts")
  @ApiOperation(value = "Find assigned concepts for a map project.",
      notes = "Gets a list of search results of assigned concepts for the specified parameters.",
      response = SearchResultList.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAssignedConcepts(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'",
        required = true) @QueryParam("query") final String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) final PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /project/id/"
        + mapProjectId.toString() + "/user/id/" + userName + "/assignedConcepts " + query);

    String project = "";
    String user = null;

    try (final WorkflowService workflowService = new WorkflowServiceJpa();
        final MappingService mappingService = new MappingServiceJpa();) {

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
      localPfs.setAscending(pfsParameter.isAscending());

      // get ALL FixErrorPath work at specialist level for the project
      final WorkflowPathHandler fixErrorHandler = new WorkflowFixErrorPathHandler();
      final SearchResultList fixErrorWork = fixErrorHandler.findAssignedWork(mapProject, mapUser,
          MapUserRole.SPECIALIST, query, localPfs, workflowService);

      // get ALL assigned work at specialist level for project
      final SearchResultList assignedWork = workflowService.findAssignedWork(mapProject, mapUser,
          MapUserRole.SPECIALIST, query, localPfs);

      // concatenate the results
      assignedWork.addSearchResults(fixErrorWork);

      // apply paging
      final int[] totalCt = new int[1];
      localPfs = new PfsParameterJpa(pfsParameter);
      localPfs.setQueryRestriction("");
      localPfs.setSortField("");

      // create list of SearchResultJpas
      // NOTE: This could be cleaned up with better typing
      // currently cannot convert List<SearchResultJpa> to
      // List<SearchResult>
      List<SearchResultJpa> results = new ArrayList<>();
      for (final SearchResult sr : assignedWork.getSearchResults()) {
        results.add((SearchResultJpa) sr);
      }

      // apply paging to the list
      results = workflowService.applyPfsToList(results, SearchResultJpa.class, totalCt, localPfs);

      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      final Map<String, String> termNotes = handler.getAllTerminologyNotes();

      // reconstruct the assignedWork search result list
      assignedWork.setSearchResults(new ArrayList<SearchResult>());
      for (final SearchResult sr : results) {
        if (termNotes != null && termNotes.containsKey(sr.getTerminologyId())) {
          sr.setTerminologyNote(termNotes.get(sr.getTerminologyId()));
        }
        assignedWork.addSearchResult(sr);
      }

      return assignedWork;

    } catch (final Exception e) {
      handleException(e, "trying to find assigned concepts", user, project, "");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/availableConflicts")
  @ApiOperation(value = "Find available conflicts for a map project.",
      notes = "Gets a list of search results of available conflicts for the specified parameters.",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAvailableConflicts(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'",
        required = true) @QueryParam("query") final String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) final PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /project/id/"
        + mapProjectId.toString() + "/user/id" + userName + "/availableConflicts " + query);

    String project = "";
    String user = null;

    try (final WorkflowService workflowService = new WorkflowServiceJpa();
        final MappingService mappingService = new MappingServiceJpa();) {

      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD, "find available conflicts",
          securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();

      final SearchResultList results = new SearchResultListJpa();

      // if conflict and review path, get only conflict work
      if (mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_AND_REVIEW_PATH)) {

        // Need to make sure not to return maps that are currently in
        // review
        final StringBuilder sb = new StringBuilder();
        if (query != null && !query.isEmpty() && !query.equals("null")) {
          sb.append(query).append(" AND ");
        }

        // Don't include Review or finished conflict records
        sb.append(
            " NOT (userAndWorkflowStatusPairs:REVIEW_* OR userAndWorkflowStatusPairs:CONFLICT_FINISHED_*)");

        // get ALL normal workflow work at lead level
        results.addSearchResults(workflowService.findAvailableWork(mapProject, mapUser,
            MapUserRole.LEAD, sb.toString(), pfsParameter));
      } else {

        // get the workflow tracking records
        results.addSearchResults(workflowService.findAvailableWork(mapProject, mapUser,
            MapUserRole.LEAD, query, pfsParameter));
      }

      if (!results.getSearchResults().isEmpty()) {
        final ProjectSpecificAlgorithmHandler handler =
            mappingService.getProjectSpecificAlgorithmHandler(mapProject);
        final Map<String, String> termNotes = handler.getAllTerminologyNotes();

        for (final SearchResult sr : results.getSearchResults()) {
          if (termNotes != null && termNotes.containsKey(sr.getTerminologyId())) {
            sr.setTerminologyNote(termNotes.get(sr.getTerminologyId()));
          }
        }
      }

      return results;

    } catch (final Exception e) {
      handleException(e, "trying to find available conflicts", user, project, "");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @DELETE
  @Path("/conversation/delete")
  @ApiOperation(value = "Remove a feedback conversation",
      notes = "Removes the specified feedback conversation.",
      response = FeedbackConversationJpa.class)
  public void removeFeedbackConversation(
    @ApiParam(value = "Feedback conversation, in JSON or XML POST data",
        required = true) final FeedbackConversationJpa feedbackConversation,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    // log call
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /conversation/delete for user "
            + feedbackConversation.getFeedbacks());

    String user = null;
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "remove feedback conversation",
          securityService);

      /*
       * for(Feedback feedback:feedbackConversation.getFeedbacks()){
       * workflowService.removeFeedback(feedback.getId()); }
       */
      workflowService.removeFeedbackConversation(feedbackConversation.getId());
    } catch (final Exception e) {
      final LocalException le = new LocalException(
          "Unable to delete feedback conversation. This is likely because the conversation is being used by a map project or map entry");
      handleException(le, "", user, "", "");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#removeFeedback(org.
   * ihtsdo.otf.mapping.jpa.FeedbackJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/feedback/delete")
  @ApiOperation(value = "Remove a feedback", notes = "Removes the specified feedback.",
      response = FeedbackJpa.class)
  public void removeFeedback(
    @ApiParam(value = "Feedback, in JSON or XML POST data", required = true) final FeedbackJpa feedback,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    // log call
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /feedback/delete  " + feedback.getId() + " "
            + feedback.getMessage() + " " + feedback.getIsError() + "  " + feedback.getMapError());

    String user = null;
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "remove feedback", securityService);

      // If there is one error feedback and it is to be removed, change the type
      // of
      // the feedback conversation to normal Feedback.
      if (feedback.getIsError()) {
        final FeedbackConversation conversation =
            workflowService.getFeedback(feedback.getId()).getFeedbackConversation();
        int errorCt = 0;
        for (final Feedback f : conversation.getFeedbacks()) {
          if (f.getIsError()) {
            errorCt++;
          }
        }
        if (errorCt == 1) {
          conversation.setTitle("Feedback");
          workflowService.updateFeedbackConversation(conversation);
        }
      }

      // remove the feedback
      workflowService.removeFeedback(feedback.getId());

    } catch (final Exception e) {
      final LocalException le = new LocalException("Unable to delete feedback");
      handleException(le, "", user, "", "");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @GET
  @Path("/project/id/{id:[0-9][0-9]*}/trackingRecords")
  @ApiOperation(value = "Get tracking records by project id",
      notes = "Gets tracking records for the specified project id.",
      response = TrackingRecordList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TrackingRecordList getTrackingRecordsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info(
        "RESTful call (Workflow): /project/id/" + mapProjectId.toString() + "/trackingRecords");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get feedback conversations for terminology id", securityService);

      final MapProject mapProject = workflowService.getMapProject(mapProjectId);

      final TrackingRecordList trackingRecordList =
          workflowService.getTrackingRecordsForMapProject(mapProject);

      // lazy initialize
      for (final TrackingRecord trackingRecord : trackingRecordList.getTrackingRecords()) {
        trackingRecord.getMapRecordIds().size();
      }

      return trackingRecordList;

    } catch (final Exception e) {
      handleException(e, "trying to get the tracking records");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#findAssignedConflicts(
   * java.lang.Long, java.lang.String, java.lang.String,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/assignedConflicts")
  @ApiOperation(value = "Find assigned conflicts for a map project.",
      notes = "Gets a list of search results of assigned conflicts for the specified parameters.",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAssignedConflicts(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'",
        required = true) @QueryParam("query") final String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) final PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /project/id/"
        + mapProjectId.toString() + "/user/id/" + userName + "/assignedConflicts " + query);
    String project = "";
    String user = null;


    try (final WorkflowService workflowService = new WorkflowServiceJpa();
        final MappingService mappingService = new MappingServiceJpa();) {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD, "find assigned conflicts",
          securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();
      final SearchResultList results = new SearchResultListJpa();

      // if conflict and review path, get only conflict work
      if (mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_AND_REVIEW_PATH)) {

        // Need to make sure not to return maps that are currently in
        // review
        final StringBuilder sb = new StringBuilder();
        if (query != null && !query.isEmpty() && !query.equals("null")) {
          sb.append(query).append(" AND ");
        }

        // Don't include Review records
        sb.append(" NOT (userAndWorkflowStatusPairs:REVIEW_*)");

        // get ALL normal workflow work at lead level
        results.addSearchResults( workflowService.findAssignedWork(mapProject, mapUser, MapUserRole.LEAD,
            sb.toString(), pfsParameter));
      }
      // Otherwise get all assigned work for the workflow type
      else {

        // get the map records
        results.addSearchResults( workflowService.findAssignedWork(mapProject, mapUser, MapUserRole.LEAD, query,
            pfsParameter));
      }

      if (!results.getSearchResults().isEmpty()) {
        final ProjectSpecificAlgorithmHandler handler =
            mappingService.getProjectSpecificAlgorithmHandler(mapProject);
        final Map<String, String> termNotes = handler.getAllTerminologyNotes();

        for (final SearchResult sr : results.getSearchResults()) {
          if (termNotes != null && termNotes.containsKey(sr.getTerminologyId())) {
            sr.setTerminologyNote(termNotes.get(sr.getTerminologyId()));
          }
        }
      }

      return results;

    } catch (final Exception e) {
      handleException(e, "trying to find assigned conflicts", user, project, "");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#
   * findAvailableReviewWork(java.lang.Long, java.lang.String, java.lang.String,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/availableReviewWork")
  @ApiOperation(value = "Find available review work for a map project.",
      notes = "Gets a list of search results of available review work for the specified parameters.",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAvailableReviewWork(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'",
        required = true) @QueryParam("query") final String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) final PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /project/id/"
        + mapProjectId.toString() + "/user/id" + userName + "/availableReviewWork " + query);

    String project = "";
    String user = null;

    try (final WorkflowService workflowService = new WorkflowServiceJpa();
        final MappingService mappingService = new MappingServiceJpa();) {
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
      localPfs.setAscending(pfsParameter.isAscending());

      SearchResultList availableWork = new SearchResultListJpa();

      // get ALL FixErrorPath work at specialist level for the project
      final WorkflowPathHandler fixErrorHandler = new WorkflowFixErrorPathHandler();
      final SearchResultList fixErrorWork = fixErrorHandler.findAvailableWork(mapProject, mapUser,
          MapUserRole.LEAD, query, localPfs, workflowService);

      // if review project path, get normal work
      if (mapProject.getWorkflowType().equals(WorkflowType.REVIEW_PROJECT)
          || mapProject.getWorkflowType().equals(WorkflowType.CONDITIONAL_REVIEW_PATH)) {

        // get ALL normal workflow work at lead level
        availableWork = workflowService.findAvailableWork(mapProject, mapUser, MapUserRole.LEAD,
            query, localPfs);
      }

      // if conflict and review path, get normal work
      if (mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_AND_REVIEW_PATH)) {

        // Need to make sure not to return maps that are currently in
        // conflict-review
        final StringBuilder sb = new StringBuilder();
        if (query != null && !query.isEmpty() && !query.equals("null")) {
          sb.append(query).append(" AND ");
        }

        sb.append(
            " NOT (userAndWorkflowStatusPairs:CONFLICT_DETECTED_* AND NOT userAndWorkflowStatusPairs:CONFLICT_FINISHED_*)");

        // get ALL normal workflow work at lead level
        availableWork = workflowService.findAvailableWork(mapProject, mapUser, MapUserRole.LEAD,
            sb.toString(), localPfs);
      }

      // combine the results
      availableWork.addSearchResults(fixErrorWork);

      // apply paging
      final int[] totalCt = new int[1];
      localPfs = new PfsParameterJpa(pfsParameter);
      localPfs.setQueryRestriction(null);
      localPfs.setSortField(null);

      // create list of SearchResultJpas
      // NOTE: This could be cleaned up with better typing
      // currently cannot convert List<SearchResultJpa> to
      // List<SearchResult>
      List<SearchResultJpa> results = new ArrayList<>();
      for (final SearchResult sr : availableWork.getSearchResults()) {
        results.add((SearchResultJpa) sr);
      }

      // apply paging to the list
      results = workflowService.applyPfsToList(results, SearchResultJpa.class, totalCt, localPfs);

      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      final Map<String, String> termNotes = handler.getAllTerminologyNotes();

      // reconstruct the assignedWork search result list
      availableWork.setSearchResults(new ArrayList<SearchResult>());
      for (final SearchResult sr : results) {
        if (termNotes != null && termNotes.containsKey(sr.getTerminologyId())) {
          sr.setTerminologyNote(termNotes.get(sr.getTerminologyId()));
        }
        availableWork.addSearchResult(sr);
      }

      return availableWork;
    } catch (final Exception e) {
      handleException(e, "trying to find available review work", user, project, "");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#findAvailableQAWork(
   * java.lang.Long, java.lang.String,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/availableQAWork")
  @ApiOperation(value = "Find available qa work for a map project.",
      notes = "Gets a list of search results of available qa work for the specified parameters.",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAvailableQAWork(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "Query, e.g. 'heart attack'",
        required = true) @QueryParam("query") final String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) final PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /project/id/"
        + mapProjectId.toString() + "/availableQAWork " + query);

    String project = "";
    String user = "qa"; // always the qa user for specialist level work

    try (final WorkflowService workflowService = new WorkflowServiceJpa();
        final MappingService mappingService = new MappingServiceJpa();) {

      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST, "find available qa work ",
          securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(user);
      user = mapUser.getUserName();

      final WorkflowPathHandler handler = new WorkflowQaPathHandler();
      final SearchResultList results = handler.findAvailableWork(mapProject, mapUser,
          MapUserRole.SPECIALIST, query, pfsParameter, workflowService);

      if (!results.getSearchResults().isEmpty()) {
        final ProjectSpecificAlgorithmHandler projectHandler =
            mappingService.getProjectSpecificAlgorithmHandler(mapProject);
        final Map<String, String> termNotes = projectHandler.getAllTerminologyNotes();

        for (final SearchResult sr : results.getSearchResults()) {
          if (termNotes != null && termNotes.containsKey(sr.getTerminologyId())) {
            sr.setTerminologyNote(termNotes.get(sr.getTerminologyId()));
          }
        }
      }

      return results;

    } catch (final Exception e) {
      e.printStackTrace();
      handleException(e, "trying to find available qa work", user, project, "");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#findAssignedReviewWork
   * (java.lang.Long, java.lang.String, java.lang.String,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/assignedReviewWork")
  @ApiOperation(value = "Find assigned review work for a map project.",
      notes = "Gets a list of search results of assigned review work for the specified parameters.",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAssignedReviewWork(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'",
        required = true) @QueryParam("query") final String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) final PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /project/id/"
        + mapProjectId.toString() + "/user/id/" + userName + "/assignedReviewWork " + query);

    String project = "";
    String user = null;

    try (final WorkflowService workflowService = new WorkflowServiceJpa();
        final MappingService mappingService = new MappingServiceJpa();) {

      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "find assigned review work ", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();

      PfsParameter localPfs = new PfsParameterJpa();
      localPfs.setQueryRestriction(pfsParameter.getQueryRestriction());
      localPfs.setSortField(pfsParameter.getSortField());
      localPfs.setAscending(pfsParameter.isAscending());

      // get ALL FixErrorPath work at specialist level for the project
      final WorkflowPathHandler fixErrorHandler = new WorkflowFixErrorPathHandler();
      final SearchResultList assignedWork = fixErrorHandler.findAssignedWork(mapProject, mapUser,
          MapUserRole.LEAD, query, localPfs, workflowService);

      // if a review project, get all normal workflow work and combine
      if (mapProject.getWorkflowType().equals(WorkflowType.REVIEW_PROJECT)
          || mapProject.getWorkflowType().equals(WorkflowType.CONDITIONAL_REVIEW_PATH)) {
        final SearchResultList reviewProjectWork = workflowService.findAssignedWork(mapProject, mapUser,
            MapUserRole.LEAD, query, localPfs);
        assignedWork.addSearchResults(reviewProjectWork);
      }

      // if conflict and review path, get normal work
      if (mapProject.getWorkflowType().equals(WorkflowType.CONFLICT_AND_REVIEW_PATH)) {

        // Need to make sure not to return maps that are currently in
        // conflict-review
        final StringBuilder sb = new StringBuilder();
        if (query != null && !query.isEmpty() && !query.equals("null")) {
          sb.append(query).append(" AND ");
        }

        sb.append(
            " NOT (userAndWorkflowStatusPairs:CONFLICT_DETECTED_* AND NOT userAndWorkflowStatusPairs:CONFLICT_FINISHED_*)");

        // get ALL normal workflow work at lead level
        assignedWork.addSearchResults(workflowService.findAssignedWork(mapProject, mapUser,
            MapUserRole.LEAD, sb.toString(), localPfs));
      }

      // apply paging
      final int[] totalCt = new int[1];
      localPfs = new PfsParameterJpa(pfsParameter);
      localPfs.setQueryRestriction("");
      localPfs.setSortField("");

      // create list of SearchResultJpas
      // NOTE: This could be cleaned up with better typing
      // currently cannot convert List<SearchResultJpa> to
      // List<SearchResult>
      List<SearchResultJpa> results = new ArrayList<>();
      for (final SearchResult sr : assignedWork.getSearchResults()) {
        results.add((SearchResultJpa) sr);
      }

      // apply paging to the list
      results = workflowService.applyPfsToList(results, SearchResultJpa.class, totalCt, localPfs);

      // reconstruct the assignedWork search result list
      assignedWork.setSearchResults(new ArrayList<SearchResult>());

      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      final Map<String, String> termNotes = handler.getAllTerminologyNotes();

      for (final SearchResult sr : results) {
        if (termNotes != null && termNotes.containsKey(sr.getTerminologyId())) {
          sr.setTerminologyNote(termNotes.get(sr.getTerminologyId()));
        }
        assignedWork.addSearchResult(sr);
      }

      return assignedWork;

    } catch (final Exception e) {
      handleException(e, "trying to find assigned review work", user, project, "");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#findAssignedQAWork(
   * java.lang.Long, java.lang.String, java.lang.String,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/assignedQAWork")
  @ApiOperation(value = "Find assigned qa work for a map project.",
      notes = "Gets a list of search results of assigned qa work for the specified parameters.",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findAssignedQAWork(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "User id, e.g. 2", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'",
        required = true) @QueryParam("query") final String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) final PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /project/id/"
        + mapProjectId.toString() + "/user/id/" + userName + "/assignedQAWork " + query);

    String project = "";
    String user = null;

    try (final WorkflowService workflowService = new WorkflowServiceJpa();
        final MappingService mappingService = new MappingServiceJpa();) {

      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST,
          "find assigned qa work ", securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      user = mapUser.getUserName();

      // SPECIAL CASE: Access QA Workflow Path directly
      final WorkflowPathHandler handler = new WorkflowQaPathHandler();
      final SearchResultList results = handler.findAssignedWork(mapProject, mapUser,
          MapUserRole.SPECIALIST, query, pfsParameter, workflowService);

      if (results.getSearchResults().isEmpty()) {
        return results;
      }

      final ProjectSpecificAlgorithmHandler projectHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      final Map<String, String> termNotes = projectHandler.getAllTerminologyNotes();

      if (termNotes == null || termNotes.isEmpty()) {
        return results;
      }

      for (final SearchResult sr : results.getSearchResults()) {
        if (termNotes != null && termNotes.containsKey(sr.getTerminologyId())) {
          sr.setTerminologyNote(termNotes.get(sr.getTerminologyId()));
        }
      }

      return results;

    } catch (final Exception e) {
      handleException(e, "trying to find assigned qa work", user, project, "");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#
   * assignConceptFromMapRecord(java.lang.String,
   * org.ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/assignFromRecord/user/id/{userName}")
  @ApiOperation(value = "Assign user to concept",
      notes = "Assigns a user (specialist or lead) to a previously mapped concept.")
  public void assignConceptFromMapRecord(
    @ApiParam(value = "Username", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Initial map record to copy, in JSON or XML POST data",
        required = true) final MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /assignFromRecord/user/id/" + userName
            + " with map record id " + mapRecord.getId() + " for project "
            + mapRecord.getMapProjectId() + " and concept id " + mapRecord.getConceptId());

    String project = "";

    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "assign concept from record", securityService);

      final MapProject mapProject = workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      final Concept concept = contentService.getConcept(mapRecord.getConceptId(),
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

      workflowService.processWorkflowAction(mapProject, concept, mapUser, mapRecord,
          WorkflowAction.ASSIGN_FROM_INITIAL_RECORD);

    } catch (final Exception e) {
      handleException(e, "trying to assign concept from a map record", userName, project,
          mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#assignConcept(java.
   * lang.Long, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @POST
  @Path("/assign/project/id/{id}/concept/id/{terminologyId}/user/id/{userName}")
  @ApiOperation(value = "Assign user to concept",
      notes = "Assigns specified user to map the specified concept for the specified project.")
  public void assignConcept(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "Concept id, e.g. 22298006",
        required = true) @PathParam("terminologyId") final String terminologyId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /assign/project/id/" + mapProjectId.toString()
            + "/concept/id/" + terminologyId + "/user/id/" + userName);

    String project = "";

    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST, "assign concept",
          securityService);

      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      final Concept concept = contentService.getConcept(terminologyId, mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion());

      workflowService.processWorkflowAction(mapProject, concept, mapUser, null,
          WorkflowAction.ASSIGN_FROM_SCRATCH);

    } catch (Exception e) {
      // deal with custom message for case when two users have claimed a concept
      // already and it is attempted to
      // be assigned to a third due to the display not getting updated.
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      if (mapProject.getWorkflowType() == WorkflowType.CONFLICT_PROJECT && e.getMessage()
          .startsWith("Workflow action ASSIGN_FROM_SCRATCH could not be performed on concept")) {
        e = new LocalException("Concept " + terminologyId
            + " is no longer available for assigning - please refresh to get up to date information.");
      }
      handleException(e, "trying to assign work", userName, project, terminologyId);
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#assignBatch(java.lang.
   * Long, java.lang.String, java.util.List, java.lang.String)
   */
  @Override
  @POST
  @Path("/assignBatch/project/id/{id}/user/id/{userName}")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Assign user to batch of concepts.",
      notes = "Assigns specified user to map the specified list of concept ids for the specified project.")
  public void assignBatch(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "List of terminology ids to be assigned, in JSON or XML POST data",
        required = true) final List<String> terminologyIds,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /assignBatch/project/id/" + mapProjectId + "/user/id/"
            + userName);

    String project = "";

    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST, "assign Batch",
          securityService);

      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);

      for (final String terminologyId : terminologyIds) {
        Logger.getLogger(WorkflowServiceRestImpl.class).info("   Assigning " + terminologyId);
        final Concept concept = contentService.getConcept(terminologyId,
            mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

        // Only process batch assignment request if the concept is
        // active
        // and thus has a tree position
        // This is a temporary fix for the Unmapped ICD-10 project
        // SEE MAP-862
        if (concept.isActive()) {

          workflowService.processWorkflowAction(mapProject, concept, mapUser, null,
              WorkflowAction.ASSIGN_FROM_SCRATCH);
        } else {
          Logger.getLogger(WorkflowServiceJpa.class)
              .warn("Skipping inactive concept " + concept.getTerminologyId());
        }
      }

    } catch (final Exception e) {
      handleException(e, "trying to assign a batch", userName, project, terminologyIds.toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#unassignConcept(java.
   * lang.Long, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @POST
  @Path("/unassign/project/id/{id}/concept/id/{terminologyId}/user/id/{userName}")
  @ApiOperation(value = "Unassign user from a concept.",
      notes = "Unassigns specified user from the specified concept for the specified project.")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Response unassignConcept(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "Concept id, e.g. 22298006",
        required = true) @PathParam("terminologyId") final String terminologyId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /unassign/project/id/" + mapProjectId.toString()
            + "/concept/id/" + terminologyId + "/user/id/" + userName);

    String project = "";
    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();

    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST, "unassign concept",
          securityService);

      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);
      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

      workflowService.processWorkflowAction(mapProject, concept, mapUser, null,
          WorkflowAction.UNASSIGN);

      return null;
    } catch (final Exception e) {
      handleException(e, "trying to unassign work", userName, project, terminologyId);
      return null;
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#unassignWorkBatch(java
   * .lang.Long, java.lang.String, java.util.List, java.lang.String)
   */
  @Override
  @POST
  @Path("/unassign/project/id/{id}/user/id/{userName}/batch")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Unassign user from a batch of concepts.",
      notes = "Unassigns specified user from the specified list of concept ids for the specified project.")
  public void unassignWorkBatch(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "List of terminology ids to be assigned, in JSON or XML POST data",
        required = true) final List<String> terminologyIds,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /unassign/project/id/" + mapProjectId.toString()
            + "/user/id/" + userName + "/batch - " + terminologyIds);

    String project = "";

    final WorkflowService workflowService = new WorkflowServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {

      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.SPECIALIST, "unassign Batch",
          securityService);

      // get the project and user
      final MapProject mapProject = workflowService.getMapProject(mapProjectId);
      project = mapProject.getName();
      final MapUser mapUser = workflowService.getMapUser(userName);

      for (final String terminologyId : terminologyIds) {
        final Concept concept = contentService.getConcept(terminologyId,
            mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());
        workflowService.processWorkflowAction(mapProject, concept, mapUser, null,
            WorkflowAction.UNASSIGN);
      }

    } catch (final Exception e) {
      handleException(e, "trying to unassign work batch", userName, project,
          terminologyIds.toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#finishWork(org.ihtsdo.
   * otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/finish")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Finish work on a map record.",
      notes = "Finished work on the specified map record if it passes validation, then moves it forward in the worfklow.")
  public void finishWork(
    @ApiParam(value = "Completed map record, in JSON or XML POST data",
        required = true) final MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /finish"
        + " for map record with id = " + mapRecord.getId().toString());

    String user = null;
    String project = "";
    final ContentService contentService = new ContentServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "finish work", securityService);

      // get the map project and map user
      final MapProject mapProject = workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final MapUser mapUser = mapRecord.getOwner();
      user = mapUser.getUserName();

      // get the concept
      final Concept concept = contentService.getConcept(mapRecord.getConceptId(),
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

      // execute the workflow call
      workflowService.processWorkflowAction(mapProject, concept, mapUser, mapRecord,
          WorkflowAction.FINISH_EDITING);

    } catch (final Exception e) {
      handleException(e, "trying to finish work", user, project, mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }

  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#publishWork(org.ihtsdo
   * .otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/publish")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Mark a map record for publication.",
      notes = "Moves a previously resolved conflict or review record owned by a lead out of the workflow and into publication-ready status")
  public void publishWork(
    @ApiParam(value = "Completed map record, in JSON or XML POST data",
        required = true) final MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /publish"
        + " for map record with id = " + mapRecord.getId().toString());

    String user = null;
    String project = "";
    final ContentService contentService = new ContentServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "publish work", securityService);

      // get the map project and map user
      final MapProject mapProject = workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final MapUser mapUser = mapRecord.getOwner();
      user = mapUser.getUserName();

      // get the concept
      final Concept concept = contentService.getConcept(mapRecord.getConceptId(),
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

      // execute the workflow call
      workflowService.processWorkflowAction(mapProject, concept, mapUser, mapRecord,
          WorkflowAction.PUBLISH);



      // mark all related feedback conversations resolved
      for (final FeedbackConversation conv : workflowService
          .getFeedbackConversationsForConcept(mapProject.getId(), concept.getTerminologyId())
          .getFeedbackConversations()) {
        conv.setResolved(true);
        conv.setLastModified(new Date());
        workflowService.updateFeedbackConversation(conv);
      }

    } catch (final Exception e) {
      handleException(e, "trying to publish work", user, project, mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }

  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#saveWork(org.ihtsdo.
   * otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/save")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Save work on a map record.",
      notes = "Updates the map record and sets workflow accordingly.")
  public void saveWork(
    @ApiParam(value = "Map record to save, in JSON or XML POST data",
        required = true) final MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /save"
        + " for map record with id = " + mapRecord.getId().toString());

    String user = null;
    String project = "";
    final ContentService contentService = new ContentServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.SPECIALIST, "save work",
          securityService);
      final MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to save a map record.").build());
      }

      // get the map project and map user
      final MapProject mapProject = workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final MapUser mapUser = mapRecord.getOwner();
      user = mapUser.getUserName();

      // get the concept
      final Concept concept = contentService.getConcept(mapRecord.getConceptId(),
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

      // process the workflow action
      workflowService.processWorkflowAction(mapProject, concept, mapUser, mapRecord,
          WorkflowAction.SAVE_FOR_LATER);

    } catch (final Exception e) {
      handleException(e, "trying to save work", user, project, mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }

  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#cancelWorkForMapRecord
   * (org.ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/cancel")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Cancel editing of a map record.",
      notes = "Cancels editing of a map record.  Performs necessary workflow actions for current workflow path and status.")
  public void cancelWorkForMapRecord(
    @ApiParam(
        value = "Map record to cancel work for , in JSON or XML POST data") final MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /cancel for map record with id = " + mapRecord.getId());

    final String userName = null;
    String project = "";
    // open the services
    final ContentService contentService = new ContentServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "cancel work", securityService);

      // get the map project and concept
      final MapProject mapProject = workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final Concept concept = contentService.getConcept(mapRecord.getConceptId(),
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

      // process the workflow action
      workflowService.processWorkflowAction(mapProject, concept, mapRecord.getOwner(), mapRecord,
          WorkflowAction.CANCEL);
    } catch (final Exception e) {
      handleException(e, "trying to cancel editing a map record", userName, project,
          mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }

  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#createQARecord(org.
   * ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/createQARecord")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Creates a qa record.", notes = "Creates a qa record given a map record.")
  public void createQARecord(@ApiParam(
      value = "Map record to create qa record for , in JSON or XML POST data") final MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info(
        "RESTful call (Workflow): /createQARecord for map record with id = " + mapRecord.getId());

    final String userName = null;
    String project = "";
    // open the services
    final ContentService contentService = new ContentServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "create QA record", securityService);

      // get the map project and concept
      final MapProject mapProject = workflowService.getMapProject(mapRecord.getMapProjectId());
      project = mapProject.getName();
      final Concept concept = contentService.getConcept(mapRecord.getConceptId(),
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

      // find the qa user
      MapUser mapUser = null;
      for (final MapUser user : workflowService.getMapUsers().getMapUsers()) {
        if (user.getUserName().equals("qa")) {
          mapUser = user;
        }
      }

      // process the workflow action
      workflowService.processWorkflowAction(mapProject, concept, mapUser, mapRecord,
          WorkflowAction.CREATE_QA_RECORD);

    } catch (final Exception e) {
      handleException(e, "trying to create a qa map record", userName, project,
          mapRecord.getId().toString());
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }

  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#createQAWork(java.lang
   * .Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/createQAWork")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Creates qa work.", notes = "Creates qa work given a report of concepts.")
  public void createQAWork(@ApiParam(
      value = "Report of concepts to create qa records for , in JSON or XML POST data") final Long reportId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /createQAWork for report with id = " + reportId);

    final String userName = null;
    final String project = "";
    Report report = null;

    final ReportService reportService = new ReportServiceJpa();
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {

      // get report and projectId
      report = reportService.getReport(reportId);

      // authorize call
      authorizeProject(report.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "create QA work", securityService);

      workflowService.createQAWork(report);
    } catch (final Exception e) {
      handleException(e, "trying to create qa work", userName, project,
          report == null ? "" : report.getId().toString());
    } finally {
      workflowService.close();
      reportService.close();
      securityService.clear();
    }

  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#
   * getAssignedMapRecordForConceptAndMapUser(java.lang.Long, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/record/project/id/{id}/concept/id/{terminologyId}/user/id/{userName}")
  @ApiOperation(value = "Get a map record for concept and user",
      notes = "Gets a map record for the specified project, concept id, and user info.")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecord getAssignedMapRecordForConceptAndMapUser(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "Concept id, e.g. 22298006",
        required = true) @PathParam("terminologyId") final String terminologyId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /assignedRecord/projectId/" + mapProjectId + "/concept/"
            + terminologyId + "/user/" + userName);

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
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

      final TrackingRecord trackingRecord =
          workflowService.getTrackingRecord(mapProject, concept.getTerminologyId());

      /**
       * Logger.getLogger(WorkflowServiceRestImpl.class).info("trackingRecord: "
       * + trackingRecord);
       *
       * for (final MapRecord mr : workflowService
       * .getMapRecordsForTrackingRecord(trackingRecord)) {
       * Logger.getLogger(WorkflowServiceRestImpl.class) .info("mr : " +
       * mr.getId() + " " + mr.getOwner().getUserName() + " " +
       * mr.getWorkflowStatus()); }
       */

      // choose the most recent of map_records that match the mapUser
      MapRecord mapRecordToReturn = null;
      for (final MapRecord mr : workflowService.getMapRecordsForTrackingRecord(trackingRecord)) {
        if (mr.getOwner().equals(mapUser)
            && (mapRecordToReturn == null || mapRecordToReturn.getId() < mr.getId())) {
          mapRecordToReturn = mr;
        }
      }

      return mapRecordToReturn;
    } catch (final Exception e) {
      handleException(e, "trying to get an assigned map record", user, project, terminologyId);
      return null;
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }

  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#
   * isMapRecordFalseConflict(java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/isFalseConflict")
  @ApiOperation(value = "Indicate whether a map record is a false conflict",
      notes = "Indicates whether the specified map record id is a false conflict.",
      response = Boolean.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Boolean isMapRecordFalseConflict(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") final Long recordId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /record/id/" + recordId + "/sFalseConflict");

    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      final MapRecord mapRecord = workflowService.getMapRecord(recordId);
      final MapProject mapProject = workflowService.getMapProject(mapRecord.getMapProjectId());

      // authorize call
      authorizeProject(mapProject.getId(), authToken, MapUserRole.SPECIALIST,
          "is map record false conflict", securityService);

      // if not a conflict resolution record, return null
      if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
          && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {
        return false;
      }

      final WorkflowException workflowException =
          workflowService.getWorkflowException(mapProject, mapRecord.getConceptId());

      if (workflowException != null) {
        if (workflowException.getFalseConflictMapRecordIds().contains(recordId)) {
          return true;
        } else {
          return false;
        }
      }
    } catch (final Exception e) {
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

  /* see superclass */
  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#
   * addFeedbackConversation(org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa,
   * java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/conversation/add")
  @ApiOperation(value = "Add a feedback conversation.",
      notes = "Adds the specified feedback conversation.", response = FeedbackConversationJpa.class)
  public FeedbackConversation addFeedbackConversation(
    @ApiParam(value = "Feedback conversation, in JSON or XML POST data",
        required = true) final FeedbackConversationJpa conversation,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    // log call
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /conversation/add");

    String userName = null;
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      userName = authorizeProject(conversation.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "add feedback conversation", securityService);

      Logger.getLogger(WorkflowServiceRestImpl.class)
          .info("RESTful call (Workflow): /conversation/update feedback msg: "
              + conversation.getFeedbacks().get(0));
      workflowService.addFeedbackConversation(conversation);

      return conversation;

    } catch (final Exception e) {
      handleException(e, "add a feedback conversation", userName, "",
          conversation.getMapRecordId().toString());
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#
   * setMapRecordFalseConflict(java.lang.Long, boolean, java.lang.String)
   */
  @Override
  @POST
  @Path("/record/id/{id:[0-9][0-9]*}/falseConflict/{isFalseConflict}")
  @ApiOperation(value = "Sets whether record is false conflict.",
      notes = "Sets a flag indicating a false conflict for the specified parameters.",
      response = Response.class)
  public void setMapRecordFalseConflict(
    @ApiParam(value = "Map record id, e.g. 7", required = true) @PathParam("id") final Long recordId,
    @ApiParam(value = "Whether is false conflict, e.g. true",
        required = true) @PathParam("isFalseConflict") final boolean isFalseConflict,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class).info(
        "RESTful call (Workflow): /record/id/" + recordId + "/setFalseConflict/" + isFalseConflict);

    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      final MapRecord mapRecord = workflowService.getMapRecord(recordId);
      final MapProject mapProject = workflowService.getMapProject(mapRecord.getMapProjectId());

      // authorize call
      authorizeProject(mapProject.getId(), authToken, MapUserRole.LEAD,
          "set map record false conflict", securityService);

      // if not a conflict resolution record, throw an error
      if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_NEW)
          && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_IN_PROGRESS)
          && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_RESOLVED)) {
        throw new WebApplicationException(Response.status(401)
            .entity("Cannot set false conflict flag on a non-conflict record").build());
      }

      WorkflowException workflowException =
          workflowService.getWorkflowException(mapProject, mapRecord.getConceptId());

      // if no workflow exception for this concept, add it
      if (workflowException == null) {
        workflowException = new WorkflowExceptionJpa();
        workflowException.setMapProjectId(mapProject.getId());
        workflowException.setTerminology(mapProject.getSourceTerminology());
        workflowException.setTerminologyVersion(mapProject.getSourceTerminologyVersion());
        workflowException.setTerminologyId(mapRecord.getConceptId());
      }

      final Set<Long> recordIds = new HashSet<>();

      // if setting to true, add the record ids
      if (isFalseConflict) {
        // find the tracking record for this map record
        final TrackingRecord trackingRecord = workflowService
            .getTrackingRecordForMapProjectAndConcept(mapProject, mapRecord.getConceptId());

        // instantiate workflow handler for this tracking record
        final WorkflowPathHandler handler =
            workflowService.getWorkflowPathHandler(trackingRecord.getWorkflowPath().toString());

        // add the specialist records for this conflict
        for (final MapRecord mr : handler
            .getOriginMapRecordsForMapRecord(mapRecord, workflowService).getIterable()) {
          recordIds.add(mr.getId());
        }

      }

      workflowException.setFalseConflictMapRecordIds(recordIds);

      // if empty, remove, if new, add, if not, update
      if (workflowException.isEmpty()) {

        // if id is set, remove the record
        if (workflowException.getId() != null) {
          workflowService.removeWorkflowException(workflowException.getId());
        }
      } else if (workflowException.getId() != null) {
        workflowService.updateWorkflowException(workflowException);
      } else {
        workflowService.addWorkflowException(workflowException);
      }

    } catch (final Exception e) {
      handleException(e, "trying to set flag for false conflict");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#
   * updateFeedbackConversation(org.ihtsdo.otf.mapping.jpa.
   * FeedbackConversationJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/conversation/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a feedback conversation.",
      notes = "Updates specified feedback conversation.", response = Response.class)
  public void updateFeedbackConversation(
    @ApiParam(value = "Feedback conversation, in JSON or XML POST data",
        required = true) final FeedbackConversationJpa conversation,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    // log call
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /conversation/update");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(conversation.getMapProjectId(), authToken, MapUserRole.VIEWER,
          "udpate feedback conversation", securityService);

      // Lock so only one can do this at a time.
      synchronized (lock) {
        // Get the old conversation
        final FeedbackConversation oldConvo =
            workflowService.getFeedbackConversation(conversation.getId());
        // hack to prevent duplicate feedback msgs from being added
        final Set<String> oldFeedbackMsgs = new HashSet<>();
        for (final Feedback f : oldConvo.getFeedbacks()) {
          oldFeedbackMsgs.add(f.getMessage());
        }
        // Find the "new" messages from this conversation and add them
        boolean found = false;
        boolean noUpdate = false;
        for (final Feedback feedback : conversation.getFeedbacks()) {
          if (feedback.getId() == null) {
            // check if "new" msg matches any already on conversation
            if (oldFeedbackMsgs.contains(feedback.getMessage())) {
              noUpdate = true;
              continue;
            }
            found = true;
            oldConvo.getFeedbacks().add(feedback);
          }
        }
        if (noUpdate && !found) {
          // do nothing - this is a duplicate request - msg matched exactly
        }
        // If a "new" one is found, add to the "old" convo
        else if (found) {
          // Set fields
          oldConvo.setDiscrepancyReview(conversation.isDiscrepancyReview());
          oldConvo.setResolved(conversation.isResolved());
          oldConvo.setTitle(conversation.getTitle());
          workflowService.updateFeedbackConversation(oldConvo);
        }
        // Otherwise, just update this
        else {
          workflowService.updateFeedbackConversation(conversation);
        }
      }

      // add debug
      final int ct = conversation.getFeedbacks().size();
      Logger.getLogger(WorkflowServiceRestImpl.class)
          .info("RESTful call (Workflow): /conversation/update feedback msg: "
              + conversation.getFeedbacks().get(ct - 1));

    } catch (final Exception e) {
      handleException(e, "update the feedback conversation");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#
   * getFeedbackConversation(java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/conversation/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get feedback conversation by map record id",
      notes = "Gets a feedback conversation for the specified map record id.",
      response = FeedbackConversation.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public FeedbackConversation getFeedbackConversation(
    @ApiParam(value = "Map record id, e.g. 28123",
        required = true) @PathParam("id") final Long mapRecordId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /conversation/id/" + mapRecordId.toString());

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final List<FeedbackConversation> conversations =
          workflowService.getFeedbackConversationsForRecord(mapRecordId).getFeedbackConversations();
      if (conversations.size() > 0) {
        // authorize call
        authorizeProject(conversations.get(0).getMapProjectId(), authToken, MapUserRole.VIEWER,
            "get feedback conversation", securityService);

        // lazy initialize
        for (final Feedback feedback : conversations.get(0).getFeedbacks()) {
          feedback.getSender().getName();
          for (final MapUser recipient : feedback.getRecipients()) {
            recipient.getName();
          }
          for (final MapUser viewedBy : feedback.getViewedBy()) {
            viewedBy.getName();
          }
        }
      } else {
        return null;
      }

      return conversations.get(0);

    } catch (final Exception e) {
      handleException(e, "trying to get the feedback conversation");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#
   * findFeedbackConversationsForMapProjectAndUser(java.lang.Long,
   * java.lang.String, java.lang.String,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/conversation/project/id/{id:[0-9][0-9]*}/{userName}")
  @ApiOperation(value = "Get feedback conversations by map project",
      notes = "Gets a list of feedback conversations for the specified map project and user.",
      response = FeedbackConversationListJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public FeedbackConversationList findFeedbackConversationsForMapProjectAndUser(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "Username", required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Query, e.g. 'heart attack'",
        required = true) @QueryParam("query") final String query,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) final PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    // log call
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /conversation/project/id/"
        + mapProjectId.toString() + " userName: " + userName + " query: " + query);

    final String user = null;

    // execute the service call
    try (final WorkflowService workflowService = new WorkflowServiceJpa();
        final MappingService mappingService = new MappingServiceJpa();) {

      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "find feedback conversations for project and user", securityService);

      final FeedbackConversationList feedbackConversationList = workflowService
          .findFeedbackConversationsForProject(mapProjectId, userName, query, pfsParameter);

      if (feedbackConversationList == null
          || feedbackConversationList.getFeedbackConversations().isEmpty()) {
        return feedbackConversationList;
      }

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);
      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      final Map<String, String> termNotes = handler.getAllTerminologyNotes();

      if (termNotes == null || termNotes.isEmpty()) {
        return feedbackConversationList;
      }

      for (final FeedbackConversation conversation : feedbackConversationList
          .getFeedbackConversations()) {
        if (termNotes.containsKey(conversation.getTerminologyId())) {
          conversation.setTerminologyNote(termNotes.get(conversation.getTerminologyId()));
        }
      }

      return feedbackConversationList;
    } catch (final Exception e) {
      handleException(e, "trying to get the feedback conversations for a map project", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      securityService.close();
    }

  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#
   * getFeedbackConversationsForTerminologyId(java.lang.Long, java.lang.String,
   * java.lang.String)
   */
  @Override
  @GET
  @Path("/conversation/project/id/{id:[0-9][0-9]*}/concept/id/{terminologyId}")
  @ApiOperation(value = "Get feedback conversations by concept id.",
      notes = "Gets a list of feedback conversations for the specified concept and project.",
      response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public FeedbackConversationListJpa getFeedbackConversationsForTerminologyId(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") final Long mapProjectId,
    @ApiParam(value = "Concept id, e.g. 22298006",
        required = true) @PathParam("terminologyId") final String conceptId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Workflow): /record/concept/id/" + conceptId);

    final String user = null;
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize call
      authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get feedback conversations for terminology id", securityService);

      return (FeedbackConversationListJpa) workflowService
          .getFeedbackConversationsForConcept(mapProjectId, conceptId);

    } catch (final Exception e) {
      handleException(e, "trying to find feedback conversations by the given concept id", user, "",
          conceptId);
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#
   * assignBatchToFixErrorPath(java.lang.Long, java.util.List, java.lang.String,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/assign/fixErrorPath/project/id/{projectId}/user/id/{userName}")
  @ApiOperation(value = "Assign concepts to fix error path",
      notes = "Assigns publication-ready map records to the Fix Error Workflow Path given a list of concept ids",
      response = ValidationResult.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  // @CookieParam(value = "userInfo")
  public ValidationResult assignBatchToFixErrorPath(
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") final Long mapProjectId,
    @ApiParam(value = "List of terminology ids to assign",
        required = true) final List<String> terminologyIds,
    @ApiParam(value = "Map user to assign to",
        required = true) @PathParam("userName") final String userName,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    // log call
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /assign/project/id/" + mapProjectId
        + "/fixErrorPath with ids: " + terminologyIds.toString());

    final String user = null;
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

      if (mapUser == null) {
        throw new LocalException("The user could not be found");
      }

      final ValidationResult result = new ValidationResultJpa();

      final List<MapRecord> mapRecords = new ArrayList<>();

      // cycle over these ids
      for (final String terminologyId : terminologyIds) {

        final MapRecordList mrList =
            workflowService.getMapRecordsForProjectAndConcept(mapProjectId, terminologyId);

        // first check: records getd
        if (mrList.getCount() == 0) {
          result.addError("No records found for concept " + terminologyId);
          continue;
        }

        // first check: only one record
        if (mrList.getCount() != 1) {
          result.addError("Multiple records present for concept " + terminologyId);
          continue;
        }

        // get the first record
        final MapRecord mapRecord = mrList.getIterable().iterator().next();

        // second check: PUBLISHED or READY_FOR_PUBLICATION
        if (!mapRecord.getWorkflowStatus().equals(WorkflowStatus.READY_FOR_PUBLICATION)
            && !mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)) {
          result.addError("Record is not publication-ready for concept " + terminologyId + ", "
              + mapRecord.getConceptName());
          continue;
        }

        mapRecords.add(mapRecord);
      }

      // cycle over all eligible map records
      for (final MapRecord mapRecord : mapRecords) {

        // get concept for this terminology id
        final Concept concept = contentService.getConcept(mapRecord.getConceptId(),
            mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());

        if (concept == null) {
          result.addError("Could not get concept for id " + mapRecord.getConceptId());
          continue;
        }

        try {

          // assign the user to this concept and record
          workflowService.processWorkflowAction(mapProject, concept, mapUser, mapRecord,
              WorkflowAction.ASSIGN_FROM_INITIAL_RECORD);

          // add success message if no errors thrown
          result.addMessage("Successfully assigned concept " + mapRecord.getConceptId() + ", "
              + concept.getDefaultPreferredName());
        } catch (final LocalException e) {
          result.addError("Concept already in workflow, could not assign concept "
              + mapRecord.getConceptId() + ", " + concept.getDefaultPreferredName());
        }
      }

      return result;

    } catch (final Exception e) {
      handleException(e, "trying to assign concepts along the fix error path.", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      workflowService.close();
      contentService.close();
      securityService.close();
    }

  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#sendFeedbackEmail(java
   * .util.List, java.lang.String)
   */
  @Override
  @POST
  @Path("/message")
  @ApiOperation(value = "Sends a feedback message email.",
      notes = "Sends a feedback message email.")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Response sendFeedbackEmail(
    @ApiParam(value = "message", required = true) final List<String> messageInfo,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    // log call
    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /message");

    String userName = "";
    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      // authorize call
      userName = securityService.getUsernameForToken(authToken);
      final MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER)) {
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add a feedback conversation.").build());
      }

      Logger.getLogger(WorkflowServiceRestImpl.class)
          .info("RESTful call (Workflow): /message msg: " + messageInfo);

      // Split up message and send parts
      workflowService.sendFeedbackEmail(messageInfo.get(0), messageInfo.get(1), messageInfo.get(2),
          messageInfo.get(3), messageInfo.get(4), messageInfo.get(5));

      return null;

    } catch (final Exception e) {
      handleException(e, "send a message email", userName, "", messageInfo.get(0));
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   *
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.WorkflowServiceRest#sendTranslationRequestEmail(java
   * .util.List, java.lang.String)
   */
  @Override
  @POST
  @Path("/translationRequest")
  @ApiOperation(value = "Sends a translation request email.",
      notes = "Sends a translation request email.")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Response sendTranslationRequestEmail(
    @ApiParam(value = "message", required = true) final List<String> messageInfo,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") final String authToken)
    throws Exception {

    // log call
    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Workflow): /translationRequest");

    String userName = "";
    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      // authorize call
      userName = securityService.getUsernameForToken(authToken);
      final MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER)) {
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to send a translation request email.").build());
      }

      Logger.getLogger(WorkflowServiceRestImpl.class)
          .info("RESTful call (Workflow): /translationRequest msg: " + messageInfo);

      // Split up message and send parts
      workflowService.sendTranslationRequestEmail(messageInfo.get(0), messageInfo.get(1), messageInfo.get(2),
          messageInfo.get(3), messageInfo.get(4), messageInfo.get(5));

      return null;

    } catch (final Exception e) {
      handleException(e, "send a translation request email", userName, "", messageInfo.get(0));
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }
}
