package org.ihtsdo.otf.mapping.workflow;

import java.util.Date;

import org.ihtsdo.otf.mapping.model.MapUser;

public interface UserError {

	void setUserReportingError(MapUser userReportingError);

	MapUser getUserReportingError();

	void setUserInError(MapUser userInError);

	MapUser getUserInError();

	void setNote(String note);

	String getNote();

	void setError(String error);

	String getError();

	void setTimestamp(Date timestamp);

	Date getTimestamp();

	void setMapRecordId(Long mapRecordId);

	Long getMapRecordId();

	void setId(Long id);

	Long getId();

}
