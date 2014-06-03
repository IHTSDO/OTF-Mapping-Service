package org.ihtsdo.otf.mapping.rest;

import java.util.Comparator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
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

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapProjectListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRelationListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TreePositionListJpa;
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
	public MapProjectListJpa getMapProjects() {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping):  /project/projects");

		try {
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
	@ApiOperation(value = "Find project by id", notes = "Returns a MapProject given a project id in either JSON or XML format", response = MapProject.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapProject getMapProjectForId(
			@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/id/"
						+ mapProjectId.toString());

		try {
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
			@ApiParam(value = "The map project to add. Must be in Json or Xml format", required = true) MapProjectJpa mapProject) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/add");

		try {
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
			@ApiParam(value = "The map project to update. Must exist in mapping database. Must be in Json or Xml format", required = true) MapProjectJpa mapProject) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/update");

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapProject(mapProject);
			mappingService.close();
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
			@ApiParam(value = "Map project object to delete", required = true)MapProjectJpa mapProject) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/delete for "
						+ mapProject.getName());

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapProject(mapProject.getId());
			mappingService.close();
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
	public SearchResultList findMapProjects(
			@ApiParam(value = "lucene search string", required = true) @PathParam("String") String query) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /project/query/" + query);

		try {
			MappingService mappingService = new MappingServiceJpa();
			SearchResultList searchResultList = mappingService.findMapProjects(
					query, new PfsParameterJpa());
			mappingService.close();
			return searchResultList;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Returns all map projects for a map user
	 * 
	 * @param mapLeadId
	 *            the map lead
	 * @return the map projects
	 */
	@GET
	@Path("/user/id/{id:[0-9][0-9]*}/projects")
	@ApiOperation(value = "Find all projects for user", notes = "Returns a MapUser's MapProjects in either JSON or XML format", response = MapProjectListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapProjectListJpa getMapProjectsForUser(
			@ApiParam(value = "Id of map lead to fetch projects for", required = true) @PathParam("id") Long mapLeadId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): lead/id/" + mapLeadId.toString()
						+ "/projects");

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapUser mapLead = mappingService.getMapUser(mapLeadId);
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
	@Path("/user/users/")
	@ApiOperation(value = "Get all mapping users", notes = "Returns all MapUsers in either JSON or XML format", response = MapUserListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapUserListJpa getMapUsers() {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /user/users");

		try {
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
	@Path("/user/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find user by id", notes = "Returns a MapUser given a user id in either JSON or XML format", response = MapUser.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapUser getMapUserForId(
			@ApiParam(value = "Id of map lead to fetch", required = true) @PathParam("id") Long mapUserId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): user/id/" + mapUserId.toString());

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapUser mapUser = mappingService.getMapUser(mapUserId);
			mappingService.close();
			return mapUser;
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
			@ApiParam(value = "The map user to add. Must be in Json or Xml format", required = true) MapUserJpa mapUser) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /user/add");

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.addMapUser(mapUser);
			mappingService.close();
			return null;
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
			@ApiParam(value = "The map user to update.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapUserJpa mapUser) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /user/update");

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapUser(mapUser);
			mappingService.close();
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
			@ApiParam(value = "The map user object to delete", required = true) MapUserJpa mapUser) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /user/delete for user " + mapUser.getName());

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapUser(mapUser.getId());
			mappingService.close();
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
	public MapRelationListJpa getMapRelations() {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /relation/relations");

		try {
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
	public MapPrinciple getMapPrincipleForId(
			@ApiParam(value = "Id of map principle to fetch", required = true) @PathParam("id") Long mapPrincipleId) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /principle/id/"
						+ mapPrincipleId.toString());
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapPrinciple mapPrinciple = mappingService
					.getMapPrinciple(mapPrincipleId);
			mappingService.close();
			return mapPrinciple;
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
	@ApiOperation(value = "Add a user preferences object", notes = "Adds a MapPrinciple", response = MapPrincipleJpa.class)
	public MapPrinciple addMapPrinciple(
			@ApiParam(value = "The map user preferences object to add. Must be in Json or XML format", required = true) MapPrincipleJpa mapPrinciple) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /userPreferences/add");

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapPrinciple result = mappingService
					.addMapPrinciple(mapPrinciple);
			mappingService.close();
			return result;
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
	public void updateMapPrincipleForId(
			@ApiParam(value = "Map Principle to update", required = true) MapPrincipleJpa mapPrinciple) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /principle/update");

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapPrinciple(mapPrinciple);
			mappingService.close();

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
	@ApiOperation(value = "Remove user preferences", notes = "Removes a set of map user preferences")
	public void removeMapPrinciple(
			@ApiParam(value = "Map user preferences object to remove", required = true) MapPrincipleJpa principle) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /principle/remove for id "
						+ principle.getId().toString());

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapPrinciple(principle.getId());
			mappingService.close();
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
	@Path("/userPreferences/{userName}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Gets a user preferences object", notes = "Gets a MapUserPreferences object for a given userName", response = MapUserPreferencesJpa.class)
	public MapUserPreferences getMapUserPreferences(
			@ApiParam(value = "The map user's user name", required = true) @PathParam("userName") String userName) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call:  /userPreferences/" + userName);

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapUserPreferences result = mappingService
					.getMapUserPreferences(userName);
			mappingService.close();
			return result;
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
			@ApiParam(value = "The map user preferences object to add. Must be in Json or XML format", required = true) MapUserPreferencesJpa mapUserPreferences) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /userPreferences/add");

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapUserPreferences result = mappingService
					.addMapUserPreferences(mapUserPreferences);
			mappingService.close();
			return result;
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
			@ApiParam(value = "The map user preferences to update.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapUserPreferencesJpa mapUserPreferences) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /userPreferences/update");

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapUserPreferences(mapUserPreferences);
			mappingService.close();

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}
	
	/**
	 * Removes a set of map user preferences
	 * @param mapUserPreferencesId
	 *            the id of the map user preferences object to be deleted
	 * @return Response the response
	 */
	@DELETE
	@Path("/userPreferences/remove")
	@ApiOperation(value = "Remove user preferences", notes = "Removes a set of map user preferences")
	public void removeMapUserPreferences(
			@ApiParam(value = "Map user preferences object to remove", required = true) MapUserPreferencesJpa mapUserPreferences) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /userPreferences/remove for id "
						+ mapUserPreferences.getId().toString());

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapUserPreferences(mapUserPreferences.getId());
			mappingService.close();
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
	@ApiOperation(value = "Find record by id", notes = "Returns a MapRecord given a record id in either JSON or XML format", response = MapRecord.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRecord getMapRecordForId(
			@ApiParam(value = "Id of map record to fetch", required = true) @PathParam("id") Long mapRecordId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/id/" + mapRecordId.toString());

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
			@ApiParam(value = "The map record to add. Must be in Json or XML format", required = true) MapRecordJpa mapRecord) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/add");

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecord result = mappingService.addMapRecord(mapRecord);
			mappingService.close();
			return result;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Updates a map record
	 * 
	 * @param mapRecord
	 *            the map record to be added
	 * @return Response the response
	 */
	@POST
	@Path("/record/update")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Update a record", notes = "Updates a map record", response = Response.class)
	public void updateMapRecord(
			@ApiParam(value = "The map record to update.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapRecordJpa mapRecord) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/update");

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.updateMapRecord(mapRecord);
			mappingService.close();
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
			@ApiParam(value = "Map Record object to delete", required = true) MapRecordJpa mapRecord) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/delete with map record id = "
						+ mapRecord.toString());

		try {
			MappingService mappingService = new MappingServiceJpa();
			mappingService.removeMapRecord(mapRecord.getId());
			mappingService.close();
			return null;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Returns the records for a given concept id
	 * 
	 * @param conceptId
	 *            the concept id
	 * @return the mapRecords
	 */
	@GET
	@Path("/record/conceptId/{String}")
	@ApiOperation(value = "Find records by concept id", notes = "Returns MapRecords given a concept id in either JSON or XML format", response = MapRecord.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRecordListJpa getMapRecordsForConceptId(
			@ApiParam(value = "Concept id of map record to fetch", required = true) @PathParam("String") String conceptId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/conceptId/" + conceptId);

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
					.getMapRecordsForConcept(conceptId);
			mappingService.close();
			return mapRecordList;
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
	@Path("/record/projectId/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find paged records by project id", notes = "Returns delimited page of published or ready-for-publicatoin MapRecords given a paging/filtering/sorting parameters object", response = MapRecordListJpa.class)
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@CookieParam(value = "userInfo")
	public MapRecordListJpa getMapRecordsForMapProjectId(
			@ApiParam(value = "Project id associated with map records", required = true) @PathParam("id") Long mapProjectId,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/projectId/"
						+ mapProjectId.toString() + " with PfsParameter: "
						+ "\n" + "     Index/Results = "
						+ Integer.toString(pfsParameter.getStartIndex()) + "/"
						+ Integer.toString(pfsParameter.getMaxResults()) + "\n"
						+ "     Sort field    = " + pfsParameter.getSortField()
						+ "     Filter String = "
						+ pfsParameter.getFilterString());

		// execute the service call

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
					.getPublishedAndReadyForPublicationMapRecordsForMapProject(mapProjectId, pfsParameter);
			mappingService.close();
			return mapRecordList;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

	/**
	 * Deletes all map records associated with a map project given a project id
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @return the number of records deleted
	 */
	@DELETE
	@Path("record/delete/projectId/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Delete all project records", notes = "Deletes all map records for a project id", response = Integer.class)
	@Produces({ MediaType.TEXT_PLAIN })
	public String removeMapRecordsForProjectId(
			@ApiParam(value = "Project id for which map records are to be deleted", required = true) @PathParam("id") Long mapProjectId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/delete/projectId"
						+ mapProjectId.toString());

		try {
			MappingService mappingService = new MappingServiceJpa();
			Long nRecords = mappingService
					.removeMapRecordsForProject(mapProjectId);
			mappingService.close();

			// Jersey can't handle Long as return type, convert to string
			return nRecords.toString();
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
	@Path("/record/{id:[0-9][0-9]*}/revisions")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Get record revision history", notes = "Returns a map record's previous versions from the audit trail", response = MapRecordListJpa.class)
	public MapRecordListJpa getMapRecordRevisions(
			@ApiParam(value = "Id of map record to get revisions for", required = true) @PathParam("id") Long mapRecordId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/validate");

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa revisions = (MapRecordListJpa) mappingService
					.getMapRecordRevisions(mapRecordId);
			mappingService.close();
			return revisions;
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
			@ApiParam(value = "", required = true) MapEntryJpa mapEntry) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /relation/compute");

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecord mapRecord = mapEntry.getMapRecord();

			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService.getProjectSpecificAlgorithmHandler(mappingService.getMapProject(mapRecord
					.getMapProjectId()));

			MapRelation mapRelation = algorithmHandler.computeMapRelation(mapRecord, mapEntry);
			mappingService.close();
			return mapRelation;
			
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
			@ApiParam(value = "", required = true) MapEntryJpa mapEntry) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /advice/compute");

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecord mapRecord = mapEntry.getMapRecord();

			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService.getProjectSpecificAlgorithmHandler(mappingService.getMapProject(mapRecord
					.getMapProjectId()));

			MapAdviceList mapAdviceList = algorithmHandler.computeMapAdvice(mapRecord, mapEntry);
			mappingService.close();
			return mapAdviceList;
			
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
	 * @param projectId
	 * @return result the role
	 */
	@GET
	@Path("/userRole/{userName}/projectId/{id:[0-9][0-9]*}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Gets the role.", notes = "Gets the role for the given userName and projectId", response = SearchResultList.class)
	public SearchResultList getMapUserRole(
			@ApiParam(value = "The map user's user name", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call:  /userRole/" + userName + "/projectId/" + mapProjectId);

		try {
			MappingService mappingService = new MappingServiceJpa();
			SearchResultList result = mappingService
					.getMapUserRole(userName, mapProjectId);
			mappingService.close();
			return result;
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
	@Path("/concept/{terminology}/{version}/id/{id}/threshold/{threshold:[0-9][0-9]*}")
	@ApiOperation(value = "Find unmapped descendants", notes = "Returns a concept's unmapped descendants given a concept id, terminology, terminology version, and low-level concept threshold", response = Concept.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList getUnmappedDescendantsForConcept(
			@ApiParam(value = "concept terminology id", required = true) @PathParam("id") String terminologyId,
			@ApiParam(value = "concept terminology", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "concept terminology version", required = true) @PathParam("version") String terminologyVersion,
			@ApiParam(value = "threshold max number of descendants for a low-level concept", required = true) @PathParam("threshold") int threshold) {

		// log call
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /concept/" + terminology + "/"
						+ terminologyVersion + "/id/" + terminologyId
						+ "/threshold/" + threshold);

		try {
			MappingService mappingService = new MappingServiceJpa();

			SearchResultList results = mappingService
					.findUnmappedDescendantsForConcept(terminologyId,
							terminology, terminologyVersion, threshold);

			mappingService.close();
			return results;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	////////////////////////////////////////////
	// Project Scope Services
	////////////////////////////////////////////
	
	/**
	 * Find the concepts included in project scope
	 * 
	 * @param mapProjectId
	 * 
	 * @return the map records for concept id
	 */
	@GET
	@Path("/scope/includes/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find scope-included concepts", notes = "Returns concepts specifically included in project scope", response = MapProject.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findConceptsInScope(
			@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /scope/includes/"
						+ mapProjectId.toString());

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(mapProjectId);
			SearchResultList searchResultList = mappingService
					.findConceptsInScope(mapProject.getId());
			mappingService.close();
			return searchResultList;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

	/**
	 * Find concepts excluded from scope.
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @return the search result list
	 */

	@GET
	@Path("/scope/excludes/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find scope-excluded concepts", notes = "Returns the concepts specifically excluded from a project's scope", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findConceptsExcludedFromScope(
			@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /scope/excludes/"
						+ mapProjectId.toString());

		try {
			MappingService mappingService = new MappingServiceJpa();
			SearchResultList searchResultList = mappingService
					.findConceptsExcludedFromScope(mapProjectId);
			mappingService.close();
			return searchResultList;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

	/**
	 * Find mapped concepts out of scope bounds.
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @return the search result list
	 */
	@GET
	@Path("/scope/outofbounds/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find concepts out of scope", notes = "Returns mapped concepts out of the project's scope", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findMappedConceptsOutOfScopeBounds(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") Long mapProjectId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /scope/outofbounds/"
						+ mapProjectId.toString());

		try {
			MappingService mappingService = new MappingServiceJpa();
			SearchResultList searchResultList = mappingService
					.findMappedConceptsOutOfScopeBounds(mapProjectId);
			mappingService.close();
			return searchResultList;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

	/**
	 * Find unmapped concepts in scope.
	 * 
	 * @param mapProjectId
	 *            the map project id
	 * @return the search result list
	 */
	@GET
	@Path("/scope/unmapped/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find unmapped concepts in scope", notes = "Returns the unmapped concepts in a project's scope", response = SearchResultList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findUnmappedConceptsInScope(
			@ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /scope/unmapped/"
						+ mapProjectId.toString());

		try {
			MappingService mappingService = new MappingServiceJpa();
			SearchResultList searchResultList = mappingService
					.findUnmappedConceptsInScope(mapProjectId);
			mappingService.close();
			return searchResultList;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

	

	
	
	
	///////////////////////////////////////////////////////
	// Tree Position Routines for Terminology Browser
	///////////////////////////////////////////////////////
	/**
	 * Finds tree positions for concept.
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
	@Path("/tree/projectId/{projectId}/concept/{terminology}/{terminologyVersion}/id/{terminologyId}")
	@ApiOperation(value = "Get concept's local tree", notes = "Returns a tree structure representing the position of a concept in a terminology and its children", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public TreePositionListJpa getLocalTreePositionsForConcept(
			@ApiParam(value = "terminology id of concept", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "terminology of concept", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "terminology version of concept", required = true) @PathParam("terminologyVersion") String terminologyVersion,
			@ApiParam(value = "id of map project this tree will be displayed for", required = true) @PathParam("projectId") Long mapProjectId

	) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /tree/projectId/"
						+ mapProjectId.toString() + "/concept/" + terminology
						+ "/" + terminologyVersion + "/id/" + terminologyId);

		try {
			// get the local tree positions from content service
			ContentService contentService = new ContentServiceJpa();
			List<TreePosition> treePositions = contentService.getLocalTrees(
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
	@Path("/tree/projectId/{projectId}/terminology/{terminology}/{terminologyVersion}")
	@ApiOperation(value = "Get top-level trees", notes = "Returns a tree structure with an artificial root node and children representing the top-level concepts of a terminology", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public TreePositionListJpa getRootTreePositionsForTerminology(
			@ApiParam(value = "terminology of concept", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "terminology version of concept", required = true) @PathParam("terminologyVersion") String terminologyVersion,
			@ApiParam(value = "id of map project this tree will be displayed for", required = true) @PathParam("projectId") Long mapProjectId) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /tree/projectId/"
						+ mapProjectId.toString() + "/concept/" + terminology
						+ "/" + terminologyVersion);

		try {

			// get the root tree positions from content service
			ContentService contentService = new ContentServiceJpa();
			List<TreePosition> treePositions = contentService
					.getRootTreePositionsForTerminology(terminology,
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
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Finds tree positions for concept query.
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
	@Path("/tree/projectId/{projectId}/terminology/{terminology}/{terminologyVersion}/query/{query}")
	@ApiOperation(value = "Get tree positions for query", notes = "Returns tree structures representing results a given terminology, terminology version, and query ", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public TreePositionListJpa getRootTreePositionsForConceptQuery(
			@ApiParam(value = "terminology of concept", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "terminology version of concept", required = true) @PathParam("terminologyVersion") String terminologyVersion,
			@ApiParam(value = "paging/filtering/sorting object", required = true) @PathParam("query") String query,
			@ApiParam(value = "id of map project this tree will be displayed for", required = true) @PathParam("projectId") Long mapProjectId) {

		Logger.getLogger(ContentServiceJpa.class).info(
				"RESTful call (Mapping): /tree/concept/" + terminology + "/"
						+ terminologyVersion + "/query/" + query);
		try {

			// get the tree positions from concept service
			ContentService contentService = new ContentServiceJpa();
			List<TreePosition> treePositions = contentService
					.getTreePositionsForConceptQuery(terminology,
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
	@Path("/recentRecords/{id}/{userName}")
	@ApiOperation(value = "Get user's edited map records", notes = "Returns paged recently edited map records for given userName and paging informatoin", response = MapRecordListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public MapRecordListJpa getRecentlyEditedMapRecords(
			@ApiParam(value = "Id of map project", required = true) @PathParam("id") String mapProjectId,
			@ApiParam(value = "User name", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /recentRecords/"
						+ mapProjectId.toString() + "/" + userName);

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecordListJpa recordList = (MapRecordListJpa) mappingService
					.getRecentlyEditedMapRecords(new Long(mapProjectId),
							userName, pfsParameter);
			mappingService.close();
			return recordList;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Returns the origin map records resulting in a conflict assigned to a lead
	 * Used by compareRecords widget
	 * 
	 * @param mapRecordId
	 * @return map records in conflict for a given conflict lead record
	 * @throws Exception
	 */
	@GET
	@Path("/record/conflictRecords/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Get specialist records for assigned conflict", notes = "Return's a list of records in conflict for a lead's conflict resolution record", response = MapRecordListJpa.class)
	public MapRecordList getOriginRecordsForConflict(
			@ApiParam(value = "id of the map lead's conflict-in-progress record", required = true) @PathParam("id") Long mapRecordId)
			throws Exception {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/conflictRecords/"
						+ mapRecordId.toString());

		MapRecordList records = new MapRecordListJpa();

		MappingService mappingService = new MappingServiceJpa();
		records = mappingService.getOriginRecordsForConflict(mapRecordId);
		mappingService.close();

		return records;
	}

}
