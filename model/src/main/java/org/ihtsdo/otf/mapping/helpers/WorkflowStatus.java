package org.ihtsdo.otf.mapping.helpers;

/**
 * Enum for workflow status values. Comprehensive for all workflow paths.
 * 
 * NOTE: Workflow status names may not be initial substrings of any other
 * workflow status. For instance, REVIEW and REVIEW_NEW cannot both be
 * specified, since REVIEW_NEW begins with REVIEW. Other substring matches are
 * allowed, e.g. NEW and REVIEW_NEW can both be values.
 * 
 * NOTE: Workflow statuses must be in ascending order, as defined by the
 * workflow diagrams. For instance, NEW must appear before EDITING_IN_PROGRESS.
 * The top three statuses MUST be READY_FOR_PUBLICATION, PUBLISHED, and
 * REVISION, in that order
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
  CONFLICT_NEW, // (can transition to CONFLICT_IN_PROGRESS,
                // READY_FOR_PUBLICATION)

  /** Conflict resolution by a lead is in progress */
  CONFLICT_IN_PROGRESS, // (can transition to CONFLICT_RESOLVED)

  /**
   * Conflict resolution by a lead is resolved, but not released (can transition
   * to READY_FOR_PUBLICATION)
   */
  CONFLICT_RESOLVED,

  /** Pre-publication state for review by lead */
  REVIEW_NEEDED, // (can transition to REVIEW_NEW, REVIEW_IN_PROGRESS)

  /** Review has been claimed by a lead, but has not been edited */
  REVIEW_NEW, // (can transition to REVIEW_IN_PROGRESS, READY_FOR_PUBLICATION)

  /** Review claimed */
  REVIEW_IN_PROGRESS, // (can transition to REVIEW_RESOLVED)

  /**
   * Review resolved, but not released (can transition to READY_FOR_PUBLICATION)
   */
  REVIEW_RESOLVED,

  /** Pre-publication state for qa */
  QA_NEEDED, // (can transition to QA_NEW, QA_IN_PROGRESS)

  /** QA has been claimed, but has not been edited */
  QA_NEW, // (can transition to QA_IN_PROGRESS, READY_FOR_PUBLICATION)

  /** QA claimed */
  QA_IN_PROGRESS, // (can transition to QA_RESOLVED)

  /** QA resolved, but not released (can transition to READY_FOR_PUBLICATION) */
  QA_RESOLVED,

  /** The consensus needed. */
  CONSENSUS_NEEDED, // (can transition to CONSENSUS_IN_PROGRESS)

  /** The consensus begun, with no editing */
  CONSENSUS_NEW, // (can transition to CONSENSUS_IN_PROGRESS,
                 // READY_FOR_PUBLICATION)

  /** The consensus resolved. */
  CONSENSUS_IN_PROGRESS, // (can transition to READY_FOR_PUBLICATION)

  /** The ready for publication. */
  READY_FOR_PUBLICATION, // (can transition to PUBLISHED, REVISION)

  /** The published. */
  PUBLISHED, // (can transition to REVISION)

  /** User or QA specified review */
  REVISION; // (can transition to REVIEW_NEEDED or to previous state of
            // READY_FOR_PUBLICATION/PUBLISHED)

}
