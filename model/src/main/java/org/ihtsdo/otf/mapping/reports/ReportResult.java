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

/*	public List<ReportNote> getNotes();

	public void setNotes(List<ReportNote> notes);*/

}
