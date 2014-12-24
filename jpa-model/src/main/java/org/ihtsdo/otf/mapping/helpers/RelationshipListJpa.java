package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;

/**
 * JAXB enabled implementation of {@link RelationshipList}.
 */
@XmlRootElement(name = "relationshipList")
public class RelationshipListJpa extends AbstractResultList<Relationship>
    implements RelationshipList {

  /** The map relationships. */
  private List<Relationship> relationships = new ArrayList<>();

  /**
   * Instantiates a new map relationship list.
   */
  public RelationshipListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.RelationshipList#addRelationship(org.ihtsdo
   * .otf.mapping.rf2.Relationship)
   */
  @Override
  public void addRelationship(Relationship relationship) {
    relationships.add(relationship);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.RelationshipList#removeRelationship(org.
   * ihtsdo.otf.mapping.rf2.Relationship)
   */
  @Override
  public void removeRelationship(Relationship relationship) {
    relationships.remove(relationship);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.RelationshipList#setRelationships(java.util
   * .List)
   */
  @Override
  public void setRelationships(List<Relationship> relationships) {
    this.relationships = new ArrayList<>();
    if (relationships != null) {
      this.relationships.addAll(relationships);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.RelationshipList#setRelationships(java.util
   * .Set)
   */
  @Override
  public void setRelationships(Set<Relationship> relationships) {
    Iterator<Relationship> iter = relationships.iterator();
    while (iter.hasNext()) {
      this.relationships.add(iter.next());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.RelationshipList#getRelationships()
   */
  @Override
  @XmlElement(type = RelationshipJpa.class, name = "relationship")
  public List<Relationship> getRelationships() {
    return relationships;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  @XmlElement(name = "count")
  public int getCount() {
    return relationships.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<Relationship> comparator) {
    Collections.sort(relationships, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(Relationship element) {
    return relationships.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<Relationship> getIterable() {
    return relationships;
  }

}
