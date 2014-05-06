package org.ihtsdo.otf.mapping.services;

import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.model.MapRecord;

/**
 * Validation service for map records.
 */
public interface ValidationService {

  /**
   * Validate map record.
   * 
   * @param mapRecord the map record
   * @return the validation result
   * @throws Exception the exception
   */
  public ValidationResult validateMapRecord(MapRecord mapRecord)
    throws Exception;
}
