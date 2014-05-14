package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Version;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.MapAdviceList;
import org.ihtsdo.otf.mapping.helpers.MapAdviceListJpa;
import org.ihtsdo.otf.mapping.helpers.MapPrincipleList;
import org.ihtsdo.otf.mapping.helpers.MapPrincipleListJpa;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.helpers.MapProjectListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRecordList;
import org.ihtsdo.otf.mapping.helpers.MapRecordListJpa;
import org.ihtsdo.otf.mapping.helpers.MapRelationList;
import org.ihtsdo.otf.mapping.helpers.MapRelationListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserPreferencesList;
import org.ihtsdo.otf.mapping.helpers.MapUserPreferencesListJpa;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.helpers.TreePositionListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapAgeRangeJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapAgeRange;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapPrinciple;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * JPA implementation of the {@link MappingService}.
 */
public class MappingServiceJpa extends RootServiceJpa implements MappingService {

	/** The manager. */
	private EntityManager manager;

	/** The transaction per operation. */
	private boolean transactionPerOperation = true;

	/** The transaction entity. */
	private EntityTransaction tx;

	/**
	 * Instantiates an empty {@link MappingServiceJpa}.
	 */
	public MappingServiceJpa() {
	  super();
		// created on each instantiation
		manager = factory.createEntityManager();
	}

	/**
	 * Close the manager when done with this service.
	 * 
	 * @throws Exception
	 *             the exception
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
	 * @param id
	 *            the auto-generated id
	 * @return the MapProject
	 */
	@Override
	public MapProject getMapProject(Long id) {

		MapProject m = null;

		javax.persistence.Query query = manager
				.createQuery("select m from MapProjectJpa m where id = :id");
		query.setParameter("id", id);

		try {
			m = (MapProject) query.getSingleResult();
			m.getScopeConcepts().size();
			m.getScopeExcludedConcepts().size();
			m.getMapAdvices().size();
			m.getMapRelations().size();
			m.getMapLeads().size();
			m.getMapSpecialists().size();
			m.getMapPrinciples().size();
			m.getPresetAgeRanges().size();
			return m;

		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map project query for id = " + id
							+ " returned no results!");
			return null;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getMapProjectByName(java
	 * .lang.String)
	 */
	@Override
	public MapProject getMapProjectByName(String name) {

		MapProject m = null;

		javax.persistence.Query query = manager.createQuery(
				"select m from MapProjectJpa m where name = :name")
				.setParameter("name", name);

		try {
			m = (MapProject) query.getSingleResult();
			m.getScopeConcepts().size();
			m.getScopeExcludedConcepts().size();
			m.getMapAdvices().size();
			m.getMapRelations().size();
			m.getMapLeads().size();
			m.getMapSpecialists().size();
			m.getMapPrinciples().size();
			m.getPresetAgeRanges().size();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map project query for name = " + name
							+ " returned no results!");
			return null;
		}
		return m;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getMapProjectByRefSetId
	 * (java.lang.String)
	 */
	@Override
	public MapProject getMapProjectByRefSetId(String refSetId) {

		MapProject m = null;

		javax.persistence.Query query = manager.createQuery(
				"select m from MapProjectJpa m where refSetId = :refSetId")
				.setParameter("refSetId", refSetId);

		try {
			m = (MapProject) query.getSingleResult();
			m.getScopeConcepts().size();
			m.getScopeExcludedConcepts().size();
			m.getMapAdvices().size();
			m.getMapRelations().size();
			m.getMapLeads().size();
			m.getMapSpecialists().size();
			m.getMapPrinciples().size();
			m.getPresetAgeRanges().size();
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
	public MapProjectList getMapProjects() {

		List<MapProject> mapProjects = null;

		// construct query
		javax.persistence.Query query = manager
				.createQuery("select m from MapProjectJpa m");

		mapProjects = query.getResultList();

		// force instantiation of lazy collections
		for (MapProject project : mapProjects) {
			project.getScopeConcepts().size();
			project.getScopeExcludedConcepts().size();
			project.getMapAdvices().size();
			project.getMapRelations().size();
			project.getMapLeads().size();
			project.getMapSpecialists().size();
			project.getMapPrinciples().size();
			project.getPresetAgeRanges().size();
		}

		MapProjectListJpa mapProjectList = new MapProjectListJpa();
		mapProjectList.setMapProjects(mapProjects);
		mapProjectList.setTotalCount(mapProjects.size());
		return mapProjectList;
	}

	/**
	 * Query for MapProjects.
	 * 
	 * @param query
	 *            the query
	 * @param pfsParameter
	 *            the pfs parameter
	 * @return the list of MapProject
	 * @throws Exception
	 *             the exception
	 */
	@Override
	@SuppressWarnings("unchecked")
	public SearchResultList findMapProjects(String query,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList s = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct luceneQuery based on URL format
		if (query.indexOf(':') == -1) { // no fields indicated
			MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
					Version.LUCENE_36, fieldNames.toArray(new String[0]),
					searchFactory.getAnalyzer(MapProjectJpa.class));
			queryParser.setAllowLeadingWildcard(false);
			luceneQuery = queryParser.parse(query);

		} else { // field:value
			QueryParser queryParser = new QueryParser(Version.LUCENE_36,
					"summary", searchFactory.getAnalyzer(MapProjectJpa.class));
			luceneQuery = queryParser.parse(query);
		}

		List<MapProject> m = fullTextEntityManager.createFullTextQuery(
				luceneQuery, MapProjectJpa.class).getResultList();

		Logger.getLogger(this.getClass()).debug(
				Integer.toString(m.size()) + " map projects retrieved");

		for (MapProject mp : m) {
			s.addSearchResult(new SearchResultJpa(mp.getId(), mp.getRefSetId()
					.toString(), mp.getName()));
		}

		// Sort by ID
		s.sortBy(new Comparator<SearchResult>() {
			@Override
			public int compare(SearchResult o1, SearchResult o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});

		fullTextEntityManager.close();

		// closing fullTextEntityManager also closes manager, recreate
		manager = factory.createEntityManager();

		return s;
	}

	/**
	 * Add a map project.
	 * 
	 * @param mapProject
	 *            the map project
	 * @return the map project
	 */
	@Override
	public MapProject addMapProject(MapProject mapProject) {

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
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
	 * @param mapProject
	 *            the changed map project
	 */
	@Override
	public void updateMapProject(MapProject mapProject) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
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
	 * @param mapProjectId
	 *            the map project to be removed
	 */
	@Override
	public void removeMapProject(Long mapProjectId) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
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
	public MapUserList getMapUsers() {

		List<MapUser> m = null;

		javax.persistence.Query query = manager
				.createQuery("select m from MapUserJpa m");

		m = query.getResultList();
		MapUserListJpa mapUserList = new MapUserListJpa();
		mapUserList.setMapUsers(m);
		mapUserList.setTotalCount(m.size());
		return mapUserList;
	}

	/**
	 * Return map specialist for auto-generated id.
	 * 
	 * @param id
	 *            the auto-generated id
	 * @return the MapSpecialist
	 */
	@Override
	public MapUser getMapUser(Long id) {

		MapUser m = null;

		javax.persistence.Query query = manager
				.createQuery("select m from MapUserJpa m where id = :id");
		query.setParameter("id", id);
		try {
			m = (MapUser) query.getSingleResult();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map specialist query for id = " + id
							+ " returned no results!");
			return null;
		}

		return m;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getMapUser(java.lang.String
	 * )
	 */
	@Override
	public MapUser getMapUser(String userName) {

		MapUser m = null;

		javax.persistence.Query query = manager
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
	 * @param mapUser
	 *            the map user
	 * @return a List of MapProjects
	 */
	@Override
	public MapProjectList getMapProjectsForMapUser(MapUser mapUser) {

		MapProjectList mpList = getMapProjects();
		List<MapProject> mpListReturn = new ArrayList<>();

		// iterate and check for presence of mapUser as specialist
		for (MapProject mp : mpList.getMapProjects()) {

			for (MapUser ms : mp.getMapSpecialists()) {
				if (ms.equals(mapUser)) {
					mpListReturn.add(mp);
				}
			}

			for (MapUser ms : mp.getMapLeads()) {
				if (ms.equals(mapUser)) {
					mpListReturn.add(mp);
				}
			}
		}

		// force instantiation of lazy collections
		for (MapProject mp : mpListReturn) {
			mp.getScopeConcepts().size();
			mp.getScopeExcludedConcepts().size();
			mp.getMapAdvices().size();
			mp.getMapRelations().size();
			mp.getMapLeads().size();
			mp.getMapSpecialists().size();
			mp.getMapPrinciples().size();
			mp.getPresetAgeRanges().size();
		}

		MapProjectListJpa mapProjectList = new MapProjectListJpa();
		mapProjectList.setMapProjects(mpListReturn);
		mapProjectList.setTotalCount(mpListReturn.size());
		return mapProjectList;
	}

	/**
	 * Update a map specialist.
	 * 
	 * @param mapUser
	 *            the changed map user
	 */
	@Override
	public void updateMapUser(MapUser mapUser) {

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
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
	 * @param mapUserId
	 *            the map user to be removed
	 */
	@Override
	public void removeMapUser(Long mapUserId) {

		tx = manager.getTransaction();

		// retrieve this map specialist
		MapUser mu = manager.find(MapUserJpa.class, mapUserId);

		// retrieve all projects on which this specialist appears
		List<MapProject> projects = getMapProjectsForMapUser(mu)
				.getMapProjects();

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
	 * @param mapUser
	 *            the map lead
	 * @return the map user
	 */
	@Override
	public MapUser addMapUser(MapUser mapUser) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
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
	public MapRecordList getMapRecords() {
		List<MapRecord> mapRecords = null;
		// construct query
		javax.persistence.Query query = manager
				.createQuery("select m from MapRecordJpa m");
		// Try query
		mapRecords = query.getResultList();
		MapRecordListJpa mapRecordList = new MapRecordListJpa();
		mapRecordList.setMapRecords(mapRecords);
		mapRecordList.setTotalCount(mapRecords.size());

		return mapRecordList;
	}

	/**
	 * Retrieve map record for given id.
	 * 
	 * @param id
	 *            the map record id
	 * @return the map record
	 */
	@Override
	public MapRecord getMapRecord(Long id) {

		javax.persistence.Query query = manager
				.createQuery("select r from MapRecordJpa r where id = :id");

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
					"MapRecord query for id = " + id
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
	 * @param query
	 *            the lucene query string
	 * @param pfsParameter
	 *            the pfs parameter
	 * @return a list of map records
	 * @throws Exception
	 *             the exception
	 */
	@Override
	@SuppressWarnings("unchecked")
	public SearchResultList findMapRecords(String query,
			PfsParameter pfsParameter) throws Exception {

		SearchResultList s = new SearchResultListJpa();

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct luceneQuery based on URL format
		if (query.indexOf(':') == -1) { // no fields indicated
			MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
					Version.LUCENE_36, fieldNames.toArray(new String[0]),
					searchFactory.getAnalyzer(MapRecordJpa.class));
			queryParser.setAllowLeadingWildcard(false);
			luceneQuery = queryParser.parse(query);

		} else { // field:value
			QueryParser queryParser = new QueryParser(Version.LUCENE_36,
					"summary", searchFactory.getAnalyzer(MapRecordJpa.class));
			luceneQuery = queryParser.parse(query);
		}

		List<MapRecord> m = fullTextEntityManager.createFullTextQuery(
				luceneQuery, MapRecordJpa.class).getResultList();

		Logger.getLogger(this.getClass()).debug(
				Integer.toString(m.size()) + " map records retrieved");

		for (MapRecord mp : m) {
			s.addSearchResult(new SearchResultJpa(mp.getId(), mp.getConceptId()
					.toString(), mp.getConceptName()));
		}

		// Sort by ID
		s.sortBy(new Comparator<SearchResult>() {
			@Override
			public int compare(SearchResult o1, SearchResult o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});

		fullTextEntityManager.close();

		// closing fullTextEntityManager also closes manager, recreate
		manager = factory.createEntityManager();

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
	 * @param mapRecord
	 *            the map record to be added
	 * @return the map record
	 * @throws Exception
	 *             the exception
	 */
	@Override
	public MapRecord addMapRecord(MapRecord mapRecord) throws Exception {

		// check if user valid
		if (mapRecord.getOwner() == null) {
			throw new Exception("Map Record requires valid user in owner field");
		}

		// check if last modified by user valid
		if (mapRecord.getLastModifiedBy() == null) {
			throw new Exception(
					"Map Record requires valid user in lastModifiedBy field");
		}

		// set the map record of all elements of this record
		mapRecord.assignToChildren();

		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();

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
	 * @param mapRecord
	 *            the map record to be updated
	 */
	@Override
	public void updateMapRecord(MapRecord mapRecord) {

		// update timestamp
		mapRecord.setLastModified((new java.util.Date()).getTime());

		// first assign the map record to its children
		mapRecord.assignToChildren();

		if (getTransactionPerOperation()) {

			tx = manager.getTransaction();
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
	 * @param id
	 *            the id of the map record to be removed
	 */
	@Override
	public void removeMapRecord(Long id) {

		tx = manager.getTransaction();

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

	/**
	 * Takes a map entry and computes any auto-generated advice for its entries
	 */
	@Override
	public MapRelation computeMapRelation(MapEntry mapEntry)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {

		MapRecord mapRecord = getMapRecord(mapEntry.getMapRecord().getId());

		MapProject mapProject = getMapProject(mapRecord.getMapProjectId());
		ProjectSpecificAlgorithmHandler algorithmHandler = getProjectSpecificAlgorithmHandler(mapProject);

		return algorithmHandler.computeMapRelation(mapRecord, mapEntry);
	}

	/**
	 * Takes a map record and computes any auto-generated advice for its entries
	 * 
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@Override
	public MapAdviceList computeMapAdvice(MapEntry mapEntry)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException {

		MapRecord mapRecord = mapEntry.getMapRecord();

		ProjectSpecificAlgorithmHandler algorithmHandler = getProjectSpecificAlgorithmHandler(getMapProject(mapRecord
				.getMapProjectId()));

		MapAdviceListJpa mapAdviceList = new MapAdviceListJpa();
		mapAdviceList.setMapAdvices(algorithmHandler.computeMapAdvice(
				mapRecord, mapEntry));
		mapAdviceList.setTotalCount(mapAdviceList.getMapAdvices().size());
		return mapAdviceList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getMapRecordRevisions(
	 * java.lang.Long)
	 */
	@Override
	public MapRecordList getMapRecordRevisions(Long mapRecordId) {

		AuditReader reader = AuditReaderFactory.get(manager);
		@SuppressWarnings("unchecked")
		List<MapRecord> revisions = reader.createQuery()
				.forRevisionsOfEntity(MapRecordJpa.class, false, false)
				.getResultList();
		MapRecordListJpa mapRecordList = new MapRecordListJpa();
		mapRecordList.setMapRecords(revisions);
		mapRecordList.setTotalCount(revisions.size());
		return mapRecordList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getRecentlyEditedMapRecords
	 * (org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public MapRecordList getRecentlyEditedMapRecords(Long projectId,
			String userName, PfsParameter pfsParameter) throws Exception {

		MapUser user = getMapUser(userName);

		AuditReader reader = AuditReaderFactory.get(manager);

		// construct the query
		AuditQuery query = reader
				.createQuery()

				// all revisions, returned as objects, finding deleted entries
				.forRevisionsOfEntity(MapRecordJpa.class, true, true)

				// add mapProjectId and owner as constraints
				.add(AuditEntity.property("mapProjectId").eq(projectId))
				.add(AuditEntity.relatedId("owner").eq(user.getId()))

				// exclude records with workflow status NEW
				.add(AuditEntity.property("workflowStatus").ne(
						WorkflowStatus.NEW))

				// sort by last modified (descending)
				.addOrder(AuditEntity.property("lastModified").desc())

				.setFirstResult(pfsParameter.getStartIndex())
				.setMaxResults(pfsParameter.getMaxResults());

		// execute the query
		List<MapRecord> editedRecords = query.getResultList();

		// create the mapRecordList and set total size
		MapRecordListJpa mapRecordList = new MapRecordListJpa();
		// mapRecordList.setTotalCount(editedRecords.size());

		// handle all lazy initializations
		for (MapRecord mapRecord : editedRecords) {
			mapRecord.getOwner().getEmail();
			mapRecord.getLastModifiedBy().getEmail();
			for (MapEntry mapEntry : mapRecord.getMapEntries()) {
				mapEntry.getMapNotes().size();
				mapEntry.getMapAdvices().size();
			}
		}

		// create the mapRecordList
		mapRecordList.setMapRecords(editedRecords);
		return mapRecordList;
	}

	// //////////////////////////////////
	// Other query services
	// //////////////////////////////////

	// //////////////////////////////////////////////
	// Descendant services
	// //////////////////////////////////////////////

	/**
	 * Retrieve map records for a given terminology id.
	 * 
	 * @param terminologyId
	 *            the concept id
	 * @return the list of map records
	 */
	@SuppressWarnings("unchecked")
	@Override
	public MapRecordList getMapRecordsForConcept(String terminologyId) {
		List<MapRecord> mapRecords = null;

		// construct query
		javax.persistence.Query query = manager
				.createQuery("select m from MapRecordJpa m where conceptId = :conceptId");

		// Try query
		query.setParameter("conceptId", terminologyId);
		mapRecords = query.getResultList();

		MapRecordListJpa mapRecordList = new MapRecordListJpa();
		mapRecordList.setMapRecords(mapRecords);
		mapRecordList.setTotalCount(mapRecords.size());
		return mapRecordList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getMapRecordsForConcept
	 * (java.lang.Long)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public MapRecordList getMapRecordsForConcept(Long conceptId) {

		// call retrieval function with concept
		Concept concept = manager.find(ConceptJpa.class, conceptId);
		List<MapRecord> results = null;
		// find maprecords where:
		// (1) the conceptId matches the concept terminologyId
		// (2) the concept terminology matches the source terminology for the
		// mapRecord's project
		// Try query
		try {
			javax.persistence.Query query = manager.createQuery(
					"select mr from MapRecordJpa mr "
							+ "where conceptId = :conceptId ").setParameter(
					"conceptId", concept.getTerminologyId());
			results = query.getResultList();
		} catch (Exception e) {
			Logger.getLogger(this.getClass()).warn(
					"Map records for concept " + concept.getTerminologyId()
							+ " returned no map record results!");
			return null;
		}
		// return results
		MapRecordListJpa mapRecordList = new MapRecordListJpa();
		mapRecordList.setMapRecords(results);
		mapRecordList.setTotalCount(results.size());
		return mapRecordList;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getMapRecordsForMapProject
	 * (java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public MapRecordList getMapRecordsForMapProject(Long mapProjectId)
			throws Exception {

		javax.persistence.Query query = manager
				.createQuery(
						"select m from MapRecordJpa m where mapProjectId = :mapProjectId")
				.setParameter("mapProjectId", mapProjectId);
		MapRecordList mapRecordList = new MapRecordListJpa();
		mapRecordList.setMapRecords(query.getResultList());
		return mapRecordList;
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
	public MapRecordList getPublishedAndReadyForPublicationMapRecordsForMapProject(Long mapProjectId,
			PfsParameter pfsParameter) throws Exception {

		// construct basic query
		String full_query = constructMapRecordForMapProjectIdQuery(
				mapProjectId, pfsParameter == null ? new PfsParameterJpa()
						: pfsParameter);
		
		full_query +=
		  " AND (workflowStatus:'PUBLISHED' OR workflowStatus:'READY_FOR_PUBLICATION')";
		 
		Logger.getLogger(MappingServiceJpa.class).info(full_query);

		FullTextEntityManager fullTextEntityManager = Search
				.getFullTextEntityManager(manager);

		SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
		Query luceneQuery;

		// construct luceneQuery based on URL format

		QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
				searchFactory.getAnalyzer(MapRecordJpa.class));
		luceneQuery = queryParser.parse(full_query);

		org.hibernate.search.jpa.FullTextQuery ftquery = fullTextEntityManager
				.createFullTextQuery(luceneQuery, MapRecordJpa.class);

		// if a sort field has been specified, add sort parameter
		if (pfsParameter != null && pfsParameter.getSortField() != null) {
				ftquery.setSort(new Sort(new SortField(pfsParameter
						.getSortField(), SortField.STRING)));
		}
		
		// get the results
		List<MapRecord> m = ftquery.getResultList();

		Logger.getLogger(this.getClass()).debug(
				Integer.toString(m.size()) + " records retrieved");

		// set the total count
		MapRecordListJpa mapRecordList = new MapRecordListJpa();
		mapRecordList.setTotalCount(m.size());
		
		int fromIndex = 0;
		int toIndex = m.size();
		
		if (pfsParameter != null) {
			if (pfsParameter.getStartIndex() != -1) fromIndex = pfsParameter.getStartIndex();
			if (pfsParameter.getMaxResults() != -1) toIndex = Math.min(m.size(), fromIndex + pfsParameter.getMaxResults());
		}
		
		// extract the required sublist of map records
		mapRecordList.setMapRecords(m.subList(fromIndex, toIndex));
		
		return mapRecordList;

	}

	/**
	 * Helper function for map record query construction using both fielded
	 * terms and unfielded terms.
	 * 
	 * @param mapProjectId
	 *            the map project id for which queries are retrieved
	 * @param pfsParameter
	 *            the pfs parameter
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

		// Pre-treatment: Find any lower-case boolean operators and set to
		// uppercase

		// //////////////////
		// Basic algorithm:
		//
		// 1) add whitespace breaks to operators
		// 2) split query on whitespace
		// 3) cycle over terms in split query to find quoted material, add each
		// term/quoted term to parsed terms\
		// a) special case: quoted term after a :
		// 3) cycle over terms in parsed terms
		// a) if an operator/parantheses, pass through unchanged (send to upper
		// case
		// for boolean)
		// b) if a fielded query (i.e. field:value), pass through unchanged
		// c) if not, construct query on all fields with this term

		// list of escape terms (i.e. quotes, operators) to be fed into query
		// untouched
		String escapeTerms = "\\+|\\-|\"|\\(|\\)";
		String booleanTerms = "and|AND|or|OR|not|NOT";

		// first cycle over the string to add artificial breaks before and after
		// control characters
		final String queryStr = (pfsParameter == null ? "" : pfsParameter
				.getFilterString());

		String queryStr_mod = queryStr;
		queryStr_mod = queryStr_mod.replace("(", " ( ");
		queryStr_mod = queryStr_mod.replace(")", " ) ");
		queryStr_mod = queryStr_mod.replace("\"", " \" ");
		queryStr_mod = queryStr_mod.replace("+", " + ");
		queryStr_mod = queryStr_mod.replace("-", " - ");

		// remove any leading or trailing whitespace (otherwise first/last null
		// term
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

					// special case check: fielded term. Impossible for first
					// term to be
					// fielded.
					if (parsedTerms.size() == 0) {
						parsedTerms.add("\"" + currentTerm + "\"");
					} else {
						String lastParsedTerm = parsedTerms.get(parsedTerms
								.size() - 1);

						// if last parsed term ended with a colon, append this
						// term to the
						// last parsed term
						if (lastParsedTerm.endsWith(":") == true) {
							parsedTerms.set(parsedTerms.size() - 1,
									lastParsedTerm + "\"" + currentTerm + "\"");
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
					currentTerm = currentTerm == "" ? terms[i] : currentTerm
							+ " " + terms[i];

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
			 * full_query += (i == 0 ? // check for first term "" : // -> if
			 * first character, add nothing
			 * parsedTerms.get(i-1).matches(escapeTerms) ? // check if last term
			 * was an escape character "": // -> if last term was an escape
			 * character, add nothing " "); // -> otherwise, add a separating
			 * space
			 */

			// if an escape character/sequence, add this term unmodified
			if (parsedTerms.get(i).matches(escapeTerms)) {

				full_query += parsedTerms.get(i);

				// else if a boolean character, add this term in upper-case form
				// (i.e.
				// lucene format)
			} else if (parsedTerms.get(i).matches(booleanTerms)) {

				full_query += parsedTerms.get(i).toUpperCase();

				// else if already a field-specific query term, add this term
				// unmodified
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
		full_query = "(" + full_query + ")" + " AND mapProjectId:"
				+ mapProjectId;

		Logger.getLogger(MappingServiceJpa.class).debug(
				"Full query: " + full_query);

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
		Logger.getLogger(this.getClass()).info(
				"Find concepts in scope for " + mapProjectId);

		MapProject project = getMapProject(mapProjectId);
		SearchResultList conceptsInScope = new SearchResultListJpa();

		ContentService contentService = new ContentServiceJpa();

		String terminology = project.getSourceTerminology();
		String terminologyVersion = project.getSourceTerminologyVersion();

		// Avoid including the scope concepts themselves in the definition
		// if we are looking for descendants
		// e.g. "Clinical Finding" does not need to be mapped for SNOMED->ICD10
		if (!project.isScopeDescendantsFlag()) {
			Logger.getLogger(this.getClass()).info(
					"  Project not using scope descendants flag - "
							+ project.getScopeConcepts());
			for (String conceptId : project.getScopeConcepts()) {
				Concept c = contentService.getConcept(conceptId, terminology,
						terminologyVersion);
				if (c == null) {
					throw new Exception(
							"Scope concept " + conceptId + " does not exist.");
				}
				SearchResult sr = new SearchResultJpa();
				sr.setId(c.getId());
				sr.setTerminologyId(c.getTerminologyId());
				sr.setTerminology(c.getTerminology());
				sr.setTerminologyVersion(c.getTerminologyVersion());
				sr.setValue(c.getDefaultPreferredName());
				conceptsInScope.addSearchResult(sr);
			}
		}

		// Include descendants in scope.
		if (project.isScopeDescendantsFlag()) {
			Logger.getLogger(this.getClass()).info(
					"  Project using scope descendants flag");
			// for each scope concept, get descendants
			for (String terminologyId : project.getScopeConcepts()) {
				SearchResultList descendants = contentService
						.findDescendantsFromTreePostions(terminologyId,
								terminology, terminologyVersion);

				Logger.getLogger(this.getClass()).info(
						"    Concept " + terminologyId + " has "
								+ descendants.getTotalCount() + " descendants");
				// cycle over descendants
				for (SearchResult sr : descendants.getSearchResults()) {
					conceptsInScope.addSearchResult(sr);
				}
			}
		}

		contentService.close();
		// get those excluded from scope
		SearchResultList excludedResultList = findConceptsExcludedFromScope(mapProjectId);

		// remove those excluded from scope
		SearchResultList finalConceptsInScope = new SearchResultListJpa();
		for (SearchResult sr : conceptsInScope.getSearchResults()) {
			if (!excludedResultList.contains(sr)) {
				finalConceptsInScope.addSearchResult(sr);
			}
		}

		finalConceptsInScope.setTotalCount(finalConceptsInScope.getCount());
		Logger.getLogger(this.getClass()).info(
				"Finished getting scope concepts - "
						+ finalConceptsInScope.getTotalCount());

		/**
		 * PrintWriter writer = new PrintWriter("C:/data/inScopeConcepts.txt",
		 * "UTF-8"); for(SearchResult sr :
		 * finalConceptsInScope.getSearchResults()) {
		 * writer.println(sr.getTerminologyId()); } writer.close();
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
		Logger.getLogger(this.getClass()).info(
				"Find unmapped concepts in scope for " + mapProjectId);
		// Get in scope concepts
		SearchResultList conceptsInScope = findConceptsInScope(mapProjectId);
		Logger.getLogger(this.getClass()).info(
				"  Project has " + conceptsInScope.getTotalCount()
						+ " concepts in scope");

		//
		// Look for concept ids that have publication records in the current
		// project
		//
		Set<String> mappedConcepts = new HashSet<>();
		for (MapRecord mapRecord : getMapRecordsForMapProject(mapProjectId)
				.getIterable()) {
			if (mapRecord.getWorkflowStatus().equals(WorkflowStatus.PUBLISHED)
					|| mapRecord.getWorkflowStatus().equals(
							WorkflowStatus.READY_FOR_PUBLICATION)) {
				mappedConcepts.add(mapRecord.getConceptId());
			}
		}

		//
		// Keep any search results that do not have mapped concepts
		//
		ContentService contentService = new ContentServiceJpa();
		SearchResultList unmappedConceptsInScope = new SearchResultListJpa();
		for (SearchResult sr : conceptsInScope.getSearchResults()) {

			// Check that the concept has an effectiveTime >= 20140131
			Calendar c = Calendar.getInstance();
			c.set(2013, 0, 1);
			Concept concept = contentService.getConcept(sr.getId());
			if (concept.getEffectiveTime().after(c.getTime())) {

				if (!mappedConcepts.contains(sr.getTerminologyId())) {
					unmappedConceptsInScope.addSearchResult(sr);
				}

			}
		}
		unmappedConceptsInScope.setTotalCount(unmappedConceptsInScope
				.getCount());
		contentService.close();

		Logger.getLogger(this.getClass()).info(
				"  Project has " + unmappedConceptsInScope.getTotalCount()
						+ " unmapped concepts in scope");

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
		List<MapRecord> mapRecordList = getMapRecordsForMapProject(mapProjectId)
				.getMapRecords();
		ContentService contentService = new ContentServiceJpa();

		for (MapRecord record : mapRecordList) {
			Concept c = contentService.getConcept(record.getConceptId(),
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
			Concept c = contentService.getConcept(conceptId, terminology,
					terminologyVersion);
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
				SearchResultList descendants = contentService
						.findDescendantsFromTreePostions(terminologyId,
								terminology, terminologyVersion);

				// cycle over descendants
				for (SearchResult sr : descendants.getSearchResults()) {
					conceptsExcludedFromScope.addSearchResult(sr);
				}
			}

		}

		contentService.close();
		conceptsExcludedFromScope.setTotalCount(conceptsExcludedFromScope
				.getCount());
		Logger.getLogger(this.getClass()).info(
				"Concepts excluded from scope "
						+ +conceptsExcludedFromScope.getTotalCount());
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
		if (!project.isScopeDescendantsFlag()
				&& !project.isScopeDescendantsFlag())
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
			if (project.isScopeDescendantsFlag()
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
	 * org.ihtsdo.otf.mapping.services.MappingService#isConceptExcludedFromScope
	 * (org.ihtsdo.otf.mapping.rf2.Concept,
	 * org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public boolean isConceptExcludedFromScope(String conceptId,
			Long mapProjectId) throws Exception {
		MapProject project = getMapProject(mapProjectId);
		// if directly matches preset scope concept return true
		for (String c : project.getScopeExcludedConcepts()) {
			if (c.equals(conceptId))
				return true;
		}
		// don't make contentService if no chance descendants meet conditions
		if (!project.isScopeDescendantsFlag()
				&& !project.isScopeDescendantsFlag())
			return false;

		ContentService contentService = new ContentServiceJpa();
		for (SearchResult tp : contentService.findTreePositionsForConcept(
				conceptId, project.getSourceTerminology(),
				project.getSourceTerminologyVersion()).getSearchResults()) {
			String ancestorPath = tp.getValue();
			if (project.isScopeDescendantsFlag()
					&& ancestorPath.contains(conceptId)) {
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
			if (project.isScopeDescendantsFlag()
					&& ancestorPath.contains(conceptId)) {
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
	 * @param terminologyId
	 *            the terminology id
	 * @param terminology
	 *            the terminology
	 * @param terminologyVersion
	 *            the terminology version
	 * @param thresholdLlc
	 *            the maximum number of descendants a concept can have before it
	 *            is no longer considered a low-level concept (i.e. return an
	 *            empty list)
	 * @return the list of unmapped descendant concepts
	 * @throws Exception
	 *             the exception
	 */
	@Override
	public SearchResultList findUnmappedDescendantsForConcept(
			String terminologyId, String terminology,
			String terminologyVersion, int thresholdLlc) throws Exception {

		SearchResultList unmappedDescendants = new SearchResultListJpa();

		// get hierarchical rel
		MetadataService metadataService = new MetadataServiceJpa();
		Map<String, String> hierarchicalRelationshipTypeMap = metadataService
				.getHierarchicalRelationshipTypes(terminology,
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
		// ASSUMPTION: only a single "isa" type
		String hierarchicalRelationshipType = hierarchicalRelationshipTypeMap
				.entrySet().iterator().next().getKey();

		// get descendants -- no pfsParameter, want all results
		ContentService contentService = new ContentServiceJpa();
		SearchResultList descendants = contentService.findDescendants(
				terminologyId, terminology, terminologyVersion,
				hierarchicalRelationshipType, null);

		// if number of descendants <= low-level concept threshold, treat as
		// high-level concept and report no unmapped
		if (descendants.getCount() <= thresholdLlc) {

			// cycle over descendants
			for (SearchResult sr : descendants.getSearchResults()) {

				// if descendant has no associated map records, add to list
				if (getMapRecordsForConcept(sr.getTerminologyId())
						.getTotalCount() == 0) {
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
			tx = manager.getTransaction();

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
			tx = manager.getTransaction();

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
			tx = manager.getTransaction();

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
			tx = manager.getTransaction();

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
			tx = manager.getTransaction();

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
	 * org.ihtsdo.otf.mapping.services.MappingService#updateMapPrinciple(org
	 * .ihtsdo .otf.mapping.model.MapPrinciple)
	 */
	@Override
	public void updateMapPrinciple(MapPrinciple mapPrinciple) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();

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

			tx = manager.getTransaction();

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
	 * org.ihtsdo.otf.mapping.services.MappingService#updateMapRelation(org.
	 * ihtsdo .otf.mapping.model.MapRelation)
	 */
	@Override
	public void updateMapRelation(MapRelation mapRelation) {
		if (getTransactionPerOperation()) {

			tx = manager.getTransaction();

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
			tx = manager.getTransaction();
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
	 * org.ihtsdo.otf.mapping.services.MappingService#removeMapPrinciple(java
	 * .lang.Long)
	 */
	@Override
	public void removeMapPrinciple(Long mapPrincipleId) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			MapPrinciple mp = manager.find(MapPrincipleJpa.class,
					mapPrincipleId);
			if (manager.contains(mp)) {
				manager.remove(mp);
			} else {
				manager.remove(manager.merge(mp));
			}
			tx.commit();
		} else {
			MapPrinciple mp = manager.find(MapPrincipleJpa.class,
					mapPrincipleId);
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
			tx = manager.getTransaction();
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
	 * org.ihtsdo.otf.mapping.services.MappingService#removeMapRelation(java
	 * .lang.Long)
	 */
	@Override
	public void removeMapRelation(Long mapRelationId) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
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
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#getMapPrinciple(java.lang
	 * .Long)
	 */
	@Override
	public MapPrinciple getMapPrinciple(Long id) {

		MapPrinciple m = null;

		javax.persistence.Query query = manager
				.createQuery("select m from MapPrincipleJpa m where id = :id");
		query.setParameter("id", id);
		try {
			m = (MapPrinciple) query.getSingleResult();
		} catch (NoResultException e) {
			Logger.getLogger(this.getClass()).warn(
					"Map principle query for id = " + id
							+ " returned no results!");
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
	public MapPrincipleList getMapPrinciples() {
		List<MapPrinciple> mapPrinciples = new ArrayList<>();

		javax.persistence.Query query = manager
				.createQuery("select m from MapPrincipleJpa m");

		// Try query
		mapPrinciples = query.getResultList();

		MapPrincipleListJpa mapPrincipleList = new MapPrincipleListJpa();
		mapPrincipleList.setMapPrinciples(mapPrinciples);
		mapPrincipleList.setTotalCount(mapPrinciples.size());
		return mapPrincipleList;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapAdvices()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public MapAdviceList getMapAdvices() {
		List<MapAdvice> mapAdvices = new ArrayList<>();

		javax.persistence.Query query = manager
				.createQuery("select m from MapAdviceJpa m");

		// Try query
		mapAdvices = query.getResultList();
		MapAdviceListJpa mapAdviceList = new MapAdviceListJpa();
		mapAdviceList.setMapAdvices(mapAdvices);
		mapAdviceList.setTotalCount(mapAdvices.size());
		return mapAdviceList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MappingService#getMapRelations()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public MapRelationList getMapRelations() {
		List<MapRelation> mapRelations = new ArrayList<>();

		javax.persistence.Query query = manager
				.createQuery("select m from MapRelationJpa m");

		// Try query
		mapRelations = query.getResultList();
		mapRelations = query.getResultList();
		MapRelationListJpa mapRelationList = new MapRelationListJpa();
		mapRelationList.setMapRelations(mapRelations);
		mapRelationList.setTotalCount(mapRelations.size());

		return mapRelationList;
	}

	// ///////////////////////////////////////
	// / Services for Map Project Creation
	// ///////////////////////////////////////

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#createMapRecordsForMapProject
	 * (java.lang.Long, org.ihtsdo.otf.mapping.helpers.WorkflowStatus)
	 */
	@Override
	public void createMapRecordsForMapProject(Long mapProjectId,
			WorkflowStatus workflowStatus) throws Exception {
		MapProject mapProject = getMapProject(mapProjectId);
		Logger.getLogger(MappingServiceJpa.class).info(
				"Called createMapRecordsForMapProject for project - " + mapProjectId
						+ " workflowStatus - " + workflowStatus);
		if (!getTransactionPerOperation()) {
			throw new IllegalStateException(
					"The application must let the service manage transactions for this method");
		}

		// retrieve all complex map ref set members for mapProject
		javax.persistence.Query query = manager
				.createQuery("select r from ComplexMapRefSetMemberJpa r "
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
						+ " complex map refset members found (some skipped)");
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

		tx = manager.getTransaction();

		int nRecords = 0;
		tx.begin();
		List<MapRecord> records = manager
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
				Integer.toString(nRecords)
						+ " records deleted for map project id = "
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
		Logger.getLogger(MappingServiceJpa.class).debug(
				"  Creating map records for map project - "
						+ mapProject.getName() + ", assigning workflow status " + WorkflowStatus.PUBLISHED);

		// Verify application is letting the service manage transactions
		if (!getTransactionPerOperation()) {
			throw new IllegalStateException(
					"The application must let the service manage transactions for this method");
		}

		// Setup content service
		ContentService contentService = new ContentServiceJpa();

		// Get map relation id->name mapping
		MetadataService metadataService = new MetadataServiceJpa();

		Map<String, String> relationIdNameMap = metadataService
				.getMapRelations(mapProject.getSourceTerminology(),
						mapProject.getSourceTerminologyVersion());
		Logger.getLogger(MappingServiceJpa.class).debug(
				"    relationIdNameMap = " + relationIdNameMap);

		// use the map relation id->name mapping to construct a hash set of
		// MapRelations
		Map<String, MapRelation> mapRelationIdMap = new HashMap<>();
		for (MapRelation mapRelation : getMapRelations().getIterable()) {
			mapRelationIdMap.put(mapRelation.getTerminologyId(), mapRelation);
		}

		Map<String, String> hierarchicalRelationshipTypeMap = metadataService
				.getHierarchicalRelationshipTypes(
						mapProject.getSourceTerminology(),
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
		// ASSUMPTION: only one "isa" type
		String hierarchicalRelationshipType = hierarchicalRelationshipTypeMap
				.entrySet().iterator().next().getKey();

		boolean prevTransactionPerOperationSetting = getTransactionPerOperation();
		setTransactionPerOperation(false);
		beginTransaction();
		MapAdviceList mapAdvices = getMapAdvices();
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
					Logger.getLogger(MappingServiceJpa.class).debug(
							"Skipping refset member "
									+ refSetMember.getTerminologyId());
					continue;
				}

				// Skip concept exclusion rules in all cases
				if (refSetMember.getMapRule().matches(
						"IFA\\s\\d*\\s\\|.*\\s\\|")
						&& !(refSetMember.getMapAdvice()
								.contains("MAP IS CONTEXT DEPENDENT FOR GENDER"))
						&& !(refSetMember.getMapRule()
								.matches("IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>]"))) {
					Logger.getLogger(MappingServiceJpa.class).debug(
							"    Skipping refset member exclusion rule "
									+ refSetMember.getTerminologyId());
					continue;
				}

				// retrieve the concept
				Logger.getLogger(MappingServiceJpa.class).debug(
						"    Get refset member concept");
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
					Logger.getLogger(MappingServiceJpa.class).debug(
							"    Creating map record for "
									+ concept.getTerminologyId());

					mapPriorityCt = 0;
					prevMapGroup = 0;
					mapRecord = new MapRecordJpa();
					mapRecord.setConceptId(concept.getTerminologyId());
					mapRecord.setConceptName(concept.getDefaultPreferredName());
					mapRecord.setMapProjectId(mapProject.getId());

					// get the number of descendants - Need to optimize this
					// Need a tool to compute and save this for LLCs (e.g.
					// having < 11
					// descendants)
					PfsParameter pfsParameter = new PfsParameterJpa();
					pfsParameter.setMaxResults(100);
					
					mapRecord.setCountDescendantConcepts(new Long(
							contentService.findDescendants(
									concept.getTerminologyId(),
									concept.getTerminology(),
									concept.getTerminologyVersion(),
									hierarchicalRelationshipType,
									pfsParameter).getCount()));
					Logger.getLogger(MappingServiceJpa.class).debug(
							"      Computing descendant ct = "
									+ mapRecord.getCountDescendantConcepts());

					// set the previous concept to this concept
					prevConceptId = refSetMember.getConcept()
							.getTerminologyId();

					// set the owner and lastModifiedBy user fields to
					// loaderUser
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

				// check if target is in desired terminology; if so, create
				// entry
				String targetName = null;

				if (!refSetMember.getMapTarget().equals("")) {
					Concept c = contentService.getConcept(
							refSetMember.getMapTarget(),
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
					relationName = relationIdNameMap.get(refSetMember
							.getMapRelationId());
					Logger.getLogger(this.getClass()).debug(
							"      Look up relation name = " + relationName);
				}

				Logger.getLogger(this.getClass()).debug(
						"      Create map entry");
				MapEntry mapEntry = new MapEntryJpa();
				mapEntry.setTargetId(refSetMember.getMapTarget());
				mapEntry.setTargetName(targetName);
				mapEntry.setMapRecord(mapRecord);
				mapEntry.setMapRelation(mapRelationIdMap.get(refSetMember
						.getMapRelationId().toString()));
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

				// Add support for advices - and there can be multiple map
				// advice values
				// Only add advice if it is an allowable value and doesn't match
				// relation name
				// This should automatically exclude IFA/ALWAYS advice
				Logger.getLogger(this.getClass()).debug(
						"      Setting map advice");
				if (refSetMember.getMapAdvice() != null
						&& !refSetMember.getMapAdvice().equals("")) {
					for (MapAdvice ma : mapAdvices.getIterable()) {
						if (refSetMember.getMapAdvice().indexOf(ma.getName()) != -1
								&& !ma.getName().equals(relationName)) {
							mapEntry.addMapAdvice(ma);
							Logger.getLogger(this.getClass()).debug(
									"    " + ma.getName());
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

	// ////////////////////////
	// AGE RANGE FUNCTIONS
	// ////////////////////////

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
		javax.persistence.Query query = manager
				.createQuery("select m from MapAgeRangeJpa m");

		// Try query

		m = query.getResultList();

		return m;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#addMapAgeRange(org.ihtsdo
	 * .otf.mapping.model.MapAgeRange)
	 */
	@Override
	public MapAgeRange addMapAgeRange(MapAgeRange mapAgeRange) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();

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
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#removeMapAgeRange(java
	 * .lang.Long)
	 */
	@Override
	public void removeMapAgeRange(Long mapAgeRangeId) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
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
	 * org.ihtsdo.otf.mapping.services.MappingService#updateMapAgeRange(org.
	 * ihtsdo .otf.mapping.model.MapAgeRange)
	 */
	@Override
	public void updateMapAgeRange(MapAgeRange mapAgeRange) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();

			tx.begin();
			manager.merge(mapAgeRange);
			tx.commit();
		} else {
			manager.merge(mapAgeRange);
		}
	}

	// /////////////////////////////////////
	// MAP USER PREFERENCES FUNCTIONS
	// /////////////////////////////////////

	@Override
	public MapUserPreferences getMapUserPreferences(String userName) {

		Logger.getLogger(MappingServiceJpa.class).info(
				"Finding user " + userName);
		MapUser mapUser = getMapUser(userName);
		javax.persistence.Query query = manager
				.createQuery(
						"select m from MapUserPreferencesJpa m where mapUser_id = :mapUser_id")
				.setParameter("mapUser_id", mapUser.getId());

		MapUserPreferences m;
		try {
			m = (MapUserPreferences) query.getSingleResult();
		}

		// catch no result exception and create default user preferences
		catch (NoResultException e) {

			// create object
			m = new MapUserPreferencesJpa();
			m.setMapUser(mapUser); // set the map user
			MapProjectList mapProjects = getMapProjects();
			m.setLastMapProjectId(mapProjects.getIterable().iterator().next()
					.getId()); // set a default project to 1st project found

			// add object
			addMapUserPreferences(m);
		}

		// return preferences
		return m;
	}

	/**
	 * Retrieve all map user preferences
	 * 
	 * @return a List of MapUserPreferencess
	 */
	@Override
	@SuppressWarnings("unchecked")
	public MapUserPreferencesList getMapUserPreferences() {

		List<MapUserPreferences> m = null;

		// construct query
		javax.persistence.Query query = manager
				.createQuery("select m from MapUserPreferencesJpa m");

		// Try query

		m = query.getResultList();
		MapUserPreferencesListJpa mapUserPreferencesList = new MapUserPreferencesListJpa();
		mapUserPreferencesList.setMapUserPreferences(m);
		mapUserPreferencesList.setTotalCount(m.size());
		return mapUserPreferencesList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#addMapUserPreferences(
	 * org.ihtsdo.otf.mapping.model.MapUserPreferences)
	 */
	@Override
	public MapUserPreferences addMapUserPreferences(
			MapUserPreferences mapUserPreferences) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();

			tx.begin();
			manager.persist(mapUserPreferences);
			tx.commit();

		} else {
			manager.persist(mapUserPreferences);
		}

		return mapUserPreferences;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MappingService#removeMapUserPreferences
	 * (java.lang.Long)
	 */
	@Override
	public void removeMapUserPreferences(Long mapUserPreferencesId) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();
			tx.begin();
			MapUserPreferences mar = manager.find(MapUserPreferencesJpa.class,
					mapUserPreferencesId);
			if (manager.contains(mar)) {
				manager.remove(mar);
			} else {
				manager.remove(manager.merge(mar));
			}
			tx.commit();
		} else {
			MapUserPreferences mar = manager.find(MapUserPreferencesJpa.class,
					mapUserPreferencesId);
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
	 * org.ihtsdo.otf.mapping.services.MappingService#updateMapUserPreferences
	 * (org.ihtsdo .otf.mapping.model.MapUserPreferences)
	 */
	@Override
	public void updateMapUserPreferences(MapUserPreferences mapUserPreferences) {
		if (getTransactionPerOperation()) {
			tx = manager.getTransaction();

			tx.begin();
			manager.merge(mapUserPreferences);
			tx.commit();
		} else {
			manager.merge(mapUserPreferences);
		}
	}

	@Override
	@XmlTransient
	public ProjectSpecificAlgorithmHandler getProjectSpecificAlgorithmHandler(
			MapProject mapProject) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {

		ProjectSpecificAlgorithmHandler algorithmHandler = (ProjectSpecificAlgorithmHandler) Class
				.forName(mapProject
										.getProjectSpecificAlgorithmHandlerClass())
				.newInstance();

		algorithmHandler.setMapProject(mapProject);
		return algorithmHandler;

	}

	/**
	 * Sets the valid field for tree positions, given a map project id.
	 * 
	 * @param treePositions
	 *            the tree positions
	 * @param mapProjectId
	 *            the map project id
	 * @return the revised list of tree positions
	 * @throws Exception
	 *             the exception
	 */
	@Override
	public TreePositionList setTreePositionValidCodes(
			List<TreePosition> treePositions, Long mapProjectId)
			throws Exception {

		// get the map project and its algorithm handler
		MapProject mapProject = getMapProject(mapProjectId);
		ProjectSpecificAlgorithmHandler algorithmHandler = getProjectSpecificAlgorithmHandler(mapProject);

		setTreePositionValidCodesHelper(treePositions, algorithmHandler);

		TreePositionListJpa treePositionList = new TreePositionListJpa();
		treePositionList.setTreePositions(treePositions);
		treePositionList.setTotalCount(treePositions.size());
		return treePositionList;
	}

	/**
	 * Helper function to recursively cycle over nodes and their children.
	 * Instantiated to prevent necessity for retrieving algorithm handler at
	 * each level. Note: Not necessary to return objects, tree positions are
	 * persisted objects
	 * 
	 * @param treePositions
	 *            the tree positions
	 * @param algorithmHandler
	 *            the algorithm handler
	 * @throws Exception
	 *             the exception
	 */
	public void setTreePositionValidCodesHelper(
			List<TreePosition> treePositions,
			ProjectSpecificAlgorithmHandler algorithmHandler) throws Exception {

		// cycle over all tree positions and check target code, recursively
		// cycle over children
		for (TreePosition tp : treePositions) {

			Logger.getLogger(MappingServiceJpa.class).info(
					"Checking valid for " + tp.getTerminologyId());

			tp.setValid(algorithmHandler.isTargetCodeValid(tp
					.getTerminologyId()));

			setTreePositionValidCodesHelper(tp.getChildren(), algorithmHandler);
		}
	}

	@Override
	public ValidationResult compareMapRecords(MapRecord mapRecord1,
			MapRecord mapRecord2) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {

		MapProject mapProject = getMapProject(mapRecord1.getMapProjectId());
		ProjectSpecificAlgorithmHandler algorithmHandler = getProjectSpecificAlgorithmHandler(mapProject);
		ValidationResult validationResult = algorithmHandler.compareMapRecords(
				mapRecord1, mapRecord2);

		return validationResult;
	}

	@Override
	public Map<Long, Long> compareFinishedMapRecords(MapProject mapProject)
			throws Exception {
		Map<MapRecord, MapRecord> finishedPairsForComparison = new HashMap<>();
		Map<Long, Long> conflicts = new HashMap<>();

		MappingService mappingService = new MappingServiceJpa();
		List<MapRecord> allMapRecords = mappingService
				.getMapRecordsForMapProject(mapProject.getId()).getMapRecords();
		List<MapRecord> finishedMapRecords = new ArrayList<>();
		for (MapRecord mapRecord : allMapRecords) {
			if (mapRecord.getWorkflowStatus().equals(
					WorkflowStatus.EDITING_DONE))
				finishedMapRecords.add(mapRecord);
		}
		MapRecord[] mapRecords = finishedMapRecords.toArray(new MapRecord[0]);
		for (int i = 0; i < mapRecords.length; i++) {
			for (int j = 0; j < mapRecords.length; j++) {
				if (mapRecords[i].getConceptId().equals(
						mapRecords[j].getConceptId())
						&& mapRecords[i].getLastModified() < mapRecords[j]
								.getLastModified()
						&& mapRecords[i].getId() != mapRecords[j].getId()) {
					finishedPairsForComparison
							.put(mapRecords[i], mapRecords[j]);
				}
			}
		}
		for (Entry<MapRecord, MapRecord> entry : finishedPairsForComparison
				.entrySet()) {
			// instantiate the algorithm handler for this project
			ProjectSpecificAlgorithmHandler handler = (ProjectSpecificAlgorithmHandler) Class
					.forName(mapProject
											.getProjectSpecificAlgorithmHandlerClass())
					.newInstance();
			handler.setMapProject(mapProject);

			// compare map records
			ValidationResult result = handler.compareMapRecords(entry.getKey(),
					entry.getValue());
			if (!result.isValid()) {
				conflicts.put(entry.getKey().getId(), entry.getValue().getId());
				entry.getKey().setWorkflowStatus(
						WorkflowStatus.CONFLICT_DETECTED);
				mappingService.updateMapRecord(entry.getKey());
				entry.getValue().setWorkflowStatus(
						WorkflowStatus.CONFLICT_DETECTED);
				mappingService.updateMapRecord(entry.getValue());
			}
		}
		return conflicts;
	}

	@Override
	public MapRecordList getRecordsInConflict(Long mapRecordId)
			throws Exception {

		Logger.getLogger(MappingServiceJpa.class).info(
				"getRecordsInConflict with record id = "
						+ mapRecordId.toString());

		MapRecordList conflictRecords = new MapRecordListJpa();

		MapRecord mapRecord = getMapRecord(mapRecordId);

		if (mapRecord == null)
			throw new Exception(
					"getRecordsInConflict: Could not find map record with id = "
							+ mapRecordId.toString() + "!");
		for (Long originId : mapRecord.getOriginIds()) {
			MapRecord mr = getMapRecord(originId);
			if (mr.getWorkflowStatus().equals(WorkflowStatus.CONFLICT_DETECTED)) {
				conflictRecords.addMapRecord(getMapRecord(originId));
			}
		}

		// set the total count for completeness (no paging here)
		conflictRecords.setTotalCount(conflictRecords.getCount());

		return conflictRecords;
	}

}
