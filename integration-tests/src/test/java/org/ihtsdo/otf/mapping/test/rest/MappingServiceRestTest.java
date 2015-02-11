package org.ihtsdo.otf.mapping.test.rest;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.rest.MappingServiceRest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Retrieves some elements from a db that is already populated.
 *
 */
public class MappingServiceRestTest {

  /** The edit mapping service */
  private static MappingServiceRest service;

  /**
   * Instantiates an empty {@link MappingServiceRestTest}.
   *
   * @throws Exception the exception
   */
  public MappingServiceRestTest() throws Exception {
    service = new MappingServiceRest();
  }

  /**
   * Initializes the tests.
   */
  @BeforeClass
  public static void init() {

    Logger.getLogger(MappingServiceRestTest.class).info(
        "Initializing MappingServiceRestTest");

  }

  /**
   * Test retrieval of existing database elements
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void testRetrieveElements() throws Exception {
    Logger.getLogger(MappingServiceRestTest.class).info(
        "Testing retrieval of elements...");

    // retrieve all
    MapProjectList projects = service.getMapProjects("adm");
    MapUserList users = service.getMapUsers("adm");

    Logger.getLogger(MappingServiceRestTest.class).info(
        Integer.toString(projects.getCount()) + " projects found");
    Logger.getLogger(MappingServiceRestTest.class).info(
        Integer.toString(users.getCount()) + " users found");

    // retrieve all projects
    service.getMapProjects("adm");
    /*
     * // retrieve projects by specialist for (MapUser m : users.getMapUsers())
     * { service.getMapProjectsForUser(m.getId().toString(), "adm"); }
     */

  }

}
