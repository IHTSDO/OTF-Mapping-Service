package org.ihtsdo.otf.mapping.rest;

import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.jpa.MapLeadList;
import org.ihtsdo.otf.mapping.jpa.MapProjectList;
import org.ihtsdo.otf.mapping.jpa.MapRecordList;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.MappingService;

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
	private MappingServiceJpa mappingService;
	
	/**
	 * Instantiates an empty {@link MappingServiceRest}.
	 */
	public MappingServiceRest() {

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
		
		try {
			MappingService mappingService = new MappingServiceJpa();
		
			//if (! mappingService.isFactoryOpen()) { System.out.println("REST: service manager not open"); }
			//if (! mappingService.isManagerOpen()) { System.out.println("REST: service manager not open"); }
			
			MapProjectList mapProjects = new MapProjectList();	
			mapProjects.setMapProjects(mappingService.getMapProjects());
			mapProjects.sortMapProjects();
			mappingService.close();
			return mapProjects;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapLeadList mapLeads = new MapLeadList();
			mapLeads.setMapLeads(mappingService.getMapLeads());
			mapLeads.sortMapLeads();
			mappingService.close();
			return mapLeads;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapSpecialistList mapSpecialists = new MapSpecialistList();
			mapSpecialists.setMapSpecialists(mappingService.getMapSpecialists());
			mapSpecialists.sortMapSpecialists();
			mappingService.close();
			return mapSpecialists;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecordList mapRecords = new MapRecordList();
			mapRecords.setMapRecords(mappingService.getMapRecords());
			mapRecords.sortMapRecords();
			mappingService.close();
			return mapRecords;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapProjectList mapProjects = new MapProjectList();	
			MapLead mapLead = mappingService.getMapLead(mapLeadId);
			mapProjects.setMapProjects(mappingService.getMapProjectsForMapLead(mapLead));
			mapProjects.sortMapProjects();
			mappingService.close();
			return mapProjects;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject =  mappingService.getMapProject(mapProjectId);
			mappingService.close();
			return mapProject;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Returns all map projects for a lucene query
	 * @param query the string query
	 * @return the map projects
	 */
	@GET
	@Path("/project/query/{String}")
	@ApiOperation(value = "Find projects by query", notes = "Returns map projects for a query in either JSON or XML format", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findMapProjects(
			@ApiParam(value = "lucene search string", required = true) @PathParam("String") String query) {
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			SearchResultList searchResultList = mappingService.findMapProjects(query, new PfsParameterJpa());
			mappingService.close();
			return searchResultList;
			
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
	}
	
	/**
	 * Returns all descendant concepts associated with a project
	 * @param query the map project id
	 * @return the SearchResultList of unmapped descendant concepts
	 */
	@GET
	@Path("/project/id/{id:[0-9][0-9]*}/unmappedDescendants")
	@ApiOperation(value = "Find projects with unmapped concepts", notes = "Returns unmapped concepts in either JSON or XML format", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Set<Concept> findUnmappedDescendantsForMapProject(
			@ApiParam(value = "lucene search string", required = true) @PathParam("id") Long projectId) {
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			Set<Concept> conceptSet =  mappingService.findUnmappedDescendantsForMapProject(projectId, new PfsParameterJpa());
			mappingService.close();
			return conceptSet;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
	}
	
	/**
	 * Returns all descendant concepts associated with a project
	 * @param projectId the map project id
	 * @return the SearchResultList of descendant concepts
	 */
	@GET
	@Path("/project/id/{id:[0-9][0-9]*}/descendants")
	@ApiOperation(value = "Find projects with unmapped concepts", notes = "Returns unmapped concepts in either JSON or XML format", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Set<Concept> findDescendantsForMapProjects(
			@ApiParam(value = "lucene search string", required = true) @PathParam("id") Long projectId) {
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			Set<Concept> conceptSet = mappingService.findUnmappedDescendantsForMapProject(projectId, new PfsParameterJpa());
			mappingService.close();
			return conceptSet;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapSpecialist mapSpecialist = mappingService.getMapSpecialist(mapSpecialistId);
			mappingService.close();
			return mapSpecialist;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			SearchResultList searchResultList = mappingService.findMapSpecialists(query, new PfsParameterJpa());
			mappingService.close();
			return searchResultList;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapLead mapLead = mappingService.getMapLead(mapLeadId);
			mappingService.close();
			return mapLead;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();	
			SearchResultList searchResultList = mappingService.findMapLeads(query, new PfsParameterJpa());		
			mappingService.close();
			return searchResultList;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecord mapRecord = mappingService.getMapRecord(mapRecordId);
			mappingService.close();
			return mapRecord;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();	
			MapRecordList mapRecords = new MapRecordList();
			mapRecords.setMapRecords(mappingService.getMapRecordsForConceptId(conceptId));
			mappingService.close();
			return mapRecords;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
			@ApiParam(value = "Concept id of map record to fetch", required = true) @PathParam("id") Long projectId) {
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecordList mapRecords = new MapRecordList();
			mapRecords.setMapRecords(mappingService.getMapRecordsForMapProjectId(projectId));
			mappingService.close();
			return mapRecords;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			SearchResultList searchResultList = mappingService.findMapRecords(query, new PfsParameterJpa());
			mappingService.close();
			return searchResultList;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
			MappingService mappingService = new MappingServiceJpa();
			mappingService.addMapProject(mapProject);
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
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
			MappingService mappingService = new MappingServiceJpa();
			mappingService.addMapLead(mapLead);
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

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
			MappingService mappingService = new MappingServiceJpa();
			mappingService.addMapSpecialist(mapSpecialist);
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
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
			MappingService mappingService = new MappingServiceJpa();
			mappingService.addMapRecord(mapRecord);
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
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
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapProject(mapProject);
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
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
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapLead(mapLead);
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapSpecialist(mapSpecialist);
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
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
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapRecord(mapRecord);
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
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

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService. removeMapProject(mapProjectId);
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapLead(mapLeadId);
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService. removeMapSpecialist(mapSpecialistId);
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
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

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService. removeMapRecord(mapRecordId);
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}


	
	
	

}
