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
import org.ihtsdo.otf.mapping.helpers.MapProjectListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionList;
import org.ihtsdo.otf.mapping.helpers.ReportList;
import org.ihtsdo.otf.mapping.helpers.ReportType;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.Report;
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
@Api(value = "/report", description = "Operations supporting reporting")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class ReportServiceRest extends RootServiceRest {

	private SecurityService securityService;

	/**
	 * Instantiates an empty {@link WorkflowServiceRest}.
	 */
	public ReportServiceRest() throws Exception {
		securityService = new SecurityServiceJpa();
	}
	
	@GET
	@Path("/definition/definitions")
	@ApiOperation(value = "Get all currently defined reports", notes = "Returns all report definitions in either JSON or XML format", response = MapProjectListJpa.class)
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
	
	@GET
	@Path("/report/reports")
	@ApiOperation(value = "Get all reports", notes = "Returns all reports in either JSON or XML format", response = MapProjectListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public ReportList getReport(
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
	@Path("/report/add")
	@ApiOperation(value = "Add a report", notes = "Returns all MapProjects in either JSON or XML format", response = MapProjectListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Report addReport(
			@ApiParam(value = "Report to add", required = true) ReportJpa report,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping):  /project/projects");
		String user = "(not retrieved)";
		String mapProjectName = "(not retrieved)";
		
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
			
			// get the project for this report -- used for error reporting
			MappingService mappingService = new MappingServiceJpa();
			mapProjectName = mappingService.getMapProject(report.getMapProjectId()).getName();
			mappingService.close();
			
			// get the reports
			ReportService reportService = new ReportServiceJpa();
			Report reportAdded = reportService.addReport(report);
			reportService.close();
			
			return reportAdded;
		} catch (Exception e) {
			
			handleException(e, "trying to add a report", user, mapProjectName, "");
			return null;
		}

	}
	
	@GET
	@Path("/report/generate/project/id/{projectId}/user/id/{userName}/definition/id/{definitionName}")
	@ApiOperation(value = "Add a report", notes = "Returns all MapProjects in either JSON or XML format", response = MapProjectListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Report generateReport(
			@ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
			@ApiParam(value = "User generating report", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Type of report to generate", required = true) @PathParam("definitionName") String definitionName,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Report):  /report/generate/project/id/" + projectId + "/user/id/" + userName + "/definition/id/" + definitionName);
		
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
			report = reportService.generateReport(mapProject, mapUser, "test report", ReportType.valueOf(definitionName), null, false);
			reportService.close();
			
			return report;
		} catch (Exception e) {
			
			handleException(e, "trying to add a report", userName, mapProjectName, "");
			return null;
			
		}
	}
		
	
}
