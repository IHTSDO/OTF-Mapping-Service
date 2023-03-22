/*
 *    Copyright 2023 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.FlatMapUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

/**
 * The Class MappingCompareReportMojo.
 * 
 * mvn package -P MappingCompareReport -Drun.config="" -Dleft.url=""
 * -Dright.url=""
 * 
 * left.url=
 * https://snowstorm.ihtsdotools.org/snowstorm/snomed-ct/MAIN%2F2023-02-28/members?referenceSet=446608001
 * right.url =
 * https://authoring.ihtsdotools.org/snowstorm/snomed-ct/MAIN%2F2023-02-28/members?referenceSet=446608001
 * 
 * Note: exclude the offset, limit and searchAfter from the url
 * 
 * @goal run-mapping-compare-report
 */
public class MappingCompareReportMojo extends AbstractOtfMappingMojo {

  /** The logger. */
  private static Logger logger =
      LoggerFactory.getLogger(MappingCompareReportMojo.class);

  /** The query params. */
  private static final String QUERY_PARAMS =
      "&offset={offset}&limit={limit}&searchAfter={searchAfter}";

  /**
   * The left url.
   *
   * @parameter
   * @required
   */
  private String leftUrl;

  /**
   * The right url.
   *
   * @parameter
   * @required
   */
  private String rightUrl;

  /* see superclass */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    getLog().info("Start Map Compare Report Mojo");

    if (leftUrl == null || rightUrl == null) {
      getLog().error("leftUrl, rightUrl parameters or both were not set.");
      throw new MojoExecutionException(
          "leftUrl, rightUrl parameters or both were not set.");
    }

    if (leftUrl.contains("offset=") || leftUrl.contains("limit=")
        || leftUrl.contains("searchAfter=") || rightUrl.contains("offset=")
        || rightUrl.contains("limit=") || rightUrl.contains("searchAfter=")) {
      getLog().error(
          "leftUrl or rightUrl URLs contains one of offset, limit, or searchAfter URL parameters.");
      throw new MojoExecutionException(
          "leftUrl or rightUrl URLs contains one of offset, limit, or searchAfter URL parameters. Please remove and retry.");
    }

    try {
      runReport();

    } catch (Exception e) {
      getLog().error("Error running Map Compare Report Mojo.", e);
    }
  }

  /**
   * Run report.
   *
   * @throws Exception the exception
   */
  private void runReport() throws Exception {

    try {

      final Map<String, JsonNode> leftMap = getMaps(leftUrl + QUERY_PARAMS);
      final Map<String, JsonNode> rightMap = getMaps(rightUrl + QUERY_PARAMS);

      // compare
      final List<String> missingFromRight = findMissing(leftMap, rightMap);

      if (missingFromRight != null) {
        logger.info(
            "\n----------------------------------------------------------------"
                + "\nIN LEFT, MISSING FROM RIGHT ..."
                + "\n----------------------------------------------------------------");
        missingFromRight.forEach(member -> {
          logger.info("" + leftMap.get(member));
        });

      }

      final List<String> missingFromLeft = findMissing(rightMap, leftMap);

      if (missingFromLeft != null) {
        logger.info(
            "\n----------------------------------------------------------------"
                + "\nIN RIGHT, MISSING FROM LEFT ..."
                + "\n----------------------------------------------------------------");
        missingFromLeft.forEach(member -> {
          logger.info("" + rightMap.get(member));
        });
      }

      // both, but not the same
      final Set<String> matchingKeys = leftMap.keySet().stream()
          .filter(rightMap::containsKey).collect(Collectors.toSet());

      if (matchingKeys != null) {
        logger.info(
            "\n----------------------------------------------------------------"
                + "\nIN BOTH BUT DIFFERENT ..."
                + "\n----------------------------------------------------------------");
        matchingKeys.forEach(key -> {
          if (!leftMap.get(key).equals(rightMap.get(key))) {
            try {
              logger.info("\tDIFFERENCES for referencedComponentId:"
                  + leftMap.get(key).get("referencedComponentId").asText());
              diffMembers(leftMap.get(key), rightMap.get(key));
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });

      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Returns the maps.
   *
   * @param url the url
   * @return the maps
   * @throws Exception the exception
   */
  // exclude offset and limit and searchAfter
  private static Map<String, JsonNode> getMaps(final String url)
    throws Exception {

    final Client client = ClientBuilder.newClient();
    String searchAfter = "";
    final int limit = 5000;
    final ObjectMapper mapper = new ObjectMapper();
    final Map<String, JsonNode> memberMap = new HashMap<String, JsonNode>();

    boolean done = false;
    while (!done) {

      int returnedMembersCount = 0;

      final WebTarget webTarget = client.target(url)
          .resolveTemplate("offset", 0).resolveTemplate("limit", limit)
          .resolveTemplate("searchAfter", searchAfter);

      logger.info("URL: {}", webTarget.getUri());

      final Response response = webTarget.request(MediaType.APPLICATION_JSON)
          .header("Accept-Language",
              "en-X-900000000000509007,en-X-900000000000508004,en")
          .header("Cookie", ConfigUtility.getGenericUserCookie()).get();

      final String resultString = response.readEntity(String.class);

      if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
        throw new LocalException(
            "Unexpected terminology server failure. Message = " + resultString);
      }

      final JsonNode jsonDocument = mapper.readTree(resultString);

      logger.info("Size: {}", jsonDocument.get("items").size());

      // get members returned in this call (up to 10000)
      for (final JsonNode itemNode : jsonDocument.get("items")) {
        final String memberId = itemNode.get("memberId").asText();
        memberMap.put(memberId, itemNode);
        returnedMembersCount++;
      }

      // if we don't get a full page of results, we've processed the final page
      searchAfter = jsonDocument.get("searchAfter").asText();
      logger.info("SearchAfter: " + searchAfter);
      done = returnedMembersCount < limit;

    }

    return memberMap;

  }

  /**
   * Find maps existing in left map which do not exist in right map.
   *
   * @param leftMap the left map
   * @param rightMap the right map
   * @return the list of memberIds
   */
  private static List<String> findMissing(final Map<String, JsonNode> leftMap,
    final Map<String, JsonNode> rightMap) {

    final List<String> missing = new ArrayList<>();

    for (final String key : leftMap.keySet()) {
      if (!rightMap.containsKey(key)) {
        missing.add(key);
      }
    }
    return missing;

  }

  /**
   * Find differences between two JSON Nodes.
   *
   * @param leftJsonNode the left json node
   * @param rightJsonNode the right json node
   * @throws Exception the exception
   */
  private static void diffMembers(final JsonNode leftJsonNode,
    final JsonNode rightJsonNode) throws Exception {

    final ObjectMapper mapper = new ObjectMapper();

    final TypeReference<HashMap<String, Object>> type =
        new TypeReference<HashMap<String, Object>>() {
        };

    final Map<String, Object> leftMap = mapper.convertValue(leftJsonNode, type);
    final Map<String, Object> rightMap =
        mapper.convertValue(rightJsonNode, type);

    final Map<String, Object> leftFlatMap = FlatMapUtility.flatten(leftMap);
    final Map<String, Object> rightFlatMap = FlatMapUtility.flatten(rightMap);

    final MapDifference<String, Object> difference =
        Maps.difference(leftFlatMap, rightFlatMap);

    if (!difference.entriesOnlyOnLeft().isEmpty()) {
      logger.info("\t   Entries only on the left --------------------------");
      difference.entriesOnlyOnLeft()
          .forEach((key, value) -> logger.info("\t\t" + key + ": " + value));
    }

    if (!difference.entriesOnlyOnRight().isEmpty()) {
      logger.info("\t   Entries only on the right --------------------------");
      difference.entriesOnlyOnRight()
          .forEach((key, value) -> logger.info("\t\t" + key + ": " + value));
    }

    if (!difference.entriesDiffering().isEmpty()) {
      logger.info("\t   Entries differing --------------------------");
      difference.entriesDiffering()
          .forEach((key, value) -> logger.info("\t\t" + key + ": " + value));
    }

  }

}
