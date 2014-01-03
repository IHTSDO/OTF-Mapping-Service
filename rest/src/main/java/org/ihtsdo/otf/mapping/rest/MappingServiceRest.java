package org.ihtsdo.otf.mapping.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.ihtsdo.otf.mapping.jpa.MapLeadList;
import org.ihtsdo.otf.mapping.jpa.MapProjectList;
import org.ihtsdo.otf.mapping.jpa.MapRecordList;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.services.SearchResultList;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * The Mapping Services REST package
 */
@Path("/mapping")
@Api(value = "/mapping", description = "Operations supporting Map objects.")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class MappingServiceRest {

	/** The mapping service jpa. */
	private MappingServiceJpa mappingServiceJpa;

	/**
	 * Instantiates an empty {@link MappingServiceRest}.
	 */
	public MappingServiceRest() {
		mappingServiceJpa = new MappingServiceJpa();
	}
	
	/////////////////////////////////////////////////////
	// Mapping Objects: Retrieval (@GET) functions
	// - getMapProjects()
	// - getMapLeads()
	// - getMapSpecialists()
	// - getMapProjectForId(Long mapProjectId)
	// - findMapProjectsForQuery(String query)
	/////////////////////////////////////////////////////

	/**
	 * Returns all map projects in either JSON or XML format
	 * 
	 * @return the map projects
	 */
	@GET
	@Path("/project/projects")
	@ApiOperation(value = "Get all projects", notes = "Returns all MapProjects in either JSON or XML format", response = MapProjectList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapProjectList getMapProjects() {
		MapProjectList mapProjects = new MapProjectList();	
		mapProjects.setMapProjects(mappingServiceJpa.getMapProjects());
		mapProjects.sortMapProjects();
		return mapProjects;
	}
	
	/**
	 * Returns all map leads in either JSON or XML format
	 * 
	 * @return the map leads
	 */
	@GET
	@Path("/lead/leads/")
	@ApiOperation(value = "Get all leads", notes = "Returns all MapLeads in either JSON or XML format", response = MapLeadList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapLeadList getMapLeads() {
		MapLeadList mapLeads = new MapLeadList();
		mapLeads.setMapLeads(mappingServiceJpa.getMapLeads());
		mapLeads.sortMapLeads();
		return mapLeads;
	}
	
	/**
	 * Returns all map specialists in either JSON or XML format
	 * 
	 * @return the map specialists
	 */
	@GET
	@Path("/specialist/specialists/")
	@ApiOperation(value = "Get all specialists", notes = "Returns all MapSpecialists in either JSON or XML format", response = MapSpecialistList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapSpecialistList getMapSpecialists() {
		MapSpecialistList mapSpecialists = new MapSpecialistList();
		mapSpecialists.setMapSpecialists(mappingServiceJpa.getMapSpecialists());
		mapSpecialists.sortMapSpecialists();
		return mapSpecialists;
	}
	
	/**
	 * Returns all map records in either JSON or XML format
	 * 
	 * @return the map records
	 */
	@GET
	@Path("/record/records/")
	@ApiOperation(value = "Get all records", notes = "Returns all MapRecords in either JSON or XML format", response = MapRecordList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRecordList getMapRecords() {
		MapRecordList mapRecords = new MapRecordList();
		mapRecords.setMapRecords(mappingServiceJpa.getMapRecords());
		mapRecords.sortMapRecords();
		return mapRecords;
	}
	
	/**
	 * Returns all map projects for a map lead in either JSON or XML format
	 * @param mapLeadId the map lead
	 * @return the map projects
	 */
	@GET
	@Path("/lead/id/{id:[0-9][0-9]*}/projects")
	@ApiOperation(value = "Find all projects for lead", notes = "Returns a MapLead's MapProjects in either JSON or XML format", response = MapProjectList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapProjectList getMapProjectsForLead(
			@ApiParam(value = "Id of map lead to fetch projects for", required = true) @PathParam("id") Long mapLeadId) { 
		MapProjectList mapProjects = new MapProjectList();	
		MapLead mapLead = mappingServiceJpa.getMapLead(mapLeadId);
		mapProjects.setMapProjects(mappingServiceJpa.getMapProjectsForMapLead(mapLead));
		mapProjects.sortMapProjects();
		return mapProjects;
	}

	/**
	 * Returns the project for a given id (auto-generated) in JSON format
	 * 
	 * @param mapProjectId the mapProjectId
	 * @return the mapProject
	 */
	@GET
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find project by id", notes = "Returns a MapProject given a project id in either JSON or XML format", response = MapProject.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapProject getMapProjectForId(
			@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {
		return mappingServiceJpa.getMapProject(mapProjectId);
	}
	
	/**
	 * Returns all map projects for a lucene query
	 * @param query the string query
	 * @return the map projects
	 */
	@GET
	@Path("/project/query/{String}")
	@ApiOperation(value = "Find projects by query", notes = "Returns map projects for a query in either JSON or XML format", response = MapProjectList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findMapProjects(
			@ApiParam(value = "lucene search string", required = true) @PathParam("String") String query) {
		return mappingServiceJpa.findMapProjects(query);
		
	}
	
	/**
	 * Returns the specialist for a given id (auto-generated) in JSON format
	 * 
	 * @param mapSpecialistId the mapSpecialistId
	 * @return the mapSpecialist
	 */
	@GET
	@Path("/specialist/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find specialist by id", notes = "Returns a MapSpecialist given a specialist id in either JSON or XML format", response = MapSpecialist.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapSpecialist getMapSpecialistForId(
			@ApiParam(value = "Id of map specialist to fetch", required = true) @PathParam("id") Long mapSpecialistId) {
		return mappingServiceJpa.getMapSpecialist(mapSpecialistId);
	}
	
	/**
	 * Returns all map specialists for a lucene query
	 * @param query the string query
	 * @return the map specialists
	 */
	@GET
	@Path("/specialist/query/{String}")
	@ApiOperation(value = "Find specialists by query", notes = "Returns map specialists for a query in either JSON or XML format", response = MapSpecialistList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findMapSpecialists(
			@ApiParam(value = "lucene search string", required = true) @PathParam("string") String query) {
		return mappingServiceJpa.findMapSpecialists(query);
	}
	
	/**
	 * Returns the lead for a given id (auto-generated) in JSON format
	 * 
	 * @param mapLeadId the mapLeadId
	 * @return the mapLead
	 */
	@GET
	@Path("/lead/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find lead by id", notes = "Returns a MapLead given a lead id in either JSON or XML format", response = MapLead.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapLead getMapLeadForId(
			@ApiParam(value = "Id of map lead to fetch", required = true) @PathParam("id") Long mapLeadId) {
		return mappingServiceJpa.getMapLead(mapLeadId);
	}
	
	/**
	 * Returns all map leads for a lucene query
	 * @param query the string query
	 * @return the map leads
	 */
	@GET
	@Path("/lead/query/{String}")
	@ApiOperation(value = "Find leads by query", notes = "Returns map leads for a query in either JSON or XML format", response = MapLeadList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findMapLeads(
			@ApiParam(value = "lucene search string", required = true) @PathParam("string") String query) {
		return mappingServiceJpa.findMapLeads(query);
	}
	
	/**
	 * Returns the record for a given id (auto-generated) in JSON format
	 * 
	 * @param mapRecordId the mapRecordId
	 * @return the mapRecord
	 */
	@GET
	@Path("/record/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find record by id", notes = "Returns a MapRecord given a record id in either JSON or XML format", response = MapRecord.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRecord getMapRecordForId(
			@ApiParam(value = "Id of map record to fetch", required = true) @PathParam("id") Long mapRecordId) {
		return mappingServiceJpa.getMapRecord(mapRecordId);
	}
	
	/**
	 * Returns the records for a given concept id
	 * 
	 * @param conceptId the concept id
	 * @return the mapRecords
	 */
	@GET
	@Path("/record/conceptId/{String}")
	@ApiOperation(value = "Find records by concept id", notes = "Returns MapRecords given a concept id in either JSON or XML format", response = MapRecord.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRecordList getMapRecordsForConceptId(
			@ApiParam(value = "Concept id of map record to fetch", required = true) @PathParam("String") String conceptId) {
		MapRecordList mapRecords = new MapRecordList();
		mapRecords.setMapRecords(mappingServiceJpa.getMapRecordsForConceptId(conceptId));
		return mapRecords;
	}
	
	/**
	 * Returns the records for a given concept id
	 * 
	 * @param projectId the projectId
	 * @return the mapRecords
	 */
	@GET
	@Path("/record/projectId/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find records by project id", notes = "Returns MapRecords given a project id in either JSON or XML format", response = MapRecord.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRecordList getMapRecordsForMapProjectId(
			@ApiParam(value = "Concept id of map record to fetch", required = true) @PathParam("id") String projectId) {
		MapRecordList mapRecords = new MapRecordList();
		mapRecords.setMapRecords(mappingServiceJpa.getMapRecordsForMapProjectId(projectId));
		return mapRecords;
	}
	
	/**
	 * Returns all map records for a lucene query
	 * @param query the string query
	 * @return the map records
	 */
	@GET
	@Path("/record/query/{string}")
	@ApiOperation(value = "Find records by query", notes = "Returns map records for a query in either JSON or XML format", response = MapRecordList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findMapRecords(
			@ApiParam(value = "lucene search string", required = true) @PathParam("string") String query) {
		return mappingServiceJpa.findMapRecords(query);
	}
	
	// ///////////////////////////////////////////////////
	// MapProject:  Add (@PUT) functions
	// - addMapProject
	// - addMapSpecialist
	// - addMapLead
	// ///////////////////////////////////////////////////
	
	/**
	 * Adds a map project
	 * @param mapProjectId the id of the map project, used for path
	 * @param mapProject the map project to be added
	 * @return Response the response
	 */
	@PUT
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Add a project", notes = "Adds a MapProject", response = MapProject.class)
	public Response addMapProject(@ApiParam(value = "Id of map project to add", required = true) @PathParam("id") Long mapProjectId,
							  @ApiParam(value = "The map project to add", required = true) MapProject mapProject) { 

		try {
			mappingServiceJpa.addMapProject(mapProject);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Adds a map lead
	 * @param mapLeadId the id of the map lead, used for path
	 * @param mapLead the map lead to be added
	 * @return Response the response
	 */
	@PUT
	@Path("/lead/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Add a lead", notes = "Adds a MapLead", response = MapLead.class)
	public Response addMapLead(@ApiParam(value = "Id of map lead to add", required = true) @PathParam("id") Long mapLeadId,
							  @ApiParam(value = "The map lead to add", required = true) MapLead mapLead) { 

		try {
			mappingServiceJpa.addMapLead(mapLead);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Adds a map specialist
	 * @param mapSpecialistId the id of the map specialist, used for path
	 * @param mapSpecialist the map specialist to be added
	 * @return Response the response
	 */
	@PUT
	@Path("/specialist/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Add a specialist", notes = "Adds a MapSpecialist", response = MapSpecialist.class)
	public Response addMapSpecialist(@ApiParam(value = "Id of map specialist to add", required = true) @PathParam("id") Long mapSpecialistId,
							  @ApiParam(value = "The map specialist to add", required = true) MapSpecialist mapSpecialist) { 

		try {
			mappingServiceJpa.addMapSpecialist(mapSpecialist);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Adds a map record
	 * @param mapRecordId the id of the map record, used for path
	 * @param mapRecord the map record to be added
	 * @return Response the response
	 */
	@PUT
	@Path("/record/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Add a record", notes = "Adds a MapRecord", response = MapRecord.class)
	public Response addMapRecord(@ApiParam(value = "Id of map record to add", required = true) @PathParam("id") Long mapRecordId,
							  @ApiParam(value = "The map record to add", required = true) MapRecord mapRecord) { 

		try {
			mappingServiceJpa.addMapRecord(mapRecord);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/////////////////////////////////////////////////////
	// MapProject:  Update (@POST) functions
	// - updateMapProject
	// - updateMapSpecialist
	// - updateMapLead
	/////////////////////////////////////////////////////
	
	
	/**
	 * Updates a map project
	 * @param mapProjectId the id of the map project, used for path
	 * @param mapProject the map project to be added
	 * @return Response the response
	 */
	@POST
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Update a project", notes = "Updates a map project", response = MapProject.class)
	public Response updateMapProject(@ApiParam(value = "Id of map project to update", required = true) @PathParam("id") Long mapProjectId,
							  @ApiParam(value = "The map project to update", required = true) MapProject mapProject) { 

		try {
			mappingServiceJpa.updateMapProject(mapProject);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Updates a map lead
	 * @param mapLeadId the id of the map lead, used for path
	 * @param mapLead the map lead to be added
	 * @return Response the response
	 */
	@POST
	@Path("/lead/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Update a lead", notes = "Updates a map lead", response = MapLead.class)
	public Response updateMapLead(@ApiParam(value = "Id of map lead to update", required = true) @PathParam("id") Long mapLeadId,
							  @ApiParam(value = "The map lead to update", required = true) MapLead mapLead) { 

		try {
			mappingServiceJpa.updateMapLead(mapLead);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Updates a map specialist
	 * @param mapSpecialistId the id of the map specialist, used for path
	 * @param mapSpecialist the map specialist to be added
	 * @return Response the response
	 */
	@POST
	@Path("/specialist/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Update a specialist", notes = "Updates a map specialist", response = MapSpecialist.class)
	public Response updateMapSpecialist(@ApiParam(value = "Id of map specialist to update", required = true) @PathParam("id") Long mapSpecialistId,
							  @ApiParam(value = "The map specialist to update", required = true) MapSpecialist mapSpecialist) { 

		try {
			mappingServiceJpa.updateMapSpecialist(mapSpecialist);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Updates a map record
	 * @param mapRecordId the id of the map record, used for path
	 * @param mapRecord the map record to be added
	 * @return Response the response
	 */
	@POST
	@Path("/record/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Update a record", notes = "Updates a map record", response = MapRecord.class)
	public Response updateMapRecord(@ApiParam(value = "Id of map record to update", required = true) @PathParam("id") Long mapRecordId,
							  @ApiParam(value = "The map record to update", required = true) MapRecord mapRecord) { 

		try {
			mappingServiceJpa.updateMapRecord(mapRecord);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/////////////////////////////////////////////////////
	// MapProject:  Removal (@DELETE) functions
	// - removeMapProject
	// - removeMapSpecialist
	// - removeMapLead
	/////////////////////////////////////////////////////
	
	/**
	 * Removes a map project
	 * @param mapProjectId the id of the map project to be deleted
	 * @return Response the response
	 */
	@DELETE
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Remove a project", notes = "Removes a map project", response = MapProject.class)
	public Response removeMapProject(@ApiParam(value = "Id of map project to remove", required = true) @PathParam("id") Long mapProjectId) { 

		mappingServiceJpa. removeMapProject(mapProjectId);
		return null;
	}
	
	/**
	 * Removes a map lead
	 * @param mapLeadId the id of the map lead to be deleted
	 * @return Response the response
	 */
	@DELETE
	@Path("/lead/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Remove a lead", notes = "Removes a map lead", response = MapLead.class)
	public Response removeMapLead(@ApiParam(value = "Id of map lead to remove", required = true) @PathParam("id") Long mapLeadId) { 

		mappingServiceJpa. removeMapLead(mapLeadId);
		return null;
	}
	
	/**
	 * Removes a map specialist
	 * @param mapSpecialistId the id of the map specialist to be deleted
	 * @return Response the response
	 */
	@DELETE
	@Path("/specialist/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Remove a specialist", notes = "Removes a map specialist", response = MapSpecialist.class)
	public Response removeMapSpecialist(@ApiParam(value = "Id of map specialist to remove", required = true) @PathParam("id") Long mapSpecialistId) { 

		mappingServiceJpa. removeMapSpecialist(mapSpecialistId);
		return null;
	}
	
	/**
	 * Removes a map record
	 * @param mapRecordId the id of the map record to be deleted
	 * @return Response the response
	 */
	@DELETE
	@Path("/record/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Remove a record", notes = "Removes a map record", response = MapRecord.class)
	public Response removeMapRecord(@ApiParam(value = "Id of map record to remove", required = true) @PathParam("id") Long mapRecordId) { 

		mappingServiceJpa. removeMapRecord(mapRecordId);
		return null;
	}


	
	
	

}
