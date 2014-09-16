package org.ihtsdo.otf.mapping.services;

import java.util.Date;

import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.reports.MapReportSpecialistOutput;

// TODO: Auto-generated Javadoc
/**
 * The Interface ReportService.
 */
public interface ReportService {

	
	/**
	 * Close the service.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	public void close() throws Exception;
	
	/**
	 * UserReportSpecialistOutput.
	 *
	 * @param specialistOutputId the specialist output id
	 * @return the user report specialist output
	 */
	public MapReportSpecialistOutput getUserReportSpecialistOutput(Long specialistOutputId);
	
	/**
	 * Adds the user report specialist output.
	 *
	 * @param specialistOutput the specialist output
	 * @return the map report specialist output
	 */
	public MapReportSpecialistOutput addUserReportSpecialistOutput(MapReportSpecialistOutput specialistOutput);
	
	/**
	 * Update user report specialist output.
	 *
	 * @param specialistOutput the specialist output
	 */
	public void updateUserReportSpecialistOutput(MapReportSpecialistOutput specialistOutput);
	
	/**
	 * Removes the user report specialist output.
	 *
	 * @param specialistOutputId the specialist output id
	 */
	public void removeUserReportSpecialistOutput(Long specialistOutputId);

	/**
	 * Compute specialist output report.
	 *
	 * @param mapUser the map user
	 * @param mapProject the map project
	 * @param startDate the start date
	 * @param endDate the end date
	 * @return the map report specialist output
	 * @throws Exception 
	 */
	public MapReportSpecialistOutput computeSpecialistOutputReport(MapUser mapUser,
			MapProject mapProject, Date startDate, Date endDate) throws Exception;
}
