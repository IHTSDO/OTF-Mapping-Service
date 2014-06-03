package org.ihtsdo.otf.mapping.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.TreePositionListJpa;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.ValidationServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.ValidationService;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * Validation service for map records.
 */
@Path("/validation")
@Api(value = "/validation", description = "Operations supporting Map objects.")
@Produces({
	MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class ValidationServiceRest {

	/** The validation service. */
	private ValidationService validationService = new ValidationServiceJpa();

	/**
	 * Validates a map record.
	 * 
	 * @param mapRecord the map record to be validated
	 * @return Response the response
	 */
	@POST
	@Path("/record/validate")
	@Consumes({
		MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	@Produces({
		MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
	})
	@ApiOperation(value = "Validates a map record", notes = "Performs validation checks on a map record", response = MapRecordJpa.class)
	public ValidationResult validateMapRecord(
			@ApiParam(value = "The map record to validate.  Must exist in mapping database. Must be in Json or Xml format", required = true) MapRecordJpa mapRecord) {

		Logger.getLogger(ValidationServiceRest.class).info(
				"RESTful call (Validation): /record/validate for map record id = " + mapRecord.getId().toString());

		// get the map project for this record

		try {
			MappingService mappingService = new MappingServiceJpa();
			MapProject mapProject;
			mapProject = mappingService.getMapProject(mapRecord
					.getMapProjectId());
			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService
					.getProjectSpecificAlgorithmHandler(mapProject);

			ValidationResult validationResult = algorithmHandler
					.validateRecord(mapRecord);
			mappingService.close();
			return validationResult;
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}


	/**
	 * Compare map records.
	 * TODO: Move this to validation services
	 * @param mapRecordId1
	 *            the map record id1
	 * @param mapRecordId2
	 *            the map record id2
	 * @return the validation result
	 */
	@GET
	@Path("/record/compare/{recordId1}/{recordId2}/")
	@ApiOperation(value = "Get the root tree (top-level concepts) for a given terminology", notes = "Returns a tree structure with an artificial root node and children representing the top-level concepts of a terminology", response = TreePositionListJpa.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public ValidationResult compareMapRecords(
			@ApiParam(value = "id of first map record", required = true) @PathParam("recordId1") Long mapRecordId1,
			@ApiParam(value = "id of second map record", required = true) @PathParam("recordId2") Long mapRecordId2) {
		Logger.getLogger(MappingServiceRest.class).info(
				"RESTful call (Mapping): /record/compare/"
						+ mapRecordId1.toString() + "/"
						+ mapRecordId2.toString());
		
		try {
			MappingService mappingService = new MappingServiceJpa();
			MapRecord mapRecord1, mapRecord2;
			
			mapRecord1 = mappingService.getMapRecord(mapRecordId1);
			mapRecord2 = mappingService.getMapRecord(mapRecordId2);

			MapProject mapProject = mappingService.getMapProject(mapRecord1
					.getMapProjectId());
			ProjectSpecificAlgorithmHandler algorithmHandler = mappingService
					.getProjectSpecificAlgorithmHandler(mapProject);
			ValidationResult validationResult = algorithmHandler.compareMapRecords(
					mapRecord1, mapRecord2);

			mappingService.close();
			return validationResult;

		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
	}
}
