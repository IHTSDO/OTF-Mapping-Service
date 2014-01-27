/*package org.ihtsdo.otf.mapping.rest;


import java.util.List;

import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.junit.BeforeClass;
import org.junit.Test;


*//**
 * The Class MapProjectJpaTest.
 * 
 * Provides test cases
 * 1. confirm MapProject data load returns expected data
 * 2. confirms indexed fields are indexed
 * 3. confirms MapProject is audited and changes are logged in audit table
 *//*
public class MappingServiceRestTest {

	*//** The edit mapping service *//*
	private static MappingServiceRest service = new MappingServiceRest();

	*//**
	 * Creates db tables, load test objects and create indexes to prepare for test
	 * cases.
	 *//*
	@BeforeClass
	public static void init() {
		
		System.out.println("Initializing EditMappingServiceJpa");

	}
	
	
	*//** 
	 * Test retrieval of existing database elements
	 * @throws Exception the exception
	 *//*
	@Test
	public void testRetrieveElements() throws Exception {
		System.out.println("Testing element add...");
		
		// retrieve all, in JSON
		List<MapProject> projects = service.getMapProjectsJson();
		List<MapLead> leads = service.getMapLeadsJson();
		List<MapSpecialist> specialists = service.getMapSpecialistsJson();
		
		System.out.println(Integer.toString(projects.size()) + " projects found");
		System.out.println(Integer.toString(specialists.size()) + " speciaists found");
		System.out.println(Integer.toString(leads.size()) + " leads found");
		
		// retrieve individual projects
		for (MapProject m : projects) {
			service.getMapProjectForIdJson(m.getId());
			service.getMapProjectForNameJson(m.getName());
		}
		
		// retrieve projects by specialist
		for (MapSpecialist m : specialists) {
			service.getMapProjectsForSpecialistJson(m);
		}
		
		// retrieve projects by lead
		for (MapLead m : leads) {
			service.getMapProjectsForLeadJson(m);
		}
		
		
		
	}
	
	
}
*/