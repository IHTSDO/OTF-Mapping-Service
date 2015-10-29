package org.ihtsdo.otf.mapping.test.rest;

import org.ihtsdo.otf.mapping.rest.SecurityServiceRest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Security Service REST Degenerate Use" Test Cases.
 */
public class SecurityServiceEdgeCasesTest {

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
   * Test edge cases of the authenticate methods of {@link SecurityServiceRest}.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesRestSecurity001() throws Exception {

    service.authenticate("guest", "guest");
    service.authenticate("guest", "guest");
    service.authenticate("demo_lead", ".");
    service.authenticate("demo_lead", ".");
  }

  /**
   * Test edge cases of logout for {@link SecurityServiceRest}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesRestSecurity002() throws Exception {
    service.logout("guest");

    service.authenticate("guest", "guest");
    service.logout("guest");
    service.logout("guest");

    service.authenticate("demo_lead", ".");
    service.logout("demo_lead");
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
