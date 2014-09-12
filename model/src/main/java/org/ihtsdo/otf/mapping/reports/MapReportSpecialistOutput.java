package org.ihtsdo.otf.mapping.reports;

import java.util.Date;
import java.util.Set;

public interface MapReportSpecialistOutput extends MapReport {

	
	
	public Date getStartDate();
	
	public void setStartDate(Date startDate);

	public Date getEndDate();
	
	public void setEndDate(Date EndDate);
	
	public Long getMapProjectId();
	
	public void setMapProjectId(Long mapProjectId);
	
	public Set<Long> getMapRecordIds();
	
	public void setMapRecordIds(Set<Long> mapRecordIds);

	public void addMapRecordId(Long id);
	
	public void removeMapRecordId(Long id);

	public void setErrorCount(int errorCount);

	public int getErrorCount();

	public void setConflictCount(int conflictCount);

	public int getConflictCount();

	public void setFinishedCount(int finishedCount);

	public int getFinishedCount();

	public void setTotalCount(int totalCount);

	public int getTotalCount();
}
