package org.ihtsdo.otf.mapping.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
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
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The Class MapRecordJpaTest.
 * 
 * Provides test cases 1. confirm MapRecord data load returns expected data 2.
 * confirms indexed fields are indexed 3. confirms MapRecord is audited and
 * changes are logged in audit table
 */
public class MapRecordJpaTest {

	/** The manager. */
	private static EntityManager manager;

	/** The factory. */
	private static EntityManagerFactory factory;

	/** The full text entity manager. */
	private static FullTextEntityManager fullTextEntityManager;

	/** The audit history reader. */
	private static AuditReader reader;

	/** The first map record for testing. */
	private static MapRecordJpa mapRecord1;
	
	private static String conceptId1 = "105480006";
	
	private static MapSpecialistJpa mapSpecialist1;


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


		List<MapProject> projects = new ArrayList<>();
		List<MapSpecialist> specialists = new ArrayList<>();
		List<MapLead> leads = new ArrayList<>();

		// Add Specialists and Leads
		MapLeadJpa mapLead = new MapLeadJpa();
		mapLead.setName("Kathy Giannangelo");
		mapLead.setUserName("kgi");
		mapLead.setEmail("kgi@ihtsdo.org");
		leads.add(mapLead);

		MapSpecialistJpa mapSpecialist = new MapSpecialistJpa();
		mapSpecialist.setName("Krista Lilly");
		mapSpecialist.setUserName("kli");
		mapSpecialist.setEmail("kli@ihtsdo.org");
		specialists.add(mapSpecialist);

		tx.begin();
		for (MapSpecialist m : specialists) {
			Logger.getLogger(MapRecordJpaTest.class).info(
					"Adding map specialist " + m.getName());
			manager.persist(m);
		}

		for (MapLead m : leads) {
			Logger.getLogger(MapRecordJpaTest.class)
					.info("Adding map lead " + m.getName());
			manager.persist(m);
		}
		tx.commit();

		MapProject mapProject = new MapProjectJpa();
		mapProject.setName("SNOMED to ICD10");
		mapProject.setRefSetId(new Long("447562003"));
		mapProject.setSourceTerminology("SNOMEDCT");
		mapProject.setSourceTerminologyVersion("20140131");
		mapProject.setDestinationTerminology("ICD10");
		mapProject.setDestinationTerminologyVersion("2010");
		mapProject.setBlockStructure(false);
		mapProject.setGroupStructure(true);
		mapProject.setPublished(true);
		mapProject.addMapLead(leads.get(0));
		mapProject.addMapSpecialist(specialists.get(0));
		
		Long mapProjectId = new Long("1");
		mapProject.setId(mapProjectId);
		projects.add(mapProject);
		
		tx.begin();
		for (MapProject m : projects) {	
			Logger.getLogger(MapRecordJpa.class).info(
					"Adding map project " + m.getName());
			manager.merge(m);
		}
		tx.commit();
		
		tx.begin();
		loadMapRecords();
		tx.commit();

		// create audit reader for history records
		reader = AuditReaderFactory.get(manager);
		
	}

	/**
	 * Test map record load.
	 */
	@Test
	public void testMapRecordLoad() {

		EntityTransaction tx = manager.getTransaction();
		Logger.getLogger(MapRecordJpaTest.class)
		 .info("testMapRecordLoad()...");

		tx.begin();
		confirmLoad();
		tx.commit();

	}

	/**
	 * Load map records.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private static void loadMapRecords() throws Exception {
		mapRecord1 = new MapRecordJpa();
		mapRecord1.setConceptId(conceptId1);
		manager.persist(mapRecord1);
		
		MapEntry mapEntry = new MapEntryJpa();
		mapEntry.setTarget("Z53.2");
		mapEntry.setMapRecord(mapRecord1);
		mapEntry.setRelationId("447561005");
		mapEntry.setRule("RULE");
		MapAdvice advice = new MapAdviceJpa();
		advice.setName("ALWAYS Z53.2");
		advice.setDetail("ALWAYS Z53.2");
		mapEntry.addMapAdvice(advice);
				
		Set<MapPrinciple> mapPrinciples = new HashSet<MapPrinciple>();
		MapPrinciple mapPrinciple = new MapPrincipleJpa();
		mapPrinciple.setDetail("testMapPrincipleDescription");
		mapPrinciple.setName("testMapPrincipleName");
		mapPrinciple.setSectionRef("testMapPrincipleSectionRef");
		mapPrinciples.add(mapPrinciple);
		
		Set<MapNote> mapNotes = new HashSet<MapNote>();
		MapNote mapNote = new MapNoteJpa();
		mapNote.setNote("testMapNote1");
		mapNote.setTimestamp(new Date(java.lang.System.currentTimeMillis()));
		mapNote.setUser(mapSpecialist1);
		mapNotes.add(mapNote);
		
		// create initial map record
		mapRecord1.addMapEntry(mapEntry);
		mapRecord1.setMapPrinciples(mapPrinciples);
		mapRecord1.setMapProjectId(new Long("1"));
		mapRecord1.setMapNotes(mapNotes);
		manager.merge(mapRecord1);
	}

	/**
	 * Confirm load.
	 */
	@SuppressWarnings("static-method")
	private void confirmLoad() {
		javax.persistence.Query query = manager
				.createQuery("select m from MapRecordJpa m where conceptId = :conceptId");

		// Try to retrieve the single expected result
		// If zero or more than one result are returned, log error and set
		// result to
		// null
		query.setParameter("conceptId", conceptId1);

		MapRecord mapRecord = (MapRecord) query.getSingleResult();
		assertEquals(mapRecord.getConceptId(), conceptId1);
		assertEquals(mapRecord.getMapProjectId(), new Long("1"));
		assertEquals(mapRecord.getMapEntries().size(), 1);
		assertEquals(mapRecord.getConceptId(), conceptId1);
		
	}

	/**
	 * Test map project indexes.
	 * 
	 * @throws ParseException
	 *             if lucene fails to parse query
	 */
	@SuppressWarnings({
			"static-method", "unchecked"
	})
	@Test
	public void testMapRecordIndex() throws ParseException {

		Logger.getLogger(MapRecordJpaTest.class)
		  .info("testMapRecordIndex()...");

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();

		QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
				searchFactory.getAnalyzer(MapProjectJpa.class));

		// test index on refSetId
		Query luceneQuery = queryParser.parse("conceptId:" + conceptId1);
		FullTextQuery fullTextQuery = fullTextEntityManager
				.createFullTextQuery(luceneQuery);
		List<MapRecord> results = fullTextQuery.getResultList();
		for (MapRecord mapRecord : results) {
			assertEquals(mapRecord.getMapProjectId(), new Long("1"));
		}
		assertTrue("results.size() " + results.size(), results.size() > 0);

	}

	/**
	 * Test map project audit reader history.
	 */
	@SuppressWarnings("static-method")
	@Test
	public void testMapRecordAuditReader() {

		Logger.getLogger(MapRecordJpaTest.class)
		 .info("testMapRecordAuditReader()...");

		// report initial number of revisions on MapRecord object
		List<Number> revNumbers = reader.getRevisions(MapRecordJpa.class, 1L);
		assertTrue(revNumbers.size() == 1);
		Logger.getLogger(MapRecordJpaTest.class)
		 .info("MapRecord: " + 1L + " - Versions: "
				+ revNumbers.toString());

		// make a change to MapRecord
		EntityTransaction tx = manager.getTransaction();
		MapNote mapNoteAddTest = new MapNoteJpa();
		tx.begin();
		mapNoteAddTest.setNote("MapNoteAddTest");
		mapNoteAddTest.setTimestamp(new Date(java.lang.System.currentTimeMillis()));
		mapNoteAddTest.setUser(mapSpecialist1);
		mapRecord1.setConceptId("1111111");
		manager.persist(mapRecord1);
		mapRecord1.addMapNote(mapNoteAddTest);
		tx.commit();

		// report incremented number of revisions on MapProject object
		revNumbers = reader.getRevisions(MapRecordJpa.class, 1L);
		Logger.getLogger(MapRecordJpaTest.class)
		 .info("MapRecord: " + 1L + " - Versions: "
				+ revNumbers.toString());
		assertTrue(revNumbers.size() == 2);

		// revert change to MapProject
		tx = manager.getTransaction();
		tx.begin();
		mapRecord1.setConceptId(conceptId1);
		mapRecord1.removeMapNote(mapNoteAddTest);
		manager.persist(mapRecord1);
		tx.commit();

	}

	/**
	 * Clean up.
	 */
	@AfterClass
	public static void cleanUp() {

		manager.close();
		factory.close();
	}

}
