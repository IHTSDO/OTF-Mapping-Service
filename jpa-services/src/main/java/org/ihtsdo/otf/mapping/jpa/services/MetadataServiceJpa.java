package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * The class for MetadataServiceJpa.
 * 
 * @author ${author}
 */
public class MetadataServiceJpa implements MetadataService {

	/** The factory. */
	private static EntityManagerFactory factory;

	/** The full text entity manager. */
	private FullTextEntityManager fullTextEntityManager;

	/** The manager. */
	private EntityManager manager;

	/** The indexed field names. */
	private Set<String> fieldNames;

	/** The helper map. */
	private Map<String, MetadataService> helperMap = null;

	/**
	 * Instantiates an empty {@link MetadataServiceJpa}.
	 */
	public MetadataServiceJpa() {

		helperMap = new HashMap<>();
		helperMap.put("SNOMEDCT", new SnomedMetadataServiceJpaHelper());
		helperMap.put("ICD10", new ClamlMetadataServiceJpaHelper());
		helperMap.put("ICD9CM", new ClamlMetadataServiceJpaHelper());
		helperMap.put("ICPC", new ClamlMetadataServiceJpaHelper());

		// created once or if the factory has closed
		if (factory == null || !factory.isOpen()) {
			factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		}
		// create on each instantiation
		manager = factory.createEntityManager();

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

		Logger.getLogger(this.getClass()).debug(
				"ended init " + fieldNames.toString());
	}

	/**
	 * Close the factory when done with this service.
	 * 
	 * @throws Exception the exception
	 */
	@Override
	public void close() throws Exception {
		if (manager.isOpen()) {
			manager.close();
		}
		if (fullTextEntityManager.isOpen()) {
			fullTextEntityManager.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getAllMetadata(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	public Map<String, Map<Long, String>> getAllMetadata(String terminology,
		String version) throws Exception {
		Map<String, Map<Long, String>> idNameMapList =
				new HashMap<>();
		Map<Long, String> modulesIdNameMap = getModules(terminology, version);
		if (modulesIdNameMap != null) {
			idNameMapList.put("modules", modulesIdNameMap);
		}
		Map<Long, String> atvIdNameMap =
				getAttributeValueRefSets(terminology, version);
		if (atvIdNameMap != null) {
			idNameMapList.put("attributeValueRefSets", atvIdNameMap);
		}
		Map<Long, String> csIdNameMap = getCaseSignificances(terminology, version);
		if (csIdNameMap != null) {
			idNameMapList.put("caseSignificances", csIdNameMap);
		}
		Map<Long, String> cmIdNameMap = getComplexMapRefSets(terminology, version);
		if (cmIdNameMap != null) {
			idNameMapList.put("complexMapRefSets", cmIdNameMap);
		}
		Map<Long, String> dsIdNameMap = getDefinitionStatuses(terminology, version);
		if (dsIdNameMap != null) {
			idNameMapList.put("definitionStatuses", dsIdNameMap);
		}
		Map<Long, String> dtIdNameMap = getDescriptionTypes(terminology, version);
		if (dtIdNameMap != null) {
			idNameMapList.put("descriptionTypes", dtIdNameMap);
		}
		Map<Long, String> lIdNameMap = getLanguageRefSets(terminology, version);
		if (lIdNameMap != null) {
			idNameMapList.put("languageRefSets", lIdNameMap);
		}
		Map<Long, String> mrIdNameMap = getMapRelations(terminology, version);
		if (mrIdNameMap != null) {
			idNameMapList.put("mapRelations", mrIdNameMap);
		}
		Map<Long, String> rctIdNameMap =
				getRelationshipCharacteristicTypes(terminology, version);
		if (rctIdNameMap != null) {
			idNameMapList.put("relationshipCharacteristicTypes", rctIdNameMap);
		}
		Map<Long, String> rmIdNameMap =
				getRelationshipModifiers(terminology, version);
		if (rmIdNameMap != null) {
			idNameMapList.put("relationshipModifiers", rmIdNameMap);
		}
		Map<Long, String> rtIdNameMap = getRelationshipTypes(terminology, version);
		if (rtIdNameMap != null) {
			idNameMapList.put("relationshipTypes", rtIdNameMap);
		}
		Map<Long, String> hierRtIdNameMap =
				getHierarchicalRelationshipTypes(terminology, version);
		if (hierRtIdNameMap != null) {
			idNameMapList.put("hierarchicalRelationshipTypes", hierRtIdNameMap);
		}
		Map<Long, String> smIdNameMap = getSimpleMapRefSets(terminology, version);
		if (smIdNameMap != null) {
			idNameMapList.put("simpleMapRefSets", smIdNameMap);
		}
		Map<Long, String> sIdNameMap = getSimpleRefSets(terminology, version);
		if (sIdNameMap != null) {
			idNameMapList.put("simpleRefSets", sIdNameMap);
		}
		return idNameMapList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getModules(java.lang.String
	 * , java.lang.String)
	 */
	@Override
	public Map<Long, String> getModules(String terminology, String version)
		throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getModules(terminology, version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getAttributeValueRefSets
	 * (java.lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getAttributeValueRefSets(String terminology,
		String version) throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getAttributeValueRefSets(terminology,
					version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getComplexMapRefSets(java
	 * .lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getComplexMapRefSets(String terminology,
		String version) throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getComplexMapRefSets(terminology,
					version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getLanguageRefSets(java
	 * .lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getLanguageRefSets(String terminology, String version)
		throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology)
					.getLanguageRefSets(terminology, version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getSimpleMapRefSets(java
	 * .lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getSimpleMapRefSets(String terminology,
		String version) throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getSimpleMapRefSets(terminology,
					version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getSimpleRefSets(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getSimpleRefSets(String terminology, String version)
		throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getSimpleRefSets(terminology, version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getMapRelations(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getMapRelations(String terminology, String version)
		throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getMapRelations(terminology, version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getDefinitionStatuses(java
	 * .lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getDefinitionStatuses(String terminology,
		String version) throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getDefinitionStatuses(terminology,
					version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getDescriptionTypes(java
	 * .lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getDescriptionTypes(String terminology,
		String version) throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getDescriptionTypes(terminology,
					version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getCaseSignificances(java
	 * .lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getCaseSignificances(String terminology,
		String version) throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getCaseSignificances(terminology,
					version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipTypes(java
	 * .lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getRelationshipTypes(String terminology,
		String version) throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getRelationshipTypes(terminology,
					version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#
	 * getHierarchicalRelationshipTypes(java.lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getHierarchicalRelationshipTypes(String terminology,
		String version) throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getHierarchicalRelationshipTypes(
					terminology, version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#
	 * getRelationshipCharacteristicTypes(java.lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getRelationshipCharacteristicTypes(
		String terminology, String version) throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getRelationshipCharacteristicTypes(
					terminology, version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}


    /*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipModifiers
	 * (java.lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getRelationshipModifiers(String terminology,
		String version) throws Exception {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getRelationshipModifiers(terminology,
					version);
		} else {
			// return an empty map
			return new HashMap<>();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getTerminologies()
	 */
	@Override
	public List<String> getTerminologies() {

		javax.persistence.Query query =
				manager.createQuery("SELECT distinct c.terminology from ConceptJpa c");
		@SuppressWarnings("unchecked")
		List<String> terminologies = query.getResultList();
		if (manager.isOpen()) {
			manager.close();
		}
		return terminologies;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getVersions(java.lang.String
	 * )
	 */
	@Override
	public List<String> getVersions(String terminology) {

		javax.persistence.Query query =
				manager
						.createQuery("SELECT distinct c.terminologyVersion from ConceptJpa c where terminology = :terminology");

		query.setParameter("terminology", terminology);
		@SuppressWarnings("unchecked")
		List<String> versions = query.getResultList();
		if (manager.isOpen()) {
			manager.close();
		}
		return versions;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getLatestVersion(java.lang
	 * .String)
	 */
	@Override
	public String getLatestVersion(String terminology) {

		javax.persistence.Query query =
				manager
						.createQuery("SELECT max(c.terminologyVersion) from ConceptJpa c where terminology = :terminology");

		query.setParameter("terminology", terminology);
		String version = query.getSingleResult().toString();
		if (manager.isOpen()) {
			manager.close();
		}
		return version;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.services.MetadataService#getTerminologyLatestVersions
	 * ()
	 */
	@Override
	public Map<String, String> getTerminologyLatestVersions() {

		javax.persistence.TypedQuery<Object[]> query =
				manager
						.createQuery(
								"SELECT c.terminology, max(c.terminologyVersion) from ConceptJpa c group by c.terminology",
								Object[].class);

		List<Object[]> resultList = query.getResultList();
		Map<String, String> resultMap =
				new HashMap<>(resultList.size());
		for (Object[] result : resultList)
			resultMap.put((String) result[0], (String) result[1]);
		if (manager.isOpen()) {
			manager.close();
		}

		return resultMap;

	}



}
