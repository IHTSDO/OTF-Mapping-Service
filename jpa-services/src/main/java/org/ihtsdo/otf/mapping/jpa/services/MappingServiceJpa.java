package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
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
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * 
 * The class for MappingServiceJpa
 *
 */
public class MappingServiceJpa implements MappingService {

	/** The factory. */
	private EntityManagerFactory factory;

	/** The manager. */
	private EntityManager manager;
	
	/** The indexed field names. */
	private Set<String> fieldNames;

	/**
	 * Instantiates an empty {@link MappingServiceJpa}.
	 */
	public MappingServiceJpa() {
		factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		manager = factory.createEntityManager();;
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
		manager.close();
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
	
	
	//////////////////////////////
	// Basic retrieval services //
	//////////////////////////////
	
	
	////////////////////////////////////
	// MapProject
	// - getMapProjects
	// - getMapProject(Long id)
	// - getMapProject(String name)
	// - findMapProjects(String query)
	////////////////////////////////////
	
	/**
	* Return map project for auto-generated id
	* @param id the auto-generated id
	* @return the MapProject
	*/
	public MapProject getMapProject(Long id) {
		MapProject m = null;
		manager = factory.createEntityManager();

		try {
			
			m = manager.find(MapProjectJpa.class, id);
		} catch (Exception e) {
			System.out.println("Could not find map project for id = " + id.toString());
		}
		manager.close();
		return m;
		
	}
	
	/**
	* Return map project by project name
	* @param name the project name
	* @return the MapProject
	*/
	public MapProject getMapProject(String name) {
		
		MapProject m = null;
		manager = factory.createEntityManager();
		javax.persistence.Query query = manager.createQuery("select m from MapProjectJpa m where name = :name");
		query.setParameter("name", name);
		
		//  Try to retrieve the single expected result
		try {
			m = (MapProject) query.getSingleResult();
		
		// no results
		} catch (NoResultException e) {
			System.out.println("Could not find project for name = " + name);

		// multiple results
		} catch (NonUniqueResultException e) {
			System.out.println("Multiple results for name = " + name + " -> Query: " + query.toString());

		} catch (Exception e) {
			System.out.println("Exception for name = " + name + " -> Query: " + query.toString());
			e.printStackTrace();
		}
		
		manager.close();
		return m;
	}
	
		
	/**
	* Retrieve all map projects
	* @return a List of MapProjects
	*/
	@SuppressWarnings("unchecked")
	public List<MapProject> getMapProjects() {
		
		List<MapProject> m = null;
		manager = factory.createEntityManager();
		
		// construct query
		javax.persistence.Query query = manager.createQuery("select m from MapProjectJpa m");
		
		// Try query
		try {
			m = (List<MapProject>) query.getResultList();
		} catch (Exception e) {
			System.out.println("MappingServiceJpa.getMapProjects(): Could not retrieve map projects.");
			e.printStackTrace();
		}
		
		manager.close();
		return m;
	}
	

	/**
	* Query for MapProjects
	* @param query the query
	* @return the list of MapProject
	*/
	@SuppressWarnings("unchecked")
	public List<MapProject> findMapProjects(String query) {

		List<MapProject> m = null;
		manager = factory.createEntityManager();
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
						searchFactory.getAnalyzer(MapLeadJpa.class));
				luceneQuery = queryParser.parse(query);
			}
			
			m = (List<MapProject>) fullTextEntityManager.createFullTextQuery(luceneQuery)
														.getResultList();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fullTextEntityManager != null) { fullTextEntityManager.close(); }
		}
		
		manager.close();
		
		return m;
	}
	
	/////////////////////////////////////////////////////////////////
	// MapSpecialist
	// - getMapSpecialists() 
	// - getMapProjectsForSpecialist(MapSpecialist mapSpecialist)
	// - findMapSpecialists(String query)
	/////////////////////////////////////////////////////////////////
	
	/**
	* Retrieve all map specialists
	* @return a List of MapSpecialists
	*/
	@SuppressWarnings("unchecked")
	public List<MapSpecialist> getMapSpecialists() {
		
		List<MapSpecialist> m = null;
		manager = factory.createEntityManager();
		javax.persistence.Query query = manager.createQuery("select m from MapSpecialistJpa m");
		
		// Try query
		try {
			m = (List<MapSpecialist>) query.getResultList();
		} catch (Exception e) {

		}
		
		manager.close();
		return m;
	}
	
	/**
	* Retrieve all map projects assigned to a particular map specialist
	* @param mapSpecialist the map specialist
	* @return a List of MapProjects
	*/
	@SuppressWarnings("unchecked")
	public List<MapProject> getMapProjectsForMapSpecialist(MapSpecialist mapSpecialist) {
		
		List<MapProject> m = null;
		manager = factory.createEntityManager();
		
		// TODO: Figure out pathing errors with non-mapped table
		javax.persistence.Query query = manager.createQuery(

				"SELECT p FROM MapProjectJpa as p " +
				"INNER JOIN map_projects_map_specialists as s " +
				"WITH p.id = s.map_projects_id " +
				"WHERE s.mapSpecialists_id = :mapSpecialistId");	
		
		query.setParameter("mapSpecialistId", mapSpecialist.getId());
				
		// Try query
		try {
			m = (List<MapProject>) query.getResultList();
		} catch (Exception e) {
				
		}
		manager.close();
		return m;
	}
	
	/** 
	* Query for MapSpecialists
	* @param query the query
	* @return the List of MapProjects
	*/
	@SuppressWarnings("unchecked")
	public List<MapSpecialist> findMapSpecialists(String query) {

		List<MapSpecialist> m = null;
		manager = factory.createEntityManager();
		
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
						searchFactory.getAnalyzer(MapSpecialistJpa.class));
				luceneQuery = queryParser.parse(query);
			}
			
			m = (List<MapSpecialist>) fullTextEntityManager.createFullTextQuery(luceneQuery)
														.getResultList();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fullTextEntityManager != null) { fullTextEntityManager.close(); }
		}
		
		manager.close();
		return m;
	}
	
	/////////////////////////////////////////////////////
	// MapLead
	// - getMapLeads() 
	// - getMapProjectsForLead(mapSpecialist)
	// - findMapSpecialists(String query)
	/////////////////////////////////////////////////////
	
	/**
	* Retrieve all map leads
	* @return a List of MapLeads
	*/
	@SuppressWarnings("unchecked")
	public List<MapLead> getMapLeads() {
		
		List<MapLead> m = null;
		manager = factory.createEntityManager();
		javax.persistence.Query query = manager.createQuery("select m from MapLeadJpa m");
		
		// Try query
		try {
			m = (List<MapLead>) query.getResultList();
		} catch (Exception e) {
			
		}
		
		manager.close();
		return m;
	}
	
	/**
	* Retrieve all map projects assigned to a particular map lead
	* @param mapLead the map lead
	* @return a List of MapProjects
	*/
	@SuppressWarnings("unchecked")
	public List<MapProject> getMapProjectsForMapLead(MapLead mapLead) {
		
		List<MapProject> m = null;
		manager = factory.createEntityManager();
		
		// TODO: Figure out pathing errors with non-mapped table
		javax.persistence.Query query = manager.createQuery(

				"SELECT p FROM MapProjectJpa as p " +
				"INNER JOIN map_projects_map_leads as l " +
				"WITH p.id = l.map_projects_id " +
				"WHERE l.mapSpecialists_id = :mapSpecialistId");
		
		query.setParameter("mapSpecialists_id", mapLead.getId());
		
		// Try query
		try {
			m = (List<MapProject>) query.getResultList();
		} catch (Exception e) {
						
		}
		
		manager.close();
		return m;
	}
	
	/**
	* Query for MapLeads
	* @param query the query
	* @return the List of MapProjects
	*/
	@SuppressWarnings("unchecked")
	public List<MapLead> findMapLeads(String query) {
		
		List<MapLead> m = null;
		manager = factory.createEntityManager();
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
			
			m = (List<MapLead>) fullTextEntityManager.createFullTextQuery(luceneQuery)
														.getResultList();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fullTextEntityManager != null) { fullTextEntityManager.close(); }
		}
		
		return m;
	}
	
	// TODO: Update this
	public List<String> getMapAdvice() { return null; }
}
