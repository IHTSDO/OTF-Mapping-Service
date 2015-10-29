package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;

/**
 * Represents a sortable list of {@link ComplexMapRefSetMember}.
 */
public interface ComplexMapRefSetMemberList extends
    ResultList<ComplexMapRefSetMember> {

  /**
   * Adds the map project.
   * 
   * @param complexMapRefSetMember the map project
   */
  public void addComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember);

  /**
   * Removes the map project.
   * 
   * @param complexMapRefSetMember the map project
   */
  public void removeComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember);

  /**
   * Sets the map projects.
   * 
   * @param complexMapRefSetMembers the new map projects
   */
  public void setComplexMapRefSetMembers(
    List<ComplexMapRefSetMember> complexMapRefSetMembers);

  /**
   * Gets the map projects.
   * 
   * @return the map projects
   */
  public List<ComplexMapRefSetMember> getComplexMapRefSetMembers();

}
