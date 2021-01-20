/*
 * Copyright 2020 Wci Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Wci Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * Wci Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package org.ihtsdo.otf.mapping.rest.impl;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.apache.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.util.Json;

/**
 * @author Nuno Marques
 *
 */
@ApplicationPath("/")
public class MappingServerApplication extends Application {

  /** The API_VERSION - also used in "swagger.htmL" */
  public final static String API_VERSION = "1.0.0";

  /**
   * Run project specific handlers' initialize method for all projects
   *
   * @throws Exception the exception
   */
  public static void initializeHandlers() throws Exception {
    final MappingService service = new MappingServiceJpa();
    try {

      for (MapProject mapProject : service.getMapProjects().getMapProjects()) {
        ProjectSpecificAlgorithmHandler handler = (ProjectSpecificAlgorithmHandler) Class
            .forName(mapProject.getProjectSpecificAlgorithmHandlerClass()).getDeclaredConstructor()
            .newInstance();

        handler.setMapProject(mapProject);
        handler.initialize();

      }
    } catch (Exception e) {
      throw e;
    } finally {
      service.close();
    }

  }

  /**
   * Instantiates an empty {@link MappingServerApplication}.
   * 
   * @throws Exception the exception
   */
  public MappingServerApplication() throws Exception {

    Logger.getLogger(getClass()).info("MAPPING SERVER APPLICATION START");

    // Instantiate bean config for Swagger
    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setTitle("Mapping Server API");
    beanConfig.setDescription("RESTful calls for mapping server");
    beanConfig.setVersion(API_VERSION);

    final URL url = new URL(ConfigUtility.getConfigProperties().getProperty("base.url"));
    final String host = url.getHost() + ":" + url.getPort();

    if (new ConfigureServiceRestImpl().isConfigured()) {
      beanConfig.setHost(host);
      beanConfig.setBasePath(url.getPath());
      beanConfig.setSchemes(new String[] {
          url.getProtocol()
      });
      beanConfig.setResourcePackage("org.ihtsdo.otf.mapping.rest.impl");
      beanConfig.setScan(true);
      beanConfig.setPrettyPrint(true);
    }

    // this makes Swagger honor JAXB annotations
    Json.mapper().registerModule(new JaxbAnnotationModule());

    initializeHandlers();

  }

  /* see superclass */
  @Override
  public Set<Class<?>> getClasses() {
    final Set<Class<?>> classes = new HashSet<Class<?>>();
    classes.add(AdminServiceRestImpl.class);
    classes.add(ConfigureServiceRestImpl.class);
    classes.add(ContentServiceRestImpl.class);
    classes.add(MappingServiceRestImpl.class);
    classes.add(MetadataServiceRestImpl.class);
    classes.add(ReportServiceRestImpl.class);
    classes.add(SecurityServiceRestImpl.class);
    classes.add(WorkflowServiceRestImpl.class);

    classes.add(io.swagger.jaxrs.listing.ApiListingResource.class);
    classes.add(io.swagger.jaxrs.listing.SwaggerSerializers.class);

    return classes;
  }

  /* see superclass */
  @Override
  public Set<Object> getSingletons() {
    final Set<Object> instances = new HashSet<Object>();
    instances.add(new JacksonFeature());
    // instances.add(new JacksonJsonProvider());
    instances.add(new JsonProcessingFeature());
    instances.add(new MultiPartFeature());
    // Enable for LOTS of logging of HTTP requests
    // instances.add(new LoggingFilter());
    return instances;
  }

}
