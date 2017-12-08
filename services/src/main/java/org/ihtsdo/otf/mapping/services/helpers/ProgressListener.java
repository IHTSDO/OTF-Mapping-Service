/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.services.helpers;

/**
 * Generically listens for progress updates.
 *
 * @see ProgressEvent
 */
public interface ProgressListener {

  /**
   * Update progress.
   *
   * @param pe the pe
   */
  public void updateProgress(ProgressEvent pe);

}
