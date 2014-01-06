package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.HashSet;
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
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.SearchResultList;
/**
 * 
 * The class for MappingServiceJpa
 *
 */
public class MappingServiceJpa implements MappingService {

		/** The factory. */
	private EntityManagerFactory factory;
	
	/** The indexed field names. */
	private Set<String> fieldNames;

	/**
	 * Instantiates an empty {@link MappingServiceJpa}.
	 */
	public MappingServiceJpa() {
		factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		EntityManager manager = factory.createEntityManager();;
		fieldNames = new HashSet<String>();

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
		
		if (fullTextEntityManager != null) { fullTextEntityManager.close(); }
		if (manager.isOpen()) { manager.close(); }
		//System.out.println("ended init " + fieldNames.toString());
	}
	
	/**
	 * Close the factory when done with this service
	 */
	public void close() {
		try {
			factory.close();
		} catch (Exception e) {
			System.out.println("Failed to close MappingService!");
			e.printStackTrace();
		}
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
	public MapProject getMapProject(Long id) {
		MapProject m = null;
		EntityManager manager = factory.createEntityManager();
		
		javax.persistence.Query query = manager.createQuery("select m from MapProjectJpa m where id = :id");
		query.setParameter("id", id);
		try {
			
			m = (MapProject) query.getSingleResult();
		} catch (Exception e) {
			System.out.println("Could not find map project for id = " + id.toString());
		}
		
		if (manager.isOpen()) { manager.close(); }
		
		return m;
		
	}
		
	/**
	* Retrieve all map projects
	* @return a List of MapProjects
	*/
	@SuppressWarnings("unchecked")
	public List<MapProject> getMapProjects() {
		
		List<MapProject> m = null;
		EntityManager manager = factory.createEntityManager();
		
		// construct query
		javax.persistence.Query query = manager.createQuery("select m from MapProjectJpa m");
		
		// Try query
		try {
			m = (List<MapProject>) query.getResultList();
		} catch (Exception e) {
			System.out.println("MappingServiceJpa.getMapProjects(): Could not retrieve map projects.");
			e.printStackTrace();
		}
		
		if (manager.isOpen()) { manager.close(); }
		
		return m;
	}
	

	/**
	* Query for MapProjects
	* @param query the query
	* @return the list of MapProject
	*/
	@SuppressWarnings("unchecked")
	public SearchResultList findMapProjects(String query) {

		SearchResultList s = new SearchResultListJpa();
		EntityManager manager = factory.createEntityManager();
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
			
			List<MapProject> m = (List<MapProject>) fullTextEntityManager.createFullTextQuery(luceneQuery, MapProjectJpa.class)
														.getResultList();	
			
			System.out.println(Integer.toString(m.size()) + " map projects retrieved");
			
			for (MapProject mp : m) {
				s.addSearchResult(new SearchResultJpa(mp.getId(), mp.getRefSetId().toString(),mp.getName()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (fullTextEntityManager != null) { fullTextEntityManager.close(); }
		if (manager.isOpen()) { manager.close(); }
		
		return s;
	}
	
	/**
	 * Add a map project
	 * @param mapProject the map project
	 */
	public void addMapProject(MapProject mapProject) {
		EntityManager manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			manager.persist(mapProject);
			tx.commit();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		if (manager.isOpen()) { manager.close(); }
		
	}
	
	/**
	 * Update a map project
	 * @param mapProject the changed map project
	 */
	public void updateMapProject(MapProject mapProject) {
		EntityManager manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		try {		
			tx.begin();
			manager.merge(mapProject);
			tx.commit();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		if (manager.isOpen()) { manager.close(); }
	}
	
	/**
	 * Remove (delete) a map project
	 * @param mapProjectId the map project to be removed
	 */
	public void removeMapProject(Long mapProjectId) {
		EntityManager manager = factory.createEntityManager();
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
		
		if (manager.isOpen()) { manager.close(); }
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
	@SuppressWarnings("unchecked")
	public List<MapSpecialist> getMapSpecialists() {
		
		List<MapSpecialist> m = null;
		EntityManager manager = factory.createEntityManager();
		javax.persistence.Query query = manager.createQuery("select m from MapSpecialistJpa m");
		
		// Try query
		try {
			m = (List<MapSpecialist>) query.getResultList();
		} catch (Exception e) {

		}
		
		if (manager.isOpen()) { manager.close(); }
		
		return m;
	}
	
	/**
	* Return map specialist for auto-generated id
	* @param id the auto-generated id
	* @return the MapSpecialist
	*/
	public MapSpecialist getMapSpecialist(Long id) {
		MapSpecialist m = null;
		EntityManager manager = factory.createEntityManager();
		
		javax.persistence.Query query = manager.createQuery("select m from MapSpecialistJpa m where id = :id");
		query.setParameter("id", id);
		try {
			m = (MapSpecialist) query.getSingleResult();
		} catch (Exception e) {
			System.out.println("Could not find map specialist for id = " + id.toString());
		}
		
		if (manager.isOpen()) { manager.close(); }
		
		return m;
		
	}
	
	/**
	* Retrieve all map projects assigned to a particular map specialist
	* @param mapSpecialist the map specialist
	* @return a List of MapProjects
	*/
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
	@SuppressWarnings("unchecked")
	public SearchResultList findMapSpecialists(String query) {

		SearchResultList s =new SearchResultListJpa();
		EntityManager manager = factory.createEntityManager();
		
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
			
			List<MapSpecialist> m = (List<MapSpecialist>) fullTextEntityManager.createFullTextQuery(luceneQuery, MapSpecialistJpa.class)
														.getResultList();
			
			for (MapSpecialist ms : m) {
				s.addSearchResult(new SearchResultJpa(ms.getId(), "", ms.getName()));
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (fullTextEntityManager != null) { fullTextEntityManager.close(); }
		if (manager.isOpen()) { manager.close(); }
		
		return s;
	}
	
	/**
	 * Add a map specialist
	 * @param mapSpecialist the map specialist
	 */
	public void addMapSpecialist(MapSpecialist mapSpecialist) {
		EntityManager manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			manager.persist(mapSpecialist);
			tx.commit();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		if (manager.isOpen()) { manager.close(); }
		
	}
	
	/**
	 * Update a map specialist
	 * @param mapSpecialist the changed map specialist
	 */
	public void updateMapSpecialist(MapSpecialist mapSpecialist) {
		
		EntityManager manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			manager.merge(mapSpecialist);
			tx.commit();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		if (manager.isOpen()) { manager.close(); }
	}
	
	/**
	 * Remove (delete) a map specialist
	 * @param mapSpecialistId the map specialist to be removed
	 */
	public void removeMapSpecialist(Long mapSpecialistId) {
		EntityManager manager = factory.createEntityManager();
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
		
		if (manager.isOpen()) { manager.close(); }
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
	@SuppressWarnings("unchecked")
	public List<MapLead> getMapLeads() {
		
		List<MapLead> mapLeads = new ArrayList<MapLead>();
		
		EntityManager manager = factory.createEntityManager();
		javax.persistence.Query query = manager.createQuery("select m from MapLeadJpa m");
		
		// Try query
		try {
			mapLeads = (List<MapLead>) query.getResultList();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (manager.isOpen()) { manager.close(); }
		
		return mapLeads;
	}
	
	/**
	* Return map lead for auto-generated id
	* @param id the auto-generated id
	* @return the MapLead
	*/
	public MapLead getMapLead(Long id) {
		MapLead m = null;
		EntityManager manager = factory.createEntityManager();
		
		javax.persistence.Query query = manager.createQuery("select m from MapLeadJpa m where id = :id");
		query.setParameter("id", id);
		try {
			m = (MapLead) query.getSingleResult();
		} catch (Exception e) {
			System.out.println("Could not find map lead for id = " + id.toString());
		}
	
		if (manager.isOpen()) { manager.close(); }
		
		return m;
		
	}
	
	/**
	* Retrieve all map projects assigned to a particular map lead
	* @param mapLead the map lead
	* @return a List of MapProjects
	*/
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
	@SuppressWarnings("unchecked")
	public SearchResultList findMapLeads(String query) {
		
		SearchResultList s = new SearchResultListJpa();
		EntityManager manager = factory.createEntityManager();
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
			
			List<MapLead> m = (List<MapLead>) fullTextEntityManager.createFullTextQuery(luceneQuery, MapLeadJpa.class)
														.getResultList();
			
			for (MapLead ml : m) {
				s.addSearchResult(new SearchResultJpa(ml.getId(), "", ml.getName()));
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		if (manager.isOpen()) { manager.close(); }
		if (fullTextEntityManager.isOpen()) { fullTextEntityManager.close(); }
		
		return s;
	}
	
	/**
	 * Add a map lead
	 * @param mapLead the map lead
	 */
	public void addMapLead(MapLead mapLead) {
		EntityManager manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			manager.persist(mapLead);
			tx.commit();
			manager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (manager.isOpen()) { manager.close(); }
		
	}
	
	/**
	 * Update a map lead
	 * @param mapLead the changed map lead
	 */
	public void updateMapLead(MapLead mapLead) {
		EntityManager manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			manager.merge(mapLead);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (manager.isOpen()) { manager.close(); }
	}
	
	/**
	 * Remove (delete) a map lead
	 * @param mapLeadId the map lead to be removed
	 */
	public void removeMapLead(Long mapLeadId) {
		EntityManager manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		// retrieve this map specialist
		
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
		
		if (manager.isOpen()) { manager.close(); }
		
	}
	

	////////////////////////////////////
	// MapRecord
	////////////////////////////////////
	
	/**
	* Retrieve all map records
	* @return a List of MapRecords
	*/
	@SuppressWarnings("unchecked")
	public List<MapRecord> getMapRecords() {
		
		List<MapRecord> m = null;
		EntityManager manager = factory.createEntityManager();
		
		// construct query
		javax.persistence.Query query = manager.createQuery("select m from MapRecordJpa m");
		
		// Try query
		try {
			m = (List<MapRecord>) query.getResultList();
		} catch (Exception e) {
			System.out.println("MappingServiceJpa.getMapRecords(): Could not retrieve map records.");
			e.printStackTrace();
		}
		
		if (manager.isOpen()) { manager.close(); }
		
		return m;
	}
	
	/** 
	 * Retrieve map record for given id
	 * @param id the map record id
	 * @return the map record
	 */
    public MapRecord getMapRecord(Long id) {
    	
    	EntityManager manager = factory.createEntityManager();
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
    @SuppressWarnings("unchecked")
    public SearchResultList findMapRecords(String query) {
    	
    	SearchResultList s = new SearchResultListJpa();
		EntityManager manager = factory.createEntityManager();
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
			
			List<MapRecord> m = (List<MapRecord>) fullTextEntityManager.createFullTextQuery(luceneQuery, MapRecordJpa.class)
														.getResultList();
			
			for (MapRecord mr : m) {
				s.addSearchResult(new SearchResultJpa(mr.getId(), "", mr.getConceptId()));
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		if (manager.isOpen()) { manager.close(); }
		if (fullTextEntityManager.isOpen()) { fullTextEntityManager.close(); }
		
		return s;
    }
    
    /**
     * Add a map record
     * @param mapRecord the map record to be added
     */
    public void addMapRecord(MapRecord mapRecord) {
    	EntityManager manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		
		try {
			tx.begin();
			manager.persist(mapRecord);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} 
			
		if (manager.isOpen()) { manager.close(); }
    }
    
    /**
     * Update a map record
     * @param mapRecord the map record to be updated
     */
    public void updateMapRecord(MapRecord mapRecord) {
    	EntityManager manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			manager.merge(mapRecord);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (manager.isOpen()) { manager.close(); }
    }
    
    /**
     * Remove (delete) a map record by id
     * @param id the id of the map record to be removed
     */
    public void removeMapRecord(Long id) {
    	EntityManager manager = factory.createEntityManager();
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
		
		if (manager.isOpen()) { manager.close(); }
		
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
	public SearchResultList findMapEntrys(String query) {
		SearchResultList s = new SearchResultListJpa();
		EntityManager manager = factory.createEntityManager();
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
			
			List<MapEntry> m = (List<MapEntry>) fullTextEntityManager.createFullTextQuery(luceneQuery, MapEntryJpa.class)
														.getResultList();
			
			for (MapEntry me : m) {
				s.addSearchResult(new SearchResultJpa(me.getId(), "", me.getMapRecord().getId().toString()));
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		if (manager.isOpen()) { manager.close(); }
		if (fullTextEntityManager.isOpen()) { fullTextEntityManager.close(); }
		
		return s;
	}
    
	/**
     * Service for finding MapAdvices by string query
     * @param query the query string
     * @return the search result list
     */
	public SearchResultList findMapAdvices(String query) {
		SearchResultList s = new SearchResultListJpa();
		EntityManager manager = factory.createEntityManager();
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
			
			List<MapAdvice> m = (List<MapAdvice>) fullTextEntityManager.createFullTextQuery(luceneQuery, MapNoteJpa.class)
														.getResultList();
			
			for (MapAdvice ma : m) {
				s.addSearchResult(new SearchResultJpa(ma.getId(), "", ma.getName()));
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		if (manager.isOpen()) { manager.close(); }
		if (fullTextEntityManager.isOpen()) { fullTextEntityManager.close(); }
		
		return s;
	}
	

	/**
     * Service for finding MapNote by string query
     * @param query the query string
     * @return the search result list
     */
	@Override
	public SearchResultList findMapNotes(String query) {
		SearchResultList s = new SearchResultListJpa();
		EntityManager manager = factory.createEntityManager();
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
			
			List<MapNote> m = (List<MapNote>) fullTextEntityManager.createFullTextQuery(luceneQuery, MapNoteJpa.class)
														.getResultList();
			
			for (MapNote me : m) {
				s.addSearchResult(new SearchResultJpa(me.getId(), "", me.getNote()));
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		if (manager.isOpen()) { manager.close(); }
		if (fullTextEntityManager.isOpen()) { fullTextEntityManager.close(); }
		
		return s;
	}
	
	////////////////////////////////////////////////
	// Service for retrieving all project details
    ////////////////////////////////////////////////
	
	/**
	 * Retrieve map records for a given concept id
	 * @param conceptId the concept id
	 * @return the list of map records
	 */
	public List<MapRecord> getMapRecordsForConceptId(String conceptId) {
		List<MapRecord> m = null;
		EntityManager manager = factory.createEntityManager();
		
		// construct query
		javax.persistence.Query query = manager.createQuery("select m from MapRecordJpa m where conceptId = :conceptId");
		
		// Try query
		try {
			query.setParameter("conceptId", conceptId);
			m = (List<MapRecord>) query.getResultList();
		} catch (Exception e) {
			System.out.println("MappingServiceJpa.getMapRecordsForConceptId(): Could not retrieve map records.");
			e.printStackTrace();
		}
	
		if (manager.isOpen()) { manager.close(); }
		
		return m;
	}
	
	/**
	 * Retrieve map records for a given concept id
	 * @param mapProjectId the concept id
	 * @return the list of map records
	 */
	public List<MapRecord> getMapRecordsForMapProjectId(String mapProjectId) {
		List<MapRecord> m = null;
		EntityManager manager = factory.createEntityManager();
		
		// construct query
		javax.persistence.Query query = manager.createQuery("select m from MapRecordJpa m where mapProjectId = :mapProjectId");
		
		// Try query
		try {
			query.setParameter("mapProjectId", new Long(mapProjectId));
			m = (List<MapRecord>) query.getResultList();
		} catch (Exception e) {
			System.out.println("MappingServiceJpa.getMapRecordsFormapProjectId(): Could not retrieve map records for project id " + mapProjectId);
			e.printStackTrace();
		}
	
		if (manager.isOpen()) { manager.close(); }
		
		return m;
	}

	

	
	
	
	
}
