package org.ihtsdo.otf.mapping.helpers;

import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;

public class ICD10ProjectSpecificAlgorithmHandler implements
		ProjectSpecificAlgorithmHandler {

	private MapProject mapProject;
	
	@Override
	public MapProject getMapProject() {
		return this.mapProject;
	}

	@Override
	public void setMapProject(MapProject mapProject) {
		this.mapProject = mapProject;
	}

	@Override
	public boolean isTargetCodeValid(MapRecord mapRecord) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isMapAdviceComputable(MapRecord mapRecord) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<MapAdvice> getComputedMapAdvice(MapRecord mapRecord) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isMapRelationComputable(MapRecord mapRecord) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public MapRelation getComputedMapRelation(MapRecord mapRecord) {
		// TODO Auto-generated method stub
		return null;
	}

}
