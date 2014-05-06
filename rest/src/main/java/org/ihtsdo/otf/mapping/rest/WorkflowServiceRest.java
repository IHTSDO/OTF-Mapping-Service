/*
 * 
 */
package org.ihtsdo.otf.mapping.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.WorkflowService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

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
	 * @param mapProjectId
	 *            the map project id
	 */
	@POST
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Compute workflow for project by id", notes = "Computes workflow given a project id.")
	public void computeWorkflow(
			@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /project/id/"
						+ mapProjectId.toString());

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
	 * @param mapProjectId
	 *            the map project id
	 * @param userName
	 *            the username
	 * @param pfsParameter
	 *            the paging parameter
	 * @return the search result list
	 */
	@POST
	@Path("/availableWork/projectId/{id:[0-9][0-9]*}/user/{userName}")
	@ApiOperation(value = "Get available work.", notes = "Returns available work for a given user on a given map project.", response = SearchResultList.class)
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findAvailableWork(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Id of map user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /availableWork/projectId/"
						+ mapProjectId.toString() + "/user/" + userName
						+ " with PfsParameter: " + "\n"
						+ "     Index/Results = "
						+ Integer.toString(pfsParameter.getStartIndex()) + "/"
						+ Integer.toString(pfsParameter.getMaxResults()) + "\n"
						+ "     Sort field    = " + pfsParameter.getSortField()
						+ "     Filter String = "
						+ pfsParameter.getFilterString());

		try {

			// retrieve the project and user
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			MapUser mapUser = mappingService.getMapUser(userName);
			mappingService.close();

			// retrieve the workflow tracking records
			WorkflowService workflowService = new WorkflowServiceJpa();
			SearchResultList results = workflowService.findAvailableWork(
					mapProject, mapUser, pfsParameter);
			workflowService.close();

			return results;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Finds assigned work for the specified map project and user.
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @param userName
	 *            the user name
	 * @param pfsParameter
	 *            the paging parameter
	 * @return the search result list
	 */
	@POST
	@Path("/assignedWork/projectId/{id:[0-9][0-9]*}/user/{userName}")
	@ApiOperation(value = "Get assigned work.", notes = "Returns assigned work for a given user on a given map project.", response = SearchResultList.class)
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findAssignedWork(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Id of map user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /assignedWork/projectId/"
						+ mapProjectId.toString() + "/user/" + userName
						+ " with PfsParameter: " + "\n"
						+ "     Index/Results = "
						+ Integer.toString(pfsParameter.getStartIndex()) + "/"
						+ Integer.toString(pfsParameter.getMaxResults()) + "\n"
						+ "     Sort field    = " + pfsParameter.getSortField()
						+ "     Filter String = "
						+ pfsParameter.getFilterString());
		try {

			// retrieve the project and user
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			MapUser mapUser = mappingService.getMapUser(userName);
			mappingService.close();

			// retrieve the workflow tracking records
			WorkflowService workflowService = new WorkflowServiceJpa();
			SearchResultList results = workflowService.findAssignedWork(
					mapProject, mapUser, pfsParameter);
			workflowService.close();

			return results;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Finds available conflicts for the specified map project and user.
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @param userName
	 *            the user name
	 * @param pfsParameter
	 *            the paging parameter
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

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /availableConflicts/projectId/"
						+ mapProjectId.toString() + "/user/" + userName
						+ " with PfsParameter: " + "\n"
						+ "     Index/Results = "
						+ Integer.toString(pfsParameter.getStartIndex()) + "/"
						+ Integer.toString(pfsParameter.getMaxResults()) + "\n"
						+ "     Sort field    = " + pfsParameter.getSortField()
						+ "     Filter String = "
						+ pfsParameter.getFilterString());

		try {

			// retrieve the project and user
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			MapUser mapUser = mappingService.getMapUser(userName);
			mappingService.close();

			// retrieve the workflow tracking records
			WorkflowService workflowService = new WorkflowServiceJpa();
			SearchResultList results = workflowService.findAvailableConflicts(
					mapProject, mapUser, pfsParameter);
			workflowService.close();

			return results;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Finds available conflicts for the specified map project and user.
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @param userName
	 *            the user name
	 * @param pfsParameter
	 *            the paging parameter
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

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /assignedConflicts/projectId/"
						+ mapProjectId.toString() + "/user/" + userName
						+ " with PfsParameter: " + "\n"
						+ "     Index/Results = "
						+ Integer.toString(pfsParameter.getStartIndex()) + "/"
						+ Integer.toString(pfsParameter.getMaxResults()) + "\n"
						+ "     Sort field    = " + pfsParameter.getSortField()
						+ "     Filter String = "
						+ pfsParameter.getFilterString());

		try {

			// retrieve the project and user
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			MapUser mapUser = mappingService.getMapUser(userName);
			mappingService.close();

			// retrieve the map records
			WorkflowService workflowService = new WorkflowServiceJpa();
			SearchResultList results = workflowService.findAssignedConflicts(
					mapProject, mapUser, pfsParameter);
			workflowService.close();

			return results;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Assign user to to work (concept mapping or conflict resolution).
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @param terminologyId
	 *            the terminology id
	 * @param userName
	 *            the user name
	 * @param mapRecord
	 *            the map record (can be null)
	 * @return the map record
	 */
	@POST
	@Path("/assign/record/projectId/{id}/concept/{terminologyId}/user/{userName}")
	@ApiOperation(value = "Assign user to concept.", notes = "Assigns the given user to the given concept.", response = Response.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response assignWorkFromRecord(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") String mapProjectId,
			@ApiParam(value = "Id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "String userName of user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Initial MapRecord to copy", required = true) MapRecordJpa mapRecord) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /assign/record/projectId/"
						+ mapProjectId.toString() + "/concept/" + terminologyId
						+ "/user/" + userName + " with record id = "
						+ mapRecord.getId().toString());

		try {
			WorkflowService workflowService = new WorkflowServiceJpa();
			MappingService mappingService = new MappingServiceJpa();
			ContentService contentService = new ContentServiceJpa();

			MapProject mapProject = mappingService.getMapProject(new Long(
					mapProjectId));
			MapUser mapUser = mappingService.getMapUser(userName);
			Concept concept = contentService.getConcept(terminologyId,
					mapProject.getSourceTerminology(),
					mapProject.getSourceTerminologyVersion());

			workflowService.processWorkflowAction(mapProject, concept, mapUser,
					mapRecord, WorkflowAction.ASSIGN_FROM_INITIAL_RECORD);

			mappingService.close();
			workflowService.close();
			contentService.close();

			return null;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Assigns work.
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @param terminologyId
	 *            the terminology id
	 * @param userName
	 *            the user name
	 * @return the response
	 */
	@POST
	@Path("/assign/projectId/{id}/concept/{terminologyId}/user/{userName}")
	@ApiOperation(value = "Assign user to concept.", notes = "Assigns the given user to the given concept.", response = Response.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response assignWork(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") String mapProjectId,
			@ApiParam(value = "Id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "String userName of user", required = true) @PathParam("userName") String userName) {
		
		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /assign/projectId/"
						+ mapProjectId.toString() + "/concept/" + terminologyId
						+ "/user/" + userName);
		
		try {
			WorkflowService workflowService = new WorkflowServiceJpa();
			MappingService mappingService = new MappingServiceJpa();
			ContentService contentService = new ContentServiceJpa();

			MapProject mapProject = mappingService.getMapProject(new Long(
					mapProjectId));
			MapUser mapUser = mappingService.getMapUser(userName);
			Concept concept = contentService.getConcept(terminologyId,
					mapProject.getSourceTerminology(),
					mapProject.getSourceTerminologyVersion());

			workflowService.processWorkflowAction(mapProject, concept, mapUser,
					null, WorkflowAction.ASSIGN_FROM_SCRATCH);

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
	 * @param mapProjectId
	 *            the map project id
	 * @param terminologyId
	 *            the terminology id
	 * @param userName
	 *            the user name
	 * @return the map record
	 */
	@POST
	@Path("/unassign/projectId/{id}/concept/{terminologyId}/user/{userName}")
	@ApiOperation(value = "Unassign user from work.", notes = "Ununassigns the user from either concept mapping or conflict resolution.", response = Response.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response unassignWork(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") String mapProjectId,
			@ApiParam(value = "Id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "String userName of user", required = true) @PathParam("userName") String userName) {
		
		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /unassign/projectId/"
						+ mapProjectId.toString() + "/concept/" + terminologyId
						+ "/user/" + userName);
		
		try {
			WorkflowService workflowService = new WorkflowServiceJpa();
			MappingService mappingService = new MappingServiceJpa();
			ContentService contentService = new ContentServiceJpa();

			MapProject mapProject = mappingService.getMapProject(new Long(
					mapProjectId));
			MapUser mapUser = mappingService.getMapUser(userName);
			Concept concept = contentService.getConcept(terminologyId,
					mapProject.getSourceTerminology(),
					mapProject.getSourceTerminologyVersion());

			workflowService.processWorkflowAction(mapProject, concept, mapUser,
					null, WorkflowAction.UNASSIGN);

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
	 * @param mapProjectId
	 *            the map project id
	 * @param userName
	 *            the user name
	 * @param terminologyIds
	 *            the terminology ids
	 * @return the Response
	 */
	@POST
	@Path("/assign/batch/projectId/{projectId}/user/{userName}")
	@ApiOperation(value = "Assign user to batch of concepts.", notes = "Assigns the given user to a batch of concepts.", response = Response.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response assignBatch(
			@ApiParam(value = "Id of map project", required = true) @PathParam("projectId") String mapProjectId,
			@ApiParam(value = "String userName of user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "List of terminology ids to be assigned", required = true) List<String> terminologyIds) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /assign/batch/projectId/"
						+ mapProjectId.toString() 
						+ "/user/" + userName);

		WorkflowService workflowService = new WorkflowServiceJpa();
		MappingService mappingService = new MappingServiceJpa();
		ContentService contentService = new ContentServiceJpa();

		try {
			MapProject mapProject = mappingService.getMapProject(new Long(
					mapProjectId));
			MapUser mapUser = mappingService.getMapUser(userName);

			for (String terminologyId : terminologyIds) {
				Logger.getLogger(WorkflowServiceRest.class).info(
						"   Assigning " + terminologyId);
				Concept concept = contentService.getConcept(terminologyId,
						mapProject.getSourceTerminology(),
						mapProject.getSourceTerminologyVersion());

				workflowService.processWorkflowAction(mapProject, concept,
						mapUser, null, WorkflowAction.ASSIGN_FROM_SCRATCH);

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
	 * Finishes work.
	 * 
	 * @param mapRecord
	 *            the map record
	 * @return the response
	 */
	@POST
	@Path("/finish")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Set record to finished", notes = "Updates the map record and sets workflow to editing done.")
	public Response finishWork(
			@ApiParam(value = "The map record", required = true) MapRecordJpa mapRecord) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /finish"
				+ " for map record with id = " + mapRecord.getId().toString());
		
		try {

			MappingService mappingService = new MappingServiceJpa();

			// get the map project and map user
			MapProject mapProject = mappingService.getMapProject(mapRecord
					.getMapProjectId());
			MapUser mapUser = mapRecord.getOwner();
			mappingService.close();

			// get the concept
			ContentService contentService = new ContentServiceJpa();
			Concept concept = contentService.getConcept(
					mapRecord.getConceptId(),
					mapProject.getSourceTerminology(),
					mapProject.getSourceTerminologyVersion());
			contentService.close();

			// execute the workflow call
			WorkflowService workflowService = new WorkflowServiceJpa();
			workflowService.processWorkflowAction(mapProject, concept, mapUser,
					mapRecord, WorkflowAction.FINISH_EDITING);
			workflowService.close();

			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

	/**
	 * Save map record (do not update workflow).
	 * 
	 * @param mapRecord
	 *            the map record
	 * @return the Response
	 */
	@POST
	@Path("/save")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Set record to editing in progress", notes = "Updates the map record and sets workflow to editing in progress.")
	public Response saveWork(
			@ApiParam(value = "MapRecord to save", required = true) MapRecordJpa mapRecord) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /save"
				+ " for map record with id = " + mapRecord.getId().toString());
		
		try {

			// get the map project and map user
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapRecord
					.getMapProjectId());
			MapUser mapUser = mapRecord.getOwner();
			mappingService.close();

			// get the concept
			ContentService contentService = new ContentServiceJpa();
			Concept concept = contentService.getConcept(
					mapRecord.getConceptId(),
					mapProject.getSourceTerminology(),
					mapProject.getSourceTerminologyVersion());
			contentService.close();

			// process the workflow action
			WorkflowService workflowService = new WorkflowServiceJpa();
			workflowService.processWorkflowAction(mapProject, concept, mapUser,
					mapRecord, WorkflowAction.SAVE_FOR_LATER);
			workflowService.close();

			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

}
