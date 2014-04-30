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
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.rf2.TreePosition;

/**
 * Concrete implementation of {@link TreePosition} for use with JPA.
 *
 */
@Entity
@Indexed
@Table(name = "tree_positions",  uniqueConstraints={
	   @UniqueConstraint(columnNames={"ancestorPath", "id"}),
	   @UniqueConstraint(columnNames={"terminologyId", "id"})
	})
@XmlRootElement

public class TreePositionJpa implements TreePosition {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	/** The ancestor path. */
	@Column(nullable = false, length = 255)
	private String ancestorPath;
	
	/** The terminology. */
	@Column(nullable = false)
	private String terminology;

	/** The concept id */
	@Column(nullable = false)
	private String terminologyId;

	/** The terminology version. */
	@Column(nullable = false)
	private String terminologyVersion;
	
	/** The default preferred name. */
	@Column(nullable = false, length = 256)
	private String defaultPreferredName;
	
	/** Flag for whether this tree position is assignable (not persisted) */
	@Transient
	private boolean valid;

	/** The children count */
	@Column(nullable = false)
	private int childrenCount;
	
	/** Terminology notes */
	@Transient
	private String terminologyNote;

	/** 
	 * The children of this TreePosition (NOT persisted)
	 * Not persisted -- used for terminology browsing
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


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.rf2.TreePosition#getAncestorPath()
	 */
	@Override
	public String getAncestorPath() {
		return ancestorPath;
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.rf2.TreePosition#setAncestorPath(java.lang.String)
	 */
	@Override
	public void setAncestorPath(String ancestorPath) {
		this.ancestorPath = ancestorPath;
	}


	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)	
	public String getTerminology() {
		return terminology;
	}


	@Override
	public void setTerminology(String terminology) {
		this.terminology = terminology;
	}


	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)	
	public String getTerminologyVersion() {
		return terminologyVersion;
	}


	@Override
	public void setTerminologyVersion(String terminologyVersion) {
		this.terminologyVersion = terminologyVersion;
	}


	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)	
	public String getTerminologyId() {
		return terminologyId;
	}


	@Override
	public void setTerminologyId(String terminologyId) {
		this.terminologyId = terminologyId;
	}
	
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)	
	public String getDefaultPreferredName() {
		return defaultPreferredName;
	}

	@Override
	public void setDefaultPreferredName(String defaultPreferredName) {
		this.defaultPreferredName = defaultPreferredName;
	}

	@Override
	public int getChildrenCount() {
		return childrenCount;
	}

	@Override
	public void setChildrenCount(int childrenCount) {
		this.childrenCount = childrenCount;
	}
	
	/**
	 * Transient required as this is used only for display purposes.
	 */
	@Override
	@Transient
	public String getTerminologyNote() {
		return terminologyNote;
	}

	@Override
	public void setTerminologyNote(String terminologyNote) {
		this.terminologyNote = terminologyNote;
	}


	/**
	 * This is not a persisted set, only used for XML/JSON serialization
	 */
	@Override
	@Transient
	@XmlElement(type=TreePositionJpa.class)
	public List<TreePosition> getChildren() {
	
		Collections.sort(this.children, 
				new Comparator<TreePosition>() {
					@Override
					public int compare(TreePosition tp1, TreePosition tp2) {
						return tp1.getTerminologyId().compareTo(tp2.getTerminologyId());
					}
				}
				);
		return children;
	}

	@Override
	public void setChildren(List<TreePosition> children) {
		this.children = children;
	}
	


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
	
	@Override
	public void addChildren(List<TreePosition> treePositions) {
		
		// for each child, call the addChild function
		for (TreePosition tp : treePositions) {
			this.addChild(tp);
		}	
	}

	@Override
	@Transient
	@XmlElement
	public boolean isValid() {
		return valid;
	}

	@Override
	public void setValid(boolean valid) {
		this.valid = valid;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((ancestorPath == null) ? 0 : ancestorPath.hashCode());
		result = prime
				* result
				+ ((defaultPreferredName == null) ? 0 : defaultPreferredName
						.hashCode());
		result = prime * result
				+ ((terminology == null) ? 0 : terminology.hashCode());
		result = prime * result
				+ ((terminologyId == null) ? 0 : terminologyId.hashCode());
		result = prime
				* result
				+ ((terminologyVersion == null) ? 0 : terminologyVersion
						.hashCode());
		return result;
	}


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


	@Override
	public String toString() {
		String childrenStr = "";
		for (TreePosition child : this.getChildren()) {
			childrenStr += child.getTerminologyId() + "-";
		}
		return "TreePositionJpa [ancestorPath=" + ancestorPath
				+ ", terminology=" + terminology + ", terminologyId="
				+ terminologyId + ", terminologyVersion=" + terminologyVersion
				+ ", defaultPreferredName=" + defaultPreferredName
				+ ", childrenCount=" + childrenCount + ", terminologyNote="
				+ terminologyNote + ", children=" + childrenStr + "]";
	}



	


}
