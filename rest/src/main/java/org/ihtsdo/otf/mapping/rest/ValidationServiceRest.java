package org.ihtsdo.otf.mapping.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ValidationServiceJpa;
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

    try {
      return validationService.validateMapRecord(mapRecord);
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }

  }
}