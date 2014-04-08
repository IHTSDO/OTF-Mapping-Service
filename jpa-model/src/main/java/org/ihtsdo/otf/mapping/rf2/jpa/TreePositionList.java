package org.ihtsdo.otf.mapping.rf2.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.rf2.TreePosition;

/**
 * Container for map treePositions.
 */
@XmlRootElement(name = "treePositionList")
public class TreePositionList {

	/** The map treePositions. */
	private List<TreePosition> TreePositions = new ArrayList<>();

	/**
	 * Instantiates a new map treePosition list.
	 */
	public TreePositionList() {
		// do nothing
	}

	/**
	 * Adds the map treePosition.
	 * 
	 * @param TreePosition
	 *            the map treePosition
	 */
	public void addTreePosition(TreePosition TreePosition) {
		TreePositions.add(TreePosition);
	}

	/**
	 * Removes the map treePosition.
	 * 
	 * @param TreePosition
	 *            the map treePosition
	 */
	public void removeTreePosition(TreePosition TreePosition) {
		TreePositions.remove(TreePosition);
	}

	/**
	 * Sets the map treePositions.
	 * 
	 * @param TreePositions
	 *            the new map treePositions
	 */
	public void setTreePositions(List<TreePosition> TreePositions) {
		this.TreePositions = new ArrayList<>();
		if (TreePositions != null) {
			this.TreePositions.addAll(TreePositions);
		}
		
		
	}
	
	/**
	 * Sorts the map treePositions alphabetically by name
	 */
	public void sortTreePositions() {
	
		Collections.sort(this.TreePositions,
			new Comparator<TreePosition>() {
				@Override
				public int compare(TreePosition o1, TreePosition o2) {
					return o1.getTerminologyId().compareTo(o2.getTerminologyId());
				}

			});
}

	/**
	 * Gets the map treePositions.
	 * 
	 * @return the map treePositions
	 */
	@XmlElement(type=TreePositionJpa.class, name="treePosition")
	public List<TreePosition> getTreePositions() {
		return TreePositions;
	}

}
