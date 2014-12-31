package org.ihtsdo.otf.mapping.dto;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Container for key value pairs.
 */
@XmlRootElement(name = "keyValuePair")
public class KeyValuePair {

  /** The key. */
  private String key;

  /** The value. */
  private String value = "";

  /**
   * Instantiates an empty {@link KeyValuePair}.
   */
  public KeyValuePair() {
    // do nothing
  }

  /**
   * Instantiates a {@link KeyValuePair} from the specified parameters.
   * 
   * @param key the key
   * @param value the value
   */
  public KeyValuePair(String key, String value) {
    this.key = key;
    this.value = value;
  }

  /**
   * Returns the key.
   * 
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets the key.
   * 
   * @param key the key
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Sets the value.
   * 
   * @param value the value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Returns the value.
   * 
   * @return the value
   */
  public String getValue() {
    return value;
  }

}
