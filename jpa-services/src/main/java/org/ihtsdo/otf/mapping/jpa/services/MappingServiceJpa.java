package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
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

import org.apache.log4j.Logger;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * JPA implementation of the {@link MappingService}
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
			fieldNames = new HashSet<String>();

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
			m = (MapProject) query.getSingleResult();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map project query for id = " + id + " returned no results!");
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

		// Try query

		m = query.getResultList();

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

		System.out.println(Integer.toString(m.size()) + " map projects retrieved");

		for (MapProject mp : m) {
			s.addSearchResult(new SearchResultJpa(mp.getId(), mp.getRefSetId()
					.toString(), mp.getName()));
		}

		s.sortSearchResultsById();

		if (fullTextEntityManager != null) {
			fullTextEntityManager.close();
		}

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
	// MapSpecialist
	// - getMapSpecialists()
	// - getMapProjectsForSpecialist(MapSpecialist mapSpecialist)
	// - findMapSpecialists(String query)
	// - addMapSpecialist(MapSpecialist mapSpecialist)
	// - updateMapSpecialist(MapSpecialist mapSpecialist)
	// - removeMapSpecialist(Long id)
	// ///////////////////////////////////////////////////////////////

	/**
	 * Retrieve all map specialists.
	 * 
	 * @return a List of MapSpecialists
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<MapSpecialist> getMapSpecialists() {

		List<MapSpecialist> m = null;

		javax.persistence.Query query =
				manager.createQuery("select m from MapSpecialistJpa m");

		// Try query

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
	public MapSpecialist getMapSpecialist(Long id) {

		MapSpecialist m = null;

		javax.persistence.Query query =
				manager.createQuery("select m from MapSpecialistJpa m where id = :id");
		query.setParameter("id", id);
		try {
			m = (MapSpecialist) query.getSingleResult();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map specialist query for id = " + id + " returned no results!");
			return null;
		}

		return m;

	}

	/**
	 * Retrieve all map projects assigned to a particular map specialist.
	 * 
	 * @param mapSpecialist the map specialist
	 * @return a List of MapProjects
	 */
	@Override
	public List<MapProject> getMapProjectsForMapSpecialist(
		MapSpecialist mapSpecialist) {

		List<MapProject> mp_list = getMapProjects();
		List<MapProject> mp_list_return = new ArrayList<MapProject>();

		// iterate and check for presence of mapSpecialist
		for (MapProject mp : mp_list) {

			Set<MapSpecialist> ms_set = mp.getMapSpecialists();

			for (MapSpecialist ms : ms_set) {
				if (ms.equals(mapSpecialist)) {
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
	public SearchResultList findMapSpecialists(String query,
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
							searchFactory.getAnalyzer(MapSpecialistJpa.class));
			queryParser.setAllowLeadingWildcard(false);
			luceneQuery = queryParser.parse(query);

		} else { // field:value
			QueryParser queryParser =
					new QueryParser(Version.LUCENE_36, "summary",
							searchFactory.getAnalyzer(MapSpecialistJpa.class));
			luceneQuery = queryParser.parse(query);
		}

		List<MapSpecialist> m =
				fullTextEntityManager.createFullTextQuery(luceneQuery,
						MapSpecialistJpa.class).getResultList();

		for (MapSpecialist ms : m) {
			s.addSearchResult(new SearchResultJpa(ms.getId(), "", ms.getName()));
		}

		s.sortSearchResultsById();

		if (fullTextEntityManager != null) {
			fullTextEntityManager.close();
		}

		return s;
	}

	/**
	 * Add a map specialist.
	 * 
	 * @param mapSpecialist the map specialist
	 */
	@Override
	public void addMapSpecialist(MapSpecialist mapSpecialist) {

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.persist(mapSpecialist);
			tx.commit();
		} else {
			manager.persist(mapSpecialist);
		}

	}

	/**
	 * Update a map specialist.
	 * 
	 * @param mapSpecialist the changed map specialist
	 */
	@Override
	public void updateMapSpecialist(MapSpecialist mapSpecialist) {

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.merge(mapSpecialist);
			tx.commit();
		} else {
			manager.merge(mapSpecialist);
		}

	}

	/**
	 * Remove (delete) a map specialist.
	 * 
	 * @param mapSpecialistId the map specialist to be removed
	 */
	@Override
	public void removeMapSpecialist(Long mapSpecialistId) {

		EntityTransaction tx = manager.getTransaction();

		// retrieve this map specialist
		MapSpecialist ms = manager.find(MapSpecialistJpa.class, mapSpecialistId);

		// retrieve all projects on which this specialist appears
		List<MapProject> projects = getMapProjectsForMapSpecialist(ms);

		if (getTransactionPerOperation()) {
			// remove specialist from all these projects
			tx.begin();
			for (MapProject mp : projects) {
				mp.removeMapSpecialist(ms);
				manager.merge(mp);
			}
			tx.commit();

			// remove specialist
			tx.begin();
			if (manager.contains(ms)) {
				manager.remove(ms);
			} else {
				manager.remove(manager.merge(ms));
			}
			tx.commit();

		} else {
			for (MapProject mp : projects) {
				mp.removeMapSpecialist(ms);
				manager.merge(mp);
			}
			if (manager.contains(ms)) {
				manager.remove(ms);
			} else {
				manager.remove(manager.merge(ms));
			}
			manager.remove(ms);
		}

	}

	// ///////////////////////////////////////////////////
	// MapLead
	// - getMapLeads()
	// - getMapProjectsForLead(mapSpecialist)
	// - findMapSpecialists(query)
	// - addMapLead(mapLead)
	// - updateMapLead(mapLead)
	// - removeMapLead(id)
	// ///////////////////////////////////////////////////

	/**
	 * Retrieve all map leads.
	 * 
	 * @return a List of MapLeads
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<MapLead> getMapLeads() {

		List<MapLead> mapLeads = new ArrayList<MapLead>();

		javax.persistence.Query query =
				manager.createQuery("select m from MapLeadJpa m");

		// Try query

		mapLeads = query.getResultList();

		return mapLeads;
	}

	/**
	 * Return map lead for auto-generated id.
	 * 
	 * @param id the auto-generated id
	 * @return the MapLead
	 */
	@Override
	public MapLead getMapLead(Long id) {

		MapLead m = null;

		javax.persistence.Query query =
				manager.createQuery("select m from MapLeadJpa m where id = :id");
		query.setParameter("id", id);
		try {
			m = (MapLead) query.getSingleResult();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map lead query for id = " + id + " returned no results!");
			return null;
		}

		return m;

	}

	/**
	 * Retrieve all map projects assigned to a particular map lead.
	 * 
	 * @param mapLead the map lead
	 * @return a List of MapProjects
	 */
	@Override
	public List<MapProject> getMapProjectsForMapLead(MapLead mapLead) {

		List<MapProject> mp_list = getMapProjects();
		List<MapProject> mp_list_return = new ArrayList<MapProject>();

		// iterate and check for presence of mapSpecialist
		for (MapProject mp : mp_list) {
			Set<MapLead> ml_set = mp.getMapLeads();

			for (MapLead ml : ml_set) {
				if (ml.equals(mapLead)) {
					mp_list_return.add(mp);
				}
			}
		}
		return mp_list_return;
	}

	/**
	 * Query for MapLeads.
	 * 
	 * @param query the query
	 * @param pfsParameter the pfs parameter
	 * @return the List of MapProjects
	 * @throws Exception the exception
	 */
	@Override
	@SuppressWarnings("unchecked")
	public SearchResultList findMapLeads(String query, PfsParameter pfsParameter)
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
							searchFactory.getAnalyzer(MapLeadJpa.class));
			queryParser.setAllowLeadingWildcard(false);
			luceneQuery = queryParser.parse(query);

		} else { // field:value
			QueryParser queryParser =
					new QueryParser(Version.LUCENE_36, "summary",
							searchFactory.getAnalyzer(MapLeadJpa.class));
			luceneQuery = queryParser.parse(query);
		}

		List<MapLead> m =
				fullTextEntityManager
						.createFullTextQuery(luceneQuery, MapLeadJpa.class).getResultList();

		for (MapLead ml : m) {
			s.addSearchResult(new SearchResultJpa(ml.getId(), "", ml.getName()));
		}

		s.sortSearchResultsById();

		return s;
	}

	/**
	 * Add a map lead.
	 * 
	 * @param mapLead the map lead
	 */
	@Override
	public void addMapLead(MapLead mapLead) {
		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.persist(mapLead);
			tx.commit();
		} else {
			manager.persist(mapLead);
		}
	}

	/**
	 * Update a map lead.
	 * 
	 * @param mapLead the changed map lead
	 */
	@Override
	public void updateMapLead(MapLead mapLead) {

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.merge(mapLead);
			tx.commit();

		} else {
			manager.merge(mapLead);
		}

	}

	/**
	 * Remove (delete) a map lead.
	 * 
	 * @param mapLeadId the map lead to be removed
	 */
	@Override
	public void removeMapLead(Long mapLeadId) {

		EntityTransaction tx = manager.getTransaction();

		// retrieve this map lead
		MapLead ml = manager.find(MapLeadJpa.class, mapLeadId);

		// retrieve all projects on which this lead appears
		List<MapProject> projects = getMapProjectsForMapLead(ml);

		if (getTransactionPerOperation()) {
			// remove lead from all these projects
			tx.begin();
			for (MapProject mp : projects) {
				mp.removeMapLead(ml);
				manager.merge(mp);
			}
			tx.commit();

			// remove lead
			tx.begin();
			if (manager.contains(ml)) {
				manager.remove(ml);
			} else {
				manager.remove(manager.merge(ml));
			}
			tx.commit();

		} else {
			for (MapProject mp : projects) {
				mp.removeMapLead(ml);
				manager.merge(mp);
			}
			if (manager.contains(ml)) {
				manager.remove(ml);
			} else {
				manager.remove(manager.merge(ml));
			}
		}

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

			System.out.println("Returning record_id... "
					+ ((r != null) ? r.getId().toString() : "null"));
			return r;

		} catch (NoResultException e) {
			System.out.println("MapRecord query for id = " + id
					+ " returned no results!");
			return null;
		} catch (NonUniqueResultException e) {
			System.out.println("MapRecord query for id = " + id
					+ " returned multiple results!");
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

		System.out.println(Integer.toString(m.size()) + " map records retrieved");

		for (MapRecord mp : m) {
			s.addSearchResult(new SearchResultJpa(mp.getId(), mp.getConceptId()
					.toString(), mp.getConceptName()));
		}

		s.sortSearchResultsById();

		if (fullTextEntityManager != null) {
			fullTextEntityManager.close();
		}

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
	 */
	@Override
	public void addMapRecord(MapRecord mapRecord) {

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();

			tx.begin();
			manager.persist(mapRecord);
			tx.commit();
		} else {
			manager.persist(mapRecord);
		}

	}

	/**
	 * Update a map record.
	 * 
	 * @param mapRecord the map record to be updated
	 */
	@Override
	public void updateMapRecord(MapRecord mapRecord) {

		if (getTransactionPerOperation()) {
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			manager.merge(mapRecord);
			tx.commit();
		} else {
			manager.merge(mapRecord);
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

	// //////////////////////////////////////////////
	// Descendant services
	// //////////////////////////////////////////////

	/**
	 * Retrieve map records for a given concept id.
	 * 
	 * @param conceptId the concept id
	 * @return the list of map records
	 */
	@Override
	public List<MapRecord> getMapRecordsForConceptId(String conceptId) {
		List<MapRecord> m = null;

		// construct query
		javax.persistence.Query query =
				manager
						.createQuery("select m from MapRecordJpa m where conceptId = :conceptId");

		// Try query
		query.setParameter("conceptId", conceptId);
		m = query.getResultList();

		return m;
	}

	/**
	 * Helper function for retrieving map records given an internal hibernate
	 * concept id
	 * @param conceptId the concept id in Long form
	 * @return the map records where this concept is referenced
	 */
	@Override
	public List<MapRecord> getMapRecordsForConcept(Long conceptId) {

		// call retrieval function with concept
		return getMapRecordsForConcept(manager.find(ConceptJpa.class, conceptId));
	}

	/**
	 * Given a Concept, retrieve map records that reference this as a source
	 * Concept
	 * @param concept the Concept object
	 * @return a list of MapRecords referencing this Concept
	 */
	@Override
	public List<MapRecord> getMapRecordsForConcept(Concept concept) {

		// find maprecords where:
		// (1) the conceptId matches the concept terminologyId
		// (2) the concept terminology matches the source terminology for the
		// mapRecord's project
		@SuppressWarnings("unchecked")
		List<MapRecord> results =
				manager
						.createQuery(
								"select mr from MapRecordJpa "
										+ "where conceptId = :conceptId and"
										+ "and mapProjectId in (select mp.id from MapProjectJpa mp where sourceTerminology = :sourceTerminology")
						.setParameter("conceptId", concept.getTerminologyId())
						.setParameter("sourceTerminology", concept.getTerminology())
						.getResultList();

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
	public Long getMapRecordCountForMapProjectId(Long mapProjectId,
		PfsParameter pfsParameter) throws Exception {

		// if no paging/filtering/sorting object, retrieve total number of records
		if (pfsParameter == null) {

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

	/**
	 * Helper function: retrieves all map records for a map project id without
	 * paging/filtering/sorting.
	 * 
	 * @param mapProjectId the concept id
	 * @return the list of map records
	 * @throws Exception
	 */
	@Override
	public List<MapRecord> getMapRecordsForMapProjectId(Long mapProjectId)
		throws Exception {

		return getMapRecordsForMapProjectId(mapProjectId, null);
	}

	@Override
	public List<MapRecord> getMapRecordsForMapProjectId(Long mapProjectId,
		PfsParameter pfsParameter) throws Exception {

		if (pfsParameter != null && pfsParameter.getFilterString() != null) {
			return getMapRecordsForMapProjectIdWithQuery(mapProjectId, pfsParameter);
		} else {
			return getMapRecordsForMapProjectIdWithNoQuery(mapProjectId, pfsParameter);
		}
	}

	/**
	 * Retrieve map records for a given concept id and paging/filtering/sorting
	 * parameters.
	 * 
	 * @param mapProjectId the concept id
	 * @param pfs the paging/filtering/sorting parameter object
	 * @return the list of map records
	 */
	@Override
	public List<MapRecord> getMapRecordsForMapProjectIdWithNoQuery(
		Long mapProjectId, PfsParameter pfsParameter) {
		List<MapRecord> m = null;

		// construct query
		javax.persistence.Query query =
				manager
						.createQuery("select m from MapRecordJpa m where mapProjectId = :mapProjectId");

		// Try query
		try {
			query.setParameter("mapProjectId", mapProjectId);

			if (pfsParameter != null) {
				query.setFirstResult(pfsParameter.getStartIndex());
				query.setMaxResults(pfsParameter.getMaxResults());
			}

			m = query.getResultList();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map project query for id = " + mapProjectId
							+ " returned no map record results!");
			return null;
		}

		return m;
	}

	/**
	 * Test function for .setParameter
	 * @throws ParseException
	 */
	@Override
	public List<MapRecord> testCriteriaQuery(Long mapProjectId, String query,
		PfsParameter pfsParameter) throws Exception {
		System.out.println("Filter string: " + pfsParameter.getFilterString());
		System.out.println("nStart  = "
				+ Integer.toString(pfsParameter.getStartIndex()));
		System.out.println("nMaxRes = "
				+ Integer.toString(pfsParameter.getMaxResults()));

		FullTextEntityManager fullTextEntityManager =
				Search.getFullTextEntityManager(manager);
		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();

		// construct luceneQuery based on URL format
		QueryParser queryParser =
				new QueryParser(Version.LUCENE_36, "summary",
						searchFactory.getAnalyzer(MapRecordJpa.class));
		Query luceneQuery = queryParser.parse(query);

		org.hibernate.search.jpa.FullTextQuery ftquery =
				(FullTextQuery) fullTextEntityManager.createFullTextQuery(luceneQuery,
						MapRecordJpa.class);

		if (pfsParameter.getStartIndex() != -1
				&& pfsParameter.getMaxResults() != -1) {
			ftquery.setFirstResult(pfsParameter.getStartIndex());
			ftquery.setMaxResults(pfsParameter.getMaxResults());
		}
		if (pfsParameter.getSortField() != null) {
			ftquery.setSort(new Sort(new SortField(pfsParameter.getSortField(),
					SortField.STRING)));
		}

		List<MapRecord> m = ftquery.getResultList();

		System.out.println(Integer.toString(m.size()) + " records retrieved");

		return m;

	}

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
	public List<MapRecord> getMapRecordsForMapProjectIdWithQuery(
		Long mapProjectId, PfsParameter pfsParameter) throws Exception {

		if (pfsParameter != null) {
			System.out.println("Filter string: " + pfsParameter.getFilterString());
			System.out.println("nStart  = "
					+ Integer.toString(pfsParameter.getStartIndex()));
			System.out.println("nMaxRes = "
					+ Integer.toString(pfsParameter.getMaxResults()));
		}
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

		org.hibernate.search.jpa.FullTextQuery ftquery =
				(FullTextQuery) fullTextEntityManager.createFullTextQuery(luceneQuery,
						MapRecordJpa.class);

		if (pfsParameter.getStartIndex() != -1
				&& pfsParameter.getMaxResults() != -1) {
			ftquery.setFirstResult(pfsParameter.getStartIndex());
			ftquery.setMaxResults(pfsParameter.getMaxResults());
		}
		if (pfsParameter.getSortField() != null) {
			ftquery.setSort(new Sort(new SortField(pfsParameter.getSortField(),
					SortField.STRING)));
		}

		List<MapRecord> m = ftquery.getResultList();

		System.out.println(Integer.toString(m.size()) + " records retrieved");

		return m;

	}

	/**
	 * Helper function for map record query construction using both fielded terms
	 * and unfielded terms
	 * @param mapProjectId the map project id for which queries are retrieved
	 * @param queryStr the string of terms to construct a query for
	 * @return the full lucene query text
	 */
	private String constructMapRecordForMapProjectIdQuery(Long mapProjectId,
		PfsParameter pfsParameter) {

		// //////////////////
		// Basic algorithm:
		// 1) add whitespace breaks to operators
		// 2) split query on whitespace
		// 3) cycle over terms in split query to find quoted material, add each
		// term/quoted term to parsed terms\
		// a) special case: quoted term after a :
		// 3) cycle over terms in parsed terms
		// a) if an operator/parantheses, pass through unchanged
		// b) if a fielded query (i.e. field:value), pass through unchanged
		// c) if not, construct query on all fields with this term

		System.out.println("--------------------------");
		System.out.println("Entering query constructor");
		System.out.println("--------------------------");

		// list of escape terms (i.e. quotes, operators) to be fed into query
		// untouched
		String escapeTerms = "\\+|\\-|\"|\\(|\\)";
		String booleanTerms = "AND|OR|NOT";

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

		System.out.println("After parentheses/quote check: " + queryStr_mod);

		// split the string by white space and single-character operators
		String[] terms = queryStr_mod.split("\\s+");

		System.out.println("Terms after query split:  ");
		for (int i = 0; i < terms.length; i++) {
			System.out.println(terms[i] + ", ");
		}

		// merge items between quotation marks
		boolean exprInQuotes = false;
		List<String> parsedTerms = new ArrayList<String>();
		List<String> parsedTerms_temp = new ArrayList<String>();
		String currentTerm = "";

		// cycle over terms to identify quoted (i.e. non-parsed) terms
		for (int i = 0; i < terms.length; i++) {

			// if an open quote is detected
			if (terms[i].equals("\"")) {

				if (exprInQuotes == true) {
					System.out.println("Close quote detected, exprInQuotes = "
							+ exprInQuotes);
					System.out.println("Adding " + currentTerm);

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
					System.out.println("Open quote detected, exprinQuotes = "
							+ exprInQuotes);
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

		System.out.println("Original query: " + queryStr);
		System.out.println("Terms in modified query:");
		for (String s : parsedTerms) {
			System.out.println("  " + s);
		}

		// begin constructing query
		String full_query = "(";

		// cycle over terms
		for (int i = 0; i < parsedTerms.size(); i++) {

			System.out.println("Next term: " + parsedTerms.get(i));

			// if a boolean or an escape character/sequence, add this term unmodified
			// (with appropriate action
			if (parsedTerms.get(i).matches(escapeTerms)
					|| parsedTerms.get(i).matches(booleanTerms)) {

				full_query += (i == 0 ? "" : " ") + parsedTerms.get(i); // if first
																																// term, do not
																																// add
																																// whitespace
																																// separator
				continue;

				// else if already a field-specific query term
			} else if (parsedTerms.get(i).contains(":")) {
				full_query += parsedTerms.get(i);

				// otherwise, treat as unfielded query term
			} else {
				full_query += "(";

				Iterator<String> names_iter = fieldNames.iterator();

				while (names_iter.hasNext()) {
					full_query += names_iter.next() + ":" + parsedTerms.get(i);
					if (names_iter.hasNext())
						full_query += " OR ";
				}

				full_query += ")";
			}

			// if further terms remain in the sequence
			if (!(i == parsedTerms.size() - 1)) {

				// if next term is not a boolean operator or an escape term, add OR
				if (!parsedTerms.get(i + 1).matches(booleanTerms)
						&& !parsedTerms.get(i + 1).matches(escapeTerms)) {
					full_query += " OR";
				}

			}
		}

		/*
		 * // cycle over terms for (int i = 0; i < terms.length; i++) {
		 * 
		 * // if a boolean operator, add unmodified (TODO update with fuller list
		 * later) if (terms[i].matches("AND|and|OR|or|NOT|not")) { full_query +=
		 * (i==0 ? "" : " ") + terms[i] + " "; continue;
		 * 
		 * // else if already a field-specific query term } else if
		 * (terms[i].contains(":")) { full_query += terms[i];
		 * 
		 * // otherwise, treat as unfielded query term } else { full_query += "(";
		 * 
		 * Iterator<String> names_iter = fieldNames.iterator();
		 * 
		 * while(names_iter.hasNext()) { full_query += names_iter.next() + ":" +
		 * terms[i]; if (names_iter.hasNext()) full_query += " OR "; }
		 * 
		 * full_query += ")"; }
		 * 
		 * // if further terms remain in the sequence, apply boolean separator if
		 * (!(i == terms.length-1)) {
		 * 
		 * // if next term is not a boolean operator, add OR
		 * if(!terms[i+1].matches("AND|and||OR|or|NOT|not")) { full_query += " OR ";
		 * } else { // do nothing, user-specified boolean will be applied above }
		 * 
		 * } }
		 */

		// add mapProjectId constraint
		full_query += ") AND mapProjectId:" + mapProjectId.toString();

		System.out.println("Full query: " + full_query);

		return full_query;

	}

	/**
	 * Helper function for restful services. Constructs a basic concept from
	 * parameters and calls getUnmappedDescentsForConcept(Concept concept, int
	 * thresholdLlc)
	 * 
	 * @param terminologyId the terminology id
	 * @param terminology the terminology
	 * @param terminologyVersion the terminology version
	 * @param thresholdLlc the threshold llc
	 * @return the unmapped descendants for concept
	 * @throws Exception the exception
	 */
	@Override
	public List<Concept> getUnmappedDescendantsForConcept(String terminologyId,
		String terminology, String terminologyVersion, int thresholdLlc)
		throws Exception {
		Concept concept = new ConceptJpa();
		concept.setTerminologyId(terminologyId);
		concept.setTerminology(terminology);
		concept.setTerminologyVersion(terminologyVersion);
		return getUnmappedDescendantsForConcept(concept, thresholdLlc);
	}

	/**
	 * Given a concept, returns a list of descendant concepts that have no
	 * associated map record.
	 * 
	 * @param concept the root concept
	 * @param thresholdLlc the maximum number of descendants a concept can have
	 *          before it is no longer considered a low-level concept (i.e. return
	 *          an empty list)
	 * @return the list of unmapped descendant concepts
	 * @throws Exception the exception
	 */
	public List<Concept> getUnmappedDescendantsForConcept(Concept concept,
		int thresholdLlc) throws Exception {

		// declare results list and content service
		List<Concept> unmappedDescendants = new ArrayList<Concept>();
		ContentService contentService = new ContentServiceJpa();

		// get descendants and construct iterator
		Set<Concept> descendants =
				contentService.getDescendants(concept.getTerminologyId(),
						concept.getTerminology(), concept.getTerminologyVersion(),
						new Long("116680003"));
		Iterator<Concept> descendants_iter = descendants.iterator();

		// if size of descendant set is greater than the low-level concept
		// threshold, skip it
		if (descendants.size() <= thresholdLlc) {

			// cycle over descendants
			while (descendants_iter.hasNext()) {

				Concept descendant = descendants_iter.next();

				// find map records for this id
				List<MapRecord> conceptRecords =
						getMapRecordsForConceptId(descendant.getTerminologyId());

				// if no records found, add to unmapped list
				if (conceptRecords.size() == 0) {
					unmappedDescendants.add(descendant);
				}
			}
		}

		// force-close the manager
		if (contentService != null) {
			contentService.close();
		}

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
	 * org.ihtsdo.otf.mapping.services.MappingService#removeMapPrinciple(java.
	 * lang.Long)
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#removeMapAdvice(java.lang
	 * .Long)
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getMapPrinciple(java.lang
	 * .Long)
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
	@Override
	public List<MapPrinciple> getMapPrinciples() {
		List<MapPrinciple> mapPrinciples = new ArrayList<MapPrinciple>();

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
	@Override
	public List<MapAdvice> getMapAdvices() {
		List<MapAdvice> mapAdvices = new ArrayList<MapAdvice>();

		javax.persistence.Query query =
				manager.createQuery("select m from MapAdviceJpa m");

		// Try query
		mapAdvices = query.getResultList();

		return mapAdvices;
	}

	// ///////////////////////////////////////
	// / Services for Map Project Creation
	// ///////////////////////////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#createMapRecordsForMapProject
	 * (org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public void createMapRecordsForMapProject(MapProject mapProject)
		throws Exception {

		Logger.getLogger(MappingServiceJpa.class).warn(
				"Creating map records for project from " + mapProject.getName());
		if (!getTransactionPerOperation()) {
			throw new IllegalStateException(
					"The application must let the service manage transactions for this method");
		}

		// retrieve all complex map ref set members for mapProject
		javax.persistence.Query query =
				manager
						.createQuery("select r from ComplexMapRefSetMemberJpa r where r.refSetId = :refSetId order by r.concept.id, "
								+ "r.mapBlock, r.mapGroup, r.mapPriority");
		query.setParameter("refSetId", mapProject.getRefSetId());
		Set<ComplexMapRefSetMember> complexMapRefSetMembers =
				new HashSet<ComplexMapRefSetMember>();
		for (Object member : query.getResultList()) {
			ComplexMapRefSetMember refSetMember = (ComplexMapRefSetMember) member;
			complexMapRefSetMembers.add(refSetMember);
		}
		createMapRecordsForMapProject(mapProject, complexMapRefSetMembers);
	}

	// ONLY FOR TESTING PURPOSES
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#removeMapRecordsForProjectId
	 * (java.lang.Long)
	 */
	@Override
	public Long removeMapRecordsForProjectId(Long mapProjectId) {

		EntityTransaction tx = manager.getTransaction();

		int nRecords = 0;
		tx.begin();
		List<MapRecord> records =
				(List<MapRecord>) manager
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
			record.setMapNotes(null); // TODO Check if this is necessary

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
				entry.setMapNotes(null); // TODO Check if this is necessary

				// remove principles
				entry.setMapPrinciples(null);

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

		System.out.println(Integer.toString(nRecords)
				+ " records deleted for map project id = " + mapProjectId.toString());

		return new Long(nRecords);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#createMapRecordsForMapProject
	 * (org.ihtsdo.otf.mapping.model.MapProject, java.util.Set)
	 */
	@Override
	public void createMapRecordsForMapProject(MapProject mapProject,
		Set<ComplexMapRefSetMember> complexMapRefSetMembers) throws Exception {

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

		boolean prevTransactionPerOperationSetting = getTransactionPerOperation();
		setTransactionPerOperation(false);
		beginTransaction();
		try {
			// instantiate other local variables
			Long prevConceptId = new Long(-1);
			MapRecord mapRecord = null;
			int ct = 0;
			for (ComplexMapRefSetMember refSetMember : complexMapRefSetMembers) {

				// Skip concept exclusion rules in all cases
				if (refSetMember.getMapRule().matches("IFA\\s\\d*\\s\\|.*\\s\\|")
						&& !(refSetMember.getMapAdvice()
								.contains("MAP IS CONTEXT DEPENDENT FOR GENDER"))
						&& !(refSetMember.getMapRule()
								.matches("IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>]")))
					continue;

				// retrieve the concept
				Concept concept = refSetMember.getConcept();

				// if no concept for this ref set member, skip
				if (concept == null) {
					throw new NoResultException("Concept is unexpectedly missing for "
							+ refSetMember.getTerminologyId());
				}

				// if different concept than previous ref set member, create new
				// mapRecord
				if (!concept.getTerminologyId().equals(prevConceptId.toString())) {

					mapRecord = new MapRecordJpa();
					mapRecord.setConceptId(concept.getTerminologyId());
					mapRecord.setConceptName(concept.getDefaultPreferredName());
					mapRecord.setMapProjectId(mapProject.getId());

					// get the number of descendants - Need to optimize this
					// Need a tool to compute and save this for LLCs (e.g. having < 11 descendants)
					mapRecord.setCountDescendantConcepts(new Long(contentService
					  .getDescendants(concept.getTerminologyId(),
					  concept.getTerminology(), concept.getTerminologyVersion(), new
					  Long("116680003")).size()));
					 
					//mapRecord.setCountDescendantConcepts(0L);

					// set the previous concept to this concept
					prevConceptId =
							new Long(refSetMember.getConcept().getTerminologyId());

					// persist the record
					addMapRecord(mapRecord);

					if (++ct % 500 == 0) {
						Logger.getLogger(MappingServiceJpa.class).info(
								ct + " records created");
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
						targetName = "Target name cound not be determined";
					} else {
						targetName = c.getDefaultPreferredName();
					}
				}

				// Set map relation id as well from the cache
				String relationName = null;
				if (refSetMember.getMapRelationId() != null)
					relationName = relationIdNameMap.get(refSetMember.getMapRelationId());

				MapEntry mapEntry = new MapEntryJpa();
				mapEntry.setTargetId(refSetMember.getMapTarget());
				mapEntry.setTargetName(targetName);
				mapEntry.setMapRecord(mapRecord);
				mapEntry.setRelationId(refSetMember.getMapRelationId().toString());
				mapEntry.setRelationName(relationName);
				mapEntry.setRule(refSetMember.getMapRule());
				mapEntry.setMapGroup(refSetMember.getMapBlock());
				mapEntry.setMapBlock(refSetMember.getMapGroup());

				mapRecord.addMapEntry(mapEntry);

				// Add support for advices - and there can be multiple map advice values
				// Only add advice if it is an allowable value and doesn't match relation name
				// This should automatically exclude IFA/ALWAYS advice 
				if (refSetMember.getMapAdvice() != null
						&& !refSetMember.getMapAdvice().equals("")) {
					List<MapAdvice> mapAdvices = getMapAdvices();
					for (MapAdvice ma : mapAdvices) {
						if (refSetMember.getMapAdvice().indexOf(ma.getName()) != -1
								&& !ma.getName().equals(relationName)) {
							mapEntry.addMapAdvice(ma);
						}
					}
				}
			}
			
			commit();
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

}
