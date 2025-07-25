/*
 * Copyright 2020 Wci Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Wci Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * Wci Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.otf.mapping.rest.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.AuthenticationException;
import javax.ws.rs.Consumes;
//import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.Hibernate;
import org.ihtsdo.otf.mapping.helpers.AdditionalMapEntryInfoListJpa;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.KeyValuePair;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairList;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairLists;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.MapAgeRangeListJpa;
import org.ihtsdo.otf.mapping.helpers.MapPrincipleListJpa;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.helpers.MapProjectListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRelationListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.TreePositionListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.AdditionalMapEntryInfoJpa;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.BeginEditingCycleHandlerJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.ExportReportHandler;
import org.ihtsdo.otf.mapping.jpa.handlers.ReleaseHandlerJpa;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.jpa.services.AmazonS3ServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.rest.MappingServiceRest;
import org.ihtsdo.otf.mapping.model.AdditionalMapEntryInfo;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.AmazonS3Service;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.BeginEditingCycleHandler;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ReleaseHandler;
import org.ihtsdo.otf.mapping.services.helpers.WorkflowPathHandler;
import org.ihtsdo.otf.mapping.workflow.TrackingRecord;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * REST implementation for mapping service.
 */
@Path("/mapping")
@Api(value = "/mapping", description = "Operations supporting map objects.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class MappingServiceRestImpl extends RootServiceRestImpl implements MappingServiceRest {

  private static final int MAX_RESULTS = 1000000;

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link MappingServiceRestImpl}.
   * 
   * @throws Exception the exception
   */
  public MappingServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Projects
  // ///////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapProjects(java.
   * lang.String)
   */
  @Override
  @GET
  @Path("/project/projects")
  @ApiOperation(value = "Get map projects", notes = "Gets a list of all map projects.",
      response = MapProjectListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProjectListJpa getMapProjects(@ApiParam(value = "Authorization token",
      required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/projects");
    final MappingService mappingService = new MappingServiceJpa();
    String user = null;
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map projects", securityService);

      // instantiate list of projects to return
      final MapProjectListJpa mapProjects = new MapProjectListJpa();

      // cycle over projects and verify that this user can view each
      // project
      for (final MapProject mapProject : mappingService.getMapProjects().getMapProjects()) {

        // if this user has a role of VIEWER or above for this project
        // (i.e. is
        // not NONE)
        if (!securityService.getMapProjectRoleForToken(authToken, mapProject.getId())
            .equals(MapUserRole.NONE) || mapProject.isPublic()) {

          // do not serialize the scope concepts or excludes
          // (retrieval
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
      return mapProjects;

    } catch (Exception e) {
      this.handleException(e, "trying to get map projects", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapProject(java.lang
   * .Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get map project by id",
      notes = "Gets a map project for the specified parameters.", response = MapProject.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProject getMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/id/" + mapProjectId.toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get the map project", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);
      mapProject.getScopeConcepts().size();
      mapProject.getScopeExcludedConcepts().size();
      mapProject.getMapAdvices().size();
      mapProject.getAdditionalMapEntryInfos().size();
      mapProject.getMapRelations().size();
      mapProject.getMapLeads().size();
      mapProject.getMapSpecialists().size();
      mapProject.getMapPrinciples().size();
      mapProject.getPresetAgeRanges().size();
      mapProject.getErrorMessages().size();
      mapProject.getReportDefinitions().size();
      return mapProject;

    } catch (Exception e) {
      handleException(e, "trying to get the map project", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapProject(org.
   * ihtsdo.otf.mapping.jpa.MapProjectJpa, java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/project/add")
  @ApiOperation(value = "Add a map project", notes = "Adds the specified map project.",
      response = MapProjectJpa.class)
  public MapProject addMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data",
        required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /project/add");

    String user = null;
    String project = "";
    final MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      user =
          authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "add a map project", securityService);
      // check that project specific handler exists as a class

      try {
        Class.forName(mapProject.getProjectSpecificAlgorithmHandlerClass());
      } catch (ClassNotFoundException e) {
        throw new LocalException(
            "Adding map project failed -- could not find project specific algorithm handler for class name: "
                + mapProject.getProjectSpecificAlgorithmHandlerClass());
      }

      final MapProject mp = mappingService.addMapProject(mapProject);
      project = mp.getName();

      return mp;

    } catch (Exception e) {
      handleException(e, "trying to add a map project", user, project, "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapProject(org.
   * ihtsdo.otf.mapping.jpa.MapProjectJpa, java.lang.String)
   */
  @Override
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/project/update")
  @ApiOperation(value = "Update a map project",
      notes = "Updates specified map project if it already exists.", response = MapProjectJpa.class)
  public void updateMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data",
        required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /project/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      user = authorizeProject(mapProject.getId(), authToken, MapUserRole.LEAD,
          "update a map project", securityService);

      // check that project specific handler exists as a class
      try {
        Class.forName(mapProject.getProjectSpecificAlgorithmHandlerClass());
      } catch (ClassNotFoundException e) {
        throw new LocalException(
            "Updating map project failed -- could not find project specific algorithm handler for class name: "
                + mapProject.getProjectSpecificAlgorithmHandlerClass());
      }

      // recompute the scope concepts for this map project
      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      handler.initialize();

      // scope includes and excludes are transient, and must be added to
      // project
      // before update from webapp
      final MapProject mapProjectFromDatabase = mappingService.getMapProject(mapProject.getId());

      // set the scope concepts and excludes to the contents of the
      // database
      // prior to update
      mapProject.setScopeConcepts(mapProjectFromDatabase.getScopeConcepts());
      mapProject.setScopeExcludedConcepts(mapProjectFromDatabase.getScopeExcludedConcepts());

      // update the project and close the service
      mappingService.updateMapProject(mapProject);

    } catch (Exception e) {
      handleException(e, "trying to update a map project", user, mapProject.getName(), "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapProject(org.
   * ihtsdo.otf.mapping.jpa.MapProjectJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/project/delete")
  @ApiOperation(value = "Remove a map project",
      notes = "Removes specified map project if it already exists.", response = MapProject.class)
  public void removeMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data",
        required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/delete for " + mapProject.getName());

    String user = null;

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "update a map project",
          securityService);

      mappingService.removeMapProject(mapProject.getId());

    } catch (Exception e) {
      handleException(e, "trying to remove a map project", user, mapProject.getName(), "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#cloneMapProject(org.
   * ihtsdo.otf.mapping.jpa.MapProjectJpa, java.lang.String)
   */
  @Override
  @PUT
  @Path("/clone")
  @ApiOperation(value = "Clone map project",
      notes = "Adds the specified map project, which is a potentially modified copy of another map project",
      response = MapProjectJpa.class)
  public MapProject cloneMapProject(
    @ApiParam(value = "MapProject PUT data", required = false) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token, e.g. 'author1'",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call PUT (Mapping): /clone " + mapProject.getId() + ", " + mapProject);

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "clone a map project", securityService);

      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();

      // Add the map project (null the id)
      final Long mapProjectId = mapProject.getId();
      final MapProject originMapProject = mappingService.getMapProject(mapProjectId);

      // Determine if refset with this id already exists in this project.
      /*
       * if (originRefset.getTerminologyId().equals(refset.getTerminologyId())
       * && originRefset.getProject().getId()
       * .equals(refset.getProject().getId())) { throw new LocalException(
       * "Duplicate refset terminology id within the project, " +
       * "please change terminology id"); }
       */
      final Set<MapUser> mapLeads = originMapProject.getMapLeads();
      // lazy init
      mapLeads.size();

      // copy map specialists and leads
      for (MapUser mapLead : originMapProject.getMapLeads()) {
        mapLead.setId(null);
      }
      for (MapUser mapSpecialist : originMapProject.getMapSpecialists()) {
        mapSpecialist.setId(null);
      }
      // clear error messages
      mapProject.setErrorMessages(new HashSet<String>());
      MapProject newMapProject = mappingService.addMapProject(mapProject);

      /*
       * // Copy all the members if EXTENSIONAL if (refset.getType() ==
       * Refset.Type.EXTENSIONAL) {
       * 
       * // Get the original reference set for (final ConceptRefsetMember
       * originMember : originMembers) { final ConceptRefsetMember member = new
       * ConceptRefsetMemberJpa(originMember); member.setPublished(false);
       * member.setPublishable(true); member.setRefset(newRefset);
       * member.setEffectiveTime(null); // Insert new members
       * member.setId(null); member.setLastModifiedBy(userName);
       * refsetService.addMember(member); } // Resolve definition if INTENSIONAL
       * } else if (refset.getType() == Refset.Type.INTENSIONAL) { // Copy
       * inclusions and exclusions from origin refset for (final
       * ConceptRefsetMember member : originMembers) { if
       * (member.getMemberType() == Refset.MemberType.INCLUSION ||
       * member.getMemberType() == Refset.MemberType.EXCLUSION) { final
       * ConceptRefsetMember member2 = new ConceptRefsetMemberJpa(member);
       * member2.setRefset(newRefset); member2.setId(null);
       * refsetService.addMember(member2); newRefset.addMember(member2); } }
       * refsetService.resolveRefsetDefinition(newRefset); }
       */

      // done
      mappingService.commit();
      return newMapProject;
    } catch (Exception e) {
      handleException(e, "trying to clone a map project");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#findMapProjectsForQuery
   * (java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/project")
  @ApiOperation(value = "Find map projects by query",
      notes = "Gets a list of search results for map projects matching the lucene query.",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findMapProjectsForQuery(
    @ApiParam(value = "Query, e.g. 'SNOMED to ICD10'",
        required = true) @QueryParam("query") String query,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /project " + query);
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "find map projects", securityService);

      final SearchResultList searchResultList =
          mappingService.findMapProjectsForQuery(query, new PfsParameterJpa());
      return searchResultList;

    } catch (Exception e) {
      handleException(e, "trying to find map projects", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapProjectsForUser(
   * java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/project/user/id/{username}")
  @ApiOperation(value = "Get all map projects for a user",
      notes = "Gets a list of map projects for the specified user.",
      response = MapProjectListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProjectListJpa getMapProjectsForUser(
    @ApiParam(value = "Username (can be specialist, lead, or admin)",
        required = true) @PathParam("username") String mapUserName,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/user/id/" + mapUserName);
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      user =
          authorizeApp(authToken, MapUserRole.VIEWER, "get map projects for user", securityService);

      final MapUser mapLead = mappingService.getMapUser(mapUserName);
      final MapProjectListJpa mapProjects =
          (MapProjectListJpa) mappingService.getMapProjectsForMapUser(mapLead);

      for (final MapProject mapProject : mapProjects.getMapProjects()) {
        mapProject.getScopeConcepts().size();
        mapProject.getScopeExcludedConcepts().size();
        mapProject.getMapAdvices().size();
        mapProject.getAdditionalMapEntryInfos().size();
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
      return mapProjects;

    } catch (Exception e) {
      handleException(e, "trying to get the map projects for a given user", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Users
  // ///////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapUsers(java.lang.
   * String)
   */
  @Override
  @GET
  @Path("/user/users")
  @ApiOperation(value = "Get all mapping users", notes = "Gets all map users.",
      response = MapUserListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapUserListJpa getMapUsers(@ApiParam(value = "Authorization token",
      required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /user/users");
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map users", securityService);

      final MapUserListJpa mapUsers = (MapUserListJpa) mappingService.getMapUsers();
      mapUsers.sortBy(new Comparator<MapUser>() {
        @Override
        public int compare(MapUser o1, MapUser o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      // remove non-ihtsdo emails to address privacy concerns
      // ihtsdo emails will be truncated to remove the domain,
      // domain will be appended again on client side
      for (MapUser mapUser : mapUsers.getMapUsers()) {
        if (mapUser.getEmail().endsWith("ihtsdo.gov")) {
          mapUser.setEmail(mapUser.getEmail().substring(0, mapUser.getEmail().indexOf('@')));
        } else {
          mapUser.setEmail("Private email");
        }
      }

      // do not return this private information if user is a guest
      if (user.equals("guest")) {
        for (MapUser mapUser : mapUsers.getMapUsers()) {
          if (mapUser.getUserName().equals("guest")) {
            MapUserListJpa list = new MapUserListJpa();
            list.addMapUser(mapUser);
            list.setTotalCount(1);
            return list;
          }
        }
      }
      return mapUsers;

    } catch (Exception e) {
      handleException(e, "trying to get map users", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getScopeConceptsForMapProject(java.lang.Long,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeConcepts")
  @ApiOperation(value = "Get scope concepts for a map project",
      notes = "Gets a (pageable) list of scope concepts for a map project",
      response = SearchResultListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getScopeConceptsForMapProject(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Paging/filtering/sorting object",
        required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "";
    String user = "";

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.VIEWER, "get scope concepts",
          securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);

      final SearchResultList results =
          mappingService.getScopeConceptsForMapProject(mapProject, pfsParameter);

      return results;

    } catch (Exception e) {
      this.handleException(e, "trying to get scope concepts", user, projectName, "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * addScopeConceptsToMapProject(java.util.List, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeConcepts/add")
  @ApiOperation(value = "Adds a list of scope concepts to a map project",
      notes = "Adds a list of scope concepts to a map project.",
      response = ValidationResultJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ValidationResult addScopeConceptsToMapProject(
    @ApiParam(value = "List of concepts to add, e.g. {'100073004', '100075006'",
        required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "";
    String user = "";
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();

    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.LEAD,
          "add scope concepts to project", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);

      //
      final ValidationResult result = new ValidationResultJpa();
      for (final String terminologyId : terminologyIds) {
        if (mapProject.getScopeConcepts().contains(terminologyId)) {
          result.addWarning("Concept " + terminologyId + " is already in scope, skipping.");
        } else if (contentService.getConcept(terminologyId, mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion()) != null) {
          mapProject.addScopeConcept(terminologyId);
          // empty the scope concepts cache to force scope recalculation
          ConfigUtility.setScopeConceptsForMapProject(mapProject.getId(), new HashSet<>());
          mappingService.updateMapProject(mapProject);
          result.addMessage("Concept " + terminologyId + " added to scope.");
        } else {
          result.addWarning("Concept " + terminologyId + " does not exist, skipping.");
        }
      }
      return result;
    } catch (Exception e) {
      this.handleException(e, "trying to add scope concept to project", user, projectName, "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * removeScopeConceptFromMapProject(java.lang.String, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeConcept/remove")
  @ApiOperation(value = "Removes a single scope concept from a map project",
      notes = "Removes a single scope concept from a map project.",
      response = ValidationResult.class)
  public ValidationResult removeScopeConceptFromMapProject(
    @ApiParam(value = "Concept to remove, e.g. 100075006", required = true) String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "";
    String user = "";
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.LEAD,
          "remove scope concept from project", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);
      final ValidationResult result = new ValidationResultJpa();

      if (mapProject.getScopeConcepts().contains(terminologyId)) {
        mapProject.removeScopeConcept(terminologyId);
        // empty the scope concepts cache to force scope recalculation
        ConfigUtility.setScopeConceptsForMapProject(mapProject.getId(), new HashSet<>());
        mappingService.updateMapProject(mapProject);
        result.addMessage("Concept " + terminologyId + " has been removed from scope.");
      } else if (contentService.getConcept(terminologyId, mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion()) == null) {
        result.addWarning("Concept " + terminologyId + " does not exist, skipping.");
      } else {
        result.addWarning(
            "Concept " + terminologyId + " was not in scope for this project, skipping.");
      }

      return result;

    } catch (Exception e) {
      this.handleException(e, "trying to remove scope concept from project", user, projectName, "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * removeScopeConceptsFromMapProject(java.util.List, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeConcepts/remove")
  @ApiOperation(value = "Removes a list of scope concepts from a map project",
      notes = "Removes a list of scope concept from a map project.",
      response = ValidationResult.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ValidationResult removeScopeConceptsFromMapProject(
    @ApiParam(value = "List of concepts to remove, e.g. {'100073004', '100075006'",
        required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "";
    String user = "";

    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();

    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.LEAD,
          "remove scope concepts from project", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);
      ValidationResult result = new ValidationResultJpa();

      for (final String terminologyId : terminologyIds) {
        if (mapProject.getScopeConcepts().contains(terminologyId)) {
          mapProject.removeScopeConcept(terminologyId);
       // empty the scope concepts cache to force scope recalculation
          ConfigUtility.setScopeConceptsForMapProject(mapProject.getId(), new HashSet<>());
          mappingService.updateMapProject(mapProject);
          result.addMessage("Concept " + terminologyId + " has been removed from scope.");
        } else if (contentService.getConcept(terminologyId, mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion()) == null) {
          result.addWarning("Concept " + terminologyId + " does not exist, skipping.");
        } else {
          result.addWarning(
              "Concept " + terminologyId + " was not in scope for this project, skipping.");
        }
      }
      return result;

    } catch (Exception e) {
      this.handleException(e, "trying to remove scope concepts from project", user, projectName,
          "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getScopeExcludedConceptsForMapProject(java.lang.Long,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts")
  @ApiOperation(value = "Get scope excluded concepts for a map project",
      notes = "Gets a (pageable) list of scope excluded concepts for a map project",
      response = SearchResultListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getScopeExcludedConceptsForMapProject(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Paging/filtering/sorting object",
        required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + projectId + "/scopeExcludedConcepts");
    String projectName = "";
    String user = "";
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.VIEWER,
          "get scope concepts for projects", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);
      final SearchResultList results =
          mappingService.getScopeExcludedConceptsForMapProject(mapProject, pfsParameter);
      return results;

    } catch (Exception e) {
      this.handleException(e, "trying to get scope excluded concepts", user, projectName, "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * addScopeExcludedConceptsToMapProject(java.util.List, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts/add")
  @ApiOperation(value = "Adds a list of scope excluded concepts to a map project",
      notes = "Adds a list of scope excluded concepts to a map project.",
      response = ValidationResult.class)
  public ValidationResult addScopeExcludedConceptsToMapProject(
    @ApiParam(value = "List of concepts to add, e.g. {'100073004', '100075006'",
        required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + projectId + "/scopeExcludedConcepts/add");
    String projectName = "";
    String user = "";
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();

    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.LEAD,
          "add scope excluded concepts to projects", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);
      final ValidationResult result = new ValidationResultJpa();
      for (final String terminologyId : terminologyIds) {
        if (mapProject.getScopeExcludedConcepts().contains(terminologyId)) {
          result.addWarning(
              "Concept " + terminologyId + " is already in the scope excluded list, skipping.");
        } else if (contentService.getConcept(terminologyId, mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion()) != null) {
          mapProject.addScopeExcludedConcept(terminologyId);
          // empty the scope concepts cache to force scope recalculation
          ConfigUtility.setScopeConceptsForMapProject(mapProject.getId(), new HashSet<>());
          mappingService.updateMapProject(mapProject);
          result.addMessage("Concept " + terminologyId + " added to scope excluded list.");
        } else {
          result.addWarning("Concept " + terminologyId + " does not exist, skipping.");
        }
      }
      return result;
    } catch (Exception e) {
      this.handleException(e, "trying to add scope excluded concept to project", user, projectName,
          "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * removeScopeExcludedConceptFromMapProject(java.lang.String, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcept/remove")
  @ApiOperation(value = "Removes a single scope excluded concept from a map project",
      notes = "Removes a single scope excluded concept from a map project.",
      response = ValidationResult.class)
  public ValidationResult removeScopeExcludedConceptFromMapProject(
    @ApiParam(value = "Concept to remove, e.g. 100075006", required = true) String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + projectId + "/scopeExcludedConcept/remove");
    String projectName = "";
    String user = "";

    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.LEAD,
          "remove scope excluded concept from projects", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);
      final ValidationResult result = new ValidationResultJpa();

      if (mapProject.getScopeExcludedConcepts().contains(terminologyId)) {
        mapProject.removeScopeExcludedConcept(terminologyId);
     // empty the scope concepts cache to force scope recalculation
        ConfigUtility.setScopeConceptsForMapProject(mapProject.getId(), new HashSet<>());
        mappingService.updateMapProject(mapProject);
        result
            .addMessage("Concept " + terminologyId + " has been removed from scope excluded list.");
      } else if (contentService.getConcept(terminologyId, mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion()) == null) {
        result.addWarning("Concept " + terminologyId + " does not exist, skipping.");
      } else {
        result.addWarning("Concept " + terminologyId
            + " was not in scope exluded list for this project, skipping.");
      }

      return result;
    } catch (Exception e) {
      this.handleException(e, "trying to remove scope excluded concept from project", user,
          projectName, "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * removeScopeExcludedConceptsFromMapProject(java.util.List, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts/remove")
  @ApiOperation(value = "Removes a list of scope excluded concepts from a map project",
      notes = "Removes a list of scope excluded concept from a map project.",
      response = ValidationResult.class)
  public ValidationResult removeScopeExcludedConceptsFromMapProject(
    @ApiParam(value = "List of concepts to remove, e.g. {'100073004', '100075006'",
        required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeExcludedConcepts/remove");
    String projectName = "";
    String user = "";
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.LEAD,
          "remove scope excluded concept from projects", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);
      projectName = mapProject.getName();

      final ValidationResult result = new ValidationResultJpa();
      for (final String terminologyId : terminologyIds) {

        if (mapProject.getScopeExcludedConcepts().contains(terminologyId)) {
          mapProject.removeScopeExcludedConcept(terminologyId);
          // empty the scope concepts cache to force scope recalculation
          ConfigUtility.setScopeConceptsForMapProject(mapProject.getId(), new HashSet<>());
          mappingService.updateMapProject(mapProject);
          result.addMessage(
              "Concept " + terminologyId + " has been removed from scope excluded list.");
        } else if (contentService.getConcept(terminologyId, mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion()) == null) {
          result.addWarning("Concept " + terminologyId + " does not exist, skipping.");
        } else {
          result.addWarning("Concept " + terminologyId
              + " was not in scope exluded list for this project, skipping.");
        }
      }
      return result;

    } catch (Exception e) {
      this.handleException(e, "trying to remove scope excluded concepts from project", user,
          projectName, "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapUser(java.lang.
   * String, java.lang.String)
   */
  @Override
  @GET
  @Path("/user/id/{username}")
  @ApiOperation(value = "Get user by username", notes = "Gets a user by a username.",
      response = MapUser.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapUser getMapUser(
    @ApiParam(value = "Username (can be specialist, lead, or admin)",
        required = true) @PathParam("username") String mapUserName,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): user/id/" + mapUserName);
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "get map user", securityService);

      final MapUser mapUser = mappingService.getMapUser(mapUserName);
      return mapUser;

    } catch (Exception e) {
      handleException(e, "trying to get a map user", mapUserName, mapUserName, "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapUser(org.ihtsdo.
   * otf.mapping.jpa.MapUserJpa, java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/user/add")
  @ApiOperation(value = "Add a user", notes = "Adds the specified user.",
      response = MapUserJpa.class)
  public MapUser addMapUser(
    @ApiParam(value = "User, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /user/add");
    String userName = null;
    final MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      userName = authorizeApp(authToken, MapUserRole.LEAD, "add a user", securityService);

      // Check if user already exists and send better message
      for (final MapUser user : mappingService.getMapUsers().getMapUsers()) {
        if (user.getName().equals(mapUser.getName())) {
          throw new LocalException("This map user already exists: " + user.getName());
        }
      }
      mappingService.addMapUser(mapUser);
      return mapUser;

    } catch (Exception e) {
      handleException(e, "trying to add a user", userName, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapUser(org.
   * ihtsdo.otf.mapping.jpa.MapUserJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/user/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a user", notes = "Updates the specified user.",
      response = MapUserJpa.class)
  public void updateMapUser(
    @ApiParam(value = "User, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /user/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "update a user", securityService);

      mappingService.updateMapUser(mapUser);

    } catch (Exception e) {
      handleException(e, "trying to update a user", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapUser(org.
   * ihtsdo.otf.mapping.jpa.MapUserJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/user/delete")
  @ApiOperation(value = "Remove a user", notes = "Removes the specified user.",
      response = MapUser.class)
  public void removeMapUser(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /user/delete for user " + mapUser.getName());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "remove a user", securityService);

      mappingService.removeMapUser(mapUser.getId());

    } catch (Exception e) {
      handleException(e, "trying to remove a user", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Advice
  // ///////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapAdvices(java.lang
   * .String)
   */
  @Override
  @GET
  @Path("/advice/advices")
  @ApiOperation(value = "Get all mapping advices", notes = "Gets a list of all map advices.",
      response = MapAdviceListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapAdviceListJpa getMapAdvices(@ApiParam(value = "Authorization token",
      required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /advice/advices");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map advices", securityService);

      final MapAdviceListJpa mapAdvices = (MapAdviceListJpa) mappingService.getMapAdvices();
      mapAdvices.sortBy(new Comparator<MapAdvice>() {
        @Override
        public int compare(MapAdvice o1, MapAdvice o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      return mapAdvices;

    } catch (Exception e) {
      handleException(e, "trying to get an advice", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapAdvice(org.ihtsdo
   * .otf.mapping.jpa.MapAdviceJpa, java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/advice/add")
  @ApiOperation(value = "Add an advice", notes = "Adds the specified map advice.",
      response = MapAdviceJpa.class)
  public MapAdvice addMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data",
        required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /advice/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "add map advice", securityService);

      // Check if advice already exists and send better message
      for (final MapAdvice advice : mappingService.getMapAdvices().getMapAdvices()) {
        if (advice.getName().equals(mapAdvice.getName())) {
          throw new LocalException("This map advice already exists: " + advice.getName());
        }
      }

      mappingService.addMapAdvice(mapAdvice);
      return mapAdvice;

    } catch (Exception e) {
      handleException(e, "trying to add an advice", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapAdvice(org.
   * ihtsdo.otf.mapping.jpa.MapAdviceJpa, java.lang.String)
   */
  @Override
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/advice/update")
  @ApiOperation(value = "Update an advice", notes = "Updates the specified advice.",
      response = MapAdviceJpa.class)
  public void updateMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data",
        required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /advice/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "update map advice", securityService);

      mappingService.updateMapAdvice(mapAdvice);
    } catch (Exception e) {
      handleException(e, "trying to update an advice", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapAdvice(org.
   * ihtsdo.otf.mapping.jpa.MapAdviceJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/advice/delete")
  @ApiOperation(value = "Remove an advice", notes = "Removes the specified map advice.",
      response = MapAdviceJpa.class)
  public void removeMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data",
        required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /advice/delete for user " + mapAdvice.getName());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "remove map advice", securityService);

      mappingService.removeMapAdvice(mapAdvice.getId());
    } catch (Exception e) {
      LocalException le = new LocalException(
          "Unable to delete map advice. This is likely because the advice is being used by a map project or map entry");
      handleException(le, "", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Additional Map Entry Info
  // ///////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getAdditionalMapEntryInfos(java.lang .String)
   */
  @Override
  @GET
  @Path("/additionalMapEntryInfo/additionalMapEntryInfos")
  @ApiOperation(value = "Get all additional map entry info",
      notes = "Gets a list of all additional map entry infos.",
      response = AdditionalMapEntryInfoListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public AdditionalMapEntryInfoListJpa getAdditionalMapEntryInfos(
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /additionalMapEntryInfo/additionalMapEntryInfos");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get additional map entry infos",
          securityService);

      final AdditionalMapEntryInfoListJpa additionalMapEntryInfos =
          (AdditionalMapEntryInfoListJpa) mappingService.getAdditionalMapEntryInfos();
      additionalMapEntryInfos.sortBy(new Comparator<AdditionalMapEntryInfo>() {
        @Override
        public int compare(AdditionalMapEntryInfo o1, AdditionalMapEntryInfo o2) {
          return o1.getValue().compareTo(o2.getValue());
        }
      });
      return additionalMapEntryInfos;

    } catch (Exception e) {
      handleException(e, "trying to get additional map entry infos", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapAdvice(org.ihtsdo
   * .otf.mapping.jpa.MapAdviceJpa, java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/additionalMapEntryInfo/add")
  @ApiOperation(value = "Add an additional Map Entry Info",
      notes = "Adds the specified additional Map Entry Info.",
      response = AdditionalMapEntryInfoJpa.class)
  public AdditionalMapEntryInfo addAdditionalMapEntryInfo(
    @ApiParam(value = "Additional Map Entry Info, in JSON or XML POST data",
        required = true) AdditionalMapEntryInfoJpa additionalMapEntryInfo,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /additionalMapEntryInfo/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "add map advice", securityService);

      // Check if advice already exists and send better message
      for (final AdditionalMapEntryInfo anAdditionalMapEntryInfo : mappingService
          .getAdditionalMapEntryInfos().getAdditionalMapEntryInfos()) {
        if (anAdditionalMapEntryInfo.getName().equals(additionalMapEntryInfo.getName())) {
          throw new LocalException("This additional map entry info already exists: "
              + anAdditionalMapEntryInfo.getName());
        }
      }

      mappingService.addAdditionalMapEntryInfo(additionalMapEntryInfo);
      return additionalMapEntryInfo;

    } catch (Exception e) {
      handleException(e, "trying to add an additional map entry info", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapAdvice(org.
   * ihtsdo.otf.mapping.jpa.MapAdviceJpa, java.lang.String)
   */
  @Override
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/additionalMapEntryInfo/update")
  @ApiOperation(value = "Update an additional map entry info",
      notes = "Updates the specified additional map entry info.",
      response = AdditionalMapEntryInfoJpa.class)
  public void updateAdditionalMapEntryInfo(
    @ApiParam(value = "Map advice, in JSON or XML POST data",
        required = true) AdditionalMapEntryInfoJpa additionalMapEntryInfo,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /additionalMapEntryInfo/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "update map advice", securityService);

      mappingService.updateAdditionalMapEntryInfo(additionalMapEntryInfo);
    } catch (Exception e) {
      handleException(e, "trying to update an additional map entry info", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapAdvice(org.
   * ihtsdo.otf.mapping.jpa.MapAdviceJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/additionalMapEntryInfo/delete")
  @ApiOperation(value = "Remove an advice", notes = "Removes the specified map advice.",
      response = AdditionalMapEntryInfoJpa.class)
  public void removeAdditionalMapEntryInfo(
    @ApiParam(value = "Map advice, in JSON or XML POST data",
        required = true) AdditionalMapEntryInfoJpa additionalMapEntryInfo,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /additionalMapEntryInfo/delete");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "remove additional map entry info",
          securityService);

      mappingService.removeAdditionalMapEntryInfo(additionalMapEntryInfo.getId());
    } catch (Exception e) {
      LocalException le = new LocalException(
          "Unable to delete additional map entry info. This is likely because the additional map entry info is being used by a map project or map entry");
      handleException(le, "", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map AgeRange
  // ///////////////////////////////////////////////////
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapAgeRanges(java.
   * lang.String)
   */
  @Override
  @GET
  @Path("/ageRange/ageRanges")
  @ApiOperation(value = "Get all mapping age ranges",
      notes = "Gets a list of all mapping age ranges.", response = MapAgeRangeListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapAgeRangeListJpa getMapAgeRanges(@ApiParam(value = "Authorization token",
      required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /ageRange/ageRanges");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get age ranges", securityService);

      final MapAgeRangeListJpa mapAgeRanges = (MapAgeRangeListJpa) mappingService.getMapAgeRanges();
      mapAgeRanges.sortBy(new Comparator<MapAgeRange>() {
        @Override
        public int compare(MapAgeRange o1, MapAgeRange o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      return mapAgeRanges;

    } catch (Exception e) {
      handleException(e, "trying to get an age range", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapAgeRange(org.
   * ihtsdo.otf.mapping.jpa.MapAgeRangeJpa, java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/ageRange/add")
  @ApiOperation(value = "Add an age range", notes = "Adds the specified age range.",
      response = MapAgeRangeJpa.class)
  public MapAgeRange addMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data",
        required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /ageRange/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "add map age range", securityService);

      // Check if age range already exists and send better message
      for (final MapAgeRange range : mappingService.getMapAgeRanges().getMapAgeRanges()) {
        if (mapAgeRange.getName().equals(range.getName())) {
          throw new LocalException("This map age range already exists: " + range.getName());
        }
      }

      mappingService.addMapAgeRange(mapAgeRange);
      return mapAgeRange;

    } catch (Exception e) {
      handleException(e, "trying to add an age range", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapAgeRange(org.
   * ihtsdo.otf.mapping.jpa.MapAgeRangeJpa, java.lang.String)
   */
  @Override
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/ageRange/update")
  @ApiOperation(value = "Update an age range", notes = "Updates the specified age range.",
      response = MapAgeRangeJpa.class)
  public void updateMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data",
        required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /ageRange/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "udpate age range", securityService);

      mappingService.updateMapAgeRange(mapAgeRange);
    } catch (Exception e) {
      handleException(e, "trying to update an age range", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapAgeRange(org.
   * ihtsdo.otf.mapping.jpa.MapAgeRangeJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/ageRange/delete")
  @ApiOperation(value = "Remove an age range", notes = "Removes the specified age range.",
      response = MapAgeRangeJpa.class)
  public void removeMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data",
        required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /ageRange/delete for user " + mapAgeRange.getName());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "remove age range", securityService);

      mappingService.removeMapAgeRange(mapAgeRange.getId());
    } catch (Exception e) {
      handleException(e, "trying to remove an age range", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Relation
  // ///////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRelations(java.
   * lang.String)
   */
  @Override
  @GET
  @Path("/relation/relations")
  @ApiOperation(value = "Get all relations", notes = "Gets a list of all map relations.",
      response = MapRelationListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRelationListJpa getMapRelations(@ApiParam(value = "Authorization token",
      required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /relation/relations");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map relations", securityService);

      final MapRelationListJpa mapRelations = (MapRelationListJpa) mappingService.getMapRelations();
      mapRelations.sortBy(new Comparator<MapRelation>() {
        @Override
        public int compare(MapRelation o1, MapRelation o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      return mapRelations;
    } catch (Exception e) {
      handleException(e, "trying to return the map relations", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapRelation(org.
   * ihtsdo.otf.mapping.jpa.MapRelationJpa, java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/relation/add")
  @ApiOperation(value = "Add a map relation", notes = "Adds the specified map relation.",
      response = MapRelationJpa.class)
  public MapRelation addMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data",
        required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /relation/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "add map relation", securityService);

      // Check if relation already exists and send better message
      for (final MapRelation relation : mappingService.getMapRelations().getMapRelations()) {
        if (relation.getName().equals(mapRelation.getName())) {
          throw new LocalException("This map relation already exists: " + relation.getName());
        }
      }

      mappingService.addMapRelation(mapRelation);
      return mapRelation;

    } catch (Exception e) {
      handleException(e, "trying to add a relation", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapRelation(org.
   * ihtsdo.otf.mapping.jpa.MapRelationJpa, java.lang.String)
   */
  @Override
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/relation/update")
  @ApiOperation(value = "Update a map relation", notes = "Updates the specified map relation.",
      response = MapRelationJpa.class)
  public void updateMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data",
        required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /relation/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "update map relation", securityService);

      mappingService.updateMapRelation(mapRelation);
    } catch (Exception e) {
      handleException(e, "trying to update a relation", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapRelation(org.
   * ihtsdo.otf.mapping.jpa.MapRelationJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/relation/delete")
  @ApiOperation(value = "Remove a map relation", notes = "Removes the specified map relation.",
      response = MapRelationJpa.class)
  public void removeMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data",
        required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /relation/delete for user " + mapRelation.getName());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "remove map relation", securityService);

      mappingService.removeMapRelation(mapRelation.getId());
    } catch (Exception e) {
      LocalException le = new LocalException(
          "Unable to delete map relation. This is likely because the relation is being used by a map project or map entry");
      handleException(le, "", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Principles
  // ///////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapPrinciples(java.
   * lang.String)
   */
  @Override
  @GET
  @Path("/principle/principles")
  @ApiOperation(value = "Get all map principles", notes = "Gets a list of all map principles.",
      response = MapPrincipleListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapPrincipleListJpa getMapPrinciples(@ApiParam(value = "Authorization token",
      required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /principle/principles");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map principles", securityService);

      final MapPrincipleListJpa mapPrinciples =
          (MapPrincipleListJpa) mappingService.getMapPrinciples();
      mapPrinciples.sortBy(new Comparator<MapPrinciple>() {
        @Override
        public int compare(MapPrinciple o1, MapPrinciple o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      return mapPrinciples;
    } catch (Exception e) {
      handleException(e, "trying to return the map principles", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapPrinciple(java.
   * lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/principle/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get a map principle", notes = "Gets a map principle for the specified id.",
      response = MapPrinciple.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapPrinciple getMapPrinciple(
    @ApiParam(value = "Map principle identifer",
        required = true) @PathParam("id") Long mapPrincipleId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /principle/id/" + mapPrincipleId.toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map principle", securityService);

      final MapPrinciple mapPrinciple = mappingService.getMapPrinciple(mapPrincipleId);
      return mapPrinciple;
    } catch (Exception e) {
      handleException(e, "trying to get the map principle", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapPrinciple(org.
   * ihtsdo.otf.mapping.jpa.MapPrincipleJpa, java.lang.String)
   */
  @Override
  @PUT
  @Path("/principle/add")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Add a map principle", notes = "Adds the specified map principle.",
      response = MapPrincipleJpa.class)
  public MapPrinciple addMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data",
        required = true) MapPrincipleJpa mapPrinciple,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /principle/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "add map principle", securityService);

      // Check if principle already exists and send better message
      for (final MapPrinciple principle : mappingService.getMapPrinciples().getMapPrinciples()) {
        if (principle.getName().equals(mapPrinciple.getName())
            && principle.getPrincipleId().equals(mapPrinciple.getPrincipleId())) {
          throw new LocalException("This map principle already exists: "
              + principle.getPrincipleId() + ", " + principle.getName());
        }
      }

      final MapPrinciple result = mappingService.addMapPrinciple(mapPrinciple);
      return result;
    } catch (Exception e) {
      handleException(e, "trying to add a map principle", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapPrinciple(org.
   * ihtsdo.otf.mapping.jpa.MapPrincipleJpa, java.lang.String)
   */
  @Override
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/principle/update")
  @ApiOperation(value = "Update a map principle", notes = "Updates the specified map principle.",
      response = MapPrincipleJpa.class)
  public void updateMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data",
        required = true) MapPrincipleJpa mapPrinciple,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /principle/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "update map principle", securityService);

      mappingService.updateMapPrinciple(mapPrinciple);

    } catch (Exception e) {
      handleException(e, "trying to update a map principle", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapPrinciple(org.
   * ihtsdo.otf.mapping.jpa.MapPrincipleJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/principle/delete")
  @ApiOperation(value = "Remove a map principle", notes = "Removes the specified map principle.")
  public void removeMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data",
        required = true) MapPrincipleJpa principle,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /principle/delete for id " + principle.getId().toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "remove map principle", securityService);

      mappingService.removeMapPrinciple(principle.getId());

    } catch (Exception e) {
      LocalException le = new LocalException(
          "Unable to delete map principle. This is likely because the principle is being used by a map project or map record");
      handleException(le, "", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map User Preferences
  // ///////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapUserPreferences(
   * java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/userPreferences/user/id/{username}")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get user preferences",
      notes = "Gets user preferences for the specified username.",
      response = MapUserPreferencesJpa.class)
  public MapUserPreferences getMapUserPreferences(
    @ApiParam(value = "Username (can be specialist, lead, or admin)",
        required = true) @PathParam("username") String username,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call:  /userPreferences/user/id/" + username);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map user prefs", securityService);
      return mappingService.getMapUserPreferences(username);

    } catch (Exception e) {
      handleException(e, "trying to get the map user preferences", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapUserPreferences(
   * org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa, java.lang.String)
   */
  @Override
  @PUT
  @Path("/userPreferences/add")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Add user preferences", notes = "Adds the specified user preferences.",
      response = MapUserPreferencesJpa.class)
  public MapUserPreferences addMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data",
        required = true) MapUserPreferencesJpa mapUserPreferences,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /userPreferences/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user =
          authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "add map user prefs", securityService);

      return mappingService.addMapUserPreferences(mapUserPreferences);

    } catch (Exception e) {
      handleException(e, "trying to add map user preferences", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * updateMapUserPreferences(org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/userPreferences/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update user preferences",
      notes = "Updates the specified user preferences.", response = MapUserPreferencesJpa.class)
  public void updateMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data",
        required = true) MapUserPreferencesJpa mapUserPreferences,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping): /userPreferences/update with \n" + mapUserPreferences.toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "update map user prefs", securityService);

      mappingService.updateMapUserPreferences(mapUserPreferences);

    } catch (Exception e) {
      handleException(e, "trying to update map user preferences", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * removeMapUserPreferences(org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa,
   * java.lang.String)
   */
  @Override
  @DELETE
  @Path("/userPreferences/remove")
  @ApiOperation(value = "Remove user preferences", notes = "Removes specified user preferences.")
  public void removeMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data",
        required = true) MapUserPreferencesJpa mapUserPreferences,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /userPreferences/remove for id "
            + mapUserPreferences.getId().toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "remove map user prefs", securityService);

      mappingService.removeMapUserPreferences(mapUserPreferences.getId());

    } catch (Exception e) {
      handleException(e, "trying to remove map user preferences", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Record
  // ///////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRecord(java.lang.
   * Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get map record by id", notes = "Gets a map record for the specified id.",
      response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecord getMapRecord(
    @ApiParam(value = "Map record id", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /record/id/"
        + (mapRecordId == null ? "" : mapRecordId.toString()));

    String user = null;
    MapRecord mapRecord = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {

      mapRecord = mappingService.getMapRecord(mapRecordId);

      if (mapRecord == null) {
        throw new LocalException("The map record " + mapRecordId
            + " no longer exists, it has probably moved on in the workflow.");
      }
      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.VIEWER,
          "get map record", securityService);

      // remove notes if this is not a specialist or above
      if (!securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId())
          .hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        mapRecord.setMapNotes(null);
      }

      return mapRecord;
    } catch (Exception e) {
      handleException(e, "trying to get the map record", user,
          mapRecord == null ? "" : mapRecord.getMapProjectId().toString(),
          mapRecordId == null ? "" : mapRecordId.toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapRecord(org.ihtsdo
   * .otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @PUT
  @Path("/record/add")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Add a map record", notes = "Adds the specified map record.",
      response = MapRecordJpa.class)
  public MapRecord addMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data",
        required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /record/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "add map record", securityService);

      return mappingService.addMapRecord(mapRecord);
    } catch (Exception e) {
      handleException(e, "trying to add a map record", user, mapRecord.getMapProjectId().toString(),
          mapRecord.getId().toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapRecord(org.
   * ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/record/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a map record", notes = "Updates the specified map record.",
      response = Response.class)
  public void updateMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data",
        required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /record/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "update map record", securityService);

      mappingService.updateMapRecord(mapRecord);

    } catch (Exception e) {
      handleException(e, "trying to update the map record", user,
          mapRecord.getMapProjectId().toString(), mapRecord.getId().toString());
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapRecord(org.
   * ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/record/delete")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Remove a map record", notes = "Removes the specified map record.",
      response = Response.class)
  public Response removeMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data",
        required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping): /record/delete with map record id = " + mapRecord.toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.ADMINISTRATOR,
          "remove map record", securityService);

      mappingService.removeMapRecord(mapRecord.getId());

      return null;
    } catch (Exception e) {
      handleException(e, "trying to delete the map record", user,
          mapRecord.getMapProjectId().toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * removeMapRecordsForMapProjectAndTerminologyIds(java.util.List,
   * java.lang.Long, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/record/records/delete/project/id/{projectId}/batch")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Remove a set of map records",
      notes = "Removes map records for specified project and a set of concept terminology ids",
      response = List.class)
  public ValidationResult removeMapRecordsForMapProjectAndTerminologyIds(
    @ApiParam(value = "Terminology ids, in JSON or XML POST data",
        required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/records/delete/project/id/" + projectId
            + "/batch with string argument " + terminologyIds);

    String user = null;
    String projectName = "";
    // instantiate the needed services
    final ContentService contentService = new ContentServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.ADMINISTRATOR,
          "remove map records for project and terminology ids", securityService);

      // validation report to return errors and warnings
      final ValidationResult validationResult = new ValidationResultJpa();

      // get the map project
      final MapProject mapProject = mappingService.getMapProject(projectId);
      projectName = mapProject.getName();

      // construct a list of terminology ids to remove
      // initially set to the Api argument
      // (instantiated to avoid concurrent modification errors
      // when modifying the list for descendant concepts)
      final List<String> terminologyIdsToRemove = new ArrayList<>(terminologyIds);

      int nRecordsRemoved = 0;
      int nScopeConceptsRemoved = 0;

      validationResult
          .addMessage(terminologyIds.size() + " concepts selected for map record removal");

      // cycle over the terminology ids
      for (final String terminologyId : terminologyIdsToRemove) {

        // get all map records for this project and concept
        final MapRecordList mapRecordList =
            mappingService.getMapRecordsForProjectAndConcept(projectId, terminologyId);

        // check if map records exist
        if (mapRecordList.getCount() == 0) {
          Logger.getLogger(MappingServiceRestImpl.class)
              .warn("No records found for project for concept id " + terminologyId);
          validationResult.addWarning("No records found for concept " + terminologyId);
        } else {
          for (final MapRecord mapRecord : mapRecordList.getMapRecords()) {
            Logger.getLogger(MappingServiceRestImpl.class)
                .info("Removing map record " + mapRecord.getId() + " for concept "
                    + mapRecord.getConceptId() + ", " + mapRecord.getConceptName());

            // remove the map record
            mappingService.removeMapRecord(mapRecord.getId());

            // increment the counts
            nRecordsRemoved++;
          }
        }

        // if a non-descendant-based project (i.e. enumerated scope),
        // remove
        // scope concept
        if (!mapProject.isScopeDescendantsFlag()) {

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
      validationResult.addMessage(nRecordsRemoved + " records successfully removed");

      // if scope concepts were removed, add a success message
      if (!mapProject.isScopeDescendantsFlag()) {

        validationResult
            .addMessage(nScopeConceptsRemoved + " concepts removed from project scope definition");
      }
      // close the services and return the validation result
      return validationResult;

    } catch (Exception e) {
      handleException(e, "trying to delete map records by terminology id", user,
          terminologyIds.toString(), projectName);
      return null;
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getMapRecordsForConceptId(java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/record/concept/id/{conceptId}")
  @ApiOperation(value = "Get map records by concept id",
      notes = "Gets a list of map records for the specified concept id.",
      response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsForConceptId(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/concept/id/" + conceptId);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map records for concept",
          securityService);

      final MapRecordListJpa mapRecordList =
          (MapRecordListJpa) mappingService.getMapRecordsForConcept(conceptId);

      // return records that this user does not have permission to see
      final MapUser mapUser =
          mappingService.getMapUser(securityService.getUsernameForToken(authToken));
      final List<MapRecord> mapRecords = new ArrayList<>();

      // cycle over records and determine if this user can see them
      for (final MapRecord mr : mapRecordList.getMapRecords()) {

        // get the user's role for this record's project
        final MapUserRole projectRole =
            securityService.getMapProjectRoleForToken(authToken, mr.getMapProjectId());

        // remove notes if this is not a specialist or above
        if (!projectRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }

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

      return mapRecordList;
    } catch (Exception e) {
      handleException(e, "trying to find records by the given concept id", user, "", conceptId);
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getMapRecordsForConceptIdHistorical(java.lang.String, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @GET
  @Path("/record/concept/id/{conceptId}/project/id/{id:[0-9][0-9]*}/historical")
  @ApiOperation(value = "Get historical map records by concept id",
      notes = "Gets the latest map record revision for each map record with given concept id.",
      response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsForConceptIdHistorical(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/concept/id/" + conceptId + "/project/id/"
            + mapProjectId + "/historical");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map records for historical concept",
          securityService);

      final MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
          .getMapRecordRevisionsForConcept(conceptId, mapProjectId);

      // return records that this user does not have permission to see
      final MapUser mapUser =
          mappingService.getMapUser(securityService.getUsernameForToken(authToken));
      final List<MapRecord> mapRecords = new ArrayList<>();

      // cycle over records and determine if this user can see them
      for (final MapRecord mr : mapRecordList.getMapRecords()) {

        // get the user's role for this record's project
        final MapUserRole projectRole =
            securityService.getMapProjectRoleForToken(authToken, mr.getMapProjectId());

        // remove notes if this is not a specialist or above
        if (!projectRole.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }

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

      return mapRecordList;
    } catch (Exception e) {
      handleException(e, "trying to find historical records by the given concept id", user, "",
          conceptId);
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getMapRecordsForConceptIdHistorical(java.lang.String, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @GET
  @Path("/record/concept/id/{conceptId}/project/id/{id:[0-9][0-9]*}/users")
  @ApiOperation(value = "Get map users by concept id",
      notes = "Gets map users for all map records with given concept id.",
      response = MapUserListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapUserListJpa getMapRecordsForConceptIdHistoricalMapUsers(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/concept/id/" + conceptId + "/project/id/"
            + mapProjectId + "/users");

    String user = null;

    try (final MappingService mappingService = new MappingServiceJpa()) {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map records for historical concept",
          securityService);

      final MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
          .getMapRecordRevisionsForConcept(conceptId, mapProjectId);

      MapUserListJpa mapUserList = new MapUserListJpa();

      for (MapRecord mr : mapRecordList.getMapRecords()) {
        Hibernate.initialize(mr.getOwner());
        mapUserList.addMapUser(mr.getOwner());
      }
      return mapUserList;

    } catch (Exception e) {
      handleException(e, "trying to find historical records by the given concept id", user, "",
          conceptId);
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getMapRecordsForMapProjectAndQuery(java.lang.Long,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String,
   * java.lang.String, boolean, java.lang.String, java.lang.String)
   */
  @Override
  @POST
  @Path("/record/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get published map records by project id",
      notes = "Gets a list of map records for the specified map project id that have a workflow status of PUBLISHED or READY_FOR_PUBLICATION.",
      response = MapRecordListJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  // @CookieParam(value = "userInfo")
  public MapRecordListJpa getMapRecordsForMapProjectAndQuery(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Ancestor concept (inclusive) to restrict search results to",
        required = true) @QueryParam("ancestorId") String ancestorId,
    @ApiParam(value = "Source concept relationship name",
        required = false) @QueryParam("relationshipName") String relationshipName,
    @ApiParam(value = "Destination concept relationship value",
        required = false) @QueryParam("relationshipValue") String relationshipValue,
    @ApiParam(value = "Excludes descendants of ancestor id ",
        required = false) @QueryParam("excludeDescendants") boolean excludeDescendants,
    @ApiParam(value = "Include non-published maps ",
        required = false) @QueryParam("includeNonPublished") boolean includeNonPublished,
    @ApiParam(value = "Search query string", required = false) @QueryParam("query") String query,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/project/id/" + mapProjectId + " " + ancestorId + ", "
            + excludeDescendants + ", " + query);
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();

    // declare content service (may not be used)
    ContentService contentService = null;

    try {

      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "find map records for map project and query", securityService);

      // determine if a query was passed
      boolean queryFlag = (query != null && !query.isEmpty() && !query.equals("null"))
          || (pfsParameter.getQueryRestriction() != null
              && !pfsParameter.getQueryRestriction().isEmpty());
      boolean ancestorFlag =
          ancestorId != null && !ancestorId.isEmpty() && !ancestorId.equals("null");

      boolean relationshipFlag = relationshipName != null && !relationshipName.isEmpty()
          && !relationshipName.equals("null");

      // instantiate the list to be returned
      final MapRecordListJpa mapRecordList = new MapRecordListJpa();

      // create local pfs parameter for query restriction modification
      final PfsParameter pfsLocal = new PfsParameterJpa(pfsParameter);
      final String queryLocal = queryFlag ? query : null;
      final PfsParameter descendantPfs = new PfsParameterJpa();
      descendantPfs.setStartIndex(pfsLocal.getStartIndex());
      descendantPfs.setMaxResults(pfsLocal.getMaxResults());

      // the revised query restriction (local variable for convenience)
      String queryRestriction = pfsLocal.getQueryRestriction();

      // if query restriction supplied, add an AND
      if (queryRestriction != null && !queryRestriction.isEmpty()) {
        queryRestriction += " AND ";
      }

      // add the map project restriction
      queryRestriction += "mapProjectId:" + mapProjectId;

      // add the role-specific workflow restrictions
      final MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        if (!includeNonPublished) {
          queryRestriction +=
              " AND (workflowStatus:PUBLISHED OR workflowStatus:READY_FOR_PUBLICATION)";
        } else {
          // Do nothing - return all map records, regardless of workflow status.
        }
      } else {
        queryRestriction += " AND workflowStatus:PUBLISHED";
      }

      // set the pfs query restriction
      pfsLocal.setQueryRestriction(queryRestriction);

      SearchResultList searchResults;

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // if ancestor id specified, need to retrieve all results
      if (ancestorFlag || relationshipFlag) {

        // If there was a search query, combine them
        if (queryFlag) {
          pfsLocal.setStartIndex(0);
          pfsLocal.setMaxResults(MAX_RESULTS);

          // perform lucene search
          searchResults = (queryFlag ? mappingService.findMapRecordsForQuery(queryLocal, pfsLocal)
              : new SearchResultListJpa());

          if (searchResults.getTotalCount() > MAX_RESULTS) {
            throw new LocalException(searchResults.getTotalCount()
                + " potential string matches for ancestor or relationship search. Narrow your search and try again.");
          }
          if (searchResults.getCount() > 0) {
            ImmutableMap<String, SearchResult> resultsMap = Maps.uniqueIndex(
                searchResults.getSearchResults(), new Function<SearchResult, String>() {

                  @Override
                  public String apply(SearchResult input) {
                    return input.getTerminologyId();
                  }

                });
            searchResults =
                mappingService.findMapRecords(mapProjectId, ancestorId, excludeDescendants,
                    relationshipName, relationshipValue, mapProject.getSourceTerminology(),
                    mapProject.getSourceTerminologyVersion(), descendantPfs, resultsMap.keySet());
          }

        }

        else {
          // Otherwise, just find all map records to include or exclude
          // descendants
          searchResults = mappingService.findMapRecords(mapProjectId, ancestorId,
              excludeDescendants, relationshipName, relationshipValue,
              mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion(),
              descendantPfs, Collections.<String> emptySet());

        }

        // workaround for typing problems between List<SearchResultJpa>
        // and
        // List<SearchResult>
        List<SearchResultJpa> results = new ArrayList<>();
        for (SearchResult sr : searchResults.getSearchResults()) {
          results.add((SearchResultJpa) sr);
        }

        // apply paging to the list -- note: use original pfs
        pfsLocal.setMaxResults(pfsParameter.getMaxResults());
        pfsLocal.setStartIndex(pfsParameter.getStartIndex());
        pfsLocal.setQueryRestriction(null);

        // check for sort field requests requiring special handling
        // due to field inconsistency between MapRecord and SearchResult
        if (pfsLocal.getSortField() != null
            && pfsLocal.getSortField().toLowerCase().equals("conceptid")) {
          pfsLocal.setSortField("terminologyId");
        } else if (pfsLocal.getSortField() != null
            && pfsLocal.getSortField().toLowerCase().equals("conceptname")) {
          pfsLocal.setSortField("value");
        }

        // reconstruct the assignedWork search result list
        searchResults.setSearchResults(new ArrayList<SearchResult>(results));

      }

      // otherwise not ancestor or relationship flags, use default paging
      else {

        // perform lucene search
        searchResults = mappingService.findMapRecordsForQuery(queryLocal, pfsLocal);
      }

      // retrieve the records for the paged results
      for (SearchResult sr : searchResults.getSearchResults()) {
        mapRecordList.addMapRecord(mappingService.getMapRecord(sr.getId()));
      }

      // set the total count
      mapRecordList.setTotalCount(searchResults.getTotalCount());

      // remove notes if this is not a specialist or above
      // unless the project is set to mapNotesPublic=true
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST) && !mapProject.isMapNotesPublic()) {
        for (final MapRecord mr : mapRecordList.getMapRecords()) {
          mr.setMapNotes(null);
        }
      }

      Set<String> scopeConcepts = ConfigUtility.getScopeConceptsForMapProject(mapProject.getId());
      for (final MapRecord mr : mapRecordList.getMapRecords()) {
        if (scopeConcepts != null && !scopeConcepts.contains(mr.getConceptId())) {
          mr.addLabel("Out of Scope");
        }
      }

      return mapRecordList;
    } catch (Exception e) {
      handleException(e, "trying to get the map records for a map project and query", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
      if (contentService != null) {
        contentService.close();
      }
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getPublishedMapRecordsForMapProject(java.lang.Long,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/record/project/id/{id:[0-9][0-9]*}/published")
  @ApiOperation(value = "Get published map records by map project id",
      notes = "Gets a list of map records for the specified map project id that have a workflow status of PUBLISHED.",
      response = MapRecordListJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  // @CookieParam(value = "userInfo")
  public MapRecordListJpa getPublishedMapRecordsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/project/id/" + mapProjectId.toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    // execute the service call
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get published records for project",
          securityService);

      final MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
          .getPublishedMapRecordsForMapProject(mapProjectId, pfsParameter);

      final MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      for (final MapRecord mr : mapRecordList.getMapRecords()) {
        // remove notes if this is not a specialist or above
        if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }
      }
      return mapRecordList;
    } catch (Exception e) {
      handleException(e, "trying to get the map records for a map project", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getPublishedMapRecordsForMapProject(java.lang.Long,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/record/project/id/{id:[0-9][0-9]*}/readyforpublication")
  @ApiOperation(value = "Get published and ready for publication map records by map project id",
      notes = "Gets a list of map records for the specified map project id that have a workflow status of PUBLISHED or READY_FOR_PUBLICATION.",
      response = MapRecordListJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  // @CookieParam(value = "userInfo")
  public MapRecordListJpa getPublishedAndReadyForPublicationMapRecordsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/project/id/" + mapProjectId.toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    // execute the service call
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get published and ready for publication records for project", securityService);

      final MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
          .getPublishedAndReadyForPublicationMapRecordsForMapProject(mapProjectId, pfsParameter);

      final MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      for (final MapRecord mr : mapRecordList.getMapRecords()) {
        // remove notes if this is not a specialist or above
        if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }
      }
      return mapRecordList;
    } catch (Exception e) {
      handleException(e, "trying to get the map records for a map project", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRecordRevisions(
   * java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/revisions")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get map record revision history",
      notes = "Gets a list of all revisions of a map record for the specified id.",
      response = MapRecordListJpa.class)
  public MapRecordList getMapRecordRevisions(
    @ApiParam(value = "Map record id, e.g. 28123",
        required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/id/" + mapRecordId + "/revisions");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      final MapUserRole role = securityService.getMapProjectRoleForToken(authToken, mapRecordId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to get the map record revisions.").build());

      final MapRecordList revisions = mappingService.getMapRecordRevisions(mapRecordId);

      for (final MapRecord mr : revisions.getMapRecords()) {
        // remove notes if this is not a specialist or above
        if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }
      }
      return revisions;
    } catch (Exception e) {
      handleException(e, "trying to get the map record revisions", user, "",
          mapRecordId.toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRecordHistorical(
   * java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/historical")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get latest state of a map record",
      notes = "Gets the current form of the map record or its last historical state for the specified map record id.",
      response = MapRecordListJpa.class)
  public MapRecord getMapRecordHistorical(
    @ApiParam(value = "Map record id, e.g. 28123",
        required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/id/" + mapRecordId + "/historical");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get historical map records for project",
          securityService);

      // try getting the current record
      MapRecord mapRecord = mappingService.getMapRecord(mapRecordId);

      // if no current record, look for revisions
      if (mapRecord == null) {
        List<MapRecord> list = mappingService.getMapRecordRevisions(mapRecordId).getMapRecords();
        mapRecord = list.get(0);
        mapRecord.getMapEntries().size();
      }

      final MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        mapRecord.setMapNotes(null);
      }

      return mapRecord;

    } catch (Exception e) {
      handleException(e, "trying to get the map record potentially using historical revisions",
          user, "", mapRecordId.toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  // //////////////////////////////////////////
  // Relation and Advice Computation
  // /////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#computeMapRelation(org.
   * ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/relation/compute")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Compute map relation",
      notes = "Gets the computed map relation for the indicated map entry of the map record.",
      response = MapRelationJpa.class)
  public MapRelation computeMapRelation(
    @ApiParam(value = "Map record, in JSON or XML POST data",
        required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /relation/compute");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "compute map relation", securityService);

      final ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(
              mappingService.getMapProject(mapRecord.getMapProjectId()));

      // We need the full in-memory (and unsaved) representation of the
      // map
      // record.
      // The entry in question is (hackishly identified by having an id of
      // -1);
      MapEntry mapEntry = null;
      for (final MapEntry entry : mapRecord.getMapEntries()) {
        if (entry.getId() != null && entry.getId() == -1) {
          mapEntry = entry;
        }
      }

      // Make sure map entries are sortedy by mapGroup/mapPriority
      Collections.sort(mapRecord.getMapEntries(), new TerminologyUtility.MapEntryComparator());

      final MapRelation mapRelation = algorithmHandler.computeMapRelation(mapRecord, mapEntry);
      return mapRelation;

    } catch (Exception e) {
      handleException(e, "trying to compute the map relations", user,
          mapRecord == null ? "" : mapRecord.getMapProjectId().toString(),
          mapRecord == null ? "" : mapRecord.getId().toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#computeMapAdvice(java.
   * lang.Integer, org.ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/advice/compute/{entryIndex}")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Compute map advices",
      notes = "Gets the computed map advices for the indicated map entry of the map record.",
      response = MapAdviceJpa.class)
  public MapAdviceList computeMapAdvice(
    @ApiParam(value = "Index of entries in map record to compute advice for",
        required = true) @PathParam("entryIndex") Integer entryIndex,
    @ApiParam(value = "Map record, in JSON or XML POST data",
        required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // call log
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /advice/compute");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // Bail if there are no entries for entryIndex
      if (mapRecord == null || mapRecord.getMapEntries() == null
          || mapRecord.getMapEntries().size() <= entryIndex) {
        return new MapAdviceListJpa();
      }

      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "compute map advice", securityService);

      final ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(
              mappingService.getMapProject(mapRecord.getMapProjectId()));

      final MapEntry mapEntry = mapRecord.getMapEntries().get(entryIndex);

      // bail if not ready yet
      if (mapEntry == null || mapEntry.getTargetId() == null) {
        return new MapAdviceListJpa();
      }

      // Make sure map entries are sorted by by mapGroup/mapPriority
      Collections.sort(mapRecord.getMapEntries(), new TerminologyUtility.MapEntryComparator());

      final MapAdviceList mapAdviceList = algorithmHandler.computeMapAdvice(mapRecord, mapEntry);
      return mapAdviceList;

    } catch (Exception e) {
      handleException(e, "trying to compute the map advice", user,
          mapRecord == null ? "" : mapRecord.getMapProjectId().toString(),
          mapRecord == null ? "" : mapRecord.getId().toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.ReportServiceRest#getQALabels(java.lang.
   * Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/tags/{mapProjectId}")
  @ApiOperation(value = "Gets all  tags", notes = "Gets all unique tags for map project",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getTagsForMapProject(
    @ApiParam(value = "Map Project id",
        required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call:  /tags/" + mapProjectId);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get tags", securityService);

      final SearchResultList results = mappingService.getTagsForMapProject(mapProjectId);

      return results;

    } catch (Exception e) {
      handleException(e, "trying to get tags", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.ReportServiceRest#getQALabels(java.lang.
   * Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/clearDataOnChange/{mapProjectId}")
  @ApiOperation(value = "Gets clear data on change", notes = "Returns whether map project is set to clear data on a target change",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Boolean getClearDataOnChange(
    @ApiParam(value = "Map Project id",
        required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call:  /clearDataOnChange/" + mapProjectId);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get tags", securityService);

      final Boolean clearDataOnChange = mappingService.getClearDataOnChangeForMapProject(mapProjectId);

      return clearDataOnChange;

    } catch (Exception e) {
      handleException(e, "trying to get clear data on change", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }  
  
  // ///////////////////////////////////////////////
  // Role Management Services
  // ///////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getMapUserRoleForMapProject(java.lang.String, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @GET
  @Path("/userRole/user/id/{username}/project/id/{id:[0-9][0-9]*}")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get the user's role for a map project",
      notes = "Gets the role for the specified user and map project.",
      response = SearchResultList.class)
  public MapUserRole getMapUserRoleForMapProject(
    @ApiParam(value = "Username (can be specialist, lead, or admin)",
        required = true) @PathParam("username") String username,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call:  /userRole/user/id" + username + "/project/id/" + mapProjectId);

    final MappingService mappingService = new MappingServiceJpa();
    try {
      final MapUserRole mapUserRole =
          mappingService.getMapUserRoleForMapProject(username, mapProjectId);
      return mapUserRole;
    } catch (Exception e) {
      handleException(e, "trying to get the map user role for a map project", username,
          mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @GET
  @Path("/userRole/user/id/{username}")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get the user's application role",
      notes = "Gets the application role for the specified user.", response = MapUserRole.class)
  public MapUserRole getMapUserRoleForApplication(
    @ApiParam(value = "Username (can be specialist, lead, or admin)",
        required = true) @PathParam("username") String username,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call:  /userRole/user/id/" + username);

    final MappingService mappingService = new MappingServiceJpa();
    try {
      final MapUserRole mapUserRole = mappingService.getMapUserRoleForApplication(username);
      return mapUserRole;
    } catch (Exception e) {
      handleException(e, "trying to get the map user application role", username, null, "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // /////////////////////////
  // Descendant services
  // /////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getUnmappedDescendantsForConcept(java.lang.String, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @GET
  @Path("/concept/id/{terminologyId}/unmappedDescendants/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Find unmapped descendants of a concept",
      notes = "Gets a list of search results for concepts having unmapped descendants.",
      response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getUnmappedDescendantsForConcept(
    @ApiParam(value = "Concept terminology id, e.g. 22298006",
        required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping): /concept/id/" + terminologyId + "/project/id/" + mapProjectId);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get unmapped descendants for concept",
          securityService);

      final SearchResultList results =
          mappingService.findUnmappedDescendantsForConcept(terminologyId, mapProjectId, null);

      return results;

    } catch (Exception e) {
      handleException(e, "trying to get unmapped descendants for a concept", user, "",
          terminologyId);
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // /////////////////////////////////////////////////////
  // Tree Position Routines for Terminology Browser
  // /////////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getTreePositionWithDescendantsForConceptAndMapProject(java.lang.String,
   * java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/treePosition/project/id/{mapProjectId}/concept/id/{terminologyId}")
  @ApiOperation(value = "Gets project-specific tree positions with desendants",
      notes = "Gets a list of tree positions and their descendants for the specified parameters. Sets flags for valid targets and assigns any terminology notes based on project.",
      response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getTreePositionWithDescendantsForConceptAndMapProject(
    @ApiParam(value = "Concept terminology id, e.g. 22298006",
        required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken

  ) throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /treePosition/project/id/" + mapProjectId.toString()
            + "/concept/id/" + terminologyId);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();

    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get tree positions with descendants for concept and project", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // get the local tree positions from content service
      final TreePositionList treePositions = contentService.getTreePositionsWithChildren(
          terminologyId, mapProject.getDestinationTerminology(),
          mapProject.getDestinationTerminologyVersion());
      Logger.getLogger(getClass()).info("  treepos count = " + treePositions.getTotalCount());
      if (treePositions.getCount() == 0) {
        return new TreePositionListJpa();
      }

      final String terminology = treePositions.getTreePositions().get(0).getTerminology();
      final String terminologyVersion =
          treePositions.getTreePositions().get(0).getTerminologyVersion();
      final Map<String, String> descTypes =
          metadataService.getDescriptionTypes(terminology, terminologyVersion);
      final Map<String, String> relTypes =
          metadataService.getRelationshipTypes(terminology, terminologyVersion);

      // Calculate info for tree position information panel
      contentService.computeTreePositionInformation(treePositions, descTypes, relTypes);

      // Determine whether code is valid (e.g. whether it should be a
      // link)
      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      mappingService.setTreePositionValidCodes(mapProject, treePositions, handler);
      // Compute any additional project specific handler info
      mappingService.setTreePositionTerminologyNotes(mapProject, treePositions, handler);

      return treePositions;
    } catch (Exception e) {
      handleException(e, "trying to get the tree positions with descendants", user,
          mapProjectId.toString(), terminologyId);
      return null;
    } finally {
      metadataService.close();
      contentService.close();
      mappingService.close();
      securityService.close();
    }
  }

  // /////////////////////////////////////////////////////
  // Tree Position Routines for Terminology Browser
  // /////////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getTreePositionWithDescendantsForConceptAndMapProject(java.lang.String,
   * java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/treePosition/project/id/{mapProjectId}/concept/id/{terminologyId}/source")
  @ApiOperation(value = "Gets project-specific tree positions with desendants",
      notes = "Gets a list of tree positions and their descendants for the specified parameters. Sets flags for valid targets and assigns any terminology notes based on project.",
      response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getSourceTreePositionWithDescendantsForConceptAndMapProject(
    @ApiParam(value = "Concept terminology id, e.g. 22298006",
        required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken

  ) throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /treePosition/project/id/" + mapProjectId.toString()
            + "/concept/id/" + terminologyId + "/source");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();

    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get tree positions with descendants for concept and project", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      final MapProjectList allProjects = mappingService.getMapProjects();
      MapProject associatedMapProject = null;
      for (MapProject project : allProjects.getIterable()) {

        if (mapProject.getSourceTerminology().equalsIgnoreCase(project.getDestinationTerminology())
            && mapProject.getSourceTerminologyVersion()
                .equalsIgnoreCase(project.getDestinationTerminologyVersion())) {
          associatedMapProject = project;
          break;
        }
      }

      // get the local tree positions from content service
      final TreePositionList treePositions =
          contentService.getTreePositionsWithChildren(terminologyId,
              mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());
      Logger.getLogger(getClass()).info("  treepos count = " + treePositions.getTotalCount());
      if (treePositions.getCount() == 0) {
        return new TreePositionListJpa();
      }

      final String terminology = treePositions.getTreePositions().get(0).getTerminology();
      final String terminologyVersion =
          treePositions.getTreePositions().get(0).getTerminologyVersion();
      final Map<String, String> descTypes =
          metadataService.getDescriptionTypes(terminology, terminologyVersion);
      final Map<String, String> relTypes =
          metadataService.getRelationshipTypes(terminology, terminologyVersion);

      // Calculate info for tree position information panel
      contentService.computeTreePositionInformation(treePositions, descTypes, relTypes);

      // If there is an associated reverse-project, you can use its specified
      // handler to calculate code validity and notes.
      if (associatedMapProject != null) {
        // Determine whether code is valid (e.g. whether it should be a
        // link)
        final ProjectSpecificAlgorithmHandler handler =
            mappingService.getProjectSpecificAlgorithmHandler(associatedMapProject);
        mappingService.setTreePositionValidCodes(associatedMapProject, treePositions, handler);
        // Compute any additional project specific handler info
        mappingService.setTreePositionTerminologyNotes(associatedMapProject, treePositions,
            handler);
      }

      return treePositions;
    } catch (Exception e) {
      handleException(e, "trying to get the tree positions with descendants", user,
          mapProjectId.toString(), terminologyId);
      return null;
    } finally {
      metadataService.close();
      contentService.close();
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getDestinationRootTreePositionsForMapProject(java.lang.Long,
   * java.lang.String)
   */
  @Override
  @GET
  @Path("/treePosition/project/id/{projectId}/destination")
  @ApiOperation(value = "Get root tree positions",
      notes = "Gets a list of tree positions at the root of the terminology.",
      response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getDestinationRootTreePositionsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /treePosition/project/id/" + mapProjectId.toString()
            + "/destination");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get destination root tree positions for project", securityService);

      // set the valid codes using mapping service
      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // get the root tree positions from content service
      final TreePositionList treePositions = contentService.getRootTreePositions(
          mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());
      Logger.getLogger(getClass()).info("  treepos count = " + treePositions.getTotalCount());
      if (treePositions.getCount() == 0) {
        return new TreePositionListJpa();
      }

      final String terminology = treePositions.getTreePositions().get(0).getTerminology();
      final String terminologyVersion =
          treePositions.getTreePositions().get(0).getTerminologyVersion();
      final Map<String, String> descTypes =
          metadataService.getDescriptionTypes(terminology, terminologyVersion);
      final Map<String, String> relTypes =
          metadataService.getRelationshipTypes(terminology, terminologyVersion);

      contentService.computeTreePositionInformation(treePositions, descTypes, relTypes);

      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      mappingService.setTreePositionValidCodes(mapProject, treePositions, handler);

      return treePositions;
    } catch (Exception e) {
      handleException(e, "trying to get the root tree positions for a terminology", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      metadataService.close();
      mappingService.close();
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getSourceRootTreePositionsForMapProject(java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/treePosition/project/id/{projectId}/source")
  @ApiOperation(value = "Get source terminology root tree positions",
      notes = "Gets a list of tree positions at the root of the terminology.",
      response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getSourceRootTreePositionsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping): /treePosition/project/id/" + mapProjectId.toString() + "/source");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get source root tree positions for project", securityService);

      // set the valid codes using mapping service
      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      final MapProjectList allProjects = mappingService.getMapProjects();
      MapProject associatedMapProject = null;
      for (MapProject project : allProjects.getIterable()) {

        if (mapProject.getSourceTerminology().equalsIgnoreCase(project.getDestinationTerminology())
            && mapProject.getSourceTerminologyVersion()
                .equalsIgnoreCase(project.getDestinationTerminologyVersion())) {
          associatedMapProject = project;
          break;
        }
      }

      // get the root tree positions from content service
      final TreePositionList treePositions = contentService.getRootTreePositions(
          mapProject.getSourceTerminology(), mapProject.getSourceTerminologyVersion());
      Logger.getLogger(getClass()).info("  treepos count = " + treePositions.getTotalCount());
      if (treePositions.getCount() == 0) {
        return new TreePositionListJpa();
      }

      final String terminology = treePositions.getTreePositions().get(0).getTerminology();
      final String terminologyVersion =
          treePositions.getTreePositions().get(0).getTerminologyVersion();
      final Map<String, String> descTypes =
          metadataService.getDescriptionTypes(terminology, terminologyVersion);
      final Map<String, String> relTypes =
          metadataService.getRelationshipTypes(terminology, terminologyVersion);

      // If there is an associated reverse-project, you can use its specified
      // handler to calculate code validity and notes.
      if (associatedMapProject != null) {
        // Determine whether code is valid (e.g. whether it should be a
        // link)
        final ProjectSpecificAlgorithmHandler handler =
            mappingService.getProjectSpecificAlgorithmHandler(associatedMapProject);
        mappingService.setTreePositionValidCodes(associatedMapProject, treePositions, handler);
        // Compute any additional project specific handler info
        mappingService.setTreePositionTerminologyNotes(associatedMapProject, treePositions,
            handler);
      }

      contentService.computeTreePositionInformation(treePositions, descTypes, relTypes);

      return treePositions;
    } catch (Exception e) {
      handleException(e, "trying to get the root tree positions for a terminology", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      metadataService.close();
      mappingService.close();
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getTreePositionGraphsForQueryAndMapProject(java.lang.String,
   * java.lang.Long, org.ihtsdo.otf.mapping.helpers.PfsParameterJpa,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/treePosition/project/id/{projectId}")
  @ApiOperation(value = "Get tree positions for query",
      notes = "Gets a list of tree positions for the specified parameters.",
      response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getTreePositionGraphsForQueryAndMapProject(
    @ApiParam(value = "Terminology browser query, e.g. 'cholera'",
        required = true) @QueryParam("query") String query,
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = false) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Mapping): /treePosition/project/id/" + mapProjectId + " " + query);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get tree position graphs for query", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // formulate an "and" search from the query if it doesn't use
      // special chars
      boolean plusFlag = false;
      final StringBuilder qb = new StringBuilder();
      if (!query.contains("\"") && !query.contains("-") && !query.contains("+")
          && !query.contains("*") && !query.matches(".[0-9].*[A-Z]")) {
        plusFlag = true;
        for (final String word : query.split("\\s")) {
          qb.append("+").append(word).append(" ");
        }
      } else if (query.matches(".[0-9].*[A-Z]")) {
        qb.append("\"").append(query).append("\"");
      } else {
        qb.append(query);
      }

      // TODO: need to figure out what "paging" means - it really has to
      // do
      // with the number of tree positions under the root node, I think.
      final PfsParameter pfs = pfsParameter != null ? pfsParameter : new PfsParameterJpa();
      // pfs.setStartIndex(0);
      // pfs.setMaxResults(10);

      // get the tree positions from concept service
      TreePositionList treePositions =
          contentService.getTreePositionGraphForQuery(mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion(), qb.toString(), pfs);
      Logger.getLogger(getClass()).info("  treepos count* = " + treePositions.getTotalCount());
      if (treePositions.getCount() == 0) {
        // Re-try search without +
        if (plusFlag) {
          treePositions =
              contentService.getTreePositionGraphForQuery(mapProject.getDestinationTerminology(),
                  mapProject.getDestinationTerminologyVersion(), query, pfs);
        }
        if (treePositions.getCount() == 0) {
          return new TreePositionListJpa();
        }
      }

      final String terminology = treePositions.getTreePositions().get(0).getTerminology();
      final String terminologyVersion =
          treePositions.getTreePositions().get(0).getTerminologyVersion();
      final Map<String, String> descTypes =
          metadataService.getDescriptionTypes(terminology, terminologyVersion);
      final Map<String, String> relTypes =
          metadataService.getRelationshipTypes(terminology, terminologyVersion);

      // set the valid codes using mapping service
      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);

      // Limit tree positions
      treePositions.setTreePositions(handler.limitTreePositions(treePositions.getTreePositions()));

      contentService.computeTreePositionInformation(treePositions, descTypes, relTypes);

      mappingService.setTreePositionValidCodes(mapProject, treePositions, handler);
      mappingService.setTreePositionTerminologyNotes(mapProject, treePositions, handler);

      // TODO: if there are too many tree positions, then chop the tree
      // off (2
      // levels?)
      return treePositions;

    } catch (

    Exception e) {
      handleException(e, "trying to get the tree position graphs for a query", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      metadataService.close();
      mappingService.close();
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getTreePositionGraphsForQueryAndMapProject(java.lang.String,
   * java.lang.Long, org.ihtsdo.otf.mapping.helpers.PfsParameterJpa,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/treePosition/project/id/{projectId}/source")
  @ApiOperation(value = "Get tree positions for query",
      notes = "Gets a list of tree positions for the specified parameters.",
      response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getSourceTreePositionGraphsForQueryAndMapProject(
    @ApiParam(value = "Terminology browser query, e.g. 'cholera'",
        required = true) @QueryParam("query") String query,
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = false) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Mapping): /treePosition/project/id/"
        + mapProjectId + "/source" + " " + query);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get tree position graphs for query", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      final MapProjectList allProjects = mappingService.getMapProjects();
      MapProject associatedMapProject = null;
      for (MapProject project : allProjects.getIterable()) {

        if (mapProject.getSourceTerminology().equalsIgnoreCase(project.getDestinationTerminology())
            && mapProject.getSourceTerminologyVersion()
                .equalsIgnoreCase(project.getDestinationTerminologyVersion())) {
          associatedMapProject = project;
          break;
        }
      }

      // formulate an "and" search from the query if it doesn't use
      // special chars
      boolean plusFlag = false;
      final StringBuilder qb = new StringBuilder();
      if (!query.contains("\"") && !query.contains("-") && !query.contains("+")
          && !query.contains("*")) {
        plusFlag = true;
        for (final String word : query.split("\\s")) {
          qb.append("+").append(word).append(" ");
        }
      }

      else {
        qb.append(query);
      }

      // TODO: need to figure out what "paging" means - it really has to
      // do
      // with the number of tree positions under the root node, I think.
      final PfsParameter pfs = pfsParameter != null ? pfsParameter : new PfsParameterJpa();
      // pfs.setStartIndex(0);
      // pfs.setMaxResults(10);

      // get the tree positions from concept service
      TreePositionList treePositions =
          contentService.getTreePositionGraphForQuery(mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion(), qb.toString(), pfs);
      Logger.getLogger(getClass()).info("  treepos count = " + treePositions.getTotalCount());
      if (treePositions.getCount() == 0) {
        // Re-try search without +
        if (plusFlag) {
          treePositions =
              contentService.getTreePositionGraphForQuery(mapProject.getSourceTerminology(),
                  mapProject.getSourceTerminologyVersion(), query, pfs);
        }
        if (treePositions.getCount() == 0) {
          return new TreePositionListJpa();
        }
      }

      final String terminology = treePositions.getTreePositions().get(0).getTerminology();
      final String terminologyVersion =
          treePositions.getTreePositions().get(0).getTerminologyVersion();
      final Map<String, String> descTypes =
          metadataService.getDescriptionTypes(terminology, terminologyVersion);
      final Map<String, String> relTypes =
          metadataService.getRelationshipTypes(terminology, terminologyVersion);

      // If there is an associated reverse-project, you can use its specified
      // handler to calculate code validity and notes.
      if (associatedMapProject != null) {
        // Determine whether code is valid (e.g. whether it should be a
        // link)
        final ProjectSpecificAlgorithmHandler handler =
            mappingService.getProjectSpecificAlgorithmHandler(associatedMapProject);
        mappingService.setTreePositionValidCodes(associatedMapProject, treePositions, handler);
        // Compute any additional project specific handler info
        mappingService.setTreePositionTerminologyNotes(associatedMapProject, treePositions,
            handler);
        // Limit tree positions
        treePositions
            .setTreePositions(handler.limitTreePositions(treePositions.getTreePositions()));
      }

      contentService.computeTreePositionInformation(treePositions, descTypes, relTypes);

      // TODO: if there are too many tree positions, then chop the tree
      // off (2
      // levels?)
      return treePositions;

    } catch (

    Exception e) {
      handleException(e, "trying to get the tree position graphs for a query", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      metadataService.close();
      mappingService.close();
      contentService.close();
      securityService.close();
    }
  }

  // //////////////////////////////////////////////////
  // Workflow-related routines
  // /////////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getMapRecordsEditedByMapUser(java.lang.Long, java.lang.String,
   * org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/record/project/id/{id}/user/id/{username}/edited")
  @ApiOperation(value = "Get map records edited by a user",
      notes = "Gets a list of map records for the specified map project and user.",
      response = MapRecordListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsEditedByMapUser(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Username (can be specialist, lead, or admin)",
        required = true) @PathParam("username") String username,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data",
        required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/project/id/" + mapProjectId + "/user/id/" + username
            + "/edited");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get map records edited by map user", securityService);

      final MapRecordListJpa recordList = (MapRecordListJpa) mappingService
          .getRecentlyEditedMapRecords(Long.valueOf(mapProjectId), username, pfsParameter);
      return recordList;
    } catch (Exception e) {
      handleException(e, "trying to get the recently edited map records", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getOriginMapRecordsForConflict(java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/conflictOrigins")
  @ApiOperation(value = "Get specialist records for an assigned conflict or review record",
      notes = "Gets a list of specialist map records corresponding to a lead conflict or review record.",
      response = MapRecordListJpa.class)
  public MapRecordList getOriginMapRecordsForConflict(
    @ApiParam(value = "Map record id, e.g. 28123",
        required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/id/" + mapRecordId + "/conflictOrigins");
    String user = null;
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final MapRecord mapRecord = workflowService.getMapRecord(mapRecordId);

      if (mapRecord == null) {
        throw new LocalException("The map record " + mapRecordId
            + " no longer exists, it has probably moved on in the workflow.");
      }

      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken, MapUserRole.SPECIALIST,
          "get origin records for conflict", securityService);

      // get the project
      MapProject project = workflowService.getMapProject(mapRecord.getMapProjectId());

      // find the tracking record for this map record
      TrackingRecord trackingRecord = workflowService
          .getTrackingRecordForMapProjectAndConcept(project, mapRecord.getConceptId());

      if (trackingRecord == null) {
        throw new LocalException(
            "Tracking record is unexpectedly missing for this project/concept");
      }
      // instantiate workflow handler for this tracking record
      WorkflowPathHandler handler =
          workflowService.getWorkflowPathHandler(trackingRecord.getWorkflowPath().toString());

      // get the origin map records
      MapRecordList mapRecords =
          handler.getOriginMapRecordsForMapRecord(mapRecord, workflowService);

      return mapRecords;

      // return
      // mappingService.getOriginMapRecordsForConflict(mapRecordId);

    } catch (Exception e) {
      handleException(e, "trying to get origin records for user review", user, "",
          mapRecordId.toString());
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  // //////////////////////////////////////////////
  // Map Record Validation and Compare Services
  // //////////////////////////////////////////////

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#validateMapRecord(org.
   * ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/validation/record/validate")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Validate a map record",
      notes = "Performs validation checks on a map record and returns the validation results.",
      response = MapRecordJpa.class)
  public ValidationResult validateMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data",
        required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /validation/record/validate for map record id = "
            + mapRecord.getId().toString());

    // get the map project for this record

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "validate map record", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapRecord.getMapProjectId());
      final ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      // Make sure map entries are sorted by by mapGroup/mapPriority
      Collections.sort(mapRecord.getMapEntries(), new TerminologyUtility.MapEntryComparator());

      final ValidationResult validationResult = algorithmHandler.validateRecord(mapRecord);
      return validationResult;
    } catch (Exception e) {
      handleException(e, "trying to validate a map record", user,
          mapRecord.getMapProjectId().toString(), mapRecord.getId().toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#compareMapRecords(java.
   * lang.Long, java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/validation/record/id/{recordId1}/record/id/{recordId2}/compare")
  @ApiOperation(value = "Compare two map records",
      notes = "Compares two map records and returns the validation results.",
      response = ValidationResultJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ValidationResult compareMapRecords(
    @ApiParam(value = "Map record id, e.g. 28123",
        required = true) @PathParam("recordId1") Long mapRecordId1,
    @ApiParam(value = "Map record id, e.g. 28124",
        required = true) @PathParam("recordId2") Long mapRecordId2,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /validation/record/id/" + mapRecordId1 + "record/id/"
            + mapRecordId1 + "/compare");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {

      final MapRecord mapRecord1 = mappingService.getMapRecord(mapRecordId1);
      final MapRecord mapRecord2 = mappingService.getMapRecord(mapRecordId2);

      // authorize call
      user = authorizeProject(mapRecord1.getMapProjectId(), authToken, MapUserRole.VIEWER,
          "compareMapRecords", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapRecord1.getMapProjectId());
      final ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      final ValidationResult validationResult =
          algorithmHandler.compareMapRecords(mapRecord1, mapRecord2);

      return validationResult;

    } catch (Exception e) {
      handleException(e, "trying to compare map records", user, "", mapRecordId1.toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#isTargetCodeValid(java.
   * lang.Long, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/project/id/{mapProjectId}/concept/isValid")
  @ApiOperation(value = "Indicate whether a target code is valid",
      notes = "Gets either a valid concept corresponding to the id, or returns null if not valid.",
      response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Concept isTargetCodeValid(
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Concept terminology id, e.g. 22298006",
        required = true) @QueryParam("terminologyId") String terminologyId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRestImpl.class).info("RESTful call (Mapping): /project/id/"
        + mapProjectId + "/concept/isValid " + terminologyId);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER, "is target code valid",
          securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);
      final ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      boolean isValid = algorithmHandler.isTargetCodeValid(terminologyId);

      // not all algorithmHandler return true or false. some default to true
      if (isValid) {
        Concept c = contentService.getConcept(terminologyId, mapProject.getDestinationTerminology(),
            mapProject.getDestinationTerminologyVersion());

        // if ICDO, it is possible that the behavioral code has been changed and
        // the exact
        // targetId will not be a loaded concept from the source
        // as a result, we fudge the concept from a concept sharing its base
        // targetId from
        // ICDO. For example, if the editor enters '9270/6', '9270/1' will be
        // the
        // concept returned with the updated terminologyId '9270/6'. See
        // MAP-1467.
        if (c == null && mapProject.getDestinationTerminology().equals("ICDO")) {
          SearchResultList list = contentService.findConceptsForQuery("terminologyId:"
              + terminologyId.substring(0, 4) + "* AND terminology:ICDO AND terminologyVersion:"
              + mapProject.getDestinationTerminologyVersion(), null);
          SearchResult result = list.getSearchResults().get(0);
          c = new ConceptJpa();
          c.setTerminologyId(terminologyId);
          c.setTerminology(result.getTerminology());
          c.setDefaultPreferredName(result.getValue());
          c.setTerminologyVersion(result.getTerminologyVersion());
          c.setId(result.getId());
        }
        //ICD10CA <-> ICD11 projects also allow a small list of non-terminologyId targets.
        //If isValid is true but c == null, it's one of those situations.
        if (c == null &&
            ((mapProject.getSourceTerminology().equals("ICD10CA") && mapProject.getDestinationTerminology().equals("ICD11"))
                || (mapProject.getSourceTerminology().equals("ICD11") && mapProject.getDestinationTerminology().equals("ICD10CA")))){
          
          c = new ConceptJpa();
          c.setTerminologyId(terminologyId);
          c.setDefaultPreferredName("Target name could not be determined");
        }        
        // Empty descriptions/relationships
        if (c != null) {
          c.setDescriptions(new HashSet<Description>());
          c.setRelationships(new HashSet<Relationship>());
          return c;
        } else {
          // if c is null, the concept was not found
          return null;
        }
      } else {
        return null;
      }

    } catch (Exception e) {
      handleException(e, "trying to determine if target code is valid", user,
          mapProjectId.toString(), terminologyId);
      return null;
    } finally {
      contentService.close();
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * uploadMappingHandbookFile(java.io.InputStream,
   * com.sun.jersey.core.header.FormDataContentDisposition, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/upload/{mapProjectId}")
  // Swagger does not support this
  @ApiOperation(value = "Upload a mapping handbook file for a project",
      notes = "Uploads a mapping handbook file for the specified project.")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces({
      MediaType.TEXT_PLAIN
  })
  public String uploadMappingHandbookFile(@FormDataParam("file") InputStream fileInputStream,
    @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
    @PathParam("mapProjectId") Long mapProjectId, @HeaderParam("Authorization") String authToken)
    throws Exception {

    String user = null;
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD, "upload handbook file",
          securityService);

      // get destination directory for uploaded file
      final Properties config = ConfigUtility.getConfigProperties();
      final String docDir = config.getProperty("map.principle.source.document.dir");

      // make sure docDir ends with /doc - validation
      // mkdirs with project id in both dir and archiveDir
      final File dir = new File(docDir);
      final File archiveDir = new File(docDir + "/archive");

      final File projectDir = new File(docDir, mapProjectId.toString());
      projectDir.mkdir();
      final File archiveProjectDir = new File(archiveDir, mapProjectId.toString());
      archiveProjectDir.mkdir();

      // compose the name of the stored file
      final MapProject mapProject = mappingService.getMapProject(Long.valueOf(mapProjectId));
      final SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd");
      final String date = dt.format(new Date());
      String extension = "";
      if (contentDispositionHeader.getFileName().indexOf(".") != -1) {
        extension = contentDispositionHeader.getFileName()
            .substring(contentDispositionHeader.getFileName().lastIndexOf("."));
      }
      final String fileName = contentDispositionHeader.getFileName()
          .substring(0, contentDispositionHeader.getFileName().lastIndexOf("."))
          .replaceAll(" ", "_");
      final File file = new File(dir, mapProjectId + "/" + fileName + extension);
      final File archiveFile =
          new File(archiveDir, mapProjectId + "/" + fileName + "." + date + extension);

      // save the file to the server
      saveFile(fileInputStream, file.getAbsolutePath());
      copyFile(file, archiveFile);

      // update project
      mapProject.setMapPrincipleSourceDocument(mapProjectId + "/" + fileName + extension);
      updateMapProject((MapProjectJpa) mapProject, authToken);

      return mapProjectId + " " + file.getName();
    } catch (Exception e) {
      handleException(e, "trying to upload a file", user, mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapProjectMetadata(
   * java.lang.String)
   */
  @Override
  @GET
  @Path("/mapProject/metadata")
  @ApiOperation(value = "Get metadata for map projects",
      notes = "Gets the key-value pairs representing all metadata for the map projects.",
      response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getMapProjectMetadata(@ApiParam(value = "Authorization token",
      required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /mapProject/metadata");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user =
          authorizeApp(authToken, MapUserRole.VIEWER, "get map project metadata", securityService);

      // call jpa service and get complex map return type
      final Map<String, Map<String, String>> mapOfMaps = mappingService.getMapProjectMetadata();

      // convert complex map to KeyValuePair objects for easy
      // transformation to
      // XML/JSON
      final KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      for (final Map.Entry<String, Map<String, String>> entry : mapOfMaps.entrySet()) {
        final String metadataType = entry.getKey();
        final Map<String, String> metadataPairs = entry.getValue();
        final KeyValuePairList keyValuePairList = new KeyValuePairList();
        keyValuePairList.setName(metadataType);
        for (final Map.Entry<String, String> pairEntry : metadataPairs.entrySet()) {
          final KeyValuePair keyValuePair =
              new KeyValuePair(pairEntry.getKey().toString(), pairEntry.getValue());
          keyValuePairList.addKeyValuePair(keyValuePair);
        }
        keyValuePairLists.addKeyValuePairList(keyValuePairList);
      }
      return keyValuePairLists;
    } catch (Exception e) {
      handleException(e, "trying to get the map project metadata", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getAllTerminologyNotes(
   * java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/mapProject/{mapProjectId}/notes")
  @ApiOperation(value = "Get metadata for map projects",
      notes = "Gets the key-value pairs representing all metadata for the map projects.",
      response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairList getAllTerminologyNotes(
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /mapProject/" + mapProjectId + "/notes");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user =
          authorizeApp(authToken, MapUserRole.VIEWER, "get all terminology notes", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);
      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      final KeyValuePairList list = new KeyValuePairList();
      for (final Map.Entry<String, String> entry : handler.getAllTerminologyNotes().entrySet()) {
        final KeyValuePair pair = new KeyValuePair(entry.getKey(), entry.getValue());
        list.addKeyValuePair(pair);
      }
      return list;
    } catch (Exception e) {
      handleException(e, "trying to get all terminology notes", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * computeDefaultPreferredNames(java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/names")
  @ApiOperation(value = "Compute default preferred names for a map project.",
      notes = "Recomputes default preferred names for the specified map project.")
  public void computeDefaultPreferredNames(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString() + "/names");

    String user = null;
    String project = "";

    final MappingService mappingService = new MappingServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD, "compute names",
          securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);
      final String terminology = mapProject.getSourceTerminology();
      final String version = mapProject.getSourceTerminologyVersion();
      final Long dpnTypeId = 900000000000003001L;
      final Long dpnrefsetId = 900000000000509007L;
      final Long dpnAcceptabilityId = 900000000000548007L;
      // FROM ComputeDefaultPreferredNameMojo
      Logger.getLogger(getClass()).info("Starting comput default preferred names");
      Logger.getLogger(getClass()).info("  terminology = " + terminology);
      Logger.getLogger(getClass()).info("  terminologyVersion = " + version);

      contentService.setTransactionPerOperation(false);
      contentService.beginTransaction();

      // Setup vars
      int dpnNotFoundCt = 0;
      int dpnFoundCt = 0;
      int dpnSkippedCt = 0;
      int objectCt = 0;

      final ConceptList concepts = contentService.getAllConcepts(terminology, version);
      contentService.clear();

      // Iterate over concepts
      for (final Concept concept2 : concepts.getConcepts()) {
        final Concept concept = contentService.getConcept(concept2.getId());
        // Skip if inactive
        if (!concept.isActive()) {
          dpnSkippedCt++;
          continue;
        }
        Logger.getLogger(getClass()).debug("  Concept " + concept.getTerminologyId());
        boolean dpnFound = false;
        // Iterate over descriptions
        for (Description description : concept.getDescriptions()) {

          // If active andn preferred type
          if (description.isActive() && description.getTypeId().equals(dpnTypeId)) {
            // Iterate over language refset members
            for (LanguageRefSetMember language : description.getLanguageRefSetMembers()) {
              // If prefrred and has correct refset
              if (Long.valueOf(language.getRefSetId()).equals(dpnrefsetId) && language.isActive()
                  && language.getAcceptabilityId().equals(dpnAcceptabilityId)) {
                // print warning for multiple names found
                if (dpnFound) {
                  Logger.getLogger(getClass())
                      .warn("Multiple default preferred names found for concept "
                          + concept.getTerminologyId());
                  Logger.getLogger(getClass())
                      .warn("  " + "Existing: " + concept.getDefaultPreferredName());
                  Logger.getLogger(getClass())
                      .warn("  " + "Replaced with: " + description.getTerm());
                }
                // Set preferred name
                concept.setDefaultPreferredName(description.getTerm());
                // set found to true
                dpnFound = true;
              }
            }
          }
        }

        // Pref name not found
        if (!dpnFound) {
          dpnNotFoundCt++;
          Logger.getLogger(getClass()).warn(
              "Could not find defaultPreferredName for concept " + concept.getTerminologyId());
          concept.setDefaultPreferredName("[Could not be determined]");
        } else {
          dpnFoundCt++;
        }

        // periodically comit
        if (++objectCt % 5000 == 0) {
          Logger.getLogger(getClass()).info("    count = " + objectCt);
          contentService.commit();
          contentService.clear();
          contentService.beginTransaction();
        }
      }

      contentService.commit();
      Logger.getLogger(getClass()).info("  found =  " + dpnFoundCt);
      Logger.getLogger(getClass()).info("  not found = " + dpnNotFoundCt);
      Logger.getLogger(getClass()).info("  skipped = " + dpnSkippedCt);

      Logger.getLogger(getClass()).info("Done...");

      return;
    } catch (Exception e) {
      handleException(e, "trying to compute names", user, project, "");
    } finally {
      contentService.close();
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Save uploaded file to a defined location on the server.
   * 
   * @param uploadedInputStream the uploaded input stream
   * @param serverLocation the server location
   */
  @SuppressWarnings("static-method")
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
  @SuppressWarnings("resource")
  public static void copyFile(File sourceFile, File destFile) throws IOException {
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

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * beginReleaseForMapProject(java.lang.String, java.lang.Long,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/release/{effectiveTime}/begin")
  @ApiOperation(value = "Begin release for map project",
      notes = "Generates release validation report for map project")
  @Produces({
      MediaType.TEXT_PLAIN
  })
  public String beginReleaseForMapProject(
    @ApiParam(value = "Effective Time, e.g. 20170131",
        required = true) @PathParam("effectiveTime") String effectiveTime,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(WorkflowServiceRestImpl.class).info("RESTful call (Mapping): /project/id/"
        + mapProjectId.toString() + "/release/" + effectiveTime + "/begin");

    String user = null;
    String project = "";
    boolean success = false;

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD, "begin release",
          securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // If other processes are already running, return the currently running
      // process information as an Exception
      // If not, obtain the processLock
      try {
        RootServiceJpa
            .lockProcess(user + " is currently running process = Begin Release for map project "
                + mapProject.getName());
      } catch (Exception e) {
        return e.getMessage();
      } finally {
        securityService.close();
      }

      // create release handler in test mode
      ReleaseHandler handler = new ReleaseHandlerJpa(true);
      handler.setEffectiveTime(effectiveTime);
      handler.setMapProject(mapProject);
      handler.beginRelease();
      RootServiceJpa.unlockProcess();
      success = true;
      return "Success";
    } catch (Exception e) {
      RootServiceJpa.unlockProcess();
      handleException(e, "trying to begin release", user, project, "");
      success = false;
      return "Failure";
    } finally {

      String notificationMessage = "";
      if (success) {
        notificationMessage =
            "Hello,\n\nBegin release for the " + project + " project completed.  \n\n";
      } else {
        notificationMessage = "Hello,\n\nBegin release for the " + project
            + " project failed. Please check the log available on the UI and report the problem to an administrator. \n\n";
      }
      sendReleaseNotification(notificationMessage, user);

      mappingService.close();
      securityService.close();
    }
  }

  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/version/{version}")
  @ApiOperation(value = "Update map project destination version",
      notes = "Updates only the map project destination terminology version")
  @Produces({
      MediaType.TEXT_PLAIN
  })
  public String updateMapProjectVersion(
    @ApiParam(value = "Destination version , e.g. 22_9",
        required = true) @PathParam("version") String version,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(WorkflowServiceRestImpl.class).info(
        "RESTful call (Mapping): /project/id/" + mapProjectId.toString() + "/version/" + version);

    String user = null;
    String project = "";

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "update project destination version", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      mapProject.setDestinationTerminologyVersion(version);
      mappingService.updateMapProject(mapProject);

      return version;
    } catch (Exception e) {
      RootServiceJpa.unlockProcess();
      handleException(e, "update project destination version", user, project, "");
      return "Failure";
    } finally {

      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns the release directory path.
   *
   * @param mapProject the map project
   * @param effectiveTime the effective time
   * @return the release directory path
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private String getReleaseDirectoryPath(MapProject mapProject, String effectiveTime)
    throws Exception {
    String rootPath =
        ConfigUtility.getConfigProperties().getProperty("map.principle.source.document.dir");
    if (!rootPath.endsWith("/") && !rootPath.endsWith("\\")) {
      rootPath += "/";
    }

    String path = rootPath + "release/" + mapProject.getSourceTerminology() + "_to_"
        + mapProject.getDestinationTerminology() + "_" + mapProject.getRefSetId() + "/"
        + effectiveTime + "/";
    path.replaceAll("\\s", "");
    return path;

  }

  /* see superclass */
  @Override
  @GET
  @Path("/project/id/{projectId}/releaseFileNames")
  @ApiOperation(value = "Get release file names",
      notes = "Gets a list of release file names from the server.", response = String.class)
  @Produces({
      MediaType.TEXT_PLAIN
  })
  public String getReleaseFileNames(
    @ApiParam(value = "Map project id, e.g. 7",
        required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + mapProjectId + "/releaseFileNames");
    String projectName = "";
    String user = "";

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.ADMINISTRATOR,
          "get scope concepts", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      final String results = mappingService.getReleaseFileNames(mapProject);

      return results;

    } catch (FileNotFoundException e) {
      // If release files don't exist yet, don't throw error to the UI, but DO
      // log it
      Logger.getLogger(MappingServiceRestImpl.class).info(e);
      return null;
    } catch (Exception e) {
      this.handleException(e, "trying to get release file names", user, projectName, "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * processReleaseForMapProject(java.lang.String, java.lang.String,
   * java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/release/{effectiveTime}/module/id/{moduleId}/process")
  @ApiOperation(value = "Process release for map project",
      notes = "Processes release and creates release files for map project")
  @Produces({
      MediaType.TEXT_PLAIN
  })
  public String processReleaseForMapProject(
    @ApiParam(value = "Module Id, e.g. 20170131",
        required = false) @PathParam("moduleId") String moduleId,
    @ApiParam(value = "Effective Time, e.g. 20170131",
        required = true) @PathParam("effectiveTime") String effectiveTime,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Current", required = false) @QueryParam("current") boolean current,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/id/" + mapProjectId.toString() + "/release/"
            + effectiveTime + "/module/id/" + moduleId + "/process " + current);

    String user = null;
    String project = "";
    boolean success = false;

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD, "process release",
          securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // If other processes are already running, return the currently running
      // process information as an Exception
      // If not, obtain the processLock
      try {
        RootServiceJpa
            .lockProcess(user + " is currently running process = Process Release for map project "
                + mapProject.getName());
      } catch (Exception e) {
        return e.getMessage();
      } finally {
        securityService.close();
      }

      if (moduleId.equals("null") || moduleId.equals("")) {
        moduleId = mapProject.getModuleId();
        if (moduleId == null || moduleId.equals("")) {
          throw new Exception("The module id must be set on the project details page.");
        }
      }

      // create release handler in test mode
      ReleaseHandler handler = new ReleaseHandlerJpa(true);
      if (current) {
        final SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd_hhmm");
        effectiveTime = dt.format(new Date());
      }
      handler.setEffectiveTime(effectiveTime);
      handler.setMapProject(mapProject);
      handler.setModuleId(moduleId);
      handler.setWriteSnapshot(true);
      handler.setWriteActiveSnapshot(current ? false : true);
      handler.setWriteDelta(current ? false : true);

      // compute output directory
      // NOTE: Use same directory as map principles for access via webapp
      String outputDir = "";
      // if processing current/interim release for sake of file comparison,
      // put in a directory called current
      if (current) {
        outputDir = this.getReleaseDirectoryPath(mapProject, "current/" + effectiveTime);
      } else {
        outputDir = this.getReleaseDirectoryPath(mapProject, effectiveTime);
      }
      handler.setOutputDir(outputDir);

      File file = new File(outputDir);
      Logger.getLogger(MappingServiceRestImpl.class).info("  exists: " + file.exists());
      // make output directory if does not exist
      if (!file.exists()) {
        Logger.getLogger(MappingServiceRestImpl.class)
            .info("  making directory: " + file.getAbsolutePath());
        file.mkdirs();
      }

      // process release
      handler.processRelease();

      RootServiceJpa.unlockProcess();
      success = true;
      return "Success";
    } catch (Exception e) {
      RootServiceJpa.unlockProcess();
      handleException(e, "trying to process release", user, project, "");
      success = false;
      return "Failure";
    } finally {

      String notificationMessage = "";
      if (success) {
        notificationMessage =
            "Hello,\n\nProcess release for the " + project + " project completed.  \n\n";
      } else {
        notificationMessage = "Hello,\n\nProcess release for the " + project
            + " project failed. Please check the log available on the UI and report the problem to an administrator. \n\n";
      }
      sendReleaseNotification(notificationMessage, user);

      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * finishReleaseForMapProject(boolean, java.lang.Long, java.lang.String,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/release/{effectiveTime}/finish")
  @ApiOperation(value = "Finish release for map project",
      notes = "Finishes release for map project from release files")
  @Produces({
      MediaType.TEXT_PLAIN
  })
  public String finishReleaseForMapProject(
    @ApiParam(value = "Preview mode", required = false) @QueryParam("test") boolean testModeFlag,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Effective Time, e.g. 20170131",
        required = true) @PathParam("effectiveTime") String effectiveTime,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/id/" + mapProjectId.toString() + "/release/finish");

    String user = null;
    String project = "";
    boolean success = false;

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD, "compute names",
          securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // If other processes are already running, return the currently running
      // process information as an Exception
      // If not, obtain the processLock
      try {
        if (testModeFlag) {
          RootServiceJpa.lockProcess(user
              + " is currently running process = Create Release Finalization QA Report for map project "
              + mapProject.getName());
        } else {
          RootServiceJpa.lockProcess(
              user + " is currently running process = Finishing Release for map project "
                  + mapProject.getName());
        }
      } catch (Exception e) {
        return e.getMessage();
      } finally {
        securityService.close();
      }

      // create release handler NOT in test mode
      // TODO Decide whether to remove the out of scope map records,
      // currently thinking NO
      // but we do want to replace the simple/complex/extended maps
      ReleaseHandler handler = new ReleaseHandlerJpa(testModeFlag);
      handler.setMapProject(mapProject);
      handler.setEffectiveTime(effectiveTime);
      String releaseDirPath = this.getReleaseDirectoryPath(mapProject, effectiveTime);

      // check for existence of file
      // TODO This computation should be moved into the ReleaseHandler and
      // made handler-specific
      String relPath = "";
      if (mapProject.getSourceTerminology().equals("SNOMEDCT_US")) {
        relPath =
            "/der2_" + handler.getPatternForType(mapProject) + mapProject.getMapRefsetPattern()
                + "ActiveSnapshot_US1000124_" + effectiveTime + ".txt";
      } else {
        relPath = "/der2_" + handler.getPatternForType(mapProject)
            + mapProject.getMapRefsetPattern() + "ActiveSnapshot_INT_" + effectiveTime + ".txt";
      }
      String mapFilePath = releaseDirPath + relPath;

      File file = new File(mapFilePath);
      if (!file.exists()) {
        throw new LocalException("Release file " + mapFilePath + " not found");
      }
      handler.setInputFile(mapFilePath);

      // process release
      handler.finishRelease();

      RootServiceJpa.unlockProcess();
      success = true;
      return "Success";
    } catch (Exception e) {
      RootServiceJpa.unlockProcess();
      handleException(e, "trying to finish release", user, project, "");
      success = false;
      return "Failure";
    } finally {

      String notificationMessage = "";
      if (success) {
        notificationMessage = "Hello,\n\n" + (testModeFlag ? "Preview f" : "F")
            + "inish release for the " + project + " project completed.  \n\n";
      } else {
        notificationMessage = "Hello,\n\n" + (testModeFlag ? "Preview f" : "F")
            + "inish release for the " + project
            + " project failed. Please check the log available on the UI and report the problem to an administrator. \n\n";
      }
      sendReleaseNotification(notificationMessage, user);

      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * startEditingCycleForMapProject(java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/release/startEditing")
  @ApiOperation(value = "Start editing cycle for map project",
      notes = "Start editing cycle for map project")
  public void startEditingCycleForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(WorkflowServiceRestImpl.class).info(
        "RESTful call (Mapping): /project/id/" + mapProjectId.toString() + "/release/startEditing");

    String user = null;
    String project = "";

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD, "start editing cycle",
          securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // Begin the release
      BeginEditingCycleHandler handler = new BeginEditingCycleHandlerJpa();

      handler.setMapProject(mapProject);
      handler.beginEditingCycle();

    } catch (Exception e) {
      handleException(e, "trying to start editing cycle", user, project, "");
    } finally {

      mappingService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#createJiraIssue(java.
   * lang.String, java.lang.String, java.lang.String,
   * org.ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/jira/{conceptId}/{conceptAuthor}")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Create a jira ticket for the content author.",
      notes = "Create a jira ticket for the content author.")
  public void createJiraIssue(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Concept author username",
        required = true) @PathParam("conceptAuthor") String conceptAuthor,
    @ApiParam(value = "Message text",
        required = false) @QueryParam("messageText") String messageText,
    @ApiParam(value = "Map record, in JSON or XML POST data",
        required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Mapping): /jira/" + conceptId.toString() + "/" + conceptAuthor);
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Mapping): /jira/" + messageText);
    try {

      // get URL and authentication for JIRA api access
      final Properties config = ConfigUtility.getConfigProperties();
      final String jiraAuthHeader = config.getProperty("jira.authHeader");
      final String jiraUrl = config.getProperty("jira.defaultUrl");
      String jiraProject = config.getProperty("jira.project");

      if (jiraAuthHeader == null || jiraUrl == null || jiraProject == null) {
        this.handleException(
            new Exception("create a JIRA issue. JIRA properties must be in configuration file"),
            "create a JIRA issue . JIRA properties must be in configuration file", "", "", "");
      }

      final Client client = ClientBuilder.newClient();
      final WebTarget target = client.target(jiraUrl + "/issue/");

      // buffer map record contents
      StringBuffer mapRecordContents = new StringBuffer();
      mapRecordContents.append("Concept:").append(mapRecord.getConceptId()).append(" ")
          .append(mapRecord.getConceptName()).append("\\\\\\\\");
      mapRecordContents.append("Map Entries").append("\\\\\\\\");
      final Comparator<MapEntry> entriesComparator = new Comparator<MapEntry>() {

        /* see superclass */
        @Override
        public int compare(MapEntry o1, MapEntry o2) {
          Integer mapGroup1 = Integer.valueOf(o1.getMapGroup());
          Integer mapGroup2 = Integer.valueOf(o2.getMapGroup());
          if (mapGroup1 == mapGroup2) {
            Integer mapPriority1 = Integer.valueOf(o1.getMapPriority());
            Integer mapPriority2 = Integer.valueOf(o2.getMapPriority());
            return mapPriority1.compareTo(mapPriority2);
          }
          return mapGroup1.compareTo(mapGroup2);
        }
      };

      // sort the map entries
      Collections.sort(mapRecord.getMapEntries(), entriesComparator);
      for (MapEntry entry : mapRecord.getMapEntries()) {
        mapRecordContents.append(entry.getMapGroup() + " / " + entry.getMapPriority()).append("  ");
        mapRecordContents.append(entry.getTargetId()).append(" ").append(entry.getTargetName())
            .append("\\\\\\\\");
        mapRecordContents.append(entry.getRule()).append("\\\\\\\\");
        for (MapAdvice mapAdvice : entry.getMapAdvices()) {
          mapRecordContents.append(mapAdvice.getName()).append("\\\\\\\\");
        }
        mapRecordContents.append(entry.getMapRelation().getName()).append("\\\\\\\\");
      }
      /*
       * if (mapRecord.getMapNotes().size() > 0) {
       * mapRecordContents.append("Notes").append("\\\\\\\\"); } for (MapNote
       * note : mapRecord.getMapNotes()) {
       * mapRecordContents.append(note.getUser().getName()).append(" on ").
       * append(note.getTimestamp()).append("\\\\\\\\");
       * mapRecordContents.append(note.getNote().replaceAll("<br>",
       * "\\\\\\\\\\\\\\\\").replaceAll("\\<.*?\\>", "").replaceAll("nbsp;", "
       * ")).append("\\\\\\\\"); }
       */

      // if test project, override author and user
      if (!config.getProperty("deploy.title").equals("Mapping Tool")) {
        conceptAuthor = "dshapiro";
        authToken = "dshapiro";
        jiraProject = "MTFP";
      }

      // create the issue object to send to JIRA Rest API
      String data = "{" + "\"fields\": {" + "\"project\":" + "{" + "\"key\": \"" + jiraProject
          + "\"" + "}," + "\"summary\": \"Mapping Feedback on " + conceptId + "\","
          + "\"assignee\": {" + "\"name\": \"" + conceptAuthor + "\"" + "}," + "\"reporter\": {"
          + "\"name\": \"" + authToken + "\"" + "}," + "\"description\": \""
          + messageText.replaceAll("\n", "\\\\\\\\\\\\\\\\").replaceAll("\\<.*?\\>", "")
          + "\\\\\\\\" + mapRecordContents.toString() + "\"," + "\"issuetype\": {"
          + "\"name\": \"Task\"" + "}" + "}" + "}";
      Logger.getLogger(MappingServiceRestImpl.class)
          .info("RESTful call (Mapping): /jira/  \n" + data);

      final Response response =
          target.request(MediaType.APPLICATION_JSON_TYPE).header("Authorization", jiraAuthHeader)
              .accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(data));
      int statusCode = response.getStatus();

      if (statusCode == 401) {
        this.handleException(new AuthenticationException("Invalid Username or Password"),
            "Invalid Username or Password", authToken, "", "");
      } else if (statusCode == 403) {
        this.handleException(new AuthenticationException("Forbidden"), "Forbidden", authToken, "",
            "");
      } else if (statusCode == 200 || statusCode == 201) {
        Logger.getLogger(MappingServiceRestImpl.class).info("Ticket Created successfully");
      } else {
        this.handleException(new AuthenticationException("Http Error : " + statusCode),
            "Http Error : " + statusCode, authToken, "", "");
        Logger.getLogger(MappingServiceRestImpl.class).info("Http Error : " + statusCode);
      }

    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /* see superclass */
  @POST
  @Path("/log/{projectId}")
  @Produces("text/plain")
  @ApiOperation(value = "Get log(s)", notes = "Gets log(s) for specified project and log type(s).",
      response = String.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Override
  public String getLog(
    @ApiParam(value = "Project id", required = true) @PathParam("projectId") String projectId,
    @ApiParam(value = "Logs requested", required = true) List<String> logTypes,
    @ApiParam(value = "Query, e.g. UPDATE", required = false) @QueryParam("query") String query,
    @ApiParam(value = "Authorization token, e.g. 'author1'",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Mapping):  /log/" + projectId + "/");

    final MappingService mappingService = new MappingServiceJpa();
    String line = null;
    StringBuffer log = new StringBuffer();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "get log", securityService);

      final MapProject mapProject =
          mappingService.getMapProject(Long.valueOf(projectId).longValue());

      // look for logs either in the project log dir, second in the
      // remover/loader log dir
      String rootPath =
          ConfigUtility.getConfigProperties().getProperty("map.principle.source.document.dir");
      if (!rootPath.endsWith("/") && !rootPath.endsWith("\\")) {
        rootPath += "/";
      }
      File logFile = null;
      File logDir = null;
      if (logTypes.get(0).toString().contains("Terminology")
          || logTypes.get(0).toString().contains("PreviousMembers")) {
        logDir = new File(rootPath + "logs");
      } else {
        logDir = new File(rootPath + mapProject.getId() + "/logs");
      }

      ArrayList<String> modifiedLogTypes = new ArrayList<>();

      // get all of the previous members logs for the given source terminology
      // if load/removePreviousMembers is called
      for (String logType : logTypes) {
        if (logTypes.get(0).toString().contains("PreviousMembers")) {
          ArrayList<MapProject> mapProjectsForSourceTerminology = new ArrayList<>();

          // Also don't include retired or pilot projects
          ArrayList<String> excludeProjects =
              new ArrayList<>(Arrays.asList("SNOMED to ICD9CM", "SNOMED to ICD11 Pilot"));

          // convert to log titles containing refset ids
          for (MapProject project : mappingService.getMapProjects().getMapProjects()) {
            if (project.getSourceTerminology().equals(mapProject.getSourceTerminology())
                && !project.getRefSetId().startsWith("P")
                && !excludeProjects.contains(project.getName())) {
              mapProjectsForSourceTerminology.add(project);
              modifiedLogTypes
                  .add(logType.replace("PreviousMembers", "_maps_" + project.getRefSetId()));

            }
          }
        }
      }
      // if there were renamed load/removePreviousMembers logs
      if (modifiedLogTypes.size() > 0) {
        logTypes = modifiedLogTypes;
      }

      // for each logType, get the logFile and append to the log
      for (String logType : logTypes) {
        if (logType.contains("Terminology")) {
          logFile = new File(logDir,
              logType.replace("Terminology", "_" + mapProject.getSourceTerminology()) + ".log");
        } else {
          logFile = new File(logDir, logType + ".log");
        }
        if (!logFile.exists()) {
          final Properties config = ConfigUtility.getConfigProperties();
          final String removerLoaderLogDir =
              config.getProperty("map.principle.source.document.dir") + "/logs";
          logFile = new File(removerLoaderLogDir,
              logType.replace("Terminology", mapProject.getSourceTerminology()) + ".log");
          if (!logFile.exists()) {
            log.append("\nA log for " + logType + " is not yet available on this server.")
                .append("\n");
            log.append("A log will be created when the process is run.").append("\n");
            continue;
          }
        }

        String logFilePath = logFile.getAbsolutePath();
        BufferedReader logReader =
            new BufferedReader(new InputStreamReader(new FileInputStream(logFilePath), "UTF-8"));
        while ((line = logReader.readLine()) != null) {
          // if filter is set
          if (query != null) {
            // if line contains filter search term, keep line
            if (line.contains(query)) {
              log.append(line).append("\n");
              // otherwise don't add line to log
            } else {
              continue;
            }
            // no filter set
          } else {
            log.append(line).append("\n");
          }
        }
        log.append("\n");
        logReader.close();
      }

      return log.toString();

    } catch (Exception e) {
      handleException(e, "trying to get log");
    } finally {
      mappingService.close();
      securityService.close();
    }
    return null;
  }

  // /**
  // * Reads an InputStream and returns its contents as a String. Also effects
  // * rate control.
  // * @param inputStream The InputStream to read from.
  // * @return The contents of the InputStream as a String.
  // * @throws Exception on error.
  // */
  // private static String inputStreamToString(final InputStream inputStream)
  // throws Exception {
  // final StringBuilder outputBuilder = new StringBuilder();
  //
  // String string;
  // if (inputStream != null) {
  // BufferedReader reader =
  // new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
  // while (null != (string = reader.readLine())) {
  // outputBuilder.append(string).append('\n');
  // }
  // }
  //
  // return outputBuilder.toString();
  // }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getConceptAuthors(java.
   * lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/authors/{conceptId}")
  @ApiOperation(value = "Gets authors for this concept",
      notes = "Gets a list of all content authors from the authoring tool.",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getConceptAuthors(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /authors/" + conceptId);

    String user = null;
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "gets authors for this concept",
          securityService);

      final Properties config = ConfigUtility.getConfigProperties();
      final String authoringAuthHeader = config.getProperty("authoring.authHeader");
      final String authoringUrl = config.getProperty("authoring.defaultUrl");

      if (authoringAuthHeader == null || authoringUrl == null) {
        this.handleException(
            new Exception(
                "retrieve concept authors. Authoring properties must be in configuration file"),
            "retrieve concept authors. Authoring properties must be in configuration file", "", "",
            "");
      }

      final Client client = ClientBuilder.newClient();
      final WebTarget target =
          client.target(authoringUrl + "/authoring-traceability-service/activities?conceptId=" + conceptId);

      final Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
          .header("Authorization", authoringAuthHeader).accept("application/json").get();
      int statusCode = response.getStatus();

      if (statusCode == 401) {
        this.handleException(new AuthenticationException("Invalid Username or Password"),
            "Invalid Username or Password", authToken, "", "");
      } else if (statusCode == 403) {
        this.handleException(new AuthenticationException("Forbidden"), "Forbidden", authToken, "",
            "");
      } else if (statusCode == 200 || statusCode == 201) {
        Logger.getLogger(MappingServiceRestImpl.class)
            .info("Traceability report retrieved successfully");
      } else {
        this.handleException(new AuthenticationException("Http Error : " + statusCode),
            "Http Error : " + statusCode, authToken, "", "");
        Logger.getLogger(MappingServiceRestImpl.class).info("Http Error : " + statusCode);
      }
      // Parse to get the authors on all changes that were promoted to MAIN
      String jsonText = response.readEntity(String.class);
      JSONObject jsonObject = new JSONObject(jsonText);
      JSONArray array = jsonObject.getJSONArray("content");
      SearchResultList searchResultList = new SearchResultListJpa();
      List<String> userNameList = new ArrayList<>();
      for (int i = 0; i < array.length(); i++) {
        JSONObject singleContent = array.getJSONObject(i);
        if (singleContent.getString("highestPromotedBranch") == null
            || !singleContent.getString("highestPromotedBranch").contains("MAIN")) {
          continue;
        }
        String userName = singleContent.getString("username");
        if (!userNameList.contains(userName)) {
          userNameList.add(userName);
        }
      }
      for (String userName : userNameList) {
        SearchResult searchResult = new SearchResultJpa();
        searchResult.setValue(userName);
        searchResultList.addSearchResult(searchResult);
      }
      searchResultList.setTotalCount(userNameList.size());
      return searchResultList;
    } catch (Exception e) {
      this.handleException(e, "trying to get authors for this concept", user, "", "");
      return null;
    } finally {
      securityService.close();
    }
  }

  /**
   * Returns the concept authoring changes.
   *
   * @param projectId the project id
   * @param conceptId the concept id
   * @param authToken the auth token
   * @return the concept authoring changes
   * @throws Exception the exception
   */
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#
   * getConceptAuthoringChanges(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  @GET
  @Path("/changes/{projectId}/{conceptId}")
  @ApiOperation(value = "Gets authoring history for this concept",
      notes = "Gets a list of all editing changes made to MAIN from the authoring tool.",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getConceptAuthoringChanges(
    @ApiParam(value = "Project id", required = true) @PathParam("projectId") String projectId,
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /changes/" + projectId + "/" + conceptId);

    final MappingService mappingService = new MappingServiceJpa();
    String user = null;
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get concept authoring changes",
          securityService);

      final MapProject mapProject =
          mappingService.getMapProject(Long.valueOf(projectId).longValue());

      final Date editingCycleBeginDate = mapProject.getEditingCycleBeginDate();
      Logger.getLogger(MappingServiceRestImpl.class)
          .info("editingCycleBeginDate:" + editingCycleBeginDate.toString());

      final Properties config = ConfigUtility.getConfigProperties();
      final String authoringAuthHeader = config.getProperty("authoring.authHeader");
      final String authoringUrl = config.getProperty("authoring.defaultUrl");

      if (authoringAuthHeader == null || authoringUrl == null) {
        this.handleException(
            new Exception(
                "retrieve authoring history. Authoring properties must be in configuration file"),
            "retrieve authoring history. Authoring properties must be in configuration file", "",
            "", "");
      }

      final Client client = ClientBuilder.newClient();
      final WebTarget target =
          client.target(authoringUrl + "/authoring-traceability-service/activities?conceptId=" + conceptId);

      final Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
          .header("Cookie", ConfigUtility.getGenericUserCookie())
          .accept(MediaType.APPLICATION_JSON_TYPE).get();

      int statusCode = response.getStatus();

      if (statusCode == 401) {
        throw new AuthenticationException("Invalid Username or Password");
      } else if (statusCode == 403) {
        throw new AuthenticationException("Forbidden");
      } else if (statusCode == 200 || statusCode == 201) {
        Logger.getLogger(MappingServiceRestImpl.class)
            .info("Traceability report retrieved successfully");
      } else {
        Logger.getLogger(MappingServiceRestImpl.class).info("Http Error : " + statusCode);
      }

      // Parse to get the editing changes that were promoted to MAIN
      SearchResultList searchResultList = new SearchResultListJpa();
      String jsonText = response.readEntity(String.class);
      JSONObject jsonObject = new JSONObject(jsonText);
      JSONArray array = jsonObject.getJSONArray("content");
      for (int i = 0; i < array.length(); i++) {
        JSONObject singleContent = array.getJSONObject(i);
        if (singleContent.getString("highestPromotedBranch") == null
            || !singleContent.getString("highestPromotedBranch").contains("MAIN")) {
          continue;
        }
        String userName = singleContent.getString("username");
        String commitDateString = singleContent.getString("commitDate");
        final SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
        Date commitDate = dt.parse(commitDateString);

        // don't include entries that occurred prior to one month before
        // the current editing cycle begin date
        Calendar c = Calendar.getInstance();
        c.setTime(editingCycleBeginDate);
        c.add(Calendar.MONTH, -1);
        Date historyWindow = c.getTime();
        if (commitDate.before(historyWindow)) {
          continue;
        }
        JSONArray conceptChangesArray = singleContent.getJSONArray("conceptChanges");
        for (int j = 0; j < conceptChangesArray.length(); j++) {
          JSONObject conceptChange = conceptChangesArray.getJSONObject(j);
          String cptId = conceptChange.getString("conceptId");
          JSONArray componentChangesArray = conceptChange.getJSONArray("componentChanges");
          for (int k = 0; k < componentChangesArray.length(); k++) {
            JSONObject componentChange = componentChangesArray.getJSONObject(k);
            String componentId = componentChange.getString("componentId");
            String componentType = componentChange.getString("componentType");
            String componentSubType = "";
            try {
              componentSubType = componentChange.getString("componentSubType");
            } catch (Exception e) {
              // do nothing
            }
            String changeType = componentChange.getString("changeType");
            SearchResult searchResult = new SearchResultJpa();
            searchResult.setValue(userName + ":" + commitDateString);
            searchResult.setValue2(cptId + ":" + componentId + ":" + componentType + ":"
                + componentSubType + ":" + changeType);
            searchResultList.addSearchResult(searchResult);
          }
        }
      }
      searchResultList.setTotalCount(searchResultList.getSearchResults().size());
      Logger.getLogger(MappingServiceRestImpl.class)
          .info("Traceability report contains " + searchResultList.getTotalCount() + " entries.");
      return searchResultList;

    } catch (Exception e) {
      this.handleException(e, "trying to get concept authoring changes", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  @POST
  @Path("/compare/files/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Compares two map files",
      notes = "Compares two files and saves the comparison report to the file system.",
      response = InputStream.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces("application/vnd.ms-excel")
  public InputStream compareMapFiles(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "File paths, in JSON or XML POST data", required = true) List<String> files,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info("RESTful call (Mapping): /compare/files"
        + mapProjectId + " " + (files != null ? files.get(0) + " " + files.get(1) : ""));

    String user = "";
    final MetadataService metadataService = new MetadataServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize
      user =
          authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "compare map files", securityService);

      MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // This can be used to run hardcoded files local to the machine for
      // testing
      // return callTestCompare(mapProject);

      String olderInputFile1 = files.get(0);
      String newerInputFile2 = files.get(1);

      AmazonS3 s3Client = null;
      s3Client = AmazonS3ServiceJpa.connectToAmazonS3();

      // Process first file
      // open first file either from aws or current release on file system
      S3Object file1 = null;
      InputStream objectData1 = null;

      // first check if file1 is a 'current' file on local file system
      // get date on file1
      Pattern datePattern = Pattern.compile("\\d{8}_\\d{4}");
      Matcher m = datePattern.matcher(olderInputFile1);
      String fileDate = "";
      if (m.find()) {
        fileDate = m.group(0);
      }
      File currentDir = new File(this.getReleaseDirectoryPath(mapProject, "current/" + fileDate));
      File currentReleaseFile = new File(currentDir, olderInputFile1);

      // if it is a current local file, read it in
      if (currentReleaseFile.exists()) {
        objectData1 = new FileInputStream(currentReleaseFile);

        // if it is not a current local file, stream file1 from aws
      } else {
        file1 = s3Client
            .getObject(new GetObjectRequest("release-ihtsdo-prod-published", olderInputFile1));
        objectData1 = file1.getObjectContent();
      }

      // Process second file
      // open second file either from aws or current release on file system
      S3Object file2 = null;
      InputStream objectData2 = null;
      List<String> notes = new ArrayList<>();

      // first check if file2 is a 'current' file on local file system
      // get date on file2
      m = datePattern.matcher(newerInputFile2);
      if (m.find()) {
        fileDate = m.group(0);
      }
      currentDir = new File(this.getReleaseDirectoryPath(mapProject, "current/" + fileDate));
      currentReleaseFile = new File(currentDir, newerInputFile2);

      // if it is a current local file, read it in
      if (currentReleaseFile.exists()) {
        objectData2 = new FileInputStream(currentReleaseFile);

        // handle special ICDO case
        if (mapProject.getDestinationTerminology().equals("ICDO")) {
          notes.add(
              "NOTE: ICDO current release file only contains morphology records, so that should be taken into account when interpreting report results.");
        }

        // if it is not a current local file, stream file2 from aws
      } else {
        file2 = s3Client
            .getObject(new GetObjectRequest("release-ihtsdo-prod-published", newerInputFile2));
        objectData2 = file2.getObjectContent();
      }

      InputStream reportInputStream = null;
      StringBuffer reportName = new StringBuffer();
      if (olderInputFile1.contains("Full") || newerInputFile2.contains("Full")) {
        throw new LocalException("Full files cannot be compared with this tool.");
      }

      // compare extended map files and compose report name
      if (olderInputFile1.contains("ExtendedMap") && newerInputFile2.contains("ExtendedMap")) {
        reportInputStream =
            compareExtendedMapFiles(objectData1, objectData2, mapProject, files, notes);
        reportName.append(olderInputFile1.substring(olderInputFile1.lastIndexOf("Extended"),
            olderInputFile1.lastIndexOf('.')));
        if (olderInputFile1.toLowerCase().contains("alpha")) {
          reportName.append("_ALPHA");
        }
        if (olderInputFile1.toLowerCase().contains("beta")) {
          reportName.append("_BETA");
        }
        reportName.append("_");
        reportName.append(newerInputFile2.substring(newerInputFile2.lastIndexOf("Extended"),
            newerInputFile2.lastIndexOf('.')));
        if (newerInputFile2.toLowerCase().contains("alpha")) {
          reportName.append("_ALPHA");
        }
        if (newerInputFile2.toLowerCase().contains("beta")) {
          reportName.append("_BETA");
        }
        reportName.append(".xls");

        // compare simple map files and compose report name
      } else if (olderInputFile1.contains("SimpleMap") && newerInputFile2.contains("SimpleMap")) {
        reportInputStream =
            compareSimpleMapFiles(objectData1, objectData2, mapProject, files, notes);
        reportName.append(olderInputFile1.substring(olderInputFile1.lastIndexOf("Simple"),
            olderInputFile1.lastIndexOf('.')));
        if (olderInputFile1.toLowerCase().contains("alpha")) {
          reportName.append("_ALPHA");
        }
        if (olderInputFile1.toLowerCase().contains("beta")) {
          reportName.append("_BETA");
        }
        reportName.append("_");
        reportName.append(newerInputFile2.substring(newerInputFile2.lastIndexOf("Simple"),
            newerInputFile2.lastIndexOf('.')));
        if (newerInputFile2.toLowerCase().contains("alpha")) {
          reportName.append("_ALPHA");
        }
        if (newerInputFile2.toLowerCase().contains("beta")) {
          reportName.append("_BETA");
        }
        reportName.append(".xls");
      }

      // create destination directory for saved report
      final Properties config = ConfigUtility.getConfigProperties();
      final String docDir = config.getProperty("map.principle.source.document.dir");

      final File projectDir = new File(docDir, mapProjectId.toString());
      if (!projectDir.exists()) {
        projectDir.mkdir();
      }

      final File reportsDir = new File(projectDir, "reports");
      if (!reportsDir.exists()) {
        reportsDir.mkdir();
      }

      final File file = new File(reportsDir, reportName.toString());

      // save the file to the server
      saveFile(reportInputStream, file.getAbsolutePath());

      objectData1.close();
      objectData2.close();

      return reportInputStream;

    } catch (Exception e) {
      handleException(e, "trying to compare map files", user, "", "");
      return null;
    } finally {
      metadataService.close();
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Compare extended map files.
   *
   * @param data1 the data 1
   * @param data2 the data 2
   * @param mapProject the map project
   * @param files the files
   * @param notes the notes
   * @return the input stream
   * @throws Exception the exception
   */
  private InputStream compareExtendedMapFiles(InputStream data1, InputStream data2,
    MapProject mapProject, List<String> files, List<String> notes) throws Exception {

    // map to list of records that have been updated (sorted by key)
    List<String> updatedList = new ArrayList<>();
    // map to list of records that are new in the second file
    Set<String> newList = new HashSet<>();
    // map to list of records that have been inactivated in the second file
    Set<String> inactivatedList = new HashSet<>();
    // map to list of records that have been removed in the second file
    Set<String> removedList = new HashSet<>();
    String line1, line2;

    BufferedReader in1 = new BufferedReader(new InputStreamReader(data1, "UTF-8"));
    in1.mark(100000000);
    BufferedReader in2 = new BufferedReader(new InputStreamReader(data2, "UTF-8"));

    int noChangeCount = 0;
    Map<String, Set<ExtendedLine>> key1Map = new HashMap<>();
    Map<String, Set<ExtendedLine>> key2Map = new HashMap<>();

    int i = 0;
    // populate key1Map with key1 and lineData (no UUID)
    while ((line1 = in1.readLine()) != null) {

      String tokens1[] = line1.split("\t");
      if (!tokens1[4].equals(mapProject.getRefSetId())) {
        continue;
      }
      i++;
      // if refCompId already in map, just add new effectiveTime/TargetId&Active
      if (key1Map.containsKey(tokens1[5] + ":" + tokens1[6] + ":" + tokens1[7])) {
        key1Map.get(tokens1[5] + ":" + tokens1[6] + ":" + tokens1[7]).add(new ExtendedLine(line1));
      } else {
        Set<ExtendedLine> setOfLines = new HashSet<>();
        setOfLines.add(new ExtendedLine(line1));
        key1Map.put(tokens1[5] + ":" + tokens1[6] + ":" + tokens1[7], setOfLines);
      }
    }
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("key1Map count: " + key1Map.size() + " " + i);

    i = 0;
    // go through second file, cache, and figure out what is new and what has
    // changed
    while ((line2 = in2.readLine()) != null) {
      String tokens2[] = line2.split("\t");
      if (!tokens2[4].equals(mapProject.getRefSetId())) {
        continue;
      }
      i++;
      // populate key2Map with key2 and lineData (no UUID)
      if (key2Map.containsKey(tokens2[5] + ":" + tokens2[6] + ":" + tokens2[7])) {
        key2Map.get(tokens2[5] + ":" + tokens2[6] + ":" + tokens2[7]).add(new ExtendedLine(line2));
      } else {
        Set<ExtendedLine> setOfLines = new HashSet<>();
        setOfLines.add(new ExtendedLine(line2));
        key2Map.put(tokens2[5] + ":" + tokens2[6] + ":" + tokens2[7], setOfLines);
      }

      // if key1Map has key2, this record is not new - either it hasn't changed
      // or it has been updated
      if (key1Map.containsKey(tokens2[5] + ":" + tokens2[6] + ":" + tokens2[7])) {
        Set<ExtendedLine> entries = key1Map.get(tokens2[5] + ":" + tokens2[6] + ":" + tokens2[7]);
        // effectiveTime, target, and active all static?
        boolean noChange = false;
        boolean inactivated = false;
        for (ExtendedLine lineData : entries) {
          if (lineData.getTargetId().equals(tokens2[10])
              && tokens2[2].equals(lineData.isActive() ? "1" : "0")
              && tokens2[3].equals(lineData.getModuleId())
              && tokens2[4].equals(lineData.getRefsetId())
              && tokens2[6].equals(lineData.getMapGroup())
              && tokens2[7].equals(lineData.getMapPriority())
              && tokens2[8].equals(lineData.getMapRule())
              && tokens2[9].equals(lineData.getMapAdvice())
              && tokens2[11].equals(lineData.getCorrelationId())
              && lineData.getEffectiveTime().equals(tokens2[1])) {
            noChange = true;
            noChangeCount++;
            break;
          }
        }

        if (!noChange) {

          // inactivated?, check if active is the only thing that isn't equal
          for (ExtendedLine lineData : entries) {
            if (lineData.getTargetId().equals(tokens2[10])
                && lineData.isActive() != Boolean.valueOf(tokens2[2])
                && tokens2[3].equals(lineData.getModuleId())
                && tokens2[4].equals(lineData.getRefsetId())
                && tokens2[6].equals(lineData.getMapGroup())
                && tokens2[7].equals(lineData.getMapPriority())
                && tokens2[8].equals(lineData.getMapRule())
                && tokens2[9].equals(lineData.getMapAdvice())
                && tokens2[11].equals(lineData.getCorrelationId())
            /* && lineData.getEffectiveTime().equals(tokens2[1]) */) {
              inactivatedList.add(line2);
              inactivated = true;
              break;
            }
          }

          // not inactivated, updated in some other way
          if (!inactivated) {
            for (ExtendedLine lineData : entries) {
              if (lineData.isActive() && tokens2[2].equals("1")) {
                // something updated
                // only add to list if records are active
                String line1Sub =
                    "\t" + lineData.getEffectiveTime() + "\t" + (lineData.isActive() ? "1" : "0")
                        + "\t" + lineData.getModuleId() + "\t" + lineData.getRefsetId() + "\t"
                        + lineData.getRefCompId() + "\t" + lineData.getMapGroup() + "\t"
                        + lineData.getMapPriority() + "\t" + lineData.getMapRule() + "\t"
                        + lineData.getMapAdvice() + "\t" + lineData.getTargetId() + "\t"
                        + lineData.getCorrelationId() + "\t" + lineData.getMapCategoryId();
                String line2Sub = line2.substring(line2.indexOf("\t"));
                updatedList.add(line1Sub + "\t" + line2Sub);
                continue;
              }
            }
          }
        }
        // key2 was not in first file, this is new
      } else {
        newList.add(line2);
      }
    }
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("key2Map count: " + key2Map.size() + " " + i);
    in1.reset();

    // determine records that were removed
    while ((line1 = in1.readLine()) != null) {
      String tokens1[] = line1.split("\t");
      if (!tokens1[4].equals(mapProject.getRefSetId())) {
        continue;
      }
      if (!key2Map.containsKey(tokens1[5] + ":" + tokens1[6] + ":" + tokens1[7])) {
        removedList.add(line1);
      }
    }

    in1.close();
    in2.close();

    // log statements
    Logger.getLogger(MappingServiceRest.class).info("new List count:" + newList.size());
    Logger.getLogger(MappingServiceRest.class)
        .info("inactivated List count:" + inactivatedList.size());
    Logger.getLogger(MappingServiceRest.class).info("updated List count:" + updatedList.size());
    Logger.getLogger(MappingServiceRest.class).info("removed List count:" + removedList.size());
    Logger.getLogger(MappingServiceRest.class).info("no change count:" + noChangeCount);

    // produce Excel report file
    final ExportReportHandler handler = new ExportReportHandler();
    return handler.exportExtendedFileComparisonReport(updatedList, newList, inactivatedList,
        removedList, files, notes);
  }

  private InputStream compareSimpleMapFiles(InputStream data1, InputStream data2,
    MapProject mapProject, List<String> files, List<String> notes) throws Exception {

    // map to list of records that have been updated (sorted by key)
    List<String> updatedList = new ArrayList<>();
    // map to list of records that are new in the second file
    Set<String> newList = new HashSet<>();
    // map to list of records that have been inactivated in the second file
    Set<String> inactivatedList = new HashSet<>();
    // map to list of records that have been removed in the second file
    Set<String> removedList = new HashSet<>();
    String line1, line2;

    BufferedReader in1 = new BufferedReader(new InputStreamReader(data1, "UTF-8"));
    in1.mark(100000000);
    BufferedReader in2 = new BufferedReader(new InputStreamReader(data2, "UTF-8"));

    int noChangeCount = 0;
    Map<String, Set<SimpleLine>> key1Map = new HashMap<>();
    Map<String, Set<SimpleLine>> key2Map = new HashMap<>();

    int i = 0;
    // populate key1Map with key1 and lineData (no UUID)
    while ((line1 = in1.readLine()) != null) {

      String tokens1[] = line1.split("\t");
      if (!tokens1[4].equals(mapProject.getRefSetId())) {
        continue;
      }
      i++;
      // if refCompId already in map, just add new effectiveTime/TargetId&Active
      if (key1Map.containsKey(tokens1[5])) {
        key1Map.get(tokens1[5]).add(new SimpleLine(line1));
      } else {
        Set<SimpleLine> setOfLines = new HashSet<>();
        setOfLines.add(new SimpleLine(line1));
        key1Map.put(tokens1[5], setOfLines);
      }
    }
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("key1Map count: " + key1Map.size() + " " + i);

    i = 0;
    // go through second file, cache, and figure out what is new and what has
    // changed
    while ((line2 = in2.readLine()) != null) {
      String tokens2[] = line2.split("\t");
      if (!tokens2[4].equals(mapProject.getRefSetId())) {
        continue;
      }
      i++;
      // populate key2Map with key2 and lineData (no UUID)
      if (key2Map.containsKey(tokens2[5])) {
        key2Map.get(tokens2[5]).add(new SimpleLine(line2));
      } else {
        Set<SimpleLine> setOfLines = new HashSet<>();
        setOfLines.add(new SimpleLine(line2));
        key2Map.put(tokens2[5], setOfLines);
      }

      // if key1Map has key2, this record is not new - either it hasn't changed
      // or it has been updated
      if (key1Map.containsKey(tokens2[5])) {
        Set<SimpleLine> entries = key1Map.get(tokens2[5]);
        // effectiveTime, target, and active all static?
        boolean noChange = false;
        boolean inactivated = false;
        for (SimpleLine lineData : entries) {
          if (lineData.getTargetId().equals(tokens2[6])
              && tokens2[2].equals(lineData.isActive() ? "1" : "0")
              && lineData.getEffectiveTime().equals(tokens2[1])) {
            noChange = true;
            noChangeCount++;
            break;
          }
        }
        if (!noChange) {

          // inactivated?, check if active is the only thing that isn't equal
          for (SimpleLine lineData : entries) {
            if (lineData.getTargetId().equals(tokens2[6])
                && lineData.isActive() != Boolean.valueOf(tokens2[2])
            /* && lineData.getEffectiveTime().equals(tokens2[1]) */) {
              inactivatedList.add(line2);
              inactivated = true;
              break;
            }
          }

          // not inactivated, updated in some other way
          if (!inactivated) {
            for (SimpleLine lineData : entries) {
              if (lineData.isActive() && tokens2[2].equals("1")) {
                // something updated
                // only add to list if records are active
                String line1Sub =
                    "\t" + lineData.getEffectiveTime() + "\t" + (lineData.isActive() ? "1" : "0")
                        + "\t" + lineData.getModuleId() + "\t" + lineData.getRefsetId() + "\t"
                        + lineData.getRefCompId() + "\t" + lineData.getTargetId();
                String line2Sub = line2.substring(line2.indexOf("\t"));
                updatedList.add(line1Sub + "\t" + line2Sub);
                continue;
              }
            }
          }
        }
        // key2 was not in first file, this is new
      } else {
        newList.add(line2);
      }
    }
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("key2Map count: " + key2Map.size() + " " + i);
    in1.reset();

    // determine records that were removed
    while ((line1 = in1.readLine()) != null) {
      String tokens1[] = line1.split("\t");
      if (!tokens1[4].equals(mapProject.getRefSetId())) {
        continue;
      }
      if (!key2Map.containsKey(tokens1[5])) {
        removedList.add(line1);
      }
    }

    in1.close();
    in2.close();

    // log statements
    Logger.getLogger(MappingServiceRest.class).info("new List count:" + newList.size());
    Logger.getLogger(MappingServiceRest.class)
        .info("inactivated List count:" + inactivatedList.size());
    Logger.getLogger(MappingServiceRest.class).info("updated List count:" + updatedList.size());
    Logger.getLogger(MappingServiceRest.class).info("removed List count:" + removedList.size());
    Logger.getLogger(MappingServiceRest.class).info("no change count:" + noChangeCount);

    // produce Excel report file
    final ExportReportHandler handler = new ExportReportHandler();
    return handler.exportSimpleFileComparisonReport(updatedList, newList, inactivatedList,
        removedList, files, notes);
  }

  @Override
  @GET
  @Path("/release/reports/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get release reports available for a given project",
      notes = "Gets release reports for a given project.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getReleaseReportList(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /release/reports/ " + mapProjectId);
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "get release reports",
          securityService);

      // get directory for release reports
      final Properties config = ConfigUtility.getConfigProperties();
      final String docDir = config.getProperty("map.principle.source.document.dir");

      final File projectDir = new File(docDir, mapProjectId.toString() + "/reports");
      File[] reports = projectDir.listFiles();

      if (reports == null) {
        return null;
      }

      final SearchResultList searchResultList = new SearchResultListJpa();
      for (File report : reports) {
        SearchResult searchResult = new SearchResultJpa();
        searchResult.setValue(report.getName());
        BasicFileAttributes attributes = Files.readAttributes(report.toPath(),
            BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        FileTime lastModifiedTime = attributes.lastModifiedTime();
        SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        String lastModified = df.format(lastModifiedTime.toMillis());
        searchResult.setValue2(lastModified);
        searchResultList.addSearchResult(searchResult);
      }
      return searchResultList;

    } catch (Exception e) {
      handleException(e, "trying to get release reports", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  @Override
  @GET
  @Path("/current/release/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get release file indicating current state for a given project",
      notes = "Get release file indicating current state for a given project.",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getCurrentReleaseFileName(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /current/release/ " + mapProjectId);
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "get current release file",
          securityService);

      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      final File projectDir = new File(this.getReleaseDirectoryPath(mapProject, "current"));
      File[] currentDirs = projectDir.listFiles();

      if (!projectDir.exists() && currentDirs == null) {
        return null;
      }

      SearchResultList searchResultList = new SearchResultListJpa();
      for (File currentDir : currentDirs) {
        SearchResult searchResult = new SearchResultJpa();
        File[] releaseFiles = currentDir.listFiles();

        if (releaseFiles == null) {
          return null;
        }

        for (File file : releaseFiles) {
          // filter out human readable and any other release by-products
          if (!file.getName().contains("SimpleMapSnapshot")
              && !file.getName().contains("ExtendedMapSnapshot")) {
            continue;
          }
          BasicFileAttributes attributes = Files.readAttributes(file.toPath(),
              BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
          FileTime lastModifiedTime = attributes.lastModifiedTime();
          SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
          String lastModified = df.format(lastModifiedTime.toMillis());
          // if this is the most recent, return this file
          if (searchResult.getValue2() == null
              || lastModified.compareTo(searchResult.getValue2()) > 0) {
            searchResult.setValue(file.getName());
            searchResult.setValue2(file.getName());
            searchResult.setTerminologyVersion(lastModified);
            searchResult.setTerminology("current");
          }
        }
        searchResultList.addSearchResult(searchResult);
      }
      return searchResultList;

    } catch (Exception e) {
      handleException(e, "trying to get current release file", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  @Override
  @POST
  @Path("/amazons3/file")
  @ApiOperation(value = "Downloads file from AWS", notes = "Downloads file from AWS.",
      response = InputStream.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces("text/plain")
  public InputStream downloadFileFromAmazonS3(
    @ApiParam(value = "File path, in JSON or XML POST data", required = true) String filePath,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Mapping): /amazons3/file" + " " + filePath);

    String user = "";
    try {
      // authorize
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "download file from aws",
          securityService);

      AmazonS3 s3Client = null;
      s3Client = AmazonS3ServiceJpa.connectToAmazonS3();

      // stream first file from aws
      S3Object file1 =
          s3Client.getObject(new GetObjectRequest("release-ihtsdo-prod-published", filePath));
      InputStream objectData1 = file1.getObjectContent();

      return objectData1;

    } catch (Exception e) {
      handleException(e, "trying to download file from AWS", user, "", "");
      return null;
    } finally {
      securityService.close();
    }
  }

  @Override
  @POST
  @Path("/current/file/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Downloads current release file", notes = "Downloads current release file.",
      response = InputStream.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces("text/plain")
  public InputStream downloadCurrentReleaseFile(
    @ApiParam(value = "File name, in JSON or XML POST data", required = true) String fileName,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Mapping): /current/file" + " " + fileName);

    final MappingService mappingService = new MappingServiceJpa();
    String user = "";
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "download current release file",
          securityService);

      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      Pattern datePattern = Pattern.compile("\\d{8}_\\d{4}");
      Matcher m = datePattern.matcher(fileName);
      String fileDate = "";
      if (m.find()) {
        fileDate = m.group(0);
      }
      final File projectDir =
          new File(this.getReleaseDirectoryPath(mapProject, "current/" + fileDate));
      InputStream objectData1 = new FileInputStream(new File(projectDir, fileName));

      return objectData1;

    } catch (Exception e) {
      handleException(e, "trying to download current release file", user, "", "");
      return null;
    } finally {
      securityService.close();
      mappingService.close();
    }
  }

  /**
   * 
   * @param string
   * @return
   */
  private String removeSpaces(String string) {
    if (string != null)
      return string.replace(" ", "").trim();
    else
      return null;
  }

  @Override
  @GET
  @Path("/amazons3/files/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get list of files from AWS S3 bucket for a given project",
      notes = "Gets list of files from AWS S3 for a given project.",
      response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getFileListFromAmazonS3(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token",
        required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /amazons3/files/" + mapProjectId);

    final MappingService mappingService = new MappingServiceJpa();
    final AmazonS3Service amazonS3Service = new AmazonS3ServiceJpa();
    String user = "";

    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "get file list from amazon s3",
          securityService);

      final MapProject mapProject =
          mappingService.getMapProject(Long.valueOf(mapProjectId).longValue());
      return amazonS3Service.getFileListFromAmazonS3(mapProject);

    } catch (Exception e) {
      handleException(e, "trying to get file list from amazon s3", user, mapProjectId.toString(),
          "");
    } finally {
      amazonS3Service.close();
      mappingService.close();
      securityService.close();
    }

    return null;
  }

  /**
   * The Class SimpleLine.
   */
  class SimpleLine {

    String targetId = "";

    String refCompId = "";

    boolean active = true;

    String refsetId = "";

    String effectiveTime = "";

    String moduleId = "";

    public SimpleLine(String line) throws Exception {
      String tokens[] = line.split("\t");

      this.targetId = tokens[6];
      this.refCompId = tokens[5];
      this.active = tokens[2].equals("1") ? true : false;
      this.moduleId = tokens[3];
      this.refsetId = tokens[4];
      this.effectiveTime = tokens[1];
    }

    public String getModuleId() {
      return moduleId;
    }

    public String getTargetId() {
      return targetId;
    }

    public String getRefCompId() {
      return refCompId;
    }

    public boolean isActive() {
      return active;
    }

    public String getRefsetId() {
      return refsetId;
    }

    public String getEffectiveTime() {
      return effectiveTime;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (active ? 1231 : 1237);
      result = prime * result + ((effectiveTime == null) ? 0 : effectiveTime.hashCode());
      result = prime * result + ((moduleId == null) ? 0 : moduleId.hashCode());
      result = prime * result + ((refCompId == null) ? 0 : refCompId.hashCode());
      result = prime * result + ((refsetId == null) ? 0 : refsetId.hashCode());
      result = prime * result + ((targetId == null) ? 0 : targetId.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      SimpleLine other = (SimpleLine) obj;
      if (active != other.active)
        return false;
      if (effectiveTime == null) {
        if (other.effectiveTime != null)
          return false;
      } else if (!effectiveTime.equals(other.effectiveTime))
        return false;
      if (moduleId == null) {
        if (other.moduleId != null)
          return false;
      } else if (!moduleId.equals(other.moduleId))
        return false;
      if (refCompId == null) {
        if (other.refCompId != null)
          return false;
      } else if (!refCompId.equals(other.refCompId))
        return false;
      if (refsetId == null) {
        if (other.refsetId != null)
          return false;
      } else if (!refsetId.equals(other.refsetId))
        return false;
      if (targetId == null) {
        if (other.targetId != null)
          return false;
      } else if (!targetId.equals(other.targetId))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "SimpleLine [targetId=" + targetId + ", refCompId=" + refCompId + ", active=" + active
          + ", refsetId=" + refsetId + ", effectiveTime=" + effectiveTime + ", moduleId=" + moduleId
          + "]";
    }

  }

  /**
   * The Class ExtendedLine.
   */
  class ExtendedLine extends SimpleLine {

    private String mapGroup = "";

    private String mapPriority = "";

    private String mapRule = "";

    private String mapAdvice = "";

    private String correlationId = "";

    private String mapCategoryId = "";

    public ExtendedLine(String line) throws Exception {

      super(line);

      String tokens[] = line.split("\t");

      this.effectiveTime = tokens[1];
      this.active = tokens[2].equals("1") ? true : false;
      this.moduleId = tokens[3];
      this.refsetId = tokens[4];
      this.refCompId = tokens[5];
      this.mapGroup = tokens[6];
      this.mapPriority = tokens[7];
      this.mapRule = tokens[8];
      this.mapAdvice = tokens[9];
      this.targetId = tokens[10];
      this.correlationId = tokens[11];
      this.mapCategoryId = tokens[12];
    }

    @Override
    public String toString() {
      return "ExtendedLine [mapGroup=" + mapGroup + ", mapPriority=" + mapPriority + ", mapRule="
          + mapRule + ", mapAdvice=" + mapAdvice + ", correlationId=" + correlationId
          + ", mapCategoryId=" + mapCategoryId + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((correlationId == null) ? 0 : correlationId.hashCode());
      result = prime * result + ((mapAdvice == null) ? 0 : mapAdvice.hashCode());
      result = prime * result + ((mapCategoryId == null) ? 0 : mapCategoryId.hashCode());
      result = prime * result + ((mapGroup == null) ? 0 : mapGroup.hashCode());
      result = prime * result + ((mapPriority == null) ? 0 : mapPriority.hashCode());
      result = prime * result + ((mapRule == null) ? 0 : mapRule.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      ExtendedLine other = (ExtendedLine) obj;
      if (!getOuterType().equals(other.getOuterType()))
        return false;
      if (correlationId == null) {
        if (other.correlationId != null)
          return false;
      } else if (!correlationId.equals(other.correlationId))
        return false;
      if (mapAdvice == null) {
        if (other.mapAdvice != null)
          return false;
      } else if (!mapAdvice.equals(other.mapAdvice))
        return false;
      if (mapCategoryId == null) {
        if (other.mapCategoryId != null)
          return false;
      } else if (!mapCategoryId.equals(other.mapCategoryId))
        return false;
      if (mapGroup == null) {
        if (other.mapGroup != null)
          return false;
      } else if (!mapGroup.equals(other.mapGroup))
        return false;
      if (mapPriority == null) {
        if (other.mapPriority != null)
          return false;
      } else if (!mapPriority.equals(other.mapPriority))
        return false;
      if (mapRule == null) {
        if (other.mapRule != null)
          return false;
      } else if (!mapRule.equals(other.mapRule))
        return false;
      return true;
    }

    public String getMapGroup() {
      return mapGroup;
    }

    public String getMapPriority() {
      return mapPriority;
    }

    public String getMapRule() {
      return mapRule;
    }

    public String getMapAdvice() {
      return mapAdvice;
    }

    public String getCorrelationId() {
      return correlationId;
    }

    public String getMapCategoryId() {
      return mapCategoryId;
    }

    private MappingServiceRestImpl getOuterType() {
      return MappingServiceRestImpl.this;
    }
  }
}
