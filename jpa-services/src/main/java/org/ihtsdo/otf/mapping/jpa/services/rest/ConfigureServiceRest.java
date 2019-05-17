/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.services.rest;

import java.util.Map;

/**
 * Represents a service for configuring an environment.
 */
public interface ConfigureServiceRest {

  /**
   * Checks if is configured.
   *
   * @return true, if is configured
   * @throws Exception the exception
   */
  public boolean isConfigured() throws Exception;

  /**
   * Returns the config properties relevant for the UI. This is the means to
   * inject configuration info into the javascript.
   *
   * @return the config properties
   */
  public Map<String, String> getConfigProperties();

}
