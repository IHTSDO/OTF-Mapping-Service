package org.ihtsdo.otf.mapping.rest;

import java.util.Collections;
import java.util.Comparator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ihtsdo.otf.mapping.jpa.MapProjectList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * The Mapping Services REST package
 */
@Path("/bac")
@Api(value = "/bac", description = "Operations supporting Map objects.")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class MappingServiceRestBac {

	/** The mapping service jpa. */
	private MappingServiceJpa mappingServiceJpa;

	/**
	 * Instantiates an empty {@link MappingServiceRestBac}.
	 */
	public MappingServiceRestBac() {
		mappingServiceJpa = new MappingServiceJpa();
	}

	// ///////////////////////////////////////////////////
	// MapProject: getMapProject
	// - getMapProjectForIdJson(Long mapProjectId)
	// - getMapProjectForIdJson(String name)
	// - getMapProjectForIdXML(Long mapProjectId)
	// - getMapProjectForIdXML(String name)
	// ///////////////////////////////////////////////////

	/**
	 * Returns the project for a given id (auto-generated) in JSON format
	 * 
	 * @param mapProjectId
	 *            the mapProjectId
	 * @return the mapProject
	 */
	@GET
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find project by id", notes = "Returns a MapProject in json given a project id.", response = MapProject.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapProject getMapProjectForId(
			@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {
		return mappingServiceJpa.getMapProject(mapProjectId);
	}

	// ///////////////////////////////////////////////////
	// MapProject: getMapProjects
	// - getMapProjectsJson()
	// - getMapProjectsXML()
	// ///////////////////////////////////////////////////

	/**
	 * Returns all map projects
	 * 
	 * @return the map projects
	 */
	@GET
	@Path("/project/projects")
	@ApiOperation(value = "Find all projects", notes = "Returns all MapProjects as JSON")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapProjectList getMapProjects() {
		MapProjectList mapProjects = new MapProjectList();
		for (MapProject project : mappingServiceJpa.getMapProjects()) {
			mapProjects.addMapProject(project);
		}
		Collections.sort(mapProjects.getMapProjects(),
				new Comparator<MapProject>() {
					@Override
					public int compare(MapProject o1, MapProject o2) {
						return o1.getName().compareTo(o2.getName());
					}

				});
		return mapProjects;
	}

}
