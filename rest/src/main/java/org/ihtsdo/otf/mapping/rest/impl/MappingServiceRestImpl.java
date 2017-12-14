/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
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

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.KeyValuePair;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairList;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairLists;
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
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.rest.MappingServiceRest;
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

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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

  private static final int MAX_RESULTS = 10000;
  
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapProjects(java.lang.String)
   */
  @Override
  @GET
  @Path("/project/projects")
  @ApiOperation(value = "Get map projects", notes = "Gets a list of all map projects.", response = MapProjectListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProjectListJpa getMapProjects(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/projects");
    final MappingService mappingService = new MappingServiceJpa();
    String user = null;
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map projects",
          securityService);

      // instantiate list of projects to return
      final MapProjectListJpa mapProjects = new MapProjectListJpa();

      // cycle over projects and verify that this user can view each
      // project
      for (final MapProject mapProject : mappingService.getMapProjects()
          .getMapProjects()) {

        // if this user has a role of VIEWER or above for this project
        // (i.e. is
        // not NONE)
        if (!securityService
            .getMapProjectRoleForToken(authToken, mapProject.getId())
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapProject(java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get map project by id", notes = "Gets a map project for the specified parameters.", response = MapProject.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProject getMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/id/" + mapProjectId.toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get the map project",
          securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);
      mapProject.getScopeConcepts().size();
      mapProject.getScopeExcludedConcepts().size();
      mapProject.getMapAdvices().size();
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapProject(org.ihtsdo.otf.mapping.jpa.MapProjectJpa, java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/project/add")
  @ApiOperation(value = "Add a map project", notes = "Adds the specified map project.", response = MapProjectJpa.class)
  public MapProject addMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/add");

    String user = null;
    String project = "";
    final MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
          "add a map project", securityService);
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapProject(org.ihtsdo.otf.mapping.jpa.MapProjectJpa, java.lang.String)
   */
  @Override
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/project/update")
  @ApiOperation(value = "Update a map project", notes = "Updates specified map project if it already exists.", response = MapProjectJpa.class)
  public void updateMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/update");

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

      // scope includes and excludes are transient, and must be added to
      // project
      // before update from webapp
      final MapProject mapProjectFromDatabase =
          mappingService.getMapProject(mapProject.getId());

      // set the scope concepts and excludes to the contents of the
      // database
      // prior to update
      mapProject.setScopeConcepts(mapProjectFromDatabase.getScopeConcepts());
      mapProject.setScopeExcludedConcepts(
          mapProjectFromDatabase.getScopeExcludedConcepts());

      // update the project and close the service
      mappingService.updateMapProject(mapProject);

    } catch (Exception e) {
      handleException(e, "trying to update a map project", user,
          mapProject.getName(), "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapProject(org.ihtsdo.otf.mapping.jpa.MapProjectJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/project/delete")
  @ApiOperation(value = "Remove a map project", notes = "Removes specified map project if it already exists.", response = MapProject.class)
  public void removeMapProject(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping): /project/delete for " + mapProject.getName());

    String user = null;

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
          "update a map project", securityService);

      mappingService.removeMapProject(mapProject.getId());

    } catch (Exception e) {
      handleException(e, "trying to remove a map project", user,
          mapProject.getName(), "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#cloneMapProject(org.ihtsdo.otf.mapping.jpa.MapProjectJpa, java.lang.String)
   */
  @Override
  @PUT
  @Path("/clone")
  @ApiOperation(value = "Clone map project", notes = "Adds the specified map project, which is a potentially modified copy of another map project", response = MapProjectJpa.class)
  public MapProject cloneMapProject(
    @ApiParam(value = "MapProject PUT data", required = false) MapProjectJpa mapProject,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call PUT (Mapping): /clone "
        + mapProject.getId() + ", " + mapProject);

    final MappingService mappingService = new MappingServiceJpa();
    try {
    	// authorize call
        authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
            "clone a map project", securityService);

      mappingService.setTransactionPerOperation(false);
      mappingService.beginTransaction();

      // Add the map project (null the id)
      final Long mapProjectId = mapProject.getId();
      final MapProject originMapProject = mappingService.getMapProject(mapProjectId);

      // Determine if refset with this id already exists in this project.
      /*if (originRefset.getTerminologyId().equals(refset.getTerminologyId())
          && originRefset.getProject().getId()
              .equals(refset.getProject().getId())) {
        throw new LocalException(
            "Duplicate refset terminology id within the project, "
                + "please change terminology id");
      }*/
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

/*      // Copy all the members if EXTENSIONAL
      if (refset.getType() == Refset.Type.EXTENSIONAL) {

        // Get the original reference set
        for (final ConceptRefsetMember originMember : originMembers) {
          final ConceptRefsetMember member =
              new ConceptRefsetMemberJpa(originMember);
          member.setPublished(false);
          member.setPublishable(true);
          member.setRefset(newRefset);
          member.setEffectiveTime(null);
          // Insert new members
          member.setId(null);
          member.setLastModifiedBy(userName);
          refsetService.addMember(member);
        }
        // Resolve definition if INTENSIONAL
      } else if (refset.getType() == Refset.Type.INTENSIONAL) {
        // Copy inclusions and exclusions from origin refset
        for (final ConceptRefsetMember member : originMembers) {
          if (member.getMemberType() == Refset.MemberType.INCLUSION
              || member.getMemberType() == Refset.MemberType.EXCLUSION) {
            final ConceptRefsetMember member2 =
                new ConceptRefsetMemberJpa(member);
            member2.setRefset(newRefset);
            member2.setId(null);
            refsetService.addMember(member2);
            newRefset.addMember(member2);
          }
        }
        refsetService.resolveRefsetDefinition(newRefset);
      }*/
      
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

  
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#findMapProjectsForQuery(java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/project")
  @ApiOperation(value = "Find map projects by query", notes = "Gets a list of search results for map projects matching the lucene query.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findMapProjectsForQuery(
    @ApiParam(value = "Query, e.g. 'SNOMED to ICD10'", required = true) @QueryParam("query") String query,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /project " + query);
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "find map projects",
          securityService);

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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapProjectsForUser(java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/project/user/id/{username}")
  @ApiOperation(value = "Get all map projects for a user", notes = "Gets a list of map projects for the specified user.", response = MapProjectListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapProjectListJpa getMapProjectsForUser(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String mapUserName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/user/id/" + mapUserName);
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get map projects for user", securityService);

      final MapUser mapLead = mappingService.getMapUser(mapUserName);
      final MapProjectListJpa mapProjects =
          (MapProjectListJpa) mappingService.getMapProjectsForMapUser(mapLead);

      for (final MapProject mapProject : mapProjects.getMapProjects()) {
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
      return mapProjects;

    } catch (Exception e) {
      handleException(e, "trying to get the map projects for a given user",
          user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // ///////////////////////////////////////////////////
  // SCRUD functions: Map Users
  // ///////////////////////////////////////////////////

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapUsers(java.lang.String)
   */
  @Override
  @GET
  @Path("/user/users")
  @ApiOperation(value = "Get all mapping users", notes = "Gets all map users.", response = MapUserListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapUserListJpa getMapUsers(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /user/users");
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map users",
          securityService);

      final MapUserListJpa mapUsers =
          (MapUserListJpa) mappingService.getMapUsers();
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
          mapUser.setEmail(
              mapUser.getEmail().substring(0, mapUser.getEmail().indexOf('@')));
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getScopeConceptsForMapProject(java.lang.Long, org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeConcepts")
  @ApiOperation(value = "Get scope concepts for a map project", notes = "Gets a (pageable) list of scope concepts for a map project", response = SearchResultListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getScopeConceptsForMapProject(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Paging/filtering/sorting object", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
    String projectName = "";
    String user = "";

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.VIEWER,
          "get scope concepts", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);

      final SearchResultList results = mappingService
          .getScopeConceptsForMapProject(mapProject, pfsParameter);

      return results;

    } catch (Exception e) {
      this.handleException(e, "trying to get scope concepts", user, projectName,
          "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }


  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addScopeConceptsToMapProject(java.util.List, java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeConcepts/add")
  @ApiOperation(value = "Adds a list of scope concepts to a map project", notes = "Adds a list of scope concepts to a map project.", response = ValidationResultJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ValidationResult addScopeConceptsToMapProject(
    @ApiParam(value = "List of concepts to add, e.g. {'100073004', '100075006'", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
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
          result.addWarning(
              "Concept " + terminologyId + " is already in scope, skipping.");
        } else if (contentService.getConcept(terminologyId,
            mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion()) != null) {
          mapProject.addScopeConcept(terminologyId);
          mappingService.updateMapProject(mapProject);
          result.addMessage("Concept " + terminologyId + " added to scope.");
        } else {
          result.addWarning(
              "Concept " + terminologyId + " does not exist, skipping.");
        }
      }
      return result;
    } catch (Exception e) {
      this.handleException(e, "trying to add scope concept to project", user,
          projectName, "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeScopeConceptFromMapProject(java.lang.String, java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeConcept/remove")
  @ApiOperation(value = "Removes a single scope concept from a map project", notes = "Removes a single scope concept from a map project.", response = Response.class)
  public ValidationResult removeScopeConceptFromMapProject(
    @ApiParam(value = "Concept to remove, e.g. 100075006", required = true) String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
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
        mappingService.updateMapProject(mapProject);
        result.addMessage("Concept " + terminologyId + " has been removed from scope.");
      } else if (contentService.getConcept(terminologyId,
        mapProject.getSourceTerminology(),
        mapProject.getSourceTerminologyVersion()) == null) {
        result.addWarning("Concept " + terminologyId + " does not exist, skipping.");
      } else {
        result.addWarning(
            "Concept " + terminologyId + " was not in scope for this project, skipping.");
      }
     
      return result;
      
    } catch (Exception e) {
      this.handleException(e, "trying to remove scope concept from project",
          user, projectName, "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeScopeConceptsFromMapProject(java.util.List, java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeConcepts/remove")
  @ApiOperation(value = "Removes a list of scope concepts from a map project", notes = "Removes a list of scope concept from a map project.", response = Response.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ValidationResult removeScopeConceptsFromMapProject(
    @ApiParam(value = "List of concepts to remove, e.g. {'100073004', '100075006'", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping):  /project/id/" + projectId + "/scopeConcepts");
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
          mappingService.updateMapProject(mapProject);
          result.addMessage("Concept " + terminologyId + " has been removed from scope.");
        } else if (contentService.getConcept(terminologyId,
          mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion()) == null) {
          result.addWarning("Concept " + terminologyId + " does not exist, skipping.");
        } else {
          result.addWarning(
              "Concept " + terminologyId + " was not in scope for this project, skipping.");
        }
      }
      return result;

    } catch (Exception e) {
      this.handleException(e, "trying to remove scope concepts from project",
          user, projectName, "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getScopeExcludedConceptsForMapProject(java.lang.Long, org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts")
  @ApiOperation(value = "Get scope excluded concepts for a map project", notes = "Gets a (pageable) list of scope excluded concepts for a map project", response = SearchResultListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getScopeExcludedConceptsForMapProject(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Paging/filtering/sorting object", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcepts");
    String projectName = "";
    String user = "";
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.VIEWER,
          "get scope concepts for projects", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);
      final SearchResultList results = mappingService
          .getScopeExcludedConceptsForMapProject(mapProject, pfsParameter);
      return results;

    } catch (Exception e) {
      this.handleException(e, "trying to get scope excluded concepts", user,
          projectName, "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addScopeExcludedConceptsToMapProject(java.util.List, java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts/add")
  @ApiOperation(value = "Adds a list of scope excluded concepts to a map project", notes = "Adds a list of scope excluded concepts to a map project.", response = Response.class)
  public ValidationResult addScopeExcludedConceptsToMapProject(
    @ApiParam(value = "List of concepts to add, e.g. {'100073004', '100075006'", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcepts/add");
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
        } else if (contentService.getConcept(terminologyId,
            mapProject.getSourceTerminology(),
            mapProject.getSourceTerminologyVersion()) != null) {
          mapProject.addScopeExcludedConcept(terminologyId);
          mappingService.updateMapProject(mapProject);
          result.addMessage("Concept " + terminologyId + " added to scope excluded list.");
        } else {
          result.addWarning(
              "Concept " + terminologyId + " does not exist, skipping.");
        }
      }
      return result;
    } catch (Exception e) {
      this.handleException(e, "trying to add scope excluded concept to project",
          user, projectName, "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeScopeExcludedConceptFromMapProject(java.lang.String, java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcept/remove")
  @ApiOperation(value = "Removes a single scope excluded concept from a map project", notes = "Removes a single scope excluded concept from a map project.", response = Response.class)
  public ValidationResult removeScopeExcludedConceptFromMapProject(
    @ApiParam(value = "Concept to remove, e.g. 100075006", required = true) String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcept/remove");
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
        mappingService.updateMapProject(mapProject);
        result.addMessage("Concept " + terminologyId + " has been removed from scope excluded list.");
      } else if (contentService.getConcept(terminologyId,
        mapProject.getSourceTerminology(),
        mapProject.getSourceTerminologyVersion()) == null) {
        result.addWarning("Concept " + terminologyId + " does not exist, skipping.");
      } else {
        result.addWarning(
            "Concept " + terminologyId + " was not in scope exluded list for this project, skipping.");
      }
     
      return result;
    } catch (Exception e) {
      this.handleException(e,
          "trying to remove scope excluded concept from project", user,
          projectName, "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeScopeExcludedConceptsFromMapProject(java.util.List, java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{projectId}/scopeExcludedConcepts/remove")
  @ApiOperation(value = "Removes a list of scope excluded concepts from a map project", notes = "Removes a list of scope excluded concept from a map project.", response = Response.class)
  public ValidationResult removeScopeExcludedConceptsFromMapProject(
    @ApiParam(value = "List of concepts to remove, e.g. {'100073004', '100075006'", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /project/id/" + projectId
            + "/scopeExcludedConcepts/remove");
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
          mappingService.updateMapProject(mapProject);
          result.addMessage("Concept " + terminologyId + " has been removed from scope excluded list.");
        } else if (contentService.getConcept(terminologyId,
          mapProject.getSourceTerminology(),
          mapProject.getSourceTerminologyVersion()) == null) {
          result.addWarning("Concept " + terminologyId + " does not exist, skipping.");
        } else {
          result.addWarning(
              "Concept " + terminologyId + " was not in scope exluded list for this project, skipping.");
        }
      }
      return result;

    } catch (Exception e) {
      this.handleException(e,
          "trying to remove scope excluded concepts from project", user,
          projectName, "");
    } finally {
      mappingService.close();
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapUser(java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/user/id/{username}")
  @ApiOperation(value = "Get user by username", notes = "Gets a user by a username.", response = MapUser.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapUser getMapUser(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String mapUserName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): user/id/" + mapUserName);
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "get map user",
          securityService);

      final MapUser mapUser = mappingService.getMapUser(mapUserName);
      return mapUser;

    } catch (Exception e) {
      handleException(e, "trying to get a map user", mapUserName, mapUserName,
          "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapUser(org.ihtsdo.otf.mapping.jpa.MapUserJpa, java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/user/add")
  @ApiOperation(value = "Add a user", notes = "Adds the specified user.", response = MapUserJpa.class)
  public MapUser addMapUser(
    @ApiParam(value = "User, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /user/add");
    String userName = null;
    final MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      userName = authorizeApp(authToken, MapUserRole.LEAD, "add a user",
          securityService);

      // Check if user already exists and send better message
      for (final MapUser user : mappingService.getMapUsers().getMapUsers()) {
        if (user.getName().equals(mapUser.getName())) {
          throw new LocalException(
              "This map user already exists: " + user.getName());
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapUser(org.ihtsdo.otf.mapping.jpa.MapUserJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/user/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a user", notes = "Updates the specified user.", response = MapUserJpa.class)
  public void updateMapUser(
    @ApiParam(value = "User, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /user/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "update a user",
          securityService);

      mappingService.updateMapUser(mapUser);

    } catch (Exception e) {
      handleException(e, "trying to update a user", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapUser(org.ihtsdo.otf.mapping.jpa.MapUserJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/user/delete")
  @ApiOperation(value = "Remove a user", notes = "Removes the specified user.", response = MapUser.class)
  public void removeMapUser(
    @ApiParam(value = "Map project, in JSON or XML POST data", required = true) MapUserJpa mapUser,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping): /user/delete for user " + mapUser.getName());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "remove a user",
          securityService);

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
  
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapAdvices(java.lang.String)
   */
  @Override
  @GET
  @Path("/advice/advices")
  @ApiOperation(value = "Get all mapping advices", notes = "Gets a list of all map advices.", response = MapAdviceListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapAdviceListJpa getMapAdvices(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /advice/advices");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map advices",
          securityService);

      final MapAdviceListJpa mapAdvices =
          (MapAdviceListJpa) mappingService.getMapAdvices();
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapAdvice(org.ihtsdo.otf.mapping.jpa.MapAdviceJpa, java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/advice/add")
  @ApiOperation(value = "Add an advice", notes = "Adds the specified map advice.", response = MapAdviceJpa.class)
  public MapAdvice addMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data", required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /advice/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "add map advice",
          securityService);

      // Check if advice already exists and send better message
      for (final MapAdvice advice : mappingService.getMapAdvices()
          .getMapAdvices()) {
        if (advice.getName().equals(mapAdvice.getName())) {
          throw new LocalException(
              "This map advice already exists: " + advice.getName());
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapAdvice(org.ihtsdo.otf.mapping.jpa.MapAdviceJpa, java.lang.String)
   */
  @Override
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/advice/update")
  @ApiOperation(value = "Update an advice", notes = "Updates the specified advice.", response = MapAdviceJpa.class)
  public void updateMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data", required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /advice/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "update map advice",
          securityService);

      mappingService.updateMapAdvice(mapAdvice);
    } catch (Exception e) {
      handleException(e, "trying to update an advice", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapAdvice(org.ihtsdo.otf.mapping.jpa.MapAdviceJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/advice/delete")
  @ApiOperation(value = "Remove an advice", notes = "Removes the specified map advice.", response = MapAdviceJpa.class)
  public void removeMapAdvice(
    @ApiParam(value = "Map advice, in JSON or XML POST data", required = true) MapAdviceJpa mapAdvice,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /advice/delete for user "
            + mapAdvice.getName());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "remove map advice",
          securityService);

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
  // SCRUD functions: Map AgeRange
  // ///////////////////////////////////////////////////
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapAgeRanges(java.lang.String)
   */
  @Override
  @GET
  @Path("/ageRange/ageRanges")
  @ApiOperation(value = "Get all mapping age ranges", notes = "Gets a list of all mapping age ranges.", response = MapAgeRangeListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapAgeRangeListJpa getMapAgeRanges(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /ageRange/ageRanges");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get age ranges",
          securityService);

      final MapAgeRangeListJpa mapAgeRanges =
          (MapAgeRangeListJpa) mappingService.getMapAgeRanges();
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapAgeRange(org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa, java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/ageRange/add")
  @ApiOperation(value = "Add an age range", notes = "Adds the specified age range.", response = MapAgeRangeJpa.class)
  public MapAgeRange addMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data", required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /ageRange/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "add map age range",
          securityService);

      // Check if age range already exists and send better message
      for (final MapAgeRange range : mappingService.getMapAgeRanges()
          .getMapAgeRanges()) {
        if (mapAgeRange.getName().equals(range.getName())) {
          throw new LocalException(
              "This map age range already exists: " + range.getName());
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapAgeRange(org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa, java.lang.String)
   */
  @Override
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/ageRange/update")
  @ApiOperation(value = "Update an age range", notes = "Updates the specified age range.", response = MapAgeRangeJpa.class)
  public void updateMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data", required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /ageRange/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "udpate age range",
          securityService);

      mappingService.updateMapAgeRange(mapAgeRange);
    } catch (Exception e) {
      handleException(e, "trying to update an age range", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapAgeRange(org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/ageRange/delete")
  @ApiOperation(value = "Remove an age range", notes = "Removes the specified age range.", response = MapAgeRangeJpa.class)
  public void removeMapAgeRange(
    @ApiParam(value = "Age range, in JSON or XML POST data", required = true) MapAgeRangeJpa mapAgeRange,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /ageRange/delete for user "
            + mapAgeRange.getName());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "remove age range",
          securityService);

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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRelations(java.lang.String)
   */
  @Override
  @GET
  @Path("/relation/relations")
  @ApiOperation(value = "Get all relations", notes = "Gets a list of all map relations.", response = MapRelationListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRelationListJpa getMapRelations(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /relation/relations");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map relations",
          securityService);

      final MapRelationListJpa mapRelations =
          (MapRelationListJpa) mappingService.getMapRelations();
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapRelation(org.ihtsdo.otf.mapping.jpa.MapRelationJpa, java.lang.String)
   */
  @Override
  @PUT
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/relation/add")
  @ApiOperation(value = "Add a map relation", notes = "Adds the specified map relation.", response = MapRelationJpa.class)
  public MapRelation addMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data", required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /relation/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "add map relation",
          securityService);

      // Check if relation already exists and send better message
      for (final MapRelation relation : mappingService.getMapRelations()
          .getMapRelations()) {
        if (relation.getName().equals(mapRelation.getName())) {
          throw new LocalException(
              "This map relation already exists: " + relation.getName());
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapRelation(org.ihtsdo.otf.mapping.jpa.MapRelationJpa, java.lang.String)
   */
  @Override
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/relation/update")
  @ApiOperation(value = "Update a map relation", notes = "Updates the specified map relation.", response = MapRelationJpa.class)
  public void updateMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data", required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /relation/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "update map relation",
          securityService);

      mappingService.updateMapRelation(mapRelation);
    } catch (Exception e) {
      handleException(e, "trying to update a relation", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapRelation(org.ihtsdo.otf.mapping.jpa.MapRelationJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/relation/delete")
  @ApiOperation(value = "Remove a map relation", notes = "Removes the specified map relation.", response = MapRelationJpa.class)
  public void removeMapRelation(
    @ApiParam(value = "Map relation, in JSON or XML POST data", required = true) MapRelationJpa mapRelation,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /relation/delete for user "
            + mapRelation.getName());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "remove map relation",
          securityService);

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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapPrinciples(java.lang.String)
   */
  @Override
  @GET
  @Path("/principle/principles")
  @ApiOperation(value = "Get all map principles", notes = "Gets a list of all map principles.", response = MapPrincipleListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapPrincipleListJpa getMapPrinciples(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /principle/principles");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map principles",
          securityService);

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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapPrinciple(java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/principle/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get a map principle", notes = "Gets a map principle for the specified id.", response = MapPrinciple.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapPrinciple getMapPrinciple(
    @ApiParam(value = "Map principle identifer", required = true) @PathParam("id") Long mapPrincipleId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping): /principle/id/" + mapPrincipleId.toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map principle",
          securityService);

      final MapPrinciple mapPrinciple =
          mappingService.getMapPrinciple(mapPrincipleId);
      return mapPrinciple;
    } catch (Exception e) {
      handleException(e, "trying to get the map principle", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapPrinciple(org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa, java.lang.String)
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
  @ApiOperation(value = "Add a map principle", notes = "Adds the specified map principle.", response = MapPrincipleJpa.class)
  public MapPrinciple addMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data", required = true) MapPrincipleJpa mapPrinciple,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /principle/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "add map principle",
          securityService);

      // Check if principle already exists and send better message
      for (final MapPrinciple principle : mappingService.getMapPrinciples()
          .getMapPrinciples()) {
        if (principle.getName().equals(mapPrinciple.getName()) && principle
            .getPrincipleId().equals(mapPrinciple.getPrincipleId())) {
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapPrinciple(org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa, java.lang.String)
   */
  @Override
  @POST
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Path("/principle/update")
  @ApiOperation(value = "Update a map principle", notes = "Updates the specified map principle.", response = MapPrincipleJpa.class)
  public void updateMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data", required = true) MapPrincipleJpa mapPrinciple,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /principle/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "update map principle",
          securityService);

      mappingService.updateMapPrinciple(mapPrinciple);

    } catch (Exception e) {
      handleException(e, "trying to update a map principle", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapPrinciple(org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/principle/delete")
  @ApiOperation(value = "Remove a map principle", notes = "Removes the specified map principle.")
  public void removeMapPrinciple(
    @ApiParam(value = "Map principle, in JSON or XML POST data", required = true) MapPrincipleJpa principle,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /principle/delete for id "
            + principle.getId().toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.LEAD, "remove map principle",
          securityService);

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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapUserPreferences(java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/userPreferences/user/id/{username}")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get user preferences", notes = "Gets user preferences for the specified username.", response = MapUserPreferencesJpa.class)
  public MapUserPreferences getMapUserPreferences(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String username,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call:  /userPreferences/user/id/" + username);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get map user prefs",
          securityService);
      return mappingService.getMapUserPreferences(username);

    } catch (Exception e) {
      handleException(e, "trying to get the map user preferences", user, "",
          "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapUserPreferences(org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa, java.lang.String)
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
  @ApiOperation(value = "Add user preferences", notes = "Adds the specified user preferences.", response = MapUserPreferencesJpa.class)
  public MapUserPreferences addMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data", required = true) MapUserPreferencesJpa mapUserPreferences,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /userPreferences/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
          "add map user prefs", securityService);

      return mappingService.addMapUserPreferences(mapUserPreferences);

    } catch (Exception e) {
      handleException(e, "trying to add map user preferences", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapUserPreferences(org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/userPreferences/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update user preferences", notes = "Updates the specified user preferences.", response = MapUserPreferencesJpa.class)
  public void updateMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data", required = true) MapUserPreferencesJpa mapUserPreferences,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /userPreferences/update with \n"
            + mapUserPreferences.toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "update map user prefs", securityService);

      mappingService.updateMapUserPreferences(mapUserPreferences);

    } catch (Exception e) {
      handleException(e, "trying to update map user preferences", user, "", "");
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapUserPreferences(org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/userPreferences/remove")
  @ApiOperation(value = "Remove user preferences", notes = "Removes specified user preferences.")
  public void removeMapUserPreferences(
    @ApiParam(value = "User preferences, in JSON or XML POST data", required = true) MapUserPreferencesJpa mapUserPreferences,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /userPreferences/remove for id "
            + mapUserPreferences.getId().toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "remove map user prefs", securityService);

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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRecord(java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get map record by id", notes = "Gets a map record for the specified id.", response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecord getMapRecord(
    @ApiParam(value = "Map record id", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/id/"
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
      user = authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.VIEWER, "get map record", securityService);

      // remove notes if this is not a specialist or above
      if (!securityService
          .getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId())
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#addMapRecord(org.ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
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
  @ApiOperation(value = "Add a map record", notes = "Adds the specified map record.", response = MapRecordJpa.class)
  public MapRecord addMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/add");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "add map record", securityService);

      return mappingService.addMapRecord(mapRecord);
    } catch (Exception e) {
      handleException(e, "trying to add a map record", user,
          mapRecord.getMapProjectId().toString(), mapRecord.getId().toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#updateMapRecord(org.ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/record/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a map record", notes = "Updates the specified map record.", response = Response.class)
  public void updateMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/update");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "update map record", securityService);

      mappingService.updateMapRecord(mapRecord);

    } catch (Exception e) {
      handleException(e, "trying to update the map record", user,
          mapRecord.getMapProjectId().toString(), mapRecord.getId().toString());
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapRecord(org.ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/record/delete")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Remove a map record", notes = "Removes the specified map record.", response = MapRecordJpa.class)
  public Response removeMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/delete with map record id = "
            + mapRecord.toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.ADMINISTRATOR, "remove map record", securityService);

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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#removeMapRecordsForMapProjectAndTerminologyIds(java.util.List, java.lang.Long, java.lang.String)
   */
  @Override
  @DELETE
  @Path("/record/records/delete/project/id/{projectId}/batch")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Remove a set of map records", notes = "Removes map records for specified project and a set of concept terminology ids", response = List.class)
  public ValidationResult removeMapRecordsForMapProjectAndTerminologyIds(
    @ApiParam(value = "Terminology ids, in JSON or XML POST data", required = true) List<String> terminologyIds,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/records/delete/project/id/"
            + projectId + "/batch with string argument " + terminologyIds);

    String user = null;
    String projectName = "";
    // instantiate the needed services
    final ContentService contentService = new ContentServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.ADMINISTRATOR,
          "remove map records for project and terminology ids",
          securityService);

      // validation report to return errors and warnings
      final ValidationResult validationResult = new ValidationResultJpa();

      // get the map project
      final MapProject mapProject = mappingService.getMapProject(projectId);
      projectName = mapProject.getName();

      // construct a list of terminology ids to remove
      // initially set to the Api argument
      // (instantiated to avoid concurrent modification errors
      // when modifying the list for descendant concepts)
      final List<String> terminologyIdsToRemove =
          new ArrayList<>(terminologyIds);

      int nRecordsRemoved = 0;
      int nScopeConceptsRemoved = 0;

      validationResult.addMessage(
          terminologyIds.size() + " concepts selected for map record removal");

      // cycle over the terminology ids
      for (final String terminologyId : terminologyIdsToRemove) {

        // get all map records for this project and concept
        final MapRecordList mapRecordList = mappingService
            .getMapRecordsForProjectAndConcept(projectId, terminologyId);

        // check if map records exist
        if (mapRecordList.getCount() == 0) {
          Logger.getLogger(MappingServiceRestImpl.class).warn(
              "No records found for project for concept id " + terminologyId);
          validationResult
              .addWarning("No records found for concept " + terminologyId);
        } else {
          for (final MapRecord mapRecord : mapRecordList.getMapRecords()) {
            Logger.getLogger(MappingServiceRestImpl.class)
                .info("Removing map record " + mapRecord.getId()
                    + " for concept " + mapRecord.getConceptId() + ", "
                    + mapRecord.getConceptName());

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
      validationResult
          .addMessage(nRecordsRemoved + " records successfully removed");

      // if scope concepts were removed, add a success message
      if (!mapProject.isScopeDescendantsFlag()) {

        validationResult.addMessage(nScopeConceptsRemoved
            + " concepts removed from project scope definition");
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRecordsForConceptId(java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/record/concept/id/{conceptId}")
  @ApiOperation(value = "Get map records by concept id", notes = "Gets a list of map records for the specified concept id.", response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsForConceptId(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/concept/id/" + conceptId);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get map records for concept", securityService);

      final MapRecordListJpa mapRecordList =
          (MapRecordListJpa) mappingService.getMapRecordsForConcept(conceptId);

      // return records that this user does not have permission to see
      final MapUser mapUser = mappingService
          .getMapUser(securityService.getUsernameForToken(authToken));
      final List<MapRecord> mapRecords = new ArrayList<>();

      // cycle over records and determine if this user can see them
      for (final MapRecord mr : mapRecordList.getMapRecords()) {

        // get the user's role for this record's project
        final MapUserRole projectRole = securityService
            .getMapProjectRoleForToken(authToken, mr.getMapProjectId());

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
      handleException(e, "trying to find records by the given concept id", user,
          "", conceptId);
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRecordsForConceptIdHistorical(java.lang.String, java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/record/concept/id/{conceptId}/project/id/{id:[0-9][0-9]*}/historical")
  @ApiOperation(value = "Get historical map records by concept id", notes = "Gets the latest map record revision for each map record with given concept id.", response = MapRecord.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsForConceptIdHistorical(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/concept/id/" + conceptId
            + "/project/id/" + mapProjectId + "/historical");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get map records for historical concept", securityService);

      final MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
          .getMapRecordRevisionsForConcept(conceptId, mapProjectId);

      // return records that this user does not have permission to see
      final MapUser mapUser = mappingService
          .getMapUser(securityService.getUsernameForToken(authToken));
      final List<MapRecord> mapRecords = new ArrayList<>();

      // cycle over records and determine if this user can see them
      for (final MapRecord mr : mapRecordList.getMapRecords()) {

        // get the user's role for this record's project
        final MapUserRole projectRole = securityService
            .getMapProjectRoleForToken(authToken, mr.getMapProjectId());

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
      handleException(e,
          "trying to find historical records by the given concept id", user, "",
          conceptId);
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRecordsForMapProjectAndQuery(java.lang.Long, org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String, java.lang.String, boolean, java.lang.String, java.lang.String)
   */
  @Override
  @POST
  @Path("/record/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get published map records by project id", notes = "Gets a list of map records for the specified map project id that have a workflow status of PUBLISHED or READY_FOR_PUBLICATION.", response = MapRecordListJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  //@CookieParam(value = "userInfo")
  public MapRecordListJpa getMapRecordsForMapProjectAndQuery(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Ancestor concept (inclusive) to restrict search results to", required = true) @QueryParam("ancestorId") String ancestorId,
    @ApiParam(value = "Source concept relationship name", required = false) @QueryParam("relationshipName") String relationshipName,
    @ApiParam(value = "Excludes descendants of ancestor id ", required = false) @QueryParam("excludeDescendants") boolean excludeDescendants,
    @ApiParam(value = "Search query string", required = false) @QueryParam("query") String query,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/project/id/" + mapProjectId + " "
            + ancestorId + ", " + excludeDescendants  + ", " + query);
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();

    // declare content service (may not be used)
    ContentService contentService = null;

    try {

      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "find map records for map project and query", securityService);

      // determine if a query was passed
      boolean queryFlag =
          (query != null && !query.isEmpty() && !query.equals("null") ) || (pfsParameter.getQueryRestriction() != null && !pfsParameter.getQueryRestriction().isEmpty());
      boolean ancestorFlag = ancestorId != null && !ancestorId.isEmpty()
          && !ancestorId.equals("null");

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
      final MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      if (role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        queryRestriction +=
            " AND (workflowStatus:PUBLISHED OR workflowStatus:READY_FOR_PUBLICATION)";
      } else {
        queryRestriction += " AND workflowStatus:PUBLISHED";
      }

      // set the pfs query restriction
      pfsLocal.setQueryRestriction(queryRestriction);

      SearchResultList searchResults;

      // if ancestor id specified, need to retrieve all results
      if (ancestorFlag || relationshipFlag) {
        final MapProject mapProject =
            mappingService.getMapProject(mapProjectId);

        // If there was a search query, combine them
        if (queryFlag) {
          pfsLocal.setStartIndex(0);
          pfsLocal.setMaxResults(MAX_RESULTS);

        // perform lucene search
        searchResults = (queryFlag
            ? mappingService.findMapRecordsForQuery(queryLocal, pfsLocal)
            : new SearchResultListJpa());

        if (searchResults.getTotalCount() > MAX_RESULTS) {
            throw new LocalException(searchResults.getTotalCount()
              + " potential string matches for ancestor or relationship search. Narrow your search and try again.");
          }
        if(searchResults.getCount() > 0) {
          ImmutableMap<String, SearchResult> resultsMap = Maps.uniqueIndex(searchResults.getSearchResults(), new Function<SearchResult, String>() {

            @Override
            public String apply(SearchResult input) {
              return input.getTerminologyId();
            }

          });
          searchResults = mappingService
              .findMapRecords(mapProjectId, ancestorId, excludeDescendants, relationshipName,
                  mapProject.getSourceTerminology(),
                  mapProject.getSourceTerminologyVersion(), descendantPfs, resultsMap.keySet());
          }
 
        }

        else {
          // Otherwise, just find all map records to include or exclude descendants
            searchResults = mappingService
                .findMapRecords(mapProjectId, ancestorId, excludeDescendants, relationshipName, 
                    mapProject.getSourceTerminology(),
                    mapProject.getSourceTerminologyVersion(), descendantPfs, Collections.<String> emptySet());
 
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

      // otherwise, use default paging
      else {

        // perform lucene search
        searchResults =
            mappingService.findMapRecordsForQuery(queryLocal, pfsLocal);
      }

      // retrieve the records for the paged results
      for (SearchResult sr : searchResults.getSearchResults()) {
        mapRecordList.addMapRecord(mappingService.getMapRecord(sr.getId()));
      }

      // set the total count
      mapRecordList.setTotalCount(searchResults.getTotalCount());

      // remove notes if this is not a specialist or above
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        for (final MapRecord mr : mapRecordList.getMapRecords()) {
          mr.setMapNotes(null);
        }
      }
      return mapRecordList;
    } catch (Exception e) {
      handleException(e,
          "trying to get the map records for a map project and query", user,
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getPublishedMapRecordsForMapProject(java.lang.Long, org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/record/project/id/{id:[0-9][0-9]*}/published")
  @ApiOperation(value = "Get published map records by map project id", notes = "Gets a list of map records for the specified map project id that have a workflow status of PUBLISHED.", response = MapRecordListJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  //@CookieParam(value = "userInfo")
  public MapRecordListJpa getPublishedMapRecordsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/project/id/"
            + mapProjectId.toString());

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    // execute the service call
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get published records for project", securityService);

      final MapRecordListJpa mapRecordList = (MapRecordListJpa) mappingService
          .getPublishedMapRecordsForMapProject(mapProjectId, pfsParameter);

      final MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapProjectId);
      for (final MapRecord mr : mapRecordList.getMapRecords()) {
        // remove notes if this is not a specialist or above
        if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
          mr.setMapNotes(null);
        }
      }
      return mapRecordList;
    } catch (Exception e) {
      handleException(e, "trying to get the map records for a map project",
          user, mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRecordRevisions(java.lang.Long, java.lang.String)
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
  @ApiOperation(value = "Get map record revision history", notes = "Gets a list of all revisions of a map record for the specified id.", response = MapRecordListJpa.class)
  public MapRecordList getMapRecordRevisions(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping): /record/id/" + mapRecordId + "/revisions");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      final MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, mapRecordId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity(
                "User does not have permissions to get the map record revisions.")
            .build());

      final MapRecordList revisions =
          mappingService.getMapRecordRevisions(mapRecordId);

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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRecordHistorical(java.lang.Long, java.lang.String)
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
  @ApiOperation(value = "Get latest state of a map record", notes = "Gets the current form of the map record or its last historical state for the specified map record id.", response = MapRecordListJpa.class)
  public MapRecord getMapRecordHistorical(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping): /record/id/" + mapRecordId + "/historical");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get historical map records for project", securityService);

      // try getting the current record
      MapRecord mapRecord = mappingService.getMapRecord(mapRecordId);

      // if no current record, look for revisions
      if (mapRecord == null) {
        List<MapRecord> list =
            mappingService.getMapRecordRevisions(mapRecordId).getMapRecords();
        mapRecord = list.get(0);
        mapRecord.getMapEntries().size();
      }

      final MapUserRole role = securityService
          .getMapProjectRoleForToken(authToken, mapRecord.getMapProjectId());
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
        mapRecord.setMapNotes(null);
      }

      return mapRecord;

    } catch (Exception e) {
      handleException(e,
          "trying to get the map record potentially using historical revisions",
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#computeMapRelation(org.ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
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
  @ApiOperation(value = "Compute map relation", notes = "Gets the computed map relation for the indicated map entry of the map record.", response = MapRelationJpa.class)
  public MapRelation computeMapRelation(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /relation/compute");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "compute map relation", securityService);

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
      Collections.sort(mapRecord.getMapEntries(),
          new TerminologyUtility.MapEntryComparator());

      final MapRelation mapRelation =
          algorithmHandler.computeMapRelation(mapRecord, mapEntry);
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#computeMapAdvice(java.lang.Integer, org.ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
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
  @ApiOperation(value = "Compute map advices", notes = "Gets the computed map advices for the indicated map entry of the map record.", response = MapAdviceJpa.class)
  public MapAdviceList computeMapAdvice(
    @ApiParam(value = "Index of entries in map record to compute advice for", required = true) @PathParam("entryIndex") Integer entryIndex,
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // call log
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /advice/compute");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // Bail if there are no entries for entryIndex
      if (mapRecord == null || mapRecord.getMapEntries() == null
          || mapRecord.getMapEntries().size() <= entryIndex) {
        return new MapAdviceListJpa();
      }

      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "compute map advice", securityService);

      final ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(
              mappingService.getMapProject(mapRecord.getMapProjectId()));

      final MapEntry mapEntry = mapRecord.getMapEntries().get(entryIndex);

      // bail if not ready yet
      if (mapEntry == null || mapEntry.getTargetId() == null) {
        return new MapAdviceListJpa();
      }

      // Make sure map entries are sorted by by mapGroup/mapPriority
      Collections.sort(mapRecord.getMapEntries(),
          new TerminologyUtility.MapEntryComparator());

      final MapAdviceList mapAdviceList =
          algorithmHandler.computeMapAdvice(mapRecord, mapEntry);
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

  // ///////////////////////////////////////////////
  // Role Management Services
  // ///////////////////////////////////////////////
  
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapUserRoleForMapProject(java.lang.String, java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/userRole/user/id/{username}/project/id/{id:[0-9][0-9]*}")
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Get the user's role for a map project", notes = "Gets the role for the specified user and map project.", response = SearchResultList.class)
  public MapUserRole getMapUserRoleForMapProject(
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String username,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call:  /userRole/user/id" + username + "/project/id/"
            + mapProjectId);

    final MappingService mappingService = new MappingServiceJpa();
    try {
      final MapUserRole mapUserRole =
          mappingService.getMapUserRoleForMapProject(username, mapProjectId);
      return mapUserRole;
    } catch (Exception e) {
      handleException(e, "trying to get the map user role for a map project",
          username, mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // /////////////////////////
  // Descendant services
  // /////////////////////////

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getUnmappedDescendantsForConcept(java.lang.String, java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/concept/id/{terminologyId}/unmappedDescendants/project/id/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Find unmapped descendants of a concept", notes = "Gets a list of search results for concepts having unmapped descendants.", response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getUnmappedDescendantsForConcept(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("id") String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    // log call
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /concept/id/" + terminologyId
            + "/project/id/" + mapProjectId);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get unmapped descendants for concept", securityService);

      final SearchResultList results = mappingService
          .findUnmappedDescendantsForConcept(terminologyId, mapProjectId, null);

      return results;

    } catch (Exception e) {
      handleException(e, "trying to get unmapped descendants for a concept",
          user, "", terminologyId);
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  // /////////////////////////////////////////////////////
  // Tree Position Routines for Terminology Browser
  // /////////////////////////////////////////////////////
  
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getTreePositionWithDescendantsForConceptAndMapProject(java.lang.String, java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/treePosition/project/id/{mapProjectId}/concept/id/{terminologyId}")
  @ApiOperation(value = "Gets project-specific tree positions with desendants", notes = "Gets a list of tree positions and their descendants for the specified parameters. Sets flags for valid targets and assigns any terminology notes based on project.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getTreePositionWithDescendantsForConceptAndMapProject(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken

  ) throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /treePosition/project/id/"
            + mapProjectId.toString() + "/concept/id/" + terminologyId);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();

    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get tree positions with descendants for concept and project",
          securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // get the local tree positions from content service
      final TreePositionList treePositions =
          contentService.getTreePositionsWithChildren(terminologyId,
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());
      Logger.getLogger(getClass())
          .info("  treepos count = " + treePositions.getTotalCount());
      if (treePositions.getCount() == 0) {
        return new TreePositionListJpa();
      }

      final String terminology =
          treePositions.getTreePositions().get(0).getTerminology();
      final String terminologyVersion =
          treePositions.getTreePositions().get(0).getTerminologyVersion();
      final Map<String, String> descTypes =
          metadataService.getDescriptionTypes(terminology, terminologyVersion);
      final Map<String, String> relTypes =
          metadataService.getRelationshipTypes(terminology, terminologyVersion);

      // Calculate info for tree position information panel
      contentService.computeTreePositionInformation(treePositions, descTypes,
          relTypes);

      // Determine whether code is valid (e.g. whether it should be a
      // link)
      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      mappingService.setTreePositionValidCodes(mapProject, treePositions,
          handler);
      // Compute any additional project specific handler info
      mappingService.setTreePositionTerminologyNotes(mapProject, treePositions,
          handler);

      return treePositions;
    } catch (Exception e) {
      handleException(e, "trying to get the tree positions with descendants",
          user, mapProjectId.toString(), terminologyId);
      return null;
    } finally {
      metadataService.close();
      contentService.close();
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getDestinationRootTreePositionsForMapProject(java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/treePosition/project/id/{projectId}/destination")
  @ApiOperation(value = "Get root tree positions", notes = "Gets a list of tree positions at the root of the terminology.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getDestinationRootTreePositionsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /treePosition/project/id/"
            + mapProjectId.toString() + "/destination");

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
      final TreePositionList treePositions = contentService
          .getRootTreePositions(mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion());
      Logger.getLogger(getClass())
          .info("  treepos count = " + treePositions.getTotalCount());
      if (treePositions.getCount() == 0) {
        return new TreePositionListJpa();
      }

      final String terminology =
          treePositions.getTreePositions().get(0).getTerminology();
      final String terminologyVersion =
          treePositions.getTreePositions().get(0).getTerminologyVersion();
      final Map<String, String> descTypes =
          metadataService.getDescriptionTypes(terminology, terminologyVersion);
      final Map<String, String> relTypes =
          metadataService.getRelationshipTypes(terminology, terminologyVersion);

      contentService.computeTreePositionInformation(treePositions, descTypes,
          relTypes);

      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      mappingService.setTreePositionValidCodes(mapProject, treePositions,
          handler);

      return treePositions;
    } catch (Exception e) {
      handleException(e,
          "trying to get the root tree positions for a terminology", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      metadataService.close();
      mappingService.close();
      contentService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getSourceRootTreePositionsForMapProject(java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/treePosition/project/id/{projectId}/source")
  @ApiOperation(value = "Get source terminology root tree positions", notes = "Gets a list of tree positions at the root of the terminology.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getSourceRootTreePositionsForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /treePosition/project/id/"
            + mapProjectId.toString() + "/source");

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
      final TreePositionList treePositions =
          contentService.getRootTreePositions(mapProject.getSourceTerminology(),
              mapProject.getSourceTerminologyVersion());
      Logger.getLogger(getClass())
          .info("  treepos count = " + treePositions.getTotalCount());
      if (treePositions.getCount() == 0) {
        return new TreePositionListJpa();
      }

      final String terminology =
          treePositions.getTreePositions().get(0).getTerminology();
      final String terminologyVersion =
          treePositions.getTreePositions().get(0).getTerminologyVersion();
      final Map<String, String> descTypes =
          metadataService.getDescriptionTypes(terminology, terminologyVersion);
      final Map<String, String> relTypes =
          metadataService.getRelationshipTypes(terminology, terminologyVersion);

      contentService.computeTreePositionInformation(treePositions, descTypes,
          relTypes);

      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      mappingService.setTreePositionValidCodes(mapProject, treePositions,
          handler);

      return treePositions;
    } catch (Exception e) {
      handleException(e,
          "trying to get the root tree positions for a terminology", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      metadataService.close();
      mappingService.close();
      contentService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getTreePositionGraphsForQueryAndMapProject(java.lang.String, java.lang.Long, org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/treePosition/project/id/{projectId}")
  @ApiOperation(value = "Get tree positions for query", notes = "Gets a list of tree positions for the specified parameters.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TreePositionList getTreePositionGraphsForQueryAndMapProject(
    @ApiParam(value = "Terminology browser query, e.g. 'cholera'", required = true) @QueryParam("query") String query,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("projectId") Long mapProjectId,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = false) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Mapping): /treePosition/project/id/" + mapProjectId
            + " " + query);

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
      final PfsParameter pfs =
          pfsParameter != null ? pfsParameter : new PfsParameterJpa();
      // pfs.setStartIndex(0);
      // pfs.setMaxResults(10);

      // get the tree positions from concept service
      TreePositionList treePositions = contentService
          .getTreePositionGraphForQuery(mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion(), qb.toString(),
              pfs);
      Logger.getLogger(getClass())
          .info("  treepos count = " + treePositions.getTotalCount());
      if (treePositions.getCount() == 0) {
        // Re-try search without +
        if (plusFlag) {
          treePositions = contentService.getTreePositionGraphForQuery(
              mapProject.getDestinationTerminology(),
              mapProject.getDestinationTerminologyVersion(), query, pfs);
        }
        if (treePositions.getCount() == 0) {
          return new TreePositionListJpa();
        }
      }

      final String terminology =
          treePositions.getTreePositions().get(0).getTerminology();
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
      treePositions.setTreePositions(
          handler.limitTreePositions(treePositions.getTreePositions()));

      contentService.computeTreePositionInformation(treePositions, descTypes,
          relTypes);

      mappingService.setTreePositionValidCodes(mapProject, treePositions,
          handler);
      mappingService.setTreePositionTerminologyNotes(mapProject, treePositions,
          handler);

      // TODO: if there are too many tree positions, then chop the tree
      // off (2
      // levels?)
      return treePositions;

    } catch (

    Exception e) {
      handleException(e, "trying to get the tree position graphs for a query",
          user, mapProjectId.toString(), "");
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapRecordsEditedByMapUser(java.lang.Long, java.lang.String, org.ihtsdo.otf.mapping.helpers.PfsParameterJpa, java.lang.String)
   */
  @Override
  @POST
  @Path("/record/project/id/{id}/user/id/{username}/edited")
  @ApiOperation(value = "Get map records edited by a user", notes = "Gets a list of map records for the specified map project and user.", response = MapRecordListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public MapRecordListJpa getMapRecordsEditedByMapUser(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Username (can be specialist, lead, or admin)", required = true) @PathParam("username") String username,
    @ApiParam(value = "Paging/filtering/sorting parameter, in JSON or XML POST data", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/project/id/" + mapProjectId
            + "/user/id/" + username + "/edited");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "get map records edited by map user", securityService);

      final MapRecordListJpa recordList =
          (MapRecordListJpa) mappingService.getRecentlyEditedMapRecords(
              new Long(mapProjectId), username, pfsParameter);
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getOriginMapRecordsForConflict(java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/record/id/{id:[0-9][0-9]*}/conflictOrigins")
  @ApiOperation(value = "Get specialist records for an assigned conflict or review record", notes = "Gets a list of specialist map records corresponding to a lead conflict or review record.", response = MapRecordListJpa.class)
  public MapRecordList getOriginMapRecordsForConflict(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("id") Long mapRecordId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /record/id/" + mapRecordId
            + "/conflictOrigins");
    String user = null;
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final MapRecord mapRecord = workflowService.getMapRecord(mapRecordId);

      if (mapRecord == null) {
        throw new LocalException("The map record " + mapRecordId
            + " no longer exists, it has probably moved on in the workflow.");
      }

      // authorize call
      user = authorizeProject(mapRecord.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "get origin records for conflict",
          securityService);

      // get the project
      MapProject project =
          workflowService.getMapProject(mapRecord.getMapProjectId());

      // find the tracking record for this map record
      TrackingRecord trackingRecord =
          workflowService.getTrackingRecordForMapProjectAndConcept(project,
              mapRecord.getConceptId());

      if (trackingRecord == null) {
        throw new LocalException(
            "Tracking record is unexpectedly missing for this project/concept");
      }
      // instantiate workflow handler for this tracking record
      WorkflowPathHandler handler = workflowService
          .getWorkflowPathHandler(trackingRecord.getWorkflowPath().toString());

      // get the origin map records
      MapRecordList mapRecords =
          handler.getOriginMapRecordsForMapRecord(mapRecord, workflowService);

      return mapRecords;

      // return
      // mappingService.getOriginMapRecordsForConflict(mapRecordId);

    } catch (Exception e) {
      handleException(e, "trying to get origin records for user review", user,
          "", mapRecordId.toString());
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  // //////////////////////////////////////////////
  // Map Record Validation and Compare Services
  // //////////////////////////////////////////////

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#validateMapRecord(org.ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
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
  @ApiOperation(value = "Validate a map record", notes = "Performs validation checks on a map record and returns the validation results.", response = MapRecordJpa.class)
  public ValidationResult validateMapRecord(
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping): /validation/record/validate for map record id = "
            + mapRecord.getId().toString());

    // get the map project for this record

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "validate map record",
          securityService);

      final MapProject mapProject =
          mappingService.getMapProject(mapRecord.getMapProjectId());
      final ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      // Make sure map entries are sorted by by mapGroup/mapPriority
      Collections.sort(mapRecord.getMapEntries(),
          new TerminologyUtility.MapEntryComparator());

      final ValidationResult validationResult =
          algorithmHandler.validateRecord(mapRecord);
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#compareMapRecords(java.lang.Long, java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/validation/record/id/{recordId1}/record/id/{recordId2}/compare")
  @ApiOperation(value = "Compare two map records", notes = "Compares two map records and returns the validation results.", response = ValidationResultJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ValidationResult compareMapRecords(
    @ApiParam(value = "Map record id, e.g. 28123", required = true) @PathParam("recordId1") Long mapRecordId1,
    @ApiParam(value = "Map record id, e.g. 28124", required = true) @PathParam("recordId2") Long mapRecordId2,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /validation/record/id/" + mapRecordId1
            + "record/id/" + mapRecordId1 + "/compare");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {

      final MapRecord mapRecord1 = mappingService.getMapRecord(mapRecordId1);
      final MapRecord mapRecord2 = mappingService.getMapRecord(mapRecordId2);

      // authorize call
      user = authorizeProject(mapRecord1.getMapProjectId(), authToken,
          MapUserRole.VIEWER, "compareMapRecords", securityService);

      final MapProject mapProject =
          mappingService.getMapProject(mapRecord1.getMapProjectId());
      final ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      final ValidationResult validationResult =
          algorithmHandler.compareMapRecords(mapRecord1, mapRecord2);

      return validationResult;

    } catch (Exception e) {
      handleException(e, "trying to compare map records", user, "",
          mapRecordId1.toString());
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#isTargetCodeValid(java.lang.Long, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/project/id/{mapProjectId}/concept/isValid")
  @ApiOperation(value = "Indicate whether a target code is valid", notes = "Gets either a valid concept corresponding to the id, or returns null if not valid.", response = TreePositionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Concept isTargetCodeValid(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @QueryParam("terminologyId") String terminologyId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/id/" + mapProjectId
            + "/concept/isValid " + terminologyId);

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.VIEWER,
          "is target code valid", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);
      final ProjectSpecificAlgorithmHandler algorithmHandler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      boolean isValid = algorithmHandler.isTargetCodeValid(terminologyId);

      if (isValid) {
        final Concept c = contentService.getConcept(terminologyId,
            mapProject.getDestinationTerminology(),
            mapProject.getDestinationTerminologyVersion());
        // Empty descriptions/relationships
        c.setDescriptions(new HashSet<Description>());
        c.setRelationships(new HashSet<Relationship>());
        return c;
      } else {
        return null;
      }

    } catch (Exception e) {
      handleException(e, "trying to compare map records", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      contentService.close();
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#uploadMappingHandbookFile(java.io.InputStream, com.sun.jersey.core.header.FormDataContentDisposition, java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/upload/{mapProjectId}")
  // Swagger does not support this
  @ApiOperation(value = "Upload a mapping handbook file for a project", notes = "Uploads a mapping handbook file for the specified project.")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadMappingHandbookFile(
    @FormDataParam("file") InputStream fileInputStream,
    @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
    @PathParam("mapProjectId") Long mapProjectId,
    @HeaderParam("Authorization") String authToken) throws Exception {

    String user = null;
    MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "upload handbook file", securityService);

      // get destination directory for uploaded file
      final Properties config = ConfigUtility.getConfigProperties();
      final String docDir =
          config.getProperty("map.principle.source.document.dir");

      // make sure docDir ends with /doc - validation
      // mkdirs with project id in both dir and archiveDir
      final File dir = new File(docDir);
      final File archiveDir = new File(docDir + "/archive");

      final File projectDir = new File(docDir, mapProjectId.toString());
      projectDir.mkdir();
      final File archiveProjectDir =
          new File(archiveDir, mapProjectId.toString());
      archiveProjectDir.mkdir();

      // compose the name of the stored file
      final MapProject mapProject =
          mappingService.getMapProject(new Long(mapProjectId));
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
      final File file =
          new File(dir, mapProjectId + "/" + fileName + extension);
      final File archiveFile = new File(archiveDir,
          mapProjectId + "/" + fileName + "." + date + extension);

      // save the file to the server
      saveFile(fileInputStream, file.getAbsolutePath());
      copyFile(file, archiveFile);

      // update project
      mapProject.setMapPrincipleSourceDocument(
          mapProjectId + "/" + fileName + extension);
      updateMapProject((MapProjectJpa) mapProject, authToken);

      return Response.status(200).entity(mapProjectId + "/" + file.getName())
          .build();
    } catch (Exception e) {
      handleException(e, "trying to upload a file", user,
          mapProjectId.toString(), "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getMapProjectMetadata(java.lang.String)
   */
  @Override
  @GET
  @Path("/mapProject/metadata")
  @ApiOperation(value = "Get metadata for map projects", notes = "Gets the key-value pairs representing all metadata for the map projects.", response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getMapProjectMetadata(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /mapProject/metadata");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get map project metadata", securityService);

      // call jpa service and get complex map return type
      final Map<String, Map<String, String>> mapOfMaps =
          mappingService.getMapProjectMetadata();

      // convert complex map to KeyValuePair objects for easy
      // transformation to
      // XML/JSON
      final KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      for (final Map.Entry<String, Map<String, String>> entry : mapOfMaps
          .entrySet()) {
        final String metadataType = entry.getKey();
        final Map<String, String> metadataPairs = entry.getValue();
        final KeyValuePairList keyValuePairList = new KeyValuePairList();
        keyValuePairList.setName(metadataType);
        for (final Map.Entry<String, String> pairEntry : metadataPairs
            .entrySet()) {
          final KeyValuePair keyValuePair = new KeyValuePair(
              pairEntry.getKey().toString(), pairEntry.getValue());
          keyValuePairList.addKeyValuePair(keyValuePair);
        }
        keyValuePairLists.addKeyValuePairList(keyValuePairList);
      }
      return keyValuePairLists;
    } catch (Exception e) {
      handleException(e, "trying to get the map project metadata", user, "",
          "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getAllTerminologyNotes(java.lang.Long, java.lang.String)
   */
  @Override
  @GET
  @Path("/mapProject/{mapProjectId}/notes")
  @ApiOperation(value = "Get metadata for map projects", notes = "Gets the key-value pairs representing all metadata for the map projects.", response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairList getAllTerminologyNotes(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /mapProject/" + mapProjectId + "/notes");

    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get all terminology notes", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);
      final ProjectSpecificAlgorithmHandler handler =
          mappingService.getProjectSpecificAlgorithmHandler(mapProject);
      final KeyValuePairList list = new KeyValuePairList();
      for (final Map.Entry<String, String> entry : handler
          .getAllTerminologyNotes().entrySet()) {
        final KeyValuePair pair =
            new KeyValuePair(entry.getKey(), entry.getValue());
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#computeDefaultPreferredNames(java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/names")
  @ApiOperation(value = "Compute default preferred names for a map project.", notes = "Recomputes default preferred names for the specified map project.")
  public void computeDefaultPreferredNames(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Workflow): /project/id/" + mapProjectId.toString()
            + "/names");

    String user = null;
    String project = "";

    final MappingService mappingService = new MappingServiceJpa();
    ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "compute names", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);
      final String terminology = mapProject.getSourceTerminology();
      final String version = mapProject.getSourceTerminologyVersion();
      final Long dpnTypeId = 900000000000003001L;
      final Long dpnrefsetId = 900000000000509007L;
      final Long dpnAcceptabilityId = 900000000000548007L;
      // FROM ComputeDefaultPreferredNameMojo
      Logger.getLogger(getClass())
          .info("Starting comput default preferred names");
      Logger.getLogger(getClass()).info("  terminology = " + terminology);
      Logger.getLogger(getClass()).info("  terminologyVersion = " + version);

      contentService.setTransactionPerOperation(false);
      contentService.beginTransaction();

      // Setup vars
      int dpnNotFoundCt = 0;
      int dpnFoundCt = 0;
      int dpnSkippedCt = 0;
      int objectCt = 0;

      final ConceptList concepts =
          contentService.getAllConcepts(terminology, version);
      contentService.clear();

      // Iterate over concepts
      for (final Concept concept2 : concepts.getConcepts()) {
        final Concept concept = contentService.getConcept(concept2.getId());
        // Skip if inactive
        if (!concept.isActive()) {
          dpnSkippedCt++;
          continue;
        }
        Logger.getLogger(getClass())
            .debug("  Concept " + concept.getTerminologyId());
        boolean dpnFound = false;
        // Iterate over descriptions
        for (Description description : concept.getDescriptions()) {

          // If active andn preferred type
          if (description.isActive()
              && description.getTypeId().equals(dpnTypeId)) {
            // Iterate over language refset members
            for (LanguageRefSetMember language : description
                .getLanguageRefSetMembers()) {
              // If prefrred and has correct refset
              if (new Long(language.getRefSetId()).equals(dpnrefsetId)
                  && language.isActive()
                  && language.getAcceptabilityId().equals(dpnAcceptabilityId)) {
                // print warning for multiple names found
                if (dpnFound) {
                  Logger.getLogger(getClass()).warn(
                      "Multiple default preferred names found for concept "
                          + concept.getTerminologyId());
                  Logger.getLogger(getClass()).warn(
                      "  " + "Existing: " + concept.getDefaultPreferredName());
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
          Logger.getLogger(getClass())
              .warn("Could not find defaultPreferredName for concept "
                  + concept.getTerminologyId());
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
  private void saveFile(InputStream uploadedInputStream,
    String serverLocation) {
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#beginReleaseForMapProject(java.lang.String, java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/release/{effectiveTime}/begin")
  @ApiOperation(value = "Begin release for map project", notes = "Generates release validation report for map project")
  public void beginReleaseForMapProject(
    @ApiParam(value = "Effective Time, e.g. 20170131", required = true) @PathParam("effectiveTime") String effectiveTime,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/id/" + mapProjectId.toString()
            + "/release/begin");

    String user = null;
    String project = "";

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "begin release", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // create release handler in test mode
      ReleaseHandler handler = new ReleaseHandlerJpa(true);
      handler.setEffectiveTime(effectiveTime);
      handler.setMapProject(mapProject);
      handler.beginRelease();
    } catch (Exception e) {
      handleException(e, "trying to begin release", user, project, "");
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
  private String getReleaseDirectoryPath(MapProject mapProject,
    String effectiveTime) throws Exception {
    String rootPath = ConfigUtility.getConfigProperties()
        .getProperty("map.principle.source.document.dir");
    if (!rootPath.endsWith("/") && !rootPath.endsWith("\\")) {
      rootPath += "/";
    }

    String path = rootPath + "release/" + mapProject.getSourceTerminology()
        + "_to_" + mapProject.getDestinationTerminology() + "/" + effectiveTime
        + "/";
    path.replaceAll("\\s", "");
    return path;

  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#processReleaseForMapProject(java.lang.String, java.lang.String, java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/release/{effectiveTime}/module/id/{moduleId}/process")
  @ApiOperation(value = "Process release for map project", notes = "Processes release and creates release files for map project")
  public void processReleaseForMapProject(
    @ApiParam(value = "Module Id, e.g. 20170131", required = false) @PathParam("moduleId") String moduleId,
    @ApiParam(value = "Effective Time, e.g. 20170131", required = true) @PathParam("effectiveTime") String effectiveTime,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Current", required = false) @QueryParam("current") boolean current,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/id/" + mapProjectId.toString()
            + "/release/" + effectiveTime + "/module/id/" + moduleId
            + "/process " + current);

    String user = null;
    String project = "";

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "process release", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);
      
      if (moduleId.equals("null") || moduleId.equals("")) {
        moduleId = mapProject.getModuleId();
        if (moduleId == null || moduleId.equals("")) {
          throw new Exception("The module id must be set on the project details page.");
        }
      }

      // create release handler in test mode
      ReleaseHandler handler = new ReleaseHandlerJpa(true);
      handler.setEffectiveTime(current ? "99999999" : effectiveTime);
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
        outputDir = this.getReleaseDirectoryPath(mapProject, "current");
      } else {
        outputDir = this.getReleaseDirectoryPath(mapProject, effectiveTime);
      }
      handler.setOutputDir(outputDir);

      File file = new File(outputDir);
      System.out.println("  exists: " + file.exists());
      // make output directory if does not exist
      if (!file.exists()) {
        System.out.println("  making directory: " + file.getAbsolutePath());
        file.mkdirs();
      }

      // process release
      handler.processRelease();
    } catch (Exception e) {
      handleException(e, "trying to process release", user, project, "");
    } finally {

      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#finishReleaseForMapProject(boolean, java.lang.Long, java.lang.String, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/release/{effectiveTime}/finish")
  @ApiOperation(value = "Finish release for map project", notes = "Finishes release for map project from release files")
  public void finishReleaseForMapProject(
    @ApiParam(value = "Preview mode", required = false) @QueryParam("test") boolean testModeFlag,
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Effective Time, e.g. 20170131", required = true) @PathParam("effectiveTime") String effectiveTime,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/id/" + mapProjectId.toString()
            + "/release/begin");

    String user = null;
    String project = "";

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "compute names", securityService);

      final MapProject mapProject = mappingService.getMapProject(mapProjectId);

      // create release handler NOT in test mode
      // TODO Decide whether to remove the out of scope map records,
      // currently thinking NO
      // but we do want to replace the simple/complex/extended maps
      ReleaseHandler handler = new ReleaseHandlerJpa(testModeFlag);
      handler.setMapProject(mapProject);
      handler.setEffectiveTime(effectiveTime);
      String releaseDirPath =
          this.getReleaseDirectoryPath(mapProject, effectiveTime);

      // check for existence of file
      // TODO This computation should be moved into the ReleaseHandler and
      // made handler-specific
      String relPath = "/der2_" + handler.getPatternForType(mapProject)
          + mapProject.getMapRefsetPattern() + "ActiveSnapshot_INT_"
          + effectiveTime + ".txt";
      String mapFilePath = releaseDirPath + relPath;

      File file = new File(mapFilePath);
      if (!file.exists()) {
        throw new LocalException("Release file " + mapFilePath + " not found");
      }
      handler.setInputFile(mapFilePath);

      // process release
      handler.finishRelease();
    } catch (Exception e) {
      handleException(e, "trying to finish release", user, project, "");
    } finally {

      mappingService.close();
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#startEditingCycleForMapProject(java.lang.Long, java.lang.String)
   */
  @Override
  @POST
  @Path("/project/id/{id:[0-9][0-9]*}/release/startEditing")
  @ApiOperation(value = "Start editing cycle for map project", notes = "Start editing cycle for map project")
  public void startEditingCycleForMapProject(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Mapping): /project/id/" + mapProjectId.toString()
            + "/release/startEditing");

    String user = null;
    String project = "";

    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(mapProjectId, authToken, MapUserRole.LEAD,
          "start editing cycle", securityService);

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
  
  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#createJiraIssue(java.lang.String, java.lang.String, java.lang.String, org.ihtsdo.otf.mapping.jpa.MapRecordJpa, java.lang.String)
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
  @ApiOperation(value = "Create a jira ticket for the content author.", notes = "Create a jira ticket for the content author.")
  public void createJiraIssue(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Concept author username", required = true) @PathParam("conceptAuthor") String conceptAuthor,
    @ApiParam(value = "Message text", required = false) @QueryParam("messageText") String messageText,
    @ApiParam(value = "Map record, in JSON or XML POST data", required = true) MapRecordJpa mapRecord,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(WorkflowServiceRestImpl.class)
        .info("RESTful call (Mapping): /jira/" + conceptId.toString() + "/"
            + conceptAuthor );
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
      mapRecordContents.append("Concept:").append(mapRecord.getConceptId())
          .append(" ").append(mapRecord.getConceptName()).append("\\\\\\\\");
      mapRecordContents.append("Map Entries").append("\\\\\\\\");
      final Comparator<MapEntry> entriesComparator =
          new Comparator<MapEntry>() {
            @Override
            public int compare(MapEntry o1, MapEntry o2) {
              Integer mapGroup1 = new Integer(o1.getMapGroup());
              Integer mapGroup2 = new Integer(o2.getMapGroup());
              if (mapGroup1 == mapGroup2) {
                Integer mapPriority1 = new Integer(o1.getMapPriority());
                Integer mapPriority2 = new Integer(o2.getMapPriority());
                return mapPriority1.compareTo(mapPriority2);
              }
              return mapGroup1.compareTo(mapGroup2);
            }
          };

      // sort the map entries
      Collections.sort(mapRecord.getMapEntries(), entriesComparator);
      for (MapEntry entry : mapRecord.getMapEntries()) {
        mapRecordContents
            .append(entry.getMapGroup() + " / " + entry.getMapPriority())
            .append("  ");
        mapRecordContents.append(entry.getTargetId()).append(" ")
            .append(entry.getTargetName()).append("\\\\\\\\");
        mapRecordContents.append(entry.getRule()).append("\\\\\\\\");
        for (MapAdvice mapAdvice : entry.getMapAdvices()) {
          mapRecordContents.append(mapAdvice.getName()).append("\\\\\\\\");
        }
        mapRecordContents.append(entry.getMapRelation().getName())
            .append("\\\\\\\\");
      }
		    /*if (mapRecord.getMapNotes().size() > 0) {
		    	mapRecordContents.append("Notes").append("\\\\\\\\");
		    }
		    for (MapNote note : mapRecord.getMapNotes()) {
		    	mapRecordContents.append(note.getUser().getName()).append(" on ").append(note.getTimestamp()).append("\\\\\\\\");
		    	mapRecordContents.append(note.getNote().replaceAll("<br>", "\\\\\\\\\\\\\\\\").replaceAll("\\<.*?\\>", "").replaceAll("nbsp;", " ")).append("\\\\\\\\");
		    }*/
		    
		    // if test project, override author and user
            if (jiraProject.equals("MTFP") || jiraProject.equals("MFTP")) {
              conceptAuthor = "dshapiro";
              authToken = "dshapiro";
              jiraProject = "MTFP";
            }
      
            // create the issue object to send to JIRA Rest API
		    String data = "{"
            + "\"fields\": {"
                + "\"project\":"
                    + "{"
                    +    "\"key\": \"" + jiraProject + "\""
                    + "},"
                + "\"summary\": \"Mapping Feedback on " + conceptId + "\","
                + "\"assignee\": {"
                        + "\"name\": \"" + conceptAuthor + "\""
                    + "},"
                 + "\"reporter\": {"
                        + "\"name\": \"" + authToken + "\""
                    + "},"
                + "\"description\": \"" + messageText.replaceAll("\n", "\\\\\\\\\\\\\\\\").replaceAll("\\<.*?\\>", "") + "\\\\\\\\" + mapRecordContents.toString() + "\","
                + "\"issuetype\": {"
                        + "\"name\": \"Task\""
                    + "}"
                + "}"
            + "}";
      Logger.getLogger(MappingServiceRestImpl.class)
          .info("RESTful call (Mapping): /jira/  \n" + data);

      final Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
          .header("Authorization", jiraAuthHeader)
          .accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(data));
      int statusCode = response.getStatus();

      if (statusCode == 401) {
        this.handleException(
            new AuthenticationException("Invalid Username or Password"),
            "Invalid Username or Password", authToken, "", "");
      } else if (statusCode == 403) {
        this.handleException(new AuthenticationException("Forbidden"),
            "Forbidden", authToken, "", "");
      } else if (statusCode == 200 || statusCode == 201) {
        Logger.getLogger(MappingServiceRestImpl.class)
            .info("Ticket Created successfully");
      } else {
        this.handleException(
            new AuthenticationException("Http Error : " + statusCode),
            "Http Error : " + statusCode, authToken, "", "");
        Logger.getLogger(MappingServiceRestImpl.class)
            .info("Http Error : " + statusCode);
      }
      
      System.out.println(response.readEntity(String.class));
      Logger.getLogger(MappingServiceRestImpl.class).info(response.readEntity(String.class));

    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getConceptAuthors(java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/authors/{conceptId}")
  @ApiOperation(value = "Gets authors for this concept", notes = "Gets a list of all content authors from the authoring tool.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getConceptAuthors(
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /authors/" + conceptId);

    String user = null;
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "gets authors for this concept", securityService);

      final Properties config = ConfigUtility.getConfigProperties();
      final String authoringAuthHeader =
          config.getProperty("authoring.authHeader");
      final String authoringUrl = config.getProperty("authoring.defaultUrl");

      if (authoringAuthHeader == null || authoringUrl == null) {
        this.handleException(
            new Exception(
                "retrieve concept authors. Authoring properties must be in configuration file"),
            "retrieve concept authors. Authoring properties must be in configuration file",
            "", "", "");
      }

      final Client client = ClientBuilder.newClient();
      final WebTarget target = client.target(authoringUrl
          + "/traceability-service/activities?conceptId=" + conceptId);

      final Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
          .header("Authorization", authoringAuthHeader)
          .accept("application/json").get();
      int statusCode = response.getStatus();

      if (statusCode == 401) {
        this.handleException(
            new AuthenticationException("Invalid Username or Password"),
            "Invalid Username or Password", authToken, "", "");
      } else if (statusCode == 403) {
        this.handleException(new AuthenticationException("Forbidden"),
            "Forbidden", authToken, "", "");
      } else if (statusCode == 200 || statusCode == 201) {
        Logger.getLogger(MappingServiceRestImpl.class)
            .info("Traceability report retrieved successfully");
      } else {
        this.handleException(
            new AuthenticationException("Http Error : " + statusCode),
            "Http Error : " + statusCode, authToken, "", "");
        Logger.getLogger(MappingServiceRestImpl.class)
            .info("Http Error : " + statusCode);
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
            || !singleContent.getJSONObject("highestPromotedBranch")
                .getString("branchPath").contains("MAIN")) {
          continue;
        }
        String userName =
            singleContent.getJSONObject("user").getString("username");
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
      this.handleException(e, "trying to get authors for this concept", user,
          "", "");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.MappingServiceRest#getConceptAuthoringChanges(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/changes/{projectId}/{conceptId}")
  @ApiOperation(value = "Gets authoring history for this concept", notes = "Gets a list of all editing changes made to MAIN from the authoring tool.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getConceptAuthoringChanges(
    @ApiParam(value = "Project id", required = true) @PathParam("projectId") String projectId,
    @ApiParam(value = "Concept id", required = true) @PathParam("conceptId") String conceptId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class).info(
        "RESTful call (Mapping):  /changes/" + projectId + "/" + conceptId);

    final MappingService mappingService = new MappingServiceJpa();
    String user = null;
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get concept authoring changes", securityService);

      final MapProject mapProject =
          mappingService.getMapProject(new Long(projectId).longValue());

      final Date editingCycleBeginDate = mapProject.getEditingCycleBeginDate();
      Logger.getLogger(MappingServiceRestImpl.class)
          .info("editingCycleBeginDate:" + editingCycleBeginDate.toString());

      final Properties config = ConfigUtility.getConfigProperties();
      final String authoringAuthHeader =
          config.getProperty("authoring.authHeader");
      final String authoringUrl = config.getProperty("authoring.defaultUrl");

      if (authoringAuthHeader == null || authoringUrl == null) {
        this.handleException(
            new Exception(
                "retrieve authoring history. Authoring properties must be in configuration file"),
            "retrieve authoring history. Authoring properties must be in configuration file",
            "", "", "");
      }

      final Client client = ClientBuilder.newClient();
      final WebTarget target = client.target(authoringUrl
          + "/traceability-service/activities?conceptId=" + conceptId);

      final Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
          .header("Authorization", authoringAuthHeader)
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
        Logger.getLogger(MappingServiceRestImpl.class)
            .info("Http Error : " + statusCode);
      }

      // Parse to get the editing changes that were promoted to MAIN
      SearchResultList searchResultList = new SearchResultListJpa();
      String jsonText = response.readEntity(String.class);
      JSONObject jsonObject = new JSONObject(jsonText);
      JSONArray array = jsonObject.getJSONArray("content");
      for (int i = 0; i < array.length(); i++) {
        JSONObject singleContent = array.getJSONObject(i);
        if (singleContent.getString("highestPromotedBranch") == null
            || !singleContent.getJSONObject("highestPromotedBranch")
                .getString("branchPath").contains("MAIN")) {
          continue;
        }
        String userName =
            singleContent.getJSONObject("user").getString("username");
        String commitDateString = singleContent.getString("commitDate");
        final SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
        Date commitDate = dt.parse(commitDateString);

        // don't include entries that occurred before the editing cycle begin
        // date
        if (commitDate.before(editingCycleBeginDate)) {
          continue;
        }
        JSONArray conceptChangesArray =
            singleContent.getJSONArray("conceptChanges");
        for (int j = 0; j < conceptChangesArray.length(); j++) {
          JSONObject conceptChange = conceptChangesArray.getJSONObject(j);
          String cptId = conceptChange.getString("conceptId");
          JSONArray componentChangesArray =
              conceptChange.getJSONArray("componentChanges");
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
            searchResult.setValue2(cptId + ":" + componentId + ":"
                + componentType + ":" + componentSubType + ":" + changeType);
            searchResultList.addSearchResult(searchResult);
          }
        }
      }
      searchResultList
          .setTotalCount(searchResultList.getSearchResults().size());
      Logger.getLogger(MappingServiceRestImpl.class)
          .info("Traceability report contains "
              + searchResultList.getTotalCount() + " entries.");
      return searchResultList;

    } catch (Exception e) {
      this.handleException(e, "trying to get concept authoring changes", user,
          "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }

  }
  

  @POST
  @Path("/compare/files/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Compares two map files", notes = "Compares two files and saves the comparison report to the file system.", response = InputStream.class)
  @Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces("application/vnd.ms-excel")
  public InputStream compareMapFiles(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "File paths, in JSON or XML POST data", required = true) List<String> files,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Mapping): /compare/files" + mapProjectId + " " + files.get(0) + " " + files.get(1));

    String user = "";
    final MetadataService metadataService = new MetadataServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize
      user = authorizeApp(authToken, MapUserRole.VIEWER, "compare map files", securityService);
            
      /*String olderInputFile1 = "C:\\Users\\dshap\\Downloads\\MappingTestFiles\\20170131Final\\der2_iisssccRefset_ExtendedMapSnapshot_INT_20170131.txt";
      String newerInputFile2 = "C:\\Users\\dshap\\Downloads\\MappingTestFiles\\20170731Final\\der2_iisssccRefset_ExtendedMapSnapshot_INT_20170731.txt";
      */
      //String olderInputFile1 = "C:\\Users\\dshap\\Downloads\\MappingTestFiles\\20170731Alpha\\xder2_sRefset_SimpleMapSnapshot_INT_20170731.txt";
      /*String newerInputFile2 = "C:\\Users\\dshap\\Downloads\\MappingTestFiles\\20180131Alpha\\xder2_sRefset_SimpleMapSnapshot_INT_20180131.txt";
      */
      /*String olderInputFile1 = "C:\\Users\\dshap\\Downloads\\MappingTestFiles\\20170731Alpha\\xder2_sRefset_SimpleMapDelta_INT_20170731.txt";
      String newerInputFile2 = "C:\\Users\\dshap\\Downloads\\MappingTestFiles\\20170731Beta\\xder2_sRefset_SimpleMapDelta_INT_20170731.txt";
      */
      /*String olderInputFile1 = "C:\\Users\\dshap\\Downloads\\MappingTestFiles\\20170731Alpha\\xder2_iisssccRefset_ExtendedMapDelta_INT_20170731.txt";
      String newerInputFile2 = "C:\\Users\\dshap\\Downloads\\MappingTestFiles\\20170731Beta\\xder2_iisssccRefset_ExtendedMapDelta_INT_20170731.txt";
       */
      String olderInputFile1 = files.get(0);
      String newerInputFile2 = files.get(1);
      
      AmazonS3 s3Client = null;
      try {
        s3Client = AmazonS3ClientBuilder.standard()
            .withCredentials(new InstanceProfileCredentialsProvider(false))
            .build();
      } catch (Exception e) {
        final Properties config = ConfigUtility.getConfigProperties();
        final String accessKey = config.getProperty("aws.access.key");
        final String secretAccessKey =
            config.getProperty("aws.secret.access.key");
        BasicAWSCredentials awsCreds =
            new BasicAWSCredentials(accessKey, secretAccessKey);
        s3Client = AmazonS3ClientBuilder.standard().withRegion("us-east-1")
            .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
            .build();
      }
     
      // stream first file from aws
      S3Object file1 = s3Client.getObject(
          new GetObjectRequest("release-ihtsdo-prod-published", olderInputFile1));
      InputStream objectData1 = file1.getObjectContent();
      
      // open second file either from aws or current release on file system
      S3Object file2 = null;
      InputStream objectData2 = null;
      // stream second/later file from aws
      if (!newerInputFile2.contains("99999999")) {
        file2 = s3Client.getObject(
          new GetObjectRequest("release-ihtsdo-prod-published", newerInputFile2));
          objectData2 = file2.getObjectContent();
      // comparing to current release file saved on file system
      } else {
        MapProject mapProject = mappingService.getMapProject(mapProjectId);
        final File projectDir = new File(this.getReleaseDirectoryPath(mapProject, "current"));
        String currentReleaseFile = new File(projectDir, newerInputFile2).getAbsolutePath();
        objectData2 = new FileInputStream(currentReleaseFile);
      }

      
      InputStream reportInputStream = null;
      StringBuffer reportName = new StringBuffer();
      if (olderInputFile1.contains("Full") || newerInputFile2.contains("Full")) {
        throw new LocalException("Full files cannot be compared with this tool.");
      }
      
      // compare extended map files and compose report name
      if (olderInputFile1.contains("ExtendedMap") && newerInputFile2.contains("ExtendedMap")) {
        reportInputStream = compareExtendedMapFiles(objectData1, objectData2);
        reportName.append(olderInputFile1.substring(olderInputFile1.lastIndexOf("Extended"), olderInputFile1.lastIndexOf('.')));
        if (olderInputFile1.toLowerCase().contains("alpha")) {reportName.append("_ALPHA");}
        if (olderInputFile1.toLowerCase().contains("beta")) {reportName.append("_BETA");}
        reportName.append("_");
        reportName.append(newerInputFile2.substring(newerInputFile2.lastIndexOf("Extended"), newerInputFile2.lastIndexOf('.')));
        if (newerInputFile2.toLowerCase().contains("alpha")) {reportName.append("_ALPHA");}
        if (newerInputFile2.toLowerCase().contains("beta")) {reportName.append("_BETA");}
        reportName.append(".xls");
        
      // compare simple map files and compose report name
      } else if (olderInputFile1.contains("SimpleMap") && newerInputFile2.contains("SimpleMap")) {
        reportInputStream = compareSimpleMapFiles(objectData1, objectData2);
        reportName.append(olderInputFile1.substring(olderInputFile1.lastIndexOf("Simple"), olderInputFile1.lastIndexOf('.')));
        if (olderInputFile1.toLowerCase().contains("alpha")) {reportName.append("_ALPHA");}
        if (olderInputFile1.toLowerCase().contains("beta")) {reportName.append("_BETA");}
        reportName.append("_");
        reportName.append(newerInputFile2.substring(newerInputFile2.lastIndexOf("Simple"), newerInputFile2.lastIndexOf('.')));
        if (newerInputFile2.toLowerCase().contains("alpha")) {reportName.append("_ALPHA");}
        if (newerInputFile2.toLowerCase().contains("beta")) {reportName.append("_BETA");}
        reportName.append(".xls");
      } 

      // create destination directory for saved report
      final Properties config = ConfigUtility.getConfigProperties();      
      final String docDir =
          config.getProperty("map.principle.source.document.dir");
      
      final File projectDir = new File(docDir, mapProjectId.toString());
      if (!projectDir.exists()) {
        projectDir.mkdir();
      }
      
      final File reportsDir = new File(projectDir, "reports");
      if (!reportsDir.exists()) {
        reportsDir.mkdir();
      }

      final File file =
          new File(reportsDir, reportName.toString());

      // save the file to the server
      saveFile(reportInputStream, file.getAbsolutePath());
      

      objectData1.close();
      objectData2.close();
      
      return reportInputStream;
      
    } catch (Exception e) {
      handleException(e,
          "trying to compare map files", user, "", "");
      return null;
    } finally {
      metadataService.close();
      mappingService.close();
      securityService.close();
    }
  }
  
  private InputStream compareExtendedMapFiles(InputStream data1, InputStream data2) throws Exception {
    // map to list of records that have been updated (sorted by key)
    TreeMap<String, String> updatedList = new TreeMap<>();
    // map to list of records that are new in the second file
    Map<String, String> newList = new LinkedHashMap<>();
    // map to list of records that have been inactivated in the second file
    Map<String, String> inactivatedList = new LinkedHashMap<>();
    // map to list of records that have been removed in the second file
    Map<String, String> removedList = new LinkedHashMap<>();
    String line1, line2;
    
    BufferedReader in1 =
        new BufferedReader(new InputStreamReader(data1, "UTF-8"));
    in1.mark(100000000);
    BufferedReader in2 =
        new BufferedReader(new InputStreamReader(data2, "UTF-8"));
    
    
      int noChangeCount = 0;
      Map<String, String> key1Map = new HashMap<>();
      Map<String, String> key2Map = new HashMap<>();
      
      // populate key1Map with key1 and line1 (no UUID)
      while((line1 = in1.readLine()) != null) {
        String tokens1[] = line1.split("\t");
        String key1 = tokens1[5] + ":" + tokens1[4] + ":" + tokens1[6] + ":" + tokens1[7];
        key1Map.put(key1, line1.substring(line1.indexOf("\t")));
      }
      
      while ((line2 = in2.readLine()) != null) {
        // populate key2Map with key2 and line2 (no UUID)
        String tokens2[] = line2.split("\t");
        String key2 = tokens2[5] + ":" + tokens2[4] + ":" + tokens2[6] + ":" + tokens2[7];
        key2Map.put(key2, line2.substring(line2.indexOf("\t")));
        
        // if key1Map has key2, this record is not new - either it hasn't changed or it has been updated
        if (key1Map.containsKey(key2)) {
          String line1Sub = key1Map.get(key2);
          String line2Sub = line2.substring(line2.indexOf("\t"));
          if (line1Sub.equals(line2Sub)) {
            // nothing has changed
            noChangeCount++;
          } else {         
            String[] line1SubTokens = line1Sub.split("\t");
            String[] line2SubTokens = line2Sub.split("\t");
            // check if everything other than the active flag and the effective time matches
            if (line1SubTokens[2].equals("1") && line2SubTokens[2].equals("0")) {
              boolean inactive = true;
              for (int i = 3; i < line1SubTokens.length; i++) {               
                  if (!line1SubTokens[i].equals(line2SubTokens[i])) {            
                    inactive = false;
                  } 
              }
              if (inactive) {
                inactivatedList.put(key2, line2);
              }
            }

            // something updated
            // only add to list if records are active
            if (line1SubTokens[2].equals("1") && line2SubTokens[2].equals("1")) {
              updatedList.put(key2, line1Sub + "\t" + line2Sub);
            }
          }
        // found key2 that is not in first file, it is new
        } else {
          newList.put(key2, line2);
        }
      }
      
      in1.reset();
      
      // determine records that were removed
      while ((line1 = in1.readLine()) != null) {
        String tokens1[] = line1.split("\t");
        String key1 = tokens1[5] + ":" + tokens1[4] + ":" + tokens1[6] + ":" + tokens1[7];
        
        // if key2Map doesn't have key1, this record has been removed
        if (!key2Map.containsKey(key1)) {
          removedList.put(key1, line1);
        }
      }
      
      // log statements
      Logger.getLogger(MappingServiceRest.class).info(
          "new List count:" + newList.size());
      Logger.getLogger(MappingServiceRest.class).info(
          "inactivated List count:" + inactivatedList.size());
      Logger.getLogger(MappingServiceRest.class).info(
          "updated List count:" + updatedList.size());
      Logger.getLogger(MappingServiceRest.class).info(
          "removed List count:" + removedList.size());
      Logger.getLogger(MappingServiceRest.class).info(
          "no change count:" + noChangeCount);
      
      in1.close();
      in2.close();
      // produce Excel report file
      final ExportReportHandler handler = new ExportReportHandler();
      return handler.exportExtendedFileComparisonReport(updatedList, newList, inactivatedList, removedList);
  }
  
  private InputStream compareSimpleMapFiles(InputStream data1, InputStream data2) throws Exception {
    
    // map to list of records that have been updated (sorted by key)
    TreeMap<String, String> updatedList = new TreeMap<>();
    // map to list of records that are new in the second file
    Map<String, String> newList = new LinkedHashMap<>();
    // map to list of records that have been inactivated in the second file
    Map<String, String> inactivatedList = new LinkedHashMap<>();
    // map to list of records that have been removed in the second file
    Map<String, String> removedList = new LinkedHashMap<>();
    String line1, line2;
  
    BufferedReader in1 =
        new BufferedReader(new InputStreamReader(data1, "UTF-8"));
    in1.mark(100000000);
    BufferedReader in2 =
        new BufferedReader(new InputStreamReader(data2, "UTF-8"));
    
      int noChangeCount = 0;
      Map<String, String> key1Map = new HashMap<>();
      Map<String, String> key2Map = new HashMap<>();
      
      // populate key1Map with key1 and line1 (no UUID)
      while((line1 = in1.readLine()) != null) {
        String tokens1[] = line1.split("\t");
        String key1 = tokens1[5] + ":" + tokens1[4];
        key1Map.put(key1, line1.substring(line1.indexOf("\t")));
      }
      
      while ((line2 = in2.readLine()) != null) {
        // populate key2Map with key2 and line2 (no UUID)
        String tokens2[] = line2.split("\t");
        String key2 = tokens2[5] + ":" + tokens2[4];
        key2Map.put(key2, line2.substring(line2.indexOf("\t")));
        
        // if key1Map has key2, this record is not new - either it hasn't changed or it has been updated
        if (key1Map.containsKey(key2)) {
          String line1Sub = key1Map.get(key2);
          String line2Sub = line2.substring(line2.indexOf("\t"));
          if (line1Sub.equals(line2Sub)) {
            // nothing has changed
            noChangeCount++;
          } else {         
            String[] line1SubTokens = line1Sub.split("\t");
            String[] line2SubTokens = line2Sub.split("\t");
            // check if everything other than the active flag and the effective time matches
            if (line1SubTokens[2].equals("1") && line2SubTokens[2].equals("0")) {
              boolean inactive = true;
              for (int i = 3; i < line1SubTokens.length; i++) {               
                  if (!line1SubTokens[i].equals(line2SubTokens[i])) {            
                    inactive = false;
                  } 
              }
              if (inactive) {
                inactivatedList.put(key2, line2);
              }
            }

            // something updated
            // only add to list if records are active
            if (line1SubTokens[2].equals("1") && line2SubTokens[2].equals("1")) {
              updatedList.put(key2, line1Sub + "\t" + line2Sub);
            }
          }
        // found key2 that is not in first file, it is new
        } else {
          newList.put(key2, line2);
        }
      }
      
      in1.reset();     
      
      // determine records that were removed
      while ((line1 = in1.readLine()) != null) {
        String tokens1[] = line1.split("\t");
        String key1 = tokens1[5] + ":" + tokens1[4];
        
        // if key2Map doesn't have key1, this record has been removed
        if (!key2Map.containsKey(key1)) {
          removedList.put(key1, line1);
        }
      }
      
      in1.close();
      in2.close();
      
      // log statements
      Logger.getLogger(MappingServiceRest.class).info(
          "new List count:" + newList.size());
      Logger.getLogger(MappingServiceRest.class).info(
              "inactivated List count:" + inactivatedList.size());
      Logger.getLogger(MappingServiceRest.class).info(
          "updated List count:" + updatedList.size());
      Logger.getLogger(MappingServiceRest.class).info(
          "removed List count:" + removedList.size());
      Logger.getLogger(MappingServiceRest.class).info(
          "no change count:" + noChangeCount);
      
      // produce Excel report file
      final ExportReportHandler handler = new ExportReportHandler();
      return handler.exportSimpleFileComparisonReport(updatedList, newList, inactivatedList, removedList);
  }
        
  @Override
  @GET
  @Path("/release/reports/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get release reports available for a given project", notes = "Gets release reports for a given project.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getReleaseReportList(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /release/reports/ " + mapProjectId);
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get release reports",
          securityService);

      // get directory for release reports
      final Properties config = ConfigUtility.getConfigProperties();
      final String docDir =
          config.getProperty("map.principle.source.document.dir");

      final File projectDir = new File(docDir, mapProjectId.toString() + "/reports");
      File[] reports = projectDir.listFiles();
      
      if (reports == null) {
        return null;
      }
      
      final SearchResultList searchResultList = new SearchResultListJpa();
      for (File report : reports) {
        SearchResult searchResult = new SearchResultJpa();
        searchResult.setValue(report.getName());
        BasicFileAttributes attributes = Files.readAttributes(report.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
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
  @ApiOperation(value = "Get release file indicating current state for a given project", notes = "Get release file indicating current state for a given project.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResult getCurrentReleaseFile(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping): /current/release/ " + mapProjectId);
    String user = null;
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get current release file",
          securityService);

      MapProject mapProject = mappingService.getMapProject(mapProjectId);
      final File projectDir = new File(this.getReleaseDirectoryPath(mapProject, "current"));
      File[] releaseFiles = projectDir.listFiles();

      SearchResult searchResult = new SearchResultJpa();
      if (!projectDir.exists() && releaseFiles == null) {
        return null;
      }
      for (File file : releaseFiles) {
        // filter out human readable and any other release by-products
        if (!file.getName().contains("SimpleMap") && !file.getName().contains("ExtendedMap")){
          continue;
        }
        BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        FileTime creationTime = attributes.creationTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        String dateCreated = df.format(creationTime.toMillis());
        // if this is the most recent, return this file
        if (searchResult.getValue2() == null || 
            dateCreated.compareTo(searchResult.getValue2()) > 0) {
          searchResult.setValue(file.getName());
          searchResult.setValue2(file.getName());
          searchResult.setTerminologyVersion(dateCreated);
          searchResult.setTerminology("current");
        }
      }
      return searchResult;

    } catch (Exception e) {
      handleException(e, "trying to get current release file", user, "", "");
      return null;
    } finally {
      mappingService.close();
      securityService.close();
    }
  }
 
  @Override
  @GET
  @Path("/amazons3/files/{id:[0-9][0-9]*}")
  @ApiOperation(value = "Get list of files from AWS S3 bucket for a given project", notes = "Gets list of files from AWS S3 for a given project.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getFileListFromAmazonS3(
    @ApiParam(value = "Map project id, e.g. 7", required = true) @PathParam("id") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRestImpl.class)
        .info("RESTful call (Mapping):  /amazons3/files/" + mapProjectId);

    final MappingService mappingService = new MappingServiceJpa();
    String user = "";

    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get current release file", securityService);

      final MapProject mapProject =
          mappingService.getMapProject(new Long(mapProjectId).longValue());
      String destinationTerminology = mapProject.getDestinationTerminology();

      String bucketName = "release-ihtsdo-prod-published";
      SearchResultList searchResults = new SearchResultListJpa();

      // Connect to server
      AmazonS3 s3Client =
          AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1)
              .withCredentials(new InstanceProfileCredentialsProvider(false))
              .build();

      // List Buckets
      List<Bucket> buckets = s3Client.listBuckets();
      for (Bucket b : buckets) {
        Logger.getLogger(MappingServiceRestImpl.class)
            .info("Bucket name " + b.getName());
      }

      // Verify Buckets Exists
      if (!s3Client.doesBucketExist(bucketName)) {
        throw new Exception("Cannot find Bucket Name");
      } else {
        Logger.getLogger(MappingServiceRestImpl.class)
            .info("Bucket " + bucketName + " accessed.");
      }

      if (mapProject.getDestinationTerminology().equals("ICPC")
          || mapProject.getDestinationTerminology().equals("GMDN")) {
        // List Files on Bucket "release-ihtsdo-prod-published"
        ObjectListing listing = null;
        if (mapProject.getDestinationTerminology().equals("ICPC")) {
          listing = s3Client.listObjects(bucketName,
              "international/SnomedCT_GPFPICPC2");
        } else {
          listing =
              s3Client.listObjects(bucketName, "international/SnomedCT_GMDN");
        }
        List<S3ObjectSummary> summaries = listing.getObjectSummaries();

        int i = 1;
        for (S3ObjectSummary sum : summaries) {
          String fileName = sum.getKey();
          if ((fileName.contains("ExtendedMap")
              || fileName.contains("SimpleMap")) && !fileName.contains("Full")
              && !fileName.contains("backup")) {
            Logger.getLogger(MappingServiceRestImpl.class)
                .info(mapProject.getDestinationTerminology() + " Summary #" + i++ + " with: " + sum.getKey());
            SearchResult result = new SearchResultJpa();
            String shortName = fileName.substring(fileName.lastIndexOf('/'));
            if (fileName.toLowerCase().contains("alpha")) {
              result.setTerminology("ALPHA");
            } else if (fileName.toLowerCase().contains("beta")) {
              result.setTerminology("BETA");
            } else {
              result.setTerminology("FINAL");
            }
            Matcher m = Pattern.compile("[0-9]{8}").matcher(fileName);
            while (m.find()) {
              result.setTerminologyVersion(m.group());
            }
            result.setValue(shortName);
            result.setValue2(fileName);
            searchResults.addSearchResult(result);
          }
        }
      } else {

        // List All Files on Bucket "release-ihtsdo-prod-published"
        ObjectListing listing =
            s3Client.listObjects(bucketName, "international/");
        List<S3ObjectSummary> summaries = listing.getObjectSummaries();
        int j = 0;
        int i = 1;
        Logger.getLogger(MappingServiceRestImpl.class)
            .info("Destination terminology *" + destinationTerminology + "*");
        while (listing.isTruncated()) {
          listing = s3Client.listNextBatchOfObjects(listing);
          summaries = listing.getObjectSummaries();

          Logger.getLogger(MappingServiceRestImpl.class)
          .info("CCC start with " + j++ + ": " + summaries.size());
          for (S3ObjectSummary sum : summaries) {
            String fileName = sum.getKey();
            if ((fileName.contains("ExtendedMap")
                || fileName.contains("SimpleMap")) && !fileName.contains("Full")
                && !fileName.contains("backup")) {
              Logger.getLogger(MappingServiceRestImpl.class)
                  .info("Summary #" + i++ + " with: " + sum.getKey());
              SearchResult result = new SearchResultJpa();
              String shortName = fileName.substring(fileName.lastIndexOf('/'));
              if (fileName.toLowerCase().contains("alpha")) {
                result.setTerminology("ALPHA");
              } else if (fileName.toLowerCase().contains("beta")) {
                result.setTerminology("BETA");
              } else if (shortName.startsWith("x")) {
                result.setTerminology("A/B");
              } else {
                result.setTerminology("FINAL");
              }
              Matcher m = Pattern.compile("[0-9]{8}").matcher(fileName);
              while (m.find()) {
                result.setTerminologyVersion(m.group());
              }

              result.setValue(shortName);
              result.setValue2(fileName);
              searchResults.addSearchResult(result);
            }
          }
        }
      }
      
      // sort files by release date
      searchResults.sortBy(new Comparator<SearchResult>() {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
          String releaseDate1 = o1.getTerminologyVersion();
          String releaseDate2 = o2.getTerminologyVersion();
          return releaseDate2.compareTo(releaseDate1);
        }
      });

      return searchResults;

    } catch (Exception e) {
      handleException(e, "trying to get files from amazon s3", user,
          mapProjectId.toString(), "");
    } finally {

      mappingService.close();
      securityService.close();
    }
    return null;
  }

  private void callTestMethod() throws Exception {
    Logger.getLogger(MappingServiceRestImpl.class).info("AAA");
    String bucketName = "release-ihtsdo-prod-published";
    String testFileName =
        "international/xSnomedCT_RF2Release_INT_20170131/Delta/Terminology/xsct2_Concept_Delta_INT_20170131.txt";

    // Connect to server
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
        .withRegion(Regions.US_EAST_1)
        .withCredentials(new InstanceProfileCredentialsProvider(false)).build();

    // List Buckets
    Logger.getLogger(MappingServiceRestImpl.class).info("BBB start");
    List<Bucket> buckets = s3Client.listBuckets();
    for (Bucket b : buckets) {
      Logger.getLogger(MappingServiceRestImpl.class)
          .info("BBB with bucket name" + b.getName());
    }
    Logger.getLogger(MappingServiceRestImpl.class).info("BBB end");

    // Verify Buckets Exists
    if (!s3Client.doesBucketExist(bucketName)) {
      throw new Exception("Cannot find Bucket Name");
    } else {
      Logger.getLogger(MappingServiceRestImpl.class)
          .info("CCC Bucket " + bucketName + " accessed.");
    }

    // List All Files on Bucket "release-ihtsdo-prod-published"
    ObjectListing listing = s3Client.listObjects(bucketName);
    List<S3ObjectSummary> summaries = listing.getObjectSummaries();
    
    System.out.println("CCC start with " + summaries.size());
    int i = 1;
    for (S3ObjectSummary sum : summaries) {
      Logger.getLogger(MappingServiceRestImpl.class)
          .info("Summary #" + i++ + " with: " + sum.getKey());
    }
    Logger.getLogger(MappingServiceRestImpl.class).info("CCC end");

    
    // Pull File Down and Copy to Local Directory (Directory must have rw/rw/rw (666) permissions )
    Logger.getLogger(MappingServiceRestImpl.class).info("DDD start");
    S3Object s3object = s3Client.getObject(bucketName, testFileName);
    S3ObjectInputStream inputStream = s3object.getObjectContent();
    FileUtils.copyInputStreamToFile(inputStream, new File("~/aws/test.txt"));
    inputStream.close();
    Logger.getLogger(MappingServiceRestImpl.class).info("DDD end");
  }

//  /**
//   * Reads an InputStream and returns its contents as a String. Also effects
//   * rate control.
//   * @param inputStream The InputStream to read from.
//   * @return The contents of the InputStream as a String.
//   * @throws Exception on error.
//   */
//  private static String inputStreamToString(final InputStream inputStream)
//    throws Exception {
//    final StringBuilder outputBuilder = new StringBuilder();
//
//    String string;
//    if (inputStream != null) {
//      BufferedReader reader =
//          new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
//      while (null != (string = reader.readLine())) {
//        outputBuilder.append(string).append('\n');
//      }
//    }
//
//    return outputBuilder.toString();
//  }

}
