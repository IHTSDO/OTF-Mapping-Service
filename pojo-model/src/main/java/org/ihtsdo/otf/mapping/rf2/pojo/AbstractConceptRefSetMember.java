package org.ihtsdo.otf.mapping.rf2.pojo;

import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.ConceptRefSetMember;

/**
 * Abstract implementation of {@link ConceptRefSetMember}.
 */
public abstract class AbstractConceptRefSetMember extends AbstractRefSetMember
    implements ConceptRefSetMember {

  /** The Concept associated with this element */
  private Concept concept;

  /**
   * Returns the Concept
   * @return the Concept
   */
  @Override
  public Concept getConcept() {
    return this.concept;
  }

  /**
   * Sets the Concept
   * @param concept the Concept
   */
  @Override
  public void setConcept(Concept concept) {
    this.concept = concept;

  }
}
