package org.ihtsdo.otf.mapping.helpers;

/**
 * Enums for workflow actions.
 */
public enum WorkflowAction {

  /** The assign from scratch. */
  ASSIGN_FROM_SCRATCH,

  /** The assign from initial record. */
  ASSIGN_FROM_INITIAL_RECORD,

  /** The unassign. */
  UNASSIGN,

  /** The save for later. */
  SAVE_FOR_LATER,

  /** The finish editing. */
  FINISH_EDITING,

  /** The publish */
  PUBLISH,

  /** Cancel work */
  CANCEL,

  /** Create qa record */
  CREATE_QA_RECORD;
}
