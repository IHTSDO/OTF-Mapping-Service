package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of tree positions with descriptions.
 */
public class TreePositionDescriptionGroupJpa implements
    TreePositionDescriptionGroup {

  private String typeId;

  private String name;

  private List<TreePositionDescription> treePositionDescriptions =
      new ArrayList<>();

  @Override
  public String getTypeId() {
    return typeId;
  }

  @Override
  public void setTypeId(String typeId) {
    this.typeId = typeId;
  }

  @Override
  public List<TreePositionDescription> getTreePositionDescriptions() {
    return treePositionDescriptions;
  }

  @Override
  public void setTreePositionDescriptions(
    List<TreePositionDescription> treePositionDescriptions) {
    this.treePositionDescriptions = treePositionDescriptions;
  }

  @Override
  public void addTreePositionDescription(
    TreePositionDescription treePositionDescription) {
    if (this.treePositionDescriptions == null)
      this.treePositionDescriptions = new ArrayList<>();
    this.treePositionDescriptions.add(treePositionDescription);
  }

  @Override
  public void removeTreePositionDescription(
    TreePositionDescription treePositionDescription) {
    if (this.treePositionDescriptions == null)
      this.treePositionDescriptions = new ArrayList<>();
    this.treePositionDescriptions.remove(treePositionDescription);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

}
