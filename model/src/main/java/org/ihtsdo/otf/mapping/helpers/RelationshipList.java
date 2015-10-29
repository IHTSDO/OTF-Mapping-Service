package org.ihtsdo.otf.mapping.helpers;

import java.util.List;
import java.util.Set;

import org.ihtsdo.otf.mapping.rf2.Relationship;

/**
 * Represents a sortable list of {@link Relationship}.
 */
public interface RelationshipList extends ResultList<Relationship> {

  /**
   * Adds the map relationship.
   * @param relationship
   */
  public void addRelationship(Relationship relationship);

  /**
   * Removes the map relationship.
   * @param relationship
   */
  public void removeRelationship(Relationship relationship);

  /**
   * Sets the map Relationships given a List of relationships
   * @param relationships
   */
  public void setRelationships(List<Relationship> relationships);

  /**
   * Sets the map Relationships given a Set of relationships
   * @param relationships the set of relationships
   */
  public void setRelationships(Set<Relationship> relationships);

  /**
   * Gets the map Relationships.
   * 
   * @return the map Relationships
   */
  public List<Relationship> getRelationships();

}
