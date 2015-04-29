package org.ihtsdo.otf.mapping.test.jpa;

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
  private RootService service;

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
    service = new RootServiceJpa() {
      @Override
      public void initializeFieldNames() throws Exception {
        // do nothing
      }
    };
  }

  /**
   * Test edge cases of transaction management methods of {@link RootServiceJpa}
   * .
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesJpaRoot001() throws Exception {

    service.setTransactionPerOperation(true);
    service.setTransactionPerOperation(true);

    service.setTransactionPerOperation(false);
    service.setTransactionPerOperation(false);

  }

  /**
   * Test edge cases of rollback and commit methods of {@link RootServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesJpaRoot002() throws Exception {
    service.setTransactionPerOperation(false);

    service.beginTransaction();
    service.commit();

    service.beginTransaction();
    service.rollback();
  }

  /**
   * Test edge cases of clear and close features of {@link RootServiceJpa}
   * Procedure 1.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesJpaRoot003Procedure1() throws Exception {
    // Procedure 1
    service.close();
  }

  /**
   * Test edge cases of clear and close features of {@link RootServiceJpa}
   * Procedure 2.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesJpaRoot003Procedure2() throws Exception {
    // Procedure 2
    service.clear();
    service.clear();
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
