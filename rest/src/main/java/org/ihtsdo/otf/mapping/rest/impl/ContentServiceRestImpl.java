package org.ihtsdo.otf.mapping.rest.impl;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.TerminologyVersion;
import org.ihtsdo.otf.mapping.helpers.TerminologyVersionList;
import org.ihtsdo.otf.mapping.jpa.algo.ClamlLoaderAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.GmdnDownloadAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.GmdnLoaderAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.MapRecordRf2ComplexMapLoaderAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.MapRecordRf2SimpleMapLoaderAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.RefsetmemberRemoverAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.RemoverAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.Rf2DeltaLoaderAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.Rf2SnapshotLoaderAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.SimpleLoaderAlgorithm;
import org.ihtsdo.otf.mapping.jpa.handlers.IndexViewerHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.rest.ContentServiceRest;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * REST implementation for content service.
 */
@Path("/content")
@Api(value = "/content", description = "Operations to get RF2 content for a terminology.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class ContentServiceRestImpl extends RootServiceRestImpl
    implements ContentServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link ContentServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public ContentServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#getConcept(java.lang.
   * String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/concept/id/{terminology}/{terminolgoyVersion}/{terminologyId}")
  @ApiOperation(value = "Get concept by id, terminology, and version", notes = "Gets the concept for the specified parameters.", response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Concept getConcept(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminolgoyVersion") String terminologyVersion,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /concept/" + terminology + "/"
            + terminologyVersion + "/id/" + terminologyId);

    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "get concept",
          securityService);

      final Concept c = contentService.getConcept(terminologyId, terminology,
          terminologyVersion);

      if (c != null) {
        // Make sure to read descriptions and relationships (prevents
        // serialization error)
        for (final Description d : c.getDescriptions()) {
          d.getLanguageRefSetMembers();
        }
        for (final Relationship r : c.getRelationships()) {
          r.getDestinationConcept();
        }
      }

      return c;
    } catch (Exception e) {
      handleException(e, "trying to get a concept");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#getConcept(java.lang.
   * String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/concept/id/{terminology}/{terminologyId}")
  @ApiOperation(value = "Get the concept for the latest version of an id and terminology.", notes = "Gets the concept for the specified parameters.", response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Concept getConcept(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /concept/" + terminology + "/id/"
            + terminologyId);

    final ContentService contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "get concept",
          securityService);

      final String version = metadataService.getLatestVersion(terminology);
      final Concept c =
          contentService.getConcept(terminologyId, terminology, version);
      c.getDescriptions();
      c.getRelationships();
      return c;

    } catch (Exception e) {
      handleException(e, "trying to get the latest version concept");
      return null;
    } finally {
      metadataService.close();
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#findConceptsForQuery(
   * java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/concept")
  @ApiOperation(value = "Find concepts matching a search query.", notes = "Gets a list of search results that match the lucene query.", response = String.class)
  public SearchResultList findConceptsForQuery(
    @ApiParam(value = "Query, e.g. 'heart attack'", required = true) @QueryParam("query") String query,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /concept " + query);

    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "find concepts",
          securityService);
      return contentService.findConceptsForQuery(query, new PfsParameterJpa());

    } catch (Exception e) {
      handleException(e, "trying to find the concepts by query");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#
   * findDescendantConcepts(java.lang.String, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/concept/id/{terminology}/{terminologyVersion}/{terminologyId}/descendants")
  @ApiOperation(value = "Find concept descendants.", notes = "Gets a list of search results for each descendant concept.", response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findDescendantConcepts(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /concept/" + terminology + "/"
            + terminologyVersion + "/id/" + terminologyId + "/descendants");

    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "find descendant concepts",
          securityService);

      // want all descendants, do not use PFS parameter
      return contentService.findDescendantConcepts(terminologyId, terminology,
          terminologyVersion, null);
    } catch (Exception e) {
      handleException(e, "trying to find descendant concepts");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#findChildConcepts(
   * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/concept/id/{terminology}/{terminologyVersion}/{terminologyId}/children")
  @ApiOperation(value = "Find concept children.", notes = "Gets a list of search results for each child concept.", response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findChildConcepts(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("terminologyId") String id,
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /concept/" + terminology + "/"
            + terminologyVersion + "/id/" + id.toString() + "/descendants");

    final ContentService contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "find child concepts",
          securityService);

      String isaId = "";
      final Map<String, String> relTypesMap = metadataService
          .getHierarchicalRelationshipTypes(terminology, terminologyVersion);
      for (final Map.Entry<String, String> entry : relTypesMap.entrySet()) {
        if (entry.getValue().toLowerCase().startsWith("is"))
          isaId = entry.getKey();
      }

      final SearchResultList results = new SearchResultListJpa();

      // get the concept and add it as first element of concept list
      final Concept concept = contentService.getConcept(id.toString(),
          terminology, terminologyVersion);

      // if no concept, return empty list
      if (concept == null) {
        return results;
      }

      // cycle over relationships
      for (final Relationship rel : concept.getInverseRelationships()) {

        if (rel.isActive() && rel.getTypeId().equals(new Long(isaId))
            && rel.getSourceConcept().isActive()) {

          final Concept c = rel.getSourceConcept();

          SearchResult sr = new SearchResultJpa();
          sr.setId(c.getId());
          sr.setTerminologyId(c.getTerminologyId());
          sr.setTerminology(c.getTerminology());
          sr.setTerminologyVersion(c.getTerminologyVersion());
          sr.setValue(c.getDefaultPreferredName());

          // add search result to list
          results.addSearchResult(sr);
        }
      }

      return results;

    } catch (Exception e) {
      handleException(e, "trying to find the child concepts");
      return null;
    } finally {
      metadataService.close();
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#
   * findDeltaConceptsForTerminology(java.lang.String, java.lang.String,
   * java.lang.String, org.ihtsdo.otf.mapping.helpers.PfsParameterJpa)
   */
  @Override
  @POST
  @Path("/terminology/id/{terminology}/{terminologyVersion}/delta")
  @ApiOperation(value = "Gets the most recently edited concepts", notes = "Gets a list of search results for concepts changed since the last delta run.", response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findDeltaConceptsForTerminology(
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken,
    @ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /terminology/id/" + terminology + "/"
            + terminologyVersion + "/delta");

    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "find delta concepts",
          securityService);

      final ConceptList conceptList = contentService
          .getConceptsModifiedSinceDate(terminology, null, pfsParameter);
      final SearchResultList results = new SearchResultListJpa();

      for (final Concept c : conceptList.getConcepts()) {

        // first pass check to see if this is a new concept or a
        // modified
        // concept
        // this will erroneously report NEW concept if all of the
        // descriptions,
        // relationships, and language ref set members were modified
        boolean modifiedConcept = false;
        for (final Description d : c.getDescriptions()) {

          if (!d.getEffectiveTime().equals(c.getEffectiveTime()))
            modifiedConcept = true;
          for (final LanguageRefSetMember l : d.getLanguageRefSetMembers()) {
            if (!l.getEffectiveTime().equals(c.getEffectiveTime()))
              modifiedConcept = true;
          }
        }

        for (final Relationship r : c.getRelationships()) {
          if (!r.getEffectiveTime().equals(c.getEffectiveTime()))
            modifiedConcept = true;
        }

        final SearchResult result = new SearchResultJpa();
        result.setId(c.getId());
        result.setTerminologyVersion(modifiedConcept ? "Modified" : "New");
        result.setTerminologyId(c.getTerminologyId());
        result.setValue(c.getDefaultPreferredName());
        results.addSearchResult(result);

      }

      results.setTotalCount(conceptList.getTotalCount());

      return results;
    } catch (Exception e) {
      handleException(e, "trying to get concepts changed in last delta");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#getIndexDomains(java.
   * lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/index/{terminology}/{terminologyVersion}")
  @ApiOperation(value = "Get the index domains available for given terminology and version.", notes = "Gets the index domains available for the given terminology and version.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getIndexDomains(
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /index/" + terminology + "/"
            + terminologyVersion);

    final IndexViewerHandler indexViewerHandler = new IndexViewerHandler();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "get index domain",
          securityService);

      return indexViewerHandler.getIndexDomains(terminology,
          terminologyVersion);

    } catch (Exception e) {
      handleException(e, "trying to get the indexes to be viewed");
      return null;
    } finally {
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#
   * getIndexViewerPagesForIndex(java.lang.String, java.lang.String,
   * java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/index/{terminology}/{terminologyVersion}/{index}")
  @ApiOperation(value = "Return the index page names available for given terminology, version and domain.", notes = "Returns the pages available for the given terminology, version and domain.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getIndexViewerPagesForIndex(
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Name of index or domain", required = true) @PathParam("index") String index,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /index/" + terminology + "/"
            + terminologyVersion + "/" + index);

    final IndexViewerHandler indexViewerHandler = new IndexViewerHandler();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "get index pages for domain",
          securityService);

      return indexViewerHandler.getIndexPagesForIndex(terminology,
          terminologyVersion, index);

    } catch (Exception e) {
      handleException(e, "trying to get the page names for the given index");
      return null;
    } finally {
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#
   * getIndexViewerDetailsForLink(java.lang.String, java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/index/{terminology}/{terminologyVersion}/{domain}/details/{link}")
  @ApiOperation(value = "Peform the search given the search terms.", notes = "Performs the search given the search terms in the given terminology.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public String getIndexViewerDetailsForLink(
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Domain/Index within terminology", required = true) @PathParam("domain") String domain,
    @ApiParam(value = "Object link", required = true) @PathParam("link") String link,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /index/" + terminology + "/"
            + terminologyVersion + "/" + domain + "/details/" + link);

    final IndexViewerHandler indexViewerHandler = new IndexViewerHandler();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER,
          "get index viewer details for object link", securityService);

      return indexViewerHandler.getDetailsAsHtmlForLink(terminology,
          terminologyVersion, domain, link);

    } catch (Exception e) {
      handleException(e, "trying to get index viewer details for object link");
      return null;
    } finally {
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#
   * findIndexViewerEntries(java.lang.String, java.lang.String,
   * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
   * boolean, java.lang.String)
   */
  @Override
  @GET
  @Path("/index/{terminology}/{terminologyVersion}/{domain}/search/{searchField}/subSearch/{subSearchField}/subSubSearch/{subSubSearchField}/{allFlag}")
  @ApiOperation(value = "Peform the search given the search terms.", notes = "Performs the search given the search terms in the given terminology.", response = SearchResultList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList findIndexViewerEntries(
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Domain/Index within terminology", required = true) @PathParam("domain") String domain,
    @ApiParam(value = "First level search field", required = true) @PathParam("searchField") String searchField,
    @ApiParam(value = "Second level search field to refine search", required = true) @PathParam("subSearchField") String subSearchField,
    @ApiParam(value = "Third level search field to refine search", required = true) @PathParam("subSubSearchField") String subSubSearchField,
    @ApiParam(value = "If all levels should be searched, e.g. true", required = true) @PathParam("allFlag") boolean allFlag,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /index/" + terminology + "/"
            + terminologyVersion + "/" + domain + "/" + searchField + "/"
            + subSearchField + "/" + subSubSearchField + "/" + allFlag);

    final IndexViewerHandler indexViewerHandler = new IndexViewerHandler();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "find index entries",
          securityService);

      final SearchResultList searchResultList =
          indexViewerHandler.findIndexEntries(terminology, terminologyVersion,
              domain, searchField, subSearchField, subSubSearchField, allFlag);
      searchResultList.setTotalCount(searchResultList.getCount());
      return searchResultList;

    } catch (Exception e) {
      handleException(e, "trying to perform a search of the indexes");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/map/record/rf2/complex")
  @Consumes(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Load complex RF2 map record data", notes = "Load complex map data.")
  public void loadMapRecordRf2ComplexMap(
    @ApiParam(value = "RF2 input file", required = true) String inputFile,
    @ApiParam(value = "Member flag", required = true) @QueryParam("memberFlag") Boolean memeberFlag,
    @ApiParam(value = "Record flag", required = true) @QueryParam("recordFlag") Boolean recordFlag,
    @ApiParam(value = "Refset id", required = false) @QueryParam("refsetId") String refsetId,
    @ApiParam(value = "Workflow status", required = true) @QueryParam("workflowStatus") String workflowStatus,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /map/record/rf2/complex");

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "remove map record",
        securityService);

    try (final MapRecordRf2ComplexMapLoaderAlgorithm algo =
        new MapRecordRf2ComplexMapLoaderAlgorithm(inputFile, memeberFlag,
            recordFlag, refsetId, workflowStatus);) {

      algo.compute();

      Logger.getLogger(getClass())
          .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
      handleException(e, "trying to load complex map record");

    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/map/record/rf2/simple")
  @Consumes(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Load simple RF2 map record data", notes = "Load simple map data.")
  public void loadMapRecordRf2SimpleMap(
    @ApiParam(value = "RF2 input file", required = true) String inputFile,
    @ApiParam(value = "Member flag", required = true) @QueryParam("memberFlag") Boolean memeberFlag,
    @ApiParam(value = "Record flag", required = true) @QueryParam("recordFlag") Boolean recordFlag,
    @ApiParam(value = "Refset id", required = false) @QueryParam("refsetId") String refsetId,
    @ApiParam(value = "Workflow status", required = true) @QueryParam("workflowStatus") String workflowStatus,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Content): /map/record/");

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "load map record RF2 simple", securityService);

    try (final MapRecordRf2SimpleMapLoaderAlgorithm algo =
        new MapRecordRf2SimpleMapLoaderAlgorithm(inputFile, memeberFlag,
            recordFlag, refsetId, workflowStatus);) {

      algo.compute();

      Logger.getLogger(getClass())
          .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
      handleException(e, "trying to load simple map record");

    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/terminology/load/claml/{terminology}/{version}")
  @Consumes(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Loads terminology Claml from file", notes = "Loads terminology RF2 delta from file for specified terminology and version")
  public void loadTerminologyClaml(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "Claml input file", required = true) String inputFile,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /terminology/load/claml/" + terminology
            + "/" + version + " from input file " + inputFile);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "load CLAML terminology",
        securityService);

    final String localTerminology = removeSpaces(terminology);
    final String localVersion = removeSpaces(version);

    // validate that the terminology & version to not already exist.
    if (doesTerminologyVersionExist(localTerminology, localVersion)) {
      handleException(new Exception("Terminology and version already exist."),
          "Terminology and version already exist.");
    }

    try (final ClamlLoaderAlgorithm algo =
        new ClamlLoaderAlgorithm(localTerminology, localVersion, inputFile);) {

      algo.compute();

      Logger.getLogger(getClass())
          .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
      handleException(e, "trying to load terminology claml from directory");
    }
  }

  /* see superclass */
  @Override
  @POST
  @Path("/terminology/download/gmdn")
  @ApiOperation(value = "Download most recent GMDN terminology from SFTP", notes = "Downloads most recent GMDN terminology from SFTP")
  public void downloadTerminologyGmdn(
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /terminology/download/gmdn/");

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "download GMDN terminology", securityService);

    try (final GmdnDownloadAlgorithm algo = new GmdnDownloadAlgorithm();) {

      algo.compute();

      Logger.getLogger(getClass())
          .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
      handleException(e,
          "trying to download most recent terminology GMDN from SFTP");
    }
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/terminology/load/gmdn/{version}")
  @Consumes(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Loads GMDN terminology from directory", notes = "Loads GMDN terminology from directory for specified terminology and version")
  public void loadTerminologyGmdn(
    @ApiParam(value = "Version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "GMDN input directory", required = true) String inputDir,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /terminology/load/gmdn/" + version
            + " from input directory " + inputDir);

    // If inputDir set as 'GENERATE', generate based on config.properties
    if (inputDir.equals("GENERATE")) {
      inputDir = ConfigUtility.getConfigProperties()
          .getProperty("map.principle.source.document.dir");
      // Strip off final folder, and replace with "GMDN/{version}"
      if (inputDir.endsWith("/")) {
        inputDir = inputDir.substring(0, inputDir.length() - 1);
      }
      inputDir = inputDir.substring(0, inputDir.lastIndexOf("/"));
      inputDir = inputDir + "/GMDN/" + version;
    }

    Logger.getLogger(getClass())
        .info("Input directory generated from config.properties, and set to "
            + inputDir);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "load GMDN terminology",
        securityService);

    try (final GmdnLoaderAlgorithm algo =
        new GmdnLoaderAlgorithm(version, inputDir);) {

      algo.compute();

      Logger.getLogger(getClass())
          .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
      handleException(e, "trying to load terminology GMDN from directory");
    }
  }

  /* see superclass */
  @Override
  @DELETE
  @Path("/map/record/{refsetId}")
  @ApiOperation(value = "Removes a terminology from a database", notes = "Removes a terminology from a database")
  public boolean removeMapRecord(
    @ApiParam(value = "RefSet Id, e.g. 2014_09_01", required = true) @PathParam("refsetId") String refsetId,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /map/record/" + refsetId);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "remove map record",
        securityService);

    try (final RefsetmemberRemoverAlgorithm algo =
        new RefsetmemberRemoverAlgorithm(refsetId);) {

      algo.compute();

      Logger.getLogger(getClass())
          .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

      return true;
    } catch (Exception e) {
      handleException(e, "trying to remove map record");
      return false;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @DELETE
  @Path("/terminology/{terminology}/{version}")
  @ApiOperation(value = "Remove a terminology", notes = "Removes all elements for a specified terminology and version")
  public boolean removeTerminology(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Content): /terminology/" + terminology + "/" + version);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    final String userName = authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "remove terminology", securityService);

    try (final RemoverAlgorithm algo =
        new RemoverAlgorithm(terminology, version);) {

      // Remove terminology
      Logger.getLogger(getClass())
          .info("  Remove terminology for  " + terminology + "/" + version);

      algo.setLastModifiedBy(userName);
      algo.compute();

      Logger.getLogger(getClass())
          .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

      securityService.close();
      securityService = new SecurityServiceJpa();
      securityService.addLogEntry(userName, terminology, version, null,
          "REMOVER", "Remove terminology");

      return true;

    } catch (Exception e) {
      handleException(e, "trying to remove terminology");
      return false;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/terminology/load/rf2/delta/{terminology}/{lastPublicationDate}")
  @Consumes(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Loads terminology RF2 delta from directory", notes = "Loads terminology RF2 delta from directory for specified terminology and version")
  public void loadTerminologyRf2Delta(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Last publication date, e.g. 20150101", required = false) @PathParam("lastPublicationDate") String lastPublicationDate,
    @ApiParam(value = "RF2 input directory", required = true) String inputDir,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /terminology/load/rf2/delta/"
            + terminology + " from input directory " + inputDir);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "load RF2 delta terminology", securityService);

    try (final Rf2DeltaLoaderAlgorithm algo = new Rf2DeltaLoaderAlgorithm(
        terminology, inputDir, lastPublicationDate);) {

      algo.compute();

      Logger.getLogger(getClass())
          .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
      handleException(e, "trying to load terminology delta from RF2 directory");
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/terminology/load/aws/rf2/snapshot/{terminology}/{version}")
  @Consumes({
      MediaType.TEXT_PLAIN
  })
  @ApiOperation(value = "Loads terminology RF2 snapshot from directory", notes = "Loads terminology RF2 snapshot from directory for specified terminology and version")
  public void loadTerminologyAwsRf2Snapshot(
    @ApiParam(value = "Terminology, e.g. SNOMED CT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Version, e.g. 20170131", required = true) @PathParam("version") String version,
    @ApiParam(value = "Aws Zip File Name, e.g. filename with full path", required = true) @QueryParam("awsZipFileName") String awsZipFileName,
    @ApiParam(value = "Calculate tree positions", required = false) @QueryParam("treePositions") Boolean treePositions,
    @ApiParam(value = "Send notification", required = false) @QueryParam("sendNotification") Boolean sendNotification,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /terminology/load/rf2/snapshot/aws/"
            + " awsZipFileName " + awsZipFileName);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "load RF2 snapshot terminology", securityService);

    final String localTerminology = removeSpaces(terminology);
    final String localVersion = removeSpaces(version);

    // validate that the terminology & version to not already exist.
    if (doesTerminologyVersionExist(localTerminology, localVersion)) {
      handleException(new Exception("Terminology and version already exist."),
          "Terminology and version already exist.");
    }

    File placementDir = null;
    Rf2SnapshotLoaderAlgorithm algo = null;

    try {
      // Access zipped awsFile
      AmazonS3 s3Client = connectToAmazonS3();

      final String bucketName = "release-ihtsdo-prod-published";
      S3Object s3object = s3Client.getObject(bucketName, awsZipFileName);

      // Unzip awsFile to temp directory
      File tempDir = FileUtils.getTempDirectory();
      placementDir = new File(tempDir.getAbsolutePath() + File.separator
          + "TerminologyLoad_" + startTimeOrig);
      placementDir.mkdir();

      Logger.getLogger(getClass())
          .info("loadTerminologyAwsRf2Snapshot - downloading " + terminology
              + " " + version + " to " + placementDir);
      S3ObjectInputStream inputStream = s3object.getObjectContent();
      File zippedFile = new File(placementDir,
          awsZipFileName.substring(awsZipFileName.lastIndexOf('/') + 1));
      FileUtils.copyInputStreamToFile(inputStream, zippedFile);
      inputStream.close();

      // UNZIP to Placement
      unzipToDirectory(zippedFile, placementDir);

      File testDir = new File(placementDir.getAbsolutePath() + File.separator
          + zippedFile.getName().substring(0, zippedFile.getName().indexOf("."))
          + File.separator + "Snapshot");

      // Load content with input pulled from S3
      algo = new Rf2SnapshotLoaderAlgorithm(localTerminology, localVersion,
          testDir.getAbsolutePath(), treePositions, sendNotification);

      algo.compute();

      Logger.getLogger(getClass())
          .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
      handleException(e,
          "trying to load terminology snapshot from RF2 directory");
    } finally {
      // Remove directory
      FileUtils.deleteDirectory(placementDir);
      algo.close();
    }
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/terminology/load/rf2/snapshot/{terminology}/{version}")
  @Consumes({
      MediaType.TEXT_PLAIN
  })
  @ApiOperation(value = "Loads terminology RF2 snapshot from directory", notes = "Loads terminology RF2 snapshot from directory for specified terminology and version")
  public void loadTerminologyRf2Snapshot(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "RF2 input directory", required = true) String inputDir,
    @ApiParam(value = "Calcualte tree positions", required = false) @QueryParam("treePositions") Boolean treePositions,
    @ApiParam(value = "Send notification", required = false) @QueryParam("sendNotification") Boolean sendNotification,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /terminology/load/rf2/snapshot/"
            + terminology + "/" + version + " from input directory "
            + inputDir);

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "load RF2 snapshot terminology", securityService);

    final String localTerminology = removeSpaces(terminology);
    final String localVersion = removeSpaces(version);

    // validate that the terminology & version to not already exist.
    if (doesTerminologyVersionExist(localTerminology, localVersion)) {
      handleException(new Exception("Terminology and version already exist."),
          "Terminology and version already exist.");
    }

    // Track system level information
    long startTimeOrig = System.nanoTime();

    Boolean localTreePostions = false;
    Boolean localSendNotification = false;

    if (treePositions != null) {
      localTreePostions = treePositions;
    }

    if (sendNotification != null) {
      localSendNotification = sendNotification;
    }

    try (final Rf2SnapshotLoaderAlgorithm algo =
        new Rf2SnapshotLoaderAlgorithm(localTerminology, localVersion, inputDir,
            localTreePostions, localSendNotification);) {

      algo.compute();

      Logger.getLogger(getClass())
          .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
      handleException(e,
          "trying to load terminology snapshot from RF2 directory");
    }

  }

  /* see superclass */
  @Override
  @PUT
  @Path("/terminology/load/simple/{terminology}/{version}")
  @Consumes(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Load simple terminology from file", notes = "Loads simple terminology from specified input file")
  public void loadTerminologySimple(
    @ApiParam(value = "Terminology, e.g. UMLS", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Version, e.g. latest", required = true) @PathParam("version") String version,
    @ApiParam(value = "Full path to input file", required = true) String inputFile,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /terminology/load/simple/ " + terminology
            + ", " + version + " from input file " + inputFile);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    final String localTerminology = removeSpaces(terminology);
    final String localVersion = removeSpaces(version);

    // validate that the terminology & version to not already exist.
    if (doesTerminologyVersionExist(localTerminology, localVersion)) {
      handleException(new Exception("Terminology and version already exist."),
          "Terminology and version already exist.");
    }

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "load simple terminology", securityService);

    try (final SimpleLoaderAlgorithm algo = new SimpleLoaderAlgorithm(
        localTerminology, localVersion, inputFile, null);) {

      algo.compute();

      Logger.getLogger(getClass())
          .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
      handleException(e, "trying to load simple terminology from directory");
    }
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/terminology/reload/rf2/snapshot/{terminology}/{version}")
  @Consumes({
      MediaType.TEXT_PLAIN
  })
  @ApiOperation(value = "Removes and Loads terminology RF2 snapshot from directory", notes = "Removes and loads terminology RF2 snapshot from directory for specified terminology and version")
  public void reloadTerminologyRf2Snapshot(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "RF2 input directory", required = true) String inputDir,
    @ApiParam(value = "Calculate tree positions", required = false) @QueryParam("treePositions") Boolean treePositions,
    @ApiParam(value = "Send notification", required = false) @QueryParam("sendNotification") Boolean sendNotification,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /terminology/reload/rf2/snapshot/"
            + terminology + "/" + version + " from input directory "
            + inputDir);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    Boolean localTreePostions = false;
    Boolean localSendNotification = false;

    if (treePositions != null) {
      localTreePostions = treePositions;
    }

    if (sendNotification != null) {
      localSendNotification = sendNotification;
    }

    String userName = authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "reload RF2 snapshot terminology", securityService);

    // If other processes are already running, return the currently running
    // process information as an Exception
    // If not, obtain the processLock
    try {
      RootServiceJpa
          .lockProcess(userName + " is currently running process = Reload "
              + terminology + ", " + version);
    } catch (Exception e) {
      handleException(e, e.getMessage());
    } finally {
      securityService.close();
    }

    try (final RemoverAlgorithm removeAlgo =
        new RemoverAlgorithm(terminology, version);) {

      // Remove terminology
      Logger.getLogger(getClass())
          .info("  Remove terminology for  " + terminology + "/" + version);

      removeAlgo.setLastModifiedBy(userName);
      removeAlgo.compute();

      Logger.getLogger(getClass()).info(
          "Remove Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

      securityService.close();
      securityService = new SecurityServiceJpa();
      securityService.addLogEntry(userName, terminology, version, null,
          "REMOVER", "Remove terminology");

      // if no errors try to load
      try (final Rf2SnapshotLoaderAlgorithm loadAlgo =
          new Rf2SnapshotLoaderAlgorithm(terminology, version, inputDir,
              localTreePostions, localSendNotification);) {

        loadAlgo.compute();

        Logger.getLogger(getClass()).info(
            "Load Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

        securityService.close();
        securityService = new SecurityServiceJpa();
        securityService.addLogEntry(userName, terminology, version, null,
            "LOAD", "Load " + terminology + " " + version);

      } catch (Exception e) {
        handleException(e,
            "trying to reload terminology snapshot from RF2 directory");
      }

    } catch (Exception e) {
      handleException(e, "trying to reload terminology");
    } finally {
      RootServiceJpa.unlockProcess();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @PUT
  @Path("/terminology/reload/aws/rf2/snapshot/{terminology}/{removeVersion}/{loadVersion}")
  @Consumes({
      MediaType.TEXT_PLAIN
  })
  @Produces({
    MediaType.TEXT_PLAIN
})
  @ApiOperation(value = "Removes a terminology, and loads terminology RF2 snapshot from aws", notes = "Removes terminology and loads RF2 snapshot from aws for specified terminology and version")
  public String reloadTerminologyAwsRf2Snapshot(
    @ApiParam(value = "Terminology, e.g. SNOMED CT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Version to remove, e.g. 20170131", required = true) @PathParam("removeVersion") String removeVersion,
    @ApiParam(value = "Version to load, e.g. 20170131", required = true) @PathParam("loadVersion") String loadVersion,
    @ApiParam(value = "Aws Zip File Name, e.g. filename with full path", required = true) @QueryParam("awsZipFileName") String awsZipFileName,
    @ApiParam(value = "Calculate tree positions", required = false) @QueryParam("treePositions") Boolean treePositions,
    @ApiParam(value = "Send notification", required = false) @QueryParam("sendNotification") Boolean sendNotification,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /terminology/reload/aws/rf2/snapshot/"
            + terminology + "/" + removeVersion + "/" + loadVersion
            + " awsZipFileName= " + awsZipFileName);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    String userName = authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "reload RF2 snapshot terminology from aws ", securityService);

    // If other processes are already running, return the currently running
    // process information as an Exception
    // If not, obtain the processLock
    try {
      RootServiceJpa
          .lockProcess(userName + " is currently running process = Reload "
              + terminology + ", removeVersion=" + removeVersion
              + ", loadVersion=" + loadVersion);
    } catch (Exception e) {
      return e.getMessage();
    } finally {
      securityService.close();
    }

    try (final RemoverAlgorithm removeAlgo =
        new RemoverAlgorithm(terminology, removeVersion);) {

      // Remove terminology
      Logger.getLogger(getClass()).info(
          "  Remove terminology for  " + terminology + ", " + removeVersion);

      removeAlgo.setLastModifiedBy(userName);
      removeAlgo.compute();

      Logger.getLogger(getClass()).info(
          "Remove Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

      securityService.close();
      securityService = new SecurityServiceJpa();
      securityService.addLogEntry(userName, terminology, removeVersion, null,
          "REMOVER", "Remove terminology");

      // if no errors try to load from aws

      final String localTerminology = removeSpaces(terminology);
      final String localVersion = removeSpaces(loadVersion);

      File placementDir = null;
      Rf2SnapshotLoaderAlgorithm algo = null;

      try {
        // Access zipped awsFile
        AmazonS3 s3Client = connectToAmazonS3();

        final String bucketName = "release-ihtsdo-prod-published";
        S3Object s3object = s3Client.getObject(bucketName, awsZipFileName);

        // Unzip awsFile to temp directory
        File tempDir = FileUtils.getTempDirectory();
        placementDir = new File(tempDir.getAbsolutePath() + File.separator
            + "TerminologyLoad_" + startTimeOrig);
        placementDir.mkdir();

        Logger.getLogger(getClass()).info("Downloading " + terminology + " "
            + loadVersion + " to " + placementDir);
        S3ObjectInputStream inputStream = s3object.getObjectContent();
        File zippedFile = new File(placementDir,
            awsZipFileName.substring(awsZipFileName.lastIndexOf('/') + 1));
        FileUtils.copyInputStreamToFile(inputStream, zippedFile);
        inputStream.close();

        // UNZIP to Placement
        Logger.getLogger(getClass()).info("Decompressing " + zippedFile);
        unzipToDirectory(zippedFile, placementDir);
        Collection<File> files = FileUtils.listFiles(placementDir, null, true);
        for (File f : files) {
          Logger.getLogger(getClass()).info("AAA with " + f.getAbsolutePath());
        }
        Logger.getLogger(getClass()).info("BBB with " + placementDir);

        File testDir = new File(placementDir.getAbsolutePath() + File.separator
            + zippedFile.getName().substring(0,
                zippedFile.getName().indexOf("."))
            + File.separator + "Snapshot");
        Logger.getLogger(getClass())
            .info("CCC with " + testDir.getAbsolutePath());
        files = FileUtils.listFiles(testDir, null, true);
        for (File f : files) {
          Logger.getLogger(getClass()).info("DDD with " + f.getAbsolutePath());
        }

        // Load content with input pulled from S3
        algo = new Rf2SnapshotLoaderAlgorithm(localTerminology, localVersion,
            testDir.getAbsolutePath(), treePositions, sendNotification);

        Logger.getLogger(getClass()).info("EEE");

        algo.compute();

        Logger.getLogger(getClass()).info("FFF");

        Logger.getLogger(getClass())
            .info("Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

        return "Success";
      } catch (Exception e) {
        handleException(e,
            "trying to load terminology snapshot from RF2 directory");
        return "Failure";
      } finally {
        // Remove directory
        FileUtils.deleteDirectory(placementDir);
        algo.close();
      }
    } catch (Exception e) {
      handleException(e, "trying to remove terminology");
      return "Failure";
    } finally {
      RootServiceJpa.unlockProcess();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/map/record/reload/{refsetId}")
  @Consumes(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Load simple RF2 map record data", notes = "Load simple map data.")
  public boolean reloadMapRecord(
    @ApiParam(value = "RefSet Id, e.g. 2014_09_01", required = true) @PathParam("refsetId") String refsetId,
    @ApiParam(value = "RF2 input file", required = true) String inputFile,
    @ApiParam(value = "Member flag", required = true) @QueryParam("memberFlag") Boolean memeberFlag,
    @ApiParam(value = "Record flag", required = true) @QueryParam("recordFlag") Boolean recordFlag,
    @ApiParam(value = "Workflow status", required = true) @QueryParam("workflowStatus") String workflowStatus,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Content): /map/record/");

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "load map record RF2 simple", securityService);

    try (final RefsetmemberRemoverAlgorithm removeAlgo =
        new RefsetmemberRemoverAlgorithm(refsetId);) {

      removeAlgo.compute();

      Logger.getLogger(getClass()).info(
          "Remove Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

      // if remove was successful do add
      try (final MapRecordRf2SimpleMapLoaderAlgorithm loadAlgo =
          new MapRecordRf2SimpleMapLoaderAlgorithm(inputFile, memeberFlag,
              recordFlag, refsetId, workflowStatus);) {

        loadAlgo.compute();

        Logger.getLogger(getClass()).info(
            "Load Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

      } catch (Exception e) {
        handleException(e, "trying to load simple map record");
        return false;
      }

    } catch (Exception e) {
      handleException(e, "trying to remove map record");
      return false;

    } finally {
      securityService.close();
    }

    return true;
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/refset/reload/aws/{refsetId}")
  @Consumes(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Removes refset member data based on refsetId, and reload from AWS snapshot file", notes = "Reload refset member data from AWS snapshot file.")
  @Produces({
    MediaType.TEXT_PLAIN
  })
  public String reloadRefsetMemberAwsSnapshot(
    @ApiParam(value = "RefSet Id, e.g. 2014_09_01", required = true) @PathParam("refsetId") String refsetId,
    @ApiParam(value = "Aws Zip MapSnapshot File Name, e.g. filename with full path", required = true) @QueryParam("awsFileName") String awsFileName,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Content): /map/record/");

    // Track system level information
    long startTimeOrig = System.nanoTime();

    final String userName = authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "reload refset members", securityService);

    // If other processes are already running, return the currently running
    // process information string
    // If not, obtain the processLock
    try {
      RootServiceJpa.lockProcess(userName
          + " is currently running process = Reload refset members for refsetId: "
          + refsetId);
    } catch (Exception e) {
      return e.getMessage();
    } finally {
      securityService.close();
    }

    try (final RefsetmemberRemoverAlgorithm removeAlgo =
        new RefsetmemberRemoverAlgorithm(refsetId);) {

      removeAlgo.compute();

      Logger.getLogger(getClass()).info(
          "Remove Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

      // if remove was successful try to load file from aws
      String inputFile = null;
      File placementDir = null;
      try {
        // Access zipped awsFile
        AmazonS3 s3Client = connectToAmazonS3();

        final String bucketName = "release-ihtsdo-prod-published";
        S3Object s3object = s3Client.getObject(bucketName, awsFileName);

        // Save awsFile to temp directory
        File tempDir = FileUtils.getTempDirectory();
        placementDir = new File(tempDir.getAbsolutePath() + File.separator
            + "TerminologyLoad_" + startTimeOrig);
        placementDir.mkdir();

        Logger.getLogger(getClass())
            .info("Downloading " + awsFileName + " to " + placementDir);
        S3ObjectInputStream inputStream = s3object.getObjectContent();
        File downloadedFile = new File(placementDir,
            awsFileName.substring(awsFileName.lastIndexOf('/') + 1));
        FileUtils.copyInputStreamToFile(inputStream, downloadedFile);
        inputStream.close();

        // Remove any inactive rows
        File filteredFile = new File(placementDir,
            awsFileName.substring(awsFileName.lastIndexOf('/') + 1)
                + "_ActiveOnly");
        BufferedReader fileReader =
            new BufferedReader(new FileReader(downloadedFile));
        BufferedWriter fileWriter =
            new BufferedWriter(new FileWriter(filteredFile));
        String input;
        while ((input = fileReader.readLine()) != null) {
          String[] fields = input.split("\\t");
          if (fields[2].equals("1")) {
            fileWriter.write(input);
            fileWriter.newLine();
          }
        }
        fileReader.close();
        fileWriter.close();

        inputFile = filteredFile.getAbsolutePath();

        // Load extended or simple maps using downloaded AWS file
        if (awsFileName.contains("ExtendedMapSnapshot")) {
          final MapRecordRf2ComplexMapLoaderAlgorithm loadAlgo =
              new MapRecordRf2ComplexMapLoaderAlgorithm(inputFile, true, false,
                  refsetId, "PUBLISHED");

          loadAlgo.compute();
          loadAlgo.close();

        } else if (awsFileName.contains("SimpleMapSnapshot")) {

          final MapRecordRf2SimpleMapLoaderAlgorithm loadAlgo =
              new MapRecordRf2SimpleMapLoaderAlgorithm(inputFile, true, false,
                  refsetId, "PUBLISHED");

          loadAlgo.compute();
          loadAlgo.close();
        } else {
          throw new Exception(
              "Filename must be a Simple or Extended map snapshot.");
        }

        Logger.getLogger(getClass()).info(
            "Load Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

      } catch (Exception e) {
        handleException(e, "trying to load refset members");
        return "Failure";
      } finally {
        // Remove directory
        FileUtils.deleteDirectory(placementDir);
      }

    } catch (Exception e) {
      handleException(e, "trying to remove refset members");
      return "Failure";

    } finally {
      RootServiceJpa.unlockProcess();
      securityService.close();
    }

    return "Success";
  }

  @Override
  @GET
  @Path("/terminology/versions/{terminology}")
  @ApiOperation(value = "Find versions for terminology.", notes = "Gets a list of recont versions for a given terminology.", response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TerminologyVersionList getTerminologyVersions(
    @ApiParam(value = "Terminology name, e.g. SNOMED CT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /terminology/" + terminology + "/");

    if (removeSpaces(terminology).equals("SNOMEDCT")) {
      terminology = "InternationalRF2";
    } else if (terminology.startsWith("ICNP")) {
      terminology = terminology.substring(0, terminology.indexOf(" "))
          + terminology.substring(terminology.indexOf(" ") + 1);
    }

    int year = Calendar.getInstance().get(Calendar.YEAR);
    String currentYear = Integer.toString(year);
    String nextYear = Integer.toString(year + 1);
    String lastYear = Integer.toString(year - 1);

    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
          "load map record RF2 simple", securityService);

      // Connect to server
      final String bucketName = "release-ihtsdo-prod-published";
      AmazonS3 s3Client = connectToAmazonS3();

      List<S3ObjectSummary> fullKeyList = new ArrayList<S3ObjectSummary>();
      ObjectListing objects = s3Client.listObjects(bucketName, "international");

      fullKeyList = objects.getObjectSummaries();

      objects = s3Client.listNextBatchOfObjects(objects);

      while (objects.isTruncated()) {
        fullKeyList.addAll(objects.getObjectSummaries());
        objects = s3Client.listNextBatchOfObjects(objects);
      }

      fullKeyList.addAll(objects.getObjectSummaries());
      TerminologyVersionList returnList = new TerminologyVersionList();
      for (S3ObjectSummary obj : fullKeyList) {
        if (obj.getKey().endsWith("zip") && obj.getKey().contains(terminology)
            && !obj.getKey().contains("published_build_backup")
            && (obj.getKey().contains(lastYear)
                || obj.getKey().contains(currentYear)
                || obj.getKey().contains(nextYear))
            && (obj.getKey().matches(".*\\d.zip")
                || obj.getKey().matches(".*\\dZ.zip"))) {
          TerminologyVersion tv =
              new TerminologyVersion(obj.getKey(), terminology);
          tv.identifyScope();
          returnList.addTerminologyVersion(tv);
        }
      }

      // Remove all duplicates defined by term-version-scope
      returnList.removeDups();

      return returnList;
    } catch (Exception e) {
      handleException(e, "trying to find descendant concepts");
      return null;
    } finally {
      securityService.close();
    }
  }

  private void unzipToDirectory(File zippedFile, File placementDir)
    throws IOException {
    if (zippedFile == null) {
    }

    if (placementDir == null) {
    }

    if (!placementDir.exists()) {
      placementDir.mkdir();
    }
    ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zippedFile));
    ZipEntry entry = zipIn.getNextEntry();

    // iterates over entries in the zip file
    while (entry != null) {
      String filePath =
          placementDir.getAbsolutePath() + File.separator + entry.getName();
      if (!entry.isDirectory()) {
        // if the entry is a file, extracts it
        extractFile(zipIn, filePath);
      } else {
        // if the entry is a directory, make the directory
        File dir = new File(filePath);
        dir.mkdir();
      }
      zipIn.closeEntry();
      entry = zipIn.getNextEntry();
    }
    zipIn.close();
  }

  /**
   * Extracts a zip entry (file entry)
   * @param zipIn
   * @param filePath
   * @throws IOException
   */
  private void extractFile(ZipInputStream zipIn, String filePath)
    throws IOException {
    final int BUFFER_SIZE = 4096;
    BufferedOutputStream bos =
        new BufferedOutputStream(new FileOutputStream(filePath));
    byte[] bytesIn = new byte[BUFFER_SIZE];
    int read = 0;
    while ((read = zipIn.read(bytesIn)) != -1) {
      bos.write(bytesIn, 0, read);
    }
    bos.close();
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
}
