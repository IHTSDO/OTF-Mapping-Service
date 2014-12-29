package org.ihtsdo.otf.mapping.helpers;

/**
 * A tree position with concept info.
 */
public class TreePositionReferencedConceptJpa implements
    TreePositionReferencedConcept {

  /** The terminology id. */
  private String terminologyId;

  /** The display name. */
  private String displayName;

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.TreePositionReferencedConcept#setTerminologyId
   * (java.lang.String)
   */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.TreePositionReferencedConcept#getTerminologyId
   * ()
   */
  @Override
  public String getTerminologyId() {
    return this.terminologyId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.TreePositionReferencedConcept#setDisplayName
   * (java.lang.String)
   */
  @Override
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.TreePositionReferencedConcept#getDisplayName
   * ()
   */
  @Override
  public String getDisplayName() {
    return this.displayName;
  }

}
