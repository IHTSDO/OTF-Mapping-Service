package org.ihtsdo.otf.mapping.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The Class MapProjectJpaTest.
 * 
 * Provides test cases 1. confirm MapProject data load returns expected data 2.
 * confirms indexed fields are indexed 3. confirms MapProject is audited and
 * changes are logged in audit table
 */
public class MapProjectJpaTest {

	/** The manager. */
	private static EntityManager manager;

	/** The factory. */
	private static EntityManagerFactory factory;

	/** The full text entity manager. */
	private static FullTextEntityManager fullTextEntityManager;

	/** The audit history reader. */
	private static AuditReader reader;

	/** The mapping to ICD10CM. */
	private static MapProjectJpa mapProject1;

	/** The mapping to ICD9CM. */
	private static MapProjectJpa mapProject3;

	/** The test ref set id. */
	private static Long testRefSetId = new Long("123456789");

	/** The test ref set id for the mapping to ICD9CM. */
	private static Long testRefSetId3 = new Long("345678912");

	/** The test name for the mapping to ICD10CM. */
	private static String testName = "SNOMEDCT to ICD10CM Mapping";

	/** The updated test name. */
	private static String testName2 = "Updated SNOMEDCT to ICD10CM Mapping";

	/** The test name for the mapping to ICD9CM. */
	private static String testName3 = "SNOMEDCT to ICD9CM Mapping";

	/** The test source terminology. */
	private static String testSourceTerminology = "SNOMEDCT";

	/** The test source terminology for the mapping to ICD9CM. */
	private static String testSourceTerminology3 = "SNOMEDCT9";

	/** The test source terminology version. */
	private static String testSourceTerminologyVersion = "20130731";

	/** The test source terminology version for the mapping to ICD9CM. */
	private static String testSourceTerminologyVersion3 = "20130131";

	/** The test destination terminology. */
	private static String testDestinationTerminology = "ICD10CM";

	/** The test destination terminology for the mapping to ICD9CM. */
	private static String testDestinationTerminology3 = "ICD9CM";

	/** The test destination terminology version. */
	private static String testDestinationTerminologyVersion = "2010";

	/** The test destination terminology version for the mapping to ICD9CM. */
	private static String testDestinationTerminologyVersion3 = "2009";

	/** The test block structure. */
	private static boolean testBlockStructure = true;

	/** The test group structure. */
	private static boolean testGroupStructure = false;

	/** The test published. */
	private static boolean testPublished = false;

	/**
	 * Creates db tables, load test objects and create indexes to prepare for
	 * test cases.
	 * 
	 * @throws Exception
	 *             if anything goes wrong
	 */
	@BeforeClass
	public static void init() throws Exception {

		// create Entitymanager
		factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		manager = factory.createEntityManager();
		fullTextEntityManager = Search.getFullTextEntityManager(manager);

		fullTextEntityManager.purgeAll(MapProjectJpa.class);
		fullTextEntityManager.flushToIndexes();

		// load test objects
		EntityTransaction tx = manager.getTransaction();

		tx.begin();
		loadMapProjects();
		tx.commit();

		// create indexes
		/**
		 * try { FullTextEntityManager fullTextEntityManager =
		 * Search.getFullTextEntityManager(manager);
		 * fullTextEntityManager.purgeAll(ConceptJpa.class);
		 * fullTextEntityManager.flushToIndexes();
		 * fullTextEntityManager.createIndexer(ConceptJpa.class)
		 * .batchSizeToLoadObjects(25).cacheMode(CacheMode.NORMAL)
		 * .threadsToLoadObjects(5).threadsForSubsequentFetching(20)
		 * .startAndWait(); } catch (Throwable e) { e.printStackTrace();
		 * fail("Indexing failed."); }
		 */

		// create audit reader for history records
		reader = AuditReaderFactory.get(manager);
	}

	/**
	 * Test map project load.
	 */
	@Test
	public void testMapProjectLoad() {

		EntityTransaction tx = manager.getTransaction();
		System.out.println("testMapProjectLoad()...");

		tx.begin();
		confirmLoad();
		tx.commit();

	}

	/**
	 * Load map projects.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private static void loadMapProjects() throws Exception {

		// create initial map project mapping to ICD10CM
		mapProject1 = new MapProjectJpa();

		mapProject1.setName(testName);
		mapProject1.setRefSetId(testRefSetId);
		mapProject1.setSourceTerminology(testSourceTerminology);
		mapProject1.setSourceTerminologyVersion(testSourceTerminologyVersion);
		mapProject1.setDestinationTerminology(testDestinationTerminology);
		mapProject1
				.setDestinationTerminologyVersion(testDestinationTerminologyVersion);
		mapProject1.setBlockStructure(testBlockStructure);
		mapProject1.setGroupStructure(testGroupStructure);
		mapProject1.setPublished(testPublished);
		manager.persist(mapProject1);

		// create secondary map project mapping to ICD9CM
		mapProject3 = new MapProjectJpa();

		mapProject3.setName(testName3);
		mapProject3.setRefSetId(testRefSetId3);
		mapProject3.setSourceTerminology(testSourceTerminology3);
		mapProject3.setSourceTerminologyVersion(testSourceTerminologyVersion3);
		mapProject3.setDestinationTerminology(testDestinationTerminology3);
		mapProject3
				.setDestinationTerminologyVersion(testDestinationTerminologyVersion3);
		mapProject3.setBlockStructure(testBlockStructure);
		mapProject3.setGroupStructure(testGroupStructure);
		mapProject3.setPublished(testPublished);
		manager.persist(mapProject3);

		MapLeadJpa mapLeadBrian = new MapLeadJpa();
		mapLeadBrian.setName("Brian");
		mapLeadBrian.setUserName("bcarlsen");
		mapLeadBrian.setEmail("***REMOVED***");
		manager.persist(mapLeadBrian);
		mapProject1.addMapLead(mapLeadBrian);

		MapLeadJpa mapLeadRory = new MapLeadJpa();
		mapLeadRory.setName("Rory");
		mapLeadRory.setUserName("rda");
		mapLeadRory.setEmail("***REMOVED***");
		manager.persist(mapLeadRory);
		mapProject1.addMapLead(mapLeadRory);

		MapSpecialistJpa mapSpecialistDeborah = new MapSpecialistJpa();
		mapSpecialistDeborah.setName("Deborah");
		mapSpecialistDeborah.setUserName("dshapiro");
		mapSpecialistDeborah.setEmail("***REMOVED***");
		manager.persist(mapSpecialistDeborah);
		mapProject1.addMapSpecialist(mapSpecialistDeborah);

		// test adding same map lead to multiple projects
		mapProject3.addMapLead(mapLeadBrian);

	}

	/**
	 * Confirm load.
	 */
	private void confirmLoad() {
		javax.persistence.Query query = manager
				.createQuery("select m from MapProjectJpa m where refSetId = :refSetId");

		// Try to retrieve the single expected result
		// If zero or more than one result are returned, log error and set
		// result to
		// null
		query.setParameter("refSetId", testRefSetId);

		MapProject mapProject = (MapProject) query.getSingleResult();
		assertEquals(mapProject.getRefSetId(), testRefSetId);

	}

	/**
	 * Test map project indexes.
	 * 
	 * @throws ParseException
	 *             if lucene fails to parse query
	 */
	@Test
	public void testMapProjectIndex() throws ParseException {

		System.out.println("testMapProjectIndex()...");

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();

		QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
				searchFactory.getAnalyzer(MapProjectJpa.class));

		// test index on refSetId
		Query luceneQuery = queryParser.parse("refSetId:" + testRefSetId);
		FullTextQuery fullTextQuery = fullTextEntityManager
				.createFullTextQuery(luceneQuery);
		List<MapProject> results = fullTextQuery.getResultList();
		for (MapProject mapProject : results) {
			assertEquals(mapProject.getName(), testName);
		}
		assertTrue("results.size() " + results.size(), results.size() > 0);

		// test index on name
		luceneQuery = queryParser.parse("name:\"" + testName3 + "\"");
		fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);
		results = fullTextQuery.getResultList();
		for (MapProject mapProject : results) {
			assertEquals(mapProject.getRefSetId(), testRefSetId3);
		}
		assertTrue("results.size() " + results.size(), results.size() > 0);

		// test index on source terminology version
		luceneQuery = queryParser.parse("sourceTerminologyVersion:"
				+ testSourceTerminologyVersion);
		fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);
		results = fullTextQuery.getResultList();
		for (MapProject mapProject : results) {
			assertEquals(mapProject.getRefSetId(), testRefSetId);
		}
		assertTrue("results.size() " + results.size(), results.size() > 0);

		// test index on destination terminology version
		luceneQuery = queryParser.parse("destinationTerminologyVersion:"
				+ testDestinationTerminologyVersion);
		fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);
		results = fullTextQuery.getResultList();
		for (MapProject mapProject : results) {
			assertEquals(mapProject.getRefSetId(), testRefSetId);
		}
		assertTrue("results.size() " + results.size(), results.size() > 0);

		// test index on destination terminology
		luceneQuery = queryParser.parse("destinationTerminology:"
				+ testDestinationTerminology);
		fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);
		results = fullTextQuery.getResultList();
		for (MapProject mapProject : results) {
			assertEquals(mapProject.getRefSetId(), testRefSetId);
		}
		assertTrue("results.size() " + results.size(), results.size() > 0);

		// test index on source terminology
		luceneQuery = queryParser.parse("sourceTerminology:"
				+ testSourceTerminology);
		fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);
		results = fullTextQuery.getResultList();
		for (MapProject mapProject : results) {
			assertEquals(mapProject.getSourceTerminologyVersion(),
					testSourceTerminologyVersion);
		}
		assertTrue("results.size() " + results.size(), results.size() > 0);

	}

	/**
	 * Test map project audit reader history.
	 */
	@Test
	public void testMapProjectAuditReader() {

		System.out.println("testMapProjectAuditReader()...");

		// report initial number of revisions on MapProject object
		List<Number> revNumbers = reader.getRevisions(MapProjectJpa.class, 1L);
		assertTrue(revNumbers.size() == 1);
		System.out.println("MapProject: " + 1L + " - Versions: "
				+ revNumbers.toString());

		// make a change to MapProject
		EntityTransaction tx = manager.getTransaction();
		MapSpecialistJpa mapSpecialistPatrick = new MapSpecialistJpa();
		tx.begin();
		mapSpecialistPatrick.setName("Patrick");
		mapSpecialistPatrick.setUserName("pgranvold");
		mapSpecialistPatrick.setEmail("***REMOVED***");
		manager.persist(mapSpecialistPatrick);
		mapProject1.setName(testName2);
		manager.persist(mapProject1);
		mapProject1.addMapSpecialist(mapSpecialistPatrick);
		tx.commit();

		// report incremented number of revisions on MapProject object
		revNumbers = reader.getRevisions(MapProjectJpa.class, 1L);
		assertTrue(revNumbers.size() == 2);
		System.out.println("MapProject: " + 1L + " - Versions: "
				+ revNumbers.toString());

		// revert change to MapProject
		tx = manager.getTransaction();
		tx.begin();
		mapProject1.setName(testName);
		mapProject1.removeMapSpecialist(mapSpecialistPatrick);
		manager.persist(mapProject1);
		tx.commit();

	}

	/**
	 * Clean up.
	 */
	@AfterClass
	public static void cleanUp() {
		EntityTransaction tx = manager.getTransaction();

		// truncate tables
		tx.begin();
		javax.persistence.Query query = manager
				.createNativeQuery("DELETE FROM map_projects_map_advices");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_advices");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_projects_map_leads");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_leads");
		query.executeUpdate();
		query = manager
				.createNativeQuery("DELETE FROM map_projects_map_specialists");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_specialists");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_projects");
		query.executeUpdate();
		query = manager
				.createNativeQuery("DELETE FROM map_projects_map_advices_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_advices_aud");
		query.executeUpdate();
		query = manager
				.createNativeQuery("DELETE FROM map_projects_map_leads_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_leads_aud");
		query.executeUpdate();
		query = manager
				.createNativeQuery("DELETE FROM map_projects_map_specialists_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_specialists_aud");
		query.executeUpdate();
		query = manager.createNativeQuery("DELETE FROM map_projects_aud");
		query.executeUpdate();
		tx.commit();

		manager.close();
		factory.close();
	}

}
