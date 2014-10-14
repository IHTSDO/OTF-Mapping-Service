package org.ihtsdo.otf.mapping.reports;

import java.util.List;

public interface ReportResult {

	public Report getReport();

	public void setReport(Report report);

	public Long getId();

	public void setId(Long id);

	public String getName();

	public void setName(String name);

	public String getValue();

	public void setValue(String value);

	public long getCt();

	public void setCt(long ct);

	public List<ReportResultItem> getReportResultItems();

	public void setReportResultItems(List<ReportResultItem> reportResultItems);

	public String getProjectName();

	public void setProjectName(String projectName);

	public String getDateValue();

	public void setDateValue(String dateValue);

	public void addReportResultItem(ReportResultItem reportResultItem);

}
