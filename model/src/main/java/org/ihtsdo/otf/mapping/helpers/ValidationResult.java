package org.ihtsdo.otf.mapping.helpers;

import java.util.Set;

/**
 * Generically represents the result of a validation check.
 */
public interface ValidationResult {

  /**
   * Checks if is valid.
   * 
   * @return true, if is valid
   */
  public boolean isValid();

  /**
   * Gets the general messages.
   *
   * @return the messages
   */
  public Set<String> getMessages();

  /**
   * Sets the messages.
   *
   * @param messages the new messages
   */
  public void setMessages(Set<String> messages);

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
   * Merge a second validation result into this validation result.
   *
   * @param validationResult the validation result
   */
  public void merge(ValidationResult validationResult);

  /**
   * Adds the messages.
   *
   * @param messageSet the message set
   */
  public void addMessages(Set<String> messageSet);

  /**
   * Adds the message.
   *
   * @param message the message
   */
  public void addMessage(String message);
  
  /**
   * Returns the concise errors.
   *
   * @return the concise errors
   */
  public Set<String> getConciseErrors();

  /**
   * Sets the concise errors.
   *
   * @param errors the concise errors
   */
  public void setConciseErrors(Set<String> errors);
  
  /**
   * Adds the concise error.
   *
   * @param error the error
   */
  public void addConciseError(String error);
  
  /**
   * Removes the concise error.
   *
   * @param error the error
   */
  public void removeConciseError(String error);
  
  /**
   * Adds the concise errors.
   *
   * @param errors the errors
   */
  public void addConciseErrors(Set<String> errors);
  

}
