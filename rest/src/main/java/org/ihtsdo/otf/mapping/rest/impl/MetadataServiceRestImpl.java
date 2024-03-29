package org.ihtsdo.otf.mapping.rest.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.KeyValuePair;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairList;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairLists;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.rest.MetadataServiceRest;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * REST implementation for metadata service.
 */
@Path("/metadata")
@Api(value = "/metadata", description = "Operations providing terminology metadata.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class MetadataServiceRestImpl extends RootServiceRestImpl
    implements MetadataServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link MetadataServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public MetadataServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MetadataServiceRest#getMetadata(java.lang.
   * String, java.lang.String, java.lang.String)
   */
  @Override
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

    Logger.getLogger(MetadataServiceRestImpl.class).info(
        "RESTful call (Metadata): /metadata/" + terminology + "/" + version);

    String user = "";
    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get metadata",
          securityService);

      // call jpa service and get complex map return type
      final Map<String, Map<String, String>> mapOfMaps =
          metadataService.getAllMetadata(terminology, version);

      // convert complex map to KeyValuePair objects for easy transformation to
      // XML/JSON
      final KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      for (final Map.Entry<String, Map<String, String>> entry : mapOfMaps
          .entrySet()) {
        final String metadataType = entry.getKey();
        final Map<String, String> metadataPairs = entry.getValue();
        final KeyValuePairList keyValuePairList = new KeyValuePairList();
        keyValuePairList.setName(metadataType);
        for (final Map.Entry<String, String> pairEntry : metadataPairs
            .entrySet()) {
          final KeyValuePair keyValuePair = new KeyValuePair(
              pairEntry.getKey().toString(), pairEntry.getValue());
          keyValuePairList.addKeyValuePair(keyValuePair);
        }
        keyValuePairLists.addKeyValuePairList(keyValuePairList);
      }
      return keyValuePairLists;
    } catch (Exception e) {
      handleException(e, "trying to get the metadata", user, "", "");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rest.impl.MetadataServiceRest#getAllMetadata(java.
   * lang.String, java.lang.String)
   */
  @Override
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

    Logger.getLogger(MetadataServiceRestImpl.class)
        .info("RESTful call (Metadata): /all/" + terminology);

    String user = "";
    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get all metadata",
          securityService);

      final String version = metadataService.getLatestVersion(terminology);
      return getMetadata(terminology, version, authToken);

    } catch (Exception e) {
      handleException(e, "trying to get all metadata", user, "", "");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MetadataServiceRest#
   * getAllTerminologiesLatestVersions(java.lang.String)
   */
  @Override
  @GET
  @Path("/terminology/terminologies/latest")
  @ApiOperation(value = "Get all terminologies and their latest versions.", notes = "Gets the list of terminologies and their latest versions.", response = KeyValuePairList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairList getAllTerminologiesLatestVersions(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MetadataServiceRestImpl.class)
        .info("RESTful call (Metadata): /terminologies/latest/");

    String user = "";
    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get all terminologies and versions", securityService);

      final Map<String, String> terminologyVersionMap =
          metadataService.getTerminologyLatestVersions();
      final KeyValuePairList keyValuePairList = new KeyValuePairList();
      for (final Map.Entry<String, String> termVersionPair : terminologyVersionMap
          .entrySet()) {
        keyValuePairList.addKeyValuePair(new KeyValuePair(
            termVersionPair.getKey(), termVersionPair.getValue()));
      }
      return keyValuePairList;
    } catch (Exception e) {
      handleException(e,
          "trying to get the latest versions of all terminologies", user, "",
          "");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MetadataServiceRest#
   * getAllTerminologiesVersions(java.lang.String)
   */
  @Override
  @GET
  @Path("/terminology/terminologies")
  @ApiOperation(value = "Get all terminologies and all their versions", notes = "Gets the list of all terminologies and all of their versions", response = KeyValuePairList.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public KeyValuePairLists getAllTerminologiesVersions(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MetadataServiceRestImpl.class)
        .info("RESTful call (Metadata): /terminologies");

    String user = "";
    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get all terminology versions", securityService);

      final KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      final List<String> terminologies = metadataService.getTerminologies();
      for (final String terminology : terminologies) {
        final List<String> versions = metadataService.getVersions(terminology);
        final KeyValuePairList keyValuePairList = new KeyValuePairList();
        for (final String version : versions) {
          keyValuePairList
              .addKeyValuePair(new KeyValuePair(terminology, version));
        }
        keyValuePairList.setName(terminology);
        keyValuePairLists.addKeyValuePairList(keyValuePairList);
      }
      return keyValuePairLists;
    } catch (Exception e) {
      handleException(e, "trying to get the versions of all terminologies",
          user, "", "");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MetadataServiceRest#
   * getAllTerminologiesVersions(java.lang.String)
   */
  @Override
  @GET
  @Path("/terminology/gmdn")
  @ApiOperation(value = "Get all downloaded gmdn versions", notes = "Gets the list of all version of gmdn that are present on the server", response = KeyValuePairList.class)
  @Produces({
      MediaType.TEXT_PLAIN
  })
  public String getAllGmdnVersions(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MetadataServiceRestImpl.class)
        .info("RESTful call (Metadata): /terminology/gmdn");

    String user = "";
    try {
      // authorize
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get all downloaded gmdn versions", securityService);

      String gmdnVersions = "";

      final String gmdnDir =
          ConfigUtility.getConfigProperties().getProperty("gmdnsftp.dir");

      File folder = new File(gmdnDir);
      if(!folder.exists()){
        throw new FileNotFoundException(folder + " not found");
      }
      
      File[] listOfFiles = folder.listFiles();

      // We only care about the directories that follow a yy_M naming convention
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isDirectory()
            && listOfFiles[i].getName().matches("\\d{2}_\\d{1,2}")) {
          gmdnVersions += listOfFiles[i].getName() + ";";
        }
      }

      //get rid of final ';'
      if(gmdnVersions.length() > 1){
        gmdnVersions = gmdnVersions.substring(0, gmdnVersions.length()-1);
      }
      
      return gmdnVersions;
    } catch (FileNotFoundException e) {
      handleException(e, "get downloaded versions of gmdn: " + e.getMessage(),
          user, "", "");
      return null;
   } catch (Exception e) {
      handleException(e, "get downloaded versions of gmdn",
          user, "", "");
      return null;
    } finally {
      securityService.close();
    }
  }
  
  /**
	 * Returns the latest atc version from the api.
	 *
	 * @param authToken the auth token
	 * @return the atc version
	 * @throws Exception the exception
	 */
  @Override
  @GET
  @Path("/terminology/atc")
  @ApiOperation(value = "Get all downloaded gmdn versions", notes = "Gets the latest atc version from the api", response = KeyValuePairList.class)
  @Produces({
      MediaType.TEXT_PLAIN
  })
  public String getAllAtcVersions(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MetadataServiceRestImpl.class)
        .info("RESTful call (Metadata): /terminology/atc");

    String user = "";
    try {
      // authorize
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get all downloaded atc versions", securityService);

      String atcVersions = "";

      final String atcDir =
          ConfigUtility.getConfigProperties().getProperty("atcAPI.dir");

      File folder = new File(atcDir);
      if(!folder.exists()){
        throw new FileNotFoundException(folder + " not found");
      }
      
      File[] listOfFiles = folder.listFiles();

      // We only care about the directories that follow a yy_MM_DD naming convention
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isDirectory()
            && listOfFiles[i].getName().matches("\\d{4}_\\d{2}_\\d{2}")) {
          atcVersions += listOfFiles[i].getName() + ";";
        }
      }

      //get rid of final ';'
      if(atcVersions.length() > 1){
    	  atcVersions = atcVersions.substring(0, atcVersions.length()-1);
      }
      
      return atcVersions;
    } catch (FileNotFoundException e) {
      handleException(e, "get downloaded versions of gmdn: " + e.getMessage(),
          user, "", "");
      return null;
   } catch (Exception e) {
      handleException(e, "get downloaded versions of gmdn",
          user, "", "");
      return null;
    } finally {
      securityService.close();
    }
  }
  
  /**
   * Returns the latest icpc2_no version from the api.
   *
   * @param authToken the auth token
   * @return the icpc2 version
   * @throws Exception the exception
   */
@Override
@GET
@Path("/terminology/icpc2no")
@ApiOperation(value = "Get all downloaded ICPC-2 versions", notes = "Gets the latest ICPC-2 version from the api", response = KeyValuePairList.class)
@Produces({
    MediaType.TEXT_PLAIN
})
public String getAllIcpc2NOVersions(
  @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
  throws Exception {

  Logger.getLogger(MetadataServiceRestImpl.class)
      .info("RESTful call (Metadata): /terminology/icpc2no");

  String user = "";
  try {
    // authorize
    user = authorizeApp(authToken, MapUserRole.VIEWER,
        "get all downloaded atc versions", securityService);

    String icpc2Versions = "";

    final String icpc2Dir =
        ConfigUtility.getConfigProperties().getProperty("icpc2noAPI.dir");

    File folder = new File(icpc2Dir);
    if(!folder.exists()){
      throw new FileNotFoundException(folder + " not found");
    }
    
    File[] listOfFiles = folder.listFiles();

    // We only care about the directories that follow a yy_MM_DD naming convention
    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isDirectory()
          && listOfFiles[i].getName().matches("\\d{4}_\\d{2}_\\d{2}")) {
        icpc2Versions += listOfFiles[i].getName() + ";";
      }
    }

    //get rid of final ';'
    if(icpc2Versions.length() > 1){
      icpc2Versions = icpc2Versions.substring(0, icpc2Versions.length()-1);
    }
    
    return icpc2Versions;
  } catch (FileNotFoundException e) {
    handleException(e, "get downloaded versions of ICPC-2_NO: " + e.getMessage(),
        user, "", "");
    return null;
 } catch (Exception e) {
    handleException(e, "get downloaded versions of ICPC-2_NO",
        user, "", "");
    return null;
  } finally {
    securityService.close();
  }
}

/**
 * Returns the latest icpc2_no version from the api.
 *
 * @param authToken the auth token
 * @return the icpc2 version
 * @throws Exception the exception
 */
@Override
@GET
@Path("/terminology/icd10no")
@ApiOperation(value = "Get all downloaded ICD10NO versions", notes = "Gets the latest ICD10NO version from the api", response = KeyValuePairList.class)
@Produces({
  MediaType.TEXT_PLAIN
})
public String getAllIcd10NOVersions(
@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
throws Exception {

Logger.getLogger(MetadataServiceRestImpl.class)
    .info("RESTful call (Metadata): /terminology/icd10no");

String user = "";
try {
  // authorize
  user = authorizeApp(authToken, MapUserRole.VIEWER,
      "get all downloaded atc versions", securityService);

  String icd10noVersions = "";

  final String icd10noDir =
      ConfigUtility.getConfigProperties().getProperty("icd10noAPI.dir");

  File folder = new File(icd10noDir);
  if(!folder.exists()){
    throw new FileNotFoundException(folder + " not found");
  }
  
  File[] listOfFiles = folder.listFiles();

  // We only care about the directories that follow a yy_MM_DD naming convention
  for (int i = 0; i < listOfFiles.length; i++) {
    if (listOfFiles[i].isDirectory()
        && listOfFiles[i].getName().matches("\\d{4}_\\d{2}_\\d{2}")) {
      icd10noVersions += listOfFiles[i].getName() + ";";
    }
  }

  //get rid of final ';'
  if(icd10noVersions.length() > 1){
    icd10noVersions = icd10noVersions.substring(0, icd10noVersions.length()-1);
  }
  
  return icd10noVersions;
} catch (FileNotFoundException e) {
  handleException(e, "get downloaded versions of ICD10NO: " + e.getMessage(),
      user, "", "");
  return null;
} catch (Exception e) {
  handleException(e, "get downloaded versions of ICD10NO",
      user, "", "");
  return null;
} finally {
  securityService.close();
}
}


  
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rest.impl.MetadataServiceRest#
   * getAllTerminologiesVersions(java.lang.String)
   */
  @Override
  @GET
  @Path("/terminology/mimsAllergy")
  @ApiOperation(value = "Get all available mims allergy versions", notes = "Gets the list of all version of mims allergy that are present", response = KeyValuePairList.class)
  @Produces({
      MediaType.TEXT_PLAIN
  })
  public String getAllMimsAllergyVersions(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MetadataServiceRestImpl.class)
        .info("RESTful call (Metadata): /terminology/mimsAllergy");

    String user = "";
    try {
      // authorize
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get all downloaded mims allergy versions", securityService);

      String mimsAllergyVersions = "";

      final String mimsAllergyDir =
          ConfigUtility.getConfigProperties().getProperty("MIMS_Allergy.dir");
      
      final String mimsAllergyLoadDir =
              ConfigUtility.getConfigProperties().getProperty("MIMS_Allergy.loadDir");

      File excelDir = new File(mimsAllergyLoadDir);
      FileFilter onlyExcel = new WildcardFileFilter("*.xlsx");
      File[] loadingFiles = excelDir.listFiles(onlyExcel);
      String version = null;
      String filename = null;
      
      File folder = new File(mimsAllergyDir);
      if(!folder.exists()){
        throw new FileNotFoundException(folder + " not found");
      }
      
      for(File file : loadingFiles) {
		filename = file.getAbsolutePath();
  	    
  	    String termVersionDate = file.getName().split("[.-]")[1];
  	    
  	    SimpleDateFormat format1 = new SimpleDateFormat("ddMMMyyyy");
  	    SimpleDateFormat format2 = new SimpleDateFormat("yyyyMMdd");
  	    Date date = format1.parse(termVersionDate);
  	    version = format2.format(date);
  	    
  	    File versionFolder = new File(mimsAllergyDir + "/" + version);
  	    if(!versionFolder.exists())
  	    	versionFolder.mkdirs();
      }
      
      File[] listOfFiles = folder.listFiles();
      // We only care about the directories that follow a yyyyMMdd naming convention
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isDirectory()
            && listOfFiles[i].getName().matches("\\d{4}\\d{2}\\d{2}")) {
      	    mimsAllergyVersions += listOfFiles[i].getName() + ";";
        }
      }

      if(version != null && !mimsAllergyVersions.contains(version)) {
    	  mimsAllergyVersions += version + ";";
    	  File newTerminologyFile = new File(filename);
    	  File newTerminologyDirectory = new File(mimsAllergyDir + version);
    	  newTerminologyDirectory.mkdir();
    	  FileUtils.copyDirectory(newTerminologyFile, newTerminologyDirectory);
      }

      //get rid of final ';'
      if(mimsAllergyVersions.length() > 1){
    	  mimsAllergyVersions = mimsAllergyVersions.substring(0, mimsAllergyVersions.length()-1);
      }
      
      return mimsAllergyVersions;
    } catch (FileNotFoundException e) {
      handleException(e, "get versions of mims allergy: " + e.getMessage(),
          user, "", "");
      return null;
   } catch (Exception e) {
      handleException(e, "get versions of mims allergy",
          user, "", "");
      return null;
    } finally {
      securityService.close();
    }
  }

}
