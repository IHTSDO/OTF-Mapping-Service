package org.ihtsdo.otf.mapping.helpers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Relationship;

/**
 * Helper class for walking graphs of objects.
 */
public class GraphHelper {

  /**
   * Returns the descendant concepts.
   * 
   * @param rootConcept the root concept
   * @param typeId the type id
   * @return the descendant concepts
   * @throws Exception the exception
   */
  public static Set<Concept> getDescendantConcepts(Concept rootConcept,
    String typeId) throws Exception {

    Queue<Concept> conceptQueue = new LinkedList<>();
    Set<Concept> conceptSet = new HashSet<>();

    // if non-null result, seed the queue with this concept
    if (rootConcept != null) {
      conceptQueue.add(rootConcept);
    }

    // while concepts remain to be checked, continue
    while (!conceptQueue.isEmpty()) {

      // retrieve this concept
      Concept c = conceptQueue.poll();

      // if concept is active
      if (c.isActive()) {

        // relationship set and iterator
        Set<Relationship> inv_relationships = c.getInverseRelationships();
        Iterator<Relationship> it_inv_rel = inv_relationships.iterator();

        // iterate over inverse relationships
        while (it_inv_rel.hasNext()) {

          // get relationship
          Relationship rel = it_inv_rel.next();

          // if relationship is active, typeId equals the provided typeId, and
          // the source concept is active
          if (rel.isActive() && rel.getTypeId().equals(new Long(typeId))
              && rel.getSourceConcept().isActive()) {

            // get source concept from inverse relationship (i.e. child of
            // concept)
            Concept c_rel = rel.getSourceConcept();

            // if set does not contain the source concept, add it to set and
            // queue
            if (!conceptSet.contains(c_rel)) {
              conceptSet.add(c_rel);
              conceptQueue.add(c_rel);

            }
          }
        }
      }
    }

    return conceptSet;
  }

  /**
   * Returns the child concepts.
   * 
   * @param rootConcept the root concept
   * @param typeId the type id
   * @return the child concepts
   * @throws Exception the exception
   */
  public static Set<Concept> getChildConcepts(Concept rootConcept, String typeId)
    throws Exception {

    // relationship set and iterator
    Set<Relationship> inv_relationships = rootConcept.getInverseRelationships();
    Iterator<Relationship> it_inv_rel = inv_relationships.iterator();
    Set<Concept> conceptSet = new HashSet<>();

    // iterate over inverse relationships
    while (it_inv_rel.hasNext()) {

      // get relationship
      Relationship rel = it_inv_rel.next();

      // if relationship is active, typeId equals the provided typeId, and
      // the source concept is active
      if (rel.isActive() && rel.getTypeId().equals(new Long(typeId))
          && rel.getSourceConcept().isActive()) {

        // get source concept from inverse relationship (i.e. child of
        // concept)
        Concept c_rel = rel.getSourceConcept();

        // if set does not contain the source concept, add it to set and
        // queue
        if (!conceptSet.contains(c_rel)) {
          conceptSet.add(c_rel);

        }
      }
    }
    return conceptSet;
  }

}
