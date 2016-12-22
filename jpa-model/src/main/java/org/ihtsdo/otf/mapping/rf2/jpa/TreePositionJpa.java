/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rf2.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.helpers.TreePositionDescriptionGroup;
import org.ihtsdo.otf.mapping.rf2.TreePosition;

/**
 * Concrete implementation of {@link TreePosition} for use with JPA.
 * 
 */
@Entity
@Indexed
// @UniqueConstraint here is being used to create an index, not to enforce
// uniqueness
@Table(name = "tree_positions", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "terminologyVersion", "id"
}))
@XmlRootElement
public class TreePositionJpa implements TreePosition {

  /** The id. */
  @Id
  @GeneratedValue
  private Long id;

  /** The ancestor path. */

  @Column(nullable = false, length = 4000)
  private String ancestorPath;

  /** The terminology. */
  @Column(nullable = false)
  private String terminology;

  /** The concept id. */
  @Column(nullable = false)
  private String terminologyId;

  /** The terminology version. */
  @Column(nullable = false)
  private String terminologyVersion;

  /** The default preferred name. */
  @Column(nullable = false, length = 256)
  private String defaultPreferredName;

  /** Flag for whether this tree position is assignable (not persisted). */
  @Transient
  private boolean valid;

  /** The desc groups. */
  @Transient
  private List<TreePositionDescriptionGroup> descGroups = new ArrayList<>();

  /** The children count. */
  @Column(nullable = false)
  private int childrenCount;

  /** The descendant count. */
  @Column(nullable = false)
  private int descendantCount;

  /** Terminology notes. */
  @Transient
  private String terminologyNote;

  /**
   * The children of this TreePosition (NOT persisted) Not persisted -- used for
   * terminology browsing.
   */
  @Transient
  private List<TreePosition> children = new ArrayList<>();

  /**
   * Instantiates an empty {@link TreePosition}.
   */
  public TreePositionJpa() {
    // empty
  }

  /**
   * Instantiates a {@link TreePositionJpa} from the specified parameters.
   * 
   * @param ancestorPath the ancestor path
   */
  public TreePositionJpa(String ancestorPath) {
    this.ancestorPath = ancestorPath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @XmlTransient
  public Long getId() {
    return this.id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setId(Long id) {
    this.id = id;
  }


  /* see superclass */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getAncestorPath() {
    return ancestorPath;
  }

  /* see superclass */
  @Override
  public void setAncestorPath(String ancestorPath) {
    this.ancestorPath = ancestorPath;
  }

  /* see superclass */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getTerminology() {
    return terminology;
  }

  /* see superclass */
  @Override
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /* see superclass */
  @Override
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  public String getTerminologyVersion() {
    return terminologyVersion;
  }

  /* see superclass */
  @Override
  public void setTerminologyVersion(String terminologyVersion) {
    this.terminologyVersion = terminologyVersion;
  }


  @Fields({
      @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO),
      @Field(name = "terminologyIdAnalyzed", index = Index.YES, analyze = Analyze.YES, store = Store.NO, analyzer = @Analyzer(definition = "noStopWord"))
  })
  @Override
  public String getTerminologyId() {
	  return terminologyId;
  }

  /* see superclass */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;
  }

  /* see superclass */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  @Analyzer(definition = "noStopWord")
  public String getDefaultPreferredName() {
    return defaultPreferredName;
  }

  /* see superclass */
  @Override
  public void setDefaultPreferredName(String defaultPreferredName) {
    this.defaultPreferredName = defaultPreferredName;
  }

  /* see superclass */
  @Override
  public int getChildrenCount() {
    return childrenCount;
  }

  /* see superclass */
  @Override
  public void setChildrenCount(int childrenCount) {
    this.childrenCount = childrenCount;
  }

  /* see superclass */
  @Override
  public int getDescendantCount() {
    return descendantCount;
  }

  /* see superclass */
  @Override
  public void setDescendantCount(int descendantCount) {
    this.descendantCount = descendantCount;
  }

  /**
   * Transient required as this is used only for display purposes.
   *
   * @return the terminology note
   */
  @Override
  @Transient
  public String getTerminologyNote() {
    return terminologyNote;
  }

  /* see superclass */
  @Override
  public void setTerminologyNote(String terminologyNote) {
    this.terminologyNote = terminologyNote;
  }

  /**
   * This is not a persisted set, only used for XML/JSON serialization.
   *
   * @return the children
   */
  @Override
  @Transient
  @XmlElement(type = TreePositionJpa.class)
  public List<TreePosition> getChildren() {

    Collections.sort(this.children, new Comparator<TreePosition>() {
      @Override
      public int compare(TreePosition tp1, TreePosition tp2) {
        return tp1.getTerminologyId().compareTo(tp2.getTerminologyId());
      }
    });
    return children;
  }

  /* see superclass */
  @Override
  public void setChildren(List<TreePosition> children) {
    this.children = children;
  }

  /* see superclass */
  @Override
  public void addChild(TreePosition treePosition) {

    // check if this child is already present
    int index = this.children.indexOf(treePosition);

    // if present, add children of this tree position to the existing object
    if (index != -1) {
      this.children.get(index).addChildren(treePosition.getChildren());

      // otherwise, add it
    } else {
      this.children.add(treePosition);
    }

  }

  /* see superclass */
  @Override
  public void addChildren(List<TreePosition> treePositions) {

    // for each child, call the addChild function
    for (TreePosition tp : treePositions) {
      this.addChild(tp);
    }
  }

  /* see superclass */
  @Override
  @Transient
  @XmlElement
  public boolean isValid() {
    return valid;
  }

  /* see superclass */
  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  /* see superclass */
  @Override
  public List<TreePositionDescriptionGroup> getDescGroups() {
    return descGroups;
  }

  /* see superclass */
  @Override
  public void setDescGroups(List<TreePositionDescriptionGroup> descGroups) {
    this.descGroups = descGroups;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((ancestorPath == null) ? 0 : ancestorPath.hashCode());
    result =
        prime
            * result
            + ((defaultPreferredName == null) ? 0 : defaultPreferredName
                .hashCode());
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result =
        prime * result
            + ((terminologyId == null) ? 0 : terminologyId.hashCode());
    result =
        prime
            * result
            + ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TreePositionJpa other = (TreePositionJpa) obj;
    if (ancestorPath == null) {
      if (other.ancestorPath != null)
        return false;
    } else if (!ancestorPath.equals(other.ancestorPath))
      return false;
    if (defaultPreferredName == null) {
      if (other.defaultPreferredName != null)
        return false;
    } else if (!defaultPreferredName.equals(other.defaultPreferredName))
      return false;
    if (terminology == null) {
      if (other.terminology != null)
        return false;
    } else if (!terminology.equals(other.terminology))
      return false;
    if (terminologyId == null) {
      if (other.terminologyId != null)
        return false;
    } else if (!terminologyId.equals(other.terminologyId))
      return false;
    if (terminologyVersion == null) {
      if (other.terminologyVersion != null)
        return false;
    } else if (!terminologyVersion.equals(other.terminologyVersion))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    String childrenStr = "";
    for (TreePosition child : this.getChildren()) {
      childrenStr += child.getTerminologyId() + "-";
    }
    return "TreePositionJpa [ancestorPath=" + ancestorPath + ", terminology="
        + terminology + ", terminologyId=" + terminologyId
        + ", terminologyVersion=" + terminologyVersion
        + ", defaultPreferredName=" + defaultPreferredName + ", childrenCount="
        + childrenCount + ", terminologyNote=" + terminologyNote
        + ", children=" + childrenStr + "]";
  }

}
