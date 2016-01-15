package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of tree positions with descriptions.
 */
public class TreePositionDescriptionGroupJpa implements
    TreePositionDescriptionGroup {

  /** The type id. */
  private String typeId;

  /** The name. */
  private String name;

  /** The tree position descriptions. */
  private List<TreePositionDescription> treePositionDescriptions =
      new ArrayList<>();

  /* see superclass */
  @Override
  public String getTypeId() {
    return typeId;
  }

  /* see superclass */
  @Override
  public void setTypeId(String typeId) {
    this.typeId = typeId;
  }

  /* see superclass */
  @Override
  public List<TreePositionDescription> getTreePositionDescriptions() {
    return treePositionDescriptions;
  }

  /* see superclass */
  @Override
  public void setTreePositionDescriptions(
    List<TreePositionDescription> treePositionDescriptions) {
    this.treePositionDescriptions = treePositionDescriptions;
  }

  /* see superclass */
  @Override
  public void addTreePositionDescription(
    TreePositionDescription treePositionDescription) {
    if (this.treePositionDescriptions == null)
      this.treePositionDescriptions = new ArrayList<>();
    this.treePositionDescriptions.add(treePositionDescription);
  }

  /* see superclass */
  @Override
  public void removeTreePositionDescription(
    TreePositionDescription treePositionDescription) {
    if (this.treePositionDescriptions == null)
      this.treePositionDescriptions = new ArrayList<>();
    this.treePositionDescriptions.remove(treePositionDescription);
  }

  /* see superclass */
  @Override
  public String getName() {
    return name;
  }

  /* see superclass */
  @Override
  public void setName(String name) {
    this.name = name;
  }

}
