package org.ihtsdo.otf.mapping.jpa.services;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.hibernate.envers.AuditReader;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The Class MappingServiceTest.
 */
public class MappingServiceJpaTest {

  /** The mapping service */
  private static MappingServiceJpa service;

  /** The manager. */
  private static EntityManager manager;

  /** The factory. */
  private static EntityManagerFactory factory;

  /** The audit history reader. */
  private static AuditReader reader;

  /**
   * Creates db tables, load test objects and create indexes to prepare for test
   * cases.
   * @throws Exception
   */
  @BeforeClass
  public static void init() throws Exception {

    Logger.getLogger(MappingServiceJpaTest.class).info(
        "Initializing MappingServiceTest");

    // construct the mapping service
    service = new MappingServiceJpa();

  }

  /**
   * Cleanup database
   * @throws Exception
   * @throws FileNotFoundException
   */
  @SuppressWarnings("static-method")
  @After
  public void cleanup() throws Exception {

    Logger.getLogger(MappingServiceJpaTest.class).info(
        "Cleaning up MappingServiceJpaTest");

    // create new database connection
    Properties config = ConfigUtility.getTestConfigProperties();
    factory =
        Persistence.createEntityManagerFactory("MappingServiceDS", config);
    manager = factory.createEntityManager();
    EntityTransaction tx = manager.getTransaction();

    // remove remaining test data
    javax.persistence.Query query;
    tx.begin();

    // //////////////////////////
    // Delete from main tables
    // //////////////////////////

    // Note: Cleaning up using queries because of need to truncate audit tables

    // notes
    query = manager.createNativeQuery("DELETE FROM map_entries_map_notes");
    query.executeUpdate();
    query = manager.createNativeQuery("DELETE FROM map_records_map_notes");
    query.executeUpdate();
    query = manager.createNativeQuery("DELETE FROM map_notes");
    query.executeUpdate();

    // principles
    query = manager.createNativeQuery("DELETE FROM map_records_map_principles");
    query.executeUpdate();
    query =
        manager.createNativeQuery("DELETE FROM map_projects_map_principles");
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

    // specialists
    query =
        manager.createNativeQuery("DELETE FROM map_projects_map_specialists");
    query.executeUpdate();

    // projects
    query = manager.createNativeQuery("DELETE FROM map_projects");
    query.executeUpdate();

    tx.commit();

    // //////////////////////////
    // Delete from audit tables
    // //////////////////////////

    tx.begin();

    // notes
    query = manager.createNativeQuery("DELETE FROM map_entries_map_notes_aud");
    query.executeUpdate();
    query = manager.createNativeQuery("DELETE FROM map_records_map_notes_aud");
    query.executeUpdate();
    query = manager.createNativeQuery("DELETE FROM map_notes_aud");
    query.executeUpdate();

    // principles
    query =
        manager.createNativeQuery("DELETE FROM map_records_map_principles_aud");
    query.executeUpdate();
    query =
        manager
            .createNativeQuery("DELETE FROM map_projects_map_principles_aud");
    query.executeUpdate();
    query = manager.createNativeQuery("DELETE FROM map_principles_aud");
    query.executeUpdate();

    // advices
    query =
        manager.createNativeQuery("DELETE FROM map_entries_map_advices_aud");
    query.executeUpdate();
    query =
        manager.createNativeQuery("DELETE FROM map_projects_map_advices_aud");
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

    // specialists
    query =
        manager
            .createNativeQuery("DELETE FROM map_projects_map_specialists_aud");
    query.executeUpdate();

    // projects
    query = manager.createNativeQuery("DELETE FROM map_projects_aud");
    query.executeUpdate();

    tx.commit();

    Logger.getLogger(MappingServiceJpaTest.class).info("Cleanup complete");

  }

  private static void fail(String string) {

    Logger.getLogger(MappingServiceJpaTest.class).info("Failed: " + string);

  }

  /**
   * Create mock data and test adding the objects
   * 
   * @throws Exception
   */
  @Before
  public void addElements() throws Exception {

    Logger.getLogger(MappingServiceJpaTest.class)
        .info("Testing element add...");

    // ASSUMPTION: Database is unloaded, starting fresh

    List<MapProject> projects = new ArrayList<>();
    List<MapUser> specialists = new ArrayList<>();
    List<MapUser> leads = new ArrayList<>();

    // Add Specialists and Leads
    MapUserJpa mapLead = new MapUserJpa();
    mapLead.setName("Kathy Giannangelo");
    mapLead.setUserName("kgi");
    mapLead.setEmail("***REMOVED***");
    leads.add(mapLead);

    mapLead = new MapUserJpa();
    mapLead.setName("Donna Morgan");
    mapLead.setUserName("dmo");
    mapLead.setEmail("***REMOVED***");
    leads.add(mapLead);

    mapLead = new MapUserJpa();
    mapLead.setName("Julie O'Halloran");
    mapLead.setUserName("joh");
    mapLead.setEmail("***REMOVED***");
    leads.add(mapLead);

    MapUserJpa mapSpecialist = new MapUserJpa();
    mapSpecialist.setName("Krista Lilly");
    mapSpecialist.setUserName("kli");
    mapSpecialist.setEmail("***REMOVED***");
    specialists.add(mapSpecialist);

    mapSpecialist = new MapUserJpa();
    mapSpecialist.setName("Nicola Ingram");
    mapSpecialist.setUserName("nin");
    mapSpecialist.setEmail("***REMOVED***");
    specialists.add(mapSpecialist);

    mapSpecialist = new MapUserJpa();
    mapSpecialist.setName("Rory Davidson");
    mapSpecialist.setUserName("rda");
    mapSpecialist.setEmail("***REMOVED***");
    specialists.add(mapSpecialist);

    mapSpecialist = new MapUserJpa();
    mapSpecialist.setName("Graeme Miller");
    mapSpecialist.setUserName("gmi");
    mapSpecialist.setEmail("***REMOVED***");
    specialists.add(mapSpecialist);

    for (MapUser m : specialists) {
      Logger.getLogger(this.getClass()).info("Adding map user " + m.getName());
      service.addMapUser(m);
    }

    for (MapUser m : leads) {
      Logger.getLogger(this.getClass()).info("Adding map user " + m.getName());
      service.addMapUser(m);
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
      advice.setAllowableForNullTarget(true);
      advice.setComputed(true);
      mapAdvices.add(advice);
      service.addMapAdvice(advice);
    }

    Map<String, MapAdvice> mapAdviceValueMap = new HashMap<>();
    for (MapAdvice m : mapAdvices) {
      Logger.getLogger(this.getClass())
          .info("Adding map advice " + m.getName());

      mapAdviceValueMap.put(m.getName(), m);
    }

    // Add map projects
    MapProject mapProject = new MapProjectJpa();
    mapProject.setName("SNOMED to ICD10");
    mapProject.setRefSetId("447562003");
    mapProject.setRefSetName("refSetName1");
    mapProject.setSourceTerminology("SNOMEDCT");
    mapProject.setSourceTerminologyVersion("20140131");
    mapProject.setDestinationTerminology("ICD10");
    mapProject.setDestinationTerminologyVersion("2010");
    mapProject.setGroupStructure(true);
    mapProject.setPublished(true);
    mapProject.setMapRelationStyle(RelationStyle.MAP_CATEGORY_STYLE);
    mapProject.setMapPrincipleSourceDocument("mapPrincipleSourceDocument1");
    mapProject.setMapPrincipleSourceDocumentName("mapPrincipleSourceDocument1");
    mapProject.setRuleBased(true);
    mapProject.setMapRefsetPattern(MapRefsetPattern.ComplexMap);
    mapProject
        .setProjectSpecificAlgorithmHandlerClass("projectSpecificAlgorithmHandlerClass1");
    mapProject.addMapLead(leads.get(0));
    mapProject.addMapLead(leads.get(1));
    mapProject.addMapSpecialist(specialists.get(0));
    mapProject.addMapSpecialist(specialists.get(1));
    mapProject.addMapSpecialist(specialists.get(2));
    mapProject.setScopeDescendantsFlag(true);
    mapProject.setScopeExcludedDescendantsFlag(true);
    for (String s : icd10AdviceValues) {
      mapProject.addMapAdvice(mapAdviceValueMap.get(s));
    }

    projects.add(mapProject);

    mapProject = new MapProjectJpa();
    mapProject.setName("SNOMED to ICD9CM");
    mapProject.setRefSetId("5781347179");
    mapProject.setRefSetName("refSetName2");
    mapProject.setSourceTerminology("SNOMEDCT");
    mapProject.setSourceTerminologyVersion("20140131");
    mapProject.setDestinationTerminology("ICD9CM");
    mapProject.setDestinationTerminologyVersion("2013");
    mapProject.setGroupStructure(true);
    mapProject.setPublished(true);
    mapProject.setMapRelationStyle(RelationStyle.MAP_CATEGORY_STYLE);
    mapProject.setMapPrincipleSourceDocument("mapPrincipleSourceDocument2");
    mapProject.setMapPrincipleSourceDocumentName("mapPrincipleSourceDocument2");
    mapProject.setRuleBased(true);
    mapProject.setMapRefsetPattern(MapRefsetPattern.ComplexMap);
    mapProject
        .setProjectSpecificAlgorithmHandlerClass("projectSpecificAlgorithmHandlerClass1");
    mapProject.addMapLead(leads.get(0));
    mapProject.addMapLead(leads.get(1));
    mapProject.addMapSpecialist(specialists.get(0));
    mapProject.addMapSpecialist(specialists.get(1));
    mapProject.setScopeDescendantsFlag(true);
    mapProject.setScopeExcludedDescendantsFlag(true);
    for (String s : icd9cmAdviceValues) {
      mapProject.addMapAdvice(mapAdviceValueMap.get(s));
    }
    projects.add(mapProject);

    mapProject = new MapProjectJpa();
    mapProject.setName("SNOMED to ICPC - Family Practice/GPF Refset");
    mapProject.setRefSetId("5235669");
    mapProject.setRefSetName("refSetName3");
    mapProject.setSourceTerminology("SNOMEDCT");
    mapProject.setSourceTerminologyVersion("20130731");
    mapProject.setDestinationTerminology("ICPC");
    mapProject.setDestinationTerminologyVersion("2");
    mapProject.setGroupStructure(false);
    mapProject.setPublished(false);
    mapProject.setMapRelationStyle(RelationStyle.MAP_CATEGORY_STYLE);
    mapProject.setMapPrincipleSourceDocument("mapPrincipleSourceDocument3");
    mapProject.setMapPrincipleSourceDocumentName("mapPrincipleSourceDocument3");
    mapProject.setRuleBased(true);
    mapProject.setMapRefsetPattern(MapRefsetPattern.ComplexMap);
    mapProject
        .setProjectSpecificAlgorithmHandlerClass("projectSpecificAlgorithmHandlerClass3");
    mapProject.addMapLead(leads.get(2));
    mapProject.addMapSpecialist(specialists.get(3));
    mapProject.setScopeDescendantsFlag(true);
    mapProject.setScopeExcludedDescendantsFlag(true);
    for (String s : icpcAdviceValues) {
      mapProject.addMapAdvice(mapAdviceValueMap.get(s));
    }
    projects.add(mapProject);

    for (MapProject m : projects) {
      Logger.getLogger(MappingServiceJpaTest.class).info(
          "Adding map project " + m.getName());
      service.addMapProject(m);
    }
  }

  /**
   * Test retrieval of elements from existing database
   * 
   * @throws Exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void testRetrieveElements() throws Exception {
    Logger.getLogger(MappingServiceJpaTest.class).info(
        "Testing element retrieval...");

    // Test retrieval of all elements

    // get all objects
    MapProjectList projects = service.getMapProjects();
    MapUserList users = service.getMapUsers();

    if (projects.getCount() != 3) {
      fail("Retrieval - getMapProjects():  Found "
          + Integer.toString(projects.getCount()) + " projects, expected 3");
    }
    if (projects.getMapProjects().get(1).getName()
        .compareTo("SNOMED to ICD9CM") != 0) {
      fail("Retrieval - getMapProjects():  Project name for project 2 not equal to 'SNOMED to ICD9CM'");
    }
    if (projects.getMapProjects().get(2).getMapLeads().size() != 1) {
      fail("Retrieval - getMapProjects():  Number of project leads for project 3 is not equal to 1");
    }

    // test users
    if (users.getCount() != 7) {
      fail("Retrieval - getMapUsers():  Found "
          + Integer.toString(users.getCount()) + " users, expected 7");
    }

    if (users.getMapUsers().get(0).getUserName().compareTo("kli") != 0) {
      fail("Retrieval - getMapUsers():  Map user username for user 1 not equal to 'kli'");
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

    // TODO: get record (id)

    // QUERY TESTS
    SearchResultList results = new SearchResultListJpa();

    // find project (query)
    results = service.findMapProjectsForQuery("ICD9CM", new PfsParameterJpa());
    if (results.getSearchResults().get(0).getValue()
        .compareTo("SNOMED to ICD9CM") != 0) {
      fail("Retrieval - findMapProjects(): Could not search by name of terminology");
    }

    results = service.findMapProjectsForQuery("Kathy", new PfsParameterJpa());
    if (results.getCount() != 1) {
      fail("Retrieval - findMapProjects(String query):  Could not search by lead name");
    }

    // test project retrieval by user (lead/specialist)
    MapUser s = service.getMapUser(new Long(1));
    if (service.getMapProjectsForMapUser(s).getCount() != 2) {
      fail("Retrieval - getMapProjectsForMapUser(MapUser mapUser): Failed to retrieve projects");
    }

    s = service.getMapUser(new Long(2));
    if (service.getMapProjectsForMapUser(s).getCount() != 1) {
      fail("Retrieval - getMapProjectsForMapUser(MapUser mapUser):  Failed to retrieve projects");
    }

    // TODO find record, advice (query)

  }

  /**
   * Test updating each type of element
   * 
   * @throws Exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void testUpdateElements() throws Exception {
    Logger.getLogger(MappingServiceJpaTest.class).info(
        "Testing element update...");

    MapProjectList projects = service.getMapProjects();
    MapUserList specialists = service.getMapUsers();

    String changedValue = "updatetest";

    // test update and envers audit of map project
    MapProject project_old = projects.getMapProjects().get(0);
    MapProject project_new;
    List<Number> revNumbers =
        reader.getRevisions(MapProject.class, project_old);

    project_old.setDestinationTerminology(changedValue);
    service.updateMapProject(project_old);

    project_new = service.getMapProject(project_old.getId());

    if (!project_new.getDestinationTerminology().equals(changedValue)) {
      fail("Failed to update project");
    } else {
      Logger.getLogger(MappingServiceJpaTest.class).info(
          "Project update successful");
    }
    // TODO Reenable for audit testing

    if (reader.getRevisions(MapProject.class, project_old).size() != revNumbers
        .size() + 1) {
      fail("Failed to update revision table:  number of revisions has not increased by 1");
    }

    // test update and envers audit of map specialist
    MapUser specialist_old = specialists.getMapUsers().get(0);
    MapUser specialist_new;
    // revNumbers = reader.getRevisions(MapSpecialist.class,
    // specialist_old); // TODO Reenable for audit testing

    specialist_old.setEmail(changedValue);
    service.updateMapUser(specialist_old);

    specialist_new = service.getMapUser(specialist_old.getId());

    if (!specialist_new.getEmail().equals(changedValue)) {
      fail("Failed to update specialist");
    } else {
      Logger.getLogger(MappingServiceJpaTest.class).info(
          "Specialist update successful");
    }
    // TODO Reenable for audit testing

    /*
     * if (reader.getRevisions(MapSpecialist.class, specialist_old).size() !=
     * revNumbers.size() + 1) { fail(
     * "Failed to update revision table:  number of revisions has not increased by 1"
     * ); }
     */

    // TODO Determine desired audit results for project

  }

  /**
   * Test removal of each type of element Further test propagation of removal of
   * lead/specialist on project
   * 
   * @throws Exception
   */
  @Test
  public void testRemoveElements() throws Exception {
    Logger.getLogger(MappingServiceJpaTest.class).info(
        "Testing element remove...");

    // test delete and envers audit of map project
    MapProjectList projects = service.getMapProjects();
    MapUserList users = service.getMapUsers();

    MapProject project_removed = projects.getMapProjects().get(0);
    MapUser user_removed = users.getMapUsers().get(0);

    // test delete of user
    Logger.getLogger(MappingServiceJpaTest.class).info(
        "Testing user removal...");
    service.removeMapUser(user_removed.getId());

    if (service.getMapUser(user_removed.getId()) != null) {
      fail("Remove user reported success, but user still present in database!");
    }

    // test delete of project
    Logger.getLogger(MappingServiceJpaTest.class).info(
        "Testing project removal...");
    service.removeMapProject(project_removed.getId());

    if (service.getMapProject(project_removed.getId()) != null) {
      fail("Remove project reported success, but project still present in database!");
    }

    cleanup();
  }

}
