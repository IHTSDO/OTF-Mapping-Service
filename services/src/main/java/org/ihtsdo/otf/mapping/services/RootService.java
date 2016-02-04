package org.ihtsdo.otf.mapping.services;

import java.util.List;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;

/**
 * Generically represents a service, with common functionality.
 */
public interface RootService {

	/**
	 * Open the factory.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void openFactory() throws Exception;

	/**
	 * Close the factory.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void closeFactory() throws Exception;

	/**
	 * Gets the transaction per operation.
	 *
	 * @return the transaction per operation
	 * @throws Exception
	 *             the exception
	 */
	public boolean getTransactionPerOperation() throws Exception;

	/**
	 * Sets the transaction per operation.
	 *
	 * @param transactionPerOperation
	 *            the new transaction per operation
	 * @throws Exception
	 *             the exception
	 */
	public void setTransactionPerOperation(boolean transactionPerOperation) throws Exception;

	/**
	 * Commit.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void commit() throws Exception;

	/**
	 * Rollback.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void rollback() throws Exception;

	/**
	 * Begin transaction.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void beginTransaction() throws Exception;

	/**
	 * Closes the manager.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public void close() throws Exception;

	/**
	 * Clears all service resources.
	 *
	 * @throws Exception the exception
	 */
	public void clear() throws Exception;

  /**
   * Apply pfs to list.
   *
   * @param <T> the
   * @param list the list
   * @param clazz the clazz
   * @param totalCt the total ct
   * @param pfs the pfs
   * @return the list
   * @throws Exception the exception
   */
  public <T> List<T> applyPfsToList(List<T> list, Class<T> clazz,
    int[] totalCt, PfsParameter pfs) throws Exception;
}
