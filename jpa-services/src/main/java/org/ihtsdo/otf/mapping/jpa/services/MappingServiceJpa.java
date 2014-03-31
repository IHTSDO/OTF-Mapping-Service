package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.Version;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;

// TODO: Auto-generated Javadoc
/**
 * JPA implementation of the {@link MappingService}.
 *
 * @author ${author}
 */
public class MappingServiceJpa implements MappingService {

	/** The factory. */
	private static EntityManagerFactory factory;

	/** The manager. */
	private EntityManager manager;

	/** The full text entity manager. */
	private FullTextEntityManager fullTextEntityManager;

	/** The indexed field names. */
	private static Set<String> fieldNames;

	/** The transaction per operation. */
	private boolean transactionPerOperation = true;

	/** The transaction entity. */
	private EntityTransaction tx;

	/**
	 * Instantiates an empty {@link MappingServiceJpa}.
	 */
	public MappingServiceJpa() {

		// created once or if the factory has been closed
		if (factory == null || !factory.isOpen()) {
			factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		}

		// created on each instantiation
		manager = factory.createEntityManager();

		// fieldNames created once
		if (fieldNames == null) {
			fieldNames = new HashSet<>();

			fullTextEntityManager =
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

			if (fullTextEntityManager != null) {
				fullTextEntityManager.close();
			}

			// closing fullTextEntityManager also closes manager, recreate
			manager = factory.createEntityManager();
		}
	}

	/**
	 * Close the manager when done with this service.
	 * 
	 * @throws Exception the exception
	 */
	@Override
	public void close() throws Exception {
		if (manager.isOpen()) {
			manager.close();
		}
	}

	// //////////////////////////////////
	// MapProject
	// - getMapProjects
	// - getMapProject(Long id)
	// - getMapProject(String name)
	// - findMapProjects(String query)
	// - addMapProject(MapProject mapProject)
	// - updateMapProject(MapProject mapProject)
	// - removeMapProject(MapProject mapProject)
	// //////////////////////////////////

	/**
	 * Return map project for auto-generated id.
	 * 
	 * @param id the auto-generated id
	 * @return the MapProject
	 */
	@Override
	public MapProject getMapProject(Long id) {

		MapProject m = null;

		javax.persistence.Query query =
				manager.createQuery("select m from MapProjectJpa m where id = :id");
		query.setParameter("id", id);

		try {
			System.out.println("GetMapProject(Long id)");
			m = (MapProject) query.getSingleResult();
			m.getScopeConcepts().size();
			m.getScopeExcludedConcepts().size();
			return m;
			
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map project query for id = " + id + " returned no results!");
			return null;
		}
		

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapProjectByName(java.lang.String)
	 */
	@Override
	public MapProject getMapProjectByName(String name) {

		MapProject m = null;

		javax.persistence.Query query =
				manager.createQuery("select m from MapProjectJpa m where name = :name")
				.setParameter("name", name);

		try {
			m = (MapProject) query.getSingleResult();
			m.getScopeConcepts().size();
			m.getScopeExcludedConcepts().size();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map project query for name = " + name + " returned no results!");
			return null;
		}
		return m;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapProjectByRefSetId(java.lang.String)
	 */
	@Override
	public MapProject getMapProjectByRefSetId(String refSetId) {

		MapProject m = null;

		javax.persistence.Query query =
				manager.createQuery(
						"select m from MapProjectJpa m where refSetId = :refSetId")
						.setParameter("refSetId", refSetId);

		try {
			m = (MapProject) query.getSingleResult();
			m.getScopeConcepts().size();
			m.getScopeExcludedConcepts().size();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map project query for refSetId = " + refSetId
					+ " returned no results!");
			return null;
		}
		
		
		return m;
	}

	/**
	 * Retrieve all map projects.
	 * 
	 * @return a List of MapProjects
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<MapProject> getMapProjects() {

		List<MapProject> m = null;

		// construct query
		javax.persistence.Query query =
				manager.createQuery("select m from MapProjectJpa m");

		m = query.getResultList();
		
		for (MapProject project : m) {
			project.getScopeConcepts().size();
			project.getScopeExcludedConcepts().size();
		}

		return m;
	}

	/**
	 * Query for MapProjects.
	 * 
	 * @param query the query
	 * @param pfsParameter the pfs parameter
	 * @return the list of MapProject
	 * @throws Exception the exception
	 */
	@Override
	@SuppressWarnings("unchecked")
	public SearchResultList findMapProjects(String query,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList s = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager =
				Search.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct luceneQuery based on URL format
		if (query.indexOf(':') == -1) { // no fields indicated
			MultiFieldQueryParser queryParser =
					new MultiFieldQueryParser(Version.LUCENE_36,
							fieldNames.toArray(new String[0]),
							searchFactory.getAnalyzer(MapProjectJpa.class));
			queryParser.setAllowLeadingWildcard(false);
			luceneQuery = queryParser.parse(query);

		} else { // field:value
			QueryParser queryParser =
					new QueryParser(Version.LUCENE_36, "summary",
							searchFactory.getAnalyzer(MapProjectJpa.class));
			luceneQuery = queryParser.parse(query);
		}

		List<MapProject> m =
				fullTextEntityManager.createFullTextQuery(luceneQuery,
						MapProjectJpa.class).getResultList();

		Logger.getLogger(this.getClass()).debug(
				Integer.toString(m.size()) + " map projects retrieved");

		for (MapProject mp : m) {
			s.addSearchResult(new SearchResultJpa(mp.getId(), mp.getRefSetId()
					.toString(), mp.getName()));
		}

		s.sortSearchResultsById();

		fullTextEntityManager.close();

		return s;
	}

	/**
	 * Add a map project.
	 * 
	 * @param mapProject the map project
	 * @return the map project
	 */
	@Override
	public MapProject addMapProject(MapProject mapProject) {

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.persist(mapProject);
			tx.commit();

			return mapProject;
		} else {
			if (!tx.isActive()) {
				throw new IllegalStateException(
						"Error attempting to change data without an active transaction");
			}
			manager.persist(mapProject);
			return mapProject;
		}

	}

	/**
	 * Update a map project.
	 * 
	 * @param mapProject the changed map project
	 */
	@Override
	public void updateMapProject(MapProject mapProject) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.merge(mapProject);
			tx.commit();
		} else {
			manager.merge(mapProject);
		}

	}

	/**
	 * Remove (delete) a map project.
	 * 
	 * @param mapProjectId the map project to be removed
	 */
	@Override
	public void removeMapProject(Long mapProjectId) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			// first, remove the leads and specialists from this project
			tx.begin();
			MapProject mp = manager.find(MapProjectJpa.class, mapProjectId);
			mp.setMapLeads(null);
			mp.setMapSpecialists(null);
			tx.commit();

			// now remove the entry
			tx.begin();
			if (manager.contains(mp)) {
				manager.remove(mp);
			} else {
				manager.remove(manager.merge(mp));
			}
			tx.commit();
		} else {
			MapProject mp = manager.find(MapProjectJpa.class, mapProjectId);
			mp.setMapLeads(null);
			mp.setMapSpecialists(null);
			if (manager.contains(mp)) {
				manager.remove(mp);
			} else {
				manager.remove(manager.merge(mp));
			}
		}

	}

	// ///////////////////////////////////////////////////////////////
	// MapUser
	// - getMapUsers()
	// - getMapProjectsForUser(MapUser mapUser)
	// - findMapUsers(String query)
	// - addMapUser(MapUser mapUser)
	// - updateMapUser(MapUser mapUser)
	// - removeMapUser(Long id)
	// ///////////////////////////////////////////////////////////////

	/**
	 * Retrieve all map users.
	 *
	 * @return a List of MapUsers
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<MapUser> getMapUsers() {

		List<MapUser> m = null;

		javax.persistence.Query query =
				manager.createQuery("select m from MapUserJpa m");

		m = query.getResultList();

		return m;
	}

	/**
	 * Return map specialist for auto-generated id.
	 * 
	 * @param id the auto-generated id
	 * @return the MapSpecialist
	 */
	@Override
	public MapUser getMapUser(Long id) {

		MapUser m = null;

		javax.persistence.Query query =
				manager.createQuery("select m from MapUserJpa m where id = :id");
		query.setParameter("id", id);
		try {
			m = (MapUser) query.getSingleResult();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map specialist query for id = " + id + " returned no results!");
			return null;
		}

		return m;

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapUser(java.lang.String)
	 */
	@Override
	public MapUser getMapUser(String userName) {

		MapUser m = null;

		javax.persistence.Query query =
				manager
				.createQuery("select m from MapUserJpa m where userName = :userName");
		query.setParameter("userName", userName);
		try {
			m = (MapUser) query.getSingleResult();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map specialist query for userName = " + userName
					+ " returned no results!");
			return null;
		}

		return m;
	}

	/**
	 * Retrieve all map projects assigned to a particular map specialist.
	 *
	 * @param mapUser the map user
	 * @return a List of MapProjects
	 */
	@Override
	public List<MapProject> getMapProjectsForMapUser(MapUser mapUser) {

		List<MapProject> mp_list = getMapProjects();
		List<MapProject> mp_list_return = new ArrayList<>();

		// iterate and check for presence of mapUser as specialist
		for (MapProject mp : mp_list) {

			Set<MapUser> ms_set = mp.getMapSpecialists();

			for (MapUser ms : ms_set) {
				if (ms.equals(mapUser)) {
					mp.getScopeConcepts().size();
					mp.getScopeExcludedConcepts().size();
					mp_list_return.add(mp);
				}
			}
		}

		return mp_list_return;
	}

	/**
	 * Query for MapSpecialists.
	 * 
	 * @param query the query
	 * @param pfsParameter the pfs parameter
	 * @return the List of MapProjects
	 * @throws Exception the exception
	 */
	@Override
	@SuppressWarnings("unchecked")
	public SearchResultList findMapUsers(String query, PfsParameter pfsParameter)
			throws Exception {

		SearchResultList s = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager =
				Search.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct luceneQuery based on URL format
		if (query.indexOf(':') == -1) { // no fields indicated
			MultiFieldQueryParser queryParser =
					new MultiFieldQueryParser(Version.LUCENE_36,
							fieldNames.toArray(new String[0]),
							searchFactory.getAnalyzer(MapUserJpa.class));
			queryParser.setAllowLeadingWildcard(false);
			luceneQuery = queryParser.parse(query);

		} else { // field:value
			QueryParser queryParser =
					new QueryParser(Version.LUCENE_36, "summary",
							searchFactory.getAnalyzer(MapUserJpa.class));
			luceneQuery = queryParser.parse(query);
		}

		List<MapUser> m =
				fullTextEntityManager
				.createFullTextQuery(luceneQuery, MapUserJpa.class).getResultList();

		for (MapUser ms : m) {
			s.addSearchResult(new SearchResultJpa(ms.getId(), "", ms.getName()));
		}

		s.sortSearchResultsById();

		fullTextEntityManager.close();

		return s;
	}

	/**
	 * Update a map specialist.
	 * 
	 * @param mapUser the changed map user
	 */
	@Override
	public void updateMapUser(MapUser mapUser) {

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.merge(mapUser);
			tx.commit();
		} else {
			manager.merge(mapUser);
		}

	}

	/**
	 * Remove (delete) a map specialist.
	 * 
	 * @param mapUserId the map user to be removed
	 */
	@Override
	public void removeMapUser(Long mapUserId) {

		EntityTransaction tx = manager.getTransaction();

		// retrieve this map specialist
		MapUser mu = manager.find(MapUserJpa.class, mapUserId);

		// retrieve all projects on which this specialist appears
		List<MapProject> projects = getMapProjectsForMapUser(mu);

		if (getTransactionPerOperation()) {
			// remove specialist from all these projects
			tx.begin();
			for (MapProject mp : projects) {
				mp.removeMapLead(mu);
				mp.removeMapSpecialist(mu);
				manager.merge(mp);
			}
			tx.commit();

			// remove specialist
			tx.begin();
			if (manager.contains(mu)) {
				manager.remove(mu);
			} else {
				manager.remove(manager.merge(mu));
			}
			tx.commit();

		} else {
			for (MapProject mp : projects) {
				mp.removeMapLead(mu);
				mp.removeMapSpecialist(mu);
				manager.merge(mp);
			}
			if (manager.contains(mu)) {
				manager.remove(mu);
			} else {
				manager.remove(manager.merge(mu));
			}
		}

	}

	/**
	 * Add a map lead.
	 *
	 * @param mapUser the map lead
	 * @return the map user
	 */
	@Override
	public MapUser addMapUser(MapUser mapUser) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.persist(mapUser);
			tx.commit();
		} else {
			manager.persist(mapUser);
		}

		return mapUser;
	}

	// //////////////////////////////////
	// MapRecord
	// //////////////////////////////////

	/**
	 * Retrieve all map records.
	 * 
	 * @return a List of MapRecords
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<MapRecord> getMapRecords() {

		List<MapRecord> m = null;

		// construct query
		javax.persistence.Query query =
				manager.createQuery("select m from MapRecordJpa m");

		// Try query
		m = query.getResultList();

		return m;
	}

	/**
	 * Retrieve map record for given id.
	 * 
	 * @param id the map record id
	 * @return the map record
	 */
	@Override
	public MapRecord getMapRecord(Long id) {

		javax.persistence.Query query =
				manager.createQuery("select r from MapRecordJpa r where id = :id");

		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */

		try {

			query.setParameter("id", id);

			MapRecord r = (MapRecord) query.getSingleResult();

			Logger.getLogger(this.getClass()).debug(
					"Returning record_id... "
							+ ((r != null) ? r.getId().toString() : "null"));

			
			return r;

		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).debug(
					"MapRecord query for id = " + id + " returned no results!");
			return null;
		} catch (NonUniqueResultException e) {
			Logger.getLogger(this.getClass()).debug(
					"MapRecord query for id = " + id + " returned multiple results!");
			return null;
		}
	}

	/*
	 * public List<MapRecord> getMapRecords(mapProjectId, sortingInfo, pageSize,
	 * page#) { ///project/id/12345/records?sort=sortkey&pageSize=100&page=1
	 * return null; }
	 */

	/**
	 * Retrieve map records for a lucene query.
	 * 
	 * @param query the lucene query string
	 * @param pfsParameter the pfs parameter
	 * @return a list of map records
	 * @throws Exception the exception
	 */
	@Override
	@SuppressWarnings("unchecked")
	public SearchResultList findMapRecords(String query, PfsParameter pfsParameter)
			throws Exception {

		SearchResultList s = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager =
				Search.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct luceneQuery based on URL format
		if (query.indexOf(':') == -1) { // no fields indicated
			MultiFieldQueryParser queryParser =
					new MultiFieldQueryParser(Version.LUCENE_36,
							fieldNames.toArray(new String[0]),
							searchFactory.getAnalyzer(MapRecordJpa.class));
			queryParser.setAllowLeadingWildcard(false);
			luceneQuery = queryParser.parse(query);

		} else { // field:value
			QueryParser queryParser =
					new QueryParser(Version.LUCENE_36, "summary",
							searchFactory.getAnalyzer(MapRecordJpa.class));
			luceneQuery = queryParser.parse(query);
		}

		List<MapRecord> m =
				fullTextEntityManager.createFullTextQuery(luceneQuery,
						MapRecordJpa.class).getResultList();

		Logger.getLogger(this.getClass()).debug(
				Integer.toString(m.size()) + " map records retrieved");

		for (MapRecord mp : m) {
			s.addSearchResult(new SearchResultJpa(mp.getId(), mp.getConceptId()
					.toString(), mp.getConceptName()));
		}

		s.sortSearchResultsById();

		fullTextEntityManager.close();

		return s;
		/*
		 * for (MapRecord mr : m) { if (pfsParameter == null ||
		 * pfsParameter.isIndexInRange(i++)) { s.addSearchResult(new
		 * SearchResultJpa(mr.getId(), "", mr.getConceptId())); } }
		 */
	}

	/**
	 * Add a map record.
	 *
	 * @param mapRecord the map record to be added
	 * @return the map record
	 * @throws Exception the exception
	 */
	@Override
	public MapRecord addMapRecord(MapRecord mapRecord) throws Exception {

		// check if user valid
		if (mapRecord.getOwner() == null) { 
			throw new Exception("Map Record requires valid user in owner field");
		}

		// check if last modified by user valid
		if (mapRecord.getLastModifiedBy() == null) {
			throw new Exception("Map Record requires valid user in lastModifiedBy field");
		}

		// set the map record of all elements of this record
		mapRecord.assignToChildren();

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.persist(mapRecord);
			tx.commit();
			return mapRecord;
		} else {
			manager.persist(mapRecord);
			return mapRecord;
		}

	}

	/**
	 * Update a map record.
	 * 
	 * @param mapRecord the map record to be updated
	 * @return the map record
	 */
	@Override
	public MapRecord updateMapRecord(MapRecord mapRecord) {

		// update timestamp
		mapRecord.setLastModified((new java.util.Date()).getTime());

		// first assign the map record to its children
		mapRecord.assignToChildren();

		if (getTransactionPerOperation()) {

			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.merge(mapRecord);
			tx.commit();
			return mapRecord;
		} else {
			manager.merge(mapRecord);
			return mapRecord;
		}

	}

	/**
	 * Remove (delete) a map record by id.
	 * 
	 * @param id the id of the map record to be removed
	 */
	@Override
	public void removeMapRecord(Long id) {

		EntityTransaction tx = manager.getTransaction();

		// find the map record
		MapRecord m = manager.find(MapRecordJpa.class, id);

		if (getTransactionPerOperation()) {
			// delete the map record
			tx.begin();
			if (manager.contains(m)) {
				manager.remove(m);
			} else {
				manager.remove(manager.merge(m));
			}
			tx.commit();

		} else {
			if (manager.contains(m)) {
				manager.remove(m);
			} else {
				manager.remove(manager.merge(m));
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MappingService#computeMapAdviceAndMapRelationsForMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@SuppressWarnings({
		"unused", "unchecked", "rawtypes"
	})
	
	
	/**
	 * Takes a map entry and computes any auto-generated advice for its entries
	 */
	@Override
	public MapRelation computeMapRelation(MapEntry mapEntry) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
	
		MapRecord mapRecord = getMapRecord(mapEntry.getMapRecord().getId());
		MapProject mapProject = getMapProject(mapRecord.getMapProjectId());
		ProjectSpecificAlgorithmHandler algorithmHandler = 
				getProjectSpecificAlgorithmHandler(mapProject);

		return algorithmHandler.computeMapRelation(mapRecord, mapEntry);
	}
	
	/**
	 * Takes a map record and computes any auto-generated advice for its entries
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public List<MapAdvice> computeMapAdvice(MapEntry mapEntry) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

		MapRecord mapRecord = mapEntry.getMapRecord();
		
		ProjectSpecificAlgorithmHandler algorithmHandler = 
				getProjectSpecificAlgorithmHandler(getMapProject(mapRecord.getMapProjectId()));

		return algorithmHandler.computeMapAdvice(mapRecord, mapEntry);
		}



	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapRecordRevisions(java.lang.Long)
	 */
	@Override
	public List<MapRecord> getMapRecordRevisions(Long mapRecordId) {

		AuditReader reader = AuditReaderFactory.get(manager);
		List<MapRecord> revisions = new ArrayList<>();
		List query =
				reader.createQuery()
				.forRevisionsOfEntity(MapRecordJpa.class, false, false)
				.getResultList();

		System.out.println(query.toString());
		return query;
	}

  /* (non-Javadoc)
   * @see org.ihtsdo.otf.mapping.services.MappingService#getRecentlyEditedMapRecords(org.ihtsdo.otf.mapping.model.MapUser)
   */
  @Override
  // TODO remove NEW records
  // TODO confirm return sorted by lastModifiedBy most recent at head
  public List<MapRecord> getRecentlyEditedMapRecords(Long projectId, String userName)  throws Exception {
  	
  	MapUser user = getMapUser(userName);
  	
  	Map<String, MapRecord> editedRecords = new HashMap<>();
  	
  	AuditReader reader = AuditReaderFactory.get(manager);
  	
  	AuditQuery query =
  			reader
        .createQuery()
        .forRevisionsOfEntity(MapRecordJpa.class, false, true)
        .add(AuditEntity.relatedId("owner").eq(user.getId()))
        .addOrder(AuditEntity.property("lastModified").desc());

    List<Object[]> allRevisions = (List<Object[]>) query.getResultList();
    for (Object[] revision : allRevisions) {
    	MapRecord record = (MapRecord)revision[0];
    	// used to force reading the graph
    	record.getLastModifiedBy().getEmail();
    	// only save the most recent revision
    	if (record.getMapProjectId().equals(projectId) &&
    			
    			!editedRecords.keySet().contains(record.getConceptId()) )
    			
    	  editedRecords.put(record.getConceptId(), record);
    }

  	return new ArrayList<MapRecord>(editedRecords.values());
  }
  
	// //////////////////////////////////
	// Other query services
	// //////////////////////////////////

	/**
	 * Service for finding MapEntrys by string query.
	 * 
	 * @param query the query string
	 * @param pfsParameter the pfs parameter
	 * @return the search result list
	 * @throws Exception the exception
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findMapEntrys(String query, PfsParameter pfsParameter)
			throws Exception {
		SearchResultList s = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager =
				Search.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct luceneQuery based on URL format
		if (query.indexOf(':') == -1) { // no fields indicated
			MultiFieldQueryParser queryParser =
					new MultiFieldQueryParser(Version.LUCENE_36,
							fieldNames.toArray(new String[0]),
							searchFactory.getAnalyzer(MapEntryJpa.class));
			queryParser.setAllowLeadingWildcard(false);
			luceneQuery = queryParser.parse(query);

		} else { // field:value
			QueryParser queryParser =
					new QueryParser(Version.LUCENE_36, "summary",
							searchFactory.getAnalyzer(MapEntryJpa.class));
			luceneQuery = queryParser.parse(query);
		}

		List<MapEntry> m =
				fullTextEntityManager.createFullTextQuery(luceneQuery,
						MapEntryJpa.class).getResultList();

		for (MapEntry me : m) {
			s.addSearchResult(new SearchResultJpa(me.getId(), "", me.getMapRecord()
					.getId().toString()));
		}

		s.sortSearchResultsById();

		return s;
	}

	/**
	 * Service for finding MapAdvices by string query.
	 * 
	 * @param query the query string
	 * @param pfsParameter the pfs parameter
	 * @return the search result list
	 * @throws Exception the exception
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findMapAdvices(String query, PfsParameter pfsParameter)
			throws Exception {
		SearchResultList s = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager =
				Search.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct luceneQuery based on URL format
		if (query.indexOf(':') == -1) { // no fields indicated
			MultiFieldQueryParser queryParser =
					new MultiFieldQueryParser(Version.LUCENE_36,
							fieldNames.toArray(new String[0]),
							searchFactory.getAnalyzer(MapAdviceJpa.class));
			queryParser.setAllowLeadingWildcard(false);
			luceneQuery = queryParser.parse(query);

		} else { // field:value
			QueryParser queryParser =
					new QueryParser(Version.LUCENE_36, "summary",
							searchFactory.getAnalyzer(MapAdviceJpa.class));
			luceneQuery = queryParser.parse(query);
		}

		List<MapAdvice> m =
				fullTextEntityManager
				.createFullTextQuery(luceneQuery, MapNoteJpa.class).getResultList();

		for (MapAdvice ma : m) {
			s.addSearchResult(new SearchResultJpa(ma.getId(), "", ma.getName()));
		}

		s.sortSearchResultsById();

		return s;
	}

	/**
	 * Service for finding MapRelations by string query.
	 * 
	 * @param query the query string
	 * @param pfsParameter the pfs parameter
	 * @return the search result list
	 * @throws Exception the exception
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SearchResultList findMapRelations(String query,
			PfsParameter pfsParameter) throws Exception {
		SearchResultList s = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager =
				Search.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct luceneQuery based on URL format
		if (query.indexOf(':') == -1) { // no fields indicated
			MultiFieldQueryParser queryParser =
					new MultiFieldQueryParser(Version.LUCENE_36,
							fieldNames.toArray(new String[0]),
							searchFactory.getAnalyzer(MapRelationJpa.class));
			queryParser.setAllowLeadingWildcard(false);
			luceneQuery = queryParser.parse(query);

		} else { // field:value
			QueryParser queryParser =
					new QueryParser(Version.LUCENE_36, "summary",
							searchFactory.getAnalyzer(MapRelationJpa.class));
			luceneQuery = queryParser.parse(query);
		}

		List<MapRelation> m =
				fullTextEntityManager
				.createFullTextQuery(luceneQuery, MapNoteJpa.class).getResultList();

		for (MapRelation ma : m) {
			s.addSearchResult(new SearchResultJpa(ma.getId(), "", ma.getName()));
		}

		s.sortSearchResultsById();

		return s;
	}

	// //////////////////////////////////////////////
	// Descendant services
	// //////////////////////////////////////////////

	/**
	 * Retrieve map records for a given terminology id.
	 * 
	 * @param terminologyId the concept id
	 * @return the list of map records
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<MapRecord> getMapRecordsForConcept(String terminologyId) {
		List<MapRecord> m = null;

		// construct query
		javax.persistence.Query query =
				manager
				.createQuery("select m from MapRecordJpa m where conceptId = :conceptId");

		// Try query
		query.setParameter("conceptId", terminologyId);
		m = query.getResultList();

		return m;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapRecordsForConcept(java.lang.Long)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<MapRecord> getMapRecordsForConcept(Long conceptId) {

		// call retrieval function with concept
		Concept concept = manager.find(ConceptJpa.class, conceptId);
		List<MapRecord> results = null;
		// find maprecords where:
		// (1) the conceptId matches the concept terminologyId
		// (2) the concept terminology matches the source terminology for the
		// mapRecord's project
		// Try query
		try {
			javax.persistence.Query query =
					manager.createQuery(
							"select mr from MapRecordJpa mr "
									+ "where conceptId = :conceptId ").setParameter("conceptId",
											concept.getTerminologyId());
			results = query.getResultList();
		} catch (Exception e) {
			Logger.getLogger(this.getClass()).warn(
					"Map records for concept " + concept.getTerminologyId()
					+ " returned no map record results!");
			return null;
		}
		// return results
		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getMapRecordCountForMapProjectId
	 * (java.lang.Long)
	 */
	@Override
	public Long getMapRecordCountForMapProject(Long mapProjectId,
			PfsParameter pfsParameter) throws Exception {

		// if no paging/filtering/sorting object, retrieve total number of records
		if (pfsParameter == null
				|| (pfsParameter.getFilterString() == null || pfsParameter
				.getFilterString().equals(""))) {

			javax.persistence.Query query =
					manager
					.createQuery("select count(m) from MapRecordJpa m where mapProjectId = :mapProjectId");

			query.setParameter("mapProjectId", mapProjectId);
			return new Long(query.getSingleResult().toString());

			// otherwise require a lucene search based on filters
		} else {
			String full_query =
					constructMapRecordForMapProjectIdQuery(mapProjectId, pfsParameter);
			FullTextEntityManager fullTextEntityManager =
					Search.getFullTextEntityManager(manager);

			SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
			Query luceneQuery;

			// construct luceneQuery based on URL format

			QueryParser queryParser =
					new QueryParser(Version.LUCENE_36, "summary",
							searchFactory.getAnalyzer(MapRecordJpa.class));
			luceneQuery = queryParser.parse(full_query);

			return new Long(fullTextEntityManager.createFullTextQuery(luceneQuery,
					MapRecordJpa.class).getResultSize());

		}

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapRecordsForMapProject(java.lang.Long)
	 */
	@Override
	public List<MapRecord> getMapRecordsForMapProject(Long mapProjectId)
			throws Exception {

		return getMapRecordsForMapProject(mapProjectId, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getMapRecordsForMapProject
	 * (java.lang.Long, org.ihtsdo.otf.mapping.helpers.PfsParameter)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<MapRecord> getMapRecordsForMapProject(Long mapProjectId,
			PfsParameter pfsParameter) throws Exception {

		String full_query =
				constructMapRecordForMapProjectIdQuery(mapProjectId,
						pfsParameter == null ? new PfsParameterJpa() : pfsParameter);

		FullTextEntityManager fullTextEntityManager =
				Search.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct luceneQuery based on URL format

		QueryParser queryParser =
				new QueryParser(Version.LUCENE_36, "summary",
						searchFactory.getAnalyzer(MapRecordJpa.class));
		luceneQuery = queryParser.parse(full_query);

		org.hibernate.search.jpa.FullTextQuery ftquery =
				fullTextEntityManager.createFullTextQuery(luceneQuery,
						MapRecordJpa.class);

		if (pfsParameter != null) {
			if (pfsParameter.getStartIndex() != -1
					&& pfsParameter.getMaxResults() != -1) {
				ftquery.setFirstResult(pfsParameter.getStartIndex());
				ftquery.setMaxResults(pfsParameter.getMaxResults());
			}
			if (pfsParameter.getSortField() != null) {
				ftquery.setSort(new Sort(new SortField(pfsParameter.getSortField(),
						SortField.STRING)));
			}
		}

		List<MapRecord> m = ftquery.getResultList();

		Logger.getLogger(this.getClass()).debug(
				Integer.toString(m.size()) + " records retrieved");

		return m;

	}

	/**
	 * Helper function for map record query construction using both fielded terms
	 * and unfielded terms.
	 * 
	 * @param mapProjectId the map project id for which queries are retrieved
	 * @param pfsParameter the pfs parameter
	 * @return the full lucene query text
	 */
	private static String constructMapRecordForMapProjectIdQuery(
			Long mapProjectId, PfsParameter pfsParameter) {

		String full_query;

		// if no filter supplied, return query based on map project id only
		if (pfsParameter.getFilterString() == null
				|| pfsParameter.getFilterString().equals("")) {
			full_query = "mapProjectId:" + mapProjectId;
			return full_query;
		}

		// Pre-treatment:  Find any lower-case boolean operators and set to uppercase

		// //////////////////
		// Basic algorithm:
		//
		// 1) add whitespace breaks to operators
		// 2) split query on whitespace
		// 3) cycle over terms in split query to find quoted material, add each
		// term/quoted term to parsed terms\
		// a) special case: quoted term after a :
		// 3) cycle over terms in parsed terms
		// a) if an operator/parantheses, pass through unchanged (send to upper case
		// for boolean)
		// b) if a fielded query (i.e. field:value), pass through unchanged
		// c) if not, construct query on all fields with this term

		// list of escape terms (i.e. quotes, operators) to be fed into query
		// untouched
		String escapeTerms = "\\+|\\-|\"|\\(|\\)";
		String booleanTerms = "and|AND|or|OR|not|NOT";

		// first cycle over the string to add artificial breaks before and after
		// control characters
		final String queryStr =
				(pfsParameter == null ? "" : pfsParameter.getFilterString());

		String queryStr_mod = queryStr;
		queryStr_mod = queryStr_mod.replace("(", " ( ");
		queryStr_mod = queryStr_mod.replace(")", " ) ");
		queryStr_mod = queryStr_mod.replace("\"", " \" ");
		queryStr_mod = queryStr_mod.replace("+", " + ");
		queryStr_mod = queryStr_mod.replace("-", " - ");

		// remove any leading or trailing whitespace (otherwise first/last null term
		// bug)
		queryStr_mod = queryStr_mod.trim();

		// split the string by white space and single-character operators
		String[] terms = queryStr_mod.split("\\s+");

		// merge items between quotation marks
		boolean exprInQuotes = false;
		List<String> parsedTerms = new ArrayList<>();
		// List<String> parsedTerms_temp = new ArrayList<String>();
		String currentTerm = "";

		// cycle over terms to identify quoted (i.e. non-parsed) terms
		for (int i = 0; i < terms.length; i++) {

			// if an open quote is detected
			if (terms[i].equals("\"")) {

				if (exprInQuotes == true) {

					// special case check: fielded term. Impossible for first term to be
					// fielded.
					if (parsedTerms.size() == 0) {
						parsedTerms.add("\"" + currentTerm + "\"");
					} else {
						String lastParsedTerm = parsedTerms.get(parsedTerms.size() - 1);

						// if last parsed term ended with a colon, append this term to the
						// last parsed term
						if (lastParsedTerm.endsWith(":") == true) {
							parsedTerms.set(parsedTerms.size() - 1, lastParsedTerm + "\""
									+ currentTerm + "\"");
						} else {
							parsedTerms.add("\"" + currentTerm + "\"");
						}
					}

					// reset current term
					currentTerm = "";
					exprInQuotes = false;

				} else {
					exprInQuotes = true;
				}

				// if no quote detected
			} else {

				// if inside quotes, continue building term
				if (exprInQuotes == true) {
					currentTerm =
							currentTerm == "" ? terms[i] : currentTerm + " " + terms[i];

							// otherwise, add to parsed list
				} else {
					parsedTerms.add(terms[i]);
				}
			}
		}

		for (String s : parsedTerms) {
			Logger.getLogger(MappingServiceJpa.class).debug("  " + s);
		}

		// cycle over terms to construct query
		full_query = "";

		for (int i = 0; i < parsedTerms.size(); i++) {

			// if not the first term AND the last term was not an escape term
			// add whitespace separator
			if (i != 0 && !parsedTerms.get(i - 1).matches(escapeTerms)) {

				full_query += " ";
			}
			/*
			 * full_query += (i == 0 ? // check for first term "" : // -> if first
			 * character, add nothing parsedTerms.get(i-1).matches(escapeTerms) ? //
			 * check if last term was an escape character "": // -> if last term was
			 * an escape character, add nothing " "); // -> otherwise, add a
			 * separating space
			 */

			// if an escape character/sequence, add this term unmodified
			if (parsedTerms.get(i).matches(escapeTerms)) {

				full_query += parsedTerms.get(i);

				// else if a boolean character, add this term in upper-case form (i.e.
				// lucene format)
			} else if (parsedTerms.get(i).matches(booleanTerms)) {

				full_query += parsedTerms.get(i).toUpperCase();

				// else if already a field-specific query term, add this term unmodified
			} else if (parsedTerms.get(i).contains(":")) {

				full_query += parsedTerms.get(i);

				// otherwise, treat as unfielded query term
			} else {

				// open parenthetical term
				full_query += "(";

				// add fielded query for each indexed term, separated by OR
				Iterator<String> names_iter = fieldNames.iterator();
				while (names_iter.hasNext()) {
					full_query += names_iter.next() + ":" + parsedTerms.get(i);
					if (names_iter.hasNext())
						full_query += " OR ";
				}

				// close parenthetical term
				full_query += ")";
			}

			// if further terms remain in the sequence
			if (!(i == parsedTerms.size() - 1)) {

				// Add a separating OR iff:
				// - this term is not an escape character
				// - this term is not a boolean term
				// - next term is not a boolean term
				if (!parsedTerms.get(i).matches(escapeTerms)
						&& !parsedTerms.get(i).matches(booleanTerms)
						&& !parsedTerms.get(i + 1).matches(booleanTerms)) {

					full_query += " OR";
				}
			}
		}

		// add parantheses and map project constraint
		full_query = "(" + full_query + ")" + " AND mapProjectId:" + mapProjectId;

		Logger.getLogger(MappingServiceJpa.class)
		.debug("Full query: " + full_query);

		return full_query;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#findConceptsInScope(org.
	 * ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public SearchResultList findConceptsInScope(Long mapProjectId)
			throws Exception {
		MapProject project = getMapProject(mapProjectId);
		SearchResultList conceptsInScope = new SearchResultListJpa();

		ContentService contentService = new ContentServiceJpa();

		String terminology = project.getSourceTerminology();
		String terminologyVersion = project.getSourceTerminologyVersion();

		for (String conceptId : project.getScopeConcepts()) {
			Concept c =
					contentService.getConcept(conceptId, terminology, terminologyVersion);
			SearchResult sr = new SearchResultJpa();
			sr.setId(c.getId());
			sr.setTerminologyId(c.getTerminologyId());
			sr.setTerminology(c.getTerminology());
			sr.setTerminologyVersion(c.getTerminologyVersion());
			sr.setValue(c.getDefaultPreferredName());
			conceptsInScope.addSearchResult(sr);
		}

		if (project.isScopeDescendantsFlag()) {

			// for each scope concept, get descendants
			for (String terminologyId : project.getScopeConcepts()) {
				SearchResultList descendants =

						contentService.findDescendantsFromTreePostions(terminologyId,
								terminology, terminologyVersion);

				// cycle over descendants
				for (SearchResult sr : descendants.getSearchResults()) {
					conceptsInScope.addSearchResult(sr);
				}
			}
		}
		contentService.close();
		// get those excluded from scope
		SearchResultList excludedResultList =
				findConceptsExcludedFromScope(mapProjectId);

		// remove those excluded from scope
		SearchResultList finalConceptsInScope = new SearchResultListJpa();
		for (SearchResult sr : conceptsInScope.getSearchResults()) {
			if (!excludedResultList.contains(sr)) {
				finalConceptsInScope.addSearchResult(sr);
			}
		}

		Logger.getLogger(this.getClass()).info(
				"Finished getting scope concepts. size:"
						+ finalConceptsInScope.getCount());

		/**
		 * PrintWriter writer = new PrintWriter("C:/data/inScopeConcepts.txt",
		 * "UTF-8"); for(SearchResult sr : finalConceptsInScope.getSearchResults())
		 * { writer.println(sr.getTerminologyId()); } writer.close();
		 */

		return finalConceptsInScope;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#findUnmappedConceptsInScope
	 * (org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public SearchResultList findUnmappedConceptsInScope(Long mapProjectId)
			throws Exception {
		SearchResultList conceptsInScope = findConceptsInScope(mapProjectId);
		SearchResultList unmappedConceptsInScope = new SearchResultListJpa();

		// take everything in scope for the project minus concepts with mappings
		// (in that project) with workflow status of PUBLISHED or
		// READY_FOR_PUBLICATION
		for (SearchResult sr : conceptsInScope.getSearchResults()) {
			// if concept has no associated map records, add to list
			List<MapRecord> mapRecords =
					getMapRecordsForConcept(sr.getTerminologyId());
			boolean foundEndStage = false;
			for (MapRecord mapRecord : mapRecords) {
				if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
						|| mapRecord.getWorkflowStatus().equals(
								WorkflowStatus.READY_FOR_PUBLICATION)) {
					foundEndStage = true;
					break;
				}
			}
			if (!foundEndStage)
				unmappedConceptsInScope.addSearchResult(sr);
		}

		return unmappedConceptsInScope;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MappingService#
	 * findMappedConceptsOutOfScopeBounds(java.lang.Long)
	 */
	@Override
	public SearchResultList findMappedConceptsOutOfScopeBounds(Long mapProjectId)
			throws Exception {
		SearchResultList mappedConceptsOutOfBounds = new SearchResultListJpa();
		MapProject project = getMapProject(mapProjectId);
		List<MapRecord> mapRecordList = getMapRecordsForMapProject(mapProjectId);
		ContentService contentService = new ContentServiceJpa();

		for (MapRecord record : mapRecordList) {
			Concept c =
					contentService.getConcept(record.getConceptId(),
							project.getSourceTerminology(),
							project.getSourceTerminologyVersion());
			if (isConceptOutOfScopeBounds(c.getTerminologyId(), mapProjectId)) {
				SearchResult sr = new SearchResultJpa();
				sr.setId(c.getId());
				sr.setTerminologyId(c.getTerminologyId());
				sr.setTerminology(c.getTerminology());
				sr.setTerminologyVersion(c.getTerminologyVersion());
				sr.setValue(c.getDefaultPreferredName());
				mappedConceptsOutOfBounds.addSearchResult(sr);
			}

		}
		contentService.close();
		return mappedConceptsOutOfBounds;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#findConceptsExcludedFromScope
	 * (org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public SearchResultList findConceptsExcludedFromScope(Long mapProjectId)
			throws Exception {
		SearchResultList conceptsExcludedFromScope = new SearchResultListJpa();

		ContentService contentService = new ContentServiceJpa();
		MapProject project = getMapProject(mapProjectId);
		String terminology = project.getSourceTerminology();
		String terminologyVersion = project.getSourceTerminologyVersion();

		// add specified excluded concepts
		for (String conceptId : project.getScopeExcludedConcepts()) {
			Concept c =
					contentService.getConcept(conceptId, terminology, terminologyVersion);
			if (c != null) {
				SearchResult sr = new SearchResultJpa();
				sr.setId(c.getId());
				sr.setTerminologyId(c.getTerminologyId());
				sr.setTerminology(c.getTerminology());
				sr.setTerminologyVersion(c.getTerminologyVersion());
				sr.setValue(c.getDefaultPreferredName());
				conceptsExcludedFromScope.addSearchResult(sr);
			}
		}

		// add descendant excluded concepts if indicated
		if (project.isScopeExcludedDescendantsFlag()) {

			// for each excluded scope concept, get descendants
			for (String terminologyId : project.getScopeExcludedConcepts()) {
				SearchResultList descendants =
						contentService.findDescendantsFromTreePostions(terminologyId,
								terminology, terminologyVersion);

				// cycle over descendants
				for (SearchResult sr : descendants.getSearchResults()) {
					conceptsExcludedFromScope.addSearchResult(sr);
				}
			}

		}

		contentService.close();
		Logger.getLogger(this.getClass()).info(
				"conceptsExcludedFromScope.size:"
						+ conceptsExcludedFromScope.getCount());
		return conceptsExcludedFromScope;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#isConceptInScope(org.ihtsdo
	 * .otf.mapping.rf2.Concept, org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public boolean isConceptInScope(String conceptId, Long mapProjectId)
			throws Exception {
		MapProject project = getMapProject(mapProjectId);
		// if directly matches preset scope concept return true
		for (String c : project.getScopeConcepts()) {
			if (c.equals(conceptId))
				return true;
		}
		// don't make contentService if no chance descendants meet conditions
		if (!project.isScopeDescendantsFlag() && !project.isScopeDescendantsFlag())
			return false;

		ContentService contentService = new ContentServiceJpa();
		for (SearchResult tp : contentService.findTreePositionsForConcept(
				conceptId, project.getSourceTerminology(),
				project.getSourceTerminologyVersion()).getSearchResults()) {
			String ancestorPath = tp.getValue();
			if (project.isScopeExcludedDescendantsFlag()
					&& ancestorPath.contains(conceptId)) {
				continue;
			}
			if (project.isScopeDescendantsFlag() && ancestorPath.contains(conceptId)) {
				contentService.close();
				return true;
			}
		}
		contentService.close();
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#isConceptExcludedFromScope
	 * (org.ihtsdo.otf.mapping.rf2.Concept,
	 * org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public boolean isConceptExcludedFromScope(String conceptId, Long mapProjectId)
			throws Exception {
		MapProject project = getMapProject(mapProjectId);
		// if directly matches preset scope concept return true
		for (String c : project.getScopeExcludedConcepts()) {
			if (c.equals(conceptId))
				return true;
		}
		// don't make contentService if no chance descendants meet conditions
		if (!project.isScopeDescendantsFlag() && !project.isScopeDescendantsFlag())
			return false;

		ContentService contentService = new ContentServiceJpa();
		for (SearchResult tp : contentService.findTreePositionsForConcept(
				conceptId, project.getSourceTerminology(),
				project.getSourceTerminologyVersion()).getSearchResults()) {
			String ancestorPath = tp.getValue();
			if (project.isScopeDescendantsFlag() && ancestorPath.contains(conceptId)) {
				continue;
			}
			if (project.isScopeExcludedDescendantsFlag()
					&& ancestorPath.contains(conceptId)) {
				contentService.close();
				return true;
			}
		}
		contentService.close();
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#isConceptOutOfScopeBounds
	 * (java.lang.String, java.lang.Long)
	 */
	@Override
	public boolean isConceptOutOfScopeBounds(String conceptId, Long mapProjectId)
			throws Exception {
		MapProject project = getMapProject(mapProjectId);
		// if directly matches preset scope concept return false
		for (String c : project.getScopeConcepts()) {
			if (c.equals(conceptId))
				return false;
		}

		ContentService contentService = new ContentServiceJpa();
		for (SearchResult tp : contentService.findTreePositionsForConcept(
				conceptId, project.getSourceTerminology(),
				project.getSourceTerminologyVersion()).getSearchResults()) {
			String ancestorPath = tp.getValue();
			if (project.isScopeDescendantsFlag() && ancestorPath.contains(conceptId)) {
				return false;
			}
		}
		contentService.close();
		return true;
	}

	/**
	 * Given a concept, returns a list of descendant concepts that have no
	 * associated map record.
	 * 
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param thresholdLlc the maximum number of descendants a concept can have
	 *          before it is no longer considered a low-level concept (i.e. return
	 *          an empty list)
	 * @return the list of unmapped descendant concepts
	 * @throws Exception the exception
	 */
	@Override
	public SearchResultList findUnmappedDescendantsForConcept(
			String terminologyId, String terminology, String terminologyVersion,
			int thresholdLlc) throws Exception {

		SearchResultList unmappedDescendants = new SearchResultListJpa();

		// get hierarchical rel
		MetadataService metadataService = new MetadataServiceJpa();
		Map<Long, String> hierarchicalRelationshipTypeMap =
				metadataService.getHierarchicalRelationshipTypes(terminology,
						terminologyVersion);
		if (hierarchicalRelationshipTypeMap.keySet().size() > 1) {
			throw new IllegalStateException(
					"Map project source terminology has too many hierarchical relationship types - "
							+ terminology);
		}
		if (hierarchicalRelationshipTypeMap.keySet().size() < 1) {
			throw new IllegalStateException(
					"Map project source terminology has too few hierarchical relationship types - "
							+ terminology);
		}
		long hierarchicalRelationshipType =
				hierarchicalRelationshipTypeMap.entrySet().iterator().next().getKey();

		// get descendants
		ContentService contentService = new ContentServiceJpa();
		SearchResultList descendants =
				contentService.findDescendants(terminologyId, terminology,
						terminologyVersion, hierarchicalRelationshipType);

		// if number of descendants <= low-level concept threshold, treat as
		// high-level concept and report no unmapped
		if (descendants.getCount() <= thresholdLlc) {

			// cycle over descendants
			for (SearchResult sr : descendants.getSearchResults()) {

				// if descendant has no associated map records, add to list
				if (getMapRecordsForConcept(sr.getTerminologyId()).size() == 0) {
					unmappedDescendants.addSearchResult(sr);
				}
			}
		}

		// close managers
		contentService.close();
		metadataService.close();

		return unmappedDescendants;

	}

	// //////////////////////////////
	// Services to be implemented //
	// //////////////////////////////

	// //////////////////////////
	// Addition services ///
	// //////////////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#addMapEntry(org.ihtsdo.otf
	 * .mapping.model.MapEntry)
	 */
	@Override
	public void addMapEntry(MapEntry mapEntry) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.persist(mapEntry);
			tx.commit();
		} else {
			manager.persist(mapEntry);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#addMapPrinciple(org.ihtsdo
	 * .otf.mapping.model.MapPrinciple)
	 */
	@Override
	public void addMapPrinciple(MapPrinciple mapPrinciple) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.persist(mapPrinciple);
			tx.commit();
		} else {
			manager.persist(mapPrinciple);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#addMapAdvice(org.ihtsdo.
	 * otf.mapping.model.MapAdvice)
	 */
	@Override
	public void addMapAdvice(MapAdvice mapAdvice) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.persist(mapAdvice);
			tx.commit();
		} else {
			manager.persist(mapAdvice);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#addMapRelation(org.ihtsdo.
	 * otf.mapping.model.MapRelation)
	 */
	@Override
	public void addMapRelation(MapRelation mapRelation) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.persist(mapRelation);
			tx.commit();
		} else {
			manager.persist(mapRelation);
		}
	}

	// //////////////////////////
	// Update services ///
	// //////////////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#updateMapEntry(org.ihtsdo
	 * .otf.mapping.model.MapEntry)
	 */
	@Override
	public void updateMapEntry(MapEntry mapEntry) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.merge(mapEntry);
			tx.commit();
		} else {
			manager.merge(mapEntry);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#updateMapPrinciple(org.ihtsdo
	 * .otf.mapping.model.MapPrinciple)
	 */
	@Override
	public void updateMapPrinciple(MapPrinciple mapPrinciple) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.merge(mapPrinciple);
			tx.commit();
			// manager.close();
		} else {
			manager.merge(mapPrinciple);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#updateMapAdvice(org.ihtsdo
	 * .otf.mapping.model.MapAdvice)
	 */
	@Override
	public void updateMapAdvice(MapAdvice mapAdvice) {
		if (getTransactionPerOperation()) {

			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.merge(mapAdvice);
			tx.commit();
			// manager.close();
		} else {
			manager.merge(mapAdvice);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#updateMapRelation(org.ihtsdo
	 * .otf.mapping.model.MapRelation)
	 */
	@Override
	public void updateMapRelation(MapRelation mapRelation) {
		if (getTransactionPerOperation()) {

			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.merge(mapRelation);
			tx.commit();
			// manager.close();
		} else {
			manager.merge(mapRelation);
		}
	}

	// //////////////////////////
	// Removal services ///
	// //////////////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#removeMapEntry(java.lang
	 * .Long)
	 */
	@Override
	public void removeMapEntry(Long mapEntryId) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			MapEntry me = manager.find(MapEntryJpa.class, mapEntryId);
			if (manager.contains(me)) {
				manager.remove(me);
			} else {
				manager.remove(manager.merge(me));
			}
			tx.commit();
		} else {
			MapEntry me = manager.find(MapEntryJpa.class, mapEntryId);
			if (manager.contains(me)) {
				manager.remove(me);
			} else {
				manager.remove(manager.merge(me));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#removeMapPrinciple(java.lang.Long)
	 */
	@Override
	public void removeMapPrinciple(Long mapPrincipleId) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			MapPrinciple mp = manager.find(MapPrincipleJpa.class, mapPrincipleId);
			if (manager.contains(mp)) {
				manager.remove(mp);
			} else {
				manager.remove(manager.merge(mp));
			}
			tx.commit();
		} else {
			MapPrinciple mp = manager.find(MapPrincipleJpa.class, mapPrincipleId);
			if (manager.contains(mp)) {
				manager.remove(mp);
			} else {
				manager.remove(manager.merge(mp));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MappingService#removeMapAdvice(java.lang.Long)
	 */
	@Override
	public void removeMapAdvice(Long mapAdviceId) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			MapAdvice ma = manager.find(MapAdviceJpa.class, mapAdviceId);
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
			tx.commit();
		} else {
			MapAdvice ma = manager.find(MapAdviceJpa.class, mapAdviceId);
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MappingService#removeMapRelation(java.lang.Long)
	 */
	@Override
	public void removeMapRelation(Long mapRelationId) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			MapRelation ma = manager.find(MapRelationJpa.class, mapRelationId);
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
			tx.commit();
		} else {
			MapRelation ma = manager.find(MapRelationJpa.class, mapRelationId);
			if (manager.contains(ma)) {
				manager.remove(ma);
			} else {
				manager.remove(manager.merge(ma));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapPrinciple(java.lang.Long)
	 */
	@Override
	public MapPrinciple getMapPrinciple(Long id) {

		MapPrinciple m = null;

		javax.persistence.Query query =
				manager.createQuery("select m from MapPrincipleJpa m where id = :id");
		query.setParameter("id", id);
		try {
			m = (MapPrinciple) query.getSingleResult();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map principle query for id = " + id + " returned no results!");
			return null;
		}

		return m;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapPrinciples()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<MapPrinciple> getMapPrinciples() {
		List<MapPrinciple> mapPrinciples = new ArrayList<>();

		javax.persistence.Query query =
				manager.createQuery("select m from MapPrincipleJpa m");

		// Try query
		mapPrinciples = query.getResultList();

		return mapPrinciples;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapAdvices()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<MapAdvice> getMapAdvices() {
		List<MapAdvice> mapAdvices = new ArrayList<>();

		javax.persistence.Query query =
				manager.createQuery("select m from MapAdviceJpa m");

		// Try query
		mapAdvices = query.getResultList();

		return mapAdvices;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapRelations()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<MapRelation> getMapRelations() {
		List<MapRelation> mapRelations = new ArrayList<>();

		javax.persistence.Query query =
				manager.createQuery("select m from MapRelationJpa m");

		// Try query
		mapRelations = query.getResultList();

		return mapRelations;
	}

	// ///////////////////////////////////////
	// / Services for Map Project Creation
	// ///////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MappingService#createMapRecordsForMapProject(java.lang.Long, org.ihtsdo.otf.mapping.helpers.WorkflowStatus)
	 */
	@Override
	public void createMapRecordsForMapProject(Long mapProjectId,
			WorkflowStatus workflowStatus) throws Exception {
		MapProject mapProject = getMapProject(mapProjectId);
		Logger.getLogger(MappingServiceJpa.class).warn(
				"Find map records from query for project - " + mapProjectId
				+ " workflowStatus - " + workflowStatus);
		if (!getTransactionPerOperation()) {
			throw new IllegalStateException(
					"The application must let the service manage transactions for this method");
		}

		// retrieve all complex map ref set members for mapProject
		javax.persistence.Query query =
				manager.createQuery("select r from ComplexMapRefSetMemberJpa r "
						+ "where r.refSetId = :refSetId order by r.concept.id, "
						+ "r.mapBlock, r.mapGroup, r.mapPriority");
		query.setParameter("refSetId", mapProject.getRefSetId());
		List<ComplexMapRefSetMember> complexMapRefSetMembers = new ArrayList<>();
		for (Object member : query.getResultList()) {
			ComplexMapRefSetMember refSetMember = (ComplexMapRefSetMember) member;
			complexMapRefSetMembers.add(refSetMember);
		}
		Logger.getLogger(MappingServiceJpa.class).warn(
				"  " + complexMapRefSetMembers.size()
				+ " map records processed (some skipped)");
		createMapRecordsForMapProject(mapProjectId, complexMapRefSetMembers,
				workflowStatus);
	}

	// ONLY FOR TESTING PURPOSES
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#removeMapRecordsForProjectId
	 * (java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Long removeMapRecordsForProject(Long mapProjectId) {

		EntityTransaction tx = manager.getTransaction();

		int nRecords = 0;
		tx.begin();
		List<MapRecord> records =
				manager
				.createQuery(
						"select m from MapRecordJpa m where m.mapProjectId = :mapProjectId")
						.setParameter("mapProjectId", mapProjectId).getResultList();

		for (MapRecord record : records) {

			// delete notes
			for (MapNote note : record.getMapNotes()) {
				if (manager.contains(note)) {
					manager.remove(note);
				} else {
					manager.remove(manager.merge(note));
				}
			}
			record.setMapNotes(null);

			// delete entries
			for (MapEntry entry : record.getMapEntries()) {

				// delete entry notes
				for (MapNote entryNote : entry.getMapNotes()) {
					if (manager.contains(entryNote)) {
						manager.remove(entryNote);
					} else {
						manager.remove(manager.merge(entryNote));
					}
				}
				entry.setMapNotes(null);

				// remove advices
				entry.setMapAdvices(null);

				// merge entry to remove principle/advice associations
				manager.merge(entry);

				// delete entry
				manager.remove(entry);

				nRecords++;
			}

			// remove principles
			record.setMapPrinciples(null);

			// merge record to remove principle associations
			manager.merge(record);

			// delete record
			manager.remove(record);
		}

		tx.commit();

		Logger.getLogger(this.getClass()).debug(
				Integer.toString(nRecords) + " records deleted for map project id = "
						+ mapProjectId);

		return new Long(nRecords);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#createMapRecordsForMapProject
	 * (java.lang.Long, java.util.List)
	 */
	@Override
	public void createMapRecordsForMapProject(Long mapProjectId,
			List<ComplexMapRefSetMember> complexMapRefSetMembers,
			WorkflowStatus workflowStatus) throws Exception {
		MapProject mapProject = getMapProject(mapProjectId);
		Logger.getLogger(this.getClass()).debug(
				"  Starting create map records for map project - "
						+ mapProject.getName());

		// Verify application is letting the service manage transactions
		if (!getTransactionPerOperation()) {
			throw new IllegalStateException(
					"The application must let the service manage transactions for this method");
		}

		// Setup content service
		ContentService contentService = new ContentServiceJpa();

		// Get map relation id->name mapping
		MetadataService metadataService = new MetadataServiceJpa();

		Map<Long, String> relationIdNameMap = metadataService.getMapRelations(mapProject.getSourceTerminology(),
				mapProject.getSourceTerminologyVersion());
		Logger.getLogger(this.getClass()).debug("    relationIdNameMap = " + relationIdNameMap);

		// use the map relation id->name mapping to construct a hash set of MapRelations
		Map<String, MapRelation> mapRelationIdMap = new HashMap<>();
		for (MapRelation mapRelation : getMapRelations()) {
			mapRelationIdMap.put(mapRelation.getTerminologyId(), mapRelation);
		}

		Map<Long, String> hierarchicalRelationshipTypeMap = metadataService.getHierarchicalRelationshipTypes(mapProject.getSourceTerminology(), 
				mapProject.getSourceTerminologyVersion());
		if (hierarchicalRelationshipTypeMap.keySet().size() > 1) {
			throw new IllegalStateException(
					"Map project source terminology has too many hierarchical relationship types - "
							+ mapProject.getSourceTerminology());
		}
		if (hierarchicalRelationshipTypeMap.keySet().size() < 1) {
			throw new IllegalStateException(
					"Map project source terminology has too few hierarchical relationship types - "
							+ mapProject.getSourceTerminology());
		}
		long hierarchicalRelationshipType =
				hierarchicalRelationshipTypeMap.entrySet().iterator().next().getKey();

		boolean prevTransactionPerOperationSetting = getTransactionPerOperation();
		setTransactionPerOperation(false);
		beginTransaction();
		List<MapAdvice> mapAdvices = getMapAdvices();
		int mapPriorityCt = 0;
		int prevMapGroup = 0;
		try {
			// instantiate other local variables
			String prevConceptId = null;
			MapRecord mapRecord = null;
			int ct = 0;
			MapUser loaderUser = getMapUser("loader");

			if (loaderUser == null) {
				throw new Exception("Loader user could not be found");
			}

			for (ComplexMapRefSetMember refSetMember : complexMapRefSetMembers) {

				// Skip inactive cases
				if (!refSetMember.isActive()) {
					Logger.getLogger(this.getClass()).debug(
							"Skipping refset member " + refSetMember.getTerminologyId());
					continue;
				}

				// Skip concept exclusion rules in all cases
				if (refSetMember.getMapRule().matches("IFA\\s\\d*\\s\\|.*\\s\\|")
						&& !(refSetMember.getMapAdvice()
								.contains("MAP IS CONTEXT DEPENDENT FOR GENDER"))
								&& !(refSetMember.getMapRule()
										.matches("IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>]"))) {
					Logger.getLogger(this.getClass()).debug(
							"    Skipping refset member exclusion rule "
									+ refSetMember.getTerminologyId());
					continue;
				}

				// retrieve the concept
				Logger.getLogger(this.getClass())
				.debug("    Get refset member concept");
				Concept concept = refSetMember.getConcept();

				// if no concept for this ref set member, skip
				if (concept == null) {
					throw new NoResultException(
							"    Concept is unexpectedly missing for "
									+ refSetMember.getTerminologyId());
				}

				// if different concept than previous ref set member, create new
				// mapRecord
				if (!concept.getTerminologyId().equals(prevConceptId)) {
					Logger.getLogger(this.getClass()).debug(
							"    Creating map record for " + concept.getTerminologyId());

					mapPriorityCt = 0;
					prevMapGroup = 0;
					mapRecord = new MapRecordJpa();
					mapRecord.setConceptId(concept.getTerminologyId());
					mapRecord.setConceptName(concept.getDefaultPreferredName());
					mapRecord.setMapProjectId(mapProject.getId());

					// get the number of descendants - Need to optimize this
					// Need a tool to compute and save this for LLCs (e.g. having < 11
					// descendants)
					mapRecord.setCountDescendantConcepts(new Long(contentService
							.findDescendants(concept.getTerminologyId(),
									concept.getTerminology(), concept.getTerminologyVersion(),
									hierarchicalRelationshipType).getCount()));
					Logger.getLogger(this.getClass()).debug(
							"      Computing descendant ct = "
									+ mapRecord.getCountDescendantConcepts());

					// mapRecord.setCountDescendantConcepts(0L);

					// set the previous concept to this concept
					prevConceptId = refSetMember.getConcept().getTerminologyId();

					// set the owner and lastModifiedBy user fields to loaderUser
					mapRecord.setOwner(loaderUser);
					mapRecord.setLastModifiedBy(loaderUser);

					// set the workflow status to published
					mapRecord.setWorkflowStatus(workflowStatus);

					// persist the record
					addMapRecord(mapRecord);

					if (++ct % 500 == 0) {
						Logger.getLogger(MappingServiceJpa.class).info(
								"    " + ct + " records created");
						commit();
						beginTransaction();
					}
				}

				// check if target is in desired terminology; if so, create entry
				String targetName = null;

				if (!refSetMember.getMapTarget().equals("")) {
					Concept c =
							contentService.getConcept(refSetMember.getMapTarget(),
									mapProject.getDestinationTerminology(),
									mapProject.getDestinationTerminologyVersion());
					if (c == null) {
						targetName = "Target name could not be determined";
					} else {
						targetName = c.getDefaultPreferredName();
					}
					Logger.getLogger(this.getClass()).debug(
							"      Setting target name " + targetName);
				}

				// Set map relation id as well from the cache
				String relationName = null;
				if (refSetMember.getMapRelationId() != null) {
					relationName = relationIdNameMap.get(refSetMember.getMapRelationId());
					Logger.getLogger(this.getClass()).debug(
							"      Look up relation name = " + relationName);
				}

				Logger.getLogger(this.getClass()).debug("      Create map entry");
				MapEntry mapEntry = new MapEntryJpa();
				mapEntry.setTargetId(refSetMember.getMapTarget());
				mapEntry.setTargetName(targetName);
				mapEntry.setMapRecord(mapRecord);
				mapEntry.setMapRelation(mapRelationIdMap.get(refSetMember.getMapRelationId().toString()));
				String rule = refSetMember.getMapRule();
				if (rule.equals("OTHERWISE TRUE"))
					rule = "TRUE";
				mapEntry.setRule(rule);
				mapEntry.setMapBlock(refSetMember.getMapBlock());
				mapEntry.setMapGroup(refSetMember.getMapGroup());
				if (prevMapGroup != refSetMember.getMapGroup()) {
					mapPriorityCt = 0;
					prevMapGroup = refSetMember.getMapGroup();
				}
				// Increment map priority as we go through records
				mapEntry.setMapPriority(++mapPriorityCt);

				mapRecord.addMapEntry(mapEntry);

				// Add support for advices - and there can be multiple map advice values
				// Only add advice if it is an allowable value and doesn't match
				// relation name
				// This should automatically exclude IFA/ALWAYS advice
				Logger.getLogger(this.getClass()).debug("      Setting map advice");
				if (refSetMember.getMapAdvice() != null
						&& !refSetMember.getMapAdvice().equals("")) {
					for (MapAdvice ma : mapAdvices) {
						if (refSetMember.getMapAdvice().indexOf(ma.getName()) != -1
								&& !ma.getName().equals(relationName)) {
							mapEntry.addMapAdvice(ma);
							Logger.getLogger(this.getClass()).debug("    " + ma.getName());
						}
					}
				}
			}

			commit();
			contentService.close();
			metadataService.close();
		} catch (Exception e) {
			setTransactionPerOperation(prevTransactionPerOperationSetting);
			throw e;
		}
		setTransactionPerOperation(prevTransactionPerOperationSetting);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getTransactionPerOperation()
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

	// ////////////////////////
	// UTILITY FUNCTIONS
	// ////////////////////////

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MappingService#userExists(java.lang.String)
	 */
	@Override
	public boolean userExists(String mapUser) {

		List<MapUser> mapUsers = getMapUsers();

		// find if user with this username exists in list of valid users
		for (MapUser m : mapUsers) {
			if (m.getUserName().equals(mapUser))
				return true;
		}

		// if not found, return false
		return false;
	}

	/**
	 * Retrieve all map age ranges.
	 * 
	 * @return a List of MapAgeRanges
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<MapAgeRange> getMapAgeRanges() {

		List<MapAgeRange> m = null;

		// construct query
		javax.persistence.Query query =
				manager.createQuery("select m from MapAgeRangeJpa m");

		// Try query

		m = query.getResultList();

		return m;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MappingService#addMapAgeRange(org.ihtsdo.otf.mapping.model.MapAgeRange)
	 */
	@Override
	public MapAgeRange addMapAgeRange(MapAgeRange mapAgeRange) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.persist(mapAgeRange);
			tx.commit();

		} else {
			manager.persist(mapAgeRange);
		}

		return mapAgeRange;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MappingService#removeMapAgeRange(java.lang.Long)
	 */
	@Override
	public void removeMapAgeRange(Long mapAgeRangeId) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			MapAgeRange mar = manager.find(MapAgeRangeJpa.class, mapAgeRangeId);
			if (manager.contains(mar)) {
				manager.remove(mar);
			} else {
				manager.remove(manager.merge(mar));
			}
			tx.commit();
		} else {
			MapAgeRange mar = manager.find(MapAgeRangeJpa.class, mapAgeRangeId);
			if (manager.contains(mar)) {
				manager.remove(mar);
			} else {
				manager.remove(manager.merge(mar));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#updateMapAgeRange(org.ihtsdo
	 * .otf.mapping.model.MapAgeRange)
	 */
	@Override
	public void updateMapAgeRange(MapAgeRange mapAgeRange) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.merge(mapAgeRange);
			tx.commit();
		} else {
			manager.merge(mapAgeRange);
		}
	}
	
	
	@Override
	@XmlTransient
	public ProjectSpecificAlgorithmHandler getProjectSpecificAlgorithmHandler(MapProject mapProject) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		
		ProjectSpecificAlgorithmHandler algorithmHandler = 
				(ProjectSpecificAlgorithmHandler) Class.forName("org.ihtsdo.otf.mapping.jpa.handlers." + mapProject.getProjectSpecificAlgorithmHandlerClass())
				.newInstance();
				
		algorithmHandler.setMapProject(mapProject); 
		return algorithmHandler;

		
	}

}
