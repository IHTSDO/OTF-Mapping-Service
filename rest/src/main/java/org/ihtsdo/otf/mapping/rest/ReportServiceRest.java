package org.ihtsdo.otf.mapping.rest;

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
import org.ihtsdo.otf.mapping.helpers.ReportType;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;
import org.ihtsdo.otf.mapping.reports.ReportJpa;
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
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class ReportServiceRest extends RootServiceRest {

	private SecurityService securityService;

	/**
	 * Instantiates an empty {@link WorkflowServiceRest}.
	 */
	public ReportServiceRest() throws Exception {
		securityService = new SecurityServiceJpa();
	}
	
	/**
	 * Returns the report.
	 *
	 * @param authToken the auth token
	 * @return the report
	 */
	@GET
	@Path("/definition/definitions")
	@ApiOperation(value = "Get all currently defined reports", notes = "Returns all report definitions in either JSON or XML format", response = ReportDefinitionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public ReportDefinitionList getReportDefinitions(
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Report):  /definition/definitions");
		String user = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve map projects.")
								.build());
			
			// get the reports
			ReportService reportService = new ReportServiceJpa();
			ReportDefinitionList definitionList = reportService.getReportDefinitionsForRole(role);
			reportService.close();
			
			return definitionList;
		} catch (Exception e) {
			handleException(e, "trying to retrieve all reports", user, "", "");
			return null;
		}

	}
	
	@POST
	@Path("/definition/add")
	@ApiOperation(value = "Add a report definition", notes = "Adds a report definition based on a JSON or XML object", response = ReportDefinitionJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public ReportDefinition addReportDefinitions(
			@ApiParam(value = "The report definition to add", required = true) ReportDefinitionJpa reportDefinition,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Report):  /definition/definitions");
		String user = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve map projects.")
								.build());
			
			// get the reports
			ReportService reportService = new ReportServiceJpa();
			ReportDefinition definition = reportService.addReportDefinition(reportDefinition);
			reportService.close();
			
			return definition;
		} catch (Exception e) {
			handleException(e, "trying to retrieve all reports", user, "", "");
			return null;
		}

	}
	
	@POST
	@Path("/definition/update")
	@ApiOperation(value = "Updates a report definition", notes = "Updates the attached report definition", response = Response.class )
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public void updateReportDefinitions(
			@ApiParam(value = "Report definition to update", required = true) ReportDefinitionJpa definition,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Report):  /definition/definitions");
		String user = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve map projects.")
								.build());
			
			// get the reports
			ReportService reportService = new ReportServiceJpa();
			reportService.updateReportDefinition(definition);
			reportService.close();

		} catch (Exception e) {
			handleException(e, "trying to retrieve all reports", user, "", "");
		}

	}
	
	@GET
	@Path("/report/reports")
	@ApiOperation(value = "Get all reports", notes = "Returns all reports in either JSON or XML format", response = ReportDefinitionListJpa.class )
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public ReportList getReports(
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Report):  /report/reports");
		String user = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve map projects.")
								.build());
			
			// get the reports
			ReportService reportService = new ReportServiceJpa();
			ReportList reportList = reportService.getReports();
			reportService.close();
			
			return reportList;
		} catch (Exception e) {
			handleException(e, "trying to retrieve all reports", user, "", "");
			return null;
		}

	}
	
	@POST
	@Path("/report/reports/project/id/{projectId}")
	@ApiOperation(value = "Get all reports of a certain type", notes = "Returns all reports in either JSON or XML format", response = ReportListJpa.class )
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
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
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve map projects.")
								.build());
			
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(projectId);
			projectName = mapProject.getName();
			mappingService.close();
			
			// get the reports
			ReportService reportService = new ReportServiceJpa();
			ReportList reportList = reportService.getReportsForMapProjectAndReportType(mapProject, null, pfsParameter);
			reportService.close();
			
			return reportList;
		} catch (Exception e) {
			handleException(e, "trying to retrieve all reports", user, projectName, "");
			return null;
		}

	}
	
	@POST
	@Path("/report/reports/project/id/{projectId}/type/{reportType}")
	@ApiOperation(value = "Get all reports of a certain type", notes = "Returns all reports in either JSON or XML format", response = ReportListJpa.class )
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public ReportList getReportsForMapProjectAndReportType(
			@ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
			@ApiParam(value = "Type of report", required = true) @PathParam("reportType") String reportType,
			@ApiParam(value = "Paging/filtering/sorting object", required = true) PfsParameterJpa pfsParameter,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Report):  /report/reports");
		String user = "";
		String projectName = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve map projects.")
								.build());
			
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(projectId);
			projectName = mapProject.getName();
			mappingService.close();
			
			// get the reports
			ReportService reportService = new ReportServiceJpa();
			ReportList reportList = reportService.getReportsForMapProjectAndReportType(mapProject, reportType, pfsParameter);
			reportService.close();
			
			return reportList;
		} catch (Exception e) {
			handleException(e, "trying to retrieve all reports", user, projectName, "");
			return null;
		}

	}
	
	@GET
	@Path("/report/project/id/{projectId}/type/{reportType}")
	@ApiOperation(value = "Get most recent report of a certain type", notes = "Returns the most recent report given a report type in either JSON or XML format", response = ReportJpa.class )
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Report getLastReportForReportType(
			@ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
			@ApiParam(value = "Type of report", required = true) @PathParam("reportType") String reportType,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Report):  /report/project/id" + projectId + "/type/" + reportType);
		String user = "";
		String projectName = "";
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve map projects.")
								.build());
			
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(projectId);
			projectName = mapProject.getName();
			mappingService.close();
			
			// get the reports
			ReportService reportService = new ReportServiceJpa();
			Report report = reportService.getLastReportForReportType(mapProject, reportType);
			reportService.close();
			
			return report;
		} catch (Exception e) {
			handleException(e, "trying to retrieve all reports", user, projectName, "");
			return null;
		}

	}
	
	
	@POST
	@Path("/report/generate/project/id/{projectId}/user/id/{userName}/type/{reportType}")
	@ApiOperation(value = "Add a report", notes = "Returns all MapProjects in either JSON or XML format", response = ReportJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Report generateReport(
			@ApiParam(value = "The report definition name", required = true)  @PathParam("reportType") String reportType,
			@ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
			@ApiParam(value = "User generating report", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Report):  /report/generate/project/id/" + projectId + "/user/id/" + userName + "/type/" + reportType	);
		
		String mapProjectName = "(not retrieved)";
		String user = "(not retrieved)";
		
		Report report = null;
		
		try {
			// authorize call
			MapUserRole role = securityService
					.getApplicationRoleForToken(authToken);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve map projects.")
								.build());
			
			// get the required objects
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(projectId);
			mapProjectName = mapProject.getName(); // for error handling
			MapUser mapUser = mappingService.getMapUser(userName);
			mappingService.close();
			
			ReportService reportService = new ReportServiceJpa();
			ReportDefinition reportDefinition = reportService.getReportDefinition(ReportType.valueOf(reportType));
			report = reportService.generateReport(mapProject, mapUser, reportDefinition.getReportName(), reportDefinition, null, false);
			reportService.close();
			
			return report;
		} catch (Exception e) {
			
			handleException(e, "trying to generate a report", userName, mapProjectName, "");
			return null;
			
		}
	}
		
	
}
