package org.ihtsdo.otf.mapping.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

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
import com.wordnik.swagger.annotations.ApiParam;

// TODO: Auto-generated Javadoc
/**
 * The Metadata Services REST package.
 *
 * @author ${author}
 */
@Path("/metadata")
@Api(value = "/metadata", description = "Operations providing metadata.")
@Produces({
		MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class MetadataServiceRest {

	/**
	 * Returns the all metadata.
	 *
	 * @param terminology the terminology
	 * @param version the version
	 * @return the all metadata
	 */
	@GET
	@Path("/all/{terminology}/{version}")
	@ApiOperation(value = "Get all metadata", notes = "Returns all metadata in either JSON or XML format", response = IdNameMapList.class)
	@Produces({
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public IdNameMapList getAllMetadata(
		@ApiParam(value = "terminology string", required = true)
		 @PathParam("terminology") String terminology,
		@ApiParam(value = "terminology version string", required = true)
		 @PathParam("version") String version) {
		try {
			MetadataService metadataService = new MetadataServiceJpa();
			IdNameMapList idNameMapList = new IdNameMapListJpa();
			idNameMapList.setIdNameMapList(metadataService.getAllMetadata(terminology,
					version));
			metadataService.close();
			return idNameMapList;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Returns the versions.
	 *
	 * @param terminology the terminology
	 * @return the versions
	 * @GET
	 * @Path("/refset/refsets/")
	 * @ApiOperation(value = "Get all complex map refsets", notes = "Returns all ComplexMapRefSets in either JSON or XML format", response = IdNameMap.class)
	 * @Produces({
	 * MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	 * })
	 * public IdNameMap getComplexMapRefSets() {
	 * try {
	 * MetadataService metadataService = new MetadataServiceJpa();
	 * IdNameMap idNameMap = new IdNameMapJpa();
	 * idNameMap = metadataService.getComplexMapRefSets("SNOMEDCT", "20130131");
	 * 
	 * metadataService.close();
	 * return idNameMap;
	 * } catch (Exception e) {
	 * throw new WebApplicationException(e);
	 * }
	 * }
	 */

	@GET
	@Path("/terminology/{String}")
	@ApiOperation(value = "Find versions of the given terminology", notes = "Returns versions of the given terminology in either JSON or XML format", response = SearchResultList.class)
	@Produces({
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public SearchResultList getVersions(
		@ApiParam(value = "Terminology name for which versions will be fetched", required = true) @PathParam("String") String terminology) {

		try {
			MetadataService metadataService = new MetadataServiceJpa();
			List<String> versions = metadataService.getVersions(terminology);
			SearchResultList searchResultList = new SearchResultListJpa();
			for (String version : versions) {
				SearchResult searchResult = new SearchResultJpa();
				searchResult.setValue(version);
				searchResultList.addSearchResult(searchResult);
			}
			metadataService.close();
			return searchResultList;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Returns the latest version.
	 *
	 * @param terminology the terminology
	 * @return the latest version
	 */
	@GET
	@Path("/terminology/latest/{String}")
	@ApiOperation(value = "Find latest version of the given terminology", notes = "Returns the latest version of the given terminology in either JSON or XML format", response = SearchResult.class)
	@Produces({
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public SearchResult getLatestVersion(
		@ApiParam(value = "Terminology name for which versions will be fetched", required = true) 
		  @PathParam("String") String terminology) {

		try {
			MetadataService metadataService = new MetadataServiceJpa();
			String version = metadataService.getLatestVersion(terminology);
			SearchResult searchResult = new SearchResultJpa();
			searchResult.setValue(version);
			metadataService.close();
			return searchResult;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/**
	 * Returns the terminologies.
	 *
	 * @return the terminologies
	 */
	@GET
	@Path("/terminologies/")
	@ApiOperation(value = "Get terminologies", notes = "Returns list of terminologies in either JSON or XML format", response = SearchResultList.class)
	@Produces({
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public SearchResultList getTerminologies() {
		try {
			MetadataService metadataService = new MetadataServiceJpa();
			List<String> terminologies = metadataService.getTerminologies();
			SearchResultList searchResultList = new SearchResultListJpa();
			for (String term : terminologies) {
				SearchResult searchResult = new SearchResultJpa();
				searchResult.setValue(term);
				searchResultList.addSearchResult(searchResult);
			}
			metadataService.close();
			return searchResultList;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}

	/** TODO: Needs to return Map, but IdNameMap is Long -> String not String -> String
	 * Is this really needed?  Can getTerminologies() and call getLatestVersion on each
	 * @GET
	@Path("/terminologies/latest/")
	@ApiOperation(value = "Get terminologies and their latest versions", notes = "Returns list of terminologies and their latest versions in either JSON or XML format", response = IdNameMap.class)
	@Produces({
			MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	public SearchResultList getTerminologyLatestVersions() {
		try {
			MetadataService metadataService = new MetadataServiceJpa();
			Map<String, String> terminologyVersionMap = metadataService.getTerminologyLatestVersions();
			IdNameMap idNameMap = new IdNameMapJpa();
			for (String term : terminologyVersionMap) {
				idNameMap.addIdNameMapEntry(id, name)
			}
			metadataService.close();
			return searchResultList;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}*/

}
