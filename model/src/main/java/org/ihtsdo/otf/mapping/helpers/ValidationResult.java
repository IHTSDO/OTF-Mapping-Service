package org.ihtsdo.otf.mapping.helpers;

import java.util.Set;

/**
 * The Interface ValidationResult.
 */
public interface ValidationResult {

  /**
   * Checks if is valid.
   * 
   * @return true, if is valid
   */
  public boolean isValid();

  /**
   * Gets the errors.
   * 
   * @return the errors
   */
  public Set<String> getErrors();

  /**
   * Sets the errors.
   * 
   * @param errors the new errors
   */
  public void setErrors(Set<String> errors);

  /**
   * Gets the warnings.
   * 
   * @return the warnings
   */
  public Set<String> getWarnings();

  /**
   * Sets the warnings.
   * 
   * @param warnings the new warnings
   */
  public void setWarnings(Set<String> warnings);

  /**
   * Removewarning.
   * 
   * @param warning the warning
   */
  public void removeWarning(String warning);

  /**
   * Addwarning.
   * 
   * @param warning the warning
   */
  public void addWarning(String warning);

  /**
   * Removes the error.
   * 
   * @param error the error
   */
  public void removeError(String error);

  /**
   * Adds the error.
   * 
   * @param error the error
   */
  public void addError(String error);

  /**
   * Adds the warnings.
   * 
   * @param warnings the warnings
   */
  public void addWarnings(Set<String> warnings);

  /**
   * Adds the errors.
   * 
   * @param errors the errors
   */
  public void addErrors(Set<String> errors);

  /**
   * Merge a second validation result into this validation result
   * 
   * @param validationResult the validation result
   */
  public void merge(ValidationResult validationResult);

}
