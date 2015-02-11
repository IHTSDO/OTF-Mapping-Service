package org.ihtsdo.otf.mapping.test.jpa;

import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Security Service Jpa Edge Cases" Test Cases.
 */
public class SecurityServiceEdgeCasesTest {

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
   * Test edge cases use of the authenticate methods of {@link SecurityServiceJpa}.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesS001() throws Exception {
    // placeholder
  }

  /**
   * Test edge cases of application roles of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesS002() throws Exception {
    // placeholder
  }

  /**
   * Test edge cases of map project roles of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesS003() throws Exception {
    // placeholder
  }

  /**
   * Test edge cases of username methods of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesS004() throws Exception {
    // placeholder
  }
  
  /**
   * Test edge cases of logout method of {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesS005() throws Exception {
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
