package org.ihtsdo.otf.mapping.jpa.services;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.search.jpa.FullTextQuery;
import org.ihtsdo.otf.mapping.helpers.HasLastModified;
import org.ihtsdo.otf.mapping.helpers.LogEntry;
import org.ihtsdo.otf.mapping.helpers.LogEntryJpa;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.jpa.helpers.IndexUtility;
import org.ihtsdo.otf.mapping.services.RootService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

// TODO: Auto-generated Javadoc
/**
 * The root service for managing the entity manager factory and hibernate search
 * field names.
 *
 * @author ${author}
 */
public abstract class RootServiceJpa implements RootService {

  /** The commit count. */
  final static int commitCt = 2000;

  /** The factory. */
  protected static EntityManagerFactory factory;

  /** The manager. */
  protected EntityManager manager;

  /** The transaction per operation. */
  protected boolean transactionPerOperation = true;

  /** The transaction entity. */
  protected EntityTransaction tx;

  /** The last modified by. */
  private String lastModifiedBy = null;

  /** The process lock. */
  private static String processLock = "";

  /**
   * Instantiates an empty {@link RootServiceJpa}.
   *
   * @throws Exception the exception
   */
  public RootServiceJpa() throws Exception {
    // created once or if the factory has closed
    if (factory == null || !factory.isOpen()) {
      openFactory();
    }

    // created on each instantiation
    manager = factory.createEntityManager();
    tx = manager.getTransaction();
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.RootService#openFactory()
   */
  @Override
  public synchronized void openFactory() throws Exception {

    // if factory has not been instantiated or has been closed, open it
    if (factory == null || !factory.isOpen()) {

      Logger.getLogger(this.getClass())
          .info("Setting root service entity manager factory.");
      Properties config = ConfigUtility.getConfigProperties();
      factory =
          Persistence.createEntityManagerFactory("MappingServiceDS", config);
    }

  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.RootService#closeFactory()
   */
  @Override
  public void closeFactory() throws Exception {
    if (factory.isOpen()) {
      factory.close();
    }
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#getTransactionPerOperation
   * ()
   */
  @Override
  public boolean getTransactionPerOperation() {
    return transactionPerOperation;
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.MappingService#setTransactionPerOperation
   * (boolean)
   */
  @Override
  public void setTransactionPerOperation(boolean transactionPerOperation) {
    this.transactionPerOperation = transactionPerOperation;
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#beginTransaction()
   */
  @Override
  public void beginTransaction() {

    if (getTransactionPerOperation())
      throw new IllegalStateException(
          "Error attempting to begin a transaction when using transactions per operation mode.");
    else if (tx != null && tx.isActive())
      throw new IllegalStateException(
          "Error attempting to begin a transaction when there "
              + "is already an active transaction");
    tx = manager.getTransaction();
    tx.begin();
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.MappingService#commit()
   */
  @Override
  public void commit() throws Exception {

    if (getTransactionPerOperation())
      throw new IllegalStateException(
          "Error attempting to commit a transaction when using transactions per operation mode.");
    else if (tx != null && !tx.isActive())
      throw new IllegalStateException(
          "Error attempting to commit a transaction when there "
              + "is no active transaction");
    if (tx == null) {
      throw new Exception("Attempting to commit a null transaction.");
    }
    tx.commit();
    manager.clear();
  }

  /* see superclass */
  @Override
  public void rollback() throws Exception {

    if (getTransactionPerOperation())
      throw new IllegalStateException(
          "Error attempting to rollback a transaction when using transactions per operation mode.");
    else if (tx != null && !tx.isActive())
      throw new IllegalStateException(
          "Error attempting to rollback a transaction when there "
              + "is no active transaction");
    if (tx == null) {
      throw new Exception("Attempting to rollback a null transaction.");
    }
    tx.rollback();
    manager.clear();
  }

  /* see superclass */
  @Override
  public void clear() throws Exception {
    manager.clear();
  }

  /* see superclass */
  @Override
  public void close() throws Exception {
    if (manager.isOpen()) {
      manager.close();
    }
  }

  /**
   * Returns the query results.
   *
   * @param <T> the
   * @param query the query
   * @param fieldNamesKey the field names key
   * @param clazz the clazz
   * @param pfs the pfs
   * @param totalCt the total ct
   * @return the query results
   * @throws Exception the exception
   */
  @Override
  public <T> List<?> getQueryResults(String query, Class<?> fieldNamesKey,
    Class<?> clazz, PfsParameter pfs, int[] totalCt) throws Exception {

    // TODO Removed this to allow blank queries for map records, discuss if this
    // impacts other areas of application
    if (query == null || query.isEmpty()) {
      // Logger.getLogger(this.getClass()).info("Empty query supplied");
      // throw new Exception("Unexpected empty query.");
    }

    FullTextQuery fullTextQuery = null;
    try {
      fullTextQuery = IndexUtility.applyPfsToLuceneQuery(clazz, fieldNamesKey,
          query, pfs, manager);
    } catch (ParseException e) {
      // If parse exception, try a literal query
      StringBuilder escapedQuery = new StringBuilder();
      if (query != null && !query.isEmpty()) {
        escapedQuery.append(QueryParser.escape(query));
      }
      fullTextQuery = IndexUtility.applyPfsToLuceneQuery(clazz, fieldNamesKey,
          escapedQuery.toString(), pfs, manager);
    }

    totalCt[0] = fullTextQuery.getResultSize();

    return fullTextQuery.getResultList();

  }

  /* see superclass */
  // this is called by REST layer and so needs to be exposed through RootService
  @Override
  public <T> List<T> applyPfsToList(List<T> list, Class<T> clazz, int[] totalCt,
    PfsParameter pfs) throws Exception {

    // Skip empty pfs
    if (pfs == null) {
      return list;
    }

    // NOTE: does not handle active/inactive logic

    List<T> result = list;

    // Handle sorting

    // apply paging, and sorting if appropriate
    if (pfs != null
        && (pfs.getSortField() != null && !pfs.getSortField().isEmpty())) {

      // check that specified sort field exists on Concept and is
      // a string
      final Method sortMethod =
          clazz.getMethod("get" + ConfigUtility.capitalize(pfs.getSortField()),
              new Class<?>[] {});

      if (!sortMethod.getReturnType().equals(String.class)
          && !sortMethod.getReturnType().isEnum()
          && !sortMethod.getReturnType().equals(Date.class)) {
        throw new Exception("Referenced sort field is not of type String");
      }

      // allow the method to be accessed
      sortMethod.setAccessible(true);

      final boolean ascending = true;

      // sort the list
      Collections.sort(result, new Comparator<T>() {
        @Override
        public int compare(T t1, T t2) {
          // if an exception is returned, simply pass equality
          try {
            final String s1 = (String) sortMethod.invoke(t1, new Object[] {});
            final String s2 = (String) sortMethod.invoke(t2, new Object[] {});
            if (ascending) {
              return s1.compareTo(s2);
            } else {
              return s2.compareTo(s1);
            }
          } catch (Exception e) {
            return 0;
          }
        }
      });
    }

    // Set total count before filtering
    totalCt[0] = result.size();

    // Handle filtering based on toString()
    if (pfs != null && (pfs.getQueryRestriction() != null
        && !pfs.getQueryRestriction().isEmpty())) {

      // Strip last char off if it is a *
      String match = pfs.getQueryRestriction();
      if (match.lastIndexOf('*') == match.length() - 1) {
        match = match.substring(0, match.length() - 1);
      }
      final List<T> filteredResult = new ArrayList<T>();
      for (T t : result) {
        if (t.toString().toLowerCase().indexOf(match.toLowerCase()) != -1) {
          filteredResult.add(t);
        }
      }

      if (filteredResult.size() != result.size()) {
        result = filteredResult;
      }
    }

    // get the start and end indexes based on paging parameters
    int startIndex = 0;
    int toIndex = result.size();
    if (pfs != null && pfs.getStartIndex() != -1) {
      startIndex = pfs.getStartIndex();
      toIndex = Math.min(result.size(), startIndex + pfs.getMaxResults());
      if (startIndex > toIndex) {
        startIndex = 0;
      }
      result = result.subList(startIndex, toIndex);
    }

    return result;
  }

  /**
   * Returns the entity manager.
   *
   * @return the entity manager
   */
  public EntityManager getEntityManager() {
    return manager;
  }

  /**
   * Returns the last modified by.
   *
   * @return the last modified by
   */
  @Override
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  /**
   * Sets the last modified by.
   *
   * @param lastModifiedBy the last modified by
   */
  @Override
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  /* see superclass */
  @Override
  public LogEntry addLogEntry(final LogEntry logEntry) throws Exception {
    // Use add object to bypass the last modified checks
    logEntry.setLastModified(new Date());
    return addObject(logEntry);
  }

  /* see superclass */
  @Override
  public void updateLogEntry(final LogEntry logEntry) throws Exception {
    // Use add object to bypass the last modified checks
    logEntry.setLastModified(new Date());
    updateObject(logEntry);
  }

  /* see superclass */
  @Override
  public void removeLogEntry(final Long id) throws Exception {
    // Use add object to bypass the last modified checks
    removeObject(getObject(id, LogEntryJpa.class));
  }

  /* see superclass */
  @Override
  public LogEntry getLogEntry(final Long id) throws Exception {
    return getHasLastModified(id, LogEntry.class);
  }

  /* see superclass */
  @Override
  public LogEntry addLogEntry(final String userName, final Long projectId,
    final Long objectId, final String activityId, final String workId,
    final String message) throws Exception {
    final LogEntry entry = new LogEntryJpa();
    entry.setLastModifiedBy(userName);
    entry.setObjectId(objectId);
    entry.setProjectId(projectId);
    entry.setTimestamp(new Date());
    entry.setActivityId(activityId);
    entry.setWorkId(workId);
    entry.setMessage(message);

    // Add component
    return addLogEntry(entry);

  }

  /* see superclass */
  @Override
  public LogEntry addLogEntry(final String userName, final String terminology,
    final String version, final String activityId, final String workId,
    final String message) throws Exception {
    final LogEntry entry = new LogEntryJpa();
    entry.setLastModifiedBy(userName);
    entry.setTerminology(terminology);
    entry.setVersion(version);
    entry.setTimestamp(new Date());
    entry.setMessage(message);
    entry.setActivityId(activityId);
    entry.setWorkId(workId);

    // Add component
    return addLogEntry(entry);

  }

  /* see superclass */
  @Override
  public LogEntry addLogEntry(final Long projectId, final String userName,
    final String terminology, final String version, final String activityId,
    final String workId, final String message) throws Exception {
    final LogEntry entry = new LogEntryJpa();
    entry.setProjectId(projectId);
    entry.setLastModifiedBy(userName);
    entry.setTerminology(terminology);
    entry.setVersion(version);
    entry.setTimestamp(new Date());
    entry.setMessage(message);
    entry.setActivityId(activityId);
    entry.setWorkId(workId);

    // Add component
    return addLogEntry(entry);

  }

  /**
   * Returns the checks for object.
   *
   * @param <T> the
   * @param id the id
   * @param clazz the clazz
   * @return the checks for object
   * @throws Exception the exception
   */
  protected <T extends Object> T getObject(final Long id, final Class<T> clazz)
    throws Exception {
    // Get transaction and object
    tx = manager.getTransaction();
    final T component = manager.find(clazz, id);
    return component;
  }

  /**
   * Adds the object.
   *
   * @param <T> the
   * @param object the object
   * @return the t
   * @throws Exception the exception
   */
  protected <T extends Object> T addObject(final T object) throws Exception {
    try {
      // add
      if (getTransactionPerOperation()) {
        tx = manager.getTransaction();
        tx.begin();
        manager.persist(object);
        tx.commit();
      } else {
        manager.persist(object);
      }
      return object;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  /**
   * Update object.
   *
   * @param <T> the
   * @param object the object
   * @throws Exception the exception
   */
  protected <T extends Object> void updateObject(final T object)
    throws Exception {
    try {
      // update
      if (getTransactionPerOperation()) {
        tx = manager.getTransaction();
        tx.begin();
        manager.merge(object);
        tx.commit();
      } else {
        manager.merge(object);
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  /**
   * Removes the object.
   *
   * @param <T> the
   * @param object the object
   * @return the t
   * @throws Exception the exception
   */
  protected <T extends Object> T removeObject(final T object) throws Exception {
    try {
      // Get transaction and object
      tx = manager.getTransaction();
      // Remove
      if (getTransactionPerOperation()) {
        // remove refset member
        tx.begin();
        if (manager.contains(object)) {
          manager.remove(object);
        } else {
          manager.remove(manager.merge(object));
        }
        tx.commit();
      } else {
        if (manager.contains(object)) {
          manager.remove(object);
        } else {
          manager.remove(manager.merge(object));
        }
      }
      return object;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  /**
   * Returns the checks for last modified.
   *
   * @param <T> the
   * @param id the id
   * @param clazz the clazz
   * @return the checks for last modified
   * @throws Exception the exception
   */
  protected <T extends HasLastModified> T getHasLastModified(final Long id,
    final Class<T> clazz) throws Exception {
    if (id == null) {
      return null;
    }
    // Get transaction and object
    tx = manager.getTransaction();
    final T component = manager.find(clazz, id);
    return component;
  }

  public synchronized static void lockProcess(String processInfo)
    throws Exception {
    // If processLock is populated, return the existing processInfo as an
    // Exception
    if (!processLock.equals("")) {
      throw new Exception(processLock);
    }
    // Otherwise, populate the processLock with the upcoming processes' info
    else {
      processLock = processInfo;
    }
  }

  public synchronized static void unlockProcess() {
    // clear out the processLock
    processLock = "";
  }

}
