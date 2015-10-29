package org.ihtsdo.otf.mapping.helpers;

/**
 * Generically represents a search result.
 */
public interface SearchResult {

  /**
   * @return the id
   */
  public Long getId();

  /**
   * @param id the id to set
   */
  public void setId(Long id);

  /**
   * @return the terminologyId
   */
  public String getTerminologyId();

  /**
   * @param terminologyId the terminologyId to set
   */
  public void setTerminologyId(String terminologyId);

  /**
   * @return the terminology
   */
  public String getTerminology();

  /**
   * @param terminology the terminology to set
   */
  public void setTerminology(String terminology);

  /**
   * @return the terminologyVersion
   */
  public String getTerminologyVersion();

  /**
   * @param terminologyVersion the terminologyVersion to set
   */
  public void setTerminologyVersion(String terminologyVersion);

  /**
   * @return the value
   */
  public String getValue();

  /**
   * @param value the value to set
   */
  public void setValue(String value);

  /**
   * Returns the value2.
   *
   * @return the value2
   */
  public String getValue2();

  /**
   * Sets the value2.
   *
   * @param value2 the value2
   */
  public void setValue2(String value2);

}
