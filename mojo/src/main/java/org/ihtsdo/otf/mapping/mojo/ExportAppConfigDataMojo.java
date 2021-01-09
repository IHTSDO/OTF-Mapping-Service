/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAgeRangeList;
import org.ihtsdo.otf.mapping.helpers.MapPrincipleList;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.helpers.MapRelationList;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.mojo.ConfigModels.MapProjectConfiguration;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mojo for exporting map project configuration data.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal export-app-config-data
 */
public class ExportAppConfigDataMojo extends AbstractOtfMappingMojo {

  /**
   * File path root for config files
   * 
   * @parameter
   */
  private String configFileRoot = null;

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

  private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Load App Config Data Started");

    setupBindInfoPackage();
    
    try {

      exportConfigData();
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
   * @throws Exception the exception
   */
  private void exportConfigData() throws Exception {

    List<MapAdvice> advices = exportAdvice();
    writeConfigToFile(adviceConfigFile, advices);

    List<MapAgeRange> ageRanges = exportAgeRange();
    writeConfigToFile(ageRangeConfigFile, ageRanges);

    List<MapPrinciple> principles = exportPrinciples();
    writeConfigToFile(principleConfigFile, principles);

    List<MapProjectConfiguration> projects = exportProjects();
    writeConfigToFile(projectConfigFile, projects);

    List<MapRelation> relations = exportRelation();
    writeConfigToFile(relationConfigFile, relations);

    List<ReportDefinition> reports = exportReports();
    writeConfigToFile(reportConfigFile, reports);

    List<MapUser> users = exportUsers();
    writeConfigToFile(userConfigFile, users);

  }

  // Mapping Projects
  // Not using MapProjectJPA since it includes the full details of MapUsers
  // and other related objects which are generated in other methods.
  // Here we only want the username or name for association
  private List<MapProjectConfiguration> exportProjects() throws Exception {

    List<MapProjectConfiguration> mapProjectConfigurations = new ArrayList<>();

    try (MappingService mappingService = new MappingServiceJpa()) {
      getLog().info(" exporting project config");

      MapProjectList mpl = mappingService.getMapProjects();

      for (MapProject mp : mpl.getMapProjects()) {
        getLog().info("  project : " + mp.getName());

        MapProjectConfiguration lmp = new MapProjectConfiguration();
        lmp.setDateFormat("yyyyMMdd");
        lmp.setDestinationTerminology(mp.getDestinationTerminology());
        lmp.setDestinationTerminologyVersion(
            mp.getDestinationTerminologyVersion());
        lmp.setEditingCycleBeginDate(
            dateFormat.format(mp.getEditingCycleBeginDate()));
        lmp.setErrorMessages(mp.getErrorMessages());
        lmp.setExcludeScopeConcepts(mp.getScopeExcludedConcepts());
        lmp.setGroupStructure(mp.isGroupStructure());
        lmp.setIsPublic(mp.isPublic());
        lmp.setIsRuleBased(mp.isRuleBased());
        lmp.setIsTeamBased(mp.isTeamBased());
        lmp.setMapRefsetPattern(mp.getMapRefsetPattern().name());
        lmp.setMapRelationStyle(mp.getMapRelationStyle().name());
        lmp.setName(mp.getName());
        lmp.setProjectSpecificAlgorithmHandlerClass(
            mp.getProjectSpecificAlgorithmHandlerClass());
        lmp.setPropagatedFlag(mp.isPropagatedFlag());
        lmp.setRefSetId(mp.getRefSetId());
        lmp.setRefSetName(mp.getRefSetName());
        lmp.setModuleId(mp.getModuleId());
        lmp.setScopeDescendantsFlag(mp.isScopeDescendantsFlag());

        lmp.setIncludeScopeConcepts(mp.getScopeConcepts());
        lmp.setExcludeScopeConcepts(mp.getScopeExcludedConcepts());

        lmp.setSourceTerminology(mp.getSourceTerminology());
        lmp.setSourceTerminologyVersion(mp.getSourceTerminologyVersion());

        lmp.setWorkflowType(mp.getWorkflowType().name());

        Set<String> specialists = new HashSet<>();
        for (MapUser specialist : mp.getMapSpecialists()) {
          specialists.add(specialist.getUserName());
        }
        lmp.setSpecialists(specialists);

        Set<String> leads = new HashSet<>();
        for (MapUser lead : mp.getMapLeads()) {
          leads.add(lead.getUserName());
        }
        lmp.setLeads(leads);

        Set<String> reports = new HashSet<>();
        for (ReportDefinition report : mp.getReportDefinitions()) {
          reports.add(report.getName());
        }
        lmp.setReports(reports);
        
        Set<String> advices = new HashSet<>();
        for (MapAdvice advice : mp.getMapAdvices()) {
          advices.add(advice.getName());
        }
        lmp.setAdvices(advices);
       
        Set<String> relations = new HashSet<>();
        for (MapRelation relation : mp.getMapRelations()) {
          relations.add(relation.getName());
        }
        lmp.setRelations(relations);
       
        Set<String> ageRanges = new HashSet<>();
        for (MapAgeRange ageRange : mp.getPresetAgeRanges()) {
          ageRanges.add(ageRange.getName());
        }
        lmp.setAgeRanges(ageRanges);

        mapProjectConfigurations.add(lmp);
      }
    }
    return mapProjectConfigurations;
  }

  // Principles
  private List<MapPrinciple> exportPrinciples() throws Exception {

    final MapPrincipleList mapPrincipleList;
    try (MappingService mappingService = new MappingServiceJpa();) {
      getLog().info(" exporting principle config");
      mapPrincipleList = mappingService.getMapPrinciples();
    }
    return mapPrincipleList.getMapPrinciples();
  }

  // Reports and QA Checks
  private List<ReportDefinition> exportReports() throws Exception {

    final List<ReportDefinition> reportDefinitions = new ArrayList<>();

    try (ReportService reportService = new ReportServiceJpa()) {
      getLog().info(" exporting report definition config");
      reportDefinitions.addAll(reportService.getReportDefinitions().getReportDefinitions());
      reportDefinitions.addAll(reportService.getQACheckDefinitions().getReportDefinitions());
    }
    
    return reportDefinitions;
  }

  // Users
  private List<MapUser> exportUsers() throws Exception {

    final MapUserList userList;
    try (SecurityService securityService = new SecurityServiceJpa()) {
      getLog().info(" exporting user config");
      userList = securityService.getMapUsers();
    }
    return userList.getMapUsers();
  }

  // Advice
  private List<MapAdvice> exportAdvice() throws Exception {

    final MapAdviceList adviceList;
    try (MappingService mappingService = new MappingServiceJpa();) {
      getLog().info(" exporting advice config");
      adviceList = mappingService.getMapAdvices();
    }
    return adviceList.getMapAdvices();
  }

  // Relation
  private List<MapRelation> exportRelation() throws Exception {

    final MapRelationList relationList;
    try (MappingService mappingService = new MappingServiceJpa();) {
      getLog().info(" exporting relation config");

      relationList = mappingService.getMapRelations();
    }
    return relationList.getMapRelations();
  }

  // Age Range
  private List<MapAgeRange> exportAgeRange() throws Exception {

    final MapAgeRangeList ageRangeList;
    try (MappingService mappingService = new MappingServiceJpa();) {
      getLog().info(" exporting age range config");
      ageRangeList = mappingService.getMapAgeRanges();
    }
    return ageRangeList.getMapAgeRanges();
  }

  private void writeConfigToFile(String fullFileName, Object object)
    throws Exception {

    ObjectMapper mapper = new ObjectMapper();
    try {
      getLog().info("Write config to file : " + configFileRoot + fullFileName);
      mapper.writerWithDefaultPrettyPrinter()
          .writeValue(new File(configFileRoot + fullFileName), object);
    } catch (Exception e) {
      getLog().error("ERROR:" + e.getMessage(), e);
      throw e;
    }

  }
}
