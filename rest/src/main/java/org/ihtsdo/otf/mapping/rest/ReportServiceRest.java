/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionList;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionListJpa;
import org.ihtsdo.otf.mapping.helpers.ReportList;
import org.ihtsdo.otf.mapping.helpers.ReportListJpa;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultItemList;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.jpa.handlers.ExportReportHandler;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;
import org.ihtsdo.otf.mapping.reports.ReportJpa;
import org.ihtsdo.otf.mapping.reports.ReportResult;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * REST implementation for report service.
 */
@Path("/reporting")
@Api(value = "/report", description = "Operations supporting reporting.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class ReportServiceRest extends RootServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link WorkflowServiceRest}.
   * 
   * @throws Exception the exception
   */
  public ReportServiceRest() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /**
   * Returns the report definitions.
   *
   * @param authToken the auth token
   * @return the report definition
   * @throws Exception the exception
   */
  @GET
  @Path("/definition/definitions")
  @ApiOperation(value = "Get all report definitions", notes = "Gets all report definitions.", response = ReportDefinitionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportDefinitionList getReportDefinitions(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /definition/definitions");
    String user = null;
    final ReportService reportService = new ReportServiceJpa();
    try {
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get report definitions", securityService);

      // get the reports
      final ReportDefinitionList definitionList =
          reportService.getReportDefinitions();

      // sort by name
      definitionList.sortBy(new Comparator<ReportDefinition>() {
        @Override
        public int compare(ReportDefinition o1, ReportDefinition o2) {
          return o1.getName().toLowerCase()
              .compareTo(o2.getName().toLowerCase());
        }
      });

      return definitionList;
    } catch (Exception e) {
      handleException(e, "trying to get report definitions", user, "", "");
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }

  /**
   * Returns the report definitions.
   *
   * @param id the id
   * @param authToken the auth token
   * @return the report definition
   * @throws Exception
   */
  @GET
  @Path("/definition/id/{id}")
  @ApiOperation(value = "Get a report definition", notes = "Gets the report definition for the specified id.", response = ReportDefinitionJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportDefinition getReportDefinition(
    @ApiParam(value = "Report definition id", required = true) @PathParam("id") Long id,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /definition/id/" + id);
    String user = null;
    final ReportService reportService = new ReportServiceJpa();
    try {
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get report definition", securityService);

      // get the reports
      return reportService.getReportDefinition(id);

    } catch (Exception e) {
      handleException(e, "trying to get report definition", user, "", "");
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }

  /**
   * Adds the report definitions.
   *
   * @param reportDefinition the report definition
   * @param authToken the auth token
   * @return the report definition
   * @throws Exception the exception
   */
  @POST
  @Path("/definition/add")
  @ApiOperation(value = "Add a report definition", notes = "Adds the specified report definition.", response = ReportDefinitionJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportDefinition addReportDefinition(
    @ApiParam(value = "The report definition to add, in JSON or XML format", required = true) ReportDefinitionJpa reportDefinition,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /definition/add");
    String user = null;
    final ReportService reportService = new ReportServiceJpa();
    try {
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
          "add report definition", securityService);

      // add the definition
      return reportService.addReportDefinition(reportDefinition);

    } catch (Exception e) {
      handleException(e, "trying to add a report definition", user, "", "");
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }

  /**
   * Update report definitions.
   * 
   * @param definition the definition
   * @param authToken the auth token
   * @throws Exception
   */
  @POST
  @Path("/definition/update")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Update a report definition", notes = "Updates the specified report definition.", response = Response.class)
  public void updateReportDefinitions(
    @ApiParam(value = "Report definition to update, in JSON or XML format", required = true) ReportDefinitionJpa definition,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /definition/update");
    String user = null;
    final ReportService reportService = new ReportServiceJpa();

    try {
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
          "update report definition", securityService);

      // update report definition
      reportService.updateReportDefinition(definition);

    } catch (Exception e) {
      handleException(e, "trying to update a report definition", user, "", "");
    } finally {
      reportService.close();
      securityService.close();
    }

  }

  /**
   * Deletes the report definitions.
   *
   * @param reportDefinition the report definition
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @DELETE
  @Path("/definition/delete")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Delete a report definition", notes = "Deletes the specified report definition.", response = ReportDefinitionJpa.class)
  public void removeReportDefinitions(
    @ApiParam(value = "The report definition to delete, in JSON or XML format", required = true) ReportDefinitionJpa reportDefinition,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /definition/delete");
    String user = null;
    final ReportServiceJpa reportService = new ReportServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();

    try {
      user = authorizeApp(authToken, MapUserRole.ADMINISTRATOR,
          "remove report definition", securityService);

      // if definition is connected to any projects, remove from projects
      for (final MapProject mp : mappingService.getMapProjects()
          .getMapProjects()) {
        Set<ReportDefinition> defsToRemove = new HashSet<>();
        for (final ReportDefinition def : mp.getReportDefinitions()) {
          if (def.getId() == reportDefinition.getId()) {
            defsToRemove.add(def);
          }
        }
        for (final ReportDefinition remove : defsToRemove) {
          mp.removeReportDefinition(remove);
        }
        mappingService.updateMapProject(mp);
      }

      // remove all reports from all projects with given definition
      final javax.persistence.Query query = reportService.getEntityManager()
          .createQuery("select a from ReportJpa a , ReportDefinitionJpa b "
              + "where a.reportDefinition = b "
              + "  and b.id = :reportDefinitionId");
      query.setParameter("reportDefinitionId", reportDefinition.getId());
      @SuppressWarnings("unchecked")
      final List<Report> reports = query.getResultList();
      for (final Report report : reports) {
        reportService.removeReport(report.getId());
      }

      // remove report definition
      reportService.removeReportDefinition(reportDefinition.getId());

    } catch (Exception e) {
      handleException(e, "trying to delete a report definition", user, "", "");
    } finally {
      reportService.close();
      mappingService.close();
      securityService.close();
    }

  }

  /**
   * Add reports.
   * 
   * @param report the report
   * @param authToken the auth token
   * @throws Exception
   */
  @POST
  @Path("/report/add")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Adds a report", notes = "Adds the specified report", response = Response.class)
  public void addReport(
    @ApiParam(value = "Report report to update, in JSON or XML format", required = true) ReportJpa report,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /report/add");
    String user = null;
    final ReportService reportService = new ReportServiceJpa();
    try {
      user = authorizeProject(report.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "update report", securityService);

      // update report
      reportService.addReport(report);

    } catch (Exception e) {
      handleException(e, "trying to add a report", user, "", "");
    } finally {
      reportService.close();
      securityService.close();
    }

  }

  /**
   * Deletes the report.
   * 
   * @param report the report
   * @param authToken the auth token
   * @throws Exception
   */
  @DELETE
  @Path("/report/delete")
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @ApiOperation(value = "Delete a report", notes = "Deletes the specified report.", response = ReportDefinitionJpa.class)
  public void removeReport(
    @ApiParam(value = "The report to delete, in JSON or XML format", required = true) ReportJpa report,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /report/delete");
    String user = null;

    final ReportService reportService = new ReportServiceJpa();
    try {
      user = authorizeProject(report.getMapProjectId(), authToken,
          MapUserRole.SPECIALIST, "remove report", securityService);

      // remove report
      reportService.removeReport(report.getId());

    } catch (Exception e) {
      handleException(e, "trying to delete a report", user, "",
          report.getId().toString());
    } finally {
      reportService.close();
      securityService.close();
    }

  }

  @GET
  @Path("/report/project/id/{projectId}/{id}")
  @ApiOperation(value = "Get a report by id", notes = "Get report by id", response = ReportJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Report getReport(
    @ApiParam(value = "Project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Report id", required = true) @PathParam("id") Long id,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /report/projectId/" + projectId + "/" + id);
    String user = null;
   
    final ReportService reportService = new ReportServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.VIEWER,
          "get report for project", securityService);
      Report report = reportService.getReport(id);

      // clear the report result items, retrieved via separate call
      for (ReportResult result : report.getResults()) {
        // trigger setting of ct
        result.getCt();
        result.setReportResultItems(null);
      }

      return report;

    } catch (Exception e) {
      handleException(e, "trying to get report for project", user, "",
          id.toString());
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }

  /**
   * Returns the reports for map project.
   * 
   * @param projectId the project id
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the reports for map project
   * @throws Exception
   */
  @POST
  @Path("/report/reports/project/id/{projectId}")
  @ApiOperation(value = "Get all reports for a project", notes = "Gets all reports for the specified project.", response = ReportListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportList getReportsForMapProject(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Paging/filtering/sorting object, in JSON or XML format", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /report/reports");
    String user = null;
    String projectName = "";

    final ReportService reportService = new ReportServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.VIEWER,
          "get reports for project", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);
      projectName = mapProject.getName();

      // get the reports
      final ReportList reportList =
          reportService.getReportsForMapProject(mapProject, pfsParameter);

      // clear report result items
      final MapUserRole userRole =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      final List<Report> reports = new ArrayList<>();
      for (final Report report : reportList.getReports()) {
        // Only process roles the user has priveleges for
        if (userRole
            .hasPrivilegesOf(report.getReportDefinition().getRoleRequired())) {
          reports.add(report);
          for (final ReportResult result : report.getResults()) {
            // trigger setting of ct
            result.getCt();
            result.setReportResultItems(null);
          }
        }
      }
      reportList.setReports(reports);
      return reportList;
    } catch (Exception e) {
      handleException(e, "trying to get all reports", user, projectName, "");
      return null;
    } finally {
      reportService.close();
      mappingService.close();
      securityService.close();
    }

  }

  /**
   * Returns the reports for map project and report type.
   * 
   * @param projectId the project id
   * @param definitionId the definition id
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the reports for map project and report type
   * @throws Exception
   */
  @POST
  @Path("/report/reports/project/id/{projectId}/definition/id/{definitionId}")
  @ApiOperation(value = "Get all reports for a definition and map project", notes = "Gets all reports for the specified project and definition.", response = ReportListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportList getReportsForMapProjectAndReportDefinition(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Report definition id", required = true) @PathParam("definitionId") Long definitionId,
    @ApiParam(value = "Paging/filtering/sorting object, in JSON or XML format", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /report/reports/project/id/"
            + projectId.toString() + "/definition/id/"
            + definitionId.toString());
    String user = null;
    String projectName = "";

    final ReportService reportService = new ReportServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.VIEWER,
          "get reports for project and defintion", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);
      projectName = mapProject.getName();

      // get the reports
      final ReportDefinition reportDefinition =
          reportService.getReportDefinition(definitionId);
      final ReportList reportList =
          reportService.getReportsForMapProjectAndReportDefinition(mapProject,
              reportDefinition, pfsParameter);

      // clear report result items
      for (final Report report : reportList.getReports()) {
        for (final ReportResult result : report.getResults()) {
          // trigger setting of ct
          result.getCt();
          result.setReportResultItems(null);
        }
      }

      return reportList;
    } catch (Exception e) {
      handleException(e, "trying to get reports", user, projectName, "");
      return null;
    } finally {
      reportService.close();
      mappingService.close();
      securityService.close();
    }

  }

  /**
   * Returns the most recent report for map project and report type.
   * 
   * @param projectId the project id
   * @param definitionId the definition id
   * @param authToken the auth token
   * @return the reports for map project and report type
   * @throws Exception
   */
  @GET
  @Path("/report/reports/latest/project/id/{projectId}/definition/id/{definitionId}/items")
  @ApiOperation(value = "Get latest report for a project and definition", notes = "Gets the latest result items for the specified definition and project", response = ReportJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Report getLatestReport(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Report definition id", required = true) @PathParam("definitionId") Long definitionId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /report/reports/latest/project/id/"
            + projectId.toString() + "/definition/id/" + definitionId.toString()
            + "/items");
    String user = null;
    String projectName = null;
    final ReportService reportService = new ReportServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();

    try {
      // authorize call
      user = authorizeProject(projectId, authToken, MapUserRole.VIEWER,
          "get latest report for definition", securityService);

      final MapProject mapProject = mappingService.getMapProject(projectId);
      projectName = mapProject.getName();

      // get the reports
      final ReportDefinition reportDefinition =
          reportService.getReportDefinition(definitionId);
      final ReportList reportList =
          reportService.getReportsForMapProjectAndReportDefinition(mapProject,
              reportDefinition, null);

      Report latestReport = null;
      for (final Report rpt : reportList.getReports()) {
        if (latestReport == null
            || rpt.getTimestamp() > latestReport.getTimestamp())
          latestReport = rpt;
      }
      if (latestReport == null) {
        return null;
      }
      // lazy initialize items
      for (final ReportResult result : latestReport.getResults()) {
        result.getReportResultItems().size();
      }
      return latestReport;
    } catch (Exception e) {
      handleException(e, "trying to get most recent report", user, projectName,
          "");
      return null;
    } finally {
      reportService.close();
      mappingService.close();
      securityService.close();
    }

  }

  /**
   * Generate report.
   * @param reportDefinition the report definition
   * 
   * @param projectId the project id
   * @param userName the user name
   * @param authToken the auth token
   * @return the report
   * @throws Exception
   */
  @POST
  @Path("/report/generate/project/id/{projectId}/user/id/{userName}")
  @ApiOperation(value = "Generate a report", notes = "Generates and returns a report for the specified report definition and project", response = ReportJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Report generateReport(
    @ApiParam(value = "The report definition, in JSON or XML format", required = true) ReportDefinitionJpa reportDefinition,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "User generating report", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /report/generate/project/id/" + projectId
            + "/user/id/" + userName + " with report definition "
            + reportDefinition.getName());

    String mapProjectName = "";
    final ReportService reportService = new ReportServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      authorizeProject(projectId, authToken, MapUserRole.SPECIALIST,
          "generate report", securityService);

      // get the required objects
      final MapProject mapProject = mappingService.getMapProject(projectId);
      mapProjectName = mapProject.getName(); // for error handling
      final MapUser mapUser = mappingService.getMapUser(userName);

      // Only generate reports that have a query type
      Report report = null;
      if (reportDefinition.getQueryType() != ReportQueryType.NONE) {
        report = reportService.generateReport(mapProject, mapUser,
            reportDefinition.getName(), reportDefinition, new Date(), false);

        reportService.addReport(report);
      }
      // Otherwise, return an empty report
      else {
        return new ReportJpa();
      }

      return report;
    } catch (Exception e) {
      handleException(e, "trying to generate a report", userName,
          mapProjectName, "");
      return null;
    } finally {
      reportService.close();
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Tests the generation of a report.
   * @param reportDefinition the report definition
   * 
   * @param projectId the project id
   * @param userName the user name
   * @param authToken the auth token
   * @return the report
   * @throws Exception
   */
  @POST
  @Path("/report/test/project/id/{projectId}/user/id/{userName}")
  @ApiOperation(value = "Tests a report", notes = "Tests generation of the specified report definition.", response = ReportJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Boolean testReport(
    @ApiParam(value = "The report definition", required = true) ReportDefinitionJpa reportDefinition,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "User generating report", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /report/test/project/id/" + projectId
            + "/user/id/" + userName + " with report definition "
            + reportDefinition.getName());

    String mapProjectName = "";
    final ReportService reportService = new ReportServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      authorizeProject(projectId, authToken, MapUserRole.SPECIALIST,
          "test report", securityService);

      // get the required objects
      final MapProject mapProject = mappingService.getMapProject(projectId);
      mapProjectName = mapProject.getName(); // for error handling
      final MapUser mapUser = mappingService.getMapUser(userName);

      // Report is NOT persisted
      // Only generate reports that have a query type
      if (reportDefinition.getQueryType() != ReportQueryType.NONE) {
        // test call, return value not important
        reportService.generateReport(mapProject, mapUser,
            reportDefinition.getName(), reportDefinition, new Date(), false);

      }
      // If no exception, we are good.
      return true;
    } catch (Exception e) {
      handleException(e, "trying to test a report", userName, mapProjectName,
          "");
      return false;
    } finally {
      reportService.close();
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns the report result items.
   * 
   * @param pfsParameter the pfs parameter
   * @param reportResultId the report result id
   * @param authToken the auth token
   * @return the report results
   * @throws Exception
   */
  @POST
  @Path("/reportResult/id/{reportResultId}/items")
  @ApiOperation(value = "Gets report result items", notes = "Gets paged report result items for the report result id", response = ReportJpa.class)
  @Consumes({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportResultItemList getReportResultItems(
    @ApiParam(value = "The paging/filtering/sorting object, in JSON or XML format", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Report id", required = true) @PathParam("reportResultId") Long reportResultId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /reportResult/id/" + reportResultId
            + "/items/");

    String user = null;
    final ReportService reportService = new ReportServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get report results",
          securityService);

      // get the report and map project for this report result
      final ReportResult reportResult =
          reportService.getReportResult(reportResultId);
      final Report report = reportResult.getReport();
      final MapProject mapProject =
          mappingService.getMapProject(report.getMapProjectId());

      // authorize call
      final MapUserRole role = securityService
          .getMapProjectRoleForToken(authToken, mapProject.getId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(Response.status(401)
            .entity(
                "User does not have permissions to get report result items.")
            .build());

      final ReportResultItemList reportResultItemList = reportService
          .getReportResultItemsForReportResult(reportResultId, pfsParameter);

      return reportResultItemList;
    } catch (Exception e) {
      handleException(e, "trying to get report result items", user,
          reportResultId.toString(), "");
      return null;
    } finally {
      reportService.close();
      mappingService.close();
      securityService.close();
    }
  }

  /**
   * Returns the qaCheck definitions.
   * 
   * @param authToken the auth token
   * @return the qaCheck definition
   * @throws Exception
   */
  @GET
  @Path("/qaCheckDefinition/qaCheckDefinitions")
  @ApiOperation(value = "Gets all qa check definitions", notes = "Gets all qa check definitions.", response = ReportDefinitionJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportDefinitionList getQACheckDefinitions(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /qaCheckDefinition/qaCheckDefinitions");
    String user = null;
    final ReportService reportService = new ReportServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER,
          "get qa check definitions", securityService);

      // get the qaChecks
      final ReportDefinitionList definitionList =
          reportService.getQACheckDefinitions();

      // sort results
      definitionList.sortBy(new Comparator<ReportDefinition>() {
        @Override
        public int compare(ReportDefinition o1, ReportDefinition o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });

      return definitionList;
    } catch (Exception e) {
      handleException(e, "trying to get qa check definitions", user, "", "");
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }

  /**
   * Returns the QA labels.
   *
   * @param mapProjectId the map project id
   * @param authToken the auth token
   * @return the QA labels
   * @throws Exception the exception
   */
  @GET
  @Path("/qaLabel/qaLabels/{mapProjectId}")
  @ApiOperation(value = "Gets all qa labels", notes = "Gets all qa labels.", response = ReportDefinitionJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getQALabels(
    @ApiParam(value = "Report id", required = true) @PathParam("mapProjectId") Long mapProjectId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {
    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /qaLabel/qaLabels");
    String user = null;
    final ReportService reportService = new ReportServiceJpa();
    try {
      // authorize call
      user = authorizeApp(authToken, MapUserRole.VIEWER, "get qa labels",
          securityService);

      // get the qaChecks
      return reportService.getQALabels(mapProjectId);

    } catch (Exception e) {
      handleException(e, "trying to get qa labels", user, "", "");
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }

  /**
   * Export report.
   * 
   * @param reportId the report id
   * @param authToken the auth token
   * @return the input stream
   * @throws Exception
   */
  @GET
  @Path("/report/export/{reportId}")
  @ApiOperation(value = "Exports a report", notes = "Exports a report the specified id.", response = ReportJpa.class)
  @Produces("application/vnd.ms-excel")
  public InputStream exportReport(
    @ApiParam(value = "Report id", required = true) @PathParam("reportId") Long reportId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
    throws Exception {

    Logger.getLogger(MappingServiceRest.class)
        .info("RESTful call (Report):  /report/export/" + reportId);

    final ReportService reportService = new ReportServiceJpa();
    try {
      Report report = reportService.getReport(reportId);
      // authorize call
      authorizeProject(report.getMapProjectId(), authToken, MapUserRole.VIEWER,
          "export report", securityService);

      final ExportReportHandler handler = new ExportReportHandler();
      return handler.exportReport(report);

    } catch (Exception e) {
      handleException(e, "trying to export a report", "", "",
          reportId.toString());
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }

}
