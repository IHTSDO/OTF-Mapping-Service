package org.ihtsdo.otf.mapping.rf2;

import java.util.List;

import org.ihtsdo.otf.mapping.helpers.TreePositionDescriptionGroup;

/**
 * Generically represents a tree position.
 */
public interface TreePosition {

  /**
   * Returns the id.
   * 
   * @return the id
   */
  public Long getId();

  /**
   * Lists the id.
   * 
   * @param id the id
   */
  public void setId(Long id);

  /**
   * Returns the terminology.
   * 
   * @return the terminology
   */
  public String getTerminology();

  /**
   * Lists the terminology.
   * 
   * @param terminology the terminology
   */
  public void setTerminology(String terminology);

  /**
   * Returns the terminology version.
   * 
   * @return the terminology version
   */
  public String getTerminologyVersion();

  /**
   * Lists the terminology version.
   * 
   * @param terminologyVersion the terminology version
   */
  public void setTerminologyVersion(String terminologyVersion);

  /**
   * Returns the ancestor path. This is a "." separated list of terminology IDs
   * starting with the root.
   * 
   * @return the ancestor path
   */
  public String getAncestorPath();

  /**
   * Lists the ancestor path.
   * 
   * @param path the ancestor path
   */
  public void setAncestorPath(String path);

  /**
   * Returns the terminology id.
   * 
   * @return the terminology id
   */
  public String getTerminologyId();

  /**
   * Lists the terminology id.
   * 
   * @param terminologyId the terminology id
   */
  public void setTerminologyId(String terminologyId);

  /**
   * Lists the children.
   * 
   * @param children the new children
   */
  public void setChildren(List<TreePosition> children);

  /**
   * Gets the children.
   * 
   * @return the children
   */
  public List<TreePosition> getChildren();

  /**
   * Gets the default preferred name.
   * 
   * @return the default preferred name
   */
  public String getDefaultPreferredName();

  /**
   * Lists the default preferred name.
   * 
   * @param defaultPreferredName the new default preferred name
   */
  public void setDefaultPreferredName(String defaultPreferredName);

  /**
   * Gets the children count.
   * 
   * @return the children count
   */
  public int getChildrenCount();

  /**
   * Lists the children count.
   * 
   * @param childrenCount the new children count
   */
  public void setChildrenCount(int childrenCount);

  /**
   * Gets the descendant count.
   *
   * @return the descendant count
   */
  public int getDescendantCount();

  /**
   * Sets the descendant count.
   *
   * @param descendantCount the new descendant count
   */
  public void setDescendantCount(int descendantCount);

  /**
   * Gets the terminology note.
   * 
   * @return the terminology note
   */
  public String getTerminologyNote();

  /**
   * Lists the terminology note.
   * 
   * @param terminologyNote the new terminology note
   */
  public void setTerminologyNote(String terminologyNote);

  /**
   * Adds the child tree position. This is not a persisted field, only used for
   * data transfer.
   * 
   * @param treePosition the tree position
   */
  public void addChild(TreePosition treePosition);

  /**
   * Adds the children. This is not a persisted field, only used for data
   * transfer.
   * 
   * @param treePositions the tree positions
   */
  public void addChildren(List<TreePosition> treePositions);

  /**
   * Sets the valid. Like children, not a persisted field.
   * 
   * @param valid the new valid
   */
  public void setValid(boolean valid);

  /**
   * Checks if is valid. Like children, not a persisted field.
   * 
   * @return true, if is valid
   */
  public boolean isValid();

  /**
   * Gets the desc groups.
   *
   * @return the desc groups
   */
  public List<TreePositionDescriptionGroup> getDescGroups();

  /**
   * Sets the desc groups.
   *
   * @param descGroups the new desc groups
   */
  public void setDescGroups(List<TreePositionDescriptionGroup> descGroups);

}
