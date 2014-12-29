package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.rf2.Description;

/**
 * Represents a sortable list of {@link Description}.
 */
public interface DescriptionList extends ResultList<Description> {

  /**
   * Adds the map description.
   * 
   * @param Description the map description
   */
  public void addDescription(Description Description);

  /**
   * Removes the map description.
   * 
   * @param Description the map description
   */
  public void removeDescription(Description Description);

  /**
   * Sets the map descriptions.
   * 
   * @param Descriptions the new map descriptions
   */
  public void setDescriptions(List<Description> Descriptions);

  /**
   * Gets the map descriptions.
   * 
   * @return the map descriptions
   */
  public List<Description> getDescriptions();

}
