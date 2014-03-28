package org.ihtsdo.otf.mapping.rest;

import java.util.ArrayList;
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
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.jpa.MapAdviceList;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleList;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectList;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordList;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationList;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
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
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@SuppressWarnings("static-method")
public class MappingServiceRest {

  /**
   * Instantiates an empty {@link MappingServiceRest}.
   */
  public MappingServiceRest() {

  }

  // ///////////////////////////////////////////////////
  // Mapping Objects: Retrieval (@GET) functions
  // - getMapProjects()
  // - getMapUsers()
  // - getMapProjectForId(Long mapProjectId)
  // - findMapProjectsForQuery(String query)
  // ///////////////////////////////////////////////////

  /**
   * Returns all map projects in either JSON or XML format
   * 
   * @return the map projects
   */
  @GET
  @Path("/project/projects")
  @ApiOperation(value = "Get all projects", notes = "Returns all MapProjects in either JSON or XML format", response = MapProjectList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProjectList getMapProjects() {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping):  /project/projects");

    try {
      MappingService mappingService = new MappingServiceJpa();

      // if (! mappingService.isFactoryOpen()) {
      // System.out.println("REST: service manager not open"); }
      // if (! mappingService.isManagerOpen()) {
      // System.out.println("REST: service manager not open"); }

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
  @Path("/user/users/")
  @ApiOperation(value = "Get all mapping users", notes = "Returns all MapUsers in either JSON or XML format", response = MapUserList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapUserList getMapUsers() {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /user/users");

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapUserList mapLeads = new MapUserList();
      mapLeads.setMapUsers(mappingService.getMapUsers());
      mapLeads.sortMapUsers();
      mappingService.close();
      return mapLeads;
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
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
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
  @Path("/user/id/{id:[0-9][0-9]*}/projects")
  @ApiOperation(value = "Find all projects for user", notes = "Returns a MapUser's MapProjects in either JSON or XML format", response = MapProjectList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProjectList getMapProjectsForUser(
    @ApiParam(value = "Id of map lead to fetch projects for", required = true) @PathParam("id") Long mapLeadId) {

    Logger.getLogger(MappingServiceRest.class)
        .info(
            "RESTful call (Mapping): lead/id/" + mapLeadId.toString()
                + "/projects");

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapProjectList mapProjects = new MapProjectList();
      MapUser mapLead = mappingService.getMapUser(mapLeadId);
      mapProjects.setMapProjects(mappingService
          .getMapProjectsForMapUser(mapLead));
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
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProject getMapProjectForId(
    @ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/id/" + mapProjectId.toString());

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      mapProject.getScopeConcepts().size();
      mapProject.getScopeExcludedConcepts().size();
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
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findMapProjects(
    @ApiParam(value = "lucene search string", required = true) @PathParam("String") String query) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /project/query/" + query);

    try {
      MappingService mappingService = new MappingServiceJpa();
      SearchResultList searchResultList =
          mappingService.findMapProjects(query, new PfsParameterJpa());
      mappingService.close();
      return searchResultList;

    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }

  /**
   * Returns the user for a given id (auto-generated) in JSON format
   * 
   * @param mapUserId the mapUserId
   * @return the mapUser
   */
  @GET
  @Path("/user/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Find user by id", notes = "Returns a MapUser given a user id in either JSON or XML format", response = MapUser.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
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
   * Returns all map users for a lucene query
   * @param query the string query
   * @return the map users
   */
  @GET
  @Path("/user/query/{string}")
  @ApiOperation(value = "Find users by query", notes = "Returns map users for a query in either JSON or XML format", response = MapUserList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findMapUsers(
    @ApiParam(value = "lucene search string", required = true) @PathParam("string") String query) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /user/query/" + query);

    try {
      MappingService mappingService = new MappingServiceJpa();
      SearchResultList searchResultList =
          mappingService.findMapUsers(query, new PfsParameterJpa());
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
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
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
   * Returns the records for a given concept id
   * 
   * @param conceptId the concept id
   * @return the mapRecords
   */
  @GET
  @Path("/record/conceptId/{String}")
  @ApiOperation(value = "Find records by concept id", notes = "Returns MapRecords given a concept id in either JSON or XML format", response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordList getMapRecordsForConceptId(
    @ApiParam(value = "Concept id of map record to fetch", required = true) @PathParam("String") String conceptId) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/conceptId/" + conceptId);

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapRecordList mapRecords = new MapRecordList();
      mapRecords.setMapRecords(mappingService
          .getMapRecordsForConcept(conceptId));
      mappingService.close();
      return mapRecords;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Returns the count of records associated with a map project given filters
   * query, ignoring paging and sorting information
   * 
   * @param mapProjectId the map project id
   * @param pfsParameter the paging/filtering/sorting parameters object
   * @return the number of records as a String object
   */
  @POST
  @Path("/record/projectId/{id:[0-9][0-9]*}/nRecords")
  @ApiOperation(value = "Find number of records by project id given filtering information", notes = "Returns count of MapRecords in database given a paging/filtering/sorting parameters object", response = MapRecordList.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces(MediaType.TEXT_PLAIN)
  public String getMapRecordCountForMapProjectId(
    @ApiParam(value = "Project id associated with map records", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/projectId/" + mapProjectId.toString()
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getFilterString());

    try {

      MappingService mappingService = new MappingServiceJpa();
      Long nRecords =
          mappingService.getMapRecordCountForMapProject(mapProjectId,
              pfsParameter);
      mappingService.close();

      // Jersey can't handle Long as return type, convert to string
      return nRecords.toString();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Returns delimited page of MapRecords given a paging/filtering/sorting
   * parameters object
   * 
   * @param mapProjectId the map project id
   * @param pfsParameter the JSON object containing the paging/filtering/sorting
   *          parameters
   * @return the list of map records
   */
  @POST
  @Path("/record/projectId/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Find paged records by project id", notes = "Returns delimited page of MapRecords given a paging/filtering/sorting parameters object", response = MapRecordList.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @CookieParam(value = "userInfo")
  public MapRecordList getMapRecordsForMapProjectId(
    @ApiParam(value = "Project id associated with map records", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter) {

    // log call
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/projectId/" + mapProjectId.toString()
            + " with PfsParameter: " + "\n" + "     Index/Results = "
            + Integer.toString(pfsParameter.getStartIndex()) + "/"
            + Integer.toString(pfsParameter.getMaxResults()) + "\n"
            + "     Sort field    = " + pfsParameter.getSortField()
            + "     Filter String = " + pfsParameter.getFilterString());

    // execute the service call

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapRecordList mapRecords = new MapRecordList();
      mapRecords.setMapRecords(mappingService.getMapRecordsForMapProject(
          mapProjectId, pfsParameter));
      mappingService.close();
      return mapRecords;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }

  /**
   * Deletes all map records associated with a map project given a project id
   * @param mapProjectId the map project id
   * @return the number of records deleted
   */
  @DELETE
  @Path("record/delete/projectId/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Deletes all records for a project id", notes = "Deletes all map records for a project id", response = Integer.class)
  @Produces({
    MediaType.TEXT_PLAIN
  })
  public String removeMapRecordsForProjectId(
    @ApiParam(value = "Project id for which map records are to be deleted", required = true) @PathParam("id") Long mapProjectId) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/delete/projectId"
            + mapProjectId.toString());

    try {
      MappingService mappingService = new MappingServiceJpa();
      Long nRecords = mappingService.removeMapRecordsForProject(mapProjectId);
      mappingService.close();

      // Jersey can't handle Long as return type, convert to string
      return nRecords.toString();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  // ///////////////////////////////////////////////////
  // MapProject: Add (@PUT) functions
  // - addMapProject
  // - addMapSpecialist
  // - addMapUser
  // ///////////////////////////////////////////////////

  /**
   * Adds a map project
   * @param mapProject the map project to be added
   * @return returns the added map project object
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/project/add")
  @ApiOperation(value = "Add a project", notes = "Adds a MapProject", response = MapProjectJpa.class)
  public MapProject addMapProject(
    @ApiParam(value = "The map project to add. Must be in Json or Xml format", required = true) MapProjectJpa mapProject) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapProject mp = mappingService.addMapProject(mapProject);
      mappingService.close();

      return mp;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }

  /**
   * Adds a map lead
   * @param mapUser the map user
   * @return Response the response
   */
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/user/add")
  @ApiOperation(value = "Add a user", notes = "Adds a MapUser", response = MapUserJpa.class)
  public Response addMapUser(
    @ApiParam(value = "The map userd to add. Must be in Json or Xml format", required = true) MapUserJpa mapUser) {

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
   * Adds a map record
   * @param mapRecord the map record to be added
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
  @ApiOperation(value = "Add a record", notes = "Adds a MapRecord", response = MapRecordJpa.class)
  public MapRecord addMapRecord(
    @ApiParam(value = "The map record to add", required = true) MapRecordJpa mapRecord) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapRecord result = mappingService.addMapRecord(mapRecord);
      mappingService.close();
      return result;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }

  // ///////////////////////////////////////////////////
  // MapProject: Update (@POST) functions
  // - updateMapProject
  // - updateMapSpecialist
  // - updateMapUser
  // ///////////////////////////////////////////////////

  /**
   * Updates a map project
   * @param mapProject the map project to be added
   * @return Response the response
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/project/update")
  @ApiOperation(value = "Update a project", notes = "Updates a map project", response = MapProjectJpa.class)
  public Response updateMapProject(
    @ApiParam(value = "The map project to update. Must exist in mapping database. Must be in Json or Xml format", required = true) MapProjectJpa mapProject) {

    System.out.println("-> Update Project");
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
   * Updates a map user
   * @param mapUser the map user to be added
   * @return Response the response
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/user/update")
  @ApiOperation(value = "Update a user", notes = "Updates a map user", response = MapUserJpa.class)
  public Response updateMapUser(
    @ApiParam(value = "The map user to update.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapUserJpa mapUser) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      mappingService.updateMapUser(mapUser);
      mappingService.close();
      return null;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Updates a map record
   * @param mapRecord the map record to be added
   * @return Response the response
   */
  @POST
  @Path("/record/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a record", notes = "Updates a map record", response = MapRecordJpa.class)
  public MapRecord updateMapRecord(
    @ApiParam(value = "The map record to update.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapRecordJpa mapRecord) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapRecord result = mappingService.updateMapRecord(mapRecord);
      mappingService.close();
      return result;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }

  /**
   * Returns the map record revisions.
   * 
   * @param mapRecordId the map record id
   * @return the map record revisions
   */
  @GET
  @Path("/record/{id:[0-9][0-9]*}/revisions")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get record revision history", notes = "Retrieves revision history of a record", response = MapRecordList.class)
  public MapRecordList getMapRecordRevisions(
    @ApiParam(value = "Id of map record to get revisions for", required = true) @PathParam("id") Long mapRecordId) {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /record/validate");

    try {
      MappingService mappingService = new MappingServiceJpa();
      List<MapRecord> revisions =
          mappingService.getMapRecordRevisions(mapRecordId);
      MapRecordList results = new MapRecordList();
      results.setMapRecords(revisions);

      mappingService.close();
      return results;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }

  // ///////////////////////////////////////////////////
  // MapProject: Removal (@DELETE) functions
  // - removeMapProject
  // - removeMapSpecialist
  // - removeMapUser
  // ///////////////////////////////////////////////////

  /**
   * Removes a map project
   * @param mapProjectId the id of the map project to be deleted
   * @return Response the response
   */
  @DELETE
  @Path("/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Remove a project", notes = "Removes a map project", response = MapProject.class)
  public Response removeMapProject(
    @ApiParam(value = "Id of map project to remove", required = true) @PathParam("id") Long mapProjectId) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      mappingService.removeMapProject(mapProjectId);
      mappingService.close();
      return null;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Removes a map user
   * @param mapUserId the id of the map user to be deleted
   * @return Response the response
   */
  @DELETE
  @Path("/user/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Remove a user", notes = "Removes a map user", response = MapUser.class)
  public Response removeMapUser(
    @ApiParam(value = "Id of map user to remove", required = true) @PathParam("id") Long mapUserId) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      mappingService.removeMapUser(mapUserId);
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
  public Response removeMapRecord(
    @ApiParam(value = "Id of map record to remove", required = true) @PathParam("id") Long mapRecordId) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      mappingService.removeMapRecord(mapRecordId);
      mappingService.close();
      return null;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Removes a map record
   * @param mapRecord the map record to delete
   * @return Response the response
   */
  @DELETE
  @Path("/record/delete")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Remove a record", notes = "Removes a map record", response = MapRecordJpa.class)
  public Response removeMapRecord(
    @ApiParam(value = "Map Record object to delete", required = true) MapRecordJpa mapRecord) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      mappingService.removeMapRecord(mapRecord.getId());
      mappingService.close();
      return null;
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
   * @param terminologyId the concept terminology id
   * @param terminology the concept terminology
   * @param terminologyVersion the concept terminology version
   * @param threshold the maximum number of descendants before a concept is no
   *          longer considered a low-level concept, and will return an empty
   *          list
   * @return the ConceptList of unmapped descendants
   */
  @GET
  @Path("/concept/{terminology}/{version}/id/{id}/threshold/{threshold:[0-9][0-9]*}")
  @ApiOperation(value = "Find concept by id, terminology", notes = "Returns a concept in either xml json given a concept id, terminology - assumes latest terminology version.", response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getUnmappedDescendantsForConcept(
    @ApiParam(value = "concept terminology id", required = true) @PathParam("id") String terminologyId,
    @ApiParam(value = "concept terminology", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "concept terminology version", required = true) @PathParam("version") String terminologyVersion,
    @ApiParam(value = "threshold max number of descendants for a low-level concept", required = true) @PathParam("threshold") int threshold) {

    // TODO Convert to Search Results
    try {
      MappingService mappingService = new MappingServiceJpa();

      SearchResultList results =
          mappingService.findUnmappedDescendantsForConcept(terminologyId,
              terminology, terminologyVersion, threshold);

      mappingService.close();
      return results;

    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Returns the map principle for id.
   * 
   * @param mapPrincipleId the map principle id
   * @return the map principle for id
   */
  @GET
  @Path("/principle/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Find principle by id", notes = "Returns a MapPrinciple given a principle id in either JSON or XML format", response = MapPrinciple.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapPrinciple getMapPrincipleForId(
    @ApiParam(value = "Id of map principle to fetch", required = true) @PathParam("id") Long mapPrincipleId) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapPrinciple mapPrinciple =
          mappingService.getMapPrinciple(mapPrincipleId);
      mappingService.close();
      return mapPrinciple;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Returns all map principles in either JSON or XML format
   * 
   * @return the map principles
   */
  @GET
  @Path("/principle/principles/")
  @ApiOperation(value = "Get all principles", notes = "Returns all MapPrinciples in either JSON or XML format", response = MapPrincipleList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapPrincipleList getMapPrinciples() {

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapPrincipleList mapPrinciples = new MapPrincipleList();
      mapPrinciples.setmapPrinciples(mappingService.getMapPrinciples());
      mapPrinciples.sortmapPrinciples();
      mappingService.close();
      return mapPrinciples;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Updates a map principle
   * @param mapPrinciple
   * @return the response
   */
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/principle/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Update principle by id", notes = "Updates a MapPrinciple. Must exist in mapping database. Must be in Json or Xml format", response = MapPrincipleJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Response updateMapPrincipleForId(
    @ApiParam(value = "Map Principle to update", required = true) MapPrincipleJpa mapPrinciple) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      mappingService.updateMapPrinciple(mapPrinciple);
      mappingService.close();

    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
    return null;
  }

  /**
   * Returns all map advices in either JSON or XML format
   * 
   * @return the map advices
   */
  @GET
  @Path("/advice/advices/")
  @ApiOperation(value = "Get all advices", notes = "Returns all MapAdvices in either JSON or XML format", response = MapAdviceList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapAdviceList getMapAdvices() {

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapAdviceList mapAdvices = new MapAdviceList();
      mapAdvices.setMapAdvices(mappingService.getMapAdvices());
      mapAdvices.sortMapAdvices();
      mappingService.close();
      return mapAdvices;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Returns all map relations in either JSON or XML format
   * 
   * @return the map relations
   */
  @GET
  @Path("/relation/relations/")
  @ApiOperation(value = "Get all relations", notes = "Returns all MapRelations in either JSON or XML format", response = MapRelationList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRelationList getMapRelations() {

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapRelationList mapRelations = new MapRelationList();
      mapRelations.setMapRelations(mappingService.getMapRelations());
      mapRelations.sortMapRelations();
      mappingService.close();
      return mapRelations;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Generates map records based on a map projects metadata. Requires map
   * project conceptId, source and destination terminologies, source and
   * destination terminology versions be set.
   * @param conceptId
   * @return returns a list of map records
   */
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/project/record/generate")
  @ApiOperation(value = "Find map records based on project metadata", notes = "Retrieves map records given project metadata", response = MapRecordList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordList getMapRecordsForConceptId(
    @ApiParam(value = "Concept hibernate id", required = true) Long conceptId) {
    MapRecordList results = new MapRecordList();
    try {
      MappingService mappingService = new MappingServiceJpa();
      results.setMapRecords(mappingService.getMapRecordsForConcept(conceptId));
      mappingService.close();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
    return results;
  }

  /**
   * Returns the map records for concept id.
   * @param mapProjectId
   * 
   * @return the map records for concept id
   */
  @GET
  @Path("/scope/includes/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Find project by id", notes = "Returns a MapProject given a project id in either JSON or XML format", response = MapProject.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findConceptsInScope(
    @ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      SearchResultList searchResultList =
          mappingService.findConceptsInScope(mapProject.getId());
      mappingService.close();
      return searchResultList;

    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }

  /**
   * Find concepts excluded from scope.
   * 
   * @param mapProjectId the map project id
   * @return the search result list
   */
  @GET
  @Path("/scope/excludes/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Find project by id", notes = "Returns a MapProject given a project id in either JSON or XML format", response = MapProject.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findConceptsExcludedFromScope(
    @ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      SearchResultList searchResultList =
          mappingService.findConceptsExcludedFromScope(mapProjectId);
      mappingService.close();
      return searchResultList;

    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }

  /**
   * Find mapped concepts out of scope bounds.
   * 
   * @param mapProjectId the map project id
   * @return the search result list
   */
  @GET
  @Path("/scope/outofbounds/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Find project by id", notes = "Returns a MapProject given a project id in either JSON or XML format", response = MapProject.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findMappedConceptsOutOfScopeBounds(
    @ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      SearchResultList searchResultList =
          mappingService.findMappedConceptsOutOfScopeBounds(mapProjectId);
      mappingService.close();
      return searchResultList;

    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }

  /**
   * Find unmapped concepts in scope.
   * 
   * @param mapProjectId the map project id
   * @return the search result list
   */
  @GET
  @Path("/scope/unmapped/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Find project by id", notes = "Returns a MapProject given a project id in either JSON or XML format", response = MapProject.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findUnmappedConceptsInScope(
    @ApiParam(value = "Id of map project to fetch", required = true) @PathParam("id") Long mapProjectId) {

    try {
      MappingService mappingService = new MappingServiceJpa();
      SearchResultList searchResultList =
          mappingService.findUnmappedConceptsInScope(mapProjectId);
      mappingService.close();
      return searchResultList;

    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }
 
  @GET
  @Path("/recentRecords/{userName}")
  @ApiOperation(value = "Find recently edited map records", notes = "Returns recently edited map records for given userName in either JSON or XML format", response = MapRecordList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordList getRecentlyEditedMapRecords(
    @ApiParam(value = "Id of user", required = true) @PathParam("userName") String userName) {
	//TODO: PfsParameter
  	List<MapRecord> editedRecords = new ArrayList<>();
    try {
      MappingService mappingService = new MappingServiceJpa();
      
      MapUser user = mappingService.getMapUser(userName);
     	 
      editedRecords = mappingService.getRecentlyEditedMapRecords(user);
      
      mappingService.close();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
    
    MapRecordList recordList = new MapRecordList();
    recordList.setMapRecords(editedRecords);
		return recordList;
	}
  
  /**
   * Updates a map record
   * @param mapRecord the map record to be added
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
  @ApiOperation(value = "", notes = "", response = MapRelationJpa.class)
  public MapRelation computeMapRelation(
    @ApiParam(value = "", required = true) MapEntryJpa mapEntry) {

	  try {
		  MappingService mappingService = new MappingServiceJpa();
		  MapRelation mapRelation = mappingService.computeMapRelation(mapEntry);
		  mappingService.close();
		  return mapRelation;
	  } catch (Exception e) {
		  throw new WebApplicationException(e);
	  }
  }
  
 /*
  @GET
  @Path("/relation/compute")
  @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  @ApiOperation(value = "Compute an entry's map relation", notes = "Given a map entry and its associated map record, computes any map relation based on target and project parameters", response = MapRelationJpa.class)
  public MapRelation computeMapRelation(
    @ApiParam(value = "The map record.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "The map entry to compute the relation for.  Need not be in database.  Must be in Json or Xml format", required = true) MapEntryJpa mapEntry) {
  
	  
	  try {
		  MappingService mappingService = new MappingServiceJpa();
		  MapRelation mapRelation = mappingService.computeMapRelation(mapRecord, mapEntry);
		  mappingService.close();
		  return mapRelation;
	  } catch (Exception e) {
		  throw new WebApplicationException(e);
	  }
	  
  }
  
  @GET
  @Path("/advice/compute")
  @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  @ApiOperation(value = "Compute an entry's map advice", notes = "Given a map entry and its associated map record, computes any map advice based on target and project parameters", response = MapRelationJpa.class)
  public MapAdviceList computeMapAdvice(
    @ApiParam(value = "The map record.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "The map entry to compute the advice for.  Need not be in database.  Must be in Json or Xml format", required = true) MapEntryJpa mapEntry) {
  
	  
	  try {
		  MappingService mappingService = new MappingServiceJpa();
		  MapAdviceList mapAdviceList = new MapAdviceList();
		  mapAdviceList.setMapAdvices(mappingService.computeMapAdvice(mapRecord, mapEntry));
		  mappingService.close();
		  return mapAdviceList;
		  
	  } catch (Exception e) {
		  throw new WebApplicationException(e);
	  }
	  
  }*/
  
}
