package org.ihtsdo.otf.mapping.rf2.jpa;

import java.util.Date;
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

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
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
// @UniqueConstraint here is being used to create an index, not to enforce
// uniqueness
@Table(name = "concepts", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "terminologyVersion"
}))
// @Audited
@Indexed
@XmlRootElement(name = "concept")
public class ConceptJpa extends AbstractComponent implements Concept {

  /** The definition status id. */
  @Column(nullable = false)
  private Long definitionStatusId;

  /** The descriptions. */
  @OneToMany(mappedBy = "concept", cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = DescriptionJpa.class)
  @IndexedEmbedded(targetElement = DescriptionJpa.class)
  // PG
  private Set<Description> descriptions = null;

  /** The relationships. */
  @OneToMany(mappedBy = "sourceConcept", cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = RelationshipJpa.class)
  @IndexedEmbedded(targetElement = RelationshipJpa.class)
  private Set<Relationship> relationships = null;

  /** The inverse relationships. */
  @OneToMany(mappedBy = "destinationConcept", targetEntity = RelationshipJpa.class)
  private Set<Relationship> inverseRelationships = null;

  /** The simple RefSet members. */
  @OneToMany(mappedBy = "concept", targetEntity = SimpleRefSetMemberJpa.class)
  private Set<SimpleRefSetMember> simpleRefSetMembers = null;

  /** The simpleMap RefSet members. */
  @OneToMany(mappedBy = "concept", targetEntity = SimpleMapRefSetMemberJpa.class)
  private Set<SimpleMapRefSetMember> simpleMapRefSetMembers = null;

  /** The complexMap RefSet members. */
  @OneToMany(mappedBy = "concept", targetEntity = ComplexMapRefSetMemberJpa.class)
  private Set<ComplexMapRefSetMember> complexMapRefSetMembers = null;

  /** The attributeValue RefSet members. */
  @OneToMany(mappedBy = "concept", targetEntity = AttributeValueRefSetMemberJpa.class)
  private Set<AttributeValueRefSetMember> attributeValueRefSetMembers = null;

  /** The default preferred name. */
  @Column(nullable = false, length = 256)
  private String defaultPreferredName;

  /**
   * Instantiates a new concept jpa.
   */
  public ConceptJpa() {
    // do nothing
  }

  /**
   * Instantiates a new concept jpa.
   *
   * @param concept the concept
   * @param deepCopy the deep copy flag
   */
  public ConceptJpa(Concept concept, boolean deepCopy) {
    setId(concept.getId());
    setActive(concept.isActive());
    setEffectiveTime(concept.getEffectiveTime());
    setLabel(concept.getLabel());
    setModuleId(concept.getModuleId());
    setTerminology(concept.getTerminology());
    setTerminologyId(concept.getTerminologyId());
    setTerminologyVersion(concept.getTerminologyVersion());
    defaultPreferredName = concept.getDefaultPreferredName();
    definitionStatusId = concept.getDefinitionStatusId();
    if (deepCopy) {
      descriptions = new HashSet<>();
      for (Description description : concept.getDescriptions()) {
        Description d = new DescriptionJpa(description, deepCopy);
        d.setConcept(this);
        descriptions.add(d);
      }
      relationships = new HashSet<>();
      for (Relationship relationship : concept.getRelationships()) {
        Relationship rel = new RelationshipJpa(relationship, deepCopy);
        rel.setSourceConcept(this);
        relationships.add(rel);
      }
      // Ignore these for now
      // inverseRelationships
      // simpleRefSetMembers = concept.getSimpleRefSetMembers();
      // simpleMapRefSetMembers = concept.getSimpleMapRefSetMembers();
      // complexMapRefSetMembers = concept.getComplexMapRefSetMembers();
      // attributeValueRefSetMembers = concept.getAttributeValueRefSetMembers();
    }
  }
  

  @Fields({
      @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO),
      @Field(name = "terminologyIdAnalyzed", index = Index.YES, analyze = Analyze.YES, store = Store.NO, analyzer = @Analyzer(definition = "noStopWord"))
  })
  @Override
  public String getTerminologyId() {
	  return super.getTerminologyId();
  }

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
    if (descriptions == null) {
      descriptions = new HashSet<>();
    }
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
    if (descriptions == null) {
      descriptions = new HashSet<>();
    }
    description.setConcept(this);
    descriptions.add(description);
  }

  /**
   * Removes the description.
   * 
   * @param description the description
   */
  @Override
  public void removeDescription(Description description) {
    if (descriptions == null) {
      return;
    }
    descriptions.remove(description);
  }

  /**
   * Returns the relationships.
   * 
   * @return the relationships
   */
  @Override
  @XmlElement(type = RelationshipJpa.class, name = "relationship")
  public Set<Relationship> getRelationships() {
    if (relationships == null) {
      relationships = new HashSet<>();
    }
    return relationships;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.Concept#addRelationship(org.ihtsdo.otf.mapping
   * .rf2.Relationship)
   */
  @Override
  public void addRelationship(Relationship relationship) {
    if (relationships == null) {
      relationships = new HashSet<>();
    }
    this.relationships.add(relationship);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.Concept#removeRelationship(org.ihtsdo.otf.
   * mapping .rf2.Relationship)
   */
  @Override
  public void removeRelationship(Relationship relationship) {
    if (relationships == null) {
      return;
    }
    this.relationships.remove(relationship);
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
    if (inverseRelationships == null) {
      inverseRelationships = new HashSet<>();
    }
    return inverseRelationships;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.Concept#addInverseRelationship(org.ihtsdo.otf
   * .mapping.rf2.Relationship)
   */
  @Override
  public void addInverseRelationship(Relationship relationship) {
    if (inverseRelationships == null) {
      inverseRelationships = new HashSet<>();
    }
    inverseRelationships.add(relationship);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.Concept#removeInverseRelationship(org.ihtsdo
   * .otf.mapping.rf2.Relationship)
   */
  @Override
  public void removeInverseRelationship(Relationship relationship) {
    if (inverseRelationships == null) {
      return;
    }
    inverseRelationships.remove(relationship);
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
   * Returns the set of SimpleRefSetMembers.
   *
   * @return the set of SimpleRefSetMembers
   */
  @XmlTransient
  @Override
  public Set<SimpleRefSetMember> getSimpleRefSetMembers() {
    if (simpleRefSetMembers == null) {
      simpleRefSetMembers = new HashSet<>();
    }
    return simpleRefSetMembers;
  }

  /**
   * Sets the set of SimpleRefSetMembers.
   *
   * @param simpleRefSetMembers the set of SimpleRefSetMembers
   */
  @Override
  public void setSimpleRefSetMembers(
    Set<SimpleRefSetMember> simpleRefSetMembers) {
    this.simpleRefSetMembers = simpleRefSetMembers;
  }

  /**
   * Adds a SimpleRefSetMember to the set of SimpleRefSetMembers.
   *
   * @param simpleRefSetMember the SimpleRefSetMembers to be added
   */
  @Override
  public void addSimpleRefSetMember(SimpleRefSetMember simpleRefSetMember) {
    if (simpleRefSetMembers == null) {
      simpleRefSetMembers = new HashSet<>();
    }
    simpleRefSetMember.setConcept(this);
    simpleRefSetMembers.add(simpleRefSetMember);
  }

  /**
   * Removes a SimpleRefSetMember from the set of SimpleRefSetMembers.
   *
   * @param simpleRefSetMember the SimpleRefSetMember to be removed
   */
  @Override
  public void removeSimpleRefSetMember(SimpleRefSetMember simpleRefSetMember) {
    if (simpleRefSetMembers == null) {
      return;
    }
    simpleRefSetMembers.remove(simpleRefSetMember);
  }

  /**
   * Returns the set of SimpleMapRefSetMembers.
   *
   * @return the set of SimpleMapRefSetMembers
   */
  @XmlTransient
  @Override
  public Set<SimpleMapRefSetMember> getSimpleMapRefSetMembers() {
    if (simpleMapRefSetMembers == null) {
      simpleMapRefSetMembers = new HashSet<>();
    }
    return simpleMapRefSetMembers;
  }

  /**
   * Sets the set of SimpleMapRefSetMembers.
   *
   * @param simpleMapRefSetMembers the set of SimpleMapRefSetMembers
   */
  @Override
  public void setSimpleMapRefSetMembers(
    Set<SimpleMapRefSetMember> simpleMapRefSetMembers) {
    this.simpleMapRefSetMembers = simpleMapRefSetMembers;
  }

  /**
   * Adds a SimpleMapRefSetMember to the set of SimpleMapRefSetMembers.
   *
   * @param simpleMapRefSetMember the SimpleMapRefSetMembers to be added
   */
  @Override
  public void addSimpleMapRefSetMember(
    SimpleMapRefSetMember simpleMapRefSetMember) {
    if (simpleMapRefSetMembers == null) {
      simpleMapRefSetMembers = new HashSet<>();
    }
    simpleMapRefSetMember.setConcept(this);
    simpleMapRefSetMembers.add(simpleMapRefSetMember);
  }

  /**
   * Removes a SimpleMapRefSetMember from the set of SimpleMapRefSetMembers.
   *
   * @param simpleMapRefSetMember the SimpleMapRefSetMember to be removed
   */
  @Override
  public void removeSimpleMapRefSetMember(
    SimpleMapRefSetMember simpleMapRefSetMember) {
    if (simpleMapRefSetMembers == null) {
      return;
    }
    simpleMapRefSetMembers.remove(simpleMapRefSetMember);
  }

  /**
   * Returns the set of ComplexMapRefSetMembers.
   *
   * @return the set of ComplexMapRefSetMembers
   */
  @XmlTransient
  @Override
  public Set<ComplexMapRefSetMember> getComplexMapRefSetMembers() {
    if (complexMapRefSetMembers == null) {
      complexMapRefSetMembers = new HashSet<>();
    }
    return complexMapRefSetMembers;
  }

  /**
   * Sets the set of ComplexMapRefSetMembers.
   *
   * @param complexMapRefSetMembers the set of ComplexMapRefSetMembers
   */
  @Override
  public void setComplexMapRefSetMembers(
    Set<ComplexMapRefSetMember> complexMapRefSetMembers) {
    this.complexMapRefSetMembers = complexMapRefSetMembers;
  }

  /**
   * Adds a ComplexMapRefSetMember to the set of ComplexMapRefSetMembers.
   *
   * @param complexMapRefSetMember the complexMapRefSetMembers to be added
   */
  @Override
  public void addComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember) {
    if (complexMapRefSetMembers == null) {
      complexMapRefSetMembers = new HashSet<>();
    }
    complexMapRefSetMember.setConcept(this);
    complexMapRefSetMembers.add(complexMapRefSetMember);
  }

  /**
   * Removes a ComplexMapRefSetMember from the set of ComplexMapRefSetMembers.
   *
   * @param complexMapRefSetMember the ComplexMapRefSetMember to be removed
   */
  @Override
  public void removeComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember) {
    if (complexMapRefSetMembers == null) {
      return;
    }
    complexMapRefSetMembers.remove(complexMapRefSetMember);
  }

  /**
   * Returns the set of AttributeValueRefSetMembers.
   *
   * @return the set of AttributeValueRefSetMembers
   */
  @XmlTransient
  @Override
  public Set<AttributeValueRefSetMember> getAttributeValueRefSetMembers() {
    if (attributeValueRefSetMembers == null) {
      attributeValueRefSetMembers = new HashSet<>();
    }
    return attributeValueRefSetMembers;
  }

  /**
   * Sets the set of AttributeValueRefSetMembers.
   *
   * @param attributeValueRefSetMembers the set of AttributeValueRefSetMembers
   */
  @Override
  public void setAttributeValueRefSetMembers(
    Set<AttributeValueRefSetMember> attributeValueRefSetMembers) {
    this.attributeValueRefSetMembers = attributeValueRefSetMembers;
  }

  /**
   * Adds a AttributeValueRefSetMember to the set of
   * AttributeValueRefSetMembers.
   *
   * @param attributeValueRefSetMember the AttributeValueRefSetMembers to be
   *          added
   */
  @Override
  public void addAttributeValueRefSetMember(
    AttributeValueRefSetMember attributeValueRefSetMember) {
    if (attributeValueRefSetMembers == null) {
      attributeValueRefSetMembers = new HashSet<>();
    }
    attributeValueRefSetMember.setConcept(this);
    attributeValueRefSetMembers.add(attributeValueRefSetMember);
  }

  /**
   * Removes a AttributeValueRefSetMember from the set of
   * AttributeValueRefSetMembers.
   *
   * @param attributeValueRefSetMember the AttributeValueRefSetMember to be
   *          removed
   */
  @Override
  public void removeAttributeValueRefSetMember(
    AttributeValueRefSetMember attributeValueRefSetMember) {
    if (attributeValueRefSetMembers == null) {
      return;
    }
    attributeValueRefSetMembers.remove(attributeValueRefSetMember);
  }

  /**
   * Override get effective time to allow indexing.
   *
   * @return the effective time
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  @Override
  public Date getEffectiveTime() {
    return super.getEffectiveTime();
  }

  /**
   * Override get effective time to allow indexing.
   *
   * @return the terminology
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.YES)
  @Override
  public String getTerminology() {
    return super.getTerminology();
  }

  /**
   * Override get effective time to allow indexing.
   *
   * @return the terminology version
   */
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.YES)
  @Override
  public String getTerminologyVersion() {
    return super.getTerminologyVersion();
  }

  /**
   * Returns the default preferred name.
   * 
   * @return the default preferred name
   */
  @Override
  @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  @Analyzer(definition = "noStopWord")
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
    return getId() + "," + getTerminology() + "," + getTerminologyId() + ","
        + getTerminologyVersion() + "," + getEffectiveTime() + "," + isActive()
        + "," + getModuleId() + "," + getDefinitionStatusId() + ","
        + getDefaultPreferredName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.rf2.jpa.AbstractComponent#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((definitionStatusId == null) ? 0 : definitionStatusId.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.rf2.jpa.AbstractComponent#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    ConceptJpa other = (ConceptJpa) obj;
    if (definitionStatusId == null) {
      if (other.definitionStatusId != null)
        return false;
    } else if (!definitionStatusId.equals(other.definitionStatusId))
      return false;
    return true;
  }

}