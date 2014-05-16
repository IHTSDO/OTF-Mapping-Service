package org.ihtsdo.otf.mapping.helpers;

/**
 * The Enum WorkflowStatus.
 * 
 * @author ${author}
 */

public enum WorkflowStatus {
  /** The new. */
  NEW,

  /** The editing in progress. */
  EDITING_IN_PROGRESS,

  /** The editing done. */
  EDITING_DONE, // (can transition to CONFILCT_DETECTED, CONSENSUS_NEEDED, or
                // READY_FOR_PUBLICATION)

  /** The conflict detected. */
  CONFLICT_DETECTED, // (can transition to READY_FOR_PUBLICATION)
  
  /** The unedited conflict state */
  CONFLICT_NEW, // (can transition to CONFLICT_IN_PROGRESS, READY_FOR_PUBLICATION)

  /** The conflict in progress. */
  CONFLICT_IN_PROGRESS, // (can transition to READY_FOR_PUBLICATION)

  /** The consensus needed. */
  CONSENSUS_NEEDED, // (can transition to CONSENSUS_RESOVLED)

  /** The consensus resolved. */
  CONSENSUS_RESOLVED, // (can transition to READY_FOR_PUBLICATION)

  /** The ready for publication. */
  READY_FOR_PUBLICATION, // (can transition to PUBLISHED)

  /** The published. */
  PUBLISHED,

  /** User or QA specified review */
  REVIEW;

}
