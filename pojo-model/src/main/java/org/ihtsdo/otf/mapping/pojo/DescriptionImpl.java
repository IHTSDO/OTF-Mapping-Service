package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.model.Description;

/**
 * Concrete implementation of {@link Description}.
 */
public class DescriptionImpl extends AbstractComponent implements Description {

	/** The language code. */
	private String languageCode;

	/** The type. */
	private Long typeId;

	/** The term. */
	private String term;

	/** The case significance id. */
	private Long caseSignificanceId;

	/** The concept. */
	private Concept concept;

	/** The terminology. */
	private String terminology;

	/**
	 * Instantiates an empty {@link Description}.
	 */
	public DescriptionImpl() {
		// empty
	}

	/**
	 * Instantiates a {@link Description} from the specified parameters.
	 *
	 * @param type the type
	 */
	public DescriptionImpl(Long typeId) {
		this.typeId = typeId;
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
	 * @param type the type id
	 */
	@Override
    public void setTypeId(Long typeId) {
		this.typeId = typeId;
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
}
