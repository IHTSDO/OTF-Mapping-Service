package org.ihtsdo.otf.mapping.rf2.pojo;

import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.otf.mapping.rf2.AttributeValueRefSetMember;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.SimpleMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;

/**
 * Concrete implementation of {@link Concept}.
 */
public class ConceptImpl extends AbstractComponent implements Concept {

  /** The definition status id. */
  private Long definitionStatusId;

  /** The descriptions. */
  private Set<Description> descriptions = new HashSet<>();

  /** The relationships. */
  private Set<Relationship> relationships = new HashSet<>();

  /** The inverse relationships. */
  private Set<Relationship> inverseRelationships = new HashSet<>();

  /** The simple RefSet members */
  private Set<SimpleRefSetMember> simpleRefSetMembers = new HashSet<>();

  /** The simpleMap RefSet members */
  private Set<SimpleMapRefSetMember> simpleMapRefSetMembers = new HashSet<>();

  /** The complexMap RefSet members */
  private Set<ComplexMapRefSetMember> complexMapRefSetMembers = new HashSet<>();

  /** the attributeValue RefSet members */
  private Set<AttributeValueRefSetMember> attributeValueRefSetMembers =
      new HashSet<>();

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
   * Returns the set of SimpleRefSetMembers
   * 
   * @return the set of SimpleRefSetMembers
   */
  @Override
  public Set<SimpleRefSetMember> getSimpleRefSetMembers() {
    return this.simpleRefSetMembers;
  }

  /**
   * Sets the set of SimpleRefSetMembers
   * 
   * @param simpleRefSetMembers the set of SimpleRefSetMembers
   */
  @Override
  public void setSimpleRefSetMembers(Set<SimpleRefSetMember> simpleRefSetMembers) {
    this.simpleRefSetMembers = simpleRefSetMembers;
  }

  /**
   * Adds a SimpleRefSetMember to the set of SimpleRefSetMembers
   * 
   * @param simpleRefSetMember the SimpleRefSetMembers to be added
   */
  @Override
  public void addSimpleRefSetMember(SimpleRefSetMember simpleRefSetMember) {
    simpleRefSetMember.setConcept(this);
    this.simpleRefSetMembers.add(simpleRefSetMember);
  }

  /**
   * Removes a SimpleRefSetMember from the set of SimpleRefSetMembers
   * 
   * @param simpleRefSetMember the SimpleRefSetMember to be removed
   */
  @Override
  public void removeSimpleRefSetMember(SimpleRefSetMember simpleRefSetMember) {
    this.simpleRefSetMembers.remove(simpleRefSetMember);
  }

  /**
   * Returns the set of SimpleMapRefSetMembers
   * 
   * @return the set of SimpleMapRefSetMembers
   */
  @Override
  public Set<SimpleMapRefSetMember> getSimpleMapRefSetMembers() {
    return this.simpleMapRefSetMembers;
  }

  /**
   * Sets the set of SimpleMapRefSetMembers
   * 
   * @param simpleMapRefSetMembers the set of SimpleMapRefSetMembers
   */
  @Override
  public void setSimpleMapRefSetMembers(
    Set<SimpleMapRefSetMember> simpleMapRefSetMembers) {
    this.simpleMapRefSetMembers = simpleMapRefSetMembers;
  }

  /**
   * Adds a SimpleMapRefSetMember to the set of SimpleMapRefSetMembers
   * 
   * @param simpleMapRefSetMember the SimpleMapRefSetMembers to be added
   */
  @Override
  public void addSimpleMapRefSetMember(
    SimpleMapRefSetMember simpleMapRefSetMember) {
    simpleMapRefSetMember.setConcept(this);
    this.simpleMapRefSetMembers.add(simpleMapRefSetMember);
  }

  /**
   * Removes a SimpleMapRefSetMember from the set of SimpleMapRefSetMembers
   * 
   * @param simpleMapRefSetMember the SimpleMapRefSetMember to be removed
   */
  @Override
  public void removeSimpleMapRefSetMember(
    SimpleMapRefSetMember simpleMapRefSetMember) {
    this.simpleMapRefSetMembers.remove(simpleMapRefSetMember);
  }

  /**
   * Returns the set of ComplexMapRefSetMembers
   * 
   * @return the set of ComplexMapRefSetMembers
   */
  @Override
  public Set<ComplexMapRefSetMember> getComplexMapRefSetMembers() {
    return this.complexMapRefSetMembers;
  }

  /**
   * Sets the set of ComplexMapRefSetMembers
   * 
   * @param complexMapRefSetMembers the set of ComplexMapRefSetMembers
   */
  @Override
  public void setComplexMapRefSetMembers(
    Set<ComplexMapRefSetMember> complexMapRefSetMembers) {
    this.complexMapRefSetMembers = complexMapRefSetMembers;
  }

  /**
   * Adds a ComplexMapSetMember to the set of SimpleRefSetMembers
   * 
   * @param complexMapRefSetMember the ComplexMapRefSetMember to be added
   */
  @Override
  public void addComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember) {
    complexMapRefSetMember.setConcept(this);
    this.complexMapRefSetMembers.add(complexMapRefSetMember);
  }

  /**
   * Removes a ComplexMapRefSetMember from the set of ComplexMapRefSetMembers
   * 
   * @param complexMapRefSetMember the ComplexMapRefSetMember to be removed
   */
  @Override
  public void removeComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember) {
    this.complexMapRefSetMembers.remove(complexMapRefSetMember);
  }

  /**
   * Returns the set of AttributeValueRefSetMembers
   * 
   * @return the set of AttributeValueRefSetMembers
   */
  @Override
  public Set<AttributeValueRefSetMember> getAttributeValueRefSetMembers() {
    return this.attributeValueRefSetMembers;
  }

  /**
   * Sets the set of AttributeValueRefSetMembers
   * 
   * @param attributeValueRefSetMembers the set of AttributeValueRefSetMembers
   */
  @Override
  public void setAttributeValueRefSetMembers(
    Set<AttributeValueRefSetMember> attributeValueRefSetMembers) {
    this.attributeValueRefSetMembers = attributeValueRefSetMembers;
  }

  /**
   * Adds a AttributeValueRefSetMember to the set of AttributeValueRefSetMembers
   * 
   * @param attributeValueRefSetMember the AttributeValueRefSetMembers to be
   *          added
   */
  @Override
  public void addAttributeValueRefSetMember(
    AttributeValueRefSetMember attributeValueRefSetMember) {
    attributeValueRefSetMember.setConcept(this);
    this.attributeValueRefSetMembers.add(attributeValueRefSetMember);
  }

  /**
   * Removes a AttributeValueRefSetMember from the set of
   * AttributeValueRefSetMembers
   * 
   * @param attributeValueRefSetMember the AttributeValueRefSetMember to be
   *          removed
   */
  @Override
  public void removeAttributeValueRefSetMember(
    AttributeValueRefSetMember attributeValueRefSetMember) {
    this.attributeValueRefSetMembers.remove(attributeValueRefSetMember);
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

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.rf2.data.model.Concept#getDefaultPreferredName()
   */
  @Override
  public String getDefaultPreferredName() {
    return defaultPreferredName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.rf2.data.model.Concept#setDefaultPreferredName(java.lang
   * .String)
   */
  @Override
  public void setDefaultPreferredName(String defaultPreferredName) {
    this.defaultPreferredName = defaultPreferredName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {

    return this.getId() + "," + this.getTerminology() + ","
        + this.getTerminologyId() + "," + this.getTerminologyVersion() + ","
        + this.getEffectiveTime() + "," + this.isActive() + ","
        + this.getModuleId() + "," + // end of basic component fields

        this.getDefinitionStatusId() + "," + this.getDefaultPreferredName(); // end
                                                                             // of
                                                                             // basic
                                                                             // concept
                                                                             // fields
  }

}
