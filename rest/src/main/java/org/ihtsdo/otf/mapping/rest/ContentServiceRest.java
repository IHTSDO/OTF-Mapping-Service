package org.ihtsdo.otf.mapping.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptList;
import org.ihtsdo.otf.mapping.services.SearchResultList;

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

	/** The content service jpa. */
	private ContentServiceJpa contentServiceJpa;

	/**
	 * Instantiates an empty {@link ContentServiceRest}.
	 */
	public ContentServiceRest() {
		contentServiceJpa = new ContentServiceJpa();

		// TODO: wire this to metadata service (getTerminologyLatestVesrions)
		terminologyLatestVersions = new HashMap<String, String>();
		terminologyLatestVersions.put("SNOMEDCT", "20130131");
	}

	/**
	 * Returns a limited number of concepts.
	 *
	 * @return the concept for id
	 */
	@GET
	@Path("/concept/concepts")
	@ApiOperation(value = "Find limited number of concepts", notes = "Returns a list of concepts for testing purposes only", response = Concept.class)
	@Produces({
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public ConceptList getConceptLimited() {
		ConceptList concepts = new ConceptList();
		concepts.setConcepts(contentServiceJpa.getConceptsLimited(50));
		concepts.sortConcepts();
		return concepts;
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
		Concept result = null;
		try {
			result = contentServiceJpa.getConcept(id);
		} catch (Exception e) {
			// do nothing, try alternative search
		}
		if (result == null) {
			return contentServiceJpa.getConcept(id, "SNOMEDCT",
					terminologyLatestVersions.get("SNOMEDCT"));
		} else {
			return result;
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
		return contentServiceJpa.getConcept(id, terminology,
				terminologyVersion);
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
		return contentServiceJpa.getConcept(id, terminology,
				terminologyLatestVersions.get(terminology));
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
				return contentServiceJpa.findConcepts(searchString);
			} catch (Exception e) {
				throw new WebApplicationException(e);
			}
	}
}
