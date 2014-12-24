package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;

/**
 * JAXB enabled implementation of {@link ConceptList}.
 */
@XmlRootElement(name = "conceptList")
public class ConceptListJpa extends AbstractResultList<Concept> implements
    ConceptList {

  /** The map concepts. */
  private List<Concept> concepts = new ArrayList<>();

  /**
   * Instantiates a new map concept list.
   */
  public ConceptListJpa() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ConceptList#addConcept(org.ihtsdo.otf.mapping
   * .rf2.Concept)
   */
  @Override
  public void addConcept(Concept Concept) {
    concepts.add(Concept);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.helpers.ConceptList#removeConcept(org.ihtsdo.otf
   * .mapping.rf2.Concept)
   */
  @Override
  public void removeConcept(Concept Concept) {
    concepts.remove(Concept);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ConceptList#setConcepts(java.util.List)
   */
  @Override
  public void setConcepts(List<Concept> Concepts) {
    this.concepts = new ArrayList<>();
    if (Concepts != null) {
      this.concepts.addAll(Concepts);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ConceptList#getConcepts()
   */
  @Override
  @XmlElement(type = ConceptJpa.class, name = "concept")
  public List<Concept> getConcepts() {
    return concepts;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getCount()
   */
  @Override
  public int getCount() {
    return concepts.size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#sortBy(java.util.Comparator)
   */
  @Override
  public void sortBy(Comparator<Concept> comparator) {
    Collections.sort(concepts, comparator);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(Concept element) {
    return concepts.contains(element);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.helpers.ResultList#getIterable()
   */
  @Override
  @XmlTransient
  public Iterable<Concept> getIterable() {
    return concepts;
  }

}
