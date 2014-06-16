package org.ihtsdo.otf.mapping.rest;

import java.util.Comparator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapProjectListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRelationListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * The Mapping Services REST package
 */
@Path("/mapping")
@Api(value = "/mapping", description = "Operations supporting Map objects.")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@SuppressWarnings("static-method")
public class MappingServiceRest {

	private SecurityService securityService = new SecurityServiceJpa();
	
	/**
	 * Instantiates an empty {@link MappingServiceRest}.
	 */
	public MappingServiceRest() {

	}

	/////////////////////////////////////////////////////
	// SCRUD functions:  Map Projects
	/////////////////////////////////////////////////////

	/**
	 * Returns all map projects in either JSON or XML format
	 * 
	 * @return the map projects
	 */
	@GET
	@Path("/project/projects")
	@ApiOperation(value = "Get all projects", notes = "Returns all MapProjects in either JSON or XML format", response = MapProjectListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapProjectListJpa getMapProjects(
		@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping):  /project/projects");
		
		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve map projects.").build());
  		
			MappingService mappingService = new MappingServiceJpa();

			MapProjectListJpa mapProjects = (MapProjectListJpa) mappingService
					.getMapProjects();

			// force instantiation of lazy collections
			for (MapProject mp : mapProjects.getMapProjects()) {
				mp.getScopeConcepts().size();
				mp.getScopeExcludedConcepts().size();
				mp.getMapAdvices().size();
				mp.getMapRelations().size();
				mp.getMapAdministrators().size();
				mp.getMapLeads().size();
				mp.getMapSpecialists().size();
				mp.getMapPrinciples().size();
				mp.getPresetAgeRanges().size();
			}

			mapProjects.sortBy(new Comparator<MapProject>() {
				@Override
				public int compare(MapProject o1, MapProject o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			mappingService.close();
			return mapProjects;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Returns the project for a given id (auto-generated) in JSON format
	 * 
	 * @param mapProjectId
	 *            the mapProjectId
	 * @return the mapProject
	 */
	@GET
	@Path("/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Get project by id", notes = "Returns a MapProject given a project id in either JSON or XML format", response = MapProject.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapProject getMapProject(
			@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/id/"
						+ mapProjectId.toString());


		
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve the map project.").build());
			
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			mapProject.getScopeConcepts().size();
			mapProject.getScopeExcludedConcepts().size();
			mapProject.getMapAdvices().size();
			mapProject.getMapRelations().size();
			mapProject.getMapAdministrators().size();
			mapProject.getMapLeads().size();
			mapProject.getMapSpecialists().size();
			mapProject.getMapPrinciples().size();
			mapProject.getPresetAgeRanges().size();
			mappingService.close();
			return mapProject;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Adds a map project
	 * 
	 * @param mapProject
	 *            the map project to be added
	 * @return returns the added map project object
	 */
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/project/add")
	@ApiOperation(value = "Add a project", notes = "Adds a MapProject", response = MapProjectJpa.class)
	public MapProject addMapProject(
			@ApiParam(value = "The map project to add. Must be in Json or Xml format", required = true) MapProjectJpa mapProject,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/add");

		
		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to add a map project.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapProject mp = mappingService.addMapProject(mapProject);
			mappingService.close();

			// force lazy instantiation of collections
			mp.getScopeConcepts().size();
			mp.getScopeExcludedConcepts().size();
			mp.getMapAdvices().size();
			mp.getMapRelations().size();
			mp.getMapAdministrators().size();
			mp.getMapLeads().size();
			mp.getMapSpecialists().size();
			mp.getMapPrinciples().size();
			mp.getPresetAgeRanges().size();

			return mp;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}
	
	/**
	 * Updates a map project
	 * 
	 * @param mapProject
	 *            the map project to be added
	 * @return Response the response
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/project/update")
	@ApiOperation(value = "Update a project", notes = "Updates a map project", response = MapProjectJpa.class)
	public void updateMapProject(
			@ApiParam(value = "The map project to update. Must exist in mapping database. Must be in Json or Xml format", required = true) MapProjectJpa mapProject,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/update");


		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProject.getId());
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to udpate a map project.").build());
			
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapProject(mapProject);
			mappingService.close();
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}
	
	/**
	 * Removes a map project.
	 * 
	 * @param mapProjectId
	 *            the map project object to delete
	 * @return Response the response
	 */
	@DELETE
	@Path("/project/delete")
	@ApiOperation(value = "Remove a project", notes = "Removes a map project", response = MapProject.class)
	public void removeMapProject(
			@ApiParam(value = "Map project object to delete", required = true)MapProjectJpa mapProject,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/delete for "
						+ mapProject.getName());


		
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProject.getId());
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to remove a map project.").build());
			
			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapProject(mapProject.getId());
			mappingService.close();
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Returns all map projects for a lucene query
	 * 
	 * @param query
	 *            the string query
	 * @return the map projects
	 */
	@GET
	@Path("/project/query/{String}")
	@ApiOperation(value = "Find projects by query", notes = "Returns map projects for a query in either JSON or XML format", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findMapProjectsForQuery(
			@ApiParam(value = "lucene search string", required = true) @PathParam("String") String query,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/query/" + query);

		
		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to find map projects.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			SearchResultList searchResultList = mappingService.findMapProjectsForQuery(
					query, new PfsParameterJpa());
			mappingService.close();
			return searchResultList;

		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Returns all map projects for a map user
	 * 
	 * @param mapUserName
	 *            the map user name
	 * @return the map projects
	 */
	@GET
	@Path("/project/user/id/{username}")
	@ApiOperation(value = "Get all projects for user", notes = "Returns a MapUser's MapProjects in either JSON or XML format", response = MapProjectListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapProjectListJpa getMapProjectsForUser(
			@ApiParam(value = "Username of map user to fetch projects for", required = true) @PathParam("username") String mapUserName,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/user/id/" + mapUserName);
			
		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to get the map projects for given user.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapUser mapLead = mappingService.getMapUser(mapUserName);
			MapProjectListJpa mapProjects = (MapProjectListJpa) mappingService
					.getMapProjectsForMapUser(mapLead);

			for (MapProject mapProject : mapProjects.getMapProjects()) {
				mapProject.getScopeConcepts().size();
				mapProject.getScopeExcludedConcepts().size();
				mapProject.getMapAdvices().size();
				mapProject.getMapRelations().size();
				mapProject.getMapAdministrators().size();
				mapProject.getMapLeads().size();
				mapProject.getMapSpecialists().size();
				mapProject.getMapPrinciples().size();
				mapProject.getPresetAgeRanges().size();
			}
			mapProjects.sortBy(new Comparator<MapProject>() {
				@Override
				public int compare(MapProject o1, MapProject o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			mappingService.close();
			return mapProjects;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	
	/////////////////////////////////////////////////////
	// SCRUD functions:  Map Users
	/////////////////////////////////////////////////////

	/**
	 * Returns all map leads in either JSON or XML format
	 * 
	 * @return the map leads
	 */
	@GET
	@Path("/user/users")
	@ApiOperation(value = "Get all mapping users", notes = "Returns all MapUsers in either JSON or XML format", response = MapUserListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapUserListJpa getMapUsers(
		@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /user/users");

		try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve the map users.").build());
			
			MappingService mappingService = new MappingServiceJpa();
			MapUserListJpa mapLeads = (MapUserListJpa) mappingService
					.getMapUsers();
			mapLeads.sortBy(new Comparator<MapUser>() {
				@Override
				public int compare(MapUser o1, MapUser o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			mappingService.close();
			return mapLeads;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Returns the user for a given id (auto-generated) in JSON format
	 * 
	 * @param mapUserId
	 *            the mapUserId
	 * @return the mapUser
	 */
	@GET
	@Path("/user/id/{username}")
	@ApiOperation(value = "Get user by username", notes = "Returns a MapUser given a user name in either JSON or XML format", response = MapUser.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapUser getMapUser(
			@ApiParam(value = "Username of MapUser to fetch", required = true) @PathParam("username") String mapUserName,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): user/id/" + mapUserName);
	
		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve a map user.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapUser mapUser = mappingService.getMapUser(mapUserName);
			mappingService.close();
			return mapUser;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	

	/**
	 * Adds a map user
	 * 
	 * @param mapUser
	 *            the map user
	 * @return Response the response
	 */
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/user/add")
	@ApiOperation(value = "Add a user", notes = "Adds a MapUser", response = MapUserJpa.class)
	public MapUser addMapUser(
			@ApiParam(value = "The map user to add. Must be in Json or Xml format", required = true) MapUserJpa mapUser,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /user/add");

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to add a user.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			mappingService.addMapUser(mapUser);
			mappingService.close();
			return null;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}
	
	/**
	 * Updates a map user
	 * 
	 * @param mapUser
	 *            the map user to be added
	 * @return Response the response
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/user/update")
	@ApiOperation(value = "Update a user", notes = "Updates a map user", response = MapUserJpa.class)
	public void updateMapUser(
			@ApiParam(value = "The map user to update.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapUserJpa mapUser,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /user/update");
	
		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to update a user.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapUser(mapUser);
			mappingService.close();
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Removes a map user
	 * 
	 * @param mapUserId
	 *            the map user object to delete
	 * @return Response the response
	 */
	@DELETE
	@Path("/user/delete")
	@ApiOperation(value = "Remove a user", notes = "Removes a map user", response = MapUser.class)
	public void removeMapUser(
			@ApiParam(value = "The map user object to delete", required = true) MapUserJpa mapUser,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /user/delete for user " + mapUser.getName());

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to remove a user.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapUser(mapUser.getId());
			mappingService.close();
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/////////////////////////////////////////////////////
	// SCRUD functions:  Map Advice, to be added later
	/////////////////////////////////////////////////////
	
	/////////////////////////////////////////////////////
	// SCRUD functions:  Map Relation
	/////////////////////////////////////////////////////

	/**
	 * Returns all map relations in either JSON or XML format
	 * 
	 * @return the map relations
	 */
	@GET
	@Path("/relation/relations")
	@ApiOperation(value = "Get all relations", notes = "Returns all MapRelations in either JSON or XML format", response = MapRelationListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRelationListJpa getMapRelations(
		@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /relation/relations");

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to return the map relations.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapRelationListJpa mapRelations = (MapRelationListJpa) mappingService
					.getMapRelations();
			mapRelations.sortBy(new Comparator<MapRelation>() {
				@Override
				public int compare(MapRelation o1, MapRelation o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			mappingService.close();
			return mapRelations;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/////////////////////////////////////////////////////
	// SCRUD functions:  Map Principles
	/////////////////////////////////////////////////////
	
	/**
	 * Returns the map principle for id.
	 * 
	 * @param mapPrincipleId
	 *            the map principle id
	 * @return the map principle for id
	 */
	@GET
	@Path("/principle/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Get principle", notes = "Returns a MapPrinciple given a principle id in either JSON or XML format", response = MapPrinciple.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapPrinciple getMapPrinciple(
			@ApiParam(value = "Id of map principle to fetch", required = true) @PathParam("id") Long mapPrincipleId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /principle/id/"
						+ mapPrincipleId.toString());

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to return the map principle.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapPrinciple mapPrinciple = mappingService
					.getMapPrinciple(mapPrincipleId);
			mappingService.close();
			return mapPrinciple;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Adds a map user preferences object
	 * 
	 * @param mapPrinciple
	 *            the map user preferences object to be added
	 * @return result the newly created map user preferences object
	 */
	@PUT
	@Path("/principle/add")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Add a map principle object", notes = "Adds a MapPrinciple", response = MapPrincipleJpa.class)
	public MapPrinciple addMapPrinciple(
			@ApiParam(value = "The map principle object to add. Must be in Json or XML format", required = true) MapPrincipleJpa mapPrinciple,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /principle/add");

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to add a map principle.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapPrinciple result = mappingService
					.addMapPrinciple(mapPrinciple);
			mappingService.close();
			return result;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

	/**
	 * Updates a map principle
	 * 
	 * @param mapPrinciple
	 * @return the response
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/principle/update")
	@ApiOperation(value = "Update principle", notes = "Updates a MapPrinciple. Must exist in mapping database. Must be in Json or Xml format", response = MapPrincipleJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public void updateMapPrinciple(
			@ApiParam(value = "Map Principle to update", required = true) MapPrincipleJpa mapPrinciple,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /principle/update");

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to update a map principle.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapPrinciple(mapPrinciple);
			mappingService.close();

		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Removes a set of map user preferences
	 * @param principleId
	 *            the id of the map user preferences object to be deleted
	 * @return Response the response
	 */
	@DELETE
	@Path("/principle/remove")
	@ApiOperation(value = "Remove map principle", notes = "Removes a map principle")
	public void removeMapPrinciple(
			@ApiParam(value = "Map user preferences object to remove", required = true) MapPrincipleJpa principle,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /principle/remove for id "
						+ principle.getId().toString());

		try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to remove a map principle.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapPrinciple(principle.getId());
			mappingService.close();
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/////////////////////////////////////////////////////
	// SCRUD functions:  Map User Preferences
	/////////////////////////////////////////////////////

	/**
	 * Gets a map user preferences object for a specified user
	 * 
	 * @param userName
	 * @return result the newly created map user preferences object
	 */
	@GET
	@Path("/userPreferences/user/id/{userName}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Gets a user preferences object", notes = "Gets a MapUserPreferences object for a given userName", response = MapUserPreferencesJpa.class)
	public MapUserPreferences getMapUserPreferences(
			@ApiParam(value = "The map user's user name", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call:  /userPreferences/user/id/" + userName);
		
		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to return the map user preferences.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapUserPreferences result = mappingService
					.getMapUserPreferences(userName);
			mappingService.close();
			return result;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Adds a map user preferences object
	 * 
	 * @param mapUserPreferences
	 *            the map user preferences object to be added
	 * @return result the newly created map user preferences object
	 */
	@PUT
	@Path("/userPreferences/add")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Add a user preferences object", notes = "Adds a MapUserPreferences", response = MapUserPreferencesJpa.class)
	public MapUserPreferences addMapUserPreferences(
			@ApiParam(value = "The map user preferences object to add. Must be in Json or XML format", required = true) MapUserPreferencesJpa mapUserPreferences,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /userPreferences/add");

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to add map user preferences.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapUserPreferences result = mappingService
					.addMapUserPreferences(mapUserPreferences);
			mappingService.close();
			return result;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

	/**
	 * Updates a map user preferences object.
	 * 
	 * @param mapUserPreferences
	 *            the map user preferences
	 * @return null
	 */
	@POST
	@Path("/userPreferences/update")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Update user preferences", notes = "Updates a set of map user preferences", response = MapUserPreferencesJpa.class)
	public void updateMapUserPreferences(
			@ApiParam(value = "The map user preferences to update.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapUserPreferencesJpa mapUserPreferences,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /userPreferences/update");

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to update map user preferences.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapUserPreferences(mapUserPreferences);
			mappingService.close();

		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}
	
	/**
	 * Removes a set of map user preferences
	 * @param mapUserPreferences
	 *            the id of the map user preferences object to be deleted
	 */
	@DELETE
	@Path("/userPreferences/remove")
	@ApiOperation(value = "Remove user preferences", notes = "Removes a set of map user preferences")
	public void removeMapUserPreferences(
			@ApiParam(value = "Map user preferences object to remove", required = true) MapUserPreferencesJpa mapUserPreferences,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /userPreferences/remove for id "
						+ mapUserPreferences.getId().toString());

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to remove map user preferences.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapUserPreferences(mapUserPreferences.getId());
			mappingService.close();
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/////////////////////////////////////////////////////
	// SCRUD functions:  Map Record
	/////////////////////////////////////////////////////

	/**
	 * Returns the record for a given id (auto-generated) in JSON format
	 * 
	 * @param mapRecordId
	 *            the mapRecordId
	 * @return the mapRecord
	 */
	@GET
	@Path("/record/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Get record by id", notes = "Returns a MapRecord given a record id in either JSON or XML format", response = MapRecord.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRecord getMapRecord(
			@ApiParam(value = "Id of map record to fetch", required = true) @PathParam("id") Long mapRecordId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/id/" + mapRecordId.toString());


		try {
  		
			MappingService mappingService = new MappingServiceJpa();
			MapRecord mapRecord = mappingService.getMapRecord(mapRecordId);
			mappingService.close();
			
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve the map record.").build());
			
			return mapRecord;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Adds a map record
	 * 
	 * @param mapRecord
	 *            the map record to be added
	 * @return Response the response
	 */
	@PUT
	@Path("/record/add")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Add a record", notes = "Adds a MapRecord", response = MapRecordJpa.class)
	public MapRecord addMapRecord(
			@ApiParam(value = "The map record to add. Must be in Json or XML format", required = true) MapRecordJpa mapRecord,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/add");

		try {
  		// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to add a map record.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapRecord result = mappingService.addMapRecord(mapRecord);
			mappingService.close();
			return result;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Updates a map record
	 * 
	 * @param mapRecord
	 *            the map record to be added
	 */
	@POST
	@Path("/record/update")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Update a record", notes = "Updates a map record", response = Response.class)
	public void updateMapRecord(
			@ApiParam(value = "The map record to update.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapRecordJpa mapRecord,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/update");

		try {
  		// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to update the map record.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapRecord(mapRecord);
			mappingService.close();
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Removes a map record given the object
	 * 
	 * @param mapRecord
	 *            the map record to delete
	 * @return Response the response
	 */
	@DELETE
	@Path("/record/delete")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Remove a record", notes = "Removes a map record", response = MapRecordJpa.class)
	public Response removeMapRecord(
			@ApiParam(value = "Map Record object to delete", required = true) MapRecordJpa mapRecord,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/delete with map record id = "
						+ mapRecord.toString());

		try {
  		// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to delete the map record.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapRecord(mapRecord.getId());
			mappingService.close();
			return null;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Returns the records for a given concept id.
	 * We don't need to know terminology or version here
	 * because we can get it from the corresponding map project.
	 * 
	 * @param conceptId
	 *            the concept id
	 * @return the mapRecords
	 */
	@GET
	@Path("/record/concept/id/{terminologyId}")
	@ApiOperation(value = "Get records by concept id", notes = "Returns MapRecords given a concept id in either JSON or XML format", response = MapRecord.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRecordListJpa getMapRecordsForTerminologyId(
			@ApiParam(value = "Concept id of map record to fetch", required = true) @PathParam("terminologyId") String conceptId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/concept/id/" + conceptId);


		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to find records by the given concept id.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
					.getMapRecordsForConcept(conceptId);
			mappingService.close();
			return mapRecordList;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Returns delimited page of Published or Ready For Publication MapRecords
	 * given a paging/filtering/sorting parameters object
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @param pfsParameter
	 *            the JSON object containing the paging/filtering/sorting
	 *            parameters
	 * @return the list of map records
	 */
	@POST
	@Path("/record/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Get paged records by project id", notes = "Returns delimited page of published or ready-for-publicatoin MapRecords given a paging/filtering/sorting parameters object", response = MapRecordListJpa.class)
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@CookieParam(value = "userInfo")
	public MapRecordListJpa getMapRecordsForMapProject(
			@ApiParam(value = "Project id associated with map records", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/project/id/"
						+ mapProjectId.toString() + " with PfsParameter: "
						+ "\n" + "     Index/Results = "
						+ Integer.toString(pfsParameter.getStartIndex()) + "/"
						+ Integer.toString(pfsParameter.getMaxResults()) + "\n"
						+ "     Sort field    = " + pfsParameter.getSortField()
						+ "     Filter String = "
						+ pfsParameter.getQueryRestriction());


		// execute the service call
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve the map records for a map project.").build());
			
			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
					.getPublishedAndReadyForPublicationMapRecordsForMapProject(mapProjectId, pfsParameter);
			mappingService.close();
			return mapRecordList;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

	/**
	 * Returns the map record revisions.
	 * 
	 * @param mapRecordId
	 *            the map record id
	 * @return the map record revisions
	 */
	@GET
	@Path("/record/id/{id:[0-9][0-9]*}/revisions")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Get record revision history", notes = "Returns a map record's previous versions from the audit trail", response = MapRecordListJpa.class)
	public MapRecordListJpa getMapRecordRevisions(
			@ApiParam(value = "Id of map record to get revisions for", required = true) @PathParam("id") Long mapRecordId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/id/" + mapRecordId + "/revisions");

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve the map record revisions.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa revisions = (MapRecordListJpa) mappingService
					.getMapRecordRevisions(mapRecordId);
			mappingService.close();
			return revisions;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}
	
	////////////////////////////////////////////
	// Relation and Advice Computation
	///////////////////////////////////////////
	
	/**
	 * Computes a map relation (if any) for a map entry's current state
	 * 
	 * @param mapEntry
	 *            the map entry
	 * @return Response the response
	 */
	@POST
	@Path("/relation/compute")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Compute map relation", notes = "Computes a map relation given the current state of a map entry", response = MapRelationJpa.class)
	public MapRelation computeMapRelation(
			@ApiParam(value = "", required = true) MapEntryJpa mapEntry,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /relation/compute");


		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to compute the map relation.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapRecord mapRecord = mapEntry.getMapRecord();

			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService.getProjectSpecificAlgorithmHandler(mappingService.getMapProject(mapRecord
					.getMapProjectId()));

			MapRelation mapRelation = algorithmHandler.computeMapRelation(mapRecord, mapEntry);
			mappingService.close();
			return mapRelation;
			
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Computes a map advice (if any) for a map entry's current state
	 * 
	 * @param mapEntry
	 *            the map entry
	 * @return Response the response
	 */
	@POST
	@Path("/advice/compute")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Compute map advice", notes = "Computes a map advice given the current state of a map entry", response = MapAdviceJpa.class)
	public MapAdviceList computeMapAdvice(
			@ApiParam(value = "", required = true) MapEntryJpa mapEntry,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// call log
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /advice/compute");

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to compute the map advice.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapRecord mapRecord = mapEntry.getMapRecord();

			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService.getProjectSpecificAlgorithmHandler(mappingService.getMapProject(mapRecord
					.getMapProjectId()));

			MapAdviceList mapAdviceList = algorithmHandler.computeMapAdvice(mapRecord, mapEntry);
			mappingService.close();
			return mapAdviceList;
			
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/////////////////////////////////////////////////
	// Role Management Services
	/////////////////////////////////////////////////
	/**
	 * Gets a map user's role for a given map project
	 * 
	 * @param userName
	 * @param mapProjectId
	 * @return result the role
	 */
	@GET
	@Path("/userRole/user/id/{userName}/project/id/{id:[0-9][0-9]*}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Gets the role.", notes = "Gets the role for the given userName and projectId", response = SearchResultList.class)
	public MapUserRole getMapUserRoleForMapProject(
			@ApiParam(value = "The map user's user name", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call:  /userRole/user/id" + userName + "/project/id/" + mapProjectId);
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapUserRole mapUserRole = mappingService
					.getMapUserRoleForMapProject(userName, mapProjectId);
			mappingService.close();
			return mapUserRole;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	// /////////////////////////
	// Descendant services
	// /////////////////////////

	/**
	 * Given concept information, returns a ConceptList of descendant concepts
	 * without associated map records
	 * 
	 * @param terminologyId
	 *            the concept terminology id
	 * @param terminology
	 *            the concept terminology
	 * @param terminologyVersion
	 *            the concept terminology version
	 * @param threshold
	 *            the maximum number of descendants before a concept is no
	 *            longer considered a low-level concept, and will return an
	 *            empty list
	 * @return the ConceptList of unmapped descendants
	 */
	@GET
	@Path("/concept/id/{terminology}/{version}/{id}/unmappedDescendants/threshold/{threshold:[0-9][0-9]*}")
	@ApiOperation(value = "Find unmapped descendants", notes = "Returns a concept's unmapped descendants given a concept id, terminology, terminology version, and low-level concept threshold", response = Concept.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList getUnmappedDescendantsForConcept(
			@ApiParam(value = "concept terminology id", required = true) @PathParam("id") String terminologyId,
			@ApiParam(value = "concept terminology", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "concept terminology version", required = true) @PathParam("version") String terminologyVersion,
			@ApiParam(value = "threshold max number of descendants for a low-level concept", required = true) @PathParam("threshold") int threshold,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /concept/id/" + terminology + "/"
						+ terminologyVersion + "/" + terminologyId
						+ "/unmappedDescendants/threshold/" + threshold);

		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve unmapped descendants for a concept.").build());
  		
			MappingService mappingService = new MappingServiceJpa();

			SearchResultList results = mappingService
					.findUnmappedDescendantsForConcept(terminologyId,
							terminology, terminologyVersion, threshold, null);

			mappingService.close();
			return results;

		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	
	///////////////////////////////////////////////////////
	// Tree Position Routines for Terminology Browser
	///////////////////////////////////////////////////////
	/**
	 * Gets tree positions for concept.
	 * 
	 * @param terminologyId
	 *            the terminology id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @param mapProjectId
	 *            the contextual project of this tree, used for determining
	 *            valid codes
	 * @return the search result list
	 */
	@GET
	@Path("/treePosition/project/id/{projectId}/concept/id/{terminology}/{terminologyVersion}/{terminologyId}")
	@ApiOperation(value = "Get concept's local tree", notes = "Returns a tree structure representing the position of a concept in a terminology and its children", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public TreePositionListJpa getTreePositionsWithDescendants(
			@ApiParam(value = "terminology id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "terminology of concept", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "terminology version of concept", required = true) @PathParam("terminologyVersion") String terminologyVersion,
			@ApiParam(value = "id of map project this tree will be displayed for", required = true) @PathParam("projectId") Long mapProjectId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken

	) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /treePosition/project/id/"
						+ mapProjectId.toString() + "/concept/id/" + terminology
						+ "/" + terminologyVersion + "/" + terminologyId);


		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to get the tree positions with descendants.").build());
			
			// get the local tree positions from content service
			ContentService contentService = new ContentServiceJpa();
			List<TreePosition> treePositions = contentService.getTreePositionsWithDescendants(
					terminologyId, terminology, terminologyVersion)
					.getTreePositions();
			contentService.close();

			// set the valid codes using mapping service
			MappingService mappingService = new MappingServiceJpa();
			mappingService.setTreePositionValidCodes(treePositions,
					mapProjectId);
			mappingService.setTreePositionTerminologyNotes(treePositions, mapProjectId);
			mappingService.close();

			// construct and return the tree position list object
			TreePositionListJpa treePositionList = new TreePositionListJpa();
			treePositionList.setTreePositions(treePositions);
			return treePositionList;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		} 
	}

	/**
	 * Gets the root-level tree positions for a given terminology and version
	 * 
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @param mapProjectId
	 *            the map project id
	 * @return the search result list
	 */
	@GET
	@Path("/treePosition/project/id/{projectId}/terminology/id/{terminology}/{terminologyVersion}")
	@ApiOperation(value = "Get top-level trees", notes = "Returns a tree structure with an artificial root node and children representing the top-level concepts of a terminology", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public TreePositionListJpa getRootTreePositionsForTerminology(
			@ApiParam(value = "terminology of concept", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "terminology version of concept", required = true) @PathParam("terminologyVersion") String terminologyVersion,
			@ApiParam(value = "id of map project this tree will be displayed for", required = true) @PathParam("projectId") Long mapProjectId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /treePosition/project/id/"
						+ mapProjectId.toString() + "/terminology/id/" + terminology
						+ "/" + terminologyVersion);


		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to get the root tree positions for a terminology.").build());
			
			// get the root tree positions from content service
			ContentService contentService = new ContentServiceJpa();
			List<TreePosition> treePositions = contentService
					.getRootTreePositions(terminology,
							terminologyVersion).getTreePositions();
			contentService.close();

			// set the valid codes using mapping service
			MappingService mappingService = new MappingServiceJpa();
			mappingService.setTreePositionValidCodes(treePositions,
					mapProjectId);
			mappingService.close();

			// construct and return the tree position list object
			TreePositionListJpa treePositionList = new TreePositionListJpa();
			treePositionList.setTreePositions(treePositions);
			return treePositionList;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Gets tree positions for concept query.
	 * 
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @param query
	 *            the query
	 * @param mapProjectId
	 *            the map project id
	 * @return the root-level trees corresponding to the query
	 */
	@GET
	@Path("/treePosition/project/id/{projectId}/terminology/id/{terminology}/{terminologyVersion}/query/{query}")
	@ApiOperation(value = "Get tree positions for query", notes = "Returns tree structures representing results a given terminology, terminology version, and query ", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public TreePositionListJpa getTreePositionGraphsForQuery(
			@ApiParam(value = "terminology of concept", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "terminology version of concept", required = true) @PathParam("terminologyVersion") String terminologyVersion,
			@ApiParam(value = "paging/filtering/sorting object", required = true) @PathParam("query") String query,
			@ApiParam(value = "id of map project this tree will be displayed for", required = true) @PathParam("projectId") Long mapProjectId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(ContentServiceJpa.class).info(
				"RESTful call (Mapping): /treePosition/project/id/" + mapProjectId
				+ "/terminology/id/" + terminology + "/"
						+ terminologyVersion + "/query/" + query);


		try {
			// authorize call	
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to get the tree position graphs for a query.").build());
			
			// get the tree positions from concept service
			ContentService contentService = new ContentServiceJpa();
			List<TreePosition> treePositions = contentService
					.getTreePositionGraphForQuery(terminology,
							terminologyVersion, query).getTreePositions();
			contentService.close();

			// set the valid codes using mapping service
			MappingService mappingService = new MappingServiceJpa();
			mappingService.setTreePositionValidCodes(treePositions,
					mapProjectId);
			mappingService.setTreePositionTerminologyNotes(treePositions, mapProjectId);
			mappingService.close();

			// construct and return the tree position list object
			TreePositionListJpa treePositionList = new TreePositionListJpa();
			treePositionList.setTreePositions(treePositions);
			return treePositionList;

		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	////////////////////////////////////////////////////
	// Workflow-related routines
	///////////////////////////////////////////////////
	
	
	/**
	 * Returns records recently edited for a project and user.
	 * Used by editedList widget.
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @param userName
	 *            the user name
	 * @param pfsParameter
	 *            the pfs parameter
	 * @return the recently edited map records
	 */
	@POST
	@Path("/record/project/id/{id}/user/id/{userName}/edited")
	@ApiOperation(value = "Get user's edited map records", notes = "Returns a paged list of records edited by a user, in reverse chronological order (most recent first)", response = MapRecordListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRecordListJpa getMapRecordsEditedByMapUser(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") String mapProjectId,
			@ApiParam(value = "User name", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/project/id/" + mapProjectId + "/user/id" + userName + "/edited");


		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, new Long(mapProjectId));
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to get the recently edited map records.").build());
			
			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa recordList = (MapRecordListJpa) mappingService
					.getRecentlyEditedMapRecords(new Long(mapProjectId),
							userName, pfsParameter);
			mappingService.close();
			return recordList;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Returns the map records that when compared
	 * lead to the specified conflict record.
	 * 
	 * @param mapRecordId
	 * @return map records in conflict for a given conflict lead record
	 * @throws Exception
	 */
	@GET
	@Path("/record/id/{id:[0-9][0-9]*}/conflictOrigins")
	@ApiOperation(value = "Get specialist records for assigned conflict", notes = "Return's a list of records in conflict for a lead's conflict resolution record", response = MapRecordListJpa.class)
	public MapRecordList getOriginMapRecordsForConflict(
			@ApiParam(value = "id of the map lead's conflict-in-progress record", required = true) @PathParam("id") Long mapRecordId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
			throws Exception {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/id/" + mapRecordId + "/conflictOrigins");
		// authorize call
		MapUserRole role = securityService.getApplicationRoleForToken(authToken);
		if (!role.hasPrivilegesOf(MapUserRole.LEAD))
			throw new WebApplicationException(Response.status(401).entity(
					"User does not have permissions to retrieve the origin map records for a conflict.").build());
		
		MapRecordList records = new MapRecordListJpa();

		MappingService mappingService = new MappingServiceJpa();
		records = mappingService.getOriginMapRecordsForConflict(mapRecordId);
		mappingService.close();

		return records;
	}
	
	////////////////////////////////////////////////
	// Map Record Validation and Compare Services
	////////////////////////////////////////////////
	
	/**
	 * Validates a map record.
	 * 
	 * @param mapRecord the map record to be validated
	 * @return Response the response
	 */
	@POST
	@Path("/validation/record/validate")
	@Consumes({
		MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	@Produces({
		MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	@ApiOperation(value = "Validates a map record", notes = "Performs validation checks on a map record", response = MapRecordJpa.class)
	public ValidationResult validateMapRecord(
			@ApiParam(value = "The map record to validate.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapRecordJpa mapRecord,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /validation/record/validate for map record id = " + mapRecord.getId().toString());


		// get the map project for this record

		try {
  		// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to validate a map record.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject;
			mapProject = mappingService.getMapProject(mapRecord
					.getMapProjectId());
			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService
					.getProjectSpecificAlgorithmHandler(mapProject);

			ValidationResult validationResult = algorithmHandler
					.validateRecord(mapRecord);
			mappingService.close();
			return validationResult;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}


	/**
	 * Compare map records and return differences
	 * 
	 * @param mapRecordId1
	 *            the map record id1
	 * @param mapRecordId2
	 *            the map record id2
	 * @return the validation result
	 */
	@GET
	@Path("/validation/record/id/{recordId1}/record/id/{recordId2}/compare")
	@ApiOperation(value = "Get the root tree (top-level concepts) for a given terminology", notes = "Returns a tree structure with an artificial root node and children representing the top-level concepts of a terminology", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public ValidationResult compareMapRecords(
			@ApiParam(value = "id of first map record", required = true) @PathParam("recordId1") Long mapRecordId1,
			@ApiParam(value = "id of second map record", required = true) @PathParam("recordId2") Long mapRecordId2,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /validation/record/id/" + mapRecordId1 + "record/id/" + mapRecordId1 + "/compare");

		
		try {
  		
			MappingService mappingService = new MappingServiceJpa();
			MapRecord mapRecord1, mapRecord2;
			
			mapRecord1 = mappingService.getMapRecord(mapRecordId1);
			mapRecord2 = mappingService.getMapRecord(mapRecordId2);

			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord1.getMapProjectId());
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to compare map records.").build());

			MapProject mapProject = mappingService.getMapProject(mapRecord1
					.getMapProjectId());
			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService
					.getProjectSpecificAlgorithmHandler(mapProject);
			ValidationResult validationResult = algorithmHandler.compareMapRecords(
					mapRecord1, mapRecord2);

			mappingService.close();
			return validationResult;

		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}


}
