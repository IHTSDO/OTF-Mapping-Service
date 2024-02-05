/**
 * 
 */
package org.ihtsdo.otf.mapping.rest.client;

import java.io.InputStream;

import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ReportDefinitionList;
import org.ihtsdo.otf.mapping.helpers.ReportList;
import org.ihtsdo.otf.mapping.helpers.ReportResultItemList;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.jpa.services.rest.ReportServiceRest;
import org.ihtsdo.otf.mapping.reports.Report;
import org.ihtsdo.otf.mapping.reports.ReportDefinition;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;
import org.ihtsdo.otf.mapping.reports.ReportJpa;

/**
 * @author Nuno Marques
 *
 */
public class ReportClientRest extends RootClientRest implements ReportServiceRest {

	@Override
	public ReportDefinitionList getReportDefinitions(String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReportDefinition getReportDefinition(Long id, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReportDefinition addReportDefinition(ReportDefinitionJpa reportDefinition, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateReportDefinitions(ReportDefinitionJpa definition, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeReportDefinitions(ReportDefinitionJpa reportDefinition, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addReport(ReportJpa report, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeReport(ReportJpa report, String authToken) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Report getReport(Long projectId, Long id, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReportList getReportsForMapProject(Long projectId, PfsParameterJpa pfsParameter, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReportList getReportsForMapProjectAndReportDefinition(Long projectId, Long definitionId,
			PfsParameterJpa pfsParameter, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Report getLatestReport(Long projectId, Long definitionId, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Report generateReport(ReportDefinitionJpa reportDefinition, Long projectId, String userName,
			String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean testReport(ReportDefinitionJpa reportDefinition, Long projectId, String userName, String authToken)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReportResultItemList getReportResultItems(PfsParameterJpa pfsParameter, Long reportResultId,
			String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReportDefinitionList getQACheckDefinitions(String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultList getQALabels(Long mapProjectId, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream exportReport(Long reportId, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String executeReport(final String userName, final String authToken, final String reportName)
	      throws Exception {
	    // TODO Auto-generated method stub
        return null;
	}

}
