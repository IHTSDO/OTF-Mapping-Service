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
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * REST implementation for metadata service.
 */
@Path("/metadata")
@Api(value = "/metadata", description = "Operations providing terminology metadata.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class MetadataServiceRest extends RootServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link MetadataServiceRest}.
   *
   * @throws Exception the exception
   */
  public MetadataServiceRest() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /**
   * Returns all metadata for a terminology and version.
   *
   * @param terminology the terminology
   * @param version the version
   * @param authToken the auth token
   * @return the all metadata
   * @throws Exception the exception
   */
  @GET
  @Path("/metadata/terminology/id/{terminology}/{version}")
  @ApiOperation(value = "Get metadata for terminology and version.", notes = "Gets the key-value pairs representing all metadata for a particular terminology and version.", response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getMetadata(
    @ApiParam(value = "Terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Terminology version, e.g. 20140731", required = true) @PathParam("version") String version,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MetadataServiceRest.class).info(
        "RESTful call (Metadata): /metadata/" + terminology + "/" + version);

    String user = "";
    MetadataService metadataService = new MetadataServiceJpa();
    try {
      user = securityService.getUsernameForToken(authToken);

      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to retrieve the metadata.")
            .build());

      // call jpa service and get complex map return type
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
      return keyValuePairLists;
    } catch (Exception e) {
      handleException(e, "trying to retrieve the metadata", user, "", "");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /**
   * Returns all metadata for the latest version.
   *
   * @param terminology the terminology
   * @param authToken the auth token
   * @return the metadata
   * @throws Exception the exception
   */
  @GET
  @Path("/metadata/terminology/id/{terminology}/latest")
  @ApiOperation(value = "Get all metadata for the the latest version of a terminology.", notes = "Returns all metadata for the latest version of a specified terminology.", response = KeyValuePairLists.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getAllMetadata(
    @ApiParam(value = "Terminology name, e.g. SNOMEDCT", required = true) @PathParam("terminology") String terminology,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MetadataServiceRest.class).info(
        "RESTful call (Metadata): /all/" + terminology);

    String user = "";
    MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize call
      user = securityService.getUsernameForToken(authToken);
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to retrieve all metadata.")
            .build());

      String version = metadataService.getLatestVersion(terminology);
      KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      keyValuePairLists = getMetadata(terminology, version, authToken);

      return keyValuePairLists;
    } catch (Exception e) {
      handleException(e, "trying to retrieve all metadata", user, "", "");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /**
   * Returns all terminologies with only their latest version.
   *
   * @param authToken the auth token
   * @return the all terminologies latest versions
   * @throws Exception the exception
   */
  @GET
  @Path("/terminology/terminologies/latest")
  @ApiOperation(value = "Get all terminologies and their latest versions.", notes = "Gets the list of terminologies and their latest versions.", response = KeyValuePairList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairList getAllTerminologiesLatestVersions(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MetadataServiceRest.class).info(
        "RESTful call (Metadata): /terminologies/latest/");

    String user = "";
    MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize call
      user = securityService.getUsernameForToken(authToken);
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the latest versions of all terminologies.")
                .build());

      Map<String, String> terminologyVersionMap =
          metadataService.getTerminologyLatestVersions();
      KeyValuePairList keyValuePairList = new KeyValuePairList();
      for (Map.Entry<String, String> termVersionPair : terminologyVersionMap
          .entrySet()) {
        keyValuePairList.addKeyValuePair(new KeyValuePair(termVersionPair
            .getKey(), termVersionPair.getValue()));
      }
      return keyValuePairList;
    } catch (Exception e) {
      handleException(e,
          "trying to retrieve the latest versions of all terminologies", user,
          "", "");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /**
   * Returns all terminologies and all versions.
   *
   * @param authToken the auth token
   * @return all terminologies and versions
   * @throws Exception the exception
   */
  @GET
  @Path("/terminology/terminologies")
  @ApiOperation(value = "Get all terminologies and all their versions", notes = "Gets the list of all terminologies and all of their versions", response = KeyValuePairList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getAllTerminologiesVersions(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MetadataServiceRest.class).info(
        "RESTful call (Metadata): /terminologies");

    String user = "";
    MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize call
      user = securityService.getUsernameForToken(authToken);
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve the versions of all terminologies.")
                .build());

      KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
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
      return keyValuePairLists;
    } catch (Exception e) {
      handleException(e,
          "trying to retrieve the versions of all terminologies", user, "", "");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

}
