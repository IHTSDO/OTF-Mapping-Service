package org.ihtsdo.otf.mapping.test.rest;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rest.SecurityServiceRest;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Security Service REST Degenerate Use" Test Cases.
 */
public class SecurityServiceEdgeCasesTest {

  /** The securityService. */
  private static SecurityServiceRest securityService;

  /** The mapping service. */
  private static MappingService mappingService;

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    securityService = new SecurityServiceRest();
    mappingService = new MappingServiceJpa();

    // create map users
    MapUser user = new MapUserJpa();
    user.setName("guest");
    user.setUserName("guest");
    user.setEmail("guest");
    user.setApplicationRole(MapUserRole.VIEWER);
    mappingService.addMapUser(user);

    MapUser demoUser = new MapUserJpa();
    demoUser.setName("demo_lead");
    demoUser.setUserName("demo_lead");
    demoUser.setEmail("demo_lead");
    demoUser.setApplicationRole(MapUserRole.VIEWER);
    mappingService.addMapUser(demoUser);
  }

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Before
  public void setup() throws Exception {
    securityService = new SecurityServiceRest();
  }

  /**
   * Test edge cases of the authenticate methods of {@link SecurityServiceRest}.
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void testEdgeCasesRestSecurity001() throws Exception {

    securityService.authenticate("guest", "guest");
    securityService.authenticate("guest", "guest");
    securityService.authenticate("demo_lead", ".");
    securityService.authenticate("demo_lead", ".");
  }

  /**
   * Test edge cases of logout for {@link SecurityServiceRest}.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void testEdgeCasesRestSecurity002() throws Exception {
    securityService.logout("guest");

    securityService.authenticate("guest", "guest");
    securityService.logout("guest");
    securityService.logout("guest");

    securityService.authenticate("demo_lead", ".");
    securityService.logout("demo_lead");
    securityService.logout("demo_lead");

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
    // remove the users
    for (MapUser user : mappingService.getMapUsers().getMapUsers()) {
      mappingService.removeMapUser(user.getId());
    }

    mappingService.close();
  }

}
