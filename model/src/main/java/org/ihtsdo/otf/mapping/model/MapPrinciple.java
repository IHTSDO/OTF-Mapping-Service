package org.ihtsdo.otf.mapping.model;

/**
 * Generically represents a map principle from a mapping handbook/guidebook.
 */
public interface MapPrinciple {

  /**
   * Returns the id
   * @return the id
   */
  public Long getId();

  /**
   * Sets the id
   * @param id the id
   */
  public void setId(Long id);

  /**
   * Returns the id in string form
   * @return the string object id
   */
  public String getObjectId();

  /**
   * Returns the terminology-specified principleId
   * @return the principleId
   */
  public String getPrincipleId();

  /**
   * Sets the terminology-specified principleId
   * @param principleId the principleId
   */
  public void setPrincipleId(String principleId);

  /**
   * Returns the detail
   * @return the detail
   */
  public String getDetail();

  /**
   * Sets the detail
   * @param detail the detail
   */
  public void setDetail(String detail);

  /**
   * Returns the name
   * @return the name
   */
  public String getName();

  /**
   * Sets the name
   * @param name the name
   */
  public void setName(String name);

  /**
   * Returns the section reference
   * @return the section reference
   */
  public String getSectionRef();

  /**
   * Sets the section reference
   * @param sectionRef the section reference
   */
  public void setSectionRef(String sectionRef);
}
