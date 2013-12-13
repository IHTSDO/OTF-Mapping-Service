package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.ReaderUtil;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.services.EditMappingService;

/**
 * The JPA mapping service to add, edit, and remove Map objects
 *
 */
public class EditMappingServiceJpa implements EditMappingService {

	/** The factory. */
	private EntityManagerFactory factory;

	
	/** The transaction. */
	private EntityTransaction tx;
	
	/** The indexed field names. */
	private Set<String> fieldNames;

	/**
	 * Instantiates an empty {@link MappingServiceJpa}.
	 */
	public EditMappingServiceJpa() {
		factory = Persistence.createEntityManagerFactory("MappingServiceDS"); // leave this in constructor
		fieldNames = new HashSet<String>();
		
		
		// move these to the methods themselves
		// for lucene searches, need to do full reinstantiation of the indexReader
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
		manager.close();
	}
	
	/**
	 * Close the factory when done with service
	 */
	public void close() {
		try {
			factory.close();
		} catch (Exception e) {
			System.out.println("Failed to close MappingService!");
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * Add a map project
	 * @param mapProject the map project
	 */
	public void addMapProject(MapProject mapProject) {
		try {
			EntityManager manager = factory.createEntityManager();
			tx = manager.getTransaction();
			tx.begin();
			manager.persist(mapProject);
			tx.commit();
			manager.close();
		} catch (Exception e) {
			tx.rollback();
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Update a map project
	 * @param mapProject the changed map project
	 */
	public void updateMapProject(MapProject mapProject) {
		
		try {
			EntityManager manager = factory.createEntityManager();
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.merge(mapProject);
			tx.commit();
			manager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove (delete) a map project
	 * @param mapProjectId the map project to be removed
	 */
	public void removeMapProject(Long mapProjectId) {
		try {
			EntityManager manager = factory.createEntityManager();
			EntityTransaction tx = manager.getTransaction();
			MapProject mp = manager.find(MapProjectJpa.class, mapProjectId);
			tx.begin();
			manager.remove(mp);
			tx.commit();
			manager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Add a map specialist
	 * @param mapSpecialist the map specialist
	 */
	public void addMapSpecialist(MapSpecialist mapSpecialist) {
		try {
			EntityManager manager = factory.createEntityManager();
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.persist(mapSpecialist);
			tx.commit();
			manager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Update a map specialist
	 * @param mapSpecialist the changed map specialist
	 */
	public void updateMapSpecialist(MapSpecialist mapSpecialist) {
		
		// TODO: Make sure merge will work
		try {
			EntityManager manager = factory.createEntityManager();
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.merge(mapSpecialist);
			tx.commit();
			manager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove (delete) a map specialist
	 * @param mapSpecialistId the map specialist to be removed
	 */
	public void removeMapSpecialist(Long mapSpecialistId) {
		try {
			EntityManager manager = factory.createEntityManager();
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			MapSpecialist mp = manager.find(MapSpecialistJpa.class, mapSpecialistId);
			manager.remove(mp);
			tx.commit();
			manager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Add a map lead
	 * @param mapLead the map lead
	 */
	public void addMapLead(MapLead mapLead) {
		try {
			EntityManager manager = factory.createEntityManager();
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.persist(mapLead);
			tx.commit();
			manager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Update a map lead
	 * @param mapLead the changed map lead
	 */
	public void updateMapLead(MapLead mapLead) {
		
		try {
			EntityManager manager = factory.createEntityManager();
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.merge(mapLead);
			tx.commit();
			manager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove (delete) a map lead
	 * @param mapLeadId the map lead to be removed
	 */
	public void removeMapLead(Long mapLeadId) {
		
		try {
			EntityManager manager = factory.createEntityManager();
			EntityTransaction tx = manager.getTransaction();
			MapLead mp = manager.find(MapLeadJpa.class, mapLeadId);
			tx.begin();
			manager.remove(mp);
			tx.commit();
			manager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
