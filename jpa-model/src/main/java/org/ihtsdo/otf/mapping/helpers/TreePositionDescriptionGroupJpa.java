package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
		Comparator<TreePositionDescription> comparator = new Comparator<TreePositionDescription>() {

			@Override
			public int compare(final TreePositionDescription o1, final TreePositionDescription o2) {
				if (o1.getName().contains(":") && o2.getName().contains(":")) {
					String o1Prefix = o1.getName().substring(0, o1.getName().indexOf(':'));
					String o2Prefix = o2.getName().substring(0, o2.getName().indexOf(':'));
					if (!o1Prefix.equals(o2Prefix)) {
						return o1.getName().compareToIgnoreCase(o2.getName());
					} else if (!o1.getName().endsWith(")")) {
						return -1;
					} else if (!o2.getName().endsWith(")")) {
						return 1;
					} else {
						return o1.getName().compareToIgnoreCase(o2.getName());
					}
				} else {
					return o1.getName().compareToIgnoreCase(o2.getName());
				}
			}

		};
		Collections.sort(treePositionDescriptions, comparator);
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
