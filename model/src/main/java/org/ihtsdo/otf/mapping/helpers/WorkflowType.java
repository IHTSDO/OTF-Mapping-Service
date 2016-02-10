package org.ihtsdo.otf.mapping.helpers;

/**
 * Enum specifying what type of workflow a map project uses.
 */
public enum WorkflowType {

  /** Simplest possible workflow, single user edits with no review */
  SIMPLE_PATH("Simple Workflow Path"),

  /** Specialist work compared with existing record, possible lead review */
  LEGACY_PATH("Legacy Workflow Path"),

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
