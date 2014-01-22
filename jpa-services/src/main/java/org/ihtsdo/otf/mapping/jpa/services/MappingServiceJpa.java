package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
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
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
/**
 * 
 * The class for MappingServiceJpa
 *
 */
public class MappingServiceJpa implements MappingService {

		/** The factory. */
	private static EntityManagerFactory factory;
	
	/** The manager */
	private EntityManager manager;
	
	/** The full text entity manager */
    private	FullTextEntityManager fullTextEntityManager;
	
	/** The indexed field names. */
	private static Set<String> fieldNames;

	/**
	 * Instantiates an empty {@link MappingServiceJpa}.
	 */
	public MappingServiceJpa() {
		
		// created once
		if (factory == null) {
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
			
			
			if (fullTextEntityManager != null) { fullTextEntityManager.close(); }
			
			// closing fullTextEntityManager also closes manager, recreate
			manager = factory.createEntityManager();
		}
	}
	
	/**
	 * Close the manager when done with this service
	 */
	@Override
	public void close() throws Exception {
		if (manager.isOpen()) { manager.close(); }
	}
	
	/**
	 * Returns the status of the manager
	 * @return true if open, false if not
	 */
	public boolean isManagerOpen() {
		return this.manager.isOpen();
	}
	
	/**
	 * Returns the status of the factory
	 * @return true if factory open, false if not
	 */
	public static boolean isFactoryOpen() {
		return MappingServiceJpa.factory.isOpen();
	}
	////////////////////////////////////
	// MapProject
	// - getMapProjects
	// - getMapProject(Long id)
	// - getMapProject(String name)
	// - findMapProjects(String query)
	// - addMapProject(MapProject mapProject)
	// - updateMapProject(MapProject mapProject)
	// - removeMapProject(MapProject mapProject)
	////////////////////////////////////
	
	/**
	* Return map project for auto-generated id
	* @param id the auto-generated id
	* @return the MapProject
	*/
	@Override
	public MapProject getMapProject(Long id) {
		
		MapProject m = null;
		
		javax.persistence.Query query = manager.createQuery("select m from MapProjectJpa m where id = :id");
		query.setParameter("id", id);
		try {
			
			m = (MapProject) query.getSingleResult();
		} catch (Exception e) {
			System.out.println("Could not find map project for id = " + id.toString());
		}

		return m;
		
	}
		
	/**
	* Retrieve all map projects
	* @return a List of MapProjects
	*/
	@Override
	@SuppressWarnings("unchecked")
	public List<MapProject> getMapProjects() {
		
		
		if (!manager.isOpen()) { System.out.println("Feh"); }
		
		List<MapProject> m = null;
			
		// construct query
		javax.persistence.Query query = manager.createQuery("select m from MapProjectJpa m");
		
		// Try query
		try {
			m = query.getResultList();
		} catch (Exception e) {
			System.out.println("MappingServiceJpa.getMapProjects(): Could not retrieve map projects.");
			e.printStackTrace();
		}
		
		
		
		return m;
	}
	

	/**
	* Query for MapProjects
	* @param query the query
	* @return the list of MapProject
	*/
	@Override
	@SuppressWarnings("unchecked")
	public SearchResultList findMapProjects(String query, PfsParameter pfsParameter) {

		
		SearchResultList s = new SearchResultListJpa();
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(manager);
		
		try {
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
				QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
						searchFactory.getAnalyzer(MapProjectJpa.class));
				luceneQuery = queryParser.parse(query);
			}
			
			List<MapProject> m = fullTextEntityManager.createFullTextQuery(luceneQuery, MapProjectJpa.class)
														.getResultList();	
			
			System.out.println(Integer.toString(m.size()) + " map projects retrieved");
			
			for (MapProject mp : m) {
				s.addSearchResult(new SearchResultJpa(mp.getId(), mp.getRefSetId().toString(),mp.getName()));
			}
			
			s.sortSearchResultsById();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (fullTextEntityManager != null) { fullTextEntityManager.close(); }
		
		
		return s;
	}
	
	/**
	 * Add a map project
	 * @param mapProject the map project
	 */
	@Override
	public void addMapProject(MapProject mapProject) {
		
		
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			manager.persist(mapProject);
			tx.commit();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		
		
	}
	
	/**
	 * Update a map project
	 * @param mapProject the changed map project
	 */
	@Override
	public void updateMapProject(MapProject mapProject) {
		
		
		EntityTransaction tx = manager.getTransaction();
		try {		
			tx.begin();
			manager.merge(mapProject);
			tx.commit();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * Remove (delete) a map project
	 * @param mapProjectId the map project to be removed
	 */
	@Override
	public void removeMapProject(Long mapProjectId) {
		
		
		EntityTransaction tx = manager.getTransaction();
		try {
			// first, remove the leads and specialists from this project
			tx.begin();
			MapProject mp = manager.find(MapProjectJpa.class, mapProjectId);
			mp.setMapLeads(null);
			mp.setMapSpecialists(null);
			tx.commit();
			
			// now remove the project
			tx.begin();
			manager.remove(mp);
			tx.commit();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		
	}
	
	/////////////////////////////////////////////////////////////////
	// MapSpecialist
	// - getMapSpecialists() 
	// - getMapProjectsForSpecialist(MapSpecialist mapSpecialist)
	// - findMapSpecialists(String query)
	// - addMapSpecialist(MapSpecialist mapSpecialist)
	// - updateMapSpecialist(MapSpecialist mapSpecialist)
	// - removeMapSpecialist(Long id)
	/////////////////////////////////////////////////////////////////
	
	/**
	* Retrieve all map specialists
	* @return a List of MapSpecialists
	*/
	@Override
	@SuppressWarnings("unchecked")
	public List<MapSpecialist> getMapSpecialists() {
		
		
		List<MapSpecialist> m = null;
		
		javax.persistence.Query query = manager.createQuery("select m from MapSpecialistJpa m");
		
		// Try query
		try {
			m = query.getResultList();
		} catch (Exception e) {
			// Do nothing
		}
		
		
		
		return m;
	}
	
	/**
	* Return map specialist for auto-generated id
	* @param id the auto-generated id
	* @return the MapSpecialist
	*/
	@Override
	public MapSpecialist getMapSpecialist(Long id) {
		
		
		MapSpecialist m = null;
		
		javax.persistence.Query query = manager.createQuery("select m from MapSpecialistJpa m where id = :id");
		query.setParameter("id", id);
		try {
			m = (MapSpecialist) query.getSingleResult();
		} catch (Exception e) {
			System.out.println("Could not find map specialist for id = " + id.toString());
		}
		
		
		
		return m;
		
	}
	
	/**
	* Retrieve all map projects assigned to a particular map specialist
	* @param mapSpecialist the map specialist
	* @return a List of MapProjects
	*/
	@Override
	public List<MapProject> getMapProjectsForMapSpecialist(MapSpecialist mapSpecialist) {
		
		List<MapProject> mp_list = getMapProjects();
		List<MapProject> mp_list_return = new ArrayList<MapProject>();
		
		// iterate and check for presence of mapSpecialist
		for (MapProject mp : mp_list ) {
		
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
	* Query for MapSpecialists
	* @param query the query
	* @return the List of MapProjects
	*/
	@Override
	@SuppressWarnings("unchecked")
	public SearchResultList findMapSpecialists(String query, PfsParameter pfsParameter) {

		
		SearchResultList s =new SearchResultListJpa();
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(manager);
		
		try {
			
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
				QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
						searchFactory.getAnalyzer(MapSpecialistJpa.class));
				luceneQuery = queryParser.parse(query);
			}
			
			List<MapSpecialist> m = fullTextEntityManager.createFullTextQuery(luceneQuery, MapSpecialistJpa.class)
														.getResultList();
			
			for (MapSpecialist ms : m) {
				s.addSearchResult(new SearchResultJpa(ms.getId(), "", ms.getName()));
			}
			
			s.sortSearchResultsById();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (fullTextEntityManager != null) { fullTextEntityManager.close(); }
		
		
		return s;
	}
	
	/**
	 * Add a map specialist
	 * @param mapSpecialist the map specialist
	 */
	@Override
	public void addMapSpecialist(MapSpecialist mapSpecialist) {
		
		
		EntityTransaction tx = manager.getTransaction();
		
		try {
			tx.begin();
			manager.persist(mapSpecialist);
			tx.commit();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		
		
	}
	
	/**
	 * Update a map specialist
	 * @param mapSpecialist the changed map specialist
	 */
	@Override
	public void updateMapSpecialist(MapSpecialist mapSpecialist) {
		
		
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			manager.merge(mapSpecialist);
			tx.commit();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * Remove (delete) a map specialist
	 * @param mapSpecialistId the map specialist to be removed
	 */
	@Override
	public void removeMapSpecialist(Long mapSpecialistId) {
		
		
		EntityTransaction tx = manager.getTransaction();
		
		// retrieve this map specialist
		MapSpecialist ms = manager.find(MapSpecialistJpa.class, mapSpecialistId);

		// retrieve all projects on which this specialist appears
		List<MapProject> projects = getMapProjectsForMapSpecialist(ms);
		
		try {
			// remove specialist from all these projects
			tx.begin();
			for (MapProject mp : projects) {
				mp.removeMapSpecialist(ms);
				manager.merge(mp);
			}
			tx.commit();

			// remove specialist
			tx.begin();
			manager.remove(ms);
			tx.commit();
			
		} catch (Exception e) {
			System.out.println("Failed to remove map specialist " + Long.toString(mapSpecialistId));
			e.printStackTrace();
		}
		
		
	}
	
	
	/////////////////////////////////////////////////////
	// MapLead
	// - getMapLeads() 
	// - getMapProjectsForLead(mapSpecialist)
	// - findMapSpecialists(query)
	// - addMapLead(mapLead)
	// - updateMapLead(mapLead)
	// - removeMapLead(id)
	/////////////////////////////////////////////////////
	
	/**
	* Retrieve all map leads
	* @return a List of MapLeads
	*/
	@Override
	@SuppressWarnings("unchecked")
	public List<MapLead> getMapLeads() {
		
		
		List<MapLead> mapLeads = new ArrayList<MapLead>();
				
		javax.persistence.Query query = manager.createQuery("select m from MapLeadJpa m");
		
		// Try query
		try {
			mapLeads = query.getResultList();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mapLeads;
	}
	
	/**
	* Return map lead for auto-generated id
	* @param id the auto-generated id
	* @return the MapLead
	*/
	@Override
	public MapLead getMapLead(Long id) {
		
		
		MapLead m = null;
				
		javax.persistence.Query query = manager.createQuery("select m from MapLeadJpa m where id = :id");
		query.setParameter("id", id);
		try {
			m = (MapLead) query.getSingleResult();
		} catch (Exception e) {
			System.out.println("Could not find map lead for id = " + id.toString());
		}
	
		
		
		return m;
		
	}
	
	/**
	* Retrieve all map projects assigned to a particular map lead
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
	* Query for MapLeads
	* @param query the query
	* @return the List of MapProjects
	*/
	@Override
	@SuppressWarnings("unchecked")
	public SearchResultList findMapLeads(String query, PfsParameter pfsParameter) {
		
		
		SearchResultList s = new SearchResultListJpa();
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(manager);
		
		try {
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
				QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
						searchFactory.getAnalyzer(MapLeadJpa.class));
				luceneQuery = queryParser.parse(query);
			}
			
			List<MapLead> m = fullTextEntityManager.createFullTextQuery(luceneQuery, MapLeadJpa.class)
														.getResultList();
			
			for (MapLead ml : m) {
				s.addSearchResult(new SearchResultJpa(ml.getId(), "", ml.getName()));
			}
			
			s.sortSearchResultsById();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		
		if (fullTextEntityManager.isOpen()) { fullTextEntityManager.close(); }
		
		return s;
	}
	
	/**
	 * Add a map lead
	 * @param mapLead the map lead
	 */
	@Override
	public void addMapLead(MapLead mapLead) {
		
		
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			manager.persist(mapLead);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Update a map lead
	 * @param mapLead the changed map lead
	 */
	@Override
	public void updateMapLead(MapLead mapLead) {
		
		
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			manager.merge(mapLead);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * Remove (delete) a map lead
	 * @param mapLeadId the map lead to be removed
	 */
	@Override
	public void removeMapLead(Long mapLeadId) {
		
		
		EntityTransaction tx = manager.getTransaction();
		
		// retrieve this map lead		
		MapLead ml = manager.find(MapLeadJpa.class, mapLeadId);

		// retrieve all projects on which this lead appears
		List<MapProject> projects = getMapProjectsForMapLead(ml);
		
		try {
			// remove lead from all these projects
			tx.begin();
			for (MapProject mp : projects) {
				mp.removeMapLead(ml);
				manager.merge(mp);	
			}
			tx.commit();

			// remove lead
			tx.begin();
			manager.remove(ml);
			tx.commit();
			
		} catch (Exception e) {
			System.out.println("Failed to remove map lead " + Long.toString(mapLeadId));
			e.printStackTrace();
		}
		
		
		
	}
	

	////////////////////////////////////
	// MapRecord
	////////////////////////////////////
	
	/**
	* Retrieve all map records
	* @return a List of MapRecords
	*/
	@Override
	@SuppressWarnings("unchecked")
	public List<MapRecord> getMapRecords() {
		
		
		List<MapRecord> m = null;
				
		// construct query
		javax.persistence.Query query = manager.createQuery("select m from MapRecordJpa m");
		
		// Try query
		try {
			m = query.getResultList();
		} catch (Exception e) {
			System.out.println("MappingServiceJpa.getMapRecords(): Could not retrieve map records.");
			e.printStackTrace();
		}
		
		
		
		return m;
	}
	
	/** 
	 * Retrieve map record for given id
	 * @param id the map record id
	 * @return the map record
	 */
    @Override
	public MapRecord getMapRecord(Long id) {
    	
    	
		javax.persistence.Query query = manager.createQuery("select r from MapRecordJpa r where id = :id");
		
		/*
		 * Try to retrieve the single expected result
		 * If zero or more than one result are returned, log error and set result to null
		 */

		try {
			
			query.setParameter("id", id);
			
			MapRecord r = (MapRecord) query.getSingleResult();

			System.out.println("Returning record_id... " + ((r != null) ? r.getId().toString() : "null"));
			return r;
			
		} catch (NoResultException e) {
			System.out.println("MapRecord query for id = " + id  + " returned no results!");
			return null;		
		} catch (NonUniqueResultException e) {
			System.out.println("MapRecord query for id = " + id  + " returned multiple results!");
			return null;
		}	
    }
    
  /*  public List<MapRecord> getMapRecords(mapProjectId, sortingInfo, pageSize, page#) {
    	///project/id/12345/records?sort=sortkey&pageSize=100&page=1
    	return null;
    }*/
    
    /**
     * Retrieve map records for a lucene query
     * @param query the lucene query string
     * @return a list of map records
     */
    @Override
	@SuppressWarnings("unchecked")
    public SearchResultList findMapRecords(String query, PfsParameter pfsParameter) {
    	
    	
    	SearchResultList s = new SearchResultListJpa();
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(manager);
		
		try {
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
				QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
						searchFactory.getAnalyzer(MapRecordJpa.class));
				luceneQuery = queryParser.parse(query);
			}
			
			List<MapRecord> m = fullTextEntityManager.createFullTextQuery(luceneQuery, MapRecordJpa.class)
														.getResultList();
			
			for (MapRecord mr : m) {
				s.addSearchResult(new SearchResultJpa(mr.getId(), "", mr.getConceptId()));
			}
			
			
			s.sortSearchResultsById();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		
		if (fullTextEntityManager.isOpen()) { fullTextEntityManager.close(); }
		
		return s;
    }
    
    /**
     * Add a map record
     * @param mapRecord the map record to be added
     */
    @Override
	public void addMapRecord(MapRecord mapRecord) {
    	
    	
		EntityTransaction tx = manager.getTransaction();
		
		try {
			tx.begin();
			manager.persist(mapRecord);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} 
			
		
    }
    
    /**
     * Update a map record
     * @param mapRecord the map record to be updated
     */
    @Override
	public void updateMapRecord(MapRecord mapRecord) {
    	
    	
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			manager.merge(mapRecord);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
    }
    
    /**
     * Remove (delete) a map record by id
     * @param id the id of the map record to be removed
     */
    @Override
	public void removeMapRecord(Long id) {
    	
    	
		EntityTransaction tx = manager.getTransaction();
		
		// find the map record		
		MapRecord m = manager.find(MapRecord.class, id);
		
		try {
			// delete the map record
			tx.begin();
			manager.remove(m);
			tx.commit();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		
		
    }
    

    
	
	////////////////////////////////////
	// Other query services
	////////////////////////////////////
	
    /**
     * Service for finding MapEntrys by string query
     * @param query the query string
     * @return the search result list
     */
	@Override
	public SearchResultList findMapEntrys(String query, PfsParameter pfsParameter) {
		SearchResultList s = new SearchResultListJpa();
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(manager);
		
		try {
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
				QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
						searchFactory.getAnalyzer(MapEntryJpa.class));
				luceneQuery = queryParser.parse(query);
			}
			
			List<MapEntry> m = fullTextEntityManager.createFullTextQuery(luceneQuery, MapEntryJpa.class)
														.getResultList();
			
			for (MapEntry me : m) {
				s.addSearchResult(new SearchResultJpa(me.getId(), "", me.getMapRecord().getId().toString()));
			}
			
			s.sortSearchResultsById();	
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		if (fullTextEntityManager.isOpen()) { fullTextEntityManager.close(); }
		
		return s;
	}
    
	/**
     * Service for finding MapAdvices by string query
     * @param query the query string
     * @return the search result list
     */
	@Override
	public SearchResultList findMapAdvices(String query, PfsParameter pfsParameter) {
		SearchResultList s = new SearchResultListJpa();
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(manager);
		
		try {
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
				QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
						searchFactory.getAnalyzer(MapAdviceJpa.class));
				luceneQuery = queryParser.parse(query);
			}
			
			List<MapAdvice> m = fullTextEntityManager.createFullTextQuery(luceneQuery, MapNoteJpa.class)
														.getResultList();
			
			for (MapAdvice ma : m) {
				s.addSearchResult(new SearchResultJpa(ma.getId(), "", ma.getName()));
			}
			
			
			s.sortSearchResultsById();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		
		if (fullTextEntityManager.isOpen()) { fullTextEntityManager.close(); }

		
		return s;
	}
	

	/**
     * Service for finding MapNote by string query
     * @param query the query string
     * @return the search result list
     */
	@Override
	public SearchResultList findMapNotes(String query, PfsParameter pfsParameter) {
		SearchResultList s = new SearchResultListJpa();
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(manager);
		
		try {
				SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();
			Query luceneQuery;
		
			// construct luceneQuery based on URL format
			if (query.indexOf(':') == -1) { // no fields indicated
				MultiFieldQueryParser queryParser =
						new MultiFieldQueryParser(Version.LUCENE_36,
								fieldNames.toArray(new String[0]),
								searchFactory.getAnalyzer(MapNoteJpa.class));
				queryParser.setAllowLeadingWildcard(false);
				luceneQuery = queryParser.parse(query);
		
			
			} else { // field:value
				QueryParser queryParser = new QueryParser(Version.LUCENE_36, "summary",
						searchFactory.getAnalyzer(MapNoteJpa.class));
				luceneQuery = queryParser.parse(query);
			}
			
			List<MapNote> m = fullTextEntityManager.createFullTextQuery(luceneQuery, MapNoteJpa.class)
														.getResultList();
			
			for (MapNote me : m) {
				s.addSearchResult(new SearchResultJpa(me.getId(), "", me.getNote()));
			}
			
			s.sortSearchResultsById();
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		
		if (fullTextEntityManager.isOpen()) { fullTextEntityManager.close(); }
		
		return s;
	}
	
	////////////////////////////////////////////////
	// Descendant services
    ////////////////////////////////////////////////
	
	/**
	 * Retrieve map records for a given concept id
	 * @param conceptId the concept id
	 * @return the list of map records
	 */
	@Override
	public List<MapRecord> getMapRecordsForConceptId(String conceptId) {
		List<MapRecord> m = null;
		
		
		// construct query
		javax.persistence.Query query = manager.createQuery("select m from MapRecordJpa m where conceptId = :conceptId");
		
		// Try query
		try {
			query.setParameter("conceptId", conceptId);
			m = query.getResultList();
		} catch (Exception e) {
			System.out.println("MappingServiceJpa.getMapRecordsForConceptId(): Could not retrieve map records.");
			e.printStackTrace();
		}
	
		
		
		return m;
	}
	
	@Override
	public Long getMapRecordCountForMapProjectId(Long mapProjectId) {
		
		javax.persistence.Query query = manager.createQuery("select count(m) from MapRecordJpa m where mapProjectId = :mapProjectId");
		
		try {
			query.setParameter("mapProjectId", mapProjectId);
			return new Long(query.getSingleResult().toString());
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
		}
		
		return new Long("-1");
		
	}
	
	/**
	 * Helper function:  retrieves all map records for a map project id without paging/filtering/sorting
	 * @param mapProjectId the concept id
	 * @return the list of map records
	 */
	@Override
	public List<MapRecord> getMapRecordsForMapProjectId(Long mapProjectId) {
		
		return getMapRecordsForMapProjectId(mapProjectId, null);
	}
	
	/**
	 * Retrieve map records for a given concept id and paging/filtering/sorting parameters
	 * @param mapProjectId the concept id
	 * @param pfs the paging/filtering/sorting parameter object
	 * @return the list of map records
	 */
	@Override
	public List<MapRecord> getMapRecordsForMapProjectId(Long mapProjectId, PfsParameter pfs) {
		List<MapRecord> m = null;
		
		
		// construct query
		javax.persistence.Query query = manager.createQuery("select m from MapRecordJpa m where mapProjectId = :mapProjectId");
		
		// Try query
		try {
			query.setParameter("mapProjectId", mapProjectId);
			
			if (pfs != null) {
				query.setFirstResult(pfs.getStartIndex());
				query.setMaxResults(pfs.getMaxResults());
			}
			
			m = query.getResultList();
		} catch (Exception e) {
			System.out.println("MappingServiceJpa.getMapRecordsFormapProjectId(): Could not retrieve map records for project id " + mapProjectId);
			e.printStackTrace();
		}
	
		
		
		return m;
	}
	
	/**
	 * Helper function for restful services.  Constructs a basic concept from parameters and calls getUnmappedDescentsForConcept(Concept concept, int thresholdLlc)
	 * @param terminologyId
	 * @param terminology
	 * @param terminologyVersion
	 * @param thresholdLlc
	 * @return
	 * @throws Exception
	 */
	@Override
	public List<Concept> getUnmappedDescendantsForConcept(String terminologyId, String terminology, String terminologyVersion, int thresholdLlc) throws Exception {
		Concept concept = new ConceptJpa();
		concept.setTerminologyId(terminologyId);
		concept.setTerminology(terminology);
		concept.setTerminologyVersion(terminologyVersion);
		return getUnmappedDescendantsForConcept(concept, thresholdLlc);
	}
	
	/**
	 * Given a concept, returns a list of descendant concepts that have no associated map record
	 * @param concept the root concept
	 * @param thresholdLlc the maximum number of descendants a concept can have before it is no longer considered a low-level concept (i.e. return an empty list)
	 * @return the list of unmapped descendant concepts
	 * @throws Exception 
	 */
	public List<Concept> getUnmappedDescendantsForConcept(Concept concept, int thresholdLlc) throws Exception {
		
		// declare results list and content service
		List<Concept> unmappedDescendants = new ArrayList<Concept>();
		ContentService contentService = new ContentServiceJpa();
				
		// get descendants and construct iterator
		Set<Concept> descendants = contentService.getDescendants(concept.getTerminologyId(), concept.getTerminology(), concept.getTerminologyVersion(), new Long("116680003"));
		Iterator<Concept> descendants_iter = descendants.iterator();
		
		// if size of descendant set is greater than the low-level concept threshold, skip it
		if (descendants.size() <= thresholdLlc) {
			
			// cycle over descendants
			while (descendants_iter.hasNext()) {
				
				Concept descendant = descendants_iter.next();
				
				// find map records for this id
				List<MapRecord> conceptRecords = getMapRecordsForConceptId(descendant.getTerminologyId());
				
				System.out.println(descendant.getTerminologyId() + " has " + Integer.toString(conceptRecords.size()) + " map records");
				
				// if no records found, add to unmapped list
				if (conceptRecords.size() == 0) {
					System.out.println("--> Adding to unmapped list");
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
	
	
	////////////////////////////////
	// Services to be implemented //
	////////////////////////////////
	
	////////////////////////////
	// Addition services     ///
	////////////////////////////
	
	/**
	 * Add a map note
	 * @param mapNote the map note
	 */
	@Override
	public void addMapNote(MapNote mapNote) {

		EntityTransaction tx = manager.getTransaction();
	
		tx.begin();
		manager.persist(mapNote);
		tx.commit();
		
	}
	
	@Override
	public void addMapEntry(MapEntry mapEntry) {

		EntityTransaction tx = manager.getTransaction();
	
		tx.begin();
		manager.persist(mapEntry);
		tx.commit();
		
	}
	
	@Override
	public void addMapPrinciple(MapPrinciple mapPrinciple) { 

		EntityTransaction tx = manager.getTransaction();
	
		tx.begin();
		manager.persist(mapPrinciple);
		tx.commit();
	}
	
	@Override
	public void addMapAdvice(MapAdvice mapAdvice) { 

		EntityTransaction tx = manager.getTransaction();
	
		tx.begin();
		manager.persist(mapAdvice);
		tx.commit();
	}
	
	////////////////////////////
	// Update services     ///
	////////////////////////////
	
	@Override
	public void updateMapNote(MapNote mapNote) {

		EntityTransaction tx = manager.getTransaction();
	
		tx.begin();
		manager.merge(mapNote);
		tx.commit();
	}
	
	@Override
	public void updateMapEntry(MapEntry mapEntry) { 

		EntityTransaction tx = manager.getTransaction();
	
		tx.begin();
		manager.merge(mapEntry);
		tx.commit();
	}
	
	@Override
	public void updateMapPrinciple(MapPrinciple mapPrinciple) { 

		EntityTransaction tx = manager.getTransaction();
	
		tx.begin();
		manager.merge(mapPrinciple);
		tx.commit();
		manager.close();
	}
	
	@Override
	public void updateMapAdvice(MapAdvice mapAdvice) { 

		EntityTransaction tx = manager.getTransaction();
	
		tx.begin();
		manager.merge(mapAdvice);
		tx.commit();
		manager.close();
	}
	
	////////////////////////////
	//Removal services     ///
	////////////////////////////
	
	@Override
	public void removeMapNote(Long mapNoteId) { 
		
		MapNote mn = manager.find(MapNoteJpa.class, mapNoteId);
			
		// remove MapNote from any associated records
		
		// remove MapNote from any associated entries

		EntityTransaction tx = manager.getTransaction();
	
		
	}
	
	@Override
	public void removeMapEntry(Long mapEntryId) { }
	
	@Override
	public void removeMapPrinciple(Long mapPrincipleId) { }
	
	@Override
	public void removeMapAdvice(Long mapAdviceId) { }

	@Override
	public MapPrinciple getMapPrinciple(Long id) {
		
		
		MapPrinciple m = null;
				
		javax.persistence.Query query = manager.createQuery("select m from MapPrincipleJpa m where id = :id");
		query.setParameter("id", id);
		try {
			m = (MapPrinciple) query.getSingleResult();
			System.out.println("HI there!");
		} catch (Exception e) {
			System.out.println("Could not find map principle for id = " + id.toString());
		}
	
		
		
		return m;
		
	}

	

	
	
	
	
}
