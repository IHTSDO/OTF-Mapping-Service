/*
 * 
 */
package org.ihtsdo.otf.mapping.rest;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

// TODO: Auto-generated Javadoc
/**
 * The Workflow Services REST package.
 */
@Path("/workflow")
@Api(value = "/workflow", description = "Operations supporting workflow.")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@SuppressWarnings("static-method")
public class WorkflowServiceRest {

	/**
	 * Instantiates an empty {@link WorkflowServiceRest}.
	 */
	public WorkflowServiceRest() {
		// do nothing
	}


	/**
	 * Compute workflow.
	 *
	 * @param mapProjectId the map project id
	 */
	@POST
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Compute workflow for project by id", notes = "Computes workflow given a project id.")
	public void computeWorkflow(
			@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {

		Logger.getLogger(WorkflowServiceRest.class).info("RESTful call (Workflow): /project/id/" + mapProjectId.toString());

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			mappingService.close();
			WorkflowService workflowService = new WorkflowServiceJpa();
			workflowService.computeWorkflow(mapProject);
			workflowService.close();
			return;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Finds available work for the specified map project and user.
	 *
	 * @param mapProjectId the map project id
	 * @param user the user id
	 * @param pfsParameter the paging parameter
	 * @return the search result list
	 */
	@POST
	@Path("/availableWork/projectId/{id:[0-9][0-9]*}/user/{userName}")
	@ApiOperation(value = "Get available work.", notes = "Returns available work for a given user on a given map project.", response = SearchResultList.class)
	@Consumes( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findAvailableWork(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId, 
			@ApiParam(value = "Id of map user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter) {
		try {
			
			
			// retrieve the project and user
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			MapUser mapUser = mappingService.getMapUser(userName);
			mappingService.close();
			
			Logger.getLogger(WorkflowServiceRest.class).info("RESTful call for project " + mapProject.getName() + " and user " + mapUser.getName() + " for index [" + pfsParameter.getStartIndex() + ":" + (pfsParameter.getMaxResults()-1) + "]");


			// retrieve the workflow tracking records
			WorkflowService workflowService = new WorkflowServiceJpa();
			List<WorkflowTrackingRecord> trackingRecords = workflowService.getAvailableWork(mapProject, mapUser);
			workflowService.close();
			
			// sort list of tracking records
			Collections.sort(
				trackingRecords,
				new Comparator<WorkflowTrackingRecord>() {
					@Override
					public int compare(WorkflowTrackingRecord w1, WorkflowTrackingRecord w2) {
						return w1.getSortKey().compareTo(w2.getSortKey());
					}
				});
			
			// initialize results list and set total count
			SearchResultList results = new SearchResultListJpa();
			results.setTotalCount(new Long(trackingRecords.size()));

			// set the start and end points (paging)
			int startIndex = (pfsParameter != null && pfsParameter.getStartIndex() != 1 
					? pfsParameter.getStartIndex() : 0);
			int endIndex = (pfsParameter != null && pfsParameter.getMaxResults() != -1 
					? Math.min(pfsParameter.getStartIndex() + pfsParameter.getMaxResults(), trackingRecords.size()) - 1 : trackingRecords.size());
			
			System.out.println("Available: Returning items from " + startIndex + " to " + endIndex);
			
			
			// for each requested object, construct a search result
			for (WorkflowTrackingRecord trackingRecord : trackingRecords.subList(startIndex, endIndex)) {
				SearchResult result = new SearchResultJpa();
				result.setTerminologyId(trackingRecord.getTerminologyId());
				result.setValue(trackingRecord.getDefaultPreferredName());
				results.addSearchResult(result);
				System.out.println("     Available -> " + result.getTerminologyId());
			}
			
			return results;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	
	//Request URL:http://localhost:8080/mapping-rest/workflow/assignedWork/id/1/user/dmo
	/**
	 * Finds assigned work for the specified map project and user.
	 *
	 * @param mapProjectId the map project id
	 * @param user the user id
	 * @param pfsParameter the paging parameter
	 * @return the search result list
	 */
	@POST
	@Path("/assignedWork/projectId/{id:[0-9][0-9]*}/user/{userName}")
	@ApiOperation(value = "Get assigned work.", notes = "Returns assigned work for a given user on a given map project.", response = SearchResultList.class)
	@Consumes( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findAssignedWork(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId, 
			@ApiParam(value = "Id of map user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter) {
		try {

			// retrieve the project and user
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			MapUser mapUser = mappingService.getMapUser(userName);
			mappingService.close();
			
			Logger.getLogger(WorkflowServiceRest.class).info("RESTful call for project " + mapProject.getName() + " and user " + mapUser.getName() + " for index [" + pfsParameter.getStartIndex() + ":" + (pfsParameter.getMaxResults()-1) + "]");


			// retrieve the workflow tracking records
			WorkflowService workflowService = new WorkflowServiceJpa();
			List<MapRecord> mapRecords = workflowService.getAssignedWork(mapProject, mapUser);
			workflowService.close();
			
			// sort list of tracking records
			Collections.sort(
				mapRecords,
				new Comparator<MapRecord>() {
					@Override
					public int compare(MapRecord w1, MapRecord w2) {
						return w1.getConceptName().compareTo(w2.getConceptName());
					}
				});
			
			// initialize results list and set total count
			SearchResultList results = new SearchResultListJpa();
			results.setTotalCount(new Long(mapRecords.size()));
			
//			System.out.println(mapRecords.size() + " assigned records");
//
//			// set the start and end points (paging)
//			int startIndex = (pfsParameter != null && pfsParameter.getStartIndex() != 1 
//					? pfsParameter.getStartIndex() : 0);
//			int endIndex = (pfsParameter != null && pfsParameter.getMaxResults() != -1 
//					? Math.min(pfsParameter.getStartIndex() + pfsParameter.getMaxResults(), mapRecords.size()) - 1 : mapRecords.size());
//			
//			System.out.println("Assigned: Returning items from " + startIndex + " to " + endIndex);
//			
			// for each requested object, construct a search result
			for (MapRecord mapRecord : mapRecords) { //.subList(startIndex, endIndex)) {
				SearchResult result = new SearchResultJpa();
				result.setId(mapRecord.getId());
				result.setTerminologyId(mapRecord.getConceptId());
				result.setValue(mapRecord.getConceptName());
				results.addSearchResult(result);
				
				System.out.println("     Assigned -> " + result.getTerminologyId());
			}
			
			return results;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Finds available conflicts for the specified map project and user.
	 *
	 * @param mapProjectId the map project id
	 * @param user the user id
	 * @param pfsParameter the paging parameter
	 * @return the search result list
	 */
	@POST
	@Path("/availableConflicts/projectId/{id:[0-9][0-9]*}/user/{userName}")
	@ApiOperation(value = "Get available conflicts.", notes = "Returns available conflicts for a given user on a given map project.", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findAvailableConflicts(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId, 
			@ApiParam(value = "Id of map user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter) {
		try {

			// retrieve the project and user
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			MapUser mapUser = mappingService.getMapUser(userName);
			mappingService.close();

			// retrieve the workflow tracking records
			WorkflowService workflowService = new WorkflowServiceJpa();
			List<WorkflowTrackingRecord> trackingRecords = workflowService.getAvailableConflicts(mapProject, mapUser);
			workflowService.close();
			
			// sort list of tracking records
			Collections.sort(
				trackingRecords,
				new Comparator<WorkflowTrackingRecord>() {
					@Override
					public int compare(WorkflowTrackingRecord w1, WorkflowTrackingRecord w2) {
						return w1.getSortKey().compareTo(w2.getSortKey());
					}
				});
			
			// initialize results list and set total count
			SearchResultList results = new SearchResultListJpa();
			results.setTotalCount(new Long(trackingRecords.size()));

			/*// set the start and end points (paging)
			int startIndex = (pfsParameter != null && pfsParameter.getStartIndex() != 1 
					? pfsParameter.getStartIndex() : 0);
			int endIndex = (pfsParameter != null && pfsParameter.getMaxResults() != -1 
					? Math.min(pfsParameter.getStartIndex() + pfsParameter.getMaxResults(), trackingRecords.size()) - 1 : trackingRecords.size());
			
			System.out.println("Returning items from " + startIndex + " to " + endIndex);
			*/	
			// for each requested object, construct a search result
			for (WorkflowTrackingRecord trackingRecord : trackingRecords) {
				SearchResult result = new SearchResultJpa();
				result.setTerminologyId(trackingRecord.getTerminologyId());
				result.setValue(trackingRecord.getDefaultPreferredName());
				results.addSearchResult(result);
				
				System.out.println("    -> Conflict for " + trackingRecord.getTerminologyId());
			}
			
			return results;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Finds available conflicts for the specified map project and user.
	 *
	 * @param mapProjectId the map project id
	 * @param userName the user name
	 * @param pfsParameter the paging parameter
	 * @return the search result list
	 */
	@POST
	@Path("/assignedConflicts/projectId/{id:[0-9][0-9]*}/user/{userName}")
	@ApiOperation(value = "Get available conflicts.", notes = "Returns available conflicts for a given user on a given map project.", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findAssignedConflicts(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId, 
			@ApiParam(value = "Id of map user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter) {
		try {

			// retrieve the project and user
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			MapUser mapUser = mappingService.getMapUser(userName);
			mappingService.close();

			// retrieve the map records
			WorkflowService workflowService = new WorkflowServiceJpa();
			List<MapRecord> mapRecords = workflowService.getAssignedConflicts(mapProject, mapUser);
			workflowService.close();
			
			// sort list of tracking records
			Collections.sort(
				mapRecords,
				new Comparator<MapRecord>() {
					@Override
					public int compare(MapRecord w1, MapRecord w2) {
						return w1.getConceptName().compareTo(w2.getConceptName());
					}
				});
			
			// initialize results list and set total count
			SearchResultList results = new SearchResultListJpa();
			results.setTotalCount(new Long(mapRecords.size()));

			/*// set the start and end points (paging)
			int startIndex = (pfsParameter != null && pfsParameter.getStartIndex() != 1 
					? pfsParameter.getStartIndex() : 0);
			int endIndex = (pfsParameter != null && pfsParameter.getMaxResults() != -1 
					? Math.min(pfsParameter.getStartIndex() + pfsParameter.getMaxResults(), mapRecords.size()) - 1 : mapRecords.size());
			
			System.out.println("Returning items from " + startIndex + " to " + endIndex);
			*/
			
			// for each requested object, construct a search result
			for (MapRecord mapRecord : mapRecords) {
				SearchResult result = new SearchResultJpa();
				result.setTerminologyId(mapRecord.getConceptId());
				result.setValue(mapRecord.getConceptName());
				results.addSearchResult(result);
			}
			
			return results;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Assign user to to work (concept mapping or conflict resolution).
	 *
	 * @param mapProjectId the map project id
	 * @param terminologyId the terminology id
	 * @param userName the user name
	 * @param mapRecord the map record (can be null)
	 * @return the map record
	 */
	@POST
	@Path("/assign/record/projectId/{id}/concept/{terminologyId}/user/{userName}")
	@ApiOperation(value = "Assign user to concept.", notes = "Assigns the given user to the given concept.", response = Response.class)
	@Produces({
		MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public Response assignWorkFromRecord(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") String mapProjectId, 
			@ApiParam(value = "Id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "String userName of user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Initial MapRecord to copy", required = true) MapRecordJpa mapRecord) {
		try {
			WorkflowService workflowService = new WorkflowServiceJpa();
			MappingService mappingService = new MappingServiceJpa();
			ContentService contentService = new ContentServiceJpa();

			MapProject mapProject = mappingService.getMapProject(new Long(mapProjectId));
			MapUser mapUser = mappingService.getMapUser(userName);
			Concept concept = contentService.getConcept(terminologyId, mapProject.getSourceTerminology(), 
					mapProject.getSourceTerminologyVersion());

			
			workflowService.processWorkflowAction(mapProject, concept, mapUser, mapRecord, WorkflowAction.ASSIGN_FROM_INITIAL_RECORD);

			mappingService.close();
			workflowService.close();
			contentService.close();

			return null;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	/*
	 * Assign user to to work (concept mapping or conflict resolution).
	 *
	 * @param mapProjectId the map project id
	 * @param terminologyId the terminology id
	 * @param userName the user name
	 * @param mapRecord the map record (can be null)
	 * @return the map record
	 */
	@POST
	@Path("/assign/projectId/{id}/concept/{terminologyId}/user/{userName}")
	@ApiOperation(value = "Assign user to concept.", notes = "Assigns the given user to the given concept.", response = Response.class)
	@Produces({
		MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public Response assignWork(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") String mapProjectId, 
			@ApiParam(value = "Id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "String userName of user", required = true) @PathParam("userName") String userName) {
		try {
			WorkflowService workflowService = new WorkflowServiceJpa();
			MappingService mappingService = new MappingServiceJpa();
			ContentService contentService = new ContentServiceJpa();

			MapProject mapProject = mappingService.getMapProject(new Long(mapProjectId));
			MapUser mapUser = mappingService.getMapUser(userName);
			Concept concept = contentService.getConcept(terminologyId, mapProject.getSourceTerminology(), 
					mapProject.getSourceTerminologyVersion());


			workflowService.processWorkflowAction(mapProject, concept, mapUser, null, WorkflowAction.ASSIGN_FROM_SCRATCH);
		
			
			mappingService.close();
			workflowService.close();
			contentService.close();

			return null;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Unassign user from work.
	 *
	 * @param mapProjectId the map project id
	 * @param terminologyId the terminology id
	 * @param userName the user name
	 * @return the map record
	 */
	@POST
	@Path("/unassign/projectId/{id}/concept/{terminologyId}/user/{userName}")
	@ApiOperation(value = "Unassign user from work.", notes = "Ununassigns the user from either concept mapping or conflict resolution.", response = Response.class)
	@Produces({
		MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public Response unassignWork(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") String mapProjectId, 
			@ApiParam(value = "Id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "String userName of user", required = true) @PathParam("userName") String userName) {
		try {
			WorkflowService workflowService = new WorkflowServiceJpa();
			MappingService mappingService = new MappingServiceJpa();
			ContentService contentService = new ContentServiceJpa();

			MapProject mapProject = mappingService.getMapProject(new Long(mapProjectId));
			MapUser mapUser = mappingService.getMapUser(userName);
			Concept concept = contentService.getConcept(terminologyId, mapProject.getSourceTerminology(), 
					mapProject.getSourceTerminologyVersion());

			workflowService.processWorkflowAction(mapProject, concept, mapUser, null, WorkflowAction.UNASSIGN);
			
			mappingService.close();
			workflowService.close();
			contentService.close();

			return null;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Assign batch to user.
	 *
	 * @param mapProjectId the map project id
	 * @param userName the user name
	 * @param terminologyIds the terminology ids
	 * @return the Response
	 */
	@POST
	@Path("/assign/batch/projectId/{projectId}/user/{userName}")
	@ApiOperation(value = "Assign user to batch of concepts.", notes = "Assigns the given user to a batch of concepts.", response = Response.class)
	@Produces({
		MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public Response assignBatch(
			@ApiParam(value = "Id of map project", required = true) @PathParam("projectId") String mapProjectId, 
			@ApiParam(value = "String userName of user", required = true) @PathParam("userName") String userName, 
			@ApiParam(value = "List of terminology ids to be assigned", required = true) List<String> terminologyIds) {

		Logger.getLogger(WorkflowServiceRest.class).info("RESTful call: assignBatchToUser");

		WorkflowService workflowService = new WorkflowServiceJpa();
		MappingService mappingService = new MappingServiceJpa();
		ContentService contentService = new ContentServiceJpa();


		try {
			MapProject mapProject = mappingService.getMapProject(new Long(mapProjectId));
			MapUser mapUser = mappingService.getMapUser(userName);

			for (String terminologyId : terminologyIds) {
				Concept concept = contentService.getConcept(terminologyId, mapProject.getSourceTerminology(), 
						mapProject.getSourceTerminologyVersion());

				workflowService.processWorkflowAction(mapProject, concept, mapUser, null, WorkflowAction.ASSIGN_FROM_SCRATCH);

			}

			mappingService.close();
			workflowService.close();
			contentService.close();

			return null;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Finish with a concept mapping or conflict resolution.
	 *
	 * @param mapProjectId the map project id
	 * @param terminologyId the terminology id
	 * @param userName the user name
	 * @param mapRecord the map record
	 * @return the Response
	 */
	@POST
	@Path("/finish")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })	
	@ApiOperation(value = "Set record to finished", notes = "Updates the map record and sets workflow to editing done.")
	public Response finishWork(
			@ApiParam(value = "The map record", required = true) MapRecordJpa mapRecord) {
		
		try {
			// get the map project and map user
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapRecord.getMapProjectId());
			MapUser mapUser = mapRecord.getOwner();
			mappingService.close();
			
			// get the concept
			ContentService contentService = new ContentServiceJpa();
			Concept concept = contentService.getConcept(mapRecord.getConceptId(), mapProject.getSourceTerminology(), 
					mapProject.getSourceTerminologyVersion());
			contentService.close();
			
			// execute the workflow call
			WorkflowService workflowService = new WorkflowServiceJpa();
			workflowService.processWorkflowAction(mapProject, concept, mapUser, mapRecord, WorkflowAction.FINISH_EDITING);
			workflowService.close();
		
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}
	
	/**
	 * Save map record (do not update workflow).
	 *
	 * @param mapRecord the map record
	 * @return the Response
	 */
	@POST
	@Path("/save")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })	
	@ApiOperation(value = "Set record to editing done", notes = "Updates the map record and sets workflow to editing done.")
	public Response saveWork(
			@ApiParam(value = "MapRecord to save", required = true) MapRecordJpa mapRecord) {
		
		try {
			
			// TODO Need to make this call agnostic (i.e. perhaps add another enum action)
			MappingService mappingService = new MappingServiceJpa();
			mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
			mappingService.updateMapRecord(mapRecord);
			mappingService.close();
			
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

}
