package org.ihtsdo.otf.mapping.test.jpa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.services.RootService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Root Service Jpa Edge Cases" Test Cases.
 */
public class RootServiceEdgeCasesTest {

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
   * Test edge cases of transaction management methods of {@link RootServiceJpa}
   * .
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void testEdgeCasesR001() throws Exception {
    assertTrue(service.getTransactionPerOperation());
    // Set transaction per operation to false
    service.setTransactionPerOperation(false);
    // Verify that it is set to false
    assertFalse(service.getTransactionPerOperation());
  }

  /**
   * Test edge cases of rollback and commit methods of {@link RootServiceJpa}.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void testEdgeCasesR002() throws Exception {
    service.setTransactionPerOperation(false);
    service.beginTransaction();
    service.rollback();
    service.beginTransaction();
    service.commit();
  }

  /**
   * Test edge cases of clear and close features of {@link RootServiceJpa}.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void testEdgeCasesR003() throws Exception {
    // Procedure 1
    service.setTransactionPerOperation(false);
    service.beginTransaction();
    service.clear();
    service.commit();

    // Procedure 2
    service.setTransactionPerOperation(true);
    service.clear();

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
