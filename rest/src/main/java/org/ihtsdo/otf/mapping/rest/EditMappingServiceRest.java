package org.ihtsdo.otf.mapping.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.ihtsdo.otf.mapping.jpa.services.EditMappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.EditMappingService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * The Mapping Service for editing (add/update/remove) Map objects
 */
@Path("/mapping")
@Api(value = "/", description = "Operations supporting Map objects.")
public class EditMappingServiceRest {

	/** The mapping service jpa. */
	private EditMappingService editMappingServiceJpa;
	
	/**
	 * Instantiates an empty {@link MappingServiceRest}.
	 */
	public EditMappingServiceRest() {
		editMappingServiceJpa = new EditMappingServiceJpa();
	}
	
	///////////////////////////////////////////////////
	// MapProject:
	// - addMapProject(MapProject mapProject)
	// - updateMapProject(MapProject mapProject)
	// - removeMapProject(Long mapProjectId)
	///////////////////////////////////////////////////
	
	// WHen looking for server results, put /mapping-rest/ as initial context, but only in eclipse
	// TODO: Probably want these to return responses. 
	// Leaving as void for now.
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
			editMappingServiceJpa.addMapProject(mapProject);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Updates a map project
	 * @param mapProjectId the id of the map project, used for path
	 * @param mapProject the map project to be added
	 * @return Response the response
	 */
	@POST
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Update a map project", notes = "Updates a map project", response = MapProject.class)
	public Response updateMapProject(@ApiParam(value = "Id of map project to update", required = true) @PathParam("id") Long mapProjectId,
							  @ApiParam(value = "The map project to update", required = true) MapProject mapProject) { 

		try {
			editMappingServiceJpa.updateMapProject(mapProject);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Removes a map project
	 * @param mapProjectId the id of the map project to be deleted
	 * @return Response the response
	 */
	@DELETE
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Removes a map project", notes = "Removes a map project", response = MapProject.class)
	public Response removeMapProject(@ApiParam(value = "Id of map project to remove", required = true) @PathParam("id") Long mapProjectId) { 

		//editMappingServiceJpa.removeMapProject(mapProjectId);
		return null;
	}

}
