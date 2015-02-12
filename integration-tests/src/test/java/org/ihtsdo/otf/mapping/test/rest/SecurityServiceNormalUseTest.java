package org.ihtsdo.otf.mapping.test.rest;

import org.ihtsdo.otf.mapping.rest.SecurityServiceRest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Security Service REST Normal Use" Test Cases.
 */
public class SecurityServiceNormalUseTest {

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
   * Test normal use of the authenticate methods of {@link SecurityServiceRest}.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseRestSecurity001() throws Exception {
    String authToken = service.authenticate("guest", "guest");
    Assert.assertEquals(authToken, "guest");
    authToken = service.authenticate("demo_lead", "demo_lead");
    Assert.assertEquals(authToken, "demo_lead");
  }

  /**
   * Test normal use of logout for {@link SecurityServiceRest}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseRestSecurity002() throws Exception {
    service.authenticate("guest", "guest");
    service.authenticate("demo_lead", "demo_lead");
    service.logout("guest");
    service.logout("demo_lead");
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
