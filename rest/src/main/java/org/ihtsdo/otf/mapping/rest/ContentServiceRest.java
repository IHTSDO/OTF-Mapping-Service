package org.ihtsdo.otf.mapping.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * REST implementation for content service
 */
@Path("/content")
@Api(value = "/content", description = "Operations to retrieve RF2 content.")
@Produces({
	MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class ContentServiceRest {

	/** The terminology versions. */
	private Map<String, String> terminologyLatestVersions = null;


	/**
	 * Instantiates an empty {@link ContentServiceRest}.
	 */
	public ContentServiceRest() {

		// TODO: wire this to metadata service (getTerminologyLatestVesrions)
		terminologyLatestVersions = new HashMap<String, String>();
		terminologyLatestVersions.put("SNOMEDCT", "20130131");
	}

	/**
	 * Returns the concept for id.
	 * 
	 * @param id the id
	 * @return the concept for id
	 */
	@GET
	@Path("/concept/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find concept by id", notes = "Returns a concept in either xml json given a concept id.", response = Concept.class)
	@Produces({
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public Concept getConceptForId(
		@ApiParam(value = "ID of concept to fetch", required = true) @PathParam("id") Long id) {
		
		try {
			ContentService contentService = new ContentServiceJpa();
			Concept c = contentService.getConcept(id);
			

			// if a terminology Id passed incorrectly, search appropriately
			if (c == null) {
				c = contentService.getConcept(id.toString(), "SNOMEDCT",
						terminologyLatestVersions.get("SNOMEDCT"));
			} 
			contentService.close();
			return c;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		} 
		
		
		
	}

	/**
	 * Returns the concept for id, terminology. Looks in the latest version of the
	 * terminology.
	 * 
	 * @param id the id
	 * @param terminology the concept terminology
	 * @param version the concept terminologyVersion
	 * @return the concept
	 */
	@GET
	@Path("/concept/{terminology}/{version}/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find concept by id, terminology", notes = "Returns a concept in either xml json given a concept id, terminology - assumes latest terminology version.", response = Concept.class)
	@Produces({
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public Concept getConceptForId(
		@ApiParam(value = "ID of concept to fetch", required = true) @PathParam("id") Long id,
		@ApiParam(value = "Concept terminology", required = true) @PathParam("terminology") String terminology,
		@ApiParam(value = "Concept terminology version", required = true) @PathParam("version") String terminologyVersion) {
		
		try {
			ContentService contentService = new ContentServiceJpa();
			Concept c = contentService.getConcept(id.toString(), terminology, terminologyVersion);
			contentService.close();
			return c;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
	}
	

	/**
	 * Returns the concept for id, terminology. Looks in the latest version of the
	 * terminology.
	 * 
	 * @param id the id
	 * @param terminology the concept terminology
	 * @return the concept
	 */
	@GET
	@Path("/concept/{terminology}/id/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find concept by id, terminology", notes = "Returns a concept in either xml json given a concept id, terminology - assumes latest terminology version.", response = Concept.class)
	@Produces({
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public Concept getConceptForId(
		@ApiParam(value = "ID of concept to fetch", required = true) @PathParam("id") Long id,
		@ApiParam(value = "Concept terminology", required = true) @PathParam("terminology") String terminology) {
		
		try {
			ContentService contentService = new ContentServiceJpa();
			Concept c = contentService.getConcept(id.toString(), terminology, terminologyLatestVersions.get(terminology));
			contentService.close();
			return c;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
	}
	
	/**
	 * Returns the concept for search string.
	 *
	 * @param searchString the lucene search string
	 * @return the concept for id
	 */
	@GET
	@Path("/concept/query/{string}")
	@ApiOperation(value = "Find concepts by search query", notes = "Returns concepts that are related to search query.", response = String.class)
	public SearchResultList findConcepts (
		@ApiParam(value = "lucene search string", required = true) @PathParam("string") String searchString) {
		
		try {
			ContentService contentService = new ContentServiceJpa();
			SearchResultList sr = contentService.findConcepts(searchString);
			contentService.close();
			return sr;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Returns the descendants of a concept as mapped by relationships and inverse relationships
	 * @param id the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @return the search result list
	 */
	@GET
	@Path("/concept/{terminology}/{version}/id/{id:[0-9][0-9]*}/descendants")
	@ApiOperation(value = "Find concept by id, terminology", notes = "Returns a concept in either xml json given a concept id, terminology - assumes latest terminology version.", response = Concept.class)
	@Produces({
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public Set<Concept> getConceptDescendants(
		@ApiParam(value = "ID of concept to fetch descendants for", required = true) @PathParam("id") Long id,
		@ApiParam(value = "Concept terminology", required = true) @PathParam("terminology") String terminology,
		@ApiParam(value = "Concept terminology version", required = true) @PathParam("version") String terminologyVersion) {
		
		try {
			ContentService contentService = new ContentServiceJpa();
			
			Set<Concept> concepts = contentService.getDescendants(id.toString(), terminology,
				terminologyVersion, new Long("116680003")); // TODO Change this to metadata reference
		
			contentService.close();
			return concepts;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
}
