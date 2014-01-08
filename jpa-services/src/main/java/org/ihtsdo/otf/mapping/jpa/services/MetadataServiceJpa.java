package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.ReaderUtil;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.ihtsdo.otf.mapping.helpers.IdNameMap;
import org.ihtsdo.otf.mapping.helpers.IdNameMapJpa;
import org.ihtsdo.otf.mapping.services.MetadataService;

// TODO: Auto-generated Javadoc
/**
 * The class for MetadataServiceJpa.
 *
 * @author ${author}
 */
public class MetadataServiceJpa implements MetadataService {

	/** The factory. */
	private static EntityManagerFactory factory;
	
	private FullTextEntityManager fullTextEntityManager;
	
	private EntityManager manager;

	/** The indexed field names. */
	private Set<String> fieldNames;

	/** The helper map. */
	private Map<String, MetadataService> helperMap = null;

	/**
	 * Instantiates an empty {@link MetadataServiceJpa}.
	 */
	public MetadataServiceJpa() {

		helperMap = new HashMap<String, MetadataService>();
		helperMap.put("SNOMEDCT", new SnomedMetadataServiceJpaHelper());
		// helperMap.put("ICD10", new Icd10MetadataServiceJpaHelper());

		// create once
		if (factory == null) {
		  factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		}
	  // create on each instantiation
		manager = factory.createEntityManager();
		
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

		
		Logger.getLogger(this.getClass()).debug("ended init " + fieldNames.toString());
	}

	/**
	 * Close the factory when done with this service.
	 */
	@Override
	public void close() throws Exception {
		if (manager.isOpen()) { manager.close(); }
		if (fullTextEntityManager.isOpen()) { fullTextEntityManager.close(); }
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getAllMetadata(java.lang.String, java.lang.String)
	 */
	@Override
	public List<IdNameMap> getAllMetadata(String terminology, String version) {
		List<IdNameMap> idNameMapList = new ArrayList<IdNameMap>();
		IdNameMap modulesIdNameMap = getModules(terminology, version);
		if (modulesIdNameMap != null) {
			modulesIdNameMap.setName("modules");
			idNameMapList.add(modulesIdNameMap);
		}
		IdNameMap atvIdNameMap = getAttributeValueRefSets(terminology, version);
		if (atvIdNameMap != null) {
			atvIdNameMap.setName("attributeValueRefSets");
			idNameMapList.add(atvIdNameMap);
		}
		IdNameMap csIdNameMap = getCaseSignificances(terminology, version);
		if (csIdNameMap != null) {
			csIdNameMap.setName("caseSignificances");
			idNameMapList.add(csIdNameMap);
		}
		IdNameMap cmIdNameMap = getComplexMapRefSets(terminology, version);
		if (cmIdNameMap != null) {
			cmIdNameMap.setName("complexMapRefSets");
			idNameMapList.add(cmIdNameMap);
		}
		IdNameMap dsIdNameMap = getDefinitionStatuses(terminology, version);
		if (dsIdNameMap != null) {
			dsIdNameMap.setName("definitionStatuses");
			idNameMapList.add(dsIdNameMap);
		}
		IdNameMap dtIdNameMap = getDescriptionTypes(terminology, version);
		if (dtIdNameMap != null) {
			dtIdNameMap.setName("descriptionTypes");
			idNameMapList.add(dtIdNameMap);
		}
		IdNameMap lIdNameMap = getLanguageRefSets(terminology, version);
		if (lIdNameMap != null) {
			lIdNameMap.setName("languageRefSets");
			idNameMapList.add(lIdNameMap);
		}
		IdNameMap mrIdNameMap = getMapRelations(terminology, version);
		if (mrIdNameMap != null) {
			mrIdNameMap.setName("mapRelations");
			idNameMapList.add(mrIdNameMap);
		}
		IdNameMap rctIdNameMap =
				getRelationshipCharacteristicTypes(terminology, version);
		if (rctIdNameMap != null) {
			rctIdNameMap.setName("relationshipCharacteristicTypes");
			idNameMapList.add(rctIdNameMap);
		}
		IdNameMap rmIdNameMap = getRelationshipModifiers(terminology, version);
		if (rmIdNameMap != null) {
			rmIdNameMap.setName("relationshipModifiers");
			idNameMapList.add(rmIdNameMap);
		}
		IdNameMap rtIdNameMap = getRelationshipTypes(terminology, version);
		if (rtIdNameMap != null) {
			rtIdNameMap.setName("relationshipTypes");
			idNameMapList.add(rtIdNameMap);
		}
		IdNameMap smIdNameMap = getSimpleMapRefSets(terminology, version);
		if (smIdNameMap != null) {
			smIdNameMap.setName("simpleMapRefSets");
			idNameMapList.add(smIdNameMap);
		}
		IdNameMap sIdNameMap = getSimpleRefSets(terminology, version);
		if (sIdNameMap != null) {
			sIdNameMap.setName("simpleRefSets");
			idNameMapList.add(sIdNameMap);
		}
		return idNameMapList;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getModules(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getModules(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getModules(terminology, version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getAttributeValueRefSets(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getAttributeValueRefSets(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getAttributeValueRefSets(terminology,
					version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getComplexMapRefSets(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getComplexMapRefSets(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getComplexMapRefSets(terminology,
					version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getLanguageRefSets(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getLanguageRefSets(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology)
					.getLanguageRefSets(terminology, version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getSimpleMapRefSets(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getSimpleMapRefSets(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getSimpleMapRefSets(terminology,
					version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getSimpleRefSets(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getSimpleRefSets(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getSimpleRefSets(terminology, version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getMapRelations(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getMapRelations(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getMapRelations(terminology, version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getDefinitionStatuses(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getDefinitionStatuses(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getDefinitionStatuses(terminology,
					version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getDescriptionTypes(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getDescriptionTypes(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getDescriptionTypes(terminology,
					version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getCaseSignificances(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getCaseSignificances(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getCaseSignificances(terminology,
					version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipTypes(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getRelationshipTypes(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getRelationshipTypes(terminology,
					version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipCharacteristicTypes(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getRelationshipCharacteristicTypes(String terminology,
		String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getRelationshipCharacteristicTypes(
					terminology, version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipModifiers(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getRelationshipModifiers(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getRelationshipModifiers(terminology,
					version);
		} else {
			// return an empty map
			return new IdNameMapJpa();
		}
	}

	@Override
	public List<String> getTerminologies() {
		
		javax.persistence.Query query =
				manager
						.createQuery("SELECT distinct c.terminology from ConceptJpa c");

		try {

			List<String> terminologies = query.getResultList();		
			if (manager.isOpen()) { manager.close(); }			
			return terminologies;			

		} catch (NoResultException e) {
			// log result and return null
			Logger.getLogger(this.getClass()).info(
					"Metadata terminologies query returned no results!");
			if (manager.isOpen()) { manager.close(); }
			return null;
		}
		
	}

	@Override
	public List<String> getVersions(String terminology) {
		
		javax.persistence.Query query =
				manager
						.createQuery("SELECT distinct c.terminologyVersion from ConceptJpa c where terminology = :terminology");

		try {

			query.setParameter("terminology", terminology);
			List<String> versions = query.getResultList();		
			if (manager.isOpen()) { manager.close(); }			
			return versions;			

		} catch (NoResultException e) {
			// log result and return null
			Logger.getLogger(this.getClass()).info(
					"Metadata versions query for terminology = "
							+ terminology + " returned no results!");
			if (manager.isOpen()) { manager.close(); }
			return null;
		}
		
	}

	@Override
	public String getLatestVersion(String terminology) {
		
		javax.persistence.Query query =
				manager
						.createQuery("SELECT max(c.terminologyVersion) from ConceptJpa c where terminology = :terminology");

		try {

			query.setParameter("terminology", terminology);
			String version = query.getSingleResult().toString();			
			if (manager.isOpen()) { manager.close(); }		
			return version;		

		} catch (NoResultException e) {
			// log result and return null
			Logger.getLogger(this.getClass()).info(
					"Metadata latest version query for terminology = "
							+ terminology + " returned no results!");
			if (manager.isOpen()) { manager.close(); }
			return null;
		}
	
	}

	@Override
	public Map<String, String> getTerminologyLatestVersions() {
		
		javax.persistence.Query query =
				manager
						.createQuery("SELECT c.terminology, max(c.terminologyVersion) from ConceptJpa c group by c.terminology");

		try {

			List<String> versions = query.getResultList();		
			if (manager.isOpen()) { manager.close(); }	
			Map<String, String> map = new HashMap<String, String>();
			for (String entry : versions) {
				map.put(entry, entry);
			}
			return map;			

		} catch (NoResultException e) {
			// log result and return null
			Logger.getLogger(this.getClass()).info(
					"Metadata latest versions query returned no results!");
			if (manager.isOpen()) { manager.close(); }
			return null;
		}

	}

}
