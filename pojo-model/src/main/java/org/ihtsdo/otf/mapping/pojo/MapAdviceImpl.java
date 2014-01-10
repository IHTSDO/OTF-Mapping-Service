package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.MapAdvice;

// TODO: Auto-generated Javadoc
/**
 * Reference implementation of {@link MapAdvice}.
 * Includes hibernate tags for MEME database.
 *
 * @author ${author}
 */
public class MapAdviceImpl implements MapAdvice {

	/** The id. */
	private Long id;
	
	/** The name. */
	private String name;
	
	/** The detail. */
	private String detail;
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#getDetail()
	 */
	@Override
	public String getDetail() {
		return detail;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#setDetail(java.lang.String)
	 */
	@Override
	public void setDetail(String detail) {
		this.detail = detail;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result =
				prime * result + ((detail == null) ? 0 : detail.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		MapAdviceImpl other = (MapAdviceImpl) obj;
		if (detail == null) {
			if (other.detail != null)
				return false;
		} else if (!detail.equals(other.detail))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

}
