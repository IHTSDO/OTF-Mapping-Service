package org.ihtsdo.otf.mapping.jpa.services;

import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.WorkflowStatus;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ValidationService;

/**
 * Reference implementation of the {@link ValidationService}.
 *
 * @author ${author}
 */
public class ValidationServiceJpa implements ValidationService {

	/**
	 * Instantiates an empty {@link ValidationServiceJpa}.
	 */
	public ValidationServiceJpa() {
	  // do nothing
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.ValidationService#validateMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public ValidationResult validateMapRecord(MapRecord mapRecord) throws Exception {
		
		
		// get the map project for this record
		MappingService mappingService = new MappingServiceJpa();
		MapProject mapProject = mappingService.getMapProject(mapRecord.getMapProjectId());
		mappingService.close();
		
		ProjectSpecificAlgorithmHandler algorithmHandler = 
				mappingService.getProjectSpecificAlgorithmHandler(mapProject);
		
		ValidationResult validationResult = algorithmHandler.validateRecord(mapRecord);
		
		mapRecord.setWorkflowStatus(WorkflowStatus.EDITING_IN_PROGRESS);
		return validationResult;
	}
}
	