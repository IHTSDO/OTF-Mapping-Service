package org.ihtsdo.otf.mapping.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptList;
import org.ihtsdo.otf.mapping.services.SearchResultList;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path("/content")
@Api(value = "/content", description = "Operations to retrieve RF2 content.")
@Produces({"application/json", "application/xml"})
public class ContentServiceRest {
	
	/** The content service jpa. */
	private ContentServiceJpa contentServiceJpa;
	
	/**
	 * Instantiates an empty {@link ContentServiceRest}.
	 */
	public ContentServiceRest() {
		contentServiceJpa = new ContentServiceJpa();
	}
	
	/**
	 * Returns a limited number of concepts
	 *
	 * @param id the id
	 * @return the concept for id
	 */
	@GET
	@Path("/concept/concepts")
	@ApiOperation(value = "Find limited number of concepts", notes = "Returns a list of concepts", response = Concept.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
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
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Concept getConceptForId(@ApiParam(value = "ID of concept to fetch", required = true) @PathParam("id") Long id) {
		return contentServiceJpa.getConceptForId(id);
	}
	
	// temporary path for json checking
	@GET
	@Path("/concept/id/json/{id:[0-9][0-9]*}")
	@ApiOperation(value = "Find concept by id", notes = "Returns a concept in either xml json given a concept id.", response = Concept.class)
	@Produces({ MediaType.APPLICATION_JSON})
	public Concept getConceptForIdJson(@ApiParam(value = "ID of concept to fetch", required = true) @PathParam("id") Long id) {
		return contentServiceJpa.getConceptForId(id);
	}

	/**
	 * Returns all map projects for a lucene query
	 * @param query the string query
	 * @return the map projects
	 * @throws Exception 
	 */
	@GET
	@Path("/concept/query/{String}")
	@ApiOperation(value = "Find concepts by query", notes = "Returns map concepts for a query in either JSON or XML format", response = ConceptList.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public SearchResultList findConcepts(
			@ApiParam(value = "lucene search string", required = true) @PathParam("String") String query) throws Exception {
		return contentServiceJpa.findConcepts(query);
	}
}
