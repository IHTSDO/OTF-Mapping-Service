package org.ihtsdo.otf.mapping.test.rest;

import static org.junit.Assert.fail;

import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rest.SecurityServiceRest;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Security Service REST Normal Use" Test Cases.
 */
public class SecurityServiceNormalUseTest {

	/** The service. */
	private static SecurityServiceRest securityService;
	private static MappingService mappingService;

	/**
	 * Create test fixtures for class.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@BeforeClass
	public static void setupClass() throws Exception {

		securityService = new SecurityServiceRest();
		mappingService = new MappingServiceJpa();

		// remove the users to ensure clean initial condition
		for (MapUser user : mappingService.getMapUsers().getMapUsers()) {
			mappingService.removeMapUser(user.getId());
		}

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
	 * @throws Exception
	 *             the exception
	 */
	@Before
	public void setup() throws Exception {

	}

	/**
	 * Test normal use of the authenticate methods of
	 * {@link SecurityServiceRest}.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testNormalUseRestSecurity001() throws Exception {
		try {
			String authToken = securityService.authenticate("guest", "guest");
			Assert.assertEquals(authToken, "guest");
			authToken = securityService.authenticate("demo_lead", "demo_lead");
			Assert.assertEquals(authToken, "demo_lead");
		} catch (Exception e) {
			fail("Error during login test");
			e.printStackTrace();
		}
	}

	/**
	 * Test normal use of logout for {@link SecurityServiceRest}.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testNormalUseRestSecurity002() throws Exception {
		try {
			securityService.authenticate("guest", "guest");
			securityService.authenticate("demo_lead", "demo_lead");
			securityService.logout("guest");
			securityService.logout("demo_lead");
		} catch (Exception e) {
			fail("Error during logout test");
			e.printStackTrace();
		}
	}

	/**
	 * Teardown.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@After
	public void teardown() throws Exception {
		// do nothing
	}

	/**
	 * Teardown class.
	 *
	 * @throws Exception
	 *             the exception
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
