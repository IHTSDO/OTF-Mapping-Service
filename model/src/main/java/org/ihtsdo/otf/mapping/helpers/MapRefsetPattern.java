package org.ihtsdo.otf.mapping.helpers;

/**
 * Enum specifying what map refset pattern a map project uses.
 */
public enum MapRefsetPattern {

  /** The Extended map. */
  ExtendedMap("Extended Map"),

  /** The Complex map. */
  ComplexMap("Complex Map"),

  /** The Simple map. */
  SimpleMap("Simple Map");

  /** The display name. */
  private String displayName = null;

  /**
   * Instantiates a {@link MapRefsetPattern} from the specified parameters.
   *
   * @param displayName the display name
   */
  private MapRefsetPattern(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Returns the display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }
}
