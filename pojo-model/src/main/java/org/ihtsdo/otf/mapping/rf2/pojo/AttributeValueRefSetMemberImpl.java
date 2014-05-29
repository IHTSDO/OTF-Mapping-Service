package org.ihtsdo.otf.mapping.rf2.pojo;

import org.ihtsdo.otf.mapping.rf2.AttributeValueRefSetMember;

/**
 * Concrete implementation of {@link AttributeValueRefSetMember}.
 */
public class AttributeValueRefSetMemberImpl extends AbstractConceptRefSetMember
    implements AttributeValueRefSetMember {

  /** The value id */
  private Long valueId;

  /**
   * Returns the value id
   * @return the value id
   */
  @Override
  public Long getValueId() {
    return this.valueId;
  }

  /**
   * Sets the value id
   * @param valueId the value id
   */
  @Override
  public void setValueId(long valueId) {
    this.valueId = valueId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this.getId()
        + ","
        + this.getTerminology()
        + ","
        + this.getTerminologyId()
        + ","
        + this.getTerminologyVersion()
        + ","
        + this.getEffectiveTime()
        + ","
        + this.isActive()
        + ","
        +

        (this.getConcept() == null ? null : this.getConcept()
            .getTerminologyId()) + "," + this.getValueId();
  }

}
