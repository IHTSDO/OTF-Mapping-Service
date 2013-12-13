package org.ihtsdo.otf.mapping.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * The Mapping Services REST package
 */
@Path("/mapping")
@Api(value = "/mapping", description = "Operations supporting Map objects.")
@Produces({MediaType.APPLICATION_JSON})
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
	// MapProject: getMapProject
	// - getMapProjectForIdJson(Long mapProjectId)
	// - getMapProjectForIdJson(String name)
	// - getMapProjectForIdXML(Long mapProjectId)
	// - getMapProjectForIdXML(String name)
	/////////////////////////////////////////////////////
	
	/**
	 * Returns the project for a given id (auto-generated) in JSON format
	 * @param mapProjectId the mapProjectId
	 * @return the mapProject
	 */
	@GET
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find project by id", notes = "Returns a MapProject in json given a project id.", response = MapProject.class)
	@Produces({MediaType.APPLICATION_JSON})
	public MapProject getMapProjectForIdJson(@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) { 
		return mappingServiceJpa.getMapProject(mapProjectId);
	}
	
	/**
	 * Returns the map project for a given name in JSON format
	 * @param mapProjectName the map project name
	 * @return the mapProject
	 */
	@GET
	@Path("/project/name/json/{name}")
	@ApiOperation(value = "Find project by name", notes = "Returns a MapProject in json given a project name.", response = MapProject.class)
	@Produces({MediaType.APPLICATION_JSON})
	public MapProject getMapProjectForNameJson(@ApiParam(value = "Name of map project to fetch", required = true) @PathParam("name") String mapProjectName) { 
		return mappingServiceJpa.getMapProject(mapProjectName);
	}
	
	/**
	 * Returns the project for a given id in XML format
	 * @param mapProjectId the mapProjectId
	 * @return the mapProject
	 */
	@GET
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find project by id", notes = "Returns a MapProject as xml given a project id.", response = MapProject.class)
	@Produces({MediaType.APPLICATION_XML})
	public MapProject getMapProjectForIdXml(@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) { 
		return mappingServiceJpa.getMapProject(mapProjectId);
	}
	
	/**
	 * Returns the map project for a given name in JSON format
	 * @param mapProjectName the map project name
	 * @return the mapProject
	 */
	@GET
	@Path("/project/name/xml/{name}")
	@ApiOperation(value = "Find project by name", notes = "Returns a MapProject as JSON given a project name.", response = MapProject.class)
	@Produces({MediaType.APPLICATION_XML})
	public MapProject getMapProjectForNameXML(@ApiParam(value = "Name of map project to fetch", required = true) @PathParam("name") String mapProjectName) { 
		return mappingServiceJpa.getMapProject(mapProjectName);
	}
	
	/////////////////////////////////////////////////////
	// MapProject: getMapProjects
	// - getMapProjectsJson()
	// - getMapProjectsXML()
	/////////////////////////////////////////////////////
	
	/**
	 * Returns all map projects in JSON format
	 * @return the map projects
	 */
	@GET
	@Path("/project/projects/json")
	@ApiOperation(value = "Find all projects", notes = "Returns all MapProjects as JSON")
	@Produces({MediaType.APPLICATION_JSON})
	public List<MapProject> getMapProjectsJson() {
		return mappingServiceJpa.getMapProjects();
	} 
	
	/**
	 * Returns all map projects in XML format
	 * @return the map projects
	 */
	@GET
	@Path("/project/projects/xml")
	@ApiOperation(value = "Find all projects", notes = "Returns all MapProjects as XML")
	@Produces({MediaType.APPLICATION_XML})
	public List<MapProject> getMapProjectsXML() {
		return mappingServiceJpa.getMapProjects();
	}
	
	/////////////////////////////////////////////////////
	// MapProject: findMapProjects
	// - findMapProjectsForQueryJson(String query)
	// - findMapProjectsForQueryXMLString query()
	/////////////////////////////////////////////////////
	
	/**
	 * Returns all map projects for a lucene query as Json
	 * @param query the string query
	 * @return the map projects
	 */
	@GET
	@Path("/project/query/json/{String}")
	@ApiOperation(value = "Find projects by query", notes = "Returns map projects for a query as JSON")
	@Produces({MediaType.APPLICATION_JSON})
	public List<MapProject> getMapProjectsForQueryJson(@ApiParam(value = "lucene search string", required = true) @PathParam("string") String query) {
		return mappingServiceJpa.findMapProjects(query);
	}
	
	/**
	 * Returns all map projects for a lucene query as XML
	 * @param query the string query
	 * @return the map projects
	 */
	@GET
	@Path("/project/query/xml/{String}")
	@ApiOperation(value = "Find projects by query", notes = "Returns map projects for a query as XML")
	@Produces({MediaType.APPLICATION_XML})
	public List<MapProject> getMapProjectsForQueryXML(@ApiParam(value = "lucene search string", required = true) @PathParam("string") String query) {
		return mappingServiceJpa.findMapProjects(query);
	}
	
	
	//////////////////////////////////////////////////////////////
	// MapSpecialist: getMapSpecialist
	// - getMapSpecialistsJson() 
	// - getMapSpecialistsXML()
	//////////////////////////////////////////////////////////////
	
	/**
	 * Returns all map specialists in JSON format
	 * @return the map specialists
	 */
	@GET
	@Path("/specialist/specialists/json")
	@ApiOperation(value = "Find all map specialists", notes = "Returns all MapSpecialists as Json")
	@Produces({MediaType.APPLICATION_JSON})
	public List<MapSpecialist> getMapSpecialistsJson() {
		return mappingServiceJpa.getMapSpecialists();
	}
	
	/**
	 * Returns all map specialists in XML format
	 * @return the map specialists
	 */
	@GET
	@Path("/specialist/specialists/xml")
	@ApiOperation(value = "Find all map specialists", notes = "Returns all MapSpecialists as Json")
	@Produces({MediaType.APPLICATION_XML})
	public List<MapSpecialist> getMapSpecialistsXML() {
		return mappingServiceJpa.getMapSpecialists();
	}
	
	///////////////////////////////////////////////////////////////////
	// MapSpecialist: getMapProjectsForSpecialist
	// - getMapProjectsForSpecialistJson(MapSpecialist mapSpecialist)
	// - getMapProjectsForSpecialistXMLMapSpecialist mapSpecialist)
	///////////////////////////////////////////////////////////////////
	
	/**
	 * Returns all projects for a map specialist in JSON format
	 * @param mapSpecialist the map specialist
	 * @return the map projects
	 */
	/**@GET
	@Path("/specialist/id/json/projects")
	@ApiOperation(value = "Find all map specialists", notes = "Returns all MapSpecialists as Json")
	@Produces({MediaType.APPLICATION_JSON})
	public List<MapProject> getMapProjectsForSpecialistJson(MapSpecialist mapSpecialist) {
		return mappingServiceJpa.getMapProjectsForMapSpecialist(mapSpecialist);
	} */
	
	/**
	 * Returns all projects for a map specialist in JSON format
	 * @param mapSpecialist the map specialist
	 * @return the map projects
	 */
	/**@GET
	@Path("/specialist/id/xml/projects")
	@ApiOperation(value = "Find all map specialists", notes = "Returns all MapSpecialists as Json")
	@Produces({MediaType.APPLICATION_XML})
	public List<MapProject> getMapProjectsForSpecialistXML(MapSpecialist mapSpecialist) {
		return mappingServiceJpa.getMapProjectsForMapSpecialist(mapSpecialist);
	} */
	
	//////////////////////////////////////////////////////////////
	// MapSpecialist: findMapSpecialist
	// - findMapSpecialistforQueryJson(String query)
	// - findMapSpecialistForQueryXML(String query)
	//////////////////////////////////////////////////////////////
	
	/**
	 * Returns all map specialists for a lucene query as Json
	 * @param query the string query
	 * @return the map specialists
	 */
	@GET
	@Path("/specialist/query/json/{String}")
	@ApiOperation(value = "Find specialists by query", notes = "Returns map specialists for a query as JSON")
	@Produces({MediaType.APPLICATION_JSON})
	public List<MapSpecialist> getMapSpecialistsForQueryJson(@ApiParam(value = "lucene search string", required = true) @PathParam("string") String query) {
		return mappingServiceJpa.findMapSpecialists(query);
	}
	
	/**
	 * Returns all map specialists for a lucene query as XML
	 * @param query the string query
	 * @return the map specialists
	 */
	@GET
	@Path("/specialist/query/xml/{String}")
	@ApiOperation(value = "Find specialists by query", notes = "Returns map specialists for a query as XML")
	@Produces({MediaType.APPLICATION_XML})
	public List<MapSpecialist> getMapSpecialistsForQueryXML(@ApiParam(value = "lucene search string", required = true) @PathParam("string") String query) {
		return mappingServiceJpa.findMapSpecialists(query);
	}
	
	//////////////////////////////////////////////////////////////
	// MapLead: getMapLead
	// - getMapLeadsJson() 
	// - getMapLeadsXML()
	//////////////////////////////////////////////////////////////
	
	/**
	* Returns all map leads in JSON format
	* @return the map leads
	*/
	@GET
	@Path("/lead/leads/json")
	@ApiOperation(value = "Find all map leads", notes = "Returns all MapLeads as Json")
	@Produces({MediaType.APPLICATION_JSON})
	public List<MapLead> getMapLeadsJson() {
	return mappingServiceJpa.getMapLeads();
	}
	
	/**
	* Returns all map leads in XML format
	* @return the map leads
	*/
	@GET
	@Path("/lead/leads/xml")
	@ApiOperation(value = "Find all map leads", notes = "Returns all MapLeads as Json")
	@Produces({MediaType.APPLICATION_XML})
	public List<MapLead> getMapLeadsXML() {
	return mappingServiceJpa.getMapLeads();
	}
	
	
	///////////////////////////////////////////////////////////////////
	// MapLead: getMapProjectsForSpecialist
	// - getMapProjectsForSpecialistJson(MapLead mapLead)
	// - getMapProjectsForSpecialistXMLMapLead mapLead)
	///////////////////////////////////////////////////////////////////
	
	/**
	* Returns all projects for a map lead in JSON format
	* @param mapLead the map lead
	* @return the map projects
	*/
	/**@GET
	@Path("/lead/id/json/projects")
	@ApiOperation(value = "Find all map leads", notes = "Returns all MapLeads as Json")
	@Produces({MediaType.APPLICATION_JSON})
	public List<MapProject> getMapProjectsForLeadJson(MapLead mapLead) {
	return mappingServiceJpa.getMapProjectsForMapLead(mapLead);
	} */
	
	/**
	* Returns all projects for a map lead in JSON format
	* @param mapLead the map lead
	* @return the map projects
	*/
	/**@GET
	@Path("/lead/id/xml/projects")
	@ApiOperation(value = "Find all map leads", notes = "Returns all MapLeads as Json")
	@Produces({MediaType.APPLICATION_XML})
	public List<MapProject> getMapProjectsForLeadXML(MapLead mapLead) {
	return mappingServiceJpa.getMapProjectsForMapLead(mapLead);
	} */
	
	//////////////////////////////////////////////////////////////
	// MapLead: findMapLead
	// - findMapLeadforQueryJson(String query)
	// - findMapLeadForQueryXML(String query)
	//////////////////////////////////////////////////////////////
	
	/**
	* Returns all map leads for a lucene query as Json
	* @param query the string query
	* @return the map leads
	*/
	@GET
	@Path("/lead/query/json/{String}")
	@ApiOperation(value = "Find leads by query", notes = "Returns map leads for a query as JSON")
	@Produces({MediaType.APPLICATION_JSON})
	public List<MapLead> getMapLeadsForQueryJson(@ApiParam(value = "lucene search string", required = true) @PathParam("string") String query) {
	return mappingServiceJpa.findMapLeads(query);
	}
	
	/**
	* Returns all map leads for a lucene query as XML
	* @param query the string query
	* @return the map leads
	*/
	@GET
	@Path("/lead/query/xml/{String}")
	@ApiOperation(value = "Find leads by query", notes = "Returns map leads for a query as XML")
	@Produces({MediaType.APPLICATION_XML})
	public List<MapLead> getMapLeadsForQueryXML(@ApiParam(value = "lucene search string", required = true) @PathParam("string") String query) {
	return mappingServiceJpa.findMapLeads(query);
	}
}
