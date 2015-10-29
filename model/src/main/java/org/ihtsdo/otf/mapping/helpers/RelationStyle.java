package org.ihtsdo.otf.mapping.helpers;

/**
 * Enum representingfwhat type of relation style a map project uses.
 */
public enum RelationStyle {

  /** The map category style. */
  MAP_CATEGORY_STYLE("Map Category Style"),

  /** The relationship style. */
  RELATIONSHIP_STYLE("Relationship Style");

  /** The display name. */
  private String displayName = null;

  /**
   * Instantiates a {@link RelationStyle} from the specified parameters.
   *
   * @param displayName the display name
   */
  private RelationStyle(String displayName) {
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
