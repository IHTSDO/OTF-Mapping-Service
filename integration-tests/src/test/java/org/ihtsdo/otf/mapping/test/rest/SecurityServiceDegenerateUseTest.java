package org.ihtsdo.otf.mapping.test.rest;

import javax.ws.rs.WebApplicationException;

import org.ihtsdo.otf.mapping.rest.SecurityServiceRest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Security Service REST Degenerate Use" Test Cases.
 */
public class SecurityServiceDegenerateUseTest {

  /** The service. */
  private SecurityServiceRest service;

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
    service = new SecurityServiceRest();
  }

  /**
   * Test degenerate use of the authenticate methods of
   * {@link SecurityServiceRest}.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseRestSecurity001() throws Exception {

    boolean flag = false;
    try {
      service.authenticate("demo_lead", null);
    } catch (WebApplicationException e) {
      if (e.getResponse().getStatus() == 401) {
        flag = true;
      }
    }
    Assert.assertTrue(
        "Expected web application exception with 401 status did not occur",
        flag);

    flag = false;
    try {
      service.authenticate("not-a-real-user", ".");
    } catch (WebApplicationException e) {
      if (e.getResponse().getStatus() == 401) {
        flag = true;
      }
    }
    Assert.assertTrue(
        "Expected web application exception with 401 status did not occur",
        flag);

    flag = false;
    try {
      service.authenticate(null, ".");
    } catch (WebApplicationException e) {
      if (e.getResponse().getStatus() == 401) {
        flag = true;
      }
    }
    Assert.assertTrue(
        "Expected web application exception with 401 status did not occur",
        flag);

  }

  /**
   * Test degenerate use of logout for {@link SecurityServiceRest}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseRestSecurity002() throws Exception {
    boolean flag = false;
    try {
      service.logout(null);
    } catch (Exception e) {
      e.printStackTrace();
      flag = true;
    }
    Assert.assertTrue("Expected exception did not occur", flag);

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
