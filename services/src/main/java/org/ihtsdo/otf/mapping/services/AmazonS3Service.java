/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.services;

import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.model.MapProject;

/**
 * Generically represents a service for interacting with objects stored on
 * amazon s3.
 */
public interface AmazonS3Service extends RootService {

  /**
   * Closes the manager associated with service.
   * 
   * @throws Exception the exception
   */
  @Override
  public void close() throws Exception;

  // ////////////////////////////
  // Basic retrieval services //
  // ////////////////////////////

  /**
   * Returns the file list from amazon S 3.
   *
   * @param mapProject the map project
   * @return the file list from amazon S 3
   * @throws Exception the exception
   */
  public SearchResultList getFileListFromAmazonS3(MapProject mapProject)
    throws Exception;

}
