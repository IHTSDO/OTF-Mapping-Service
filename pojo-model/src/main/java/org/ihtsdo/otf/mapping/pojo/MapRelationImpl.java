package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.MapRelation;

/**
 * Implementation of MapRelation.
 */
public class MapRelationImpl implements MapRelation {

  /** The id. */
  private Long id;

  /** The terminology id. */
  private String terminologyId;

  /** The name. */
  private String name;

  /** The abbreviated name for display */
  private String abbreviation;

  /** Whether this relation can be applied for null targets */
  private boolean isAllowableForNullTarget;

  /** Whether this relation is applied algorithmically */
  private boolean isComputed;

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#getId()
   */
  @Override
  public Long getId() {
    return id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#setId(java.lang.Long)
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#getTerminologyId()
   */
  @Override
  public String getTerminologyId() {
    return terminologyId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRelation#setTerminologyId(java.lang.String)
   */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#setName(java.lang.String)
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getAbbreviation() {
    return this.abbreviation;
  }

  @Override
  public void setAbbreviation(String abbreviation) {
    this.abbreviation = abbreviation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.model.MapRelation#isAllowableForNullTarget()
   */
  @Override
  public boolean isAllowableForNullTarget() {
    return isAllowableForNullTarget;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.model.MapRelation#setAllowableForNullTarget(boolean)
   */
  @Override
  public void setAllowableForNullTarget(boolean isAllowableForNullTarget) {
    this.isAllowableForNullTarget = isAllowableForNullTarget;
  }

  @Override
  public boolean isComputed() {
    return isComputed;
  }

  @Override
  public void setComputed(boolean isComputed) {
    this.isComputed = isComputed;

  }

}
