package org.ihtsdo.otf.mapping.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ihtsdo.otf.mapping.helpers.IdNameMap;
import org.ihtsdo.otf.mapping.helpers.IdNameMapList;
import org.ihtsdo.otf.mapping.helpers.IdNameMapListJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * The Metadata Services REST package
 */
@Path("/metadata")
@Api(value = "/metadata", description = "Operations providing metadata.")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class MetadataServiceRest {

		/** The metadata service jpa. */
		private MetadataServiceJpa metadataServiceJpa;

		/**
		 * Instantiates an empty {@link MappingServiceRest}.
		 */
		public MetadataServiceRest() {
			metadataServiceJpa = new MetadataServiceJpa();
		}
		
		@GET
		@Path("/metadata/metadata/")
		@ApiOperation(value = "Get all metadata", notes = "Returns all metadata in either JSON or XML format", response = IdNameMapList.class)
		@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public IdNameMapList getAllMetadata() {
		//@ApiParam(value = "terminology string", required = true) @PathParam("string") String terminology, 
		//@ApiParam(value = "terminology version string", required = true) @PathParam("string") String version) {
		IdNameMapList idNameMapList = new IdNameMapListJpa();
		// TODO: make these as parameters
		idNameMapList.setIdNameMapList(metadataServiceJpa.getAllMetadata("SNOMEDCT", "20130131"));
		return idNameMapList;
	}



	@GET
	@Path("/refset/refsets/")
	@ApiOperation(value = "Get all complex map refsets", notes = "Returns all ComplexMapRefSets in either JSON or XML format", response = IdNameMap.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public IdNameMap getComplexMapRefSets() {
		return metadataServiceJpa.getComplexMapRefSets("SNOMEDCT", "20130131");
	}

	@GET
	@Path("/refset/refsets2/")
	@ApiOperation(value = "Get all attribute value refsets", notes = "Returns all AttributeValueRefSets in either JSON or XML format", response = IdNameMap.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public IdNameMap getAttributeValueRefSets() {
		return metadataServiceJpa.getAttributeValueRefSets("SNOMEDCT", "20130131");
	}
	
	@GET
	@Path("/refset/refsets3/")
	@ApiOperation(value = "Get all case significances", notes = "Returns all CaseSignificances in either JSON or XML format", response = IdNameMap.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public IdNameMap getCaseSignificances() {
		return metadataServiceJpa.getCaseSignificances("SNOMEDCT", "20130131");
	}
	
	
}
