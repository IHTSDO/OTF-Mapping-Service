package org.ihtsdo.otf.mapping.rest;

import java.io.InputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
 * The Workflow Services REST package.
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
   */
  @GET
  @Path("/definition/definitions")
  @ApiOperation(value = "Gets all report definitions", notes = "Returns all report definitions in JSON or XML format", response = ReportDefinitionListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportDefinitionList getReportDefinitions(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /definition/definitions");
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get report definitions.")
                .build());

      // get the reports
      ReportService reportService = new ReportServiceJpa();
      ReportDefinitionList definitionList =
          reportService.getReportDefinitions();

      reportService.close();

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
    }
  }

  /**
   * Returns the report definitions.
   * 
   * @param authToken the auth token
   * @return the report definition
   */
  @GET
  @Path("/definition/id/{id}")
  @ApiOperation(value = "Gets a report definitions", notes = "Returns a report definition by id in JSON or XML format", response = ReportDefinitionJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportDefinition getReportDefinition(
    @ApiParam(value = "Report definition id", required = true) @PathParam("id") Long id,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /definition/id/" + id);
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to get report definitions.")
                .build());

      // get the reports
      ReportService reportService = new ReportServiceJpa();
      ReportDefinition definition = reportService.getReportDefinition(id);

      reportService.close();

      return definition;
    } catch (Exception e) {
      handleException(e, "trying to get report definition", user, "", "");
      return null;
    }
  }

  /**
   * Adds the report definitions.
   * 
   * @param reportDefinition the report definition
   * @param authToken the auth token
   * @return the report definition
   */
  @POST
  @Path("/definition/add")
  @ApiOperation(value = "Add a report definition", notes = "Adds a report definition based on a JSON or XML object", response = ReportDefinitionJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportDefinition addReportDefinitions(
    @ApiParam(value = "The report definition to add", required = true) ReportDefinitionJpa reportDefinition,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /definition/add");
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to add a report definition.")
            .build());

      // get the reports
      ReportService reportService = new ReportServiceJpa();
      ReportDefinition definition =
          reportService.addReportDefinition(reportDefinition);
      reportService.close();

      return definition;
    } catch (Exception e) {
      handleException(e, "trying to add a report definition", user, "", "");
      return null;
    }

  }

  /**
   * Update report definitions.
   * 
   * @param definition the definition
   * @param authToken the auth token
   */
  @POST
  @Path("/definition/update")
  @ApiOperation(value = "Updates a report definition", notes = "Updates the attached report definition", response = Response.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public void updateReportDefinitions(
    @ApiParam(value = "Report definition to update", required = true) ReportDefinitionJpa definition,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /definition/update");
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to update report definitions.")
            .build());

      // get the reports
      ReportService reportService = new ReportServiceJpa();
      reportService.updateReportDefinition(definition);
      reportService.close();

    } catch (Exception e) {
      handleException(e, "trying to update a report definition", user, "", "");
    }

  }

  /**
   * Deletes the report definitions.
   * 
   * @param reportDefinition the report definition
   * @param authToken the auth token
   */
  @DELETE
  @Path("/definition/delete")
  @ApiOperation(value = "Delete a report definition", notes = "Deletes a report definition based on a JSON or XML object", response = ReportDefinitionJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public void removeReportDefinitions(
    @ApiParam(value = "The report definition to delete", required = true) ReportDefinitionJpa reportDefinition,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /definition/delete");
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to delete a report definition.")
                .build());

      // if definition is connected to any projects, remove from projects
      MappingService mappingService = new MappingServiceJpa();
      for (MapProject mp : mappingService.getMapProjects().getMapProjects()) {
        Set<ReportDefinition> defsToRemove = new HashSet<>();
        for (ReportDefinition def : mp.getReportDefinitions()) {
          if (def.getId() == reportDefinition.getId()) {
            defsToRemove.add(def);
          }
        }
        for (ReportDefinition remove : defsToRemove) {
          mp.removeReportDefinition(remove);
        }
        mappingService.updateMapProject(mp);
      }
      mappingService.close();

      // remove all reports from all projects with given definition
      ReportService reportService = new ReportServiceJpa();
      Set<Report> reportsToRemove = new HashSet<>();
      for (Report report : reportService.getReports().getReports()) {
        if (report.getReportDefinition().getId() == reportDefinition.getId())
          reportsToRemove.add(report);
      }
      for (Report report : reportsToRemove) {
        reportService.removeReport(report.getId());
      }

      // remove report definition
      reportService.removeReportDefinition(reportDefinition.getId());
      reportService.close();

    } catch (Exception e) {
      handleException(e, "trying to delete a report definition", user, "", "");
    }

  }

  /**
   * Update reports.
   * 
   * @param report the report
   * @param authToken the auth token
   */
  @POST
  @Path("/report/add")
  @ApiOperation(value = "Adds a report", notes = "Adds a report", response = Response.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public void updateReports(
    @ApiParam(value = "Report report to update", required = true) ReportJpa report,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /report/add");
    String user = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              report.getMapProjectId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to add reports.").build());

      // get the reports
      ReportService reportService = new ReportServiceJpa();
      reportService.addReport(report);
      reportService.close();

    } catch (Exception e) {
      handleException(e, "trying to add a report", user, "", "");
    }

  }

  /**
   * Deletes the report.
   * 
   * @param report the report
   * @param authToken the auth token
   */
  @DELETE
  @Path("/report/delete")
  @ApiOperation(value = "Delete a report", notes = "Deletes a report based on a JSON or XML object", response = ReportDefinitionJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public void removeReport(
    @ApiParam(value = "The report to delete", required = true) ReportJpa report,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /report/delete");
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to delete a report.")
            .build());

      // get the reports
      ReportService reportService = new ReportServiceJpa();
      reportService.removeReport(report.getId());
      reportService.close();

    } catch (Exception e) {
      handleException(e, "trying to delete a report", user, "", report.getId()
          .toString());
    }

  }

  /**
   * Returns the reports for map project.
   * 
   * @param projectId the project id
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the reports for map project
   */
  @POST
  @Path("/report/reports/project/id/{projectId}")
  @ApiOperation(value = "Get all reports for a project", notes = "Returns all reports for a project in either JSON or XML format", response = ReportListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportList getReportsForMapProject(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Paging/filtering/sorting object", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /report/reports");
    String user = "";
    String projectName = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve reports for this project.")
                .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(projectId);
      projectName = mapProject.getName();
      mappingService.close();

      // get the reports
      ReportService reportService = new ReportServiceJpa();
      ReportList reportList =
          reportService.getReportsForMapProject(mapProject, pfsParameter);

      reportService.close();

      return reportList;
    } catch (Exception e) {
      handleException(e, "trying to retrieve all reports", user, projectName,
          "");
      return null;
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
   */
  @POST
  @Path("/report/reports/project/id/{projectId}/definition/id/{definitionId}")
  @ApiOperation(value = "Get all reports for a definition", notes = "Returns all reports for a definition in either JSON or XML format", response = ReportListJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportList getReportsForMapProjectAndReportType(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Report definition", required = true) @PathParam("definitionId") Long definitionId,
    @ApiParam(value = "Paging/filtering/sorting object", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /report/reports/project/id/"
            + projectId.toString() + "/definition/id/"
            + definitionId.toString());
    String user = "";
    String projectName = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to retrieve reports.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(projectId);
      projectName = mapProject.getName();
      mappingService.close();

      // get the reports
      ReportService reportService = new ReportServiceJpa();
      ReportDefinition reportDefinition =
          reportService.getReportDefinition(definitionId);
      ReportList reportList =
          reportService.getReportsForMapProjectAndReportDefinition(mapProject,
              reportDefinition, pfsParameter);
      reportService.close();

      return reportList;
    } catch (Exception e) {
      handleException(e, "trying to retrieve all reports", user, projectName,
          "");
      return null;
    }

  }

  /**
   * Returns the most recent report for map project and report type.
   * 
   * @param projectId the project id
   * @param definitionId the definition id
   * @param authToken the auth token
   * @return the reports for map project and report type
   */
  @GET
  @Path("/report/reports/latest/project/id/{projectId}/definition/id/{definitionId}")
  @ApiOperation(value = "Get most recent report for a definition", notes = "Returns most recent report for a definition in either JSON or XML format", response = ReportJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Report getLatestReportForMapProjectAndReportType(
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "Report definition", required = true) @PathParam("definitionId") Long definitionId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /report/reports/latest/project/id/"
            + projectId.toString() + "/definition/id/"
            + definitionId.toString());
    String user = "";
    String projectName = "";

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to retrieve most recent report.")
            .build());

      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(projectId);
      projectName = mapProject.getName();
      mappingService.close();

      // get the reports
      ReportService reportService = new ReportServiceJpa();
      ReportDefinition reportDefinition =
          reportService.getReportDefinition(definitionId);
      ReportList reportList =
          reportService.getReportsForMapProjectAndReportDefinition(mapProject,
              reportDefinition, null);
      reportService.close();

      Report latestReport = null;
      for (Report rpt : reportList.getReports()) {
      	if (latestReport == null || rpt.getTimestamp() > latestReport.getTimestamp())
      		latestReport = rpt;
      }
      return latestReport;
    } catch (Exception e) {
      handleException(e, "trying to retrieve most recent report", user, projectName,
          "");
      return null;
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
   */
  @POST
  @Path("/report/generate/project/id/{projectId}/user/id/{userName}")
  @ApiOperation(value = "Generate a report", notes = "Generates and returns a report given a definition, in either JSON or XML format", response = ReportJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Report generateReport(
    @ApiParam(value = "The report definition", required = true) ReportDefinitionJpa reportDefinition,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "User generating report", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /report/generate/project/id/" + projectId
            + "/user/id/" + userName + " with report definition "
            + reportDefinition.getName());

    String mapProjectName = "(not retrieved)";

    Report report = null;

    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to retrieve reports.")
            .build());

      // get the required objects
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(projectId);
      mapProjectName = mapProject.getName(); // for error handling
      MapUser mapUser = mappingService.getMapUser(userName);
      mappingService.close();

      // Only generate reports that have a query type
      if (reportDefinition.getQueryType() != ReportQueryType.NONE) {
        ReportService reportService = new ReportServiceJpa();
        report =
            reportService
                .generateReport(mapProject, mapUser,
                    reportDefinition.getName(), reportDefinition, new Date(),
                    false);

        reportService.addReport(report);
        reportService.close();
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
   */
  @POST
  @Path("/report/test/project/id/{projectId}/user/id/{userName}")
  @ApiOperation(value = "Tests a report", notes = "Generates a report given a definition, indicates if the generation was successful or not.", response = ReportJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public Boolean testReport(
    @ApiParam(value = "The report definition", required = true) ReportDefinitionJpa reportDefinition,
    @ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
    @ApiParam(value = "User generating report", required = true) @PathParam("userName") String userName,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /report/test/project/id/" + projectId
            + "/user/id/" + userName + " with report definition "
            + reportDefinition.getName());

    String mapProjectName = "(not retrieved)";
    try {
      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken, projectId);
      if (!role.hasPrivilegesOf(MapUserRole.LEAD))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to test  reports.").build());

      // get the required objects
      MappingService mappingService = new MappingServiceJpa();
      MapProject mapProject = mappingService.getMapProject(projectId);
      mapProjectName = mapProject.getName(); // for error handling
      MapUser mapUser = mappingService.getMapUser(userName);
      mappingService.close();

      // Report is NOT persisted
      // Only generate reports that have a query type
      if (reportDefinition.getQueryType() != ReportQueryType.NONE) {
        ReportService reportService = new ReportServiceJpa();
        // test call, return value not important
        reportService.generateReport(mapProject, mapUser,
            reportDefinition.getName(), reportDefinition, new Date(), false);

        reportService.close();
      }
      // If no exception, we are good.
      return true;
    } catch (Exception e) {

      handleException(e, "trying to test a report", userName, mapProjectName,
          "");
      return false;

    }
  }

  /**
   * Returns the report results.
   * 
   * @param pfsParameter the pfs parameter
   * @param reportResultId the report result id
   * @param authToken the auth token
   * @return the report results
   */
  @POST
  @Path("/reportResult/id/{reportResultId}/items")
  @ApiOperation(value = "Gets report result items", notes = "Returns paged report result items in either JSON or XML format", response = ReportJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportResultItemList getReportResults(
    @ApiParam(value = "The paging/filtering/sorting object", required = true) PfsParameterJpa pfsParameter,
    @ApiParam(value = "Report id", required = true) @PathParam("reportResultId") Long reportResultId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /reportResult/id/" + reportResultId
            + "/items/");

    String user = "(not retrieved)";

    try {

      // retrieve the report and map project for this report result
      ReportService reportService = new ReportServiceJpa();
      MappingService mappingService = new MappingServiceJpa();

      ReportResult reportResult = reportService.getReportResult(reportResultId);
      Report report = reportResult.getReport();
      MapProject mapProject =
          mappingService.getMapProject(report.getMapProjectId());

      // authorize call
      MapUserRole role =
          securityService.getMapProjectRoleForToken(authToken,
              mapProject.getId());
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
        throw new WebApplicationException(
            Response
                .status(401)
                .entity(
                    "User does not have permissions to retrieve report result items.")
                .build());

      ReportResultItemList reportResultItemList =
          reportService.getReportResultItemsForReportResult(reportResultId,
              pfsParameter);

      reportService.close();

      return reportResultItemList;
    } catch (Exception e) {

      handleException(e, "trying to retrieve report result items", user,
          reportResultId.toString(), "");
      return null;

    }
  }

  /**
   * Returns the qaCheck definitions.
   * 
   * @param authToken the auth token
   * @return the qaCheck definition
   */
  @GET
  @Path("/qaCheckDefinition/qaCheckDefinitions")
  @ApiOperation(value = "Gets all qaCheck definitions", notes = "Returns all qaCheck definitions in JSON or XML format", response = ReportDefinitionJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public ReportDefinitionList getQACheckDefinitions(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /qaCheckDefinition/qaCheckDefinitions");
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response
            .status(401)
            .entity(
                "User does not have permissions to get qaCheck definitions.")
            .build());

      // get the qaChecks
      ReportService qaCheckService = new ReportServiceJpa();
      ReportDefinitionList definitionList =
          qaCheckService.getQACheckDefinitions();
      qaCheckService.close();

      // sort results
      definitionList.sortBy(new Comparator<ReportDefinition>() {
        @Override
        public int compare(ReportDefinition o1, ReportDefinition o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });

      return definitionList;
    } catch (Exception e) {
      handleException(e, "trying to get qaCheck definitions", user, "", "");
      return null;
    }
  }

  /**
   * Returns the QA labels.
   * 
   * @param authToken the auth token
   * @return the QA labels
   */
  @GET
  @Path("/qaLabel/qaLabels")
  @ApiOperation(value = "Gets all qa labels", notes = "Returns all qa labels in JSON or XML format", response = ReportDefinitionJpa.class)
  @Produces({
      MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
  })
  public SearchResultList getQALabels(
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /qaLabel/qaLabels");
    String user = "";

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      user = securityService.getUsernameForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to get qa labels.").build());

      // get the qaChecks
      ReportService qaCheckService = new ReportServiceJpa();
      SearchResultList labelList = qaCheckService.getQALabels();
      qaCheckService.close();

      return labelList;
    } catch (Exception e) {
      handleException(e, "trying to get qa labels", user, "", "");
      return null;
    }
  }

  /**
   * Export report.
   * 
   * @param reportId the report id
   * @param authToken the auth token
   * @return the input stream
   */
  @GET
  @Path("/report/export/{reportId}")
  @ApiOperation(value = "Exports a report", notes = "Exports a report given a report id", response = ReportJpa.class)
  @Produces("application/vnd.ms-excel")
  public InputStream exportReport(
    @ApiParam(value = "Report id", required = true) @PathParam("reportId") Long reportId,
    @ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

    Logger.getLogger(MappingServiceRest.class).info(
        "RESTful call (Report):  /report/export/" + reportId);

    try {
      // authorize call
      MapUserRole role = securityService.getApplicationRoleForToken(authToken);
      if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
        throw new WebApplicationException(Response.status(401)
            .entity("User does not have permissions to export report.").build());

      ReportService reportService = new ReportServiceJpa();
      Report report = reportService.getReport(reportId);

      ExportReportHandler handler = new ExportReportHandler();
      InputStream is = handler.exportReport(report);

      reportService.close();
      return is;

    } catch (Exception e) {

      handleException(e, "trying to export a report", "", "",
          reportId.toString());
      return null;

    }
  }

}
