package org.ihtsdo.otf.mapping.jpa.services;


import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
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
public class MappingServiceTest {

	/** The  mapping service */
	private static MappingServiceJpa service;
	
	/** The manager. */
	private static EntityManager manager;

	/** The factory. */
	private static EntityManagerFactory factory;

	/** The full text entity manager. */
	private static FullTextEntityManager fullTextEntityManager;
	
	/** The audit history reader. */
	private static AuditReader reader;
	

	/**
	 * Creates db tables, load test objects and create indexes to prepare for test
	 * cases.
	 */
	@BeforeClass
	public static void init() {
		
		System.out.println("Initializing EditMappingServiceJpa");

		// construct the mapping service
		service = new MappingServiceJpa();
		
	/*	// create local connection entities for audit testing
		factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		manager = factory.createEntityManager();
		fullTextEntityManager = Search.getFullTextEntityManager(manager);
		reader = AuditReaderFactory.get( manager );

		fullTextEntityManager.purgeAll(MapProjectJpa.class);
		fullTextEntityManager.flushToIndexes();*/
	}
	
	/**
	 * Close services after complete
	 */
	/*@AfterClass
	public static void cleanup() {
		
		System.out.println("Cleaning up EditMappingServiceJpa");
		
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
			fail("Failed to remove generated data.");
		}
		
		manager.close();
		factory.close();
	}*/
	
	private static void fail(String string) {
		
		System.out.println("Failed: " + string);
		
	}

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
		mapLead.setEmail("***REMOVED***");
		leads.add(mapLead);

		mapLead = new MapLeadJpa();
		mapLead.setName("Rory");
		mapLead.setUserName("rda");
		mapLead.setEmail("***REMOVED***");
		leads.add(mapLead);
		
		MapSpecialistJpa mapSpecialist = new MapSpecialistJpa();
		mapSpecialist.setName("Patrick");
		mapSpecialist.setUserName("pgranvold");
		mapSpecialist.setEmail("***REMOVED***");
		specialists.add(mapSpecialist);
		
		mapSpecialist = new MapSpecialistJpa();
		mapSpecialist.setName("Deborah");
		mapSpecialist.setUserName("dshapiro");
		mapSpecialist.setEmail("***REMOVED***");
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
		mapProject.addMapLead(leads.get(0));
		mapProject.addMapLead(leads.get(1));
		mapProject.addMapSpecialist(specialists.get(0));
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
		
		
		try {
			
			for (MapSpecialist m : specialists) {
				System.out.println("Adding map specialist " + m.getName());
				service.addMapSpecialist(m);
			}
		} catch (Exception e) {
			fail("Failed to add map specialists");
		}
		
		try {
			
			for (MapLead m : leads) {
				System.out.println("Adding map lead " + m.getName());
				service.addMapLead(m);
			}
		} catch (Exception e) {
			fail("Failed to add map leads");
		}
		
		try {
			
			for (MapProject m : projects) {
				System.out.println("Adding map project " + m.getName());
				service.addMapProject(m);
			}
		} catch (Exception e) {
			fail("Failed to add map projects");
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
				System.out.println("Successful retrieval by id= " + mp.getId().toString());
			} catch (Exception e) {
				fail("getMapProject failed for id = " + m.getId().toString());
			}
		}
		
		// Test retrieval of projects by specialist
		for (MapSpecialist m : specialists) {
			System.out.println("Projects for specialist " + m.getId().toString() + ", " + m.getName());
			try {
				for (MapProject p : service.getMapProjectsForMapSpecialist(m)) {
					System.out.println("-> " + p.getId().toString() + ", " + p.getName());
				}
			} catch (Exception e) {
				
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
	
	/** 
	 * Test updating each type of element
	 * @throws Exception
	 */
	@Test
	public void testUpdateElements() throws Exception {
		System.out.println("Testing element update...");
		
		List<MapProject> projects = service.getMapProjects();
		List<MapSpecialist> specialists = service.getMapSpecialists();
		List<MapLead> leads = service.getMapLeads();
			
		String changedValue = "updatetest";
		
		// test update and envers audit of map project
		MapProject project_old = projects.get(0);  
		MapProject project_new;
		//List<Number> revNumbers = reader.getRevisions(MapProject.class, project_old); // TODO Reenable for audit testing
		
		try {
			project_old.setDestinationTerminology(changedValue);
			service.updateMapProject(project_old);
			
			project_new = service.getMapProject(project_old.getId());
		
			if (!project_new.getDestinationTerminology().equals(changedValue)) {
					fail("Failed to update project");
			} else {
				System.out.println("Project update successful");
			}
			// TODO Reenable for audit testing
			/*if (reader.getRevisions(MapProject.class, project_old).size() != revNumbers.size() + 1) {
				fail("Failed to update revision table:  number of revisions has not increased by 1");
			}*/
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected error in element update");
		}
		
		// test update and envers audit of map specialist 
		MapSpecialist specialist_old = specialists.get(0);  
		MapSpecialist specialist_new;
		//revNumbers = reader.getRevisions(MapSpecialist.class, specialist_old); // TODO Reenable for audit testing
		
		try {
			specialist_old.setEmail(changedValue);
			service.updateMapSpecialist(specialist_old);
			
			specialist_new = service.getMapSpecialist(specialist_old.getId());
		
			if (!specialist_new.getEmail().equals(changedValue)) {
					fail("Failed to update specialist");
			} else {
				System.out.println("Specialist update successful");
			}
			// TODO Reenable for audit testing
			/*if (reader.getRevisions(MapSpecialist.class, specialist_old).size() != revNumbers.size() + 1) {
				fail("Failed to update revision table:  number of revisions has not increased by 1");
			}*/
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected error in element update");
		}
		
		// test update and envers audit of map lead

		MapLead lead_old = leads.get(0);  
		MapLead lead_new;
		//revNumbers = reader.getRevisions(MapLead.class, lead_old);  // TODO Reenable for audit testing
		
		try {
			lead_old.setEmail(changedValue);
			service.updateMapLead(lead_old);
			
			lead_new = service.getMapLead(lead_old.getId());
		
			if (!lead_new.getEmail().equals(changedValue)) {
					fail("Failed to update lead");
			} else {
				System.out.println("Lead update successful");
			}
			
			// TODO Reenable for audit testing
			/*if (reader.getRevisions(MapLead.class, lead_old).size() != revNumbers.size() + 1) {
				fail("Failed to update revision table:  number of revisions has not increased by 1");
			}*/
		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected error in element update");
		}
		
		// TODO Determine desired audit results for project
		
	}
	
	/**
	 * Test removal of each type of element
	 * Further test propagation of removal of lead/specialist on project
	 * @throws Exception
	 */
	// TODO: This still needs some way to handle the map_projects_map_[leads/specialists] constraints
	//@Test
	public void testRemoveElements() throws Exception {
		System.out.println("Testing element remove...");
		
		// test delete and envers audit of map project
		List<MapProject> projects = service.getMapProjects();
		List<MapSpecialist> specialists = service.getMapSpecialists();
		List<MapLead> leads = service.getMapLeads();
		
		MapProject project_removed = projects.get(0);
		MapSpecialist specialist_removed = specialists.get(0);
		MapLead lead_removed = leads.get(0);
		
		// test delete and envers audit of map specialist
		try {
			service.removeMapSpecialist(specialist_removed.getId());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to remove map specialist");
		}
		
		try {
			service.removeMapLead(lead_removed.getId());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to remove map lead");
		}
		
		try {
			service.removeMapProject(project_removed.getId());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to remove map project");
		}
		
		// TODO Check expected audit results and add checks
		
		// check if specialist / lead delete propagates through project
		
		MapProject project = service.getMapProject(new Long(1)); // originally two leads and specialists on this project
	
		if (project.getMapSpecialists().size() != 1) {
			fail("Removal of map specialist did not remove specialist from project");
		} else if (!project.getMapSpecialists().contains(specialist_removed)) {
			fail("Removal of map specialist resulted in wrong specialist being removed ");
		}
		
		if (project.getMapLeads().size() != 1) {
			fail("Removal of map lead did not remove specialist from project");
		} else if (project.getMapLeads().contains(lead_removed)) {
			fail("Removal of map lead resulted in wrong lead being removed");
		}
		
		
	}
	
}
