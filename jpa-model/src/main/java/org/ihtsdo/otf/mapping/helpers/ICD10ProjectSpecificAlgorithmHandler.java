/*
 * 
 */
package org.ihtsdo.otf.mapping.helpers;

import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;

/**
 * Project specific algorithm handler for ICD10.
 */
public class ICD10ProjectSpecificAlgorithmHandler implements
		ProjectSpecificAlgorithmHandler {

	/** The map project. */
	private MapProject mapProject;
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#getMapProject()
	 */
	@Override
	public MapProject getMapProject() {
		return this.mapProject;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#setMapProject(org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public void setMapProject(MapProject mapProject) {
		this.mapProject = mapProject;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#isTargetCodeValid(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public boolean isTargetCodeValid(MapRecord mapRecord) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#isMapAdviceComputable(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public boolean isMapAdviceComputable(MapRecord mapRecord) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#getComputedMapAdvice(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public Set<MapAdvice> getComputedMapAdvice(MapRecord mapRecord) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#isMapRelationComputable(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public boolean isMapRelationComputable(MapRecord mapRecord) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler#getComputedMapRelation(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public MapRelation getComputedMapRelation(MapRecord mapRecord) {
		// TODO Auto-generated method stub
		return null;
	}

}
