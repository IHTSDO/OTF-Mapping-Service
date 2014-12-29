package org.ihtsdo.otf.mapping.rf2;

/**
 * Generically represents a reference set member
 */
public interface RefSetMember extends Component {

  /*
   * Component methods relevant to RefSetMember attributes getId, setId
   * getEffectiveTime, setEffectiveTime isActive, setActive getModuleId,
   * setModuleId
   */

  /**
   * returns the refSetId
   * @return the id
   */
  public String getRefSetId();

  /**
   * sets the refSetId
   * 
   * @param refSetId the reference set id
   */
  public void setRefSetId(String refSetId);

  /**
   * returns the referencedComponentId
   * @return the referenced component id
   */

  /*
   * PG111513: Additional RefSet elements aren't necessary for our purposes per
   * Brian Carlsen Commented out.
   * 
   * public Long getReferencedComponentId();
   * 
   * /** sets the referencedComponentId
   * 
   * @param referencedComponentId the reference component id
   *//*
      * public void setReferencedComponentId(Long referencedComponentId);
      *//**
   * returns the attributeDescription
   * @return the attribute description
   */
  /*
   * public Long getAttributeDescription();
   *//**
   * sets the attributeDescription
   * @param attributeDescription the attribute description
   */
  /*
   * public void setAttributeDescription(Long attributeDescription);
   *//**
   * returns the attributeType
   * @return the attribute type
   */
  /*
   * public Long getAttributeType();
   *//**
   * sets the attributeType
   * @param attributeType the attribute type
   */
  /*
   * public void setAttributeType(Long attributeType);
   *//**
   * returns the attributeOrder
   * @return the attribute order
   */
  /*
   * public int getAttributeOrder();
   *//**
   * sets the attributeOrder
   * @param attributeOrder the attribute order
   */
  /*
   * public void setAttributeOrder(int attributeOrder);
   */
}