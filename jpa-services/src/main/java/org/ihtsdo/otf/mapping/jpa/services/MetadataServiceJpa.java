package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.ReaderUtil;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * 
 * The class for MetadataServiceJpa
 *
 */
public class MetadataServiceJpa implements MetadataService {

		/** The factory. */
	private EntityManagerFactory factory;
	
	/** The indexed field names. */
	private Set<String> fieldNames;

	Map<String, MetadataService> helperMap = null;
	
	/**
	 * Instantiates an empty {@link MetadataServiceJpa}.
	 */
	public MetadataServiceJpa() {
		
		helperMap = new HashMap<String, MetadataService>();
		helperMap.put("SNOMEDCT", new SnomedMetadataServiceJpaHelper());
		//helperMap.put("ICD10", new Icd10MetadataServiceJpaHelper());

		factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		EntityManager manager = factory.createEntityManager();
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
			System.out.println("Failed to close MetadataService!");
			e.printStackTrace();
		}
	}


	@Override
	public Map<Long, String> getAllMetadata(String terminology, String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getModules(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getModules(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getAttributeValueRefSets(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getAttributeValueRefSets(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getComplexMapRefSets(String terminology,
		String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getComplexMapRefSets(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getLanguageRefsets(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getLanguageRefsets(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getSimpleMapRefsets(String terminology,
		String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getSimpleMapRefsets(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getSimpleRefsets(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getSimpleRefsets(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getMapRelations(String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getMapRelations(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getDefinitionStatuses(String terminology,
		String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getDefinitionStatuses(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getDescriptionTypes(String terminology,
		String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getDescriptionTypes(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getCaseSignificances(String terminology,
		String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getCaseSignificances(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getRelationshipTypes(String terminology,
		String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getRelationshipTypes(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getRelationshipCharacteristicTypes(
		String terminology, String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getRelationshipCharacteristicTypes(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}

	@Override
	public Map<Long, String> getRelationshipModifiers(String terminology,
		String version) {
		if (helperMap.containsKey(terminology)) {
			return helperMap.get(terminology).getRelationshipModifiers(terminology,version);
		} else {
			// return an empty map
			return new HashMap<Long, String>();
		}
	}
	
	
	
}
