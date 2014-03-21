package org.ihtsdo.otf.mapping.helpers;

import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;

// TODO: Auto-generated Javadoc
/**
 * The Interface ProjectSpecificAlgorithmHandler.
 */
public interface ProjectSpecificAlgorithmHandler extends Configurable {

	/**
	 * Gets the map project.
	 *
	 * @return the map project
	 */
	public MapProject getMapProject();
	
	/**
	 * Sets the map project.
	 *
	 * @param mapProject the new map project
	 */
	public void setMapProject(MapProject mapProject);
	
	/**
	 * Checks if is target code valid.
	 *
	 * @param mapRecord the map record
	 * @return true, if is target code valid
	 */
	public boolean isTargetCodeValid(MapRecord mapRecord);
	
	/**
	 * Checks if the map advice is computable.
	 *
	 * @param mapRecord the map record
	 * @return true, if is map advice computable
	 */
	public boolean isMapAdviceComputable(MapRecord mapRecord);
	
	/**
	 * Gets the computed map advice.
	 *
	 * @param mapRecord the map record
	 * @return the computed map advice
	 */
	public Set<MapAdvice> getComputedMapAdvice(MapRecord mapRecord);
	
	/**
	 * Checks if the map relation is computable.
	 *
	 * @param mapRecord the map record
	 * @return true, if is map relation computable
	 */
	public boolean isMapRelationComputable(MapRecord mapRecord);
	
	/**
	 * Gets the computed map relation.
	 *
	 * @param mapRecord the map record
	 * @return the computed map relation
	 */
	public MapRelation getComputedMapRelation(MapRecord mapRecord);
	
}
