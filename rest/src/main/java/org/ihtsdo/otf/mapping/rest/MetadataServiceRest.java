package org.ihtsdo.otf.mapping.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.ihtsdo.otf.mapping.helpers.IdNameMap;
import org.ihtsdo.otf.mapping.helpers.IdNameMapJpa;
import org.ihtsdo.otf.mapping.helpers.IdNameMapList;
import org.ihtsdo.otf.mapping.helpers.IdNameMapListJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.services.MetadataService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * The Metadata Services REST package
 */
@Path("/metadata")
@Api(value = "/metadata", description = "Operations providing metadata.")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class MetadataServiceRest {

		
		@GET
		@Path("/all/")
		@ApiOperation(value = "Get all metadata", notes = "Returns all metadata in either JSON or XML format", response = IdNameMapList.class)
		@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public IdNameMapList getAllMetadata() {
		//@ApiParam(value = "terminology string", required = true) @PathParam("string") String terminology, 
		//@ApiParam(value = "terminology version string", required = true) @PathParam("string") String version) {
		try {
			MetadataService metadataService = new MetadataServiceJpa();
			IdNameMapList idNameMapList = new IdNameMapListJpa();
		  // TODO: make these as parameters
		  idNameMapList.setIdNameMapList(metadataService.getAllMetadata("SNOMEDCT", "20130131"));
		  metadataService.close();
		  return idNameMapList;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}



	@GET
	@Path("/refset/refsets/")
	@ApiOperation(value = "Get all complex map refsets", notes = "Returns all ComplexMapRefSets in either JSON or XML format", response = IdNameMap.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public IdNameMap getComplexMapRefSets() {
		try {
			MetadataService metadataService = new MetadataServiceJpa();
			IdNameMap idNameMap = new IdNameMapJpa();
		  idNameMap = metadataService.getComplexMapRefSets("SNOMEDCT", "20130131");

	    metadataService.close();
	    return idNameMap;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	@GET
	@Path("/terminologies/")
	@ApiOperation(value = "Get terminologies", notes = "Returns list of terminologies in either JSON or XML format")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
  public SearchResultList getTerminologies() {
	try {
		MetadataService metadataService = new MetadataServiceJpa();
	  List<String> terminologies = metadataService.getTerminologies();
	  SearchResultList searchResultList = new SearchResultListJpa();
	  for (String term : terminologies) {
	  	SearchResult searchResult = new SearchResultJpa();
	  	searchResult.setDescription(term);
	  	searchResultList.addSearchResult(searchResult);
	  }
	  metadataService.close();
	  return searchResultList;
	} catch (Exception e) {
		throw new WebApplicationException(e);
	}
}

}
