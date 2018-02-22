package org.ihtsdo.otf.mapping.helpers;

/**
 * Represents a thing that has an id.
 */
public interface HasId {

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

}
