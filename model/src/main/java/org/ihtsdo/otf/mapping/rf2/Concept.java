package org.ihtsdo.otf.mapping.rf2;

import java.util.Set;

/**
 * Generically represents a concept in a terminology.
 */
public interface Concept extends Component {

  /**
   * Returns the definition status id.
   * 
   * @return definitionStatusId the definition status id
   */
  public Long getDefinitionStatusId();

  /**
   * Sets the definition status id.
   * 
   * @param definitionStatusId the definition status id
   */
  public void setDefinitionStatusId(Long definitionStatusId);

  /**
   * Returns the descriptions.
   * 
   * @return the descriptions
   */
  public Set<Description> getDescriptions();

  /**
   * Sets the descriptions.
   * 
   * @param descriptions the descriptions
   */
  public void setDescriptions(Set<Description> descriptions);

  /**
   * Adds the description.
   * 
   * @param description the description
   */
  public void addDescription(Description description);

  /**
   * Removes the description.
   * 
   * @param description the description
   */
  public void removeDescription(Description description);

  /**
   * Returns the relationships.
   * 
   * @return the relationships
   */
  public Set<Relationship> getRelationships();

  /**
   * Adds the relationship.
   * 
   * @param relationship the relationship
   */
  public void addRelationship(Relationship relationship);

  /**
   * Removes the relationship.
   * 
   * @param relationship the relationship
   */
  public void removeRelationship(Relationship relationship);

  /**
   * Sets the relationships.
   * 
   * @param relationships the relationships
   */
  public void setRelationships(Set<Relationship> relationships);

  /**
   * Returns the inverse relationships.
   * 
   * @return the inverse relationships
   */
  public Set<Relationship> getInverseRelationships();

  /**
   * Adds the inverse relationship.
   * 
   * @param relationship the relationship
   */
  public void addInverseRelationship(Relationship relationship);

  /**
   * Removes the inverse relationship.
   * 
   * @param relationship the relationship
   */
  public void removeInverseRelationship(Relationship relationship);

  /**
   * Sets the inverse relationships.
   * 
   * @param inverseRelationships the inverse relationships
   */
  public void setInverseRelationships(Set<Relationship> inverseRelationships);

  /**
   * Gets the default preferred name.
   * 
   * @return the default preferred name
   */
  public String getDefaultPreferredName();

  /**
   * Sets the default preferred name.
   * 
   * @param defaultPreferredName the new default preferred name
   */
  public void setDefaultPreferredName(String defaultPreferredName);

  /**
   * Returns the set of SimpleRefSetMembers
   * 
   * @return the set of SimpleRefSetMembers
   */
  public Set<SimpleRefSetMember> getSimpleRefSetMembers();

  /**
   * Sets the set of SimpleRefSetMembers
   * 
   * @param simpleRefSetMembers the set of SimpleRefSetMembers
   */
  public void setSimpleRefSetMembers(Set<SimpleRefSetMember> simpleRefSetMembers);

  /**
   * Adds a SimpleRefSetMember to the set of SimpleRefSetMembers
   * 
   * @param simpleRefSetMember the SimpleRefSetMembers to be added
   */
  public void addSimpleRefSetMember(SimpleRefSetMember simpleRefSetMember);

  /**
   * Removes a SimpleRefSetMember from the set of SimpleRefSetMembers
   * 
   * @param simpleRefSetMember the SimpleRefSetMember to be removed
   */
  public void removeSimpleRefSetMember(SimpleRefSetMember simpleRefSetMember);

  /**
   * Returns the set of SimpleMapRefSetMembers
   * 
   * @return the set of SimpleMapRefSetMembers
   */
  public Set<SimpleMapRefSetMember> getSimpleMapRefSetMembers();

  /**
   * Sets the set of SimpleMapRefSetMembers
   * 
   * @param simpleMapRefSetMembers the set of SimpleMapRefSetMembers
   */
  public void setSimpleMapRefSetMembers(
    Set<SimpleMapRefSetMember> simpleMapRefSetMembers);

  /**
   * Adds a SimpleMapRefSetMember to the set of SimpleMapRefSetMembers
   * 
   * @param simpleMapRefSetMember the SimpleMapRefSetMembers to be added
   */
  public void addSimpleMapRefSetMember(
    SimpleMapRefSetMember simpleMapRefSetMember);

  /**
   * Removes a SimpleMapRefSetMember from the set of SimpleMapRefSetMembers
   * 
   * @param simpleMapRefSetMember the SimpleMapRefSetMember to be removed
   */
  public void removeSimpleMapRefSetMember(
    SimpleMapRefSetMember simpleMapRefSetMember);

  /**
   * Returns the set of ComplexMapRefSetMembers
   * 
   * @return the set of ComplexMapRefSetMembers
   */
  public Set<ComplexMapRefSetMember> getComplexMapRefSetMembers();

  /**
   * Sets the set of ComplexMapRefSetMembers
   * 
   * @param complexMapRefSetMembers the set of ComplexMapRefSetMembers
   */
  public void setComplexMapRefSetMembers(
    Set<ComplexMapRefSetMember> complexMapRefSetMembers);

  /**
   * Adds a ComplexMapRefSetMember to the set of ComplexMapRefSetMembers
   * 
   * @param complexMapRefSetMember the ComplexMapRefSetMembers to be added
   */
  public void addComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember);

  /**
   * Removes a ComplexMapRefSetMember from the set of ComplexMapRefSetMembers
   * 
   * @param complexMapRefSetMember the ComplexMapRefSetMember to be removed
   */
  public void removeComplexMapRefSetMember(
    ComplexMapRefSetMember complexMapRefSetMember);

  /**
   * Returns the set of AttributeValueRefSetMembers
   * 
   * @return the set of AttributeValueRefSetMembers
   */
  public Set<AttributeValueRefSetMember> getAttributeValueRefSetMembers();

  /**
   * Sets the set of AttributeValueRefSetMembers
   * 
   * @param attributeValueRefSetMembers the set of AttributeValueRefSetMembers
   */
  public void setAttributeValueRefSetMembers(
    Set<AttributeValueRefSetMember> attributeValueRefSetMembers);

  /**
   * Adds a AttributeValueRefSetMember to the set of AttributeValueRefSetMembers
   * 
   * @param attributeValueRefSetMember the AttributeValueRefSetMembers to be
   *          added
   */
  public void addAttributeValueRefSetMember(
    AttributeValueRefSetMember attributeValueRefSetMember);

  /**
   * Removes a AttributeValueRefSetMember from the set of
   * AttributeValueRefSetMembers
   * 
   * @param attributeValueRefSetMember the AttributeValueRefSetMember to be
   *          removed
   */
  public void removeAttributeValueRefSetMember(
    AttributeValueRefSetMember attributeValueRefSetMember);

}
