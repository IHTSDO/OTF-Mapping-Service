package org.ihtsdo.otf.mapping.rf2;

/**
 * Generically represents a reference set member with associated Concept
 */
public interface ConceptRefSetMember extends RefSetMember {

  /**
   * returns the Concept
   * @return the Concept
   */
  public Concept getConcept();

  /**
   * sets the Concept
   * @param concept the Concept
   */
  public void setConcept(Concept concept);

}
