package org.ihtsdo.otf.mapping.test.other;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A JUnit test class for {@link MapRecord}.
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

  /** The concept id1. */
  private static String conceptId1 = "105480006";

  /** The map user1. */
  private static MapUserJpa mapUser1;

  /**
   * Creates db tables, load test objects and create indexes to prepare for test
   * cases.
   * 
   * @throws Exception if anything goes wrong
   */
  @BeforeClass
  public static void init() throws Exception {

    // create Entity Manager
    String configFileName = System.getProperty("run.config.test");
    Logger.getLogger(MapRecordJpaTest.class).info(
        "  run.config.test = " + configFileName);
    Properties config = new Properties();
    FileReader in = new FileReader(new File(configFileName));
    config.load(in);
    in.close();
    Logger.getLogger(MapRecordJpaTest.class).info("  properties = " + config);
    factory =
        Persistence.createEntityManagerFactory("MappingServiceDS", config);
    manager = factory.createEntityManager();
    fullTextEntityManager = Search.getFullTextEntityManager(manager);

    fullTextEntityManager.purgeAll(MapRecordJpa.class);
    fullTextEntityManager.flushToIndexes();

    // create audit reader for history records
    reader = AuditReaderFactory.get(manager);

  }

  /**
   * Test map record indexes.
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings({
      "static-method", "unchecked"
  })
  @Test
  public void testMapRecordIndex() throws Exception {

    Logger.getLogger(MapRecordJpaTest.class).info("testMapRecordIndex()...");

    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();

    QueryParser queryParser =
        new QueryParser(Version.LUCENE_36, "summary",
            searchFactory.getAnalyzer(MapProjectJpa.class));

    // test index on refSetId
    Query luceneQuery = queryParser.parse("conceptId:" + conceptId1);
    FullTextQuery fullTextQuery =
        fullTextEntityManager.createFullTextQuery(luceneQuery);
    List<MapRecord> results = fullTextQuery.getResultList();
    for (MapRecord mapRecord : results) {
      assertEquals(mapRecord.getMapProjectId(), new Long("1"));
    }
    assertTrue("results.size() " + results.size(), results.size() > 0);

  }

  /**
   * Test map project audit reader history.
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void testMapRecordAuditReader() throws Exception {

    Logger.getLogger(MapRecordJpaTest.class).info(
        "testMapRecordAuditReader()...");

    // report initial number of revisions on MapRecord object
    List<Number> revNumbers =
        reader.getRevisions(MapRecordJpa.class, mapRecord1.getId());
    Logger.getLogger(MapRecordJpaTest.class).info(
        "MapRecord: " + 1L + " - Versions: " + revNumbers.toString());
    assertTrue(revNumbers.size() == 1);

    // make a change to MapRecord
    EntityTransaction tx = manager.getTransaction();
    MapNote mapNoteAddTest = new MapNoteJpa();
    tx.begin();
    mapNoteAddTest.setNote("MapNoteAddTest");
    mapNoteAddTest.setTimestamp(new Date(java.lang.System.currentTimeMillis()));
    mapNoteAddTest.setUser(mapUser1);
    mapRecord1.setConceptId("1111111");
    manager.persist(mapRecord1);
    mapRecord1.addMapNote(mapNoteAddTest);
    tx.commit();

    // report incremented number of revisions on MapProject object
    revNumbers = reader.getRevisions(MapRecordJpa.class, mapRecord1.getId());
    Logger.getLogger(MapRecordJpaTest.class).info(
        "MapRecord: " + 1L + " - Versions: " + revNumbers.toString());
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
   * Test map record delete functions for both record and mapped relationships.
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void confirmMapRecordDelete() throws Exception {

    Logger.getLogger(MapRecordJpaTest.class).info(
        "Testing MapRecord delete functions...");

    EntityTransaction tx = manager.getTransaction();
    Logger.getLogger(MapRecordJpaTest.class).info("testMapRecordDelete()...");

    MapRecord mapRecord =
        (MapRecord) manager.createQuery(
            "select m from MapRecordJpa m where conceptId = " + conceptId1)
            .getSingleResult();

    // retrieve id of principle, entry, note
    Long recordId = mapRecord.getId();
    Long principleId = mapRecord.getMapPrinciples().iterator().next().getId();
    Long noteId = mapRecord.getMapEntries().iterator().next().getId();

    MapEntry entry = mapRecord.getMapEntries().iterator().next();
    Long entryId = entry.getId();
    Long entryAdviceId = entry.getMapAdvices().iterator().next().getId();
    // Long entryPrincipleId =
    // entry.getMapPrinciples().iterator().next().getId();

    // delete the map record
    tx.begin();
    if (manager.contains(mapRecord)) {
      manager.remove(mapRecord);
    } else {
      manager.remove(manager.merge(mapRecord));
    }

    tx.commit();

    // test removal of record
    assertTrue(manager.find(MapRecordJpa.class, recordId) == null);

    // test existence of principle (should not have been deleted)
    assertTrue(manager.find(MapPrincipleJpa.class, principleId) != null);

    // test existence of entry (should have been deleted)
    assertTrue(manager.find(MapEntryJpa.class, entryId) == null);

    // test existence of note (should have been deleted)
    assertTrue(manager.find(MapNoteJpa.class, noteId) == null);

    // test existence of entry principle (should not have been deleted)
    // assertTrue(manager.find(MapPrincipleJpa.class, entryPrincipleId) !=
    // null);

    // test existence of entry advice (should not have been deleted)
    assertTrue(manager.find(MapAdviceJpa.class, entryAdviceId) != null);

  }

  /**
   * Tests cascading delete settings from Entry to Note, Principle, Advice.
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void confirmMapEntryDelete() throws Exception {

    Logger.getLogger(MapRecordJpaTest.class).info(
        "Testing MapEntry delete functions...");

    EntityTransaction tx = manager.getTransaction();

    MapEntry mapEntry =
        (MapEntry) manager.createQuery("select m from MapEntryJpa m")
            .getSingleResult();

    // retrieve id of advice, entry, note

    Long entryId = mapEntry.getId();
    Long entryAdviceId = mapEntry.getMapAdvices().iterator().next().getId();
    Logger.getLogger(MapRecordJpaTest.class).info("entryId " + entryId);

    // delete the map entry
    tx.begin();
    if (manager.contains(mapEntry)) {
      manager.remove(mapEntry);
    } else {
      manager.remove(manager.merge(mapEntry));
    }

    tx.commit();

    // test existence of entry (should have been deleted)
    assertTrue(manager.find(MapEntryJpa.class, entryId) == null);

    // test existence of entry advice (should not have been deleted)
    assertTrue(manager.find(MapAdviceJpa.class, entryAdviceId) != null);

  }

  /**
   * Confirms map record load.
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void confirmMapRecordLoad() throws Exception {

    Logger.getLogger(MapRecordJpaTest.class).info("Testing MapRecord load...");

    // test load of record
    javax.persistence.Query query =
        manager
            .createQuery("select m from MapRecordJpa m where conceptId = :conceptId");

    query.setParameter("conceptId", conceptId1);

    MapRecord mapRecord = (MapRecord) query.getSingleResult();
    assertEquals(mapRecord.getConceptId(), conceptId1);
    assertEquals(mapRecord.getMapProjectId(), new Long("1"));
    assertEquals(mapRecord.getMapEntries().size(), 1);
    assertEquals(mapRecord.getMapPrinciples().size(), 1);
    assertEquals(mapRecord.getConceptId(), conceptId1);
    assertEquals(mapRecord.getMapNotes().size(), 1);

  }

  /**
   * Load map records. Called before each unit test Creates one mapRecord with:
   * - 1 entry (has 1 principle, 1 advice, 1 note) - 1 principle - 1 note
   * 
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Before
  public void addTestData() throws Exception {

    EntityTransaction tx = manager.getTransaction();

    tx.begin();

    // create user (owner)
    mapUser1 = new MapUserJpa();
    mapUser1.setEmail("new email");
    mapUser1.setName("user1");
    mapUser1.setUserName("username1");
    manager.persist(mapUser1);

    // create map record
    mapRecord1 = new MapRecordJpa();
    mapRecord1.setConceptId(conceptId1);
    mapRecord1.setConceptName("conceptTestName");
    mapRecord1.setMapProjectId(new Long("1"));
    mapRecord1.setFlagForConsensusReview(false);
    mapRecord1.setFlagForEditorialReview(false);
    mapRecord1.setFlagForMapLeadReview(false);
    mapRecord1.setLastModified(new Long("1"));
    mapRecord1.setTimestamp(System.currentTimeMillis());
    mapRecord1.setOwner(mapUser1);
    manager.persist(mapRecord1);

    // create map entry
    MapEntry mapEntry = new MapEntryJpa();
    mapEntry.setTargetId("Z53.2");
    mapEntry.setMapRecord(mapRecord1);
    mapEntry.setMapBlock(0);
    mapEntry.setMapGroup(1);
    mapEntry.setMapPriority(1);
    // mapEntry.setRelationId("447561005");
    mapEntry.setRule("RULE");
    mapEntry.setMapRecord(mapRecord1);
    manager.persist(mapEntry);

    // create map advice and persist (independent object)
    MapAdvice mapAdvice = new MapAdviceJpa();
    mapAdvice.setName("ALWAYS Z53.2");
    mapAdvice.setDetail("ALWAYS Z53.2");
    mapAdvice.setAllowableForNullTarget(false);
    mapAdvice.setComputed(false);
    manager.persist(mapAdvice);

    // create map principle and persist (independent object)
    MapPrinciple mapPrinciple = new MapPrincipleJpa();
    mapPrinciple.setDetail("testMapPrincipleDescription");
    mapPrinciple.setName("testMapPrincipleName");
    mapPrinciple.setSectionRef("testMapPrincipleSectionRef");
    manager.persist(mapPrinciple);

    MapNote mapNote = new MapNoteJpa();
    mapNote.setNote("testMapNote1");
    mapNote.setTimestamp(new Date(java.lang.System.currentTimeMillis()));
    mapNote.setUser(mapUser1);

    // add elements to map entry
    // mapEntry.addMapPrinciple(mapPrinciple);
    mapEntry.addMapAdvice(mapAdvice);

    // add elements to map record
    mapRecord1.addMapEntry(mapEntry);
    mapRecord1.addMapPrinciple(mapPrinciple);
    mapRecord1.addMapNote(mapNote);
    manager.merge(mapRecord1);

    tx.commit();
  }

  /**
   * Removes all test data, called after each unit test.
   */
  @SuppressWarnings({
      "unchecked", "static-method"
  })
  @After
  public void removeTestData() {

    EntityTransaction tx = manager.getTransaction();
    // remove map records
    for (MapRecord m : (List<MapRecord>) manager.createQuery(
        "select m from MapRecordJpa m").getResultList()) {
      // delete the map record
      tx.begin();
      if (manager.contains(m)) {
        manager.remove(m);
      } else {
        manager.remove(manager.merge(m));
      }
      tx.commit();
    }

    // remove map advice
    for (MapAdvice m : (List<MapAdvice>) manager.createQuery(
        "select m from MapAdviceJpa m").getResultList()) {
      // delete the map record
      tx.begin();
      if (manager.contains(m)) {
        manager.remove(m);
      } else {
        manager.remove(manager.merge(m));
      }
      tx.commit();
    }

    // remove map users
    for (MapUser m : (List<MapUser>) manager.createQuery(
        "select m from MapUserJpa m").getResultList()) {
      // delete the map record
      tx.begin();
      if (manager.contains(m)) {
        manager.remove(m);
      } else {
        manager.remove(manager.merge(m));
      }
      tx.commit();
    }
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
