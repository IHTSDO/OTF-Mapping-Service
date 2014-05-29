package org.ihtsdo.otf.mapping.rf2.pojo;

import org.ihtsdo.otf.mapping.rf2.RefSetMember;

/**
 * Abstract implementation of {@link RefSetMember}.
 */
public abstract class AbstractRefSetMember extends AbstractComponent implements
    RefSetMember {

  /** The ref set id */
  String refSetId;

  /**
   * Returns the ref set id
   * @return the ref set id
   */
  @Override
  public String getRefSetId() {
    return this.refSetId;
  }

  /**
   * Sets the ref set id
   * @param refSetId the ref set id
   */
  @Override
  public void setRefSetId(String refSetId) {
    this.refSetId = refSetId;

  }
}
