package org.ihtsdo.otf.mapping.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.model.Description;
import org.ihtsdo.otf.mapping.model.LanguageRefSetMember;

/**
 * Concrete implementation of {@link Description} for use with JPA.
 */
@Entity
@Table(name = "descriptions", uniqueConstraints=@UniqueConstraint(columnNames={"terminologyId", "terminology", "terminologyVersion"}))
@Audited
public class DescriptionJpa extends AbstractComponent implements Description {

	/** The language code. */
	@Column(nullable = false, length = 10)
	private String languageCode;

	/** The typeId. */
	@Column(nullable = false)
	private Long typeId;

	/** The term. */
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
	
	/** The language RefSet members */
	@OneToMany(mappedBy = "description", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, targetEntity=LanguageRefSetMemberJpa.class)
	private Set<LanguageRefSetMember> languageRefSetMembers = new HashSet<LanguageRefSetMember>();

    
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
	 * Returns the set of SimpleRefSetMembers
	 *
	 * @return the set of SimpleRefSetMembers
	 */
	@Override
	public Set<LanguageRefSetMember> getLanguageRefSetMembers() {
		return this.languageRefSetMembers;
	}

	/**
	 * Sets the set of LanguageRefSetMembers
	 *
	 * @param languageRefSetMembers the set of LanguageRefSetMembers
	 */
	@Override
	public void setLanguageRefSetMembers(Set<LanguageRefSetMember> languageRefSetMembers) {
		this.languageRefSetMembers = languageRefSetMembers;
	}
	
	/**
	 * Adds a LanguageRefSetMember to the set of LanguageRefSetMembers
	 *
	 * @param languageRefSetMember the LanguageRefSetMembers to be added
	 */
	@Override
	public void addLanguageRefSetMember(LanguageRefSetMember languageRefSetMember) {
		languageRefSetMember.setDescription(this);
		this.languageRefSetMembers.add(languageRefSetMember);
	}
	
	/**
	 * Removes a LanguageRefSetMember from the set of LanguageRefSetMembers
	 *
	 * @param languageRefSetMember the LanguageRefSetMember to be removed
	*/
	@Override
	public void removeLanguageRefSetMember(LanguageRefSetMember languageRefSetMember) {
		this.languageRefSetMembers.remove(languageRefSetMember);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		 return this.getId() + "," +
				 this.getTerminology() + "," +
				 this.getTerminologyId() + "," +
				 this.getTerminologyVersion() + "," +
				 this.getEffectiveTime() + "," +
				 this.isActive() + "," +
				 this.getModuleId() + "," + // end of basic component fields
				 
				 (this.getConcept() == null ? null : getConcept().getTerminologyId()) + "," +
				 this.getLanguageCode() + "," +
				 this.getTypeId() + "," +
				 this.getTerm() + "," +
				 this.getCaseSignificanceId(); // end of basic description fields
	 }


}
