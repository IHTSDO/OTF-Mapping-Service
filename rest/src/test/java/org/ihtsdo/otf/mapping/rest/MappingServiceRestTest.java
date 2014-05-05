package org.ihtsdo.otf.mapping.rest;



import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Retrieves some elements from a db that is already populated.
 *
 */
public class MappingServiceRestTest {

	/** The edit mapping service */
	private static MappingServiceRest service = new MappingServiceRest();


	/**
	 * Initializes the tests.
	 */
	@BeforeClass
	public static void init() {
		
		Logger.getLogger(MappingServiceRestTest.class).info("Initializing MappingServiceRestTest");

	}
	
	
	/** 
	 * Test retrieval of existing database elements
	 * @throws Exception the exception
	 */
	@Test
	public void testRetrieveElements() throws Exception {
		Logger.getLogger(MappingServiceRestTest.class).info("Testing retrieval of elements...");
		
		// retrieve all
		MapProjectList projects = service.getMapProjects();
		MapUserList users = service.getMapUsers();
		
		Logger.getLogger(MappingServiceRestTest.class).info(Integer.toString(projects.getCount()) + " projects found");
		Logger.getLogger(MappingServiceRestTest.class).info(Integer.toString(users.getCount()) + " users found");
		
		// retrieve individual projects
		for (MapProject m : projects.getMapProjects()) {
			service.getMapProjectForId(m.getId());
		}
		
		// retrieve projects by specialist
		for (MapUser m : users.getMapUsers()) {
			service.getMapProjectsForUser(m.getId());
		}
		
	}
	
	
}
