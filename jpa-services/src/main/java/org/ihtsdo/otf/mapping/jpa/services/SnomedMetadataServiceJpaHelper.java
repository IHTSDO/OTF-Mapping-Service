package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.IdNameMap;
import org.ihtsdo.otf.mapping.helpers.IdNameMapJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * The Class SnomedMetadataServiceJpaHelper.
 *
 * @author ${author}
 */
public class SnomedMetadataServiceJpaHelper implements MetadataService {

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
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 900000000000443000 
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("900000000000443000", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getAttributeValueRefSets(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getAttributeValueRefSets(String terminology,
		String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 900000000000480006
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("900000000000480006", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getComplexMapRefSets(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getComplexMapRefSets(String terminology,
		String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 447250001
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("447250001", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getLanguageRefSets(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getLanguageRefSets(String terminology, String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 900000000000506000
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("900000000000506000", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getSimpleMapRefSets(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getSimpleMapRefSets(String terminology,
		String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 900000000000496009
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("900000000000496009", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getSimpleRefSets(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getSimpleRefSets(String terminology, String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 446609009
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("446609009", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getMapRelations(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getMapRelations(String terminology, String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 609330002
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("609330002", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getDefinitionStatuses(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getDefinitionStatuses(String terminology,
		String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 900000000000444006
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("900000000000444006", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getDescriptionTypes(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getDescriptionTypes(String terminology,
		String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 900000000000446008
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("900000000000446008", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getCaseSignificances(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getCaseSignificances(String terminology,
		String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 900000000000447004
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("900000000000447004", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipTypes(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getRelationshipTypes(String terminology,
		String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 900000000000463007
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("900000000000463007", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipCharacteristicTypes(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getRelationshipCharacteristicTypes(
		String terminology, String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 900000000000449001
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("900000000000449001", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;		
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getRelationshipModifiers(java.lang.String, java.lang.String)
	 */
	@Override
	public IdNameMap getRelationshipModifiers(String terminology,
		String version) {
		IdNameMap map = new IdNameMapJpa();

	  // find all active descendants of 900000000000450001
		ContentService contentService = new ContentServiceJpa();
	  Set<Concept> descendants = contentService.getDescendants("900000000000450001", terminology, version, new Long("116680003"));

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.addIdNameMapEntry(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;		
	}

	@Override
	public void close() throws Exception {
		// no-op  - this is just helper class
	}

	@Override
	public List<String> getTerminologies() {
		// no-op  - this is just helper class
		return null;
	}

	@Override
	public List<String> getVersions(String terminology) {
		// no-op  - this is just helper class
		return null;
	}

	@Override
	public String getLatestVersion(String terminology) {
		// no-op  - this is just helper class
		return null;
	}

	@Override
	public Map<String, String> getTerminologyLatestVersions() {
		// no-op  - this is just helper class
		return null;
	}

}
