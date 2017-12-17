package org.ihtsdo.otf.mapping.rest.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import org.ihtsdo.otf.mapping.jpa.algo.MapsRemoverAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.RemoverAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.Rf2DeltaLoaderAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.Rf2SnapshotLoaderAlgorithm;
import org.ihtsdo.otf.mapping.jpa.algo.SimpleLoaderAlgorithm;
import org.ihtsdo.otf.mapping.jpa.handlers.IndexViewerHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * REST implementation for content service.
 */
@Path("/content")
@Api(value = "/content", description = "Operations to get RF2 content for a terminology.")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class ContentServiceRestImpl extends RootServiceRestImpl
		implements ContentServiceRest {

	/** The security service. */
	private SecurityService securityService;

	/**
	 * Instantiates an empty {@link ContentServiceRestImpl}.
	 *
	 * @throws Exception
	 *             the exception
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
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
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

			final Concept c = contentService.getConcept(terminologyId,
					terminology, terminologyVersion);

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
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
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

			final String version = metadataService
					.getLatestVersion(terminology);
			final Concept c = contentService.getConcept(terminologyId,
					terminology, version);
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
			return contentService.findConceptsForQuery(query,
					new PfsParameterJpa());

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
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  public SearchResultList findDescendantConcepts(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("terminologyId") String terminologyId,
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /concept/" + terminology + "/"
						+ terminologyVersion + "/id/" + terminologyId
						+ "/descendants");

    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
			authorizeApp(authToken, MapUserRole.VIEWER,
					"find descendant concepts", securityService);

      // want all descendants, do not use PFS parameter
			return contentService.findDescendantConcepts(terminologyId,
					terminology, terminologyVersion, null);
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
	 * @see
	 * org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#findChildConcepts(
   * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("/concept/id/{terminology}/{terminologyVersion}/{terminologyId}/children")
  @ApiOperation(value = "Find concept children.", notes = "Gets a list of search results for each child concept.", response = Concept.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  public SearchResultList findChildConcepts(
    @ApiParam(value = "Concept terminology id, e.g. 22298006", required = true) @PathParam("terminologyId") String id,
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /concept/" + terminology + "/"
						+ terminologyVersion + "/id/" + id.toString()
						+ "/descendants");

    final ContentService contentService = new ContentServiceJpa();
    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "find child concepts",
          securityService);

      String isaId = "";
      final Map<String, String> relTypesMap = metadataService
					.getHierarchicalRelationshipTypes(terminology,
							terminologyVersion);
			for (final Map.Entry<String, String> entry : relTypesMap
					.entrySet()) {
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
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  public SearchResultList findDeltaConceptsForTerminology(
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken,
    @ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
				.info("RESTful call (Content): /terminology/id/" + terminology
						+ "/" + terminologyVersion + "/delta");

    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "find delta concepts",
          securityService);

      final ConceptList conceptList = contentService
					.getConceptsModifiedSinceDate(terminology, null,
							pfsParameter);
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
					for (final LanguageRefSetMember l : d
							.getLanguageRefSetMembers()) {
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
				result.setTerminologyVersion(
						modifiedConcept ? "Modified" : "New");
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
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
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
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
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
			authorizeApp(authToken, MapUserRole.VIEWER,
					"get index pages for domain", securityService);

      return indexViewerHandler.getIndexPagesForIndex(terminology,
          terminologyVersion, index);

    } catch (Exception e) {
			handleException(e,
					"trying to get the page names for the given index");
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
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  public String getIndexViewerDetailsForLink(
    @ApiParam(value = "Concept terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Concept terminology version, e.g. 20140731", required = true) @PathParam("terminologyVersion") String terminologyVersion,
    @ApiParam(value = "Domain/Index within terminology", required = true) @PathParam("domain") String domain,
    @ApiParam(value = "Object link", required = true) @PathParam("link") String link,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /index/" + terminology + "/"
						+ terminologyVersion + "/" + domain + "/details/"
						+ link);

    final IndexViewerHandler indexViewerHandler = new IndexViewerHandler();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER,
					"get index viewer details for object link",
					securityService);

      return indexViewerHandler.getDetailsAsHtmlForLink(terminology,
          terminologyVersion, domain, link);

    } catch (Exception e) {
			handleException(e,
					"trying to get index viewer details for object link");
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
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
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
						+ terminologyVersion + "/" + domain + "/" + searchField
						+ "/" + subSearchField + "/" + subSubSearchField + "/"
						+ allFlag);

    final IndexViewerHandler indexViewerHandler = new IndexViewerHandler();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER, "find index entries",
          securityService);

			final SearchResultList searchResultList = indexViewerHandler
					.findIndexEntries(terminology, terminologyVersion, domain,
							searchField, subSearchField, subSubSearchField,
							allFlag);
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
    @ApiParam(value = "Workflow status", required = true) @QueryParam("workflowStatus") String workflowStatus,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

		Logger.getLogger(getClass())
				.info("RESTful call (Content): /map/record/");

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "remove map record",
        securityService);

		try (final MapRecordRf2ComplexMapLoaderAlgorithm algo = new MapRecordRf2ComplexMapLoaderAlgorithm(
				inputFile, memeberFlag, recordFlag, workflowStatus);) {

      algo.compute();

			Logger.getLogger(getClass()).info(
					"Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

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
    @ApiParam(value = "Workflow status", required = true) @QueryParam("workflowStatus") String workflowStatus,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

		Logger.getLogger(getClass())
				.info("RESTful call (Content): /map/record/");

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "load map record RF2 simple", securityService);

		try (final MapRecordRf2SimpleMapLoaderAlgorithm algo = new MapRecordRf2SimpleMapLoaderAlgorithm(
				inputFile, memeberFlag, recordFlag, workflowStatus);) {

      algo.compute();

			Logger.getLogger(getClass()).info(
					"Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

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
				.info("RESTful call (Content): /terminology/load/claml/"
						+ terminology + "/" + version + " from input file "
						+ inputFile);

    // Track system level information
    long startTimeOrig = System.nanoTime();

		authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
				"load CLAML terminology", securityService);

		try (final ClamlLoaderAlgorithm algo = new ClamlLoaderAlgorithm(
				terminology, version, inputFile);) {

      algo.compute();

			Logger.getLogger(getClass()).info(
					"Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
			handleException(e,
					"trying to load terminology claml from directory");
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

            Logger.getLogger(getClass()).info(
                    "Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

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
				.info("RESTful call (Content): /terminology/load/gmdn/"
						+ version + " from input directory "
						+ inputDir);

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

		authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
				"load GMDN terminology", securityService);

		try (final GmdnLoaderAlgorithm algo = new GmdnLoaderAlgorithm(
				version, inputDir);) {

      algo.compute();

			Logger.getLogger(getClass()).info(
					"Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
			handleException(e,
					"trying to load terminology GMDN from directory");
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

		try (final MapsRemoverAlgorithm algo = new MapsRemoverAlgorithm(
				refsetId);) {

      algo.compute();

			Logger.getLogger(getClass()).info(
					"Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

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

		Logger.getLogger(getClass())
				.info("RESTful call (Content): /terminology/" + terminology
						+ "/" + version);

    // Track system level information
    long startTimeOrig = System.nanoTime();

		final String userName = authorizeApp(authToken,
				MapUserRole.ADMINISTRATOR, "remove terminology",
				securityService);

		try (final RemoverAlgorithm algo = new RemoverAlgorithm(terminology,
				version);) {

      // Remove terminology
			Logger.getLogger(getClass()).info(
					"  Remove terminology for  " + terminology + "/" + version);

      algo.setLastModifiedBy(userName);
      algo.compute();

			Logger.getLogger(getClass()).info(
					"Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

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

			Logger.getLogger(getClass()).info(
					"Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
			handleException(e,
					"trying to load terminology delta from RF2 directory");
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/terminology/load/rf2/snapshot/aws/{terminology}/{version}")
  @Consumes({
      MediaType.TEXT_PLAIN
  })
  @ApiOperation(value = "Loads terminology RF2 snapshot from directory", notes = "Loads terminology RF2 snapshot from directory for specified terminology and version")
  public void loadTerminologyRf2SnapshotAws(
    @ApiParam(value = "Terminology, e.g. SNOMED CT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Version, e.g. 20170131", required = true) @PathParam("version") String version,
    @ApiParam(value = "Aws Zip File Name, e.g. filename with full path", required = true) @QueryParam("awsZipFileName") String awsZipFileName,
    @ApiParam(value = "Calculate tree positions", required = false) @QueryParam("treePositions") Boolean treePositions,
    @ApiParam(value = "Send notification", required = false) @QueryParam("sendNotification") Boolean sendNotification,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Content): /terminology/load/rf2/snapshot/aws/"
            + " awsFileName " + awsZipFileName);
/*
    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "load RF2 snapshot terminology", securityService);

    File placementDir = null;
    Rf2SnapshotLoaderAlgorithm algo = null;

    try {
      // Access zipped awsFile
      AmazonS3 s3Client =
          AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1)
              .withCredentials(new InstanceProfileCredentialsProvider(false))
              .build();

      final String bucketName = "release-ihtsdo-prod-published";
      S3Object s3object = s3Client.getObject(bucketName, awsFileName);

      // Unzip awsFile to temp directory
      File tempDir = FileUtils.getTempDirectory();
      placementDir = new File(tempDir.getAbsolutePath() + File.separator
          + "TerminologyLoad_" + startTimeOrig);
      placementDir.mkdir();

      S3ObjectInputStream inputStream = s3object.getObjectContent();
      File zippedFile = new File(placementDir,
          awsFileName.substring(awsFileName.lastIndexOf('/') + 1));
      FileUtils.copyInputStreamToFile(inputStream, zippedFile);
      inputStream.close();

      // UNZIP to Placement
      unzipToDirectory(zippedFile, placementDir);
      FileUtils.deleteDirectory(placementDir);

      // Load content with input pulled from S3
      algo = new Rf2SnapshotLoaderAlgorithm(terminology, version,
          placementDir.getAbsolutePath(), treePositions, sendNotification);

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
    */
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/terminology/load/rf2/snapshot/{terminology}/{version}")
	@Consumes({ MediaType.TEXT_PLAIN })
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

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "load RF2 snapshot terminology", securityService);

		try (final Rf2SnapshotLoaderAlgorithm algo = new Rf2SnapshotLoaderAlgorithm(
				terminology, version, inputDir, localTreePostions, localSendNotification);) {

      algo.compute();

			Logger.getLogger(getClass()).info(
					"Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

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
				.info("RESTful call (Content): /terminology/load/simple/ "
						+ terminology + ", " + version
						+ " from input file " + inputFile);

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "load simple terminology", securityService);

		try (final SimpleLoaderAlgorithm algo = new SimpleLoaderAlgorithm(
				terminology, version, inputFile, null);) {

      algo.compute();

			Logger.getLogger(getClass()).info(
					"Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

    } catch (Exception e) {
			handleException(e,
					"trying to load simple terminology from directory");
    }
  }

  /* see superclass */
  @Override
  @PUT
  @Path("/terminology/reload/rf2/snapshot/{terminology}/{version}")
	@Consumes({ MediaType.TEXT_PLAIN })
  @ApiOperation(value = "Removes and Loads terminology RF2 snapshot from directory", notes = "Removes and loads terminology RF2 snapshot from directory for specified terminology and version")
  public void reloadTerminologyRf2Snapshot(
    @ApiParam(value = "Terminology, e.g. SNOMEDCT_US", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Version, e.g. 2014_09_01", required = true) @PathParam("version") String version,
    @ApiParam(value = "RF2 input directory", required = true) String inputDir,
    @ApiParam(value = "Calcualte tree positions", required = false) @QueryParam("treePositions") Boolean treePositions,
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

		try (final RemoverAlgorithm removeAlgo = new RemoverAlgorithm(terminology,
				version);) {

      // Remove terminology
			Logger.getLogger(getClass()).info(
					"  Remove terminology for  " + terminology + "/" + version);

      removeAlgo.setLastModifiedBy(userName);
      removeAlgo.compute();

      Logger.getLogger(getClass()).info(
          "Remove Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

      securityService.addLogEntry(userName, terminology, version, null,
          "REMOVER", "Remove terminology");

      // if no errors try to load
			try (final Rf2SnapshotLoaderAlgorithm loadAlgo = new Rf2SnapshotLoaderAlgorithm(
					terminology, version, inputDir, localTreePostions, localSendNotification);) {

        loadAlgo.compute();

        Logger.getLogger(getClass()).info(
            "Load Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

        securityService.addLogEntry(userName, terminology, version, null,
            "LOAD", "Load " + terminology + " " + version);

      } catch (Exception e) {
        handleException(e,
            "trying to reload terminology snapshot from RF2 directory");
      }

    } catch (Exception e) {
      handleException(e, "trying to remove terminology");
    } finally {
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

		Logger.getLogger(getClass())
				.info("RESTful call (Content): /map/record/");

    // Track system level information
    long startTimeOrig = System.nanoTime();

    authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
        "load map record RF2 simple", securityService);

		try (final MapsRemoverAlgorithm removeAlgo = new MapsRemoverAlgorithm(
				refsetId);) {

      removeAlgo.compute();

      Logger.getLogger(getClass()).info(
          "Remove Elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));

      // if remove was successful do add
			try (final MapRecordRf2SimpleMapLoaderAlgorithm loadAlgo = new MapRecordRf2SimpleMapLoaderAlgorithm(
					inputFile, memeberFlag, recordFlag, workflowStatus);) {

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

    if (terminology.equals("SNOMED CT")) {
      terminology = "InternationalRF2";
    } else if (terminology.startsWith("ICNP")) {
      terminology = terminology.substring(0, terminology.indexOf(" "))
          + terminology.substring(terminology.indexOf(" ") + 1);
    }

    int year = Calendar.getInstance().get(Calendar.YEAR);
    String currentYear = Integer.toString(year);
    String nextYear = Integer.toString(year + 1);
    String lastYear = Integer.toString(year - 1);

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /terminology/" + terminology + "/");

    final String bucketName = "release-ihtsdo-prod-published";

    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER,
          "find version for terminology", securityService);

      /*
       * // Connect to server AmazonS3 s3Client =
       * AmazonS3ClientBuilder.standard() .withRegion(Regions.US_EAST_1)
       * .withCredentials(new
       * InstanceProfileCredentialsProvider(false)).build();
       * 
       * 
       * List<S3ObjectSummary> fullKeyList = new ArrayList<S3ObjectSummary>();
       * ObjectListing objects = s3Client.listObjects(bucketName, "zip");
       * fullKeyList = objects.getObjectSummaries(); objects =
       * s3Client.listNextBatchOfObjects(objects); int loopCounter = 0;
       * 
       * while (objects.isTruncated()){
       * fullKeyList.addAll(objects.getObjectSummaries()); objects =
       * s3Client.listNextBatchOfObjects(objects);
       * 
       * Logger.getLogger(MappingServiceRestImpl.class).info("DDD1 at loop #" +
       * ++loopCounter + " with keList.size(): " + fullKeyList.size()); }
       * fullKeyList.addAll(objects.getObjectSummaries());
       * 
       * TerminologyVersionList returnList = new TerminologyVersionList(); for
       * (S3ObjectSummary obj : fullKeyList) { if (obj.getKey().startsWith("international") && obj.getKey().endsWith("zip")
       * && obj.getKey().contains(terminology) &&
       * (obj.getKey().contains(lastYear) || obj.getKey().contains(currentYear)
       * || obj.getKey().contains(nextYear)) {
       * returnList.addTerminologyVersion(new TerminologyVersion(terminology,
       * obj.getKey())); } }
       */

      Set<String> testSet = new HashSet<>();
      testSet.add("SnomedCT_BetaRF1Release_INT_20150731.zip");
      testSet.add("SnomedCT_BetaRF2Release_INT_20150731.zip");
      testSet.add("SnomedCT_GMDNMapRelease_INT_20150731.zip");
      testSet.add("SnomedCT_GMDNMapRelease_INT_20160131.zip");
      testSet.add("SnomedCT_GMDNMapRelease_INT_20160731.zip");
      testSet.add("SnomedCT_GMDNMapRelease_Production_20170228T120000.zip");
      testSet.add("SnomedCT_GMDNMapRelease_Production_20170908T120000Z.zip");
      testSet.add("SnomedCT_GMDNMapRelease_Production_INT_20170228T120000.zip");
      testSet.add("SnomedCT_GPFPICPC2_Baseline_INT_20150930.zip");
      testSet.add("SnomedCT_GPFPICPC2_Baseline_INT_20150930_orig.zip");
      testSet.add("SnomedCT_GPFPICPC2_PRODUCTION_20171018T120000Z.zip");
      testSet.add("SnomedCT_GPFPICPC2_Production_20170331T120000.zip");
      testSet.add("SnomedCT_GPFPICPC2_Production_INT_20160131.zip");
      testSet.add("SnomedCT_GPFPICPC2_Production_INT_20160731.zip");
      testSet.add("SnomedCT_GeneralDentistry_PRODUCTION_20171018T120000Z.zip");
      testSet.add("SnomedCT_GeneralDentistry_Production_20170324T120000.zip");
      testSet.add("SnomedCT_ICD-9-CM_20160501.zip");
      testSet
          .add("SnomedCT_ICNPDiagnosesRelease_PRODUCTION_20171130T120000Z.zip");
      testSet
          .add("SnomedCT_ICNPDiagnosesRelease_Production_20170324T120000.zip");
      testSet.add("SnomedCT_ICNPDiagnosesRelease_Production_INT_20160131.zip");
      testSet.add("SnomedCT_ICNPDiagnosesRelease_Production_INT_20160731.zip");
      testSet.add(
          "SnomedCT_ICNPDiagnosesRelease_Production_INT_20170324T120000.zip");
      testSet.add(
          "SnomedCT_ICNPInterventionsRelease_PRODUCTION_20171130T120000Z.zip");
      testSet.add(
          "SnomedCT_ICNPInterventionsRelease_Production_20170317T120000.zip");
      testSet
          .add("SnomedCT_ICNPInterventionsRelease_Production_INT_20160131.zip");
      testSet
          .add("SnomedCT_ICNPInterventionsRelease_Production_INT_20160731.zip");
      testSet.add("SnomedCT_ICNPRelease_Baseline_INT_20150901.zip");
      testSet.add("SnomedCT_IdentifierRefset_DEPRECATED_20170731T120000Z.zip");
      testSet.add(
          "SnomedCT_InternationalRF2_PRODUCTION_20170731T120000Z (RECALLED).zip");
      testSet.add(
          "SnomedCT_InternationalRF2_PRODUCTION_20170731T120000Z_publishing_manually (RECALLED).zip");
      testSet.add("SnomedCT_InternationalRF2_PRODUCTION_20170731T150000Z.zip");
      testSet.add(
          "SnomedCT_InternationalRF2_PRODUCTION_20170731T150000Z_UPDATEDNAMES.zip");
      testSet.add("SnomedCT_InternationalRF2_Production_20170131T120000.zip");
      testSet.add(
          "SnomedCT_InternationalRF2_Production_20170131T120000WithoutRT.zip");
      testSet.add("SnomedCT_LOINCRF2_PRODUCTION_20170831T120000Z.zip");
      testSet.add("SnomedCT_LOINC_AlphaPhase3_HumanReadable_INT_20160401.zip");
      testSet.add("SnomedCT_LOINC_AlphaPhase3_INT_20160401.zip");
      testSet.add("SnomedCT_LOINC_AlphaPhase3_OWL_INT_20160401.zip");
      testSet.add(
          "SnomedCT_LOINC_TechnologyPreview_HumanReadable_INT_20150801.zip");
      testSet.add("SnomedCT_LOINC_TechnologyPreview_INT_20150801.zip");
      testSet.add("SnomedCT_LOINC_TechnologyPreview_OWL_INT_20150801.zip");
      testSet.add("SnomedCT_MedicalDevicesTechnologyPreview_INT_20130131.zip");
      testSet.add("SnomedCT_NursingActivities_PRODUCTION_20171011T120000Z.zip");
      testSet
          .add("SnomedCT_NursingHealthIssues_PRODUCTION_20171011T120000Z.zip");
      testSet.add("SnomedCT_Odontogram_PRODUCTION_20171011T120000Z.zip");
      testSet.add("SnomedCT_RF1CompatibilityPackage_INT_20160131.zip");
      testSet.add("SnomedCT_RF1Release_INT_20150131.zip");
      testSet.add("SnomedCT_RF1Release_INT_20150731.zip");
      testSet.add("SnomedCT_RF1Release_INT_20160131 v1.0.zip");
      testSet.add("SnomedCT_RF1Release_INT_20160131.zip");
      testSet.add("SnomedCT_RF1Release_INT_20160731(RECALLED).zip");
      testSet.add("SnomedCT_RF1Release_INT_20160731.zip");
      testSet.add("SnomedCT_RF2Release_INT_20150131(RECALLED).zip");
      testSet.add("SnomedCT_RF2Release_INT_20150131.zip");
      testSet.add("SnomedCT_RF2Release_INT_20150731.zip");
      testSet.add("SnomedCT_RF2Release_INT_20160131.zip");
      testSet.add("SnomedCT_RF2Release_INT_20160731(RECALLED).zip");
      testSet.add("SnomedCT_RF2Release_INT_20160731.zip");
      testSet.add("SnomedCT_Release-es_INT_20140430.zip");
      testSet.add("SnomedCT_Release_INT_20140131.zip");
      testSet.add("SnomedCT_Release_INT_20140731.zip");
      testSet.add("SnomedCT_SpanishRelease-es_INT_20141031.zip");
      testSet.add("SnomedCT_SpanishRelease-es_INT_20150430.zip");
      testSet.add("SnomedCT_SpanishRelease-es_INT_20151031.zip");
      testSet.add("SnomedCT_SpanishRelease-es_INT_20160430.zip");
      testSet.add("SnomedCT_SpanishRelease-es_INT_20161031.zip");
      testSet.add("SnomedCT_SpanishRelease-es_Production_20170430T120000.zip");
      testSet.add("SnomedCT_SpanishRelease-es_Production_20171031T120000Z.zip");
      testSet.add("xSnomedCT_InternationalRF2_ALPHA_20170731T120000Z.zip");
      testSet.add("xSnomedCT_InternationalRF2_ALPHA_20180131T120000Z.zip");
      testSet.add("xSnomedCT_InternationalRF2_BETA_20170731T120000Z.zip");
      testSet.add("xSnomedCT_InternationalRF2_BETA_20180131T120000Z.zip");
      testSet.add("xSnomedCT_InternationalRF2_Beta_20170131T120000.zip");
      testSet.add("xSnomedCT_LOINC_Beta_20170331T120000.zip");
      testSet.add("xSnomedCT_MRCMReferenceSets_Beta_20170210T120000.zip");
      testSet.add("xSnomedCT_RF1Release_INT_20160131.zip");
      testSet.add("xSnomedCT_RF1Release_INT_20160731.zip");
      testSet.add("xSnomedCT_RF2Release_INT_20160131.zip");
      testSet.add("xSnomedCT_RF2Release_INT_20160731.zip");
      testSet.add("xSnomedCT_RF2Release_INT_20160731_alpha.zip");
      testSet.add("xSnomedCT_RF2Release_INT_20170131.zip");
      testSet.add("xSnomedCT_SpanishRelease-es_Beta__20170430T120000.zip");
      testSet.add("xSnomedCT_SpanishRelease-es_INT_20160430.zip");
      testSet.add("xSnomedCT_SpanishRelease-es_INT_20161031.zip");
      testSet.add("xSnomedCT_SpanishRelease-es_Production_20170430T120000.zip");
      testSet
          .add("xSnomedCT_SpanishRelease-es_Production_20171031T120000Z.zip");
      testSet.add(
          "xSnomedCT_StarterSetTranslationPackage_Alpha_20170421T120000.zip");

      TerminologyVersionList returnList = new TerminologyVersionList();

      for (String obj : testSet) {
        if (obj.endsWith("zip") && obj.contains(terminology)
            && (obj.contains(lastYear) || obj.contains(currentYear)
                || obj.contains(nextYear))) {
          returnList
              .addTerminologyVersion(new TerminologyVersion(obj, terminology));
        }
      }

      returnList.removeDupVersions();

      // want all descendants, do not use PFS parameter
      return returnList;
    } catch (Exception e) {
      handleException(e, "trying to find descendant concepts");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  @Override
  @GET
  @Path("/terminology/scope/{terminology}/{version}")
  @ApiOperation(value = "Find versions for terminology.", notes = "Gets a list of recont versions for a given terminology.", response = Concept.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public TerminologyVersionList getTerminologyVersionScopes(
    @ApiParam(value = "Terminology name, e.g. SNOMED CT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Version name, e.g. 20170131", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(ContentServiceRestImpl.class)
        .info("RESTful call (Content): /terminology/scope/" + terminology + "/"
            + version + "/");

    if (!terminology.equals("SNOMED CT")) {
      throw new Exception("Scope not relevant to handle " + terminology);
    } else {
      terminology = "InternationalRF2";
    }

    final String bucketName = "release-ihtsdo-prod-published";

    final ContentService contentService = new ContentServiceJpa();
    try {
      // authorize call
      authorizeApp(authToken, MapUserRole.VIEWER,
          "find version for terminology", securityService);

      /*
       * // Connect to server AmazonS3 s3Client =
       * AmazonS3ClientBuilder.standard() .withRegion(Regions.US_EAST_1)
       * .withCredentials(new
       * InstanceProfileCredentialsProvider(false)).build();
       * 
       * 
       * List<S3ObjectSummary> fullKeyList = new ArrayList<S3ObjectSummary>();
       * ObjectListing objects = s3Client.listObjects(bucketName, "zip");
       * fullKeyList = objects.getObjectSummaries(); objects =
       * s3Client.listNextBatchOfObjects(objects); int loopCounter = 0;
       * 
       * while (objects.isTruncated()){
       * fullKeyList.addAll(objects.getObjectSummaries()); objects =
       * s3Client.listNextBatchOfObjects(objects);
       * 
       * Logger.getLogger(MappingServiceRestImpl.class).info("DDD1 at loop #" +
       * ++loopCounter + " with keList.size(): " + fullKeyList.size()); }
       * fullKeyList.addAll(objects.getObjectSummaries());
       * 
       * TerminologyVersionList returnList = new TerminologyVersionList(); for
       * (S3ObjectSummary obj : fullKeyList) { if (obj.getKey().endsWith("zip")
       * && obj.getKey().contains(terminology) &&
       * (obj.getKey().contains(lastYear) ||
       * obj.getKey().contains(currentYear))) {
       * returnList.addTerminologyVersion(new TerminologyVersion(terminology,
       * obj.getKey())); } }
       */

      Set<String> testSet = new HashSet<>();
      testSet.add("SnomedCT_BetaRF1Release_INT_20150731.zip");
      testSet.add("SnomedCT_BetaRF2Release_INT_20150731.zip");
      testSet.add("SnomedCT_GMDNMapRelease_INT_20150731.zip");
      testSet.add("SnomedCT_GMDNMapRelease_INT_20160131.zip");
      testSet.add("SnomedCT_GMDNMapRelease_INT_20160731.zip");
      testSet.add("SnomedCT_GMDNMapRelease_Production_20170228T120000.zip");
      testSet.add("SnomedCT_GMDNMapRelease_Production_20170908T120000Z.zip");
      testSet.add("SnomedCT_GMDNMapRelease_Production_INT_20170228T120000.zip");
      testSet.add("SnomedCT_GPFPICPC2_Baseline_INT_20150930.zip");
      testSet.add("SnomedCT_GPFPICPC2_Baseline_INT_20150930_orig.zip");
      testSet.add("SnomedCT_GPFPICPC2_PRODUCTION_20171018T120000Z.zip");
      testSet.add("SnomedCT_GPFPICPC2_Production_20170331T120000.zip");
      testSet.add("SnomedCT_GPFPICPC2_Production_INT_20160131.zip");
      testSet.add("SnomedCT_GPFPICPC2_Production_INT_20160731.zip");
      testSet.add("SnomedCT_GeneralDentistry_PRODUCTION_20171018T120000Z.zip");
      testSet.add("SnomedCT_GeneralDentistry_Production_20170324T120000.zip");
      testSet.add("SnomedCT_ICD-9-CM_20160501.zip");
      testSet
          .add("SnomedCT_ICNPDiagnosesRelease_PRODUCTION_20171130T120000Z.zip");
      testSet
          .add("SnomedCT_ICNPDiagnosesRelease_Production_20170324T120000.zip");
      testSet.add("SnomedCT_ICNPDiagnosesRelease_Production_INT_20160131.zip");
      testSet.add("SnomedCT_ICNPDiagnosesRelease_Production_INT_20160731.zip");
      testSet.add(
          "SnomedCT_ICNPDiagnosesRelease_Production_INT_20170324T120000.zip");
      testSet.add(
          "SnomedCT_ICNPInterventionsRelease_PRODUCTION_20171130T120000Z.zip");
      testSet.add(
          "SnomedCT_ICNPInterventionsRelease_Production_20170317T120000.zip");
      testSet
          .add("SnomedCT_ICNPInterventionsRelease_Production_INT_20160131.zip");
      testSet
          .add("SnomedCT_ICNPInterventionsRelease_Production_INT_20160731.zip");
      testSet.add("SnomedCT_ICNPRelease_Baseline_INT_20150901.zip");
      testSet.add("SnomedCT_IdentifierRefset_DEPRECATED_20170731T120000Z.zip");
      testSet.add(
          "SnomedCT_InternationalRF2_PRODUCTION_20170731T120000Z (RECALLED).zip");
      testSet.add(
          "SnomedCT_InternationalRF2_PRODUCTION_20170731T120000Z_publishing_manually (RECALLED).zip");
      testSet.add("SnomedCT_InternationalRF2_PRODUCTION_20170731T150000Z.zip");
      testSet.add(
          "SnomedCT_InternationalRF2_PRODUCTION_20170731T150000Z_UPDATEDNAMES.zip");
      testSet.add("SnomedCT_InternationalRF2_Production_20170131T120000.zip");
      testSet.add(
          "SnomedCT_InternationalRF2_Production_20170131T120000WithoutRT.zip");
      testSet.add("SnomedCT_LOINCRF2_PRODUCTION_20170831T120000Z.zip");
      testSet.add("SnomedCT_LOINC_AlphaPhase3_HumanReadable_INT_20160401.zip");
      testSet.add("SnomedCT_LOINC_AlphaPhase3_INT_20160401.zip");
      testSet.add("SnomedCT_LOINC_AlphaPhase3_OWL_INT_20160401.zip");
      testSet.add(
          "SnomedCT_LOINC_TechnologyPreview_HumanReadable_INT_20150801.zip");
      testSet.add("SnomedCT_LOINC_TechnologyPreview_INT_20150801.zip");
      testSet.add("SnomedCT_LOINC_TechnologyPreview_OWL_INT_20150801.zip");
      testSet.add("SnomedCT_MedicalDevicesTechnologyPreview_INT_20130131.zip");
      testSet.add("SnomedCT_NursingActivities_PRODUCTION_20171011T120000Z.zip");
      testSet
          .add("SnomedCT_NursingHealthIssues_PRODUCTION_20171011T120000Z.zip");
      testSet.add("SnomedCT_Odontogram_PRODUCTION_20171011T120000Z.zip");
      testSet.add("SnomedCT_RF1CompatibilityPackage_INT_20160131.zip");
      testSet.add("SnomedCT_RF1Release_INT_20150131.zip");
      testSet.add("SnomedCT_RF1Release_INT_20150731.zip");
      testSet.add("SnomedCT_RF1Release_INT_20160131 v1.0.zip");
      testSet.add("SnomedCT_RF1Release_INT_20160131.zip");
      testSet.add("SnomedCT_RF1Release_INT_20160731(RECALLED).zip");
      testSet.add("SnomedCT_RF1Release_INT_20160731.zip");
      testSet.add("SnomedCT_RF2Release_INT_20150131(RECALLED).zip");
      testSet.add("SnomedCT_RF2Release_INT_20150131.zip");
      testSet.add("SnomedCT_RF2Release_INT_20150731.zip");
      testSet.add("SnomedCT_RF2Release_INT_20160131.zip");
      testSet.add("SnomedCT_RF2Release_INT_20160731(RECALLED).zip");
      testSet.add("SnomedCT_RF2Release_INT_20160731.zip");
      testSet.add("SnomedCT_Release-es_INT_20140430.zip");
      testSet.add("SnomedCT_Release_INT_20140131.zip");
      testSet.add("SnomedCT_Release_INT_20140731.zip");
      testSet.add("SnomedCT_SpanishRelease-es_INT_20141031.zip");
      testSet.add("SnomedCT_SpanishRelease-es_INT_20150430.zip");
      testSet.add("SnomedCT_SpanishRelease-es_INT_20151031.zip");
      testSet.add("SnomedCT_SpanishRelease-es_INT_20160430.zip");
      testSet.add("SnomedCT_SpanishRelease-es_INT_20161031.zip");
      testSet.add("SnomedCT_SpanishRelease-es_Production_20170430T120000.zip");
      testSet.add("SnomedCT_SpanishRelease-es_Production_20171031T120000Z.zip");
      testSet.add("xSnomedCT_InternationalRF2_ALPHA_20170731T120000Z.zip");
      testSet.add("xSnomedCT_InternationalRF2_ALPHA_20180131T120000Z.zip");
      testSet.add("xSnomedCT_InternationalRF2_BETA_20170731T120000Z.zip");
      testSet.add("xSnomedCT_InternationalRF2_BETA_20180131T120000Z.zip");
      testSet.add("xSnomedCT_InternationalRF2_Beta_20170131T120000.zip");
      testSet.add("xSnomedCT_LOINC_Beta_20170331T120000.zip");
      testSet.add("xSnomedCT_MRCMReferenceSets_Beta_20170210T120000.zip");
      testSet.add("xSnomedCT_RF1Release_INT_20160131.zip");
      testSet.add("xSnomedCT_RF1Release_INT_20160731.zip");
      testSet.add("xSnomedCT_RF2Release_INT_20160131.zip");
      testSet.add("xSnomedCT_RF2Release_INT_20160731.zip");
      testSet.add("xSnomedCT_RF2Release_INT_20160731_alpha.zip");
      testSet.add("xSnomedCT_RF2Release_INT_20170131.zip");
      testSet.add("xSnomedCT_SpanishRelease-es_Beta__20170430T120000.zip");
      testSet.add("xSnomedCT_SpanishRelease-es_INT_20160430.zip");
      testSet.add("xSnomedCT_SpanishRelease-es_INT_20161031.zip");
      testSet.add("xSnomedCT_SpanishRelease-es_Production_20170430T120000.zip");
      testSet
          .add("xSnomedCT_SpanishRelease-es_Production_20171031T120000Z.zip");
      testSet.add(
          "xSnomedCT_StarterSetTranslationPackage_Alpha_20170421T120000.zip");

      TerminologyVersionList returnList = new TerminologyVersionList();

      for (String obj : testSet) {
        if (obj.endsWith("zip") && obj.contains(terminology)
            && obj.contains(version)) {
          TerminologyVersion tv = new TerminologyVersion(obj, terminology);
          tv.identifyScope();
          returnList.addTerminologyVersion(tv);
        }
      }

      returnList.removeDupScopes();

      // want all descendants, do not use PFS parameter
      return returnList;
    } catch (Exception e) {
      handleException(e, "trying to find descendant concepts");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  private void unzipToDirectory(File zippedFile, File placementDir)
    throws IOException {

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
}
