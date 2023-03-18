package org.ihtsdo.otf.mapping.model;

import java.util.Date;

/**
 * Generically represents a map note.
 */
public interface AdditionalMapEntryInfo {

  /**
   * Returns the id.
   * 
   * @return the id
   */
  public Long getId();

  /**
   * Sets the id.
   * 
   * @param id the id
   */
  public void setId(Long id);

  /**
   * Returns the name.
   * 
   * @return the name
   */
  public String getName();

  /**
   * Sets the name.
   * 
   * @param name the name
   */
  public void setName(String name);
  
  /**
   * Returns the field.
   * 
   * @return the field
   */
  public String getField();

  /**
   * Sets the field.
   * 
   * @param field the field
   */
  public void setField(String field);

  /**
   * Returns the value.
   * 
   * @return the value
   */
  public String getValue();

  /**
   * Sets the value.
   * 
   * @param value the value
   */
  public void setValue(String value);
}
