/*
 *    Copyright 2017 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.test.mojo;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.WorkflowService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A mechanism to reset to the stock demo database.
 */
public class ResetIcd11Database {

  /** The properties. */
  static Properties config;

  /** The server. */
  static String server = "false";

  /** The security service. */
  SecurityService securityService = null;

  /** The content service. */
  ContentService contentService = null;

  /** The mapping service. */
  MappingService mappingService = null;

  /** The workflow service. */
  WorkflowService workflowService = null;

  /** The report service. */
  ReportService reportService = null;

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    config = ConfigUtility.getConfigProperties();
  }

  /**
   * Test the sequence:
   * 
   * <pre>
   * 1. Load SNOMED terminology
   * 2. Load "allergy" terminology
   * 3. Load "medication" terminology
   * 4. Load the "rxnorm" terminology
   * </pre>
   * 
   * @throws Exception the exception
   */
  @Test
  public void test() throws Exception {

    if (config.getProperty("data.dir") == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Create database
    Logger.getLogger(getClass()).info("Create database");
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/updatedb/pom.xml"));
    request.setProfiles(Arrays.asList("Updatedb"));
    request.setGoals(Arrays.asList("clean", "install"));
    Properties p = new Properties();
    p.setProperty("run.config", System.getProperty("run.config"));
    p.setProperty("hibernate.hbm2ddl.auto", "create");
    request.setProperties(p);
    request.setDebug(false);
    Invoker invoker = new DefaultInvoker();
    InvocationResult result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Clear indexes
    Logger.getLogger(getClass()).info("Clear indexes");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/lucene/pom.xml"));
    request.setProfiles(Arrays.asList("Reindex"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config", System.getProperty("run.config"));
    request.setProperties(p);
    request.setDebug(false);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Load ICD11 from simple files
    Logger.getLogger(getClass()).info("Load ICD11");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("Simple"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config", System.getProperty("run.config"));
    p.setProperty("terminology", "ICD11");
    p.setProperty("version", "latest");
    p.setProperty("input.file",
        System.getProperty("icd11.dir") + "/" + "icd11Concepts.txt");
    p.setProperty("par.chd.file",
        System.getProperty("icd11.dir") + "/" + "icd11ParChd.txt");
    request.setProperties(p);
    request.setDebug(false);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Load RF2 snapshot
    Logger.getLogger(getClass()).info("Load SNOMED");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("RF2-snapshot"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config", System.getProperty("run.config"));
    p.setProperty("terminology", "SNOMEDCT");
    p.setProperty("version", "latest");
    if (System.getProperty("sct.dir") == null) {
      throw new Exception("Property sct.dir must be set");
    }
    p.setProperty("input.dir", System.getProperty("sct.dir"));
    request.setProperties(p);
    request.setDebug(false);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Create projects and users and all that
    loadIcd11Project();

    // Load map records
    Logger.getLogger(getClass()).info("Load ICD11 Maps");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("MapRecords"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config", System.getProperty("run.config"));
    p.setProperty("input.file",
        System.getProperty("icd11.dir") + "/icd11Map.txt");
    p.setProperty("member.flag", "false");
    p.setProperty("record.flag", "true");
    request.setProperties(p);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Reindex
    // Using this load mechanism, reindexing is requrired for ConceptJpa,
    // possibly others
    Logger.getLogger(getClass()).info("Reindex concepts");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/lucene/pom.xml"));
    request.setProfiles(Arrays.asList("Reindex"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config", System.getProperty("run.config"));
    p.setProperty("indexed.objects", "ConceptJpa");
    request.setProperties(p);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

  }

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    // n/a
  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    // n/a
  }

  /**
   * Load sample data.
   *
   * @throws Exception the exception
   */
  private void loadIcd11Project() throws Exception {
    securityService = new SecurityServiceJpa();
    contentService = new ContentServiceJpa();
    mappingService = new MappingServiceJpa();
    workflowService = new WorkflowServiceJpa();
    reportService = new ReportServiceJpa();

    //
    // Add lead users
    //
    Logger.getLogger(getClass()).info("Add new lead users");
    MapUserJpa lead1 = (MapUserJpa) securityService.getMapUser("lead1");
    if (lead1 == null) {
      lead1 = makeMapUser("lead1", "Lead1");
      lead1 = (MapUserJpa) securityService.addMapUser(lead1);
    }
    MapUserJpa lead2 = (MapUserJpa) securityService.getMapUser("lead2");
    if (lead2 == null) {
      lead2 = makeMapUser("lead2", "Lead2");
      lead2 = (MapUserJpa) securityService.addMapUser(lead2);
    }
    MapUserJpa lead3 = (MapUserJpa) securityService.getMapUser("lead3");
    if (lead3 == null) {
      lead3 = makeMapUser("lead3", "Lead3");
      lead3 = (MapUserJpa) securityService.addMapUser(lead3);
    }

    //
    // Add specialist users
    //
    Logger.getLogger(getClass()).info("Add new specialist users");
    MapUserJpa specialist1 =
        (MapUserJpa) securityService.getMapUser("specialist1");
    if (specialist1 == null) {
      specialist1 = makeMapUser("specialist1", "Specialist1");
      specialist1 = (MapUserJpa) securityService.addMapUser(specialist1);
    }
    MapUserJpa specialist2 =
        (MapUserJpa) securityService.getMapUser("specialist2");
    if (specialist2 == null) {
      specialist2 = makeMapUser("specialist2", "Specialist2");
      specialist2 = (MapUserJpa) securityService.addMapUser(specialist2);
    }
    MapUserJpa specialist3 =
        (MapUserJpa) securityService.getMapUser("specialist3");
    if (specialist3 == null) {
      specialist3 = makeMapUser("specialist3", "Specialist3");
      specialist3 = (MapUserJpa) securityService.addMapUser(specialist3);
    }

    //
    // Mapping relationships
    //
    final Set<MapRelation> mapRelations = new HashSet<>();
    // | 37 | none | 0 | 1 | MAP SOURCE CONCEPT IS PROPERLY CLASSIFIED |
    // 447637006
    MapRelation relation = new MapRelationJpa();
    relation.setAbbreviation("none");
    relation.setName("MAP SOURCE CONCEPT IS PROPERLY CLASSIFIED");
    relation.setTerminologyId("447637006");
    relation.setAllowableForNullTarget(false);
    relation.setComputed(true);
    mappingService.addMapRelation(relation);
    mapRelations.add(relation);
    // | 38 | Not Classifiable |1 | 0 | MAP SOURCE CONCEPT CANNOT BE CLASSIFIED
    // WITH AVAILABLE DATA | 447638001 |
    relation = new MapRelationJpa();
    relation.setAbbreviation("Not Classifiable");
    relation
        .setName("MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA");
    relation.setTerminologyId("447638001");
    relation.setAllowableForNullTarget(true);
    relation.setComputed(false);
    mappingService.addMapRelation(relation);
    mapRelations.add(relation);
    // | 39 | none |0 | 1 | MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT |
    // 447639009 |
    relation = new MapRelationJpa();
    relation.setAbbreviation("none");
    relation.setName("MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT");
    relation.setTerminologyId("447639009");
    relation.setAllowableForNullTarget(false);
    relation.setComputed(true);
    mappingService.addMapRelation(relation);
    mapRelations.add(relation);
    // | 40 | Ambiguous SNOMED CT Concept |1 | 0 | SOURCE SNOMED CONCEPT IS
    // AMBIGUOUS | 447640006 |
    relation = new MapRelationJpa();
    relation.setAbbreviation("Ambiguous SNOMED CT Concept");
    relation.setName("SOURCE SNOMED CONCEPT IS AMBIGUOUS");
    relation.setTerminologyId("447640006");
    relation.setAllowableForNullTarget(true);
    relation.setComputed(false);
    mappingService.addMapRelation(relation);
    mapRelations.add(relation);
    // | 41 | Incomplete |1 | 0 | SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED
    // | 447641005 |
    relation = new MapRelationJpa();
    relation.setAbbreviation("Incomplete");
    relation.setName("SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED");
    relation.setTerminologyId("447641005");
    relation.setAllowableForNullTarget(true);
    relation.setComputed(false);
    mappingService.addMapRelation(relation);
    mapRelations.add(relation);
    // | 42 | Ambiguous WHO ICD-10 Code |1 | 0 | MAPPING GUIDANCE FROM WHO IS
    // AMBIGUOUS | 447635003 |
    relation = new MapRelationJpa();
    relation.setAbbreviation("Ambiguous WHO ICD-10 Code");
    relation.setName("MAPPING GUIDANCE FROM WHO IS AMBIGUOUS");
    relation.setTerminologyId("447635003");
    relation.setAllowableForNullTarget(true);
    relation.setComputed(false);
    mappingService.addMapRelation(relation);
    mapRelations.add(relation);

    //
    // Mapping Advice
    //
    final Set<MapAdvice> mapAdvices = new HashSet<>();
    for (final String adv : new String[] {
        "ADDITIONAL CODES MAY BE ADDED AS SANCTIONED BY WHO",
        "CODES SANCTIONED BY WHO MAY BE ADDED FROM CHAPTER 23 EXTERNAL CAUSES AND DRUG EXTENSION CODES IF RELEVANT",
        "CODES SANCTIONED BY WHO MAY BE ADDED FROM CHAPTER 23 EXTERNAL CAUSES AND EXTENSION CODES IF RELEVANT",
        "EXTENSION CODES SANCTIONED BY WHO MAY BE ADDED IF RELEVANT",
        "MAPPED FOLLOWING SNOMED GUIDANCE", "MAPPED FOLLOWING WHO GUIDANCE",
        "POSSIBLE REQUIREMENT FOR INFECTIOUS AGENT EXTENSION CODE",
        "THIS CODE MAY BE USED IN THE PRIMARY POSITION WHEN THE MANIFESTATION IS THE PRIMARY FOCUS OF CARE",
        "THIS IS AN EXTERNAL CAUSE CODE AND/OR EXTENSION CODE FOR USE IN A SECONDARY POSITION"
    }) {
      final MapAdvice advice = new MapAdviceJpa();
      advice.setAllowableForNullTarget(false);
      advice.setComputed(false);
      advice.setDetail(adv);
      advice.setName(adv);
      mappingService.addMapAdvice(advice);
      mapAdvices.add(advice);
    }

    // Create project SNOMED to ICD10
    Logger.getLogger(getClass()).info("Create project SNOMEDCT to ICD11");
    MapProject project3 = new MapProjectJpa();
    project3.setDestinationTerminology("ICD11");
    project3.setDestinationTerminologyVersion("latest");
    project3.setGroupStructure(true);
    project3.setMapRefsetPattern(MapRefsetPattern.ExtendedMap);
    project3.setName("SNOMEDCT to ICD11 Pilot");
    project3.setProjectSpecificAlgorithmHandlerClass(
        "org.ihtsdo.otf.mapping.jpa.handlers.ICD11ProjectSpecificAlgorithmHandler");
    project3.setPropagatedFlag(false);
    project3.setRefSetId("icd11RefsetId");
    project3.setRefSetName("SNOMEDCT to ICD11");
    project3.setSourceTerminology("SNOMEDCT");
    project3.setSourceTerminologyVersion("latest");
    project3.setRuleBased(true);
    project3.setGroupStructure(true);
    project3.setWorkflowType(WorkflowType.CONFLICT_PROJECT);
    project3.setMapRelationStyle(RelationStyle.MAP_CATEGORY_STYLE);
    project3.getScopeConcepts().add("243796009");
    project3.getScopeConcepts().add("272379006");
    project3.getScopeConcepts().add("404684003");
    project3.setScopeDescendantsFlag(true);
    project3.setMapRelations(mapRelations);
    project3.setMapAdvices(mapAdvices);
    project3.getMapLeads().add(lead1);
    project3.getMapLeads().add(lead2);
    project3.getMapSpecialists().add(specialist1);
    project3.getMapSpecialists().add(specialist2);
    project3.getMapSpecialists().add(specialist3);

    // Add project
    Logger.getLogger(getClass()).info("  add " + project3);
    project3 = mappingService.addMapProject(project3);
    Logger.getLogger(getClass()).info(" compute workflow");
    workflowService.computeWorkflow(project3);

  }

  /**
   * Make user.
   *
   * @param userName the user name
   * @param name the name
   * @return the user
   */
  @SuppressWarnings("static-method")
  private MapUserJpa makeMapUser(String userName, String name) {
    final MapUserJpa user = new MapUserJpa();
    user.setUserName(userName);
    user.setName(name);
    user.setEmail(userName + "@example.com");
    user.setApplicationRole(MapUserRole.VIEWER);
    return user;
  }

  /**
   * Teardown class.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {
    // n/a
  }

}
