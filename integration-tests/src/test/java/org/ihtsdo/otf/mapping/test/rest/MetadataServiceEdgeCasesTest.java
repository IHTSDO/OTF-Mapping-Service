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
public class MetadataServiceEdgeCasesTest {

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
   * Test edge cases of getting the latest terminology versions from
   * {@link MetadataServiceRest}.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesRestMetadata001() throws Exception {
    // TBD
  }

  /**
   * Test edge cases of getting all terminology versions from
   * {@link MetadataServiceRest}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesRestMetadata002() throws Exception {
    // TBD
  }

  /**
   * Test edge cases of getting all metadata for terminology from
   * {@link MetadataServiceRest}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesRestMetadata003() throws Exception {
    // TBD
  }

  /**
   * Test edge cases of getting all metadata from {@link MetadataServiceRest}.
   *
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCasesRestMetadata004() throws Exception {
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
