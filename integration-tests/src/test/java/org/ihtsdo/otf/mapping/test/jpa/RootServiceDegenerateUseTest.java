package org.ihtsdo.otf.mapping.test.jpa;

import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.services.RootService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Root Service Jpa Degenerate Use" Test Cases.
 */
public class RootServiceDegenerateUseTest {

  /** The service. */
  private static RootService service;

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    service = new RootServiceJpa() {
      @Override
      public void initializeFieldNames() throws Exception {
        // do nothing
      }
    };
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
   * Test degenerate use of transaction management methods of {@link RootServiceJpa}
   * .
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseR001() throws Exception {
    // placeholder
  }

  /**
   * Test degenerate use of rollback and commit methods of {@link RootServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseR002() throws Exception {
    // placeholder
  }

  /**
   * Test degenerate use of clear and close features of {@link RootServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseR003() throws Exception {
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
    service.close();
  }

}
