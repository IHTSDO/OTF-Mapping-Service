package org.ihtsdo.otf.mapping.helpers;

/**
 * Generically represents a tree position with a referenced concept.
 */
public interface TreePositionReferencedConcept {

  /**
   * Sets the terminology id.
   *
   * @param term the terminology id
   */
  public void setTerminologyId(String term);

  /**
   * Returns the terminology id.
   *
   * @return the terminology id
   */
  public String getTerminologyId();

  /**
   * Sets the display name.
   *
   * @param displayName the display name
   */
  public void setDisplayName(String displayName);

  /**
   * Returns the display name.
   *
   * @return the display name
   */
  public String getDisplayName();

}
