package org.ihtsdo.otf.mapping.jpa.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.TreePosition;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

/**
 * GMDN project specific algorithm handler.
 */
public class GmdnProjectSpecificAlgorithmHandler
    extends DefaultProjectSpecificAlgorithmHandler {

  /** The term type. */
  private static Long termType = null;

  @Override
  public ValidationResult validateTargetCodes(MapRecord record)
    throws Exception {
    final ValidationResult result = new ValidationResultJpa();
    result.merge(this.recordViolatesOneToOneConstraintHelper(record));
    return result;
  }

  /* see superclass */
  @Override
  public boolean isTargetCodeValid(String terminologyId) throws Exception {

    // Cache the "Term" term type - valid codes require it
    cacheTermType(mapProject.getDestinationTerminology(),
        mapProject.getDestinationTerminologyVersion());

    final ContentService contentService = new ContentServiceJpa();

    try {
      // Concept must exist
      final Concept concept = contentService.getConcept(terminologyId,
          mapProject.getDestinationTerminology(),
          mapProject.getDestinationTerminologyVersion());

      // Only concepts with "term" description types
      if (concept != null) {
        for (final Description desc : concept.getDescriptions()) {
          if (desc.getTypeId().equals(termType)) {
            return true;
          }
        }
      }
      return false;

    } catch (Exception e) {
      throw e;
    } finally {
      contentService.close();
    }
  }

  /**
   * Cache term type.
   *
   * @param terminology the terminology
   * @param version the version
   * @throws Exception the exception
   */
  private static void cacheTermType(String terminology, String version)
    throws Exception {
    // lazy initialize
    if (termType == null) {
      final MetadataService service = new MetadataServiceJpa();
      try {
        for (final Map.Entry<String, String> entry : service
            .getDescriptionTypes(terminology, version).entrySet()) {
          if (entry.getValue().equals("Term")) {
            termType = Long.valueOf(entry.getKey());
          }
        }

      } catch (Exception e) {
        throw e;
      } finally {
        service.close();
      }
    }
  }

  /* see superclass */
  @Override
  public List<TreePosition> limitTreePositions(
    List<TreePosition> treePositions) {
    // If the tree structure has more than say 100 positions, just return the
    // top one from each root
    List<TreePosition> result = new ArrayList<TreePosition>();
    if (countTreePositions(treePositions) > 100) {
      for (final TreePosition treePosition : treePositions) {
        stripTreePosition(treePosition);
        result.add(treePosition);
      }
    } else {
      return treePositions;
    }

    return result;
  }

  /**
   * Count tree positions.
   *
   * @param treePositions the tps
   * @return the int
   */
  private int countTreePositions(List<TreePosition> treePositions) {
    int i = 0;
    for (final TreePosition treePosition : treePositions) {
      i++;
      i += countTreePositions(treePosition.getChildren());
    }

    return i;

  }

  /**
   * Strip tree position.
   *
   * @param treePosition the tree position
   */
  private void stripTreePosition(TreePosition treePosition) {
    if (treePosition.getChildren().size() > 1) {
      final TreePosition firstChild = treePosition.getChildren().get(0);
      treePosition.getChildren().clear();
      treePosition.getChildren().add(firstChild);
    }
    for (final TreePosition child : treePosition.getChildren()) {
      stripTreePosition(child);
    }
  }

  @Override
  public boolean isOneToOneConstrained() {
    return true;
  }

  @Override
  public boolean recordViolatesOneToOneConstraint(MapRecord record)
    throws Exception {
    final ValidationResult result =
        recordViolatesOneToOneConstraintHelper(record);

    return result.getWarnings().size() > 0;
  }

  private ValidationResult recordViolatesOneToOneConstraintHelper(
    MapRecord record) throws Exception {
    final ContentService service = new ContentServiceJpa();
    final ValidationResult result = new ValidationResultJpa();
    try {
      // check for one to one constraint (if not blank)
      for (final MapEntry entry : record.getMapEntries()) {
        if (entry.getTargetId() != null && !entry.getTargetId().isEmpty()) {
          final int[] totalCt = new int[1];
          @SuppressWarnings("unchecked")
          List<MapRecord> records = (List<MapRecord>) service.getQueryResults(
              "mapProjectId:" + mapProject.getId()
                  + " AND (workflowStatus:PUBLISHED OR workflowStatus:READY_FOR_PUBLICATION)"
                  + " AND mapEntries.targetId:\"" + entry.getTargetId() + "\"",
              MapRecordJpa.class, MapRecordJpa.class, null, totalCt);
          for (final MapRecord r : records) {
            // NOTE: id not currently indexed, cannot include in query
            if (!r.getConceptId().equals(record.getConceptId())) {
              result.getWarnings()
                  .add("Target code " + entry.getTargetId()
                      + " already mapped from concept " + r.getConceptId()
                      + " | " + r.getConceptName() + " |");
            }
          }
        }
      }
    } catch (Exception e) {
      throw e;
    } finally {
      service.close();
    }
    return result;
  }

}
