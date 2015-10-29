package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.rf2.TreePosition;

/**
 * Represents a sortable list of {@link TreePosition}.
 */
public interface TreePositionList extends ResultList<TreePosition> {

  /**
   * Adds the map treePosition.
   * 
   * @param TreePosition the map treePosition
   */
  public void addTreePosition(TreePosition TreePosition);

  /**
   * Removes the map treePosition.
   * 
   * @param TreePosition the map treePosition
   */
  public void removeTreePosition(TreePosition TreePosition);

  /**
   * Sets the map treePositions.
   * 
   * @param TreePositions the new map treePositions
   */
  public void setTreePositions(List<TreePosition> TreePositions);

  /**
   * Gets the map treePositions.
   * 
   * @return the map treePositions
   */
  public List<TreePosition> getTreePositions();

}
