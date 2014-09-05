package org.ihtsdo.otf.mapping.helpers;

import java.lang.Exception;

/**
 * The Class LocalException.
 */
@SuppressWarnings("serial")
public class LocalException extends Exception {
  
  /**
   * Instantiates a {@link LocalException} from the specified parameters.
   *
   * @param message the message
   * @param t the t
   */
  public LocalException(String message, Throwable t) {
  	super(message, t);
  }

	/**
   * Instantiates a {@link LocalException} from the specified parameters.
   *
   * @param message the message
   */
  public LocalException(String message) {
      super(message);
      
  }
}
