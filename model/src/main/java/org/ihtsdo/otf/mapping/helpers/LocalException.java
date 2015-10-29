package org.ihtsdo.otf.mapping.helpers;


/**
 * Represents an exception for a known condition that can be simply handled and
 * reported to the user.
 */
@SuppressWarnings("serial")
public class LocalException extends Exception {

  /** The response code. */
  private String responseCode = null;

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

  /**
   * Instantiates a new local exception.
   *
   * @param message the message
   * @param responseCode the response code
   */
  public LocalException(String message, String responseCode) {
    super(message);
    this.responseCode = responseCode;
  }

  /**
   * Instantiates a new local exception.
   *
   * @param message the message
   * @param t the t
   * @param responseCode the response code
   */
  public LocalException(String message, Throwable t, String responseCode) {
    super(message, t);
    this.responseCode = responseCode;
  }

  /**
   * Gets the response code.
   *
   * @return the response code
   */
  public String getResponseCode() {
    return responseCode;
  }

  /**
   * Sets the response code.
   *
   * @param responseCode the new response code
   */
  public void setResponseCode(String responseCode) {
    this.responseCode = responseCode;
  }

}
