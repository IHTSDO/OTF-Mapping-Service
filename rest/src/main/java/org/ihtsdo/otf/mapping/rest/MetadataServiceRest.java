package org.ihtsdo.otf.mapping.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.dto.KeyValuePair;
import org.ihtsdo.otf.mapping.dto.KeyValuePairList;
import org.ihtsdo.otf.mapping.dto.KeyValuePairLists;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * Metadata Services REST package.
 */
@Path("/metadata")
@Api(value = "/metadata", description = "Operations providing metadata.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@SuppressWarnings("static-method")
public class MetadataServiceRest {

	/**  The security service. */
	private SecurityService securityService = new SecurityServiceJpa();
	
  /**
   * Returns all metadata for a terminology and version
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the all metadata
   */
  @GET
  @Path("/metadata/terminology/id/{terminology}/{version}")
  @ApiOperation(value = "Get metadata for terminology and version", notes = "Gets the key-value pairs representing metadata for a particular terminology and version", response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getMetadata(
    @ApiParam(value = "terminology string", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "terminology version string", required = true) @PathParam("version") String version,
		@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    
	  Logger.getLogger(MetadataServiceRest.class).info("RESTful call (Metadata): /metadata/" + terminology + "/" + version);
	  
	  try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve the metadata.").build());
			
  		
      // call jpa service and get complex map return type
      MetadataService metadataService = new MetadataServiceJpa();
      Map<String, Map<String, String>> mapOfMaps =
          metadataService.getAllMetadata(terminology, version);

      // convert complex map to KeyValuePair objects for easy transformation to
      // XML/JSON
      KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      for (Map.Entry<String, Map<String, String>> entry : mapOfMaps.entrySet()) {
        String metadataType = entry.getKey();
        Map<String, String> metadataPairs = entry.getValue();
        KeyValuePairList keyValuePairList = new KeyValuePairList();
        keyValuePairList.setName(metadataType);
        for (Map.Entry<String, String> pairEntry : metadataPairs.entrySet()) {
          KeyValuePair keyValuePair =
              new KeyValuePair(pairEntry.getKey().toString(),
                  pairEntry.getValue());
          keyValuePairList.addKeyValuePair(keyValuePair);
        }
        keyValuePairLists.addKeyValuePairList(keyValuePairList);
      }
      metadataService.close();
      return keyValuePairLists;
		} catch (LocalException e) { 
			e.printStackTrace();
			throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
		} catch (Exception e) { 
			e.printStackTrace();
			throw new WebApplicationException(Response.status(500).entity(
					"Unexpected error trying to retrieve the metadata. Please contact the administrator.").build());
		}
  }

  /**
   * Returns all metadata for the latest version.
   * 
   * @param terminology the terminology
   * @return the metadata
   */
  @GET
  @Path("/metadata/terminology/id/{terminology}/latest")
  @ApiOperation(value = "Get the latest version of terminology metadata", notes = "Returns all metadata for a specified terminology with the latest version in either JSON or XML format", response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getAllMetadata(
    @ApiParam(value = "terminology string", required = true) @PathParam("terminology") String terminology,
		@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
	  
	  Logger.getLogger(MetadataServiceRest.class).info("RESTful call (Metadata): /all/" + terminology); 
	    	
    try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve all metadata.").build());
			
  		
      MetadataService metadataService = new MetadataServiceJpa();
      String version = metadataService.getLatestVersion(terminology);
      KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      keyValuePairLists = getMetadata(terminology, version, authToken);

      metadataService.close();
      return keyValuePairLists;
		} catch (LocalException e) { 
			e.printStackTrace();
			throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
		} catch (Exception e) { 
			e.printStackTrace();
			throw new WebApplicationException(Response.status(500).entity(
					"Unexpected error trying to retrieve all metadata. Please contact the administrator.").build());
		}
  }

  /**
   * Returns all terminologies with only their latest version
   * 
   * @return the all terminologies latest versions
   */
  @GET
  @Path("/terminology/terminologies/latest")
  @ApiOperation(value = "Get all terminologies and their latest versions", notes = "Returns list of terminologies and their latest versions in either JSON or XML format", response = KeyValuePairList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairList getAllTerminologiesLatestVersions(
		@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
	  
	  Logger.getLogger(MetadataServiceRest.class).info("RESTful call (Metadata): /terminologies/latest/");
	  

		
    try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve the latest versions of all terminologies.").build());
			
  		
      MetadataService metadataService = new MetadataServiceJpa();
      Map<String, String> terminologyVersionMap =
          metadataService.getTerminologyLatestVersions();
      KeyValuePairList keyValuePairList = new KeyValuePairList();
      for (Map.Entry<String, String> termVersionPair : terminologyVersionMap
          .entrySet()) {
        keyValuePairList.addKeyValuePair(new KeyValuePair(termVersionPair
            .getKey(), termVersionPair.getValue()));
      }
      metadataService.close();
      return keyValuePairList;
		} catch (LocalException e) { 
			e.printStackTrace();
			throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
		} catch (Exception e) { 
			e.printStackTrace();
			throw new WebApplicationException(Response.status(500).entity(
					"Unexpected error trying to retrieve the latest versions of all terminologies. Please contact the administrator.").build());
		}
  }

  /**
   * Returns all terminologies and all versions
   * 
   * @return all terminologies and versions
   */
  @GET
  @Path("/terminology/terminologies")
  @ApiOperation(value = "Get all terminologies and all their versions", notes = "Returns list of terminologies and their versions in either JSON or XML format", response = KeyValuePairList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getAllTerminologiesVersions(
		@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
	  
	  Logger.getLogger(MetadataServiceRest.class).info("RESTful call (Metadata): /terminologies");
	  
  	
    try {
			// authorize call
			MapUserRole role = securityService.getApplicationRoleForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(Response.status(401).entity(
						"User does not have permissions to retrieve the versions of all terminologies.").build());
			
  		
      KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      MetadataService metadataService = new MetadataServiceJpa();
      List<String> terminologies = metadataService.getTerminologies();
      for (String terminology : terminologies) {
        List<String> versions = metadataService.getVersions(terminology);
        KeyValuePairList keyValuePairList = new KeyValuePairList();
        for (String version : versions) {
          keyValuePairList.addKeyValuePair(new KeyValuePair(terminology,
              version));
        }
        keyValuePairList.setName(terminology);
        keyValuePairLists.addKeyValuePairList(keyValuePairList);
      }
      metadataService.close();
      return keyValuePairLists;
		} catch (LocalException e) { 
			e.printStackTrace();
			throw new WebApplicationException(Response.status(500).entity(e.getMessage()).build());
		} catch (Exception e) { 
			e.printStackTrace();
			throw new WebApplicationException(Response.status(500).entity(
					"Unexpected error trying to retrieve the versions of all terminologies. Please contact the administrator.").build());
		}
  }
  

}
