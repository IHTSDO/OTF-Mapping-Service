package org.ihtsdo.otf.mapping.rf2.jpa;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.rf2.AttributeValueRefSetMember;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.SimpleMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;

/**
 * Concrete implementation of {@link Concept} for use with JPA.
 */
@Entity
//@UniqueConstraint here is being used to create an index, not to enforce uniqueness
@Table(name = "concepts", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "terminologyVersion"
}))
@Audited
@Indexed
@XmlRootElement(name = "concept")
public class ConceptJpa extends AbstractComponent implements Concept {

  /** The definition status id. */
  @Column(nullable = false)
  private Long definitionStatusId;

  /** The descriptions. */
  @OneToMany(mappedBy = "concept", cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = DescriptionJpa.class)
  private Set<Description> descriptions = new HashSet<>();

  /** The relationships. */
  @OneToMany(mappedBy = "sourceConcept", cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = RelationshipJpa.class)
  @IndexedEmbedded(targetElement = RelationshipJpa.class)
  private Set<Relationship> relationships = new HashSet<>();

  /** The inverse relationships. */
  @OneToMany(mappedBy = "destinationConcept", orphanRemoval = true, targetEntity = RelationshipJpa.class)
  private Set<Relationship> inverseRelationships = new HashSet<>();

  /** The simple RefSet members */
  @OneToMany(mappedBy = "concept", cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = SimpleRefSetMemberJpa.class)
  private Set<SimpleRefSetMember> simpleRefSetMembers = new HashSet<>();

  /** The simpleMap RefSet members */
  @OneToMany(mappedBy = "concept", cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = SimpleMapRefSetMemberJpa.class)
  private Set<SimpleMapRefSetMember> simpleMapRefSetMembers = new HashSet<>();

  /** The complexMap RefSet members */
  @OneToMany(mappedBy = "concept", cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = ComplexMapRefSetMemberJpa.class)
  private Set<ComplexMapRefSetMember> complexMapRefSetMembers = new HashSet<>();

  /** The attributeValue RefSet members */
  @OneToMany(mappedBy = "concept", cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = AttributeValueRefSetMemberJpa.class)
  private Set<AttributeValueRefSetMember> attributeValueRefSetMembers =
      new HashSet<>();

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
  @XmlElement(type = DescriptionJpa.class, name = "description")
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
  @XmlElement(type = RelationshipJpa.class, name = "relationship")
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
  @XmlTransient
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
  @XmlTransient
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
  @XmlTransient
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
  @XmlTransient
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
   * Adds a ComplexMapRefSetMember to the set of ComplexMapRefSetMembers
   * 
   * @param complexMapRefSetMember the complexMapRefSetMembers to be added
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
  @XmlTransient
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
   * Returns the default preferred name.
   * 
   * @return the default preferred name
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
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

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    return true;
  }
}