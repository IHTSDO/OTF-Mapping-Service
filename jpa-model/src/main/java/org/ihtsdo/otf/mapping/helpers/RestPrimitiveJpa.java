package org.ihtsdo.otf.mapping.helpers;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Generic object to contain a primitive result.
 * 
 */
@XmlRootElement(name = "restPrimitive")
public class RestPrimitiveJpa implements RestPrimitive {

  /** The value. */
  private String value;

  /** The type. */
  private String type;

  /**
   * Default constructor.
   */
  public RestPrimitiveJpa() {
    // left empty
  }

  /**
   * Constructor.
   * 
   * @param value the value
   * @param type the type
   */
  public RestPrimitiveJpa(String value, String type) {
    this.value = value;
    this.type = type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.RestPrimitive#getValue()
   */
  @Override
  @XmlElement(name = "value")
  public String getValue() {
    return value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.RestPrimitive#setValue(java.lang.String)
   */
  @Override
  public void setValue(String value) {
    this.value = value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.RestPrimitive#getType()
   */
  @Override
  public String getType() {
    return type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.RestPrimitive#setType(java.lang.String)
   */
  @Override
  public void setType(String type) {
    this.type = type;

  }

}
