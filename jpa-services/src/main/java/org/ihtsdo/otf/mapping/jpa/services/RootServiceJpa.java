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
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.jpa.helpers.IndexUtility;
import org.ihtsdo.otf.mapping.services.RootService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * The root service for managing the entity manager factory and hibernate search
 * field names
 */
public abstract class RootServiceJpa implements RootService {

  /** The factory. */
  protected static EntityManagerFactory factory;

  /** The manager. */
  protected EntityManager manager;

  /** The transaction per operation. */
  protected boolean transactionPerOperation = true;

  /** The transaction entity. */
  protected EntityTransaction tx;

  /**
   * Instantiates an empty {@link RootServiceJpa}.
   * 
   * @throws Exception
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

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.RootService#openFactory()
   */
  @Override
  public synchronized void openFactory() throws Exception {

    // if factory has not been instantiated or has been closed, open it
    if (factory == null || !factory.isOpen()) {

      Logger.getLogger(this.getClass()).info(
          "Setting root service entity manager factory.");
      Properties config = ConfigUtility.getConfigProperties();
      factory =
          Persistence.createEntityManagerFactory("MappingServiceDS", config);
    }

  }

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

  @Override
  public void clear() throws Exception {
    manager.clear();
  }

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

    if (query == null || query.isEmpty()) {
      throw new Exception("Unexpected empty query.");
    }

    FullTextQuery fullTextQuery = null;
    try {
      fullTextQuery =
          IndexUtility.applyPfsToLuceneQuery(clazz, fieldNamesKey, query, pfs,
              manager);
    } catch (ParseException e) {
      // If parse exception, try a literal query
      StringBuilder escapedQuery = new StringBuilder();
      if (query != null && !query.isEmpty()) {
        escapedQuery.append(QueryParser.escape(query));
      }
      fullTextQuery =
          IndexUtility.applyPfsToLuceneQuery(clazz, fieldNamesKey,
              escapedQuery.toString(), pfs, manager);
    }

    totalCt[0] = fullTextQuery.getResultSize();
    
    // TODO REmove after debugging
    Logger.getLogger(this.getClass()).info("  Query " + query + "\n   Results:" + fullTextQuery.getResultList().size() + "/" + totalCt[0]);
    return fullTextQuery.getResultList();

  }

  // this is called by REST layer and so needs to be exposed through RootService
  @Override
  public <T> List<T> applyPfsToList(List<T> list, Class<T> clazz,
    int[] totalCt, PfsParameter pfs) throws Exception {

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
    if (pfs != null
        && (pfs.getQueryRestriction() != null && !pfs.getQueryRestriction()
            .isEmpty())) {

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
}
