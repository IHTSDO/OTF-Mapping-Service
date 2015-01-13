package org.ihtsdo.otf.mapping.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
import org.ihtsdo.otf.mapping.dto.KeyValuePair;
import org.ihtsdo.otf.mapping.dto.KeyValuePairList;
import org.ihtsdo.otf.mapping.dto.KeyValuePairLists;
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
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
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
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * The Mapping Services REST package.
 */
@Path("/mapping")
@Api(value = "/mapping", description = "Operations supporting map objects.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class MappingServiceRest extends RootServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link MappingServiceRest}.
   *
   * @throws Exception the exception
   */
  public MappingServiceRest() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Projects
  // ///////////////////////////////////////////////////

  /**
   * Returns all map projects in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map projects
   */
  @GET
  @Path("/project/projects")
  @ApiOperation(value = "Get map projects.", notes = "Gets a list of all map projects.", response = MapProjectListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProjectListJpa getMapProjects(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/projects");
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to retrieve map projects.")
            .build());

      MappingService mappingService = new MappingServiceJpa();

      // instantiate list of projects to return
      MapProjectListJpa mapProjects = new MapProjectListJpa();

      // cycle over projects and verify that this user can view each project
      for (MapProject mapProject : mappingService.getMapProjects()
          .getMapProjects()) {

        // if this user has a role of VIEWER or above for this project (i.e. is
        // not NONE)
        if (!securityService.getMapProjectRoleForToken(authToken,
            mapProject.getId()).equals(MapUserRole.NONE)
            || mapProject.isPublic() == true) {

          // do not serialize the scope concepts or excludes (retrieval
          // optimization)
          mapProject.setScopeConcepts(null);
          mapProject.setScopeExcludedConcepts(null);

          mapProjects.addMapProject(mapProject);
        }

      }

      // set total count to count for completeness (not a paged list)
      mapProjects.setTotalCount(mapProjects.getCount());

      // sort projects by name
      mapProjects.sortBy(new Comparator<MapProject>() {
        @Override
        public int compare(MapProject o1, MapProject o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });

      // close the mapping service and return the viewable projects
      mappingService.close();
      return mapProjects;

    } catch (Exception e) {
      this.handleException(e, "trying to retrieve map projects", user, "", "");
      return null;
    }
  }

  /**
   * Returns the project for a given id (auto-generated) in JSON format.
   *
   * @param mapProjectId the mapProjectId
   * @param authToken the auth token
   * @return the mapProject
   */
  @GET
  @Path("/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get map project by id.", notes = "Gets a map project for the specified parameters.", response = MapProject.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProject getMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/id/" + mapProjectId.toString());

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to retrieve the map project.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      mapProject.getScopeConcepts().size();
      mapProject.getScopeExcludedConcepts().size();
      mapProject.getMapAdvices().size();
      mapProject.getMapRelations().size();
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
   * Adds a map project.
   *
   * @param mapProject the map project to be added
   * @param authToken the auth token
   * @return returns the added map project object
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/project/add")
  @ApiOperation(value = "Add a map project.", notes = "Adds the specified map project.", response = MapProjectJpa.class)
  public MapProject addMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/add");

    String user = "";
    String project = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
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
   * Updates a map project.
   *
   * @param mapProject the map project to be added
   * @param authToken the auth token
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/project/update")
  @ApiOperation(value = "Update a map project.", notes = "Updates specified map project if it already exists.", response = MapProjectJpa.class)
  public void updateMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/update");

    String user = "";

    try {
      // authorize call
      // IF application role = admin
      // OR map project role = lead

      MapUserRole roleProject =
          securityService.getMapProjectRoleForToken(authToken,
              mapProject.getId());
      MapUserRole roleApplication =
          securityService.getApplicationRoleForToken(authToken);

      if (!(roleApplication.hasPrivilegesOf(MapUserRole.ADMINISTRATOR) || roleProject
          .hasPrivilegesOf(MapUserRole.LEAD)))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to udpate a map project.")
            .build());

      MappingService mappingService = new MappingServiceJpa();

      // scope includes and excludes are transient, and must be added to project
      // before update from webapp
      MapProject mapProjectFromDatabase =
          mappingService.getMapProject(mapProject.getId());

      // set the scope concepts and excludes to the contents of the database
      // prior to update
      mapProject.setScopeConcepts(mapProjectFromDatabase.getScopeConcepts());
      mapProject.setScopeExcludedConcepts(mapProjectFromDatabase
          .getScopeExcludedConcepts());

      // update the project and close the service
      mappingService.updateMapProject(mapProject);
      mappingService.close();

    } catch (Exception e) {
      handleException(e, "trying to update a map project", user,
          mapProject.getName(), "");
    }
  }

  /**
   * Removes a map project.
   *
   * @param mapProject the map project
   * @param authToken the auth token
   */
  @DELETE
  @Path("/project/delete")
  @ApiOperation(value = "Remove a map project.", notes = "Removes specified map project if it already exists.", response = MapProject.class)
  public void removeMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/delete for " + mapProject.getName());

    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove a map project.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      mappingService.removeMapProject(mapProject.getId());
      mappingService.close();

    } catch (Exception e) {
      handleException(e, "trying to remove a map project", user,
          mapProject.getName(), "");
    }
  }

  /**
   * Returns all map projects for a lucene query.
   *
   * @param query the string query
   * @param authToken the auth token
   * @return the map projects
   */
  @GET
  @Path("/project/query/{String}")
  @ApiOperation(value = "Find map projects by query.", notes = "Gets a list of search results for map projects matching the lucene query.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findMapProjectsForQuery(
    @ApiParam(value = "Query, e.g. 'SNOMED to ICD10'", required = true) @PathParam("String") String query,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/query/" + query);
    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to find map projects.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      SearchResultList searchResultList =
          mappingService.findMapProjectsForQuery(query, new PfsParameterJpa());
      mappingService.close();
      return searchResultList;

    } catch (Exception e) {
      handleException(e, "trying to find map projects", user, "", "");
      return null;
    }
  }

  /**
   * Returns all map projects for a map user.
   *
   * @param mapUserName the map user name
   * @param authToken the auth token
   * @return the map projects
   */
  @GET
  @Path("/project/user/id/{username}")
  @ApiOperation(value = "Get all map projects for a user.", notes = "Gets a list of map projects for the specified user.", response = MapProjectListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProjectListJpa getMapProjectsForUser(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String mapUserName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/user/id/" + mapUserName);
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get the map projects for given user.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapUser mapLead = mappingService.getMapUser(mapUserName);
      MapProjectListJpa mapProjects =
          (MapProjectListJpa) mappingService.getMapProjectsForMapUser(mapLead);

      for (MapProject mapProject : mapProjects.getMapProjects()) {
        mapProject.getScopeConcepts().size();
        mapProject.getScopeExcludedConcepts().size();
        mapProject.getMapAdvices().size();
        mapProject.getMapRelations().size();
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
      handleException(e, "trying to get the map projects for a given user",
          user, "", "");
      return null;
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Users
  // ///////////////////////////////////////////////////

  /**
   * Returns all map leads in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map leads
   */
  @GET
  @Path("/user/users")
  @ApiOperation(value = "Get all mapping users.", notes = "Gets all map users.", response = MapUserListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapUserListJpa getMapUsers(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /user/users");
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map users.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapUserListJpa mapLeads = (MapUserListJpa) mappingService.getMapUsers();
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
   * Returns the scope concepts for map project.
   *
   * @param projectId the project id
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the scope concepts for map project
   */
  @POST
  @Path("/project/id/{projectId}/scopeConcepts")
  @ApiOperation(value = "Get scope concepts for a map project.", notes = "Gets a (pageable) list of scope concepts for a map project", response = SearchResultListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getScopeConceptsForMapProject(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Paging/filtering/sorting object", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to retrieve scope concepts.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(projectId);

      SearchResultList results =
          mappingService
              .getScopeConceptsForMapProject(mapProject, pfsParameter);

      mappingService.close();

      return results;

    } catch (Exception e) {
      this.handleException(e, "trying to retrieve scope concepts", user,
          projectName, "");
      return null;
    }
  }

  /**
   * Adds the scope concept to map project.
   *
   * @param terminologyId the terminology id
   * @param projectId the project id
   * @param authToken the auth token
   */
  @POST
  @Path("/project/id/{projectId}/scopeConcepts/add")
  @ApiOperation(value = "Adds scope concept to a map project.", notes = "Adds scope concept to a map project.", response = Response.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public void addScopeConceptToMapProject(
    @ApiParam(value = "Concept to add", required = true) String terminologyId,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to retrieve scope concepts.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(projectId);

      mapProject.addScopeConcept(terminologyId);
      mappingService.updateMapProject(mapProject);

      mappingService.close();

    } catch (Exception e) {
      this.handleException(e, "trying to add scope concept to project", user,
          projectName, "");
    }
  }

  /**
   * Removes the scope concept from map project.
   *
   * @param terminologyId the terminology id
   * @param projectId the project id
   * @param authToken the auth token
   */
  @POST
  @Path("/project/id/{projectId}/scopeConcepts/remove")
  @ApiOperation(value = "Removes scope concept from a map project.", notes = "Removes scope concept from a map project.", response = Response.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public void removeScopeConceptFromMapProject(
    @ApiParam(value = "Concept to add", required = true) String terminologyId,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove scope concepts.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(projectId);

      mapProject.removeScopeConcept(terminologyId);
      mappingService.updateMapProject(mapProject);

      mappingService.close();

    } catch (Exception e) {
      this.handleException(e, "trying to remove scope concept from project",
          user, projectName, "");
    }
  }

  /**
   * Returns the scope excluded concepts for map project.
   *
   * @param projectId the project id
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the scope excluded concepts for map project
   */
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts")
  @ApiOperation(value = "Get scope excluded concepts for a map project.", notes = "Gets a (pageable) list of scope excluded concepts for a map project", response = SearchResultListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getScopeExcludedConceptsForMapProject(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Paging/filtering/sorting object", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve scope excluded concepts.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(projectId);

      SearchResultList results =
          mappingService.getScopeExcludedConceptsForMapProject(mapProject,
              pfsParameter);

      mappingService.close();

      return results;

    } catch (Exception e) {
      this.handleException(e, "trying to retrieve scope excluded concepts",
          user, projectName, "");
      return null;
    }
  }

  /**
   * Adds the scope excluded conceptso map project.
   *
   * @param terminologyId the terminology id
   * @param projectId the project id
   * @param authToken the auth token
   */
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts/add")
  @ApiOperation(value = "Adds scope excluded concept to a map project.", notes = "Adds scope excluded concept to a map project.", response = Response.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public void addScopeExcludedConceptsoMapProject(
    @ApiParam(value = "ExcludedConcept to add", required = true) String terminologyId,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve scope excluded concepts.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(projectId);

      mapProject.addScopeExcludedConcept(terminologyId);
      mappingService.updateMapProject(mapProject);

      mappingService.close();

    } catch (Exception e) {
      this.handleException(e,
          "trying to add scope excluded concept to project", user, projectName,
          "");
    }
  }

  /**
   * Removes the scope excluded concept from map project.
   *
   * @param terminologyId the terminology id
   * @param projectId the project id
   * @param authToken the auth token
   */
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts/remove")
  @ApiOperation(value = "Removes scope excluded concept from a map project.", notes = "Removes scope excluded concept from a map project.", response = Response.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public void removeScopeExcludedConceptFromMapProject(
    @ApiParam(value = "ExcludedConcept to add", required = true) String terminologyId,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcepts");
    String projectName = "(not retrieved)";
    String user = "(not retrieved)";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to remove scope excluded concepts.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(projectId);

      mapProject.removeScopeExcludedConcept(terminologyId);
      mappingService.updateMapProject(mapProject);

      mappingService.close();

    } catch (Exception e) {
      this.handleException(e,
          "trying to remove scope excluded concept from project", user,
          projectName, "");
    }
  }

  /**
   * Returns the user for a given id (auto-generated) in JSON format.
   *
   * @param mapUserName the map user name
   * @param authToken the auth token
   * @return the mapUser
   */
  @GET
  @Path("/user/id/{username}")
  @ApiOperation(value = "Get user by username.", notes = "Gets a user by a username.", response = MapUser.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapUser getMapUser(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String mapUserName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): user/id/" + mapUserName);

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
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
   * Adds a map user.
   *
   * @param mapUser the map user
   * @param authToken the auth token
   * @return Response the response
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/user/add")
  @ApiOperation(value = "Add a user.", notes = "Adds the specified user.", response = MapUserJpa.class)
  public MapUser addMapUser(
    @ApiParam(value = "User, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /user/add");
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add a user.").build());

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
   * Updates a map user.
   *
   * @param mapUser the map user to be added
   * @param authToken the auth token
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/user/update")
  @ApiOperation(value = "Update a user.", notes = "Updates the specified user.", response = MapUserJpa.class)
  public void updateMapUser(
    @ApiParam(value = "User, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /user/update");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to update a user.").build());

      MappingService mappingService = new MappingServiceJpa();
      mappingService.updateMapUser(mapUser);
      mappingService.close();
    } catch (Exception e) {
      handleException(e, "trying to update a user", user, "", "");
    }
  }

  /**
   * Removes a map user.
   *
   * @param mapUser the map user
   * @param authToken the auth token
   */
  @DELETE
  @Path("/user/delete")
  @ApiOperation(value = "Remove a user.", notes = "Removes the specified user.", response = MapUser.class)
  public void removeMapUser(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /user/delete for user " + mapUser.getName());

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove a user.").build());

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
   * Returns all map advices in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map advices
   */
  @GET
  @Path("/advice/advices")
  @ApiOperation(value = "Get all mapping advices.", notes = "Gets a list of all map advices.", response = MapAdviceListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapAdviceListJpa getMapAdvices(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /advice/advices");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to retrieve the map advices.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapAdviceListJpa mapAdvices =
          (MapAdviceListJpa) mappingService.getMapAdvices();
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
   * Adds a map advice.
   *
   * @param mapAdvice the map advice
   * @param authToken the auth token
   * @return Response the response
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/advice/add")
  @ApiOperation(value = "Add an advice.", notes = "Adds the specified map advice.", response = MapAdviceJpa.class)
  public MapUser addMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data", required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /advice/add");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add an advice.").build());

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
   * Updates a map advice.
   *
   * @param mapAdvice the map advice to be added
   * @param authToken the auth token
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/advice/update")
  @ApiOperation(value = "Update an advice.", notes = "Updates the specified advice.", response = MapAdviceJpa.class)
  public void updateMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data", required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /advice/update");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
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
   * Removes a map advice.
   *
   * @param mapAdvice the map advice
   * @param authToken the auth token
   */
  @DELETE
  @Path("/advice/delete")
  @ApiOperation(value = "Remove an advice.", notes = "Removes the specified map advice.", response = MapAdviceJpa.class)
  public void removeMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data", required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /advice/delete for user "
            + mapAdvice.getName());

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove a user.").build());

      MappingService mappingService = new MappingServiceJpa();
      mappingService.removeMapAdvice(mapAdvice.getId());
      mappingService.close();
    } catch (Exception e) {
      LocalException le =
          new LocalException(
              "Unable to delete map advice. This is likely because the advice is being used by a map project or map entry");
      handleException(le, "", user, "", "");
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map AgeRange
  // ///////////////////////////////////////////////////
  /**
   * Returns all map age ranges in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map age ranges
   */
  @GET
  @Path("/ageRange/ageRanges")
  @ApiOperation(value = "Get all mapping age ranges.", notes = "Gets a list of all mapping age ranges.", response = MapAgeRangeListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapAgeRangeListJpa getMapAgeRanges(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /ageRange/ageRanges");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map age ranges.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapAgeRangeListJpa mapAgeRanges =
          (MapAgeRangeListJpa) mappingService.getMapAgeRanges();
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
   * Adds a map age range.
   *
   * @param mapAgeRange the map ageRange
   * @param authToken the auth token
   * @return Response the response
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/ageRange/add")
  @ApiOperation(value = "Add an age range.", notes = "Adds the specified age range.", response = MapAgeRangeJpa.class)
  public MapUser addMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data", required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /ageRange/add");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
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
   * Updates a map age range.
   *
   * @param mapAgeRange the map ageRange to be added
   * @param authToken the auth token
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/ageRange/update")
  @ApiOperation(value = "Update an age range.", notes = "Updates the specified age range.", response = MapAgeRangeJpa.class)
  public void updateMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data", required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /ageRange/update");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
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
   * Removes a map age range.
   *
   * @param mapAgeRange the map age range
   * @param authToken the auth token
   */
  @DELETE
  @Path("/ageRange/delete")
  @ApiOperation(value = "Remove an age range.", notes = "Removes the specified age range.", response = MapAgeRangeJpa.class)
  public void removeMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data", required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /ageRange/delete for user "
            + mapAgeRange.getName());

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
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
   * Returns all map relations in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map relations
   */
  @GET
  @Path("/relation/relations")
  @ApiOperation(value = "Get all relations.", notes = "Gets a list of all map relations.", response = MapRelationListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRelationListJpa getMapRelations(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /relation/relations");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to return the map relations.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapRelationListJpa mapRelations =
          (MapRelationListJpa) mappingService.getMapRelations();
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
   * Adds a map relation.
   *
   * @param mapRelation the map relation
   * @param authToken the auth token
   * @return Response the response
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/relation/add")
  @ApiOperation(value = "Add a map relation.", notes = "Adds the specified map relation.", response = MapRelationJpa.class)
  public MapUser addMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data", required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /relation/add");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
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
   * Updates a map relation.
   *
   * @param mapRelation the map relation to be added
   * @param authToken the auth token
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/relation/update")
  @ApiOperation(value = "Update a map relation.", notes = "Updates the specified map relation.", response = MapRelationJpa.class)
  public void updateMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data", required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /relation/update");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
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
   * Removes a map relation.
   *
   * @param mapRelation the map relation
   * @param authToken the auth token
   */
  @DELETE
  @Path("/relation/delete")
  @ApiOperation(value = "Remove a map relation.", notes = "Removes the specified map relation.", response = MapRelationJpa.class)
  public void removeMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data", required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /relation/delete for user "
            + mapRelation.getName());

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to remove a relation.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      mappingService.removeMapRelation(mapRelation.getId());
      mappingService.close();
    } catch (Exception e) {
      LocalException le =
          new LocalException(
              "Unable to delete map relation. This is likely because the relation is being used by a map project or map entry");
      handleException(le, "", user, "", "");
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Principles
  // ///////////////////////////////////////////////////

  /**
   * Returns all map principles in either JSON or XML format.
   *
   * @param authToken the auth token
   * @return the map principles
   */
  @GET
  @Path("/principle/principles")
  @ApiOperation(value = "Get all map principles.", notes = "Gets a list of all map principles.", response = MapPrincipleListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapPrincipleListJpa getMapPrinciples(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /principle/principles");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to return the map principles.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapPrincipleListJpa mapPrinciples =
          (MapPrincipleListJpa) mappingService.getMapPrinciples();
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
   * @param mapPrincipleId the map principle id
   * @param authToken the auth token
   * @return the map principle for id
   */
  @GET
  @Path("/principle/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get a map principle.", notes = "Gets a map principle for the specified id.", response = MapPrinciple.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapPrinciple getMapPrinciple(
    @ApiParam(value = "Map principle identifer", required = true) @PathParam("id") Long mapPrincipleId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /principle/id/" + mapPrincipleId.toString());

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map principle.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapPrinciple mapPrinciple =
          mappingService.getMapPrinciple(mapPrincipleId);
      mappingService.close();
      return mapPrinciple;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the map principle", user, "", "");
      return null;
    }
  }

  /**
   * Adds a map user principle object.
   *
   * @param mapPrinciple the map user principle object to be added
   * @param authToken the auth token
   * @return result the newly created map user principle object
   */
  @PUT
  @Path("/principle/add")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Add a map principle.", notes = "Adds the specified map principle.", response = MapPrincipleJpa.class)
  public MapPrinciple addMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data", required = true) MapPrincipleJpa mapPrinciple,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /principle/add");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
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
   * Updates a map principle.
   *
   * @param mapPrinciple the map principle
   * @param authToken the auth token
   */
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/principle/update")
  @ApiOperation(value = "Update a map principle.", notes = "Updates the specified map principle.", response = MapPrincipleJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public void updateMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data", required = true) MapPrincipleJpa mapPrinciple,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /principle/update");

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to update a map principle.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      mappingService.updateMapPrinciple(mapPrinciple);
      mappingService.close();

    } catch (Exception e) {
      handleException(e, "trying to update a map principle", user, "", "");
    }
  }

  /**
   * Removes a set of map user preferences.
   *
   * @param principle the principle
   * @param authToken the auth token
   */
  @DELETE
  @Path("/principle/delete")
  @ApiOperation(value = "Remove a map principle.", notes = "Removes the specified map principle.")
  public void removeMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data", required = true) MapPrincipleJpa principle,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /principle/delete for id "
            + principle.getId().toString());

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to remove a map principle.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      mappingService.removeMapPrinciple(principle.getId());
      mappingService.close();
    } catch (Exception e) {
      LocalException le =
          new LocalException(
              "Unable to delete map principle. This is likely because the principle is being used by a map project or map record");
      handleException(le, "", user, "", "");
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map User Preferences
  // ///////////////////////////////////////////////////

  /**
   * Gets a map user preferences object for a specified user.
   *
   * @param username the username
   * @param authToken the auth token
   * @return result the newly created map user preferences object
   */
  @GET
  @Path("/userPreferences/user/id/{username}")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get user preferences.", notes = "Gets user preferences for the specified username.", response = MapUserPreferencesJpa.class)
  public MapUserPreferences getMapUserPreferences(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String username,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call:  /userPreferences/user/id/" + username);

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to return the map user preferences.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapUserPreferences result =
          mappingService.getMapUserPreferences(username);
      mappingService.close();
      return result;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the map user preferences", user,
          "", "");
      return null;
    }
  }

  /**
   * Adds a map user preferences object.
   *
   * @param mapUserPreferences the map user preferences object to be added
   * @param authToken the auth token
   * @return result the newly created map user preferences object
   */
  @PUT
  @Path("/userPreferences/add")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Add user preferences.", notes = "Adds the specified user preferences.", response = MapUserPreferencesJpa.class)
  public MapUserPreferences addMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data", required = true) MapUserPreferencesJpa mapUserPreferences,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /userPreferences/add");

    String user = "";
    try {
      // authorize call -- note that any user must be able to add their
      // own preferences, therefore this /add call only requires VIEWER
      // role
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to add map user preferences.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapUserPreferences result =
          mappingService.addMapUserPreferences(mapUserPreferences);
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
   * @param mapUserPreferences the map user preferences
   * @param authToken the auth token
   */
  @POST
  @Path("/userPreferences/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update user preferences.", notes = "Updates the specified user preferences.", response = MapUserPreferencesJpa.class)
  public void updateMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data", required = true) MapUserPreferencesJpa mapUserPreferences,
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
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to update map user preferences.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      mappingService.updateMapUserPreferences(mapUserPreferences);
      mappingService.close();

    } catch (Exception e) {
      handleException(e, "trying to update map user preferences", user, "", "");
    }

  }

  /**
   * Removes a set of map user preferences.
   *
   * @param mapUserPreferences the id of the map user preferences object to be
   *          deleted
   * @param authToken the auth token
   */
  @DELETE
  @Path("/userPreferences/remove")
  @ApiOperation(value = "Remove user preferences.", notes = "Removes specified user preferences.")
  public void removeMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data", required = true) MapUserPreferencesJpa mapUserPreferences,
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
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to remove map user preferences.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      mappingService.removeMapUserPreferences(mapUserPreferences.getId());
      mappingService.close();
    } catch (Exception e) {
      handleException(e, "trying to remove map user preferences", user, "", "");
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Record
  // ///////////////////////////////////////////////////

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Record
  // ///////////////////////////////////////////////////

  /**
   * Returns the record for a given id (auto-generated) in JSON format.
   *
   * @param mapRecordId the mapRecordId
   * @param authToken the auth token
   * @return the mapRecord
   */
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get map record by id.", notes = "Gets a map record for the specified id.", response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecord getMapRecord(
    @ApiParam(value = "Map record id", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/id/"
            + (mapRecordId == null ? "" : mapRecordId.toString()));

    String user = "";
    MapRecord mapRecord = null;
    try {

      MappingService mappingService = new MappingServiceJpa();
      mapRecord = mappingService.getMapRecord(mapRecordId);
      mappingService.close();

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to retrieve the map record.")
            .build());

      // remove notes if this is not a specialist or above
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        mapRecord.setMapNotes(null);
      }

      return mapRecord;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the map record", user,
          mapRecord == null ? "" : mapRecord.getMapProjectId().toString(),
          mapRecordId == null ? "" : mapRecordId.toString());
      return null;
    }
  }

  /**
   * Adds a map record.
   *
   * @param mapRecord the map record to be added
   * @param authToken the auth token
   * @return Response the response
   */
  @PUT
  @Path("/record/add")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Add a map record.", notes = "Adds the specified map record.", response = MapRecordJpa.class)
  public MapRecord addMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/add");

    String user = "";
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add a map record.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapRecord result = mappingService.addMapRecord(mapRecord);
      mappingService.close();
      return result;
    } catch (Exception e) {
      handleException(e, "trying to add a map record", user, mapRecord
          .getMapProjectId().toString(), mapRecord.getId().toString());
      return null;
    }
  }

  /**
   * Updates a map record.
   *
   * @param mapRecord the map record to be added
   * @param authToken the auth token
   */
  @POST
  @Path("/record/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a map record.", notes = "Updates the specified map record.", response = Response.class)
  public void updateMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/update");

    String user = "";
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to update the map record.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      mappingService.updateMapRecord(mapRecord);
      mappingService.close();
    } catch (Exception e) {
      handleException(e, "trying to update the map record", user, mapRecord
          .getMapProjectId().toString(), mapRecord.getId().toString());
    }
  }

  /**
   * Removes a map record given the object.
   *
   * @param mapRecord the map record to delete
   * @param authToken the auth token
   * @return Response the response
   */
  @DELETE
  @Path("/record/delete")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Remove a map record.", notes = "Removes the specified map record.", response = MapRecordJpa.class)
  public Response removeMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
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
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to delete the map record.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      mappingService.removeMapRecord(mapRecord.getId());
      mappingService.close();
      return null;
    } catch (Exception e) {
      handleException(e, "trying to delete the map record", user, mapRecord
          .getMapProjectId().toString(), "");
      return null;
    }
  }

  /**
   * Removes a set of map records for a project and a set of terminology ids.
   *
   * @param terminologyIds the terminology ids
   * @param projectId the project id
   * @param authToken the auth token
   * @return Response the response
   */
  @DELETE
  @Path("/record/records/delete/project/id/{projectId}/batch")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Remove a set of map records.", notes = "Removes map records for specified project and a set of concept terminology ids", response = List.class)
  public ValidationResult removeMapRecordsForMapProjectAndTerminologyIds(
    @ApiParam(value = "Terminology ids, in JSON or XML POST data", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/records/delete/project/id/"
            + projectId + "/batch with string argument " + terminologyIds);

    String user = "";
    String projectName = "(not retrieved)";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to delete the map record.")
            .build());

      // validation report to return errors and warnings
      ValidationResult validationResult = new ValidationResultJpa();

      // instantiate the needed services
      ContentService contentService = new ContentServiceJpa();
      MappingService mappingService = new MappingServiceJpa();

      // retrieve the map project
      MapProject mapProject = mappingService.getMapProject(projectId);
      projectName = mapProject.getName();

      // construct a list of terminology ids to remove
      // initially set to the Api argument
      // (instantiated to avoid concurrent modification errors
      // when modifying the list for descendant concepts)
      List<String> terminologyIdsToRemove = new ArrayList<>(terminologyIds);

      int nRecordsRemoved = 0;
      int nScopeConceptsRemoved = 0;

      validationResult.addMessage(terminologyIds.size()
          + " concepts selected for map record removal");

      // cycle over the terminology ids
      for (String terminologyId : terminologyIdsToRemove) {

        // retrieve all map records for this project and concept
        MapRecordList mapRecordList =
            mappingService.getMapRecordsForProjectAndConcept(projectId,
                terminologyId);

        // check if map records exist
        if (mapRecordList.getCount() == 0) {
          Logger.getLogger(MappingServiceRest.class).warn(
              "No records found for project for concept id " + terminologyId);
          validationResult.addWarning("No records found for concept "
              + terminologyId);
        } else {
          for (MapRecord mapRecord : mapRecordList.getMapRecords()) {
            Logger.getLogger(MappingServiceRest.class).info(
                "Removing map record " + mapRecord.getId() + " for concept "
                    + mapRecord.getConceptId() + ", "
                    + mapRecord.getConceptName());

            // remove the map record
            mappingService.removeMapRecord(mapRecord.getId());

            // increment the counts
            nRecordsRemoved++;
          }
        }

        // if a non-descendant-based project (i.e. enumerated scope), remove
        // scope concept
        if (mapProject.isScopeDescendantsFlag() == false) {

          // remove this terminology id from the scope concepts
          if (mapProject.getScopeConcepts().contains(terminologyId)) {
            mapProject.removeScopeConcept(terminologyId);
            nScopeConceptsRemoved++;
          }

          // update the map project
          mappingService.updateMapProject(mapProject);

        }

      }

      // add the counter information to the validation result
      validationResult.addMessage(nRecordsRemoved
          + " records successfully removed");

      // if scope concepts were removed, add a success message
      if (mapProject.isScopeDescendantsFlag() == false) {

        validationResult.addMessage(nScopeConceptsRemoved
            + " concepts removed from project scope definition");
      }
      // close the services and return the validation result
      contentService.close();
      mappingService.close();
      return validationResult;

    } catch (Exception e) {
      handleException(e, "trying to delete map records by terminology id",
          user, terminologyIds.toString(), projectName);
      return null;
    }
  }

  /**
   * Returns the records for a given concept id. We don't need to know
   * terminology or version here because we can get it from the corresponding
   * map project.
   *
   * @param conceptId the concept id
   * @param authToken the auth token
   * @return the mapRecords
   */
  @GET
  @Path("/record/concept/id/{conceptId}")
  @ApiOperation(value = "Get map records by concept id.", notes = "Gets a list of map records for the specified concept id.", response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsForConceptId(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/concept/id/" + conceptId);

    String user = "";
    try {
      // authorize call
      MapUserRole applicationRole =
          securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);

      if (!applicationRole.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to find records by the given concept id.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapRecordListJpa mapRecordList =
          (MapRecordListJpa) mappingService.getMapRecordsForConcept(conceptId);

      // return records that this user does not have permission to see
      MapUser mapUser =
          mappingService.getMapUser(securityService
              .getUsernameForToken(authToken));
      List<MapRecord> mapRecords = new ArrayList<>();

      // cycle over records and determine if this user can see them
      for (MapRecord mr : mapRecordList.getMapRecords()) {

        // get the user's role for this record's project
        MapUserRole projectRole =
            securityService.getMapProjectRoleForToken(authToken,
                mr.getMapProjectId());

        // remove notes if this is not a specialist or above
        if (!projectRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }

        // System.out.println(projectRole + " " + mr.toString());

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

      // set the list of records to the filtered object and return
      mapRecordList.setMapRecords(mapRecords);

      mappingService.close();
      return mapRecordList;
    } catch (Exception e) {
      handleException(e, "trying to find records by the given concept id",
          user, "", conceptId);
      return null;
    }
  }

  /**
   * Gets the latest map record revision for each map record with given concept
   * id.
   * 
   * @param conceptId the concept id
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the map records for concept id historical
   */
  @GET
  @Path("/record/concept/id/{conceptId}/project/id/{id:[0-9][0-9]*}/historical")
  @ApiOperation(value = "Get historical map records by concept id.", notes = "Gets the latest map record revision for each map record with given concept id.", response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsForConceptIdHistorical(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/concept/id/" + conceptId
            + "/project/id/" + mapProjectId + "/historical");

    String user = "";
    try {
      // authorize call
      MapUserRole applicationRole =
          securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);

      if (!applicationRole.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to find historical records by the given concept id.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapRecordListJpa mapRecordList =
          (MapRecordListJpa) mappingService.getMapRecordRevisionsForConcept(
              conceptId, mapProjectId);

      // return records that this user does not have permission to see
      MapUser mapUser =
          mappingService.getMapUser(securityService
              .getUsernameForToken(authToken));
      List<MapRecord> mapRecords = new ArrayList<>();

      // cycle over records and determine if this user can see them
      for (MapRecord mr : mapRecordList.getMapRecords()) {

        // get the user's role for this record's project
        MapUserRole projectRole =
            securityService.getMapProjectRoleForToken(authToken,
                mr.getMapProjectId());

        // remove notes if this is not a specialist or above
        if (!projectRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }

        // System.out.println(projectRole + " " + mr.toString());

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

      // set the list of records to the filtered object and return
      mapRecordList.setMapRecords(mapRecords);

      mappingService.close();
      return mapRecordList;
    } catch (Exception e) {
      handleException(e,
          "trying to find historical records by the given concept id", user,
          "", conceptId);
      return null;
    }
  }

  /**
   * Returns delimited page of Published or Ready For Publication MapRecords
   * given a paging/filtering/sorting parameters object.
   *
   * @param mapProjectId the map project id
   * @param pfsParameter the JSON object containing the paging/filtering/sorting
   *          parameters
   * @param authToken the auth token
   * @return the list of map records
   */
  @POST
  @Path("/record/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get published map records by project id.", notes = "Gets a list of map records for the specified map project id that have a workflow status of PUBLISHED or READY_FOR_PUBLICATION.", response = MapRecordListJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @CookieParam(value = "userInfo")
  public MapRecordListJpa getPublishedAndReadyForPublicationMapRecordsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/project/id/" + mapProjectId.toString()
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String user = "";
    // execute the service call
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the publication-ready map records for a map project.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapRecordListJpa mapRecordList =
          (MapRecordListJpa) mappingService
              .getPublishedAndReadyForPublicationMapRecordsForMapProject(
                  mapProjectId, pfsParameter);

      for (MapRecord mr : mapRecordList.getMapRecords()) {
        // remove notes if this is not a specialist or above
        if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }
      }
      mappingService.close();
      return mapRecordList;
    } catch (Exception e) {
      handleException(e,
          "trying to retrieve the map records for a map project", user,
          mapProjectId.toString(), "");
      return null;
    }

  }

  /**
   * Returns delimited page of Published or Ready For Publication MapRecords
   * given a paging/filtering/sorting parameters object.
   *
   * @param mapProjectId the map project id
   * @param pfsParameter the JSON object containing the paging/filtering/sorting
   *          parameters
   * @param authToken the auth token
   * @return the list of map records
   */
  @POST
  @Path("/record/project/id/{id:[0-9][0-9]*}/published")
  @ApiOperation(value = "Get published map records by map project id.", notes = "Gets a list of map records for the specified map project id that have a workflow status of PUBLISHED.", response = MapRecordListJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @CookieParam(value = "userInfo")
  public MapRecordListJpa getPublishedMapRecordsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/project/id/" + mapProjectId.toString()
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getQueryRestriction());

    String user = "";
    // execute the service call
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map records for a map project.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapRecordListJpa mapRecordList =
          (MapRecordListJpa) mappingService
              .getPublishedMapRecordsForMapProject(mapProjectId, pfsParameter);

      for (MapRecord mr : mapRecordList.getMapRecords()) {
        // remove notes if this is not a specialist or above
        if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }
      }
      mappingService.close();
      return mapRecordList;
    } catch (Exception e) {
      handleException(e,
          "trying to retrieve the map records for a map project", user,
          mapProjectId.toString(), "");
      return null;
    }

  }

  /**
   * Returns the map record revisions.
   * 
   * NOTE: currently not called, but we are going to want to call this to do
   * history-related stuff thus it is anticipating the future dev and should be
   * kept.
   *
   * @param mapRecordId the map record id
   * @param authToken the auth token
   * @return the map record revisions
   */
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/revisions")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get map record revision history.", notes = "Gets a list of all revisions of a map record for the specified id.", response = MapRecordListJpa.class)
  public MapRecordList getMapRecordRevisions(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/id/" + mapRecordId + "/revisions");

    String user = "";
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapRecordId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map record revisions.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapRecordList revisions =
          mappingService.getMapRecordRevisions(mapRecordId);

      for (MapRecord mr : revisions.getMapRecords()) {
        // remove notes if this is not a specialist or above
        if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }
      }
      mappingService.close();
      return revisions;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the map record revisions", user,
          "", mapRecordId.toString());
      return null;
    }

  }

  /**
   * Returns the map record using historical revisions if the record no longer
   * exists.
   * 
   * @param mapRecordId the map record id
   * @param authToken the auth token
   * @return the map record historical
   */
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/historical")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get latest state of a map record.", notes = "Gets the current form of the map record or its last historical state for the specified map record id.", response = MapRecordListJpa.class)
  public MapRecord getMapRecordHistorical(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long mapRecordId,
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
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map record potentially using historical revisions.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      // try getting the current record
      MapRecord mapRecord = mappingService.getMapRecord(mapRecordId);

      // if no current record, look for revisions
      if (mapRecord == null) {
        mapRecord =
            mappingService.getMapRecordRevisions(mapRecordId).getMapRecords()
                .get(0);
      }

      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        mapRecord.setMapNotes(null);
      }

      mappingService.close();
      return mapRecord;

    } catch (Exception e) {
      handleException(
          e,
          "trying to retrieve the map record potentially using historical revisions",
          user, "", mapRecordId.toString());
      return null;
    }

  }

  // //////////////////////////////////////////
  // Relation and Advice Computation
  // /////////////////////////////////////////

  /**
   * Computes a map relation (if any) for a map entry's current state.
   *
   * @param mapEntry the map entry
   * @param authToken the auth token
   * @return Response the response
   */
  @POST
  @Path("/relation/compute")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Compute map relation.", notes = "Gets the computed map relation for the specified map entry.", response = MapRelationJpa.class)
  public MapRelation computeMapRelation(
    @ApiParam(value = "Map entry, in JSON or XML POST data", required = true) MapEntryJpa mapEntry,
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
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        mappingService.close();
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to compute the map relation.")
            .build());
      }

      // System.out.println(mapRecord.toString());
      if (mapRecord.getMapProjectId() == null) {
        return null;
      }
      // System.out.println("Retrieving project handler");

      ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mappingService
              .getMapProject(mapRecord.getMapProjectId()));

      MapRelation mapRelation =
          algorithmHandler.computeMapRelation(mapRecord, mapEntry);
      mappingService.close();
      return mapRelation;

    } catch (Exception e) {
      handleException(e, "trying to compute the map relations", user,
          mapRecord == null ? "" : mapRecord.getMapProjectId().toString(),
          mapRecord == null ? "" : mapRecord.getId().toString());
      return null;
    }
  }

  /**
   * Computes a map advice (if any) for a map entry's current state.
   *
   * @param mapEntry the map entry
   * @param authToken the auth token
   * @return Response the response
   */
  @POST
  @Path("/advice/compute")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Compute map advices.", notes = "Gets the computed map advices for the specified map entry.", response = MapAdviceJpa.class)
  public MapAdviceList computeMapAdvice(
    @ApiParam(value = "Map entry, in JSON or XML POST data", required = true) MapEntryJpa mapEntry,
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
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        mappingService.close();
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to compute the map advice.")
                .build());
      }

      ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mappingService
              .getMapProject(mapRecord.getMapProjectId()));

      MapAdviceList mapAdviceList =
          algorithmHandler.computeMapAdvice(mapRecord, mapEntry);
      mappingService.close();
      return mapAdviceList;

    } catch (Exception e) {
      handleException(e, "trying to compute the map advice", user,
          mapRecord == null ? "" : mapRecord.getMapProjectId().toString(),
          mapRecord == null ? "" : mapRecord.getId().toString());
      return null;
    }
  }

  // ///////////////////////////////////////////////
  // Role Management Services
  // ///////////////////////////////////////////////
  /**
   * Gets a map user's role for a given map project.
   *
   * @param username the username
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return result the role
   */
  @GET
  @Path("/userRole/user/id/{username}/project/id/{id:[0-9][0-9]*}")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get the user's role for a map project.", notes = "Gets the role for the specified user and map project.", response = SearchResultList.class)
  public MapUserRole getMapUserRoleForMapProject(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String username,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call:  /userRole/user/id" + username + "/project/id/"
            + mapProjectId);

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapUserRole mapUserRole =
          mappingService.getMapUserRoleForMapProject(username, mapProjectId);
      mappingService.close();
      return mapUserRole;
    } catch (Exception e) {
      handleException(e, "trying to get the map user role for a map project",
          username, mapProjectId.toString(), "");
      return null;
    }
  }

  // /////////////////////////
  // Descendant services
  // /////////////////////////

  /**
   * TODO: Make this project specific
   * 
   * Given concept information, returns a ConceptList of descendant concepts
   * without associated map records.
   * 
   * @param terminologyId the concept terminology id
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the ConceptList of unmapped descendants
   */
  @GET
  @Path("/concept/id/{terminologyId}/unmappedDescendants/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Find unmapped descendants of a concept.", notes = "Gets a list of search results for concepts having unmapped descendants.", response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getUnmappedDescendantsForConcept(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("id") String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /concept/id/" + terminologyId + "/project/id/"
            + mapProjectId);

    String user = "";
    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve unmapped descendants for a concept.")
                .build());

      MappingService mappingService = new MappingServiceJpa();

      SearchResultList results =
          mappingService.findUnmappedDescendantsForConcept(terminologyId,
              mapProjectId, null);

      mappingService.close();
      return results;

    } catch (Exception e) {
      handleException(e,
          "trying to retrieve unmapped descendants for a concept", user, "",
          terminologyId);
      return null;
    }
  }

  // /////////////////////////////////////////////////////
  // Tree Position Routines for Terminology Browser
  // /////////////////////////////////////////////////////
  /**
   * Gets tree positions for concept.
   *
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param mapProjectId the contextual project of this tree, used for
   *          determining valid codes
   * @param authToken the auth token
   * @return the search result list
   */
  @GET
  @Path("/treePosition/project/id/{mapProjectId}/concept/id/{terminology}/{terminologyVersion}/{terminologyId}")
  @ApiOperation(value = "Gets project-specific tree positions with desendants.", notes = "Gets a list of tree positions and their descendants for the specified parameters. Sets flags for valid targets and assigns any terminology notes based on project.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getTreePositionsWithDescendants(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken

  ) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /treePosition/project/id/"
            + mapProjectId.toString() + "/concept/id/" + terminology + "/"
            + terminologyVersion + "/" + terminologyId);

    String user = "";
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get the tree positions with descendants.")
                .build());

      // get the local tree positions from content service
      ContentService contentService = new ContentServiceJpa();
      TreePositionList treePositions =
          contentService.getTreePositionsWithChildren(terminologyId,
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
      handleException(e, "trying to get the tree positions with descendants",
          user, mapProjectId.toString(), terminologyId);
      return null;
    }
  }

  /**
   * Gets the root-level tree positions for a given terminology and version.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the search result list
   */
  @GET
  @Path("/treePosition/project/id/{projectId}/terminology/id/{terminology}/{terminologyVersion}")
  @ApiOperation(value = "Get root tree positions.", notes = "Gets a list of tree positions at the root of the terminology.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getRootTreePositionsForTerminology(
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /treePosition/project/id/"
            + mapProjectId.toString() + "/terminology/id/" + terminology + "/"
            + terminologyVersion);

    String user = "";
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get the root tree positions for a terminology.")
                .build());

      // get the root tree positions from content service
      ContentService contentService = new ContentServiceJpa();
      TreePositionList treePositions =
          contentService.getRootTreePositions(terminology, terminologyVersion);
      contentService.computeTreePositionInformation(treePositions);
      contentService.close();

      // set the valid codes using mapping service
      MappingService mappingService = new MappingServiceJpa();
      mappingService.setTreePositionValidCodes(
          treePositions.getTreePositions(), mapProjectId);
      mappingService.close();

      return treePositions;
    } catch (Exception e) {
      handleException(e,
          "trying to get the root tree positions for a terminology", user,
          mapProjectId.toString(), "");
      return null;
    }
  }

  /**
   * Gets tree positions for concept query.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param query the query
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the root-level trees corresponding to the query
   */
  @GET
  @Path("/treePosition/project/id/{projectId}/terminology/id/{terminology}/{terminologyVersion}/query/{query}")
  @ApiOperation(value = "Get tree positions for query.", notes = "Gets a list of tree positions for the specified parameters.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getTreePositionGraphsForQuery(
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Terminology browser query, e.g. 'cholera'", required = true) @PathParam("query") String query,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(ContentServiceJpa.class).info(
        "RESTful call (Mapping): /treePosition/project/id/" + mapProjectId
            + "/terminology/id/" + terminology + "/" + terminologyVersion
            + "/query/" + query);

    String user = "";
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get the tree position graphs for a query.")
                .build());

      // get the tree positions from concept service
      ContentService contentService = new ContentServiceJpa();
      TreePositionList treePositions =
          contentService.getTreePositionGraphForQuery(terminology,
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
      handleException(e, "trying to get the tree position graphs for a query",
          user, mapProjectId.toString(), "");
      return null;
    }
  }

  // //////////////////////////////////////////////////
  // Workflow-related routines
  // /////////////////////////////////////////////////

  /**
   * Returns records recently edited for a project and user. Used by editedList
   * widget.
   *
   * @param mapProjectId the map project id
   * @param username the user name
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the recently edited map records
   */
  @POST
  @Path("/record/project/id/{id}/user/id/{username}/edited")
  @ApiOperation(value = "Get map records edited by a user.", notes = "Gets a list of map records for the specified map project and user.", response = MapRecordListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsEditedByMapUser(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") String mapProjectId,
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String username,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/project/id/" + mapProjectId
            + "/user/id" + username + "/edited");

    String user = "";
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, new Long(
              mapProjectId));
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get the recently edited map records.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapRecordListJpa recordList =
          (MapRecordListJpa) mappingService.getRecentlyEditedMapRecords(
              new Long(mapProjectId), username, pfsParameter);
      mappingService.close();
      return recordList;
    } catch (Exception e) {
      handleException(e, "trying to get the recently edited map records", user,
          mapProjectId.toString(), "");
      return null;
    }
  }

  /**
   * Returns the map records that when compared lead to the specified conflict
   * record.
   *
   * @param mapRecordId the map record id
   * @param authToken the auth token
   * @return map records in conflict for a given conflict lead record
   * @throws Exception the exception
   */
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/conflictOrigins")
  @ApiOperation(value = "Get specialist records for an assigned conflict or review record.", notes = "Gets a list of specialist map records corresponding to a lead conflict or review record.", response = MapRecordListJpa.class)
  public MapRecordList getOriginMapRecordsForConflict(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/id/" + mapRecordId
            + "/conflictOrigins");
    String user = "";
    try {
      MappingService mappingService = new MappingServiceJpa();
      MapRecord mapRecord = mappingService.getMapRecord(mapRecordId);
      Logger.getLogger(MappingServiceRest.class).info(
          "  mapRecord.mapProjectId = " + mapRecord.getMapProjectId());

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      // needed at specialist level, so specialists can review on QA_PATH
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the origin map records for a conflict/review.")
                .build());

      MapRecordList records = new MapRecordListJpa();

      records = mappingService.getOriginMapRecordsForConflict(mapRecordId);
      mappingService.close();

      return records;
    } catch (Exception e) {
      handleException(e,
          "trying to retrieve origin records for conflict/review", user, "",
          mapRecordId.toString());
      return null;
    }

  }

  // //////////////////////////////////////////////
  // Map Record Validation and Compare Services
  // //////////////////////////////////////////////

  /**
   * Validates a map record.
   *
   * @param mapRecord the map record to be validated
   * @param authToken the auth token
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
  @ApiOperation(value = "Validate a map record.", notes = "Performs validation checks on a map record and returns the validation results.", response = MapRecordJpa.class)
  public ValidationResult validateMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /validation/record/validate for map record id = "
            + mapRecord.getId().toString());

    // get the map project for this record

    String user = "";
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to validate a map record.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject;
      mapProject = mappingService.getMapProject(mapRecord.getMapProjectId());
      ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);

      ValidationResult validationResult =
          algorithmHandler.validateRecord(mapRecord);
      mappingService.close();
      return validationResult;
    } catch (Exception e) {
      handleException(e, "trying to validate a map record", user, mapRecord
          .getMapProjectId().toString(), mapRecord.getId().toString());
      return null;
    }
  }

  /**
   * Compare map records and return differences.
   *
   * @param mapRecordId1 the map record id1
   * @param mapRecordId2 the map record id2
   * @param authToken the auth token
   * @return the validation result
   */
  @GET
  @Path("/validation/record/id/{recordId1}/record/id/{recordId2}/compare")
  @ApiOperation(value = "Compare two map records.", notes = "Compares two map records and returns the validation results.", response = ValidationResultJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ValidationResult compareMapRecords(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("recordId1") Long mapRecordId1,
    @ApiParam(value = "Map record id, e.g. 28124", required = true) @PathParam("recordId2") Long mapRecordId2,
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
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapRecord1.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to compare map records.")
            .build());

      MapProject mapProject =
          mappingService.getMapProject(mapRecord1.getMapProjectId());
      ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      ValidationResult validationResult =
          algorithmHandler.compareMapRecords(mapRecord1, mapRecord2);

      mappingService.close();
      return validationResult;

    } catch (Exception e) {
      handleException(e, "trying to compare map records", user, "",
          mapRecordId1.toString());
      return null;
    }
  }

  /**
   * Is target code valid.
   * 
   * @param mapProjectId the map project id
   * @param terminologyId the terminology id
   * @param authToken the auth token
   * @return the concept
   */
  @GET
  @Path("/project/id/{mapProjectId}/concept/{terminologyId}/isValid")
  @ApiOperation(value = "Indicate whether a target code is valid.", notes = "Gets either a valid concept corresponding to the id, or returns null if not valid.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Concept isTargetCodeValid(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/id/" + mapProjectId + "/concept/"
            + terminologyId + "/isValid");

    String user = "";
    try {
      MappingService mappingService = new MappingServiceJpa();

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to check valid target codes")
            .build());

      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      boolean isValid = algorithmHandler.isTargetCodeValid(terminologyId);

      mappingService.close();
      if (isValid == true) {
        ContentService contentService = new ContentServiceJpa();
        Concept c =
            contentService.getConcept(terminologyId,
                mapProject.getDestinationTerminology(),
                mapProject.getDestinationTerminologyVersion());
        return c;
      } else {
        return null;
      }

    } catch (Exception e) {
      handleException(e, "trying to compare map records", user,
          mapProjectId.toString(), "");
      return null;
    }
  }

  /**
   * Upload file.
   * 
   * @param fileInputStream the file input stream
   * @param contentDispositionHeader the content disposition header
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the response
   */
  @POST
  @Path("/upload/{mapProjectId}")
  // Swagger does not support this
  @ApiOperation(value = "Upload a mapping handbook file for a project.", notes = "Uploads a mapping handbook file for the specified project.", response = TreePositionListJpa.class)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadMappingHandbookFile(
    @FormDataParam("file") InputStream fileInputStream,
    @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
    @PathParam("mapProjectId") Long mapProjectId,
    @HeaderParam("Authorization") String authToken) {

    String user = "";
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to check valid target codes")
            .build());

      // get destination directory for uploaded file
      Properties config = ConfigUtility.getConfigProperties();

      String docDir = config.getProperty("map.principle.source.document.dir");

      File dir = new File(docDir);
      File archiveDir = new File(docDir + "/archive");

      // compose the name of the stored file
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject =
          mappingService.getMapProject(new Long(mapProjectId));
      mappingService.close();
      SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");
      String date = dt.format(new Date());

      String extension = "";
      if (contentDispositionHeader.getFileName().indexOf(".") != -1) {
        extension =
            contentDispositionHeader.getFileName().substring(
                contentDispositionHeader.getFileName().lastIndexOf("."));
      }
      String fileName = contentDispositionHeader.getFileName()
          .substring(0,contentDispositionHeader.getFileName().lastIndexOf(".")-1)
          .replaceAll(" ","_");
      File file =
          new File(dir, mapProjectId + "_" + fileName + extension);
      File archiveFile =
          new File(archiveDir, mapProjectId + "_" + fileName + "."
              + date + extension);

      // save the file to the server
      saveFile(fileInputStream, file.getAbsolutePath());
      copyFile(file, archiveFile);

      String output =
          "File saved to server location : " + file.getAbsolutePath() + " and "
              + archiveFile.getAbsolutePath();

      // update project
      mapProject.setMapPrincipleSourceDocument(mapProjectId + "_"
          + fileName + extension);
      updateMapProject((MapProjectJpa) mapProject, authToken);

      return Response.status(200).entity(output).build();
    } catch (Exception e) {
      handleException(e, "trying to upload a file", user,
          mapProjectId.toString(), "");
      return null;
    }
  }

  /**
   * Returns all map projects metadata.
   *
   * @param authToken the auth token
   * @return the map projects metadata
   */
  @GET
  @Path("/mapProject/metadata")
  @ApiOperation(value = "Get metadata for map projects.", notes = "Gets the key-value pairs representing all metadata for the map projects.", response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getMapProjectMetadata(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /mapProject/metadata");

    String user = "";
    try {
      user = securityService.getUsernameForToken(authToken);

      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the map project metadata.")
                .build());

      // call jpa service and get complex map return type
      MappingService mappingService = new MappingServiceJpa();
      Map<String, Map<String, String>> mapOfMaps =
          mappingService.getMapProjectMetadata();

      // add project specific handlers
      // TODO: move this to jpa layer
      Reflections reflections =
          new Reflections(
              ClasspathHelper.forPackage("org.ihtsdo.otf.mapping.jpa.handlers"),
              new SubTypesScanner());
      Set<Class<? extends ProjectSpecificAlgorithmHandler>> implementingTypes =
          reflections.getSubTypesOf(ProjectSpecificAlgorithmHandler.class);

      Map<String, String> handlerMap = new HashMap<>();
      for (Class<? extends ProjectSpecificAlgorithmHandler> handler : implementingTypes) {
        handlerMap.put(handler.getName(), handler.getSimpleName());
      }
      if (handlerMap.size() > 0) {
        mapOfMaps.put("Project Specific Handlers", handlerMap);
      }

      // convert complex map to KeyValuePair objects for easy
      // transformation to
      // XML/JSON
      KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      for (Map.Entry<String, Map<String, String>> entry : mapOfMaps.entrySet()) {
        String metadataType = entry.getKey();
        Map<String, String> metadataPairs = entry.getValue();
        KeyValuePairList keyValuePairList = new KeyValuePairList();
        keyValuePairList.setName(metadataType);
        for (Map.Entry<String, String> pairEntry : metadataPairs.entrySet()) {
          KeyValuePair keyValuePair =
              new KeyValuePair(pairEntry.getKey().toString(),
                  pairEntry.getValue());
          keyValuePairList.addKeyValuePair(keyValuePair);
        }
        keyValuePairLists.addKeyValuePairList(keyValuePairList);
      }
      mappingService.close();
      return keyValuePairLists;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the map project metadata", user,
          "", "");
      return null;
    }
  }

  /**
   * Save uploaded file to a defined location on the server.
   * 
   * @param uploadedInputStream the uploaded input stream
   * @param serverLocation the server location
   */
  private void saveFile(InputStream uploadedInputStream, String serverLocation) {
    OutputStream outputStream = null;
    try {
      outputStream = new FileOutputStream(new File(serverLocation));
      int read = 0;
      byte[] bytes = new byte[1024];
      outputStream.close();
      outputStream = new FileOutputStream(new File(serverLocation));
      while ((read = uploadedInputStream.read(bytes)) != -1) {
        outputStream.write(bytes, 0, read);
      }
      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      try {
        if (outputStream != null) {
          outputStream.close();
        }
      } catch (IOException e1) {
        // do nothing
      }
      e.printStackTrace();
    }

  }

  /**
   * Copy file.
   * 
   * @param sourceFile the source file
   * @param destFile the dest file
   * @throws IOException Signals that an I/O exception has occurred.
   */
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
