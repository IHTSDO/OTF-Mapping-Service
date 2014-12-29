package org.ihtsdo.otf.mapping.rf2;

/**
 * Generically represents a reference set member with associated Description
 */
public interface DescriptionRefSetMember extends RefSetMember {

  /**
   * returns the Description
   * @return the Description
   */
  public Description getDescription();

  /**
   * sets the Description
   * @param description the Description
   */
  public void setDescription(Description description);
}
