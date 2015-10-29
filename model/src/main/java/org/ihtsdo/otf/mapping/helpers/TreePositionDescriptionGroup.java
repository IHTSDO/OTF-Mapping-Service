package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

/**
 * Generically represents agroup of TreePosition format-ready descriptions.
 */
public interface TreePositionDescriptionGroup {

  /**
   * Gets the type id.
   *
   * @return the type id
   */
  public String getTypeId();

  /**
   * Sets the type id.
   *
   * @param typeId the new type id
   */
  public void setTypeId(String typeId);

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName();

  /**
   * Sets the name.
   *
   * @param name the new name
   */
  public void setName(String name);

  /**
   * Gets the descriptions.
   *
   * @return the descriptions
   */
  public List<TreePositionDescription> getTreePositionDescriptions();

  /**
   * Sets the descriptions.
   *
   * @param treePositionDescription the new tree position descriptions
   */
  public void setTreePositionDescriptions(
    List<TreePositionDescription> treePositionDescription);

  /**
   * Adds the referenced concept.
   *
   * @param treePositionDescription the tree position description
   */
  public void addTreePositionDescription(
    TreePositionDescription treePositionDescription);

  /**
   * Removes the tree position description.
   *
   * @param treePositionDescription the tree position description
   */
  public void removeTreePositionDescription(
    TreePositionDescription treePositionDescription);

}
