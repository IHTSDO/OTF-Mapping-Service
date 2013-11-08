package org.ihtsdo.otf.mapping.pojo;

import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.model.Description;
import org.ihtsdo.otf.mapping.model.Relationship;

/**
 * Concrete implementation of {@link Concept}.
 */
public class ConceptImpl extends AbstractComponent implements Concept {

	/** The definition status id. */
	private Long definitionStatusId;

	/** The descriptions. */
	private Set<Description> descriptions = new HashSet<Description>();

	/** The relationships. */
	private Set<Relationship> relationships = new HashSet<Relationship>();

	/** The inverse relationships. */
	private Set<Relationship> inverseRelationships = new HashSet<Relationship>();

	/** The terminology. */
	private String terminology;
	
	/** The default preferred name. */
	private String defaultPreferredName;

	/**
	 * Returns the definition status id.
	 *
	 * @return the definition status id
	 */
	@Override
    public Long getDefinitionStatusId() {
		return definitionStatusId;
	}

	/**
	 * Sets the definition status id.
	 *
	 * @param definitionStatusId the definition status id
	 */
	@Override
    public void setDefinitionStatusId(Long definitionStatusId) {
		this.definitionStatusId = definitionStatusId;
	}

	/**
	 * Returns the descriptions.
	 *
	 * @return the descriptions
	 */
	@Override
    public Set<Description> getDescriptions() {
		return descriptions;
	}

	/**
	 * Sets the descriptions.
	 *
	 * @param descriptions the descriptions
	 */
	@Override
    public void setDescriptions(Set<Description> descriptions) {
		this.descriptions = descriptions;
	}

	/**
	 * Adds the description.
	 *
	 * @param description the description
	 */
	@Override
    public void addDescription(Description description) {
		description.setConcept(this);
		this.descriptions.add(description);
	}

	/**
	 * Removes the description.
	 *
	 * @param description the description
	 */
	@Override
    public void removeDescription(Description description) {
		this.descriptions.remove(description);
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public String toString() {
		return String.valueOf(getId());
	}

	/**
	 * Returns the relationships.
	 *
	 * @return the relationships
	 */
	@Override
    public Set<Relationship> getRelationships() {
		return relationships;
	}

	/**
	 * Sets the relationships.
	 *
	 * @param relationships the relationships
	 */
	@Override
    public void setRelationships(Set<Relationship> relationships) {
		this.relationships = relationships;
	}

	/**
	 * Returns the inverse relationships.
	 *
	 * @return the inverse relationships
	 */
	@Override
    public Set<Relationship> getInverseRelationships() {
		return inverseRelationships;
	}

	/**
	 * Sets the inverse relationships.
	 *
	 * @param inverseRelationships the inverse relationships
	 */
	@Override
    public void setInverseRelationships(Set<Relationship> inverseRelationships) {
		this.inverseRelationships = inverseRelationships;
	}


    /**
     * {@inheritDoc}
     */
	@Override
	public String getTerminology() {
		return terminology;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void setTerminology(String terminology) {
		this.terminology = terminology;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.rf2.data.model.Concept#getDefaultPreferredName()
	 */
	@Override
	public String getDefaultPreferredName() {
		return defaultPreferredName;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.rf2.data.model.Concept#setDefaultPreferredName(java.lang.String)
	 */
	@Override	
	public void setDefaultPreferredName(String defaultPreferredName) {
		this.defaultPreferredName = defaultPreferredName;
	}
}
