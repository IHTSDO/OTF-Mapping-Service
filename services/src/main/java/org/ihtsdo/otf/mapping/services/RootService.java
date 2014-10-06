package org.ihtsdo.otf.mapping.services;

// TODO: Auto-generated Javadoc
/**
 * The Interface RootService. Manages Factory and lucene field names
 */
public interface RootService {

  /**
   * Open the factory.
   *
   * @throws Exception the exception
   */
  public void openFactory() throws Exception;

  /**
   * Close the factory.
   *
   * @throws Exception the exception
   */
  public void closeFactory() throws Exception;

  /**
   * Initialize field names.
   *
   * @throws Exception the exception
   */
  public void initializeFieldNames() throws Exception;

  /**
   * Gets the transaction per operation.
   *
   * @return the transaction per operation
   * @throws Exception
   */
  public boolean getTransactionPerOperation() throws Exception;

  /**
   * Sets the transaction per operation.
   *
   * @param transactionPerOperation the new transaction per operation
   * @throws Exception 
   */
  public void setTransactionPerOperation(boolean transactionPerOperation)
    throws Exception;

  /**
   * Commit.
   * @throws Exception 
   */
  public void commit() throws Exception;

  /**
   * Begin transaction.
   * @throws Exception 
   */
  public void beginTransaction() throws Exception;

  /**
   * Closes the manager
   * @throws Exception 
   */
  public void close() throws Exception;

}
