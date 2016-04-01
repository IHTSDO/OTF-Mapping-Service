package org.ihtsdo.otf.mapping.test.jpa;

import java.util.Map;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.helpers.TreePositionList;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.GmdnProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Implementation of the "Metadata Service REST Degenerate Use" Test Cases.
 */
public class GmdnTreePositionTest {

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    // do nothing
  }

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    //
  }

  /**
   * Test treepos lookup. Commented out because it requries GMDN loaded.
   *
   * @throws Exception the exception
   */
  @Test
  public void testTreeposLookup() throws Exception {
    ContentService contentService = new ContentServiceJpa();
    MetadataService metadataService = new MetadataServiceJpa();
    MappingService mappingService = new MappingServiceJpa();
    TreePositionList treePositions =
        contentService.getTreePositionsWithChildren("61690", "GMDN", "16_1");
    Logger.getLogger(getClass()).info(
        "  treepos count = " + treePositions.getTotalCount());

    String terminology =
        treePositions.getTreePositions().get(0).getTerminology();
    String terminologyVersion =
        treePositions.getTreePositions().get(0).getTerminologyVersion();
    Map<String, String> descTypes =
        metadataService.getDescriptionTypes(terminology, terminologyVersion);
    Map<String, String> relTypes =
        metadataService.getRelationshipTypes(terminology, terminologyVersion);

    // Calculate info for tree position information panel
    contentService.computeTreePositionInformation(treePositions, descTypes,
        relTypes);

    // Determine whether code is valid (e.g. whether it should be a
    // link)
    final ProjectSpecificAlgorithmHandler handler =
        new GmdnProjectSpecificAlgorithmHandler();
    MapProject mp = new MapProjectJpa();
    mp.setDestinationTerminology("GMDN");
    mp.setDestinationTerminologyVersion("16_1");
    handler.setMapProject(mp);

    // Compute any additional project specific handler info
    mappingService.setTreePositionValidCodes(null, treePositions, handler);
    mappingService
        .setTreePositionTerminologyNotes(null, treePositions, handler);

    // Second
    treePositions =
        contentService.getTreePositionGraphForQuery("GMDN", "16_1", "terminologyId:61690");
    Logger.getLogger(getClass()).info(
        "  treepos count = " + treePositions.getTotalCount());

    terminology = treePositions.getTreePositions().get(0).getTerminology();
    terminologyVersion =
        treePositions.getTreePositions().get(0).getTerminologyVersion();
    descTypes =
        metadataService.getDescriptionTypes(terminology, terminologyVersion);
    relTypes =
        metadataService.getRelationshipTypes(terminology, terminologyVersion);

    contentService.computeTreePositionInformation(treePositions, descTypes,
        relTypes);

    mappingService.setTreePositionValidCodes(null, treePositions, handler);
    mappingService
        .setTreePositionTerminologyNotes(null, treePositions, handler);

  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    // do nothing
  }

  /**
   * Teardown class.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {
    // do nothing
  }

}
