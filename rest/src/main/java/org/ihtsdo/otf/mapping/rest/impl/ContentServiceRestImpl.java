package org.ihtsdo.otf.mapping.rest.impl;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
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

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * REST implementation for content service.
 */
@Path("/content")
@Api(value = "/content", description = "Operations to get RF2 content for a terminology.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class ContentServiceRestImpl extends RootServiceRestImpl implements ContentServiceRest {

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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#getConcept(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#getConcept(java.lang.String, java.lang.String, java.lang.String)
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#findConceptsForQuery(java.lang.String, java.lang.String)
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#findDescendantConcepts(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#findChildConcepts(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#findDeltaConceptsForTerminology(java.lang.String, java.lang.String, java.lang.String, org.ihtsdo.otf.mapping.helpers.PfsParameterJpa)
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

        // first pass check to see if this is a new concept or a modified
        // concept
        // this will erroneously report NEW concept if all of the descriptions,
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#getIndexDomains(java.lang.String, java.lang.String, java.lang.String)
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#getIndexViewerPagesForIndex(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#getIndexViewerDetailsForLink(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
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

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.rest.impl.ContentServiceRest#findIndexViewerEntries(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, java.lang.String)
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

}
