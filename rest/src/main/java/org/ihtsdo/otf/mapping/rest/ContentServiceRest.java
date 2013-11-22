package org.ihtsdo.otf.mapping.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.model.Concept;

@Path("/content")
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
	 * Returns the concept for id.
	 *
	 * @param id the id
	 * @return the concept for id
	 */
	@GET
	@Path("/concept/json/{id:[0-9][0-9]*}")
	@Produces({MediaType.APPLICATION_JSON})
	public Concept getConceptForIdJson(@PathParam("id") Long id) {
		return contentServiceJpa.getConceptForId(id);

	}

	/**
	 * Returns the concept for id.
	 *
	 * @param id the id
	 * @return the concept for id
	 */
	@GET
	@Path("/concept/xml/{id:[0-9][0-9]*}")
	@Produces({MediaType.APPLICATION_XML})
	public Concept getConceptForIdXml(@PathParam("id") Long id) {
		return contentServiceJpa.getConceptForId(id);

	}

}
