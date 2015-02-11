package org.ihtsdo.otf.mapping.test.jpa;

import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Security Service Jpa Normal Use" Test Cases.
 */
public class SecurityServiceNormalUseTest {

  /**  The service. */
  private static SecurityService service;
  
  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    service = new SecurityServiceJpa();
  }

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    // do nothing
  }

  /**
   * Test normal use of the authenticate methods of {@link SecurityServiceJpa}.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseS001() throws Exception {
    // placeholder
  }

  /**
   * Test normal use of application roles of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseS002() throws Exception {
    // placeholder
  }

  /**
   * Test normal use of map project roles of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseS003() throws Exception {
    // placeholder
  }

  /**
   * Test normal use of username methods of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseS004() throws Exception {
    // placeholder
  }
  
  /**
   * Test normal use of logout method of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseS005() throws Exception {
    // placeholder
  }
  
  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    // close test fixtures per test
  }

  /**
   * Teardown class.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {
    // close test fixtures per class
  }

}
