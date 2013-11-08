package org.ihtsdo.otf.mapping.jpa;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.model.Description;

/**
 * Concrete implementation of {@link Description} for use with JPA.
 */
@Entity
@Table(name = "descriptions")
public class DescriptionJpa extends AbstractComponent implements Description {

	/** The language code. */
	@Column(nullable = false, length = 10)
	private String languageCode;

	/** The typeId. */
	@Column(nullable = false)
	private Long typeId;

	/** The term. */
	// TODO: @Field(index = Index.TOKENIZED, store = Store.NO)
	@Column(nullable = false, length = 256)
	private String term;

	/** The case significance id. */
	@Column(nullable = false)
	private Long caseSignificanceId;

	/** The concept. */
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE
	}, targetEntity=ConceptJpa.class)
	private Concept concept;

    
	/**
	 * Instantiates an empty {@link Description}.
	 */
	public DescriptionJpa() {
		// empty
	}

	/**
	 * Instantiates a {@link Description} from the specified parameters.
	 *
	 * @param type the type
	 */
	public DescriptionJpa(Long type) {
		this.typeId = type;
	}

	/**
	 * Returns the language code.
	 *
	 * @return the language code
	 */
	@Override
    public String getLanguageCode() {
		return languageCode;
	}

	/**
	 * Sets the language code.
	 *
	 * @param languageCode the language code
	 */
	@Override
    public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}

	/**
	 * Returns the type.
	 *
	 * @return the type
	 */
	@Override
    public Long getTypeId() {
		return typeId;
	}

	/**
	 * Sets the type.
	 *
	 * @param type the type
	 */
	@Override
    public void setTypeId(Long type) {
		this.typeId = type;
	}

	/**
	 * Returns the term.
	 *
	 * @return the term
	 */
	@Override
    public String getTerm() {
		return term;
	}

	/**
	 * Sets the term.
	 *
	 * @param term the term
	 */
	@Override
    public void setTerm(String term) {
		this.term = term;
	}

	/**
	 * Returns the case significance id.
	 *
	 * @return the case significance id
	 */
	@Override
    public Long getCaseSignificanceId() {
		return caseSignificanceId;
	}

	/**
	 * Sets the case significance id.
	 *
	 * @param caseSignificanceId the case significance id
	 */
	@Override
    public void setCaseSignificanceId(Long caseSignificanceId) {
		this.caseSignificanceId = caseSignificanceId;
	}

	/**
	 * Returns the concept.
	 *
	 * @return the concept
	 */
	@Override
    public Concept getConcept() {
		return concept;
	}

	/**
	 * Sets the concept.
	 *
	 * @param concept the concept
	 */
	@Override
    public void setConcept(Concept concept) {
		this.concept = concept;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}


}
