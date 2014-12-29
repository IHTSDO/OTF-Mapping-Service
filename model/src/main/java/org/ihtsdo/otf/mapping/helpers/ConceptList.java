package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.rf2.Concept;

/**
 * Represents a sortable list of {@link Concept}.
 */
public interface ConceptList extends ResultList<Concept> {

  /**
   * Adds the map concept.
   * 
   * @param Concept the map concept
   */
  public void addConcept(Concept Concept);

  /**
   * Removes the map concept.
   * 
   * @param Concept the map concept
   */
  public void removeConcept(Concept Concept);

  /**
   * Sets the map concepts.
   * 
   * @param Concepts the new map concepts
   */
  public void setConcepts(List<Concept> Concepts);

  /**
   * Gets the map concepts.
   * 
   * @return the map concepts
   */
  public List<Concept> getConcepts();

}
