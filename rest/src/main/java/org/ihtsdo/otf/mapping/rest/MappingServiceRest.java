package org.ihtsdo.otf.mapping.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.MapAgeRangeListJpa;
import org.ihtsdo.otf.mapping.helpers.MapPrincipleListJpa;
import org.ihtsdo.otf.mapping.helpers.MapProjectListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRelationListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.TreePositionListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
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
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * The Mapping Services REST package
 */
@Path("/mapping")
@Api(value = "/mapping", description = "Operations supporting Map objects.")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class MappingServiceRest extends RootServiceRest {

	private SecurityService securityService;

	/**
	 * Instantiates an empty {@link MappingServiceRest}.
	 */
	public MappingServiceRest() throws Exception {
		securityService = new SecurityServiceJpa();
	}

	// ///////////////////////////////////////////////////
	// SCRUD functions: Map Projects
	// ///////////////////////////////////////////////////

	/**
	 * Returns all map projects in either JSON or XML format
	 * 
	 * @param authToken
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
		String user = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve map projects.")
								.build());

			MappingService mappingService = new MappingServiceJpa();

			MapProjectListJpa mapProjects = (MapProjectListJpa) mappingService
					.getMapProjects();

			mapProjects.sortBy(new Comparator<MapProject>() {
				@Override
				public int compare(MapProject o1, MapProject o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			mappingService.close();
			return mapProjects;

		} catch (Exception e) {
			handleException(e, "trying to retrieve map projects", user, "", "");
			return null;
		}
	}

	/**
	 * Returns the project for a given id (auto-generated) in JSON format
	 * 
	 * @param mapProjectId
	 *            the mapProjectId
	 * @param authToken
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

		String user = "";		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve the map project.")
								.build());

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

		} catch (Exception e) {
			handleException(e, "trying to retrieve the map project", user, "", "");
			return null;
		}
	}

	/**
	 * Adds a map project
	 * 
	 * @param mapProject
	 *            the map project to be added
	 * @param authToken
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

		String user = "";
		String project = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to add a map project.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapProject mp = mappingService.addMapProject(mapProject);
			project = mp.getName();		
			mappingService.close();

			return mp;

		} catch (Exception e) {
			handleException(e, "trying to add a map project", user, project, "");
			return null;
		}
	}

	/**
	 * Updates a map project
	 * 
	 * @param mapProject
	 *            the map project to be added
	 * @param authToken
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
		
		String user = "";

		try {
			// authorize call
			// IF application role = admin
			// OR map project role = lead

			MapUserRole roleProject = securityService
					.getMapProjectRoleForToken(authToken, mapProject.getId());
			MapUserRole roleApplication = securityService
					.getApplicationRoleForToken(authToken);

			if (!(roleApplication.hasPrivilegesOf(MapUserRole.ADMINISTRATOR) || roleProject
					.hasPrivilegesOf(MapUserRole.LEAD)))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to udpate a map project.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapProject(mapProject);
			mappingService.close();

		} catch (Exception e) {
			handleException(e, "trying to update a map project", user, mapProject.getName(), "");
		}
	}

	/**
	 * Removes a map project.
	 * 
	 * @param mapProject
	 * @param authToken
	 */
	@DELETE
	@Path("/project/delete")
	@ApiOperation(value = "Remove a project", notes = "Removes a map project", response = MapProject.class)
	public void removeMapProject(
			@ApiParam(value = "Map project object to delete", required = true) MapProjectJpa mapProject,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/delete for "
						+ mapProject.getName());

		String user = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to remove a map project.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapProject(mapProject.getId());
			mappingService.close();

		} catch (Exception e) {
			handleException(e, "trying to remove a map project", user, mapProject.getName(), "");
		}
	}

	/**
	 * Returns all map projects for a lucene query
	 * 
	 * @param query
	 *            the string query
	 * @param authToken
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
		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to find map projects.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			SearchResultList searchResultList = mappingService
					.findMapProjectsForQuery(query, new PfsParameterJpa());
			mappingService.close();
			return searchResultList;

		} catch (Exception e) {
			handleException(e, "trying to find map projects", user, "", "");
			return null;
		}
	}

	/**
	 * Returns all map projects for a map user
	 * 
	 * @param mapUserName
	 *            the map user name
	 * @param authToken
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
		String user = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to get the map projects for given user.")
								.build());

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

		} catch (Exception e) {
			handleException(e, "trying to get the map projects for a given user", user, "", "");
			return null;
		}
	}

	// ///////////////////////////////////////////////////
	// SCRUD functions: Map Users
	// ///////////////////////////////////////////////////

	/**
	 * Returns all map leads in either JSON or XML format
	 * 
	 * @param authToken
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
		String user = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve the map users.")
								.build());

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

		} catch (Exception e) {
			handleException(e, "trying to retrieve a concept", user, "", "");
			return null;
		}
	}

	/**
	 * Returns the user for a given id (auto-generated) in JSON format
	 * 
	 * @param mapUserName
	 * @param authToken
	 * 
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
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve a map user.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapUser mapUser = mappingService.getMapUser(mapUserName);
			mappingService.close();
			return mapUser;

		} catch (Exception e) {
			handleException(e, "trying to retrieve a map user", mapUserName, "", "");
			return null;
		}
	}

	/**
	 * Adds a map user
	 * 
	 * @param mapUser
	 *            the map user
	 * @param authToken
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
		String user = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to add a user.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.addMapUser(mapUser);
			mappingService.close();
			return null;

		} catch (Exception e) {
			handleException(e, "trying to add a user", user, "", "");
			return null;
		}
	}

	/**
	 * Updates a map user
	 * 
	 * @param mapUser
	 *            the map user to be added
	 * @param authToken
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

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to update a user.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapUser(mapUser);
			mappingService.close();
		} catch (Exception e) {
			handleException(e, "trying to update a user", user, "", "");
		}
	}

	/**
	 * Removes a map user
	 * 
	 * @param mapUser
	 * @param authToken
	 */
	@DELETE
	@Path("/user/delete")
	@ApiOperation(value = "Remove a user", notes = "Removes a map user", response = MapUser.class)
	public void removeMapUser(
			@ApiParam(value = "The map user object to delete", required = true) MapUserJpa mapUser,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /user/delete for user "
						+ mapUser.getName());

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to remove a user.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapUser(mapUser.getId());
			mappingService.close();
		} catch (Exception e) {
			handleException(e, "trying to remove a user", user, "", "");
		}
	}

	// ///////////////////////////////////////////////////
	// SCRUD functions: Map Advice
	// ///////////////////////////////////////////////////
	/**
	 * Returns all map advices in either JSON or XML format
	 * 
	 * @param authToken
	 * 
	 * @return the map advices
	 */
	@GET
	@Path("/advice/advices")
	@ApiOperation(value = "Get all mapping advices", notes = "Returns all MapAdvices in either JSON or XML format", response = MapAdviceListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapAdviceListJpa getMapAdvices(
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /advice/advices");

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve the map advices.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapAdviceListJpa mapAdvices = (MapAdviceListJpa) mappingService
					.getMapAdvices();
			mapAdvices.sortBy(new Comparator<MapAdvice>() {
				@Override
				public int compare(MapAdvice o1, MapAdvice o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			mappingService.close();
			return mapAdvices;

		} catch (Exception e) {
			handleException(e, "trying to retrieve an advice", user, "", "");
			return null;
		}
	}

	/**
	 * Adds a map advice
	 * 
	 * @param mapAdvice
	 *            the map advice
	 * @param authToken
	 * @return Response the response
	 */
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/advice/add")
	@ApiOperation(value = "Add an advice", notes = "Adds a MapAdvice", response = MapAdviceJpa.class)
	public MapUser addMapAdvice(
			@ApiParam(value = "The map advice to add. Must be in Json or Xml format", required = true) MapAdviceJpa mapAdvice,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /advice/add");

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to add an advice.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.addMapAdvice(mapAdvice);
			mappingService.close();
			return null;

		} catch (Exception e) {
			handleException(e, "trying to add an advice", user, "", "");
			return null;
		}
	}

	/**
	 * Updates a map advice
	 * 
	 * @param mapAdvice
	 *            the map advice to be added
	 * @param authToken
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/advice/update")
	@ApiOperation(value = "Update an advice", notes = "Updates a map advice", response = MapAdviceJpa.class)
	public void updateMapAdvice(
			@ApiParam(value = "The map advice to update.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapAdviceJpa mapAdvice,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /advice/update");

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to update an advice.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapAdvice(mapAdvice);
			mappingService.close();
		} catch (Exception e) {
			handleException(e, "trying to update an advice", user, "", "");
		}
	}

	/**
	 * Removes a map advice
	 * 
	 * @param mapAdvice
	 * @param authToken
	 */
	@DELETE
	@Path("/advice/delete")
	@ApiOperation(value = "Remove an advice", notes = "Removes a map advice", response = MapAdviceJpa.class)
	public void removeMapAdvice(
			@ApiParam(value = "The map advice object to delete", required = true) MapAdviceJpa mapAdvice,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /advice/delete for user "
						+ mapAdvice.getName());

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to remove a user.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapAdvice(mapAdvice.getId());
			mappingService.close();
		} catch (Exception e) {
			LocalException le = new LocalException("Unable to delete map advice. This is likely because the advice is being used by a map project or map entry");
			handleException(le, "", user, "", "");
		}
	}

	// ///////////////////////////////////////////////////
	// SCRUD functions: Map AgeRange
	// ///////////////////////////////////////////////////
	/**
	 * Returns all map age ranges in either JSON or XML format
	 * 
	 * @param authToken
	 * 
	 * @return the map age ranges
	 */
	@GET
	@Path("/ageRange/ageRanges")
	@ApiOperation(value = "Get all mapping age ranges", notes = "Returns all MapAgeRanges in either JSON or XML format", response = MapAgeRangeListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapAgeRangeListJpa getMapAgeRanges(
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /ageRange/ageRanges");

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve the map age ranges.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapAgeRangeListJpa mapAgeRanges = (MapAgeRangeListJpa) mappingService
					.getMapAgeRanges();
			mapAgeRanges.sortBy(new Comparator<MapAgeRange>() {
				@Override
				public int compare(MapAgeRange o1, MapAgeRange o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			mappingService.close();
			return mapAgeRanges;

		} catch (Exception e) {
			handleException(e, "trying to retrieve an age range", user, "", "");
			return null;
		}
	}

	/**
	 * Adds a map age range
	 * 
	 * @param mapAgeRange
	 *            the map ageRange
	 * @param authToken
	 * @return Response the response
	 */
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/ageRange/add")
	@ApiOperation(value = "Add an age range", notes = "Adds a MapAgeRange", response = MapAgeRangeJpa.class)
	public MapUser addMapAgeRange(
			@ApiParam(value = "The map ageRange to add. Must be in Json or Xml format", required = true) MapAgeRangeJpa mapAgeRange,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /ageRange/add");

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to add an age range.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.addMapAgeRange(mapAgeRange);
			mappingService.close();
			return null;

		} catch (Exception e) {
			handleException(e, "trying to add an age range", user, "", "");
			return null;
		}
	}

	/**
	 * Updates a map age range
	 * 
	 * @param mapAgeRange
	 *            the map ageRange to be added
	 * @param authToken
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/ageRange/update")
	@ApiOperation(value = "Update an age range", notes = "Updates a map age range", response = MapAgeRangeJpa.class)
	public void updateMapAgeRange(
			@ApiParam(value = "The map age range to update.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapAgeRangeJpa mapAgeRange,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /ageRange/update");

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to update an age range.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapAgeRange(mapAgeRange);
			mappingService.close();
		} catch (Exception e) {
			handleException(e, "trying to update an age range", user, "", "");
		}
	}

	/**
	 * Removes a map age range
	 * 
	 * @param mapAgeRange
	 * @param authToken
	 */
	@DELETE
	@Path("/ageRange/delete")
	@ApiOperation(value = "Remove an age range", notes = "Removes a map age range", response = MapAgeRangeJpa.class)
	public void removeMapAgeRange(
			@ApiParam(value = "The map age range object to delete", required = true) MapAgeRangeJpa mapAgeRange,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /ageRange/delete for user "
						+ mapAgeRange.getName());
		
		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to remove an age range.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapAgeRange(mapAgeRange.getId());
			mappingService.close();
		} catch (Exception e) {
			handleException(e, "trying to remove an age range", user, "", "");
		}
	}

	// ///////////////////////////////////////////////////
	// SCRUD functions: Map Relation
	// ///////////////////////////////////////////////////

	/**
	 * Returns all map relations in either JSON or XML format
	 * 
	 * @param authToken
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
		
		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to return the map relations.")
								.build());

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
		} catch (Exception e) {
			handleException(e, "trying to return the map relations", user, "", "");
			return null;
		}
	}

	/**
	 * Adds a map relation
	 * 
	 * @param mapRelation
	 *            the map relation
	 * @param authToken
	 * @return Response the response
	 */
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/relation/add")
	@ApiOperation(value = "Add an relation", notes = "Adds a MapRelation", response = MapRelationJpa.class)
	public MapUser addMapRelation(
			@ApiParam(value = "The map relation to add. Must be in Json or Xml format", required = true) MapRelationJpa mapRelation,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /relation/add");

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to add a relation.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.addMapRelation(mapRelation);
			mappingService.close();
			return null;

		} catch (Exception e) {
			handleException(e, "trying to add a relation", user, "", "");
			return null;
		}
	}

	/**
	 * Updates a map relation
	 * 
	 * @param mapRelation
	 *            the map relation to be added
	 * @param authToken
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/relation/update")
	@ApiOperation(value = "Update an relation", notes = "Updates a map relation", response = MapRelationJpa.class)
	public void updateMapRelation(
			@ApiParam(value = "The map relation to update.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapRelationJpa mapRelation,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /relation/update");

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to update a relation.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapRelation(mapRelation);
			mappingService.close();
		} catch (Exception e) {
			handleException(e, "trying to update a relation", user, "", "");
		}
	}

	/**
	 * Removes a map relation
	 * 
	 * @param mapRelation
	 * @param authToken
	 */
	@DELETE
	@Path("/relation/delete")
	@ApiOperation(value = "Remove an relation", notes = "Removes a map relation", response = MapRelationJpa.class)
	public void removeMapRelation(
			@ApiParam(value = "The map relation object to delete", required = true) MapRelationJpa mapRelation,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /relation/delete for user "
						+ mapRelation.getName());

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to remove a relation.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapRelation(mapRelation.getId());
			mappingService.close();
		} catch (Exception e) {
			LocalException le = new LocalException("Unable to delete map relation. This is likely because the relation is being used by a map project or map entry");
			handleException(le, "", user, "", "");
		}
	}

	// ///////////////////////////////////////////////////
	// SCRUD functions: Map Principles
	// ///////////////////////////////////////////////////

	/**
	 * Returns all map principles in either JSON or XML format
	 * 
	 * @param authToken
	 * 
	 * @return the map principles
	 */
	@GET
	@Path("/principle/principles")
	@ApiOperation(value = "Get all principles", notes = "Returns all MapPrinciples in either JSON or XML format", response = MapPrincipleListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapPrincipleListJpa getMapPrinciples(
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /principle/principles");

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to return the map principles.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapPrincipleListJpa mapPrinciples = (MapPrincipleListJpa) mappingService
					.getMapPrinciples();
			mapPrinciples.sortBy(new Comparator<MapPrinciple>() {
				@Override
				public int compare(MapPrinciple o1, MapPrinciple o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			mappingService.close();
			return mapPrinciples;
		} catch (Exception e) {
			handleException(e, "trying to return the map principles", user, "", "");
			return null;
		}
	}

	/**
	 * Returns the map principle for id.
	 * 
	 * @param mapPrincipleId
	 *            the map principle id
	 * @param authToken
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

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve the map principle.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapPrinciple mapPrinciple = mappingService
					.getMapPrinciple(mapPrincipleId);
			mappingService.close();
			return mapPrinciple;
		} catch (Exception e) {
			handleException(e, "trying to retrieve the map principle", user, "", "");
			return null;
		}
	}

	/**
	 * Adds a map user principle object
	 * 
	 * @param mapPrinciple
	 *            the map user principle object to be added
	 * @param authToken
	 * @return result the newly created map user principle object
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

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to add a map principle.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapPrinciple result = mappingService.addMapPrinciple(mapPrinciple);
			mappingService.close();
			return result;
		} catch (Exception e) {
			handleException(e, "trying to add a map principle", user, "", "");
			return null;
		}

	}

	/**
	 * Updates a map principle
	 * 
	 * @param mapPrinciple
	 * @param authToken
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

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to update a map principle.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapPrinciple(mapPrinciple);
			mappingService.close();

		} catch (Exception e) {
			handleException(e, "trying to update a map principle", user, "", "");
		}
	}

	/**
	 * Removes a set of map user preferences
	 * 
	 * @param principle
	 * @param authToken
	 */
	@DELETE
	@Path("/principle/delete")
	@ApiOperation(value = "Remove map principle", notes = "Removes a map principle")
	public void removeMapPrinciple(
			@ApiParam(value = "Map user preferences object to remove", required = true) MapPrincipleJpa principle,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /principle/delete for id "
						+ principle.getId().toString());

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to remove a map principle.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapPrinciple(principle.getId());
			mappingService.close();
		} catch (Exception e) {
			LocalException le = new LocalException("Unable to delete map principle. This is likely because the principle is being used by a map project or map record");
			handleException(le, "", user, "", "");
		}
	}

	// ///////////////////////////////////////////////////
	// SCRUD functions: Map User Preferences
	// ///////////////////////////////////////////////////

	/**
	 * Gets a map user preferences object for a specified user
	 * 
	 * @param userName
	 * @param authToken
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

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to return the map user preferences.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapUserPreferences result = mappingService
					.getMapUserPreferences(userName);
			mappingService.close();
			return result;
		} catch (Exception e) {
			handleException(e, "trying to retrieve the map user preferences", user, "", "");
			return null;
		}
	}

	/**
	 * Adds a map user preferences object
	 * 
	 * @param mapUserPreferences
	 *            the map user preferences object to be added
	 * @param authToken
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

		String user = "";
		try {
			// authorize call -- note that any user must be able to add their
			// own preferences, therefore this /add call only requires VIEWER
			// role
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to add map user preferences.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapUserPreferences result = mappingService
					.addMapUserPreferences(mapUserPreferences);
			mappingService.close();
			return result;
		} catch (Exception e) {
			handleException(e, "trying to add map user preferences", user, "", "");
			return null;
		}

	}

	/**
	 * Updates a map user preferences object.
	 * 
	 * @param mapUserPreferences
	 *            the map user preferences
	 * @param authToken
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
				"RESTful call (Mapping): /userPreferences/update with \n"
						+ mapUserPreferences.toString());

		String user = "";
		try {
			// authorize call -- note that any user must be able to update their
			// own preferences, therefore this /update call only requires VIEWER
			// role
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to update map user preferences.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapUserPreferences(mapUserPreferences);
			mappingService.close();

		} catch (Exception e) {
			handleException(e, "trying to update map user preferences", user, "", "");
		}

	}

	/**
	 * Removes a set of map user preferences
	 * 
	 * @param mapUserPreferences
	 *            the id of the map user preferences object to be deleted
	 * @param authToken
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

		String user = "";
		try {
			// authorize call -- note that any user must be able to delete their
			// own preferences, therefore this /remove call only requires VIEWER
			// role
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to remove map user preferences.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapUserPreferences(mapUserPreferences.getId());
			mappingService.close();
		} catch (Exception e) {
			handleException(e, "trying to remove map user preferences", user, "", "");
		}
	}
	

	/////////////////////////////////////////////////////
	// SCRUD functions:  Map Record
	/////////////////////////////////////////////////////

	// ///////////////////////////////////////////////////
	// SCRUD functions: Map Record
	// ///////////////////////////////////////////////////

	/**
	 * Returns the record for a given id (auto-generated) in JSON format
	 * 
	 * @param mapRecordId
	 *            the mapRecordId
	 * @param authToken
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


		String user = "";
		MapRecord mapRecord = null;
		try {
  		
			MappingService mappingService = new MappingServiceJpa();
			mapRecord = mappingService.getMapRecord(mapRecordId);
			mappingService.close();

			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapRecord.getMapProjectId());
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve the map record.")
								.build());

			return mapRecord;
		} catch (Exception e) {
			handleException(e, "trying to retrieve the map record", 
					user, mapRecord.getMapProjectId().toString(), mapRecordId.toString());
			return null;
		}
	}

	/**
	 * Adds a map record
	 * 
	 * @param mapRecord
	 *            the map record to be added
	 * @param authToken
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

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapRecord.getMapProjectId());
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to add a map record.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapRecord result = mappingService.addMapRecord(mapRecord);
			mappingService.close();
			return result;
		} catch (Exception e) {
			handleException(e, "trying to add a map record", user, 
					mapRecord.getMapProjectId().toString(), mapRecord.getId().toString());
			return null;
		}
	}

	/**
	 * Updates a map record
	 * 
	 * @param mapRecord
	 *            the map record to be added
	 * @param authToken
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

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapRecord.getMapProjectId());
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to update the map record.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapRecord(mapRecord);
			mappingService.close();
		} catch (Exception e) {
			handleException(e, "trying to update the map record", 
					user, mapRecord.getMapProjectId().toString(), mapRecord.getId().toString());
		}
	}

	/**
	 * Removes a map record given the object
	 * 
	 * @param mapRecord
	 *            the map record to delete
	 * @param authToken
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

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to delete the map record.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapRecord(mapRecord.getId());
			mappingService.close();
			return null;
		} catch (Exception e) {
			handleException(e, "trying to delete the map record", user, mapRecord.getMapProjectId().toString(), "");
			return null;
		}
	}

	/**
	 * Returns the records for a given concept id. We don't need to know
	 * terminology or version here because we can get it from the corresponding
	 * map project.
	 * 
	 * @param conceptId
	 *            the concept id
	 * @param authToken
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


		String user = "";
		try {
  		// authorize call
			MapUserRole applicationRole = securityService.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			
			
			if (!applicationRole.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to find records by the given concept id.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
					.getMapRecordsForConcept(conceptId);

			// return records that this user does not have permission to see
			MapUser mapUser = mappingService.getMapUser(securityService
					.getUsernameForToken(authToken));
			List<MapRecord> mapRecords = new ArrayList<>();

			// cycle over records and determine if this user can see them
			for (MapRecord mr : mapRecordList.getMapRecords()) {

				// get the user's role for this record's project
				MapUserRole projectRole = securityService
						.getMapProjectRoleForToken(authToken,
								mr.getMapProjectId());

				System.out.println(projectRole + " " + mr.toString());

				switch (mr.getWorkflowStatus()) {

				// any role can see published
				case PUBLISHED:
					mapRecords.add(mr);
					break;

				// only roles above specialist can see ready_for_publication
				case READY_FOR_PUBLICATION:
					if (projectRole.hasPrivilegesOf(MapUserRole.SPECIALIST))
						mapRecords.add(mr);
					break;
				// otherwise
				// - if lead, add record
				// - if specialist, only add record if owned
				default:
					if (projectRole.hasPrivilegesOf(MapUserRole.LEAD))
						mapRecords.add(mr);
					else if (mr.getOwner().equals(mapUser))
						mapRecords.add(mr);
					break;

				}
			}

			// if not a mapping user (specialist or above), remove all notes
			// from records prior to returning
			// TODO: Make this flag on MapUserRole for Application (add a GUEST
			// enum?)
			if (mapUser.getUserName().equals("guest")) {
				for (MapRecord mr : mapRecords) {
					mr.setMapNotes(null);
				}
			}

			// set the list of records to the filtered object and return
			mapRecordList.setMapRecords(mapRecords);

			mappingService.close();
			return mapRecordList;
		} catch (Exception e) {
			handleException(e, "trying to find records by the given concept id", user, "", conceptId);
			return null;
		}
	}

	/**
	 * 
	 * Returns delimited page of Published or Ready For Publication MapRecords
	 * given a paging/filtering/sorting parameters object
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @param pfsParameter
	 *            the JSON object containing the paging/filtering/sorting
	 *            parameters
	 * @param authToken
	 * @return the list of map records
	 */
	@POST
	@Path("/record/project/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Get paged records by project id", notes = "Returns delimited page of published or ready-for-publicatoin MapRecords given a paging/filtering/sorting parameters object", response = MapRecordListJpa.class)
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@CookieParam(value = "userInfo")
	public MapRecordListJpa getPublishedAndReadyForPublicationMapRecordsForMapProject(
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

		String user = "";
		// execute the service call
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapProjectId);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve the publication-ready map records for a map project.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
					.getPublishedAndReadyForPublicationMapRecordsForMapProject(
							mapProjectId, pfsParameter);
			mappingService.close();
			return mapRecordList;
		} catch (Exception e) { 
			handleException(e, "trying to retrieve the map records for a map project", user, mapProjectId.toString(), "");
			return null;
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
	 * @param authToken
	 * @return the list of map records
	 */
	@POST
	@Path("/record/project/id/{id:[0-9][0-9]*}/published")
	@ApiOperation(value = "Get paged records by project id", notes = "Returns delimited page of published or ready-for-publicatoin MapRecords given a paging/filtering/sorting parameters object", response = MapRecordListJpa.class)
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@CookieParam(value = "userInfo")
	public MapRecordListJpa getPublishedMapRecordsForMapProject(
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

		String user = "";
		// execute the service call
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapProjectId);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve the map records for a map project.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
					.getPublishedMapRecordsForMapProject(mapProjectId,
							pfsParameter);
			mappingService.close();
			return mapRecordList;
		} catch (Exception e) { 
			handleException(e, "trying to retrieve the map records for a map project", user, mapProjectId.toString(), "");
			return null;
		}

	}

	/**
	 * Returns the map record revisions.
	 * 
	 * NOTE: currently not called, but we are going to want to call this to do
	 * history-related stuff thus it is anticipating the future dev and should
	 * be kept.
	 * 
	 * @param mapRecordId
	 *            the map record id
	 * @param authToken
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
				"RESTful call (Mapping): /record/id/" + mapRecordId
						+ "/revisions");

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapRecordId);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve the map record revisions.")
								.build());

			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa revisions = (MapRecordListJpa) mappingService
					.getMapRecordRevisions(mapRecordId);
			mappingService.close();
			return revisions;
		} catch (Exception e) {
			handleException(e, "trying to retrieve the map record revisions", user, "", mapRecordId.toString());
			return null;
		}

	}
	
	/**
	 * Returns the map record using historical revisions if the record no longer exists.
	 *
	 * @param mapRecordId the map record id
	 * @param authToken the auth token
	 * @return the map record historical
	 */
	@GET
	@Path("/record/id/{id:[0-9][0-9]*}/historical")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Get record using revision history if necessary", notes = "Returns a map record using previous versions from the audit trail if it no longer exists", response = MapRecordListJpa.class)
	public MapRecord getMapRecordHistorical(
			@ApiParam(value = "Id of map record to get revisions for", required = true) @PathParam("id") Long mapRecordId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/id/" + mapRecordId + "/historical");

		String user = "";
		try {
  		// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve the map record potentially using historical revisions.").build());
  		
			MappingService mappingService = new MappingServiceJpa();
			// try getting the current record
			MapRecord mapRecord = mappingService.getMapRecord(mapRecordId);
			
			// if no current record, look for revisions
			if (mapRecord == null) {
			  mapRecord = mappingService
					.getMapRecordRevisions(mapRecordId).getMapRecords().get(0);
			}
			mappingService.close();
			return mapRecord;
			
		} catch (Exception e) { 
			handleException(e, "trying to retrieve the map record potentially using historical revisions", user, "", mapRecordId.toString());
			return null;
		}

	}
	
	////////////////////////////////////////////
	// Relation and Advice Computation
	// /////////////////////////////////////////

	/**
	 * Computes a map relation (if any) for a map entry's current state
	 * 
	 * @param mapEntry
	 *            the map entry
	 * @param authToken
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

		String user = "";
		MapRecord mapRecord = null;
		try {
  		
			MappingService mappingService = new MappingServiceJpa();
			
            // after deserialization, the entry has a dummy map record with id
            // get the actual record
            mapRecord = mappingService.getMapRecord(mapEntry.getMapRecord().getId());
            Logger.getLogger(MappingServiceRest.class).info(
                "  mapEntry.mapRecord.mapProjectId = " + mapRecord.getMapProjectId());


			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapRecord.getMapProjectId());
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to compute the map relation.")
								.build());

			System.out.println(mapRecord.toString());
			if (mapRecord.getMapProjectId() == null) {
				return null;
			}
			System.out.println("Retrieving project handler");

			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService
					.getProjectSpecificAlgorithmHandler(mappingService
							.getMapProject(mapRecord.getMapProjectId()));

			MapRelation mapRelation = algorithmHandler.computeMapRelation(
					mapRecord, mapEntry);
			mappingService.close();
			return mapRelation;

		} catch (Exception e) {
			handleException(e, "trying to compute the map relations", user, 
					mapRecord.getMapProjectId().toString(), mapRecord.getId().toString() );
			return null;
		}
	}

	/**
	 * Computes a map advice (if any) for a map entry's current state
	 * 
	 * @param mapEntry
	 *            the map entry
	 * @param authToken
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

		String user = "";
		MapRecord mapRecord = null;
		try {

			MappingService mappingService = new MappingServiceJpa();
            // after deserialization, the entry has a dummy map record with id
            // get the actual record
            mapRecord = mappingService.getMapRecord(mapEntry.getMapRecord().getId());
            Logger.getLogger(MappingServiceRest.class).info(
                "  mapEntry.mapRecord.mapProjectId = " + mapRecord.getMapProjectId());

	        // authorize call
            MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
      			user = securityService.getUsernameForToken(authToken);
            if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
                throw new WebApplicationException(Response.status(401).entity(
                        "User does not have permissions to compute the map advice.").build());
			

			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService
					.getProjectSpecificAlgorithmHandler(mappingService
							.getMapProject(mapRecord.getMapProjectId()));

			MapAdviceList mapAdviceList = algorithmHandler.computeMapAdvice(
					mapRecord, mapEntry);
			mappingService.close();
			return mapAdviceList;

		} catch (Exception e) {
			handleException(e, "trying to compute the map advice", user, 
					mapRecord.getMapProjectId().toString(), mapRecord.getId().toString());
			return null;
		}
	}

	// ///////////////////////////////////////////////
	// Role Management Services
	// ///////////////////////////////////////////////
	/**
	 * Gets a map user's role for a given map project
	 * 
	 * @param userName
	 * @param mapProjectId
	 * @param authToken
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
				"RESTful call:  /userRole/user/id" + userName + "/project/id/"
						+ mapProjectId);

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapUserRole mapUserRole = mappingService
					.getMapUserRoleForMapProject(userName, mapProjectId);
			mappingService.close();
			return mapUserRole;
		} catch (Exception e) { 
			handleException(e, "trying to get the map user role for a map project", userName, mapProjectId.toString(), "");
			return null;
		}
	}

	// /////////////////////////
	// Descendant services
	// /////////////////////////

	/**
	 * TODO:  Make this project specific
	 * 
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
	 * @param authToken
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

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve unmapped descendants for a concept.")
								.build());

			MappingService mappingService = new MappingServiceJpa();

			SearchResultList results = mappingService
					.findUnmappedDescendantsForConcept(terminologyId,
							terminology, terminologyVersion, threshold, null);

			mappingService.close();
			return results;

		} catch (Exception e) { 
			handleException(e, "trying to retrieve unmapped descendants for a concept", user, "", terminologyId);
			return null;
		}
	}

	// /////////////////////////////////////////////////////
	// Tree Position Routines for Terminology Browser
	// /////////////////////////////////////////////////////
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
	 * @param authToken
	 * @return the search result list
	 */
	@GET
	@Path("/treePosition/project/id/{projectId}/concept/id/{terminology}/{terminologyVersion}/{terminologyId}")
	@ApiOperation(value = "Get concept's local tree", notes = "Returns a tree structure representing the position of a concept in a terminology and its children", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public TreePositionList getTreePositionsWithDescendants(
			@ApiParam(value = "terminology id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "terminology of concept", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "terminology version of concept", required = true) @PathParam("terminologyVersion") String terminologyVersion,
			@ApiParam(value = "id of map project this tree will be displayed for", required = true) @PathParam("projectId") Long mapProjectId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken

	) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /treePosition/project/id/"
						+ mapProjectId.toString() + "/concept/id/"
						+ terminology + "/" + terminologyVersion + "/"
						+ terminologyId);

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapProjectId);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to get the tree positions with descendants.")
								.build());

			// get the local tree positions from content service
			ContentService contentService = new ContentServiceJpa();
			TreePositionList treePositions = contentService
					.getTreePositionsWithDescendants(terminologyId,
							terminology, terminologyVersion);
			contentService.computeTreePositionInformation(treePositions);
			contentService.close();

			// set the valid codes using mapping service
			MappingService mappingService = new MappingServiceJpa();
			mappingService.setTreePositionValidCodes(
					treePositions.getTreePositions(), mapProjectId);
			mappingService.setTreePositionTerminologyNotes(
					treePositions.getTreePositions(), mapProjectId);
			mappingService.close();

			return treePositions;
		} catch (Exception e) { 
			handleException(e, "trying to get the tree positions with descendants", user, mapProjectId.toString(), terminologyId);
			return null;
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
	 * @param authToken
	 * @return the search result list
	 */
	@GET
	@Path("/treePosition/project/id/{projectId}/terminology/id/{terminology}/{terminologyVersion}")
	@ApiOperation(value = "Get top-level trees", notes = "Returns a tree structure with an artificial root node and children representing the top-level concepts of a terminology", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public TreePositionList getRootTreePositionsForTerminology(
			@ApiParam(value = "terminology of concept", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "terminology version of concept", required = true) @PathParam("terminologyVersion") String terminologyVersion,
			@ApiParam(value = "id of map project this tree will be displayed for", required = true) @PathParam("projectId") Long mapProjectId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /treePosition/project/id/"
						+ mapProjectId.toString() + "/terminology/id/"
						+ terminology + "/" + terminologyVersion);

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapProjectId);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to get the root tree positions for a terminology.")
								.build());

			// get the root tree positions from content service
			ContentService contentService = new ContentServiceJpa();
			TreePositionList treePositions = contentService
					.getRootTreePositions(terminology, terminologyVersion);
			contentService.computeTreePositionInformation(treePositions);
			contentService.close();

			// set the valid codes using mapping service
			MappingService mappingService = new MappingServiceJpa();
			mappingService.setTreePositionValidCodes(
					treePositions.getTreePositions(), mapProjectId);
			mappingService.close();

			return treePositions;
		} catch (Exception e) { 
			handleException(e, "trying to get the root tree positions for a terminology", user, mapProjectId.toString(), "");
			return null;
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
	 * @param authToken
	 * @return the root-level trees corresponding to the query
	 */
	@GET
	@Path("/treePosition/project/id/{projectId}/terminology/id/{terminology}/{terminologyVersion}/query/{query}")
	@ApiOperation(value = "Get tree positions for query", notes = "Returns tree structures representing results a given terminology, terminology version, and query ", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public TreePositionList getTreePositionGraphsForQuery(
			@ApiParam(value = "terminology of concept", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "terminology version of concept", required = true) @PathParam("terminologyVersion") String terminologyVersion,
			@ApiParam(value = "paging/filtering/sorting object", required = true) @PathParam("query") String query,
			@ApiParam(value = "id of map project this tree will be displayed for", required = true) @PathParam("projectId") Long mapProjectId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(ContentServiceJpa.class).info(
				"RESTful call (Mapping): /treePosition/project/id/"
						+ mapProjectId + "/terminology/id/" + terminology + "/"
						+ terminologyVersion + "/query/" + query);

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapProjectId);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to get the tree position graphs for a query.")
								.build());

			// get the tree positions from concept service
			ContentService contentService = new ContentServiceJpa();
			TreePositionList treePositions = contentService
					.getTreePositionGraphForQuery(terminology,
							terminologyVersion, query);
			contentService.computeTreePositionInformation(treePositions);
			contentService.close();

			// set the valid codes using mapping service
			MappingService mappingService = new MappingServiceJpa();
			mappingService.setTreePositionValidCodes(
					treePositions.getTreePositions(), mapProjectId);
			mappingService.setTreePositionTerminologyNotes(
					treePositions.getTreePositions(), mapProjectId);
			mappingService.close();

			return treePositions;

		} catch (Exception e) { 
			handleException(e, "trying to get the tree position graphs for a query", user, mapProjectId.toString(), "");
			return null;
		}
	}

	// //////////////////////////////////////////////////
	// Workflow-related routines
	// /////////////////////////////////////////////////

	/**
	 * Returns records recently edited for a project and user. Used by
	 * editedList widget.
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @param userName
	 *            the user name
	 * @param pfsParameter
	 *            the pfs parameter
	 * @param authToken
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
				"RESTful call (Mapping): /record/project/id/" + mapProjectId
						+ "/user/id" + userName + "/edited");

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, new Long(mapProjectId));
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to get the recently edited map records.").build());
			
			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa recordList = (MapRecordListJpa) mappingService
					.getRecentlyEditedMapRecords(new Long(mapProjectId),
							userName, pfsParameter);
			mappingService.close();
			return recordList;
		} catch (Exception e) {
			handleException(e, "trying to get the recently edited map records", user, mapProjectId.toString(), "");
			return null;
		}
	}

	/**
	 * Returns the map records that when compared lead to the specified conflict
	 * record.
	 * 
	 * @param mapRecordId
	 * @param authToken
	 * @return map records in conflict for a given conflict lead record
	 * @throws Exception
	 */
	@GET
	@Path("/record/id/{id:[0-9][0-9]*}/conflictOrigins")
	@ApiOperation(value = "Get specialist records for assigned conflict", notes = "Return's a list of records in conflict for a lead's conflict resolution record", response = MapRecordListJpa.class)
	public MapRecordList getOriginMapRecordsForConflict(
			@ApiParam(value = "id of the map lead's conflict-in-progress or review record", required = true) @PathParam("id") Long mapRecordId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
			throws Exception {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/id/" + mapRecordId
						+ "/conflictOrigins");
  String user = "";
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecord mapRecord = mappingService.getMapRecord(mapRecordId);
			Logger.getLogger(MappingServiceRest.class)
					.info("  mapRecord.mapProjectId = "
							+ mapRecord.getMapProjectId());

		  // authorize call
		  MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
			user = securityService.getUsernameForToken(authToken);
		  if (!role.hasPrivilegesOf(MapUserRole.LEAD))
		  	throw new WebApplicationException(Response.status(401).entity(
					"User does not have permissions to retrieve the origin map records for a conflict.").build());
		
		  MapRecordList records = new MapRecordListJpa();

			records = mappingService
					.getOriginMapRecordsForConflict(mapRecordId);
			mappingService.close();

			return records;
		} catch (Exception e) {
			handleException(e, "trying to save work", user, "", mapRecordId.toString());
			return null;
		}

	}

	// //////////////////////////////////////////////
	// Map Record Validation and Compare Services
	// //////////////////////////////////////////////

	/**
	 * Validates a map record.
	 * 
	 * @param mapRecord
	 *            the map record to be validated
	 * @param authToken
	 * @return Response the response
	 */
	@POST
	@Path("/validation/record/validate")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Validates a map record", notes = "Performs validation checks on a map record", response = MapRecordJpa.class)
	public ValidationResult validateMapRecord(
			@ApiParam(value = "The map record to validate.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapRecordJpa mapRecord,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /validation/record/validate for map record id = "
						+ mapRecord.getId().toString());

		// get the map project for this record

		String user = "";
		try {
			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapRecord.getMapProjectId());
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to validate a map record.")
								.build());

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
		} catch (Exception e) {
			handleException(e, "trying to validate a map record", user, 
					mapRecord.getMapProjectId().toString(), mapRecord.getId().toString());
			return null;
		}
	}

	/**
	 * Compare map records and return differences
	 * 
	 * @param mapRecordId1
	 *            the map record id1
	 * @param mapRecordId2
	 *            the map record id2
	 * @param authToken
	 * @return the validation result
	 */
	@GET
	@Path("/validation/record/id/{recordId1}/record/id/{recordId2}/compare")
	@ApiOperation(value = "Compare two map records", notes = "Returns a list of warnings and errors from comparing two map records", response = ValidationResultJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public ValidationResult compareMapRecords(
			@ApiParam(value = "id of first map record", required = true) @PathParam("recordId1") Long mapRecordId1,
			@ApiParam(value = "id of second map record", required = true) @PathParam("recordId2") Long mapRecordId2,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /validation/record/id/" + mapRecordId1
						+ "record/id/" + mapRecordId1 + "/compare");


		String user = "";	
		try {

			MappingService mappingService = new MappingServiceJpa();
			MapRecord mapRecord1, mapRecord2;

			mapRecord1 = mappingService.getMapRecord(mapRecordId1);
			mapRecord2 = mappingService.getMapRecord(mapRecordId2);

			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecord1.getMapProjectId());
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to compare map records.").build());

			MapProject mapProject = mappingService.getMapProject(mapRecord1
					.getMapProjectId());
			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService
					.getProjectSpecificAlgorithmHandler(mapProject);
			ValidationResult validationResult = algorithmHandler
					.compareMapRecords(mapRecord1, mapRecord2);

			mappingService.close();
			return validationResult;

		} catch (Exception e) {
			handleException(e, "trying to compare map records", user, "", mapRecordId1.toString());
			return null;
		}
	}

	@GET
	@Path("/project/id/{mapProjectId}/concept/{terminologyId}/isValid")
	@ApiOperation(value = "Get the root tree (top-level concepts) for a given terminology", notes = "Returns a tree structure with an artificial root node and children representing the top-level concepts of a terminology", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Concept isTargetCodeValid(
			@ApiParam(value = "map project id", required = true) @PathParam("mapProjectId") Long mapProjectId,
			@ApiParam(value = "terminology id", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/id/" + mapProjectId
						+ "/concept/" + terminologyId + "/isValid");

		String user = "";
		try {
			MappingService mappingService = new MappingServiceJpa();

			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapProjectId);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to check valid target codes")
								.build());

			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService
					.getProjectSpecificAlgorithmHandler(mapProject);
			boolean isValid = algorithmHandler.isTargetCodeValid(terminologyId);

			mappingService.close();
			if (isValid == true) {
				ContentService contentService = new ContentServiceJpa();
				Concept c = contentService.getConcept(terminologyId,
						mapProject.getDestinationTerminology(),
						mapProject.getDestinationTerminologyVersion());
				return c;
			} else {
				return null;
			}

		} catch (Exception e) {
			handleException(e, "trying to compare map records", user, mapProjectId.toString(), "");
			return null;
		}
	}
	

	@POST
	@Path("/upload/{mapProjectId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
		@FormDataParam("file") InputStream fileInputStream,
		@FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
		@PathParam("mapProjectId") Long mapProjectId,
		@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
		
		String user = "";
		try {
			MappingService mappingService = new MappingServiceJpa();

			// authorize call
			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapProjectId);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.LEAD))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to check valid target codes")
								.build());

			// get destination directory for uploaded file
			String configFileName = System.getProperty("run.config");
			Logger.getLogger(MappingServiceRest.class).info(
					"  run.config = " + configFileName);
			Properties config = new Properties();
			FileReader in = new FileReader(new File(configFileName));
			config.load(in);
			in.close();
			Logger.getLogger(MappingServiceRest.class).info(
					"  properties = " + config);

			String docDir = config.getProperty("map.principle.source.document.dir");
			
			File dir = new File(docDir);
			File archiveDir = new File(docDir + "/archive");

		  // compose the name of the stored file
			MapProject mapProject = getMapProject(new Long(mapProjectId), authToken);
			SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");
			String date = dt.format(new Date());

			String extension = "";
			if (contentDispositionHeader.getFileName().indexOf(".") != -1) {
				extension =
						contentDispositionHeader.getFileName().substring(
								contentDispositionHeader.getFileName().lastIndexOf("."));
			}
			String camelCaseFileName =
					mapProject.getMapPrincipleSourceDocumentName().replaceAll(" ", "");
			File file =
					new File(dir, mapProjectId + "_" + camelCaseFileName + extension);
			File archiveFile =
					new File(archiveDir, mapProjectId + "_" + camelCaseFileName + "." + date
							+ extension);

			// save the file to the server
			saveFile(fileInputStream, file.getAbsolutePath());
		  copyFile(file, archiveFile);

			String output =
					"File saved to server location : " + file.getAbsolutePath() + " and "
							+ archiveFile.getAbsolutePath();

			// update project
			// TODO: removed for now, until MapProjectJpa changes can be recovered
			//mapProject.setMapPrincipleSourceDocument(mapProjectId + "_" + camelCaseFileName + extension);
			updateMapProject((MapProjectJpa) mapProject, authToken);

			return Response.status(200).entity(output).build();
		} catch (Exception e) {
			handleException(e, "trying to upload a file", user, mapProjectId.toString(), "");
			return null;
		}
	}

	// save uploaded file to a defined location on the server
	private void saveFile(InputStream uploadedInputStream, String serverLocation) {

		try {

			OutputStream outputStream =
					new FileOutputStream(new File(serverLocation));
			int read = 0;
			byte[] bytes = new byte[1024];

			outputStream = new FileOutputStream(new File(serverLocation));
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	public static void copyFile(File sourceFile, File destFile)
		throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;

		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}
}
