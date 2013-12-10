package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.MapAdvice;

/**
 * Reference implementation of {@link MapAdvice}.
 * Includes hibernate tags for MEME database.
 */
public class MapAdviceImpl implements MapAdvice {

	/** The id. */
	private Long id;
	
	/** The name. */
	private String name;
	
	/** The description. */
	private String description;
	
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
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.model.MapAdvice#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(String description) {
		this.description = description;
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

}
