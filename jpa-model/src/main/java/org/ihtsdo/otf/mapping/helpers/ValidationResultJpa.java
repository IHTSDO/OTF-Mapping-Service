package org.ihtsdo.otf.mapping.helpers;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * The Class ValidationResultJpa.
 */
@XmlRootElement
public class ValidationResultJpa implements ValidationResult {

  /** The general messages */
  private Set<String> messages = new HashSet<>();

  /** The errors. */
  private Set<String> errors = new HashSet<>();

  /** The warnings. */
  private Set<String> warnings = new HashSet<>();

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ValidationResult#isValid()
   */
  @Override
  public boolean isValid() {
    return errors.size() == 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ValidationResult#getMessages()
   */
  @Override
  public Set<String> getMessages() {
    return messages;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ValidationResult#setMessages(java.util
   * .Set)
   */
  @Override
  public void setMessages(Set<String> messages) {
    this.messages = messages;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ValidationResult#addMessage(java.lang.String
   * )
   */
  @Override
  public void addMessage(String message) {
    if (this.messages == null) {
      this.messages = new HashSet<>();
    }
    this.messages.add(message);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ValidationResult#addMessages(java.util.Set)
   */
  @Override
  public void addMessages(Set<String> messageSet) {
    if (this.messages != null) {
      this.messages.addAll(messages);
    } else {
      this.messages = new HashSet<>(messageSet);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ValidationResult#getErrors()
   */
  @Override
  public Set<String> getErrors() {
    return errors;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ValidationResult#setErrors(java.util.Set)
   */
  @Override
  public void setErrors(Set<String> errors) {
    this.errors = errors;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ValidationResult#addError(java.lang.String )
   */
  @Override
  public void addError(String error) {
    this.errors.add(error);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ValidationResult#addErrors(java.util.Set)
   */
  @Override
  public void addErrors(Set<String> errorSet) {
    if (this.errors != null) {
      this.errors.addAll(errorSet);
    } else {
      this.errors = new HashSet<>(errorSet);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ValidationResult#removeError(java.lang
   * .String )
   */
  @Override
  public void removeError(String error) {
    this.errors.remove(error);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ValidationResult#getWarnings()
   */
  @Override
  public Set<String> getWarnings() {
    return warnings;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ValidationResult#setWarnings(java.util
   * .Set)
   */
  @Override
  public void setWarnings(Set<String> warnings) {
    this.warnings = warnings;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ValidationResult#addWarning(java.lang.
   * String )
   */
  @Override
  public void addWarning(String warning) {
    this.warnings.add(warning);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ValidationResult#addWarnings(java.util
   * .Set)
   */
  @Override
  public void addWarnings(Set<String> warningSet) {
    if (this.warnings != null)
      this.warnings.addAll(warningSet);
    else
      this.warnings = new HashSet<>(warningSet);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ValidationResult#removeWarning(java.lang
   * .String)
   */
  @Override
  public void removeWarning(String warning) {
    this.warnings.remove(warning);
  }

  @Override
  public void merge(ValidationResult validationResult) {

    this.errors.addAll(validationResult.getErrors());
    this.warnings.addAll(validationResult.getWarnings());

  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "ERRORS: " + errors + ", WARNINGS: " + warnings;
  }

}
