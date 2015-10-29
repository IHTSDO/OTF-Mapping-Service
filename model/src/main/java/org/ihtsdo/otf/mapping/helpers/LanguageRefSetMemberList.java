package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;

/**
 * Represents a sortable list of {@link LanguageRefSetMember}.
 */
public interface LanguageRefSetMemberList extends
    ResultList<LanguageRefSetMember> {

  /**
   * Adds the map languageRefSetMember.
   * 
   * @param LanguageRefSetMember the map languageRefSetMember
   */
  public void addLanguageRefSetMember(LanguageRefSetMember LanguageRefSetMember);

  /**
   * Removes the map languageRefSetMember.
   * 
   * @param LanguageRefSetMember the map languageRefSetMember
   */
  public void removeLanguageRefSetMember(
    LanguageRefSetMember LanguageRefSetMember);

  /**
   * Sets the map languageRefSetMembers.
   * 
   * @param LanguageRefSetMembers the new map languageRefSetMembers
   */
  public void setLanguageRefSetMembers(
    List<LanguageRefSetMember> LanguageRefSetMembers);

  /**
   * Gets the map languageRefSetMembers.
   * 
   * @return the map languageRefSetMembers
   */
  public List<LanguageRefSetMember> getLanguageRefSetMembers();

}
