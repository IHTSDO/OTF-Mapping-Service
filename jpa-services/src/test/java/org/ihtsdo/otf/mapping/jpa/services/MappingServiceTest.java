/*package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.junit.BeforeClass;
import org.junit.Test;

*//**
 * The Class MapProjectJpaTest.
 * 
 * Provides test cases 1. confirm MapProject data load returns expected data 2.
 * confirms indexed fields are indexed 3. confirms MapProject is audited and
 * changes are logged in audit table
 *//*
public class MappingServiceTest {

	*//** The mapping service *//*
	private static MappingServiceJpa service;

	*//** The manager. *//*
	private static EntityManager manager;

	*//** The factory. *//*
	private static EntityManagerFactory factory;

	*//**
	 * Creates db tables, load test objects and create indexes to prepare for
	 * test cases.
	 *//*
	@BeforeClass
	public static void init() {

		System.out.println("Ensuring test database is empty");
		cleanup();
		
		System.out.println("Initializing EditMappingServiceJpa");

		// construct the mapping service
		service = new MappingServiceJpa();

		
		 * // create local connection entities for audit testing factory =
		 * Persistence.createEntityManagerFactory("MappingServiceDS"); manager =
		 * factory.createEntityManager(); fullTextEntityManager =
		 * Search.getFullTextEntityManager(manager); reader =
		 * AuditReaderFactory.get( manager );
		 * 
		 * fullTextEntityManager.purgeAll(MapProjectJpa.class);
		 * fullTextEntityManager.flushToIndexes();
		 
	}

	*//**
	 * Close services after complete
	 *//*
	// @AfterClass
	public static void cleanup() {

		System.out.println("Cleaning up EditMappingServiceJpa");

		// create new database connection
		factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();

		// remove remaining test data
		javax.persistence.Query query;
		tx.begin();

		////////////////////////////
		// Delete from main tables
		////////////////////////////
		
		// notes
		query = manager.createNativeQuery("DELETE FROM map_entries_map_notes");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_records_map_notes");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_notes");
		query.executeUpdate();
		
		// principles
		query = manager.createNativeQuery("DELETE FROM map_entries_map_principles");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_records_map_principles");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_projects_map_principles");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_principles");
		query.executeUpdate();
		
		// advices
		query = manager.createNativeQuery("DELETE FROM map_entries_map_advices");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_projects_map_advices");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_advices");
		query.executeUpdate();
		
		// entries
		query = manager.createNativeQuery("DELETE FROM map_entries");
		query.executeUpdate();
		
		// records
		query = manager.createNativeQuery("DELETE FROM map_records");
		query.executeUpdate();
		
		// leads
		query = manager.createNativeQuery("DELETE FROM map_projects_map_leads");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_leads");
		query.executeUpdate();
		
		// specialists
		query = manager.createNativeQuery("DELETE FROM map_projects_map_specialists");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_specialists");
		query.executeUpdate();
		
		// projects
		query = manager.createNativeQuery("DELETE FROM map_projects");
		query.executeUpdate();
		
		tx.commit();
		
		////////////////////////////
		// Delete from audit tables
		////////////////////////////
		
		tx.begin();
		
		// notes
		query = manager.createNativeQuery("DELETE FROM map_entries_map_notes_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_records_map_notes_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_notes_aud");
		query.executeUpdate();
		
		// principles
		query = manager.createNativeQuery("DELETE FROM map_entries_map_principles_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_records_map_principles_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_projects_map_principles_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_principles_aud");
		query.executeUpdate();
		
		// advices
		query = manager.createNativeQuery("DELETE FROM map_entries_map_advices_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_projects_map_advices_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_advices_aud");
		query.executeUpdate();
		
		// entries
		query = manager.createNativeQuery("DELETE FROM map_entries_aud");
		query.executeUpdate();
		
		// records
		query = manager.createNativeQuery("DELETE FROM map_records_aud");
		query.executeUpdate();
		
		// leads
		query = manager.createNativeQuery("DELETE FROM map_projects_map_leads_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_leads_aud");
		query.executeUpdate();
		
		// specialists
		query = manager.createNativeQuery("DELETE FROM map_projects_map_specialists_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_specialists_aud");
		query.executeUpdate();
		
		// projects
		query = manager.createNativeQuery("DELETE FROM map_projects_aud");
		query.executeUpdate();
		
		tx.commit();

		System.out.println("Cleanup complete");

		manager.close();
		factory.close();
	}

	private static void fail(String string) {

		System.out.println("Failed: " + string);

	}

	*//**
	 * Create mock data and test adding the objects
	 * 
	 * @throws Exception
	 *//*
	@Test
	public void testAddElements() throws Exception {

		System.out.println("Testing element add...");

		// ASSUMPTION: Database is unloaded, starting fresh

		List<MapProject> projects = new ArrayList<>();
		List<MapSpecialist> specialists = new ArrayList<>();
		List<MapLead> leads = new ArrayList<>();

		// Add Specialists and Leads
		MapLeadJpa mapLead = new MapLeadJpa();
		mapLead.setName("Kathy Giannangelo");
		mapLead.setUserName("kgi");
		mapLead.setEmail("***REMOVED***");
		leads.add(mapLead);

		mapLead = new MapLeadJpa();
		mapLead.setName("Donna Morgan");
		mapLead.setUserName("dmo");
		mapLead.setEmail("***REMOVED***");
		leads.add(mapLead);

		mapLead = new MapLeadJpa();
		mapLead.setName("Julie O'Halloran");
		mapLead.setUserName("joh");
		mapLead.setEmail("***REMOVED***");
		leads.add(mapLead);

		MapSpecialistJpa mapSpecialist = new MapSpecialistJpa();
		mapSpecialist.setName("Krista Lilly");
		mapSpecialist.setUserName("kli");
		mapSpecialist.setEmail("***REMOVED***");
		specialists.add(mapSpecialist);

		mapSpecialist = new MapSpecialistJpa();
		mapSpecialist.setName("Nicola Ingram");
		mapSpecialist.setUserName("nin");
		mapSpecialist.setEmail("***REMOVED***");
		specialists.add(mapSpecialist);

		mapSpecialist = new MapSpecialistJpa();
		mapSpecialist.setName("Rory Davidson");
		mapSpecialist.setUserName("rda");
		mapSpecialist.setEmail("***REMOVED***");
		specialists.add(mapSpecialist);

		mapSpecialist = new MapSpecialistJpa();
		mapSpecialist.setName("Julie O'Halloran");
		mapSpecialist.setUserName("joh");
		mapSpecialist.setEmail("***REMOVED***");
		specialists.add(mapSpecialist);

		mapSpecialist = new MapSpecialistJpa();
		mapSpecialist.setName("Graeme Miller");
		mapSpecialist.setUserName("gmi");
		mapSpecialist.setEmail("***REMOVED***");
		specialists.add(mapSpecialist);

		for (MapSpecialist m : specialists) {
			Logger.getLogger(this.getClass()).info(
					"Adding map specialist " + m.getName());
			service.addMapSpecialist(m);
		}

		for (MapLead m : leads) {
			Logger.getLogger(this.getClass())
					.info("Adding map lead " + m.getName());
			service.addMapLead(m);
		}
		

		// Add map advice
		List<MapAdvice> mapAdvices = new ArrayList<>();

		String[] adviceValues =
				new String[] {
						"ADDITIONAL CODES MAY BE REQUIRED TO IDENTIFY PLACE OF OCCURRENCE",
						"Broad to narrow map from SNOMED CT source code to target code",
						"CONSIDER ADDITIONAL CODE TO IDENTIFY SPECIFIC CONDITION OR DISEASE",
						"CONSIDER LATERALITY SPECIFICATION",
						"CONSIDER STAGE OF GLAUCOMA SPECIFICATION",
						"CONSIDER TIME OF COMA SCALE SPECIFICATION",
						"CONSIDER TOPHUS SPECIFICATION",
						"CONSIDER TRIMESTER SPECIFICATION",
						"CONSIDER WHICH FETUS IS AFFECTED BY THE MATERNAL CONDITION",
						"DESCENDANTS NOT EXHAUSTIVELY MAPPED",
						"EPISODE OF CARE INFORMATION NEEDED",
						"Exact match map from SNOMED CT source code to target code",
						"FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE",
						"MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION",
						"MAP IS CONTEXT DEPENDENT FOR GENDER",
						"MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT",
						"MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
						"MAPPED FOLLOWING IHTSDO GUIDANCE",
						"MAPPED FOLLOWING WHO GUIDANCE",
						"MAPPED WITH IHTSDO GUIDANCE",
						"MAPPED WITH NCHS GUIDANCE",
						"MAPPING GUIDANCE FROM WHO IS AMBIGUOUS",
						"Narrow to broad map from SNOMED CT source code to target code",
						"NCHS ADVISES TO ASSUME CLOSED FRACTURE",
						"Partial overlap between SNOMED CT source code and target code",
						"POSSIBLE REQUIREMENT FOR ADDITIONAL CODE TO FULLY DESCRIBE DISEASE OR CONDITION",
						"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
						"POSSIBLE REQUIREMENT FOR CAUSATIVE AGENT CODE",
						"POSSIBLE REQUIREMENT FOR CAUSATIVE DISEASE CODE",
						"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
						"POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE",
						"SNOMED CT source code not mappable to target coding scheme",
						"SOURCE CONCEPT HAS BEEN RETIRED FROM MAP SCOPE",
						"SOURCE SNOMED CONCEPT IS AMBIGUOUS",
						"SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED",
						"THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION",
						"THIS IS A MANIFESTATION CODE FOR USE IN A SECONDARY POSITION",
						"THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION",
						"THIS IS AN INFECTIOUS AGENT CODE FOR USE IN A SECONDARY POSITION",
						"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)",
						"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29(Burns)"
				};

		String[] icd10AdviceValues =
				new String[] {
						"DESCENDANTS NOT EXHAUSTIVELY MAPPED",
						"FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE",
						"MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION",
						"MAP IS CONTEXT DEPENDENT FOR GENDER",
						"MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT",
						"MAPPED FOLLOWING IHTSDO GUIDANCE",
						"MAPPED FOLLOWING WHO GUIDANCE",
						"MAPPING GUIDANCE FROM WHO IS AMBIGUOUS",
						"MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
						"POSSIBLE REQUIREMENT FOR ADDITIONAL CODE TO FULLY DESCRIBE DISEASE OR CONDITION",
						"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
						"POSSIBLE REQUIREMENT FOR CAUSATIVE AGENT CODE",
						"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
						"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
						"POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE",
						"SOURCE CONCEPT HAS BEEN RETIRED FROM MAP SCOPE",
						"SOURCE SNOMED CONCEPT IS AMBIGUOUS",
						"SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED",
						"THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION",
						"THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION",
						"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29(Burns)"
				};
		String[] icd9cmAdviceValues =
				new String[] {
						"SNOMED CT source code not mappable to target coding scheme",
						"Exact match map from SNOMED CT source code to target code",
						"Narrow to broad map from SNOMED CT source code to target code",
						"Broad to narrow map from SNOMED CT source code to target code",
						"Partial overlap between SNOMED CT source code and target code"
				};

		String[] icpcAdviceValues =
				new String[] {
						"ADDITIONAL CODES MAY BE REQUIRED TO IDENTIFY PLACE OF OCCURRENCE",
						"CONSIDER ADDITIONAL CODE TO IDENTIFY SPECIFIC CONDITION OR DISEASE",
						"CONSIDER LATERALITY SPECIFICATION",
						"CONSIDER STAGE OF GLAUCOMA SPECIFICATION",
						"CONSIDER TIME OF COMA SCALE SPECIFICATION",
						"CONSIDER TOPHUS SPECIFICATION",
						"CONSIDER TRIMESTER SPECIFICATION",
						"CONSIDER WHICH FETUS IS AFFECTED BY THE MATERNAL CONDITION",
						"DESCENDANTS NOT EXHAUSTIVELY MAPPED",
						"EPISODE OF CARE INFORMATION NEEDED",
						"MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION",
						"MAP IS CONTEXT DEPENDENT FOR GENDER",
						"MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT",
						"MAPPED WITH IHTSDO GUIDANCE",
						"MAPPED WITH NCHS GUIDANCE",
						"MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
						"NCHS ADVISES TO ASSUME CLOSED FRACTURE",
						"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
						"POSSIBLE REQUIREMENT FOR CAUSATIVE DISEASE CODE",
						"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
						"SOURCE SNOMED CONCEPT IS AMBIGUOUS",
						"SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED",
						"THIS IS A MANIFESTATION CODE FOR USE IN A SECONDARY POSITION",
						"THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION",
						"THIS IS AN INFECTIOUS AGENT CODE FOR USE IN A SECONDARY POSITION",
						"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)"
				};

		for (String value : adviceValues) {
			MapAdvice advice = new MapAdviceJpa();
			advice.setName(value);
			advice.setDetail(value);
			mapAdvices.add(advice);
		}

		Map<String, MapAdvice> mapAdviceValueMap = new HashMap<>();
		for (MapAdvice m : mapAdvices) {
			Logger.getLogger(this.getClass()).info(
					"Adding map advice " + m.getName());
			
			mapAdviceValueMap.put(m.getName(), m);
		}

		// Add map projects
		Map<String, Long> refSetIdToMapProjectIdMap = new HashMap<String, Long>();
		MapProject mapProject = new MapProjectJpa();
		mapProject.setName("SNOMED to ICD10");
		mapProject.setRefSetId("447562003");
		mapProject.setSourceTerminology("SNOMEDCT");
		mapProject.setSourceTerminologyVersion("20140131");
		mapProject.setDestinationTerminology("ICD10");
		mapProject.setDestinationTerminologyVersion("2010");
		mapProject.setBlockStructure(false);
		mapProject.setGroupStructure(true);
		mapProject.setPublished(true);
		mapProject.addMapLead(leads.get(0));
		mapProject.addMapLead(leads.get(1));
		mapProject.addMapSpecialist(specialists.get(0));
		mapProject.addMapSpecialist(specialists.get(1));
		mapProject.addMapSpecialist(specialists.get(2));
		for (String s : icd10AdviceValues) {
			mapProject.addMapAdvice(mapAdviceValueMap.get(s));
		}
		Long mapProjectId = new Long("1");
		mapProject.setId(mapProjectId);
		refSetIdToMapProjectIdMap.put(mapProject.getRefSetId(), mapProjectId);
		projects.add(mapProject);

		mapProject = new MapProjectJpa();
		mapProject.setName("SNOMED to ICD9CM");
		mapProject.setRefSetId("5781347179");
		mapProject.setSourceTerminology("SNOMEDCT");
		mapProject.setSourceTerminologyVersion("20140131");
		mapProject.setDestinationTerminology("ICD9CM");
		mapProject.setDestinationTerminologyVersion("2013");
		mapProject.setBlockStructure(false);
		mapProject.setGroupStructure(true);
		mapProject.setPublished(true);
		mapProject.addMapLead(leads.get(0));
		mapProject.addMapLead(leads.get(1));
		mapProject.addMapSpecialist(specialists.get(0));
		mapProject.addMapSpecialist(specialists.get(1));
		for (String s : icd9cmAdviceValues) {
			mapProject.addMapAdvice(mapAdviceValueMap.get(s));
		}
		mapProjectId = new Long("2");
		mapProject.setId(mapProjectId);
		refSetIdToMapProjectIdMap.put(mapProject.getRefSetId(), mapProjectId);
		projects.add(mapProject);

		mapProject = new MapProjectJpa();
		mapProject.setName("SNOMED to ICPC - Family Practice/GPF Refset");
		mapProject.setRefSetId("5235669");
		mapProject.setSourceTerminology("SNOMEDCT");
		mapProject.setSourceTerminologyVersion("20130731");
		mapProject.setDestinationTerminology("ICPC");
		mapProject.setDestinationTerminologyVersion("2");
		mapProject.setBlockStructure(false);
		mapProject.setGroupStructure(false);
		mapProject.setPublished(false);
		mapProject.addMapLead(leads.get(2));
		mapProject.addMapSpecialist(specialists.get(3));
		for (String s : icpcAdviceValues) {
			mapProject.addMapAdvice(mapAdviceValueMap.get(s));
		}
		mapProjectId = new Long("3");
		mapProject.setId(mapProjectId);
		refSetIdToMapProjectIdMap.put(mapProject.getRefSetId(), mapProjectId);
		projects.add(mapProject);

		
		for (MapProject m : projects) {
			System.out.println("Adding map project " + m.getName());
			service.addMapProject(m);
		}
	}

	*//**
	 * Test retrieval of elements from existing database
	 * 
	 * @throws Exception
	 *//*
	@SuppressWarnings("static-method")
	@Test
	public void testRetrieveElements() throws Exception {
		System.out.println("Testing element retrieval...");

		// Test retrieval of all elements
		
		// get all objects
		List<MapProject> projects = service.getMapProjects();
		List<MapSpecialist> specialists = service.getMapSpecialists();
		List<MapLead> leads = service.getMapLeads();
		List<MapRecord> records = service.getMapRecords();

		// test projects 
		// 3 expected, see testAddElements() for project details
		
		if (projects.size() != 3) {
			fail("Retrieval - getMapProjects():  Found " + Integer.toString(projects.size()) + " projects, expected 3");
		}
		if (projects.get(1).getName().compareTo("SNOMED to ICD9CM") != 0 ) {
			fail("Retrieval - getMapProjects():  Project name for project 2 not equal to 'SNOMED to ICD9CM'");
		}
		if (projects.get(2).getMapLeads().size() != 2) {
			fail("Retrieval - getMapProjects():  Number of project leads for project 3 is not equal to 2");
		}
		
		// test leads
		if (leads.size() != 3) {
			fail("Retrieval - getMapLeads():  Found " + Integer.toString(leads.size()) + " leads, expected 3");
		}
		
		if (leads.get(0).getUserName().compareTo("kgi") != 0) {
			fail("Retrieval - getMapLeads():  Map lead username for lead 1 not equal to 'kgi'");
		}
		
		// test specialists
		if (specialists.size() != 5) {
			fail("Retrieval - getMapSpecialists():  Found " + Integer.toString(specialists.size()) + " specialists, expected 5");
		}
		
		if (specialists.get(4).getEmail().compareTo("***REMOVED***") != 0) {
			fail("Retrieval - getMapSpecialists():  Map specialist email for lead 5 not equal to '***REMOVED***'");
		}
		
		// TODO test records
		
		// get project (id)
		MapProject p = service.getMapProject(new Long(2));
		
		if (p.getRefSetId().compareTo("5781347179") != 0) {
			fail("Retrieval - getMapProject(Long id): refSetId invalid");
		}
		
		if (p.getSourceTerminology().compareTo("SNOMEDCT") != 0) {
			fail("Retrieval - getMapProject(Long id): source terminology invalid");
		}
		
		// get specialist (id)
		MapSpecialist s = service.getMapSpecialist(new Long(1));
				
		if (s.getEmail().compareTo("***REMOVED***") != 0) {
			fail("Retrieval - getMapSpecialist(Long id):  Map specialist email for lead 5 not equal to '***REMOVED***'");
		}
		
		// get lead (id)
		MapLead l = service.getMapLead(new Long(1));
		
		if (l.getUserName().compareTo("kgi") != 0) {
			fail("Retrieval - getMapLead(Long id):  Map lead username for lead 1 not equal to 'kgi'");
		}
		
		
		// TODO: get record (id)
		
		// QUERY TESTS
		SearchResultList results = new SearchResultListJpa();
		
		// find project (query)
		results = service.findMapProjects("ICD9CM", new PfsParameterJpa());
		if (results.getSearchResults().get(0).getValue().compareTo("SNOMED to ICD9CM") != 0) {
			fail("Retrieval - findMapProjects(): Could not search by name or terminology");
		}
		
		results = service.findMapProjects("Kathy", new PfsParameterJpa());
		if (results.getCount() != 2) {
			fail("Retrieval - findMapProjects(String query):  Could not search by lead name");
		}
		
		// find specialist (query)
		results = service.findMapSpecialists("rda", new PfsParameterJpa());
		if (results.getSearchResults().get(0).getValue().compareTo("Rory Davidson") != 0) {
			fail("Retrieval - findMapSpecialist(String query): Could not search by username");
		}
		
		// find lead (query)
		results = service.findMapLeads("kgi", new PfsParameterJpa());
		if (results.getSearchResults().get(0).getValue().compareTo("Kathy Giannangelo") != 0) {
			fail("Retrieval - findMapLeads(String query): Could not search by username");
		}
		
		// test project retrieval by user (lead/specialist)		
		s = service.getMapSpecialist(new Long(1));
		if (service.getMapProjectsForMapSpecialist(s).size() != 2) {
			fail("Retrieval - findMapProjectsForMapSpecialist(MapSpecialist mapSpecialist): Failed to retrieve projects");
		}
		
		l = service.getMapLead(new Long(2));
		if (service.getMapProjectsForMapLead(l).size() != 1) {
			fail("Retrieval - findMapProjectsForMapLead(MapLead mapLead):  Failed to retrieve projects");
		}
		
		// TODO find record, advice (query)
		

	}

	*//**
	 * Test updating each type of element
	 * 
	 * @throws Exception
	 *//*
	@SuppressWarnings("static-method")
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
		// List<Number> revNumbers = reader.getRevisions(MapProject.class,
		// project_old); // TODO Reenable for audit testing

		project_old.setDestinationTerminology(changedValue);
		service.updateMapProject(project_old);

		project_new = service.getMapProject(project_old.getId());

		if (!project_new.getDestinationTerminology().equals(changedValue)) {
			fail("Failed to update project");
		} else {
			System.out.println("Project update successful");
		}
		// TODO Reenable for audit testing
		
		 * if (reader.getRevisions(MapProject.class, project_old).size() !=
		 * revNumbers.size() + 1) { fail(
		 * "Failed to update revision table:  number of revisions has not increased by 1"
		 * ); }
		 

		// test update and envers audit of map specialist
		MapSpecialist specialist_old = specialists.get(0);
		MapSpecialist specialist_new;
		// revNumbers = reader.getRevisions(MapSpecialist.class,
		// specialist_old); // TODO Reenable for audit testing

		specialist_old.setEmail(changedValue);
		service.updateMapSpecialist(specialist_old);

		specialist_new = service.getMapSpecialist(specialist_old.getId());

		if (!specialist_new.getEmail().equals(changedValue)) {
			fail("Failed to update specialist");
		} else {
			System.out.println("Specialist update successful");
		}
		// TODO Reenable for audit testing
		
		 * if (reader.getRevisions(MapSpecialist.class, specialist_old).size()
		 * != revNumbers.size() + 1) { fail(
		 * "Failed to update revision table:  number of revisions has not increased by 1"
		 * ); }
		 

		// test update and envers audit of map lead

		MapLead lead_old = leads.get(0);
		MapLead lead_new;
		// revNumbers = reader.getRevisions(MapLead.class, lead_old); // TODO
		// Reenable for audit testing

		lead_old.setEmail(changedValue);
		service.updateMapLead(lead_old);

		lead_new = service.getMapLead(lead_old.getId());

		if (!lead_new.getEmail().equals(changedValue)) {
			fail("Failed to update lead");
		} else {
			System.out.println("Lead update successful");
		}

		// TODO Reenable for audit testing
		
		 * if (reader.getRevisions(MapLead.class, lead_old).size() !=
		 * revNumbers.size() + 1) { fail(
		 * "Failed to update revision table:  number of revisions has not increased by 1"
		 * ); }
		 

		// TODO Determine desired audit results for project

	}

	*//**
	 * Test removal of each type of element Further test propagation of removal
	 * of lead/specialist on project
	 * 
	 * @throws Exception
	 *//*
	@SuppressWarnings("static-method")
	@Test
	public void testRemoveElements() throws Exception {
		System.out.println("Testing element remove...");

		// test delete and envers audit of map project
		List<MapProject> projects = service.getMapProjects();
		List<MapSpecialist> specialists = service.getMapSpecialists();
		List<MapLead> leads = service.getMapLeads();

		MapProject project_removed = projects.get(0);
		MapSpecialist specialist_removed = specialists.get(0);
		MapLead lead_removed = leads.get(0);

		// test delete of lead
		System.out.println("Testing lead removal...");
		service.removeMapLead(lead_removed.getId());

		// test delete
		if (service.getMapLead(lead_removed.getId()) != null) {
			fail("Remove lead reported success, but lead still present in database!");
		}

		// test delete of specialist
		System.out.println("Testing specialist removal...");
		service.removeMapSpecialist(specialist_removed.getId());

		if (service.getMapSpecialist(specialist_removed.getId()) != null) {
			fail("Remove specialist reported success, but specialist still present in database!");
		}

		// test delete of project
		System.out.println("Testing project removal...");
		service.removeMapProject(project_removed.getId());

		if (service.getMapProject(project_removed.getId()) != null) {
			fail("Remove project reported success, but project still present in database!");
		}

	}

}
*/