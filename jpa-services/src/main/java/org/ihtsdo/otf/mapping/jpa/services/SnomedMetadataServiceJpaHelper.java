package org.ihtsdo.otf.mapping.jpa.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.MetadataService;

public class SnomedMetadataServiceJpaHelper implements MetadataService {

	@Override
	public Map<Long, String> getAllMetadata(String terminology, String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getModules(String terminology, String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getAttributeValueRefSets(String terminology,
		String version) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.MetadataService#getComplexMapRefSets(java.lang.String, java.lang.String)
	 */
	@Override
	public Map<Long, String> getComplexMapRefSets(String terminology,
		String version) {
		Map<Long,String> map = new HashMap<Long, String>();

	  // find all active descendants of 447250001
		ContentServiceJpa contentService = new ContentServiceJpa();
	  Set<Concept> descendants = new HashSet<Concept>();
	  contentService.getDescendants(new Long("447250001"), terminology, version, descendants);

	  for (Concept descendant : descendants) {
	  	if (descendant.isActive()) {
	      map.put(new Long(descendant.getTerminologyId()), 
	    		descendant.getDefaultPreferredName());
	  	}
	  }
	  return map;

	}

	@Override
	public Map<Long, String> getLanguageRefsets(String terminology, String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getSimpleMapRefsets(String terminology,
		String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getSimpleRefsets(String terminology, String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getMapRelations(String terminology, String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getDefinitionStatuses(String terminology,
		String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getDescriptionTypes(String terminology,
		String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getCaseSignificances(String terminology,
		String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getRelationshipTypes(String terminology,
		String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getRelationshipCharacteristicTypes(
		String terminology, String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Long, String> getRelationshipModifiers(String terminology,
		String version) {
		// TODO Auto-generated method stub
		return null;
	}

}
