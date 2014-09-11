package org.ihtsdo.otf.mapping.rest;

import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ReportServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput;
import org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutputJpa;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ReportService;
import org.ihtsdo.otf.mapping.services.SecurityService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * The Workflow Services REST package.
 */
@Path("/report")
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

	/**
	 * Compute and return a report for a specialist
	 * 
	 * @throws Exception
	 */
	@GET
	@Path("/project/id/{projectId}/user/id/{userName}/start/{startTime}/end/{endTime}/specialistOutput")
	@ApiOperation(value = "Compute specialist output report", notes = "Calculates productivity and output of a specialist on a project for a given time frame")
	public MapReportSpecialistOutput computeSpecialistOutput(
			@ApiParam(value = "Map Project id", required = true) @PathParam("projectId") Long mapProjectId,
			@ApiParam(value = "Map User name", required = true) @PathParam("userName") String mapUserName,
			@ApiParam(value = "Start Time (in ms)", required = true) @PathParam("startTime") Long startTime,
			@ApiParam(value = "End Time (in ms)", required = true) @PathParam("endTime") Long endTime,
			@ApiParam(value = "Authorization token", required = true) @HeaderParam("Authorization") String authToken)
			throws Exception {
		
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping):  /report/project/id/" + mapProjectId + "/user/id/" + mapUserName + "/start/" + startTime + "/end/" + endTime + "/specialistOutput");

		System.out.println(startTime);
		System.out.println((new Date(startTime)).toString());
		
		MapReportSpecialistOutput report = new MapReportSpecialistOutputJpa();

		// instantiate the services
		MappingService mappingService = new MappingServiceJpa();
		ReportService reportService = new ReportServiceJpa();

		// get the mapping objects
		MapProject mapProject = mappingService.getMapProject(mapProjectId);
		MapUser mapUser = mappingService.getMapUser(mapUserName);

		// initialize calling user's name
		String userName = securityService.getUsernameForToken(authToken);

		try {
			// authorize call

			MapUserRole role = securityService.getMapProjectRoleForToken(
					authToken, mapProjectId);

			// if not a lead or administrator, check that user is requesting own
			// report
			if (!role.hasPrivilegesOf(MapUserRole.LEAD)) {
				if (!userName.equals(mapUserName))
					throw new WebApplicationException(
							Response.status(401)
									.entity("User does not have permission to view another user's reports.")
									.build());
			}

			// if not a specialist, no access to reports
			if (!role.hasPrivilegesOf(MapUserRole.SPECIALIST)) {
				throw new WebApplicationException(
						Response.status(401)
								.entity("User does not have permissions to view reports.")
								.build());
			}

			report = reportService.computeSpecialistOutputReport(mapUser, mapProject,
					new Date(startTime), new Date(endTime));

		} catch (Exception e) {

			handleException(e, "trying to compute workflow", userName,
					mapProject.getName(), "");
		}

		return report;
	}
}
