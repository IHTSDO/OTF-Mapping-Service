package org.ihtsdo.otf.mapping.rf2.pojo;

import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;

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

  /** The language RefSet members */
  private Set<LanguageRefSetMember> languageRefSetMembers = new HashSet<>();

  /**
   * Instantiates an empty {@link Description}.
   */
  public DescriptionImpl() {
    // empty
  }

  /**
   * Instantiates a {@link Description} from the specified parameters.
   * 
   * @param typeId the typeId
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
   * @param typeId the type id
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
  public void setLanguageRefSetMembers(
    Set<LanguageRefSetMember> languageRefSetMembers) {
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
  public void removeLanguageRefSetMember(
    LanguageRefSetMember languageRefSetMember) {
    this.languageRefSetMembers.remove(languageRefSetMember);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return this.getId() + ","
        + this.getTerminology()
        + ","
        + this.getTerminologyId()
        + ","
        + this.getTerminologyVersion()
        + ","
        + this.getEffectiveTime()
        + ","
        + this.isActive()
        + ","
        + this.getModuleId()
        + ","
        + // end of basic component fields

        (this.getConcept() == null ? null : this.getConcept()
            .getTerminologyId()) + "," + this.getTypeId() + ","
        + this.getTerm() + "," + this.getCaseSignificanceId();
  }

}
