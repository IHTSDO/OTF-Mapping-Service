package org.ihtsdo.otf.mapping.jpa.services;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
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

}
