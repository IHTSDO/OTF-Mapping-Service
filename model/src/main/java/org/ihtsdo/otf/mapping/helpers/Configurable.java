package org.ihtsdo.otf.mapping.helpers;

import java.util.Properties;

/**
 * Generically represents something configurable by a properties object.
 */
public interface Configurable {

  /**
   * Sets the properties.
   *
   * @param properties the properties
   */
  public void setProperties(Properties properties);
}
