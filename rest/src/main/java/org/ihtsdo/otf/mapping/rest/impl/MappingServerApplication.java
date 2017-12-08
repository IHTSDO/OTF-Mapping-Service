/**
 * 
 */
package org.ihtsdo.otf.mapping.rest.impl;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.apache.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.util.Json;

/**
 * @author Nuno Marques
 *
 */
@ApplicationPath("/")
public class MappingServerApplication extends Application { //ResourceConfig {

	/** The API_VERSION - also used in "swagger.htmL" */
	public final static String API_VERSION = "1.0.0";
	
	/**
	 * Instantiates an empty {@link MappingServerApplication}.
	 * 
	 * @throws Exception the exception
	 */
	public MappingServerApplication() throws Exception {
			
		Logger.getLogger(getClass()).info("MAPPING SERVER APPLICATION START");
			    
	    // Instantiate bean config for Swagger
	    BeanConfig beanConfig = new BeanConfig();
	    beanConfig.setTitle("Term Server API");
	    beanConfig.setDescription("RESTful calls for mapping server");
	    beanConfig.setVersion(API_VERSION);
	    final URL url = new URL(ConfigUtility.getConfigProperties().getProperty("base.url"));
	    beanConfig.setBasePath(url.getHost() + ":" + url.getPort());
	    beanConfig.setResourcePackage("org.ihtsdo.otf.mapping.rest.impl");
	    beanConfig.setScan(true);


	    // this makes Swagger honor JAXB annotations
	    Json.mapper().registerModule(new JaxbAnnotationModule());

	}
	
	/* see superclass */
	@Override
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> classes = new HashSet<Class<?>>();
	    classes.add(AdminServiceRestImpl.class);
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
	    instances.add(new JacksonJsonProvider());
	    instances.add(new MultiPartFeature());
	    // Enable for LOTS of logging of HTTP requests
	    // instances.add(new LoggingFilter());
	    return instances;
	  }

}
