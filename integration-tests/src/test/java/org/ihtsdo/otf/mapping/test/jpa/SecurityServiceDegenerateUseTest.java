package org.ihtsdo.otf.mapping.test.jpa;

import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Security Service Jpa Degenerate Use" Test Cases.
 */
public class SecurityServiceDegenerateUseTest {

  /** The service. */
  private SecurityService service;

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
    service = new SecurityServiceJpa();
  }

  /**
   * Test degenerate use of the authenticate methods of
   * {@link SecurityServiceJpa}.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseJpaSecurity001() throws Exception {
    // placeholder
  }

  /**
   * Test degenerate use of application roles of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseJpaSecurity002() throws Exception {
    // placeholder
  }

  /**
   * Test degenerate use of map project roles of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseJpaSecurity003() throws Exception {
    // placeholder
  }

  /**
   * Test degenerate use of username methods of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseJpaSecurity004() throws Exception {
    // placeholder
  }

  /**
   * Test degenerate use of logout method of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseJpaSecurity005() throws Exception {
    // placeholder
  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    // close test fixtures per class
    service.close();
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
