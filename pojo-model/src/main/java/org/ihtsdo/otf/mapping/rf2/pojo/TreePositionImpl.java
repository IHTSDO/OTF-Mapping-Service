package org.ihtsdo.otf.mapping.rf2.pojo;



import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.rf2.TreePosition;


/**
 * Reference implementation of {@link TreePosition}.
 *
 */
public class TreePositionImpl implements TreePosition {

	/**  The id. */
	private Long id;
	
	/** The ancestor path. */
	private String ancestorPath;

	/** The terminology. */
	private String terminology;

	/** The concept id */
	private String conceptId;

	/** The terminology version. */
	private String terminologyVersion;

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
	public void setAncestorPath(String path) {
		ancestorPath = path;
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
	public String toString() {
		return "TreePositionImpl [ancestorPath=" + ancestorPath + ", terminology="
				+ terminology + ", conceptId=" + conceptId + ", terminologyVersion="
				+ terminologyVersion + "]";
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
		TreePositionImpl other = (TreePositionImpl) obj;
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






}
