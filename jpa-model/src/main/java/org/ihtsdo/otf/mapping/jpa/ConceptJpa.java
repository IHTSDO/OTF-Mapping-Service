package org.ihtsdo.otf.mapping.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.model.Description;
import org.ihtsdo.otf.mapping.model.Relationship;

/**
 * Concrete implementation of {@link Concept} for use with JPA.
 */
@Entity
@Table(name = "concepts")
@XmlRootElement(name="concept")
public class ConceptJpa extends AbstractComponent implements Concept {

	/** The definition status id. */
	@Column(nullable = false)
	private Long definitionStatusId;

	/** The descriptions. */
	@OneToMany(mappedBy = "concept", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, targetEntity=DescriptionJpa.class)
	private Set<Description> descriptions = new HashSet<Description>();

	/** The relationships. */
	@OneToMany(mappedBy = "sourceConcept", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, targetEntity=RelationshipJpa.class)
	private Set<Relationship> relationships = new HashSet<Relationship>();

	/** The inverse relationships. */
	@OneToMany(mappedBy = "destinationConcept", fetch = FetchType.EAGER, orphanRemoval = true, targetEntity=RelationshipJpa.class)
	private Set<Relationship> inverseRelationships = new HashSet<Relationship>();

	/** The default preferred name. */
	@Column(nullable = false, length = 256)
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
	@XmlElement(type=DescriptionJpa.class)
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
	@XmlElement(type=RelationshipJpa.class)
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
	@XmlElement(type=RelationshipJpa.class)
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
	 * Returns the default preferred name.
	 *
	 * @return the default preferred name
	 */
    @Override
    public String getDefaultPreferredName() {
    	return defaultPreferredName;
	}	

	/**
	 * Sets the default preferred name.
	 *
	 * @param defaultPreferredName the default preferred name
	 */
	@Override
	public void setDefaultPreferredName(String defaultPreferredName) {
		this.defaultPreferredName = defaultPreferredName;
	}
}