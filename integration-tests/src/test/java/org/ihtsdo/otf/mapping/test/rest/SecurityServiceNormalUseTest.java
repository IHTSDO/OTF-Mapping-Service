package org.ihtsdo.otf.mapping.test.rest;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rest.SecurityServiceRest;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.test.other.MapRecordJpaTest;
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

	/** The manager. */
	private static EntityManager manager;

	/** The factory. */
	private static EntityManagerFactory factory;

	/** The full text entity manager. */
	private static FullTextEntityManager fullTextEntityManager;

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

		// create Entity Manager
		String configFileName = System.getProperty("run.config.test");
		Logger.getLogger(MapRecordJpaTest.class).info("  run.config.test = " + configFileName);
		Properties config = new Properties();
		FileReader in = new FileReader(new File(configFileName));
		config.load(in);
		in.close();
		Logger.getLogger(MapRecordJpaTest.class).info("  properties = " + config);
		factory = Persistence.createEntityManagerFactory("MappingServiceDS", config);
		manager = factory.createEntityManager();


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
