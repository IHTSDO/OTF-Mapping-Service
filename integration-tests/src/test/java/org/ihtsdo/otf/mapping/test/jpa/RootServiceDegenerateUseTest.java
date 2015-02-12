package org.ihtsdo.otf.mapping.test.jpa;

import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.services.RootService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Root Service Jpa Degenerate Use" Test Cases.
 */
public class RootServiceDegenerateUseTest {

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
   * Test degenerate use of transaction management methods of
   * {@link RootServiceJpa} .
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseJpaRoot001() throws Exception {

    service.setTransactionPerOperation(true);
    boolean flag = false;
    try {
      service.beginTransaction();
    } catch (Exception e) {
      flag = true;
    }
    Assert.assertTrue("Expeced exception did not occur", flag);

    service.setTransactionPerOperation(false);
    flag = false;
    service.beginTransaction();
    try {
      service.beginTransaction();
    } catch (Exception e) {
      flag = true;
    }
    Assert.assertTrue("Expeced exception did not occur", flag);

  }

  /**
   * Test degenerate use of rollback and commit methods of
   * {@link RootServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseJpaRoot002() throws Exception {

    service.setTransactionPerOperation(true);
    boolean flag = false;
    try {
      service.rollback();
    } catch (Exception e) {
      flag = true;
    }
    Assert.assertTrue("Expeced exception did not occur", flag);

    flag = false;
    try {
      service.commit();
    } catch (Exception e) {
      flag = true;
    }
    Assert.assertTrue("Expeced exception did not occur", flag);

    service.setTransactionPerOperation(false);
    flag = false;
    try {
      service.rollback();
    } catch (Exception e) {
      flag = true;
    }
    Assert.assertTrue("Expeced exception did not occur", flag);

    flag = false;
    try {
      service.commit();
    } catch (Exception e) {
      flag = true;
    }
    Assert.assertTrue("Expeced exception did not occur", flag);

    service.beginTransaction();
    service.rollback();
    flag = false;
    try {
      service.rollback();
    } catch (Exception e) {
      flag = true;
    }
    Assert.assertTrue("Expeced exception did not occur", flag);

    service.beginTransaction();
    service.commit();
    flag = false;
    try {
      service.commit();
    } catch (Exception e) {
      flag = true;
    }
    Assert.assertTrue("Expeced exception did not occur", flag);

  }

  /**
   * Test degenerate use of clear and close features of {@link RootServiceJpa}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseJpaRoot003Procedure() throws Exception {
    service.close();
    boolean flag = false;
    try {
      service.beginTransaction();
    } catch (Exception e) {
      flag = true;
    }
    Assert.assertTrue("Expected exception not thrown.", flag);
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
