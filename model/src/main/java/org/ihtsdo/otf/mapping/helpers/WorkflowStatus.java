package org.ihtsdo.otf.mapping.helpers;

/**
 * The Enum WorkflowStatus.
 */

public enum WorkflowStatus {
  /** New, unedited specialist record */
  NEW, // (can transition to EDITING_IN_PROGRESS, EDITING_DONE)

  /** Editing in progress by a specialist */
  EDITING_IN_PROGRESS, // (can transition to EDITING_DONE)

  /** Editing completed by a specialist */
  EDITING_DONE, // (can transition to CONFILCT_DETECTED, CONSENSUS_NEEDED, or
                // REVIEW_NEEDED)

  /** Conflict has been detected. */
  CONFLICT_DETECTED, // (can transition to CONFLICT_NEW)
  
  /** Conflict has been claimed by a lead, but has not been edited */
  CONFLICT_NEW, // (can transition to CONFLICT_IN_PROGRESS, READY_FOR_PUBLICATION)

  /** Conflict resolution by a lead is in progress */
  CONFLICT_IN_PROGRESS, // (can transition to READY_FOR_PUBLICATION)
  
  /** Pre-publication state for review by lead */
  REVIEW_NEEDED, // (can transition to REVIEW_NEW, REVIEW_IN_PROGRESS)
 
  /** Review has been claimed by a lead, but has not been edited */
  REVIEW_NEW, // (can transition to REVIEW_IN_PROGRESS, READY_FOR_PUBLICATION)
  
  /** Review claimed */
  REVIEW_IN_PROGRESS, // (can transition to READY_FOR_PUBLICATION)
  
  /** The consensus needed. */
  CONSENSUS_NEEDED, // (can transition to CONSENSUS_IN_PROGRESS)
  
  /** The consensus begun, with no editing */
  CONSENSUS_NEW, // (can transition to CONSENSUS_IN_PROGRESS, READY_FOR_PUBLICATION)

  /** The consensus resolved. */
  CONSENSUS_IN_PROGRESS, // (can transition to READY_FOR_PUBLICATION)

  /** The ready for publication. */
  READY_FOR_PUBLICATION, // (can transition to PUBLISHED, REVISION)

  /** The published. */
  PUBLISHED, // (can transition to REVISION)
  
  /** User or QA specified review */ 
  REVISION; // (can transition to REVIEW_NEEDED or to previous state of READY_FOR_PUBLICATION/PUBLISHED)

}
