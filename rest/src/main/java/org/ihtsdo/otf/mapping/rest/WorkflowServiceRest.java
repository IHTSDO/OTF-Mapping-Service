package org.ihtsdo.otf.mapping.rest;

import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordList;
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
import org.ihtsdo.otf.mapping.workflow.Workflow;
import org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * The Workflow Services REST package
 */
@Path("/workflow")
@Api(value = "/workflow", description = "Operations supporting workflow.")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class WorkflowServiceRest {

	
	/**
	 * Instantiates an empty {@link WorkflowServiceRest}.
	 */
	public WorkflowServiceRest() {

	}
	
	@GET
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Compute workflow for project by id", notes = "Computes workflow given a project id.")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public void computeWorkflow(
			@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {
		
		Logger.getLogger(WorkflowServiceRest.class).info("RESTful call (Workflow): /project/id/" + mapProjectId.toString());
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			WorkflowService workflowService = new WorkflowServiceJpa();
			workflowService.computeWorkflow(mapProject);
			workflowService.close();
			mappingService.close();
			return;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	@GET
	@Path("/work/id/{id:[0-9][0-9]*}/user/{userid:[0-9][0-9]*}")
	@ApiOperation(value = "Find available work.", notes = "Returns available work for a given user on a given map project.", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findAvailableWork(
		@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId, 
		@ApiParam(value = "Id of map user", required = true) @PathParam("userid") Long userId) {
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapProject project = mappingService.getMapProject(mapProjectId);
			MapUser user = mappingService.getMapUser(userId);
			mappingService.close();
			
			WorkflowService workflowService = new WorkflowServiceJpa();

			SearchResultList searchResultList = new SearchResultListJpa();
			
		  /** call getWorkflow and get the tracking records for unmapped in scope concepts.*/
		  Workflow workflow = workflowService.getWorkflow(project);
		  Set<WorkflowTrackingRecord> trackingRecords = workflow.getTrackingRecordsForUnmappedInScopeConcepts();
	    for (WorkflowTrackingRecord trackingRecord : trackingRecords) {
		    /** don't add cases where there are 2 assigned users already */
	      /** don't add cases where this specialist is already an assigned user */	
		    if (trackingRecord.getAssignedUsers().size() >= 2 ||
		    		trackingRecord.getAssignedUsers().contains(user)) {
		    	continue; 
	      } else {
	      	SearchResult searchResult = new SearchResultJpa();
	      	searchResult.setTerminology(trackingRecord.getTerminology());
	      	searchResult.setTerminologyId(trackingRecord.getTerminologyId());
	      	searchResult.setTerminologyVersion(trackingRecord.getTerminologyVersion());
	      	searchResult.setValue(trackingRecord.getDefaultPreferredName());
		    	searchResultList.addSearchResult(searchResult);
		    }   	
	    }
	    return searchResultList;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	@PUT
	@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
	@Path("/assign/{projectid:[0-9][0-9]*}/{terminologyid:[0-9][0-9]*}/{userid:[0-9][0-9]*}")
	@ApiOperation(value = "Assign user to concept.", notes = "Assigns the given user to the given concept.", response = Response.class)
	public Response assignUserToConcept(
		@ApiParam(value = "Id of map project", required = true) @PathParam("mapProjectId") Long mapProjectId, 
		@ApiParam(value = "Id of concept", required = true) @PathParam("terminologyId") Long terminologyId, 
		@ApiParam(value = "Id of user", required = true) @PathParam("userId") Long userId) {
		try {
			WorkflowService workflowService = new WorkflowServiceJpa();
			MappingService mappingService = new MappingServiceJpa();
			ContentService contentService = new ContentServiceJpa();
			MapProject project = mappingService.getMapProject(mapProjectId);
			MapUser user = mappingService.getMapUser(userId);
			Concept concept = contentService.getConcept(terminologyId);
			
			workflowService.assignUserToConcept(project, concept, user);
			
			mappingService.close();
			workflowService.close();
			contentService.close();
			
			
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		return null;
	}
	
	@PUT
	@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
	@Path("/assign/{projectid:[0-9][0-9]*}/{terminologyid:[0-9][0-9]*}/{recordid:[0-9][0-9]*}/{userid:[0-9][0-9]*}")
	@ApiOperation(value = "Assign user to concept.", notes = "Assigns the given user to the given concept.", response = Response.class)
	public Response assignUserToConcept(
		@ApiParam(value = "Id of map project", required = true) @PathParam("mapProjectId") Long mapProjectId, 
		@ApiParam(value = "Id of concept", required = true) @PathParam("terminologyId") Long terminologyId,  
		@ApiParam(value = "Id of map record", required = true) @PathParam("recordId") Long recordId, 
		@ApiParam(value = "Id of user", required = true) @PathParam("userId") Long userId) {
			try {
			WorkflowService workflowService = new WorkflowServiceJpa();
			MappingService mappingService = new MappingServiceJpa();
			ContentService contentService = new ContentServiceJpa();
			MapProject project = mappingService.getMapProject(new Long(mapProjectId));
			MapUser user = mappingService.getMapUser(new Long(userId));
			MapRecord record = mappingService.getMapRecord(new Long(recordId));
			Concept concept = contentService.getConcept(new Long(terminologyId));
			
			workflowService.assignUserToConcept(project, concept, record, user);
			
			
			mappingService.close();
			workflowService.close();
			contentService.close();
			
			
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		return null;
	}

	@GET
	@Path("/assigned/user/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Returns records assigned to given user.", notes = "Returns work assigned to a given user.")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })	
	public MapRecordList getRecordsAssignedToUser(
		@ApiParam(value = "Id of user", required = true) @PathParam("id")  Long userId) {
		MapRecordList assigned = new MapRecordList();
		try {
		  MappingService mappingService = new MappingServiceJpa();
		  List<MapRecord> mapRecords = mappingService.getMapRecords();
		  for (MapRecord mapRecord : mapRecords) {
			  if (mapRecord.getOwner().getId().toString().equals(userId.toString()))
			  	assigned.addMapRecord(mapRecord);
		  }
		  mappingService.close();
		  return assigned;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	public MapRecordList getRecentlyEditedMapRecords(MapUser specialist, PfsParameter pfsParameter) {
		//TODO: what does this mean?  
		// do envers query  AuditReader owner set to this user
		// get all the records (latest revision) of which the given user touched
		// this goes into the mappingService
		return null;
	}
}
