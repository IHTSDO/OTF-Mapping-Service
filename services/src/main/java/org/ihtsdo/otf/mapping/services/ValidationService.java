package org.ihtsdo.otf.mapping.services;

import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.model.MapRecord;

public interface ValidationService {

	
	public ValidationResult validateMapRecord(MapRecord mapRecord) throws Exception;
}
