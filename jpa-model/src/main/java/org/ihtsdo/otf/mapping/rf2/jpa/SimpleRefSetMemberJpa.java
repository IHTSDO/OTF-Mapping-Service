package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;

/**
 * Concrete implementation of {@link SimpleRefSetMember}.
 */
@Entity
@Table(name = "simple_refset_members")
// @Audited
public class SimpleRefSetMemberJpa extends AbstractConceptRefSetMember
    implements SimpleRefSetMember {

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
        + (this.getConcept() == null ? null : this.getConcept()
            .getTerminologyId());
  }

}
