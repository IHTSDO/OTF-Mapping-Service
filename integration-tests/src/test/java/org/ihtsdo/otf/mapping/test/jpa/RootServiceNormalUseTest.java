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
 * Implementation of the "Root Service Jpa Normal Use" Test Cases.
 */
public class RootServiceNormalUseTest {

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
   * Test normal use of transaction management methods of {@link RootServiceJpa}
   * .
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseJpaRoot001() throws Exception {
    assertTrue(service.getTransactionPerOperation());
    // Set transaction per operation to false
    service.setTransactionPerOperation(false);
    // Verify that it is set to false
    assertFalse(service.getTransactionPerOperation());
  }

  /**
   * Test normal use of rollback and commit methods of {@link RootServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseJpaRoot002() throws Exception {
    service.setTransactionPerOperation(false);
    service.beginTransaction();
    service.rollback();
    service.beginTransaction();
    service.commit();
  }

  /**
   * Test normal use of clear and close features of {@link RootServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseJpaRoot003() throws Exception {
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
