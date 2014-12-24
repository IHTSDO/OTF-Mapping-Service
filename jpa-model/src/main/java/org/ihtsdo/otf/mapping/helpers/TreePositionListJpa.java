package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.rf2.jpa.TreePositionJpa;

/**
 * JAXB enabled implementation of {@link TreePositionList}.
 */
@XmlRootElement(name = "treePositionList")
public class TreePositionListJpa extends AbstractResultList<TreePosition>
    implements TreePositionList {

  /** The map treePositions. */
  private List<TreePosition> treePositions = new ArrayList<>();

  /**
   * Instantiates a new map treePosition list.
   */
  public TreePositionListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.TreePositionList#addTreePosition(org.ihtsdo
   * .otf.mapping.rf2.TreePosition)
   */
  @Override
  public void addTreePosition(TreePosition TreePosition) {
    treePositions.add(TreePosition);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.TreePositionList#removeTreePosition(org.
   * ihtsdo.otf.mapping.rf2.TreePosition)
   */
  @Override
  public void removeTreePosition(TreePosition TreePosition) {
    treePositions.remove(TreePosition);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.TreePositionList#setTreePositions(java.util
   * .List)
   */
  @Override
  public void setTreePositions(List<TreePosition> TreePositions) {
    this.treePositions = new ArrayList<>();
    if (TreePositions != null) {
      this.treePositions.addAll(TreePositions);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.TreePositionList#getTreePositions()
   */
  @Override
  @XmlElement(type = TreePositionJpa.class, name = "treePosition")
  public List<TreePosition> getTreePositions() {
    return treePositions;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  public int getCount() {
    return treePositions.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<TreePosition> comparator) {
    Collections.sort(treePositions, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(TreePosition element) {
    return treePositions.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<TreePosition> getIterable() {
    return treePositions;
  }

}
