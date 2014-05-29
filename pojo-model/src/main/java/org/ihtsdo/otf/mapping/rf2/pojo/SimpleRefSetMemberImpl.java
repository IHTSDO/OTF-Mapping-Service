package org.ihtsdo.otf.mapping.rf2.pojo;

import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;

/**
 * Concrete implementation of {@link SimpleRefSetMember}.
 */
public class SimpleRefSetMemberImpl extends AbstractConceptRefSetMember
    implements SimpleRefSetMember {

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
            .getTerminologyId());
  }
}
