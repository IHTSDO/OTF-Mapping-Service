package org.ihtsdo.otf.mapping.services;

import java.util.List;

import org.ihtsdo.otf.mapping.helpers.PfsParameter;

// TODO: Auto-generated Javadoc
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
	 * Gets the query results.
	 *
	 * @param <T> the generic type
	 * @param query the query
	 * @param fieldNamesKey the field names key
	 * @param clazz the clazz
	 * @param pfs the pfs
	 * @param totalCt the total ct
	 * @return the query results
	 * @throws Exception the exception
	 */
	public <T> List<?> getQueryResults(String query, Class<?> fieldNamesKey, Class<T> clazz, PfsParameter pfs,
			int[] totalCt) throws Exception;

}
