package org.ihtsdo.otf.mapping.rf2.pojo;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.rf2.TreePosition;

/**
 * Reference implementation of {@link TreePosition}.
 * 
 */
public class TreePositionImpl implements TreePosition {

  /** The id. */
  private Long id;

  /** The ancestor path. */
  private String ancestorPath;

  /** The terminology. */
  private String terminology;

  /** The terminology id. */
  private String terminologyId;

  /** The terminology version. */
  private String terminologyVersion;

  /** The default preferred name. */
  private String defaultPreferredName;

  /** Flag for whether this concept node is a valid target code */
  private boolean valid;

  /** The count of children. */
  private int childrenCount;

  /** The terminology note */
  private String terminologyNote;

  /** The child tree positions. */
  private List<TreePosition> children = new ArrayList<>();

  /**
   * Instantiates an empty {@link TreePositionImpl}.
   */
  public TreePositionImpl() {
    // empty
  }

  /**
   * Instantiates a {@link TreePositionImpl} from the specified parameters.
   * 
   * @param path the path
   */
  public TreePositionImpl(String path) {
    this.ancestorPath = path;
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

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.TreePosition#getAncestorPath()
   */
  @Override
  public String getAncestorPath() {
    return ancestorPath;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.TreePosition#setAncestorPath(java.lang.String)
   */
  @Override
  public void setAncestorPath(String path) {
    ancestorPath = path;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.TreePosition#getTerminology()
   */
  @Override
  public String getTerminology() {
    return terminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.TreePosition#setTerminology(java.lang.String)
   */
  @Override
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.TreePosition#getTerminologyVersion()
   */
  @Override
  public String getTerminologyVersion() {
    return terminologyVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.TreePosition#setTerminologyVersion(java.lang
   * .String)
   */
  @Override
  public void setTerminologyVersion(String terminologyVersion) {
    this.terminologyVersion = terminologyVersion;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.TreePosition#getTerminologyId()
   */
  @Override
  public String getTerminologyId() {
    return terminologyId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.TreePosition#setTerminologyId(java.lang.String)
   */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "TreePositionImpl [ancestorPath=" + ancestorPath + ", terminology="
        + terminology + ", terminologyId=" + terminologyId
        + ", terminologyVersion=" + terminologyVersion + "]";
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((ancestorPath == null) ? 0 : ancestorPath.hashCode());
    result =
        prime * result
            + ((terminologyId == null) ? 0 : terminologyId.hashCode());
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result =
        prime
            * result
            + ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TreePositionImpl other = (TreePositionImpl) obj;
    if (ancestorPath == null) {
      if (other.ancestorPath != null)
        return false;
    } else if (!ancestorPath.equals(other.ancestorPath))
      return false;
    if (terminologyId == null) {
      if (other.terminologyId != null)
        return false;
    } else if (!terminologyId.equals(other.terminologyId))
      return false;
    if (terminology == null) {
      if (other.terminology != null)
        return false;
    } else if (!terminology.equals(other.terminology))
      return false;
    if (terminologyVersion == null) {
      if (other.terminologyVersion != null)
        return false;
    } else if (!terminologyVersion.equals(other.terminologyVersion))
      return false;
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.TreePosition#setChildren(java.util.List)
   */
  @Override
  public void setChildren(List<TreePosition> children) {
    this.children = children;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.TreePosition#getChildren()
   */
  @Override
  public List<TreePosition> getChildren() {
    return this.children;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.TreePosition#getDefaultPreferredName()
   */
  @Override
  public String getDefaultPreferredName() {
    return this.defaultPreferredName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.TreePosition#setDefaultPreferredName(java.lang
   * .String)
   */
  @Override
  public void setDefaultPreferredName(String defaultPreferredName) {
    this.defaultPreferredName = defaultPreferredName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.TreePosition#getChildrenCount()
   */
  @Override
  public int getChildrenCount() {
    return this.childrenCount;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.TreePosition#setChildrenCount(int)
   */
  @Override
  public void setChildrenCount(int childrenCount) {
    this.childrenCount = childrenCount;
  }

  @Override
  public String getTerminologyNote() {
    return this.terminologyNote;
  }

  @Override
  public void setTerminologyNote(String terminologyNote) {
    this.terminologyNote = terminologyNote;

  }

  @Override
  public void addChild(TreePosition treePosition) {
    this.children.add(treePosition);

  }

  @Override
  public void addChildren(List<TreePosition> treePositions) {
    this.children.addAll(treePositions);

  }

  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  @Override
  public boolean isValid() {
    return this.valid;
  }

}
