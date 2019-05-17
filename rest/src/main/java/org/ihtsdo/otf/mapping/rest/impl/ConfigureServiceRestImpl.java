/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest.impl;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.jpa.services.rest.ConfigureServiceRest;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;

/**
 * REST implementation for {@link HistoryServiceRest}.
 */
@Path("/configure")
@Api(value = "/configure")
@SwaggerDefinition(info = @Info(description = "Operations to configure application", title = "Configure API", version = "1.0.0"))
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class ConfigureServiceRestImpl extends RootServiceRestImpl
    implements ConfigureServiceRest {

  /**
   * Instantiates an empty {@link ConfigureServiceRestImpl}.
   *
   * @throws Exception
   *           the exception
   */
  public ConfigureServiceRestImpl() throws Exception {
    // n/a
  }

  /**
   * Checks if application is configured.
   *
   * @return the release history
   * @throws Exception
   *           the exception
   */
  /* see superclass */
  @GET
  @Override
  @Path("/configured")
  @ApiOperation(value = "Checks if application is configured", notes = "Returns true if application is configured, false if not", response = Boolean.class)
  public boolean isConfigured() throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Configure): /configure/configured");

    try {
      String configFileName = ConfigUtility.getLocalConfigFile();
      return ConfigUtility.getConfigProperties() != null
          || (Files.exists(Paths.get(configFileName)));

    } catch (Exception e) {
      handleException(e, "checking if application is configured");
      return false;
    }
  }

  /* see superclass */
  @GET
  @Override
  @Path("/properties")
  @Produces({ MediaType.APPLICATION_JSON })
  @ApiOperation(value = "Get configuration properties", notes = "Gets user interface-relevant configuration properties", response = String.class, responseContainer = "Map")
  public Map<String, String> getConfigProperties() {
    Logger.getLogger(getClass())
        .info("RESTful call (Configure): /configure/properties");
    try {
      Map<String, String> map = new HashMap<>();
      for (final Map.Entry<Object, Object> o : ConfigUtility
          .getUiConfigProperties().entrySet()) {
        map.put(o.getKey().toString(), o.getValue().toString());
      }
      return map;
    } catch (Exception e) {
      handleException(e, "getting ui config properties");
    } finally {
      // n/a
    }
    return null;
  }
}
