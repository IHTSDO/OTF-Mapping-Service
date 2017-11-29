package org.ihtsdo.otf.mapping.rest.impl;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
//import org.ihtsdo.otf.mapping.jpa.algo.LuceneReindexAlgorithm;
import org.ihtsdo.otf.mapping.jpa.services.SecurityServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.rest.AdminSerivceRest;
import org.ihtsdo.otf.mapping.services.SecurityService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * REST implementation for admin service.
 */
@Path("/admin")
@Api(value = "/admin", description = "Operations providing administration functionality.")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class AdminServiceRestImpl extends RootServiceRestImpl implements AdminSerivceRest {

	/** The security service. */
	private SecurityService securityService;

	/**
	 * Instantiates an empty {@link AdminServiceRestImpl}.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public AdminServiceRestImpl() throws Exception {
		securityService = new SecurityServiceJpa();
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.rest.impl.AdminSerivceRest#luceneReindex(java.lang.String, java.lang.String)
	 */
	@Override
	@POST
	@Path("/reindex")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@ApiOperation(value = "Reindexes specified objects", notes = "Recomputes lucene indexes for the specified comma-separated objects")
	public void luceneReindex(
			@ApiParam(value = "Comma-separated list of objects to reindex, e.g. ConceptJpa (optional)", required = false) @HeaderParam("Indexed-Objects") String indexedObjects,
			@ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @HeaderParam("Authorization") String authToken)
			throws Exception {

		Logger.getLogger(getClass())
				.info("RESTful call (Content): /reindex "
						+ (indexedObjects == null ? "with no objects specified"
								: "with specified objects " + indexedObjects));

//		// Track system level information
//		long startTimeOrig = System.nanoTime();
//		final LuceneReindexAlgorithm algo = new LuceneReindexAlgorithm();
//		try {
//			//final String userName =
//			authorizeApp(authToken, MapUserRole.ADMINISTRATOR, "lucene reindex",
//					securityService);
//
//			// set of objects to be re-indexed
//			Set<String> objectsToReindex = new HashSet<>();
//
//			// if no parameter specified, re-index all objects
//			if (indexedObjects == null) {
//				objectsToReindex.add("ConceptJpa");
//				objectsToReindex.add("MapProjectJpa");
//				objectsToReindex.add("MapRecordJpa");
//				objectsToReindex.add("TreePositionJpa");
//				objectsToReindex.add("TrackingRecordJpa");
//				objectsToReindex.add("FeedbackConversationJpa");
//				objectsToReindex.add("ReportJpa");
//
//				// otherwise, construct set of indexed objects
//			} else {
//
//				// remove white-space and split by comma
//				String[] objects = indexedObjects.replaceAll(" ", "")
//						.split(",");
//
//				// add each value to the set
//				for (String object : objects)
//					objectsToReindex.add(object);
//			}
//
//			// algo.setLastModifiedBy(userName);
//			algo.setIndexedObjects(indexedObjects);
//			algo.compute();
//
//			// Final logging messages
//			Logger.getLogger(getClass()).info("      elapsed time = "
//					+ getTotalElapsedTimeStr(startTimeOrig));
//			Logger.getLogger(getClass()).info("done ...");
//
//		} catch (Exception e) {
//			handleException(e, "trying to reindex");
//		} finally {
//			algo.close();
//			securityService.close();
//		}

	}

}
