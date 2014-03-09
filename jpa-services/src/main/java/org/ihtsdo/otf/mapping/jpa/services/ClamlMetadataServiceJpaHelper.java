package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * The Class SnomedMetadataServiceJpaHelper.
 * 
 * @author ${author}
 */
public class ClamlMetadataServiceJpaHelper implements MetadataService {

	/**
	 * Returns the isa relationship type.
	 * 
	 * @param terminology the terminology
	 * @param version the version
	 * @return the isa relationship type
	 * @throws Exception the exception
	 */
	private static Long getIsaRelationshipType(String terminology, String version)
		throws Exception {
		ContentService contentService = new ContentServiceJpa();
		SearchResultList results =
				contentService.findConcepts("Isa", new PfsParameterJpa());
		for (SearchResult result : results.getSearchResults()) {
			if (result.getTerminology().equals(terminology)
					&& result.getTerminologyVersion().equals(version)
					&& result.getValue().equals("Isa")) {
				
				contentService.close();
				return new Long(result.getTerminologyId());
			}
		}
		contentService.close();
		return -1L;
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
		String version) {
		// no-op - this is just helper class
		return null;
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
		Map<Long, String> map = new HashMap<Long, String>();

		ContentService contentService = new ContentServiceJpa();
		String rootId = "";
		SearchResultList results =
				contentService.findConcepts("Module", new PfsParameterJpa());
		for (SearchResult result : results.getSearchResults()) {
			if (result.getTerminology().equals(terminology)
					&& result.getTerminologyVersion().equals(version)
					&& result.getValue().equals("Module")) {
				rootId = result.getTerminologyId();
				break;
			}
		}

		Set<Concept> descendants =
				contentService.getDescendants(rootId, terminology, version,
						getIsaRelationshipType(terminology, version));

		for (Concept descendant : descendants) {
			if (descendant.isActive()) {
				map.put(new Long(descendant.getTerminologyId()),
						descendant.getDefaultPreferredName());
			}
		}
		contentService.close();
		return map;
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
		String version) throws NumberFormatException, Exception {
		return new HashMap<Long, String>();
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
		String version) throws NumberFormatException, Exception {
		return new HashMap<Long, String>();

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
		throws NumberFormatException, Exception {
		return new HashMap<Long, String>();
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
		String version) throws NumberFormatException, Exception {

		return new HashMap<Long, String>();
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
		throws NumberFormatException, Exception {
		return new HashMap<Long, String>();
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
		throws NumberFormatException, Exception {
		return new HashMap<Long, String>();
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
		String version) throws NumberFormatException, Exception {
		Map<Long, String> map = new HashMap<Long, String>();

		ContentService contentService = new ContentServiceJpa();
		String rootId = "";
		SearchResultList results =
				contentService.findConcepts("Definition status", new PfsParameterJpa());
		for (SearchResult result : results.getSearchResults()) {
			if (result.getTerminology().equals(terminology)
					&& result.getTerminologyVersion().equals(version)
					&& result.getValue().equals("Definition status")) {
				rootId = result.getTerminologyId();
				break;
			}
		}

		Set<Concept> descendants =
				contentService.getDescendants(rootId, terminology, version,
						getIsaRelationshipType(terminology, version));

		for (Concept descendant : descendants) {
			if (descendant.isActive()) {
				map.put(new Long(descendant.getTerminologyId()),
						descendant.getDefaultPreferredName());
			}
		}
		contentService.close();
		return map;
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
		String version) throws NumberFormatException, Exception {
		Map<Long, String> map = new HashMap<Long, String>();

		ContentService contentService = new ContentServiceJpa();
		String rootId = "";
		SearchResultList results =
				contentService.findConcepts("Description type", new PfsParameterJpa());
		for (SearchResult result : results.getSearchResults()) {
			if (result.getTerminology().equals(terminology)
					&& result.getTerminologyVersion().equals(version)
					&& result.getValue().equals("Description type")) {
				rootId = result.getTerminologyId();
				break;
			}
		}

		Set<Concept> descendants =
				contentService.getDescendants(rootId, terminology, version,
						getIsaRelationshipType(terminology, version));

		for (Concept descendant : descendants) {
			if (descendant.isActive()) {
				map.put(new Long(descendant.getTerminologyId()),
						descendant.getDefaultPreferredName());
			}
		}
		contentService.close();
		return map;
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
		String version) throws NumberFormatException, Exception {
		Map<Long, String> map = new HashMap<Long, String>();

		ContentService contentService = new ContentServiceJpa();
		String rootId = "";
		SearchResultList results =
				contentService.findConcepts("Case significance", new PfsParameterJpa());
		for (SearchResult result : results.getSearchResults()) {
			if (result.getTerminology().equals(terminology)
					&& result.getTerminologyVersion().equals(version)
					&& result.getValue().equals("Case significance")) {
				rootId = result.getTerminologyId();
				break;
			}
		}
		Set<Concept> descendants =
				contentService.getDescendants(rootId, terminology, version,
						getIsaRelationshipType(terminology, version));

		for (Concept descendant : descendants) {
			if (descendant.isActive()) {
				map.put(new Long(descendant.getTerminologyId()),
						descendant.getDefaultPreferredName());
			}
		}
		contentService.close();
		return map;
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
		String version) throws NumberFormatException, Exception {
		Map<Long, String> map = new HashMap<Long, String>();

		// find all active descendants of 106237007
		ContentService contentService = new ContentServiceJpa();
		String rootId = "";
		SearchResultList results =
				contentService.findConcepts("Relationship type", new PfsParameterJpa());
		for (SearchResult result : results.getSearchResults()) {
			if (result.getTerminology().equals(terminology)
					&& result.getTerminologyVersion().equals(version)
					&& result.getValue().equals("Relationship type")) {
				rootId = result.getTerminologyId();
				break;
			}
		}
		Set<Concept> descendants =
				contentService.getDescendants(rootId, terminology, version,
						getIsaRelationshipType(terminology, version));

		for (Concept descendant : descendants) {
			if (descendant.isActive()) {
				map.put(new Long(descendant.getTerminologyId()),
						descendant.getDefaultPreferredName());
			}
		}
		contentService.close();
		return map;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#
	 * getHierarchicalRelationshipTypes(java.lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getHierarchicalRelationshipTypes(String terminology,
		String version) throws NumberFormatException, Exception {
		Map<Long, String> map = new HashMap<Long, String>();

		// find all active descendants of 106237007
		ContentService contentService = new ContentServiceJpa();
		Concept isaRel =
				contentService.getConcept(getIsaRelationshipType(terminology,
						version).toString(),terminology, version);
		map.put(new Long(isaRel.getTerminologyId()),
				isaRel.getDefaultPreferredName());
		contentService.close();
		return map;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#
	 * getRelationshipCharacteristicTypes(java.lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getRelationshipCharacteristicTypes(
		String terminology, String version) throws NumberFormatException, Exception {
		return new HashMap<Long, String>();
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
		String version) throws NumberFormatException, Exception {
		Map<Long, String> map = new HashMap<Long, String>();

		ContentService contentService = new ContentServiceJpa();
		String rootId = "";
		SearchResultList results =
				contentService.findConcepts("Modifier", new PfsParameterJpa());
		for (SearchResult result : results.getSearchResults()) {
			if (result.getTerminology().equals(terminology)
					&& result.getTerminologyVersion().equals(version)
					&& result.getValue().equals("Modifier")) {
				rootId = result.getTerminologyId();
				break;
			}
		}
		Set<Concept> descendants =
				contentService.getDescendants(rootId, terminology, version,
						getIsaRelationshipType(terminology, version));

		for (Concept descendant : descendants) {
			if (descendant.isActive()) {
				map.put(new Long(descendant.getTerminologyId()),
						descendant.getDefaultPreferredName());
			}
		}
		contentService.close();
		return map;
	}

	@Override
	public void close() {
		// no-op - this is just helper class
	}

	@Override
	public List<String> getTerminologies() {
		// no-op - this is just helper class
		return null;
	}

	@Override
	public List<String> getVersions(String terminology) {
		// no-op - this is just helper class
		return null;
	}

	@Override
	public String getLatestVersion(String terminology) {
		// no-op - this is just helper class
		return null;
	}

	@Override
	public Map<String, String> getTerminologyLatestVersions() {
		// no-op - this is just helper class
		return null;
	}

}
