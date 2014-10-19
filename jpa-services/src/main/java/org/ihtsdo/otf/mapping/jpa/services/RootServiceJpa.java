package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.ReaderUtil;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.ihtsdo.otf.mapping.services.RootService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * The root service for managing the entity manager factory and hibernate search
 * field names
 */
public class RootServiceJpa implements RootService {

	/** The factory. */
	protected static EntityManagerFactory factory;

	/** The indexed field names. */
	protected static Set<String> fieldNames;

	/** The lock. */
	private static String lock = "lock";

	/** The manager. */
	protected EntityManager manager;

	/** The transaction per operation. */
	protected boolean transactionPerOperation = true;

	/** The transaction entity. */
	protected EntityTransaction tx;
	
	/**  The config. */
	public Properties config = null;

	/**
	 * Instantiates an empty {@link RootServiceJpa}.
	 * 
	 * @throws Exception
	 */
	public RootServiceJpa() throws Exception {
		// created once or if the factory has closed
		synchronized (lock) {
			if (factory == null || !factory.isOpen()) {
				openFactory();
			}
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
	public void openFactory() throws Exception {

		// if factory has not been instantiated or has been closed, open it
		if (factory == null || !factory.isOpen()) {

			Logger.getLogger(this.getClass()).info(
					"Setting root service entity manager factory.");
		    Properties config = ConfigUtility.getConfigProperties();
			factory = Persistence.createEntityManagerFactory(
					"MappingServiceDS", config);
		}

		// if the field names have not been set, initialize
		if (fieldNames == null)
			initializeFieldNames();
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
	 * @see org.ihtsdo.otf.mapping.services.RootService#initializeFieldNames()
	 */
	@Override
	public void initializeFieldNames() throws Exception {

		if (fieldNames == null) {
			fieldNames = new HashSet<>();
			EntityManager manager = factory.createEntityManager();
			FullTextEntityManager fullTextEntityManager = org.hibernate.search.jpa.Search
					.getFullTextEntityManager(manager);
			IndexReaderAccessor indexReaderAccessor = fullTextEntityManager
					.getSearchFactory().getIndexReaderAccessor();
			Set<String> indexedClassNames = fullTextEntityManager
					.getSearchFactory().getStatistics().getIndexedClassNames();
			for (String indexClass : indexedClassNames) {
				IndexReader indexReader = indexReaderAccessor.open(indexClass);
				try {
					for (FieldInfo info : ReaderUtil
							.getMergedFieldInfos(indexReader)) {
						fieldNames.add(info.name);
					}
				} finally {
					indexReaderAccessor.close(indexReader);
				}
			}

			fullTextEntityManager.close();
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
	public void commit() {

		if (getTransactionPerOperation())
			throw new IllegalStateException(
					"Error attempting to commit a transaction when using transactions per operation mode.");
		else if (tx != null && !tx.isActive())
			throw new IllegalStateException(
					"Error attempting to commit a transaction when there "
							+ "is no active transaction");
		tx.commit();
	}
	
	@Override
	public void close() throws Exception {
		if (manager.isOpen()) {
			manager.close();
		}
	}
	
	
	


}
