package org.ihtsdo.otf.mapping.rf2.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.rf2.TreePosition;

/**
 * Concrete implementation of {@link TreePosition} for use with JPA.
 *
 */
@Entity
@Table(name = "tree_positions")
public class TreePositionJpa implements TreePosition {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	/** The ancestor path. */
	@Column(nullable = false, length = 400)
	private String ancestorPath;
	
	/** The terminology. */
	@Column(nullable = false)
	private String terminology;

	/** The concept id */
	@Column(nullable = false)
	private String conceptId;

	/** The terminology version. */
	@Column(nullable = false)
	private String terminologyVersion;


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
	public String getTerminology() {
		return terminology;
	}


	@Override
	public void setTerminology(String terminology) {
		this.terminology = terminology;
	}


	@Override
	public String getTerminologyVersion() {
		return terminologyVersion;
	}


	@Override
	public void setTerminologyVersion(String terminologyVersion) {
		this.terminologyVersion = terminologyVersion;
	}


	@Override
	public String getConceptId() {
		return conceptId;
	}


	@Override
	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result =
				prime * result + ((ancestorPath == null) ? 0 : ancestorPath.hashCode());
		result = prime * result + ((conceptId == null) ? 0 : conceptId.hashCode());
		result =
				prime * result + ((terminology == null) ? 0 : terminology.hashCode());
		result =
				prime
						* result
						+ ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
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
		if (conceptId == null) {
			if (other.conceptId != null)
				return false;
		} else if (!conceptId.equals(other.conceptId))
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


	@Override
	public String toString() {
		return "TreePositionJpa [ancestorPath=" + ancestorPath + ", terminology="
				+ terminology + ", conceptId=" + conceptId + ", terminologyVersion="
				+ terminologyVersion + "]";
	}



}
