/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.jpa.helpers.LoggerUtility;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressEvent;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;
import org.reflections.Reflections;

/**
 * Implementation of an algorithm to reindex all classes annotated with @Indexed
 */
public class LuceneReindexAlgorithm extends RootServiceJpa implements Algorithm {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** The request cancel flag. */
  boolean requestCancel = false;

  /** The terminology. */
  private String indexedObjects;

  /** The full text entity manager. */
  private FullTextEntityManager fullTextEntityManager;
  
  /** The log. */
  private static Logger log;

  /** The log file. */
  private File logFile;

  /**
   * Instantiates an empty {@link LuceneReindexAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public LuceneReindexAlgorithm() throws Exception {
    super();
    
    
    //initialize logger
    String rootPath = ConfigUtility.getConfigProperties()
          .getProperty("map.principle.source.document.dir");
    if (!rootPath.endsWith("/") && !rootPath.endsWith("\\")) {
      rootPath += "/";
    }
    rootPath += "logs";
    File logDirectory = new File(rootPath);
    if (!logDirectory.exists()) {
        logDirectory.mkdir();
    }
    
    logFile = new File(logDirectory, "reindex.log");
    LoggerUtility.setConfiguration("reindex", logFile.getAbsolutePath());
    this.log = LoggerUtility.getLogger("reindex");
  }

  /**
   * Sets the indexed objects.
   *
   * @param indexedObjects the indexed objects
   */
  public void setIndexedObjects(String indexedObjects) {
    this.indexedObjects = indexedObjects;
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    
    // clear log before starting process
    PrintWriter writer = new PrintWriter(logFile);
    writer.print("");
    writer.close(); 
   
    if (fullTextEntityManager == null) {
      fullTextEntityManager = Search.getFullTextEntityManager(manager);
    }
    computeLuceneIndexes(indexedObjects);
    // fullTextEntityManager.close();
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    if (fullTextEntityManager == null) {
      fullTextEntityManager = Search.getFullTextEntityManager(manager);
    }
    clearLuceneIndexes();
    // fullTextEntityManager.close();
  }

  /**
   * Compute lucene indexes.
   *
   * @param indexedObjects the indexed objects
   * @throws Exception the exception
   */
  private void computeLuceneIndexes(String indexedObjects) throws Exception {
    try {
    // set of objects to be re-indexed
    final Set<String> objectsToReindex = new HashSet<>();
    final Map<String, Class<?>> reindexMap = new HashMap<>();
    final Reflections reflections = new Reflections("org.ihtsdo.otf");
    for (final Class<?> clazz : reflections
        .getTypesAnnotatedWith(Indexed.class)) {
      reindexMap.put(clazz.getSimpleName(), clazz);
    }

    // if no parameter specified, re-index all objects
    if (indexedObjects == null || indexedObjects.isEmpty()) {

      // Add all class names
      for (final String className : reindexMap.keySet()) {
        if (objectsToReindex.contains(className)) {
          // This restriction can be removed by using full class names
          // however, then calling the mojo is more complicated
          throw new Exception(
              "Reindex process assumes simple class names are different.");
        }
        objectsToReindex.add(className);
      }

      // otherwise, construct set of indexed objects
    } else {

      // remove white-space and split by comma
      String[] objects = indexedObjects.replaceAll(" ", "").split(",");

      // add each value to the set
      for (String object : objects)
        objectsToReindex.add(object);

    }

    Logger.getLogger(getClass()).info("Starting reindexing for:");
    for (String objectToReindex : objectsToReindex) {
      Logger.getLogger(getClass()).info("  " + objectToReindex);
    }

    // Reindex each object
    for (final String key : reindexMap.keySet()) {
      // Concepts
      if (objectsToReindex.contains(key)) {
        Logger.getLogger(getClass()).info("  Creating indexes for " + key);
        fullTextEntityManager.purgeAll(reindexMap.get(key));
        fullTextEntityManager.flushToIndexes();
//        if (key.contains("MapRecordJpa")) {
//        	fullTextEntityManager.setProperty(ROLE, ROLE);
//        }
        fullTextEntityManager.createIndexer(reindexMap.get(key))
            .batchSizeToLoadObjects(100)
            .cacheMode(CacheMode.NORMAL)
            .threadsToLoadObjects(4)
            .threadsForSubsequentFetching(8)
            .startAndWait();

        objectsToReindex.remove(key);
      }
    }

    if (objectsToReindex.size() != 0) {
      throw new Exception(
          "The following objects were specified for re-indexing, but do not exist as indexed objects: "
              + objectsToReindex.toString());
    }

    // Cleanup
    Logger.getLogger(getClass()).info("done ...");
    } catch(Exception e) {
      log.error(e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        log.error(element.toString());
      }
      throw new Exception(e);
    }
  }

  /**
   * Clear lucene indexes.
   *
   * @throws Exception the exception
   */
  private void clearLuceneIndexes() throws Exception {

    final Reflections reflections = new Reflections("org.ihtsdo.otf");
    for (final Class<?> clazz : reflections
        .getTypesAnnotatedWith(Indexed.class)) {
      fullTextEntityManager.purgeAll(clazz);
    }
  }

  /**
   * Fires a {@link ProgressEvent}.
   * @param pct percent done
   * @param note progress note
   */
  public void fireProgressEvent(int pct, String note) {
    ProgressEvent pe = new ProgressEvent(this, pct, pct, note);
    for (int i = 0; i < listeners.size(); i++) {
      listeners.get(i).updateProgress(pe);
    }
    Logger.getLogger(getClass()).info("    " + pct + "% " + note);
  }

  /* see superclass */
  @Override
  public void addProgressListener(ProgressListener l) {
    listeners.add(l);
  }

  /* see superclass */
  @Override
  public void removeProgressListener(ProgressListener l) {
    listeners.remove(l);
  }

  /* see superclass */
  @Override
  public void cancel() {
    requestCancel = true;
  }

//  /* see superclass */
//  @Override
//  public void refreshCaches() throws Exception {
//    // n/a
//  }

  @Override
  public void checkPreconditions() throws Exception {
    // n/a
  }

}
