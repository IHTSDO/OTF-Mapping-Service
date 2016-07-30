package org.ihtsdo.otf.mapping.test.rest;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.test.helpers.TestSupport;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * Simple REST test to verify we can read config properties from the server.
 */
public class ConfigPropertiesTest extends TestSupport {

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    // do nothing
  }

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    // n/a
  }

  /**
   * Test getting the config properties from the server.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testGetConfigProperties() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    // It's enough that this completes
    final Client c = Client.create();
    final WebResource r =
        c.resource("http://localhost:8080/mapping-rest/security/properties");
    final String response =
        r.accept(MediaType.APPLICATION_JSON).get(String.class);
    Logger.getLogger(getClass()).info("  properties = " + response);

  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    // do nothing
  }

  /**
   * Teardown class.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {
    // do nothing
  }

}
