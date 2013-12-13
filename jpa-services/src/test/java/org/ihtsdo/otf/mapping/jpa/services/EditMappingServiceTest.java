package org.ihtsdo.otf.mapping.jpa.services;


import java.util.ArrayList;
import java.util.List;

import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * The Class MapProjectJpaTest.
 * 
 * Provides test cases
 * 1. confirm MapProject data load returns expected data
 * 2. confirms indexed fields are indexed
 * 3. confirms MapProject is audited and changes are logged in audit table
 */
public class EditMappingServiceTest {

	/** The edit mapping service */
	private static EditMappingServiceJpa editService;
	
	private static MappingServiceJpa service;

	/**
	 * Creates db tables, load test objects and create indexes to prepare for test
	 * cases.
	 */
	@BeforeClass
	public static void init() {
		
		System.out.println("Initializing EditMappingServiceJpa");

		service = new MappingServiceJpa();
		editService = new EditMappingServiceJpa();
	}
	
	/**
	 * Close services after complete
	 */
	@AfterClass
	public static void cleanup() {
		
		editService.close();
		service.close();
	}
	
	/*@AfterClass
	public static void cleanup() {
		
		System.out.println("Cleaning up EditMappingServiceJpa");
		
		EntityManagerFactory factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		EntityManager manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		
		
			// truncate tables
		try {
			
			javax.persistence.Query query;
			
			tx.begin();	
			
			query = manager.createNativeQuery("DELETE FROM map_projects_map_leads");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects_map_specialists");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_leads");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_specialists");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects_map_leads_aud");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects_map_specialists_aud");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_leads_aud");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_specialists_aud");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects_aud");
			query.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			fail("Failure to detach mapProject.");
		}
		
		manager.close();
		factory.close();
	}*/
	
	/**
	 * Create mock data and test adding the objects
	 * @throws Exception
	 */
	@Test
	public void testAddElements() throws Exception {
		
		System.out.println("Testing element add...");
		
		/** The lists */
		List<MapProject> projects = new ArrayList<MapProject>();
	    List<MapSpecialist> specialists = new ArrayList<MapSpecialist>();
		List<MapLead> leads = new ArrayList<MapLead>();
		
		MapLeadJpa mapLead = new MapLeadJpa();
		mapLead.setName("Brian");
		mapLead.setUserName("bcarlsen");
		mapLead.setEmail("bcarlsen@westcoastinformatics.com");
		leads.add(mapLead);

		mapLead = new MapLeadJpa();
		mapLead.setName("Rory");
		mapLead.setUserName("rda");
		mapLead.setEmail("rda@ihtsdo.org");
		leads.add(mapLead);
		
		MapSpecialistJpa mapSpecialist = new MapSpecialistJpa();
		mapSpecialist.setName("Patrick");
		mapSpecialist.setUserName("pgranvold");
		mapSpecialist.setEmail("pgranvold@westcoastinformatics.com");
		specialists.add(mapSpecialist);
		
		mapSpecialist = new MapSpecialistJpa();
		mapSpecialist.setName("Deborah");
		mapSpecialist.setUserName("dshapiro");
		mapSpecialist.setEmail("dshapiro@westcoastinformatics.com");
		specialists.add(mapSpecialist);
		
		mapSpecialist = new MapSpecialistJpa();
		mapSpecialist.setName("Mycroft");
		mapSpecialist.setUserName("doomchinchilla");
		mapSpecialist.setEmail("mycroft@underthecouch.com");
		specialists.add(mapSpecialist);
		
		MapProject mapProject = new MapProjectJpa();
		mapProject.setName("ProjectFoo");
		mapProject.setRefSetId(new Long("5781349"));
		mapProject.setSourceTerminology("SNOMEDCT");
		mapProject.setSourceTerminologyVersion("20130731");
		mapProject.setDestinationTerminology("ICD9CM");
		mapProject.setDestinationTerminologyVersion("2009");
		mapProject.setBlockStructure(true);
		mapProject.setGroupStructure(false);
		mapProject.setPublished(true);
		mapProject.addMapLead(leads.get(0));
		mapProject.addMapSpecialist(specialists.get(0));
		projects.add(mapProject);
		
		mapProject = new MapProjectJpa();
		mapProject.setName("ProjectBar");
		mapProject.setRefSetId(new Long("5781347179"));
		mapProject.setSourceTerminology("SNOMEDCT");
		mapProject.setSourceTerminologyVersion("20130731");
		mapProject.setDestinationTerminology("ICD10CM");
		mapProject.setDestinationTerminologyVersion("2013");
		mapProject.setBlockStructure(false);
		mapProject.setGroupStructure(false);
		mapProject.setPublished(false);
		mapProject.addMapLead(leads.get(1));
		mapProject.addMapSpecialist(specialists.get(1));
		projects.add(mapProject);
		
		mapProject = new MapProjectJpa();
		mapProject.setName("ProjectWUNDERBAR");
		mapProject.setRefSetId(new Long("5235669"));
		mapProject.setSourceTerminology("SNOMEDCT");
		mapProject.setSourceTerminologyVersion("20130731");
		mapProject.setDestinationTerminology("ICD73CM");
		mapProject.setDestinationTerminologyVersion("2100");
		mapProject.setBlockStructure(true);
		mapProject.setGroupStructure(true);
		mapProject.setPublished(false);
		mapProject.addMapLead(leads.get(1));
		mapProject.addMapSpecialist(specialists.get(0));
		mapProject.addMapSpecialist(specialists.get(1));
		projects.add(mapProject);
		
		mapProject = new MapProjectJpa();
		mapProject.setName("ChinchillaApocalypse");
		mapProject.setRefSetId(new Long("46381358"));
		mapProject.setSourceTerminology("Chinchilla Docility");
		mapProject.setSourceTerminologyVersion("20131201");
		mapProject.setDestinationTerminology("Chinchilla ATTACK");
		mapProject.setDestinationTerminologyVersion("20140101");
		mapProject.setBlockStructure(true);
		mapProject.setGroupStructure(true);
		mapProject.setPublished(false);
		mapProject.addMapLead(leads.get(1));
		mapProject.addMapSpecialist(specialists.get(2));
		mapProject.addMapSpecialist(specialists.get(1));
		projects.add(mapProject);
		
		
		
		for (MapSpecialist m : specialists) {
			System.out.println("Adding map specialist " + m.getName());
			editService.addMapSpecialist(m);
		}
		
		for (MapLead m : leads) {
			System.out.println("Adding map lead " + m.getName());
			editService.addMapLead(m);
		}
		
		for (MapProject m : projects) {
			System.out.println("Adding map project " + m.getName());
			editService.addMapProject(m);
		}
		
	}
	
	/**
	 * Test retrieval of elements from existing database
	 * @throws Exception
	 */
	@Test
	public void testRetrieveElements() throws Exception {
		System.out.println("Testing element retrieval...");
		
		
		// Test retrieval of all elements
		
		List<MapProject> projects = service.getMapProjects();
		List<MapSpecialist> specialists = service.getMapSpecialists();
		List<MapLead> leads = service.getMapLeads();
		
		System.out.println(Integer.toString(projects.size()) + " projects found");
		System.out.println(Integer.toString(specialists.size()) + " specialists found");
		System.out.println(Integer.toString(leads.size()) + " leads found");
		
		// Test retrieval of individual projects
		for (MapProject m : projects) {
			try {
				MapProject mp = service.getMapProject(m.getId());
				System.out.println("By id= " + mp.getId().toString());
			} catch (Exception e) {
				System.out.println("getMapProject failed for id = " + m.getId().toString());
			}
			
			try {
				MapProject mp = service.getMapProject(m.getName());
				System.out.println("By name= " + mp.getName().toString());
			} catch (Exception e) {
				System.out.println("getMapProject(name) failed for name = " + m.getName());
			}
			
		}
		
		// Test retrieval of projects by specialist
		for (MapSpecialist m : specialists) {
			System.out.println("Projects for specialist " + m.getId().toString() + ", " + m.getName());
			for (MapProject p : service.getMapProjectsForMapSpecialist(m)) {
				System.out.println("-> " + p.getId().toString() + ", " + p.getName());
			}
		}
		
		// Test retrieval of projects by lead
		for (MapLead m : leads) {
			System.out.println("Projects for lead " + m.getId().toString() + ", " + m.getName());
			for (MapProject p : service.getMapProjectsForMapLead(m)) {
				System.out.println("-> " + p.getId().toString() + ", " + p.getName());
			}
		}
	}
	/*
	
	@Test
	public void testRemoveElements() throws Exception {
		System.out.println("Testing element remove...");
	}*/
	
}
