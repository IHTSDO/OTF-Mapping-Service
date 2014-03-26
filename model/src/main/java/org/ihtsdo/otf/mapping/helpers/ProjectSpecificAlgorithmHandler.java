package org.ihtsdo.otf.mapping.helpers;

import java.util.Set;

import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
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
	 * Checks if the map advice is computable.
	 *
	 * @param mapRecord the map record
	 * @return true, if is map advice computable
	 */
	public boolean isMapAdviceComputable(MapRecord mapRecord);
	
	/**
	 * Checks if the map relation is computable.
	 *
	 * @param mapRecord the map record
	 * @return true, if is map relation computable
	 */
	public boolean isMapRelationComputable(MapRecord mapRecord);
	
	
	/**
	 * Performs basic checks against:
	 * - record with no entries
	 * - duplicate map entries
	 * - multiple groups in project with no group structure
	 * - higher level groups without any targets
	 * - invalid TRUE rules
	 * - advices are valid for the project
	 *
	 * @param mapRecord the map record
	 * @return the validation result
	 * @throws Exception the exception
	 */
	public ValidationResult validateRecord(MapRecord mapRecord) throws Exception;

	
	/**
	 * Validate target codes.  Must be overwritten for each project handler.
	 *
	 * @param mapRecord the map record
	 * @return the validation result
	 * @throws Exception 
	 */
	public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception;
	
	/**
	 * Compute map advice and map relations. Must be overwritten for each project handler.
	 *
	 * @param mapRecord the map record
	 * @param mapEntry the map entry
	 * @return 
	 */
	public ValidationResult computeMapAdviceAndMapRelations(MapRecord mapRecord);
	
}
