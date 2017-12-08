package org.ihtsdo.otf.mapping.services;

import java.util.List;

import org.ihtsdo.otf.mapping.helpers.LogEntry;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;

/**
 * Generically represents a service, with common functionality.
 */
public interface RootService extends AutoCloseable {

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
   * Gets the transaction per operation.
   *
   * @return the transaction per operation
   * @throws Exception the exception
   */
  public boolean getTransactionPerOperation() throws Exception;

  /**
   * Sets the transaction per operation.
   *
   * @param transactionPerOperation the new transaction per operation
   * @throws Exception the exception
   */
  public void setTransactionPerOperation(boolean transactionPerOperation)
    throws Exception;

  /**
   * Commit.
   *
   * @throws Exception the exception
   */
  public void commit() throws Exception;

  /**
   * Rollback.
   *
   * @throws Exception the exception
   */
  public void rollback() throws Exception;

  /**
   * Begin transaction.
   *
   * @throws Exception the exception
   */
  public void beginTransaction() throws Exception;

  /**
   * Closes the manager.
   *
   * @throws Exception the exception
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
  public <T> List<T> applyPfsToList(List<T> list, Class<T> clazz, int[] totalCt,
    PfsParameter pfs) throws Exception;

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
  public <T> List<?> getQueryResults(String query, Class<?> fieldNamesKey,
    Class<?> clazz, PfsParameter pfs, int[] totalCt) throws Exception;
  
  /**
   * Returns the last modified by.
   *
   * @return the last modified by
   */
  public String getLastModifiedBy();

  /**
   * Sets the last modified by.
   *
   * @param lastModifiedBy the last modified by
   */
  public void setLastModifiedBy(String lastModifiedBy);

  /**
   * Update log entry.
   *
   * @param logEntry the log entry
   * @throws Exception the exception
   */
	public void updateLogEntry(LogEntry logEntry) throws Exception;

  /**
   * Removes the log entry.
   *
   * @param id the id
   * @throws Exception the exception
   */
	public void removeLogEntry(Long id) throws Exception;

  /**
   * Gets the log entry.
   *
   * @param id the id
   * @return the log entry
   * @throws Exception the exception
   */
	public LogEntry getLogEntry(Long id) throws Exception;

  /**
   * Adds the log entry.
   *
   * @param logEntry the log entry
   * @return the log entry
   * @throws Exception the exception
   */
	public LogEntry addLogEntry(LogEntry logEntry) throws Exception;

  /**
   * Adds the log entry.
   *
   * @param userName the user name
   * @param terminology the terminology
   * @param version the version
   * @param activityId the activity id
   * @param workId the work id
   * @param message the message
   * @return the log entry
   * @throws Exception the exception
   */
	public LogEntry addLogEntry(String userName, String terminology,
			String version, String activityId, String workId, String message)
			throws Exception;

  /**
   * Adds the log entry.
   *
   * @param userName the user name
   * @param projectId the project id
   * @param objectId the object id
   * @param activityId the activity id
   * @param workId the work id
   * @param message the message
   * @return the log entry
   * @throws Exception the exception
   */
	public LogEntry addLogEntry(String userName, Long projectId, Long objectId,
			String activityId, String workId, String message) throws Exception;

  /**
   * Adds the log entry.
   *
   * @param projectId the project id
   * @param userName the user name
   * @param terminology the terminology
   * @param version the version
   * @param activityId the activity id
   * @param workId the work id
   * @param message the message
   * @return the log entry
   * @throws Exception the exception
   */
	public LogEntry addLogEntry(final Long projectId, final String userName,
			final String terminology, final String version,
			final String activityId, final String workId, final String message)
			throws Exception;
  
}
