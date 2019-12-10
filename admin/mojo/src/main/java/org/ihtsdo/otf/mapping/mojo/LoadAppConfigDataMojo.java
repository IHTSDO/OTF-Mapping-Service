/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionList;
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
import org.ihtsdo.otf.mapping.mojo.ConfigModels.MapProjectConfiguration;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mojo for generating loading map project configuration data into a new
 * instance.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-app-config-data
 */
public class LoadAppConfigDataMojo extends AbstractOtfMappingMojo {

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

  private ObjectMapper mapper = new ObjectMapper();

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Load App Config Data Started");

    try {
      setupBindInfoPackage();

      loadConfigData();
      getLog().info("Load App Config Data Finished");

    } catch (Exception e) {
      getLog().error(e);
      throw new MojoExecutionException(
          "LoadAppConfigData mojo failed to complete", e);
    }

  }

  /**
   * Load configuration data.
   *
   * @throws Exception the exception
   */
  private void loadConfigData() throws Exception {

    if (projectConfigFile == null)
      throw new IllegalArgumentException(
          "Mapping Project Configuration file not specified.  Please check configuration file.");

    if (!Files.exists(Paths.get(configFileRoot + projectConfigFile)))
      throw new FileNotFoundException(String.format(
          "Mapping Project Configuration file does not exist.  Please check file path %s.",
          projectConfigFile));

    if (reportConfigFile == null)
      getLog().info("ReportConfigFile is not provided.  Not loading reports.");
    else if (!Files.exists(Paths.get(configFileRoot + reportConfigFile)))
      throw new FileNotFoundException(String.format(
          "Report Configuration file does not exist.  Please check file path %s.",
          configFileRoot + reportConfigFile));

    // verify all files can be parsed before calling load
    if (userConfigFile != null
        && Files.exists(Paths.get(configFileRoot + userConfigFile))) {
      getLog().info("  loading " + configFileRoot + userConfigFile);
      MapUser[] usersConfig = mapper.readValue(
          new File(configFileRoot + userConfigFile), MapUserJpa[].class);
      loadUsers(usersConfig);
    }

    if (principleConfigFile != null
        && Files.exists(Paths.get(configFileRoot + principleConfigFile))) {
      getLog().info("  loading " + configFileRoot + principleConfigFile);
      MapPrinciple[] principles =
          mapper.readValue(new File(configFileRoot + principleConfigFile),
              MapPrincipleJpa[].class);
      loadPrinciples(principles);
    }

    if (adviceConfigFile != null
        && Files.exists(Paths.get(configFileRoot + adviceConfigFile))) {
      getLog().info("  loading " + configFileRoot + adviceConfigFile);
      MapAdvice[] advices = mapper.readValue(
          new File(configFileRoot + adviceConfigFile), MapAdviceJpa[].class);
      loadAdvice(advices);
    }

    if (ageRangeConfigFile != null
        && Files.exists(Paths.get(configFileRoot + ageRangeConfigFile))) {
      getLog().info("  loading " + configFileRoot + ageRangeConfigFile);
      MapAgeRange[] ageRanges =
          mapper.readValue(new File(configFileRoot + ageRangeConfigFile),
              MapAgeRangeJpa[].class);
      loadAgeRange(ageRanges);
    }

    if (relationConfigFile != null
        && Files.exists(Paths.get(configFileRoot + relationConfigFile))) {
      getLog().info("  loading " + configFileRoot + relationConfigFile);
      MapRelation[] relations =
          mapper.readValue(new File(configFileRoot + relationConfigFile),
              MapRelationJpa[].class);
      loadRelation(relations);
    }

    if (reportConfigFile != null
        && Files.exists(Paths.get(configFileRoot + reportConfigFile))) {
      getLog().info("  loading " + configFileRoot + reportConfigFile);
      ReportDefinition[] reportsConfig =
          mapper.readValue(new File(configFileRoot + reportConfigFile),
              ReportDefinitionJpa[].class);
      loadReports(reportsConfig);
    }

    if (projectConfigFile != null
        && Files.exists(Paths.get(configFileRoot + projectConfigFile))) {
      getLog().info(" loading " + configFileRoot + projectConfigFile);
      MapProjectConfiguration[] mapProjectsConfig =
          mapper.readValue(new File(configFileRoot + projectConfigFile),
              MapProjectConfiguration[].class);
      loadProjects(mapProjectsConfig);
    }

  }

  // Mapping Projects
  private void loadProjects(MapProjectConfiguration[] mapProjectsConfig)
    throws Exception {

    try (MappingService mappingService = new MappingServiceJpa();
        ReportService reportService = new ReportServiceJpa()) {

      mappingService.setTransactionPerOperation(false);

      final ReportDefinitionList existingReports =
          reportService.getReportDefinitions();
      final List<MapProject> mapProjects =
          mappingService.getMapProjects().getMapProjects();

      for (MapProjectConfiguration mapProject : mapProjectsConfig) {
        getLog().info("Map Project json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(mapProject));

        if (!containsMapProject(mapProjects, mapProject.getName())) {
          MapProject project = new MapProjectJpa();
          project.setName(mapProject.getName());
          project.setDestinationTerminology(
              mapProject.getDestinationTerminology());
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
            final SimpleDateFormat dateFormat =
                new SimpleDateFormat(mapProject.getDateFormat());
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
                getLog().warn(String.format(
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
                getLog().warn(String.format(
                    "Username %s does not exist. Cannot add as specialist to project.",
                    username));
              }
            }
          }

          for (String reportName : mapProject.getReports()) {
            for (ReportDefinition rd : existingReports.getReportDefinitions()) {
              if (rd.getName().equals(reportName)) {
                getLog().info("adding report to project: " + reportName + " to "
                    + project.getName());
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

        } else {
          getLog().info(
              " Map Project '" + mapProject.getName() + "' already exists.");
        }
      }
    }
  }

  // Principles
  private void loadPrinciples(MapPrinciple[] principles) throws Exception {
    try (MappingService mappingService = new MappingServiceJpa();) {
      getLog().info("  loading PrincipleConfigFile");

      List<MapPrinciple> mapPrinciples =
          mappingService.getMapPrinciples().getMapPrinciples();

      for (MapPrinciple principleDef : principles) {
        getLog().info("MapPrinciple json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(principleDef));
        if (!containsMapPrinciple(mapPrinciples, principleDef.getName())) {
          MapPrinciple principle = new MapPrincipleJpa();
          principle.setName(principleDef.getName());
          principle.setDetail(principleDef.getDetail());
          principle.setSectionRef(principleDef.getSectionRef());
          principle.setPrincipleId(principleDef.getPrincipleId());

          mappingService.getMapPrinciples().addMapPrinciple(principle);
        } else {
          getLog().info(" Map Principle '" + principleDef.getName()
              + "' already exists.");
        }
      }
    }
  }

  // Reports
  private void loadReports(ReportDefinition[] reports) throws Exception {
    try (ReportService reportService = new ReportServiceJpa()) {
      reportService.setTransactionPerOperation(false);

      getLog().info("  loading ReportDefinitionConfig");

      List<ReportDefinition> reportDefinitions =
          reportService.getReportDefinitions().getReportDefinitions();

      for (ReportDefinition reportDef : reports) {
        getLog().info("ReportDefinition json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(reportDef));
        if (!containsReportDefintion(reportDefinitions, reportDef.getName())) {
          ReportDefinition report = new ReportDefinitionJpa();
          report.setDescription(reportDef.getDescription());
          report.setDiffReportDefinitionName(
              reportDef.getDiffReportDefinitionName());

          if (reportDef.getFrequency() != null)
            report.setFrequency(
                ReportFrequency.valueOf(reportDef.getFrequency().name()));

          report.setDiffReport(reportDef.isDiffReport());
          report.setQACheck(reportDef.isQACheck());
          report.setName(reportDef.getName());
          report.setQuery(reportDef.getQuery());

          if (reportDef.getQueryType() != null)
            report.setQueryType(
                ReportQueryType.valueOf(reportDef.getQueryType().name()));

          if (reportDef.getResultType() != null)
            report.setResultType(
                ReportResultType.valueOf(reportDef.getResultType().name()));

          if (reportDef.getRoleRequired() != null)
            report.setRoleRequired(
                MapUserRole.valueOf(reportDef.getRoleRequired().name()));

          if (reportDef.getTimePeriod() != null)
            report.setTimePeriod(
                ReportTimePeriod.valueOf(reportDef.getTimePeriod().name()));

          reportService.beginTransaction();
          reportService.addReportDefinition(report);
          reportService.commit();
        } else {
          getLog()
              .info(" Report '" + reportDef.getName() + "' already exists.");
        }
      }
    }
  }

  // Users
  private void loadUsers(MapUser[] users) throws Exception {
    try (SecurityService securityService = new SecurityServiceJpa()) {
      getLog().info("  loading UserDefinitionConfig");

      for (MapUser userDef : users) {
        // if does not exists, add, otherwise skip
        MapUser mapUser = securityService.getMapUser(userDef.getUserName());

        if (mapUser == null) {
          mapUser = new MapUserJpa();
          mapUser.setUserName(userDef.getUserName());
          mapUser.setName(userDef.getName());
          mapUser.setEmail(userDef.getEmail());
          mapUser.setTeam(userDef.getTeam());
          mapUser.setApplicationRole(
              MapUserRole.valueOf(userDef.getApplicationRole().name()));

          getLog().info("ADDING USER " + userDef.getUserName());
          securityService.addMapUser(mapUser);

        } else {
          getLog()
              .info("SKIPPING user already exists: " + userDef.getUserName());
        }
      }
    }
  }

  // Advice
  private void loadAdvice(MapAdvice[] advices) throws Exception {
    try (MappingService mappingService = new MappingServiceJpa();) {
      getLog().info("  loading AdviceConfigFile");

      List<MapAdvice> mapAdvices =
          mappingService.getMapAdvices().getMapAdvices();

      for (MapAdvice adviceDef : advices) {
        getLog().info("MapAdvice json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(adviceDef));
        if (!containsMapAdvice(mapAdvices, adviceDef.getName())) {
          MapAdvice advice = new MapAdviceJpa();
          advice.setName(adviceDef.getName());
          advice.setDetail(adviceDef.getDetail());
          advice
              .setAllowableForNullTarget(adviceDef.isAllowableForNullTarget());
          advice.setComputed(adviceDef.isComputed());

          mappingService.getMapAdvices().addMapAdvice(advice);
        } else {
          getLog().info(
              " Map Advice '" + adviceDef.getName() + "' already exists.");
        }
      }
    }
  }

  // Relation
  private void loadRelation(MapRelation[] relations) throws Exception {
    try (MappingService mappingService = new MappingServiceJpa();) {
      getLog().info("  loading RelationConfigFile");

      List<MapRelation> mapRelations =
          mappingService.getMapRelations().getMapRelations();

      for (MapRelation relationDef : relations) {
        getLog().info("MapRelation json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(relationDef));
        if (!containsMapRelation(mapRelations, relationDef.getName())) {
          MapRelation relation = new MapRelationJpa();
          relation.setName(relationDef.getName());
          relation.setAbbreviation(relationDef.getAbbreviation());
          relation.setTerminologyId(relationDef.getTerminologyId());
          relation.setAllowableForNullTarget(
              relationDef.isAllowableForNullTarget());
          relation.setComputed(relationDef.isComputed());

          mappingService.getMapRelations().addMapRelation(relation);
        } else {
          getLog().info(
              " Map Relation '" + relationDef.getName() + "' already exists.");
        }
      }
    }
  }

  // Age Range
  private void loadAgeRange(MapAgeRange[] ageRanges) throws Exception {
    try (MappingService mappingService = new MappingServiceJpa();) {
      getLog().info("  loading AgeRangeConfigFile");

      List<MapAgeRange> mapAgeRanges =
          mappingService.getMapAgeRanges().getMapAgeRanges();

      for (MapAgeRange ageRangeDef : ageRanges) {
        getLog().info("MapAgeRange json:" + mapper
            .writerWithDefaultPrettyPrinter().writeValueAsString(ageRangeDef));
        if (!containsMapAgeRange(mapAgeRanges, ageRangeDef.getName())) {
          MapAgeRange ageRange = new MapAgeRangeJpa();
          ageRange.setName(ageRangeDef.getName());
          ageRange.setLowerInclusive(ageRangeDef.getLowerInclusive());
          ageRange.setLowerUnits(ageRangeDef.getLowerUnits());
          ageRange.setLowerValue(ageRangeDef.getLowerValue());
          ageRange.setUpperInclusive(ageRangeDef.getUpperInclusive());
          ageRange.setUpperUnits(ageRangeDef.getUpperUnits());
          ageRange.setUpperValue(ageRangeDef.getUpperValue());

          mappingService.getMapAgeRanges().addMapAgeRange(ageRange);
        } else {
          getLog().info(
              " Map AgeRange '" + ageRangeDef.getName() + "' already exists.");
        }
      }
    }
  }

  private boolean containsMapPrinciple(List<MapPrinciple> mapPrinciples,
    String mapPrincipleName) {
    if (mapPrinciples == null || mapPrincipleName == null) {
      return false;
    } else {
      for (MapPrinciple mapPrinciple : mapPrinciples) {
        if (mapPrincipleName.equals(mapPrinciple.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean containsMapAdvice(List<MapAdvice> mapAdvices,
    String mapAdviceName) {
    if (mapAdvices == null || mapAdviceName == null) {
      return false;
    } else {
      for (MapAdvice mapAdvice : mapAdvices) {
        if (mapAdviceName.equals(mapAdvice.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean containsMapRelation(List<MapRelation> mapRelations,
    String mapRelationName) {
    if (mapRelations == null || mapRelationName == null) {
      return false;
    } else {
      for (MapRelation mapRelation : mapRelations) {
        if (mapRelationName.equals(mapRelation.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean containsMapAgeRange(List<MapAgeRange> mapAgeRanges,
    String mapAgeRangeName) {
    if (mapAgeRanges == null || mapAgeRangeName == null) {
      return false;
    } else {
      for (MapAgeRange mapAgeRange : mapAgeRanges) {
        if (mapAgeRangeName.equals(mapAgeRange.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean containsReportDefintion(
    List<ReportDefinition> reportDefintions, String reportName) {
    if (reportDefintions == null || reportName == null) {
      return false;
    } else {
      for (ReportDefinition reportDefintion : reportDefintions) {
        if (reportName.equals(reportDefintion.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean containsMapProject(List<MapProject> mapProjects,
    String mapProjectName) {
    if (mapProjectName == null || mapProjects == null) {
      return false;
    } else {
      for (MapProject mapProject : mapProjects) {
        if (mapProjectName.equals(mapProject.getName())) {
          return true;
        }
      }
    }
    return false;
  }
}
