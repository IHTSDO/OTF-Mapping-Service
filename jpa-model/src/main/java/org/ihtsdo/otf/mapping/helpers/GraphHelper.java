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
        Set<Relationship> invRelationships = c.getInverseRelationships();
        Iterator<Relationship> invRelIterator = invRelationships.iterator();

        // iterate over inverse relationships
        while (invRelIterator.hasNext()) {

          // get relationship
          Relationship rel = invRelIterator.next();

          // if relationship is active, typeId equals the provided typeId, and
          // the source concept is active
          if (rel.isActive() && rel.getTypeId().equals(new Long(typeId))
              && rel.getSourceConcept().isActive()) {

            // get source concept from inverse relationship (i.e. child of
            // concept)
            Concept sourceConcept = rel.getSourceConcept();

            // if set does not contain the source concept, add it to set and
            // queue
            if (!conceptSet.contains(sourceConcept)) {
              conceptSet.add(sourceConcept);
              conceptQueue.add(sourceConcept);

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
    Set<Relationship> invRelationships = rootConcept.getInverseRelationships();
    Iterator<Relationship> invRelIterator = invRelationships.iterator();
    Set<Concept> conceptSet = new HashSet<>();

    // iterate over inverse relationships
    while (invRelIterator.hasNext()) {

      // get relationship
      Relationship rel = invRelIterator.next();

      // if relationship is active, typeId equals the provided typeId, and
      // the source concept is active
      if (rel.isActive() && rel.getTypeId().equals(new Long(typeId))
          && rel.getSourceConcept().isActive()) {

        // get source concept from inverse relationship (i.e. child of
        // concept)
        Concept sourceConcept = rel.getSourceConcept();

        // if set does not contain the source concept, add it to set and
        // queue
        if (!conceptSet.contains(sourceConcept)) {
          conceptSet.add(sourceConcept);

        }
      }
    }
    return conceptSet;
  }

}
