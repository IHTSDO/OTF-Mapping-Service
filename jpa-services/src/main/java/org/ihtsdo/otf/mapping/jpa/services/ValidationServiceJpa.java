package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.ValidationService;

public class ValidationServiceJpa implements ValidationService {

	public ValidationServiceJpa() { }
	
	@Override
	public ValidationResult validateMapRecord(MapRecord mapRecord) throws Exception {
		
		
		// get the map project for this record
		MappingService mappingService = new MappingServiceJpa();
		MapProject mapProject = mappingService.getMapProject(mapRecord.getMapProjectId());
		mappingService.close();
		
		ProjectSpecificAlgorithmHandler algorithmHandler = 
				mapProject.getProjectSpecificAlgorithmHandler();
		
		ValidationResult validationResult = algorithmHandler.validateRecord(mapRecord);
		
		return validationResult;
	}
}
	