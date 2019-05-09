/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.ReportFrequency;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.helpers.ReportTimePeriod;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mojo for generating sample data for a demo of map application.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-app-config-data
 */
public class LoadAppConfigDataMojo extends AbstractMojo {

  /**
   * Advice Config Load file
   * 
   * @parameter
   */
  private String adviceConfigFile = null;

  /**
   * AgeRange Config Load file
   * 
   * @parameter
   */
  private String ageRangeConfigFile = null;

  /**
   * Relation Config Load file
   * 
   * @parameter
   */
  private String relationConfigFile = null;

  /**
   * Mapping Project Config Load file
   * 
   * @parameter
   * @required
   */
  private String projectConfigFile = null;

  /**
   * Report Config Load file
   * 
   * @parameter
   */
  private String reportConfigFile = null;

  /**
   * User Config Load file
   * 
   * @parameter
   */
  private String userConfigFile = null;

  /**
   * Principle Config Load file
   * 
   * @parameter
   */
  private String principleConfigFile = null;

  private ObjectMapper mapper = new ObjectMapper();

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Load App Config Data Started");

    try {
      loadConfigData();
      getLog().info("Load App Config Data Finished");

    } catch (Exception e) {
      getLog().error(e);
      throw new MojoExecutionException(
          "LoadAppConfigData mojo failed to complete", e);
    }

  }

  /**
   * Load sample data.
   *
   * @throws Exception
   *           the exception
   */
  private void loadConfigData() throws Exception {

    if (projectConfigFile == null)
      throw new IllegalArgumentException(
          "Mapping Project Configuration file not specified.  Please check configuration file.");

    if (!Files.exists(Paths.get(projectConfigFile)))
      throw new FileNotFoundException(String.format(
          "Mapping Project Configuration file does not exist.  Please check file path %s.",
          projectConfigFile));

    if (reportConfigFile == null)
      Logger.getLogger(getClass())
          .info("ReportConfigFile is not provided.  Not loading reports.");
    else if (!Files.exists(Paths.get(reportConfigFile)))
      throw new FileNotFoundException(String.format(
          "Report Configuration file does not exist.  Please check file path %s.",
          reportConfigFile));

    LoadUser[] usersConfig = new LoadUser[] {};
    LoadPrinciple[] principlesConfig = new LoadPrinciple[] {};
    LoadAdvice[] advicesConfig = new LoadAdvice[] {};
    LoadAgeRange[] ageRangesConfig = new LoadAgeRange[] {};
    LoadRelation[] relationsConfig = new LoadRelation[] {};
    LoadReportDefinition[] reportsConfig = new LoadReportDefinition[] {};
    LoadMapProject[] mapProjectsConfig = new LoadMapProject[] {};

    // verify all files can be parsed before calling load
    if (userConfigFile != null && Files.exists(Paths.get(userConfigFile))) {
      usersConfig = mapper.readValue(new File(userConfigFile),
          LoadUser[].class);
    }

    if (principleConfigFile != null
        && Files.exists(Paths.get(principleConfigFile))) {
      principlesConfig = mapper.readValue(new File(principleConfigFile),
          LoadPrinciple[].class);
    }

    if (adviceConfigFile != null && Files.exists(Paths.get(adviceConfigFile))) {
      advicesConfig = mapper.readValue(new File(adviceConfigFile),
          LoadAdvice[].class);
    }

    if (ageRangeConfigFile != null
        && Files.exists(Paths.get(ageRangeConfigFile))) {
      ageRangesConfig = mapper.readValue(new File(ageRangeConfigFile),
          LoadAgeRange[].class);
    }

    if (relationConfigFile != null
        && Files.exists(Paths.get(relationConfigFile))) {
      relationsConfig = mapper.readValue(new File(relationConfigFile),
          LoadRelation[].class);
    }

    if (reportConfigFile != null && Files.exists(Paths.get(reportConfigFile))) {
      reportsConfig = mapper.readValue(new File(reportConfigFile),
          LoadReportDefinition[].class);

    }

    if (projectConfigFile != null
        && Files.exists(Paths.get(projectConfigFile))) {
      mapProjectsConfig = mapper.readValue(new File(projectConfigFile),
          LoadMapProject[].class);
    }

    loadUsers(usersConfig);
    loadPrinciples(principlesConfig);
    loadAdvice(advicesConfig);
    loadAgeRange(ageRangesConfig);
    loadRelation(relationsConfig);
    loadReports(reportsConfig);
    loadProjects(mapProjectsConfig);

  }

  // Mapping Projects
  private void loadProjects(LoadMapProject[] mapProjectsConfig)
      throws Exception {
    try (MappingService mappingService = new MappingServiceJpa();
        ReportService reportService = new ReportServiceJpa()) {

      mappingService.setTransactionPerOperation(false);

      final List<ReportDefinition> existingReports = reportService
          .getReportDefinitions().getReportDefinitions();

      for (LoadMapProject mapProject : mapProjectsConfig) {
        Logger.getLogger(getClass()).info("Map Project json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(mapProject));

        MapProject project = new MapProjectJpa();
        project.setName(mapProject.getName());
        project
            .setDestinationTerminology(mapProject.getDestinationTerminology());
        project.setDestinationTerminologyVersion(
            mapProject.getDestinationTerminologyVersion());
        project.setGroupStructure(mapProject.getGroupStructure());

        if (mapProject.getMapRefsetPattern() != null)
          project.setMapRefsetPattern(
              MapRefsetPattern.valueOf(mapProject.getMapRefsetPattern()));

        project.setProjectSpecificAlgorithmHandlerClass(
            mapProject.getProjectSpecificAlgorithmHandlerClass());
        project.setPropagatedFlag(mapProject.getPropagatedFlag());
        project.setPublic(mapProject.getIsPublic());
        project.setTeamBased(mapProject.getIsTeamBased());
        project.setRefSetId(mapProject.getRefSetId());
        project.setRefSetName(mapProject.getRefSetName());
        project.setSourceTerminology(mapProject.getSourceTerminology());
        project.setSourceTerminologyVersion(
            mapProject.getSourceTerminologyVersion());

        if (mapProject.getWorkflowType() != null)
          project.setWorkflowType(
              WorkflowType.valueOf(mapProject.getWorkflowType()));

        if (mapProject.getMapRelationStyle() != null)
          project.setMapRelationStyle(
              RelationStyle.valueOf(mapProject.getMapRelationStyle()));

        project.setScopeDescendantsFlag(mapProject.getScopeDescendantsFlag());
        if (mapProject.getIncludeScopeConcepts() != null
            && !mapProject.getIncludeScopeConcepts().isEmpty()) {
          for (String concept : mapProject.getIncludeScopeConcepts()) {
            project.getScopeConcepts().add(concept);
          }
        }

        if (mapProject.getExcludeScopeConcepts() != null
            && !mapProject.getExcludeScopeConcepts().isEmpty()) {
          for (String concept : mapProject.getExcludeScopeConcepts()) {
            project.getScopeExcludedConcepts().add(concept);
          }
        }
        
        if (mapProject.getEditingCycleBeginDate() != null) {
          final SimpleDateFormat dateFormat = new SimpleDateFormat(
              mapProject.getDateFormat());
          project.setEditingCycleBeginDate(
              dateFormat.parse(mapProject.getEditingCycleBeginDate()));
        }

        mappingService.beginTransaction();

        MapUser mapUser;
        // add users to map project
        try (SecurityService securityService = new SecurityServiceJpa()) {
          for (String username : mapProject.getLeads()) {
            mapUser = securityService.getMapUser(username);
            if (mapUser != null) {
              project.getMapLeads().add(mapUser);
            } else {
              Logger.getLogger(getClass())
                  .warn(String.format(
                      "Username %s does not exist. Cannot add as lead to project.",
                      username));
            }
          }
          mapUser = null;
          for (String username : mapProject.getSpecialists()) {
            mapUser = securityService.getMapUser(username);
            if (mapUser != null) {
              project.getMapSpecialists().add(mapUser);
            } else {
              Logger.getLogger(getClass())
                  .warn(String.format(
                      "Username %s does not exist. Cannot add as specialist to project.",
                      username));
            }
          }
        }

        for (String reportName : mapProject.getReports()) {
          for (ReportDefinition rd : existingReports) {
            if (rd.getName().equals(reportName)) {
              Logger.getLogger(getClass()).info("adding report to project: "
                  + reportName + " to " + project.getName());
              project.getReportDefinitions().add(rd);
              break;
            }
          }
        }
        
        for (String errorMessage : mapProject.getErrorMessages()) {
          project.getErrorMessages().add(errorMessage);
        }

        mappingService.addMapProject(project);
        mappingService.commit();
      }
    }
  }

  // Principles
  private void loadPrinciples(LoadPrinciple[] principlesConfig)
      throws Exception {
    try (MappingService mappingService = new MappingServiceJpa();) {
      Logger.getLogger(getClass()).info("  loading PrincipleConfigFile");

      for (LoadPrinciple principleDef : principlesConfig) {
        Logger.getLogger(getClass()).info("MapPrinciple json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(principleDef));

        MapPrinciple principle = new MapPrincipleJpa();
        principle.setName(principleDef.getName());
        principle.setDetail(principleDef.getDetail());
        principle.setSectionRef(principleDef.getSectionRef());
        principle.setPrincipleId(principleDef.getPrinicpleId());

        mappingService.getMapPrinciples().addMapPrinciple(principle);
      }
    }
  }

  // Reports
  private void loadReports(LoadReportDefinition[] reportsConfig)
      throws Exception {
    try (ReportService reportService = new ReportServiceJpa()) {
      reportService.setTransactionPerOperation(false);

      Logger.getLogger(getClass()).info("  loading ReportDefinitionConfig");

      for (LoadReportDefinition reportDef : reportsConfig) {
        Logger.getLogger(getClass()).info("ReportDefinition json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(reportDef));

        ReportDefinition report = new ReportDefinitionJpa();
        report.setDescription(reportDef.getDescription());
        report.setDiffReportDefinitionName(
            reportDef.getDiffReportDefinitionName());

        if (reportDef.getFrequency() != null)
          report
              .setFrequency(ReportFrequency.valueOf(reportDef.getFrequency()));

        report.setDiffReport(reportDef.getIsDiffReport());
        report.setQACheck(reportDef.getIsQACheck());
        report.setName(reportDef.getName());
        report.setQuery(reportDef.getQuery());

        if (reportDef.getQueryType() != null)
          report
              .setQueryType(ReportQueryType.valueOf(reportDef.getQueryType()));

        if (reportDef.getResultType() != null)
          report.setResultType(
              ReportResultType.valueOf(reportDef.getResultType()));

        if (reportDef.getRoleRequired() != null)
          report.setRoleRequired(
              MapUserRole.valueOf(reportDef.getRoleRequired()));

        if (reportDef.getTimePeriod() != null)
          report.setTimePeriod(
              ReportTimePeriod.valueOf(reportDef.getTimePeriod()));

        reportService.beginTransaction();
        reportService.addReportDefinition(report);
        reportService.commit();

      }
    }
  }

  // Users
  private void loadUsers(LoadUser[] usersConfig) throws Exception {
    try (SecurityService securityService = new SecurityServiceJpa()) {
      Logger.getLogger(getClass()).info("  loading UserDefinitionConfig");

      MapUserList userList = securityService.getMapUsers();
      
      for (LoadUser user : usersConfig) {
        // if does not exists, add, otherwise skip
        Logger.getLogger(getClass()).info("MapPrinciple json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(usersConfig));
        
        MapUser mapUser = securityService.getMapUser(user.getUsername());
        if (mapUser == null) {
          mapUser = new MapUserJpa();
          mapUser.setUserName(user.getUsername());
          mapUser.setName(user.getName());
          mapUser.setEmail(user.getEmail());
          mapUser.setTeam(user.getTeam());
          mapUser.setApplicationRole(
              MapUserRole.valueOf(user.getApplicationRole()));
          if (!userList.contains(mapUser)) {
            Logger.getLogger(getClass()).info("ADDING USER " + user.getUsername());
            securityService.addMapUser(mapUser);
          }
          else {
            Logger.getLogger(getClass()).info("SKIPPING user already exists: " + user.getUsername());
          }
        }
      }
    }
  }

  // Advice
  private void loadAdvice(LoadAdvice[] advicesConfig) throws Exception {
    try (MappingService mappingService = new MappingServiceJpa();) {
      Logger.getLogger(getClass()).info("  loading AdviceConfigFile");

      for (LoadAdvice adviceDef : advicesConfig) {
        Logger.getLogger(getClass()).info("MapAdvice json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(adviceDef));

        MapAdvice advice = new MapAdviceJpa();
        advice.setName(adviceDef.getName());
        advice.setDetail(adviceDef.getDetail());
        advice.setAllowableForNullTarget(adviceDef.isAllowableForNullTarget());
        advice.setComputed(adviceDef.isComputed());

        mappingService.getMapAdvices().addMapAdvice(advice);
      }
    }
  }

  // Relation
  private void loadRelation(LoadRelation[] relationsConfig) throws Exception {
    try (MappingService mappingService = new MappingServiceJpa();) {
      Logger.getLogger(getClass()).info("  loading RelationConfigFile");

      for (LoadRelation relationDef : relationsConfig) {
        Logger.getLogger(getClass()).info("MapRelation json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(relationDef));

        MapRelation relation = new MapRelationJpa();
        relation.setName(relationDef.getName());
        relation.setAbbreviation(relationDef.getAbbreviation());
        relation.setTerminologyId(relationDef.getTerminologyId());
        relation
            .setAllowableForNullTarget(relationDef.isAllowableForNullTarget());
        relation.setComputed(relationDef.isComputed());

        mappingService.getMapRelations().addMapRelation(relation);
      }
    }
  }

  // Age Range
  private void loadAgeRange(LoadAgeRange[] ageRangesConfig) throws Exception {
    try (MappingService mappingService = new MappingServiceJpa();) {
      Logger.getLogger(getClass()).info("  loading AgeRangeConfigFile");

      for (LoadAgeRange ageRangeDef : ageRangesConfig) {
        Logger.getLogger(getClass()).info("MapAgeRange json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(ageRangeDef));

        MapAgeRange ageRange = new MapAgeRangeJpa();
        ageRange.setName(ageRangeDef.getName());
        ageRange.setLowerInclusive(ageRangeDef.isLowerInclusive());
        ageRange.setLowerUnits(ageRangeDef.getLowerUnits());
        ageRange.setLowerValue(ageRangeDef.getLowerValue());
        ageRange.setUpperInclusive(ageRangeDef.isUpperInclusive());
        ageRange.setUpperUnits(ageRangeDef.getUpperUnits());
        ageRange.setUpperValue(ageRangeDef.getUpperValue());

        mappingService.getMapAgeRanges().addMapAgeRange(ageRange);
      }
    }
  }

  @SuppressWarnings("unused")
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class LoadMapProject {
    private String destinationTerminology;
    private String destinationTerminologyVersion;
    private Boolean groupStructure;
    private String mapRefsetPattern;
    private String name;
    private String projectSpecificAlgorithmHandlerClass;
    private Boolean propagatedFlag;
    private Boolean isPublic;
    private Boolean isTeamBased;
    private String refSetId;
    private String refSetName;
    private String sourceTerminology;
    private String sourceTerminologyVersion;
    private String workflowType;
    private String mapRelationStyle;
    private Boolean scopeDescendantsFlag;
    private String dateFormat;
    private String editingCycleBeginDate;
    private List<String> includeScopeConcepts = new ArrayList<>();
    private List<String> excludeScopeConcepts = new ArrayList<>();
    private List<String> reports = new ArrayList<>();
    private List<String> leads = new ArrayList<>();
    private List<String> specialists = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public LoadMapProject() {
    }

    /**
     * 
     * @param dateFormat
     * @param destinationTerminology
     * @param destinationTerminologyVersion
     * @param editingCycleBeginDate
     * @param groupStructure
     * @param mapRefsetPattern
     * @param name
     * @param projectSpecificAlgorithmHandlerClass
     * @param propagatedFlag
     * @param isPublic
     * @param isTeamBased
     * @param refSetId
     * @param refSetName
     * @param sourceTerminology
     * @param sourceTerminologyVersion
     * @param workflowType
     * @param mapRelationStyle
     * @param scopeDescendantsFlag
     * @param scopeConcepts
     * @param reports
     * @param leads
     * @param specialists
     */
    @JsonCreator
    public LoadMapProject(@JsonProperty("dateFormat") String dateFormat,
        @JsonProperty("destinationTerminology") String destinationTerminology,
        @JsonProperty("destinationTerminologyVersion") String destinationTerminologyVersion,
        @JsonProperty("editingCycleBeginDate") String editingCycleBeginDate,
        @JsonProperty("groupStructure") Boolean groupStructure,
        @JsonProperty("mapRefsetPattern") String mapRefsetPattern,
        @JsonProperty("name") String name,
        @JsonProperty("projectSpecificAlgorithmHandlerClass") String projectSpecificAlgorithmHandlerClass,
        @JsonProperty("propagatedFlag") Boolean propagatedFlag,
        @JsonProperty("isPublic") Boolean isPublic,
        @JsonProperty("isTeamBased") Boolean isTeamBased,
        @JsonProperty("refSetId") String refSetId,
        @JsonProperty("refSetName") String refSetName,
        @JsonProperty("sourceTerminology") String sourceTerminology,
        @JsonProperty("sourceTerminologyVersion") String sourceTerminologyVersion,
        @JsonProperty("workflowType") String workflowType,
        @JsonProperty("mapRelationStyle") String mapRelationStyle,
        @JsonProperty("scopeDescendantsFlag") Boolean scopeDescendantsFlag,
        @JsonProperty("includeScopeConcepts") List<String> includeScopeConcepts,
        @JsonProperty("excludeScopeConcepts") List<String> excludeScopeConcepts,
        @JsonProperty("reports") List<String> reports,
        @JsonProperty("leads") List<String> leads,
        @JsonProperty("specialists") List<String> specialists,
        @JsonProperty("errorMessages") List<String> errorMessages) {
      super();
      this.dateFormat = dateFormat;
      this.destinationTerminology = destinationTerminology;
      this.destinationTerminologyVersion = destinationTerminologyVersion;
      this.editingCycleBeginDate = editingCycleBeginDate;
      this.groupStructure = groupStructure;
      this.mapRefsetPattern = mapRefsetPattern;
      this.name = name;
      this.projectSpecificAlgorithmHandlerClass = projectSpecificAlgorithmHandlerClass;
      this.propagatedFlag = propagatedFlag;
      this.isPublic = isPublic;
      this.isTeamBased = isTeamBased;
      this.refSetId = refSetId;
      this.refSetName = refSetName;
      this.sourceTerminology = sourceTerminology;
      this.sourceTerminologyVersion = sourceTerminologyVersion;
      this.workflowType = workflowType;
      this.mapRelationStyle = mapRelationStyle;
      this.scopeDescendantsFlag = scopeDescendantsFlag;
      this.includeScopeConcepts = includeScopeConcepts;
      this.excludeScopeConcepts = excludeScopeConcepts;
      this.reports = reports;
      this.leads = leads;
      this.specialists = specialists;
      this.errorMessages = errorMessages;
    }

    public String getDateFormat() {
      return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
      this.dateFormat = dateFormat;
    }

    public String getDestinationTerminology() {
      return destinationTerminology;
    }

    public void setDestinationTerminology(String destinationTerminology) {
      this.destinationTerminology = destinationTerminology;
    }

    public String getDestinationTerminologyVersion() {
      return destinationTerminologyVersion;
    }

    public void setDestinationTerminologyVersion(
        String destinationTerminologyVersion) {
      this.destinationTerminologyVersion = destinationTerminologyVersion;
    }

    public String getEditingCycleBeginDate() {
      return editingCycleBeginDate;
    }

    public void setEditingCycleBeginDate(String editingCycleBeginDate) {
      this.editingCycleBeginDate = editingCycleBeginDate;
    }

    public Boolean getGroupStructure() {
      return groupStructure;
    }

    public void setGroupStructure(Boolean groupStructure) {
      this.groupStructure = groupStructure;
    }

    public String getMapRefsetPattern() {
      return mapRefsetPattern;
    }

    public void setMapRefsetPattern(String mapRefsetPattern) {
      this.mapRefsetPattern = mapRefsetPattern;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getProjectSpecificAlgorithmHandlerClass() {
      return projectSpecificAlgorithmHandlerClass;
    }

    public void setProjectSpecificAlgorithmHandlerClass(
        String projectSpecificAlgorithmHandlerClass) {
      this.projectSpecificAlgorithmHandlerClass = projectSpecificAlgorithmHandlerClass;
    }

    public Boolean getPropagatedFlag() {
      return propagatedFlag;
    }

    public void setPropagatedFlag(Boolean propagatedFlag) {
      this.propagatedFlag = propagatedFlag;
    }

    public Boolean getIsPublic() {
      return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
      this.isPublic = isPublic;
    }

    public Boolean getIsTeamBased() {
      return isTeamBased;
    }

    public void setIsTeamBased(Boolean isTeamBased) {
      this.isTeamBased = isTeamBased;
    }

    public String getRefSetId() {
      return refSetId;
    }

    public void setRefSetId(String refSetId) {
      this.refSetId = refSetId;
    }

    public String getRefSetName() {
      return refSetName;
    }

    public void setRefSetName(String refSetName) {
      this.refSetName = refSetName;
    }

    public String getSourceTerminology() {
      return sourceTerminology;
    }

    public void setSourceTerminology(String sourceTerminology) {
      this.sourceTerminology = sourceTerminology;
    }

    public String getSourceTerminologyVersion() {
      return sourceTerminologyVersion;
    }

    public void setSourceTerminologyVersion(String sourceTerminologyVersion) {
      this.sourceTerminologyVersion = sourceTerminologyVersion;
    }

    public String getWorkflowType() {
      return workflowType;
    }

    public void setWorkflowType(String workflowType) {
      this.workflowType = workflowType;
    }

    public String getMapRelationStyle() {
      return mapRelationStyle;
    }

    public void setMapRelationStyle(String mapRelationStyle) {
      this.mapRelationStyle = mapRelationStyle;
    }

    public Boolean getScopeDescendantsFlag() {
      return scopeDescendantsFlag;
    }

    public void setScopeDescendantsFlag(Boolean scopeDescendantsFlag) {
      this.scopeDescendantsFlag = scopeDescendantsFlag;
    }

    public List<String> getIncludeScopeConcepts() {
      return includeScopeConcepts;
    }

    public void setIncludeScopeConcepts(List<String> includeScopeConcepts) {
      this.includeScopeConcepts = includeScopeConcepts;
    }
    
    public List<String> getExcludeScopeConcepts() {
      return excludeScopeConcepts;
    }

    public void setExcludeScopeConcepts(List<String> excludeScopeConcepts) {
      this.excludeScopeConcepts = excludeScopeConcepts;
    }

    public List<String> getReports() {
      return reports;
    }

    public void setReports(List<String> reports) {
      this.reports = reports;
    }

    public List<String> getLeads() {
      return leads;
    }

    public void setLeads(List<String> leads) {
      this.leads = leads;
    }

    public List<String> getSpecialists() {
      return specialists;
    }

    public void setSpecialists(List<String> specialists) {
      this.specialists = specialists;
    }
    
    public List<String> getErrorMessages() {
      return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
      this.errorMessages = errorMessages;
    }

  }

  @SuppressWarnings("unused")
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class LoadReportDefinition {
    private String description;
    private String diffReportDefinitionName;
    private String frequency;
    private Boolean isDiffReport;
    private Boolean isQACheck;
    private String name;
    private String query;
    private String queryType;
    private String resultType;
    private String roleRequired;
    private String timePeriod;

    /**
     * Default constructor.
     */
    public LoadReportDefinition() {
    }

    /**
     * 
     * @param description
     * @param diffReportDefinitionName
     * @param frequency
     * @param isDiffReport
     * @param isQACheck
     * @param name
     * @param query
     * @param queryType
     * @param resultType
     * @param roleRequired
     * @param timePeriod
     */
    @JsonCreator
    public LoadReportDefinition(@JsonProperty("description") String description,
        @JsonProperty("diffReportDefinitionName") String diffReportDefinitionName,
        @JsonProperty("frequency") String frequency,
        @JsonProperty("isDiffReport") Boolean isDiffReport,
        @JsonProperty("isQACheck") Boolean isQACheck,
        @JsonProperty("name") String name, @JsonProperty("query") String query,
        @JsonProperty("queryType") String queryType,
        @JsonProperty("resultType") String resultType,
        @JsonProperty("roleRequired") String roleRequired,
        @JsonProperty("timePeriod") String timePeriod) {
      super();
      this.description = description;
      this.diffReportDefinitionName = diffReportDefinitionName;
      this.frequency = frequency;
      this.isDiffReport = isDiffReport;
      this.isQACheck = isQACheck;
      this.name = name;
      this.query = query;
      this.queryType = queryType;
      this.resultType = resultType;
      this.roleRequired = roleRequired;
      this.timePeriod = timePeriod;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getDiffReportDefinitionName() {
      return diffReportDefinitionName;
    }

    public void setDiffReportDefinitionName(String diffReportDefinitionName) {
      this.diffReportDefinitionName = diffReportDefinitionName;
    }

    public String getFrequency() {
      return frequency;
    }

    public void setFrequency(String frequency) {
      this.frequency = frequency;
    }

    public Boolean getIsDiffReport() {
      return isDiffReport;
    }

    public void setIsDiffReport(Boolean isDiffReport) {
      this.isDiffReport = isDiffReport;
    }

    public Boolean getIsQACheck() {
      return isQACheck;
    }

    public void setIsQACheck(Boolean isQACheck) {
      this.isQACheck = isQACheck;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getQuery() {
      return query;
    }

    public void setQuery(String query) {
      this.query = query;
    }

    public String getQueryType() {
      return queryType;
    }

    public void setQueryType(String queryType) {
      this.queryType = queryType;
    }

    public String getResultType() {
      return resultType;
    }

    public void setResultType(String resultType) {
      this.resultType = resultType;
    }

    public String getRoleRequired() {
      return roleRequired;
    }

    public void setRoleRequired(String roleRequired) {
      this.roleRequired = roleRequired;
    }

    public String getTimePeriod() {
      return timePeriod;
    }

    public void setTimePeriod(String timePeriod) {
      this.timePeriod = timePeriod;
    }

  }

  @SuppressWarnings("unused")
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class LoadUser {
    private String username;
    private String name;
    private String email;
    private String applicationRole;
    private String team;

    /**
     * No args constructor for use in serialization
     * 
     */
    public LoadUser() {
    }

    /**
     * 
     * @param username
     * @param email
     * @param name
     * @param applicationRole
     */
    @JsonCreator
    public LoadUser(@JsonProperty("username") String username,
        @JsonProperty("name") String name, @JsonProperty("email") String email,
        @JsonProperty("applicationRole") String applicationRole,
        @JsonProperty("team") String team) {
      super();
      this.username = username;
      this.name = name;
      this.email = email;
      this.applicationRole = applicationRole;
      this.team = team;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public String getApplicationRole() {
      return applicationRole;
    }

    public void setApplicationRole(String applicationRole) {
      this.applicationRole = applicationRole;
    }

    public String getTeam() {
      return team;
    }

    public void setTeam(String team) {
      this.team = team;
    }

  }

  @SuppressWarnings("unused")
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class LoadPrinciple {
    private String name;
    private String detail;
    private String sectionRef;
    private String prinicpleId;

    public LoadPrinciple() {
    }

    /**
     * 
     * @param name
     * @param detail
     * @param sectionRef
     * @param principleId
     */
    @JsonCreator
    public LoadPrinciple(@JsonProperty("name") String name,
        @JsonProperty("detail") String detail,
        @JsonProperty("sectionRef") String sectionRef,
        @JsonProperty("principleId") String principleId) {
      super();
      this.name = name;
      this.detail = detail;
      this.sectionRef = sectionRef;
      this.prinicpleId = principleId;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDetail() {
      return detail;
    }

    public void setDetail(String detail) {
      this.detail = detail;
    }

    public String getSectionRef() {
      return sectionRef;
    }

    public void setSectionRef(String sectionRef) {
      this.sectionRef = sectionRef;
    }

    public String getPrinicpleId() {
      return prinicpleId;
    }

    public void setPrinicpleId(String prinicpleId) {
      this.prinicpleId = prinicpleId;
    }

  }

  @SuppressWarnings("unused")
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class LoadAdvice {
    private String name;
    private String detail;
    private boolean isAllowableForNullTarget;
    private boolean isComputed;

    public LoadAdvice() {
    }

    public LoadAdvice(@JsonProperty("name") String name,
        @JsonProperty("detail") String detail,
        @JsonProperty("isAllowableForNullTarget") boolean isAllowableForNullTarget,
        @JsonProperty("isComputed") boolean isComputed) {

    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDetail() {
      return detail;
    }

    public void setDetail(String detail) {
      this.detail = detail;
    }

    public boolean isAllowableForNullTarget() {
      return isAllowableForNullTarget;
    }

    public void setAllowableForNullTarget(boolean isAllowableForNullTarget) {
      this.isAllowableForNullTarget = isAllowableForNullTarget;
    }

    public boolean isComputed() {
      return isComputed;
    }

    public void setComputed(boolean isComputed) {
      this.isComputed = isComputed;
    }
  }

  @SuppressWarnings("unused")
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class LoadRelation {
    private String name;
    private String abbreviation;
    private String terminologyId;
    private boolean isAllowableForNullTarget;
    private boolean isComputed;

    public LoadRelation() {
    }

    public LoadRelation(@JsonProperty("name") String name,
        @JsonProperty("abbreviation") String abbreviation,
        @JsonProperty("terminologyId") String terminologyId,
        @JsonProperty("isAllowableForNullTarget") boolean isAllowableForNullTarget,
        @JsonProperty("isComputed") boolean isComputed) {
      this.name = name;
      this.abbreviation = abbreviation;
      this.terminologyId = terminologyId;
      this.isAllowableForNullTarget = isAllowableForNullTarget;
      this.isComputed = isComputed;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getAbbreviation() {
      return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
      this.abbreviation = abbreviation;
    }

    public String getTerminologyId() {
      return terminologyId;
    }

    public void setTerminologyId(String terminologyId) {
      this.terminologyId = terminologyId;
    }

    public boolean isAllowableForNullTarget() {
      return isAllowableForNullTarget;
    }

    public void setAllowableForNullTarget(boolean isAllowableForNullTarget) {
      this.isAllowableForNullTarget = isAllowableForNullTarget;
    }

    public boolean isComputed() {
      return isComputed;
    }

    public void setComputed(boolean isComputed) {
      this.isComputed = isComputed;
    }

  }

  @SuppressWarnings("unused")
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class LoadAgeRange {
    private String name;
    private boolean lowerInclusive;
    private String lowerUnits;
    private Integer lowerValue;
    private boolean upperInclusive;
    private String upperUnits;
    private Integer upperValue;

    public LoadAgeRange() {
    }

    public LoadAgeRange(@JsonProperty("name") String name,
        @JsonProperty("lowerInclusive") boolean lowerInclusive,
        @JsonProperty("lowerUnits") String lowerUnits,
        @JsonProperty("lowerValue") Integer lowerValue,
        @JsonProperty("upperInclusive") boolean upperInclusive,
        @JsonProperty("upperUnits") String upperUnits,
        @JsonProperty("upperValue") Integer upperValue) {
      this.name = name;
      this.lowerInclusive = lowerInclusive;
      this.lowerUnits = lowerUnits;
      this.lowerValue = lowerValue;
      this.upperInclusive = upperInclusive;
      this.upperUnits = upperUnits;
      this.upperValue = upperValue;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public boolean isLowerInclusive() {
      return lowerInclusive;
    }

    public void setLowerInclusive(boolean lowerInclusive) {
      this.lowerInclusive = lowerInclusive;
    }

    public String getLowerUnits() {
      return lowerUnits;
    }

    public void setLowerUnits(String lowerUnits) {
      this.lowerUnits = lowerUnits;
    }

    public Integer getLowerValue() {
      return lowerValue;
    }

    public void setLowerValue(Integer lowerValue) {
      this.lowerValue = lowerValue;
    }

    public boolean isUpperInclusive() {
      return upperInclusive;
    }

    public void setUpperInclusive(boolean upperInclusive) {
      this.upperInclusive = upperInclusive;
    }

    public String getUpperUnits() {
      return upperUnits;
    }

    public void setUpperUnits(String upperUnits) {
      this.upperUnits = upperUnits;
    }

    public Integer getUpperValue() {
      return upperValue;
    }

    public void setUpperValue(Integer upperValue) {
      this.upperValue = upperValue;
    }

  }
}
