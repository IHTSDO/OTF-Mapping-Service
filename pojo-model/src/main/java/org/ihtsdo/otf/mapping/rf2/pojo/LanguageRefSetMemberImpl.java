package org.ihtsdo.otf.mapping.rf2.pojo;

import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;

/**
 * Concrete implementation of {@link LanguageRefSetMember}.
 */
public class LanguageRefSetMemberImpl extends AbstractDescriptionRefSetMember
    implements LanguageRefSetMember {

  /** the acceptability id */
  private Long acceptabilityId;

  /**
   * returns the acceptability id
   * @return the acceptability id
   */
  @Override
  public Long getAcceptabilityId() {
    return this.acceptabilityId;
  }

  /**
   * sets the acceptability id
   * @param acceptabilityId the acceptability id
   */
  @Override
  public void setAcceptabilityId(Long acceptabilityId) {
    this.acceptabilityId = acceptabilityId;

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {

    return this.getId() + ","
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
        + this.getModuleId()
        + ","
        + // end of basic component fields

        (this.getDescription() == null ? null : this.getDescription()
            .getTerminologyId());

  }
}
