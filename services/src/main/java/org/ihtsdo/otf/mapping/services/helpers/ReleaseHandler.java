package org.ihtsdo.otf.mapping.services.helpers;

import java.util.List;

import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;

/**
 * Generically represents a handler for performing a release.
 */
public interface ReleaseHandler {

  /**
   * Process both snapshot and delta release for all publication-ready records
   * for a project.
   *
   * @throws Exception the exception
   */
  public void processRelease() throws Exception;

  /**
   * Perform checks and record modification prior to release.
   *
   * @throws Exception the exception
   */
  public void beginRelease() throws Exception;

  /**
   * Finish release.
   *
   * @throws Exception the exception
   */
  public void finishRelease() throws Exception;

  /**
   * Close.
   *
   * @throws Exception the exception
   */
  public void close() throws Exception;

  /**
   * Sets the effective time.
   *
   * @param effectiveTime the effectiveTime to set
   */
  public void setEffectiveTime(String effectiveTime);

  /**
   * Sets the module id.
   *
   * @param moduleId the moduleId to set
   */
  public void setModuleId(String moduleId);

  /**
   * Sets the output dir.
   *
   * @param outputDir the output dir
   */
  public void setOutputDir(String outputDir);

  /**
   * Sets the write snapshot.
   *
   * @param writeSnapshot the writeSnapshot to set
   */
  public void setWriteSnapshot(boolean writeSnapshot);

  /**
   * Sets the write delta.
   *
   * @param writeDelta the writeDelta to set
   */
  public void setWriteDelta(boolean writeDelta);

  /**
   * Sets the map project.
   *
   * @param mapProject the mapProject to set
   * @throws Exception the exception
   */
  public void setMapProject(MapProject mapProject) throws Exception;

  /**
   * Sets the map records.
   *
   * @param mapRecords the map records
   */
  public void setMapRecords(List<MapRecord> mapRecords);

  /**
   * Sets the input file.
   *
   * @param inputFile the input file
   */
  public void setInputFile(String inputFile);
}
