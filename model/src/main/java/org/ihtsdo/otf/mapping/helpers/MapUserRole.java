package org.ihtsdo.otf.mapping.helpers;

/**
 * Enum for available user application and project roles.
 *
 */
public enum MapUserRole {

  /** No role. */
  NONE("None"),

  /** The viewer. */
  VIEWER("Viewer"),

  /** The specialist. */
  SPECIALIST("Specialist"),

  /** The lead. */
  LEAD("Lead"),

  /** The administrator. */
  ADMINISTRATOR("Administrator");

  /**  The value. */
  private String value;

  /**
   * Instantiates a {@link MapUserRole} from the specified parameters.
   *
   * @param value the value
   */
  private MapUserRole(String value) {
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

  /**
   * Checks for privileges of.
   *
   * @param role the role
   * @return true, if successful
   */
  public boolean hasPrivilegesOf(MapUserRole role) {
    if (this.equals(MapUserRole.VIEWER) && role.equals(MapUserRole.VIEWER))
      return true;
    else if (this.equals(MapUserRole.SPECIALIST)
        && (role.equals(MapUserRole.VIEWER) || role
            .equals(MapUserRole.SPECIALIST)))
      return true;
    else if (this.equals(MapUserRole.LEAD)
        && (role.equals(MapUserRole.VIEWER)
            || role.equals(MapUserRole.SPECIALIST) || role
              .equals(MapUserRole.LEAD)))
      return true;
    else if (this.equals(MapUserRole.ADMINISTRATOR))
      return true;
    else
      return false;
  }

}
