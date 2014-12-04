package org.ihtsdo.otf.mapping.helpers;

/**
 * The Enum WorkflowType specifying what type of workflow a map project uses.
 */
public enum WorkflowType {

  /** Two specialists map, lead reviews conflicts. */
  CONFLICT_PROJECT("Conflict Project"),

  /** One specialist maps, lead reviews result. */
  REVIEW_PROJECT("Review Project");

  /** The display name. */
  private String displayName = null;

  /**
   * Instantiates a {@link WorkflowType} from the specified parameters.
   *
   * @param displayName the display name
   */
  private WorkflowType(String displayName) {
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
