package org.ihtsdo.otf.mapping.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.ihtsdo.otf.mapping.dto.KeyValuePair;
import org.ihtsdo.otf.mapping.dto.KeyValuePairList;
import org.ihtsdo.otf.mapping.dto.KeyValuePairLists;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.services.MetadataService;

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

  /**
   * Returns the all metadata.
   * 
   * @param terminology the terminology
   * @param version the version
   * @return the all metadata
   */
  @GET
  @Path("/all/{terminology}/{version}")
  @ApiOperation(value = "Get all metadata", notes = "Returns all metadata in either JSON or XML format", response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getAllMetadata(
    @ApiParam(value = "terminology string", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "terminology version string", required = true) @PathParam("version") String version) {
    try {
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
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Returns all metadata for the latest version.
   * 
   * @param terminology the terminology
   * @return the all metadata
   */
  @GET
  @Path("/all/{terminology}")
  @ApiOperation(value = "Get all metadata with the latest version", notes = "Returns all metadata with the latest version in either JSON or XML format", response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getAllMetadata(
    @ApiParam(value = "terminology string", required = true) @PathParam("terminology") String terminology) {
    try {
      MetadataService metadataService = new MetadataServiceJpa();
      String version = metadataService.getLatestVersion(terminology);
      KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      keyValuePairLists = getAllMetadata(terminology, version);

      metadataService.close();
      return keyValuePairLists;
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Returns the all terminologies latest versions.
   * 
   * @return the all terminologies latest versions
   */
  @GET
  @Path("/terminologies/latest/")
  @ApiOperation(value = "Get all terminologies and their latest versions", notes = "Returns list of terminologies and their latest versions in either JSON or XML format", response = KeyValuePairList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairList getAllTerminologiesLatestVersions() {
    try {
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
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  /**
   * Returns the all terminologies versions.
   * 
   * @return the all terminologies versions
   */
  @GET
  @Path("/terminologies/")
  @ApiOperation(value = "Get all terminologies and all their versions", notes = "Returns list of terminologies and their versions in either JSON or XML format", response = KeyValuePairList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getAllTerminologiesVersions() {
    try {
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
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

}
