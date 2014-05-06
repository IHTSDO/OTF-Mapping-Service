package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.ReaderUtil;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.ihtsdo.otf.mapping.services.RootService;

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

  /**
   * Instantiates an empty {@link RootServiceJpa}.
   */
  public RootServiceJpa() {
    // created once or if the factory has closed
    synchronized (lock) {
      if (factory == null || !factory.isOpen()) {
        openFactory();
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.ihtsdo.otf.mapping.services.RootService#openFactory()
   */
  @Override
  public void openFactory() {

    // if factory has not been instantiated or has been closed, open it
    if (factory == null || !factory.isOpen()) {

      Logger.getLogger(this.getClass()).info(
          "Setting root service entity manager factory.");
      factory = Persistence.createEntityManagerFactory("MappingServiceDS");
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
  public void closeFactory() {
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
  public void initializeFieldNames() {

    if (fieldNames == null) {
      fieldNames = new HashSet<>();
      EntityManager manager = factory.createEntityManager();
      FullTextEntityManager fullTextEntityManager =
          org.hibernate.search.jpa.Search.getFullTextEntityManager(manager);
      IndexReaderAccessor indexReaderAccessor =
          fullTextEntityManager.getSearchFactory().getIndexReaderAccessor();
      Set<String> indexedClassNames =
          fullTextEntityManager.getSearchFactory().getStatistics()
              .getIndexedClassNames();
      for (String indexClass : indexedClassNames) {
        IndexReader indexReader = indexReaderAccessor.open(indexClass);
        try {
          for (FieldInfo info : ReaderUtil.getMergedFieldInfos(indexReader)) {
            fieldNames.add(info.name);
          }
        } finally {
          indexReaderAccessor.close(indexReader);
        }
      }

      fullTextEntityManager.close();
    }
  }

}
