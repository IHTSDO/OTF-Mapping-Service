package org.ihtsdo.otf.mapping.rf2;

/**
 * Generically represents an attribute value reference set
 */
public interface AttributeValueRefSetMember extends ConceptRefSetMember {

  /**
   * returns the value id
   * @return the value id
   * 
   */
  public Long getValueId();

  /**
   * sets the value id
   * @param valueId the value id
   */
  public void setValueId(long valueId);
}
