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
import org.ihtsdo.otf.mapping.helpers.ReportList;
import org.ihtsdo.otf.mapping.helpers.ReportListJpa;
import org.ihtsdo.otf.mapping.helpers.ReportResultItemList;
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
	@ApiOperation(value = "Gets all report definitions", notes = "Gets all currently defined report definitions as a JSON or XML object", response = ReportDefinitionJpa.class)
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
			if (!role.hasPrivilegesOf(MapUserRole.VIEWER))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve definitions.")
								.build()); 

			// get the reports
			ReportService reportService = new ReportServiceJpa();
			ReportDefinitionList definitionList = reportService
					.getReportDefinitions();
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
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to add reports.")
								.build()); 

			// get the reports
			ReportService reportService = new ReportServiceJpa();
			ReportDefinition definition = reportService
					.addReportDefinition(reportDefinition);
			reportService.close();

			return definition;
		} catch (Exception e) {
			handleException(e, "trying to retrieve all reports", user, "", "");
			return null;
		}

	}

	@POST
	@Path("/definition/update")
	@ApiOperation(value = "Updates a report definition", notes = "Updates the attached report definition", response = Response.class)
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
			if (!role.hasPrivilegesOf(MapUserRole.ADMINISTRATOR)) 
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve reports.")
								.build()); 

			// get the reports
			ReportService reportService = new ReportServiceJpa();
			reportService.updateReportDefinition(definition);
			reportService.close();

		} catch (Exception e) {
			handleException(e, "trying to retrieve all reports", user, "", "");
		}

	}

	@POST
	@Path("/report/reports/project/id/{projectId}")
	@ApiOperation(value = "Get all reports of a certain type", notes = "Returns all reports in either JSON or XML format", response = ReportListJpa.class)
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
					.getMapProjectRoleForToken(authToken, projectId);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) 
				 throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve reports for this project.")
								.build()); 

			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(projectId);
			projectName = mapProject.getName();
			mappingService.close();

			// get the reports
			ReportService reportService = new ReportServiceJpa();
			ReportList reportList = reportService
					.getReportsForMapProject(mapProject,
							pfsParameter);
			reportService.close();

			return reportList;
		} catch (Exception e) {
			handleException(e, "trying to retrieve all reports", user,
					projectName, "");
			return null;
		}

	}

	@POST
	@Path("/report/reports/project/id/{projectId}/definition/id/{definitionId}")
	@ApiOperation(value = "Get all reports for a definition", notes = "Returns all reports for a definition in either JSON or XML format", response = ReportListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public ReportList getReportsForMapProjectAndReportType(
			@ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
			@ApiParam(value = "Report definition", required = true) @PathParam("definitionId") Long definitionId,
			@ApiParam(value = "Paging/filtering/sorting object", required = true) PfsParameterJpa pfsParameter,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Report):  /report/reports");
		String user = "";
		String projectName = "";

		try {
			// authorize call
			MapUserRole role = securityService
					.getMapProjectRoleForToken(authToken, projectId);
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) 
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve reports.")
								.build()); 

			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(projectId);
			projectName = mapProject.getName();
			mappingService.close();

			// get the reports
			ReportService reportService = new ReportServiceJpa();
			ReportDefinition reportDefinition = reportService.getReportDefinition(definitionId);
			ReportList reportList = reportService
					.getReportsForMapProjectAndReportDefinition(mapProject,
							reportDefinition, pfsParameter);
			reportService.close();

			return reportList;
		} catch (Exception e) {
			handleException(e, "trying to retrieve all reports", user,
					projectName, "");
			return null;
		}

	}
	
	@POST
	@Path("/report/generate/project/id/{projectId}/user/id/{userName}")
	@ApiOperation(value = "Generate a report", notes = "Generates and returns a report given a definition, in either JSON or XML format", response = ReportJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Report generateReport(
			@ApiParam(value = "The report definition", required = true) ReportDefinitionJpa reportDefinition,
			@ApiParam(value = "Map project id", required = true) @PathParam("projectId") Long projectId,
			@ApiParam(value = "User generating report", required = true) @PathParam("userName") String userName,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken) {

		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Report):  /report/generate/project/id/"
						+ projectId + "/user/id/" + userName + " with report definition "
						+ reportDefinition.getName());

		String mapProjectName = "(not retrieved)";

		Report report = null;

		try {
			// authorize call
			MapUserRole role = securityService
					.getMapProjectRoleForToken(authToken, projectId);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) 
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve reports.")
								.build()); 

			// get the required objects
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject = mappingService.getMapProject(projectId);
			mapProjectName = mapProject.getName(); // for error handling
			MapUser mapUser = mappingService.getMapUser(userName);
			mappingService.close();

			ReportService reportService = new ReportServiceJpa();
			report = reportService.generateReport(mapProject, mapUser,
					reportDefinition.getName(), reportDefinition, null,
					false);
			reportService.close();

			return report;
		} catch (Exception e) {

			handleException(e, "trying to generate a report", userName,
					mapProjectName, "");
			return null;

		}
	}

	@POST
	@Path("/reportResult/id/{reportResultId}/items")
	@ApiOperation(value = "Gets report result items", notes = "Returns paged report result items in either JSON or XML format", response = ReportJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
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
			MapProject mapProject = mappingService.getMapProject(report.getMapProjectId());
			
			// authorize call
			MapUserRole role = securityService
					.getMapProjectRoleForToken(authToken, mapProject.getId());
			user = securityService.getUsernameForToken(authToken);
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST))
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to retrieve report result items.")
								.build());

			
			ReportResultItemList reportResultItemList = reportService
					.getReportResultItemsForReportResult(reportResultId,
							pfsParameter);

			reportService.close();

			return reportResultItemList;
		} catch (Exception e) {

			handleException(e, "trying to retrieve report result items", user,
					reportResultId.toString(), "");
			return null;

		}
	}

}
