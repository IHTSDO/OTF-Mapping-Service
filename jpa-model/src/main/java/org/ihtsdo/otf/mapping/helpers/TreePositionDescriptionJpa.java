package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree position with a description.
 */
public class TreePositionDescriptionJpa implements TreePositionDescription {

  private String name;

  private List<TreePositionReferencedConcept> referencedConcepts;

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void setName(String name) {
    this.name = name;

  }

  @Override
  public List<TreePositionReferencedConcept> getReferencedConcepts() {
    return this.referencedConcepts;
  }

  @Override
  public void setReferencedConcepts(
    List<TreePositionReferencedConcept> referencedConcepts) {
    this.referencedConcepts = referencedConcepts;
  }

  @Override
  public void addReferencedConcept(
    TreePositionReferencedConcept referencedConcept) {
    if (this.referencedConcepts == null)
      this.referencedConcepts = new ArrayList<>();
    this.referencedConcepts.add(referencedConcept);

  }

  @Override
  public void removeReferencedConcept(
    TreePositionReferencedConcept referencedConcept) {
    if (this.referencedConcepts == null)
      this.referencedConcepts = new ArrayList<>();
    this.referencedConcepts.remove(referencedConcept);

  }

}
