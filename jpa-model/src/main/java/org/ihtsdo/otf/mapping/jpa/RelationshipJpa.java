package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonIgnore;

import org.hibernate.search.annotations.Indexed;
import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.model.Relationship;

/**
 * Concrete implementation of {@link Relationship} for use with JPA.
 */
@Entity
@Table(name = "relationships")
@XmlRootElement(name="relationship")
public class RelationshipJpa extends AbstractComponent implements Relationship {

	/** The source concept. */
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE
	}, targetEntity=ConceptJpa.class)
	private Concept sourceConcept;

	/** The destination concept. */
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE
	}, targetEntity=ConceptJpa.class)
	private Concept destinationConcept;

	/** The type id. */
	@Column(nullable = false)
	private Long typeId;

	/** The characteristic type id. */
	@Column(nullable = false)
	private Long characteristicTypeId;

	/** The modifier id. */
	@Column(nullable = false)
	private Long modifierId;

	/** The relationship group. */
	@Column(nullable = true)
	private Integer relationshipGroup;


	/*
	 * (non-Javadoc)
	 *
	 * @see gov.nih.nlm.rqs.entity.AbstractComponent#toString()
	 */
	@Override
	public String toString() {
		return (getSourceConcept() == null ? null : getSourceConcept()
				.getId())
				+ " "
				+ getTypeId()
				+ " "
				+ (getDestinationConcept() == null ? null : getDestinationConcept()
						.getId());
	}

	/**
	 * Returns the type id.
	 *
	 * @return the type id
	 */
	@Override
    public Long getTypeId() {
		return typeId;
	}

	/**
	 * Sets the type id.
	 *
	 * @param typeId the type id
	 */
	@Override
    public void setTypeId(Long typeId) {
		this.typeId = typeId;
	}

	/**
	 * Returns the characteristic type id.
	 *
	 * @return the characteristic type id
	 */
	@Override
    public Long getCharacteristicTypeId() {
		return characteristicTypeId;
	}

	/**
	 * Sets the characteristic type id.
	 *
	 * @param characteristicTypeId the characteristic type id
	 */
	@Override
    public void setCharacteristicTypeId(Long characteristicTypeId) {
		this.characteristicTypeId = characteristicTypeId;
	}

	/**
	 * Returns the modifier id.
	 *
	 * @return the modifier id
	 */
	@Override
    public Long getModifierId() {
		return modifierId;
	}

	/**
	 * Sets the modifier id.
	 *
	 * @param modifierId the modifier id
	 */
	@Override
    public void setModifierId(Long modifierId) {
		this.modifierId = modifierId;
	}

	/**
	 * Returns the source concept.
	 *
	 * @return the source concept
	 */
	@Override
	@XmlIDREF
	@XmlAttribute
	@JsonIgnore
    public ConceptJpa getSourceConcept() {
		return (ConceptJpa) sourceConcept;
	}

	/**
	 * Sets the source concept.
	 *
	 * @param sourceConcept the source concept
	 */
	@Override
    public void setSourceConcept(Concept sourceConcept) {
		this.sourceConcept = sourceConcept;
	}

	/**
	 * Returns the destination concept.
	 *
	 * @return the destination concept
	 */
	@Override
	@XmlIDREF
	@XmlAttribute
	@JsonIgnore
    public ConceptJpa getDestinationConcept() {
		return (ConceptJpa) destinationConcept;
	}

	/**
	 * Sets the destination concept.
	 *
	 * @param destinationConcept the destination concept
	 */
	@Override
    public void setDestinationConcept(Concept destinationConcept) {
		this.destinationConcept = destinationConcept;
	}

	/**
	 * Returns the relationship group.
	 *
	 * @return the relationship group
	 */
	@Override
    public Integer getRelationshipGroup() {
		return relationshipGroup;
	}

	/**
	 * Sets the relationship group.
	 *
	 * @param relationshipGroup the relationship group
	 */
	@Override
    public void setRelationshipGroup(Integer relationshipGroup) {
		this.relationshipGroup = relationshipGroup;
	}

 
}
