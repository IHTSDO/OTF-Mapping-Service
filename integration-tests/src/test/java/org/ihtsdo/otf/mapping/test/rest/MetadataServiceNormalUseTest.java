package org.ihtsdo.otf.mapping.test.rest;

import org.ihtsdo.otf.mapping.rest.MetadataServiceRest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Metadata Service REST Normal Use" Test Cases.
 */
public class MetadataServiceNormalUseTest {

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
    service = new MetadataServiceRest();
  }

  /**
   * Test normal use of getting the latest terminology versions from
   * {@link MetadataServiceRest}.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseRestMetadata001() throws Exception {
    // TBD
  }

  /**
   * Test normal use of getting all terminology versions from
   * {@link MetadataServiceRest}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseRestMetadata002() throws Exception {
    // TBD
  }

  /**
   * Test normal use of getting all metadata for terminology from
   * {@link MetadataServiceRest}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseRestMetadata003() throws Exception {
    // TBD
  }

  /**
   * Test normal use of getting all metadata from {@link MetadataServiceRest}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseRestMetadata004() throws Exception {
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
