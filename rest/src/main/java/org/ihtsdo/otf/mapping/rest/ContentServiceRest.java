package org.ihtsdo.otf.mapping.rest;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
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
 * REST implementation for content service
 */
@Path("/content")
@Api(value = "/content", description = "Operations to retrieve RF2 content.")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class ContentServiceRest extends RootServiceRest {

	/** The security service. */
	private SecurityService securityService;

	/**
	 * Instantiates an empty {@link ContentServiceRest}.
	 */
	public ContentServiceRest() throws Exception {
		securityService = new SecurityServiceJpa();
	}

	/**
	 * Returns the concept for id, terminology, and terminology version
	 * 
	 * @param terminologyId
	 *            the terminology id
	 * @param terminology
	 *            the concept terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @param authToken 
	 * @return the concept
	 */
	@GET
	@Path("/concept/id/{terminology}/{version}/{terminologyId}")
	@ApiOperation(value = "Get concept by id, version, and terminology", notes = "Returns a concept in either xml json given a concept id, terminology, and terminology version.", response = Concept.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Concept getConcept(
			@ApiParam(value = "Concept terminology id", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "Concept terminology", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "Concept terminology version", required = true) @PathParam("version") String terminologyVersion,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(ContentServiceRest.class).info(
				"RESTful call (Content): /concept/" + terminology + "/"
						+ terminologyVersion + "/id/" + terminologyId);

		try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve the concept.").build());
			
  		
			ContentService contentService = new ContentServiceJpa();
			Concept c = contentService.getConcept(terminologyId, terminology,
					terminologyVersion);

			if (c != null) {
				// Make sure to read descriptions and relationships (prevents
				// serialization error)
				for (Description d : c.getDescriptions()) {
					d.getLanguageRefSetMembers();
				}
				for (Relationship r : c.getRelationships()) {
					r.getDestinationConcept();
				}
			}

			contentService.close();
			return c;
		} catch (Exception e) { 
			handleException(e, "trying to retrieve a concept");
			return null;
		}

	}


	/**
	 * Returns the concept for id, terminology. Looks in the latest version of
	 * the terminology.
	 * 
	 * @param terminologyId
	 *            the id
	 * @param terminology
	 *            the concept terminology
	 * @param authToken 
	 * @return the concept
	 */
	@GET
	@Path("/concept/id/{terminology}/{terminologyId}")
	@ApiOperation(value = "Get the latest version of a concept", notes = "Returns a concept given a concept id, terminology - assumes latest terminology version.", response = Concept.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Concept getConcept(
			@ApiParam(value = "Concept terminologyId", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "Concept terminology", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(ContentServiceRest.class).info(
				"RESTful call (Content): /concept/" + terminology + "/id/"
						+ terminologyId);

		try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve the latest version concept.").build());
			
  		
			ContentService contentService = new ContentServiceJpa();
			MetadataService metadataService = new MetadataServiceJpa();
			String version = metadataService.getLatestVersion(terminology);
			Concept c = contentService.getConcept(terminologyId, terminology,
					version);
			c.getDescriptions();
			c.getRelationships();
			metadataService.close();
			contentService.close();
			return c;

		} catch (Exception e) {
			handleException(e, "trying to retrieve the latest version concept");
			return null;
		}
	}

	/**
	 * Returns the concept for search string.
	 * 
	 * @param searchString
	 *            the lucene search string
	 * @param authToken 
	 * @return the concept for id
	 */
	@GET
	@Path("/concept/query/{string}")
	@ApiOperation(value = "Find concepts by search query", notes = "Returns a list of concepts that are related to given lucene search query.", response = String.class)
	public SearchResultList findConceptsForQuery(
			@ApiParam(value = "query as lucene search string", required = true) @PathParam("string") String searchString,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
			
		Logger.getLogger(ContentServiceRest.class).info(
				"RESTful call (Content): /concept/query/" + searchString);

		try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to find the concepts by query.").build());
			
  		
			ContentService contentService = new ContentServiceJpa();
			SearchResultList sr =
					contentService.findConceptsForQuery(searchString,
							new PfsParameterJpa());
			contentService.close();
			return sr;

		} catch (Exception e) {
			handleException(e, "trying to find the concepts by query");
			return null;
		}
	}

	/**
	 * Returns the descendants of a concept as mapped by relationships and
	 * inverse relationships
	 * 
	 * @param terminologyId
	 *            the terminology id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @param authToken 
	 * @return the search result list
	 */
	@GET
	@Path("/concept/id/{terminology}/{version}/{terminologyId}/descendants")
	@ApiOperation(value = "Find a concept's descendants", notes = "Returns a list of a concept's descendants given a concept id, terminology, and terminology version.", response = Concept.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findDescendantConcepts(
			@ApiParam(value = "Concept terminology id", required = true) @PathParam("terminologyId") String terminologyId,
			@ApiParam(value = "Concept terminology", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "Concept terminology version", required = true) @PathParam("version") String terminologyVersion,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(ContentServiceRest.class).info(
				"RESTful call (Content): /concept/" + terminology + "/"
						+ terminologyVersion + "/id/" + terminologyId
						+ "/descendants");

		try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to find the descendant concepts.").build());
			
  		
			ContentService contentService = new ContentServiceJpa();

			// want all descendants, do not use PFS parameter
			SearchResultList results = contentService.findDescendantConcepts(
					terminologyId, terminology, terminologyVersion, null);

			contentService.close();
			return results;

		} catch (Exception e) {
			handleException(e, "trying to find descendant concepts");
			return null;
		}
	}

	/**
	 * Returns the immediate children of a concept given terminology information
	 * 
	 * @param id
	 *            the terminology id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @param authToken 
	 * @return the search result list
	 */
	@GET
	@Path("/concept/id/{terminology}/{version}/{id}/children")
	@ApiOperation(value = "Find a concept's immediate children", notes = "Returns a concept's children in either xml json given a concept id, terminology, and terminology version.", response = Concept.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findChildConcepts(
			@ApiParam(value = "ID of concept to fetch descendants for", required = true) @PathParam("id") Long id,
			@ApiParam(value = "Concept terminology", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "Concept terminology version", required = true) @PathParam("version") String terminologyVersion,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(ContentServiceRest.class).info(
				"RESTful call (Content): /concept/" + terminology + "/"
						+ terminologyVersion + "/id/" + id.toString()
						+ "/descendants");

		try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to find the child concepts.").build());
			
  		
			ContentService contentService = new ContentServiceJpa();
			MetadataService metadataService = new MetadataServiceJpa();

			String isaId = "";
			Map<String, String> relTypesMap = metadataService
					.getHierarchicalRelationshipTypes(terminology,
							terminologyVersion);
			for (Map.Entry<String, String> entry : relTypesMap.entrySet()) {
				if (entry.getValue().toLowerCase().startsWith("is"))
					isaId = entry.getKey();
			}

			SearchResultList results = new SearchResultListJpa();

			// get the concept and add it as first element of concept list
			Concept concept = contentService.getConcept(id.toString(),
					terminology, terminologyVersion);

			// if no concept, return empty list
			if (concept == null) {
				return results;
			}

			// cycle over relationships
			for (Relationship rel : concept.getInverseRelationships()) {

				if (rel.isActive() && rel.getTypeId().equals(new Long(isaId))
						&& rel.getSourceConcept().isActive()) {

					Concept c = rel.getSourceConcept();

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

			metadataService.close();
			contentService.close();
			return results;

		} catch (Exception e) {
			handleException(e, "trying to find the child concepts");
			return null;
		}
	}
	
	@POST
	@Path("/terminology/id/{terminology}/{version}/delta")
	@ApiOperation(value = "Returns a list of the most recently edited concepts", notes = "For a given terminology and version, returns the set of concepts last modified in the database", response = Concept.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findDeltaConceptsForTerminology(
			@ApiParam(value = "Terminology", required = true) @PathParam("terminology") String terminology,
			@ApiParam(value = "Terminology version", required = true) @PathParam("version") String terminologyVersion,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken,
			@ApiParam(value = "Paging/filtering/sorting parameter object", required = true) PfsParameterJpa pfsParameter) {
		
		Logger.getLogger(ContentServiceRest.class).info(
				"RESTful call (Content): /terminology/id/" + terminology + "/"
						+ terminologyVersion
						+ "/delta");

		try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to find the child concepts.").build());
			
			ContentService contentService = new ContentServiceJpa();
			
			ConceptList conceptList = contentService.getConceptsModifiedSinceDate(terminology, null, pfsParameter);
			SearchResultList results = new SearchResultListJpa();

			for (Concept c : conceptList.getConcepts()) {
				
				// first pass check to see if this is a new concept or a modified concept
				// this will erroneously report NEW concept if all of the descriptions, relationships, and language ref set members were modified
				boolean modifiedConcept = false;
				for (Description d : c.getDescriptions()) {
					
					if (!d.getEffectiveTime().equals(c.getEffectiveTime())) modifiedConcept = true;
					for (LanguageRefSetMember l : d.getLanguageRefSetMembers()) {
						if (!l.getEffectiveTime().equals(c.getEffectiveTime())) modifiedConcept = true;
					}
				}
				
				for (Relationship r : c.getRelationships()) {
					if (!r.getEffectiveTime().equals(c.getEffectiveTime())) modifiedConcept = true;
				}
				
				
				SearchResult result = new SearchResultJpa();
				result.setId(c.getId());
				result.setTerminologyVersion(modifiedConcept == true ? "Modified" : "New");
				result.setTerminologyId(c.getTerminologyId());
				result.setValue(c.getDefaultPreferredName());
				results.addSearchResult(result);

			}
			
			results.setTotalCount(conceptList.getTotalCount());
			
			return results;
		} catch (Exception e) {
			handleException(e, "trying to retrieve concepts changed in last delta");
			return null;
		}
	}
			
			


}
