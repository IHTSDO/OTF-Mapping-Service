package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree position with a description.
 */
public class TreePositionDescriptionJpa implements TreePositionDescription {

  /** The name. */
  private String name;

  /** The referenced concepts. */
  private List<TreePositionReferencedConcept> referencedConcepts;

  /* see superclass */
  @Override
  public String getName() {
    return this.name;
  }

  /* see superclass */
  @Override
  public void setName(String name) {
    this.name = name;

  }

  /* see superclass */
  @Override
  public List<TreePositionReferencedConcept> getReferencedConcepts() {
    return this.referencedConcepts;
  }

  /* see superclass */
  @Override
  public void setReferencedConcepts(
    List<TreePositionReferencedConcept> referencedConcepts) {
    this.referencedConcepts = referencedConcepts;
  }

  /* see superclass */
  @Override
  public void addReferencedConcept(
    TreePositionReferencedConcept referencedConcept) {
    if (this.referencedConcepts == null)
      this.referencedConcepts = new ArrayList<>();
    this.referencedConcepts.add(referencedConcept);

  }

  /* see superclass */
  @Override
  public void removeReferencedConcept(
    TreePositionReferencedConcept referencedConcept) {
    if (this.referencedConcepts == null)
      this.referencedConcepts = new ArrayList<>();
    this.referencedConcepts.remove(referencedConcept);

  }

}
