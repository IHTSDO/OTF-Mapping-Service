package org.ihtsdo.otf.mapping.mojo;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.ReportFrequency;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.helpers.ReportTimePeriod;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.WorkflowServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.WorkflowService;

/**
 * Mojo for generating sample data for a demo of map application.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal generate-demo-data
 */
public class GenerateDemoDataMojo extends AbstractMojo {

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

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Generate demo data");

    try {

      securityService = new SecurityServiceJpa();
      contentService = new ContentServiceJpa();
      mappingService = new MappingServiceJpa();
      workflowService = new WorkflowServiceJpa();
      reportService = new ReportServiceJpa();

      loadDemoData();

      getLog().info("Finished");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Ad-hoc mojo failed to complete", e);
    } finally {
      try {
        securityService.close();
        contentService.close();
        mappingService.close();
        workflowService.close();
      } catch (Exception e) {
        e.printStackTrace();
        throw new MojoExecutionException(
            "Ad-hoc mojo failed to close services.", e);
      }
    }

  }

  /**
   * Load sample data.
   *
   * @throws Exception the exception
   */
  private void loadDemoData() throws Exception {

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
    for (final String rel : new String[] {
        "exact", "partial", "narrower", "broader"
    }) {
      final String ucRel = rel.substring(0, 1).toUpperCase() + rel.substring(1);
      final MapRelation relation = new MapRelationJpa();
      relation.setAbbreviation(ucRel);
      relation.setAllowableForNullTarget(false);
      relation.setComputed(false);
      relation.setName(ucRel + " match");
      relation.setTerminologyId(rel);
      mappingService.addMapRelation(relation);
      mapRelations.add(relation);
    }

    //
    // Make a project
    //
    Logger.getLogger(getClass()).info("Create project 1");
    MapProject project1 = new MapProjectJpa();
    project1.setDestinationTerminology("SNOMEDCT");
    project1.setDestinationTerminologyVersion("20140731");
    project1.setGroupStructure(true);
    project1.setMapRefsetPattern(MapRefsetPattern.ComplexMap);
    project1.setName("ALLERGY to SNOMEDCT with REVIEW");
    project1
        .setProjectSpecificAlgorithmHandlerClass("org.ihtsdo.otf.mapping.jpa.handlers.AllergyProjectSpecificAlgorithmHandler");
    project1.setPropagatedFlag(false);
    project1.setRefSetId("12345");
    project1.setRefSetName("Allergy to SNOMED Refset");
    project1.setSourceTerminology("ALLERGY");
    project1.setSourceTerminologyVersion("latest");
    project1.setWorkflowType(WorkflowType.REVIEW_PROJECT);
    project1.setMapRelationStyle(RelationStyle.RELATIONSHIP_STYLE);
    project1.getScopeConcepts().add("root");
    project1.setScopeDescendantsFlag(true);
    project1.setMapRelations(mapRelations);
    project1.getMapLeads().add(lead1);
    project1.getMapLeads().add(lead2);
    project1.getMapSpecialists().add(specialist1);
    project1.getMapSpecialists().add(specialist2);
    project1.getMapSpecialists().add(specialist3);

    // Add project
    Logger.getLogger(getClass()).info("  add " + project1);
    project1 = mappingService.addMapProject(project1);
    Logger.getLogger(getClass()).info("  compute workflow");
    workflowService.computeWorkflow(project1);

    // Create project 2
    Logger.getLogger(getClass()).info("Create project 2");
    MapProject project2 = new MapProjectJpa();
    project2.setDestinationTerminology("RXNORM");
    project2.setDestinationTerminologyVersion("2016AA");
    project2.setGroupStructure(true);
    project2.setMapRefsetPattern(MapRefsetPattern.ComplexMap);
    project2.setName("MEDICATION to RXNORM with REVIEW");
    project2
        .setProjectSpecificAlgorithmHandlerClass("org.ihtsdo.otf.mapping.jpa.handlers.MedicationProjectSpecificAlgorithmHandler");
    project2.setPropagatedFlag(false);
    project2.setRefSetId("23456");
    project2.setRefSetName("Medication to RXNORM Mapping");
    project2.setSourceTerminology("MEDICATION");
    project2.setSourceTerminologyVersion("latest");
    project2.setWorkflowType(WorkflowType.REVIEW_PROJECT);
    project2.setMapRelationStyle(RelationStyle.RELATIONSHIP_STYLE);
    project2.getScopeConcepts().add("root");
    project2.setScopeDescendantsFlag(true);
    project2.setMapRelations(mapRelations);
    project2.getMapLeads().add(lead1);
    project2.getMapLeads().add(lead2);
    project2.getMapSpecialists().add(specialist1);
    project2.getMapSpecialists().add(specialist2);
    project2.getMapSpecialists().add(specialist3);

    // Add project
    Logger.getLogger(getClass()).info("  add " + project2);
    project2 = mappingService.addMapProject(project2);
    Logger.getLogger(getClass()).info("  compute workflow");
    workflowService.computeWorkflow(project2);

    // Start editing cycle
    SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    project1.setEditingCycleBeginDate(DATE_FORMAT.parse("20160101"));
    mappingService.updateMapProject(project1);
    project2.setEditingCycleBeginDate(DATE_FORMAT.parse("20160101"));
    mappingService.updateMapProject(project2);

    // Reports
    ReportDefinition def1 = new ReportDefinitionJpa();
    def1.setDescription("Specialist productivity report.");
    def1.setDiffReport(false);
    def1.setFrequency(ReportFrequency.DAILY);
    def1.setName("Specialist productivity");
    def1.setQACheck(false);
    def1.setQuery("select distinct mu.userName value, mr.conceptId itemId, mr.conceptName itemName "
        + "from map_records_AUD mr, map_projects mp, map_users mu  "
        + "where mp.id = :MAP_PROJECT_ID: "
        + " and mr.lastModified >= :EDITING_CYCLE_BEGIN_DATE:    "
        + " and mr.mapProjectId = mp.id "
        + "  and mr.workflowStatus IN ('REVIEW_NEEDED','EDITING_DONE','QA_RESOLVED')  "
        + "  and mu.userName not in ('loader','qa')  "
        + "  and mr.owner_id = mu.id "
        + "  and mr.revtype != 2 "
        + "group by mu.userName, mr.lastModified " + "ORDER BY 1,2");
    def1.setQueryType(ReportQueryType.SQL);
    def1.setResultType(ReportResultType.CONCEPT);
    def1.setRoleRequired(MapUserRole.SPECIALIST);
    def1.setTimePeriod(null);
    reportService.addReportDefinition(def1);

    ReportDefinition def2 = new ReportDefinitionJpa();
    def2.setDescription("Lead productivity report.");
    def2.setDiffReport(false);
    def2.setFrequency(ReportFrequency.DAILY);
    def2.setName("Lead productivity");
    def2.setQACheck(false);
    def2.setQuery("select distinct mu.userName value, mr.conceptId itemId, mr.conceptName itemName "
        + "from map_records_AUD mr, map_projects mp, map_users mu "
        + "where mp.id = :MAP_PROJECT_ID:  "
        + " and mr.lastModified >= :EDITING_CYCLE_BEGIN_DATE:   "
        + " and mr.mapProjectId = mp.id "
        + "  and mr.workflowStatus IN ('READY_FOR_PUBLICATION')  "
        + "  and mu.userName != 'loader'  "
        + "  and mr.owner_id = mu.id "
        + "  and mr.lastModified <= :TIMESTAMP:  "
        + "  and mr.revtype != 2 "
        + "  and mr.conceptId IN "
        + "  (select conceptid from map_records_AUD mr2 "
        + "   where mapProjectId = :MAP_PROJECT_ID: "
        + "     and workflowStatus in ('CONFLICT_RESOLVED','REVIEW_RESOLVED') "
        + "     and mr.owner_id = mr2.owner_id) "
        + "group by mu.userName, mr.lastModified  " + "ORDER BY 1,2");
    def2.setQueryType(ReportQueryType.SQL);
    def2.setResultType(ReportResultType.CONCEPT);
    def2.setRoleRequired(MapUserRole.LEAD);
    def2.setTimePeriod(null);
    reportService.addReportDefinition(def2);

    // Daily specialist report
    ReportDefinition def3 = new ReportDefinitionJpa();
    def3.setDescription("Daily specialist productivity report.");
    def3.setDiffReport(true);
    def3.setFrequency(ReportFrequency.DAILY);
    def3.setName("Daily specialist productivity");
    def3.setQACheck(false);
    def3.setQuery(null);
    def3.setQueryType(ReportQueryType.NONE);
    def3.setResultType(ReportResultType.CONCEPT);
    def3.setRoleRequired(MapUserRole.SPECIALIST);
    def3.setTimePeriod(ReportTimePeriod.DAILY);
    def3.setDiffReportDefinitionName("Specialist productivity");
    reportService.addReportDefinition(def3);

    // Daily lead report
    ReportDefinition def4 = new ReportDefinitionJpa();
    def4.setDescription("Daily lead productivity report.");
    def4.setDiffReport(true);
    def4.setFrequency(ReportFrequency.DAILY);
    def4.setName("Daily lead productivity");
    def4.setQACheck(false);
    def4.setQuery(null);
    def4.setQueryType(ReportQueryType.NONE);
    def4.setResultType(ReportResultType.CONCEPT);
    def4.setRoleRequired(MapUserRole.LEAD);
    def4.setTimePeriod(ReportTimePeriod.DAILY);
    def3.setDiffReportDefinitionName("Lead productivity");
    reportService.addReportDefinition(def4);

    // specialist productivity, lead productivity

    // Add report definitions to the project(s)
    project1.getReportDefinitions().add(def1);
    project1.getReportDefinitions().add(def2);
    project1.getReportDefinitions().add(def3);
    project1.getReportDefinitions().add(def4);
    mappingService.updateMapProject(project1);

    project2.getReportDefinitions().add(def1);
    project2.getReportDefinitions().add(def2);
    project2.getReportDefinitions().add(def3);
    project2.getReportDefinitions().add(def4);
    mappingService.updateMapProject(project2);

    // Generate the reports
    reportService.generateDailyReports(project1, lead1);
    reportService.generateDailyReports(project2, lead1);

    // QA checks
    ReportDefinition qa1 = new ReportDefinitionJpa();
    qa1.setDescription("Sample QA check to identify mappings involving the word 'peanut'");
    qa1.setDiffReport(false);
    qa1.setFrequency(ReportFrequency.ON_DEMAND);
    qa1.setName("Peanut records");
    qa1.setQACheck(true);
    qa1.setQuery("select distinct mr.conceptName value, mr.conceptId itemId, mr.conceptName itemName "
        + "from map_records mr "
        + "where mr.mapProjectId = :MAP_PROJECT_ID: "
        + " and mr.conceptName like '%eanut%'");
    qa1.setQueryType(ReportQueryType.SQL);
    qa1.setResultType(ReportResultType.CONCEPT);
    qa1.setRoleRequired(MapUserRole.SPECIALIST);
    qa1.setTimePeriod(null);
    reportService.addReportDefinition(qa1);

    // TODO: add qa check for "invalid codes"
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
}
