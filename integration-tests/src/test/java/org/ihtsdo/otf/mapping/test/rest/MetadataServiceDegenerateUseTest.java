package org.ihtsdo.otf.mapping.test.rest;

import org.ihtsdo.otf.mapping.jpa.services.rest.MetadataServiceRest;
import org.ihtsdo.otf.mapping.rest.impl.MetadataServiceRestImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Metadata Service REST Degenerate Use" Test Cases.
 */
public class MetadataServiceDegenerateUseTest {

  /** The service. */
  @SuppressWarnings("unused")
  private MetadataServiceRest service;

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
    service = new MetadataServiceRestImpl();
  }

  /**
   * Test degenerate use of getting the latest terminology versions from
   * {@link MetadataServiceRestImpl}.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseRestMetadata001() throws Exception {
    // TBD
  }

  /**
   * Test degenerate use of getting all terminology versions from
   * {@link MetadataServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseRestMetadata002() throws Exception {
    // TBD
  }

  /**
   * Test degenerate use of getting all metadata for terminology from
   * {@link MetadataServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseRestMetadata003() throws Exception {
    // TBD
  }

  /**
   * Test degenerate use of getting all metadata from
   * {@link MetadataServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUseRestMetadata004() throws Exception {
    // TBD
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
