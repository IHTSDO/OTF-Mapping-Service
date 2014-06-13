/*
 * 
 */
package org.ihtsdo.otf.mapping.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.WorkflowAction;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

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

	/**  The security service. */
	private SecurityService securityService = new SecurityServiceJpa();

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
	@Path("/project/id/{id:[0-9][0-9]*}/compute")
	@ApiOperation(value = "Compute workflow for project", notes = "Destroys and recomputes the current workflow status from existing map records for a map project.  No mapping data is modified.")
	public void computeWorkflow(
			@ApiParam(value = "Map Project id", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /project/id/"
						+ mapProjectId.toString() + "/compute");


		try {	
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to compute workflow.").build());
			
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			mappingService.close();
			WorkflowService workflowService = new WorkflowServiceJpa();
			workflowService.computeWorkflow(mapProject);
			workflowService.close();
			return;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Finds available concepts for the specified map project and user.
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
	@Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/availableConcepts")
	@ApiOperation(value = "Find available concepts", notes = "Returns a paged list of work available to a specialist or lead for the specified map project.", response = SearchResultList.class)
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findAvailableConcepts(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Id of map user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /project/id/"
						+ mapProjectId.toString() + "/user/id/" + userName
						+ "/availableConcepts"
						+ " with PfsParameter: " + "\n"
						+ "     Index/Results = "
						+ Integer.toString(pfsParameter.getStartIndex()) + "/"
						+ Integer.toString(pfsParameter.getMaxResults()) + "\n"
						+ "     Sort field    = " + pfsParameter.getSortField()
						+ "     Filter String = "
						+ pfsParameter.getQueryRestriction());


		try {
			System.out.println("Authorizing at: " + System.currentTimeMillis()/1000);
			
			
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to find available work.").build());
			
			System.out.println("Retrieving objects at " + System.currentTimeMillis()/1000);
			
			
			// retrieve the project and user
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			MapUser mapUser = mappingService.getMapUser(userName);
			mappingService.close();
			
			System.out.println("Getting available work at " + System.currentTimeMillis()/1000);
			

			// retrieve the workflow tracking records
			WorkflowService workflowService = new WorkflowServiceJpa();
			SearchResultList results = workflowService.findAvailableWork(
					mapProject, mapUser, pfsParameter);
			workflowService.close();
			
			System.out.println("Done at " + System.currentTimeMillis()/1000);
			

			return results;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Finds assigned concepts for the specified map project and user.
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
	@Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/assignedConcepts")
	@ApiOperation(value = "Find assigned concepts", notes = "Returns a paged list of concepts assigned to a specialist or lead for the specified map project", response = SearchResultList.class)
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findAssignedConcepts(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Id of map user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /project/id/"
						+ mapProjectId.toString() + "/user/id/" + userName
						+ "/assignedConcepts"
						+ " with PfsParameter: " + "\n"
						+ "     Index/Results = "
						+ Integer.toString(pfsParameter.getStartIndex()) + "/"
						+ Integer.toString(pfsParameter.getMaxResults()) + "\n"
						+ "     Sort field    = " + pfsParameter.getSortField()
						+ "     Filter String = "
						+ pfsParameter.getQueryRestriction());
		

		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to find assigned work.").build());
			
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

		} catch (WebApplicationException e) {
			throw e;
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
	@Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/availableConflicts")
	@ApiOperation(value = "Find available conflicts", notes = "Returns a paged list of detected conflicts eligible for a lead's resolution for a specified map project.", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findAvailableConflicts(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Id of map user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /project/id/"
						+ mapProjectId.toString() + "/user/id" + userName
						+ "/availableConflicts"
						+ " with PfsParameter: " + "\n"
						+ "     Index/Results = "
						+ Integer.toString(pfsParameter.getStartIndex()) + "/"
						+ Integer.toString(pfsParameter.getMaxResults()) + "\n"
						+ "     Sort field    = " + pfsParameter.getSortField()
						+ "     Filter String = "
						+ pfsParameter.getQueryRestriction());


		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.LEAD))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to find available conflicts.").build());
			
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
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Finds assigned conflicts for the specified map project and user.
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
	@Path("/project/id/{id:[0-9][0-9]*}/user/id/{userName}/assignedConflicts")
	@ApiOperation(value = "Find assigned conflicts", notes = "Returns a paged list of conflicts assigned to a map lead for resolution for a specified map project.", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findAssignedConflicts(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Id of map user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /project/id/"
						+ mapProjectId.toString() + "/user/id/" + userName
						+ "/assignedConflicts"
						+ " with PfsParameter: " + "\n"
						+ "     Index/Results = "
						+ Integer.toString(pfsParameter.getStartIndex()) + "/"
						+ Integer.toString(pfsParameter.getMaxResults()) + "\n"
						+ "     Sort field    = " + pfsParameter.getSortField()
						+ "     Filter String = "
						+ pfsParameter.getQueryRestriction());


		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.LEAD))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to find assigned conflicts.").build());
			
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
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Assign user to to work based on an existing map record.
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
	@Path("/assignFromRecord/user/id/{userName}")
	@ApiOperation(value = "Assign user to concept", notes = "Assigns a user (specialist or lead) to a previously mapped concept.  The existing map record must be attached.")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public void assignConceptFromMapRecord(
			@ApiParam(value = "User name", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Initial map record to copy", required = true) MapRecordJpa mapRecord,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /assignFromRecord/user/id/" + userName
				+ " with map record id " + mapRecord.getId() + " for project " + mapRecord.getMapProjectId() + " and concept id " + mapRecord.getConceptId());


		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, new Long(mapRecord.getMapProjectId()));
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to assign work from a map record.").build());
			
			WorkflowService workflowService = new WorkflowServiceJpa();
			MappingService mappingService = new MappingServiceJpa();
			ContentService contentService = new ContentServiceJpa();

			MapProject mapProject = mappingService.getMapProject(new Long(
					mapRecord.getMapProjectId()));
			MapUser mapUser = mappingService.getMapUser(userName);
			Concept concept = contentService.getConcept(mapRecord.getConceptId(),
					mapProject.getSourceTerminology(),
					mapProject.getSourceTerminologyVersion());

			workflowService.processWorkflowAction(mapProject, concept, mapUser,
					mapRecord, WorkflowAction.ASSIGN_FROM_INITIAL_RECORD);

			mappingService.close();
			workflowService.close();
			contentService.close();

		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Assigns user to unmapped concept.
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
	@Path("/assign/project/id/{id}/concept/id/{terminologyId}/user/id/{userName}")
	@ApiOperation(value = "Assign user to concept", notes = "Assigns a user to an unmapped concept for a specified map project.")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public void assignConcept(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") String mapProjectId,
			@ApiParam(value = "Id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "User name", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
		
		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /assign/project/id/"
						+ mapProjectId.toString() + "/concept/id/" + terminologyId
						+ "/user/id/" + userName);
		

		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, new Long(mapProjectId));
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to assign work.").build());
			
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

		} catch (WebApplicationException e) {
			throw e;
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
	@Path("/assignBatch/project/id/{projectId}/user/id/{userName}")
	@ApiOperation(value = "Assign user to batch of concepts.", notes = "Assigns the given user to a batch of concepts corresponding to the passed list of concept ids")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public void assignBatch(
			@ApiParam(value = "Id of map project", required = true) @PathParam("projectId") String mapProjectId,
			@ApiParam(value = "User name", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "List of terminology ids to be assigned", required = true) List<String> terminologyIds,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /assignBatch/project/id/"
						+ mapProjectId.toString() 
						+ "/user/id/" + userName);

        WorkflowService workflowService = null;
        MappingService mappingService = null;
        ContentService contentService = null;

		try {
			
			// authorize call
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, new Long(mapProjectId));
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to assign batches.").build());
			
	        workflowService = new WorkflowServiceJpa();
	        mappingService = new MappingServiceJpa();
	        contentService = new ContentServiceJpa();

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

		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Unassign user from a concept.
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
	@Path("/unassign/project/id/{id}/concept/id/{terminologyId}/user/id/{userName}")
	@ApiOperation(value = "Unassign user from work.", notes = "Ununassigns the user from either concept mapping or conflict resolution.", response = Response.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response unassignConcept(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") String mapProjectId,
			@ApiParam(value = "Id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "User name", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
		
		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /unassign/project/id/"
						+ mapProjectId.toString() + "/concept/id/" + terminologyId
						+ "/user/id" + userName);
		

		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, new Long(mapProjectId));
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to unassign work.").build());
			
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

		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	 
 	/**
 	 * Unassign user from all currently assigned work.
 	 *
 	 * @param mapProjectId the map project id
 	 * @param userName the user name
 	 * @return the map record
 	 */
	@POST
	@Path("/unassign/project/id/{id}/user/id/{userName}")
	@ApiOperation(value = "Unassign user from all currently assigned work.", notes = "Unassigns the user from all currently assigned work.  Destroys any editing completed.")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public void unassignAllWork(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "User name", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
		
		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /unassign/project/id/"
						+ mapProjectId.toString()
						+ "/user/id/" + userName);
		
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to unassign all work.").build());
			
			WorkflowService workflowService = new WorkflowServiceJpa();
			MappingService mappingService = new MappingServiceJpa();
			ContentService contentService = new ContentServiceJpa();

			// get the project and user
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			MapUser mapUser = mappingService.getMapUser(userName);
			
			// find the assigned work and assigned conflicts
			SearchResultList assignedConcepts = workflowService.findAssignedWork(mapProject, mapUser, null);
			SearchResultList assignedConflicts = workflowService.findAssignedConflicts(mapProject, mapUser, null);
			
			// unassign both types of work
			for (SearchResult searchResult : assignedConcepts.getSearchResults()) {
				System.out.println(searchResult.toString());
				Concept concept = contentService.getConcept(
						searchResult.getTerminologyId(),
						mapProject.getSourceTerminology(),
						mapProject.getSourceTerminologyVersion());
				System.out.println(concept);
				System.out.println("Concept: " + concept.getTerminologyId());
				
				workflowService.processWorkflowAction(mapProject, concept, mapUser, null, WorkflowAction.UNASSIGN);
			}
			for (SearchResult searchResult : assignedConflicts.getSearchResults()) {
				Concept concept = contentService.getConcept(
						searchResult.getTerminologyId(),
						mapProject.getSourceTerminology(),
						mapProject.getSourceTerminologyVersion());

				MapRecord mapRecord = mappingService.getMapRecord(searchResult.getId());

				workflowService.processWorkflowAction(mapProject, concept, mapUser, mapRecord, WorkflowAction.UNASSIGN);
			}
			

			mappingService.close();
			workflowService.close();
			contentService.close();
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}


	/**
	 * Attempt to validate and finish work on a record.
	 * 
	 * @param mapRecord
	 *            the completed map record
	 * @return the response
	 */
	@POST
	@Path("/finish")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Attempt to validate and finish work on a record", notes = "If a completed map record passes vlidation checks, updates the map record and advances the record in the workflow.")
	public void finishWork(
			@ApiParam(value = "Completed map record", required = true) MapRecordJpa mapRecord,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /finish"
				+ " for map record with id = " + mapRecord.getId().toString());
		
		try {
  		// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to set a record to finished.").build());
  		
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

		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

	/**
	 * Save map record without validation checks or workflow action.
	 * 
	 * @param mapRecord
	 *            the map record
	 * @return the Response
	 */
	@POST
	@Path("/save")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Saves an in-progress record", notes = "Updates the map record and sets workflow to editing in progress. Does not validate the record or advance it in the workflow process")
	public void saveWork(
			@ApiParam(value = "Map record to save", required = true) MapRecordJpa mapRecord,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /save"
				+ " for map record with id = " + mapRecord.getId().toString());
		
		try {
  		// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to set a record to editing in progress.").build());
  		
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

		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
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
	@ApiOperation(value = "Cancel editing a map record", notes="Cancels editing a record.  Performs any necessary workflow action depending on workflow path and status")
	public void cancelWorkForMapRecord(@ApiParam(value="The map record to cancel work for") MapRecordJpa mapRecord,
		@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) throws Exception {
		
		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /cancel for map record with id = " + mapRecord.getId());
		

		// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
			throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to cancel editing a map record.").build());
		
		
		// open the services
		ContentService contentService = new ContentServiceJpa();
		MappingService mappingService = new MappingServiceJpa();
		WorkflowService workflowService = new WorkflowServiceJpa();
		
		// get the map project and concept
		MapProject mapProject = mappingService.getMapProject(mapRecord.getMapProjectId());
		Concept concept = contentService.getConcept(mapRecord.getConceptId(), mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());
		
		// process the workflow action
		workflowService.processWorkflowAction(
				mapProject, 
				concept, 
				mapRecord.getOwner(), 
				mapRecord, 
				WorkflowAction.CANCEL);
		
	}

	
	/**
	 * Indicates whether or the record is editable by the user.
	 *
	 * @param userName the user name
	 * @param mapRecord the map record
	 * @return <code>true</code> if so, <code>false</code> otherwise
	 * @throws Exception the exception
	 */
	@POST
	@Path("/checkRecordEditable/user/id/{userName}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Set record to editing in progress", notes = "Updates the map record and sets workflow to editing in progress.")
	public boolean isMapRecordEditable(
			@ApiParam(value = "Name of map user", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "MapRecord to check permissions for", required = true) MapRecordJpa mapRecord,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) throws Exception {
		
		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /record/isEditable/" + userName
				+ " for map record with id = " + mapRecord.getId().toString());
		
		// authorize call
		MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
		if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
			throw new WebApplicationException(Response.status(401).entity(
					"User does not have permissions to query if map record is editable.").build());
		
		
		// get the map user and map project
		MappingService mappingService = new MappingServiceJpa();
		MapUser mapUser = mappingService.getMapUser(userName);
		MapProject mapProject = mappingService.getMapProject(mapRecord.getMapProjectId());
		
		ProjectSpecificAlgorithmHandler algorithmHandler =
		        mappingService.getProjectSpecificAlgorithmHandler(mapProject);
		mappingService.close();
		
		return algorithmHandler.isRecordEditableByUser(mapRecord, mapUser);

	}
	
	/**
	 * Gets the assigned map record from the existing workflow for concept and map user, if it exists
	 *
	 * @param mapProjectId the map project id
	 * @param terminologyId the terminology id
	 * @param userName the user name
	 * @return the assigned map record for concept and map user
	 * @throws Exception the exception
	 */
	@GET
	@Path("/record/project/id/{id}/concept/id/{terminologyId}/user/id/{userName}")
	@ApiOperation(value = "Return record for concept and user.", notes = "Given concept and user information, returns an assigned record if record exists in current workflow")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRecord getAssignedMapRecordForConceptAndMapUser(
			@ApiParam(value = "Map project id", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Terminology id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "User name", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) throws Exception {
		
		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /assignedRecord/projectId/" + mapProjectId + "/concept/" + terminologyId + "/user/" + userName);
		
		// authorize call
		MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
		if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
			throw new WebApplicationException(Response.status(401).entity(
					"User does not have permissions to retrieve an assigned map record given concept and user.").build());
		
		
		MappingService mappingService = new MappingServiceJpa();
		MapUser mapUser = mappingService.getMapUser(userName);
		MapProject mapProject = mappingService.getMapProject(mapProjectId);
		mappingService.close();
		
		ContentService contentService = new ContentServiceJpa();
		Concept concept = contentService.getConcept(terminologyId, mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());
		contentService.close();
		
		WorkflowService workflowService = new WorkflowServiceJpa();
		TrackingRecord trackingRecord = workflowService.getTrackingRecord(mapProject, concept);
		
		for (MapRecord mr : workflowService.getMapRecordsForTrackingRecord(trackingRecord)) {
			if (mr.getOwner().equals(mapUser)) {
				workflowService.close();
				return mr;
			}
		}
		
		workflowService.close();
		
		return null;
	
	}
	
	/**
	 * Generate random conflicts.
	 *
	 * @param mapProjectId the map project id
	 * @param nConflicts the n conflicts
	 * @throws Exception the exception
	 */
	@POST
	@Path("/project/id/{id:[0-9][0-9]*}/generateConflicts/maxConflicts/{nConflicts:[0-9][0-9]*}")
	@ApiOperation(value = "Generate random conflicts.", notes = "Attempts to generate up to a specified number of conflicts using randomized assignment and editing of map records")
	public void generateRandomConflicts(
			@ApiParam(value = "Map Project id", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Number of conflicts to randomly generate", required = true) @PathParam("nConflicts") int nConflicts,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken
			) throws Exception {
		
		Logger.getLogger(WorkflowServiceRest.class).info(
				"RESTful call (Workflow): /project/id/" + mapProjectId + "/generateConflicts/maxConflicts/" + nConflicts);
		
		// authorize call
		MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
		if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
			throw new WebApplicationException(Response.status(401).entity(
					"User does not have permissions to unassign work.").build());
		
		
		MappingService mappingService = new MappingServiceJpa();
		MapProject mapProject = mappingService.getMapProject(mapProjectId);
		mappingService.close();
		
		WorkflowService workflowService = new WorkflowServiceJpa();
		workflowService.generateRandomConflictData(mapProject, nConflicts);
		workflowService.close();
	}
	
	/**
	 * Generate mapping testing state, using concepts in the mapping period preceding June 2014.
	 *
	 * @param mapProjectId the map project id
	 * @param authToken the auth token
	 * @throws Exception the exception
	 */
	@POST
	@Path("/project/id/{id:[0-9][0-9]*}/generateTestingState")
	@ApiOperation(value = "Generate a workflow testing scenario for project", notes = "Performs concept assignment to test functionality, using concepts mapped in Cartographer prior to June 2014.")
	public void generateMappingTestingState(
			@ApiParam(value = "Id of map project to generate the testing state for", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) throws Exception {
		
		// authorize call
		MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
		if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
			throw new WebApplicationException(Response.status(401).entity(
					"User does not have permissions to generate a mapping testing state.").build());
		
		
		MappingService mappingService = new MappingServiceJpa();
		MapProject mapProject = mappingService.getMapProject(mapProjectId);
		
		WorkflowService workflowService = new WorkflowServiceJpa();
		workflowService.generateMapperTestingState(mapProject);
		workflowService.close();
	}

	

	
}
